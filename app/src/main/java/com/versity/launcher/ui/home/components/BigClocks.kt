package com.versity.launcher.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.theme.LauncherTheme
import com.versity.launcher.ui.theme.LiningFigures
import java.time.LocalTime
import kotlin.math.min

/** The big-clock family: large, glanceable faces that fill the width. */
val BigClockStyles = setOf(
    ClockStyle.GIANT, ClockStyle.OUTLINE, ClockStyle.NEON, ClockStyle.EXTRUDED, ClockStyle.SEGMENT, ClockStyle.FLIP,
    ClockStyle.STAGE, ClockStyle.GLASS,
)

/**
 * Draws one of the big-clock styles. Sizes come from the available width (capped by [maxSize]),
 * so the same face works in a small preview, on a phone and on a tablet.
 */
@Composable
fun BigClock(
    style: ClockStyle,
    time: LocalTime,
    is24Hour: Boolean,
    modifier: Modifier = Modifier,
    centered: Boolean = true,
    maxSize: Dp = 420.dp,
    date: java.time.LocalDate? = null,
) {
    val h = hourText(time, is24Hour)
    val m = minuteText(time)
    BoxWithConstraints(
        modifier.testTag(TestTags.clockFace(style.name)).semantics { contentDescription = "$h:$m" },
        contentAlignment = if (centered) Alignment.TopCenter else Alignment.TopStart,
    ) {
        val w = min(maxWidth.value, maxSize.value)
        when (style) {
            ClockStyle.GIANT -> GiantClock(h, m, w, centered)
            ClockStyle.OUTLINE -> OutlineClock(h, m, w, centered)
            ClockStyle.NEON -> NeonClock(h, m, w)
            ClockStyle.EXTRUDED -> ExtrudedClock(h, m, w)
            ClockStyle.SEGMENT -> SegmentClock(h, m, w)
            ClockStyle.FLIP -> FlipClock(h, m, w)
            ClockStyle.STAGE -> StageClock(h, m, w, date, centered)
            ClockStyle.GLASS -> GlassClock(h, m, w)
            else -> Unit
        }
    }
}

private fun digitStyle(size: Float, weight: FontWeight) = TextStyle(
    fontSize = size.sp,
    lineHeight = (size * 0.86f).sp,
    fontWeight = weight,
    letterSpacing = (-size * 0.035f).sp,
    fontFeatureSettings = LiningFigures,
)

/**
 * Outline-only digits. Variable fonts draw glyphs from overlapping contours, which a plain stroke
 * shows as stray inner lines; the glyph outlines are merged with a path union first, then stroked.
 * [glow] adds a blurred halo of the same path behind it (neon).
 */
@Composable
private fun GlyphOutline(
    text: String,
    sizeSp: Float,
    weight: FontWeight,
    color: Color,
    strokeDp: Float,
    glow: Color? = null,
    core: Color? = null,
    fill: List<Color>? = null,
    shadow: Color? = null,
) {
    val density = LocalDensity.current
    val resolver = LocalFontFamilyResolver.current
    val family = LauncherTheme.font
    val typeface = remember(family, weight) { resolver.resolve(family, weight).value as? android.graphics.Typeface }
    val px = with(density) { sizeSp.sp.toPx() }
    val strokePx = with(density) { strokeDp.dp.toPx() }
    val pad = strokePx * 4 + (if (glow != null) px * 0.12f else 0f)
    val shape = remember(text, typeface, px) {
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = px
            this.typeface = typeface
            fontFeatureSettings = LiningFigures
        }
        val raw = android.graphics.Path()
        val fm = paint.fontMetrics
        paint.getTextPath(text, 0, text.length, 0f, -fm.ascent, raw)
        val merged = android.graphics.Path().apply { op(raw, android.graphics.Path(), android.graphics.Path.Op.UNION) }
        val bounds = android.graphics.RectF().also { merged.computeBounds(it, true) }
        Triple(merged, paint.measureText(text), bounds)
    }
    val (path, advance, bounds) = shape
    val w = with(density) { (advance + pad * 2).toDp() }
    val h = with(density) { (bounds.bottom + pad * 2).toDp() }
    Canvas(Modifier.size(w, h)) {
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.translate(pad, pad)
            fun stroke(c: Color, width: Float, blur: Float = 0f) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = width
                strokeJoin = android.graphics.Paint.Join.ROUND
                this.color = c.toArgb()
                if (blur > 0f) maskFilter = android.graphics.BlurMaskFilter(blur, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
            if (shadow != null) {
                native.save(); native.translate(0f, px * 0.04f)
                native.drawPath(path, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = shadow.toArgb()
                    maskFilter = android.graphics.BlurMaskFilter(px * 0.06f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                })
                native.restore()
            }
            if (fill != null) native.drawPath(path, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.LinearGradient(0f, bounds.top, 0f, bounds.bottom, fill.map { it.toArgb() }.toIntArray(), null, android.graphics.Shader.TileMode.CLAMP)
            })
            if (glow != null) native.drawPath(path, stroke(glow, strokePx * 3f, px * 0.08f))
            native.drawPath(path, stroke(color, strokePx))
            if (core != null) native.drawPath(path, stroke(core, strokePx * 0.35f))
            native.restore()
        }
    }
}

