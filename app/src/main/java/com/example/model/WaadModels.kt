package com.example.model

import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class StageConfig(
    val name: String,
    val maxGrade: Int = 100,
    val subjects: List<String> = emptyList(),
    val sections: List<String> = listOf("أ")
)

data class DeptConfig(
    val name: String,
    val stages: List<StageConfig> = emptyList()
)

data class AppSettings(
    val structure: List<DeptConfig> = defaultStructure(),
    val ministryName: String = "وزارة التربية",
    val schoolName: String = "اعدادية الزيتون المهنية للبنين",
    val academicYear: String = "2025 - 2026",
    val teacherName: String = "الأستاذ : وعد بزيع"
) {
    companion object {
        fun defaultStructure(): List<DeptConfig> = listOf(
            DeptConfig(
                name = "الابتدائية",
                stages = listOf(
                    StageConfig(
                        name = "الأول الابتدائي",
                        maxGrade = 10,
                        subjects = listOf("رياضيات", "قراءة", "التربية الإسلامية"),
                        sections = listOf("أ", "ب", "ج")
                    ),
                    StageConfig(
                        name = "الخامس الابتدائي",
                        maxGrade = 100,
                        subjects = listOf("رياضيات", "علوم", "عربي", "التربية الإسلامية"),
                        sections = listOf("أ", "ب")
                    ),
                    StageConfig(
                        name = "السادس الابتدائي",
                        maxGrade = 100,
                        subjects = listOf("التربية الإسلامية", "عربي", "رياضيات", "علوم"),
                        sections = listOf("أ", "ب")
                    )
                )
            ),
            DeptConfig(
                name = "المتوسطة",
                stages = listOf(
                    StageConfig(
                        name = "الأول المتوسط",
                        maxGrade = 100,
                        subjects = listOf("عربي", "إنجليزي", "التربية الإسلامية"),
                        sections = listOf("أ", "ب")
                    )
                )
            ),
            DeptConfig(
                name = "الإعدادية",
                stages = listOf(
                    StageConfig(
                        name = "الرابع العلمي",
                        maxGrade = 100,
                        subjects = listOf("فيزياء", "كيمياء", "التربية الإسلامية"),
                        sections = listOf("أ")
                    )
                )
            )
        )
    }
}

data class Student(
    val id: String,
    val name: String,
    val section: String,
    val phone: String = "",
    val deptName: String,
    val stageName: String
)

data class SubjectGrades(
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
) {
    fun getField(fieldName: String): String = when (fieldName) {
        "v1" -> v1
        "v2" -> v2
        "v3" -> v3
        "v4" -> v4
        "v5" -> v5
        "v6" -> v6
        "mid" -> mid
        "v7" -> v7
        "v8" -> v8
        "v9" -> v9
        "v10" -> v10
        "v11" -> v11
        "v12" -> v12
        "final" -> finalExam
        "secondRound" -> secondRound
        else -> ""
    }

    fun withField(fieldName: String, value: String): SubjectGrades = when (fieldName) {
        "v1" -> copy(v1 = value)
        "v2" -> copy(v2 = value)
        "v3" -> copy(v3 = value)
        "v4" -> copy(v4 = value)
        "v5" -> copy(v5 = value)
        "v6" -> copy(v6 = value)
        "mid" -> copy(mid = value)
        "v7" -> copy(v7 = value)
        "v8" -> copy(v8 = value)
        "v9" -> copy(v9 = value)
        "v10" -> copy(v10 = value)
        "v11" -> copy(v11 = value)
        "v12" -> copy(v12 = value)
        "final" -> copy(finalExam = value)
        "secondRound" -> copy(secondRound = value)
        else -> this
    }
}

