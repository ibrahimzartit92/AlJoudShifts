package com.aljoud.shifts.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aljoud.shifts.data.AppDatabase
import com.aljoud.shifts.data.entities.ShiftTemplate
import com.aljoud.shifts.data.repository.ShiftTemplateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

class ShiftTemplatesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ShiftTemplateRepository(AppDatabase.get(app))

    val templates = repo.list()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, start: LocalTime, end: LocalTime) =
        viewModelScope.launch { repo.add(name, start, end) }

    fun delete(t: ShiftTemplate) =
        viewModelScope.launch { repo.delete(t) }

    fun seedDefaultsIfEmpty() =
        viewModelScope.launch { repo.ensureSamples() }

    fun clear() = viewModelScope.launch { repo.clear() }
}
