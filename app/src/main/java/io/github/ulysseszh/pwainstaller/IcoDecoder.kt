package io.github.ulysseszh.pwainstaller

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** ICO directories contain PNG images or bottom-up DIBs with a separate transparency mask. */
internal object IcoDecoder {
	data class Raster(val width: Int, val height: Int, val pixels: IntArray)
	fun matches(bytes: ByteArray) = bytes.size >= 6 && bytes[0] == 0.toByte() && bytes[1] == 0.toByte() &&
		bytes[2] == 1.toByte() && bytes[3] == 0.toByte()

	fun images(bytes: ByteArray): List<ByteArray> {
		require(matches(bytes)) { "Invalid ICO header" }
		val input = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
		val count = input.getShort(4).toInt() and 65535
		require(count in 1..256 && 6 + count * 16 <= bytes.size) { "Invalid ICO directory" }
		data class Entry(val area: Int, val depth: Int, val offset: Int, val size: Int)
		return (0 until count).mapNotNull { index ->
			val entry = 6 + index * 16
			fun dimension(offset: Int) = (bytes[offset].toInt() and 255).let { if (it == 0) 256 else it }
			val size = input.getInt(entry + 8)
			val offset = input.getInt(entry + 12)
			if (size <= 0 || offset < 6 + count * 16 || offset > bytes.size || size > bytes.size - offset) null
			else Entry(dimension(entry) * dimension(entry + 1), input.getShort(entry + 6).toInt() and 65535, offset, size)
		}.sortedWith(compareByDescending<Entry> { it.area }.thenByDescending { it.depth })
			.distinctBy { it.offset }.take(32).map { bytes.copyOfRange(it.offset, it.offset + it.size) }
	}

	fun bitmap(bytes: ByteArray): Raster {
		require(bytes.size >= 40) { "Invalid ICO bitmap header" }
		val input = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
		val header = input.getInt(0)
		val width = input.getInt(4)
		val combinedHeight = input.getInt(8)
		require(header in 40..bytes.size && width in 1..256 && combinedHeight != 0 && combinedHeight in -512..512 && combinedHeight % 2 == 0)
		val height = kotlin.math.abs(combinedHeight) / 2
		val depth = input.getShort(14).toInt() and 65535
		require(input.getShort(12).toInt() == 1 && depth in listOf(1, 4, 8, 24, 32) && input.getInt(16) == 0) { "Unsupported ICO bitmap" }
		val colors = if (depth <= 8) input.getInt(32).let { if (it == 0) 1 shl depth else it } else 0
		require(colors in 0..256 && (depth > 8 || colors in 1..(1 shl depth)))
		val start = header + colors * 4
		val stride = (width * depth + 31) / 32 * 4
		val end = start + stride * height
		require(start <= bytes.size && end <= bytes.size) { "Truncated ICO pixels" }
		val maskStride = (width + 31) / 32 * 4
		val maskPresent = end + maskStride * height <= bytes.size
		require(depth == 32 || maskPresent) { "Missing ICO transparency mask" }
		fun byte(offset: Int) = bytes[offset].toInt() and 255
		val pixels = IntArray(width * height)
		var hasAlpha = false
		for (y in 0 until height) {
			val row = start + (if (combinedHeight > 0) height - 1 - y else y) * stride
			for (x in 0 until width) {
				val position = if (depth <= 8) {
					val index = (byte(row + x * depth / 8) shr (8 - depth - x * depth % 8)) and ((1 shl depth) - 1)
					require(index < colors) { "Invalid ICO palette index" }
					header + index * 4
				} else row + x * (depth / 8)
				val alpha = if (depth == 32) byte(position + 3) else 255
				if (depth == 32 && alpha != 0) hasAlpha = true
				pixels[y * width + x] = (alpha shl 24) or (byte(position + 2) shl 16) or (byte(position + 1) shl 8) or byte(position)
			}
		}
		if (!hasAlpha) for (y in 0 until height) {
			val row = end + (if (combinedHeight > 0) height - 1 - y else y) * maskStride
			for (x in 0 until width) {
				val transparent = maskPresent && byte(row + x / 8) and (128 shr (x % 8)) != 0
				val index = y * width + x
				pixels[index] = (pixels[index] and 0xffffff) or (if (transparent) 0 else 255 shl 24)
			}
		}
		return Raster(width, height, pixels)
	}
}
