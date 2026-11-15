package io.github.ulysseszh.pwainstaller.core

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Changes scalar color entries without changing resource IDs, offsets, or string pools. */
object BinaryResources {
	const val BACKGROUND = 0xff010203.toInt()
	const val THEME = 0xff040506.toInt()
	const val DARK_BACKGROUND = 0xff070809.toInt()
	const val DARK_THEME = 0xff0a0b0c.toInt()

	fun colors(table: ByteArray, replacements: Map<Int, Int>): ByteArray {
		val result = table.copyOf()
		val data = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN)
		fun short(offset: Int) = data.getShort(offset).toInt() and 65535
		require(result.size >= 12 && short(0) == 2 && data.getInt(4) == result.size) { "Invalid resource table" }
		val found = mutableSetOf<Int>()
		fun visit(start: Int, end: Int) {
			var offset = start
			while (offset < end) {
				require(offset <= end - 8) { "Invalid resource chunk" }
				val type = short(offset)
				val header = short(offset + 2)
				val size = data.getInt(offset + 4)
				require(header >= 8 && size >= header && size <= end - offset) { "Invalid resource chunk size" }
				if (type == 0x0200) visit(offset + header, offset + size)
				if (type == 0x0201) {
					require(header >= 20) { "Invalid resource type" }
					val flags = result[offset + 9].toInt() and 255
					val count = data.getInt(offset + 12)
					val entries = data.getInt(offset + 16)
					val stride = if (flags and 2 != 0) 2 else 4
					require(count >= 0 && entries >= header && entries <= size && count <= (entries - header) / stride) { "Invalid resource entries" }
					repeat(count) { index ->
						val address = offset + header + index * stride
						val relative = when {
							flags and 1 != 0 -> short(address + 2) * 4
							flags and 2 != 0 -> short(address).let { if (it == 65535) -1 else it * 4 }
							else -> data.getInt(address)
						}
						if (relative == -1) return@repeat
						require(relative >= 0 && relative <= size - entries - 8) { "Invalid resource entry offset" }
						val entry = offset + entries + relative
						if (short(entry + 2) and 1 != 0) return@repeat // Style/bag, not a scalar color.
						val value = entry + short(entry)
						require(value >= entry + 8 && value <= offset + size - 8 && short(value) == 8) { "Invalid resource value" }
						if ((result[value + 3].toInt() and 255) in 28..31) {
							val original = data.getInt(value + 4)
							replacements[original]?.let { data.putInt(value + 4, it); found.add(original) }
						}
					}
				}
				offset += size
			}
		}
		visit(short(2), result.size)
		require(found.containsAll(replacements.keys)) { "Missing template colors" }
		return result
	}
}
