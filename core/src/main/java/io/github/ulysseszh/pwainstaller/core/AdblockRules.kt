package io.github.ulysseszh.pwainstaller.core

import java.net.IDN
import java.net.URI
import java.util.Locale

/** AdGuard rule selection and network filtering. Cosmetic execution lives in the WebView module. */
object AdblockRules {
	const val MAX_LENGTH = 8 * 1024 * 1024
	const val MAX_LINE_LENGTH = 256 * 1024

	data class Cosmetic(
		val domains: String,
		val operator: String,
		val body: String,
	) {
		val exception get() = '@' in operator
	}

	private val markers = arrayOf("#@\$?#", "#\$?#", "#@%#", "#@?#", "#@\$#", "#@#", "#%#", "#?#", "#\$#", "##")

	fun lines(text: String): List<String> =
		text
			.lineSequence()
			.map(String::trim)
			.filter { it.isNotEmpty() && !it.startsWith('!') && !it.startsWith('[') }
			.distinct()
			.toList()

	fun cosmetic(line: String): Cosmetic? {
		var index = line.indexOf('#')
		while (index >= 0) {
			val marker = markers.firstOrNull { line.startsWith(it, index) }
			if (marker != null) {
				return Cosmetic(line.substring(0, index), marker, line.substring(index + marker.length))
			}
			index = line.indexOf('#', index + 1)
		}
		return null
	}

	fun host(url: String): String =
		runCatching {
			URI(url)
				.host
				?.lowercase(Locale.ROOT)
				?.trimEnd('.')
				.orEmpty()
		}.getOrDefault("")

	private fun domainMatches(
		pattern: String,
		host: String,
	): Boolean {
		val value = pattern.lowercase(Locale.ROOT)
		if (value.startsWith('/') && value.endsWith('/') && value.length <= 512) {
			return runCatching { Regex(value.drop(1).dropLast(1)).containsMatchIn(host) }.getOrDefault(false)
		}
		val ascii =
			if (value.all { it.code < 128 }) {
				value
			} else {
				runCatching { IDN.toASCII(value) }.getOrDefault(value)
			}
		if ('*' !in ascii) {
			return host == ascii || host.endsWith(".$ascii")
		}
		val regex = ascii.split('*').joinToString(".*", "^(?:.*\\.)?", "$") { Regex.escape(it) }
		return runCatching { Regex(regex).matches(host) }.getOrDefault(false)
	}

	fun domainsMatch(
		domains: String,
		host: String,
		separator: Char = ',',
	): Boolean {
		return domainsMatch(domains, separator) { domainMatches(it, host) }
	}

	private inline fun domainsMatch(
		domains: String,
		separator: Char,
		matches: (String) -> Boolean,
	): Boolean {
		var hasIncluded = false
		var included = false
		var start = 0
		while (start < domains.length) {
			val end =
				domains.indexOf(separator, start).let {
					if (it < 0) {
						domains.length
					} else {
						it
					}
				}
			val value = domains.substring(start, end).trim()
			if (value.startsWith('~')) {
				if (matches(value.drop(1))) {
					return false
				}
			} else if (value.isNotEmpty()) {
				hasIncluded = true
				if (!included && matches(value)) {
					included = true
				}
			}
			start = end + 1
		}
		return !hasIncluded || included
	}

	fun select(
		text: String,
		baseUrl: String,
	): String {
		val host = host(baseUrl)
		require(host.isNotEmpty()) { "Enter a base URL first" }
		val domains = HashMap<String, Boolean>()

		fun matches(pattern: String) = domains.getOrPut(pattern) { domainMatches(pattern, host) }
		return lines(text)
			.filter { line ->
				val cosmetic = cosmetic(line)
				if (cosmetic != null) {
					domainsMatch(cosmetic.domains, ',', ::matches)
				} else {
					val rule = parseNetwork(line, checkCosmetic = false)
					rule != null && domainsMatch(rule.domains, '|', ::matches) &&
						(!rawRegex(rule.pattern) || runCatching { compilePattern(rule) }.isSuccess)
				}
			}.joinToString("\n")
	}

	fun validate(text: String) {
		require(text.toByteArray(Charsets.UTF_8).size <= MAX_LENGTH) { "Web app filter rules exceed 8 MiB" }
		require(text.lineSequence().none { it.length > MAX_LINE_LENGTH || '\u0000' in it }) { "Invalid filter rule" }
	}

