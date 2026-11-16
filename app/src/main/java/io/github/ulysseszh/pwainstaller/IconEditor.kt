package io.github.ulysseszh.pwainstaller

import android.graphics.BitmapFactory
import android.view.Gravity
import android.widget.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import org.json.JSONArray
import org.json.JSONObject

class IconEditor(
	private val screen: Screen,
	private var app: WebApp,
	private val purposeChanged: (Int, String) -> Unit = { _, _ -> },
	private val upload: () -> Unit,
) : LinearLayout(screen) {
	private var generation = 0

	init {
		orientation = VERTICAL
		render()
	}

	fun bind(value: WebApp) {
		app = value
		render()
	}

	fun add(icons: List<JSONObject>) {
		icons.forEach { app.icons().put(it) }
		app.set("iconsManual", true)
		app.updateIcon()
		render()
	}

	private fun render() {
		val currentGeneration = ++generation
		removeAllViews()
		screen.text(this, screen.getString(R.string.icons), 18)
		screen.text(this, screen.getString(R.string.icons_hint), 14)
		val icons = app.icons()
		repeat(icons.length()) { index ->
			val icon = icons.getJSONObject(index)
			val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
			val data = icon.getString("data")
			val preview =
				ImageView(context).apply {
					setImageResource(R.drawable.ic_app)
					contentDescription = screen.getString(R.string.icon_number, index + 1)
				}
			row.addView(preview, LayoutParams(screen.dp(48), screen.dp(48)))
			InstallerApplication.readers.execute {
				val bitmap =
					runCatching {
						val bytes = IconData.decode(data)
						val dimensions = IconData.dimensions(data)
						val options = BitmapFactory.Options().apply { inSampleSize = maxOf(1, maxOf(dimensions.first, dimensions.second) / 128) }
						BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
					}.getOrNull()
				screen.runOnUiThread {
					if (currentGeneration == generation && !screen.isFinishing && !screen.isDestroyed) {
						preview.setImageBitmap(bitmap)
					} else {
						bitmap?.recycle()
					}
				}
			}
			row.addView(
				TextView(context).apply {
					val size =
						if (IconData.scalable(icon)) {
							screen.getString(R.string.scalable_icon)
						} else {
							IconData.sizes(icon.getString("data"))
						}
					text = "${screen.getString(R.string.icon_number, index + 1)} · $size"
					setPadding(screen.dp(8), 0, 0, 0)
				},
				LayoutParams(0, -2, 1f),
			)
			row.addView(
				MaterialButton(context).apply {
					setText(R.string.remove_icon)
					setOnClickListener {
						val remaining = JSONArray()
						repeat(icons.length()) {
							if (it != index) {
								remaining.put(icons.getJSONObject(it))
							}
						}
						app.set("icons", remaining)
						app.set("iconsManual", true)
						app.updateIcon()
						render()
					}
				},
			)
			addView(row)
			// Purposes are a set: a single image may serve several roles.
			val purposes = LinearLayout(context).apply { orientation = VERTICAL }
			listOf(
				"any" to R.string.icon_any,
				"maskable" to R.string.icon_maskable,
				"monochrome" to R.string.icon_monochrome,
			).forEach { (purpose, label) ->
				purposes.addView(
					MaterialCheckBox(context).apply {
						setText(label)
						contentDescription = "${screen.getString(R.string.icon_number, index + 1)}: ${screen.getString(label)}"
						isChecked = purpose in IconData.purposes(icon.optString("purpose", "any"))
						setOnCheckedChangeListener { _, checked ->
							val selected = IconData.purposes(icon.optString("purpose", "any")).toMutableSet()
							if (checked) {
								selected.add(purpose)
							} else {
								selected.remove(purpose)
							}
							if (selected.isEmpty()) {
								isChecked = true
								return@setOnCheckedChangeListener
							}
							val value = selected.joinToString(" ")
							icon.put("purpose", value)
							app.set("iconsManual", true)
							app.updateIcon()
							purposeChanged(index, value)
						}
					},
				)
			}
			addView(purposes)
		}
		screen.button(this, R.string.upload_icons) { upload() }
	}
}
