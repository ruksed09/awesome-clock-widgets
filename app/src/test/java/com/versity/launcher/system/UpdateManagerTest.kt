package com.versity.launcher.system

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A release found once keeps showing on later launches until this install catches up with it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UpdateManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun remember(tag: String) = context.getSharedPreferences("updates", Context.MODE_PRIVATE).edit()
        .putString(
            "latest_json",
            """{"tag_name":"$tag","body":"Notes","html_url":"https://example.com/r",
               "assets":[{"name":"versity.apk","browser_download_url":"https://example.com/v.apk","size":10}]}""",
        ).commit()

    @Test fun aRememberedNewerReleaseShowsAtOnce() {
        remember("v99.0.0")
        val state = UpdateManager(context, CoroutineScope(Dispatchers.Unconfined)).state.value
        assertTrue("$state", state is UpdateState.Available)
        assertEquals("99.0.0", (state as UpdateState.Available).release.version)
    }

    @Test fun aRememberedReleaseThisInstallAlreadyHasIsNotOffered() {
        remember("v0.0.1")
        assertEquals(UpdateState.Idle, UpdateManager(context, CoroutineScope(Dispatchers.Unconfined)).state.value)
    }
}
