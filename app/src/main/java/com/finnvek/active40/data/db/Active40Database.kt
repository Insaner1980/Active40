package com.finnvek.active40.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Exercise::class,
        Routine::class,
        RoutineExercise::class,
        StrengthSession::class,
        SessionExercise::class,
        SetEntry::class,
        Run::class,
        CheckIn::class,
        Symptom::class,
        CheckInSymptom::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class Active40Database : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

    abstract fun symptomDao(): SymptomDao

    abstract fun routineDao(): RoutineDao

    abstract fun strengthDao(): StrengthDao

    abstract fun runDao(): RunDao

    abstract fun checkInDao(): CheckInDao

    abstract fun backupDao(): BackupDao

    companion object {
        fun build(context: Context): Active40Database = Room.databaseBuilder(context, Active40Database::class.java, "active40.db").build()
    }
}
