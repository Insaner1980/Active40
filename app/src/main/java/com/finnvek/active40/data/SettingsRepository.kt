package com.finnvek.active40.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.finnvek.active40.domain.DistanceUnit
import com.finnvek.active40.domain.WeightUnit
import com.finnvek.active40.domain.defaultDistanceUnit
import com.finnvek.active40.domain.defaultWeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

data class AppSettings(
    val weightUnit: WeightUnit,
    val distanceUnit: DistanceUnit,
    val symptomTrackingEnabled: Boolean,
    val onboardingCompleted: Boolean,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(
    context: Context,
) {
    private val dataStore = context.settingsDataStore

    private object Keys {
        val weightUnit = stringPreferencesKey("weight_unit")
        val distanceUnit = stringPreferencesKey("distance_unit")
        val symptomTracking = booleanPreferencesKey("symptom_tracking_enabled")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val notificationPermissionAsked = booleanPreferencesKey("notification_permission_asked")
    }

    /** Yksiköiden oletus tulee laitteen kieliasetuksesta, kunnes käyttäjä valitsee ne. */
    val settings: Flow<AppSettings> =
        dataStore.data.map { prefs ->
            val locale = Locale.getDefault()
            AppSettings(
                weightUnit = prefs[Keys.weightUnit]?.let(WeightUnit::valueOf) ?: defaultWeightUnit(locale),
                distanceUnit = prefs[Keys.distanceUnit]?.let(DistanceUnit::valueOf) ?: defaultDistanceUnit(locale),
                symptomTrackingEnabled = prefs[Keys.symptomTracking] ?: false,
                onboardingCompleted = prefs[Keys.onboardingCompleted] ?: false,
            )
        }

    /** Sisäinen tieto: onko ilmoituslupaa jo pyydetty palautusajastinta varten (pyydetään vain kerran). */
    val notificationPermissionAsked: Flow<Boolean> = dataStore.data.map { it[Keys.notificationPermissionAsked] ?: false }

    suspend fun setNotificationPermissionAsked() {
        dataStore.edit { it[Keys.notificationPermissionAsked] = true }
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        dataStore.edit { it[Keys.weightUnit] = unit.name }
    }

    suspend fun setDistanceUnit(unit: DistanceUnit) {
        dataStore.edit { it[Keys.distanceUnit] = unit.name }
    }

    suspend fun setSymptomTrackingEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.symptomTracking] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.onboardingCompleted] = completed }
    }
}
