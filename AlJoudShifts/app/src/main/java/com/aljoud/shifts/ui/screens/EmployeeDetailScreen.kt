@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.aljoud.shifts.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aljoud.shifts.data.dao.ShiftWithNames
import com.aljoud.shifts.ui.EmployeeDetailViewModel
import com.aljoud.shifts.ui.ShiftTemplatesViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EmployeeDetailScreen(
    employeeId: Long,
    employeeName: String,
    vm: EmployeeDetailViewModel = viewModel(),
    templatesVM: ShiftTemplatesViewModel = viewModel()
) {
    LaunchedEffect(employeeId) { vm.setEmployee(employeeId) }
    LaunchedEffect(Unit) { templatesVM.seedDefaultsIfEmpty() }

    val ar = Locale("ar")
    val weekRangeFmt = remember { DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", ar) }
    val dayHeaderFmt = remember { DateTimeFormatter.ofPattern("EEEE d MMMM", ar) }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm", ar) }

    val weekStart = vm.weekStart.collectAsStateWithLifecycle()
    val weekEnd   = vm.weekEnd.collectAsStateWithLifecycle()
    val shifts    = vm.shifts.collectAsStateWithLifecycle()
    val phone     = vm.phone.collectAsStateWithLifecycle()
    val branches  = vm.branches.collectAsStateWithLifecycle()
    val templates = templatesVM.templates.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is EmployeeDetailViewModel.UiEvent.Info  -> snackbar.showSnackbar(ev.msg)
                is EmployeeDetailViewModel.UiEvent.Error -> snackbar.showSnackbar(ev.msg)
            }
        }
    }

    var editingPhone by remember { mutableStateOf(false) }
    var phoneDraft by remember { mutableStateOf("") }

    var toDelete by remember { mutableStateOf<ShiftWithNames?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var showTimeOff by remember { mutableStateOf(false) }
    var fabMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الموظف: $employeeName") },
                actions = {
                    TextButton(onClick = { vm.prevWeek() }) { Text("السابق") }
                    TextButton(onClick = { vm.nextWeek() }) { Text("التالي") }
                    val ctx = LocalContext.current
                    IconButton(onClick = {
                        val ym = java.time.YearMonth.now() // تصدير الشهر الحالي
                        vm.exportMonthlyPdf(ctx, ym)
                    }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "تصدير PDF")
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { fabMenuExpanded = !fabMenuExpanded }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
                DropdownMenu(
                    expanded = fabMenuExpanded,
                    onDismissRequest = { fabMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("إضافة شِفت") },
                        onClick = { fabMenuExpanded = false; showAdd = true }
                    )
                    DropdownMenuItem(
                        text = { Text("تعيين عطلة") },
                        onClick = { fabMenuExpanded = false; showTimeOff = true }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .padding(12.dp)
                .fillMaxSize()
        ) {
            // الهاتف
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = if (editingPhone) phoneDraft else phone.value,
                    onValueChange = { phoneDraft = it },
                    label = { Text("رقم الهاتف (E.164 بدون +)") },
                    modifier = Modifier.weight(1f),
                    enabled = editingPhone,
                    singleLine = true
                )
                if (!editingPhone) {
                    TextButton(onClick = {
                        phoneDraft = phone.value
                        editingPhone = true
                    }) { Text("تعديل") }
                } else {
                    TextButton(onClick = {
                        vm.updatePhone(phoneDraft) { editingPhone = false }
                    }) { Text("حفظ") }
                }
            }

            Spacer(Modifier.height(12.dp))

            // عنوان الأسبوع — ذهبي
            Text(
                "${weekStart.value.format(weekRangeFmt)} → ${weekEnd.value.format(weekRangeFmt)}",
                color = Color(0xFFFFD700)
            )
            Spacer(Modifier.height(8.dp))

            // الإثنين → الأحد
            val startMonday = remember(weekStart.value) { weekStart.value.with(DayOfWeek.MONDAY) }
            val daysOfWeek = remember(startMonday) { (0..6).map { startMonday.plusDays(it.toLong()) } }

            // تجميع حسب اليوم
            val shiftsByDate = remember(shifts.value) {
                shifts.value
                    .sortedWith(compareBy<ShiftWithNames>({ it.date }, { it.start }, { it.branchName }))
                    .groupBy { it.date }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                daysOfWeek.forEach { day ->
                    item(key = "header_$day") {
                        Text(
                            text = day.format(dayHeaderFmt),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFFD700)
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    val dayShifts = shiftsByDate[day].orEmpty()
                    if (dayShifts.isEmpty()) {
                        item(key = "empty_$day") {
                            Text(
                                "— لا شِفتات —",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Divider()
                        }
                    } else {
                        items(dayShifts, key = { it.id }) { s ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { /* تفاصيل لاحقًا */ },
                                        onLongClick = { toDelete = s }
                                    )
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    s.branchName, // فرع الشِفت
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "${s.start.format(timeFmt)} – ${s.end.format(timeFmt)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Divider()
                        }
                    }
                }
            }
        }
    }

    // إضافة شفت (مدى تواريخ) للموظف الواحد + اختيار قالب + اختيار فرع
    if (showAdd) {
        AddEmployeeShiftRangeDialog(
            branches = branches.value.map { it.id to it.name },
            templates = templates.value.map { Triple(it.id, it.name, it.start to it.end) },
            onDismiss = { showAdd = false },
            onConfirm = { from, to, start, end, branchId ->
                vm.addShiftRangeForEmployee(from, to, start, end, branchId)
                showAdd = false
            }
        )
    }

    // تعيين عطلة لهذا الموظف
    if (showTimeOff) {
        TimeOffSingleDialog(
            onDismiss = { showTimeOff = false },
            onConfirm = { from, to ->
                vm.setTimeOff(from, to)
                showTimeOff = false
            }
        )
    }

    // تأكيد حذف شفت (اختياري تكملته لاحقًا)
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("حذف الشِفت") },
            text = { Text("هل تريد حذف هذا الشِفت؟") },
            confirmButton = {
                TextButton(onClick = {
                    // vm.deleteShiftById(toDelete!!.id) // إذا عملت دالة لاحقًا
                    toDelete = null
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("إلغاء") } }
        )
    }
}

