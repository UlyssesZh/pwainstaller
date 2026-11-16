package io.github.ulysseszh.pwainstaller

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject

class EditorActivity : Screen() {
	override val resultRequestCodes = setOf(41, 42)
	override val workExecutor get() = InstallerApplication.editor
	private lateinit var app: WebApp
	private var original: WebApp? = null
	private var stateToken =
		java.util.UUID
			.randomUUID()
			.toString()
	private var editing = false
	private lateinit var saveAction: android.view.MenuItem
	private lateinit var initialFormState: Map<String, Any>
	private var discardDialog: androidx.appcompat.app.AlertDialog? = null
	private var loadingError: String? = null
	private var loadingErrorDialog: androidx.appcompat.app.AlertDialog? = null
	private var retrySave = false
	private var loginSession: String? = null
	private var pendingLoginProfile: java.io.File? = null
	private var iconSources = ""
	private var rulesCustomized = false
	private var settingRules = false
	private var rulesText = ""
	private var rulesExpanded = false

	@Volatile private var rulesGeneration = 0
	private val ruleHandler = android.os.Handler(android.os.Looper.getMainLooper())
	private var ruleSuggestion: Runnable? = null
	private var rulesRenderGeneration = 0
	private var rulesPages = mutableListOf("")
	private var rulesPage = 0
	private var rulesDirty = false
	private var refreshingRules = false
	private lateinit var rulesPager: LinearLayout
	private lateinit var rulesPageLabel: android.widget.TextView
	private lateinit var rulesPrevious: android.widget.Button
	private lateinit var rulesNext: android.widget.Button
	private lateinit var rulesLoading: android.widget.ProgressBar
	private lateinit var adblockInput: TextInputEditText
	private lateinit var base: TextInputEditText
	private lateinit var manifestUrl: TextInputEditText
	private lateinit var packageNameInput: TextInputEditText
	private lateinit var urlPatternInput: TextInputEditText
	private lateinit var userAgentInput: TextInputEditText
	private lateinit var headers: TextInputEditText
	private lateinit var iconEditor: IconEditor
	private lateinit var json: TextInputEditText
	private lateinit var useManifest: MaterialCheckBox
	private lateinit var debugging: MaterialCheckBox
	private lateinit var notchAreas: MaterialCheckBox
	private lateinit var install: MaterialCheckBox
	private lateinit var advanced: LinearLayout
	private lateinit var manual: LinearLayout
	private lateinit var primaryFields: LinearLayout
	private lateinit var manifestSection: LinearLayout
	private val colors = linkedMapOf<String, ColorButton>()
	private val optionColors = linkedMapOf<String, ColorButton>()
	private val selections = linkedMapOf<String, SelectionInput>()
	private val fields = linkedMapOf<String, TextInputEditText>()
	private var userLibrary = emptyList<UserContentFiles.Entry>()
	private lateinit var userFiles: UserContentSelection

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		title(
			if (intent.hasExtra("id")) {
				R.string.edit_app
			} else {
				R.string.new_app
			},
			true,
		)
		saveAction =
			toolbar.menu.add(0, android.R.id.button1, 0, R.string.save).apply {
				val drawable =
					androidx.appcompat.content.res.AppCompatResources
						.getDrawable(this@EditorActivity, R.drawable.ic_check)!!
						.mutate()
				androidx.core.graphics.drawable.DrawableCompat.setTint(
					drawable,
					com.google.android.material.color.MaterialColors
						.getColor(toolbar, com.google.android.material.R.attr.colorOnSurface),
				)
				icon = drawable
				setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
				isEnabled = false
				setOnMenuItemClickListener {
					save()
					true
				}
			}
		onBackPressedDispatcher.addCallback(
			this,
			object : OnBackPressedCallback(true) {
				override fun handleOnBackPressed() = navigateBack()
			},
		)
		state?.getString("loadingError")?.let {
			showLoadingError(it)
			return
		}
		work({
			val recovery =
				intent
					.getStringExtra("saveDraft")
					?.let { SaveJobs.recoveryFile(this, it) }
					?.takeIf { it.isFile }
					?.let { JSONObject(it.readText()) }
			AppDatabase(this).use { db ->
				original =
					(
						recovery?.optString("originalKey")?.takeIf(String::isNotBlank)
							?: intent.getStringExtra("id")
					)?.let(db::get)
			}
			original?.let { InstallCoordinator.ensureEditable(this, it) }
			editing = original != null
			val initial = original ?: WebApp().apply { intent.getStringExtra("baseUrl")?.let { set("baseUrl", it) } }
			initialFormState = initialFormState(initial)
			state?.getString("stateToken")?.takeIf { it.matches(Regex("[0-9a-f-]{36}")) }?.let { stateToken = it }
			val draft = java.io.File(cacheDir, "editor-$stateToken.json")
			app = (
				if (state != null && draft.isFile) {
					WebApp(JSONObject(draft.readText()))
				} else {
					recovery?.getJSONObject("app")?.let(::WebApp)
				}
			)
				?: initial.copy()
			loginSession = state?.getString("loginSession") ?: recovery?.optString("loginSession")?.takeIf { it.isNotBlank() && it != "null" }
			pendingLoginProfile =
				(state?.getString("loginProfile") ?: recovery?.optString("loginProfile"))
					?.takeIf { it.matches(Regex("login-[0-9a-f-]{36}\\.zip")) }
					?.let { java.io.File(filesDir, it) }
					?.takeIf { it.isFile }
			retrySave = state?.getBoolean("retrySave") ?: false
			rulesCustomized = state?.getBoolean("rulesCustomized") ?: app.data.has("adblockRules")
			rulesExpanded = state?.getBoolean("rulesExpanded") ?: false
			iconSources = state?.getString("iconSources") ?: app
				.manifest()
				.optJSONArray("icons")
				?.toString()
				.orEmpty()
			userLibrary = UserContentLibrary.load(this)
		}, { showForm(state) }, { failure -> showLoadingError(failure.message ?: failure.toString()) })
	}

	private fun showLoadingError(message: String) {
		loadingError = message
		loadingErrorDialog =
			MaterialAlertDialogBuilder(this)
				.setTitle(R.string.error_title)
				.setMessage(getString(R.string.error_detail, getString(R.string.action_failed), message))
				.setPositiveButton(android.R.string.ok) { _, _ -> finish() }
				.setOnCancelListener { finish() }
				.show()
	}

	private fun showForm(state: Bundle?) {
		title(
			if (editing) {
				R.string.edit_app
			} else {
				R.string.new_app
			},
			true,
		)
		val form = scrollBody()
		base =
			if (editing) {
				TextInputEditText(this).apply { setText(app.s("baseUrl")) }
			} else {
				field(form, R.string.base_url, app.s("baseUrl"), false).apply { inputType = 17 }
			}
		if (!editing) {
			button(form, R.string.advanced) {
				advanced.visibility =
					if (advanced.visibility ==
						View.VISIBLE
					) {
						View.GONE
					} else {
						View.VISIBLE
					}
			}
		}
		advanced =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				visibility =
					if (editing) {
						View.VISIBLE
					} else {
						View.GONE
					}
				form.addView(this)
			}
		useManifest =
			MaterialCheckBox(this).apply {
				setText(R.string.use_manifest)
				isChecked = app.b("useManifest")
				if (!editing) {
					advanced.addView(this)
				}
			}
		manifestSection =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				advanced.addView(this)
			}
		manifestUrl = field(manifestSection, R.string.manifest_url, app.s("manifestUrl"), false).apply { inputType = 17 }
		button(manifestSection, R.string.fetch_manifest) {
			if (editing) {
				MaterialAlertDialogBuilder(this)
					.setTitle(R.string.fetch_manifest)
					.setMessage(R.string.fetch_manifest_confirmation)
					.setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(R.string.fetch_manifest) { _, _ -> fetch(false) }
					.show()
			} else {
				fetch(false)
			}
		}
		val jsonSection =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				visibility = View.GONE
			}
		button(advanced, R.string.view_json) {
			jsonSection.visibility =
				if (jsonSection.visibility == View.VISIBLE) {
					View.GONE
				} else {
					View.VISIBLE
				}
		}
		advanced.addView(jsonSection)
		text(jsonSection, getString(R.string.json_hint), 14)
		json =
			field(jsonSection, R.string.json_manifest, ManifestText.format(app.manifest()), true).apply {
				typeface = Typeface.MONOSPACE
				minLines = 8
			}
		button(jsonSection, R.string.apply_json) {
			try {
				app.set("baseUrl", base.text.toString().trim())
				app.set("manifestUrl", manifestUrl.text.toString().trim())
				app.set("manifest", JSONObject(json.text.toString()))
				app.applyManifestScope()
				app.syncIconPurposes()
				populate()
			} catch (error: Exception) {
				error(error)
			}
		}
		primaryFields =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				advanced.addView(this)
			}
		fields["name"] = field(primaryFields, R.string.name, app.manifest().optString("name", ""), false)
		fields["start_url"] = field(primaryFields, R.string.start_url, app.manifest().optString("start_url", ""), false).apply { inputType = 17 }
		urlPatternInput = field(primaryFields, R.string.url_patterns, app.s("urlPatterns"), true)
		text(primaryFields, getString(R.string.url_patterns_hint), 14)
		packageNameInput = field(advanced, R.string.package_name, app.s("packageName"), false)
		headers = field(advanced, R.string.headers, app.s("headers"), true)
		userAgentInput = field(advanced, R.string.user_agent, app.s("userAgent"), false)
		if (!editing) {
			button(advanced, R.string.open_temporary_browser) { openTemporaryBrowser() }
		}
		userFiles = UserContentSelection(this, advanced, userLibrary, UserContentFiles.read(app.data))
		rulesText = app.s("adblockRules")
		val rulesContainer =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				visibility =
					if (rulesExpanded) {
						View.VISIBLE
					} else {
						View.GONE
					}
			}
		button(advanced, R.string.edit_adblock_rules) {
			rulesText = currentRules()
			rulesExpanded = !rulesExpanded
			rulesContainer.visibility =
				if (rulesExpanded) {
					View.VISIBLE
				} else {
					View.GONE
				}
			rulesRenderGeneration++
			if (rulesExpanded) {
				setRules(rulesText)
			}
		}
		advanced.addView(rulesContainer)
		adblockInput =
			field(rulesContainer, R.string.adblock_rules, "", true).apply {
				minLines = 4
				maxLines = 8
				setHorizontallyScrolling(true)
				isVerticalScrollBarEnabled = true
				isHorizontalScrollBarEnabled = true
				enableTouchScrolling(this)
			}
		rulesLoading =
			android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
				isIndeterminate = true
				visibility = View.GONE
				contentDescription = getString(R.string.working)
				rulesContainer.addView(this, LinearLayout.LayoutParams(-1, dp(3)))
			}
		rulesPager =
			LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = android.view.Gravity.CENTER_VERTICAL
				rulesContainer.addView(this)
			}
		rulesPrevious =
			button(rulesPager, R.string.previous_rules_page) { showRulesPage(rulesPage - 1) }.apply {
				layoutParams =
					LinearLayout.LayoutParams(-2, -2)
			}
		rulesPageLabel = text(rulesPager, "", 14).apply { gravity = android.view.Gravity.CENTER }
		rulesPageLabel.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
		rulesNext =
			button(rulesPager, R.string.next_rules_page) { showRulesPage(rulesPage + 1) }.apply { layoutParams = LinearLayout.LayoutParams(-2, -2) }
		button(rulesContainer, R.string.clear_rules) {
			rulesCustomized = true
			rulesGeneration++
			setRules("")
		}
		text(advanced, getString(R.string.adblock_rules_hint), 14)
		button(advanced, R.string.refresh_default_rules) {
			if (busy) {
				return@button
			}
			val url = rulesUrl()
			val generation = ++rulesGeneration
			refreshingRules = true
			work({
				val rules = AdblockSettings.cachedDefaults(this, url)
				rules to
					io.github.ulysseszh.pwainstaller.core.RulePages
						.split(rules)
			}, { (rules, pages) ->
				refreshingRules = false
				if (generation == rulesGeneration) {
					rulesCustomized = false
					replaceRules(rules, pages)
					toast(R.string.rules_refreshed)
				}
			}, { failure ->
				refreshingRules = false
				error(failure)
			})
		}
		adblockInput.addTextChangedListener(
			object : android.text.TextWatcher {
				override fun beforeTextChanged(
					s: CharSequence?,
					start: Int,
					count: Int,
					after: Int,
				) = Unit

				override fun onTextChanged(
					s: CharSequence?,
					start: Int,
					before: Int,
					count: Int,
				) {
					if (!settingRules) {
						rulesCustomized = true
						rulesGeneration++
						rulesDirty = true
						rulesPages[rulesPage] = s.toString()
					}
				}

				override fun afterTextChanged(s: android.text.Editable?) = Unit
			},
		)
		base.addTextChangedListener(
			object : android.text.TextWatcher {
				override fun beforeTextChanged(
					s: CharSequence?,
					start: Int,
					count: Int,
					after: Int,
				) = Unit

				override fun onTextChanged(
					s: CharSequence?,
					start: Int,
					before: Int,
					count: Int,
				) {
					suggestRules()
				}

				override fun afterTextChanged(s: android.text.Editable?) = Unit
			},
		)
		install =
			MaterialCheckBox(this).apply {
				setText(R.string.install_added)
				isChecked = app.b("install")
				advanced.addView(this)
			}
		debugging =
			MaterialCheckBox(this).apply {
				setText(R.string.webview_debugging)
				isChecked = app.b("webViewDebugging")
				advanced.addView(this)
			}
		notchAreas =
			MaterialCheckBox(this).apply {
				setText(R.string.use_notch_areas)
				isChecked = app.b("useNotchAreas")
				advanced.addView(this)
			}
		val iconContainer =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				advanced.addView(this)
			}
		manual =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				advanced.addView(this)
			}
		text(manual, getString(R.string.manual_hint), 14)
		iconEditor =
			IconEditor(this, app, { iconIndex, purpose ->
				try {
					val value = JSONObject(json.text.toString())
					val cached = app.icons().getJSONObject(iconIndex)
					value.optJSONArray("icons")?.let { icons ->
						val position = cached.optInt("manifestIndex", -1)
						val source = cached.getString("src")
						val matching =
							if (position in 0 until icons.length() && icons.getJSONObject(position).optString("src") == source) {
								icons.getJSONObject(position)
							} else {
								(0 until icons.length()).mapNotNull { icons.optJSONObject(it) }.singleOrNull { it.optString("src") == source }
							}
						matching?.put("purpose", purpose)
					}
					json.setText(ManifestText.format(value))
				} catch (failure: Exception) {
					error(failure)
				}
			}) {
				launchForResult(
					Intent(Intent.ACTION_OPEN_DOCUMENT)
						.setType("image/*")
						.addCategory(Intent.CATEGORY_OPENABLE)
						.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true),
					42,
				)
			}.also { iconContainer.addView(it) }
		button(iconContainer, R.string.fetch_online_icons) { fetchOnlineIcons() }
		val labels =
			linkedMapOf(
				"short_name" to R.string.short_name,
				"description" to R.string.description,
				"id" to R.string.id,
				"lang" to R.string.lang,
				"dir" to R.string.dir,
			)
		labels.forEach { (key, label) -> fields[key] = field(manual, label, app.manifest().optString(key, ""), key == "description") }
		listOf(
			Triple("theme_color", R.string.theme_color, "darkThemeColor" to R.string.dark_theme_color),
			Triple("background_color", R.string.background_color, "darkBackgroundColor" to R.string.dark_background_color),
		).forEach { (key, label, dark) ->
			colors[key] =
				ColorButton(this, label).apply {
					value = app.manifest().optString(key)
					manual.addView(this, LinearLayout.LayoutParams(-1, -2))
				}
			optionColors[dark.first] =
				ColorButton(this, dark.second).apply {
					value = app.s(dark.first)
					manual.addView(this, LinearLayout.LayoutParams(-1, -2))
				}
		}
		text(manual, getString(R.string.dark_colors_hint), 14)

		fun selection(
			key: String,
			label: Int,
			values: List<String>,
			labels: Int,
		) {
			selections[key] =
				SelectionInput(this, label, values.zip(resources.getStringArray(labels).toList())).apply {
					value = app.manifest().optString(key)
					manual.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
				}
		}
		selection("display", R.string.display, listOf("", "standalone", "fullscreen", "minimal-ui", "browser"), R.array.display_options)
		selection(
			"orientation",
			R.string.orientation,
			listOf(
				"",
				"any",
				"natural",
				"portrait",
				"portrait-primary",
				"portrait-secondary",
				"landscape",
				"landscape-primary",
				"landscape-secondary",
			),
			R.array.orientation_options,
		)
		useManifest.setOnCheckedChangeListener { _, _ -> updateVisibility() }
		updateVisibility()
		saveAction.isEnabled = true
		if (state != null) {
			base.setText(state.getString("base"))
			manifestUrl.setText(state.getString("manifestUrl"))
			packageNameInput.setText(state.getString("packageName"))
			userAgentInput.setText(state.getString("userAgent"))
			urlPatternInput.setText(state.getString("urlPatterns"))
			setRules(state.getString("adblockRules") ?: app.s("adblockRules"))
			rulesPage = state.getInt("rulesPage")
			headers.setText(state.getString("headers"))
			json.setText(state.getString("json")?.let(ManifestText::readableSlashes))
			fields.forEach { (key, view) -> view.setText(state.getString("field:$key")) }
			colors.forEach { (key, view) -> view.value = state.getString("field:$key").orEmpty() }
			optionColors.forEach { (key, view) -> view.value = state.getString("option:$key").orEmpty() }
			selections.forEach { (key, view) -> view.value = state.getString("field:$key").orEmpty() }
			advanced.visibility =
				if (editing || state.getBoolean("advanced")) {
					View.VISIBLE
				} else {
					View.GONE
				}
		}
		suggestRules()
	}

	// Returning false preserves EditText's native click and accessibility handling.
	@android.annotation.SuppressLint("ClickableViewAccessibility")
	private fun enableTouchScrolling(input: TextInputEditText) {
		input.setOnTouchListener { view, event ->
			val scrolling =
				event.actionMasked != android.view.MotionEvent.ACTION_UP && event.actionMasked != android.view.MotionEvent.ACTION_CANCEL &&
					(view.canScrollVertically(1) || view.canScrollVertically(-1) || view.canScrollHorizontally(1) || view.canScrollHorizontally(-1))
			view.parent.requestDisallowInterceptTouchEvent(scrolling)
			false
		}
	}

	private fun currentRules(): String {
		if (rulesDirty) {
			rulesText = rulesPages.joinToString("")
			rulesDirty = false
		}
		return rulesText
	}

	private fun replaceRules(
		value: String,
		pages: List<String>,
	) {
		rulesRenderGeneration++
		rulesText = value
		rulesDirty = false
		rulesPages = pages.toMutableList()
		rulesPage = 0
		if (rulesExpanded) {
			showRulesPage(0)
		}
	}

	private fun showRulesPage(index: Int) {
		if (index !in rulesPages.indices) {
			return
		}
		rulesPage = index
		settingRules = true
		try {
			adblockInput.setText(rulesPages[index])
			adblockInput.scrollTo(0, 0)
		} finally {
			settingRules = false
		}
		adblockInput.isEnabled = true
		rulesLoading.visibility = View.GONE
		rulesPager.visibility =
			if (rulesPages.size > 1) {
				View.VISIBLE
			} else {
				View.GONE
			}
		rulesPageLabel.text = getString(R.string.rules_page, index + 1, rulesPages.size)
		rulesPrevious.isEnabled = index > 0
		rulesNext.isEnabled = index < rulesPages.lastIndex
	}

	private fun setRules(value: String) {
		val generation = ++rulesRenderGeneration
		rulesText = value
		rulesDirty = false
		rulesPage = 0
		if (!rulesExpanded) {
			return
		}
		settingRules = true
		try {
			adblockInput.setText("")
		} finally {
			settingRules = false
		}
		adblockInput.isEnabled = false
		rulesPager.visibility = View.GONE
		rulesLoading.visibility = View.VISIBLE
		InstallerApplication.readers.execute {
			val pages =
				io.github.ulysseszh.pwainstaller.core.RulePages
					.split(value)
			runOnUiThread {
				if (!isFinishing && !isDestroyed && rulesExpanded && generation == rulesRenderGeneration) {
					val page = rulesPage
					replaceRules(value, pages)
					showRulesPage(page.coerceIn(0, rulesPages.lastIndex))
				}
			}
		}
	}

	private fun suggestRules() {
		val generation = ++rulesGeneration
		ruleSuggestion?.let(ruleHandler::removeCallbacks)
		if (editing || rulesCustomized) {
			return
		}
		val url = base.text.toString().trim()
		if (runCatching {
				io.github.ulysseszh.pwainstaller.core.PackageNames
					.webUrl(url)
			}.isFailure
		) {
			return
		}
		ruleSuggestion =
			Runnable {
				InstallerApplication.readers.execute {
					if (generation != rulesGeneration) {
						return@execute
					}
					val rules = runCatching { AdblockSettings.cachedDefaults(this, url) }.getOrNull() ?: return@execute
					runOnUiThread {
						if (!isFinishing && !isDestroyed && !busy && !rulesCustomized && generation == rulesGeneration) {
							setRules(rules)
						}
					}
				}
			}.also { ruleHandler.postDelayed(it, 350) }
	}

	private fun rulesUrl(): String {
		if (!editing) {
			return base.text.toString().trim()
		}
		val start =
			fields
				.getValue("start_url")
				.text
				.toString()
				.trim()
				.ifEmpty { app.s("baseUrl") }
		return runCatching {
			java.net
				.URI(
					manifestUrl.text
						.toString()
						.trim()
						.ifEmpty { app.s("baseUrl") },
				).resolve(start)
				.toString()
		}.getOrDefault(start)
	}

	// Retain string references rather than serializing embedded icon data on the UI thread.
	private fun iconState(source: WebApp): List<Map<String, String>> =
		source.icons().let { icons ->
			(0 until icons.length()).map { index ->
				icons.getJSONObject(index).let { icon ->
					icon.keys().asSequence().associateWith { icon.get(it).toString() }
				}
			}
		}

	private fun initialFormState(source: WebApp): Map<String, Any> =
		buildMap {
			textOptions.forEach { put(it, source.s(it)) }
			booleanOptions.forEach { put(it, source.b(it)) }
			manifestOptions.forEach { put("field:$it", source.manifest().optString(it, "")) }
			put("json", ManifestText.format(source.manifest()))
			put("icons", iconState(source))
			put("icon", source.s("icon"))
			put("userFiles", UserContentFiles.read(source.data).map { it.id }.sorted())
		}

	private fun currentFormState(): Map<String, Any> =
		buildMap {
			put("baseUrl", base.text.toString())
			put("manifestUrl", manifestUrl.text.toString())
			put("packageName", packageNameInput.text.toString())
			put("headers", headers.text.toString())
			put("userAgent", userAgentInput.text.toString())
			put("urlPatterns", urlPatternInput.text.toString())
			put("adblockRules", currentRules())
			put("useManifest", useManifest.isChecked)
			put("install", install.isChecked)
			put("webViewDebugging", debugging.isChecked)
			put("useNotchAreas", notchAreas.isChecked)
			fields.forEach { (key, input) -> put("field:$key", input.text.toString()) }
			colors.forEach { (key, input) -> put("field:$key", input.value) }
			optionColors.forEach { (key, input) -> put(key, input.value) }
			selections.forEach { (key, input) -> put("field:$key", input.value) }
			put("json", json.text.toString())
			put("icons", iconState(app))
			put("icon", app.s("icon"))
			put("userFiles", userFiles.selected().map { it.id }.sorted())
		}

	override fun navigateBack() {
		if ((busy && ::base.isInitialized) || discardDialog?.isShowing == true) {
			return
		}
		if (!::base.isInitialized || currentFormState() == initialFormState) {
			finish()
			return
		}
		discardDialog =
			MaterialAlertDialogBuilder(this)
				.setTitle(R.string.discard_changes_title)
				.setMessage(R.string.discard_changes_message)
				.setNegativeButton(R.string.keep_editing, null)
				.setPositiveButton(R.string.discard_changes) { _, _ -> finish() }
				.create()
				.also { dialog ->
					dialog.setOnDismissListener { discardDialog = null }
					dialog.show()
				}
	}

	private fun defaultRules(
		candidate: WebApp,
		progress: (FetchProgress) -> Unit,
	) {
		if (!rulesCustomized) {
			progress(FetchProgress(FetchProgress.Stage.FILTERS))
			candidate.set("adblockRules", AdblockSettings.cachedDefaults(this, candidate.s("baseUrl")))
		}
	}

	private fun updateVisibility() {
		manual.visibility =
			if (editing || !useManifest.isChecked) {
				View.VISIBLE
			} else {
				View.GONE
			}
		primaryFields.visibility = manual.visibility
		manifestSection.visibility =
			if (editing || useManifest.isChecked) {
				View.VISIBLE
			} else {
				View.GONE
			}
	}

	private fun populate() {
		iconEditor.bind(app)
		setRules(app.s("adblockRules"))
		manifestUrl.setText(app.s("manifestUrl"))
		urlPatternInput.setText(app.s("urlPatterns"))
		fields.forEach { (key, view) -> view.setText(app.manifest().optString(key, "")) }
		colors.forEach { (key, view) -> view.value = app.manifest().optString(key) }
		optionColors.forEach { (key, view) -> view.value = app.s(key) }
		selections.forEach { (key, view) -> view.value = app.manifest().optString(key) }
		json.setText(ManifestText.format(app.manifest()))
	}

	private fun read(): WebApp = snapshot()()

	private fun snapshot(): () -> WebApp {
		val source = app
		val selectedFiles = userFiles.selected()
		val values =
			linkedMapOf<String, Any>(
				"baseUrl" to base.text.toString().trim(),
				"manifestUrl" to manifestUrl.text.toString().trim(),
				"packageName" to packageNameInput.text.toString().trim(),
				"headers" to headers.text.toString(),
				"userAgent" to userAgentInput.text.toString(),
				"urlPatterns" to urlPatternInput.text.toString().trim(),
				"adblockRules" to currentRules(),
				"useManifest" to useManifest.isChecked,
				"install" to install.isChecked,
				"webViewDebugging" to debugging.isChecked,
				"useNotchAreas" to notchAreas.isChecked,
			)
		optionColors.forEach { (key, input) -> values[key] = input.value }
		val manifestText = json.text.toString()
		val manifestFields =
			fields.map { (key, input) -> key to input.text.toString().trim() } +
				colors.map { it.key to it.value.value } + selections.map { it.key to it.value.value }
		return {
			source.copy().apply {
				values.forEach { (key, value) -> set(key, value) }
				UserContentFiles.put(data, selectedFiles)
				val manifest = JSONObject(manifestText)
				manifestFields.forEach { (key, value) ->
					if (value.isEmpty()) {
						manifest.remove(key)
					} else {
						manifest.put(key, value)
					}
				}
				set("manifest", manifest)
				syncIconPurposes()
			}
		}
	}

	private fun validateSave(candidate: WebApp) {
		candidate.validateIdentity()
	}

	private fun fetch(saving: Boolean) {
		if (busy) {
			return
		}
		rulesGeneration++
		ruleSuggestion?.let(ruleHandler::removeCallbacks)
		try {
			val draft = snapshot()
			retrySave = saving
			fetchWork({ progress ->
				val candidate = ManifestFetcher.fetch(draft(), networkSession(), validateMetadata = !saving, onProgress = progress)
				if (!saving) {
					defaultRules(candidate, progress)
				}
				candidate.also {
					if (saving) {
						validateSave(it)
					}
				}
			}, { result ->
				if (saving) {
					finishSave(result)
				} else {
					app = result
					iconSources =
						result
							.manifest()
							.optJSONArray("icons")
							?.toString()
							.orEmpty()
					populate()
					toast(R.string.manifest_updated)
				}
			}, ::fetchFailure)
		} catch (error: Exception) {
			error(error)
		}
	}

	private fun networkSession(): ManifestFetcher.Session {
		val token = loginSession
		val current = LoginSessions.current(this)
		if (token != null && current?.optString("token") == token && current.optString("appKey") == app.s("key")) {
			return ManifestFetcher.Session(this, token)
		}
		loginSession = null
		return ManifestFetcher.Session()
	}

	private fun fetchOnlineIcons() {
		if (busy) {
			return
		}
		val content =
			LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(dp(24), dp(12), dp(24), 0)
			}
		val url = field(content, R.string.icon_fetch_url, rulesUrl(), false).apply { inputType = 17 }
		val dialog =
			MaterialAlertDialogBuilder(this)
				.setTitle(R.string.fetch_online_icons)
				.setView(content)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.fetch_online_icons, null)
				.create()
		dialog.setOnShowListener {
			dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
				try {
					val location =
						io.github.ulysseszh.pwainstaller.core.PackageNames
							.webUrl(url.text.toString().trim())
					val draft = snapshot()
					dialog.dismiss()
					fetchWork({ progress ->
						val icons = ManifestFetcher.onlineIcons(draft(), location, networkSession(), progress)
						require(app.icons().length() + icons.length() <= 32) { "Too many icons (maximum 32)" }
						(0 until icons.length()).map { icons.getJSONObject(it) }
					}, { icons -> iconEditor.add(icons) })
				} catch (failure: Exception) {
					url.error = failure.message
				}
			}
		}
		dialog.show()
	}

	private fun openTemporaryBrowser() {
		if (busy) {
			return
		}
		val draft = snapshot()
		work({
			val candidate = draft()
			io.github.ulysseszh.pwainstaller.core.PackageNames
				.webUrl(candidate.s("baseUrl"))
			val current = LoginSessions.current(this)
			val token =
				loginSession?.takeIf { current?.optString("token") == it && current.optString("appKey") == candidate.s("key") }
					?: if (editing && original?.let { Packages.installed(this, it.s("packageName")) } == true) {
						val seed = java.io.File(cacheDir, "browser-seed-${java.util.UUID.randomUUID()}.zip")
						try {
							Profiles.export(this, original!!.s("packageName"), seed)
							LoginSessions.begin(this, candidate.s("key"), seed)
						} finally {
							seed.delete()
						}
					} else {
						LoginSessions.begin(this, candidate.s("key"))
					}
			candidate to token
		}, { (candidate, token) ->
			loginSession = token
			retrySave = false
			launchForResult(LoginSessions.intent(this, candidate, token), 41)
		})
	}

	private fun fetchFailure(failure: Throwable) {
		if (failure is ManifestFetcher.CredentialsRequired) {
			val dialog =
				MaterialAlertDialogBuilder(this)
					.setTitle(R.string.login_title)
					.setMessage(
						if (failure is ManifestFetcher.ManifestNotFound) {
							R.string.manifest_login_message
						} else {
							R.string.login_message
						},
					).setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(R.string.login) { _, _ -> openTemporaryBrowser() }
			if (failure is ManifestFetcher.ManifestNotFound) {
				dialog.setNeutralButton(R.string.add_without_manifest) { _, _ ->
					useManifest.isChecked = false
					advanced.visibility = View.VISIBLE
					updateVisibility()
					retrySave = false
				}
			}
			dialog.show()
		} else {
			error(failure)
		}
	}

	private fun save() {
		if (refreshingRules) {
			toast(R.string.rules_refreshing)
			return
		}
		if (busy) {
			return
		}
		rulesGeneration++
		ruleSuggestion?.let(ruleHandler::removeCallbacks)
		if (!editing && useManifest.isChecked) {
			fetch(true)
			return
		}
		try {
			val draft = snapshot()
			retrySave = true
			if (editing) {
				val task = { draft().also(::validateSave) }
				work(task, ::finishSave)
				return
			}
			fetchWork({ progress ->
				val candidate = draft()
				ManifestFetcher.prepareManual(candidate, networkSession(), progress)
				if (candidate
						.s(
							"urlPatterns",
						).isBlank()
				) {
					candidate.set(
						"urlPatterns",
						io.github.ulysseszh.pwainstaller.core.UrlRules
							.defaultScopes(candidate.s("baseUrl")),
					)
				}
				validateSave(candidate)
				candidate
			}, ::finishSave, ::fetchFailure)
		} catch (error: Exception) {
			error(error)
		}
	}

	private fun finishSave(candidate: WebApp) {
		val old = original
		if (old != null && old.s("packageName") != candidate.s("packageName") && Packages.installed(this, old.s("packageName"))) {
			MaterialAlertDialogBuilder(this)
				.setTitle(R.string.migrate_title)
				.setMessage(R.string.migrate_message)
				.setNegativeButton(android.R.string.cancel, null)
				.setNeutralButton(R.string.save_only) { _, _ -> persist(candidate, false) }
				.setPositiveButton(R.string.migrate) { _, _ -> persist(candidate, true) }
				.show()
		} else {
			persist(candidate, false)
		}
	}

	private fun persist(
		candidate: WebApp,
		migrate: Boolean,
	) {
		try {
			ProgressNotifications.requestPermission(this)
			if (!SaveJobs.start(
					this,
					candidate,
					if (migrate) {
						original!!.s("packageName")
					} else {
						null
					},
					pendingLoginProfile,
					loginSession,
					original?.s("key"),
					selectDefaultRules = !editing && !rulesCustomized,
				)
			) {
				toast(R.string.build_already_running)
				return
			}
			// Ownership of the temporary profile passes to the background job.
			loginSession = null
			pendingLoginProfile = null
			val next = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
			startActivity(next)
			finish()
		} catch (failure: Exception) {
			error(failure)
		}
	}

	override fun onExternalActivityResult(
		code: Int,
		result: Int,
		data: Intent?,
	) {
		if (code == 42 && result == RESULT_OK && data != null) {
			val uris =
				data.clipData?.let { clip -> (0 until clip.itemCount).map { clip.getItemAt(it).uri } }
					?: listOfNotNull(data.data)
			if (uris.isNotEmpty()) {
				work({
					require(app.icons().length() + uris.size <= 32) { "Too many icons (maximum 32)" }
					uris.map { uri ->
						val input = contentResolver.openInputStream(uri) ?: throw java.io.IOException("Cannot read icon")
						input.use { IconData.entry(ManifestFetcher.read(it, 2 * 1024 * 1024), "upload:${java.util.UUID.randomUUID()}") }
					}
				}, { icons -> iconEditor.add(icons) })
			}
		}
		if (code == 41) {
			loginSession = data?.getStringExtra("session") ?: loginSession
			retrySave = false
			loginSession?.let { token ->
				work({
					LoginSessions.call(this, token, "flush")
					LoginSessions.completed(this, token)
				}, { /* Return to the draft; the user chooses when to fetch or save. */ })
			}
		}
	}

	override fun onDestroy() {
		ruleHandler.removeCallbacksAndMessages(null)
		discardDialog?.dismiss()
		discardDialog = null
		loadingErrorDialog?.dismiss()
		loadingErrorDialog = null
		if (isFinishing) {
			java.io.File(cacheDir, "editor-$stateToken.json").delete()
			if (!busy) {
				pendingLoginProfile?.delete()
			}
			loginSession?.let { token -> InstallerApplication.worker.execute { runCatching { LoginSessions.discard(this, token) } } }
		}
		super.onDestroy()
	}

	override fun onSaveInstanceState(out: Bundle) {
		loadingError?.let { out.putString("loadingError", it) }
		if (!::base.isInitialized || isFinishing) {
			super.onSaveInstanceState(out)
			return
		}
		app.set("useManifest", useManifest.isChecked)
		app.set("install", install.isChecked)
		app.set("webViewDebugging", debugging.isChecked)
		app.set("useNotchAreas", notchAreas.isChecked)
		out.putBoolean("rulesCustomized", rulesCustomized)
		out.putBoolean("rulesExpanded", rulesExpanded)
		out.putInt("rulesPage", rulesPage)
		out.putString("loginProfile", pendingLoginProfile?.name)
		out.putString("loginSession", loginSession)
		out.putString("iconSources", iconSources)
		// Keep large rule snapshots out of the Binder saved-state transaction.
		val draft = JSONObject(app.data.toString()).put("adblockRules", currentRules())
		UserContentFiles.put(draft, userFiles.selected())
		java.io.File(cacheDir, "editor-$stateToken.json").writeText(draft.toString())
		out.putString("stateToken", stateToken)
		out.putBoolean("retrySave", retrySave)
		out.putString("base", base.text.toString())
		out.putString("manifestUrl", manifestUrl.text.toString())
		out.putString("packageName", packageNameInput.text.toString())
		out.putString("urlPatterns", urlPatternInput.text.toString())
		out.putString("userAgent", userAgentInput.text.toString())
		out.putString("headers", headers.text.toString())
		out.putString("json", json.text.toString())
		fields.forEach { (key, view) -> out.putString("field:$key", view.text.toString()) }
		colors.forEach { (key, view) -> out.putString("field:$key", view.value) }
		optionColors.forEach { (key, view) -> out.putString("option:$key", view.value) }
		selections.forEach { (key, view) -> out.putString("field:$key", view.value) }
		out.putBoolean("advanced", advanced.visibility == View.VISIBLE)
		super.onSaveInstanceState(out)
	}

	companion object {
		private val textOptions =
			listOf(
				"baseUrl",
				"manifestUrl",
				"packageName",
				"headers",
				"userAgent",
				"urlPatterns",
				"adblockRules",
				"darkThemeColor",
				"darkBackgroundColor",
			)
		private val booleanOptions = listOf("useManifest", "install", "webViewDebugging", "useNotchAreas")
		private val manifestOptions =
			listOf(
				"name",
				"short_name",
				"description",
				"start_url",
				"id",
				"lang",
				"dir",
				"theme_color",
				"background_color",
				"display",
				"orientation",
			)
	}
}
