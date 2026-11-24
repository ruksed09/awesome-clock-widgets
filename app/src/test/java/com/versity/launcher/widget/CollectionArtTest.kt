package com.versity.launcher.widget

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The radial launcher's shapes, drawn for real: the backdrop appears, fades or goes as chosen. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class CollectionArtTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val size = 300

    private fun launcher(style: WidgetStyle, bg: WidgetBackground) =
        WidgetSpec.default(WidgetType.LAUNCHER).copy(style = style, background = bg, shadow = false, opacity = 100)

    /** A point between the top and right circles: on the backdrop for every shape but separate rings. */
    private fun between(style: WidgetStyle, bg: WidgetBackground): Int =
        CollectionArt.radial(context, launcher(style, bg), size).getPixel((size * 0.62f).toInt(), (size * 0.38f).toInt())

    @Test fun backdropFollowsTheBackgroundSetting() {
        val shaped = listOf(WidgetStyle.RADIAL_FLOWER, WidgetStyle.RADIAL_ORBIT, WidgetStyle.RADIAL_CROSS, WidgetStyle.RADIAL_TILE)
        for (style in shaped) {
            assertEquals("$style solid", 255, Color.alpha(between(style, WidgetBackground.SOLID)))
            assertEquals("$style none", 0, Color.alpha(between(style, WidgetBackground.NONE)))
            val glass = Color.alpha(between(style, WidgetBackground.GLASS))
            assertTrue("$style glass $glass", glass in 20..120)
        }
    }

    @Test fun tileFillsItsCornersAndOrbitDoesNot() {
        val tile = CollectionArt.radial(context, launcher(WidgetStyle.RADIAL_TILE, WidgetBackground.SOLID), size)
        val orbit = CollectionArt.radial(context, launcher(WidgetStyle.RADIAL_ORBIT, WidgetBackground.SOLID), size)
        val p = (size * 0.12f).toInt()
        assertEquals(255, Color.alpha(tile.getPixel(p, p)))
        assertEquals(0, Color.alpha(orbit.getPixel(p, p)))
    }

    @Test fun everyStyleDrawsWithAndWithoutRings() {
        for (style in WidgetStyle.of(WidgetType.LAUNCHER)) for (rings in 0..3) for (bg in WidgetBackground.entries) {
            val spec = launcher(style, bg).copy(rings = rings, roundIcons = rings % 2 == 0, featured = rings > 1)
            val art = CollectionArt.radial(context, spec, size)
            // The centre well is always drawn.
            assertEquals("$style $rings $bg", 255, Color.alpha(art.getPixel(size / 2, size / 2)))
        }
    }
}
