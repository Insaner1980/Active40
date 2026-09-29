package com.finnvek.active40.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.active40.data.ExerciseRepository
import com.finnvek.active40.data.RoutineItem
import com.finnvek.active40.data.RoutineRepository
import com.finnvek.active40.data.db.Exercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Muokattava rivi. key pitää Compose-rivit vakaina, vaikka sama liike olisi rutiinissa kahdesti. */
data class EditableRoutineItem(
    val key: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val setsText: String = "",
    val repsText: String = "",
)

data class RoutineEditState(
    val isNew: Boolean,
    val isLoading: Boolean = true,
    val name: String = "",
    val items: List<EditableRoutineItem> = emptyList(),
    val hasChanges: Boolean = false,
    val isSaved: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && items.isNotEmpty()
}

const val NEW_ROUTINE_ID = 0L

class RoutineEditViewModel(
    private val routineId: Long,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RoutineEditState(isNew = routineId == NEW_ROUTINE_ID))
    val state: StateFlow<RoutineEditState> = _state.asStateFlow()

    val activeExercises: StateFlow<List<Exercise>> =
        exerciseRepository.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var nextKey = 0L

    init {
        viewModelScope.launch {
            if (routineId == NEW_ROUTINE_ID) {
                _state.update { it.copy(isLoading = false) }
            } else {
                val routine = routineRepository.get(routineId)
                val items = routineRepository.itemsFor(routineId)
                _state.update {
                    it.copy(
                        isLoading = false,
                        name = routine?.name.orEmpty(),
                        items =
                            items.map { item ->
                                EditableRoutineItem(
                                    key = nextKey++,
                                    exerciseId = item.exerciseId,
                                    exerciseName = item.exerciseName,
                                    setsText = item.targetSets?.toString().orEmpty(),
                                    repsText = item.targetReps?.toString().orEmpty(),
                                )
                            },
                    )
                }
            }
        }
    }

    private fun edit(transform: (RoutineEditState) -> RoutineEditState) {
        _state.update { transform(it).copy(hasChanges = true) }
    }

    private fun editItem(
        key: Long,
        transform: (EditableRoutineItem) -> EditableRoutineItem,
    ) = edit { state -> state.copy(items = state.items.map { if (it.key == key) transform(it) else it }) }

    fun setName(name: String) = edit { it.copy(name = name) }

    fun addExercise(exercise: Exercise) =
        edit {
            it.copy(items = it.items + EditableRoutineItem(key = nextKey++, exerciseId = exercise.id, exerciseName = exercise.name))
        }

    /** Luo uuden oman liikkeen ja lisää sen rutiiniin. */
    fun createAndAddExercise(name: String) {
        viewModelScope.launch {
            val id = exerciseRepository.add(name)
            edit {
                it.copy(items = it.items + EditableRoutineItem(key = nextKey++, exerciseId = id, exerciseName = name.trim()))
            }
        }
    }

    fun remove(key: Long) = edit { state -> state.copy(items = state.items.filterNot { it.key == key }) }

    fun move(
        key: Long,
        delta: Int,
    ) = edit { state ->
        val list = state.items.toMutableList()
        val index = list.indexOfFirst { it.key == key }
        val target = index + delta
        if (index < 0 || target !in list.indices) return@edit state
        list.add(target, list.removeAt(index))
        state.copy(items = list)
    }

    fun setSets(
        key: Long,
        text: String,
    ) = editItem(key) { it.copy(setsText = text.digitsOnly(MAX_SETS_DIGITS)) }

    fun setReps(
        key: Long,
        text: String,
    ) = editItem(key) { it.copy(repsText = text.digitsOnly(MAX_REPS_DIGITS)) }

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        viewModelScope.launch {
            routineRepository.save(
                id = if (current.isNew) null else routineId,
                name = current.name,
                items =
                    current.items.map {
                        RoutineItem(
                            exerciseId = it.exerciseId,
                            exerciseName = it.exerciseName,
                            targetSets = it.setsText.toPositiveIntOrNull(),
                            targetReps = it.repsText.toPositiveIntOrNull(),
                        )
                    },
            )
            _state.update { it.copy(isSaved = true) }
        }
    }

    private companion object {
        const val MAX_SETS_DIGITS = 2
        const val MAX_REPS_DIGITS = 3
    }
}

private fun String.digitsOnly(maxLength: Int) = filter(Char::isDigit).take(maxLength)

private fun String.toPositiveIntOrNull() = toIntOrNull()?.takeIf { it > 0 }
