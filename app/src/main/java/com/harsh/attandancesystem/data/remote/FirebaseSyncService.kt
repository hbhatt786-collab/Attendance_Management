package com.harsh.attandancesystem.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.harsh.attandancesystem.data.local.Student
import com.harsh.attandancesystem.data.local.StudentAttendanceRecord
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseSyncService(context: Context) {

    init {
        FirebaseApp.initializeApp(context.applicationContext)
    }

    private val rootRef by lazy {
        FirebaseDatabase
            .getInstance(DATABASE_URL)
            .reference
            .child(ROOT_NODE)
    }

    fun syncStudent(student: Student) {
        rootRef.child(STUDENTS_NODE)
            .child(student.id.toString())
            .setValue(student.toFirebaseMap())
            .addOnFailureListener { logFailure("syncStudent", it) }
    }

    fun deleteStudent(studentId: Int) {
        rootRef.child(STUDENTS_NODE)
            .child(studentId.toString())
            .removeValue()
            .addOnFailureListener { logFailure("deleteStudent", it) }
    }

    fun syncAttendance(record: StudentAttendanceRecord, date: String) {
        rootRef.child(ATTENDANCE_NODE)
            .child(date)
            .child(record.id.toString())
            .setValue(record.toFirebaseMap(date))
            .addOnFailureListener { logFailure("syncAttendance", it) }
    }

    suspend fun fetchStudents(): List<Student> = suspendCoroutine { continuation ->
        rootRef.child(STUDENTS_NODE)
            .get()
            .addOnSuccessListener { snapshot ->
                continuation.resume(snapshot.children.mapNotNull { child -> child.toStudentOrNull() })
            }
            .addOnFailureListener { throwable ->
                logFailure("fetchStudents", throwable)
                continuation.resume(emptyList())
            }
    }

    private fun Student.toFirebaseMap(): Map<String, Any> = hashMapOf(
        "id" to id,
        "name" to name,
        "className" to className,
        "email" to email,
        "password" to password,
        "mobileNo" to mobileNo,
        "abcId" to abcId,
        "enrollmentNo" to enrollmentNo,
        "rollNo" to rollNo,
        "division" to division,
        "branch" to branch
    )

    private fun StudentAttendanceRecord.toFirebaseMap(date: String): Map<String, Any> = hashMapOf(
        "studentId" to id,
        "name" to name,
        "className" to className,
        "rollNo" to rollNo,
        "email" to email,
        "division" to division,
        "date" to date,
        "status" to status
    )

    private fun logFailure(action: String, throwable: Throwable) {
        Log.w(TAG, "Firebase sync failed for $action", throwable)
    }

    private fun DataSnapshot.toStudentOrNull(): Student? {
        val id = child("id").getValue(Int::class.java) ?: key?.toIntOrNull() ?: 0
        val name = child("name").getValue(String::class.java).orEmpty().trim()
        val className = child("className").getValue(String::class.java).orEmpty().trim()
        if (name.isEmpty() || className.isEmpty()) {
            return null
        }

        return Student(
            id = id,
            name = name,
            className = className,
            email = child("email").getValue(String::class.java).orEmpty(),
            password = child("password").getValue(String::class.java).orEmpty(),
            mobileNo = child("mobileNo").getValue(String::class.java).orEmpty(),
            abcId = child("abcId").getValue(String::class.java).orEmpty(),
            enrollmentNo = child("enrollmentNo").getValue(String::class.java).orEmpty(),
            rollNo = child("rollNo").getValue(String::class.java).orEmpty(),
            division = child("division").getValue(String::class.java).orEmpty(),
            branch = child("branch").getValue(String::class.java).orEmpty()
        )
    }

    companion object {
        private const val TAG = "FirebaseSyncService"
        private const val DATABASE_URL = "https://studentattendancemanagem-17a73-default-rtdb.firebaseio.com/"
        private const val ROOT_NODE = "attendance_app"
        private const val STUDENTS_NODE = "students"
        private const val ATTENDANCE_NODE = "attendance"
    }
}