/** Hours over minutes, very large and heavy, with a soft cast shadow for depth. */
@Composable
private fun GiantClock(h: String, m: String, width: Float, centered: Boolean) {
    val c = LauncherTheme.colors
    val size = width * 0.5f
    val base = digitStyle(size, FontWeight.Bold).copy(
        fontFamily = LauncherTheme.font,
        shadow = Shadow(Color.Black.copy(alpha = 0.28f), Offset(0f, size * 0.05f), size * 0.12f),
    )
    Column(horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        Text(h, style = base.copy(brush = Brush.verticalGradient(listOf(c.onSurface, c.onSurface.copy(alpha = 0.82f)))), maxLines = 1)
        Text(m, style = base.copy(brush = Brush.verticalGradient(listOf(c.accent, lerp(c.accent, c.onSurface, 0.3f)))), maxLines = 1)
    }
}

/** Big hollow digits: a clean stroke, so the wallpaper shows through. */
@Composable
private fun OutlineClock(h: String, m: String, width: Float, centered: Boolean) {
    val c = LauncherTheme.colors
    val size = width * 0.5f
    Column(
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy((-size * 0.12f).dp),
    ) {
        GlyphOutline(h, size, FontWeight.Bold, c.onSurface, size * 0.018f)
        GlyphOutline(m, size, FontWeight.Bold, c.accent, size * 0.018f)
    }
}

/** Glowing tube digits on one line: a bright core over two widening halos. */
@Composable
private fun NeonClock(h: String, m: String, width: Float) {
    val c = LauncherTheme.colors
    val size = width * 0.3f
    // A blurred halo, the coloured tube, then a white-hot core.
    GlyphOutline("$h:$m", size, FontWeight.Medium, c.accent, size * 0.03f, glow = c.accent.copy(alpha = 0.8f), core = Color.White.copy(alpha = 0.9f))
}

/** Extruded digits: stacked offset copies in a deeper tone, then a lit front face. */
@Composable
private fun ExtrudedClock(h: String, m: String, width: Float) {
    val c = LauncherTheme.colors
    val size = width * 0.27f
    val text = "$h:$m"
    val base = digitStyle(size, FontWeight.Black).copy(fontFamily = LauncherTheme.font, textAlign = TextAlign.Center)
    val depth = 10
    val step = (size * 0.011f).dp
    val side = lerp(c.accent, Color.Black, 0.45f)
    Box(contentAlignment = Alignment.Center) {
        for (i in depth downTo 1) {
            Text(
                text,
                style = base.copy(color = lerp(side, Color.Black, i / depth.toFloat() * 0.35f)),
                maxLines = 1,
                modifier = Modifier.offset(x = step * i, y = step * i),
            )
        }
        Text(text, style = base.copy(brush = Brush.verticalGradient(listOf(Color.White, lerp(c.accent, Color.White, 0.55f)))), maxLines = 1)
    }
}

private val StageDate = java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM")

/** Lock-screen style: the date in small semibold above heavy, rounded, tightly set numerals. */
@Composable
private fun StageClock(h: String, m: String, width: Float, date: java.time.LocalDate?, centered: Boolean) {
    val c = LauncherTheme.colors
    val size = width * 0.36f
    Column(horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        if (date != null) {
            Text(
                date.format(StageDate),
                style = TextStyle(fontFamily = LauncherTheme.font, fontSize = (size * 0.16f).sp, fontWeight = FontWeight.SemiBold, color = c.onSurface.copy(alpha = 0.9f), fontFeatureSettings = LiningFigures),
                maxLines = 1,
            )
        }
        Text(
            "$h:$m",
            style = digitStyle(size, FontWeight.SemiBold).copy(
                fontFamily = LauncherTheme.font,
                brush = Brush.verticalGradient(listOf(c.onSurface, c.onSurface.copy(alpha = 0.82f))),
                shadow = Shadow(Color.Black.copy(alpha = 0.18f), Offset(0f, size * 0.03f), size * 0.1f),
                lineHeight = (size * 1.02f).sp,
            ),
            maxLines = 1,
        )
    }
}

/** Frosted numerals: a translucent white fill that fades downwards, a lit rim and a soft shadow. */
@Composable
private fun GlassClock(h: String, m: String, width: Float) {
    val size = width * 0.32f
    GlyphOutline(
        "$h:$m", size, FontWeight.Bold, Color.White.copy(alpha = 0.75f), size * 0.008f,
        fill = listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.16f)),
        shadow = Color.Black.copy(alpha = 0.22f),
    )
}

// ---------------------------------------------------------------- Seven-segment

/** Segments lit for each digit, in order a b c d e f g. */
private val SegmentsFor = mapOf(
    '0' to "abcdef", '1' to "bc", '2' to "abdeg", '3' to "abcdg", '4' to "bcfg",
    '5' to "acdfg", '6' to "acdefg", '7' to "abc", '8' to "abcdefg", '9' to "abcdfg",
)

