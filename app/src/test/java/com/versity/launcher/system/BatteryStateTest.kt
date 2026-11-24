package com.versity.launcher.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryStateTest {
    @Test fun mapsLevelOverScaleToPercent() {
        assertEquals(50, batteryPercent(50, 100))
        assertEquals(100, batteryPercent(100, 100))
        assertEquals(0, batteryPercent(0, 100))
    }

    @Test fun roundsToNearestPercent() {
        assertEquals(33, batteryPercent(1, 3))
        assertEquals(67, batteryPercent(2, 3))
    }

    @Test fun handlesNonStandardScale() {
        assertEquals(50, batteryPercent(100, 200))
        assertEquals(100, batteryPercent(255, 255))
    }

    @Test fun clampsOutOfRangeLevel() {
        assertEquals(100, batteryPercent(150, 100))
    }

    @Test fun missingValuesReturnNull() {
        assertNull(batteryPercent(-1, 100))
        assertNull(batteryPercent(50, -1))
        assertNull(batteryPercent(50, 0))
    }
}
