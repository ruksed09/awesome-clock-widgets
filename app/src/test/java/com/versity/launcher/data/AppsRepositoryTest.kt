package com.versity.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppsRepositoryTest {
    private fun repo(vararg apps: RawApp) =
        AppsRepository(InstalledAppSource { apps.toList() }, Dispatchers.Unconfined)

    @Test fun returnsOnlyLaunchableApps() = runTest {
        val result = repo(
            RawApp("Camera", "p.camera", "p.camera.Main"),
            RawApp("Service only", "p.service", null),
            RawApp("", "", "x.Main"),
        ).loadApps()
        assertEquals(listOf("Camera"), result.map { it.label })
    }

    @Test fun sortedByLabelIgnoringCase() = runTest {
        val result = repo(
            RawApp("cherry", "p.c", "p.c.A"),
            RawApp("Banana", "p.b", "p.b.A"),
            RawApp("apple", "p.a", "p.a.A"),
        ).loadApps()
        assertEquals(listOf("apple", "Banana", "cherry"), result.map { it.label })
    }

    @Test fun duplicateActivitiesAreCollapsed() = runTest {
        val result = repo(
            RawApp("Maps", "p.maps", "p.maps.A"),
            RawApp("Maps", "p.maps", "p.maps.A"),
        ).loadApps()
        assertEquals(1, result.size)
    }

    private val sample = listOf(
        AppEntry("Camera", "p.cam", "p.cam.A"),
        AppEntry("Calculator", "p.calc", "p.calc.A"),
        AppEntry("Music", "p.mus", "p.mus.A"),
    )

    @Test fun searchIsCaseInsensitive() {
        assertEquals(listOf("Camera"), filterApps(sample, "cAm").map { it.label })
        assertEquals(listOf("Camera", "Calculator"), filterApps(sample, "CA").map { it.label })
    }

    @Test fun searchMatchesAnywhereInLabelAndTrimsWhitespace() {
        assertEquals(listOf("Music"), filterApps(sample, "  usi ").map { it.label })
    }

    @Test fun blankQueryReturnsEverythingInOrder() {
        assertEquals(sample, filterApps(sample, ""))
        assertEquals(sample, filterApps(sample, "   "))
    }

    @Test fun noMatchReturnsEmpty() {
        assertEquals(emptyList<AppEntry>(), filterApps(sample, "zzz"))
    }

    @Test fun sameAppInTwoProfilesKeepsBothEntries() = runTest {
        val result = repo(
            RawApp("WhatsApp", "p.wa", "p.wa.Main", userSerial = 0),
            RawApp("WhatsApp", "p.wa", "p.wa.Main", userSerial = 999),
        ).loadApps()
        assertEquals(listOf(0L, 999L), result.map { it.userSerial }.sorted())
    }

    @Test fun keyIncludesPackageActivityAndProfile() {
        assertEquals("p.a/p.a.Main#999", AppEntry("A", "p.a", "p.a.Main", 999).key)
    }

    @Test fun favouritesResolveInKeyOrderAndSkipMissingApps() {
        val resolved = resolveFavourites(listOf(sample[2].key, "gone/X#0", sample[0].key), sample)
        assertEquals(listOf("Music", "Camera"), resolved.map { it.label })
    }

    @Test fun noFavouritesResolvesToEmpty() {
        assertEquals(emptyList<AppEntry>(), resolveFavourites(emptyList(), sample))
    }
}
