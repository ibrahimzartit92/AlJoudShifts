package com.aljoud.shifts.data.dao

import androidx.room.*
import com.aljoud.shifts.data.entities.ShiftTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftTemplateDao {
    @Query("SELECT * FROM shift_templates ORDER BY name ASC")
    fun list(): Flow<List<ShiftTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: ShiftTemplate): Long

    @Delete
    suspend fun delete(t: ShiftTemplate)

    @Query("DELETE FROM shift_templates")
    suspend fun clear()
}
