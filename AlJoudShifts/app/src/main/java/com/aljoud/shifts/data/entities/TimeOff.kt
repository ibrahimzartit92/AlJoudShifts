package com.aljoud.shifts.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "time_off")
data class TimeOff(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val fromDate: LocalDate,
    val toDate: LocalDate
)

