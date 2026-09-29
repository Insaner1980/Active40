package com.finnvek.active40.data.db

import androidx.room.TypeConverter
import com.finnvek.active40.domain.RunType
import com.finnvek.active40.domain.SorenessArea
import java.time.Instant
import java.time.LocalDate

class Converters {
    // ISO-muoto (yyyy-MM-dd) lajittuu oikein merkkijonona, joten aikavälikyselyt toimivat.
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun runTypeToString(type: RunType): String = type.name

    @TypeConverter
    fun stringToRunType(value: String): RunType = RunType.valueOf(value)

    @TypeConverter
    fun sorenessToString(areas: Set<SorenessArea>?): String? = areas?.joinToString(",") { it.name }

    @TypeConverter
    fun stringToSoreness(value: String?): Set<SorenessArea>? =
        value
            ?.split(",")
            ?.filter { it.isNotEmpty() }
            ?.map(SorenessArea::valueOf)
            ?.toSet()
}
