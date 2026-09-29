package com.finnvek.active40.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.finnvek.active40.R
import com.finnvek.active40.data.AppSettings
import com.finnvek.active40.data.SettingsRepository
import com.finnvek.active40.data.backup.BackupError
import com.finnvek.active40.data.backup.BackupException
import com.finnvek.active40.data.backup.BackupRepository
import com.finnvek.active40.data.backup.ParsedBackup
import com.finnvek.active40.domain.DistanceUnit
import com.finnvek.active40.domain.WeightUnit
import com.finnvek.active40.timer.RestTimer
import com.finnvek.active40.ui.appViewModel
import com.finnvek.active40.ui.components.Active40Dialog
import com.finnvek.active40.ui.components.Active40TextField
import com.finnvek.active40.ui.components.FullScreen
import com.finnvek.active40.ui.components.GlassCard
import com.finnvek.active40.ui.components.GlassListRow
import com.finnvek.active40.ui.components.SectionTitle
import com.finnvek.active40.ui.components.SelectableChip
import com.finnvek.active40.ui.components.SwitchRow
import com.finnvek.active40.ui.components.chipColorsOnBackground
import com.finnvek.active40.ui.formatDayLong
import com.finnvek.active40.ui.label
import com.finnvek.active40.ui.theme.Active40Colors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val restTimer: RestTimer,
) : ViewModel() {
    val settings: StateFlow<AppSettings?> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Viimeisimmän toiminnon tulos näytettäväksi (merkkijonoresurssi). */
    val message = MutableStateFlow<Int?>(null)

    /** Luettu ja tarkistettu tuontitiedosto, joka odottaa vahvistusta. */
    val pendingImport = MutableStateFlow<ParsedBackup?>(null)

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch { settingsRepository.setWeightUnit(unit) }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch { settingsRepository.setDistanceUnit(unit) }
    }

    fun setSymptomTracking(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSymptomTrackingEnabled(enabled) }
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            message.value =
                try {
                    backupRepository.export(uri)
                    R.string.export_done
                } catch (_: IOException) {
                    R.string.export_failed
                } catch (_: SecurityException) {
                    R.string.export_failed
                }
        }
    }

    fun readImport(uri: Uri) {
        viewModelScope.launch {
            try {
                pendingImport.value = backupRepository.read(uri)
                message.value = null
            } catch (e: BackupException) {
                message.value =
                    when (e.error) {
                        BackupError.UNREADABLE -> R.string.import_error_unreadable
                        BackupError.NEWER_VERSION -> R.string.import_error_newer
                        BackupError.INVALID_DATA -> R.string.import_error_invalid
                    }
            }
        }
    }

    fun confirmImport() {
        val backup = pendingImport.value ?: return
        pendingImport.value = null
        restTimer.stop()
        viewModelScope.launch {
            backupRepository.import(backup)
            message.value = R.string.import_done
        }
    }

    fun cancelImport() {
        pendingImport.value = null
    }

    fun deleteAll() {
        restTimer.stop()
        viewModelScope.launch {
            backupRepository.deleteAll()
            message.value = R.string.delete_all_done
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageExercises: () -> Unit,
    onManageSymptoms: () -> Unit,
    onManageRoutines: () -> Unit,
) {
    val viewModel = appViewModel { SettingsViewModel(it.settingsRepository, it.backupRepository, it.restTimer) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let(viewModel::export)
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::readImport) }
    val fileName = stringResource(R.string.backup_file_name, LocalDate.now().toString())

    FullScreen(title = stringResource(R.string.settings), onBack = onBack) {
        val current = settings ?: return@FullScreen

        SectionTitle(stringResource(R.string.settings_units))
        UnitRow(R.string.settings_weight, WeightUnit.entries, current.weightUnit, { it.label() }, viewModel::setWeightUnit)
        UnitRow(R.string.settings_distance, DistanceUnit.entries, current.distanceUnit, { it.label() }, viewModel::setDistanceUnit)

        GlassCard(Modifier.fillMaxWidth()) {
            SwitchRow(
                text = stringResource(R.string.settings_symptom_tracking),
                supportingText = stringResource(R.string.settings_symptom_tracking_detail),
                checked = current.symptomTrackingEnabled,
                onCheckedChange = viewModel::setSymptomTracking,
                onLightSurface = false,
            )
        }

        SectionTitle(stringResource(R.string.settings_manage))
        GlassListRow(title = stringResource(R.string.settings_manage_exercises), onClick = onManageExercises)
        GlassListRow(title = stringResource(R.string.settings_manage_symptoms), onClick = onManageSymptoms)
        GlassListRow(title = stringResource(R.string.settings_manage_routines), onClick = onManageRoutines)

        SectionTitle(stringResource(R.string.settings_data))
        message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyLarge) }
        GlassListRow(
            title = stringResource(R.string.settings_export),
            supportingText = stringResource(R.string.settings_export_detail),
            onClick = { exportLauncher.launch(fileName) },
        )
        GlassListRow(
            title = stringResource(R.string.settings_import),
            supportingText = stringResource(R.string.settings_import_detail),
            onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
        )
        GlassListRow(
            title = stringResource(R.string.settings_delete_all),
            supportingText = stringResource(R.string.settings_delete_all_detail),
            onClick = { confirmDelete = true },
        )

        SectionTitle(stringResource(R.string.settings_about))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.settings_version, appVersion()), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.settings_privacy), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    pendingImport?.let { backup ->
        ImportConfirmDialog(backup, onConfirm = viewModel::confirmImport, onDismiss = viewModel::cancelImport)
    }
    if (confirmDelete) {
        DeleteAllDialog(
            onConfirm = {
                confirmDelete = false
                viewModel.deleteAll()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return remember {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> UnitRow(
    @StringRes labelRes: Int,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        options.forEach { option ->
            SelectableChip(
                text = label(option),
                selected = option == selected,
                onClick = { onSelect(option) },
                colors = chipColorsOnBackground,
            )
        }
    }
}

@Composable
private fun ImportConfirmDialog(
    backup: ParsedBackup,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val d = backup.data
    val sessions = d.sessions.size
    val runs = d.runs.size
    val checkIns = d.checkIns.size
    val routines = d.routines.size
    Active40Dialog(
        title = stringResource(R.string.import_confirm_title),
        confirmText = stringResource(R.string.import_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(
                    R.string.import_summary_exported,
                    formatDayLong(backup.exportedAt.atZone(ZoneId.systemDefault()).toLocalDate()),
                ),
            )
            Text(pluralStringResource(R.plurals.summary_sessions, sessions, sessions))
            Text(pluralStringResource(R.plurals.summary_runs, runs, runs))
            Text(pluralStringResource(R.plurals.summary_checkins, checkIns, checkIns))
            Text(pluralStringResource(R.plurals.summary_routines, routines, routines))
            Text(stringResource(R.string.import_replace_warning), color = Active40Colors.onSurface)
        }
    }
}

/** Kaiken datan poisto vaatii kirjoitetun vahvistuksen. */
@Composable
private fun DeleteAllDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    val word = stringResource(R.string.delete_all_word)
    Active40Dialog(
        title = stringResource(R.string.delete_all_title),
        confirmText = stringResource(R.string.action_delete),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmEnabled = typed.trim().equals(word, ignoreCase = true),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.delete_all_message))
            Active40TextField(
                value = typed,
                onValueChange = { typed = it },
                label = stringResource(R.string.delete_all_label),
                onLightSurface = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
