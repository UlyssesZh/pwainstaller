package io.github.ulysseszh.pwainstaller

import io.github.ulysseszh.pwainstaller.browser.BrowserActivity
import android.content.Intent
import android.os.Bundle
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.ulysseszh.pwainstaller.browser.R as BrowserR

class LoginActivity : BrowserActivity() {
    private var ready = false
    private lateinit var loading: LinearProgressIndicator
    override fun canStartBrowser() = ready
    override fun onCreate(state: Bundle?) {
        ready = runCatching {
            LoginSessions.requireSession(this, intent.getStringExtra("session").orEmpty())
            LoginProfile.acquire(this)
            LoginSessions.requireSession(this, intent.getStringExtra("session").orEmpty())
        }.isSuccess
        super.onCreate(state)
        if (ready) {
            active = this
            val light = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = light
                isAppearanceLightNavigationBars = light
            }
            ViewCompat.requestApplyInsets(root)
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun createBrowserBar(): View {
        val surface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, Color.WHITE)
        val onSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
        root.setBackgroundColor(surface)
        val bar = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(surface) }
        val toolbar = MaterialToolbar(this).apply {
            setBackgroundColor(surface)
            setTitle(R.string.temporary_browser)
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            setNavigationIconTint(onSurface)
            setNavigationContentDescription(BrowserR.string.back)
            setNavigationOnClickListener { onBackPressed() }
            menu.add(0, 1, 0, BrowserR.string.refresh).apply { setIcon(R.drawable.ic_refresh); setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS) }
            menu.add(0, 2, 1, BrowserR.string.done).apply { setIcon(R.drawable.ic_check); setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS) }
            menu.add(0, 3, 2, BrowserR.string.open_external)
            for (index in 0 until menu.size()) menu.getItem(index).icon?.let { drawable ->
                androidx.core.graphics.drawable.DrawableCompat.setTint(drawable.mutate(), onSurface)
            }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> current().reload()
                    2 -> completed()
                    3 -> current().url?.let { external(android.net.Uri.parse(it)) }
                }
                true
            }
        }
        bar.addView(toolbar, LinearLayout.LayoutParams(-1, dp(64)))
        val addressRow = LinearLayout(this).apply { setPadding(dp(16), dp(4), dp(16), dp(12)) }
        val field = TextInputLayout(this).apply {
            setHint(BrowserR.string.address)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            setEndIconDrawable(R.drawable.ic_arrow_forward)
            setEndIconContentDescription(BrowserR.string.go)
        }
        val input = TextInputEditText(field.context).apply {
            setSingleLine(); textSize = 16f
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setText(initialUrl()); setSelectAllOnFocus(true)
        }
        fun go() {
            navigate(input.text.toString())
            input.clearFocus()
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(input.windowToken, 0)
        }
        input.setOnEditorActionListener { _, action, event ->
            if (action == EditorInfo.IME_ACTION_GO || event?.let { it.keyCode == android.view.KeyEvent.KEYCODE_ENTER && it.action == android.view.KeyEvent.ACTION_UP } == true) {
                go(); true
            } else false
        }
        field.setEndIconOnClickListener { go() }
        field.addView(input, LinearLayout.LayoutParams(-1, -2)); address = input
        addressRow.addView(field, LinearLayout.LayoutParams(-1, -2)); bar.addView(addressRow)
        loading = LinearProgressIndicator(this).apply { max = 100; visibility = View.INVISIBLE; contentDescription = getString(R.string.working) }
        bar.addView(loading, LinearLayout.LayoutParams(-1, dp(3)))
        bar.isFocusableInTouchMode = true
        bar.requestFocus()
        return bar
    }
    override fun onBrowserProgress(view: WebView, progress: Int) {
        if (view !== current()) return
        loading.setProgressCompat(progress, false)
        loading.visibility = if (progress in 1..99) View.VISIBLE else View.INVISIBLE
        if (address?.hasFocus() != true) view.url?.let { address?.setText(it) }
    }
    override fun showBrowserDialog(title: String, positive: String, negative: String, onPositive: () -> Unit,
        onNegative: () -> Unit, message: String?, view: View?) {
        MaterialAlertDialogBuilder(this).setTitle(title).setMessage(message).setView(view)
            .setPositiveButton(positive) { _, _ -> onPositive() }.setNegativeButton(negative) { _, _ -> onNegative() }
            .setOnCancelListener { onNegative() }.show()
    }
    override fun createContextMenu(title: String, labels: Array<String>, selected: (Int) -> Unit): android.app.Dialog =
        MaterialAlertDialogBuilder(this).setTitle(title).setItems(labels) { _, index -> selected(index) }
            .setNegativeButton(android.R.string.cancel, null).create()
    override fun createAuthenticationForm(savedUsername: String): AuthenticationForm {
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), dp(8)) }
        fun input(label: Int, password: Boolean): TextInputEditText {
            val field = TextInputLayout(this).apply {
                setHint(label); boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                if (password) endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            }
            val edit = TextInputEditText(field.context).apply {
                setSingleLine(); inputType = if (password) 129 else android.text.InputType.TYPE_CLASS_TEXT
                if (!password) setText(savedUsername)
            }
            field.addView(edit, LinearLayout.LayoutParams(-1, -2))
            form.addView(field, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
            return edit
        }
        return AuthenticationForm(form, input(BrowserR.string.username, false), input(BrowserR.string.password, true))
    }
    override fun loadInitialPage(view: WebView, url: String) {
        // Chromium versions may keep their HTTP cache outside app_webview.
        // Clear it before the first navigation as well as replacing the on-disk profile.
        if (intent.getBooleanExtra("fresh", true)) view.clearCache(true)
        view.clearHistory()
        super.loadInitialPage(view, url)
    }
    override fun loginMode() = true
    override fun requestHeaders() = ManifestFetcher.headers(intent.getStringExtra("headers").orEmpty())
    override fun completed() = finishSignIn()
    fun finishSignIn() {
        CookieManager.getInstance().flush()
        val token = intent.getStringExtra("session") ?: return
        LoginSessions.completed(this, token, current().url)
        setResult(RESULT_OK, Intent().putExtra("session", token))
        finish()
    }
    override fun onDestroy() { if (active === this) active = null; super.onDestroy() }
    companion object { var active: LoginActivity? = null }
}
