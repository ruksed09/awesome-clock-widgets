package com.versity.launcher.domain

import com.versity.launcher.data.Backdrop
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.LauncherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePresetsTest {
    private fun channel(argb: Long, shift: Int): Double {
        val c = ((argb shr shift) and 0xFF) / 255.0
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }

    private fun luminance(argb: Long) =
        0.2126 * channel(argb, 16) + 0.7152 * channel(argb, 8) + 0.0722 * channel(argb, 0)

    private fun contrast(a: Long, b: Long): Double {
        val (hi, lo) = luminance(a).let { la -> luminance(b).let { lb -> maxOf(la, lb) to minOf(la, lb) } }
        return (hi + 0.05) / (lo + 0.05)
    }

    @Test fun presetsAreNotEmpty() {
        assertTrue(ThemePresets.palettes.size >= 3)
        assertTrue(ThemePresets.fonts.size >= 3)
    }

    @Test fun planNamedPalettesExist() {
        val ids = ThemePresets.palettes.map { it.id }
        assertTrue(ids.containsAll(listOf("midnight", "ocean", "ember")))
    }

    @Test fun everyPresetIdIsUnique() {
        val p = ThemePresets.palettes.map { it.id }
        val f = ThemePresets.fonts.map { it.id }
        assertTrue(p.isNotEmpty() && f.isNotEmpty())
        assertEquals(p.size, p.toSet().size)
        assertEquals(f.size, f.toSet().size)
    }

    @Test fun everyFontIdResolvesToAResource() {
        assertTrue(ThemePresets.fonts.isNotEmpty())
        for (font in ThemePresets.fonts) {
            assertEquals(font, ThemePresets.font(font.id))
            assertTrue("${font.id} has no font resource", font.resId != 0)
        }
    }

    @Test fun defaultSettingsReferenceExistingPresets() {
        assertTrue(ThemePresets.palettes.any { it.id == LauncherSettings.DEFAULT.paletteId })
        assertTrue(ThemePresets.fonts.any { it.id == LauncherSettings.DEFAULT.fontId })
    }

    @Test fun everyPaletteHasAllColourRolesOpaque() {
        assertTrue(ThemePresets.palettes.isNotEmpty())
        for (p in ThemePresets.palettes) {
            assertTrue("${p.id} has blank name", p.name.isNotBlank())
            listOf(p.backgroundTop, p.backgroundBottom, p.surface, p.onSurface, p.onSurfaceMuted, p.accent, p.rimUpper, p.rimLower)
                .forEach { assertEquals("${p.id} colour not opaque", 0xFFL, (it shr 24) and 0xFF) }
        }
    }

    @Test fun paletteTextIsReadableOnItsBackground() {
        assertTrue(ThemePresets.palettes.isNotEmpty())
        for (p in ThemePresets.palettes) {
            for (bg in listOf(p.backgroundTop, p.backgroundBottom)) {
                assertTrue("${p.id} onSurface contrast", contrast(p.onSurface, bg) >= 4.5)
                assertTrue("${p.id} muted contrast", contrast(p.onSurfaceMuted, bg) >= 3.0)
                assertTrue("${p.id} accent contrast", contrast(p.accent, bg) >= 3.0)
            }
        }
    }

    @Test fun everyClockStyleIsOffered() {
        assertEquals(
            setOf(ClockStyle.STACKED, ClockStyle.SINGLE_LINE, ClockStyle.ANALOGUE, ClockStyle.ORNAMENT),
            ThemePresets.clockStyles.toSet(),
        )
    }

    @Test fun celestialThemeUsesOrnamentSerifAndStars() {
        val t = ThemePresets.themes.first { it.id == "celestial" }
        assertEquals(ClockStyle.ORNAMENT, t.clockStyle)
        assertEquals("cormorant", t.fontId)
        assertEquals(Backdrop.STARS, t.backdrop)
        assertEquals(com.versity.launcher.data.Ornament.CELESTIAL, t.ornament)
    }

    @Test fun atLeastFourBundledThemes() {
        assertTrue(ThemePresets.themes.size >= 4)
    }

    @Test fun themeIdsAreUniqueAndLooksAreDistinct() {
        val t = ThemePresets.themes
        assertTrue(t.isNotEmpty())
        assertEquals(t.size, t.map { it.id }.toSet().size)
        assertEquals(t.size, t.map { listOf(it.paletteId, it.fontId, it.clockStyle, it.layout) }.toSet().size)
    }

    @Test fun everyThemeReferencesExistingPartsAndValidDim() {
        assertTrue(ThemePresets.themes.isNotEmpty())
        for (t in ThemePresets.themes) {
            assertTrue("${t.id} palette", ThemePresets.palettes.any { it.id == t.paletteId })
            assertTrue("${t.id} font", ThemePresets.fonts.any { it.id == t.fontId })
            assertTrue("${t.id} dim", t.dimPercent in LauncherSettings.MIN_DIM..LauncherSettings.MAX_DIM)
            assertTrue("${t.id} name", t.name.isNotBlank() && t.tagline.isNotBlank())
        }
    }

    @Test fun themesUseMoreThanOneLayoutAndClockStyle() {
        assertTrue(ThemePresets.themes.map { it.layout }.toSet().size >= 2)
        assertTrue(ThemePresets.themes.map { it.clockStyle }.toSet().size >= 3)
    }

    @Test fun themesUseAtLeastThreeDistinctBackdrops() {
        assertTrue(ThemePresets.themes.map { it.backdrop }.toSet().size >= 3)
    }

    @Test fun everyBackdropIsOfferedByAtLeastOneTheme() {
        assertEquals(Backdrop.entries.toSet(), ThemePresets.themes.map { it.backdrop }.toSet())
    }

    @Test fun defaultSettingsEqualTheOrbitTheme() {
        val orbit = ThemePresets.themes.first { it.id == "orbit" }
        val d = LauncherSettings.DEFAULT
        assertEquals(orbit.paletteId, d.paletteId)
        assertEquals(orbit.fontId, d.fontId)
        assertEquals(orbit.clockStyle, d.clockStyle)
        assertEquals(orbit.dimPercent, d.dimPercent)
        assertEquals(orbit.accentArgb, d.accentArgb)
        assertEquals(orbit.layout, d.layout)
        assertEquals(orbit.backdrop, d.backdrop)
    }

    @Test fun lockStyleIsOffByDefault() {
        assertEquals(false, LauncherSettings.DEFAULT.lockStyleEnabled)
    }
}
