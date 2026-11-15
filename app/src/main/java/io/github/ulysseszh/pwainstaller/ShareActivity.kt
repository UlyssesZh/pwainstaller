package io.github.ulysseszh.pwainstaller

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/** External inputs can only prefill a new record; they cannot modify saved apps. */
class ShareActivity : Activity() {
	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		val url = when (intent.action) {
			Intent.ACTION_VIEW -> SharedWebUrl.extract(intent.dataString)
			Intent.ACTION_SEND -> SharedWebUrl.extract(intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString())
				?: SharedWebUrl.extract(intent.dataString)
				?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.let {
					SharedWebUrl.extract(it.text?.toString()) ?: SharedWebUrl.extract(it.uri?.toString())
				}
			else -> null
		}
		if (url != null) startActivity(Intent(this, EditorActivity::class.java).putExtra("baseUrl", url))
		else Toast.makeText(this, R.string.invalid_shared_url, Toast.LENGTH_LONG).show()
		finish()
	}
}