/* ===================== الحوارات كما هي ===================== */

@Composable
private fun AddEmployeeShiftRangeDialog(
    branches: List<Pair<Long, String>>,
    templates: List<Triple<Long, String, Pair<LocalTime, LocalTime>>>,
    onDismiss: () -> Unit,
    onConfirm: (from: LocalDate, to: LocalDate, start: LocalTime, end: LocalTime, branchId: Long) -> Unit
) {
    var from by remember { mutableStateOf(LocalDate.now()) }
    var to by remember { mutableStateOf(LocalDate.now()) }
    var showFrom by remember { mutableStateOf(false) }
    var showTo by remember { mutableStateOf(false) }

    var startTxt by remember { mutableStateOf("09:00") }
    var endTxt by remember { mutableStateOf("17:00") }
    var error by remember { mutableStateOf<String?>(null) }

    var showTemplatePicker by remember { mutableStateOf(false) }
    var showBranchPicker by remember { mutableStateOf(false) }

    var chosenBranch: Pair<Long, String>? by remember { mutableStateOf(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة شِفت (مدى تواريخ)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = from.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        onValueChange = {}, readOnly = true, label = { Text("من تاريخ") },
                        trailingIcon = { TextButton(onClick = { showFrom = true }) { Text("اختيار") } },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        onValueChange = {}, readOnly = true, label = { Text("إلى تاريخ") },
                        trailingIcon = { TextButton(onClick = { showTo = true }) { Text("اختيار") } },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (showTemplatePicker) "جارٍ الاختيار…" else "انقر لاختيار القالب (اختياري)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("القالب") },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showTemplatePicker = true }) { Text("اختيار") }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startTxt, onValueChange = { startTxt = it }, label = { Text("من (HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endTxt, onValueChange = { endTxt = it }, label = { Text("إلى (HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chosenBranch?.second ?: "انقر لاختيار الفرع",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الفرع") },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showBranchPicker = true }) { Text("اختيار") }
                }

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = null
                if (to.isBefore(from)) { error = "نطاق التاريخ غير صحيح"; return@TextButton }
                val start = runCatching { LocalTime.parse(startTxt) }.getOrNull()
                    ?: return@TextButton run { error = "صيغة الوقت خاطئة" }
                val end = runCatching { LocalTime.parse(endTxt) }.getOrNull()
                    ?: return@TextButton run { error = "صيغة الوقت خاطئة" }
                if (!end.isAfter(start)) { error = "النهاية يجب أن تكون بعد البداية"; return@TextButton }
                val b = chosenBranch ?: return@TextButton run { error = "اختر الفرع" }
                onConfirm(from, to, start, end, b.first)
            }) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )

    if (showFrom) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = from.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showFrom = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        from = java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showFrom = false
                }) { Text("تم") }
            },
            dismissButton = { TextButton(onClick = { showFrom = false }) { Text("إلغاء") } }
        ) { DatePicker(state = state) }
    }
    if (showTo) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = to.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showTo = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        to = java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showTo = false
                }) { Text("تم") }
            },
            dismissButton = { TextButton(onClick = { showTo = false }) { Text("إلغاء") } }
        ) { DatePicker(state = state) }
    }

    if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            title = { Text("اختر قالب الشِفت") },
            text = {
                if (templates.isEmpty()) {
                    Text("لا توجد قوالب. أضف قوالب من شاشة قوالب الشِفتات.")
                } else {
                    LazyColumn(Modifier.fillMaxWidth()) {
                        item {
                            ListItem(
                                headlineContent = { Text("بدون قالب (تعبئة يدوية)") },
                                supportingContent = { Text("لن يتم تعبئة الوقت تلقائيًا") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .combinedClickable(onClick = {
                                        showTemplatePicker = false
                                    })
                            )
                        }
                        items(templates) { t ->
                            val (_, name, times) = t
                            ListItem(
                                headlineContent = { Text(name) },
                                supportingContent = { Text("${times.first} – ${times.second}") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .combinedClickable(onClick = {
                                        startTxt = times.first.toString()
                                        endTxt   = times.second.toString()
                                        showTemplatePicker = false
                                    })
                            )
                            Divider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTemplatePicker = false }) { Text("إغلاق") } }
        )
    }

    if (showBranchPicker) {
        AlertDialog(
            onDismissRequest = { showBranchPicker = false },
            title = { Text("اختر الفرع") },
            text = {
                if (branches.isEmpty()) {
                    Text("لا توجد فروع. أضف فرعًا من شاشة الفروع.")
                } else {
                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(branches) { (id, name) ->
                            ListItem(
                                headlineContent = { Text(name) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .combinedClickable(onClick = {
                                        chosenBranch = id to name
                                        showBranchPicker = false
                                    })
                            )
                            Divider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBranchPicker = false }) { Text("إغلاق") } }
        )
    }
}

@Composable
private fun TimeOffSingleDialog(
    onDismiss: () -> Unit,
    onConfirm: (from: LocalDate, to: LocalDate) -> Unit
) {
    var from by remember { mutableStateOf(LocalDate.now()) }
    var to by remember { mutableStateOf(LocalDate.now()) }
    var showFrom by remember { mutableStateOf(false) }
    var showTo by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعيين عطلة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = from.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        onValueChange = {}, readOnly = true, label = { Text("من تاريخ") },
                        trailingIcon = { TextButton(onClick = { showFrom = true }) { Text("اختيار") } },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        onValueChange = {}, readOnly = true, label = { Text("إلى تاريخ") },
                        trailingIcon = { TextButton(onClick = { showTo = true }) { Text("اختيار") } },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = null
                if (to.isBefore(from)) { error = "نطاق التاريخ غير صحيح"; return@TextButton }
                onConfirm(from, to)
            }) { Text("تعيين عطلة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )

    if (showFrom) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = from.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showFrom = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        from = java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showFrom = false
                }) { Text("تم") }
            },
            dismissButton = { TextButton(onClick = { showFrom = false }) { Text("إلغاء") } }
        ) { DatePicker(state = state) }
    }
    if (showTo) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = to.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showTo = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        to = java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showTo = false
                }) { Text("تم") }
            },
            dismissButton = { TextButton(onClick = { showTo = false }) { Text("إلغاء") } }
        ) { DatePicker(state = state) }
    }
}
