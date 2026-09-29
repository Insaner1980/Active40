package com.finnvek.active40.domain

import com.finnvek.active40.data.db.SetEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class TrainingStatsTest {
    private val monday = DayOfWeek.MONDAY
    private val sunday = DayOfWeek.SUNDAY

    // Tiistai 22.9.2026
    private val tue = LocalDate.of(2026, 9, 22)

    private fun strength(
        date: LocalDate,
        minutes: Long,
    ) = TrainingActivity(date, ActivityKind.STRENGTH, minutes * 60)

    private fun run(
        date: LocalDate,
        km: Double,
        minutes: Long,
    ) = TrainingActivity(date, ActivityKind.RUN, minutes * 60, km * 1000)

    @Test
    fun `week start follows first day of week`() {
        assertEquals(LocalDate.of(2026, 9, 21), weekStart(tue, monday))
        assertEquals(LocalDate.of(2026, 9, 20), weekStart(tue, sunday))
        assertEquals(tue, weekStart(tue, DayOfWeek.TUESDAY))
    }

    @Test
    fun `week days are seven consecutive days`() {
        val days = weekDays(tue, monday)
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 9, 21), days.first())
        assertEquals(LocalDate.of(2026, 9, 27), days.last())
    }

    @Test
    fun `totals count sessions runs distance and time`() {
        val t =
            totals(
                listOf(
                    strength(tue, 45),
                    strength(tue.plusDays(2), 50),
                    run(tue, 5.0, 30),
                    run(tue.plusDays(3), 8.4, 52),
                ),
            )
        assertEquals(2, t.strengthSessions)
        assertEquals(2, t.runs)
        assertEquals(13400.0, t.runDistanceMeters, 1e-9)
        assertEquals((45 + 50 + 30 + 52) * 60L, t.trainingSeconds)
    }

    @Test
    fun `totals of nothing are zero`() {
        assertEquals(WeekTotals(0, 0, 0.0, 0), totals(emptyList()))
    }

    @Test
    fun `weekly run distance includes empty weeks`() {
        val from = LocalDate.of(2026, 9, 1)
        val to = LocalDate.of(2026, 9, 22)
        val result =
            weeklyRunDistance(
                listOf(
                    run(LocalDate.of(2026, 9, 1), 5.0, 30),
                    run(LocalDate.of(2026, 9, 3), 3.0, 20),
                    strength(LocalDate.of(2026, 9, 8), 40),
                    run(LocalDate.of(2026, 9, 22), 10.0, 60),
                ),
                from,
                to,
                monday,
            )
        assertEquals(
            listOf(
                WeekValue(LocalDate.of(2026, 8, 31), 8000.0),
                WeekValue(LocalDate.of(2026, 9, 7), 0.0),
                WeekValue(LocalDate.of(2026, 9, 14), 0.0),
                WeekValue(LocalDate.of(2026, 9, 21), 10000.0),
            ),
            result,
        )
    }

    @Test
    fun `weekly average pace is total time over total distance`() {
        val result =
            weeklyAveragePace(
                listOf(run(LocalDate.of(2026, 9, 14), 5.0, 30), run(LocalDate.of(2026, 9, 16), 10.0, 55)),
                LocalDate.of(2026, 9, 14),
                LocalDate.of(2026, 9, 22),
                monday,
                DistanceUnit.KM,
            )
        // (30 + 55) min / 15 km = 340 s/km
        assertEquals(340.0, result[0].value!!, 1e-9)
        assertNull(result[1].value)
    }

    @Test
    fun `heaviest set counts only completed sets`() {
        val sets =
            listOf(
                SetEntry(sessionExerciseId = 1, position = 0, weightKg = 80.0, reps = 10, isCompleted = true),
                SetEntry(sessionExerciseId = 1, position = 1, weightKg = 82.5, reps = 8, isCompleted = true),
                SetEntry(sessionExerciseId = 1, position = 2, weightKg = 90.0, reps = 5, isCompleted = false),
            )
        assertEquals(82.5, heaviestCompletedSetKg(sets)!!, 1e-9)
        assertNull(heaviestCompletedSetKg(sets.map { it.copy(isCompleted = false) }))
        assertNull(heaviestCompletedSetKg(emptyList()))
    }

    @Test
    fun `percentage change`() {
        assertEquals(13.793103, percentageChange(72.5, 82.5)!!, 1e-6)
        assertEquals(-10.0, percentageChange(100.0, 90.0)!!, 1e-9)
        assertEquals(0.0, percentageChange(50.0, 50.0)!!, 1e-9)
        assertNull(percentageChange(0.0, 20.0))
    }

    @Test
    fun `balance by training time`() {
        val share = strengthShareByTime(listOf(strength(tue, 46), run(tue, 5.0, 54)))
        assertEquals(0.46, share!!, 1e-9)
        assertEquals(1.0, strengthShareByTime(listOf(strength(tue, 30)))!!, 1e-9)
        assertNull(strengthShareByTime(emptyList()))
    }

    @Test
    fun `consistency counts consecutive active weeks including current`() {
        val dates =
            listOf(
                tue, // tämä viikko
                LocalDate.of(2026, 9, 15), // edellinen
                LocalDate.of(2026, 9, 13), // kaksi viikkoa sitten (su)
                LocalDate.of(2026, 8, 25), // katko ennen tätä
            )
        assertEquals(3, consecutiveActiveWeeks(dates, tue, monday))
    }

    @Test
    fun `consistency does not break on unfinished current week`() {
        val dates = listOf(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 8))
        assertEquals(2, consecutiveActiveWeeks(dates, tue, monday))
    }

    @Test
    fun `consistency is zero without recent training`() {
        assertEquals(0, consecutiveActiveWeeks(listOf(LocalDate.of(2026, 9, 1)), tue, monday))
        assertEquals(0, consecutiveActiveWeeks(emptyList(), tue, monday))
    }

    @Test
    fun `consistency ignores future dates`() {
        assertEquals(0, consecutiveActiveWeeks(listOf(tue.plusWeeks(1)), tue, monday))
    }

    @Test
    fun `month grid pads to whole weeks`() {
        // Syyskuu 2026 alkaa tiistaina
        val mondayFirst = monthGrid(YearMonth.of(2026, 9), monday)
        assertEquals(35, mondayFirst.size)
        assertEquals(listOf(null, LocalDate.of(2026, 9, 1)), mondayFirst.take(2))
        assertEquals(LocalDate.of(2026, 9, 30), mondayFirst.filterNotNull().last())

        val sundayFirst = monthGrid(YearMonth.of(2026, 9), sunday)
        assertEquals(listOf(null, null, LocalDate.of(2026, 9, 1)), sundayFirst.take(3))
        assertEquals(0, sundayFirst.size % 7)
    }
}
