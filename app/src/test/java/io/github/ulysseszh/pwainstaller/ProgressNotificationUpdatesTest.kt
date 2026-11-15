package io.github.ulysseszh.pwainstaller

import org.junit.Assert.*
import org.junit.Test

class ProgressNotificationUpdatesTest {
	@Test fun droppedNotificationsAreRetriedWithoutDuplicatingPendingPosts() {
		val updates = ProgressNotificationUpdates<String>()
		val desired = mapOf(1 to "first", 2 to "second")
		assertEquals(desired, updates.select(desired, 0))
		updates.retainVisible(emptySet(), 100)
		assertTrue(updates.select(desired, 100).isEmpty())
		updates.retainVisible(setOf(1), 1000)
		assertEquals(mapOf(2 to "second"), updates.select(desired, 1000))
	}
	@Test fun aBurstOfEightJobsGetsAllNotificationsWithinTheUpdateBudget() {
		val updates = ProgressNotificationUpdates<String>()
		val desired = (1..8).associateWith { "app $it" }
		updates.record(1, desired.getValue(1), 0)
		assertEquals(setOf(2, 3, 4), updates.select(desired, 0).keys)
		assertTrue(updates.select(desired, 999).isEmpty())
		assertEquals(setOf(5, 6, 7, 8), updates.select(desired, 1000).keys)
		assertTrue(updates.select(desired, 2000).isEmpty())
	}

	@Test fun changingProgressDoesNotStarveOtherJobsAndNeverExceedsTheRollingBudget() {
		val updates = ProgressNotificationUpdates<String>()
		val posts = mutableListOf<Pair<Long, Int>>()
		for (now in 0L..5000L step 100) {
			updates.select((1..8).associateWith { "$it at $now" }, now).keys.forEach { posts.add(now to it) }
			assertTrue(posts.count { now - it.first < 1000 } <= 4)
		}
		assertEquals((1..8).toSet(), posts.filter { it.first <= 1000 }.map { it.second }.toSet())
		(1..8).forEach { id -> assertTrue("Job $id must keep receiving updates", posts.count { it.second == id } >= 2) }
	}

	@Test fun pendingProgressIsCoalescedAndCompletedJobsAreForgotten() {
		val updates = ProgressNotificationUpdates<String>(1)
		assertEquals(mapOf(1 to "first"), updates.select(mapOf(1 to "first", 2 to "old"), 0))
		assertTrue(updates.select(mapOf(2 to "latest"), 500).isEmpty())
		assertEquals(mapOf(2 to "latest"), updates.select(mapOf(2 to "latest"), 1000))
		assertTrue(updates.select(emptyMap(), 2000).isEmpty())
		assertEquals(mapOf(2 to "latest"), updates.select(mapOf(2 to "latest"), 2000))
	}

	@Test fun newlyStartedJobsTakePriorityOverExistingProgressUpdates() {
		val updates = ProgressNotificationUpdates<String>(1)
		updates.record(1, "old", 0)
		assertEquals(mapOf(2 to "new job"), updates.select(mapOf(1 to "new progress", 2 to "new job"), 1000))
		assertEquals(mapOf(1 to "latest progress"), updates.select(mapOf(1 to "latest progress", 2 to "new job"), 2000))
	}
}
