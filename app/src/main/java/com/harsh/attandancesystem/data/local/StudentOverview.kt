package com.harsh.attandancesystem.data.local

import com.harsh.attandancesystem.util.AttendanceCalculator

data class StudentOverview(
    val id: Int,
    val name: String,
    val className: String,
    val email: String = "",
    val password: String = "",
    val presentCount: Int,
    val totalCount: Int,
    val mobileNo: String = "",
    val abcId: String = "",
    val enrollmentNo: String = "",
    val rollNo: String = "",
    val division: String = "",
    val branch: String = ""
) {
    fun getAttendancePercentage(): Int {
        return AttendanceCalculator.calculatePercentage(presentCount, totalCount)
    }

    fun isLowAttendance(): Boolean {
        return totalCount > 0 && getAttendancePercentage() < 75
    }
}
