package io.github.ulysseszh.pwainstaller

import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONObject

/** Advances only installations the user has already started, independently of activities. */
object InstallPipeline {
	private val processing = Any()

	private fun plans(context: Context) =
		InstallCoordinator.tokens(context).mapNotNull { token ->
			try {
				token to InstallCoordinator.summary(context, token)
			} catch (_: java.io.FileNotFoundException) {
				null
			}
		}

	fun allowed(context: Context): Boolean =
		if (InstallationSettings.method(context) == "shizuku") {
			Build.VERSION.SDK_INT >= 24 && ShizukuInstaller.authorized()
		} else {
			Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()
		}

	fun start(
		context: Context,
		token: String,
	) {
		val plan = InstallCoordinator.authorizeBackground(context, token)
		if (plan.getJSONObject("app").getString("key") in ReinstallQueue.pendingKeys(context)) {
			ReinstallQueue.allowBackground(context)
		}
		try {
			if (plan.getString("state") == "prepared") {
				build(context, token, plan)
			} else {
				BuildService.start(context)
			}
		} catch (failure: Exception) {
			// If Android refuses a new foreground start, retain an explicit retry
			// rather than leaving an authorized request looking as if it is running.
			runCatching {
				InstallCoordinator.interrupt(context, token)
				ReinstallQueue.pause(context, token)
			}
			context.sendBroadcast(Intent(InstallReceiver.CHANGED).setPackage(context.packageName))
			throw failure
		}
	}

	private fun build(
		context: Context,
		token: String,
		plan: JSONObject,
	): Boolean {
		val app = plan.getJSONObject("app")
		return BuildJobs.start(context, "install:$token", app.getString("key"), WebApp.name(app)) { progress, log ->
			try {
				InstallCoordinator.prepareApk(context, token, progress, log)
			} catch (
				failure: Exception,
			) {
				runCatching { InstallCoordinator.fail(context, token, failure.message) }
				throw failure
			}
			null
		}
	}

	private fun paused(
		context: Context,
		plan: JSONObject,
	) = ReinstallQueue.paused(context) &&
		plan.getJSONObject("app").getString("key") in ReinstallQueue.pendingKeys(context)

	private fun managed(plan: JSONObject) = plan.optBoolean("background") && !plan.optBoolean("interrupted")

	fun advance(
		context: Context,
		wake: () -> Unit,
	): Boolean =
		synchronized(processing) {
			var changed = false

			fun step(
				token: String,
				action: () -> Unit,
			) {
				try {
					action()
				} catch (
					_: java.io.FileNotFoundException,
				) {
				} catch (failure: Exception) {
					runCatching { InstallCoordinator.fail(context, token, failure.toString()) }
				}
				changed = true
			}
			var current = plans(context)
			for ((token, plan) in current) {
				if (!plan.optBoolean("background")) {
					continue
				}
				val job = BuildJobs.get("install:$token")
				if (job?.done == true) {
					BuildJobs.remove("install:$token")
					changed = true
				}
				if (job != null && !job.done) {
					continue
				}
				when (plan.getString("state")) {
					"installed" -> {
						step(token) {
							wake()
							InstallCoordinator.complete(context, token)
						}
					}

					"done" -> {
						if (ReinstallQueue.active(context) == token) {
							ReinstallQueue.complete(context, token)
							changed = true
						}
					}

					"failed", "replacement_failed", "signature_conflict", "backup_failed", "transfer_failed", "awaiting_uninstall" -> {
						if (ReinstallQueue.active(context) == token &&
							!ReinstallQueue.paused(context)
						) {
							ReinstallQueue.pause(context, token)
							changed = true
						}
					}
				}
			}
			current = plans(context)
			// Completion can leave the head ready to advance in this same pass.
			current
				.filter { it.second.optBoolean("background") && it.second.getString("state") == "done" }
				.forEach { (token, _) ->
					if (ReinstallQueue.active(context) == token) {
						ReinstallQueue.complete(context, token)
						changed = true
					}
				}
			val permitted = allowed(context)
			if (permitted && ReinstallQueue.backgroundAllowed(context) && ReinstallQueue.has(context) && !ReinstallQueue.paused(context)) {
				val previous = ReinstallQueue.active(context)
				if (previous == null) {
					wake()
				}
				ReinstallQueue.next(context)
				if (previous != ReinstallQueue.active(context)) {
					changed = true
				}
				val existing = plans(context).associate { (token, plan) -> plan.getJSONObject("app").getString("key") to token }
				for (key in ReinstallQueue.pendingKeys(context).take(BuildSettings.builders(context))) {
					val token =
						existing[key] ?: run {
							wake()
							AppDatabase(context).use { it.get(key) }?.let { InstallCoordinator.prepare(context, it, null) }
						} ?: continue
					var plan = InstallCoordinator.summary(context, token)
					if (!plan.optBoolean("background") && !plan.optBoolean("interrupted")) {
						plan = InstallCoordinator.authorizeBackground(context, token)
						changed = true
					}
					// Loading and validating later apps can be expensive. Submit each
					// prepared request immediately so those reads do not idle builders.
					if (managed(plan) && !paused(context, plan) && plan.getString("state") == "prepared" && BuildJobs.get("install:$token") == null) {
						step(token) {
							wake()
							build(context, token, plan)
						}
					}
				}
			}
			current = plans(context)
			for ((token, plan) in current) {
				if (!managed(plan) || paused(context, plan)) {
					continue
				}
				if (!permitted && plan.getString("state") in listOf("prepared", "ready")) {
					step(token) {
						InstallCoordinator.interrupt(context, token)
						ReinstallQueue.pause(context, token)
					}
					continue
				}
				if (plan.getString("state") == "prepared" && BuildJobs.get("install:$token") == null) {
					step(token) {
						wake()
						build(context, token, plan)
					}
				}
			}
			current = plans(context)
			if (permitted && current.none { it.second.getString("state") in listOf("committed", "confirmation", "installed") }) {
				val ready =
					current.firstOrNull { (token, plan) ->
						managed(plan) && !paused(context, plan) && plan.getString("state") == "ready" && BuildJobs.get("install:$token")?.done != false &&
							(plan.getJSONObject("app").getString("key") !in ReinstallQueue.pendingKeys(context) || ReinstallQueue.active(context) == token)
					}
				ready?.let { (token, _) ->
					step(token) {
						wake()
						InstallCoordinator.installPrepared(context, token)
					}
				}
			}
			if (changed) {
				context.sendBroadcast(Intent(InstallReceiver.CHANGED).setPackage(context.packageName))
			}
			changed
		}

	fun needsCpu(context: Context): Boolean {
		if (BuildJobs.active().any { !it.queued }) {
			return true
		}
		val current = plans(context)
		val permitted = allowed(context)
		val waiting = current.any { it.second.getString("state") in listOf("committed", "confirmation", "installed") }
		return current.any { (_, plan) ->
			managed(plan) &&
				when (plan.getString("state")) {
					"installed" -> {
						true
					}

					"prepared" -> {
						permitted && !paused(context, plan)
					}

					"ready" -> {
						permitted && !waiting && !paused(context, plan)
					}

					else -> {
						false
					}
				}
		}
	}
}
