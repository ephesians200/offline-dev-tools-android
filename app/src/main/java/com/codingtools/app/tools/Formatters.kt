package com.codingtools.app.tools

import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource
import java.io.StringReader

object Toml {
    @Suppress("UNCHECKED_CAST")
    private fun descend(root: MutableMap<String, Any?>, parts: List<String>): MutableMap<String, Any?> {
        var n = root
        for (p in parts) {
            val x = n.getOrPut(p) { LinkedHashMap<String, Any?>() }
            n = when (x) {
                is MutableMap<*, *> -> x as MutableMap<String, Any?>
                is MutableList<*> -> x.last() as MutableMap<String, Any?>
                else -> throw IllegalArgumentException("Key conflict at '$p'")
            }
        }
        return n
    }
    private fun stripComment(l: String): String {
        var q = '\u0000'
        for (i in l.indices) { val c = l[i]; if (q != '\u0000') { if (c == q) q = '\u0000' } else if (c == '"' || c == '\'') q = c else if (c == '#') return l.substring(0, i) }
        return l
    }
    private fun splitTop(s: String): List<String> {
        val out = ArrayList<String>(); var d = 0; var q = '\u0000'; val cur = StringBuilder()
        for (c in s) {
            if (q != '\u0000') { if (c == q) q = '\u0000' } else if (c == '"' || c == '\'') q = c
            else if (c == '[' || c == '{') d++ else if (c == ']' || c == '}') d--
            if (c == ',' && d == 0 && q == '\u0000') { out.add(cur.toString()); cur.clear() } else cur.append(c)
        }
        if (cur.isNotBlank()) out.add(cur.toString())
        return out.map { it.trim() }.filter { it.isNotEmpty() }
    }
    private fun value(v0: String): Any? {
        val v = v0.trim()
        return when {
            v.startsWith("\"") -> Json.parse(v)
            v.startsWith("'") -> v.trim('\'')
            v == "true" -> true
            v == "false" -> false
            v.startsWith("[") -> splitTop(v.removeSurrounding("[", "]")).map { value(it) }
            v.startsWith("{") -> { val m = LinkedHashMap<String, Any?>(); splitTop(v.removeSurrounding("{", "}")).forEach { kvp -> val i = kvp.indexOf('='); m[kvp.substring(0, i).trim().trim('"')] = value(kvp.substring(i + 1)) }; m }
            else -> v.replace("_", "").let { it.toLongOrNull() ?: it.toDoubleOrNull() ?: v }
        }
    }
    @Suppress("UNCHECKED_CAST")
    fun parse(s: String): Map<String, Any?> {
        val root = LinkedHashMap<String, Any?>(); var cur: MutableMap<String, Any?> = root
        for (raw in s.lines()) {
            val l = stripComment(raw).trim(); if (l.isEmpty()) continue
            if (l.startsWith("[[")) {
                val parts = l.removeSurrounding("[[", "]]").split('.').map { it.trim().trim('"') }
                val parent = descend(root, parts.dropLast(1))
                val list = parent.getOrPut(parts.last()) { ArrayList<Any?>() } as MutableList<Any?>
                val nm = LinkedHashMap<String, Any?>(); list.add(nm); cur = nm
            } else if (l.startsWith("[")) {
                cur = descend(root, l.removeSurrounding("[", "]").split('.').map { it.trim().trim('"') })
            } else {
                val i = l.indexOf('='); if (i < 0) throw IllegalArgumentException("Expected key = value: '$l'")
                val keyParts = l.substring(0, i).trim().split('.').map { it.trim().trim('"') }
                descend(cur, keyParts.dropLast(1))[keyParts.last()] = value(l.substring(i + 1))
            }
        }
        return root
    }
    private fun tv(v: Any?): String = when (v) {
        null -> "\"\""
        is String -> Json.quote(v)
        is List<*> -> v.joinToString(", ", "[", "]") { tv(it) }
        is Map<*, *> -> v.entries.joinToString(", ", "{ ", " }") { "${it.key} = ${tv(it.value)}" }
        is Double -> if (v == Math.floor(v)) v.toLong().toString() else v.toString()
        else -> v.toString()
    }
    private fun isTA(v: Any?) = v is List<*> && v.isNotEmpty() && v.all { it is Map<*, *> }
    fun write(m: Map<*, *>, prefix: String = "", sb: StringBuilder = StringBuilder()): String {
        m.forEach { (k, v) -> if (v !is Map<*, *> && !isTA(v)) sb.append("$k = ${tv(v)}\n") }
        m.forEach { (k, v) ->
            if (v is Map<*, *>) { sb.append("\n[$prefix$k]\n"); write(v, "$prefix$k.", sb) }
            else if (isTA(v)) (v as List<*>).forEach { sb.append("\n[[$prefix$k]]\n"); write(it as Map<*, *>, "$prefix$k.", sb) }
        }
        return sb.toString().trim()
    }
}

