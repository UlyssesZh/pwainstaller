# PWA Installer

An Android app that installs independent web apps backed by the system WebView.

## Use

Tap **+**, enter the website URL, and save.
The installer discovers `<link rel="manifest">`, downloads metadata and icons, and asks Android to install the generated APK.

When adding an app without a manifest, page discovery also fills unset regular and dark theme colors from valid `theme-color` meta tags,
including `prefers-color-scheme` light/dark queries.

App URL scopes and patterns in the app settings determine which top-level URLs stay in the app.
Enter multiple URLs, one per line; matching any entry keeps the content in the app.
An ordinary scope URL such as `https://example.com/app/` includes its descendants on the same origin.
Fetching or applying a manifest replaces these entries with its resolved `scope` (or its default scope).
Wildcard patterns can also be used, for example `https://example.com/app/*` or `https://*.example.com/*`;
`*` matches any scheme, host, port, or path where supplied.

A reinstall with a different signing key offers uninstall/reinstall recovery.
The installer first exports and validates a private browsing-data backup.
Removal uses the selected system or Shizuku method, then installation proceeds and the backup is restored.

Global adblock settings accept one AdGuard filter URL per line.
Lines beginning with `#` are disabled and retained in settings and backups.
Each web app stores its own editable rule snapshot, initially selected from the global lists for its base URL.
Updating global lists never changes saved or installed apps.
Network blocking, exceptions, cosmetic CSS, ExtendedCSS, and recognized AdGuard scriptlets are supported.
Unsupported network transformations, arbitrary JavaScript rules, and HTML response rewriting are skipped.

User JavaScript runs once when each main document's DOM is ready, in the page context,
with a separate function scope and error handling for each file.
CSS is inserted into that document; embedded frames are excluded.
These are plain JavaScript/CSS files, without userscript-manager APIs such as `GM_*` or interpretation of userscript metadata headers.
CSS resource URLs resolve relative to the page and remain subject to its loading policy.

## Development

Install a JDK compatible with Gradle (17 or newer), Android SDK platform 37, and build tools 36.0.0.
Set `ANDROID_HOME` or put your SDK path in an untracked `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

```sh
./gradlew :app:assembleDebug
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug
```

The installer APK is `app/build/outputs/apk/debug/app-debug.apk`.
Gradle automatically builds both unsigned runtime templates and embeds them as assets.

Release builds use `./gradlew :app:assembleRelease` and need your own installer signing configuration before distribution.
Keep the installer certificate stable: generated apps pin it for profile access.

With a running emulator or connected device:

```sh
./gradlew :app:connectedDebugAndroidTest
```

Device tests serve a local fixture website on the device.

## License

PWA Installer's original code is licensed under the MIT License.

Bundled AdGuard ExtendedCSS and Scriptlets use GPL-3.0;
their license also applies to those components when included in generated APKs.

Core-js uses MIT.

Public Suffix List uses MPL-2.0.
