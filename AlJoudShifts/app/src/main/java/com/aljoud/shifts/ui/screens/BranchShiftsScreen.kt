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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aljoud.shifts.data.dao.ShiftWithNames
import com.aljoud.shifts.ui.BranchShiftsViewModel
import com.aljoud.shifts.ui.EmployeesViewModel
import com.aljoud.shifts.ui.ShiftTemplatesViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext


/* ============================== شاشة الشفتات لفرع ============================== */

@Composable
fun BranchShiftsScreen(
    branchId: Long,
    branchName: String,
    vm: BranchShiftsViewModel = viewModel(),
    employeesVM: EmployeesViewModel = viewModel(),
    templatesVM: ShiftTemplatesViewModel = viewModel()
) {
    LaunchedEffect(branchId) { vm.setBranch(branchId) }
    LaunchedEffect(Unit) { templatesVM.seedDefaultsIfEmpty() }

    val ar = Locale("ar")
    val weekRangeFmt = remember { DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", ar) }
    val dayHeaderFmt = remember { DateTimeFormatter.ofPattern("EEEE d MMMM", ar) }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm", ar) }

    val weekStart = vm.weekStart.collectAsStateWithLifecycle()
    val weekEnd   = vm.weekEnd.collectAsStateWithLifecycle()
    val shifts    = vm.shifts.collectAsStateWithLifecycle()
    val employees = employeesVM.employees.collectAsStateWithLifecycle()
    val templates = templatesVM.templates.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            (ev as? BranchShiftsViewModel.UiEvent.Info)?.let { snackbar.showSnackbar(it.msg) }
        }
    }

    var toDelete by remember { mutableStateOf<ShiftWithNames?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var showTimeOff by remember { mutableStateOf(false) }
    var fabMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("شِفتات $branchName") },
                actions = {
                    TextButton(onClick = { vm.prevWeek() }) { Text("السابق") }
                    TextButton(onClick = { vm.nextWeek() }) { Text("التالي") }
                    val ctx = LocalContext.current
                    TextButton(onClick = {
                        val ym = java.time.YearMonth.now()
                        vm.exportMonthlyPdfForBranch(ctx, branchId, branchName, ym)
                    }) { Text("PDF شهري") }

                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { fabMenuExpanded = !fabMenuExpanded }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
                DropdownMenu(
                    expanded = fabMenuExpanded,
                    onDismissRequest = { fabMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("إضافة شِفت جديد") },
                        onClick = {
                            fabMenuExpanded = false
                            showAdd = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("تعيين عطلة") },
                        onClick = {
                            fabMenuExpanded = false
                            showTimeOff = true
                        }
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
            Text(
                "${weekStart.value.format(weekRangeFmt)} → ${weekEnd.value.format(weekRangeFmt)}",
                color = Color(0xFFFFD700)
            )
            Spacer(Modifier.height(8.dp))

            val startMonday = remember(weekStart.value) { weekStart.value.with(DayOfWeek.MONDAY) }
            val daysOfWeek = remember(startMonday) { (0..6).map { startMonday.plusDays(it.toLong()) } }

            val shiftsByDate = remember(shifts.value) {
                shifts.value
                    .sortedWith(compareBy<ShiftWithNames>({ it.date }, { it.start }, { it.employeeName }))
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
                                    s.employeeName,
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

    // إضافة شفتات لمدى تواريخ + موظفين متعددين مع اختيار قالب يملأ الأوقات
    if (showAdd) {
        AddBranchShiftRangeDialog(
            employees = employees.value.map { it.id to it.fullName },
            templates = templates.value.map { Triple(it.id, it.name, it.start to it.end) },
            onDismiss = { showAdd = false },
            onConfirm = { from, to, start, end, selectedIds ->
                vm.addShiftForEmployeesRange(branchId, from, to, start, end, selectedIds)
                showAdd = false
            }
        )
    }

    // تأكيد حذف شفت
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("حذف الشِفت") },
            text = { Text("هل تريد حذف هذا الشِفت؟") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteShift(toDelete!!.id)
                    toDelete = null
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("إلغاء") } }
        )
    }

    // تعيين عطلة لمدى تواريخ + موظفين متعددين
    if (showTimeOff) {
        TimeOffDialog(
            employees = employees.value.map { it.id to it.fullName },
            onDismiss = { showTimeOff = false },
            onConfirm = { from, to, ids ->
                vm.setTimeOffForEmployees(from, to, ids)
                showTimeOff = false
            }
        )
    }
}

/* ============================== الحوارات ============================== */

