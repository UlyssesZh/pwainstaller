package io.github.ulysseszh.pwainstaller

import android.content.Context
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileLock

/** Held for the :login process lifetime, including background Chromium cookie writes. */
internal object LoginProfile {
	private var file: RandomAccessFile? = null
	private var lease: FileLock? = null

	@Synchronized fun acquire(context: Context) {
		if (lease?.isValid == true) {
			return
		}
		val opened = RandomAccessFile(File(context.filesDir, "login-profile.lock"), "rw")
		val deadline = System.currentTimeMillis() + 10000
		try {
			while (lease == null && System.currentTimeMillis() < deadline) {
				lease =
					try {
						opened.channel.tryLock(0, Long.MAX_VALUE, true)
					} catch (error: IOException) {
						if (error.message.orEmpty().contains("EAGAIN") || error.message.orEmpty().contains("EACCES")) {
							null
						} else {
							throw error
						}
					}
				if (lease == null) {
					Thread.sleep(25)
				}
			}
			check(lease != null) { "Sign-in profile is busy" }
			file = opened
		} catch (error: Exception) {
			opened.close()
			throw error
		}
	}
}
