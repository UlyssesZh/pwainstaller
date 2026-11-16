package io.github.ulysseszh.pwainstaller

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import io.github.ulysseszh.pwainstaller.core.ApkProgress
import io.github.ulysseszh.pwainstaller.core.DataArchive
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

/** Durable journal, including backups retained across signature replacement and process restarts. */
object InstallCoordinator {
	private val process = UUID.randomUUID().toString()
	private val submission = Any()
	private val migrated =
		java.util.concurrent.ConcurrentHashMap
			.newKeySet<String>()
	private val migration = Any()
	private val locks = Array(32) { Any() }
	private val progress = java.util.concurrent.ConcurrentHashMap<String, ApkProgress>()

	fun progress(token: String) = progress[token]

	private fun lock(token: String) = locks[(token.hashCode() and Int.MAX_VALUE) % locks.size]

	private fun directory(context: Context) = File(context.filesDir, "operations").apply { check(mkdirs() || isDirectory) }

	private fun operation(
		context: Context,
		token: String,
		suffix: String = "",
	): File {
		require(token.matches(Regex("[0-9a-f-]{36}"))) { "Invalid operation ID" }
		return File(directory(context), "$token$suffix.json")
	}

	private fun migrate(context: Context) {
		if (context.filesDir.absolutePath in migrated) {
			return
		}
		synchronized(migration) { migrateLocked(context) }
	}

	private fun migrateLocked(context: Context) {
		val path = context.filesDir.absolutePath
		if (path in migrated) {
			return
		}
		val preferences = context.getSharedPreferences("operations", 0)
		for ((token, value) in preferences.all) {
			if (value is String) {
				synchronized(lock(token)) {
					val file = operation(context, token)
					if (!file.exists() && !File(file.path + ".bak").exists()) {
						writeFiles(context, token, JSONObject(value))
					}
				}
			}
			check(preferences.edit().remove(token).commit())
		}
		migrated.add(path)
	}

	private fun atomic(
		file: File,
		bytes: ByteArray,
	) {
		val store = android.util.AtomicFile(file)
		val output = store.startWrite()
		try {
			output.write(bytes)
			store.finishWrite(output)
		} catch (failure: Exception) {
			store.failWrite(output)
			throw failure
		}
	}

	private fun writeFiles(
		context: Context,
		token: String,
		plan: JSONObject,
	) {
		val app = plan.getJSONObject("app")
		val metadata = operation(context, token, "-app")
		if (!metadata.exists() && !File(metadata.path + ".bak").exists()) {
			atomic(metadata, app.toString().toByteArray(Charsets.UTF_8))
		}
		val header = JSONObject()
		plan.keys().forEach {
			if (it != "app") {
				header.put(it, plan.get(it))
			}
		}
		header.put(
			"app",
			JSONObject()
				.put("key", app.getString("key"))
				.put("packageName", app.getString("packageName"))
				.put("baseUrl", app.optString("baseUrl"))
				.put("manifest", JSONObject().put("name", WebApp.name(app))),
		)
		atomic(operation(context, token), header.toString().toByteArray(Charsets.UTF_8))
	}

	@Synchronized fun prepare(
		context: Context,
		app: WebApp,
		oldPackage: String?,
		loginProfile: File? = null,
		token: String = UUID.randomUUID().toString(),
		startRequested: Boolean = false,
	): String {
		app.validate()
		ensureEditable(context, app)
		check(!operation(context, token).exists()) { "Installation request already exists" }
		val pendingProfile = loginProfile ?: restoredProfile(context, app.s("packageName")).takeIf(File::isFile)
		val previousUpdate = runCatching { context.packageManager.getPackageInfo(app.s("packageName"), 0).lastUpdateTime }.getOrDefault(0L)
		val plan =
			JSONObject()
				.put("app", app.data)
				.put("state", "prepared")
				.put("owner", process)
				.put("previousUpdateTime", previousUpdate)
		if (startRequested) {
			plan.put("startRequested", true)
		}
		if (oldPackage != null) {
			plan.put("oldPackage", oldPackage)
		}
		if (pendingProfile != null) {
			val folder = File(context.filesDir, "migrations").apply { check(mkdirs() || isDirectory) }
			val destination = File(folder, "$token-login.zip")
			pendingProfile.copyTo(destination, overwrite = true)
			plan.put("loginSnapshot", destination.absolutePath)
		}
		write(context, token, plan)
		pendingProfile?.delete()
		return token
	}

