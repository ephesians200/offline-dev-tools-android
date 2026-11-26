@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.codingtools.app.ui

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingtools.app.data.LocalStore
import com.codingtools.app.tools.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

// ───────────────────────── Syntax highlighting ─────────────────────────
private const val KW = "abstract|actual|and|as|assert|async|await|base|bool|boolean|break|by|byte|case|catch|char|class|companion|const|constructor|continue|data|def|default|defer|del|delete|do|double|dynamic|elif|else|elsif|end|enum|except|export|expect|extends|extern|false|final|finally|float|fn|for|foreach|fun|func|function|get|go|goto|if|impl|implements|import|in|init|inline|instanceof|int|interface|internal|is|lambda|late|lateinit|let|local|long|loop|match|module|mut|namespace|new|nil|not|null|object|of|open|operator|or|override|package|pass|print|private|protected|pub|public|raise|readonly|ref|repeat|return|sealed|select|self|set|short|static|string|struct|super|suspend|switch|synchronized|template|then|this|throw|throws|trait|true|try|type|typealias|typeof|undefined|union|unless|unsafe|until|use|using|val|var|virtual|void|volatile|when|where|while|with|yield|SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|DELETE|CREATE|TABLE|JOIN|LEFT|RIGHT|INNER|ON|GROUP|BY|ORDER|HAVING|LIMIT|AND|OR|NOT|NULL|AS|DISTINCT|PRIMARY|KEY|INTEGER|TEXT|REAL|select|from|where|insert|into|values|update|delete|create|table|join|left|right|inner|group|order|having|limit|distinct|primary|key|integer|text|real"
private val hashLangs = setOf("Python", "Ruby", "Bash", "YAML")
private val dashLangs = setOf("SQL", "Lua")

private fun highlight(code: String, lang: String, kw: Color, str: Color, num: Color, cmt: Color): AnnotatedString {
    val comment = when {
        lang in hashLangs -> "#[^\\n]*"
        lang in dashLangs -> "--[^\\n]*|/\\*.*?\\*/"
        lang == "HTML" -> "<!--.*?-->"
        lang == "JSON" || lang == "Plain" -> "(?!x)x"
        else -> "//[^\\n]*|/\\*.*?\\*/"
    }
    val re = Regex("(?<cmt>$comment)|(?<str>\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'|`[^`]*`)|(?<num>\\b\\d+(?:\\.\\d+)?\\b)|(?<kw>\\b(?:$KW)\\b)", RegexOption.DOT_MATCHES_ALL)
    return buildAnnotatedString {
        append(code)
        if (lang == "Plain") return@buildAnnotatedString
        re.findAll(code).forEach { m ->
            val c = when {
                m.groups["cmt"] != null -> cmt
                m.groups["str"] != null -> str
                m.groups["num"] != null -> num
                else -> kw
            }
            addStyle(SpanStyle(color = c, fontWeight = if (c == kw) FontWeight.SemiBold else null), m.range.first, m.range.last + 1)
        }
    }
}

private class Highlighter(val lang: String) : VisualTransformation {
    override fun filter(text: AnnotatedString) = TransformedText(highlight(text.text, lang, Color(0xFF8A5CF6), Color(0xFF1E9E6A), Color(0xFFE08A00), Color(0xFF8A94A8)), OffsetMapping.Identity)
}

private val editorLangs = listOf("Kotlin", "Java", "JavaScript", "TypeScript", "Python", "C", "C++", "C#", "Go", "Rust", "Swift", "Dart", "PHP", "Ruby", "SQL", "Bash", "HTML", "CSS", "JSON", "Lua", "Plain")
private val fmtKey = mapOf("Kotlin" to "kotlin", "Java" to "java", "JavaScript" to "js", "TypeScript" to "ts", "Python" to "python", "C" to "c", "C++" to "cpp", "C#" to "cs", "Go" to "go", "Rust" to "rust", "Swift" to "swift", "Dart" to "dart", "PHP" to "php", "Ruby" to "ruby", "SQL" to "sql", "Bash" to "bash", "HTML" to "html", "CSS" to "css", "JSON" to "json", "Lua" to "lua", "Plain" to "")

