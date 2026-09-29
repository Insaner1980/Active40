package com.finnvek.active40.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaceTest {
    @Test
    fun `pace per km`() {
        // 5 km in 30:00 = 6:00 /km
        assertEquals(360.0, paceSecondsPerUnit(5000.0, 1800, DistanceUnit.KM)!!, 1e-9)
        assertEquals("6:00", formatPace(360.0))
    }

    @Test
    fun `pace per mile`() {
        // 5 km in 30:00 = 9:39 /mi (579.36384 s)
        val pace = paceSecondsPerUnit(5000.0, 1800, DistanceUnit.MI)!!
        assertEquals(579.36384, pace, 1e-6)
        assertEquals("9:39", formatPace(pace))
    }

    @Test
    fun `formatting rounds to whole seconds without 60`() {
        assertEquals("6:00", formatPace(359.6))
        assertEquals("5:59", formatPace(359.4))
        assertEquals("10:05", formatPace(605.0))
    }

    @Test
    fun `zero distance or duration gives no pace`() {
        assertNull(paceSecondsPerUnit(0.0, 1800, DistanceUnit.KM))
        assertNull(paceSecondsPerUnit(5000.0, 0, DistanceUnit.KM))
    }
}
