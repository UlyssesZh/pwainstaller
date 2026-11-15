package io.github.ulysseszh.pwainstaller

/** Debug-only host for lifecycle and rendering regression checks in the app process. */
class BrowserTestActivity : io.github.ulysseszh.pwainstaller.browser.BrowserActivity() {
    val page get() = web
    val pageRoot get() = root
    override fun showBrowserBar() = false
    override fun initialUrl() = intent.getStringExtra("url")
    override fun webContentsDebuggingEnabled() = intent.getBooleanExtra("debug", false)
}
