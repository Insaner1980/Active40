package com.finnvek.active40.data.backup

import com.finnvek.active40.data.db.BackupData
import com.finnvek.active40.data.db.CheckIn
import com.finnvek.active40.data.db.CheckInSymptom
import com.finnvek.active40.data.db.Exercise
import com.finnvek.active40.data.db.Routine
import com.finnvek.active40.data.db.RoutineExercise
import com.finnvek.active40.data.db.Run
import com.finnvek.active40.data.db.SessionExercise
import com.finnvek.active40.data.db.SetEntry
import com.finnvek.active40.data.db.StrengthSession
import com.finnvek.active40.data.db.Symptom
import com.finnvek.active40.domain.DistanceUnit
import com.finnvek.active40.domain.RunType
import com.finnvek.active40.domain.SorenessArea
import com.finnvek.active40.domain.WeightUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

/**
 * Vientitiedoston muoto. formatVersion kasvaa, jos muoto muuttuu; uudemmat versiot osaavat lukea vanhemmat.
 * Paino kg, matka m, kesto s, päivät ISO-muodossa (yyyy-MM-dd), aikaleimat millisekunteina.
 */
const val BACKUP_FORMAT_VERSION = 1

@Serializable
data class BackupFile(
    val formatVersion: Int,
    val exportedAt: Long,
    val settings: BackupSettings,
    val exercises: List<ExerciseDto> = emptyList(),
    val symptoms: List<SymptomDto> = emptyList(),
    val routines: List<RoutineDto> = emptyList(),
    val routineExercises: List<RoutineExerciseDto> = emptyList(),
    val sessions: List<SessionDto> = emptyList(),
    val sessionExercises: List<SessionExerciseDto> = emptyList(),
    val sets: List<SetDto> = emptyList(),
    val runs: List<RunDto> = emptyList(),
    val checkIns: List<CheckInDto> = emptyList(),
    val checkInSymptoms: List<CheckInSymptomDto> = emptyList(),
)

@Serializable
data class BackupSettings(
    val weightUnit: String,
    val distanceUnit: String,
    val symptomTrackingEnabled: Boolean,
)

@Serializable
data class ExerciseDto(
    val id: Long,
    val name: String,
    val isBuiltIn: Boolean,
    val isArchived: Boolean,
    val createdAt: Long,
)

@Serializable
data class SymptomDto(
    val id: Long,
    val name: String,
    val isBuiltIn: Boolean,
    val isArchived: Boolean,
)

@Serializable
data class RoutineDto(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val sortOrder: Int,
)

@Serializable
data class RoutineExerciseDto(
    val id: Long,
    val routineId: Long,
    val exerciseId: Long,
    val position: Int,
    val targetSets: Int? = null,
    val targetReps: Int? = null,
)

@Serializable
data class SessionDto(
    val id: Long,
    val date: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val routineId: Long? = null,
    val name: String? = null,
    val note: String? = null,
    val effort: Int? = null,
    val restSeconds: Int,
)

@Serializable
data class SessionExerciseDto(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val position: Int,
)

@Serializable
data class SetDto(
    val id: Long,
    val sessionExerciseId: Long,
    val position: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Int? = null,
    val isCompleted: Boolean,
)

@Serializable
data class RunDto(
    val id: Long,
    val date: String,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val runType: String,
    val effort: Int? = null,
    val note: String? = null,
    val createdAt: Long,
)

@Serializable
data class CheckInDto(
    val id: Long,
    val date: String,
    val sleep: Int? = null,
    val energy: Int? = null,
    val soreness: List<String>? = null,
    val note: String? = null,
)

@Serializable
data class CheckInSymptomDto(
    val checkInId: Long,
    val symptomId: Long,
)

/** Luettu ja tarkistettu vientitiedosto. */
data class ParsedBackup(
    val exportedAt: Instant,
    val weightUnit: WeightUnit,
    val distanceUnit: DistanceUnit,
    val symptomTrackingEnabled: Boolean,
    val data: BackupData,
)

enum class BackupError { UNREADABLE, NEWER_VERSION, INVALID_DATA }

class BackupException(
    val error: BackupError,
) : Exception(error.name)

val backupJson =
    Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

