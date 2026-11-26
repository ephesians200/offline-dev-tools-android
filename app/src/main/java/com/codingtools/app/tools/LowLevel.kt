package com.codingtools.app.tools

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.PriorityQueue
import java.util.zip.*

object Low {
    fun dump(b: ByteArray): String = b.toList().chunked(16).mapIndexed { i, row -> "%08x  %-47s  |%s|".format(i * 16, row.joinToString(" ") { "%02x".format(it) }, row.map { if (it in 32..126) it.toInt().toChar() else '.' }.joinToString("")) }.joinToString("\n")
    fun anyBytes(s: String): ByteArray { val t = s.trim(); return if (Regex("^(0x)?[0-9a-fA-F\\s:,-]+$").matches(t) && t.replace(Regex("[^0-9a-fA-F]"), "").length % 2 == 0) hexToBytes(t) else java.util.Base64.getMimeDecoder().decode(t) }
    private val magic = listOf("89504E470D0A1A0A" to "PNG image", "FFD8FF" to "JPEG image", "47494638" to "GIF image", "25504446" to "PDF document", "504B0304" to "ZIP / JAR / APK / DOCX / XLSX", "1F8B08" to "GZIP", "7F454C46" to "ELF executable", "4D5A" to "Windows PE / DOS executable", "CAFEBABE" to "Java class / Mach-O fat binary", "FEEDFACE" to "Mach-O 32-bit", "FEEDFACF" to "Mach-O 64-bit", "CFFAEDFE" to "Mach-O 64-bit (LE)", "52494646" to "RIFF (WAV / AVI / WEBP)", "49443303" to "MP3 (ID3)", "4F676753" to "OGG", "664C6143" to "FLAC", "7B22" to "JSON text", "3C3F786D6C" to "XML", "53514C69746520666F726D6174203300" to "SQLite database", "377ABCAF271C" to "7-Zip", "526172211A07" to "RAR", "425A68" to "BZIP2", "FD377A585A00" to "XZ", "6465780A" to "Android DEX", "0061736D" to "WebAssembly", "D0CF11E0A1B11AE1" to "MS Office (OLE2)", "00000100" to "ICO icon", "424D" to "BMP image", "2321" to "Script (#!)", "EFBBBF" to "UTF-8 text with BOM", "FFFE" to "UTF-16 LE text", "FEFF" to "UTF-16 BE text")
    fun identify(b: ByteArray): String { val h = b.take(16).joinToString("") { "%02X".format(it) }; return magic.firstOrNull { h.startsWith(it.first) }?.second ?: "Unknown signature ($h)" }
    fun pbDecode(b: ByteArray, ind: String = ""): String? {
        var i = 0; val sb = StringBuilder()
        fun varint(): Long { var r = 0L; var sh = 0; while (true) { if (i >= b.size) throw IllegalStateException(); val x = b[i++].toInt(); r = r or ((x and 0x7f).toLong() shl sh); if (x and 0x80 == 0) break; sh += 7; if (sh > 63) throw IllegalStateException() }; return r }
        try { while (i < b.size) { val key = varint(); val f = key shr 3; val wt = (key and 7).toInt(); if (f == 0L) return null
            when (wt) { 0 -> { val v = varint(); sb.append("$ind$f: $v${if (v > 0) "  (zigzag ${(v ushr 1) xor -(v and 1)})" else ""}\n") }
                1 -> { if (i + 8 > b.size) return null; val v = ByteBuffer.wrap(b, i, 8).order(ByteOrder.LITTLE_ENDIAN); sb.append("$ind$f: fixed64 ${v.long} / double ${ByteBuffer.wrap(b, i, 8).order(ByteOrder.LITTLE_ENDIAN).double}\n"); i += 8 }
                2 -> { val n = varint().toInt(); if (n < 0 || i + n > b.size) return null; val sl = b.copyOfRange(i, i + n); i += n; val nested = if (n > 1) pbDecode(sl, "$ind  ") else null
                    sb.append(if (nested != null && nested.isNotEmpty()) "$ind$f: {\n$nested$ind}\n" else if (sl.all { it in 32..126 || it.toInt() == 10 }) "$ind$f: \"${String(sl)}\"\n" else "$ind$f: bytes ${sl.hex()}\n") }
                5 -> { if (i + 4 > b.size) return null; sb.append("$ind$f: fixed32 ${ByteBuffer.wrap(b, i, 4).order(ByteOrder.LITTLE_ENDIAN).int} / float ${ByteBuffer.wrap(b, i, 4).order(ByteOrder.LITTLE_ENDIAN).float}\n"); i += 4 }
                else -> return null } } } catch (e: Exception) { return null }
        return sb.toString()
    }
    class Rd(val b: ByteArray) { var i = 0; fun u8() = b[i++].toInt() and 0xff; fun n(k: Int): Long { var r = 0L; repeat(k) { r = (r shl 8) or u8().toLong() }; return r }; fun bytes(k: Int) = b.copyOfRange(i, i + k).also { i += k } }
    fun msgpack(r: Rd): Any? { val t = r.u8()
        fun str(n: Int) = String(r.bytes(n))
        fun arr(n: Int) = (0 until n).map { msgpack(r) }
        fun map(n: Int): Any = LinkedHashMap<String, Any?>().also { m -> repeat(n) { m[msgpack(r).toString()] = msgpack(r) } }
        return when { t <= 0x7f -> t.toLong(); t in 0x80..0x8f -> map(t and 0xf); t in 0x90..0x9f -> arr(t and 0xf); t in 0xa0..0xbf -> str(t and 0x1f); t >= 0xe0 -> (t - 256).toLong()
            else -> when (t) { 0xc0 -> null; 0xc2 -> false; 0xc3 -> true; 0xc4 -> r.bytes(r.n(1).toInt()).hex(); 0xc5 -> r.bytes(r.n(2).toInt()).hex(); 0xc6 -> r.bytes(r.n(4).toInt()).hex(); 0xca -> java.lang.Float.intBitsToFloat(r.n(4).toInt()).toDouble(); 0xcb -> java.lang.Double.longBitsToDouble(r.n(8))
                0xcc -> r.n(1); 0xcd -> r.n(2); 0xce -> r.n(4); 0xcf -> r.n(8); 0xd0 -> r.n(1).toByte().toLong(); 0xd1 -> r.n(2).toShort().toLong(); 0xd2 -> r.n(4).toInt().toLong(); 0xd3 -> r.n(8)
                0xd9 -> str(r.n(1).toInt()); 0xda -> str(r.n(2).toInt()); 0xdb -> str(r.n(4).toInt()); 0xdc -> arr(r.n(2).toInt()); 0xdd -> arr(r.n(4).toInt()); 0xde -> map(r.n(2).toInt()); 0xdf -> map(r.n(4).toInt()); else -> throw IllegalArgumentException("Unsupported MessagePack type 0x%02x".format(t)) } } }
    fun cbor(r: Rd): Any? { val ib = r.u8(); val mt = ib shr 5; val ai = ib and 31
        val v: Long = when { ai < 24 -> ai.toLong(); ai == 24 -> r.n(1); ai == 25 -> r.n(2); ai == 26 -> r.n(4); ai == 27 -> r.n(8); else -> -1 }
        return when (mt) { 0 -> v; 1 -> -1 - v; 2 -> r.bytes(v.toInt()).hex(); 3 -> String(r.bytes(v.toInt())); 4 -> (0 until v.toInt()).map { cbor(r) }; 5 -> LinkedHashMap<String, Any?>().also { m -> repeat(v.toInt()) { m[cbor(r).toString()] = cbor(r) } }; 6 -> cbor(r)
            else -> when (ai) { 20 -> false; 21 -> true; 22, 23 -> null; 25 -> { val h = v.toInt(); val e = (h shr 10) and 31; val f = h and 1023; (if (e == 0) f * Math.pow(2.0, -24.0) else if (e != 31) (1 + f / 1024.0) * Math.pow(2.0, (e - 15).toDouble()) else Double.NaN) * (if (h and 0x8000 != 0) -1 else 1) }; 26 -> java.lang.Float.intBitsToFloat(v.toInt()).toDouble(); 27 -> java.lang.Double.longBitsToDouble(v); else -> throw IllegalArgumentException("Unsupported CBOR simple value") } } }
    private val oidNames = mapOf("1.2.840.113549.1.1.1" to "rsaEncryption", "1.2.840.113549.1.1.11" to "sha256WithRSA", "1.2.840.10045.2.1" to "ecPublicKey", "1.2.840.10045.3.1.7" to "prime256v1", "2.5.4.3" to "commonName", "2.5.4.6" to "countryName", "2.5.4.10" to "organizationName", "2.5.29.17" to "subjectAltName", "2.5.29.19" to "basicConstraints")
    fun asn1(b: ByteArray, ind: String = "", sb: StringBuilder = StringBuilder()): String {
        var i = 0
        while (i < b.size) { val tag = b[i++].toInt() and 0xff; var len = b[i++].toInt() and 0xff; if (len and 0x80 != 0) { val n = len and 0x7f; len = 0; repeat(n) { len = (len shl 8) or (b[i++].toInt() and 0xff) } }
            val body = b.copyOfRange(i, minOf(b.size, i + len)); i += len; val cls = tag and 0x1f
            val name = if (tag and 0xc0 == 0x80) "[${cls}]" else when (cls) { 2 -> "INTEGER"; 3 -> "BIT STRING"; 4 -> "OCTET STRING"; 5 -> "NULL"; 6 -> "OBJECT IDENTIFIER"; 12 -> "UTF8String"; 19 -> "PrintableString"; 22 -> "IA5String"; 23 -> "UTCTime"; 24 -> "GeneralizedTime"; 16 -> "SEQUENCE"; 17 -> "SET"; 1 -> "BOOLEAN"; else -> "TAG 0x%02x".format(tag) }
            if (tag and 0x20 != 0) { sb.append("$ind$name ($len)\n"); asn1(body, "$ind  ", sb) }
            else { val v = when (cls) { 6 -> { val f = body[0].toInt() and 0xff; val parts = arrayListOf((f / 40).toString(), (f % 40).toString()); var x = 0L; for (k in 1 until body.size) { x = (x shl 7) or (body[k].toLong() and 0x7f); if (body[k].toInt() and 0x80 == 0) { parts.add(x.toString()); x = 0 } }; val o = parts.joinToString("."); o + (oidNames[o]?.let { " ($it)" } ?: "") }; 12, 19, 22, 23, 24 -> "\"${String(body)}\""; 2 -> java.math.BigInteger(body).let { if (it.bitLength() > 64) "0x" + it.toString(16) else it.toString() }; 1 -> (body[0].toInt() != 0).toString(); 5 -> ""; else -> body.hex().take(80) }
                sb.append("$ind$name ($len) $v\n") } }
        return sb.toString()
    }
    fun huffman(s: String): String { need(s.isNotEmpty(), "Enter text"); class N(val ch: Char?, val w: Int, val l: N? = null, val r: N? = null)
        val pq = PriorityQueue<N>(compareBy<N> { it.w }); s.groupingBy { it }.eachCount().forEach { (c, w) -> pq.add(N(c, w)) }; if (pq.size == 1) pq.add(N(null, 0))
        while (pq.size > 1) { val a = pq.poll(); val b = pq.poll(); pq.add(N(null, a.w + b.w, a, b)) }
        val codes = HashMap<Char, String>(); fun walk(n: N, p: String) { if (n.ch != null) codes[n.ch] = p.ifEmpty { "0" } else { n.l?.let { walk(it, p + "0") }; n.r?.let { walk(it, p + "1") } } }; walk(pq.poll(), "")
        val enc = s.map { codes.getValue(it) }.joinToString(""); return codes.entries.sortedBy { it.value.length }.joinToString("\n") { "'${if (it.key == ' ') "␠" else it.key.toString()}'  ${it.value}" } + "\n\nEncoded (${enc.length} bits vs ${s.length * 8} original, ${100 - enc.length * 100 / (s.length * 8)}% smaller):\n$enc" }
    fun rle(s: String) = Regex("(.)\\1*").findAll(s).joinToString("") { "${it.value.length}${it.value[0]}" }
    fun unrle(s: String) = Regex("(\\d+)(\\D)").findAll(s).joinToString("") { it.groupValues[2].repeat(it.groupValues[1].toInt()) }
    fun gray(n: Long) = n xor (n ushr 1)
    fun ungray(g: Long): Long { var n = g; var s = g ushr 1; while (s != 0L) { n = n xor s; s = s ushr 1 }; return n }
    fun compress(s: String, kind: String): String { val raw = s.toByteArray(); val bo = ByteArrayOutputStream(); (if (kind == "gzip") GZIPOutputStream(bo) else DeflaterOutputStream(bo)).use { it.write(raw) }
        val out = bo.toByteArray(); return "Original: ${raw.size} B   Compressed: ${out.size} B   (${if (raw.isEmpty()) 0 else 100 - out.size * 100 / raw.size}% saved)\n\n" + java.util.Base64.getEncoder().encodeToString(out) }
    fun decompress(b64: String): String { val raw = java.util.Base64.getMimeDecoder().decode(b64.trim()); val ins = if (raw.size > 2 && raw[0] == 0x1f.toByte() && raw[1] == 0x8b.toByte()) GZIPInputStream(raw.inputStream()) else InflaterInputStream(raw.inputStream()); return String(ins.readBytes()) }
    private val sizes = mapOf("char" to 1, "signed char" to 1, "unsigned char" to 1, "bool" to 1, "_bool" to 1, "int8_t" to 1, "uint8_t" to 1, "short" to 2, "unsigned short" to 2, "int16_t" to 2, "uint16_t" to 2, "int" to 4, "unsigned int" to 4, "unsigned" to 4, "int32_t" to 4, "uint32_t" to 4, "float" to 4, "long" to 8, "unsigned long" to 8, "long long" to 8, "unsigned long long" to 8, "int64_t" to 8, "uint64_t" to 8, "double" to 8, "size_t" to 8, "ssize_t" to 8, "void*" to 8, "long double" to 16)
    fun structLayout(s: String, ptr: Int): String { var off = 0; var maxA = 1; val rows = ArrayList<List<String>>(); rows.add(listOf("offset", "size", "pad", "field")); var padTotal = 0; val fieldSizes = ArrayList<Int>()
        for (ln in lines(s).map { it.trim().removeSuffix(";").trim() }.filter { it.isNotEmpty() && !it.startsWith("//") && !it.startsWith("struct") && it != "{" && it != "}" }) {
            val star = ln.contains('*'); val arr = Regex("\\[(\\d+)]").find(ln)?.groupValues?.get(1)?.toInt() ?: 1; val clean = ln.replace(Regex("\\[\\d+]"), "").replace("*", " * ").trim(); val parts = clean.split(Regex("\\s+")); val name = parts.last(); val type = parts.dropLast(1).filter { it != "*" }.joinToString(" ").lowercase()
            val sz = if (star) ptr else sizes[type] ?: throw IllegalArgumentException("Unknown type '$type'"); val al = minOf(sz, 16).coerceAtLeast(1); val pad = (al - off % al) % al; off += pad; padTotal += pad; rows.add(listOf(off.toString(), (sz * arr).toString(), pad.toString(), "$ln")); off += sz * arr; maxA = maxOf(maxA, al); fieldSizes.add(sz * arr) }
        val tail = (maxA - off % maxA) % maxA; val total = off + tail
        return table(rows) + "\n\nTrailing padding: $tail   Total size: $total   Alignment: $maxA   Wasted: ${padTotal + tail} bytes\nTip: ordering fields from largest to smallest alignment usually minimises padding."
    }
    fun structPack(fmt: String, vals: String, unpackHex: String?): String {
        var order = ByteOrder.LITTLE_ENDIAN; var f = fmt.trim(); if (f.isNotEmpty() && f[0] in "<>!=@") { if (f[0] == '>' || f[0] == '!') order = ByteOrder.BIG_ENDIAN; f = f.substring(1) }
        val items = Regex("(\\d*)([xcbB?hHiIlLqQfds])").findAll(f).toList(); val values = Regex("\"[^\"]*\"|[^,\\s]+").findAll(vals).map { it.value.trim('"') }.toList(); var vi = 0
        if (unpackHex != null) { val bb = ByteBuffer.wrap(hexToBytes(unpackHex)).order(order); val out = ArrayList<String>()
            for (m in items) { val n = m.groupValues[1].toIntOrNull() ?: 1; when (m.groupValues[2]) { "x" -> bb.position(bb.position() + n); "s" -> { val a = ByteArray(n); bb.get(a); out.add(Json.quote(String(a).trimEnd('\u0000'))) }; else -> repeat(n) { out.add(when (m.groupValues[2]) { "b" -> bb.get().toString(); "B", "c", "?" -> (bb.get().toInt() and 0xff).toString(); "h" -> bb.short.toString(); "H" -> (bb.short.toInt() and 0xffff).toString(); "i", "l" -> bb.int.toString(); "I", "L" -> (bb.int.toLong() and 0xffffffffL).toString(); "q" -> bb.long.toString(); "Q" -> java.lang.Long.toUnsignedString(bb.long); "f" -> bb.float.toString(); else -> bb.double.toString() }) } } }
            return "(" + out.joinToString(", ") + ")" }
        val bo = ByteArrayOutputStream()
        fun put(n: Int, v: Long) { val a = ByteBuffer.allocate(n).order(order); when (n) { 1 -> a.put(v.toByte()); 2 -> a.putShort(v.toShort()); 4 -> a.putInt(v.toInt()); else -> a.putLong(v) }; bo.write(a.array()) }
        for (m in items) { val n = m.groupValues[1].toIntOrNull() ?: 1; when (m.groupValues[2]) { "x" -> repeat(n) { bo.write(0) }; "s" -> { val s = values.getOrElse(vi++) { "" }.toByteArray(); bo.write(s.copyOf(n)) }
            else -> repeat(n) { val v = values.getOrElse(vi++) { "0" }; when (m.groupValues[2]) { "b", "B", "c", "?" -> put(1, v.toDoubleOrNull()?.toLong() ?: if (v == "true") 1 else v.firstOrNull()?.code?.toLong() ?: 0); "h", "H" -> put(2, v.toLong()); "i", "I", "l", "L" -> put(4, v.toLong()); "q", "Q" -> put(8, v.toLong()); "f" -> put(4, java.lang.Float.floatToIntBits(v.toFloat()).toLong()); else -> put(8, java.lang.Double.doubleToLongBits(v.toDouble())) } } } }
        val b = bo.toByteArray(); return "${b.size} bytes\n${b.hex(" ")}\n\n${dump(b)}"
    }
    fun bitfield(v: String, spec: String): String { val n = Conv.parseNum(v, null).toLong(); var shift = 0; val out = ArrayList<String>()
        spec.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { f -> val (name, w) = f.split(':').let { it[0] to it[1].toInt() }; val x = (n ushr shift) and ((1L shl w) - 1); out.add("${name.padEnd(12)} bits ${shift + w - 1}..$shift  = $x  (0b${x.toString(2).padStart(w, '0')})"); shift += w }
        return "Value 0x${n.toString(16).uppercase()}  (0b${n.toString(2)})\n\n" + out.joinToString("\n") }
    fun endian(s: String): String { val b = hexToBytes(s); return "Original (as given): ${b.hex(" ")}\nSwapped (reversed) : ${b.reversedArray().hex(" ")}\nAs BE integer      : ${java.math.BigInteger(1, b)}\nAs LE integer      : ${java.math.BigInteger(1, b.reversedArray())}" }
}

