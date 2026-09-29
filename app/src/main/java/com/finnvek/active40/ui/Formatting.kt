package com.finnvek.active40.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.finnvek.active40.R
import com.finnvek.active40.data.db.CheckInWithSymptoms
import com.finnvek.active40.data.db.Run
import com.finnvek.active40.data.db.SetEntry
import com.finnvek.active40.data.db.StrengthSession
import com.finnvek.active40.domain.DistanceUnit
import com.finnvek.active40.domain.LastTimeSummary
import com.finnvek.active40.domain.RunType
import com.finnvek.active40.domain.SorenessArea
import com.finnvek.active40.domain.WeightUnit
import com.finnvek.active40.domain.formatClock
import com.finnvek.active40.domain.formatDistance
import com.finnvek.active40.domain.formatPace
import com.finnvek.active40.domain.formatWeight
import com.finnvek.active40.domain.paceSecondsPerUnit
import com.finnvek.active40.domain.summarizeLastTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeightUnit.label(): String =
    stringResource(
        when (this) {
            WeightUnit.KG -> R.string.unit_kg
            WeightUnit.LB -> R.string.unit_lb
        },
    )

@Composable
fun DistanceUnit.label(): String =
    stringResource(
        when (this) {
            DistanceUnit.KM -> R.string.unit_km
            DistanceUnit.MI -> R.string.unit_mi
        },
    )

/** "Last time: 3 × 10 at 80 kg" tai "First time". Paino 0 (kehonpaino) näytetään ilman painoa. */
@Composable
fun lastTimeText(
    lastTime: List<SetEntry>,
    unit: WeightUnit,
): String {
    val unitLabel = unit.label()
    val summary = summarizeLastTime(lastTime) ?: return stringResource(R.string.first_time)
    val body =
        when (summary) {
            is LastTimeSummary.Uniform -> {
                if (summary.weightKg == 0.0) {
                    stringResource(R.string.last_time_uniform_bodyweight, summary.sets, summary.reps)
                } else {
                    stringResource(R.string.last_time_uniform, summary.sets, summary.reps, formatWeight(summary.weightKg, unit), unitLabel)
                }
            }

            is LastTimeSummary.SameWeight -> {
                if (summary.weightKg == 0.0) {
                    summary.reps.joinToString(", ")
                } else {
                    stringResource(
                        R.string.last_time_same_weight,
                        summary.reps.joinToString(", "),
                        formatWeight(summary.weightKg, unit),
                        unitLabel,
                    )
                }
            }

            is LastTimeSummary.Mixed -> {
                val itemTemplate = stringResource(R.string.last_time_item)
                summary.sets.joinToString(", ") { itemTemplate.format(it.reps, formatWeight(it.weightKg, unit), unitLabel) }
            }
        }
    return stringResource(R.string.last_time, body)
}

@Composable
fun RunType.label(): String =
    stringResource(
        when (this) {
            RunType.EASY -> R.string.run_type_easy
            RunType.TEMPO -> R.string.run_type_tempo
            RunType.INTERVALS -> R.string.run_type_intervals
            RunType.LONG -> R.string.run_type_long
            RunType.OTHER -> R.string.run_type_other
        },
    )

/** Pitkä päiväys ilman vuotta laitteen kieliasetuksen mukaan, esim. "Tuesday, September 22". */
fun formatDayLong(date: LocalDate): String {
    val locale = Locale.getDefault()
    return date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale))
}

@Composable
fun SorenessArea.label(): String =
    stringResource(
        when (this) {
            SorenessArea.NONE -> R.string.soreness_none
            SorenessArea.LEGS -> R.string.soreness_legs
            SorenessArea.BACK -> R.string.soreness_back
            SorenessArea.SHOULDERS -> R.string.soreness_shoulders
            SorenessArea.ARMS -> R.string.soreness_arms
            SorenessArea.WHOLE_BODY -> R.string.soreness_whole_body
        },
    )

/** Unen sanallinen arvo 1 to 5 (Poor ... Great). */
@Composable
fun sleepWord(value: Int): String = stringArrayResource(R.array.sleep_words)[value - 1]

/** Energian sanallinen arvo 1 to 5 (Very low ... High). */
@Composable
fun energyWord(value: Int): String = stringArrayResource(R.array.energy_words)[value - 1]

/**
 * Kirjauksen tiivistelmä riveinä. Vain vastatut kentät näytetään; oireet vain, kun seuranta on päällä.
 * Puuttuva vastaus ei tarkoita huonoa palautumista eikä "ei oireita", joten sitä ei tulkita mitenkään.
 */
@Composable
fun checkInSummaryLines(
    item: CheckInWithSymptoms,
    showSymptoms: Boolean,
): List<String> {
    val c = item.checkIn
    return buildList {
        c.sleep?.let { add(stringResource(R.string.summary_sleep, sleepWord(it))) }
        c.energy?.let { add(stringResource(R.string.summary_energy, energyWord(it))) }
        c.soreness?.takeIf { it.isNotEmpty() }?.let { areas ->
            val labels = SorenessArea.entries.filter { it in areas }.map { it.label() }
            add(stringResource(R.string.summary_soreness, labels.joinToString(", ")))
        }
        if (showSymptoms && item.symptoms.isNotEmpty()) {
            add(stringResource(R.string.summary_symptoms, item.symptoms.joinToString(", ") { it.name }))
        }
        c.note?.let { add(stringResource(R.string.summary_note, it)) }
    }
}

/** Harjoitusaika muodossa "4 h 12 min" tai "45 min". */
@Composable
fun formatTrainingTime(seconds: Long): String {
    val totalMinutes = seconds / 60
    val hours = totalMinutes / 60
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours.toInt(), (totalMinutes % 60).toInt())
    } else {
        stringResource(R.string.duration_minutes, totalMinutes.toInt())
    }
}

/** Voimaharjoituksen otsikko: oma nimi, muuten rutiinin nimi, muuten "Strength session". */
@Composable
fun sessionTitle(
    session: StrengthSession,
    routineName: String?,
): String = session.name ?: routineName ?: stringResource(R.string.strength_session)

/** Harjoituksen päänumerot: sarjat ja kesto. */
@Composable
fun sessionNumbers(
    completedSets: Int,
    durationSeconds: Long,
): String =
    listOf(
        pluralStringResource(R.plurals.completed_sets, completedSets, completedSets),
        formatTrainingTime(durationSeconds),
    ).joinToString(", ")

/** Juoksun päänumerot: matka, kesto ja vauhti, esim. "5.24 km, 31:20, 5:59 /km". */
@Composable
fun runNumbers(
    run: Run,
    unit: DistanceUnit,
): String {
    val unitLabel = unit.label()
    val pace = paceSecondsPerUnit(run.distanceMeters, run.durationSeconds, unit)
    return listOfNotNull(
        "${formatDistance(run.distanceMeters, unit)} $unitLabel",
        formatClock(run.durationSeconds),
        pace?.let { stringResource(R.string.pace_value, formatPace(it), unitLabel) },
    ).joinToString(", ")
}
