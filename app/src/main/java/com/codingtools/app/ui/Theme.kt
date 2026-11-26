package com.codingtools.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codingtools.app.R

val Blue900 = Color(0xFF0B3D91)
val Blue700 = Color(0xFF1456C8)
val Blue500 = Color(0xFF3B82F6)
val Blue200 = Color(0xFF9CC2FF)
val Coral = Color(0xFFFF6B6B)
val Mint = Color(0xFF7CF2C4)
/** Bundled JetBrains Mono (SIL OFL 1.1) for every input, output and code surface. */
val CodeFont = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
    Font(R.font.jetbrains_mono_bold, FontWeight.SemiBold)
)
val HeaderBrush = Brush.verticalGradient(listOf(Color(0xFF0B3D91), Color(0xFF1456C8)))

private val Light = lightColorScheme(
    primary = Blue700, onPrimary = Color.White, primaryContainer = Color(0xFFDCE8FF), onPrimaryContainer = Blue900,
    secondary = Blue500, background = Color(0xFFEEF3FB), onBackground = Color(0xFF0F1B33),
    surface = Color.White, onSurface = Color(0xFF0F1B33), surfaceVariant = Color(0xFFE6EDFA), onSurfaceVariant = Color(0xFF55627F),
    outline = Color(0xFFC3CEE3), error = Color(0xFFD64545)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF7FB2FF), onPrimary = Color(0xFF002F6C), primaryContainer = Color(0xFF123A78), onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Blue500, background = Color(0xFF0B1220), onBackground = Color(0xFFE6ECF8),
    surface = Color(0xFF131C2E), onSurface = Color(0xFFE6ECF8), surfaceVariant = Color(0xFF1B2740), onSurfaceVariant = Color(0xFFB4C0D8),
    outline = Color(0xFF3A4A6B), error = Color(0xFFFF8A8A)
)

@Composable
fun CodingToolsTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(26.dp)),
        content = content
    )
}
