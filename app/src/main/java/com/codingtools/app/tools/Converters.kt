package com.codingtools.app.tools

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.math.BigInteger
import java.util.Locale

object Conv {
    // ---------- JSON <-> XML ----------
    private fun safeName(n: String): String { val s = n.replace(Regex("[^A-Za-z0-9_.-]"), "_"); return if (s.isEmpty() || s[0].isDigit()) "_$s" else s }
    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    private fun el(name: String, v: Any?, lvl: Int, sb: StringBuilder) {
        val p = "  ".repeat(lvl); val n = safeName(name)
        when (v) {
            is Map<*, *> -> { sb.append("$p<$n>\n"); v.forEach { (k, x) -> el(k.toString(), x, lvl + 1, sb) }; sb.append("$p</$n>\n") }
            is List<*> -> v.forEach { el(name, it, lvl, sb) }
            null -> sb.append("$p<$n/>\n")
            else -> sb.append("$p<$n>${esc(v.toString())}</$n>\n")
        }
    }
    fun jsonToXml(s: String): String {
        val v = Json.parse(s); val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        el("root", if (v is Map<*, *>) v else mapOf("item" to v), 0, sb); return sb.toString().trim()
    }
    @Suppress("UNCHECKED_CAST")
    private fun xel(e: Element): Any? {
        val m = LinkedHashMap<String, Any?>(); val at = e.attributes
        for (i in 0 until at.length) m["@" + at.item(i).nodeName] = at.item(i).nodeValue
        val ch = e.childNodes; val text = StringBuilder()
        for (i in 0 until ch.length) {
            val n = ch.item(i)
            when (n.nodeType) {
                Node.ELEMENT_NODE -> {
                    val k = n.nodeName; val v = xel(n as Element)
                    if (m.containsKey(k)) { val ex = m[k]; if (ex is MutableList<*> && ex.isNotEmpty() && m.getValue(k) is ArrayList<*>) (ex as MutableList<Any?>).add(v) else m[k] = arrayListOf(ex, v) } else m[k] = v
                }
                Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> text.append(n.nodeValue)
            }
        }
        val t = text.toString().trim()
        if (m.isEmpty()) return t
        if (t.isNotEmpty()) m["#text"] = t
        return m
    }
    fun xmlToJson(s: String): String { val r = Xml.parseDom(s).documentElement; return Json.write(mapOf(r.nodeName to xel(r))) }

    // ---------- JSON <-> CSV ----------
    fun jsonToCsv(s: String, d: Char = ','): String {
        var v = Json.parse(s); if (v is Map<*, *> && v.size == 1 && v.values.first() is List<*>) v = v.values.first()
        val rows = if (v is List<*>) v else listOf(v)
        val maps = rows.map { if (it is Map<*, *>) it else mapOf("value" to it) }
        val keys = LinkedHashSet<String>(); maps.forEach { r -> r.keys.forEach { keys.add(it.toString()) } }
        fun cell(x: Any?) = csvEscape(when (x) { null -> ""; is Map<*, *>, is List<*> -> Json.write(x, 0); else -> x.toString() }, d)
        return (listOf(keys.joinToString(d.toString()) { csvEscape(it, d) }) + maps.map { r -> keys.joinToString(d.toString()) { k -> cell(r[k]) } }).joinToString("\n")
    }
    fun csvToJson(s: String, d: Char? = null): String {
        val rows = csvParse(s, d ?: detectDelim(s)); if (rows.isEmpty()) return "[]"
        val h = rows[0]
        return Json.write(rows.drop(1).map { r -> LinkedHashMap<String, Any?>().also { m -> h.forEachIndexed { i, k -> m[k] = r.getOrNull(i)?.let { c -> c.toLongOrNull() ?: c.toDoubleOrNull() ?: when (c) { "true" -> true; "false" -> false; else -> c } } } } })
    }
    fun propsToJson(s: String): String {
        val m = LinkedHashMap<String, Any?>()
        lines(s).forEach { l -> val t = l.trim(); if (t.isEmpty() || t.startsWith("#") || t.startsWith("!") || t.startsWith(";") || t.startsWith("[")) return@forEach
            val i = t.indexOfFirst { it == '=' || it == ':' }; if (i > 0) m[t.substring(0, i).trim()] = t.substring(i + 1).trim().trim('"') }
        return Json.write(m)
    }
    fun jsonToProps(s: String): String {
        val out = ArrayList<String>()
        fun walk(v: Any?, p: String) { when (v) { is Map<*, *> -> v.forEach { (k, x) -> walk(x, if (p.isEmpty()) "$k" else "$p.$k") }; is List<*> -> v.forEachIndexed { i, x -> walk(x, "$p[$i]") }; else -> out.add("$p=${v ?: ""}") } }
        walk(Json.parse(s), ""); return out.joinToString("\n")
    }

