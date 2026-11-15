package io.github.ulysseszh.pwainstaller.runtime

import android.content.Intent
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Gravity
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import io.github.ulysseszh.pwainstaller.browser.BrowserActivity
import io.github.ulysseszh.pwainstaller.core.PackageNames
import io.github.ulysseszh.pwainstaller.core.CssColors
import io.github.ulysseszh.pwainstaller.core.UrlRules
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.io.RandomAccessFile
import java.net.URI
import java.nio.channels.FileLock

class WebActivity : BrowserActivity() {
	private var launch: JSONObject? = null
	private var splash: FrameLayout? = null
	private var config: JSONObject? = null
	private var lockFile: RandomAccessFile? = null
	private var profileLock: FileLock? = null
	private val colorHandler = Handler(Looper.getMainLooper())
	private var resumed = false
	private var checkingColor = false
	private var colorStarted = 0L
	private var navigationBackground: Int? = null
	private val colorReady = mutableSetOf<WebView>()
	private var pageBackground = Color.WHITE
	private var appThemeColor: Int? = null
	private var pageThemeColor: Int? = null
	private var themeView: WebView? = null
	private val pageColorsScript by lazy { assets.open("page-colors.js").bufferedReader().use { it.readText() } }
	private var statusBackground: Int? = null
	private val colorPoll = object : Runnable {
		override fun run() { refreshPageColor(); if (resumed) colorHandler.postDelayed(this, 100) }
	}
	override fun adblockRules(): String = config?.optString("adblockRules").orEmpty()
	override fun adblockScript(): String? = runCatching { assets.open("adblock.js").bufferedReader().use { it.readText() } }.getOrNull()
	override fun webContentsDebuggingEnabled() = config?.optBoolean("webViewDebugging", false) ?: false
	override fun useNotchAreas() = config?.optBoolean("useNotchAreas", true) ?: true
	override fun browserDisplayMode() = display()
	override fun fullscreenDisplayMode() = display() == "fullscreen"
	override fun documentScript() = assets.open("display-mode.js").bufferedReader().use { it.readText() }
		.replace("__PWA_DISPLAY_MODE__", JSONObject.quote(browserDisplayMode()))
	override fun customPageScripts(): List<Pair<String, String>> {
		val files = config?.optJSONArray("userContent") ?: return emptyList()
		return (0 until files.length()).map { index ->
			val file = files.getJSONObject(index)
			val path = file.getString("asset")
			require(path.matches(Regex("user-content/[0-9]{1,2}\\.js")))
			file.getString("marker") to assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
		}
	}
	override fun canStartBrowser() = profileLock != null
	override fun pageProgressColor() = pageThemeColor ?: appThemeColor ?: super.pageProgressColor()

