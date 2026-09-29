package com.finnvek.active40.ui.routines

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.active40.R
import com.finnvek.active40.ui.appViewModel
import com.finnvek.active40.ui.components.Active40TextField
import com.finnvek.active40.ui.components.ConfirmDialog
import com.finnvek.active40.ui.components.ExercisePickerSheet
import com.finnvek.active40.ui.components.FullScreen
import com.finnvek.active40.ui.components.GlassButton
import com.finnvek.active40.ui.components.LightCard
import com.finnvek.active40.ui.components.PillButton
import com.finnvek.active40.ui.components.PlainIconButton
import com.finnvek.active40.ui.components.SectionTitle
import com.finnvek.active40.ui.theme.Active40Colors

@Composable
fun RoutineEditScreen(
    routineId: Long,
    onDone: () -> Unit,
) {
    val viewModel =
        appViewModel { RoutineEditViewModel(routineId, it.routineRepository, it.exerciseRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exercises by viewModel.activeExercises.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) { if (state.isSaved) onDone() }

    val onBack: () -> Unit = { if (state.hasChanges) confirmDiscard = true else onDone() }
    BackHandler(onBack = onBack)

    FullScreen(
        title = stringResource(if (state.isNew) R.string.routine_edit_new_title else R.string.routine_edit_title),
        onBack = onBack,
        bottomBar = {
            PillButton(
                text = stringResource(R.string.action_save),
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        if (state.isLoading) return@FullScreen
        Active40TextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = stringResource(R.string.routine_name_label),
            modifier = Modifier.fillMaxWidth(),
        )
        SectionTitle(stringResource(R.string.routine_exercises_section))
        Text(
            stringResource(if (state.items.isEmpty()) R.string.routine_no_exercises else R.string.routine_targets_optional),
            style = MaterialTheme.typography.bodySmall,
            color = Active40Colors.onBackgroundMuted,
        )
        state.items.forEachIndexed { index, item ->
            RoutineItemCard(
                item = item,
                isFirst = index == 0,
                isLast = index == state.items.lastIndex,
                onSetsChange = { viewModel.setSets(item.key, it) },
                onRepsChange = { viewModel.setReps(item.key, it) },
                onMove = { delta -> viewModel.move(item.key, delta) },
                onRemove = { viewModel.remove(item.key) },
            )
        }
        GlassButton(
            text = stringResource(R.string.exercises_add),
            onClick = { showPicker = true },
            iconRes = R.drawable.ic_plus,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showPicker) {
        ExercisePickerSheet(
            exercises = exercises,
            onPick = {
                viewModel.addExercise(it)
                showPicker = false
            },
            onCreate = {
                viewModel.createAndAddExercise(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.routine_discard_title),
            message = stringResource(R.string.routine_discard_message),
            confirmText = stringResource(R.string.action_discard),
            onConfirm = {
                confirmDiscard = false
                onDone()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun RoutineItemCard(
    item: EditableRoutineItem,
    isFirst: Boolean,
    isLast: Boolean,
    onSetsChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    LightCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.exerciseName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            PlainIconButton(
                R.drawable.ic_chevron_up,
                stringResource(R.string.action_move_up, item.exerciseName),
                onClick = { onMove(-1) },
                enabled = !isFirst,
            )
            PlainIconButton(
                R.drawable.ic_chevron_down,
                stringResource(R.string.action_move_down, item.exerciseName),
                onClick = { onMove(1) },
                enabled = !isLast,
            )
            PlainIconButton(
                R.drawable.ic_close,
                stringResource(R.string.action_remove_item, item.exerciseName),
                onClick = onRemove,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Active40TextField(
                value = item.setsText,
                onValueChange = onSetsChange,
                label = stringResource(R.string.routine_target_sets),
                onLightSurface = true,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                modifier = Modifier.weight(1f),
            )
            Active40TextField(
                value = item.repsText,
                onValueChange = onRepsChange,
                label = stringResource(R.string.routine_target_reps),
                onLightSurface = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
