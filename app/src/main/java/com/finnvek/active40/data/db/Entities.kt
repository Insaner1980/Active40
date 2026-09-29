package com.finnvek.active40.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.finnvek.active40.domain.RunType
import com.finnvek.active40.domain.SorenessArea
import java.time.Instant
import java.time.LocalDate

// Tallennuskäytäntö: paino kilogrammoina, matka metreinä, kesto sekunteina.
// Päivämäärät paikallisina päivinä (LocalDate), aloitus- ja lopetushetket aikaleimoina (Instant).

/** Sisäänrakennettuja liikkeitä ei poisteta, vain arkistoidaan. Nimi toimii niiden pysyvänä avaimena. */
@Entity(tableName = "exercise")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isBuiltIn: Boolean,
    val isArchived: Boolean = false,
    val createdAt: Instant,
)

@Entity(tableName = "routine")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Instant,
    val sortOrder: Int,
)

@Entity(
    tableName = "routine_exercise",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"]),
    ],
    indices = [Index("routineId"), Index("exerciseId")],
)
data class RoutineExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val position: Int,
    val targetSets: Int? = null,
    val targetReps: Int? = null,
)

/**
 * Voimaharjoitus. endedAt on null, kun harjoitus on käynnissä.
 * name on käyttäjän antama nimi (valikon "Rename"); null tarkoittaa oletusotsikkoa.
 * restSeconds on tämän harjoituksen palautusajastimen pituus (säädettävissä harjoituskohtaisesti).
 */
@Entity(
    tableName = "strength_session",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("date"), Index("routineId"), Index("endedAt")],
)
data class StrengthSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val routineId: Long? = null,
    val name: String? = null,
    val note: String? = null,
    val effort: Int? = null,
    val restSeconds: Int = DEFAULT_REST_SECONDS,
)

const val DEFAULT_REST_SECONDS = 90

@Entity(
    tableName = "session_exercise",
    foreignKeys = [
        ForeignKey(
            entity = StrengthSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"]),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SessionExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val position: Int,
)

@Entity(
    tableName = "set_entry",
    foreignKeys = [
        ForeignKey(
            entity = SessionExercise::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionExerciseId")],
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val position: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Int? = null,
    val isCompleted: Boolean = false,
)

/** Juoksu. Vauhti lasketaan aina matkasta ja kestosta, sitä ei tallenneta. */
@Entity(tableName = "run", indices = [Index("date")])
data class Run(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val runType: RunType,
    val effort: Int? = null,
    val note: String? = null,
    val createdAt: Instant,
)

/**
 * Päivän kirjaus. Kaikki kentät ovat valinnaisia; puuttuva kirjaus tarkoittaa "ei tietoa".
 * soreness = null: ei vastattu. Joukko, jossa on NONE: ei arkuutta.
 */
@Entity(tableName = "check_in", indices = [Index("date", unique = true)])
data class CheckIn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val sleep: Int? = null,
    val energy: Int? = null,
    val soreness: Set<SorenessArea>? = null,
    val note: String? = null,
)

@Entity(tableName = "symptom")
data class Symptom(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isBuiltIn: Boolean,
    val isArchived: Boolean = false,
)

@Entity(
    tableName = "check_in_symptom",
    primaryKeys = ["checkInId", "symptomId"],
    foreignKeys = [
        ForeignKey(
            entity = CheckIn::class,
            parentColumns = ["id"],
            childColumns = ["checkInId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(entity = Symptom::class, parentColumns = ["id"], childColumns = ["symptomId"]),
    ],
    indices = [Index("symptomId")],
)
data class CheckInSymptom(
    val checkInId: Long,
    val symptomId: Long,
)
