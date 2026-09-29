package com.finnvek.active40.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnvek.active40.data.CheckInRepository
import com.finnvek.active40.data.RunRepository
import com.finnvek.active40.data.SettingsRepository
import com.finnvek.active40.data.StrengthRepository
import com.finnvek.active40.data.db.CheckInWithSymptoms
import com.finnvek.active40.data.db.Run
import com.finnvek.active40.data.db.SessionSummary
import com.finnvek.active40.domain.DistanceUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** Historian rivi. sortTime järjestää saman päivän kirjaukset. */
sealed interface HistoryEntry {
    val date: LocalDate
    val sortTime: Instant?

    data class Strength(
        val summary: SessionSummary,
    ) : HistoryEntry {
        override val date: LocalDate get() = summary.session.date
        override val sortTime: Instant get() = summary.session.startedAt
    }

    data class Running(
        val run: Run,
    ) : HistoryEntry {
        override val date: LocalDate get() = run.date
        override val sortTime: Instant get() = run.createdAt
    }

    data class Check(
        val item: CheckInWithSymptoms,
    ) : HistoryEntry {
        override val date: LocalDate get() = item.checkIn.date
        override val sortTime: Instant? get() = null
    }
}

data class DayFlags(
    val strength: Boolean = false,
    val run: Boolean = false,
    val checkIn: Boolean = false,
)

data class HistoryState(
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate? = null,
    val entries: List<HistoryEntry> = emptyList(),
    val flags: Map<LocalDate, DayFlags> = emptyMap(),
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val symptomTrackingEnabled: Boolean = false,
) {
    val visibleEntries: List<HistoryEntry> get() = selected?.let { day -> entries.filter { it.date == day } } ?: entries
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    initialDay: LocalDate?,
    strengthRepository: StrengthRepository,
    runRepository: RunRepository,
    checkInRepository: CheckInRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val month = MutableStateFlow(initialDay?.let(YearMonth::from) ?: YearMonth.now())
    private val selected = MutableStateFlow(initialDay)

    private val monthEntries =
        month.flatMapLatest { m ->
            val from = m.atDay(1)
            val to = m.atEndOfMonth()
            combine(
                strengthRepository.observeSummariesBetween(from, to),
                runRepository.observeBetween(from, to),
                checkInRepository.observeBetweenWithSymptoms(from, to),
            ) { sessions, runs, checkIns ->
                val entries =
                    sessions.map { HistoryEntry.Strength(it) } + runs.map { HistoryEntry.Running(it) } +
                        checkIns.map { HistoryEntry.Check(it) }
                // Uusin ensin; saman päivän harjoitukset aikajärjestyksessä, päivän kirjaus viimeisenä.
                entries.sortedWith(
                    compareByDescending<HistoryEntry> { it.date }.thenByDescending { it.sortTime ?: Instant.MIN },
                )
            }
        }

    val state: StateFlow<HistoryState> =
        combine(month, selected, monthEntries, settingsRepository.settings) { m, day, entries, settings ->
            HistoryState(
                month = m,
                selected = day,
                entries = entries,
                flags =
                    entries.groupBy { it.date }.mapValues { (_, list) ->
                        DayFlags(
                            strength = list.any { it is HistoryEntry.Strength },
                            run = list.any { it is HistoryEntry.Running },
                            checkIn = list.any { it is HistoryEntry.Check },
                        )
                    },
                distanceUnit = settings.distanceUnit,
                symptomTrackingEnabled = settings.symptomTrackingEnabled,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState(month = month.value, selected = initialDay))

    fun changeMonth(delta: Long) {
        month.value = month.value.plusMonths(delta)
        selected.value = null
    }

    /** Päivän napautus valitsee päivän; uudelleen napautus palaa koko kuukauden näkymään. */
    fun toggleDay(day: LocalDate) {
        selected.value = if (selected.value == day) null else day
    }

    fun showWholeMonth() {
        selected.value = null
    }
}
