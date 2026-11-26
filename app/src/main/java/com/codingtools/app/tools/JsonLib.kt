package com.codingtools.app.tools

/** Dependency-free JSON model: Map / List / String / Long / Double / Boolean / null. */
object Json {
    fun parse(s: String): Any? {
        val p = P(s); p.ws(); val v = p.value(); p.ws()
        if (p.i < s.length) throw IllegalArgumentException("Unexpected '${s[p.i]}' at position ${p.i}")
        return v
    }

    private class P(val s: String) {
        var i = 0
        fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
        fun fail(m: String): Nothing = throw IllegalArgumentException("$m at position $i (line ${s.take(i).count { it == '\n' } + 1})")
        fun value(): Any? {
            ws(); if (i >= s.length) fail("Unexpected end of input")
            val c = s[i]
            return when {
                c == '{' -> obj()
                c == '[' -> arr()
                c == '"' -> str()
                c == 't' -> lit("true", true)
                c == 'f' -> lit("false", false)
                c == 'n' -> lit("null", null)
                c == '-' || c.isDigit() -> num()
                else -> fail("Unexpected '$c'")
            }
        }
        fun lit(w: String, v: Any?): Any? { if (!s.startsWith(w, i)) fail("Invalid literal"); i += w.length; return v }
        fun num(): Any {
            val st = i; if (s[i] == '-') i++
            while (i < s.length && (s[i].isDigit() || s[i] in ".eE+-")) i++
            val t = s.substring(st, i)
            return t.toLongOrNull() ?: t.toDoubleOrNull() ?: fail("Invalid number '$t'")
        }
        fun str(): String {
            i++; val sb = StringBuilder()
            while (true) {
                if (i >= s.length) fail("Unterminated string")
                val c = s[i++]
                if (c == '"') return sb.toString()
                if (c == '\\') {
                    when (val e = s[i++]) {
                        'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r')
                        'b' -> sb.append('\b'); 'f' -> sb.append('\u000c')
                        '/', '\\', '"' -> sb.append(e)
                        'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                        else -> fail("Bad escape \\$e")
                    }
                } else sb.append(c)
            }
        }
        fun obj(): Any? {
            i++; val m = LinkedHashMap<String, Any?>(); ws()
            if (i < s.length && s[i] == '}') { i++; return m }
            while (true) {
                ws(); if (i >= s.length || s[i] != '"') fail("Expected string key")
                val k = str(); ws(); if (i >= s.length || s[i] != ':') fail("Expected ':'"); i++
                m[k] = value(); ws()
                if (i >= s.length) fail("Unterminated object")
                if (s[i] == ',') { i++; continue }
                if (s[i] == '}') { i++; return m }
                fail("Expected ',' or '}'")
            }
        }
        fun arr(): Any? {
            i++; val l = ArrayList<Any?>(); ws()
            if (i < s.length && s[i] == ']') { i++; return l }
            while (true) {
                l.add(value()); ws()
                if (i >= s.length) fail("Unterminated array")
                if (s[i] == ',') { i++; continue }
                if (s[i] == ']') { i++; return l }
                fail("Expected ',' or ']'")
            }
        }
    }

    fun quote(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) when (c) {
            '"' -> sb.append("\\\""); '\\' -> sb.append("\\\\"); '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r"); '\t' -> sb.append("\\t")
            else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
        }
        return sb.append('"').toString()
    }

    fun write(v: Any?, indent: Int = 2, sortKeys: Boolean = false): String { val sb = StringBuilder(); w(v, indent, 0, sb, sortKeys); return sb.toString() }

    private fun w(v: Any?, ind: Int, lvl: Int, sb: StringBuilder, sort: Boolean) {
        val nl = if (ind > 0) "\n" + " ".repeat(ind * (lvl + 1)) else ""
        val end = if (ind > 0) "\n" + " ".repeat(ind * lvl) else ""
        when (v) {
            null -> sb.append("null")
            is String -> sb.append(quote(v))
            is Boolean, is Long, is Int -> sb.append(v.toString())
            is Double -> sb.append(if (v == Math.floor(v) && Math.abs(v) < 1e15) v.toLong().toString() else v.toString())
            is Map<*, *> -> {
                if (v.isEmpty()) { sb.append("{}"); return }
                sb.append('{')
                val es = if (sort) v.entries.sortedBy { it.key.toString() } else v.entries.toList()
                es.forEachIndexed { i, e ->
                    if (i > 0) sb.append(',')
                    sb.append(nl).append(quote(e.key.toString())).append(if (ind > 0) ": " else ":")
                    w(e.value, ind, lvl + 1, sb, sort)
                }
                sb.append(end).append('}')
            }
            is List<*> -> {
                if (v.isEmpty()) { sb.append("[]"); return }
                sb.append('[')
                v.forEachIndexed { i, x -> if (i > 0) sb.append(','); sb.append(nl); w(x, ind, lvl + 1, sb, sort) }
                sb.append(end).append(']')
            }
            else -> sb.append(quote(v.toString()))
        }
    }

    /** Flattened "path = value" listing (tree view). */
    fun paths(v: Any?, path: String = "$", out: MutableList<String> = ArrayList()): List<String> {
        when (v) {
            is Map<*, *> -> if (v.isEmpty()) out.add("$path = {}") else v.forEach { (k, x) -> paths(x, "$path.$k", out) }
            is List<*> -> if (v.isEmpty()) out.add("$path = []") else v.forEachIndexed { i, x -> paths(x, "$path[$i]", out) }
            is String -> out.add("$path = ${quote(v)}")
            else -> out.add("$path = $v")
        }
        return out
    }
}

