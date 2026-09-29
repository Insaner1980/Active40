package com.finnvek.active40.ui.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.active40.R
import com.finnvek.active40.domain.RunInputError
import com.finnvek.active40.domain.RunType
import com.finnvek.active40.domain.formatPace
import com.finnvek.active40.ui.appViewModel
import com.finnvek.active40.ui.components.Active40TextField
import com.finnvek.active40.ui.components.ConfirmDialog
import com.finnvek.active40.ui.components.DateField
import com.finnvek.active40.ui.components.FullScreen
import com.finnvek.active40.ui.components.MenuAction
import com.finnvek.active40.ui.components.OverflowMenu
import com.finnvek.active40.ui.components.PillButton
import com.finnvek.active40.ui.components.ScaleSelector
import com.finnvek.active40.ui.components.SectionTitle
import com.finnvek.active40.ui.components.SelectableChip
import com.finnvek.active40.ui.components.chipColorsOnBackground
import com.finnvek.active40.ui.label
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.NumberFont

private val effortRange = 1..10
private const val EFFORT_COLUMNS = 5

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RunLogScreen(
    runId: Long,
    onClose: () -> Unit,
) {
    val viewModel = appViewModel { RunLogViewModel(runId, it.runRepository, it.settingsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.isClosed) { if (state.isClosed) onClose() }

    FullScreen(
        title = stringResource(if (state.isNew) R.string.run_log_title else R.string.run_edit_title),
        onBack = onClose,
        actions = {
            if (!state.isNew) {
                OverflowMenu(
                    contentDescription = stringResource(R.string.run_options),
                    actions = listOf(MenuAction(stringResource(R.string.action_delete)) { confirmDelete = true }),
                )
            }
        },
        bottomBar = {
            PillButton(
                text = stringResource(R.string.action_save),
                onClick = viewModel::save,
                enabled = state.errors.isEmpty(),
                containerColor = Active40Colors.accentRun,
                contentColor = Active40Colors.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        if (state.isLoading) return@FullScreen
        val unitLabel = state.unit.label()
        DateField(label = stringResource(R.string.date_label), date = state.date, onDateChange = viewModel::setDate)

        Active40TextField(
            value = state.distanceText,
            onValueChange = viewModel::setDistance,
            label = stringResource(R.string.run_distance_label, unitLabel),
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.distanceText.isNotEmpty() && RunInputError.DISTANCE in state.errors) {
            ErrorText(stringResource(R.string.run_error_distance))
        }

        SectionTitle(stringResource(R.string.run_duration_label))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DurationField(state.hours, viewModel::setHours, R.string.run_hours, Modifier.weight(1f))
            DurationField(state.minutes, viewModel::setMinutes, R.string.run_minutes, Modifier.weight(1f))
            DurationField(state.seconds, viewModel::setSeconds, R.string.run_seconds, Modifier.weight(1f), ImeAction.Done)
        }
        val durationTouched = state.hours.isNotEmpty() || state.minutes.isNotEmpty() || state.seconds.isNotEmpty()
        when {
            RunInputError.TIME_PARTS in state.errors -> ErrorText(stringResource(R.string.run_error_time_parts))
            durationTouched && RunInputError.DURATION in state.errors -> ErrorText(stringResource(R.string.run_error_duration))
        }

        val pace = state.paceSeconds
        if (pace != null) {
            Text(
                stringResource(R.string.run_pace, formatPace(pace), unitLabel),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = NumberFont,
            )
        } else {
            Text(
                stringResource(R.string.run_pace_empty),
                style = MaterialTheme.typography.bodySmall,
                color = Active40Colors.onBackgroundMuted,
            )
        }

        SectionTitle(stringResource(R.string.run_type_label))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RunType.entries.forEach { type ->
                SelectableChip(
                    text = type.label(),
                    selected = state.runType == type,
                    onClick = { viewModel.setRunType(type) },
                    colors = chipColorsOnBackground,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle(stringResource(R.string.run_effort_label))
            Text(
                stringResource(R.string.effort_optional),
                style = MaterialTheme.typography.bodySmall,
                color = Active40Colors.onBackgroundMuted,
            )
        }
        ScaleSelector(
            range = effortRange,
            selected = state.effort,
            onSelect = viewModel::setEffort,
            colors = chipColorsOnBackground,
            columns = EFFORT_COLUMNS,
        )

        Active40TextField(
            value = state.note,
            onValueChange = viewModel::setNote,
            label = stringResource(R.string.session_note_label),
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.run_delete_title),
            message = stringResource(R.string.run_delete_message),
            confirmText = stringResource(R.string.action_delete),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun DurationField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    modifier: Modifier,
    imeAction: ImeAction = ImeAction.Next,
) {
    Active40TextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(labelRes),
        keyboardType = KeyboardType.Number,
        imeAction = imeAction,
        modifier = modifier,
    )
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Active40Colors.onBackground)
}
