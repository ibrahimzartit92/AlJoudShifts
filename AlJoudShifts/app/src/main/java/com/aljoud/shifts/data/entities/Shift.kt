package com.aljoud.shifts.data.entities

import androidx.room.*
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "shifts",
    foreignKeys = [
        ForeignKey(
            entity = Employee::class,
            parentColumns = ["id"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Branch::class,
            parentColumns = ["id"],
            childColumns = ["branchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("employeeId"), Index("branchId"), Index("date")]
)
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val branchId: Long,          // ⬅️ جديد
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime
)
