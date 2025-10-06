package com.aljoud.shifts.data.dao

import androidx.room.*
import com.aljoud.shifts.data.entities.Branch
import kotlinx.coroutines.flow.Flow

@Dao
interface BranchDao {

    @Query("SELECT * FROM branches ORDER BY name ASC")
    fun list(): Flow<List<Branch>>

    @Insert(onConflict = OnConflictStrategy.IGNORE) // ← يمنع تكرار الاسم
    suspend fun insert(branch: Branch): Long        // يرجّع -1 إذا الاسم مكرر

    @Query("SELECT COUNT(*) FROM branches")
    suspend fun count(): Int

    @Query("DELETE FROM branches")
    suspend fun clear()
}
