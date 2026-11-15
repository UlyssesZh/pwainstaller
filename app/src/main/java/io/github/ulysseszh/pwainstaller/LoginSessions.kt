package io.github.ulysseszh.pwainstaller

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import io.github.ulysseszh.pwainstaller.core.DataArchive
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.util.UUID

/** Temporary credentials and site storage never enter portable web-app metadata. */
object LoginSessions {
	private data class Reserved(val appKey: String, var archive: File? = null)
	private val reserved = mutableMapOf<String, Reserved>()
	@Synchronized fun reserve(token: String, appKey: String) {
		check(token !in reserved) { "Temporary browsing data is already being saved" }
		reserved[token] = Reserved(appKey)
	}
	@Synchronized fun release(token: String) { reserved.remove(token) }
	private fun record(context: Context) = File(context.filesDir, "login-session.json")
	fun current(context: Context): JSONObject? = record(context).takeIf(File::isFile)?.let { JSONObject(it.readText()) }
	fun requireSession(context: Context, token: String): JSONObject = requireNotNull(current(context)?.takeIf { it.optString("token") == token }) { "Sign-in session has expired" }
	private fun write(context: Context, value: JSONObject) {
		val temporary = File(context.filesDir, "login-session.tmp")
		temporary.writeText(value.toString())
		check(temporary.renameTo(record(context))) { "Cannot save sign-in session" }
	}
	private fun stop(context: Context) {
		val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
		val processes = manager.runningAppProcesses.orEmpty().filter { it.uid == Process.myUid() && it.processName == "${context.packageName}:login" }
		processes.forEach { Process.killProcess(it.pid) }
		val deadline = System.currentTimeMillis() + 10000
		while (processes.any { File("/proc/${it.pid}").exists() } && System.currentTimeMillis() < deadline) Thread.sleep(25)
		check(processes.none { File("/proc/${it.pid}").exists() }) { "Sign-in browser did not stop" }
	}
	private fun <T> exclusive(context: Context, action: () -> T): T {
		RandomAccessFile(File(context.filesDir, "login-profile.lock"), "rw").use { file ->
			val deadline = System.currentTimeMillis() + 10000
			var lock: FileLock? = null
			while (lock == null && System.currentTimeMillis() < deadline) {
				lock = try { file.channel.tryLock() } catch (error: IOException) {
					if (error.message.orEmpty().contains("EAGAIN") || error.message.orEmpty().contains("EACCES")) null else throw error
				}
				if (lock == null) Thread.sleep(25)
			}
			check(lock != null) { "Sign-in profile is busy" }
			try { return action() } finally { lock.release() }
		}
	}
	private fun browserFiles(context: Context): List<File> = browserFiles(File(context.applicationInfo.dataDir).canonicalFile)
	private fun browserFiles(root: File): List<File> {
		val databases = File(root, "databases").listFiles().orEmpty().filter {
			listOf("http_auth.db", "webview.db", "webviewCache.db").any { name -> it.name == name || it.name.startsWith("$name-") }
		}
		val preferences = File(root, "shared_prefs").listFiles().orEmpty().filter { it.name.startsWith("WebView") }
		return listOf(File(root, "app_webview")) + databases + preferences
	}
	private fun clear(context: Context) = browserFiles(context).filter(File::exists).forEach(DataArchive::delete)

	@Synchronized fun begin(context: Context, appKey: String, seed: File? = null): String {
		// A queued save owns this profile even before a builder is available.
		// Archive it before a different temporary browser clears the shared storage.
		current(context)?.let { session ->
			val token = session.getString("token")
			reserved[token]?.let { seal(context, token, it.appKey) }
		}
		record(context).delete() // Invalidate any old cookie requests before stopping the process.
		stop(context)
		return exclusive(context) {
			clear(context)
			val token = UUID.randomUUID().toString()
			if (seed != null) {
				val stage = File(context.cacheDir, "login-seed-$token").apply { check(mkdirs()) }
				try {
					seed.inputStream().use { DataArchive.importProfile(stage, it) }
					val root = File(context.applicationInfo.dataDir)
					for (file in browserFiles(stage).filter(File::exists)) {
						val relative = stage.toURI().relativize(file.toURI()).path
						val destination = File(root, relative)
						destination.parentFile!!.mkdirs()
						check(file.renameTo(destination)) { "Cannot import temporary browsing data" }
					}
				} finally { stage.deleteRecursively() }
			}
			write(context, JSONObject().put("token", token).put("appKey", appKey).put("completed", false).put("fresh", seed == null))
			token
		}
	}
	fun intent(context: Context, app: WebApp, token: String): Intent {
		val session = requireSession(context, token)
		val fresh = session.optBoolean("fresh") && !session.optBoolean("opened")
		write(context, session.put("opened", true))
		val url = session.optString("lastUrl").ifEmpty {
			java.net.URI(app.s("manifestUrl").ifEmpty { app.s("baseUrl") }).resolve(app.manifest().optString("start_url").ifEmpty { app.s("baseUrl") }).toString()
		}
		return Intent(context, LoginActivity::class.java).putExtra("fresh", fresh)
			.putExtra("session", token).putExtra("url", url).putExtra("headers", app.s("headers")).putExtra("userAgent", app.s("userAgent"))
	}
	fun completed(context: Context, token: String, url: String? = null) {
		val session = requireSession(context, token).put("completed", true)
		if (url != null) session.put("lastUrl", url)
		write(context, session)
	}
	fun call(context: Context, token: String, method: String, url: String? = null, value: String? = null): Bundle =
		requireNotNull(context.contentResolver.call(Uri.parse("content://${context.packageName}.login"), method, token,
			Bundle().apply { putString("url", url); putString("value", value) }))

	/** Quiesce Chromium, then archive only the temporary profile, never installer DBs or keys. */
	@Synchronized fun seal(context: Context, token: String, appKey: String): File {
		val reservation = reserved[token]
		if (reservation != null) {
			check(reservation.appKey == appKey) { "Temporary browsing data belongs to another app" }
			reservation.archive?.takeIf(File::isFile)?.let { return it }
		}
		val session = requireSession(context, token)
		check(session.getString("appKey") == appKey && session.getBoolean("completed")) { "Sign-in is not complete for this app" }
		call(context, token, "flush")
		stop(context)
		return exclusive(context) {
			val stage = File(context.cacheDir, "login-export-$token").apply { check(mkdirs() || isDirectory) }
			val archive = File(context.filesDir, "login-$token.zip")
			try {
				val root = File(context.applicationInfo.dataDir).canonicalFile
				fun copy(source: File, destination: File) {
					check(source.canonicalPath == source.absolutePath) { "Symbolic link in sign-in profile" }
					if (source.isDirectory) {
						check(destination.mkdirs() || destination.isDirectory)
						source.listFiles().orEmpty().forEach { copy(it, File(destination, it.name)) }
					} else { destination.parentFile!!.mkdirs(); source.copyTo(destination, overwrite = true) }
				}
				browserFiles(context).filter(File::exists).forEach { copy(it, File(stage, root.toURI().relativize(it.toURI()).path)) }
				archive.outputStream().use { DataArchive.exportProfile(stage, it) }
				record(context).delete()
				clear(context)
				reservation?.archive = archive
				archive
			} catch (error: Exception) { archive.delete(); throw error }
			finally { stage.deleteRecursively() }
		}
	}
	@Synchronized fun discard(context: Context, token: String) {
		if (current(context)?.optString("token") != token) return
		record(context).delete()
		stop(context)
		exclusive(context) { clear(context) }
	}
}