data class CalculatedResult(
    val m1: Int,
    val m2: Int,
    val avg1: Int,
    val mid: Int,
    val m3: Int,
    val m4: Int,
    val avg2: Int,
    val saei: Int,
    val finalExam: Int,
    val secondRoundExam: Int,
    val qarar: Int,
    val finalResult: Int,
    val secondRoundResult: Int?,
    val hasM1: Boolean,
    val hasM2: Boolean,
    val hasAvg1: Boolean,
    val hasMid: Boolean,
    val hasM3: Boolean,
    val hasM4: Boolean,
    val hasAvg2: Boolean,
    val hasSaei: Boolean,
    val hasFinal: Boolean,
    val hasFinalResult: Boolean,
    val isExempt: Boolean,
    val isFailedFirstRound: Boolean,
    val hasSecondRound: Boolean,
    val hasSecondRoundResult: Boolean
)

data class ScheduledClass(
    val deptIdx: Int = 0,
    val stageIdx: Int = 0,
    val section: String = "أ",
    val subject: String = "",
    val lessonTitle: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val notifyEnabled: Boolean = true
)

data class ScheduleNotificationSettings(
    val enabled: Boolean = true,
    val leadMinutes: Int = 5
)

data class LiveLessonInfo(
    val dayName: String,
    val lessonNum: Int,
    val lessonOrdinalName: String = "الحصة الأولى",
    val startTime: String = "08:00",
    val endTime: String = "08:45",
    val isLiveTimeWindow: Boolean = false,
    val scheduledClass: ScheduledClass?
)

data class StatsSummary(
    val total: Int,
    val activeCount: Int,
    val passed: Int,
    val failed: Int,
    val rate: String,
    val labelText: String
)

data class ActivationState(
    val isActivated: Boolean,
    val isExpired: Boolean,
    val remainingTrialDays: Int,
    val subRemainingDays: Int,
    val deviceID: String
)

data class BonusModalState(
    val isOpen: Boolean = false,
    val student: Student? = null,
    val subject: String = "",
    val monthField: String = "v1",
    val baseGrade: Int = 0,
    val otherFieldsSum: Int = 0,
    val selectedBonus: Int = 0
)

data class ShareModalState(
    val isOpen: Boolean = false,
    val student: Student? = null,
    val parentPhone: String = "",
    val reportScope: String = "single" // "single" or "all"
)

data class DiceModalState(
    val isOpen: Boolean = false,
    val selectedStudent: Student? = null,
    val isRolling: Boolean = false
)

data class DeleteStructTarget(
    val type: String, // "d", "s", "b", "sec"
    val idx: Int
)

data class SpeedConfig(
    val activeIndex: Int = 0,
    val activeTab: String = "m1", // m1, m2, mid, m3, m4, final, secondRound
    val activeSubFieldIdx: Int = 0 // 0, 1, 2
)

data class SectionCurriculumNote(
    val lastTopic: String = "",
    val homework: String = "",
    val updatedAt: String = ""
)

object WaadCalculator {
    const val ADMIN_WHATSAPP = "9647727486682"
    const val TRIAL_DAYS = 7
    const val SUBSCRIPTION_DAYS = 365
    const val INTISAB_TAG = "انتساب 🏠"

    val BEHAVIOR_TAGS = listOf(
        INTISAB_TAG,
        "مميز ⭐",
        "متفوق 🏆",
        "يحتاج متابعة ⚠️",
        "مشاغب 🛑",
        "غير متفاعل 💤"
    )

    fun isStudentIntisab(studentId: String, studentTags: Map<String, List<String>>): Boolean {
        return studentTags[studentId]?.any { it.contains("انتساب") } == true
    }

    fun resolveEffectiveAttendanceStatus(
        studentId: String,
        dateStr: String,
        attendance: Map<String, String>,
        studentTags: Map<String, List<String>>
    ): String {
        val raw = attendance["${studentId}_$dateStr"]
        if (raw == "intisab") return "intisab"
        if (isStudentIntisab(studentId, studentTags)) {
            return raw ?: "intisab"
        }
        return raw ?: "present"
    }

    val WEEK_DAYS = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")

