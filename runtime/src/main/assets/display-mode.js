(function () {
	if (!/^https?:$/.test(location.protocol) || window.__pwaDisplayModeInstalled) return;
	window.__pwaDisplayModeInstalled = true;
	var mode = __PWA_DISPLAY_MODE__, originalMatchMedia = window.matchMedia;
	function query(value) {
		return String(value).replace(/\(\s*display-mode\s*(?::\s*([a-z-]+)\s*)?\)/gi, function (_, requested) {
			return !requested || requested.toLowerCase() === mode ? '(min-width: 0px)' : '(-pwa-display-mode: none)';
		});
	}
	function stylesheet(text) {
		var result = '', header = false, i = 0;
		while (i < text.length) {
			var start = i, c = text.charAt(i);
			if (text.substr(i, 2) === '/*') {
				i = text.indexOf('*/', i + 2); i = i < 0 ? text.length : i + 2;
				result += text.substring(start, i); continue;
			}
			if (c === '"' || c === "'") {
				i++;
				while (i < text.length) { var next = text.charAt(i++); if (next === '\\') i++; else if (next === c) break; }
				result += text.substring(start, i); continue;
			}
			if (c === '\\') { result += text.substr(i, 2); i += 2; continue; }
			if (c === '@') { var name = /^@([a-z-]+)/i.exec(text.substring(i)); if (name && /^(media|import)$/i.test(name[1])) header = true; }
			if (header && c === '(') {
				var feature = /^\(\s*display-mode\s*(?::\s*[a-z-]+\s*)?\)/i.exec(text.substring(i));
				if (feature) { result += query(feature[0]); i += feature[0].length; continue; }
			}
			if (c === '{' || c === ';') header = false;
			result += c; i++;
		}
		return result;
	}
	window.matchMedia = function (value) {
		var original = String(value), translated = query(original), result = originalMatchMedia.call(window, translated);
		if (translated !== original) {
			try { Object.defineProperty(result, 'media', {value: original, configurable: true}); } catch (_) {}
			function eventMedia(event) { try { Object.defineProperty(event, 'media', {value: original, configurable: true}); } catch (_) {} }
			// Early WebViews expose a read-only, nonconfigurable .media property.
			if (result.media !== original) {
				var native = result;
				result = {media: original, addListener: function (listener) { native.addListener(listener); }, removeListener: function (listener) { native.removeListener(listener); }};
				Object.defineProperty(result, 'matches', {get: function () { return native.matches; }});
			}
			if (result.addEventListener) result.addEventListener('change', eventMedia, true);
			else if (result.addListener) result.addListener(eventMedia);
		}
		return result;
	};
	function media(value) {
		if (!value) return;
		var before = value.mediaText, after = query(before);
		if (before !== after) try { value.mediaText = after; } catch (_) {}
	}
	function sheet(value) {
		try {
			media(value.media);
			var rules = value.cssRules;
			for (var i = 0; rules && i < rules.length; i++) {
				media(rules[i].media);
				if (rules[i].styleSheet) sheet(rules[i].styleSheet);
				if (rules[i].cssRules) sheet(rules[i]);
			}
		} catch (_) {} // Opaque cross-origin sheets are handled by the native CSS interceptor.
	}
	var boundMedia = [];
	function elementMedia(element) {
		var before = element.getAttribute('media'), previous = element.__pwaMedia;
		if (previous && previous.applied === before) return;
		if (previous) previous.query.removeListener(previous.update);
		var after = query(before);
		if (before === after) { element.__pwaMedia = null; return; }
		var binding = {element: element, query: originalMatchMedia.call(window, after), applied: null};
		binding.update = function () {
			// Legacy WebKit parses unknown features in matchMedia, but skips entire style
			// elements when the same features appear in their media attribute.
			var value = binding.query.matches ? 'all' : 'not all';
			if (element.getAttribute('media') === value) return;
			binding.applied = value;
			element.setAttribute('media', value);
			try { if (element.sheet) element.sheet.media.mediaText = value; } catch (_) {}
			if (binding.query.matches && element.tagName.toLowerCase() === 'style' && !element.sheet && element.parentNode) {
				var parent = element.parentNode, next = element.nextSibling;
				parent.removeChild(element); parent.insertBefore(element, next);
			}
		};
		element.__pwaMedia = binding;
		boundMedia.push(binding);
		binding.query.addListener(binding.update);
		binding.update();
	}
	function apply() {
		boundMedia = boundMedia.filter(function (binding) {
			if (binding.element.__pwaMedia === binding && binding.element.hasAttribute('media') && document.documentElement.contains(binding.element)) return true;
			if (binding.element.__pwaMedia === binding) binding.element.__pwaMedia = null;
			binding.query.removeListener(binding.update); return false;
		});
		// Old CSS parsers discard unknown media features from CSSOM; use the source text.
		var styles = document.querySelectorAll('style');
		for (var i = 0; i < styles.length; i++) {
			var before = styles[i].textContent;
			if (styles[i].__pwaCss === before) continue;
			var after = stylesheet(before);
			styles[i].__pwaCss = after;
			if (before !== after) styles[i].textContent = after;
		}
		var elements = document.querySelectorAll('style[media],link[media]');
		for (var i = 0; i < elements.length; i++) elementMedia(elements[i]);
		for (var i = 0; i < document.styleSheets.length; i++) sheet(document.styleSheets[i]);
		var adopted = document.adoptedStyleSheets || [];
		for (var i = 0; i < adopted.length; i++) sheet(adopted[i]);
	}
	try { if (window.CSSStyleSheet) {
		var proto = CSSStyleSheet.prototype, insert = proto.insertRule;
		if (insert) proto.insertRule = function () { var args = Array.prototype.slice.call(arguments); args[0] = stylesheet(String(args[0])); var result = insert.apply(this, args); sheet(this); return result; };
		var replace = proto.replaceSync;
		if (replace) proto.replaceSync = function () { var result = replace.call(this, stylesheet(String(arguments[0]))); sheet(this); return result; };
		var asyncReplace = proto.replace;
		if (asyncReplace) proto.replace = function () { var self = this; return asyncReplace.call(this, stylesheet(String(arguments[0]))).then(function (result) { sheet(self); return result; }); };
	} } catch (_) {}
	try { if (window.MediaList) {
		var descriptor = Object.getOwnPropertyDescriptor(MediaList.prototype, 'mediaText');
		if (descriptor && descriptor.set && descriptor.configurable) {
			var setter = descriptor.set;
			descriptor.set = function (value) { setter.call(this, query(value)); };
			Object.defineProperty(MediaList.prototype, 'mediaText', descriptor);
		}
		['appendMedium', 'deleteMedium'].forEach(function (name) {
			var method = MediaList.prototype[name];
			if (method) MediaList.prototype[name] = function (value) { return method.call(this, query(value)); };
		});
	} } catch (_) {}
	var pending = false;
	function schedule() { if (!pending) { pending = true; setTimeout(function () { pending = false; apply(); }, 0); } }
	if (window.MutationObserver) new MutationObserver(schedule).observe(document, {subtree:true, childList:true, characterData:true, attributes:true, attributeFilter:['media']});
	document.addEventListener('load', schedule, true);
	document.addEventListener('DOMContentLoaded', apply);
	apply();
})();