private fun runJs(ctx: Context, code: String, done: (String) -> Unit) {
    val script = "(function(){var __o=[];var __f=function(a){try{return (typeof a==='object'&&a!==null)?JSON.stringify(a):String(a)}catch(e){return String(a)}};" +
        "var console={log:function(){__o.push(Array.prototype.slice.call(arguments).map(__f).join(' '))}};console.error=console.warn=console.info=console.log;" +
        "try{var __r=eval(${Json.quote(code)});if(__r!==undefined)__o.push(__f(__r))}catch(e){__o.push('Error: '+e.message)}return __o.join('\\n')})()"
    val wv = WebView(ctx)
    wv.settings.javaScriptEnabled = true
    wv.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            view?.evaluateJavascript(script) { r -> done((runCatching { Json.parse(r) as? String }.getOrNull() ?: r ?: "").ifEmpty { "(no output)" }); view.destroy() }
        }
    }
    wv.loadDataWithBaseURL(null, "<html><body></body></html>", "text/html", "utf-8", null)
}

private data class EditorTab(val name: String, val lang: String, val code: String)

@Composable
fun EditorScreen(tool: Tool, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = LocalStore.current
    val clip = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val tabs = remember {
        mutableStateListOf<EditorTab>().apply {
            val saved = runCatching { (Json.parse(store.get("editor", "[]")) as List<*>).map { t -> (t as Map<*, *>).let { EditorTab(it["name"].toString(), it["lang"].toString(), it["code"].toString()) } } }.getOrDefault(emptyList())
            addAll(saved.ifEmpty { listOf(EditorTab("main.js", "JavaScript", "const xs = [3, 1, 2];\nconsole.log(xs.sort((a, b) => a - b));\nconsole.log(xs.map(x => x * 2));")) })
        }
    }
    var cur by remember { mutableIntStateOf(0) }
    var output by remember { mutableStateOf("") }
    val tab = tabs[cur.coerceIn(0, tabs.size - 1)]
    LaunchedEffect(tabs.toList()) { store.put("editor", Json.write(tabs.map { mapOf("name" to it.name, "lang" to it.lang, "code" to it.code) }, 0)) }
    fun update(t: EditorTab) { tabs[cur.coerceIn(0, tabs.size - 1)] = t }

    Frame(tool, onBack) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { i, t ->
                if (tabs.size > 1) FilterChip(cur == i, { cur = i }, { Text(t.name) }, trailingIcon = { Icon(Icons.Rounded.Close, "Close tab", Modifier.size(16.dp).clickable { tabs.removeAt(i); cur = 0 }) })
                else FilterChip(cur == i, { cur = i }, { Text(t.name) })
            }
            IconButton({ tabs.add(EditorTab("untitled-${tabs.size + 1}", "Kotlin", "")); cur = tabs.size - 1 }) { Icon(Icons.Rounded.Add, "New tab") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            editorLangs.forEach { l -> FilterChip(tab.lang == l, { update(tab.copy(lang = l)) }, { Text(l) }) }
        }
        OutlinedTextField(
            tab.code, { update(tab.copy(code = it)) }, Modifier.fillMaxWidth(), minLines = 12, maxLines = 30,
            textStyle = TextStyle(fontFamily = CodeFont, fontSize = 13.sp, lineHeight = 19.sp), visualTransformation = Highlighter(tab.lang), shape = RoundedCornerShape(16.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            PillButton("Run", Icons.Rounded.PlayArrow) {
                when (tab.lang) {
                    "JavaScript" -> { output = "Running…"; runJs(ctx, tab.code) { output = it } }
                    "SQL" -> scope.launch { output = withContext(Dispatchers.IO) { runCatching { SqlPlay.run(tab.code) }.getOrElse { "Error: ${it.message}" } } }
                    "JSON" -> output = runCatching { Json.write(Json.parse(tab.code)) }.getOrElse { "Error: ${it.message}" }
                    else -> output = "Running is available for JavaScript (built-in engine), SQL (SQLite) and JSON validation. Compiled languages can't be executed without a toolchain; use Beautify, the analysis tools and the cheat sheets for ${tab.lang}."
                }
            }
            PillButton("Beautify", Icons.Rounded.AutoAwesome, filled = false) { runCatching { CodeFmt.beautify(tab.code, fmtKey[tab.lang] ?: "") }.onSuccess { update(tab.copy(code = it)) }.onFailure { output = "Error: ${it.message}" } }
            PillButton("Copy", Icons.Rounded.ContentCopy, filled = false) { clip.setText(AnnotatedString(tab.code)) }
        }
        if (output.isNotEmpty()) SoftCard { Text("Output", fontWeight = FontWeight.Bold); SelectionContainer { Text(output, fontFamily = CodeFont, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) } }
        Text("JavaScript runs in the device's built-in WebView engine, fully offline. An infinite loop will freeze the app until you close it.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ───────────────────────── Snippet manager ─────────────────────────
private data class Snip(val title: String, val lang: String, val tags: String, val code: String)

@Composable
fun SnippetsScreen(tool: Tool, onBack: () -> Unit) {
    val store = LocalStore.current
    val clip = LocalClipboardManager.current
    var list by remember { mutableStateOf(runCatching { (Json.parse(store.get("snippets", "[]")) as List<*>).map { t -> (t as Map<*, *>).let { Snip(it["title"].toString(), it["lang"].toString(), it["tags"].toString(), it["code"].toString()) } } }.getOrDefault(emptyList())) }
    var q by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Snip?>(null) }
    var isNew by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }; var lang by remember { mutableStateOf("Kotlin") }; var tags by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }
    fun save(l: List<Snip>) { list = l; store.put("snippets", Json.write(l.map { mapOf("title" to it.title, "lang" to it.lang, "tags" to it.tags, "code" to it.code) }, 0)) }
    fun open(s: Snip?) { isNew = s == null; editing = s ?: Snip("", "Kotlin", "", ""); title = s?.title ?: ""; lang = s?.lang ?: "Kotlin"; tags = s?.tags ?: ""; code = s?.code ?: "" }

    Frame(tool, onBack, actions = { IconButton({ open(null) }) { Icon(Icons.Rounded.Add, "New snippet", tint = Color.White) } }) {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search title, tag, language or code") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, shape = RoundedCornerShape(50))
        val shown = list.filter { q.isBlank() || (it.title + it.tags + it.lang + it.code).contains(q, true) }
        if (shown.isEmpty()) Text("No snippets yet. Tap + to save your first one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        shown.forEach { s ->
            SoftCard(Modifier.clickable { open(s) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(s.lang, fontSize = 11.sp, color = Blue700, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 8.dp, vertical = 2.dp))
                }
                if (s.tags.isNotBlank()) Text(s.tags.split(',').joinToString("  ") { "#" + it.trim() }, fontSize = 11.sp, color = Blue500)
                Text(s.code.lines().take(4).joinToString("\n"), fontFamily = CodeFont, fontSize = 11.5.sp, maxLines = 4, modifier = Modifier.padding(top = 6.dp))
                TextButton({ clip.setText(AnnotatedString(s.code)) }) { Text("Copy") }
            }
        }
    }
    editing?.let { cur ->
        AlertDialog(
            onDismissRequest = { editing = null }, title = { Text(if (isNew) "New snippet" else "Edit snippet") },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(lang, { lang = it }, label = { Text("Language") }, singleLine = true)
                OutlinedTextField(tags, { tags = it }, label = { Text("Tags (comma separated)") }, singleLine = true)
                OutlinedTextField(code, { code = it }, label = { Text("Code") }, minLines = 5, maxLines = 12, textStyle = TextStyle(fontFamily = CodeFont, fontSize = 12.sp), visualTransformation = Highlighter(lang))
                if (!isNew) TextButton({ save(list.filter { it != cur }); editing = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { TextButton({ val n = Snip(title.ifBlank { "Untitled" }, lang.ifBlank { "Plain" }, tags, code); save(if (isNew) listOf(n) + list else list.map { if (it == cur) n else it }); editing = null }) { Text("Save") } },
            dismissButton = { TextButton({ editing = null }) { Text("Cancel") } }
        )
    }
}

// ───────────────────────── Markdown editor ─────────────────────────
private fun inlineMd(s: String, codeBg: Color, link: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    Regex("\\*\\*(.+?)\\*\\*|\\*(.+?)\\*|`(.+?)`|\\[(.+?)]\\((.+?)\\)").findAll(s).forEach { m ->
        append(s.substring(i, m.range.first))
        when {
            m.groups[1] != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[1]) }
            m.groups[2] != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(m.groupValues[2]) }
            m.groups[3] != null -> withStyle(SpanStyle(fontFamily = CodeFont, background = codeBg)) { append(m.groupValues[3]) }
            else -> withStyle(SpanStyle(color = link, textDecoration = TextDecoration.Underline)) { append(m.groupValues[4]) }
        }
        i = m.range.last + 1
    }
    append(s.substring(i))
}

