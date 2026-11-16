package io.github.ulysseszh.pwainstaller

import io.github.ulysseszh.pwainstaller.core.CssColors
import org.jsoup.nodes.Document
import java.util.Locale

/** Discover static light/dark defaults without loading a page in WebView. */
internal object PageThemeColors {
	data class Colors(
		val theme: String?,
		val darkTheme: String?,
	)

	fun read(document: Document): Colors {
		val candidates =
			document.select("meta[name][content]").filter {
				it.attr("name").equals("theme-color", true) && CssColors.parse(it.attr("content")) != null
			}
		val light = candidates.firstOrNull { matches(it.attr("media"), false) == true }
		val dark = candidates.firstOrNull { matches(it.attr("media"), true) == true }
		return Colors(light?.attr("content")?.trim(), dark?.takeIf { it !== light }?.attr("content")?.trim())
	}

	// Only color-scheme and screen media are known during discovery. Unknown
	// viewport/device conditions stay unknown, including under negation.
	private fun matches(
		media: String,
		dark: Boolean,
	): Boolean? {
		if (media.length > 4096) {
			return null
		}
		val query = media.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
		return if (query.isEmpty()) {
			true
		} else {
			evaluate(query, dark, 0)
		}
	}

	private fun evaluate(
		query: String,
		dark: Boolean,
		depth: Int,
	): Boolean? {
		if (depth > 16 || query.isEmpty()) {
			return null
		}

		fun combine(
			parts: List<String>,
			any: Boolean,
		): Boolean? {
			val values = parts.map { evaluate(it, dark, depth + 1) }
			return if (any && true in values) {
				true
			} else if (!any && false in values) {
				false
			} else if (null in values) {
				null
			} else {
				any.not()
			}
		}
		val commas = split(query, ",") ?: return null
		if (commas.size > 1) {
			return combine(commas, true)
		}
		val type = Regex("(?:(not|only) )?(all|screen|print)(?: and (.+))?").matchEntire(query)
		if (type != null) {
			val condition = type.groupValues[3].takeIf { it.isNotEmpty() }?.let { evaluate(it, dark, depth + 1) ?: return null } ?: true
			val result = type.groupValues[2] != "print" && condition
			return if (type.groupValues[1] == "not") {
				!result
			} else {
				result
			}
		}
		val ors = split(query, " or ") ?: return null
		if (ors.size > 1) {
			return combine(ors, true)
		}
		val ands = split(query, " and ") ?: return null
		if (ands.size > 1) {
			return combine(ands, false)
		}
		if (query.startsWith("not ")) {
			return evaluate(query.removePrefix("not "), dark, depth + 1)?.not()
		}
		val feature = Regex("\\(\\s*prefers-color-scheme\\s*:\\s*(light|dark)\\s*\\)").matchEntire(query)
		if (feature != null) {
			return (feature.groupValues[1] == "dark") == dark
		}
		return if (query.startsWith('(') && query.endsWith(')')) {
			evaluate(query.substring(1, query.lastIndex).trim(), dark, depth + 1)
		} else {
			null
		}
	}

	private fun split(
		query: String,
		separator: String,
	): List<String>? {
		val parts = mutableListOf<String>()
		var depth = 0
		var start = 0
		var index = 0
		while (index < query.length) {
			when (query[index]) {
				'(' -> {
					if (++depth > 16) {
						return null
					}
				}

				')' -> {
					if (--depth < 0) {
						return null
					}
				}
			}
			if (depth == 0 && query.startsWith(separator, index)) {
				parts.add(query.substring(start, index).trim())
				index += separator.length
				start = index
			} else {
				index++
			}
		}
		if (depth != 0) {
			return null
		}
		return (parts + query.substring(start).trim()).takeIf { it.size <= 64 }
	}
}
