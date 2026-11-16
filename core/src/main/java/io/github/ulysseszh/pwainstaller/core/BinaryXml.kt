package io.github.ulysseszh.pwainstaller.core

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Rebuilds the compiled manifest string pool while preserving resource IDs and XML node indices. */
object BinaryXml {
	data class IntentFilter(
		val actions: List<String>,
		val mimeTypes: List<String> = emptyList(),
		val schemes: List<String> = emptyList(),
		val categories: List<String> = listOf("android.intent.category.DEFAULT"),
		val data: List<Map<String, String>> = emptyList(),
	)

	/** Replace handler filters with manifest-specific filters, retaining Android attribute IDs. */
	fun intentFilters(
		xml: ByteArray,
		activities: Map<String, List<IntentFilter>>,
	): ByteArray {
		val input = ByteBuffer.wrap(xml).order(ByteOrder.LITTLE_ENDIAN)
		val pool = input.getShort(2).toInt() and 65535
		val header = input.getShort(pool + 2).toInt() and 65535
		val poolSize = input.getInt(pool + 4)
		val utf8 = input.getInt(pool + 16) and 256 != 0
		require(input.getInt(pool + 12) == 0) { "Styled XML strings unsupported" }
		val strings =
			MutableList(input.getInt(pool + 8)) { index ->
				input.position(pool + input.getInt(pool + 20) + input.getInt(pool + header + index * 4))
				if (utf8) {
					length8(input)
					ByteArray(length8(input)).also(input::get).toString(Charsets.UTF_8)
				} else {
					ByteArray(length16(input) * 2).also(input::get).toString(Charsets.UTF_16LE)
				}
			}

		fun index(value: String): Int = strings.indexOf(value).takeIf { it >= 0 } ?: strings.size.also { strings.add(value) }
		val namespace = index("http://schemas.android.com/apk/res/android")
		val attributeIds =
			mapOf(
				"name" to 0x01010003,
				"mimeType" to 0x01010026,
				"scheme" to 0x01010027,
				"host" to 0x01010028,
				"port" to 0x01010029,
				"path" to 0x0101002a,
				"pathPrefix" to 0x0101002b,
				"pathPattern" to 0x0101002c,
			)
		attributeIds.keys.forEach(::index)

		fun start(
			name: String,
			attributes: Map<String, String> = emptyMap(),
		): ByteArray =
			ByteBuffer
				.allocate(36 + attributes.size * 20)
				.order(ByteOrder.LITTLE_ENDIAN)
				.apply {
					putShort(0x0102)
					putShort(16)
					putInt(capacity())
					putInt(0)
					putInt(-1)
					putInt(-1)
					putInt(index(name))
					putShort(20)
					putShort(20)
					putShort(attributes.size.toShort())
					putShort(0)
					putShort(0)
					putShort(0)
					attributes.forEach { (key, value) ->
						putInt(namespace)
						putInt(index(key))
						putInt(index(value))
						putShort(8)
						put(0)
						put(3)
						putInt(index(value))
					}
				}.array()

		fun end(name: String): ByteArray =
			ByteBuffer
				.allocate(24)
				.order(ByteOrder.LITTLE_ENDIAN)
				.apply {
					putShort(0x0103)
					putShort(16)
					putInt(24)
					putInt(0)
					putInt(-1)
					putInt(-1)
					putInt(index(name))
				}.array()
		val nodes = ByteArrayOutputStream()

		fun element(
			name: String,
			attributes: Map<String, String>,
		) {
			nodes.write(start(name, attributes))
			nodes.write(end(name))
		}
		var activity: String? = null
		var skipping = 0
		var offset = pool + poolSize
		val found = mutableSetOf<String>()
		while (offset < xml.size) {
			require(offset <= xml.size - 8) { "Invalid XML chunk" }
			val type = input.getShort(offset).toInt() and 65535
			val size = input.getInt(offset + 4)
			require(size >= 8 && size <= xml.size - offset) { "Invalid XML node" }
			if (type == 0x0180) {
				val ids = IntArray(maxOf((size - 8) / 4, attributeIds.keys.maxOf(::index) + 1))
				repeat((size - 8) / 4) { ids[it] = input.getInt(offset + 8 + it * 4) }
				attributeIds.forEach { (key, value) -> ids[index(key)] = value }
				nodes.write(
					ByteBuffer
						.allocate(8 + ids.size * 4)
						.order(ByteOrder.LITTLE_ENDIAN)
						.apply {
							putShort(0x0180)
							putShort(8)
							putInt(capacity())
							ids.forEach(::putInt)
						}.array(),
				)
				offset += size
				continue
			}
			val name =
				if (type in listOf(0x0102, 0x0103)) {
					strings[input.getInt(offset + 20)]
				} else {
					""
				}
			if (type == 0x0102 && name == "activity") {
				val first = offset + 16 + (input.getShort(offset + 24).toInt() and 65535)
				val stride = input.getShort(offset + 26).toInt() and 65535
				repeat(input.getShort(offset + 28).toInt() and 65535) { i ->
					val attribute = first + i * stride
					if (strings[input.getInt(attribute + 4)] == "name" && input.get(attribute + 15).toInt() == 3) {
						activity = strings[input.getInt(attribute + 16)].takeIf(activities::containsKey)
					}
				}
			}
			if (skipping > 0) {
				if (type == 0x0102) {
					skipping++
				}
				if (type == 0x0103) {
					skipping--
				}
			} else if (activity != null && type == 0x0102 && name == "intent-filter") {
				skipping = 1
			} else {
				if (activity != null && type == 0x0103 && name == "activity") {
					found.add(activity!!)
					activities.getValue(activity!!).forEach { filter ->
						nodes.write(start("intent-filter"))
						filter.actions.forEach { element("action", mapOf("name" to it)) }
						filter.categories.forEach { element("category", mapOf("name" to it)) }
						filter.schemes.forEach { element("data", mapOf("scheme" to it)) }
						filter.mimeTypes.forEach { element("data", mapOf("mimeType" to it)) }
						filter.data.forEach { element("data", it) }
						nodes.write(end("intent-filter"))
					}
					activity = null
				}
				nodes.write(xml, offset, size)
			}
			offset += size
		}
		require(found.containsAll(activities.keys)) { "Missing runtime handler activities" }
		val encoded = ByteArrayOutputStream()
		val offsets =
			strings.map { value ->
				val position = encoded.size()
				if (utf8) {
					val bytes = value.toByteArray(Charsets.UTF_8)
					putLength8(encoded, value.length)
					putLength8(encoded, bytes.size)
					encoded.write(bytes)
					encoded.write(0)
				} else {
					putLength16(encoded, value.length)
					encoded.write(value.toByteArray(Charsets.UTF_16LE))
					encoded.write(0)
					encoded.write(0)
				}
				position
			}
		while (encoded.size() and 3 != 0) {
			encoded.write(0)
		}
		val dataStart = header + strings.size * 4
		val newPoolSize = dataStart + encoded.size()
		return ByteBuffer
			.allocate(pool + newPoolSize + nodes.size())
			.order(ByteOrder.LITTLE_ENDIAN)
			.apply {
				put(xml, 0, pool)
				putShort(1)
				putShort(header.toShort())
				putInt(newPoolSize)
				putInt(strings.size)
				putInt(0)
				putInt(input.getInt(pool + 16) and 1.inv())
				putInt(dataStart)
				putInt(0)
				while (position() < pool + header) {
					put(0)
				}
				offsets.forEach(::putInt)
				put(encoded.toByteArray())
				put(nodes.toByteArray())
				putInt(4, capacity())
			}.array()
	}

