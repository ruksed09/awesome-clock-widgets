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

    @Test fun spinnerPutsTheTurnedAppUnderThePointer() {
        // App i sits at i steps clockwise plus the turn; the one at twelve is fully in focus.
        for (turn in 0 until CollectionArt.SPIN_SLOTS) {
            val top = (0 until CollectionArt.SPIN_SLOTS).maxBy { CollectionArt.focusOf(CollectionArt.spinAngle(it, turn * CollectionArt.SPIN_STEP)) }
            assertEquals((CollectionArt.SPIN_SLOTS - turn) % CollectionArt.SPIN_SLOTS, top)
        }
        assertEquals(1f, CollectionArt.focusOf(720f), 0.0001f)
        assertEquals(0f, CollectionArt.focusOf(CollectionArt.SPIN_STEP), 0.0001f)
    }

    @Test fun spinnerPlateFollowsTheBackgroundAndStaysInside() {
        val edge = (size * 0.06f).toInt() // just inside the plate's rim at twelve
        for (bg in WidgetBackground.entries) {
            val art = CollectionArt.radial(context, launcher(WidgetStyle.RADIAL_SPINNER, bg), size)
            val a = Color.alpha(art.getPixel(size / 2 + size / 10, edge))
            when (bg) {
                WidgetBackground.SOLID -> assertEquals(255, a)
                WidgetBackground.GLASS -> assertTrue("$a", a in 20..140)
                WidgetBackground.NONE -> assertEquals(0, a)
            }
            // Nothing is drawn in the margin, so no shadow or edge is ever cut off.
            for (x in listOf(0, size - 1)) for (y in listOf(0, size / 2, size - 1)) assertEquals(0, Color.alpha(art.getPixel(x, y)))
        }
    }

    @Test fun shapedOutlinesAreClosedAndHoldEveryCircle() {
        val c = 150f
        val d = 90f
        val r = 42f
        for (path in listOf(CollectionArt.flowerPath(c, d, r), CollectionArt.crossPath(c, d, r * 1.06f))) {
            val region = android.graphics.Region().apply { setPath(path, android.graphics.Region(0, 0, 300, 300)) }
            // The centre, each circle's centre and the inner side of each outer circle are inside.
            for ((x, y) in listOf(c to c, c to c - d, c + d to c, c to c + d, c - d to c, c to c - d + r * 0.9f)) {
                assertTrue("$x,$y", region.contains(x.toInt(), y.toInt()))
            }
            // The corners are outside.
            assertTrue(!region.contains(20, 20) && !region.contains(280, 280))
        }
    }
}
