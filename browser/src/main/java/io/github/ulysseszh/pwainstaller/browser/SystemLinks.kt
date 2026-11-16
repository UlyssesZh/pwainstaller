package io.github.ulysseszh.pwainstaller.browser

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/** Use Android's associations for the destination URL, avoiding a return to the caller. */
object SystemLinks {
	fun intent(
		context: Context,
		uri: Uri,
	): Intent {
		val view = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
		val manager = context.packageManager
		// App-link resolution can filter browsers out when the caller owns the
		// verified domain. Include those alternatives when excluding the caller.
		val queryFlags =
			PackageManager.MATCH_DEFAULT_ONLY or
				if (Build.VERSION.SDK_INT >= 23) {
					PackageManager.MATCH_ALL
				} else {
					0
				}
		val matches =
			manager
				.queryIntentActivities(view, queryFlags)
				.map { ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
				.distinct()
		val own = matches.filter { it.packageName == context.packageName }
		if (own.isEmpty()) {
			return view
		}
		val others = matches - own.toSet()
		val preferred =
			manager
				.resolveActivity(view, PackageManager.MATCH_DEFAULT_ONLY)
				?.activityInfo
				?.let { ComponentName(it.packageName, it.name) }
		if (preferred in others) {
			return view
		}
		// Android can return only the verified app even with MATCH_ALL. If
		// that app is the caller, resolve a browser independently to escape it.
		if (others.isEmpty()) {
			return SystemBrowser.intent(context, uri)
		}
		if (others.size == 1) {
			return Intent(view).setComponent(others.single())
		}
		if (Build.VERSION.SDK_INT >= 24) {
			return Intent
				.createChooser(view, null)
				.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, own.toTypedArray())
		}
		// Android 5 and 6 cannot exclude components from an implicit chooser.
		val options = others.map { Intent(view).setComponent(it) }
		return Intent
			.createChooser(options.first(), null)
			.putExtra(Intent.EXTRA_INITIAL_INTENTS, options.drop(1).toTypedArray())
	}
}
