package com.versity.launcher.screenshots

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.versity.launcher.LauncherApplication
import com.versity.launcher.widget.CollectionArt
import com.versity.launcher.widget.HandStyle
import com.versity.launcher.widget.FaceShape
import com.versity.launcher.widget.WidgetBackground
import com.versity.launcher.widget.WidgetRenderer
import com.versity.launcher.widget.WidgetSpec
import com.versity.launcher.widget.WidgetStyle
import com.versity.launcher.widget.WidgetType
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The collection widgets on a starry black wallpaper, with five stand-in apps, for comparing with the
 * reference design. Opt-in like the other screenshot suites (VERSITY_SCREENSHOTS). With
 * VERSITY_PREVIEWS set to the drawable-nodpi folder it also writes the widget picker previews.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class CollectionScreenshots {
    private val context = ApplicationProvider.getApplicationContext<LauncherApplication>()
    private val out: File? = System.getenv("VERSITY_SCREENSHOTS")?.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.isAbsolute }
    private val previews: File? = System.getenv("VERSITY_PREVIEWS")?.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.isAbsolute }
    private val colours = listOf(0xFF25D366, 0xFFE1306C, 0xFF1DB954, 0xFF2AABEE, 0xFFFF0000)
    private val apps = colours.indices.map { i ->
        val c = ComponentName("com.example.app$i", "com.example.app$i.Main")
        shadowOf(context.packageManager).addActivityIfNotPresent(c)
        "${c.packageName}/${c.className}#0"
    }.also { keys ->
        // Stand-in icons: rounded squares in the colours of the reference's apps.
        CollectionArt.iconSource = { _, key -> keys.indexOf(key).takeIf { it >= 0 }?.let { GradientDrawable().apply { setColor(colours[it].toInt()); cornerRadius = 36f } } }
    }

    @Before fun onlyWhenAsked() = assumeTrue(out != null || previews != null)

    @Test fun collection() {
        val looks = listOf(
            "countdown" to Triple(WidgetSpec.default(WidgetType.COUNTDOWN).copy(title = "Doomsday", target = System.currentTimeMillis() + 22 * 86_400_000L + 25_685_000L), 300, 150),
            "countdown_big" to Triple(WidgetSpec.default(WidgetType.COUNTDOWN).copy(style = WidgetStyle.COUNTDOWN_BIG, title = "Doomsday", target = System.currentTimeMillis() + 22 * 86_400_000L), 220, 150),
            "launcher" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(apps = apps), 220, 220),
            "launcher_rings" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(style = WidgetStyle.RADIAL_RINGS, apps = apps, spacing = 10), 220, 220),
            "launcher_orbit" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(apps = apps, style = WidgetStyle.RADIAL_ORBIT, featured = true), 220, 220),
            "launcher_cross" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(apps = apps, style = WidgetStyle.RADIAL_CROSS, backgroundColor = 0xFF0E1913, textColor = 0xFF2F4A3B, accentColor = 0xFF14211A), 220, 220),
            "launcher_tile" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(apps = apps, style = WidgetStyle.RADIAL_TILE, roundIcons = true, rings = 1), 220, 220),
            "launcher_none" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(apps = apps, background = WidgetBackground.NONE, featured = true), 220, 220),
            "launcher_glass" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(apps = apps, style = WidgetStyle.RADIAL_CROSS, background = WidgetBackground.GLASS, rings = 0, roundIcons = true, accentColor = 0xFF20232B), 220, 220),
            "launcher_glass_orbit" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(apps = apps, style = WidgetStyle.RADIAL_ORBIT, background = WidgetBackground.GLASS, rings = 1), 220, 220),
            "spinner" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(apps = apps + apps.take(3).reversed(), style = WidgetStyle.RADIAL_SPINNER), 220, 220),
            "spinner_turned" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(apps = apps + apps.take(3).reversed(), style = WidgetStyle.RADIAL_SPINNER, turn = 3, roundIcons = true), 220, 220),
            "spinner_glass" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(apps = apps + apps.take(3).reversed(), style = WidgetStyle.RADIAL_SPINNER, background = WidgetBackground.GLASS, rings = 1, accentColor = 0xFF20232B), 220, 220),
            "spinner_green" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(apps = apps + apps.take(3).reversed(), style = WidgetStyle.RADIAL_SPINNER, background = WidgetBackground.NONE, textColor = 0xFF2F4A3B, accentColor = 0xFF14211A), 220, 220),
            "spinner_big" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(apps = apps + apps.take(3).reversed(), style = WidgetStyle.RADIAL_SPINNER, spacing = 0), 400, 400),
            "launcher_big" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(apps = apps + apps.take(3).reversed()), 400, 400),
            "launcher_cross_big" to Triple(WidgetSpec.default(WidgetType.LAUNCHER).copy(apps = apps + apps.take(3).reversed(), style = WidgetStyle.RADIAL_CROSS), 400, 400),
            "dial_railway" to Triple(WidgetSpec.default(WidgetType.ANALOGUE).copy(style = WidgetStyle.DIAL_RAILWAY, hands = HandStyle.NEEDLE, seconds = true), 160, 160),
            "dial_sector" to Triple(WidgetSpec.default(WidgetType.ANALOGUE).copy(style = WidgetStyle.DIAL_SECTOR, hands = HandStyle.DAUPHINE, textColor = 0xFF141418, backgroundColor = 0xFFEDE6D6, opacity = 100, accentColor = 0xFFC2452D, seconds = true), 160, 160),
            "dial_diver" to Triple(WidgetSpec.default(WidgetType.ANALOGUE).copy(style = WidgetStyle.DIAL_DIVER, hands = HandStyle.SKELETON, backgroundColor = 0xFF0E1913, opacity = 100, accentColor = 0xFF7DD69B), 160, 160),
            "dial_segments" to Triple(WidgetSpec.default(WidgetType.ANALOGUE).copy(style = WidgetStyle.DIAL_SEGMENTS, hands = HandStyle.BOLD, accentColor = 0xFF8FA3FF), 160, 160),
            "dial_dual" to Triple(WidgetSpec.default(WidgetType.ANALOGUE).copy(style = WidgetStyle.DIAL_DUAL, typeface = "space_grotesk", hands = HandStyle.DAUPHINE), 160, 160),
            "dial_diver_square" to Triple(WidgetSpec.default(WidgetType.ANALOGUE).copy(style = WidgetStyle.DIAL_DIVER, faceShape = FaceShape.SQUARE, hands = HandStyle.NEEDLE, backgroundColor = 0xFF1F2430, opacity = 100), 160, 160),
            "dial_segments_square" to Triple(WidgetSpec.default(WidgetType.ANALOGUE).copy(style = WidgetStyle.DIAL_SEGMENTS, faceShape = FaceShape.SQUARE, hands = HandStyle.SKELETON), 160, 160),
            "shortcut" to Triple(WidgetSpec.default(WidgetType.SHORTCUT).copy(apps = apps.take(1)), 110, 110),
            "shortcut_halo" to Triple(WidgetSpec.default(WidgetType.SHORTCUT).copy(style = WidgetStyle.SHORTCUT_HALO, apps = apps.drop(2).take(1), textColor = 0xFF7DD69B), 110, 110),
            "badge" to Triple(WidgetSpec.default(WidgetType.BADGE), 150, 150),
            "badge_petals" to Triple(WidgetSpec.default(WidgetType.BADGE).copy(style = WidgetStyle.BADGE_PETALS, apps = apps.take(1), backgroundColor = 0xFF0E1913), 150, 150),
            "badge_circle" to Triple(WidgetSpec.default(WidgetType.BADGE).copy(style = WidgetStyle.BADGE_CIRCLE, rings = 3), 150, 150),
        )
        val activity = Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java).setup().get()
        val density = activity.resources.displayMetrics.density
        for ((name, look) in looks) {
            val (spec, wDp, hDp) = look
            // Glass needs something to show through, so it sits on a colour wash rather than black.
            val host = FrameLayout(activity).apply {
                background = if ("glass" in name) android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF2B4C7E.toInt(), 0xFF6A3E7C.toInt(), 0xFFB0606A.toInt()),
                ) else starry()
            }
            host.addView(WidgetRenderer.build(activity, spec, wDp, hDp).apply(activity, host))
            val w = (wDp * density).toInt()
            val h = (hDp * density).toInt()
            (activity.window.decorView as ViewGroup).addView(host, ViewGroup.LayoutParams(w, h))
            host.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            host.layout(0, 0, w, h)
            out?.let { dir ->
                dir.mkdirs()
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                host.draw(Canvas(bmp))
                File(dir, "collection_$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            (activity.window.decorView as ViewGroup).removeView(host)
        }
        previews?.let { dir ->
            save(dir, "widget_preview_radial", CollectionArt.radial(context, WidgetSpec.default(WidgetType.LAUNCHER), 360))
            save(dir, "widget_preview_shortcut", CollectionArt.shortcut(context, WidgetSpec.default(WidgetType.SHORTCUT), 200))
            save(dir, "widget_preview_badge", CollectionArt.badge(context, WidgetSpec.default(WidgetType.BADGE), 300))
        }
    }

    private fun save(dir: File, name: String, bmp: Bitmap) =
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

    /** A black sky with a scatter of stars, like the reference wallpaper. */
    private fun starry() = object : android.graphics.drawable.Drawable() {
        override fun draw(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x88FFFFFF.toInt() }
            val r = java.util.Random(7)
            repeat(260) { canvas.drawCircle(r.nextFloat() * bounds.width(), r.nextFloat() * bounds.height(), 0.6f + r.nextFloat() * 1.2f, p) }
        }
        override fun setAlpha(alpha: Int) = Unit
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) = Unit
        @Deprecated("Deprecated in Java") override fun getOpacity() = android.graphics.PixelFormat.OPAQUE
    }
}
