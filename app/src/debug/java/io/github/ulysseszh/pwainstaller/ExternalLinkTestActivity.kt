package io.github.ulysseszh.pwainstaller

/** Debug-only native URL handler; its reserved .invalid domain has no production use. */
class ExternalLinkTestActivity : android.app.Activity() {
	override fun onCreate(state: android.os.Bundle?) {
		super.onCreate(state)
		setContentView(android.widget.TextView(this).apply { text = intent.dataString })
	}
}
