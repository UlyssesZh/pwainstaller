(function (sampleBackgrounds) {
	if (!document.documentElement) {
		return null;
	}
	function themeColor() {
		var nodes = document.getElementsByTagName('meta');
		var cache = window.__pwaPageThemeColor;
		for (var i = 0; i < nodes.length; i++) {
			var node = nodes[i];
			if ((node.getAttribute('name') || '').toLowerCase() !== 'theme-color' || !node.hasAttribute('content')) {
				continue;
			}
			var media = node.getAttribute('media');
			if (media && !window.matchMedia(media).matches) {
				continue;
			}
			var value = node.getAttribute('content').trim();
			if (!cache) {
				var canvas = document.createElement('canvas');
				canvas.width = canvas.height = 1;
				cache = window.__pwaPageThemeColor = { paint: canvas.getContext('2d', { willReadFrequently: true }) };
			}
			if (cache.value === value) {
				return cache.color;
			}
			var paint = cache.paint;
			if (!paint) {
				return value;
			}
			// Invalid assignments retain fillStyle. Two sentinels distinguish
			// invalid CSS from a valid color equal to either sentinel.
			paint.fillStyle = '#010203';
			paint.fillStyle = value;
			var first = paint.fillStyle;
			paint.fillStyle = '#040506';
			paint.fillStyle = value;
			if (paint.fillStyle !== first) {
				continue;
			}
			paint.clearRect(0, 0, 1, 1);
			paint.fillRect(0, 0, 1, 1);
			var rgba = paint.getImageData(0, 0, 1, 1).data;
			cache.value = value;
			cache.color = 'rgba(' + rgba[0] + ',' + rgba[1] + ',' + rgba[2] + ',' + rgba[3] / 255 + ')';
			return cache.color;
		}
		return null;
	}

	function layers(y) {
		var element = document.elementFromPoint(innerWidth / 2, y),
			colors = [];
		while (element && element !== document.body && element !== document.documentElement) {
			var bounds = element.getBoundingClientRect();
			if (bounds.left <= 1 && bounds.right >= innerWidth - 1) {
				colors.push(getComputedStyle(element).backgroundColor);
			}
			element = element.parentElement;
		}
		return colors.reverse();
	}
	return [
		location.href,
		sampleBackgrounds ? getComputedStyle(document.documentElement).backgroundColor : null,
		sampleBackgrounds && document.body ? getComputedStyle(document.body).backgroundColor : null,
		sampleBackgrounds ? layers(0) : [],
		sampleBackgrounds ? layers(Math.max(0, innerHeight - 1)) : [],
		themeColor(),
	];
})(__PWA_SAMPLE_BACKGROUNDS__);
