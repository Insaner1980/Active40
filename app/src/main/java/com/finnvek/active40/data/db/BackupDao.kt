package com.finnvek.active40.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/** Kaiken datan luku ja korvaus vientiä, tuontia ja kaiken datan poistoa varten. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM exercise")
    suspend fun exercises(): List<Exercise>

    @Query("SELECT * FROM symptom")
    suspend fun symptoms(): List<Symptom>

    @Query("SELECT * FROM routine")
    suspend fun routines(): List<Routine>

    @Query("SELECT * FROM routine_exercise")
    suspend fun routineExercises(): List<RoutineExercise>

    @Query("SELECT * FROM strength_session")
    suspend fun sessions(): List<StrengthSession>

    @Query("SELECT * FROM session_exercise")
    suspend fun sessionExercises(): List<SessionExercise>

    @Query("SELECT * FROM set_entry")
    suspend fun sets(): List<SetEntry>

    @Query("SELECT * FROM run")
    suspend fun runs(): List<Run>

    @Query("SELECT * FROM check_in")
    suspend fun checkIns(): List<CheckIn>

    @Query("SELECT * FROM check_in_symptom")
    suspend fun checkInSymptoms(): List<CheckInSymptom>

    @Insert
    suspend fun insertExercises(items: List<Exercise>)

    @Insert
    suspend fun insertSymptoms(items: List<Symptom>)

    @Insert
    suspend fun insertRoutines(items: List<Routine>)

    @Insert
    suspend fun insertRoutineExercises(items: List<RoutineExercise>)

    @Insert
    suspend fun insertSessions(items: List<StrengthSession>)

    @Insert
    suspend fun insertSessionExercises(items: List<SessionExercise>)

    @Insert
    suspend fun insertSets(items: List<SetEntry>)

    @Insert
    suspend fun insertRuns(items: List<Run>)

    @Insert
    suspend fun insertCheckIns(items: List<CheckIn>)

    @Insert
    suspend fun insertCheckInSymptoms(items: List<CheckInSymptom>)

    @Query("DELETE FROM check_in_symptom")
    suspend fun deleteCheckInSymptoms()

    @Query("DELETE FROM check_in")
    suspend fun deleteCheckIns()

    @Query("DELETE FROM symptom")
    suspend fun deleteSymptoms()

    @Query("DELETE FROM set_entry")
    suspend fun deleteSets()

    @Query("DELETE FROM session_exercise")
    suspend fun deleteSessionExercises()

    @Query("DELETE FROM strength_session")
    suspend fun deleteSessions()

    @Query("DELETE FROM routine_exercise")
    suspend fun deleteRoutineExercises()

    @Query("DELETE FROM routine")
    suspend fun deleteRoutines()

    @Query("DELETE FROM run")
    suspend fun deleteRuns()

    @Query("DELETE FROM exercise")
    suspend fun deleteExercises()

    /** Poistaa kaikki rivit lapsitauluista alkaen, jotta viiteavaimet eivät estä poistoa. */
    @Transaction
    suspend fun deleteAll() {
        deleteCheckInSymptoms()
        deleteCheckIns()
        deleteSymptoms()
        deleteSets()
        deleteSessionExercises()
        deleteSessions()
        deleteRoutineExercises()
        deleteRoutines()
        deleteRuns()
        deleteExercises()
    }

    /** Korvaa kaiken datan: poistaa nykyisen ja lisää annetun vanhemmista lapsiin. */
    @Transaction
    suspend fun replaceAll(data: BackupData) {
        deleteAll()
        insertExercises(data.exercises)
        insertSymptoms(data.symptoms)
        insertRoutines(data.routines)
        insertRoutineExercises(data.routineExercises)
        insertSessions(data.sessions)
        insertSessionExercises(data.sessionExercises)
        insertSets(data.sets)
        insertRuns(data.runs)
        insertCheckIns(data.checkIns)
        insertCheckInSymptoms(data.checkInSymptoms)
    }
}

/** Kaikki tietokannan rivit yhdessä (vienti ja tuonti). */
data class BackupData(
    val exercises: List<Exercise>,
    val symptoms: List<Symptom>,
    val routines: List<Routine>,
    val routineExercises: List<RoutineExercise>,
    val sessions: List<StrengthSession>,
    val sessionExercises: List<SessionExercise>,
    val sets: List<SetEntry>,
    val runs: List<Run>,
    val checkIns: List<CheckIn>,
    val checkInSymptoms: List<CheckInSymptom>,
)
