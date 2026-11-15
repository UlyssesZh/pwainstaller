package io.github.ulysseszh.pwainstaller

import android.widget.LinearLayout
import com.google.android.material.checkbox.MaterialCheckBox

class UserContentSelection(screen: Screen, parent: LinearLayout,
	library: List<UserContentFiles.Entry>, enabled: List<UserContentFiles.Entry>) {
	private val choices = mutableListOf<Pair<UserContentFiles.Entry, MaterialCheckBox>>()
	init {
		screen.text(parent, screen.getString(R.string.user_content_hint), 14)
		// Retain enabled files that were deleted from the library or arrived in an import.
		val available = library + enabled.filter { saved -> library.none { it.id == saved.id } }
		UserContentFiles.Kind.entries.forEach { kind ->
			screen.text(parent, screen.getString(if (kind == UserContentFiles.Kind.SCRIPT) R.string.user_scripts else R.string.user_styles), 20)
			val entries = available.filter { it.kind == kind }
			if (entries.isEmpty()) screen.text(parent, screen.getString(R.string.user_files_empty), 14)
			entries.forEach { entry ->
				val check = MaterialCheckBox(screen).apply {
					text = entry.name; isChecked = enabled.any { it.id == entry.id }
					parent.addView(this)
				}
				choices.add(entry to check)
			}
		}
	}
	fun selected() = choices.filter { it.second.isChecked }.map { it.first }
}
