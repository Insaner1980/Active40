package com.finnvek.active40.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.active40.data.CheckInRepository
import com.finnvek.active40.data.SettingsRepository
import com.finnvek.active40.data.SymptomRepository
import com.finnvek.active40.data.db.CheckIn
import com.finnvek.active40.data.db.Symptom
import com.finnvek.active40.domain.SorenessArea
import com.finnvek.active40.domain.toggleSoreness
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CheckInForm(
    val isLoading: Boolean = true,
    val existing: CheckIn? = null,
    val sleep: Int? = null,
    val energy: Int? = null,
    val soreness: Set<SorenessArea> = emptySet(),
    val symptomIds: Set<Long> = emptySet(),
    val note: String = "",
    val isClosed: Boolean = false,
) {
    /** Vähintään yksi vastaus riittää tallennukseen. */
    val hasAnswer: Boolean
        get() = sleep != null || energy != null || soreness.isNotEmpty() || symptomIds.isNotEmpty() || note.isNotBlank()
}

data class CheckInUiState(
    val form: CheckInForm,
    val symptomTrackingEnabled: Boolean,
    /** Näytettävät oireet: aktiiviset sekä arkistoidut, jotka on merkitty tähän kirjaukseen. */
    val symptoms: List<Symptom>,
)

class CheckInViewModel(
    val date: LocalDate,
    private val checkInRepository: CheckInRepository,
    private val symptomRepository: SymptomRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val form = MutableStateFlow(CheckInForm())

    val state: StateFlow<CheckInUiState> =
        combine(form, settingsRepository.settings, symptomRepository.observeAll()) { f, settings, symptoms ->
            CheckInUiState(
                form = f,
                symptomTrackingEnabled = settings.symptomTrackingEnabled,
                symptoms = symptoms.filter { !it.isArchived || it.id in f.symptomIds },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckInUiState(CheckInForm(), false, emptyList()))

    init {
        viewModelScope.launch {
            val existing = checkInRepository.getByDate(date)
            form.value =
                if (existing == null) {
                    CheckInForm(isLoading = false)
                } else {
                    CheckInForm(
                        isLoading = false,
                        existing = existing,
                        sleep = existing.sleep,
                        energy = existing.energy,
                        soreness = existing.soreness.orEmpty(),
                        symptomIds = checkInRepository.symptomIdsFor(existing),
                        note = existing.note.orEmpty(),
                    )
                }
        }
    }

    fun setSleep(value: Int?) = form.update { it.copy(sleep = value) }

    fun setEnergy(value: Int?) = form.update { it.copy(energy = value) }

    fun toggleSoreness(area: SorenessArea) = form.update { it.copy(soreness = toggleSoreness(it.soreness, area)) }

    fun toggleSymptom(id: Long) = form.update { it.copy(symptomIds = if (id in it.symptomIds) it.symptomIds - id else it.symptomIds + id) }

    fun setNote(note: String) = form.update { it.copy(note = note) }

    /** Lisää oman oireen ja valitsee sen. */
    fun addCustomSymptom(name: String) {
        viewModelScope.launch {
            val id = symptomRepository.add(name)
            form.update { it.copy(symptomIds = it.symptomIds + id) }
        }
    }

    fun save() {
        val f = form.value
        if (!f.hasAnswer) return
        val tracking = state.value.symptomTrackingEnabled
        viewModelScope.launch {
            checkInRepository.save(
                CheckIn(
                    date = date,
                    sleep = f.sleep,
                    energy = f.energy,
                    soreness = f.soreness.takeIf { it.isNotEmpty() },
                    note = f.note.trim().ifEmpty { null },
                ),
                // Kun seuranta on pois päältä, oireisiin ei kosketa, jotta piilotettu data säilyy.
                symptomIds = if (tracking) f.symptomIds else null,
            )
            form.update { it.copy(isClosed = true) }
        }
    }

    fun delete() {
        val existing = form.value.existing ?: return
        viewModelScope.launch {
            checkInRepository.delete(existing)
            form.update { it.copy(isClosed = true) }
        }
    }
}
