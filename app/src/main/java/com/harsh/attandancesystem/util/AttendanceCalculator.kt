package com.harsh.attandancesystem.util

import kotlin.math.roundToInt

object AttendanceCalculator {
    @JvmStatic
    fun calculatePercentage(present: Int, total: Int): Int {
        if (total <= 0) {
            return 0
        }
        return ((present.toDouble() / total.toDouble()) * 100.0).roundToInt()
    }
}
