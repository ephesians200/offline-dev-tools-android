package com.codingtools.app.tools

import java.util.regex.Pattern

object Txt {
    fun diff(a: List<String>, b: List<String>): String {
        need(a.size.toLong() * b.size <= 6_000_000L, "Inputs too large for on-device diff")
        val n = a.size; val m = b.size; val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) dp[i][j] = if (a[i] == b[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
        var i = 0; var j = 0; val out = ArrayList<String>(); var add = 0; var del = 0
        while (i < n && j < m) when {
            a[i] == b[j] -> { out.add("  " + a[i]); i++; j++ }
            dp[i + 1][j] >= dp[i][j + 1] -> { out.add("- " + a[i]); i++; del++ }
            else -> { out.add("+ " + b[j]); j++; add++ }
        }
        while (i < n) { out.add("- " + a[i++]); del++ }; while (j < m) { out.add("+ " + b[j++]); add++ }
        return "+$add added, -$del removed\n\n" + out.joinToString("\n")
    }
    fun jsonDiff(a: String, b: String): String {
        val pa = Json.paths(Json.parse(a)).associate { it.substringBefore(" = ") to it.substringAfter(" = ") }
        val pb = Json.paths(Json.parse(b)).associate { it.substringBefore(" = ") to it.substringAfter(" = ") }
        val out = ArrayList<String>()
        (pa.keys + pb.keys).toSortedSet().forEach { k -> val x = pa[k]; val y = pb[k]; when { x == null -> out.add("+ $k = $y"); y == null -> out.add("- $k = $x"); x != y -> out.add("~ $k: $x → $y") } }
        return if (out.isEmpty()) "✔ Identical" else out.joinToString("\n")
    }
    fun flags(s: String): Int { var f = 0; if ('i' in s) f = f or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE; if ('m' in s) f = f or Pattern.MULTILINE; if ('s' in s) f = f or Pattern.DOTALL; if ('x' in s) f = f or Pattern.COMMENTS; return f }
    fun matches(text: String, pat: String, fl: String): String {
        need(pat.isNotEmpty(), "Enter a pattern in the first parameter field")
        val mt = Pattern.compile(pat, flags(fl)).matcher(guarded(text)); val sb = StringBuilder(); var n = 0
        while (mt.find()) {
            n++; sb.append("#$n [${mt.start()}–${mt.end()}] ${Json.quote(mt.group())}\n")
            for (g in 1..mt.groupCount()) sb.append("     group $g: ${mt.group(g)?.let { Json.quote(it) } ?: "∅"}\n")
            if (n >= 500) { sb.append("… (truncated)\n"); break }
        }
        return if (n == 0) "No matches" else "$n match(es)\n\n$sb"
    }
    fun explain(p: String): String {
        val out = ArrayList<String>(); var i = 0
        val esc = mapOf('d' to "digit [0-9]", 'D' to "non-digit", 'w' to "word char [A-Za-z0-9_]", 'W' to "non-word char", 's' to "whitespace", 'S' to "non-whitespace", 'b' to "word boundary", 'B' to "non-boundary", 'n' to "newline", 't' to "tab", 'r' to "carriage return")
        while (i < p.length) {
            val c = p[i]; var tok = c.toString(); var d: String
            when {
                c == '\\' && i + 1 < p.length -> { tok = p.substring(i, i + 2); d = esc[p[i + 1]] ?: "literal '${p[i + 1]}'"; i++ }
                c == '[' -> { val e = p.indexOf(']', i + 2).let { if (it < 0) p.length - 1 else it }; tok = p.substring(i, e + 1); d = (if (tok.startsWith("[^")) "any char NOT in " else "any char in ") + tok; i = e }
                c == '(' -> { tok = when { p.startsWith("(?:", i) -> "(?:"; p.startsWith("(?=", i) -> "(?="; p.startsWith("(?!", i) -> "(?!"; p.startsWith("(?<=", i) -> "(?<="; p.startsWith("(?<!", i) -> "(?<!"; p.startsWith("(?<", i) -> p.substring(i, p.indexOf('>', i) + 1); else -> "(" }
                    d = when { tok == "(?:" -> "non-capturing group"; tok == "(?=" -> "positive lookahead"; tok == "(?!" -> "negative lookahead"; tok == "(?<=" -> "positive lookbehind"; tok == "(?<!" -> "negative lookbehind"; tok.startsWith("(?<") -> "named group '${tok.drop(3).dropLast(1)}'"; else -> "capturing group start" }; i += tok.length - 1 }
                c == ')' -> d = "group end"
                c == '.' -> d = "any character (except newline)"
                c == '^' -> d = "start of line/input"; c == '$' -> d = "end of line/input"
                c == '|' -> d = "OR"
                c == '*' -> d = "zero or more"; c == '+' -> d = "one or more"; c == '?' -> d = "optional (or lazy modifier after a quantifier)"
                c == '{' -> { val e = p.indexOf('}', i).let { if (it < 0) p.length - 1 else it }; tok = p.substring(i, e + 1); d = "repeat $tok"; i = e }
                else -> d = "literal '$c'"
            }
            out.add(tok.padEnd(12) + "→ $d"); i++
        }
        return out.joinToString("\n")
    }
    fun regexFrom(examples: String): String {
        fun gen(s: String): String { val sb = StringBuilder(); var i = 0
            while (i < s.length) { val c = s[i]; val cls = when { c.isDigit() -> "\\d"; c.isLetter() -> "[A-Za-z]"; c.isWhitespace() -> "\\s"; else -> Regex.escape(c.toString()).let { Pattern.quote(c.toString()).let { _ -> "\\" + c } } }
                var j = i; while (j < s.length && (when { s[j].isDigit() -> "\\d"; s[j].isLetter() -> "[A-Za-z]"; s[j].isWhitespace() -> "\\s"; else -> "\\" + s[j] }) == cls) j++
                sb.append(if (c.isLetterOrDigit() || c.isWhitespace()) cls + (if (j - i > 1) "{${j - i}}" else "") else cls.repeat(1).let { cls + (if (j - i > 1) "{${j - i}}" else "") }); i = j }
            return sb.toString() }
        val pats = lines(examples).filter { it.isNotBlank() }.map { gen(it.trim()) }
        val uniq = pats.distinct()
        return "Per-example patterns:\n" + uniq.joinToString("\n") + (if (uniq.size > 1) "\n\nCombined:\n^(?:" + uniq.joinToString("|") + ")$" else "\n\nAnchored:\n^" + uniq.first() + "$")
    }
    fun counts(s: String): String { val w = s.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return "Characters: ${s.length}\nCharacters (no spaces): ${s.count { !it.isWhitespace() }}\nWords: ${w.size}\nLines: ${lines(s).size}\nSentences: ${Regex("[.!?]+(\\s|$)").findAll(s).count()}\nBytes (UTF-8): ${s.toByteArray().size}\nReading time: ${"%.1f".format(w.size / 220.0)} min" }
    fun slug(s: String) = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
}

val textTools: List<Tool> = listOf(
    tool("diff", "Text / JSON Diff", Category.TEXT, "Side-by-side style line diff and JSON path diff",
        mp("Line diff") { a, p -> Txt.diff(lines(a), lines(p.getOrElse(0) { "" })) }, mp("JSON diff") { a, p -> Txt.jsonDiff(a, p.getOrElse(0) { "" }) },
        mp("Similarity") { a, p -> val x = lines(a).toSet(); val y = lines(p.getOrElse(0) { "" }).toSet(); "${(100.0 * x.intersect(y).size / maxOf(1, x.union(y).size)).toInt()}% of lines are common" },
        hint = "Text A (original)", params = listOf("Text B (changed)"), sample = "one\ntwo\nthree", kw = "compare patch", psample = listOf("one\nTWO\nthree\nfour")),
    tool("find-replace", "Find & Replace (Regex)", Category.TEXT, "Regex or plain replace with capture groups ($1)",
        mp("Regex replace") { s, p -> Regex(p.getOrElse(0) { "" }).replace(guarded(s), p.getOrElse(1) { "" }) }, mp("Regex replace (ignore case)") { s, p -> Regex(p.getOrElse(0) { "" }, RegexOption.IGNORE_CASE).replace(guarded(s), p.getOrElse(1) { "" }) },
        mp("Plain replace") { s, p -> s.replace(p.getOrElse(0) { "" }, p.getOrElse(1) { "" }) }, mp("Count matches") { s, p -> Regex(p.getOrElse(0) { "" }).findAll(guarded(s)).count().toString() },
        hint = "Text", params = listOf("Find", "Replace with"), sample = "John Smith, Jane Doe", psample = listOf("(\\w+) (\\w+)", "\$2 \$1")),
    tool("lines", "Line Tools", Category.TEXT, "Sort, dedupe, reverse, number, trim, filter",
        m("Sort A→Z") { lines(it).sorted().joinToString("\n") }, m("Sort Z→A") { lines(it).sortedDescending().joinToString("\n") },
        m("Sort (ignore case)") { lines(it).sortedBy { l -> l.lowercase() }.joinToString("\n") }, m("Sort numeric") { lines(it).sortedBy { l -> l.trim().toDoubleOrNull() ?: Double.MAX_VALUE }.joinToString("\n") },
        m("Deduplicate") { lines(it).distinct().joinToString("\n") }, m("Reverse order") { lines(it).reversed().joinToString("\n") },
        m("Number lines") { lines(it).mapIndexed { i, l -> "${i + 1}. $l" }.joinToString("\n") }, m("Trim lines") { lines(it).joinToString("\n") { l -> l.trim() } },
        m("Remove empty") { lines(it).filter { l -> l.isNotBlank() }.joinToString("\n") }, m("Join with comma") { lines(it).filter { l -> l.isNotBlank() }.joinToString(", ") },
        mp("Prefix / suffix") { s, p -> lines(s).joinToString("\n") { p.getOrElse(0) { "" } + it + p.getOrElse(1) { "" } } },
        mp("Keep lines matching") { s, p -> lines(s).filter { Regex(p.getOrElse(0) { "" }).containsMatchIn(guarded(it)) }.joinToString("\n") },
        hint = "Lines", params = listOf("Prefix / regex", "Suffix"), sample = "pear\napple\nbanana\napple"),
    tool("counter", "Word / Character / Line Counter", Category.TEXT, "Text statistics", m("Count") { Txt.counts(it) }, hint = "Text"),
    tool("slug", "Slugify", Category.TEXT, "URL-friendly slugs", m("Slug") { lines(it).joinToString("\n") { l -> Txt.slug(l) } }, hint = "Text", sample = "Héllo, World! Kotlin & Compose"),
    tool("whitespace", "Whitespace & Line Endings", Category.TEXT, "CRLF ⇄ LF, tabs, trailing spaces",
        m("CRLF → LF") { it.replace("\r\n", "\n").replace("\r", "\n") }, m("LF → CRLF") { it.replace("\r\n", "\n").replace("\n", "\r\n") },
        m("Tabs → 4 spaces") { it.replace("\t", "    ") }, m("4 spaces → tabs") { it.replace("    ", "\t") },
        m("Trim trailing") { lines(it).joinToString("\n") { l -> l.trimEnd() } }, m("Collapse spaces") { it.replace(Regex("[ \\t]+"), " ") },
        m("Detect") { "CRLF: ${Regex("\r\n").findAll(it).count()}  LF-only: ${Regex("(?<!\r)\n").findAll(it).count()}  Tabs: ${it.count { c -> c == '\t' }}  Trailing-space lines: ${lines(it).count { l -> l != l.trimEnd() }}" },
        hint = "Text"),
    tool("columns", "Column Extractor", Category.TEXT, "Pick columns from CSV / TSV / space-separated text",
        mp("Extract columns") { s, p -> val d = p.getOrElse(1) { "" }.ifEmpty { "," }; val cols = p.getOrElse(0) { "" }.split(',').mapNotNull { it.trim().toIntOrNull() }; lines(s).joinToString("\n") { l -> val parts = if (d == "\\s") l.trim().split(Regex("\\s+")) else l.split(d); cols.joinToString(d.replace("\\s", " ")) { c -> parts.getOrElse(c - 1) { "" } } } },
        hint = "Delimited text", params = listOf("Columns, e.g. 1,3", "Delimiter (default , ; use \\s for whitespace)"), sample = "a,b,c\n1,2,3", psample = listOf("1,3", ",")),
    tool("regex", "Regex Tester", Category.TEXT, "Live matches with groups, replace, split and a token-by-token explainer (Java/Kotlin engine; syntax is close to PCRE/JS/.NET)",
        mp("Find all") { s, p -> Txt.matches(s, p.getOrElse(0) { "" }, "") }, mp("Ignore case") { s, p -> Txt.matches(s, p.getOrElse(0) { "" }, "i") },
        mp("Multiline") { s, p -> Txt.matches(s, p.getOrElse(0) { "" }, "m") }, mp("Dotall") { s, p -> Txt.matches(s, p.getOrElse(0) { "" }, "s") },
        mp("Replace") { s, p -> Pattern.compile(p.getOrElse(0) { "" }).matcher(guarded(s)).replaceAll(p.getOrElse(1) { "" }) },
        mp("Split") { s, p -> Regex(p.getOrElse(0) { "" }).split(guarded(s)).joinToString("\n") { Json.quote(it) } },
        mp("Explain pattern") { _, p -> Txt.explain(p.getOrElse(0) { "" }) },
        hint = "Test text", params = listOf("Pattern, e.g. (\\w+)@(\\w+)\\.com", "Replacement ($1 = group 1)"), sample = "ada@example.com, linus@kernel.com", kw = "regexp pcre", psample = listOf("(\\w+)@(\\w+)\\.com", "<\$1> at \$2")),
    tool("regex-gen", "Regex from Examples", Category.TEXT, "Generalise example strings (one per line) into a pattern",
        m("Generate") { Txt.regexFrom(it) }, hint = "Examples, one per line", sample = "AB-1234\nXY-9876\nQQ-0001"),
)
