package com.aljoud.shifts.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aljoud.shifts.data.dao.*
import com.aljoud.shifts.data.entities.*

@Database(
    entities = [
        Branch::class,
        Employee::class,
        Shift::class,
        TimeOff::class,
        ShiftTemplate::class       // ⬅️ جديد
    ],
    version = 7,                  // ⬅️ رفعنا النسخة بعد إضافة الكيان الجديد
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun branchDao(): BranchDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun shiftDao(): ShiftDao
    abstract fun timeOffDao(): TimeOffDao
    abstract fun shiftTemplateDao(): ShiftTemplateDao   // ⬅️ جديد

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aljoud.db"
                )
                    .fallbackToDestructiveMigration() // خلال التطوير
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
