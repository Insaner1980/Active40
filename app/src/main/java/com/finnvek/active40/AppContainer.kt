package com.finnvek.active40

import android.content.Context
import com.finnvek.active40.data.BuiltInData
import com.finnvek.active40.data.CheckInRepository
import com.finnvek.active40.data.ExerciseRepository
import com.finnvek.active40.data.RoutineRepository
import com.finnvek.active40.data.RunRepository
import com.finnvek.active40.data.SettingsRepository
import com.finnvek.active40.data.StrengthRepository
import com.finnvek.active40.data.SymptomRepository
import com.finnvek.active40.data.backup.BackupRepository
import com.finnvek.active40.data.db.Active40Database
import com.finnvek.active40.timer.RestTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Sovelluksen riippuvuudet käsin rakennettuina. Yksi instanssi, joka elää Applicationin mukana. */
class AppContainer(
    context: Context,
) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database = Active40Database.build(context)

    val settingsRepository = SettingsRepository(context)
    val exerciseRepository = ExerciseRepository(database)
    val symptomRepository = SymptomRepository(database)
    val routineRepository = RoutineRepository(database)
    val strengthRepository = StrengthRepository(database, routineRepository)
    val runRepository = RunRepository(database)
    val checkInRepository = CheckInRepository(database)

    val backupRepository = BackupRepository(context, database, settingsRepository)

    val restTimer = RestTimer(context, applicationScope)

    init {
        restTimer.createChannel()
        applicationScope.launch { BuiltInData.seed(database) }
    }
}
