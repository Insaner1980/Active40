package com.finnvek.active40.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunInputTest {
    @Test
    fun `duration from parts`() {
        assertEquals(3725L, parseDurationSeconds("1", "2", "5"))
        assertEquals(1800L, parseDurationSeconds("", "30", ""))
        assertEquals(0L, parseDurationSeconds("", "", ""))
    }

    @Test
    fun `invalid time parts`() {
        assertNull(parseDurationSeconds("0", "60", "0"))
        assertNull(parseDurationSeconds("0", "10", "75"))
        assertNull(parseDurationSeconds("x", "10", "0"))
    }

    @Test
    fun `valid run has no errors`() {
        assertTrue(validateRun("5,2", "", "31", "20").isEmpty())
    }

    @Test
    fun `zero distance and duration are blocked`() {
        assertEquals(setOf(RunInputError.DISTANCE, RunInputError.DURATION), validateRun("0", "", "", ""))
        assertEquals(setOf(RunInputError.DISTANCE), validateRun("", "0", "30", "0"))
        assertEquals(setOf(RunInputError.TIME_PARTS), validateRun("5", "0", "65", "0"))
    }
}
