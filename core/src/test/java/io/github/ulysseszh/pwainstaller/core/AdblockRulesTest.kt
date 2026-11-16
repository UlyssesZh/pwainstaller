package io.github.ulysseszh.pwainstaller.core

import org.junit.Assert.*
import org.junit.Test

class AdblockRulesTest {
	@Test fun selectionMatchesTheNetworkParserWithoutCompilingOrdinaryUrlPatterns() {
		val rules =
			listOf(
				"##.generic",
				"example.com#@#.generic",
				"example.com#%#//scriptlet('set-cookie', 'x', '1')",
				"example.*#?#.notice:contains(Cookies)",
				"~example.com,other.test##.excluded",
				"/example\\.com/##.regex-domain",
				"xn--bcher-kva.example##.idn",
				"bücher.example##.unicode",
				"||ads.test^",
				"@@||site.example^\$document",
				"@@||site.example^\$generichide",
				"||ads.test^\$script,~image,domain=example.com|~excluded.example.com,third-party,important,match-case",
				"||ads.test^\$xhr,~first-party",
				"||ads.test^\$~third-party,~match-case,~important",
				"||ads.test^\$~domain=example.com",
				"||ads.test^\$replace=/a/b/",
				"||ads.test^\$generichide",
				"||ads.test^\$unknown",
				"/a(b|c)+/\$domain=example.com,script",
				"/[/",
				"/bad[/\$image",
				"/foo$/",
				"|||*^\\E[é]#fragment|",
				"||example.com/price\$5",
				"//",
				"@@",
				"! Comment",
				"[Adblock Plus]",
			) + (1..200).map { "||ad-$it.example/path*^\$domain=example.com|~excluded.example.com,script" }
		val text = rules.joinToString("\n")
		for (url in listOf("https://example.com/", "https://excluded.example.com/", "https://other.test/", "https://xn--bcher-kva.example/")) {
			val host = AdblockRules.host(url)
			val expected =
				AdblockRules
					.lines(text)
					.filter { line ->
						val cosmetic = AdblockRules.cosmetic(line)
						if (cosmetic != null) {
							AdblockRules.domainsMatch(cosmetic.domains, host)
						} else {
							AdblockRules.network(line)?.domainAllowed(host) == true
						}
					}.joinToString("\n")
			assertEquals(url, expected, AdblockRules.select(text, url))
		}
		assertNull(AdblockRules.cosmetic("https://example.com/path#fragment"))
		assertEquals("##", AdblockRules.cosmetic("example.com##a[href='#fragment']")!!.operator)
		for (marker in listOf("#@\$?#", "#\$?#", "#@%#", "#@?#", "#@\$#", "#@#", "#%#", "#?#", "#\$#", "##")) {
			val rule = requireNotNull(AdblockRules.cosmetic("#fragment.example.com$marker.notice"))
			assertEquals(marker, rule.operator)
			assertEquals("#fragment.example.com", rule.domains)
			assertEquals(".notice", rule.body)
		}
	}

	@Test fun selectsDomainRulesWithGenericDefaultsAndExclusions() {
		val text =
			"""
			! comment
			##.global
			example.com,another.test##.cookie
			~example.com##.excluded
			elsewhere.test##.elsewhere
			example.*#${'$'}#body { overflow: auto !important }
			||ads.test^${'$'}domain=example.com|another.test,script
			||tracker.test^${'$'}domain=elsewhere.test
			""".trimIndent().replace("\\$", "$")
		val selected = AdblockRules.select(text, "https://shop.example.com/app/")
		assertTrue(selected.contains("##.global"))
		assertTrue(selected.contains("##.cookie"))
		assertTrue(selected.contains("overflow"))
		assertTrue(selected.contains("||ads.test^"))
		assertFalse(selected.contains("excluded"))
		assertFalse(selected.contains("elsewhere"))
	}

	@Test fun networkRulesUseDomainAnchorsResourceTypesAndExceptions() {
		val engine =
			AdblockRules.Engine(
				"""
				||ads.example^${'$'}script,domain=site.example
				@@||ads.example/allowed.js${'$'}script
				||ads.example/important.js${'$'}script,important
				@@||ads.example/important.js${'$'}script
				""".trimIndent().replace("\\$", "$"),
			)
		assertTrue(engine.blocks("https://sub.ads.example/banner.js", "https://site.example/", "script"))
		assertFalse(engine.blocks("https://ads.example/banner.png", "https://site.example/", "image"))
		assertFalse(engine.blocks("https://ads.example/banner.js", "https://elsewhere.example/", "script"))
		assertFalse(engine.blocks("https://ads.example.evil/banner.js", "https://site.example/", "script"))
		assertFalse(engine.blocks("https://ads.example/allowed.js", "https://site.example/", "script"))
		assertTrue(engine.blocks("https://ads.example/important.js", "https://site.example/", "script"))
	}

	@Test fun thirdPartyUsesThePublicSuffixList() {
		val engine = AdblockRules.Engine("||example.co.uk^\$third-party")
		assertFalse(engine.blocks("https://cdn.example.co.uk/a", "https://www.example.co.uk/", "other"))
		assertTrue(engine.blocks("https://cdn.example.co.uk/a", "https://another.co.uk/", "other"))
	}

	@Test fun documentAndCosmeticExceptionsAreRespected() {
		val engine =
			AdblockRules.Engine(
				"""
				##.cookie
				example.com#@#.cookie
				||ad.test^
				@@||allowed.test^${'$'}document
				""".trimIndent().replace("\\$", "$"),
			)
		assertTrue(engine.cosmeticsFor("https://example.com/").isEmpty())
		assertFalse(engine.blocks("https://ad.test/a", "https://allowed.test/", "other"))
		assertTrue(engine.cosmeticsFor("https://allowed.test/").isEmpty())
		assertTrue(engine.blocks("https://ad.test/a", "https://other.test/", "other"))
	}

	@Test fun unsupportedTransformationsDoNotBecomeBroadBlocks() {
		assertNull(AdblockRules.network("||example.com^\$replace=/a/b/"))
		assertNull(AdblockRules.network("||example.com^\$unknown"))
		assertThrows(IllegalArgumentException::class.java) { AdblockRules.validate("x".repeat(AdblockRules.MAX_LENGTH + 1)) }
	}

	@Test fun largeSnapshotsKeepTheEightMebibyteUtf8BudgetAndAllowLongCosmeticRules() {
		val line = "a".repeat(4095) + "\n"
		val rules = line.repeat(8 * 1024 * 1024 / line.length)
		AdblockRules.validate(rules)
		assertThrows(IllegalArgumentException::class.java) { AdblockRules.validate(rules + "\n") }
		val failure =
			assertThrows(IllegalArgumentException::class.java) {
				AdblockRules.validate("é".repeat(AdblockRules.MAX_LENGTH / 2 + 1))
			}
		assertTrue(failure.message!!.contains("8 MiB"))
		assertThrows(IllegalArgumentException::class.java) { AdblockRules.validate("a".repeat(AdblockRules.MAX_LINE_LENGTH + 1)) }
		AdblockRules.validate("facebook.com#%#" + "a".repeat(11000))
		assertNull(AdblockRules.network("||ad.test^" + "a".repeat(11000)))
		assertThrows(IllegalArgumentException::class.java) { AdblockRules.validate("||ad.test^\u0000") }
	}
}
