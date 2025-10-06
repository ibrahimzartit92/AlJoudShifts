package com.aljoud.shifts.data.dao

import androidx.room.*
import com.aljoud.shifts.data.entities.TimeOff
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TimeOffDao {

    @Query("SELECT * FROM time_off ORDER BY fromDate DESC")
    fun list(): Flow<List<TimeOff>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timeOff: TimeOff)

    @Query("DELETE FROM time_off")
    suspend fun clear()

    @Query("DELETE FROM time_off WHERE employeeId = :employeeId")
    suspend fun deleteForEmployee(employeeId: Long)

    // هل هذا اليوم ضمن عطلة؟
    @Query("""
        SELECT COUNT(*) FROM time_off
        WHERE employeeId = :employeeId
          AND :date BETWEEN fromDate AND toDate
    """)
    suspend fun isDayOff(employeeId: Long, date: LocalDate): Int
}
