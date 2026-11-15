package io.github.ulysseszh.pwainstaller

import android.view.View
import android.widget.LinearLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator

class FetchDialog(screen: Screen) {
	private val content = LinearLayout(screen).apply {
		orientation = LinearLayout.VERTICAL
		setPadding(screen.dp(24), screen.dp(12), screen.dp(24), screen.dp(24))
	}
	private val status = screen.text(content, screen.getString(R.string.fetch_preparing), 16)
	private val progress = LinearProgressIndicator(screen).apply {
		max = 100
		content.addView(this, LinearLayout.LayoutParams(-1, screen.dp(6)).apply { topMargin = screen.dp(12) })
	}
	private val dialog = MaterialAlertDialogBuilder(screen).setTitle(R.string.fetching_web_app).setView(content).setCancelable(false).create()
	fun show(value: FetchProgress) {
		if (value.stage == FetchProgress.Stage.FILE) dialog.setTitle(R.string.fetch_user_file)
		val label = status.context.getString(when (value.stage) {
			FetchProgress.Stage.PREPARING -> R.string.fetch_preparing
			FetchProgress.Stage.PAGE -> R.string.fetch_page
			FetchProgress.Stage.MANIFEST -> R.string.fetch_manifest_progress
			FetchProgress.Stage.ICON -> R.string.fetch_icon
			FetchProgress.Stage.FILTERS -> R.string.fetch_filter_rules
			FetchProgress.Stage.FILE -> R.string.fetch_user_file_progress
		})
		status.text = value.percent?.let { "$label · $it%" } ?: label
		progress.contentDescription = status.text
		progress.visibility = View.INVISIBLE
		progress.isIndeterminate = value.percent == null
		value.percent?.let { progress.setProgressCompat(it, false) }
		progress.visibility = View.VISIBLE
		if (!dialog.isShowing) dialog.show()
	}
	fun dismiss() = dialog.dismiss()
}
