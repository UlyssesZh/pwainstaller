package io.github.ulysseszh.pwainstaller

import org.json.JSONArray
import org.json.JSONObject
import io.github.ulysseszh.pwainstaller.core.PackageNames
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.UUID

/** Selected files are self-contained snapshots, including in metadata exports. */
object UserContentFiles {
	const val MAX_FILE_BYTES = 1024 * 1024
	const val MAX_TOTAL_BYTES = 8 * 1024 * 1024
	enum class Kind(val key: String) { SCRIPT("userScripts"), STYLE("userStyles") }
	data class Entry(val id: String, val kind: Kind, val name: String, val code: String, val url: String = "") {
		fun json() = JSONObject().put("id", id).put("name", name).put("code", code).put("url", url)
		fun validate() {
			require(UUID.fromString(id).toString() == id) { "Invalid user file ID" }
			require(name.isNotBlank() && name.length <= 256 && '\u0000' !in name) { "Invalid user file name" }
			require(code.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES && '\u0000' !in code) { "User files must be UTF-8 text, at most 1 MiB each" }
			if (url.isNotEmpty()) PackageNames.webUrl(url)
		}
	}
	fun decode(bytes: ByteArray): String {
		require(bytes.size <= MAX_FILE_BYTES) { "User file exceeds 1 MiB" }
		return Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
			.decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF").also { require('\u0000' !in it) { "Choose a UTF-8 text file" } }
	}
	fun read(data: JSONObject): List<Entry> = Kind.entries.flatMap { kind ->
		require(!data.has(kind.key) || data.opt(kind.key) is JSONArray) { "Invalid user files" }
		val files = data.optJSONArray(kind.key) ?: JSONArray()
		require(files.length() <= 64) { "Too many user files" }
		(0 until files.length()).map { index -> files.getJSONObject(index).let {
			Entry(it.getString("id"), kind, it.getString("name"), it.getString("code"), it.optString("url"))
		} }
	}
	fun put(data: JSONObject, files: List<Entry>) {
		Kind.entries.forEach { kind -> data.put(kind.key, JSONArray(files.filter { it.kind == kind }.map { it.json() })) }
	}
	fun validate(data: JSONObject) {
		val files = read(data)
		require(files.size <= 64 && files.map { it.id }.distinct().size == files.size) { "Too many or duplicate user files" }
		files.forEach(Entry::validate)
		require(payload(data).toByteArray(Charsets.UTF_8).size <= MAX_TOTAL_BYTES) { "Selected user files exceed 8 MiB" }
	}
	fun payload(data: JSONObject): String = JSONObject().also { output ->
		Kind.entries.forEach { output.put(it.key, data.optJSONArray(it.key) ?: JSONArray()) }
	}.toString()
	fun marker(entry: Entry) = "__pwaUserFile_" + entry.id.replace("-", "")
	fun script(entry: Entry): String {
		val css = JSONObject.quote(entry.code).replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
		val code = if (entry.kind == Kind.SCRIPT) "try{(function(){\n${entry.code}\n}).call(window);}catch(e){console.error('User script failed: '+e);}"
			else """var css=$css,style=document.createElement('style'),nonce=document.querySelector('style[nonce],script[nonce]');
				if(nonce)style.setAttribute('nonce',nonce.nonce||nonce.getAttribute('nonce'));style.textContent=css;document.documentElement.appendChild(style);
				try{if(!style.sheet&&window.CSSStyleSheet&&CSSStyleSheet.prototype.replaceSync&&'adoptedStyleSheets' in document){
				var sheet=new CSSStyleSheet();sheet.replaceSync(css);document.adoptedStyleSheets=Array.prototype.slice.call(document.adoptedStyleSheets).concat([sheet]);style.remove();}}catch(e){}""".trimIndent()
		return """(function(){if(!/^https?:${'$'}/.test(location.protocol)||window.top!==window||window[${JSONObject.quote(marker(entry))}])return;
			window[${JSONObject.quote(marker(entry))}]=true;function run(){$code}
			if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run,{once:true});else run();})();
		""".trimIndent()
	}
}
