# Partial web notification support

This is a proposal, not an implemented feature. Android WebView still lacks the
Notification and Push APIs tracked by [Chromium issue 40388442](https://issues.chromium.org/issues/40388442).
The issue page requires sign-in here, so no additional rationale is inferred from its comments.

## Recommended first step: notifications from an open page

Add an optional per-app **Web notifications** setting, disabled by default. A small
JavaScript compatibility layer installed at document start would implement
`Notification.requestPermission()`, `Notification.permission`, `new Notification()`,
`close()`, and replacement by `tag`. It could also implement calls to
`ServiceWorkerRegistration.showNotification()` and `getNotifications()` made **from
the page**. Kotlin would display actual Android notifications using the generated
app's package, name and icon. Tapping a notification would reopen its saved, validated
app URL; live pages could additionally receive click/close events. JavaScript
callbacks cannot survive a killed page, so this would not claim persistent
`notificationclick` service-worker semantics.

Use Android's [origin-scoped WebMessageListener](https://developer.android.com/reference/androidx/webkit/WebViewCompat#addWebMessageListener(android.webkit.WebView,java.lang.String,java.util.Set,androidx.webkit.WebViewCompat.WebMessageListener))
when available, checking the supplied origin and main-frame flag natively. Restrict
access to explicitly granted HTTPS origins, independently of broad navigation scopes;
require a user action for permission prompts and Android notification permission;
reflect revocation in `Notification.permission`. Limit payload sizes and posting rate,
and validate click URLs. Unsupported WebView features should leave the feature
unavailable rather than expose an unrestricted JavaScript interface. Notification
support code would be included only for apps enabling the option.

This would help sites that generate notifications in a live document, including
messages arriving over an existing WebSocket. A posted Android notification can
remain after the page closes, but **new messages are not guaranteed once Android
suspends or kills the page**. We would not claim Push API support merely because
notifications work. Calls inside an arbitrary site's service worker would remain
unsupported in this first step.

## Experimental second step: native push with explicit site integration

A native Web Push receiver could subscribe through a
[WebPush-compatible UnifiedPush distributor](https://unifiedpush.org/news/20221218_unifiedpush/),
keep per-origin subscription/encryption keys, and decrypt received messages. This
avoids keeping a hidden WebView or socket alive. It still needs an installed distributor
and a server willing to send to that subscription endpoint; Firebase-specific SDKs or
servers that insist on their own provider may be incompatible.

Transport alone is insufficient: the [Push API](https://www.w3.org/TR/push-api/)
delivers a message by starting the subscribed service worker and dispatching `push`.
WebView's public [service-worker controller](https://developer.android.com/reference/androidx/webkit/ServiceWorkerControllerCompat)
exposes settings and request interception, not a native method to start a worker and
dispatch that event. An inference from this API surface is that a Kotlin receiver
cannot transparently execute an arbitrary site's existing push handler.

For cooperating sites or explicit per-site adapters, native code could instead map
the decrypted payload's title, body and click URL into an Android notification. This
would be documented as a separate integration, not silently presented as full Web
Push compatibility. Returning apparently valid subscriptions without a working
receiver and payload handler would be avoided.

## If generic background Web Push is required

A separate engine option using GeckoView is a more credible route than a large
WebView service-worker shim: its
[WebPushController](https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/WebPushController.html)
exposes push-event delivery for a worker scope, including decrypted payloads.
Native subscription transport, permission handling and Android notifications are
still required. Bundling another browser engine increases APK size and introduces
engine updates and profile compatibility work, so it conflicts with the current
small-APK goal and should be a separate optional template/edition.

The recommended sequence is to prototype the live-page notification bridge first,
measure compatibility with representative sites, and consider native push adapters
or a separate GeckoView edition only if background delivery is needed.
