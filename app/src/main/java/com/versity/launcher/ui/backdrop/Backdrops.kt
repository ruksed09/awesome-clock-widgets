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
        Backdrop.STARS -> drawStars(upper, Color(palette.onSurface))
        Backdrop.AURORA -> drawAurora(upper, lower)
        Backdrop.DUNES -> drawDunes(upper, lower)
        Backdrop.PEAKS -> drawPeaks(upper, lower)
        Backdrop.ECLIPSE -> drawEclipse(upper, Color(palette.backgroundBottom))
        Backdrop.BOKEH -> drawBokeh(upper, lower)
        Backdrop.GRID -> drawGrid(upper, lower, Color(palette.backgroundBottom))
        Backdrop.ARCS -> drawArcs(upper, lower, accent)
        Backdrop.RAIN -> drawRain(upper)
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

/**
 * Soft diagonal light beam from the top-left, a scattered starfield and a few four-point sparkles.
 * Positions come from a fixed seed, so the sky never shuffles between frames or screens.
 */
private fun DrawScope.drawStars(beam: Color, star: Color) {
    val w = size.width
    val h = size.height
    val diag = Brush.linearGradient(
        0f to beam.copy(alpha = 0.32f), 0.35f to beam.copy(alpha = 0.08f), 0.6f to Color.Transparent,
        start = Offset(0f, 0f), end = Offset(w * 0.9f, h * 0.55f),
    )
    drawRect(diag)
    val glow = Brush.linearGradient(
        0.5f to Color.Transparent, 1f to beam.copy(alpha = 0.18f),
        start = Offset(0f, 0f), end = Offset(w, h),
    )
    drawRect(glow)
    val rnd = java.util.Random(1111L)
    val dot = 1.dp.toPx()
    repeat(90) {
        val x = rnd.nextFloat() * w
        val y = rnd.nextFloat() * h
        drawCircle(star.copy(alpha = 0.25f + rnd.nextFloat() * 0.6f), dot * (0.4f + rnd.nextFloat() * 0.9f), Offset(x, y))
    }
    val unit = min(w, h)
    listOf(0.14f to 0.42f, 0.82f to 0.33f, 0.68f to 0.58f, 0.2f to 0.72f, 0.88f to 0.8f).forEachIndexed { i, (fx, fy) ->
        drawStarPoint(Offset(w * fx, h * fy), unit * (if (i == 2) 0.028f else 0.018f), star)
    }
}

private fun DrawScope.drawStarPoint(c: Offset, r: Float, ink: Color) {
    drawCircle(Brush.radialGradient(listOf(ink.copy(alpha = 0.3f), Color.Transparent), c, r * 1.5f), r * 1.5f, c)
    val k = r * 0.14f
    val path = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x + k, c.y - k, c.x + r, c.y)
        quadraticTo(c.x + k, c.y + k, c.x, c.y + r)
        quadraticTo(c.x - k, c.y + k, c.x - r, c.y)
        quadraticTo(c.x - k, c.y - k, c.x, c.y - r)
        close()
    }
    drawPath(path, ink.copy(alpha = 0.9f))
}

/** Three soft curtains of light across the upper sky, alternating the two rim colours. */
private fun DrawScope.drawAurora(upper: Color, lower: Color) {
    val w = size.width
    val h = size.height
    val steps = 32
    listOf(Triple(0.10f, upper, 0.0f), Triple(0.18f, lower, 1.7f), Triple(0.26f, upper, 3.1f)).forEachIndexed { i, (top, color, phase) ->
        val bandH = h * (0.22f - i * 0.03f)
        val amp = h * 0.035f
        val path = Path()
        for (k in 0..steps) {
            val x = w * k / steps
            val y = h * top + amp * sin(x / w * 2f * PI.toFloat() * 1.3f + phase)
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        for (k in steps downTo 0) {
            val x = w * k / steps
            val y = h * top + bandH + amp * 0.6f * sin(x / w * 2f * PI.toFloat() * 1.3f + phase + 0.8f)
            path.lineTo(x, y)
        }
        path.close()
        val yTop = h * top - amp
        drawPath(path, Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = 0.30f), color.copy(alpha = 0.05f)), yTop, yTop + bandH + amp * 2))
    }
}

