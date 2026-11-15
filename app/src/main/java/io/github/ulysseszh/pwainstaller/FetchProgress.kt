package io.github.ulysseszh.pwainstaller

data class FetchProgress(val stage: Stage = Stage.PREPARING, val received: Long = 0, val total: Long? = null) {
    enum class Stage { PREPARING, PAGE, MANIFEST, ICON, FILTERS, FILE }
    val percent: Int? get() = total?.takeIf { it > 0 }?.let { (received * 100 / it).coerceIn(0, 100).toInt() }
}
