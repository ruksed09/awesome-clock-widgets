package com.versity.launcher.widget

import com.versity.launcher.data.ClockPlacement
import com.versity.launcher.data.HomeWidget
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSpecTest {
    @Test fun everyStyleRoundTripsThroughStorage() {
        for (style in WidgetStyle.entries) {
            val spec = WidgetSpec(
                style = style, typeface = "syne_bold", textColor = 0xFF123456, accentColor = 0xFFABCDEF, twoTone = true,
                ornament = WidgetOrnament.MOON, background = WidgetBackground.SOLID, backgroundColor = 0xFF0E2A2E, opacity = 37,
                corner = WidgetCorner.SMALL, border = true, align = WidgetAlign.END, timeFormat = TimeFormat.H24,
                dateStyle = DateStyle.SHORT, dialFace = true, seconds = true,
                hands = HandStyle.BOLD, faceShape = FaceShape.SQUARE,
            )
            assertEquals(spec, WidgetSpec.decode(spec.encode(), style.type))
        }
    }

    @Test fun missingOrBrokenValuesFallBackToTheDefaults() {
        for (type in WidgetType.entries) {
            assertEquals(WidgetSpec.default(type), WidgetSpec.decode(null, type))
            assertEquals(WidgetSpec.default(type), WidgetSpec.decode("", type))
            assertEquals(WidgetSpec.default(type), WidgetSpec.decode("style=NOPE|font=gone|text=zz|opacity=x", type))
        }
        // Opacity is clamped, and a style from another family is ignored.
        val odd = WidgetSpec.decode("style=MONTH|opacity=400", WidgetType.DIGITAL)
        assertEquals(WidgetSpec.default(WidgetType.DIGITAL).style, odd.style)
        assertEquals(100, odd.opacity)
    }

    @Test fun everyFamilyHasStylesAndADefaultFromIt() {
        for (type in WidgetType.entries) {
            assertTrue(WidgetStyle.of(type).size >= if (type.collection) 2 else 3)
            assertEquals(type, WidgetSpec.default(type).type)
        }
    }

    @Test fun placedCelestialWidgetsKeepTheirLook() {
        // Widgets placed by 0.2 and 0.3 have no saved look; their families' defaults are the Celestial design.
        for (type in listOf(WidgetType.DIGITAL, WidgetType.CLOCK_DATE)) {
            val d = WidgetSpec.default(type)
            assertEquals("cormorant_light", d.typeface)
            assertEquals(0xFFDCEBFA, d.textColor)
            assertTrue(d.usesOrnament)
            assertEquals(WidgetOrnament.STAR, d.ornament)
        }
        assertTrue(WidgetSpec.default(WidgetType.CLOCK_DATE).showsDate)
    }

    @Test fun everyTypefaceHasAGeneratedLayoutAndAFont() {
        val ids = WidgetTypefaces.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        for (tf in WidgetTypefaces.all) {
            assertTrue("${tf.id} layout missing", File("src/main/res/layout/${tf.layoutName}.xml").exists())
            assertTrue("${tf.id} weight", tf.weight in 100..900)
        }
        assertEquals(ids.size, ids.map { WidgetRenderer.textLayout(it) }.toSet().size)
        val generated = File("src/main/res/layout").listFiles()!!.map { it.name }.filter { it.startsWith("widget_text_") }
        assertEquals(ids.map { "widget_text_$it.xml" }.toSet(), generated.toSet())
    }

    @Test fun homeWidgetsEncodeAndSkipBrokenEntries() {
        val list = listOf(HomeWidget(4, ClockPlacement(0.5f, 0.25f, 1.2f)), HomeWidget(9, ClockPlacement(0.2f, 0.8f, 0.6f)))
        assertEquals(list, HomeWidget.decodeAll(HomeWidget.encodeAll(list)))
        assertEquals(list.take(1), HomeWidget.decodeAll(HomeWidget.encodeAll(list.take(1)) + ";garbage;x:1,2"))
        assertEquals(emptyList<HomeWidget>(), HomeWidget.decodeAll(null))
    }

    @Test fun aspectsSuitTheirStyles() {
        for (style in WidgetStyle.entries) {
            val a = WidgetSpec(style).aspect
            assertTrue("$style", a in 0.8f..3f)
        }
        assertEquals(1f, WidgetSpec.default(WidgetType.ANALOGUE).aspect)
    }

    @Test fun numeralDialsUseATypefaceAndMarkDialsDoNot() {
        val numerals = setOf(WidgetStyle.DIAL_QUARTERS, WidgetStyle.DIAL_NUMBERS, WidgetStyle.DIAL_ROMAN)
        for (style in WidgetStyle.of(WidgetType.ANALOGUE)) {
            assertEquals("$style", style in numerals, WidgetSpec(style).usesTypeface)
        }
        assertTrue(WidgetStyle.of(WidgetType.ANALOGUE).size >= 7)
    }

    @Test fun collectionFieldsSurviveAwkwardCharacters() {
        val spec = WidgetSpec.default(WidgetType.LAUNCHER).copy(
            title = "Doom|sday = 100%, soon", target = 1_797_552_000_000L,
            apps = listOf("com.a/com.a.Main#0", "", "com.b/.X#10"), image = "file:///data/photos/p|1.jpg",
            iconScale = 55, spacing = 12, ringWidth = 7, rings = 3, shadow = false,
        )
        val back = WidgetSpec.decode(spec.encode(), WidgetType.LAUNCHER)
        assertEquals(spec.title, back.title)
        assertEquals(spec.target, back.target)
        assertEquals(listOf("com.a/com.a.Main#0", "com.b/.X#10"), back.apps)
        assertEquals(spec.image, back.image)
        assertEquals(spec.copy(apps = back.apps), back)
    }

    @Test fun countdownSplitsIntoDaysAndAFinalDayTimer() {
        val day = 86_400_000L
        val now = 1_000_000_000_000L
        val target = now + 22 * day + (7 * 3600 + 8 * 60 + 5) * 1000L
        val st = Collection.state(target, now)
        assertEquals(22L, st.days)
        assertEquals(now + (7 * 3600 + 8 * 60 + 5) * 1000L, st.timerEnd)
        assertEquals(false, st.done)
        assertEquals(true, Collection.state(now - 1, now).done)
    }

    @Test fun anUnsetCountdownCountsToNewYear() {
        val zone = java.time.ZoneId.of("UTC")
        val now = java.time.ZonedDateTime.of(2026, 11, 24, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        val t = Collection.target(WidgetSpec.default(WidgetType.COUNTDOWN), zone, now)
        assertEquals(java.time.ZonedDateTime.of(2027, 1, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli(), t)
    }
}
