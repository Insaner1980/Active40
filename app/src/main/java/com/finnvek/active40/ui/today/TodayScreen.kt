package com.finnvek.active40.ui.today

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.active40.R
import com.finnvek.active40.data.db.CheckIn
import com.finnvek.active40.domain.durationSeconds
import com.finnvek.active40.domain.formatClock
import com.finnvek.active40.domain.formatDistance
import com.finnvek.active40.ui.appViewModel
import com.finnvek.active40.ui.components.AdaptiveRow
import com.finnvek.active40.ui.components.GlassCard
import com.finnvek.active40.ui.components.GlassIconButton
import com.finnvek.active40.ui.components.LightCard
import com.finnvek.active40.ui.components.RunMarker
import com.finnvek.active40.ui.components.StrengthMarker
import com.finnvek.active40.ui.components.isLargeFontScale
import com.finnvek.active40.ui.components.rememberNow
import com.finnvek.active40.ui.energyWord
import com.finnvek.active40.ui.formatDayLong
import com.finnvek.active40.ui.formatTrainingTime
import com.finnvek.active40.ui.label
import com.finnvek.active40.ui.runNumbers
import com.finnvek.active40.ui.sessionNumbers
import com.finnvek.active40.ui.sessionTitle
import com.finnvek.active40.ui.sleepWord
import com.finnvek.active40.ui.strength.StrengthStartSheet
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Shapes
import com.finnvek.active40.ui.theme.Active40Spacing
import com.finnvek.active40.ui.theme.NumberFont
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private const val ICON_BACKDROP_ALPHA = 0.12f
private const val NOON = 12
private const val EVENING = 18
private val dayPillShape = RoundedCornerShape(18.dp)

@Composable
fun TodayScreen(
    onOpenSettings: () -> Unit,
    onOpenSession: (Long) -> Unit,
    onManageRoutines: () -> Unit,
    onLogRun: () -> Unit,
    onOpenCheckIn: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    val viewModel =
        appViewModel {
            TodayViewModel(it.strengthRepository, it.routineRepository, it.runRepository, it.checkInRepository, it.settingsRepository)
        }
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showStartSheet by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Active40Spacing.screenHorizontal, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Header(state.today, onOpenSettings)
        CheckInChip(state.checkIn) { onOpenCheckIn(state.today) }
        WeekCard(state, onOpenDay)

        activeSession?.let { session ->
            val now by rememberNow()
            GlassCard(Modifier.fillMaxWidth(), onClick = { onOpenSession(session.id) }) {
                Text(stringResource(R.string.session_in_progress), style = MaterialTheme.typography.titleMedium)
                Text(
                    formatClock(Duration.between(session.startedAt, now).seconds),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = NumberFont,
                    color = Active40Colors.onGlassSecondary,
                )
            }
        }

        // Suurella fonttikoolla painikkeet pinotaan, jotta teksti ei katkea kesken sanan.
        AdaptiveRow {
            LogActionButton(
                text = stringResource(R.string.log_strength),
                iconRes = R.drawable.ic_strength,
                color = Active40Colors.accentStrength,
                modifier = it,
                onClick = {
                    // Vain yksi harjoitus kerrallaan: käynnissä olevaan palataan suoraan.
                    val active = activeSession
                    if (active != null) onOpenSession(active.id) else showStartSheet = true
                },
            )
            LogActionButton(
                text = stringResource(R.string.log_run),
                iconRes = R.drawable.ic_run,
                color = Active40Colors.accentRun,
                modifier = it,
                onClick = onLogRun,
            )
        }

        state.lastActivity?.let { LastActivityCard(it, state, onOpenDay) }
    }

    if (showStartSheet) {
        StrengthStartSheet(
            routines = routines,
            onStart = { routineId ->
                showStartSheet = false
                viewModel.startSession(routineId, onOpenSession)
            },
            onManageRoutines = {
                showStartSheet = false
                onManageRoutines()
            },
            onDismiss = { showStartSheet = false },
        )
    }
}

@Composable
private fun Header(
    today: LocalDate,
    onOpenSettings: () -> Unit,
) {
    val hour = LocalTime.now().hour
    val greeting =
        stringResource(
            when {
                hour < NOON -> R.string.greeting_morning
                hour < EVENING -> R.string.greeting_afternoon
                else -> R.string.greeting_evening
            },
        )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatDayLong(today), style = MaterialTheme.typography.bodySmall, color = Active40Colors.onBackgroundMuted)
            Text(greeting, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        }
        GlassIconButton(R.drawable.ic_settings, stringResource(R.string.settings), onClick = onOpenSettings)
    }
}

