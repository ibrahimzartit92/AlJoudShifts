package com.aljoud.shifts.data.entities

import androidx.room.Embedded
import androidx.room.Relation

data class EmployeeWithBranch(
    @Embedded val employee: Employee,
    @Relation(
        parentColumn = "branchId",
        entityColumn = "id"
    )
    val branch: Branch?
)
