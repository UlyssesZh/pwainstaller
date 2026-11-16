package io.github.ulysseszh.pwainstaller

import io.github.ulysseszh.pwainstaller.core.BinaryXml
import io.github.ulysseszh.pwainstaller.core.PackageNames
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.Locale

object ManifestFeatures {
	private const val RUNTIME = "io.github.ulysseszh.pwainstaller.runtime."

	private fun mime(value: String) = value == "*/*" || value.matches(Regex("[a-zA-Z0-9!#$&^_.+-]+/(?:[a-zA-Z0-9!#$&^_.+-]+|\\*)"))

	fun files(params: JSONObject): List<JSONObject> =
		when (val files = params.opt("files")) {
			is JSONArray -> {
				(0 until files.length()).map { files.getJSONObject(it) }
			}

			is JSONObject -> {
				listOf(files)
			}

			else -> {
				emptyList()
			}
		}

	fun accepts(file: JSONObject): List<String> =
		when (val accept = file.opt("accept")) {
			is JSONArray -> {
				(0 until accept.length()).map { accept.getString(it) }
			}

			is String -> {
				listOf(accept)
			}

			else -> {
				emptyList()
			}
		}

	fun validate(
		manifest: JSONObject,
		base: String,
	) {
		val scope = manifest.getString("scope")

		fun action(item: JSONObject) {
			val action = PackageNames.webUrl(URI(base).resolve(item.getString("action")).toString())
			require(PackageNames.sameOrigin(action, scope) && URI(action).path.startsWith(URI(scope).path)) {
				"Handler action must be within the web app scope"
			}
			item.put("action", action)
		}
		manifest.optJSONArray("file_handlers")?.let { handlers ->
			require(handlers.length() <= 16) { "Too many file handlers" }
			repeat(handlers.length()) { i ->
				val handler = handlers.getJSONObject(i)
				action(handler)
				val accept = handler.getJSONObject("accept")
				require(accept.length() in 1..32) { "File handler needs accepted file types" }
				accept.keys().forEach { require(mime(it)) { "Invalid file MIME type" } }
			}
		}
		manifest.optJSONObject("share_target")?.let { target ->
			action(target)
			val method = target.optString("method", "GET").uppercase(Locale.ROOT)
			val encoding = target.optString("enctype", "application/x-www-form-urlencoded").lowercase(Locale.ROOT)
			require(method in listOf("GET", "POST")) { "Share method must be GET or POST" }
			require(encoding in listOf("application/x-www-form-urlencoded", "multipart/form-data")) { "Unsupported share encoding" }
			target.put("method", method).put("enctype", encoding)
			val params = target.getJSONObject("params")
			val files = files(params)
			require(files.size <= 16) { "Too many shared file fields" }
			require(files.isEmpty() || (method == "POST" && encoding == "multipart/form-data")) { "Shared files require multipart POST" }
			files.forEach { file ->
				require(file.getString("name").isNotBlank()) { "Missing shared file field name" }
				require(
					accepts(file).isNotEmpty() &&
						accepts(file).all {
							mime(it) || it.matches(Regex("\\.[a-zA-Z0-9._+-]+"))
						},
				) { "Invalid shared file types" }
			}
		}
	}

	fun filters(
		manifest: JSONObject,
		baseUrl: String = manifest.getString("scope"),
		urlPatterns: String = "",
	): Map<String, List<BinaryXml.IntentFilter>> {
		val handlers = manifest.optJSONArray("file_handlers")
		val types =
			if (handlers == null) {
				emptyList()
			} else {
				(0 until handlers.length())
					.flatMap {
						handlers
							.getJSONObject(it)
							.getJSONObject("accept")
							.keys()
							.asSequence()
							.toList()
					}.distinct()
			}
		val fileFilters =
			if (types.isEmpty()) {
				emptyList()
			} else {
				listOf(
					BinaryXml.IntentFilter(listOf("android.intent.action.VIEW"), types, listOf("content")),
				)
			}
		val share = manifest.optJSONObject("share_target")
		val shareFilters =
			if (share == null) {
				emptyList()
			} else {
				val params = share.getJSONObject("params")
				val accepts =
					files(params).flatMap(::accepts).map {
						if (it.startsWith('.')) {
							"*/*"
						} else {
							it
						}
					}
				val mimeTypes =
					(
						accepts +
							if (listOf("title", "text", "url").any { params.optString(it).isNotEmpty() }) {
								listOf("text/plain")
							} else {
								emptyList()
							}
					).distinct()
				if (mimeTypes.isEmpty()) {
					emptyList()
				} else {
					listOf(
						BinaryXml.IntentFilter(
							if (files(
									params,
								).isEmpty()
							) {
								listOf("android.intent.action.SEND")
							} else {
								listOf("android.intent.action.SEND", "android.intent.action.SEND_MULTIPLE")
							},
							mimeTypes,
						),
					)
				}
			}
		return mapOf(
			RUNTIME + "FileHandlerActivity" to fileFilters,
			RUNTIME + "ShareTargetActivity" to shareFilters,
			RUNTIME + "WebActivity" to listOf(
				BinaryXml.IntentFilter(
					listOf("android.intent.action.MAIN"),
					categories = listOf("android.intent.category.LAUNCHER"),
				),
			) +
				io.github.ulysseszh.pwainstaller.core.UrlRules
					.androidFilters(baseUrl, urlPatterns),
		)
	}
}
