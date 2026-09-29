package com.finnvek.active40.domain

import com.finnvek.active40.data.db.SetEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ProgressTest {
    // Keskiviikko 23.9.2026
    private val today = LocalDate.of(2026, 9, 23)

    @Test
    fun `range start dates`() {
        assertEquals(LocalDate.of(2026, 8, 31), ProgressRange.WEEKS_4.startDate(today, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 8, 3), ProgressRange.WEEKS_8.startDate(today, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 3, 24), ProgressRange.MONTHS_6.startDate(today, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2025, 9, 24), ProgressRange.YEAR_1.startDate(today, DayOfWeek.MONDAY))
        val weeks = weekStartsBetween(ProgressRange.WEEKS_4.startDate(today, DayOfWeek.MONDAY), today, DayOfWeek.MONDAY)
        assertEquals(4, weeks.size)
    }

    private fun row(
        session: Long,
        day: Int,
        kg: Double,
        done: Boolean = true,
    ) = SessionSet(
        session,
        LocalDate.of(2026, 9, day),
        SetEntry(sessionExerciseId = session, position = 0, weightKg = kg, reps = 5, isCompleted = done),
    )

    @Test
    fun `heaviest per session keeps order and skips sessions without done sets`() {
        val rows =
            listOf(
                row(1, 1, 70.0),
                row(1, 1, 72.5),
                row(2, 8, 90.0, done = false),
                row(3, 15, 80.0),
                row(3, 15, 82.5),
            )
        assertEquals(
            listOf(SessionValue(1, LocalDate.of(2026, 9, 1), 72.5), SessionValue(3, LocalDate.of(2026, 9, 15), 82.5)),
            heaviestPerSession(rows),
        )
    }

    @Test
    fun `trend needs two data points`() {
        assertFalse(hasTrend(listOf(80.0)))
        assertFalse(hasTrend(listOf(null, 80.0, null)))
        assertTrue(hasTrend(listOf(80.0, null, 82.5)))
    }
}
