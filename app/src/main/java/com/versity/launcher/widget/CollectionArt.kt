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
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.DisplayMetrics
import androidx.core.content.res.ResourcesCompat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Draws the collection's image widgets: the radial launcher, the app shortcut and the badge. Widgets can
 * only show images and text, so these shapes are rendered here (in software, once per change) and the
 * widget lays tap targets over them. Dark, layered and quiet, after the reference design.
 */
object CollectionArt {
    /** Launcher slots in order: top, left, centre, right, bottom. */
    val Slots = listOf(0.5f to 1 / 6f, 1 / 6f to 0.5f, 0.5f to 0.5f, 5 / 6f to 0.5f, 0.5f to 5 / 6f)
    const val SLOT_COUNT = 5

    /** How many apps a launcher style holds: five in the shapes, eight on the spinner. */
    fun slotCount(spec: WidgetSpec) = if (spec.style == WidgetStyle.RADIAL_SPINNER) SPIN_SLOTS else SLOT_COUNT

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

    fun icon(context: Context, key: String): Drawable? = iconSource(context, key)

    /**
     * The app's icon at the highest density it ships, so it stays sharp when drawn large; the plain
     * lookups are the fallback.
     */
    private fun systemIcon(context: Context, key: String): Drawable? = component(key)?.let { c ->
        val pm = context.packageManager
        runCatching {
            val info = pm.getActivityInfo(c, 0)
            val id = info.iconResource
            if (id == 0) null
            else ResourcesCompat.getDrawableForDensity(pm.getResourcesForApplication(info.applicationInfo), id, DisplayMetrics.DENSITY_XXXHIGH, null)
        }.getOrNull()
            ?: runCatching { pm.getActivityIcon(c) }.getOrNull()
            ?: runCatching { pm.getApplicationIcon(c.packageName) }.getOrNull()
    }

    private fun shadowed(paint: Paint, on: Boolean, r: Float) {
        if (on) paint.setShadowLayer(r * 0.09f, 0f, r * 0.05f, 0x99000000.toInt()) else paint.clearShadowLayer()
    }

    // ------------------------------------------------------------ Radial launcher

    /**
     * Room kept clear around the art, as a share of its side, so soft shadows and hairline edges are
     * never cut off where the image ends.
     */
    private const val MARGIN = 0.035f

    fun radial(context: Context, spec: WidgetSpec, sizePx: Int): Bitmap {
        val size = sizePx.coerceIn(150, 1080)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (spec.style == WidgetStyle.RADIAL_SPINNER) {
            drawSpinner(canvas, spec, size.toFloat(), spec.turn * SPIN_STEP) { icon(context, it) }
            return bitmap
        }
        val m = size * MARGIN
        val inner = size - 2 * m
        val cell = inner / 3f
        val c = size / 2f
        // Circles shrink as spacing grows, leaving the shape visible between them. The orbit's circles
        // sit inside a round plate, so they are a little smaller there.
        val shrink = if (spec.style == WidgetStyle.RADIAL_ORBIT) 0.84f else 1f
        val r = cell / 2f * (0.98f - spec.spacing / 100f) * shrink
        val centres = Slots.map { (x, y) -> m + x * inner to m + y * inner }
        val fill = backdrop(spec, size)
        if (fill != null) {
            val shape = when (spec.style) {
                WidgetStyle.RADIAL_FLOWER -> flowerPath(c, cell, r)
                WidgetStyle.RADIAL_ORBIT -> Path().apply { addCircle(c, c, inner / 2f, Path.Direction.CW) }
                WidgetStyle.RADIAL_CROSS -> crossPath(c, cell, r * 1.06f)
                WidgetStyle.RADIAL_TILE -> squircle(c, inner / 2f)
                else -> null // Rings: each circle gets its own disc below.
            }
            shape?.let { edged(canvas, spec, fill, it, size) }
        }
        if (spec.style == WidgetStyle.RADIAL_ORBIT) orbit(canvas, spec, c, cell, r)
        // The centre is drawn last so a featured centre sits above its neighbours.
        listOf(0, 1, 3, 4, 2).forEach { i ->
            val (cx, cy) = centres[i]
            val featured = spec.featured && i == 2
            // A featured centre grows only as far as the space to its neighbours allows.
            val rr = if (featured) r * min(1.12f, (cell - r) / r * 0.98f).coerceAtLeast(1f) else r
            val disc = if (spec.style == WidgetStyle.RADIAL_RINGS) fill else null
            lobe(canvas, cx, cy, rr, spec, spec.apps.getOrNull(i)?.let { icon(context, it) }, disc, if (featured) 1f else 0f)
        }
        return bitmap
    }

