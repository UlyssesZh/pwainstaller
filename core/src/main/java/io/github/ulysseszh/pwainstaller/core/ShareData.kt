package io.github.ulysseszh.pwainstaller.core

/** Android uses EXTRA_TEXT for both text and links; move a bare URL rather than copying it. */
data class ShareData(val title: String = "", val text: String = "", val url: String = "") {
    fun fields(names: Map<String, String>): List<Pair<String, String>> {
        // Text-only targets can still receive a link without a separate URL mapping.
        val fallback = url.isNotEmpty() && names["url"].isNullOrEmpty() && text.isEmpty()
        return listOf("title" to title, "text" to if (fallback) url else text, "url" to if (fallback) "" else url)
            .mapNotNull { (key, value) -> names[key]?.takeIf { it.isNotEmpty() && value.isNotEmpty() }?.let { it to value } }
    }

    companion object {
        fun fromAndroid(text: String, title: String = ""): ShareData {
            val url = runCatching { PackageNames.webUrl(text) }.getOrNull()
            return if (url != null) ShareData(title = title, url = url) else ShareData(title = title, text = text)
        }
    }
}
