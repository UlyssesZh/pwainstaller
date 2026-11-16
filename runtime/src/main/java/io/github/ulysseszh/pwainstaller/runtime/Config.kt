package io.github.ulysseszh.pwainstaller.runtime

import android.content.Context
import org.json.JSONObject

object Config {
	fun read(context: Context): JSONObject =
		context.assets
			.open("webapp.json")
			.bufferedReader(Charsets.UTF_8)
			.use { JSONObject(it.readText()) }
}
