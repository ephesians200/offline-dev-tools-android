@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.codingtools.app.ui

import android.content.Intent
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingtools.app.data.LocalStore
import com.codingtools.app.data.Shared
import com.codingtools.app.tools.Category
import com.codingtools.app.tools.OutKind
import com.codingtools.app.tools.Tool
import com.codingtools.app.tools.detectKind
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private val symbols = listOf("{", "}", "[", "]", "(", ")", "<", ">", "\"", "'", "`", ";", ":", ",", ".", "/", "\\", "|", "&", "_", "-", "=", "+", "*", "#", "$", "%", "@", "!", "?", "~", "^", "\t")

/** Hard ceiling for one tool run. Regex guards usually fire much earlier (3 s, see tools/Guard.kt). */
private const val RUN_TIMEOUT_MS = 60_000L
/** Output longer than this is previewed; "Show all" reveals the rest. Copy/Share always use the full text. */
private const val OUTPUT_PREVIEW = 20_000
/** Last input is only persisted below this size (SharedPreferences is not a file store). */
private const val SAVED_INPUT_MAX = 20_000
/** Values above this are not written into the saved-instance-state Bundle (Android's limit is ~1 MB for the whole app). */
private const val SAVE_STATE_MAX = 50_000

/** Tools run on low-priority daemon threads so a runaway computation cannot starve the UI thread. */
private val ToolWorkers: CoroutineDispatcher by lazy {
    Executors.newCachedThreadPool { r -> Thread(r, "tool-worker").apply { isDaemon = true; priority = Thread.MIN_PRIORITY } }.asCoroutineDispatcher()
}

private val CappedTextSaver: Saver<TextFieldValue, Any> = Saver(
    save = { if (it.text.length > SAVE_STATE_MAX) "" else it.text },
    restore = { TextFieldValue(it as String) }
)
private val CappedStringSaver: Saver<String, Any> = Saver(
    save = { if (it.length > SAVE_STATE_MAX) "" else it },
    restore = { it as String }
)

/** Categories where re-running on every keystroke is cheap and side-effect free. */
private val liveCategories = setOf(
    Category.FORMAT, Category.CONVERT, Category.CODEC, Category.TEXT, Category.DATETIME,
    Category.WEB, Category.MATH, Category.CODE, Category.LOWLEVEL, Category.MOBILE
)

