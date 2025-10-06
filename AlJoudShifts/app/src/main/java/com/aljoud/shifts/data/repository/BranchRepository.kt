package com.aljoud.shifts.data.repository

import com.aljoud.shifts.data.AppDatabase
import com.aljoud.shifts.data.entities.Branch
import kotlinx.coroutines.flow.Flow

class BranchRepository(private val db: AppDatabase) {

    fun list(): Flow<List<Branch>> = db.branchDao().list()

    // يرجع true إذا انضاف، false إذا كان مكرر
    suspend fun add(name: String): Boolean {
        val id = db.branchDao().insert(Branch(name = name.trim()))
        return id != -1L
    }

    suspend fun clear() = db.branchDao().clear()

    suspend fun addSampleIfEmpty() {
        if (db.branchDao().count() == 0) {
            db.branchDao().insert(Branch(name = "BerlinerTor"))
            db.branchDao().insert(Branch(name = "Eiffestraße"))
            db.branchDao().insert(Branch(name = "Hansaplatz"))
        }
    }
}
