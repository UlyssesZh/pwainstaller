package io.github.ulysseszh.pwainstaller

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

abstract class Screen : AppCompatActivity() {
	lateinit var root: LinearLayout
	lateinit var body: LinearLayout
	lateinit var toolbar: MaterialToolbar
	protected open val showWorkProgress get() = true
	private var progress: ProgressBar? = null
	private var fetchDialog: FetchDialog? = null
	var busy = false
		private set

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
		setContentView(root)
		val light = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK != android.content.res.Configuration.UI_MODE_NIGHT_YES
		WindowCompat.getInsetsController(window, window.decorView).apply {
			isAppearanceLightStatusBars = light
			isAppearanceLightNavigationBars = light
		}
		ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
			val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
			view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
			insets
		}
		toolbar = MaterialToolbar(this)
		root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(64)))
		if (showWorkProgress) {
			progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
				isIndeterminate = true; visibility = View.GONE; contentDescription = getString(R.string.working)
				root.addView(this, LinearLayout.LayoutParams(-1, dp(3)))
			}
		}
		body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
		root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
	}
	fun title(title: Int, back: Boolean) {
		toolbar.setTitle(title)
		if (back) {
			toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
			toolbar.setNavigationContentDescription(io.github.ulysseszh.pwainstaller.browser.R.string.back)
			toolbar.setNavigationOnClickListener { navigateBack() }
		}
	}
	protected open fun navigateBack() { finish() }
	fun scrollBody(): LinearLayout {
		val scroll = ScrollView(this).apply { isFillViewport = true }
		body.addView(scroll, LinearLayout.LayoutParams(-1, -1))
		val content = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(20), dp(12), dp(20), dp(24))
			scroll.addView(this)
		}
		return content
	}
	fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
	fun text(parent: LinearLayout, value: String, size: Int): TextView = TextView(this).apply {
		text = value; textSize = size.toFloat(); setPadding(0, dp(8), 0, dp(8)); parent.addView(this)
	}
	fun field(parent: LinearLayout, label: Int, value: String, multiline: Boolean): TextInputEditText {
		val container = TextInputLayout(this).apply { hint = getString(label) }
		val input = TextInputEditText(container.context).apply {
			setText(value); textSize = 15f
			inputType = InputType.TYPE_CLASS_TEXT or if (multiline) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0
			setSingleLine(!multiline); isSaveEnabled = false
		}
		container.addView(input, LinearLayout.LayoutParams(-1, -2))
		parent.addView(container, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
		return input
	}
	fun button(parent: LinearLayout, label: Int, listener: (View) -> Unit): MaterialButton = MaterialButton(this).apply {
		setText(label); setOnClickListener(listener); parent.addView(this, LinearLayout.LayoutParams(-1, -2))
	}
	// Framework toast layouts must not pass through AppCompat's activity inflater on API 21.
	fun toast(message: Int) { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
	fun error(error: Throwable) { showError(error, null) }
	fun buildError(error: Throwable, key: String) { showError(error, key) }
	private fun showError(error: Throwable, key: String?) {
		if (isFinishing || isDestroyed) return
		val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.error_title)
			.setMessage(getString(R.string.error_detail, getString(R.string.action_failed), error.message ?: error.toString()))
			.setPositiveButton(android.R.string.ok, null)
		if (key != null) dialog.setNeutralButton(R.string.view_build_logs) { _, _ -> BuildLogActivity.open(this, key) }
		if (key != null && SaveJobs.recoveryFile(this, key)?.isFile == true)
			dialog.setNegativeButton(R.string.edit_app) { _, _ ->
				startActivity(android.content.Intent(this, EditorActivity::class.java).putExtra("saveDraft", key))
			}
		dialog.show()
	}
	protected fun setBusy(value: Boolean) { busy = value; progress?.visibility = if (value) View.VISIBLE else View.GONE }
	protected open val workExecutor: java.util.concurrent.Executor get() = InstallerApplication.worker
	protected fun <T> fetchWork(task: ((FetchProgress) -> Unit) -> T, result: (T) -> Unit, failure: (Throwable) -> Unit = ::error) {
		if (busy) return
		fetchDialog = FetchDialog(this).also { it.show(FetchProgress()) }
		fun dismiss() { fetchDialog?.dismiss(); fetchDialog = null }
		work({ task { update -> runOnUiThread {
			if (!isFinishing && !isDestroyed) fetchDialog?.show(update)
		} } }, { value -> dismiss(); result(value) }, { error -> dismiss(); failure(error) })
	}
	override fun onDestroy() { fetchDialog?.dismiss(); fetchDialog = null; super.onDestroy() }
	fun <T> work(task: () -> T, result: (T) -> Unit, failure: (Throwable) -> Unit = ::error) {
		if (busy) return
		setBusy(true)
		workExecutor.execute {
			try {
				val value = task()
				runOnUiThread {
					setBusy(false)
					if (!isFinishing && !isDestroyed) try { result(value) } catch (error: Exception) { error(error) }
				}
			} catch (error: Exception) {
				runOnUiThread {
					setBusy(false)
					if (!isFinishing && !isDestroyed) try { failure(error) } catch (next: Exception) { error(next) }
				}
			}
		}
	}
}
