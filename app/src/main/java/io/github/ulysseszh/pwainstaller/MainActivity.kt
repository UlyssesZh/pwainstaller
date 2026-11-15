package io.github.ulysseszh.pwainstaller

import android.content.*
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Base64
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.ulysseszh.pwainstaller.core.DataArchive
import io.github.ulysseszh.pwainstaller.core.ApkProgress
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.Collator

class MainActivity : Screen() {
    override val showWorkProgress get() = false
    private lateinit var list: RecyclerView
    private lateinit var continueButton: FloatingActionButton
    private var listGeneration = 0
    private data class Row(val entry: AppDatabase.Summary, val icon: android.graphics.Bitmap?, val installed: Boolean)
    private data class RowStatus(val key: String, val name: String, val tile: Boolean, val card: View, val text: TextView, val progress: com.google.android.material.progressindicator.LinearProgressIndicator)
    private var rows = emptyList<Row>()
    private var libraryLoaded = false
    private class RowHolder(val container: FrameLayout) : RecyclerView.ViewHolder(container)
    private val listAdapter = object : RecyclerView.Adapter<RowHolder>() {
        override fun getItemCount() = rows.size + 1
        override fun onCreateViewHolder(parent: ViewGroup, type: Int) = RowHolder(FrameLayout(this@MainActivity).apply {
            layoutParams = RecyclerView.LayoutParams(-1, -2)
        })
        override fun onBindViewHolder(holder: RowHolder, position: Int) {
            holder.container.removeAllViews()
            if (position == 0) {
                val heading = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
                holder.container.addView(heading)
                if (!libraryLoaded) text(heading, getString(R.string.working), 16)
                else if (rows.isEmpty()) {
                    text(heading, getString(R.string.empty_title), 26).setPadding(dp(8), dp(72), dp(8), dp(12))
                    text(heading, getString(R.string.empty_body), 16)
                }
            } else holder.container.addView(createRow(rows[position - 1]), FrameLayout.LayoutParams(-1, -2).apply {
                val tiles = AppearanceSettings.layout(this@MainActivity) == "tiles"
                val gap = if (tiles) dp(1) else 0
                setMargins(gap, 0, gap, dp(if (tiles) 2 else 10))
            })
        }
    }
    private fun updateLibraryLayout() {
        if (!::list.isInitialized) return
        val columns = if (AppearanceSettings.layout(this) == "tiles") (resources.configuration.screenWidthDp / 96).coerceIn(2, 6) else 1
        list.layoutManager = GridLayoutManager(this, columns).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() { override fun getSpanSize(position: Int) = if (position == 0) columns else 1 }
        }
        listAdapter.notifyDataSetChanged()
    }
    private var documentAction: String? = null
    private var documentApp: String? = null
    private var documentName: String? = null
    private var metadataImportFile: java.io.File? = null
    private var metadataChecks: BooleanArray? = null
    private var loginApp: String? = null
    private var loginSession: String? = null
    private var confirmBrowserChanges = false
    private var browserChangesDialog: androidx.appcompat.app.AlertDialog? = null
    private var permissionOperation: String? = null
    private var shizukuOperation: String? = null
    private val shizukuPermission = rikka.shizuku.Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == 221) {
            val token = shizukuOperation; shizukuOperation = null
            if (token != null && result == android.content.pm.PackageManager.PERMISSION_GRANTED) startInstall(token)
            else { token?.let { InstallCoordinator.interrupt(this, it) }; renderContinue(); error(IllegalStateException(getString(R.string.shizuku_not_ready))) }
        }
    }
    private var renderedBuilds = emptyMap<String, BuildJobs.State>()
    private var operationStatus = emptyMap<String, String>()
    private var queueKeys = emptySet<String>()
    private val starting = mutableSetOf<String>()
    private var inspecting = false
    private var visible = false
    private var pendingInspection = false
    private var recoverInspection = false
    private val inspectionPoll = Runnable { inspectOperations() }
    private var submittingInstall = false
    private val buildHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val buildPoll = Runnable { renderBuild() }
    private val shown = mutableSetOf<String>()
    private val changes = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { refresh(); inspectOperations() }
    }

    private fun showAbout() {
        val versions = VersionInformation.collect(this)
        val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.about)
            .setMessage("$versions\n\n${getString(R.string.about_text)}")
            .setNeutralButton(R.string.copy_version_info, null)
            .setPositiveButton(android.R.string.ok, null).show()
        dialog.findViewById<TextView>(android.R.id.message)?.setTextIsSelectable(true)
        dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText(getString(R.string.about), versions))
            toast(R.string.version_info_copied)
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        title(R.string.app_name, false)
        if (Build.VERSION.SDK_INT >= 24) rikka.shizuku.Shizuku.addRequestPermissionResultListener(shizukuPermission)
        listOf(1 to R.string.export_all, 2 to R.string.import_all, 3 to R.string.settings, 4 to R.string.about, 6 to R.string.reinstall_all)
            .forEach { (id, label) -> toolbar.menu.add(0, id, id, label) }
        toolbar.setOnMenuItemClickListener { item ->
            if (!busy) when (item.itemId) {
                1 -> MaterialAlertDialogBuilder(this).setMessage(R.string.metadata_notice).setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.export_all) { _, _ -> document("export_all", null, true, "application/json", "pwa-installer.json") }.show()
                2 -> MaterialAlertDialogBuilder(this).setMessage(R.string.import_notice).setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.import_all) { _, _ -> document("import_all", null, false, "application/json", null) }.show()
                3 -> startActivity(Intent(this, SettingsActivity::class.java))
                4 -> showAbout()
                6 -> reinstallAll()
            }
            true
        }
        val frame = FrameLayout(this)
        body.addView(frame, LinearLayout.LayoutParams(-1, -1))
        list = RecyclerView(this).apply {
            setPadding(dp(16), dp(8), dp(16), dp(100)); clipToPadding = false
            adapter = listAdapter
        }
        frame.addView(list, FrameLayout.LayoutParams(-1, -1))
        updateLibraryLayout()
        val add = FloatingActionButton(this).apply { setImageResource(R.drawable.ic_add); contentDescription = getString(R.string.add_app) }
        frame.addView(add, FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM or Gravity.END).apply { setMargins(dp(24), dp(24), dp(24), dp(24)) })
        add.setOnClickListener { if (!busy) startActivity(Intent(this, EditorActivity::class.java)) }
        continueButton = FloatingActionButton(this).apply {
            setImageResource(R.drawable.ic_play)
            contentDescription = getString(R.string.continue_action)
            androidx.appcompat.widget.TooltipCompat.setTooltipText(this, contentDescription)
            visibility = View.GONE
            setOnClickListener {
                if (!busy) { ReinstallQueue.resume(this@MainActivity); shown.clear(); inspectOperations(recoverConfirmation = true) }
            }
        }
        frame.addView(continueButton, FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM or Gravity.END).apply {
            setMargins(dp(24), dp(24), dp(24), dp(96))
        })
        if (state != null) {
            shizukuOperation = state.getString("shizukuOperation")
            documentAction = state.getString("documentAction"); documentApp = state.getString("documentApp"); documentName = state.getString("documentName")
            loginSession = state.getString("loginSession")
            confirmBrowserChanges = state.getBoolean("confirmBrowserChanges")
            loginApp = state.getString("loginApp"); permissionOperation = state.getString("permissionOperation")
            metadataImportFile = state.getString("metadataImportFile")?.let { java.io.File(it) }?.takeIf { it.isFile && it.parentFile == cacheDir }
            metadataChecks = state.getBooleanArray("metadataChecks")
        }
        handleIntent(intent)
        metadataImportFile?.let { file -> work({ JSONObject(file.readText()).getJSONArray("apps") }, ::chooseMetadata) }
        if (confirmBrowserChanges) confirmTemporaryBrowser()
    }
    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(this, changes, IntentFilter(InstallReceiver.CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
    }
    override fun onPause() { visible = false; super.onPause() }
    override fun onStop() { buildHandler.removeCallbacks(buildPoll); buildHandler.removeCallbacks(inspectionPoll); unregisterReceiver(changes); super.onStop() }
    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= 24) rikka.shizuku.Shizuku.removeRequestPermissionResultListener(shizukuPermission)
        browserChangesDialog?.dismiss(); browserChangesDialog = null
        buildHandler.removeCallbacks(buildPoll); super.onDestroy() }
    override fun onResume() { super.onResume(); visible = true; updateLibraryLayout(); refresh(); renderBuild(); inspectOperations() }
    override fun onWindowFocusChanged(focused: Boolean) {
        super.onWindowFocusChanged(focused)
        if (focused && visible && pendingInspection) inspectOperations()
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleIntent(intent) }
    private fun reinstallAll() {
        if (busy) return
        val previous = operationStatus
        showQueued(rows.map { it.entry.key })
        work({ ReinstallQueue.begin(this) }, { refresh(); continueQueue() }, { failure ->
            operationStatus = previous; updateVisibleStatuses(); error(failure)
        })
    }
    private fun handleIntent(intent: Intent) {
        if (intent.action == "io.github.ulysseszh.pwainstaller.REINSTALL_ALL") {
            intent.action = Intent.ACTION_MAIN // Consume once, including across activity recreation.
            reinstallAll()
        }
        if (intent.getBooleanExtra("resumeInstallConfirmation", false)) {
            intent.removeExtra("resumeInstallConfirmation")
            shown.clear(); inspectOperations(recoverConfirmation = true)
        }
        val operation = intent.getStringExtra("operation")
        intent.removeExtra("operation")
        if (operation != null) {
            intent.getStringExtra("operationKey")?.let { showQueued(listOf(it)) }
            intent.removeExtra("operationKey")
            startInstall(operation)
        }
    }
    private fun app(key: String): WebApp = AppDatabase(this).use { it.get(key) ?: error("App no longer exists") }
    private fun refresh() {
        if (!::list.isInitialized) return
        val generation = ++listGeneration
        val context = applicationContext
        InstallerApplication.readers.execute {
            try {
                val names = Collator.getInstance().apply { strength = Collator.PRIMARY }
                val saved = AppDatabase(context).use { db -> db.summaries() }
                val pending = BuildJobs.active().mapNotNull { it.entry }
                val entries = (saved + pending.filter { entry -> saved.none { it.key == entry.key } }).sortedWith { left, right ->
                    names.compare(left.name, right.name).takeIf { it != 0 }
                        ?: left.packageName.compareTo(right.packageName)
                }
                val rows = entries.map { entry ->
                    val bitmap = entry.thumbnail?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    Row(entry, bitmap, Packages.installed(context, entry.packageName))
                }
                runOnUiThread {
                    if (isFinishing || isDestroyed || generation != listGeneration) return@runOnUiThread
                    this.rows = rows; libraryLoaded = true
                    toolbar.menu.findItem(6).isEnabled = rows.isNotEmpty() && !ReinstallQueue.has(this)
                    listAdapter.notifyDataSetChanged()
                }
            } catch (failure: Exception) { runOnUiThread { if (generation == listGeneration) error(failure) } }
        }
        renderContinue()
    }
    private fun renderContinue() {
        queueKeys = ReinstallQueue.pendingKeys(this).toSet()
        val pending = ReinstallQueue.paused(this) || InstallCoordinator.tokens(this).any { token -> token !in starting && InstallCoordinator.interrupted(this, token) }
        continueButton.visibility = if (pending) View.VISIBLE else View.GONE
        list.setPadding(dp(16), dp(8), dp(16), dp(if (pending) 172 else 100))
    }
    private fun createRow(value: Row): MaterialCardView {
        val app = value.entry
        val tiles = AppearanceSettings.layout(this) == "tiles"
        val installed = value.installed
        val card = MaterialCardView(this).apply { radius = dp(18).toFloat(); cardElevation = 0f; strokeWidth = 0 }
        val row = LinearLayout(this).apply {
            orientation = if (tiles) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            gravity = if (tiles) Gravity.CENTER_HORIZONTAL else Gravity.CENTER_VERTICAL
            val padding = dp(if (tiles) 4 else 14)
            setPadding(padding, padding, dp(if (tiles) 4 else 8), padding)
            if (tiles) minimumHeight = dp(88)
        }
        card.addView(row)
        val icon = ImageView(this).apply { setImageResource(R.drawable.ic_app); alpha = if (installed) 1f else .38f }
        value.icon?.let(icon::setImageBitmap)
        val iconFrame = FrameLayout(this).apply { addView(icon, FrameLayout.LayoutParams(-1, -1)) }
        row.addView(iconFrame, LinearLayout.LayoutParams(dp(48), dp(48)))
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; alpha = if (installed) 1f else .45f
            setPadding(if (tiles) 0 else dp(14), if (tiles) dp(4) else 0, if (tiles) 0 else dp(6), 0)
        }
        row.addView(labels, if (tiles) LinearLayout.LayoutParams(-1, -2) else LinearLayout.LayoutParams(0, -2, 1f))
        text(labels, if (tiles) app.shortName else app.name, if (tiles) 15 else 18).apply {
            setPadding(0, 0, 0, dp(3)); maxLines = 2; ellipsize = TextUtils.TruncateAt.END
            if (tiles) gravity = Gravity.CENTER
        }
        if (!tiles) text(labels, if (installed) app.baseUrl else getString(R.string.not_installed), 12).apply {
            setPadding(0, 0, 0, 0); maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        }
        val statusText = text(labels, "", 12).apply { setPadding(0, dp(4), 0, dp(2)) }
        val statusProgress = com.google.android.material.progressindicator.LinearProgressIndicator(this).apply {
            max = 100; visibility = View.GONE
            if (tiles) iconFrame.addView(this, FrameLayout.LayoutParams(-1, dp(4), Gravity.BOTTOM))
            else labels.addView(this, LinearLayout.LayoutParams(-1, dp(4)))
        }
        card.tag = RowStatus(app.key, app.name, tiles, card, statusText, statusProgress).also(::updateStatus)
        var anchor: View = card
        fun openMenu() {
            if (!busy) menu(value, if (anchor.isAttachedToWindow) anchor else toolbar)
        }
        if (!tiles) {
            val more = ImageButton(this).apply {
                setImageResource(R.drawable.ic_more); setBackgroundColor(Color.TRANSPARENT)
                contentDescription = "${getString(R.string.more)}: ${app.name}"
                setOnClickListener { openMenu() }
                setOnLongClickListener { openMenu(); true }
            }
            row.addView(more, LinearLayout.LayoutParams(dp(48), dp(48))); anchor = more
        }
        card.setOnLongClickListener { openMenu(); true }
        row.setOnLongClickListener { openMenu(); true }
        fun launch() {
            if (installed) packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                try {
                    if (Build.VERSION.SDK_INT >= 26) startActivity(it, android.app.ActivityOptions.makeBasic().apply {
                        launchDisplayId = list.display?.displayId ?: 0
                    }.toBundle()) else startActivity(it)
                } catch (error: Exception) { error(error) }
            }
        }
        row.setOnClickListener { launch() }; card.setOnClickListener { launch() }
        return card
    }
    private fun updateStatus(row: RowStatus) {
        val building = BuildJobs.forApp(row.key)
        val label = building?.let { BuildJobs.label(this, it) } ?: operationStatus[row.key]
            ?: if (row.key in queueKeys) getString(R.string.build_queued) else null
        row.text.text = label.orEmpty(); row.text.visibility = if (label == null || row.tile) View.GONE else View.VISIBLE
        if (row.tile) row.card.contentDescription = if (label == null) row.name else "${row.name}: $label"
        row.progress.contentDescription = label
        row.progress.visibility = View.INVISIBLE
        if (label != null) {
            row.progress.isIndeterminate = false
            row.progress.setProgressCompat(when {
                building?.queued == false -> building.progress.overallPercent
                building != null || row.key in queueKeys || operationStatus[row.key] == getString(R.string.build_queued) -> 0
                else -> 100
            }, false)
            row.progress.visibility = View.VISIBLE
        } else row.progress.visibility = View.GONE
    }
    private fun updateVisibleStatuses() {
        if (!::list.isInitialized) return
        for (index in 0 until list.childCount) {
            val holder = list.getChildAt(index) as? FrameLayout ?: continue
            (holder.getChildAt(0)?.tag as? RowStatus)?.let(::updateStatus)
        }
    }
    private fun showQueued(keys: List<String>) {
        operationStatus = operationStatus + keys.associateWith { getString(R.string.build_queued) }
        updateVisibleStatuses()
    }
    private fun menu(row: Row, anchor: View) {
        if (busy) return
        val menu = PopupMenu(this, anchor)
        val entry = row.entry
        val installed = row.installed
        // Icons and full manifest metadata can be large. Opening a menu needs only
        // the cached list entry; load the app after the user chooses an action.
        fun load(action: (WebApp) -> Unit) { work({ app(entry.key) }, action) }
        fun action(label: Int, enabled: Boolean = true, block: () -> Unit) {
            menu.menu.add(label).setEnabled(enabled).setOnMenuItemClickListener {
                try { block() } catch (error: Exception) { error(error) }
                true
            }
        }
        action(R.string.view_build_logs) { BuildLogActivity.open(this, entry.key) }
        action(R.string.reinstall, BuildJobs.forApp(entry.key) == null) {
            val previous = operationStatus[entry.key]
            showQueued(listOf(entry.key))
            work({ InstallCoordinator.prepare(this, app(entry.key), null) }, ::startInstall, { failure ->
                operationStatus = if (previous == null) operationStatus - entry.key else operationStatus + (entry.key to previous)
                updateVisibleStatuses(); error(failure)
            })
        }
        action(R.string.fetch_manifest, BuildJobs.forApp(entry.key) == null) { load(::fetch) }
        action(R.string.edit, BuildJobs.forApp(entry.key) == null) { startActivity(Intent(this, EditorActivity::class.java).putExtra("id", entry.key)) }
        action(R.string.open_temporary_browser, installed && BuildJobs.forApp(entry.key) == null) {
            work({
                val app = app(entry.key); ensureNoPending(app)
                val seed = java.io.File.createTempFile("browser-seed-", ".zip", cacheDir)
                try { Profiles.export(this, app.s("packageName"), seed); app to LoginSessions.begin(this, app.s("key"), seed) }
                finally { seed.delete() }
            }, { (app, token) ->
                loginApp = app.s("key"); loginSession = token
                startActivityForResult(LoginSessions.intent(this, app, token), 64)
            })
        }
        action(R.string.delete, BuildJobs.forApp(entry.key) == null) { load(::delete) }
        action(R.string.info, installed) { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${entry.packageName}"))) }
        action(R.string.export_data, installed) {
            MaterialAlertDialogBuilder(this).setMessage(R.string.data_notice).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.export_data) { _, _ -> load { document("export_data", it, true, "application/zip", "${entry.packageName}.zip") } }.show()
        }
        action(R.string.import_data, installed) {
            MaterialAlertDialogBuilder(this).setMessage(getString(R.string.import_data_question, entry.name)).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.import_data) { _, _ -> load { document("import_data", it, false, "application/zip", null) } }.show()
        }
        action(R.string.export_apk, BuildJobs.forApp(entry.key) == null) { load { document("export_apk", it, true, "application/vnd.android.package-archive", "${entry.packageName}.apk") } }
        menu.show()
    }
    private fun delete(app: WebApp) {
        val container = LinearLayout(this).apply { setPadding(dp(24), 0, dp(24), 0) }
        val uninstall = MaterialCheckBox(this).apply {
            setText(R.string.uninstall_too); isEnabled = Packages.installed(this@MainActivity, app.s("packageName"))
            setPadding(0, 0, 0, 0); container.addView(this)
        }
        MaterialAlertDialogBuilder(this).setTitle(R.string.delete).setMessage(getString(R.string.delete_question, app.name())).setView(container)
            .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.delete) { _, _ ->
                try {
                    ensureNoPending(app)
                    AppDatabase(this).use { it.remove(app) }
                    refresh()
                    if (uninstall.isChecked) Packages.uninstall(this, app.s("packageName"))
                } catch (error: Exception) { error(error) }
            }.show()
    }
    private fun ensureNoPending(app: WebApp) {
        check(BuildJobs.forApp(app.s("key")) == null) { getString(R.string.build_already_running) }
        InstallCoordinator.ensureEditable(this, app)
    }
    private fun fetch(app: WebApp) {
        try { ensureNoPending(app) } catch (error: Exception) { error(error); return }
        loginApp = app.s("key")
        val active = LoginSessions.current(this)
        if (active?.optString("token") != loginSession || active?.optString("appKey") != app.s("key")) loginSession = null
        fetchWork({ progress -> ManifestFetcher.fetch(app, loginSession?.let { ManifestFetcher.Session(this, it) } ?: ManifestFetcher.Session(), onProgress = progress).also { updated ->
            AppDatabase(this).use { it.save(updated) }
            loginSession?.let { LoginSessions.discard(this, it) }
        } }, {
            loginSession = null
            refresh(); toast(R.string.manifest_updated); inspectOperations()
        }, { failure ->
            if (failure is ManifestFetcher.CredentialsRequired) MaterialAlertDialogBuilder(this).setTitle(R.string.login_title).setMessage(if (failure is ManifestFetcher.ManifestNotFound) R.string.manifest_login_message else R.string.login_message)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton(R.string.login) { _, _ ->
                    work({ LoginSessions.begin(this, app.s("key")) }, { token ->
                        loginSession = token
                        startActivityForResult(LoginSessions.intent(this, app, token), 61)
                    })
                }.show()
            else error(failure)
        })
    }
    private fun document(action: String, app: WebApp?, create: Boolean, mime: String, filename: String?) {
        documentAction = action; documentApp = app?.s("key"); documentName = app?.name()
        val intent = Intent(if (create) Intent.ACTION_CREATE_DOCUMENT else Intent.ACTION_OPEN_DOCUMENT).setType(mime).addCategory(Intent.CATEGORY_OPENABLE)
        if (filename != null) intent.putExtra(Intent.EXTRA_TITLE, filename)
        try { startActivityForResult(intent, 60) } catch (error: Exception) { error(error) }
    }
    override fun onActivityResult(code: Int, result: Int, data: Intent?) {
        super.onActivityResult(code, result, data)
        if (code == 63) { refresh(); inspectOperations(); return }
        if (code == 64) {
            loginSession = data?.getStringExtra("session") ?: loginSession
            if (loginSession != null && loginApp != null && result == RESULT_OK) {
                confirmBrowserChanges = true; confirmTemporaryBrowser()
            } else discardTemporaryBrowser()
            return
        }
        if (code == 62) {
            val token = permissionOperation
            permissionOperation = null
            if (token != null) {
                if (Build.VERSION.SDK_INT < 26 || packageManager.canRequestPackageInstalls()) startInstall(token)
                else { InstallCoordinator.interrupt(this, token); renderContinue() }
            }
            return
        }
        if (code == 61) {
            if (result == RESULT_OK) {
                loginSession = data?.getStringExtra("session") ?: loginSession
                loginApp?.let { fetch(app(it)) }
            } else {
                loginSession?.let { token -> InstallerApplication.worker.execute { runCatching { LoginSessions.discard(this, token) } } }
                loginSession = null
            }
            return
        }
        if (result != RESULT_OK) return
        if (code != 60) return
        val uri = data?.data ?: return
        val action = documentAction ?: return
        val key = documentApp
        val name = documentName
        documentAction = null; documentApp = null; documentName = null
        if (action == "import_all") {
            work({
                val bytes = requireNotNull(contentResolver.openInputStream(uri)).use { ManifestFetcher.read(it, 32 * 1024 * 1024) }
                val metadata = JSONObject(bytes.toString(Charsets.UTF_8))
                require(metadata.getInt("format") == 1) { "Unsupported metadata format" }
                val apps = metadata.getJSONArray("apps"); require(apps.length() <= 10000)
                metadataImportFile = java.io.File.createTempFile("metadata-import-", ".json", cacheDir).apply { writeBytes(bytes) }
                metadataChecks = BooleanArray(apps.length()) { true }
                apps
            }, ::chooseMetadata)
            return
        }
        if (action == "export_apk") startBuild("export:${java.util.UUID.randomUUID()}", requireNotNull(key), requireNotNull(name)) { progress, log -> transferDocument(uri, action, key, progress, log) }
        else work({ transferDocument(uri, action, key) }, { refresh(); toast(it); inspectOperations() })
    }
    private fun chooseMetadata(apps: JSONArray) {
        val checks = metadataChecks?.takeIf { it.size == apps.length() } ?: BooleanArray(apps.length()) { true }.also { metadataChecks = it }
        val labels = (0 until apps.length()).map { index ->
            val app = apps.getJSONObject(index)
            "${WebApp.name(app)}\n${app.optString("packageName") }"
        }.toTypedArray()
        fun clear() { metadataImportFile?.delete(); metadataImportFile = null; metadataChecks = null }
        MaterialAlertDialogBuilder(this).setTitle(R.string.select_metadata_import)
            .setMultiChoiceItems(labels, checks) { _, index, checked -> checks[index] = checked }
            .setNegativeButton(android.R.string.cancel) { _, _ -> clear() }
            .setOnCancelListener { clear() }
            .setPositiveButton(R.string.import_all) { _, _ ->
                val selected = JSONArray((0 until apps.length()).filter { checks[it] }.map { apps.getJSONObject(it) })
                work({
                    AppDatabase(this).use { db ->
                        val existing = db.summaries().associateBy { it.packageName }
                        repeat(selected.length()) { index ->
                            val data = selected.getJSONObject(index)
                            existing[data.optString("packageName")]?.let { db.get(it.key)?.let { app -> ensureNoPending(app) } }
                        }
                        db.importAll(selected)
                    }
                }, { clear(); refresh(); toast(R.string.imported) }, { failure -> clear(); error(failure) })
            }.show()
    }
    private fun transferDocument(uri: Uri, action: String, key: String?, progress: (ApkProgress) -> Unit = {}, log: (String) -> Unit = {}): Int {
        val target = key?.let(::app)
        when (action) {
            "import_all" -> {
                val input = contentResolver.openInputStream(uri) ?: throw IOException("Cannot read file")
                val metadata = input.use { JSONObject(ManifestFetcher.read(it, 32 * 1024 * 1024).toString(Charsets.UTF_8)) }
                require(metadata.getInt("format") == 1) { "Unsupported metadata format" }
                check(InstallCoordinator.tokens(this).isEmpty()) { getString(R.string.pending_migration) }
                AppDatabase(this).use { it.importAll(metadata.getJSONArray("apps")) }
                return R.string.imported
            }
            "import_data" -> {
                requireNotNull(target)
                ensureNoPending(target)
                val input = contentResolver.openInputStream(uri) ?: throw IOException("Cannot read file")
                input.use { source ->
                    val output = contentResolver.openOutputStream(Packages.profile(target.s("packageName")), "w") ?: throw IOException("Cannot open profile")
                    output.use { DataArchive.copy(source, it) }
                }
                contentResolver.call(Packages.profile(target.s("packageName")), "commit", null, null)
                return R.string.imported
            }
            "export_all" -> {
                val apps = JSONArray().apply { AppDatabase(this@MainActivity).use { db -> db.all().forEach { put(it.data) } } }
                val bytes = JSONObject().put("format", 1).put("apps", apps).toString(2).toByteArray(Charsets.UTF_8)
                (contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot write file")).use { it.write(bytes) }
            }
            "export_data" -> {
                requireNotNull(target)
                val input = contentResolver.openInputStream(Packages.profile(target.s("packageName"))) ?: throw IOException("Cannot open profile")
                input.use { source -> (contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot write file")).use { DataArchive.copy(source, it) } }
            }
            "export_apk" -> {
                val apk = ApkBuilder.buildLogged(this, requireNotNull(target), progress, log)
                try { apk.inputStream().use { source -> (contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot write file")).use { DataArchive.copy(source, it) } } }
                finally { apk.delete() }
            }
        }
        return R.string.exported
    }
    private fun discardTemporaryBrowser() {
        val token = loginSession
        confirmBrowserChanges = false; loginSession = null; loginApp = null
        if (token != null) InstallerApplication.worker.execute { runCatching { LoginSessions.discard(this, token) } }
    }
    private fun confirmTemporaryBrowser() {
        if (browserChangesDialog?.isShowing == true) return
        val token = loginSession ?: return
        val key = loginApp ?: return
        browserChangesDialog = MaterialAlertDialogBuilder(this).setTitle(R.string.apply_browser_changes)
            .setMessage(R.string.apply_browser_changes_message)
            .setNegativeButton(android.R.string.cancel) { _, _ -> discardTemporaryBrowser() }
            .setOnCancelListener { discardTemporaryBrowser() }
            .setPositiveButton(R.string.save) { _, _ ->
                confirmBrowserChanges = false
                work({
                    val app = app(key); ensureNoPending(app)
                    val archive = LoginSessions.seal(this, token, key)
                    InstallCoordinator.importProfile(this, app, archive)
                }, { loginSession = null; loginApp = null; refresh(); inspectOperations() })
            }.create().also { it.show() }
    }
    private fun startInstall(token: String) {
        if (busy) return
        shown.add("$token:prepared")
        val shizuku = InstallationSettings.method(this) == "shizuku"
        if (shizuku) {
            if (Build.VERSION.SDK_INT < 24 || !ShizukuInstaller.ready()) { InstallCoordinator.interrupt(this, token); renderContinue(); error(IllegalStateException(getString(R.string.shizuku_not_ready))); return }
            if (!ShizukuInstaller.authorized()) {
                shizukuOperation = token
                rikka.shizuku.Shizuku.requestPermission(221)
                return
            }
        }
        if (!shizuku && Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
            MaterialAlertDialogBuilder(this).setTitle(R.string.install_permission).setMessage(R.string.install_permission_message)
                .setNegativeButton(android.R.string.cancel) { _, _ -> InstallCoordinator.interrupt(this, token); ReinstallQueue.pause(this, token); renderContinue() }
                .setOnCancelListener { InstallCoordinator.interrupt(this, token); ReinstallQueue.pause(this, token); renderContinue() }.setPositiveButton(R.string.continue_action) { _, _ ->
                    permissionOperation = token
                    startActivityForResult(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")), 62)
                }.show()
            return
        }
        if (BuildJobs.get("install:$token") != null || !starting.add(token)) return
        ProgressNotifications.requestPermission(this)
        val context = applicationContext
        InstallerApplication.readers.execute {
            try {
                InstallPipeline.start(context, token)
                runOnUiThread {
                    starting.remove(token)
                    if (!isFinishing && !isDestroyed) { renderBuild(); inspectOperations() }
                }
            } catch (failure: Exception) { runOnUiThread { starting.remove(token); if (!isFinishing && !isDestroyed) error(failure) } }
        }
    }
    private fun startBuild(id: String, key: String, name: String, task: ((ApkProgress) -> Unit, (String) -> Unit) -> Int?) {
        ProgressNotifications.requestPermission(this)
        try {
            if (!BuildJobs.start(applicationContext, id, key, name, task = task)) { toast(R.string.build_already_running); return }
            renderBuild()
        } catch (failure: Exception) { buildError(failure, key) }
    }
    private fun renderBuild() {
        buildHandler.removeCallbacks(buildPoll)
        val states = BuildJobs.all()
        var completed = false
        var failureShown = false
        states.forEach { (id, state) ->
            if (state.done && (state.error == null || hasWindowFocus() && !failureShown)) {
                BuildJobs.remove(id); completed = true
                if (state.error != null && (!id.startsWith("install:") || id.removePrefix("install:") !in InstallCoordinator.tokens(this))) {
                    failureShown = true; buildError(state.error, state.key)
                }
                state.notice?.let(::toast)
            }
        }
        if (renderedBuilds != states) { renderedBuilds = states; updateVisibleStatuses() }
        if (completed) { refresh(); inspectOperations() }
        if (BuildJobs.all().isNotEmpty()) buildHandler.postDelayed(buildPoll, 250)
    }
    private fun fillQueue() {
        if (ReinstallQueue.backgroundAllowed(this) && ReinstallQueue.has(this) && !ReinstallQueue.paused(this)) BuildService.start(this)
    }
    private fun continueQueue() {
        work({ ReinstallQueue.next(this) }, { token ->
            if (token != null) startInstall(token)
            fillQueue(); refresh(); inspectOperations()
        })
    }
    private fun inspectOperations(recoverConfirmation: Boolean = false) {
        if (isFinishing || isDestroyed) return
        recoverInspection = recoverInspection || recoverConfirmation
        if (!visible || !hasWindowFocus() || busy || inspecting) {
            pendingInspection = true
            if (visible && busy && !inspecting) { buildHandler.removeCallbacks(inspectionPoll); buildHandler.postDelayed(inspectionPoll, 100) }
            return
        }
        pendingInspection = false; inspecting = true
        val recover = recoverInspection; recoverInspection = false
        InstallerApplication.worker.execute {
            try {
                val plans = InstallCoordinator.tokens(this).map { it to InstallCoordinator.summary(this, it) }
                runOnUiThread {
                    inspecting = false
                    if (!isFinishing && !isDestroyed) {
                        if (visible && hasWindowFocus() && !busy) inspectPlans(plans, recover)
                        else { pendingInspection = true; recoverInspection = recoverInspection || recover }
                        if (pendingInspection && visible) inspectOperations()
                    }
                }
            } catch (failure: Exception) { runOnUiThread { inspecting = false; error(failure) } }
        }
    }
    private fun inspectPlans(plans: List<Pair<String, JSONObject>>, recoverConfirmation: Boolean) {
        if (busy || isFinishing || isDestroyed) return
        if (plans.any { it.second.optBoolean("background") && !it.second.optBoolean("interrupted") && it.second.getString("state") in listOf("prepared", "ready", "installed") })
            BuildService.start(this)
        operationStatus = plans.mapNotNull { (_, plan) ->
            val label = when (plan.getString("state")) {
                "prepared" -> R.string.build_queued
                "ready" -> R.string.build_ready
                "committed", "confirmation", "installed" -> R.string.install_pending
                else -> return@mapNotNull null
            }
            plan.getJSONObject("app").getString("key") to getString(label)
        }.toMap()
        updateVisibleStatuses()
        if (ReinstallQueue.has(this) && ReinstallQueue.active(this) == null && !ReinstallQueue.paused(this)) { continueQueue(); return }
        fillQueue()
        for ((token, plan) in plans) try {
            if (BuildJobs.get("install:$token") != null || token in starting) continue
            val state = plan.getString("state")
            if (plan.optBoolean("background") && !plan.optBoolean("interrupted") && state in listOf("prepared", "ready", "installed")) continue
            if (plan.optBoolean("startRequested") && plan.optBoolean("interrupted") && !recoverConfirmation && state in listOf("prepared", "ready")) continue
            if (state == "ready") {
                if (plan.optBoolean("startRequested") && !plan.optBoolean("background")) { startInstall(token); return }
                if (plan.optBoolean("interrupted")) { startInstall(token); return }
                val key = plan.getJSONObject("app").getString("key")
                val inQueue = key in ReinstallQueue.pendingKeys(this)
                if (!submittingInstall && (!inQueue || ReinstallQueue.active(this) == token) &&
                    plans.none { it.second.getString("state") in listOf("committed", "confirmation", "installed") }) {
                    submittingInstall = true
                    InstallerApplication.installEvents.execute {
                        runCatching { InstallCoordinator.installPrepared(applicationContext, token) }
                        runOnUiThread { submittingInstall = false; refresh(); inspectOperations() }
                    }
                    return
                }
                continue
            }
            // The system confirmation is normally already on screen. Offer recovery only on request.
            if (state == "confirmation" && !recoverConfirmation) continue
            if (state == "awaiting_uninstall" && !Packages.installed(this, plan.getString("oldPackage"))) { InstallCoordinator.discard(this, token); continue }
            if (state == "replacement_uninstall" && !Packages.installed(this, WebApp(plan.getJSONObject("app")).s("packageName"))) {
                InstallCoordinator.resumeReplacement(this, token)
                startInstall(token)
                return
            }
            if (state == "installed") { work({ InstallCoordinator.complete(this, token) }, { refresh(); inspectOperations() }); return }
            if (!shown.add("$token:$state")) continue
            when (state) {
                "signature_conflict" -> {
                    MaterialAlertDialogBuilder(this).setTitle(R.string.signature_conflict_title).setMessage(R.string.signature_conflict_message)
                        .setNegativeButton(if (plan.has("loginSnapshot")) R.string.later else android.R.string.cancel) { _, _ ->
                            if (plan.optBoolean("replacement") || plan.has("loginSnapshot")) { /* Keep an existing recovery backup. */ }
                            else { ReinstallQueue.pause(this, token, forgetOperation = true); InstallCoordinator.discard(this, token); refresh() }
                        }
                        .setPositiveButton(R.string.uninstall_reinstall) { _, _ ->
                            work({ InstallCoordinator.replaceSignature(this, token) }, { refresh(); inspectOperations() })
                        }.show()
                    return
                }
                "backup_failed" -> {
                    MaterialAlertDialogBuilder(this).setTitle(R.string.backup_failed_title)
                        .setMessage(getString(R.string.backup_failed_message) + "\n\n" + plan.optString("error"))
                        .setNegativeButton(if (plan.has("loginSnapshot")) R.string.later else android.R.string.cancel) { _, _ ->
                            if (!plan.has("loginSnapshot")) { ReinstallQueue.pause(this, token, forgetOperation = true); InstallCoordinator.discard(this, token); refresh() }
                        }
                        .setNeutralButton(R.string.without_data) { _, _ ->
                            MaterialAlertDialogBuilder(this).setMessage(R.string.without_data_warning)
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(R.string.uninstall_reinstall) { _, _ ->
                                    work({ InstallCoordinator.replaceSignature(this, token, withoutData = true) }, { refresh(); inspectOperations() })
                                }.show()
                        }
                        .setPositiveButton(R.string.retry) { _, _ ->
                            work({ InstallCoordinator.replaceSignature(this, token) }, { shown.remove("$token:backup_failed"); refresh(); inspectOperations() })
                        }.show()
                    return
                }
                "replacement_uninstall" -> {
                    MaterialAlertDialogBuilder(this).setTitle(R.string.uninstall_reinstall).setMessage(R.string.replacement_ready)
                        .setNegativeButton(R.string.later, null)
                        .setPositiveButton(R.string.uninstall) { _, _ ->
                            val name = plan.getJSONObject("app").getString("packageName")
                            if (InstallationSettings.method(this) == "shizuku") Packages.uninstall(this, name) { refresh(); inspectOperations() }
                            else startActivityForResult(Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:$name"))
                                .putExtra(Intent.EXTRA_RETURN_RESULT, true), 63)
                        }.show()
                    return
                }
                "replacement_failed" -> {
                    MaterialAlertDialogBuilder(this).setTitle(R.string.reinstall).setMessage(getString(R.string.replacement_failed) + "\n\n" + plan.optString("error"))
                        .setNeutralButton(R.string.view_build_logs) { _, _ -> BuildLogActivity.open(this, plan.getJSONObject("app").getString("key")) }
                        .setNegativeButton(R.string.later, null)
                        .setPositiveButton(R.string.retry) { _, _ -> InstallCoordinator.resumeReplacement(this, token); startInstall(token) }.show()
                    return
                }
                "confirmation" -> {
                    MaterialAlertDialogBuilder(this).setTitle(R.string.continue_action).setMessage(WebApp(plan.getJSONObject("app")).name())
                        .setNegativeButton(R.string.later, null)
                        .setPositiveButton(R.string.continue_action) { _, _ ->
                            startActivity(Intent.parseUri(plan.getString("confirmation"), Intent.URI_INTENT_SCHEME))
                        }.show()
                    return
                }
                "prepared" -> {
                    if (plan.optBoolean("startRequested") && !plan.optBoolean("interrupted")) { startInstall(token); return }
                    if (plan.getJSONObject("app").getString("key") in ReinstallQueue.pendingKeys(this) && !ReinstallQueue.paused(this)) { startInstall(token); return }
                    MaterialAlertDialogBuilder(this).setTitle(R.string.reinstall).setMessage(WebApp(plan.getJSONObject("app")).name())
                        .setNegativeButton(if (plan.optBoolean("replacement") || plan.has("loginSnapshot")) R.string.later else android.R.string.cancel) { _, _ ->
                            if (!plan.optBoolean("replacement") && !plan.has("loginSnapshot")) try { ReinstallQueue.pause(this, token, forgetOperation = true); InstallCoordinator.discard(this, token); refresh() } catch (error: Exception) { error(error) }
                        }
                        .setPositiveButton(R.string.continue_action) { _, _ -> startInstall(token) }.show()
                    return
                }
                "done" -> {
                    ReinstallQueue.complete(this, token)
                    if (!plan.optBoolean("completionNotified"))
                        Toast.makeText(applicationContext, if (plan.optBoolean("profileOnly")) getString(R.string.imported) else getString(R.string.install_success, WebApp.name(plan.getJSONObject("app"))), Toast.LENGTH_SHORT).show()
                    InstallCoordinator.discard(this, token); refresh()
                    if (ReinstallQueue.has(this) && !ReinstallQueue.paused(this)) { continueQueue(); return }
                }
                "failed" -> {
                    ReinstallQueue.pause(this, token, forgetOperation = true)
                    pendingInspection = true
                    buildError(IOException(getString(R.string.install_failed, plan.optString("error"))), plan.getJSONObject("app").getString("key"))
                    InstallCoordinator.discard(this, token); refresh(); return
                }
                "transfer_failed" -> {
                    MaterialAlertDialogBuilder(this).setTitle(R.string.migrate_title).setMessage(getString(R.string.migration_failed) + "\n\n" + plan.optString("error"))
                        .setNegativeButton(R.string.later, null).setPositiveButton(R.string.retry) { _, _ ->
                            work({ InstallCoordinator.complete(this, token) }, { shown.remove("$token:transfer_failed"); refresh(); inspectOperations() })
                        }.show()
                    return
                }
                "awaiting_uninstall" -> {
                    MaterialAlertDialogBuilder(this).setMessage(R.string.migration_ready).setNegativeButton(R.string.later, null)
                        .setPositiveButton(R.string.uninstall) { _, _ -> try { Packages.uninstall(this, plan.getString("oldPackage")) } catch (error: Exception) { error(error) } }.show()
                    return
                }
            }
        } catch (error: Exception) { error(error); return }
    }
    override fun onSaveInstanceState(out: Bundle) {
        out.putString("shizukuOperation", shizukuOperation)
        out.putString("documentAction", documentAction); out.putString("documentApp", documentApp); out.putString("documentName", documentName)
        out.putBoolean("confirmBrowserChanges", confirmBrowserChanges)
        out.putString("loginSession", loginSession)
        out.putString("loginApp", loginApp); out.putString("permissionOperation", permissionOperation)
        out.putString("metadataImportFile", metadataImportFile?.absolutePath); out.putBooleanArray("metadataChecks", metadataChecks)
        super.onSaveInstanceState(out)
    }
}
