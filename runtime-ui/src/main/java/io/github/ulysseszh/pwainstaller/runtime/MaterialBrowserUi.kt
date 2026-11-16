package io.github.ulysseszh.pwainstaller.runtime

import android.app.Activity
import android.view.*
import android.view.inputmethod.*
import android.widget.ImageButton
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.*
import io.github.ulysseszh.pwainstaller.browser.*
import io.github.ulysseszh.pwainstaller.browser.R as BrowserR

class MaterialBrowserUi : BrowserChromeFactory {
	override fun create(
		activity: Activity,
		actions: BrowserActions,
	): BrowserChrome {
		val context =
			DynamicColors.wrapContextIfAvailable(
				ContextThemeWrapper(activity, com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar),
			)

		fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
		val toolbar =
			MaterialToolbar(context).apply {
				layoutParams = android.widget.LinearLayout.LayoutParams(-1, dp(64))
				setNavigationIcon(R.drawable.browser_back)
				setNavigationContentDescription(BrowserR.string.back)
				setNavigationOnClickListener { actions.back() }
			}

		fun history() {
			val stack = actions.webView().copyBackForwardList()
			val indices = (stack.currentIndex - 1 downTo 0).toList()
			val dialog = MaterialAlertDialogBuilder(context).setTitle(BrowserR.string.browser_history)
			if (indices.isEmpty()) {
				dialog.setMessage(BrowserR.string.browser_history_empty).setPositiveButton(android.R.string.ok, null)
			} else {
				dialog
					.setItems(
						indices
							.map { index ->
								stack.getItemAtIndex(index).let { "${it.title.orEmpty()}\n${it.url}" }
							}.toTypedArray(),
					) { _, position ->
						actions.webView().goBackOrForward(indices[position] - stack.currentIndex)
					}.setNegativeButton(android.R.string.cancel, null)
			}
			dialog.show()
		}
		repeat(toolbar.childCount) { index ->
			(toolbar.getChildAt(index) as? ImageButton)
				?.takeIf { it.contentDescription == context.getString(BrowserR.string.back) }
				?.setOnLongClickListener {
					history()
					true
				}
		}
		val field =
			TextInputLayout(context).apply {
				setHint(BrowserR.string.address)
				boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
				endIconMode = TextInputLayout.END_ICON_CUSTOM
				setEndIconDrawable(R.drawable.browser_forward)
				setEndIconContentDescription(BrowserR.string.go)
			}
		val input =
			TextInputEditText(field.context).apply {
				setSingleLine()
				textSize = 15f
				inputType = 17
				imeOptions = EditorInfo.IME_ACTION_GO
				hint = context.getString(BrowserR.string.address)
				setSelectAllOnFocus(true)
				setPadding(dp(10), dp(8), dp(10), dp(8))
			}

		fun go() {
			actions.navigate(input.text.toString())
			input.clearFocus()
			toolbar.requestFocus()
			(activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(input.windowToken, 0)
		}
		field.setEndIconOnClickListener { go() }
		input.setOnEditorActionListener { _, action, event ->
			if (action == EditorInfo.IME_ACTION_GO ||
				event?.let { it.keyCode == KeyEvent.KEYCODE_ENTER && it.action == KeyEvent.ACTION_UP } == true
			) {
				go()
				true
			} else {
				false
			}
		}
		field.addView(input, android.widget.LinearLayout.LayoutParams(-1, -2))
		field.isHintEnabled = false
		toolbar.addView(
			field,
			androidx.appcompat.widget.Toolbar
				.LayoutParams(-1, dp(48))
				.apply { gravity = Gravity.CENTER_VERTICAL },
		)
		listOf(
			6 to BrowserR.string.refresh,
			1 to BrowserR.string.browser_forward,
			2 to BrowserR.string.context_share_link,
			3 to BrowserR.string.context_copy_link,
			4 to BrowserR.string.browser_download_page,
			5 to BrowserR.string.browser_print_page,
		).forEachIndexed { index, (id, title) -> toolbar.menu.add(0, id, index, title) }
		toolbar.setOnMenuItemClickListener { item ->
			when (item.itemId) {
				1 -> {
					if (actions.webView().canGoForward()) {
						actions.webView().goForward()
					}
				}

				2 -> {
					actions.share()
				}

				3 -> {
					actions.copy()
				}

				4 -> {
					actions.download()
				}

				5 -> {
					actions.print()
				}

				6 -> {
					actions.webView().reload()
				}
			}
			true
		}
		toolbar.isFocusableInTouchMode = true
		toolbar.requestFocus()
		return object : BrowserChrome {
			override val view: View = toolbar

			override fun colors(
				background: Int,
				foreground: Int,
			) {
				toolbar.setBackgroundColor(background)
				toolbar.setNavigationIconTint(foreground)
				toolbar.overflowIcon = toolbar.overflowIcon?.mutate()?.apply { setTint(foreground) }
				input.setTextColor(foreground)
				input.setHintTextColor(foreground)
				field.boxBackgroundColor = background
				field.setBoxStrokeColorStateList(
					android.content.res.ColorStateList
						.valueOf(foreground),
				)
				field.setEndIconTintList(
					android.content.res.ColorStateList
						.valueOf(foreground),
				)
				if (android.os.Build.VERSION.SDK_INT >= 29) {
					input.textCursorDrawable = input.textCursorDrawable?.mutate()?.apply { setTint(foreground) }
				}
			}

			override fun update(url: String) {
				if (!input.hasFocus()) {
					input.setText(url)
				}
				toolbar.menu.findItem(1).isEnabled = actions.webView().canGoForward()
			}
		}
	}
}
