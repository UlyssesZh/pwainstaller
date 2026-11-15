package io.github.ulysseszh.pwainstaller.core

import org.junit.Assert.*
import org.junit.Test

class WebAppColorsTest {
	@Test fun lightModeKeepsRegularColorsAndDarkModeUsesOverrides() {
		val args = listOf("#123456", "#abcdef", "#654321", "#fedcba")
		assertEquals(WebAppColors(CssColors.parse(args[0]), CssColors.parse(args[1])!!), WebAppColors.resolve(args[0], args[1], args[2], args[3]))
		assertEquals(WebAppColors(CssColors.parse(args[2]), CssColors.parse(args[3])!!), WebAppColors.resolve(args[0], args[1], args[2], args[3], true))
	}
	@Test fun blankOrInvalidNightOverridesReuseTheRegularColors() {
		val regular = WebAppColors.resolve("purple", "white")
		assertEquals(regular, WebAppColors.resolve("purple", "white", dark = true))
		assertEquals(regular, WebAppColors.resolve("purple", "white", "invalid", "invalid", true))
		assertEquals(WebAppColors(null, -1), WebAppColors.resolve("", "", dark = true))
	}
	@Test fun colorsAreOpaqueAndEachOverrideIsIndependent() {
		val colors = WebAppColors.resolve("#123456", "#abcdef", "#0008", dark = true)
		assertEquals(CssColors.opaque(CssColors.parse("#0008")!!), colors.theme)
		assertEquals(CssColors.parse("#abcdef"), colors.background)
	}
}
