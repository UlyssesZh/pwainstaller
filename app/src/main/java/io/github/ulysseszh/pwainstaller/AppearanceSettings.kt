package io.github.ulysseszh.pwainstaller

import android.content.Context

object AppearanceSettings {
	fun layout(context: Context) = context.getSharedPreferences("appearance", 0).getString("layout", "list")!!

	fun save(
		context: Context,
		value: String,
	) {
		require(value in listOf("list", "tiles"))
		context
			.getSharedPreferences("appearance", 0)
			.edit()
			.putString("layout", value)
			.apply()
	}
}
