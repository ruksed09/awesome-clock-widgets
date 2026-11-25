package com.versity.launcher.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.versity.launcher.data.ClockPlacement
import com.versity.launcher.data.HomeWidget
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.studio.StudioTheme
import com.versity.launcher.widget.WidgetSpec
import com.versity.launcher.widget.WidgetStore
import com.versity.launcher.widget.WidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Moving widgets on Versity's home screen: a drag on a widget moves that widget, all the way up or down. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h800dp")
class ArrangeEditorTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun edit(widgets: List<HomeWidget>, clock: ClockPlacement? = ClockPlacement.DEFAULT): () -> Triple<ClockPlacement?, ClockPlacement?, List<HomeWidget>>? {
        val store = WidgetStore(ApplicationProvider.getApplicationContext())
        widgets.forEach { store.saveHome(it.id, WidgetSpec.default(WidgetType.DIGITAL)) }
        var result: Triple<ClockPlacement?, ClockPlacement?, List<HomeWidget>>? = null
        val settings = LauncherSettings.DEFAULT.copy(homeWidgets = widgets, clockPlacement = clock)
        rule.setContent {
            StudioTheme(settings.uiMode) {
                ArrangeEditor(settings, onDone = { h, l, w -> result = Triple(h, l, w) }, onCancel = {})
            }
        }
        rule.waitForIdle()
        return { rule.onNodeWithTag(TestTags.EDITOR_DONE).performClick(); rule.waitForIdle(); result }
    }

    private fun drag(from: Offset, by: Offset) = rule.onNodeWithTag(TestTags.CLOCK_EDITOR).performTouchInput {
        val start = Offset(from.x * width, from.y * height)
        down(start)
        for (k in 1..20) moveTo(start + by * (k / 20f) * height.toFloat(), delayMillis = 16)
        up()
    }

    @Test fun draggingAWidgetMovesItDown() {
        val done = edit(listOf(HomeWidget(1, ClockPlacement(0.5f, 0.52f, 0.8f))))
        drag(Offset(0.5f, 0.52f), Offset(0f, 0.3f))
        val w = done()!!.third.single().placement
        assertTrue("moved to ${w.y}", w.y > 0.72f)
    }

    @Test fun draggingAWidgetMovesItUp() {
        val done = edit(listOf(HomeWidget(1, ClockPlacement(0.5f, 0.70f, 0.8f))))
        drag(Offset(0.5f, 0.70f), Offset(0f, -0.3f))
        val w = done()!!.third.single().placement
        assertTrue("moved to ${w.y}", w.y < 0.5f)
    }

    @Test fun aWidgetUnderTheClockAreaCanStillBeMoved() {
        // A wide widget just below the clock: grabbing its top half must move the widget, not the clock.
        val done = edit(listOf(HomeWidget(1, ClockPlacement(0.5f, 0.38f, 1.4f))))
        drag(Offset(0.5f, 0.33f), Offset(0f, 0.3f))
        val (clock, _, widgets) = done()!!
        assertEquals(ClockPlacement.DEFAULT, clock)
        assertTrue("moved to ${widgets.single().placement.y}", widgets.single().placement.y > 0.55f)
    }

    @Test fun aWidgetPushedPastTheBottomComesBackAtOnce() {
        val done = edit(listOf(HomeWidget(1, ClockPlacement(0.5f, 0.6f, 0.8f))))
        rule.onNodeWithTag(TestTags.CLOCK_EDITOR).performTouchInput {
            val start = Offset(0.5f * width, 0.6f * height)
            down(start)
            // Far past the bottom edge, then back up by a tenth of the screen.
            moveTo(start + Offset(0f, height * 0.6f), delayMillis = 16)
            moveTo(start + Offset(0f, height * 0.5f), delayMillis = 16)
            up()
        }
        val y = done()!!.third.single().placement.y
        // Without the dead zone it moved up straight away instead of sitting at the edge.
        assertTrue("ended at $y", y < 0.86f)
    }
}
