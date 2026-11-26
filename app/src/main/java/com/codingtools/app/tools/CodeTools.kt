package com.codingtools.app.tools

object Code {
    fun loc(src: String, lang: String): String {
        val l = lang.lowercase().ifBlank { "kotlin" }; val line = when (l) { "python", "py", "ruby", "rb", "sh", "bash", "yaml", "r", "perl" -> "#"; "sql", "lua", "haskell" -> "--"; else -> "//" }
        var blank = 0; var comment = 0; var code = 0; var inBlock = false
        for (ln in lines(src)) { val t = ln.trim()
            when { t.isEmpty() -> blank++
                inBlock -> { comment++; if (t.contains("*/")) inBlock = false }
                t.startsWith(line) -> comment++
                t.startsWith("/*") && line == "//" -> { comment++; if (!t.contains("*/")) inBlock = true }
                else -> code++ } }
        val total = blank + comment + code
        return "Total lines : $total\nCode        : $code\nComments    : $comment\nBlank       : $blank\nComment ratio: ${if (total == 0) 0 else comment * 100 / maxOf(1, code + comment)}%\nLongest line: ${lines(src).maxOfOrNull { it.length } ?: 0} chars"
    }
    fun complexity(src: String): String {
        val s = src.replace(Regex("\"(\\\\.|[^\"\\\\])*\""), "\"\"").replace(Regex("//.*"), "").replace(Regex("#.*"), "")
        val kw = listOf("if", "else if", "elif", "for", "foreach", "while", "case", "when", "catch", "except", "&&", "||", "and", "or", "\\?").associateWith { k -> Regex(if (k.all { it.isLetter() || it == ' ' }) "\\b$k\\b" else k).findAll(s).count() }.filter { it.value > 0 }
        val v = 1 + (kw["if"] ?: 0) + (kw["elif"] ?: 0) + (kw["for"] ?: 0) + (kw["foreach"] ?: 0) + (kw["while"] ?: 0) + (kw["case"] ?: 0) + (kw["when"] ?: 0) + (kw["catch"] ?: 0) + (kw["except"] ?: 0) + (kw["&&"] ?: 0) + (kw["||"] ?: 0) + (kw["and"] ?: 0) + (kw["or"] ?: 0) + (kw["\\?"] ?: 0)
        val fns = Regex("\\b(fun|def|function|func|void|int|String|public|private)\\s+\\w+\\s*\\(").findAll(s).count().coerceAtLeast(1)
        return "Estimated cyclomatic complexity (whole snippet): $v\nApprox. functions: $fns  →  ~${v / fns + 0} per function\nRisk: ${when { v / fns <= 5 -> "low"; v / fns <= 10 -> "moderate"; v / fns <= 20 -> "high" else -> "very high – consider splitting" }}\n\nDecision points: ${kw.entries.joinToString { "${it.key.replace("\\", "")}×${it.value}" }}"
    }
    fun dupes(s: String): String { val ls = lines(s).map { it.trim() }
        val lineDup = ls.withIndex().filter { it.value.length > 12 && !it.value.startsWith("//") }.groupBy { it.value }.filter { it.value.size > 1 }
        val blocks = (0..ls.size - 3).map { i -> ls.subList(i, i + 3).joinToString("\n") to i + 1 }.filter { (b, _) -> b.lines().all { it.length > 3 } }.groupBy({ it.first }, { it.second }).filter { it.value.size > 1 }
        return "Duplicate lines: ${lineDup.size}\n" + lineDup.entries.take(25).joinToString("\n") { (k, v) -> "  ×${v.size} (lines ${v.joinToString { (it.index + 1).toString() }}): ${k.take(70)}" } + "\n\nDuplicate 3-line blocks: ${blocks.size}\n" + blocks.entries.take(10).joinToString("\n") { (_, v) -> "  at lines ${v.joinToString()}" } }
    fun naming(s: String): String {
        fun style(n: String) = when { n.all { it.isUpperCase() || it.isDigit() || it == '_' } && n.any { it == '_' } -> "CONSTANT_CASE"; n.contains('-') -> "kebab-case"; n.contains('_') && n.none { it.isUpperCase() } -> "snake_case"; n[0].isUpperCase() && n.any { it.isLowerCase() } -> "PascalCase"; n[0].isLowerCase() && n.any { it.isUpperCase() } -> "camelCase"; n.all { it.isLowerCase() || it.isDigit() } -> "lowercase"; else -> "mixed" }
        val ids = Regex("[A-Za-z_][A-Za-z0-9_-]*").findAll(s).map { it.value }.filter { it.length > 2 }.distinct().toList(); val g = ids.groupBy { style(it) }
        return "Identifier styles found:\n" + g.entries.sortedByDescending { it.value.size }.joinToString("\n") { "  ${it.key.padEnd(14)} ${it.value.size}  e.g. ${it.value.take(4).joinToString()}" } + "\n\n" + (if (g.keys.count { it in setOf("camelCase", "snake_case") } > 1) "⚠ Mixed camelCase and snake_case – pick one." else "✔ Consistent")
    }
    fun indent(s: String, to: String): String = when (to) { "spaces" -> s.replace("\t", "    "); "tabs" -> lines(s).joinToString("\n") { l -> val n = l.length - l.trimStart(' ').length; "\t".repeat(n / 4) + " ".repeat(n % 4) + l.trimStart(' ') }; else -> "Tab-indented lines: ${lines(s).count { it.startsWith("\t") }}\nSpace-indented lines: ${lines(s).count { it.startsWith(" ") }}\nMixed lines: ${lines(s).count { Regex("^( +\t|\t+ )").containsMatchIn(it) }}" }
    fun todos(s: String) = lines(s).withIndex().filter { Regex("\\b(TODO|FIXME|HACK|XXX|BUG|NOTE|OPTIMIZE)\\b").containsMatchIn(it.value) }.joinToString("\n") { "L${it.index + 1}: ${it.value.trim()}" }.ifEmpty { "No TODO / FIXME markers found" }
    private val secrets = listOf("AWS access key" to "AKIA[0-9A-Z]{16}", "Google API key" to "AIza[0-9A-Za-z_-]{35}", "GitHub token" to "gh[pousr]_[A-Za-z0-9]{36,}", "Slack token" to "xox[baprs]-[A-Za-z0-9-]{10,}", "Stripe live key" to "sk_live_[0-9a-zA-Z]{20,}", "Private key block" to "-----BEGIN (RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----", "JWT" to "eyJ[A-Za-z0-9_-]{10,}\\.eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}", "Password assignment" to "(?i)(password|passwd|pwd|secret|api[_-]?key|token)\\s*[:=]\\s*[\"'][^\"']{6,}[\"']", "Basic-auth URL" to "[a-z]+://[^/\\s:@]+:[^/\\s:@]+@")
    fun scan(s: String): String { val out = ArrayList<String>(); lines(s).forEachIndexed { i, l -> secrets.forEach { (n, p) -> Regex(p).find(l)?.let { out.add("L${i + 1}  $n  →  ${it.value.take(6)}…${it.value.takeLast(3)}") } } }
        return if (out.isEmpty()) "✔ No obvious secrets found (pattern-based check only)" else "⚠ ${out.size} potential secret(s):\n" + out.joinToString("\n") + "\n\nRotate anything real and move it to environment variables / a secrets manager." }
    fun deps(s: String): String { val t = s.trim()
        if (t.startsWith("{")) { val d = Json.parse(t) as Map<*, *>; (d["packages"] as? Map<*, *>)?.let { p -> return "${p.size - 1} packages\n" + p.entries.filter { it.key != "" }.joinToString("\n") { (k, v) -> k.toString().removePrefix("node_modules/").padEnd(40) + " " + (v as? Map<*, *>)?.get("version") } }
            (d["dependencies"] as? Map<*, *>)?.let { p -> return "${p.size} dependencies\n" + p.entries.joinToString("\n") { (k, v) -> k.toString().padEnd(40) + " " + ((v as? Map<*, *>)?.get("version") ?: v) } } }
        val gradle = Regex("(implementation|api|compileOnly|runtimeOnly|testImplementation|kapt|ksp)\\s*\\(?\\s*[\"']([^\"']+)[\"']").findAll(t).toList(); if (gradle.isNotEmpty()) return gradle.joinToString("\n") { it.groupValues[1].padEnd(18) + it.groupValues[2] }
        val req = lines(t).filter { it.isNotBlank() && !it.startsWith("#") }; return req.joinToString("\n") { it.trim().replace(Regex("^require\\s+"), "") } }
    fun trace(s: String): String { val out = ArrayList<String>()
        lines(s).forEach { l -> val t = l.trim(); when {
            t.startsWith("Caused by:") -> out.add("⚠ $t")
            Regex("^at ([\\w.$<>]+)\\((.*?):(\\d+)\\)").find(t) != null -> Regex("^at ([\\w.$<>]+)\\((.*?):(\\d+)\\)").find(t)!!.let { out.add("  ${it.groupValues[1]}  → ${it.groupValues[2]}:${it.groupValues[3]}") }
            Regex("File \"(.*?)\", line (\\d+), in (.*)").find(t) != null -> Regex("File \"(.*?)\", line (\\d+), in (.*)").find(t)!!.let { out.add("  ${it.groupValues[3]}  → ${it.groupValues[1].substringAfterLast('/')}:${it.groupValues[2]}") }
            Regex("^at (.*?) \\((.*?):(\\d+):(\\d+)\\)").find(t) != null -> Regex("^at (.*?) \\((.*?):(\\d+):(\\d+)\\)").find(t)!!.let { out.add("  ${it.groupValues[1]}  → ${it.groupValues[2].substringAfterLast('/')}:${it.groupValues[3]}") }
            Regex("^[\\w.$]+(Exception|Error)\\b.*").matches(t) || t.startsWith("Traceback") || Regex("^\\w*Error:").containsMatchIn(t) -> out.add("✘ $t")
        } }
        return if (out.isEmpty()) "No stack frames recognised (Java/Kotlin, Python, JS supported)" else out.joinToString("\n") + "\n\n${out.count { it.startsWith("  ") }} frame(s)" }
    fun log(s: String, filter: String, level: String): String { val ls = lines(s); val lv = level.uppercase().trim()
        val counts = listOf("ERROR", "WARN", "INFO", "DEBUG", "TRACE", "FATAL").associateWith { k -> ls.count { it.uppercase().contains(k) } }.filter { it.value > 0 }
        val r = if (filter.isBlank()) null else Regex(filter, RegexOption.IGNORE_CASE); val res = ls.filter { (lv.isEmpty() || it.uppercase().contains(lv)) && (r == null || r.containsMatchIn(it)) }
        return "Levels: ${counts.entries.joinToString { "${it.key}=${it.value}" }}\nShowing ${res.size} of ${ls.size} lines\n\n" + res.take(500).joinToString("\n") }
    fun deobf(s: String): String { var x = Regex("\\\\x([0-9a-fA-F]{2})").replace(s) { it.groupValues[1].toInt(16).toChar().toString().let { c -> if (c == "\"" || c == "\\") "\\" + c else c } }
        x = Regex("\\\\u([0-9a-fA-F]{4})").replace(x) { it.groupValues[1].toInt(16).toChar().toString().let { c -> if (c == "\"" || c == "\\") "\\" + c else c } }
        return CodeFmt.beautify(x, "js") }
    fun sourcemap(s: String): String { val d = Json.parse(s) as Map<*, *>; return "version : ${d["version"]}\nfile    : ${d["file"]}\nsources : ${(d["sources"] as? List<*>)?.size}\n" + (d["sources"] as? List<*>)?.take(40)?.joinToString("\n") { "  $it" } + "\nnames   : ${(d["names"] as? List<*>)?.size}\nmappings: ${d["mappings"]?.toString()?.length} chars\nsourcesContent embedded: ${d.containsKey("sourcesContent")}" }
}

