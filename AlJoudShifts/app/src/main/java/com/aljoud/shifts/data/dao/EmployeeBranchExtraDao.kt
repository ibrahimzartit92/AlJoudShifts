package com.aljoud.shifts.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aljoud.shifts.data.entities.EmployeeBranchExtra
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeBranchExtraDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(employeeBranch: EmployeeBranchExtra)

    @Query("DELETE FROM employee_branch_extra WHERE employeeId = :employeeId AND branchId = :branchId")
    suspend fun remove(employeeId: Long, branchId: Long)

    @Query("""
        SELECT b.id, b.name 
        FROM employee_branch_extra ebe
        JOIN branches b ON b.id = ebe.branchId
        WHERE ebe.employeeId = :employeeId
        ORDER BY b.name ASC
    """)
    fun branchesForEmployee(employeeId: Long): Flow<List<BranchLite>>
}

data class BranchLite(
    val id: Long,
    val name: String
)
