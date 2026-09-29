package com.finnvek.active40.data

import androidx.room.withTransaction
import com.finnvek.active40.data.db.Active40Database
import com.finnvek.active40.data.db.CheckIn
import com.finnvek.active40.data.db.CheckInWithSymptoms
import com.finnvek.active40.data.db.Exercise
import com.finnvek.active40.data.db.Routine
import com.finnvek.active40.data.db.RoutineExercise
import com.finnvek.active40.data.db.RoutineExerciseWithName
import com.finnvek.active40.data.db.RoutineSummary
import com.finnvek.active40.data.db.Run
import com.finnvek.active40.data.db.Symptom
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

class ExerciseRepository(
    db: Active40Database,
) {
    private val dao = db.exerciseDao()

    fun observeAll(): Flow<List<Exercise>> = dao.observeAll()

    fun observeActive(): Flow<List<Exercise>> = dao.observeActive()

    suspend fun add(name: String): Long = dao.insert(Exercise(name = name.trim(), isBuiltIn = false, createdAt = Instant.now()))

    /** Vain käyttäjän omat liikkeet voi nimetä uudelleen. */
    suspend fun rename(
        exercise: Exercise,
        name: String,
    ) {
        if (!exercise.isBuiltIn) dao.update(exercise.copy(name = name.trim()))
    }

    suspend fun setArchived(
        exercise: Exercise,
        archived: Boolean,
    ) = dao.update(exercise.copy(isArchived = archived))
}

class SymptomRepository(
    db: Active40Database,
) {
    private val dao = db.symptomDao()

    fun observeAll(): Flow<List<Symptom>> = dao.observeAll()

    suspend fun add(name: String): Long = dao.insert(Symptom(name = name.trim(), isBuiltIn = false))

    suspend fun rename(
        symptom: Symptom,
        name: String,
    ) {
        if (!symptom.isBuiltIn) dao.update(symptom.copy(name = name.trim()))
    }

    suspend fun setArchived(
        symptom: Symptom,
        archived: Boolean,
    ) = dao.update(symptom.copy(isArchived = archived))
}

/** Rutiinin liike muokkausta varten. */
data class RoutineItem(
    val exerciseId: Long,
    val exerciseName: String,
    val targetSets: Int? = null,
    val targetReps: Int? = null,
)

class RoutineRepository(
    private val db: Active40Database,
) {
    private val dao = db.routineDao()

    fun observeSummaries(): Flow<List<RoutineSummary>> = dao.observeSummaries()

    suspend fun get(id: Long): Routine? = dao.getById(id)

    suspend fun itemsFor(routineId: Long): List<RoutineItem> = dao.exercisesFor(routineId).map(RoutineExerciseWithName::toItem)

    /** Luo uuden (id = null) tai päivittää olemassa olevan rutiinin ja sen liikkeet. Palauttaa rutiinin id:n. */
    suspend fun save(
        id: Long?,
        name: String,
        items: List<RoutineItem>,
    ): Long =
        db.withTransaction {
            val routineId =
                if (id == null) {
                    dao.insert(Routine(name = name.trim(), createdAt = Instant.now(), sortOrder = dao.maxSortOrder() + 1))
                } else {
                    val existing = requireNotNull(dao.getById(id))
                    dao.update(existing.copy(name = name.trim()))
                    id
                }
            dao.replaceExercises(
                routineId,
                items.map {
                    RoutineExercise(
                        routineId = routineId,
                        exerciseId = it.exerciseId,
                        position = 0,
                        targetSets = it.targetSets,
                        targetReps = it.targetReps,
                    )
                },
            )
            routineId
        }

    /** Tallentaa rutiinien järjestyksen annetun listan mukaan. */
    suspend fun reorder(routines: List<Routine>) {
        db.withTransaction {
            routines.forEachIndexed { index, routine -> dao.update(routine.copy(sortOrder = index)) }
        }
    }

    suspend fun delete(routine: Routine) = dao.delete(routine)
}

private fun RoutineExerciseWithName.toItem() =
    RoutineItem(
        exerciseId = routineExercise.exerciseId,
        exerciseName = exerciseName,
        targetSets = routineExercise.targetSets,
        targetReps = routineExercise.targetReps,
    )

class RunRepository(
    db: Active40Database,
) {
    private val dao = db.runDao()

    fun observeBetween(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<Run>> = dao.observeBetween(from, to)

    fun observeLatest(): Flow<Run?> = dao.observeLatest()

    fun observeDates(): Flow<List<LocalDate>> = dao.observeRunDates()

    suspend fun get(id: Long): Run? = dao.getById(id)

    suspend fun save(run: Run): Long =
        if (run.id == 0L) {
            dao.insert(run)
        } else {
            dao.update(run)
            run.id
        }

    suspend fun delete(run: Run) = dao.delete(run)
}

class CheckInRepository(
    db: Active40Database,
) {
    private val dao = db.checkInDao()

    fun observeAllWithSymptoms(): Flow<List<CheckInWithSymptoms>> = dao.observeAllWithSymptoms()

    fun observeWithSymptomsByDate(date: LocalDate): Flow<CheckInWithSymptoms?> = dao.observeWithSymptomsByDate(date)

    suspend fun getByDate(date: LocalDate): CheckIn? = dao.getByDate(date)

    fun observeBetweenWithSymptoms(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<CheckInWithSymptoms>> = dao.observeBetweenWithSymptoms(from, to)

    suspend fun symptomIdsFor(checkIn: CheckIn): Set<Long> = dao.symptomIdsFor(checkIn.id).toSet()

    /** symptomIds = null säilyttää aiemmat oireet (oireseuranta pois päältä). */
    suspend fun save(
        checkIn: CheckIn,
        symptomIds: Set<Long>?,
    ): Long = dao.save(checkIn, symptomIds)

    suspend fun delete(checkIn: CheckIn) = dao.delete(checkIn)
}
