package com.versity.launcher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.data.UiMode
import com.versity.launcher.ui.settings.ApplyTarget
import com.versity.launcher.ui.settings.CustomiseActions
import com.versity.launcher.ui.settings.CustomiseContent
import com.versity.launcher.ui.settings.CustomisePage
import com.versity.launcher.ui.settings.ThemeApplyRequest
import com.versity.launcher.ui.settings.WallpaperApplyRequest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CustomiseScreenUiTest {
    @get:Rule val rule = createComposeRule()

    private fun previewPalette() =
        rule.onNodeWithTag(TestTags.PREVIEW).fetchSemanticsNode().config[PreviewPaletteKey]

    private fun host(page: CustomisePage): () -> LauncherSettings {
        var settings by mutableStateOf(LauncherSettings.DEFAULT)
        val actions = object : CustomiseActions {
            override fun onPalette(id: String) { settings = settings.copy(paletteId = id) }
            override fun onReset() { settings = LauncherSettings.DEFAULT }
        }
        rule.setContent { CustomiseContent(settings, actions, initialPage = page) }
        return { settings }
    }

    @Test fun hubOpensEachSection() {
        rule.setContent { CustomiseContent(LauncherSettings.DEFAULT, object : CustomiseActions {}) }
        rule.onNodeWithTag(TestTags.nav(CustomisePage.THEMES.name)).performClick()
        rule.onNodeWithTag(TestTags.theme("leviathan")).assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithTag(TestTags.nav(CustomisePage.WALLPAPERS.name)).performClick()
        rule.onNodeWithTag(TestTags.wallpaper("moon_tide")).assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithTag(TestTags.nav(CustomisePage.MORE.name)).performClick()
        rule.onNodeWithTag(TestTags.nav(CustomisePage.STYLE.name)).performClick()
        rule.onNodeWithTag(TestTags.PREVIEW_FAB).assertExists()
    }

    @Test fun selectingAPaletteUpdatesTheLiveLook() {
        val settings = host(CustomisePage.STYLE)
        assertEquals(LauncherSettings.DEFAULT.paletteId, previewPalette())
        rule.onNodeWithTag(TestTags.palette("ocean")).performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals("ocean", settings().paletteId)
        assertEquals("ocean", previewPalette())
    }

    @Test fun resetAsksThenRestoresDefaults() {
        val settings = host(CustomisePage.STYLE)
        rule.onNodeWithTag(TestTags.palette("ember")).performScrollTo().performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithTag(TestTags.RESET).performScrollTo().performClick()
        rule.onNodeWithTag(TestTags.RESET_CONFIRM).performClick()
        rule.waitForIdle()
        assertEquals(LauncherSettings.DEFAULT, settings())
    }

    @Test fun resetCanBeCancelled() {
        var resets = 0
        val actions = object : CustomiseActions { override fun onReset() { resets++ } }
        rule.setContent { CustomiseContent(LauncherSettings.DEFAULT, actions, initialPage = CustomisePage.MORE) }
        rule.onNodeWithTag(TestTags.RESET).performScrollTo().performClick()
        rule.onNodeWithTag(TestTags.RESET_CANCEL).performClick()
        rule.waitForIdle()
        assertEquals(0, resets)
    }

    @Test fun previewLockButtonCallsTheAction() {
        var previews = 0
        val actions = object : CustomiseActions { override fun onPreviewLock() { previews++ } }
        rule.setContent { CustomiseContent(LauncherSettings.DEFAULT, actions, initialPage = CustomisePage.LOCK) }
        rule.onNodeWithTag(TestTags.PREVIEW_LOCK).performScrollTo().performClick()
        assertEquals(1, previews)
    }

    @Test fun lockWallpaperAsksFirstAndAppliesOnce() {
        var applied = 0
        val actions = object : CustomiseActions { override fun onSetLockWallpaper() { applied++ } }
        rule.setContent { CustomiseContent(LauncherSettings.DEFAULT, actions, initialPage = CustomisePage.LOCK) }
        rule.onNodeWithTag(TestTags.SET_LOCK_WALLPAPER).performScrollTo().performClick()
        assertEquals(0, applied)
        rule.onNodeWithTag(TestTags.LOCK_WALLPAPER_CONFIRM).performClick()
        rule.waitForIdle()
        assertEquals(1, applied)
        rule.onNodeWithTag(TestTags.LOCK_WALLPAPER_CONFIRM).assertDoesNotExist()
    }

    @Test fun cancellingTheLockWallpaperDialogAppliesNothing() {
        var applied = 0
        val actions = object : CustomiseActions { override fun onSetLockWallpaper() { applied++ } }
        rule.setContent { CustomiseContent(LauncherSettings.DEFAULT, actions, initialPage = CustomisePage.LOCK) }
        rule.onNodeWithTag(TestTags.SET_LOCK_WALLPAPER).performScrollTo().performClick()
        rule.onNodeWithTag(TestTags.LOCK_WALLPAPER_CANCEL).performClick()
        rule.waitForIdle()
        assertEquals(0, applied)
    }

    private var lastTheme: ThemeApplyRequest? = null
    private var lastWallpaper: WallpaperApplyRequest? = null
    private var lastMode: UiMode? = null

    @Test fun applyingAThemeToTheLockScreenOnlySendsThatChoice() {
        val actions = object : CustomiseActions { override fun onApplyTheme(request: ThemeApplyRequest) { lastTheme = request } }
        rule.setContent { CustomiseContent(LauncherSettings.DEFAULT, actions, initialPage = CustomisePage.THEMES) }
        rule.onNodeWithTag(TestTags.theme("celestial")).performScrollTo().performClick()
        rule.onNodeWithTag(TestTags.applyTarget(ApplyTarget.LOCK.name)).performClick()
        rule.onNodeWithTag(TestTags.OPT_LOCK_WALLPAPER).performScrollTo().performClick()
        rule.onNodeWithTag(TestTags.APPLY_CONFIRM).performClick()
        rule.waitForIdle()
        assertEquals("celestial", lastTheme?.theme?.id)
        assertEquals(ApplyTarget.LOCK, lastTheme?.target)
        assertEquals(false, lastTheme?.setPhoneWallpaper)
        assertEquals(false, lastTheme?.setLockWallpaper)
        assertEquals(true, lastTheme?.showLockScreen)
        rule.onNodeWithTag(TestTags.APPLY_SHEET).assertDoesNotExist()
    }

    @Test fun cancellingTheThemeSheetAppliesNothing() {
        var applied = 0
        val actions = object : CustomiseActions { override fun onApplyTheme(request: ThemeApplyRequest) { applied++ } }
        rule.setContent { CustomiseContent(LauncherSettings.DEFAULT, actions, initialPage = CustomisePage.THEMES) }
        rule.onNodeWithTag(TestTags.theme("noir")).performScrollTo().performClick()
        rule.onNodeWithTag(TestTags.APPLY_CANCEL).performClick()
        rule.waitForIdle()
        assertEquals(0, applied)
    }

    @Test fun settingAWallpaperOnTheLockScreenSendsThatChoice() {
        val actions = object : CustomiseActions { override fun onApplyWallpaper(request: WallpaperApplyRequest) { lastWallpaper = request } }
        rule.setContent { CustomiseContent(LauncherSettings.DEFAULT, actions, initialPage = CustomisePage.WALLPAPERS) }
        rule.onNodeWithTag(TestTags.wallpaper("moon_tide")).performScrollTo().performClick()
        rule.onNodeWithTag(TestTags.applyTarget(ApplyTarget.LOCK.name)).performClick()
        rule.onNodeWithTag(TestTags.APPLY_CONFIRM).performClick()
        rule.waitForIdle()
        assertEquals("moon_tide", lastWallpaper?.wallpaper?.id)
        assertEquals(ApplyTarget.LOCK, lastWallpaper?.target)
        assertEquals(false, lastWallpaper?.setPhoneWallpaper)
        assertEquals(true, lastWallpaper?.setLockWallpaper)
    }

    @Test fun themeToggleSwitchesToTheOtherMode() {
        val actions = object : CustomiseActions { override fun onUiMode(mode: UiMode) { lastMode = mode } }
        rule.setContent { CustomiseContent(LauncherSettings.DEFAULT.copy(uiMode = UiMode.LIGHT), actions) }
        rule.onNodeWithTag(TestTags.THEME_TOGGLE).performClick()
        rule.waitForIdle()
        assertEquals(UiMode.DARK, lastMode)
    }

    @Test fun clockEditorSavesBothPlacementsOnDone() {
        var saves = 0
        val actions = object : CustomiseActions {
            override fun onClockPlacement(home: com.versity.launcher.data.ClockPlacement?, lock: com.versity.launcher.data.ClockPlacement?) { saves++ }
        }
        rule.setContent { CustomiseContent(LauncherSettings.DEFAULT, actions, initialPage = CustomisePage.STYLE) }
        rule.onNodeWithTag(TestTags.EDIT_CLOCK).performScrollTo().performClick()
        rule.onNodeWithTag(TestTags.CLOCK_EDITOR).assertExists()
        rule.onNodeWithTag(TestTags.EDITOR_DONE).performClick()
        rule.waitForIdle()
        assertEquals(1, saves)
        rule.onNodeWithTag(TestTags.CLOCK_EDITOR).assertDoesNotExist()
    }
}
