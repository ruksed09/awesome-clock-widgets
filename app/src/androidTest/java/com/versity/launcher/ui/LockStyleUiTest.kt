package com.versity.launcher.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.versity.launcher.data.Backdrop
import com.versity.launcher.data.HomeLayout
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.ui.home.HomeUiState
import com.versity.launcher.ui.lock.LockStyleContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LockStyleUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun showsLockScreenWithSlideHandle() {
        rule.setContent { LockStyleContent(HomeUiState(), onUnlock = {}) }
        rule.onNodeWithTag(TestTags.LOCK_SCREEN).assertExists()
        rule.onNodeWithTag(TestTags.LOCK_HANDLE).assertExists()
    }

    @Test fun swipingUpUnlocks() {
        var unlocked = 0
        rule.setContent { LockStyleContent(HomeUiState(), onUnlock = { unlocked++ }) }
        rule.onNodeWithTag(TestTags.LOCK_SCREEN).performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(1, unlocked)
    }

    @Test fun smallDragDoesNotUnlock() {
        var unlocked = 0
        rule.setContent { LockStyleContent(HomeUiState(), onUnlock = { unlocked++ }) }
        rule.onNodeWithTag(TestTags.LOCK_SCREEN).performTouchInput {
            down(center); moveBy(androidx.compose.ui.geometry.Offset(0f, -20f)); up()
        }
        rule.waitForIdle()
        assertEquals(0, unlocked)
    }

    private fun lock(settings: LauncherSettings) =
        rule.setContent { LockStyleContent(HomeUiState(settings = settings), onUnlock = {}) }

    @Test fun lockDrawsTheBackdropOfItsSettings() {
        lock(LauncherSettings.DEFAULT.copy(backdrop = Backdrop.STARS))
        rule.onNodeWithTag(TestTags.backdrop("STARS")).assertExists()
    }

    @Test fun anotherBackdropReplacesIt() {
        lock(LauncherSettings.DEFAULT.copy(backdrop = Backdrop.RINGS))
        rule.onNodeWithTag(TestTags.backdrop("RINGS")).assertExists()
        rule.onNodeWithTag(TestTags.backdrop("STARS")).assertDoesNotExist()
    }

    @Test fun minimalLockHidesTheBatteryBar() {
        lock(LauncherSettings.DEFAULT.copy(layout = HomeLayout.MINIMAL))
        rule.onNodeWithTag(TestTags.LOCK_HANDLE).assertExists()
        rule.onNodeWithTag(TestTags.BATTERY_BAR).assertDoesNotExist()
    }

    @Test fun classicAndOrbitLockShowTheBatteryBar() {
        lock(LauncherSettings.DEFAULT.copy(layout = HomeLayout.CLASSIC))
        rule.onNodeWithTag(TestTags.BATTERY_BAR).assertExists()
    }
}
