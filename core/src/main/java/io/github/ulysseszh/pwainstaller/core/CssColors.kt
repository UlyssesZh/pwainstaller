package io.github.ulysseszh.pwainstaller.core

import java.util.Locale
import kotlin.math.*

/** Converts manifest sRGB colors to Android ARGB without depending on an Android API level. */
object CssColors {
    private val named = """
        aliceblue:f0f8ff antiquewhite:faebd7 aqua:00ffff aquamarine:7fffd4 azure:f0ffff beige:f5f5dc bisque:ffe4c4
        black:000000 blanchedalmond:ffebcd blue:0000ff blueviolet:8a2be2 brown:a52a2a burlywood:deb887 cadetblue:5f9ea0
        chartreuse:7fff00 chocolate:d2691e coral:ff7f50 cornflowerblue:6495ed cornsilk:fff8dc cyan:00ffff darkblue:00008b
        darkcyan:008b8b darkgoldenrod:b8860b darkgray:a9a9a9 darkgreen:006400 darkgrey:a9a9a9 darkkhaki:bdb76b darkmagenta:8b008b
        darkolivegreen:556b2f darkorange:ff8c00 darkorchid:9932cc darkred:8b0000 darksalmon:e9967a darkseagreen:8fbc8f darkslateblue:483d8b
        darkslategray:2f4f4f darkslategrey:2f4f4f darkturquoise:00ced1 darkviolet:9400d3 debianred:d70751 deeppink:ff1493 deepskyblue:00bfff
        dimgray:696969 dimgrey:696969 dodgerblue:1e90ff firebrick:b22222 floralwhite:fffaf0 forestgreen:228b22 fuchsia:ff00ff
        gainsboro:dcdcdc ghostwhite:f8f8ff gold:ffd700 goldenrod:daa520 gray:808080 green:008000 greenyellow:adff2f
        grey:808080 honeydew:f0fff0 hotpink:ff69b4 indianred:cd5c5c ivory:fffff0 khaki:f0e68c lavender:e6e6fa
        lavenderblush:fff0f5 lawngreen:7cfc00 lemonchiffon:fffacd lightblue:add8e6 lightcoral:f08080 lightcyan:e0ffff lightgoldenrod:eedd82
        lightgoldenrodyellow:fafad2 lightgray:d3d3d3 lightgreen:90ee90 lightgrey:d3d3d3 lightpink:ffb6c1 lightsalmon:ffa07a lightseagreen:20b2aa
        lightskyblue:87cefa lightslateblue:8470ff lightslategray:778899 lightslategrey:778899 lightsteelblue:b0c4de lightyellow:ffffe0 lime:00ff00
        limegreen:32cd32 linen:faf0e6 magenta:ff00ff maroon:800000 mediumaquamarine:66cdaa mediumblue:0000cd mediumorchid:ba55d3
        mediumpurple:9370db mediumseagreen:3cb371 mediumslateblue:7b68ee mediumspringgreen:00fa9a mediumturquoise:48d1cc mediumvioletred:c71585 midnightblue:191970
        mintcream:f5fffa mistyrose:ffe4e1 moccasin:ffe4b5 navajowhite:ffdead navy:000080 navyblue:000080 oldlace:fdf5e6
        olive:808000 olivedrab:6b8e23 orange:ffa500 orangered:ff4500 orchid:da70d6 palegoldenrod:eee8aa palegreen:98fb98
        paleturquoise:afeeee palevioletred:db7093 papayawhip:ffefd5 peachpuff:ffdab9 peru:cd853f pink:ffc0cb plum:dda0dd
        powderblue:b0e0e6 purple:800080 rebeccapurple:663399 red:ff0000 rosybrown:bc8f8f royalblue:4169e1 saddlebrown:8b4513
        salmon:fa8072 sandybrown:f4a460 seagreen:2e8b57 seashell:fff5ee sienna:a0522d silver:c0c0c0 skyblue:87ceeb
        slateblue:6a5acd slategray:708090 slategrey:708090 snow:fffafa springgreen:00ff7f steelblue:4682b4 tan:d2b48c
        teal:008080 thistle:d8bfd8 tomato:ff6347 turquoise:40e0d0 violet:ee82ee violetred:d02090 wheat:f5deb3
        white:ffffff whitesmoke:f5f5f5 yellow:ffff00 yellowgreen:9acd32
    """.trimIndent().split(Regex("\\s+")).associate { val pair = it.split(':'); pair[0] to pair[1].toLong(16).toInt().or(0xff000000.toInt()) }

    fun parse(value: String): Int? = runCatching {
        val color = value.trim().lowercase(Locale.ROOT)
        if (color == "transparent") return 0
        named[color]?.let { return it }
        if (color.startsWith('#')) {
            val hex = color.drop(1)
            return when (hex.length) {
                3 -> ("ff" + hex.flatMap { listOf(it, it) }.joinToString("")).toLong(16).toInt()
                4 -> ("${hex[3]}${hex[3]}" + hex.take(3).flatMap { listOf(it, it) }.joinToString("")).toLong(16).toInt()
                6 -> ("ff" + hex).toLong(16).toInt()
                8 -> (hex.takeLast(2) + hex.take(6)).toLong(16).toInt()
                else -> null
            }
        }
        val match = Regex("(rgba?|hsla?)\\((.*)\\)").matchEntire(color) ?: return null
        val values = match.groupValues[2].trim().split(Regex("[,/\\s]+"))
        if (values.size !in 3..4) return null
        val alpha = values.getOrNull(3)?.let { number(it, 1f).coerceIn(0f, 1f) } ?: 1f
        val rgb = if (match.groupValues[1].startsWith("rgb")) values.take(3).map { number(it, 255f).coerceIn(0f, 255f).roundToInt() }
        else {
            val h = values[0].removeSuffix("deg").toFloat().let { ((it % 360) + 360) % 360 }
            val s = number(values[1], 1f).coerceIn(0f, 1f)
            val l = number(values[2], 1f).coerceIn(0f, 1f)
            val chroma = (1 - abs(2 * l - 1)) * s
            val x = chroma * (1 - abs((h / 60) % 2 - 1))
            val components = when (h.toInt() / 60) {
                0 -> listOf(chroma, x, 0f); 1 -> listOf(x, chroma, 0f); 2 -> listOf(0f, chroma, x)
                3 -> listOf(0f, x, chroma); 4 -> listOf(x, 0f, chroma); else -> listOf(chroma, 0f, x)
            }
            components.map { ((it + l - chroma / 2) * 255).roundToInt().coerceIn(0, 255) }
        }
        ((alpha * 255).roundToInt() shl 24) or (rgb[0] shl 16) or (rgb[1] shl 8) or rgb[2]
    }.getOrNull()
    /** Android launch windows require an opaque color. Composite CSS alpha over white. */
    fun opaque(color: Int): Int {
        val alpha = color ushr 24
        fun channel(shift: Int) = (((color ushr shift) and 255) * alpha + 255 * (255 - alpha) + 127) / 255
        return (255 shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
    /** Black or white, whichever has the greater WCAG contrast against an opaque sRGB color. */
    fun foreground(background: Int): Int {
        fun linear(shift: Int): Double {
            val value = ((background ushr shift) and 255) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        val luminance = 0.2126 * linear(16) + 0.7152 * linear(8) + 0.0722 * linear(0)
        return if ((luminance + 0.05) / 0.05 >= 1.05 / (luminance + 0.05)) 0xff000000.toInt() else 0xffffffff.toInt()
    }
    private fun number(text: String, scale: Float) = if (text.endsWith('%')) text.dropLast(1).toFloat() * scale / 100 else text.toFloat()
}
