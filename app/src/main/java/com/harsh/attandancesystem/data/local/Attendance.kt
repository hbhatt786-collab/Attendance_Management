package com.harsh.attandancesystem.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["studentId", "date"], unique = true)
    ]
)
data class Attendance(
    @PrimaryKey(autoGenerate = true) val recordId: Int = 0,
    val studentId: Int,
    val className: String,
    val division: String,
    val date: String,
    val status: String
)
