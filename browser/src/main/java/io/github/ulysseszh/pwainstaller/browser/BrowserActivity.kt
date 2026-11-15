package io.github.ulysseszh.pwainstaller.browser

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Intent
import android.content.ComponentCallbacks
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.view.*
import android.webkit.*
import android.widget.*
import java.util.Locale
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.ScriptHandler

/** System-WebView browser shared by installed PWAs and interactive authentication. */
open class BrowserActivity : Activity() {
    protected lateinit var web: WebView
    protected lateinit var root: LinearLayout
    protected lateinit var content: FrameLayout
    protected var address: EditText? = null
    private var pageProgress: ProgressBar? = null
    private var progressColor: Int? = null
    private var browserResumed = false
    private var viewportDisplay = Display.DEFAULT_DISPLAY
    private var viewportDensity = 0
    private var browserNightMode = Configuration.UI_MODE_NIGHT_UNDEFINED
    private var viewportNeedsReset = false
    private var viewportRepairScheduled = false
    private var viewportAttachedOnce = false
    private var viewportSequence = 0L
    private data class ViewportPage(val view: WebView, val url: String?, val x: Float, val y: Float)
    private var pausedViewportPages = emptyList<ViewportPage>()
    private fun viewportPages(densityDpi: Int? = null) = (listOf(web) + popups).map { view ->
        var scale = view.scale.takeIf { it.isFinite() && it > 0 } ?: 1f
        // WebView can already report the new display's scale before onPause,
        // while its scroll pixels still belong to the previous rendered display.
        if (densityDpi != null && view.resources.displayMetrics.density > 0)
            scale *= densityDpi / (160f * view.resources.displayMetrics.density)
        ViewportPage(view, view.url, view.scrollX / scale, view.scrollY / scale)
    }
    private val browserConfiguration = BrowserConfiguration(
        { display?.displayId ?: Display.DEFAULT_DISPLAY }, { resources.configuration.densityDpi },
        { oldDensity, change -> withDetachedWebViews(change, oldDensity) })
    private data class DisplayGeometry(val id: Int, val width: Int, val height: Int, val density: Int, val rotation: Int)
    private var viewportGeometry: DisplayGeometry? = null
    private fun geometry(display: Display?): DisplayGeometry? = display?.takeIf { it.isValid }?.let {
        val metrics = android.util.DisplayMetrics()
        it.getRealMetrics(metrics)
        DisplayGeometry(it.displayId, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, it.rotation)
    }
    private var displays: DisplayManager? = null
    private fun withDetachedWebViews(change: () -> Unit, oldDensity: Int? = null) {
        if (!::web.isInitialized) { change(); return }
        val attached = (listOf(web) + popups).mapNotNull { view ->
            (view.parent as? ViewGroup)?.let { parent -> Triple(view, parent, parent.indexOfChild(view)) }
        }
        if (attached.isEmpty()) { change(); return }
        val positions = viewportPages(oldDensity).map { page -> pausedViewportPages.firstOrNull { it.view === page.view && it.url == page.url } ?: page }
        if (oldDensity != null && pausedViewportPages.isEmpty()) pausedViewportPages = positions
        // AwContents removes its density observer from WindowAndroid's CURRENT
        // display on detach. Detach before a context callback changes that display,
        // or the observer remains subscribed to the phone's old display forever.
        attached.forEach { (view, parent, _) -> parent.removeView(view) }
        try { change() }
        finally {
            attached.forEach { (view, parent, index) -> parent.addView(view, index.coerceAtMost(parent.childCount), view.layoutParams) }
            positions.filter { it.x != 0f || it.y != 0f }.forEach { (view, url, x, y) ->
                view.postOnAnimation {
                    if (!isFinishing && !isDestroyed && view.isAttachedToWindow && view.url == url)
                        view.evaluateJavascript("if(location.href===${org.json.JSONObject.quote(url.orEmpty())})window.scrollTo($x,$y);", null)
                }
            }
        }
    }
    private val viewportHandler = Handler(Looper.getMainLooper())
    private val viewportRepair = Runnable {
        viewportRepairScheduled = false
        if (browserResumed && ::root.isInitialized && root.isAttachedToWindow && viewportNeedsReset &&
            root.display?.isValid == true && !root.isLayoutRequested && web.width > 1 && web.height > 1) {
            val sequence = ++viewportSequence
            val pages = viewportPages().map { page ->
                pausedViewportPages.firstOrNull { it.view === page.view && it.url == page.url } ?: page
            }
            // Refresh the context's observers as well as the View. A view-only
            // configuration dispatch leaves Chromium's cached WindowAndroid display stale.
            traceViewport("repair")
            withDetachedWebViews({ browserConfiguration.refresh(resources.configuration) })
            viewportNeedsReset = false
            for ((view, url, x, y) in pages) {
                if (view.width > 1 && view.height > 1) {
                    // Retain the document while rebuilding Chromium's display/compositor
                    // attachment, whose scale can otherwise remain tied to the old display.
                    val parent = view.parent as? ViewGroup
                    if (parent != null) {
                        val index = parent.indexOfChild(view); val parameters = view.layoutParams
                        parent.removeView(view); parent.addView(view, index, parameters)
                    }
                    view.dispatchConfigurationChanged(resources.configuration)
                    // A real size transition makes Chromium recalculate a stale display
                    // viewport. Parent layout restores MATCH_PARENT before drawing.
                    view.layout(view.left, view.top, view.right - 1, view.bottom - 1)
                    view.requestLayout(); view.invalidate()
                    if (x != 0f || y != 0f) {
                        // Reattaching can reset scrolling. Preserve CSS coordinates
                        // across density changes, as well as the live document itself.
                        val scale = view.scale.takeIf { it.isFinite() && it > 0 } ?: 1f
                        view.scrollTo((x * scale).toInt(), (y * scale).toInt())
                        fun restore() {
                            if (!isFinishing && !isDestroyed && browserResumed && fullscreen == null &&
                                sequence == viewportSequence && view.isAttachedToWindow && view.url == url) {
                                view.evaluateJavascript("if(location.href===${org.json.JSONObject.quote(url.orEmpty())})window.scrollTo($x,$y);", null)
                            }
                        }
                        view.postOnAnimation { restore() }
                        if (WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK))
                            WebViewCompat.postVisualStateCallback(view, sequence, object : WebViewCompat.VisualStateCallback {
                                override fun onComplete(requestId: Long) { view.postOnAnimation { restore() } }
                            })
                    }
                }
            }
            content.requestLayout(); root.requestLayout()
            pausedViewportPages = emptyList()
            checkRendererDisplay(sequence, pages)
        }
    }
    private fun checkRendererDisplay(sequence: Long, pages: List<ViewportPage>) {
        if (Build.VERSION.SDK_INT < 30) return
        val expected = resources.displayMetrics.density
        var reconnected = false
        for (page in pages) {
            val view = page.view; val url = page.url
            view.evaluateJavascript("window.devicePixelRatio") { raw ->
                val ratio = raw?.toFloatOrNull() ?: return@evaluateJavascript
                if (reconnected || !ratio.isFinite() || kotlin.math.abs(ratio - expected) < .01f ||
                    sequence != viewportSequence || !browserResumed || isFinishing || isDestroyed ||
                    !view.isAttachedToWindow || view.url != url || root.display?.isValid != true ||
                    kotlin.math.abs(resources.displayMetrics.density - expected) > .01f) return@evaluateJavascript
                val count = browserConfiguration.reconnectRendererDisplays()
                reconnected = count > 0
                if (webContentsDebuggingEnabled()) android.util.Log.d("PwaViewport", "renderer repair expected=$expected actual=$ratio pages=$count")
                if (reconnected) {
                    for (page in pages) {
                        page.view.requestLayout(); page.view.invalidate()
                        page.view.postOnAnimation {
                            if (sequence == viewportSequence && browserResumed && !isFinishing && !isDestroyed &&
                                page.view.isAttachedToWindow && page.view.url == page.url && (page.x != 0f || page.y != 0f))
                                page.view.evaluateJavascript("if(location.href===${org.json.JSONObject.quote(page.url.orEmpty())})window.scrollTo(${page.x},${page.y});", null)
                        }
                    }
                }
            }
        }
    }
    private val displayChanges = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(id: Int) {
            if (id == viewportDisplay || root.display?.displayId != viewportDisplay) refreshBrowserViewport(true)
        }
        override fun onDisplayRemoved(id: Int) {
            if (id == viewportDisplay || id == root.display?.displayId) refreshBrowserViewport(true)
        }
        override fun onDisplayChanged(id: Int) {
            // Video can change refresh rate, HDR or brightness without changing the
            // viewport. Reattaching its WebView for those events tears down surfaces.
            if ((id == viewportDisplay || id == root.display?.displayId) && geometry(root.display) != viewportGeometry)
                refreshBrowserViewport(true)
        }
    }
    private var chooser: ValueCallback<Array<Uri>>? = null
    private var webPermission: PermissionRequest? = null
    private var geoCallback: GeolocationPermissions.Callback? = null
    private var geoOrigin = ""
    private var fullscreen: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var fullscreenContainer: FrameLayout? = null
    private data class FullscreenScroll(val page: WebView, val url: String?, val x: Int, val y: Int, val scale: Float, val sequence: Long)
    private var fullscreenScroll: FullscreenScroll? = null
    private var fullscreenSequence = 0L
    private var restoringFullscreenInsets = false
    private var fullscreenInsetsDeadline = 0L
    private var previousFullscreenFlag = false
    private var previousSystemUi = 0
    private var previousBarsBehavior = 0
    private var previousVisibleBars = 0
    private var filterEngine = io.github.ulysseszh.pwainstaller.core.AdblockRules.Engine("")
    private var filterScript: String? = null
    private var pageScript: String? = null
    private var customScripts = emptyList<Pair<String, String>>()
    private var chrome: BrowserChrome? = null
    private var pageDownload: java.io.File? = null
    private val pageScriptHandlers = mutableListOf<ScriptHandler>()
    private val pageUrls = java.util.concurrent.ConcurrentHashMap<WebView, String>()
    private val launchScripts = mutableListOf<ScriptHandler>()
    protected fun installLaunchScript(view: WebView, script: String, origin: String): Boolean {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return false
        launchScripts.add(WebViewCompat.addDocumentStartJavaScript(view, script, setOf(origin)))
        return true
    }
    private val popups = mutableListOf<WebView>()
    private val redirectWorker = java.util.concurrent.Executors.newFixedThreadPool(2)
    private class Redirect(val url: String) { var task: java.util.concurrent.Future<*>? = null }
    private val redirects = mutableMapOf<WebView, Redirect>()
    private val contextMenus by lazy { BrowserContextMenus(this, { view, url ->
        val uri = Uri.parse(url)
        if (isWeb(uri)) { if (!routeOutside(view, url)) view.loadUrl(url, contextHeaders(url)) }
        else if (url.startsWith("blob:", true) || url.startsWith("data:image/", true)) view.loadUrl(url)
        else external(uri)
    }, ::external, ::contextHeaders, ::createContextMenu, ::error) }

    private fun contextHeaders(url: String): Map<String, String> = if (runCatching {
        io.github.ulysseszh.pwainstaller.core.PackageNames.sameOrigin(initialUrl().orEmpty(), url)
    }.getOrDefault(false)) requestHeaders() else emptyMap()

    protected open fun createContextMenu(title: String, labels: Array<String>, selected: (Int) -> Unit): android.app.Dialog =
        AlertDialog.Builder(this).setTitle(title).setItems(labels) { _, index -> selected(index) }
            .setNegativeButton(android.R.string.cancel, null).create()

    protected open fun initialUrl(): String? = intent.getStringExtra("url")
    protected open fun showBrowserBar() = true
    protected open fun fullscreenDisplayMode() = false
    protected open fun chromeFactory(): BrowserChromeFactory? = null
    protected open fun showPageProgress() = false
    protected open fun pageProgressColor() = Color.rgb(25, 118, 210)
    protected open fun loginMode() = false
    protected open fun userAgent(): String? = intent.getStringExtra("userAgent")?.takeIf { it.isNotBlank() }
    protected open fun requestHeaders(): Map<String, String> = emptyMap()
    protected open fun shouldOpenInWebView(url: String) = true
    protected open fun onBrowserPageReady(view: WebView) = Unit
    protected open fun onBrowserPageStarted(view: WebView) = Unit
    protected open fun onBrowserProgress(view: WebView, progress: Int) = Unit
    protected open fun webContentsDebuggingEnabled() = false
    protected open fun useNotchAreas() = false
    protected open fun canStartBrowser() = true
    protected open fun adblockRules(): String = ""
    protected open fun adblockScript(): String? = null
    protected open fun browserDisplayMode(): String? = null
    protected open fun documentScript(): String? = null
    protected open fun customPageScripts(): List<Pair<String, String>> = emptyList()
    protected open fun loadInitialPage(view: WebView, url: String) { if (!routeOutside(view, url)) view.loadUrl(url, requestHeaders()) }
    protected open fun completed() { CookieManager.getInstance().flush(); setResult(RESULT_OK); finish() }

    override fun getDisplay(): Display? {
        // The activity context moves before ViewRoot adopts its display. Prefer
        // that valid display, falling back to the view if the context still refers
        // to a removed display during reconnection.
        if (Build.VERSION.SDK_INT >= 30) super.getDisplay()?.takeIf { it.isValid }?.let { return it }
        if (::root.isInitialized && root.isAttachedToWindow)
            root.display?.takeIf { it.isValid }?.let { return it }
        return if (Build.VERSION.SDK_INT >= 30) super.getDisplay()?.takeIf { it.isValid }
            else (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
    }

    override fun onCreate(state: Bundle?) {
        applyBrowserColorScheme(resources.configuration)
        super.onCreate(state)
        pageDownload = state?.getString("pageDownload")?.takeIf { it.matches(Regex("page-[0-9a-f-]{36}\\.mht")) }?.let { java.io.File(cacheDir, it) }
        if (!canStartBrowser()) { finish(); return }
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 28) window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = if (!useNotchAreas()) WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
                else if (Build.VERSION.SDK_INT >= 30) WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                else WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        filterEngine = io.github.ulysseszh.pwainstaller.core.AdblockRules.Engine(adblockRules())
        filterScript = adblockScript()
        pageScript = documentScript()
        customScripts = customPageScripts()
        WebView.setWebContentsDebuggingEnabled(webContentsDebuggingEnabled())
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            applyBrowserInsets(view, insets)
        }
        setContentView(root)
        if (immersive()) hideSystemBars()
        root.requestApplyInsets()
        if (Build.VERSION.SDK_INT >= 33) onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT) { onBackPressed() }
        if (showBrowserBar()) root.addView(createBrowserBar())
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        web = BrowserWebView(this).also(::configure)
        WebView.setWebContentsDebuggingEnabled(webContentsDebuggingEnabled())
        content.addView(web, FrameLayout.LayoutParams(-1, -1))
        if (showPageProgress()) {
            pageProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                // Draw into the full 2 dp height without the platform drawable's insets.
                progressColor = pageProgressColor()
                progressDrawable = pageProgressDrawable(progressColor!!)
                progressTintList = null; progressBackgroundTintList = null
                setPadding(0, 0, 0, 0)
                visibility = View.GONE
                contentDescription = getString(R.string.loading_page)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                content.addView(this, FrameLayout.LayoutParams(-1, (2 * resources.displayMetrics.density).toInt().coerceAtLeast(1), Gravity.TOP))
            }
        }
        contextMenus.restore(state)
        viewportDensity = resources.displayMetrics.densityDpi
        displays = (getSystemService(DISPLAY_SERVICE) as DisplayManager).also { it.registerDisplayListener(displayChanges, viewportHandler) }
        root.viewTreeObserver.addOnPreDrawListener {
            // Desktop hotplug may retain identical window bounds. No layout-change
            // callback is delivered when ViewRoot finally adopts the new display.
            val display = root.display
            if (display?.isValid == true && display.displayId != viewportDisplay) refreshBrowserViewport(true)
            true
        }
        root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val display = root.display
            if (display?.isValid == true && (display.displayId != viewportDisplay || geometry(display) != viewportGeometry))
                refreshBrowserViewport(true)
            else scheduleViewportRepair()
        }
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                refreshBrowserViewport(viewportAttachedOnce)
                viewportAttachedOnce = true
            }
            override fun onViewDetachedFromWindow(view: View) {
                viewportNeedsReset = true
                viewportHandler.removeCallbacks(viewportRepair); viewportRepairScheduled = false
            }
        })
        if (state == null || web.restoreState(state) == null) initialUrl()?.let { loadInitialPage(web, it) }
    }
    protected fun setPageProgressColor(color: Int) {
        if (progressColor == color) return
        progressColor = color
        pageProgress?.progressDrawable = pageProgressDrawable(color)
    }
    private fun pageProgressDrawable(color: Int) = android.graphics.drawable.LayerDrawable(arrayOf(
        android.graphics.drawable.ColorDrawable(Color.argb(51, Color.red(color), Color.green(color), Color.blue(color))),
        android.graphics.drawable.ClipDrawable(android.graphics.drawable.ColorDrawable(color), Gravity.LEFT, android.graphics.drawable.ClipDrawable.HORIZONTAL)
    )).apply { setId(0, android.R.id.background); setId(1, android.R.id.progress) }
    private fun refreshBrowserViewport(reset: Boolean = false) {
        if (!::web.isInitialized) return
        viewportNeedsReset = viewportNeedsReset || reset || root.display?.displayId != viewportDisplay
        viewportDisplay = root.display?.displayId ?: viewportDisplay
        viewportGeometry = geometry(root.display) ?: viewportGeometry
        androidx.core.view.ViewCompat.requestApplyInsets(root)
        root.requestLayout(); content.requestLayout(); fullscreenContainer?.requestLayout()
        for (view in listOf(web) + popups) { view.requestLayout(); view.invalidate() }
        scheduleViewportRepair()
    }
    private fun traceViewport(event: String) {
        if (!webContentsDebuggingEnabled()) return
        android.util.Log.d("PwaViewport", "$event view=${root.display?.displayId} context=${if (Build.VERSION.SDK_INT >= 30) super.getDisplay()?.displayId else -1} " +
            "density=${resources.configuration.densityDpi} tracked=$viewportDisplay callbacks=${browserConfiguration.description()}")
    }
    private fun scheduleViewportRepair() {
        if (browserResumed && viewportNeedsReset && !viewportRepairScheduled) {
            viewportRepairScheduled = true
            viewportHandler.postDelayed(viewportRepair, 100)
        }
    }
    override fun registerComponentCallbacks(callback: ComponentCallbacks) {
        super.registerComponentCallbacks(browserConfiguration.add(callback))
    }
    override fun unregisterComponentCallbacks(callback: ComponentCallbacks) {
        super.unregisterComponentCallbacks(browserConfiguration.remove(callback) ?: callback)
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        val changedDensity = viewportDensity != newConfig.densityDpi
        val changedNightMode = browserNightMode != (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        applyBrowserColorScheme(newConfig)
        super.onConfigurationChanged(newConfig)
        if (changedNightMode && ::web.isInitialized) {
            // Refresh renderer preferences after updating isLightTheme. Retain the
            // same document so CSS and MediaQueryList change listeners update live.
            (listOf(web) + popups).forEach { it.dispatchConfigurationChanged(newConfig) }
        }
        viewportDensity = newConfig.densityDpi
        pageProgress?.contentDescription = getString(R.string.loading_page)
        refreshBrowserViewport(changedDensity)
    }
    private fun applyBrowserColorScheme(configuration: Configuration) {
        browserNightMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (Build.VERSION.SDK_INT >= 29) theme.applyStyle(
            if (browserNightMode == Configuration.UI_MODE_NIGHT_YES) R.style.BrowserColorScheme_Dark else R.style.BrowserColorScheme_Light,
            true
        )
    }
    private fun immersive() = fullscreen != null || fullscreenDisplayMode()
    private fun applyBrowserInsets(view: View, insets: androidx.core.view.WindowInsetsCompat): androidx.core.view.WindowInsetsCompat {
        // The fullscreen overlay has its own insets. Keep the document's viewport
        // unchanged beneath it so virtualized feeds retain their scroll anchor.
        if (view === root) {
            if (fullscreen != null) return androidx.core.view.WindowInsetsCompat.CONSUMED
            if (restoringFullscreenInsets && previousVisibleBars != 0 &&
                android.os.SystemClock.uptimeMillis() < fullscreenInsetsDeadline && !insets.isVisible(previousVisibleBars))
                return androidx.core.view.WindowInsetsCompat.CONSUMED
            restoringFullscreenInsets = false
        }
        // Transient bars overlay fullscreen content. Keep desktop captions, keyboards, and
        // the user's cutout preference, without reserving space for hidden navigation bars.
        var types = androidx.core.view.WindowInsetsCompat.Type.ime() or if (immersive())
            androidx.core.view.WindowInsetsCompat.Type.captionBar() else androidx.core.view.WindowInsetsCompat.Type.systemBars()
        if (!useNotchAreas()) types = types or androidx.core.view.WindowInsetsCompat.Type.displayCutout()
        val bars = insets.getInsets(types)
        view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        // Native layout already handles these areas. Forward zeroes instead of
        // consuming the update, so WebView can discard old viewport/cutout bounds
        // after a display move or keyboard transition without adding duplicate padding.
        val system = androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout()
        return androidx.core.view.WindowInsetsCompat.Builder(insets)
            .setInsets(system or androidx.core.view.WindowInsetsCompat.Type.ime(), androidx.core.graphics.Insets.NONE)
            .setInsetsIgnoringVisibility(system, androidx.core.graphics.Insets.NONE)
            .setDisplayCutout(null)
            .build()
    }
    private fun hideSystemBars() {
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars())
        }
        if (::root.isInitialized) androidx.core.view.ViewCompat.requestApplyInsets(root)
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::root.isInitialized && immersive()) hideSystemBars()
        if (hasFocus) refreshBrowserViewport()
    }
    protected open fun createBrowserBar(): View {
    chromeFactory()?.let { factory ->
        chrome = factory.create(this, object : BrowserActions {
            override fun webView() = current()
            override fun back() = onBackPressed()
            override fun navigate(url: String) = this@BrowserActivity.navigate(url)
            override fun share() { current().url?.let { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND)
                .setType("text/plain").putExtra(Intent.EXTRA_TEXT, it).putExtra(Intent.EXTRA_SUBJECT, current().title), getString(R.string.context_share_link))) } }
            override fun copy() { current().url?.let { url ->
                (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText(current().title, url))
                if (Build.VERSION.SDK_INT < 33) Toast.makeText(applicationContext, R.string.context_copied, Toast.LENGTH_SHORT).show()
            } }
            override fun print() {
                val title = current().title?.takeIf(String::isNotBlank) ?: getString(R.string.browser_print_page)
                (getSystemService(PRINT_SERVICE) as android.print.PrintManager).print(title, current().createPrintDocumentAdapter(title), android.print.PrintAttributes.Builder().build())
            }
            override fun download() {
                val file = java.io.File(cacheDir, "page-${java.util.UUID.randomUUID()}.mht")
                current().saveWebArchive(file.absolutePath, false) { path ->
                    if (path == null) { error(java.io.IOException(getString(R.string.browser_download_failed))); return@saveWebArchive }
                    if (isFinishing || isDestroyed) { file.delete(); return@saveWebArchive }
                    pageDownload?.delete(); pageDownload = file
                    startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("multipart/related").addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_TITLE, "page.mht"), 75)
                }
            }
        })
        return requireNotNull(chrome).view
    }
    val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
    bar.addView(button(getString(R.string.back)).apply { setOnClickListener { onBackPressed() } })
    address = EditText(this).apply {
        setSingleLine(); textSize = 14f; inputType = 17; setHint(R.string.address)
        setOnEditorActionListener { _, _, _ -> navigate(text.toString()); true }
    }
    bar.addView(address, LinearLayout.LayoutParams(0, (52 * resources.displayMetrics.density).toInt(), 1f))
    bar.addView(button(getString(R.string.go)).apply { setOnClickListener { navigate(address?.text.toString()) } })
    bar.addView(button("⋮").apply {
        contentDescription = getString(R.string.refresh)
        setOnClickListener {
            PopupMenu(this@BrowserActivity, this).apply {
                menu.add(R.string.refresh).setOnMenuItemClickListener { current().reload(); true }
                menu.add(R.string.open_external).setOnMenuItemClickListener { current().url?.let { external(Uri.parse(it)) }; true }
                show()
            }
        }
    })
    if (loginMode()) bar.addView(button("✓").apply { contentDescription = getString(R.string.done); setOnClickListener { completed() } })
        return bar
    }
    private fun button(label: String) = Button(this).apply { text = label; minWidth = 0; minimumWidth = 0; setPadding(12, 0, 12, 0) }
    protected fun current(): WebView = popups.lastOrNull() ?: web
    protected fun browserBarColors(background: Int, foreground: Int) { chrome?.colors(background, foreground) }
    private fun updatePageProgress(view: WebView, value: Int) {
        if (view !== current()) return
        chrome?.update(view.url.orEmpty())
        pageProgress?.apply {
            progress = value.coerceIn(0, 100)
            visibility = if (value < 100) View.VISIBLE else View.GONE
            bringToFront()
        }
    }
    protected fun navigate(text: String) {
        val value = text.trim().let { if (':' in it) it else "https://$it" }
        val uri = Uri.parse(value)
        if (isWeb(uri)) { if (!routeOutside(current(), value)) current().loadUrl(value) } else external(uri)
    }
    private fun isWeb(uri: Uri) = uri.scheme.equals("http", true) || uri.scheme.equals("https", true)

    private fun injectFilters(view: WebView) {
        for ((marker, script) in customScripts) {
            view.evaluateJavascript("/^https?:$/.test(location.protocol)&&!window[${org.json.JSONObject.quote(marker)}]") { needed ->
                if (needed == "true") view.evaluateJavascript(script, null)
            }
        }
        pageScript?.let { script ->
            view.evaluateJavascript("/^https?:$/.test(location.protocol)&&!window.__pwaDisplayModeInstalled") { needed ->
                if (needed == "true") view.evaluateJavascript(script, null)
            }
        }
        val script = filterScript ?: return
        // Old WebViews can evaluate onPageStarted in the previous document. Retry as the
        // new document loads, without repeatedly parsing the filter bundle once applied.
        view.evaluateJavascript("/^https?:$/.test(location.protocol)&&window.__pwaFilterApplied!==location.href") { needed ->
            if (needed == "true") view.evaluateJavascript(script, null)
        }
    }

    protected open fun configure(view: WebView) {
        contextMenus.attach(view)
        pageUrls[view] = initialUrl().orEmpty()
        view.settings.apply {
            userAgent()?.let { userAgentString = it }
            javaScriptEnabled = true; domStorageEnabled = true; databaseEnabled = true
            allowFileAccess = false; allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(true); javaScriptCanOpenWindowsAutomatically = false
            builtInZoomControls = true; displayZoomControls = false
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_AUTHENTICATION)) {
            androidx.webkit.WebSettingsCompat.setWebAuthenticationSupport(view.settings,
                androidx.webkit.WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_BROWSER)
        }
        CookieManager.getInstance().apply { setAcceptCookie(true); setAcceptThirdPartyCookies(view, true) }
        pageScript?.let { script ->
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT))
                pageScriptHandlers.add(WebViewCompat.addDocumentStartJavaScript(view, script, setOf("*")))
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            for ((_, script) in customScripts)
                pageScriptHandlers.add(WebViewCompat.addDocumentStartJavaScript(view, script, setOf("*")))
        }
        filterScript?.let { script ->
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT))
                WebViewCompat.addDocumentStartJavaScript(view, script, setOf("*"))
        }
        view.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()
                if (!isWeb(request.url)) return null
                val headers = request.requestHeaders.mapKeys { it.key.lowercase(java.util.Locale.ROOT) }
                val page = headers["referer"] ?: pageUrls[view].orEmpty().ifEmpty { url }
                val path = request.url.path.orEmpty().lowercase(java.util.Locale.ROOT)
                val type = when {
                    request.isForMainFrame -> "document"
                    headers["sec-fetch-dest"] == "script" -> "script"
                    headers["sec-fetch-dest"] == "style" -> "stylesheet"
                    headers["sec-fetch-dest"] == "image" -> "image"
                    headers["sec-fetch-dest"] == "font" -> "font"
                    headers["sec-fetch-dest"] in listOf("frame", "iframe") -> "subdocument"
                    headers["x-requested-with"] == "XMLHttpRequest" -> "xmlhttprequest"
                    path.endsWith(".js") || headers["accept"].orEmpty().contains("javascript") -> "script"
                    path.endsWith(".css") -> "stylesheet"
                    Regex("\\.(png|jpe?g|gif|svg|webp|ico|avif)$").containsMatchIn(path) || headers["accept"].orEmpty().startsWith("image/") -> "image"
                    Regex("\\.(woff2?|ttf|otf)$").containsMatchIn(path) -> "font"
                    Regex("\\.(mp4|webm|mp3|ogg|wav)$").containsMatchIn(path) -> "media"
                    path.endsWith(".json") -> "xmlhttprequest"
                    else -> "other"
                }
                if (!filterEngine.blocks(url, page, type)) return browserDisplayMode()?.let { DisplayModeStyles.intercept(request, it, page) }
                return WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", mapOf("Cache-Control" to "no-store"), java.io.ByteArrayInputStream(byteArrayOf()))
            }
            override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, host: String, realm: String) {
                val credentialUrl = listOfNotNull(view.url, view.originalUrl, initialUrl())
                    .firstOrNull { Uri.parse(it).host.equals(host, true) }
                val credentialKey = credentialUrl?.let(BrowserCredentials::origin)
                fun remember(username: String, password: String) {
                    credentialUrl?.let {
                        BrowserCredentials.remember(it, realm, username, password)
                    }
                }
                val saved = credentialKey?.let { view.getHttpAuthUsernamePassword(it, realm) }
                if (handler.useHttpAuthUsernamePassword() && saved?.size == 2) {
                    remember(saved[0], saved[1]); handler.proceed(saved[0], saved[1]); return
                }
                val form = createAuthenticationForm(saved?.getOrNull(0).orEmpty())
                showBrowserDialog(getString(R.string.http_auth, host, realm), getString(R.string.sign_in), getString(android.R.string.cancel),
                    onPositive = {
                        val user = form.username.text.toString(); val secret = form.password.text.toString()
                        credentialKey?.let { view.setHttpAuthUsernamePassword(it, realm, user, secret) }
                        remember(user, secret); handler.proceed(user, secret)
                    }, onNegative = { handler.cancel() }, view = form.view)
            }
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val uri = Uri.parse(url)
                if (isWeb(uri)) return routeOutside(view, url)
                external(uri)
                return true
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                return shouldOverrideUrlLoading(view, request.url.toString())
            }
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                onBrowserPageStarted(view)
                updatePageProgress(view, 0)
                contextMenus.dismiss()
                pageUrls[view] = url
                if (isWeb(Uri.parse(url))) routeOutside(view, url)
                injectFilters(view)
            }
            override fun onPageCommitVisible(view: WebView, url: String) { onBrowserPageReady(view) }
            override fun onPageFinished(view: WebView, url: String) {
                if (view.url == url) updatePageProgress(view, 100)
                injectFilters(view); address?.setText(url); CookieManager.getInstance().flush(); onBrowserPageReady(view); launchScripts.forEach { it.remove() }; launchScripts.clear()
            }
            @Suppress("DEPRECATION")
            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                updatePageProgress(view, 100)
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) updatePageProgress(view, 100)
            }
        }
        view.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) { updatePageProgress(view, newProgress); injectFilters(view); onBrowserProgress(view, newProgress) }
            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                chooser?.onReceiveValue(null)
                chooser = callback
                try { startActivityForResult(params.createIntent(), 71) }
                catch (error: Exception) { chooser?.onReceiveValue(null); chooser = null; error(error) }
                return true
            }
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    if (!trustedOrigin(request.origin.toString(), view)) { request.deny(); return@runOnUiThread }
                    val permissions = mutableListOf<String>()
                    val resources = mutableListOf<String>()
                    request.resources.forEach { resource -> when (resource) {
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> { permissions.add(Manifest.permission.CAMERA); resources.add(getString(R.string.camera)) }
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> { permissions.add(Manifest.permission.RECORD_AUDIO); resources.add(getString(R.string.microphone)) }
                    } }
                    if (permissions.isEmpty()) { request.deny(); return@runOnUiThread }
                    showBrowserDialog(getString(R.string.permission_title), getString(R.string.allow), getString(R.string.deny),
                        onPositive = {
                            if (Build.VERSION.SDK_INT >= 23) {
                                webPermission?.deny(); webPermission = request
                                requestPermissions(permissions.toTypedArray(), 72)
                            } else grantSupported(request)
                        }, onNegative = { request.deny() },
                        message = getString(R.string.permission_message, request.origin, resources.joinToString(", ")))
                }
            }
            override fun onPermissionRequestCanceled(request: PermissionRequest) { if (webPermission === request) webPermission = null }
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                if (!trustedOrigin(origin, view)) { callback.invoke(origin, false, false); return }
                showBrowserDialog(getString(R.string.permission_title), getString(R.string.allow), getString(R.string.deny),
                    onPositive = {
                        if (Build.VERSION.SDK_INT >= 23) {
                            geoOrigin = origin; geoCallback = callback
                            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 73)
                        } else callback.invoke(origin, true, false)
                    }, onNegative = { callback.invoke(origin, false, false) },
                    message = getString(R.string.permission_message, origin, getString(R.string.location)))
            }
            override fun onCreateWindow(view: WebView, dialog: Boolean, gesture: Boolean, result: Message): Boolean {
                if (!gesture) return false
                val popup = BrowserWebView(this@BrowserActivity).also(::configure)
                popups.add(popup)
                content.addView(popup, FrameLayout.LayoutParams(-1, -1))
                (result.obj as WebView.WebViewTransport).webView = popup
                result.sendToTarget()
                return true
            }
            override fun onCloseWindow(window: WebView) {
                if (popups.remove(window)) {
                    contextMenus.dismiss(); content.removeView(window); window.destroy()
                    updatePageProgress(current(), current().progress)
                }
            }
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (fullscreen != null) { callback.onCustomViewHidden(); return }
                restoringFullscreenInsets = false
                current().let { fullscreenScroll = FullscreenScroll(it, it.url, it.scrollX, it.scrollY,
                    it.scale.takeIf { scale -> scale > 0 && scale.isFinite() } ?: resources.displayMetrics.density, ++fullscreenSequence) }
                previousFullscreenFlag = window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0
                previousSystemUi = window.decorView.systemUiVisibility
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                previousBarsBehavior = controller.systemBarsBehavior
                val insets = androidx.core.view.ViewCompat.getRootWindowInsets(root)
                previousVisibleBars = 0
                val status = androidx.core.view.WindowInsetsCompat.Type.statusBars()
                val navigation = androidx.core.view.WindowInsetsCompat.Type.navigationBars()
                if (insets?.isVisible(status) ?: !previousFullscreenFlag) previousVisibleBars = previousVisibleBars or status
                if (insets?.isVisible(navigation) ?: (previousSystemUi and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0)) previousVisibleBars = previousVisibleBars or navigation
                fullscreen = view; fullscreenCallback = callback
                fullscreenContainer = FrameLayout(this@BrowserActivity).apply {
                    setBackgroundColor(Color.BLACK)
                    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { target, applied -> applyBrowserInsets(target, applied) }
                    addView(view, FrameLayout.LayoutParams(-1, -1))
                }.also { overlay ->
                    (window.decorView as ViewGroup).addView(overlay, ViewGroup.LayoutParams(-1, -1))
                    androidx.core.view.ViewCompat.requestApplyInsets(overlay)
                }
                // The opaque overlay covers the page. Hiding the WebView itself
                // sends a visibility change to sites and can reset virtualized feeds.
                view.keepScreenOn = true
                hideSystemBars()
            }
            override fun onHideCustomView() = hideFullscreen()
        }
        view.setDownloadListener { url, agent, disposition, mime, _ ->
            showBrowserDialog(getString(R.string.download), getString(R.string.download), getString(android.R.string.cancel),
                message = URLUtil.guessFileName(url, disposition, mime), onPositive = {
                    if (Build.VERSION.SDK_INT in 23..28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 74)
                    } else try {
                        val request = DownloadManager.Request(Uri.parse(url)).apply {
                            CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
                            addRequestHeader("User-Agent", agent)
                            setMimeType(mime)
                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, disposition, mime))
                        }
                        (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                        Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show()
                    } catch (error: Exception) { error(error) }
                })
        }
    }
    protected data class AuthenticationForm(val view: View, val username: EditText, val password: EditText)
    protected open fun createAuthenticationForm(savedUsername: String): AuthenticationForm {
        val fields = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 8, 32, 8) }
        val username = EditText(this).apply { setHint(R.string.username); setSingleLine(); setText(savedUsername); fields.addView(this) }
        val password = EditText(this).apply { setHint(R.string.password); setSingleLine(); inputType = 129; fields.addView(this) }
        return AuthenticationForm(fields, username, password)
    }
    protected open fun showBrowserDialog(title: String, positive: String, negative: String, onPositive: () -> Unit,
        onNegative: () -> Unit = {}, message: String? = null, view: View? = null) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setView(view)
            .setPositiveButton(positive) { _, _ -> onPositive() }.setNegativeButton(negative) { _, _ -> onNegative() }
            .setOnCancelListener { onNegative() }.show()
    }
    private fun routeOutside(view: WebView, url: String): Boolean {
        if (!isWeb(Uri.parse(url)) || shouldOpenInWebView(url)) {
            redirects.remove(view)?.task?.cancel(true)
            return false
        }
        if (redirects[view]?.url == url) return true
        redirects.remove(view)?.task?.cancel(true)
        view.stopLoading()
        val redirect = Redirect(url)
        redirects[view] = redirect
        val page = view.url.orEmpty().takeIf { isWeb(Uri.parse(it)) } ?: url
        val agent = view.settings.userAgentString
        val origin = initialUrl().orEmpty()
        val headers = requestHeaders()
        redirect.task = redirectWorker.submit {
            val destination = try {
                BrowserRedirects.resolve(url, page, agent, origin, headers, ::shouldOpenInWebView)
            } catch (_: InterruptedException) { return@submit }
            catch (_: Exception) { url } // A system browser may be able to recover a failed request.
            view.post {
                if (isFinishing || isDestroyed || redirects[view] !== redirect) return@post
                redirects.remove(view)
                if (!view.isAttachedToWindow) return@post
                if (isWeb(Uri.parse(destination)) && shouldOpenInWebView(destination)) view.loadUrl(destination, contextHeaders(destination))
                else {
                    val target = Uri.parse(destination)
                    if (target.scheme?.lowercase(Locale.ROOT) !in listOf("file", "content", "javascript", "data", "intent"))
                        try { startActivity(SystemLinks.intent(this, target)) } catch (error: Exception) { error(error) }
                    if (popups.remove(view)) {
                        content.removeView(view); view.destroy(); updatePageProgress(current(), current().progress)
                    }
                    else if (view === web && view.url.isNullOrBlank()) finish()
                }
            }
        }
        return true
    }
    private fun trustedOrigin(origin: String, view: WebView): Boolean {
        val url = view.url ?: return false
        val a = Uri.parse(origin)
        val b = Uri.parse(url)
        return a.scheme == "https" && a.host == b.host && effectivePort(a) == effectivePort(b)
    }
    private fun effectivePort(uri: Uri) = if (uri.port != -1) uri.port else if (uri.scheme == "https") 443 else 80
    private fun grantSupported(request: PermissionRequest) {
        val allowed = request.resources.filter { it == PermissionRequest.RESOURCE_VIDEO_CAPTURE || it == PermissionRequest.RESOURCE_AUDIO_CAPTURE }
        if (trustedOrigin(request.origin.toString(), current())) request.grant(allowed.toTypedArray()) else request.deny()
    }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (code == 72) webPermission?.let { request ->
            if (results.isNotEmpty() && results.all { it == PackageManager.PERMISSION_GRANTED }) grantSupported(request) else request.deny()
            webPermission = null
        }
        if (code == 73) geoCallback?.let {
            it.invoke(geoOrigin, results.any { value -> value == PackageManager.PERMISSION_GRANTED } && trustedOrigin(geoOrigin, current()), false)
            geoCallback = null
        }
    }
    protected fun external(uri: Uri) {
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return
        if (scheme in listOf("file", "content", "javascript", "data", "intent")) return
        try { startActivity(if (isWeb(uri)) SystemBrowser.intent(this, uri) else Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)) } catch (error: Exception) { error(error) }
    }
    protected fun error(error: Exception) { Toast.makeText(this, getString(R.string.error, error.message), Toast.LENGTH_LONG).show() }
    private fun hideFullscreen() {
        val view = fullscreen ?: return
        val callback = fullscreenCallback
        val scroll = fullscreenScroll; fullscreenScroll = null
        restoringFullscreenInsets = !fullscreenDisplayMode(); fullscreenInsetsDeadline = android.os.SystemClock.uptimeMillis() + 1000
        fullscreen = null; fullscreenCallback = null
        view.keepScreenOn = false
        fullscreenContainer?.let { overlay -> overlay.removeView(view); (overlay.parent as? ViewGroup)?.removeView(overlay) }
        fullscreenContainer = null
        root.visibility = View.VISIBLE
        window.decorView.systemUiVisibility = previousSystemUi
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = previousBarsBehavior
            show(previousVisibleBars)
            hide((androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars()) and previousVisibleBars.inv())
        }
        if (previousFullscreenFlag) window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (fullscreenDisplayMode()) hideSystemBars()
        androidx.core.view.ViewCompat.requestApplyInsets(root)
        callback?.onCustomViewHidden()
        scroll?.let { saved ->
            fun valid() = !isFinishing && !isDestroyed && fullscreen == null && saved.sequence == fullscreenSequence && saved.page === current() && saved.page.url == saved.url
            fun restore() {
                if (valid()) saved.page.scrollTo(saved.x, saved.y)
            }
            fun restoreDocument() {
                if (valid()) {
                    restore()
                    // Chromium can publish its old fullscreen scroll offset after the
                    // native view has restored. Restore in the document after its exit
                    // frame is committed as well, using CSS rather than device pixels.
                    // WebView.getScale() can still report the previous scale while
                    // fullscreen changes commit. Derive the current CSS/device ratio
                    // from the document's density and visual viewport zoom.
                    saved.page.evaluateJavascript("""(function(){
                        var url=${org.json.JSONObject.quote(saved.url.orEmpty())};
                        if(location.href!==url)return;
                        if(window.__pwaFullscreenScrollCleanup)window.__pwaFullscreenScrollCleanup();
                        var active=true,timer;
                        var inputs=['touchstart','pointerdown','mousedown','wheel','keydown'];
                        function cleanup(){
                            active=false;clearTimeout(timer);
                            window.removeEventListener('scroll',restore,true);
                            window.removeEventListener('resize',restore,true);
                            document.removeEventListener('fullscreenchange',changed,true);
                            document.removeEventListener('webkitfullscreenchange',changed,true);
                            inputs.forEach(function(e){window.removeEventListener(e,cleanup,true);});
                        }
                        function restore(){
                            if(!active)return;
                            if(location.href!==url){cleanup();return;}
                            if(document.fullscreenElement||document.webkitFullscreenElement)return;
                            var width=window.visualViewport&&window.visualViewport.width||window.innerWidth;
                            var scale=window.devicePixelRatio*(window.visualViewport?window.visualViewport.scale:1);
                            if(!(scale>0))scale=width>0?${saved.page.width}/width:${saved.scale};
                            if(scale>0){
                                var x=${saved.x}/scale,y=${saved.y}/scale;
                                if(Math.abs(window.scrollX-x)>.5||Math.abs(window.scrollY-y)>.5)window.scrollTo(x,y);
                            }
                        }
                        function changed(){
                            if(document.fullscreenElement||document.webkitFullscreenElement){cleanup();return;}
                            requestAnimationFrame(function(){restore();requestAnimationFrame(restore);});
                        }
                        window.__pwaFullscreenScrollCleanup=cleanup;
                        window.addEventListener('scroll',restore,true);
                        window.addEventListener('resize',restore,true);
                        document.addEventListener('fullscreenchange',changed,true);
                        document.addEventListener('webkitfullscreenchange',changed,true);
                        inputs.forEach(function(e){window.addEventListener(e,cleanup,true);});
                        timer=setTimeout(cleanup,1000);
                        restore();requestAnimationFrame(function(){restore();requestAnimationFrame(restore);});
                    })();""".trimIndent(), null)
                }
            }
            restore()
            root.postOnAnimation { restore(); root.postOnAnimation { restore() } }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK)) {
                WebViewCompat.postVisualStateCallback(saved.page, saved.sequence, object : WebViewCompat.VisualStateCallback {
                    override fun onComplete(requestId: Long) { root.postOnAnimation { restoreDocument() } }
                })
            } else root.postDelayed({ restoreDocument() }, 120)
        }
    }
    override fun onActivityResult(code: Int, result: Int, data: Intent?) {
        super.onActivityResult(code, result, data)
        if (code == 75) {
            val file = pageDownload; pageDownload = null
            if (file != null && result == RESULT_OK && data?.data != null) redirectWorker.execute {
                try {
                    requireNotNull(contentResolver.openOutputStream(data.data!!, "wt")).use { output -> file.inputStream().use { it.copyTo(output) } }
                    runOnUiThread { Toast.makeText(applicationContext, R.string.browser_page_saved, Toast.LENGTH_SHORT).show() }
                } catch (failure: Exception) { runOnUiThread { error(failure) } }
                finally { file.delete() }
            } else file?.delete()
            return
        }
        contextMenus.activityResult(code, result, data)
        if (code == 71) { chooser?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data)); chooser = null }
    }
    @Deprecated("Uses a registered OnBackInvokedCallback on API 33+")
    override fun onBackPressed() {
        redirects.remove(current())?.let { it.task?.cancel(true); return }
        when {
            fullscreen != null -> hideFullscreen()
            current().canGoBack() -> current().goBack()
            popups.isNotEmpty() -> popups.removeAt(popups.lastIndex).let {
                content.removeView(it); it.destroy(); updatePageProgress(current(), current().progress)
            }
            else -> super.onBackPressed()
        }
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("pageDownload", pageDownload?.name); if (::web.isInitialized) web.saveState(outState); contextMenus.save(outState); super.onSaveInstanceState(outState) }
    override fun onPause() {
        // Capture before a moved activity receives its new density. Dividing an
        // old pixel offset by that new density would restore a different CSS position.
        if (::web.isInitialized && pausedViewportPages.isEmpty()) pausedViewportPages = viewportPages(viewportDensity.takeIf { it > 0 })
        browserResumed = false; viewportHandler.removeCallbacks(viewportRepair); viewportRepairScheduled = false
        super.onPause()
        if (::web.isInitialized) { web.onPause(); CookieManager.getInstance().flush() }
    }
    override fun onResume() {
        super.onResume(); browserResumed = true
        if (::root.isInitialized && immersive()) hideSystemBars()
        if (::web.isInitialized) {
            traceViewport("resume")
            if (root.isAttachedToWindow && root.display?.isValid == true)
                browserConfiguration.refresh(resources.configuration)
            web.onResume()
            refreshBrowserViewport(root.display?.displayId != viewportDisplay)
            if (!viewportNeedsReset) pausedViewportPages = emptyList()
        }
    }
    override fun onDestroy() {
        displays?.unregisterDisplayListener(displayChanges); viewportHandler.removeCallbacksAndMessages(null)
        redirects.values.forEach { it.task?.cancel(true) }; redirects.clear(); redirectWorker.shutdownNow()
        contextMenus.destroy()
        hideFullscreen()
        pageScriptHandlers.forEach { it.remove() }; pageScriptHandlers.clear()
        pageUrls.clear()
        chooser?.onReceiveValue(null); webPermission?.deny(); popups.forEach { it.destroy() }
        if (::web.isInitialized) { content.removeView(web); web.destroy() }
        browserConfiguration.clear { super.unregisterComponentCallbacks(it) }
        super.onDestroy()
    }
}
