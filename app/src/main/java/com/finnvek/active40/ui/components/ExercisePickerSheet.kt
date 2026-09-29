package com.finnvek.active40.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.finnvek.active40.R
import com.finnvek.active40.data.db.Exercise
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Spacing

/**
 * Liikkeen valinta alapaneelissa: haku aktiivisista liikkeistä ja uuden liikkeen luonti hakusanasta,
 * jos samannimistä ei ole.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    exercises: List<Exercise>,
    onPick: (Exercise) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val trimmed = query.trim()
    val matches = exercises.filter { it.name.contains(trimmed, ignoreCase = true) }
    val exactExists = exercises.any { it.name.equals(trimmed, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Active40Colors.surfaceLight,
        contentColor = Active40Colors.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Active40Spacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Active40TextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.exercises_search_label),
                onLightSurface = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                if (trimmed.isNotEmpty() && !exactExists) {
                    item {
                        PickerRow(stringResource(R.string.exercises_create_named, trimmed)) { onCreate(trimmed) }
                    }
                }
                items(matches, key = { it.id }) { exercise ->
                    PickerRow(exercise.name) { onPick(exercise) }
                }
                if (matches.isEmpty() && trimmed.isEmpty()) {
                    item { Text(stringResource(R.string.exercises_none_found), Modifier.padding(vertical = 12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = Active40Spacing.minTouchTarget)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 13.dp),
    )
}
