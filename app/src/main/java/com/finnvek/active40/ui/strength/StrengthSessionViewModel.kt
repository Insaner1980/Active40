package com.finnvek.active40.ui.strength

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.active40.data.ExerciseRepository
import com.finnvek.active40.data.RoutineRepository
import com.finnvek.active40.data.SettingsRepository
import com.finnvek.active40.data.StrengthRepository
import com.finnvek.active40.data.db.Exercise
import com.finnvek.active40.data.db.SessionExerciseDetail
import com.finnvek.active40.data.db.SetEntry
import com.finnvek.active40.data.db.StrengthSession
import com.finnvek.active40.domain.WeightUnit
import com.finnvek.active40.domain.parseDecimal
import com.finnvek.active40.domain.toKg
import com.finnvek.active40.timer.RestTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class SessionUiState(
    val isLoading: Boolean = true,
    val session: StrengthSession? = null,
    val routineName: String? = null,
    val exercises: List<SessionExerciseDetail> = emptyList(),
    val currentId: Long? = null,
    val lastTime: Map<Long, List<SetEntry>> = emptyMap(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val restEndsAt: Instant? = null,
    val isClosed: Boolean = false,
) {
    /** Päättyneen harjoituksen muokkaus (historiasta): ei ajastimia. */
    val isEditingFinished: Boolean get() = session?.endedAt != null

    val current: SessionExerciseDetail? get() = exercises.firstOrNull { it.sessionExercise.id == currentId } ?: exercises.firstOrNull()
    val completedSetCount: Int get() = exercises.sumOf { e -> e.sets.count { it.isCompleted } }
    val undoneSetCount: Int get() = exercises.sumOf { e -> e.sets.count { !it.isCompleted } }
}

private const val MIN_REST_SECONDS = 15
private const val MAX_REST_SECONDS = 600
const val REST_STEP_SECONDS = 15
private const val MAX_RPE = 10

class StrengthSessionViewModel(
    private val sessionId: Long,
    private val strengthRepository: StrengthRepository,
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
    private val settingsRepository: SettingsRepository,
    private val restTimer: RestTimer,
) : ViewModel() {
    private val currentId = MutableStateFlow<Long?>(null)
    private val lastTime = MutableStateFlow<Map<Long, List<SetEntry>>>(emptyMap())
    private val routineName = MutableStateFlow<String?>(null)
    private val closed = MutableStateFlow(false)

    val activeExercises: StateFlow<List<Exercise>> =
        exerciseRepository.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val detail =
        strengthRepository.observeSession(sessionId).map { detail ->
            detail?.let { d ->
                // Rutiinin nimi ja edellisen kerran sarjat haetaan kerran liikettä kohden.
                if (routineName.value == null) {
                    d.session.routineId?.let { id -> routineName.value = routineRepository.get(id)?.name }
                }
                val missing = d.exercises.map { it.exercise.id }.filterNot { it in lastTime.value }
                if (missing.isNotEmpty()) {
                    val loaded = missing.associateWith { strengthRepository.lastTime(it, sessionId) }
                    lastTime.update { it + loaded }
                }
                d.copy(
                    exercises =
                        d.exercises
                            .sortedBy { it.sessionExercise.position }
                            .map { e -> e.copy(sets = e.sets.sortedBy { it.position }) },
                )
            }
        }

    val state: StateFlow<SessionUiState> =
        combine(
            detail,
            combine(currentId, lastTime, routineName, ::Triple),
            settingsRepository.settings,
            restTimer.endsAt,
            closed,
        ) { d, (current, last, routine), settings, restEndsAt, isClosed ->
            SessionUiState(
                isLoading = false,
                session = d?.session,
                routineName = routine,
                exercises = d?.exercises.orEmpty(),
                currentId =
                    current ?: d
                        ?.exercises
                        ?.firstOrNull { e -> e.sets.any { !it.isCompleted } }
                        ?.sessionExercise
                        ?.id,
                lastTime = last,
                weightUnit = settings.weightUnit,
                restEndsAt = restEndsAt,
                // Harjoitus poistettiin tai tallennettiin: näkymä sulkeutuu.
                isClosed = isClosed || d == null,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState())

    /** Ilmoituslupaa pyydetään ensimmäisellä kerralla, kun palautusajastin käynnistyy. */
    val shouldAskNotificationPermission = MutableStateFlow(false)

    fun select(sessionExerciseId: Long) {
        currentId.value = sessionExerciseId
    }

    fun setWeight(
        set: SetEntry,
        text: String,
        unit: WeightUnit,
    ) = persist { strengthRepository.setWeight(set.id, unit.toKg(parseDecimal(text) ?: 0.0)) }

    fun setReps(
        set: SetEntry,
        text: String,
    ) = persist { strengthRepository.setReps(set.id, text.toIntOrNull() ?: 0) }

    fun setRpe(
        set: SetEntry,
        text: String,
    ) = persist { strengthRepository.setRpe(set.id, text.toIntOrNull()?.takeIf { it in 1..MAX_RPE }) }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun toggleDone(set: SetEntry) {
        val done = !set.isCompleted
        persist { strengthRepository.setCompleted(set.id, done) }
        val session = state.value.session ?: return
        if (done && session.endedAt == null) {
            restTimer.start(session.restSeconds)
            viewModelScope.launch {
                if (!settingsRepository.notificationPermissionAsked.first()) {
                    shouldAskNotificationPermission.value = true
                }
            }
        }
    }

    fun onNotificationPermissionAsked() {
        shouldAskNotificationPermission.value = false
        viewModelScope.launch { settingsRepository.setNotificationPermissionAsked() }
    }

    fun addSet(detail: SessionExerciseDetail) {
        viewModelScope.launch { strengthRepository.addSet(detail) }
    }

    fun removeLastSet(detail: SessionExerciseDetail) {
        viewModelScope.launch { strengthRepository.removeLastSet(detail) }
    }

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            strengthRepository.addExercise(sessionId, exercise.id)
            selectLast()
        }
    }

    fun createAndAddExercise(name: String) {
        viewModelScope.launch {
            val id = exerciseRepository.add(name)
            strengthRepository.addExercise(sessionId, id)
            selectLast()
        }
    }

    private suspend fun selectLast() {
        val detail = strengthRepository.observeSession(sessionId).first()
        currentId.value =
            detail
                ?.exercises
                ?.maxByOrNull { it.sessionExercise.position }
                ?.sessionExercise
                ?.id
    }

    fun removeExercise(detail: SessionExerciseDetail) {
        if (currentId.value == detail.sessionExercise.id) currentId.value = null
        viewModelScope.launch { strengthRepository.removeExercise(detail.sessionExercise) }
    }

    fun moveExercise(
        detail: SessionExerciseDetail,
        delta: Int,
    ) {
        viewModelScope.launch { strengthRepository.moveExercise(detail.sessionExercise, delta) }
    }

    private fun updateSession(transform: (StrengthSession) -> StrengthSession) {
        val session = state.value.session ?: return
        viewModelScope.launch { strengthRepository.updateSession(transform(session)) }
    }

    fun rename(name: String) = updateSession { it.copy(name = name.trim().ifEmpty { null }) }

    fun setNote(note: String) = updateSession { it.copy(note = note.trim().ifEmpty { null }) }

    /** Muuttaa tämän harjoituksen palautusajan ja siirtää käynnissä olevaa ajastinta samalla määrällä. */
    fun adjustRest(deltaSeconds: Int) {
        val session = state.value.session ?: return
        val newLength = (session.restSeconds + deltaSeconds).coerceIn(MIN_REST_SECONDS, MAX_REST_SECONDS)
        val applied = newLength - session.restSeconds
        if (applied == 0) return
        updateSession { it.copy(restSeconds = newLength) }
        restTimer.shift(applied.toLong())
    }

    fun skipRest() = restTimer.stop()

    fun discard() {
        val session = state.value.session ?: return
        if (session.endedAt == null) restTimer.stop()
        closed.value = true
        viewModelScope.launch { strengthRepository.discard(session) }
    }

    fun finish(
        effort: Int?,
        note: String,
        routineName: String?,
    ) {
        if (!state.value.isEditingFinished) restTimer.stop()
        viewModelScope.launch {
            strengthRepository.finish(sessionId, effort, note, routineName?.trim()?.ifEmpty { null })
            closed.value = true
        }
    }
}