/** Custom-drawn LED digits, slanted slightly, with the unlit segments faintly visible. */
@Composable
private fun SegmentClock(h: String, m: String, width: Float) {
    val c = LauncherTheme.colors
    val digitW = width * 0.17f
    val digitH = digitW * 1.8f
    val gap = width * 0.03f
    Row(horizontalArrangement = Arrangement.spacedBy(gap.dp), verticalAlignment = Alignment.CenterVertically) {
        (h + m).forEachIndexed { i, ch ->
            if (i == 2) {
                Canvas(Modifier.size(width = (digitW * 0.18f).dp, height = digitH.dp)) {
                    val r = size.width * 0.42f
                    listOf(0.34f, 0.66f).forEach { fy -> drawCircle(c.accent, r, Offset(size.width / 2, size.height * fy)) }
                }
            }
            Canvas(Modifier.size(width = digitW.dp, height = digitH.dp)) { drawSegments(SegmentsFor[ch].orEmpty(), c.accent) }
        }
    }
}

private fun DrawScope.drawSegments(lit: String, on: Color) {
    val w = size.width
    val h = size.height
    val t = w * 0.17f            // segment thickness
    val hh = t / 2
    val slant = w * 0.08f        // italic lean: the top shifts right
    fun pt(path: Path, px: Float, py: Float, first: Boolean = false) {
        val sx = px + slant * (1 - py / h)
        if (first) path.moveTo(sx, py) else path.lineTo(sx, py)
    }
    /** Hexagonal bar along y = [y] from [x0] to [x1]. */
    fun horizontal(x0: Float, x1: Float, y: Float) = Path().apply {
        pt(this, x0, y, true); pt(this, x0 + hh, y - hh); pt(this, x1 - hh, y - hh)
        pt(this, x1, y); pt(this, x1 - hh, y + hh); pt(this, x0 + hh, y + hh); close()
    }
    /** Hexagonal bar along x = [x] from [y0] to [y1]. */
    fun vertical(x: Float, y0: Float, y1: Float) = Path().apply {
        pt(this, x, y0, true); pt(this, x + hh, y0 + hh); pt(this, x + hh, y1 - hh)
        pt(this, x, y1); pt(this, x - hh, y1 - hh); pt(this, x - hh, y0 + hh); close()
    }
    val l = hh + slant * 0.1f
    val r = w - slant - hh
    val top = hh
    val mid = h / 2
    val bot = h - hh
    val g = t * 0.14f
    val segments = mapOf(
        'a' to horizontal(l + g, r - g, top),
        'g' to horizontal(l + g, r - g, mid),
        'd' to horizontal(l + g, r - g, bot),
        'f' to vertical(l, top + g, mid - g),
        'b' to vertical(r, top + g, mid - g),
        'e' to vertical(l, mid + g, bot - g),
        'c' to vertical(r, mid + g, bot - g),
    )
    segments.forEach { (key, path) ->
        val isOn = key in lit
        if (isOn) drawPath(path, on.copy(alpha = 0.35f), style = Stroke(t * 0.45f))
        drawPath(path, if (isOn) on else on.copy(alpha = 0.09f))
    }
}

// ---------------------------------------------------------------- Flip cards

/** Split-flap cards: two soft clay tiles, a hinge line across the middle, and big centred digits. */
@Composable
private fun FlipClock(h: String, m: String, width: Float) {
    val c = LauncherTheme.colors
    val cardW = width * 0.44f
    val cardH = cardW * 0.92f
    val size = cardW * 0.62f
    val shape = RoundedCornerShape((cardW * 0.1f).dp)
    val face = lerp(c.surface, Color.Black, 0.25f)
    val faceTop = lerp(face, Color.White, 0.08f)
    Row(horizontalArrangement = Arrangement.spacedBy((width * 0.04f).dp)) {
        listOf(h, m).forEach { pair ->
            Box(
                Modifier
                    .size(width = cardW.dp, height = cardH.dp)
                    .shadow((cardW * 0.06f).dp, shape, clip = false)
                    .background(Brush.verticalGradient(listOf(faceTop, face)), shape)
                    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)), shape)
                    .drawWithContent {
                        drawContent()
                        val area = this.size
                        val y = area.height / 2
                        val fold = area.height * 0.12f
                        // Hinge: a dark seam, then a short shadow on the lower flap.
                        drawRect(Color.Black.copy(alpha = 0.45f), Offset(0f, y - 1.dp.toPx()), androidx.compose.ui.geometry.Size(area.width, 2.dp.toPx()))
                        drawRect(
                            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent), y, y + fold),
                            Offset(0f, y), androidx.compose.ui.geometry.Size(area.width, fold),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(pair, style = digitStyle(size, FontWeight.SemiBold).copy(fontFamily = LauncherTheme.font, color = c.onSurface, lineHeight = size.sp), maxLines = 1)
            }
        }
    }
}
