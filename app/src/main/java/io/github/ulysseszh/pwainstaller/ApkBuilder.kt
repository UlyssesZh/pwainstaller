package io.github.ulysseszh.pwainstaller

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.android.apksig.ApkSigner
import com.android.apksig.KeyConfig
import io.github.ulysseszh.pwainstaller.browser.PackageSignatures
import io.github.ulysseszh.pwainstaller.core.ApkProgress
import io.github.ulysseszh.pwainstaller.core.BinaryResources
import io.github.ulysseszh.pwainstaller.core.BinaryXml
import io.github.ulysseszh.pwainstaller.core.CssColors
import io.github.ulysseszh.pwainstaller.core.PackageNames
import org.json.JSONObject
import java.io.*
import java.net.URI
import java.util.zip.*

object ApkBuilder {
	@JvmStatic fun build(
		context: Context,
		app: WebApp,
		progress: (ApkProgress) -> Unit = {},
	): File = buildLogged(context, app, progress, {})

	fun buildLogged(
		context: Context,
		app: WebApp,
		progress: (ApkProgress) -> Unit,
		log: (String) -> Unit,
	): File {
		progress(ApkProgress(ApkProgress.Stage.VALIDATING))
		log("Validating metadata for ${app.s("packageName")}")
		app.validate()
		progress(ApkProgress(ApkProgress.Stage.KEY))
		log("Loading signing key")
		val identity = SigningKeys.load(context)
		log("Signing certificate SHA-256: ${PackageNames.digest("SHA-256", identity.certificates.first().encoded)}")
		log("Compiling ${app.s("adblockRules").toByteArray(Charsets.UTF_8).size} bytes of adblock rules")
		progress(ApkProgress(ApkProgress.Stage.FILTERS))
		// Updating the installer also invalidates cached assets when bundled helpers change.
		val compiler =
			CompiledFilters(
				File(context.cacheDir, "compiled-filters"),
				context.packageManager
					.getPackageInfo(context.packageName, 0)
					.lastUpdateTime
					.toString(),
			)
		val filters =
			compiler.get(
				app.s("adblockRules"),
				{ path ->
					context.assets
						.open(path)
						.bufferedReader()
						.use { it.readText() }
				},
				{ percent -> progress(ApkProgress(ApkProgress.Stage.FILTERS, percent)) },
				log,
			)
		log("Packaging ${app.icons().length()} original icons and generated launcher resources")
		return assemble(
			context.assets.open(
				if (app.displayMode() ==
					"browser"
				) {
					"runtime-ui.apk"
				} else {
					"runtime.apk"
				},
			),
			File(context.cacheDir, "apks"),
			app,
			identity,
			installerCertificate(context),
			progress,
			filters,
		).also { log("Signed APK: ${it.length()} bytes; ${app.s("packageName")}") }
	}

