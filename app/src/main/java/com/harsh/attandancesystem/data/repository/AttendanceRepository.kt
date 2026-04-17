package com.harsh.attandancesystem.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.harsh.attandancesystem.data.local.Attendance
import com.harsh.attandancesystem.data.local.AttendanceDao
import com.harsh.attandancesystem.data.local.ReportSummary
import com.harsh.attandancesystem.data.local.Student
import com.harsh.attandancesystem.data.local.StudentAttendanceRecord
import com.harsh.attandancesystem.data.local.StudentDao
import com.harsh.attandancesystem.data.local.StudentOverview
import com.harsh.attandancesystem.data.local.StudentStatusRecord
import com.harsh.attandancesystem.data.remote.FirebaseSyncService

class AttendanceRepository(
    context: Context,
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceDao
) {
    private val firebaseSyncService = FirebaseSyncService(context.applicationContext)

    fun getStudentOverviews(): LiveData<List<StudentOverview>> = attendanceDao.getStudentOverviews()

    fun getDistinctDivisions(): LiveData<List<String>> = attendanceDao.getDistinctDivisions()

    fun getAttendanceForDateAndDivision(date: String, division: String): LiveData<List<StudentAttendanceRecord>> =
        attendanceDao.getAttendanceForDateAndDivision(date, division)

    fun getStudentStatusForDate(email: String, date: String): LiveData<StudentStatusRecord?> =
        attendanceDao.getStudentStatusForDate(email, date)

    fun getReportSummaries(startDate: String, endDate: String): LiveData<List<ReportSummary>> =
        attendanceDao.getReportSummaries(startDate, endDate)

    suspend fun syncStudentsFromRemote() {
        val remoteStudents = firebaseSyncService.fetchStudents()
        if (remoteStudents.isNotEmpty()) {
            studentDao.insertAll(remoteStudents)
        }
    }

    suspend fun addStudent(
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
        val student = Student(
            name = name.trim(),
            className = className.trim(),
            email = email.trim(),
            password = password,
            rollNo = rollNo,
            division = division,
            branch = branch,
            enrollmentNo = enrollmentNo,
            mobileNo = mobileNo,
            abcId = abcId
        )
        val insertedId = studentDao.insert(student).toInt()
        firebaseSyncService.syncStudent(student.copy(id = insertedId))
    }

    suspend fun updateStudent(
        id: Int,
        name: String,
        className: String,
        email: String = "",
        password: String = "",
        rollNo: String,
        division: String,
        branch: String,
        enrollmentNo: String,
        mobileNo: String,
        abcId: String
    ) {
        val student = Student(
            id = id,
            name = name.trim(),
            className = className.trim(),
            email = email.trim(),
            password = password,
            rollNo = rollNo,
            division = division,
            branch = branch,
            enrollmentNo = enrollmentNo,
            mobileNo = mobileNo,
            abcId = abcId
        )
        studentDao.update(student)
        firebaseSyncService.syncStudent(student)
    }

    suspend fun deleteStudent(student: Student) {
        studentDao.delete(student)
        firebaseSyncService.deleteStudent(student.id)
    }

    suspend fun addStudents(students: List<Student>) {
        if (students.isNotEmpty()) {
            val insertedIds = studentDao.insertAll(students)
            students.forEachIndexed { index, student ->
                val studentId = insertedIds.getOrNull(index)?.toInt() ?: student.id
                firebaseSyncService.syncStudent(student.copy(id = studentId))
            }
        }
    }

    suspend fun saveAttendance(records: List<StudentAttendanceRecord>, date: String) {
        records.forEach { record ->
            attendanceDao.insertAttendance(
                Attendance(
                    studentId = record.id,
                    className = record.className,
                    division = record.division,
                    date = date,
                    status = record.status
                )
            )
            firebaseSyncService.syncAttendance(record, date)
        }
    }

    suspend fun getStudentsBelowThreshold(threshold: Int): List<StudentOverview> {
        return attendanceDao.getStudentsBelowThreshold(threshold)
    }
}
