package com.codingtools.app.tools

import java.util.Locale

fun words(s: String): List<String> {
    val spaced = s.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
        .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1 $2")
    return spaced.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
}
fun pascal(s: String) = words(s).joinToString("") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
fun camel(s: String) = pascal(s).replaceFirstChar { it.lowercase() }
fun snake(s: String) = words(s).joinToString("_") { it.lowercase() }
fun kebab(s: String) = words(s).joinToString("-") { it.lowercase() }
fun constant(s: String) = words(s).joinToString("_") { it.uppercase() }
fun titleCase(s: String) = words(s).joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }

fun ByteArray.hex(sep: String = ""): String = joinToString(sep) { "%02x".format(it) }
fun hexToBytes(s: String): ByteArray {
    val c = s.replace(Regex("(0x|\\\\x|[\\s,:-])"), "")
    require(c.length % 2 == 0) { "Hex string must have an even number of digits" }
    return ByteArray(c.length / 2) { c.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}

/** "key: value" lines -> map (also accepts key=value). */
fun kv(input: String): Map<String, String> {
    val out = LinkedHashMap<String, String>()
    input.lines().forEach { l ->
        val t = l.trim()
        if (t.isEmpty() || t.startsWith("#")) return@forEach
        val i = t.indexOfFirst { it == ':' || it == '=' }
        if (i > 0) out[t.substring(0, i).trim()] = t.substring(i + 1).trim()
    }
    return out
}

fun Map<String, String>.getOr(k: String, d: String) = this[k]?.takeIf { it.isNotBlank() } ?: d

fun table(rows: List<List<String>>, header: Boolean = true): String {
    if (rows.isEmpty()) return ""
    val cols = rows.maxOf { it.size }
    val w = IntArray(cols) { c -> rows.maxOf { r -> r.getOrElse(c) { "" }.length.coerceAtMost(60) } }
    val sb = StringBuilder()
    rows.forEachIndexed { ri, r ->
        sb.append((0 until cols).joinToString(" | ") { c -> r.getOrElse(c) { "" }.take(60).padEnd(w[c]) }.trimEnd()).append('\n')
        if (ri == 0 && header) sb.append(w.joinToString("-+-") { "-".repeat(it) }).append('\n')
    }
    return sb.toString().trimEnd()
}

fun lines(s: String) = s.split("\r\n", "\n", "\r")
fun String.indentBy(n: Int) = lines(this).joinToString("\n") { " ".repeat(n) + it }
fun need(cond: Boolean, msg: String) { if (!cond) throw IllegalArgumentException(msg) }
fun fmt(d: Double): String = if (d == Math.floor(d) && Math.abs(d) < 1e15) d.toLong().toString() else String.format(Locale.US, "%.6f", d).trimEnd('0').trimEnd('.')

fun csvParse(text: String, delim: Char = ','): List<List<String>> {
    val rows = ArrayList<List<String>>(); var row = ArrayList<String>(); val f = StringBuilder(); var q = false; var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            q -> if (c == '"') { if (i + 1 < text.length && text[i + 1] == '"') { f.append('"'); i++ } else q = false } else f.append(c)
            c == '"' -> q = true
            c == delim -> { row.add(f.toString()); f.clear() }
            c == '\n' || c == '\r' -> { if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++; row.add(f.toString()); f.clear(); rows.add(row); row = ArrayList() }
            else -> f.append(c)
        }
        i++
    }
    if (f.isNotEmpty() || row.isNotEmpty()) { row.add(f.toString()); rows.add(row) }
    return rows.filter { !(it.size == 1 && it[0].isEmpty()) }
}
fun detectDelim(t: String): Char {
    val first = t.lineSequence().firstOrNull() ?: return ','
    return listOf(',', ';', '\t', '|').maxByOrNull { d -> first.count { it == d } } ?: ','
}
fun csvEscape(s: String, d: Char = ','): String = if (s.any { it == d || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
