package io.github.ulysseszh.pwainstaller

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.io.File

object Packages {
    fun installed(context: Context, name: String): Boolean = try { context.packageManager.getPackageInfo(name, 0); true } catch (_: PackageManager.NameNotFoundException) { false }
    fun uninstall(context: Context, name: String, completed: () -> Unit = {}) {
        if (InstallationSettings.method(context) == "shizuku" && Build.VERSION.SDK_INT >= 24) {
            check(ShizukuInstaller.authorized()) { context.getString(R.string.shizuku_not_ready) }
            InstallerApplication.installEvents.execute {
                try {
                    ShizukuInstaller.uninstall(context.applicationContext, name)
                    context.sendBroadcast(Intent(InstallReceiver.CHANGED).setPackage(context.packageName))
                    android.os.Handler(android.os.Looper.getMainLooper()).post { completed() }
                } catch (failure: Exception) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post { if (context is Screen) context.error(failure) }
                }
            }
        } else context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$name")))
    }
    @JvmStatic fun profile(name: String): Uri = Uri.parse("content://$name.data/profile")
    fun signatureConflict(context: Context, name: String, apk: File): Boolean {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val installed = try { context.packageManager.getPackageInfo(name, flags) }
            catch (_: PackageManager.NameNotFoundException) { return false }
        val candidate = requireNotNull(context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)) { "Invalid APK" }
        val existing = if (Build.VERSION.SDK_INT >= 28) installed.signingInfo?.apkContentsSigners else installed.signatures
        val incoming = if (Build.VERSION.SDK_INT >= 28) candidate.signingInfo?.apkContentsSigners else candidate.signatures
        require(!existing.isNullOrEmpty() && !incoming.isNullOrEmpty()) { "Missing APK signature" }
        return existing.size != incoming.size || existing.any { a -> incoming.none { b -> a.toByteArray().contentEquals(b.toByteArray()) } }
    }
}
