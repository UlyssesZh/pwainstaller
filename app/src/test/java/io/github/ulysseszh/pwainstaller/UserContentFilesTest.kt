package io.github.ulysseszh.pwainstaller

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class UserContentFilesTest {
    private fun entry(kind: UserContentFiles.Kind, code: String) = UserContentFiles.Entry(UUID.randomUUID().toString(), kind, "Fixture", code)
    @Test fun enabledFilesRoundTripIndependentlyOfIconAndManifestBudgets() {
        val files = listOf(entry(UserContentFiles.Kind.SCRIPT, "/*" + "x".repeat(800_000) + "*/"),
            entry(UserContentFiles.Kind.STYLE, "/*" + "y".repeat(800_000) + "*/"))
        val app = WebApp().apply { set("baseUrl", "https://example.com/"); UserContentFiles.put(data, files); validate() }
        assertEquals(files, UserContentFiles.read(JSONObject(app.data.toString())))
        assertTrue(app.data.toString().toByteArray().size > 1536 * 1024)
        assertTrue(app.metadataForStorage().toByteArray().size < 1536 * 1024)
        assertEquals(0, JSONObject(app.metadataForStorage()).getJSONArray("userScripts").length())
    }
    @Test fun rejectsBinaryOversizedAndDuplicateFiles() {
        assertEquals("hello", UserContentFiles.decode(byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()) + "hello".toByteArray()))
        assertThrows(Exception::class.java) { UserContentFiles.decode(byteArrayOf(0xff.toByte())) }
        assertThrows(IllegalArgumentException::class.java) { UserContentFiles.decode("binary\u0000".toByteArray()) }
        assertThrows(IllegalArgumentException::class.java) { entry(UserContentFiles.Kind.SCRIPT, "x".repeat(UserContentFiles.MAX_FILE_BYTES + 1)).validate() }
        val file = entry(UserContentFiles.Kind.STYLE, "body{color:red}")
        val data = JSONObject(); UserContentFiles.put(data, listOf(file, file))
        assertThrows(IllegalArgumentException::class.java) { UserContentFiles.validate(data) }
    }
    @Test fun stylesQuoteArbitraryCssAndHaveIndependentDocumentGuards() {
        val style = entry(UserContentFiles.Kind.STYLE, "a:after{content:'</script>\\\"\u2028'}")
        val code = UserContentFiles.script(style)
        assertTrue(code.contains("\\u2028")); assertFalse(code.contains('\u2028'))
        assertTrue(code.contains(UserContentFiles.marker(style)))
        assertTrue(code.contains("DOMContentLoaded")); assertTrue(code.contains("window.top!==window"))
        val script = entry(UserContentFiles.Kind.SCRIPT, "throw new Error('broken')")
        assertNotEquals(UserContentFiles.marker(style), UserContentFiles.marker(script))
        assertTrue(UserContentFiles.script(script).contains("console.error"))
    }
}
