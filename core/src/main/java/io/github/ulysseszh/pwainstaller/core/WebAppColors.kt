package io.github.ulysseszh.pwainstaller.core

/** App defaults; page metadata and authored backgrounds take precedence at runtime. */
data class WebAppColors(
	val theme: Int?,
	val background: Int,
) {
	companion object {
		fun resolve(
			theme: String,
			background: String,
			darkTheme: String = "",
			darkBackground: String = "",
			dark: Boolean = false,
		): WebAppColors {
			fun color(
				normal: String,
				night: String,
			) = (
				if (dark) {
					CssColors.parse(night)
				} else {
					null
				}
			) ?: CssColors.parse(normal)
			return WebAppColors(color(theme, darkTheme)?.let(CssColors::opaque), CssColors.opaque(color(background, darkBackground) ?: -1))
		}
	}
}
