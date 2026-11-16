package io.github.ulysseszh.pwainstaller

import io.github.ulysseszh.pwainstaller.core.DisplayMode
import io.github.ulysseszh.pwainstaller.core.PackageNames
import io.github.ulysseszh.pwainstaller.core.UrlRules
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.UUID

class WebApp {
	@JvmField val data: JSONObject
	constructor() {
		data =
			JSONObject()
				.put("key", UUID.randomUUID().toString())
				.put("useManifest", true)
				.put(
					"install",
					true,
				).put("webViewDebugging", false)
				.put("useNotchAreas", true)
				.put("icon", IconData.default)
				.put("manifest", JSONObject())
	}
	constructor(source: JSONObject) {
		data = JSONObject(source.toString())
		if (s("key").isEmpty()) {
			set("key", UUID.randomUUID().toString())
		}
		if (s("icon").isEmpty()) {
			set("icon", IconData.default)
		}
		if (!data.has("useNotchAreas")) {
			set("useNotchAreas", true)
		}
		data.remove("selectedIcon")
		data.remove("browserUi")
		repeat(icons().length()) {
			icons().getJSONObject(it).let { icon ->
				if (IconData.scalable(icon)) {
					icon.put("type", "image/svg+xml")
					icon.put("sizes", "any")
				}
			}
		}
		if (icons().length() > 0) {
			updateIcon()
		}
		require(data.opt("manifest") is JSONObject) { "Missing manifest object" }
		if (s("id").isEmpty() && manifest().optString("id").isNotEmpty()) {
			runCatching {
				set("id", PackageNames.withoutFragment(PackageNames.webUrl(manifest().getString("id"))))
			}
		}
	}

	fun copy() = WebApp(data)

	fun s(key: String): String = data.optString(key, "")

	fun b(key: String): Boolean = data.optBoolean(key, key != "webViewDebugging")

	fun set(
		key: String,
		value: Any,
	) {
		data.put(key, value)
	}

	fun icons(): JSONArray = data.optJSONArray("icons") ?: JSONArray().also { set("icons", it) }

	fun syncIconPurposes() {
		val sources = manifest().optJSONArray("icons") ?: return

		fun resolved(source: JSONObject): String? =
			runCatching {
				URI(s("manifestUrl").ifEmpty { s("baseUrl") }).resolve(source.optString("src")).toString()
			}.getOrNull()
		repeat(icons().length()) { iconIndex ->
			val icon = icons().getJSONObject(iconIndex)
			val position = icon.optInt("manifestIndex", -1)
			val indexed = sources.optJSONObject(position)?.takeIf { resolved(it) == icon.optString("src") }
			val source =
				indexed ?: (0 until sources.length())
					.mapNotNull { sources.optJSONObject(it) }
					.singleOrNull { resolved(it) == icon.optString("src") }
			if (source != null) {
				icon.put("purpose", IconData.purposes(source.optString("purpose", "any")).joinToString(" "))
			}
		}
		if (icons().length() > 0) {
			updateIcon()
		}
	}

	fun updateIcon() {
		data.remove("selectedIcon")
		set("icon", IconData.representative(icons()))
	}

	fun manifest(): JSONObject = data.getJSONObject("manifest")

	fun applyManifestScope() {
		val source = URI(s("manifestUrl").ifEmpty { s("baseUrl") })
		val start = PackageNames.webUrl(source.resolve(manifest().optString("start_url", s("baseUrl"))).toString())
		val scope = PackageNames.webUrl(source.resolve(manifest().optString("scope").ifBlank { URI(start).resolve(".").toString() }).toString())
		val resolved = PackageNames.withoutFragment(scope).substringBefore('?')
		manifest().put("scope", resolved)
		set("urlPatterns", UrlRules.defaultScopes(resolved))
	}

	fun displayMode(): String {
		val overrides = manifest().optJSONArray("display_override")
		return DisplayMode.resolve(
			manifest().optString("display"),
			if (overrides == null) {
				emptyList()
			} else {
				(0 until overrides.length()).map { overrides.optString(it) }
			},
		)
	}

	fun name(): String = name(data)

	companion object {
		fun name(data: JSONObject): String =
			data.getJSONObject("manifest").let { manifest ->
				manifest.optString("name", manifest.optString("short_name", URI(data.optString("baseUrl", "")).host ?: ""))
			}
	}