/** Layered dunes rising from the bottom with a low sun behind them. */
private fun DrawScope.drawDunes(upper: Color, lower: Color) {
    val w = size.width
    val h = size.height
    val sun = Offset(w * 0.68f, h * 0.58f)
    val sunR = min(w, h) * 0.16f
    drawCircle(Brush.radialGradient(listOf(upper.copy(alpha = 0.35f), Color.Transparent), sun, sunR * 2.2f), sunR * 2.2f, sun)
    drawCircle(upper.copy(alpha = 0.22f), sunR, sun)
    listOf(0.64f to 0.10f, 0.72f to 0.16f, 0.80f to 0.24f, 0.89f to 0.34f).forEachIndexed { i, (y0, alpha) ->
        val color = if (i % 2 == 0) lower else upper
        val path = Path().apply {
            moveTo(0f, h * y0)
            cubicTo(w * 0.25f, h * (y0 - 0.06f + i * 0.01f), w * 0.55f, h * (y0 + 0.05f), w, h * (y0 - 0.03f))
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path, Brush.verticalGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.4f)), h * (y0 - 0.06f), h))
    }
}

/** Two mountain ranges with a fine ridge line on the front range. */
private fun DrawScope.drawPeaks(upper: Color, lower: Color) {
    val w = size.width
    val h = size.height
    fun range(points: List<Pair<Float, Float>>): Path = Path().apply {
        moveTo(0f, h)
        points.forEach { (fx, fy) -> lineTo(w * fx, h * fy) }
        lineTo(w, h)
        close()
    }
    val back = listOf(0f to 0.66f, 0.18f to 0.55f, 0.34f to 0.63f, 0.52f to 0.48f, 0.7f to 0.6f, 0.86f to 0.52f, 1f to 0.6f)
    val front = listOf(0f to 0.78f, 0.14f to 0.7f, 0.3f to 0.76f, 0.46f to 0.64f, 0.62f to 0.74f, 0.8f to 0.67f, 1f to 0.76f)
    drawPath(range(back), Brush.verticalGradient(listOf(upper.copy(alpha = 0.22f), upper.copy(alpha = 0.04f)), h * 0.48f, h))
    drawPath(range(front), Brush.verticalGradient(listOf(lower.copy(alpha = 0.30f), lower.copy(alpha = 0.08f)), h * 0.64f, h))
    val ridge = Path().apply { front.forEachIndexed { i, (fx, fy) -> if (i == 0) moveTo(w * fx, h * fy) else lineTo(w * fx, h * fy) } }
    drawPath(ridge, upper.copy(alpha = 0.55f), style = Stroke(1.dp.toPx()))
}

/** A dark disc with a glowing corona, in the empty middle of the screen. */
private fun DrawScope.drawEclipse(corona: Color, disc: Color) {
    val c = Offset(size.width * 0.5f, size.height * 0.56f)
    val r = min(size.width, size.height) * 0.26f
    drawCircle(
        Brush.radialGradient(
            0f to Color.Transparent, 0.62f to Color.Transparent, 0.66f to corona.copy(alpha = 0.55f),
            0.75f to corona.copy(alpha = 0.18f), 1f to Color.Transparent,
            center = c, radius = r * 1.55f,
        ),
        r * 1.55f, c,
    )
    drawCircle(disc, r, c)
    drawCircle(
        Brush.linearGradient(listOf(corona.copy(alpha = 0.9f), corona.copy(alpha = 0.15f)), c - Offset(r, r), c + Offset(r, r)),
        r, c, style = Stroke(1.5.dp.toPx()),
    )
}

