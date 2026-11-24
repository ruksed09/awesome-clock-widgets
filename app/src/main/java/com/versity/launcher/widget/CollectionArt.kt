package com.versity.launcher.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.net.Uri
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Draws the collection's image widgets: the radial launcher, the app shortcut and the badge. Widgets can
 * only show images and text, so these shapes are rendered here (in software, once per change) and the
 * widget lays tap targets over them. Dark, layered and quiet, after the reference design.
 */
object CollectionArt {
    /** Launcher slots in order: top, left, centre, right, bottom. */
    val Slots = listOf(0.5f to 1 / 6f, 1 / 6f to 0.5f, 0.5f to 0.5f, 5 / 6f to 0.5f, 0.5f to 5 / 6f)
    const val SLOT_COUNT = 5

    fun component(key: String): ComponentName? {
        val name = key.substringBefore('#')
        val pkg = name.substringBefore('/', "")
        val cls = name.substringAfter('/', "")
        return if (pkg.isBlank() || cls.isBlank()) null else ComponentName(pkg, cls)
    }

    /** An intent that opens the app behind [key], or null when it is no longer installed. */
    fun launchIntent(context: Context, key: String): Intent? {
        val c = component(key) ?: return null
        val exists = runCatching { context.packageManager.getActivityInfo(c, 0) }.isSuccess
        val intent = if (exists) Intent.makeMainActivity(c) else context.packageManager.getLaunchIntentForPackage(c.packageName)
        return intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }

    fun label(context: Context, key: String): String? = component(key)?.let { c ->
        runCatching { context.packageManager.getActivityInfo(c, 0).loadLabel(context.packageManager).toString() }.getOrNull()
    }

    /** Where app icons come from; tests swap in stand-ins. */
    @androidx.annotation.VisibleForTesting
    internal var iconSource: (Context, String) -> Drawable? = ::systemIcon

    private fun icon(context: Context, key: String): Drawable? = iconSource(context, key)

    private fun systemIcon(context: Context, key: String): Drawable? = component(key)?.let { c ->
        runCatching { context.packageManager.getActivityIcon(c) }.getOrNull()
            ?: runCatching { context.packageManager.getApplicationIcon(c.packageName) }.getOrNull()
    }

    private fun shadowed(paint: Paint, on: Boolean, r: Float) {
        if (on) paint.setShadowLayer(r * 0.09f, 0f, r * 0.05f, 0x99000000.toInt()) else paint.clearShadowLayer()
    }

    // ------------------------------------------------------------ Radial launcher

