package com.finnvek.active40.ui.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.active40.data.RunRepository
import com.finnvek.active40.data.SettingsRepository
import com.finnvek.active40.data.db.Run
import com.finnvek.active40.domain.DistanceUnit
import com.finnvek.active40.domain.RunInputError
import com.finnvek.active40.domain.RunType
import com.finnvek.active40.domain.formatDecimal
import com.finnvek.active40.domain.fromMeters
import com.finnvek.active40.domain.paceSecondsPerUnit
import com.finnvek.active40.domain.parseDecimal
import com.finnvek.active40.domain.parseDurationSeconds
import com.finnvek.active40.domain.toMeters
import com.finnvek.active40.domain.validateRun
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class RunFormState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    val distanceText: String = "",
    val hours: String = "",
    val minutes: String = "",
    val seconds: String = "",
    val runType: RunType = RunType.EASY,
    val effort: Int? = null,
    val note: String = "",
    val unit: DistanceUnit = DistanceUnit.KM,
    val isClosed: Boolean = false,
) {
    val errors: Set<RunInputError> get() = validateRun(distanceText, hours, minutes, seconds)

    /** Vauhti lasketaan lennossa; null, kun matka tai kesto puuttuu. */
    val paceSeconds: Double?
        get() {
            val distance = parseDecimal(distanceText) ?: return null
            val duration = parseDurationSeconds(hours, minutes, seconds) ?: return null
            return paceSecondsPerUnit(unit.toMeters(distance), duration, unit)
        }
}

const val NEW_RUN_ID = 0L
private const val MAX_DISTANCE_CHARS = 7
private const val MAX_TIME_PART_CHARS = 2
private const val MAX_HOURS_CHARS = 2

class RunLogViewModel(
    private val runId: Long,
    private val runRepository: RunRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RunFormState(isNew = runId == NEW_RUN_ID))
    val state: StateFlow<RunFormState> = _state.asStateFlow()

    private var existing: Run? = null

    // Muokkauksessa matka näytetään pyöristettynä; jos kenttää ei muuteta, alkuperäiset metrit säilyvät.
    private var initialDistanceText: String? = null

    init {
        viewModelScope.launch {
            val unit = settingsRepository.settings.first().distanceUnit
            val run = if (runId == NEW_RUN_ID) null else runRepository.get(runId)
            existing = run
            initialDistanceText = run?.let { r -> formatDecimal(unit.fromMeters(r.distanceMeters)) }
            _state.update {
                if (run == null) {
                    it.copy(isLoading = false, unit = unit)
                } else {
                    val total = run.durationSeconds
                    it.copy(
                        isLoading = false,
                        unit = unit,
                        date = run.date,
                        distanceText = initialDistanceText.orEmpty(),
                        hours = (total / 3600).takeIf { h -> h > 0 }?.toString().orEmpty(),
                        minutes = ((total % 3600) / 60).toString(),
                        seconds = (total % 60).toString(),
                        runType = run.runType,
                        effort = run.effort,
                        note = run.note.orEmpty(),
                    )
                }
            }
        }
    }

    fun setDate(date: LocalDate) = _state.update { it.copy(date = date) }

    fun setDistance(text: String) =
        _state.update { it.copy(distanceText = text.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(MAX_DISTANCE_CHARS)) }

    fun setHours(text: String) = _state.update { it.copy(hours = text.filter(Char::isDigit).take(MAX_HOURS_CHARS)) }

    fun setMinutes(text: String) = _state.update { it.copy(minutes = text.filter(Char::isDigit).take(MAX_TIME_PART_CHARS)) }

    fun setSeconds(text: String) = _state.update { it.copy(seconds = text.filter(Char::isDigit).take(MAX_TIME_PART_CHARS)) }

    fun setRunType(type: RunType) = _state.update { it.copy(runType = type) }

    fun setEffort(effort: Int?) = _state.update { it.copy(effort = effort) }

    fun setNote(note: String) = _state.update { it.copy(note = note) }

    fun save() {
        val s = _state.value
        if (s.errors.isNotEmpty()) return
        val distance = parseDecimal(s.distanceText) ?: return
        val duration = parseDurationSeconds(s.hours, s.minutes, s.seconds) ?: return
        val run =
            Run(
                id = existing?.id ?: 0,
                date = s.date,
                distanceMeters =
                    existing?.takeIf { s.distanceText == initialDistanceText }?.distanceMeters ?: s.unit.toMeters(distance),
                durationSeconds = duration,
                runType = s.runType,
                effort = s.effort,
                note = s.note.trim().ifEmpty { null },
                createdAt = existing?.createdAt ?: Instant.now(),
            )
        viewModelScope.launch {
            runRepository.save(run)
            _state.update { it.copy(isClosed = true) }
        }
    }

    fun delete() {
        val run = existing ?: return
        viewModelScope.launch {
            runRepository.delete(run)
            _state.update { it.copy(isClosed = true) }
        }
    }
}
