package com.finnvek.active40.ui.strength

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.finnvek.active40.R
import com.finnvek.active40.data.db.RoutineSummary
import com.finnvek.active40.ui.components.SectionTitle
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Spacing

/** "Log strength" -paneeli: tyhjä harjoitus, rutiinit ja rutiinien hallinta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrengthStartSheet(
    routines: List<RoutineSummary>,
    onStart: (routineId: Long?) -> Unit,
    onManageRoutines: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Active40Colors.surfaceLight,
        contentColor = Active40Colors.onSurface,
    ) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Active40Spacing.screenHorizontal)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.log_strength), style = MaterialTheme.typography.headlineMedium)
            SheetRow(stringResource(R.string.start_empty_session)) { onStart(null) }
            if (routines.isNotEmpty()) {
                HorizontalDivider(color = Active40Colors.onSurfaceMuted.copy(alpha = 0.3f))
                SectionTitle(stringResource(R.string.start_routines_heading), Modifier.padding(top = 8.dp))
                routines.forEach { summary ->
                    SheetRow(
                        title = summary.routine.name,
                        supporting =
                            pluralStringResource(R.plurals.routine_exercise_count, summary.exerciseCount, summary.exerciseCount),
                    ) { onStart(summary.routine.id) }
                }
            }
            HorizontalDivider(color = Active40Colors.onSurfaceMuted.copy(alpha = 0.3f))
            SheetRow(stringResource(R.string.settings_manage_routines), onClick = onManageRoutines)
        }
    }
}

@Composable
private fun SheetRow(
    title: String,
    supporting: String? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = Active40Spacing.minTouchTarget)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (supporting != null) {
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = Active40Colors.onSurfaceMuted)
        }
    }
}
