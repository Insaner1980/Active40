package com.finnvek.active40.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.finnvek.active40.data.BuiltInData
import com.finnvek.active40.data.SettingsRepository
import com.finnvek.active40.data.db.Active40Database
import com.finnvek.active40.data.db.BackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant

/** Vienti, tuonti ja kaiken datan poisto. Tiedostot kulkevat järjestelmän tiedostovalitsimen kautta (ei tallennuslupaa). */
class BackupRepository(
    private val context: Context,
    private val db: Active40Database,
    private val settingsRepository: SettingsRepository,
) {
    private val dao = db.backupDao()

    private suspend fun readAll(): BackupData =
        db.withTransaction {
            BackupData(
                exercises = dao.exercises(),
                symptoms = dao.symptoms(),
                routines = dao.routines(),
                routineExercises = dao.routineExercises(),
                sessions = dao.sessions(),
                sessionExercises = dao.sessionExercises(),
                sets = dao.sets(),
                runs = dao.runs(),
                checkIns = dao.checkIns(),
                checkInSymptoms = dao.checkInSymptoms(),
            )
        }

    /** Kirjoittaa kaiken datan ja asetukset JSON-tiedostoon. */
    suspend fun export(uri: Uri) {
        val settings = settingsRepository.settings.first()
        val file = readAll().toFile(Instant.now(), settings.weightUnit, settings.distanceUnit, settings.symptomTrackingEnabled)
        val text = backupJson.encodeToString(BackupFile.serializer(), file)
        withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
    }

    /** Lukee ja tarkistaa tiedoston muuttamatta mitään. Heittää BackupExceptionin, jos tiedostoa ei voi tuoda. */
    suspend fun read(uri: Uri): ParsedBackup {
        val text =
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                } catch (_: IOException) {
                    null
                }
            } ?: throw BackupException(BackupError.UNREADABLE)
        return withContext(Dispatchers.Default) { parseBackup(text) }
    }

    /** Korvaa kaiken nykyisen datan ja asetukset tuodulla. */
    suspend fun import(backup: ParsedBackup) {
        db.withTransaction {
            dao.replaceAll(backup.data)
            // Tiedostosta voi puuttua sisäänrakennettuja nimiä (esim. vanhempi versio); lisätään puuttuvat.
            BuiltInData.seed(db)
        }
        settingsRepository.setWeightUnit(backup.weightUnit)
        settingsRepository.setDistanceUnit(backup.distanceUnit)
        settingsRepository.setSymptomTrackingEnabled(backup.symptomTrackingEnabled)
    }

    /** Poistaa kaikki kirjaukset, rutiinit ja omat liikkeet ja oireet. Asetukset säilyvät. */
    suspend fun deleteAll() {
        db.withTransaction {
            dao.deleteAll()
            BuiltInData.seed(db)
        }
    }
}
