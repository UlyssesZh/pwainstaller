package io.github.ulysseszh.pwainstaller

import android.content.Context

object InstallationSettings {
	fun method(context: Context): String = context.getSharedPreferences("installation", 0).getString("method", "system")!!

	fun save(
		context: Context,
		method: String,
	) {
		require(method in setOf("system", "shizuku"))
		require(method != "shizuku" || android.os.Build.VERSION.SDK_INT >= 24) { "Shizuku requires Android 7 or newer" }
		context
			.getSharedPreferences("installation", 0)
			.edit()
			.putString("method", method)
			.apply()
	}
}
