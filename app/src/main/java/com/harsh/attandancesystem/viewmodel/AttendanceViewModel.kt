package com.harsh.attandancesystem.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.harsh.attandancesystem.SessionManager
import com.harsh.attandancesystem.data.local.AttendanceDatabase
import com.harsh.attandancesystem.data.local.ReportSummary
import com.harsh.attandancesystem.data.local.Student
import com.harsh.attandancesystem.data.local.StudentAttendanceRecord
import com.harsh.attandancesystem.data.local.StudentOverview
import com.harsh.attandancesystem.data.local.StudentStatusRecord
import com.harsh.attandancesystem.data.repository.AttendanceRepository
import com.harsh.attandancesystem.util.DateUtils
import com.harsh.attandancesystem.util.NotificationHelper
import kotlinx.coroutines.launch

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private data class AttendanceRequest(val date: String, val division: String)
    private data class ReportRequest(val date: String, val mode: String)

    private val sessionManager = SessionManager(application)
    private val repository: AttendanceRepository
    private val selectedDate = MutableLiveData(sessionManager.getLastAttendanceDate())
    private val selectedDivision = MutableLiveData("")
    private val reportMode = MutableLiveData(REPORT_MODE_WEEKLY)
    private val attendanceRequest = MediatorLiveData<AttendanceRequest>()
    private val reportRequest = MediatorLiveData<ReportRequest>()

    val divisions: LiveData<List<String>>
    val studentOverviews: LiveData<List<StudentOverview>>
    val attendanceRecords: LiveData<List<StudentAttendanceRecord>>
    val reportSummaries: LiveData<List<ReportSummary>>
    val dashboardStats = MediatorLiveData<DashboardStats>()
    val lowAttendanceStudents = MediatorLiveData<List<StudentOverview>>()
    val selectedDateLabel: LiveData<String> = selectedDate.map {
        DateUtils.getDisplayDate(it)
    }
    val reportRangeLabel: LiveData<String> = MediatorLiveData<String>().apply {
        fun update() {
            val date = selectedDate.value ?: DateUtils.getToday()
            val mode = reportMode.value ?: REPORT_MODE_WEEKLY
            value = DateUtils.getRangeLabel(mode, date)
        }
        addSource(selectedDate) { update() }
        addSource(reportMode) { update() }
        update()
    }

    init {
        val database = AttendanceDatabase.getInstance(application)
        repository = AttendanceRepository(application, database.studentDao(), database.attendanceDao())
        divisions = repository.getDistinctDivisions()
        studentOverviews = repository.getStudentOverviews()
        attendanceRecords = attendanceRequest.switchMap { request ->
            repository.getAttendanceForDateAndDivision(request.date, request.division)
        }
        reportSummaries = reportRequest.switchMap { request ->
            val range = if (request.mode == REPORT_MODE_MONTHLY) {
                DateUtils.getMonthlyRange(request.date)
            } else if (request.mode == REPORT_MODE_SIX_MONTHS) {
                DateUtils.getSixMonthRange(request.date)
            } else {
                DateUtils.getWeeklyRange(request.date)
            }
            repository.getReportSummaries(range[0], range[1])
        }
        attendanceRequest.addSource(selectedDate) { updateAttendanceRequest() }
        attendanceRequest.addSource(selectedDivision) { updateAttendanceRequest() }
        reportRequest.addSource(selectedDate) { updateReportRequest() }
        reportRequest.addSource(reportMode) { updateReportRequest() }
        updateAttendanceRequest()
        updateReportRequest()
        setupDashboardSources()
        setupLowAttendanceSource()
        NotificationHelper.createNotificationChannel(application)
        viewModelScope.launch {
            repository.syncStudentsFromRemote()
        }
    }

    fun setSelectedDate(date: String) {
        selectedDate.value = date
        sessionManager.saveLastAttendanceDate(date)
    }

    fun getSelectedDateValue(): String {
        return selectedDate.value ?: sessionManager.getLastAttendanceDate()
    }

    fun getStudentStatusForSelectedDate(email: String): LiveData<StudentStatusRecord?> {
        return selectedDate.switchMap { date ->
            repository.getStudentStatusForDate(email, date ?: sessionManager.getLastAttendanceDate())
        }
    }

    fun getStudentStatusForToday(email: String): LiveData<StudentStatusRecord?> {
        return repository.getStudentStatusForDate(email, DateUtils.getToday())
    }

    fun setSelectedDivision(division: String) {
        selectedDivision.value = division
    }

    fun getSelectedDivision(): String {
        return selectedDivision.value.orEmpty()
    }

    fun setReportMode(mode: String) {
        reportMode.value = mode
    }

    fun getReportMode(): String {
        return reportMode.value ?: REPORT_MODE_WEEKLY
    }

    @JvmOverloads
    fun addStudent(
        name: String,
        className: String,
        email: String = "",
        password: String = "",
        rollNo: String = "",
        division: String = "",
        branch: String = "",
        enrollmentNo: String = "",
        mobileNo: String = "",
        abcId: String = ""
    ) {
        viewModelScope.launch {
            repository.addStudent(name, className, email, password, rollNo, division, branch, enrollmentNo, mobileNo, abcId)
        }
    }

    @JvmOverloads
    fun updateStudent(
        id: Int,
        name: String,
        className: String,
        email: String = "",
        password: String = "",
        rollNo: String = "",
        division: String = "",
        branch: String = "",
        enrollmentNo: String = "",
        mobileNo: String = "",
        abcId: String = ""
    ) {
        viewModelScope.launch {
            repository.updateStudent(id, name, className, email, password, rollNo, division, branch, enrollmentNo, mobileNo, abcId)
        }
    }

    fun deleteStudent(overview: StudentOverview) {
        viewModelScope.launch {
            repository.deleteStudent(
                Student(
                    id = overview.id,
                    name = overview.name,
                    className = overview.className,
                    email = overview.email,
                    password = overview.password,
                    rollNo = overview.rollNo,
                    division = overview.division,
                    branch = overview.branch,
                    enrollmentNo = overview.enrollmentNo,
                    mobileNo = overview.mobileNo,
                    abcId = overview.abcId
                )
            )
        }
    }

    fun importStudents(students: List<Student>) {
        viewModelScope.launch {
            repository.addStudents(students)
        }
    }

    fun saveAttendanceRecords(records: List<StudentAttendanceRecord>) {
        val date = selectedDate.value ?: sessionManager.getLastAttendanceDate()
        viewModelScope.launch {
            repository.saveAttendance(records, date)
            sessionManager.saveLastAttendanceDate(date)
            repository.getStudentsBelowThreshold(75).forEach { overview ->
                NotificationHelper.sendLowAttendanceNotification(getApplication(), overview)
            }
        }
    }

    private fun setupDashboardSources() {
        fun updateStats() {
            val studentList = studentOverviews.value.orEmpty()
            val attendanceList = attendanceRecords.value.orEmpty()
            val presentToday = attendanceList.count { it.status == STATUS_PRESENT }
            val averageAttendance = if (studentList.isEmpty()) {
                0
            } else {
                studentList.map { it.getAttendancePercentage() }.average().toInt()
            }
            val lowCount = studentList.count { it.isLowAttendance() }
            dashboardStats.value = DashboardStats(
                totalStudents = studentList.size,
                presentToday = presentToday,
                averageAttendance = averageAttendance,
                lowAttendanceCount = lowCount
            )
        }

        dashboardStats.addSource(studentOverviews) { updateStats() }
        dashboardStats.addSource(attendanceRecords) { updateStats() }
    }

    private fun setupLowAttendanceSource() {
        lowAttendanceStudents.addSource(studentOverviews) { list ->
            lowAttendanceStudents.value = list.filter { it.isLowAttendance() }
        }
    }

    private fun updateAttendanceRequest() {
        val division = selectedDivision.value.orEmpty()
        attendanceRequest.value = AttendanceRequest(
            date = selectedDate.value ?: sessionManager.getLastAttendanceDate(),
            division = division
        )
    }

    private fun updateReportRequest() {
        reportRequest.value = ReportRequest(
            date = selectedDate.value ?: sessionManager.getLastAttendanceDate(),
            mode = reportMode.value ?: REPORT_MODE_WEEKLY
        )
    }

    companion object {
        const val STATUS_PRESENT = "PRESENT"
        const val STATUS_ABSENT = "ABSENT"
        const val REPORT_MODE_WEEKLY = "WEEKLY"
        const val REPORT_MODE_MONTHLY = "MONTHLY"
        const val REPORT_MODE_SIX_MONTHS = "SIX_MONTHS"
    }
}
