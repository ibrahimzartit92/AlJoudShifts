package com.aljoud.shifts.data.repository

import com.aljoud.shifts.data.AppDatabase
import com.aljoud.shifts.data.entities.ShiftTemplate
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

class ShiftTemplateRepository(private val db: AppDatabase) {
    private val dao = db.shiftTemplateDao()

    fun list(): Flow<List<ShiftTemplate>> = dao.list()

    suspend fun add(name: String, start: LocalTime, end: LocalTime) =
        dao.insert(ShiftTemplate(name = name.trim(), start = start, end = end))

    suspend fun delete(t: ShiftTemplate) = dao.delete(t)

    suspend fun clear() = dao.clear()

    suspend fun ensureSamples() {
        // عينات بسيطة لو ما في شيء
        // رح نضيف 3 قوالب: صباحي/مسائي/ليلي
        dao.insert(ShiftTemplate(name = "صباحي", start = LocalTime.of(9,0), end = LocalTime.of(17,0)))
        dao.insert(ShiftTemplate(name = "مسائي", start = LocalTime.of(17,0), end = LocalTime.of(23,0)))
        dao.insert(ShiftTemplate(name = "ليلي",  start = LocalTime.of(23,0), end = LocalTime.of(7,0)))
    }
}
