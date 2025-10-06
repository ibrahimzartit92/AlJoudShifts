package com.aljoud.shifts.data.dao

import androidx.room.*
import com.aljoud.shifts.data.entities.Employee
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    @Query("SELECT * FROM employees ORDER BY fullName ASC")
    fun list(): Flow<List<Employee>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employee: Employee): Long

    @Delete
    suspend fun delete(employee: Employee)

    @Query("DELETE FROM employees")
    suspend fun clear()

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Employee?

    // ✅ رقم الهاتف كـ Flow (استخدم phoneE164)
    @Query("SELECT phoneE164 FROM employees WHERE id = :id")
    fun getPhoneFlow(id: Long): Flow<String>

    // ✅ تحديث رقم الهاتف (استخدم phoneE164)
    @Query("UPDATE employees SET phoneE164 = :newPhone WHERE id = :id")
    suspend fun updatePhone(id: Long, newPhone: String)

    // ✅ تحديث الفرع
    @Query("UPDATE employees SET branchId = :branchId WHERE id = :id")
    suspend fun updateBranch(id: Long, branchId: Long)
}
