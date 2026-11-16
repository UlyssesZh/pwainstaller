package io.github.ulysseszh.pwainstaller

import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import androidx.core.os.ParcelCompat
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Runs only inside an explicitly authorized Shizuku user-service process, never an app service. */
class ShizukuInstallerService(
	context: Context,
) : Binder() {
	private val ownerUid = context.applicationInfo.uid

	override fun onTransact(
		code: Int,
		data: Parcel,
		reply: Parcel?,
		flags: Int,
	): Boolean {
		val caller = getCallingUid()
		check(caller == ownerUid || caller == 0 || caller == 2000) { "Installer identity required" }
		if (code == 16777115) {
			android.os.Process.killProcess(android.os.Process.myPid())
			return true
		}
		if (code == IBinder.INTERFACE_TRANSACTION) {
			reply?.writeString(DESCRIPTOR)
			return true
		}
		if (code != INSTALL && code != UNINSTALL) {
			return super.onTransact(code, data, reply, flags)
		}
		data.enforceInterface(DESCRIPTOR)
		check(caller == ownerUid) { "Installer identity required" }
		val user = data.readInt().also { require(it in 0..21474) }
		val temporary = File("/data/local/tmp/pwa-installer-${UUID.randomUUID()}.apk")
		try {
			val command =
				if (code == INSTALL) {
					val descriptor =
						requireNotNull(
							ParcelCompat
								.readParcelable(data, ParcelFileDescriptor::class.java.classLoader, ParcelFileDescriptor::class.java),
						)
					ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input -> temporary.outputStream().use { output -> input.copyTo(output) } }
					check(temporary.setReadable(true, false))
					listOf("pm", "install", "-r", "--user", user.toString(), temporary.absolutePath)
				} else {
					val name = requireNotNull(data.readString())
					require(
						io.github.ulysseszh.pwainstaller.core.PackageNames
							.valid(name),
					)
					listOf("pm", "uninstall", "--user", user.toString(), name)
				}
			val process = ProcessBuilder(command).redirectErrorStream(true).start()
			val executor = Executors.newSingleThreadExecutor()
			try {
				val result =
					executor
						.submit<String> {
							process.inputStream
								.bufferedReader()
								.use { it.readText() }
								.also { process.waitFor() }
						}.get(120, TimeUnit.SECONDS)
				check(process.exitValue() == 0 && result.lineSequence().any { it.trim() == "Success" }) { result.take(65536) }
				reply!!.writeNoException()
				reply.writeString(result)
			} finally {
				process.destroy()
				executor.shutdownNow()
			}
		} catch (
			failure: Exception,
		) {
			reply!!.writeException(IllegalStateException(failure.message ?: failure.toString()))
		} finally {
			temporary.delete()
		}
		return true
	}

	companion object {
		const val DESCRIPTOR = "io.github.ulysseszh.pwainstaller.ShizukuInstaller"
		const val INSTALL = IBinder.FIRST_CALL_TRANSACTION
		const val UNINSTALL = IBinder.FIRST_CALL_TRANSACTION + 1
	}
}
