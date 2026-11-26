@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.codingtools.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingtools.app.data.LocalStore
import com.codingtools.app.tools.Category
import com.codingtools.app.tools.Tool
import com.codingtools.app.tools.isPremium
import kotlinx.coroutines.delay

fun categoryIcon(c: Category): ImageVector = when (c) {
    Category.FORMAT -> Icons.Rounded.DataObject
    Category.CONVERT -> Icons.Rounded.SwapHoriz
    Category.CODEC -> Icons.Rounded.Tag
    Category.CRYPTO -> Icons.Rounded.Lock
    Category.GENERATE -> Icons.Rounded.AutoAwesome
    Category.TEXT -> Icons.Rounded.TextFields
    Category.DATETIME -> Icons.Rounded.Schedule
    Category.WEB -> Icons.Rounded.Language
    Category.NETWORK -> Icons.Rounded.Wifi
    Category.DATA -> Icons.Rounded.Storage
    Category.MATH -> Icons.Rounded.Calculate
    Category.CODE -> Icons.Rounded.Code
    Category.LOWLEVEL -> Icons.Rounded.Memory
    Category.MOBILE -> Icons.Rounded.PhoneAndroid
    Category.DEVOPS -> Icons.Rounded.Build
    Category.REFERENCE -> Icons.Rounded.MenuBook
    Category.LEARN -> Icons.Rounded.School
    Category.PRODUCTIVITY -> Icons.Rounded.Bolt
}

@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 44.dp, tint: Color = MaterialTheme.colorScheme.primary, bg: Color = MaterialTheme.colorScheme.primaryContainer) {
    Box(modifier.size(size).clip(RoundedCornerShape(14.dp)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** White rounded card row for a tool: springy press feedback and an animated favourite star. */
@Composable
fun ToolRow(tool: Tool, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val store = LocalStore.current
    val fav = tool.id in store.favorites
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier.fillMaxWidth().cardClickable(shape, 3.dp, onClick = onClick),
        color = MaterialTheme.colorScheme.surface, shape = shape
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(categoryIcon(tool.category))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tool.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (tool.isPremium && !store.premium) { Spacer(Modifier.width(8.dp)); ProBadge() }
                }
                Text(tool.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            FavStar(fav, MaterialTheme.colorScheme.outline, Color(0xFFFFB020)) { store.toggleFav(tool.id) }
        }
    }
}

/** Small amber "PRO" pill shown next to locked tools (hidden once Pro is unlocked). */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFB020).copy(alpha = 0.18f)).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Lock, null, tint = Color(0xFFB36B00), modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text("PRO", color = Color(0xFFB36B00), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/** Star that pops with a bouncy spring when switched on, and cross-fades its colour. */
@Composable
fun FavStar(fav: Boolean, offTint: Color, onTint: Color, onClick: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val s = remember { Animatable(1f) }
    var skip by remember { mutableStateOf(true) }          // no pop on first composition
    LaunchedEffect(fav) {
        if (!skip && fav && !reduce) { s.snapTo(0.6f); s.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f)) }
        skip = false
    }
    val tint by animateColorAsState(if (fav) onTint else offTint, motion(reduce, androidx.compose.animation.core.tween(180)), label = "star")
    IconButton(onClick, Modifier.size(36.dp)) {
        Icon(if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder, "Favourite", tint = tint,
            modifier = Modifier.graphicsLayer { scaleX = s.value; scaleY = s.value })
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
        if (action != null) Text(action, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable(onClick = onAction))
    }
}

/** Blue gradient header with rounded bottom corners (matches the "Today" / "My Patients" headers). */
@Composable
fun BlueHeader(modifier: Modifier = Modifier, bottomRadius: Dp = 32.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = bottomRadius, bottomEnd = bottomRadius)).background(HeaderBrush).statusBarsPadding(),
        content = content
    )
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    BlueHeader(bottomRadius = 28.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White) } else Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            actions()
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
fun FavAction(tool: Tool) {
    val store = LocalStore.current
    FavStar(tool.id in store.favorites, Color.White, Color(0xFFFFD166)) { store.toggleFav(tool.id) }
}

@Composable
fun PillButton(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier, filled: Boolean = true, loading: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    if (filled) Button(onClick, modifier, shape = shape, colors = ButtonDefaults.buttonColors(containerColor = Blue500, contentColor = Color.White)) {
        if (loading) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)) }
        else if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, fontWeight = FontWeight.SemiBold)
    } else OutlinedButton(onClick, modifier, shape = shape) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }; Text(text)
    }
}

/** Copy button that morphs into a green "Copied" check for 1.5 s and gives a haptic tick. */
@Composable
fun CopyPill(modifier: Modifier = Modifier, onCopy: () -> Unit) {
    val reduce = LocalReduceMotion.current
    var copied by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(copied) { if (copied) { delay(1500); copied = false } }
    val container by animateColorAsState(if (copied) Color(0xFF1E9E6A) else Blue500, motion(reduce, androidx.compose.animation.core.tween(200)), label = "copyBg")
    Button(
        onClick = { onCopy(); copied = true; haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
        modifier = modifier, shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = Color.White)
    ) {
        AnimatedContent(copied, transitionSpec = {
            if (reduce) EnterTransition.None togetherWith ExitTransition.None
            else (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith fadeOut()
        }, label = "copy") { c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (c) Icons.Rounded.Check else Icons.Rounded.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text(if (c) "Copied" else "Copy", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SoftCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val glow = Blue900.copy(alpha = 0.12f)
    val base = modifier.fillMaxWidth()
    Surface(
        if (onClick != null) base.cardClickable(shape, 3.dp, glow, onClick) else base.shadow(3.dp, shape, ambientColor = glow, spotColor = glow),
        shape = shape, color = MaterialTheme.colorScheme.surface
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun Dot(active: Boolean) {
    Box(Modifier.padding(3.dp).size(if (active) 18.dp else 6.dp, 6.dp).clip(CircleShape).background(if (active) Blue500 else Color.White.copy(alpha = 0.45f)))
}