    // ---------- JSON -> typed code ----------
    sealed class T { object S : T(); object I : T(); object D : T(); object B : T(); object A : T(); data class L(val e: T) : T(); data class O(val n: String) : T() }
    class Infer {
        val classes = LinkedHashMap<String, LinkedHashMap<String, T>>()
        private fun singular(n: String): String { val p = pascal(n); return when { p.endsWith("ies") -> p.dropLast(3) + "y"; p.endsWith("s") && p.length > 1 -> p.dropLast(1); else -> p }.ifEmpty { "Item" } }
        fun infer(v: Any?, name: String): T = when (v) {
            null -> T.A
            is String -> T.S
            is Boolean -> T.B
            is Long, is Int -> T.I
            is Double -> T.D
            is List<*> -> T.L(if (v.isEmpty()) T.A else elems(v, name))
            is Map<*, *> -> {
                val cn = pascal(name).ifEmpty { "Root" }; val f = classes.getOrPut(cn) { LinkedHashMap() }
                v.forEach { (k, x) -> val t = infer(x, k.toString()); val old = f[k.toString()]; f[k.toString()] = if (old == null || old == T.A) t else if (old == T.I && t == T.D) T.D else old }
                T.O(cn)
            }
            else -> T.S
        }
        private fun elems(l: List<*>, name: String): T {
            if (l.all { it is Map<*, *> }) {
                val merged = LinkedHashMap<String, Any?>()
                l.forEach { (it as Map<*, *>).forEach { (k, x) -> if (merged[k.toString()] == null) merged[k.toString()] = x } }
                return infer(merged, singular(name)).also { classes.getOrPut(singular(name)) { LinkedHashMap() } }
            }
            val ts = l.map { infer(it, singular(name)) }.distinct()
            return if (ts.size == 1) ts[0] else if (ts.toSet() == setOf<T>(T.I, T.D)) T.D else T.A
        }
    }
    private val prim = mapOf(
        "kotlin" to listOf("String", "Long", "Double", "Boolean", "Any?"), "ts" to listOf("string", "number", "number", "boolean", "any"),
        "java" to listOf("String", "Long", "Double", "Boolean", "Object"), "go" to listOf("string", "int64", "float64", "bool", "interface{}"),
        "swift" to listOf("String", "Int", "Double", "Bool", "Any?"), "rust" to listOf("String", "i64", "f64", "bool", "serde_json::Value"),
        "cs" to listOf("string", "long", "double", "bool", "object"), "dart" to listOf("String", "int", "double", "bool", "dynamic"),
        "py" to listOf("str", "int", "float", "bool", "Any"), "php" to listOf("string", "int", "float", "bool", "mixed"))
    private fun tn(t: T, lang: String): String = when (t) {
        T.S -> prim.getValue(lang)[0]; T.I -> prim.getValue(lang)[1]; T.D -> prim.getValue(lang)[2]; T.B -> prim.getValue(lang)[3]; T.A -> prim.getValue(lang)[4]
        is T.O -> t.n
        is T.L -> when (lang) { "ts" -> tn(t.e, lang) + "[]"; "go" -> "[]" + tn(t.e, lang); "swift" -> "[" + tn(t.e, lang) + "]"; "rust" -> "Vec<" + tn(t.e, lang) + ">"; "py" -> "List[" + tn(t.e, lang) + "]"; "php" -> "array"; else -> "List<" + tn(t.e, lang) + ">" }
    }
    fun jsonToCode(json: String, lang: String, root: String): String {
        val inf = Infer(); inf.infer(Json.parse(json), root)
        val out = ArrayList<String>()
        for ((cn, f) in inf.classes) {
            val sb = StringBuilder()
            when (lang) {
                "kotlin" -> { sb.append("data class $cn(\n"); sb.append(f.entries.joinToString(",\n") { "    val ${camel(it.key).ifEmpty { "value" }}: ${tn(it.value, lang)}" }); sb.append("\n)") }
                "ts" -> { sb.append("export interface $cn {\n"); f.forEach { (k, t) -> sb.append("  ${if (Regex("[A-Za-z_]\\w*").matches(k)) k else Json.quote(k)}: ${tn(t, lang)};\n") }; sb.append("}") }
                "java" -> { sb.append("public record $cn(\n"); sb.append(f.entries.joinToString(",\n") { "    ${tn(it.value, lang)} ${camel(it.key)}" }); sb.append("\n) {}") }
                "go" -> { sb.append("type $cn struct {\n"); f.forEach { (k, t) -> sb.append("\t${pascal(k)} ${tn(t, lang)} `json:\"$k\"`\n") }; sb.append("}") }
                "swift" -> { sb.append("struct $cn: Codable {\n"); f.forEach { (k, t) -> sb.append("    let ${camel(k)}: ${tn(t, lang)}\n") }; sb.append("}") }
                "rust" -> { sb.append("#[derive(Debug, Serialize, Deserialize)]\npub struct $cn {\n"); f.forEach { (k, t) -> if (snake(k) != k) sb.append("    #[serde(rename = \"$k\")]\n"); sb.append("    pub ${snake(k)}: ${tn(t, lang)},\n") }; sb.append("}") }
                "cs" -> { sb.append("public class $cn\n{\n"); f.forEach { (k, t) -> sb.append("    [JsonPropertyName(\"$k\")]\n    public ${tn(t, lang)} ${pascal(k)} { get; set; }\n") }; sb.append("}") }
                "dart" -> { sb.append("class $cn {\n"); f.forEach { (k, t) -> sb.append("  final ${tn(t, lang)} ${camel(k)};\n") }; sb.append("\n  $cn({${f.keys.joinToString(", ") { "required this.${camel(it)}" }}});\n}") }
                "py" -> { sb.append("@dataclass\nclass $cn:\n"); if (f.isEmpty()) sb.append("    pass") else f.forEach { (k, t) -> sb.append("    ${snake(k)}: ${tn(t, lang)}\n") } }
                "php" -> { sb.append("class $cn\n{\n"); f.forEach { (k, t) -> sb.append("    public ${tn(t, lang)} \$${camel(k)};\n") }; sb.append("}") }
            }
            out.add(sb.toString().trimEnd())
        }
        val head = when (lang) { "py" -> "from dataclasses import dataclass\nfrom typing import Any, List\n\n"; "cs" -> "using System.Collections.Generic;\nusing System.Text.Json.Serialization;\n\n"; "php" -> "<?php\n\n"; "java" -> "import java.util.List;\n\n"; "rust" -> "use serde::{Deserialize, Serialize};\n\n"; "dart" -> ""; else -> "" }
        return head + out.reversed().joinToString("\n\n")
    }
    private fun schema(v: Any?): Any? = when (v) {
        is Map<*, *> -> linkedMapOf("type" to "object", "properties" to LinkedHashMap<String, Any?>().also { p -> v.forEach { (k, x) -> p[k.toString()] = schema(x) } }, "required" to v.keys.map { it.toString() })
        is List<*> -> linkedMapOf("type" to "array", "items" to (if (v.isEmpty()) LinkedHashMap<String, Any?>() else if (v.all { it is Map<*, *> }) schema(LinkedHashMap<String, Any?>().also { mm -> v.forEach { (it as Map<*, *>).forEach { (k, x) -> if (mm[k.toString()] == null) mm[k.toString()] = x } } }) else schema(v[0])))
        is String -> mapOf("type" to "string"); is Long, is Int -> mapOf("type" to "integer"); is Double -> mapOf("type" to "number")
        is Boolean -> mapOf("type" to "boolean"); null -> mapOf("type" to "null"); else -> mapOf("type" to "string")
    }
    fun jsonSchema(s: String) = Json.write(linkedMapOf("\$schema" to "https://json-schema.org/draft/2020-12/schema") + (schema(Json.parse(s)) as Map<String, Any?>))

