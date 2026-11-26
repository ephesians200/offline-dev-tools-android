package com.codingtools.app.tools

import java.math.BigInteger
import java.net.IDN
import java.net.URLDecoder
import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Codec {
    private const val B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val B58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    fun b32enc(b: ByteArray): String {
        val sb = StringBuilder(); var buf = 0; var bits = 0
        for (x in b) { buf = (buf shl 8) or (x.toInt() and 0xff); bits += 8; while (bits >= 5) { sb.append(B32[(buf shr (bits - 5)) and 31]); bits -= 5 } }
        if (bits > 0) sb.append(B32[(buf shl (5 - bits)) and 31])
        while (sb.length % 8 != 0) sb.append('='); return sb.toString()
    }
    fun b32dec(s: String): ByteArray {
        val out = java.io.ByteArrayOutputStream(); var buf = 0; var bits = 0
        for (c in s.uppercase().replace("=", "").replace(" ", "")) { val v = B32.indexOf(c); need(v >= 0, "Invalid Base32 character '$c'"); buf = (buf shl 5) or v; bits += 5; if (bits >= 8) { out.write((buf shr (bits - 8)) and 0xff); bits -= 8 } }
        return out.toByteArray()
    }
    fun b58enc(b: ByteArray): String {
        var n = BigInteger(1, b); val sb = StringBuilder(); val f = BigInteger.valueOf(58)
        while (n.signum() > 0) { val qr = n.divideAndRemainder(f); sb.append(B58[qr[1].toInt()]); n = qr[0] }
        for (x in b) if (x.toInt() == 0) sb.append('1') else break
        return sb.reverse().toString()
    }
    fun b58dec(s: String): ByteArray {
        var n = BigInteger.ZERO; for (c in s.trim()) { val v = B58.indexOf(c); need(v >= 0, "Invalid Base58 character '$c'"); n = n.multiply(BigInteger.valueOf(58)).add(BigInteger.valueOf(v.toLong())) }
        val raw = n.toByteArray().let { if (it.size > 1 && it[0].toInt() == 0) it.copyOfRange(1, it.size) else it }
        val zeros = s.takeWhile { it == '1' }.length
        return ByteArray(zeros) + (if (n.signum() == 0) ByteArray(0) else raw)
    }
    fun a85enc(b: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i < b.size) {
            val n = minOf(4, b.size - i); var v = 0L
            for (k in 0 until 4) v = (v shl 8) or (if (k < n) (b[i + k].toLong() and 0xff) else 0L)
            if (v == 0L && n == 4) sb.append('z') else { val ch = CharArray(5); var t = v; for (k in 4 downTo 0) { ch[k] = (t % 85 + 33).toInt().toChar(); t /= 85 }; sb.append(ch, 0, n + 1) }
            i += 4
        }
        return sb.toString()
    }
    fun a85dec(s: String): ByteArray {
        val out = java.io.ByteArrayOutputStream(); val t = s.replace(Regex("\\s"), "").removePrefix("<~").removeSuffix("~>"); var i = 0
        while (i < t.length) {
            if (t[i] == 'z') { repeat(4) { out.write(0) }; i++; continue }
            val n = minOf(5, t.length - i); var v = 0L
            for (k in 0 until 5) v = v * 85 + (if (k < n) t[i + k].code - 33 else 84)
            for (k in 0 until n - 1) out.write(((v shr (24 - 8 * k)) and 0xff).toInt()); i += 5
        }
        return out.toByteArray()
    }
    fun urlEnc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
    fun htmlEsc(s: String) = s.map { when (it) { '&' -> "&amp;"; '<' -> "&lt;"; '>' -> "&gt;"; '"' -> "&quot;"; '\'' -> "&#39;"; else -> if (it.code > 127) "&#${it.code};" else it.toString() } }.joinToString("")
    fun htmlUnesc(s: String) = s.replace(Regex("&#x([0-9a-fA-F]+);")) { String(Character.toChars(it.groupValues[1].toInt(16))) }.replace(Regex("&#(\\d+);")) { String(Character.toChars(it.groupValues[1].toInt())) }
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&nbsp;", " ").replace("&copy;", "©").replace("&amp;", "&")
    fun b64urlDec(s: String): ByteArray = java.util.Base64.getUrlDecoder().decode(s.trim().replace('+', '-').replace('/', '_').trimEnd('='))
    fun jwt(t: String, secret: String): String {
        val p = t.trim().split("."); need(p.size >= 2, "A JWT has 3 dot-separated parts")
        val h = String(b64urlDec(p[0])); val pl = String(b64urlDec(p[1]))
        val sb = StringBuilder("HEADER\n${Json.write(Json.parse(h))}\n\nPAYLOAD\n${Json.write(Json.parse(pl))}\n")
        val claims = Json.parse(pl) as? Map<*, *>
        val fmt = java.time.format.DateTimeFormatter.ISO_INSTANT
        claims?.forEach { (k, v) -> if (k in setOf("exp", "iat", "nbf") && v is Number) sb.append("\n$k → ${fmt.format(java.time.Instant.ofEpochSecond(v.toLong()))}") }
        (claims?.get("exp") as? Number)?.let { val d = it.toLong() - System.currentTimeMillis() / 1000; sb.append(if (d < 0) "\n⚠ Expired ${Conv.duration(-d)} ago" else "\n✔ Expires in ${Conv.duration(d)}") }
        val alg = (Json.parse(h) as? Map<*, *>)?.get("alg")?.toString()
        if (secret.isNotBlank() && p.size == 3) {
            val macName = when (alg) { "HS256" -> "HmacSHA256"; "HS384" -> "HmacSHA384"; "HS512" -> "HmacSHA512"; else -> null }
            if (macName != null) {
                val mac = Mac.getInstance(macName); mac.init(SecretKeySpec(secret.toByteArray(), macName))
                val sig = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal("${p[0]}.${p[1]}".toByteArray()))
                sb.append(if (sig == p[2]) "\n\n✔ Signature VALID ($alg)" else "\n\n✘ Signature INVALID")
            } else if (alg == "RS256") {
                val kf = java.security.KeyFactory.getInstance("RSA")
                val der = java.util.Base64.getMimeDecoder().decode(secret.replace(Regex("-----[A-Z ]+-----"), "").trim())
                val pub = kf.generatePublic(java.security.spec.X509EncodedKeySpec(der))
                val sg = java.security.Signature.getInstance("SHA256withRSA"); sg.initVerify(pub); sg.update("${p[0]}.${p[1]}".toByteArray())
                sb.append(if (sg.verify(b64urlDec(p[2]))) "\n\n✔ Signature VALID (RS256)" else "\n\n✘ Signature INVALID")
            } else sb.append("\n\nVerification for alg=$alg is not supported")
        } else if (secret.isBlank()) sb.append("\n(alg: $alg – paste secret / PEM public key in the parameter field to verify)")
        return sb.toString()
    }
    private val morse = mapOf('A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--", 'Z' to "--..", '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.")
    fun morseEnc(s: String) = s.uppercase().split(" ").joinToString(" / ") { w -> w.mapNotNull { morse[it] }.joinToString(" ") }
    fun morseDec(s: String): String { val r = morse.entries.associate { it.value to it.key }; return s.trim().split(Regex("\\s*/\\s*")).joinToString(" ") { w -> w.trim().split(" ").mapNotNull { r[it] }.joinToString("") } }
    fun rot(s: String, n: Int) = s.map { c -> when (c) { in 'a'..'z' -> 'a' + Math.floorMod(c - 'a' + n, 26); in 'A'..'Z' -> 'A' + Math.floorMod(c - 'A' + n, 26); else -> c } }.joinToString("")
    fun codepoints(s: String): String = s.codePoints().toArray().joinToString("\n") { cp ->
        val ch = String(Character.toChars(cp)); val utf8 = ch.toByteArray(Charsets.UTF_8).hex(" ")
        "U+%04X  %-3s  UTF-8: %-12s UTF-16: %s  %s".format(cp, if (Character.isISOControl(cp)) "·" else ch, utf8, ch.toByteArray(Charsets.UTF_16BE).hex(" "), Character.getName(cp) ?: "")
    }
    fun unicodeEsc(s: String) = s.map { if (it.code in 32..126) it.toString() else "\\u%04x".format(it.code) }.joinToString("")
    fun unicodeUnesc(s: String) = s.replace(Regex("\\\\u([0-9a-fA-F]{4})")) { it.groupValues[1].toInt(16).toChar().toString() }.replace(Regex("\\\\U([0-9a-fA-F]{8})")) { String(Character.toChars(it.groupValues[1].toInt(16))) }
}

