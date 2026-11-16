package io.github.ulysseszh.pwainstaller.core

import java.net.URI

/** One HTTP(S) scope URL or wildcard pattern per line; any matching entry stays in the app. */
object UrlRules {
	fun defaultScopes(url: String): String {
		val scope = URI(PackageNames.webUrl(url))
		val host = scope.host.lowercase()
		val bare =
			'.' in host && host.any { it.isLetter() } && !host.startsWith("www.") && SiteDomains.site(host) == host &&
				SiteDomains.site("www.$host") == host
		val www =
			if (bare) {
				URI(scope.scheme, scope.userInfo, "www.$host", scope.port, scope.path, scope.query, null).toString()
			} else {
				null
			}
		return listOfNotNull(scope.toString(), www).joinToString("\n")
	}

	private data class Rule(
		val scheme: String,
		val host: String,
		val port: String?,
		val path: String,
		val query: String?,
		val scope: URI?,
	)

	private fun parse(text: String): List<Rule> {
		val lines =
			text
				.lineSequence()
				.map(String::trim)
				.filter(String::isNotEmpty)
				.toList()
		require(lines.size <= 64) { "Too many URL patterns" }
		return lines.map { line ->
			require(line.length <= 2048 && line.none { it.isWhitespace() || it == '#' }) { "Invalid URL pattern" }
			val match =
				requireNotNull(Regex("(https?|\\*)://(\\[[0-9a-fA-F:]+]|\\*|(?:\\*\\.)?[a-zA-Z0-9.-]+)(?::([0-9]+|\\*))?(/[^#]*)?").matchEntire(line)) {
					"Use HTTP(S) URL patterns such as https://example.com/*"
				}
			val port = match.groupValues[3].takeIf(String::isNotEmpty)
			require(port == null || port == "*" || port.toIntOrNull() in 1..65535) { "Invalid pattern port" }
			val path = match.groupValues[4].ifEmpty { "/*" }
			val scope =
				if ('*' !in line && '?' !in line) {
					URI(PackageNames.webUrl(line))
				} else {
					null
				}
			Rule(
				match.groupValues[1],
				match.groupValues[2].lowercase(),
				port,
				path.substringBefore('?'),
				path.substringAfter('?', "").takeIf {
					'?' in
						path
				},
				scope,
			)
		}
	}

	fun validate(text: String) {
		parse(text)
	}

	/** One filter per authority avoids Android mixing unrelated hosts, ports and paths. */
	fun androidFilters(
		baseUrl: String,
		patterns: String,
	): List<BinaryXml.IntentFilter> {
		val rules = parse(patterns).ifEmpty { parse(defaultScopes(baseUrl)) }
		return rules
			.flatMap { rule ->
				val schemes =
					if (rule.scheme == "*") {
						listOf("http", "https")
					} else {
						listOf(rule.scheme)
					}
				val hosts =
					if (rule.host.startsWith("*.")) {
						listOf(rule.host.drop(2), rule.host)
					} else {
						listOf(rule.host)
					}
				val paths =
					if (rule.scope != null) {
						val prefix = path(rule.scope).trimEnd('/')
						if (prefix.isEmpty()) {
							listOf("pathPrefix" to "/")
						} else {
							listOf("path" to path(rule.scope), "pathPrefix" to "$prefix/")
						}
					} else {
						listOf(
							"pathPattern" to
								buildString {
									rule.path.forEach { char ->
										when (char) {
											'*' -> {
												append(".*")
											}

											'.', '\\' -> {
												append('\\')
												append(char)
											}

											else -> {
												append(char)
											}
										}
									}
								},
						)
					}
				schemes.flatMap { scheme ->
					hosts.map { host ->
						val authority = linkedMapOf("scheme" to scheme, "host" to host)
						rule.port?.takeIf { it != "*" }?.let { authority["port"] = it }
						BinaryXml.IntentFilter(
							listOf("android.intent.action.VIEW"),
							categories = listOf("android.intent.category.DEFAULT", "android.intent.category.BROWSABLE"),
							data = paths.map { (key, value) -> authority + (key to value) },
						)
					}
				}
			}.distinct()
	}

	private fun path(uri: URI): String {
		var path = URI(null, null, uri.path.ifEmpty { "/" }, null).normalize().path
		while (path.startsWith("/../")) {
			path = path.removePrefix("/..")
		}
		return if (path == "/..") {
			"/"
		} else {
			path
		}
	}

	fun matches(
		url: String,
		baseUrl: String,
		patterns: String,
	): Boolean =
		runCatching {
			val uri = URI(PackageNames.webUrl(url))
			val rules = parse(patterns)
			if (rules.isEmpty()) {
				return defaultScopes(baseUrl).lineSequence().any { inScope(uri, URI(it)) }
			}
			rules.any { rule ->
				if (rule.scope != null) {
					return@any inScope(uri, rule.scope)
				}
				val host = uri.host.lowercase()
				val hostMatches =
					rule.host == "*" || rule.host == host || (
						rule.host.startsWith("*.") &&
							(host == rule.host.drop(2) || host.endsWith(rule.host.drop(1)))
					)
				val port =
					if (uri.port != -1) {
						uri.port
					} else if (uri.scheme == "https") {
						443
					} else {
						80
					}
				val expectedPort =
					rule.port?.toIntOrNull() ?: if (uri.scheme == "https") {
						443
					} else {
						80
					}
				(rule.scheme == "*" || rule.scheme == uri.scheme) && hostMatches &&
					(rule.port == "*" || port == expectedPort) && glob(rule.path, path(uri)) &&
					(rule.query == null || glob(rule.query, uri.rawQuery.orEmpty()))
			}
		}.getOrDefault(false)

	private fun inScope(
		uri: URI,
		scope: URI,
	): Boolean {
		val prefix = path(scope).trimEnd('/')
		return PackageNames.sameOrigin(uri.toString(), scope.toString()) &&
			(path(uri) == path(scope) || path(uri).startsWith("$prefix/"))
	}

	// Greedy glob matching avoids regular-expression backtracking on imported patterns.
	private fun glob(
		pattern: String,
		value: String,
	): Boolean {
		var p = 0
		var v = 0
		var star = -1
		var retry = 0
		while (v < value.length) {
			when {
				p < pattern.length && pattern[p] == '*' -> {
					star = p++
					retry = v
				}

				p < pattern.length && pattern[p] == value[v] -> {
					p++
					v++
				}

				star != -1 -> {
					p = star + 1
					v = ++retry
				}

				else -> {
					return false
				}
			}
		}
		while (p < pattern.length && pattern[p] == '*') {
			p++
		}
		return p == pattern.length
	}
}