    fun radial(context: Context, spec: WidgetSpec, sizePx: Int): Bitmap {
        val size = sizePx.coerceIn(150, 720)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cell = size / 3f
        // Circles shrink as spacing grows, leaving the silhouette visible between them.
        val r = cell / 2f * (0.98f - spec.spacing / 100f)
        val centres = Slots.map { (x, y) -> x * size to y * size }
        if (spec.style == WidgetStyle.RADIAL_FLOWER) {
            val silhouette = flowerMask(size, centres, r)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = spec.backgroundColor.toInt(); alpha = spec.opacity * 255 / 100 }
            shadowed(paint, spec.shadow, r)
            canvas.drawBitmap(silhouette, 0f, 0f, paint)
        }
        centres.forEachIndexed { i, (cx, cy) ->
            lobe(context, canvas, cx, cy, r, spec, spec.apps.getOrNull(i), disc = spec.style == WidgetStyle.RADIAL_RINGS)
        }
        return bitmap
    }

    /**
     * The unified, organic outline: five circles and the cross between them, blurred and cut back so the
     * joins curve like one shape rather than meeting at corners.
     */
    private fun flowerMask(size: Int, centres: List<Pair<Float, Float>>, r: Float): Bitmap {
        val small = (size / 2).coerceAtLeast(60)
        val k = small / size.toFloat()
        val mask = Bitmap.createBitmap(small, small, Bitmap.Config.ARGB_8888)
        val c = Canvas(mask)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        centres.forEach { (x, y) -> c.drawCircle(x * k, y * k, r * k * 1.02f, p) }
        val bar = r * k * 1.15f
        c.drawRect(size * k / 2 - bar, centres[0].second * k, size * k / 2 + bar, centres[4].second * k, p)
        c.drawRect(centres[1].first * k, size * k / 2 - bar, centres[3].first * k, size * k / 2 + bar, p)
        val px = IntArray(small * small)
        mask.getPixels(px, 0, small, 0, 0, small, small)
        val alpha = IntArray(px.size) { (px[it] ushr 24) and 0xFF }
        val radius = (r * k * 0.28f).roundToInt().coerceAtLeast(2)
        repeat(3) { boxBlur(alpha, small, radius) }
        for (i in px.indices) {
            // Cut back at half strength with a soft edge: concave corners become smooth curves.
            val a = ((alpha[i] - 128) * 6 + 128).coerceIn(0, 255)
            px[i] = (a shl 24) or 0xFFFFFF
        }
        mask.setPixels(px, 0, small, 0, 0, small, small)
        // An alpha-only mask, so drawing it takes the paint's colour, opacity and shadow.
        return Bitmap.createScaledBitmap(mask, size, size, true).extractAlpha()
    }

    private fun boxBlur(a: IntArray, n: Int, r: Int) {
        val tmp = IntArray(a.size)
        val w = 2 * r + 1
        for (y in 0 until n) {
            var sum = 0
            for (x in -r..r) sum += a[y * n + x.coerceIn(0, n - 1)]
            for (x in 0 until n) {
                tmp[y * n + x] = sum / w
                sum += a[y * n + (x + r + 1).coerceAtMost(n - 1)] - a[y * n + (x - r).coerceAtLeast(0)]
            }
        }
        for (x in 0 until n) {
            var sum = 0
            for (y in -r..r) sum += tmp[y.coerceIn(0, n - 1) * n + x]
            for (y in 0 until n) {
                a[y * n + x] = sum / w
                sum += tmp[(y + r + 1).coerceAtMost(n - 1) * n + x] - tmp[(y - r).coerceAtLeast(0) * n + x]
            }
        }
    }

    /**
     * One circle: an optional disc (for separate rings), concentric rings lit softly from above, a
     * recessed centre and the app icon, or a quiet "+" when the slot is empty.
     */
    private fun lobe(context: Context, canvas: Canvas, cx: Float, cy: Float, r: Float, spec: WidgetSpec, app: String?, disc: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (disc) {
            paint.color = spec.backgroundColor.toInt()
            paint.alpha = spec.opacity * 255 / 100
            shadowed(paint, spec.shadow, r)
            canvas.drawCircle(cx, cy, r, paint)
            paint.clearShadowLayer()
        }
        val w = r * spec.ringWidth * 0.045f
        val ring = spec.textColor.toInt()
        var outer = r * 0.86f
        repeat(spec.rings.coerceIn(1, 3)) { i ->
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = w
            // Light from above: the top of each ring a little brighter than the bottom.
            paint.shader = LinearGradient(cx, cy - outer, cx, cy + outer, lighten(ring, 0.14f - i * 0.04f), darken(ring, 0.25f), Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, outer - w / 2, paint)
            outer -= w + r * 0.05f
        }
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = spec.accentColor.toInt()
        canvas.drawCircle(cx, cy, outer, paint)
        val iconSize = r * 2 * spec.iconScale / 100f
        val drawable = app?.let { icon(context, it) }
        if (drawable != null) {
            val half = (iconSize / 2).roundToInt()
            drawable.setBounds((cx - half).roundToInt(), (cy - half).roundToInt(), (cx + half).roundToInt(), (cy + half).roundToInt())
            drawable.draw(canvas)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = r * 0.035f
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = lighten(spec.textColor.toInt(), 0.2f)
            val a = iconSize * 0.22f
            canvas.drawLine(cx - a, cy, cx + a, cy, paint)
            canvas.drawLine(cx, cy - a, cx, cy + a, paint)
        }
    }

    // ------------------------------------------------------------ App shortcut

    fun shortcut(context: Context, spec: WidgetSpec, sizePx: Int): Bitmap {
        val size = sizePx.coerceIn(96, 560)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val r = size / 2f * 0.9f
        if (spec.style == WidgetStyle.SHORTCUT_HALO) {
            // A soft halo in the ring colour instead of a hard disc.
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.RadialGradient(size / 2f, size / 2f, r * 1.08f,
                    intArrayOf(withAlpha(spec.textColor.toInt(), 0x66), withAlpha(spec.textColor.toInt(), 0x00)), floatArrayOf(0.7f, 1f), Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(size / 2f, size / 2f, r * 1.08f, glow)
        }
        lobe(context, canvas, size / 2f, size / 2f, r, spec, spec.apps.firstOrNull(), disc = true)
        return bitmap
    }

    // ------------------------------------------------------------ Badge

    fun badge(context: Context, spec: WidgetSpec, sizePx: Int): Bitmap {
        val size = sizePx.coerceIn(96, 640)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val c = size / 2f
        val outer = c * 0.92f
        val (lobes, depth) = when (spec.style) {
            WidgetStyle.BADGE_PETALS -> 6 to 0.11f
            WidgetStyle.BADGE_CIRCLE -> 0 to 0f
            else -> 10 to 0.045f
        }
        val shape = Path()
        for (i in 0..360) {
            val a = Math.toRadians(i.toDouble())
            val rr = if (lobes == 0) outer else outer * (1f - depth * (1f - cos(lobes * a).toFloat()) / 2f)
            val x = c + rr * cos(a - Math.PI / 2).toFloat()
            val y = c + rr * sin(a - Math.PI / 2).toFloat()
            if (i == 0) shape.moveTo(x, y) else shape.lineTo(x, y)
        }
        shape.close()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = spec.backgroundColor.toInt(); alpha = spec.opacity * 255 / 100 }
        shadowed(paint, spec.shadow, outer)
        canvas.drawPath(shape, paint)
        paint.clearShadowLayer()
        paint.alpha = 255
        // The ring band, then any extra thin rings, then the inner disc.
        paint.color = spec.accentColor.toInt()
        canvas.drawCircle(c, c, outer * 0.68f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = outer * spec.ringWidth * 0.008f
        paint.color = lighten(spec.accentColor.toInt(), 0.12f)
        repeat(spec.rings.coerceIn(1, 3) - 1) { i -> canvas.drawCircle(c, c, outer * (0.76f + i * 0.06f), paint) }
        paint.style = Paint.Style.FILL
        val inner = outer * 0.56f
        paint.color = spec.textColor.toInt()
        canvas.drawCircle(c, c, inner, paint)
        val logoSize = inner * 2 * spec.iconScale / 100f
        val image = spec.image?.let { loadBitmap(context, it, logoSize.roundToInt()) }
        val app = spec.apps.firstOrNull()?.let { icon(context, it) }
        when {
            image != null -> {
                // The user's picture fills a circle inside the disc.
                val s = logoSize / min(image.width, image.height)
                val m = Matrix().apply { setScale(s, s); postTranslate(c - image.width * s / 2, c - image.height * s / 2) }
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(image, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(m) } }
                canvas.drawCircle(c, c, logoSize / 2, p)
            }
            app != null -> {
                val half = (logoSize / 2).roundToInt()
                app.setBounds((c - half).roundToInt(), (c - half).roundToInt(), (c + half).roundToInt(), (c + half).roundToInt())
                app.draw(canvas)
            }
            else -> star(canvas, c, logoSize * 0.42f, darken(spec.textColor.toInt(), 0.8f))
        }
        return bitmap
    }

    private fun star(canvas: Canvas, c: Float, r: Float, color: Int) {
        val k = r * 0.2f
        val p = Path().apply {
            moveTo(c, c - r); quadTo(c + k, c - k, c + r, c); quadTo(c + k, c + k, c, c + r)
            quadTo(c - k, c + k, c - r, c); quadTo(c - k, c - k, c, c - r); close()
        }
        canvas.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    private fun loadBitmap(context: Context, uri: String, target: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= max(target, 64)) sample *= 2
        context.contentResolver.openInputStream(Uri.parse(uri))?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.getOrNull()

    // ------------------------------------------------------------ Colour helpers

    private fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha shl 24)

    fun lighten(color: Int, f: Float): Int = blend(color, Color.WHITE, f.coerceIn(0f, 1f))
    fun darken(color: Int, f: Float): Int = blend(color, Color.BLACK, f.coerceIn(0f, 1f))

    private fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(shift: Int) = ((((a shr shift) and 0xFF) * (1 - t)) + (((b shr shift) and 0xFF) * t)).roundToInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