val lowLevelTools: List<Tool> = listOf(
    tool("hexdump", "Hex Dump & Viewer", Category.LOWLEVEL, "Classic offset / hex / ASCII view of text, hex or Base64 bytes",
        m("Text → dump") { Low.dump(it.toByteArray()) }, m("Hex/Base64 → dump") { Low.dump(Low.anyBytes(it)) }, m("Dump → text") { s -> String(hexToBytes(lines(s).joinToString(" ") { l -> l.substringAfter("  ", l).substringBefore("  |") })) }, hint = "Input", sample = "Hello, Kotlin!"),
    tool("magic", "File Signature (Magic Number) Identifier", Category.LOWLEVEL, "Paste the first bytes in hex or Base64", m("Identify") { Low.identify(Low.anyBytes(it)) }, hint = "e.g. 89 50 4E 47 0D 0A 1A 0A", sample = "89504E470D0A1A0A"),
    tool("charset", "Charset / Code-page Converter", Category.LOWLEVEL, "ASCII, UTF-8/16, ISO-8859-x, Windows-125x, EBCDIC (IBM037), Shift_JIS…",
        mp("Text → bytes") { s, p -> s.toByteArray(java.nio.charset.Charset.forName(p.getOrElse(0) { "" }.ifBlank { "UTF-8" })).hex(" ") }, mp("Bytes → text") { s, p -> String(hexToBytes(s), java.nio.charset.Charset.forName(p.getOrElse(0) { "" }.ifBlank { "UTF-8" })) },
        m("Available charsets") { java.nio.charset.Charset.availableCharsets().keys.joinToString(", ") }, hint = "Text or hex", params = listOf("Charset name (UTF-8)"), sample = "héllo"),
    tool("endian", "Endianness Converter", Category.LOWLEVEL, "Byte-order swap and integer interpretation", m("Convert") { Low.endian(it) }, hint = "Hex bytes", sample = "DE AD BE EF"),
    tool("bitfield", "Bit-field / Flag Decoder", Category.LOWLEVEL, "Split a register or flags value into named fields", mp("Decode") { s, p -> Low.bitfield(s, p.getOrElse(0) { "" }) }, hint = "Value (0x… / dec)", params = listOf("Fields low→high, e.g. R:1,W:1,X:1,mode:3"), sample = "0x2D", psample = listOf("R:1,W:1,X:1,mode:3")),
    tool("struct-layout", "C Struct Layout & Padding", Category.LOWLEVEL, "Field offsets, padding and total size",
        m("64-bit (LP64)") { Low.structLayout(it, 8) }, m("32-bit (ILP32)") { Low.structLayout(it, 4) }, hint = "One field per line", sample = "char a;\nint b;\nshort c;\ndouble d;\nchar *name;"),
    tool("struct-pack", "Struct Pack / Unpack (Python style)", Category.LOWLEVEL, "Formats: < > ! b B h H i I q Q f d ? s x",
        mp("Pack") { fmt, p -> Low.structPack(fmt, p.getOrElse(0) { "" }, null) }, mp("Unpack hex") { fmt, p -> Low.structPack(fmt, "", p.getOrElse(0) { "" }) }, hint = "Format e.g. <IH4s", params = listOf("Values (comma separated) / hex to unpack"), sample = "<IH4s", psample = listOf("1, 2, abcd")),
    tool("protobuf", "Protobuf Raw Decoder", Category.LOWLEVEL, "Schema-less decode of Protobuf bytes (hex or Base64)", m("Decode") { Low.pbDecode(Low.anyBytes(it)) ?: "Not valid protobuf wire format" }, hint = "Hex / Base64", sample = "0a03416461109601"),
    tool("msgpack-cbor", "MessagePack / CBOR Decoder", Category.LOWLEVEL, "Binary → JSON", m("MessagePack → JSON") { Json.write(Low.msgpack(Low.Rd(Low.anyBytes(it)))) }, m("CBOR → JSON") { Json.write(Low.cbor(Low.Rd(Low.anyBytes(it)))) }, hint = "Hex / Base64", sample = "82a3666f6f01a3626172c3"),
    tool("asn1", "ASN.1 / DER / PEM Viewer", Category.LOWLEVEL, "Dump the TLV structure of certificates and keys",
        m("Dump") { s -> val t = s.trim(); Low.asn1(if (t.startsWith("-----")) java.util.Base64.getMimeDecoder().decode(t.lines().filter { !it.startsWith("-----") }.joinToString("")) else Low.anyBytes(t)) }, hint = "PEM / hex / Base64"),
    tool("compress", "Compression Playground", Category.LOWLEVEL, "GZIP / zlib-deflate text ⇄ Base64",
        m("GZIP compress") { Low.compress(it, "gzip") }, m("Deflate compress") { Low.compress(it, "deflate") }, m("Decompress Base64") { Low.decompress(it) }, hint = "Text or Base64", sample = "aaaaaaaaaabbbbbbbbbbaaaaaaaaaa".repeat(8)),
    tool("encoding-algos", "Gray Code · Huffman · RLE", Category.LOWLEVEL, "Classic encoding algorithms",
        m("Gray encode (n)") { s -> val n = s.trim().toLong(); "gray = ${Low.gray(n)} (0b${Low.gray(n).toString(2)})" }, m("Gray decode (n)") { Low.ungray(it.trim().toLong()).toString() },
        m("RLE encode") { Low.rle(it) }, m("RLE decode") { Low.unrle(it) }, m("Huffman codes") { Low.huffman(it) }, hint = "Number or text", sample = "aaabccddddd"),
    custom("file-inspector", "File Inspector (hex · hashes · archives · ELF/PE/Mach-O)", Category.LOWLEVEL, "Pick any file: hex view, magic type, MD5/SHA, verify checksum, list ZIP contents, parse executable headers", "hex editor checksum verify zip elf pe"),
)