	/** Rules are stored separately so the metadata row fits Android's cursor window. */
	fun metadataForStorage(): String {
		val metadata =
			JSONObject()
				.apply {
					data.keys().forEach { key ->
						when {
							key == "adblockRules" -> {
								put(key, "")
							}

							UserContentFiles.Kind.entries.any { it.key == key } -> {
								put(key, JSONArray())
							}

							else -> {
								put(key, data.get(key))
							}
						}
					}
				}.toString()
		require(metadata.toByteArray(Charsets.UTF_8).size <= 1536 * 1024) { "Metadata excluding filter rules exceeds 1.5 MiB; remove some icons" }
		return metadata
	}

	fun validate() {
		data.remove("browserUi")
		set("baseUrl", PackageNames.webUrl(s("baseUrl")))
		if (s("manifestUrl").isNotEmpty()) {
			set("manifestUrl", PackageNames.webUrl(s("manifestUrl")))
		}
		require(s("userAgent").length <= 8192 && s("userAgent").none { it == '\r' || it == '\n' || it == '\u0000' }) { "Invalid user agent" }
		UrlRules.validate(s("urlPatterns"))
		io.github.ulysseszh.pwainstaller.core.AdblockRules
			.validate(s("adblockRules"))
		UserContentFiles.validate(data)
		ManifestFetcher.headers(s("headers"))
		for (key in listOf("darkThemeColor", "darkBackgroundColor")) {
			require(
				s(key).isBlank() || io.github.ulysseszh.pwainstaller.core.CssColors
					.parse(s(key)) != null,
			) { "Invalid dark mode color" }
		}
		validateIdentity()
		val manifest = manifest()
		if (s("icon").isEmpty()) {
			set("icon", IconData.default)
		}
		IconData.decode(s("icon"))
		require(icons().length() <= 32) { "Too many icons (maximum 32)" }
		repeat(icons().length()) {
			val icon = icons().getJSONObject(it)
			IconData.validate(icon)
			icon.put("purpose", IconData.purposes(icon.optString("purpose", "any")).joinToString(" "))
		}
		if (icons().length() > 0) {
			updateIcon()
		}
		data.remove("selectedIcon")
		data.optJSONObject("shortcutIcons")?.let { cached ->
			cached.keys().forEach { key ->
				require(key.toIntOrNull() in 0..15) { "Invalid shortcut icon" }
				IconData.decode(cached.getString(key))
			}
		}
		ManifestFeatures.validate(manifest, s("manifestUrl").ifEmpty { s("baseUrl") })
		metadataForStorage()
	}

	/** Resolve the package before a migration decision, without scanning icons or large rule/file snapshots. */
	fun validateIdentity() {
		set("baseUrl", PackageNames.webUrl(s("baseUrl")))
		if (s("manifestUrl").isNotEmpty()) {
			set("manifestUrl", PackageNames.webUrl(s("manifestUrl")))
		}
		val manifest = manifest()
		val source = s("manifestUrl").ifEmpty { s("baseUrl") }
		val start = PackageNames.webUrl(URI(source).resolve(manifest.optString("start_url", s("baseUrl"))).toString())
		require(PackageNames.sameOrigin(start, s("baseUrl"))) { "start_url must have the same origin as the base URL" }
		manifest.put("start_url", start)
		val scope = PackageNames.webUrl(URI(source).resolve(manifest.optString("scope", URI(start).resolve(".").toString())).toString())
		require(PackageNames.sameOrigin(scope, start) && URI(start).path.startsWith(URI(scope).path)) {
			"scope must contain start_url on the same origin"
		}
		manifest.put("scope", PackageNames.withoutFragment(scope).substringBefore('?'))
		val id = PackageNames.resolveId(start, manifest.optString("id", ""))
		require(PackageNames.sameOrigin(id, start)) { "id must have the same origin as start_url" }
		manifest.put("id", id)
		set("id", id)
		if (manifest.optString("name").isBlank()) {
			manifest.put("name", manifest.optString("short_name").ifBlank { URI(start).host })
		}
		if (s("packageName").isEmpty()) {
			set("packageName", PackageNames.forId(id))
		}
		require(PackageNames.valid(s("packageName"))) { "Invalid Android package name" }
	}
}
