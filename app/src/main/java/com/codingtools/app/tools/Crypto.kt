package com.codingtools.app.tools

import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.crypto.digests.SHA3Digest
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.generators.OpenBSDBCrypt
import org.bouncycastle.crypto.generators.SCrypt
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.zip.Adler32
import java.util.zip.CRC32
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Crypt {
    val rnd = SecureRandom()
    fun digest(alg: String, b: ByteArray): ByteArray = when (alg) {
        "SHA3-256" -> SHA3Digest(256).let { d -> d.update(b, 0, b.size); ByteArray(32).also { o -> d.doFinal(o, 0) } }
        "SHA3-512" -> SHA3Digest(512).let { d -> d.update(b, 0, b.size); ByteArray(64).also { o -> d.doFinal(o, 0) } }
        "BLAKE2b-256" -> Blake2bDigest(256).let { d -> d.update(b, 0, b.size); ByteArray(32).also { o -> d.doFinal(o, 0) } }
        "BLAKE2b-512" -> Blake2bDigest(512).let { d -> d.update(b, 0, b.size); ByteArray(64).also { o -> d.doFinal(o, 0) } }
        "CRC32" -> CRC32().also { it.update(b) }.value.let { v -> byteArrayOf((v shr 24).toByte(), (v shr 16).toByte(), (v shr 8).toByte(), v.toByte()) }
        "Adler32" -> Adler32().also { it.update(b) }.value.let { v -> byteArrayOf((v shr 24).toByte(), (v shr 16).toByte(), (v shr 8).toByte(), v.toByte()) }
        else -> MessageDigest.getInstance(alg).digest(b)
    }
    fun hmac(alg: String, key: String, msg: String): String { val mac = Mac.getInstance(alg); mac.init(SecretKeySpec(key.toByteArray(), alg)); return mac.doFinal(msg.toByteArray()).hex() }
    private fun key(pw: String, salt: ByteArray): SecretKeySpec = SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(pw.toCharArray(), salt, 120_000, 256)).encoded, "AES")
    fun aesEnc(text: String, pw: String): String {
        need(pw.isNotEmpty(), "Enter a password in the parameter field")
        val salt = ByteArray(16).also { rnd.nextBytes(it) }; val iv = ByteArray(12).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key(pw, salt), GCMParameterSpec(128, iv))
        return Base64.getEncoder().encodeToString(salt + iv + c.doFinal(text.toByteArray()))
    }
    fun aesDec(b64: String, pw: String): String {
        val all = Base64.getMimeDecoder().decode(b64.trim())
        val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, key(pw, all.copyOfRange(0, 16)), GCMParameterSpec(128, all.copyOfRange(16, 28)))
        return String(c.doFinal(all.copyOfRange(28, all.size)))
    }
    fun chachaEnc(text: String, pw: String): String {
        need(pw.isNotEmpty(), "Enter a password"); val salt = ByteArray(16).also { rnd.nextBytes(it) }; val iv = ByteArray(12).also { rnd.nextBytes(it) }
        val k = SecretKeySpec(key(pw, salt).encoded, "ChaCha20"); val c = Cipher.getInstance("ChaCha20-Poly1305"); c.init(Cipher.ENCRYPT_MODE, k, IvParameterSpec(iv))
        return Base64.getEncoder().encodeToString(salt + iv + c.doFinal(text.toByteArray()))
    }
    fun chachaDec(b64: String, pw: String): String {
        val all = Base64.getMimeDecoder().decode(b64.trim()); val k = SecretKeySpec(key(pw, all.copyOfRange(0, 16)).encoded, "ChaCha20")
        val c = Cipher.getInstance("ChaCha20-Poly1305"); c.init(Cipher.DECRYPT_MODE, k, IvParameterSpec(all.copyOfRange(16, 28))); return String(c.doFinal(all.copyOfRange(28, all.size)))
    }
    private fun pem(type: String, der: ByteArray) = "-----BEGIN $type-----\n" + Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(der) + "\n-----END $type-----"
    fun rsa(bits: Int): String { val kp = KeyPairGenerator.getInstance("RSA").apply { initialize(bits) }.generateKeyPair(); return pem("PUBLIC KEY", kp.public.encoded) + "\n\n" + pem("PRIVATE KEY", kp.private.encoded) }
    fun ec(): String { val kp = KeyPairGenerator.getInstance("EC").apply { initialize(java.security.spec.ECGenParameterSpec("secp256r1")) }.generateKeyPair(); return pem("PUBLIC KEY", kp.public.encoded) + "\n\n" + pem("PRIVATE KEY", kp.private.encoded) }
    private fun sshStr(b: ByteArray) = java.nio.ByteBuffer.allocate(4 + b.size).putInt(b.size).put(b).array()
    fun ssh(comment: String): String {
        val kp = KeyPairGenerator.getInstance("RSA").apply { initialize(3072) }.generateKeyPair(); val pub = kp.public as RSAPublicKey
        val blob = sshStr("ssh-rsa".toByteArray()) + sshStr(pub.publicExponent.toByteArray()) + sshStr(pub.modulus.toByteArray())
        return "ssh-rsa " + Base64.getEncoder().encodeToString(blob) + " " + comment + "\n\n" + pem("PRIVATE KEY", kp.private.encoded) + "\n\nSave the private key as ~/.ssh/id_rsa (chmod 600) and add the first line to authorized_keys."
    }
    fun totp(secret: String, digits: Int, period: Int, counter: Long? = null): String {
        val key = Codec.b32dec(secret); val now = System.currentTimeMillis() / 1000
        fun code(c: Long): String { val mac = Mac.getInstance("HmacSHA1"); mac.init(SecretKeySpec(key, "HmacSHA1")); val h = mac.doFinal(java.nio.ByteBuffer.allocate(8).putLong(c).array()); val o = h.last().toInt() and 0xf
            val bin = ((h[o].toInt() and 0x7f) shl 24) or ((h[o + 1].toInt() and 0xff) shl 16) or ((h[o + 2].toInt() and 0xff) shl 8) or (h[o + 3].toInt() and 0xff); return (bin % Math.pow(10.0, digits.toDouble()).toInt()).toString().padStart(digits, '0') }
        if (counter != null) return "HOTP[$counter] = ${code(counter)}"
        val c = now / period
        return "Current : ${code(c)}   (${period - now % period}s left)\nPrevious: ${code(c - 1)}\nNext    : ${code(c + 1)}"
    }
    fun cert(pem: String): String {
        val cf = CertificateFactory.getInstance("X.509")
        val certs = cf.generateCertificates(pem.trim().byteInputStream()).map { it as java.security.cert.X509Certificate }
        return certs.mapIndexed { i, c ->
            "Certificate #${i + 1}\n  Subject : ${c.subjectX500Principal}\n  Issuer  : ${c.issuerX500Principal}\n  Serial  : ${c.serialNumber.toString(16)}\n  Valid   : ${c.notBefore.toInstant()} → ${c.notAfter.toInstant()}\n  Expired : ${c.notAfter.before(java.util.Date())}\n  SigAlg  : ${c.sigAlgName}\n  PubKey  : ${c.publicKey.algorithm}${(c.publicKey as? RSAPublicKey)?.let { " ${it.modulus.bitLength()} bit" } ?: ""}\n  SANs    : ${c.subjectAlternativeNames?.joinToString { it[1].toString() } ?: "-"}\n  CA      : ${c.basicConstraints >= 0}\n  SHA-256 : ${MessageDigest.getInstance("SHA-256").digest(c.encoded).hex(":")}"
        }.joinToString("\n\n")
    }
    fun entropy(pw: String): Double {
        var pool = 0; if (pw.any { it.isLowerCase() }) pool += 26; if (pw.any { it.isUpperCase() }) pool += 26; if (pw.any { it.isDigit() }) pool += 10; if (pw.any { !it.isLetterOrDigit() }) pool += 33
        return if (pool == 0) 0.0 else pw.length * (Math.log(pool.toDouble()) / Math.log(2.0))
    }
    fun strength(pw: String): String {
        val e = entropy(pw); val guesses = Math.pow(2.0, e) / 2; val secs = guesses / 1e10
        val t = when { secs < 1 -> "instantly"; secs < 3600 -> "${secs.toLong() / 60} minutes"; secs < 86400 * 365 -> "${(secs / 86400).toLong()} days"; secs < 86400.0 * 365 * 1e6 -> "${(secs / 86400 / 365).toLong()} years"; else -> "centuries" }
        val common = listOf("password", "123456", "qwerty", "letmein", "admin", "welcome", "iloveyou").any { pw.lowercase().contains(it) }
        return "Length: ${pw.length}\nEntropy: ${"%.1f".format(e)} bits\nRating: ${when { common -> "Very weak (common pattern)"; e < 40 -> "Weak"; e < 60 -> "Fair"; e < 80 -> "Strong"; else -> "Very strong" }}\nOffline crack time @10¹⁰ guesses/s: $t"
    }
    private val words = "able acid also area army atom aunt back bake bark base beam bell bird blue boat bold bone book brave bread brick brook cable cake calm camp cargo cedar chair chalk chess cloud coast coral craft crane creek crown dance delta dream drift eagle earth ember fable fairy fiber field flame flint forest frost garden giant glass globe grain grape habit harbor hazel heart honey igloo image ivory jewel jolly kayak kettle lemon light lunar maple marble meadow mirror noble ocean olive orbit panda pearl piano pixel planet quartz quiet radar river robin rocket saddle sailor shadow silver spark spice stone storm sunset tiger timber tulip unity valley velvet violet walnut whale willow winter wizard yellow zebra zenith".split(" ")
    fun password(kind: String, len: Int): String {
        fun pick(chars: String) = (1..len).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
        val lo = "abcdefghijklmnopqrstuvwxyz"; val up = lo.uppercase(); val dg = "0123456789"; val sy = "!@#$%^&*()-_=+[]{};:,.?"
        return when (kind) {
            "strong" -> { var p: String; do { p = pick(lo + up + dg + sy) } while (!(p.any { it.isLowerCase() } && p.any { it.isUpperCase() } && p.any { it.isDigit() } && p.any { it in sy }) && len >= 8); p }
            "alnum" -> pick(lo + up + dg)
            "pin" -> pick(dg)
            "hex" -> pick("0123456789abcdef")
            "b64" -> Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(len).also { rnd.nextBytes(it) }).take(len)
            else -> (1..maxOf(3, len)).joinToString("-") { words[rnd.nextInt(words.size)] }
        }
    }
    fun bcrypt(pw: String, cost: Int): String { val salt = ByteArray(16).also { rnd.nextBytes(it) }; return OpenBSDBCrypt.generate(pw.toCharArray(), salt, cost) }
    fun scrypt(pw: String): String { val salt = ByteArray(16).also { rnd.nextBytes(it) }; val h = SCrypt.generate(pw.toByteArray(), salt, 16384, 8, 1, 32); return "\$scrypt\$N=16384,r=8,p=1\$${salt.hex()}\$${h.hex()}" }
    fun argon2(pw: String): String {
        val salt = ByteArray(16).also { rnd.nextBytes(it) }
        val p = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id).withSalt(salt).withIterations(3).withMemoryAsKB(19456).withParallelism(1).build()
        val g = Argon2BytesGenerator(); g.init(p); val out = ByteArray(32); g.generateBytes(pw.toByteArray(), out)
        val e = Base64.getEncoder().withoutPadding()
        return "\$argon2id\$v=19\$m=19456,t=3,p=1\$${e.encodeToString(salt)}\$${e.encodeToString(out)}"
    }
    fun identifyHash(h0: String): String {
        val h = h0.trim()
        return when {
            h.startsWith("\$2a\$") || h.startsWith("\$2b\$") || h.startsWith("\$2y\$") -> "bcrypt"
            h.startsWith("\$argon2") -> "Argon2 (${h.substring(1).substringBefore('$')})"
            h.startsWith("\$scrypt") -> "scrypt"
            h.startsWith("\$6\$") -> "SHA-512 crypt"; h.startsWith("\$5\$") -> "SHA-256 crypt"; h.startsWith("\$1\$") -> "MD5 crypt"
            h.startsWith("\$pbkdf2") -> "PBKDF2"
            Regex("[0-9a-fA-F]+").matches(h) -> when (h.length) { 8 -> "CRC32 / Adler32"; 32 -> "MD5 / NTLM / MD4"; 40 -> "SHA-1 / RIPEMD-160"; 56 -> "SHA-224 / SHA3-224"; 64 -> "SHA-256 / SHA3-256 / BLAKE2s / Keccak-256"; 96 -> "SHA-384"; 128 -> "SHA-512 / SHA3-512 / BLAKE2b / Whirlpool"; else -> "Unknown hex length ${h.length}" }
            else -> "Unknown format"
        }
    }
}

