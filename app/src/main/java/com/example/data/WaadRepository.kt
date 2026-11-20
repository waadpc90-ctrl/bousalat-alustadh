package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppSettings
import com.example.model.DeptConfig
import com.example.model.ScheduleNotificationSettings
import com.example.model.ScheduledClass
import com.example.model.SectionCurriculumNote
import com.example.model.StageConfig
import com.example.model.Student
import com.example.model.SubjectGrades
import com.example.model.WaadCalculator
import com.example.update.DriveUpdateConfig
import com.example.update.GoogleDriveUpdateManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class WaadRepository(
    private val dao: WaadDao,
    private val prefs: SharedPreferences
) {
    val studentsFlow: Flow<List<Student>> = dao.getAllStudents().map { list ->
        list.map {
            Student(
                id = it.id,
                name = it.name,
                section = it.section,
                phone = it.phone,
                deptName = it.deptName,
                stageName = it.stageName
            )
        }
    }

    val studentTagsFlow: Flow<Map<String, List<String>>> = dao.getAllStudents().map { list ->
        val map = mutableMapOf<String, List<String>>()
        for (entity in list) {
            val tags = parseJsonStringList(entity.tagsJson)
            if (tags.isNotEmpty()) {
                map[entity.id] = tags
            }
        }
        map
    }

    val gradesFlow: Flow<Map<String, SubjectGrades>> = dao.getAllGrades().map { list ->
        list.associate { entity ->
            entity.key to SubjectGrades(
                v1 = entity.v1,
                v2 = entity.v2,
                v3 = entity.v3,
                v4 = entity.v4,
                v5 = entity.v5,
                v6 = entity.v6,
                mid = entity.mid,
                v7 = entity.v7,
                v8 = entity.v8,
                v9 = entity.v9,
                v10 = entity.v10,
                v11 = entity.v11,
                v12 = entity.v12,
                finalExam = entity.finalExam,
                secondRound = entity.secondRound,
                qarar = entity.qarar
            )
        }
    }

    val attendanceFlow: Flow<Map<String, String>> = dao.getAllAttendance().map { list ->
        list.associate { it.key to it.status }
    }

    suspend fun ensureInitialDataSeeded() {
        val seeded = prefs.getBoolean("WAAD_INITIAL_SEED_DONE", false)
        if (seeded) return

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val sampleStudents = listOf(
            StudentEntity(
                id = "std_101",
                name = "أحمد محمد علي الكعبي",
                section = "أ",
                phone = "07701234567",
                deptName = "الابتدائية",
                stageName = "الخامس الابتدائي",
                tagsJson = JSONArray(listOf("متفوق 🏆", "مميز ⭐")).toString()
            ),
            StudentEntity(
                id = "std_102",
                name = "زهراء حيدر كاظم",
                section = "أ",
                phone = "07801122334",
                deptName = "الابتدائية",
                stageName = "الخامس الابتدائي",
                tagsJson = JSONArray(listOf("مميز ⭐")).toString()
            ),
            StudentEntity(
                id = "std_103",
                name = "مصطفى عبد الله جاسم",
                section = "ب",
                phone = "07719988776",
                deptName = "الابتدائية",
                stageName = "الخامس الابتدائي",
                tagsJson = JSONArray(listOf("يحتاج متابعة ⚠️")).toString()
            ),
            StudentEntity(
                id = "std_104",
                name = "علي حسين فاضل",
                section = "أ",
                phone = "07723344556",
                deptName = "الابتدائية",
                stageName = "الأول الابتدائي",
                tagsJson = JSONArray(listOf("متفوق 🏆")).toString()
            ),
            StudentEntity(
                id = "std_105",
                name = "فاطمة عقيل رحيم",
                section = "أ",
                phone = "07814455667",
                deptName = "الابتدائية",
                stageName = "الأول الابتدائي",
                tagsJson = JSONArray(listOf("مميز ⭐")).toString()
            ),
            StudentEntity(
                id = "std_106",
                name = "حسنين طارق محمود",
                section = "ب",
                phone = "07735566778",
                deptName = "الابتدائية",
                stageName = "الأول الابتدائي",
                tagsJson = JSONArray(listOf("يحتاج متابعة ⚠️")).toString()
            ),
            StudentEntity(
                id = "std_107",
                name = "يوسف عمار سعدون",
                section = "أ",
                phone = "07708877665",
                deptName = "المتوسطة",
                stageName = "الأول المتوسط",
                tagsJson = JSONArray(listOf("متفوق 🏆")).toString()
            ),
            StudentEntity(
                id = "std_108",
                name = "مريم سلام عبد الكريم",
                section = "أ",
                phone = "07805544332",
                deptName = "الإعدادية",
                stageName = "الرابع العلمي",
                tagsJson = JSONArray(listOf("متفوق 🏆", "مميز ⭐")).toString()
            )
        )

        val sampleGrades = listOf(
            // الأول الابتدائي (نظام من 10)
            GradeEntity(
                key = "std_104_رياضيات",
                v1 = "3", v2 = "3", v3 = "4",
                v4 = "3", v5 = "3", v6 = "3",
                mid = "10",
                v7 = "3", v8 = "3", v9 = "4",
                v10 = "3", v11 = "3", v12 = "4",
                finalExam = "10"
            ),
            GradeEntity(
                key = "std_104_قراءة",
                v1 = "3", v2 = "3", v3 = "3",
                v4 = "3", v5 = "3", v6 = "4",
                mid = "9",
                v7 = "3", v8 = "3", v9 = "3",
                v10 = "3", v11 = "3", v12 = "4",
                finalExam = "10"
            ),
            GradeEntity(
                key = "std_105_رياضيات",
                v1 = "3", v2 = "2", v3 = "3",
                v4 = "3", v5 = "3", v6 = "3",
                mid = "9",
                v7 = "3", v8 = "3", v9 = "3",
                v10 = "3", v11 = "2", v12 = "3",
                finalExam = "9"
            ),
            GradeEntity(
                key = "std_106_رياضيات",
                v1 = "1", v2 = "1", v3 = "2",
                v4 = "1", v5 = "1", v6 = "2",
                mid = "4",
                v7 = "1", v8 = "1", v9 = "2",
                v10 = "1", v11 = "2", v12 = "1",
                finalExam = "4",
                secondRound = "6"
            ),
            // الخامس الابتدائي (نظام من 100)
            GradeEntity(
                key = "std_101_رياضيات",
                v1 = "30", v2 = "30", v3 = "35",
                v4 = "30", v5 = "30", v6 = "34",
                mid = "94",
                v7 = "30", v8 = "30", v9 = "36",
                v10 = "30", v11 = "30", v12 = "35",
                finalExam = "96"
            ),
            GradeEntity(
                key = "std_102_رياضيات",
                v1 = "25", v2 = "25", v3 = "32",
                v4 = "26", v5 = "26", v6 = "33",
                mid = "86",
                v7 = "25", v8 = "28", v9 = "32",
                v10 = "27", v11 = "27", v12 = "34",
                finalExam = "88"
            ),
            GradeEntity(
                key = "std_103_رياضيات",
                v1 = "12", v2 = "14", v3 = "18",
                v4 = "15", v5 = "12", v6 = "19",
                mid = "45",
                v7 = "14", v8 = "14", v9 = "18",
                v10 = "15", v11 = "14", v12 = "18",
                finalExam = "44",
                qarar = 5
            )
        )

        val sampleAttendance = listOf(
            AttendanceEntity("${sampleStudents[0].id}_$today", "present"),
            AttendanceEntity("${sampleStudents[1].id}_$today", "present"),
            AttendanceEntity("${sampleStudents[2].id}_$today", "absent"),
            AttendanceEntity("${sampleStudents[3].id}_$today", "present"),
            AttendanceEntity("${sampleStudents[4].id}_$today", "present"),
            AttendanceEntity("${sampleStudents[5].id}_$today", "excused")
        )

        dao.insertStudents(sampleStudents)
        dao.insertGrades(sampleGrades)
        dao.insertAttendances(sampleAttendance)

        // Start with an empty/vacant schedule on first app open
        saveSchedule(emptyMap())

        prefs.edit().putBoolean("WAAD_INITIAL_SEED_DONE", true).apply()
        prefs.edit().putBoolean("WAAD_SCHEDULE_ENRICHED_V2", true).apply()
        prefs.edit().putBoolean("WAAD_EMPTY_SCHEDULE_MIGRATED_V1", true).apply()
    }

    fun ensureRichScheduleIfMinimal() {
        if (!prefs.getBoolean("WAAD_EMPTY_SCHEDULE_MIGRATED_V1", false)) {
            val current = loadSchedule()
            val demoTitles = setOf(
                "الأعداد والجمع التفاعلي",
                "الكسور العشرية والعمليات",
                "الطاقة المتجددة والتجارب",
                "قواعد اللغة العربية والإعراب",
                "قوانين الحركة والميكانيك",
                "الدرس الأساسي والتطبيقات",
                "حل تمارين ومراجعة",
                "حصة دراسية منهجية"
            )
            if (current.isNotEmpty() && current.values.all { it.lessonTitle in demoTitles }) {
                saveSchedule(emptyMap())
            }
            prefs.edit().putBoolean("WAAD_EMPTY_SCHEDULE_MIGRATED_V1", true).apply()
        }
        prefs.edit().putBoolean("WAAD_SCHEDULE_ENRICHED_V2", true).apply()
    }

    private fun buildDefaultRichSchedule(): Map<String, ScheduledClass> {
        val defaultSched = mutableMapOf<String, ScheduledClass>()
        for (day in WaadCalculator.WEEK_DAYS) {
            defaultSched["${day}_1"] = ScheduledClass(
                deptIdx = 0,
                stageIdx = 0,
                section = "أ",
                subject = "رياضيات",
                lessonTitle = "الأعداد والجمع التفاعلي",
                startTime = "08:00",
                endTime = "08:45",
                notifyEnabled = true
            )
            defaultSched["${day}_2"] = ScheduledClass(
                deptIdx = 0,
                stageIdx = 1,
                section = "أ",
                subject = "رياضيات",
                lessonTitle = "الكسور العشرية والعمليات",
                startTime = "08:50",
                endTime = "09:35",
                notifyEnabled = true
            )
            defaultSched["${day}_3"] = ScheduledClass(
                deptIdx = 0,
                stageIdx = 1,
                section = "ب",
                subject = "علوم",
                lessonTitle = "الطاقة المتجددة والتجارب",
                startTime = "09:40",
                endTime = "10:25",
                notifyEnabled = true
            )
            defaultSched["${day}_4"] = ScheduledClass(
                deptIdx = 1,
                stageIdx = 0,
                section = "أ",
                subject = "عربي",
                lessonTitle = "قواعد اللغة العربية والإعراب",
                startTime = "10:45",
                endTime = "11:30",
                notifyEnabled = true
            )
            defaultSched["${day}_5"] = ScheduledClass(
                deptIdx = 2,
                stageIdx = 0,
                section = "أ",
                subject = "فيزياء",
                lessonTitle = "قوانين الحركة والميكانيك",
                startTime = "11:35",
                endTime = "12:20",
                notifyEnabled = true
            )
        }
        return defaultSched
    }

    suspend fun saveStudent(student: Student, tags: List<String>) {
        dao.insertStudent(
            StudentEntity(
                id = student.id,
                name = student.name,
                section = student.section,
                phone = student.phone,
                deptName = student.deptName,
                stageName = student.stageName,
                tagsJson = JSONArray(tags).toString()
            )
        )
    }

    suspend fun saveStudentsBulk(students: List<Student>) {
        dao.insertStudents(
            students.map {
                StudentEntity(
                    id = it.id,
                    name = it.name,
                    section = it.section,
                    phone = it.phone,
                    deptName = it.deptName,
                    stageName = it.stageName,
                    tagsJson = "[]"
                )
            }
        )
    }

    suspend fun deleteStudent(studentId: String) {
        dao.deleteStudentById(studentId)
        dao.deleteGradesByPrefix("${studentId}_")
        dao.deleteAttendanceByPrefix("${studentId}_")
    }

    suspend fun deleteStudentsBulk(studentIds: List<String>) {
        if (studentIds.isEmpty()) return
        dao.deleteStudentsByIds(studentIds)
        for (id in studentIds) {
            dao.deleteGradesByPrefix("${id}_")
            dao.deleteAttendanceByPrefix("${id}_")
        }
    }

    suspend fun deleteStudentsByDept(deptName: String, studentIds: List<String>) {
        dao.deleteStudentsByDept(deptName)
        for (id in studentIds) {
            dao.deleteGradesByPrefix("${id}_")
            dao.deleteAttendanceByPrefix("${id}_")
        }
    }

    suspend fun deleteStudentsByStage(deptName: String, stageName: String, studentIds: List<String>) {
        dao.deleteStudentsByStage(deptName, stageName)
        for (id in studentIds) {
            dao.deleteGradesByPrefix("${id}_")
            dao.deleteAttendanceByPrefix("${id}_")
        }
    }

    suspend fun saveGrade(key: String, g: SubjectGrades) {
        dao.insertGrade(
            GradeEntity(
                key = key,
                v1 = g.v1,
                v2 = g.v2,
                v3 = g.v3,
                v4 = g.v4,
                v5 = g.v5,
                v6 = g.v6,
                mid = g.mid,
                v7 = g.v7,
                v8 = g.v8,
                v9 = g.v9,
                v10 = g.v10,
                v11 = g.v11,
                v12 = g.v12,
                finalExam = g.finalExam,
                secondRound = g.secondRound,
                qarar = g.qarar
            )
        )
    }

    suspend fun saveAttendance(key: String, status: String) {
        dao.insertAttendance(AttendanceEntity(key = key, status = status))
    }

    suspend fun saveAttendancesBulk(entries: Map<String, String>) {
        dao.insertAttendances(
            entries.map { (k, v) -> AttendanceEntity(key = k, status = v) }
        )
    }

    suspend fun resetFullAcademicYear(
        deleteStudents: Boolean = true,
        currentStudents: List<Student> = emptyList()
    ) {
        dao.clearAllGrades()
        dao.clearAllAttendance()
        saveMissingHomework(emptyMap())
        saveSectionNotes(emptyMap())
        prefs.edit().putBoolean("WAAD_INITIAL_SEED_DONE", true).apply()
        if (deleteStudents) {
            dao.clearAllStudents()
        } else {
            // Reset tags on students only if keeping student names
            dao.insertStudents(
                currentStudents.map {
                    StudentEntity(
                        id = it.id,
                        name = it.name,
                        section = it.section,
                        phone = it.phone,
                        deptName = it.deptName,
                        stageName = it.stageName,
                        tagsJson = "[]"
                    )
                }
            )
        }
    }

    suspend fun resetGradesAndAttendance(currentStudents: List<Student>) {
        resetFullAcademicYear(deleteStudents = true, currentStudents = currentStudents)
    }

    fun loadDarkMode(): Boolean = prefs.getBoolean("WAAD_DARK_MODE", false)
    fun saveDarkMode(dark: Boolean) = prefs.edit().putBoolean("WAAD_DARK_MODE", dark).apply()

    fun loadSavedPin(): String = prefs.getString("WAAD_PIN_CODE", "") ?: ""
    fun savePin(pin: String) = prefs.edit().putString("WAAD_PIN_CODE", pin).apply()

    fun getOrCreateDeviceId(): String {
        var devId = prefs.getString("WAAD_DEVICE_ID", null)
        if (devId.isNullOrBlank()) {
            devId = Random.nextInt(10000, 99999).toString()
            prefs.edit().putString("WAAD_DEVICE_ID", devId).apply()
        }
        return devId
    }

    fun getInstallDateMs(): Long {
        var installDate = prefs.getLong("WAAD_INSTALL_DATE", 0L)
        if (installDate == 0L) {
            installDate = System.currentTimeMillis()
            prefs.edit().putLong("WAAD_INSTALL_DATE", installDate).apply()
        }
        return installDate
    }

    fun getLicenseToken(): String = prefs.getString("WAAD_LICENSE_TOKEN", "") ?: ""
    fun getActivationDateMs(): Long = prefs.getLong("WAAD_ACTIVATION_DATE", 0L)

    fun saveActivation(token: String, dateMs: Long) {
        prefs.edit()
            .putString("WAAD_LICENSE_TOKEN", token.trim().uppercase())
            .putLong("WAAD_ACTIVATION_DATE", dateMs)
            .apply()
    }

    fun loadSettings(): AppSettings {
        val raw = prefs.getString("WAAD_SETTINGS_V180", null) ?: return AppSettings()
        return try {
            val parsed = parseSettingsJson(JSONObject(raw))
            if (!prefs.getBoolean("WAAD_SETTINGS_MIGRATED_V34", false)) {
                saveSettings(parsed)
                prefs.edit().putBoolean("WAAD_SETTINGS_MIGRATED_V34", true).apply()
            }
            parsed
        } catch (e: Exception) {
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        val json = serializeSettingsJson(settings)
        prefs.edit().putString("WAAD_SETTINGS_V180", json.toString()).apply()
    }

    fun loadNotificationSettings(): ScheduleNotificationSettings {
        val enabled = prefs.getBoolean("WAAD_NOTIF_ENABLED", true)
        val lead = prefs.getInt("WAAD_NOTIF_LEAD_MINUTES", 5)
        return ScheduleNotificationSettings(enabled = enabled, leadMinutes = lead)
    }

    fun saveNotificationSettings(settings: ScheduleNotificationSettings) {
        prefs.edit()
            .putBoolean("WAAD_NOTIF_ENABLED", settings.enabled)
            .putInt("WAAD_NOTIF_LEAD_MINUTES", settings.leadMinutes)
            .apply()
    }

    fun loadSchedule(): Map<String, ScheduledClass> {
        val raw = prefs.getString("WAAD_SCHEDULE_V180", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, ScheduledClass>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val item = obj.optJSONObject(k) ?: continue
                val lessonNum = k.substringAfter("_").toIntOrNull() ?: 1
                val (defStart, defEnd) = WaadCalculator.getDefaultLessonTimes(lessonNum)
                result[k] = ScheduledClass(
                    deptIdx = item.optInt("deptIdx", 0),
                    stageIdx = item.optInt("stageIdx", 0),
                    section = item.optString("section", "أ"),
                    subject = item.optString("subject", ""),
                    lessonTitle = item.optString("lessonTitle", ""),
                    startTime = item.optString("startTime", defStart).ifBlank { defStart },
                    endTime = item.optString("endTime", defEnd).ifBlank { defEnd },
                    notifyEnabled = item.optBoolean("notifyEnabled", true)
                )
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveSchedule(schedule: Map<String, ScheduledClass>) {
        val obj = JSONObject()
        for ((k, v) in schedule) {
            val item = JSONObject()
            item.put("deptIdx", v.deptIdx)
            item.put("stageIdx", v.stageIdx)
            item.put("section", v.section)
            item.put("subject", v.subject)
            item.put("lessonTitle", v.lessonTitle)
            item.put("startTime", v.startTime)
            item.put("endTime", v.endTime)
            item.put("notifyEnabled", v.notifyEnabled)
            obj.put(k, item)
        }
        prefs.edit().putString("WAAD_SCHEDULE_V180", obj.toString()).apply()
    }

    fun loadSectionNotes(): Map<String, SectionCurriculumNote> {
        val raw = prefs.getString("WAAD_SECTION_NOTES_V1", null)
        if (raw.isNullOrBlank()) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val defaults = mapOf(
                WaadCalculator.buildSectionCurriculumKey("الابتدائية", "الأول الابتدائي", "أ", "رياضيات") to SectionCurriculumNote(
                    lastTopic = "الأعداد حتى 20 والجمع التفاعلي (ص 28)",
                    homework = "حل تمارين الكتاب صفحة 29",
                    updatedAt = today
                ),
                WaadCalculator.buildSectionCurriculumKey("الابتدائية", "الأول الابتدائي", "ب", "رياضيات") to SectionCurriculumNote(
                    lastTopic = "الأعداد حتى 15 والترتيب التصاعدي (ص 24)",
                    homework = "كتابة الأعداد من 10 إلى 15 في الدفتر",
                    updatedAt = today
                ),
                WaadCalculator.buildSectionCurriculumKey("الابتدائية", "الخامس الابتدائي", "أ", "رياضيات") to SectionCurriculumNote(
                    lastTopic = "الكسور العشرية ومقارنتها (ص 45)",
                    homework = "حل تمارين (4-1) صفحة 46",
                    updatedAt = today
                ),
                WaadCalculator.buildSectionCurriculumKey("الابتدائية", "الخامس الابتدائي", "ب", "رياضيات") to SectionCurriculumNote(
                    lastTopic = "تحويل الكسور الاعتيادية إلى عشرية (ص 42)",
                    homework = "حل الأسئلة 1 إلى 5 صفحة 43",
                    updatedAt = today
                )
            )
            saveSectionNotes(defaults)
            return defaults
        }
        return try {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, SectionCurriculumNote>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val item = obj.optJSONObject(k) ?: continue
                result[k] = SectionCurriculumNote(
                    lastTopic = item.optString("lastTopic", ""),
                    homework = item.optString("homework", ""),
                    updatedAt = item.optString("updatedAt", "")
                )
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveSectionNotes(notes: Map<String, SectionCurriculumNote>) {
        val obj = JSONObject()
        for ((k, v) in notes) {
            if (v.lastTopic.isBlank() && v.homework.isBlank()) continue
            val item = JSONObject()
            item.put("lastTopic", v.lastTopic)
            item.put("homework", v.homework)
            item.put("updatedAt", v.updatedAt)
            obj.put(k, item)
        }
        prefs.edit().putString("WAAD_SECTION_NOTES_V1", obj.toString()).apply()
    }

    fun loadMissingHomework(): Map<String, Int> {
        val raw = prefs.getString("WAAD_MISSING_HW_V1", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val count = obj.optInt(k, 0)
                if (count > 0) {
                    result[k] = count
                }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveMissingHomework(map: Map<String, Int>) {
        val obj = JSONObject()
        for ((k, count) in map) {
            if (count > 0) {
                obj.put(k, count)
            }
        }
        prefs.edit().putString("WAAD_MISSING_HW_V1", obj.toString()).apply()
    }

    fun exportFullBackupJson(
        students: List<Student>,
        grades: Map<String, SubjectGrades>,
        attendance: Map<String, String>,
        studentTags: Map<String, List<String>>,
        teacherSchedule: Map<String, ScheduledClass>,
        settings: AppSettings,
        sectionNotes: Map<String, SectionCurriculumNote> = emptyMap(),
        missingHomework: Map<String, Int> = emptyMap()
    ): String {
        val root = JSONObject()

        val stdArr = JSONArray()
        for (s in students) {
            val sObj = JSONObject()
            sObj.put("id", s.id)
            sObj.put("name", s.name)
            sObj.put("section", s.section)
            sObj.put("phone", s.phone)
            sObj.put("deptName", s.deptName)
            sObj.put("stageName", s.stageName)
            sObj.put("dept", s.deptName)
            sObj.put("stage", s.stageName)
            stdArr.put(sObj)
        }
        root.put("students", stdArr)

        val gradesObj = JSONObject()
        for ((k, g) in grades) {
            val gObj = JSONObject()
            if (g.v1.isNotEmpty()) gObj.put("v1", g.v1)
            if (g.v2.isNotEmpty()) gObj.put("v2", g.v2)
            if (g.v3.isNotEmpty()) gObj.put("v3", g.v3)
            if (g.v4.isNotEmpty()) gObj.put("v4", g.v4)
            if (g.v5.isNotEmpty()) gObj.put("v5", g.v5)
            if (g.v6.isNotEmpty()) gObj.put("v6", g.v6)
            if (g.mid.isNotEmpty()) gObj.put("mid", g.mid)
            if (g.v7.isNotEmpty()) gObj.put("v7", g.v7)
            if (g.v8.isNotEmpty()) gObj.put("v8", g.v8)
            if (g.v9.isNotEmpty()) gObj.put("v9", g.v9)
            if (g.v10.isNotEmpty()) gObj.put("v10", g.v10)
            if (g.v11.isNotEmpty()) gObj.put("v11", g.v11)
            if (g.v12.isNotEmpty()) gObj.put("v12", g.v12)
            if (g.finalExam.isNotEmpty()) {
                gObj.put("final", g.finalExam)
                gObj.put("finalExam", g.finalExam)
            }
            if (g.secondRound.isNotEmpty()) gObj.put("secondRound", g.secondRound)
            if (g.qarar != 0) gObj.put("qarar", g.qarar)
            gradesObj.put(k, gObj)
        }
        root.put("grades", gradesObj)

        val attObj = JSONObject()
        for ((k, status) in attendance) {
            attObj.put(k, status)
        }
        root.put("attendance", attObj)

        val tagsObj = JSONObject()
        for ((k, list) in studentTags) {
            tagsObj.put(k, JSONArray(list))
        }
        root.put("studentTags", tagsObj)

        val schedObj = JSONObject()
        for ((k, v) in teacherSchedule) {
            val item = JSONObject()
            item.put("deptIdx", v.deptIdx)
            item.put("stageIdx", v.stageIdx)
            item.put("section", v.section)
            item.put("subject", v.subject)
            item.put("lessonTitle", v.lessonTitle)
            item.put("startTime", v.startTime)
            item.put("endTime", v.endTime)
            item.put("notifyEnabled", v.notifyEnabled)
            schedObj.put(k, item)
        }
        root.put("teacherSchedule", schedObj)

        val notesObj = JSONObject()
        for ((k, v) in sectionNotes) {
            if (v.lastTopic.isBlank() && v.homework.isBlank()) continue
            val item = JSONObject()
            item.put("lastTopic", v.lastTopic)
            item.put("homework", v.homework)
            item.put("updatedAt", v.updatedAt)
            notesObj.put(k, item)
        }
        root.put("sectionNotes", notesObj)

        val hwObj = JSONObject()
        for ((k, c) in missingHomework) {
            if (c > 0) hwObj.put(k, c)
        }
        root.put("missingHomework", hwObj)

        root.put("settings", serializeSettingsJson(settings))
        return root.toString(2)
    }

    suspend fun importFullBackupJson(jsonString: String): Pair<AppSettings, Map<String, ScheduledClass>>? {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("students")) return null

            val tagsMap = mutableMapOf<String, List<String>>()
            root.optJSONObject("studentTags")?.let { tObj ->
                val keys = tObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val arr = tObj.optJSONArray(k) ?: continue
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        list.add(arr.optString(i))
                    }
                    tagsMap[k] = list
                }
            }

            val stdArr = root.getJSONArray("students")
            val stdEntities = mutableListOf<StudentEntity>()
            for (i in 0 until stdArr.length()) {
                val sObj = stdArr.optJSONObject(i) ?: continue
                val id = sObj.optString("id")
                if (id.isBlank()) continue
                val resolvedDept = sObj.optString("deptName", "").ifBlank { sObj.optString("dept", "") }
                val resolvedStage = sObj.optString("stageName", "").ifBlank { sObj.optString("stage", "") }
                stdEntities.add(
                    StudentEntity(
                        id = id,
                        name = sObj.optString("name"),
                        section = sObj.optString("section", "أ"),
                        phone = sObj.optString("phone", ""),
                        deptName = resolvedDept,
                        stageName = resolvedStage,
                        tagsJson = JSONArray(tagsMap[id] ?: emptyList<String>()).toString()
                    )
                )
            }

            val gradeEntities = mutableListOf<GradeEntity>()
            root.optJSONObject("grades")?.let { gRoot ->
                val keys = gRoot.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val gObj = gRoot.optJSONObject(k) ?: continue
                    val resolvedFinal = gObj.optString("final", "").ifBlank { gObj.optString("finalExam", "") }
                    val resolvedQarar = gObj.optString("qarar", "").trim().toIntOrNull() ?: gObj.optInt("qarar", 0)
                    gradeEntities.add(
                        GradeEntity(
                            key = k,
                            v1 = gObj.optString("v1", ""),
                            v2 = gObj.optString("v2", ""),
                            v3 = gObj.optString("v3", ""),
                            v4 = gObj.optString("v4", ""),
                            v5 = gObj.optString("v5", ""),
                            v6 = gObj.optString("v6", ""),
                            mid = gObj.optString("mid", ""),
                            v7 = gObj.optString("v7", ""),
                            v8 = gObj.optString("v8", ""),
                            v9 = gObj.optString("v9", ""),
                            v10 = gObj.optString("v10", ""),
                            v11 = gObj.optString("v11", ""),
                            v12 = gObj.optString("v12", ""),
                            finalExam = resolvedFinal,
                            secondRound = gObj.optString("secondRound", ""),
                            qarar = resolvedQarar
                        )
                    )
                }
            }

            val attEntities = mutableListOf<AttendanceEntity>()
            root.optJSONObject("attendance")?.let { aRoot ->
                val keys = aRoot.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    attEntities.add(AttendanceEntity(key = k, status = aRoot.optString(k, "present")))
                }
            }

            dao.clearAllStudents()
            dao.clearAllGrades()
            dao.clearAllAttendance()

            dao.insertStudents(stdEntities)
            dao.insertGrades(gradeEntities)
            dao.insertAttendances(attEntities)

            val newSettings = root.optJSONObject("settings")?.let { parseSettingsJson(it) } ?: loadSettings()
            saveSettings(newSettings)

            val newSchedule = mutableMapOf<String, ScheduledClass>()
            root.optJSONObject("teacherSchedule")?.let { sRoot ->
                val keys = sRoot.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val item = sRoot.optJSONObject(k) ?: continue
                    val lessonNum = k.substringAfter("_").toIntOrNull() ?: 1
                    val (defStart, defEnd) = WaadCalculator.getDefaultLessonTimes(lessonNum)
                    newSchedule[k] = ScheduledClass(
                        deptIdx = item.optInt("deptIdx", 0),
                        stageIdx = item.optInt("stageIdx", 0),
                        section = item.optString("section", "أ"),
                        subject = item.optString("subject", ""),
                        lessonTitle = item.optString("lessonTitle", ""),
                        startTime = item.optString("startTime", defStart).ifBlank { defStart },
                        endTime = item.optString("endTime", defEnd).ifBlank { defEnd },
                        notifyEnabled = item.optBoolean("notifyEnabled", true)
                    )
                }
            }
            if (newSchedule.isNotEmpty()) {
                saveSchedule(newSchedule)
            }

            root.optJSONObject("sectionNotes")?.let { nRoot ->
                val notesMap = mutableMapOf<String, SectionCurriculumNote>()
                val keys = nRoot.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val item = nRoot.optJSONObject(k) ?: continue
                    notesMap[k] = SectionCurriculumNote(
                        lastTopic = item.optString("lastTopic", ""),
                        homework = item.optString("homework", ""),
                        updatedAt = item.optString("updatedAt", "")
                    )
                }
                saveSectionNotes(notesMap)
            }

            root.optJSONObject("missingHomework")?.let { hRoot ->
                val hwMap = mutableMapOf<String, Int>()
                val keys = hRoot.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val c = hRoot.optInt(k, 0)
                    if (c > 0) hwMap[k] = c
                }
                saveMissingHomework(hwMap)
            }

            Pair(newSettings, if (newSchedule.isNotEmpty()) newSchedule else loadSchedule())
        } catch (e: Exception) {
            null
        }
    }

    private fun parseJsonStringList(raw: String): List<String> {
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { idx -> arr.optString(idx) }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeSettingsJson(settings: AppSettings): JSONObject {
        val obj = JSONObject()
        obj.put("ministryName", settings.ministryName)
        obj.put("schoolName", settings.schoolName)
        obj.put("academicYear", settings.academicYear)
        obj.put("teacherName", settings.teacherName)

        val structArr = JSONArray()
        for (dept in settings.structure) {
            val dObj = JSONObject()
            dObj.put("name", dept.name)
            val stagesArr = JSONArray()
            for (stage in dept.stages) {
                val sObj = JSONObject()
                sObj.put("name", stage.name)
                sObj.put("maxGrade", stage.maxGrade)
                sObj.put("subjects", JSONArray(stage.subjects))
                sObj.put("sections", JSONArray(stage.sections))
                stagesArr.put(sObj)
            }
            dObj.put("stages", stagesArr)
            structArr.put(dObj)
        }
        obj.put("structure", structArr)
        return obj
    }

    private fun parseSettingsJson(obj: JSONObject): AppSettings {
        val defaults = AppSettings()
        val structArr = obj.optJSONArray("structure")
        val structure = if (structArr != null && structArr.length() > 0) {
            val depts = mutableListOf<DeptConfig>()
            for (i in 0 until structArr.length()) {
                val dObj = structArr.optJSONObject(i) ?: continue
                val stagesArr = dObj.optJSONArray("stages") ?: JSONArray()
                val stages = mutableListOf<StageConfig>()
                for (j in 0 until stagesArr.length()) {
                    val sObj = stagesArr.optJSONObject(j) ?: continue
                    val subsArr = sObj.optJSONArray("subjects") ?: JSONArray()
                    val secsArr = sObj.optJSONArray("sections") ?: JSONArray()
                    stages.add(
                        StageConfig(
                            name = sObj.optString("name", "عام"),
                            maxGrade = sObj.optInt("maxGrade", 100),
                            subjects = List(subsArr.length()) { subsArr.optString(it) }.filter { it.isNotBlank() },
                            sections = List(secsArr.length()) { secsArr.optString(it) }.filter { it.isNotBlank() }.ifEmpty { listOf("أ") }
                        )
                    )
                }
                depts.add(DeptConfig(name = dObj.optString("name", "عام"), stages = stages))
            }
            // Merge any newly added default stages or subjects (e.g. السادس الابتدائي / التربية الإسلامية)
            defaults.structure.forEach { defaultDept ->
                val existingDeptIdx = depts.indexOfFirst { it.name == defaultDept.name }
                if (existingDeptIdx == -1) {
                    depts.add(defaultDept)
                } else {
                    val existingDept = depts[existingDeptIdx]
                    val mergedStages = existingDept.stages.toMutableList()
                    defaultDept.stages.forEach { defaultStage ->
                        val existingStageIdx = mergedStages.indexOfFirst { it.name == defaultStage.name }
                        if (existingStageIdx == -1) {
                            mergedStages.add(defaultStage)
                        } else {
                            val existingStage = mergedStages[existingStageIdx]
                            val mergedSubjects = (existingStage.subjects + defaultStage.subjects).distinct()
                            val mergedSections = (existingStage.sections + defaultStage.sections).distinct()
                            mergedStages[existingStageIdx] = existingStage.copy(
                                subjects = mergedSubjects,
                                sections = mergedSections
                            )
                        }
                    }
                    depts[existingDeptIdx] = existingDept.copy(stages = mergedStages)
                }
            }
            depts
        } else {
            defaults.structure
        }

        val rawSchool = obj.optString("schoolName", defaults.schoolName)
        val migratedSchool = if (
            rawSchool == "ثانوية الواعد النموذجية" ||
            rawSchool == "إعدادية الواعد الذكية" ||
            rawSchool.isBlank()
        ) {
            defaults.schoolName
        } else {
            rawSchool
        }
        val rawYear = obj.optString("academicYear", defaults.academicYear)
        val migratedYear = if (
            rawYear == "2024 - 2025" ||
            rawYear == "2023 - 2024" ||
            rawYear.isBlank()
        ) {
            defaults.academicYear
        } else {
            rawYear
        }
        val rawTeacher = obj.optString("teacherName", defaults.teacherName)
        val migratedTeacher = if (
            rawTeacher == "الأستاذ وعد" ||
            rawTeacher == "الأستاذ الواعد" ||
            rawTeacher.isBlank()
        ) {
            defaults.teacherName
        } else {
            rawTeacher
        }

        return AppSettings(
            structure = structure,
            ministryName = obj.optString("ministryName", defaults.ministryName).ifBlank { defaults.ministryName },
            schoolName = migratedSchool,
            academicYear = migratedYear,
            teacherName = migratedTeacher
        )
    }

    fun loadDriveUpdateConfig(): DriveUpdateConfig {
        val raw = prefs.getString("WAAD_DRIVE_UPDATE_CONFIG_V1", null) ?: return DriveUpdateConfig()
        return try {
            val obj = JSONObject(raw)
            DriveUpdateConfig(
                driveUrl = obj.optString("driveUrl", ""),
                targetVersionName = obj.optString("targetVersionName", GoogleDriveUpdateManager.CURRENT_APP_VERSION_NAME)
                    .ifBlank { GoogleDriveUpdateManager.CURRENT_APP_VERSION_NAME },
                targetVersionCode = obj.optInt("targetVersionCode", GoogleDriveUpdateManager.CURRENT_APP_VERSION_CODE),
                releaseNotes = obj.optString(
                    "releaseNotes",
                    "تحديث جديد لتطبيق بوصلة الأستاذ يتضمن أحدث الميزات والتحسينات في سجل الدرجات والحضور والغياب."
                ),
                showPromptOnStartup = obj.optBoolean("showPromptOnStartup", false),
                isMandatory = obj.optBoolean("isMandatory", false),
                lastBroadcastId = obj.optString("lastBroadcastId", ""),
                acknowledgedBroadcastId = obj.optString("acknowledgedBroadcastId", ""),
                acknowledgedDriveFileId = obj.optString("acknowledgedDriveFileId", ""),
                acknowledgedVersionName = obj.optString("acknowledgedVersionName", ""),
                acknowledgedVersionCode = obj.optInt("acknowledgedVersionCode", 0)
            )
        } catch (_: Exception) {
            DriveUpdateConfig()
        }
    }

    fun saveDriveUpdateConfig(config: DriveUpdateConfig, commitImmediately: Boolean = false) {
        val obj = JSONObject().apply {
            put("driveUrl", config.driveUrl.trim())
            put("targetVersionName", config.targetVersionName.trim().ifBlank { GoogleDriveUpdateManager.CURRENT_APP_VERSION_NAME })
            put("versionCode", config.targetVersionCode)
            put("targetVersionCode", config.targetVersionCode)
            put("releaseNotes", config.releaseNotes.trim())
            put("showPromptOnStartup", config.showPromptOnStartup)
            put("isMandatory", config.isMandatory)
            put("lastBroadcastId", config.lastBroadcastId.trim())
            put("acknowledgedBroadcastId", config.acknowledgedBroadcastId.trim())
            put("acknowledgedDriveFileId", config.acknowledgedDriveFileId.trim())
            put("acknowledgedVersionName", config.acknowledgedVersionName.trim())
            put("acknowledgedVersionCode", config.acknowledgedVersionCode)
        }
        val editor = prefs.edit().putString("WAAD_DRIVE_UPDATE_CONFIG_V1", obj.toString())
        if (commitImmediately) {
            editor.commit()
        } else {
            editor.apply()
        }
    }
}

