package com.finnvek.active40.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise WHERE isArchived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: Long): Exercise?

    @Query("SELECT name FROM exercise WHERE isBuiltIn = 1")
    suspend fun builtInNames(): List<String>

    @Insert
    suspend fun insert(exercise: Exercise): Long

    @Insert
    suspend fun insertAll(exercises: List<Exercise>)

    @Update
    suspend fun update(exercise: Exercise)
}

@Dao
interface SymptomDao {
    @Query("SELECT * FROM symptom ORDER BY isBuiltIn DESC, id")
    fun observeAll(): Flow<List<Symptom>>

    @Query("SELECT name FROM symptom WHERE isBuiltIn = 1")
    suspend fun builtInNames(): List<String>

    @Insert
    suspend fun insert(symptom: Symptom): Long

    @Insert
    suspend fun insertAll(symptoms: List<Symptom>)

    @Update
    suspend fun update(symptom: Symptom)
}

/** Rutiinin liike ja liikkeen nimi samassa rivissä. */
data class RoutineExerciseWithName(
    @Embedded val routineExercise: RoutineExercise,
    val exerciseName: String,
)

/** Rutiini ja sen liikkeiden määrä listanäkymää varten. */
data class RoutineSummary(
    @Embedded val routine: Routine,
    val exerciseCount: Int,
)

@Dao
interface RoutineDao {
    @Query(
        """
        SELECT routine.*, (SELECT COUNT(*) FROM routine_exercise WHERE routineId = routine.id) AS exerciseCount
        FROM routine ORDER BY sortOrder, id
        """,
    )
    fun observeSummaries(): Flow<List<RoutineSummary>>

    @Query("SELECT * FROM routine WHERE id = :id")
    suspend fun getById(id: Long): Routine?

    @Query(
        """
        SELECT routine_exercise.*, exercise.name AS exerciseName
        FROM routine_exercise JOIN exercise ON exercise.id = routine_exercise.exerciseId
        WHERE routine_exercise.routineId = :routineId
        ORDER BY routine_exercise.position
        """,
    )
    suspend fun exercisesFor(routineId: Long): List<RoutineExerciseWithName>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM routine")
    suspend fun maxSortOrder(): Int

    @Insert
    suspend fun insert(routine: Routine): Long

    @Update
    suspend fun update(routine: Routine)

    @Delete
    suspend fun delete(routine: Routine)

    @Insert
    suspend fun insertExercises(exercises: List<RoutineExercise>)

    @Query("DELETE FROM routine_exercise WHERE routineId = :routineId")
    suspend fun deleteExercisesFor(routineId: Long)

    /** Korvaa rutiinin liikkeet annetulla listalla (järjestys = listan järjestys). */
    @Transaction
    suspend fun replaceExercises(
        routineId: Long,
        exercises: List<RoutineExercise>,
    ) {
        deleteExercisesFor(routineId)
        insertExercises(exercises.mapIndexed { index, it -> it.copy(id = 0, routineId = routineId, position = index) })
    }
}

/** Harjoituksen liike, sen nimi ja sarjat. */
data class SessionExerciseDetail(
    @Embedded val sessionExercise: SessionExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: Exercise,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId")
    val sets: List<SetEntry>,
)

