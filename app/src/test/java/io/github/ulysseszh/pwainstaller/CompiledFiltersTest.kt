package io.github.ulysseszh.pwainstaller

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

class CompiledFiltersTest {
	private fun asset(path: String) = File("src/main/assets/$path").readText()

	@Test fun persistsExactAssetsAndInvalidatesChangedRulesVersionsAndCorruptFiles() {
		val directory =
			kotlin.io.path
				.createTempDirectory("compiled-filters")
				.toFile()
		try {
			val rules = "example.com#%#//scriptlet('set-cookie', 'consent', 'true')\n##.banner"
			val progress = mutableListOf<Int>()
			val expected = CompiledFilters(directory, "installer-v1").get(rules, ::asset, progress::add)
			assertEquals(0, progress.first())
			assertEquals(100, progress.last())
			assertTrue(progress.zipWithNext().all { (a, b) -> a <= b })
			assertTrue(progress.size > 4)
			progress.clear()
			val cached = CompiledFilters(directory, "installer-v1").get(rules, { error("Cache hit must not read assets") }, progress::add)
			assertEquals(listOf(0, 100), progress)
			assertEquals(expected.keys, cached.keys)
			expected.forEach { (name, bytes) -> assertArrayEquals(bytes, cached.getValue(name)) }
			assertTrue(CompiledFilters(directory, "installer-v1").get("", ::asset).isEmpty())
			val versioned = CompiledFilters(directory, "installer-v2").get(rules, ::asset)
			expected.forEach { (name, bytes) -> assertArrayEquals(bytes, versioned.getValue(name)) }
			assertEquals(3, directory.listFiles()!!.size)
			// A torn or corrupt cache must be repaired, never shipped into a generated APK.
			directory.listFiles()!!.forEach { it.writeBytes(byteArrayOf(1, 2, 3)) }
			val repaired = CompiledFilters(directory, "installer-v1").get(rules, ::asset)
			expected.forEach { (name, bytes) -> assertArrayEquals(bytes, repaired.getValue(name)) }
		} finally {
			directory.deleteRecursively()
		}
	}

	@Test fun simultaneousBuildsCompileTheSameSnapshotOnlyOnce() {
		val directory =
			kotlin.io.path
				.createTempDirectory("compiled-filters")
				.toFile()
		val executor = Executors.newFixedThreadPool(2)
		val entered = CountDownLatch(1)
		val release = CountDownLatch(1)
		val reads = AtomicInteger()
		try {
			val asset: (String) -> String = { path ->
				reads.incrementAndGet()
				entered.countDown()
				check(release.await(5, TimeUnit.SECONDS))
				asset(path)
			}
			val first = executor.submit(Callable { CompiledFilters(directory, "v1").get("##.banner", asset) })
			assertTrue(entered.await(5, TimeUnit.SECONDS))
			val second = executor.submit(Callable { CompiledFilters(directory, "v1").get("##.banner", asset) })
			release.countDown()
			assertArrayEquals(
				first.get(5, TimeUnit.SECONDS).getValue("assets/adblock.js"),
				second.get(5, TimeUnit.SECONDS).getValue("assets/adblock.js"),
			)
			assertEquals(1, reads.get())
		} finally {
			release.countDown()
			executor.shutdownNow()
			directory.deleteRecursively()
		}
	}
}
