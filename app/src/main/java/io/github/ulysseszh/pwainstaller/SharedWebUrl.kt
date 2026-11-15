package io.github.ulysseszh.pwainstaller

import io.github.ulysseszh.pwainstaller.core.PackageNames

object SharedWebUrl {
    fun extract(text: String?): String? {
        val value = text?.trim()?.takeIf { it.length <= 65536 } ?: return null
        runCatching { PackageNames.webUrl(value) }.getOrNull()?.let { return it }
        return Regex("https?://[^\\s<>\"\\u0000-\\u001f]+", RegexOption.IGNORE_CASE).findAll(value)
            .mapNotNull { match -> runCatching { PackageNames.webUrl(match.value.trimEnd('.', ',', ';', '!', ')', ']', '}')) }.getOrNull() }
            .firstOrNull()
    }
}
