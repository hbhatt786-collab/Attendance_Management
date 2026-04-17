package com.harsh.attandancesystem.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Student::class, Attendance::class],
    version = 5,
    exportSchema = false
)
abstract class AttendanceDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao

    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AttendanceDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): AttendanceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AttendanceDatabase::class.java,
                    "attendance_management.db"
                )
                    // Recreate older local schemas so launch-time Room queries do not crash.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
