package app.suran

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Ink {
    // Palette of suran.tech (style.css), so app and website are one brand.
    val bg = Color(0xFFF7F5F1)          // --color-bg
    val ink = Color(0xFF1A1918)         // --color-text
    val muted = Color(0xFF5F5B54)       // --color-text-secondary (6.2:1 on bg)
    val line = Color(0xFFDEDAD2)        // --color-border: hairlines, disabled fill (never text)
    val accent = Color(0xFF3F4A3C)      // --color-accent: primary button, selected dot
    val accentPressed = Color(0xFF333C31) // --color-accent-hover
    val surfaceAlt = Color(0xFFEFECE6)  // --color-bg-alt: pressed rows, quiet notice
    val error = Color(0xFF9C4530)       // --color-error (5.8:1 on bg): failure text only
}

@Composable
fun T(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Ink.ink,
    weight: FontWeight = FontWeight.Normal,
    spacing: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    lineHeight: TextUnit = TextUnit.Unspecified,
    underline: Boolean = false,
) {
    // Website rule (--tracking-tight: -0.02em) for headline-sized text.
    val tracking = if (spacing == TextUnit.Unspecified && size.value >= 24f) (size.value * -0.02f).sp else spacing
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color, fontSize = size, fontWeight = weight, fontFamily = FontFamily.SansSerif,
            letterSpacing = tracking, lineHeight = lineHeight,
            textDecoration = if (underline) TextDecoration.Underline else null,
        ),
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
    )
}

@Composable
fun TA(text: AnnotatedString, size: TextUnit, modifier: Modifier = Modifier, color: Color = Ink.muted) = BasicText(
    text = text,
    modifier = modifier,
    style = TextStyle(color = color, fontSize = size, fontFamily = FontFamily.SansSerif),
)

/** Plain tap target with a quiet pressed state (100 ms fade to #E8E8ED). */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role = Role.Button,
    label: String? = null,
): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed && enabled) Ink.surfaceAlt else Color.Transparent, tween(100), label = "press")
    return this.background(bg).clickable(
        interactionSource = src, indication = null, enabled = enabled,
        onClickLabel = label, role = role, onClick = onClick,
    )
}

/** Bottom sheets: same surface as the page, defined by a 1 dp hairline (like the website's cards), no shadow. */
val SheetShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)

fun Modifier.sheetSurface(): Modifier = this.background(Ink.bg, SheetShape).border(1.dp, Ink.line, SheetShape)
