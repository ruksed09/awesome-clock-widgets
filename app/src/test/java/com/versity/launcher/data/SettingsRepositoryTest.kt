package com.versity.launcher.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val theme = ThemeBundle("t", "T", "tag", "ocean", "outfit", ClockStyle.ANALOGUE, 55, 0xFF112233L, HomeLayout.MINIMAL)
        repo.applyTheme(theme)
        val s = repo.settings.first()
        assertEquals("ocean", s.paletteId)
        assertEquals("outfit", s.fontId)
        assertEquals(ClockStyle.ANALOGUE, s.clockStyle)
        assertEquals(55, s.dimPercent)
        assertEquals(0xFF112233L, s.accentArgb)
        assertEquals(HomeLayout.MINIMAL, s.layout)
        assertEquals("content://w", s.wallpaperUri)
        assertEquals(listOf("a/A#0"), s.favourites)
        assertEquals(true, s.lockStyleEnabled)
    }

    @Test fun applyingAThemeWithoutAccentClearsTheOldAccent() = runTest {
        val repo = newRepo()
        repo.setAccentArgb(0xFF445566L)
        repo.applyTheme(ThemeBundle("t", "T", "tag", "midnight", "inter", ClockStyle.GIANT, 30, null, HomeLayout.ORBIT))
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
        repo.applyTheme(ThemeBundle("t", "T", "tag", "midnight", "inter", ClockStyle.GIANT, 30, null, HomeLayout.ORBIT))
        assertEquals(Ornament.MOON, repo.settings.first().ornament)
        repo.applyTheme(ThemeBundle("c", "C", "tag", "abyss", "cormorant", ClockStyle.ORNAMENT, 20, null, HomeLayout.MINIMAL, Ornament.CORNERS))
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
        repo.applyTheme(ThemeBundle("t", "T", "tag", "ocean", "outfit", ClockStyle.ANALOGUE, 20, null, HomeLayout.ORBIT))
        val s = repo.settings.first()
        assertEquals(DrawerStyle.PLAIN, s.drawerStyle)
        assertEquals(IconStyle.ORIGINAL, s.iconStyle)
        assertEquals("noir", s.lockThemeId)
    }

    @Test fun applyThemeCanSwapThePhotoForTheThemeBackground() = runTest {
        val repo = newRepo()
        repo.setWallpaperUri("content://w")
        val t = ThemeBundle("t", "T", "tag", "ocean", "outfit", ClockStyle.ANALOGUE, 20, null, HomeLayout.ORBIT)
        repo.applyTheme(t)
        assertEquals("content://w", repo.settings.first().wallpaperUri)
        repo.applyTheme(t, replaceWallpaper = true)
        assertNull(repo.settings.first().wallpaperUri)
    }

    @Test fun appAppearanceDefaultsToSystemAndRoundTrips() = runTest {
        val repo = newRepo()
        assertEquals(UiMode.SYSTEM, repo.settings.first().uiMode)
        for (m in UiMode.entries) {
            repo.setUiMode(m)
            assertEquals(m, repo.settings.first().uiMode)
        }
    }

    @Test fun defaultWallpaperIsLeviathanAndCanBeTurnedOff() = runTest {
        val repo = newRepo()
        assertEquals(BundledWallpaper.uri("leviathan"), repo.settings.first().wallpaperUri)
        repo.setWallpaperUri(null)
        assertNull(repo.settings.first().wallpaperUri)
        repo.reset()
        assertEquals(BundledWallpaper.uri("leviathan"), repo.settings.first().wallpaperUri)
    }

    @Test fun themeWithAWallpaperBringsItWhenReplacing() = runTest {
        val repo = newRepo()
        repo.setWallpaperUri("content://photo")
        val t = ThemeBundle("t", "T", "tag", "violet", "inter", ClockStyle.GIANT, 10, null, HomeLayout.MINIMAL, wallpaperId = "silk")
        repo.applyTheme(t)
        assertEquals("content://photo", repo.settings.first().wallpaperUri)
        repo.applyTheme(t, replaceWallpaper = true)
        assertEquals(BundledWallpaper.uri("silk"), repo.settings.first().wallpaperUri)
    }

    @Test fun lockWallpaperRoundTripsAndClears() = runTest {
        val repo = newRepo()
        assertNull(repo.settings.first().lockWallpaperUri)
        repo.setLockWallpaperUri(BundledWallpaper.uri("dunes"))
        assertEquals(BundledWallpaper.uri("dunes"), repo.settings.first().lockWallpaperUri)
        repo.setLockWallpaperUri(null)
        assertNull(repo.settings.first().lockWallpaperUri)
    }

    @Test fun clockPlacementsSaveTogetherAndClear() = runTest {
        val repo = newRepo()
        val home = ClockPlacement(0.5f, 0.42f, 1.3f)
        val lock = ClockPlacement(0.3f, 0.2f, 0.9f)
        repo.setClockPlacements(home, lock)
        repo.settings.first().let { assertEquals(home, it.clockPlacement); assertEquals(lock, it.lockClockPlacement) }
        repo.setClockPlacements(null, lock)
        repo.settings.first().let { assertNull(it.clockPlacement); assertEquals(lock, it.lockClockPlacement) }
    }

    @Test fun homeWidgetsRoundTripInOrderAndClear() = runTest {
        val repo = newRepo()
        assertEquals(emptyList<HomeWidget>(), repo.settings.first().homeWidgets)
        val widgets = listOf(HomeWidget(3, ClockPlacement(0.5f, 0.6f, 1.4f)), HomeWidget(1, ClockPlacement(0.3f, 0.4f, 0.8f)))
        repo.setHomeWidgets(widgets)
        assertEquals(widgets, repo.settings.first().homeWidgets)
        repo.setHomeWidgets(emptyList())
        assertEquals(emptyList<HomeWidget>(), repo.settings.first().homeWidgets)
    }

    @Test fun arrangementSavesClocksAndWidgetsTogether() = runTest {
        val repo = newRepo()
        val home = ClockPlacement(0.5f, 0.3f, 1.1f)
        val widgets = listOf(HomeWidget(7, ClockPlacement(0.5f, 0.7f, 1f)))
        repo.setArrangement(home, null, widgets)
        repo.settings.first().let {
            assertEquals(home, it.clockPlacement)
            assertNull(it.lockClockPlacement)
            assertEquals(widgets, it.homeWidgets)
        }
    }

    @Test fun removedStylesAndLayoutsFallBackToDefaults() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "old.preferences_pb") }
        store.edit {
            it[stringPreferencesKey("clock_style")] = "STACKED"
            it[stringPreferencesKey("layout")] = "CLASSIC"
            it[stringPreferencesKey("backdrop")] = "RAIN"
        }
        val s = SettingsRepository(store).settings.first()
        assertEquals(LauncherSettings.DEFAULT.clockStyle, s.clockStyle)
        assertEquals(LauncherSettings.DEFAULT.layout, s.layout)
    }

    @Test fun nothingIsAppliedOnAFreshInstall() = runTest {
        val s = newRepo().settings.first()
        assertNull(s.appliedThemeId)
        assertEquals(false, s.wallpaperChosen)
        assertEquals(emptyList<String>(), s.photos)
        assertNull(s.phoneHomeWallpaper)
    }

    @Test fun applyingAThemeRecordsIt() = runTest {
        val repo = newRepo()
        val theme = com.versity.launcher.domain.ThemePresets.theme("noir")!!
        repo.applyTheme(theme, replaceWallpaper = true)
        val s = repo.settings.first()
        assertEquals("noir", s.appliedThemeId)
        assertEquals(true, s.wallpaperChosen)
    }

    @Test fun earlierVersionsGetTheThemeTheirLookStillMatches() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "legacy.preferences_pb") }
        val repo = SettingsRepository(store)
        // A 0.4 user who applied Noir: the look is stored, the applied id is not.
        val noir = com.versity.launcher.domain.ThemePresets.theme("noir")!!
        repo.applyTheme(noir)
        store.edit { it.remove(stringPreferencesKey("applied_theme")) }
        assertEquals("noir", repo.settings.first().appliedThemeId)
    }

    @Test fun photosKeepOrderDropDuplicatesAndCap() = runTest {
        val repo = newRepo()
        val many = (1..12).map { "file:///p$it.jpg" }
        repo.setPhotos(listOf("file:///p1.jpg") + many)
        val s = repo.settings.first()
        assertEquals(LauncherSettings.MAX_PHOTOS, s.photos.size)
        assertEquals("file:///p1.jpg", s.photos.first())
        assertEquals(s.photos.size, s.photos.toSet().size)
    }

    @Test fun phoneWallpapersAreRecordedWithTheirIds() = runTest {
        val repo = newRepo()
        repo.recordPhoneWallpapers("file:///home.jpg" to 41, null)
        repo.recordPhoneWallpapers(null, "file:///lock.jpg" to 42)
        val s = repo.settings.first()
        assertEquals("file:///home.jpg", s.phoneHomeWallpaper)
        assertEquals(41, s.phoneHomeWallpaperId)
        assertEquals("file:///lock.jpg", s.phoneLockWallpaper)
        assertEquals(42, s.phoneLockWallpaperId)
    }

    @Test fun updateChecksAreOnByDefaultAndCanBeTurnedOff() = runTest {
        val repo = newRepo()
        assertEquals(true, repo.settings.first().autoUpdateCheck)
        repo.setAutoUpdateCheck(false)
        assertEquals(false, repo.settings.first().autoUpdateCheck)
    }
}