/** Out-of-focus light: soft discs at fixed seeded positions. */
private fun DrawScope.drawBokeh(upper: Color, lower: Color) {
    val rnd = java.util.Random(7L)
    val unit = min(size.width, size.height)
    repeat(16) { i ->
        val c = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
        val r = unit * (0.04f + rnd.nextFloat() * 0.12f)
        val color = if (i % 3 == 0) lower else upper
        val a = 0.06f + rnd.nextFloat() * 0.14f
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = a), color.copy(alpha = a * 0.6f), Color.Transparent), c, r), r, c)
    }
}

/** Retro sun cut by bands, over a perspective grid floor. */
private fun DrawScope.drawGrid(sun: Color, line: Color, bg: Color) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.6f
    val r = min(w, h) * 0.24f
    val c = Offset(w / 2f, horizon)
    clipRect(bottom = horizon) {
        drawCircle(Brush.verticalGradient(listOf(sun.copy(alpha = 0.85f), line.copy(alpha = 0.7f)), c.y - r, c.y), r, c)
        val band = r * 0.06f
        listOf(0.22f, 0.4f, 0.56f, 0.7f, 0.82f).forEachIndexed { i, f ->
            val y = c.y - r * (1f - f)
            drawRect(bg, Offset(c.x - r, y), androidx.compose.ui.geometry.Size(r * 2, band * (1f + i * 0.35f)))
        }
    }
    drawRect(Brush.verticalGradient(listOf(line.copy(alpha = 0.16f), Color.Transparent), horizon, h), Offset(0f, horizon))
    val stroke = 1.dp.toPx()
    var y = horizon
    var gap = h * 0.012f
    while (y < h) {
        drawLine(line.copy(alpha = 0.5f), Offset(0f, y), Offset(w, y), stroke)
        y += gap
        gap *= 1.35f
    }
    for (k in -8..8) {
        val xBottom = w / 2f + k * w * 0.16f
        drawLine(line.copy(alpha = 0.45f), Offset(w / 2f + k * w * 0.02f, horizon), Offset(xBottom, h), stroke)
    }
}

/** Bauhaus-style geometry: a filled quarter circle, an open half ring and a small solid disc. */
private fun DrawScope.drawArcs(upper: Color, lower: Color, accent: Color) {
    val w = size.width
    val h = size.height
    val unit = min(w, h)
    val stroke = 1.dp.toPx()
    drawCircle(lower.copy(alpha = 0.32f), unit * 0.5f, Offset(0f, h))
    drawCircle(upper.copy(alpha = 0.5f), unit * 0.62f, Offset(0f, h), style = Stroke(stroke))
    drawCircle(upper.copy(alpha = 0.3f), unit * 0.74f, Offset(0f, h), style = Stroke(stroke))
    drawArc(upper.copy(alpha = 0.6f), 90f, 180f, false, Offset(w - unit * 0.22f, h * 0.36f), androidx.compose.ui.geometry.Size(unit * 0.44f, unit * 0.44f), style = Stroke(stroke * 1.5f))
    drawCircle(accent.copy(alpha = 0.75f), unit * 0.05f, Offset(w * 0.78f, h * 0.7f))
}

/** Fine slanting streaks that fade at both ends. */
private fun DrawScope.drawRain(color: Color) {
    val rnd = java.util.Random(42L)
    val w = size.width
    val h = size.height
    val stroke = 1.dp.toPx()
    repeat(70) {
        val x = rnd.nextFloat() * (w * 1.2f)
        val y = rnd.nextFloat() * h
        val len = h * (0.03f + rnd.nextFloat() * 0.09f)
        val start = Offset(x, y)
        val end = Offset(x - len * 0.27f, y + len)
        val a = 0.10f + rnd.nextFloat() * 0.30f
        drawLine(Brush.linearGradient(listOf(Color.Transparent, color.copy(alpha = a), Color.Transparent), start, end), start, end, stroke)
    }
}
