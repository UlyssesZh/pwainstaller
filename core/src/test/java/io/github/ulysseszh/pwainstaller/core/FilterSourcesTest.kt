package io.github.ulysseszh.pwainstaller.core

import org.junit.Assert.*
import org.junit.Test

class FilterSourcesTest {
	@Test fun defaultsKeepTheBaseFilterDisabled() {
		assertTrue(FilterSources.DEFAULT.startsWith("# https://filters.adtidy.org/android/filters/2_optimized.txt\n"))
		assertEquals(
			listOf("https://filters.adtidy.org/android/filters/18.txt", "https://filters.adtidy.org/android/filters/20.txt"),
			FilterSources.enabled(FilterSources.DEFAULT),
		)
	}

	@Test fun commentsSurviveNormalizationAndFragmentsRemainPartOfTheUrl() {
		val text =
			"  # temporarily disabled\r\n# https://disabled.test/list\n" +
				" https://example.com/list#section \nhttps://example.com/list#section\n"
		assertEquals(listOf("https://example.com/list#section"), FilterSources.enabled(text))
		assertEquals("# temporarily disabled\n# https://disabled.test/list\nhttps://example.com/list#section", FilterSources.normalize(text))
		assertEquals(emptyList<String>(), FilterSources.enabled((1..20).joinToString("\n") { "# https://disabled.test/$it" }))
	}

	@Test fun enabledSourcesStillRequireValidUrlsAndRespectTheLimit() {
		for (text in listOf("# comment\ninvalid", (1..17).joinToString("\n") { "https://example.com/$it" })) {
			try {
				FilterSources.normalize(text)
				fail("Expected invalid source list")
			} catch (_: IllegalArgumentException) {
			}
		}
	}
}
