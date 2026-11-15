package io.github.ulysseszh.pwainstaller.browser

import android.webkit.WebView
import org.json.JSONObject

/** Supplement WebView's native hit test for CSS images, overlays and shadow DOM. */
internal data class BrowserContextHit(val link: String?, val source: String?, val text: String?, val editable: Boolean) {
    companion object {
        fun read(view: WebView, x: Float, y: Float, ready: (BrowserContextHit?) -> Unit) {
            if (view.width <= 0 || !x.isFinite() || !y.isFinite()) return
            // Both axes use the viewport width to account for page zoom and density.
            val script = """(function(){try{
                var v=window.visualViewport,w=v?v.width:innerWidth;
                var x=${x / view.width}*w+(v?v.offsetLeft:0),y=${y / view.width}*w+(v?v.offsetTop:0);
                var nodes=[],seen=[],link=null,text=null,source=null,editable=false;
                function parent(e){return e.parentElement||(e.parentNode&&e.parentNode.host)||(e.getRootNode&&e.getRootNode().host)||null;}
                function collect(root,depth){
                    if(depth>8)return;
                    var list=root.elementsFromPoint?root.elementsFromPoint(x,y):[root.elementFromPoint(x,y)];
                    for(var i=0;i<list.length&&nodes.length<64;i++){
                        var e=list[i];if(!e||seen.indexOf(e)>=0)continue;seen.push(e);
                        if(e.shadowRoot&&e.shadowRoot.elementFromPoint)collect(e.shadowRoot,depth+1);
                        nodes.push(e);
                    }
                }
                collect(document,0);
                var top=nodes[0];
                for(var e=top;e;e=parent(e)){
                    // Older WebViews have elementFromPoint but no elementsFromPoint.
                    if(nodes.indexOf(e)<0)nodes.push(e);
                    if(e.isContentEditable||/^(INPUT|TEXTAREA|SELECT)$/.test(e.tagName)){editable=true;break;}
                    if(!link&&e.tagName==='A'&&e.href){link=e.href;text=(typeof e.innerText==='string'?e.innerText:e.textContent||'').trim();}
                }
                if(editable)return {editable:true};
                function absolute(url,e){var a=e.ownerDocument.createElement('a');a.href=url;return a.href;}
                function background(e,pseudo){
                    var b=e.ownerDocument.defaultView.getComputedStyle(e,pseudo).backgroundImage;
                    var m=/url\(\s*(?:"((?:\\.|[^"\\])*)"|'((?:\\.|[^'\\])*)'|([^)]*))\s*\)/i.exec(b||'');
                    return m?absolute((m[1]||m[2]||m[3]||'').trim().replace(/\\(["'()\\])/g,'$1'),e):null;
                }
                function image(e){
                    if(e.tagName==='IMG')return e.currentSrc||e.src;
                    if(e.localName==='image'){var href=e.getAttribute('href')||e.getAttribute('xlink:href');if(href)return absolute(href,e);}
                    return background(e,null)||background(e,'::before')||background(e,'::after');
                }
                var checked=0;
                function children(e,depth){
                    if(depth>8)return null;
                    for(var c=e.firstElementChild;c&&checked++<128;c=c.nextElementSibling){
                        var r=c.getBoundingClientRect();
                        if(x<r.left||x>=r.right||y<r.top||y>=r.bottom)continue;
                        var s=c.ownerDocument.defaultView.getComputedStyle(c);
                        if(s.visibility==='hidden'||s.display==='none'||s.opacity==='0')continue;
                        var found=image(c)||children(c,depth+1);if(found)return found;
                    }
                    return null;
                }
                for(var i=0;i<nodes.length&&!source;i++)source=image(nodes[i]);
                // pointer-events:none images are absent from elementsFromPoint.
                for(var i=0;i<nodes.length&&!source;i++)source=children(nodes[i],0);
                return {link:link,source:source,text:text,editable:false};
            }catch(e){return null;}})()""".trimIndent()
            view.evaluateJavascript(script) { result ->
                val hit = runCatching {
                    val data = JSONObject(result)
                    fun value(key: String) = data.optString(key).takeIf { !data.isNull(key) && it.isNotBlank() }
                    BrowserContextHit(value("link"), value("source"), value("text"), data.optBoolean("editable"))
                }.getOrNull()
                ready(hit)
            }
        }
    }
}
