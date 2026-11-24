package com.versity.launcher.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.versity.launcher.R
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
            for (page in listOf(CustomisePage.HUB, CustomisePage.THEMES, CustomisePage.WALLPAPERS, CustomisePage.STYLE)) {
                shoot("${mode.name.lowercase()}_${page.name.lowercase()}") { CustomiseContent(s, object : CustomiseActions {}, initialPage = page, isDefaultLauncher = false) }
                screen = null
                rule.waitForIdle()
            }
        }
        for (id in listOf("leviathan", "mega", "frost", "flipboard", "led", "pebble")) {
            val s = LauncherSettings.DEFAULT.withTheme(ThemePresets.theme(id)!!, useThemeWallpaper = true)
            shoot("home_$id") { HomeContent(HomeUiState(settings = s, batteryPercent = 72, now = now)) }
            shoot("lock_$id") { LockStyleContent(HomeUiState(settings = s, batteryPercent = 72, now = now), onUnlock = {}) }
        }
        val placed = LauncherSettings.DEFAULT.withTheme(ThemePresets.theme("mega")!!).copy(
            clockPlacement = ClockPlacement(0.5f, 0.5f, 1.6f), lockClockPlacement = ClockPlacement(0.5f, 0.5f, 1.6f),
        )
        shoot("placed_max_home") { HomeContent(HomeUiState(settings = placed, batteryPercent = 72, now = now)) }
        shoot("placed_max_lock") { LockStyleContent(HomeUiState(settings = placed.lockLook(), batteryPercent = 72, now = now), onUnlock = {}) }
    }

    /** The home-screen widgets inflated at real cell sizes, as a launcher would host them. */
    @Test fun widgets() {
        val ctx = rule.activity
        val density = ctx.resources.displayMetrics.density
        val sizes = listOf("2x1" to (140 to 70), "3x1" to (220 to 80), "4x2" to (300 to 150), "5x3" to (380 to 230), "tablet" to (520 to 260))
        for (layout in listOf(R.layout.widget_celestial_time, R.layout.widget_celestial_time_date)) {
            for ((label, wh) in sizes) {
                val (wDp, hDp) = wh
                val host = FrameLayout(ctx).apply { setBackgroundColor(0xFF1B3F66.toInt()) }
                LayoutInflater.from(ctx).inflate(layout, host, true)
                // TextClock only fills itself in once attached to a window; give it sample text here.
                host.findViewById<android.widget.TextView>(R.id.widget_time).text = "11:11"
                host.findViewById<android.widget.TextView>(R.id.widget_date)?.text = "Wednesday  ·  11 November"
                val w = (wDp * density).toInt()
                val h = (hDp * density).toInt()
                host.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                host.layout(0, 0, w, h)
                val name = if (layout == R.layout.widget_celestial_time) "time" else "timedate"
                save("widget_${name}_$label", host)
            }
        }
    }
}

@Config(sdk = [35], qualifiers = "w320dp-h568dp-xhdpi") class SmallPhoneScreens : DeviceScreenshots("small")
@Config(sdk = [35], qualifiers = "w280dp-h653dp-xhdpi") class CoverScreenScreens : DeviceScreenshots("cover")
@Config(sdk = [35], qualifiers = "w780dp-h360dp-land-xhdpi") class PhoneLandscapeScreens : DeviceScreenshots("phoneland")
@Config(sdk = [35], qualifiers = "w800dp-h1280dp-hdpi") class TabletPortraitScreens : DeviceScreenshots("tablet")
@Config(sdk = [35], qualifiers = "w1280dp-h800dp-land-hdpi") class TabletLandscapeScreens : DeviceScreenshots("tabletland")
