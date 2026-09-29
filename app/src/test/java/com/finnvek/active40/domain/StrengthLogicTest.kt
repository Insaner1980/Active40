package com.finnvek.active40.domain

import com.finnvek.active40.data.db.SetEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StrengthLogicTest {
    private fun set(
        position: Int,
        kg: Double,
        reps: Int,
        done: Boolean = true,
    ) = SetEntry(sessionExerciseId = 1, position = position, weightKg = kg, reps = reps, isCompleted = done)

    @Test
    fun `initial sets copy last time`() {
        val last = listOf(set(0, 80.0, 10), set(1, 80.0, 9), set(2, 82.5, 8))
        assertEquals(
            listOf(SetValues(80.0, 10), SetValues(80.0, 9), SetValues(82.5, 8)),
            initialSetValues(targetSets = null, targetReps = null, lastTime = last),
        )
    }

    @Test
    fun `routine target overrides set count and reps`() {
        val last = listOf(set(0, 40.0, 8))
        assertEquals(
            listOf(SetValues(40.0, 12), SetValues(40.0, 12), SetValues(40.0, 12)),
            initialSetValues(targetSets = 3, targetReps = 12, lastTime = last),
        )
    }

    @Test
    fun `first time uses defaults`() {
        assertEquals(List(DEFAULT_SET_COUNT) { SetValues(0.0, 0) }, initialSetValues(null, null, emptyList()))
        assertEquals(List(2) { SetValues(0.0, 10) }, initialSetValues(2, 10, emptyList()))
    }

    @Test
    fun `next set copies previous set in this session`() {
        val current = listOf(set(0, 60.0, 10), set(1, 62.5, 8))
        assertEquals(SetValues(62.5, 8), nextSetValues(current, listOf(set(0, 50.0, 12))))
    }

    @Test
    fun `next set falls back to last time then empty`() {
        assertEquals(SetValues(50.0, 12), nextSetValues(emptyList(), listOf(set(0, 50.0, 12))))
        assertEquals(SetValues(0.0, 0), nextSetValues(emptyList(), emptyList()))
    }

    @Test
    fun `last time summaries`() {
        assertEquals(
            LastTimeSummary.Uniform(3, 10, 80.0),
            summarizeLastTime(listOf(set(0, 80.0, 10), set(1, 80.0, 10), set(2, 80.0, 10))),
        )
        assertEquals(
            LastTimeSummary.SameWeight(listOf(10, 9, 8), 82.5),
            summarizeLastTime(listOf(set(0, 82.5, 10), set(1, 82.5, 9), set(2, 82.5, 8))),
        )
        assertEquals(
            LastTimeSummary.Mixed(listOf(SetValues(80.0, 10), SetValues(85.0, 8))),
            summarizeLastTime(listOf(set(1, 85.0, 8), set(0, 80.0, 10))),
        )
    }

    @Test
    fun `last time ignores sets not done`() {
        assertEquals(LastTimeSummary.Uniform(1, 10, 80.0), summarizeLastTime(listOf(set(0, 80.0, 10), set(1, 90.0, 5, done = false))))
        assertNull(summarizeLastTime(listOf(set(0, 80.0, 10, done = false))))
        assertNull(summarizeLastTime(emptyList()))
    }

    @Test
    fun `decimal formatting trims zeros`() {
        assertEquals("80", formatDecimal(80.0))
        assertEquals("82.5", formatDecimal(82.5))
        assertEquals("181.88", formatDecimal(181.8779))
        assertEquals("0", formatDecimal(0.0))
    }

    @Test
    fun `weight formatting in pounds round trips`() {
        val kg = WeightUnit.LB.toKg(180.0)
        assertEquals("180", formatWeight(kg, WeightUnit.LB))
        assertEquals("81.65", formatWeight(kg, WeightUnit.KG))
    }

    @Test
    fun `decimal parsing accepts comma and point`() {
        assertEquals(82.5, parseDecimal("82,5")!!, 1e-9)
        assertEquals(82.5, parseDecimal(" 82.5 ")!!, 1e-9)
        assertNull(parseDecimal(""))
        assertNull(parseDecimal("abc"))
        assertNull(parseDecimal("-5"))
    }
}