@Composable
private fun MarkdownView(md: String) {
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    var inCode = false
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (line in md.lines()) {
            val t = line.trim()
            if (t.startsWith("```")) { inCode = !inCode; continue }
            if (inCode) { Text(line, Modifier.fillMaxWidth().background(codeBg).padding(horizontal = 8.dp), fontFamily = CodeFont, fontSize = 12.sp); continue }
            val h = Regex("^(#{1,6})\\s+(.*)").find(t)
            when {
                t.isEmpty() -> Spacer(Modifier.height(4.dp))
                h != null -> Text(inlineMd(h.groupValues[2], codeBg, Blue500), fontSize = (28 - h.groupValues[1].length * 3).sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                Regex("^[-*+]\\s+.*").matches(t) -> Row { Text("•  "); Text(inlineMd(t.substring(2), codeBg, Blue500)) }
                Regex("^\\d+[.)]\\s+.*").matches(t) -> Text(inlineMd(t, codeBg, Blue500), modifier = Modifier.padding(start = 4.dp))
                t.startsWith(">") -> Text(inlineMd(t.removePrefix(">").trim(), codeBg, Blue500), fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 10.dp))
                t == "---" || t == "***" -> HorizontalDivider()
                else -> Text(inlineMd(t, codeBg, Blue500))
            }
        }
    }
}