@Composable
fun ToolScreen(tool: Tool, onBack: () -> Unit) {
    val store = LocalStore.current
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val reduce = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(tool.id) { store.touch(tool.id) }

    // Never persist what people type into hashing / security tools (keys, tokens, passwords).
    val persist = store.rememberInput && tool.category != Category.CRYPTO

    // rememberSaveable: survives rotation / process death. Large values are dropped by the savers.
    var modeIdx by rememberSaveable(tool.id) { mutableStateOf(0) }
    var input by rememberSaveable(tool.id, stateSaver = CappedTextSaver) {
        mutableStateOf(TextFieldValue(Shared.take() ?: if (persist) store.get("in_${tool.id}") else ""))
    }
    var params by rememberSaveable(tool.id) {
        mutableStateOf<List<String>>(ArrayList(tool.params.indices.map { tool.paramSamples.getOrElse(it) { "" } }))
    }
    var output by rememberSaveable(tool.id, stateSaver = CappedStringSaver) { mutableStateOf("") }
    var isError by rememberSaveable(tool.id) { mutableStateOf(false) }
    var elapsed by rememberSaveable(tool.id) { mutableLongStateOf(0L) }
    var live by rememberSaveable(tool.id) { mutableStateOf(false) }
    var running by remember(tool.id) { mutableStateOf(false) }
    var runId by remember(tool.id) { mutableIntStateOf(0) }            // bumps on every finished run (drives the error shake)
    var pendingConfirm by remember(tool.id) { mutableStateOf<Int?>(null) }
    val jobHolder = remember(tool.id) { arrayOfNulls<Job>(1) }

    fun setParam(i: Int, v: String) { params = ArrayList(params).also { it[i] = v } }

    fun run(idx: Int = modeIdx) {
        val mode = tool.modes.getOrNull(idx) ?: return
        val text = input.text
        val p = params.toList()
        jobHolder[0]?.cancel()
        jobHolder[0] = scope.launch {
            running = true
            val t0 = SystemClock.elapsedRealtime()
            val work = async(ToolWorkers) { runCatching { mode.run(text, p) } }
            // await() is a suspension point, so the timeout can fire even though the tool code itself is blocking.
            val r = withTimeoutOrNull(RUN_TIMEOUT_MS) { work.await() }
            if (r == null) {
                work.cancel()
                output = "Timed out after ${RUN_TIMEOUT_MS / 1000} s. The input or pattern is probably too expensive for this tool."
                isError = true
            } else r.onSuccess { output = it; isError = false }.onFailure { output = it.message ?: it.toString(); isError = true }
            elapsed = SystemClock.elapsedRealtime() - t0
            runId++
            running = false
        }
    }

    /** Destructive modes (e.g. "Reset database") must be confirmed; everything else just runs. */
    fun requestRun(idx: Int) {
        val m = tool.modes.getOrNull(idx) ?: return
        if (m.destructive) pendingConfirm = idx else run(idx)
    }

    LaunchedEffect(tool.id) { if (tool.autoRun) run() }

    // Remember the last input per tool (debounced; removed again when blank or too large to store).
    LaunchedEffect(input.text, persist) {
        if (!persist) return@LaunchedEffect
        delay(600)
        val k = "in_${tool.id}"
        if (input.text.isBlank() || input.text.length > SAVED_INPUT_MAX) store.remove(k) else store.put(k, input.text)
    }

    // Live mode: re-run shortly after the user stops typing.
    val liveOk = tool.modes.isNotEmpty() && tool.category in liveCategories && tool.modes.none { it.destructive }
    LaunchedEffect(live, input.text, modeIdx, params) {
        if (live && liveOk && input.text.isNotBlank()) { delay(450); run() }
    }

    fun insert(sym: String) {
        val s = input.selection
        input = TextFieldValue(input.text.replaceRange(s.min, s.max, sym), TextRange(s.min + sym.length))
    }

    // Shake the output card when a run ends in an error.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(runId) {
        if (runId > 0 && isError && !reduce) {
            repeat(3) { shake.animateTo(8f, tween(40)); shake.animateTo(-8f, tween(40)) }
            shake.animateTo(0f, tween(40))
        }
    }

    pendingConfirm?.let { idx ->
        val m = tool.modes[idx]
        AlertDialog(
            onDismissRequest = { pendingConfirm = null },
            title = { Text("${m.name}?") },
            text = { Text("This cannot be undone.") },
            confirmButton = { TextButton({ pendingConfirm = null; run(idx) }) { Text(m.name, color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ pendingConfirm = null }) { Text("Cancel") } }
        )
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(tool.title, tool.category.title, onBack) { FavAction(tool) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tool.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (tool.modes.size > 1) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tool.modes.forEachIndexed { i, m ->
                        FilterChip(modeIdx == i, {
                            modeIdx = i
                            // Selecting a destructive mode never runs it; the Run button asks first.
                            if (!m.destructive && (input.text.isNotBlank() || tool.autoRun || tool.params.isNotEmpty())) run(i)
                        }, { Text(m.name) })
                    }
                }
            }

            OutlinedTextField(
                input, { input = it }, Modifier.fillMaxWidth(),
                label = { Text(tool.inputHint, maxLines = 1) }, minLines = 5, maxLines = 14,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = CodeFont, fontSize = 13.sp), shape = RoundedCornerShape(16.dp)
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                symbols.forEach { s ->
                    Surface(onClick = { insert(s) }, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(if (s == "\t") "⇥" else s, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontFamily = CodeFont, fontSize = 14.sp)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Paste", Icons.Rounded.ContentPaste, filled = false) { clipboard.getText()?.let { insert(it.text) } }
                if (tool.sample.isNotEmpty()) PillButton("Sample", Icons.Rounded.AutoAwesome, filled = false) { input = TextFieldValue(tool.sample) }
                PillButton("Clear", Icons.Rounded.Close, filled = false) { input = TextFieldValue(""); output = "" }
            }

            tool.params.forEachIndexed { i, hint ->
                OutlinedTextField(params.getOrElse(i) { "" }, { setParam(i, it) }, Modifier.fillMaxWidth(), label = { Text(hint) }, minLines = 1, maxLines = 6,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = CodeFont, fontSize = 13.sp), shape = RoundedCornerShape(16.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton(
                    if (running) "Running…  tap to cancel" else "Run  ·  ${tool.modes.getOrNull(modeIdx)?.name ?: ""}",
                    Icons.Rounded.PlayArrow, Modifier.weight(1f).height(50.dp), loading = running
                ) { if (running) { jobHolder[0]?.cancel(); running = false } else requestRun(modeIdx) }
                if (liveOk) FilterChip(live, { live = !live }, { Text("Live") }, leadingIcon = { Icon(Icons.Rounded.Bolt, null, Modifier.size(18.dp)) })
            }

            AnimatedVisibility(
                output.isNotEmpty(),
                enter = if (reduce) EnterTransition.None else fadeIn() + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = ExitTransition.None
            ) {
                var showAll by remember(output) { mutableStateOf(false) }
                val kind = remember(output, isError) { if (isError) OutKind.PLAIN else detectKind(tool.id, tool.title, output) }
                val truncated = !showAll && output.length > OUTPUT_PREVIEW
                val shown = if (truncated) output.take(OUTPUT_PREVIEW) else output
                val styled = rememberHighlighted(shown, kind)
                SoftCard(Modifier.offset { IntOffset(shake.value.roundToInt(), 0) }.animateContentSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isError) "Error" else "Output", fontWeight = FontWeight.Bold, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Text("${output.length} chars · ${elapsed} ms", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SelectionContainer {
                        Text(styled, Modifier.padding(top = 8.dp), fontFamily = CodeFont, fontSize = 13.sp, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                    if (truncated) Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Showing the first ${"%,d".format(OUTPUT_PREVIEW)} of ${"%,d".format(output.length)} characters", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        TextButton({ showAll = true }) { Text("Show all") }
                    }
                    if (!isError) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CopyPill { clipboard.setText(AnnotatedString(output)) }
                        PillButton("Share", Icons.Rounded.Share, filled = false) {
                            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, output) }, "Share output"))
                        }
                        PillButton("Use as input", Icons.Rounded.SwapVert, filled = false) { input = TextFieldValue(output); output = "" }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ReferenceScreen(tool: Tool, onBack: () -> Unit) {
    val store = LocalStore.current
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(tool.id) { store.touch(tool.id) }
    val lines = remember(tool.id) { tool.reference!!.lines() }
    var q by remember { mutableStateOf("") }
    val shown = remember(q, lines) { if (q.isBlank()) lines else lines.filter { it.contains(q, ignoreCase = true) } }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(tool.title, tool.description, onBack) {
            IconButton({ clipboard.setText(AnnotatedString(tool.reference!!)) }) { Icon(Icons.Rounded.ContentCopy, "Copy all", tint = androidx.compose.ui.graphics.Color.White) }
            FavAction(tool)
        }
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(16.dp), singleLine = true, placeholder = { Text("Filter lines…") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, shape = RoundedCornerShape(50),
            trailingIcon = { if (q.isNotEmpty()) IconButton({ q = "" }) { Icon(Icons.Rounded.Close, "Clear") } })
        SelectionContainer(Modifier.weight(1f)) {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                items(shown) { l ->
                    val key = l.takeWhile { !it.isWhitespace() }
                    val head = l.isNotBlank() && !l.startsWith(" ") && (l == l.uppercase() || l.length < 4)
                    Text(l.ifEmpty { " " }, fontFamily = CodeFont, fontSize = 12.5.sp, lineHeight = 18.sp,
                        fontWeight = if (head && key.length > 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (head && key.length > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}
