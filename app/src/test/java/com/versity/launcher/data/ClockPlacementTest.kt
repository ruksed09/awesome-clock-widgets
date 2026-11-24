package com.versity.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClockPlacementTest {
    @Test fun encodesAndDecodesExactly() {
        val p = ClockPlacement(0.5f, 0.3333f, 1.25f)
        assertEquals(p, ClockPlacement.decode(p.encode()))
    }

    @Test fun decodingClampsToTheScreenAndSizeLimits() {
        assertEquals(ClockPlacement(0.05f, 0.95f, ClockPlacement.MAX_SCALE), ClockPlacement.decode("-1,4,9"))
        assertEquals(ClockPlacement.MIN_SCALE, ClockPlacement.decode("0.5,0.5,0.01")!!.scale)
    }

    @Test fun badValuesDecodeToNull() {
        assertNull(ClockPlacement.decode(null))
        assertNull(ClockPlacement.decode(""))
        assertNull(ClockPlacement.decode("0.5,0.5"))
        assertNull(ClockPlacement.decode("a,b,c"))
        assertNull(ClockPlacement.decode("NaN,0.5,1"))
    }
}
