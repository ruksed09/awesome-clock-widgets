package com.versity.launcher.domain

import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.HomeLayout
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
            listOf(p.backgroundTop, p.backgroundBottom, p.surface, p.onSurface, p.onSurfaceMuted, p.accent)
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
            ClockStyle.entries.toSet(),
            ThemePresets.clockStyles.toSet(),
        )
    }

    @Test fun leviathanThemeUsesOrnamentSerifAndStar() {
        val t = ThemePresets.themes.first { it.id == "leviathan" }
        assertEquals(ClockStyle.ORNAMENT, t.clockStyle)
        assertEquals("cormorant", t.fontId)
        assertEquals("leviathan", t.wallpaperId)
        assertEquals(com.versity.launcher.data.Ornament.CELESTIAL, t.ornament)
    }

    @Test fun atLeastFortyBundledThemes() {
        assertTrue(ThemePresets.themes.size >= 40)
    }

    @Test fun everyCategoryHasAtLeastThreeThemes() {
        for (c in ThemeCategory.entries) {
            assertTrue("$c", ThemePresets.themes.count { it.category == c } >= 3)
        }
    }

    @Test fun everyThemeHasItsOwnName() {
        val names = ThemePresets.themes.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test fun ornamentThemesNameTheirOrnament() {
        ThemePresets.themes.filter { it.clockStyle == ClockStyle.ORNAMENT }.forEach {
            assertTrue("${it.id} has no ornament", it.ornament != null)
        }
    }

    @Test fun withThemeChangesTheLookButKeepsPersonalChoices() {
        val mine = LauncherSettings.DEFAULT.copy(
            wallpaperUri = "content://photo", favourites = listOf("a/A#0"), lockStyleEnabled = true,
            drawerStyle = com.versity.launcher.data.DrawerStyle.PLAIN, iconStyle = com.versity.launcher.data.IconStyle.ORIGINAL,
        )
        val t = ThemePresets.theme("moonrise")!!
        val s = mine.withTheme(t)
        assertTrue(t.matches(s))
        assertEquals("content://photo", s.wallpaperUri)
        assertEquals(listOf("a/A#0"), s.favourites)
        assertEquals(true, s.lockStyleEnabled)
        assertEquals(com.versity.launcher.data.DrawerStyle.PLAIN, s.drawerStyle)
        assertEquals(com.versity.launcher.data.IconStyle.ORIGINAL, s.iconStyle)
    }

    @Test fun lockLookFollowsHomeUntilALockThemeIsChosen() {
        val home = LauncherSettings.DEFAULT.withTheme(ThemePresets.theme("shoreline")!!).copy(wallpaperUri = "content://photo")
        assertEquals(home, home.lockLook())
        val lock = home.copy(lockThemeId = "noir").lockLook()
        assertTrue(ThemePresets.theme("noir")!!.matches(lock))
        // A lock theme shows its own background, not the home photo.
        assertEquals(null, lock.wallpaperUri)
    }

    @Test fun staleLockThemeIdFallsBackToHome() {
        val home = LauncherSettings.DEFAULT.copy(lockThemeId = "removed_theme")
        assertEquals(home, home.lockLook())
    }

    @Test fun drawerAndIconsAreLeftAloneByDefault() {
        assertEquals(com.versity.launcher.data.DrawerStyle.PLAIN, LauncherSettings.DEFAULT.drawerStyle)
        assertEquals(com.versity.launcher.data.IconStyle.ORIGINAL, LauncherSettings.DEFAULT.iconStyle)
    }

    @Test fun themeIdsAreUniqueAndLooksAreDistinct() {
        val t = ThemePresets.themes
        assertTrue(t.isNotEmpty())
        assertEquals(t.size, t.map { it.id }.toSet().size)
        assertEquals(t.size, t.map { listOf(it.paletteId, it.fontId, it.clockStyle, it.layout, it.wallpaperId) }.toSet().size)
        // Themes are recognised without their wallpaper (a photo can replace it), so the rest must differ too.
        assertEquals(t.size, t.map { listOf(it.paletteId, it.fontId, it.clockStyle, it.dimPercent, it.layout, it.ornament) }.toSet().size)
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

    @Test fun almostEveryThemeBringsAWallpaper() {
        assertTrue(ThemePresets.themes.count { it.wallpaperId == null } <= 6)
    }

    @Test fun everyClockStyleIsUsedByATheme() {
        assertEquals(ClockStyle.entries.toSet(), ThemePresets.themes.map { it.clockStyle }.toSet())
    }

    @Test fun defaultSettingsEqualTheLeviathanTheme() {
        val t = ThemePresets.themes.first { it.id == "leviathan" }
        val d = LauncherSettings.DEFAULT
        assertTrue(t.matches(d))
        assertEquals(t.wallpaperUri, d.wallpaperUri)
        assertEquals(com.versity.launcher.data.BundledWallpaper.uri("leviathan"), d.wallpaperUri)
    }

    @Test fun everyThemeWallpaperIsBundled() {
        ThemePresets.themes.mapNotNull { it.wallpaperId }.forEach { id ->
            assertTrue("$id missing", Wallpapers.byId(id) != null)
        }
    }

    @Test fun wallpapersHaveUniqueIdsAndResources() {
        val w = Wallpapers.all
        assertTrue(w.size >= 10)
        assertEquals(w.size, w.map { it.id }.toSet().size)
        w.forEach { assertTrue(it.full != 0 && it.thumb != 0 && it.full != it.thumb) }
        w.forEach { assertEquals(it, Wallpapers.byUri(it.uri)) }
    }

    @Test fun lightWallpaperThemesUseLightPalettes() {
        ThemePresets.themes.filter { t -> Wallpapers.byId(t.wallpaperId)?.light == true }.forEach { t ->
            assertTrue("${t.id} needs dark ink on its light wallpaper", ThemePresets.palette(t.paletteId).isLight())
        }
    }

    @Test fun lockOnlyWallpaperWinsOnTheLockScreen() {
        val s = LauncherSettings.DEFAULT.copy(lockThemeId = "noir", lockWallpaperUri = Wallpapers.byId("silk")!!.uri)
        assertEquals(Wallpapers.byId("silk")!!.uri, s.lockLook().wallpaperUri)
        assertTrue(ThemePresets.theme("noir")!!.matches(s.lockLook()))
    }

    @Test fun lockThemeBringsItsOwnWallpaper() {
        val s = LauncherSettings.DEFAULT.copy(wallpaperUri = null, lockThemeId = "moonrise")
        assertEquals(Wallpapers.byId("moon_tide")!!.uri, s.lockLook().wallpaperUri)
    }

    @Test fun everyBigClockStyleHasATheme() {
        val big = com.versity.launcher.ui.home.components.BigClockStyles
        assertTrue(ThemePresets.themes.map { it.clockStyle }.toSet().containsAll(big))
    }

    @Test fun lockPlacementIsSeparateFromHome() {
        val home = com.versity.launcher.data.ClockPlacement(0.5f, 0.6f, 1.2f)
        val s = LauncherSettings.DEFAULT.copy(clockPlacement = home)
        assertEquals(null, s.lockLook().clockPlacement)
        val lock = com.versity.launcher.data.ClockPlacement(0.5f, 0.3f, 0.8f)
        assertEquals(lock, s.copy(lockClockPlacement = lock).lockLook().clockPlacement)
    }

    @Test fun wideScreensGetTheLandscapeRendering() {
        val crystal = Wallpapers.byId("crystal_sea")!!
        assertEquals(com.versity.launcher.data.BundledWallpaper.uri("crystal_sea_land"), Wallpapers.sourceFor(crystal.uri, 2560, 1600))
        assertEquals(crystal.uri, Wallpapers.sourceFor(crystal.uri, 1080, 2340))
        assertEquals(com.versity.launcher.data.BundledWallpaper.uri("crystal_sea_thumb"), Wallpapers.sourceFor(crystal.uri, 300, 650))
        // A tablet held upright stays on the portrait image.
        assertEquals(crystal.uri, Wallpapers.sourceFor(crystal.uri, 1600, 2560))
    }

    @Test fun photosPassThroughUnchanged() {
        assertEquals("content://photo/1", Wallpapers.sourceFor("content://photo/1", 2560, 1600))
        assertEquals(null, Wallpapers.sourceFor(null, 2560, 1600))
    }

    @Test fun everyGeneratedWallpaperHasALandscapeRendering() {
        Wallpapers.all.filter { it.id != "leviathan" }.forEach { assertTrue("${it.id} has no landscape version", it.land != null) }
    }

    @Test fun wallpaperStylesAreVaried() {
        assertTrue(Wallpapers.all.size >= 26)
        assertTrue(Wallpapers.collections.size >= 7)
    }

    @Test fun legacyPiecesAreGone() {
        assertTrue(ClockStyle.entries.none { it.name == "STACKED" || it.name == "SINGLE_LINE" })
        assertTrue(HomeLayout.entries.none { it.name == "CLASSIC" })
        assertTrue(ThemePresets.themes.none { it.id in setOf("orbit", "synthwave", "terminal", "cyber", "monsoon") })
    }

    @Test fun lockStyleIsOffByDefault() {
        assertEquals(false, LauncherSettings.DEFAULT.lockStyleEnabled)
    }
}
