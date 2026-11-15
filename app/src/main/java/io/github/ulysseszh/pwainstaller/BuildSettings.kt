package io.github.ulysseszh.pwainstaller

import android.content.Context

object BuildSettings {
	const val MAX_BUILDERS = 16
	val defaultBuilders get() = Runtime.getRuntime().availableProcessors().coerceIn(1, 2)
	fun builders(context: Context) = context.getSharedPreferences("installation", 0)
		.getInt("builders", defaultBuilders).coerceIn(1, MAX_BUILDERS)
	fun save(context: Context, count: Int) {
		require(count in 1..MAX_BUILDERS)
		context.getSharedPreferences("installation", 0).edit().putInt("builders", count).apply()
		BuildJobs.configure(count)
	}
}
