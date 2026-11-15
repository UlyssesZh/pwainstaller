package io.github.ulysseszh.pwainstaller.browser

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import io.github.ulysseszh.pwainstaller.core.DisplayMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** CSSOM cannot read opaque cross-origin stylesheets. Translate those at the resource boundary. */
internal object DisplayModeStyles {
    fun intercept(request: WebResourceRequest, mode: String, pageUrl: String): WebResourceResponse? {
        if (request.isForMainFrame || request.method != "GET" || request.url.scheme !in listOf("http", "https")) return null
        val headers = request.requestHeaders.mapKeys { it.key.lowercase(java.util.Locale.ROOT) }
        if (headers["sec-fetch-dest"] != "style" && !headers["accept"].orEmpty().contains("text/css") &&
            !request.url.path.orEmpty().endsWith(".css", true)) return null
        val connection = URL(request.url.toString()).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 8000; connection.readTimeout = 10000
            connection.instanceFollowRedirects = false
            headers.filterKeys { it !in setOf("host", "connection", "content-length", "accept-encoding", "if-none-match", "if-modified-since") }
                .forEach { (name, value) -> connection.setRequestProperty(name, value) }
            connection.setRequestProperty("Accept-Encoding", "identity")
            if (!headers.containsKey("cookie") && runCatching {
                io.github.ulysseszh.pwainstaller.core.PackageNames.sameOrigin(pageUrl, request.url.toString())
            }.getOrDefault(false)) CookieManager.getInstance().getCookie(request.url.toString())?.let { connection.setRequestProperty("Cookie", it) }
            // Let WebView handle redirects and authentication with its normal network stack.
            if (connection.responseCode != 200 || !connection.contentType.orEmpty().startsWith("text/css", true)) return null
            val charsetName = Regex("(?i)charset\\s*=\\s*[\"']?([^;\\s\"']+)").find(connection.contentType.orEmpty())?.groupValues?.get(1) ?: "UTF-8"
            val charset = charset(charsetName)
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(16384)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 4 * 1024 * 1024) return null
                    output.write(buffer, 0, count)
                }
            }
            val bytes = DisplayMode.stylesheet(output.toByteArray().toString(charset), mode).toByteArray(charset)
            connection.headerFields.filterKeys { it.equals("Set-Cookie", true) }.values.flatten().forEach {
                CookieManager.getInstance().setCookie(request.url.toString(), it)
            }
            val responseHeaders = connection.headerFields.filterKeys { it != null && it.lowercase(java.util.Locale.ROOT) !in setOf("content-length", "content-encoding", "transfer-encoding", "set-cookie") }
                .mapValues { it.value.joinToString(", ") }
            WebResourceResponse("text/css", charset.name(), 200, "OK", responseHeaders, ByteArrayInputStream(bytes))
        } catch (_: Exception) { null } finally { connection.disconnect() }
    }
}