val codecTools: List<Tool> = listOf(
    tool("base64", "Base64", Category.CODEC, "Encode / decode text (standard & URL-safe)",
        m("Encode") { java.util.Base64.getEncoder().encodeToString(it.toByteArray()) },
        m("Decode") { String(java.util.Base64.getMimeDecoder().decode(it.trim().replace('-', '+').replace('_', '/'))) },
        m("URL-safe encode") { java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray()) },
        m("Decode → hex") { java.util.Base64.getMimeDecoder().decode(it.trim().replace('-', '+').replace('_', '/')).hex(" ") },
        hint = "Text or Base64", sample = "Hello, Programmers!"),
    tool("base32-58-85", "Base32 · Base58 · Base85", Category.CODEC, "Other binary-to-text encodings",
        m("Base32 encode") { Codec.b32enc(it.toByteArray()) }, m("Base32 decode") { String(Codec.b32dec(it)) },
        m("Base58 encode") { Codec.b58enc(it.toByteArray()) }, m("Base58 decode") { String(Codec.b58dec(it)) },
        m("Base85 encode") { Codec.a85enc(it.toByteArray()) }, m("Base85 decode") { String(Codec.a85dec(it)) },
        hint = "Text", sample = "Hello"),
    tool("url", "URL Encode / Decode", Category.CODEC, "Percent-encoding for query strings and paths",
        m("Encode") { Codec.urlEnc(it) }, m("Decode") { URLDecoder.decode(it, "UTF-8") },
        m("Encode (form +)") { URLEncoder.encode(it, "UTF-8") }, m("Encode full URL") { java.net.URI(null, null, it, null).rawPath ?: it },
        hint = "Text or URL", sample = "name=Ada Lovelace&lang=C++/Kotlin"),
    tool("html-entities", "HTML Entities", Category.CODEC, "Escape and unescape HTML",
        m("Escape") { Codec.htmlEsc(it) }, m("Unescape") { Codec.htmlUnesc(it) }, hint = "Text", sample = "<a href=\"x\">Tom & Jerry © 2026</a>"),
    tool("unicode", "Unicode Inspector & Escaper", Category.CODEC, "Code points, UTF-8/UTF-16 bytes, \\u escapes",
        m("Inspect code points") { Codec.codepoints(it) }, m("Escape \\uXXXX") { Codec.unicodeEsc(it) }, m("Unescape \\uXXXX") { Codec.unicodeUnesc(it) },
        m("UTF-8 bytes (hex)") { it.toByteArray(Charsets.UTF_8).hex(" ") }, m("UTF-16 bytes (hex)") { it.toByteArray(Charsets.UTF_16BE).hex(" ") },
        m("Count") { "Chars (UTF-16 units): ${it.length}\nCode points: ${it.codePointCount(0, it.length)}\nUTF-8 bytes: ${it.toByteArray().size}" },
        hint = "Text", sample = "héllo 世界 😀"),
    tool("jwt", "JWT Decoder & Verifier", Category.CODEC, "Decode header/payload, explain exp, verify HS256/384/512 or RS256",
        mp("Decode / Verify") { s, p -> Codec.jwt(s, p.getOrElse(0) { "" }) },
        hint = "JWT", params = listOf("Secret (HS*) or PEM public key (RS256) – optional"),
        sample = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkFkYSIsImlhdCI6MTUxNjIzOTAyMn0.t2kq1Kz2ZpuQz1U5uSJkKqZx7p6LkYhS5Zl2c1bYx9Q"),
    tool("hex-text", "Hex · Binary · ASCII", Category.CODEC, "Text ⇄ hex / binary / decimal bytes",
        m("Text → hex") { it.toByteArray().hex(" ") }, m("Hex → text") { String(hexToBytes(it)) },
        m("Text → binary") { it.toByteArray().joinToString(" ") { b -> (b.toInt() and 0xff).toString(2).padStart(8, '0') } },
        m("Binary → text") { s -> String(s.trim().split(Regex("\\s+")).map { it.toInt(2).toByte() }.toByteArray()) },
        m("Text → decimal") { it.toByteArray().joinToString(" ") { b -> (b.toInt() and 0xff).toString() } },
        m("Decimal → text") { s -> String(s.trim().split(Regex("[\\s,]+")).map { it.toInt().toByte() }.toByteArray()) },
        hint = "Input", sample = "Kotlin"),
    tool("punycode", "Punycode / IDN", Category.CODEC, "Internationalized domain names",
        m("To ASCII") { IDN.toASCII(it.trim()) }, m("To Unicode") { IDN.toUnicode(it.trim()) }, hint = "Domain", sample = "münchen.de"),
    tool("classic-ciphers", "Morse · ROT13 · Caesar · Atbash", Category.CODEC, "Fun classic encodings",
        m("Morse encode") { Codec.morseEnc(it) }, m("Morse decode") { Codec.morseDec(it) }, m("ROT13") { Codec.rot(it, 13) },
        mp("Caesar +N") { s, p -> Codec.rot(s, p.getOrElse(0) { "" }.toIntOrNull() ?: 3) }, mp("Caesar −N") { s, p -> Codec.rot(s, -(p.getOrElse(0) { "" }.toIntOrNull() ?: 3)) },
        m("Atbash") { s -> s.map { c -> when (c) { in 'a'..'z' -> 'z' - (c - 'a'); in 'A'..'Z' -> 'Z' - (c - 'A'); else -> c } }.joinToString("") },
        m("Reverse") { it.reversed() }, hint = "Text", params = listOf("Shift N (default 3)"), sample = "SOS HELLO"),
    custom("qr-barcode", "QR Code & Barcode Generator", Category.CODEC, "QR, Code128, Code39, EAN-13, PDF417, Aztec, Data Matrix – generated on device", "qrcode barcode"),
)
