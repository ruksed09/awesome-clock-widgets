package com.versity.launcher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.ui.settings.CustomiseActions
import com.versity.launcher.ui.settings.CustomiseContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CustomiseScreenUiTest {
    @get:Rule val rule = createComposeRule()

    private fun previewPalette() =
        rule.onNodeWithTag(TestTags.PREVIEW).fetchSemanticsNode().config[PreviewPaletteKey]

    private fun host(): () -> LauncherSettings {
        var settings by mutableStateOf(LauncherSettings.DEFAULT)
        val actions = object : CustomiseActions {
            override fun onPalette(id: String) { settings = settings.copy(paletteId = id) }
            override fun onReset() { settings = LauncherSettings.DEFAULT }
        }
        rule.setContent { CustomiseContent(settings, actions) }
        return { settings }
    }

    @Test fun selectingAPresetUpdatesThePreview() {
        val settings = host()
        assertEquals(LauncherSettings.DEFAULT.paletteId, previewPalette())
        rule.onNodeWithTag(TestTags.palette("ocean")).performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals("ocean", settings().paletteId)
        assertEquals("ocean", previewPalette())
    }

    @Test fun resetRestoresDefaults() {
        val settings = host()
        rule.onNodeWithTag(TestTags.palette("ember")).performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals("ember", previewPalette())
        rule.onNodeWithTag(TestTags.RESET).performClick()
        rule.waitForIdle()
        assertEquals(LauncherSettings.DEFAULT, settings())
        assertEquals(LauncherSettings.DEFAULT.paletteId, previewPalette())
    }

    @Test fun previewLockButtonCallsTheAction() {
        var previews = 0
        val actions = object : CustomiseActions { override fun onPreviewLock() { previews++ } }
        rule.setContent { CustomiseContent(LauncherSettings.DEFAULT, actions) }
        rule.onNodeWithTag(TestTags.PREVIEW_LOCK).performScrollTo().performClick()
        assertEquals(1, previews)
    }

    @Test fun setLockWallpaperOpensDialogWithoutApplyingYet() {
        var applied = 0
        val actions = object : CustomiseActions { override fun onSetLockWallpaper() { applied++ } }
        rule.setContent { CustomiseContent(LauncherSettings.DEFAULT, actions) }
        rule.onNodeWithTag(TestTags.SET_LOCK_WALLPAPER).performScrollTo().performClick()
        rule.onNodeWithTag(TestTags.LOCK_WALLPAPER_CONFIRM).assertExists()
        assertEquals(0, applied)
    }

    @Test fun confirmingTheDialogAppliesOnce() {
        var applied = 0
        val actions = object : CustomiseActions { override fun onSetLockWallpaper() { applied++ } }
        rule.setContent { CustomiseContent(LauncherSettings.DEFAULT, actions) }
        rule.onNodeWithTag(TestTags.SET_LOCK_WALLPAPER).performScrollTo().performClick()
        rule.onNodeWithTag(TestTags.LOCK_WALLPAPER_CONFIRM).performClick()
        rule.waitForIdle()
        assertEquals(1, applied)
        rule.onNodeWithTag(TestTags.LOCK_WALLPAPER_CONFIRM).assertDoesNotExist()
    }

    @Test fun cancellingTheDialogAppliesNothing() {
        var applied = 0
        val actions = object : CustomiseActions { override fun onSetLockWallpaper() { applied++ } }
        rule.setContent { CustomiseContent(LauncherSettings.DEFAULT, actions) }
        rule.onNodeWithTag(TestTags.SET_LOCK_WALLPAPER).performScrollTo().performClick()
        rule.onNodeWithTag(TestTags.LOCK_WALLPAPER_CANCEL).performClick()
        rule.waitForIdle()
        assertEquals(0, applied)
    }
}
