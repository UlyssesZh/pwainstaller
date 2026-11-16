package io.github.ulysseszh.pwainstaller

import android.content.Context
import android.content.SharedPreferences
import io.github.ulysseszh.pwainstaller.core.DataArchive
import io.github.ulysseszh.pwainstaller.core.PackageNames
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.UUID
import java.util.zip.*

/** A portable archive: signing passwords are re-encrypted with the receiving device's key. */
object FullBackup {
	private const val LIMIT = 2L * 1024 * 1024 * 1024
	private val preferenceNames = setOf("adblock", "appearance", "installation", "build-notifications")

	private fun idle(context: Context) {
		check(BuildJobs.active().isEmpty()) { context.getString(R.string.finish_pending_first) }
		AppDatabase(context).use { db -> db.all().forEach { InstallCoordinator.ensureEditable(context, it) } }
		check(InstallCoordinator.tokens(context).isEmpty()) { context.getString(R.string.finish_pending_first) }
	}

	private fun encode(prefs: SharedPreferences): JSONObject =
		JSONObject().apply {
			prefs.all.forEach { (key, value) ->
				val type =
					when (value) {
						is String -> {
							"string"
						}

						is Boolean -> {
							"boolean"
						}

						is Int -> {
							"int"
						}

						is Long -> {
							"long"
						}

						is Float -> {
							"float"
						}

						is Set<*> -> {
							"set"
						}

						else -> {
							error("Unknown preference type")
						}
					}
				put(
					key,
					JSONObject().put("type", type).put(
						"value",
						if (value is Set<*>) {
							JSONArray(value.toList())
						} else {
							value
						},
					),
				)
			}
		}

	private fun apply(
		prefs: SharedPreferences,
		data: JSONObject,
	) {
		val edit = prefs.edit().clear()
		data.keys().forEach { key ->
			val item = data.getJSONObject(key)
			when (item.getString("type")) {
				"string" -> {
					edit.putString(key, item.getString("value"))
				}

				"boolean" -> {
					edit.putBoolean(key, item.getBoolean("value"))
				}

				"int" -> {
					edit.putInt(key, item.getInt("value"))
				}

				"long" -> {
					edit.putLong(key, item.getLong("value"))
				}

				"float" -> {
					edit.putFloat(key, item.getDouble("value").toFloat())
				}

				"set" -> {
					edit.putStringSet(key, item.getJSONArray("value").let { array -> (0 until array.length()).map(array::getString).toSet() })
				}

				else -> {
					error("Invalid preference type")
				}
			}
		}
		check(edit.commit()) { "Cannot restore settings" }
	}

	private fun settings(context: Context) =
		JSONObject().put("signing", SigningKeys.settings(context)).put(
			"preferences",
			JSONObject().apply {
				File(context.applicationInfo.dataDir, "shared_prefs")
					.listFiles()
					.orEmpty()
					.filter {
						it.extension == "xml" &&
							it.nameWithoutExtension in preferenceNames
					}.forEach { put(it.nameWithoutExtension, encode(context.getSharedPreferences(it.nameWithoutExtension, 0))) }
			},
		)

	fun export(
		context: Context,
		output: OutputStream,
	) {
		idle(context)
		val stage = File(context.cacheDir, "backup-export-${UUID.randomUUID()}").apply { check(mkdirs()) }
		try {
			val apps = AppDatabase(context).use { it.all() }
			val profiles = JSONArray()
			for (app in apps) {
				val name = app.s("packageName")
				val pending = InstallCoordinator.restoredProfile(context, name)
				if (Packages.installed(context, name) || pending.isFile) {
					val relative = "profiles/${PackageNames.digest("SHA-256", name.toByteArray())}.zip"
					val profile = File(stage, relative).apply { parentFile!!.mkdirs() }
					if (pending.isFile) {
						pending.copyTo(profile)
					} else {
						Profiles.export(context, name, profile)
					}
					profiles.put(JSONObject().put("packageName", name).put("file", relative))
				}
			}
			val config = settings(context)
			File(stage, "settings.json").writeText(config.toString())
			File(stage, "metadata.json").writeText(
				JSONObject()
					.put("format", 1)
					.put(
						"apps",
						JSONArray(
							apps.map {
								it.data
							},
						),
					).put("profiles", profiles)
					.toString(),
			)
			val store = File(context.filesDir, config.getJSONObject("signing").getString("file"))
			require(store.canonicalFile.parentFile == context.filesDir.canonicalFile)
			store.copyTo(File(stage, "keystore.store"))
			File(context.filesDir, "user-content.json").takeIf(File::isFile)?.copyTo(File(stage, "user-content.json"))
			File(context.filesDir, "filters").listFiles().orEmpty().filter { it.name.matches(Regex("[0-9a-f]{64}\\.txt")) }.forEach {
				it.copyTo(File(stage, "filters/${it.name}").apply { parentFile!!.mkdirs() })
			}
			File(stage, "pwa-backup-v1").writeText("")
			var total = 0L
			ZipOutputStream(output).use { zip ->
				stage.walkTopDown().filter(File::isFile).forEach { file ->
					val path = stage.toURI().relativize(file.toURI()).path
					total += file.length()
					require(total <= LIMIT && file.length() <= entryLimit(path)) { "Backup exceeds size limit" }
					zip.putNextEntry(ZipEntry(path))
					file.inputStream().use { DataArchive.copy(it, zip) }
					zip.closeEntry()
				}
			}
		} finally {
			stage.deleteRecursively()
		}
	}

