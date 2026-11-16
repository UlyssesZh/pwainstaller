package io.github.ulysseszh.pwainstaller

import android.content.Context
import io.github.ulysseszh.pwainstaller.core.DataArchive
import java.io.File

object Profiles {
	fun export(
		context: Context,
		packageName: String,
		destination: File,
	) {
		destination.parentFile!!.mkdirs()
		requireNotNull(context.contentResolver.openInputStream(Packages.profile(packageName))).use { input ->
			destination.outputStream().use { DataArchive.copy(input, it) }
		}
	}

	fun import(
		context: Context,
		packageName: String,
		source: File,
	) {
		source.inputStream().use { input ->
			requireNotNull(context.contentResolver.openOutputStream(Packages.profile(packageName), "w")).use { DataArchive.copy(input, it) }
		}
		context.contentResolver.call(Packages.profile(packageName), "commit", null, null)
	}
}
