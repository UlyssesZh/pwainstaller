package io.github.ulysseszh.pwainstaller.core

import org.junit.Assert.*
import org.junit.Test

class PackageNamesTest {
	@Test fun defaultNamesRetainTheWholeHashAndAreInstallable() {
		var numeric = false
		var alphabetic = false
		repeat(100) {
			val id = "https://example.com/$it"
			val hash = PackageNames.digest("SHA-1", id.toByteArray(Charsets.UTF_8))
			val name = PackageNames.forId(id)
			assertTrue(PackageNames.valid(name))
			assertTrue(name.endsWith(hash))
			assertEquals("${PackageNames.INSTALLER}.p_$hash", name)
			if (hash.first().isDigit()) numeric = true else alphabetic = true
		}
		assertTrue(numeric && alphabetic)
	}
	@Test fun rejectsInvalidPackageSegmentsAndInstallerIdentity() {
		listOf(PackageNames.INSTALLER, "com.123abc", "com..app", "com._app", "com.app;bad", "single", "com.app.")
			.forEach { assertFalse(it, PackageNames.valid(it)) }
		assertTrue(PackageNames.valid("com.Example_app.a123"))
	}
	@Test fun originsIncludePortsAndNormalizeDefaults() {
		assertTrue(PackageNames.sameOrigin("https://example.com:443/a", "https://EXAMPLE.com/b"))
		assertFalse(PackageNames.sameOrigin("https://example.com/a", "http://example.com/a"))
		assertFalse(PackageNames.sameOrigin("https://example.com:444/a", "https://example.com/a"))
	}
	@Test fun webUrlsRejectCredentialsAndDangerousSchemes() {
		listOf("file:///data/data/secrets", "javascript:alert(1)", "https://user:pass@example.com/", "ftp://example.com/").forEach {
			assertThrows(IllegalArgumentException::class.java) { PackageNames.webUrl(it) }
		}
		assertEquals("https://example.com/app", PackageNames.webUrl("https://example.com/a/../app"))
	}
	@Test fun manifestIdentityUsesTheStartOrigin() {
		listOf("foo", "./foo", "../foo", "/foo").forEach {
			assertEquals("https://example.com/foo", PackageNames.resolveId("https://example.com/my-app/start", it))
		}
		assertEquals("https://example.com/my-app/", PackageNames.resolveId("https://example.com/my-app/#here", ""))
	}
}