	@JvmStatic fun replace(
		xml: ByteArray,
		replacements: Map<String, String>,
	): ByteArray {
		require(xml.size >= 36) { "Not binary XML" }
		val input = ByteBuffer.wrap(xml).order(ByteOrder.LITTLE_ENDIAN)
		require(input.getShort(0).toInt() == 3) { "Not binary XML" }
		val pool = input.getShort(2).toInt() and 65535
		require(input.getShort(pool).toInt() == 1) { "Missing string pool" }
		val headerSize = input.getShort(pool + 2).toInt() and 65535
		val size = input.getInt(pool + 4)
		val count = input.getInt(pool + 8)
		val flags = input.getInt(pool + 16)
		require(input.getInt(pool + 12) == 0) { "Styled manifest strings unsupported" }
		val start = input.getInt(pool + 20)
		val utf8 = flags and 0x100 != 0
		val strings = ByteArrayOutputStream()
		val offsets = IntArray(count)
		repeat(count) { index ->
			input.position(pool + start + input.getInt(pool + headerSize + index * 4))
			val value =
				if (utf8) {
					length8(input)
					ByteArray(length8(input)).also(input::get).toString(Charsets.UTF_8)
				} else {
					ByteArray(length16(input) * 2).also(input::get).toString(Charsets.UTF_16LE)
				}.let { replacements[it] ?: it }
			offsets[index] = strings.size()
			if (utf8) {
				val bytes = value.toByteArray(Charsets.UTF_8)
				putLength8(strings, value.length)
				putLength8(strings, bytes.size)
				strings.write(bytes)
				strings.write(0)
			} else {
				putLength16(strings, value.length)
				strings.write(value.toByteArray(Charsets.UTF_16LE))
				strings.write(0)
				strings.write(0)
			}
		}
		while (strings.size() and 3 != 0) {
			strings.write(0)
		}
		val dataStart = headerSize + 4 * count
		val newSize = dataStart + strings.size()
		val output = ByteBuffer.allocate(xml.size - size + newSize).order(ByteOrder.LITTLE_ENDIAN)
		output.put(xml, 0, pool)
		output.putShort(1).putShort(headerSize.toShort()).putInt(newSize)
		output
			.putInt(count)
			.putInt(0)
			.putInt(flags and 1.inv())
			.putInt(dataStart)
			.putInt(0)
		while (output.position() < pool + headerSize) {
			output.put(0)
		}
		offsets.forEach(output::putInt)
		output.put(strings.toByteArray())
		output.put(xml, pool + size, xml.size - pool - size)
		output.putInt(4, output.capacity())
		return output.array()
	}

	private fun length8(input: ByteBuffer): Int {
		val n = input.get().toInt() and 255
		return if (n and 128 == 0) {
			n
		} else {
			((n and 127) shl 8) or (input.get().toInt() and 255)
		}
	}

	private fun length16(input: ByteBuffer): Int {
		val n = input.short.toInt() and 65535
		return if (n and 32768 == 0) {
			n
		} else {
			((n and 32767) shl 16) or (input.short.toInt() and 65535)
		}
	}

	private fun putLength8(
		output: ByteArrayOutputStream,
		n: Int,
	) {
		require(n <= 32767) { "Manifest string too long" }
		if (n > 127) {
			output.write((n shr 8) or 128)
		}
		output.write(n and 255)
	}

	private fun putLength16(
		output: ByteArrayOutputStream,
		n: Int,
	) {
		if (n > 32767) {
			val high = (n shr 16) or 32768
			output.write(high and 255)
			output.write(high shr 8)
		}
		output.write(n and 255)
		output.write((n shr 8) and 255)
	}
}
