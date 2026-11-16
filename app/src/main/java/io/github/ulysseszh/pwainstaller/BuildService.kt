package io.github.ulysseszh.pwainstaller

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.ulysseszh.pwainstaller.core.ApkProgress

/** One notification per operation, with the foreground slot belonging to one of those operations. */
class BuildService : Service() {
	private data class Status(
		val name: String,
		val text: String,
		val percent: Int?,
		val confirmation: Boolean = false,
	)

	private val handler = Handler(Looper.getMainLooper())
	private val poll = Runnable { update() }
	private var notifications = emptySet<Int>()
	private val ids = mutableMapOf<String, Int>()
	private var nextId = 10000
	private var primary: String? = null
	private var states = emptyMap<String, Status>()
	private var foregroundStatus: Status? = null
	private var foregroundStarted = false
	private val notificationUpdates = ProgressNotificationUpdates<Status>()
	private val statusWorker =
		java.util.concurrent.Executors
			.newSingleThreadExecutor()
	private var scanning = false
	private var advancing = false
	private var cpuNeeded = false
	private var destroyed = false
	private var revision = 0
	private var lastStartId = 0
	private lateinit var cpu: PowerManager.WakeLock
	private var lastWake = 0L

	override fun onBind(intent: Intent?) = null

	override fun onCreate() {
		super.onCreate()
		cpu =
			(getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:apk-work").apply {
				setReferenceCounted(false)
			}
		running = this
		// Recover child notifications left by a killed service, without touching filter downloads.
		if (Build.VERSION.SDK_INT >= 23) {
			val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
			manager.activeNotifications
				.filter {
					it.id in 10000 until 0x40000000 &&
						(Build.VERSION.SDK_INT < 26 || it.notification.channelId == ProgressNotifications.BUILDS)
				}.forEach { manager.cancel(it.id) }
		}
	}

	private fun notification(state: Status?) =
		ProgressNotifications.create(
			this,
			ProgressNotifications.BUILDS,
			state?.name ?: getString(R.string.build_notifications),
			state?.text ?: getString(R.string.build_preparing),
			state?.percent,
			confirmation =
				state?.confirmation == true,
		)

	override fun onStartCommand(
		intent: Intent?,
		flags: Int,
		id: Int,
	): Int {
		lastStartId = id
		revision++
		val first =
			states[primary]
				?: BuildJobs.active().firstOrNull()?.let {
					Status(
						it.name,
						BuildJobs.label(this, it),
						if (it.queued) {
							0
						} else {
							it.progress.overallPercent
						},
					)
				}
				?: intent?.getStringExtra("appName")?.let { Status(it, getString(intent.getIntExtra("stageLabel", R.string.build_preparing)), null) }
				?: foregroundStatus
		if (!foregroundStarted) {
			foregroundStatus = first
			startForeground(NOTIFICATION, notification(first))
			foregroundStarted = true
			if (first != null) {
				notificationUpdates.record(NOTIFICATION, first, android.os.SystemClock.elapsedRealtime())
			}
		}
		keepCpu(true)
		update()
		return START_STICKY
	}

	private fun builds() =
		BuildJobs.all().filterValues { !it.done }.mapValues { (_, state) ->
			Status(
				state.name,
				BuildJobs.label(this, state),
				if (state.queued) {
					0
				} else {
					state.progress.overallPercent
				},
			)
		}

	private fun snapshot(): Map<String, Status> {
		// An install build writes "ready" before finishing. Reading builds first avoids
		// a gap between in-memory build progress and the durable installation journal.
		val builds = builds()
		val result = linkedMapOf<String, Status>()
		for (token in InstallCoordinator.tokens(this)) {
			runCatching {
				val plan = InstallCoordinator.summary(this, token)
				if (plan.optBoolean("profileOnly")) {
					return@runCatching
				}
				val progress = InstallCoordinator.progress(token)
				val label =
					when (plan.getString("state")) {
						"ready" -> {
							if (progress?.stage == ApkProgress.Stage.INSTALLING) {
								R.string.build_installing
							} else {
								R.string.build_ready
							}
						}

						"committed" -> {
							if (progress?.stage == ApkProgress.Stage.INSTALLING) {
								R.string.build_installing
							} else {
								R.string.build_waiting
							}
						}

						"confirmation" -> {
							R.string.install_confirmation_needed
						}

						"installed" -> {
							R.string.install_finishing
						}

						else -> {
							return@runCatching
						}
					}
				result["install:$token"] =
					Status(
						WebApp.name(plan.getJSONObject("app")),
						getString(label),
						progress?.takeIf { it.stage == ApkProgress.Stage.INSTALLING }?.percent,
						plan.getString("state") == "confirmation",
					)
			}
		}
		result.putAll(builds)
		return result
	}

