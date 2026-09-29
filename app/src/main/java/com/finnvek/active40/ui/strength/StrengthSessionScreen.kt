package com.finnvek.active40.ui.strength

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.active40.R
import com.finnvek.active40.data.db.SessionExerciseDetail
import com.finnvek.active40.domain.formatClock
import com.finnvek.active40.ui.appViewModel
import com.finnvek.active40.ui.components.ConfirmDialog
import com.finnvek.active40.ui.components.ExercisePickerSheet
import com.finnvek.active40.ui.components.FullScreen
import com.finnvek.active40.ui.components.GlassCard
import com.finnvek.active40.ui.components.GlassIconButton
import com.finnvek.active40.ui.components.LightCard
import com.finnvek.active40.ui.components.MenuAction
import com.finnvek.active40.ui.components.OverflowMenu
import com.finnvek.active40.ui.components.PillButton
import com.finnvek.active40.ui.components.PlainIconButton
import com.finnvek.active40.ui.components.TextInputDialog
import com.finnvek.active40.ui.components.rememberNow
import com.finnvek.active40.ui.formatDayLong
import com.finnvek.active40.ui.lastTimeText
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Shapes
import com.finnvek.active40.ui.theme.NumberFont
import java.time.Duration

private enum class SessionDialog { Rename, Note, Discard }

@Composable
fun StrengthSessionScreen(
    sessionId: Long,
    onBack: () -> Unit,
    onClosed: () -> Unit,
) {
    val viewModel =
        appViewModel {
            StrengthSessionViewModel(
                sessionId,
                it.strengthRepository,
                it.exerciseRepository,
                it.routineRepository,
                it.settingsRepository,
                it.restTimer,
            )
        }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exercises by viewModel.activeExercises.collectAsStateWithLifecycle()
    val askPermission by viewModel.shouldAskNotificationPermission.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<SessionDialog?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }

    LaunchedEffect(state.isClosed) { if (state.isClosed) onClosed() }
    NotificationPermissionRequest(askPermission, viewModel::onNotificationPermissionAsked)

    val session = state.session ?: return
    val editing = state.isEditingFinished
    val defaultTitle = stringResource(R.string.strength_session)
    val title = session.name ?: state.routineName ?: defaultTitle
    val now by rememberNow()

    FullScreen(
        title = title,
        subtitle = if (title != defaultTitle) defaultTitle else null,
        onBack = onBack,
        headerCenter = {
            if (editing) {
                Text(formatDayLong(session.date), style = MaterialTheme.typography.bodySmall, color = Active40Colors.onBackgroundMuted)
            } else {
                SessionClock(Duration.between(session.startedAt, now).seconds)
            }
        },
        actions = {
            OverflowMenu(
                contentDescription = stringResource(R.string.session_menu),
                actions =
                    listOf(
                        MenuAction(stringResource(R.string.action_rename)) { dialog = SessionDialog.Rename },
                        MenuAction(
                            stringResource(if (session.note == null) R.string.session_add_note else R.string.session_edit_note),
                        ) { dialog = SessionDialog.Note },
                        MenuAction(stringResource(if (editing) R.string.session_delete else R.string.session_discard)) {
                            dialog = SessionDialog.Discard
                        },
                    ),
            )
        },
        bottomBar = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(
                    iconRes = R.drawable.ic_plus,
                    contentDescription = stringResource(R.string.exercises_add),
                    onClick = { showPicker = true },
                    modifier = Modifier.size(56.dp),
                )
                PillButton(
                    text = stringResource(if (editing) R.string.session_save_changes else R.string.finish_session),
                    onClick = { showFinish = true },
                    containerColor = Active40Colors.accentStrength,
                    contentColor = Active40Colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        val current = state.current
        if (current == null) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.session_no_exercises), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            key(current.sessionExercise.id) {
                CurrentExerciseCard(
                    detail = current,
                    state = state,
                    viewModel = viewModel,
                    isFirst = state.exercises.firstOrNull() == current,
                    isLast = state.exercises.lastOrNull() == current,
                )
            }
            if (!editing) {
                RestTimerRow(
                    restSeconds = session.restSeconds,
                    remainingSeconds = state.restEndsAt?.let { Duration.between(now, it).seconds.coerceAtLeast(0) },
                    onShorter = { viewModel.adjustRest(-REST_STEP_SECONDS) },
                    onLonger = { viewModel.adjustRest(REST_STEP_SECONDS) },
                    onSkip = viewModel::skipRest,
                )
            }
        }
        state.exercises.filter { it != current }.forEach { detail ->
            OtherExerciseCard(detail, state) { viewModel.select(detail.sessionExercise.id) }
        }
    }

    when (dialog) {
        SessionDialog.Rename -> {
            TextInputDialog(
                title = stringResource(R.string.session_rename_title),
                label = stringResource(R.string.session_name_label),
                confirmText = stringResource(R.string.action_save),
                initialValue = title,
                onConfirm = {
                    viewModel.rename(it)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
        }

        SessionDialog.Note -> {
            TextInputDialog(
                title = stringResource(if (session.note == null) R.string.session_add_note else R.string.session_edit_note),
                label = stringResource(R.string.session_note_label),
                confirmText = stringResource(R.string.action_save),
                initialValue = session.note.orEmpty(),
                singleLine = false,
                allowBlank = true,
                onConfirm = {
                    viewModel.setNote(it)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
        }

        SessionDialog.Discard -> {
            ConfirmDialog(
                title = stringResource(if (editing) R.string.session_delete_title else R.string.session_discard_title),
                message = stringResource(if (editing) R.string.session_delete_message else R.string.session_discard_message),
                confirmText = stringResource(if (editing) R.string.action_delete else R.string.action_discard),
                onConfirm = {
                    dialog = null
                    viewModel.discard()
                },
                onDismiss = { dialog = null },
            )
        }

        null -> {}
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

    if (showFinish) {
        FinishSessionSheet(
            title = stringResource(if (editing) R.string.session_save_changes else R.string.finish_session),
            initialEffort = session.effort,
            completedSets = state.completedSetCount,
            undoneSets = state.undoneSetCount,
            initialNote = session.note.orEmpty(),
            defaultRoutineName = title,
            onFinish = { effort, note, routineName ->
                showFinish = false
                viewModel.finish(effort, note, routineName)
            },
            onDismiss = { showFinish = false },
        )
    }
}

@Composable
private fun SessionClock(elapsedSeconds: Long) {
    val clock = formatClock(elapsedSeconds)
    val description = stringResource(R.string.session_time_description, clock)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            Modifier
                .heightIn(min = 40.dp)
                .background(Active40Colors.inkDark, Active40Shapes.pill)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Box(Modifier.size(8.dp).background(Active40Colors.accentStrength, Active40Shapes.pill))
        Text(clock, style = MaterialTheme.typography.titleSmall, fontFamily = NumberFont, color = Active40Colors.onBackground)
    }
}

@Composable
private fun CurrentExerciseCard(
    detail: SessionExerciseDetail,
    state: SessionUiState,
    viewModel: StrengthSessionViewModel,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val name = detail.exercise.name
    LightCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, style = MaterialTheme.typography.titleLarge)
                    // Vanhaa harjoitusta muokattaessa "Last time" voisi viitata myöhempään harjoitukseen, joten se piilotetaan.
                    if (!state.isEditingFinished) {
                        Text(
                            lastTimeText(state.lastTime[detail.exercise.id].orEmpty(), state.weightUnit),
                            style = MaterialTheme.typography.bodySmall,
                            color = Active40Colors.onSurfaceMuted,
                        )
                    }
                }
                OverflowMenu(
                    contentDescription = stringResource(R.string.action_options_for, name),
                    actions =
                        buildList {
                            if (!isFirst) add(MenuAction(stringResource(R.string.exercise_move_up)) { viewModel.moveExercise(detail, -1) })
                            if (!isLast) add(MenuAction(stringResource(R.string.exercise_move_down)) { viewModel.moveExercise(detail, 1) })
                            add(MenuAction(stringResource(R.string.exercise_remove)) { viewModel.removeExercise(detail) })
                        },
                )
            }
            SetHeaderRow(state.weightUnit)
            detail.sets.forEachIndexed { index, set ->
                key(set.id) {
                    SetRow(
                        set = set,
                        number = index + 1,
                        unit = state.weightUnit,
                        onWeightChange = { viewModel.setWeight(set, it, state.weightUnit) },
                        onRepsChange = { viewModel.setReps(set, it) },
                        onRpeChange = { viewModel.setRpe(set, it) },
                        onToggleDone = { viewModel.toggleDone(set) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                LightTextButton(stringResource(R.string.add_set)) { viewModel.addSet(detail) }
                if (detail.sets.isNotEmpty()) {
                    LightTextButton(stringResource(R.string.remove_set)) { viewModel.removeLastSet(detail) }
                }
            }
        }
    }
}

@Composable
private fun LightTextButton(
    text: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = Active40Colors.onSurface)
    }
}

