package com.finnvek.active40.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.active40.R
import com.finnvek.active40.domain.ProgressFilter
import com.finnvek.active40.domain.ProgressRange
import com.finnvek.active40.domain.RunType
import com.finnvek.active40.domain.formatDistance
import com.finnvek.active40.domain.formatPace
import com.finnvek.active40.domain.formatWeight
import com.finnvek.active40.domain.hasTrend
import com.finnvek.active40.domain.percentageChange
import com.finnvek.active40.ui.appViewModel
import com.finnvek.active40.ui.components.AdaptiveRow
import com.finnvek.active40.ui.components.BarChart
import com.finnvek.active40.ui.components.GlassButton
import com.finnvek.active40.ui.components.GlassCard
import com.finnvek.active40.ui.components.LightCard
import com.finnvek.active40.ui.components.LineChart
import com.finnvek.active40.ui.components.RunMarker
import com.finnvek.active40.ui.components.ScreenTitle
import com.finnvek.active40.ui.components.SelectableChip
import com.finnvek.active40.ui.components.SplitBar
import com.finnvek.active40.ui.components.StrengthMarker
import com.finnvek.active40.ui.components.chipColorsOnBackground
import com.finnvek.active40.ui.components.isLargeFontScale
import com.finnvek.active40.ui.label
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Spacing
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PERCENT = 100
private const val GLASS_BAR_ALPHA = 0.22f

@Composable
private fun ProgressRange.label(): String =
    stringResource(
        when (this) {
            ProgressRange.WEEKS_4 -> R.string.range_4_weeks
            ProgressRange.WEEKS_8 -> R.string.range_8_weeks
            ProgressRange.MONTHS_6 -> R.string.range_6_months
            ProgressRange.YEAR_1 -> R.string.range_1_year
        },
    )

@Composable
private fun ProgressFilter.label(): String =
    stringResource(
        when (this) {
            ProgressFilter.ALL -> R.string.filter_all
            ProgressFilter.STRENGTH -> R.string.filter_strength
            ProgressFilter.RUNNING -> R.string.filter_running
        },
    )

@Composable
fun ProgressScreen() {
    val viewModel = appViewModel { ProgressViewModel(it.strengthRepository, it.runRepository, it.settingsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Active40Spacing.screenHorizontal, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScreenTitle(stringResource(R.string.progress_title), Modifier.weight(1f))
            RangeMenu(state.range, viewModel::setRange)
        }
        AdaptiveRow(spacing = 6.dp) { itemModifier ->
            ProgressFilter.entries.forEach { f ->
                SelectableChip(
                    text = f.label(),
                    selected = state.filter == f,
                    onClick = { viewModel.setFilter(f) },
                    colors = chipColorsOnBackground,
                    modifier = itemModifier,
                )
            }
        }
        if (state.isLoading) return@Column

        if (state.filter != ProgressFilter.RUNNING) StrengthCard(state, viewModel::selectExercise)
        if (state.filter != ProgressFilter.STRENGTH) {
            DistanceCard(state)
            PaceCard(state, viewModel::setRunType)
        }
        if (state.filter == ProgressFilter.ALL) {
            if (isLargeFontScale()) {
                BalanceCard(state.strengthShare, Modifier.fillMaxWidth())
                ConsistencyCard(state.consistencyWeeks, Modifier.fillMaxWidth())
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                    BalanceCard(state.strengthShare, Modifier.weight(1f).fillMaxHeight())
                    ConsistencyCard(state.consistencyWeeks, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun RangeMenu(
    range: ProgressRange,
    onSelect: (ProgressRange) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = range.label()
    val description = stringResource(R.string.range_menu, current)
    Box {
        GlassButton(
            text = current,
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = description },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ProgressRange.entries.forEach { r ->
                DropdownMenuItem(text = { Text(r.label()) }, onClick = {
                    expanded = false
                    onSelect(r)
                })
            }
        }
    }
}

@Composable
private fun CardHeader(
    title: String,
    marker: @Composable () -> Unit,
    trailing: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        marker()
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        trailing?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Active40Colors.onGlassSecondary) }
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = Active40Colors.onGlassSecondary)
}

@Composable
private fun StrengthCard(
    state: ProgressState,
    onSelectExercise: (Long) -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val selected = state.selectedExercise
            if (selected == null) {
                CardHeader(stringResource(R.string.filter_strength), { StrengthMarker(Active40Colors.accentStrength, size = 8.dp) })
                EmptyText(stringResource(R.string.progress_no_strength))
                return@Column
            }
            ExerciseMenu(state, onSelectExercise)
            Text(
                stringResource(R.string.progress_heaviest),
                style = MaterialTheme.typography.bodySmall,
                color = Active40Colors.onGlassSecondary,
            )
            val series = state.strengthSeries
            if (!hasTrend(series.map { it.value })) {
                EmptyText(stringResource(R.string.progress_need_two_sessions))
                return@Column
            }
            val first = series.first().value
            val last = series.last().value
            val unitLabel = state.weightUnit.label()
            Text(
                stringResource(
                    R.string.progress_from_to,
                    formatWeight(first, state.weightUnit),
                    formatWeight(last, state.weightUnit),
                    unitLabel,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Active40Colors.onGlassSecondary,
            )
            percentageChange(first, last)?.let { change ->
                val weightDelta = formatWeight(abs(last - first), state.weightUnit)
                val sign = if (last >= first) "+" else "-"
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.progress_percent, signed(change.roundToInt())),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Text(
                        "$sign$weightDelta $unitLabel",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            LineChart(
                values = series.map { it.value },
                color = Active40Colors.accentStrength,
                description = stringResource(R.string.chart_strength_description, series.size),
            )
        }
    }
}

private fun signed(value: Int) = if (value > 0) "+$value" else value.toString()

@Composable
private fun ExerciseMenu(
    state: ProgressState,
    onSelect: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val name = state.selectedExercise?.name.orEmpty()
    val description = stringResource(R.string.progress_exercise_menu, name)
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = description },
        ) {
            StrengthMarker(Active40Colors.accentStrength, size = 8.dp)
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                color = Active40Colors.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Icon(
                painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = Active40Colors.onBackground,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.exercises.forEach { exercise ->
                DropdownMenuItem(text = { Text(exercise.name) }, onClick = {
                    expanded = false
                    onSelect(exercise.id)
                })
            }
        }
    }
}

