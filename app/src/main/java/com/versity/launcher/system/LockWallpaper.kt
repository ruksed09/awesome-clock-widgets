package com.versity.launcher.system

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.net.toUri
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.ui.backdrop.drawBackdrop
import com.versity.launcher.ui.home.decodeDownsampled
import com.versity.launcher.ui.theme.resolveColors
import com.versity.launcher.util.centerCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders the current look (gradient, wallpaper, dim, backdrop) to a bitmap and sets it as the system
 * lock-screen wallpaper. No text is drawn into it, so it never shows a stale clock. The phone's own clock
 * and unlock screen stay on top; some ColorOS versions or Theme Store themes may override it.
 */
object LockWallpaper {
    /**
     * [which] is [WallpaperManager.FLAG_LOCK] (default), [WallpaperManager.FLAG_SYSTEM] for the home
     * wallpaper, or both OR-ed together.
     */
    suspend fun apply(
        context: Context,
        settings: LauncherSettings,
        which: Int = WallpaperManager.FLAG_LOCK,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val manager = WallpaperManager.getInstance(context)
            check(manager.isWallpaperSupported) { "This device does not support changing the wallpaper." }
            check(manager.isSetWallpaperAllowed) { "Changing the wallpaper is not allowed on this device." }
            val (w, h) = portraitSize(context)
            manager.setBitmap(render(context, settings, w, h), null, true, which)
            Unit
        }
    }

    private fun portraitSize(context: Context): Pair<Int, Int> {
        val windows = context.getSystemService(WindowManager::class.java)
        val (a, b) = if (Build.VERSION.SDK_INT >= 30) {
            windows.maximumWindowMetrics.bounds.let { it.width() to it.height() }
        } else {
            val m = DisplayMetrics()
            @Suppress("DEPRECATION") windows.defaultDisplay.getRealMetrics(m)
            m.widthPixels to m.heightPixels
        }
        return if (a > b) b to a else a to b
    }

    private fun render(context: Context, settings: LauncherSettings, w: Int, h: Int): android.graphics.Bitmap {
        val palette = ThemePresets.palette(settings.paletteId)
        val colors = resolveColors(palette, settings.accentArgb)
        val dim = settings.dimPercent.coerceIn(LauncherSettings.MIN_DIM, LauncherSettings.MAX_DIM) / 100f
        val photo = com.versity.launcher.domain.Wallpapers.sourceFor(settings.wallpaperUri, w, h)?.let { decodeDownsampled(context, it.toUri(), w, h) }
        val image = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(context.resources.displayMetrics.density), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat())) {
            drawRect(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)))
            if (photo != null) {
                val c = centerCrop(photo.width, photo.height, w, h)
                drawImage(photo, srcOffset = IntOffset(c.left, c.top), srcSize = IntSize(c.width, c.height), dstSize = IntSize(w, h))
            }
            drawRect(Color.Black.copy(alpha = dim))
            drawBackdrop(settings.backdrop, palette, colors.accent)
        }
        return image.asAndroidBitmap()
    }
}
