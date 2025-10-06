package com.aljoud.shifts.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aljoud.shifts.ui.BranchViewModel
import com.aljoud.shifts.ui.EmployeesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesScreen(
    employeesVM: EmployeesViewModel = viewModel(),
    branchesVM: BranchViewModel = viewModel(),
    onEmployeeClick: (Long, String) -> Unit = { _, _ -> }   // ✅ لإرسال المستخدم لتفاصيل الموظف
) {
    val branches = branchesVM.branches.collectAsStateWithLifecycle()
    val employees = employeesVM.employees.collectAsStateWithLifecycle()

    // تأكد من وجود فروع تجريبية أول مرة
    LaunchedEffect(Unit) { branchesVM.addSampleIfEmpty() }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedBranchId by remember { mutableStateOf<Long?>(null) }

    val selectedBranchName = remember(branches.value, selectedBranchId) {
        branches.value.firstOrNull { it.id == selectedBranchId }?.name ?: "اختر الفرع"
    }

    var showAddEmployee by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("قائمة الموظفين") },
                actions = {
                    IconButton(onClick = { showAddEmployee = true }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة موظف")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            if (employees.value.isEmpty()) {
                Text("لا يوجد موظفون بعد…")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(employees.value) { e ->
                        ElevatedCard(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onEmployeeClick(e.id, e.fullName) }   // ✅ افتح التفاصيل
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                val branchName =
                                    branches.value.firstOrNull { it.id == e.branchId }?.name
                                        ?: "غير محدد"
                                Text(e.fullName, style = MaterialTheme.typography.titleMedium)
                                Text("فرع: $branchName")
                                Text("واتساب: +${e.phoneE164}")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { employeesVM.clear() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("حذف جميع الموظفين (تجريبي)")
            }
        }
    }

    // نافذة إضافة موظف
    if (showAddEmployee) {
        AlertDialog(
            onDismissRequest = { showAddEmployee = false },
            confirmButton = {},
            title = { Text("إضافة موظف جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم الموظف") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم واتساب (E.164 بدون +)") },
                        placeholder = { Text("مثال: 4915XXXXXXXXX") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedBranchName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الفرع") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            branches.value.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.name) },
                                    onClick = {
                                        selectedBranchId = b.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val ok = name.isNotBlank() &&
                                    phone.matches(Regex("""^49\d{7,13}$""")) &&
                                    selectedBranchId != null
                            if (ok) {
                                employeesVM.add(name, phone, selectedBranchId!!)
                                name = ""
                                phone = ""
                                selectedBranchId = null
                                showAddEmployee = false
                            }
                        },
                        enabled = branches.value.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("إضافة") }

                    OutlinedButton(
                        onClick = { showAddEmployee = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("إلغاء") }
                }
            }
        )
    }
}
