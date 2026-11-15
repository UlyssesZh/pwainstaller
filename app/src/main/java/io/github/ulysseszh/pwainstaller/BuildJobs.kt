package io.github.ulysseszh.pwainstaller

import android.content.Context
import io.github.ulysseszh.pwainstaller.core.ApkProgress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Builds never occupy the UI's metadata worker. Progress and logs belong to the app, not an activity. */
object BuildJobs {
    data class State(val key: String, val name: String, val queued: Boolean = true,
        val progress: ApkProgress = ApkProgress(ApkProgress.Stage.PREPARING),
        val done: Boolean = false, val notice: Int? = null, val error: Throwable? = null,
        val entry: AppDatabase.Summary? = null)
    private val jobs = ConcurrentHashMap<String, State>()
    private val executor = ThreadPoolExecutor(BuildSettings.defaultBuilders, BuildSettings.defaultBuilders,
        0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
    @Synchronized fun configure(count: Int) {
        require(count in 1..BuildSettings.MAX_BUILDERS)
        if (count > executor.maximumPoolSize) executor.maximumPoolSize = count
        executor.corePoolSize = count
        if (count < executor.maximumPoolSize) executor.maximumPoolSize = count
    }
    fun get(id: String) = jobs[id]
    fun all(): Map<String, State> = jobs.toMap()
    fun forApp(key: String) = jobs.values.firstOrNull { it.key == key && !it.done }
    fun active() = jobs.values.filterNot { it.done }
    fun remove(id: String) { jobs.remove(id) }
    @Synchronized fun start(context: Context, id: String, key: String, name: String,
        entry: AppDatabase.Summary? = null,
        initialProgress: ApkProgress = ApkProgress(ApkProgress.Stage.PREPARING),
        task: ((ApkProgress) -> Unit, (String) -> Unit) -> Int?): Boolean {
        if (jobs.containsKey(id) || forApp(key) != null) return false
        val appContext = context.applicationContext
        configure(BuildSettings.builders(appContext))
        val initial = State(key, name, progress = initialProgress, entry = entry)
        jobs[id] = initial
        try { BuildService.start(appContext, name, R.string.build_queued) }
        catch (failure: Exception) { jobs.remove(id); throw failure }
        executor.execute {
            jobs[id] = initial.copy(queued = false)
            try {
                BuildLogs.begin(appContext, key).use { output ->
                    val log: (String) -> Unit = { BuildLogs.line(output, it) }
                    log("Building $name; app=$key; job=$id")
                    var previous: ApkProgress? = null
                    try {
                        val notice = task({ progress ->
                            jobs[id] = initial.copy(queued = false, progress = progress)
                            if (previous != progress) { log("${progress.stage}${progress.percent?.let { " $it%" }.orEmpty()}"); previous = progress }
                        }, log)
                        log("Build completed successfully")
                        jobs[id] = initial.copy(queued = false, done = true, notice = notice)
                    } catch (failure: Exception) {
                        log("Build failed: $failure"); failure.printStackTrace(output); output.flush(); throw failure
                    }
                }
            } catch (failure: Exception) { jobs[id] = initial.copy(queued = false, done = true, error = failure) }
        }
        return true
    }
    fun label(context: Context, state: State): String {
        if (state.queued) return context.getString(R.string.build_queued)
        val stage = when (state.progress.stage) {
            ApkProgress.Stage.PREPARING -> R.string.build_preparing
            ApkProgress.Stage.VALIDATING -> R.string.build_validating
            ApkProgress.Stage.SAVING -> R.string.build_saving
            ApkProgress.Stage.KEY -> R.string.loading_signing_key
            ApkProgress.Stage.FILTERS -> R.string.build_filters
            ApkProgress.Stage.TEMPLATE -> R.string.build_template
            ApkProgress.Stage.ICONS -> R.string.build_icons
            ApkProgress.Stage.RESOURCES -> R.string.build_resources
            ApkProgress.Stage.SIGNING -> R.string.build_signing
            ApkProgress.Stage.BACKUP -> R.string.build_backup
            ApkProgress.Stage.INSTALLING -> R.string.build_installing
            ApkProgress.Stage.WAITING -> R.string.build_waiting
        }
        return context.getString(stage) + " · ${state.progress.overallPercent}%"
    }
}
