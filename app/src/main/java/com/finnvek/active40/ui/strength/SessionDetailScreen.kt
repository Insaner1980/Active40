package com.finnvek.active40.ui.strength

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.finnvek.active40.R
import com.finnvek.active40.data.RoutineRepository
import com.finnvek.active40.data.SettingsRepository
import com.finnvek.active40.data.StrengthRepository
import com.finnvek.active40.data.db.SessionDetail
import com.finnvek.active40.domain.WeightUnit
import com.finnvek.active40.domain.durationSeconds
import com.finnvek.active40.domain.formatWeight
import com.finnvek.active40.ui.appViewModel
import com.finnvek.active40.ui.components.ConfirmDialog
import com.finnvek.active40.ui.components.FullScreen
import com.finnvek.active40.ui.components.GlassCard
import com.finnvek.active40.ui.components.LightCard
import com.finnvek.active40.ui.components.MenuAction
import com.finnvek.active40.ui.components.OverflowMenu
import com.finnvek.active40.ui.components.TextInputDialog
import com.finnvek.active40.ui.formatDayLong
import com.finnvek.active40.ui.formatTrainingTime
import com.finnvek.active40.ui.label
import com.finnvek.active40.ui.sessionTitle
import com.finnvek.active40.ui.theme.Active40Colors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class SessionDetailState(
    val isLoading: Boolean = true,
    val detail: SessionDetail? = null,
    val routineName: String? = null,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val routineSaved: Boolean = false,
)

class SessionDetailViewModel(
    private val sessionId: Long,
    private val strengthRepository: StrengthRepository,
    routineRepository: RoutineRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val routineSaved = MutableStateFlow(false)

    val state: StateFlow<SessionDetailState> =
        combine(
            strengthRepository.observeSession(sessionId).map { detail ->
                detail?.let { it to it.session.routineId?.let { id -> routineRepository.get(id)?.name } }
            },
            settingsRepository.settings,
            routineSaved,
        ) { pair, settings, saved ->
            SessionDetailState(
                isLoading = false,
                detail =
                    pair?.first?.let { d ->
                        d.copy(
                            exercises =
                                d.exercises
                                    .sortedBy { it.sessionExercise.position }
                                    .map { e -> e.copy(sets = e.sets.sortedBy { it.position }) },
                        )
                    },
                routineName = pair?.second,
                weightUnit = settings.weightUnit,
                routineSaved = saved,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailState())

    fun delete() {
        val session = state.value.detail?.session ?: return
        viewModelScope.launch { strengthRepository.discard(session) }
    }

    fun saveAsRoutine(name: String) {
        viewModelScope.launch {
            strengthRepository.saveAsRoutine(sessionId, name)
            routineSaved.value = true
        }
    }
}

private enum class DetailDialog { SaveRoutine, Delete }

/** Päättyneen voimaharjoituksen tiedot. Muokkaus avaa harjoitusnäkymän muokkaustilassa. */
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val viewModel =
        appViewModel { SessionDetailViewModel(sessionId, it.strengthRepository, it.routineRepository, it.settingsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<DetailDialog?>(null) }

    // Poistettu harjoitus: näkymä sulkeutuu.
    LaunchedEffect(state.isLoading, state.detail) { if (!state.isLoading && state.detail == null) onBack() }
    val detail = state.detail ?: return
    val session = detail.session
    val title = sessionTitle(session, state.routineName)
    val unitLabel = state.weightUnit.label()
    val startTime =
        session.startedAt
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

    FullScreen(
        title = title,
        subtitle = stringResource(R.string.strength_session).takeIf { it != title },
        onBack = onBack,
        actions = {
            OverflowMenu(
                contentDescription = stringResource(R.string.session_menu),
                actions =
                    listOf(
                        MenuAction(stringResource(R.string.session_edit), onEdit),
                        MenuAction(stringResource(R.string.session_save_as_routine)) { dialog = DetailDialog.SaveRoutine },
                        MenuAction(stringResource(R.string.session_delete)) { dialog = DetailDialog.Delete },
                    ),
            )
        },
    ) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatDayLong(session.date), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.session_detail_started, startTime, formatTrainingTime(session.durationSeconds())),
                    style = MaterialTheme.typography.bodyLarge,
                )
                session.effort?.let { Text(stringResource(R.string.session_effort, it), style = MaterialTheme.typography.bodyLarge) }
                session.note?.let { Text(stringResource(R.string.summary_note, it), style = MaterialTheme.typography.bodyLarge) }
                if (state.routineSaved) {
                    Text(
                        stringResource(R.string.routine_saved),
                        style = MaterialTheme.typography.bodySmall,
                        color = Active40Colors.onGlassSecondary,
                    )
                }
            }
        }
        detail.exercises.forEach { exercise ->
            LightCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(exercise.exercise.name, style = MaterialTheme.typography.titleMedium)
                    exercise.sets.forEachIndexed { index, set ->
                        val number = index + 1
                        val line =
                            when {
                                set.weightKg == 0.0 -> {
                                    stringResource(R.string.session_set_line_bodyweight, number, set.reps)
                                }

                                set.rpe != null -> {
                                    stringResource(
                                        R.string.session_set_line_rpe,
                                        number,
                                        "${formatWeight(set.weightKg, state.weightUnit)} $unitLabel",
                                        set.reps,
                                        set.rpe,
                                    )
                                }

                                else -> {
                                    stringResource(
                                        R.string.session_set_line,
                                        number,
                                        "${formatWeight(set.weightKg, state.weightUnit)} $unitLabel",
                                        set.reps,
                                    )
                                }
                            }
                        Text(line, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    when (dialog) {
        DetailDialog.SaveRoutine -> {
            TextInputDialog(
                title = stringResource(R.string.session_save_as_routine),
                label = stringResource(R.string.routine_name_label_new),
                confirmText = stringResource(R.string.action_save),
                initialValue = title,
                onConfirm = {
                    viewModel.saveAsRoutine(it)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
        }

        DetailDialog.Delete -> {
            ConfirmDialog(
                title = stringResource(R.string.session_delete_title),
                message = stringResource(R.string.session_delete_message),
                confirmText = stringResource(R.string.action_delete),
                onConfirm = {
                    dialog = null
                    viewModel.delete()
                },
                onDismiss = { dialog = null },
            )
        }

        null -> {}
    }
}
