package com.example

import com.example.model.SubjectGrades
import com.example.model.WaadCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun gradeCalculation_exemptionAndFinalResult_isCorrect() {
        val grades = SubjectGrades(
            v1 = "30", v2 = "30", v3 = "35", // m1 = 95
            v4 = "30", v5 = "30", v6 = "33", // m2 = 93 -> avg1 = 94
            mid = "92",
            v7 = "30", v8 = "30", v9 = "34", // m3 = 94
            v10 = "30", v11 = "30", v12 = "36" // m4 = 96 -> avg2 = 95
        )
        val res = WaadCalculator.calculateResults(grades, 100)
        assertEquals(95, res.m1)
        assertEquals(93, res.m2)
        assertEquals(94, res.avg1)
        assertEquals(95, res.avg2)
        assertEquals(94, res.saei)
        assertTrue(res.isExempt)
        assertEquals(94, res.finalResult)
    }

    @Test
    fun activationKey_verification_isCorrect() {
        val devId = "54321"
        val validKey = WaadCalculator.generateKeyForDevice(devId)
        assertTrue(WaadCalculator.verifyActivationKey(devId, validKey))
        assertFalse(WaadCalculator.verifyActivationKey(devId, "WAAD-0000-0000"))
    }

    @Test
    fun spokenArabicNumbers_parsedCorrectly() {
        assertEquals(15, WaadCalculator.parseSpokenNumber("خمسة عشر", 100))
        assertEquals(15, WaadCalculator.parseSpokenNumber("خمسطعش", 100))
        assertEquals(14, WaadCalculator.parseSpokenNumber("اربعطعش", 100))
        assertEquals(25, WaadCalculator.parseSpokenNumber("خمسة وعشرين", 100))
        assertEquals(34, WaadCalculator.parseSpokenNumber("أربعة وثلاثون", 100))
        assertEquals(100, WaadCalculator.parseSpokenNumber("كامل", 100))
        assertEquals(10, WaadCalculator.parseSpokenNumber("كامل", 10))
        assertEquals(85, WaadCalculator.parseSpokenNumber("85", 100))
        assertEquals(25, WaadCalculator.parseSpokenNumber("٢٥", 100))
        assertEquals(
            listOf(10, 15, 40),
            WaadCalculator.parseSpokenNumbers("عشرة خمسة عشر أربعين", 100)
        )
    }

    @Test
    fun lessonOrdinalNamesAndTimes_areCorrect() {
        assertEquals("الحصة الأولى", WaadCalculator.getLessonOrdinalName(1))
        assertEquals("الحصة السادسة", WaadCalculator.getLessonOrdinalName(6))
        assertEquals(480, WaadCalculator.parseTimeMinutes("08:00"))
        val (start, end) = WaadCalculator.resolveLessonTimes(1, null)
        assertEquals("08:00", start)
        assertEquals("08:45", end)
    }
}

