package io.github.ulysseszh.pwainstaller

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class IcoDecoderTest {
	private fun ico(vararg entries: Pair<Int, ByteArray>): ByteArray {
		val bytes = ByteBuffer.allocate(6 + entries.size * 16 + entries.sumOf { it.second.size }).order(ByteOrder.LITTLE_ENDIAN)
		bytes.putShort(0).putShort(1).putShort(entries.size.toShort())
		var offset = 6 + entries.size * 16
		entries.forEach { (size, image) ->
			bytes
				.put(size.toByte())
				.put(size.toByte())
				.putShort(0)
				.putShort(1)
				.putShort(32)
				.putInt(image.size)
				.putInt(offset)
			offset += image.size
		}
		entries.forEach { bytes.put(it.second) }
		return bytes.array()
	}

	private fun dib(
		width: Int,
		height: Int,
		depth: Int,
		pixels: ByteArray,
		palette: ByteArray = byteArrayOf(),
		mask: ByteArray =
			ByteArray((width + 31) / 32 * 4 * height),
	): ByteArray =
		ByteBuffer
			.allocate(40 + palette.size + pixels.size + mask.size)
			.order(ByteOrder.LITTLE_ENDIAN)
			.apply {
				putInt(40)
					.putInt(width)
					.putInt(height * 2)
					.putShort(1)
					.putShort(depth.toShort())
					.putInt(0)
				putInt(pixels.size)
					.putInt(0)
					.putInt(0)
					.putInt(palette.size / 4)
					.putInt(0)
				put(palette).put(pixels).put(mask)
			}.array()

	@Test fun extractsLargestPngAndKeepsItsActualBytes() {
		val png = IconData.decode(IconData.default)
		val bytes = ico(16 to byteArrayOf(1, 2, 3), 256 to png)
		assertTrue(IcoDecoder.matches(bytes))
		assertArrayEquals(png, IcoDecoder.images(bytes).first())
		assertEquals(IconData.default, IconData.normalize(bytes))
	}

	@Test fun decodesBottomUpArgbAndPreservesPartialTransparency() {
		val raster = IcoDecoder.bitmap(dib(1, 2, 32, byteArrayOf(255.toByte(), 0, 0, 128.toByte(), 0, 0, 255.toByte(), 255.toByte())))
		assertEquals(1, raster.width)
		assertEquals(2, raster.height)
		assertArrayEquals(intArrayOf(0xffff0000.toInt(), 0x800000ff.toInt()), raster.pixels)
	}

	@Test fun appliesTheLegacyAndMaskWhenAlphaIsAbsent() {
		val raster = IcoDecoder.bitmap(dib(2, 1, 32, byteArrayOf(0, 0, 255.toByte(), 0, 0, 255.toByte(), 0, 0), mask = byteArrayOf(64, 0, 0, 0)))
		assertArrayEquals(intArrayOf(0xffff0000.toInt(), 0x0000ff00), raster.pixels)
		val rgb = IcoDecoder.bitmap(dib(1, 1, 24, byteArrayOf(10, 20, 30, 0)))
		assertArrayEquals(intArrayOf(0xff1e140a.toInt()), rgb.pixels)
	}

	@Test fun readsPackedPalettePixelsAndWordAlignedRows() {
		val palette = byteArrayOf(0, 0, 0, 0, 0, 0, 255.toByte(), 0)
		val raster = IcoDecoder.bitmap(dib(3, 1, 1, byteArrayOf(160.toByte(), 0, 0, 0), palette))
		assertArrayEquals(intArrayOf(0xffff0000.toInt(), 0xff000000.toInt(), 0xffff0000.toInt()), raster.pixels)
	}

	@Test fun rejectsMalformedDirectoriesTruncatedPixelsAndOversizedImages() {
		assertThrows(IllegalArgumentException::class.java) { IcoDecoder.images(byteArrayOf(0, 0, 1, 0, 1, 0)) }
		val invalid = ico(16 to byteArrayOf(1)).also { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).putInt(18, Int.MAX_VALUE) }
		assertTrue(IcoDecoder.images(invalid).isEmpty())
		assertThrows(IllegalArgumentException::class.java) { IcoDecoder.bitmap(dib(256, 256, 32, byteArrayOf())) }
		assertThrows(IllegalArgumentException::class.java) { IcoDecoder.bitmap(dib(257, 1, 32, ByteArray(257 * 4))) }
	}
}