    fun buildSectionCurriculumKey(
        deptName: String,
        stageName: String,
        section: String,
        subject: String
    ): String {
        return "${deptName.trim()}_${stageName.trim()}_${section.trim()}_${subject.trim()}"
    }

    fun verifyActivationKey(devId: String, code: String): Boolean {
        val cleanId = devId.replace(Regex("[^0-9]"), "")
        val cleanCode = code.trim().uppercase().replace(Regex("\\s+"), "")
        if (cleanId.isEmpty() || cleanCode.isEmpty()) return false
        val num = cleanId.toLongOrNull() ?: return false
        val part1 = ((num * 1993L) % 8999L + 1000L).toString()
        val part2 = ((num * 2026L) % 8999L + 1000L).toString()
        return cleanCode == "WAAD-$part1-$part2"
    }

    fun generateKeyForDevice(devId: String): String {
        val cleanId = devId.replace(Regex("[^0-9]"), "")
        val num = cleanId.toLongOrNull() ?: 10000L
        val part1 = ((num * 1993L) % 8999L + 1000L).toString()
        val part2 = ((num * 2026L) % 8999L + 1000L).toString()
        return "WAAD-$part1-$part2"
    }

    fun getStudentMaxGrade(
        student: Student?,
        settings: AppSettings,
        currentDept: DeptConfig,
        currentStage: StageConfig
    ): Int {
        if (student != null) {
            val dObj = settings.structure.find { it.name == student.deptName }
            val sObj = dObj?.stages?.find { it.name == student.stageName }
            if (sObj != null && sObj.maxGrade > 0) return sObj.maxGrade
            val stgName = student.stageName.trim()
            val dptName = student.deptName.trim()
            if ((dptName.contains("ابتدائ") || stgName.contains("ابتدائ")) &&
                !stgName.contains("خامس") && !stgName.contains("سادس")
            ) {
                return 10
            }
            return 100
        }
        if (currentStage.maxGrade > 0) return currentStage.maxGrade
        val dept = currentDept.name.trim()
        val stage = currentStage.name.trim()
        if ((dept.contains("ابتدائ") || stage.contains("ابتدائ")) &&
            !stage.contains("خامس") && !stage.contains("سادس")
        ) {
            return 10
        }
        return 100
    }

