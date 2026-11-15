# AdGuard helpers

The installer vendors these upstream JavaScript helpers to execute the AdGuard rules saved with a web app:

- [ExtendedCss 2.3.0](https://registry.npmjs.org/@adguard/extended-css/-/extended-css-2.3.0.tgz), GPL-3.0. Source: [AdGuard ExtendedCss](https://github.com/AdguardTeam/ExtendedCss).
- [Scriptlets 2.5.1](https://registry.npmjs.org/@adguard/scriptlets/-/scriptlets-2.5.1.tgz), GPL-3.0. Source: [AdGuard Scriptlets](https://github.com/AdguardTeam/Scriptlets).
- [core-js-bundle 3.51.0](https://www.npmjs.com/package/core-js-bundle/v/3.51.0), MIT, for older system WebViews.

Original license texts are included here and in APKs that use these helpers. The vendored files are transpiled for Chrome 37 using the exact Babel versions in BUILD-TOOLS.txt. `app/src/main/assets/adguard/runner.js` is this project's WebView integration. `AdblockScript.kt` selects only the scriptlet implementations referenced by a web app's own rules; ExtendedCss and polyfills are included only when needed. Full upstream filter lists are downloaded by the installer, not bundled into generated apps.

`vendor.cjs` reproduces the transformation. Install the versions in BUILD-TOOLS.txt in a separate directory and set NODE_PATH to its node_modules. Extract the upstream archives into a directory containing `extended/extended-css.js`, `extended/LICENSE`, `scriptlets/scriptlets.corelibs.json`, and `scriptlets/LICENSE`, then run `node third_party/adguard/vendor.cjs /path/to/input`.

The Public Suffix List used for network rules is retained verbatim in `core/src/main/resources/public_suffix_list.dat`, including its Mozilla Public License notice and source URL.
