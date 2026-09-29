package com.finnvek.active40.data.backup

import com.finnvek.active40.data.db.BackupData
import com.finnvek.active40.data.db.CheckIn
import com.finnvek.active40.data.db.CheckInSymptom
import com.finnvek.active40.data.db.Exercise
import com.finnvek.active40.data.db.Routine
import com.finnvek.active40.data.db.RoutineExercise
import com.finnvek.active40.data.db.Run
import com.finnvek.active40.data.db.SessionExercise
import com.finnvek.active40.data.db.SetEntry
import com.finnvek.active40.data.db.StrengthSession
import com.finnvek.active40.data.db.Symptom
import com.finnvek.active40.domain.DistanceUnit
import com.finnvek.active40.domain.RunType
import com.finnvek.active40.domain.SorenessArea
import com.finnvek.active40.domain.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BackupFormatTest {
    private val t0 = Instant.ofEpochMilli(1_790_000_000_000)
    private val day = LocalDate.of(2026, 9, 22)

    private val data =
        BackupData(
            exercises = listOf(Exercise(1, "Leg press", true, false, t0), Exercise(2, "Nordic curl", false, true, t0)),
            symptoms = listOf(Symptom(1, "Night sweats", true, false)),
            routines = listOf(Routine(1, "Lower body A", t0, 0)),
            routineExercises = listOf(RoutineExercise(1, 1, 1, 0, 3, 10)),
            sessions = listOf(StrengthSession(1, day, t0, t0.plusSeconds(2700), 1, null, "Felt good", 7, 90)),
            sessionExercises = listOf(SessionExercise(1, 1, 1, 0)),
            sets = listOf(SetEntry(1, 1, 0, 82.5, 10, 8, true)),
            runs = listOf(Run(1, day, 5240.0, 1880, RunType.TEMPO, 6, null, t0)),
            checkIns = listOf(CheckIn(1, day, null, 2, setOf(SorenessArea.LEGS), null)),
            checkInSymptoms = listOf(CheckInSymptom(1, 1)),
        )

    private fun encode(d: BackupData = data) =
        backupJson.encodeToString(BackupFile.serializer(), d.toFile(t0, WeightUnit.LB, DistanceUnit.MI, true))

    @Test
    fun `export and import round trip keeps everything`() {
        val parsed = parseBackup(encode())
        assertEquals(data, parsed.data)
        assertEquals(WeightUnit.LB, parsed.weightUnit)
        assertEquals(DistanceUnit.MI, parsed.distanceUnit)
        assertTrue(parsed.symptomTrackingEnabled)
        assertEquals(t0, parsed.exportedAt)
    }

    @Test
    fun `file contains format version`() {
        assertTrue(encode().contains("\"formatVersion\": $BACKUP_FORMAT_VERSION"))
    }

    private fun assertError(
        expected: BackupError,
        text: String,
    ) {
        try {
            parseBackup(text)
            fail("Expected $expected")
        } catch (e: BackupException) {
            assertEquals(expected, e.error)
        }
    }

    @Test
    fun `rejects unreadable newer and broken files`() {
        assertError(BackupError.UNREADABLE, "not json")
        assertError(BackupError.NEWER_VERSION, encode().replace("\"formatVersion\": 1", "\"formatVersion\": 99"))
        assertError(BackupError.INVALID_DATA, encode().replace("\"TEMPO\"", "\"SPRINT\""))
    }

    @Test
    fun `consistency catches broken references and ranges`() {
        assertTrue(isConsistent(data))
        assertFalse(isConsistent(data.copy(sets = listOf(SetEntry(1, 99, 0, 80.0, 10, null, true)))))
        assertFalse(isConsistent(data.copy(checkIns = listOf(CheckIn(1, day, 7, null, null, null)))))
        assertFalse(isConsistent(data.copy(runs = listOf(Run(1, day, 0.0, 100, RunType.EASY, null, null, t0)))))
        val twoActive = listOf(StrengthSession(1, day, t0), StrengthSession(2, day, t0))
        assertFalse(isConsistent(data.copy(sessions = twoActive, sessionExercises = emptyList(), sets = emptyList())))
    }
}
