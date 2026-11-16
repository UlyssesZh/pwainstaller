package io.github.ulysseszh.pwainstaller

import org.json.JSONObject

/** Escaping a slash is optional in JSON; show URLs and paths as people normally write them. */
object ManifestText {
	fun format(manifest: JSONObject): String = readableSlashes(manifest.toString(2))

	fun readableSlashes(text: String): String =
		buildString {
			var index = 0
			while (index < text.length) {
				if (text[index] == '\\' && index + 1 < text.length) {
					// Consume an entire escape, preserving literal backslashes before a slash.
					if (text[index + 1] != '/') {
						append('\\')
					}
					append(text[index + 1])
					index += 2
				} else {
					append(text[index++])
				}
			}
		}
}
