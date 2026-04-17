package com.harsh.attandancesystem.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.OnConflictStrategy

@Dao
interface StudentDao {

    @Query("SELECT * FROM students ORDER BY id ASC")
    fun getAllStudents(): LiveData<List<Student>>

    @Query("SELECT COUNT(*) FROM students")
    suspend fun getStudentCount(): Int

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: Int): Student?

    @Query("SELECT * FROM students WHERE LOWER(email) = LOWER(:email) AND password = :password LIMIT 1")
    fun getStudentByCredentials(email: String, password: String): Student?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(students: List<Student>): List<Long>

    @Update
    suspend fun update(student: Student)

    @Delete
    suspend fun delete(student: Student)
}
