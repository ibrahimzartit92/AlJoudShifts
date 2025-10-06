package com.aljoud.shifts.data.repository

import com.aljoud.shifts.data.AppDatabase
import com.aljoud.shifts.data.entities.TimeOff
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TimeOffRepository(private val db: AppDatabase) {

    private val dao = db.timeOffDao()

    fun list(): Flow<List<TimeOff>> = dao.list()
    suspend fun add(t: TimeOff) = dao.insert(t)
    suspend fun clear() = dao.clear()
    suspend fun deleteForEmployee(employeeId: Long) = dao.deleteForEmployee(employeeId)

    suspend fun isDayOff(employeeId: Long, date: LocalDate): Boolean =
        dao.isDayOff(employeeId, date) > 0
}
