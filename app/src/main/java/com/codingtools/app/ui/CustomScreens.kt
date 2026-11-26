@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.codingtools.app.ui

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.view.WindowManager
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.codingtools.app.data.LocalStore
import com.codingtools.app.data.Vault
import com.codingtools.app.tools.Json
import com.codingtools.app.tools.Registry
import com.codingtools.app.tools.Tool
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun Frame(tool: Tool, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    val store = LocalStore.current
    LaunchedEffect(tool.id) { store.touch(tool.id) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(tool.title, tool.category.title, onBack) { actions(); FavAction(tool) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** Every custom tool id maps to its screen here. AppRoot verifies (debug builds) that no tool points at a missing id. */
private val customScreens: Map<String, @Composable (Tool, () -> Unit) -> Unit> = buildMap<String, @Composable (Tool, () -> Unit) -> Unit> {
    put("image-base64") { t, b -> ImageBase64Screen(t, b) }
    put("qr-barcode") { t, b -> QrScreen(t, b) }
    put("vault") { t, b -> VaultScreen(t, b) }
    put("clipboard") { t, b -> ClipboardScreen(t, b) }
    put("pomodoro") { t, b -> PomodoroScreen(t, b) }
    put("pipeline") { t, b -> PipelineScreen(t, b) }
    put("editor") { t, b -> EditorScreen(t, b) }
    put("snippets") { t, b -> SnippetsScreen(t, b) }
    put("markdown") { t, b -> MarkdownScreen(t, b) }
    put("file-inspector") { t, b -> FileInspectorScreen(t, b) }
    put("sort-vis") { t, b -> SortVisScreen(t, b) }
    put("flashcards") { t, b -> FlashcardsScreen(t, b) }
}

val customScreenIds: Set<String> get() = customScreens.keys

@Composable
fun CustomScreen(tool: Tool, onBack: () -> Unit) {
    val screen = customScreens[tool.custom]
    if (screen != null) screen(tool, onBack) else Frame(tool, onBack) { Text("This tool is not available in this build.") }
}

// ───────────────────────── Image ⇄ Base64 ─────────────────────────
@Composable
private fun ImageBase64Screen(tool: Tool, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var maxDim by remember { mutableFloatStateOf(1024f) }
    var quality by remember { mutableFloatStateOf(80f) }
    var fmt by remember { mutableStateOf("JPEG") }
    var b64 by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var decodeIn by remember { mutableStateOf("") }
    var decoded by remember { mutableStateOf<Bitmap?>(null) }
    var err by remember { mutableStateOf("") }

    fun encode() {
        val b = bmp ?: return
        val sc = minOf(1f, maxDim / maxOf(b.width, b.height))
        val r = if (sc < 1f) Bitmap.createScaledBitmap(b, (b.width * sc).toInt().coerceAtLeast(1), (b.height * sc).toInt().coerceAtLeast(1), true) else b
        val os = ByteArrayOutputStream()
        val (cf, mime) = when (fmt) {
            "PNG" -> Bitmap.CompressFormat.PNG to "image/png"
            "WEBP" -> (if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP) to "image/webp"
            else -> Bitmap.CompressFormat.JPEG to "image/jpeg"
        }
        r.compress(cf, quality.toInt(), os)
        val bytes = os.toByteArray()
        b64 = "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        info = "${r.width}×${r.height} px · ${"%.1f".format(bytes.size / 1024.0)} KB · Base64 ${b64.length} chars"
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runCatching { ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } }.onSuccess { bmp = it; encode() }.onFailure { err = it.message ?: "Could not open image" }
    }

    Frame(tool, onBack) {
        PillButton("Pick image", Icons.Rounded.Image, Modifier.fillMaxWidth()) { picker.launch("image/*") }
        if (err.isNotEmpty()) Text(err, color = MaterialTheme.colorScheme.error)
        bmp?.let { b ->
            SoftCard {
                Image(b.asImageBitmap(), null, Modifier.fillMaxWidth().heightIn(max = 220.dp), contentScale = ContentScale.Fit)
                Text("Original ${b.width}×${b.height}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("JPEG", "PNG", "WEBP").forEach { f -> FilterChip(fmt == f, { fmt = f; encode() }, { Text(f) }) }
                }
                Text("Max size: ${maxDim.toInt()} px")
                Slider(maxDim, { maxDim = it }, valueRange = 64f..4096f, onValueChangeFinished = { encode() })
                Text("Quality: ${quality.toInt()} (JPEG / WEBP)")
                Slider(quality, { quality = it }, valueRange = 5f..100f, onValueChangeFinished = { encode() })
                Text(info, fontWeight = FontWeight.SemiBold)
                PillButton("Copy data URI", Icons.Rounded.ContentCopy) { clip.setText(AnnotatedString(b64)) }
                PillButton("Copy raw Base64", Icons.Rounded.ContentCopy, filled = false) { clip.setText(AnnotatedString(b64.substringAfter("base64,"))) }
            }
        }
        SoftCard {
            Text("Base64 → image", fontWeight = FontWeight.Bold)
            OutlinedTextField(decodeIn, { decodeIn = it }, Modifier.fillMaxWidth(), label = { Text("Paste Base64 or data URI") }, minLines = 3, maxLines = 6)
            PillButton("Decode & preview", Icons.Rounded.Image) {
                runCatching { val raw = Base64.decode(decodeIn.substringAfter("base64,").trim(), Base64.DEFAULT); BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: error("Not an image") }
                    .onSuccess { decoded = it; err = "" }.onFailure { err = it.message ?: "Decode failed" }
            }
            decoded?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().heightIn(max = 240.dp), contentScale = ContentScale.Fit) }
        }
    }
}

