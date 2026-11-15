(function () {
	if (window.__pwaFilterApplied === location.href) return;
	window.__pwaFilterApplied = location.href;
	function domainMatches(domains) {
		var host = location.hostname.toLowerCase(), parts = domains.split(','), included = false, hasPositive = false;
		function matches(value) {
			if (value.charAt(0) === '/' && value.charAt(value.length - 1) === '/') {
				try { return new RegExp(value.slice(1, -1), 'i').test(host); } catch (_) { return false; }
			}
			var pattern = value.replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '.*');
			try { return new RegExp('^(?:.*\\.)?' + pattern + '$', 'i').test(host); } catch (_) { return false; }
		}
		for (var i = 0; i < parts.length; i++) {
			var value = parts[i].trim(); if (!value) continue;
			if (value.charAt(0) === '~') { if (matches(value.slice(1))) return false; }
			else { hasPositive = true; included = included || matches(value); }
		}
		return !hasPositive || included;
	}
	var disabled = {}, pageExceptions = __PWA_DOCUMENT_EXCEPTIONS__;
	for (var n = 0; n < pageExceptions.length; n++) {
		var exception = pageExceptions[n];
		if (domainMatches(exception.domains)) try {
			var target = exception.anchor ? location.host + location.pathname + location.search : location.href;
			if (new RegExp(exception.pattern, exception.flags).test(target))
				for (var k = 0; k < exception.switches.length; k++) disabled[exception.switches[k]] = true;
		} catch (_) {}
	}
	var cosmeticRules = __PWA_COSMETIC_RULES__, scriptletRules = __PWA_SCRIPTLET_RULES__;
	function applies(rule) {
		if (!domainMatches(rule.domains)) return false;
		for (var i = 0; i < rule.exceptions.length; i++) if (domainMatches(rule.exceptions[i])) return false;
		return true;
	}
	__PWA_SCRIPTLET_FUNCTIONS__
	for (var i = 0; i < scriptletRules.length; i++) {
		var rule = scriptletRules[i];
		if (!disabled.jsinject && applies(rule)) try {
			scriptlets[rule.name]({name: rule.name, args: rule.args, engine:'extension', version:'2.5.1', verbose:false, uniqueId:'pwa-' + i}, rule.args);
		} catch (_) {}
	}
	var simple = [], extended = [];
	for (var i = 0; i < cosmeticRules.length; i++) {
		var rule = cosmeticRules[i];
		if (disabled.elemhide || disabled.generichide && !rule.domains || disabled.specifichide && rule.domains || !applies(rule)) continue;
		if (rule.extended) extended.push(rule.css);
		else if (rule.style) {
			try {
				var doc = document.implementation.createHTMLDocument(''), style = doc.createElement('style');
				style.textContent = rule.css; doc.head.appendChild(style);
				var css = style.sheet.cssRules;
				for (var j = 0; j < css.length; j++) if (css[j].selectorText) simple.push({selector:css[j].selectorText, style:css[j].style.cssText});
			} catch (_) {}
		} else simple.push({selector:rule.selector, style:'display: none !important;'});
	}
	function applySimple() {
		for (var i = 0; i < simple.length; i++) try {
			var nodes = document.querySelectorAll(simple[i].selector), holder = document.createElement('span');
			holder.style.cssText = simple[i].style;
			for (var j = 0; j < nodes.length; j++) for (var k = 0; k < holder.style.length; k++) {
				var key = holder.style[k], value = holder.style.getPropertyValue(key), priority = holder.style.getPropertyPriority(key);
				if (nodes[j].style.getPropertyValue(key) !== value || nodes[j].style.getPropertyPriority(key) !== priority)
					nodes[j].style.setProperty(key, value, priority);
			}
		} catch (_) {}
	}
	function ready() {
		applySimple();
		if (simple.length && window.MutationObserver) {
			var scheduled = false;
			new MutationObserver(function () {
				if (!scheduled) { scheduled = true; setTimeout(function () { scheduled = false; applySimple(); }, 40); }
			}).observe(document, {subtree:true, childList:true, attributes:true});
		}
		if (extended.length && typeof ExtendedCss !== 'undefined') try { new ExtendedCss({cssRules:extended}).apply(); } catch (_) {}
	}
	if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', ready);
	else ready();
})();
