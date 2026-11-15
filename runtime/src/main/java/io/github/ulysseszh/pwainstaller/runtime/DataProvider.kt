package io.github.ulysseszh.pwainstaller.runtime

import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.*
import android.system.ErrnoException
import android.system.OsConstants
import io.github.ulysseszh.pwainstaller.core.DataArchive
import io.github.ulysseszh.pwainstaller.core.PackageNames
import java.io.*
import java.nio.channels.FileLock

/** A private profile bridge: checks both the caller package and its embedded certificate pin. */
class DataProvider : ContentProvider() {
	override fun onCreate() = true
	private val appContext get() = requireNotNull(context)
	private val dataDir get() = File(appContext.applicationInfo.dataDir)

	private fun authorize() {
		try {
			val pm = appContext.packageManager
			val packages = pm.getPackagesForUid(Binder.getCallingUid())
			if (packages?.size != 1 || packages[0] != PackageNames.INSTALLER) throw SecurityException("Installer identity required")
			val expected = Config.read(appContext).getString("installerCertificate")
			val info = pm.getPackageInfo(PackageNames.INSTALLER, if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
			val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
			if (signatures?.size != 1 || expected != PackageNames.digest("SHA-256", signatures[0].toByteArray())) throw SecurityException("Installer certificate mismatch")
		} catch (error: SecurityException) { throw error } catch (error: Exception) { throw SecurityException("Cannot verify installer", error) }
	}
	private fun stopBrowser() {
		val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
		manager.runningAppProcesses?.filter { it.uid == Process.myUid() && it.processName == "${appContext.packageName}:web" }
			?.forEach { Process.killProcess(it.pid) }
	}
	private fun exclusive(transfer: () -> Unit) {
		stopBrowser()
		RandomAccessFile(File(appContext.filesDir, "profile.lock"), "rw").use { file ->
			val deadline = System.currentTimeMillis() + 10000
			var lock: FileLock? = null
			while (lock == null && System.currentTimeMillis() < deadline) {
				lock = try { file.channel.tryLock() } catch (error: IOException) {
					// Android 5's FileChannel throws EAGAIN while another process releases a lock.
					val errno = (error.cause as? ErrnoException)?.errno
					if (errno == OsConstants.EAGAIN || errno == OsConstants.EACCES || error.message.orEmpty().contains("EAGAIN")) null else throw error
				}
				if (lock == null) Thread.sleep(50)
			}
			if (lock == null) throw IOException("Browser is busy")
			try { transfer() } finally { lock.release() }
		}
	}
	@Synchronized override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
		authorize()
		if (uri.path != "/profile") throw FileNotFoundException("Unknown path")
		try {
			return when (mode) {
				"r" -> {
					val snapshot = File(appContext.cacheDir, "profile-export.zip")
					exclusive { snapshot.outputStream().use { DataArchive.exportProfile(dataDir, it) } }
					ParcelFileDescriptor.open(snapshot, ParcelFileDescriptor.MODE_READ_ONLY).also { snapshot.delete() }
				}
				"w", "wt" -> ParcelFileDescriptor.open(File(appContext.cacheDir, "profile-import.zip"), ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_WRITE_ONLY)
				else -> throw IOException("Unsupported mode")
			}
		} catch (error: Exception) { throw FileNotFoundException(error.toString()).apply { initCause(error) } }
	}
	@Synchronized override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
		authorize()
		require(method == "commit") { "Unknown operation" }
		val staged = File(appContext.cacheDir, "profile-import.zip")
		try {
			exclusive { staged.inputStream().use { DataArchive.importProfile(dataDir, it) } }
			return Bundle.EMPTY
		} catch (error: Exception) { throw IllegalStateException("Profile import failed", error) } finally { staged.delete() }
	}
	override fun getType(uri: Uri): String { authorize(); return "application/zip" }
	override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, order: String?): Cursor? { authorize(); throw UnsupportedOperationException() }
	override fun insert(uri: Uri, values: ContentValues?): Uri? { authorize(); throw UnsupportedOperationException() }
	override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?): Int { authorize(); throw UnsupportedOperationException() }
	override fun delete(uri: Uri, selection: String?, args: Array<out String>?): Int { authorize(); throw UnsupportedOperationException() }
}
