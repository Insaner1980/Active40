package com.finnvek.active40.domain

import com.finnvek.active40.data.db.Run
import com.finnvek.active40.data.db.SetEntry
import com.finnvek.active40.data.db.StrengthSession
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class ActivityKind { STRENGTH, RUN }

/** Yksi päättynyt harjoitus laskentaa varten. Voimaharjoituksen kesto on endedAt miinus startedAt. */
data class TrainingActivity(
    val date: LocalDate,
    val kind: ActivityKind,
    val durationSeconds: Long,
    val distanceMeters: Double = 0.0,
)

/** Päättyneen voimaharjoituksen kesto; käynnissä olevalla 0. */
fun StrengthSession.durationSeconds(): Long = endedAt?.let { Duration.between(startedAt, it).seconds.coerceAtLeast(0) } ?: 0

fun StrengthSession.toActivity() = TrainingActivity(date, ActivityKind.STRENGTH, durationSeconds())

fun Run.toActivity() = TrainingActivity(date, ActivityKind.RUN, durationSeconds, distanceMeters)

data class WeekTotals(
    val strengthSessions: Int,
    val runs: Int,
    val runDistanceMeters: Double,
    val trainingSeconds: Long,
)

/** Arvo yhdelle viikolle; value on null, jos viikolla ei ole dataa. */
data class WeekValue(
    val weekStart: LocalDate,
    val value: Double?,
)

fun weekStart(
    date: LocalDate,
    firstDayOfWeek: DayOfWeek,
): LocalDate = date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

/** Viikon seitsemän päivää viikon ensimmäisestä päivästä alkaen. */
fun weekDays(
    date: LocalDate,
    firstDayOfWeek: DayOfWeek,
): List<LocalDate> {
    val start = weekStart(date, firstDayOfWeek)
    return (0L until 7L).map(start::plusDays)
}

/** Yhteenlasketut luvut annetuille harjoituksille (kutsuja rajaa ne halutulle viikolle). */
fun totals(activities: List<TrainingActivity>): WeekTotals {
    val runs = activities.filter { it.kind == ActivityKind.RUN }
    return WeekTotals(
        strengthSessions = activities.count { it.kind == ActivityKind.STRENGTH },
        runs = runs.size,
        runDistanceMeters = runs.sumOf { it.distanceMeters },
        trainingSeconds = activities.sumOf { it.durationSeconds },
    )
}

/** Viikkojen alkupäivät välillä from..to, molemmat viikot mukaan lukien. */
fun weekStartsBetween(
    from: LocalDate,
    to: LocalDate,
    firstDayOfWeek: DayOfWeek,
): List<LocalDate> {
    val first = weekStart(from, firstDayOfWeek)
    val last = weekStart(to, firstDayOfWeek)
    val weeks = ChronoUnit.WEEKS.between(first, last)
    return (0L..weeks).map(first::plusWeeks)
}

private fun List<TrainingActivity>.groupByWeek(firstDayOfWeek: DayOfWeek) = groupBy { weekStart(it.date, firstDayOfWeek) }

/** Juoksumatka metreinä viikoittain. Viikot ilman juoksuja saavat arvon 0. */
fun weeklyRunDistance(
    activities: List<TrainingActivity>,
    from: LocalDate,
    to: LocalDate,
    firstDayOfWeek: DayOfWeek,
): List<WeekValue> {
    val byWeek = activities.filter { it.kind == ActivityKind.RUN }.groupByWeek(firstDayOfWeek)
    return weekStartsBetween(from, to, firstDayOfWeek).map { start ->
        WeekValue(start, byWeek[start].orEmpty().sumOf { it.distanceMeters })
    }
}

/**
 * Viikon keskivauhti sekunteina matkayksikköä kohden: viikon kokonaisaika jaettuna kokonaismatkalla,
 * jolloin pidemmät juoksut painavat enemmän. Viikot ilman juoksuja saavat arvon null.
 */
fun weeklyAveragePace(
    activities: List<TrainingActivity>,
    from: LocalDate,
    to: LocalDate,
    firstDayOfWeek: DayOfWeek,
    unit: DistanceUnit,
): List<WeekValue> {
    val byWeek = activities.filter { it.kind == ActivityKind.RUN }.groupByWeek(firstDayOfWeek)
    return weekStartsBetween(from, to, firstDayOfWeek).map { start ->
        val runs = byWeek[start].orEmpty()
        WeekValue(start, paceSecondsPerUnit(runs.sumOf { it.distanceMeters }, runs.sumOf { it.durationSeconds }, unit))
    }
}

/** Painavin tehdyksi merkitty sarja. Null, jos tehtyjä sarjoja ei ole. */
fun heaviestCompletedSetKg(sets: List<SetEntry>): Double? = sets.filter { it.isCompleted }.maxOfOrNull { it.weightKg }

/** Muutos prosentteina ensimmäisestä viimeiseen. Null, jos lähtöarvo on nolla. */
fun percentageChange(
    first: Double,
    last: Double,
): Double? = if (first == 0.0) null else (last - first) / first * 100.0

/** Voimaharjoittelun osuus (0..1) harjoitusajasta. Null, jos harjoitusaikaa ei ole. */
fun strengthShareByTime(activities: List<TrainingActivity>): Double? {
    val total = activities.sumOf { it.durationSeconds }
    if (total <= 0L) return null
    val strength = activities.filter { it.kind == ActivityKind.STRENGTH }.sumOf { it.durationSeconds }
    return strength.toDouble() / total
}

/**
 * Peräkkäiset viikot, joilla on vähintään yksi harjoitus, kuluva viikko mukaan lukien.
 * Kuluva viikko on vielä kesken, joten jos siltä ei vielä ole harjoitusta, laskenta alkaa edellisestä viikosta
 * eikä putki katkea.
 */
fun consecutiveActiveWeeks(
    activityDates: Collection<LocalDate>,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
): Int {
    val activeWeeks = activityDates.filter { !it.isAfter(today) }.map { weekStart(it, firstDayOfWeek) }.toSet()
    var week = weekStart(today, firstDayOfWeek)
    if (week !in activeWeeks) week = week.minusWeeks(1)
    var count = 0
    while (week in activeWeeks) {
        count++
        week = week.minusWeeks(1)
    }
    return count
}

/**
 * Kuukausikalenterin solut viikkoriveittäin: null tarkoittaa tyhjää solua ennen kuun alkua tai lopun jälkeen.
 * Solujen määrä on aina seitsemällä jaollinen.
 */
fun monthGrid(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val days = (1..month.lengthOfMonth()).map(month::atDay)
    val cells = List(leading) { null } + days
    val trailing = (7 - cells.size % 7) % 7
    return cells + List(trailing) { null }
}
