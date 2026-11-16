package io.github.ulysseszh.pwainstaller

import io.github.ulysseszh.pwainstaller.core.AdblockRules
import org.json.JSONArray
import org.json.JSONObject

/** Build only the helpers and scriptlet implementations required by this web app's snapshot. */
object AdblockScript {
	fun arguments(body: String): List<String>? {
		val value = body.trim().removeSuffix(";")
		if (!value.startsWith("//scriptlet(") || !value.endsWith(')')) {
			return null
		}
		val text = value.removePrefix("//scriptlet(").dropLast(1)
		val result = mutableListOf<String>()
		var index = 0
		while (index < text.length) {
			while (index < text.length && text[index].isWhitespace()) {
				index++
			}
			if (index == text.length) {
				break
			}
			val quote = text[index++]
			if (quote != '\'' && quote != '"') {
				return null
			}
			val argument = StringBuilder()
			var closed = false
			while (index < text.length) {
				val char = text[index++]
				if (char == quote) {
					closed = true
					break
				}
				if (char == '\\' && index < text.length) {
					val escaped = text[index++]
					argument.append(
						when (escaped) {
							'n' -> {
								'\n'
							}

							'r' -> {
								'\r'
							}

							't' -> {
								'\t'
							}

							else -> {
								escaped
							}
						},
					)
				} else {
					argument.append(char)
				}
			}
			if (!closed) {
				return null
			}
			result.add(argument.toString())
			while (index < text.length && text[index].isWhitespace()) {
				index++
			}
			if (index < text.length && text[index++] != ',') {
				return null
			}
		}
		return result
	}

	// Bump when the generated representation or compiler semantics change.
	const val VERSION = 2
	private val extendedSelector = Regex(":(?:has|contains|matches-|remove|upward|nth-ancestor|xpath)\\b")
	private val quotedPattern = Regex("\\\\Q(.*?)\\\\E")
	private val workers by lazy {
		java.util.concurrent.Executors
			.newFixedThreadPool(2)
	}

	private data class Output(
		val value: JSONObject,
		val script: Boolean = false,
		val name: String? = null,
		val function: String? = null,
		val extended: Boolean = false,
	)

	fun compile(
		text: String,
		asset: (String) -> String,
	): Map<String, ByteArray> = compile(text, {}, asset)

	fun compile(
		text: String,
		progress: (Int) -> Unit,
		asset: (String) -> String,
	): Map<String, ByteArray> = compile(text, Runtime.getRuntime().availableProcessors().coerceIn(1, 2), progress, asset)

