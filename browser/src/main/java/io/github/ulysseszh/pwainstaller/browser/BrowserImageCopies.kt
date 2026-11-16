package io.github.ulysseszh.pwainstaller.browser

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.AtomicFile
import android.webkit.MimeTypeMap
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import java.util.concurrent.Executors

/** A clipboard URI is available before its bytes. Small, private completion records
 * also let the provider (in a different process) serve it after the browser closes. */
internal object BrowserImageCopies {
	private val worker = Executors.newFixedThreadPool(2)
	private val tokenPattern = Regex("[0-9a-f-]{36}")
	private val imagePattern = Regex("[0-9a-f-]{36}\\.[a-zA-Z0-9]+")
	private val mimePattern = Regex("image/[a-zA-Z0-9.+-]+")
	private const val MAX_AGE = 86400000L

	fun owns(uri: Uri) = uri.pathSegments.firstOrNull() == "copy"

	fun create(
		context: Context,
		source: String,
	): Uri {
		val declared =
			if (source.startsWith("data:image/", true)) {
				source
					.take(128)
					.substringAfter(':')
					.substringBefore(';')
					.substringBefore(',')
			} else {
				MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(source).lowercase())
			}
		val mime = declared?.takeIf { mimePattern.matches(it) } ?: "image/*"
		return Uri
			.Builder()
			.scheme("content")
			.authority("${context.packageName}.images")
			.appendPath("copy")
			.appendPath(UUID.randomUUID().toString())
			.appendQueryParameter("mime", mime)
			.build()
	}

	private fun record(
		context: Context,
		uri: Uri,
	): File {
		val path = uri.pathSegments
		if (uri.authority != "${context.packageName}.images" || path.size != 2 || path[0] != "copy" || !tokenPattern.matches(path[1])) {
			throw FileNotFoundException("Invalid image URI")
		}
		return File(context.cacheDir, "browser-image-copies/${path[1]}.json")
	}

	private fun state(
		context: Context,
		uri: Uri,
	): JSONObject? {
		val file = record(context, uri)
		if (System.currentTimeMillis() - file.lastModified() > MAX_AGE) {
			return null
		}
		return try {
			JSONObject(AtomicFile(file).readFully().toString(Charsets.UTF_8))
		} catch (_: FileNotFoundException) {
			null
		}
	}

	private fun publish(
		context: Context,
		uri: Uri,
		data: JSONObject,
	) {
		val file = record(context, uri)
		check(file.parentFile!!.mkdirs() || file.parentFile!!.isDirectory)
		val atomic = AtomicFile(file)
		val stream = atomic.startWrite()
		try {
			stream.write(data.toString().toByteArray(Charsets.UTF_8))
			atomic.finishWrite(stream)
		} catch (
			failure: Exception,
		) {
			atomic.failWrite(stream)
			throw failure
		}
		file.parentFile!!
			.listFiles()
			.orEmpty()
			.filter { it.extension == "json" }
			.sortedByDescending(File::lastModified)
			.forEachIndexed { index, old ->
				if (index >= 256 || System.currentTimeMillis() - old.lastModified() > MAX_AGE) {
					old.delete()
				}
			}
	}

	fun load(
		context: Context,
		uri: Uri,
		source: String,
		page: String,
		agent: String,
		headers: Map<String, String>,
		failed: (Exception) -> Unit,
	) {
		// Independent of the activity's save/share executor: finishing the browser
		// must not cancel a copy that has already been placed on the clipboard.
		val application = context.applicationContext
		worker.execute {
			try {
				val image = BrowserImages.read(application, source, page, agent, headers)
				publish(application, uri, JSONObject().put("file", image.file.name).put("mime", image.mime))
			} catch (failure: Exception) {
				fail(application, uri)
				failed(failure)
			}
		}
	}

	private fun fail(
		context: Context,
		uri: Uri,
	) {
		runCatching { publish(context, uri, JSONObject().put("failed", true)) }
	}

	fun failAsync(
		context: Context,
		uri: Uri,
	) {
		val application = context.applicationContext
		worker.execute { fail(application, uri) }
	}

	fun mime(
		context: Context,
		uri: Uri,
	): String =
		state(context, uri)?.optString("mime")?.takeIf { mimePattern.matches(it) }
			?: uri.getQueryParameter("mime")?.takeIf { mimePattern.matches(it) } ?: "image/*"

	fun ready(
		context: Context,
		uri: Uri,
	): File? {
		val data = state(context, uri) ?: return null
		if (data.optBoolean("failed")) {
			throw FileNotFoundException("Image unavailable")
		}
		val name = data.optString("file")
		if (!imagePattern.matches(name)) {
			throw FileNotFoundException("Invalid image file")
		}
		return File(context.cacheDir, "browser-images/$name").takeIf(File::isFile)
			?: throw FileNotFoundException("Image expired")
	}

	fun open(
		context: Context,
		uri: Uri,
	): ParcelFileDescriptor {
		val deadline = SystemClock.elapsedRealtime() + 60000
		while (true) {
			ready(context, uri)?.let { return ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) }
			if (SystemClock.elapsedRealtime() >= deadline) {
				throw FileNotFoundException("Image unavailable")
			}
			try {
				Thread.sleep(25)
			} catch (
				_: InterruptedException,
			) {
				Thread.currentThread().interrupt()
				throw FileNotFoundException("Image transfer interrupted")
			}
		}
	}
}
