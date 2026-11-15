package io.github.ulysseszh.pwainstaller

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import io.github.ulysseszh.pwainstaller.core.PackageNames
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID

class SettingsActivity : Screen() {
    private var settings: JSONObject? = null
    private var selected: File? = null
    private var exportKeystore: String? = null
    private lateinit var filterUrls: TextInputEditText
    private lateinit var updateInterval: SelectionInput
    private lateinit var installationMethod: SelectionInput
    private lateinit var parallelBuilders: SelectionInput
    private lateinit var libraryLayout: SelectionInput
    private val filterHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val applyFilterChanges = Runnable { applyFilters() }
    private val applySigningImport = Runnable { importSigningKey() }
    private lateinit var signingStatus: TextView
    private lateinit var filterStatus: TextView
    private lateinit var current: TextView
    private lateinit var fingerprint: TextView
    private lateinit var type: TextInputEditText
    private lateinit var alias: TextInputEditText
    private lateinit var storePassword: TextInputEditText
    private lateinit var keyPassword: TextInputEditText

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        title(R.string.settings, true)
        val form = scrollBody()
        text(form, getString(R.string.installation_method), 20)
        installationMethod = SelectionInput(this, R.string.installation_method,
            listOf("system" to getString(R.string.installer_system)) +
                if (android.os.Build.VERSION.SDK_INT >= 24) listOf("shizuku" to getString(R.string.installer_shizuku)) else emptyList()).apply {
            value = state?.getString("installationMethod") ?: InstallationSettings.method(this@SettingsActivity)
            form.addView(this)
        }
        text(form, getString(R.string.shizuku_intro), 14)
        installationMethod.onValueChanged = { value ->
            try {
                InstallationSettings.save(this, value)
                if (value == "shizuku" && android.os.Build.VERSION.SDK_INT >= 24) {
                    check(ShizukuInstaller.ready()) { getString(R.string.shizuku_not_ready) }
                    if (!ShizukuInstaller.authorized()) rikka.shizuku.Shizuku.requestPermission(223)
                }
            } catch (failure: Exception) { error(failure) }
        }
        parallelBuilders = SelectionInput(this, R.string.parallel_builders,
            (1..BuildSettings.MAX_BUILDERS).map { it.toString() to it.toString() }).apply {
            value = BuildSettings.builders(this@SettingsActivity).toString()
            onValueChanged = { BuildSettings.save(this@SettingsActivity, it.toInt()) }
            form.addView(this)
        }
        text(form, getString(R.string.parallel_builders_hint), 14)
        libraryLayout = SelectionInput(this, R.string.library_layout, listOf("list" to getString(R.string.layout_list), "tiles" to getString(R.string.layout_tiles))).apply {
            value = AppearanceSettings.layout(this@SettingsActivity)
            onValueChanged = { AppearanceSettings.save(this@SettingsActivity, it) }
            form.addView(this)
        }
        text(form, getString(R.string.adblock), 20)
        text(form, getString(R.string.adblock_global_hint), 14)
        filterUrls = field(form, R.string.filter_urls, state?.getString("filterUrls") ?: AdblockSettings.urls(this), true)
        text(form, getString(R.string.filter_urls_hint), 14)
        updateInterval = SelectionInput(this, R.string.filter_interval, listOf(
            "0" to getString(R.string.filter_manual), "24" to getString(R.string.filter_daily),
            "168" to getString(R.string.filter_weekly), "720" to getString(R.string.filter_monthly))).apply {
            value = state?.getString("updateInterval") ?: AdblockSettings.hours(this@SettingsActivity).toString()
            form.addView(this)
        }
        filterStatus = text(form, "", 14)
        showFilterStatus()
        androidx.work.WorkManager.getInstance(this).getWorkInfosByTagLiveData(AdblockSettings.FETCH_TAG).observe(this) { jobs ->
            val running = jobs.firstOrNull { it.state == androidx.work.WorkInfo.State.RUNNING }
            // Periodic work remains enqueued until its next scheduled run, even after
            // a successful download. Only an immediate update should appear queued.
            val queued = jobs.any { !it.state.isFinished && AdblockSettings.FETCH_WORK in it.tags }
            if (running != null) {
                val message = running.progress.getString("text") ?: getString(R.string.filters_fetching)
                val percent = running.progress.getInt("percent", -1)
                filterStatus.text = message + if (percent >= 0) " · $percent%" else ""
            } else if (queued) filterStatus.setText(R.string.filters_queued)
            else showFilterStatus()
        }
        updateInterval.onValueChanged = { applyFilters() }
        filterUrls.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterHandler.removeCallbacks(applyFilterChanges); filterHandler.postDelayed(applyFilterChanges, 500)
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        button(form, R.string.refresh_filters) { saveFilters(true) }
        text(form, getString(R.string.user_content), 20)
        button(form, R.string.manage_user_content) { startActivity(Intent(this, UserContentActivity::class.java)) }
        text(form, getString(R.string.full_backup), 20)
        text(form, getString(R.string.full_backup_hint), 14)
        button(form, R.string.export_full_backup) {
            if (!busy) startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/zip")
                .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "pwa-installer-backup.zip"), 56)
        }
        button(form, R.string.import_full_backup) {
            if (!busy) startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE), 57)
        }
        text(form, getString(R.string.signing_key), 20)
        text(form, getString(R.string.signing_intro), 15)
        current = text(form, getString(R.string.loading_signing_key), 14); fingerprint = text(form, "", 13)
        type = field(form, R.string.keystore_type, "PKCS12", false)
        alias = field(form, R.string.key_alias, "", false)
        storePassword = passwordField(form, R.string.store_password)
        keyPassword = passwordField(form, R.string.key_password)
        text(form, getString(R.string.keystore_import_hint), 14)
        signingStatus = text(form, "", 14)
        button(form, R.string.choose_keystore) {
            if (!busy) com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.replace_signing_key).setMessage(R.string.replace_signing_key_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.choose_keystore) { _, _ ->
                    startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE), 51)
                }.show()
        }
        button(form, R.string.export_keystore) {
            if (!busy) try {
                val config = settings ?: SigningKeys.settings(this)
                exportKeystore = config.getString("file")
                val extension = when (config.getString("type")) { "PKCS12" -> "p12"; "BKS" -> "bks"; else -> "jks" }
                startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/octet-stream")
                    .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "pwa-signing.$extension"), 52)
            } catch (failure: Exception) { error(failure) }
        }
        text(form, getString(R.string.export_keystore_hint), 14)
        if (state != null) {
            exportKeystore = state.getString("exportKeystore")
            settings = JSONObject(state.getString("settings", "{}")).takeIf { it.has("file") }
            selected = state.getString("selected")?.let(::File)
            type.setText(state.getString("type")); alias.setText(state.getString("alias"))
            storePassword.setText(state.getString("storePassword")); keyPassword.setText(state.getString("keyPassword"))
            fingerprint.text = state.getString("fingerprint").orEmpty()
            if (settings != null) showFile()
        }
        if (settings == null) loadSigningKey()
        val credentialsChanged = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (selected != null) {
                    filterHandler.removeCallbacks(applySigningImport)
                    filterHandler.postDelayed(applySigningImport, 700)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        }
        signingFields().forEach { it.addTextChangedListener(credentialsChanged) }
        if (selected != null) filterHandler.post(applySigningImport)
    }
    private fun loadSigningKey() {
        current.setText(R.string.loading_signing_key)
        work({
            SigningKeys.settings(this) to PackageNames.digest("SHA-256", SigningKeys.load(this).certificates.first().encoded)
        }, { (config, hash) ->
            settings = config
            type.setText(config.getString("type")); alias.setText(config.getString("alias"))
            storePassword.setText(config.getString("storePassword")); keyPassword.setText(config.getString("keyPassword"))
            fingerprint.text = getString(R.string.signature_fingerprint, hash)
            showFile()
        }, { failure -> current.setText(R.string.loading_signing_key_failed); error(failure) })
    }
    private fun passwordField(form: android.widget.LinearLayout, label: Int): TextInputEditText = field(form, label, "", false).apply {
        inputType = 129
        var ancestor: android.view.ViewParent? = parent
        while (ancestor != null && ancestor !is com.google.android.material.textfield.TextInputLayout) ancestor = ancestor.parent
        (ancestor as? com.google.android.material.textfield.TextInputLayout)?.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
    }
    private fun showFilterStatus() {
        val updated = AdblockSettings.updated(this)
        filterStatus.text = if (updated == 0L) getString(R.string.filters_not_fetched)
            else getString(R.string.filters_updated, java.text.DateFormat.getDateTimeInstance().format(java.util.Date(updated)))
    }
    private fun saveFilters(refresh: Boolean) {
        filterHandler.removeCallbacks(applyFilterChanges)
        if (!applyFilters()) return
        if (refresh) {
            ProgressNotifications.requestPermission(this)
            AdblockSettings.fetchNow(this)
            toast(R.string.filters_queued)
        }
    }
    private fun applyFilters(): Boolean = try {
        AdblockSettings.save(this, filterUrls.text.toString(), updateInterval.value.toLong())
        filterUrls.error = null
        true
    } catch (failure: Exception) { filterUrls.error = failure.message; false }
    override fun onStop() {
        filterHandler.removeCallbacks(applyFilterChanges)
        if (::filterUrls.isInitialized) applyFilters()
        super.onStop()
    }
    private fun showFile() { current.text = getString(R.string.key_current, settings?.optString("file").orEmpty()) }
    private fun signingFields() = listOf(type, alias, storePassword, keyPassword)
    private fun importSigningKey() {
        val candidate = selected ?: return
        if (busy) { filterHandler.postDelayed(applySigningImport, 300); return }
        filterHandler.removeCallbacks(applySigningImport)
        val storeType = type.text.toString().trim().uppercase(Locale.ROOT)
        val keyAlias = alias.text.toString().trim()
        val storePass = storePassword.text.toString()
        val keyPass = keyPassword.text.toString()
        signingFields().forEach { it.isEnabled = false }
        signingStatus.setText(R.string.importing_keystore)
        work({
            require(candidate.isFile) { "Choose a keystore first" }
            require(storeType in listOf("PKCS12", "JKS", "BKS")) { "Use PKCS12, JKS, or BKS" }
            val identity = SigningKeys.load(candidate, storeType, keyAlias, storePass, keyPass)
            val destination = File(filesDir, "custom-${UUID.randomUUID()}.store")
            val config = JSONObject().put("file", destination.name).put("type", storeType).put("alias", keyAlias)
                .put("storePassword", storePass).put("keyPassword", keyPass)
            val hash = PackageNames.digest("SHA-256", identity.certificates.first().encoded)
            try {
                candidate.copyTo(destination)
                SigningKeys.save(this, config)
            } catch (failure: Exception) { destination.delete(); throw failure }
            config to hash
        }, { (config, hash) ->
            settings = config; selected = null; candidate.delete()
            signingFields().forEach { it.isEnabled = true }
            signingStatus.text = ""
            fingerprint.text = getString(R.string.signature_fingerprint, hash)
            showFile(); toast(R.string.signing_saved)
        }, { failure ->
            signingFields().forEach { it.isEnabled = true }
            signingStatus.text = getString(R.string.keystore_import_failed, failure.message ?: failure.toString())
        })
    }
    override fun onActivityResult(code: Int, result: Int, data: Intent?) {
        super.onActivityResult(code, result, data)
        if (code in listOf(56, 57)) {
            val uri = data?.data
            if (result != RESULT_OK || uri == null) return
            if (code == 56) work({
                requireNotNull(contentResolver.openOutputStream(uri, "wt")).use { FullBackup.export(this, it) }
            }, { toast(R.string.exported) })
            else work({ requireNotNull(contentResolver.openInputStream(uri)).use { FullBackup.restore(this, it) } }, { pending ->
                filterUrls.setText(AdblockSettings.urls(this)); updateInterval.value = AdblockSettings.hours(this).toString()
                libraryLayout.value = AppearanceSettings.layout(this); installationMethod.value = InstallationSettings.method(this)
                parallelBuilders.value = BuildSettings.builders(this).toString()
                settings = null; selected?.delete(); selected = null; loadSigningKey(); showFilterStatus()
                if (pending > 0) com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setMessage(getString(R.string.backup_pending_profiles, pending)).setPositiveButton(android.R.string.ok, null).show()
                else toast(R.string.imported)
            })
            return
        }
        if (code == 52) {
            val filename = exportKeystore; exportKeystore = null
            if (result == RESULT_OK && filename != null && data?.data != null) work({
                val file = File(filesDir, filename)
                check(file.canonicalFile.parentFile == filesDir.canonicalFile && file.isFile)
                val output = contentResolver.openOutputStream(data.data!!, "wt") ?: throw IOException("Cannot export keystore")
                output.use { destination -> file.inputStream().use { it.copyTo(destination) } }
            }, { toast(R.string.exported) })
            return
        }
        if (code != 51 || result != RESULT_OK) return
        val uri = data?.data ?: return
        work({
            val file = File.createTempFile("keystore-", ".import", cacheDir)
            val input = contentResolver.openInputStream(uri) ?: throw IOException("Cannot read keystore")
            input.use { file.writeBytes(ManifestFetcher.read(it, 16 * 1024 * 1024)) }
            file
        }, { file -> selected?.delete(); selected = file; showFile(); importSigningKey() })
    }
    override fun onSaveInstanceState(out: Bundle) {
        out.putString("fingerprint", fingerprint.text.toString())
        out.putString("exportKeystore", exportKeystore)
        out.putString("filterUrls", filterUrls.text.toString())
        out.putString("updateInterval", updateInterval.value)
        out.putString("installationMethod", installationMethod.value)
        out.putString("settings", settings?.toString() ?: "{}"); out.putString("selected", selected?.absolutePath)
        out.putString("type", type.text.toString()); out.putString("alias", alias.text.toString())
        out.putString("storePassword", storePassword.text.toString()); out.putString("keyPassword", keyPassword.text.toString())
        super.onSaveInstanceState(out)
    }
    override fun onDestroy() {
        filterHandler.removeCallbacks(applySigningImport)
        if (isFinishing && !busy) selected?.delete()
        super.onDestroy()
    }
}
