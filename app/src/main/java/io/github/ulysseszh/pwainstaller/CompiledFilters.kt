package io.github.ulysseszh.pwainstaller

import java.io.*
import java.security.MessageDigest
import java.util.zip.CRC32

/** Reusable build assets, independent of a web app's name, package, icon, or signing key. */
class CompiledFilters(private val directory: File, private val version: String) {
	companion object {
		private const val MAGIC = 0x50574132
		private const val BUDGET = 64L * 1024 * 1024
		private val names = setOf("assets/adblock.js", "assets/licenses/adguard.txt", "assets/licenses/core-js.txt", "assets/licenses/NOTICE.txt")
		// Two builds of the same snapshot share the completed result; unrelated snapshots proceed independently.
		private val locks = Array(32) { Any() }
		private val eviction = Any()
	}

	fun get(text: String, asset: (String) -> String, progress: (Int) -> Unit = {}, log: (String) -> Unit = {}): Map<String, ByteArray> {
		progress(0)
		val digest = MessageDigest.getInstance("SHA-256")
		digest.update("${AdblockScript.VERSION}:$version\u0000".toByteArray(Charsets.UTF_8))
		val key = digest.digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
		return synchronized(locks[(key.hashCode() and Int.MAX_VALUE) % locks.size]) {
			val file = File(directory, "$key.bin")
			val cached = try { read(file) } catch (_: IOException) { file.delete(); null }
			if (cached != null) {
				file.setLastModified(System.currentTimeMillis())
				log("Using cached compiled filter rules ($key)")
				progress(100)
				cached
			} else {
				val started = System.nanoTime()
				val result = AdblockScript.compile(text, progress, asset)
				log("Compiled filter rules in ${(System.nanoTime() - started) / 1_000_000} ms")
				try { write(file, result) }
				catch (failure: IOException) { log("Could not cache compiled filter rules: ${failure.message}") }
				result
			}
		}
	}

	private fun read(file: File): Map<String, ByteArray>? {
		if (!file.isFile) return null
		requireSize(file.length() in 8..BUDGET)
		return DataInputStream(file.inputStream().buffered()).use { input ->
			requireSize(input.readInt() == MAGIC)
			val count = input.readInt(); requireSize(count in 0..names.size)
			val result = linkedMapOf<String, ByteArray>()
			var total = 0L
			repeat(count) {
				val name = input.readUTF(); requireSize(name in names && name !in result)
				val size = input.readInt(); total += size
				requireSize(size >= 0 && total <= BUDGET && size <= file.length())
				val crc = input.readLong()
				val bytes = ByteArray(size); input.readFully(bytes)
				requireSize(CRC32().apply { update(bytes) }.value == crc)
				result[name] = bytes
			}
			requireSize(input.read() == -1 && (result.isEmpty() || "assets/adblock.js" in result))
			result
		}
	}

	private fun requireSize(valid: Boolean) { if (!valid) throw IOException("Invalid compiled-filter cache") }

	private fun write(file: File, assets: Map<String, ByteArray>) {
		if (assets.values.sumOf { it.size.toLong() } > BUDGET - 4096) return
		if (!directory.mkdirs() && !directory.isDirectory) throw IOException("Cannot create compiled-filter cache")
		val temporary = File.createTempFile("compiling-", ".tmp", directory)
		try {
			DataOutputStream(temporary.outputStream().buffered()).use { output ->
				output.writeInt(MAGIC); output.writeInt(assets.size)
				for ((name, bytes) in assets) {
					output.writeUTF(name); output.writeInt(bytes.size)
					output.writeLong(CRC32().apply { update(bytes) }.value); output.write(bytes)
				}
			}
			if (!temporary.renameTo(file)) throw IOException("Cannot publish compiled-filter cache")
			synchronized(eviction) {
				val entries = directory.listFiles { entry -> entry.extension == "bin" }.orEmpty().sortedByDescending { it.lastModified() }
				var size = 0L
				entries.forEachIndexed { index, entry -> size += entry.length(); if (index >= 64 || size > BUDGET) entry.delete() }
			}
		} finally { temporary.delete() }
	}
}
