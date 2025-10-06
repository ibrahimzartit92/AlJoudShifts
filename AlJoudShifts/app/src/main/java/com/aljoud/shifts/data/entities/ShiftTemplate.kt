package com.aljoud.shifts.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(tableName = "shift_templates")
data class ShiftTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val start: LocalTime,
    val end: LocalTime
)
