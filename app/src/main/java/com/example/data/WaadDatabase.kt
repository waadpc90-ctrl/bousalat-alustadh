package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val section: String,
    val phone: String,
    val deptName: String,
    val stageName: String,
    val tagsJson: String = "[]"
)

@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey val key: String, // "${studentId}_${subject}"
    val v1: String = "",
    val v2: String = "",
    val v3: String = "",
    val v4: String = "",
    val v5: String = "",
    val v6: String = "",
    val mid: String = "",
    val v7: String = "",
    val v8: String = "",
    val v9: String = "",
    val v10: String = "",
    val v11: String = "",
    val v12: String = "",
    val finalExam: String = "",
    val secondRound: String = "",
    val qarar: Int = 0
)

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey val key: String, // "${studentId}_${date}"
    val status: String // "present", "absent", "excused"
)

@Dao
interface WaadDao {
    @Query("SELECT * FROM students")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudentById(studentId: String)

    @Query("DELETE FROM students WHERE id IN (:studentIds)")
    suspend fun deleteStudentsByIds(studentIds: List<String>)

    @Query("DELETE FROM students WHERE deptName = :deptName")
    suspend fun deleteStudentsByDept(deptName: String)

    @Query("DELETE FROM students WHERE deptName = :deptName AND stageName = :stageName")
    suspend fun deleteStudentsByStage(deptName: String, stageName: String)

    @Query("DELETE FROM students")
    suspend fun clearAllStudents()

    @Query("SELECT * FROM grades")
    fun getAllGrades(): Flow<List<GradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: GradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEntity>)

    @Query("DELETE FROM grades WHERE `key` LIKE :prefix || '%'")
    suspend fun deleteGradesByPrefix(prefix: String)

    @Query("DELETE FROM grades")
    suspend fun clearAllGrades()

    @Query("SELECT * FROM attendance")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendances(attendances: List<AttendanceEntity>)

    @Query("DELETE FROM attendance WHERE `key` LIKE :prefix || '%'")
    suspend fun deleteAttendanceByPrefix(prefix: String)

    @Query("DELETE FROM attendance")
    suspend fun clearAllAttendance()
}

@Database(
    entities = [StudentEntity::class, GradeEntity::class, AttendanceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WaadDatabase : RoomDatabase() {
    abstract fun waadDao(): WaadDao

    companion object {
        @Volatile
        private var INSTANCE: WaadDatabase? = null

        fun getInstance(context: Context): WaadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WaadDatabase::class.java,
                    "waad_smart_v180.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
