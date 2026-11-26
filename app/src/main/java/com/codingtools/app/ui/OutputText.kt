package com.codingtools.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.codingtools.app.tools.OutKind
import com.codingtools.app.tools.Tok
import com.codingtools.app.tools.tokenize

private fun tokColor(t: Tok, dark: Boolean): Color = when (t) {
    Tok.KEY -> if (dark) Color(0xFF7FB2FF) else Blue700
    Tok.STRING -> if (dark) Color(0xFF7CF2C4) else Color(0xFF1E8E5F)
    Tok.NUMBER -> if (dark) Color(0xFFFFC46B) else Color(0xFFB86E00)
    Tok.LITERAL, Tok.KEYWORD -> if (dark) Color(0xFFD7A8FF) else Color(0xFF7A4CE0)
    Tok.COMMENT -> if (dark) Color(0xFF8A94A8) else Color(0xFF6B7690)
}

/** Syntax coloured text for JSON / SQL output; plain text is returned untouched. */
@Composable
fun rememberHighlighted(text: String, kind: OutKind): AnnotatedString {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return remember(text, kind, dark) {
        if (kind == OutKind.PLAIN) AnnotatedString(text)
        else buildAnnotatedString {
            append(text)
            tokenize(text, kind).forEach {
                addStyle(SpanStyle(color = tokColor(it.type, dark), fontWeight = if (it.type == Tok.KEYWORD) FontWeight.SemiBold else null), it.start, it.end)
            }
        }
    }
}
