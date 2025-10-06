package com.aljoud.shifts.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    /* LocalDate <-> String (ISO-8601) */
    @TypeConverter
    fun fromDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun dateToString(date: LocalDate?): String? = date?.toString()

    /* LocalTime <-> String (HH:mm:ss.nnn) */
    @TypeConverter
    fun fromTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    @TypeConverter
    fun timeToString(time: LocalTime?): String? = time?.toString()
}
