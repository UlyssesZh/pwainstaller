package io.github.ulysseszh.pwainstaller.core

import java.net.URI
import java.security.MessageDigest
import java.util.Locale

object PackageNames {
	const val INSTALLER = "io.github.ulysseszh.pwainstaller"

	@JvmStatic fun digest(algorithm: String, bytes: ByteArray): String =
		MessageDigest.getInstance(algorithm).digest(bytes).joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 255) }

	@JvmStatic fun forId(id: String): String {
		val hash = digest("SHA-1", id.toByteArray(Charsets.UTF_8))
		return "$INSTALLER.p_$hash"
	}

	@JvmStatic fun valid(name: String?): Boolean = name != null && name.length <= 223 &&
		name != INSTALLER && name.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))

	@JvmStatic fun webUrl(value: String): String {
		val uri = URI(value.trim()).normalize()
		require(uri.scheme?.lowercase(Locale.ROOT) in listOf("https", "http") && uri.host != null && uri.userInfo == null) {
			"Expected an HTTP(S) URL without embedded credentials"
		}
		val scheme = uri.scheme.lowercase(Locale.ROOT)
		var path = uri.rawPath.orEmpty().ifEmpty { "/" }
		// java.net.URI retains parents above the root; the web URL standard clamps them.
		while (path.startsWith("/../")) path = path.removePrefix("/..")
		if (path == "/..") path = "/"
		val port = if (uri.port == -1 || (scheme == "https" && uri.port == 443) || (scheme == "http" && uri.port == 80)) "" else ":${uri.port}"
		val query = uri.rawQuery?.let { "?$it" }.orEmpty()
		val fragment = uri.rawFragment?.let { "#$it" }.orEmpty()
		return URI("$scheme://${uri.host.lowercase(Locale.ROOT)}$port$path$query$fragment").toASCIIString()
	}

	@JvmStatic fun resolveId(start: String, id: String?): String = withoutFragment(
		if (id.isNullOrEmpty()) start else webUrl(URI(start).resolve("/").resolve(id).toString())
	)

	@JvmStatic fun withoutFragment(value: String): String = value.substringBefore('#')

	@JvmStatic fun sameOrigin(a: String, b: String): Boolean {
		val x = URI(a)
		val y = URI(b)
		return x.scheme.equals(y.scheme, true) && x.host.equals(y.host, true) && port(x) == port(y)
	}

	private fun port(uri: URI) = if (uri.port >= 0) uri.port else if (uri.scheme.equals("https", true)) 443 else 80
}
