package com.aljoud.shifts.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aljoud.shifts.data.AppDatabase
import com.aljoud.shifts.data.repository.BranchRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BranchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = BranchRepository(AppDatabase.get(app))

    val branches = repo.list()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ترجع true لو انضاف، false لو مكرر
    suspend fun addBranchAndKnow(name: String): Boolean =
        suspendCancellableCoroutine { cont ->
            viewModelScope.launch {
                val ok = repo.add(name)
                cont.resume(ok)
            }
        }

    fun addSampleIfEmpty() = viewModelScope.launch { repo.addSampleIfEmpty() }
    fun clearAll() = viewModelScope.launch { repo.clear() }
}