@Composable
private fun DistanceCard(state: ProgressState) {
    val unitLabel = state.distanceUnit.label()
    val totalMeters = state.runs.sumOf { it.distanceMeters }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CardHeader(stringResource(R.string.progress_weekly_distance), { RunMarker(Active40Colors.accentRun, size = 8.dp) })
            if (state.runs.isEmpty()) {
                EmptyText(stringResource(R.string.progress_no_runs))
                return@Column
            }
            Text(
                stringResource(
                    R.string.progress_distance_total,
                    formatDistance(totalMeters, state.distanceUnit, maxDecimals = 1),
                    unitLabel,
                ) +
                    ", " + pluralStringResource(R.plurals.run_count, state.runs.size, state.runs.size),
                style = MaterialTheme.typography.bodyLarge,
            )
            val weeks = state.weeklyDistance
            if (!hasTrend(weeks.map { it.value?.takeIf { v -> v > 0 } })) {
                EmptyText(stringResource(R.string.progress_need_two_weeks))
                return@Column
            }
            BarChart(
                values = weeks.map { it.value ?: 0.0 },
                barColor = Active40Colors.onBackground.copy(alpha = GLASS_BAR_ALPHA),
                highlightColor = Active40Colors.accentRun,
                description = stringResource(R.string.chart_distance_description, weeks.size),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaceCard(
    state: ProgressState,
    onRunType: (RunType?) -> Unit,
) {
    val unitLabel = state.distanceUnit.label()
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CardHeader(stringResource(R.string.progress_avg_pace), { RunMarker(Active40Colors.accentRun, size = 8.dp) })
            if (state.runs.isEmpty()) {
                EmptyText(stringResource(R.string.progress_no_runs))
                return@Column
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SelectableChip(
                    text = stringResource(R.string.progress_all_run_types),
                    selected = state.runTypeFilter == null,
                    onClick = { onRunType(null) },
                    colors = chipColorsOnBackground,
                )
                RunType.entries.forEach { type ->
                    SelectableChip(
                        text = type.label(),
                        selected = state.runTypeFilter == type,
                        onClick = { onRunType(type) },
                        colors = chipColorsOnBackground,
                    )
                }
            }
            val paces = state.weeklyPace.map { it.value }
            val present = paces.filterNotNull()
            if (!hasTrend(paces)) {
                EmptyText(stringResource(R.string.progress_need_two_weeks))
                return@Column
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(formatPace(present.last()), style = MaterialTheme.typography.displaySmall)
                Text("/$unitLabel", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 6.dp))
                Text(
                    stringResource(R.string.progress_pace_was, formatPace(present.first()), unitLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = Active40Colors.onGlassSecondary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            // Pienempi vauhti on parempi, joten akseli käännetään: nousu tarkoittaa nopeampaa.
            LineChart(
                values = paces,
                color = Active40Colors.accentRun,
                invert = true,
                description = stringResource(R.string.chart_pace_description, present.size),
            )
        }
    }
}

@Composable
private fun BalanceCard(
    strengthShare: Double?,
    modifier: Modifier = Modifier,
) {
    LightCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.balance_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.balance_by_time),
                style = MaterialTheme.typography.bodySmall,
                color = Active40Colors.onSurfaceMuted,
            )
            if (strengthShare == null) {
                Text(stringResource(R.string.balance_empty), style = MaterialTheme.typography.bodySmall)
                return@Column
            }
            val strengthPct = (strengthShare * PERCENT).roundToInt()
            val runPct = PERCENT - strengthPct
            SplitBar(
                firstShare = strengthShare,
                firstColor = Active40Colors.accentStrengthOnLight,
                secondColor = Active40Colors.accentRunOnLight,
                description = stringResource(R.string.chart_balance_description, strengthPct, runPct),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StrengthMarker(Active40Colors.accentStrengthOnLight, size = 8.dp)
                Text(stringResource(R.string.balance_strength, strengthPct), style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RunMarker(Active40Colors.accentRunOnLight, size = 8.dp)
                Text(stringResource(R.string.balance_running, runPct), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ConsistencyCard(
    weeks: Int,
    modifier: Modifier = Modifier,
) {
    LightCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.consistency_title), style = MaterialTheme.typography.titleSmall)
            Text(pluralStringResource(R.plurals.consistency_weeks, weeks, weeks), style = MaterialTheme.typography.displaySmall)
            Text(
                stringResource(R.string.consistency_detail),
                style = MaterialTheme.typography.bodySmall,
                color = Active40Colors.onSurfaceMuted,
            )
        }
    }
}