	fun write(
		context: Context,
		token: String,
		plan: JSONObject,
	) {
		migrate(context)
		synchronized(lock(token)) { writeFiles(context, token, plan) }
		if (plan.getString("state") !in listOf("ready", "committed", "confirmation", "installed")) {
			progress.remove(token)
		}
	}

	fun summary(
		context: Context,
		token: String,
	): JSONObject {
		migrate(context)
		return synchronized(lock(token)) {
			android.util
				.AtomicFile(operation(context, token))
				.openRead()
				.bufferedReader()
				.use { JSONObject(it.readText()) }
		}
	}

	fun authorizeBackground(
		context: Context,
		token: String,
	): JSONObject {
		migrate(context)
		return synchronized(lock(token)) {
			summary(context, token).also { plan ->
				plan.put("background", true).put("interrupted", false)
				write(context, token, plan)
			}
		}
	}

	fun read(
		context: Context,
		token: String,
	): JSONObject {
		migrate(context)
		return synchronized(lock(token)) {
			summary(context, token).also { plan ->
				plan.put(
					"app",
					android.util
						.AtomicFile(operation(context, token, "-app"))
						.openRead()
						.bufferedReader()
						.use { JSONObject(it.readText()) },
				)
			}
		}
	}

	fun tokens(context: Context): List<String> {
		migrate(context)
		return directory(context)
			.listFiles()
			.orEmpty()
			.map {
				it.name.removeSuffix(".bak")
			}.filter { it.matches(Regex("[0-9a-f-]{36}\\.json")) }
			.map { it.removeSuffix(".json") }
			.distinct()
	}

	fun interrupted(
		context: Context,
		token: String,
	): Boolean {
		// A completed operation can be removed after the caller lists its token.
		val plan =
			try {
				summary(context, token)
			} catch (_: java.io.FileNotFoundException) {
				return false
			}
		if (BuildJobs.forApp(plan.getJSONObject("app").getString("key")) != null) {
			return false
		}
		return when (plan.getString("state")) {
			"prepared" -> {
				plan.optBoolean("interrupted") || (plan.optString("owner") != process && !plan.optBoolean("background"))
			}

			"ready" -> {
				plan.optBoolean("interrupted")
			}

			"signature_conflict",
			"backup_failed",
			"replacement_uninstall",
			"replacement_failed",
			"transfer_failed",
			"awaiting_uninstall",
			"confirmation",
			-> {
				true
			}

			else -> {
				false
			}
		}
	}

	fun interrupt(
		context: Context,
		token: String,
	) {
		write(context, token, summary(context, token).put("interrupted", true))
	}

	fun ensureEditable(
		context: Context,
		app: WebApp,
	) {
		for (token in tokens(context)) {
			val plan = summary(context, token)
			if (plan.getJSONObject("app").getString("key") != app.s("key")) {
				continue
			}
			if (plan.getString("state") in listOf("done", "failed") ||
				(plan.getString("state") == "awaiting_uninstall" && !Packages.installed(context, plan.getString("oldPackage")))
			) {
				discard(context, token)
				continue
			}
			if (plan.getString("state") in listOf("committed", "confirmation") && plan.has("session") &&
				context.packageManager.packageInstaller.getSessionInfo(plan.getInt("session")) == null
			) {
				val updated = runCatching { context.packageManager.getPackageInfo(app.s("packageName"), 0).lastUpdateTime }.getOrDefault(0L)
				if (plan.has("previousUpdateTime") &&
					updated > plan.getLong("previousUpdateTime")
				) {
					installed(context, token)
					complete(context, token)
				} else {
					fail(context, token, "Installation session expired")
				}
				if (summary(context, token).getString("state") in listOf("done", "failed")) {
					discard(context, token)
					continue
				}
			}
			throw IllegalStateException(context.getString(R.string.pending_operation))
		}
	}

