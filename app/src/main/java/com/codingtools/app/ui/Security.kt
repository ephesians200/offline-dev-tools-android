package com.codingtools.app.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

private const val SECRET_CLIP_MS = 45_000L

/**
 * Copies a secret: marks the clip as sensitive (Android 13+ hides it from the clipboard preview)
 * and makes a best-effort attempt to clear it after 45 s. Android only lets the focused app read
 * the clipboard, so the clear is skipped when we can no longer verify the clip is still ours.
 */
fun copySecret(ctx: Context, text: String) {
    val app = ctx.applicationContext
    val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("secret", text)
    clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
    cm.setPrimaryClip(clip)
    Handler(Looper.getMainLooper()).postDelayed({
        runCatching {
            val cur = cm.primaryClip
            val same = cur != null && cur.itemCount > 0 && cur.getItemAt(0).text?.toString() == text
            if (same) { if (android.os.Build.VERSION.SDK_INT >= 28) cm.clearPrimaryClip() else cm.setPrimaryClip(ClipData.newPlainText("", "")) }
        }
    }, SECRET_CLIP_MS)
}
