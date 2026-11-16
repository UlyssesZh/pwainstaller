package io.github.ulysseszh.pwainstaller.browser

import android.content.ComponentCallbacks
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap

/** Optional Chromium workaround; does not access hidden Android framework APIs. */
internal object BrowserRendererDisplay {
	private const val CONTENTS = "org.chromium.android_webview.AwContents"
	private const val WEB_CONTENTS = "org.chromium.content_public.browser.WebContents"
	private const val WINDOW = "org.chromium.ui.base.WindowAndroid"

	fun reconnect(callbacks: List<ComponentCallbacks>): Int {
		val repaired = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
		for (callback in callbacks) {
			// AwContents registers an inner ComponentCallbacks on its activity.
			// Discover its enclosing instance by type, independent of R8 field names.
			val contents =
				runCatching {
					callback.javaClass.declaredFields
						.firstOrNull {
							!Modifier.isStatic(it.modifiers) && it.type.name == CONTENTS
						}?.let { field ->
							field.isAccessible = true
							field.get(callback)
						}
				}.getOrNull() ?: continue
			runCatching {
				val getter =
					contents.javaClass.methods.singleOrNull {
						it.parameterTypes.isEmpty() && it.returnType.name == WEB_CONTENTS
					}
				val page = getter?.invoke(contents) ?: findPage(contents) ?: return@runCatching
				if (page in repaired) {
					return@runCatching
				}
				// Use the WebContents interface, rather than similarly shaped
				// WindowEventObserver methods on its implementation class.
				val contract = Class.forName(WEB_CONTENTS, false, contents.javaClass.classLoader)
				if (!contract.isInstance(page)) {
					return@runCatching
				}
				val methods = contract.methods
				val windowGetter =
					methods.singleOrNull {
						it.parameterTypes.isEmpty() && it.returnType.name == WINDOW
					} ?: return@runCatching
				val setter =
					methods.singleOrNull {
						it.returnType == Void.TYPE && it.parameterTypes.size == 1 && it.parameterTypes[0].name == WINDOW
					} ?: return@runCatching
				val window = windowGetter.invoke(page) ?: return@runCatching
				// Android View detach/attach only refreshes BrowserViewRenderer.
				// Reparenting this native view also makes RenderWidgetHostView
				// refresh ScreenInfo, which otherwise keeps the phone's density.
				try {
					setter.invoke(page, null)
				} finally {
					setter.invoke(page, window)
				}
				repaired.add(page)
			}
			// Provider internals are optional. Unsupported versions retain the
			// normal public-API display repair and must never crash the browser.
		}
		return repaired.size
	}

	private fun findPage(contents: Any): Any? {
		// R8 can inline getWebContents and move its field into a helper object.
		// Search only provider-owned objects, with small depth/object limits.
		val loader = contents.javaClass.classLoader
		val seen = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())

		fun visit(
			owner: Any,
			depth: Int,
		): Any? {
			if (depth > 2 || seen.size >= 64 || !seen.add(owner)) {
				return null
			}
			val fields = owner.javaClass.declaredFields.filter { !Modifier.isStatic(it.modifiers) }
			fields.firstOrNull { it.type.name == WEB_CONTENTS }?.let { field ->
				field.isAccessible = true
				field.get(owner)?.let { return it }
			}
			for (field in fields) {
				if (field.type.isPrimitive || field.type.isArray || field.type.classLoader !== loader) {
					continue
				}
				val child =
					runCatching {
						field.isAccessible = true
						field.get(owner)
					}.getOrNull() ?: continue
				visit(child, depth + 1)?.let { return it }
			}
			return null
		}
		return visit(contents, 0)
	}
}
