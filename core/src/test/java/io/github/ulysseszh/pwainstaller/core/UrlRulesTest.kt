package io.github.ulysseszh.pwainstaller.core

import org.junit.Assert.*
import org.junit.Test

class UrlRulesTest {
	@Test fun androidLinksKeepEachScopeAuthorityAndItsOwnPathsTogether() {
		val filters =
			UrlRules.androidFilters(
				"https://example.com/",
				"https://example.com/app\nhttp://other.example:8080/files/\nhttps://*.third.example/shared/*",
			)
		assertEquals(4, filters.size)
		assertTrue(filters.all { it.categories.contains("android.intent.category.BROWSABLE") })
		assertEquals(listOf("example.com", "other.example", "third.example", "*.third.example"), filters.map { it.data.first().getValue("host") })
		assertEquals(listOf("path" to "/app", "pathPrefix" to "/app/"), filters.first().data.map { it.entries.last().toPair() })
		assertEquals("8080", filters[1].data.first()["port"])
		assertEquals("/shared/.*", filters[2].data.first()["pathPattern"])
		assertEquals(2, UrlRules.androidFilters("https://example.com/", "").size)
	}

	@Test fun anyPlainScopeUrlAllowsItsOwnDescendantsAndCanMixWithPatterns() {
		val scopes = "https://example.com/app\nhttps://login.example.net/session/\nhttps://*.example.org/shared/*"
		listOf(
			"https://example.com/app",
			"https://example.com/app/child?x=1",
			"https://login.example.net/session/next",
			"https://sub.example.org/shared/file",
		).forEach {
			assertTrue(it, UrlRules.matches(it, "https://other.com/", scopes))
		}
		listOf(
			"https://example.com/application",
			"https://login.example.net/session-other",
			"http://example.com/app/",
			"https://example.com:8443/app/",
			"https://example.com/app/%2e%2e/outside",
		).forEach {
			assertFalse(it, UrlRules.matches(it, "https://other.com/", scopes))
		}
		assertTrue(UrlRules.matches("https://example.com/a%20b/child", "https://other.com/", "https://example.com/a%20b"))
	}

	@Test fun defaultsToBaseUrlAndDescendantsOnTheSameOrigin() {
		val base = "https://example.com/app"
		listOf(base, "$base?query=1#section", "$base/", "$base/a/b").forEach { assertTrue(it, UrlRules.matches(it, base, "")) }
		listOf(
			"https://example.com/application",
			"https://example.com/outside",
			"https://example.com.evil/app",
			"https://evil.com/app",
			"http://example.com/app",
			"https://example.com:8443/app",
		).forEach {
			assertFalse(it, UrlRules.matches(it, base, ""))
		}
		assertTrue(UrlRules.matches("https://example.com/anywhere", "https://example.com", ""))
	}

	@Test fun bareDomainsIncludeWwwWithoutBroadeningSubdomainsOrExplicitScopes() {
		assertEquals("https://ebay.com/\nhttps://www.ebay.com/", UrlRules.defaultScopes("https://ebay.com"))
		assertTrue(UrlRules.matches("https://www.ebay.com/item", "https://ebay.com", ""))
		assertFalse(UrlRules.matches("https://www.ebay.com/item", "https://ebay.com", "https://ebay.com/"))
		assertEquals("https://shop.example.co.uk/app/", UrlRules.defaultScopes("https://shop.example.co.uk/app/"))
		assertEquals(
			"https://example.co.uk:8443/app/\nhttps://www.example.co.uk:8443/app/",
			UrlRules.defaultScopes("https://example.co.uk:8443/app/"),
		)
		listOf("https://www.example.com/", "https://co.uk/", "http://127.0.0.1/", "http://localhost/", "https://blogspot.com/").forEach {
			assertEquals(it, UrlRules.defaultScopes(it))
		}
	}

	@Test fun normalizesDotSegmentsBeforeCheckingTheBasePath() {
		listOf("https://example.com/app/../outside", "https://example.com/app/%2e%2e/outside", "https://example.com/app/%2E%2E/outside").forEach {
			assertFalse(it, UrlRules.matches(it, "https://example.com/app/", ""))
		}
	}

	@Test fun supportsMultiplePatternsAndConstrainedWildcardHosts() {
		val rules = "https://*.example.com/app/*\nhttps://login.example.net/*"
		listOf("https://example.com/app/a", "https://sub.example.com/app/a", "https://login.example.net/sign-in").forEach {
			assertTrue(UrlRules.matches(it, "https://other.com/", rules))
		}
		listOf(
			"https://example.com.evil/app/a",
			"https://evil.com/path.example.com/app/a",
			"https://sub.example.com/outside",
			"https://login.example.net:8443/a",
		).forEach {
			assertFalse(UrlRules.matches(it, "https://other.com/", rules))
		}
	}

	@Test fun supportsExplicitSchemePortAndQueryRules() {
		assertTrue(UrlRules.matches("http://localhost:8080/a", "https://example.com/", "*://localhost:*/a"))
		assertTrue(UrlRules.matches("https://example.com/a?mode=edit#tab", "https://example.com/", "https://example.com/a?mode=*"))
		assertFalse(UrlRules.matches("https://example.com/a?other=value", "https://example.com/", "https://example.com/a?mode=*"))
		assertTrue(UrlRules.matches("https://[::1]:8443/a", "https://example.com/", "https://[::1]:8443/*"))
	}

	@Test fun rejectsUnsupportedSchemesAndInvalidPatterns() {
		listOf("file:///private", "javascript:alert(1)", "https://user:password@example.com/app").forEach {
			assertFalse(UrlRules.matches(it, "https://example.com/", "*://*/*"))
		}
		listOf(
			".*",
			"ftp://example.com/*",
			"https://user@example.com/*",
			"https://example.com:99999/*",
			"https://example.com/a b",
			"https://example.com/#*",
		).forEach {
			assertThrows(it, IllegalArgumentException::class.java) { UrlRules.validate(it) }
		}
	}
}
