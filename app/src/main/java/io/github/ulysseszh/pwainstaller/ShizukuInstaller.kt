package io.github.ulysseszh.pwainstaller

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import androidx.annotation.RequiresApi
import rikka.shizuku.Shizuku
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RequiresApi(24)
object ShizukuInstaller {
	fun ready() = runCatching { Shizuku.pingBinder() && Shizuku.getVersion() >= 13 }.getOrDefault(false)
	fun authorized() = ready() && runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
	fun install(context: Context, apk: File) {
		ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
			transact(context, ShizukuInstallerService.INSTALL) { data -> data.writeParcelable(descriptor, 0) }
		}
	}
	fun uninstall(context: Context, name: String) {
		require(io.github.ulysseszh.pwainstaller.core.PackageNames.valid(name))
		transact(context, ShizukuInstallerService.UNINSTALL) { it.writeString(name) }
	}
	@Synchronized private fun transact(context: Context, transaction: Int, payload: (Parcel) -> Unit) {
		check(authorized()) { context.getString(R.string.shizuku_not_ready) }
		val args = Shizuku.UserServiceArgs(ComponentName(context, ShizukuInstallerService::class.java))
			.daemon(false).processNameSuffix("installer").tag("pwa-installer").version(1)
		val connected = CountDownLatch(1)
		var remote: IBinder? = null
		var bindFailure: Exception? = null
		val connection = object : ServiceConnection {
			override fun onServiceConnected(name: ComponentName, service: IBinder) { remote = service; connected.countDown() }
			override fun onServiceDisconnected(name: ComponentName) { remote = null; connected.countDown() }
		}
		val main = Handler(Looper.getMainLooper())
		main.post {
			try { Shizuku.bindUserService(args, connection) }
			catch (failure: Exception) { bindFailure = failure; connected.countDown() }
		}
		try {
			check(connected.await(20, TimeUnit.SECONDS)) { "Shizuku installer service did not start" }
			bindFailure?.let { throw it }
			val binder = checkNotNull(remote)
			val data = Parcel.obtain(); val reply = Parcel.obtain()
			try {
				data.writeInterfaceToken(ShizukuInstallerService.DESCRIPTOR)
				data.writeInt(android.os.Process.myUid() / 100000)
				payload(data)
				check(binder.transact(transaction, data, reply, 0))
				reply.readException()
			} finally { data.recycle(); reply.recycle() }
		} finally { main.post { runCatching { Shizuku.unbindUserService(args, connection, true) } } }
	}
}