	data class Network(
		val exception: Boolean,
		val important: Boolean,
		val domains: String,
		val thirdParty: Boolean?,
		val types: Set<String>,
		val excluded: Set<String>,
		val regex: Regex,
		val domainAnchor: Boolean,
		val hostKey: String?,
		val switches: Set<String>,
	) {
		fun domainAllowed(host: String) = domainsMatch(domains, host, '|')

		fun matches(
			url: String,
			page: String,
			type: String,
		): Boolean {
			if (!domainAllowed(host(page)) || type in excluded || (types.isNotEmpty() && type !in types)) {
				return false
			}
			if (type == "document" && "document" !in types && !exception) {
				return false
			}
			if (thirdParty != null && thirdParty != (SiteDomains.site(host(url)) != SiteDomains.site(host(page)))) {
				return false
			}
			val value =
				if (domainAnchor) {
					runCatching {
						URI(url).let {
							it.host + (
								if (it.port == -1) {
									""
								} else {
									":${it.port}"
								}
							) + it.rawPath.orEmpty() +
								(it.rawQuery?.let { query -> "?$query" } ?: "")
						}
					}.getOrDefault("")
				} else {
					url
				}
			return regex.containsMatchIn(value.take(16384))
		}
	}

	private val resourceTypes =
		setOf(
			"script",
			"image",
			"stylesheet",
			"font",
			"media",
			"xmlhttprequest",
			"object",
			"other",
			"document",
			"subdocument",
			"ping",
			"websocket",
		)
	private val switches = setOf("elemhide", "generichide", "specifichide", "jsinject")
	private val hostAnchor = Regex("^\\|\\|([A-Za-z0-9.-]+)(?:\\^|/|$)")

	private data class NetworkSpec(
		val pattern: String,
		val exception: Boolean,
		val important: Boolean,
		val domains: String,
		val thirdParty: Boolean?,
		val types: Set<String>,
		val excluded: Set<String>,
		val switches: Set<String>,
		val matchCase: Boolean,
	)

	private fun parseNetwork(
		source: String,
		checkCosmetic: Boolean = true,
	): NetworkSpec? {
		if (source.isEmpty() || source.length > 8192 || (checkCosmetic && cosmetic(source) != null) || source.startsWith('!') ||
			source.startsWith('[')
		) {
			return null
		}
		val exception = source.startsWith("@@")
		val line = source.removePrefix("@@")
		val dollar =
			if (line.endsWith('/') && line.startsWith('/')) {
				-1
			} else {
				line.lastIndexOf('$')
			}
		val pattern =
			if (dollar < 0) {
				line
			} else {
				line.substring(0, dollar)
			}
		val options =
			if (dollar < 0) {
				emptyList()
			} else {
				line.substring(dollar + 1).split(',')
			}
		var domains = ""
		var third: Boolean? = null
		var matchCase = false
		var important = false
		val types = mutableSetOf<String>()
		val excluded = mutableSetOf<String>()
		val toggles = mutableSetOf<String>()
		for (option in options) {
			val negated = option.startsWith('~')
			val name = option.removePrefix("~")
			when {
				name.startsWith("domain=") -> {
					domains = name.substringAfter('=')
				}

				name == "third-party" || name == "3p" -> {
					third = !negated
				}

				name == "first-party" || name == "1p" -> {
					third = negated
				}

				name == "match-case" -> {
					matchCase = !negated
				}

				name == "important" -> {
					important = !negated
				}

				name in switches -> {
					if (!exception) {
						return null
					}
					toggles.add(name)
				}

				name in resourceTypes -> {
					(
						if (negated) {
							excluded
						} else {
							types
						}
					).add(name)
				}

				name == "xhr" -> {
					(
						if (negated) {
							excluded
						} else {
							types
						}
					).add("xmlhttprequest")
				}

				else -> {
					return null
				} // Unsupported transformations must not become broader blocking rules.
			}
		}
		return NetworkSpec(pattern, exception, important, domains, third, types, excluded, toggles, matchCase)
	}

	private fun rawRegex(pattern: String) = pattern.startsWith('/') && pattern.endsWith('/') && pattern.length > 2