fun BackupData.toFile(
    exportedAt: Instant,
    weightUnit: WeightUnit,
    distanceUnit: DistanceUnit,
    symptomTrackingEnabled: Boolean,
) = BackupFile(
    formatVersion = BACKUP_FORMAT_VERSION,
    exportedAt = exportedAt.toEpochMilli(),
    settings = BackupSettings(weightUnit.name, distanceUnit.name, symptomTrackingEnabled),
    exercises = exercises.map { ExerciseDto(it.id, it.name, it.isBuiltIn, it.isArchived, it.createdAt.toEpochMilli()) },
    symptoms = symptoms.map { SymptomDto(it.id, it.name, it.isBuiltIn, it.isArchived) },
    routines = routines.map { RoutineDto(it.id, it.name, it.createdAt.toEpochMilli(), it.sortOrder) },
    routineExercises =
        routineExercises.map { RoutineExerciseDto(it.id, it.routineId, it.exerciseId, it.position, it.targetSets, it.targetReps) },
    sessions =
        sessions.map {
            SessionDto(
                it.id,
                it.date.toString(),
                it.startedAt.toEpochMilli(),
                it.endedAt?.toEpochMilli(),
                it.routineId,
                it.name,
                it.note,
                it.effort,
                it.restSeconds,
            )
        },
    sessionExercises = sessionExercises.map { SessionExerciseDto(it.id, it.sessionId, it.exerciseId, it.position) },
    sets = sets.map { SetDto(it.id, it.sessionExerciseId, it.position, it.weightKg, it.reps, it.rpe, it.isCompleted) },
    runs =
        runs.map {
            RunDto(
                it.id,
                it.date.toString(),
                it.distanceMeters,
                it.durationSeconds,
                it.runType.name,
                it.effort,
                it.note,
                it.createdAt.toEpochMilli(),
            )
        },
    checkIns =
        checkIns.map {
            CheckInDto(it.id, it.date.toString(), it.sleep, it.energy, it.soreness?.map(SorenessArea::name), it.note)
        },
    checkInSymptoms = checkInSymptoms.map { CheckInSymptomDto(it.checkInId, it.symptomId) },
)

/** Lukee ja tarkistaa vientitiedoston. Heittää BackupExceptionin, jos tiedostoa ei voi tuoda. */
fun parseBackup(text: String): ParsedBackup {
    val file =
        try {
            backupJson.decodeFromString<BackupFile>(text)
        } catch (_: IllegalArgumentException) {
            throw BackupException(BackupError.UNREADABLE)
        }
    if (file.formatVersion > BACKUP_FORMAT_VERSION) throw BackupException(BackupError.NEWER_VERSION)
    if (file.formatVersion < 1) throw BackupException(BackupError.UNREADABLE)
    val parsed =
        try {
            file.toParsed()
        } catch (_: IllegalArgumentException) {
            throw BackupException(BackupError.INVALID_DATA)
        } catch (_: java.time.format.DateTimeParseException) {
            throw BackupException(BackupError.INVALID_DATA)
        }
    if (!isConsistent(parsed.data)) throw BackupException(BackupError.INVALID_DATA)
    return parsed
}