    fun calculateResults(
        g: SubjectGrades?,
        maxAllowedGrade: Int
    ): CalculatedResult {
        val failThreshold = if (maxAllowedGrade == 10) 5 else 50
        val grades = g ?: SubjectGrades()

        val hasM1 = grades.v1.isNotBlank() || grades.v2.isNotBlank() || grades.v3.isNotBlank()
        val hasM2 = grades.v4.isNotBlank() || grades.v5.isNotBlank() || grades.v6.isNotBlank()
        val hasMid = grades.mid.isNotBlank()
        val hasM3 = grades.v7.isNotBlank() || grades.v8.isNotBlank() || grades.v9.isNotBlank()
        val hasM4 = grades.v10.isNotBlank() || grades.v11.isNotBlank() || grades.v12.isNotBlank()
        val hasFinal = grades.finalExam.isNotBlank()
        val hasSecondRound = grades.secondRound.isNotBlank()

        val m1 = if (hasM1) (grades.v1.toIntOrNull() ?: 0) + (grades.v2.toIntOrNull() ?: 0) + (grades.v3.toIntOrNull() ?: 0) else 0
        val m2 = if (hasM2) (grades.v4.toIntOrNull() ?: 0) + (grades.v5.toIntOrNull() ?: 0) + (grades.v6.toIntOrNull() ?: 0) else 0
        val mid = if (hasMid) (grades.mid.toIntOrNull() ?: 0) else 0
        val m3 = if (hasM3) (grades.v7.toIntOrNull() ?: 0) + (grades.v8.toIntOrNull() ?: 0) + (grades.v9.toIntOrNull() ?: 0) else 0
        val m4 = if (hasM4) (grades.v10.toIntOrNull() ?: 0) + (grades.v11.toIntOrNull() ?: 0) + (grades.v12.toIntOrNull() ?: 0) else 0
        val finalExam = if (hasFinal) (grades.finalExam.toIntOrNull() ?: 0) else 0
        val secondRoundExam = if (hasSecondRound) (grades.secondRound.toIntOrNull() ?: 0) else 0
        val qarar = grades.qarar

        val hasAvg1 = hasM1 || hasM2
        val avg1 = if (hasAvg1) ((m1 + m2).toDouble() / if (hasM1 && hasM2) 2.0 else 1.0).roundToInt() else 0

        val hasAvg2 = hasM3 || hasM4
        val avg2 = if (hasAvg2) ((m3 + m4).toDouble() / if (hasM3 && hasM4) 2.0 else 1.0).roundToInt() else 0

        var saei = 0
        var hasSaei = false
        if (hasAvg2 && hasMid && hasAvg1) {
            saei = ((avg1 + mid + avg2) / 3.0).roundToInt()
            hasSaei = true
        } else if (hasMid && hasAvg1) {
            saei = ((avg1 + mid) / 2.0).roundToInt()
            hasSaei = true
        } else if (hasAvg1) {
            saei = avg1
            hasSaei = true
        }

        val isExempt = maxAllowedGrade == 100 && hasSaei && saei >= 90

        var finalResult = 0
        var hasFinalResult = false
        if (isExempt) {
            finalResult = saei
            hasFinalResult = true
        } else if (hasSaei && hasFinal) {
            finalResult = min(maxAllowedGrade, ((saei + finalExam) / 2.0).roundToInt() + qarar)
            hasFinalResult = true
        } else if (hasSaei) {
            finalResult = min(maxAllowedGrade, saei + qarar)
        }

        val isFailedFirstRound = hasFinalResult && finalResult < failThreshold
        var secondRoundResult: Int? = null
        var hasSecondRoundResult = false
        if (isFailedFirstRound && hasSecondRound) {
            secondRoundResult = secondRoundExam
            hasSecondRoundResult = true
        }

        return CalculatedResult(
            m1 = m1,
            m2 = m2,
            avg1 = avg1,
            mid = mid,
            m3 = m3,
            m4 = m4,
            avg2 = avg2,
            saei = saei,
            finalExam = finalExam,
            secondRoundExam = secondRoundExam,
            qarar = qarar,
            finalResult = finalResult,
            secondRoundResult = secondRoundResult,
            hasM1 = hasM1,
            hasM2 = hasM2,
            hasAvg1 = hasAvg1,
            hasMid = hasMid,
            hasM3 = hasM3,
            hasM4 = hasM4,
            hasAvg2 = hasAvg2,
            hasSaei = hasSaei,
            hasFinal = hasFinal,
            hasFinalResult = hasFinalResult,
            isExempt = isExempt,
            isFailedFirstRound = isFailedFirstRound,
            hasSecondRound = hasSecondRound,
            hasSecondRoundResult = hasSecondRoundResult
        )
    }

    fun getMonthOtherSum(g: SubjectGrades?, mField: String): Int {
        val grades = g ?: return 0
        val fNum = mField.removePrefix("v").toIntOrNull() ?: 1
        val mStart = ((fNum - 1) / 3) * 3 + 1
        val s1 = if (fNum != mStart) (grades.getField("v$mStart").toIntOrNull() ?: 0) else 0
        val s2 = if (fNum != mStart + 1) (grades.getField("v${mStart + 1}").toIntOrNull() ?: 0) else 0
        val s3 = if (fNum != mStart + 2) (grades.getField("v${mStart + 2}").toIntOrNull() ?: 0) else 0
        return s1 + s2 + s3
    }

