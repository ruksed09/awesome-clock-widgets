package com.versity.launcher

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.NodeList

/** Parses the source manifest on the JVM; the unit-test working dir is the :app module. */
class ManifestTest {
    private val ns = "http://schemas.android.com/apk/res/android"
    private val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))

    private fun NodeList.elements() = (0 until length).map { item(it) as Element }
    private fun Element.name(): String = getAttributeNS(ns, "name")

    private val homeActivity: Element? = doc.getElementsByTagName("activity").elements().firstOrNull { activity ->
        activity.getElementsByTagName("category").elements().any { it.name() == "android.intent.category.HOME" }
    }

    @Test fun homeActivityDeclaresHomeAndDefaultCategories() {
        assertNotNull("no activity declares CATEGORY_HOME", homeActivity)
        val filter = homeActivity!!.getElementsByTagName("intent-filter").elements().first { f ->
            f.getElementsByTagName("category").elements().any { it.name() == "android.intent.category.HOME" }
        }
        val categories = filter.getElementsByTagName("category").elements().map { it.name() }
        assertTrue(categories.containsAll(listOf("android.intent.category.HOME", "android.intent.category.DEFAULT")))
        assertTrue(filter.getElementsByTagName("action").elements().any { it.name() == "android.intent.action.MAIN" })
    }

    @Test fun homeActivityIsSingleTaskAndStateNotNeeded() {
        assertNotNull("no HOME activity", homeActivity)
        assertEquals("singleTask", homeActivity!!.getAttributeNS(ns, "launchMode"))
        assertEquals("true", homeActivity.getAttributeNS(ns, "stateNotNeeded"))
    }

    @Test fun queriesDeclareMainLauncherIntent() {
        val queries = doc.getElementsByTagName("queries").elements().firstOrNull()
        assertNotNull("no <queries> element", queries)
        val intent = queries!!.getElementsByTagName("intent").elements().firstOrNull()
        assertNotNull("no <intent> inside <queries>", intent)
        assertTrue(intent!!.getElementsByTagName("action").elements().any { it.name() == "android.intent.action.MAIN" })
        assertTrue(intent.getElementsByTagName("category").elements().any { it.name() == "android.intent.category.LAUNCHER" })
    }

    @Test fun noBroadPermissionsRequested() {
        val perms = doc.getElementsByTagName("uses-permission").elements().map { it.name() }
        assertTrue("QUERY_ALL_PACKAGES must not be used", "android.permission.QUERY_ALL_PACKAGES" !in perms)
        assertTrue("Photo Picker needs no media permission", "android.permission.READ_MEDIA_IMAGES" !in perms)
    }

    @Test fun onlyWallpaperAndUpdatePermissionsAreRequested() {
        val perms = doc.getElementsByTagName("uses-permission").elements().map { it.name() }.toSet()
        assertEquals(
            setOf("android.permission.SET_WALLPAPER", "android.permission.INTERNET", "android.permission.REQUEST_INSTALL_PACKAGES"),
            perms,
        )
    }
}