	private fun entryLimit(name: String) =
		if (name.startsWith("profiles/")) {
			512L * 1024 * 1024
		} else {
			64L * 1024 * 1024
		}

	private fun validatePreferences(prefs: JSONObject) {
		prefs.keys().forEach { name ->
			require(name in preferenceNames) { "Unknown settings group" }
			val values = prefs.getJSONObject(name)
			values.keys().forEach { key ->
				val item = values.getJSONObject(key)
				val value = item.get("value")
				require(
					when (item.getString("type")) {
						"string" -> {
							value is String
						}

						"boolean" -> {
							value is Boolean
						}

						"int" -> {
							value is Number && value.toLong() in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() &&
								value.toDouble() == value.toLong().toDouble()
						}

						"long" -> {
							value is Number && value.toDouble() == value.toLong().toDouble()
						}

						"float" -> {
							value is Number && value.toDouble().toFloat().isFinite()
						}

						"set" -> {
							value is JSONArray && (0 until value.length()).all { value.get(it) is String }
						}

						else -> {
							false
						}
					},
				) { "Invalid setting value" }
			}

			fun value(
				key: String,
				type: String,
			): Any? =
				values.optJSONObject(key)?.let {
					require(it.getString("type") == type)
					it.get("value")
				}
			when (name) {
				"appearance" -> {
					value("layout", "string")?.let { require(it in setOf("list", "tiles")) }
				}

				"installation" -> {
					value("method", "string")?.let {
						require(it in setOf("system", "shizuku"))
						require(it != "shizuku" || android.os.Build.VERSION.SDK_INT >= 24) { "Shizuku requires Android 7 or newer" }
					}
					value("builders", "int")?.let { require((it as Number).toInt() in 1..BuildSettings.MAX_BUILDERS) }
				}

				"build-notifications" -> {
					value("asked", "boolean")
				}

				"adblock" -> {
					value("urls", "string")?.let { urls ->
						io.github.ulysseszh.pwainstaller.core.FilterSources
							.enabled(urls as String)
					}
					value("hours", "long")?.let { require((it as Number).toLong().let { hours -> hours == 0L || hours in 1..8760 }) }
					value("updated", "long")?.let { require((it as Number).toLong() >= 0) }
				}
			}
		}
	}

	private fun atomicCopy(
		source: File,
		target: File,
	) {
		check(target.parentFile!!.mkdirs() || target.parentFile!!.isDirectory)
		val file = android.util.AtomicFile(target)
		val output = file.startWrite()
		try {
			source.inputStream().use { DataArchive.copy(it, output) }
			file.finishWrite(output)
		} catch (
			failure: Exception,
		) {
			file.failWrite(output)
			throw failure
		}
	}

	private fun allowed(name: String) =
		name in setOf("pwa-backup-v1", "metadata.json", "settings.json", "keystore.store", "user-content.json") ||
			name.matches(Regex("profiles/[0-9a-f]{64}\\.zip|filters/[0-9a-f]{64}\\.txt"))