@Composable
private fun RestTimerRow(
    restSeconds: Int,
    remainingSeconds: Long?,
    onShorter: () -> Unit,
    onLonger: () -> Unit,
    onSkip: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlainIconButton(R.drawable.ic_minus, stringResource(R.string.rest_shorter), onClick = onShorter)
            Text(
                text =
                    if (remainingSeconds != null) {
                        stringResource(R.string.rest_label, formatClock(remainingSeconds))
                    } else {
                        stringResource(R.string.rest_length, formatClock(restSeconds.toLong()))
                    },
                style = MaterialTheme.typography.titleSmall,
                fontFamily = NumberFont,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
            PlainIconButton(R.drawable.ic_plus, stringResource(R.string.rest_longer), onClick = onLonger)
            if (remainingSeconds != null) {
                TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.rest_skip), color = Active40Colors.onBackground)
                }
            }
        }
    }
}

@Composable
private fun OtherExerciseCard(
    detail: SessionExerciseDetail,
    state: SessionUiState,
    onClick: () -> Unit,
) {
    val done = detail.sets.count { it.isCompleted }
    val total = detail.sets.size
    val progressDescription = stringResource(R.string.set_progress_description, done, total)
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(detail.exercise.name, style = MaterialTheme.typography.titleMedium)
                if (!state.isEditingFinished) {
                    Text(
                        lastTimeText(state.lastTime[detail.exercise.id].orEmpty(), state.weightUnit),
                        style = MaterialTheme.typography.bodySmall,
                        color = Active40Colors.onGlassSecondary,
                    )
                }
            }
            Text(
                stringResource(R.string.set_progress, done, total),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = NumberFont,
                color = Active40Colors.onGlassSecondary,
                modifier = Modifier.semantics { contentDescription = progressDescription },
            )
        }
    }
}

/** Pyytää ilmoitusluvan (Android 13+) ensimmäisellä ajastimen käyttökerralla. Ajastin toimii ilman lupaakin. */
@Composable
private fun NotificationPermissionRequest(
    ask: Boolean,
    onAsked: () -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onAsked() }
    LaunchedEffect(ask) {
        if (!ask) return@LaunchedEffect
        val needsRequest =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsRequest) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else onAsked()
    }
}
