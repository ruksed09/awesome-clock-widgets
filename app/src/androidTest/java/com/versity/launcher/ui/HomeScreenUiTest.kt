package com.versity.launcher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.versity.launcher.data.AppEntry
import com.versity.launcher.ui.home.HomeActions
import androidx.compose.ui.test.swipeUp
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.HomeLayout
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.ui.home.HomeContent
import com.versity.launcher.ui.home.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenUiTest {
    @get:Rule val rule = createComposeRule()

    private val tags = mapOf(
        ClockStyle.ORNAMENT to TestTags.CLOCK_ORNAMENT,
        ClockStyle.ANALOGUE to TestTags.CLOCK_ANALOGUE,
        ClockStyle.GIANT to TestTags.clockFace(ClockStyle.GIANT.name),
    )

    private fun assertOnly(style: ClockStyle) {
        tags.forEach { (s, tag) ->
            if (s == style) rule.onNodeWithTag(tag).assertExists()
            else rule.onNodeWithTag(tag).assertDoesNotExist()
        }
    }

    @Test fun clockStyleSwitchChangesRenderedClock() {
        var state by mutableStateOf(HomeUiState(LauncherSettings.DEFAULT.copy(clockStyle = ClockStyle.ORNAMENT)))
        rule.setContent { HomeContent(state) }
        assertOnly(ClockStyle.ORNAMENT)
        for (style in listOf(ClockStyle.GIANT, ClockStyle.ANALOGUE, ClockStyle.ORNAMENT)) {
            state = state.copy(settings = state.settings.copy(clockStyle = style))
            rule.waitForIdle()
            assertOnly(style)
        }
    }

    @Test fun dimOverlayAlphaMatchesSetting() {
        var state by mutableStateOf(HomeUiState(LauncherSettings.DEFAULT.copy(dimPercent = 40)))
        rule.setContent { HomeContent(state) }
        fun alpha() = rule.onNodeWithTag(TestTags.DIM_OVERLAY).fetchSemanticsNode().config[DimAlphaKey]
        assertEquals(0.40f, alpha(), 0.001f)
        state = state.copy(settings = state.settings.copy(dimPercent = 80))
        rule.waitForIdle()
        assertEquals(0.80f, alpha(), 0.001f)
    }

    private val apps = listOf(
        AppEntry("Camera", "p.cam", "p.cam.A"),
        AppEntry("Music", "p.mus", "p.mus.A"),
    )

    @Test fun favouritesSitInTheDock() {
        rule.setContent { HomeContent(HomeUiState(favourites = apps)) }
        apps.forEach { rule.onNodeWithTag(TestTags.dockApp(it.key)).assertExists() }
        rule.onNodeWithTag(TestTags.DOCK_APPS).assertExists()
    }

    @Test fun bothLayoutsKeepTheDock() {
        var state by mutableStateOf(HomeUiState(settings = LauncherSettings.DEFAULT.copy(layout = HomeLayout.MINIMAL)))
        rule.setContent { HomeContent(state) }
        rule.onNodeWithTag(TestTags.DOCK).assertExists()
        state = state.copy(settings = state.settings.copy(layout = HomeLayout.ORBIT))
        rule.waitForIdle()
        rule.onNodeWithTag(TestTags.DOCK).assertExists()
    }

    @Test fun swipingUpOpensAllApps() {
        var opened = 0
        rule.setContent { HomeContent(HomeUiState(), actions = object : HomeActions() { override fun openDrawer() { opened++ } }) }
        rule.onNodeWithTag(TestTags.DIM_OVERLAY).performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(1, opened)
    }

    @Test fun longPressingADockAppDoesNotOpenCustomise() {
        var opened = 0
        rule.setContent { HomeContent(HomeUiState(favourites = apps), actions = object : HomeActions() { override fun openCustomise() { opened++ } }) }
        rule.onNodeWithTag(TestTags.dockApp(apps[0].key)).performTouchInput { longClick() }
        rule.waitForIdle()
        assertEquals(0, opened)
    }

    @Test fun nothingThemedIsDrawnBeforeSettingsLoad() {
        rule.setContent { HomeContent(HomeUiState(loaded = false)) }
        tags.values.forEach { rule.onNodeWithTag(it).assertDoesNotExist() }
        rule.onNodeWithTag(TestTags.DOCK_APPS).assertDoesNotExist()
    }
}
