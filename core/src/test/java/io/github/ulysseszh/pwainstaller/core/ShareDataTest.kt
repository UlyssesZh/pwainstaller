package io.github.ulysseszh.pwainstaller.core

import org.junit.Assert.*
import org.junit.Test

class ShareDataTest {
	private val names = mapOf("title" to "subject", "text" to "message", "url" to "link")

	@Test fun mixedTextAndLinksRemainOneTextField() {
		val text = "PWA share test 123 https://example.com/"
		assertEquals(listOf("message" to text), ShareData.fromAndroid(text).fields(names))
		assertEquals(
			listOf("message" to "https://example.com/\nSome more text"),
			ShareData.fromAndroid("https://example.com/\nSome more text").fields(names),
		)
	}

	@Test fun bareUrlsBecomeOnlyTheUrlFieldWhileTitlesRemainIndependent() {
		assertEquals(
			listOf("subject" to "A page", "link" to "https://example.com/"),
			ShareData.fromAndroid(" https://EXAMPLE.com ", "A page").fields(names),
		)
	}

	@Test fun bareUrlsStillWorkWithTextOnlyTargets() {
		assertEquals(
			listOf("message" to "https://example.com/"),
			ShareData.fromAndroid("https://example.com/").fields(mapOf("text" to "message")),
		)
		assertTrue(ShareData.fromAndroid("Text containing https://example.com/").fields(mapOf("url" to "link")).isEmpty())
	}

	@Test fun distinctTextAndUrlFromStructuredShareDataCanCoexist() {
		assertEquals(
			listOf("message" to "A description", "link" to "https://example.com/"),
			ShareData(text = "A description", url = "https://example.com/").fields(names),
		)
	}
}
