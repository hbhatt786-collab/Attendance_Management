package com.harsh.attandancesystem.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    @Query(
        """
        SELECT s.id, s.name, s.className, s.email, s.password, s.mobileNo, s.abcId, s.enrollmentNo, s.rollNo, s.division, s.branch,
               COALESCE(SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS presentCount,
               COUNT(a.recordId) AS totalCount
        FROM students s
        LEFT JOIN attendance_records a ON s.id = a.studentId
        GROUP BY s.id
        ORDER BY s.id ASC
        """
    )
    fun getStudentOverviews(): LiveData<List<StudentOverview>>

    @Query(
        """
        SELECT DISTINCT CASE
               WHEN TRIM(className) != '' AND TRIM(division) != '' THEN className || ' - ' || division
               WHEN TRIM(division) != '' THEN division
               ELSE className
               END
        FROM students
        WHERE TRIM(division) != '' OR TRIM(className) != ''
        ORDER BY 1 COLLATE NOCASE ASC
        """
    )
    fun getDistinctDivisions(): LiveData<List<String>>

    @Query(
        """
        SELECT s.id, s.name, s.className, s.rollNo, s.email, s.division,
               COALESCE(a.status, 'PRESENT') AS status
        FROM students s
        LEFT JOIN attendance_records a
            ON s.id = a.studentId AND a.date = :date
        WHERE CASE
              WHEN TRIM(s.className) != '' AND TRIM(s.division) != '' THEN s.className || ' - ' || s.division
              WHEN TRIM(s.division) != '' THEN s.division
              ELSE s.className
              END = :division
        ORDER BY s.id ASC
        """
    )
    fun getAttendanceForDateAndDivision(date: String, division: String): LiveData<List<StudentAttendanceRecord>>

    @Query(
        """
        SELECT COALESCE(a.status, '') AS status
        FROM students s
        LEFT JOIN attendance_records a
            ON s.id = a.studentId AND a.date = :date
        WHERE LOWER(s.email) = LOWER(:email)
        LIMIT 1
        """
    )
    fun getStudentStatusForDate(email: String, date: String): LiveData<StudentStatusRecord?>

    @Query(
        """
        SELECT s.id, s.name, s.className,
               COALESCE(SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS presentCount,
               COUNT(a.recordId) AS totalCount
        FROM students s
        LEFT JOIN attendance_records a
            ON s.id = a.studentId AND a.date BETWEEN :startDate AND :endDate
        GROUP BY s.id
        ORDER BY s.id ASC
        """
    )
    fun getReportSummaries(startDate: String, endDate: String): LiveData<List<ReportSummary>>

    @Query(
        """
        SELECT s.id, s.name, s.className, s.email, s.password, s.mobileNo, s.abcId, s.enrollmentNo, s.rollNo, s.division, s.branch,
               COALESCE(SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS presentCount,
               COUNT(a.recordId) AS totalCount
        FROM students s
        LEFT JOIN attendance_records a ON s.id = a.studentId
        GROUP BY s.id
        HAVING COUNT(a.recordId) > 0
        AND ((COALESCE(SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END), 0) * 100.0) / COUNT(a.recordId)) < :threshold
        ORDER BY s.id ASC
        """
    )
    suspend fun getStudentsBelowThreshold(threshold: Int): List<StudentOverview>
}