val codeTools: List<Tool> = listOf(
    tool("loc", "Lines of Code Counter", Category.CODE, "Code / comment / blank lines", mp("Count") { s, p -> Code.loc(s, p.getOrElse(0) { "" }) }, hint = "Source code", params = listOf("Language (kotlin, python, sql…)")),
    tool("complexity", "Cyclomatic Complexity (estimate)", Category.CODE, "Counts decision points in any C-like / Python source", m("Analyse") { Code.complexity(it) }, hint = "Source code", sample = "fun f(a: Int): Int {\n  if (a > 1 && a < 5) { for (i in 0..a) { if (i % 2 == 0) println(i) } }\n  return a\n}"),
    tool("dupes", "Duplicate Code Finder", Category.CODE, "Repeated lines and 3-line blocks", m("Find") { Code.dupes(it) }, hint = "Source code"),
    tool("naming", "Naming Convention Checker", Category.CODE, "Detect mixed camelCase / snake_case", m("Check") { Code.naming(it) }, hint = "Source code", sample = "val userName = 1\nval user_id = 2\nconst MAX_SIZE = 3\nclass HttpClient"),
    tool("indent", "Indentation Fixer", Category.CODE, "Detect and convert tabs ⇄ spaces", m("Detect") { Code.indent(it, "") }, m("Tabs → spaces") { Code.indent(it, "spaces") }, m("Spaces → tabs") { Code.indent(it, "tabs") }, hint = "Source code"),
    tool("todos", "TODO / FIXME Extractor", Category.CODE, "List all marker comments with line numbers", m("Extract") { Code.todos(it) }, hint = "Source code", sample = "// TODO: add tests\nval x = 1 // FIXME later"),
    tool("secret-scan", "Secret Scanner", Category.CODE, "Find API keys, tokens, private keys and passwords in pasted code", m("Scan") { Code.scan(it) }, hint = "Paste code or config", sample = "aws = \"AKIAIOSFODNN7EXAMPLE\"\npassword = \"hunter2hunter2\""),
    tool("deps", "Dependency List Viewer", Category.CODE, "package-lock.json, Gradle dependencies, requirements.txt, go.mod", m("List") { Code.deps(it) }, hint = "Lock / build file"),
    tool("stacktrace", "Stack Trace Parser", Category.CODE, "Java, Kotlin, Python and JavaScript traces", m("Parse") { Code.trace(it) }, hint = "Stack trace"),
    tool("logview", "Log Viewer & Filter", Category.CODE, "Filter by level and regex, count levels", mp("Filter") { s, p -> Code.log(s, p.getOrElse(0) { "" }, p.getOrElse(1) { "" }) }, hint = "Log text", params = listOf("Regex filter", "Level (ERROR, WARN…)")),
    tool("deobfuscate", "JS De-obfuscator / Beautifier", Category.CODE, "Decode \\x / \\u escapes and re-indent minified code", m("De-obfuscate") { Code.deobf(it) }, m("Source map summary") { Code.sourcemap(it) }, hint = "Minified code or .map JSON"),
    custom("editor", "Code Editor & Runner", Category.CODE, "Syntax-highlighted editor for every major language. Run JavaScript and SQLite offline", "run javascript snippet"),
    custom("snippets", "Snippet Manager", Category.CODE, "Local snippet library with tags and search", "notes library"),
    custom("markdown", "Markdown Editor & Preview", Category.CODE, "Write Markdown with live preview", "md readme"),
    custom("clipboard", "Clipboard History", Category.CODE, "Collect code you copy and reuse it", "paste history"),
)