    // ---------- cURL ----------
    fun shellSplit(s: String): List<String> {
        val out = ArrayList<String>(); val cur = StringBuilder(); var q = '\u0000'; var has = false; var i = 0
        val t = s.replace("\\\r\n", " ").replace("\\\n", " ")
        while (i < t.length) {
            val c = t[i]
            when {
                q != '\u0000' -> if (c == q) q = '\u0000' else if (c == '\\' && q == '"' && i + 1 < t.length) { i++; cur.append(t[i]) } else cur.append(c)
                c == '"' || c == '\'' -> { q = c; has = true }
                c.isWhitespace() -> if (has || cur.isNotEmpty()) { out.add(cur.toString()); cur.clear(); has = false }
                c == '\\' && i + 1 < t.length -> { i++; cur.append(t[i]) }
                else -> cur.append(c)
            }
            i++
        }
        if (has || cur.isNotEmpty()) out.add(cur.toString())
        return out
    }
    class Req(var url: String = "", var method: String = "", val headers: LinkedHashMap<String, String> = LinkedHashMap(), var body: String? = null)
    fun parseCurl(cmd: String): Req {
        val a = shellSplit(cmd.trim()); val r = Req(); var i = if (a.firstOrNull() == "curl") 1 else 0
        while (i < a.size) {
            val x = a[i]
            fun nxt() = a.getOrElse(++i) { "" }
            when {
                x == "-X" || x == "--request" -> r.method = nxt().uppercase()
                x == "-H" || x == "--header" -> { val h = nxt(); val c = h.indexOf(':'); if (c > 0) r.headers[h.substring(0, c).trim()] = h.substring(c + 1).trim() }
                x in setOf("-d", "--data", "--data-raw", "--data-binary", "--data-ascii", "--data-urlencode") -> r.body = (r.body?.let { it + "&" } ?: "") + nxt()
                x == "-u" || x == "--user" -> r.headers["Authorization"] = "Basic " + java.util.Base64.getEncoder().encodeToString(nxt().toByteArray())
                x == "-A" || x == "--user-agent" -> r.headers["User-Agent"] = nxt()
                x == "-b" || x == "--cookie" -> r.headers["Cookie"] = nxt()
                x == "--url" -> r.url = nxt()
                x.startsWith("-") -> {}
                else -> if (r.url.isEmpty()) r.url = x
            }
            i++
        }
        if (r.method.isEmpty()) r.method = if (r.body != null) "POST" else "GET"
        return r
    }
    fun curlTo(cmd: String, lang: String): String {
        val r = parseCurl(cmd); need(r.url.isNotEmpty(), "No URL found in cURL command")
        val hs = r.headers; val body = r.body
        return when (lang) {
            "fetch" -> "fetch(${Json.quote(r.url)}, {\n  method: \"${r.method}\"" + (if (hs.isNotEmpty()) ",\n  headers: " + Json.write(hs, 2).replace("\n", "\n  ") else "") + (if (body != null) ",\n  body: ${Json.quote(body)}" else "") + "\n})\n  .then(r => r.text())\n  .then(console.log);"
            "axios" -> "import axios from \"axios\";\n\naxios.request({\n  method: \"${r.method.lowercase()}\",\n  url: ${Json.quote(r.url)}" + (if (hs.isNotEmpty()) ",\n  headers: " + Json.write(hs, 2).replace("\n", "\n  ") else "") + (if (body != null) ",\n  data: ${Json.quote(body)}" else "") + "\n}).then(res => console.log(res.data));"
            "python" -> "import requests\n\nresponse = requests.request(\n    \"${r.method}\",\n    ${Json.quote(r.url)}" + (if (hs.isNotEmpty()) ",\n    headers=" + Json.write(hs, 4).replace("\n", "\n    ") else "") + (if (body != null) ",\n    data=${Json.quote(body)}" else "") + "\n)\nprint(response.text)"
            "go" -> "package main\n\nimport (\n\t\"fmt\"\n\t\"io\"\n\t\"net/http\"\n\t\"strings\"\n)\n\nfunc main() {\n\treq, _ := http.NewRequest(\"${r.method}\", ${Json.quote(r.url)}, strings.NewReader(${Json.quote(body ?: "")}))\n" + hs.entries.joinToString("") { "\treq.Header.Set(${Json.quote(it.key)}, ${Json.quote(it.value)})\n" } + "\tres, err := http.DefaultClient.Do(req)\n\tif err != nil { panic(err) }\n\tdefer res.Body.Close()\n\tb, _ := io.ReadAll(res.Body)\n\tfmt.Println(string(b))\n}"
            "php" -> "<?php\n\$ch = curl_init(${Json.quote(r.url).replace("$", "\\$")});\ncurl_setopt(\$ch, CURLOPT_CUSTOMREQUEST, \"${r.method}\");\ncurl_setopt(\$ch, CURLOPT_RETURNTRANSFER, true);\n" + (if (hs.isNotEmpty()) "curl_setopt(\$ch, CURLOPT_HTTPHEADER, [" + hs.entries.joinToString(", ") { Json.quote(it.key + ": " + it.value) } + "]);\n" else "") + (if (body != null) "curl_setopt(\$ch, CURLOPT_POSTFIELDS, ${Json.quote(body).replace("$", "\\$")});\n" else "") + "echo curl_exec(\$ch);\ncurl_close(\$ch);"
            "okhttp" -> "val client = OkHttpClient()\nval request = Request.Builder()\n    .url(${Json.quote(r.url).replace("$", "\\$")})\n" + hs.entries.joinToString("") { "    .addHeader(${Json.quote(it.key)}, ${Json.quote(it.value)})\n" } + "    .method(\"${r.method}\", " + (if (body != null) "${Json.quote(body).replace("$", "\\$")}.toRequestBody()" else if (r.method in setOf("POST", "PUT", "PATCH")) "\"\".toRequestBody()" else "null") + ")\n    .build()\nclient.newCall(request).execute().use { println(it.body?.string()) }"
            "swift" -> "var request = URLRequest(url: URL(string: ${Json.quote(r.url)})!)\nrequest.httpMethod = \"${r.method}\"\n" + hs.entries.joinToString("") { "request.setValue(${Json.quote(it.value)}, forHTTPHeaderField: ${Json.quote(it.key)})\n" } + (if (body != null) "request.httpBody = ${Json.quote(body)}.data(using: .utf8)\n" else "") + "URLSession.shared.dataTask(with: request) { data, _, _ in\n    if let data = data { print(String(data: data, encoding: .utf8) ?? \"\") }\n}.resume()"
            else -> "?"
        }
    }

