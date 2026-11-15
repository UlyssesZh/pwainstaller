package io.github.ulysseszh.pwainstaller

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.github.ulysseszh.pwainstaller.core.AdblockRules
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class AppDatabase(context: Context, name: String = "webapps.db") : SQLiteOpenHelper(context, name, null, 5), java.io.Closeable {
	data class Summary(val key: String, val packageName: String, val name: String, val baseUrl: String, val thumbnail: ByteArray?, val shortName: String = name)
	// SQLiteOpenHelper only implements AutoCloseable on newer Android releases.
	override fun close() = super.close()
	override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
	override fun onCreate(db: SQLiteDatabase) {
		db.execSQL("CREATE TABLE apps (id TEXT PRIMARY KEY, package TEXT NOT NULL UNIQUE, metadata TEXT NOT NULL, name TEXT NOT NULL DEFAULT '', base_url TEXT NOT NULL DEFAULT '', thumbnail BLOB, short_name TEXT)")
		createRules(db)
		createUserFiles(db)
	}
	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		if (oldVersion < 2) {
			db.execSQL("ALTER TABLE apps ADD COLUMN name TEXT NOT NULL DEFAULT ''")
			db.execSQL("ALTER TABLE apps ADD COLUMN base_url TEXT NOT NULL DEFAULT ''")
			db.execSQL("ALTER TABLE apps ADD COLUMN thumbnail BLOB")
		}
		if (oldVersion < 3) createRules(db)
		if (oldVersion < 4) createUserFiles(db)
		if (oldVersion < 5) db.execSQL("ALTER TABLE apps ADD COLUMN short_name TEXT")
	}
	private fun createUserFiles(db: SQLiteDatabase) {
		db.execSQL("CREATE TABLE app_user_files (app_id TEXT NOT NULL REFERENCES apps(id) ON DELETE CASCADE, position INTEGER NOT NULL, data BLOB NOT NULL, PRIMARY KEY(app_id, position))")
	}
	private fun createRules(db: SQLiteDatabase) {
		db.execSQL("CREATE TABLE app_rules (app_id TEXT NOT NULL REFERENCES apps(id) ON DELETE CASCADE, position INTEGER NOT NULL, data BLOB NOT NULL, PRIMARY KEY(app_id, position))")
	}
	private fun <T> snapshot(read: (SQLiteDatabase) -> T): T {
		val db = readableDatabase
		// Metadata and rule chunks must come from the same revision, even while
		// another database instance saves an edited web app.
		db.beginTransactionNonExclusive()
		try { return read(db).also { db.setTransactionSuccessful() } }
		finally { db.endTransaction() }
	}
	private fun metadata(db: SQLiteDatabase, key: String, json: String): JSONObject {
		val result = JSONObject(json)
		val bytes = ByteArrayOutputStream()
		db.query("app_rules", arrayOf("position", "data"), "app_id=?", arrayOf(key), null, null, "position").use { cursor ->
			var position = 0
			while (cursor.moveToNext()) {
				check(cursor.getInt(0) == position++) { "Incomplete filter rules" }
				val chunk = cursor.getBlob(1)
				check(bytes.size() + chunk.size <= AdblockRules.MAX_LENGTH) { "Stored filter rules exceed 8 MiB" }
				bytes.write(chunk)
			}
		}
		// Version 1/2 records retain their inline rules until their next save.
		if (bytes.size() > 0) result.put("adblockRules", bytes.toString("UTF-8"))
		val userBytes = ByteArrayOutputStream()
		db.query("app_user_files", arrayOf("position", "data"), "app_id=?", arrayOf(key), null, null, "position").use { cursor ->
			var position = 0
			while (cursor.moveToNext()) {
				check(cursor.getInt(0) == position++) { "Incomplete user files" }
				val chunk = cursor.getBlob(1)
				check(userBytes.size() + chunk.size <= UserContentFiles.MAX_TOTAL_BYTES) { "Stored user files exceed 8 MiB" }
				userBytes.write(chunk)
			}
		}
		if (userBytes.size() > 0) {
			val content = JSONObject(userBytes.toString("UTF-8"))
			UserContentFiles.Kind.entries.forEach { kind -> result.put(kind.key, content.getJSONArray(kind.key)) }
		}
		return result
	}
	@Synchronized fun summaries(): List<Summary> {
		// Backfill old libraries on the reader thread, without parsing metadata on each resume.
		val missing = buildList { writableDatabase.query("apps", arrayOf("id"), "name=''", null, null, null, null).use { cursor ->
			while (cursor.moveToNext()) add(cursor.getString(0))
		} }
		missing.forEach { key -> get(key)?.let { writableDatabase.update("apps", summaryValues(it), "id=? AND name=''", arrayOf(key)) } }
		// Backfill the small display label without loading rule or user-file chunks.
		writableDatabase.query("apps", arrayOf("id", "metadata", "name"), "short_name IS NULL", null, null, null, null).use { cursor ->
			while (cursor.moveToNext()) {
				val shortName = JSONObject(cursor.getString(1)).getJSONObject("manifest").optString("short_name").trim().ifEmpty { cursor.getString(2) }
				writableDatabase.update("apps", ContentValues().apply { put("short_name", shortName) }, "id=?", arrayOf(cursor.getString(0)))
			}
		}
		return buildList {
			readableDatabase.query("apps", arrayOf("id", "package", "name", "base_url", "thumbnail", "short_name"), null, null, null, null, "rowid DESC").use { cursor ->
				while (cursor.moveToNext()) add(Summary(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getBlob(4), cursor.getString(5)))
			}
		}
	}
	private fun summaryValues(app: WebApp): ContentValues = ContentValues().apply {
		put("name", app.name()); put("base_url", app.s("baseUrl"))
		put("short_name", app.manifest().optString("short_name").trim().ifEmpty { app.name() })
		val thumbnail = runCatching {
			val bytes = IconData.decode(app.s("icon"))
			val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
			android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
			options.inJustDecodeBounds = false; options.inSampleSize = maxOf(1, maxOf(options.outWidth, options.outHeight) / 96)
			val image = requireNotNull(android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options))
			try { java.io.ByteArrayOutputStream().apply { check(image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, this)) }.toByteArray() }
			finally { image.recycle() }
		}.getOrNull()
		put("thumbnail", thumbnail)
	}
	@Synchronized fun all(): List<WebApp> = snapshot { db -> buildList {
		db.query("apps", arrayOf("id", "metadata"), null, null, null, null, "rowid DESC").use { cursor ->
			while (cursor.moveToNext()) add(metadata(db, cursor.getString(0), cursor.getString(1)))
		}
	} }.map(::WebApp)
	@Synchronized fun get(id: String): WebApp? = snapshot { db ->
		db.query("apps", arrayOf("metadata"), "id=?", arrayOf(id), null, null, null, "1").use {
			if (it.moveToFirst()) metadata(db, id, it.getString(0)) else null
		}
	}?.let(::WebApp)
	@Synchronized fun conflictingPackage(app: WebApp): Boolean = readableDatabase.query("apps", arrayOf("id"), "package=? AND id<>?",
		arrayOf(app.s("packageName"), app.s("key")), null, null, null, "1").use { it.moveToFirst() }
	@Synchronized fun save(app: WebApp) {
		AdblockRules.validate(app.s("adblockRules"))
		UserContentFiles.validate(app.data)
		val metadata = app.metadataForStorage()
		val rules = app.s("adblockRules").toByteArray(Charsets.UTF_8)
		val userFiles = if (UserContentFiles.read(app.data).isEmpty()) byteArrayOf() else UserContentFiles.payload(app.data).toByteArray(Charsets.UTF_8)
		val values = summaryValues(app).apply {
			put("id", app.s("key")); put("package", app.s("packageName")); put("metadata", metadata)
		}
		val db = writableDatabase
		db.beginTransaction()
		try {
			val key = app.s("key")
			if (db.update("apps", values, "id=?", arrayOf(key)) == 0) db.insertOrThrow("apps", null, values)
			db.delete("app_rules", "app_id=?", arrayOf(key))
			for (offset in rules.indices step RULE_CHUNK_BYTES) {
				db.insertOrThrow("app_rules", null, ContentValues().apply {
					put("app_id", key); put("position", offset / RULE_CHUNK_BYTES)
					put("data", rules.copyOfRange(offset, minOf(offset + RULE_CHUNK_BYTES, rules.size)))
				})
			}
			db.delete("app_user_files", "app_id=?", arrayOf(key))
			for (offset in userFiles.indices step RULE_CHUNK_BYTES) {
				db.insertOrThrow("app_user_files", null, ContentValues().apply {
					put("app_id", key); put("position", offset / RULE_CHUNK_BYTES)
					put("data", userFiles.copyOfRange(offset, minOf(offset + RULE_CHUNK_BYTES, userFiles.size)))
				})
			}
			db.setTransactionSuccessful()
		} finally { db.endTransaction() }
	}
	@Synchronized fun remove(app: WebApp) { writableDatabase.delete("apps", "id=?", arrayOf(app.s("key"))) }
	@Synchronized fun importAll(entries: JSONArray) {
		require(entries.length() <= 10000) { "Too many apps" }
		val apps = (0 until entries.length()).map { WebApp(entries.getJSONObject(it)).apply { validate() } }
		require(apps.map { it.s("key") }.distinct().size == apps.size && apps.map { it.s("packageName") }.distinct().size == apps.size) { "Duplicate app in import" }
		val db = writableDatabase
		db.beginTransaction()
		try {
			val existing = buildMap {
				db.query("apps", arrayOf("package", "id"), null, null, null, null, null).use { cursor ->
					while (cursor.moveToNext()) put(cursor.getString(0), cursor.getString(1))
				}
			}
			apps.forEach { incoming ->
				existing[incoming.s("packageName")]?.let { incoming.set("key", it) }
				// An imported UUID cannot overwrite a different installed record.
				if (get(incoming.s("key"))?.s("packageName")?.let { it != incoming.s("packageName") } == true) {
					incoming.set("key", java.util.UUID.randomUUID().toString())
				}
				save(incoming)
			}
			db.setTransactionSuccessful()
		} finally { db.endTransaction() }
	}
	companion object { private const val RULE_CHUNK_BYTES = 128 * 1024 }
}
