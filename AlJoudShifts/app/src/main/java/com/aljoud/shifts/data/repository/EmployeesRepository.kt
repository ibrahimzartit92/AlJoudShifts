package com.aljoud.shifts.data.repository

import com.aljoud.shifts.data.AppDatabase
import com.aljoud.shifts.data.entities.Employee
import kotlinx.coroutines.flow.Flow

class EmployeesRepository(private val db: AppDatabase) {

    private val dao = db.employeeDao()

    fun list() = dao.list()
    suspend fun add(employee: Employee) = dao.insert(employee)
    suspend fun clear() = dao.clear()

    fun getPhoneFlow(id: Long) = dao.getPhoneFlow(id)
    suspend fun updatePhone(id: Long, newPhoneE164: String) = dao.updatePhone(id, newPhoneE164)
}