val cryptoTools: List<Tool> = listOf(
    tool("hash", "Hash Generator", Category.CRYPTO, "MD5, SHA-1/2/3, BLAKE2, CRC32, Adler-32",
        m("All") { s -> listOf("MD5", "SHA-1", "SHA-256", "SHA-384", "SHA-512", "SHA3-256", "BLAKE2b-256", "CRC32", "Adler32").joinToString("\n") { "${it.padEnd(12)} ${Crypt.digest(it, s.toByteArray()).hex()}" } },
        *listOf("MD5", "SHA-1", "SHA-256", "SHA-384", "SHA-512", "SHA3-256", "SHA3-512", "BLAKE2b-256", "BLAKE2b-512", "CRC32", "Adler32").map { a -> m(a) { s -> Crypt.digest(a, s.toByteArray()).hex() } }.toTypedArray(),
        hint = "Text", sample = "hello world", kw = "md5 sha checksum blake"),
    tool("hmac", "HMAC Generator", Category.CRYPTO, "Keyed hash for webhooks and API signing",
        mp("HMAC-SHA256") { s, p -> Crypt.hmac("HmacSHA256", p.getOrElse(0) { "" }, s) }, mp("HMAC-SHA1") { s, p -> Crypt.hmac("HmacSHA1", p.getOrElse(0) { "" }, s) },
        mp("HMAC-SHA512") { s, p -> Crypt.hmac("HmacSHA512", p.getOrElse(0) { "" }, s) }, mp("HMAC-SHA256 (Base64)") { s, p -> Base64.getEncoder().encodeToString(hexToBytes(Crypt.hmac("HmacSHA256", p.getOrElse(0) { "" }, s))) },
        hint = "Message", params = listOf("Secret key"), sample = "payload", psample = listOf("my-secret")),
    tool("password", "Password & Token Generator", Category.CRYPTO, "Cryptographically secure randoms. Input = length",
        mp("Strong (10)") { s, _ -> (1..10).joinToString("\n") { Crypt.password("strong", s.trim().toIntOrNull() ?: 20) } },
        m("Alphanumeric") { s -> Crypt.password("alnum", s.trim().toIntOrNull() ?: 24) }, m("PIN") { s -> Crypt.password("pin", s.trim().toIntOrNull() ?: 6) },
        m("Passphrase (words)") { s -> Crypt.password("words", s.trim().toIntOrNull() ?: 5) }, m("Hex token") { s -> Crypt.password("hex", s.trim().toIntOrNull() ?: 32) },
        m("URL-safe token") { s -> Crypt.password("b64", s.trim().toIntOrNull() ?: 32) }, hint = "Length", sample = "20", auto = false),
    tool("pw-strength", "Password Strength & Entropy", Category.CRYPTO, "Entropy estimate and crack-time",
        m("Check") { Crypt.strength(it) }, hint = "Password (stays on device)", sample = "correct-horse-battery-staple"),
    tool("aes", "AES-GCM / ChaCha20 Encrypt & Decrypt", Category.CRYPTO, "Password-based authenticated encryption (PBKDF2-SHA256, 120k rounds)",
        mp("AES-256-GCM encrypt") { s, p -> Crypt.aesEnc(s, p.getOrElse(0) { "" }) }, mp("AES-256-GCM decrypt") { s, p -> Crypt.aesDec(s, p.getOrElse(0) { "" }) },
        mp("ChaCha20 encrypt") { s, p -> Crypt.chachaEnc(s, p.getOrElse(0) { "" }) }, mp("ChaCha20 decrypt") { s, p -> Crypt.chachaDec(s, p.getOrElse(0) { "" }) },
        hint = "Text / Base64 ciphertext", params = listOf("Password"), sample = "secret message", psample = listOf("correct horse battery staple")),
    tool("keygen", "RSA / EC / SSH Key Generator", Category.CRYPTO, "Key pairs generated on this device (PEM)",
        m("RSA 2048") { Crypt.rsa(2048) }, m("RSA 4096") { Crypt.rsa(4096) }, m("EC P-256") { Crypt.ec() }, mp("SSH (RSA 3072)") { s, _ -> Crypt.ssh(s.ifBlank { "user@device" }) },
        hint = "Comment (SSH only)", sample = "me@phone"),
    tool("kdf", "bcrypt · scrypt · Argon2", Category.CRYPTO, "Password hashing & verification",
        m("bcrypt hash (cost 10)") { Crypt.bcrypt(it, 10) }, m("bcrypt hash (cost 12)") { Crypt.bcrypt(it, 12) },
        mp("bcrypt verify") { s, p -> if (OpenBSDBCrypt.checkPassword(p.getOrElse(0) { "" }.trim(), s.toCharArray())) "✔ Match" else "✘ No match" },
        m("scrypt hash") { Crypt.scrypt(it) }, m("Argon2id hash") { Crypt.argon2(it) },
        hint = "Password", params = listOf("bcrypt hash to verify against"), sample = "hunter2"),
    tool("totp", "TOTP / HOTP Tester", Category.CRYPTO, "Two-factor codes from a Base32 secret",
        mp("TOTP now") { s, p -> Crypt.totp(s, p.getOrElse(0) { "" }.toIntOrNull() ?: 6, 30) },
        mp("HOTP counter") { s, p -> Crypt.totp(s, 6, 30, p.getOrElse(1) { "" }.toLongOrNull() ?: 0L) },
        hint = "Base32 secret", params = listOf("Digits (6)", "HOTP counter (0)"), sample = "JBSWY3DPEHPK3PXP", psample = listOf("6", "0")),
    tool("x509", "X.509 Certificate / Chain Viewer", Category.CRYPTO, "Decode PEM certificates (several allowed)",
        m("Decode") { Crypt.cert(it) }, hint = "PEM certificate(s)"),
    tool("hash-id", "Hash Type Identifier", Category.CRYPTO, "Guess the algorithm from a hash string",
        m("Identify") { Crypt.identifyHash(it) }, hint = "Hash", sample = "5d41402abc4b2a76b9719d911017c592"),
    custom("vault", "Secure Notes Vault", Category.CRYPTO, "Notes & secrets encrypted with the Android Keystore", "secrets notes encrypted"),
)
