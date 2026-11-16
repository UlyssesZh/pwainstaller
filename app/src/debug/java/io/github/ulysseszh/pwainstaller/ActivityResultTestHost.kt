package io.github.ulysseszh.pwainstaller

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView

/** Debug-only hosts for a real result round trip across caller recreation. */
class ActivityResultTestHost : Screen() {
	override val resultRequestCodes = setOf(91)
	var received: String? = null
		private set
	var deliveries = 0
		private set

	fun openTarget() {
		launchForResult(Intent(this, ActivityResultTestTarget::class.java), 91)
	}

	override fun onExternalActivityResult(
		code: Int,
		result: Int,
		data: Intent?,
	) {
		if (code == 91 && result == RESULT_OK) {
			received = data?.getStringExtra("payload")
			deliveries++
		}
	}
}

class ActivityResultTestTarget : Activity() {
	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		setContentView(TextView(this).apply { text = "Activity result check" })
	}

	fun complete() {
		setResult(RESULT_OK, Intent().putExtra("payload", "returned after recreation"))
		finish()
	}
}
