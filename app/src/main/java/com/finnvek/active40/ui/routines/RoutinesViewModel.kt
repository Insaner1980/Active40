package com.finnvek.active40.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.active40.data.RoutineRepository
import com.finnvek.active40.data.db.Routine
import com.finnvek.active40.data.db.RoutineSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutinesViewModel(
    private val repository: RoutineRepository,
) : ViewModel() {
    val routines: StateFlow<List<RoutineSummary>> =
        repository.observeSummaries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Siirtää rutiinia listassa (delta -1 ylös, +1 alas). */
    fun move(
        index: Int,
        delta: Int,
    ) {
        val list = routines.value.map { it.routine }.toMutableList()
        val target = index + delta
        if (target !in list.indices) return
        list.add(target, list.removeAt(index))
        viewModelScope.launch { repository.reorder(list) }
    }

    fun delete(routine: Routine) {
        viewModelScope.launch { repository.delete(routine) }
    }
}
