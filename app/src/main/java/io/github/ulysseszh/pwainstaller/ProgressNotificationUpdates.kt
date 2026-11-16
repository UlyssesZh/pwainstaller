package io.github.ulysseszh.pwainstaller

import java.util.ArrayDeque

/** Coalesce progress and share an update budget fairly across concurrent builds. */
internal class ProgressNotificationUpdates<T : Any>(
	private val perSecond: Int = 4,
) {
	private val published = mutableMapOf<Int, T>()
	private val publishedAt = mutableMapOf<Int, Long>()
	private val pending = linkedMapOf<Int, T>()
	private val timestamps = ArrayDeque<Long>()

	fun retainVisible(
		ids: Set<Int>,
		now: Long,
	) {
		// Android can drop an update or cancel an old notification during a
		// service restart. A missing row must be eligible for publication again.
		published.keys.removeAll { it !in ids && now - publishedAt.getValue(it) >= 1000 }
		publishedAt.keys.retainAll(published.keys)
	}

	fun isPublished(id: Int) = id in published

	fun record(
		id: Int,
		value: T,
		now: Long,
	) {
		published[id] = value
		publishedAt[id] = now
		pending.remove(id)
		expire(now)
		timestamps.addLast(now)
	}

	fun select(
		desired: Map<Int, T>,
		now: Long,
	): Map<Int, T> {
		published.keys.retainAll(desired.keys)
		publishedAt.keys.retainAll(desired.keys)
		pending.keys.retainAll(desired.keys)
		desired.forEach { (id, value) ->
			if (published[id] == value) {
				pending.remove(id)
			} else {
				pending[id] = value
			}
		}
		expire(now)
		return linkedMapOf<Int, T>().apply {
			while (timestamps.size < perSecond && pending.isNotEmpty()) {
				// Make new jobs visible before updating progress on existing ones.
				val next = pending.entries.firstOrNull { it.key !in published } ?: pending.entries.first()
				val id = next.key
				val value = next.value
				pending.remove(id)
				record(id, value, now)
				put(id, value)
			}
		}
	}

	private fun expire(now: Long) {
		while (timestamps.isNotEmpty() && now - timestamps.first >= 1000) {
			timestamps.removeFirst()
		}
	}
}
