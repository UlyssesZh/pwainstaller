package io.github.ulysseszh.pwainstaller.runtime

import org.json.JSONObject
import java.net.URI

/** Origin-scoped data delivery without exposing a JavaScript-to-native interface. */
object LaunchScripts {
	private fun safe(value: String) = value.replace("<", "\\u003c").replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
	private fun json(value: String) = safe(JSONObject.quote(value))
	private val decodeFile = """
		function file(item){var raw=atob(item.data),bytes=new Uint8Array(raw.length);
		for(var i=0;i<raw.length;i++)bytes[i]=raw.charCodeAt(i);
		var result;try{result=new File([bytes],item.name,{type:item.type});}
		catch(e){result=new Blob([bytes],{type:item.type});result.name=item.name;}return result;}
	""".trimIndent()

	fun queue(payload: JSONObject, scope: String): String {
		val origin = URI(scope).let { "${it.scheme}://${it.rawAuthority}" }
		return """
			(function(){if(window.top!==window||location.protocol+'//'+location.host!==${json(origin)}||location.pathname.indexOf(${json(URI(scope).path)})!==0)return;
			if(window.__pwaLaunchQueue)return;
			$decodeFile
			var items=${safe(payload.getJSONArray("files").toString())};
			var handles=items.map(function(item){return {kind:'file',name:item.name,
			  getFile:function(){return Promise.resolve(file(item));},
			  queryPermission:function(options){return Promise.resolve(options&&options.mode==='readwrite'?'denied':'granted');},
			  requestPermission:function(options){return this.queryPermission(options);}};});
			var delivered=false,queue={setConsumer:function(consumer){if(typeof consumer!=='function')throw new TypeError('Expected a function');
			  if(!delivered){delivered=true;Promise.resolve().then(function(){consumer({files:handles,targetURL:${json(payload.getString("url"))}});});}}};
			window.__pwaLaunchQueue=queue;
			try{Object.defineProperty(window,'launchQueue',{value:queue,configurable:true});}catch(e){window.launchQueue=queue;}
			})();
		""".trimIndent()
	}

	/** Legacy WebViews need the queue installed before the handler page's first script executes. */
	fun fileBootstrap(payload: JSONObject, scope: String, failureMessage: String): String {
		val target = payload.getString("url")
		val prefix = "<script>" + queue(payload, scope).replace("</", "<\\/") + "</script>"
		return html("""
			var xhr=new XMLHttpRequest();xhr.open('GET',${json(target)},true);xhr.withCredentials=true;
			xhr.onload=function(){render(xhr,${json(prefix)},${json(scope)});};xhr.onerror=failed;xhr.send();
		""".trimIndent(), failureMessage)
	}

	/** Use a document navigation so service workers, redirects and response policies work normally. */
	fun post(payload: JSONObject, scope: String, failureMessage: String): String = html("""
		$decodeFile
		var fields=${safe(payload.getJSONArray("fields").toString())},files=${safe(payload.getJSONArray("files").toString())};
		var form=document.createElement('form');form.method='POST';form.action=${json(payload.getString("url"))};
		form.enctype=${json(payload.optString("enctype", "application/x-www-form-urlencoded"))};form.acceptCharset='UTF-8';
		fields.forEach(function(item){var input=document.createElement('input');input.type='hidden';input.name=item.name;input.value=item.value;form.appendChild(input);});
		try {
			var groups=Object.create(null);files.forEach(function(item){var group=groups[item.field];
				if(!group){var input=document.createElement('input');input.type='file';input.name=item.field;input.multiple=true;
					group=groups[item.field]={input:input,transfer:new DataTransfer()};form.appendChild(input);}
				group.transfer.items.add(file(item));});
			Object.keys(groups).forEach(function(name){var group=groups[name];group.input.files=group.transfer.files;
				if(group.input.files.length!==group.transfer.files.length)throw new Error('File assignment unavailable');});
		} catch(error) {
			// Legacy WebViews cannot assign FileLists. Keep file sharing available there.
			var data=new FormData();fields.forEach(function(item){data.append(item.name,item.value);});
			files.forEach(function(item){data.append(item.field,file(item),item.name);});
			var xhr=new XMLHttpRequest();xhr.open('POST',${json(payload.getString("url"))},true);xhr.withCredentials=true;
			xhr.onload=function(){render(xhr,'',${json(scope)});};xhr.onerror=failed;xhr.send(data);return;
		}
		document.body.appendChild(form);HTMLFormElement.prototype.submit.call(form);
	""".trimIndent(), failureMessage)

	private fun html(script: String, failureMessage: String): String {
		val helpers = """
			function failed(){document.body.textContent=${json(failureMessage)};}
			function render(xhr,prefix,scope){var allowed=document.createElement('a'),actual=document.createElement('a');
			  allowed.href=scope;actual.href=xhr.responseURL||location.href;
			  if(actual.protocol!==allowed.protocol||actual.host!==allowed.host||actual.pathname.indexOf(allowed.pathname)!==0){failed();return;}
			  if(xhr.status===0){failed();return;}
			  var contentType=xhr.getResponseHeader('Content-Type')||'';
			  if(!/^(text\/html|application\/xhtml\+xml)(;|${'$'})/i.test(contentType)){document.body.textContent=xhr.responseText;return;}
			  var policy=xhr.getResponseHeader('Content-Security-Policy');
			  var meta=policy?'<meta http-equiv="Content-Security-Policy" content="'+policy.replace(/&/g,'&amp;').replace(/"/g,'&quot;')+'">':'';
			  var base='<base href="'+actual.href.replace(/&/g,'&amp;').replace(/"/g,'&quot;')+'">';
			  history.replaceState(null,'',actual.href);document.open();document.write(prefix+meta+base+xhr.responseText);document.close();}
		""".trimIndent()
		return "<!doctype html><meta name=viewport content=\"width=device-width,initial-scale=1\"><body><script>" +
			("(function(){" + helpers + "\n" + script + "})();").replace("</script", "<\\/script", true) + "</script></body>"
	}
}
