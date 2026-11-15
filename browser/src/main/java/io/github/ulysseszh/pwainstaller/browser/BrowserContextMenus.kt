package io.github.ulysseszh.pwainstaller.browser

import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

internal class BrowserContextMenus(
    private val activity: Activity,
    private val open: (WebView, String) -> Unit,
    private val external: (Uri) -> Unit,
    private val headers: (String) -> Map<String, String>,
    private val createMenu: (String, Array<String>, (Int) -> Unit) -> Dialog,
    private val error: (Exception) -> Unit
) {
    private val worker = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var dialog: Dialog? = null
    private var generation = 0
    private var transferring = false
    private val pendingCopies = mutableSetOf<Uri>()
    private var pendingSave: File? = null
    private fun alive() = !activity.isFinishing && !activity.isDestroyed
    private fun web(url: String) = Uri.parse(url).scheme?.lowercase() in listOf("http", "https")
    private fun image(url: String) = web(url) || url.startsWith("data:image/", true) || url.startsWith("blob:", true)

    @android.annotation.SuppressLint("ClickableViewAccessibility") // Observe touches; WebView still handles every gesture.
    fun attach(view: WebView) {
        var touch = 0
        var downX = 0f
        var downY = 0f
        var dom: BrowserContextHit? = null
        val slop = ViewConfiguration.get(activity).scaledTouchSlop
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val token = ++touch
                    val page = view.url
                    val menuGeneration = generation
                    downX = event.x; downY = event.y; dom = null
                    // Resolve while the finger is held down, so showing the menu does
                    // not wait for JavaScript. No bridge or page event interception.
                    BrowserContextHit.read(view, downX, downY) { result ->
                        if (alive() && touch == token && generation == menuGeneration && view.url == page) dom = result
                    }
                }
                MotionEvent.ACTION_MOVE -> if (kotlin.math.abs(event.x - downX) > slop || kotlin.math.abs(event.y - downY) > slop) { touch++; dom = null }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { touch++; dom = null }
            }
            false
        }
        view.setOnLongClickListener {
            val hit = view.hitTestResult ?: return@setOnLongClickListener false
            val target = dom
            if (hit.type == WebView.HitTestResult.EDIT_TEXT_TYPE || target?.editable == true) return@setOnLongClickListener false
            val isImage = hit.type == WebView.HitTestResult.IMAGE_TYPE || hit.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
            val isLink = hit.type == WebView.HitTestResult.SRC_ANCHOR_TYPE || hit.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
            val special = when (hit.type) {
                WebView.HitTestResult.EMAIL_TYPE -> "mailto:${hit.extra}"
                WebView.HitTestResult.PHONE_TYPE -> "tel:${hit.extra}"
                WebView.HitTestResult.GEO_TYPE -> "geo:0,0?q=${Uri.encode(hit.extra)}"
                else -> null
            }
            if (!isImage && !isLink && special == null) {
                if (target?.link == null && target?.source?.let(::image) != true) return@setOnLongClickListener false
                generation++
                show(view, target?.link, target?.source, target?.text)
                return@setOnLongClickListener true
            }
            val token = ++generation
            val page = view.url
            val fallback = hit.extra
            val callback = Handler(Looper.getMainLooper()) { message ->
                if (alive() && view.isAttachedToWindow && generation == token && view.url == page) {
                    val link = target?.link ?: message.data.getString("url")?.takeIf(String::isNotBlank) ?: special ?: fallback?.takeIf { isLink && !isImage }
                    val source = target?.source ?: message.data.getString("src")?.takeIf(String::isNotBlank) ?: fallback?.takeIf { isImage }
                    show(view, link, source, target?.text ?: message.data.getString("title"))
                }
                true
            }
            view.requestFocusNodeHref(Message.obtain(callback))
            true
        }
    }
    private fun show(view: WebView, link: String?, source: String?, title: String?) {
        val actions = mutableListOf<Pair<Int, () -> Unit>>()
        fun add(label: Int, action: () -> Unit) { actions.add(label to action) }
        if (!link.isNullOrBlank()) {
            val scheme = Uri.parse(link).scheme?.lowercase()
            if (scheme in listOf("http", "https", "mailto", "tel", "geo")) add(R.string.context_open_link) { open(view, link) }
            if (web(link)) add(R.string.context_open_browser) { external(Uri.parse(link)) }
            add(R.string.context_copy_link) { copyText(link) }
            title?.trim()?.takeIf(String::isNotEmpty)?.let { text -> add(R.string.context_copy_link_text) { copyText(text) } }
            add(R.string.context_share_link) {
                activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, link), activity.getString(R.string.context_share_link)))
            }
        }
        if (!source.isNullOrBlank() && image(source)) {
            add(R.string.context_open_image) { open(view, source) }
            add(R.string.context_copy_image) { copyImage(view, source) }
            add(R.string.context_copy_image_address) { copyText(source) }
            add(R.string.context_download_image) { transfer(view, source, "save") }
            add(R.string.context_share_image) { transfer(view, source, "share") }
        }
        if (actions.isEmpty()) return
        val heading = link ?: source?.takeIf(::web) ?: activity.getString(R.string.context_image)
        dialog?.dismiss()
        dialog = createMenu(heading.take(300), actions.map { activity.getString(it.first) }.toTypedArray()) { index ->
            try { actions[index].second() } catch (failure: Exception) { error(failure) }
        }.also { it.show() }
    }
    private fun clipboard() = activity.getSystemService(Activity.CLIPBOARD_SERVICE) as ClipboardManager
    private fun copied() {
        if (android.os.Build.VERSION.SDK_INT < 33) Toast.makeText(activity, R.string.context_copied, Toast.LENGTH_SHORT).show()
    }
    private fun copyText(value: String) { clipboard().setPrimaryClip(ClipData.newPlainText("", value)); copied() }
    private fun copyImage(view: WebView, source: String) {
        val page = view.url.orEmpty().takeIf(::web) ?: source
        val agent = view.settings.userAgentString
        val requestHeaders = headers(page)
        val uri = BrowserImageCopies.create(activity, source)
        // Use a known image type directly. ClipData.newUri would synchronously ask
        // the provider for metadata before publishing this not-yet-loaded image.
        clipboard().setPrimaryClip(ClipData(activity.getString(R.string.context_image), arrayOf(
            uri.getQueryParameter("mime") ?: "image/*"), ClipData.Item(uri)))
        copied()
        fun read(url: String) {
            pendingCopies.remove(uri)
            BrowserImageCopies.load(activity, uri, url, page, agent, requestHeaders) { failure ->
                handler.post { if (alive()) error(failure) }
            }
        }
        if (source.startsWith("blob:", true)) {
            pendingCopies.add(uri)
            readBlob(view, source, ::read) { failure ->
                pendingCopies.remove(uri); BrowserImageCopies.failAsync(activity, uri)
                if (alive()) error(failure)
            }
        } else read(source)
    }
    private fun transfer(view: WebView, source: String, action: String) {
        if (transferring) return
        val page = view.url.orEmpty().takeIf(::web) ?: source
        val agent = view.settings.userAgentString
        val requestHeaders = headers(page)
        transferring = true
        Toast.makeText(activity, R.string.context_preparing_image, Toast.LENGTH_SHORT).show()
        fun read(url: String) {
            worker.execute {
                try {
                    val result = BrowserImages.read(activity, url, page, agent, requestHeaders)
                    handler.post {
                        transferring = false
                        if (alive()) try { complete(result, action, source) } catch (failure: Exception) { error(failure) }
                    }
                } catch (failure: Exception) { handler.post { transferring = false; if (alive()) error(failure) } }
            }
        }
        if (source.startsWith("blob:", true)) readBlob(view, source, ::read) else read(source)
    }
    private fun readBlob(view: WebView, source: String, ready: (String) -> Unit,
        failed: (Exception) -> Unit = { transferring = false; if (alive()) error(it) }) {
        val key = JSONObject.quote("__pwaImage${UUID.randomUUID().toString().replace("-", "")}")
        val page = view.url
        val started = android.os.SystemClock.uptimeMillis()
        fun clean() { if (alive() && view.url == page) view.evaluateJavascript("delete window[$key]", null) }
        fun fail(failure: Exception) { clean(); failed(failure) }
        fun poll() {
            if (!alive()) return
            if (view.url != page || android.os.SystemClock.uptimeMillis() - started > 20000) {
                fail(java.io.IOException(activity.getString(R.string.context_image_unavailable))); return
            }
            view.evaluateJavascript("JSON.stringify(window[$key]||null)") { encoded ->
                if (!alive()) return@evaluateJavascript
                worker.execute {
                    try {
                        val result = (JSONTokener(encoded).nextValue() as? String)?.let(::JSONObject)
                            ?: throw java.io.IOException(activity.getString(R.string.context_image_unavailable))
                        if (result.has("error")) throw java.io.IOException(result.getString("error"))
                        val data = result.optString("data")
                        handler.post {
                            if (alive()) {
                                if (data.isNotEmpty()) { clean(); ready(data) }
                                else handler.postDelayed({ poll() }, 100)
                            }
                        }
                    } catch (failure: Exception) { handler.post { fail(failure) } }
                }
            }
        }
        // No JavaScript-to-native bridge: read this user-selected blob through a bounded,
        // temporary result, and remove it after transfer or navigation.
        view.evaluateJavascript("""(function(){window[$key]={};try{var x=new XMLHttpRequest();x.open('GET',${JSONObject.quote(source)},true);x.responseType='blob';x.onload=function(){
            var b=x.response;if(!b||b.size>${BrowserImages.LIMIT}){window[$key]={error:'Invalid image size (maximum 16 MiB)'};return}var r=new FileReader();
            r.onload=function(){window[$key]={data:r.result}};r.onerror=function(){window[$key]={error:'Image unavailable'}};r.readAsDataURL(b);
            };x.onerror=function(){window[$key]={error:'Image unavailable'}};x.send()}catch(e){window[$key]={error:String(e)}}})()""".trimIndent()) { poll() }
    }
    private fun complete(image: BrowserImages.Image, action: String, source: String) {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.images", image.file)
        when (action) {
            "share" -> activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(image.mime)
                .putExtra(Intent.EXTRA_STREAM, uri).apply { clipData = ClipData.newUri(activity.contentResolver, "", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, activity.getString(R.string.context_share_image)))
            "save" -> {
                pendingSave = image.file
                activity.startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType(image.mime).addCategory(Intent.CATEGORY_OPENABLE)
                    .putExtra(Intent.EXTRA_TITLE, if (web(source)) android.webkit.URLUtil.guessFileName(source, null, image.mime) else "image.${image.file.extension}"), SAVE_IMAGE)
            }
        }
    }
    fun activityResult(code: Int, result: Int, data: Intent?) {
        if (code != SAVE_IMAGE) return
        val file = pendingSave; pendingSave = null
        val uri = data?.data
        if (file == null || uri == null || result != Activity.RESULT_OK) return
        worker.execute {
            try {
                file.inputStream().use { input ->
                    (activity.contentResolver.openOutputStream(uri, "wt") ?: throw java.io.IOException("Cannot save image")).use { input.copyTo(it) }
                }
                handler.post { if (alive()) Toast.makeText(activity, R.string.context_image_saved, Toast.LENGTH_SHORT).show() }
            } catch (failure: Exception) { handler.post { if (alive()) error(failure) } }
        }
    }
    fun restore(state: Bundle?) {
        state?.getString("contextImageSave")?.takeIf { it.matches(Regex("[0-9a-f-]{36}\\.[a-zA-Z0-9]+")) }
            ?.let { pendingSave = File(activity.cacheDir, "browser-images/$it").takeIf(File::isFile) }
    }
    fun save(state: Bundle) { state.putString("contextImageSave", pendingSave?.name) }
    fun dismiss() { generation++; dialog?.dismiss(); dialog = null }
    fun destroy() {
        dismiss(); handler.removeCallbacksAndMessages(null); worker.shutdownNow()
        pendingCopies.toList().forEach { uri -> BrowserImageCopies.failAsync(activity, uri) }
        pendingCopies.clear()
    }
    // BrowserActivity uses 75 for saving the current page.
    companion object { private const val SAVE_IMAGE = 76 }
}
