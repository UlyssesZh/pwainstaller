package io.github.ulysseszh.pwainstaller

import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WebsiteIconsTest {
    private data class Reply(val bytes: ByteArray, val status: Int = 200, val headers: Map<String, String> = emptyMap())
    private data class Request(val path: String, val agent: String?, val cookie: String?, val authorization: String?)
    private lateinit var server: ServerSocket
    private val worker = Executors.newCachedThreadPool()
    @Volatile private var closed = false
    private val replies = ConcurrentHashMap<String, Reply>()
    private val requests = CopyOnWriteArrayList<Request>()
    @Before fun start() {
        server = ServerSocket(0)
        worker.execute {
            while (!closed) try {
                val socket = server.accept()
                worker.execute {
                    socket.use { client ->
                        val reader = client.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                        val path = reader.readLine().split(' ')[1]
                        val headers = mutableMapOf<String, String>()
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            headers[line.substringBefore(':').lowercase()] = line.substringAfter(':').trim()
                        }
                        requests.add(Request(path, headers["user-agent"], headers["cookie"], headers["authorization"]))
                        val reply = replies[path] ?: Reply("missing".toByteArray(), 404)
                        client.getOutputStream().use { output ->
                            val response = "HTTP/1.1 ${reply.status} Response\r\n" + reply.headers.entries.joinToString("") { "${it.key}: ${it.value}\r\n" } +
                                "Content-Length: ${reply.bytes.size}\r\nConnection: close\r\n\r\n"
                            output.write(response.toByteArray()); output.write(reply.bytes)
                        }
                    }
                }
            } catch (_: java.io.IOException) { }
        }
    }
    @After fun stop() { closed = true; server.close(); worker.shutdownNow() }
    private fun url(path: String) = "http://127.0.0.1:${server.localPort}$path"
    private fun app(path: String = "/") = WebApp().apply { set("baseUrl", url(path)) }
    private fun html(path: String, body: String) { replies[path] = Reply(body.toByteArray()) }
    private fun png(size: Int): ByteArray = IconData.decode(testPng(size))
    private fun ico(png: ByteArray): ByteArray = ByteBuffer.allocate(22 + png.size).order(ByteOrder.LITTLE_ENDIAN).apply {
        putShort(0).putShort(1).putShort(1).put(32).put(32).putShort(0).putShort(1).putShort(32)
        putInt(png.size).putInt(22).put(png)
    }.array()

    @Test fun savingCanDeferSnapshotValidationUntilAfterManifestFetching() {
        html("/manifest.json", """{"name":"Deferred validation","start_url":"/app/"}""")
        val original = app().apply {
            set("manifestUrl", url("/manifest.json")); set("iconsManual", true)
            set("adblockRules", "bad\u0000rule")
        }
        val fetched = ManifestFetcher.fetch(original, validateMetadata = false)
        assertEquals("Deferred validation", fetched.name())
        assertEquals(url("/app/"), fetched.s("id"))
        assertTrue(fetched.s("packageName").startsWith("io.github.ulysseszh.pwainstaller.p_"))
        assertEquals("bad\u0000rule", fetched.s("adblockRules"))
        assertThrows(IllegalArgumentException::class.java) { fetched.validate() }
        assertThrows(IllegalArgumentException::class.java) { ManifestFetcher.fetch(original) }
    }

    @Test fun discoversMultipleLinkIconsUsingHtmlBaseAndReusesManifestDiscoveryPage() {
        html("/", """<base href="/assets/"><link rel="MANIFEST" href="/manifest.json">
            <link rel="shortcut ICON" href="small.png"><link rel="apple-touch-icon" href="large.png">
            <link rel="icon" href="small.png"><link rel="stylesheet" href="ignore.png">""")
        html("/manifest.json", """{"name":"No manifest icons"}""")
        val small = png(32); val large = png(128)
        replies["/assets/small.png"] = Reply(small); replies["/assets/large.png"] = Reply(large)
        val progress = mutableListOf<FetchProgress>()
        val fetched = ManifestFetcher.fetch(app(), onProgress = progress::add)
        assertEquals(setOf(FetchProgress.Stage.PAGE, FetchProgress.Stage.MANIFEST, FetchProgress.Stage.ICON), progress.map { it.stage }.toSet())
        assertTrue(progress.any { it.percent == null })
        assertEquals(100, progress.last().percent)
        assertEquals(2, fetched.icons().length())
        assertArrayEquals(large, IconData.decode(fetched.s("icon")))
        assertEquals(url("/assets/small.png"), fetched.icons().getJSONObject(0).getString("src"))
        assertFalse(fetched.icons().getJSONObject(0).has("manifestIndex"))
        assertFalse(fetched.manifest().has("icons"))
        assertEquals(1, requests.count { it.path == "/" })
        assertEquals(0, requests.count { it.path in listOf("/favicon.ico", "/assets/ignore.png") })
        val restored = WebApp(JSONObject(fetched.data.toString()))
        assertArrayEquals(large, IconData.decode(restored.s("icon")))
    }
    @Test fun fallsBackToRootIcoAfterRedirectAndUnusableLinks() {
        replies["/deep/app/"] = Reply("redirect".toByteArray(), 302, mapOf("Location" to "/landing/"))
        html("/landing/", """<base href="/assets/"><link rel="icon" href="broken.png"><link rel="icon" href="denied.png">""")
        replies["/assets/broken.png"] = Reply("not an image".toByteArray())
        replies["/assets/denied.png"] = Reply("denied".toByteArray(), 401)
        val icon = png(32); replies["/favicon.ico"] = Reply(ico(icon))
        val app = app("/deep/app/").apply { set("useManifest", false) }
        ManifestFetcher.refreshIcon(app)
        assertArrayEquals(icon, IconData.decode(app.s("icon")))
        assertEquals(url("/favicon.ico"), app.icons().getJSONObject(0).getString("src"))
        assertEquals(1, requests.count { it.path == "/favicon.ico" })
        assertFalse(app.data.optBoolean("iconsManual", false))
    }
    @Test fun iconDiscoveryUsesSessionCookiesAndAgentAndConfinesCustomCredentials() {
        replies["/"] = Reply("""<link rel="icon" href="http://localhost:${server.localPort}/site.png">""".toByteArray(),
            headers = mapOf("Set-Cookie" to "signed_in=yes; Path=/"))
        // The linked icon fails; the origin favicon must still receive the page's cookie.
        replies["/site.png"] = Reply("missing".toByteArray(), 404)
        replies["/favicon.ico"] = Reply(png(32))
        val app = app().apply { set("headers", "Authorization: Bearer private"); set("userAgent", "CustomAgent/5") }
        ManifestFetcher.refreshIcon(app)
        assertTrue(requests.all { it.agent == "CustomAgent/5" })
        val linked = requests.single { it.path == "/site.png" }
        assertNull(linked.authorization); assertNull(linked.cookie)
        val favicon = requests.single { it.path == "/favicon.ico" }
        assertEquals("Bearer private", favicon.authorization)
        assertTrue(favicon.cookie.orEmpty().contains("signed_in=yes"))
    }
    @Test fun manifestIconsTakePriorityWithoutFetchingTheWebsite() {
        html("/manifest.json", """{"icons":[{"src":"/manifest-icon.png"}]}""")
        val icon = png(64); replies["/manifest-icon.png"] = Reply(icon)
        val fetched = ManifestFetcher.fetch(app().apply { set("manifestUrl", url("/manifest.json")) })
        assertArrayEquals(icon, IconData.decode(fetched.s("icon")))
        assertEquals(listOf("/manifest.json", "/manifest-icon.png"), requests.map { it.path })
    }
    @Test fun uploadedIconsPreventFallbackAndFailedDiscoveryPreservesSavedIcons() {
        val icon = java.util.Base64.getEncoder().encodeToString(png(32))
        val app = app().apply {
            icons().put(JSONObject().put("src", "upload:custom").put("data", icon).put("purpose", "any"))
            set("iconsManual", true); updateIcon()
        }
        ManifestFetcher.refreshIcon(app)
        assertTrue(requests.isEmpty()); assertEquals(icon, app.s("icon"))
        app.set("iconsManual", false)
        ManifestFetcher.refreshIcon(app)
        assertEquals(icon, app.s("icon"))
        assertEquals("upload:custom", app.icons().getJSONObject(0).getString("src"))
        assertEquals(listOf("/", "/favicon.ico"), requests.map { it.path })
    }
    @Test fun discoveryResumesAfterAllManualIconsAreRemoved() {
        html("/", "<link rel=icon href=/site.png>")
        replies["/site.png"] = Reply(png(32))
        val app = app().apply { set("iconsManual", true); set("icons", JSONArray()); updateIcon() }
        ManifestFetcher.refreshIcon(app)
        assertEquals(url("/site.png"), app.icons().getJSONObject(0).getString("src"))
    }
    @Test fun onlineIconFetchAcceptsDirectImagesAndHtmlWithoutSavingTheLookupUrl() {
        replies["/direct.png"] = Reply(png(64))
        html("/lookup", "<base href='/assets/'><link rel=icon href=small.png><link rel=apple-touch-icon href=large.png>")
        replies["/assets/small.png"] = Reply(png(32)); replies["/assets/large.png"] = Reply(png(128))
        val app = app().apply { set("headers", "Authorization: Bearer icon-secret"); set("userAgent", "IconFetcher/1") }
        val before = app.data.toString()
        val direct = ManifestFetcher.onlineIcons(app, url("/direct.png"), ManifestFetcher.Session()) { }
        assertEquals("64x64", direct.getJSONObject(0).getString("sizes"))
        val icons = ManifestFetcher.onlineIcons(app, url("/lookup"), ManifestFetcher.Session()) { }
        assertEquals(2, icons.length())
        repeat(icons.length()) { assertTrue(icons.getJSONObject(it).getString("src").startsWith("upload:")) }
        assertTrue(requests.all { it.authorization == "Bearer icon-secret" && it.agent == "IconFetcher/1" })
        assertFalse(icons.toString().contains(url("/lookup")))
        assertEquals(before, app.data.toString())
        assertThrows(java.io.IOException::class.java) { ManifestFetcher.onlineIcons(app, url("/missing"), ManifestFetcher.Session()) { } }
        assertEquals(before, app.data.toString())
    }
    @Test fun onlineIconLookupKeepsCustomCredentialsOnTheEnteredOriginAcrossRedirects() {
        replies["/lookup-redirect"] = Reply(byteArrayOf(), 302, mapOf("Location" to "http://localhost:${server.localPort}/redirect/page"))
        html("/redirect/page", "<link rel=icon href=icon.png>")
        replies["/redirect/icon.png"] = Reply(png(64))
        val app = app().apply { set("headers", "Authorization: Bearer private") }
        assertEquals(1, ManifestFetcher.onlineIcons(app, url("/lookup-redirect"), ManifestFetcher.Session()) { }.length())
        assertEquals("Bearer private", requests.single { it.path == "/lookup-redirect" }.authorization)
        assertNull(requests.single { it.path == "/redirect/page" }.authorization)
        assertNull(requests.single { it.path == "/redirect/icon.png" }.authorization)
    }
    @Test fun manualCreationDiscoversLightAndDarkMetaColorsWithANamedAppAndUploadedIcon() {
        html("/", """<title>Website name</title><link rel=manifest href=/must-not-fetch.json>
            <meta name=theme-color media="(prefers-color-scheme: light)" content=#eeeeee>
            <meta name=theme-color media="(prefers-color-scheme: dark)" content=#111111>""")
        val app = app().apply {
            set("useManifest", false); set("headers", "Authorization: Bearer theme-test"); set("userAgent", "ColorFetcher/1")
            manifest().put("name", "Chosen name")
            set("iconsManual", true); icons().put(JSONObject().put("data", IconData.default))
        }
        val progress = mutableListOf<FetchProgress>()
        ManifestFetcher.prepareManual(app, ManifestFetcher.Session(), progress::add)
        assertEquals("Chosen name", app.name())
        assertEquals("#eeeeee", app.manifest().getString("theme_color"))
        assertEquals("#111111", app.s("darkThemeColor"))
        assertFalse(app.manifest().has("darkThemeColor"))
        assertEquals(listOf("/"), requests.map { it.path })
        assertEquals("ColorFetcher/1", requests.single().agent)
        assertEquals("Bearer theme-test", requests.single().authorization)
        assertTrue(progress.any { it.stage == FetchProgress.Stage.PAGE })
    }

    @Test fun manualCreationKeepsChosenColorsAndReusesThePageForTitleAndIcons() {
        html("/", """<title>Site title</title><link rel=icon href=/site.png>
            <meta name=theme-color content=red><meta name=theme-color media="(prefers-color-scheme: dark)" content=black>""")
        replies["/site.png"] = Reply(png(64))
        val app = app().apply { manifest().put("theme_color", "#112233"); set("darkThemeColor", "#445566") }
        ManifestFetcher.prepareManual(app, ManifestFetcher.Session()) { }
        assertEquals("Site title", app.name())
        assertEquals("#112233", app.manifest().getString("theme_color")); assertEquals("#445566", app.s("darkThemeColor"))
        assertEquals(listOf("/", "/site.png"), requests.map { it.path })
    }

    @Test fun optionalColorDiscoveryDoesNotRequireCredentialsOrFetchWhenAllValuesAreProvided() {
        replies["/"] = Reply("Sign in".toByteArray(), 401)
        val app = app().apply {
            manifest().put("name", "Manual name"); set("iconsManual", true)
            icons().put(JSONObject().put("data", IconData.default))
        }
        ManifestFetcher.prepareManual(app, ManifestFetcher.Session()) { }
        assertFalse(app.manifest().has("theme_color")); assertEquals("", app.s("darkThemeColor"))
        assertEquals(1, requests.size)
        app.manifest().put("theme_color", "#123456"); app.set("darkThemeColor", "#654321")
        ManifestFetcher.prepareManual(app, ManifestFetcher.Session()) { }
        assertEquals(1, requests.size)
    }

    @Test fun manualCreationUsesThePageTitleOnlyWhenTheNameIsBlankAndReusesThePage() {
        html("/", "<title>Shop &amp; More</title><link rel=icon href=/site.png>")
        replies["/site.png"] = Reply(png(64))
        val unnamed = app().apply { set("useManifest", false) }
        ManifestFetcher.prepareManual(unnamed, ManifestFetcher.Session()) { }
        assertEquals("Shop & More", unnamed.manifest().getString("name"))
        assertEquals(1, requests.count { it.path == "/" })
        val named = app().apply { manifest().put("name", "My name") }
        ManifestFetcher.prepareManual(named, ManifestFetcher.Session()) { }
        assertEquals("My name", named.name())
    }

}
