package com.finnvek.active40.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.finnvek.active40.R
import com.finnvek.active40.data.AppSettings
import com.finnvek.active40.data.SettingsRepository
import com.finnvek.active40.domain.DistanceUnit
import com.finnvek.active40.domain.WeightUnit
import com.finnvek.active40.ui.appViewModel
import com.finnvek.active40.ui.components.GlassButton
import com.finnvek.active40.ui.components.PillButton
import com.finnvek.active40.ui.components.ScreenTitle
import com.finnvek.active40.ui.components.SelectableChip
import com.finnvek.active40.ui.components.chipColorsOnBackground
import com.finnvek.active40.ui.label
import com.finnvek.active40.ui.theme.Active40Colors
import com.finnvek.active40.ui.theme.Active40Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STEP_COUNT = 2

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    // Yksiköiden oletus tulee laitteen kieliasetuksesta (SettingsRepository).
    val settings: StateFlow<AppSettings?> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch { settingsRepository.setWeightUnit(unit) }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch { settingsRepository.setDistanceUnit(unit) }
    }

    /** Tallentaa yksiköt (myös oletuksina valitut) ja oireseurannan valinnan ja päättää käyttöönoton. */
    fun finish(symptomTracking: Boolean) {
        val current = settings.value ?: return
        viewModelScope.launch {
            settingsRepository.setWeightUnit(current.weightUnit)
            settingsRepository.setDistanceUnit(current.distanceUnit)
            settingsRepository.setSymptomTrackingEnabled(symptomTracking)
            settingsRepository.setOnboardingCompleted(true)
        }
    }
}

/** Kaksivaiheinen käyttöönotto ensimmäisellä käynnistyksellä: yksiköt ja valinnainen oireseuranta. */
@Composable
fun OnboardingScreen() {
    val viewModel = appViewModel { OnboardingViewModel(it.settingsRepository) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(1) }
    val current = settings ?: return

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Active40Spacing.screenHorizontal, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            stringResource(R.string.onboarding_step, step, STEP_COUNT),
            style = MaterialTheme.typography.bodySmall,
            color = Active40Colors.onBackgroundMuted,
        )
        if (step == 1) {
            ScreenTitle(stringResource(R.string.onboarding_units_title))
            Text(stringResource(R.string.onboarding_units_body), style = MaterialTheme.typography.bodyLarge)
            ChoiceRow(stringResource(R.string.settings_weight)) {
                WeightUnit.entries.forEach { unit ->
                    SelectableChip(unit.label(), current.weightUnit == unit, { viewModel.setWeightUnit(unit) }, chipColorsOnBackground)
                }
            }
            ChoiceRow(stringResource(R.string.settings_distance)) {
                DistanceUnit.entries.forEach { unit ->
                    SelectableChip(unit.label(), current.distanceUnit == unit, { viewModel.setDistanceUnit(unit) }, chipColorsOnBackground)
                }
            }
            PillButton(
                text = stringResource(R.string.onboarding_continue),
                onClick = { step = 2 },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            ScreenTitle(stringResource(R.string.onboarding_symptoms_title))
            Text(stringResource(R.string.onboarding_symptoms_body), style = MaterialTheme.typography.bodyLarge)
            PillButton(
                text = stringResource(R.string.onboarding_turn_on),
                onClick = { viewModel.finish(symptomTracking = true) },
                modifier = Modifier.fillMaxWidth(),
            )
            GlassButton(
                text = stringResource(R.string.onboarding_not_now),
                onClick = { viewModel.finish(symptomTracking = false) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    chips: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        chips()
    }
}
