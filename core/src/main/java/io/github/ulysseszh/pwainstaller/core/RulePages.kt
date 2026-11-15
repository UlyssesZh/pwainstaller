package io.github.ulysseszh.pwainstaller.core

/** Bound editable layout work while retaining the exact text between pages. */
object RulePages {
	const val CHARACTERS = 8192
	private const val LINES = 128
	fun split(text: String): List<String> {
		if (text.isEmpty()) return listOf("")
		val pages = mutableListOf<String>()
		var start = 0
		while (start < text.length) {
			var end = minOf(start + CHARACTERS, text.length)
			if (end < text.length && text[end - 1].isHighSurrogate() && text[end].isLowSurrogate()) end--
			var lines = 0
			var lastBreak = -1
			for (index in start until end) if (text[index] == '\n') {
				lastBreak = index + 1
				if (++lines == LINES) { end = lastBreak; break }
			}
			if (end < text.length && lastBreak > start) end = lastBreak
			pages.add(text.substring(start, end)); start = end
		}
		return pages
	}
}
