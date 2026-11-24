package com.versity.launcher.ui.drawer

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.versity.launcher.data.AppEntry
import com.versity.launcher.data.IconStyle
import com.versity.launcher.system.AppLauncher
import com.versity.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Icon look for the current screen. ORIGINAL unless the user turned themed icons on. */
val LocalIconStyle = staticCompositionLocalOf { IconStyle.ORIGINAL }

private val iconCache = LruCache<String, ImageBitmap>(256)

/** A loaded icon. [tinted] is true when the bitmap already carries the theme colours (monochrome layer). */
private class LoadedIcon(val bitmap: ImageBitmap, val tinted: Boolean)

/** Apps already found to have no monochrome layer, so themed mode does not retry them on every scroll. */
private val noMonochrome = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

private fun loadIcon(launcher: AppLauncher, app: AppEntry, px: Int, style: IconStyle, ink: Color, plate: Color): LoadedIcon? {
    val plainKey = "${app.key}@$px"
    if (style == IconStyle.THEMED && app.key !in noMonochrome) {
        val monoKey = "$plainKey/mono/${ink.toArgb()}/${plate.toArgb()}"
        iconCache.get(monoKey)?.let { return LoadedIcon(it, tinted = true) }
        val drawable = runCatching { launcher.icon(app) }.getOrNull() ?: return null
        val mono = monochrome(drawable, ink, plate)
        if (mono != null) {
            val bmp = runCatching { mono.toBitmap(px, px).asImageBitmap() }.getOrNull() ?: return null
            iconCache.put(monoKey, bmp)
            return LoadedIcon(bmp, tinted = true)
        }
        noMonochrome.add(app.key)
    }
    iconCache.get(plainKey)?.let { return LoadedIcon(it, tinted = false) }
    val bmp = runCatching { launcher.icon(app)?.toBitmap(px, px)?.asImageBitmap() }.getOrNull() ?: return null
    iconCache.put(plainKey, bmp)
    return LoadedIcon(bmp, tinted = false)
}

/** Android 13+ themed-icon layer on a theme-coloured plate, in the system's icon shape. Null when the app has none. */
private fun monochrome(drawable: Drawable, ink: Color, plate: Color): Drawable? {
    if (Build.VERSION.SDK_INT < 33 || drawable !is AdaptiveIconDrawable) return null
    val mono = drawable.monochrome?.mutate() ?: return null
    mono.setTint(ink.toArgb())
    return AdaptiveIconDrawable(ColorDrawable(plate.toArgb()), mono)
}

/** Greyscale, then multiplied by [tint]: used for themed icons when an app has no monochrome layer. */
private fun tintMatrix(tint: Color): ColorMatrix {
    val r = tint.red
    val g = tint.green
    val b = tint.blue
    return ColorMatrix(
        floatArrayOf(
            0.299f * r, 0.587f * r, 0.114f * r, 0f, 0f,
            0.299f * g, 0.587f * g, 0.114f * g, 0f, 0f,
            0.299f * b, 0.587f * b, 0.114f * b, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
}

@Composable
fun AppIcon(app: AppEntry, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val launcher = remember(context) { AppLauncher(context) }
    val px = with(LocalDensity.current) { size.roundToPx() }
    val style = LocalIconStyle.current
    val colors = LauncherTheme.colors
    val ink = colors.accent
    val plate = colors.surface
    val icon by produceState<LoadedIcon?>(null, app.key, px, style, ink, plate) {
        value = withContext(Dispatchers.Default) { loadIcon(launcher, app, px, style, ink, plate) }
    }
    val filter = remember(style, ink) {
        if (style == IconStyle.THEMED) ColorFilter.colorMatrix(tintMatrix(lerpToward(Color.White, ink))) else null
    }
    Box(modifier.size(size)) {
        icon?.let {
            Image(
                it.bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                colorFilter = if (it.tinted) null else filter,
            )
        }
    }
}

/** Mostly the accent, lifted a little toward white so dark icon details stay visible. */
private fun lerpToward(white: Color, accent: Color) = Color(
    red = accent.red * 0.8f + white.red * 0.2f,
    green = accent.green * 0.8f + white.green * 0.2f,
    blue = accent.blue * 0.8f + white.blue * 0.2f,
)
