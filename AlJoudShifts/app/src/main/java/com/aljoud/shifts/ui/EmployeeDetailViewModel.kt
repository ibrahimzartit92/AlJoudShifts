package com.aljoud.shifts.ui

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aljoud.shifts.data.AppDatabase
import com.aljoud.shifts.data.entities.Shift
import com.aljoud.shifts.data.repository.BranchRepository
import com.aljoud.shifts.data.repository.EmployeesRepository
import com.aljoud.shifts.data.repository.ShiftRepository
import com.aljoud.shifts.data.repository.TimeOffRepository
import com.aljoud.shifts.util.PdfReportGenerator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import com.aljoud.shifts.util.PdfReportGenerator


class EmployeeDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val employeesRepo = EmployeesRepository(db)
    private val shiftsRepo    = ShiftRepository(db)
    private val branchesRepo  = BranchRepository(db)
    private val timeOffRepo   = TimeOffRepository(db) // قد نستخدمه لاحقاً

    private val _employeeId = MutableStateFlow<Long?>(null)

    // أسبوع افتراضي: الإثنين → الأحد
    private val _weekStart = MutableStateFlow(LocalDate.now().with(DayOfWeek.MONDAY))
    val weekStart: StateFlow<LocalDate> = _weekStart.asStateFlow()
    val weekEnd: StateFlow<LocalDate> =
        _weekStart.map { it.plusDays(6) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, LocalDate.now())

    // بيانات المعروض
    val phone: StateFlow<String> =
        _employeeId.flatMapLatest { id ->
            if (id == null) flowOf("") else employeesRepo.getPhoneFlow(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val shifts =
        _employeeId.filterNotNull().flatMapLatest { id ->
            weekStart.flatMapLatest { from ->
                shiftsRepo.listForEmployeeBetween(id, from, from.plusDays(6))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val branches = branchesRepo.list()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // أحداث واجهة
    sealed interface UiEvent {
        data class Info(val msg: String) : UiEvent
        data class Error(val msg: String) : UiEvent
    }
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setEmployee(id: Long) { _employeeId.value = id }

    fun nextWeek() { _weekStart.value = _weekStart.value.plusWeeks(1) }
    fun prevWeek() { _weekStart.value = _weekStart.value.minusWeeks(1) }

    fun updatePhone(newPhone: String, onDone: () -> Unit) {
        val id = _employeeId.value ?: return
        viewModelScope.launch {
            try {
                // رقم ألماني بصيغة E.164 بدون +
                val ok = newPhone.matches(Regex("""^49\d{7,13}$"""))
                if (!ok) {
                    _events.send(UiEvent.Error("صيغة الرقم غير صحيحة"))
                } else {
                    employeesRepo.updatePhone(id, newPhone)
                    _events.send(UiEvent.Info("تم حفظ الرقم"))
                }
            } catch (t: Throwable) {
                _events.send(UiEvent.Error("فشل حفظ الرقم"))
            } finally {
                onDone()
            }
        }
    }

    fun addShiftRangeForEmployee(
        from: LocalDate,
        to: LocalDate,
        start: LocalTime,
        end: LocalTime,
        branchId: Long
    ) {
        val id = _employeeId.value ?: return
        viewModelScope.launch {
            var added = 0
            var skipped = 0
            var conflictMsg: String? = null
            var d = from
            while (!d.isAfter(to)) {
                // عطلة؟
                if (timeOffRepo.isDayOff(id, d)) {
                    skipped++
                } else if (shiftsRepo.hasOverlap(id, d, start, end)) {
                    // تداخل؟
                    if (conflictMsg == null) {
                        val ov = shiftsRepo.firstOverlap(id, d, start, end)
                        conflictMsg = "تداخل شفت بتاريخ $d (من ${ov?.start} إلى ${ov?.end})"
                    }
                    skipped++
                } else {
                    shiftsRepo.add(
                        Shift(
                            employeeId = id,
                            branchId = branchId,
                            date = d,
                            start = start,
                            end = end
                        )
                    )
                    added++
                }
                d = d.plusDays(1)
            }
            if (added > 0) _events.send(UiEvent.Info("تمت إضافة $added شفت"))
            if (skipped > 0) _events.send(UiEvent.Info("تمّ تجاوز $skipped يوم (عطلة/تداخل)"))
            conflictMsg?.let { _events.send(UiEvent.Error(it)) }
        }
    }

    fun setTimeOff(from: LocalDate, to: LocalDate) {
        val id = _employeeId.value ?: return
        viewModelScope.launch {
            // العملية الذرّية: تسجّل عطلة وتحذف الشفتات ضمن المدى
            shiftsRepo.setTimeOffAndPurgeShifts(id, from, to)
            _events.send(UiEvent.Info("تم تعيين عطلة وحذف الشفتات ضمن المدى"))
        }
    }

    // تصدير تقرير شهري PDF لهذا الموظف
    @RequiresApi(Build.VERSION_CODES.O)
    fun exportMonthlyPdf(context: Context, month: YearMonth) {
        val id = _employeeId.value ?: return
        viewModelScope.launch {
            try {
                val from = month.atDay(1)
                val to = month.atEndOfMonth()
                val data = shiftsRepo.getEmployeeShiftsBetweenOnce(id, from, to)
                val name = data.firstOrNull()?.employeeName ?: "الموظف"
                PdfReportGenerator.exportMonthlyEmployee(context, name, month, data)
                _events.send(UiEvent.Info("تم إنشاء تقرير PDF في مجلد التنزيلات"))
            } catch (t: Throwable) {
                _events.send(UiEvent.Error("فشل إنشاء تقرير PDF: ${t.message ?: "خطأ غير معروف"}"))
            }
        }
    }
}
