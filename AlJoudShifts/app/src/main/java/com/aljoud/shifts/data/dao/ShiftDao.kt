package com.aljoud.shifts.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.aljoud.shifts.data.entities.Shift
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

// نتيجة انضمام الأسماء مع الشفتات
data class ShiftWithNames(
    val id: Long,
    val employeeId: Long,
    val branchId: Long,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    val employeeName: String,
    val branchName: String
)

@Dao
interface ShiftDao {

    @Insert
    suspend fun insert(shift: Shift): Long

    @Delete
    suspend fun delete(shift: Shift)

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM shifts")
    suspend fun clearAll()

    // حذف شفتات موظف ضمن مدى تواريخ (للعُطل)
    @Query("""
    DELETE FROM shifts
    WHERE employeeId = :employeeId
      AND date BETWEEN :from AND :to
""")
    suspend fun deleteForEmployeeBetween(
        employeeId: Long,
        from: LocalDate,
        to: LocalDate
    )
    // جلب شفتات موظف ضمن مدى (مرة واحدة بدون Flow) — مفيد للتقارير
    @Query("""
    SELECT s.id, s.employeeId, s.branchId, s.date, s.start, s.end,
           e.fullName AS employeeName,
           b.name     AS branchName
    FROM shifts s
    JOIN employees e ON e.id = s.employeeId
    JOIN branches  b ON b.id = s.branchId
    WHERE s.employeeId = :employeeId AND s.date BETWEEN :from AND :to
    ORDER BY s.date ASC, s.start ASC
""")
    suspend fun listForEmployeeBetweenWithNamesOnce(
        employeeId: Long,
        from: java.time.LocalDate,
        to: java.time.LocalDate
    ): List<ShiftWithNames>

    // جلب شفتات فرع ضمن مدى (مرة واحدة بدون Flow)
    @Query("""
    SELECT s.id, s.employeeId, s.branchId, s.date, s.start, s.end,
           e.fullName AS employeeName,
           b.name     AS branchName
    FROM shifts s
    JOIN employees e ON e.id = s.employeeId
    JOIN branches  b ON b.id = s.branchId
    WHERE s.branchId = :branchId AND s.date BETWEEN :from AND :to
    ORDER BY s.date ASC, s.start ASC, e.fullName ASC
""")
    suspend fun listForBranchBetweenWithNamesOnce(
        branchId: Long,
        from: java.time.LocalDate,
        to: java.time.LocalDate
    ): List<ShiftWithNames>


    // عدد السجلات المتداخلة لنفس الموظف في نفس اليوم
    @Query("""
        SELECT COUNT(*) FROM shifts
        WHERE employeeId = :employeeId
          AND date = :date
          AND NOT (:end <= start OR :start >= end)
    """)
    suspend fun countOverlaps(
        employeeId: Long,
        date: LocalDate,
        start: LocalTime,
        end: LocalTime
    ): Int

    // أول سجل متداخل (للرسالة)
    @Query("""
        SELECT * FROM shifts
        WHERE employeeId = :employeeId
          AND date = :date
          AND NOT (:end <= start OR :start >= end)
        ORDER BY start ASC
        LIMIT 1
    """)
    suspend fun findFirstOverlap(
        employeeId: Long,
        date: LocalDate,
        start: LocalTime,
        end: LocalTime
    ): Shift?

    // شفتات ضمن نطاق + أسماء
    @Query("""
        SELECT s.id, s.employeeId, s.branchId, s.date, s.start, s.end,
               e.fullName AS employeeName,
               b.name     AS branchName
        FROM shifts s
        JOIN employees e ON e.id = s.employeeId
        JOIN branches  b ON b.id = s.branchId
        WHERE s.date BETWEEN :from AND :to
        ORDER BY s.date ASC, s.start ASC, e.fullName ASC
    """)
    fun listBetweenWithNames(from: LocalDate, to: LocalDate): Flow<List<ShiftWithNames>>

    // شفتات موظف ضمن نطاق + أسماء
    @Query("""
        SELECT s.id, s.employeeId, s.branchId, s.date, s.start, s.end,
               e.fullName AS employeeName,
               b.name     AS branchName
        FROM shifts s
        JOIN employees e ON e.id = s.employeeId
        JOIN branches  b ON b.id = s.branchId
        WHERE s.employeeId = :employeeId AND s.date BETWEEN :from AND :to
        ORDER BY s.date ASC, s.start ASC
    """)
    fun listForEmployeeBetweenWithNames(
        employeeId: Long,
        from: LocalDate,
        to: LocalDate
    ): Flow<List<ShiftWithNames>>

    // شفتات فرع ضمن نطاق + أسماء
    @Query("""
        SELECT s.id, s.employeeId, s.branchId, s.date, s.start, s.end,
               e.fullName AS employeeName,
               b.name     AS branchName
        FROM shifts s
        JOIN employees e ON e.id = s.employeeId
        JOIN branches  b ON b.id = s.branchId
        WHERE s.branchId = :branchId AND s.date BETWEEN :from AND :to
        ORDER BY s.date ASC, s.start ASC, e.fullName ASC
    """)
    fun listForBranchBetweenWithNames(
        branchId: Long,
        from: LocalDate,
        to: LocalDate
    ): Flow<List<ShiftWithNames>>
}
