package io.github.ulysseszh.pwainstaller.core

/** Full-line comments disable sources without losing their URLs in settings or backups. */
object FilterSources {
	const val DEFAULT = "# https://filters.adtidy.org/android/filters/2_optimized.txt\n" +
		"https://filters.adtidy.org/android/filters/18.txt\n" +
		"https://filters.adtidy.org/android/filters/20.txt"

	fun enabled(text: String): List<String> = text.lineSequence().map(String::trim)
		.filter { it.isNotEmpty() && !it.startsWith('#') }.map(PackageNames::webUrl).distinct().toList().also {
			require(it.size <= 16) { "At most 16 enabled filter URLs are allowed" }
		}

	fun normalize(text: String): String {
		enabled(text) // Validate active URLs before persisting anything.
		val seen = mutableSetOf<String>()
		return text.lineSequence().map(String::trim).filter(String::isNotEmpty).mapNotNull {
			if (it.startsWith('#')) it else PackageNames.webUrl(it).takeIf(seen::add)
		}.joinToString("\n")
	}
}
