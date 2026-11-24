package com.versity.launcher.ui.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.versity.launcher.data.Backdrop
import com.versity.launcher.domain.Palette
import com.versity.launcher.ui.TestTags
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The one place backdrops are drawn. Home, lock-style screen, drawer, Customise preview, theme tiles
 * and the exported lock wallpaper all call this, so a theme looks the same everywhere.
 * Everything is a fraction of the canvas size, with dp strokes, so it adapts to any screen.
 */
fun DrawScope.drawBackdrop(backdrop: Backdrop, palette: Palette, accent: Color) {
    val upper = Color(palette.rimUpper)
    val lower = Color(palette.rimLower)
    when (backdrop) {
        Backdrop.NONE -> Unit
        Backdrop.ORBIT -> drawOrbit(upper, lower)
        Backdrop.RINGS -> drawRings(upper, lower)
        Backdrop.HORIZON -> drawHorizon(upper, lower, Color(palette.onSurface))
        Backdrop.WAVES -> drawWaves(upper, accent)
    }
}

/** Draws [backdrop] over its parent. Tagged so tests can tell which one is on screen. */
@Composable
fun BackdropLayer(backdrop: Backdrop, palette: Palette, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .testTag(TestTags.backdrop(backdrop.name))
            .then(if (backdrop == Backdrop.NONE) Modifier else Modifier.drawBehind { drawBackdrop(backdrop, palette, accent) }),
    )
}

/** Two touching circles: stacked in portrait, side by side in landscape. Only this one has a glow. */
private fun DrawScope.drawOrbit(upper: Color, lower: Color) {
    val w = size.width
    val h = size.height
    val wide = w > h
    val r = if (wide) min(w / 4f, h / 2f) else min(w / 2f, h / 4f)
    val c1 = if (wide) Offset(w / 2f - r, h / 2f) else Offset(w / 2f, h / 2f - r)
    val c2 = if (wide) Offset(w / 2f + r, h / 2f) else Offset(w / 2f, h / 2f + r)
    val upperRim = Brush.verticalGradient(listOf(upper.copy(alpha = 0.18f), upper), c1.y - r, c1.y + r)
    val upperFill = Brush.verticalGradient(listOf(upper.copy(alpha = 0.30f), Color.Transparent), c1.y - r, c1.y + r * 0.45f)
    val lowerRim = Brush.verticalGradient(listOf(lower, lower.copy(alpha = 0.30f)), c2.y - r, c2.y + r)
    val lowerFill = Brush.verticalGradient(listOf(Color.Transparent, lower.copy(alpha = 0.40f)), c2.y - r * 0.2f, c2.y + r)
    drawCircle(upperFill, r, c1)
    drawCircle(lowerFill, r, c2)
    // Soft glow without RenderEffect (API 31+ only): wide faint strokes under a crisp one.
    drawCircle(upperRim, r, c1, alpha = 0.05f, style = Stroke(14.dp.toPx()))
    drawCircle(lowerRim, r, c2, alpha = 0.05f, style = Stroke(14.dp.toPx()))
    drawCircle(upperRim, r, c1, alpha = 0.10f, style = Stroke(7.dp.toPx()))
    drawCircle(lowerRim, r, c2, alpha = 0.10f, style = Stroke(7.dp.toPx()))
    drawCircle(upperRim, r, c1, style = Stroke(2.5.dp.toPx()))
    drawCircle(lowerRim, r, c2, style = Stroke(2.5.dp.toPx()))
}

/** Five concentric arcs rising from the bottom-left corner, fading outwards; one picks up the second rim colour. */
private fun DrawScope.drawRings(upper: Color, lower: Color) {
    val centre = Offset(size.width * 0.05f, size.height * 0.95f)
    val base = max(size.width, size.height) * 0.22f
    for (k in 1..5) {
        drawCircle(
            color = if (k == 3) lower else upper,
            radius = base * k,
            center = centre,
            alpha = 0.35f - (k - 1) * 0.07f,
            style = Stroke(1.dp.toPx()),
        )
    }
}

/** A low sun sitting on a horizon line, with short fading reflections below it. */
private fun DrawScope.drawHorizon(upper: Color, lower: Color, line: Color) {
    val w = size.width
    val y = size.height * 0.64f
    val r = w * 0.36f
    val centre = Offset(w / 2f, y)
    clipRect(left = 0f, top = 0f, right = w, bottom = y) {
        drawCircle(Brush.verticalGradient(listOf(lower.copy(alpha = 0.05f), lower.copy(alpha = 0.55f)), y - r, y), r, centre)
        drawCircle(upper, r, centre, style = Stroke(1.5.dp.toPx()))
    }
    drawLine(line.copy(alpha = 0.25f), Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
    listOf(0.5f to 0.5f, 0.4f to 0.35f, 0.3f to 0.2f, 0.2f to 0.1f).forEachIndexed { i, (share, alpha) ->
        val ly = y + 14.dp.toPx() * (i + 1)
        drawLine(lower.copy(alpha = alpha), Offset(w * (0.5f - share / 2f), ly), Offset(w * (0.5f + share / 2f), ly), strokeWidth = 1.dp.toPx())
    }
}

/** Four thin sine lines near the bottom; one uses the accent colour. */
private fun DrawScope.drawWaves(upper: Color, accent: Color) {
    val w = size.width
    val amplitude = 10.dp.toPx()
    val wavelength = w / 2f
    val step = 6.dp.toPx()
    for (i in 0 until 4) {
        val baseY = size.height * (0.72f + 0.05f * i)
        val phase = i * 0.6f
        val path = Path().apply {
            moveTo(0f, baseY + amplitude * sin(phase))
            var x = step
            while (x <= w + step) {
                lineTo(x, baseY + amplitude * sin((2.0 * PI * x / wavelength + phase).toFloat()))
                x += step
            }
        }
        val isAccent = i == 1
        drawPath(path, if (isAccent) accent else upper, alpha = if (isAccent) 0.8f else 0.5f - i * 0.12f, style = Stroke(1.dp.toPx()))
    }
}
