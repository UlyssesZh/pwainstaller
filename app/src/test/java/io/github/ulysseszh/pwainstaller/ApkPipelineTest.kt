package io.github.ulysseszh.pwainstaller

import com.android.apksig.ApkVerifier
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.*
import java.security.cert.X509Certificate
import java.util.Date
import java.util.zip.ZipFile
import javax.security.auth.x500.X500Principal

class ApkPipelineTest {
    @get:Rule val temp = TemporaryFolder()
    private lateinit var pair: KeyPair
    private lateinit var cert: X509Certificate
    @Before fun identity() {
        pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val provider = BouncyCastleProvider()
        val name = X500Principal("CN=PWA Pipeline Test")
        cert = JcaX509CertificateConverter().setProvider(provider).getCertificate(
            JcaX509v3CertificateBuilder(name, BigInteger.ONE, Date(0), Date(4102444800000L), name, pair.public)
                .build(JcaContentSignerBuilder("SHA256withRSA").setProvider(provider).build(pair.private))
        )
    }
    @Test fun loadsJksPkcs12AndBksAndRejectsWrongPasswords() {
        listOf("JKS", "PKCS12", "BKS").forEach { type ->
            val store = if (type == "JKS") KeyStore.getInstance(type) else KeyStore.getInstance(type, BouncyCastleProvider())
            store.load(null as InputStream?, "store-password".toCharArray())
            store.setKeyEntry("my-alias", pair.private, "key-password".toCharArray(), arrayOf(cert))
            val file = temp.newFile().also { it.outputStream().use { output -> store.store(output, "store-password".toCharArray()) } }
            val identity = SigningKeys.load(file, type, "my-alias", "store-password", "key-password")
            assertArrayEquals(pair.private.encoded, identity.key.encoded); assertArrayEquals(cert.encoded, identity.certificates.first().encoded)
            assertThrows(Exception::class.java) { SigningKeys.load(file, type, "my-alias", "wrong", "key-password") }
            if (type != "PKCS12") assertThrows(Exception::class.java) { SigningKeys.load(file, type, "my-alias", "store-password", "wrong") }
            assertThrows(Exception::class.java) { SigningKeys.load(file, type, "missing", "store-password", "key-password") }
        }
    }
    @Test fun generatesVerifiableAlignedApkFromRealRuntimeWithPinnedInstaller() {
        val app = WebApp().apply {
            set("baseUrl", "https://example.com/app/")
            set("webViewDebugging", true)
            set("useNotchAreas", false)
            set("darkThemeColor", "#8bd5ca"); set("darkBackgroundColor", "#20242a")
            listOf(64 to "any", 192 to "any", 512 to "any", 256 to "maskable", 128 to "monochrome").forEach { (size, purpose) ->
                val encoded = testPng(size)
                icons().put(JSONObject().put("src", "upload:$size").put("purpose", purpose)
                    .put("data", encoded))
            }
            manifest().put("name", "Test 应用 🐉 " + "A".repeat(160)).put("id", "/stable-identity")
            validate()
        }
        val template = File("../runtime/build/outputs/apk/release/runtime-release-unsigned.apk")
        assertTrue("Build the runtime template first", template.isFile)
        val apk = ApkBuilder.assemble(template.inputStream(), temp.newFolder(), app, SigningKeys.Identity(pair.private, listOf(cert)), "pinned-fingerprint")
        val verification = ApkVerifier.Builder(apk).setMinCheckedPlatformVersion(21).build().verify()
        assertTrue(verification.errors.toString(), verification.isVerified)
        assertTrue(verification.isVerifiedUsingV1Scheme); assertTrue(verification.isVerifiedUsingV2Scheme)
        assertArrayEquals(cert.encoded, verification.signerCertificates.first().encoded)
        ZipFile(apk).use { zip ->
            val config = JSONObject(zip.getInputStream(zip.getEntry("assets/webapp.json")).bufferedReader().readText())
            assertTrue(config.getBoolean("webViewDebugging"))
            assertFalse(config.getBoolean("useNotchAreas"))
            assertEquals("#8bd5ca", config.getString("darkThemeColor")); assertEquals("#20242a", config.getString("darkBackgroundColor"))
            assertArrayEquals(IconData.decode(app.s("icon")), zip.getInputStream(zip.getEntry("res/drawable/web_icon.png")).readBytes())
            assertEquals(5, config.getJSONArray("icons").length())
            val densitySizes = mapOf("mdpi" to 64, "hdpi" to 192, "xhdpi" to 192, "xxhdpi" to 192, "xxxhdpi" to 192)
            densitySizes.forEach { (density, expected) ->
                fun width(name: String): Int {
                    val bytes = zip.getInputStream(zip.getEntry("res/mipmap-$density-v4/$name.png")).readBytes()
                    return ByteBuffer.wrap(bytes).getInt(16)
                }
                assertEquals(expected, width("web_icon"))
                assertEquals(256, width("web_adaptive_icon"))
                assertEquals(128, width("web_monochrome_icon"))
            }
            repeat(5) { index -> assertArrayEquals(IconData.decode(app.icons().getJSONObject(index).getString("data")),
                zip.getInputStream(zip.getEntry("assets/icons/$index.png")).readBytes()) }
            ZipFile(template).use { original ->
                assertArrayEquals(original.getInputStream(original.getEntry("res/drawable-v26/web_maskable_foreground.xml")).readBytes(),
                    zip.getInputStream(zip.getEntry("res/drawable-v26/web_icon_foreground.xml")).readBytes())
                assertArrayEquals(original.getInputStream(original.getEntry("res/mipmap-anydpi-v33/web_launcher.xml")).readBytes(),
                    zip.getInputStream(zip.getEntry("res/mipmap-anydpi-v33/web_launcher.xml")).readBytes())
            }
            assertEquals("pinned-fingerprint", config.getString("installerCertificate")); assertEquals("https://example.com/stable-identity", config.getString("id"))
            val manifest = zip.getInputStream(zip.getEntry("AndroidManifest.xml")).readBytes()
            listOf("DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION", "androidx-startup", "ProfileInstallReceiver").forEach { unused ->
                assertFalse(unused, manifest.toString(Charsets.UTF_16LE).contains(unused) || manifest.toString(Charsets.UTF_8).contains(unused))
            }
            assertEquals(manifest.size, ByteBuffer.wrap(manifest).order(ByteOrder.LITTLE_ENDIAN).getInt(4))
            assertTrue(manifest.toString(Charsets.UTF_16LE).contains(app.s("packageName")) || manifest.toString(Charsets.UTF_8).contains(app.s("packageName")))
            assertTrue(manifest.toString(Charsets.UTF_16LE).contains("${app.s("packageName")}.images") || manifest.toString(Charsets.UTF_8).contains("${app.s("packageName")}.images"))
            assertFalse(manifest.toString(Charsets.UTF_16LE).contains("io.github.ulysseszh.pwainstaller.template.images") || manifest.toString(Charsets.UTF_8).contains("io.github.ulysseszh.pwainstaller.template.images"))
        }
        val bytes = apk.readBytes()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var resources = false
        // Compressed entries can use data descriptors, so locate the resource local header directly.
        for (position in 0 until bytes.size - 30) {
            if (buffer.getInt(position) != 0x04034b50) continue
            val nameLength = buffer.getShort(position + 26).toInt() and 65535
            val extraLength = buffer.getShort(position + 28).toInt() and 65535
            if (position + 30 + nameLength + extraLength > bytes.size) continue
            val name = String(bytes, position + 30, nameLength, Charsets.UTF_8)
            if (name == "resources.arsc") {
                resources = true
                assertEquals(0, (position + 30 + nameLength + extraLength) % 4)
                assertEquals(0, buffer.getShort(position + 8).toInt())
            }
        }
        ZipFile(apk).use { zip ->
            assertEquals(java.util.zip.ZipEntry.DEFLATED, zip.getEntry("classes.dex").method)
            assertEquals(java.util.zip.ZipEntry.DEFLATED, zip.getEntry("assets/webapp.json").method)
        }
        assertTrue("Resource table must remain uncompressed and aligned", resources)
    }
}