	fun retainProfile(
		context: Context,
		packageName: String,
		archive: File,
	) {
		val target = restoredProfile(context, packageName).apply { check(parentFile!!.mkdirs() || parentFile!!.isDirectory) }
		val store = android.util.AtomicFile(target)
		val output = store.startWrite()
		try {
			archive.inputStream().use { DataArchive.copy(it, output) }
			store.finishWrite(output)
		} catch (
			failure: Exception,
		) {
			store.failWrite(output)
			throw failure
		}
		archive.delete()
	}

	fun restoredProfile(
		context: Context,
		packageName: String,
	) = File(
		context.filesDir,
		"restored-profiles/${io.github.ulysseszh.pwainstaller.core.PackageNames.digest("SHA-256", packageName.toByteArray())}.zip",
	)

	fun start(
		context: Context,
		token: String,
		progress: (ApkProgress) -> Unit = {},
	) {
		prepareApk(context, token, progress)
		installPrepared(context, token, progress)
	}

	fun prepareApk(
		context: Context,
		token: String,
		progress: (ApkProgress) -> Unit = {},
		log: (String) -> Unit = {},
	) {
		val plan = read(context, token)
		if (plan.getString("state") != "prepared") {
			return
		}
		val app = WebApp(plan.getJSONObject("app"))
		val apk =
			plan
				.optString("apk")
				.takeIf { it.isNotEmpty() }
				?.let(::File)
				?.takeIf { it.isFile }
				?: ApkBuilder.buildLogged(context, app, progress, log).also {
					plan.put("apk", it.absolutePath)
					write(context, token, plan)
				}
		if (Packages.signatureConflict(context, app.s("packageName"), apk)) {
			plan.put("state", "signature_conflict")
			write(context, token, plan)
			return
		}
		if (plan.has("oldPackage") && !plan.has("snapshot") && !plan.optBoolean("skipSnapshot")) {
			progress(ApkProgress(ApkProgress.Stage.BACKUP))
			snapshot(context, token, plan, plan.getString("oldPackage"))
		}
		plan.put("state", "ready")
		write(context, token, plan)
	}

	fun installPrepared(
		context: Context,
		token: String,
		progress: (ApkProgress) -> Unit = {},
	): Boolean =
		synchronized(submission) {
			installPreparedLocked(context, token) { value ->
				this.progress[token] = value
				progress(value)
			}
		}

