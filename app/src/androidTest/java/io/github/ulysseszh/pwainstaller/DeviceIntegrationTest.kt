package io.github.ulysseszh.pwainstaller

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.content.pm.PackageManager
import android.content.pm.ShortcutManager
import android.net.Uri
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.util.Date
import javax.security.auth.x500.X500Principal
import java.util.zip.ZipFile
import android.view.accessibility.AccessibilityNodeInfo
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.ulysseszh.pwainstaller.core.DataArchive
import io.github.ulysseszh.pwainstaller.browser.BrowserCredentials
import io.github.ulysseszh.pwainstaller.browser.R as BrowserR
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import android.content.ClipData
import androidx.core.content.FileProvider
import java.util.zip.ZipInputStream

@RunWith(AndroidJUnit4::class)
class DeviceIntegrationTest {
    private lateinit var context: Context
    private lateinit var fixture: Fixture
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private var originalImmersiveConfirmation = ""
    @Before fun setup() {
        context = instrumentation.targetContext; fixture = Fixture()
        originalImmersiveConfirmation = shell("settings get secure immersive_mode_confirmations").trim()
        shell("settings put secure immersive_mode_confirmations confirmed")
    }
    @After fun cleanup() {
        fixture.close()
        if (originalImmersiveConfirmation == "confirmed") shell("settings put secure immersive_mode_confirmations confirmed")
        else shell("settings delete secure immersive_mode_confirmations")
    }

