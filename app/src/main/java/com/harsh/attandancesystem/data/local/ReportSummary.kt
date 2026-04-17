package com.harsh.attandancesystem.data.local

import com.harsh.attandancesystem.util.AttendanceCalculator

data class ReportSummary(
    val id: Int,
    val name: String,
    val className: String,
    val presentCount: Int,
    val totalCount: Int
) {
    fun getAttendancePercentage(): Int {
        return AttendanceCalculator.calculatePercentage(presentCount, totalCount)
    }

    fun isWarning(): Boolean {
        return totalCount > 0 && getAttendancePercentage() < 75
    }
}
