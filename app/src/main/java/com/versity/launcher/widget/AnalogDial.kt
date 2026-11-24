package com.versity.launcher.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.versity.launcher.domain.ThemePresets
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws the analogue widget's dial: the face (round or rounded square, in the panel colour and
 * opacity), the marks and, for numeral dials, the numbers in the chosen typeface. A bitmap works in
 * widgets on every Android version, so the dial is fully customisable even where the hands are not.
 * Geometry matches the hands drawables: a 200-unit dial with the centre at 100.
 */
object AnalogDial {
    private val Roman = listOf("XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI")

    fun render(context: Context, spec: WidgetSpec, sizePx: Int): Bitmap {
        val size = sizePx.coerceIn(120, 720)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val u = size / 200f
        val c = size / 2f
        val ink = spec.textColor.toInt()
        fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha shl 24)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (spec.dialFace) {
            paint.style = Paint.Style.FILL
            paint.color = withAlpha(spec.backgroundColor.toInt(), spec.opacity.coerceIn(0, 100) * 255 / 100)
            when (spec.faceShape) {
                FaceShape.ROUND -> canvas.drawCircle(c, c, 99f * u, paint)
                FaceShape.SQUARE -> canvas.drawRoundRect(RectF(1f * u, 1f * u, 199f * u, 199f * u), 46f * u, 46f * u, paint)
            }
        }
        // Marks sit on a circle for round faces and follow the outline for square ones.
        fun radiusAt(angle: Double, r: Float): Float {
            if (spec.faceShape == FaceShape.ROUND) return r
            val s = kotlin.math.abs(sin(angle)).coerceAtLeast(1e-6)
            val k = kotlin.math.abs(cos(angle)).coerceAtLeast(1e-6)
            // Blend between a circle and a square outline so corners reach out gently.
            val square = r / maxOf(s, k).toFloat()
            return r + (square - r) * 0.55f
        }
        fun point(index: Int, steps: Int, r: Float): Pair<Float, Float> {
            val a = Math.toRadians(index * 360.0 / steps - 90.0)
            val rr = radiusAt(a, r) * u
            return (c + cos(a).toFloat() * rr) to (c + sin(a).toFloat() * rr)
        }
        fun tick(i: Int, steps: Int, from: Float, to: Float, width: Float, color: Int) {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = width * u
            paint.color = color
            val (x1, y1) = point(i, steps, from)
            val (x2, y2) = point(i, steps, to)
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
        fun dot(i: Int, steps: Int, r: Float, radius: Float, color: Int) {
            paint.style = Paint.Style.FILL
            paint.color = color
            val (x, y) = point(i, steps, r)
            canvas.drawCircle(x, y, radius * u, paint)
        }
        val minor = withAlpha(ink, 0x70)

        when (spec.style) {
            WidgetStyle.DIAL_INDEX -> repeat(60) { i ->
                if (i % 5 == 0) tick(i, 60, 83f, 93f, 2.6f, ink) else tick(i, 60, 88f, 93f, 1.1f, minor)
            }
            WidgetStyle.DIAL_MINIMAL -> repeat(4) { i -> tick(i, 4, 80f, 93f, 3f, ink) }
            WidgetStyle.DIAL_DOTS -> repeat(12) { i -> dot(i, 12, 88f, if (i % 3 == 0) 3.4f else 2.1f, ink) }
            WidgetStyle.DIAL_BARS -> repeat(12) { i -> tick(i, 12, if (i % 3 == 0) 76f else 80f, 93f, if (i % 3 == 0) 5f else 3.2f, ink) }
            WidgetStyle.DIAL_QUARTERS -> {
                repeat(60) { i -> if (i % 15 != 0) tick(i, 60, if (i % 5 == 0) 86f else 89f, 93f, if (i % 5 == 0) 1.8f else 0.9f, if (i % 5 == 0) ink else minor) }
                numerals(context, canvas, spec, listOf(0, 3, 6, 9), u, c, 76f, 24f, ::radiusAt) { (if (it == 0) 12 else it).toString() }
            }
            WidgetStyle.DIAL_NUMBERS -> {
                repeat(60) { i -> if (i % 5 != 0) tick(i, 60, 90f, 93f, 0.9f, minor) }
                numerals(context, canvas, spec, (0 until 12).toList(), u, c, 78f, 17f, ::radiusAt) { (if (it == 0) 12 else it).toString() }
            }
            WidgetStyle.DIAL_ROMAN -> {
                repeat(60) { i -> tick(i, 60, if (i % 5 == 0) 87f else 90f, 93f, if (i % 5 == 0) 1.6f else 0.8f, if (i % 5 == 0) ink else minor) }
                numerals(context, canvas, spec, (0 until 12).toList(), u, c, 74f, 14f, ::radiusAt) { Roman[it] }
            }
            else -> Unit
        }
        return bitmap
    }

    private fun numerals(
        context: Context,
        canvas: Canvas,
        spec: WidgetSpec,
        hours: List<Int>,
        u: Float,
        c: Float,
        radius: Float,
        textSize: Float,
        radiusAt: (Double, Float) -> Float,
        label: (Int) -> String,
    ) {
        val tf = WidgetTypefaces.of(spec.typeface)
        val base: Typeface = runCatching { ResourcesCompat.getFont(context, ThemePresets.font(tf.fontId).resId) }.getOrNull() ?: Typeface.DEFAULT
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = base
            // Numerals are small next to the time widgets' digits, so give thin faces a little weight.
            fontVariationSettings = "'wght' ${tf.weight.coerceAtLeast(350)}"
            fontFeatureSettings = "lnum, tnum"
            color = spec.textColor.toInt()
            this.textSize = textSize * u
            textAlign = Paint.Align.CENTER
        }
        val fm = paint.fontMetrics
        val centre = -(fm.ascent + fm.descent) / 2f
        for (h in hours) {
            val a = Math.toRadians(h * 30.0 - 90.0)
            val r = radiusAt(a, radius) * u
            canvas.drawText(label(h), c + cos(a).toFloat() * r, c + sin(a).toFloat() * r + centre, paint)
        }
    }
}
