package com.versity.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.domain.FontOption
import com.versity.launcher.domain.Palette
import com.versity.launcher.domain.ThemePresets

/** Resolved colours for the current settings: palette, with the accent override applied. */
@Immutable
class LauncherColors(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val surface: Color,
    val onSurface: Color,
    val muted: Color,
    val accent: Color,
) {
    /** Thin 1dp lines everywhere. */
    val border: Color get() = onSurface.copy(alpha = 0.14f)
}

fun resolveColors(palette: Palette, accentArgb: Long?) = LauncherColors(
    backgroundTop = Color(palette.backgroundTop),
    backgroundBottom = Color(palette.backgroundBottom),
    surface = Color(palette.surface),
    onSurface = Color(palette.onSurface),
    muted = Color(palette.onSurfaceMuted),
    accent = Color(accentArgb ?: palette.accent),
)

/** Variable fonts are bundled; weights are selected through variation settings (API 26+). */
@OptIn(ExperimentalTextApi::class)
fun fontFamilyFor(option: FontOption): FontFamily = FontFamily(
    listOf(300, 400, 500, 600, 700).map { w ->
        Font(
            resId = option.resId,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    },
)

val LocalLauncherColors = staticCompositionLocalOf {
    resolveColors(ThemePresets.palette(LauncherSettings.DEFAULT.paletteId), null)
}
val LocalLauncherFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

object LauncherTheme {
    val colors: LauncherColors @Composable get() = LocalLauncherColors.current
    val font: FontFamily @Composable get() = LocalLauncherFont.current
}

@Composable
fun AppTheme(settings: LauncherSettings, content: @Composable () -> Unit) {
    val colors = remember(settings.paletteId, settings.accentArgb) {
        resolveColors(ThemePresets.palette(settings.paletteId), settings.accentArgb)
    }
    val font = remember(settings.fontId) { fontFamilyFor(ThemePresets.font(settings.fontId)) }
    val scheme = darkColorScheme(
        primary = colors.accent,
        onPrimary = colors.backgroundBottom,
        background = colors.backgroundBottom,
        onBackground = colors.onSurface,
        surface = colors.surface,
        onSurface = colors.onSurface,
        onSurfaceVariant = colors.muted,
        outline = colors.border,
    )
    CompositionLocalProvider(LocalLauncherColors provides colors, LocalLauncherFont provides font) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