object Xml {
    private val tok = Regex("<!--.*?-->|<!\\[CDATA\\[.*?]]>|<[^>]+>|[^<]+", RegexOption.DOT_MATCHES_ALL)
    private val voidTags = setOf("br", "hr", "img", "input", "meta", "link", "area", "base", "col", "embed", "source", "track", "wbr", "!doctype")
    fun format(s: String, ind: Int = 2, html: Boolean = false): String {
        val t = tok.findAll(s.trim()).map { it.value }.toList()
        var d = 0; val sb = StringBuilder(); var i = 0
        fun pad() = " ".repeat(d * ind)
        while (i < t.size) {
            val x = t[i]
            if (!x.startsWith("<")) { val tx = x.trim().replace(Regex("\\s+"), " "); if (tx.isNotEmpty()) sb.append(pad()).append(tx).append('\n') }
            else if (x.startsWith("</")) { d = maxOf(0, d - 1); sb.append(pad()).append(x).append('\n') }
            else if (x.startsWith("<?") || x.startsWith("<!") || x.endsWith("/>")) sb.append(pad()).append(x).append('\n')
            else {
                val name = x.removePrefix("<").takeWhile { !it.isWhitespace() && it != '>' }.lowercase()
                if (html && name in voidTags) sb.append(pad()).append(x).append('\n')
                else if (i + 2 < t.size && !t[i + 1].startsWith("<") && t[i + 2].startsWith("</") && t[i + 1].length < 120)
                { sb.append(pad()).append(x).append(t[i + 1].trim()).append(t[i + 2]).append('\n'); i += 2 }
                else { sb.append(pad()).append(x).append('\n'); d++ }
            }
            i++
        }
        return sb.toString().trimEnd()
    }
    fun minify(s: String) = s.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "").replace(Regex(">\\s+<"), "><").trim()
    fun parseDom(s: String): org.w3c.dom.Document {
        val f = DocumentBuilderFactory.newInstance()
        runCatching { f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        return f.newDocumentBuilder().parse(InputSource(StringReader(s)))
    }
}

