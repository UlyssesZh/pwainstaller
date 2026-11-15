package io.github.ulysseszh.pwainstaller

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AdblockScriptTest {
    private fun compile(rules: String) = AdblockScript.compile(rules) { File("src/main/assets/$it").readText() }
    @Test fun networkRulesNeedNoScriptAndSimpleCssNeedsNoVendorLibraries() {
        assertTrue(compile("||ads.example^").isEmpty())
        val assets = compile("example.com##.cookie")
        assertEquals(setOf("assets/adblock.js"), assets.keys)
        val script = assets.getValue("assets/adblock.js").toString(Charsets.UTF_8)
        assertTrue(script.contains(".cookie")); assertFalse(script.contains("EXTENDED_CSS_VERSION"))
        assertFalse(script.contains("__PWA_COSMETIC_RULES__"))
    }
    @Test fun bundlesOnlyRequestedScriptletsAndTheirLicenses() {
        val assets = compile("example.com#%#//scriptlet('set-cookie', 'consent', 'true')")
        assertTrue(assets.containsKey("assets/licenses/adguard.txt"))
        val script = assets.getValue("assets/adblock.js").toString(Charsets.UTF_8)
        assertTrue(script.contains("consent")); assertTrue(script.contains("function setCookie"))
        assertFalse(script.contains("function trustedClickElement"))
        assertFalse(script.contains("EXTENDED_CSS_VERSION"))
    }
    @Test fun parsesQuotedScriptletArgumentsAndConvertsDocumentExceptionPatterns() {
        assertEquals(listOf("remove-class", "a,b", "div[class=\"x\"]"), AdblockScript.arguments("""//scriptlet('remove-class', 'a,b', 'div[class="x"]')"""))
        assertNull(AdblockScript.arguments("//scriptlet('set-cookie', unquoted)"))
        val script = compile("##.cookie\n@@||example.com^\$document").getValue("assets/adblock.js").toString(Charsets.UTF_8)
        assertTrue(script.contains("pageExceptions"))
        assertFalse(script.contains("\\\\Q"))
    }

    @Test fun parallelOutputPreservesOrderingExceptionsAndCompilerProgress() {
        val rules = buildString {
            append("first.example#@#.banner\nsecond.example#@#\nthird.example#@#.banner\n")
            append("example.com#@%#//scriptlet('set-cookie', 'consent')\n")
            repeat(1200) { append("example.com##.banner$it\n") }
            append("example.com##.banner\nexample.com#%#//scriptlet('set-cookie', 'consent', 'true')")
        }
        val expected = AdblockScript.compile(rules, 1, {}) { File("src/main/assets/$it").readText() }
        val progress = mutableListOf<Int>()
        val parallel = AdblockScript.compile(rules, 2, progress::add) { File("src/main/assets/$it").readText() }
        assertEquals(expected.keys, parallel.keys)
        expected.forEach { (name, bytes) -> assertArrayEquals(bytes, parallel.getValue(name)) }
        assertEquals(0, progress.first()); assertEquals(100, progress.last())
        assertTrue(progress.zipWithNext().all { (a, b) -> a < b })
        val script = expected.getValue("assets/adblock.js").toString(Charsets.UTF_8)
        assertTrue(script.contains("[\"first.example\",\"second.example\",\"third.example\"]"))
    }
}