/** Koko harjoitus liikkeineen ja sarjoineen. Järjestys (position) lajitellaan käyttäjäpuolella. */
data class SessionDetail(
    @Embedded val session: StrengthSession,
    @Relation(entity = SessionExercise::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<SessionExerciseDetail>,
)

/** Sarja ja sen harjoituksen tunniste ja päivä. */
data class SessionSetRow(
    val sessionId: Long,
    val date: LocalDate,
    @Embedded val set: SetEntry,
)

/** Päättyneen harjoituksen yhteenveto listoihin: rutiinin nimi ja tehtyjen sarjojen määrä. */
data class SessionSummary(
    @Embedded val session: StrengthSession,
    val routineName: String?,
    val completedSets: Int,
)

private const val SESSION_SUMMARY_SELECT = """
    SELECT s.*, r.name AS routineName,
        (SELECT COUNT(*) FROM set_entry e JOIN session_exercise x ON x.id = e.sessionExerciseId
         WHERE x.sessionId = s.id AND e.isCompleted = 1) AS completedSets
    FROM strength_session s LEFT JOIN routine r ON r.id = s.routineId
    WHERE s.endedAt IS NOT NULL
"""

@Dao
interface StrengthDao {
    @Query("$SESSION_SUMMARY_SELECT AND s.date BETWEEN :from AND :to ORDER BY s.date DESC, s.startedAt DESC")
    fun observeSummariesBetween(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<SessionSummary>>

    @Query("$SESSION_SUMMARY_SELECT ORDER BY s.date DESC, s.startedAt DESC LIMIT 1")
    fun observeLatestSummary(): Flow<SessionSummary?>

    /** Liikkeet, joista on tehtyjä sarjoja päättyneissä harjoituksissa aikavälillä. */
    @Query(
        """
        SELECT DISTINCT ex.* FROM exercise ex
        JOIN session_exercise x ON x.exerciseId = ex.id
        JOIN strength_session s ON s.id = x.sessionId
        JOIN set_entry e ON e.sessionExerciseId = x.id
        WHERE e.isCompleted = 1 AND s.endedAt IS NOT NULL AND s.date BETWEEN :from AND :to
        ORDER BY ex.name COLLATE NOCASE
        """,
    )
    fun observeExercisesWithData(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<Exercise>>

    /** Liikkeen sarjat päättyneistä harjoituksista aikavälillä, vanhin ensin. */
    @Query(
        """
        SELECT s.id AS sessionId, s.date AS date, e.* FROM set_entry e
        JOIN session_exercise x ON x.id = e.sessionExerciseId
        JOIN strength_session s ON s.id = x.sessionId
        WHERE x.exerciseId = :exerciseId AND s.endedAt IS NOT NULL AND s.date BETWEEN :from AND :to
        ORDER BY s.date, s.startedAt, e.position
        """,
    )
    fun observeExerciseSets(
        exerciseId: Long,
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<SessionSetRow>>

    @Query("SELECT DISTINCT date FROM strength_session WHERE endedAt IS NOT NULL")
    fun observeSessionDates(): Flow<List<LocalDate>>

    @Query("SELECT * FROM strength_session WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<StrengthSession?>

    @Query("SELECT * FROM strength_session WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): StrengthSession?

    @Query("SELECT * FROM strength_session WHERE id = :id")
    suspend fun getSession(id: Long): StrengthSession?

    @Transaction
    @Query("SELECT * FROM strength_session WHERE id = :id")
    fun observeSessionDetail(id: Long): Flow<SessionDetail?>

    @Transaction
    @Query("SELECT * FROM strength_session WHERE id = :id")
    suspend fun getSessionDetail(id: Long): SessionDetail?

    /**
     * Tehdyt sarjat viimeisimmästä päättyneestä harjoituksesta, jossa liike oli mukana ja siitä tehtiin
     * vähintään yksi sarja. excludeSessionId rajaa pois käynnissä olevan harjoituksen.
     */
    @Query(
        """
        SELECT * FROM set_entry WHERE isCompleted = 1 AND sessionExerciseId = (
            SELECT sx.id FROM session_exercise sx
            JOIN strength_session s ON s.id = sx.sessionId
            WHERE sx.exerciseId = :exerciseId AND s.endedAt IS NOT NULL AND s.id != :excludeSessionId
              AND EXISTS (SELECT 1 FROM set_entry e WHERE e.sessionExerciseId = sx.id AND e.isCompleted = 1)
            ORDER BY s.date DESC, s.startedAt DESC, sx.position DESC
            LIMIT 1
        )
        ORDER BY position
        """,
    )
    suspend fun lastCompletedSets(
        exerciseId: Long,
        excludeSessionId: Long,
    ): List<SetEntry>

    @Insert
    suspend fun insertSession(session: StrengthSession): Long

    @Update
    suspend fun updateSession(session: StrengthSession)

    @Delete
    suspend fun deleteSession(session: StrengthSession)

    @Query("SELECT * FROM session_exercise WHERE sessionId = :sessionId ORDER BY position")
    suspend fun exercisesFor(sessionId: Long): List<SessionExercise>

    @Insert
    suspend fun insertSessionExercise(exercise: SessionExercise): Long

    @Update
    suspend fun updateSessionExercises(exercises: List<SessionExercise>)

    @Delete
    suspend fun deleteSessionExercise(exercise: SessionExercise)

    @Query("SELECT * FROM set_entry WHERE sessionExerciseId = :sessionExerciseId ORDER BY position")
    suspend fun setsFor(sessionExerciseId: Long): List<SetEntry>

    @Insert
    suspend fun insertSet(set: SetEntry): Long

    @Insert
    suspend fun insertSets(sets: List<SetEntry>)

    // Kenttäkohtaiset päivitykset: nopeat peräkkäiset muokkaukset eri kenttiin eivät ylikirjoita toisiaan.
    @Query("UPDATE set_entry SET weightKg = :weightKg WHERE id = :id")
    suspend fun updateSetWeight(
        id: Long,
        weightKg: Double,
    )

    @Query("UPDATE set_entry SET reps = :reps WHERE id = :id")
    suspend fun updateSetReps(
        id: Long,
        reps: Int,
    )

    @Query("UPDATE set_entry SET rpe = :rpe WHERE id = :id")
    suspend fun updateSetRpe(
        id: Long,
        rpe: Int?,
    )

    @Query("UPDATE set_entry SET isCompleted = :completed WHERE id = :id")
    suspend fun updateSetCompleted(
        id: Long,
        completed: Boolean,
    )

    @Delete
    suspend fun deleteSet(set: SetEntry)

    @Query(
        """
        DELETE FROM set_entry WHERE isCompleted = 0
        AND sessionExerciseId IN (SELECT id FROM session_exercise WHERE sessionId = :sessionId)
        """,
    )
    suspend fun deleteIncompleteSets(sessionId: Long)

    @Query(
        """
        DELETE FROM session_exercise WHERE sessionId = :sessionId
        AND NOT EXISTS (SELECT 1 FROM set_entry WHERE sessionExerciseId = session_exercise.id)
        """,
    )
    suspend fun deleteEmptyExercises(sessionId: Long)
}

@Dao
interface RunDao {
    @Query("SELECT * FROM run WHERE id = :id")
    suspend fun getById(id: Long): Run?

    @Query("SELECT DISTINCT date FROM run")
    fun observeRunDates(): Flow<List<LocalDate>>

    @Query("SELECT * FROM run ORDER BY date DESC, createdAt DESC LIMIT 1")
    fun observeLatest(): Flow<Run?>

    @Query("SELECT * FROM run WHERE date BETWEEN :from AND :to ORDER BY date, createdAt")
    fun observeBetween(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<Run>>

    @Insert
    suspend fun insert(run: Run): Long

    @Update
    suspend fun update(run: Run)

    @Delete
    suspend fun delete(run: Run)
}

/** Päivän kirjaus ja siihen merkityt oireet. */
data class CheckInWithSymptoms(
    @Embedded val checkIn: CheckIn,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(CheckInSymptom::class, parentColumn = "checkInId", entityColumn = "symptomId"),
    )
    val symptoms: List<Symptom>,
)

@Dao
interface CheckInDao {
    @Transaction
    @Query("SELECT * FROM check_in ORDER BY date DESC")
    fun observeAllWithSymptoms(): Flow<List<CheckInWithSymptoms>>

    @Transaction
    @Query("SELECT * FROM check_in WHERE date = :date")
    fun observeWithSymptomsByDate(date: LocalDate): Flow<CheckInWithSymptoms?>

    @Query("SELECT * FROM check_in WHERE date = :date")
    suspend fun getByDate(date: LocalDate): CheckIn?

    @Transaction
    @Query("SELECT * FROM check_in WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    fun observeBetweenWithSymptoms(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<CheckInWithSymptoms>>

    @Query("SELECT symptomId FROM check_in_symptom WHERE checkInId = :checkInId")
    suspend fun symptomIdsFor(checkInId: Long): List<Long>

    @Insert
    suspend fun insert(checkIn: CheckIn): Long

    @Update
    suspend fun update(checkIn: CheckIn)

    @Delete
    suspend fun delete(checkIn: CheckIn)

    @Insert
    suspend fun insertSymptoms(links: List<CheckInSymptom>)

    @Query("DELETE FROM check_in_symptom WHERE checkInId = :checkInId")
    suspend fun deleteSymptomsFor(checkInId: Long)

    /**
     * Tallentaa kirjauksen (uusi tai olemassa oleva samalle päivälle) ja sen oireet.
     * symptomIds = null jättää oireet ennalleen; tätä käytetään, kun oireseuranta on pois päältä,
     * jotta piilotettu oiredata ei katoa.
     */
    @Transaction
    suspend fun save(
        checkIn: CheckIn,
        symptomIds: Set<Long>?,
    ): Long {
        val existing = getByDate(checkIn.date)
        val id =
            if (existing == null) {
                insert(checkIn.copy(id = 0))
            } else {
                update(checkIn.copy(id = existing.id))
                existing.id
            }
        if (symptomIds != null) {
            deleteSymptomsFor(id)
            insertSymptoms(symptomIds.map { CheckInSymptom(checkInId = id, symptomId = it) })
        }
        return id
    }
}
