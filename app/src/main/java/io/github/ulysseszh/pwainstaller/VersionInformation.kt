package io.github.ulysseszh.pwainstaller

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import androidx.webkit.WebViewCompat

object VersionInformation {
    fun collect(context: Context): String {
        val app = context.packageManager.getPackageInfo(context.packageName, 0)
        val unavailable = context.getString(R.string.version_unavailable)
        val webView = runCatching { WebViewCompat.getCurrentWebViewPackage(context) }.getOrNull()
        val webViewVersion = webView?.let { "${it.versionName ?: unavailable} (${PackageInfoCompat.getLongVersionCode(it)})" } ?: unavailable
        return listOfNotNull(
            context.getString(R.string.version_app, context.getString(R.string.app_name), app.versionName ?: unavailable, PackageInfoCompat.getLongVersionCode(app)),
            context.getString(R.string.version_package, context.packageName),
            context.getString(R.string.version_webview, webViewVersion),
            context.getString(R.string.version_webview_package, webView?.packageName ?: unavailable),
            context.getString(R.string.version_device, Build.MANUFACTURER, Build.MODEL, Build.DEVICE),
            context.getString(R.string.version_android, Build.VERSION.RELEASE, Build.VERSION.SDK_INT),
            if (Build.VERSION.SDK_INT >= 23) context.getString(R.string.version_security_patch, Build.VERSION.SECURITY_PATCH.ifBlank { unavailable }) else null,
            context.getString(R.string.version_os_build, Build.DISPLAY),
            context.getString(R.string.version_abis, Build.SUPPORTED_ABIS.joinToString(", "))
        ).joinToString("\n")
    }
}
