package io.github.ulysseszh.pwainstaller.browser

import android.content.ComponentCallbacks
import android.content.res.Configuration

/** Configuration observers registered on the browser's activity context by WebView. */
internal class BrowserConfiguration(
	private val display: () -> Int,
	private val density: () -> Int,
	private val changingDisplay: (Int, () -> Unit) -> Unit,
) {
	private inner class Observer(
		val callback: ComponentCallbacks,
	) : android.content.ComponentCallbacks2 {
		private var displayId = display()
		private var densityDpi = density()

		override fun onConfigurationChanged(configuration: Configuration) {
			val nextDisplay = display()
			val nextDensity = density()
			val notify = { callback.onConfigurationChanged(configuration) }
			if (nextDisplay != displayId || nextDensity != densityDpi) {
				changingDisplay(densityDpi, notify)
			} else {
				notify()
			}
			displayId = nextDisplay
			densityDpi = nextDensity
		}

		// Still required by the ComponentCallbacks contract on older Android releases.
		@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
		override fun onLowMemory() = callback.onLowMemory()

		override fun onTrimMemory(level: Int) {
			(callback as? android.content.ComponentCallbacks2)?.onTrimMemory(level)
		}
	}

	private val callbacks = linkedMapOf<ComponentCallbacks, Observer>()

	fun add(callback: ComponentCallbacks): ComponentCallbacks = callbacks.getOrPut(callback) { Observer(callback) }

	fun remove(callback: ComponentCallbacks): ComponentCallbacks? = callbacks.remove(callback)

	fun description() = callbacks.keys.joinToString { it.javaClass.name }

	fun reconnectRendererDisplays() = BrowserRendererDisplay.reconnect(callbacks.keys.toList())

	fun refresh(configuration: Configuration) {
		// Chromium's WindowAndroid normally updates its display from this callback.
		// Unlike Chrome, WebView doesn't notify WindowAndroid when an activity resumes
		// after reconnecting a display without a configuration change (crbug.com/444627601).
		callbacks.toList().forEach { (callback, observer) ->
			if (callback in callbacks) {
				observer.onConfigurationChanged(Configuration(configuration))
			}
		}
	}

	fun clear(unregister: (ComponentCallbacks) -> Unit) {
		callbacks.values.toList().forEach(unregister)
		callbacks.clear()
	}
}
