package com.finnvek.active40.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockTest {
    @Test
    fun `clock format`() {
        assertEquals("0:00", formatClock(0))
        assertEquals("1:30", formatClock(90))
        assertEquals("32:14", formatClock(32 * 60 + 14))
        assertEquals("1:02:05", formatClock(3725))
        assertEquals("0:00", formatClock(-5))
    }
}
