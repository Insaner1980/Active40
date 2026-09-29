package com.finnvek.active40.ui.checkin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.active40.R
import com.finnvek.active40.domain.SorenessArea
import com.finnvek.active40.ui.appViewModel
import com.finnvek.active40.ui.components.Active40TextField
import com.finnvek.active40.ui.components.ConfirmDialog
import com.finnvek.active40.ui.components.FullScreen
import com.finnvek.active40.ui.components.LightCard
import com.finnvek.active40.ui.components.MenuAction
import com.finnvek.active40.ui.components.OverflowMenu
import com.finnvek.active40.ui.components.PillButton
import com.finnvek.active40.ui.components.ScaleSelector
import com.finnvek.active40.ui.components.SectionTitle
import com.finnvek.active40.ui.components.SelectableChip
import com.finnvek.active40.ui.components.TextInputDialog
import com.finnvek.active40.ui.components.chipColorsOnBackground
import com.finnvek.active40.ui.components.chipColorsOnLight
import com.finnvek.active40.ui.energyWord
import com.finnvek.active40.ui.formatDayLong
import com.finnvek.active40.ui.label
import com.finnvek.active40.ui.sleepWord
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Shapes
import java.time.LocalDate

private val ratingRange = 1..5

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInScreen(
    date: LocalDate,
    onClose: () -> Unit,
) {
    val viewModel =
        appViewModel { CheckInViewModel(date, it.checkInRepository, it.symptomRepository, it.settingsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val form = state.form
    var addSymptom by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(form.isClosed) { if (form.isClosed) onClose() }

    FullScreen(
        title = stringResource(if (date == LocalDate.now()) R.string.checkin_title_today else R.string.checkin_title_other),
        onBack = onClose,
        backIconRes = R.drawable.ic_close,
        backDescription = stringResource(R.string.checkin_close),
        headerCenter = {
            Text(formatDayLong(date), style = MaterialTheme.typography.bodySmall, color = Active40Colors.onBackgroundMuted)
        },
        actions = {
            if (form.existing != null) {
                OverflowMenu(
                    contentDescription = stringResource(R.string.checkin_options),
                    actions = listOf(MenuAction(stringResource(R.string.action_delete)) { confirmDelete = true }),
                )
            }
        },
        bottomBar = {
            PillButton(
                text = stringResource(R.string.checkin_save),
                onClick = viewModel::save,
                enabled = form.hasAnswer,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        if (form.isLoading) return@FullScreen

        RatingQuestion(
            title = stringResource(R.string.checkin_sleep),
            word = form.sleep?.let { sleepWord(it) },
            selected = form.sleep,
            onSelect = viewModel::setSleep,
        )
        RatingQuestion(
            title = stringResource(R.string.checkin_energy),
            word = form.energy?.let { energyWord(it) },
            selected = form.energy,
            onSelect = viewModel::setEnergy,
        )

        SectionTitle(stringResource(R.string.checkin_soreness))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SorenessArea.entries.forEach { area ->
                SelectableChip(
                    text = area.label(),
                    selected = area in form.soreness,
                    onClick = { viewModel.toggleSoreness(area) },
                    colors = chipColorsOnBackground,
                )
            }
        }

        if (state.symptomTrackingEnabled) {
            LightCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle(stringResource(R.string.checkin_symptoms), Modifier.weight(1f))
                        Text(
                            stringResource(R.string.checkin_optional),
                            style = MaterialTheme.typography.bodySmall,
                            color = Active40Colors.onSurfaceMuted,
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.symptoms.forEach { symptom ->
                            SelectableChip(
                                text = symptom.name,
                                selected = symptom.id in form.symptomIds,
                                onClick = { viewModel.toggleSymptom(symptom.id) },
                                colors = chipColorsOnLight,
                            )
                        }
                        Surface(
                            onClick = { addSymptom = true },
                            shape = Active40Shapes.pill,
                            color = Active40Colors.surfaceLight,
                            contentColor = Active40Colors.onSurface,
                            border = BorderStroke(1.dp, Active40Colors.onSurfaceMuted),
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(
                                stringResource(R.string.checkin_custom_symptom),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            painterResource(R.drawable.ic_lock),
                            contentDescription = null,
                            tint = Active40Colors.onSurfaceMuted,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            stringResource(R.string.checkin_stored_locally),
                            style = MaterialTheme.typography.bodySmall,
                            color = Active40Colors.onSurfaceMuted,
                        )
                    }
                }
            }
        }

        Active40TextField(
            value = form.note,
            onValueChange = viewModel::setNote,
            label = stringResource(R.string.session_note_label),
            placeholder = stringResource(R.string.checkin_note_placeholder),
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (addSymptom) {
        TextInputDialog(
            title = stringResource(R.string.checkin_custom_symptom_title),
            label = stringResource(R.string.checkin_symptom_name),
            confirmText = stringResource(R.string.action_add),
            onConfirm = {
                viewModel.addCustomSymptom(it)
                addSymptom = false
            },
            onDismiss = { addSymptom = false },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.checkin_delete_title),
            message = stringResource(R.string.checkin_delete_message),
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
private fun RatingQuestion(
    title: String,
    word: String?,
    selected: Int?,
    onSelect: (Int?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(title, Modifier.weight(1f))
            Text(
                word ?: stringResource(R.string.checkin_not_answered),
                style = MaterialTheme.typography.bodySmall,
                color = Active40Colors.onBackgroundMuted,
            )
        }
        ScaleSelector(range = ratingRange, selected = selected, onSelect = onSelect, colors = chipColorsOnBackground)
    }
}
