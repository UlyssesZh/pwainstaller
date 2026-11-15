package io.github.ulysseszh.pwainstaller

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.ulysseszh.pwainstaller.core.CssColors
import java.util.Locale

class ColorButton(context: Context, private val label: Int) : MaterialButton(context) {
	var value: String = ""
		set(value) {
			field = value
			text = "${context.getString(label)}: ${value.ifEmpty { context.getString(R.string.default_value) }}"
			icon = ColorDrawable(CssColors.parse(value) ?: Color.WHITE)
			iconTint = null
		}
	init {
		setOnClickListener { ColorPicker.show(context, label, value) { value = it } }
	}
}

object ColorPicker {
	fun show(context: Context, title: Int, initial: String, chosen: (String) -> Unit): androidx.appcompat.app.AlertDialog {
		fun dp(n: Int) = (n * context.resources.displayMetrics.density).toInt()
		val hsv = FloatArray(3)
		var selected = CssColors.parse(initial) ?: Color.WHITE
		Color.colorToHSV(selected, hsv)
		var alpha = Color.alpha(selected)
		var syncing = false
		var validHex = true
		var dialog: androidx.appcompat.app.AlertDialog? = null
		val bars = mutableMapOf<Int, SeekBar>()
		val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), 0) }
		val preview = TextView(context).apply { gravity = android.view.Gravity.CENTER; setPadding(0, dp(16), 0, dp(16)); body.addView(this) }
		val hexContainer = com.google.android.material.textfield.TextInputLayout(context).apply {
			hint = context.getString(R.string.hex_color)
			helperText = context.getString(R.string.hex_color_hint)
			body.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
		}
		val hex = com.google.android.material.textfield.TextInputEditText(hexContainer.context).apply {
			setSingleLine(true)
			inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
			isSaveEnabled = false
			hexContainer.addView(this, LinearLayout.LayoutParams(-1, -2))
		}
		lateinit var plane: ColorPlane
		fun update(updateHex: Boolean = true) {
			syncing = true
			try {
				preview.text = css(selected)
				preview.setBackgroundColor(selected or 0xff000000.toInt())
				preview.setTextColor(CssColors.foreground(selected or 0xff000000.toInt()))
				val values = mapOf(R.string.hue to hsv[0].toInt(), R.string.saturation to (hsv[1] * 100).toInt(),
					R.string.brightness to (hsv[2] * 100).toInt(), R.string.opacity to alpha,
					R.string.red to Color.red(selected), R.string.green to Color.green(selected), R.string.blue to Color.blue(selected))
				values.forEach { (label, progress) -> bars[label]?.progress = progress }
				if (updateHex) {
					validHex = true; hexContainer.error = null
					val formatted = css(selected)
					if (hex.text.toString() != formatted) hex.setText(formatted)
				}
				dialog?.getButton(android.content.DialogInterface.BUTTON_POSITIVE)?.isEnabled = validHex
				plane.invalidate()
			} finally { syncing = false }
		}
		fun hsvChanged() { selected = Color.HSVToColor(alpha, hsv) }
		plane = ColorPlane(context, hsv) { hsvChanged(); update() }
		body.addView(plane, LinearLayout.LayoutParams(-1, dp(200)))
		fun slider(label: Int, maximum: Int, progress: Int, changed: (Int) -> Unit) {
			val caption = TextView(context).apply { body.addView(this) }
			fun describe(value: Int) { caption.text = context.getString(R.string.color_channel_value, context.getString(label), value) }
			val bar = SeekBar(context).apply {
				contentDescription = context.getString(label); max = maximum; this.progress = progress
				setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
					override fun onProgressChanged(bar: SeekBar, value: Int, user: Boolean) {
						describe(value)
						if (user && !syncing) { changed(value); update() }
					}
					override fun onStartTrackingTouch(bar: SeekBar) = Unit
					override fun onStopTrackingTouch(bar: SeekBar) = Unit
				})
			}
			describe(progress); bars[label] = bar; body.addView(bar)
		}
		slider(R.string.hue, 359, hsv[0].toInt()) { hsv[0] = it.toFloat(); hsvChanged() }
		// HSV sliders also make the color plane accessible without a touch gesture.
		slider(R.string.saturation, 100, (hsv[1] * 100).toInt()) { hsv[1] = it / 100f; hsvChanged() }
		slider(R.string.brightness, 100, (hsv[2] * 100).toInt()) { hsv[2] = it / 100f; hsvChanged() }
		listOf(R.string.red to 16, R.string.green to 8, R.string.blue to 0).forEach { (label, shift) ->
			slider(label, 255, (selected ushr shift) and 255) { value ->
				selected = (selected and (255 shl shift).inv()) or (value shl shift)
				Color.colorToHSV(selected, hsv)
			}
		}
		slider(R.string.opacity, 255, alpha) { alpha = it; selected = (selected and 0xffffff) or (alpha shl 24) }
		hex.addTextChangedListener(object : android.text.TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				if (syncing) return
				val value = s.toString().trim()
				val valid = value.matches(Regex("#?(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})"))
				val parsed = if (valid) CssColors.parse(if (value.startsWith('#')) value else "#$value") else null
				validHex = parsed != null
				hexContainer.error = if (validHex) null else context.getString(R.string.invalid_hex_color)
				if (parsed != null) {
					selected = parsed; alpha = Color.alpha(parsed); Color.colorToHSV(parsed, hsv)
					update(false)
				} else dialog?.getButton(android.content.DialogInterface.BUTTON_POSITIVE)?.isEnabled = false
			}
			override fun afterTextChanged(s: android.text.Editable?) = Unit
		})
		update()
		return MaterialAlertDialogBuilder(context).setTitle(title).setView(ScrollView(context).apply { addView(body) })
			.setNegativeButton(android.R.string.cancel, null)
			.setNeutralButton(R.string.default_value) { _, _ -> chosen("") }
			.setPositiveButton(android.R.string.ok) { _, _ -> chosen(css(selected)) }.show().also { dialog = it }
	}

	private fun css(color: Int): String {
		val rgb = String.format(Locale.ROOT, "#%06x", color and 0xffffff)
		return if (Color.alpha(color) == 255) rgb else rgb + String.format(Locale.ROOT, "%02x", Color.alpha(color))
	}

	private class ColorPlane(context: Context, private val hsv: FloatArray, private val changed: () -> Unit) : View(context) {
		private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
		init { contentDescription = context.getString(R.string.saturation_brightness); isClickable = true; setLayerType(LAYER_TYPE_SOFTWARE, null) }
		override fun onDraw(canvas: Canvas) {
			super.onDraw(canvas)
			if (width == 0 || height == 0) return
			val hue = Color.HSVToColor(floatArrayOf(hsv[0], 1f, 1f))
			paint.shader = ComposeShader(
				LinearGradient(0f, 0f, width.toFloat(), 0f, Color.WHITE, hue, Shader.TileMode.CLAMP),
				LinearGradient(0f, 0f, 0f, height.toFloat(), Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP), PorterDuff.Mode.MULTIPLY)
			paint.style = Paint.Style.FILL
			canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
			paint.shader = null; paint.style = Paint.Style.STROKE; paint.strokeWidth = 3 * resources.displayMetrics.density
			paint.color = if (hsv[2] > .5) Color.BLACK else Color.WHITE
			canvas.drawCircle(hsv[1] * width, (1 - hsv[2]) * height, 7 * resources.displayMetrics.density, paint)
		}
		override fun onTouchEvent(event: MotionEvent): Boolean {
			if (event.action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP)) {
				hsv[1] = (event.x / width).coerceIn(0f, 1f); hsv[2] = 1 - (event.y / height).coerceIn(0f, 1f)
				parent.requestDisallowInterceptTouchEvent(event.action != MotionEvent.ACTION_UP)
				changed(); invalidate()
				if (event.action == MotionEvent.ACTION_UP) performClick()
				return true
			}
			return super.onTouchEvent(event)
		}
		override fun performClick(): Boolean { super.performClick(); return true }
	}
}
