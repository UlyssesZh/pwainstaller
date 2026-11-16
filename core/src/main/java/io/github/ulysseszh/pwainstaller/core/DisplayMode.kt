package io.github.ulysseszh.pwainstaller.core

/** Translate the shell's display mode into media features understood by system WebView. */
object DisplayMode {
	private val supported = setOf("standalone", "minimal-ui", "browser", "fullscreen")

	/** Select one mode for APK packaging, native chrome and CSS media queries. */
	fun resolve(
		display: String,
		overrides: List<String> = emptyList(),
	): String = overrides.firstOrNull { it in supported } ?: display.takeIf { it in supported } ?: "standalone"

	private val feature = Regex("\\(\\s*display-mode\\s*(?::\\s*([a-z-]+)\\s*)?\\)", RegexOption.IGNORE_CASE)

	fun query(
		text: String,
		mode: String,
	): String = rewrite(text, mode, false)

	fun stylesheet(
		text: String,
		mode: String,
	): String = rewrite(text, mode, true)

	private fun rewrite(
		text: String,
		mode: String,
		css: Boolean,
	): String {
		val output = StringBuilder(text.length)
		var index = 0
		var header = !css
		while (index < text.length) {
			val char = text[index]
			if (char == '/' && text.getOrNull(index + 1) == '*') {
				val end =
					text.indexOf("*/", index + 2).let {
						if (it < 0) {
							text.length
						} else {
							it + 2
						}
					}
				output.append(text, index, end)
				index = end
				continue
			}
			if (char == '\'' || char == '"') {
				val start = index++
				while (index < text.length) {
					val next = text[index++]
					if (next == '\\' && index < text.length) {
						index++
					} else if (next == char) {
						break
					}
				}
				output.append(text, start, index)
				continue
			}
			if (char == '\\' && index + 1 < text.length) {
				output.append(text, index, index + 2)
				index += 2
				continue
			}
			if (css && char == '@') {
				var end = index + 1
				while (end < text.length && (text[end].isLetter() || text[end] == '-')) {
					end++
				}
				val name = text.substring(index + 1, end).lowercase()
				if (name == "media" || name == "import") {
					header = true
				}
			}
			if (header && char == '(') {
				val match = feature.find(text, index)?.takeIf { it.range.first == index }
				if (match != null) {
					val requested = match.groupValues[1]
					output.append(
						if (requested.isEmpty() || requested.equals(mode, true)) {
							"(min-width: 0px)"
						} else {
							"(-pwa-display-mode: none)"
						},
					)
					index = match.range.last + 1
					continue
				}
			}
			if (css && (char == '{' || char == ';')) {
				header = false
			}
			output.append(char)
			index++
		}
		return output.toString()
	}
}
