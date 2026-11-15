package io.github.ulysseszh.pwainstaller

import android.content.Context
import org.json.JSONArray

/** One installation at a time, with the queue retained across confirmations and process restarts. */
object ReinstallQueue {
	private fun prefs(context: Context) = context.getSharedPreferences("reinstall-all", 0)
	private fun keys(context: Context): JSONArray = JSONArray(prefs(context).getString("keys", "[]"))
	fun pendingKeys(context: Context): List<String> = keys(context).let { value -> (0 until value.length()).map(value::getString) }
	fun has(context: Context): Boolean = keys(context).length() > 0
	fun active(context: Context): String? = prefs(context).getString("active", null)
	fun paused(context: Context): Boolean = prefs(context).getBoolean("paused", false)
	fun backgroundAllowed(context: Context): Boolean = prefs(context).getBoolean("background", false)
	fun allowBackground(context: Context) { check(prefs(context).edit().putBoolean("background", true).commit()) }
	@Synchronized fun begin(context: Context) {
		check(!has(context)) { "A reinstall queue is already pending" }
		check(InstallCoordinator.tokens(context).isEmpty()) { context.getString(R.string.finish_pending_first) }
		val apps = AppDatabase(context).use { it.summaries() }
		check(apps.isNotEmpty()) { context.getString(R.string.empty_title) }
		check(prefs(context).edit().putString("keys", JSONArray(apps.map { it.key }).toString())
			.remove("active").putBoolean("paused", false).putBoolean("background", false).commit())
	}
	@Synchronized fun next(context: Context): String? {
		if (paused(context)) return null
		active(context)?.let { token ->
			if (token in InstallCoordinator.tokens(context)) return token
			pause(context, token)
			return null
		}
		val keys = keys(context)
		while (keys.length() > 0) {
			val key = keys.getString(0)
			val app = AppDatabase(context).use { it.get(key) }
			if (app == null) { keys.remove(0); check(prefs(context).edit().putString("keys", keys.toString()).commit()); continue }
			val existing = InstallCoordinator.tokens(context).find {
				InstallCoordinator.summary(context, it).getJSONObject("app").getString("key") == key
			}
			val token = existing ?: InstallCoordinator.prepare(context, app, null)
			check(prefs(context).edit().putString("active", token).commit())
			return token
		}
		return null
	}
	@Synchronized fun complete(context: Context, token: String) {
		if (active(context) != token) return
		val keys = keys(context)
		if (keys.length() > 0) keys.remove(0)
		check(prefs(context).edit().putString("keys", keys.toString()).remove("active").commit())
	}
	@Synchronized fun pause(context: Context, token: String, forgetOperation: Boolean = false) {
		if (active(context) != token) return
		val edit = prefs(context).edit().putBoolean("paused", true)
		if (forgetOperation || token !in InstallCoordinator.tokens(context)) edit.remove("active")
		check(edit.commit())
	}
	fun resume(context: Context) {
		val edit = prefs(context).edit().putBoolean("paused", false)
		if (active(context)?.let { it !in InstallCoordinator.tokens(context) } == true) edit.remove("active")
		check(edit.commit())
	}
}