@Composable
fun MarkdownScreen(tool: Tool, onBack: () -> Unit) {
    val store = LocalStore.current
    var text by remember { mutableStateOf(TextFieldValue(store.get("md", "# Hello Markdown\n\nWrite **bold**, *italic*, `code` and [links](https://example.com).\n\n- item one\n- item two\n\n```kotlin\nfun main() = println(\"hi\")\n```"))) }
    var tab by remember { mutableIntStateOf(0) }
    LaunchedEffect(text.text) { store.put("md", text.text) }
    Frame(tool, onBack) {
        TabRow(tab) { Tab(tab == 0, { tab = 0 }, text = { Text("Write") }); Tab(tab == 1, { tab = 1 }, text = { Text("Preview") }) }
        if (tab == 0) OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), minLines = 14, textStyle = TextStyle(fontFamily = CodeFont, fontSize = 13.sp), shape = RoundedCornerShape(16.dp))
        else SoftCard { MarkdownView(text.text) }
    }
}

// ───────────────────────── File inspector ─────────────────────────
private fun hex4(b: ByteArray, o: Int, le: Boolean = true): Long { var r = 0L; for (k in 0 until 4) { val v = b[o + k].toLong() and 0xff; r = if (le) r or (v shl (8 * k)) else (r shl 8) or v }; return r }
private fun u16(b: ByteArray, o: Int, le: Boolean = true): Int = if (le) (b[o].toInt() and 0xff) or ((b[o + 1].toInt() and 0xff) shl 8) else ((b[o].toInt() and 0xff) shl 8) or (b[o + 1].toInt() and 0xff)

private fun execInfo(h: ByteArray): String {
    if (h.size < 64) return ""
    if (h[0].toInt() == 0x7f && h[1] == 'E'.code.toByte() && h[2] == 'L'.code.toByte() && h[3] == 'F'.code.toByte()) {
        val le = h[5].toInt() == 1
        val type = when (u16(h, 16, le)) { 1 -> "relocatable"; 2 -> "executable"; 3 -> "shared object / PIE"; 4 -> "core dump"; else -> "?" }
        val mach = when (u16(h, 18, le)) { 0x3E -> "x86-64"; 0x03 -> "x86"; 0xB7 -> "AArch64"; 0x28 -> "ARM"; 0xF3 -> "RISC-V"; 0x08 -> "MIPS"; else -> "0x%X".format(u16(h, 18, le)) }
        return "ELF · ${if (h[4].toInt() == 2) "64" else "32"}-bit · ${if (le) "little" else "big"}-endian · $type · $mach"
    }
    if (h[0] == 'M'.code.toByte() && h[1] == 'Z'.code.toByte()) {
        val pe = hex4(h, 0x3C).toInt()
        if (pe in 0..(h.size - 30) && h[pe] == 'P'.code.toByte() && h[pe + 1] == 'E'.code.toByte()) {
            val mach = when (u16(h, pe + 4)) { 0x14c -> "x86"; 0x8664 -> "x86-64"; 0xAA64 -> "ARM64"; 0x1c0 -> "ARM"; else -> "0x%X".format(u16(h, pe + 4)) }
            val ch = u16(h, pe + 22)
            return "PE · $mach · ${u16(h, pe + 6)} sections · ${if (ch and 0x2000 != 0) "DLL" else if (ch and 0x2 != 0) "EXE" else "object"}"
        }
        return "DOS MZ executable"
    }
    val magic = hex4(h, 0, false)
    if (magic == 0xFEEDFACEL || magic == 0xFEEDFACFL || magic == 0xCEFAEDFEL || magic == 0xCFFAEDFEL) {
        val le = magic == 0xCEFAEDFEL || magic == 0xCFFAEDFEL
        val cpu = hex4(h, 4, le)
        return "Mach-O · ${if (magic == 0xFEEDFACFL || magic == 0xCFFAEDFEL) "64" else "32"}-bit · cpu ${when (cpu) { 7L -> "x86"; 0x01000007L -> "x86-64"; 12L -> "ARM"; 0x0100000CL -> "ARM64"; else -> "0x%X".format(cpu) }} · filetype ${when (hex4(h, 12, le)) { 1L -> "object"; 2L -> "executable"; 6L -> "dylib"; 8L -> "bundle"; else -> "other" }}"
    }
    return ""
}

