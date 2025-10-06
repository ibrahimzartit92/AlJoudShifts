package com.aljoud.shifts.data.entities

import androidx.room.*
import androidx.room.PrimaryKey

@Entity(
    tableName = "employees",
    foreignKeys = [
        ForeignKey(
            entity = Branch::class,
            parentColumns = ["id"],
            childColumns = ["branchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("branchId"), Index("fullName")]
)
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val phoneE164: String, // مثل 9639XXXXXXXX
    val branchId: Long
)