    // ---------- numbers ----------
    fun bases(n: BigInteger): String {
        val bits = n.abs().bitLength().coerceAtLeast(1)
        val sb = StringBuilder()
        sb.append("DEC : $n\nHEX : ${if (n.signum() < 0) "-" else ""}0x${n.abs().toString(16).uppercase()}\nOCT : ${n.toString(8)}\nBIN : ${n.toString(2)}\nB32 : ${n.toString(32)}\nB36 : ${n.toString(36)}\nBits: $bits\n")
        for (w in listOf(8, 16, 32, 64)) {
            val mod = BigInteger.ONE.shiftLeft(w)
            val u = n.mod(mod)
            sb.append("\n${w.toString().padStart(2)}-bit two's complement: 0x${u.toString(16).uppercase().padStart(w / 4, '0')}  (unsigned $u, signed ${if (u.testBit(w - 1)) u - mod else u})")
        }
        return sb.toString()
    }
    fun parseNum(s0: String, radix: Int?): BigInteger {
        var s = s0.trim().replace("_", "").replace(" ", ""); var neg = false
        if (s.startsWith("-")) { neg = true; s = s.substring(1) }
        var r = radix ?: 10
        val l = s.lowercase()
        if (radix == null) { when { l.startsWith("0x") -> { r = 16; s = s.substring(2) }; l.startsWith("0b") -> { r = 2; s = s.substring(2) }; l.startsWith("0o") -> { r = 8; s = s.substring(2) } } }
        else if (radix == 16 && l.startsWith("0x")) s = s.substring(2) else if (radix == 2 && l.startsWith("0b")) s = s.substring(2)
        val v = BigInteger(s, r); return if (neg) v.negate() else v
    }

