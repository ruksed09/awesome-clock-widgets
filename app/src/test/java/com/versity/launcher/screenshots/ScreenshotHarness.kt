package com.versity.launcher.screenshots

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.versity.launcher.data.AppEntry
import com.versity.launcher.data.DrawerStyle
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.data.UiMode
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.domain.withTheme
import com.versity.launcher.domain.lockLook
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.drawer.AppDrawerContent
import com.versity.launcher.ui.drawer.DrawerUiState
import com.versity.launcher.ui.home.HomeContent
import com.versity.launcher.ui.home.HomeUiState
import com.versity.launcher.ui.lock.LockStyleContent
import com.versity.launcher.ui.settings.CustomiseActions
import com.versity.launcher.ui.settings.CustomiseContent
import com.versity.launcher.ui.settings.CustomisePage
import com.versity.launcher.ui.theme.DrawerTheme
import com.versity.launcher.data.ClockPlacement
import com.versity.launcher.data.HomeWidget
import com.versity.launcher.widget.WidgetSpec
import com.versity.launcher.widget.WidgetStore
import com.versity.launcher.widget.WidgetStyle
import com.versity.launcher.widget.WidgetType
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
 * Renders real screens to PNG for design review. Skipped unless VERSITY_SCREENSHOTS names an output
 * folder, so normal test runs and CI never pay for it:
 *
 *     VERSITY_SCREENSHOTS=/tmp/shots ./gradlew testDebugUnitTest --tests '*ScreenshotHarness*'
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h780dp-xhdpi")
class ScreenshotHarness {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    /** Output folder; must be an absolute path, so a blank or relative value can never write somewhere unexpected. */
    private val out: File? = System.getenv("VERSITY_SCREENSHOTS")?.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.isAbsolute }
    private val now = LocalDateTime.of(2026, 11, 11, 11, 11)

    @Before fun onlyWhenAsked() {
        assumeTrue("Set VERSITY_SCREENSHOTS to an absolute folder to render screenshots", out != null)
        out!!.mkdirs()
    }

    private var screen by mutableStateOf<(@Composable () -> Unit)?>(null)

    private fun shoot(name: String, content: @Composable () -> Unit) {
        screen = content
        rule.waitForIdle()
        save(name)
    }

    /** Draws the window straight into a bitmap; Robolectric has no hardware frames to capture. */
    private fun save(name: String) {
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun host() = rule.setContent { screen?.invoke() }

    @Test fun everyThemeOnHomeAndLock() {
        host()
        for (t in ThemePresets.themes) {
            val s = LauncherSettings.DEFAULT.withTheme(t)
            shoot("theme_${t.id}_home") { HomeContent(HomeUiState(settings = s, now = now)) }
            shoot("theme_${t.id}_lock") { LockStyleContent(HomeUiState(settings = s, now = now), onUnlock = {}) }
        }
    }

    @Test fun customisePages() {
        host()
        for (mode in listOf(UiMode.LIGHT, UiMode.DARK)) {
            val s = LauncherSettings.DEFAULT.copy(uiMode = mode)
            for (page in CustomisePage.entries) {
                shoot("customise_${mode.name.lowercase()}_${page.name.lowercase()}") { CustomiseContent(s, object : CustomiseActions {}, initialPage = page, isDefaultLauncher = page != CustomisePage.HUB || mode == UiMode.DARK) }
                screen = null
                rule.waitForIdle()
            }
        }
    }

    @Test fun placedClocks() {
        host()
        val base = LauncherSettings.DEFAULT.withTheme(ThemePresets.theme("stage")!!)
        shoot("placed_home") { HomeContent(HomeUiState(settings = base.copy(clockPlacement = com.versity.launcher.data.ClockPlacement(0.5f, 0.45f, 1.25f)), now = now)) }
        shoot("placed_lock") {
            LockStyleContent(HomeUiState(settings = base.copy(lockClockPlacement = com.versity.launcher.data.ClockPlacement(0.3f, 0.72f, 0.7f)).lockLook(), now = now), onUnlock = {})
        }
    }

    @Test fun clockEditor() {
        val s = LauncherSettings.DEFAULT.withTheme(ThemePresets.theme("frost")!!).copy(uiMode = UiMode.DARK)
        rule.setContent { CustomiseContent(s, object : CustomiseActions {}, initialPage = CustomisePage.LAYOUT) }
        rule.onNodeWithTag(TestTags.EDIT_CLOCK).performClick()
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        save("clock_editor")
    }

    @Test fun widgetStudios() {
        for (mode in listOf(UiMode.LIGHT, UiMode.DARK)) {
            for (type in WidgetType.entries) {
                val s = LauncherSettings.DEFAULT.copy(uiMode = mode)
                rule.activityRule.scenario.recreate()
                rule.setContent { CustomiseContent(s, object : CustomiseActions {}, initialPage = CustomisePage.WIDGETS) }
                rule.onNodeWithTag(TestTags.widgetType(type.name)).performClick()
                rule.mainClock.advanceTimeBy(600)
                rule.waitForIdle()
                save("studio_${type.name.lowercase()}_${mode.name.lowercase()}")
            }
        }
    }

    /** The launcher studio on the spinner, at the top and scrolled to its last options. */
    @Test fun spinnerStudio() {
        for (mode in listOf(UiMode.LIGHT, UiMode.DARK)) {
            val s = LauncherSettings.DEFAULT.copy(uiMode = mode)
            rule.activityRule.scenario.recreate()
            rule.setContent { CustomiseContent(s, object : CustomiseActions {}, initialPage = CustomisePage.WIDGETS) }
            rule.onNodeWithTag(TestTags.widgetType(WidgetType.LAUNCHER.name)).performClick()
            rule.mainClock.advanceTimeBy(600)
            rule.onNodeWithText("Spinner").performScrollTo().performClick()
            rule.mainClock.advanceTimeBy(600)
            rule.waitForIdle()
            save("studio_spinner_${mode.name.lowercase()}")
            rule.onNodeWithText("Soft shadow").performScrollTo()
            rule.mainClock.advanceTimeBy(400)
            rule.waitForIdle()
            save("studio_spinner_options_${mode.name.lowercase()}")
        }
    }

    @Test fun homeWithWidgets() {
        val store = WidgetStore(rule.activity)
        store.saveHome(1, WidgetSpec.default(WidgetType.CLOCK_DATE).copy(style = WidgetStyle.EDITORIAL, typeface = "inter_bold", accentColor = 0xFFFF8A70))
        store.saveHome(2, WidgetSpec.default(WidgetType.ANALOGUE))
        store.saveHome(3, WidgetSpec.default(WidgetType.DATE))
        val widgets = listOf(
            HomeWidget(1, ClockPlacement(0.5f, 0.3f, 1.4f)),
            HomeWidget(2, ClockPlacement(0.28f, 0.6f, 0.72f)),
            HomeWidget(3, ClockPlacement(0.72f, 0.6f, 0.72f)),
        )
        host()
        val s = LauncherSettings.DEFAULT.withTheme(ThemePresets.theme("ink")!!).copy(homeWidgets = widgets, clockPlacement = ClockPlacement(0.5f, 0.06f, 0.5f))
        shoot("home_widgets") { HomeContent(HomeUiState(settings = s, now = now)) }
    }

    @Test fun applySheetLight() = sheet(UiMode.LIGHT, CustomisePage.THEMES, TestTags.theme("moonrise"), "apply_sheet_light")

    @Test fun applySheetDark() = sheet(UiMode.DARK, CustomisePage.THEMES, TestTags.theme("moonrise"), "apply_sheet_dark")

    @Test fun wallpaperSheetLight() = sheet(UiMode.LIGHT, CustomisePage.WALLPAPERS, TestTags.wallpaper("leviathan"), "wallpaper_sheet_light")

    private fun sheet(mode: UiMode, page: CustomisePage, tag: String, name: String) {
        val s = LauncherSettings.DEFAULT.copy(uiMode = mode)
        rule.setContent { CustomiseContent(s, object : CustomiseActions {}, initialPage = page) }
        rule.onNodeWithTag(tag).performClick()
        rule.mainClock.advanceTimeBy(800)
        rule.waitForIdle()
        save(name)
    }

    @Test fun drawers() {
        host()
        val apps = listOf("Calculator", "Calendar", "Camera", "Chrome", "Clock", "Contacts", "Drive", "Files", "Gmail", "Maps", "Messages", "Music", "Photos", "Settings", "Weather", "YouTube")
            .map { AppEntry(it, "com.example.${it.lowercase()}", "Main") }
        val state = DrawerUiState(apps = apps, loading = false)
        val themed = LauncherSettings.DEFAULT.withTheme(ThemePresets.theme("leviathan")!!)
        for (mode in listOf(UiMode.LIGHT, UiMode.DARK)) {
            val p = themed.copy(uiMode = mode)
            shoot("drawer_plain_${mode.name.lowercase()}") { DrawerTheme(p) { AppDrawerContent(state, {}, {}, settings = p) } }
        }
        val t = themed.copy(drawerStyle = DrawerStyle.THEMED)
        shoot("drawer_themed") { DrawerTheme(t) { AppDrawerContent(state, {}, {}, settings = t) } }
    }
}
