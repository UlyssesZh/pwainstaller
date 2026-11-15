package io.github.ulysseszh.pwainstaller

import android.content.Context
import androidx.work.*
import io.github.ulysseszh.pwainstaller.core.AdblockRules
import io.github.ulysseszh.pwainstaller.core.PackageNames
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** Global downloads are a source for defaults; saved web-app rules are independent snapshots. */
object AdblockSettings {
    const val FETCH_TAG = "adguard-fetch"
    const val FETCH_WORK = "adguard-update"
    data class FetchProgress(val index: Int, val count: Int, val url: String, val received: Long = 0, val total: Long? = null) {
        val percent: Int? get() = if (count == 0) 100 else total?.takeIf { it > 0 }?.let {
            ((index * 100L + (received * 100 / it).coerceIn(0, 100)) / count).toInt()
        }
    }
    private val cacheLock = Any()
    private data class SourceStamp(val path: String, val size: Long, val modified: Long)
    private data class Selection(val host: String, val sources: List<SourceStamp>, val rules: String)
    // Retain only the last host's snapshot, rather than every site's potentially multi-MiB rules.
    private var selection: Selection? = null
    private var cacheRevision = 0L
    private val defaultUrls = io.github.ulysseszh.pwainstaller.core.FilterSources.DEFAULT
    private fun prefs(context: Context) = context.getSharedPreferences("adblock", 0)
    fun urls(context: Context): String = prefs(context).getString("urls", defaultUrls)!!
    fun hours(context: Context): Long = prefs(context).getLong("hours", 168)
    fun updated(context: Context): Long = prefs(context).getLong("updated", 0)
    fun invalidate(context: Context) { synchronized(cacheLock) { selection = null; cacheRevision++ }; schedule(context) }
    private fun parseUrls(text: String) = io.github.ulysseszh.pwainstaller.core.FilterSources.enabled(text)
    private fun file(context: Context, url: String): File = File(context.filesDir, "filters/${PackageNames.digest("SHA-256", url.toByteArray())}.txt")
    fun save(context: Context, urls: String, hours: Long) {
        val sources = parseUrls(urls)
        require(hours == 0L || hours in 1..8760) { "Choose a valid filter update interval" }
        val value = io.github.ulysseszh.pwainstaller.core.FilterSources.normalize(urls)
        if (urls(context) == value && hours(context) == hours) return
        val editor = prefs(context).edit().putString("urls", value).putLong("hours", hours)
        if (parseUrls(urls(context)) != sources) editor.putLong("updated", 0)
        editor.apply()
        schedule(context)
    }
    @Synchronized fun refresh(context: Context, progress: (FetchProgress) -> Unit = {}) {
        val sources = parseUrls(urls(context))
        val staged = mutableListOf<Pair<File, File>>()
        try {
            for ((index, url) in sources.withIndex()) {
                val bytes = ManifestFetcher.get(url, emptyMap(), 8 * 1024 * 1024, url,
                    onProgress = { received, total -> progress(FetchProgress(index, sources.size, url, received, total)) }).bytes
                val text = bytes.toString(Charsets.UTF_8)
                require(!text.trimStart().startsWith('<')) { "Filter URL returned a web page" }
                require(AdblockRules.lines(text).isNotEmpty()) { "Filter list is empty" }
                val destination = file(context, url).apply { parentFile!!.mkdirs() }
                val part = File(destination.path + ".tmp").apply { writeBytes(bytes) }
                staged.add(part to destination)
            }
            progress(FetchProgress(sources.size, sources.size, "", 0, 1))
            // Downloads must all succeed before replacing any previous list.
            synchronized(cacheLock) {
                selection = null; cacheRevision++
                staged.forEach { (part, destination) -> check(part.renameTo(destination)) { "Cannot save filter list" } }
                if (parseUrls(urls(context)) == sources) check(prefs(context).edit().putLong("updated", System.currentTimeMillis()).commit())
            }
        } finally { staged.forEach { it.first.delete() } }
    }
    /** This path never downloads, and never waits for a global download's network lock. */
    fun cachedDefaults(context: Context, baseUrl: String): String {
        val host = AdblockRules.host(baseUrl)
        require(host.isNotEmpty()) { "Enter a base URL first" }
        val (sources, lists, revision) = synchronized(cacheLock) {
            val sources = sourceStamps(context)
            selection?.takeIf { it.host == host && it.sources == sources }?.let { return it.rules }
            Triple(sources, sources.mapNotNull { File(it.path).takeIf(File::isFile)?.readText() }, cacheRevision)
        }
        val result = lists.asSequence().flatMap { AdblockRules.select(it, baseUrl).lineSequence() }
            .filter(String::isNotEmpty).distinct().joinToString("\n")
        AdblockRules.validate(result)
        synchronized(cacheLock) {
            if (revision == cacheRevision && sources == sourceStamps(context)) selection = Selection(host, sources, result)
        }
        return result
    }
    private fun sourceStamps(context: Context) = parseUrls(urls(context)).map { url ->
        file(context, url).let { SourceStamp(it.absolutePath, it.length(), it.lastModified()) }
    }
    fun schedule(context: Context) {
        val manager = WorkManager.getInstance(context)
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val interval = hours(context)
        if (interval == 0L) manager.cancelUniqueWork("adguard-periodic")
        else manager.enqueueUniquePeriodicWork("adguard-periodic", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<AdblockUpdateWorker>(interval, TimeUnit.HOURS).setInitialDelay(interval, TimeUnit.HOURS)
                .setConstraints(constraints).addTag(FETCH_TAG).build())
        // Initial and overdue downloads run independently of editor/fetch work.
        if (interval > 0 && (updated(context) == 0L || System.currentTimeMillis() - updated(context) >= TimeUnit.HOURS.toMillis(interval)))
            fetchNow(context)
    }
    fun fetchNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(FETCH_WORK, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<AdblockUpdateWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .addTag(FETCH_TAG).addTag(FETCH_WORK).build())
    }
}

class AdblockUpdateWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    private val notificationId = 0x40000000 or (id.hashCode() and 0x3fffffff)
    private val notificationLock = Any()
    private fun clearNotification() = synchronized(notificationLock) {
        (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).cancel(notificationId)
    }
    override fun onStopped() { clearNotification(); super.onStopped() }
    override fun doWork(): Result {
        var lastUpdate = 0L
        var previousIndex = -1
        var previousDeterminate = false
        var previousReceived = 0L
        fun report(progress: AdblockSettings.FetchProgress) {
            if (isStopped) throw java.util.concurrent.CancellationException()
            val now = android.os.SystemClock.elapsedRealtime()
            val determinate = progress.percent != null
            val firstBytes = previousReceived == 0L && progress.received > 0
            if (progress.index == previousIndex && determinate == previousDeterminate && !firstBytes && now - lastUpdate < 250 && progress.percent != 100) return
            previousIndex = progress.index; previousDeterminate = determinate; previousReceived = progress.received; lastUpdate = now
            val context = applicationContext
            val text = if (progress.index == progress.count) context.getString(R.string.filters_saving)
                else context.getString(R.string.filters_fetch_progress, progress.index + 1, progress.count, android.net.Uri.parse(progress.url).host.orEmpty())
            val notification = ProgressNotifications.create(context, ProgressNotifications.FILTERS,
                context.getString(R.string.filter_notifications), text, progress.percent, settings = true)
            // WorkManager owns this short network job. An ordinary progress notification
            // also works for scheduled updates that cannot start a foreground service.
            synchronized(notificationLock) {
                if (!isStopped && (android.os.Build.VERSION.SDK_INT < 33 || androidx.core.content.ContextCompat.checkSelfPermission(context,
                        android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED))
                    (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).notify(notificationId, notification)
            }
            setProgressAsync(Data.Builder().putString("text", text).putInt("percent", progress.percent ?: -1).build()).get()
        }
        return try { AdblockSettings.refresh(applicationContext, ::report); Result.success() }
        catch (error: Exception) {
            if (isStopped) Result.failure()
            else if (runAttemptCount < 3) Result.retry()
            else Result.failure(Data.Builder().putString("error", error.message ?: error.toString()).build())
        } finally { clearNotification() }
    }
}