    // ---------- units ----------
    fun bytesAll(b: java.math.BigDecimal): String {
        val units = listOf("B" to 1.0, "KB" to 1e3, "KiB" to 1024.0, "MB" to 1e6, "MiB" to 1048576.0, "GB" to 1e9, "GiB" to 1073741824.0, "TB" to 1e12, "TiB" to 1099511627776.0)
        return units.joinToString("\n") { (u, f) -> "${u.padEnd(4)}: " + java.math.BigDecimal(b.toDouble() / f).setScale(6, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() }
    }
    fun parseSize(s: String): Double {
        val mt = Regex("([\\d.]+)\\s*([a-zA-Z]*)").matchEntire(s.trim()) ?: throw IllegalArgumentException("Use e.g. 2.5 GiB")
        val f = mapOf("" to 1.0, "b" to 1.0, "kb" to 1e3, "kib" to 1024.0, "mb" to 1e6, "mib" to 1048576.0, "gb" to 1e9, "gib" to 1073741824.0, "tb" to 1e12, "tib" to 1099511627776.0)
        return mt.groupValues[1].toDouble() * (f[mt.groupValues[2].lowercase()] ?: throw IllegalArgumentException("Unknown unit"))
    }
    fun duration(sec: Long): String { val d = sec / 86400; val h = sec % 86400 / 3600; val m = sec % 3600 / 60; val s = sec % 60; return "${d}d ${h}h ${m}m ${s}s" }
    fun parseDuration(s: String): Long = Regex("(\\d+(?:\\.\\d+)?)\\s*(d|h|m|s|ms)").findAll(s.lowercase()).sumOf { val v = it.groupValues[1].toDouble(); when (it.groupValues[2]) { "d" -> v * 86400; "h" -> v * 3600; "m" -> v * 60; "s" -> v; else -> v / 1000 }.toLong() }
    fun cssUnits(input: String, root: Double, vw: Double): String = lines(input).filter { it.isNotBlank() }.joinToString("\n\n") { l ->
        val mt = Regex("(-?[\\d.]+)\\s*(px|rem|em|pt|pc|in|cm|mm|vw)?").matchEntire(l.trim()) ?: return@joinToString "$l → ?"
        val v = mt.groupValues[1].toDouble(); val u = mt.groupValues[2].ifEmpty { "px" }
        val px = when (u) { "px" -> v; "rem", "em" -> v * root; "pt" -> v * 96 / 72; "pc" -> v * 16; "in" -> v * 96; "cm" -> v * 96 / 2.54; "mm" -> v * 96 / 25.4; else -> v * vw / 100 }
        "${l.trim()}\n  px  ${fmt(px)}\n  rem ${fmt(px / root)}\n  pt  ${fmt(px * 72 / 96)}\n  pc  ${fmt(px / 16)}\n  in  ${fmt(px / 96)}\n  cm  ${fmt(px * 2.54 / 96)}\n  mm  ${fmt(px * 25.4 / 96)}\n  vw  ${fmt(px / vw * 100)}"
    }

    // ---------- markdown <-> html ----------
    private fun inline(s0: String): String {
        var s = s0.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        s = s.replace(Regex("!\\[([^]]*)]\\(([^)]+)\\)"), "<img src=\"$2\" alt=\"$1\">")
        s = s.replace(Regex("\\[([^]]+)]\\(([^)]+)\\)"), "<a href=\"$2\">$1</a>")
        s = s.replace(Regex("`([^`]+)`"), "<code>$1</code>")
        s = s.replace(Regex("\\*\\*([^*]+)\\*\\*"), "<strong>$1</strong>").replace(Regex("(?<![*\\w])\\*([^*]+)\\*(?!\\w)"), "<em>$1</em>")
        return s.replace(Regex("~~([^~]+)~~"), "<del>$1</del>")
    }
    fun mdToHtml(md: String): String {
        val out = ArrayList<String>(); var inCode = false; var list: String? = null; val para = ArrayList<String>()
        fun flushP() { if (para.isNotEmpty()) { out.add("<p>" + para.joinToString(" ") { inline(it) } + "</p>"); para.clear() } }
        fun closeList() { list?.let { out.add("</$it>") }; list = null }
        for (line in lines(md)) {
            if (line.trim().startsWith("```")) { flushP(); closeList(); if (!inCode) { out.add("<pre><code>"); inCode = true } else { out.add("</code></pre>"); inCode = false }; continue }
            if (inCode) { out.add(line.replace("&", "&amp;").replace("<", "&lt;")); continue }
            val t = line.trim()
            val h = Regex("^(#{1,6})\\s+(.*)").find(t); val ul = Regex("^[-*+]\\s+(.*)").find(t); val ol = Regex("^\\d+[.)]\\s+(.*)").find(t)
            when {
                t.isEmpty() -> { flushP(); closeList() }
                h != null -> { flushP(); closeList(); out.add("<h${h.groupValues[1].length}>${inline(h.groupValues[2])}</h${h.groupValues[1].length}>") }
                ul != null -> { flushP(); if (list != "ul") { closeList(); out.add("<ul>"); list = "ul" }; out.add("  <li>${inline(ul.groupValues[1])}</li>") }
                ol != null -> { flushP(); if (list != "ol") { closeList(); out.add("<ol>"); list = "ol" }; out.add("  <li>${inline(ol.groupValues[1])}</li>") }
                t.startsWith(">") -> { flushP(); closeList(); out.add("<blockquote>${inline(t.removePrefix(">").trim())}</blockquote>") }
                t == "---" || t == "***" -> { flushP(); closeList(); out.add("<hr>") }
                else -> { closeList(); para.add(t) }
            }
        }
        flushP(); closeList(); return out.joinToString("\n")
    }
    fun htmlToMd(html: String): String {
        var s = html
        for (i in 1..6) s = s.replace(Regex("<h$i[^>]*>(.*?)</h$i>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "\n" + "#".repeat(i) + " $1\n")
        s = s.replace(Regex("<(strong|b)>(.*?)</\\1>", RegexOption.IGNORE_CASE), "**$2**").replace(Regex("<(em|i)>(.*?)</\\1>", RegexOption.IGNORE_CASE), "*$2*")
        s = s.replace(Regex("<a [^>]*href=\"([^\"]*)\"[^>]*>(.*?)</a>", RegexOption.IGNORE_CASE), "[$2]($1)")
        s = s.replace(Regex("<img [^>]*src=\"([^\"]*)\"[^>]*alt=\"([^\"]*)\"[^>]*>", RegexOption.IGNORE_CASE), "![$2]($1)")
        s = s.replace(Regex("<pre><code>(.*?)</code></pre>", RegexOption.DOT_MATCHES_ALL), "\n```\n$1\n```\n").replace(Regex("<code>(.*?)</code>"), "`$1`")
        s = s.replace(Regex("<li[^>]*>(.*?)</li>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "- $1\n").replace(Regex("<br\\s*/?>"), "\n").replace(Regex("</p>"), "\n\n")
        s = s.replace(Regex("<blockquote>(.*?)</blockquote>", RegexOption.DOT_MATCHES_ALL), "> $1\n").replace(Regex("<hr\\s*/?>"), "\n---\n")
        s = s.replace(Regex("<[^>]+>"), "").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
        return s.replace(Regex("\n{3,}"), "\n\n").trim()
    }
}

private fun conv(name: String, f: (String) -> String) = m(name, f)

val convertTools: List<Tool> = listOf(
    tool("data-convert", "JSON ⇄ YAML ⇄ XML ⇄ CSV ⇄ TOML ⇄ TSV", Category.CONVERT, "Convert between the common data formats",
        conv("JSON → YAML") { Yaml.write(Json.parse(it)) }, conv("YAML → JSON") { Json.write(Yaml.parse(it)) },
        conv("JSON → XML") { Conv.jsonToXml(it) }, conv("XML → JSON") { Conv.xmlToJson(it) },
        conv("JSON → CSV") { Conv.jsonToCsv(it) }, conv("CSV → JSON") { Conv.csvToJson(it) },
        conv("JSON → TSV") { Conv.jsonToCsv(it, '\t') }, conv("TSV → JSON") { Conv.csvToJson(it, '\t') },
        conv("JSON → TOML") { Toml.write(Json.parse(it) as Map<*, *>) }, conv("TOML → JSON") { Json.write(Toml.parse(it)) },
        conv("CSV → TSV") { csvParse(it, detectDelim(it)).joinToString("\n") { r -> r.joinToString("\t") } },
        conv("TSV → CSV") { csvParse(it, '\t').joinToString("\n") { r -> r.joinToString(",") { c -> csvEscape(c) } } },
        conv("Props/.env → JSON") { Conv.propsToJson(it) }, conv("JSON → Props") { Conv.jsonToProps(it) },
        hint = "Source data", sample = "{\"users\":[{\"id\":1,\"name\":\"Ada\"},{\"id\":2,\"name\":\"Linus\"}]}", kw = "convert yaml xml csv toml tsv properties"),
    tool("json-code", "JSON → Classes / Structs", Category.CONVERT, "Generate typed models for 10 languages from sample JSON",
        mp("Kotlin") { s, p -> Conv.jsonToCode(s, "kotlin", p.getOrElse(0) { "" }.ifBlank { "Root" }) },
        mp("TypeScript") { s, p -> Conv.jsonToCode(s, "ts", p.getOrElse(0) { "" }.ifBlank { "Root" }) },
        mp("Java") { s, p -> Conv.jsonToCode(s, "java", p.getOrElse(0) { "" }.ifBlank { "Root" }) },
        mp("Swift") { s, p -> Conv.jsonToCode(s, "swift", p.getOrElse(0) { "" }.ifBlank { "Root" }) },
        mp("Go") { s, p -> Conv.jsonToCode(s, "go", p.getOrElse(0) { "" }.ifBlank { "Root" }) },
        mp("Rust") { s, p -> Conv.jsonToCode(s, "rust", p.getOrElse(0) { "" }.ifBlank { "Root" }) },
        mp("C#") { s, p -> Conv.jsonToCode(s, "cs", p.getOrElse(0) { "" }.ifBlank { "Root" }) },
        mp("Dart") { s, p -> Conv.jsonToCode(s, "dart", p.getOrElse(0) { "" }.ifBlank { "Root" }) },
        mp("Python") { s, p -> Conv.jsonToCode(s, "py", p.getOrElse(0) { "" }.ifBlank { "Root" }) },
        mp("PHP") { s, p -> Conv.jsonToCode(s, "php", p.getOrElse(0) { "" }.ifBlank { "Root" }) },
        hint = "Sample JSON", params = listOf("Root class name (default Root)"),
        sample = "{\"id\":7,\"name\":\"Ada\",\"score\":9.5,\"tags\":[\"a\",\"b\"],\"address\":{\"city\":\"Accra\",\"zip\":\"00233\"},\"orders\":[{\"sku\":\"x1\",\"qty\":2}]}", kw = "model dto data class struct interface"),
    tool("json-schema", "JSON → JSON Schema", Category.CONVERT, "Infer a JSON Schema (draft 2020-12) from a sample",
        m("Generate") { Conv.jsonSchema(it) }, hint = "Sample JSON", sample = "{\"id\":1,\"email\":\"a@b.c\",\"tags\":[\"x\"]}"),
    tool("curl", "cURL → Code", Category.CONVERT, "Turn a cURL command into fetch, axios, Python, Go, PHP, OkHttp or Swift",
        m("fetch") { Conv.curlTo(it, "fetch") }, m("axios") { Conv.curlTo(it, "axios") }, m("Python requests") { Conv.curlTo(it, "python") },
        m("Go") { Conv.curlTo(it, "go") }, m("PHP") { Conv.curlTo(it, "php") }, m("Kotlin OkHttp") { Conv.curlTo(it, "okhttp") }, m("Swift") { Conv.curlTo(it, "swift") },
        hint = "cURL command", sample = "curl -X POST https://api.example.com/v1/users -H 'Content-Type: application/json' -H 'Authorization: Bearer TOKEN' -d '{\"name\":\"Ada\"}'"),
    tool("base-convert", "Number Base Converter", Category.CONVERT, "Binary, octal, decimal, hex and any base up to 36 – with two's complement",
        m("Auto (0x / 0b / 0o)") { Conv.bases(Conv.parseNum(it, null)) }, m("From BIN") { Conv.bases(Conv.parseNum(it, 2)) },
        m("From OCT") { Conv.bases(Conv.parseNum(it, 8)) }, m("From DEC") { Conv.bases(Conv.parseNum(it, 10)) },
        m("From HEX") { Conv.bases(Conv.parseNum(it, 16)) },
        mp("From base N") { s, p -> Conv.bases(Conv.parseNum(s, p.getOrElse(0) { "" }.ifBlank { "36" }.toInt())) },
        hint = "Number", params = listOf("Base N (2–36) for 'From base N'"), sample = "0xFF", kw = "hex binary octal decimal radix"),
    tool("units", "Unit Converters", Category.CONVERT, "Bytes (KB/KiB…), durations, angles, temperature",
        m("Bytes → all units") { Conv.bytesAll(it.trim().toBigDecimal()) },
        m("Parse size (2.5 GiB)") { val b = Conv.parseSize(it); "${b.toLong()} bytes\n\n" + Conv.bytesAll(b.toBigDecimal()) },
        m("Seconds → d h m s") { Conv.duration(it.trim().toDouble().toLong()) },
        m("Duration → seconds") { Conv.parseDuration(it).toString() + " s" },
        m("Degrees ↔ radians") { val v = it.trim().toDouble(); "${fmt(v)}° = ${v * Math.PI / 180} rad\n${fmt(v)} rad = ${Math.toDegrees(v)}°" },
        m("Temperature") { val v = it.trim().toDouble(); "${fmt(v)} °C = ${fmt(v * 9 / 5 + 32)} °F = ${fmt(v + 273.15)} K\n${fmt(v)} °F = ${fmt((v - 32) * 5 / 9)} °C" },
        hint = "Value", sample = "1536000"),
    tool("css-units", "CSS Units: px ↔ rem ↔ em ↔ pt", Category.CONVERT, "One value per line, e.g. 24px, 1.5rem, 12pt",
        mp("Convert all") { s, p -> Conv.cssUnits(s, p.getOrElse(0) { "" }.toDoubleOrNull() ?: 16.0, p.getOrElse(1) { "" }.toDoubleOrNull() ?: 390.0) },
        hint = "Values", params = listOf("Root font size px (16)", "Viewport width px (390)"), sample = "24px\n1.5rem\n12pt"),
    tool("case", "Case Converter", Category.CONVERT, "camelCase, snake_case, kebab-case, PascalCase, CONSTANT_CASE…",
        m("All cases") { s -> listOf("camelCase" to camel(s), "PascalCase" to pascal(s), "snake_case" to snake(s), "kebab-case" to kebab(s), "CONSTANT_CASE" to constant(s), "Title Case" to titleCase(s), "dot.case" to words(s).joinToString(".") { it.lowercase() }, "path/case" to words(s).joinToString("/") { it.lowercase() }, "UPPER" to s.uppercase(), "lower" to s.lowercase()).joinToString("\n") { (k, v) -> "${k.padEnd(14)} $v" } },
        m("camelCase") { lines(it).joinToString("\n") { l -> camel(l) } }, m("PascalCase") { lines(it).joinToString("\n") { l -> pascal(l) } },
        m("snake_case") { lines(it).joinToString("\n") { l -> snake(l) } }, m("kebab-case") { lines(it).joinToString("\n") { l -> kebab(l) } },
        m("CONSTANT_CASE") { lines(it).joinToString("\n") { l -> constant(l) } }, m("Swap case") { s -> s.map { if (it.isUpperCase()) it.lowercaseChar() else it.uppercaseChar() }.joinToString("") },
        hint = "Text / identifiers", sample = "user profile pictureURL"),
    tool("md-html", "Markdown ⇄ HTML", Category.CONVERT, "Convert Markdown to HTML and back",
        m("Markdown → HTML") { Conv.mdToHtml(it) }, m("HTML → Markdown") { Conv.htmlToMd(it) },
        hint = "Source", sample = "# Title\n\nSome **bold** text and a [link](https://example.com).\n\n- one\n- two"),
    custom("image-base64", "Image ⇄ Base64 & Compressor", Category.CONVERT, "Pick an image → data URI, decode Base64 to image, resize/compress on-device", "png jpg webp resize compress"),
)
