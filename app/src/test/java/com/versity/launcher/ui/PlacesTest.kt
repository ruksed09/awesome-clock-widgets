package com.versity.launcher.ui

import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.ui.settings.Places
import com.versity.launcher.ui.settings.homeSummary
import com.versity.launcher.ui.settings.placesOf
import org.junit.Assert.assertEquals
import org.junit.Test

class PlacesTest {
    private val photo = "file:///photo.jpg"

    @Test fun theDefaultWallpaperIsNotShownAsChosen() {
        val s = LauncherSettings.DEFAULT
        assertEquals(Places(false, false), placesOf(s.wallpaperUri!!, s, isDefault = true, ids = null to null))
        assertEquals("No theme yet", homeSummary(s))
    }

    @Test fun aPhotoVersitySetOnThePhoneIsCurrentWhileItIsStillThere() {
        val s = LauncherSettings.DEFAULT.copy(phoneHomeWallpaper = photo, phoneHomeWallpaperId = 7, phoneLockWallpaper = photo, phoneLockWallpaperId = 7)
        assertEquals(Places(true, true), placesOf(photo, s, isDefault = false, ids = 7 to 7))
        // The user changed the phone's wallpaper somewhere else: no longer current.
        assertEquals(Places(false, false), placesOf(photo, s, isDefault = false, ids = 9 to 9))
    }

    @Test fun versitysOwnHomeCountsOnlyWhenVersityIsTheHomeApp() {
        val s = LauncherSettings.DEFAULT.copy(wallpaperUri = photo, wallpaperChosen = true)
        assertEquals(Places(true, false), placesOf(photo, s, isDefault = true, ids = null to null))
        assertEquals(Places(false, false), placesOf(photo, s, isDefault = false, ids = null to null))
    }
}
