package com.finnvek.active40.ui.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.finnvek.active40.R
import com.finnvek.active40.data.CheckInRepository
import com.finnvek.active40.data.SettingsRepository
import com.finnvek.active40.data.db.CheckInWithSymptoms
import com.finnvek.active40.ui.appViewModel
import com.finnvek.active40.ui.checkInSummaryLines
import com.finnvek.active40.ui.components.GlassCard
import com.finnvek.active40.ui.components.GlassListRow
import com.finnvek.active40.ui.components.LightCard
import com.finnvek.active40.ui.components.PillButton
import com.finnvek.active40.ui.components.ScreenTitle
import com.finnvek.active40.ui.components.SectionTitle
import com.finnvek.active40.ui.formatDayLong
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class RecoveryState(
    val checkIns: List<CheckInWithSymptoms> = emptyList(),
    val symptomTrackingEnabled: Boolean = false,
)

class RecoveryViewModel(
    checkInRepository: CheckInRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<RecoveryState> =
        combine(checkInRepository.observeAllWithSymptoms(), settingsRepository.settings) { checkIns, settings ->
            RecoveryState(checkIns, settings.symptomTrackingEnabled)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecoveryState())
}

@Composable
fun RecoveryScreen(onOpenCheckIn: (LocalDate) -> Unit) {
    val viewModel = appViewModel { RecoveryViewModel(it.checkInRepository, it.settingsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val todayItem = state.checkIns.firstOrNull { it.checkIn.date == today }
    val past = state.checkIns.filter { it.checkIn.date.isBefore(today) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Active40Spacing.screenHorizontal, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle(stringResource(R.string.recovery_title))

        if (todayItem == null) {
            LightCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(stringResource(R.string.recovery_today))
                    PillButton(
                        text = stringResource(R.string.add_today_checkin),
                        onClick = { onOpenCheckIn(today) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            LightCard(Modifier.fillMaxWidth(), onClick = { onOpenCheckIn(today) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionTitle(stringResource(R.string.recovery_today))
                    checkInSummaryLines(todayItem, state.symptomTrackingEnabled).forEach {
                        Text(it, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        stringResource(R.string.recovery_edit_today),
                        style = MaterialTheme.typography.labelLarge,
                        color = Active40Colors.onSurfaceMuted,
                    )
                }
            }
        }

        SectionTitle(stringResource(R.string.recovery_past))
        if (past.isEmpty()) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.recovery_no_past), style = MaterialTheme.typography.bodyLarge)
            }
        }
        past.forEach { item ->
            GlassListRow(
                title = formatDayLong(item.checkIn.date),
                supportingText = checkInSummaryLines(item, state.symptomTrackingEnabled).joinToString("\n"),
                onClick = { onOpenCheckIn(item.checkIn.date) },
            )
        }
    }
}
