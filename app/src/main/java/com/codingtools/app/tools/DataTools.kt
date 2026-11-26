package com.codingtools.app.tools

import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

object Query {
    private fun children(v: Any?): List<Any?> = when (v) { is Map<*, *> -> v.values.toList(); is List<*> -> v; else -> emptyList() }
    private fun descend(v: Any?, out: MutableList<Any?>) { out.add(v); children(v).forEach { descend(it, out) } }
    private fun cmp(a: Any?, op: String, b: Any?): Boolean {
        val x = (a as? Number)?.toDouble(); val y = (b as? Number)?.toDouble()
        if (x != null && y != null) return when (op) { "==" -> x == y; "!=" -> x != y; ">" -> x > y; "<" -> x < y; ">=" -> x >= y; else -> x <= y }
        val s = a?.toString(); val t = b?.toString()
        return when (op) { "==" -> s == t; "!=" -> s != t; else -> if (s == null || t == null) false else { val c = s.compareTo(t); when (op) { ">" -> c > 0; "<" -> c < 0; ">=" -> c >= 0; else -> c <= 0 } } }
    }
    private fun get(v: Any?, path: String): Any? { var cur = v; for (k in path.split('.').filter { it.isNotEmpty() }) cur = (cur as? Map<*, *>)?.get(k); return cur }
    fun jsonPath(root: Any?, path: String): List<Any?> {
        val p = path.trim(); var cur: List<Any?> = listOf(root); var i = if (p.startsWith("$")) 1 else 0
        fun name(): String { val st = i; if (i < p.length && p[i] == '*') { i++; return "*" }; while (i < p.length && (p[i].isLetterOrDigit() || p[i] in "_-")) i++; return p.substring(st, i) }
        while (i < p.length) {
            when {
                p.startsWith("..", i) -> { i += 2; val n = name(); val all = ArrayList<Any?>(); cur.forEach { descend(it, all) }; cur = if (n == "*") all.flatMap { children(it) } else all.filter { it is Map<*, *> && it.containsKey(n) }.map { (it as Map<*, *>)[n] } }
                p[i] == '.' -> { i++; val n = name(); cur = if (n == "*") cur.flatMap { children(it) } else cur.filter { it is Map<*, *> && it.containsKey(n) }.map { (it as Map<*, *>)[n] } }
                p[i] == '[' -> {
                    val e = p.indexOf(']', i).let { if (it < 0) throw IllegalArgumentException("Missing ]") else it }
                    val inner = p.substring(i + 1, e).trim(); i = e + 1
                    cur = when {
                        inner == "*" -> cur.flatMap { children(it) }
                        inner.startsWith("'") || inner.startsWith("\"") -> inner.trim('\'', '"').let { k -> cur.filter { it is Map<*, *> && it.containsKey(k) }.map { (it as Map<*, *>)[k] } }
                        inner.startsWith("?") -> { val m = Regex("\\?\\(@\\.?([\\w.]*)\\s*(==|!=|>=|<=|>|<)\\s*(.+)\\)").matchEntire(inner) ?: throw IllegalArgumentException("Unsupported filter: $inner"); val rhs = Yaml.scalar(m.groupValues[3].trim())
                            cur.flatMap { children(it) }.filter { cmp(get(it, m.groupValues[1]), m.groupValues[2], rhs) } }
                        inner.contains(':') -> { val (a, b) = inner.split(':').let { (it[0].toIntOrNull() ?: 0) to it.getOrNull(1)?.toIntOrNull() }; cur.flatMap { (it as? List<*>)?.let { l -> l.subList(a.coerceIn(0, l.size), (b ?: l.size).coerceIn(0, l.size)) } ?: emptyList() } }
                        else -> { val n = inner.toInt(); cur.mapNotNull { (it as? List<*>)?.let { l -> l.getOrNull(if (n < 0) l.size + n else n) } } }
                    }
                }
                else -> throw IllegalArgumentException("Unexpected '${p[i]}' at $i")
            }
        }
        return cur
    }
    private fun path(v: Any?, e0: String): List<Any?> {
        var cur = listOf(v); val e = e0.trim(); var i = 0
        if (e == ".") return cur
        while (i < e.length) {
            if (e[i] == '.') { i++; if (i < e.length && e[i] == '"') { val j = e.indexOf('"', i + 1); val k = e.substring(i + 1, j); i = j + 1; cur = cur.map { (it as? Map<*, *>)?.get(k) }; continue }
                val st = i; while (i < e.length && (e[i].isLetterOrDigit() || e[i] in "_-")) i++; val k = e.substring(st, i); if (k.isNotEmpty()) cur = cur.map { (it as? Map<*, *>)?.get(k) } }
            else if (e[i] == '[') { val j = e.indexOf(']', i); val inner = e.substring(i + 1, j).trim(); i = j + 1
                cur = if (inner.isEmpty()) cur.flatMap { children(it) } else cur.map { (it as? List<*>)?.getOrNull(inner.toInt().let { n -> if (n < 0) it.size + n else n }) } }
            else throw IllegalArgumentException("Bad expression near '${e.substring(i)}'")
        }
        return cur
    }
    private fun splitPipe(s: String): List<String> { val out = ArrayList<String>(); var d = 0; var q = false; val cur = StringBuilder()
        for (c in s) { if (c == '"') q = !q; if (!q) { if (c == '(' || c == '[') d++; if (c == ')' || c == ']') d-- }; if (c == '|' && d == 0 && !q) { out.add(cur.toString().trim()); cur.clear() } else cur.append(c) }
        out.add(cur.toString().trim()); return out }
    fun jq(v: Any?, expr: String): List<Any?> {
        var stream = listOf(v)
        for (st in splitPipe(expr)) {
            stream = when {
                st.startsWith(".") -> stream.flatMap { path(it, st) }
                st == "keys" -> stream.map { (it as? Map<*, *>)?.keys?.toList()?.sortedBy { k -> k.toString() } ?: (it as? List<*>)?.indices?.toList() }
                st == "length" -> stream.map { when (it) { is Map<*, *> -> it.size.toLong(); is List<*> -> it.size.toLong(); is String -> it.length.toLong(); null -> 0L; else -> it } }
                st == "values" -> stream.map { (it as? Map<*, *>)?.values?.toList() }
                st == "first" -> stream.map { (it as? List<*>)?.firstOrNull() }; st == "last" -> stream.map { (it as? List<*>)?.lastOrNull() }
                st == "reverse" -> stream.map { (it as? List<*>)?.reversed() }; st == "unique" -> stream.map { (it as? List<*>)?.distinct() }
                st == "sort" -> stream.map { (it as? List<*>)?.sortedBy { x -> x.toString().padStart(20) } }
                st == "add" -> stream.map { l -> (l as? List<*>)?.let { x -> if (x.all { n -> n is Number }) x.sumOf { n -> (n as Number).toDouble() } else x.joinToString("") } }
                st.startsWith("map(") -> { val inner = st.removePrefix("map(").removeSuffix(")"); stream.map { l -> (l as? List<*>)?.flatMap { x -> jq(x, inner) } } }
                st.startsWith("select(") -> { val m = Regex("select\\(\\s*(\\.[\\w.\\[\\]]*)\\s*(==|!=|>=|<=|>|<)\\s*(.+)\\)").matchEntire(st) ?: throw IllegalArgumentException("Unsupported select: $st"); val rhs = Yaml.scalar(m.groupValues[3].trim())
                    stream.filter { cmp(path(it, m.groupValues[1]).firstOrNull(), m.groupValues[2], rhs) } }
                else -> throw IllegalArgumentException("Unsupported filter '$st'. Supported: .a.b[0], .[], keys, length, values, first, last, sort, unique, reverse, add, map(...), select(.k == v)")
            }
        }
        return stream
    }
    fun show(l: List<Any?>) = if (l.isEmpty()) "(no results)" else l.joinToString("\n") { Json.write(it) }
    fun validate(v: Any?, s: Map<*, *>, path: String, err: MutableList<String>) {
        fun typeOk(t: String) = when (t) { "string" -> v is String; "integer" -> v is Long || (v is Double && v == Math.floor(v)); "number" -> v is Number; "boolean" -> v is Boolean; "array" -> v is List<*>; "object" -> v is Map<*, *>; "null" -> v == null; else -> true }
        (s["type"])?.let { t -> val ts = if (t is List<*>) t.map { it.toString() } else listOf(t.toString()); if (ts.none { typeOk(it) }) { err.add("$path: expected ${ts.joinToString("|")}"); return } }
        (s["enum"] as? List<*>)?.let { if (it.none { x -> x == v || x?.toString() == v?.toString() }) err.add("$path: not in enum $it") }
        if (s.containsKey("const") && s["const"].toString() != v.toString()) err.add("$path: must equal ${s["const"]}")
        if (v is Number) { (s["minimum"] as? Number)?.let { if (v.toDouble() < it.toDouble()) err.add("$path: < minimum $it") }; (s["maximum"] as? Number)?.let { if (v.toDouble() > it.toDouble()) err.add("$path: > maximum $it") } }
        if (v is String) { (s["minLength"] as? Number)?.let { if (v.length < it.toInt()) err.add("$path: shorter than $it") }; (s["maxLength"] as? Number)?.let { if (v.length > it.toInt()) err.add("$path: longer than $it") }; (s["pattern"] as? String)?.let { if (!Regex(it).containsMatchIn(v)) err.add("$path: does not match /$it/") }
            if (s["format"] == "email" && !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(v)) err.add("$path: invalid email") }
        if (v is Map<*, *>) { (s["required"] as? List<*>)?.forEach { if (!v.containsKey(it.toString())) err.add("$path: missing required '$it'") }
            val props = s["properties"] as? Map<*, *>; props?.forEach { (k, sub) -> if (v.containsKey(k.toString())) validate(v[k.toString()], sub as Map<*, *>, "$path.$k", err) }
            if (s["additionalProperties"] == false) v.keys.filter { props?.containsKey(it) != true }.forEach { err.add("$path: unexpected property '$it'") } }
        if (v is List<*>) { (s["minItems"] as? Number)?.let { if (v.size < it.toInt()) err.add("$path: fewer than $it items") }; (s["maxItems"] as? Number)?.let { if (v.size > it.toInt()) err.add("$path: more than $it items") }
            (s["items"] as? Map<*, *>)?.let { sub -> v.forEachIndexed { i, x -> validate(x, sub, "$path[$i]", err) } } }
        (s["allOf"] as? List<*>)?.forEach { validate(v, it as Map<*, *>, path, err) }
        (s["anyOf"] as? List<*>)?.let { l -> if (l.none { sub -> mutableListOf<String>().also { e -> validate(v, sub as Map<*, *>, path, e) }.isEmpty() }) err.add("$path: matches none of anyOf") }
        (s["oneOf"] as? List<*>)?.let { l -> val n = l.count { sub -> mutableListOf<String>().also { e -> validate(v, sub as Map<*, *>, path, e) }.isEmpty() }; if (n != 1) err.add("$path: matches $n of oneOf (needs exactly 1)") }
    }
    fun xpath(xml: String, expr: String): String {
        val doc = Xml.parseDom(xml); val xp = XPathFactory.newInstance().newXPath()
        val nodes = xp.evaluate(expr, doc, XPathConstants.NODESET) as org.w3c.dom.NodeList
        return if (nodes.length == 0) runCatching { xp.evaluate(expr, doc) }.getOrDefault("(no results)").ifEmpty { "(no results)" } else (0 until nodes.length).joinToString("\n") { val n = nodes.item(it); "<${n.nodeName}> ${n.textContent.trim()}" }
    }
    fun xsd(xml: String, xsd: String): String {
        val schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(StreamSource(StringReader(xsd)))
        return try { schema.newValidator().validate(StreamSource(StringReader(xml))); "✔ Valid against schema" } catch (e: org.xml.sax.SAXException) { "✘ ${e.message}" }
    }
}

