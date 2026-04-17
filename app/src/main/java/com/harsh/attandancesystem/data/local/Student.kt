package com.harsh.attandancesystem.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val className: String,
    val email: String = "",
    val password: String = "", // Added for Admin-assigned login
    val mobileNo: String = "",
    val abcId: String = "",
    val enrollmentNo: String = "",
    val rollNo: String = "",
    val division: String = "",
    val branch: String = ""
)
