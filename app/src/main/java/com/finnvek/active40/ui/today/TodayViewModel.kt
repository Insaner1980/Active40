package com.finnvek.active40.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.active40.data.CheckInRepository
import com.finnvek.active40.data.RoutineRepository
import com.finnvek.active40.data.RunRepository
import com.finnvek.active40.data.SettingsRepository
import com.finnvek.active40.data.StrengthRepository
import com.finnvek.active40.data.db.CheckIn
import com.finnvek.active40.data.db.RoutineSummary
import com.finnvek.active40.data.db.Run
import com.finnvek.active40.data.db.SessionSummary
import com.finnvek.active40.data.db.StrengthSession
import com.finnvek.active40.domain.ActivityKind
import com.finnvek.active40.domain.DistanceUnit
import com.finnvek.active40.domain.WeekTotals
import com.finnvek.active40.domain.toActivity
import com.finnvek.active40.domain.totals
import com.finnvek.active40.domain.weekDays
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

data class DayMarkers(
    val date: LocalDate,
    val hasStrength: Boolean,
    val hasRun: Boolean,
)

/** Viimeisin kirjaus: joko voimaharjoitus tai juoksu. */
sealed interface LastActivity {
    data class Strength(
        val summary: SessionSummary,
    ) : LastActivity

    data class Running(
        val run: Run,
    ) : LastActivity
}

data class TodayState(
    val today: LocalDate = LocalDate.now(),
    val checkIn: CheckIn? = null,
    val days: List<DayMarkers> = emptyList(),
    val totals: WeekTotals = WeekTotals(0, 0, 0.0, 0),
    val lastActivity: LastActivity? = null,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
)

class TodayViewModel(
    private val strengthRepository: StrengthRepository,
    routineRepository: RoutineRepository,
    runRepository: RunRepository,
    checkInRepository: CheckInRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val today = LocalDate.now()

    // Viikon ensimmäinen päivä tulee laitteen kieliasetuksesta.
    private val week = weekDays(today, WeekFields.of(Locale.getDefault()).firstDayOfWeek)

    val activeSession: StateFlow<StrengthSession?> =
        strengthRepository.observeActiveSession().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val routines: StateFlow<List<RoutineSummary>> =
        routineRepository.observeSummaries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val weekActivities =
        combine(
            strengthRepository.observeSummariesBetween(week.first(), week.last()),
            runRepository.observeBetween(week.first(), week.last()),
        ) { sessions, runs -> sessions.map { it.session.toActivity() } + runs.map { it.toActivity() } }

    private val lastActivity =
        combine(strengthRepository.observeLatestSummary(), runRepository.observeLatest()) { session, run ->
            when {
                session == null -> run?.let { LastActivity.Running(it) }

                run == null -> LastActivity.Strength(session)

                // Uudempi päivä voittaa; samana päivänä verrataan aloitus- ja kirjaushetkeä.
                run.date.isAfter(session.session.date) -> LastActivity.Running(run)

                session.session.date.isAfter(run.date) -> LastActivity.Strength(session)

                run.createdAt.isAfter(session.session.startedAt) -> LastActivity.Running(run)

                else -> LastActivity.Strength(session)
            }
        }

    val state: StateFlow<TodayState> =
        combine(
            weekActivities,
            lastActivity,
            checkInRepository.observeWithSymptomsByDate(today).map { it?.checkIn },
            settingsRepository.settings,
        ) { activities, last, checkIn, settings ->
            TodayState(
                today = today,
                checkIn = checkIn,
                days =
                    week.map { day ->
                        DayMarkers(
                            date = day,
                            hasStrength = activities.any { it.date == day && it.kind == ActivityKind.STRENGTH },
                            hasRun = activities.any { it.date == day && it.kind == ActivityKind.RUN },
                        )
                    },
                totals = totals(activities),
                lastActivity = last,
                distanceUnit = settings.distanceUnit,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayState())

    /** Aloittaa harjoituksen (tai palauttaa jo käynnissä olevan) ja kertoo sen id:n. */
    fun startSession(
        routineId: Long?,
        onStarted: (Long) -> Unit,
    ) {
        viewModelScope.launch { onStarted(strengthRepository.startSession(routineId)) }
    }
}
