package com.aljoud.shifts.data.repository

import androidx.room.withTransaction
import com.aljoud.shifts.data.AppDatabase
import com.aljoud.shifts.data.dao.ShiftWithNames
import com.aljoud.shifts.data.entities.Shift
import com.aljoud.shifts.data.entities.TimeOff
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

class ShiftRepository(private val database: AppDatabase) {

    private val shiftDao = database.shiftDao()
    private val timeOffDao = database.timeOffDao()

    suspend fun add(shift: Shift): Long = shiftDao.insert(shift)
    suspend fun delete(shift: Shift) = shiftDao.delete(shift)
    suspend fun deleteById(id: Long) = shiftDao.deleteById(id)
    suspend fun clear() = shiftDao.clearAll()

    fun listBetween(from: LocalDate, to: LocalDate): Flow<List<ShiftWithNames>> =
        shiftDao.listBetweenWithNames(from, to)

    fun listForEmployeeBetween(
        employeeId: Long,
        from: LocalDate,
        to: LocalDate
    ): Flow<List<ShiftWithNames>> =
        shiftDao.listForEmployeeBetweenWithNames(employeeId, from, to)

    fun listForBranchBetween(
        branchId: Long,
        from: LocalDate,
        to: LocalDate
    ): Flow<List<ShiftWithNames>> =
        shiftDao.listForBranchBetweenWithNames(branchId, from, to)

    suspend fun hasOverlap(
        employeeId: Long,
        date: LocalDate,
        start: LocalTime,
        end: LocalTime
    ): Boolean = shiftDao.countOverlaps(employeeId, date, start, end) > 0

    suspend fun firstOverlap(
        employeeId: Long,
        date: LocalDate,
        start: LocalTime,
        end: LocalTime
    ): Shift? = shiftDao.findFirstOverlap(employeeId, date, start, end)

    // حذف شفتات موظف ضمن مدى تواريخ (تموضعي فقط)
    suspend fun deleteForEmployeeBetween(
        employeeId: Long,
        from: LocalDate,
        to: LocalDate
    ) {
        shiftDao.deleteForEmployeeBetween(employeeId, from, to)
    }

    suspend fun setTimeOffAndPurgeShifts(
        employeeId: Long,
        from: LocalDate,
        to: LocalDate
    ) {
        database.withTransaction {
            timeOffDao.insert(
                TimeOff(
                    employeeId = employeeId,
                    fromDate = from,
                    toDate = to
                )
            )
            shiftDao.deleteForEmployeeBetween(employeeId, from, to)
        }
    }


    suspend fun isDayOff(employeeId: Long, date: LocalDate): Boolean =
        timeOffDao.isDayOff(employeeId, date) > 0
    suspend fun getEmployeeShiftsBetweenOnce(
        employeeId: Long,
        from: java.time.LocalDate,
        to: java.time.LocalDate
    ): List<ShiftWithNames> = shiftDao.listForEmployeeBetweenWithNamesOnce(employeeId, from, to)

    suspend fun getBranchShiftsBetweenOnce(
        branchId: Long,
        from: java.time.LocalDate,
        to: java.time.LocalDate
    ): List<ShiftWithNames> = shiftDao.listForBranchBetweenWithNamesOnce(branchId, from, to)

}