@Composable
fun FileInspectorScreen(tool: Tool, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf("") }
    var hashes by remember { mutableStateOf(emptyMap<String, String>()) }
    var verify by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            busy = true
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        val md = listOf("MD5", "SHA-1", "SHA-256").map { MessageDigest.getInstance(it) }
                        val head = ByteArray(4096); var headLen = 0; var total = 0L
                        ctx.contentResolver.openInputStream(uri)!!.use { ins ->
                            val buf = ByteArray(65536)
                            while (true) {
                                val n = ins.read(buf); if (n < 0) break
                                if (headLen < head.size) { val c = minOf(n, head.size - headLen); System.arraycopy(buf, 0, head, headLen, c); headLen += c }
                                md.forEach { it.update(buf, 0, n) }; total += n
                            }
                        }
                        val h = head.copyOf(headLen)
                        val hs = md.associate { it.algorithm to it.digest().hex() }
                        val sb = StringBuilder()
                        sb.append("Size      : $total bytes (${"%.2f".format(total / 1048576.0)} MiB)\nType      : ${Low.identify(h)}\n")
                        execInfo(h).takeIf { it.isNotEmpty() }?.let { sb.append("Executable: $it\n") }
                        hs.forEach { (k, v) -> sb.append("${k.padEnd(9)} : $v\n") }
                        if (h.size > 3 && h[0].toInt() == 0x50 && h[1].toInt() == 0x4B) {
                            sb.append("\nArchive entries:\n")
                            runCatching { ctx.contentResolver.openInputStream(uri)!!.use { s -> java.util.zip.ZipInputStream(s).use { z -> var e = z.nextEntry; var n = 0; while (e != null && n < 300) { sb.append("  ${e.name}${if (e.size >= 0) "  (${e.size} B)" else ""}\n"); e = z.nextEntry; n++ } } } }
                        }
                        sb.append("\nHex dump (first 512 bytes):\n").append(Low.dump(h.copyOf(minOf(512, h.size))))
                        hs to sb.toString()
                    }
                }
                res.onSuccess { hashes = it.first; report = it.second }.onFailure { report = "Error: ${it.message}" }
                busy = false
            }
        }
    }
    Frame(tool, onBack) {
        PillButton("Pick a file", Icons.Rounded.Storage, Modifier.fillMaxWidth()) { picker.launch(arrayOf("*/*")) }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (hashes.isNotEmpty()) {
            OutlinedTextField(verify, { verify = it }, Modifier.fillMaxWidth(), label = { Text("Paste expected checksum to verify") }, singleLine = true)
            if (verify.isNotBlank()) {
                val hit = hashes.entries.firstOrNull { it.value.equals(verify.trim(), true) }
                Text(if (hit != null) "✔ Matches ${hit.key}" else "✘ Does not match MD5, SHA-1 or SHA-256", fontWeight = FontWeight.Bold, color = if (hit != null) Color(0xFF1E9E6A) else MaterialTheme.colorScheme.error)
            }
        }
        if (report.isNotEmpty()) SoftCard { SelectionContainer { Text(report, fontFamily = CodeFont, fontSize = 11.5.sp) } }
    }
}

// ───────────────────────── Sorting visualizer ─────────────────────────
private class SFrame(val a: IntArray, val x: Int, val y: Int)

