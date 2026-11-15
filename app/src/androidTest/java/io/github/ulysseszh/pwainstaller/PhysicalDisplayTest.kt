package io.github.ulysseszh.pwainstaller

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject

/** Opt-in physical-monitor check: -e externalDisplayId ID, then unplug/reconnect. */
@RunWith(AndroidJUnit4::class)
@androidx.test.filters.SdkSuppress(minSdkVersion = 30)
class PhysicalDisplayTest {
    @Test fun retainedWindowUsesTheReconnectedDisplaysDensity() {
        val display = InstrumentationRegistry.getArguments().getString("externalDisplayId")?.toIntOrNull()
        assumeTrue(Build.VERSION.SDK_INT >= 30 && display != null)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val page = """<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1">
            <style>html,body{margin:0;height:100%;box-sizing:border-box;background:#eef6ff}body{border:12px solid #16804a;font:24px sans-serif}
            .corner{position:fixed;width:40px;height:40px;background:#df2b48}.r{right:0}.b{bottom:0}</style>
            <div class="corner"></div><div class="corner r"></div><div class="corner b"></div><div class="corner r b"></div>
            <h1>External display viewport check</h1><p>All four red corners should remain visible.</p><input value="Unsaved text stays here">
            <script>window.probeToken=String(Date.now())+Math.random();</script>""".trimIndent()
        val intent = Intent(context, BrowserTestActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("debug", true).putExtra("url", "data:text/html," + android.net.Uri.encode(page))
        context.startActivity(intent, ActivityOptions.makeBasic().setLaunchDisplayId(requireNotNull(display)).toBundle())
        var screen: BrowserTestActivity? = null
        try {
            val keepFailed = InstrumentationRegistry.getArguments().getString("keepFailedWindow") == "true"
            val deadline = System.currentTimeMillis() + if (keepFailed) 600000 else 180000
            var initialToken: String? = null
            var initialDisplay = -1
            var last = ""
            var stableSince = 0L
            var final = JSONObject()
            var dumped = false
            while (System.currentTimeMillis() < deadline) {
                val done = CountDownLatch(1)
                var nativeDisplay = -1; var density = 0f; var width = 0
                var result: JSONObject? = null
                instrumentation.runOnMainSync {
                    screen = screen ?: ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                        .filterIsInstance<BrowserTestActivity>().singleOrNull()
                    screen?.let { host ->
                        nativeDisplay = host.page.display?.displayId ?: -1
                        density = host.resources.displayMetrics.density; width = host.page.width
                        host.page.evaluateJavascript("JSON.stringify({dpr:devicePixelRatio,width:innerWidth,token:window.probeToken})") { raw ->
                            result = runCatching { JSONObject(org.json.JSONTokener(raw).nextValue() as String) }.getOrNull(); done.countDown()
                        }
                    } ?: done.countDown()
                }
                done.await(2, TimeUnit.SECONDS)
                val value = result
                if (value != null && value.has("token") && width > 0) {
                    if (initialToken == null) { initialToken = value.getString("token"); initialDisplay = nativeDisplay }
                    final = value
                    val state = "display=$nativeDisplay density=$density width=$width css=${value.optInt("width")} dpr=${value.optDouble("dpr")}" 
                    if (state != last) { instrumentation.sendStatus(0, android.os.Bundle().apply { putString("viewport", state) }); last = state; stableSince = System.currentTimeMillis() }
                    if (nativeDisplay > 0 && nativeDisplay != initialDisplay && System.currentTimeMillis() - stableSince > 3000) {
                        if (!dumped) {
                            instrumentation.runOnMainSync { dumpProviderDisplays(requireNotNull(screen)) }
                            dumped = true
                        }
                        if (keepFailed && kotlin.math.abs(density - value.getDouble("dpr")) > .01) { Thread.sleep(500); continue }
                        assertEquals("The retained document must survive", initialToken, value.getString("token"))
                        assertEquals("WebView must use the external display density", density.toDouble(), value.getDouble("dpr"), 0.01)
                        assertEquals("CSS viewport must match the actual Android view", width / density.toDouble(), value.getDouble("width"), 2.0)
                        return
                    }
                }
                Thread.sleep(200)
            }
            fail("No successful physical-display reconnection: $last $final")
        } finally { instrumentation.runOnMainSync { screen?.finish() } }
    }
    private fun dumpProviderDisplays(host: BrowserTestActivity) {
        val messages = mutableListOf<String>()
        val seen = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Any, Boolean>())
        fun visit(value: Any?, path: String, depth: Int) {
            if (value == null || depth > 5 || seen.size > 200 || !seen.add(value)) return
            when (value) {
                is android.content.Context -> messages.add("$path ${value.javaClass.name} display=${runCatching { value.display?.displayId }.getOrNull()} dpi=${value.resources.configuration.densityDpi}")
                is android.view.Display -> messages.add("$path Display id=${value.displayId} valid=${value.isValid}")
                is java.lang.ref.Reference<*> -> visit(value.get(), path + "->ref", depth + 1)
                is Float -> messages.add("$path $value")
                else -> if (value.javaClass.name.startsWith("WV.") || value.javaClass.name.startsWith("org.chromium.")) {
                    var type: Class<*>? = value.javaClass
                    while (type != null && type != Any::class.java) {
                        type.declaredFields.filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }.forEach { field ->
                            runCatching { field.isAccessible = true; visit(field.get(value), "$path.${field.name}:${field.type.simpleName}", depth + 1) }
                        }
                        type = type.superclass
                    }
                }
            }
        }
        val browser = io.github.ulysseszh.pwainstaller.browser.BrowserActivity::class.java
        val configuration = browser.getDeclaredField("browserConfiguration").apply { isAccessible = true }.get(host)
        val callbacks = configuration.javaClass.getDeclaredField("callbacks").apply { isAccessible = true }.get(configuration) as Map<*, *>
        callbacks.keys.forEach { visit(it, it!!.javaClass.name, 0) }
        val display = host.page.display
        if (display != null) {
            val context = host.applicationContext.createWindowContext(display, android.view.WindowManager.LayoutParams.TYPE_APPLICATION, null)
            messages.add("fresh WindowContext display=${context.display?.displayId} dpi=${context.resources.configuration.densityDpi}")
        }
        InstrumentationRegistry.getInstrumentation().sendStatus(0, android.os.Bundle().apply { putString("providerDisplays", messages.joinToString("\n")) })
    }
}
