package io.github.ulysseszh.pwainstaller

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Stable launcher-shortcut entry point; the editor itself remains private. */
class AddAppActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        startActivity(Intent(this, EditorActivity::class.java))
        finish()
    }
}
