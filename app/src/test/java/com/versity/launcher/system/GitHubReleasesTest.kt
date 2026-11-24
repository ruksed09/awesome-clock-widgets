package com.versity.launcher.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs under Robolectric for Android's org.json. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GitHubReleasesTest {
    @Test fun newerVersionsCompareByNumberNotText() {
        assertTrue(GitHubReleases.isNewer("0.5.0", "0.4.0"))
        assertTrue(GitHubReleases.isNewer("v0.10.0", "0.9.9"))
        assertTrue(GitHubReleases.isNewer("1.0", "0.9.9"))
        assertFalse(GitHubReleases.isNewer("0.4.0", "0.4.0"))
        assertFalse(GitHubReleases.isNewer("0.3.9", "0.4.0"))
        assertFalse(GitHubReleases.isNewer("0.4.0", "0.4.0.1"))
    }

    @Test fun parsesTheLatestReleaseAndItsApk() {
        val json = """
            {"tag_name":"v0.5.0","body":"## Versity 0.5.0\n- Tablets","html_url":"https://github.com/x/y/releases/tag/v0.5.0",
             "assets":[{"name":"notes.txt","browser_download_url":"https://e/notes.txt","size":3},
                       {"name":"versity-0.5.0.apk","browser_download_url":"https://e/versity-0.5.0.apk","size":15000000,
                        "digest":"sha256:abc123"}]}
        """.trimIndent()
        val r = GitHubReleases.parse(json)!!
        assertEquals("0.5.0", r.version)
        assertEquals("https://e/versity-0.5.0.apk", r.apkUrl)
        assertEquals(15000000L, r.apkSize)
        assertEquals("abc123", r.sha256)
    }

    @Test fun aReleaseWithoutAnApkIsIgnored() {
        assertNull(GitHubReleases.parse("""{"tag_name":"v9.9.9","assets":[]}"""))
    }
}
