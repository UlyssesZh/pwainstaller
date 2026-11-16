package io.github.ulysseszh.pwainstaller

import android.content.Context
import android.content.Intent
import io.github.ulysseszh.pwainstaller.core.ApkProgress
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** The editor hands off an accepted draft before profile export or disk writes begin. */
object SaveJobs {
	fun start(
		context: Context,
		app: WebApp,
		oldPackage: String? = null,
		loginProfile: File? = null,
		loginSession: String? = null,
		originalKey: String? = null,
		selectDefaultRules: Boolean = false,
	): Boolean {
		val context = context.applicationContext
		val token = UUID.randomUUID().toString()
		val install = oldPackage != null || app.b("install")
		val key = app.s("key")
		val entry =
			AppDatabase.Summary(
				key,
				app.s("packageName"),
				app.name(),
				app.s("baseUrl"),
				null,
				app.manifest().optString("short_name").ifBlank { app.name() },
			)
		loginSession?.let { LoginSessions.reserve(it, key) }
		return try {
			BuildJobs
				.start(
					context,
					if (install) {
						"install:$token"
					} else {
						"save:$token"
					},
					key,
					app.name(),
					entry,
					ApkProgress(ApkProgress.Stage.VALIDATING),
				) { progress, log ->
					fun changed() {
						context.sendBroadcast(Intent(InstallReceiver.CHANGED).setPackage(context.packageName))
					}
					var prepared = false
					var profile = loginProfile
					try {
						progress(ApkProgress(ApkProgress.Stage.VALIDATING))
						if (selectDefaultRules) {
							log("Selecting default filter rules from the local cache")
							app.set("adblockRules", AdblockSettings.cachedDefaults(context, app.s("baseUrl")))
						}
						log("Validating submitted metadata")
						app.validate()
						InstallCoordinator.ensureEditable(context, app)
						AppDatabase(context).use { db -> require(!db.conflictingPackage(app)) { context.getString(R.string.duplicate_package) } }
						progress(ApkProgress(ApkProgress.Stage.SAVING))
						log("Saving web app metadata")
						if (oldPackage == null) {
							AppDatabase(context).use { it.save(app) }
						}
						changed()
						profile = profile ?: loginSession?.let {
							log("Exporting temporary browser data")
							LoginSessions.seal(context, it, key)
						}
						if (install) {
							log("Preparing installation request")
							InstallCoordinator.prepare(context, app, oldPackage, profile, token, startRequested = true)
							prepared = true
							recoveryFile(context, key)?.delete()
							// Build under the same job/notification identity. Permission prompts
							// still belong to MainActivity, which resumes this prepared request.
							if (InstallPipeline.allowed(context)) {
								InstallCoordinator.authorizeBackground(context, token)
							}
							InstallCoordinator.prepareApk(context, token, { update ->
								// This draft was validated before saving; the builder's
								// defensive revalidation must not move the bar backwards.
								if (update.stage != ApkProgress.Stage.VALIDATING) {
									progress(update)
								}
							}, log)
						} else {
							profile?.let { InstallCoordinator.retainProfile(context, app.s("packageName"), it) }
						}
						recoveryFile(context, key)?.delete()
						null
					} catch (failure: Exception) {
						if (prepared) {
							runCatching { InstallCoordinator.fail(context, token, failure.message) }
						} else {
							runCatching {
								if (profile == null && loginSession != null) {
									profile =
										runCatching { LoginSessions.seal(context, loginSession, key) }
											.onFailure { log("Could not archive temporary browsing data: $it") }
											.getOrNull()
								}
								// Validation happens after leaving the editor. Keep all of the
								// submitted values and temporary browsing data available to fix it.
								val draft =
									JSONObject()
										.put("app", app.data)
										.put("originalKey", originalKey)
										.put("loginProfile", profile?.name)
										.put("loginSession", loginSession)
								recoveryFile(context, key)?.writeText(draft.toString())
							}.onFailure { log("Could not retain submitted draft: $it") }
						}
						throw failure
					} finally {
						loginSession?.let(LoginSessions::release)
						changed()
					}
				}.also { started ->
					if (!started) {
						loginSession?.let(LoginSessions::release)
					}
				}
		} catch (failure: Exception) {
			loginSession?.let(LoginSessions::release)
			throw failure
		}
	}

	fun recoveryFile(
		context: Context,
		key: String,
	): File? =
		key
			.takeIf { it.matches(Regex("[0-9a-f-]{36}")) }
			?.let { File(context.filesDir, "save-draft-$it.json") }
}
