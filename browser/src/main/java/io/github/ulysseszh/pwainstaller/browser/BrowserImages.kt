package io.github.ulysseszh.pwainstaller.browser

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import io.github.ulysseszh.pwainstaller.core.PackageNames
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.UUID

/** Bounded image transfers; callers run these methods off the UI thread. */
internal object BrowserImages {
	const val LIMIT = 16 * 1024 * 1024
	data class Image(val file: File, val mime: String)
	fun read(context: Context, source: String, page: String, agent: String, headers: Map<String, String>): Image {
		if (source.startsWith("data:", true)) {
			require(source.length <= LIMIT * 3) { "Image exceeds 16 MiB" }
			val comma = source.indexOf(','); require(comma > 0) { "Invalid image data" }
			val metadata = source.substring(5, comma)
			val bytes = if (metadata.endsWith(";base64", true)) Base64.decode(Uri.decode(source.substring(comma + 1)), Base64.DEFAULT)
				else Uri.decode(source.substring(comma + 1)).toByteArray(Charsets.UTF_8)
			return store(context, bytes, metadata.substringBefore(';'))
		}
		var url = PackageNames.webUrl(source)
		var authorization: String? = null
		val attempted = mutableSetOf<String>()
		repeat(10) {
			val connection = URL(url).openConnection() as HttpURLConnection
			connection.instanceFollowRedirects = false; connection.connectTimeout = 15000; connection.readTimeout = 15000
			connection.setRequestProperty("User-Agent", agent)
			if (PackageNames.sameOrigin(page, url)) {
				for ((key, value) in headers) connection.setRequestProperty(key, value)
				connection.setRequestProperty("Referer", page)
			}
			CookieManager.getInstance().getCookie(url)?.let { connection.setRequestProperty("Cookie", it) }
			authorization?.let { connection.setRequestProperty("Authorization", it) }
			try {
				val status = connection.responseCode
				connection.headerFields.filterKeys { it.equals("Set-Cookie", true) }.values.flatten().forEach {
					CookieManager.getInstance().setCookie(url, it)
				}
				if (status == 401) {
					val realm = Regex("(?i)\\bBasic\\s+realm=\"([^\"]*)\"").find(connection.getHeaderField("WWW-Authenticate").orEmpty())?.groupValues?.get(1)
					val cached = realm?.let { BrowserCredentials.authorization(url, it) }
					if (cached != null && attempted.add(url)) { authorization = cached; return@repeat }
				}
				if (status in 300..399) {
					val target = connection.getHeaderField("Location") ?: throw IOException("Redirect missing Location")
					val next = PackageNames.webUrl(java.net.URI(url).resolve(target).toString())
					require(!url.startsWith("https:") || !next.startsWith("http:")) { "Refusing HTTPS downgrade" }
					url = next; authorization = null
				} else {
					require(status in 200..299) { "HTTP $status" }
					require(connection.contentLengthLongCompat() <= LIMIT) { "Image exceeds 16 MiB" }
					val bytes = connection.inputStream.use { input ->
						val output = ByteArrayOutputStream(); val buffer = ByteArray(32768)
						while (true) {
							val count = input.read(buffer); if (count < 0) break
							require(output.size() + count <= LIMIT) { "Image exceeds 16 MiB" }; output.write(buffer, 0, count)
						}
						output.toByteArray()
					}
					return store(context, bytes, connection.contentType.orEmpty().substringBefore(';'))
				}
			} finally { connection.disconnect() }
		}
		throw IOException("Too many redirects")
	}
	private fun HttpURLConnection.contentLengthLongCompat() = getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
	private fun store(context: Context, bytes: ByteArray, declared: String): Image {
		require(bytes.isNotEmpty() && bytes.size <= LIMIT) { "Invalid image size (maximum 16 MiB)" }
		val mime = declared.trim().lowercase().takeIf { it.startsWith("image/") }
			?: URLConnection.guessContentTypeFromStream(ByteArrayInputStream(bytes))?.takeIf { it.startsWith("image/") }
			?: throw IOException("Not an image")
		val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "img"
		val directory = File(context.cacheDir, "browser-images").apply { check(mkdirs() || isDirectory) }
		// Keep recent clipboard images, while bounding this disposable cache to 64 MiB.
		var total = 0L
		directory.listFiles().orEmpty().sortedByDescending(File::lastModified).forEach { file ->
			total += file.length()
			if (total + bytes.size > 64L * 1024 * 1024 || System.currentTimeMillis() - file.lastModified() > 86400000L) file.delete()
		}
		return Image(File(directory, "${UUID.randomUUID()}.$extension").apply { writeBytes(bytes) }, mime)
	}
}
