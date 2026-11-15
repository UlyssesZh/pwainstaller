# External-display recovery

To reproduce the reported issue, open an installed web app as a freeform window on an external display in desktop mode, disconnect that display while the window is open, then reconnect it and return the retained app to the external display. The original report concerns Android 17 with Vanadium 156.0.8078.47.0. A physical reconnect also reproduced it on a Pixel 9 running Android 17 with Vanadium 157.0.8103.78.0: the native view used the external display's 196 dpi, but the renderer retained the phone's 420 dpi (a device pixel ratio of 2.625 instead of 1.225).

## Why a layout request alone is insufficient

Chromium caches a display association in `WindowAndroid`. Its [version 156 source](https://chromium.googlesource.com/chromium/src/+/refs/tags/156.0.8078.47/ui/android/java/src/org/chromium/ui/base/WindowAndroid.java) notes that display reconnection can stop and resume an activity without a configuration change, and that WebView does not invoke the activity-resume path used by Chrome to refresh that association. Dispatching a configuration change only to the Android `View` does not notify the context observers registered by WebView.

The browser tracks those publicly registered `ComponentCallbacks` and refreshes them when resuming an attached window and repairing a display change. On Android 11 and later, display lookup prefers the activity's valid display, since the activity context can move before the view hierarchy does; it falls back to the attached hierarchy when the activity still refers to a removed display. A pre-draw check detects the view hierarchy adopting a new display even when the window's dimensions have not changed.

Configuration callbacks are forwarded through observers that track their previous display and density. When either changes, WebViews are detached before forwarding the callback and reattached afterward. Chromium's `AwContents` removes its density observer from `WindowAndroid`'s current display during detachment; changing that display first can leave the observer registered on the old display. This ordering retains the live WebView and document while refreshing its display attachment.

The physical-monitor test exposed a second layer of cached state: Java's `WindowAndroid` and its display context both had the correct external display ID and density, but the renderer's `ScreenInfo` still described the phone. Android WebView detach/attach only reaches `BrowserViewRenderer`, rather than reparenting the native page view. Reparenting the existing `WebContents` to the same native window through a temporary null parent corrected the live test's device pixel ratio from 2.625 to 1.225 while preserving the original document token.

After normal recovery, the browser checks whether the renderer's device pixel ratio still disagrees with the activity density. Only that mismatch triggers `BrowserRendererDisplay`, an optional Chromium compatibility bridge. It discovers `AwContents` through the provider's registered callbacks and identifies the `WebContents` getter and window setter by their types, without relying on R8 method or field names. The original native window is restored in a `finally` block. This uses reflection into the WebView provider, not hidden Android framework APIs; unsupported providers keep the normal public-API recovery. Future provider changes may disable this optional workaround, so an upstream WebView fix remains preferable.

A pending repair is retained while bounds are empty or a window is detached. Once layout is ready, the existing WebView is reattached and resized without replacing its document. Ordinary window resizing and refresh-rate/HDR-only display events do not trigger this reattachment.

Scroll offsets are captured before a display callback detaches the view or the activity pauses, then restored in CSS coordinates. The first snapshot is retained through subsequent callbacks. The conversion accounts for WebView reporting the new density before its old scroll pixels have been rescaled; otherwise a 300 CSS-pixel scroll offset can incorrectly become 114 pixels when moving from 160 to 420 dpi. Restoration is skipped if the page URL changes. Native padding handles system bars, cutouts, and the keyboard; handled inset dimensions are forwarded as zeroes instead of consuming the update. This follows [Android’s WebView inset guidance](https://developer.android.com/develop/ui/views/layout/webapps/understand-window-insets), allowing the provider to clear stale viewport state.

## Regression checks

`DeviceIntegrationTest` covers:

- `reconnectNotifiesWebViewContextObserversEvenWithoutAConfigurationChange`: resume an unchanged window and check context callback delivery and unregistration without a page reload.
- `nativeRendererDisplayRepairRetainsTheDocumentAndFormState`: on Chromium 156/157, exercise the native compatibility bridge and check document identity, unsaved text, scroll position, density, and main-document request count.
- `nativeInsetsReachWebViewAsZeroUpdatesInsteadOfBeingConsumed`: native padding is applied while clearing updates still reach WebView, including keyboard dismissal.
- `displayRepairWaitsForLayoutInsteadOfLosingAnEmptyViewport`: a temporarily empty viewport does not consume the pending repair.
- `unpluggingAnExternalFreeformDisplayAndReconnectingAtTheSameSizeKeepsAllFourPageCornersVisible`: launch on a public virtual display, release that display while the app is open, create another display with the same 1280×900 dimensions and 160 dpi, and reopen the retained task as a freeform window. Captured display buffers must show all four page corners at the expected physical scale and density. The document token, unsaved form text, scroll position, and main-document request count are also checked.
- The existing resize/display-move, fullscreen, keyboard, and video-display-event checks cover adjacent behavior.

The public virtual display intentionally migrates its task on removal. The older overlay-display test manually moved the task before removing its display, which missed this sequence. Pixel checks also detect a bad compositor scale even when DOM dimensions appear consistent.

`PhysicalDisplayTest#retainedWindowUsesTheReconnectedDisplaysDensity` is an opt-in check for a real monitor. Pass its current logical display ID using `-e externalDisplayId ID` to the AndroidJUnitRunner, then physically unplug and reconnect the monitor. It checks the retained document token, device pixel ratio, and CSS viewport against the native view. Without that argument, it is skipped. Debug-enabled builds also record display IDs and density under the `PwaViewport` log tag without logging page contents.

## Verification limits

The physical-monitor check passes on the Pixel 9 with Android 17 and Vanadium 157.0.8103.78.0. After disconnect/reconnect, automatic recovery detected the stale 2.625 renderer ratio, reattached the native page, and restored the expected 1.225 ratio for the 196 dpi monitor. The original document token survived and the CSS viewport again matched the native 1382-pixel-wide view. The native bridge's separate document/form/scroll preservation check passes on the same device.

The updated public-virtual-display hotplug test also passes on this device, including physical pixel rendering, density, scroll, unsaved form text, and no reload. The callback, insets, deferred-layout, and video/fullscreen checks pass on an Android 15 AVD. A display power cycle with `cmd display disable-display`/`enable-display` briefly changes density but recovers by itself, and is not equivalent to unplugging the monitor.

Earlier hotplug checks passed on an Android 15 AVD, including an x86-64 Chromium 156.0.8078.0 provider from the [official Chromium snapshot archive](https://storage.googleapis.com/chromium-browser-snapshots/index.html?prefix=AndroidDesktop_x64/1706493/). The earlier callback/resume check also passed on an Android 5.0 (API 21) AVD. Very old providers such as the AVD's original WebView 124 do not implement the same context-observer display-refresh mechanism.

The changes are in the generated runtime. Updating PWA Installer must be followed by reinstalling the affected generated web apps.
