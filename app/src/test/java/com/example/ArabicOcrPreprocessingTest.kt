package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import com.example.data.SmartStudentExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ArabicOcrPreprocessingTest {

    @Test
    fun `verify ara_traineddata asset is bundled and copied for Tesseract`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tessRoot = SmartStudentExtractor.ensureArabicTessDataCopied(context)
        assertNotNull("Tesseract root directory should be initialized", tessRoot)

        val araModelFile = File(tessRoot!!, "tessdata/ara.traineddata")
        assertTrue("ara.traineddata must exist in filesDir/tesseract/tessdata", araModelFile.exists())
        assertTrue(
            "ara.traineddata must be complete (> 1 MB), actual=${araModelFile.length()}",
            araModelFile.length() > 1_000_000L
        )
    }

    @Test
    fun `verify resolution normalization upscales low-res captures and caps huge images`() {
        val smallBmp = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
        val upscaled = SmartStudentExtractor.normalizeResolutionForArabicOcr(
            smallBmp,
            targetMinLongEdge = 1400,
            maxLongEdge = 2400
        )
        assertEquals(1400, maxOf(upscaled.width, upscaled.height))

        val hugeBmp = Bitmap.createBitmap(3000, 4000, Bitmap.Config.ARGB_8888)
        val downscaled = SmartStudentExtractor.normalizeResolutionForArabicOcr(
            hugeBmp,
            targetMinLongEdge = 1400,
            maxLongEdge = 2400
        )
        assertEquals(2400, maxOf(downscaled.width, downscaled.height))
    }

    @Test
    fun `verify Arabic image preprocessing flattens shadows removes grid lines and preserves text strokes`() {
        val width = 1400
        val height = 1000
        val pixels = IntArray(width * height)

        // 1. Fill uneven shadow background (bright 240 on left, shadowed 150 on right)
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val shade = (240 - (x * 90) / width).coerceIn(140, 240)
                pixels[rowOffset + x] = Color.rgb(shade, shade, shade)
            }
        }

        // 2. Draw a thin 2px horizontal table border line across the width at y = 300..301
        for (y in 300..301) {
            val rowOffset = y * width
            for (x in 40 until width - 40) {
                pixels[rowOffset + x] = Color.rgb(60, 60, 60)
            }
        }

        // 3. Draw a 20x20 dark block simulating an Arabic character stroke at (800..820, 500..520)
        for (y in 500..520) {
            val rowOffset = y * width
            for (x in 800..820) {
                pixels[rowOffset + x] = Color.rgb(20, 20, 20)
            }
        }

        val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        source.setPixels(pixels, 0, width, 0, 0, width, height)

        val processed = SmartStudentExtractor.preprocessBitmapForArabicOcr(source, removeGridLines = true)
        assertNotNull(processed)

        // Verify shadowed background pixel at (1200, 200) became crisp white (255)
        val bgPixel = processed.getPixel(1200, 200) and 0xFF
        assertTrue("Shadowed paper background should be normalized to white, got $bgPixel", bgPixel >= 240)

        // Verify thin horizontal table border at (700, 300) was suppressed to white (255)
        val gridPixel = processed.getPixel(700, 300) and 0xFF
        assertEquals("Thin horizontal table line should be suppressed", 255, gridPixel)

        // Verify thick character stroke at (810, 510) remained dark for OCR
        val strokePixel = processed.getPixel(810, 510) and 0xFF
        assertTrue("Arabic ink stroke should remain dark (< 90), got $strokePixel", strokePixel < 90)
    }

    @Test
    fun `verify RTL student name column ROI extraction crops right-side column`() {
        val fullPage = Bitmap.createBitmap(1000, 1400, Bitmap.Config.ARGB_8888)
        val roi = SmartStudentExtractor.extractStudentNameColumnRoi(fullPage)
        assertNotNull(roi)
        assertEquals(520, roi!!.width)
        assertEquals(1148, roi.height)
    }

    @Test
    fun `verify Iraqi gradebook Arabic student names extraction and header filtering`() {
        val sampleGradebookText = """
            بسم الله الرحمن الرحيم
            جمهورية العراق - وزارة التربية
            المديرية العامة لتربية بغداد
            سجل درجات التربية الإسلامية
            اليومي والإمتحانات الشهرية
            سجل درحات التربيه الاسلاميه
            اليومي والامتحابات السهريه
            للعام الدراسي 2023-2024
            الصف السارس الشعبة أ لشهر تشرين الاول
            ت | اسم التلاميذ | تحضير | دفتر/ سلوك | درجة الشفوي التلاوة | درجة الامتحان الشهري | الدرجة النهائية رقماً كتابة
            ١ احمد عبد الله خزام الصف السادس ممتاز 90 ناجح
            ٢ امير علي حسين جيد جدا 85 ناجح
            ٣ باقر عباس ناصر حاضر
            ٤ حسين احمد طارق
            ٥ حسين احمد كاظم
            ٦ حسين علي عبد الستار
            ٧ رضا سعد حسن
            ٨ عبد الله احمد عبد العزيز
            ٩ علي الدر قاسم عبد الحسين
            ١٠ علي بلال طارق
            ١١ علي ليث جاسم
            ١٢ كرار كريم طحياش
            ١٣ محمد الباقر حازم صالح
            ١٤ بنين حيدر وزير
            ١٥ جنات سمير داخل
            ١٦ ريحانة احمد مهدي
            ١٧ سما نورس كامل
            ١٨ فاطمة سعد جلب
            مدرس المادة : الأستاذ وعد بزيع
            مدير المدرسة : توقيع وملاحظات الإدارة
        """.trimIndent()

        val normalized = SmartStudentExtractor.normalizeArabicText(sampleGradebookText)
        val candidates = SmartStudentExtractor.parseSmartStudentCandidates(normalized)

        assertEquals(18, candidates.size)
        assertEquals("احمد عبد الله خزام", candidates[0].name)
        assertEquals("امير علي حسين", candidates[1].name)
        assertEquals("باقر عباس ناصر", candidates[2].name)
        assertEquals("علي الدر قاسم عبد الحسين", candidates[8].name)
        assertEquals("فاطمة سعد جلب", candidates[17].name)
    }
}
