package com.aljoud.shifts.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aljoud.shifts.ui.ShiftTemplatesViewModel
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftTemplatesScreen(vm: ShiftTemplatesViewModel = viewModel()) {
    val templates by vm.templates.collectAsState()

    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("قوالب الشِفتات") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { inner ->
        Column(Modifier.padding(inner).padding(16.dp)) {
            if (templates.isEmpty()) {
                Text("لا توجد قوالب بعد…")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.seedDefaultsIfEmpty() }) {
                    Text("إضافة قوالب افتراضية")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(templates, key = { it.id }) { t ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(t.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${t.start} → ${t.end}")
                                }
                                TextButton(onClick = { vm.delete(t) }) { Text("حذف") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var startTxt by remember { mutableStateOf("09:00") }
        var endTxt by remember { mutableStateOf("17:00") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("إضافة قالب جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم القالب") })
                    OutlinedTextField(value = startTxt, onValueChange = { startTxt = it }, label = { Text("من (HH:mm)") })
                    OutlinedTextField(value = endTxt, onValueChange = { endTxt = it }, label = { Text("إلى (HH:mm)") })
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    error = null
                    val start = runCatching { LocalTime.parse(startTxt) }.getOrNull()
                        ?: return@TextButton run { error = "صيغة الوقت غير صحيحة" }
                    val end = runCatching { LocalTime.parse(endTxt) }.getOrNull()
                        ?: return@TextButton run { error = "صيغة الوقت غير صحيحة" }
                    if (!end.isAfter(start)) { error = "النهاية يجب أن تكون بعد البداية"; return@TextButton }
                    if (name.isBlank()) { error = "أدخل اسم القالب"; return@TextButton }

                    vm.add(name.trim(), start, end)
                    showAdd = false
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("إلغاء") } }
        )
    }
}
