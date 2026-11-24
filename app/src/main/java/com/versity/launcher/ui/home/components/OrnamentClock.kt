package com.versity.launcher.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.data.Ornament
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.theme.LauncherTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.min

private val OrnamentDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE  ·  d MMMM")

/**
 * Light digits with a thin decoration: a fading line with a star, crescent or nothing in the middle,
 * or corner marks framing the time. An optional date sits under the divider in small spaced capitals.
 * Everything scales from the digit size, so it works from the Customise preview up to tablets.
 */
@Composable
fun OrnamentClock(
    time: LocalTime,
    is24Hour: Boolean,
    ornament: Ornament,
    modifier: Modifier = Modifier,
    date: LocalDate? = null,
    sizeRange: ClosedFloatingPointRange<Float> = 44f..96f,
    widthFactor: Float = 0.2f,
    centered: Boolean = true,
) {
    BoxWithConstraints(
        modifier.testTag(TestTags.CLOCK_ORNAMENT).semantics { contentDescription = "Clock, ${ornament.name.lowercase()} style" },
        contentAlignment = if (centered) Alignment.TopCenter else Alignment.TopStart,
    ) {
        val sizeSp = (maxWidth.value * widthFactor).coerceIn(sizeRange.start, sizeRange.endInclusive)
        val colors = LauncherTheme.colors
        val ink = colors.onSurface
        val digits = TextStyle(
            color = ink,
            fontFamily = LauncherTheme.font,
            fontWeight = FontWeight.Light,
            fontSize = sizeSp.sp,
            lineHeight = (sizeSp * 1.1f).sp,
            letterSpacing = (sizeSp * 0.02f).sp,
            // Faint halo so thin digits hold up on busy wallpapers.
            shadow = Shadow(ink.copy(alpha = 0.35f), blurRadius = sizeSp * 0.25f),
        )
        val lineWidth: Dp = min(sizeSp * 3.6f, maxWidth.value * 0.9f).dp
        val text = "${hourText(time, is24Hour)}:${minuteText(time)}"

        Column(horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
            if (ornament == Ornament.CORNERS) {
                Box(
                    Modifier
                        .drawBehind { drawCorners(ink.copy(alpha = 0.7f), (sizeSp * 0.28f).dp.toPx()) }
                        .padding(horizontal = (sizeSp * 0.35f).dp, vertical = (sizeSp * 0.12f).dp),
                ) { Text(text, style = digits, maxLines = 1) }
            } else {
                Text(text, style = digits, maxLines = 1)
                Spacer(Modifier.height((sizeSp * 0.08f).dp))
                Canvas(Modifier.size(lineWidth, (sizeSp * 0.3f).dp)) { drawDivider(ornament, ink) }
            }
            if (date != null) {
                Spacer(Modifier.height((sizeSp * 0.14f).dp))
                Text(
                    date.format(OrnamentDateFormat).uppercase(),
                    color = colors.muted,
                    fontFamily = LauncherTheme.font,
                    fontSize = (sizeSp * 0.15f).coerceIn(10f, 15f).sp,
                    letterSpacing = 2.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Fading horizontal line with an optional centre symbol. Sizes are fractions of the canvas height. */
private fun DrawScope.drawDivider(ornament: Ornament, ink: Color) {
    val y = size.height / 2f
    val stroke = 1.dp.toPx()
    val symbol = size.height * 0.42f
    val gap = when (ornament) {
        Ornament.UNDERLINE -> 0f
        else -> symbol * 1.6f
    }
    val cx = size.width / 2f
    val left = Brush.horizontalGradient(listOf(Color.Transparent, ink.copy(alpha = 0.75f)), 0f, cx - gap)
    val right = Brush.horizontalGradient(listOf(ink.copy(alpha = 0.75f), Color.Transparent), cx + gap, size.width)
    drawLine(left, Offset(0f, y), Offset(cx - gap, y), stroke)
    drawLine(right, Offset(cx + gap, y), Offset(size.width, y), stroke)
    when (ornament) {
        Ornament.CELESTIAL -> drawSparkle(Offset(cx, y), symbol, ink)
        Ornament.MOON -> drawCrescent(Offset(cx, y), symbol * 0.62f, ink)
        Ornament.UNDERLINE, Ornament.CORNERS -> Unit
    }
}

/** Four-point star with concave sides and a soft glow. */
internal fun DrawScope.drawSparkle(c: Offset, r: Float, ink: Color) {
    drawCircle(Brush.radialGradient(listOf(ink.copy(alpha = 0.35f), Color.Transparent), c, r * 1.4f), r * 1.4f, c)
    val k = r * 0.16f
    val path = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x + k, c.y - k, c.x + r, c.y)
        quadraticTo(c.x + k, c.y + k, c.x, c.y + r)
        quadraticTo(c.x - k, c.y + k, c.x - r, c.y)
        quadraticTo(c.x - k, c.y - k, c.x, c.y - r)
        close()
    }
    drawPath(path, ink)
}

private fun DrawScope.drawCrescent(c: Offset, r: Float, ink: Color) {
    val outer = Path().apply { addOval(Rect(c, r)) }
    val bite = Path().apply { addOval(Rect(c + Offset(r * 0.45f, -r * 0.2f), r * 0.85f)) }
    drawPath(Path.combine(PathOperation.Difference, outer, bite), ink)
}

/** Four L-shaped marks at the corners of the drawing area. */
private fun DrawScope.drawCorners(ink: Color, arm: Float) {
    val s = 1.dp.toPx()
    val w = size.width
    val h = size.height
    listOf(
        Triple(Offset(0f, 0f), 1f, 1f),
        Triple(Offset(w, 0f), -1f, 1f),
        Triple(Offset(0f, h), 1f, -1f),
        Triple(Offset(w, h), -1f, -1f),
    ).forEach { (p, dx, dy) ->
        drawLine(ink, p, p + Offset(arm * dx, 0f), s)
        drawLine(ink, p, p + Offset(0f, arm * dy), s)
    }
}
