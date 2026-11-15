package io.github.ulysseszh.pwainstaller

import android.content.Context
import android.security.KeyPairGeneratorSpec
import android.util.Base64
import io.github.ulysseszh.pwainstaller.core.PackageNames
import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.json.JSONObject
import java.io.*
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

object SigningKeys {
	private val provider = BouncyCastleProvider()
	private const val WRAP_ALIAS = "pwa-installer-secret-wrap"
	class Identity(@JvmField val key: PrivateKey, @JvmField val certificates: List<X509Certificate>)
	private var cachedSettings: Pair<String, String>? = null
	private var cachedIdentity: Pair<String, Identity>? = null

	@Synchronized fun settings(context: Context): JSONObject {
		val encrypted = context.getSharedPreferences("signing", 0).getString("settings", null)
		if (encrypted != null) {
			val plain = cachedSettings?.takeIf { it.first == encrypted }?.second ?: decrypt(context, encrypted).also {
				cachedSettings = encrypted to it
			}
			return JSONObject(plain)
		}
		val file = File(context.filesDir, "default.p12")
		if (file.exists()) throw IOException("Default key exists but its settings are missing; import a signing key to recover")
		val password = UUID.randomUUID().toString()
		val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(3072) }.generateKeyPair()
		val before = Date(System.currentTimeMillis() - 86400000L)
		val after = Date(System.currentTimeMillis() + 50L * 365 * 86400000L)
		val name = X500Principal("CN=PWA Installer")
		val cert = JcaX509CertificateConverter().setProvider(provider).getCertificate(
			JcaX509v3CertificateBuilder(name, BigInteger(128, SecureRandom()), before, after, name, pair.public)
				.build(JcaContentSignerBuilder("SHA256withRSA").setProvider(provider).build(pair.private))
		)
		val store = KeyStore.getInstance("PKCS12", provider).apply {
			load(null as InputStream?, password.toCharArray())
			setKeyEntry("pwa", pair.private, password.toCharArray(), arrayOf(cert))
		}
		val temporary = File(context.filesDir, "default.p12.tmp")
		temporary.outputStream().use { store.store(it, password.toCharArray()) }
		val config = JSONObject().put("file", file.name).put("type", "PKCS12").put("alias", "pwa")
			.put("storePassword", password).put("keyPassword", password)
		save(context, config)
		if (!temporary.renameTo(file)) throw IOException("Cannot persist default key")
		return config
	}

	@Synchronized fun save(context: Context, settings: JSONObject) {
		val plain = settings.toString(); val encrypted = encrypt(context, plain)
		if (!context.getSharedPreferences("signing", 0).edit().putString("settings", encrypted).commit())
			throw IOException("Cannot save signing settings")
		cachedSettings = encrypted to plain; cachedIdentity = null
	}
	@Synchronized fun load(context: Context): Identity {
		val config = settings(context)
		val file = File(context.filesDir, config.getString("file"))
		val stamp = "${context.filesDir}:$config:${file.length()}:${file.lastModified()}"
		cachedIdentity?.takeIf { it.first == stamp && file.isFile }?.let { return it.second }
		return load(file, config.getString("type"), config.getString("alias"), config.getString("storePassword"), config.getString("keyPassword"))
			.also { cachedIdentity = stamp to it }
	}

	@JvmStatic fun load(file: File, type: String, alias: String, storePassword: String, keyPassword: String): Identity {
		val identity = if (type == "JKS") readJks(file, alias, storePassword, keyPassword) else {
			val store = KeyStore.getInstance(type, provider)
			file.inputStream().use { store.load(it, storePassword.toCharArray()) }
			val key = store.getKey(alias, keyPassword.toCharArray()) as? PrivateKey ?: throw GeneralSecurityException("Alias has no private key")
			val chain = store.getCertificateChain(alias)?.takeIf { it.isNotEmpty() } ?: throw GeneralSecurityException("Missing signing certificate")
			Identity(key, chain.map { it as X509Certificate })
		}
		val algorithm = identity.key.algorithm
		require(algorithm == "RSA" || algorithm == "EC") { "Use an RSA or EC signing key" }
		val proof = Signature.getInstance(if (algorithm == "RSA") "SHA256withRSA" else "SHA256withECDSA")
		val challenge = ByteArray(32).also { SecureRandom().nextBytes(it) }
		proof.initSign(identity.key)
		proof.update(challenge)
		val signed = proof.sign()
		proof.initVerify(identity.certificates.first())
		proof.update(challenge)
		if (!proof.verify(signed)) throw GeneralSecurityException("Key and certificate do not match")
		return identity
	}

	private fun passwordBytes(password: String) = password.toByteArray(Charsets.UTF_16BE)
	private fun readJks(file: File, alias: String, storePassword: String, keyPassword: String): Identity {
		val bytes = file.inputStream().use { ManifestFetcher.read(it, 16 * 1024 * 1024) }
		if (bytes.size < 32) throw IOException("Invalid JKS")
		val digest = MessageDigest.getInstance("SHA-1")
		digest.update(passwordBytes(storePassword))
		digest.update("Mighty Aphrodite".toByteArray(Charsets.UTF_8))
		digest.update(bytes, 0, bytes.size - 20)
		if (!MessageDigest.isEqual(digest.digest(), bytes.copyOfRange(bytes.size - 20, bytes.size))) throw GeneralSecurityException("Wrong store password or corrupt JKS")
		DataInputStream(ByteArrayInputStream(bytes, 0, bytes.size - 20)).use { input ->
			if (input.readInt() != 0xfeedfeed.toInt()) throw IOException("Invalid JKS")
			val version = input.readInt()
			if (version !in 1..2) throw IOException("Unsupported JKS version")
			val count = input.readInt()
			if (count !in 0..10000) throw IOException("Invalid JKS entry count")
			repeat(count) {
				val tag = input.readInt()
				val entryAlias = input.readUTF()
				input.readLong()
				when (tag) {
					1 -> {
						val encrypted = block(input)
						val certCount = input.readInt()
						if (certCount !in 1..100) throw IOException("Invalid certificate chain")
						val certs = List(certCount) {
							if (version == 2) input.readUTF()
							CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(block(input))) as X509Certificate
						}
						if (entryAlias.equals(alias, true)) {
							val info = EncryptedPrivateKeyInfo.getInstance(encrypted)
							if (info.encryptionAlgorithm.algorithm.id != "1.3.6.1.4.1.42.2.17.1.1") throw GeneralSecurityException("Unsupported JKS key protection")
							val protectedKey = info.encryptedData
							if (protectedKey.size < 40) throw IOException("Corrupt protected key")
							val password = passwordBytes(keyPassword)
							var previous = protectedKey.copyOfRange(0, 20)
							val plain = ByteArray(protectedKey.size - 40)
							for (offset in plain.indices step 20) {
								digest.reset(); digest.update(password)
								previous = digest.digest(previous)
								repeat(minOf(20, plain.size - offset)) { k -> plain[offset + k] = (protectedKey[20 + offset + k].toInt() xor previous[k].toInt()).toByte() }
							}
							digest.reset(); digest.update(password)
							if (!MessageDigest.isEqual(digest.digest(plain), protectedKey.copyOfRange(protectedKey.size - 20, protectedKey.size))) throw GeneralSecurityException("Wrong key password")
							return Identity(KeyFactory.getInstance(certs.first().publicKey.algorithm).generatePrivate(PKCS8EncodedKeySpec(plain)), certs)
						}
					}
					2 -> { if (version == 2) input.readUTF(); block(input) }
					else -> throw IOException("Invalid JKS entry")
				}
			}
		}
		throw GeneralSecurityException("Key alias not found")
	}
	private fun block(input: DataInputStream): ByteArray {
		val n = input.readInt()
		if (n < 0 || n > input.available()) throw IOException("Invalid JKS length")
		return ByteArray(n).also(input::readFully)
	}

	@Synchronized private fun wrappingStore(context: Context): KeyStore {
		val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
		if (!store.containsAlias(WRAP_ALIAS)) {
			val end = Calendar.getInstance().apply { add(Calendar.YEAR, 50) }
			val generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
			generator.initialize(KeyPairGeneratorSpec.Builder(context).setAlias(WRAP_ALIAS)
				.setSubject(X500Principal("CN=PWA Installer Settings")).setSerialNumber(BigInteger.ONE)
				.setStartDate(Date()).setEndDate(end.time).setKeySize(2048).build())
			generator.generateKeyPair()
		}
		return store
	}
	private fun encrypt(context: Context, plain: String): String {
		val random = SecureRandom()
		val key = ByteArray(32).also(random::nextBytes)
		val iv = ByteArray(12).also(random::nextBytes)
		val aes = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv)) }
		val rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding").apply { init(Cipher.ENCRYPT_MODE, wrappingStore(context).getCertificate(WRAP_ALIAS).publicKey) }
		return listOf(rsa.doFinal(key), iv, aes.doFinal(plain.toByteArray(Charsets.UTF_8))).joinToString(":", transform = ::b64)
	}
	private fun decrypt(context: Context, encrypted: String): String {
		val parts = encrypted.split(':')
		if (parts.size != 3) throw GeneralSecurityException("Invalid encrypted settings")
		val rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding").apply { init(Cipher.DECRYPT_MODE, wrappingStore(context).getKey(WRAP_ALIAS, null)) }
		val key = rsa.doFinal(Base64.decode(parts[0], 0))
		val aes = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, Base64.decode(parts[1], 0))) }
		return aes.doFinal(Base64.decode(parts[2], 0)).toString(Charsets.UTF_8)
	}
	private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
}