    /**
     * The paint for the launcher's backdrop, or null for none. Solid is the chosen colour, lit softly from
     * above; glass is a pale translucent film that lets the wallpaper through.
     */
    private fun backdrop(spec: WidgetSpec, size: Int): Paint? {
        val alpha = spec.opacity.coerceIn(0, 100) * 255 / 100
        return when (spec.background) {
            WidgetBackground.NONE -> null
            WidgetBackground.GLASS -> Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; this.alpha = alpha * 30 / 100 }
            WidgetBackground.SOLID -> Paint(Paint.ANTI_ALIAS_FLAG).apply {
                val bg = spec.backgroundColor.toInt()
                shader = LinearGradient(0f, 0f, 0f, size.toFloat(), lighten(bg, 0.06f), darken(bg, 0.12f), Shader.TileMode.CLAMP)
                this.alpha = alpha
                shadowed(this, spec.shadow, size / 6f)
            }
        }
    }

    /**
     * Fills [path] and traces it with a hairline: bright on glass so the film has an edge, faint on a
     * solid shape so it stays crisp against dark wallpapers.
     */
    private fun edged(canvas: Canvas, spec: WidgetSpec, fill: Paint, path: Path, size: Int) {
        canvas.drawPath(path, fill)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, size * 0.0032f)
            color = if (spec.background == WidgetBackground.GLASS) withAlpha(Color.WHITE, 0x55)
            else withAlpha(lighten(spec.backgroundColor.toInt(), 0.22f), 0x40 * spec.opacity / 100)
        }
        canvas.drawPath(path, edge)
    }

    /**
     * The flower: the four outer circles joined by concave arcs, each arc a circle touching both of its
     * neighbours, so the outline is one smooth curve with no corners. It is grown until the notches
     * clear the centre circle, which therefore always sits inside. [d] is the distance from the centre
     * to an outer circle and [rs] the circles' radius.
     */
    internal fun flowerPath(c: Float, d: Float, rs: Float): Path {
        // Fillet radius: at least wide enough to reach both neighbours, then grown for a gentle notch.
        var f = max(rs * 0.5f, d / SQRT2 - rs + rs * 0.05f)
        fun reach(f: Float) = (d + sqrt(max(0f, 2 * (rs + f) * (rs + f) - d * d))) / 2f
        var guard = 0
        while (reach(f) * SQRT2 - f < rs * 1.12f && guard++ < 200) f *= 1.03f
        val t = reach(f)
        // Outer circles clockwise from the top, and the fillet between each and the next.
        val dirs = listOf(0f to -1f, 1f to 0f, 0f to 1f, -1f to 0f)
        val lobes = dirs.map { (x, y) -> c + x * d to c + y * d }
        val fillets = dirs.indices.map { k ->
            val (ax, ay) = dirs[k]
            val (bx, by) = dirs[(k + 1) % 4]
            c + (ax + bx) * t to c + (ay + by) * t
        }
        val path = Path()
        for (k in 0 until 4) {
            val (ax, ay) = lobes[k]
            val (px, py) = fillets[(k + 3) % 4]
            val (nx, ny) = fillets[k]
            val start = angle(px - ax, py - ay)
            path.arcTo(RectF(ax - rs, ay - rs, ax + rs, ay + rs), start, positive(angle(nx - ax, ny - ay) - start), k == 0)
            val (bx, by) = lobes[(k + 1) % 4]
            val fs = angle(ax - nx, ay - ny)
            path.arcTo(RectF(nx - f, ny - f, nx + f, ny + f), fs, positive(angle(bx - nx, by - ny) - fs) - 360f, false)
        }
        path.close()
        return path
    }

    /**
     * A plus of four arms with round ends, [h] wide on each side, and the inside corners rounded too,
     * traced as one outline so it has no seams.
     */
    internal fun crossPath(c: Float, d: Float, h: Float): Path {
        val q = h * 0.55f
        val dirs = listOf(0f to -1f, 1f to 0f, 0f to 1f, -1f to 0f)
        val path = Path()
        for (k in 0 until 4) {
            val (ux, uy) = dirs[k]
            val (vx, vy) = dirs[(k + 1) % 4] // clockwise neighbour, also this arm's right-hand side
            val ex = c + ux * d
            val ey = c + uy * d
            if (k == 0) path.moveTo(c - vx * h + ux * (h + q), c - vy * h + uy * (h + q))
            path.lineTo(ex - vx * h, ey - vy * h)
            path.arcTo(RectF(ex - h, ey - h, ex + h, ey + h), angle(-vx, -vy), 180f, false)
            path.lineTo(c + vx * h + ux * (h + q), c + vy * h + uy * (h + q))
            val qx = c + (vx + ux) * (h + q)
            val qy = c + (vy + uy) * (h + q)
            path.arcTo(RectF(qx - q, qy - q, qx + q, qy + q), angle(-vx, -vy), -90f, false)
        }
        path.close()
        return path
    }

    /** A squircle (superellipse) centred on [c]: corners that ease in rather than switching to an arc. */
    internal fun squircle(c: Float, half: Float, n: Double = 5.0): Path {
        val path = Path()
        val steps = 720
        for (i in 0..steps) {
            val a = 2 * Math.PI * i / steps
            val ca = cos(a)
            val sa = sin(a)
            val x = c + half * (Math.signum(ca) * Math.pow(kotlin.math.abs(ca), 2 / n)).toFloat()
            val y = c + half * (Math.signum(sa) * Math.pow(kotlin.math.abs(sa), 2 / n)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private const val SQRT2 = 1.4142135f
    private fun angle(x: Float, y: Float) = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
    private fun positive(deg: Float): Float { var a = deg % 360f; if (a <= 0f) a += 360f; return a }

    /** The orbit: a fine circle through the outer apps, with a small bead between each pair. */
    private fun orbit(canvas: Canvas, spec: WidgetSpec, c: Float, radius: Float, r: Float) {
        val ring = lighten(spec.textColor.toInt(), 0.1f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, r * 0.03f)
            color = withAlpha(ring, 0xB0)
        }
        canvas.drawCircle(c, c, radius, p)
        p.style = Paint.Style.FILL
        p.color = ring
        for (k in 0 until 4) {
            val a = Math.toRadians(45.0 + k * 90.0)
            canvas.drawCircle(c + radius * cos(a).toFloat(), c + radius * sin(a).toFloat(), r * 0.07f, p)
        }
    }

    // ------------------------------------------------------------ Spinner

    const val SPIN_SLOTS = 8
    const val SPIN_STEP = 360f / SPIN_SLOTS

    /** The spinner's measurements for a [size] square: plate, the circle the apps ride on, app and hub radii. */
    class SpinnerGeometry(size: Float, spacing: Int) {
        val c = size / 2f
        val plate = size * (0.5f - MARGIN)
        val track = plate * 0.66f
        val lobe = plate * (0.235f - spacing * 0.0022f)
        val hub = plate * 0.29f
    }

    /** Where app [i] sits on a wheel turned [angle] degrees clockwise, as degrees clockwise from twelve. */
    fun spinAngle(i: Int, angle: Float) = i * SPIN_STEP + angle

    /** 1 for the app under the pointer at twelve, easing to 0 one step away. */
    fun focusOf(deg: Float): Float {
        var d = deg % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        val t = (1f - kotlin.math.abs(d) / SPIN_STEP).coerceIn(0f, 1f)
        return t * t * (3 - 2 * t)
    }

    /**
     * Draws the spinner turned [angle] degrees: a round plate with a ticked rim that turns with the
     * wheel, eight apps on a fine track (the one under the pointer at twelve larger and brighter), and a
     * hub with grip lines and a turn mark. The same drawing serves the widget image and the live,
     * finger-driven wheel on Versity's home screen.
     */
    fun drawSpinner(canvas: Canvas, spec: WidgetSpec, size: Float, angle: Float, icon: (String) -> Drawable?) {
        val g = SpinnerGeometry(size, spec.spacing)
        val ring = spec.textColor.toInt()
        val sizeInt = size.toInt().coerceAtLeast(1)
        backdrop(spec, sizeInt)?.let { edged(canvas, spec, it, Path().apply { addCircle(g.c, g.c, g.plate, Path.Direction.CW) }, sizeInt) }
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        // The rim: fine ticks that turn with the wheel, a longer one between each pair of apps. Without a
        // plate the apps float on the wallpaper, and a loose ring of ticks would only add noise.
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        val ticks = if (spec.background == WidgetBackground.NONE) 0 else SPIN_SLOTS * 6
        for (k in 0 until ticks) {
            val major = k % 6 == 3
            val a = Math.toRadians((k * 360.0 / ticks) + angle)
            val r1 = g.plate * (if (major) 0.885f else 0.915f)
            val r2 = g.plate * 0.95f
            p.strokeWidth = max(1f, g.plate * (if (major) 0.014f else 0.008f))
            p.color = withAlpha(lighten(ring, 0.2f), if (major) 0xC0 else 0x70)
            val sx = sin(a).toFloat()
            val cy = -cos(a).toFloat()
            canvas.drawLine(g.c + sx * r1, g.c + cy * r1, g.c + sx * r2, g.c + cy * r2, p)
        }
        // The track the apps ride on.
        p.strokeWidth = max(1f, g.plate * 0.008f)
        p.color = withAlpha(lighten(ring, 0.1f), 0x80)
        canvas.drawCircle(g.c, g.c, g.track, p)
        // Apps, the focused one last so it sits on top.
        (0 until SPIN_SLOTS).map { i -> i to focusOf(spinAngle(i, angle)) }.sortedBy { it.second }.forEach { (i, focus) ->
            val a = Math.toRadians(spinAngle(i, angle).toDouble())
            val x = g.c + g.track * sin(a).toFloat()
            val y = g.c - g.track * cos(a).toFloat()
            lobe(canvas, x, y, g.lobe * (1f + 0.16f * focus), spec, spec.apps.getOrNull(i)?.takeIf { it.isNotBlank() }?.let(icon), null, focus)
        }
        // The pointer at twelve, outside the turning parts.
        p.style = Paint.Style.FILL
        p.color = lighten(ring, 0.6f)
        val tip = g.c - g.plate * 0.905f
        val base = g.c - g.plate * 0.99f
        val w = g.plate * 0.055f
        canvas.drawPath(Path().apply {
            moveTo(g.c, tip)
            quadTo(g.c - w * 0.25f, tip - (tip - base) * 0.35f, g.c - w, base)
            lineTo(g.c + w, base)
            quadTo(g.c + w * 0.25f, tip - (tip - base) * 0.35f, g.c, tip)
            close()
        }, p)
        hub(canvas, spec, g, angle)
    }

    /** The hub: a ringed knob with grip lines that turn with the wheel and a fixed turn mark. */
    private fun hub(canvas: Canvas, spec: WidgetSpec, g: SpinnerGeometry, angle: Float) {
        val ring = spec.textColor.toInt()
        val well = spec.accentColor.toInt()
        val r = g.hub
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(g.c, g.c - r, g.c, g.c + r, lighten(ring, 0.12f), darken(ring, 0.3f), Shader.TileMode.CLAMP)
        canvas.drawCircle(g.c, g.c, r, p)
        val inner = r * 0.84f
        p.shader = LinearGradient(g.c, g.c - inner, g.c, g.c + inner, darken(well, 0.3f), lighten(well, 0.05f), Shader.TileMode.CLAMP)
        canvas.drawCircle(g.c, g.c, inner, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = max(1f, r * 0.05f)
        p.color = withAlpha(lighten(ring, 0.15f), 0x90)
        for (k in 0 until 12) {
            val a = Math.toRadians(k * 30.0 + angle)
            val sx = sin(a).toFloat()
            val cy = -cos(a).toFloat()
            canvas.drawLine(g.c + sx * inner * 0.68f, g.c + cy * inner * 0.68f, g.c + sx * inner * 0.86f, g.c + cy * inner * 0.86f, p)
        }
        // A circular arrow: tap or drag to turn.
        val ar = inner * 0.34f
        p.strokeWidth = max(1f, r * 0.085f)
        p.color = lighten(ring, 0.55f)
        canvas.drawArc(RectF(g.c - ar, g.c - ar, g.c + ar, g.c + ar), -60f, 290f, false, p)
        p.style = Paint.Style.FILL
        val ha = Math.toRadians(-60.0)
        val hx = g.c + ar * cos(ha).toFloat()
        val hy = g.c + ar * sin(ha).toFloat()
        val s = ar * 0.55f
        canvas.drawPath(Path().apply {
            // Arrowhead pointing along the arc's clockwise end.
            moveTo(hx + s * 0.9f, hy + s * 0.15f)
            lineTo(hx - s * 0.35f, hy - s * 0.75f)
            lineTo(hx - s * 0.2f, hy + s * 0.75f)
            close()
        }, p)
    }

    /**
     * One circle: an optional disc (for separate rings), concentric rings lit softly from above, a
     * recessed well and the app icon, or a quiet "+" when the slot is empty. [bright] (0..1) lifts the
     * rings for a featured or focused app.
     */
    private fun lobe(canvas: Canvas, cx: Float, cy: Float, r: Float, spec: WidgetSpec, drawable: Drawable?, disc: Paint?, bright: Float = 0f) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (disc != null) canvas.drawCircle(cx, cy, r, disc)
        val w = r * spec.ringWidth * 0.045f
        val ring = lighten(spec.textColor.toInt(), 0.22f * bright)
        var outer = r * 0.86f
        repeat(spec.rings.coerceIn(0, 3)) { i ->
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = w
            // Light from above: the top of each ring a little brighter than the bottom.
            paint.shader = LinearGradient(cx, cy - outer, cx, cy + outer, lighten(ring, 0.14f - i * 0.04f), darken(ring, 0.25f), Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, outer - w / 2, paint)
            outer -= w + r * 0.05f
        }
        // The well is a touch darker at the top, so it reads as set into the ring.
        val well = spec.accentColor.toInt()
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(cx, cy - outer, cx, cy + outer, darken(well, 0.3f), lighten(well, 0.05f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, outer, paint)
        paint.shader = null
        val iconSize = min(r * 2 * spec.iconScale / 100f, outer * 2f)
        if (drawable != null) {
            val half = iconSize / 2f
            drawable.setBounds((cx - half).roundToInt(), (cy - half).roundToInt(), (cx + half).roundToInt(), (cy + half).roundToInt())
            if (spec.roundIcons) {
                canvas.save()
                canvas.clipPath(Path().apply { addCircle(cx, cy, half, Path.Direction.CW) })
                drawable.draw(canvas)
                canvas.restore()
            } else {
                drawable.draw(canvas)
            }
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = max(1f, r * 0.035f)
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
        lobe(canvas, size / 2f, size / 2f, r, spec, spec.apps.firstOrNull()?.let { icon(context, it) }, backdrop(spec, size))
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
