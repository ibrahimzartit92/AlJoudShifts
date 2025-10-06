package com.aljoud.shifts.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "branches",
    indices = [Index(value = ["name"], unique = true)] // ← فهرس فريد
)
data class Branch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
