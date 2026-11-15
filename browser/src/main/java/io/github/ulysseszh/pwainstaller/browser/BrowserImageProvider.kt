package io.github.ulysseszh.pwainstaller.browser

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.FileNotFoundException

/** Only explicitly copied/shared images are exposed, with temporary URI grants. */
class BrowserImageProvider : androidx.core.content.FileProvider() {
	override fun getType(uri: Uri): String? = if (BrowserImageCopies.owns(uri)) BrowserImageCopies.mime(requireNotNull(context), uri)
		else super.getType(uri)
	override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
		if (!BrowserImageCopies.owns(uri)) return super.query(uri, projection, selection, selectionArgs, sortOrder)
		val file = BrowserImageCopies.ready(requireNotNull(context), uri)
		val columns = (projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
			.filter { it == OpenableColumns.DISPLAY_NAME || it == OpenableColumns.SIZE }.toTypedArray()
		val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(getType(uri).orEmpty())
		return MatrixCursor(columns, 1).apply { addRow(columns.map {
			if (it == OpenableColumns.SIZE) file?.length() else extension?.let { "image.$it" } ?: "image"
		}.toTypedArray()) }
	}
	override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
		if (!BrowserImageCopies.owns(uri)) return super.openFile(uri, mode)
		if (mode != "r") throw FileNotFoundException("Clipboard images are read-only")
		// Reading may wait for bytes; metadata and setting the clipboard never do.
		return BrowserImageCopies.open(requireNotNull(context), uri)
	}
	override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
		if (BrowserImageCopies.owns(uri)) throw UnsupportedOperationException("Clipboard images are read-only")
		return super.delete(uri, selection, selectionArgs)
	}
}
