package io.github.ulysseszh.pwainstaller

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Typeface

class BuildLogActivity : Screen() {
    private val handler = Handler(Looper.getMainLooper())
    private val poll = Runnable { load() }
    private var content = ""
    private var reading = false
    private var resumed = false
    private lateinit var log: TextView
    private lateinit var scroll: ScrollView
    private lateinit var copy: MenuItem
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); title(R.string.view_build_logs, true)
        copy = toolbar.menu.add(R.string.copy_build_log).setIcon(R.drawable.ic_copy).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS); isEnabled = false
            setOnMenuItemClickListener { copyLog(); true }
        }
        log = TextView(this).apply {
            typeface = Typeface.MONOSPACE; textSize = 13f
            setPadding(dp(16), dp(12), dp(16), dp(24)); setTextIsSelectable(true)
        }
        scroll = ScrollView(this).apply { addView(log, android.view.ViewGroup.LayoutParams(-1, -2)) }
        body.addView(scroll, android.view.ViewGroup.LayoutParams(-1, -1))
    }
    override fun onResume() { super.onResume(); resumed = true; load() }
    private fun load() {
        if (reading || !resumed) return
        reading = true
        val key = intent.getStringExtra("key").orEmpty()
        val job = BuildJobs.forApp(key)
        InstallerApplication.readers.execute {
            val updated = if (job?.queued == true) null else runCatching { BuildLogs.file(this, key).takeIf { it.isFile }?.readText() }.getOrNull()
            runOnUiThread {
                reading = false
                if (resumed && !isFinishing && !isDestroyed) {
                    val follow = if (content.isEmpty()) job != null else !scroll.canScrollVertically(1)
                    val shown = updated ?: getString(if (job?.queued == true) R.string.build_queued else R.string.no_build_logs)
                    if (shown != log.text.toString()) {
                        if (content.isNotEmpty() && updated != null && updated.startsWith(content)) log.append(updated.substring(content.length))
                        else log.setText(shown, TextView.BufferType.EDITABLE)
                        if (follow) scroll.post { scroll.fullScroll(android.view.View.FOCUS_DOWN) }
                    }
                    content = updated.orEmpty(); copy.isEnabled = content.isNotEmpty()
                    handler.removeCallbacks(poll)
                    // An active read can race the last write. Read once more after completion.
                    if (job != null || BuildJobs.forApp(key) != null) handler.postDelayed(poll, 500)
                }
            }
        }
    }
    private fun copyLog() {
        val key = intent.getStringExtra("key").orEmpty()
        InstallerApplication.readers.execute {
            val result = runCatching { BuildLogs.file(this, key).readText() }
            runOnUiThread {
                if (!isFinishing && !isDestroyed) result.onSuccess { text ->
                    (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(getString(R.string.view_build_logs), text))
                    toast(R.string.build_log_copied)
                }.onFailure(::error)
            }
        }
    }
    override fun onPause() { resumed = false; handler.removeCallbacks(poll); super.onPause() }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }
    companion object {
        fun open(context: android.content.Context, key: String) { context.startActivity(android.content.Intent(context, BuildLogActivity::class.java).putExtra("key", key)) }
    }
}
