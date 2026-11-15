package io.github.ulysseszh.pwainstaller

import android.content.Context
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout

class SelectionInput(context: Context, label: Int, private val options: List<Pair<String, String>>) : TextInputLayout(context) {
	private val input = MaterialAutoCompleteTextView(context).apply {
		inputType = InputType.TYPE_NULL
		keyListener = null
		isSaveEnabled = false
	}
	private var entries = options
	var onValueChanged: ((String) -> Unit)? = null
	var value: String
		get() = entries.find { it.second == input.text.toString() }?.first.orEmpty()
		set(value) {
			// Preserve a future manifest value until the user explicitly selects another option.
			entries = if (options.any { it.first == value }) options else options + (value to value)
			input.setAdapter(ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, entries.map { it.second }))
			input.setText(entries.first { it.first == value }.second, false)
			onValueChanged?.invoke(value)
		}
	init {
		hint = context.getString(label)
		endIconMode = END_ICON_DROPDOWN_MENU
		addView(input, LinearLayout.LayoutParams(-1, -2))
		input.setOnClickListener { input.showDropDown() }
		input.setOnItemClickListener { _, _, _, _ -> onValueChanged?.invoke(value) }
		value = ""
	}
}
