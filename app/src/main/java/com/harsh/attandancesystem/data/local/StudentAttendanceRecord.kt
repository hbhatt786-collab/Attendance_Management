package com.harsh.attandancesystem.data.local

data class StudentAttendanceRecord(
    val id: Int,
    val name: String,
    val className: String,
    val rollNo: String = "",
    val email: String = "",
    val division: String = "",
    var status: String
)
