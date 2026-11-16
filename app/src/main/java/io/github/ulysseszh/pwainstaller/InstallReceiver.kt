package io.github.ulysseszh.pwainstaller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat

class InstallReceiver : BroadcastReceiver() {
	override fun onReceive(
		context: Context,
		intent: Intent,
	) {
		val token = intent.data?.schemeSpecificPart ?: return
		val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
		if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
			IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)?.let { confirmation ->
				val plan =
					try {
						InstallCoordinator.summary(context, token)
					} catch (_: java.io.FileNotFoundException) {
						return
					}.put("state", "confirmation")
						.put("confirmation", confirmation.toUri(Intent.URI_INTENT_SCHEME))
				InstallCoordinator.write(context, token, plan)
				BuildService.signal(context)
				runCatching { context.startActivity(confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
			}
			return
		}
		val pending = goAsync()
		InstallerApplication.installEvents.execute {
			try {
				if (status == PackageInstaller.STATUS_SUCCESS) {
					InstallCoordinator.installed(context, token)
				} else if (status != PackageInstaller.STATUS_FAILURE_CONFLICT || !InstallCoordinator.signatureRejected(context, token)) {
					InstallCoordinator.fail(context, token, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
				}
			} catch (
				error: Exception,
			) {
				runCatching { InstallCoordinator.fail(context, token, error.toString()) }
			} finally {
				BuildService.signal(context)
				context.sendBroadcast(Intent(CHANGED).setPackage(context.packageName))
				pending.finish()
			}
		}
	}

	companion object {
		const val CHANGED = "io.github.ulysseszh.pwainstaller.INSTALL_CHANGED"
	}
}
