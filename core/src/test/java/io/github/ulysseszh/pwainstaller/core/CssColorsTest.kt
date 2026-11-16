package io.github.ulysseszh.pwainstaller.core
import org.junit.Assert.*
import org.junit.Test

class CssColorsTest {
	@Test fun launchColorsCompositeCssAlphaOverWhite() {
		assertEquals(-1, CssColors.opaque(0))
		assertEquals(0xff123456.toInt(), CssColors.opaque(0xff123456.toInt()))
		assertEquals(0xff808080.toInt(), CssColors.opaque(0x7f000000))
		assertEquals(0xffff7f7f.toInt(), CssColors.opaque(0x80ff0000.toInt()))
	}

	@Test fun parsesCssHexAlphaAndNamedColors() {
		assertEquals(0xffaabbcc.toInt(), CssColors.parse("#abc"))
		assertEquals(0x44aabbcc, CssColors.parse("#abc4"))
		assertEquals(0x44aabbcc, CssColors.parse("#aabbcc44"))
		assertEquals(0xff008000.toInt(), CssColors.parse("green"))
		assertEquals(0xfff0f8ff.toInt(), CssColors.parse("AliceBlue"))
		assertEquals(0, CssColors.parse("transparent"))
	}

	@Test fun parsesRgbHslAndPercentages() {
		assertEquals(0x80ff0000.toInt(), CssColors.parse("rgba(100%, 0%, 0%, 50%)"))
		assertEquals(0xff00ff00.toInt(), CssColors.parse("hsl(120, 100%, 50%)"))
		assertEquals(0xff0000ff.toInt(), CssColors.parse("rgb(0 0 255)"))
		assertNull(CssColors.parse("not-a-color"))
	}

	@Test fun foregroundKeepsTextReadableOnDarkMidtoneAndSaturatedBackgrounds() {
		listOf("#000000", "#161616", "#0000ff").forEach { assertEquals(it, -1, CssColors.foreground(CssColors.parse(it)!!)) }
		listOf("#ffffff", "#808080", "#00ff00").forEach { assertEquals(it, 0xff000000.toInt(), CssColors.foreground(CssColors.parse(it)!!)) }
	}
}
