package io.github.ulysseszh.pwainstaller.browser

import android.util.Base64
import java.net.URI
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Session-only HTTP credentials, isolated by scheme, host, port, and authentication realm. */
object BrowserCredentials {
	private val values = ConcurrentHashMap<Pair<String, String>, String>()
	internal fun origin(url: String): String {
		val uri = URI(url)
		require(uri.scheme in listOf("http", "https") && uri.host != null)
		val port = if (uri.port != -1) uri.port else if (uri.scheme == "https") 443 else 80
		return "${uri.scheme}://${uri.host.lowercase(Locale.ROOT)}:$port"
	}
	fun remember(url: String, realm: String, username: String, password: String) {
		val encoded = Base64.encodeToString("$username:$password".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
		values[origin(url) to realm] = "Basic $encoded"
	}
	fun authorization(url: String, realm: String): String? = values[origin(url) to realm]
}
