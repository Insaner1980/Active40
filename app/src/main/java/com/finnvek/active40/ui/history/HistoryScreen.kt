package com.finnvek.active40.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.active40.R
import com.finnvek.active40.domain.durationSeconds
import com.finnvek.active40.domain.monthGrid
import com.finnvek.active40.ui.appViewModel
import com.finnvek.active40.ui.checkInSummaryLines
import com.finnvek.active40.ui.components.CheckInMarker
import com.finnvek.active40.ui.components.GlassCard
import com.finnvek.active40.ui.components.GlassListRow
import com.finnvek.active40.ui.components.LightCard
import com.finnvek.active40.ui.components.PlainIconButton
import com.finnvek.active40.ui.components.RunMarker
import com.finnvek.active40.ui.components.ScreenTitle
import com.finnvek.active40.ui.components.StrengthMarker
import com.finnvek.active40.ui.formatDayLong
import com.finnvek.active40.ui.label
import com.finnvek.active40.ui.runNumbers
import com.finnvek.active40.ui.sessionNumbers
import com.finnvek.active40.ui.sessionTitle
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Spacing
import com.finnvek.active40.ui.theme.NumberFont
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private val cellShape = RoundedCornerShape(14.dp)
private const val MARKER_SIZE_DP = 5

@Composable
fun HistoryScreen(
    initialDay: LocalDate?,
    onOpenSession: (Long) -> Unit,
    onOpenRun: (Long) -> Unit,
    onOpenCheckIn: (LocalDate) -> Unit,
) {
    val viewModel =
        appViewModel {
            HistoryViewModel(initialDay, it.strengthRepository, it.runRepository, it.checkInRepository, it.settingsRepository)
        }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val monthLabel = state.month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Active40Spacing.screenHorizontal, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle(stringResource(R.string.history_title))
        CalendarCard(state, monthLabel, viewModel::changeMonth, viewModel::toggleDay)

        val selected = state.selected
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (selected != null) formatDayLong(selected) else stringResource(R.string.history_all_month, monthLabel),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (selected != null) {
                TextButton(onClick = viewModel::showWholeMonth, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.history_show_month), color = Active40Colors.onBackground)
                }
            }
        }

        val entries = state.visibleEntries
        if (entries.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(if (selected != null) R.string.history_empty_day else R.string.history_empty_month),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        entries.forEach { entry ->
            val dateLine = if (selected == null) formatDayLong(entry.date) + "\n" else ""
            when (entry) {
                is HistoryEntry.Strength -> {
                    val s = entry.summary
                    GlassListRow(
                        title = sessionTitle(s.session, s.routineName),
                        supportingText = dateLine + sessionNumbers(s.completedSets, s.session.durationSeconds()),
                        onClick = { onOpenSession(s.session.id) },
                    )
                }

                is HistoryEntry.Running -> {
                    GlassListRow(
                        title = stringResource(R.string.run_title, entry.run.runType.label()),
                        supportingText = dateLine + runNumbers(entry.run, state.distanceUnit),
                        onClick = { onOpenRun(entry.run.id) },
                    )
                }

                is HistoryEntry.Check -> {
                    GlassListRow(
                        title = stringResource(R.string.history_checkin_title),
                        supportingText = dateLine + checkInSummaryLines(entry.item, state.symptomTrackingEnabled).joinToString(", "),
                        onClick = { onOpenCheckIn(entry.date) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarCard(
    state: HistoryState,
    monthLabel: String,
    onChangeMonth: (Long) -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val today = LocalDate.now()
    LightCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlainIconButton(R.drawable.ic_back, stringResource(R.string.history_previous_month), onClick = { onChangeMonth(-1) })
                Text(
                    monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                PlainIconButton(
                    R.drawable.ic_forward,
                    stringResource(R.string.history_next_month),
                    onClick = { onChangeMonth(1) },
                    enabled = state.month < YearMonth.from(today),
                )
            }
            Row {
                (0L until 7L).map { firstDay.plus(it) }.forEach { dow ->
                    Text(
                        dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = Active40Colors.onSurfaceMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            monthGrid(state.month, firstDay).chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    week.forEach { day ->
                        if (day == null) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            DayCell(
                                day = day,
                                flags = state.flags[day] ?: DayFlags(),
                                isSelected = day == state.selected,
                                isToday = day == today,
                                modifier = Modifier.weight(1f),
                                onClick = { onDayClick(day) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    flags: DayFlags,
    isSelected: Boolean,
    isToday: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val description =
        listOfNotNull(
            formatDayLong(day),
            stringResource(R.string.today_marker).takeIf { isToday },
            stringResource(R.string.marker_strength).takeIf { flags.strength },
            stringResource(R.string.marker_run).takeIf { flags.run },
            stringResource(R.string.marker_checkin).takeIf { flags.checkIn },
        ).joinToString(", ")
    val onDark = isSelected
    Surface(
        onClick = onClick,
        shape = cellShape,
        color = if (isSelected) Active40Colors.inkDark else Color.Transparent,
        contentColor = if (isSelected) Active40Colors.onBackground else Active40Colors.onSurface,
        border = if (isToday && !isSelected) BorderStroke(1.dp, Active40Colors.onSurface) else null,
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .semantics {
                    contentDescription = description
                    this.selected = isSelected
                },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
            Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium, fontFamily = NumberFont)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.heightIn(min = MARKER_SIZE_DP.dp)) {
                val size = MARKER_SIZE_DP.dp
                if (flags.strength) {
                    StrengthMarker(if (onDark) Active40Colors.accentStrength else Active40Colors.accentStrengthOnLight, size = size)
                }
                if (flags.run) RunMarker(if (onDark) Active40Colors.accentRun else Active40Colors.accentRunOnLight, size = size)
                if (flags.checkIn) CheckInMarker(if (onDark) Active40Colors.onBackground else Active40Colors.onSurfaceMuted, size = size)
            }
        }
    }
}