object CodeFmt {
    private fun cmt(lang: String) = when (lang.lowercase()) {
        "python", "py", "ruby", "rb", "shell", "bash", "sh", "yaml", "r", "perl", "toml", "makefile" -> "#"
        "sql", "lua", "haskell" -> "--"
        else -> "//"
    }
    /** Walks code, returning for each char whether it is "live" code (not string/comment). */
    private fun live(code: String, lang: String): BooleanArray {
        val out = BooleanArray(code.length) { true }
        val lc = cmt(lang); val block = lc == "//" || lang.lowercase() == "css"
        var i = 0
        while (i < code.length) {
            val c = code[i]
            if (block && code.startsWith("/*", i)) { val e = code.indexOf("*/", i + 2).let { if (it < 0) code.length - 2 else it }; for (k in i..minOf(e + 1, code.length - 1)) out[k] = false; i = e + 2; continue }
            if (lc != "//" || !lang.equals("css", true)) if (code.startsWith(lc, i) && !(lc == "#" && code.startsWith("#[", i)) ) { val e = code.indexOf('\n', i).let { if (it < 0) code.length else it }; for (k in i until e) out[k] = false; i = e; continue }
            if (c == '"' || c == '\'' || c == '`') {
                var j = i + 1
                while (j < code.length && code[j] != c) { if (code[j] == '\\') j++; if (c != '`' && j < code.length && code[j] == '\n') break; j++ }
                for (k in i..minOf(j, code.length - 1)) out[k] = false; i = j + 1; continue
            }
            i++
        }
        return out
    }
    fun explode(code: String, lang: String): String {
        val lv = live(code, lang); val sb = StringBuilder(); var par = 0
        for (i in code.indices) {
            val c = code[i]
            if (!lv[i]) { sb.append(c); continue }
            when (c) {
                '(' -> { par++; sb.append(c) }
                ')' -> { par--; sb.append(c) }
                '{' -> { if (sb.isNotEmpty() && !sb.last().isWhitespace()) sb.append(' '); sb.append(c).append('\n') }
                '}' -> { sb.append('\n').append(c); val rest = code.substring(i + 1).trimStart()
                    if (Regex("^(else|catch|finally)\\b").containsMatchIn(rest)) sb.append(' ') else if (i + 1 < code.length && code[i + 1] !in ";,)\n") sb.append('\n') }
                ';' -> { sb.append(c); if (par <= 0) sb.append('\n') }
                else -> sb.append(c)
            }
        }
        return sb.toString().replace(Regex("\n[ \\t]*\n"), "\n")
    }
    fun beautify(code: String, lang: String, indent: Int = 4): String {
        val l = lang.lowercase().ifBlank { "js" }
        if (l in setOf("python", "py", "yaml", "makefile")) return lines(code).joinToString("\n") { it.replace("\t", "    ").trimEnd() }.trim()
        if (l == "sql") return Sql.format(code)
        if (l == "css" || l == "scss") return css(code)
        if (l == "html" || l == "xml") return Xml.format(code, 2, l == "html")
        val ls = lines(code)
        val src = if (ls.any { it.length > 100 } || (ls.size <= 2 && code.length > 40 && code.count { it == ';' || it == '{' } > 1)) explode(code, l) else code
        val lv = live(src, l); var level = 0; val out = ArrayList<String>(); var pos = 0
        for (line in lines(src)) {
            val start = pos; pos += line.length + 1
            val t = line.trim()
            if (t.isEmpty()) { if (out.isNotEmpty() && out.last().isNotEmpty()) out.add(""); continue }
            var lead = 0; var opens = 0; var closes = 0; var atStart = true
            for (k in line.indices) {
                if (!lv[minOf(start + k, lv.size - 1)] && !line[k].isWhitespace()) { atStart = false; continue }
                val c = line[k]
                if (c.isWhitespace()) continue
                if (c in "}])") { closes++; if (atStart) lead++ } else { atStart = false; if (c in "{[(") opens++ }
            }
            val cont = if (t.startsWith(".") || t.startsWith("?") || t.startsWith(":")) 1 else 0
            out.add(" ".repeat(maxOf(0, level - lead + cont) * indent) + t)
            level = maxOf(0, level + opens - closes)
        }
        return out.joinToString("\n").trim()
    }
    fun css(s: String): String {
        val src = s.replace(Regex("\\s+"), " ").replace(Regex("\\s*\\{\\s*"), " {\n").replace(Regex(";\\s*"), ";\n").replace(Regex("\\s*}\\s*"), "\n}\n")
        var lvl = 0; val out = ArrayList<String>()
        for (ln in src.lines()) { val t = ln.trim(); if (t.isEmpty()) continue; if (t.startsWith("}")) lvl = maxOf(0, lvl - 1); out.add("  ".repeat(lvl) + t.replace(Regex(":\\s*"), ": ").let { if (t.endsWith("{")) t else it }); if (t.endsWith("{")) lvl++ }
        return out.joinToString("\n").replace("}\n", "}\n")
    }
    fun stripComments(code: String, lang: String): String {
        val lv = live(code, lang); val sb = StringBuilder()
        for (i in code.indices) if (lv[i] || (code[i] == '\n')) sb.append(code[i]) else if (!lv[i] && isInStringLike(code, lv, i)) sb.append(code[i])
        return sb.toString()
    }
    // a non-live char that belongs to a string literal (as opposed to a comment) – strings start with a quote char
    private fun isInStringLike(code: String, lv: BooleanArray, i: Int): Boolean {
        var j = i
        while (j > 0 && !lv[j - 1]) j--
        return code[j] == '"' || code[j] == '\'' || code[j] == '`'
    }
    fun minify(code: String, lang: String): String {
        val l = lang.lowercase()
        if (l == "css") return code.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "").replace(Regex("\\s+"), " ").replace(Regex("\\s*([{}:;,>])\\s*"), "$1").replace(";}", "}").trim()
        if (l == "html" || l == "xml") return Xml.minify(code)
        if (l == "json") return Json.write(Json.parse(code), 0)
        val nc = stripComments(code, l)
        return lines(nc).map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    }
}

