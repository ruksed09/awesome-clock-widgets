package com.versity.launcher.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.versity.launcher.widget.WidgetAlign
import com.versity.launcher.widget.WidgetBackground
import com.versity.launcher.widget.WidgetRenderer
import com.versity.launcher.widget.WidgetSpec
import com.versity.launcher.widget.WidgetStyle
import com.versity.launcher.widget.WidgetType
import com.versity.launcher.data.ClockPlacement
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.data.UiMode
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.domain.lockLook
import com.versity.launcher.domain.withTheme
import com.versity.launcher.ui.home.HomeContent
import com.versity.launcher.ui.home.HomeUiState
import com.versity.launcher.ui.lock.LockStyleContent
import com.versity.launcher.ui.settings.CustomiseActions
import com.versity.launcher.ui.settings.CustomiseContent
import com.versity.launcher.ui.settings.CustomisePage
import com.versity.launcher.ui.TestTags
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import java.io.File
import java.time.LocalDateTime
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The same screens at several device classes, to check layouts hold on small phones, cover screens,
 * landscape phones and tablets. Opt-in like [ScreenshotHarness] (VERSITY_SCREENSHOTS).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class DeviceScreenshots(private val device: String) {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val out: File? = System.getenv("VERSITY_SCREENSHOTS")?.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.isAbsolute }
    private val now = LocalDateTime.of(2026, 11, 11, 11, 11)
    private var screen by mutableStateOf<(@Composable () -> Unit)?>(null)

    @Before fun onlyWhenAsked() {
        assumeTrue("Set VERSITY_SCREENSHOTS to an absolute folder to render screenshots", out != null)
        out!!.mkdirs()
    }

    private fun save(name: String, view: View = rule.activity.window.decorView) {
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        File(out, "${device}_$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun shoot(name: String, content: @Composable () -> Unit) {
        screen = content
        rule.waitForIdle()
        save(name)
    }

    @Test fun screens() {
        rule.setContent { screen?.invoke() }
        for (mode in listOf(UiMode.LIGHT, UiMode.DARK)) {
            val s = LauncherSettings.DEFAULT.copy(uiMode = mode)
            for (page in listOf(CustomisePage.HUB, CustomisePage.THEMES, CustomisePage.WALLPAPERS, CustomisePage.WIDGETS, CustomisePage.SETTINGS, CustomisePage.CLOCK, CustomisePage.COLOURS)) {
                shoot("${mode.name.lowercase()}_${page.name.lowercase()}") { CustomiseContent(s, object : CustomiseActions {}, initialPage = page, isDefaultLauncher = false) }
                screen = null
                rule.waitForIdle()
            }
        }
        for (id in listOf("leviathan", "mega", "frost", "flipboard", "led", "pebble")) {
            val s = LauncherSettings.DEFAULT.withTheme(ThemePresets.theme(id)!!, useThemeWallpaper = true)
            shoot("home_$id") { HomeContent(HomeUiState(settings = s, now = now)) }
            shoot("lock_$id") { LockStyleContent(HomeUiState(settings = s, now = now), onUnlock = {}) }
        }
        val placed = LauncherSettings.DEFAULT.withTheme(ThemePresets.theme("mega")!!).copy(
            clockPlacement = ClockPlacement(0.5f, 0.5f, 1.6f), lockClockPlacement = ClockPlacement(0.5f, 0.5f, 1.6f),
        )
        shoot("placed_max_home") { HomeContent(HomeUiState(settings = placed, now = now)) }
        shoot("placed_max_lock") { LockStyleContent(HomeUiState(settings = placed.lockLook(), now = now), onUnlock = {}) }
    }

    /** The widget studio, which switches to a two-pane layout on wide screens. */
    @Test fun studio() {
        val s = LauncherSettings.DEFAULT.copy(uiMode = UiMode.LIGHT)
        rule.setContent { CustomiseContent(s, object : CustomiseActions {}, initialPage = CustomisePage.WIDGETS) }
        rule.onNodeWithTag(TestTags.widgetType(WidgetType.CLOCK_DATE.name)).performScrollTo().performClick()
        rule.mainClock.advanceTimeBy(600)
        rule.waitForIdle()
        save("studio_clock_date")
    }

    /**
     * Every widget style built by the real renderer and applied at launcher cell sizes, on a dark and a
     * light wallpaper colour. The views are attached to the window so TextClock fills itself in.
     */
    @Test fun widgets() {
        val ctx = rule.activity
        val sizes = listOf("2x1" to (140 to 70), "2x2" to (150 to 150), "4x2" to (300 to 150), "tablet" to (520 to 260))
        val looks = listOf(
            WidgetSpec.default(WidgetType.DIGITAL),
            WidgetSpec.default(WidgetType.DIGITAL).copy(style = WidgetStyle.STACKED, typeface = "outfit_black", twoTone = true, accentColor = 0xFFFF8A70),
            WidgetSpec.default(WidgetType.DIGITAL).copy(style = WidgetStyle.LINE, typeface = "inter_thin", background = WidgetBackground.GLASS, opacity = 18, border = true),
            WidgetSpec.default(WidgetType.CLOCK_DATE),
            WidgetSpec.default(WidgetType.CLOCK_DATE).copy(style = WidgetStyle.EDITORIAL, typeface = "inter_bold", align = WidgetAlign.START, accentColor = 0xFFFF8A70),
            WidgetSpec.default(WidgetType.CLOCK_DATE).copy(style = WidgetStyle.COMPACT, typeface = "space_grotesk", background = WidgetBackground.SOLID, backgroundColor = 0xFF111114, opacity = 80),
            WidgetSpec.default(WidgetType.CLOCK_DATE).copy(style = WidgetStyle.STACKED_DATE, typeface = "syne_bold", textColor = 0xFF111114, accentColor = 0xFF3E5BD8),
            WidgetSpec.default(WidgetType.ANALOGUE),
            WidgetSpec.default(WidgetType.ANALOGUE).copy(style = WidgetStyle.DIAL_DOTS, dialFace = false, background = WidgetBackground.GLASS, opacity = 16, border = true, seconds = true),
            WidgetSpec.default(WidgetType.ANALOGUE).copy(style = WidgetStyle.DIAL_MINIMAL, textColor = 0xFF111114, backgroundColor = 0xFFF4F1EA, opacity = 90),
            WidgetSpec.default(WidgetType.DATE),
            WidgetSpec.default(WidgetType.DATE).copy(style = WidgetStyle.DATE_LINE, typeface = "playfair", background = WidgetBackground.NONE, border = false),
            WidgetSpec.default(WidgetType.DATE).copy(style = WidgetStyle.MONTH, typeface = "inter_bold"),
        )
        looks.forEachIndexed { i, spec ->
            for ((label, wh) in sizes) {
                val (wDp, hDp) = wh
                for ((bgName, bg) in listOf("dark" to 0xFF1B3F66.toInt(), "light" to 0xFFE9E4DA.toInt())) {
                    val light = WidgetRenderer.luminance(spec.textColor) > 0.5
                    // Dark ink on a dark wallpaper (or light on light) is not a real use; skip those pairs.
                    if ((bgName == "dark") != light && spec.background == WidgetBackground.NONE) continue
                    renderWidget(spec, wDp, hDp, bg, "widget_${i}_${spec.style.name.lowercase()}_${label}_$bgName")
                }
            }
        }
    }

    private fun renderWidget(spec: WidgetSpec, wDp: Int, hDp: Int, background: Int, name: String) {
        val ctx = rule.activity
        val density = ctx.resources.displayMetrics.density
        val host = FrameLayout(ctx).apply { setBackgroundColor(background) }
        host.addView(WidgetRenderer.build(ctx, spec, wDp, hDp).apply(ctx, host))
        val decor = ctx.window.decorView as android.view.ViewGroup
        val w = (wDp * density).toInt()
        val h = (hDp * density).toInt()
        decor.addView(host, android.view.ViewGroup.LayoutParams(w, h))
        rule.waitForIdle()
        host.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        host.layout(0, 0, w, h)
        save(name, host)
        decor.removeView(host)
    }
}

@Config(sdk = [35], qualifiers = "w320dp-h568dp-xhdpi") class SmallPhoneScreens : DeviceScreenshots("small")
@Config(sdk = [35], qualifiers = "w280dp-h653dp-xhdpi") class CoverScreenScreens : DeviceScreenshots("cover")
@Config(sdk = [35], qualifiers = "w780dp-h360dp-land-xhdpi") class PhoneLandscapeScreens : DeviceScreenshots("phoneland")
@Config(sdk = [35], qualifiers = "w800dp-h1280dp-hdpi") class TabletPortraitScreens : DeviceScreenshots("tablet")
@Config(sdk = [35], qualifiers = "w1280dp-h800dp-land-hdpi") class TabletLandscapeScreens : DeviceScreenshots("tabletland")