// ───────────────────────── QR / barcode ─────────────────────────
private val barcodeFormats = listOf("QR Code" to BarcodeFormat.QR_CODE, "Code 128" to BarcodeFormat.CODE_128, "Code 39" to BarcodeFormat.CODE_39, "EAN-13" to BarcodeFormat.EAN_13, "PDF417" to BarcodeFormat.PDF_417, "Aztec" to BarcodeFormat.AZTEC, "Data Matrix" to BarcodeFormat.DATA_MATRIX)

private fun makeCode(text: String, f: BarcodeFormat): Bitmap {
    val square = f == BarcodeFormat.QR_CODE || f == BarcodeFormat.AZTEC || f == BarcodeFormat.DATA_MATRIX
    val w = if (square) 640 else 900
    val h = if (square) 640 else 320
    val m = MultiFormatWriter().encode(text, f, w, h, mapOf(EncodeHintType.MARGIN to 1))
    val px = IntArray(m.width * m.height) { i -> if (m.get(i % m.width, i / m.width)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
    return Bitmap.createBitmap(px, m.width, m.height, Bitmap.Config.ARGB_8888)
}

@Composable
private fun QrScreen(tool: Tool, onBack: () -> Unit) {
    var text by remember { mutableStateOf("https://example.com") }
    var idx by remember { mutableIntStateOf(0) }
    val result = remember(text, idx) { runCatching { makeCode(text.ifEmpty { " " }, barcodeFormats[idx].second) } }
    Frame(tool, onBack) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            barcodeFormats.forEachIndexed { i, (n, _) -> FilterChip(idx == i, { idx = i }, { Text(n) }) }
        }
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text("Content") }, minLines = 2, maxLines = 6)
        SoftCard {
            result.onSuccess { Image(it.asImageBitmap(), "Generated code", Modifier.fillMaxWidth().heightIn(max = 360.dp), contentScale = ContentScale.Fit) }
                .onFailure { Text(it.message ?: "Cannot encode this content in the chosen format", color = MaterialTheme.colorScheme.error) }
        }
        Text("EAN-13 needs 12–13 digits · Code 39 supports A–Z 0–9 - . $ / + % space · QR holds up to ~2900 bytes.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ───────────────────────── Secure vault ─────────────────────────
/**
 * Gate: device credential / biometric before the notes are shown, FLAG_SECURE so the screen never
 * appears in screenshots or the recents thumbnail, and re-lock whenever the app leaves the foreground.
 * If the device has no screen lock at all the gate cannot work; the vault opens with a warning.
 */
@Composable
private fun VaultScreen(tool: Tool, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx.findActivity() }
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    val gateAvailable = remember(activity) {
        activity is FragmentActivity && BiometricManager.from(activity).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var prompting by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    fun startAuth() {
        val fa = activity as? FragmentActivity ?: return
        if (prompting) return
        prompting = true; authError = null
        val prompt = BiometricPrompt(fa, ContextCompat.getMainExecutor(fa), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { prompting = false; unlocked = true }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { prompting = false; authError = errString.toString() }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock vault").setSubtitle("Confirm it's you to view your secure notes")
                .setAllowedAuthenticators(authenticators).build()
        )
    }
    // ON_START is replayed when this composable enters, so this is also the initial prompt.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { if (gateAvailable && !unlocked && !prompting) startAuth() }
    // Ignore the stop caused by the system credential screen itself (prompting == true).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (!prompting) unlocked = false }

    if (gateAvailable && !unlocked) {
        Frame(tool, onBack) {
            Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(56.dp), tint = Blue500)
                Text("Vault is locked", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(authError ?: "Use your fingerprint, face or screen lock to open your secure notes.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                PillButton("Unlock", Icons.Rounded.LockOpen) { startAuth() }
            }
        }
    } else VaultContent(tool, onBack, noScreenLock = !gateAvailable)
}

@Composable
private fun VaultContent(tool: Tool, onBack: () -> Unit, noScreenLock: Boolean) {
    val store = LocalStore.current
    val ctx = LocalContext.current
    var loadKey by remember { mutableIntStateOf(0) }                    // bump to retry decryption
    val loaded = remember(loadKey) { Vault.load(store.get("vault")) }
    val failure = loaded.exceptionOrNull()
    var notes by remember(loadKey) { mutableStateOf(loaded.getOrNull() ?: emptyList()) }
    var editing by remember { mutableStateOf<Map<String, String>?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var confirmErase by remember { mutableStateOf(false) }

    fun save(l: List<Map<String, String>>) {
        if (failure != null) return                                      // never write over data we could not read
        val old = store.get("vault")
        if (old.isNotBlank()) store.put("vault_bak", old)                // one-step undo for corruption
        notes = l
        store.put("vault", Vault.encrypt(Json.write(l, 0)))
    }

    if (failure != null) {
        Frame(tool, onBack) {
            SoftCard {
                Text("Can't open the vault", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text(
                    "Your encrypted notes could not be decrypted (${failure.message ?: failure::class.simpleName}). This can happen after the device lock screen or biometrics change, or if the app's key was invalidated. Nothing has been deleted or overwritten.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)
                )
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton("Try again", Icons.Rounded.Refresh) { loadKey++ }
                    PillButton("Erase vault", Icons.Rounded.Delete, filled = false) { confirmErase = true }
                }
            }
        }
        if (confirmErase) AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Erase the vault?") },
            text = { Text("All stored notes are permanently deleted. This cannot be undone.") },
            confirmButton = { TextButton({ Vault.resetKey(); store.remove("vault"); store.remove("vault_bak"); confirmErase = false; loadKey++ }) { Text("Erase", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ confirmErase = false }) { Text("Cancel") } }
        )
        return
    }

    Frame(tool, onBack, actions = { IconButton({ editing = emptyMap(); title = ""; body = "" }) { Icon(Icons.Rounded.Add, "New note", tint = androidx.compose.ui.graphics.Color.White) } }) {
        Text("Encrypted with AES-256-GCM. The key lives in the Android Keystore and never leaves this device.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (noScreenLock) Text("⚠ This device has no screen lock, so the vault cannot ask you to authenticate. Set a PIN, pattern or biometric in system settings.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        if (notes.isEmpty()) Text("No notes yet. Tap + to add API keys, recovery codes or snippets.")
        notes.forEach { n ->
            SoftCard(onClick = { editing = n; title = n["title"] ?: ""; body = n["body"] ?: "" }) {
                Text(n["title"] ?: "", fontWeight = FontWeight.Bold)
                // Fixed-length mask: the number of dots must not leak the length of the secret.
                if ((n["body"] ?: "").isNotEmpty()) Text("••••••••", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    editing?.let { cur ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (cur.isEmpty()) "New secure note" else "Edit note") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(body, { body = it }, label = { Text("Secret / note") }, minLines = 4, maxLines = 10, textStyle = androidx.compose.ui.text.TextStyle(fontFamily = CodeFont))
                if (cur.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Marked sensitive and cleared after 45 s (best effort, see copySecret).
                    TextButton({ copySecret(ctx, body) }) { Text("Copy") }
                    TextButton({ save(notes.filter { it != cur }); editing = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
            } },
            confirmButton = { TextButton({
                val n = mapOf("title" to title.ifBlank { "Untitled" }, "body" to body)
                save(if (cur.isEmpty()) notes + n else notes.map { if (it == cur) n else it }); editing = null
            }) { Text("Save") } },
            dismissButton = { TextButton({ editing = null }) { Text("Cancel") } }
        )
    }
}

// ───────────────────────── Clipboard history ─────────────────────────
@Composable
private fun ClipboardScreen(tool: Tool, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = LocalStore.current
    val clip = LocalClipboardManager.current
    var items by remember { mutableStateOf(runCatching { (Json.parse(store.get("clip", "[]")) as List<*>).map { it.toString() } }.getOrDefault(emptyList())) }
    fun save(l: List<String>) { items = l; store.put("clip", Json.write(l, 0)) }
    fun capture() {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val t = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString()
        if (!t.isNullOrBlank() && t != items.firstOrNull()) save((listOf(t) + items.filter { it != t }).take(100))
    }
    LaunchedEffect(Unit) { delay(300); capture() }
    Frame(tool, onBack, actions = { TextButton({ save(emptyList()) }) { Text("Clear", color = androidx.compose.ui.graphics.Color.White) } }) {
        PillButton("Capture current clipboard", Icons.Rounded.ContentPaste, Modifier.fillMaxWidth()) { capture() }
        Text("Android only lets apps read the clipboard while they are in the foreground, so open this screen after copying. Items never leave the device.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        items.forEach { t ->
            SoftCard(onClick = { clip.setText(AnnotatedString(t)) }) {
                Text(t.take(300), fontFamily = CodeFont, fontSize = 12.sp, maxLines = 6)
                Row { Text("Tap to copy again", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text("Delete", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.clickable { save(items - t) }) }
            }
        }
    }
}

// ───────────────────────── Pomodoro / time tracker ─────────────────────────
@Composable
private fun PomodoroScreen(tool: Tool, onBack: () -> Unit) {
    val store = LocalStore.current
    val reduce = LocalReduceMotion.current
    val haptic = LocalHapticFeedback.current
    val presets = listOf("Focus 25" to 25, "Short 5" to 5, "Long 15" to 15, "Stopwatch" to 0)

    // Timer state is rememberSaveable and based on wall-clock timestamps instead of a per-tick counter:
    // rotation, backgrounding and process death neither reset it nor let it drift.
    var preset by rememberSaveable { mutableIntStateOf(0) }
    var project by rememberSaveable { mutableStateOf("My project") }
    var running by rememberSaveable { mutableStateOf(false) }
    var baseMs by rememberSaveable { mutableLongStateOf(25 * 60_000L) }   // remaining (countdown) or elapsed (stopwatch) at anchorMs
    var anchorMs by rememberSaveable { mutableLongStateOf(0L) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var doneTick by remember { mutableIntStateOf(0) }
    var totals by remember { mutableStateOf(runCatching { (Json.parse(store.get("pomo", "{}")) as Map<*, *>).entries.associate { it.key.toString() to ((it.value as? Number)?.toDouble() ?: 0.0) } }.getOrDefault(emptyMap())) }

    val stop = presets[preset].second == 0
    val totalMs = presets[preset].second * 60_000L
    fun msAt(t: Long): Long = when {
        !running -> baseMs
        stop -> baseMs + (t - anchorMs)
        else -> (baseMs - (t - anchorMs)).coerceAtLeast(0L)
    }
    fun addMinutes(m: Double) { if (m <= 0) return; val t = totals.toMutableMap(); t[project] = (t[project] ?: 0.0) + m; totals = t; store.put("pomo", Json.write(t)) }
    fun toggle() {
        val t = System.currentTimeMillis()
        if (running) { baseMs = msAt(t); running = false }
        else { if (!stop && baseMs <= 0) baseMs = totalMs; finished = false; anchorMs = t; nowMs = t; running = true }
    }
    fun reset() { running = false; finished = false; baseMs = if (stop) 0L else totalMs }

    LaunchedEffect(running, preset) {
        while (running) {
            nowMs = System.currentTimeMillis()
            if (!stop && baseMs - (nowMs - anchorMs) <= 0) {
                running = false; baseMs = 0L; finished = true; doneTick++
                if (preset == 0) addMinutes(25.0)
            }
            delay(250)
        }
    }

    // Completion: haptic tick and three quick pulses of the ring.
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(doneTick) {
        if (doneTick > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!reduce) repeat(3) { pulse.animateTo(1.07f, tween(180)); pulse.animateTo(1f, tween(180)) }
        }
    }

    val shownMs = msAt(nowMs)
    val seconds = (if (stop) shownMs / 1000 else (shownMs + 999) / 1000).toInt()
    val frac = if (totalMs == 0L) 0f else (1f - shownMs / totalMs.toFloat()).coerceIn(0f, 1f)
    val ring by animateFloatAsState(frac, if (reduce) androidx.compose.animation.core.snap() else tween(250, easing = LinearEasing), label = "ring")

    Frame(tool, onBack) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEachIndexed { i, (n, _) -> FilterChip(preset == i, { preset = i; running = false; finished = false; baseMs = presets[i].second * 60_000L }, { Text(n) }) }
        }
        OutlinedTextField(project, { project = it }, Modifier.fillMaxWidth(), label = { Text("Project / task") }, singleLine = true)
        SoftCard {
            Box(Modifier.fillMaxWidth().graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }, contentAlignment = Alignment.Center) {
                if (!stop) CircularProgressIndicator(
                    progress = { ring }, modifier = Modifier.size(200.dp), strokeWidth = 12.dp,
                    color = if (finished) Color(0xFF1E9E6A) else ProgressIndicatorDefaults.circularColor
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = if (stop) 40.dp else 0.dp)) {
                    Text("%02d:%02d".format(seconds / 60, seconds % 60), fontSize = 46.sp, fontWeight = FontWeight.Bold)
                    if (finished) Text("Session complete ✔", fontSize = 12.sp, color = Color(0xFF1E9E6A), fontWeight = FontWeight.SemiBold)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                PillButton(if (running) "Pause" else "Start", if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow) { toggle() }
                PillButton("Reset", Icons.Rounded.Refresh, filled = false) { if (stop) addMinutes(msAt(System.currentTimeMillis()) / 60000.0); reset() }
            }
        }
        SoftCard {
            Text("Time per project", fontWeight = FontWeight.Bold)
            if (totals.isEmpty()) Text("Completed focus sessions are logged here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            totals.entries.sortedByDescending { it.value }.forEach { (k, v) -> Row { Text(k, Modifier.weight(1f)); Text("%.0f min".format(v), fontWeight = FontWeight.SemiBold) } }
            if (totals.isNotEmpty()) TextButton({ totals = emptyMap(); store.put("pomo", "{}") }) { Text("Reset log") }
        }
    }
}

// ───────────────────────── Pipeline builder ─────────────────────────
private data class Step(val toolId: String, val modeIdx: Int)

@Composable
private fun PipelineScreen(tool: Tool, onBack: () -> Unit) {
    val clip = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val candidates = remember { Registry.all.filter { it.custom == null && it.reference == null && it.params.isEmpty() && it.modes.isNotEmpty() && !it.autoRun && it.id != "sqlite" }.flatMap { t -> t.modes.indices.map { Step(t.id, it) } } }
    val steps = remember { mutableStateListOf(Step("base64", 0)) }
    var input by remember { mutableStateOf("  Hello, pipeline!  ") }
    var outputs by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAdd by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    fun label(s: Step): String { val t = Registry.byId[s.toolId]!!; return t.title + " · " + t.modes[s.modeIdx].name }

    Frame(tool, onBack) {
        Text("Chain transforms: the output of each step feeds the next. Only tools without extra parameters can be chained.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth(), label = { Text("Input") }, minLines = 3, maxLines = 8, textStyle = androidx.compose.ui.text.TextStyle(fontFamily = CodeFont, fontSize = 13.sp))
        steps.forEachIndexed { i, s ->
            SoftCard { Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}.", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)); Text(label(s), Modifier.weight(1f), fontSize = 13.sp)
                IconButton({ steps.removeAt(i) }) { Icon(Icons.Rounded.Close, "Remove") }
            } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("Add step", Icons.Rounded.Add, filled = false) { showAdd = true }
            PillButton("Run chain", Icons.Rounded.PlayArrow) {
                scope.launch {
                    val res = withContext(Dispatchers.Default) {
                        val out = ArrayList<String>(); var cur = input
                        for (s in steps) { val r = runCatching { Registry.byId[s.toolId]!!.modes[s.modeIdx].run(cur, emptyList()) }; if (r.isFailure) { out.add("✘ ${label(s)}: ${r.exceptionOrNull()?.message}"); break }; cur = r.getOrThrow(); out.add(cur) }
                        out
                    }
                    outputs = res
                }
            }
        }
        outputs.forEachIndexed { i, o ->
            SoftCard {
                Text(if (i < steps.size) "After step ${i + 1}" else "Result", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelectionContainer { Text(o.take(4000), fontFamily = CodeFont, fontSize = 12.sp) }
            }
        }
        if (outputs.isNotEmpty()) PillButton("Copy final result", Icons.Rounded.ContentCopy) { clip.setText(AnnotatedString(outputs.last())) }
    }
    if (showAdd) AlertDialog(
        onDismissRequest = { showAdd = false }, title = { Text("Add step") },
        text = { Column {
            OutlinedTextField(q, { q = it }, singleLine = true, placeholder = { Text("Search tool or mode") })
            val list = candidates.filter { q.isBlank() || label(it).contains(q, true) }.take(60)
            LazyColumn(Modifier.heightIn(max = 320.dp)) { items(list) { s -> Text(label(s), Modifier.fillMaxWidth().clickable { steps.add(s); showAdd = false }.padding(vertical = 10.dp), fontSize = 14.sp) } }
        } },
        confirmButton = { TextButton({ showAdd = false }) { Text("Close") } }
    )
}