/** Minimal YAML subset (maps, lists, scalars, inline flow collections) – covers config files. */
object Yaml {
    fun parse(text: String): Any? {
        val lines = text.lines().map { it.replace("\t", "  ").replace(Regex("\\s+#.*$"), "") }
            .filter { it.trim().isNotEmpty() && !it.trim().startsWith("#") && it.trim() != "---" && it.trim() != "..." }
            .toMutableList()
        if (lines.isEmpty()) return null
        return block(lines, intArrayOf(0), ind(lines[0]))
    }
    private fun ind(l: String) = l.length - l.trimStart().length
    private fun isItem(l: String): Boolean { val t = l.trim(); return t == "-" || t.startsWith("- ") }
    private fun block(l: MutableList<String>, p: IntArray, i: Int): Any? = if (isItem(l[p[0]])) list(l, p, i) else map(l, p, i)
    private fun list(l: MutableList<String>, p: IntArray, i: Int): Any? {
        val out = ArrayList<Any?>()
        while (p[0] < l.size && ind(l[p[0]]) == i && isItem(l[p[0]])) {
            val rest = l[p[0]].trim().removePrefix("-").trim()
            if (rest.isEmpty()) {
                p[0]++
                if (p[0] < l.size && ind(l[p[0]]) > i) out.add(block(l, p, ind(l[p[0]]))) else out.add(null)
            } else if (Regex("^[^\"'\\[{][^:]*:(\\s.*)?$").matches(rest)) {
                l[p[0]] = " ".repeat(i + 2) + rest
                out.add(map(l, p, i + 2))
            } else { out.add(scalar(rest)); p[0]++ }
        }
        return out
    }
    private fun map(l: MutableList<String>, p: IntArray, i: Int): Any? {
        val m = LinkedHashMap<String, Any?>()
        while (p[0] < l.size && ind(l[p[0]]) == i && !isItem(l[p[0]])) {
            val t = l[p[0]].trim(); val c = t.indexOf(':')
            if (c < 0) throw IllegalArgumentException("Expected 'key: value' near '$t'")
            val k = t.substring(0, c).trim().trim('"', '\''); val v = t.substring(c + 1).trim(); p[0]++
            if (v.isEmpty() || v == "|" || v == ">") {
                if (v.isNotEmpty()) {
                    val sb = ArrayList<String>()
                    while (p[0] < l.size && ind(l[p[0]]) > i) { sb.add(l[p[0]].trim()); p[0]++ }
                    m[k] = sb.joinToString(if (v == "|") "\n" else " ")
                } else if (p[0] < l.size && (ind(l[p[0]]) > i || (ind(l[p[0]]) == i && isItem(l[p[0]])))) m[k] = block(l, p, ind(l[p[0]]))
                else m[k] = null
            } else m[k] = scalar(v)
        }
        return m
    }
    fun scalar(s: String): Any? {
        if ((s.startsWith("\"") && s.endsWith("\"") && s.length >= 2)) return runCatching { Json.parse(s) }.getOrDefault(s.substring(1, s.length - 1))
        if (s.startsWith("'") && s.endsWith("'") && s.length >= 2) return s.substring(1, s.length - 1).replace("''", "'")
        if (s.startsWith("[") || s.startsWith("{")) runCatching { return Json.parse(s) }
        return when (s.lowercase()) {
            "null", "~" -> null
            "true" -> true
            "false" -> false
            else -> s.toLongOrNull() ?: s.toDoubleOrNull() ?: s
        }
    }

    fun write(v: Any?): String { val sb = StringBuilder(); emit(v, 0, sb); return sb.toString().trimEnd() }
    private fun nonEmpty(x: Any?) = (x is Map<*, *> && x.isNotEmpty()) || (x is List<*> && x.isNotEmpty())
    private fun sc(x: Any?): String = when (x) {
        null -> "null"
        is Boolean, is Number -> if (x is Double && x == Math.floor(x)) x.toLong().toString() else x.toString()
        is Map<*, *> -> "{}"
        is List<*> -> "[]"
        else -> { val s = x.toString(); if (s.isEmpty() || s.contains(": ") || s.contains(" #") || s.first() in "-?:,[]{}#&*!|>'\"%@`" || s.lowercase() in setOf("true", "false", "null", "yes", "no", "~") || s.toDoubleOrNull() != null || s.contains('\n') || s.trim() != s) Json.quote(s) else s }
    }
    private fun emit(v: Any?, ind: Int, sb: StringBuilder) {
        val p = " ".repeat(ind)
        when (v) {
            is Map<*, *> -> if (v.isEmpty()) sb.append(p).append("{}\n") else v.forEach { (k, x) ->
                if (nonEmpty(x)) { sb.append(p).append(k).append(":\n"); emit(x, if (x is List<*>) ind else ind + 2, sb) }
                else sb.append(p).append(k).append(": ").append(sc(x)).append('\n')
            }
            is List<*> -> if (v.isEmpty()) sb.append(p).append("[]\n") else v.forEach { x ->
                if (x is Map<*, *> && x.isNotEmpty()) { val sub = StringBuilder(); emit(x, ind + 2, sub); sb.append(p).append("- ").append(sub.toString().substring(ind + 2)) }
                else if (x is List<*> && x.isNotEmpty()) { sb.append(p).append("-\n"); emit(x, ind + 2, sb) }
                else sb.append(p).append("- ").append(sc(x)).append('\n')
            }
            else -> sb.append(p).append(sc(v)).append('\n')
        }
    }
}

/** Parses JSON or YAML automatically. */
fun parseAny(s: String): Any? {
    val t = s.trim()
    return if (t.startsWith("{") || t.startsWith("[")) Json.parse(t) else Yaml.parse(t)
}