private fun sortFrames(algo: String, start: IntArray): List<SFrame> {
    val a = start.clone(); val f = ArrayList<SFrame>()
    fun snap(x: Int, y: Int) { if (f.size < 5000) f.add(SFrame(a.clone(), x, y)) }
    fun swap(i: Int, j: Int) { val t = a[i]; a[i] = a[j]; a[j] = t }
    fun ms(l: Int, r: Int) {
        if (r <= l) return
        val m = (l + r) / 2; ms(l, m); ms(m + 1, r)
        val tmp = ArrayList<Int>(); var i = l; var j = m + 1
        while (i <= m && j <= r) { snap(i, j); if (a[i] <= a[j]) tmp.add(a[i++]) else tmp.add(a[j++]) }
        while (i <= m) tmp.add(a[i++]); while (j <= r) tmp.add(a[j++])
        for (k in tmp.indices) { a[l + k] = tmp[k]; snap(l + k, l + k) }
    }
    fun qs(l: Int, r: Int) {
        if (l >= r) return
        val p = a[r]; var i = l
        for (j in l until r) { snap(j, r); if (a[j] < p) { swap(i, j); snap(i, j); i++ } }
        swap(i, r); snap(i, r); qs(l, i - 1); qs(i + 1, r)
    }
    when (algo) {
        "Bubble" -> for (i in a.indices) for (j in 0 until a.size - 1 - i) { snap(j, j + 1); if (a[j] > a[j + 1]) { swap(j, j + 1); snap(j, j + 1) } }
        "Selection" -> for (i in a.indices) { var m = i; for (j in i + 1 until a.size) { snap(j, m); if (a[j] < a[m]) m = j }; swap(i, m); snap(i, m) }
        "Insertion" -> for (i in 1 until a.size) { var j = i; while (j > 0 && a[j - 1] > a[j]) { snap(j - 1, j); swap(j - 1, j); j-- }; snap(j, j) }
        "Merge" -> ms(0, a.size - 1)
        else -> qs(0, a.size - 1)
    }
    f.add(SFrame(start.sortedArray(), -1, -1))
    return f
}

@Composable
fun SortVisScreen(tool: Tool, onBack: () -> Unit) {
    val algos = listOf("Bubble", "Selection", "Insertion", "Merge", "Quick")
    var algo by remember { mutableStateOf("Bubble") }
    var count by remember { mutableFloatStateOf(32f) }
    var seed by remember { mutableIntStateOf(1) }
    var speed by remember { mutableFloatStateOf(30f) }
    val data = remember(count, seed) { val r = java.util.Random(seed.toLong()); IntArray(count.toInt()) { 5 + r.nextInt(95) } }
    val frames = remember(algo, data) { sortFrames(algo, data) }
    var idx by remember(frames) { mutableIntStateOf(0) }
    var playing by remember(frames) { mutableStateOf(false) }
    LaunchedEffect(playing, frames) { while (playing && idx < frames.size - 1) { delay(speed.toLong().coerceAtLeast(1)); idx++ }; if (idx >= frames.size - 1) playing = false }
    val reduce = LocalReduceMotion.current
    val fr = frames[idx.coerceIn(0, frames.size - 1)]
    val prev = frames[(idx - 1).coerceIn(0, frames.size - 1)]
    val done = idx >= frames.size - 1
    // While playing, bars glide from the previous step's heights to the current ones.
    val glide = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(idx) {
        if (playing && !reduce) { glide.snapTo(0f); glide.animateTo(1f, androidx.compose.animation.core.tween(speed.toInt().coerceIn(30, 200), easing = androidx.compose.animation.core.LinearEasing)) }
        else glide.snapTo(1f)
    }
    // When the sort finishes the bars turn green in a left-to-right wave.
    val wave = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(done) {
        if (done && !reduce) { wave.snapTo(0f); wave.animateTo(1f, androidx.compose.animation.core.tween(600)) } else wave.snapTo(if (done) 1f else 0f)
    }
    Frame(tool, onBack) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { algos.forEach { a -> FilterChip(algo == a, { algo = a }, { Text(a) }) } }
        SoftCard {
            Canvas(Modifier.fillMaxWidth().height(220.dp)) {
                val n = fr.a.size; val w = size.width / n
                fr.a.forEachIndexed { i, v ->
                    val vv = prev.a[i] + (v - prev.a[i]) * glide.value
                    val h = size.height * vv / 100f
                    val c = when { done -> if (i.toFloat() / n <= wave.value) Mint else Blue500; i == fr.x -> Coral; i == fr.y -> Blue200; else -> Blue500 }
                    drawRect(c, Offset(i * w + 1f, size.height - h), Size(w - 2f, h))
                }
            }
            Text("Step ${idx + 1} / ${frames.size}${if (done) " · sorted ✔" else ""}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(if (playing) "Pause" else "Play", if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow) { if (done) idx = 0; playing = !playing }
            PillButton("Shuffle", Icons.Rounded.Refresh, filled = false) { seed++ }
        }
        Text("Bars: ${count.toInt()}"); Slider(count, { count = it }, valueRange = 8f..64f)
        Text("Delay per step: ${speed.toInt()} ms"); Slider(speed, { speed = it }, valueRange = 2f..200f)
        val info = mapOf("Bubble" to "O(n²) · stable · swaps neighbours", "Selection" to "O(n²) · picks the minimum each pass", "Insertion" to "O(n²), O(n) when nearly sorted · stable", "Merge" to "O(n log n) · stable · needs O(n) space", "Quick" to "O(n log n) average, O(n²) worst · in place")
        Text(info[algo] ?: "", fontSize = 13.sp)
    }
}

