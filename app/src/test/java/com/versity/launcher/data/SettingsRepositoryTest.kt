package com.versity.launcher.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.versity.launcher.domain.ThemeBundle
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun TestScope.newRepo() = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "settings.preferences_pb") },
    )

    @Test fun defaultsReturnedOnEmptyStore() = runTest {
        assertEquals(LauncherSettings.DEFAULT, newRepo().settings.first())
    }

    @Test fun wallpaperUriRoundTripsAndClears() = runTest {
        val repo = newRepo()
        repo.setWallpaperUri("content://media/picker/0/1")
        assertEquals("content://media/picker/0/1", repo.settings.first().wallpaperUri)
        repo.setWallpaperUri(null)
        assertNull(repo.settings.first().wallpaperUri)
    }

    @Test fun dimRoundTrips() = runTest {
        val repo = newRepo()
        repo.setDimPercent(45)
        assertEquals(45, repo.settings.first().dimPercent)
    }

    @Test fun dimIsClampedToRange() = runTest {
        val repo = newRepo()
        repo.setDimPercent(95)
        assertEquals(80, repo.settings.first().dimPercent)
        repo.setDimPercent(-5)
        assertEquals(0, repo.settings.first().dimPercent)
    }

    @Test fun clockStyleRoundTripsForEveryStyle() = runTest {
        val repo = newRepo()
        for (style in ClockStyle.entries) {
            repo.setClockStyle(style)
            assertEquals(style, repo.settings.first().clockStyle)
        }
    }

    @Test fun fontAndPaletteRoundTrip() = runTest {
        val repo = newRepo()
        repo.setFontId("outfit")
        repo.setPaletteId("ember")
        val s = repo.settings.first()
        assertEquals("outfit", s.fontId)
        assertEquals("ember", s.paletteId)
    }

    @Test fun accentRoundTripsAndClears() = runTest {
        val repo = newRepo()
        repo.setAccentArgb(0xFF3366CCL)
        assertEquals(0xFF3366CCL, repo.settings.first().accentArgb)
        repo.setAccentArgb(null)
        assertNull(repo.settings.first().accentArgb)
    }

    @Test fun changingOneFieldLeavesOthersUntouched() = runTest {
        val repo = newRepo()
        repo.setPaletteId("ocean")
        repo.setDimPercent(10)
        repo.setClockStyle(ClockStyle.ANALOGUE)
        val s = repo.settings.first()
        assertEquals("ocean", s.paletteId)
        assertEquals(10, s.dimPercent)
        assertEquals(ClockStyle.ANALOGUE, s.clockStyle)
        assertEquals(LauncherSettings.DEFAULT.fontId, s.fontId)
    }

    @Test fun resetRestoresDefaults() = runTest {
        val repo = newRepo()
        repo.setWallpaperUri("content://x")
        repo.setDimPercent(60)
        repo.setAccentArgb(0xFF112233L)
        repo.reset()
        assertEquals(LauncherSettings.DEFAULT, repo.settings.first())
    }

    @Test fun layoutRoundTripsForEveryLayout() = runTest {
        val repo = newRepo()
        assertEquals(LauncherSettings.DEFAULT.layout, repo.settings.first().layout)
        for (layout in HomeLayout.entries) {
            repo.setLayout(layout)
            assertEquals(layout, repo.settings.first().layout)
        }
    }

    @Test fun favouritesKeepOrderAndDropDuplicates() = runTest {
        val repo = newRepo()
        repo.setFavourites(listOf("b/B#0", "a/A#0", "b/B#0", "c/C#10"))
        assertEquals(listOf("b/B#0", "a/A#0", "c/C#10"), repo.settings.first().favourites)
    }

    @Test fun favouritesAreCappedAndCanBeCleared() = runTest {
        val repo = newRepo()
        repo.setFavourites((1..12).map { "p$it/A#0" })
        assertEquals(LauncherSettings.MAX_FAVOURITES, repo.settings.first().favourites.size)
        assertEquals("p1/A#0", repo.settings.first().favourites.first())
        repo.setFavourites(emptyList())
        assertEquals(emptyList<String>(), repo.settings.first().favourites)
    }

    @Test fun lockStyleToggleRoundTrips() = runTest {
        val repo = newRepo()
        assertEquals(false, repo.settings.first().lockStyleEnabled)
        repo.setLockStyleEnabled(true)
        assertEquals(true, repo.settings.first().lockStyleEnabled)
    }

    @Test fun applyThemeSetsLookButKeepsWallpaperFavouritesAndLockStyle() = runTest {
        val repo = newRepo()
        repo.setWallpaperUri("content://w")
        repo.setFavourites(listOf("a/A#0"))
        repo.setLockStyleEnabled(true)
        val theme = ThemeBundle("t", "T", "tag", "ocean", "outfit", ClockStyle.ANALOGUE, 55, 0xFF112233L, HomeLayout.MINIMAL, Backdrop.WAVES)
        repo.applyTheme(theme)
        val s = repo.settings.first()
        assertEquals("ocean", s.paletteId)
        assertEquals("outfit", s.fontId)
        assertEquals(ClockStyle.ANALOGUE, s.clockStyle)
        assertEquals(55, s.dimPercent)
        assertEquals(0xFF112233L, s.accentArgb)
        assertEquals(HomeLayout.MINIMAL, s.layout)
        assertEquals(Backdrop.WAVES, s.backdrop)
        assertEquals("content://w", s.wallpaperUri)
        assertEquals(listOf("a/A#0"), s.favourites)
        assertEquals(true, s.lockStyleEnabled)
    }

    @Test fun applyingAThemeWithoutAccentClearsTheOldAccent() = runTest {
        val repo = newRepo()
        repo.setAccentArgb(0xFF445566L)
        repo.applyTheme(ThemeBundle("t", "T", "tag", "midnight", "inter", ClockStyle.STACKED, 30, null, HomeLayout.CLASSIC, Backdrop.NONE))
        assertNull(repo.settings.first().accentArgb)
    }

    @Test fun ornamentAndDateRoundTrip() = runTest {
        val repo = newRepo()
        assertEquals(Ornament.CELESTIAL, repo.settings.first().ornament)
        assertEquals(false, repo.settings.first().ornamentDate)
        for (o in Ornament.entries) {
            repo.setOrnament(o)
            assertEquals(o, repo.settings.first().ornament)
        }
        repo.setOrnamentDate(true)
        assertEquals(true, repo.settings.first().ornamentDate)
    }

    @Test fun themeWithOrnamentSetsItAndThemeWithoutKeepsIt() = runTest {
        val repo = newRepo()
        repo.setOrnament(Ornament.MOON)
        repo.applyTheme(ThemeBundle("t", "T", "tag", "midnight", "inter", ClockStyle.STACKED, 30, null, HomeLayout.CLASSIC, Backdrop.NONE))
        assertEquals(Ornament.MOON, repo.settings.first().ornament)
        repo.applyTheme(ThemeBundle("c", "C", "tag", "abyss", "cormorant", ClockStyle.ORNAMENT, 20, null, HomeLayout.MINIMAL, Backdrop.STARS, Ornament.CORNERS))
        assertEquals(Ornament.CORNERS, repo.settings.first().ornament)
    }

    @Test fun lockThemeRoundTripsAndClears() = runTest {
        val repo = newRepo()
        assertNull(repo.settings.first().lockThemeId)
        repo.setLockThemeId("noir")
        assertEquals("noir", repo.settings.first().lockThemeId)
        repo.setLockThemeId(null)
        assertNull(repo.settings.first().lockThemeId)
    }

    @Test fun drawerAndIconStylesRoundTrip() = runTest {
        val repo = newRepo()
        repo.setDrawerStyle(DrawerStyle.THEMED)
        repo.setIconStyle(IconStyle.THEMED)
        val s = repo.settings.first()
        assertEquals(DrawerStyle.THEMED, s.drawerStyle)
        assertEquals(IconStyle.THEMED, s.iconStyle)
    }

    @Test fun applyingAThemeLeavesDrawerIconsAndLockThemeAlone() = runTest {
        val repo = newRepo()
        repo.setDrawerStyle(DrawerStyle.PLAIN)
        repo.setIconStyle(IconStyle.ORIGINAL)
        repo.setLockThemeId("noir")
        repo.applyTheme(ThemeBundle("t", "T", "tag", "ocean", "outfit", ClockStyle.ANALOGUE, 20, null, HomeLayout.CLASSIC, Backdrop.WAVES))
        val s = repo.settings.first()
        assertEquals(DrawerStyle.PLAIN, s.drawerStyle)
        assertEquals(IconStyle.ORIGINAL, s.iconStyle)
        assertEquals("noir", s.lockThemeId)
    }

    @Test fun applyThemeCanSwapThePhotoForTheThemeBackground() = runTest {
        val repo = newRepo()
        repo.setWallpaperUri("content://w")
        val t = ThemeBundle("t", "T", "tag", "ocean", "outfit", ClockStyle.ANALOGUE, 20, null, HomeLayout.CLASSIC, Backdrop.WAVES)
        repo.applyTheme(t)
        assertEquals("content://w", repo.settings.first().wallpaperUri)
        repo.applyTheme(t, clearWallpaper = true)
        assertNull(repo.settings.first().wallpaperUri)
    }

    @Test fun backdropRoundTripsForEveryBackdrop() = runTest {
        val repo = newRepo()
        assertEquals(LauncherSettings.DEFAULT.backdrop, repo.settings.first().backdrop)
        for (b in Backdrop.entries) {
            repo.setBackdrop(b)
            assertEquals(b, repo.settings.first().backdrop)
        }
    }
}
