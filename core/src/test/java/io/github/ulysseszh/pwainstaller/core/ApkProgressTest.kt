package io.github.ulysseszh.pwainstaller.core

import org.junit.Assert.*
import org.junit.Test

class ApkProgressTest {
	@Test fun buildStagesAdvanceFromEmptyToFullWithoutRestarting() {
		val values = ApkProgress.Stage.entries.flatMap { stage -> listOf(null, 0, 25, 100).map { ApkProgress(stage, it).overallPercent } }
		assertEquals(0, values.first())
		assertEquals(100, values.last())
		assertEquals(values.sorted(), values)
		assertEquals(90, ApkProgress(ApkProgress.Stage.SIGNING).overallPercent)
		assertEquals(100, ApkProgress(ApkProgress.Stage.SIGNING, 100).overallPercent)
	}
}