// ───────────────────────── Flashcards (Leitner) ─────────────────────────
private val deck = listOf(
    "Big-O" to ("What is the time complexity of binary search?" to "O(log n) – halve the search space each step (sorted input)."),
    "Big-O" to ("Average and worst case of quicksort?" to "O(n log n) average, O(n²) worst case (bad pivots)."),
    "Big-O" to ("Hash table lookup complexity?" to "O(1) average, O(n) worst case with many collisions."),
    "Data structures" to ("Stack vs queue?" to "Stack = LIFO (push/pop at the top). Queue = FIFO (enqueue at back, dequeue at front)."),
    "Data structures" to ("When to use a heap?" to "Priority queues, top-K problems, scheduling. Insert/remove-min are O(log n)."),
    "Data structures" to ("What makes a BST 'balanced' and why care?" to "Heights of subtrees differ little (AVL, red-black). Keeps operations at O(log n) instead of O(n)."),
    "Data structures" to ("Array vs linked list?" to "Array: O(1) index, contiguous memory, costly inserts. Linked list: O(1) insert at a known node, O(n) index."),
    "Algorithms" to ("BFS vs DFS?" to "BFS explores level by level with a queue (shortest path unweighted). DFS dives deep with a stack/recursion (cycles, topological sort)."),
    "Algorithms" to ("What is dynamic programming?" to "Solve overlapping subproblems once and reuse results (memoisation or tabulation)."),
    "Algorithms" to ("Dijkstra's algorithm requirement?" to "Non-negative edge weights. Uses a priority queue; O((V+E) log V)."),
    "OOP / design" to ("What does SOLID stand for?" to "Single responsibility, Open/closed, Liskov substitution, Interface segregation, Dependency inversion."),
    "OOP / design" to ("Composition vs inheritance?" to "Prefer composition: combine small objects instead of deep class hierarchies; more flexible and testable."),
    "OOP / design" to ("What is dependency injection?" to "Passing a class its dependencies instead of creating them inside – improves testing and decoupling."),
    "OS" to ("Process vs thread?" to "Process: own address space. Thread: shares the process memory, lighter, needs synchronisation."),
    "OS" to ("What is a deadlock?" to "Threads wait on each other's locks forever. Needs mutual exclusion, hold-and-wait, no pre-emption, circular wait."),
    "OS" to ("Mutex vs semaphore?" to "Mutex: one owner, binary lock. Semaphore: counter allowing N concurrent holders."),
    "Networking" to ("TCP vs UDP?" to "TCP: reliable, ordered, connection-based. UDP: connectionless, no guarantees, low latency."),
    "Networking" to ("What happens when you type a URL?" to "DNS lookup → TCP (+TLS) handshake → HTTP request → server response → render."),
    "Networking" to ("Idempotent HTTP methods?" to "GET, PUT, DELETE, HEAD, OPTIONS. Repeating them has the same effect. POST is not idempotent."),
    "Web" to ("What is CORS?" to "Browser rule that blocks cross-origin requests unless the server allows them with Access-Control-* headers."),
    "Web" to ("401 vs 403?" to "401 = not authenticated. 403 = authenticated but not allowed."),
    "Web" to ("What is XSS and the main defence?" to "Injecting script into pages. Defend with output encoding and a Content-Security-Policy."),
    "Databases" to ("ACID stands for?" to "Atomicity, Consistency, Isolation, Durability."),
    "Databases" to ("Why add an index, and what is the cost?" to "Speeds up reads on the indexed columns; slows writes and uses storage."),
    "Databases" to ("INNER vs LEFT JOIN?" to "INNER: only matching rows. LEFT: all left rows, NULLs where the right has no match."),
    "Git" to ("Merge vs rebase?" to "Merge keeps history with a merge commit. Rebase replays commits on a new base for linear history (don't rebase shared branches)."),
    "Git" to ("Undo the last commit but keep the changes?" to "git reset --soft HEAD~1"),
    "Kotlin" to ("val vs var vs const val?" to "val: read-only reference. var: mutable. const val: compile-time constant (primitives/String, top-level or object)."),
    "Kotlin" to ("What is a suspend function?" to "A function that can pause without blocking a thread; callable from coroutines or other suspend functions."),
    "Kotlin" to ("Sealed class use case?" to "Closed hierarchy for exhaustive 'when' – ideal for UI state and results."),
    "Compose" to ("What triggers recomposition?" to "Reads of State objects (mutableStateOf) that changed. Keep composables pure and hoist state."),
    "Compose" to ("remember vs rememberSaveable?" to "remember survives recomposition. rememberSaveable also survives configuration change / process death (Bundle-able values)."),
    "Android" to ("Activity lifecycle order?" to "onCreate → onStart → onResume → onPause → onStop → onDestroy."),
    "Android" to ("Why not do network calls on the main thread?" to "It blocks the UI (ANR) and throws NetworkOnMainThreadException. Use coroutines / background dispatchers."),
    "Security" to ("Why hash passwords with bcrypt/Argon2 rather than SHA-256?" to "They are deliberately slow and salted, resisting brute force; plain SHA-256 is far too fast."),
    "Security" to ("What is SQL injection and the fix?" to "Untrusted input altering a query. Fix: parameterised queries / prepared statements."),
    "Concurrency" to ("What is a race condition?" to "Result depends on unsynchronised timing of threads accessing shared state. Fix with locks, atomics or immutability."),
    "Testing" to ("Unit vs integration test?" to "Unit: one component in isolation (mocks). Integration: several parts working together."),
    "Math" to ("What is two's complement?" to "Signed integer encoding: invert the bits and add 1 to negate. Range for n bits: −2^(n−1) … 2^(n−1)−1."),
    "Math" to ("Why is 0.1 + 0.2 ≠ 0.3 in floating point?" to "0.1 and 0.2 have no exact binary representation, so tiny rounding errors accumulate (IEEE 754)."),
    "Encoding" to ("Base64 overhead?" to "About 33%: every 3 bytes become 4 ASCII characters."),
    "Encoding" to ("UTF-8 vs UTF-16?" to "UTF-8: 1–4 bytes, ASCII-compatible. UTF-16: 2 or 4 bytes (surrogate pairs)."),
)

