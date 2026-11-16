package io.github.ulysseszh.pwainstaller.core

import java.io.*
import java.util.UUID
import java.util.zip.*

/** Portable private WebView profile. Validation finishes before any live directory is replaced. */
object DataArchive {
	private const val LIMIT = 512L * 1024 * 1024
	private const val MARKER = "pwa-profile-v1"
	private val roots = listOf("app_webview", "shared_prefs", "databases")

	@JvmStatic
	@Throws(IOException::class)
	fun exportProfile(
		dataDir: File,
		stream: OutputStream,
	) {
		val canonical = dataDir.canonicalFile
		ZipOutputStream(stream).use { output ->
			output.putNextEntry(ZipEntry(MARKER))
			output.closeEntry()
			roots.forEach { add(canonical, File(canonical, it), output) }
		}
	}

	private fun add(
		base: File,
		file: File,
		output: ZipOutputStream,
	) {
		if (!file.exists()) {
			return
		}
		checkIo(file.canonicalPath == file.absolutePath, "Symbolic link in profile")
		val name = base.toURI().relativize(file.toURI()).path
		if (file.isDirectory) {
			output.putNextEntry(
				ZipEntry(
					if (name.endsWith('/')) {
						name
					} else {
						"$name/"
					},
				),
			)
			output.closeEntry()
			(file.listFiles() ?: throw IOException("Unreadable profile directory")).forEach { add(base, it, output) }
		} else {
			if (file.name.startsWith("Singleton")) {
				return
			}
			output.putNextEntry(ZipEntry(name))
			file.inputStream().use { copy(it, output) }
			output.closeEntry()
		}
	}

	@JvmStatic
	@Throws(IOException::class)
	fun importProfile(
		dataDir: File,
		stream: InputStream,
	) {
		val stage = File(dataDir, ".profile-stage-${UUID.randomUUID()}")
		checkIo(stage.mkdirs(), "Cannot stage profile")
		val backup = File(stage, "backup")
		val names = mutableSetOf<String>()
		var marker = false
		var total = 0L
		var preserveBackup = false
		try {
			ZipInputStream(stream).use { input ->
				val buffer = ByteArray(32768)
				while (true) {
					val entry = input.nextEntry ?: break
					val name = entry.name
					checkIo(names.add(name) && names.size <= 100000, "Duplicate/excessive archive entries")
					if (name == MARKER) {
						checkIo(input.read() == -1, "Invalid profile marker")
						marker = true
						continue
					}
					checkIo(allowed(name), "Invalid profile path: $name")
					val target = File(stage, name)
					checkIo(target.canonicalPath.startsWith(stage.canonicalPath + File.separator), "Path traversal")
					if (entry.isDirectory) {
						checkIo(target.mkdirs() || target.isDirectory, "Cannot create directory")
						continue
					}
					checkIo(target.parentFile!!.mkdirs() || target.parentFile!!.isDirectory, "Cannot create directory")
					target.outputStream().use { output ->
						while (true) {
							val n = input.read(buffer)
							if (n == -1) {
								break
							}
							total += n
							checkIo(total <= LIMIT, "Profile exceeds 512 MiB")
							output.write(buffer, 0, n)
						}
					}
				}
			}
			checkIo(marker, "Not a PWA Installer profile archive")
			checkIo(backup.mkdir(), "Cannot back up profile")
			val moved = mutableListOf<String>()
			val replaced = mutableListOf<String>()
			try {
				roots.forEach { root ->
					val live = File(dataDir, root)
					val old = File(backup, root)
					val next = File(stage, root)
					if (live.exists()) {
						checkIo(live.renameTo(old), "Cannot back up $root")
						moved.add(root)
					}
					if (next.exists()) {
						checkIo(next.renameTo(live), "Cannot restore $root")
						replaced.add(root)
					}
				}
			} catch (error: IOException) {
				replaced.forEach { delete(File(dataDir, it)) }
				moved.forEach {
					if (!File(backup, it).renameTo(File(dataDir, it))) {
						preserveBackup = true
						error.addSuppressed(IOException("Rollback backup retained at ${File(backup, it)}"))
					}
				}
				throw error
			}
		} finally {
			if (!preserveBackup) {
				delete(stage)
			}
		}
	}

	private fun allowed(name: String): Boolean =
		!name.startsWith('/') && !name.contains('\\') &&
			!name.contains('\u0000') && name.split('/').none { it == "." || it == ".." } &&
			roots.any { name.startsWith("$it/") }

	@JvmStatic
	@Throws(IOException::class)
	fun copy(
		input: InputStream,
		output: OutputStream,
	) {
		input.copyTo(output, 32768)
	}

	@JvmStatic
	@Throws(IOException::class)
	fun delete(file: File) {
		if (!file.exists()) {
			return
		}
		if (file.isDirectory) {
			(file.listFiles() ?: throw IOException("Cannot list $file")).forEach(::delete)
		}
		checkIo(file.delete(), "Cannot delete $file")
	}

	private fun checkIo(
		condition: Boolean,
		message: String,
	) {
		if (!condition) {
			throw IOException(message)
		}
	}
}
