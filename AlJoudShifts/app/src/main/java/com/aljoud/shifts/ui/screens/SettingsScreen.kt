package com.aljoud.shifts.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    var serverUrl by remember { mutableStateOf("") } // مثال مستقبلاً لنسخ احتياطي NAS
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("الإعدادات", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("رابط السيرفر (NAS) للنسخ الاحتياطي)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { /* TODO: احفظ الإعدادات */ }) {
            Text("حفظ")
        }
    }
}
