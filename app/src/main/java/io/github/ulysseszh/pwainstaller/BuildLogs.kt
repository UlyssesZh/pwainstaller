package io.github.ulysseszh.pwainstaller

import android.content.Context
import io.github.ulysseszh.pwainstaller.core.PackageNames
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BuildLogs {
	fun file(
		context: Context,
		key: String,
	): File =
		File(
			context.filesDir,
			"build-logs/${PackageNames.digest("SHA-256", key.toByteArray(Charsets.UTF_8))}.log",
		)

	fun begin(
		context: Context,
		key: String,
	): PrintWriter {
		val file = file(context, key)
		check(file.parentFile!!.mkdirs() || file.parentFile!!.isDirectory)
		return PrintWriter(file.bufferedWriter())
	}

	fun line(
		output: PrintWriter,
		message: String,
	) {
		output.println("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT).format(Date())}  $message")
		output.flush()
	}
}
