package io.github.ulysseszh.pwainstaller

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.caverock.androidsvg.SVG
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.util.Base64
import java.util.zip.CRC32

/** Raster previews plus original scalable artwork; native launcher resources use PNGs. */
object IconData {
    val default: String by lazy {
        val bytes = checkNotNull(IconData::class.java.getResourceAsStream("/web_icon.png")) { "Missing default icon" }
            .use { it.readBytes() }
        Base64.getEncoder().encodeToString(bytes)
    }

    fun decode(encoded: String): ByteArray {
        require(encoded.length <= 4 * 1024 * 1024) { "Icon exceeds size limit" }
        val bytes = Base64.getDecoder().decode(encoded.filterNot { it.isWhitespace() })
        val signature = byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)
        require(bytes.size >= 33 && bytes.copyOfRange(0, 8).contentEquals(signature)) { "Icon must be a PNG image" }
        val input = ByteBuffer.wrap(bytes)
        require(input.getInt(8) == 13 && input.getInt(12) == 0x49484452) { "Invalid PNG header" }
        require(input.getInt(16) in 1..1024 && input.getInt(20) in 1..1024) { "Invalid icon dimensions" }
        var offset = 8
        var imageData = false
        while (offset <= bytes.size - 12) {
            val length = input.getInt(offset)
            require(length >= 0 && length <= bytes.size - offset - 12) { "Invalid PNG chunk" }
            val type = input.getInt(offset + 4)
            val crc = CRC32().apply { update(bytes, offset + 4, length + 4) }.value.toInt()
            require(crc == input.getInt(offset + 8 + length)) { "Invalid PNG checksum" }
            offset += length + 12
            if (type == 0x49444154) imageData = true
            if (type == 0x49454e44) {
                require(imageData && length == 0 && offset == bytes.size) { "Invalid PNG end" }
                return bytes
            }
        }
        throw IllegalArgumentException("Incomplete PNG image")
    }

    fun dimensions(encoded: String): Pair<Int, Int> {
        // Selection needs only IHDR; full CRC validation belongs to decode()/validate().
        val prefix = encoded.asSequence().filterNot(Char::isWhitespace).take(32).joinToString("")
        val bytes = Base64.getDecoder().decode(prefix)
        require(bytes.size >= 24 && bytes[0] == 137.toByte() && bytes[1] == 80.toByte()) { "Invalid PNG header" }
        return ByteBuffer.wrap(bytes).let { (it.getInt(16) to it.getInt(20)).also { (w, h) -> require(w in 1..1024 && h in 1..1024) } }
    }
    fun sizes(encoded: String): String = dimensions(encoded).let { "${it.first}x${it.second}" }
    private val knownPurposes = setOf("any", "maskable", "monochrome")
    fun purposes(value: String): Set<String> = value.lowercase(java.util.Locale.ROOT).split(Regex("\\s+"))
        .filter { it in knownPurposes }.toSet().ifEmpty { setOf("any") }

    /** Smallest adequate image avoids upscaling; the largest is the fallback. */
    fun scalable(icon: org.json.JSONObject): Boolean = icon.optString("type").equals("image/svg+xml", true) || icon.has("sourceData") ||
        icon.optString("src").startsWith("data:image/svg+xml", true) || runCatching {
            java.net.URI(icon.optString("src")).path.orEmpty().endsWith(".svg", true)
        }.getOrDefault(false)
    fun bestEntry(icons: org.json.JSONArray, target: Int, purpose: String): org.json.JSONObject? {
        val candidates = (0 until icons.length()).map { icons.getJSONObject(it) }
            .filter { purpose in purposes(it.optString("purpose", "any")) }
            .sortedBy { if (scalable(it)) Int.MAX_VALUE else dimensions(it.getString("data")).let { size -> minOf(size.first, size.second) } }
        return candidates.firstOrNull { scalable(it) || dimensions(it.getString("data")).let { size -> minOf(size.first, size.second) >= target } } ?: candidates.lastOrNull()
    }
    fun best(icons: org.json.JSONArray, target: Int, purpose: String): String? = bestEntry(icons, target, purpose)?.getString("data")
    fun representative(icons: org.json.JSONArray): String = best(icons, 1024, "any")
        ?: best(icons, 1024, "maskable") ?: default

    fun fromDataUrl(source: String): ByteArray {
        require(source.length <= 3 * 1024 * 1024) { "Icon exceeds size limit" }
        val comma = source.indexOf(',')
        require(comma > 0 && source.substring(0, comma).startsWith("data:image/", true)) { "Invalid icon data URL" }
        val bytes = if (source.substring(0, comma).endsWith(";base64", true))
            Base64.getDecoder().decode(source.substring(comma + 1))
        else URLDecoder.decode(source.substring(comma + 1).replace("+", "%2B"), "UTF-8").toByteArray(Charsets.UTF_8)
        require(bytes.size <= 2 * 1024 * 1024) { "Icon exceeds size limit" }
        return bytes
    }

    private data class Normalized(val preview: String, val source: ByteArray? = null)
    fun entry(bytes: ByteArray, source: String, purpose: String = "any"): org.json.JSONObject {
        val normalized = normalized(bytes)
        return org.json.JSONObject().put("src", source).put("data", normalized.preview)
            .put("purpose", purposes(purpose).joinToString(" ")).apply {
                if (normalized.source != null) {
                    put("type", "image/svg+xml"); put("sizes", "any")
                    put("sourceData", Base64.getEncoder().encodeToString(normalized.source))
                } else { put("type", "image/png"); put("sizes", sizes(normalized.preview)) }
            }
    }
    fun source(icon: org.json.JSONObject): ByteArray? {
        if (!icon.has("sourceData")) return null
        val encoded = icon.getString("sourceData")
        require(encoded.length <= 3 * 1024 * 1024) { "SVG exceeds size limit" }
        return Base64.getDecoder().decode(encoded).also { require(it.size <= 2 * 1024 * 1024) { "SVG exceeds size limit" } }
    }
    private fun svg(bytes: ByteArray): SVG {
        SVG.setInternalEntitiesEnabled(false)
        return SVG.getFromInputStream(bytes.inputStream())
    }
    fun validate(icon: org.json.JSONObject) {
        decode(icon.getString("data"))
        source(icon)?.let { svg(it) }
        icon.put("sizes", if (scalable(icon)) "any" else sizes(icon.getString("data")))
        icon.put("type", if (scalable(icon)) "image/svg+xml" else "image/png")
    }
    fun render(icon: org.json.JSONObject, size: Int): ByteArray = source(icon)?.let { bytes ->
        decode(encode(renderSvg(bytes, size)))
    } ?: decode(icon.getString("data"))
    /** Rasterize SVGs locally; no scripts or external resource resolver are involved. */
    fun normalize(bytes: ByteArray): String = normalized(bytes).preview
    private fun normalized(bytes: ByteArray): Normalized {
        require(bytes.size <= 2 * 1024 * 1024) { "Icon exceeds size limit" }
        // Keep valid PNGs byte-for-byte, including their original dimensions.
        val encoded = Base64.getEncoder().encodeToString(bytes)
        if (runCatching { decode(encoded) }.isSuccess) return Normalized(encoded)
        if (IcoDecoder.matches(bytes)) {
            for (image in IcoDecoder.images(bytes)) try {
                if (image.size >= 8 && image.copyOfRange(0, 8).contentEquals(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)))
                    return Normalized(normalize(image))
                val raster = IcoDecoder.bitmap(image)
                return Normalized(encode(Bitmap.createBitmap(raster.pixels, raster.width, raster.height, Bitmap.Config.ARGB_8888)))
            } catch (_: Exception) { /* Try the next image in the ICO directory. */ }
            throw IllegalArgumentException("No usable image in ICO file")
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val icon = if (bounds.outWidth > 0 && bounds.outHeight > 0) {
            require(bounds.outWidth <= 8192 && bounds.outHeight <= 8192) { "Icon dimensions exceed limit" }
            bounds.inJustDecodeBounds = false
            bounds.inSampleSize = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 1024)
            val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)) { "Cannot decode icon" }
            val ratio = minOf(1f, 1024f / maxOf(bitmap.width, bitmap.height))
            val scaled = Bitmap.createScaledBitmap(bitmap, maxOf(1, (bitmap.width * ratio).toInt()), maxOf(1, (bitmap.height * ratio).toInt()), true)
            if (scaled !== bitmap) bitmap.recycle()
            scaled
        } else {
            return Normalized(encode(renderSvg(bytes, 512)), bytes)
        }
        return Normalized(encode(icon))
    }
    private fun renderSvg(bytes: ByteArray, size: Int): Bitmap {
        require(size in 1..1024) { "Invalid icon dimensions" }
        val svg = svg(bytes)
        if (svg.documentViewBox == null && svg.documentWidth > 0 && svg.documentHeight > 0)
            svg.setDocumentViewBox(0f, 0f, svg.documentWidth, svg.documentHeight)
        svg.setDocumentWidth("100%"); svg.setDocumentHeight("100%")
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also {
            try { svg.renderToCanvas(Canvas(it)) } catch (error: Exception) { it.recycle(); throw error }
        }
    }
    private fun encode(icon: Bitmap): String = try {
            val output = ByteArrayOutputStream()
            check(icon.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Cannot encode icon" }
            Base64.getEncoder().encodeToString(output.toByteArray())
        } finally { icon.recycle() }
}
