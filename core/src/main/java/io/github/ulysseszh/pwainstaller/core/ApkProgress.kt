package io.github.ulysseszh.pwainstaller.core

/** A percentage is shown only for work whose completion can be measured. */
data class ApkProgress(
	val stage: Stage,
	val percent: Int? = null,
) {
	enum class Stage { PREPARING, VALIDATING, SAVING, KEY, FILTERS, TEMPLATE, ICONS, RESOURCES, SIGNING, BACKUP, INSTALLING, WAITING }

	/** Fixed phase ranges keep the build indicator monotonic instead of restarting each phase. */
	val overallPercent: Int get() {
		val range =
			when (stage) {
				Stage.PREPARING -> {
					0 to 0
				}

				Stage.VALIDATING -> {
					0 to 3
				}

				Stage.SAVING -> {
					3 to 5
				}

				Stage.KEY -> {
					5 to 10
				}

				Stage.FILTERS -> {
					10 to 45
				}

				Stage.TEMPLATE -> {
					45 to 50
				}

				Stage.ICONS -> {
					50 to 65
				}

				Stage.RESOURCES -> {
					65 to 90
				}

				Stage.SIGNING -> {
					90 to 100
				}

				Stage.BACKUP, Stage.INSTALLING, Stage.WAITING -> {
					100 to 100
				}
			}
		return range.first + (range.second - range.first) * (percent ?: 0).coerceIn(0, 100) / 100
	}
}
