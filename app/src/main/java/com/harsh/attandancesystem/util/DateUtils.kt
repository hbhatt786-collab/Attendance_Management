package com.harsh.attandancesystem.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {
    private val storageFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    @JvmStatic
    fun getToday(): String {
        return storageFormat.format(Calendar.getInstance().time)
    }

    @JvmStatic
    fun getDisplayDate(date: String): String {
        return try {
            displayFormat.format(storageFormat.parse(date)!!)
        } catch (_: Exception) {
            date
        }
    }

    @JvmStatic
    fun buildDate(year: Int, monthOfYear: Int, dayOfMonth: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, monthOfYear, dayOfMonth, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return storageFormat.format(calendar.time)
    }

    @JvmStatic
    fun getWeeklyRange(endDate: String): Array<String> {
        val endCalendar = toCalendar(endDate)
        val startCalendar = endCalendar.clone() as Calendar
        startCalendar.add(Calendar.DAY_OF_YEAR, -6)
        return arrayOf(storageFormat.format(startCalendar.time), storageFormat.format(endCalendar.time))
    }

    @JvmStatic
    fun getMonthlyRange(endDate: String): Array<String> {
        val endCalendar = toCalendar(endDate)
        val startCalendar = endCalendar.clone() as Calendar
        startCalendar.add(Calendar.DAY_OF_YEAR, -29)
        return arrayOf(storageFormat.format(startCalendar.time), storageFormat.format(endCalendar.time))
    }

    @JvmStatic
    fun getSixMonthRange(endDate: String): Array<String> {
        val endCalendar = toCalendar(endDate)
        val startCalendar = endCalendar.clone() as Calendar
        startCalendar.add(Calendar.MONTH, -5)
        return arrayOf(storageFormat.format(startCalendar.time), storageFormat.format(endCalendar.time))
    }

    @JvmStatic
    fun getRangeLabel(mode: String, endDate: String): String {
        val range = when (mode) {
            "MONTHLY" -> getMonthlyRange(endDate)
            "SIX_MONTHS" -> getSixMonthRange(endDate)
            else -> getWeeklyRange(endDate)
        }
        return "${getDisplayDate(range[0])} - ${getDisplayDate(range[1])}"
    }

    private fun toCalendar(date: String): Calendar {
        val calendar = Calendar.getInstance()
        calendar.time = storageFormat.parse(date) ?: Calendar.getInstance().time
        return calendar
    }
}
