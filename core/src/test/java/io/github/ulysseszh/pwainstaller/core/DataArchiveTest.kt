package io.github.ulysseszh.pwainstaller.core

import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import java.io.*
import java.nio.file.Files
import java.util.zip.*

class DataArchiveTest {
	@get:Rule val temp = TemporaryFolder()
	private fun write(base: File, name: String, value: String) { File(base, name).apply { parentFile.mkdirs(); writeText(value) } }
	@Test fun restoresCookiesStorageAndPreferencesWithoutUnrelatedFiles() {
		val source = temp.newFolder()
		val target = temp.newFolder()
		write(source, "app_webview/Default/Cookies", "cookie-database")
		write(source, "app_webview/Default/IndexedDB/site/data", "storage")
		write(source, "shared_prefs/permissions.xml", "permissions")
		write(source, "databases/http_auth.db", "http credentials")
		write(source, "files/profile.lock", "private-lock")
		write(target, "app_webview/stale", "stale")
		write(target, "files/keep", "keep")
		val archive = ByteArrayOutputStream().also { DataArchive.exportProfile(source, it) }.toByteArray()
		DataArchive.importProfile(target, archive.inputStream())
		assertEquals("cookie-database", File(target, "app_webview/Default/Cookies").readText())
		assertEquals("storage", File(target, "app_webview/Default/IndexedDB/site/data").readText())
		assertEquals("permissions", File(target, "shared_prefs/permissions.xml").readText())
		assertEquals("http credentials", File(target, "databases/http_auth.db").readText())
		assertFalse(File(target, "app_webview/stale").exists())
		assertFalse(File(target, "files/profile.lock").exists())
		assertEquals("keep", File(target, "files/keep").readText())
	}
	private fun malformed(name: String, marker: Boolean): ByteArray {
		val bytes = ByteArrayOutputStream()
		ZipOutputStream(bytes).use {
			if (marker) { it.putNextEntry(ZipEntry("pwa-profile-v1")); it.closeEntry() }
			it.putNextEntry(ZipEntry(name)); it.write("evil".toByteArray()); it.closeEntry()
		}
		return bytes.toByteArray()
	}
	@Test fun rejectsTraversalAndUnexpectedRootsWithoutChangingLiveData() {
		listOf("app_webview/../../escaped", "/app_webview/file", "shared_prefs/../evil", "files/profile.lock", "app_webview\\evil", "app_webview/./bad").forEach { path ->
			val target = temp.newFolder()
			write(target, "app_webview/Cookies", "original")
			assertThrows(IOException::class.java) { DataArchive.importProfile(target, malformed(path, true).inputStream()) }
			assertEquals("original", File(target, "app_webview/Cookies").readText())
			assertEquals(1, target.list()!!.size)
		}
	}
	@Test fun rejectsNonProfileZipAndKeepsLiveData() {
		val target = temp.newFolder()
		write(target, "app_webview/Cookies", "original")
		assertThrows(IOException::class.java) { DataArchive.importProfile(target, malformed("app_webview/new", false).inputStream()) }
		assertEquals("original", File(target, "app_webview/Cookies").readText())
	}
	@Test fun rejectsSymlinksInExport() {
		val source = temp.newFolder()
		val outside = temp.newFolder()
		write(outside, "secret", "secret")
		File(source, "app_webview").mkdir()
		Files.createSymbolicLink(File(source, "app_webview/link").toPath(), File(outside, "secret").toPath())
		assertThrows(IOException::class.java) { DataArchive.exportProfile(source, ByteArrayOutputStream()) }
	}
	@Test fun emptyProfileCanBeTransferred() {
		val source = temp.newFolder()
		val target = temp.newFolder()
		write(target, "app_webview/old", "old")
		val bytes = ByteArrayOutputStream().also { DataArchive.exportProfile(source, it) }.toByteArray()
		DataArchive.importProfile(target, bytes.inputStream())
		assertEquals(0, target.list()!!.size)
	}
	@Test fun canonicalizesAndroidStyleDataDirectoryAliases() {
		val source = temp.newFolder()
		write(source, "app_webview/Default/Cookies", "cookie")
		val alias = File(temp.newFolder(), "alias")
		Files.createSymbolicLink(alias.toPath(), source.toPath())
		val bytes = ByteArrayOutputStream().also { DataArchive.exportProfile(alias, it) }.toByteArray()
		val target = temp.newFolder()
		DataArchive.importProfile(target, bytes.inputStream())
		assertEquals("cookie", File(target, "app_webview/Default/Cookies").readText())
	}
}