	/** Returns the count of profiles retained for the next reinstall or retry. */
	fun restore(
		context: Context,
		input: InputStream,
	): Int {
		idle(context)
		val stage = File(context.cacheDir, "backup-import-${UUID.randomUUID()}").apply { check(mkdirs()) }
		try {
			var total = 0L
			val names = mutableSetOf<String>()
			ZipInputStream(input).use { zip ->
				while (true) {
					val entry = zip.nextEntry ?: break
					require(!entry.isDirectory && allowed(entry.name) && names.add(entry.name) && names.size < 20000) { "Invalid backup entry" }
					val file = File(stage, entry.name).apply { parentFile!!.mkdirs() }
					val limit = entryLimit(entry.name)
					var size = 0L
					file.outputStream().use { out ->
						val buffer = ByteArray(32768)
						while (true) {
							val count = zip.read(buffer)
							if (count == -1) {
								break
							}
							total += count
							size += count
							require(total <= LIMIT && size <= limit) { "Backup exceeds size limit" }
							out.write(buffer, 0, count)
						}
					}
				}
			}
			require(
				setOf("pwa-backup-v1", "metadata.json", "settings.json", "keystore.store").all {
					it in names
				},
			) { "Not a complete PWA Installer backup" }
			val metadata = JSONObject(File(stage, "metadata.json").readText())
			require(metadata.getInt("format") == 1)
			val entries = metadata.getJSONArray("apps")
			val apps = (0 until entries.length()).map { WebApp(entries.getJSONObject(it)).apply { validate() } }
			require(apps.size <= 10000 && apps.map { it.s("packageName") }.distinct().size == apps.size)
			val config = JSONObject(File(stage, "settings.json").readText())
			val signing = config.getJSONObject("signing")
			SigningKeys.load(
				File(stage, "keystore.store"),
				signing.getString("type"),
				signing.getString("alias"),
				signing.getString("storePassword"),
				signing.getString("keyPassword"),
			)
			val library = File(stage, "user-content.json").takeIf(File::isFile)?.let { UserContentFiles.read(JSONObject(it.readText())) }.orEmpty()
			require(library.size <= 64)
			library.forEach { it.validate() }
			val prefs = config.getJSONObject("preferences")
			validatePreferences(prefs)
			val profiles = metadata.getJSONArray("profiles")
			val packages = mutableSetOf<String>()
			repeat(profiles.length()) { index ->
				val profile = profiles.getJSONObject(index)
				val name = profile.getString("packageName")
				val path = profile.getString("file")
				require(
					apps.any { it.s("packageName") == name } && packages.add(name) &&
						path == "profiles/${PackageNames.digest("SHA-256", name.toByteArray())}.zip" &&
						path in names,
				)
				val validation = File(stage, "validate-$index").apply { check(mkdirs()) }
				try {
					File(stage, path).inputStream().use { DataArchive.importProfile(validation, it) }
				} finally {
					validation.deleteRecursively()
				}
			}
			// All payloads are validated before settings or metadata are changed.
			val store = File(context.filesDir, "restored-${UUID.randomUUID()}.store")
			File(stage, "keystore.store").copyTo(store)
			signing.put("file", store.name)
			val previous = settings(context)
			val oldApps = AppDatabase(context).use { it.all() }
			val previousLibrary = UserContentLibrary.load(context)
			val replaced = mutableListOf<Pair<File, File?>>()

			fun replace(
				source: File,
				target: File,
			) {
				val old = target.takeIf(File::isFile)?.let { existing -> File(stage, "previous-${replaced.size}").also { existing.copyTo(it) } }
				replaced.add(target to old)
				atomicCopy(source, target)
			}
			try {
				File(stage, "filters").listFiles().orEmpty().forEach { replace(it, File(context.filesDir, "filters/${it.name}")) }
				repeat(profiles.length()) { index ->
					val profile = profiles.getJSONObject(index)
					replace(File(stage, profile.getString("file")), InstallCoordinator.restoredProfile(context, profile.getString("packageName")))
				}
				prefs.keys().forEach { apply(context.getSharedPreferences(it, 0), prefs.getJSONObject(it)) }
				SigningKeys.save(context, signing)
				UserContentLibrary.replace(context, library)
				AppDatabase(context).use { it.importAll(entries) }
			} catch (failure: Exception) {
				previous.getJSONObject("preferences").let { old ->
					preferenceNames.forEach { apply(context.getSharedPreferences(it, 0), old.optJSONObject(it) ?: JSONObject()) }
				}
				SigningKeys.save(context, previous.getJSONObject("signing"))
				UserContentLibrary.replace(context, previousLibrary)
				replaced.asReversed().forEach { (target, old) ->
					if (old ==
						null
					) {
						android.util.AtomicFile(target).delete()
					} else {
						atomicCopy(old, target)
					}
				}
				AppDatabase(context).use { db ->
					db.all().forEach(db::remove)
					db.importAll(JSONArray(oldApps.map { it.data }))
				}
				store.delete()
				throw failure
			}
			var pending = 0
			repeat(profiles.length()) { index ->
				val profile = profiles.getJSONObject(index)
				val name = profile.getString("packageName")
				val target = InstallCoordinator.restoredProfile(context, name)
				if (Packages.installed(context, name) &&
					runCatching { Profiles.import(context, name, target) }.isSuccess
				) {
					target.delete()
				} else {
					pending++
				}
			}
			AdblockSettings.invalidate(context)
			return pending
		} finally {
			stage.deleteRecursively()
		}
	}
}
