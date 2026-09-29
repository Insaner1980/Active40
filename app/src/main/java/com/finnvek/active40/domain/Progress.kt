package com.finnvek.active40.domain

import com.finnvek.active40.data.db.SetEntry
import java.time.DayOfWeek
import java.time.LocalDate

enum class ProgressFilter { ALL, STRENGTH, RUNNING }

enum class ProgressRange { WEEKS_4, WEEKS_8, MONTHS_6, YEAR_1 }

/** Aikavälin ensimmäinen päivä. Viikkovälit alkavat viikon alusta, jotta viikkopalkit ovat kokonaisia. */
fun ProgressRange.startDate(
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
): LocalDate =
    when (this) {
        ProgressRange.WEEKS_4 -> weekStart(today, firstDayOfWeek).minusWeeks(3)
        ProgressRange.WEEKS_8 -> weekStart(today, firstDayOfWeek).minusWeeks(7)
        ProgressRange.MONTHS_6 -> today.minusMonths(6).plusDays(1)
        ProgressRange.YEAR_1 -> today.minusYears(1).plusDays(1)
    }

/** Yksi liikkeen sarja harjoituksen tiedoin (kaavion laskentaa varten). */
data class SessionSet(
    val sessionId: Long,
    val date: LocalDate,
    val set: SetEntry,
)

/** Kaavion piste: harjoituksen päivä ja arvo. */
data class SessionValue(
    val sessionId: Long,
    val date: LocalDate,
    val value: Double,
)

/** Painavin tehty sarja harjoituksittain, annetussa järjestyksessä. Harjoitukset ilman tehtyjä sarjoja jäävät pois. */
fun heaviestPerSession(rows: List<SessionSet>): List<SessionValue> =
    rows
        .groupBy { it.sessionId }
        .mapNotNull { (sessionId, list) ->
            heaviestCompletedSetKg(list.map { it.set })?.let { SessionValue(sessionId, list.first().date, it) }
        }

/** Kaavioon tarvitaan vähintään kaksi pistettä. */
const val MIN_TREND_POINTS = 2

fun hasTrend(values: List<Double?>): Boolean = values.count { it != null } >= MIN_TREND_POINTS