	private fun compilePattern(rule: NetworkSpec): Regex {
		val pattern = rule.pattern
		val anchor = pattern.startsWith("||")
		val regex =
			if (rawRegex(pattern)) {
				pattern.drop(1).dropLast(1)
			} else {
				val content =
					pattern
						.removePrefix(
							if (anchor) {
								"||"
							} else {
								"|"
							},
						).removeSuffix("|")
				buildString {
					if (anchor) {
						append("^(?:[^/]*\\.)?")
					} else if (pattern.startsWith('|')) {
						append('^')
					}
					content.forEach { char ->
						when (char) {
							'*' -> {
								append(".*")
							}

							'^' -> {
								append("(?:[^a-zA-Z0-9_.%\\-]|$)")
							}

							else -> {
								append(Regex.escape(char.toString()))
							}
						}
					}
					if (pattern.endsWith('|')) {
						append('$')
					}
				}
			}
		return Regex(
			regex,
			if (rule.matchCase) {
				emptySet()
			} else {
				setOf(RegexOption.IGNORE_CASE)
			},
		)
	}

	fun network(source: String): Network? {
		val rule = parseNetwork(source) ?: return null
		return runCatching {
			val anchor = rule.pattern.startsWith("||")
			val key =
				if (anchor) {
					hostAnchor
						.find(rule.pattern)
						?.groupValues
						?.get(1)
						?.lowercase(Locale.ROOT)
				} else {
					null
				}
			Network(
				rule.exception,
				rule.important,
				rule.domains,
				rule.thirdParty,
				rule.types,
				rule.excluded,
				compilePattern(rule),
				anchor,
				key,
				rule.switches,
			)
		}.getOrNull()
	}

	class Engine(
		text: String,
	) {
		val cosmetics = lines(text).mapNotNull(::cosmetic)
		private val rules = lines(text).mapNotNull(::network)
		private val generic = rules.filter { it.hostKey == null }
		private val indexed = rules.filter { it.hostKey != null }.groupBy { it.hostKey!! }

		private fun candidates(url: String): List<Network> =
			buildList {
				addAll(generic)
				var host = host(url)
				while (host.isNotEmpty()) {
					indexed[host]?.let(::addAll)
					host = host.substringAfter('.', "")
				}
			}

		fun blocks(
			url: String,
			page: String,
			type: String,
		): Boolean {
			if (candidates(page).any { it.exception && "document" in it.types && it.matches(page, page, "document") }) {
				return false
			}
			val matches = candidates(url).filter { it.switches.isEmpty() && it.matches(url, page, type) }
			return matches.any { !it.exception && it.important } || (matches.any { !it.exception } && matches.none { it.exception })
		}

		fun cosmeticsFor(url: String): List<Cosmetic> {
			val exceptions = candidates(url).filter { it.exception && it.matches(url, url, "document") }
			if (exceptions.any { "document" in it.types || "elemhide" in it.switches }) {
				return emptyList()
			}
			val matched = cosmetics.filter { domainsMatch(it.domains, host(url)) }
			return matched.filterNot { rule ->
				rule.exception ||
					(rule.domains.isEmpty() && exceptions.any { "generichide" in it.switches }) ||
					(rule.domains.isNotEmpty() && exceptions.any { "specifichide" in it.switches }) ||
					matched.any { it.exception && it.operator.replace("@", "") == rule.operator && (it.body.isEmpty() || it.body == rule.body) }
			}
		}
	}
}

/** Public Suffix List matching keeps third-party rules correct for sites such as example.co.uk. */
object SiteDomains {
	private val suffixes by lazy {
		SiteDomains::class.java.getResourceAsStream("/public_suffix_list.dat")!!.bufferedReader().useLines { lines ->
			lines
				.map(String::trim)
				.filter { it.isNotEmpty() && !it.startsWith("//") }
				.map { value ->
					value.split('.').joinToString(".") { label ->
						if (label == "*" || label.startsWith('!')) {
							label
						} else {
							runCatching { IDN.toASCII(label) }.getOrDefault(label)
						}
					}
				}.toSet()
		}
	}

	fun site(host: String): String {
		val labels = host.split('.')
		if (labels.size < 2 || host.all { it.isDigit() || it == '.' }) {
			return host
		}
		var length = 1
		for (index in labels.indices) {
			val value = labels.drop(index).joinToString(".")
			if ("!$value" in suffixes) {
				return labels.takeLast(labels.size - index).joinToString(".")
			}
			if (value in suffixes) {
				length = maxOf(length, labels.size - index)
			}
			if (index > 0 && "*.$value" in suffixes) {
				length = maxOf(length, labels.size - index + 1)
			}
		}
		return labels.takeLast(minOf(labels.size, length + 1)).joinToString(".")
	}
}
