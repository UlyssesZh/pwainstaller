package io.github.ulysseszh.pwainstaller.browser

import android.webkit.CookieManager
import io.github.ulysseszh.pwainstaller.core.PackageNames
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** Resolve HTTP redirects without loading an outside page or running its scripts. */
internal object BrowserRedirects {
	fun resolve(source: String, page: String, agent: String, headerOrigin: String, headers: Map<String, String>, inside: (String) -> Boolean): String {
		var url = PackageNames.webUrl(source)
		var authorization: String? = null
		val visited = mutableSetOf<String>()
		val authenticated = mutableSetOf<String>()
		repeat(20) {
			if (Thread.currentThread().isInterrupted) throw InterruptedException()
			if (inside(url)) return url
			if (!visited.add(url)) return url
			val connection = URL(url).openConnection() as HttpURLConnection
			connection.instanceFollowRedirects = false
			connection.connectTimeout = 10000; connection.readTimeout = 10000
			connection.setRequestProperty("User-Agent", agent)
			if (PackageNames.sameOrigin(headerOrigin, url)) {
				for ((key, value) in headers) connection.setRequestProperty(key, value)
			}
			if (PackageNames.sameOrigin(page, url)) connection.setRequestProperty("Referer", page)
			CookieManager.getInstance().getCookie(url)?.let { connection.setRequestProperty("Cookie", it) }
			authorization?.let { connection.setRequestProperty("Authorization", it) }
			try {
				val status = connection.responseCode
				connection.headerFields.filterKeys { it.equals("Set-Cookie", true) }.values.flatten().forEach {
					CookieManager.getInstance().setCookie(url, it)
				}
				if (status == 401 && authenticated.add(url)) {
					val realm = Regex("(?i)\\bBasic\\s+realm=\"([^\"]*)\"").find(connection.getHeaderField("WWW-Authenticate").orEmpty())?.groupValues?.get(1)
					authorization = realm?.let { BrowserCredentials.authorization(url, it) }
					if (authorization != null) { visited.remove(url); return@repeat }
				}
				if (status !in listOf(301, 302, 303, 307, 308)) return url
				val location = connection.getHeaderField("Location") ?: return url
				val next = URI(url).resolve(location).toString() +
					if ('#' !in location) URI(url).rawFragment?.let { "#$it" }.orEmpty() else ""
				if (URI(next).scheme?.lowercase() !in listOf("http", "https")) return next
				val normalized = PackageNames.webUrl(next)
				// Let the system browser handle a downgrade rather than probing it.
				if (url.startsWith("https:") && normalized.startsWith("http:")) return normalized
				url = normalized; authorization = null
			} catch (failure: Exception) {
				if (Thread.currentThread().isInterrupted) throw InterruptedException()
				// Keep redirects already resolved when the last server cannot be probed.
				return url
			} finally { connection.disconnect() }
		}
		return url
	}
}
