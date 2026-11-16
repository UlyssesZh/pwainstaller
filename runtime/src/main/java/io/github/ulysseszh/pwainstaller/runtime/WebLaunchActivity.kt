package io.github.ulysseszh.pwainstaller.runtime

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import io.github.ulysseszh.pwainstaller.core.PackageNames
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors

abstract class WebLaunchActivity : Activity() {
	protected abstract fun prepare(config: JSONObject): JSONObject

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(ProgressBar(this))
		executor.execute {
			try {
				val config = Config.read(this)
				val payload = prepare(config)
				val target = scopedAction(config, payload.getString("url"))
				payload.put("url", target)
				val directory = File(cacheDir, "incoming").apply { check(mkdirs() || isDirectory) }
				directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 86400000 }?.forEach(File::delete)
				val token = UUID.randomUUID().toString()
				File(directory, "$token.json").writeText(payload.toString())
				runOnUiThread {
					if (!isFinishing && !isDestroyed) {
						startActivity(Intent(this, WebActivity::class.java).putExtra("launchToken", token))
					}
					finish()
				}
			} catch (error: Exception) {
				runOnUiThread {
					Toast.makeText(this, getString(R.string.launch_failed), Toast.LENGTH_LONG).show()
					finish()
				}
			}
		}
	}

	companion object {
		private val executor = Executors.newSingleThreadExecutor()

		fun scopedAction(
			config: JSONObject,
			action: String,
		): String {
			val target = PackageNames.webUrl(URI(config.getString("manifestBase")).resolve(action).toString())
			val scope = config.getString("scope")
			require(PackageNames.sameOrigin(target, scope) && URI(target).path.startsWith(URI(scope).path)) { "Action outside app scope" }
			return target
		}
	}
}

class FileHandlerActivity : WebLaunchActivity() {
	override fun prepare(config: JSONObject): JSONObject {
		require(intent.action == Intent.ACTION_VIEW) { "Expected a file-open action" }
		val files = IncomingFiles.read(this, intent)
		require(files.length() > 0) { "No files" }
		val handlers = config.getJSONArray("file_handlers")
		val handler =
			(0 until handlers.length()).map { handlers.getJSONObject(it) }.firstOrNull { handler ->
				val accept = handler.getJSONObject("accept")
				(0 until files.length()).all { index ->
					val file = files.getJSONObject(index)
					accept.keys().asSequence().any { type ->
						IncomingFiles.matches(type, file) || accept.optJSONArray(type)?.let { extensions ->
							(0 until extensions.length()).any { IncomingFiles.matches(extensions.getString(it), file) }
						} == true
					}
				}
			} ?: error("No matching file handler")
		return JSONObject().put("kind", "file").put("url", handler.getString("action")).put("files", files)
	}
}

class ShareTargetActivity : WebLaunchActivity() {
	override fun prepare(config: JSONObject): JSONObject {
		require(intent.action in listOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) { "Expected a share action" }
		val target = config.getJSONObject("share_target")
		val params = target.getJSONObject("params")
		val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
		val title =
			intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()
				?: intent.getCharSequenceExtra(Intent.EXTRA_TITLE)?.toString().orEmpty()
		require(text.length + title.length <= 1024 * 1024) { "Shared text is too large" }
		val fields = JSONArray()
		val names = listOf("title", "text", "url").associateWith { params.optString(it) }
		io.github.ulysseszh.pwainstaller.core.ShareData.fromAndroid(text, title).fields(names).forEach { (name, value) ->
			fields.put(JSONObject().put("name", name).put("value", value))
		}
		val definitions =
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
		val files = IncomingFiles.read(this, intent)
		repeat(files.length()) { i ->
			val file = files.getJSONObject(i)
			val definition =
				definitions.firstOrNull { item ->
					when (val accepts = item.opt("accept")) {
						is JSONArray -> {
							(0 until accepts.length()).any { IncomingFiles.matches(accepts.getString(it), file) }
						}

						is String -> {
							IncomingFiles.matches(accepts, file)
						}

						else -> {
							false
						}
					}
				} ?: error("Shared file type is not accepted")
			file.put("field", definition.getString("name"))
		}
		require(fields.length() > 0 || files.length() > 0) { "No accepted shared content" }
		return JSONObject()
			.put("kind", "share")
			.put("url", target.getString("action"))
			.put("method", target.optString("method", "GET"))
			.put("enctype", target.optString("enctype", "application/x-www-form-urlencoded"))
			.put("fields", fields)
			.put("files", files)
	}
}
