package com.codingtools.app.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** True when the user switched on "Reduce motion" in Settings or the system animations are off. */
val LocalReduceMotion = compositionLocalOf { false }

/** Returns [spec], or an instant snap when motion is reduced. */
fun <T> motion(reduce: Boolean, spec: AnimationSpec<T>): AnimationSpec<T> = if (reduce) snap() else spec

/**
 * Clickable card surface with a springy press-scale. Order matters: the scale is applied first so the
 * shadow, clip and ripple all scale together.
 */
@Composable
fun Modifier.cardClickable(shape: Shape, elevation: Dp = 3.dp, glow: Color = Blue900.copy(alpha = 0.15f), onClick: () -> Unit): Modifier {
    val reduce = LocalReduceMotion.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, motion(reduce, spring(dampingRatio = 0.6f, stiffness = 500f)), label = "press")
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .shadow(elevation, shape, ambientColor = glow, spotColor = glow)
        .clickable(src, LocalIndication.current, onClick = onClick)
}

/**
 * Fade + rise entrance, 40 ms apart per index. Only the first rows animate: a LazyColumn replays this
 * when a row re-enters composition, so deeper rows are left alone.
 */
@Composable
fun Modifier.staggeredEntrance(index: Int): Modifier {
    val reduce = LocalReduceMotion.current
    if (reduce || index > 8) return this
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(index * 40L); a.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) }
    return this.graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 24.dp.toPx() }
}