	@JvmStatic fun assemble(
		template: InputStream,
		dir: File,
		app: WebApp,
		identity: SigningKeys.Identity,
		installerFingerprint: String,
		progress: (ApkProgress) -> Unit = {
		},
		filterAssets: Map<String, ByteArray> = emptyMap(),
	): File {
		if (!dir.mkdirs() && !dir.isDirectory) {
			throw IOException("Cannot create APK directory")
		}
		val unsigned = File.createTempFile("unsigned-", ".apk", dir)
		val signed = File.createTempFile("webapp-", ".apk", dir)
		val scope = URI(app.manifest().getString("scope"))
		val replacements =
			mapOf(
				"io.github.ulysseszh.pwainstaller.template" to app.s("packageName"),
				"io.github.ulysseszh.pwainstaller.template.data" to "${app.s("packageName")}.data",
				"io.github.ulysseszh.pwainstaller.template.images" to "${app.s("packageName")}.images",
				"PWA_TEMPLATE_LABEL" to app.name(),
				"pwa-template-scheme" to scope.scheme,
				"pwa-template-host" to scope.host,
				"/pwa-template-path" to scope.path.ifEmpty { "/" },
			)
		val config =
			JSONObject(app.manifest().toString())
				.put("installerCertificate", installerFingerprint)
				.put("adblockRules", app.s("adblockRules"))
				.put("baseUrl", app.s("baseUrl"))
				.put("urlPatterns", app.s("urlPatterns"))
				.put("userAgent", app.s("userAgent"))
				.put("darkThemeColor", app.s("darkThemeColor"))
				.put("darkBackgroundColor", app.s("darkBackgroundColor"))
				.put("webViewDebugging", app.b("webViewDebugging"))
				.put("useNotchAreas", app.b("useNotchAreas"))
				.put("manifestBase", app.s("manifestUrl").ifEmpty { app.s("baseUrl") })
		val userFiles = UserContentFiles.read(app.data)
		config.put(
			"userContent",
			org.json.JSONArray(
				userFiles.mapIndexed { index, entry ->
					JSONObject()
						.put("marker", UserContentFiles.marker(entry))
						.put("asset", "user-content/$index.js")
						.put("name", entry.name)
						.put("kind", entry.kind.name)
				},
			),
		)
		try {
			progress(ApkProgress(ApkProgress.Stage.TEMPLATE))
			val entries = linkedMapOf<String, ByteArray>()
			ZipInputStream(template).use { input ->
				while (true) {
					val entry = input.nextEntry ?: break
					if (!entry.isDirectory && !entry.name.startsWith("META-INF/")) {
						entries[entry.name] = ManifestFetcher.read(input, 32 * 1024 * 1024)
					}
				}
			}
			progress(ApkProgress(ApkProgress.Stage.ICONS, 0))
			val densities = mapOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192)
			val hasMaskable = IconData.best(app.icons(), 192, "maskable") != null
			val hasMonochrome = IconData.best(app.icons(), 192, "monochrome") != null
			val renderedIcons = mutableMapOf<Pair<JSONObject, Int>, ByteArray>()

			fun image(
				target: Int,
				purpose: String,
			): ByteArray {
				val icon =
					IconData.bestEntry(app.icons(), target, purpose) ?: IconData.bestEntry(app.icons(), target, "any")
						?: IconData.bestEntry(app.icons(), target, "maskable") ?: return IconData.decode(app.s("icon"))
				return renderedIcons.getOrPut(icon to target) { IconData.render(icon, target) }
			}
			val densityPattern =
				Regex("res/mipmap-(mdpi|hdpi|xhdpi|xxhdpi|xxxhdpi)(?:-v[0-9]+)?/(web_icon|web_adaptive_icon|web_monochrome_icon)\\.png")
			val densityEntries = entries.keys.mapNotNull { name -> densityPattern.matchEntire(name)?.let { name to it } }
			val densityImages =
				densityEntries
					.mapIndexed { index, (name, match) ->
						val purpose =
							when (match.groupValues[2]) {
								"web_adaptive_icon" -> {
									if (hasMaskable) {
										"maskable"
									} else {
										"any"
									}
								}

								"web_monochrome_icon" -> {
									"monochrome"
								}

								else -> {
									"any"
								}
							}
						val target = densities.getValue(match.groupValues[1])
						// Maskable artwork fills 76% of Android's 108dp adaptive canvas.
						val pixels =
							if (match.groupValues[2] == "web_adaptive_icon" && hasMaskable) {
								kotlin.math.ceil(target * 108.0 / 48 * .76).toInt()
							} else {
								target
							}
						val bytes = image(pixels, purpose)
						progress(ApkProgress(ApkProgress.Stage.ICONS, (index + 1) * 100 / densityEntries.size))
						name to bytes
					}.toMap()
			run {
				CountingOutputStream(unsigned.outputStream()).use { counter ->
					ZipOutputStream(counter).use { output ->
						output.setLevel(Deflater.BEST_COMPRESSION)
						var iconWritten = false
						var densityIconsWritten = 0
						progress(ApkProgress(ApkProgress.Stage.RESOURCES, 0))
						var completed = 0
						for ((name, original) in entries) {
							if (name == "assets/webapp.json") {
								continue
							}
							var bytes = original
							if (name in densityImages) {
								bytes = densityImages.getValue(name)
								densityIconsWritten++
							}
							if (name == "res/drawable-v26/web_icon_foreground.xml" && hasMaskable) {
								bytes = entries.getValue("res/drawable-v26/web_maskable_foreground.xml")
							}
							if (name.startsWith("res/mipmap-anydpi-v33/") && !hasMonochrome) {
								bytes = entries.getValue(name.replace("-v33/", "-v26/"))
							}
							if (name == "resources.arsc") {
								val theme = app.manifest().optString("theme_color")
								val background = app.manifest().optString("background_color")
								val light =
									io.github.ulysseszh.pwainstaller.core.WebAppColors
										.resolve(theme, background)
								val dark =
									io.github.ulysseszh.pwainstaller.core.WebAppColors.resolve(
										theme,
										background,
										app.s("darkThemeColor"),
										app.s("darkBackgroundColor"),
										true,
									)
								bytes =
									BinaryResources.colors(
										bytes,
										mapOf(
											BinaryResources.BACKGROUND to light.background,
											BinaryResources.THEME to (light.theme ?: light.background),
											BinaryResources.DARK_BACKGROUND to dark.background,
											BinaryResources.DARK_THEME to (dark.theme ?: dark.background),
										),
									)
							}
							if (name ==
								"AndroidManifest.xml"
							) {
								bytes =
									BinaryXml.intentFilters(
										BinaryXml.replace(bytes, replacements),
										ManifestFeatures.filters(app.manifest(), app.s("baseUrl"), app.s("urlPatterns")),
									)
							}
							if (name == "res/drawable/web_icon.png") {
								bytes = IconData.decode(app.s("icon"))
								iconWritten = true
							}
							putAligned(output, counter, name, bytes)
							progress(ApkProgress(ApkProgress.Stage.RESOURCES, ++completed * 100 / entries.size))
						}
						filterAssets.forEach { (name, bytes) ->
							require(name == "assets/adblock.js" || name.startsWith("assets/licenses/"))
							putAligned(output, counter, name, bytes)
						}
						userFiles.forEachIndexed { index, entry ->
							putAligned(output, counter, "assets/user-content/$index.js", UserContentFiles.script(entry).toByteArray(Charsets.UTF_8))
						}
						app.data.optJSONObject("shortcutIcons")?.let { cached ->
							cached.keys().forEach { index ->
								require(index.toIntOrNull() in 0..15) { "Invalid shortcut icon" }
								putAligned(output, counter, "assets/shortcut-icons/$index.png", IconData.decode(cached.getString(index)))
							}
						}
						check(iconWritten && densityIconsWritten == 15) { "Template icon resources are missing" }
						repeat(app.icons().length()) { index ->
							val icon = app.icons().getJSONObject(index)
							putAligned(output, counter, "assets/icons/$index.png", IconData.decode(icon.getString("data")))
							IconData.source(icon)?.let { putAligned(output, counter, "assets/icons/$index.svg", it) }
						}
						config.put(
							"icons",
							org.json.JSONArray().apply {
								repeat(app.icons().length()) { index ->
									val icon = app.icons().getJSONObject(index)
									val vector = icon.has("sourceData")
									put(
										JSONObject()
											.put(
												"src",
												"icons/$index.${if (vector) {
													"svg"
												} else {
													"png"
												}}",
											).put(
												"sizes",
												if (vector) {
													"any"
												} else {
													IconData.sizes(icon.getString("data"))
												},
											).put(
												"type",
												if (vector) {
													"image/svg+xml"
												} else {
													"image/png"
												},
											).put("purpose", icon.optString("purpose", "any")),
									)
								}
							},
						)
						putAligned(output, counter, "assets/webapp.json", config.toString().toByteArray(Charsets.UTF_8))
					}
				}
			}
			progress(ApkProgress(ApkProgress.Stage.RESOURCES, 100))
			progress(ApkProgress(ApkProgress.Stage.SIGNING))
			val signer =
				ApkSigner.SignerConfig
					.Builder(
						"pwa",
						KeyConfig
							.Jca(identity.key),
						identity.certificates,
					).build()
			ApkSigner
				.Builder(listOf(signer))
				.setInputApk(unsigned)
				.setOutputApk(signed)
				.setMinSdkVersion(21)
				.setV1SigningEnabled(true)
				.setV2SigningEnabled(true)
				.setV3SigningEnabled(false)
				.setV4SigningEnabled(false)
				.build()
				.sign()
			progress(ApkProgress(ApkProgress.Stage.SIGNING, 100))
			return signed
		} catch (error: Exception) {
			signed.delete()
			throw error
		} finally {
			unsigned.delete()
		}
	}

	fun installerCertificate(context: Context): String {
		val info =
			context.packageManager.getPackageInfo(
				PackageNames.INSTALLER,
				PackageSignatures
					.flags(),
			)
		val signatures =
			PackageSignatures
				.read(info)
		require(signatures.size == 1) { "Installer must have a single signer" }
		return PackageNames.digest("SHA-256", signatures[0].toByteArray())
	}

	private fun putAligned(
		output: ZipOutputStream,
		counter: CountingOutputStream,
		name: String,
		bytes: ByteArray,
	) {
		val entry =
			ZipEntry(name).apply {
				method =
					if (name == "resources.arsc" || name.endsWith(".png")) {
						ZipEntry.STORED
					} else {
						ZipEntry.DEFLATED
					}
				size = bytes.size.toLong()
				if (method == ZipEntry.STORED) {
					compressedSize = size
				}
				crc = CRC32().apply { update(bytes) }.value
				time = 315532800000L
			}
		val padding = ((4 - ((counter.count + 30 + name.toByteArray(Charsets.UTF_8).size) and 3)) and 3).toInt()
		if (entry.method == ZipEntry.STORED &&
			padding != 0
		) {
			entry.extra =
				ByteArray(4 + padding).apply {
					this[0] = 0xfe.toByte()
					this[1] = 0xca.toByte()
					this[2] = padding.toByte()
				}
		}
		output.putNextEntry(entry)
		output.write(bytes)
		output.closeEntry()
	}

	private class CountingOutputStream(
		output: OutputStream,
	) : FilterOutputStream(output) {
		var count = 0L

		override fun write(b: Int) {
			out.write(b)
			count++
		}

		override fun write(
			bytes: ByteArray,
			offset: Int,
			length: Int,
		) {
			out.write(bytes, offset, length)
			count += length
		}
	}
}
