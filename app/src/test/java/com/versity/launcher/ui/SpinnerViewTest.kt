package com.versity.launcher.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.versity.launcher.ui.home.components.SpinnerView
import com.versity.launcher.widget.WidgetSpec
import com.versity.launcher.widget.WidgetStyle
import com.versity.launcher.widget.WidgetType
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The live spinner, driven by touch: the hub turns it a step, a finger turns it round. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SpinnerViewTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val spec = WidgetSpec.default(WidgetType.LAUNCHER).copy(style = WidgetStyle.RADIAL_SPINNER)

    private fun show(spinnable: Boolean, turns: MutableList<Int>) = rule.setContent {
        SpinnerView(spec, Modifier.size(240.dp).testTag("spinner"), spinnable = spinnable, onTurn = { turns += it })
    }

    @Test fun tappingTheHubTurnsOneStep() {
        val turns = mutableListOf<Int>()
        show(true, turns)
        rule.onNodeWithTag("spinner").performTouchInput { click(center) }
        rule.mainClock.advanceTimeBy(3_000)
        rule.waitForIdle()
        assertEquals(listOf(1), turns)
    }

    @Test fun draggingRoundTheWheelTurnsIt() {
        val turns = mutableListOf<Int>()
        show(true, turns)
        rule.onNodeWithTag("spinner").performTouchInput {
            // A slow quarter turn clockwise along the track, so there is no flick.
            val r = width * 0.33f
            down(Offset(center.x, center.y - r))
            for (k in 1..30) {
                val a = Math.toRadians(-90.0 + k * 3.0)
                moveTo(Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat()), delayMillis = 40)
            }
            advanceEventTime(300)
            up()
        }
        rule.mainClock.advanceTimeBy(3_000)
        rule.waitForIdle()
        // 90 degrees is two steps.
        assertEquals(listOf(2), turns)
    }

    @Test fun aStillSpinnerIgnoresTouch() {
        val turns = mutableListOf<Int>()
        show(false, turns)
        rule.onNodeWithTag("spinner").performTouchInput { click(center) }
        rule.mainClock.advanceTimeBy(1_000)
        assertTrue(turns.isEmpty())
    }
}
