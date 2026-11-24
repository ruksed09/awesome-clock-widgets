package com.versity.launcher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.lightColorScheme
import com.versity.launcher.data.DrawerStyle
import com.versity.launcher.data.IconStyle
import com.versity.launcher.ui.drawer.LocalIconStyle
import com.versity.launcher.ui.studio.StudioDark
import com.versity.launcher.ui.studio.StudioLight
import com.versity.launcher.ui.studio.studioIsDark
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

/** OpenType lining figures: "11:11" rather than old-style numerals that read as "II:II" in serif fonts. */
const val LiningFigures = "lnum"

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
val LocalLauncherFont = staticCompositionLocalOf<FontFamily> { fontFamilyFor(ThemePresets.font(APP_FONT)) }

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
    CompositionLocalProvider(
        LocalLauncherColors provides colors,
        LocalLauncherFont provides font,
        LocalIconStyle provides settings.iconStyle,
        LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(fontFamily = font, fontFeatureSettings = LiningFigures)),
    ) {
        MaterialTheme(colorScheme = scheme, typography = remember(font) { typographyFor(font) }, content = content)
    }
}

/**
 * The plain app drawer follows the phone, not Versity: the phone's light or dark mode and, on Android 12
 * and newer, the phone's own wallpaper-based colours (Material You). Text stays in Versity's font.
 */
@Composable
fun PlainTheme(settings: LauncherSettings, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
        dark -> StudioDark.let { s -> darkColorScheme(primary = s.brand, background = s.page, surface = s.surface, surfaceContainerHigh = s.panel, onSurface = s.ink, onSurfaceVariant = s.muted, outline = s.line) }
        else -> StudioLight.let { s -> lightColorScheme(primary = s.brand, background = s.page, surface = s.surface, surfaceContainerHigh = s.panel, onSurface = s.ink, onSurfaceVariant = s.muted, outline = s.line) }
    }
    val colors = LauncherColors(scheme.surface, scheme.surface, scheme.surfaceContainerHigh, scheme.onSurface, scheme.onSurfaceVariant, scheme.primary)
    val font = remember { fontFamilyFor(ThemePresets.font(APP_FONT)) }
    CompositionLocalProvider(
        LocalLauncherColors provides colors,
        LocalLauncherFont provides font,
        LocalIconStyle provides settings.iconStyle,
        LocalTextStyle provides TextStyle(fontFamily = font, fontFeatureSettings = LiningFigures),
    ) {
        MaterialTheme(colorScheme = scheme, typography = remember(font) { typographyFor(font) }, content = content)
    }
}

/** Versity's own interface font. Nothing in the app falls back to the phone's font. */
const val APP_FONT = "inter"

/** Material's type scale in [family], so any text that does not set a font still uses Versity's. */
fun typographyFor(family: FontFamily): Typography {
    val t = Typography()
    fun TextStyle.f() = copy(fontFamily = family, fontFeatureSettings = LiningFigures)
    return Typography(
        displayLarge = t.displayLarge.f(), displayMedium = t.displayMedium.f(), displaySmall = t.displaySmall.f(),
        headlineLarge = t.headlineLarge.f(), headlineMedium = t.headlineMedium.f(), headlineSmall = t.headlineSmall.f(),
        titleLarge = t.titleLarge.f(), titleMedium = t.titleMedium.f(), titleSmall = t.titleSmall.f(),
        bodyLarge = t.bodyLarge.f(), bodyMedium = t.bodyMedium.f(), bodySmall = t.bodySmall.f(),
        labelLarge = t.labelLarge.f(), labelMedium = t.labelMedium.f(), labelSmall = t.labelSmall.f(),
    )
}

/**
 * Lets text grow with the phone's font size setting, but only up to [max] (115%), so layouts never
 * break while people who need larger text still get it.
 */
@Composable
fun CappedFontScale(max: Float = 1.15f, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    if (density.fontScale <= max) {
        content()
    } else {
        CompositionLocalProvider(LocalDensity provides Density(density.density, max), content = content)
    }
}

/** The app drawer's look: the theme, or the plain system-like look (the default). */
@Composable
fun DrawerTheme(settings: LauncherSettings, content: @Composable () -> Unit) =
    if (settings.drawerStyle == DrawerStyle.THEMED) AppTheme(settings, content) else PlainTheme(settings, content)

/** True when the plain drawer is showing in light mode, so the status bar needs dark icons. */
@Composable
fun plainDrawerIsLight(settings: LauncherSettings): Boolean =
    settings.drawerStyle == DrawerStyle.PLAIN && !isSystemInDarkTheme()
