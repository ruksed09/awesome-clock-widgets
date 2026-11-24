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
            val host = FrameLayout(activity).apply { background = starry() }
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
