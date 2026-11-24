package com.versity.launcher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.versity.launcher.data.AppEntry
import com.versity.launcher.data.Backdrop
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
        ClockStyle.STACKED to TestTags.CLOCK_STACKED,
        ClockStyle.SINGLE_LINE to TestTags.CLOCK_SINGLE_LINE,
        ClockStyle.ANALOGUE to TestTags.CLOCK_ANALOGUE,
    )

    private fun assertOnly(style: ClockStyle) {
        tags.forEach { (s, tag) ->
            if (s == style) rule.onNodeWithTag(tag).assertExists()
            else rule.onNodeWithTag(tag).assertDoesNotExist()
        }
    }

    @Test fun clockStyleSwitchChangesRenderedClock() {
        var state by mutableStateOf(HomeUiState(LauncherSettings.DEFAULT.copy(clockStyle = ClockStyle.STACKED)))
        rule.setContent { HomeContent(state) }
        assertOnly(ClockStyle.STACKED)
        for (style in listOf(ClockStyle.SINGLE_LINE, ClockStyle.ANALOGUE, ClockStyle.STACKED)) {
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
        state = state.copy(settings = state.settings.copy(dimPercent = 0))
        rule.waitForIdle()
        assertEquals(0.0f, alpha(), 0.001f)
    }

    private val apps = listOf(
        AppEntry("Camera", "p.cam", "p.cam.A"),
        AppEntry("Music", "p.mus", "p.mus.A"),
    )

    @Test fun favouritesAreShownOnHome() {
        rule.setContent { HomeContent(HomeUiState(favourites = apps)) }
        apps.forEach { rule.onNodeWithTag(TestTags.favourite(it.key)).assertExists() }
    }

    @Test fun minimalLayoutHidesWidgetsButKeepsFavouritesAndDock() {
        val s = LauncherSettings.DEFAULT.copy(layout = HomeLayout.MINIMAL)
        rule.setContent { HomeContent(HomeUiState(settings = s, favourites = apps)) }
        rule.onNodeWithTag(TestTags.BATTERY_BAR).assertDoesNotExist()
        rule.onNodeWithTag(TestTags.DATE_RING).assertDoesNotExist()
        rule.onNodeWithTag(TestTags.WIDGET_ANALOGUE).assertDoesNotExist()
        rule.onNodeWithTag(TestTags.favourite(apps[0].key)).assertExists()
        rule.onNodeWithTag(TestTags.DOCK_APPS).assertExists()
    }

    @Test fun classicLayoutShowsWidgets() {
        val s = LauncherSettings.DEFAULT.copy(layout = HomeLayout.CLASSIC)
        rule.setContent { HomeContent(HomeUiState(settings = s)) }
        rule.onNodeWithTag(TestTags.BATTERY_BAR).assertExists()
        rule.onNodeWithTag(TestTags.DATE_RING).assertExists()
    }

    @Test fun longPressingAFavouriteDoesNotOpenCustomise() {
        var opened = 0
        rule.setContent { HomeContent(HomeUiState(favourites = apps), onOpenCustomise = { opened++ }) }
        rule.onNodeWithTag(TestTags.favourite(apps[0].key)).performTouchInput { longClick() }
        rule.waitForIdle()
        assertEquals(0, opened)
    }

    @Test fun nothingThemedIsDrawnBeforeSettingsLoad() {
        rule.setContent { HomeContent(HomeUiState(loaded = false)) }
        tags.values.forEach { rule.onNodeWithTag(it).assertDoesNotExist() }
        rule.onNodeWithTag(TestTags.DOCK_APPS).assertDoesNotExist()
    }

    @Test fun homeDrawsTheBackdropOfTheSettings() {
        val s = LauncherSettings.DEFAULT.copy(backdrop = Backdrop.HORIZON)
        rule.setContent { HomeContent(HomeUiState(settings = s)) }
        rule.onNodeWithTag(TestTags.backdrop("HORIZON")).assertExists()
        rule.onNodeWithTag(TestTags.backdrop("STARS")).assertDoesNotExist()
    }
}
