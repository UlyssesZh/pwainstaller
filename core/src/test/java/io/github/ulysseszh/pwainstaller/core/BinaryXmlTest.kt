package io.github.ulysseszh.pwainstaller.core

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryXmlTest {
	private fun fixture(utf8: Boolean): ByteArray {
		val strings = ByteArrayOutputStream()
		val values = listOf("template", "keep", "last")
		val offsets = values.map { value ->
			val offset = strings.size()
			val bytes = value.toByteArray(if (utf8) Charsets.UTF_8 else Charsets.UTF_16LE)
			strings.write(value.length); strings.write(if (utf8) bytes.size else 0); strings.write(bytes); strings.write(0)
			if (!utf8) strings.write(0)
			offset
		}
		while (strings.size() % 4 != 0) strings.write(0)
		val poolSize = 28 + 12 + strings.size()
		return ByteBuffer.allocate(8 + poolSize + 8).order(ByteOrder.LITTLE_ENDIAN).apply {
			putShort(3); putShort(8); putInt(capacity())
			putShort(1); putShort(28); putInt(poolSize); putInt(3); putInt(0); putInt(if (utf8) 256 else 0); putInt(40); putInt(0)
			offsets.forEach(::putInt); put(strings.toByteArray()); putLong(0x123456789abcdefL)
		}.array()
	}
	private fun decode(xml: ByteArray, index: Int): String {
		val input = ByteBuffer.wrap(xml).order(ByteOrder.LITTLE_ENDIAN)
		val utf8 = input.getInt(24) and 256 != 0
		input.position(8 + input.getInt(28) + input.getInt(36 + index * 4))
		var n = input.get().toInt() and 255
		if (utf8) {
			if (n > 127) input.get()
			n = input.get().toInt() and 255
			if (n > 127) n = ((n and 127) shl 8) or (input.get().toInt() and 255)
		} else n = n or ((input.get().toInt() and 255) shl 8)
		return ByteArray(if (utf8) n else n * 2).also(input::get).toString(if (utf8) Charsets.UTF_8 else Charsets.UTF_16LE)
	}
	@Test fun rebuildsBothEncodingsAndPreservesNodeIndices() {
		val replacement = "应用 🐉 " + "x".repeat(200)
		listOf(true, false).forEach { utf8 ->
			val result = BinaryXml.replace(fixture(utf8), mapOf("template" to replacement))
			assertEquals(replacement, decode(result, 0)); assertEquals("keep", decode(result, 1)); assertEquals("last", decode(result, 2))
			val buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN)
			assertEquals(result.size, buffer.getInt(4)); assertEquals(0x123456789abcdefL, buffer.getLong(result.size - 8)); assertEquals(0, result.size % 4)
		}
	}
	@Test fun rejectsNonXml() { assertThrows(IllegalArgumentException::class.java) { BinaryXml.replace(ByteArray(80), emptyMap()) } }
}