	private fun installPreparedLocked(
		context: Context,
		token: String,
		progress: (ApkProgress) -> Unit,
	): Boolean {
		val plan = read(context, token)
		if (plan.getString("state") != "ready") {
			return false
		}
		// System confirmation activities must not race each other. APK generation
		// is independent and can continue for other apps while the user confirms.
		if (tokens(context).any {
				it != token && summary(context, it).optString("state") in listOf("committed", "confirmation", "installed")
			}
		) {
			return false
		}
		BuildService.start(context, WebApp.name(plan.getJSONObject("app")), R.string.build_installing)
		val app = WebApp(plan.getJSONObject("app"))
		val apk = File(plan.getString("apk"))
		if (InstallationSettings.method(context) == "shizuku") {
			if (Build.VERSION.SDK_INT < 24) {
				throw IllegalStateException(context.getString(R.string.shizuku_not_ready))
			}
			progress(ApkProgress(ApkProgress.Stage.INSTALLING))
			plan.put("installer", "shizuku").put("state", "committed")
			write(context, token, plan)
			try {
				ShizukuInstaller.install(context, apk)
				installed(context, token)
			} catch (
				failure: Exception,
			) {
				fail(context, token, failure.message)
				throw failure
			}
			return true
		}
		val installer = context.packageManager.packageInstaller
		val params =
			PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
				setAppPackageName(app.s("packageName"))
				setSize(apk.length())
				if (Build.VERSION.SDK_INT >= 31) {
					setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
				}
			}
		val sessionId = installer.createSession(params)
		try {
			installer.openSession(sessionId).use { session ->
				progress(ApkProgress(ApkProgress.Stage.INSTALLING, 0))
				apk.inputStream().use { input ->
					session.openWrite("base.apk", 0, apk.length()).use { output ->
						val buffer = ByteArray(32768)
						var written = 0L
						while (true) {
							val count = input.read(buffer)
							if (count < 0) {
								break
							}
							output.write(buffer, 0, count)
							written += count
							progress(ApkProgress(ApkProgress.Stage.INSTALLING, (written * 100 / apk.length()).toInt()))
						}
						session.fsync(output)
					}
				}
				plan.put("session", sessionId).put("state", "committed")
				write(context, token, plan)
				val callback =
					Intent(
						context,
						InstallReceiver::class.java,
					).setAction("io.github.ulysseszh.pwainstaller.INSTALL").setData(Uri.parse("pwa-install:$token"))
				val pending =
					PendingIntent.getBroadcast(
						context,
						sessionId,
						callback,
						PendingIntent.FLAG_UPDATE_CURRENT or
							if (Build.VERSION.SDK_INT >= 31) {
								PendingIntent.FLAG_MUTABLE
							} else {
								0
							},
					)
				progress(ApkProgress(ApkProgress.Stage.WAITING))
				session.commit(pending.intentSender)
			}
		} catch (error: Exception) {
			runCatching { installer.abandonSession(sessionId) }
			fail(context, token, error.toString())
			throw error
		}
		return true
	}

	private fun snapshot(
		context: Context,
		token: String,
		plan: JSONObject,
		name: String,
	) {
		val directory = File(context.filesDir, "migrations")
		if (!directory.mkdirs() && !directory.isDirectory) {
			throw IOException("Cannot create migration directory")
		}
		val snapshot = File(directory, "$token.zip")
		val temporary = File(directory, "$token.zip.part")
		try {
			val input = context.contentResolver.openInputStream(Packages.profile(name)) ?: throw IOException("No profile provider")
			input.use { source -> temporary.outputStream().use { DataArchive.copy(source, it) } }
			// Validate the whole archive before allowing the old package to be removed.
			val validation = File(directory, "$token.validation")
			try {
				check(validation.mkdirs())
				temporary.inputStream().use { DataArchive.importProfile(validation, it) }
			} finally {
				validation.deleteRecursively()
			}
			if (!temporary.renameTo(snapshot)) {
				throw IOException("Cannot persist browsing-data backup")
			}
			plan.put("snapshot", snapshot.absolutePath)
			write(context, token, plan)
		} finally {
			temporary.delete()
		}
	}

	fun replaceSignature(
		context: Context,
		token: String,
		withoutData: Boolean = false,
	) {
		val plan = read(context, token)
		check(plan.getString("state") in listOf("signature_conflict", "backup_failed")) { "Not awaiting signature replacement" }
		if (!withoutData && !plan.has("snapshot")) {
			try {
				val app = WebApp(plan.getJSONObject("app"))
				snapshot(context, token, plan, plan.optString("oldPackage", app.s("packageName")))
			} catch (error: Exception) {
				plan.put("state", "backup_failed").put("error", error.toString())
				write(context, token, plan)
				return
			}
		}
		plan.put("replacement", true).put("skipSnapshot", withoutData).put("state", "replacement_uninstall")
		write(context, token, plan)
	}

	fun resumeReplacement(
		context: Context,
		token: String,
	) {
		val plan = read(context, token)
		check(plan.getString("state") in listOf("replacement_uninstall", "replacement_failed"))
		check(!Packages.installed(context, WebApp(plan.getJSONObject("app")).s("packageName")) || plan.getString("state") == "replacement_failed")
		plan.put("state", "prepared")
		write(context, token, plan)
	}

	fun signatureRejected(
		context: Context,
		token: String,
	): Boolean {
		val plan = read(context, token)
		val apk = plan.optString("apk").takeIf { it.isNotEmpty() }?.let(::File) ?: return false
		if (!apk.isFile || !Packages.signatureConflict(context, WebApp(plan.getJSONObject("app")).s("packageName"), apk)) {
			return false
		}
		plan.put("state", "signature_conflict")
		write(context, token, plan)
		return true
	}

	fun installed(
		context: Context,
		token: String,
	) {
		val plan = read(context, token).put("state", "installed")
		write(context, token, plan)
	}

	@Synchronized fun complete(
		context: Context,
		token: String,
	) {
		val plan = read(context, token)
		if (plan.getString("state") !in listOf("installed", "transfer_failed")) {
			return
		}
		val app = WebApp(plan.getJSONObject("app"))
		if (plan.has("loginSnapshot") || plan.has("snapshot")) {
			try {
				File(plan.optString("loginSnapshot", plan.optString("snapshot"))).inputStream().use { input ->
					val output =
						context.contentResolver.openOutputStream(Packages.profile(app.s("packageName")), "w") ?: throw IOException("No profile provider")
					output.use { DataArchive.copy(input, it) }
				}
				context.contentResolver.call(Packages.profile(app.s("packageName")), "commit", null, null)
			} catch (error: Exception) {
				plan.put("state", "transfer_failed").put("error", error.toString())
				write(context, token, plan)
				return
			}
		}
		if (!plan.optBoolean("profileOnly")) {
			AppDatabase(context).use { it.save(app) }
		}
		plan.put(
			"state",
			if (plan.has("oldPackage")) {
				"awaiting_uninstall"
			} else {
				"done"
			},
		)
		val notify = plan.optBoolean("background") && !plan.optBoolean("completionNotified")
		if (notify) {
			plan.put("completionNotified", true)
		}
		write(context, token, plan)
		deleteArtifacts(plan)
		if (notify) {
			android.os.Handler(android.os.Looper.getMainLooper()).post {
				android.widget.Toast
					.makeText(
						context.applicationContext,
						context.getString(
							if (plan.optBoolean("profileOnly")) {
								R.string.imported
							} else {
								R.string.install_success
							},
							app.name(),
						),
						android.widget.Toast.LENGTH_SHORT,
					).show()
			}
		}
	}

	fun importProfile(
		context: Context,
		app: WebApp,
		archive: File,
	): String {
		val token = prepare(context, app, null, archive)
		write(context, token, summary(context, token).put("profileOnly", true).put("state", "installed"))
		complete(context, token)
		return token
	}

	fun fail(
		context: Context,
		token: String,
		message: String?,
	) {
		val plan = read(context, token)
		plan
			.put(
				"state",
				if (plan.optBoolean("replacement") || plan.has("loginSnapshot")) {
					"replacement_failed"
				} else {
					"failed"
				},
			).put("error", message ?: "Unknown installation error")
		write(context, token, plan)
		if (!plan.optBoolean("replacement") && !plan.has("loginSnapshot")) {
			deleteArtifacts(plan)
		}
	}

	fun discard(
		context: Context,
		token: String,
		discardBackup: Boolean = false,
	) {
		migrate(context)
		synchronized(lock(token)) {
			val file = operation(context, token)
			if (!file.exists() && !File(file.path + ".bak").exists()) {
				return
			}
			val plan = summary(context, token)
			check(
				discardBackup || plan.optString("state") in listOf("done", "awaiting_uninstall") ||
					((!plan.optBoolean("replacement") || !File(plan.optString("snapshot")).isFile) && !File(plan.optString("loginSnapshot")).isFile),
			) {
				"Recovery backup must be retained until browsing data is restored"
			}
			deleteArtifacts(plan)
			android.util.AtomicFile(operation(context, token)).delete()
			android.util.AtomicFile(operation(context, token, "-app")).delete()
			progress.remove(token)
		}
	}

	private fun deleteArtifacts(plan: JSONObject) {
		listOf("snapshot", "loginSnapshot", "apk").filter(plan::has).forEach { File(plan.getString(it)).delete() }
	}
}
