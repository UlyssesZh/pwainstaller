package io.github.ulysseszh.pwainstaller.browser

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object SystemBrowser {
    fun intent(context: Context, uri: Uri): Intent {
        val pm = context.packageManager
        fun handlers(url: String) = pm.queryIntentActivities(Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addCategory(Intent.CATEGORY_BROWSABLE), PackageManager.MATCH_DEFAULT_ONLY).map { it.activityInfo.packageName }.toSet()
        val generic = handlers("https://example.invalid/").intersect(handlers("http://browser-probe.invalid/"))
        val browsers = (pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER), 0)
            .map { it.activityInfo.packageName } + generic)
            .filter { it != context.packageName && it != "io.github.ulysseszh.pwainstaller" }.distinct()
        // Resolve a generic web URL so an installed app's deep-link filter cannot intercept it.
        val preferred = pm.resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.invalid/"))
            .addCategory(Intent.CATEGORY_BROWSABLE), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        val view = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        if (preferred in browsers) return view.setPackage(preferred)
        if (browsers.size == 1) return view.setPackage(browsers.single())
        if (browsers.isNotEmpty()) {
            val options = browsers.map { Intent(view).setPackage(it) }
            return Intent.createChooser(options.first(), null).putExtra(Intent.EXTRA_INITIAL_INTENTS, options.drop(1).toTypedArray())
        }
        throw android.content.ActivityNotFoundException("No system browser available")
    }
}