/** Tämän päivän kirjauksen tiivistelmä tai kehotus lisätä se. Puuttuva kirjaus ei tarkoita mitään muuta. */
@Composable
private fun CheckInChip(
    checkIn: CheckIn?,
    onClick: () -> Unit,
) {
    val text =
        if (checkIn == null) {
            stringResource(R.string.add_today_checkin)
        } else {
            listOfNotNull(
                checkIn.sleep?.let { stringResource(R.string.chip_sleep, sleepWord(it)) },
                checkIn.energy?.let { stringResource(R.string.chip_energy, energyWord(it)) },
            ).joinToString(", ").ifEmpty { stringResource(R.string.today_checkin_saved) }
        }
    Row {
        Surface(
            onClick = onClick,
            shape = Active40Shapes.pill,
            color = if (checkIn == null) Color.Transparent else Active40Colors.surfaceGlass,
            contentColor = Active40Colors.onBackground,
            border = BorderStroke(1.dp, Active40Colors.outlineGlass),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(painterResource(R.drawable.ic_nav_recovery), contentDescription = null, modifier = Modifier.size(16.dp))
                Text(text, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun WeekCard(
    state: TodayState,
    onOpenDay: (LocalDate) -> Unit,
) {
    val t = state.totals
    LightCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(R.string.week_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Text(
                    formatTrainingTime(t.trainingSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = Active40Colors.onSurfaceMuted,
                )
            }
            AdaptiveRow {
                Metric(t.strengthSessions.toString(), stringResource(R.string.week_strength), it) {
                    StrengthMarker(Active40Colors.accentStrengthOnLight, size = 8.dp)
                }
                Metric(t.runs.toString(), stringResource(R.string.week_runs), it) {
                    RunMarker(Active40Colors.accentRunOnLight, size = 8.dp)
                }
                Metric(
                    formatDistance(t.runDistanceMeters, state.distanceUnit, maxDecimals = 1),
                    stringResource(R.string.week_distance_run, state.distanceUnit.label()),
                    it,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                state.days.forEach { day ->
                    DayPill(day, isToday = day.date == state.today, Modifier.weight(1f)) { onOpenDay(day.date) }
                }
            }
        }
    }
}

@Composable
private fun Metric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    marker: (@Composable () -> Unit)? = null,
) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.displaySmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            marker?.invoke()
            Text(label, style = MaterialTheme.typography.bodySmall, color = Active40Colors.onSurfaceMuted)
        }
    }
}

@Composable
private fun DayPill(
    day: DayMarkers,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // Suurella fonttikoolla lyhin muoto (M, T...), jotta nimi mahtuu kokonaan; koko nimi on kuvauksessa.
    val nameStyle = if (isLargeFontScale()) TextStyle.NARROW_STANDALONE else TextStyle.SHORT_STANDALONE
    val shortName = day.date.dayOfWeek.getDisplayName(nameStyle, Locale.getDefault())
    val description =
        listOfNotNull(
            formatDayLong(day.date),
            stringResource(R.string.today_marker).takeIf { isToday },
            stringResource(R.string.marker_strength).takeIf { day.hasStrength },
            stringResource(R.string.marker_run).takeIf { day.hasRun },
        ).joinToString(", ")
    Surface(
        onClick = onClick,
        shape = dayPillShape,
        color = if (isToday) Active40Colors.inkDark else Active40Colors.surfaceWhite,
        contentColor = if (isToday) Active40Colors.onBackground else Active40Colors.onSurface,
        modifier = modifier.heightIn(min = 68.dp).semantics { contentDescription = description },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            Text(
                shortName,
                style = MaterialTheme.typography.labelSmall,
                color = if (isToday) Active40Colors.onBackgroundMuted else Active40Colors.onSurfaceMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmall, fontFamily = NumberFont)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.heightIn(min = 6.dp)) {
                // Tämän päivän tummalla pohjalla käytetään vaaleita aksentteja, muuten tummempia variantteja.
                if (day.hasStrength) {
                    StrengthMarker(if (isToday) Active40Colors.accentStrength else Active40Colors.accentStrengthOnLight, size = 6.dp)
                }
                if (day.hasRun) {
                    RunMarker(if (isToday) Active40Colors.accentRun else Active40Colors.accentRunOnLight, size = 6.dp)
                }
            }
        }
    }
}

private data class LastActivityContent(
    val date: LocalDate,
    val title: String,
    val numbers: String,
    @param:DrawableRes val iconRes: Int,
)

@Composable
private fun LastActivityCard(
    activity: LastActivity,
    state: TodayState,
    onOpenDay: (LocalDate) -> Unit,
) {
    val content =
        when (activity) {
            is LastActivity.Strength -> {
                val s = activity.summary
                LastActivityContent(
                    s.session.date,
                    sessionTitle(s.session, s.routineName),
                    sessionNumbers(s.completedSets, s.session.durationSeconds()),
                    R.drawable.ic_strength,
                )
            }

            is LastActivity.Running -> {
                LastActivityContent(
                    activity.run.date,
                    stringResource(R.string.run_title, activity.run.runType.label()),
                    runNumbers(activity.run, state.distanceUnit),
                    R.drawable.ic_run,
                )
            }
        }
    GlassCard(Modifier.fillMaxWidth(), onClick = { onOpenDay(content.date) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(44.dp).background(Active40Colors.surfaceWhite, Active40Shapes.pill),
            ) {
                Icon(
                    painterResource(content.iconRes),
                    contentDescription = null,
                    tint = Active40Colors.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.last_activity),
                    style = MaterialTheme.typography.bodySmall,
                    color = Active40Colors.onGlassSecondary,
                )
                Text(content.title, style = MaterialTheme.typography.titleMedium)
                Text(formatDayLong(content.date), style = MaterialTheme.typography.bodySmall, color = Active40Colors.onGlassSecondary)
                Text(content.numbers, style = MaterialTheme.typography.bodySmall, color = Active40Colors.onGlassSecondary)
            }
        }
    }
}

/** Suuri kirjauspainike aksenttivärillä. Laji erottuu myös ikonista ja tekstistä, ei pelkästä väristä. */
@Composable
fun LogActionButton(
    text: String,
    @DrawableRes iconRes: Int,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 76.dp),
        shape = Active40Shapes.cardSecondary,
        color = color,
        contentColor = Active40Colors.onSurface,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(Active40Colors.onSurface.copy(alpha = ICON_BACKDROP_ALPHA), Active40Shapes.pill),
            ) {
                Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(22.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
