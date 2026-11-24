package com.versity.launcher.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CenterCropTest {
    @Test fun wideSourceIsCroppedLeftAndRightEqually() {
        // 2000x1000 into a 1:2 portrait target keeps a 500x1000 slice from the middle.
        assertEquals(CropRect(750, 0, 500, 1000), centerCrop(2000, 1000, 540, 1080))
    }

    @Test fun tallSourceIsCroppedTopAndBottomEqually() {
        assertEquals(CropRect(0, 500, 1000, 1000), centerCrop(1000, 2000, 500, 500))
    }

    @Test fun exactAspectKeepsTheWholeImage() {
        assertEquals(CropRect(0, 0, 1080, 2400), centerCrop(1080, 2400, 540, 1200))
    }

    @Test fun cropNeverExceedsTheSource() {
        for ((sw, sh) in listOf(1920 to 1080, 1080 to 1920, 4000 to 3000, 123 to 4567)) {
            val c = centerCrop(sw, sh, 1080, 2400)
            assertTrue("$sw x $sh -> $c", c.left >= 0 && c.top >= 0 && c.left + c.width <= sw && c.top + c.height <= sh)
        }
    }
}