	private fun update() {
		handler.removeCallbacks(poll)
		if (destroyed) {
			return
		}
		if (!advancing) {
			advancing = true
			keepCpu(true)
			InstallerApplication.installPipeline.execute {
				runCatching { InstallPipeline.advance(this) { keepCpu(true) } }
					.onFailure { android.util.Log.e("PwaBuildService", "Cannot advance installation queue", it) }
				handler.post {
					if (!destroyed) {
						advancing = false
						scan()
					}
				}
			}
		}
		// Do not wait for APK submission, data migration, or another installation
		// pipeline task to publish the builders' in-memory progress.
		states = states + builds()
		render(allowStop = false)
		scan()
		handler.postDelayed(poll, 500)
	}

	private fun scan() {
		if (scanning || destroyed) {
			return
		}
		scanning = true
		val version = revision
		statusWorker.execute {
			val snapshot =
				runCatching {
					snapshot() to InstallPipeline.needsCpu(this)
				}
			handler.post {
				scanning = false
				if (destroyed) {
					return@post
				}
				snapshot.onSuccess { (next, working) ->
					cpuNeeded = working
					states = next + builds()
					keepCpu(advancing || cpuNeeded || BuildJobs.active().any { !it.queued })
					// Starts arriving during a scan must not discard progress for
					// other jobs, or let a stale empty scan stop the service.
					render(allowStop = version == revision && !advancing)
				}
				if (version != revision) {
					scan()
				}
			}
		}
	}

	@Synchronized private fun keepCpu(working: Boolean) {
		if (!::cpu.isInitialized) {
			return
		}
		if (working && !destroyed) {
			val now = android.os.SystemClock.elapsedRealtime()
			if (!cpu.isHeld || now - lastWake >= 60000) {
				cpu.acquire(10 * 60 * 1000L)
				lastWake = now
			}
		} else if (cpu.isHeld) {
			cpu.release()
		}
	}

	private fun render(allowStop: Boolean) {
		val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
		if (primary !in states) {
			primary = states.keys.firstOrNull()
		}
		val children = states.filterKeys { it != primary }.mapKeys { (key, _) -> ids.getOrPut(key) { nextId++ } }
		for (id in notifications - children.keys) {
			manager.cancel(id)
		}
		notifications = children.keys
		// A newer start can already be pending while this snapshot is delivered.
		// Stop only the request we have handled, so that new work keeps its service.
		if (states.isEmpty()) {
			if (allowStop) {
				foregroundStarted = false
				ServiceCompat
					.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
				manager.cancel(NOTIFICATION)
				stopSelf(lastStartId)
			}
			return
		}
		foregroundStatus = states.getValue(primary!!)
		if (Build.VERSION.SDK_INT >= 23) {
			val visible = manager.activeNotifications.map { it.id }.toSet()
			notificationUpdates.retainVisible(visible, android.os.SystemClock.elapsedRealtime())
			if (NOTIFICATION !in visible && !notificationUpdates.isPublished(NOTIFICATION)) {
				startForeground(NOTIFICATION, notification(foregroundStatus))
				foregroundStarted = true
				notificationUpdates.record(NOTIFICATION, foregroundStatus!!, android.os.SystemClock.elapsedRealtime())
			}
		}
		val desired = linkedMapOf(NOTIFICATION to foregroundStatus!!).apply { putAll(children) }
		notificationUpdates.select(desired, android.os.SystemClock.elapsedRealtime()).forEach { (id, state) ->
			manager.notify(id, notification(state))
		}
		ids.keys.retainAll(states.keys)
	}

	override fun onTimeout(
		startId: Int,
		fgsType: Int,
	) {
		foregroundStarted = false
		ServiceCompat
			.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
		stopSelf()
	}

	override fun onDestroy() {
		destroyed = true
		handler.removeCallbacksAndMessages(null)
		statusWorker.shutdownNow()
		if (running === this) {
			running = null
		}
		keepCpu(false)
		val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
		notifications.forEach { manager.cancel(it) }
		ServiceCompat
			.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
		manager.cancel(NOTIFICATION)
		super.onDestroy()
	}

	companion object {
		private const val NOTIFICATION = 1901

		@Volatile private var running: BuildService? = null

		fun start(
			context: Context,
			name: String? = null,
			label: Int = R.string.build_preparing,
		) {
			ContextCompat.startForegroundService(
				context,
				Intent(context, BuildService::class.java).putExtra("appName", name).putExtra("stageLabel", label),
			)
		}

		fun signal(context: Context) {
			val service = running
			if (service != null && !service.destroyed) {
				// Take over from the installer broadcast before its CPU allowance ends.
				service.keepCpu(true)
				service.handler.post {
					service.revision++
					service.update()
				}
			} else {
				runCatching { start(context) }
			}
		}
	}
}