	/** workers is also exposed internally so device benchmarks can compare identical output. */
	internal fun compile(
		text: String,
		workerCount: Int,
		progress: (Int) -> Unit,
		asset: (String) -> String,
	): Map<String, ByteArray> {
		var lastPercent = -1
		val progressLock = Any()

		fun report(percent: Int) =
			synchronized(progressLock) {
				if (percent > lastPercent) {
					lastPercent = percent
					progress(percent)
				}
			}
		report(0)
		val lines = AdblockRules.lines(text)
		report(5)
		val rules =
			lines.mapIndexedNotNull { index, line ->
				val rule = AdblockRules.cosmetic(line)
				report(5 + (index + 1) * 20 / lines.size.coerceAtLeast(1))
				rule
			}
		if (rules.none { !it.exception }) {
			report(100)
			return emptyMap()
		}
		val css = JSONArray()
		val scripts = JSONArray()
		val functions = linkedMapOf<String, String>()
		var extended = false
		val exceptionsByOperator =
			rules
				.withIndex()
				.filter { it.value.exception }
				.groupBy { it.value.operator.replace("@", "") }
		val cssExceptions = exceptionsByOperator.mapValues { (_, exceptions) -> exceptions.groupBy { it.value.body } }
		val scriptExceptions =
			exceptionsByOperator["#%#"]
				.orEmpty()
				.mapNotNull { exception ->
					arguments(exception.value.body)?.let { it to exception.value.domains }
				}.groupBy { it.first.firstOrNull() }
		val blocking = rules.filterNot { it.exception }
		// Resolve aliases once instead of walking the complete vendor library for each rule.
		val definitions = mutableMapOf<String, String>()
		if (blocking.any { it.operator == "#%#" }) {
			val library = JSONObject(asset("adguard/scriptlets.json")).getJSONArray("scriptlets")
			repeat(library.length()) { index ->
				val definition = library.getJSONObject(index)
				val names = definition.getJSONArray("names")
				repeat(names.length()) { alias ->
					val name = names.getString(alias)
					if (!definitions.containsKey(name)) {
						definitions[name] = definition.getString("scriptlet")
					}
				}
			}
		}
		report(30)

		fun compileRule(rule: AdblockRules.Cosmetic): Output? {
			if (rule.operator == "#%#") {
				val args = arguments(rule.body)?.takeIf { it.isNotEmpty() } ?: return null
				val name = args.first()
				val definition = definitions[name] ?: return null
				val excluded =
					scriptExceptions[name]
						.orEmpty()
						.filter { (other, _) ->
							other.size <= args.size && other.indices.all { other[it] == args[it] }
						}.map { it.second }
				return Output(
					JSONObject()
						.put("domains", rule.domains)
						.put("name", name)
						.put("args", JSONArray(args.drop(1)))
						.put("exceptions", JSONArray(excluded)),
					true,
					name,
					definition,
				)
			}
			val byBody = cssExceptions[rule.operator].orEmpty()
			val exceptions = byBody[rule.body].orEmpty()
			val excluded =
				if (rule.body.isEmpty() || byBody[""].isNullOrEmpty()) {
					exceptions
				} else {
					(exceptions + byBody[""].orEmpty()).sortedBy { it.index }
				}
			val isStyle = '$' in rule.operator
			val needsExtended = '?' in rule.operator || extendedSelector.containsMatchIn(rule.body)
			return Output(
				JSONObject()
					.put("domains", rule.domains)
					.put("selector", rule.body)
					.put("style", isStyle)
					.put("extended", needsExtended)
					.put(
						"css",
						if (isStyle) {
							rule.body
						} else {
							"${rule.body} { display: none !important; }"
						},
					).put("exceptions", JSONArray(excluded.map { it.value.domains })),
				extended = needsExtended,
			)
		}
		val completed =
			java.util.concurrent.atomic
				.AtomicInteger()

		fun batch(
			from: Int,
			until: Int,
		): List<Output> =
			(from until until).mapNotNull { index ->
				val output = compileRule(blocking[index])
				report(30 + completed.incrementAndGet() * 55 / blocking.size)
				output
			}
		val outputs =
			if (workerCount > 1 && blocking.size >= 1024) {
				val middle = blocking.size / 2
				val pending = workers.submit(java.util.concurrent.Callable { batch(middle, blocking.size) })
				try {
					batch(0, middle) + pending.get()
				} finally {
					if (!pending.isDone) {
						pending.cancel(true)
					}
				}
			} else {
				batch(0, blocking.size)
			}
		for (output in outputs) {
			if (output.script) {
				scripts.put(output.value)
				functions[requireNotNull(output.name)] = requireNotNull(output.function)
			} else {
				css.put(output.value)
				extended = extended || output.extended
			}
		}
		report(90)
		if (css.length() == 0 && scripts.length() == 0) {
			report(100)
			return emptyMap()
		}
		val libraryNeeded = extended || functions.isNotEmpty()
		val documentExceptions = JSONArray()
		lines
			.asSequence()
			.filter { it.startsWith("@@") }
			.mapNotNull(AdblockRules::network)
			.filter {
				it.exception && ("document" in it.types || it.switches.isNotEmpty())
			}.forEach { rule ->
				val pattern =
					quotedPattern.replace(rule.regex.pattern) { match ->
						match.groupValues[1]
							.map { char ->
								if (char in "^$\\.*+?()[]{}|") {
									"\\$char"
								} else {
									char.toString()
								}
							}.joinToString("")
					}
				documentExceptions.put(
					JSONObject()
						.put("pattern", pattern)
						.put("anchor", rule.domainAnchor)
						.put("domains", rule.domains.replace('|', ','))
						.put(
							"flags",
							if (RegexOption.IGNORE_CASE in rule.regex.options) {
								"i"
							} else {
								""
							},
						).put(
							"switches",
							JSONArray(
								if ("document" in rule.types) {
									listOf("elemhide", "jsinject")
								} else {
									rule.switches.toList()
								},
							),
						),
				)
			}
		report(95)
		val runner =
			asset(
				"adguard/runner.js",
			).replace("__PWA_DOCUMENT_EXCEPTIONS__", documentExceptions.toString())
				.replace("__PWA_COSMETIC_RULES__", css.toString())
				.replace("__PWA_SCRIPTLET_RULES__", scripts.toString())
				.replace(
					"__PWA_SCRIPTLET_FUNCTIONS__",
					functions.entries.joinToString(",\n", "var scriptlets = {\n", "\n};") { (name, code) -> "${JSONObject.quote(name)}: $code" },
				)
		val libraries =
			buildString {
				if (libraryNeeded) {
					append(asset("adguard/polyfills.js")).append('\n')
				}
				if (extended) {
					append(asset("adguard/extended-css.js")).append('\n')
				}
			}
		// Old WebViews may retain JS globals injected into their initial about:blank page.
		val script =
			"""
			(function () {
				if (!/^https?:${'$'}/.test(location.protocol) || window.__pwaFilterApplied === location.href) {
					return;
				}
				try {
					$libraries
					$runner
				} catch (error) {
					console.warn('PWA filter initialization failed: ' + error);
				}
			})();
			""".trimIndent()
		val result =
			linkedMapOf("assets/adblock.js" to script.toByteArray()).apply {
				if (libraryNeeded) {
					put("assets/licenses/adguard.txt", asset("adguard/ADGUARD-LICENSE").toByteArray())
					put("assets/licenses/core-js.txt", asset("adguard/CORE-JS-LICENSE").toByteArray())
					put("assets/licenses/NOTICE.txt", asset("adguard/NOTICE.txt").toByteArray())
				}
			}
		report(100)
		return result
	}
}
