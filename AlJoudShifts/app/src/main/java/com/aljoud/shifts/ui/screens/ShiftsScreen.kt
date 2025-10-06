package com.aljoud.shifts.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aljoud.shifts.ui.BranchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftsScreen(
    branchesVM: BranchViewModel = viewModel(),
    onOpenBranch: (Long, String) -> Unit = { _, _ -> }
) {
    val branches = branchesVM.branches.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { branchesVM.addSampleIfEmpty() }

    Scaffold(topBar = { TopAppBar(title = { Text("اختر فرعًا") }) }) { inner ->
        Column(
            Modifier.padding(inner).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (branches.value.isEmpty()) {
                Text("لا توجد فروع بعد…")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(branches.value, key = { it.id }) { b ->
                        ElevatedCard(
                            Modifier.fillMaxWidth().clickable { onOpenBranch(b.id, b.name) }
                        ) { Text(b.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp)) }
                    }
                }
            }
        }
    }
}
