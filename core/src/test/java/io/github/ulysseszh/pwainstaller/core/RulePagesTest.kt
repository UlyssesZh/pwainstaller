package io.github.ulysseszh.pwainstaller.core

import org.junit.Assert.*
import org.junit.Test

class RulePagesTest {
	@Test fun pagesKeepEveryCharacterAndBoundLayoutWork() {
		val text = ("reddit.com##.banner-🐉\r\n").repeat(40000)
		val pages = RulePages.split(text)
		assertEquals(text, pages.joinToString(""))
		assertTrue(pages.size > 1)
		pages.forEach { page ->
			assertTrue(page.length <= RulePages.CHARACTERS)
			assertTrue(page.count { it == '\n' } <= 128)
		}
		assertEquals(listOf(""), RulePages.split(""))
	}

	@Test fun veryLongLinesAndSurrogatePairsRemainIntact() {
		val text = "a".repeat(8191) + "🐉" + "b".repeat(12000)
		val pages = RulePages.split(text)
		assertEquals(text, pages.joinToString(""))
		assertFalse(pages.any { it.first().isLowSurrogate() || it.last().isHighSurrogate() })
		val edited = pages.toMutableList().apply { this[1] = this[1] + "\n! edit" }
		assertEquals(pages.first() + pages[1] + "\n! edit" + pages.drop(2).joinToString(""), edited.joinToString(""))
	}
}