private fun BackupFile.toParsed() =
    ParsedBackup(
        exportedAt = Instant.ofEpochMilli(exportedAt),
        weightUnit = WeightUnit.valueOf(settings.weightUnit),
        distanceUnit = DistanceUnit.valueOf(settings.distanceUnit),
        symptomTrackingEnabled = settings.symptomTrackingEnabled,
        data =
            BackupData(
                exercises = exercises.map { Exercise(it.id, it.name, it.isBuiltIn, it.isArchived, Instant.ofEpochMilli(it.createdAt)) },
                symptoms = symptoms.map { Symptom(it.id, it.name, it.isBuiltIn, it.isArchived) },
                routines = routines.map { Routine(it.id, it.name, Instant.ofEpochMilli(it.createdAt), it.sortOrder) },
                routineExercises =
                    routineExercises.map { RoutineExercise(it.id, it.routineId, it.exerciseId, it.position, it.targetSets, it.targetReps) },
                sessions =
                    sessions.map {
                        StrengthSession(
                            id = it.id,
                            date = LocalDate.parse(it.date),
                            startedAt = Instant.ofEpochMilli(it.startedAt),
                            endedAt = it.endedAt?.let(Instant::ofEpochMilli),
                            routineId = it.routineId,
                            name = it.name,
                            note = it.note,
                            effort = it.effort,
                            restSeconds = it.restSeconds,
                        )
                    },
                sessionExercises = sessionExercises.map { SessionExercise(it.id, it.sessionId, it.exerciseId, it.position) },
                sets = sets.map { SetEntry(it.id, it.sessionExerciseId, it.position, it.weightKg, it.reps, it.rpe, it.isCompleted) },
                runs =
                    runs.map {
                        Run(
                            id = it.id,
                            date = LocalDate.parse(it.date),
                            distanceMeters = it.distanceMeters,
                            durationSeconds = it.durationSeconds,
                            runType = RunType.valueOf(it.runType),
                            effort = it.effort,
                            note = it.note,
                            createdAt = Instant.ofEpochMilli(it.createdAt),
                        )
                    },
                checkIns =
                    checkIns.map {
                        CheckIn(
                            id = it.id,
                            date = LocalDate.parse(it.date),
                            sleep = it.sleep,
                            energy = it.energy,
                            soreness = it.soreness?.map(SorenessArea::valueOf)?.toSet(),
                            note = it.note,
                        )
                    },
                checkInSymptoms = checkInSymptoms.map { CheckInSymptom(it.checkInId, it.symptomId) },
            ),
    )

private val scale5 = 1..5
private val scale10 = 1..10

private fun <T> List<T>.uniqueBy(key: (T) -> Any) = map(key).toSet().size == size

/**
 * Tarkistaa, että data on eheää: tunnisteet ovat yksilöllisiä, viittaukset osoittavat olemassa oleviin riveihin
 * ja arvot ovat sallituissa rajoissa. Näin tuonti ei voi rikkoa tietokantaa.
 */
fun isConsistent(d: BackupData): Boolean {
    val exerciseIds = d.exercises.map { it.id }.toSet()
    val symptomIds = d.symptoms.map { it.id }.toSet()
    val routineIds = d.routines.map { it.id }.toSet()
    val sessionIds = d.sessions.map { it.id }.toSet()
    val sessionExerciseIds = d.sessionExercises.map { it.id }.toSet()
    val checkInIds = d.checkIns.map { it.id }.toSet()
    return listOf(
        d.exercises.all { it.id > 0 && it.name.isNotBlank() } && d.exercises.uniqueBy { it.id },
        d.symptoms.all { it.id > 0 && it.name.isNotBlank() } && d.symptoms.uniqueBy { it.id },
        d.routines.all { it.id > 0 } && d.routines.uniqueBy { it.id },
        d.routineExercises.all { it.id > 0 && it.routineId in routineIds && it.exerciseId in exerciseIds } &&
            d.routineExercises.uniqueBy { it.id },
        d.sessions.all { it.id > 0 && (it.routineId == null || it.routineId in routineIds) } &&
            d.sessions.all { s -> s.effort == null || s.effort in scale10 } &&
            d.sessions.all { it.restSeconds > 0 } &&
            d.sessions.uniqueBy { it.id } &&
            d.sessions.count { it.endedAt == null } <= 1,
        d.sessionExercises.all { it.id > 0 && it.sessionId in sessionIds && it.exerciseId in exerciseIds } &&
            d.sessionExercises.uniqueBy { it.id },
        d.sets.all { it.id > 0 && it.sessionExerciseId in sessionExerciseIds && it.weightKg >= 0 && it.reps >= 0 } &&
            d.sets.all { s -> s.rpe == null || s.rpe in scale10 } &&
            d.sets.uniqueBy { it.id },
        d.runs.all { it.id > 0 && it.distanceMeters > 0 && it.durationSeconds > 0 } &&
            d.runs.all { r -> r.effort == null || r.effort in scale10 } &&
            d.runs.uniqueBy { it.id },
        d.checkIns.all { c -> c.id > 0 && (c.sleep == null || c.sleep in scale5) && (c.energy == null || c.energy in scale5) } &&
            d.checkIns.uniqueBy { it.id } &&
            d.checkIns.uniqueBy { it.date },
        d.checkInSymptoms.all { it.checkInId in checkInIds && it.symptomId in symptomIds } &&
            d.checkInSymptoms.uniqueBy { it.checkInId to it.symptomId },
    ).all { it }
}
