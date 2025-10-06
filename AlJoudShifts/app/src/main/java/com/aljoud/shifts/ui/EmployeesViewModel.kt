package com.aljoud.shifts.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aljoud.shifts.data.AppDatabase
import com.aljoud.shifts.data.entities.Employee
import com.aljoud.shifts.data.repository.EmployeesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EmployeesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = EmployeesRepository(AppDatabase.get(app))

    // تدفّق قائمة الموظفين
    val employees = repo.list()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(fullName: String, phoneE164: String, branchId: Long) {
        viewModelScope.launch {
            // نبني كائن Employee ونمرّره للريبو (الريبو يتوقع Employee)
            val emp = Employee(
                id = 0L,                // يترك لـ Room توليد المعرّف
                fullName = fullName,
                phoneE164 = phoneE164,  // تأكد أن الحقل في الـ Entity اسمه phoneE164
                branchId = branchId
            )
            repo.add(emp)
        }
    }

    fun clear() {
        viewModelScope.launch { repo.clear() }
    }

    // تحديث رقم الهاتف (إن احتجته من شاشة التفاصيل)
    fun updatePhone(id: Long, newPhoneE164: String) {
        viewModelScope.launch { repo.updatePhone(id, newPhoneE164) }
    }
}
