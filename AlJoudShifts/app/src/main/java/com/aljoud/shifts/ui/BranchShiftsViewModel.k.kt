package com.aljoud.shifts.ui

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aljoud.shifts.data.AppDatabase
import com.aljoud.shifts.data.dao.ShiftWithNames
import com.aljoud.shifts.data.entities.Shift
import com.aljoud.shifts.data.repository.ShiftRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*

class BranchShiftsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ShiftRepository(AppDatabase.get(app))

    private val _branchId = MutableStateFlow<Long?>(null)
    fun setBranch(id: Long) { _branchId.value = id }

    private val _weekStart = MutableStateFlow(LocalDate.now().with(DayOfWeek.MONDAY))
    val weekStart: StateFlow<LocalDate> = _weekStart.asStateFlow()
    val weekEnd: StateFlow<LocalDate> =
        _weekStart.map { it.plusDays(6) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, LocalDate.now())

    val shifts: StateFlow<List<ShiftWithNames>> =
        combine(_branchId, _weekStart) { bid, from -> bid to from }
            .flatMapLatest { (bid, from) ->
                if (bid == null) flowOf(emptyList())
                else repo.listForBranchBetween(bid, from, from.plusDays(6))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun nextWeek() { _weekStart.value = _weekStart.value.plusWeeks(1) }
    fun prevWeek() { _weekStart.value = _weekStart.value.minusWeeks(1) }

    // أحداث UI (SnackBar)
    sealed interface UiEvent { data class Info(val msg: String) : UiEvent }
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    // ←ـــــــــــــــــــــــ الدالة اللي سببت الخطأ: صارت داخل الكلاس
    @RequiresApi(Build.VERSION_CODES.O)
    fun exportMonthlyPdfForBranch(
        context: Context,
        branchId: Long,
        branchName: String,
        month: YearMonth
    ) {
        viewModelScope.launch {
            val from = month.atDay(1)
            val to = month.atEndOfMonth()
            val data = repo.getBranchShiftsBetweenOnce(branchId, from, to)
            com.aljoud.shifts.util.PdfReportGenerator
                .exportMonthlyBranch(context, branchName, month, data)
            _events.emit(UiEvent.Info("تم إنشاء تقرير PDF للفرع في مجلد التنزيلات"))
        }
    }

    // إضافة شفت واحد لعدة موظفين على مدى تواريخ
    fun addShiftForEmployeesRange(
        branchId: Long,
        from: LocalDate,
        to: LocalDate,
        start: LocalTime,
        end: LocalTime,
        employeeIds: List<Long>
    ) {
        viewModelScope.launch {
            if (!end.isAfter(start)) {
                _events.emit(UiEvent.Info("وقت النهاية يجب أن يكون بعد البداية"))
                return@launch
            }
            var ok = 0
            var conflicts = 0
            var offs = 0

            var d = from
            while (!d.isAfter(to)) {
                for (eid in employeeIds) {
                    if (repo.isDayOff(eid, d)) { offs++; continue }
                    if (repo.hasOverlap(eid, d, start, end)) { conflicts++; continue }
                    repo.add(Shift(employeeId = eid, branchId = branchId, date = d, start = start, end = end))
                    ok++
                }
                d = d.plusDays(1)
            }

            val msg = buildString {
                append("أُضيفت $ok شفت/شفتات.")
                if (conflicts > 0) append(" تعارض: $conflicts.")
                if (offs > 0) append(" عطلة: $offs.")
            }
            _events.emit(UiEvent.Info(msg))
        }
    }

    // تعيين عطلة لمجموعة موظفين مع حذف شفتاتهم ضمن المدى
    fun setTimeOffForEmployees(from: LocalDate, to: LocalDate, employeeIds: List<Long>) {
        viewModelScope.launch {
            var done = 0
            for (eid in employeeIds) {
                repo.setTimeOffAndPurgeShifts(eid, from, to)
                done++
            }
            _events.emit(UiEvent.Info("تم ضبط عطلة لعدد $done موظف/موظفين وحذف الشفتات ضمن المدى"))
        }
    }

    fun deleteShift(id: Long) = viewModelScope.launch { repo.deleteById(id) }
}
