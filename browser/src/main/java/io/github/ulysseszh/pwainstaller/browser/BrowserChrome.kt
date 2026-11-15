package io.github.ulysseszh.pwainstaller.browser

import android.app.Activity
import android.view.View
import android.webkit.WebView

/** Implemented only by the optional Material runtime; the small runtime has no UI dependency. */
interface BrowserChrome {
	val view: View
	fun update(url: String)
	fun colors(background: Int, foreground: Int)
}
interface BrowserActions {
	fun webView(): WebView
	fun back()
	fun navigate(url: String)
	fun share()
	fun copy()
	fun download()
	fun print()
}
interface BrowserChromeFactory {
	fun create(activity: Activity, actions: BrowserActions): BrowserChrome
}