object SqlTools {
    class Col(val name: String, val type: String, val pk: Boolean, val notNull: Boolean, val ref: String?)
    class Tbl(val name: String, val cols: List<Col>)
    private fun splitTop(s: String): List<String> { val out = ArrayList<String>(); var d = 0; val cur = StringBuilder(); for (c in s) { if (c == '(') d++; if (c == ')') d--; if (c == ',' && d == 0) { out.add(cur.toString().trim()); cur.clear() } else cur.append(c) }; if (cur.isNotBlank()) out.add(cur.toString().trim()); return out }
    fun parse(sql: String): List<Tbl> = Regex("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?[`\"\\[]?(\\w+)[`\"\\]]?\\s*\\((.*?)\\)\\s*(?:;|$)", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).findAll(sql).map { m ->
        val items = splitTop(m.groupValues[2]); val pkCols = items.filter { it.uppercase().startsWith("PRIMARY KEY") }.flatMap { Regex("\\((.*?)\\)").find(it)?.groupValues?.get(1)?.split(',')?.map { c -> c.trim().trim('`', '"') } ?: emptyList() }
        val fks = items.filter { it.uppercase().startsWith("FOREIGN KEY") }.associate { Regex("\\((.*?)\\)\\s*REFERENCES\\s+(\\w+)", RegexOption.IGNORE_CASE).find(it)?.let { r -> r.groupValues[1].trim() to r.groupValues[2] } ?: ("" to "") }
        Tbl(m.groupValues[1], items.filter { val u = it.uppercase(); !(u.startsWith("PRIMARY") || u.startsWith("FOREIGN") || u.startsWith("UNIQUE") || u.startsWith("CONSTRAINT") || u.startsWith("KEY") || u.startsWith("INDEX") || u.startsWith("CHECK")) }.map { c ->
            val p = c.trim().split(Regex("\\s+")); val n = p[0].trim('`', '"', '[', ']'); val u = c.uppercase()
            Col(n, p.getOrElse(1) { "text" }, u.contains("PRIMARY KEY") || n in pkCols, u.contains("NOT NULL") || u.contains("PRIMARY KEY"), Regex("REFERENCES\\s+(\\w+)", RegexOption.IGNORE_CASE).find(c)?.groupValues?.get(1) ?: fks[n]) })
    }.toList()
    private fun kind(t: String) = t.lowercase().let { when { Regex("int|serial").containsMatchIn(it) && !it.contains("point") -> "int"; Regex("char|text|clob|uuid|json").containsMatchIn(it) -> "str"; Regex("bool|bit").containsMatchIn(it) -> "bool"; Regex("float|double|real|decimal|numeric|money").containsMatchIn(it) -> "float"; Regex("date|time").containsMatchIn(it) -> "date"; else -> "str" } }
    fun orm(sql: String, target: String): String { val t = parse(sql); need(t.isNotEmpty(), "No CREATE TABLE statement found")
        return t.joinToString("\n\n") { tb -> val cn = pascal(tb.name).let { if (it.endsWith("s") && it.length > 3) it.dropLast(1) else it }
            when (target) {
                "room" -> "@Entity(tableName = \"${tb.name}\")\ndata class $cn(\n" + tb.cols.joinToString(",\n") { c -> "    " + (if (c.pk) "@PrimaryKey " else "") + (if (c.name != camel(c.name)) "@ColumnInfo(name = \"${c.name}\") " else "") + "val ${camel(c.name)}: " + when (kind(c.type)) { "int" -> "Long"; "bool" -> "Boolean"; "float" -> "Double"; else -> "String" } + (if (c.notNull) "" else "?") } + "\n)"
                "jpa" -> "@Entity\n@Table(name = \"${tb.name}\")\npublic class $cn {\n" + tb.cols.joinToString("\n\n") { c -> (if (c.pk) "    @Id\n" else "") + "    @Column(name = \"${c.name}\"${if (c.notNull && !c.pk) ", nullable = false" else ""})\n    private " + when (kind(c.type)) { "int" -> "Long"; "bool" -> "Boolean"; "float" -> "Double"; "date" -> "java.time.Instant"; else -> "String" } + " ${camel(c.name)};" } + "\n}"
                "django" -> "class $cn(models.Model):\n" + tb.cols.filter { !(it.pk && kind(it.type) == "int") }.joinToString("\n") { c -> "    ${snake(c.name)} = models." + when (kind(c.type)) { "int" -> "IntegerField"; "bool" -> "BooleanField"; "float" -> "FloatField"; "date" -> "DateTimeField"; else -> if (c.type.lowercase().contains("text")) "TextField" else "CharField" }.let { f -> if (f == "CharField") "$f(max_length=255${if (c.notNull) "" else ", null=True"})" else "$f(${if (c.notNull) "" else "null=True"})" } } + "\n\n    class Meta:\n        db_table = \"${tb.name}\""
                "sqlalchemy" -> "class $cn(Base):\n    __tablename__ = \"${tb.name}\"\n\n" + tb.cols.joinToString("\n") { c -> "    ${snake(c.name)} = Column(" + when (kind(c.type)) { "int" -> "Integer"; "bool" -> "Boolean"; "float" -> "Float"; "date" -> "DateTime"; else -> "String" } + (if (c.pk) ", primary_key=True" else "") + (if (!c.notNull) ", nullable=True" else "") + ")" }
                else -> "model $cn {\n" + tb.cols.joinToString("\n") { c -> "  ${camel(c.name)} ".padEnd(16) + when (kind(c.type)) { "int" -> "Int"; "bool" -> "Boolean"; "float" -> "Float"; "date" -> "DateTime"; else -> "String" } + (if (c.notNull) "" else "?") + (if (c.pk) " @id" else "") } + "\n  @@map(\"${tb.name}\")\n}"
            } }
    }
    fun mermaid(sql: String): String { val t = parse(sql); need(t.isNotEmpty(), "No CREATE TABLE found")
        return "erDiagram\n" + t.joinToString("\n") { tb -> tb.cols.mapNotNull { c -> c.ref?.let { "    ${it.uppercase()} ||--o{ ${tb.name.uppercase()} : \"${c.name}\"" } }.joinToString("\n") }.trim() + "\n\n" + t.joinToString("\n") { tb -> "    ${tb.name.uppercase()} {\n" + tb.cols.joinToString("\n") { c -> "        ${c.type.replace(Regex("\\(.*\\)"), "")} ${c.name}${if (c.pk) " PK" else if (c.ref != null) " FK" else ""}" } + "\n    }" } }
    fun csvInsert(csv: String, table: String): String { val rows = csvParse(csv, detectDelim(csv)); need(rows.size > 1, "Need a header and at least one row"); val h = rows[0]
        val num = h.indices.map { c -> rows.drop(1).all { r -> r.getOrNull(c)?.let { v -> v.isEmpty() || v.toDoubleOrNull() != null } ?: true } }
        return rows.drop(1).joinToString("\n") { r -> "INSERT INTO $table (${h.joinToString(", ") { snake(it) }}) VALUES (${h.indices.joinToString(", ") { c -> val v = r.getOrElse(c) { "" }; if (v.isEmpty()) "NULL" else if (num[c]) v else "'" + v.replace("'", "''") + "'" }});" } }
    fun indexHints(sql: String): String { val tables = Regex("(?:FROM|JOIN)\\s+(\\w+)(?:\\s+(?:AS\\s+)?(\\w+))?", RegexOption.IGNORE_CASE).findAll(sql).map { it.groupValues[1] to it.groupValues[2] }.toList(); need(tables.isNotEmpty(), "No table found")
        val cols = Regex("(?:WHERE|AND|OR|ON)\\s+(?:(\\w+)\\.)?(\\w+)\\s*(?:=|>|<|>=|<=|LIKE|IN|BETWEEN)", RegexOption.IGNORE_CASE).findAll(sql).map { it.groupValues[1] to it.groupValues[2] }.toList()
        val order = Regex("ORDER\\s+BY\\s+(?:(\\w+)\\.)?(\\w+)", RegexOption.IGNORE_CASE).findAll(sql).map { it.groupValues[1] to it.groupValues[2] }.toList()
        fun tableOf(alias: String) = if (alias.isEmpty()) tables.first().first else tables.firstOrNull { it.second == alias || it.first == alias }?.first ?: tables.first().first
        val out = (cols + order).groupBy({ tableOf(it.first) }, { it.second }).map { (t, cs) -> "CREATE INDEX idx_${t}_${cs.distinct().joinToString("_")} ON $t (${cs.distinct().joinToString(", ")});" }
        return (if (out.isEmpty()) "No obvious filter / join columns found." else "Suggested indexes (heuristic – verify with EXPLAIN):\n" + out.joinToString("\n")) + "\n\nTips: put equality columns first, range columns last; avoid functions on indexed columns; SELECT only needed columns."
    }
    fun mustache(tpl: String, data: Any?): String {
        fun look(ctx: List<Any?>, key: String): Any? { if (key == "." || key == "this") return ctx.last(); for (c in ctx.reversed()) { var cur: Any? = c; var ok = true; for (k in key.split('.')) { if (cur is Map<*, *> && cur.containsKey(k)) cur = cur[k] else { ok = false; break } }; if (ok) return cur }; return null }
        fun render(t: String, ctx: List<Any?>): String {
            var s = Regex("\\{%\\s*for\\s+(\\w+)\\s+in\\s+([\\w.]+)\\s*%}(.*?)\\{%\\s*endfor\\s*%}", RegexOption.DOT_MATCHES_ALL).replace(t) { m -> val l = look(ctx, m.groupValues[2]) as? List<*> ?: emptyList<Any?>(); l.joinToString("") { x -> render(m.groupValues[3].replace(Regex("\\{\\{\\s*" + m.groupValues[1] + "(\\.[\\w.]+)?\\s*}}")) { r -> "{{this${r.groupValues[1]}}}" }, ctx + x) } }
            s = Regex("\\{\\{#(\\w[\\w.]*)}}(.*?)\\{\\{/\\1}}", RegexOption.DOT_MATCHES_ALL).replace(s) { m -> when (val v = look(ctx, m.groupValues[1])) { is List<*> -> v.joinToString("") { render(m.groupValues[2], ctx + it) }; null, false -> ""; else -> render(m.groupValues[2], ctx + v) } }
            s = Regex("\\{\\{\\^(\\w[\\w.]*)}}(.*?)\\{\\{/\\1}}", RegexOption.DOT_MATCHES_ALL).replace(s) { m -> val v = look(ctx, m.groupValues[1]); if (v == null || v == false || (v as? List<*>)?.isEmpty() == true) render(m.groupValues[2], ctx) else "" }
            return Regex("\\{\\{\\{?\\s*([\\w.]+)\\s*}?}}").replace(s) { m -> look(ctx, m.groupValues[1])?.toString() ?: "" }
        }
        return render(tpl, listOf(data))
    }
    fun table(csv: String): String { val rows = csvParse(csv, detectDelim(csv)); return "${rows.size - 1} rows × ${rows.firstOrNull()?.size ?: 0} columns\n\n" + com.codingtools.app.tools.table(rows) }
}

val dataTools: List<Tool> = listOf(
    tool("jsonpath", "JSONPath Tester", Category.DATA, "\$.store.book[?(@.price < 10)].title, \$..author, [*], slices",
        mp("Evaluate") { s, p -> Query.show(Query.jsonPath(parseAny(s), p.getOrElse(0) { "$" }.ifBlank { "$" })) }, hint = "JSON / YAML", params = listOf("JSONPath, e.g. \$.users[*].name"),
        sample = "{\"users\":[{\"name\":\"Ada\",\"age\":36},{\"name\":\"Linus\",\"age\":55}]}", kw = "jmespath query", psample = listOf("\$.users[*].name")),
    tool("jq", "jq-style Filter", Category.DATA, "Subset of jq: .a.b[0], .[], keys, length, map(), select(), sort, unique, add",
        mp("Run filter") { s, p -> Query.show(Query.jq(parseAny(s), p.getOrElse(0) { "." }.ifBlank { "." })) }, hint = "JSON / YAML", params = listOf("Filter, e.g. .users | map(.name)"),
        sample = "{\"users\":[{\"name\":\"Ada\",\"age\":36},{\"name\":\"Linus\",\"age\":55}]}", psample = listOf(".users | map(.name)")),
    tool("xpath", "XPath Tester", Category.DATA, "Query XML documents", mp("Evaluate") { s, p -> Query.xpath(s, p.getOrElse(0) { "/" }) }, hint = "XML", params = listOf("XPath, e.g. //item[@id='2']"), sample = "<root><item id=\"1\">A</item><item id=\"2\">B</item></root>", psample = listOf("//item[@id='2']")),
    tool("json-validate", "JSON Schema Validator", Category.DATA, "Validate JSON against a schema (type, required, properties, items, enum, min/max, pattern, anyOf/oneOf/allOf)",
        mp("Validate") { s, p -> val e = ArrayList<String>(); Query.validate(parseAny(s), Json.parse(p.getOrElse(0) { "{}" }) as Map<*, *>, "$", e); if (e.isEmpty()) "✔ Valid" else "✘ ${e.size} error(s)\n" + e.joinToString("\n") },
        hint = "JSON document", params = listOf("JSON Schema"), sample = "{\"name\":\"Ada\",\"age\":-1}", psample = listOf("{\"type\":\"object\",\"required\":[\"name\",\"age\"],\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\",\"minimum\":0}}}")),
    tool("xsd", "XML Schema (XSD) Validator", Category.DATA, "Validate XML against an XSD", mp("Validate") { s, p -> Query.xsd(s, p.getOrElse(0) { "" }) }, hint = "XML", params = listOf("XSD")),
    tool("sql-orm", "SQL → ORM Models", Category.DATA, "CREATE TABLE → Room, JPA, Django, SQLAlchemy, Prisma",
        m("Room (Kotlin)") { SqlTools.orm(it, "room") }, m("JPA (Java)") { SqlTools.orm(it, "jpa") }, m("Django") { SqlTools.orm(it, "django") }, m("SQLAlchemy") { SqlTools.orm(it, "sqlalchemy") }, m("Prisma") { SqlTools.orm(it, "prisma") },
        hint = "CREATE TABLE …", sample = "CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR(100) NOT NULL, email TEXT, is_active BOOLEAN, created_at TIMESTAMP);"),
    tool("sql-er", "SQL → ER Diagram (Mermaid)", Category.DATA, "Entity-relationship source from CREATE TABLE statements", m("Mermaid ER") { SqlTools.mermaid(it) }, hint = "CREATE TABLE …",
        sample = "CREATE TABLE users (id INT PRIMARY KEY, name TEXT);\nCREATE TABLE posts (id INT PRIMARY KEY, user_id INT REFERENCES users(id), title TEXT);"),
    tool("csv-sql", "CSV / Excel → SQL INSERT", Category.DATA, "Paste rows copied from Excel or CSV", mp("Generate INSERTs") { s, p -> SqlTools.csvInsert(s, p.getOrElse(0) { "" }.ifBlank { "my_table" }) }, hint = "CSV with header", params = listOf("Table name"), sample = "id,name,score\n1,Ada,9.5\n2,Linus,8", psample = listOf("scores")),
    tool("csv-view", "CSV / TSV Viewer", Category.DATA, "Aligned table view with delimiter auto-detect", m("View") { SqlTools.table(it) }, hint = "CSV / TSV", sample = "id,name,score\n1,Ada,9.5\n2,Linus,8"),
    tool("sql-index", "SQL Index Hints", Category.DATA, "Heuristic index suggestions for a query", m("Suggest") { SqlTools.indexHints(it) }, hint = "SELECT …", sample = "SELECT * FROM orders o JOIN users u ON u.id = o.user_id WHERE o.status = 'paid' AND o.total > 10 ORDER BY o.created_at"),
    tool("template", "Template Tester (Mustache / Jinja-style)", Category.DATA, "{{var}}, {{#list}}…{{/list}}, {% for x in xs %}…{% endfor %}",
        mp("Render") { s, p -> SqlTools.mustache(s, parseAny(p.getOrElse(0) { "{}" })) }, hint = "Template", params = listOf("Data (JSON)"), sample = "Hello {{name}}!\n{% for t in tags %}- {{t}}\n{% endfor %}", psample = listOf("{\"name\":\"Ada\",\"tags\":[\"kotlin\",\"rust\"]}")),
    tool("ndjson", "JSON Lines (NDJSON) Viewer", Category.DATA, "Validate and pretty-print each line", m("View") { s -> lines(s).filter { it.isNotBlank() }.mapIndexed { i, l -> "#${i + 1}  " + runCatching { Json.write(Json.parse(l), 0) }.getOrElse { "✘ ${it.message}" } }.joinToString("\n") }, hint = "NDJSON", sample = "{\"a\":1}\n{\"a\":2}"),
) + sqliteTools
