package com.aljoud.shifts.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aljoud.shifts.data.AppDatabase
import com.aljoud.shifts.data.dao.ShiftWithNames
import com.aljoud.shifts.data.entities.Shift
import com.aljoud.shifts.data.repository.ShiftRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ShiftsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ShiftRepository(AppDatabase.get(app))

    // أسبوع يبدأ الاثنين
    private val _weekStart = MutableStateFlow(LocalDate.now().with(DayOfWeek.MONDAY))
    val weekStart: StateFlow<LocalDate> = _weekStart.asStateFlow()
    val weekEnd: StateFlow<LocalDate> =
        _weekStart.map { it.plusDays(6) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, LocalDate.now())

    // الشفتات مع الأسماء
    val shifts: StateFlow<List<ShiftWithNames>> =
        _weekStart
            .flatMapLatest { from -> repo.listBetween(from, from.plusDays(6)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun nextWeek() { _weekStart.value = _weekStart.value.plusWeeks(1) }
    fun prevWeek() { _weekStart.value = _weekStart.value.minusWeeks(1) }

    // ==== أحداث واجهة (Snackbars) ====
    sealed interface UiEvent {
        data class Success(val msg: String): UiEvent
        data class Conflict(val msg: String): UiEvent
        data class Error(val msg: String): UiEvent
    }
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    // إضافة شيفت مع فحص تعارض
    fun addShift(employeeId: Long, branchId: Long, date: LocalDate, start: LocalTime, end: LocalTime) {
        viewModelScope.launch {
            if (!end.isAfter(start)) {
                _events.emit(UiEvent.Error("وقت النهاية يجب أن يكون بعد البداية"))
                return@launch
            }
            // فحص تداخل لنفس الموظف
            if (repo.hasOverlap(employeeId, date, start, end)) {
                val overlap = repo.firstOverlap(employeeId, date, start, end)
                val info = overlap?.let { "تداخل مع شفت من ${it.start} إلى ${it.end}" } ?: ""
                _events.emit(UiEvent.Conflict("هناك تداخل في الوقت لنفس الموظف. $info"))
                return@launch
            }

            repo.add(
                Shift(
                    employeeId = employeeId,
                    branchId = branchId,
                    date = date,
                    start = start,
                    end = end
                )
            )
            _events.emit(UiEvent.Success("تم تخصيص الشفت بنجاح"))
        }
    }

    fun deleteShift(id: Long) = viewModelScope.launch { repo.deleteById(id) }
    fun clearAll() = viewModelScope.launch { repo.clear() }
}
