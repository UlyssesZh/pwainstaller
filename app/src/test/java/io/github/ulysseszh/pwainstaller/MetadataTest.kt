package io.github.ulysseszh.pwainstaller

import io.github.ulysseszh.pwainstaller.core.PackageNames
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MetadataTest {
    @Test fun displayModeReplacesLegacyBrowserToolbarOption() {
        listOf(true, false).forEach { legacy ->
            listOf("", "standalone", "minimal-ui", "browser", "fullscreen").forEach { mode ->
                val source = app().data.put("browserUi", legacy)
                source.getJSONObject("manifest").put("display", mode)
                val restored = WebApp(source)
                assertFalse(restored.data.has("browserUi"))
                assertEquals(mode.ifEmpty { "standalone" }, restored.displayMode())
                restored.manifest().put("display_override", org.json.JSONArray(listOf("unsupported", "browser")))
                assertEquals("browser", restored.displayMode())
            }
        }
    }

    @Test fun darkColorsArePortableAppOptionsAndRemainOutsideTheManifest() {
        val app = app().apply { set("darkThemeColor", "#8bd5ca"); set("darkBackgroundColor", "#20242a"); validate() }
        val restored = WebApp(JSONObject(app.data.toString())).apply { validate() }
        assertEquals("#8bd5ca", restored.s("darkThemeColor")); assertEquals("#20242a", restored.s("darkBackgroundColor"))
        assertFalse(restored.manifest().has("darkThemeColor")); assertFalse(restored.manifest().has("darkBackgroundColor"))
        restored.set("darkThemeColor", "invalid")
        assertThrows(IllegalArgumentException::class.java) { restored.validate() }
    }

    @Test fun largeFilterSnapshotsHaveASeparateBudgetFromManifestAndIconMetadata() {
        val rules = ("||ads.example/" + "a".repeat(1000) + "^\n").repeat(2200)
        val app = app().apply { set("adblockRules", rules); validate() }
        assertTrue(app.data.toString().toByteArray().size > 2 * 1024 * 1024)
        assertTrue(app.metadataForStorage().toByteArray().size < 1536 * 1024)
        assertEquals(rules, WebApp(JSONObject(app.data.toString())).s("adblockRules"))
        app.manifest().put("oversized", "a".repeat(1536 * 1024))
        assertThrows(IllegalArgumentException::class.java) { app.validate() }
    }
    @Test fun scalableIconMetadataUsesAnyAndIsAvailableAtEveryResolution() {
        val app = WebApp().apply { set("baseUrl", "https://example.com/") }
        val raster = JSONObject().put("src", "upload:raster").put("data", testPng(48)).put("purpose", "any")
        val vector = JSONObject().put("src", "https://example.com/icon.svg").put("data", IconData.default).put("purpose", "any")
        app.icons().put(raster).put(vector); app.validate()
        assertEquals("any", vector.getString("sizes"))
        assertEquals("image/svg+xml", vector.getString("type"))
        assertSame(raster, IconData.bestEntry(app.icons(), 48, "any"))
        assertSame(vector, IconData.bestEntry(app.icons(), 4096, "any"))
        val restored = WebApp(JSONObject(app.data.toString())).apply { validate() }
        assertEquals("any", restored.icons().getJSONObject(1).getString("sizes"))
    }

    @Test fun notchAreasDefaultOnAndPersistAnExplicitOptOut() {
        assertTrue(WebApp().b("useNotchAreas"))
        val old = app().data.apply { remove("useNotchAreas") }
        val restored = WebApp(old)
        assertTrue(restored.data.getBoolean("useNotchAreas"))
        restored.set("useNotchAreas", false)
        assertFalse(WebApp(JSONObject(restored.data.toString())).b("useNotchAreas"))
    }
    private fun app() = WebApp().apply { set("baseUrl", "https://example.com/web/index"); set("manifestUrl", "https://example.com/manifests/app.json") }
    @Test fun applyingAManifestReplacesCustomAppScopesWithItsResolvedScope() {
        val app = app().apply {
            set("urlPatterns", "https://example.com/old/\nhttps://another.example/*")
            manifest().put("start_url", "../web/start").put("scope", "../web/")
        }
        app.applyManifestScope()
        assertEquals("https://example.com/web/\nhttps://www.example.com/web/", app.s("urlPatterns"))
        assertEquals(app.manifest().getString("scope"), app.s("urlPatterns").lineSequence().first())
        app.manifest().remove("scope")
        app.manifest().put("start_url", "../other/start")
        app.applyManifestScope()
        assertEquals("https://example.com/other/\nhttps://www.example.com/other/", app.s("urlPatterns"))
    }
    @Test fun resolvesManifestUrlsAndIdentityAgainstStartOrigin() {
        val app = app().apply { set("manifest", JSONObject("""{"name":"应用","start_url":"../web/start","id":"identity#fragment","scope":"../web/","custom":{"preserve":true}}""")) }
        app.validate()
        assertEquals("https://example.com/web/start", app.manifest().getString("start_url"))
        assertEquals("https://example.com/identity", app.manifest().getString("id"))
        assertEquals("https://example.com/identity", app.s("id"))
        assertEquals(app.s("id"), WebApp(JSONObject(app.data.toString())).s("id"))
        assertEquals("https://example.com/web/", app.manifest().getString("scope"))
        assertEquals("应用", app.name())
        assertTrue(app.manifest().getJSONObject("custom").getBoolean("preserve"))
        assertTrue(PackageNames.valid(app.s("packageName")))
    }
    @Test fun manifestUrlChangesIndependentlyOfResolvedMetadata() {
        val app = app().apply { validate() }
        val start = app.manifest().getString("start_url")
        val id = app.manifest().getString("id")
        val pkg = app.s("packageName")
        app.set("manifestUrl", "https://cdn.example.net/new.json")
        app.validate()
        assertEquals(start, app.manifest().getString("start_url")); assertEquals(id, app.manifest().getString("id")); assertEquals(pkg, app.s("packageName"))
    }
    @Test fun manualAppsReceiveDefaultsAndCopiesAreIndependent() {
        val app = app().apply { set("useManifest", false); validate() }
        val copy = app.copy().apply { manifest().put("name", "changed") }
        assertEquals("example.com", app.name()); assertEquals("changed", copy.name()); assertEquals("https://example.com/web/", app.manifest().getString("scope"))
    }
    @Test fun rejectsCrossOriginStartIdAndScope() {
        listOf("start_url", "id", "scope").forEach { key ->
            val app = app().apply { manifest().put(key, "https://attacker.example/") }
            assertThrows(IllegalArgumentException::class.java) { app.validate() }
        }
    }
    @Test fun headersValidateWithoutDiscardingColonsInValues() {
        val headers = ManifestFetcher.headers("Authorization: Bearer token\nX-Example: a:b\n\n")
        assertEquals("a:b", headers["X-Example"]); assertEquals("Bearer token", headers["Authorization"])
        listOf("missing-colon", "Invalid Name: value", "Host: attacker", "Content-Length: 9", "X: abc\rOther: value").forEach {
            assertThrows(IllegalArgumentException::class.java) { ManifestFetcher.headers(it) }
        }
    }
    @Test fun embedsDefaultIconAndUpgradesLegacyMetadataWithoutNetworkAccess() {
        val app = app().apply { validate() }
        assertTrue(IconData.decode(app.s("icon")).isNotEmpty())
        val restored = WebApp(JSONObject(app.data.toString()))
        assertEquals(app.s("icon"), restored.s("icon"))
        app.data.remove("icon")
        assertEquals(restored.s("icon"), WebApp(app.data).s("icon"))
        app.set("icon", "")
        app.validate()
        assertEquals(restored.s("icon"), app.s("icon"))
    }
    @Test fun rejectsCorruptedOrOversizedEmbeddedPngData() {
        val bytes = IconData.decode(IconData.default)
        bytes[40] = (bytes[40].toInt() xor 1).toByte()
        val encoded = java.util.Base64.getEncoder().encodeToString(bytes)
        assertThrows(IllegalArgumentException::class.java) { app().apply { set("icon", encoded); validate() } }
        assertThrows(IllegalArgumentException::class.java) { IconData.decode("A".repeat(4 * 1024 * 1024 + 1)) }
    }
    @Test fun webViewDebuggingIsOptInAndSurvivesMetadataRoundTrips() {
        val app = app()
        assertFalse(app.b("webViewDebugging"))
        app.data.remove("webViewDebugging")
        assertFalse(WebApp(app.data).b("webViewDebugging"))
        app.set("webViewDebugging", true)
        assertTrue(WebApp(JSONObject(app.data.toString())).b("webViewDebugging"))
    }
    @Test fun extractsOnlyWebUrlsFromSharedText() {
        assertEquals("https://example.com/a?q=one%20two#part", SharedWebUrl.extract("https://example.com/a?q=one%20two#part"))
        assertEquals("http://example.com/", SharedWebUrl.extract("Look at this:\nhttp://example.com"))
        assertEquals("https://example.com/page", SharedWebUrl.extract("A page (https://example.com/page)."))
        listOf("file:///private", "javascript:alert(1)", "ftp://example.com", "https://user:password@example.com", "plain text").forEach {
            assertNull(SharedWebUrl.extract(it))
        }
    }
    @Test fun decodesEmbeddedSvgUrlsWithoutChangingPlusSigns() {
        val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><text>+</text></svg>"
        val source = "data:image/svg+xml," + java.net.URLEncoder.encode(svg, "UTF-8").replace("+", "%20")
        assertEquals(svg, IconData.fromDataUrl(source).toString(Charsets.UTF_8))
    }
    @Test fun validatesHandlerActionsAndBuildsOnlyDeclaredIntentFilters() {
        val app = app().apply {
            manifest().put("scope", "/web/")
            manifest().put("file_handlers", org.json.JSONArray("""[{"action":"/web/open","accept":{"text/plain":[".txt"]}}]"""))
            manifest().put("share_target", JSONObject("""{"action":"/web/share","method":"POST","enctype":"multipart/form-data","params":{"text":"message","files":[{"name":"uploads","accept":["image/*",".txt"]}]}}"""))
            validate()
        }
        assertEquals("https://example.com/web/open", app.manifest().getJSONArray("file_handlers").getJSONObject(0).getString("action"))
        val filters = ManifestFeatures.filters(app.manifest())
        assertEquals(listOf("text/plain"), filters.getValue("io.github.ulysseszh.pwainstaller.runtime.FileHandlerActivity").single().mimeTypes)
        assertTrue(filters.getValue("io.github.ulysseszh.pwainstaller.runtime.ShareTargetActivity").single().actions.contains("android.intent.action.SEND_MULTIPLE"))
        app.manifest().getJSONObject("share_target").put("action", "https://another.example/share")
        assertThrows(IllegalArgumentException::class.java) { app.validate() }
        assertTrue(ManifestFeatures.filters(app().apply { validate() }.manifest()).filterKeys { !it.endsWith(".WebActivity") }.values.all { it.isEmpty() })
    }
    @Test fun rejectsFileSharingWithGetAndInvalidUserAgentHeaders() {
        val app = app().apply { manifest().put("share_target", JSONObject("""{"action":"/share","params":{"files":{"name":"files","accept":"image/*"}}}""")) }
        assertThrows(IllegalArgumentException::class.java) { app.validate() }
        listOf("agent\r\nX-Evil: yes", "agent\u0000").forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { app().apply { set("userAgent", invalid); validate() } }
        }
    }
    @Test fun persistsMultipleIconPayloadsAndPurposesWithoutAnExclusiveSelection() {
        val app = app()
        repeat(2) { app.icons().put(JSONObject().put("src", "upload:$it").put("data", IconData.default).put("purpose", if (it == 0) "any maskable" else "monochrome")) }
        app.set("selectedIcon", "upload:1"); app.set("iconsManual", true)
        app.validate()
        val restored = WebApp(JSONObject(app.data.toString()))
        assertEquals(2, restored.icons().length())
        assertFalse(restored.data.has("selectedIcon"))
        assertEquals("any maskable", restored.icons().getJSONObject(0).getString("purpose"))
        assertEquals("monochrome", restored.icons().getJSONObject(1).getString("purpose"))
        assertEquals(IconData.default, restored.icons().getJSONObject(1).getString("data"))
        assertTrue(restored.data.getBoolean("iconsManual"))
    }
    @Test fun manifestEditorShowsReadableSlashesWithoutChangingStringValues() {
        val values = JSONObject().put("start_url", "https://example.com/path/next")
            .put("literal", "a\\/b").put("html", "</script>")
        assertEquals("{\"url\":\"https://example.com/app/\"}", ManifestText.readableSlashes("""{"url":"https:\/\/example.com\/app\/"}"""))
        val formatted = ManifestText.format(values)
        assertTrue(formatted.contains("https://example.com/path/next"))
        assertTrue(formatted.contains("</script>"))
        val restored = JSONObject(formatted)
        values.keys().forEach { assertEquals(values.getString(it), restored.getString(it)) }
    }
    @Test fun choosesResolutionWithinEachIconPurpose() {
        val icons = org.json.JSONArray()
        fun png(size: Int, purpose: String): String {
            val encoded = testPng(size)
            icons.put(JSONObject().put("data", encoded).put("purpose", purpose))
            return encoded
        }
        val small = png(64, "any"); val medium = png(192, "any"); val large = png(512, "any")
        val maskable = png(256, "maskable"); val mono = png(128, "monochrome")
        assertEquals(small, IconData.best(icons, 48, "any"))
        assertEquals(medium, IconData.best(icons, 144, "any"))
        assertEquals(large, IconData.best(icons, 256, "any"))
        assertEquals(maskable, IconData.best(icons, 512, "maskable"))
        assertEquals(mono, IconData.best(icons, 192, "monochrome"))
        assertEquals(large, IconData.representative(icons))
        assertEquals(setOf("any"), IconData.purposes("unknown"))
    }
    @Test fun fullManifestPurposeEditsApplyToCachedImages() {
        val app = app()
        app.icons().put(JSONObject().put("src", "https://example.com/icon.png").put("data", IconData.default).put("purpose", "any"))
        app.manifest().put("icons", org.json.JSONArray().put(JSONObject().put("src", "/icon.png").put("purpose", "maskable monochrome")))
        app.syncIconPurposes()
        assertEquals("maskable monochrome", app.icons().getJSONObject(0).getString("purpose"))
        assertEquals(IconData.default, IconData.best(app.icons(), 192, "maskable"))
        assertNull(IconData.best(app.icons(), 192, "any"))
    }
}