@Composable
private fun AddBranchShiftRangeDialog(
    employees: List<Pair<Long, String>>,
    templates: List<Triple<Long, String, Pair<LocalTime, LocalTime>>>,
    onDismiss: () -> Unit,
    onConfirm: (from: LocalDate, to: LocalDate, start: LocalTime, end: LocalTime, employeeIds: List<Long>) -> Unit
) {
    var from by remember { mutableStateOf(LocalDate.now()) }
    var to by remember { mutableStateOf(LocalDate.now()) }
    var showFrom by remember { mutableStateOf(false) }
    var showTo by remember { mutableStateOf(false) }

    var startTxt by remember { mutableStateOf("09:00") }
    var endTxt by remember { mutableStateOf("17:00") }
    var error by remember { mutableStateOf<String?>(null) }

    // بدّلنا القوائم المنسدلة إلى حوارات ثانوية لتفادي مشاكل الطبقات
    var showTemplatePicker by remember { mutableStateOf(false) }
    var showEmployeesPicker by remember { mutableStateOf(false) }

    // حالة اختيار الموظفين
    val selected = remember { mutableStateMapOf<Long, Boolean>() }
    employees.forEach { (id, _) -> if (selected[id] == null) selected[id] = false }

    // ملخص الأسماء المختارة
    val selectedNames = employees.filter { selected[it.first] == true }.map { it.second }
    val selectedSummary = when {
        selectedNames.isEmpty() -> "اختر الموظفين"
        selectedNames.size <= 2 -> selectedNames.joinToString("، ")
        else -> "${selectedNames.take(2).joinToString("، ")} (+${selectedNames.size - 2})"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة شِفت لعدة موظفين (مدى تواريخ)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // من/إلى تاريخ
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

                // اختيار قالب (حوار ثانوي)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = "القالب: ${if (showTemplatePicker) "جارٍ الاختيار…" else "انقر للاختيار"}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("القالب (اختياري)") },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showTemplatePicker = true }) { Text("اختيار") }
                }

                // أوقات من/إلى — تبقى قابلة للتعديل حتى لو اخترنا قالب
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startTxt, onValueChange = { startTxt = it }, label = { Text("من (HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endTxt, onValueChange = { endTxt = it }, label = { Text("إلى (HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                Divider()

                // اختيار الموظفين (حوار ثانوي متعدد الاختيار)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = selectedSummary,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الموظفون") },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showEmployeesPicker = true }) { Text("اختيار") }
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
                val ids = selected.filterValues { it }.keys.toList()
                if (ids.isEmpty()) { error = "اختر موظفًا واحدًا على الأقل"; return@TextButton }
                onConfirm(from, to, start, end, ids)
            }) { Text("إضافة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )

    // Date pickers
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

    /* ======= حوار اختيار القالب ======= */
    if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            title = { Text("اختر قالب الشِفت") },
            text = {
                if (templates.isEmpty()) {
                    Text("لا توجد قوالب. يمكنك إضافة قوالب من شاشة قوالب الشِفتات.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
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
                            val (id, name, times) = t
                            ListItem(
                                headlineContent = { Text(name) },
                                supportingContent = { Text("${times.first} – ${times.second}") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .combinedClickable(onClick = {
                                        // تعبئة تلقائية للحقلين
                                        startTxt = times.first.toString()
                                        endTxt = times.second.toString()
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

    /* ======= حوار اختيار الموظفين (متعدد) ======= */
    if (showEmployeesPicker) {
        var tmpSelected by remember {
            mutableStateOf(selected.toMap()) // snapshot مؤقت
        }
        AlertDialog(
            onDismissRequest = { showEmployeesPicker = false },
            title = { Text("اختر الموظفين") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(employees) { (id, name) ->
                        val checked = tmpSelected[id] == true
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { on ->
                                    tmpSelected = tmpSelected.toMutableMap().apply { put(id, on) }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(name)
                        }
                        Divider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // نسخ الاختيارات المؤقتة إلى الحالة الأساسية
                    tmpSelected.forEach { (k, v) -> selected[k] = v }
                    showEmployeesPicker = false
                }) { Text("تم") }
            },
            dismissButton = { TextButton(onClick = { showEmployeesPicker = false }) { Text("إلغاء") } }
        )
    }
}

/* ------------------ العطلة ------------------ */

@Composable
private fun TimeOffDialog(
    employees: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onConfirm: (from: LocalDate, to: LocalDate, employeeIds: List<Long>) -> Unit
) {
    var from by remember { mutableStateOf(LocalDate.now()) }
    var to by remember { mutableStateOf(LocalDate.now()) }
    var showFrom by remember { mutableStateOf(false) }
    var showTo by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // حوار ثانوي لاختيار الموظفين (بدل Dropdown داخل Dialog)
    var showEmployeesPicker by remember { mutableStateOf(false) }
    val selected = remember { mutableStateMapOf<Long, Boolean>() }
    employees.forEach { (id, _) -> if (selected[id] == null) selected[id] = false }

    val selectedNames = employees.filter { selected[it.first] == true }.map { it.second }
    val selectedSummary = when {
        selectedNames.isEmpty() -> "اختر الموظفين"
        selectedNames.size <= 2 -> selectedNames.joinToString("، ")
        else -> "${selectedNames.take(2).joinToString("، ")} (+${selectedNames.size - 2})"
    }

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

                Divider()

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = selectedSummary,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الموظفون") },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showEmployeesPicker = true }) { Text("اختيار") }
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
                val ids = selected.filterValues { it }.keys.toList()
                if (ids.isEmpty()) { error = "اختر موظفًا واحدًا على الأقل"; return@TextButton }
                onConfirm(from, to, ids)
            }) { Text("تعيين عطلة") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )

    // Date pickers
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

    // حوار اختيار الموظفين للعطلة
    if (showEmployeesPicker) {
        var tmpSelected by remember { mutableStateOf(selected.toMap()) }
        AlertDialog(
            onDismissRequest = { showEmployeesPicker = false },
            title = { Text("اختر الموظفين") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(employees) { (id, name) ->
                        val checked = tmpSelected[id] == true
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { on ->
                                    tmpSelected = tmpSelected.toMutableMap().apply { put(id, on) }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(name)
                        }
                        Divider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    tmpSelected.forEach { (k, v) -> selected[k] = v }
                    showEmployeesPicker = false
                }) { Text("تم") }
            },
            dismissButton = { TextButton(onClick = { showEmployeesPicker = false }) { Text("إلغاء") } }
        )
    }
}
