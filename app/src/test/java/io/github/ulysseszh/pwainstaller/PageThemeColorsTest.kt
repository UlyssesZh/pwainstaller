package io.github.ulysseszh.pwainstaller

import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class PageThemeColorsTest {
	private fun colors(html: String) = PageThemeColors.read(Jsoup.parse(html))

	@Test fun selectsTheFirstValidMatchingColorForEachScheme() {
		val result = colors("""
			<meta name="theme-color" content="invalid">
			<meta name="theme-color" media="print" content="red">
			<meta name="theme-color" media="(min-width: 1px)" content="red">
			<meta NAME="THEME-COLOR" media="only screen and (prefers-color-scheme: dark)" content=" #010203 ">
			<meta name="theme-color" media="(prefers-color-scheme: light)" content="rgb(4, 5, 6)">
			<meta name="theme-color" content="blue">
		""")
		assertEquals("rgb(4, 5, 6)", result.theme)
		assertEquals("#010203", result.darkTheme)
	}

	@Test fun anUnconditionalColorIsTheFallbackForBothSchemes() {
		val result = colors("""<meta name=theme-color content=#abcdef><meta name=theme-color media="(prefers-color-scheme: dark)" content=black>""")
		assertEquals("#abcdef", result.theme)
		assertNull(result.darkTheme)
	}

	@Test fun handlesDarkOnlyTagsAndBooleanColorSchemeQueries() {
		assertEquals(PageThemeColors.Colors(null, "black"), colors("""<meta name=theme-color media="not (prefers-color-scheme: light)" content=black>"""))
		assertEquals(PageThemeColors.Colors("red", "black"), colors("""
			<meta name=theme-color media="not screen and (prefers-color-scheme: dark)" content=red>
			<meta name=theme-color media="screen and (prefers-color-scheme: dark)" content=black>
		"""))
		assertEquals(PageThemeColors.Colors("white", null), colors("""
			<meta name=theme-color media="screen and ((prefers-color-scheme: light) or (prefers-color-scheme: dark))" content=white>
		"""))
		assertEquals(PageThemeColors.Colors("white", null), colors("""
			<meta name=theme-color media="(prefers-color-scheme: light), (prefers-color-scheme: dark)" content=white>
		"""))
	}

	@Test fun doesNotGuessUnknownConditionsOrFailOnExcessiveNesting() {
		val deep = "(".repeat(64) + "prefers-color-scheme: dark" + ")".repeat(64)
		val result = colors("""
			<meta name=theme-color media="not (min-width: 1px)" content=red>
			<meta name=theme-color media="$deep" content=red>
			<meta name=theme-color content="transparent">
		""")
		assertEquals("transparent", result.theme)
		assertNull(result.darkTheme)
		assertEquals(PageThemeColors.Colors(null, null), colors("<meta name=color-scheme content=dark>"))
	}
}