    private fun normalizeArabicText(input: String): String {
        return input.trim().lowercase()
            // Convert Eastern Arabic & Persian digits
            .replace('٠', '0').replace('۰', '0')
            .replace('١', '1').replace('۱', '1')
            .replace('٢', '2').replace('۲', '2')
            .replace('٣', '3').replace('۳', '3')
            .replace('٤', '4').replace('۴', '4')
            .replace('٥', '5').replace('۵', '5')
            .replace('٦', '6').replace('۶', '6')
            .replace('٧', '7').replace('۷', '7')
            .replace('٨', '8').replace('۸', '8')
            .replace('٩', '9').replace('۹', '9')
            // Remove Arabic diacritics (tashkeel) and tatweel
            .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
            // Normalize Alef variants, Teh Marbuta, Alef Maqsura
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            // Normalize punctuation to spaces
            .replace(Regex("[,،.;؛/\\\\\\-_+]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun parseSpokenNumbers(txt: String, maxAllowedGrade: Int): List<Int> {
        val norm = normalizeArabicText(txt)
        if (norm.isEmpty()) return emptyList()

        // Replace multi-word teens & special phrases with numeric tokens first
        val phraseReplaced = norm
            .replace("درجه كامله", " $maxAllowedGrade ")
            .replace("الدرجه الكامله", " $maxAllowedGrade ")
            .replace("درجه واحده", " 1 ")
            .replace("احد عشر", " 11 ")
            .replace("احدي عشر", " 11 ")
            .replace("اثنا عشر", " 12 ")
            .replace("اثني عشر", " 12 ")
            .replace("ثلاثه عشر", " 13 ")
            .replace("ثلاث عشر", " 13 ")
            .replace("تلاثه عشر", " 13 ")
            .replace("اربعه عشر", " 14 ")
            .replace("اربع عشر", " 14 ")
            .replace("خمسه عشر", " 15 ")
            .replace("خمس عشر", " 15 ")
            .replace("حمسه عشر", " 15 ")
            .replace("سته عشر", " 16 ")
            .replace("ست عشر", " 16 ")
            .replace("سبعه عشر", " 17 ")
            .replace("سبع عشر", " 17 ")
            .replace("ثمانيه عشر", " 18 ")
            .replace("ثماني عشر", " 18 ")
            .replace("ثمان عشر", " 18 ")
            .replace("تمانيه عشر", " 18 ")
            .replace("تسعه عشر", " 19 ")
            .replace("تسع عشر", " 19 ")
            .replace("نص عشره", " 5 ")
            .replace("نصف عشره", " 5 ")
            .replace("نص ميه", " 50 ")
            .replace("نصف مئه", " 50 ")

        val wordToNum = mapOf(
            "صفر" to 0, "زيرو" to 0, "لاشئ" to 0,
            "واحد" to 1, "واحده" to 1, "درجه" to 1, "ووحده" to 1,
            "اثنين" to 2, "اثنان" to 2, "ثنين" to 2, "تنين" to 2, "درجتين" to 2, "درجتان" to 2,
            "ثلاثه" to 3, "ثلاث" to 3, "تلاثه" to 3, "تلاث" to 3, "تلاته" to 3,
            "اربعه" to 4, "اربع" to 4,
            "خمسه" to 5, "خمس" to 5, "حمسه" to 5, "حمس" to 5,
            "سته" to 6, "ست" to 6, "ستت" to 6,
            "سبعه" to 7, "سبع" to 7,
            "ثمانيه" to 8, "ثماني" to 8, "ثمان" to 8, "ثمن" to 8, "تمانيه" to 8, "تمان" to 8,
            "تسعه" to 9, "تسع" to 9,
            "عشره" to 10, "عشر" to 10,
            "دعش" to 11, "حدعش" to 11, "ادعش" to 11, "حداشر" to 11, "احدعش" to 11,
            "طعش" to 12, "اثنعش" to 12, "ثنعش" to 12, "تنعش" to 12, "اتناشر" to 12,
            "ثلطعش" to 13, "تلطعش" to 13, "ثلاثطعش" to 13, "تلتطعش" to 13, "تلتاش" to 13, "ثلاثطاش" to 13,
            "اربعطعش" to 14, "اربعتعش" to 14, "اربعطاش" to 14,
            "خمسطعش" to 15, "خمستعش" to 15, "خمسطاش" to 15, "خمستاش" to 15, "حمستعش" to 15,
            "ستطعش" to 16, "سطعش" to 16, "ستطاش" to 16, "ستاش" to 16,
            "سبعطعش" to 17, "سبعتعش" to 17, "سبعطاش" to 17,
            "ثمنطعش" to 18, "ثمانطعش" to 18, "تمنطعش" to 18, "ثمنطاش" to 18, "تمنطاش" to 18,
            "تسعطعش" to 19, "تسعتعش" to 19, "تسعطاش" to 19,
            "عشرون" to 20, "عشرين" to 20,
            "ثلاثون" to 30, "ثلاثين" to 30, "تلاثين" to 30, "تلاتين" to 30,
            "اربعون" to 40, "اربعين" to 40,
            "خمسون" to 50, "خمسين" to 50, "حمسين" to 50,
            "ستون" to 60, "ستين" to 60,
            "سبعون" to 70, "سبعين" to 70,
            "ثمانون" to 80, "ثمانين" to 80, "تمانين" to 80,
            "تسعون" to 90, "تسعين" to 90,
            "مئه" to 100, "مائه" to 100, "ميه" to 100, "اميه" to 100,
            "كامل" to maxAllowedGrade, "كامله" to maxAllowedGrade, "فول" to maxAllowedGrade
        )

        val rawTokens = phraseReplaced.split(" ").filter { it.isNotBlank() }

        data class ParsedToken(val value: Int?, val isAnd: Boolean)

        val parsedTokens = mutableListOf<ParsedToken>()
        for (raw in rawTokens) {
            if (raw == "و") {
                parsedTokens.add(ParsedToken(null, true))
                continue
            }
            // Direct integer token
            val directInt = raw.toIntOrNull()
            if (directInt != null) {
                parsedTokens.add(ParsedToken(directInt, false))
                continue
            }
            // Exact dictionary match
            val exact = wordToNum[raw]
            if (exact != null) {
                parsedTokens.add(ParsedToken(exact, false))
                continue
            }
            // Check if token starts with 'و' (e.g., "وعشرين", "وثلاثين", "وخمسه")
            if (raw.startsWith("و") && raw.length > 1) {
                val stripped = raw.substring(1)
                val strippedVal = stripped.toIntOrNull() ?: wordToNum[stripped]
                if (strippedVal != null) {
                    parsedTokens.add(ParsedToken(null, true))
                    parsedTokens.add(ParsedToken(strippedVal, false))
                    continue
                }
            }
            // Check if token starts with "ال" (e.g., "العشره", "الخمسه")
            if (raw.startsWith("ال") && raw.length > 2) {
                val stripped = raw.substring(2)
                val strippedVal = wordToNum[stripped]
                if (strippedVal != null) {
                    parsedTokens.add(ParsedToken(strippedVal, false))
                    continue
                }
            }
            // Extract embedded digits if any (e.g., "15درجه")
            val embeddedDigits = raw.replace(Regex("[^0-9]"), "")
            if (embeddedDigits.isNotEmpty()) {
                embeddedDigits.toIntOrNull()?.let {
                    parsedTokens.add(ParsedToken(it, false))
                }
            }
        }

        // Combine compound Arabic numbers: <unit 1..9> + (isAnd) + <tens 20..90> => unit + tens (e.g., 5 + و + 20 = 25)
        val resultNumbers = mutableListOf<Int>()
        var i = 0
        while (i < parsedTokens.size) {
            val current = parsedTokens[i]
            val v1 = current.value
            if (v1 == null) {
                i++
                continue
            }
            if (v1 in 1..9 && i + 2 < parsedTokens.size && parsedTokens[i + 1].isAnd) {
                val v2 = parsedTokens[i + 2].value
                if (v2 != null && v2 in 20..90 && v2 % 10 == 0) {
                    resultNumbers.add(v1 + v2)
                    i += 3
                    continue
                }
            }
            // Also support adjacent <unit 1..9> <tens 20..90> if spoken as "خمسه عشرين"
            if (v1 in 1..9 && i + 1 < parsedTokens.size) {
                val v2 = parsedTokens[i + 1].value
                if (v2 != null && v2 in 20..90 && v2 % 10 == 0) {
                    resultNumbers.add(v1 + v2)
                    i += 2
                    continue
                }
            }
            resultNumbers.add(v1)
            i++
        }

        return resultNumbers
    }

    fun parseSpokenNumber(txt: String, maxAllowedGrade: Int): Int? {
        return parseSpokenNumbers(txt, maxAllowedGrade).firstOrNull()
    }

    fun parseBestSpokenCandidates(candidates: List<String>, maxAllowedGrade: Int): List<Int> {
        for (candidate in candidates) {
            val nums = parseSpokenNumbers(candidate, maxAllowedGrade).filter { it in 0..maxAllowedGrade }
            if (nums.isNotEmpty()) return nums
        }
        for (candidate in candidates) {
            val nums = parseSpokenNumbers(candidate, maxAllowedGrade)
            if (nums.isNotEmpty()) return nums
        }
        return emptyList()
    }

    val LESSON_ORDINAL_NAMES = mapOf(
        1 to "الحصة الأولى",
        2 to "الحصة الثانية",
        3 to "الحصة الثالثة",
        4 to "الحصة الرابعة",
        5 to "الحصة الخامسة",
        6 to "الحصة السادسة"
    )

    val DEFAULT_LESSON_TIMES = mapOf(
        1 to ("08:00" to "08:45"),
        2 to ("08:50" to "09:35"),
        3 to ("09:40" to "10:25"),
        4 to ("10:45" to "11:30"),
        5 to ("11:35" to "12:20"),
        6 to ("12:25" to "13:10")
    )

    fun getLessonOrdinalName(lessonNum: Int): String =
        LESSON_ORDINAL_NAMES[lessonNum] ?: "الحصة $lessonNum"

    fun getDefaultLessonTimes(lessonNum: Int): Pair<String, String> =
        DEFAULT_LESSON_TIMES[lessonNum] ?: ("08:00" to "08:45")

    fun resolveLessonTimes(lessonNum: Int, scheduledClass: ScheduledClass?): Pair<String, String> {
        val def = getDefaultLessonTimes(lessonNum)
        val start = scheduledClass?.startTime?.takeIf { it.isNotBlank() } ?: def.first
        val end = scheduledClass?.endTime?.takeIf { it.isNotBlank() } ?: def.second
        return start to end
    }

    fun parseTimeMinutes(timeStr: String): Int? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    fun calculateLessonDurationMinutes(startTimeStr: String, endTimeStr: String): Int? {
        val startMin = parseTimeMinutes(startTimeStr) ?: return null
        val endMin = parseTimeMinutes(endTimeStr) ?: return null
        return endMin - startMin
    }

    fun calculateBreakMinutes(endTimeStr: String, nextStartTimeStr: String): Int? {
        val endMin = parseTimeMinutes(endTimeStr) ?: return null
        val nextStartMin = parseTimeMinutes(nextStartTimeStr) ?: return null
        return nextStartMin - endMin
    }

    fun calculateDayTotalBreakMinutes(day: String, schedule: Map<String, ScheduledClass>): Int {
        var totalBreak = 0
        for (lessonNum in 1..5) {
            val (_, curEnd) = resolveLessonTimes(lessonNum, schedule["${day}_$lessonNum"])
            val (nextStart, _) = resolveLessonTimes(lessonNum + 1, schedule["${day}_${lessonNum + 1}"])
            val diff = calculateBreakMinutes(curEnd, nextStart) ?: 0
            if (diff > 0) {
                totalBreak += diff
            }
        }
        return totalBreak
    }

    fun computeCurrentLiveLesson(schedule: Map<String, ScheduledClass>): LiveLessonInfo {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1 .. Saturday = 7
        val dayNames = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val todayName = dayNames.getOrElse(dayOfWeek - 1) { "الأحد" }
        val effectiveDay = if (todayName in WEEK_DAYS) todayName else "الأحد"
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val totalMinutes = hour * 60 + minute

        var matchedLesson = 0
        var isLiveWindow = false
        for (lesson in 1..6) {
            val slot = schedule["${effectiveDay}_$lesson"]
            val (startStr, endStr) = resolveLessonTimes(lesson, slot)
            val startMin = parseTimeMinutes(startStr) ?: (480 + (lesson - 1) * 50)
            val endMin = parseTimeMinutes(endStr) ?: (startMin + 45)
            if (totalMinutes in startMin..endMin) {
                matchedLesson = lesson
                isLiveWindow = true
                break
            }
        }

        if (matchedLesson == 0) {
            // Find next upcoming lesson today or fallback to first scheduled lesson today
            for (lesson in 1..6) {
                val slot = schedule["${effectiveDay}_$lesson"]
                val (startStr, _) = resolveLessonTimes(lesson, slot)
                val startMin = parseTimeMinutes(startStr) ?: 480
                if (totalMinutes < startMin && slot != null && slot.subject.isNotBlank()) {
                    matchedLesson = lesson
                    break
                }
            }
        }

        if (matchedLesson == 0) {
            matchedLesson = (1..6).firstOrNull { l ->
                val s = schedule["${effectiveDay}_$l"]
                s != null && s.subject.isNotBlank()
            } ?: 1
        }

        val key = "${effectiveDay}_$matchedLesson"
        val scheduled = schedule[key]
        val (resolvedStart, resolvedEnd) = resolveLessonTimes(matchedLesson, scheduled)
        return LiveLessonInfo(
            dayName = effectiveDay,
            lessonNum = matchedLesson,
            lessonOrdinalName = getLessonOrdinalName(matchedLesson),
            startTime = resolvedStart,
            endTime = resolvedEnd,
            isLiveTimeWindow = isLiveWindow && todayName in WEEK_DAYS,
            scheduledClass = scheduled
        )
    }

    fun computeActivationState(
        deviceID: String,
        savedToken: String,
        activationDateMs: Long,
        installDateMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): ActivationState {
        var isActivated = false
        var subRemaining = 0
        if (savedToken.isNotBlank() && verifyActivationKey(deviceID, savedToken) && activationDateMs > 0L) {
            val daysPassed = (nowMs - activationDateMs).toDouble() / (1000.0 * 60 * 60 * 24)
            if (daysPassed <= SUBSCRIPTION_DAYS) {
                isActivated = true
                subRemaining = max(0, ceil(SUBSCRIPTION_DAYS - daysPassed).toInt())
            }
        }
        val trialDaysPassed = (nowMs - installDateMs).toDouble() / (1000.0 * 60 * 60 * 24)
        val remainingTrial = max(0, ceil(TRIAL_DAYS - trialDaysPassed).toInt())
        return ActivationState(
            isActivated = isActivated,
            isExpired = !isActivated && trialDaysPassed > TRIAL_DAYS,
            remainingTrialDays = remainingTrial,
            subRemainingDays = subRemaining,
            deviceID = deviceID
        )
    }

    /**
     * Normalizes an Arabic student name for duplicate detection:
     * - Trims leading/trailing whitespace and collapses multiple spaces
     * - Strips Arabic diacritics (tashkeel) and tatweel
     * - Unifies Alef variants (أ, إ, آ, ٱ -> ا), Teh Marbuta (ة -> ه), and Alef Maksura (ى -> ي)
     */
    fun normalizeStudentNameForDedup(name: String): String {
        return name
            .trim()
            .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
            .replace(Regex("\\s+"), " ")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .lowercase()
    }
}

