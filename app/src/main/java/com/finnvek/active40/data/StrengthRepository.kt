package com.finnvek.active40.data

import androidx.room.withTransaction
import com.finnvek.active40.data.db.Active40Database
import com.finnvek.active40.data.db.Exercise
import com.finnvek.active40.data.db.SessionDetail
import com.finnvek.active40.data.db.SessionExercise
import com.finnvek.active40.data.db.SessionExerciseDetail
import com.finnvek.active40.data.db.SessionSummary
import com.finnvek.active40.data.db.SetEntry
import com.finnvek.active40.data.db.StrengthSession
import com.finnvek.active40.domain.SessionSet
import com.finnvek.active40.domain.initialSetValues
import com.finnvek.active40.domain.nextSetValues
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

/**
 * Voimaharjoitukset. Kaikki muutokset tallennetaan heti tietokantaan, jotta käynnissä oleva harjoitus
 * säilyy, vaikka sovellus suljettaisiin kesken.
 */
class StrengthRepository(
    private val db: Active40Database,
    private val routineRepository: RoutineRepository,
) {
    private val dao = db.strengthDao()

    fun observeActiveSession(): Flow<StrengthSession?> = dao.observeActiveSession()

    fun observeSummariesBetween(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<SessionSummary>> = dao.observeSummariesBetween(from, to)

    fun observeLatestSummary(): Flow<SessionSummary?> = dao.observeLatestSummary()

    fun observeExercisesWithData(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<Exercise>> = dao.observeExercisesWithData(from, to)

    fun observeExerciseSets(
        exerciseId: Long,
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<SessionSet>> =
        dao.observeExerciseSets(exerciseId, from, to).map { rows -> rows.map { SessionSet(it.sessionId, it.date, it.set) } }

    fun observeSessionDates(): Flow<List<LocalDate>> = dao.observeSessionDates()

    fun observeSession(id: Long): Flow<SessionDetail?> = dao.observeSessionDetail(id)

    /** Edellisen kerran tehdyt sarjat liikkeelle, käynnissä oleva harjoitus pois lukien. */
    suspend fun lastTime(
        exerciseId: Long,
        excludeSessionId: Long,
    ): List<SetEntry> = dao.lastCompletedSets(exerciseId, excludeSessionId)

    /**
     * Aloittaa harjoituksen tyhjänä tai rutiinista. Vain yksi harjoitus voi olla käynnissä:
     * jos sellainen on jo, palautetaan sen id eikä uutta luoda.
     */
    suspend fun startSession(routineId: Long?): Long =
        db.withTransaction {
            dao.getActiveSession()?.let { return@withTransaction it.id }
            val sessionId =
                dao.insertSession(StrengthSession(date = LocalDate.now(), startedAt = Instant.now(), routineId = routineId))
            if (routineId != null) {
                routineRepository.itemsFor(routineId).forEach { item ->
                    addExerciseInternal(sessionId, item.exerciseId, item.targetSets, item.targetReps)
                }
            }
            sessionId
        }

    suspend fun addExercise(
        sessionId: Long,
        exerciseId: Long,
    ) {
        db.withTransaction { addExerciseInternal(sessionId, exerciseId, targetSets = null, targetReps = null) }
    }

    private suspend fun addExerciseInternal(
        sessionId: Long,
        exerciseId: Long,
        targetSets: Int?,
        targetReps: Int?,
    ) {
        val position = dao.exercisesFor(sessionId).size
        val sessionExerciseId =
            dao.insertSessionExercise(SessionExercise(sessionId = sessionId, exerciseId = exerciseId, position = position))
        val values = initialSetValues(targetSets, targetReps, dao.lastCompletedSets(exerciseId, sessionId))
        dao.insertSets(
            values.mapIndexed { index, v ->
                SetEntry(sessionExerciseId = sessionExerciseId, position = index, weightKg = v.weightKg, reps = v.reps)
            },
        )
    }

    suspend fun addSet(detail: SessionExerciseDetail) {
        val lastTime = dao.lastCompletedSets(detail.sessionExercise.exerciseId, detail.sessionExercise.sessionId)
        val values = nextSetValues(detail.sets, lastTime)
        dao.insertSet(
            SetEntry(
                sessionExerciseId = detail.sessionExercise.id,
                position = (detail.sets.maxOfOrNull { it.position } ?: -1) + 1,
                weightKg = values.weightKg,
                reps = values.reps,
            ),
        )
    }

    suspend fun setWeight(
        setId: Long,
        weightKg: Double,
    ) = dao.updateSetWeight(setId, weightKg)

    suspend fun setReps(
        setId: Long,
        reps: Int,
    ) = dao.updateSetReps(setId, reps)

    suspend fun setRpe(
        setId: Long,
        rpe: Int?,
    ) = dao.updateSetRpe(setId, rpe)

    suspend fun setCompleted(
        setId: Long,
        completed: Boolean,
    ) = dao.updateSetCompleted(setId, completed)

    suspend fun removeLastSet(detail: SessionExerciseDetail) {
        detail.sets.maxByOrNull { it.position }?.let { dao.deleteSet(it) }
    }

    suspend fun removeExercise(sessionExercise: SessionExercise) {
        db.withTransaction {
            dao.deleteSessionExercise(sessionExercise)
            renumber(dao.exercisesFor(sessionExercise.sessionId))
        }
    }

    /** Siirtää liikettä harjoituksen sisällä (delta -1 ylös, +1 alas). */
    suspend fun moveExercise(
        sessionExercise: SessionExercise,
        delta: Int,
    ) {
        db.withTransaction {
            val list = dao.exercisesFor(sessionExercise.sessionId).toMutableList()
            val index = list.indexOfFirst { it.id == sessionExercise.id }
            val target = index + delta
            if (index < 0 || target !in list.indices) return@withTransaction
            list.add(target, list.removeAt(index))
            renumber(list)
        }
    }

    private suspend fun renumber(list: List<SessionExercise>) {
        dao.updateSessionExercises(list.mapIndexed { index, it -> it.copy(position = index) })
    }

    suspend fun updateSession(session: StrengthSession) = dao.updateSession(session)

    suspend fun discard(session: StrengthSession) = dao.deleteSession(session)

    /**
     * Päättää harjoituksen (tai tallentaa jo päättyneen muokkaukset): tekemättömät sarjat ja liikkeet,
     * joille ei jäänyt sarjoja, poistetaan. Päättyneen harjoituksen lopetusaika säilyy.
     * routineName != null tallentaa harjoituksen myös uudeksi rutiiniksi.
     */
    suspend fun finish(
        sessionId: Long,
        effort: Int?,
        note: String?,
        routineName: String?,
    ) {
        db.withTransaction {
            val session = dao.getSession(sessionId) ?: return@withTransaction
            dao.deleteIncompleteSets(sessionId)
            dao.deleteEmptyExercises(sessionId)
            renumber(dao.exercisesFor(sessionId))
            dao.updateSession(
                session.copy(endedAt = session.endedAt ?: Instant.now(), effort = effort, note = note?.takeIf { it.isNotBlank() }),
            )
            if (routineName != null) saveAsRoutine(sessionId, routineName)
        }
    }

    /**
     * Tallentaa harjoituksen liikkeet uudeksi rutiiniksi. Tavoitesarjat = tehtyjen sarjojen määrä,
     * tavoitetoistot vain jos kaikissa sarjoissa oli sama toistomäärä.
     */
    suspend fun saveAsRoutine(
        sessionId: Long,
        name: String,
    ): Long? {
        val detail = dao.getSessionDetail(sessionId) ?: return null
        val items =
            detail.exercises.sortedBy { it.sessionExercise.position }.map { exercise ->
                val done = exercise.sets.filter { it.isCompleted }
                RoutineItem(
                    exerciseId = exercise.exercise.id,
                    exerciseName = exercise.exercise.name,
                    targetSets = done.size.takeIf { it > 0 },
                    targetReps = done.map { it.reps }.distinct().singleOrNull(),
                )
            }
        if (items.isEmpty()) return null
        return routineRepository.save(id = null, name = name, items = items)
    }
}