	override fun onCreate(state: Bundle?) {
		try {
			config = Config.read(this)
			readAppColors()
			readLaunch(intent)
			lockFile = RandomAccessFile(File(filesDir, "profile.lock"), "rw")
			profileLock = lockFile!!.channel.tryLock(0, Long.MAX_VALUE, true)
			check(profileLock != null) { "Profile transfer in progress; reopen the app when it finishes" }
		} catch (error: Exception) { super.onCreate(state); error(error); finish(); return }
		super.onCreate(state)
		if (isFinishing) return
		val metadata = config ?: return
		val background = pageBackground
		root.background = object : android.graphics.drawable.Drawable() {
			private val paint = android.graphics.Paint()
			override fun draw(canvas: android.graphics.Canvas) {
				canvas.drawColor(statusBackground ?: pageBackground)
				paint.color = navigationBackground ?: pageBackground
				canvas.drawRect(0f, (bounds.bottom - root.paddingBottom).toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), paint)
				if (root.paddingLeft > 0) canvas.drawRect(0f, 0f, root.paddingLeft.toFloat(), bounds.bottom.toFloat(), paint)
				if (root.paddingRight > 0) canvas.drawRect((bounds.right - root.paddingRight).toFloat(), 0f, bounds.right.toFloat(), bounds.bottom.toFloat(), paint)
			}
			override fun setAlpha(alpha: Int) = Unit
			override fun setColorFilter(filter: android.graphics.ColorFilter?) = Unit
			@Suppress("DEPRECATION") override fun getOpacity() = android.graphics.PixelFormat.OPAQUE
		}
		setStatusBackground(background)
		setNavigationBackground(background)
		content.setBackgroundColor(background)
		web.setBackgroundColor(background)
		window.setBackgroundDrawableResource(R.color.web_background)
		splash = FrameLayout(this).apply {
			setBackgroundColor(background)
			val size = (resources.displayMetrics.density * 144).toInt()
			addView(ImageView(this@WebActivity).apply {
				setImageResource(R.drawable.web_icon)
				scaleType = ImageView.ScaleType.FIT_CENTER
				contentDescription = metadata.optString("name")
			}, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
			content.addView(this, FrameLayout.LayoutParams(-1, -1))
		}
		requestedOrientation = when (metadata.optString("orientation")) {
			"any" -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
			"portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
			"portrait-primary" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
			"portrait-secondary" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
			"landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
			"landscape-primary" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
			"landscape-secondary" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
			"natural" -> ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
			else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
		}
		root.textDirection = when (metadata.optString("dir")) { "rtl" -> View.TEXT_DIRECTION_RTL; "ltr" -> View.TEXT_DIRECTION_LTR; else -> View.TEXT_DIRECTION_LOCALE }
		if (Build.VERSION.SDK_INT >= 25) updateShortcuts(metadata)
	}
	private fun readAppColors() {
		val metadata = config ?: return
		val colors = io.github.ulysseszh.pwainstaller.core.WebAppColors.resolve(
			metadata.optString("theme_color"), metadata.optString("background_color"),
			metadata.optString("darkThemeColor"), metadata.optString("darkBackgroundColor"),
			resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
		appThemeColor = colors.theme; pageBackground = colors.background
	}
	override fun configure(view: WebView) { super.configure(view); view.setBackgroundColor(pageBackground) }
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		if (config == null || isFinishing) return
		val previousTheme = appThemeColor; val previousBackground = pageBackground
		readAppColors()
		if (previousTheme != appThemeColor || previousBackground != pageBackground) {
			content.setBackgroundColor(pageBackground)
			web.setBackgroundColor(pageBackground); current().setBackgroundColor(pageBackground)
			splash?.setBackgroundColor(pageBackground)
			window.setBackgroundDrawableResource(R.color.web_background)
			setPageProgressColor(pageProgressColor())
			if (showBrowserBar()) setStatusBackground(pageBackground)
			refreshPageColor()
		}
	}
	private fun readLaunch(incoming: Intent) {
		launch = null
		incoming.getStringExtra("launchToken")?.takeIf { it.matches(Regex("[0-9a-f-]{36}")) }?.let { token ->
			val payload = File(cacheDir, "incoming/$token.json")
			if (payload.isFile && payload.length() <= 24 * 1024 * 1024) {
				launch = JSONObject(payload.readText()).also { WebLaunchActivity.scopedAction(config!!, it.getString("url")) }
			}
		}
	}
	override fun onNewIntent(incoming: Intent) {
		super.onNewIntent(incoming)
		setIntent(incoming)
		try {
			readLaunch(incoming)
			if (incoming.data != null || incoming.hasExtra("launchToken")) loadInitialPage(web, initialUrl())
		}
		catch (failure: Exception) { error(failure) }
	}
	override fun onBrowserPageReady(view: WebView) {
		colorReady.add(view)
		if (view === web) { splash?.let(content::removeView); splash = null }
		if (view === current()) refreshPageColor()
	}
	override fun onBrowserPageStarted(view: WebView) {
		colorReady.remove(view)
		if (view === current()) {
			setPageThemeColor(null)
			if (showBrowserBar()) setStatusBackground(pageBackground)
		}
	}
	override fun onBrowserProgress(view: WebView, progress: Int) { if (view === current()) refreshPageColor() }
	override fun onResume() {
		super.onResume(); resumed = true
		if (!isFinishing) { colorHandler.removeCallbacks(colorPoll); colorHandler.post(colorPoll) }
	}
	override fun onPause() { resumed = false; colorHandler.removeCallbacks(colorPoll); super.onPause() }
	private fun refreshPageColor() {
		if (!resumed || isFinishing) return
		val now = android.os.SystemClock.uptimeMillis()
		if (checkingColor && now - colorStarted < 1000) return
		val view = current()
		// Meta tags do not need layout. On old WebViews defer style sampling until
		// parsing finishes, while still allowing the loading bar to pick up metadata.
		val sampleBackgrounds = Build.VERSION.SDK_INT >= 23 || view in colorReady
		if (themeView !== view) { themeView = view; view.setBackgroundColor(pageBackground); setPageThemeColor(null) }
		val url = view.url?.takeUnless { it == "about:blank" } ?: return
		checkingColor = true; colorStarted = now
		view.evaluateJavascript(pageColorsScript.replace("__PWA_SAMPLE_BACKGROUNDS__", sampleBackgrounds.toString())) { result ->
			checkingColor = false
			if (resumed && !isFinishing && !isDestroyed && view === current() && view.url == url) runCatching {
				val colors = JSONArray(result)
				if (colors.optString(0) == url) {
					setPageThemeColor(CssColors.parse(colors.optString(5))?.let(CssColors::opaque))
					val html = CssColors.parse(colors.optString(1)) ?: Color.TRANSPARENT
					val body = CssColors.parse(colors.optString(2)) ?: Color.TRANSPARENT
					val base = composite(body, composite(html, pageBackground))
					fun sample(index: Int): Int {
						var result = base; val layers = colors.optJSONArray(index)
						if (layers != null) repeat(layers.length()) { result = composite(CssColors.parse(layers.optString(it)) ?: Color.TRANSPARENT, result) }
						return result
					}
					setStatusBackground(sample(3))
					setNavigationBackground(sample(4))
				}
			}
		}
	}
	private fun setPageThemeColor(color: Int?) {
		if (pageThemeColor == color) return
		pageThemeColor = color
		setPageProgressColor(pageProgressColor())
	}
	private fun composite(foreground: Int, background: Int): Int {
		val alpha = Color.alpha(foreground)
		fun channel(shift: Int) = (((foreground ushr shift) and 255) * alpha + ((background ushr shift) and 255) * (255 - alpha) + 127) / 255
		return Color.rgb(channel(16), channel(8), channel(0))
	}
	private fun setStatusBackground(pageColor: Int) {
		val color = if (showBrowserBar()) pageThemeColor ?: appThemeColor ?: pageBackground else pageColor
		if (statusBackground != color) {
			statusBackground = color
			window.statusBarColor = color
			// The root also paints behind system bars on Android's edge-to-edge layouts.
			root.background.invalidateSelf()
			browserBarColors(color, CssColors.foreground(color))
		}
		val foreground = CssColors.foreground(color)
		if (Build.VERSION.SDK_INT >= 23) {
			val light = foreground == Color.BLACK
			val flags = window.decorView.systemUiVisibility
			val updated = if (light) flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
			if (updated != flags) window.decorView.systemUiVisibility = updated
		}
	}
	private fun setNavigationBackground(color: Int) {
		val light = CssColors.foreground(color) == Color.BLACK
		// Older Android versions cannot draw dark navigation buttons.
		val painted = if (Build.VERSION.SDK_INT < 26 && light) Color.rgb(40, 40, 40) else color
		if (navigationBackground != painted) {
			navigationBackground = painted
			window.navigationBarColor = painted
			root.background.invalidateSelf()
			if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = true
		}
		if (Build.VERSION.SDK_INT >= 26) {
			val flags = window.decorView.systemUiVisibility
			val updated = if (light) flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
			if (updated != flags) window.decorView.systemUiVisibility = updated
		}
	}
	private fun display(): String {
		val metadata = config ?: return "standalone"
		val overrides = metadata.optJSONArray("display_override")
		return io.github.ulysseszh.pwainstaller.core.DisplayMode.resolve(metadata.optString("display"),
			if (overrides == null) emptyList() else (0 until overrides.length()).map { overrides.optString(it) })
	}
	override fun shouldOpenInWebView(url: String): Boolean {
		val metadata = config ?: return false
		return UrlRules.matches(url, metadata.optString("baseUrl", metadata.optString("start_url")), metadata.optString("urlPatterns"))
	}
	override fun userAgent(): String? = config?.optString("userAgent")?.takeIf { it.isNotBlank() }
	override fun loadInitialPage(view: WebView, url: String) {
		if (!shouldOpenInWebView(url)) { super.loadInitialPage(view, url); return }
		val payload = launch
		if (payload == null) { super.loadInitialPage(view, url); return }
		val target = payload.getString("url")
		val scope = config!!.getString("scope")
		if (payload.getString("kind") == "file") {
			val origin = URI(scope).let { "${it.scheme}://${it.rawAuthority}" }
			if (installLaunchScript(view, LaunchScripts.queue(payload, scope), origin)) view.loadUrl(target)
			else view.loadDataWithBaseURL(target, LaunchScripts.fileBootstrap(payload, scope, getString(R.string.launch_failed)), "text/html", "UTF-8", target)
		} else {
			val fields = payload.getJSONArray("fields")
			val encoded = (0 until fields.length()).joinToString("&") { index ->
				val field = fields.getJSONObject(index)
				java.net.URLEncoder.encode(field.getString("name"), "UTF-8") + "=" + java.net.URLEncoder.encode(field.getString("value"), "UTF-8")
			}
			if (payload.optString("method", "GET") == "GET") {
				val base = target.substringBefore('#')
				val fragment = target.substringAfter('#', "").let { if (it.isEmpty()) "" else "#$it" }
				view.loadUrl(base + (if ('?' in base) "&" else "?") + encoded + fragment)
			} else {
				view.loadDataWithBaseURL(target, LaunchScripts.post(payload, scope, getString(R.string.launch_failed)), "text/html", "UTF-8", target)
			}
		}
	}
	override fun showBrowserBar() = display() == "browser"
	override fun chromeFactory(): io.github.ulysseszh.pwainstaller.browser.BrowserChromeFactory? =
		if (!showBrowserBar()) null else Class.forName("io.github.ulysseszh.pwainstaller.runtime.MaterialBrowserUi")
			.getDeclaredConstructor().newInstance() as io.github.ulysseszh.pwainstaller.browser.BrowserChromeFactory
	override fun showPageProgress() = true
	override fun initialUrl(): String {
		val metadata = config ?: return "about:blank"
		launch?.let { return it.getString("url") }
		intent.dataString?.let { requested ->
			runCatching {
				val url = PackageNames.webUrl(requested)
				return url
			}
		}
		return metadata.optString("start_url", "about:blank")
	}
	@android.annotation.TargetApi(25)
	private fun updateShortcuts(metadata: JSONObject) {
		runCatching {
			val manager = getSystemService(ShortcutManager::class.java)
			val shortcuts = metadata.optJSONArray("shortcuts")
			val items = if (shortcuts == null) emptyList() else (0 until minOf(shortcuts.length(), manager.maxShortcutCountPerActivity)).mapNotNull { index ->
				val item = shortcuts.optJSONObject(index) ?: return@mapNotNull null
				val base = metadata.getString("manifestBase")
				val url = PackageNames.webUrl(URI(base).resolve(item.optString("url")).toString())
				val scope = metadata.getString("scope")
				if (item.optString("name").isBlank() || !PackageNames.sameOrigin(url, scope) || !URI(url).path.startsWith(URI(scope).path)) return@mapNotNull null
				ShortcutInfo.Builder(this, "manifest-$index").setShortLabel(item.optString("short_name", item.getString("name")))
					.setLongLabel(item.getString("name")).setIcon(runCatching {
						assets.open("shortcut-icons/$index.png").use { Icon.createWithBitmap(android.graphics.BitmapFactory.decodeStream(it)) }
					}.getOrElse { Icon.createWithResource(this, R.drawable.web_icon) })
					.setIntent(Intent(this, WebActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse(url))).build()
			}
			manager.dynamicShortcuts = items
		}
	}
	override fun onDestroy() { colorHandler.removeCallbacksAndMessages(null); super.onDestroy(); runCatching { profileLock?.release(); lockFile?.close() } }
}