object Sql {
    private val kws = listOf("SELECT", "DISTINCT", "FROM", "WHERE", "GROUP BY", "ORDER BY", "HAVING", "LIMIT", "OFFSET", "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE TABLE", "ALTER TABLE", "DROP TABLE", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "CROSS JOIN", "JOIN", "ON", "AND", "OR", "NOT", "NULL", "IS", "IN", "AS", "UNION ALL", "UNION", "CASE", "WHEN", "THEN", "ELSE", "END", "LIKE", "BETWEEN", "EXISTS", "ASC", "DESC", "PRIMARY KEY", "FOREIGN KEY", "REFERENCES", "DEFAULT", "UNIQUE", "INDEX", "WITH", "COUNT", "SUM", "AVG", "MIN", "MAX")
    private val newline = listOf("FROM", "WHERE", "GROUP BY", "ORDER BY", "HAVING", "LIMIT", "OFFSET", "VALUES", "SET", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "CROSS JOIN", "JOIN", "UNION ALL", "UNION")
    private val seg = Regex("'(?:[^']|'')*'|\"[^\"]*\"|--[^\n]*|[^'\"]+")
    private fun map(s: String, f: (String) -> String) = seg.findAll(s).joinToString("") { val v = it.value; if (v.startsWith("'") || v.startsWith("\"") || v.startsWith("--")) v else f(v) }
    fun format(sql: String): String {
        var s = minify(sql)
        s = map(s) { p -> var r = p; kws.sortedByDescending { it.length }.forEach { k -> r = r.replace(Regex("\\b" + k.replace(" ", "\\s+") + "\\b", RegexOption.IGNORE_CASE), k) }; r }
        val nl = Regex("\\s*\\b(" + newline.sortedByDescending { it.length }.joinToString("|") { it.replace(" ", "\\s+") } + ")\\b")
        s = map(s) { p -> nl.replace(p) { "\n" + it.groupValues[1] }.replace(Regex("\\s+(AND|OR)\\b"), "\n  $1") }
        s = map(s) { it.replace(Regex(",\\s*"), ", ") }
        return s.replace(Regex("\\s*;\\s*"), ";\n\n").trim()
    }
    fun minify(sql: String): String = map(sql.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ")) { it.replace(Regex("\\s+"), " ") }.trim()
}

val formatTools: List<Tool> = listOf(
    tool("json", "JSON Formatter & Validator", Category.FORMAT, "Format, minify, validate, sort keys, tree view",
        m("Format (2)") { Json.write(Json.parse(it), 2) }, m("Format (4)") { Json.write(Json.parse(it), 4) },
        m("Minify") { Json.write(Json.parse(it), 0) }, m("Validate") { Json.parse(it); "✔ Valid JSON" },
        m("Sort keys") { Json.write(Json.parse(it), 2, true) }, m("Tree") { Json.paths(Json.parse(it)).joinToString("\n") },
        m("Escape") { Json.quote(it) }, m("Unescape") { Json.parse(it.trim()).toString() },
        hint = "JSON", sample = "{\"name\":\"Ada\",\"langs\":[\"Kotlin\",\"Rust\"],\"active\":true}", kw = "pretty beautify tree viewer"),
    tool("xml", "XML Formatter & Validator", Category.FORMAT, "Pretty print, minify and validate XML",
        m("Format") { Xml.format(it) }, m("Minify") { Xml.minify(it) }, m("Validate") { Xml.parseDom(it); "✔ Well-formed XML" },
        hint = "XML", sample = "<root><item id=\"1\">A</item><item id=\"2\">B</item></root>"),
    tool("yaml", "YAML Formatter & Validator", Category.FORMAT, "Normalize and validate YAML (maps, lists, scalars)",
        m("Format") { Yaml.write(Yaml.parse(it)) }, m("Validate") { Yaml.parse(it); "✔ Valid YAML" },
        hint = "YAML", sample = "name: app\nversion: 1\ntags:\n  - a\n  - b\nserver:\n  port: 8080"),
    tool("toml", "TOML Formatter & Validator", Category.FORMAT, "Format and validate TOML",
        m("Format") { Toml.write(Toml.parse(it)) }, m("Validate") { Toml.parse(it); "✔ Valid TOML" },
        hint = "TOML", sample = "[package]\nname = \"demo\"\nversion = \"0.1.0\"\n\n[dependencies]\nserde = \"1\""),
    tool("ini", "INI / .properties / .env Formatter", Category.FORMAT, "Tidy, sort and validate key=value config files",
        m("Tidy") { s -> lines(s).joinToString("\n") { l -> val t = l.trim(); val i = t.indexOf('='); if (t.startsWith("#") || t.startsWith("[") || i < 0) t else t.substring(0, i).trim() + "=" + t.substring(i + 1).trim() } },
        m("Sort keys") { s -> lines(s).map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.sorted().joinToString("\n") },
        m("Align =") { s -> val kvl = lines(s).map { it.trim() }.filter { it.contains('=') && !it.startsWith("#") }.map { it.substringBefore('=').trim() to it.substringAfter('=').trim() }; val w = kvl.maxOfOrNull { it.first.length } ?: 0; kvl.joinToString("\n") { it.first.padEnd(w) + " = " + it.second } },
        m("Validate") { s -> val bad = lines(s).withIndex().filter { (_, l) -> val t = l.trim(); t.isNotEmpty() && !t.startsWith("#") && !t.startsWith(";") && !t.startsWith("[") && !t.contains('=') }; if (bad.isEmpty()) "✔ Looks valid" else bad.joinToString("\n") { "Line ${it.index + 1}: missing '=' → ${it.value.trim()}" } },
        hint = ".env / .ini / .properties", sample = "# demo\nAPI_URL = https://api.example.com\nDEBUG=true\nPORT =8080"),
    tool("html", "HTML Beautifier / Minifier", Category.FORMAT, "Indent or minify HTML",
        m("Beautify") { Xml.format(it, 2, true) }, m("Minify") { Xml.minify(it) },
        hint = "HTML", sample = "<div><p>Hello <b>world</b></p><ul><li>One</li><li>Two</li></ul></div>"),
    tool("css", "CSS Beautifier / Minifier", Category.FORMAT, "Format or compress CSS",
        m("Beautify") { CodeFmt.css(it) }, m("Minify") { CodeFmt.minify(it, "css") },
        hint = "CSS", sample = "body{margin:0;padding:0}.card{color:#fff;background:#0b3d91}"),
    tool("sql-format", "SQL Formatter", Category.FORMAT, "Uppercase keywords and break clauses (MySQL, PostgreSQL, SQLite, T-SQL)",
        m("Format") { Sql.format(it) }, m("Minify") { Sql.minify(it) },
        hint = "SQL", sample = "select u.id,u.name,count(o.id) from users u left join orders o on o.user_id=u.id where u.active=1 and o.total>10 group by u.id order by 3 desc limit 10"),
    tool("code-beautify", "Code Beautifier (any language)", Category.FORMAT,
        "Re-indent and tidy JavaScript, TypeScript, Java, Kotlin, C, C++, C#, Go, Rust, PHP, Swift, Dart, GraphQL, Protobuf, Python and more",
        mp("Beautify") { s, p -> CodeFmt.beautify(s, p.getOrElse(0) { "" }) },
        mp("Minify (safe)") { s, p -> CodeFmt.minify(s, p.getOrElse(0) { "" }.ifBlank { "js" }) },
        m("Tabs → spaces") { s -> lines(s).joinToString("\n") { it.replace("\t", "    ") } },
        hint = "Code", params = listOf("Language (js, ts, java, kotlin, c, cpp, cs, go, rust, php, swift, dart, graphql, proto, python, css, html, sql)"),
        sample = "function add(a,b){if(a>b){return a+b;}else{return b-a;}}", kw = "prettier indent reformat javascript java python"),
)
