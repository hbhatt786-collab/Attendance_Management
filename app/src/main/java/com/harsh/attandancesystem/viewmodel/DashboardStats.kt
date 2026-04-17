package com.harsh.attandancesystem.viewmodel

data class DashboardStats(
    val totalStudents: Int = 0,
    val presentToday: Int = 0,
    val averageAttendance: Int = 0,
    val lowAttendanceCount: Int = 0
)
