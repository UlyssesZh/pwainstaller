package io.github.ulysseszh.pwainstaller.runtime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.content.pm.PackageManager
import android.provider.OpenableColumns
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale

object IncomingFiles {
    fun read(context: Context, intent: Intent): JSONArray {
        val uris = buildList {
            if (intent.action == Intent.ACTION_VIEW) intent.data?.let(::add)
            if (intent.action == Intent.ACTION_SEND_MULTIPLE) intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(::addAll)
            else intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(::add)
            intent.clipData?.let { clip -> repeat(clip.itemCount) { clip.getItemAt(it).uri?.let(::add) } }
        }.distinct()
        require(uris.size <= 16) { "Too many files" }
        var total = 0
        return JSONArray().apply {
            uris.forEach { uri ->
                require(uri.scheme == "content") { "Unsupported file URI" }
                require(uri.authority != "${context.packageName}.data") { "Private profile is not a shared file" }
                require(context.checkUriPermission(uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
                    "A read grant is required for shared files"
                }
                var name = uri.lastPathSegment.orEmpty().substringAfterLast('/')
                var type = intent.type ?: "application/octet-stream"
                if (uri.scheme == "content") {
                    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) name = cursor.getString(0).orEmpty()
                    }
                    type = context.contentResolver.getType(uri) ?: type
                }
                name = name.substringAfterLast('/').substringAfterLast('\\').filter { it >= ' ' }.take(255).ifBlank { "file" }
                val output = ByteArrayOutputStream()
                requireNotNull(context.contentResolver.openInputStream(uri)) { "Cannot open shared file" }.use { input ->
                    val buffer = ByteArray(16384)
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break
                        total += count
                        require(total <= 16 * 1024 * 1024) { "Shared files exceed 16 MiB" }
                        output.write(buffer, 0, count)
                    }
                }
                put(JSONObject().put("name", name).put("type", type).put("data", Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)))
            }
        }
    }
    fun matches(accept: String, file: JSONObject): Boolean {
        val pattern = accept.lowercase(Locale.ROOT)
        val type = file.getString("type").substringBefore(';').lowercase(Locale.ROOT)
        return if (pattern.startsWith('.')) file.getString("name").lowercase(Locale.ROOT).endsWith(pattern)
        else pattern == "*/*" || pattern == type || pattern.endsWith("/*") && type.startsWith(pattern.substringBefore('/') + "/")
    }
}
