package io.github.ulysseszh.pwainstaller

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

object UserContentLibrary {
	private fun file(context: Context) = AtomicFile(File(context.filesDir, "user-content.json"))

	@Synchronized fun load(context: Context): List<UserContentFiles.Entry> {
		val file = file(context)
		if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) {
			return emptyList()
		}
		return file.openRead().use { UserContentFiles.read(JSONObject(String(ManifestFetcher.read(it, 64 * 1024 * 1024), Charsets.UTF_8))) }
	}

	@Synchronized fun save(
		context: Context,
		entry: UserContentFiles.Entry,
	) {
		entry.validate()
		val files = load(context).toMutableList()
		val index = files.indexOfFirst { it.id == entry.id }
		if (index >= 0) {
			files[index] = entry
		} else {
			files.add(entry)
		}
		require(files.size <= 64) { "The library supports at most 64 files" }
		write(context, files)
	}

	@Synchronized fun remove(
		context: Context,
		id: String,
	) = write(context, load(context).filterNot { it.id == id })

	@Synchronized fun replace(
		context: Context,
		files: List<UserContentFiles.Entry>,
	) {
		require(files.size <= 64)
		files.forEach { it.validate() }
		write(context, files)
	}

	private fun write(
		context: Context,
		files: List<UserContentFiles.Entry>,
	) {
		val data = JSONObject()
		UserContentFiles.put(data, files)
		val bytes = data.toString().toByteArray(Charsets.UTF_8)
		require(bytes.size <= 64 * 1024 * 1024) { "The user file library exceeds 64 MiB" }
		val file = file(context)
		val output = file.startWrite()
		try {
			output.write(bytes)
			file.finishWrite(output)
		} catch (failure: Exception) {
			file.failWrite(output)
			throw failure
		}
	}
}
