package com.finnvek.active40.ui.strength

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finnvek.active40.R
import com.finnvek.active40.ui.components.Active40TextField
import com.finnvek.active40.ui.components.PillButton
import com.finnvek.active40.ui.components.ScaleSelector
import com.finnvek.active40.ui.components.SectionTitle
import com.finnvek.active40.ui.components.SwitchRow
import com.finnvek.active40.ui.components.chipColorsOnLight
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Spacing

private val effortRange = 1..10
private const val EFFORT_COLUMNS = 5

/**
 * Harjoituksen päättäminen: kertoo, montako tekemätöntä sarjaa poistuu, ja kysyy valinnaisen
 * kokonaisrasituksen ja muistiinpanon. Voi tallentaa harjoituksen myös uudeksi rutiiniksi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishSessionSheet(
    title: String,
    initialEffort: Int?,
    completedSets: Int,
    undoneSets: Int,
    initialNote: String,
    defaultRoutineName: String,
    onFinish: (effort: Int?, note: String, routineName: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var effort by rememberSaveable { mutableStateOf(initialEffort) }
    var note by rememberSaveable { mutableStateOf(initialNote) }
    var saveAsRoutine by rememberSaveable { mutableStateOf(false) }
    var routineName by rememberSaveable { mutableStateOf(defaultRoutineName) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Active40Colors.surfaceLight,
        contentColor = Active40Colors.onSurface,
    ) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Active40Spacing.screenHorizontal)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (completedSets == 0) {
                Text(stringResource(R.string.finish_no_sets), style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            if (undoneSets > 0) {
                Text(pluralStringResource(R.plurals.finish_undone_sets, undoneSets, undoneSets), style = MaterialTheme.typography.bodyLarge)
            }
            SectionTitle(stringResource(R.string.effort_label))
            Text(
                stringResource(R.string.effort_optional),
                style = MaterialTheme.typography.bodySmall,
                color = Active40Colors.onSurfaceMuted,
            )
            ScaleSelector(
                range = effortRange,
                selected = effort,
                onSelect = { effort = it },
                colors = chipColorsOnLight,
                columns = EFFORT_COLUMNS,
            )
            Active40TextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.session_note_label),
                onLightSurface = true,
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )
            SwitchRow(
                text = stringResource(R.string.finish_save_as_routine),
                checked = saveAsRoutine,
                onCheckedChange = { saveAsRoutine = it },
            )
            if (saveAsRoutine) {
                Active40TextField(
                    value = routineName,
                    onValueChange = { routineName = it },
                    label = stringResource(R.string.routine_name_label_new),
                    onLightSurface = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PillButton(
                text = title,
                onClick = { onFinish(effort, note, if (saveAsRoutine) routineName else null) },
                enabled = !saveAsRoutine || routineName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
