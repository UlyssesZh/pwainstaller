package io.github.ulysseszh.pwainstaller.browser

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build

/** Current APK signers, without broadening certificate pins to signing history. */
object PackageSignatures {
	fun flags(): Int {
		return if (Build.VERSION.SDK_INT >= 28) {
			PackageManager.GET_SIGNING_CERTIFICATES
		} else {
			legacyFlags()
		}
	}

	fun read(info: PackageInfo): Array<out Signature> {
		return if (Build.VERSION.SDK_INT >= 28) {
			info.signingInfo?.apkContentsSigners.orEmpty()
		} else {
			legacySignatures(info)
		}
	}

	// API 21–27 have no SigningInfo API. Keep their deprecated calls isolated.
	@Suppress("DEPRECATION")
	private fun legacyFlags(): Int = PackageManager.GET_SIGNATURES

	@Suppress("DEPRECATION")
	private fun legacySignatures(info: PackageInfo): Array<out Signature> = info.signatures.orEmpty()
}
