package io.github.ulysseszh.pwainstaller.core

import org.junit.Assert.*
import org.junit.Test

class DisplayModeTest {
	@Test fun resolvesSupportedModesAndOverridesConsistently() {
		listOf("standalone", "minimal-ui", "browser", "fullscreen").forEach { mode ->
			assertEquals(mode, DisplayMode.resolve(mode))
		}
		listOf("", "unknown", "window-controls-overlay").forEach { assertEquals("standalone", DisplayMode.resolve(it)) }
		assertEquals("browser", DisplayMode.resolve("fullscreen", listOf("unknown", "browser", "standalone")))
		assertEquals("minimal-ui", DisplayMode.resolve("minimal-ui", listOf("unknown")))
	}
	@Test fun eachModeMatchesOnlyItsOwnMediaFeature() {
		listOf("standalone", "minimal-ui", "browser", "fullscreen").forEach { mode ->
			listOf("standalone", "minimal-ui", "browser", "fullscreen").forEach { requested ->
				val expected = if (requested == mode) "(min-width: 0px)" else "(-pwa-display-mode: none)"
				assertEquals(expected, DisplayMode.query("(display-mode: $requested)", mode))
				assertEquals("@media $expected { a { color: red; } }",
					DisplayMode.stylesheet("@media (display-mode: $requested) { a { color: red; } }", mode))
			}
		}
	}

	@Test fun changesOnlyDisplayFeaturesAndPreservesBooleanComposition() {
		assertEquals("screen and (min-width: 0px) and (min-width: 600px)", DisplayMode.query("screen and (display-mode: standalone) and (min-width: 600px)", "standalone"))
		assertEquals("not all and (-pwa-display-mode: none)", DisplayMode.query("not all and (display-mode: browser)", "standalone"))
		assertEquals("(min-width: 0px)", DisplayMode.query("(DISPLAY-MODE: FULLSCREEN)", "fullscreen"))
	}
	@Test fun rewritesMediaAndImportConditionsButPreservesCommentsStringsAndOtherRules() {
		val input = """/* @media (display-mode: browser) */
			@import "https://example.com/(display-mode: browser).css" (display-mode: standalone);
			@media screen and (display-mode: standalone) { .x { content: "(display-mode: browser)"; } }
			@supports (display-mode: browser) { .x { color: red; } }
		""".trimIndent()
		val result = DisplayMode.stylesheet(input, "standalone")
		assertTrue(result.contains("/* @media (display-mode: browser) */"))
		assertTrue(result.contains("\"https://example.com/(display-mode: browser).css\" (min-width: 0px)"))
		assertTrue(result.contains("@media screen and (min-width: 0px)"))
		assertTrue(result.contains("content: \"(display-mode: browser)\""))
		assertTrue(result.contains("@supports (display-mode: browser)"))
	}
}
