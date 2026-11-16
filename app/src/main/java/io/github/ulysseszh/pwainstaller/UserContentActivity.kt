package io.github.ulysseszh.pwainstaller

import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.LinearLayout
import android.widget.PopupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.UUID

class UserContentActivity : Screen() {
	override val resultRequestCodes = setOf(81)
	private lateinit var form: LinearLayout
	private var importing = UserContentFiles.Kind.SCRIPT

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		title(R.string.user_content, true)
		importing = state?.getString("importing")?.let(UserContentFiles.Kind::valueOf) ?: UserContentFiles.Kind.SCRIPT
		form = scrollBody()
		work({ UserContentLibrary.load(this) }, ::showFiles)
	}

	private fun showFiles(files: List<UserContentFiles.Entry>) {
		form.removeAllViews()
		text(form, getString(R.string.user_content_library_hint), 14)
		UserContentFiles.Kind.entries.forEach { kind ->
			text(
				form,
				getString(
					if (kind == UserContentFiles.Kind.SCRIPT) {
						R.string.user_scripts
					} else {
						R.string.user_styles
					},
				),
				20,
			)
			val entries = files.filter { it.kind == kind }
			if (entries.isEmpty()) {
				text(form, getString(R.string.user_files_empty), 14)
			}
			entries.forEach { entry ->
				val row =
					LinearLayout(this).apply {
						orientation = LinearLayout.HORIZONTAL
						gravity = android.view.Gravity.CENTER_VERTICAL
					}
				form.addView(row)
				val label =
					text(
						row,
						entry.name +
							if (entry.url.isNotEmpty()) {
								"\n${entry.url}"
							} else {
								""
							},
						15,
					)
				label.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
				val menu =
					button(row, R.string.more) { view ->
						if (!busy) {
							PopupMenu(this, view).apply {
								menu.add(R.string.rename_user_file).setOnMenuItemClickListener {
									rename(entry)
									true
								}
								menu.add(R.string.refresh_user_file).apply { isEnabled = entry.url.isNotEmpty() }.setOnMenuItemClickListener {
									fetch(entry.kind, entry.url, entry.name, entry)
									true
								}
								menu.add(R.string.delete).setOnMenuItemClickListener {
									MaterialAlertDialogBuilder(this@UserContentActivity)
										.setTitle(R.string.delete)
										.setMessage(getString(R.string.delete_user_file, entry.name))
										.setNegativeButton(android.R.string.cancel, null)
										.setPositiveButton(R.string.delete) { _, _ ->
											work({
												UserContentLibrary.remove(this@UserContentActivity, entry.id)
												UserContentLibrary.load(this@UserContentActivity)
											}, ::showFiles)
										}.show()
									true
								}
								show()
							}
						}
					}
				menu.text = "⋮"
				menu.contentDescription = getString(R.string.user_file_actions, entry.name)
				menu.layoutParams = LinearLayout.LayoutParams(dp(56), dp(48))
			}
			button(
				form,
				if (kind == UserContentFiles.Kind.SCRIPT) {
					R.string.add_user_script
				} else {
					R.string.add_user_style
				},
			) {
				if (!busy) {
					MaterialAlertDialogBuilder(
						this,
					).setItems(arrayOf(getString(R.string.import_user_file), getString(R.string.fetch_user_file))) { _, index ->
						if (index == 0) {
							importing = kind
							launchForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE), 81)
						} else {
							askUrl(kind)
						}
					}.show()
				}
			}
		}
	}

	private fun rename(entry: UserContentFiles.Entry) {
		val content =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(dp(24), dp(12), dp(24), 0)
			}
		val name = field(content, R.string.user_file_name, entry.name, false)
		val dialog =
			MaterialAlertDialogBuilder(this)
				.setTitle(R.string.rename_user_file)
				.setView(content)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.save, null)
				.create()
		dialog.setOnShowListener {
			dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
				try {
					val updated = entry.copy(name = name.text.toString().trim())
					updated.validate()
					dialog.dismiss()
					work({
						UserContentLibrary.save(this, updated)
						UserContentLibrary.load(this)
					}, ::showFiles)
				} catch (failure: Exception) {
					name.error = failure.message
				}
			}
		}
		dialog.show()
	}

	private fun askUrl(kind: UserContentFiles.Kind) {
		val content =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(dp(24), dp(12), dp(24), 0)
			}
		val url = field(content, R.string.user_file_url, "", false).apply { inputType = 17 }
		val name = field(content, R.string.user_file_name_optional, "", false)
		val dialog =
			MaterialAlertDialogBuilder(this)
				.setTitle(R.string.fetch_user_file)
				.setView(content)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.fetch_user_file, null)
				.create()
		dialog.setOnShowListener {
			dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
				try {
					val location =
						io.github.ulysseszh.pwainstaller.core.PackageNames
							.webUrl(url.text.toString().trim())
					dialog.dismiss()
					fetch(kind, location, name.text.toString().trim())
				} catch (failure: Exception) {
					url.error = failure.message
				}
			}
		}
		dialog.show()
	}

	private fun fetch(
		kind: UserContentFiles.Kind,
		url: String,
		name: String,
		previous: UserContentFiles.Entry? = null,
	) {
		fetchWork({ progress ->
			val response =
				ManifestFetcher.get(
					url,
					emptyMap(),
					UserContentFiles.MAX_FILE_BYTES,
					url,
					onProgress = { received, total -> progress(FetchProgress(FetchProgress.Stage.FILE, received, total)) },
				)
			val filename =
				name.ifBlank {
					android.net.Uri
						.parse(response.url)
						.lastPathSegment
						?.takeIf { it.isNotBlank() }
						?: if (kind == UserContentFiles.Kind.SCRIPT) {
							"script.js"
						} else {
							"style.css"
						}
				}
			val entry =
				UserContentFiles.Entry(
					previous?.id ?: UUID.randomUUID().toString(),
					kind,
					filename.take(256),
					UserContentFiles.decode(response.bytes),
					url,
				)
			UserContentLibrary.save(this, entry)
			UserContentLibrary.load(this)
		}, {
			showFiles(it)
			toast(R.string.user_file_saved)
		})
	}

	override fun onExternalActivityResult(
		code: Int,
		result: Int,
		data: Intent?,
	) {
		if (code != 81 || result != RESULT_OK) {
			return
		}
		val uri = data?.data ?: return
		val kind = importing
		work({
			val name =
				contentResolver
					.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
					?.use { cursor ->
						if (cursor.moveToFirst()) {
							cursor.getString(0)
						} else {
							null
						}
					}?.takeIf { it.isNotBlank() } ?: if (kind == UserContentFiles.Kind.SCRIPT) {
					"script.js"
				} else {
					"style.css"
				}
			val bytes = requireNotNull(contentResolver.openInputStream(uri)).use { ManifestFetcher.read(it, UserContentFiles.MAX_FILE_BYTES) }
			UserContentLibrary.save(this, UserContentFiles.Entry(UUID.randomUUID().toString(), kind, name.take(256), UserContentFiles.decode(bytes)))
			UserContentLibrary.load(this)
		}, {
			showFiles(it)
			toast(R.string.user_file_saved)
		})
	}

	override fun onSaveInstanceState(out: Bundle) {
		out.putString("importing", importing.name)
		super.onSaveInstanceState(out)
	}
}
