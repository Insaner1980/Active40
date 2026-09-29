package com.finnvek.active40.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.active40.data.RunRepository
import com.finnvek.active40.data.SettingsRepository
import com.finnvek.active40.data.StrengthRepository
import com.finnvek.active40.data.db.Exercise
import com.finnvek.active40.data.db.Run
import com.finnvek.active40.domain.DistanceUnit
import com.finnvek.active40.domain.ProgressFilter
import com.finnvek.active40.domain.ProgressRange
import com.finnvek.active40.domain.RunType
import com.finnvek.active40.domain.SessionValue
import com.finnvek.active40.domain.WeekValue
import com.finnvek.active40.domain.WeightUnit
import com.finnvek.active40.domain.consecutiveActiveWeeks
import com.finnvek.active40.domain.heaviestPerSession
import com.finnvek.active40.domain.startDate
import com.finnvek.active40.domain.strengthShareByTime
import com.finnvek.active40.domain.toActivity
import com.finnvek.active40.domain.weeklyAveragePace
import com.finnvek.active40.domain.weeklyRunDistance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

data class ProgressState(
    val isLoading: Boolean = true,
    val filter: ProgressFilter = ProgressFilter.ALL,
    val range: ProgressRange = ProgressRange.WEEKS_8,
    val exercises: List<Exercise> = emptyList(),
    val selectedExercise: Exercise? = null,
    val strengthSeries: List<SessionValue> = emptyList(),
    val runs: List<Run> = emptyList(),
    val runTypeFilter: RunType? = null,
    val weeklyDistance: List<WeekValue> = emptyList(),
    val weeklyPace: List<WeekValue> = emptyList(),
    val strengthShare: Double? = null,
    val consistencyWeeks: Int = 0,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
)

private data class Controls(
    val filter: ProgressFilter,
    val range: ProgressRange,
    val exerciseId: Long?,
    val runType: RunType?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    private val strengthRepository: StrengthRepository,
    private val runRepository: RunRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val today = LocalDate.now()
    private val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    private val filter = MutableStateFlow(ProgressFilter.ALL)
    private val range = MutableStateFlow(ProgressRange.WEEKS_8)
    private val exerciseId = MutableStateFlow<Long?>(null)
    private val runType = MutableStateFlow<RunType?>(null)

    private val controls = combine(filter, range, exerciseId, runType, ::Controls)

    private val consistency: Flow<Int> =
        combine(strengthRepository.observeSessionDates(), runRepository.observeDates()) { a, b ->
            consecutiveActiveWeeks(a + b, today, firstDayOfWeek)
        }

    val state: StateFlow<ProgressState> =
        combine(controls, settingsRepository.settings, consistency) { c, settings, weeks -> Triple(c, settings, weeks) }
            .flatMapLatest { (c, settings, weeks) ->
                val from = c.range.startDate(today, firstDayOfWeek)
                combine(
                    strengthRepository.observeSummariesBetween(from, today),
                    runRepository.observeBetween(from, today),
                    strengthRepository.observeExercisesWithData(from, today).flatMapLatest { exercises ->
                        // Valittu liike säilyy, jos sillä on dataa aikavälillä; muuten ensimmäinen liike.
                        val selected = exercises.firstOrNull { it.id == c.exerciseId } ?: exercises.firstOrNull()
                        if (selected == null) {
                            flowOf(Triple(exercises, null, emptyList()))
                        } else {
                            strengthRepository.observeExerciseSets(selected.id, from, today).map {
                                Triple(exercises, selected, heaviestPerSession(it))
                            }
                        }
                    },
                ) { sessions, runs, (exercises, selected, series) ->
                    val runActivities = runs.map { it.toActivity() }
                    val paceRuns = runs.filter { c.runType == null || it.runType == c.runType }.map { it.toActivity() }
                    ProgressState(
                        isLoading = false,
                        filter = c.filter,
                        range = c.range,
                        exercises = exercises,
                        selectedExercise = selected,
                        strengthSeries = series,
                        runs = runs,
                        runTypeFilter = c.runType,
                        weeklyDistance = weeklyRunDistance(runActivities, from, today, firstDayOfWeek),
                        weeklyPace = weeklyAveragePace(paceRuns, from, today, firstDayOfWeek, settings.distanceUnit),
                        strengthShare = strengthShareByTime(sessions.map { it.session.toActivity() } + runActivities),
                        consistencyWeeks = weeks,
                        weightUnit = settings.weightUnit,
                        distanceUnit = settings.distanceUnit,
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    fun setFilter(value: ProgressFilter) {
        filter.value = value
    }

    fun setRange(value: ProgressRange) {
        range.value = value
    }

    fun selectExercise(id: Long) {
        exerciseId.value = id
    }

    fun setRunType(value: RunType?) {
        runType.value = value
    }
}
