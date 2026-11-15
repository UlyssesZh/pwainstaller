package io.github.ulysseszh.pwainstaller

import android.content.Context
import io.github.ulysseszh.pwainstaller.browser.BrowserCredentials
import io.github.ulysseszh.pwainstaller.core.PackageNames
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.*
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale

object ManifestFetcher {
	open class CredentialsRequired(message: String = "Authentication required") : IOException(message)
	class ManifestNotFound : CredentialsRequired("No web app manifest found; sign in to retry, enter a manifest URL, or turn off manifest use")
	class Session(private val context: Context? = null, private val token: String? = null) {
		private val cookies = java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ORIGINAL_SERVER)
		fun cookie(url: String): String? = if (context != null && token != null)
			LoginSessions.call(context, token, "cookie", url).getString("value")
			else cookies.get(URI(url), emptyMap()).filterKeys { it.equals("Cookie", true) }.values.flatten().joinToString("; ").ifEmpty { null }
		fun setCookie(url: String, values: List<String>) {
			if (context != null && token != null) values.forEach { LoginSessions.call(context, token, "setCookie", url, it) }
			else cookies.put(URI(url), mapOf("Set-Cookie" to values))
		}
		fun authorization(url: String, realm: String): String? = if (context != null && token != null)
			LoginSessions.call(context, token, "authorization", url, realm).getString("value")
			else BrowserCredentials.authorization(url, realm)
	}
	data class Response(val bytes: ByteArray, val url: String)

	@JvmStatic fun headers(text: String): Map<String, String> = buildMap {
		text.split(Regex("\\r?\\n")).filter { it.isNotBlank() }.forEach { line ->
			val colon = line.indexOf(':')
			require(colon > 0 && line.substring(0, colon).matches(Regex("[!#$%&'*+.^_`|~0-9A-Za-z-]+")) && !line.contains('\r')) { "Headers must use one Name: value per line" }
			val key = line.substring(0, colon)
			val value = line.substring(colon + 1).trim()
			require(!value.contains('\u0000')) { "Invalid header" }
			require(key.lowercase(Locale.ROOT) !in listOf("host", "content-length", "connection")) { "Restricted HTTP header: $key" }
			put(key, value)
		}
	}

	fun get(location: String, headers: Map<String, String>, limit: Int, credentialOrigin: String, userAgent: String? = null, session: Session = Session(), onProgress: ((Long, Long?) -> Unit)? = null): Response {
		var url = PackageNames.webUrl(location)
		var authorization: String? = null
		val attemptedAuth = mutableSetOf<Pair<String, String>>()
		repeat(10) {
			onProgress?.invoke(0, null)
			val connection = URL(url).openConnection() as HttpURLConnection
			connection.connectTimeout = 15000
			connection.readTimeout = 20000
			connection.instanceFollowRedirects = false
			connection.setRequestProperty("User-Agent", "PWAInstaller/1.0 (Android)")
			if (PackageNames.sameOrigin(credentialOrigin, url)) headers.forEach(connection::setRequestProperty)
			userAgent?.takeIf { it.isNotBlank() }?.let { connection.setRequestProperty("User-Agent", it) }
			authorization?.let { connection.setRequestProperty("Authorization", it) }
			session.cookie(url)?.let { connection.setRequestProperty("Cookie", it) }
			try {
				val status = connection.responseCode
				session.setCookie(url, connection.headerFields.filterKeys { it.equals("Set-Cookie", true) }.values.flatten())
				if (status == 401) {
					val challenge = connection.getHeaderField("WWW-Authenticate").orEmpty()
					val realm = Regex("(?i)\\bBasic\\s+realm=\"([^\"]*)\"").find(challenge)?.groupValues?.get(1)
					val cached = realm?.let { session.authorization(url, it) }
					if (realm != null && cached != null && attemptedAuth.add(url to realm)) {
						authorization = cached
						return@repeat
					}
					throw CredentialsRequired()
				}
				if (status == 403) throw CredentialsRequired()
				if (status in 300..399) {
					val target = connection.getHeaderField("Location") ?: throw IOException("Redirect missing Location")
					val next = PackageNames.webUrl(URI(url).resolve(target).toString())
					if (url.startsWith("https:") && next.startsWith("http:")) throw IOException("Refusing HTTPS downgrade")
					url = next
					authorization = null
				} else {
					if (status !in 200..299) throw IOException("HTTP $status")
					val total = connection.getHeaderField("Content-Length")?.toLongOrNull()?.takeIf { it > 0 }
					return connection.inputStream.use { Response(readProgress(it, limit, total, onProgress), url) }
				}
			} finally { connection.disconnect() }
		}
		throw IOException("Too many redirects")
	}

	@JvmStatic @Throws(IOException::class)
	fun read(input: InputStream, limit: Int): ByteArray = readProgress(input, limit, null, null)
	private fun readProgress(input: InputStream, limit: Int, total: Long?, onProgress: ((Long, Long?) -> Unit)?): ByteArray {
		onProgress?.invoke(0, total)
		val output = ByteArrayOutputStream()
		val buffer = ByteArray(16384)
		while (true) {
			val n = input.read(buffer)
			if (n == -1) break
			if (output.size() + n > limit) throw IOException("File exceeds size limit")
			output.write(buffer, 0, n)
			onProgress?.invoke(output.size().toLong(), total)
		}
		return output.toByteArray()
	}

	@JvmStatic @Throws(Exception::class)
	fun fetch(original: WebApp, session: Session = Session(), validateMetadata: Boolean = true,
		onProgress: (FetchProgress) -> Unit = {}): WebApp {
		val app = original.copy()
		val base = PackageNames.webUrl(app.s("baseUrl"))
		app.set("baseUrl", base)
		val requestHeaders = headers(app.s("headers"))
		var location = app.s("manifestUrl")
		var document: org.jsoup.nodes.Document? = null
		if (location.isEmpty()) {
			val page = get(base, requestHeaders, 4 * 1024 * 1024, base, app.s("userAgent"), session) { received, total -> onProgress(FetchProgress(FetchProgress.Stage.PAGE, received, total)) }
			document = Jsoup.parse(page.bytes.toString(Charsets.UTF_8), page.url)
			val link = document.select("link[rel]").find { "manifest" in it.attr("rel").lowercase(Locale.ROOT).split(Regex("\\s+")) }
			if (link == null) {
				throw ManifestNotFound()
			}
			location = link.absUrl("href").ifEmpty { throw IOException("Empty manifest link") }
		}
		val response = get(location, requestHeaders, 2 * 1024 * 1024, base, app.s("userAgent"), session) { received, total -> onProgress(FetchProgress(FetchProgress.Stage.MANIFEST, received, total)) }
		app.set("manifestUrl", response.url)
		val text = response.bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
		val manifest = try { JSONObject(text) } catch (error: Exception) {
			if (Jsoup.parse(text).select("input[type=password]").isNotEmpty()) throw CredentialsRequired()
			throw error
		}
		val start = runCatching { PackageNames.webUrl(URI(response.url).resolve(manifest.optString("start_url", base)).toString()) }
			.getOrDefault(base).takeIf { PackageNames.sameOrigin(it, base) } ?: base
		manifest.put("start_url", start)
		val id = runCatching { PackageNames.resolveId(start, manifest.optString("id", "")) }
			.getOrDefault(start).takeIf { PackageNames.sameOrigin(it, start) } ?: PackageNames.withoutFragment(start)
		manifest.put("id", id)
		val defaultScope = URI(start).resolve(".").toString()
		val scope = runCatching { PackageNames.webUrl(URI(response.url).resolve(manifest.optString("scope", defaultScope)).toString()) }
			.getOrDefault(defaultScope).takeIf { PackageNames.sameOrigin(it, start) && URI(start).path.startsWith(URI(it).path) } ?: defaultScope
		manifest.put("scope", scope.substringBefore('?').substringBefore('#'))
		resolveArray(manifest.optJSONArray("icons"), "src", response.url)
		resolveArray(manifest.optJSONArray("screenshots"), "src", response.url)
		manifest.optJSONArray("shortcuts")?.let { shortcuts ->
			repeat(shortcuts.length()) { index -> shortcuts.optJSONObject(index)?.let {
				resolve(it, "url", response.url)
				resolveArray(it.optJSONArray("icons"), "src", response.url)
			} }
		}
		app.set("manifest", manifest)
		app.applyManifestScope()
		refreshIcon(app, session, document, onProgress)
		if (validateMetadata) app.validate() else app.validateIdentity()
		return app
	}

	private fun resolve(value: JSONObject, key: String, base: String) {
		if (value.optString(key).isNotEmpty()) value.put(key, URI(base).resolve(value.getString(key)).toString())
	}
	private fun resolveArray(array: JSONArray?, key: String, base: String) {
		if (array != null) repeat(array.length()) { array.optJSONObject(it)?.let { item -> resolve(item, key, base) } }
	}
	fun refreshIcon(app: WebApp, session: Session = Session(), onProgress: (FetchProgress) -> Unit = {}) = refreshIcon(app, session, null, onProgress)
	fun onlineIcons(app: WebApp, location: String, session: Session, progress: (FetchProgress) -> Unit): JSONArray {
		val url = PackageNames.webUrl(location)
		val response = get(url, headers(app.s("headers")), 4 * 1024 * 1024, url, app.s("userAgent"), session) { received, total ->
			progress(FetchProgress(FetchProgress.Stage.ICON, received, total))
		}
		val direct = runCatching { IconData.entry(response.bytes, "upload:${java.util.UUID.randomUUID()}") }.getOrNull()
		if (direct != null) return JSONArray().put(direct)
		val page = Jsoup.parse(response.bytes.toString(Charsets.UTF_8), response.url)
		val source = app.copy().apply { set("baseUrl", url) }
		val icons = websiteIcons(source, session, page, progress)
		require(icons.length() > 0) { "No usable icons found at this URL" }
		repeat(icons.length()) { index -> icons.getJSONObject(index).put("src", "upload:${java.util.UUID.randomUUID()}").remove("manifestIndex") }
		return icons
	}
	fun prepareManual(app: WebApp, session: Session, onProgress: (FetchProgress) -> Unit) {
		val needsTitle = app.manifest().optString("name").isBlank() && app.manifest().optString("short_name").isBlank()
		val needsTheme = app.manifest().optString("theme_color").isBlank()
		val needsDarkTheme = app.s("darkThemeColor").isBlank()
		val page = if (needsTitle || needsTheme || needsDarkTheme) try {
			val base = PackageNames.webUrl(app.s("baseUrl"))
			val response = get(base, headers(app.s("headers")), 4 * 1024 * 1024, base, app.s("userAgent"), session) { received, total ->
				onProgress(FetchProgress(FetchProgress.Stage.PAGE, received, total))
			}
			Jsoup.parse(response.bytes.toString(Charsets.UTF_8), response.url).also { document ->
				if (needsTitle) document.title().trim().takeIf(String::isNotEmpty)?.let { app.manifest().put("name", it) }
				val colors = PageThemeColors.read(document)
				if (needsTheme) colors.theme?.let { app.manifest().put("theme_color", it) }
				if (needsDarkTheme) colors.darkTheme?.let { app.set("darkThemeColor", it) }
			}
		} catch (failure: CredentialsRequired) { if (needsTitle) throw failure else null } catch (_: Exception) { null } else null
		refreshIcon(app, session, page, onProgress)
		refreshShortcuts(app, session = session, onProgress = onProgress)
	}
	private fun refreshIcon(app: WebApp, session: Session, page: org.jsoup.nodes.Document?, onProgress: (FetchProgress) -> Unit) {
		val manual = app.data.optBoolean("iconsManual", false) &&
			(app.icons().length() > 0 || app.s("icon") != IconData.default)
		if (!manual) {
			val sources = app.manifest().optJSONArray("icons")
			val assets = if (sources == null || sources.length() == 0) websiteIcons(app, session, page, onProgress)
				else downloadIcons(app, sources, session, onProgress = onProgress)
			if (assets.length() > 0) {
				app.set("icons", assets)
				app.updateIcon()
			}
		}
		refreshShortcuts(app, true, session, onProgress)
	}
	private fun websiteIcons(app: WebApp, session: Session, page: org.jsoup.nodes.Document?, onProgress: (FetchProgress) -> Unit): JSONArray {
		val base = app.s("baseUrl")
		var location = base
		val document = page ?: try {
			val response = get(base, headers(app.s("headers")), 4 * 1024 * 1024, base, app.s("userAgent"), session) { received, total -> onProgress(FetchProgress(FetchProgress.Stage.PAGE, received, total)) }
			location = response.url
			Jsoup.parse(response.bytes.toString(Charsets.UTF_8), response.url)
		} catch (_: Exception) { null } // Optional discovery must not prevent saving an app.
		if (document != null) location = document.location()
		val links = JSONArray()
		val seen = mutableSetOf<String>()
		document?.select("link[rel][href]")?.forEach { link ->
			val rel = link.attr("rel").lowercase(Locale.ROOT).split(Regex("\\s+"))
			if (rel.none { it in setOf("icon", "apple-touch-icon", "apple-touch-icon-precomposed") }) return@forEach
			val source = link.absUrl("href")
			if (source.isNotEmpty() && seen.add(source) && links.length() < 32)
				links.put(JSONObject().put("src", source))
		}
		val icons = downloadIcons(app, links, session, website = true, onProgress = onProgress)
		if (icons.length() > 0) return icons
		val favicon = URI(location).resolve("/favicon.ico").toString()
		return downloadIcons(app, JSONArray().put(JSONObject().put("src", favicon)), session, website = true, onProgress = onProgress)
	}
	fun refreshShortcuts(app: WebApp, force: Boolean = false, session: Session = Session(), onProgress: (FetchProgress) -> Unit = {}) {
		val shortcuts = app.manifest().optJSONArray("shortcuts") ?: return
		val sources = shortcuts.toString()
		if (!force && app.s("shortcutIconSources") == sources) return
		val cached = app.data.optJSONObject("shortcutIcons") ?: JSONObject()
		repeat(minOf(shortcuts.length(), 16)) { index ->
			val assets = downloadIcons(app, shortcuts.optJSONObject(index)?.optJSONArray("icons"), session, onProgress = onProgress)
			if (assets.length() > 0) cached.put(index.toString(), IconData.best(assets, 192, "any")
				?: IconData.best(assets, 192, "maskable") ?: IconData.default)
		}
		app.set("shortcutIcons", cached)
		app.set("shortcutIconSources", sources)
	}
	private fun downloadIcons(app: WebApp, array: JSONArray?, session: Session, website: Boolean = false, onProgress: (FetchProgress) -> Unit = {}): JSONArray {
		val result = JSONArray()
		if (array == null) return result
		for (index in 0 until minOf(array.length(), 32)) try {
			val icon = array.optJSONObject(index) ?: continue
			val source = URI(app.s("manifestUrl").ifEmpty { app.s("baseUrl") }).resolve(icon.getString("src")).toString()
			val bytes = if (source.startsWith("data:", true)) IconData.fromDataUrl(source)
				else get(source, headers(app.s("headers")), 2 * 1024 * 1024, app.s("baseUrl"), app.s("userAgent"), session) { received, total -> onProgress(FetchProgress(FetchProgress.Stage.ICON, received, total)) }.bytes
			result.put(IconData.entry(bytes, source, icon.optString("purpose", "any")).apply { if (!website) put("manifestIndex", index) })
		} catch (error: CredentialsRequired) { if (!website) throw error } catch (_: Exception) { /* Keep other usable icons. */ }
		return result
	}
}
