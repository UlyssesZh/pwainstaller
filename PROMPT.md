Develop an Android app called "PWA Installer" (package name `io.github.ulysseszh.pwainstaller`)
that generates and installs apps that run progressive web apps on system WebView.
In the following, "the app" refers to PWA Installer, and "web app" refers to an app installed by PWA Installer.

## Web apps list

The landing page of the app is a list of web apps that were installed by the app.
This list is maintained in a database in the app's private storage.

Each list item has an app icon, an app name, and a menu button clicking which gives the user more options.
Clicking the list item outside the menu button launches the web app.
If a web app has been uninstalled, the list item grays out, but the menu button still works.
Long press a list item is equivalent to clicking the menu button.

In the menu given by clicking the menu button of a list item, there are the following items:

- Reinstall: reinstall the web app with the same metadata as before.
- Fetch manifest: fetch `manifest.json` again and update the metadata according to fetched manifest.
- Edit: edit metadata directly.
- Delete: delete the web app from the list. It opens a confirmation dialogue, in which the user can also check a box to uninstall the web app as well if the web app is still installed.
- Info: open the app info page of the web app in system settings. Grayed out if the web app is not installed.
- Export data: export browsing data and save as a file.
- Import data: import browsing data by selecting a file.
- Export APK.

There is a floating button with a plus sign in the corner of the screen for adding a new web app.

## New web app

In the menu for creating a new web app, there is a text box for entering the base URL of the web app.
All other options are hidden in expandable advanced settings.

There is a checkbox for whether the manifest is used.
By default it is checked.

If the manifest is used, there is a text box for entering the manifest URL.
If the text box is empty, look for the `<link rel="manifest">` element in the web page to get the manifest.

If the manifest is not used, all manifest items that are supported by the web manifest format and are relevant to an Android app can be manually edited.

There is an option for entering the package name for the installed web app.
By default, it is `io.github.ulysseszh.pwainstaller.<id>`,
where `<id>` is the SHA-1 hex digest of the web app ID (the resolved URL according to the `id` field of the manifest).

There is a text box for entering the headers for fetching the web page and the manifest.

There is a checkbox indicating whether the added web app will be installed.
It is checked by default.
Otherwise, only fetch the metadata and add to the web apps list but not install the web app.

If fetching fails and the error indicates a problem of missing credentials,
prompt the user whether they want to log in.
If yes, open a web view and go to the base URL.
The user can do anything that they could do in a general browser.
There is a check mark button at the top bar of this interface,
clicking which the cookies populated in the process will be used to try fetching again.

## Editing a web app

In the interface for editing a web app, all metadata can be manually edited.
This include all options that can be tweaked when adding a web app.
The manifest URL is separate metadata from other metadata, and they can be edited independently.
There is a button after the text box for the manifest URL, clicking which populates
other metadata by fetching and parsing the manifest.

If the user edits the package name and the web app is currently installed,
prompt whether they want to migrate the old web app to the new web app.
If they want to migrate, install the web app with the new package name and migrate the browsing data,
and then uninstall the old web app.

## Additional menu

At the top bar of the web apps list, there is a menu button clicking which gives an additional menu
including the following items:

- Export: export the metadata of all web apps and save as a file.
- Import: import the metadata of web apps by selecting a file.
- Signing key: see below.
- About.

The user can upload their own keystore for signing web apps,
and the user can edit all relevant information involving signing (e.g., the key alias and the store password).
The signing key used for a web app is not stored in its metadata.

## Web app

The version name of a installed web app is always `1.0.0`, and the version code is always `1`.

The web app allows the app to access the browsing data.
Only the app with the correct package name and signing cert can have access.
This is used for exporting and migrating browsing data.

## Miscellaneous

The minimum API level is 21.
The target API level is 37.
The Java compatibility version is 11.

The app is internationalized.
It supports en-US and zh-CN.

The UI of the app follows material design with dynamic color.Develop an Android app called "PWA Installer" (package name `io.github.ulysseszh.pwainstaller`)
that generates and installs apps that run progressive web apps on system WebView.
In the following, "the app" refers to PWA Installer, and "web app" refers to an app installed by PWA Installer.

## Web apps list

The landing page of the app is a list of web apps that were installed by the app.
This list is maintained in a database in the app's private storage.

Each list item has an app icon, an app name, and a menu button clicking which gives the user more options.
Clicking the list item outside the menu button launches the web app.
If a web app has been uninstalled, the list item grays out, but the menu button still works.
Long press a list item is equivalent to clicking the menu button.

In the menu given by clicking the menu button of a list item, there are the following items:

- Reinstall: reinstall the web app with the same metadata as before.
- Fetch manifest: fetch `manifest.json` again and update the metadata according to fetched manifest.
- Edit: edit metadata directly.
- Delete: delete the web app from the list. It opens a confirmation dialogue, in which the user can also check a box to uninstall the web app as well if the web app is still installed.
- Info: open the app info page of the web app in system settings. Grayed out if the web app is not installed.
- Export data: export browsing data and save as a file.
- Import data: import browsing data by selecting a file.

There is a floating button with a plus sign in the corner of the screen for adding a new web app.

## New web app

In the menu for creating a new web app, there is a text box for entering the base URL of the web app.
All other options are hidden in expandable advanced settings.

There is a checkbox for whether the manifest is used.
By default it is checked.

If the manifest is used, there is a text box for entering the manifest URL.
If the text box is empty, look for the `<link rel="manifest">` element in the web page to get the manifest.

If the manifest is not used, all manifest items that are supported by the web manifest format and are relevant to an Android app can be manually edited.

There is an option for entering the package name for the installed web app.
By default, it is `io.github.ulysseszh.pwainstaller.<id>`,
where `<id>` is the SHA-1 hex digest of the web app ID (the resolved URL according to the `id` field of the manifest).

There is a text box for entering the headers for fetching the web page and the manifest.

There is a checkbox indicating whether the added web app will be installed.
It is checked by default.
Otherwise, only fetch the metadata and add to the web apps list but not install the web app.

If fetching fails and the error indicates a problem of missing credentials,
prompt the user whether they want to log in.
If yes, open a web view and go to the base URL.
The user can do anything that they could do in a general browser.
There is a check mark button at the top bar of this interface,
clicking which the cookies populated in the process will be used to try fetching again.

## Editing a web app

In the interface for editing a web app, all metadata can be manually edited.
This include all options that can be tweaked when adding a web app.
The manifest URL is separate metadata from other metadata, and they can be edited independently.
There is a button after the text box for the manifest URL, clicking which populates
other metadata by fetching and parsing the manifest.

If the user edits the package name and the web app is currently installed,
prompt whether they want to migrate the old web app to the new web app.
If they want to migrate, install the web app with the new package name and migrate the browsing data,
and then uninstall the old web app.

## Additional menu

At the top bar of the web apps list, there is a menu button clicking which gives an additional menu
including the following items:

- Export: export the metadata of all web apps and save as a file.
- Import: import the metadata of web apps by selecting a file.
- Signing key: see below.
- About.

The user can upload their own keystore for signing web apps,
and the user can edit all relevant information involving signing (e.g., the key alias and the store password).
The signing key used for a web app is not stored in its metadata.

## Web app

The version name of a installed web app is always `1.0.0`, and the version code is always `1`.

The web app allows the app to access the browsing data.
Only the app with the correct package name and signing cert can have access.
This is used for exporting and migrating browsing data.

## Miscellaneous

The minimum API level is 21.
The target API level is 37.
The Java compatibility version is 11.

The app is internationalized.
It supports en-US and zh-CN.

The UI of the app follows material design with dynamic color.
