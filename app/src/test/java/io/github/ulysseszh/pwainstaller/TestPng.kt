package io.github.ulysseszh.pwainstaller

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.Base64
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream

/** Solid opaque PNG fixture that needs neither Android graphics nor desktop AWT. */
fun testPng(size: Int): String {
	val result = ByteArrayOutputStream()
	val output = DataOutputStream(result)
	output.write(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10))
	fun chunk(type: String, bytes: ByteArray) {
		val tag = type.toByteArray(Charsets.US_ASCII)
		output.writeInt(bytes.size); output.write(tag); output.write(bytes)
		output.writeInt(CRC32().apply { update(tag); update(bytes) }.value.toInt())
	}
	val header = ByteArrayOutputStream()
	DataOutputStream(header).apply { writeInt(size); writeInt(size); write(byteArrayOf(8, 6, 0, 0, 0)) }
	chunk("IHDR", header.toByteArray())
	val compressed = ByteArrayOutputStream()
	DeflaterOutputStream(compressed).use { stream ->
		val row = ByteArray(size * 4 + 1) { if (it == 0) 0 else 255.toByte() }
		repeat(size) { stream.write(row) }
	}
	chunk("IDAT", compressed.toByteArray()); chunk("IEND", byteArrayOf())
	return Base64.getEncoder().encodeToString(result.toByteArray())
}
