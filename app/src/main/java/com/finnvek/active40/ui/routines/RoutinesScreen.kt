package com.finnvek.active40.ui.routines

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.active40.R
import com.finnvek.active40.data.db.Routine
import com.finnvek.active40.ui.appViewModel
import com.finnvek.active40.ui.components.ConfirmDialog
import com.finnvek.active40.ui.components.FullScreen
import com.finnvek.active40.ui.components.GlassCard
import com.finnvek.active40.ui.components.GlassListRow
import com.finnvek.active40.ui.components.MenuAction
import com.finnvek.active40.ui.components.OverflowMenu
import com.finnvek.active40.ui.components.PillButton
import com.finnvek.active40.ui.components.PlainIconButton

@Composable
fun RoutinesScreen(
    onBack: () -> Unit,
    onEdit: (routineId: Long) -> Unit,
    onCreate: () -> Unit,
) {
    val viewModel = appViewModel { RoutinesViewModel(it.routineRepository) }
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<Routine?>(null) }

    FullScreen(title = stringResource(R.string.routines_title), onBack = onBack) {
        PillButton(
            text = stringResource(R.string.routines_new),
            onClick = onCreate,
            iconRes = R.drawable.ic_plus,
            modifier = Modifier.fillMaxWidth(),
        )
        if (routines.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.routines_empty), style = MaterialTheme.typography.bodyLarge)
            }
        }
        routines.forEachIndexed { index, summary ->
            val name = summary.routine.name
            GlassListRow(
                title = name,
                supportingText =
                    pluralStringResource(R.plurals.routine_exercise_count, summary.exerciseCount, summary.exerciseCount),
                onClick = { onEdit(summary.routine.id) },
            ) {
                PlainIconButton(
                    R.drawable.ic_chevron_up,
                    stringResource(R.string.action_move_up, name),
                    onClick = { viewModel.move(index, -1) },
                    enabled = index > 0,
                )
                PlainIconButton(
                    R.drawable.ic_chevron_down,
                    stringResource(R.string.action_move_down, name),
                    onClick = { viewModel.move(index, 1) },
                    enabled = index < routines.lastIndex,
                )
                OverflowMenu(
                    stringResource(R.string.action_options_for, name),
                    listOf(MenuAction(stringResource(R.string.action_delete)) { toDelete = summary.routine }),
                )
            }
        }
    }

    toDelete?.let { routine ->
        ConfirmDialog(
            title = stringResource(R.string.routines_delete_title),
            message = stringResource(R.string.routines_delete_message, routine.name),
            confirmText = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.delete(routine)
                toDelete = null
            },
            onDismiss = { toDelete = null },
        )
    }
}