@Composable
fun FlashcardsScreen(tool: Tool, onBack: () -> Unit) {
    val store = LocalStore.current
    var boxes by remember { mutableStateOf(runCatching { (Json.parse(store.get("fc", "{}")) as Map<*, *>).entries.associate { it.key.toString().toInt() to (it.value as Number).toInt() } }.getOrDefault(emptyMap())) }
    var cur by remember { mutableIntStateOf(-1) }
    var flipped by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("All") }
    val cats = remember { listOf("All") + deck.map { it.first }.distinct() }
    fun next() {
        val pool = deck.indices.filter { filter == "All" || deck[it].first == filter }
        val min = pool.minOf { boxes[it] ?: 0 }
        val cand = pool.filter { (boxes[it] ?: 0) == min && it != cur }.ifEmpty { pool }
        cur = cand.random(); flipped = false
    }
    fun rate(good: Boolean) {
        val m = boxes.toMutableMap(); m[cur] = if (good) minOf(4, (m[cur] ?: 0) + 1) else 0; boxes = m
        store.put("fc", Json.write(m.entries.associate { it.key.toString() to it.value }, 0)); next()
    }
    LaunchedEffect(filter) { next() }
    Frame(tool, onBack) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { cats.forEach { c -> FilterChip(filter == c, { filter = c }, { Text(c) }) } }
        if (cur >= 0) SoftCard(Modifier.clickable { flipped = !flipped }) {
            Text(deck[cur].first + " · box ${(boxes[cur] ?: 0) + 1}/5", fontSize = 12.sp, color = Blue500)
            Spacer(Modifier.height(8.dp))
            Text(if (flipped) deck[cur].second.second else deck[cur].second.first, fontSize = 19.sp, fontWeight = if (flipped) FontWeight.Normal else FontWeight.SemiBold, modifier = Modifier.heightIn(min = 140.dp))
            Text(if (flipped) "Tap to hide answer" else "Tap to reveal answer", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (flipped) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Again", Icons.Rounded.Refresh, Modifier.weight(1f), filled = false) { rate(false) }
            PillButton("Got it", Icons.Rounded.Check, Modifier.weight(1f)) { rate(true) }
        } else PillButton("Skip", Icons.Rounded.ChevronRight, Modifier.fillMaxWidth(), filled = false) { next() }
        SoftCard {
            Text("Progress", fontWeight = FontWeight.Bold)
            (0..4).forEach { b -> val n = deck.indices.count { (boxes[it] ?: 0) == b }; Text("Box ${b + 1}: $n cards${if (b == 4) " (mastered)" else ""}", fontSize = 13.sp) }
            Text("${deck.size} cards total. Cards you miss go back to box 1 and return more often.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