    private fun shell(command: String): String {
        // UiAutomation executes an argv directly. A script captures stderr and supports shell syntax.
        val script = File(context.getExternalFilesDir(null), "integration-command.sh")
        script.writeText("#!/system/bin/sh\n$command 2>&1\n")
        return ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("sh ${script.absolutePath}"))
            .use { ManifestFetcher.read(it, 1024 * 1024).toString(Charsets.UTF_8) }
    }

    private fun install(apk: File) {
        val shared = File(context.getExternalFilesDir(null), "integration.apk")
        apk.copyTo(shared, overwrite = true)
        shell("cp ${shared.absolutePath} /data/local/tmp/pwa-integration.apk")
        shell("chmod 644 /data/local/tmp/pwa-integration.apk")
        val response = shell("pm install -r /data/local/tmp/pwa-integration.apk")
        assertTrue(response, response.contains("Success"))
        shared.delete()
        shell("rm /data/local/tmp/pwa-integration.apk")
    }

    @Test fun discoversManifestAndRetriesWithInteractiveCookies() {
        val app = WebApp().apply { set("baseUrl", fixture.url("/auth/")); set("headers", "X-Test: expected"); set("userAgent", "PwaFixture/1.0") }
        try { ManifestFetcher.fetch(app); fail("Expected credentials challenge") } catch (_: ManifestFetcher.CredentialsRequired) { }
        val token = LoginSessions.begin(context, app.s("key"))
        val fetched = try {
            LoginSessions.call(context, token, "setCookie", fixture.url("/"), "authenticated=yes; Path=/")
            ManifestFetcher.fetch(app, ManifestFetcher.Session(context, token))
        } finally { LoginSessions.discard(context, token) }
        assertEquals("Device fixture", fetched.name())
        assertEquals(fixture.url("/identity"), fetched.manifest().getString("id"))
        assertEquals(fixture.url("/manifest.json"), fetched.s("manifestUrl"))
        assertTrue(fixture.sawHeader.get())
        assertEquals("PwaFixture/1.0", fixture.lastUserAgent.get())
    }

    @Test fun svgIconMetadataSurvivesExportImportFailedRefreshAndOfflineApkGeneration() {
        val name = "io.github.ulysseszh.pwainstaller.integrationicons"
        val app = ManifestFetcher.fetch(WebApp().apply { set("baseUrl", fixture.url("/")); set("packageName", name) })
        assertNotEquals(IconData.default, app.s("icon"))
        val vector = app.icons().getJSONObject(0)
        assertEquals("image/svg+xml", vector.getString("type")); assertEquals("any", vector.getString("sizes"))
        val originalSvg = requireNotNull(IconData.source(vector))
        assertTrue(originalSvg.toString(Charsets.UTF_8).contains("<svg"))
        val icon = IconData.decode(app.s("icon"))
        val image = BitmapFactory.decodeByteArray(icon, 0, icon.size)
        assertEquals(512, image.width); assertEquals(512, image.height)
        assertEquals(Color.rgb(36, 163, 105), image.getPixel(256, 256)); image.recycle()
        app.manifest().put("background_color", "#123456").put("theme_color", "#abcdef")
        app.set("webViewDebugging", true)
        fun purposeIcon(purpose: String, artwork: String) {
            val svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">$artwork</svg>"""
            app.icons().put(IconData.entry(svg.toByteArray(), "upload:$purpose", purpose))
        }
        purposeIcon("maskable", """<rect width="100" height="100" fill="yellow"/>""")
        purposeIcon("monochrome", """<circle cx="50" cy="50" r="30" fill="white"/>""")
        val exported = JSONObject(app.data.toString())
        AppDatabase(context).use { db ->
            try {
                db.importAll(JSONArray().put(exported))
                val restored = db.get(app.s("key"))!!
                assertEquals(app.s("icon"), restored.s("icon"))
                assertArrayEquals(originalSvg, IconData.source(restored.icons().getJSONObject(0)))
                assertEquals("any", restored.icons().getJSONObject(0).getString("sizes"))
                restored.manifest().put("icons", JSONArray().put(JSONObject().put("src", fixture.url("/invalid-icon.png"))))
                ManifestFetcher.refreshIcon(restored)
                assertEquals(app.s("icon"), restored.s("icon"))
                fixture.close() // Building from metadata must not depend on the website.
                val apk = ApkBuilder.build(context, restored)
                try {
                    ZipFile(apk).use { zip ->
                        assertArrayEquals(icon, zip.getInputStream(zip.getEntry("res/drawable/web_icon.png")).readBytes())
                        assertArrayEquals(originalSvg, zip.getInputStream(zip.getEntry("assets/icons/0.svg")).readBytes())
                        val config = JSONObject(zip.getInputStream(zip.getEntry("assets/webapp.json")).bufferedReader().readText())
                        assertEquals("any", config.getJSONArray("icons").getJSONObject(0).getString("sizes"))
                        assertEquals("image/svg+xml", config.getJSONArray("icons").getJSONObject(0).getString("type"))
                        val raster = zip.getInputStream(zip.getEntry("res/mipmap-xxxhdpi-v4/web_icon.png")).readBytes()
                        assertEquals(192, BitmapFactory.decodeByteArray(raster, 0, raster.size).width)
                    }
                    install(apk)
                    val resources = context.packageManager.getResourcesForApplication(name)
                    assertEquals(Color.rgb(18, 52, 86), resources.getColor(resources.getIdentifier("web_background", "color", "io.github.ulysseszh.pwainstaller.template")))
                    assertEquals(Color.rgb(171, 205, 239), resources.getColor(resources.getIdentifier("web_theme", "color", "io.github.ulysseszh.pwainstaller.template")))
                    val splash = resources.getDrawable(resources.getIdentifier("web_splash", "drawable", "io.github.ulysseszh.pwainstaller.template"))
                    val bitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
                    splash.setBounds(0, 0, 1024, 1024); splash.draw(Canvas(bitmap))
                    assertEquals(Color.rgb(18, 52, 86), bitmap.getPixel(0, 0))
                    assertEquals(Color.rgb(36, 163, 105), bitmap.getPixel(512, 512)); bitmap.recycle()
                    if (Build.VERSION.SDK_INT >= 26) {
                        val launcher = context.packageManager.getApplicationIcon(name)
                        assertTrue(launcher is android.graphics.drawable.AdaptiveIconDrawable)
                        val preview = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
                        launcher.setBounds(0, 0, 192, 192); launcher.draw(Canvas(preview))
                        assertEquals(Color.YELLOW, preview.getPixel(96, 96)); preview.recycle()
                        if (Build.VERSION.SDK_INT >= 33) {
                            val monochrome = (launcher as android.graphics.drawable.AdaptiveIconDrawable).monochrome
                            assertNotNull("Themed icon must use the monochrome purpose", monochrome)
                            val mono = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
                            monochrome!!.setBounds(0, 0, 192, 192); monochrome.draw(Canvas(mono))
                            assertEquals(Color.WHITE, mono.getPixel(96, 96))
                            assertEquals(0, Color.alpha(mono.getPixel(0, 0))); mono.recycle()
                        }
                        assertTrue(context.packageManager.getApplicationIcon(context.packageName) is android.graphics.drawable.AdaptiveIconDrawable)
                    }
                } finally { apk.delete() }
            } finally { db.remove(app); shell("pm uninstall $name") }
        }
    }

    @Test fun acceptsEmbeddedSvgIconsAndPreservesTheirAspectRatio() {
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" width="100" height="50"><rect width="100" height="50" fill="#ff0000"/></svg>"""
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/"))
            manifest().put("icons", JSONArray().put(JSONObject().put("src", "data:image/svg+xml;base64," + android.util.Base64.encodeToString(svg.toByteArray(), android.util.Base64.NO_WRAP))))
        }
        ManifestFetcher.refreshIcon(app)
        val bytes = IconData.decode(app.s("icon"))
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertEquals(Color.RED, bitmap.getPixel(256, 256))
        assertEquals(0, Color.alpha(bitmap.getPixel(256, 0))); bitmap.recycle()
    }

    @Test fun generatedAppOpensFilesReceivesMultipartSharesAndUsesItsUserAgentAndShortcutIcons() {
        val name = "io.github.ulysseszh.pwainstaller.integrationfeatures"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name); set("userAgent", "PwaFeatures/3.0")
            manifest().put("file_handlers", JSONArray("""[{"action":"/open-file","accept":{"text/plain":[".txt"]}}]"""))
            manifest().put("share_target", JSONObject("""{"action":"/share","method":"POST","enctype":"multipart/form-data","params":{"title":"subject","text":"message","url":"link","files":[{"name":"uploads","accept":["text/plain",".txt"]}]}}"""))
            manifest().put("shortcuts", JSONArray().put(JSONObject().put("name", "Open fixture").put("url", fixture.url("/shortcut"))
                .put("icons", JSONArray().put(JSONObject().put("src", fixture.url("/icon.svg"))))))
            validate()
        }
        val file = File(context.cacheDir, "apks/shared.txt").apply { parentFile!!.mkdirs(); writeText("Opened file content") }
        val second = File(context.cacheDir, "apks/second.txt").apply { writeText("Second shared file") }
        try {
            ManifestFetcher.refreshIcon(app)
            val apk = ApkBuilder.build(context, app)
            ZipFile(apk).use { assertNotNull(it.getEntry("assets/shortcut-icons/0.png")) }
            install(apk); apk.delete()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            val uri2 = FileProvider.getUriForFile(context, "${context.packageName}.files", second)
            val open = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "text/plain").setPackage(name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            assertTrue(context.packageManager.queryIntentActivities(open, PackageManager.MATCH_DEFAULT_ONLY).any { it.activityInfo.name.endsWith("FileHandlerActivity") })
            context.startActivity(open)
            assertTrue("File did not reach launchQueue", fixture.fileOpened.await(30, TimeUnit.SECONDS))
            assertTrue(fixture.openedFile.get(), fixture.openedFile.get().contains("name=shared.txt&body=Opened%20file%20content"))
            assertEquals("PwaFeatures/3.0", fixture.lastUserAgent.get())
            val share = Intent(Intent.ACTION_SEND_MULTIPLE).setType("text/plain").setPackage(name)
                .putExtra(Intent.EXTRA_SUBJECT, "Shared title").putExtra(Intent.EXTRA_TEXT, "Message & https://example.com/shared")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri, uri2))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            share.clipData = ClipData.newRawUri("files", uri).apply { addItem(ClipData.Item(uri2)) }
            assertTrue(context.packageManager.queryIntentActivities(share, PackageManager.MATCH_DEFAULT_ONLY).any { it.activityInfo.name.endsWith("ShareTargetActivity") })
            context.startActivity(share)
            assertTrue("Multipart share did not reach its action", fixture.shareReceived.await(30, TimeUnit.SECONDS))
            val body = fixture.sharedBody.get()
            listOf("name=\"subject\"", "Shared title", "name=\"message\"", "Message &", "https://example.com/shared", "name=\"uploads\"", "filename=\"shared.txt\"", "Opened file content", "filename=\"second.txt\"", "Second shared file").forEach {
                assertTrue("Missing multipart field $it: $body", body.contains(it))
            }
            assertFalse("A link embedded in shared text must not also be sent as URL data", body.contains("name=\"link\""))
            assertEquals(1, Regex(Regex.escape("https://example.com/shared")).findAll(body).count())
            assertEquals("PwaFeatures/3.0", fixture.lastUserAgent.get())
            if (Build.VERSION.SDK_INT >= 25) assertTrue(shell("dumpsys shortcut").contains("Open fixture"))
        } finally { shell("pm uninstall $name"); file.delete(); second.delete() }
    }

    @Test fun generatedShareTargetSupportsGetAndUrlEncodedPost() {
        val name = "io.github.ulysseszh.pwainstaller.integrationshare"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name)
            manifest().put("share_target", JSONObject("""{"action":"/share?existing=1","params":{"title":"subject","text":"message","url":"link"}}"""))
            validate()
        }
        try {
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            context.startActivity(Intent(Intent.ACTION_SEND).setType("text/plain").setPackage(name)
                .putExtra(Intent.EXTRA_SUBJECT, "Shared & title").putExtra(Intent.EXTRA_TEXT, "https://example.com/path?q=1")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            assertTrue(fixture.shareReceived.await(30, TimeUnit.SECONDS))
            assertTrue(fixture.shareQuery.get(), fixture.shareQuery.get().contains("existing=1&"))
            assertTrue(fixture.shareQuery.get(), fixture.shareQuery.get().contains("subject=Shared+%26+title"))
            assertTrue(fixture.shareQuery.get(), fixture.shareQuery.get().contains("link=https%3A%2F%2Fexample.com%2Fpath%3Fq%3D1"))
            assertFalse("A bare URL must not also be sent through the text parameter", fixture.shareQuery.get().contains("message="))
            app.manifest().getJSONObject("share_target").put("method", "POST")
            val updated = ApkBuilder.build(context, app); install(updated); updated.delete()
            fixture.sharedBody.set(null); fixture.readyPaths.remove("/share")
            context.startActivity(Intent(Intent.ACTION_SEND).setType("text/plain").setPackage(name)
                .putExtra(Intent.EXTRA_TEXT, "Posted & text").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val deadline = System.currentTimeMillis() + 30000
            while (fixture.sharedBody.get() == null && System.currentTimeMillis() < deadline) Thread.sleep(100)
            assertEquals("message=Posted+%26+text", fixture.sharedBody.get())
            awaitNative("The first POST document must finish loading before the next share") { fixture.readyPaths.contains("/share") }
            fixture.sharedBody.set(null); fixture.readyPaths.remove("/share")
            context.startActivity(Intent(Intent.ACTION_SEND).setType("text/plain").setPackage(name)
                .putExtra(Intent.EXTRA_TEXT, "https://example.com/link").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            awaitNative("A URL-only POST must reach the share target") { fixture.sharedBody.get() != null }
            assertEquals("link=https%3A%2F%2Fexample.com%2Flink", fixture.sharedBody.get())
            app.manifest().getJSONObject("share_target").getJSONObject("params").remove("url")
            val textOnly = ApkBuilder.build(context, app); install(textOnly); textOnly.delete()
            fixture.sharedBody.set(null)
            context.startActivity(Intent(Intent.ACTION_SEND).setType("text/plain").setPackage(name)
                .putExtra(Intent.EXTRA_TEXT, "https://example.com/link").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            awaitNative("Text-only targets must still receive shared links") { fixture.sharedBody.get() != null }
            assertEquals("message=https%3A%2F%2Fexample.com%2Flink", fixture.sharedBody.get())
        } finally { shell("pm uninstall $name") }
    }

    @Test fun multipartShareNavigatesThroughTheServiceWorkerAndUpdatesBothSystemBarColors() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 26)
        val name = "io.github.ulysseszh.pwainstaller.integrationshareworker"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name)
            manifest().put("start_url", fixture.url("/share-worker/start")).put("background_color", "#ffffff")
                .put("share_target", JSONObject("""{"action":"/share-worker/submit","method":"POST","enctype":"multipart/form-data","params":{"title":"title","text":"text","url":"url"}}"""))
            validate()
        }
        try {
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            awaitNative("The app must have an active service worker before receiving shared content") { fixture.requestedPaths.contains("/share-worker/registered") }
            for (cold in listOf(false, true)) {
                if (cold) shell("am force-stop $name")
                val text = "Shared & + text ✓ ${if (cold) "cold" else "warm"} https://example.com/shared"
                fixture.shareWorkerStates.clear()
                context.startActivity(Intent(Intent.ACTION_SEND).setType("text/plain").setPackage(name)
                    .putExtra(Intent.EXTRA_SUBJECT, "Shared title").putExtra(Intent.EXTRA_TEXT, text).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                awaitNative("Multipart POST must reach the navigation-only service-worker share handler", timeout = 20000) {
                    fixture.shareWorkerStates.lastOrNull()?.optString("text") == text
                }
                val result = fixture.shareWorkerStates.last()
                assertEquals("navigate", result.getString("mode")); assertEquals("Shared title", result.getString("title"))
                assertEquals("Mixed text and URL shares must not duplicate the URL field", "", result.getString("url"))
                val expected = Color.rgb(24, 54, 80)
                awaitNative("Sharing must update status and navigation backgrounds to the resulting page") {
                    val picture = instrumentation.uiAutomation.takeScreenshot() ?: return@awaitNative false
                    try {
                        val density = context.resources.displayMetrics.density
                        val status = picture.getPixel(picture.width / 2, (12 * density).toInt())
                        val navigation = picture.getPixel(picture.width / 3, picture.height - (24 * density).toInt())
                        status == expected && navigation == expected
                    } finally { picture.recycle() }
                }
            }
        } finally { shell("pm uninstall $name") }
    }

    @Test fun generatedAppRegistersEveryScopeDomainAndKeepsItsLauncherFilter() {
        val name = "io.github.ulysseszh.pwainstaller.integrationdomains"
        val secondary = "http://localhost:${fixture.port}/secondary/deep"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/primary/")); set("packageName", name)
            set("urlPatterns", fixture.url("/primary/") + "\nhttp://localhost:${fixture.port}/secondary/\nhttps://*.example.org/shared/*")
            validate()
        }
        fun handles(url: String) = context.packageManager.queryIntentActivities(Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(name), PackageManager.MATCH_DEFAULT_ONLY)
            .any { it.activityInfo.name.endsWith(".WebActivity") }
        try {
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            assertNotNull(context.packageManager.getLaunchIntentForPackage(name))
            listOf(fixture.url("/primary/child"), secondary, "https://example.org/shared/test", "https://sub.example.org/shared/test").forEach { assertTrue(it, handles(it)) }
            listOf(fixture.url("/secondary/deep"), "http://localhost:${fixture.port}/primary/child", "https://example.org/outside", "https://unrelated.example/shared/test").forEach { assertFalse(it, handles(it)) }
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(secondary)).setPackage(name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            awaitNative("An implicit link for a secondary scope domain must load inside the app") {
                fixture.requestedPaths.contains("/secondary/deep") && activeNativeNodes().any { it.packageName?.toString() == name }
            }
        } finally { shell("pm uninstall $name") }
    }

    @Test fun outsideRedirectDispatchesItsLastKnownDestinationToTheNativeUrlHandler() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 31)
        val name = "io.github.ulysseszh.pwainstaller.integrationredirectdispatch"
        val host = "external-dispatch-fixture.invalid"
        val destination = "https://$host/handoff/from-redirect?item=1"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/inside/")); set("packageName", name)
            manifest().put("start_url", fixture.url("/inside/native-launch")); validate()
        }
        shell("pm set-app-links-allowed --user 0 --package ${context.packageName} true")
        shell("pm set-app-links-user-selection --user 0 --package ${context.packageName} true $host")
        try {
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            awaitNative("The navigation page must load") { fixture.readyPaths.contains("/inside/native-launch") }
            clickText("Native app redirect", name)
            awaitNative("The final redirect URL must go straight to its native handler even when its server is unreachable", timeout = 20000) {
                activeNativeNodes().any { it.packageName?.toString() == context.packageName && it.text?.toString() == destination }
            }
            assertEquals("The system must receive the resolved URL without loading the original redirect again", 1,
                fixture.requestedPaths.count { it == "/outside/native" })
        } finally {
            shell("input keyevent KEYCODE_BACK"); shell("pm uninstall $name")
            shell("pm set-app-links-user-selection --user 0 --package ${context.packageName} false $host")
        }
    }

    @Test fun editorRetainsMultipleIconResolutionsAndAllowsCombinedPurposes() {
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("install", false); validate() }
        AppDatabase(context).use { it.save(app) }
        val svg = File(context.cacheDir, "apks/upload.svg").apply { parentFile!!.mkdirs(); writeText("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10"><rect width="10" height="10" fill="red"/></svg>""") }
        val png = File(context.cacheDir, "apks/upload.png").apply { writeBytes(IconData.decode(IconData.default)) }
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("id", app.s("key"))) as EditorActivity
        awaitEditor(editor)
        try {
            val result = Intent().apply {
                clipData = ClipData.newRawUri("icons", FileProvider.getUriForFile(context, "${context.packageName}.files", svg))
                    .apply { addItem(ClipData.Item(FileProvider.getUriForFile(context, "${context.packageName}.files", png))) }
            }
            instrumentation.runOnMainSync {
                EditorActivity::class.java.getDeclaredMethod("onActivityResult", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Intent::class.java)
                    .apply { isAccessible = true }.invoke(editor, 42, android.app.Activity.RESULT_OK, result)
            }
            val deadline = System.currentTimeMillis() + 10000
            var busy = true
            while (busy && System.currentTimeMillis() < deadline) {
                Thread.sleep(50); instrumentation.runOnMainSync { busy = editor.busy }
            }
            instrumentation.runOnMainSync {
                val icons = views(editor.root).filterIsInstance<IconEditor>().single()
                assertTrue(views(icons).filterIsInstance<android.widget.RadioButton>().isEmpty())
                val purposes = views(icons).filterIsInstance<com.google.android.material.checkbox.MaterialCheckBox>()
                assertEquals(6, purposes.size)
                purposes[1].isChecked = true
                purposes[5].isChecked = true
                val updated = EditorActivity::class.java.getDeclaredMethod("read").apply { isAccessible = true }.invoke(editor) as WebApp
                assertEquals(2, updated.icons().length())
                assertEquals(updated.icons().getJSONObject(0).getString("data"), updated.s("icon"))
                assertEquals("any", updated.icons().getJSONObject(0).getString("sizes"))
                assertEquals("image/svg+xml", updated.icons().getJSONObject(0).getString("type"))
                assertArrayEquals(svg.readBytes(), IconData.source(updated.icons().getJSONObject(0)))
                assertTrue(views(icons).filterIsInstance<TextView>().any { it.text.toString().contains(context.getString(R.string.scalable_icon)) })
                assertEquals("192x192", updated.icons().getJSONObject(1).getString("sizes"))
                assertEquals("any maskable", updated.icons().getJSONObject(0).getString("purpose"))
                assertEquals("any monochrome", updated.icons().getJSONObject(1).getString("purpose"))
                val restored = WebApp(JSONObject(updated.data.toString()))
                assertFalse(restored.data.has("selectedIcon"))
                assertEquals(2, restored.icons().length())
            }
        } finally { instrumentation.runOnMainSync { editor.finish() }; AppDatabase(context).use { it.remove(app) }; svg.delete(); png.delete() }
    }

    private fun awaitEditor(editor: EditorActivity) {
        val deadline = System.currentTimeMillis() + 15000
        var loaded = false
        while (!loaded && System.currentTimeMillis() < deadline) {
            instrumentation.runOnMainSync { loaded = !editor.busy && views(editor.root).any { it is IconEditor } }
            if (!loaded) Thread.sleep(50)
        }
        assertTrue("Editor did not finish loading", loaded)
        instrumentation.runOnMainSync {
            val action = editor.toolbar.menu.findItem(android.R.id.button1)
            assertNotNull("Save must be a checkmark action in the top bar", action.icon)
            assertEquals(context.getString(R.string.save), action.title)
            assertTrue(action.isEnabled)
            assertFalse("The editor must not have a footer save button", views(editor.body).filterIsInstance<android.widget.Button>().any { it.text.toString() == context.getString(R.string.save) })
        }
    }

    private fun views(view: View): List<View> = buildList {
        add(view)
        if (view is ViewGroup) repeat(view.childCount) { addAll(views(view.getChildAt(it))) }
    }

    @Test fun shareAndLauncherShortcutOpenNewAppEditorAndRejectNonWebSchemes() {
        val shared = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "A website:\nhttps://example.com/from-share?q=1")
        assertTrue(context.packageManager.queryIntentActivities(shared.setPackage(context.packageName), PackageManager.MATCH_DEFAULT_ONLY).any { it.activityInfo.name.endsWith("ShareActivity") })
        listOf("http", "https").forEach { scheme ->
            val view = Intent(Intent.ACTION_VIEW, Uri.parse("$scheme://example.com/")).setPackage(context.packageName)
            assertTrue(context.packageManager.queryIntentActivities(view, PackageManager.MATCH_DEFAULT_ONLY).any { it.activityInfo.name.endsWith("ShareActivity") })
        }
        val invalid = Intent(Intent.ACTION_VIEW, Uri.parse("file:///private")).setPackage(context.packageName)
        assertTrue(context.packageManager.queryIntentActivities(invalid, PackageManager.MATCH_DEFAULT_ONLY).none { it.activityInfo.name.endsWith("ShareActivity") })
        var monitor = instrumentation.addMonitor(EditorActivity::class.java.name, null, false)
        try {
            context.startActivity(shared.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val editor = monitor.waitForActivityWithTimeout(10000) as? EditorActivity
            assertNotNull(editor)
            awaitEditor(editor!!)
            instrumentation.runOnMainSync {
                assertTrue(views(editor!!.root).filterIsInstance<TextView>().any { it.text.toString() == "https://example.com/from-share?q=1" })
                editor.finish()
            }
            instrumentation.removeMonitor(monitor)
            monitor = instrumentation.addMonitor(EditorActivity::class.java.name, null, false)
            if (Build.VERSION.SDK_INT >= 25) {
                val shortcuts = context.getSystemService(ShortcutManager::class.java).manifestShortcuts
                val reinstall = shortcuts.single { it.id == "reinstall_all" }
                assertEquals(MainActivity::class.java.name, reinstall.intent!!.component!!.className)
                assertEquals("io.github.ulysseszh.pwainstaller.REINSTALL_ALL", reinstall.intent!!.action)
                val shortcut = shortcuts.single { it.id == "add_web_app" }
                assertEquals(AddAppActivity::class.java.name, shortcut.intent!!.component!!.className)
                context.startActivity(shortcut.intent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else context.startActivity(Intent(context, AddAppActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val empty = monitor.waitForActivityWithTimeout(10000) as? EditorActivity
            assertNotNull(empty)
            awaitEditor(empty!!)
            assertNull(empty!!.intent.getStringExtra("baseUrl"))
            instrumentation.runOnMainSync {
                assertTrue(views(empty.root).filterIsInstance<TextView>().any { it.text.toString() == context.getString(R.string.use_manifest) })
                empty.finish()
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun colorPickerSynchronizesRgbHsvOpacityAndHexWithoutApplyingInvalidInput() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 24)
        val main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        var dialog: androidx.appcompat.app.AlertDialog? = null
        var chosen: String? = null
        try {
            instrumentation.runOnMainSync {
                dialog = ColorPicker.show(main, R.string.theme_color, "#11223380") { chosen = it }
                val window = dialog!!.window!!.decorView
                val hex = views(window).filterIsInstance<com.google.android.material.textfield.TextInputEditText>().single()
                val bars = views(window).filterIsInstance<android.widget.SeekBar>().associateBy { it.contentDescription.toString() }
                fun bar(label: Int) = bars.getValue(context.getString(label))
                fun channels(red: Int, green: Int, blue: Int, alpha: Int) {
                    assertEquals(red, bar(R.string.red).progress); assertEquals(green, bar(R.string.green).progress)
                    assertEquals(blue, bar(R.string.blue).progress); assertEquals(alpha, bar(R.string.opacity).progress)
                }
                fun move(label: Int, progress: Int) {
                    assertTrue(bar(label).performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,
                        Bundle().apply { putFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, progress.toFloat()) }))
                }
                channels(17, 34, 51, 128)
                hex.setText("abcdef40"); channels(171, 205, 239, 64)
                assertEquals("abcdef40", hex.text.toString())
                hex.setText("#12zzzz")
                assertFalse(dialog!!.getButton(android.content.DialogInterface.BUTTON_POSITIVE).isEnabled)
                channels(171, 205, 239, 64)
                move(R.string.red, 64); channels(64, 205, 239, 64)
                assertEquals("#40cdef40", hex.text.toString())
                assertTrue(dialog!!.getButton(android.content.DialogInterface.BUTTON_POSITIVE).isEnabled)
                move(R.string.opacity, 255); channels(64, 205, 239, 255)
                assertEquals("#40cdef", hex.text.toString())
                move(R.string.hue, 180)
                val color = requireNotNull(io.github.ulysseszh.pwainstaller.core.CssColors.parse(hex.text.toString()))
                channels(Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color))
                hex.setText("#0f08"); channels(0, 255, 0, 136)
                assertEquals("#0f08", hex.text.toString())
                dialog!!.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
            }
            instrumentation.waitForIdleSync()
            assertEquals("#00ff0088", chosen)
            instrumentation.runOnMainSync {
                dialog = ColorPicker.show(main, R.string.theme_color, "#112233") { chosen = it }
                views(dialog!!.window!!.decorView).filterIsInstance<com.google.android.material.textfield.TextInputEditText>().single().setText("#ffffff")
                dialog!!.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick()
            }
            instrumentation.waitForIdleSync()
            assertEquals("#00ff0088", chosen)
        } finally { instrumentation.runOnMainSync { dialog?.dismiss(); main.finish() } }
    }

    @Test fun editorUsesColorPickerAndSelectionMenusAndKeepsDebuggingChoice() {
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("install", false); validate() }
        AppDatabase(context).use { it.save(app) }
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("id", app.s("key"))) as EditorActivity
        awaitEditor(editor)
        try {
            instrumentation.runOnMainSync {
                val all = views(editor.root)
                assertTrue(all.filterIsInstance<TextView>().none { it.text.toString() == context.getString(R.string.use_manifest) })
                assertTrue(all.filterIsInstance<TextView>().any { it.text.toString() == context.getString(R.string.fetch_manifest) })
                assertEquals(4, all.filterIsInstance<ColorButton>().size)
                val menus = all.filterIsInstance<SelectionInput>()
                assertEquals(2, menus.size)
                menus[0].value = "fullscreen"; menus[1].value = "landscape-secondary"
                menus.forEach { assertNull(views(it).filterIsInstance<MaterialAutoCompleteTextView>().single().keyListener) }
                val debugging = all.filterIsInstance<com.google.android.material.checkbox.MaterialCheckBox>().single { it.text.toString() == context.getString(R.string.webview_debugging) }
                assertFalse(debugging.isChecked); debugging.isChecked = true
                val notch = all.filterIsInstance<com.google.android.material.checkbox.MaterialCheckBox>().single { it.text.toString() == context.getString(R.string.use_notch_areas) }
                assertTrue(notch.isChecked); notch.isChecked = false
                val options = EditorActivity::class.java.getDeclaredField("optionColors").apply { isAccessible = true }.get(editor) as Map<*, *>
                (options["darkThemeColor"] as ColorButton).value = "#8bd5ca"
                (options["darkBackgroundColor"] as ColorButton).value = "#20242a"
                val standard = EditorActivity::class.java.getDeclaredField("colors").apply { isAccessible = true }.get(editor) as Map<*, *>
                val color = standard["theme_color"] as ColorButton
                color.value = "#33669980"
                color.performClick()
            }
            instrumentation.waitForIdleSync()
            var root = instrumentation.uiAutomation.rootInActiveWindow
            val dialogDeadline = System.currentTimeMillis() + 5000
            while (root == null && System.currentTimeMillis() < dialogDeadline) {
                Thread.sleep(100); root = instrumentation.uiAutomation.rootInActiveWindow
            }
            assertNotNull(root)
            assertTrue(root!!.findAccessibilityNodeInfosByText(context.getString(R.string.hue)).isNotEmpty())
            val ok = root!!.findAccessibilityNodeInfosByText(context.getString(android.R.string.ok)).first { it.className.toString().contains("Button") }
            assertTrue(ok.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                val read = EditorActivity::class.java.getDeclaredMethod("read").apply { isAccessible = true }
                val updated = read.invoke(editor) as WebApp
                assertEquals("fullscreen", updated.manifest().getString("display"))
                assertEquals("landscape-secondary", updated.manifest().getString("orientation"))
                assertEquals("#33669980", updated.manifest().getString("theme_color"))
                assertEquals("#8bd5ca", updated.s("darkThemeColor")); assertEquals("#20242a", updated.s("darkBackgroundColor"))
                assertFalse(updated.manifest().has("darkThemeColor")); assertFalse(updated.manifest().has("darkBackgroundColor"))
                updated.validate(); AppDatabase(context).use { db -> db.save(updated); assertEquals("#20242a", db.get(updated.s("key"))!!.s("darkBackgroundColor")) }
                assertTrue(updated.b("webViewDebugging"))
                assertFalse(updated.b("useNotchAreas"))
            }
        } finally { instrumentation.runOnMainSync { editor.finish() }; AppDatabase(context).use { it.remove(app) } }
    }

    @Test fun persistsMetadataAndImportsAtomicallyOnTheMinimumApi() {
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/"))
            set("packageName", "io.github.ulysseszh.pwainstaller.integrationmetadata.p_${UUID.randomUUID().toString().replace('-', '_')}")
            validate()
        }
        AppDatabase(context).use { db ->
            try {
                db.save(app)
                assertEquals(app.s("packageName"), db.get(app.s("key"))!!.s("packageName"))
                val update = app.copy().apply { manifest().put("name", "Updated metadata") }
                val invalid = WebApp().apply { set("baseUrl", "file:///data/data/private") }
                try {
                    db.importAll(JSONArray().put(update.data).put(invalid.data))
                    fail("Invalid imports must not partially change the library")
                } catch (_: IllegalArgumentException) { }
                assertEquals(app.name(), db.get(app.s("key"))!!.name())
                db.importAll(JSONArray().put(update.data))
                assertEquals("Updated metadata", db.get(app.s("key"))!!.name())
            } finally { db.remove(app) }
        }
    }

    @Test fun benchmarkSequentialAndParallelApkBuilds() {
        val apps = (0..3).map { index -> WebApp().apply {
            set("baseUrl", "https://example.com/benchmark-$index/")
            manifest().put("name", "Benchmark $index")
            set("adblockRules", (1..1500).joinToString("\n") { "||ad-$it.example^" })
            validate()
        } }
        // Warm class loading, key generation, JIT and filesystem caches before measurement.
        SigningKeys.load(context)
        ApkBuilder.build(context, apps.first().copy()).delete()
        val executor = Executors.newFixedThreadPool(2)
        fun measure(parallel: Boolean): Long {
            val started = android.os.SystemClock.elapsedRealtime()
            if (parallel) executor.invokeAll(apps.map { app -> Callable { ApkBuilder.build(context, app.copy()).also { file ->
                ZipFile(file).use { zip -> assertEquals(app.name(), JSONObject(zip.getInputStream(zip.getEntry("assets/webapp.json")).bufferedReader().readText()).getString("name")) }
                file.delete()
            } } }).forEach { it.get() }
            else apps.forEach { ApkBuilder.build(context, it.copy()).delete() }
            return android.os.SystemClock.elapsedRealtime() - started
        }
        try {
            val sequential = mutableListOf<Long>(); val parallel = mutableListOf<Long>()
            repeat(3) { round -> if (round % 2 == 0) { sequential.add(measure(false)); parallel.add(measure(true)) }
                else { parallel.add(measure(true)); sequential.add(measure(false)) } }
            val result = "sequential_ms=$sequential parallel_ms=$parallel sequential_median=${sequential.sorted()[1]} parallel_median=${parallel.sorted()[1]}"
            android.util.Log.i("PwaBuildBenchmark", result)
            File(context.getExternalFilesDir(null), "build-benchmark.txt").writeText(result)
        } finally { executor.shutdownNow() }
    }

    @Test fun largeFilterSnapshotsUseChunkedStorageAndRoundTripImports() {
        val name = "large-rules-${UUID.randomUUID()}.db"
        // Exercise UTF-8 code points crossing chunk boundaries, and exceed the
        // entire default SQLite cursor window rather than only the old rule cap.
        val rules = ("! " + "界".repeat(1000) + "🐉\n").repeat(2600) + "||ads.example^\n"
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("adblockRules", rules); validate() }
        try {
            AppDatabase(context, name).use { db ->
                db.save(app)
                assertEquals(rules, db.get(app.s("key"))!!.s("adblockRules"))
                assertEquals(app.data.toString(), db.all().single().data.toString())
                db.readableDatabase.rawQuery("SELECT count(*), max(length(data)) FROM app_rules", null).use {
                    assertTrue(it.moveToFirst()); assertTrue(it.getInt(0) > 1); assertTrue(it.getInt(1) <= 128 * 1024)
                }
                val exported = JSONArray(db.all().map { it.data }).toString()
                db.remove(app)
                db.readableDatabase.rawQuery("SELECT count(*) FROM app_rules", null).use { assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0)) }
                db.importAll(JSONArray(exported))
                assertEquals(rules, db.get(app.s("key"))!!.s("adblockRules"))
                db.save(app.copy().apply { set("adblockRules", "") })
                assertEquals("", db.get(app.s("key"))!!.s("adblockRules"))
                db.readableDatabase.rawQuery("SELECT count(*) FROM app_rules", null).use { assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0)) }
                db.save(app)
            }
            AppDatabase(context, name).use { assertEquals(rules, it.get(app.s("key"))!!.s("adblockRules")) }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun versionTwoLibrariesKeepInlineRulesAndMoveThemToChunksOnSave() {
        val name = "inline-rules-${UUID.randomUUID()}.db"
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("adblockRules", "##.cookie\n||ad.example^"); validate() }
        try {
            android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
                db.execSQL("CREATE TABLE apps (id TEXT PRIMARY KEY, package TEXT NOT NULL UNIQUE, metadata TEXT NOT NULL, name TEXT NOT NULL DEFAULT '', base_url TEXT NOT NULL DEFAULT '', thumbnail BLOB)")
                db.execSQL("INSERT INTO apps (id, package, metadata) VALUES (?, ?, ?)", arrayOf(app.s("key"), app.s("packageName"), app.data.toString()))
                db.version = 2
            }
            AppDatabase(context, name).use { db ->
                val restored = db.get(app.s("key"))!!
                assertEquals(app.data.toString(), restored.data.toString())
                db.save(restored)
                db.readableDatabase.rawQuery("SELECT metadata FROM apps", null).use {
                    assertTrue(it.moveToFirst()); assertEquals("", JSONObject(it.getString(0)).getString("adblockRules"))
                }
                assertEquals(app.data.toString(), db.get(app.s("key"))!!.data.toString())
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun largeFilterSnapshotsSurviveEditorRecreationAndApkGeneration() {
        val supplied = if (InstrumentationRegistry.getArguments().getString("useRedditRules") == "true")
            File(context.getExternalFilesDir(null), "reddit-rules.txt").readText() else "||ads.example^"
        val rules = supplied + "\n" + ("! " + "界".repeat(1000) + "🐉\n").repeat(800)
        val name = "io.github.ulysseszh.pwainstaller.integrationlargerules"
        val app = WebApp().apply { set("baseUrl", fixture.url("/inside/")); set("packageName", name); set("adblockRules", rules); validate() }
        AppDatabase(context).use { it.save(app) }
        var editor: EditorActivity? = null
        try {
            val saved = AppDatabase(context).use { it.get(app.s("key"))!! }
            val progress = mutableListOf<io.github.ulysseszh.pwainstaller.core.ApkProgress>()
            val apk = ApkBuilder.build(context, saved, progress::add)
            val stages = io.github.ulysseszh.pwainstaller.core.ApkProgress.Stage.entries
            assertEquals(listOf("VALIDATING", "KEY", "FILTERS", "TEMPLATE", "ICONS", "RESOURCES", "SIGNING"), progress.map { it.stage.name }.distinct())
            for (stage in stages.filter { it.name in listOf("FILTERS", "ICONS", "RESOURCES") }) {
                val percentages = progress.filter { it.stage == stage }.mapNotNull { it.percent }
                assertEquals(0, percentages.first()); assertEquals(100, percentages.last())
                assertEquals(percentages.sorted(), percentages)
            }
            ZipFile(apk).use { zip ->
                assertEquals(rules, JSONObject(zip.getInputStream(zip.getEntry("assets/webapp.json")).bufferedReader().readText()).getString("adblockRules"))
            }
            install(apk); apk.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            awaitNative("Generated app must load with its complete large filter snapshot") { fixture.readyPaths.contains("/inside/") }
            editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("id", app.s("key"))) as EditorActivity
            awaitEditor(editor!!)
            val edited = rules + "\n! Unsaved filter change"
            val previous = editor!!
            instrumentation.runOnMainSync {
                EditorActivity::class.java.getDeclaredMethod("setRules", String::class.java).apply { isAccessible = true }.invoke(previous, edited)
                val state = Bundle()
                instrumentation.callActivityOnSaveInstanceState(previous, state)
                assertFalse(state.containsKey("adblockRules"))
                android.os.Parcel.obtain().also { parcel ->
                    try { parcel.writeBundle(state); assertTrue("Large rules must stay out of Binder state", parcel.dataSize() < 256 * 1024) }
                    finally { parcel.recycle() }
                }
                previous.recreate()
            }
            var next: EditorActivity? = null
            awaitUi("Large-rule editor must recreate") {
                next = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).filterIsInstance<EditorActivity>().singleOrNull { it !== previous }
                next != null
            }
            editor = requireNotNull(next); awaitEditor(editor!!)
            instrumentation.runOnMainSync {
                val current = EditorActivity::class.java.getDeclaredMethod("currentRules").apply { isAccessible = true }.invoke(editor) as String
                assertEquals(edited, current)
            }
            assertEquals(rules, AppDatabase(context).use { it.get(app.s("key"))!!.s("adblockRules") })
        } finally {
            instrumentation.runOnMainSync { editor?.finish() }
            AppDatabase(context).use { it.remove(app) }; shell("pm uninstall $name")
        }
    }

    @Test fun libraryMigrationBuildsSmallSummariesWithoutChangingPortableMetadata() {
        val name = "summary-migration-${UUID.randomUUID()}.db"
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); manifest().put("name", "Migration fixture").put("short_name", "Migrated"); validate() }
        try {
            context.openOrCreateDatabase(name, 0, null).use { db ->
                db.execSQL("CREATE TABLE apps (id TEXT PRIMARY KEY, package TEXT NOT NULL UNIQUE, metadata TEXT NOT NULL)")
                db.execSQL("INSERT INTO apps VALUES (?, ?, ?)", arrayOf(app.s("key"), app.s("packageName"), app.data.toString()))
                db.version = 1
            }
            AppDatabase(context, name).use { db ->
                val row = db.summaries().single()
                assertEquals(app.name(), row.name)
                assertEquals("Migrated", row.shortName)
                assertEquals(app.s("baseUrl"), row.baseUrl)
                assertNotNull(row.thumbnail)
                val image = BitmapFactory.decodeByteArray(row.thumbnail, 0, row.thumbnail!!.size)
                assertTrue(image.width <= 128 && image.height <= 128); image.recycle()
                assertEquals(app.data.toString(), db.get(app.s("key"))!!.data.toString())
                assertFalse(db.conflictingPackage(app))
                assertTrue(db.conflictingPackage(app.copy().apply { set("key", UUID.randomUUID().toString()) }))
                app.manifest().put("name", "Updated summary"); db.save(app)
                assertEquals("Updated summary", db.summaries().single().name)
                assertEquals("Migrated", db.summaries().single().shortName)
                app.manifest().put("short_name", " "); db.save(app)
                assertEquals("Updated summary", db.summaries().single().shortName)
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun settingsApplyLiveAndTilesHaveOnlyALongPressMenu() {
        val oldLayout = AppearanceSettings.layout(context)
        val oldMethod = InstallationSettings.method(context)
        val oldUrls = AdblockSettings.urls(context)
        val oldHours = AdblockSettings.hours(context)
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); manifest().put("name", "Layout fixture").put("short_name", "Layout"); validate() }
        AppDatabase(context).use { it.save(app) }
        var settings: SettingsActivity? = null
        var main: MainActivity? = null
        fun openSettings(): SettingsActivity = (instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity).also { settings = it }
        fun selection(screen: SettingsActivity, name: String) = SettingsActivity::class.java.getDeclaredField(name).apply { isAccessible = true }.get(screen) as SelectionInput
        fun item(screen: MainActivity, tiles: Boolean): com.google.android.material.card.MaterialCardView {
            val deadline = System.currentTimeMillis() + 10000
            var result: com.google.android.material.card.MaterialCardView? = null
            while (result == null && System.currentTimeMillis() < deadline) {
                instrumentation.runOnMainSync { result = if (screen.busy) null else views(screen.root).filterIsInstance<com.google.android.material.card.MaterialCardView>()
                    .firstOrNull { card -> views(card).filterIsInstance<TextView>().any { it.text.toString() == (if (tiles) "Layout" else app.name()) } &&
                        views(card).filterIsInstance<android.widget.ImageButton>().size == (if (tiles) 0 else 1) } }
                if (result == null) Thread.sleep(50)
            }
            return requireNotNull(result) { "Library item did not appear" }
        }
        try {
            val screen = openSettings()
            instrumentation.runOnMainSync {
                selection(screen, "libraryLayout").value = "tiles"
                selection(screen, "installationMethod").value = "system"
                val urls = SettingsActivity::class.java.getDeclaredField("filterUrls").apply { isAccessible = true }.get(screen) as TextView
                urls.text = fixture.url("/filters.txt")
                selection(screen, "updateInterval").value = "24"
                assertEquals(fixture.url("/filters.txt"), AdblockSettings.urls(context))
                assertEquals(24L, AdblockSettings.hours(context))
                assertEquals("tiles", AppearanceSettings.layout(context))
                // All settings, including keystore uploads, save without a Save action.
                assertEquals(0, views(screen.root).filterIsInstance<android.widget.Button>().count { it.text.toString() == context.getString(R.string.save) })
                screen.finish()
            }
            instrumentation.waitForIdleSync(); settings = null
            main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            val tile = item(main!!, tiles = true)
            instrumentation.runOnMainSync {
                assertEquals(0, tile.strokeWidth)
                val icon = views(tile).filterIsInstance<android.widget.ImageView>().single()
                assertEquals(main!!.dp(48), icon.width); assertEquals(main!!.dp(48), icon.height)
                assertTrue(views(tile).filterIsInstance<TextView>().any { it.text.toString() == "Layout" })
                assertTrue(views(tile).none { it is android.widget.ImageButton })
                tile.performClick() // Uninstalled tile must not open its menu on tap.
            }
            instrumentation.waitForIdleSync()
            assertTrue(instrumentation.uiAutomation.rootInActiveWindow.findAccessibilityNodeInfosByText(context.getString(R.string.edit)).isEmpty())
            val workerStarted = CountDownLatch(1)
            val releaseWorker = CountDownLatch(1)
            InstallerApplication.worker.execute { workerStarted.countDown(); releaseWorker.await(15, TimeUnit.SECONDS) }
            assertTrue(workerStarted.await(5, TimeUnit.SECONDS))
            try {
                instrumentation.runOnMainSync { assertTrue(tile.performLongClick()); assertFalse(main!!.busy) }
                awaitNative("Menu must open without loading full metadata on the worker") {
                    activeNativeNodes().any { it.text?.toString() == context.getString(R.string.edit) }
                }
            } finally { releaseWorker.countDown() }
            val deadline = System.currentTimeMillis() + 10000
            while (instrumentation.uiAutomation.rootInActiveWindow.findAccessibilityNodeInfosByText(context.getString(R.string.edit)).isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(50)
            val popupRoot = instrumentation.uiAutomation.rootInActiveWindow
            if (popupRoot.findAccessibilityNodeInfosByText(context.getString(R.string.edit)).isEmpty()) {
                val image = instrumentation.uiAutomation.takeScreenshot()
                File(context.getExternalFilesDir(null), "tile-menu-failure.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
                fun nodes(node: AccessibilityNodeInfo): List<String> = buildList { add("${node.className}: ${node.text}"); repeat(node.childCount) { node.getChild(it)?.let { addAll(nodes(it)) } } }
                fail("Tile menu is not accessible: ${nodes(popupRoot)}")
            }
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            val next = openSettings()
            instrumentation.runOnMainSync { selection(next, "libraryLayout").value = "list"; next.finish() }
            instrumentation.waitForIdleSync(); settings = null
            val listItem = item(main!!, tiles = false)
            instrumentation.runOnMainSync {
                assertEquals(0, listItem.strokeWidth)
                val icon = views(listItem).filterIsInstance<android.widget.ImageView>().first { it !is android.widget.ImageButton }
                assertEquals(main!!.dp(48), icon.width); assertEquals(main!!.dp(48), icon.height)
                assertTrue(views(listItem).filterIsInstance<TextView>().any { it.text.toString() == app.name() })
                assertEquals(1, views(listItem).filterIsInstance<android.widget.ImageButton>().size)
                assertTrue(views(listItem).filterIsInstance<android.widget.ImageButton>().single().contentDescription.startsWith(context.getString(R.string.more)))
            }
        } finally {
            instrumentation.runOnMainSync { settings?.finish(); main?.finish() }
            instrumentation.waitForIdleSync()
            AppearanceSettings.save(context, oldLayout); InstallationSettings.save(context, oldMethod); AdblockSettings.save(context, oldUrls, oldHours)
            AppDatabase(context).use { it.remove(app) }
        }
    }

    @Test fun retriesBasicAuthenticationWithoutLeakingCredentialsToAnotherOrigin() {
        val app = WebApp().apply { set("baseUrl", fixture.url("/basic/")) }
        try { ManifestFetcher.fetch(app); fail("Expected Basic authentication challenge") } catch (_: ManifestFetcher.CredentialsRequired) { }
        BrowserCredentials.remember(fixture.url("/"), "fixture", "user", "secret")
        assertNotNull(BrowserCredentials.authorization(fixture.url("/other"), "fixture"))
        assertNull(BrowserCredentials.authorization("http://localhost:${fixture.port}/", "fixture"))
        assertNull(BrowserCredentials.authorization("https://127.0.0.1:${fixture.port}/", "fixture"))
        assertNull(BrowserCredentials.authorization(fixture.url("/"), "another realm"))
        assertEquals("Device fixture", ManifestFetcher.fetch(app).name())
    }

    @Test fun webViewBasicSignInSuppliesCredentialsForTheManifestRetry() {
        val app = WebApp().apply { set("baseUrl", fixture.url("/basic/")); set("userAgent", "PwaFixture/Login") }
        val token = LoginSessions.begin(context, app.s("key"))
        context.startActivity(LoginSessions.intent(context, app, token).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            fun descendants(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> = buildList {
                add(node)
                repeat(node.childCount) { index -> node.getChild(index)?.let { addAll(descendants(it)) } }
            }
            val deadline = System.currentTimeMillis() + 15000
            var fields = emptyList<AccessibilityNodeInfo>()
            var signIn: AccessibilityNodeInfo? = null
            while (System.currentTimeMillis() < deadline) {
                val visible = instrumentation.uiAutomation.rootInActiveWindow?.let(::descendants).orEmpty()
                fields = visible.filter { it.className?.toString() == "android.widget.EditText" }
                signIn = visible.find { it.className?.toString() == "android.widget.Button" && it.text?.toString().equals("Sign in", true) }
                if (fields.size == 2 && signIn != null) break
                Thread.sleep(100)
            }
            assertEquals("Expected the native HTTP authentication dialog", 2, fields.size)
            assertNotNull(signIn)
            listOf("user", "secret").forEachIndexed { index, value ->
                assertTrue(fields[index].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,
                    Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) }))
            }
            assertTrue(signIn!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            assertTrue("The authenticated website did not load", fixture.firstLoaded.await(20, TimeUnit.SECONDS))
            assertEquals("PwaFixture/Login", fixture.lastUserAgent.get())
            assertEquals("Device fixture", ManifestFetcher.fetch(app, ManifestFetcher.Session(context, token)).name())
            LoginSessions.call(context, token, "finish")
        } finally { LoginSessions.discard(context, token) }
    }

    @Test fun manualCreationDiscoversAndDecodesTheWebsiteFavicon() {
        val oldUrls = AdblockSettings.urls(context); val oldHours = AdblockSettings.hours(context)
        var editor: EditorActivity? = null
        val url = fixture.url("/icon-fallback/manual/")
        try {
            AdblockSettings.save(context, "", 0)
            editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).putExtra("baseUrl", url)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as EditorActivity
                awaitEditor(editor)
            val screen = editor
            instrumentation.runOnMainSync {
                val boxes = views(screen.root).filterIsInstance<com.google.android.material.checkbox.MaterialCheckBox>()
                boxes.single { it.text.toString() == context.getString(R.string.use_manifest) }.isChecked = false
                boxes.single { it.text.toString() == context.getString(R.string.install_added) }.isChecked = false
                screen.toolbar.menu.performIdentifierAction(android.R.id.button1, 0)
            }
            var saved: WebApp? = null
            val deadline = System.currentTimeMillis() + 15000
            while (saved == null && System.currentTimeMillis() < deadline) {
                saved = AppDatabase(context).use { db -> db.all().find { it.s("baseUrl") == url } }
                if (saved == null) Thread.sleep(100)
            }
            val app = requireNotNull(saved) { "Manual app was not saved" }
            assertEquals(fixture.url("/favicon.ico"), app.icons().getJSONObject(0).getString("src"))
            assertEquals("16x16", app.icons().getJSONObject(0).getString("sizes"))
            val bytes = IconData.decode(app.s("icon"))
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertEquals(Color.rgb(36, 163, 105), bitmap.getPixel(0, 0)); bitmap.recycle()
        } finally {
            editor?.let { screen -> instrumentation.runOnMainSync { screen.finish() } }
            AppDatabase(context).use { db -> db.all().filter { it.s("baseUrl") == url }.forEach(db::remove) }
            AdblockSettings.save(context, oldUrls, oldHours)
        }
    }

    @Test fun newAppCreationDoesNotWaitForRuleSuggestionsOrMissingFilterDownloads() {
        val previousUrls = AdblockSettings.urls(context); val previousHours = AdblockSettings.hours(context)
        val filterPath = "/not-needed-to-create.txt"
        AdblockSettings.save(context, fixture.url(filterPath), 0)
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as EditorActivity
        val locked = CountDownLatch(2); val release = CountDownLatch(1)
        var key = ""
        try {
            awaitEditor(editor)
            repeat(2) { InstallerApplication.readers.execute { locked.countDown(); release.await(30, TimeUnit.SECONDS) } }
            assertTrue(locked.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                val base = editorInput(editor, "base")
                val url = fixture.url("/")
                for (length in 1..url.length) base.setText(url.take(length))
                editorInput(editor, "manifestUrl").setText(fixture.url("/slow-manifest.json"))
                (EditorActivity::class.java.getDeclaredField("install").apply { isAccessible = true }.get(editor) as android.widget.CheckBox).isChecked = false
                key = (EditorActivity::class.java.getDeclaredField("app").apply { isAccessible = true }.get(editor) as WebApp).s("key")
                editor.toolbar.menu.performIdentifierAction(android.R.id.button1, 0)
            }
            assertTrue("Manifest fetching must start while rule-suggestion workers are unavailable", fixture.fetchStarted.await(5, TimeUnit.SECONDS))
            fixture.continueFetch.countDown()
            awaitUi("Missing filter lists must not prevent creating the app") { editor.isFinishing }
            val saved = AppDatabase(context).use { it.get(key) }
            assertEquals("Progress fixture", saved?.name()); assertEquals("", saved?.s("adblockRules"))
            assertFalse(fixture.requestedPaths.contains(filterPath))
        } finally {
            release.countDown(); fixture.continueFetch.countDown()
            instrumentation.runOnMainSync {
                editor.finish()
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .filterIsInstance<MainActivity>().forEach { it.finish() }
            }
            if (key.isNotEmpty()) AppDatabase(context).use { it.get(key)?.let(it::remove) }
            AdblockSettings.save(context, previousUrls, previousHours)
        }
    }

    @Test fun webFetchingDisplaysDownloadProgressUntilTheManifestArrives() {
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("manifestUrl", fixture.url("/slow-manifest.json"))
            set("install", false); set("adblockRules", ""); validate()
        }
        AppDatabase(context).use { it.save(app) }
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).putExtra("id", app.s("key"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as EditorActivity
            awaitEditor(editor)
        try {
            instrumentation.runOnMainSync {
                views(editor.root).filterIsInstance<TextView>().single { it.text.toString() == context.getString(R.string.fetch_manifest) }.performClick()
            }
            clickText(context.getString(R.string.fetch_manifest))
            assertTrue(fixture.fetchStarted.await(15, TimeUnit.SECONDS))
            val label = context.getString(R.string.fetch_manifest_progress) + " · 50%"
            val deadline = System.currentTimeMillis() + 10000
            while (instrumentation.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText(label)?.isNotEmpty() != true && System.currentTimeMillis() < deadline) Thread.sleep(50)
            assertTrue("Manifest download percentage should be visible", instrumentation.uiAutomation.rootInActiveWindow!!.findAccessibilityNodeInfosByText(label).isNotEmpty())
            fixture.continueFetch.countDown()
            var busy = true
            val completedDeadline = System.currentTimeMillis() + 15000
            while (busy && System.currentTimeMillis() < completedDeadline) {
                Thread.sleep(50); instrumentation.runOnMainSync { busy = editor.busy }
            }
            instrumentation.runOnMainSync {
                assertFalse(editor.busy)
                assertTrue(views(editor.root).filterIsInstance<TextView>().any { it.text.toString() == "Progress fixture" })
            }
        } finally {
            fixture.continueFetch.countDown(); instrumentation.runOnMainSync { editor.finish() }
            AppDatabase(context).use { it.remove(app) }
        }
    }

    @Test fun notchPreferenceChangesTheInstalledWebViewsUsableArea() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 30)
        val overlay = "com.android.internal.display.cutout.emulation.tall"
        val overlays = shell("cmd overlay list")
        Assume.assumeTrue(overlays.contains(overlay))
        val previouslyEnabled = overlays.lineSequence().any { it.contains("[x]") && it.contains(overlay) }
        val name = "io.github.ulysseszh.pwainstaller.integrationnotch"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name)
            manifest().put("start_url", fixture.url("/cutout-check")).put("display", "fullscreen").put("orientation", "landscape-primary")
            validate()
        }
        fun launchAndMeasure(): Int {
            fixture.cutoutWidths.clear()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            val deadline = System.currentTimeMillis() + 15000
            while (fixture.cutoutWidths.isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(100)
            assertTrue("Cutout fixture did not report a viewport", fixture.cutoutWidths.isNotEmpty())
            Thread.sleep(700) // Let orientation and window insets settle.
            return fixture.cutoutWidths.last()
        }
        try {
            shell("cmd overlay enable --user 0 $overlay")
            val enabled = ApkBuilder.build(context, app); install(enabled); enabled.delete()
            val wide = launchAndMeasure()
            app.set("useNotchAreas", false)
            val disabled = ApkBuilder.build(context, app); install(disabled); disabled.delete()
            val narrow = launchAndMeasure()
            assertTrue("Using notch areas should expand the viewport: on=$wide, off=$narrow", wide > narrow)
        } finally {
            shell("pm uninstall $name")
            if (!previouslyEnabled) shell("cmd overlay disable --user 0 $overlay")
        }
    }

    private fun longPressWebElement(label: String, packageName: String) {
        fun webBounds(node: AccessibilityNodeInfo): android.graphics.Rect? {
            if (node.packageName?.toString() == packageName && node.className?.toString() == "android.webkit.WebView")
                return android.graphics.Rect().also(node::getBoundsInScreen)
            repeat(node.childCount) { node.getChild(it)?.let { child -> webBounds(child)?.let { return it } } }
            return null
        }
        val deadline = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < deadline) {
            val point = fixture.clickPoints[label]
            val bounds = instrumentation.uiAutomation.rootInActiveWindow?.let(::webBounds)
            if (point != null && bounds != null && bounds.width() > 0) {
                if (Build.VERSION.SDK_INT >= 23) activeNativeNodes().firstOrNull { it.packageName?.toString() == packageName &&
                    (it.text?.toString() == label || it.contentDescription?.toString() == label) }
                    ?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
                Thread.sleep(400) // Wait for the clipboard-reader activity's exit animation.
                val target = activeNativeNodes().firstOrNull { it.packageName?.toString() == packageName && it.isVisibleToUser &&
                    (it.text?.toString() == label || it.contentDescription?.toString() == label) }
                    ?.let { android.graphics.Rect().also(it::getBoundsInScreen) }?.takeIf { it.width() > 0 && it.height() > 0 }
                val settled = instrumentation.uiAutomation.rootInActiveWindow?.let(::webBounds) ?: continue
                val scale = settled.width() / point.getDouble("width")
                val x = target?.centerX() ?: (settled.left + point.getDouble("x") * scale).toInt()
                val y = target?.centerY() ?: (settled.top + point.getDouble("y") * scale).toInt()
                android.util.Log.d("PwaContextTest", "Long press $label at $x,$y; web=$settled")
                shell("input swipe $x $y $x $y 850"); return
            }
            Thread.sleep(50)
        }
        fail("Web element is unavailable for long press: $label")
    }
    private fun inspectClipboard(expected: (ClipData) -> Boolean, check: (ClipData) -> Unit) {
        val main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            awaitUi("Clipboard reader did not gain focus") { main.hasWindowFocus() }
            var clip: ClipData? = null
            awaitUi("Context action did not update the system clipboard") {
                clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).primaryClip
                clip?.let(expected) == true
            }
            check(requireNotNull(clip))
        } finally { instrumentation.runOnMainSync { main.finish() } }
    }
    private fun awaitNative(message: String, timeout: Long = 10000, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(50)
        }
        throw AssertionError(message)
    }
    private fun exerciseWebContextMenus(packageName: String) {
        longPressWebElement("Text link", packageName)
        awaitNative("Link menu title must show the URL") { activeNativeNodes().any { it.text?.toString() == fixture.url("/context-target") } }
        clickText(context.getString(BrowserR.string.context_copy_link), packageName)
        inspectClipboard({ it.getItemAt(0).text?.toString() == fixture.url("/context-target") }) { }
        longPressWebElement("Text link", packageName)
        clickText(context.getString(BrowserR.string.context_copy_link_text), packageName)
        inspectClipboard({ it.getItemAt(0).text?.toString() == "Context link" }) { }
        longPressWebElement("Linked image", packageName)
        clickText(context.getString(BrowserR.string.context_copy_link), packageName)
        inspectClipboard({ it.getItemAt(0).text?.toString() == fixture.url("/context-target?image") }) { }
        var previous: Uri? = null
        listOf("Standalone image", "Data image", "Blob image", "CSS banner", "Overlay banner", "Passive image", "Pseudo banner", "Shadow banner").forEach { label ->
            longPressWebElement(label, packageName)
            clickText(context.getString(BrowserR.string.context_copy_image), packageName)
            inspectClipboard({ it.getItemAt(0).uri?.let { uri -> uri != previous && uri.authority == "$packageName.images" } == true }) { clip ->
                val uri = requireNotNull(clip.getItemAt(0).uri); previous = uri
                assertArrayEquals(IconData.decode(IconData.default), context.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
                assertEquals("image/png", context.contentResolver.getType(uri))
            }
        }
        longPressWebElement("Standalone image", packageName)
        clickText(context.getString(BrowserR.string.context_copy_image_address), packageName)
        inspectClipboard({ it.getItemAt(0).text?.toString() == fixture.url("/context-image.png") }) { }
        longPressWebElement("Text link", packageName)
        clickText(context.getString(BrowserR.string.context_open_link), packageName)
        val deadline = System.currentTimeMillis() + 10000
        while (!fixture.readyPaths.contains("/context-target") && System.currentTimeMillis() < deadline) Thread.sleep(50)
        assertTrue("Open link did not navigate to the link target", fixture.readyPaths.contains("/context-target"))
    }
    @Test fun installedAppsOfferLinkAndImageContextActionsWithRealClipboardImages() {
        val name = "io.github.ulysseszh.pwainstaller.integrationcontext"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name); set("userAgent", "ContextMenu/1.0")
            manifest().put("start_url", fixture.url("/context-menu")); validate()
        }
        try {
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            exerciseWebContextMenus(name)
        } finally { shell("pm uninstall $name") }
    }
    @Test fun copyingAnImagePublishesTheClipboardBeforeDownloadingAndSurvivesClosingTheBrowser() {
        val name = "io.github.ulysseszh.pwainstaller.integrationinstantcopy"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name); set("userAgent", "InstantCopy/1.0"); set("adblockRules", "")
            manifest().put("start_url", fixture.url("/instant-image-menu")); validate()
        }
        try {
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            longPressWebElement("Slow image", name)
            clickText(context.getString(BrowserR.string.context_copy_image), name)
            assertTrue("The selected image must load in the background", fixture.imageCopyStarted.await(5, TimeUnit.SECONDS))
            inspectClipboard({ it.getItemAt(0).uri?.authority == "$name.images" }) { clip ->
                val uri = requireNotNull(clip.getItemAt(0).uri)
                assertEquals("The clipboard must be set before the download finishes", 1L, fixture.continueImageCopy.count)
                assertEquals("image/png", context.contentResolver.getType(uri))
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)!!.use {
                    assertTrue(it.moveToFirst()); assertTrue("Pending metadata must not wait for image bytes", it.isNull(0))
                }
                // Destroy the activity while the image is still downloading.
                shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
                shell("input keyevent 4")
                val bytes = CompletableFuture.supplyAsync {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                }
                try {
                    bytes.get(250, TimeUnit.MILLISECONDS)
                    fail("A partially downloaded image must not be served")
                } catch (_: TimeoutException) { }
                fixture.continueImageCopy.countDown()
                assertArrayEquals(IconData.decode(IconData.default), bytes.get(10, TimeUnit.SECONDS))
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)!!.use {
                    assertTrue(it.moveToFirst()); assertEquals(IconData.decode(IconData.default).size.toLong(), it.getLong(0))
                }
                try {
                    context.contentResolver.openOutputStream(uri, "w")?.close()
                    fail("A clipboard reader must not be able to overwrite the copied image")
                } catch (_: SecurityException) { }
                catch (_: FileNotFoundException) { }
            }
        } finally { fixture.continueImageCopy.countDown(); shell("pm uninstall $name") }
    }
    private fun activeNativeNodes(): List<AccessibilityNodeInfo> {
        fun children(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> = buildList {
            add(node); repeat(node.childCount) { node.getChild(it)?.let { addAll(children(it)) } }
        }
        return instrumentation.uiAutomation.rootInActiveWindow?.let(::children).orEmpty()
    }
    @Test fun installedAppDownloadsAndOpensImagesFromTheContextMenu() {
        val name = "io.github.ulysseszh.pwainstaller.integrationimagedownload"
        val filename = "pwa-context-${UUID.randomUUID()}.png"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name)
            manifest().put("start_url", fixture.url("/context-menu")); validate()
        }
        try {
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            longPressWebElement("Standalone image", name)
            clickText(context.getString(BrowserR.string.context_download_image), name)
            val deadline = System.currentTimeMillis() + 15000
            var picker = ""
            while (picker.isEmpty() && System.currentTimeMillis() < deadline) {
                picker = activeNativeNodes().firstOrNull { it.packageName?.toString()?.endsWith(".documentsui") == true }?.packageName?.toString().orEmpty()
                if (picker.isEmpty()) Thread.sleep(50)
            }
            assertTrue("Download image must open the system save dialog", picker.isNotEmpty())
            clickText("Show roots", picker); clickText("Downloads", picker)
            val input = activeNativeNodes().first { it.className?.toString() == "android.widget.EditText" }
            assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, filename)
            }))
            clickText("Save", picker)
            val savedDeadline = System.currentTimeMillis() + 15000
            var saved = false
            while (!saved && System.currentTimeMillis() < savedDeadline) {
                saved = shell("base64 /sdcard/Download/$filename").replace(Regex("\\s"), "") == IconData.default
                if (!saved) Thread.sleep(100)
            }
            assertTrue("Image bytes were not saved to the selected Downloads file", saved)
            longPressWebElement("Standalone image", name)
            clickText(context.getString(BrowserR.string.context_open_image), name)
            val openedDeadline = System.currentTimeMillis() + 10000
            var opened = false
            while (!opened && System.currentTimeMillis() < openedDeadline) {
                opened = activeNativeNodes().count { it.packageName?.toString() == name && it.className?.toString() == "android.widget.Image" } == 1
                if (!opened) Thread.sleep(50)
            }
            assertTrue("Open image did not show the image document", opened)
        } finally { shell("rm -f /sdcard/Download/$filename"); shell("pm uninstall $name") }
    }

    @Test fun temporaryBrowserOffersTheSameLinkAndImageContextActions() {
        val app = WebApp().apply { set("baseUrl", fixture.url("/context-menu")); set("userAgent", "ContextMenu/Login") }
        val session = LoginSessions.begin(context, app.s("key"))
        try {
            context.startActivity(LoginSessions.intent(context, app, session).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            exerciseWebContextMenus(context.packageName)
        } finally { LoginSessions.discard(context, session) }
    }

    @Test fun installedDisplayModeControlsNativeChromeAndMatchesInJavaScriptAndStyles() {
        val name = "io.github.ulysseszh.pwainstaller.integrationdisplay"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name)
            manifest().put("start_url", fixture.url("/display-check"))
            validate()
        }
        try {
            listOf("", "standalone", "minimal-ui", "browser", "fullscreen", "override-browser", "override-fullscreen").forEach { display ->
                val mode = display.removePrefix("override-").ifEmpty { "standalone" }
                app.manifest().put("display", if (display.startsWith("override-")) "minimal-ui" else display)
                if (display.startsWith("override-")) app.manifest().put("display_override", org.json.JSONArray(listOf("unsupported", mode)))
                else app.manifest().remove("display_override")
                fixture.displayResult.set(null)
                val apk = ApkBuilder.build(context, app)
                install(apk); apk.delete()
                shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
                val deadline = System.currentTimeMillis() + 20000
                while (fixture.displayResult.get() == null && System.currentTimeMillis() < deadline) Thread.sleep(100)
                val result = requireNotNull(fixture.displayResult.get()) { "No display-mode report for $display" }
                assertEquals(result.toString(), mode, result.getString("mode"))
                listOf("inline", "external", "attribute", "dynamic", "nativeQueries", "mediaText").forEach { key -> assertTrue("$display $key: $result", result.getBoolean(key)) }
                // Recent WebViews support injection before the page's first script.
                if (Build.VERSION.SDK_INT >= 35) assertTrue(result.toString(), result.getBoolean("early"))
                fun nodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> = buildList { add(node); repeat(node.childCount) { node.getChild(it)?.let { addAll(nodes(it)) } } }
                val root = instrumentation.uiAutomation.rootInActiveWindow
                assertEquals("$display toolbar visibility", mode == "browser", nodes(root).any { it.className.toString() == "android.widget.EditText" })
                shell("am force-stop $name")
            }
        } finally { shell("pm uninstall $name") }
    }

    @Test fun temporaryBrowserToolbarNavigatesRefreshesAndCompletesSignIn() {
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("userAgent", "MaterialLogin/1") }
        val token = LoginSessions.begin(context, app.s("key"))
        try {
            context.startActivity(LoginSessions.intent(context, app, token).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            assertTrue(fixture.firstLoaded.await(20, TimeUnit.SECONDS))
            awaitNative("Temporary browser must use its descriptive title") {
                activeNativeNodes().any { it.text?.toString() == context.getString(R.string.temporary_browser) }
            }
            fun descendants(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> = buildList {
                add(node); repeat(node.childCount) { node.getChild(it)?.let { child -> addAll(descendants(child)) } }
            }
            fun addressNode(): AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow?.let(::descendants)
                ?.singleOrNull { it.className?.toString() == "android.widget.EditText" && it.packageName?.toString() == context.packageName }
            val addressDeadline = System.currentTimeMillis() + 10000
            while (addressNode() == null && System.currentTimeMillis() < addressDeadline) Thread.sleep(100)
            val address = requireNotNull(addressNode())
            assertTrue(address.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, fixture.url("/inside/allowed"))
            }))
            clickText(context.getString(io.github.ulysseszh.pwainstaller.browser.R.string.go))
            val deadline = System.currentTimeMillis() + 15000
            while (fixture.navigationAgents["/inside/allowed"] == null && System.currentTimeMillis() < deadline) Thread.sleep(100)
            assertEquals("MaterialLogin/1", fixture.navigationAgents["/inside/allowed"])
            clickText(context.getString(io.github.ulysseszh.pwainstaller.browser.R.string.back))
            fun addressText() = addressNode()?.text?.toString()?.substringBefore(", " + context.getString(io.github.ulysseszh.pwainstaller.browser.R.string.address))
            val backDeadline = System.currentTimeMillis() + 10000
            while (addressText() != fixture.url("/") && System.currentTimeMillis() < backDeadline) Thread.sleep(100)
            assertEquals(fixture.url("/"), addressText())
            clickText(context.getString(io.github.ulysseszh.pwainstaller.browser.R.string.refresh))
            val screenshot = File(context.getExternalFilesDir(null), "material-browser.png")
            instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
                screenshot.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
            }
            shell("cp ${screenshot.absolutePath} /data/local/tmp/pwa-material-browser.png")
            clickText(context.getString(io.github.ulysseszh.pwainstaller.browser.R.string.done))
            val completedDeadline = System.currentTimeMillis() + 10000
            while (LoginSessions.current(context)?.optBoolean("completed") != true && System.currentTimeMillis() < completedDeadline) Thread.sleep(100)
            assertTrue(LoginSessions.current(context)!!.getBoolean("completed"))
        } finally { LoginSessions.discard(context, token) }
    }

    @Test fun createsInstallsLaunchesAndMigratesAnIsolatedWebViewProfile() {
        val first = "io.github.ulysseszh.pwainstaller.integrationfirst"
        val second = "io.github.ulysseszh.pwainstaller.integrationsecond"
        try {
            val app = ManifestFetcher.fetch(WebApp().apply { set("baseUrl", fixture.url("/")); set("packageName", first) })
            app.set("webViewDebugging", true)
            app.manifest().put("start_url", fixture.url("/?app=first"))
            val apk = ApkBuilder.build(context, app)
            val archive = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            assertNotNull(archive)
            assertEquals(first, archive!!.packageName)
            assertEquals(1, archive.versionCode)
            assertEquals("1.0.0", archive.versionName)
            install(apk); apk.delete()
            shell("am start -n $first/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            assertTrue("First PWA did not load", fixture.firstLoaded.await(30, TimeUnit.SECONDS))
            assertTrue("Storage did not persist", fixture.storageReady.await(15, TimeUnit.SECONDS))
            assertDebuggingSocket(first, true)
            val profile = context.contentResolver.openInputStream(Packages.profile(first))!!.use { ManifestFetcher.read(it, 32 * 1024 * 1024) }
            var cookies = false
            var storage = false
            ZipInputStream(profile.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    cookies = cookies || entry.name.endsWith("Cookies")
                    storage = storage || entry.name.contains("Local Storage")
                }
            }
            assertTrue("Cookie database missing", cookies)
            assertTrue("Local storage missing", storage)
            val denied = shell("content read --uri content://$first.data/profile")
            assertTrue("Shell must not access profiles: $denied", denied.contains("SecurityException") || denied.contains("Installer identity required"))
            val migrated = app.copy().apply { set("packageName", second); set("webViewDebugging", false); manifest().put("start_url", fixture.url("/?app=second")) }
            val next = ApkBuilder.build(context, migrated)
            ZipFile(next).use { zip ->
                val config = JSONObject(zip.getInputStream(zip.getEntry("assets/webapp.json")).bufferedReader().readText())
                assertFalse(config.getBoolean("webViewDebugging"))
            }
            install(next); next.delete()
            context.contentResolver.openOutputStream(Packages.profile(second), "w")!!.use { it.write(profile) }
            context.contentResolver.call(Packages.profile(second), "commit", null, null)
            shell("am start -n $second/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            assertTrue("Migrated localStorage/cookie/IndexedDB were not restored", fixture.restored.await(30, TimeUnit.SECONDS))
            assertDebuggingSocket(second, false)
        } finally { shell("pm uninstall $first"); shell("pm uninstall $second") }
    }

    private fun assertDebuggingSocket(name: String, enabled: Boolean) {
        val processes = shell(if (Build.VERSION.SDK_INT >= 26) "ps -A" else "ps")
        val process = processes.lineSequence().firstOrNull { it.trim().endsWith("$name:web") }
        assertNotNull(processes, process)
        val pid = process!!.trim().split(Regex("\\s+"))[1]
        // Chromium deliberately forces debugging on userdebug/eng Android builds.
        val forcedBySystem = shell("getprop ro.debuggable").trim() == "1"
        assertEquals("Debug socket for $name", enabled || forcedBySystem,
            shell("cat /proc/net/unix").lineSequence().any { it.trimEnd().endsWith("@webview_devtools_remote_$pid") })
    }

    private fun anotherIdentity(): SigningKeys.Identity {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val provider = BouncyCastleProvider()
        val name = X500Principal("CN=Different key integration test")
        val cert = JcaX509CertificateConverter().setProvider(provider).getCertificate(
            JcaX509v3CertificateBuilder(name, BigInteger.ONE, Date(0), Date(System.currentTimeMillis() + 86400000), name, pair.public)
                .build(JcaContentSignerBuilder("SHA256withRSA").setProvider(provider).build(pair.private)))
        return SigningKeys.Identity(pair.private, listOf(cert))
    }

    @Test fun signatureReplacementBacksUpBeforeUninstallAndRestoresAfterAnInterruptedRetry() {
        val name = "io.github.ulysseszh.pwainstaller.integrationsignature"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name)
            manifest().put("start_url", fixture.url("/?app=first")); validate()
        }
        var token: String? = null
        try {
            val original = ApkBuilder.assemble(context.assets.open("runtime.apk"), File(context.cacheDir, "apks"), app, anotherIdentity(), ApkBuilder.installerCertificate(context))
            install(original); original.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            assertTrue(fixture.firstLoaded.await(30, TimeUnit.SECONDS))
            assertTrue(fixture.storageReady.await(15, TimeUnit.SECONDS))
            token = InstallCoordinator.prepare(context, app, null)
            InstallCoordinator.start(context, token)
            assertEquals("signature_conflict", InstallCoordinator.read(context, token).getString("state"))
            assertTrue(Packages.installed(context, name))
            InstallCoordinator.replaceSignature(context, token)
            val plan = InstallCoordinator.read(context, token)
            assertEquals("replacement_uninstall", plan.getString("state"))
            val backup = File(plan.getString("snapshot"))
            assertTrue(backup.isFile && backup.length() > 0)
            assertTrue(Packages.installed(context, name))
            shell("pm uninstall $name")
            assertFalse(Packages.installed(context, name))
            InstallCoordinator.fail(context, token, "Interrupted installation")
            assertEquals("replacement_failed", InstallCoordinator.read(context, token).getString("state"))
            assertTrue("Recovery must retain the backup", backup.isFile)
            try { InstallCoordinator.discard(context, token); fail("Cancel must not discard the only recovery backup") }
            catch (_: IllegalStateException) { }
            assertTrue(backup.isFile)
            InstallCoordinator.resumeReplacement(context, token)
            assertEquals("prepared", InstallCoordinator.read(context, token).getString("state"))
            install(File(plan.getString("apk")))
            InstallCoordinator.installed(context, token)
            InstallCoordinator.complete(context, token)
            assertEquals("done", InstallCoordinator.read(context, token).getString("state"))
            assertFalse("Delete backup only after successful restore", backup.exists())
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity -a android.intent.action.VIEW -d '${fixture.url("/?app=second")}'")
            assertTrue("Signature replacement lost browsing data", fixture.restored.await(30, TimeUnit.SECONDS))
        } finally {
            token?.let { InstallCoordinator.discard(context, it, discardBackup = true) }
            AppDatabase(context).use { it.remove(app) }
            shell("pm uninstall $name")
        }
    }

    @Test fun rejectsTheInstallerWhenItsCertificateDoesNotMatchThePin() {
        val name = "io.github.ulysseszh.pwainstaller.integrationbadpin"
        var token: String? = null
        try {
            val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("packageName", name); validate() }
            val apk = ApkBuilder.assemble(context.assets.open("runtime.apk"), File(context.cacheDir, "apks"),
                app, anotherIdentity(), "0".repeat(64))
            install(apk)
            apk.delete()
            try {
                context.contentResolver.openInputStream(Packages.profile(name))?.close()
                fail("A matching installer package with the wrong certificate must be denied")
            } catch (expected: SecurityException) {
                assertTrue(expected.toString(), expected.message.orEmpty().contains("certificate mismatch"))
            }
            token = InstallCoordinator.prepare(context, app, null)
            InstallCoordinator.start(context, token)
            assertEquals("signature_conflict", InstallCoordinator.read(context, token).getString("state"))
            InstallCoordinator.replaceSignature(context, token)
            assertEquals("backup_failed", InstallCoordinator.read(context, token).getString("state"))
            assertTrue("Backup failure must leave the installed app in place", Packages.installed(context, name))
            InstallCoordinator.replaceSignature(context, token, withoutData = true)
            val recovery = InstallCoordinator.read(context, token)
            assertEquals("replacement_uninstall", recovery.getString("state"))
            assertFalse(recovery.has("snapshot"))
            assertTrue(recovery.getBoolean("skipSnapshot"))
            assertTrue("Removal still needs user confirmation", Packages.installed(context, name))
        } finally { token?.let { InstallCoordinator.discard(context, it, discardBackup = true) }; shell("pm uninstall $name") }
    }

    private fun clickText(text: String, packageName: String = context.packageName) {
        fun descendants(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> = buildList {
            add(node); repeat(node.childCount) { node.getChild(it)?.let { child -> addAll(descendants(child)) } }
        }
        val deadline = System.currentTimeMillis() + 10000
        var visible = emptyList<AccessibilityNodeInfo>()
        while (System.currentTimeMillis() < deadline) {
            visible = instrumentation.uiAutomation.rootInActiveWindow?.let(::descendants).orEmpty()
            val matches = visible.filter { it.packageName?.toString() == packageName && (it.text?.toString().equals(text, true) || it.contentDescription?.toString().equals(text, true)) }
            if (matches
                    .any { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }) return
            matches.firstOrNull { it.isVisibleToUser }?.let { node ->
                val bounds = android.graphics.Rect(); node.getBoundsInScreen(bounds)
                if (bounds.width() > 0 && bounds.height() > 0) {
                    shell("input tap ${bounds.centerX()} ${bounds.centerY()}"); return
                }
            }
            // Old WebViews expose only their native surface. The fixture reports DOM bounds
            // over HTTP, allowing a real touch without assuming a viewport scale or density.
            val web = visible.firstOrNull { it.className?.toString() == "android.webkit.WebView" && it.packageName?.toString() == packageName }
            if (web != null) {
                val bounds = android.graphics.Rect(); web.getBoundsInScreen(bounds)
                val point = fixture.clickPoints[text]
                if (bounds.width() > 0 && bounds.height() > 0 && point != null) {
                    val scale = bounds.width().toDouble() / point.getDouble("width")
                    shell("input tap ${(bounds.left + point.getDouble("x") * scale).toInt()} ${(bounds.top + point.getDouble("y") * scale).toInt()}")
                    return
                }
            }
            Thread.sleep(100)
        }
        fail("No clickable text: $text; visible=${visible.map { it.className.toString() + ':' + it.text }}")
    }

    @Test fun signInStartsCleanAndImmediateInstallationImportsItsCompleteProfile() {
        val name = "io.github.ulysseszh.pwainstaller.integrationlogin"
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("packageName", name); set("userAgent", "PwaLogin/4.0") }
        var session: String? = null
        var operation: String? = null
        try {
            // Seed the previous sign-in browser with cookies, localStorage and IndexedDB.
            val previous = LoginSessions.begin(context, app.s("key")); session = previous
            context.startActivity(LoginSessions.intent(context, app, previous).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            assertTrue(fixture.storageReady.await(30, TimeUnit.SECONDS))
            LoginSessions.call(context, previous, "finish")
            app.set("baseUrl", fixture.url("/soft-auth"))
            try { ManifestFetcher.fetch(app); fail("A page without a manifest must offer sign-in") }
            catch (_: ManifestFetcher.ManifestNotFound) { }
            val token = LoginSessions.begin(context, app.s("key")); session = token
            try { LoginSessions.call(context, previous, "cookie", fixture.url("/")); fail("An expired session must be rejected") }
            catch (_: IllegalArgumentException) { }
            context.startActivity(LoginSessions.intent(context, app, token).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            assertTrue("Second sign-in did not check its profile", fixture.cleanChecked.await(30, TimeUnit.SECONDS))
            assertEquals("/clean?state=true", fixture.cleanState.get())
            clickText("Log in")
            assertTrue("Sign-in storage was not written", fixture.loginReady.await(30, TimeUnit.SECONDS))
            LoginSessions.call(context, token, "finish")
            val fetched = ManifestFetcher.fetch(app, ManifestFetcher.Session(context, token)).apply {
                set("urlPatterns", fixture.url("/*")); manifest().put("start_url", fixture.url("/?app=second")); validate()
            }
            assertEquals("PwaLogin/4.0", fixture.lastUserAgent.get())
            val profile = LoginSessions.seal(context, token, fetched.s("key")); session = null
            ZipFile(profile).use { zip ->
                val names = zip.entries().asSequence().map { it.name }.toList()
                assertTrue(names.toString(), names.any { it.endsWith("Cookies") })
                assertTrue(names.toString(), names.any { it.contains("Local Storage") })
                assertTrue(names.toString(), names.any { it.contains("IndexedDB") })
                assertFalse(names.toString(), names.any { it.contains("webapps.db") || it.contains("signing.xml") || it.contains("operations.xml") })
            }
            operation = InstallCoordinator.prepare(context, fetched, null, profile)
            assertFalse(profile.exists())
            val backup = File(InstallCoordinator.read(context, operation).getString("loginSnapshot"))
            InstallCoordinator.fail(context, operation, "Interrupted before installation")
            try { InstallCoordinator.discard(context, operation); fail("Must retain temporary browsing data for retry") }
            catch (_: IllegalStateException) { }
            assertTrue(backup.isFile)
            InstallCoordinator.resumeReplacement(context, operation)
            val apk = ApkBuilder.build(context, fetched); install(apk); apk.delete()
            InstallCoordinator.installed(context, operation); InstallCoordinator.complete(context, operation)
            assertEquals("done", InstallCoordinator.read(context, operation).getString("state"))
            assertFalse(backup.exists())
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            assertTrue("Temporary cookies/localStorage/IndexedDB did not migrate", fixture.restored.await(30, TimeUnit.SECONDS))
        } finally {
            session?.let { LoginSessions.discard(context, it) }
            operation?.let { InstallCoordinator.discard(context, it, discardBackup = true) }
            AppDatabase(context).use { it.remove(app) }; shell("pm uninstall $name")
        }
    }

    @Test fun installedWebAppsShowThinPageLoadingProgress() {
        val name = "io.github.ulysseszh.pwainstaller.integrationpageprogress"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name)
            manifest().put("start_url", fixture.url("/slow-page")); validate()
        }
        fun loadingBar() = activeNativeNodes().firstOrNull {
            it.packageName?.toString() == name && it.isVisibleToUser &&
                it.className?.toString() == "android.widget.ProgressBar" &&
                it.contentDescription?.toString() == context.getString(BrowserR.string.loading_page)
        }
        try {
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            assertTrue("Page must remain loading while its script is held", fixture.pageResourceStarted.await(15, TimeUnit.SECONDS))
            awaitNative("A thin progress bar must be visible during page loading") { loadingBar() != null }
            val bar = requireNotNull(loadingBar())
            val bounds = android.graphics.Rect().also(bar::getBoundsInScreen)
            val webView = activeNativeNodes().first { it.packageName?.toString() == name && it.className?.toString() == "android.webkit.WebView" }
            val pageBounds = android.graphics.Rect().also(webView::getBoundsInScreen)
            assertEquals("Progress must overlay the top of the page", pageBounds.top, bounds.top)
            assertTrue("Progress must be thin", bounds.height() in 1..(3 * context.resources.displayMetrics.density).toInt())
            assertEquals(pageBounds.width(), bounds.width())
            awaitNative("The progress indicator must render visibly even with the default white app theme") {
                val image = instrumentation.uiAutomation.takeScreenshot() ?: return@awaitNative false
                try {
                    (bounds.top until bounds.bottom).any { y -> (bounds.left until bounds.right).any { x ->
                        val pixel = image.getPixel(x, y)
                        Color.blue(pixel) > Color.red(pixel) + 40 && Color.blue(pixel) > Color.green(pixel)
                    } }
                } finally { image.recycle() }
            }
            assertFalse("Web apps must have no address field", activeNativeNodes().any {
                it.packageName?.toString() == name && it.className?.toString() == "android.widget.EditText"
            })
            fixture.continuePage.countDown()
            awaitNative("Page must finish loading") { fixture.readyPaths.contains("/slow-page") }
            awaitNative("Progress must disappear when loading finishes") { loadingBar() == null }
        } finally { fixture.continuePage.countDown(); shell("pm uninstall $name") }
    }

    @Test fun reconnectNotifiesWebViewContextObserversEvenWithoutAConfigurationChange() {
        val screen = instrumentation.startActivitySync(Intent(context, BrowserTestActivity::class.java)
            .putExtra("url", fixture.url("/window-sizing")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as BrowserTestActivity
        val changes = java.util.concurrent.atomic.AtomicInteger()
        val callback = object : android.content.ComponentCallbacks {
            override fun onConfigurationChanged(configuration: android.content.res.Configuration) { changes.incrementAndGet() }
            override fun onLowMemory() = Unit
        }
        try {
            awaitNative("The display fixture must load") { fixture.viewportStates.isNotEmpty() }
            val token = fixture.viewportStates.last().getString("token")
            instrumentation.runOnMainSync {
                screen.registerComponentCallbacks(callback)
                // A desktop display may stop/resume a window with identical metrics.
                // Chromium listens on the context, which a View-only dispatch misses.
                instrumentation.callActivityOnPause(screen)
                instrumentation.callActivityOnResume(screen)
                assertEquals(1, changes.get())
                screen.unregisterComponentCallbacks(callback)
                instrumentation.callActivityOnPause(screen)
                instrumentation.callActivityOnResume(screen)
                assertEquals("Unregistered callbacks must stay unregistered", 1, changes.get())
            }
            awaitNative("The existing page must keep reporting after resuming") { fixture.viewportStates.size >= 3 }
            assertEquals(token, fixture.viewportStates.last().getString("token"))
            assertEquals(1, fixture.requestedPaths.count { it == "/window-sizing" })
        } finally { instrumentation.runOnMainSync { screen.unregisterComponentCallbacks(callback); screen.finish() } }
    }

    @Test fun nativeRendererDisplayRepairRetainsTheDocumentAndFormState() {
        val version = androidx.webkit.WebViewCompat.getCurrentWebViewPackage(context)?.versionName
            ?.substringBefore('.')?.toIntOrNull() ?: 0
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 30 && version in 156..157)
        val screen = instrumentation.startActivitySync(Intent(context, BrowserTestActivity::class.java)
            .putExtra("url", fixture.url("/window-sizing")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as BrowserTestActivity
        try {
            awaitNative("The display fixture must load") { fixture.viewportStates.isNotEmpty() }
            val initial = fixture.viewportStates.last()
            val count = fixture.viewportStates.size
            instrumentation.runOnMainSync {
                val field = io.github.ulysseszh.pwainstaller.browser.BrowserActivity::class.java
                    .getDeclaredField("browserConfiguration").apply { isAccessible = true }
                val observers = field.get(screen)
                val repaired = observers.javaClass.getDeclaredMethod("reconnectRendererDisplays").invoke(observers) as Int
                assertEquals("The current Chromium provider's native page must be reattached", 1, repaired)
            }
            awaitNative("The original document must continue reporting after native reattachment") {
                fixture.viewportStates.size >= count + 2
            }
            val state = fixture.viewportStates.last()
            assertEquals(initial.getString("token"), state.getString("token"))
            assertEquals("Unsaved form text", state.getString("input"))
            assertTrue("Native reattachment must retain scrolling", state.getDouble("scroll") >= 250)
            assertEquals(screen.resources.displayMetrics.density.toDouble(), state.getDouble("ratio"), .01)
            assertEquals("The main document must not reload", 1, fixture.requestedPaths.count { it == "/window-sizing" })
        } finally { instrumentation.runOnMainSync { screen.finish() } }
    }

    @Test fun nativeInsetsReachWebViewAsZeroUpdatesInsteadOfBeingConsumed() {
        val screen = instrumentation.startActivitySync(Intent(context, BrowserTestActivity::class.java)
            .putExtra("url", fixture.url("/media-lifecycle")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as BrowserTestActivity
        try {
            awaitUi("The browser must be laid out") { screen.page.width > 0 && screen.page.progress == 100 }
            instrumentation.runOnMainSync {
                var received: androidx.core.view.WindowInsetsCompat? = null
                androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(screen.page) { _, insets -> received = insets; insets }
                val bars = androidx.core.view.WindowInsetsCompat.Type.systemBars()
                val ime = androidx.core.view.WindowInsetsCompat.Type.ime()
                val cutout = androidx.core.view.WindowInsetsCompat.Type.displayCutout()
                val incoming = androidx.core.view.WindowInsetsCompat.Builder()
                    .setInsets(bars, androidx.core.graphics.Insets.of(0, 24, 0, 32))
                    .setInsets(ime, androidx.core.graphics.Insets.of(0, 0, 0, 300))
                    .setVisible(bars or ime, true).build()
                androidx.core.view.ViewCompat.dispatchApplyWindowInsets(screen.pageRoot, incoming)
                assertEquals(24, screen.pageRoot.paddingTop); assertEquals(300, screen.pageRoot.paddingBottom)
                assertNotNull("WebView must receive a clearing update, including after display moves", received)
                assertEquals(androidx.core.graphics.Insets.NONE, received!!.getInsets(bars or ime or cutout))
                assertFalse(received!!.isConsumed)
                received = null
                androidx.core.view.ViewCompat.dispatchApplyWindowInsets(screen.pageRoot,
                    androidx.core.view.WindowInsetsCompat.Builder(incoming).setInsets(ime, androidx.core.graphics.Insets.NONE).setVisible(ime, false).build())
                assertEquals(32, screen.pageRoot.paddingBottom)
                assertNotNull(received)
                assertEquals(androidx.core.graphics.Insets.NONE, received!!.getInsets(bars or ime or cutout))
                androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(screen.page, null)
                androidx.core.view.ViewCompat.requestApplyInsets(screen.pageRoot)
            }
        } finally { instrumentation.runOnMainSync { screen.finish() } }
    }

    @Test fun displayRepairWaitsForLayoutInsteadOfLosingAnEmptyViewport() {
        val screen = instrumentation.startActivitySync(Intent(context, BrowserTestActivity::class.java)
            .putExtra("url", fixture.url("/window-sizing")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as BrowserTestActivity
        val detaches = java.util.concurrent.atomic.AtomicInteger()
        var parameters: android.view.ViewGroup.LayoutParams? = null
        try {
            awaitUi("The browser must be laid out") { screen.page.width > 0 && screen.page.progress == 100 }
            instrumentation.runOnMainSync {
                parameters = screen.page.layoutParams
                screen.page.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: View) = Unit
                    override fun onViewDetachedFromWindow(view: View) { detaches.incrementAndGet() }
                })
                screen.page.layoutParams = android.widget.FrameLayout.LayoutParams(0, 0)
                val activity = io.github.ulysseszh.pwainstaller.browser.BrowserActivity::class.java
                activity.getDeclaredField("viewportNeedsReset").apply { isAccessible = true }.setBoolean(screen, true)
            }
            awaitUi("A disconnected window may temporarily have no viewport") { screen.page.width == 0 && screen.page.height == 0 }
            instrumentation.runOnMainSync {
                val repair = io.github.ulysseszh.pwainstaller.browser.BrowserActivity::class.java
                    .getDeclaredField("viewportRepair").apply { isAccessible = true }.get(screen) as Runnable
                repair.run()
                assertEquals(0, detaches.get())
                screen.page.layoutParams = parameters
            }
            awaitUi("A pending repair must run once the viewport is laid out again") { screen.page.width > 1 && detaches.get() > 0 }
            assertEquals(1, fixture.requestedPaths.count { it == "/window-sizing" })
        } finally { instrumentation.runOnMainSync { screen.finish() } }
    }

    private class CapturedDisplay(context: Context) : java.io.Closeable {
        private val thread = android.os.HandlerThread("display-capture").apply { start() }
        private val handler = android.os.Handler(thread.looper)
        private val reader = android.media.ImageReader.newInstance(1280, 900, android.graphics.PixelFormat.RGBA_8888, 3)
        private val requested = AtomicBoolean()
        private val closed = AtomicBoolean()
        private val frame = AtomicReference<Bitmap>()
        private val virtual = requireNotNull((context.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager)
            .createVirtualDisplay("PWA reconnection test", 1280, 900, 160, reader.surface,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                    android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY))
        val id get() = virtual.display.displayId
        init {
            reader.setOnImageAvailableListener({ images ->
                if (!closed.get()) images.acquireLatestImage()?.use { image ->
                    if (requested.compareAndSet(true, false)) {
                        val plane = image.planes[0]
                        val padded = Bitmap.createBitmap(plane.rowStride / plane.pixelStride, image.height, Bitmap.Config.ARGB_8888)
                        padded.copyPixelsFromBuffer(plane.buffer)
                        val copy = if (padded.width == image.width) padded else
                            Bitmap.createBitmap(padded, 0, 0, image.width, image.height).also { padded.recycle() }
                        frame.getAndSet(copy)?.recycle()
                    }
                }
            }, handler)
        }
        fun capture(): Bitmap? = frame.getAndSet(null).also { if (it == null) requested.set(true) }
        override fun close() {
            if (!closed.compareAndSet(false, true)) return
            virtual.release()
            val finished = CountDownLatch(1)
            handler.post { reader.close(); frame.getAndSet(null)?.recycle(); thread.quitSafely(); finished.countDown() }
            check(finished.await(5, TimeUnit.SECONDS))
        }
    }

    @Test fun unpluggingAnExternalFreeformDisplayAndReconnectingAtTheSameSizeKeepsAllFourPageCornersVisible() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 30)
        val name = "io.github.ulysseszh.pwainstaller.integrationhotplug"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name)
            manifest().put("start_url", fixture.url("/window-sizing")); validate()
        }
        val settings = listOf("enable_freeform_support", "force_resizable_activities")
            .associateWith { shell("settings get global $it").trim() }
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
        var external: CapturedDisplay? = null
        fun stack(): String = shell("am stack list").split("\n\n").firstOrNull { name in it }.orEmpty()
        fun taskId() = Regex("taskId=(\\d+): $name/").find(stack())!!.groupValues[1].toInt()
        fun rootId() = Regex("RootTask id=(\\d+)").find(stack())!!.groupValues[1].toInt()
        val colors = listOf("#f34a91", "#19d68c", "#2e6ae8", "#f6b820").map(Color::parseColor)
        var diagnostic = ""
        fun verify(display: CapturedDisplay, token: String) {
            awaitNative("The reconnected window must paint all four corners at the correct scale", timeout = 20000) {
                val state = fixture.viewportStates.lastOrNull() ?: return@awaitNative false
                val picture = display.capture() ?: return@awaitNative false
                try {
                    val pixels = IntArray(picture.width * picture.height)
                    picture.getPixels(pixels, 0, picture.width, 0, 0, picture.width, picture.height)
                    val bounds = colors.map { android.graphics.Rect(picture.width, picture.height, -1, -1) }
                    val counts = IntArray(4)
                    for (offset in pixels.indices step 2) {
                        val corner = colors.indexOf(pixels[offset])
                        if (corner >= 0) {
                            counts[corner]++
                            val x = offset % picture.width; val y = offset / picture.width
                            val box = bounds[corner]
                            box.left = minOf(box.left, x); box.top = minOf(box.top, y)
                            box.right = maxOf(box.right, x + 1); box.bottom = maxOf(box.bottom, y + 1)
                        }
                    }
                    val width = bounds[1].right - bounds[0].left
                    val height = bounds[2].bottom - bounds[0].top
                    diagnostic = "state=$state corners=$bounds counts=${counts.toList()} painted=${width}x$height"
                    val correct = counts.all { it > 100 } && kotlin.math.abs(state.getDouble("ratio") - 1.0) <= .01 &&
                        kotlin.math.abs(width - state.getDouble("width") * state.getDouble("ratio")) <= 8 &&
                        kotlin.math.abs(height - state.getDouble("height") * state.getDouble("ratio")) <= 8 && state.getDouble("scroll") >= 250
                    if (!correct) java.io.File(context.getExternalFilesDir(null), "display-reconnect-failure.png").outputStream().use {
                        picture.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    correct
                } finally { picture.recycle() }
            }
            val state = fixture.viewportStates.last()
            assertEquals(diagnostic, token, state.getString("token"))
            assertEquals("Unsaved form text", state.getString("input"))
            assertTrue(state.getDouble("scroll") >= 250)
            assertEquals(1, fixture.requestedPaths.count { it == "/window-sizing" })
        }
        try {
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            shell("settings put global enable_freeform_support 1"); shell("settings put global force_resizable_activities 1")
            external = CapturedDisplay(context)
            val firstId = external.id
            shell("am start --display $firstId --windowingMode 5 -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            awaitNative("The app must open directly on the external display") {
                fixture.viewportStates.isNotEmpty() && "displayId=$firstId " in stack() && "mWindowingMode=freeform" in stack()
            }
            shell("am task resize ${taskId()} 40 70 1140 810")
            val token = fixture.viewportStates.last().getString("token")
            verify(external, token)
            val old = external.id
            // Public virtual displays migrate their tasks on removal. Do not move
            // the task first: that hid the disconnect-while-open bug in the older test.
            external.close(); external = null
            awaitNative("Unplugging must retain the task on the built-in display") {
                manager.getDisplay(old) == null && "displayId=0 " in stack()
            }
            external = CapturedDisplay(context) // Exactly the same dimensions and density.
            assertNotEquals(old, external.id)
            shell("am display move-stack ${rootId()} ${external.id}")
            shell("am start --display ${external.id} --windowingMode 5 -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            awaitNative("The retained task must reopen as a freeform window") { "mWindowingMode=freeform" in stack() }
            shell("am task resize ${taskId()} 40 70 1140 810")
            verify(external, token)
        } catch (failure: AssertionError) { throw AssertionError("${failure.message}; $diagnostic; ${stack()}", failure) }
        finally {
            shell("pm uninstall $name"); external?.close()
            settings.forEach { (key, value) -> if (value == "null") shell("settings delete global $key") else shell("settings put global $key '$value'") }
        }
    }

    @Test fun freeformResizingAndDisplayReconnectionRetainThePageAndRefreshItsViewport() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 30)
        val name = "io.github.ulysseszh.pwainstaller.integrationwindows"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name)
            manifest().put("start_url", fixture.url("/window-sizing")); validate()
        }
        val settings = listOf("enable_freeform_support", "force_resizable_activities", "overlay_display_devices")
            .associateWith { shell("settings get global $it").trim() }
        val displays = context.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
        val service = instrumentation.uiAutomation.serviceInfo
        val oldFlags = service.flags
        fun trace(step: String) { instrumentation.sendStatus(0, Bundle().apply { putString("window_step", step) }) }
        fun stack(): String = shell("am stack list").split("\n\n").firstOrNull { name in it }.orEmpty()
        fun stackId(): Int = Regex("RootTask id=(\\d+)").find(stack())!!.groupValues[1].toInt()
        fun taskId(): Int = Regex("taskId=(\\d+): $name/").find(stack())!!.groupValues[1].toInt()
        fun webBounds(display: Int): android.graphics.Rect? {
            fun find(node: AccessibilityNodeInfo): android.graphics.Rect? {
                if (node.packageName?.toString() == name && node.className?.toString() == "android.webkit.WebView")
                    return android.graphics.Rect().also(node::getBoundsInScreen)
                repeat(node.childCount) { index -> node.getChild(index)?.let { find(it)?.let { bounds -> return bounds } } }
                return null
            }
            return instrumentation.uiAutomation.windowsOnAllDisplays[display].orEmpty().firstNotNullOfOrNull { it.root?.let(::find) }
        }
        fun verifyViewport(display: Int, token: String) {
            var debug = ""
            try {
                awaitNative("CSS viewport must fill the WebView after a window/display change on $display") {
                    // Overlay test displays do not expose accessibility windows. Their
                    // content has no system bars, so the WindowManager task frame is the viewport.
                    val bounds = webBounds(display) ?: if (display != 0) Regex("bounds=\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]").find(stack())?.let { match ->
                        match.groupValues.drop(1).map(String::toInt).let { android.graphics.Rect(it[0], it[1], it[2], it[3]) }
                    } else null
                    val state = fixture.viewportStates.lastOrNull()
                    debug = "bounds=$bounds state=$state windows=${instrumentation.uiAutomation.windowsOnAllDisplays}"
                    if (bounds == null || state == null) return@awaitNative false
                    val ratio = state.getDouble("ratio")
                    kotlin.math.abs(state.getDouble("width") * ratio - bounds.width()) <= 3 &&
                        kotlin.math.abs(state.getDouble("height") * ratio - bounds.height()) <= 3
                }
            } catch (failure: AssertionError) { throw AssertionError("${failure.message}; $debug; ${stack()}") }
            val state = fixture.viewportStates.last()
            assertEquals("Window changes must preserve this document's JS state", token, state.getString("token"))
            assertEquals("Unsaved form text", state.getString("input"))
            assertTrue("Window changes must retain scrolling", state.getDouble("scroll") >= 250)
            assertEquals("Window changes must not reload the main document", 1, fixture.requestedPaths.count { it == "/window-sizing" })
        }
        try {
            trace("building")
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            shell("settings put global enable_freeform_support 1"); shell("settings put global force_resizable_activities 1")
            service.flags = service.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            instrumentation.uiAutomation.serviceInfo = service
            shell("am start --windowingMode 5 -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            awaitNative("The document must load inside a real freeform window") { fixture.viewportStates.isNotEmpty() && "mWindowingMode=freeform" in stack() }
            val token = fixture.viewportStates.last().getString("token")
            verifyViewport(0, token)
            trace("resizing")
            val initialWidth = fixture.viewportStates.last().getInt("width")
            shell("am task resize ${taskId()} 40 140 1040 1550")
            awaitNative("Resizing must update CSS width") { fixture.viewportStates.last().getInt("width") != initialWidth }
            verifyViewport(0, token)
            // Delivering a launcher intent to the existing singleTask activity must retain its document too.
            shell("am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            verifyViewport(0, token)
            trace("moving to external display")
            val existing = displays.displays.map { it.displayId }.toSet()
            shell("settings put global overlay_display_devices 1280x720/160")
            var external = -1
            awaitNative("A secondary display must be created") { external = displays.displays.firstOrNull { it.displayId !in existing }?.displayId ?: -1; external >= 0 }
            shell("am display move-stack ${stackId()} $external")
            awaitNative("The activity must move to the secondary display") { "displayId=$external " in stack() }
            shell("am task resize ${taskId()} 40 70 1100 650")
            verifyViewport(external, token)
            trace("disconnecting and reconnecting")
            // Move before removal: Android's virtual-display policy can otherwise destroy its task.
            shell("am display move-stack ${stackId()} 0")
            shell("am task resize ${taskId()} 40 140 1040 1550")
            verifyViewport(0, token)
            shell("settings delete global overlay_display_devices")
            awaitNative("The old display must disconnect") { displays.getDisplay(external) == null }
            shell("settings put global overlay_display_devices 1600x900/200")
            val oldDisplay = external
            awaitNative("The secondary display must reconnect with new density and dimensions") {
                external = displays.displays.firstOrNull { it.displayId !in existing && it.displayId != oldDisplay }?.displayId ?: -1; external >= 0
            }
            shell("am display move-stack ${stackId()} $external")
            shell("am task resize ${taskId()} 50 90 1450 820")
            verifyViewport(external, token)
            trace("complete")
        } finally {
            shell("pm uninstall $name")
            settings.forEach { (key, value) -> if (value == "null") shell("settings delete global $key") else shell("settings put global $key '$value'") }
            service.flags = oldFlags; instrumentation.uiAutomation.serviceInfo = service
        }
    }

    @Test fun systemDarkModeUpdatesCssAndMediaQueryListenersWithoutReloading() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 29)
        val originalMode = shell("cmd uimode night").substringAfterLast(':').trim()
        val packages = listOf("plain", "toolbar").associateWith { "io.github.ulysseszh.pwainstaller.integrationcolorscheme$it" }
        var session: String? = null
        fun night(dark: Boolean) { shell("cmd uimode night ${if (dark) "yes" else "no"}") }
        fun verify(case: String, dark: Boolean, token: String? = null, changes: Int = 0): JSONObject {
            var latest: JSONObject? = null
            try { awaitNative("$case must report ${if (dark) "dark" else "light"} in both CSS and matchMedia", timeout = 20000) {
                latest = fixture.colorSchemeStates.lastOrNull { it.optString("case") == case }
                latest?.let { state ->
                    state.optBoolean("ready") && state.getBoolean("dark") == dark && state.getBoolean("light") != dark && state.getBoolean("observedDark") == dark &&
                        state.getString("color") == (if (dark) "rgb(211, 222, 233)" else "rgb(11, 22, 33)") &&
                        state.getInt("changes") >= changes
                } == true
            } } catch (failure: AssertionError) { throw AssertionError("${failure.message}; last=$latest; expectedToken=$token", failure) }
            val state = requireNotNull(latest)
            if (token != null) assertEquals("Theme changes must preserve the document's JS state", token, state.getString("token"))
            assertEquals("Unsaved text", state.getString("draft"))
            instrumentation.sendStatus(0, Bundle().apply { putString("color_scheme", "$case dark=$dark changes=${state.getInt("changes")}") })
            return state
        }
        fun toggle(case: String, expectedRequests: Int) {
            val initial = verify(case, true)
            val token = initial.getString("token")
            night(false); val light = verify(case, false, token, initial.getInt("changes") + 1)
            night(true); verify(case, true, token, light.getInt("changes") + 1)
            assertEquals("Switching themes must not reload the page", expectedRequests,
                fixture.requestedPaths.count { it == "/color-scheme?case=$case" })
        }
        try {
            for ((case, name) in packages) {
                val path = "/color-scheme?case=$case"
                val app = WebApp().apply {
                    set("baseUrl", fixture.url("/")); set("packageName", name); manifest().put("display", if (case == "toolbar") "browser" else "standalone")
                    set("adblockRules", ""); manifest().put("start_url", fixture.url(path)); validate()
                }
                val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
                night(true)
                shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
                toggle(case, 1)
                shell("am force-stop $name"); night(false); fixture.colorSchemeStates.clear()
                shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
                val initial = verify(case, false)
                night(true); verify(case, true, initial.getString("token"), initial.getInt("changes") + 1)
                assertEquals(2, fixture.requestedPaths.count { it == path })
                shell("am force-stop $name")
            }
            // The temporary browser shares the same WebView configuration path.
            night(true)
            val app = WebApp().apply { set("baseUrl", fixture.url("/color-scheme?case=temporary")); set("adblockRules", ""); validate() }
            session = LoginSessions.begin(context, app.s("key"))
            context.startActivity(LoginSessions.intent(context, app, session!!).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            toggle("temporary", 1)
        } finally {
            session?.let { LoginSessions.discard(context, it) }
            packages.values.forEach { shell("pm uninstall $it") }
            shell("cmd uimode night ${originalMode.takeIf { it in listOf("auto", "yes", "no", "custom") } ?: "auto"}")
        }
    }

    @Test fun pageThemeColorsFollowMediaChangesAndMetadataWithManifestFallback() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 29)
        val originalMode = shell("cmd uimode night").substringAfterLast(':').trim()
        val fallback = Color.rgb(177, 55, 161)
        val background = Color.rgb(225, 226, 227)
        val darkBackground = Color.rgb(32, 36, 42)
        val darkFallback = Color.rgb(139, 213, 202)
        val names = listOf("plain", "toolbar").associateWith { "io.github.ulysseszh.pwainstaller.integrationpagetheme$it" }
        fun verify(name: String, toolbar: Boolean, theme: Int, pageBackground: Int = background) {
            var debug = "no progress bar"
            try { awaitNative("Progress${if (toolbar) ", toolbar and status" else ""} must use the current page theme, retaining the background-based status without a toolbar") {
                val nodes = activeNativeNodes().filter { it.packageName?.toString() == name }
                val progress = nodes.firstOrNull { it.className?.toString() == "android.widget.ProgressBar" && it.isVisibleToUser }
                    ?.let { android.graphics.Rect().also(it::getBoundsInScreen) } ?: run {
                        debug = "no progress bar; nodes=" + nodes.map { "${it.className}:${it.text}" } + "; paths=" + fixture.requestedPaths.takeLast(8)
                        return@awaitNative false
                    }
                val image = instrumentation.uiAutomation.takeScreenshot() ?: return@awaitNative false
                try {
                    fun pixel(x: Int, y: Int) = image.getPixel(x, y) and 0xffffff
                    val tinted = pixel(progress.left + 2, progress.centerY()) == (theme and 0xffffff)
                    val statusHeight = progress.top - if (toolbar) (64 * context.resources.displayMetrics.density).toInt() else 0
                    val sampledStatus = pixel(image.width / 2, (statusHeight / 2).coerceAtLeast(1))
                    val status = sampledStatus == ((if (toolbar) theme else pageBackground) and 0xffffff)
                    debug = "progress=$progress tint=${pixel(progress.left + 2, progress.centerY()).toString(16)} expected=${(theme and 0xffffff).toString(16)} status=${sampledStatus.toString(16)} toolbar=${pixel(2, progress.top - 8).toString(16)}"
                    tinted && status && (!toolbar || pixel(2, progress.top - 8) == (theme and 0xffffff))
                } finally { image.recycle() }
            } } catch (failure: AssertionError) { throw AssertionError("${failure.message}; $debug", failure) }
        }
        try {
            for ((case, name) in names) {
                val gate = CountDownLatch(1)
                fixture.themePageGates[case] = gate
                fixture.themeCommands[case] = "initial"
                val path = "/theme-colors?case=$case"
                val app = WebApp().apply {
                    set("baseUrl", fixture.url("/")); set("packageName", name); manifest().put("display", if (case == "toolbar") "browser" else "standalone"); set("adblockRules", "")
                    set("darkThemeColor", "#8bd5ca"); set("darkBackgroundColor", "#20242a")
                    manifest().put("start_url", fixture.url(path)).put("theme_color", "#b137a1").put("background_color", "#e1e2e3"); validate()
                }
                val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
                shell("cmd uimode night no")
                shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
                verify(name, case == "toolbar", Color.rgb(20, 107, 140))
                shell("cmd uimode night yes")
                verify(name, case == "toolbar", Color.rgb(237, 188, 66))
                assertEquals("Changing color scheme must not reload the page", 1, fixture.requestedPaths.count { it == path })
                fixture.themeCommands[case] = "dynamic"
                verify(name, case == "toolbar", Color.rgb(29, 161, 242))
                fixture.themeCommands[case] = "invalid"
                verify(name, case == "toolbar", darkFallback)
                fixture.themeCommands[case] = "dynamic"
                verify(name, case == "toolbar", Color.rgb(29, 161, 242))
                fixture.themeCommands[case] = "navigate"
                awaitNative("The next page must load without theme metadata") { fixture.requestedPaths.any { it == "$path&missing=1" } }
                verify(name, case == "toolbar", darkFallback, darkBackground)
                shell("cmd uimode night no")
                verify(name, case == "toolbar", fallback, background)
                gate.countDown()
                shell("pm uninstall $name")
            }
        } finally {
            fixture.themePageGates.values.forEach(CountDownLatch::countDown)
            names.values.forEach { shell("pm uninstall $it") }
            shell("cmd uimode night ${originalMode.takeIf { it in listOf("auto", "yes", "no", "custom") } ?: "auto"}")
        }
    }

    @Test fun pageBackgroundColorsUpdateTheStatusBarAndThemeColorsTintLoadingProgress() {
        fun trace(step: String) {
            if (InstrumentationRegistry.getArguments().getString("traceColors") == "true") instrumentation.sendStatus(0, Bundle().apply { putString("color_step", step) })
        }
        val name = "io.github.ulysseszh.pwainstaller.integrationpagecolors"
        val theme = Color.rgb(177, 55, 161)
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name)
            manifest().put("start_url", fixture.url("/page-colors")).put("theme_color", "#b137a1").put("background_color", "#102030")
            validate()
        }
        fun barBounds() = activeNativeNodes().firstOrNull {
            it.packageName?.toString() == name && it.isVisibleToUser && it.className?.toString() == "android.widget.ProgressBar"
        }?.let { node -> android.graphics.Rect().also(node::getBoundsInScreen) }
        fun pixelMatches(x: Int, y: Int, color: Int): Boolean {
            val image = instrumentation.uiAutomation.takeScreenshot() ?: return false
            return try { (image.getPixel(x, y) and 0xffffff) == (color and 0xffffff) } finally { image.recycle() }
        }
        fun statusMatches(color: Int): Boolean {
            val webView = activeNativeNodes().firstOrNull { it.packageName?.toString() == name && it.className?.toString() == "android.webkit.WebView" } ?: return false
            val bounds = android.graphics.Rect().also(webView::getBoundsInScreen)
            return bounds.top > 1 && pixelMatches(bounds.centerX(), bounds.top / 2, color)
        }
        try {
            trace("building")
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            trace("launching")
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            assertTrue(fixture.pageResourceStarted.await(15, TimeUnit.SECONDS))
            trace("reading progress color")
            awaitNative("Loading progress must use the app's theme color") {
                barBounds()?.let { pixelMatches(it.left + 2, it.centerY(), theme) } == true
            }
            trace("reading status background")
            if (Build.VERSION.SDK_INT < 23) {
                fixture.continuePage.countDown()
                awaitNative("Old WebView must finish before querying page styles") { fixture.readyPaths.contains("/page-colors") }
            }
            awaitNative("Status bar must composite the page's body and root background colors") { statusMatches(Color.rgb(137, 26, 43)) }
            trace("finishing page load")
            fixture.continuePage.countDown()
            awaitNative("Color fixture must finish loading") { fixture.readyPaths.contains("/page-colors") }
            clickText("Change background", name)
            trace("reading changed background")
            awaitNative("Status bar must follow a changed page background without navigation") { statusMatches(Color.rgb(245, 245, 245)) }
        } finally { fixture.continuePage.countDown(); shell("pm uninstall $name") }
    }

    @Test fun browserToolbarAndStatusBarUseThemeWhileNavigationFollowsFullWidthBackgrounds() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 23)
        val name = "io.github.ulysseszh.pwainstaller.integrationtoolbarcolors"
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("packageName", name); manifest().put("display", "browser"); manifest().put("start_url", fixture.url("/toolbar-colors")); validate() }
        fun verify(color: Int, foreground: Int) {
            awaitNative("The status and browser bars must use the app theme color with readable controls") {
                val nodes = activeNativeNodes().filter { it.packageName?.toString() == name }
                val web = nodes.firstOrNull { it.className?.toString() == "android.webkit.WebView" } ?: return@awaitNative false
                val address = nodes.firstOrNull { it.className?.toString() == "android.widget.EditText" } ?: return@awaitNative false
                val bounds = android.graphics.Rect().also(web::getBoundsInScreen)
                val input = android.graphics.Rect().also(address::getBoundsInScreen)
                val image = instrumentation.uiAutomation.takeScreenshot() ?: return@awaitNative false
                try {
                    fun pixel(x: Int, y: Int) = image.getPixel(x, y) and 0xffffff
                    var foregroundPixels = 0
                    for (y in input.top.coerceAtLeast(0) until input.bottom.coerceAtMost(image.height))
                        for (x in input.left.coerceAtLeast(0) until input.right.coerceAtMost(image.width)) if (pixel(x, y) == (foreground and 0xffffff)) foregroundPixels++
                    pixel(2, bounds.top - 8) == (color and 0xffffff) && pixel(image.width / 2, input.top / 3) == (color and 0xffffff) && foregroundPixels > 20
                } finally { image.recycle() }
            }
        }
        fun verifyNavigation(color: Int) {
            awaitNative("Navigation background must follow full-width footers and ignore narrow buttons") {
                val web = activeNativeNodes().firstOrNull { it.packageName?.toString() == name && it.className?.toString() == "android.webkit.WebView" } ?: return@awaitNative false
                val bounds = android.graphics.Rect().also(web::getBoundsInScreen)
                val image = instrumentation.uiAutomation.takeScreenshot() ?: return@awaitNative false
                try {
                    bounds.bottom < image.height &&
                        (image.getPixel(2, (bounds.bottom + image.height) / 2) and 0xffffff) == (color and 0xffffff)
                } finally { image.recycle() }
            }
        }
        try {
            for ((theme, foreground) in listOf(Color.rgb(7, 59, 98) to Color.WHITE, Color.rgb(245, 228, 207) to Color.BLACK)) {
                app.manifest().put("theme_color", String.format(Locale.ROOT, "#%06x", theme and 0xffffff))
                val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
                shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
                verify(theme, foreground)
                verifyNavigation(Color.rgb(22, 22, 22))
                clickText("Change bottom button", name)
                awaitNative("Bottom button color must change in the fixture") { activeNativeNodes().any { it.text?.toString() == "Bottom button changed" } }
                verifyNavigation(Color.rgb(22, 22, 22))
                clickText("Change button", name)
                awaitNative("Button color must change in the fixture") { activeNativeNodes().any { it.text?.toString() == "Button changed" } }
                verify(theme, foreground)
                val pageBounds = android.graphics.Rect().also { bounds -> activeNativeNodes().first { it.packageName?.toString() == name && it.className?.toString() == "android.webkit.WebView" }.getBoundsInScreen(bounds) }
                shell("input swipe ${pageBounds.centerX()} ${pageBounds.bottom - 100} ${pageBounds.centerX()} ${pageBounds.top + 150} 400")
                verify(theme, foreground)
                verifyNavigation(Color.rgb(22, 22, 22))
                clickText("Change footer", name)
                verifyNavigation(Color.rgb(18, 52, 86))
                clickText("Change page", name)
                verify(theme, foreground)
                verifyNavigation(Color.rgb(18, 52, 86))
                activeNativeNodes().first { it.packageName?.toString() == name && it.contentDescription?.toString() == "More options" }.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                awaitNative("Browser overflow menu must appear") { activeNativeNodes().any { it.text?.toString() == context.getString(BrowserR.string.browser_print_page) } }
                val menuTexts = activeNativeNodes().filter { it.packageName?.toString() == name && it.className?.toString() == "android.widget.TextView" }.mapNotNull { it.text?.toString() }
                assertEquals(context.getString(BrowserR.string.refresh), menuTexts.first())
                assertTrue(menuTexts.contains("Print")); assertTrue(menuTexts.contains("Download"))
            }
        } finally { shell("pm uninstall $name") }
    }

    @Test fun fixedBottomBarsUseTheWholeViewportAndKeyboardInsetsAreAppliedOnce() {
        val name = "io.github.ulysseszh.pwainstaller.integrationinsets"
        val service = instrumentation.uiAutomation.serviceInfo
        val oldFlags = service.flags
        service.flags = service.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        instrumentation.uiAutomation.serviceInfo = service
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("packageName", name); manifest().put("start_url", fixture.url("/inset-layout")); validate() }
        fun verify() {
            awaitNative("The fixed bar must reach the WebView bottom with no duplicate safe-area or viewport inset") {
                val state = fixture.insetStates.lastOrNull() ?: return@awaitNative false
                val view = activeNativeNodes().firstOrNull { it.packageName?.toString() == name && it.className?.toString() == "android.webkit.WebView" } ?: return@awaitNative false
                val bounds = android.graphics.Rect().also(view::getBoundsInScreen)
                kotlin.math.abs(state.getDouble("height") * state.getDouble("ratio") - bounds.height()) <= 3 &&
                    kotlin.math.abs(state.getDouble("bottom") - state.getDouble("height")) <= 1 &&
                    state.getDouble("safeBottom") == 0.0 && kotlin.math.abs(state.getDouble("visualHeight") - state.getDouble("height")) <= 1
            }
        }
        try {
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            verify(); val initialHeight = fixture.insetStates.last().getDouble("height")
            clickText("Keyboard field", name)
            awaitNative("Keyboard must resize the native WebView") { fixture.insetStates.last().getDouble("height") < initialHeight - 80 }
            verify()
            awaitNative("Native layout must reserve keyboard space exactly once") {
                val keyboard = instrumentation.uiAutomation.windows.firstOrNull { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD } ?: return@awaitNative false
                val keyboardBounds = android.graphics.Rect().also(keyboard::getBoundsInScreen)
                val view = activeNativeNodes().firstOrNull { it.packageName?.toString() == name && it.className?.toString() == "android.webkit.WebView" } ?: return@awaitNative false
                val viewBounds = android.graphics.Rect().also(view::getBoundsInScreen)
                kotlin.math.abs(viewBounds.bottom - keyboardBounds.top) <= 3
            }
            shell("input keyevent KEYCODE_BACK")
            try {
                awaitNative("Closing the keyboard must restore the full viewport") { kotlin.math.abs(fixture.insetStates.last().getDouble("height") - initialHeight) <= 1 }
            } catch (failure: AssertionError) {
                throw AssertionError("${failure.message}: initial=$initialHeight, states=${fixture.insetStates.distinctBy { it.toString() }}", failure)
            }
            verify()
        } finally { shell("pm uninstall $name"); service.flags = oldFlags; instrumentation.uiAutomation.serviceInfo = service }
    }

    @Test fun outsideLinksHonorSystemAppAssociationsAndExcludeTheCallingApp() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 31)
        val host = "external-dispatch-fixture.invalid"
        val uri = Uri.parse("https://$host/handoff/document?item=3#details")
        shell("pm set-app-links-allowed --user 0 --package ${context.packageName} true")
        shell("pm set-app-links-user-selection --user 0 --package ${context.packageName} true $host")
        var handler: ExternalLinkTestActivity? = null
        try {
            val caller = object : android.content.ContextWrapper(context) { override fun getPackageName() = "io.github.ulysseszh.externalcaller" }
            val intent = io.github.ulysseszh.pwainstaller.browser.SystemLinks.intent(caller, uri)
            assertEquals(Intent.ACTION_VIEW, intent.action); assertEquals(uri, intent.data)
            assertNull(intent.component); assertNull(intent.`package`)
            assertEquals(android.content.ComponentName(context, ExternalLinkTestActivity::class.java), intent.resolveActivity(context.packageManager))
            handler = instrumentation.startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ExternalLinkTestActivity
            assertEquals(uri, handler!!.intent.data)
            // The caller itself can be the selected URL handler for broader manifest scopes.
            val escape = io.github.ulysseszh.pwainstaller.browser.SystemLinks.intent(context, uri)
            if (escape.action == Intent.ACTION_CHOOSER) {
                val excluded = escape.getParcelableArrayExtra(Intent.EXTRA_EXCLUDE_COMPONENTS).orEmpty().filterIsInstance<android.content.ComponentName>()
                assertTrue(excluded.any { it.packageName == context.packageName })
            } else assertNotEquals(context.packageName, escape.resolveActivity(context.packageManager)?.packageName)
        } finally {
            handler?.let { instrumentation.runOnMainSync { it.finish() } }
            shell("pm set-app-links-user-selection --user 0 --package ${context.packageName} false $host")
        }
    }

    @Test fun navigationPatternsKeepMatchingLinksAndOpenOtherLinksInTheSystemBrowser() {
        val name = "io.github.ulysseszh.pwainstaller.integrationnavigation"
        val agent = "PwaNavigation/5.0"
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/inside/")); set("packageName", name); set("userAgent", agent)
            manifest().put("scope", fixture.url("/")); validate()
        }
        fun awaitRequest(path: String, external: Boolean = false): String {
            val deadline = System.currentTimeMillis() + 20000
            while (System.currentTimeMillis() < deadline) {
                fixture.navigationAgents[path]?.let { observedAgent ->
                    if ((!external || observedAgent != agent) &&
                        (!path.startsWith("/inside/") || fixture.readyPaths.contains(path.substringBefore('?')))) return observedAgent
                }
                // Android 5 has no default browser on the test image. Exercise
                // its ordinary chooser rather than requiring a forced browser.
                if (external && Build.VERSION.SDK_INT < 23) {
                    val browser = io.github.ulysseszh.pwainstaller.browser.SystemBrowser.intent(context, Uri.parse(fixture.url(path))).`package`
                    val label = browser?.let { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(it, 0)).toString() }
                    val choices = activeNativeNodes().filter { it.packageName?.toString() == "android" }
                    var choice = choices.firstOrNull { it.text?.toString() == label }
                    while (choice != null) {
                        if (choice.isClickable && choice.performAction(AccessibilityNodeInfo.ACTION_CLICK)) break
                        choice = choice.parent
                    }
                    activeNativeNodes().firstOrNull { it.packageName?.toString() == "android" && it.text?.toString()?.equals("Just once", true) == true }
                        ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                Thread.sleep(100)
            }
            throw AssertionError("Navigation did not reach $path: ${fixture.navigationAgents}")
        }
        fun launch() {
            // Returning from another application also dismisses an outstanding
            // legacy resolver, which may outlive the browser's first request.
            if (activeNativeNodes().any { it.packageName?.toString() == "android" && it.text?.toString() == "Open with" }) shell("input keyevent KEYCODE_BACK")
            fixture.readyPaths.remove("/inside/")
            shell("am start -a android.intent.action.VIEW -d '${fixture.url("/inside/")}' -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            awaitNative("Navigation fixture must finish loading after launch") { fixture.readyPaths.contains("/inside/") }
            instrumentation.waitForIdleSync()
            if (Build.VERSION.SDK_INT < 23) Thread.sleep(300) // Allow the legacy renderer to commit its first frame.
        }
        try {
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            launch(); assertEquals(agent, awaitRequest("/inside/")); clickText("Allowed link", name)
            assertEquals(agent, awaitRequest("/inside/allowed"))
            clickText("Outside link", name)
            assertNotEquals(agent, awaitRequest("/outside?link", external = true))
            launch(); clickText("External redirect", name)
            assertNotEquals(agent, awaitRequest("/outside?redirect", external = true))
            launch(); clickText("External popup", name)
            assertNotEquals(agent, awaitRequest("/outside?popup", external = true))
            for (status in listOf(301, 302, 303, 307, 308)) {
                launch(); clickText("Redirect back $status", name)
                val target = "/inside/returned?status=$status"
                assertEquals(agent, awaitRequest(target))
                assertTrue("Redirect cookies must reach the web app", fixture.navigationCookies[target].orEmpty().contains("redirected=yes"))
                assertTrue("Returning redirect must remain in the web app", activeNativeNodes().any { it.packageName?.toString() == name })
            }
            launch(); clickText("Popup back", name)
            assertEquals(agent, awaitRequest("/inside/returned?popup"))
            launch(); clickText("Cross origin back", name)
            assertEquals(agent, awaitRequest("/inside/returned?cross"))
            launch(); clickText("Outside redirect", name)
            assertNotEquals(agent, awaitRequest("/outside/final", external = true))
            app.set("urlPatterns", fixture.url("/inside/") + "\n" + fixture.url("/outside"))
            val updated = ApkBuilder.build(context, app); install(updated); updated.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity -a android.intent.action.VIEW -d '${fixture.url("/outside/child?custom")}'")
            assertEquals(agent, awaitRequest("/outside/child?custom"))
        } finally { shell("pm uninstall $name") }
    }

    @Test fun commentedFilterSourcesArePreservedAndNeverFetched() {
        val oldUrls = AdblockSettings.urls(context); val oldHours = AdblockSettings.hours(context)
        try {
            val text = "# " + fixture.url("/disabled-filter") + "\n" + fixture.url("/filters.txt")
            AdblockSettings.save(context, text, 0)
            assertEquals(text, AdblockSettings.urls(context))
            val before = fixture.requestCount.get()
            AdblockSettings.refresh(context)
            assertEquals("Only the enabled list may be fetched", before + 1, fixture.requestCount.get())
            assertTrue(AdblockSettings.cachedDefaults(context, fixture.url("/")).contains("cookie-notice"))
            val updated = AdblockSettings.updated(context)
            AdblockSettings.save(context, "# A comment\n" + text, 0)
            assertEquals("Changing comments must retain freshness", updated, AdblockSettings.updated(context))
            assertEquals(before + 1, fixture.requestCount.get())
        } finally { AdblockSettings.save(context, oldUrls, oldHours) }
    }

    @Test fun adguardDefaultsAreSnapshotsAndBlockNetworkCosmeticExtendedAndScriptletContent() {
        val name = "io.github.ulysseszh.pwainstaller.integrationadblock"
        val oldUrls = AdblockSettings.urls(context); val oldHours = AdblockSettings.hours(context)
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("packageName", name); manifest().put("start_url", fixture.url("/adblock-check")) }
        var editor: EditorActivity? = null
        try {
            AdblockSettings.save(context, fixture.url("/filters.txt"), 0)
            AdblockSettings.refresh(context)
            val original = AdblockSettings.cachedDefaults(context, app.s("baseUrl"))
            assertTrue(original.contains("cookie-notice")); assertFalse(original.contains("elsewhere.test"))
            app.set("adblockRules", original); app.validate()
            AppDatabase(context).use { it.save(app) }
            val apk = ApkBuilder.build(context, app)
            ZipFile(apk).use { zip ->
                assertNotNull(zip.getEntry("assets/adblock.js"))
                assertNotNull(zip.getEntry("assets/licenses/adguard.txt"))
            }
            assertTrue("Filtered APK should stay below 1 MiB: ${apk.length()}", apk.length() < 1024 * 1024)
            install(apk); apk.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            assertTrue("Filter probe did not finish", fixture.adblockDone.await(30, TimeUnit.SECONDS))
            val result = fixture.adblockResult.get()
            listOf("hidden=true", "extended=true", "style=true", "cookie=true", "network=true").forEach { assertTrue("$result; resource requests=${fixture.adResourceRequests.get()}", result.contains(it)) }
            assertEquals("Blocked image must never reach the server", 0, fixture.adResourceRequests.get())
            fixture.filterVersion.set(2); AdblockSettings.refresh(context)
            val updated = AdblockSettings.cachedDefaults(context, app.s("baseUrl"))
            assertTrue(updated.contains("updated-cookie")); assertNotEquals(original, updated)
            assertEquals(original, app.s("adblockRules"))
            assertEquals(original, AppDatabase(context).use { it.get(app.s("key"))!!.s("adblockRules") })
            AdblockSettings.save(context, fixture.url("/filters.txt") + "\n" + fixture.url("/uncached-filters.txt"), 0)
            val requestsBeforeRefresh = fixture.requestCount.get()
            editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).putExtra("id", app.s("key")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as EditorActivity
            awaitEditor(editor)
            val screen = editor
            instrumentation.runOnMainSync {
                views(screen.root).filterIsInstance<TextView>().single { it.text.toString() == context.getString(R.string.refresh_default_rules) }.performClick()
            }
            val deadline = System.currentTimeMillis() + 10000
            var busy = true
            while (busy && System.currentTimeMillis() < deadline) { Thread.sleep(50); instrumentation.runOnMainSync { busy = screen.busy } }
            instrumentation.runOnMainSync {
                val read = EditorActivity::class.java.getDeclaredMethod("read").apply { isAccessible = true }.invoke(screen) as WebApp
                assertEquals(updated, read.s("adblockRules"))
            }
            assertEquals("Refreshing app defaults must not download missing filter lists", requestsBeforeRefresh, fixture.requestCount.get())
            assertEquals("Refresh must not silently save an editor draft", original, AppDatabase(context).use { it.get(app.s("key"))!!.s("adblockRules") })
        } finally {
            editor?.let { instrumentation.runOnMainSync { it.finish() } }
            AdblockSettings.save(context, oldUrls, oldHours)
            AppDatabase(context).use { it.remove(app) }; shell("pm uninstall $name")
        }
    }

    @Test fun userFileLibraryImportsLocalFilesAndFetchesAndRefreshesUrls() {
        val originalIds = UserContentLibrary.load(context).map { it.id }.toSet()
        val local = File(context.cacheDir, "apks/fixture-user-style.css").apply { parentFile!!.mkdirs(); writeText("#styled{color:red}") }
        val activity = instrumentation.startActivitySync(Intent(context, UserContentActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as UserContentActivity
        try {
            awaitUi("Library must load") { !activity.busy }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", local)
            instrumentation.runOnMainSync {
                UserContentActivity::class.java.getDeclaredField("importing").apply { isAccessible = true }.set(activity, UserContentFiles.Kind.STYLE)
                UserContentActivity::class.java.getDeclaredMethod("onActivityResult", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Intent::class.java)
                    .apply { isAccessible = true }.invoke(activity, 81, android.app.Activity.RESULT_OK, Intent().setData(uri))
            }
            awaitUi("A local CSS file must appear in the library") {
                !activity.busy && views(activity.root).filterIsInstance<TextView>().any { it.text.toString() == local.name }
            }
            val imported = UserContentLibrary.load(context).single { it.id !in originalIds }
            assertEquals(UserContentFiles.Kind.STYLE, imported.kind); assertEquals(local.readText(), imported.code)
            fun fetch(previous: UserContentFiles.Entry? = null) = instrumentation.runOnMainSync {
                UserContentActivity::class.java.getDeclaredMethod("fetch", UserContentFiles.Kind::class.java, String::class.java, String::class.java, UserContentFiles.Entry::class.java)
                    .apply { isAccessible = true }.invoke(activity, UserContentFiles.Kind.SCRIPT, fixture.url("/library-script.js"), "Fetched script", previous)
            }
            fetch()
            awaitUi("A fetched script must appear in the library") { !activity.busy && views(activity.root).filterIsInstance<TextView>().any { it.text.toString().startsWith("Fetched script") } }
            val fetched = UserContentLibrary.load(context).single { it.url == fixture.url("/library-script.js") }
            assertEquals("window.libraryVersion=1;", fetched.code)
            fixture.filterVersion.set(2); fetch(fetched)
            awaitUi("Refreshing a URL must finish") { !activity.busy }
            val refreshed = UserContentLibrary.load(context).single { it.id == fetched.id }
            assertEquals("window.libraryVersion=2;", refreshed.code)
            assertEquals(2, UserContentLibrary.load(context).count { it.id !in originalIds })
        } finally {
            instrumentation.runOnMainSync { activity.finish() }; local.delete()
            UserContentLibrary.load(context).filter { it.id !in originalIds }.forEach { UserContentLibrary.remove(context, it.id) }
        }
    }

    @Test fun selectedScriptsAndStylesSurviveExportsAndRunOncePerDocumentWithoutTheLibrary() {
        val name = "io.github.ulysseszh.pwainstaller.integrationusercontent"
        fun entry(kind: UserContentFiles.Kind, title: String, code: String) = UserContentFiles.Entry(UUID.randomUUID().toString(), kind, title, code)
        val files = listOf(
            entry(UserContentFiles.Kind.SCRIPT, "Enabled script", "window.userScriptRuns=(window.userScriptRuns||0)+1;document.getElementById('styled').textContent='User script enabled';"),
            entry(UserContentFiles.Kind.SCRIPT, "Broken script", "function ("),
            entry(UserContentFiles.Kind.SCRIPT, "Disabled script", "window.disabledScriptRan=true;"),
            entry(UserContentFiles.Kind.STYLE, "Enabled style", "#styled{color:rgb(1,2,3)!important}"))
        files.forEach { UserContentLibrary.save(context, it) }
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name); set("install", false)
            manifest().put("start_url", fixture.url("/custom-content")); validate()
        }
        AppDatabase(context).use { it.save(app) }
        var editor: EditorActivity? = null
        try {
            editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("id", app.s("key"))) as EditorActivity
            awaitEditor(editor!!)
            instrumentation.runOnMainSync {
                val checks = views(editor!!.root).filterIsInstance<com.google.android.material.checkbox.MaterialCheckBox>()
                files.filter { it.name != "Disabled script" }.forEach { file -> checks.single { it.text.toString() == file.name }.isChecked = true }
                editor!!.toolbar.menu.performIdentifierAction(android.R.id.button1, 0)
            }
            awaitUi("Selected files must save with the app") { editor!!.isFinishing }
            val saved = AppDatabase(context).use { it.get(app.s("key"))!! }
            assertEquals(files.filter { it.name != "Disabled script" }.map { it.id }.toSet(), UserContentFiles.read(saved.data).map { it.id }.toSet())
            val exported = JSONArray(listOf(saved.data)).toString()
            AppDatabase(context).use { it.remove(saved); it.importAll(JSONArray(exported)) }
            files.forEach { UserContentLibrary.remove(context, it.id) }
            val restored = AppDatabase(context).use { it.get(saved.s("key"))!! }
            assertEquals(UserContentFiles.read(saved.data), UserContentFiles.read(restored.data))
            val requests = fixture.requestCount.get()
            val apk = ApkBuilder.build(context, restored)
            assertEquals("Building selected files must not fetch their sources", requests, fixture.requestCount.get())
            ZipFile(apk).use { zip -> assertEquals(3, zip.entries().asSequence().count { it.name.startsWith("assets/user-content/") }) }
            install(apk); apk.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            awaitNative("Selected JavaScript and CSS must apply despite a separate invalid script") {
                fixture.customStates.any { it.optBoolean("main") && it.optInt("runs") == 1 && it.optString("color") == "rgb(1, 2, 3)" &&
                    it.optString("text") == "User script enabled" && !it.optBoolean("disabled") }
            }
            awaitNative("User files must not run in embedded frames") { fixture.customStates.any { !it.optBoolean("main") && it.optInt("runs") == 0 } }
            assertFalse("Progress retries must never execute the same script twice", fixture.customStates.any { it.optInt("runs") > 1 })
            fixture.customStates.clear()
            shell("am start -a android.intent.action.VIEW -d '${fixture.url("/custom-content?second")}' -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            awaitNative("Selected files must apply to each new document") { fixture.customStates.any { it.optBoolean("main") && it.optInt("runs") == 1 && it.optString("color") == "rgb(1, 2, 3)" } }
        } finally {
            editor?.let { instrumentation.runOnMainSync { it.finish() } }
            files.forEach { UserContentLibrary.remove(context, it.id) }
            AppDatabase(context).use { it.remove(app) }; shell("pm uninstall $name")
        }
    }

    @Test fun largeUserFilesUseChunkedStorageAndRoundTripImports() {
        val database = "user-files-${UUID.randomUUID()}.db"
        val files = (0..2).map { UserContentFiles.Entry(UUID.randomUUID().toString(), UserContentFiles.Kind.SCRIPT, "Large script $it", "/*" + "x".repeat(800_000) + "*/") }
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); UserContentFiles.put(data, files); validate() }
        try {
            AppDatabase(context, database).use { db ->
                db.save(app)
                assertEquals(files, UserContentFiles.read(db.get(app.s("key"))!!.data))
                db.readableDatabase.rawQuery("SELECT count(*),max(length(data)) FROM app_user_files", null).use { cursor ->
                    assertTrue(cursor.moveToFirst()); assertTrue(cursor.getInt(0) > 1); assertTrue(cursor.getInt(1) <= 128 * 1024)
                }
                val export = JSONArray(db.all().map { it.data }).toString()
                db.remove(app); db.importAll(JSONArray(export))
                assertEquals(files, UserContentFiles.read(db.get(app.s("key"))!!.data))
                UserContentFiles.put(app.data, emptyList()); db.save(app)
                assertTrue(UserContentFiles.read(db.get(app.s("key"))!!.data).isEmpty())
                db.readableDatabase.rawQuery("SELECT count(*) FROM app_user_files", null).use { assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0)) }
            }
        } finally { context.deleteDatabase(database) }
    }

    @Test fun compiledFilterAssetsPersistAndReportPercentageProgress() {
        val directory = File(context.cacheDir, "integration-compiled-filters").apply { deleteRecursively() }
        val rules = (0..1200).joinToString("\n") { "example.com##.banner$it" }
        val progress = mutableListOf<Int>()
        try {
            val expected = CompiledFilters(directory, "fixture-v1").get(rules,
                { path -> context.assets.open(path).bufferedReader().use { it.readText() } }, progress::add)
            assertEquals(0, progress.first()); assertEquals(100, progress.last()); assertTrue(progress.size > 40)
            assertTrue(progress.zipWithNext().all { (a, b) -> a <= b })
            progress.clear()
            val cached = CompiledFilters(directory, "fixture-v1").get(rules, { error("Must reuse persisted assets") }, progress::add)
            assertEquals(listOf(0, 100), progress)
            expected.forEach { (name, bytes) -> assertArrayEquals(bytes, cached.getValue(name)) }
        } finally { directory.deleteRecursively() }
    }

    @Test fun benchmarkCompilingCompleteRedditRules() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("benchmarkRuleLists") == "true")
        val rules = File(context.getExternalFilesDir(null), "reddit.txt").readText()
        assertTrue(rules.toByteArray().size > 768 * 1024)
        val directory = File(context.cacheDir, "benchmark-compiled-filters").apply { deleteRecursively() }
        val asset: (String) -> String = { path -> context.assets.open(path).bufferedReader().use { it.readText() } }
        val timings = JSONObject()
        val sequential = JSONArray(); val parallel = JSONArray(); val cache = JSONArray()
        var expected: Map<String, ByteArray>? = null
        fun measure(count: Int, output: JSONArray) {
            val progress = mutableListOf<Int>()
            val started = android.os.SystemClock.elapsedRealtime()
            val result = AdblockScript.compile(rules, count, progress::add, asset)
            output.put(android.os.SystemClock.elapsedRealtime() - started)
            assertEquals(0, progress.first()); assertEquals(100, progress.last())
            assertTrue(progress.zipWithNext().all { (a, b) -> a < b })
            if (expected == null) expected = result
            else requireNotNull(expected).forEach { (name, bytes) -> assertArrayEquals(bytes, result.getValue(name)) }
        }
        try {
            repeat(4) { index ->
                if (index % 2 == 0) { measure(1, sequential); measure(2, parallel) }
                else { measure(2, parallel); measure(1, sequential) }
            }
            repeat(4) { index ->
                val started = android.os.SystemClock.elapsedRealtime()
                val compiled = CompiledFilters(directory, "benchmark-v1").get(rules, if (index == 0) asset else { _ -> error("Cache hit must not read assets") })
                cache.put(android.os.SystemClock.elapsedRealtime() - started)
                requireNotNull(expected).forEach { (name, bytes) -> assertArrayEquals(bytes, compiled.getValue(name)) }
            }
            timings.put("sequential_ms", sequential).put("parallel_ms", parallel).put("persisted_cache_ms", cache)
                .put("rules_bytes", rules.toByteArray().size).put("compiled_bytes", requireNotNull(expected).values.sumOf { it.size })
            File(context.getExternalFilesDir(null), "compilation-benchmark.json").writeText(timings.toString())
            android.util.Log.i("PwaCompilationBenchmark", timings.toString())
        } finally { directory.deleteRecursively() }
    }

    @Test fun benchmarkCachedDefaultRulesFromCompleteLists() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("benchmarkRuleLists") == "true")
        val previousUrls = AdblockSettings.urls(context); val previousHours = AdblockSettings.hours(context)
        val sources = listOf("2_optimized", "18", "20").map { fixture.url("/benchmark-$it.txt") to File(context.getExternalFilesDir(null), "$it.txt") }
        val files = sources.map { (url, _) -> File(context.filesDir, "filters/${io.github.ulysseszh.pwainstaller.core.PackageNames.digest("SHA-256", url.toByteArray())}.txt") }
        try {
            sources.zip(files).forEach { (source, target) -> target.parentFile!!.mkdirs(); source.second.copyTo(target, overwrite = true) }
            AdblockSettings.save(context, sources.joinToString("\n") { it.first }, 0)
            val requests = fixture.requestCount.get()
            val timings = mutableListOf<Long>()
            val hashes = mutableListOf<Int>()
            repeat(3) {
                val started = android.os.SystemClock.elapsedRealtime()
                val rules = AdblockSettings.cachedDefaults(context, "https://www.reddit.com/")
                timings.add(android.os.SystemClock.elapsedRealtime() - started); hashes.add(rules.hashCode())
                assertEquals(File(context.getExternalFilesDir(null), "reddit.txt").readText(), rules)
                assertTrue(rules.toByteArray(Charsets.UTF_8).size > 768 * 1024)
                assertTrue(rules.contains("reddit.com"))
            }
            assertEquals(requests, fixture.requestCount.get()); assertEquals(1, hashes.distinct().size)
            val result = "selection_ms=$timings hashes=$hashes"
            android.util.Log.i("PwaRulesBenchmark", result)
            File(context.getExternalFilesDir(null), "rule-benchmark.txt").writeText(result)
        } finally { AdblockSettings.save(context, previousUrls, previousHours); files.forEach { it.delete() } }
    }

    @Test fun savingDuringRuleRefreshShowsFeedbackAndWaitsForTheNewSnapshot() {
        val previousUrls = AdblockSettings.urls(context); val previousHours = AdblockSettings.hours(context)
        val url = fixture.url("/save-feedback-rules.txt")
        val cachedFile = File(context.filesDir, "filters/${io.github.ulysseszh.pwainstaller.core.PackageNames.digest("SHA-256", url.toByteArray())}.txt")
        val app = WebApp().apply { set("baseUrl", "https://www.reddit.com/"); set("adblockRules", "##.original"); set("install", false); validate() }
        cachedFile.parentFile!!.mkdirs(); cachedFile.writeText("reddit.com##.cached")
        AdblockSettings.save(context, url, 0); AppDatabase(context).use { it.save(app) }
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra("id", app.s("key"))) as EditorActivity
        val locked = CountDownLatch(1); val release = CountDownLatch(1)
        try {
            awaitEditor(editor); awaitUi("Editor must have focus") { editor.hasWindowFocus() }
            InstallerApplication.editor.execute { locked.countDown(); release.await(30, TimeUnit.SECONDS) }
            assertTrue(locked.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                views(editor.root).filterIsInstance<TextView>().single { it.text.toString() == context.getString(R.string.refresh_default_rules) }.performClick()
                assertTrue(editor.busy)
            }
            val event = instrumentation.uiAutomation.executeAndWaitForEvent({
                instrumentation.runOnMainSync {
                    editor.toolbar.menu.performIdentifierAction(android.R.id.button1, 0)
                }
            }, { event -> event.eventType == android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED &&
                event.text.any { it.toString() == context.getString(R.string.rules_refreshing) } }, 5000)
            event.recycle()
            assertEquals("##.original", AppDatabase(context).use { it.get(app.s("key"))!!.s("adblockRules") })
            release.countDown()
            awaitUi("Refresh must complete before saving") { !editor.busy }
            instrumentation.runOnMainSync { editor.toolbar.menu.performIdentifierAction(android.R.id.button1, 0) }
            awaitUi("Save must work after refreshing finishes") { editor.isFinishing }
            assertEquals("reddit.com##.cached", AppDatabase(context).use { it.get(app.s("key"))!!.s("adblockRules") })
        } finally {
            release.countDown(); instrumentation.runOnMainSync { editor.finish() }
            AdblockSettings.save(context, previousUrls, previousHours); cachedFile.delete(); AppDatabase(context).use { it.remove(app) }
        }
    }

    @Test fun cachedRuleSelectionsAreReusedAndInvalidatedByHostSourcesAndDownloadedFiles() {
        val previousUrls = AdblockSettings.urls(context); val previousHours = AdblockSettings.hours(context)
        val urls = listOf(fixture.url("/cached-a"), fixture.url("/cached-b"))
        val files = urls.map { url -> File(context.filesDir, "filters/${io.github.ulysseszh.pwainstaller.core.PackageNames.digest("SHA-256", url.toByteArray())}.txt") }
        try {
            files[0].parentFile!!.mkdirs(); files[0].writeText("reddit.com##.first\nexample.com##.other")
            AdblockSettings.save(context, urls.joinToString("\n"), 0)
            val requests = fixture.requestCount.get()
            val first = AdblockSettings.cachedDefaults(context, "https://www.reddit.com/")
            assertEquals("reddit.com##.first", first)
            assertSame(first, AdblockSettings.cachedDefaults(context, "https://www.reddit.com/r/android/"))
            assertEquals("example.com##.other", AdblockSettings.cachedDefaults(context, "https://example.com/"))
            files[0].writeText("reddit.com##.changed")
            assertEquals("reddit.com##.changed", AdblockSettings.cachedDefaults(context, "https://www.reddit.com/"))
            files[1].writeText("reddit.com##.second")
            assertEquals("reddit.com##.changed\nreddit.com##.second", AdblockSettings.cachedDefaults(context, "https://www.reddit.com/"))
            AdblockSettings.save(context, urls[1], 0)
            assertEquals("reddit.com##.second", AdblockSettings.cachedDefaults(context, "https://www.reddit.com/"))
            assertEquals(requests, fixture.requestCount.get())
        } finally { AdblockSettings.save(context, previousUrls, previousHours); files.forEach { it.delete() } }
    }

    @Test fun cachedDefaultRefreshKeepsLargeExpandedRulesResponsiveAndOffline() {
        val previousUrls = AdblockSettings.urls(context); val previousHours = AdblockSettings.hours(context)
        val url = fixture.url("/large-cached-rules.txt")
        val app = WebApp().apply { set("baseUrl", "https://www.reddit.com/"); set("adblockRules", "##.original"); validate() }
        val cachedFile = File(context.filesDir, "filters/${io.github.ulysseszh.pwainstaller.core.PackageNames.digest("SHA-256", url.toByteArray())}.txt")
        val source = if (InstrumentationRegistry.getArguments().getString("useRedditRules") == "true") File(context.getExternalFilesDir(null), "reddit-rules.txt").readText()
            else (1..35000).joinToString("\n") { "||ad-$it.example^" }
        cachedFile.parentFile!!.mkdirs(); cachedFile.writeText(source)
        AdblockSettings.save(context, url + "\n" + fixture.url("/uncached-rules.txt"), 0)
        val expected = AdblockSettings.cachedDefaults(context, app.s("baseUrl"))
        AppDatabase(context).use { it.save(app) }
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("id", app.s("key"))) as EditorActivity
        val locked = CountDownLatch(1); val release = CountDownLatch(1)
        val holder = Thread { synchronized(AdblockSettings) { locked.countDown(); release.await(45, TimeUnit.SECONDS) } }.apply { start() }
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var last = 0L; var largestGap = 0L
        val heartbeat = object : Runnable {
            override fun run() {
                val now = android.os.SystemClock.uptimeMillis()
                if (last > 0) largestGap = maxOf(largestGap, now - last)
                last = now; handler.postDelayed(this, 20)
            }
        }
        try {
            awaitEditor(editor)
            assertTrue(locked.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { views(editor.root).filterIsInstance<TextView>().single { it.text.toString() == context.getString(R.string.edit_adblock_rules) }.performClick() }
            val input = editorInput(editor, "adblockInput")
            awaitUi("Small rules must be ready") { input.isEnabled && input.text.toString().contains("original") }
            val requests = fixture.requestCount.get()
            instrumentation.runOnMainSync {
                handler.post(heartbeat)
                views(editor.root).filterIsInstance<TextView>().single { it.text.toString() == context.getString(R.string.refresh_default_rules) }.performClick()
            }
            awaitUi("Cached refresh must finish without waiting for a global download lock") { !editor.busy && input.isEnabled && input.text.toString() != "##.original" }
            Thread.sleep(100) // Include the first layout/draw after the new page is attached.
            instrumentation.runOnMainSync {
                handler.removeCallbacks(heartbeat)
                assertTrue("UI was stalled for $largestGap ms", largestGap < 1000)
                assertTrue(input.length() <= io.github.ulysseszh.pwainstaller.core.RulePages.CHARACTERS)
                fun read() = EditorActivity::class.java.getDeclaredMethod("currentRules").apply { isAccessible = true }.invoke(editor) as String
                assertEquals(expected, read())
                input.append("! page edit\n")
                val edited = read()
                views(editor.root).filterIsInstance<TextView>().single { it.text.toString() == context.getString(R.string.next_rules_page) }.performClick()
                views(editor.root).filterIsInstance<TextView>().single { it.text.toString() == context.getString(R.string.previous_rules_page) }.performClick()
                assertTrue(input.text.toString().contains("page edit")); assertEquals(edited, read())
            }
            assertEquals("No cached-refresh request may reach the network, including missing lists", requests, fixture.requestCount.get())
            assertEquals("##.original", AppDatabase(context).use { it.get(app.s("key"))!!.s("adblockRules") })
        } finally {
            release.countDown(); holder.join(5000); handler.removeCallbacks(heartbeat)
            instrumentation.runOnMainSync { editor.finish() }
            AppDatabase(context).use { it.remove(app) }; AdblockSettings.save(context, previousUrls, previousHours); cachedFile.delete()
        }
    }

    @Test fun adblockRulesScrollWithTouchInsideTheEditor() {
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/"))
            set("adblockRules", (1..100).joinToString("\n") { "127.0.0.1##.rule-$it" }); validate()
        }
        AppDatabase(context).use { it.save(app) }
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("id", app.s("key"))) as EditorActivity
        awaitEditor(editor)
        try {
            instrumentation.runOnMainSync {
                views(editor.root).filterIsInstance<TextView>().single { it.text.toString() == context.getString(R.string.edit_adblock_rules) }.performClick()
            }
            val input = editorInput(editor, "adblockInput")
            awaitUi("Adblock rules did not become scrollable") { input.height > 0 && input.canScrollVertically(1) }
            val position = IntArray(2)
            var startY = 0; var endY = 0; var pageScroll = 0
            val page = views(editor.root).filterIsInstance<android.widget.ScrollView>().single()
            instrumentation.runOnMainSync {
                input.requestRectangleOnScreen(android.graphics.Rect(0, 0, input.width, input.height), true)
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                input.scrollTo(0, 0); input.getLocationOnScreen(position)
                position[0] += input.width / 2
                startY = position[1] + input.height * 3 / 4; endY = position[1] + input.height / 4
                pageScroll = page.scrollY
            }
            shell("input swipe ${position[0]} $startY ${position[0]} $endY 500")
            awaitUi("Touchscreen swipe did not scroll the adblock rules") { input.scrollY > 0 }
            instrumentation.runOnMainSync { assertEquals("The page must not intercept rule-editor swipes", pageScroll, page.scrollY) }
        } finally { instrumentation.runOnMainSync { editor.finish() }; AppDatabase(context).use { it.remove(app) } }
    }

    @Test fun signingKeyLoadingContinuesAfterSettingsRecreation() {
        val entered = CountDownLatch(1); val release = CountDownLatch(1)
        InstallerApplication.worker.execute { entered.countDown(); release.await(45, TimeUnit.SECONDS) }
        assertTrue(entered.await(10, TimeUnit.SECONDS))
        var settings: SettingsActivity? = null
        try {
            settings = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
            val previous = settings
            instrumentation.runOnMainSync {
                assertTrue(previous.busy)
                assertTrue(views(previous.root).filterIsInstance<TextView>().any { it.text.toString() == context.getString(R.string.loading_signing_key) })
                previous.recreate()
            }
            var next: SettingsActivity? = null
            awaitUi("Settings did not recreate while the signing key was loading") {
                next = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .filterIsInstance<SettingsActivity>().singleOrNull { it !== previous }
                next != null
            }
            settings = requireNotNull(next)
            val current = settings
            instrumentation.runOnMainSync {
                assertTrue(current.busy)
                assertTrue(views(current.root).filterIsInstance<TextView>().any { it.text.toString() == context.getString(R.string.loading_signing_key) })
            }
            release.countDown()
            awaitUi("Recreated settings did not finish loading the signing key") {
                !current.busy && views(current.root).filterIsInstance<TextView>().any {
                    it.text.toString().startsWith(context.getString(R.string.signature_fingerprint, "")) && it.text.length > context.getString(R.string.signature_fingerprint, "").length
                }
            }
            instrumentation.runOnMainSync {
                assertFalse(views(current.root).filterIsInstance<TextView>().any { it.text.toString() == context.getString(R.string.loading_signing_key) })
            }
        } finally { release.countDown(); settings?.let { screen -> instrumentation.runOnMainSync { screen.finish() } } }
    }

    @Test fun keystoreImportConfirmsReplacementAndSavesAfterUploadWithoutASaveButton() {
        val original = SigningKeys.settings(context)
        val screen = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
        val upload = File(context.cacheDir, "apks/imported-keystore.p12").apply { parentFile!!.mkdirs() }
        var imported: File? = null
        fun input(name: String) = SettingsActivity::class.java.getDeclaredField(name).apply { isAccessible = true }.get(screen) as TextView
        try {
            awaitUi("Signing key did not load") { !screen.busy }
            instrumentation.runOnMainSync {
                assertTrue(views(screen.root).filterIsInstance<android.widget.Button>().none { it.text.toString() == context.getString(R.string.save) })
                views(screen.root).filterIsInstance<android.widget.Button>().single { it.text.toString() == context.getString(R.string.choose_keystore) }.performClick()
            }
            awaitNative("Import must confirm replacement before selecting a file") { activeNativeNodes().any { it.text?.toString() == context.getString(R.string.replace_signing_key) } }
            clickText(context.getString(android.R.string.cancel))
            assertEquals(original.toString(), SigningKeys.settings(context).toString())
            File(context.filesDir, original.getString("file")).copyTo(upload, overwrite = true)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", upload)
            instrumentation.runOnMainSync {
                input("storePassword").text = "incorrect password"
                SettingsActivity::class.java.getDeclaredMethod("onActivityResult", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Intent::class.java)
                    .apply { isAccessible = true }.invoke(screen, 51, android.app.Activity.RESULT_OK, Intent().setData(uri))
            }
            awaitUi("Invalid upload must report the error without replacing the key") {
                !screen.busy && (SettingsActivity::class.java.getDeclaredField("signingStatus").apply { isAccessible = true }.get(screen) as TextView).text.toString().contains("Keystore was not imported")
            }
            assertEquals(original.toString(), SigningKeys.settings(context).toString())
            instrumentation.runOnMainSync { input("storePassword").text = original.getString("storePassword") }
            awaitUi("Correcting credentials must import automatically") {
                !screen.busy && SigningKeys.settings(context).getString("file") != original.getString("file")
            }
            val config = SigningKeys.settings(context)
            imported = File(context.filesDir, config.getString("file"))
            assertArrayEquals(upload.readBytes(), imported!!.readBytes())
            assertFalse(screen.isFinishing)
            assertArrayEquals(SigningKeys.load(context).certificates.first().encoded,
                SigningKeys.load(upload, original.getString("type"), original.getString("alias"), original.getString("storePassword"), original.getString("keyPassword")).certificates.first().encoded)
        } finally {
            instrumentation.runOnMainSync { screen.finish() }
            SigningKeys.save(context, original); upload.delete(); imported?.delete()
        }
    }

    @Test fun settingsExportsTheActiveKeystoreWithoutChangingItsFormatOrPasswords() {
        val main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        instrumentation.runOnMainSync {
            assertEquals(context.getString(R.string.settings), main.toolbar.menu.findItem(3).title.toString())
            assertEquals(context.getString(R.string.reinstall_all), main.toolbar.menu.findItem(6).title.toString())
            main.finish()
        }
        val settings = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
        val output = File(context.cacheDir, "apks/exported-keystore.p12").apply { parentFile!!.mkdirs() }
        try {
            val deadline = System.currentTimeMillis() + 10000
            var busy = true
            while (busy && System.currentTimeMillis() < deadline) { Thread.sleep(50); instrumentation.runOnMainSync { busy = settings.busy } }
            val config = SigningKeys.settings(context)
            val original = File(context.filesDir, config.getString("file")).readBytes()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", output)
            instrumentation.runOnMainSync {
                assertTrue(views(settings.root).filterIsInstance<TextView>().any { it.text.toString() == context.getString(R.string.export_keystore) })
                assertTrue(views(settings.root).filterIsInstance<TextView>().any { it.text.toString() == context.getString(R.string.adblock) })
                SettingsActivity::class.java.getDeclaredField("exportKeystore").apply { isAccessible = true }.set(settings, config.getString("file"))
                SettingsActivity::class.java.getDeclaredMethod("onActivityResult", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Intent::class.java)
                    .apply { isAccessible = true }.invoke(settings, 52, android.app.Activity.RESULT_OK, Intent().setData(uri))
            }
            busy = true
            val exportDeadline = System.currentTimeMillis() + 10000
            while (busy && System.currentTimeMillis() < exportDeadline) { Thread.sleep(50); instrumentation.runOnMainSync { busy = settings.busy } }
            assertArrayEquals(original, output.readBytes())
            SigningKeys.load(output, config.getString("type"), config.getString("alias"), config.getString("storePassword"), config.getString("keyPassword"))
        } finally { instrumentation.runOnMainSync { settings.finish() }; output.delete() }
    }

    @Test fun backgroundBuildsRemainInteractiveShowQueueNotificationsAndKeepLogs() {
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        val oldBuilders = BuildSettings.builders(context)
        BuildSettings.save(context, 2)
        val apps = (1..3).map { number -> WebApp().apply {
            set("baseUrl", fixture.url("/build-$number/")); manifest().put("name", "Background build $number"); validate()
        } }
        AppDatabase(context).use { db -> apps.forEach(db::save) }
        val ids = apps.map { "test-build-${it.s("key")}" }
        val signing = CountDownLatch(2); val release = CountDownLatch(1)
        var main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        try {
            instrumentation.runOnMainSync {
                apps.forEachIndexed { index, app ->
                    assertTrue(BuildJobs.start(context, ids[index], app.s("key"), app.name()) { report, log ->
                        log("Diagnostic for ${app.name()}")
                        if (index == 2) throw IOException("Controlled APK failure")
                        val apk = ApkBuilder.buildLogged(context, app, { progress ->
                            report(progress)
                            if (progress.stage == io.github.ulysseszh.pwainstaller.core.ApkProgress.Stage.SIGNING) {
                                signing.countDown(); check(release.await(45, TimeUnit.SECONDS))
                            }
                        }, log)
                        assertTrue(apk.length() < 400 * 1024); apk.delete(); null
                    })
                }
                MainActivity::class.java.getDeclaredMethod("renderBuild").apply { isAccessible = true }.invoke(main)
            }
            assertTrue("Two APK builds must enter signing concurrently", signing.await(30, TimeUnit.SECONDS))
            assertTrue("Third build must remain queued", BuildJobs.get(ids[2])!!.queued)
            awaitUi("List must show building and queued apps without blocking the UI") {
                !main.busy && views(main.root).filterIsInstance<TextView>().any { it.text.toString().contains(context.getString(R.string.build_signing)) } &&
                    views(main.root).filterIsInstance<TextView>().any { it.text.toString() == context.getString(R.string.build_queued) }
            }
            awaitNative("Foreground build notification must be present") {
                val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                manager.getRunningServices(100).any { it.service.className == BuildService::class.java.name && it.foreground }
            }
            if (Build.VERSION.SDK_INT >= 23) {
                awaitNative("Progress notifications must name the apps") {
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    val titles = manager.activeNotifications.map { it.notification.extras.getCharSequence("android.title").toString() }
                    apps.all { app -> titles.count { it == app.name() } == 1 }
                }
            }
            // Metadata work and navigation must remain available while both builders wait.
            instrumentation.runOnMainSync { main.toolbar.menu.performIdentifierAction(3, 0) }
            var settings: SettingsActivity? = null
            awaitUi("Settings must open while APKs are building") {
                settings = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .filterIsInstance<SettingsActivity>().singleOrNull()
                settings?.busy == false
            }
            instrumentation.runOnMainSync { settings!!.finish() }
            awaitUi("Main must regain focus before recreation") { main.hasWindowFocus() && !main.isFinishing }
            val previous = main
            instrumentation.runOnMainSync { main.recreate() }
            var next: MainActivity? = null
            awaitUi("Build status must survive recreation") {
                next = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .filterIsInstance<MainActivity>().firstOrNull { it !== previous }
                next != null && !next!!.busy
            }
            main = requireNotNull(next)
            shell("input keyevent KEYCODE_HOME")
            awaitUi("Main must stop before background completion") {
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.STOPPED).contains(main)
            }
            assertEquals(2, BuildJobs.active().count { !it.queued })
            release.countDown()
            awaitNative("APKs must finish while the UI is in the background") { ids.all { BuildJobs.get(it)?.done == true } }
            shell("am start -n ${context.packageName}/.MainActivity")
            awaitUi("Main must resume after background work") {
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .filterIsInstance<MainActivity>().firstOrNull()?.let { main = it; true } ?: false
            }
            clickText(context.getString(R.string.view_build_logs))
            awaitNative("Failure dialog must navigate to its saved build log") {
                activeNativeNodes().any { it.text?.toString()?.contains("Controlled APK failure") == true }
            }
            val logScreen = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
            instrumentation.runOnMainSync { logScreen.getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).filterIsInstance<BuildLogActivity>().forEach { it.finish() } }
            awaitNative("Completed background jobs must be consumed") { ids.all { BuildJobs.get(it) == null } }
            assertTrue(BuildLogs.file(context, apps[0].s("key")).readText().contains("Signed APK:"))
            assertTrue(BuildLogs.file(context, apps[2].s("key")).readText().contains("Controlled APK failure"))
            awaitUi("Main must regain focus after viewing failure logs") { main.hasWindowFocus() }
            Thread.sleep(300)
            awaitUi("Completed app must offer its logs in the item menu") {
                views(main.root).filterIsInstance<com.google.android.material.card.MaterialCardView>().firstOrNull { card ->
                    views(card).filterIsInstance<TextView>().any { it.text.toString() == apps[0].name() }
                }?.let { it.performLongClick(); true } ?: false
            }
            clickText(context.getString(R.string.view_build_logs))
            awaitNative("Last successful build log must remain available after the job ends") {
                activeNativeNodes().any { it.text?.toString()?.contains("Building ${apps[0].name()}") == true }
            }
        } finally {
            release.countDown()
            instrumentation.runOnMainSync {
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .filterIsInstance<Screen>().forEach { it.finish() }
                main.finish()
            }
            ids.forEach(BuildJobs::remove)
            BuildSettings.save(context, oldBuilders)
            AppDatabase(context).use { db -> apps.forEach(db::remove) }
        }
    }

    @Test fun rejectedBackgroundServiceStartRetainsAnInterruptedRequest() {
        val app = WebApp().apply { set("baseUrl", fixture.url("/blocked-start")); validate() }
        val token = InstallCoordinator.prepare(context, app, null)
        val blocked = object : android.content.ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
            override fun startService(service: Intent): android.content.ComponentName? = throw IllegalStateException("Blocked foreground start")
            @android.annotation.TargetApi(26)
            override fun startForegroundService(service: Intent): android.content.ComponentName? = throw IllegalStateException("Blocked foreground start")
        }
        try {
            try { InstallPipeline.start(blocked, token); fail("Foreground start must fail in this fixture") }
            catch (failure: IllegalStateException) { assertEquals("Blocked foreground start", failure.message) }
            assertTrue(InstallCoordinator.interrupted(context, token))
            assertNull(BuildJobs.get("install:$token"))
            assertEquals("prepared", InstallCoordinator.summary(context, token).getString("state"))
        } finally { InstallCoordinator.discard(context, token, true) }
    }

    @Test fun backgroundInstallationBatchAdvancesWithoutAnActivity() {
        Assume.assumeTrue(InstallCoordinator.tokens(context).isEmpty() && !ReinstallQueue.has(context))
        if (Build.VERSION.SDK_INT >= 26) shell("appops set ${context.packageName} REQUEST_INSTALL_PACKAGES allow")
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        val oldBuilders = BuildSettings.builders(context); val oldMethod = InstallationSettings.method(context)
        BuildSettings.save(context, 1); InstallationSettings.save(context, "system")
        val apps = (1..3).map { index -> WebApp().apply {
            set("baseUrl", fixture.url("/background-install-$index")); manifest().put("name", "Background install $index"); validate()
        } }
        AppDatabase(context).use { db -> apps.forEach(db::save) }
        val main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        val completions = CopyOnWriteArrayList<String>()
        val accessibility = instrumentation.uiAutomation.serviceInfo
        val eventTypes = accessibility.eventTypes
        accessibility.eventTypes = accessibility.eventTypes or android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
        instrumentation.uiAutomation.serviceInfo = accessibility
        instrumentation.uiAutomation.setOnAccessibilityEventListener { event ->
            if (event.eventType == android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
                completions.add(event.text.joinToString("\n"))
        }
        try {
            awaitUi("Library must load") { main.toolbar.menu.findItem(6).isEnabled }
            instrumentation.runOnMainSync { main.toolbar.menu.performIdentifierAction(6, 0) }
            awaitNative("The batch must be authorized before leaving the activity") {
                InstallCoordinator.tokens(context).any { InstallCoordinator.summary(context, it).optBoolean("background") }
            }
            instrumentation.runOnMainSync { main.finish() }
            awaitUi("The activity must be destroyed") { main.isDestroyed }
            repeat(3) {
                awaitNative("The service must build and submit the next APK without an activity") {
                    InstallCoordinator.tokens(context).any { InstallCoordinator.summary(context, it).getString("state") == "confirmation" }
                }
                val token = InstallCoordinator.tokens(context).single { InstallCoordinator.summary(context, it).getString("state") == "confirmation" }
                val plan = InstallCoordinator.summary(context, token)
                val uri = plan.getString("confirmation").replace("'", "'\\''")
                shell("am start '$uri'") // Simulate the user opening the system confirmation directly.
                clickText("Install", "com.android.packageinstaller")
                val key = plan.getJSONObject("app").getString("key")
                awaitNative("Installation, metadata persistence and queue advancement must finish in the service") {
                    key !in ReinstallQueue.pendingKeys(context) && AppDatabase(context).use { it.get(key) != null } &&
                        Packages.installed(context, plan.getJSONObject("app").getString("packageName"))
                }
                val message = context.getString(R.string.install_success, WebApp.name(plan.getJSONObject("app")))
                try {
                    awaitNative("Background completion must display the app-named installation toast") { completions.any { it.contains(message) } }
                } catch (failure: AssertionError) { throw AssertionError("Expected toast: $message; received: $completions", failure) }
                instrumentation.runOnMainSync {
                    assertTrue(main.isDestroyed)
                    assertTrue(androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).none { it is MainActivity })
                }
            }
            assertFalse(ReinstallQueue.has(context))
            if (Build.VERSION.SDK_INT >= 23) awaitNative("No build notification, including the generic foreground placeholder, may remain after completion") {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.activeNotifications.none { if (Build.VERSION.SDK_INT >= 26) it.notification.channelId == ProgressNotifications.BUILDS
                    else it.id == 1901 || it.id in 10000 until 0x40000000 }
            }
            awaitNative("The processing wake lock must be released after the batch finishes") {
                !shell("dumpsys power").lineSequence().any { it.contains("PARTIAL_WAKE_LOCK") && it.contains("${context.packageName}:apk-work") }
            }
        } finally {
            instrumentation.uiAutomation.setOnAccessibilityEventListener(null)
            accessibility.eventTypes = eventTypes; instrumentation.uiAutomation.serviceInfo = accessibility
            context.getSharedPreferences("reinstall-all", 0).edit().clear().commit()
            instrumentation.runOnMainSync { main.finish() }
            InstallCoordinator.tokens(context).forEach { token ->
                val plan = InstallCoordinator.summary(context, token)
                if (plan.has("session")) runCatching { context.packageManager.packageInstaller.abandonSession(plan.getInt("session")) }
                InstallCoordinator.discard(context, token, true)
            }
            apps.forEach { shell("pm uninstall ${it.s("packageName")}") }
            AppDatabase(context).use { db -> apps.forEach(db::remove) }
            BuildSettings.save(context, oldBuilders); InstallationSettings.save(context, oldMethod)
            shell("input keyevent KEYCODE_BACK")
        }
    }

    @Test fun seedBackgroundBuildForProcessRestartCheck() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("recoveryProbe") == "true")
        if (Build.VERSION.SDK_INT >= 26) shell("appops set ${context.packageName} REQUEST_INSTALL_PACKAGES allow")
        val screen = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
        val app = WebApp().apply { set("baseUrl", "https://restart-fixture.invalid/"); manifest().put("name", "Recovery fixture"); validate() }
        val token = InstallCoordinator.prepare(context, app, null)
        InstallCoordinator.authorizeBackground(context, token)
        File(context.filesDir, "recovery-probe-token").writeText(token)
        instrumentation.runOnMainSync {
            assertTrue(BuildJobs.start(context, "install:$token", app.s("key"), app.name()) { report, _ ->
                report(io.github.ulysseszh.pwainstaller.core.ApkProgress(io.github.ulysseszh.pwainstaller.core.ApkProgress.Stage.FILTERS, 10))
                Thread.sleep(120000); null
            })
            screen.finish()
        }
        awaitNative("A running background build must hold a processing wake lock") {
            shell("dumpsys power").lineSequence().any { it.contains("PARTIAL_WAKE_LOCK") && it.contains("${context.packageName}:apk-work") }
        }
    }

    @Test fun cleanupBackgroundProcessRestartCheck() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("recoveryProbe") == "true")
        val marker = File(context.filesDir, "recovery-probe-token")
        val token = marker.readText()
        val plan = InstallCoordinator.summary(context, token)
        assertEquals("confirmation", plan.getString("state"))
        assertTrue(File(plan.getString("apk")).isFile)
        context.packageManager.packageInstaller.abandonSession(plan.getInt("session"))
        InstallCoordinator.discard(context, token, true); BuildJobs.remove("install:$token"); marker.delete()
    }

    @android.annotation.TargetApi(26)
    @Test fun mediaDisplayEventsDoNotDetachTheWebViewAndFullscreenPreservesScroll() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 26)
        val screen = instrumentation.startActivitySync(Intent(context, BrowserTestActivity::class.java)
            .putExtra("url", fixture.url("/media-lifecycle")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as BrowserTestActivity
        val detaches = java.util.concurrent.atomic.AtomicInteger()
        val hidden = AtomicBoolean()
        try {
            awaitUi("Media fixture must load") { screen.page.progress == 100 && screen.page.contentHeight > 2000 }
            instrumentation.runOnMainSync {
                screen.page.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: View) = Unit
                    override fun onViewDetachedFromWindow(view: View) { detaches.incrementAndGet() }
                })
                val listener = io.github.ulysseszh.pwainstaller.browser.BrowserActivity::class.java.getDeclaredField("displayChanges")
                    .apply { isAccessible = true }.get(screen) as android.hardware.display.DisplayManager.DisplayListener
                repeat(20) { listener.onDisplayChanged(screen.pageRoot.display.displayId) }
                listener.onDisplayAdded(screen.pageRoot.display.displayId + 1000)
                screen.page.scrollTo(0, 1000)
            }
            Thread.sleep(300)
            assertEquals("Refresh-rate-only and unrelated-display events must not recreate the surface", 0, detaches.get())
            var beforeHeight = 0; var beforeScroll = 0
            instrumentation.runOnMainSync {
                beforeHeight = screen.page.height; beforeScroll = screen.page.scrollY
                assertTrue(beforeScroll > 0)
                screen.page.webChromeClient!!.onShowCustomView(android.widget.FrameLayout(screen), object : android.webkit.WebChromeClient.CustomViewCallback {
                    override fun onCustomViewHidden() { hidden.set(true) }
                })
            }
            Thread.sleep(300)
            instrumentation.runOnMainSync {
                assertTrue("The document must stay visible beneath the fullscreen overlay", screen.page.isShown)
                assertEquals(beforeHeight, screen.page.height); assertEquals(beforeScroll, screen.page.scrollY)
                screen.page.webChromeClient!!.onHideCustomView()
            }
            Thread.sleep(300)
            instrumentation.runOnMainSync {
                assertTrue(hidden.get()); assertEquals(beforeHeight, screen.page.height)
                assertTrue("Fullscreen scroll must be preserved within CSS pixel rounding", kotlin.math.abs(beforeScroll - screen.page.scrollY) <= 2)
                assertEquals(0, detaches.get())
            }
        } finally { instrumentation.runOnMainSync { screen.finish() } }
    }

    @Test fun playingVideoFullscreenReturnsToTheSameDocumentScrollPosition() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 26)
        val screen = instrumentation.startActivitySync(Intent(context, BrowserTestActivity::class.java)
            .putExtra("url", fixture.url("/video-scroll")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as BrowserTestActivity
        val fullscreen = io.github.ulysseszh.pwainstaller.browser.BrowserActivity::class.java.getDeclaredField("fullscreen").apply { isAccessible = true }
        var before = 0
        try {
            awaitUi("The scrollable video page must load") { screen.page.progress == 100 && screen.page.contentHeight > 2000 }
            for (position in listOf(1000, 1600, 800, 2000, 1200)) {
                // A real input cancels the brief exit guard before the next user
                // scroll. A bare native scrollTo would mimic Chromium's reset.
                var x = 0; var y = 0
                instrumentation.runOnMainSync {
                    val location = IntArray(2); screen.page.getLocationOnScreen(location)
                    x = location[0] + screen.page.width * 9 / 10; y = location[1] + screen.page.height / 2
                }
                shell("input tap $x $y"); Thread.sleep(100)
                instrumentation.runOnMainSync { screen.page.scrollTo(0, position) }
                Thread.sleep(100)
                awaitUi("The document must be scrolled before entering fullscreen") { screen.page.scrollY == position }
                instrumentation.runOnMainSync { before = screen.page.scrollY }
                clickText("Fullscreen video", context.packageName)
                awaitUi("The playing video must use the native fullscreen overlay") { fullscreen.get(screen) != null }
                awaitNative("The captured video stream must actually be playing") { fixture.readyPaths.contains("/video-playing") }
                var captured = -1
                instrumentation.runOnMainSync {
                    val saved = io.github.ulysseszh.pwainstaller.browser.BrowserActivity::class.java.getDeclaredField("fullscreenScroll").apply { isAccessible = true }.get(screen)
                    captured = saved.javaClass.getDeclaredField("y").apply { isAccessible = true }.getInt(saved)
                }
                Thread.sleep(250)
                instrumentation.runOnMainSync { screen.onBackPressed() }
                awaitUi("Closing video fullscreen must restore the document") { fullscreen.get(screen) == null }
                Thread.sleep(500)
                instrumentation.runOnMainSync { assertTrue("Video fullscreen must preserve document scroll (entry snapshot=$captured, before=$before, after=${screen.page.scrollY})", kotlin.math.abs(before - screen.page.scrollY) <= 2) }
            }
        } finally { instrumentation.runOnMainSync { screen.finish() } }
    }

    @Test fun builderLimitAppliesLiveWithoutInterruptingRunningJobs() {
        val old = BuildSettings.builders(context)
        val ids = (0..3).map { "builder-limit-${UUID.randomUUID()}" }
        val started = List(4) { CountDownLatch(1) }; val release = List(4) { CountDownLatch(1) }
        val screen = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
        fun limit(count: Int) {
            instrumentation.runOnMainSync {
                val input = SettingsActivity::class.java.getDeclaredField("parallelBuilders").apply { isAccessible = true }.get(screen) as SelectionInput
                input.value = count.toString()
            }
            assertEquals(count, BuildSettings.builders(context))
        }
        fun start(index: Int) {
            instrumentation.runOnMainSync {
                assertTrue(BuildJobs.start(context, ids[index], ids[index], "Builder limit $index") { report, _ ->
                    report(io.github.ulysseszh.pwainstaller.core.ApkProgress(io.github.ulysseszh.pwainstaller.core.ApkProgress.Stage.SIGNING, 50))
                    started[index].countDown(); check(release[index].await(45, TimeUnit.SECONDS)); null
                })
            }
        }
        try {
            limit(1); (0..2).forEach(::start)
            assertTrue(started[0].await(10, TimeUnit.SECONDS))
            assertTrue(BuildJobs.get(ids[1])!!.queued); assertTrue(BuildJobs.get(ids[2])!!.queued)
            limit(3)
            assertTrue(started[1].await(10, TimeUnit.SECONDS)); assertTrue(started[2].await(10, TimeUnit.SECONDS))
            limit(1); start(3)
            assertTrue((0..2).all { BuildJobs.get(ids[it])?.let { state -> !state.queued && !state.done } == true })
            release[0].countDown(); release[1].countDown()
            awaitNative("Running builds must finish when the limit is lowered") { (0..1).all { BuildJobs.get(ids[it])?.done == true } }
            assertTrue(BuildJobs.get(ids[3])!!.queued)
            release[2].countDown()
            assertTrue(started[3].await(10, TimeUnit.SECONDS)); release[3].countDown()
            awaitNative("All jobs must finish under the new limit") { ids.all { BuildJobs.get(it)?.done == true } }
        } finally {
            release.forEach { it.countDown() }
            val deadline = System.currentTimeMillis() + 10000
            while (ids.any { BuildJobs.get(it)?.done == false } && System.currentTimeMillis() < deadline) Thread.sleep(25)
            ids.forEach(BuildJobs::remove); BuildSettings.save(context, old)
            instrumentation.runOnMainSync { screen.finish() }
        }
    }

    @Test fun everyBuilderKeepsItsNotificationWhileInstallationWorkIsBlocked() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 23)
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        val previousBuilders = BuildSettings.builders(context)
        BuildSettings.save(context, 8)
        val apps = (1..8).map { index -> WebApp().apply {
            set("baseUrl", fixture.url("/notifications-$index/")); manifest().put("name", "Concurrent notice $index"); validate()
        } }
        val ids = apps.map { "notice-test:${it.s("key")}" }
        val pipelineBlocked = CountDownLatch(1); val releasePipeline = CountDownLatch(1)
        val buildersStarted = CountDownLatch(8); val releaseBuilders = CountDownLatch(1)
        val screen = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        fun notices() = manager.activeNotifications.filter { notification ->
            apps.any { it.name() == notification.notification.extras.getCharSequence("android.title")?.toString() }
        }
        try {
            InstallerApplication.installPipeline.execute { pipelineBlocked.countDown(); releasePipeline.await(45, TimeUnit.SECONDS) }
            assertTrue(pipelineBlocked.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                apps.forEachIndexed { index, app ->
                    assertTrue(BuildJobs.start(context, ids[index], app.s("key"), app.name()) { report, _ ->
                        buildersStarted.countDown()
                        var percent = 0
                        while (!releaseBuilders.await(25, TimeUnit.MILLISECONDS)) {
                            report(io.github.ulysseszh.pwainstaller.core.ApkProgress(io.github.ulysseszh.pwainstaller.core.ApkProgress.Stage.FILTERS, percent++ % 100))
                        }
                        null
                    })
                }
            }
            assertTrue("All eight builders must be running", buildersStarted.await(10, TimeUnit.SECONDS))
            try { awaitNative("Every running builder needs its own notification even while installation work is blocked") {
                val active = notices()
                apps.all { app -> active.count { it.notification.extras.getCharSequence("android.title")?.toString() == app.name() } == 1 }
            } } catch (failure: AssertionError) {
                val service = BuildService::class.java.getDeclaredField("running").apply { isAccessible = true }.get(null)
                val fields = if (service == null) "stopped" else listOf("states", "scanning", "advancing", "notifications", "foregroundStarted").associateWith {
                    BuildService::class.java.getDeclaredField(it).apply { isAccessible = true }.get(service)
                }.toString()
                throw AssertionError("${failure.message}; active=" + manager.activeNotifications.map { "${it.id}:${it.notification.extras.getCharSequence("android.title")}" } +
                    "; jobs=${BuildJobs.active().size}; service=$fields", failure)
            }
            val identities = notices().associate { it.notification.extras.getCharSequence("android.title").toString() to it.id }
            Thread.sleep(2000)
            assertEquals("Progress updates must retain all eight notification identities", identities,
                notices().associate { it.notification.extras.getCharSequence("android.title").toString() to it.id })
        } finally {
            releaseBuilders.countDown(); releasePipeline.countDown()
            awaitNative("Builders must finish before cleanup") { ids.all { BuildJobs.get(it)?.done != false } }
            ids.forEach(BuildJobs::remove)
            InstallerApplication.installPipeline.submit { }.get(10, TimeUnit.SECONDS)
            awaitNative("Completed builders must leave no progress notifications") { notices().isEmpty() }
            instrumentation.runOnMainSync { screen.finish() }
            BuildSettings.save(context, previousBuilders)
        }
    }

    @Test fun installationNotificationsKeepOneIdentityAcrossBuildAndInstallAndClearAtTheEnd() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 23)
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        val app = WebApp().apply { set("baseUrl", fixture.url("/notification-install")); manifest().put("name", "Installation progress fixture"); validate() }
        val token = InstallCoordinator.prepare(context, app, null)
        val id = "install:$token"
        val screen = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        fun notifications() = manager.activeNotifications.filter { it.notification.extras.getCharSequence("android.title")?.toString() == app.name() }
        fun notice(label: Int): Int {
            awaitNative("One installation notification must show ${context.getString(label)}") {
                notifications().let { it.size == 1 && it.single().notification.extras.getCharSequence("android.text")?.toString() == context.getString(label) }
            }
            return notifications().single().id
        }
        try {
            instrumentation.runOnMainSync {
                assertTrue(BuildJobs.start(context, id, app.s("key"), app.name()) { report, log -> InstallCoordinator.prepareApk(context, token, report, log); null })
            }
            awaitNative("Install APK must finish building") { BuildJobs.get(id)?.done == true }
            BuildJobs.remove(id)
            val notificationId = notice(R.string.build_ready)
            shell("input keyevent KEYCODE_HOME")
            InstallCoordinator.write(context, token, InstallCoordinator.summary(context, token).put("state", "committed"))
            assertEquals(notificationId, notice(R.string.build_waiting))
            InstallCoordinator.write(context, token, InstallCoordinator.summary(context, token).put("state", "confirmation"))
            assertEquals(notificationId, notice(R.string.install_confirmation_needed))
            InstallCoordinator.installed(context, token)
            assertEquals(notificationId, notice(R.string.install_finishing))
            InstallCoordinator.complete(context, token)
            awaitNative("Completed installation must remove its notification") { notifications().isEmpty() }
            InstallCoordinator.discard(context, token)
            val failure = InstallCoordinator.prepare(context, app, null)
            try {
                InstallCoordinator.write(context, failure, InstallCoordinator.summary(context, failure).put("state", "ready"))
                instrumentation.runOnMainSync { context.startActivity(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                BuildService.start(context); notice(R.string.build_ready)
                InstallCoordinator.fail(context, failure, "Controlled installation failure")
                awaitNative("Failed installation must remove its progress notification") { notifications().isEmpty() }
            } finally { InstallCoordinator.discard(context, failure) }
        } finally {
            BuildJobs.remove(id); InstallCoordinator.discard(context, token, discardBackup = true)
            AppDatabase(context).use { it.remove(app) }
            instrumentation.runOnMainSync {
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .filterIsInstance<SettingsActivity>().forEach { it.finish() }
                screen.finish()
            }
        }
    }

    @Test fun filterFetchNotificationsShowByteProgressAndContinueAfterSettingsCloses() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 26)
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        val oldUrls = AdblockSettings.urls(context); val oldHours = AdblockSettings.hours(context)
        val work = androidx.work.WorkManager.getInstance(context)
        work.cancelUniqueWork(AdblockSettings.FETCH_WORK).result.get()
        work.cancelUniqueWork("adguard-periodic").result.get()
        val urls = listOf(fixture.url("/slow-filters.txt"), fixture.url("/unknown-length-filters.txt"))
        AdblockSettings.save(context, urls.joinToString("\n"), 0)
        val screen = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        fun notices() = manager.activeNotifications.filter { it.notification.channelId == ProgressNotifications.FILTERS }
        var reopened: SettingsActivity? = null
        try {
            instrumentation.runOnMainSync {
                views(screen.root).filterIsInstance<android.widget.Button>().single { it.text.toString() == context.getString(R.string.refresh_filters) }.performClick()
            }
            assertTrue(fixture.filterFetchStarted[0].await(20, TimeUnit.SECONDS))
            awaitNative("A filter download with Content-Length must show byte progress") {
                notices().let { list -> list.size == 1 && list.single().notification.extras.let {
                    !it.getBoolean("android.progressIndeterminate") && it.getInt("android.progress") in 1..49 &&
                        it.getCharSequence("android.text")?.toString() == context.getString(R.string.filters_fetch_progress, 1, 2, "127.0.0.1")
                } }
            }
            val notificationId = notices().single().id
            instrumentation.runOnMainSync { screen.finish() }
            fixture.continueFilters[0].countDown()
            assertTrue(fixture.filterFetchStarted[1].await(20, TimeUnit.SECONDS))
            awaitNative("Unknown-length downloads must keep the same notification with indeterminate progress") {
                notices().let { list -> list.size == 1 && list.single().id == notificationId && list.single().notification.extras.let {
                    it.getBoolean("android.progressIndeterminate") &&
                        it.getCharSequence("android.text")?.toString() == context.getString(R.string.filters_fetch_progress, 2, 2, "127.0.0.1")
                } }
            }
            fixture.continueFilters[1].countDown()
            awaitNative("All filter downloads must finish in the background") {
                AdblockSettings.updated(context) > 0 && work.getWorkInfosForUniqueWork(AdblockSettings.FETCH_WORK).get().any { it.state == androidx.work.WorkInfo.State.SUCCEEDED }
            }
            awaitNative("Completed filter downloads must remove their notification") { notices().isEmpty() }
            assertTrue(AdblockSettings.cachedDefaults(context, fixture.url("/")).contains("downloaded-banner"))
            AdblockSettings.save(context, urls.joinToString("\n"), 168)
            awaitNative("Periodic updates must be scheduled for the future") { work.getWorkInfosForUniqueWork("adguard-periodic").get().any { it.state == androidx.work.WorkInfo.State.ENQUEUED } }
            reopened = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
            val status = SettingsActivity::class.java.getDeclaredField("filterStatus").apply { isAccessible = true }.get(reopened) as TextView
            val expected = context.getString(R.string.filters_updated, java.text.DateFormat.getDateTimeInstance().format(Date(AdblockSettings.updated(context))))
            awaitUi("The settings observer must show the completed update") { !reopened!!.busy && status.text.toString() == expected }
            Thread.sleep(500)
            instrumentation.runOnMainSync { assertEquals("A future periodic update must not appear as a queued download", expected, status.text.toString()) }
        } finally {
            fixture.continueFilters.forEach { it.countDown() }
            work.cancelUniqueWork(AdblockSettings.FETCH_WORK).result.get()
            instrumentation.runOnMainSync { reopened?.finish(); screen.finish() }
            AdblockSettings.save(context, oldUrls, oldHours)
        }
    }

    @Test fun buildLogsUpdateLivePreserveTextSelectionAndCopyTheWholeLog() {
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        val key = UUID.randomUUID().toString(); val id = "live-log-$key"
        val first = CountDownLatch(1); val more = CountDownLatch(1); val second = CountDownLatch(1); val finish = CountDownLatch(1)
        val main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        var viewer: BuildLogActivity? = null
        try {
            awaitUi("Main must have focus before starting a foreground build") { main.hasWindowFocus() }
            instrumentation.runOnMainSync {
                assertTrue(BuildJobs.start(context, id, key, "Live logs fixture") { _, log ->
                    log("First build phase\nSelectable second line"); first.countDown(); check(more.await(30, TimeUnit.SECONDS))
                    log("Second build phase"); second.countDown(); check(finish.await(30, TimeUnit.SECONDS)); null
                })
            }
            assertTrue(first.await(10, TimeUnit.SECONDS))
            viewer = instrumentation.startActivitySync(Intent(context, BuildLogActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("key", key)) as BuildLogActivity
            val current = viewer
            fun text() = views(current.root).filterIsInstance<TextView>().singleOrNull { it.text.toString().contains("First build phase") }
            awaitUi("Current build log must load") { text()?.isTextSelectable == true && current.hasWindowFocus() }
            var selectedEnd = 0
            instrumentation.runOnMainSync {
                val view = requireNotNull(text()); selectedEnd = view.text.toString().indexOf("Selectable second line") + 10
                android.text.Selection.setSelection(view.text as android.text.Spannable, 0, selectedEnd)
            }
            more.countDown(); assertTrue(second.await(10, TimeUnit.SECONDS))
            awaitUi("New build log lines must appear without reopening the viewer") { text()?.text.toString().contains("Second build phase") }
            instrumentation.runOnMainSync {
                assertEquals(0, requireNotNull(text()).selectionStart); assertEquals(selectedEnd, requireNotNull(text()).selectionEnd)
                current.toolbar.menu.performIdentifierAction(0, 0)
            }
            val expected = BuildLogs.file(context, key).readText()
            awaitUi("Copy must include every log line, including lines outside the selection") {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).primaryClip?.getItemAt(0)?.text?.toString() == expected
            }
            finish.countDown()
            awaitNative("Controlled build must finish") { BuildJobs.get(id)?.done == true }
            awaitUi("Live logs must include the final completion line") { text()?.text.toString().contains("Build completed successfully") }
        } finally {
            more.countDown(); finish.countDown()
            instrumentation.runOnMainSync { viewer?.finish(); main.finish() }
            awaitNative("Build must stop before cleaning its log") { BuildJobs.get(id)?.done != false }
            BuildJobs.remove(id); BuildLogs.file(context, key).delete()
        }
    }

    @Test fun tileProgressOverlaysItsIconAndDisappearsAfterInstallationCompletes() {
        Assume.assumeTrue(InstallCoordinator.tokens(context).isEmpty() && !ReinstallQueue.has(context))
        val previousLayout = AppearanceSettings.layout(context)
        AppearanceSettings.save(context, "tiles")
        val app = WebApp().apply { set("baseUrl", fixture.url("/finished-build/")); manifest().put("name", "Finished build fixture"); validate() }
        AppDatabase(context).use { it.save(app) }
        ReinstallQueue.begin(context)
        val token = requireNotNull(ReinstallQueue.next(context))
        InstallCoordinator.write(context, token, InstallCoordinator.read(context, token).put("state", "committed"))
        val main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        fun tile() = views(main.root).filterIsInstance<com.google.android.material.card.MaterialCardView>().firstOrNull {
            views(it).filterIsInstance<TextView>().any { text -> text.text.toString() == app.name() }
        }
        try {
            awaitUi("Pending installation must show a progress overlay on its own tile") {
                tile()?.let { card -> views(card).filterIsInstance<com.google.android.material.progressindicator.LinearProgressIndicator>().any { it.isShown } } == true
            }
            var height = 0
            instrumentation.runOnMainSync {
                val card = requireNotNull(tile()); height = card.height
                val progress = views(card).filterIsInstance<com.google.android.material.progressindicator.LinearProgressIndicator>().single()
                val icon = views(card).filterIsInstance<android.widget.ImageView>().single()
                assertSame(icon.parent, progress.parent)
                assertTrue(card.contentDescription.toString().contains(context.getString(R.string.install_pending)))
                assertFalse(views(card).filterIsInstance<TextView>().any { it.isShown && it.text.toString() == context.getString(R.string.install_pending) })
            }
            InstallCoordinator.installed(context, token)
            context.sendBroadcast(Intent(InstallReceiver.CHANGED).setPackage(context.packageName))
            awaitUi("Completed installation must leave no queued status or progress") {
                token !in InstallCoordinator.tokens(context) && !ReinstallQueue.has(context) && tile()?.let { card ->
                    views(card).filterIsInstance<com.google.android.material.progressindicator.LinearProgressIndicator>().none { it.isShown } &&
                        card.contentDescription?.toString() == app.name() && card.height == height
                } == true
            }
        } finally {
            instrumentation.runOnMainSync { main.finish() }
            context.getSharedPreferences("reinstall-all", 0).edit().clear().commit()
            if (token in InstallCoordinator.tokens(context)) InstallCoordinator.discard(context, token, true)
            AppDatabase(context).use { it.remove(app) }; AppearanceSettings.save(context, previousLayout)
        }
    }

    @Test fun reinstallRequestsShowQueuedProgressBeforeMetadataWorkStarts() {
        val previousLayout = AppearanceSettings.layout(context); AppearanceSettings.save(context, "tiles")
        val app = WebApp().apply { set("baseUrl", fixture.url("/immediate-build/")); manifest().put("name", "Immediate build fixture"); validate() }
        AppDatabase(context).use { it.save(app) }
        val main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        val locked = CountDownLatch(1); val release = CountDownLatch(1)
        fun tile() = views(main.root).filterIsInstance<com.google.android.material.card.MaterialCardView>().firstOrNull {
            views(it).filterIsInstance<TextView>().any { text -> text.text.toString() == app.name() }
        }
        try {
            awaitUi("Library must load") { tile() != null && main.hasWindowFocus() }
            InstallerApplication.worker.execute { locked.countDown(); release.await(30, TimeUnit.SECONDS) }
            assertTrue(locked.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { requireNotNull(tile()).performLongClick() }
            clickText(context.getString(R.string.reinstall), context.packageName)
            awaitUi("Queued icon progress must appear before metadata preparation can run") {
                main.busy && tile()?.let { card -> card.contentDescription.toString().contains(context.getString(R.string.build_queued)) &&
                    views(card).filterIsInstance<com.google.android.material.progressindicator.LinearProgressIndicator>().any { it.isShown } } == true
            }
        } finally {
            // Close the test activity before releasing its metadata task, so no installation is submitted.
            instrumentation.runOnMainSync { main.finish() }; release.countDown()
            InstallerApplication.worker.submit { }.get(10, TimeUnit.SECONDS)
            InstallCoordinator.tokens(context).filter { InstallCoordinator.read(context, it).getJSONObject("app").getString("key") == app.s("key") }
                .forEach { InstallCoordinator.discard(context, it, true) }
            AppDatabase(context).use { it.remove(app) }; AppearanceSettings.save(context, previousLayout)
        }
    }

    @Test fun reinstallAllStartsPreparedBuildersBeforeTheRestOfTheBatchIsPrepared() {
        Assume.assumeTrue(InstallCoordinator.tokens(context).isEmpty() && !ReinstallQueue.has(context))
        if (Build.VERSION.SDK_INT >= 26) shell("appops set ${context.packageName} REQUEST_INSTALL_PACKAGES allow")
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        val previousBuilders = BuildSettings.builders(context)
        BuildSettings.save(context, 4)
        val apps = (1..5).map { number -> WebApp().apply {
            set("baseUrl", fixture.url("/startup-$number/")); manifest().put("name", "Batch startup $number"); validate()
        } }
        AppDatabase(context).use { db -> apps.forEach(db::save) }
        val screen = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
        val pipelineBlocked = CountDownLatch(1); val releasePipeline = CountDownLatch(1)
        val preparationBlocked = CountDownLatch(1); val releasePreparation = CountDownLatch(1)
        val firstStarted = CountDownLatch(1); val releaseFirst = CountDownLatch(1)
        val preparations = java.util.concurrent.atomic.AtomicInteger()
        val preparingContext = object : android.content.ContextWrapper(context) {
            override fun getPackageManager(): android.content.pm.PackageManager {
                if (Thread.currentThread().stackTrace.any { it.className == InstallCoordinator::class.java.name && it.methodName == "prepare" } &&
                    preparations.incrementAndGet() == 3) {
                    preparationBlocked.countDown()
                    check(releasePreparation.await(60, TimeUnit.SECONDS))
                }
                return super.getPackageManager()
            }
        }
        val executor = Executors.newSingleThreadExecutor()
        var advance: Future<*>? = null
        try {
            InstallerApplication.installPipeline.execute { pipelineBlocked.countDown(); releasePipeline.await(60, TimeUnit.SECONDS) }
            assertTrue(pipelineBlocked.await(5, TimeUnit.SECONDS))
            ReinstallQueue.begin(context)
            val first = requireNotNull(ReinstallQueue.next(context))
            val keys = ReinstallQueue.pendingKeys(context)
            val firstApp = apps.single { it.s("key") == keys.first() }
            InstallCoordinator.authorizeBackground(context, first); ReinstallQueue.allowBackground(context)
            instrumentation.runOnMainSync {
                assertTrue(BuildJobs.start(context, "install:$first", firstApp.s("key"), firstApp.name()) { report, _ ->
                    report(io.github.ulysseszh.pwainstaller.core.ApkProgress(io.github.ulysseszh.pwainstaller.core.ApkProgress.Stage.FILTERS, 10))
                    firstStarted.countDown(); check(releaseFirst.await(60, TimeUnit.SECONDS)); null
                })
            }
            assertTrue(firstStarted.await(5, TimeUnit.SECONDS))
            advance = executor.submit { InstallPipeline.advance(preparingContext) { } }
            assertTrue("The last request in the batch must be held during preparation", preparationBlocked.await(10, TimeUnit.SECONDS))
            awaitNative("Prepared builders must start while the first build and a later request are still blocked", timeout = 8000) {
                keys.take(3).all { key -> BuildJobs.all().values.any { it.key == key && !it.queued } }
            }
            assertFalse(BuildJobs.get("install:$first")!!.done)
            if (Build.VERSION.SDK_INT >= 23) awaitNative("Each started app must already have its own progress notification") {
                val titles = (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).activeNotifications
                    .map { it.notification.extras.getCharSequence("android.title")?.toString() }
                apps.filter { it.s("key") in keys.take(3) }.all { app -> titles.count { it == app.name() } == 1 }
            }
        } finally {
            releasePreparation.countDown()
            advance?.get(30, TimeUnit.SECONDS)
            context.getSharedPreferences("reinstall-all", 0).edit().clear().commit()
            releaseFirst.countDown()
            awaitNative("The startup batch must finish before cleanup", timeout = 30000) {
                BuildJobs.all().values.filter { state -> apps.any { it.s("key") == state.key } }.all { it.done }
            }
            InstallCoordinator.tokens(context).filter { token -> apps.any { it.s("key") == InstallCoordinator.summary(context, token).getJSONObject("app").getString("key") } }
                .forEach { InstallCoordinator.discard(context, it, true) }
            BuildJobs.all().filterValues { state -> apps.any { it.s("key") == state.key } }.keys.forEach(BuildJobs::remove)
            releasePipeline.countDown(); InstallerApplication.installPipeline.submit { }.get(10, TimeUnit.SECONDS)
            executor.shutdownNow(); instrumentation.runOnMainSync { screen.finish() }
            AppDatabase(context).use { db -> apps.forEach(db::remove) }
            BuildSettings.save(context, previousBuilders)
            if (Build.VERSION.SDK_INT >= 26) awaitNative("The startup batch must leave no progress notifications") {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).activeNotifications.none { it.notification.channelId == ProgressNotifications.BUILDS }
            }
        }
    }

    @Test fun reinstallAllBuildsAheadWhileOnlyOneInstallerConfirmationIsPending() {
        Assume.assumeTrue(InstallCoordinator.tokens(context).isEmpty() && !ReinstallQueue.has(context))
        if (Build.VERSION.SDK_INT >= 26) shell("appops set ${context.packageName} REQUEST_INSTALL_PACKAGES allow")
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        val previousBuilders = BuildSettings.builders(context)
        BuildSettings.save(context, 8)
        val apps = (1..9).map { number -> WebApp().apply {
            set("baseUrl", fixture.url("/batch-$number/")); manifest().put("name", "Batch build $number"); validate()
        } }
        AppDatabase(context).use { db -> apps.forEach(db::save) }
        val main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        try {
            awaitUi("Library must load before reinstalling all") { main.toolbar.menu.findItem(6).isEnabled }
            if (Build.VERSION.SDK_INT >= 25) {
                val shortcut = context.getSystemService(ShortcutManager::class.java).manifestShortcuts.single { it.id == "reinstall_all" }
                context.startActivity(shortcut.intent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            } else instrumentation.runOnMainSync { main.toolbar.menu.performIdentifierAction(6, 0) }
            awaitNative("Eight APKs must be built before the first confirmation is answered", timeout = 60000) {
                val states = InstallCoordinator.tokens(context).map { InstallCoordinator.read(context, it).optString("state") }
                states.count { it == "confirmation" } == 1 && states.count { it == "ready" } == 7
            }
            val plans = InstallCoordinator.tokens(context).map { InstallCoordinator.read(context, it) }
            assertEquals(1, plans.count { it.getString("state") in listOf("committed", "confirmation") })
            assertEquals(8, plans.size)
            assertEquals(9, ReinstallQueue.pendingKeys(context).size)
            if (Build.VERSION.SDK_INT >= 23) awaitNative("Each of the eight operations must have exactly one progress notification") {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val titles = manager.activeNotifications.map { it.notification.extras.getCharSequence("android.title")?.toString() }
                plans.all { plan -> titles.count { it == WebApp.name(plan.getJSONObject("app")) } == 1 } &&
                    titles.count { title -> apps.any { it.name() == title } } == 8
            }
            plans.forEach { plan ->
                assertTrue(File(plan.getString("apk")).isFile)
                assertTrue(BuildLogs.file(context, plan.getJSONObject("app").getString("key")).readText().contains("Signed APK:"))
            }
        } catch (failure: Throwable) {
            instrumentation.sendStatus(0, Bundle().apply { putString("batch_failure", failure.toString() + " states=" + InstallCoordinator.tokens(context).map { InstallCoordinator.summary(context, it).optString("state") }) })
            throw failure
        } finally {
            context.getSharedPreferences("reinstall-all", 0).edit().clear().commit()
            instrumentation.runOnMainSync { main.finish() }
            awaitUi("Batch activity must close before installation cleanup") { main.isDestroyed }
            InstallerApplication.worker.submit { }.get(10, TimeUnit.SECONDS)
            InstallerApplication.installPipeline.submit { }.get(15, TimeUnit.SECONDS)
            InstallCoordinator.tokens(context).forEach { token ->
                val plan = InstallCoordinator.read(context, token)
                if (plan.has("session")) runCatching { context.packageManager.packageInstaller.abandonSession(plan.getInt("session")) }
                InstallCoordinator.discard(context, token, true)
            }
            BuildJobs.all().filterValues { state -> apps.any { it.s("key") == state.key } }.keys.forEach(BuildJobs::remove)
            shell("input keyevent KEYCODE_BACK")
            AppDatabase(context).use { db -> apps.forEach(db::remove) }
            BuildSettings.save(context, previousBuilders)
            if (Build.VERSION.SDK_INT >= 26) awaitNative("Batch cleanup must remove its progress notifications") {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).activeNotifications.none { it.notification.channelId == ProgressNotifications.BUILDS }
            }
        }
    }

    @Test fun reinstallAllKeepsASequentialQueueAndResumesAfterAnInterruption() {
        val apps = listOf("queuefirst", "queuesecond").map { suffix -> WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", "io.github.ulysseszh.pwainstaller.integration$suffix"); validate()
        } }
        val tokens = mutableListOf<String>()
        try {
            AppDatabase(context).use { db -> apps.forEach(db::save) }
            ReinstallQueue.begin(context)
            val first = ReinstallQueue.next(context)!!; tokens.add(first)
            assertEquals(first, ReinstallQueue.next(context))
            ReinstallQueue.pause(context, first)
            assertNull(ReinstallQueue.next(context))
            ReinstallQueue.resume(context)
            assertEquals(first, ReinstallQueue.next(context))
            val firstApp = WebApp(InstallCoordinator.read(context, first).getJSONObject("app"))
            val apk = ApkBuilder.build(context, firstApp); install(apk); apk.delete()
            InstallCoordinator.installed(context, first); InstallCoordinator.complete(context, first)
            ReinstallQueue.complete(context, first); InstallCoordinator.discard(context, first)
            val second = ReinstallQueue.next(context)!!; tokens.add(second)
            assertNotEquals(first, second)
            val secondApp = WebApp(InstallCoordinator.read(context, second).getJSONObject("app"))
            assertNotEquals(firstApp.s("key"), secondApp.s("key"))
            val next = ApkBuilder.build(context, secondApp); install(next); next.delete()
            InstallCoordinator.installed(context, second); InstallCoordinator.complete(context, second)
            ReinstallQueue.complete(context, second); InstallCoordinator.discard(context, second)
            assertFalse(ReinstallQueue.has(context)); assertNull(ReinstallQueue.next(context))
        } finally {
            context.getSharedPreferences("reinstall-all", 0).edit().clear().commit()
            tokens.filter { it in InstallCoordinator.tokens(context) }.forEach { InstallCoordinator.discard(context, it, true) }
            AppDatabase(context).use { db -> apps.forEach(db::remove) }
            apps.forEach { shell("pm uninstall ${it.s("packageName")}") }
        }
    }

    private fun editorInput(editor: EditorActivity, property: String): TextView =
        EditorActivity::class.java.getDeclaredField(property).apply { isAccessible = true }.get(editor) as TextView

    private fun awaitUi(message: String, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 15000
        var passed = false
        while (!passed && System.currentTimeMillis() < deadline) {
            instrumentation.runOnMainSync { passed = predicate() }
            if (!passed) Thread.sleep(50)
        }
        assertTrue(message, passed)
    }

    @Test fun editorBackConfirmsDiscardAndKeepEditingPreservesUnsavedText() {
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("adblockRules", "")
            manifest().put("name", "Saved title"); validate()
        }
        AppDatabase(context).use { it.save(app) }
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("id", app.s("key"))) as EditorActivity
        awaitEditor(editor)
        try {
            instrumentation.runOnMainSync {
                val labels = views(editor.root).filterIsInstance<com.google.android.material.textfield.TextInputLayout>().map { it.hint.toString() }
                assertFalse(labels.contains(context.getString(R.string.base_url)))
                assertFalse(views(editor.root).filterIsInstance<TextView>().any { it.text.toString() == context.getString(R.string.advanced) })
                assertTrue(labels.contains(context.getString(R.string.start_url)))
                val namePosition = labels.indexOf(context.getString(R.string.name))
                val startPosition = labels.indexOf(context.getString(R.string.start_url))
                val packagePosition = labels.indexOf(context.getString(R.string.package_name))
                assertTrue("Name and start URL must precede package name", namePosition >= 0 && startPosition > namePosition && packagePosition > startPosition)
                val controls = views(editor.root).filterIsInstance<TextView>()
                val fetchPosition = controls.indexOfFirst { it.text.toString() == context.getString(R.string.fetch_manifest) }
                val jsonPosition = controls.indexOfFirst { it.text.toString() == context.getString(R.string.view_json) }
                assertEquals("JSON editor button must immediately follow Fetch manifest", fetchPosition + 1, jsonPosition)
                // Malformed JSON is still an unsaved change and must not bypass the dialog.
                editorInput(editor, "json").text = "{invalid unsaved JSON"
                views(editor.toolbar).first { it.contentDescription?.toString() == context.getString(io.github.ulysseszh.pwainstaller.browser.R.string.back) }.performClick()
            }
            clickText(context.getString(R.string.keep_editing))
            awaitUi("Keep editing must return focus to the editor") { editor.hasWindowFocus() }
            instrumentation.runOnMainSync {
                assertFalse(editor.isFinishing)
                assertEquals("{invalid unsaved JSON", editorInput(editor, "json").text.toString())
            }
            instrumentation.runOnMainSync { editor.onBackPressedDispatcher.onBackPressed() }
            clickText(context.getString(R.string.discard_changes))
            awaitUi("Discard must close the editor") { editor.isFinishing }
            assertEquals("Saved title", AppDatabase(context).use { it.get(app.s("key")) }!!.name())
            assertEquals(0, fixture.requestCount.get())
        } finally { instrumentation.runOnMainSync { editor.finish() }; AppDatabase(context).use { it.remove(app) } }
    }

    @Test fun editorBackDoesNotPromptAfterChangesAreReverted() {
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("adblockRules", ""); validate() }
        AppDatabase(context).use { it.save(app) }
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("id", app.s("key"))) as EditorActivity
        awaitEditor(editor)
        try {
            instrumentation.runOnMainSync {
                val userAgent = editorInput(editor, "userAgentInput")
                userAgent.text = "Changed user agent"; userAgent.text = ""
                val notch = EditorActivity::class.java.getDeclaredField("notchAreas").apply { isAccessible = true }.get(editor) as com.google.android.material.checkbox.MaterialCheckBox
                notch.isChecked = !notch.isChecked; notch.isChecked = !notch.isChecked
                val colors = EditorActivity::class.java.getDeclaredField("colors").apply { isAccessible = true }.get(editor) as Map<*, *>
                val theme = colors["theme_color"] as ColorButton
                val previous = theme.value; theme.value = "#123456"; theme.value = previous
                editor.onBackPressedDispatcher.onBackPressed()
                assertTrue("Reverted changes should leave immediately", editor.isFinishing)
            }
        } finally { instrumentation.runOnMainSync { editor.finish() }; AppDatabase(context).use { it.remove(app) } }
    }

    @Test fun editorRetainsUnsavedIconChangesAndDiscardBaselineAcrossRecreation() {
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("adblockRules", ""); validate() }
        AppDatabase(context).use { it.save(app) }
        var editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("id", app.s("key"))) as EditorActivity
        awaitEditor(editor)
        val icon = IconData.entry(IconData.decode(IconData.default), "upload:unsaved")
        try {
            instrumentation.runOnMainSync {
                views(editor.root).filterIsInstance<IconEditor>().single().add(listOf(icon))
                editor.onBackPressedDispatcher.onBackPressed()
            }
            clickText(context.getString(R.string.keep_editing))
            val previous = editor
            instrumentation.runOnMainSync { editor.recreate() }
            var next: EditorActivity? = null
            awaitUi("Editor did not recreate") {
                next = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .filterIsInstance<EditorActivity>().singleOrNull { it !== previous }
                next != null
            }
            editor = requireNotNull(next); awaitEditor(editor)
            instrumentation.runOnMainSync {
                assertEquals(1, views(editor.root).filterIsInstance<TextView>().count { it.text.toString().startsWith(context.getString(R.string.icon_number, 1) + " ·") })
                editor.onBackPressedDispatcher.onBackPressed()
            }
            clickText(context.getString(R.string.discard_changes))
            awaitUi("Recreated editor must confirm and discard unsaved changes") { editor.isFinishing }
            assertEquals(0, AppDatabase(context).use { it.get(app.s("key")) }!!.icons().length())
        } finally { instrumentation.runOnMainSync { editor.finish() }; AppDatabase(context).use { it.remove(app) } }
    }

    @Test fun pendingEditorErrorWaitsForAcknowledgementAndSurvivesRecreation() = checkPendingEditorError(cancel = false)

    @Test fun pendingEditorErrorWaitsUntilTheUserCancelsIt() = checkPendingEditorError(cancel = true)

    private fun checkPendingEditorError(cancel: Boolean) {
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/pending-editor/")); set("install", false)
            manifest().put("name", "Pending editor fixture"); validate()
        }
        AppDatabase(context).use { it.save(app) }
        val token = InstallCoordinator.prepare(context, app, null)
        var editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra("id", app.s("key"))) as EditorActivity
        val message = context.getString(R.string.error_detail, context.getString(R.string.action_failed), context.getString(R.string.pending_operation))
        fun assertReadableError() {
            awaitNative("The pending-operation explanation must remain readable", timeout = 5000) {
                activeNativeNodes().any { it.text?.toString() == message }
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertFalse("The editor must wait for acknowledgement before closing", editor.isFinishing)
                assertFalse(editor.busy)
                assertFalse(editor.toolbar.menu.findItem(android.R.id.button1).isEnabled)
                assertFalse("Blocked metadata must not be editable", views(editor.root).any { it is IconEditor })
            }
        }
        try {
            assertReadableError()
            assertEquals("prepared", InstallCoordinator.summary(context, token).getString("state"))
            if (!cancel) {
                // Even if the job ends meanwhile, recreation must keep the unread
                // explanation rather than silently loading the form in its place.
                InstallCoordinator.discard(context, token)
                val previous = editor
                instrumentation.runOnMainSync { previous.recreate() }
                var next: EditorActivity? = null
                awaitUi("The blocked editor must recreate") {
                    next = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                        .filterIsInstance<EditorActivity>().singleOrNull { it !== previous }
                    next != null
                }
                editor = requireNotNull(next)
                assertReadableError()
                clickText(context.getString(android.R.string.ok))
            } else shell("input keyevent 4")
            awaitUi("Acknowledging or cancelling must close the blocked editor") { editor.isDestroyed }
            assertEquals("Pending editor fixture", AppDatabase(context).use { it.get(app.s("key"))!!.name() })
            assertEquals(0, fixture.requestCount.get())
            if (cancel) assertEquals("Cancelling the explanation must not cancel the pending job", "prepared", InstallCoordinator.summary(context, token).getString("state"))
            else {
                editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra("id", app.s("key"))) as EditorActivity
                awaitEditor(editor)
            }
        } finally {
            instrumentation.runOnMainSync { editor.finish() }
            InstallCoordinator.discard(context, token)
            AppDatabase(context).use { it.remove(app) }
        }
    }

    @Test fun addingShowsBaseUrlAndAdvancedWhileAnUnchangedFormLeavesImmediately() {
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as EditorActivity
        awaitEditor(editor)
        try {
            instrumentation.runOnMainSync {
                assertTrue(views(editor.root).filterIsInstance<com.google.android.material.textfield.TextInputLayout>().any { it.hint.toString() == context.getString(R.string.base_url) })
                assertTrue(views(editor.root).filterIsInstance<TextView>().any { it.text.toString() == context.getString(R.string.advanced) })
                editor.onBackPressedDispatcher.onBackPressed()
                assertTrue("Unchanged creation form must close without a prompt", editor.isFinishing)
            }
        } finally { instrumentation.runOnMainSync { editor.finish() } }
    }

    @Test fun editingConfirmsManifestReplacementAndCancelKeepsUnsavedMetadata() {
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("adblockRules", ""); manifest().put("name", "Saved title")
            set("urlPatterns", fixture.url("/custom/") + "\n" + fixture.url("/another/")); validate()
        }
        AppDatabase(context).use { it.save(app) }
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("id", app.s("key"))) as EditorActivity
        awaitEditor(editor)
        fun clickFetch() = instrumentation.runOnMainSync {
            views(editor.root).filterIsInstance<TextView>().single { it.text.toString() == context.getString(R.string.fetch_manifest) }.performClick()
        }
        try {
            instrumentation.runOnMainSync {
                assertFalse(views(editor.root).filterIsInstance<com.google.android.material.textfield.TextInputLayout>().any { it.hint.toString() == context.getString(R.string.scope) })
                @Suppress("UNCHECKED_CAST")
                val fields = EditorActivity::class.java.getDeclaredField("fields").apply { isAccessible = true }.get(editor) as Map<String, com.google.android.material.textfield.TextInputEditText>
                fields.getValue("name").setText("Unsaved title")
            }
            clickFetch()
            clickText(context.getString(android.R.string.cancel))
            assertEquals(0, fixture.requestCount.get())
            instrumentation.runOnMainSync {
                val read = EditorActivity::class.java.getDeclaredMethod("read").apply { isAccessible = true }
                assertEquals("Unsaved title", (read.invoke(editor) as WebApp).name())
                assertEquals(app.s("urlPatterns"), (read.invoke(editor) as WebApp).s("urlPatterns"))
            }
            clickFetch(); clickText(context.getString(R.string.fetch_manifest))
            val deadline = System.currentTimeMillis() + 15000
            var updated = false
            while (!updated && System.currentTimeMillis() < deadline) {
                instrumentation.runOnMainSync {
                    updated = !editor.busy && views(editor.root).filterIsInstance<TextView>().any { it.text.toString() == "Device fixture" }
                }
                if (!updated) Thread.sleep(50)
            }
            assertTrue("Confirmed fetching did not populate the manifest", updated)
            assertTrue(fixture.requestCount.get() > 0)
            instrumentation.runOnMainSync {
                val labels = views(editor.root).filterIsInstance<TextView>().map { it.text.toString() }
                assertTrue(labels.any { it.contains(context.getString(R.string.scalable_icon)) })
                assertFalse(labels.any { it.startsWith(context.getString(R.string.icon_number, 1)) && it.contains("512x512") })
                assertEquals(fixture.url("/"), editorInput(editor, "urlPatternInput").text.toString())
            }
        } finally { instrumentation.runOnMainSync { editor.finish() }; AppDatabase(context).use { it.remove(app) } }
    }

    @Test fun backgroundSaveBuildsBeforeInstallPermissionAndCancelLeavesAnExplicitRetry() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 26 && InstallCoordinator.tokens(context).isEmpty())
        // Set the app-op before starting instrumentation: changing it kills the app.
        Assume.assumeFalse(context.packageManager.canRequestPackageInstalls())
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/save-permission/")); manifest().put("name", "Save permission fixture"); validate()
        }
        val screen = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
        var id: String? = null
        var token: String? = null
        var main: MainActivity? = null
        try {
            instrumentation.runOnMainSync { assertTrue(SaveJobs.start(context, app)) }
            id = BuildJobs.all().entries.single { it.value.key == app.s("key") }.key
            token = id.removePrefix("install:")
            awaitNative("An APK can build before permission to install is granted", timeout = 30000) { BuildJobs.get(id)?.done == true }
            assertNull(BuildJobs.get(id)?.error)
            assertEquals("ready", InstallCoordinator.summary(context, token).getString("state"))
            assertFalse(InstallCoordinator.summary(context, token).optBoolean("background"))
            val log = BuildLogs.file(context, app.s("key")).readText()
            assertTrue(log.contains("Signed APK:"))
            instrumentation.runOnMainSync { screen.startActivity(Intent(screen, MainActivity::class.java)) }
            awaitUi("The app list must resume") {
                main = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).filterIsInstance<MainActivity>().firstOrNull()
                main != null
            }
            awaitNative("The list must request installation permission for the completed editor save") {
                activeNativeNodes().any { it.text?.toString() == context.getString(R.string.install_permission) }
            }
            clickText(context.getString(android.R.string.cancel), context.packageName)
            awaitUi("Cancel must expose Continue without repeatedly showing the permission prompt") {
                views(main!!.root).filterIsInstance<com.google.android.material.floatingactionbutton.FloatingActionButton>()
                    .any { it.isShown && it.contentDescription.toString() == context.getString(R.string.continue_action) }
            }
            assertTrue(InstallCoordinator.summary(context, token).getBoolean("interrupted"))
            context.sendBroadcast(Intent(InstallReceiver.CHANGED).setPackage(context.packageName))
            instrumentation.waitForIdleSync()
            assertFalse(activeNativeNodes().any { it.text?.toString() == context.getString(R.string.install_permission) })
            assertEquals("Permission handling must not replace the save/build log", log, BuildLogs.file(context, app.s("key")).readText())
        } finally {
            instrumentation.runOnMainSync { main?.finish(); screen.finish() }
            id?.let(BuildJobs::remove); token?.let { InstallCoordinator.discard(context, it, true) }
            AppDatabase(context).use { it.remove(app) }
        }
    }

    @Test fun backgroundSaveKeepsTheSameProgressNotificationThroughRequestPreparationAndBuilding() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 26 && InstallCoordinator.tokens(context).isEmpty())
        shell("appops set ${context.packageName} REQUEST_INSTALL_PACKAGES allow")
        if (Build.VERSION.SDK_INT >= 33) shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/save-notification/")); manifest().put("name", "Save progress fixture"); validate()
        }
        val saving = CountDownLatch(1); val releaseSaving = CountDownLatch(1)
        val compiling = CountDownLatch(1); val releaseCompiling = CountDownLatch(1)
        val pipeline = CountDownLatch(1); val releasePipeline = CountDownLatch(1)
        val heldSave = java.util.concurrent.atomic.AtomicBoolean()
        val heldBuild = java.util.concurrent.atomic.AtomicBoolean()
        val gated = object : android.content.ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
            override fun getPackageManager(): android.content.pm.PackageManager {
                val stack = Thread.currentThread().stackTrace
                if (stack.any { it.className == InstallCoordinator::class.java.name && it.methodName == "prepare" } && heldSave.compareAndSet(false, true)) {
                    saving.countDown(); check(releaseSaving.await(60, TimeUnit.SECONDS))
                } else if (stack.any { it.className == ApkBuilder::class.java.name && it.methodName == "buildLogged" } && heldBuild.compareAndSet(false, true)) {
                    compiling.countDown(); check(releaseCompiling.await(60, TimeUnit.SECONDS))
                }
                return super.getPackageManager()
            }
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        fun notices() = manager.activeNotifications.filter { it.notification.extras.getCharSequence("android.title")?.toString() == app.name() }
        val screen = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
        var token: String? = null
        var id: String? = null
        try {
            InstallerApplication.installPipeline.execute { pipeline.countDown(); releasePipeline.await(60, TimeUnit.SECONDS) }
            assertTrue(pipeline.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { assertTrue(SaveJobs.start(gated, app)) }
            assertTrue("Request preparation must run in the save job", saving.await(15, TimeUnit.SECONDS))
            id = BuildJobs.all().entries.single { it.value.key == app.s("key") }.key
            token = id.removePrefix("install:")
            assertEquals(io.github.ulysseszh.pwainstaller.core.ApkProgress.Stage.SAVING, BuildJobs.get(id)!!.progress.stage)
            assertEquals(3, BuildJobs.get(id)!!.progress.overallPercent)
            awaitNative("Saving must already have its own progress notification") {
                notices().let { it.size == 1 && it.single().notification.extras.getCharSequence("android.text").toString().startsWith(context.getString(R.string.build_saving)) }
            }
            val notificationId = notices().single().id
            releaseSaving.countDown()
            assertTrue("Compilation must use that same job", compiling.await(15, TimeUnit.SECONDS))
            assertTrue(InstallCoordinator.summary(context, token).getBoolean("background"))
            awaitNative("Compilation must keep the saving notification identity") {
                notices().let { it.size == 1 && it.single().id == notificationId &&
                    it.single().notification.extras.getCharSequence("android.text").toString().startsWith(context.getString(R.string.build_filters)) }
            }
            releaseCompiling.countDown()
            awaitNative("Saved app APK must finish building", timeout = 30000) { BuildJobs.get(id)?.done == true }
            assertNull(BuildJobs.get(id)?.error)
            assertEquals("ready", InstallCoordinator.summary(context, token).getString("state"))
            BuildJobs.remove(id)
            awaitNative("Ready installation must keep the same notification") {
                notices().let { it.size == 1 && it.single().id == notificationId && it.single().notification.extras.getCharSequence("android.text").toString() == context.getString(R.string.build_ready) }
            }
            InstallCoordinator.write(context, token, InstallCoordinator.summary(context, token).put("state", "committed"))
            awaitNative("Installation must keep the saving/building notification") {
                notices().let { it.size == 1 && it.single().id == notificationId && it.single().notification.extras.getCharSequence("android.text").toString() == context.getString(R.string.build_waiting) }
            }
            InstallCoordinator.installed(context, token); InstallCoordinator.complete(context, token)
            releasePipeline.countDown()
            awaitNative("Completion must clear the save/build/install notification") { notices().isEmpty() }
            val log = BuildLogs.file(context, app.s("key")).readText()
            assertTrue(log.indexOf("Saving web app metadata") < log.indexOf("Preparing installation request"))
            assertTrue(log.contains("Signed APK:"))
        } finally {
            releaseSaving.countDown(); releaseCompiling.countDown()
            awaitNative("The save builder must finish before cleanup", timeout = 30000) { BuildJobs.forApp(app.s("key")) == null }
            id?.let(BuildJobs::remove)
            token?.let { InstallCoordinator.discard(context, it, true) }
            AppDatabase(context).use { it.remove(app) }
            releasePipeline.countDown(); InstallerApplication.installPipeline.submit { }.get(10, TimeUnit.SECONDS)
            instrumentation.runOnMainSync { screen.finish() }
        }
    }

    @Test fun editorReturnsToTheLibraryWhileExistingAndNewAppsWaitForBackgroundSaving() {
        val previousBuilders = BuildSettings.builders(context)
        val previousLayout = AppearanceSettings.layout(context)
        BuildSettings.save(context, 1); AppearanceSettings.save(context, "list")
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/queued-save/")); set("install", false)
            manifest().put("name", "Before background save"); validate()
        }
        AppDatabase(context).use { it.save(app) }
        val started = CountDownLatch(1); val release = CountDownLatch(1)
        val blocker = "save-blocker:${java.util.UUID.randomUUID()}"
        val editors = mutableListOf<EditorActivity>()
        var newKey: String? = null
        var main: MainActivity? = null
        try {
            val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("id", app.s("key"))) as EditorActivity
            editors.add(editor); awaitEditor(editor)
            instrumentation.runOnMainSync {
                assertTrue(BuildJobs.start(context, blocker, blocker, "Held builder") { _, _ ->
                    started.countDown(); check(release.await(60, TimeUnit.SECONDS)); null
                })
            }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                @Suppress("UNCHECKED_CAST")
                val fields = EditorActivity::class.java.getDeclaredField("fields").apply { isAccessible = true }.get(editor) as Map<String, TextView>
                fields.getValue("name").text = "After background save"
                editor.toolbar.menu.performIdentifierAction(android.R.id.button1, 0)
            }
            awaitUi("The editor must close while its save job is still queued") { editor.isFinishing }
            assertTrue(BuildJobs.forApp(app.s("key"))!!.queued)
            assertEquals("Before background save", AppDatabase(context).use { it.get(app.s("key"))!!.name() })
            awaitUi("The list must remain interactive and show queued saving") {
                main = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).filterIsInstance<MainActivity>().firstOrNull()
                main?.let { screen -> !screen.busy && views(screen.root).filterIsInstance<TextView>()
                    .any { it.text.toString() == context.getString(R.string.build_queued) } } == true
            }
            assertEquals("Editing must not fetch web content", 0, fixture.requestCount.get())

            instrumentation.runOnMainSync {
                views(main!!.root).filterIsInstance<com.google.android.material.floatingactionbutton.FloatingActionButton>()
                    .single { it.contentDescription.toString() == context.getString(R.string.add_app) }.performClick()
            }
            var opened: EditorActivity? = null
            awaitUi("Adding another app must remain available while saves are queued") {
                opened = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).filterIsInstance<EditorActivity>().firstOrNull()
                opened != null
            }
            val adding = requireNotNull(opened)
            editors.add(adding); awaitEditor(adding)
            instrumentation.runOnMainSync {
                editorInput(adding, "base").text = fixture.url("/")
                (EditorActivity::class.java.getDeclaredField("install").apply { isAccessible = true }.get(adding) as com.google.android.material.checkbox.MaterialCheckBox).isChecked = false
                EditorActivity::class.java.getDeclaredField("rulesCustomized").apply { isAccessible = true }.setBoolean(adding, true)
                newKey = (EditorActivity::class.java.getDeclaredField("app").apply { isAccessible = true }.get(adding) as WebApp).s("key")
                adding.toolbar.menu.performIdentifierAction(android.R.id.button1, 0)
            }
            awaitUi("Creation must return after fetching, before queued persistence starts") { adding.isFinishing }
            assertTrue(BuildJobs.forApp(newKey!!)!!.queued)
            assertNull(AppDatabase(context).use { it.get(newKey!!) })
            awaitUi("A new app must appear in the list before its metadata is saved") {
                main!!.hasWindowFocus() && views(main!!.root).filterIsInstance<TextView>().any { it.text.toString() == "Device fixture" } &&
                    views(main!!.root).filterIsInstance<TextView>().count { it.text.toString() == context.getString(R.string.build_queued) } == 2
            }
            release.countDown()
            awaitNative("Both queued metadata saves must finish", timeout = 30000) {
                AppDatabase(context).use { it.get(app.s("key"))?.name() == "After background save" && it.get(newKey!!)?.name() == "Device fixture" } &&
                    BuildJobs.forApp(app.s("key")) == null && BuildJobs.forApp(newKey!!) == null
            }
            awaitUi("Completed saves must leave no progress on the list") {
                views(main!!.root).filterIsInstance<com.google.android.material.progressindicator.LinearProgressIndicator>().none { it.isShown }
            }
            val log = BuildLogs.file(context, app.s("key")).readText()
            assertTrue(log.contains("VALIDATING")); assertTrue(log.contains("SAVING"))
        } finally {
            release.countDown()
            awaitNative("Save jobs must finish before cleanup", timeout = 30000) {
                BuildJobs.get(blocker)?.done != false && BuildJobs.forApp(app.s("key")) == null && newKey?.let { BuildJobs.forApp(it) == null } != false
            }
            instrumentation.runOnMainSync { editors.forEach { it.finish() }; main?.finish() }
            BuildJobs.all().filterValues { it.key in listOf(blocker, app.s("key"), newKey) }.keys.forEach(BuildJobs::remove)
            AppDatabase(context).use { db -> db.remove(app); newKey?.let { db.get(it)?.let(db::remove) } }
            BuildSettings.save(context, previousBuilders); AppearanceSettings.save(context, previousLayout)
        }
    }

    @Test fun failedBackgroundSaveRestoresTheSubmittedValuesForCorrection() {
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/failed-save/")); set("install", false)
            manifest().put("name", "Saved name"); validate()
        }
        AppDatabase(context).use { it.save(app) }
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("id", app.s("key"))) as EditorActivity
        var restored: EditorActivity? = null
        try {
            awaitEditor(editor)
            instrumentation.runOnMainSync {
                @Suppress("UNCHECKED_CAST")
                val fields = EditorActivity::class.java.getDeclaredField("fields").apply { isAccessible = true }.get(editor) as Map<String, TextView>
                fields.getValue("name").text = "Keep my changed name"
                editorInput(editor, "headers").text = "Invalid header without a colon"
                editor.toolbar.menu.performIdentifierAction(android.R.id.button1, 0)
            }
            awaitUi("Validation must run after returning to the list") { editor.isFinishing }
            awaitNative("A failed save must offer to edit the submitted draft") {
                activeNativeNodes().any { it.text?.toString() == context.getString(R.string.edit_app) && it.isClickable }
            }
            assertEquals("Saved name", AppDatabase(context).use { it.get(app.s("key"))!!.name() })
            clickText(context.getString(R.string.edit_app), context.packageName)
            awaitUi("Correcting a save must reopen the editor") {
                restored = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).filterIsInstance<EditorActivity>().firstOrNull { it !== editor }
                restored != null && !restored!!.busy && views(restored!!.root).any { it is IconEditor }
            }
            instrumentation.runOnMainSync {
                val fields = EditorActivity::class.java.getDeclaredField("fields").apply { isAccessible = true }.get(restored) as Map<*, *>
                assertEquals("Keep my changed name", (fields["name"] as TextView).text.toString())
                assertEquals("Invalid header without a colon", editorInput(restored!!, "headers").text.toString())
                editorInput(restored!!, "headers").text = ""
                restored!!.toolbar.menu.performIdentifierAction(android.R.id.button1, 0)
            }
            awaitNative("The corrected draft must save", timeout = 15000) {
                AppDatabase(context).use { it.get(app.s("key"))?.name() == "Keep my changed name" } && BuildJobs.forApp(app.s("key")) == null
            }
            assertFalse(SaveJobs.recoveryFile(context, app.s("key"))!!.exists())
            assertEquals(0, fixture.requestCount.get())
        } finally {
            instrumentation.runOnMainSync {
                editor.finish(); restored?.finish()
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .filterIsInstance<MainActivity>().forEach { it.finish() }
            }
            AppDatabase(context).use { it.remove(app) }; SaveJobs.recoveryFile(context, app.s("key"))?.delete()
        }
    }

    @Test fun editingSavesLocallyWithoutFetchingPagesIconsShortcutsOrRules() {
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/offline-before/")); set("install", false)
            set("adblockRules", "127.0.0.1##.saved-rule"); validate()
        }
        AppDatabase(context).use { it.save(app) }
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("id", app.s("key"))) as EditorActivity
        awaitEditor(editor)
        try {
            instrumentation.runOnMainSync {
                val json = EditorActivity::class.java.getDeclaredField("json").apply { isAccessible = true }.get(editor) as TextView
                json.text = app.manifest().toString().let(::JSONObject).put("name", "Offline edit")
                    .put("start_url", fixture.url("/offline-after/"))
                    .put("scope", fixture.url("/"))
                    .put("icons", JSONArray().put(JSONObject().put("src", fixture.url("/edited-icon.svg"))))
                    .put("shortcuts", JSONArray().put(JSONObject().put("name", "Offline shortcut").put("url", fixture.url("/shortcut"))
                        .put("icons", JSONArray().put(JSONObject().put("src", fixture.url("/shortcut-icon.svg")))))).toString()
                views(editor.root).filterIsInstance<TextView>().single { it.text.toString() == context.getString(R.string.apply_json) }.performClick()
                editor.toolbar.menu.performIdentifierAction(android.R.id.button1, 0)
            }
            val deadline = System.currentTimeMillis() + 15000
            var saved: WebApp? = null
            while (System.currentTimeMillis() < deadline) {
                saved = AppDatabase(context).use { it.get(app.s("key")) }
                if (saved?.name() == "Offline edit") break
                Thread.sleep(50)
            }
            assertEquals("Offline edit", saved!!.name())
            assertEquals(app.s("baseUrl"), saved.s("baseUrl"))
            assertEquals(fixture.url("/offline-after/"), saved.manifest().getString("start_url"))
            assertEquals(fixture.url("/"), saved.s("urlPatterns"))
            assertEquals("127.0.0.1##.saved-rule", saved.s("adblockRules"))
            assertEquals(fixture.url("/edited-icon.svg"), saved.manifest().getJSONArray("icons").getJSONObject(0).getString("src"))
            assertEquals(0, fixture.requestCount.get())
        } finally {
            instrumentation.runOnMainSync {
                editor.finish()
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                    .filterIsInstance<MainActivity>().forEach { it.finish() }
            }
            AppDatabase(context).use { it.remove(app) }
        }
    }

    @Test fun fullscreenCanvasUsesTheNavigationAreaAndRestoresThePreviousWindowMode() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 30)
        val name = "io.github.ulysseszh.pwainstaller.integrationfullscreen"
        val immersiveConfirmation = shell("settings get secure immersive_mode_confirmations").trim()
        shell("settings put secure immersive_mode_confirmations confirmed")
        val app = WebApp().apply {
            set("baseUrl", fixture.url("/")); set("packageName", name)
            manifest().put("start_url", fixture.url("/fullscreen-canvas")); validate()
        }
        fun fullSize(state: JSONObject) = kotlin.math.abs(state.getDouble("width") - state.getDouble("screenWidth")) <= 2 &&
            kotlin.math.abs(state.getDouble("height") - state.getDouble("screenHeight")) <= 2
        fun awaitState(predicate: (JSONObject) -> Boolean): JSONObject {
            val deadline = System.currentTimeMillis() + 15000
            while (System.currentTimeMillis() < deadline) {
                fixture.fullscreenStates.lastOrNull()?.takeIf(predicate)?.let { return it }
                Thread.sleep(100)
            }
            throw AssertionError("Unexpected fullscreen viewport: ${fixture.fullscreenStates.lastOrNull()}")
        }
        fun awaitBars(visible: Boolean) {
            if (Build.VERSION.SDK_INT < 35) return
            val deadline = System.currentTimeMillis() + 10000
            while (System.currentTimeMillis() < deadline) {
                val state = shell("dumpsys window displays")
                if (listOf("statusBars", "navigationBars").all { type ->
                    Regex("InsetsSource .*type=$type .*visible=$visible(?: |$)").containsMatchIn(state)
                }) return
                Thread.sleep(100)
            }
            fail("Status and navigation bars did not become visible=$visible")
        }
        fun tapFullscreen(width: Double) {
            var x = 0; var y = 0
            awaitNative("The fullscreen button must have current document bounds") {
                val point = fixture.fullscreenStates.lastOrNull() ?: return@awaitNative false
                if (kotlin.math.abs(point.getDouble("width") - width) > 2) return@awaitNative false
                val view = activeNativeNodes().firstOrNull { it.packageName?.toString() == name && it.className?.toString() == "android.webkit.WebView" }
                    ?: return@awaitNative false
                val bounds = android.graphics.Rect().also(view::getBoundsInScreen)
                if (bounds.width() <= 0 || bounds.height() <= 0) return@awaitNative false
                val scale = bounds.width() / point.getDouble("width")
                x = (bounds.left + point.getDouble("buttonX") * scale).toInt()
                y = (bounds.top + point.getDouble("buttonY") * scale).toInt()
                bounds.contains(x, y)
            }
            // A real input event supplies fullscreen's transient user activation;
            // accepting an accessibility ACTION_CLICK does not guarantee it on newer providers.
            shell("input tap $x $y")
        }
        try {
            var previousToken: String? = null
            listOf("standalone" to "portrait-primary", "standalone" to "landscape-primary", "minimal-ui" to "portrait-primary", "browser" to "portrait-primary", "fullscreen" to "portrait-primary", "fullscreen" to "landscape-primary").forEach { (mode, orientation) ->
                fixture.fullscreenStates.clear(); fixture.clickPoints.clear()
                app.manifest().put("display", mode).put("orientation", orientation)
                val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
                shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
                val before = awaitState { it.optString("token") != previousToken && !it.getBoolean("fullscreen") &&
                    (it.getDouble("screenWidth") > it.getDouble("screenHeight")) == orientation.startsWith("landscape") && fullSize(it) == (mode == "fullscreen") }
                previousToken = before.getString("token")
                awaitBars(mode != "fullscreen")
                tapFullscreen(before.getDouble("width"))
                val during = awaitState { it.getBoolean("fullscreen") && fullSize(it) }
                awaitBars(false)
                assertTrue(during.toString(), kotlin.math.abs(during.getDouble("canvasHeight") - during.getDouble("screenHeight")) <= 2)
                assertTrue(during.toString(), kotlin.math.abs(during.getDouble("canvasWidth") - during.getDouble("screenWidth")) <= 2)
                shell("input keyevent 4")
                val after = awaitState { !it.getBoolean("fullscreen") && kotlin.math.abs(it.getDouble("width") - before.getDouble("width")) <= 2 &&
                    kotlin.math.abs(it.getDouble("height") - before.getDouble("height")) <= 2 }
                assertEquals("$mode must restore its original window mode", mode == "fullscreen", fullSize(after))
                awaitBars(mode != "fullscreen")
                shell("am force-stop $name")
            }
        } finally {
            shell("pm uninstall $name")
            if (immersiveConfirmation == "confirmed") shell("settings put secure immersive_mode_confirmations confirmed")
            else shell("settings delete secure immersive_mode_confirmations")
        }
    }

    @Test fun continueFloatingButtonAppearsAboveAddAndRecoversAPendingConfirmation() {
        Assume.assumeTrue(InstallCoordinator.tokens(context).isEmpty() && !ReinstallQueue.has(context))
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); manifest().put("name", "Pending confirmation fixture"); validate() }
        val main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        var token: String? = null
        fun refresh() { context.sendBroadcast(Intent(InstallReceiver.CHANGED).setPackage(context.packageName)) }
        fun continueButton() = views(main.root).filterIsInstance<com.google.android.material.floatingactionbutton.FloatingActionButton>()
            .single { it.contentDescription == context.getString(R.string.continue_action) }
        fun awaitVisibility(expected: Boolean) {
            val deadline = System.currentTimeMillis() + 10000
            var visible = !expected
            while (visible != expected && System.currentTimeMillis() < deadline) {
                instrumentation.runOnMainSync { visible = continueButton().isShown }
                if (visible != expected) Thread.sleep(50)
            }
            assertEquals("Continue visibility", expected, visible)
        }
        try {
            instrumentation.runOnMainSync {
                assertTrue((0 until main.toolbar.menu.size()).none { main.toolbar.menu.getItem(it).title == context.getString(R.string.continue_action) })
                assertFalse(continueButton().isShown)
            }
            token = InstallCoordinator.prepare(context, app, null)
            InstallCoordinator.write(context, token, InstallCoordinator.read(context, token).put("state", "confirmation"))
            refresh(); awaitVisibility(true)
            instrumentation.runOnMainSync {
                val resume = continueButton()
                val add = views(main.root).filterIsInstance<com.google.android.material.floatingactionbutton.FloatingActionButton>()
                    .single { it.contentDescription == context.getString(R.string.add_app) }
                val resumePosition = IntArray(2); val addPosition = IntArray(2)
                resume.getLocationOnScreen(resumePosition); add.getLocationOnScreen(addPosition)
                assertTrue(resumePosition[1] + resume.height < addPosition[1])
                assertTrue(resume.performClick())
            }
            val deadline = System.currentTimeMillis() + 10000
            while (instrumentation.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText(app.name())?.isNotEmpty() != true && System.currentTimeMillis() < deadline) Thread.sleep(50)
            assertTrue("Continue did not recover the confirmation", instrumentation.uiAutomation.rootInActiveWindow.findAccessibilityNodeInfosByText(app.name()).isNotEmpty())
            clickText(context.getString(R.string.later))
            InstallCoordinator.discard(context, token); token = null
            refresh(); awaitVisibility(false)
        } finally {
            instrumentation.runOnMainSync { main.finish() }
            token?.let { InstallCoordinator.discard(context, it, true) }
        }
    }

    @Test fun shizukuInstallsSilentlyThroughItsAuthorizedUserService() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= 24)
        val deadline = System.currentTimeMillis() + 10000
        while (!ShizukuInstaller.ready() && System.currentTimeMillis() < deadline) Thread.sleep(100)
        Assume.assumeTrue("Start Shizuku to exercise silent installation", ShizukuInstaller.ready())
        val main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val oldMethod = InstallationSettings.method(context)
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("packageName", "io.github.ulysseszh.pwainstaller.integrationsilent"); validate() }
        var token: String? = null
        var settings: SettingsActivity? = null
        try {
            val wasAuthorized = ShizukuInstaller.authorized()
            settings = instrumentation.startActivitySync(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as SettingsActivity
            val screen = settings
            instrumentation.runOnMainSync {
                val picker = SettingsActivity::class.java.getDeclaredField("installationMethod").apply { isAccessible = true }.get(screen) as SelectionInput
                picker.value = "shizuku"
                assertEquals("shizuku", InstallationSettings.method(context))
            }
            if (!wasAuthorized) {
                var sawPermission = false
                val permissionDeadline = System.currentTimeMillis() + 15000
                while (!ShizukuInstaller.authorized() && System.currentTimeMillis() < permissionDeadline) {
                    val root = instrumentation.uiAutomation.rootInActiveWindow
                    root?.findAccessibilityNodeInfosByText("Allow")?.firstOrNull { it.isClickable && it.text?.toString()?.startsWith("Allow") == true }
                        ?.let { sawPermission = true; it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
                    Thread.sleep(100)
                }
                assertTrue("Applying the installation method must request permission", sawPermission)
            }
            assertTrue("Shizuku permission was not granted", ShizukuInstaller.authorized())
            instrumentation.runOnMainSync { screen.finish(); main.finish() }; settings = null
            instrumentation.waitForIdleSync()
            token = InstallCoordinator.prepare(context, app, null)
            InstallCoordinator.start(context, token)
            assertEquals("installed", InstallCoordinator.read(context, token).getString("state"))
            assertTrue(Packages.installed(context, app.s("packageName")))
            assertFalse(shell("dumpsys activity activities").lineSequence().filter { "topResumedActivity=" in it }.any { it.contains("packageinstaller", true) })
            InstallCoordinator.complete(context, token)
            assertEquals("done", InstallCoordinator.read(context, token).getString("state"))
            shell("am start -n ${app.s("packageName")}/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            assertTrue(fixture.firstLoaded.await(30, TimeUnit.SECONDS))
            val removed = CountDownLatch(1)
            instrumentation.runOnMainSync { Packages.uninstall(context, app.s("packageName")) { removed.countDown() } }
            assertTrue("Shizuku uninstall must complete silently", removed.await(30, TimeUnit.SECONDS))
            assertFalse(Packages.installed(context, app.s("packageName")))
        } finally {
            InstallationSettings.save(context, oldMethod)
            instrumentation.runOnMainSync { settings?.finish(); main.finish() }
            token?.let { InstallCoordinator.discard(context, it, true) }
            AppDatabase(context).use { it.remove(app) }; shell("pm uninstall ${app.s("packageName")}")
        }
    }

    @Test fun newDraftTemporaryBrowserReturnsToEditingReusesItsSessionAndRetainsDataOnSave() {
        val previousBuilders = BuildSettings.builders(context); BuildSettings.save(context, 1)
        val blocker = "browser-save-blocker:${java.util.UUID.randomUUID()}"
        val started = CountDownLatch(1); val release = CountDownLatch(1)
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as EditorActivity
        var key: String? = null
        var packageName: String? = null
        var session: String? = null
        try {
            awaitEditor(editor)
            instrumentation.runOnMainSync {
                editorInput(editor, "base").text = fixture.url("/")
                (EditorActivity::class.java.getDeclaredField("useManifest").apply { isAccessible = true }.get(editor) as com.google.android.material.checkbox.MaterialCheckBox).isChecked = false
                (EditorActivity::class.java.getDeclaredField("install").apply { isAccessible = true }.get(editor) as com.google.android.material.checkbox.MaterialCheckBox).isChecked = false
                val fields = EditorActivity::class.java.getDeclaredField("fields").apply { isAccessible = true }.get(editor) as Map<*, *>
                (fields["name"] as TextView).text = "Temporary draft fixture"
                EditorActivity::class.java.getDeclaredMethod("openTemporaryBrowser").apply { isAccessible = true }.invoke(editor)
            }
            assertTrue(fixture.storageReady.await(30, TimeUnit.SECONDS))
            val record = requireNotNull(LoginSessions.current(context)); session = record.getString("token"); key = record.getString("appKey")
            LoginSessions.call(context, session, "finish")
            awaitUi("Finishing the temporary browser must return to the editable draft") { editor.hasWindowFocus() && !editor.busy && !editor.isFinishing }
            assertNull(AppDatabase(context).use { it.get(key) })
            val before = fixture.requestedPaths.count { it == "/" }
            instrumentation.runOnMainSync { EditorActivity::class.java.getDeclaredMethod("openTemporaryBrowser").apply { isAccessible = true }.invoke(editor) }
            awaitNative("Opening the temporary browser again must continue the existing session") { fixture.requestedPaths.count { it == "/" } > before }
            assertEquals(session, LoginSessions.current(context)!!.getString("token"))
            assertTrue(LoginSessions.call(context, session, "cookie", fixture.url("/")).getString("value").orEmpty().contains("session=persisted"))
            LoginSessions.call(context, session, "finish")
            awaitUi("Second browser visit must return to the same draft") { editor.hasWindowFocus() && !editor.busy && !editor.isFinishing }
            instrumentation.runOnMainSync {
                assertTrue(BuildJobs.start(context, blocker, blocker, "Held browser save") { _, _ ->
                    started.countDown(); check(release.await(60, TimeUnit.SECONDS)); null
                })
            }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { editor.toolbar.menu.performIdentifierAction(android.R.id.button1, 0) }
            awaitUi("Saving metadata must return without forcing installation") { editor.isFinishing }
            assertTrue(BuildJobs.forApp(key!!)!!.queued)
            assertNull(AppDatabase(context).use { it.get(key!!) })
            val next = LoginSessions.begin(context, java.util.UUID.randomUUID().toString())
            LoginSessions.discard(context, next)
            release.countDown()
            awaitNative("Temporary browsing data must finish saving in the background", timeout = 30000) {
                BuildJobs.forApp(key!!) == null && AppDatabase(context).use { it.get(key!!) }?.let {
                    InstallCoordinator.restoredProfile(context, it.s("packageName")).isFile
                } == true
            }
            val saved = AppDatabase(context).use { it.get(key)!! }; packageName = saved.s("packageName")
            assertTrue(InstallCoordinator.restoredProfile(context, packageName).isFile)
            ZipFile(InstallCoordinator.restoredProfile(context, packageName)).use { zip ->
                assertTrue("A later temporary browser must not erase the queued app's cookies", zip.entries().asSequence()
                    .filter { !it.isDirectory && (it.name.endsWith("/Cookies") || it.name.endsWith("/Cookies-wal")) }
                    .any { zip.getInputStream(it).use { input -> input.readBytes().toString(Charsets.ISO_8859_1).contains("persisted") } })
            }
            assertEquals("Temporary draft fixture", saved.name())
            assertTrue(InstallCoordinator.tokens(context).isEmpty())
            session = null
        } finally {
            release.countDown()
            awaitNative("Queued browser saves must finish before cleanup", timeout = 30000) {
                BuildJobs.get(blocker)?.done != false && key?.let { BuildJobs.forApp(it) == null } != false
            }
            session?.let { LoginSessions.discard(context, it) }
            instrumentation.runOnMainSync {
                editor.finish()
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).filterIsInstance<MainActivity>().forEach { it.finish() }
            }
            key?.let { id -> AppDatabase(context).use { db -> db.get(id)?.let(db::remove) } }
            packageName?.let { InstallCoordinator.restoredProfile(context, it).delete() }
            BuildJobs.remove(blocker); BuildSettings.save(context, previousBuilders)
        }
    }

    @Test fun metadataImportCheckboxesImportOnlyTheSelectedApps() {
        val apps = listOf("Selected", "Not selected").map { title -> WebApp().apply { set("baseUrl", fixture.url("/${title.replace(" ", "-")}/")); manifest().put("name", title); validate() } }
        val main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            awaitUi("Main screen must gain focus") { main.hasWindowFocus() }
            instrumentation.runOnMainSync { MainActivity::class.java.getDeclaredMethod("chooseMetadata", JSONArray::class.java).apply { isAccessible = true }.invoke(main, JSONArray(apps.map { it.data })) }
            awaitNative("Metadata import checkboxes must be visible") { activeNativeNodes().any { it.text?.toString()?.startsWith("Not selected") == true } }
            val second = activeNativeNodes().first { it.text?.toString()?.startsWith("Not selected") == true }
            if (!second.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                val bounds = android.graphics.Rect().also(second::getBoundsInScreen)
                shell("input tap ${bounds.centerX()} ${bounds.centerY()}")
            }
            clickText(context.getString(R.string.import_all))
            awaitNative("Selected app metadata must be imported") { AppDatabase(context).use { it.get(apps[0].s("key")) != null } }
            assertNull(AppDatabase(context).use { it.get(apps[1].s("key")) })
        } finally { instrumentation.runOnMainSync { main.finish() }; AppDatabase(context).use { db -> apps.forEach(db::remove) } }
    }

    @Test fun installedAppTemporaryBrowserRequiresConfirmationAndSurvivesItsDialogRecreation() {
        val name = "io.github.ulysseszh.pwainstaller.integrationseededbrowser"
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("packageName", name); manifest().put("name", "Seeded browser fixture").put("start_url", fixture.url("/?app=second")); validate() }
        var main: MainActivity? = null
        var token: String? = null
        try {
            AppDatabase(context).use { it.save(app) }
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            assertTrue(fixture.storageReady.await(30, TimeUnit.SECONDS))
            main = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            fun openBrowser() {
                val screen = requireNotNull(main)
                awaitUi("Installed app must appear in the list") { views(screen.root).filterIsInstance<com.google.android.material.card.MaterialCardView>().any { card -> views(card).filterIsInstance<TextView>().any { it.text.toString() == app.name() } } }
                instrumentation.runOnMainSync { views(screen.root).filterIsInstance<com.google.android.material.card.MaterialCardView>().single { card -> views(card).filterIsInstance<TextView>().any { it.text.toString() == app.name() } }.performLongClick() }
                clickText(context.getString(R.string.open_temporary_browser))
                awaitNative("The seeded temporary browser must open") { LoginSessions.current(context)?.optBoolean("opened") == true }
                token = LoginSessions.current(context)!!.getString("token")
                assertFalse(LoginSessions.current(context)!!.getBoolean("fresh"))
            }
            openBrowser()
            assertTrue("Temporary browser must receive the installed profile", fixture.restored.await(30, TimeUnit.SECONDS))
            LoginSessions.call(context, token!!, "setCookie", fixture.url("/"), "temporary_changed=yes; Path=/")
            LoginSessions.call(context, token!!, "finish")
            awaitNative("Importing browser changes must require confirmation") { activeNativeNodes().any { it.text?.toString() == context.getString(R.string.apply_browser_changes) } }
            val previous = main
            instrumentation.runOnMainSync { main!!.recreate() }
            awaitUi("Confirmation must survive recreating the app list") {
                main = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).filterIsInstance<MainActivity>().singleOrNull { it !== previous } ?: main
                main !== previous
            }
            clickText(context.getString(R.string.save))
            awaitNative("Confirmed changes must be imported and the recovery job cleaned up") { LoginSessions.current(context) == null && InstallCoordinator.tokens(context).isEmpty() }
            token = null
            fixture.profileCookie.set(null)
            shell("am start -a android.intent.action.VIEW -d '${fixture.url("/verify-cookie")}' -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            awaitNative("Installed app must use the confirmed browser changes") { fixture.profileCookie.get()?.contains("temporary_changed=yes") == true }
            instrumentation.runOnMainSync { main!!.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
            openBrowser()
            LoginSessions.call(context, token!!, "setCookie", fixture.url("/"), "canceled_change=yes; Path=/")
            LoginSessions.call(context, token!!, "finish")
            awaitNative("Second browser visit must also ask before import") { activeNativeNodes().any { it.text?.toString() == context.getString(R.string.apply_browser_changes) } }
            clickText(context.getString(android.R.string.cancel))
            awaitNative("Cancel must discard only the temporary profile") { LoginSessions.current(context) == null }
            token = null; fixture.profileCookie.set(null)
            shell("am start -a android.intent.action.VIEW -d '${fixture.url("/verify-cookie")}' -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            awaitNative("Original profile must remain usable after cancel") { fixture.profileCookie.get() != null }
            assertFalse(fixture.profileCookie.get().contains("canceled_change=yes"))
            assertTrue(fixture.profileCookie.get().contains("temporary_changed=yes"))
        } finally {
            token?.let { LoginSessions.discard(context, it) }
            instrumentation.runOnMainSync { main?.finish() }
            InstallCoordinator.tokens(context).filter { InstallCoordinator.summary(context, it).getJSONObject("app").getString("key") == app.s("key") }.forEach { InstallCoordinator.discard(context, it, true) }
            shell("pm uninstall $name"); AppDatabase(context).use { it.remove(app) }
        }
    }

    @Test fun browserDisplayModeSupportsAddressBackHistoryForwardAndCopy() {
        val name = "io.github.ulysseszh.pwainstaller.integrationbrowserui"
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("packageName", name); manifest().put("display", "browser"); manifest().put("start_url", fixture.url("/browser-ui/one")); validate() }
        fun address() = activeNativeNodes().firstOrNull { it.packageName?.toString() == name && it.className?.toString() == "android.widget.EditText" }
        fun navigate(path: String) {
            val input = requireNotNull(address())
            input.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, fixture.url(path)) }))
            clickText(context.getString(BrowserR.string.go), name)
            awaitNative("Browser address must navigate to $path") { fixture.readyPaths.contains(path) }
        }
        try {
            val apk = ApkBuilder.build(context, app)
            ZipFile(apk).use { zip ->
                val dex = zip.getInputStream(zip.getEntry("classes.dex")).readBytes().toString(Charsets.ISO_8859_1)
                assertTrue(dex.contains("MaterialBrowserUi")); assertTrue(dex.contains("MaterialToolbar"))
            }
            install(apk); apk.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            awaitNative("Optional toolbar must expose the current address") { address()?.text?.toString() == fixture.url("/browser-ui/one") }
            clickText("Next page", name)
            awaitNative("Link navigation must update the address") { address()?.text?.toString() == fixture.url("/browser-ui/two") }
            val back = activeNativeNodes().first { it.packageName?.toString() == name && it.contentDescription?.toString() == context.getString(BrowserR.string.back) }
            assertTrue(back.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK))
            clickText("Browser fixture one\n" + fixture.url("/browser-ui/one"), name)
            awaitNative("History selection must go back without reloading") { address()?.text?.toString() == fixture.url("/browser-ui/one") }
            val overflow = activeNativeNodes().first { it.packageName?.toString() == name && it.contentDescription?.toString() == "More options" }
            assertTrue(overflow.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            clickText(context.getString(BrowserR.string.browser_forward), name)
            awaitNative("Forward must restore the following history entry") { address()?.text?.toString() == fixture.url("/browser-ui/two") }
            navigate("/browser-ui/three")
            activeNativeNodes().first { it.packageName?.toString() == name && it.contentDescription?.toString() == "More options" }.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            clickText(context.getString(BrowserR.string.context_copy_link), name)
            inspectClipboard({ it.getItemAt(0).text?.toString() == fixture.url("/browser-ui/three") }, {})
        } finally { shell("pm uninstall $name") }
    }

    @Test fun credentialApiCapabilitiesMatchInInstalledAppsAndTheTemporaryBrowser() {
        val name = "io.github.ulysseszh.pwainstaller.integrationcredentials"
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("packageName", name); manifest().put("start_url", fixture.url("/credential-page?installed")); validate() }
        var session: String? = null
        try {
            val apk = ApkBuilder.build(context, app)
            ZipFile(apk).use { zip ->
                val manifest = zip.getInputStream(zip.getEntry("AndroidManifest.xml")).readBytes()
                assertTrue(manifest.toString(Charsets.UTF_8).contains("android.permission.CREDENTIAL_MANAGER_SET_ORIGIN") || manifest.toString(Charsets.UTF_16LE).contains("android.permission.CREDENTIAL_MANAGER_SET_ORIGIN"))
                val dex = zip.getInputStream(zip.getEntry("classes.dex")).readBytes().toString(Charsets.ISO_8859_1)
                assertTrue(dex.contains("WEB_AUTHENTICATION")); assertTrue(dex.contains("setWebauthnSupport"))
            }
            install(apk); apk.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            awaitNative("Installed app must expose its native credential capability") { fixture.credentialStates.size == 1 }
            app.manifest().put("start_url", fixture.url("/credential-page?temporary"))
            val token = LoginSessions.begin(context, app.s("key")); session = token
            context.startActivity(LoginSessions.intent(context, app, token).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            awaitNative("Temporary browser must expose its native credential capability") { fixture.credentialStates.size == 2 }
            val installed = fixture.credentialStates[0]; val temporary = fixture.credentialStates[1]
            assertTrue(installed.getBoolean("secure")); assertTrue(temporary.getBoolean("secure"))
            assertEquals(installed.getString("publicKey"), temporary.getString("publicKey"))
            assertEquals(installed.getString("credentials"), temporary.getString("credentials"))
            instrumentation.sendStatus(0, Bundle().apply { putString("credential_capabilities", installed.toString()) })
        } finally { session?.let { LoginSessions.discard(context, it) }; shell("pm uninstall $name") }
    }

    @Test fun onlineIconsUseFetchHeadersSupportSvgAndLeaveTheLookupUrlOutOfMetadata() {
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("install", false); set("headers", "X-Test: expected"); set("userAgent", "OnlineIcons/1"); validate() }
        AppDatabase(context).use { it.save(app) }
        val editor = instrumentation.startActivitySync(Intent(context, EditorActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("id", app.s("key"))) as EditorActivity
        try {
            awaitEditor(editor)
            instrumentation.runOnMainSync { views(editor.root).filterIsInstance<TextView>().single { it.text.toString() == context.getString(R.string.fetch_online_icons) }.performClick() }
            var popupInput: AccessibilityNodeInfo? = null
            awaitNative("Icon URL popup must use the start URL") {
                val nodes = activeNativeNodes()
                popupInput = nodes.singleOrNull { it.className?.toString() == "android.widget.EditText" }
                nodes.any { it.text?.toString().equals(context.getString(android.R.string.cancel), true) } && popupInput?.text?.toString()?.startsWith(fixture.url("/")) == true
            }
            val input = requireNotNull(popupInput)
            assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, fixture.url("/icon.svg")) }))
            clickText(context.getString(R.string.fetch_online_icons))
            awaitUi("Fetched SVG must be added to the editor") { !editor.busy && views(editor.root).filterIsInstance<TextView>().any { it.text.toString().contains(context.getString(R.string.scalable_icon)) } }
            instrumentation.runOnMainSync {
                val updated = EditorActivity::class.java.getDeclaredMethod("read").apply { isAccessible = true }.invoke(editor) as WebApp
                assertEquals(1, updated.icons().length()); assertEquals("any", updated.icons().getJSONObject(0).getString("sizes"))
                assertTrue(updated.icons().getJSONObject(0).getString("src").startsWith("upload:"))
                assertTrue(updated.icons().getJSONObject(0).has("sourceData"))
                assertFalse(updated.data.toString().contains(fixture.url("/icon.svg")))
            }
            assertTrue(fixture.sawHeader.get()); assertEquals("OnlineIcons/1", fixture.lastUserAgent.get())
        } finally { instrumentation.runOnMainSync { editor.finish() }; AppDatabase(context).use { it.remove(app) } }
    }

    @Test fun journalSummariesStaySmallTerminalOperationsDoNotBlockEditingAndOnlyInterruptionsContinue() {
        val app = WebApp().apply {
            set("baseUrl", "https://www.facebook.com/")
            set("adblockRules", "facebook.com#%#" + "a".repeat(11000) + "\n" + "||ad.test^\n".repeat(40000)); validate()
        }
        val token = InstallCoordinator.prepare(context, app, null)
        try {
            assertFalse(InstallCoordinator.interrupted(context, token))
            assertTrue(InstallCoordinator.summary(context, token).toString().length < 2048)
            assertEquals(app.s("adblockRules"), InstallCoordinator.read(context, token).getJSONObject("app").getString("adblockRules"))
            InstallCoordinator.interrupt(context, token); assertTrue(InstallCoordinator.interrupted(context, token))
            InstallCoordinator.fail(context, token, "Test failure")
            InstallCoordinator.ensureEditable(context, app)
            assertFalse(token in InstallCoordinator.tokens(context))
        } finally { if (token in InstallCoordinator.tokens(context)) InstallCoordinator.discard(context, token, true) }
    }

    @Test fun completeFacebookRulesValidateAndBuildOfflineWithMonotonicProgress() {
        val rules = File(context.getExternalFilesDir(null), "facebook.txt")
        Assume.assumeTrue("Supply the complete selected Facebook rule snapshot", rules.isFile)
        val app = WebApp().apply { set("baseUrl", "https://www.facebook.com/"); set("adblockRules", rules.readText()); validate() }
        val progress = mutableListOf<io.github.ulysseszh.pwainstaller.core.ApkProgress>()
        val before = fixture.requestCount.get()
        try {
            AppDatabase(context).use { it.save(app) }
            val saved = AppDatabase(context).use { it.get(app.s("key"))!! }
            assertEquals(rules.readText(), saved.s("adblockRules"))
            val apk = ApkBuilder.build(context, saved, progress::add)
            ZipFile(apk).use { zip -> assertNotNull(zip.getEntry("assets/adblock.js")) }; apk.delete()
            val values = progress.map { it.overallPercent }
            assertEquals(values.sorted(), values); assertEquals(0, values.first()); assertEquals(100, values.last())
            assertEquals(before, fixture.requestCount.get())
        } finally { AppDatabase(context).use { it.remove(app) } }
    }

    @Test fun anExpiredInstallerSessionDoesNotApplyAnUpdateThatWasNeverInstalled() {
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("packageName", "io.github.ulysseszh.pwainstaller.integrationexpired"); manifest().put("name", "Original installed metadata"); validate() }
        var token: String? = null
        try {
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete(); AppDatabase(context).use { it.save(app) }
            val edited = app.copy().apply { manifest().put("name", "Canceled update") }
            token = InstallCoordinator.prepare(context, edited, null)
            InstallCoordinator.write(context, token, InstallCoordinator.summary(context, token).put("state", "committed").put("session", Int.MAX_VALUE))
            InstallCoordinator.ensureEditable(context, app)
            assertFalse(token in InstallCoordinator.tokens(context))
            assertEquals("Original installed metadata", AppDatabase(context).use { it.get(app.s("key"))!!.name() })
            InstallCoordinator.discard(context, token)
        } finally { token?.let { InstallCoordinator.discard(context, it, true) }; shell("pm uninstall ${app.s("packageName")}"); AppDatabase(context).use { it.remove(app) } }
    }

    @Test fun cachedSigningIdentityIsReusedAndInvalidatedWhenSettingsChange() {
        val original = SigningKeys.settings(context)
        val first = SigningKeys.load(context)
        assertSame(first, SigningKeys.load(context))
        val edited = SigningKeys.settings(context).put("alias", "nonexistent-test-alias")
        try {
            SigningKeys.save(context, edited)
            try { SigningKeys.load(context); fail("A cached identity must not hide invalid new settings") } catch (_: java.security.GeneralSecurityException) { }
        } finally { SigningKeys.save(context, original) }
        assertArrayEquals(first.certificates.first().encoded, SigningKeys.load(context).certificates.first().encoded)
    }

    @Test fun allDataBackupRestoresMetadataSettingsLibrarySigningKeyAndRetainsAnUninstalledProfile() {
        Assume.assumeTrue(BuildJobs.active().isEmpty() && InstallCoordinator.tokens(context).isEmpty())
        val name = "io.github.ulysseszh.pwainstaller.integrationbackup"
        val app = WebApp().apply { set("baseUrl", fixture.url("/")); set("packageName", name); manifest().put("start_url", fixture.url("/?app=second")); validate() }
        val oldLayout = AppearanceSettings.layout(context); val oldMethod = InstallationSettings.method(context)
        val oldBuilders = BuildSettings.builders(context)
        val oldUrls = AdblockSettings.urls(context); val oldHours = AdblockSettings.hours(context)
        val oldLibrary = UserContentLibrary.load(context)
        val identity = SigningKeys.load(context).certificates.first().encoded
        val archive = File(context.cacheDir, "full-backup-test.zip")
        val ruleUrl = fixture.url("/backup-filters")
        val sourceText = "# " + fixture.url("/disabled-backup-filter") + "\n" + ruleUrl
        val cached = File(context.filesDir, "filters/${io.github.ulysseszh.pwainstaller.core.PackageNames.digest("SHA-256", ruleUrl.toByteArray())}.txt")
        val entry = UserContentFiles.Entry(UUID.randomUUID().toString(), UserContentFiles.Kind.STYLE, "Backup style", "body {color: red}")
        var token: String? = null
        try {
            AppDatabase(context).use { it.save(app) }; UserContentLibrary.save(context, entry)
            AppearanceSettings.save(context, "tiles"); InstallationSettings.save(context, "system"); AdblockSettings.save(context, sourceText, 0)
            BuildSettings.save(context, 3)
            cached.parentFile!!.mkdirs(); cached.writeText("127.0.0.1##.banner")
            val apk = ApkBuilder.build(context, app); install(apk); apk.delete()
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            assertTrue(fixture.storageReady.await(30, TimeUnit.SECONDS))
            archive.outputStream().use { FullBackup.export(context, it) }
            ZipFile(archive).use { zip ->
                assertNotNull(zip.getEntry("keystore.store")); assertNotNull(zip.getEntry("settings.json"))
                assertTrue(zip.entries().asSequence().any { it.name.startsWith("profiles/") })
            }
            shell("pm uninstall $name"); AppDatabase(context).use { it.remove(app) }
            UserContentLibrary.remove(context, entry.id); AppearanceSettings.save(context, "list"); AdblockSettings.save(context, "", 0); cached.delete()
            BuildSettings.save(context, 1)
            assertEquals(1, archive.inputStream().use { FullBackup.restore(context, it) })
            assertEquals("tiles", AppearanceSettings.layout(context)); assertEquals(sourceText, AdblockSettings.urls(context))
            assertEquals(3, BuildSettings.builders(context))
            assertTrue(UserContentLibrary.load(context).any { it.id == entry.id }); assertEquals("127.0.0.1##.banner", cached.readText())
            assertArrayEquals(identity, SigningKeys.load(context).certificates.first().encoded)
            val restored = AppDatabase(context).use { it.get(app.s("key"))!! }
            assertEquals(app.data.toString(), restored.data.toString())
            val profile = InstallCoordinator.restoredProfile(context, name); assertTrue(profile.isFile)
            token = InstallCoordinator.prepare(context, restored, null); assertFalse(profile.exists())
            assertTrue(File(InstallCoordinator.read(context, token).getString("loginSnapshot")).isFile)
            val rebuilt = ApkBuilder.build(context, restored); install(rebuilt); rebuilt.delete()
            InstallCoordinator.installed(context, token); InstallCoordinator.complete(context, token)
            shell("am start -n $name/io.github.ulysseszh.pwainstaller.runtime.WebActivity")
            assertTrue("Backup must restore cookies, localStorage and IndexedDB", fixture.restored.await(30, TimeUnit.SECONDS))
        } finally {
            token?.let { InstallCoordinator.discard(context, it, true) }; InstallCoordinator.restoredProfile(context, name).delete()
            shell("pm uninstall $name"); AppDatabase(context).use { it.remove(app) }; archive.delete(); cached.delete()
            UserContentLibrary.replace(context, oldLibrary); AppearanceSettings.save(context, oldLayout); InstallationSettings.save(context, oldMethod); AdblockSettings.save(context, oldUrls, oldHours)
            BuildSettings.save(context, oldBuilders)
        }
    }

    @Test fun allDataImportRejectsInvalidSettingsBeforeMutatingAnything() {
        Assume.assumeTrue(BuildJobs.active().isEmpty() && InstallCoordinator.tokens(context).isEmpty())
        val data = ByteArrayOutputStream(); FullBackup.export(context, data)
        val changed = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(changed).use { output -> ZipInputStream(ByteArrayInputStream(data.toByteArray())).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                var bytes = input.readBytes()
                if (entry.name == "settings.json") bytes = JSONObject(bytes.toString(Charsets.UTF_8)).apply {
                    getJSONObject("preferences").put("appearance", JSONObject().put("layout", JSONObject().put("type", "int").put("value", 42)))
                }.toString().toByteArray()
                output.putNextEntry(java.util.zip.ZipEntry(entry.name)); output.write(bytes); output.closeEntry()
            }
        } }
        val oldLayout = AppearanceSettings.layout(context); val identity = SigningKeys.load(context).certificates.first().encoded
        val metadata = AppDatabase(context).use { JSONArray(it.all().map { app -> app.data }).toString() }
        try { FullBackup.restore(context, ByteArrayInputStream(changed.toByteArray())); fail("Malformed settings must be rejected") } catch (_: IllegalArgumentException) { }
        assertEquals(oldLayout, AppearanceSettings.layout(context)); assertArrayEquals(identity, SigningKeys.load(context).certificates.first().encoded)
        assertEquals(metadata, AppDatabase(context).use { JSONArray(it.all().map { app -> app.data }).toString() })
    }

    private class Fixture : Closeable {
        private val server = ServerSocket(0)
        private val worker = Executors.newCachedThreadPool()
        val sawHeader = AtomicBoolean()
        val lastUserAgent = AtomicReference<String>()
        val openedFile = AtomicReference<String>()
        val sharedBody = AtomicReference<String>()
        val shareQuery = AtomicReference<String>()
        val shareWorkerStates = CopyOnWriteArrayList<JSONObject>()
        val fileOpened = CountDownLatch(1)
        val shareReceived = CountDownLatch(1)
        val firstLoaded = CountDownLatch(1)
        val storageReady = CountDownLatch(1)
        val restored = CountDownLatch(1)
        val filterVersion = java.util.concurrent.atomic.AtomicInteger(1)
        val filterFetchStarted = List(2) { CountDownLatch(1) }
        val continueFilters = List(2) { CountDownLatch(1) }
        val adResourceRequests = java.util.concurrent.atomic.AtomicInteger()
        val adblockDone = CountDownLatch(1)
        val adblockResult = AtomicReference<String>()
        val cleanChecked = CountDownLatch(1)
        val loginReady = CountDownLatch(1)
        val cleanState = AtomicReference<String>()
        val navigationCookies = ConcurrentHashMap<String, String>()
        val navigationAgents = ConcurrentHashMap<String, String>()
        val clickPoints = ConcurrentHashMap<String, JSONObject>()
        val readyPaths = java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
        val fetchStarted = CountDownLatch(1)
        val continueFetch = CountDownLatch(1)
        val pageResourceStarted = CountDownLatch(1)
        val continuePage = CountDownLatch(1)
        val imageCopyStarted = CountDownLatch(1)
        val continueImageCopy = CountDownLatch(1)
        val imageCopyRequests = java.util.concurrent.atomic.AtomicInteger()
        val cutoutWidths = CopyOnWriteArrayList<Int>()
        val displayResult = AtomicReference<JSONObject>()
        val requestCount = java.util.concurrent.atomic.AtomicInteger()
        val requestedPaths = CopyOnWriteArrayList<String>()
        val fullscreenStates = CopyOnWriteArrayList<JSONObject>()
        val viewportStates = CopyOnWriteArrayList<JSONObject>()
        val customStates = CopyOnWriteArrayList<JSONObject>()
        val credentialStates = CopyOnWriteArrayList<JSONObject>()
        val profileCookie = AtomicReference<String>()
        val insetStates = CopyOnWriteArrayList<JSONObject>()
        val colorSchemeStates = CopyOnWriteArrayList<JSONObject>()
        val themePageGates = ConcurrentHashMap<String, CountDownLatch>()
        val themeCommands = ConcurrentHashMap<String, String>()
        private val layoutScript = """
            <script>addEventListener('load',function(){var points=[];var nodes=document.querySelectorAll('a,button,img,[data-label]');
            for(var i=0;i<nodes.length;i++){var r=nodes[i].getBoundingClientRect();points.push({label:nodes[i].getAttribute('data-label')||nodes[i].textContent,width:innerWidth,x:r.left+Math.min(60,r.width/2),y:r.top+r.height/2});}
            var x=new XMLHttpRequest();x.open('GET','/layout?page='+encodeURIComponent(location.pathname)+'&data='+encodeURIComponent(JSON.stringify(points)),true);x.send();});</script>
        """.trimIndent()
        @Volatile private var closed = false
        init {
            worker.execute { while (!closed) try { val socket = server.accept(); worker.execute { handle(socket) } } catch (_: IOException) { } }
        }
        fun url(path: String) = "http://127.0.0.1:${server.localPort}$path"
        val port get() = server.localPort
        private fun favicon(): ByteArray {
            val image = java.nio.ByteBuffer.allocate(40 + 16 * 16 * 4 + 16 * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            image.putInt(40).putInt(16).putInt(32).putShort(1).putShort(32).putInt(0)
                .putInt(16 * 16 * 4).putInt(0).putInt(0).putInt(0).putInt(0)
            repeat(16 * 16) { image.put(105).put(163.toByte()).put(36).put(255.toByte()) }
            return java.nio.ByteBuffer.allocate(22 + image.capacity()).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
                putShort(0).putShort(1).putShort(1).put(16).put(16).putShort(0).putShort(1).putShort(32)
                putInt(image.capacity()).putInt(22).put(image.array())
            }.array()
        }
        private fun handle(socket: Socket) {
            try {
                socket.use { client ->
                    val reader = client.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                    val first = reader.readLine() ?: return
                    val path = first.split(' ')[1]
                    requestedPaths.add(path)
                    requestCount.incrementAndGet()
                    val headers = mutableMapOf<String, String>()
                    while (true) {
                        val line = reader.readLine()?.takeIf { it.isNotEmpty() } ?: break
                        val colon = line.indexOf(':')
                        if (colon > 0) headers[line.substring(0, colon).lowercase(Locale.ROOT)] = line.substring(colon + 1).trim()
                    }
                    val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                    val posted = CharArray(contentLength)
                    var consumed = 0
                    while (consumed < contentLength) { val count = reader.read(posted, consumed, contentLength - consumed); if (count == -1) break; consumed += count }
                    lastUserAgent.set(headers["user-agent"])
                    if (path.startsWith("/share")) {
                        sharedBody.set(String(posted, 0, consumed)); shareQuery.set(path); shareReceived.countDown()
                    }
                    if (headers["x-test"] == "expected") sawHeader.set(true)
                    if (path.startsWith("/inside/") || path.startsWith("/outside")) {
                        navigationAgents[path] = headers["user-agent"].orEmpty()
                        navigationCookies[path] = headers["cookie"].orEmpty()
                    }
                    var status = 200
                    var type = "text/html"
                    var binary: ByteArray? = null
                    var location: String? = null
                    var responseCookie = ""
                    var waitForImageCopy = false
                    val body = when {
                        path.startsWith("/theme-command?") -> {
                            type = "text/plain"; themeCommands[Uri.parse(path).getQueryParameter("case")].orEmpty()
                        }
                        path.startsWith("/theme-hold.js?") -> { type = "application/javascript"; "/*".padEnd(65534, ' ') + "*/" }
                        path.startsWith("/theme-colors?") -> {
                            val case = Uri.parse(path).getQueryParameter("case")!!
                            val missing = Uri.parse(path).getQueryParameter("missing") == "1"
                            val metas = if (missing) "" else """<meta name="theme-color" content="not-a-color">
                                <meta name="theme-color" content="red" media="(min-width:999999px)">
                                <meta id="light-theme" name="THEME-COLOR" content="#146b8c" media="(prefers-color-scheme:light)">
                                <meta id="dark-theme" name="theme-color" content="#edbc42" media="(prefers-color-scheme:dark)">"""
                            val pageBackground = if (missing) "transparent" else "#e1e2e3"
                            """<!doctype html><html style="background:$pageBackground"><head><meta name="viewport" content="width=device-width,initial-scale=1">$metas</head>
                            <body style="margin:0;background:$pageBackground">Page theme fixture<script>
                            var stage='initial';setInterval(function(){var x=new XMLHttpRequest();x.open('GET','/theme-command?case=$case',true);x.onload=function(){
                            var next=x.responseText;if(next===stage)return;stage=next;
                            if(next==='navigate'){if(location.search.indexOf('missing=1')<0)location.href='/theme-colors?case=$case&missing=1';return;}
                            if(next==='dynamic'){['light-theme','dark-theme'].forEach(function(id){var e=document.getElementById(id);if(e)e.content='rgb(29,161,242)';});}
                            if(next==='invalid'){var nodes=document.getElementsByTagName('meta');for(var i=0;i<nodes.length;i++)if((nodes[i].name||'').toLowerCase()==='theme-color')nodes[i].content='not-a-color';}
                            };x.send();},100);</script><script src="/theme-hold.js?case=$case"></script></body></html>"""
                        }
                        path.startsWith("/color-scheme-state?data=") -> { colorSchemeStates.add(JSONObject(Uri.parse(path).getQueryParameter("data")!!)); "ok" }
                        path.startsWith("/color-scheme?") -> """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
                            <style>#sample{color:rgb(11,22,33)}@media(prefers-color-scheme:dark){#sample{color:rgb(211,222,233)}}</style></head>
                            <body><span id="sample">Theme probe</span><input id="draft"><script>
                            var token=String(Date.now())+Math.random(),changes=0,ready=false,dark=matchMedia('(prefers-color-scheme: dark)'),observedDark=dark.matches;
                            document.getElementById('draft').value='Unsaved text';
                            // Use fresh queries for polling: old Chromium mutates a watched query's cached result when reading .matches.
                            dark.addEventListener('change',function(event){changes++;observedDark=event.matches;});requestAnimationFrame(function(){requestAnimationFrame(function(){ready=true;});});
                            setInterval(function(){var x=new XMLHttpRequest();x.open('GET','/color-scheme-state?data='+encodeURIComponent(JSON.stringify({
                            case:new URL(location.href).searchParams.get('case'),token:token,changes:changes,ready:ready,observedDark:observedDark,
                            dark:matchMedia('(prefers-color-scheme: dark)').matches,light:matchMedia('(prefers-color-scheme: light)').matches,
                            color:getComputedStyle(document.getElementById('sample')).color,draft:document.getElementById('draft').value})),true);x.send();},100);
                            </script></body></html>"""
                        path.startsWith("/inset-state?data=") -> { insetStates.add(JSONObject(Uri.parse(path).getQueryParameter("data")!!)); "ok" }
                        // Disable old WebView input auto-zoom so this fixture measures native insets at a fixed page scale.
                        path == "/media-lifecycle" -> """<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1"><body style="height:4000px;margin:0;background:#ddd">Media lifecycle fixture</body>"""
                        path == "/video-playing" -> { readyPaths.add(path); "ok" }
                        path == "/video-scroll" -> """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">$layoutScript</head><body style="height:4000px;margin:0">
                            <button style="position:fixed;top:20px;left:20px;z-index:2;padding:16px" onclick="v.play();v.requestFullscreen()">Fullscreen video</button>
                            <div style="height:1500px">Video feed</div><video id="v" muted autoplay playsinline style="width:300px;height:180px"></video><canvas id="source" width="160" height="90" hidden></canvas>
                            <script>var c=document.getElementById('source'),ctx=c.getContext('2d'),v=document.getElementById('v'),n=0;
                            v.addEventListener('playing',function(){var r=new XMLHttpRequest();r.open('GET','/video-playing',true);r.send();});
                            v.srcObject=c.captureStream(24);setInterval(function(){ctx.fillStyle=(++n%2)?'#345678':'#abcdef';ctx.fillRect(0,0,160,90);},40);</script></body></html>"""
                        path == "/inset-layout" -> """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,viewport-fit=cover">$layoutScript
                            <style>html,body{margin:0}#bar{position:fixed;bottom:0;width:100%;height:40px;padding-bottom:0;padding-bottom:env(safe-area-inset-bottom);background:#123456}input{position:fixed;top:40px;width:180px;font-size:20px}</style></head><body>
                            <input placeholder="Keyboard field" data-label="Keyboard field" aria-label="Keyboard field"><div id="bar">Bottom bar</div><script>setInterval(function(){var b=document.getElementById('bar');var x=new XMLHttpRequest();x.open('GET','/inset-state?data='+encodeURIComponent(JSON.stringify({height:innerHeight,visualHeight:window.visualViewport?visualViewport.height:innerHeight,ratio:devicePixelRatio,bottom:b.getBoundingClientRect().bottom,safeBottom:parseFloat(getComputedStyle(b).paddingBottom)||0})),true);x.send();},150);</script></body></html>"""
                        path == "/toolbar-colors" -> """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">$layoutScript<style>html,body{margin:0}#page{height:2500px;width:100%;background:#161616}button{position:fixed;left:50%;transform:translateX(-50%);top:0;width:150px;background:red;color:white;height:40px}#change-page{top:60px}#footer{position:fixed;bottom:0;width:100%;height:48px;background:#161616}#bottom-button{top:auto;bottom:0}#change-footer{top:auto;bottom:60px}</style></head>
                            <body><div id="page"><button onclick="this.style.background='lime';this.textContent='Button changed'">Change button</button><button id="change-page" onclick="document.getElementById('page').style.background='#e0e0e0'">Change page</button></div><div id="footer"><button id="bottom-button" onclick="this.style.background='lime';this.textContent='Bottom button changed'">Change bottom button</button><button id="change-footer" onclick="document.getElementById('footer').style.background='#123456'">Change footer</button></div></body></html>"""
                        path == "/verify-cookie" -> { profileCookie.set(headers["cookie"].orEmpty()); "Profile cookies checked" }
                        path.startsWith("/browser-ui/") -> {
                            val page = path.substringAfterLast('/'); readyPaths.add(path)
                            "<html><head><title>Browser fixture $page</title>$layoutScript<meta name='viewport' content='width=device-width,initial-scale=1'></head><body><a href='/browser-ui/two'>Next page</a></body></html>"
                        }
                        path.startsWith("/credential-state?data=") -> { credentialStates.add(JSONObject(Uri.parse(path).getQueryParameter("data")!!)); "ok" }
                        path.startsWith("/credential-page?") -> """<meta name="viewport" content="width=device-width,initial-scale=1"><script>
                            var x=new XMLHttpRequest();x.open('GET','/credential-state?data='+encodeURIComponent(JSON.stringify({secure:window.isSecureContext===true||location.hostname==='127.0.0.1',publicKey:typeof PublicKeyCredential,credentials:typeof navigator.credentials})),true);x.send();</script>"""
                        path == "/instant-image-menu" -> """<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1">$layoutScript
                            <script>document.cookie='menu=yes; Path=/'</script>
                            <img src="/slow-copy-image.png" data-label="Slow image" alt="Slow image" width="160" height="80">"""
                        path == "/slow-copy-image.png" -> {
                            type = "image/png"
                            if (!headers["cookie"].orEmpty().contains("menu=yes") || headers["user-agent"] != "InstantCopy/1.0") { status = 403; "Sign in" }
                            else { waitForImageCopy = imageCopyRequests.incrementAndGet() > 1; binary = IconData.decode(IconData.default); "" }
                        }
                        path == "/context-menu" -> """
                            <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">$layoutScript
                            <style>body{margin:0}a,img,.banner{display:block;height:32px}img{width:80px}input{margin-top:10px}.banner{width:160px;position:relative}.banner img{width:100%}.cover{position:absolute;left:0;top:0;width:100%;height:100%}.pseudo:before{content:'';position:absolute;left:0;top:0;width:100%;height:100%;background-image:url('/context-image.png')}</style>
                            <script>document.cookie='menu=yes; Path=/'</script></head><body>
                            <a href="/context-target" data-label="Text link" aria-label="Text link">Context link</a>
                            <a href="/context-target?image" data-label="Image link" aria-label="Image link"><img src="/context-image.png" data-label="Linked image" alt="Linked image"></a>
                            <img src="/context-image.png" data-label="Standalone image" alt="Standalone image">
                            <img src="data:image/png;base64,${IconData.default}" data-label="Data image" alt="Data image">
                            <img id="blob" data-label="Blob image" alt="Blob image">
                            <div class="banner" role="img" data-label="CSS banner" aria-label="CSS banner" style="background-image:url('/context-image.png')"></div>
                            <div class="banner" role="img" data-label="Overlay banner" aria-label="Overlay banner"><img src="/context-image.png"><span class="cover"></span></div>
                            <div class="banner" role="img" data-label="Passive image" aria-label="Passive image"><img style="pointer-events:none" src="/context-image.png"></div>
                            <div class="banner pseudo" role="img" data-label="Pseudo banner" aria-label="Pseudo banner"></div>
                            <div id="shadow" class="banner" role="img" data-label="Shadow banner" aria-label="Shadow banner"></div>
                            <input value="Native text selection">
                            <script>var h=document.getElementById('shadow');var root=h.attachShadow?h.attachShadow({mode:'open'}):h.createShadowRoot();
                            root.innerHTML='<img style="display:block;width:160px;height:32px" src="/context-image.png">';</script>
                            <script>var x=new XMLHttpRequest();x.open('GET','/context-image.png',true);x.responseType='blob';x.onload=function(){
                            document.getElementById('blob').src=URL.createObjectURL(x.response);};x.send();</script></body></html>
                        """.trimIndent()
                        path == "/context-image.png" -> {
                            type = "image/png"
                            if (!headers["cookie"].orEmpty().contains("menu=yes")) { status = 403; "Sign in" }
                            else { binary = IconData.decode(IconData.default); "" }
                        }
                        path.startsWith("/context-target") -> { readyPaths.add("/context-target"); "<html><body>Context target</body></html>" }
                        path.startsWith("/fullscreen-state?data=") -> { fullscreenStates.add(JSONObject(java.net.URLDecoder.decode(path.substringAfter("data="), "UTF-8"))); "ok" }
                        path == "/fullscreen-canvas" -> """
                            <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">$layoutScript
                            <style>body{margin:0}button{height:56px;font-size:20px}canvas{width:100%;height:40px;background:#24a369}canvas:fullscreen,canvas:-webkit-full-screen{width:100vw;height:100vh}</style></head>
                            <body><button onclick="var c=document.querySelector('canvas');if(c.requestFullscreen)c.requestFullscreen();else c.webkitRequestFullscreen();">Fullscreen canvas</button><canvas></canvas><script>
                            var token=String(Math.random());function report(){var c=document.querySelector('canvas'),r=c.getBoundingClientRect(),b=document.querySelector('button').getBoundingClientRect(),f=!!(document.fullscreenElement||document.webkitFullscreenElement);
                            var state={token:token,fullscreen:f,width:innerWidth,height:innerHeight,screenWidth:screen.width,screenHeight:screen.height,canvasWidth:r.width,canvasHeight:r.height,buttonX:b.left+Math.min(60,b.width/2),buttonY:b.top+b.height/2};
                            var x=new XMLHttpRequest();x.open('GET','/fullscreen-state?data='+encodeURIComponent(JSON.stringify(state)),true);x.send();}
                            addEventListener('load',function(){setInterval(report,200);});</script></body></html>
                        """.trimIndent()
                        path.startsWith("/display-result?data=") -> { displayResult.set(JSONObject(java.net.URLDecoder.decode(path.substringAfter("data="), "UTF-8"))); "ok" }
                        path == "/display-external.css" -> {
                            type = "text/css"
                            listOf("standalone", "minimal-ui", "browser", "fullscreen").joinToString("\n") { mode ->
                                "@media (display-mode: $mode) { #external { --reported-mode: $mode; } }"
                            }
                        }
                        path == "/display-check" -> """
                            <!doctype html><html><head><script>
                            window.modes=['standalone','minimal-ui','browser','fullscreen'];
                            function matchingModes(){return modes.filter(function(mode){return matchMedia('(display-mode: '+mode+')').matches;});}
                            window.earlyModes=matchingModes();</script>
                            <style>${listOf("standalone", "minimal-ui", "browser", "fullscreen").joinToString("\n") { mode ->
                                "@media (display-mode: $mode) { #inline { --reported-mode: $mode; } }"
                            }}</style>
                            ${listOf("standalone", "minimal-ui", "browser", "fullscreen").joinToString("\n") { mode ->
                                "<style media=\"(display-mode: $mode)\">#attribute { --reported-mode: $mode; }</style>"
                            }}
                            <link rel="stylesheet" href="http://localhost:${server.localPort}/display-external.css"></head>
                            <body><div id="inline">Inline</div><div id="external">External</div><div id="attribute">Attribute</div><div id="dynamic">Dynamic</div>
                            <script>var attempts=0,dynamicAdded=false;
                            function check(){var matched=matchingModes(),mode=matched.length===1?matched[0]:'invalid';
                            if(window.__pwaDisplayModeInstalled&&!dynamicAdded){var style=document.createElement('style');document.head.appendChild(style);modes.forEach(function(m){style.sheet.insertRule('@media (display-mode: '+m+') { #dynamic { --reported-mode: '+m+'; } }',style.sheet.cssRules.length);});dynamicAdded=true;}
                            function correct(id){return getComputedStyle(document.getElementById(id)).getPropertyValue('--reported-mode').trim()===mode;}
                            var result={mode:mode,early:window.earlyModes.length===1&&window.earlyModes[0]===mode,inline:correct('inline'),external:correct('external'),attribute:correct('attribute'),dynamic:correct('dynamic'),
                            nativeQueries:matchMedia('(min-width: 1px)').matches&&!matchMedia('(max-width: 0px)').matches,
                            mediaText:matchMedia('(display-mode: '+mode+')').media==='(display-mode: '+mode+')'};
                            if(result.inline&&result.external&&result.attribute&&result.dynamic&&result.nativeQueries||++attempts>60){var x=new XMLHttpRequest();x.open('GET','/display-result?data='+encodeURIComponent(JSON.stringify(result)),true);x.send();}else setTimeout(check,100);}
                            addEventListener('load',check);</script></body></html>
                        """.trimIndent()
                        path.startsWith("/cutout-size?") -> { cutoutWidths.add(path.substringAfter("width=").toInt()); "ok" }
                        path == "/cutout-check" -> """<meta name="viewport" content="width=device-width,initial-scale=1"><script>
                            function report(){var x=new XMLHttpRequest();x.open('GET','/cutout-size?width='+innerWidth,true);x.send();}
                            addEventListener('load',report);addEventListener('resize',report);setTimeout(report,400);
                            </script>Cutout fixture"""
                        path == "/slow-manifest.json" -> { type = "application/manifest+json"; """{"name":"Progress fixture","icons":[]}""".padEnd(65536, ' ') }
                        path == "/library-script.js" -> { type = "application/javascript"; "window.libraryVersion=${filterVersion.get()};" }
                        path.startsWith("/custom-state?data=") -> { customStates.add(JSONObject(Uri.parse(path).getQueryParameter("data")!!)); "ok" }
                        path.startsWith("/custom-content") || path == "/custom-frame" -> """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                            <body><div id="styled">Original text</div>${if (path == "/custom-frame") "" else "<iframe src='/custom-frame'></iframe>"}<script src="/custom-reporter.js"></script></body></html>"""
                        path == "/custom-reporter.js" -> { type = "application/javascript"; """
                            function report(){var e=document.getElementById('styled');var s={main:window.top===window,runs:window.userScriptRuns||0,disabled:!!window.disabledScriptRan,color:getComputedStyle(e).color,text:e.textContent};
                            var x=new XMLHttpRequest();x.open('GET','/custom-state?data='+encodeURIComponent(JSON.stringify(s)),true);x.send();}
                            addEventListener('load',function(){setInterval(report,200);});""" }
                        path.startsWith("/viewport-state?data=") -> { viewportStates.add(JSONObject(Uri.parse(path).getQueryParameter("data")!!)); "ok" }
                        path == "/window-sizing" -> """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
                            <style>body{margin:0;height:4000px}input{position:fixed;top:40px;left:40px}
                            .corner{position:fixed;width:32px;height:32px;pointer-events:none}.tl{top:0;left:0;background:#f34a91}.tr{top:0;right:0;background:#19d68c}
                            .bl{bottom:0;left:0;background:#2e6ae8}.br{bottom:0;right:0;background:#f6b820}#render-tick{position:fixed;top:80px;left:40px}</style></head>
                            <body><div class="corner tl"></div><div class="corner tr"></div><div class="corner bl"></div><div class="corner br"></div><span id="render-tick"></span><input id="draft"><script>
                            var token=String(Math.random());document.getElementById('draft').value='Unsaved form text';
                            function report(){var s={token:token,width:innerWidth,height:innerHeight,ratio:devicePixelRatio,scroll:scrollY,input:document.getElementById('draft').value};
                            document.getElementById('render-tick').textContent=Date.now();
                            var x=new XMLHttpRequest();x.open('GET','/viewport-state?data='+encodeURIComponent(JSON.stringify(s)),true);x.send();}
                            addEventListener('load',function(){scrollTo(0,300);setInterval(report,200);});</script></body></html>"""
                        path == "/slow-page" -> "<html><head>$layoutScript<script src='/slow-page-resource.js'></script></head><body>Loading fixture</body></html>"
                        path == "/page-colors" -> """<html style="background:#123456"><head><meta name="viewport" content="width=device-width,initial-scale=1">$layoutScript</head>
                            <body style="margin:0;background:rgba(255,0,0,0.5)"><button onclick="document.body.style.background='#f5f5f5'">Change background</button>
                            <script src='/slow-page-resource.js'></script></body></html>"""
                        path == "/slow-page-resource.js" -> { type = "application/javascript"; "/*".padEnd(65534, ' ') + "*/" }
                        path == "/favicon.ico" -> { type = "image/x-icon"; binary = favicon(); "" }
                        path == "/icon-fallback/manual/" -> "<link rel='shortcut ICON' href='missing.png'>"
                        path == "/icon-fallback/manual/missing.png" -> { status = 404; "No icon" }
                        path == "/slow-filters.txt" || path == "/unknown-length-filters.txt" -> {
                            type = "text/plain"
                            "127.0.0.1##.downloaded-banner\n!".padEnd(128 * 1024, ' ') + "\n"
                        }
                        path == "/filters.txt" -> {
                            type = "text/plain"
                            """
                                ! AdGuard fixture
                                127.0.0.1###${if (filterVersion.get() == 1) "cookie-notice" else "updated-cookie"}
                                127.0.0.1#${'$'}#body { overflow: auto !important; }
                                127.0.0.1#?#.banner:contains(Install our app)
                                127.0.0.1#%#//scriptlet('set-cookie', 'pwaConsent', 'true')
                                ||localhost^${'$'}image,domain=127.0.0.1
                                elsewhere.test##.shouldRemain
                            """.trimIndent()
                        }
                        path == "/ad-resource.svg" -> {
                            adResourceRequests.incrementAndGet(); type = "image/svg+xml"
                            """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10"><rect width="10" height="10" fill="red"/></svg>"""
                        }
                        path.startsWith("/ad-result?") -> { adblockResult.set(path); adblockDone.countDown(); "ok" }
                        path.startsWith("/adblock-check") -> """
                            <html><body style="overflow:hidden"><div id="cookie-notice">Cookie notice</div><div class="banner">Install our app</div>
                            <img src="http://localhost:${server.localPort}/ad-resource.svg" onerror="window.imageBlocked=true" onload="window.imageBlocked=false">
                            <script>var count=0;function check(){var h=getComputedStyle(document.getElementById('cookie-notice')).display==='none';
                            var e=getComputedStyle(document.querySelector('.banner')).display==='none';var s=getComputedStyle(document.body).overflow==='auto';
                            var c=document.cookie.indexOf('pwaConsent=true')>=0;var n=window.imageBlocked===true;
                            if(h&&e&&s&&c&&n||++count>=40){var x=new XMLHttpRequest();x.open('GET','/ad-result?hidden='+h+'&extended='+e+'&style='+s+'&cookie='+c+'&network='+n,true);x.send();}
                            else setTimeout(check,250);}setTimeout(check,500);</script></body></html>
                        """.trimIndent()
                        path.startsWith("/layout?") -> {
                            readyPaths.add(Uri.parse(path).getQueryParameter("page").orEmpty())
                            val points = JSONArray(java.net.URLDecoder.decode(path.substringAfter("data="), "UTF-8"))
                            repeat(points.length()) { index -> points.getJSONObject(index).let { clickPoints[it.getString("label")] = it } }
                            "ok"
                        }
                        path.startsWith("/soft-auth") && !headers["cookie"].orEmpty().contains("authenticated=yes") -> """
                            <html><head>$layoutScript<meta name="viewport" content="width=device-width,initial-scale=1"><style>body{margin:0}button{height:56px;font-size:20px}</style></head><body><button onclick="document.cookie='authenticated=yes; Path=/';location.reload()">Log in</button><script>
                            var clean=!localStorage.getItem('marker')&&document.cookie.indexOf('session=persisted')<0;
                            var r=indexedDB.open('pwa-test',1);r.onupgradeneeded=function(){r.result.createObjectStore('store');};
                            r.onsuccess=function(){var tx=r.result.transaction('store');var get=tx.objectStore('store').get('marker');
                            get.onsuccess=function(){var x=new XMLHttpRequest();x.open('GET','/clean?state='+(clean&&!get.result),true);x.send();};};
                            </script></body></html>
                        """.trimIndent()
                        path.startsWith("/clean?") -> { cleanState.set(path); cleanChecked.countDown(); "ok" }
                        path.startsWith("/login-ready") -> { loginReady.countDown(); "ok" }
                        path.startsWith("/outside/back?") -> {
                            val query = Uri.parse(path)
                            status = query.getQueryParameter("status")?.toInt() ?: 302
                            location = if (query.getQueryParameter("cross") == "yes") url("/inside/returned?cross") else "/outside/hop?" + path.substringAfter('?')
                            responseCookie = "Set-Cookie: redirected=yes; Path=/\r\n"
                            "Redirecting"
                        }
                        path.startsWith("/outside/hop?") -> { status = 302; location = "../inside/returned?" + path.substringAfter('?'); "Redirecting" }
                        path == "/outside/forward" -> { status = 302; location = "/outside/final"; "Redirecting" }
                        path == "/inside/native-launch" -> """<html><head>$layoutScript</head><body><a href="/outside/native">Native app redirect</a></body></html>"""
                        path == "/outside/native" -> { status = 301; location = "https://external-dispatch-fixture.invalid/handoff/from-redirect?item=1"; "Redirecting" }
                        path == "/inside/redirect" -> { status = 302; location = "/outside?redirect"; "Redirecting" }
                        path.startsWith("/inside/") || path.startsWith("/outside") -> """
                            <html><head>${if (path.startsWith("/inside/")) layoutScript else ""}<meta name="viewport" content="width=device-width,initial-scale=1"><style>body{margin:0}a{display:block;height:56px;font-size:20px}</style></head><body><a href="/inside/allowed">Allowed link</a>
                            <a href="/outside?link">Outside link</a>
                            <a href="/inside/redirect">External redirect</a>
                            <a href="/outside?popup" target="_blank">External popup</a>
                            ${listOf(301, 302, 303, 307, 308).joinToString(" ") { "<a href='/outside/back?status=$it'>Redirect back $it</a>" }}
                            <a href="/outside/back?popup" target="_blank">Popup back</a>
                            <a href="http://localhost:${server.localPort}/outside/back?cross=yes">Cross origin back</a>
                            <a href="/outside/forward">Outside redirect</a></body></html>
                        """.trimIndent()
                        path.startsWith("/basic") && headers["authorization"] != "Basic dXNlcjpzZWNyZXQ=" -> { status = 401; "Sign in" }
                        path.startsWith("/auth") && !headers["cookie"].orEmpty().contains("authenticated=yes") -> { status = 401; "Sign in" }
                        path.startsWith("/manifest.json") -> { type = "application/manifest+json"; """{"name":"Device fixture","id":"identity","start_url":"/","scope":"/","display":"standalone","icons":[{"src":"/invalid-icon.png","sizes":"512x512"},{"src":"/icon.svg","sizes":"any","type":"image/svg+xml"}]}""" }
                        path == "/icon.svg" -> { type = "image/svg+xml"; """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" fill="#24a369"/></svg>""" }
                        path.startsWith("/opened?") -> { openedFile.set(path); fileOpened.countDown(); "ok" }
                        path == "/share-worker/start" -> """<!doctype html><script>
                            navigator.serviceWorker.register('/share-worker/sw.js').then(function(){return navigator.serviceWorker.ready;})
                            .then(function(){fetch('/share-worker/registered');});</script>Ready for shares"""
                        path == "/share-worker/sw.js" -> { type = "application/javascript"; """
                            self.addEventListener('install',function(e){self.skipWaiting();});
                            self.addEventListener('activate',function(e){e.waitUntil(self.clients.claim());});
                            self.addEventListener('fetch',function(e){
                                if(new URL(e.request.url).pathname==='/share-worker/submit'&&e.request.method==='POST'&&e.request.mode==='navigate')
                                e.respondWith(e.request.formData().then(function(data){
                                    var params=new URLSearchParams();['text','title','url'].forEach(function(k){params.set(k,data.get(k)||'');});
                                    params.set('mode',e.request.mode);return Response.redirect(new URL('/share-worker/result?'+params,e.request.url),303);
                                }));
                            });
                        """ }
                        path.startsWith("/share-worker/state?") -> { shareWorkerStates.add(JSONObject(Uri.parse(path).getQueryParameter("data")!!)); "ok" }
                        path.startsWith("/share-worker/result?") -> """<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1">
                            <style>html,body{margin:0;min-height:100%;background:#183650;color:white}</style><body><div id="composer"></div>
                            <script>var params=new URL(location.href).searchParams,result={};['text','title','url','mode'].forEach(function(k){result[k]=params.get(k);});
                            document.getElementById('composer').textContent=result.text;
                            addEventListener('load',function(){fetch('/share-worker/state?data='+encodeURIComponent(JSON.stringify(result)));});</script>"""
                        path.startsWith("/share") -> "<html><head>$layoutScript</head><body>Blank composer</body></html>"
                        path.startsWith("/open-file") -> """
                            <!doctype html><script>
                            launchQueue.setConsumer(function(launch){launch.files[0].getFile().then(function(file){
                            var r=new FileReader();r.onload=function(){var x=new XMLHttpRequest();
                            x.open('GET','/opened?name='+encodeURIComponent(file.name)+'&body='+encodeURIComponent(r.result),true);x.send();};r.readAsText(file);});});
                            </script>
                        """.trimIndent()
                        path.startsWith("/first") -> { firstLoaded.countDown(); "ok" }
                        path.startsWith("/ready") -> { storageReady.countDown(); "ok" }
                        path.startsWith("/restored") -> { restored.countDown(); "ok" }
                        else -> """
                            <html><head><link rel='alternate MANIFEST' href='/manifest.json'></head><body>Device fixture<script>
                            function ping(path){var x=new XMLHttpRequest();x.open('GET',path,true);x.send();}
                            var second=location.search.indexOf('second')>=0;var old=localStorage.getItem('marker');
                            var cookie=document.cookie.indexOf('session=persisted')>=0;
                            localStorage.setItem('marker','persisted');document.cookie='session=persisted; Path=/';
                            var r=indexedDB.open('pwa-test',1);r.onupgradeneeded=function(){r.result.createObjectStore('store');};
                            r.onsuccess=function(){var tx=r.result.transaction('store','readwrite');var st=tx.objectStore('store');
                            var get=st.get('marker');get.onsuccess=function(){var indexed=get.result==='persisted';st.put('persisted','marker');
                            tx.oncomplete=function(){ping('/first');setTimeout(function(){ping('/ready');if(location.pathname==='/soft-auth')ping('/login-ready');},1000);
                            if(second&&old==='persisted'&&cookie&&indexed)ping('/restored');};};};
                            </script></body></html>
                        """.trimIndent()
                    }
                    val bytes = binary ?: body.toByteArray(Charsets.UTF_8)
                    val redirect = location?.let { "Location: $it\r\n" }.orEmpty() + responseCookie
                    val challenge = if (status == 401 && path.startsWith("/basic")) "WWW-Authenticate: Basic realm=\"fixture\"\r\n" else ""
                    val policy = if (path.startsWith("/custom-content") && InstrumentationRegistry.getArguments().getString("strictUserContentCsp") == "true")
                        "Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'none'\r\n" else ""
                    client.getOutputStream().apply {
                        val length = if (path == "/unknown-length-filters.txt") "" else "Content-Length: ${bytes.size}\r\n"
                        write("HTTP/1.1 $status OK\r\n${redirect}${challenge}${policy}Content-Type: $type; charset=utf-8\r\n${length}Connection: close\r\n\r\n".toByteArray(Charsets.UTF_8))
                        if (waitForImageCopy) {
                            write(bytes, 0, bytes.size / 2); flush(); imageCopyStarted.countDown()
                            check(continueImageCopy.await(30, TimeUnit.SECONDS))
                            write(bytes, bytes.size / 2, bytes.size - bytes.size / 2)
                        } else if (path == "/slow-filters.txt" || path == "/unknown-length-filters.txt") {
                            val index = if (path == "/slow-filters.txt") 0 else 1
                            write(bytes, 0, bytes.size / 2); flush(); filterFetchStarted[index].countDown()
                            check(continueFilters[index].await(45, TimeUnit.SECONDS))
                            write(bytes, bytes.size / 2, bytes.size - bytes.size / 2)
                        } else if (path == "/slow-manifest.json") {
                            write(bytes, 0, bytes.size / 2); flush(); fetchStarted.countDown()
                            check(continueFetch.await(15, TimeUnit.SECONDS))
                            write(bytes, bytes.size / 2, bytes.size - bytes.size / 2)
                        } else if (path.startsWith("/theme-hold.js?")) {
                            write(bytes, 0, bytes.size / 2); flush()
                            themePageGates[Uri.parse(path).getQueryParameter("case")]?.await(60, TimeUnit.SECONDS)
                            write(bytes, bytes.size / 2, bytes.size - bytes.size / 2)
                        } else if (path == "/slow-page-resource.js") {
                            write(bytes, 0, bytes.size / 2); flush(); pageResourceStarted.countDown()
                            check(continuePage.await(30, TimeUnit.SECONDS))
                            write(bytes, bytes.size / 2, bytes.size - bytes.size / 2)
                        } else write(bytes)
                        flush()
                    }
                }
            } catch (_: Exception) { }
        }
        override fun close() { closed = true; server.close(); worker.shutdownNow() }
    }
}
