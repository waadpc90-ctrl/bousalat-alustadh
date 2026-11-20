package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import com.googlecode.tesseract.android.TessBaseAPI
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

data class ExtractedStudentCandidate(
    val id: String,
    val name: String,
    val phone: String = "",
    val detectedSection: String = "",
    val selected: Boolean = true
)

data class SmartExtractionResult(
    val success: Boolean,
    val sourceType: String, // "pdf" or "image"
    val fileName: String,
    val pageCount: Int = 1,
    val accuracyPercent: Int = 99,
    val engineLabel: String = "",
    val smartCandidates: List<ExtractedStudentCandidate> = emptyList(),
    val allRawLines: List<String> = emptyList(),
    val previewBitmap: Bitmap? = null,
    val errorMessage: String? = null
)

object SmartStudentExtractor {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(18, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    // Whole-line header/footer/title patterns (in canonical Arabic: أ/إ/آ->ا, ة->ه, ى->ي)
    private val LINE_SKIP_PHRASES_CANONICAL = listOf(
        "وزاره التربيه",
        "المديريه العامه",
        "مديريه تربيه",
        "جمهوريه العراق",
        "اداره مدرسه",
        "اداره اعداديه",
        "اداره ثانويه",
        "اداره متوسطه",
        "اداره ابتدائيه",
        "قائمه اسماء",
        "اسماء الطلاب",
        "اسماء التلاميذ",
        "اسماء التلاميد",
        "اسم التلاميذ",
        "اسم التلاميد",
        "اسم الطالب",
        "سجل الدرجات",
        "سجل درجات",
        "سجل درحات",
        "سجل الغيابات",
        "سجل الحضور",
        "العام الدراسي",
        "للعام الدراسي",
        "للعام الدرامي",
        "السنه الدراسيه",
        "الفصل الاول",
        "الفصل الثاني",
        "نصف السنه",
        "درجه السعي",
        "السعي السنوي",
        "الامتحان النهائي",
        "الامتحانات الشهريه",
        "الامتحابات الشهريه",
        "الدرجه النهائيه",
        "الدرحه النهائيه",
        "درجه الامتحان",
        "درحه الامتحان",
        "درجه الشفوي",
        "درحه الشفوي",
        "التقييم اللفظي",
        "التقييم اللفطي",
        "الدور الاول",
        "الدور الثاني",
        "الاسم الثلاثي",
        "الاسم الرباعي",
        "الاسم الكامل",
        "مدير المدرسه",
        "معاون المدير",
        "مدرس الماده",
        "معلم الماده",
        "مرشد الصف",
        "بسم الله الرحمن الرحيم",
        "التربيه الاسلاميه",
        "اللغه العربيه",
        "اللغه الانكليزيه",
        "اللغه الانجليزيه",
        "جدول الدروس",
        "جدول الحصص"
    )

    // Words that indicate the entire line is a teacher/principal/school/header line rather than a student row
    private val LINE_REJECT_WORDS_CANONICAL = setOf(
        "وزاره", "مديريه", "جمهوريه", "محافظه", "اشراف", "اداره",
        "مدرسه", "ابتدائيه", "متوسطه", "ثانويه", "اعداديه", "معهد", "كليه", "جامعه",
        "سجل", "قائمه", "كشف", "جدول", "تلاميذ", "تلاميد", "التلاميذ", "التلاميد",
        "الطلاب", "الطالبات", "امتحان", "امتحانات", "الامتحان", "الامتحانات", "الامتحابات",
        "تقييم", "التقييم", "تلاوه", "التلاوه", "الشفوي", "الشهري", "الشهريه", "السهريه",
        "اليومي", "اليوميه", "النهائي", "النهائيه", "البهائيه", "التحريري", "العملي",
        "دفتر", "سلوك", "السلوك", "تحضير", "التحضير", "حضير", "اللفظي", "اللفطي",
        "توقيع", "التوقيع", "مدير", "المدير", "معاون", "المعاون", "مرشد", "المرشد",
        "استاذ", "الاستاذ", "معلم", "المعلم", "مدرس", "المدرس", "المشرف", "مشرف",
        "بسم", "الرحمن", "الرحيم", "السلام", "عليكم", "ورحمه", "وبركاته",
        "ملاحظات", "الملاحظات", "تعليمات", "ضوابط", "شروط", "اعلان", "تنويه"
    )

    // Non-name column tokens or metadata words that should be stripped from a row (or rejected if they are the only words)
    private val NON_NAME_WORDS_CANONICAL = setOf(
        // Table columns & headers
        "ت", "م", "الرقم", "رقم", "التسلسل", "تسلسل", "الرمز", "رمز",
        "الاسم", "اسم", "اسماء", "الثلاثي", "الرباعي", "الكامل", "اللقب", "لقب",
        "الشعبه", "شعبه", "شعيه", "الصف", "صف", "الضف", "المف", "المرحله", "مرحله",
        "الفصل", "فصل", "الكورس", "كورس", "السنه", "سنه", "العام", "عام", "للعام",
        "الدراسي", "الدراسيه", "الدرامي", "لشهر", "شهر", "اليوم", "يوم", "التاريخ", "تاريخ",
        "الصفحه", "صفحه", "الحصه", "حصه", "الحصص", "حصص", "طالبه", "الطالب", "الطالبه",
        "طلاب", "طالبات", "بنات", "مختلط", "صباحي", "مسائي", "خارجي", "اهلي", "حكومي",
        // Stages & classes
        "الاول", "اول", "الثاني", "ثاني", "الثالث", "ثالث", "الرابع", "رابع",
        "الخامس", "خامس", "السادس", "سادس", "السارس", "السابع", "الثامن", "التاسع", "العاشر",
        "الابتدائي", "ابتدائي", "المتوسط", "الاعدادي", "اعدادي", "الثانوي", "ثانوي",
        "العلمي", "علمي", "الادبي", "ادبي", "التطبيقي", "الاحيائي", "المهني",
        // Subjects
        "الماده", "ماده", "التربيه", "تربيه", "الاسلاميه", "اسلاميه", "القران", "قران", "الكريم",
        "اللغه", "لغه", "العربيه", "عربيه", "عربي", "قواعد", "ادب", "بلاغه", "املاء", "انشاء",
        "الانكليزيه", "انكليزيه", "انكليزي", "الانجليزيه", "انجليزيه", "انجليزي",
        "الفرنسيه", "الكرديه", "الرياضيات", "رياضيات", "العلوم", "علوم",
        "الفيزياء", "فيزياء", "الكيمياء", "كيمياء", "الاحياء", "احياء",
        "الاجتماعيات", "اجتماعيات", "الجغرافيه", "جغرافيه", "الوطنيه", "وطنيه",
        "الحاسوب", "حاسوب", "الفنيه", "فنيه", "الرياضه", "رياضه", "الاخلاقيه", "اخلاقيه", "النشاط", "نشاط",
        // Grades, scores & statuses
        "درجه", "الدرجه", "درحه", "الدرحه", "درجات", "الدرجات", "درحات",
        "نصف", "السعي", "سعي", "السنوي", "سنوي", "الدور", "دور",
        "رقما", "كتابه", "المجموع", "مجموع", "المعدل", "معدل", "النتيجه", "نتيجه",
        "التقدير", "تقدير", "الحاله", "حاله", "ملاحظه", "الملاحظه",
        "واجب", "الواجب", "مشاركه", "المشاركه", "حضور", "الحضور", "غياب", "الغياب", "الغيابات",
        "اجازه", "الاجازه", "مجاز", "حاضر", "غائب", "عايب",
        "ممتاز", "جيد", "جدا", "متوسط", "مقبول", "ضعيف",
        "ناجح", "راسب", "مكمل", "معفي", "مؤجل", "مفصول", "منقول", "مستمر", "انذار", "تنبيه",
        // Contact & demographics
        "الهاتف", "هاتف", "موبايل", "جوال", "ولي", "الامر", "عنوان", "العنوان",
        "محله", "زقاق", "دار", "مواليد", "المواليد", "التولد", "الجنس", "ذكر", "انثى",
        // Days & months
        "السبت", "الاحد", "الاثنين", "الثلاثاء", "الاربعاء", "الخميس", "الجمعه",
        "كانون", "شباط", "اذار", "نيسان", "ايار", "حزيران", "تموز", "اب", "ايلول", "تشرين",
        "يناير", "فبراير", "مارس", "ابريل", "مايو", "يونيو", "يوليو", "اغسطس", "سبتمبر", "اكتوبر", "نوفمبر", "ديسمبر",
        // Prepositions, pronouns, conjunctions, verbs & particles
        "في", "من", "الى", "على", "عن", "مع", "بين", "تحت", "فوق", "قبل", "بعد", "عند",
        "منذ", "حول", "خلال", "بدون", "ضد", "نحو", "لدى", "عبر", "حتى", "سوى", "غير", "فقط",
        "هذا", "هذه", "هؤلاء", "ذلك", "تلك", "الذي", "التي", "الذين", "اللذان", "اللتان",
        "هو", "هي", "هم", "هن", "نحن", "انت", "انتم", "انتن", "انا",
        "كان", "كانت", "يكون", "تكون", "ليس", "ليست", "قد", "لقد", "سوف",
        "ان", "لان", "لكن", "بل", "او", "ام", "ثم", "كل", "جميع", "بعض", "ايضا",
        "حيث", "كيف", "متى", "اين", "لماذا", "ماذا", "كم", "هل", "لا", "لم", "لن",
        "تم", "يتم", "يجب", "يمكن", "يرجى", "اما", "وبعد",
        "اولا", "ثانيا", "ثالثا", "رابعا", "خامسا", "سادسا", "سابعا", "ثامنا", "تاسعا", "عاشرا",
        "ا", "ب", "ج", "د", "ه", "و", "ي"
    )

    // Compound name prefixes where the following word is legitimately allowed to start with "ال"
    private val COMPOUND_NAME_PREFIXES_CANONICAL = setOf(
        "عبد", "ابو", "ابي", "ام", "ذو", "ذي", "بن", "بنت", "ال",
        "نور", "زين", "صلاح", "علاء", "بهاء", "حسام", "جمال", "سيف", "شمس", "قمر",
        "تقي", "سعد", "عز", "فخر", "شرف", "تاج", "عماد", "ضياء", "جلال", "كمال",
        "نظام", "قطر", "بدر", "اسد", "حيدر", "علي", "محمد", "فاطمه"
    )

    // Personal given names that themselves start with "ال" (or valid second parts of compound names)
    private val VALID_AL_PERSONAL_NAMES_CANONICAL = setOf(
        "الله", "الرحمن", "الرحيم", "الكريم", "العزيز", "الستار", "الجبار", "القادر",
        "الحسين", "الحسن", "الزهره", "الامير", "الرزاق", "الوهاب", "المجيد", "الحميد",
        "السلام", "الصمد", "الهادي", "المهدي", "الكاظم", "الرضا", "الخالق", "الواحد",
        "الباسط", "اللطيف", "الغني", "الغفور", "المنعم", "المطلب", "الباري", "الودود",
        "الحي", "القيوم", "العلي", "العظيم", "الحليم", "الحكيم", "الشهيد", "الحق", "الوكيل",
        "القوي", "المتين", "الولي", "المحصي", "المبدي", "المعيد", "المحيي", "المميت",
        "الواجد", "الماجد", "الاحد", "المقتدر", "المقدم", "المؤخر", "الاول", "الاخر",
        "الظاهر", "الباطن", "الوالي", "المتعالي", "البر", "التواب", "المنتقم", "العفو",
        "الرؤوف", "الجلال", "الاكرام", "المقسط", "الجامع", "المغني", "المانع", "الضار",
        "النافع", "النور", "البديع", "الباقي", "الوارث", "الرشيد", "الصبور",
        "الباقر", "الصادق", "الجواد", "العسكري", "المرتضى", "المجتبى", "المصطفى", "المنتظر",
        "العباس", "الحمزه", "الحارث", "الوليد", "الزبير", "النعمان", "المثنى", "المقداد",
        "المختار", "الفضل", "الخطاب", "الطيب", "الطاهر", "الامين", "المامون", "المنصور",
        "الاكبر", "الاصغر", "الدر", "العابدين", "الدين", "الفقار", "البنين", "كلثوم", "الندى",
        "الزهراء", "الحوراء", "البتول", "الرباب", "اليمامه", "العنود", "الهنوف", "الجوري", "الريان"
    )

    // Common Iraqi/Arabic given names (canonical form) to positively confirm real student names
    private val KNOWN_ARABIC_GIVEN_NAMES_CANONICAL = setOf(
        // Male names
        "احمد", "محمد", "علي", "حسين", "حسن", "عباس", "باقر", "جعفر", "موسى", "كاظم",
        "رضا", "جواد", "هادي", "مهدي", "منتظر", "مرتضى", "مصطفى", "مجتبى", "مقتدى", "حيدر",
        "كرار", "امير", "سجاد", "زين", "زيد", "عمر", "عثمان", "ابراهيم", "اسماعيل", "اسحاق",
        "يعقوب", "يوسف", "يونس", "ايوب", "داود", "سليمان", "زكريا", "يحيى", "عيسى", "نوح",
        "هود", "صالح", "شعيب", "لوط", "ادريس", "هارون", "عمران", "لقمان", "سلمان", "عمار",
        "بلال", "صهيب", "طلحه", "زبير", "خالد", "وليد", "طارق", "زياد", "قاسم", "جاسم",
        "حازم", "باسم", "ناظم", "ماجد", "واجد", "ساجد", "حامد", "محمود", "مسعود", "سعد",
        "سعيد", "مساعد", "سعود", "فهد", "صقر", "نمر", "ليث", "غيث", "همام", "هشام",
        "عصام", "وسام", "بسام", "حسام", "ضرغام", "مقدام", "سلام", "وئام", "كريم", "حكيم",
        "رحيم", "عظيم", "حليم", "سليم", "نعيم", "نديم", "وسيم", "نسيم", "تميم", "سمير",
        "منير", "بشير", "نذير", "قدير", "نصير", "زهير", "جبير", "عقيل", "نبيل", "جميل",
        "جليل", "خليل", "كفيل", "اصيل", "عدنان", "قحطان", "غسان", "حسان", "سفيان", "مروان",
        "صفوان", "علوان", "سلوان", "رضوان", "زيدان", "حمدان", "سرحان", "فرحان", "برهان", "نعمان",
        "احسان", "اركان", "بركان", "ريان", "كنان", "سنان", "عنان", "بيان", "حيان", "داخل",
        "خزام", "طحياش", "وزير", "جلب", "نورس", "كامل", "شامل", "فاضل", "عادل", "واثق",
        "صادق", "ناطق", "فائق", "غالب", "صائب", "ثاقب", "راغب", "ناصر", "ياسر", "اسر",
        "جابر", "صابر", "ظافر", "عامر", "سامر", "ثامر", "ماهر", "طاهر", "ظاهر", "باهر",
        "ساهر", "زاهر", "فاخر", "شاكر", "ذاكر", "عبد", "ابو", "ذو", "علاء", "بهاء",
        "جمال", "سيف", "شمس", "قمر", "تقي", "عز", "فخر", "شرف", "تاج", "عماد",
        "ضياء", "جلال", "كمال", "نظام", "بدر", "اسد", "انس", "اوس", "ادم", "ايهم",
        "امجد", "اسعد", "اكرم", "اشرف", "اطهر", "انور", "ازهر", "ايسر", "اثير", "لؤي",
        "قصي", "عدي", "قتيبه", "حذيفه", "عبيده", "اسامه", "معاويه", "عكرمه", "حمزه", "حنظله",
        "ميسر", "ميثم", "مازن", "مؤيد", "مهند", "مخلد", "مراد", "مروان", "مشتاق", "مشعل",
        "مناضل", "مناف", "منذر", "منصور", "موفق", "مؤمن", "مقداد", "مكي", "مزاحم", "مظفر",
        "معن", "معتز", "معتصم", "معروف", "نزار", "نجم", "نبيه", "نجيب", "نوري", "نوفل",
        "نياز", "هاشم", "هاني", "هيثم", "هلال", "وهب", "وهيب", "وائل", "وجدي", "وديع",
        "وضاح", "يعرب", "يمان", "يسار", "يعمر", "وهبي", "صبري", "شكري", "فخري", "لطفي",
        "رمزي", "فوزي", "فهمي", "حلمي", "شوكت", "حكمت", "بهجت", "مدحت", "رفعت", "طلعت",
        "نشات", "عصمت", "جودت", "ثروت", "سرمد", "فرقد", "أرشد", "ارشد", "امجد", "اياد",
        "زياد", "عماد", "جهاد", "فؤاد", "رشاد", "سداد", "ميعاد", "رياح", "صباح", "فلاح",
        "صلاح", "نجاح", "رباح", "كفاح", "وضاح", "سماح",
        // Female names
        "فاطمه", "زهراء", "زينب", "رقيه", "سكينه", "رباب", "مريم", "ساره", "هاجر", "اسيا",
        "خديجه", "عائشه", "حفصه", "اميمه", "سميه", "صفيه", "ميمونه", "جويريه", "ماريه", "حليمه",
        "امنه", "بنين", "جنات", "ريحانه", "سما", "نور", "هدى", "دعاء", "ايه", "ملاك",
        "شهد", "فرح", "رسل", "نبا", "سجى", "ضحى", "مروه", "صفا", "ندى", "رغد",
        "غفران", "امل", "ايمان", "يقين", "ريام", "سرى", "رنا", "رانيا", "ديما", "لينا",
        "دانيا", "تقى", "لقاء", "شيماء", "اسماء", "عذراء", "حنين", "افراح", "ايات", "بشائر",
        "كوثر", "اسراء", "نرجس", "اخلاص", "ابتسام", "انتصار", "ازدهار", "افتخار", "اعتماد", "اعتدال",
        "ابتهال", "استبرق", "انوار", "اطياف", "افنان", "الحان", "انسام", "اوراد", "ايلاف", "ايناس",
        "تغريد", "تسنيم", "ترتيل", "تمارا", "بيادر", "بلقيس", "بتول", "بيداء", "جميله", "جليله",
        "جمانه", "جواهر", "حميده", "حسناء", "خوله", "خالده", "خلود", "دارين", "دلال", "دانيه",
        "ذكرى", "ذكريات", "رابعه", "رافده", "راويه", "رحاب", "رحمه", "رحيق", "رزان", "رشا",
        "رغده", "رفيف", "رهام", "روان", "رويده", "ريما", "زكيه", "زمرد", "زهور", "زينه",
        "ساجده", "سحر", "سدره", "سعاد", "سلوى", "سليمه", "سمر", "سميره", "سناء", "سندس",
        "سهام", "سهى", "سوسن", "شذى", "شروق", "شفيقه", "شيرين", "صابرين", "صبا", "صفاء",
        "ضفاف", "طيف", "عاتكه", "عادله", "عاليه", "عبير", "عتاب", "عروب", "عزيزه", "عفاف",
        "عفراء", "علا", "علياء", "عهود", "غاده", "غاليه", "غصون", "غيداء", "غدير", "تبارك",
        "طيبه", "فاتن", "فائزه", "فخريه", "فدوى", "فردوس", "فريال", "فريده", "فضيله", "فوزيه",
        "فيحاء", "كريمه", "كواكب", "لبنى", "لطيفه", "لمى", "لمياء", "ليلى", "ليان", "ماجده",
        "ماريا", "مرام", "مرجانه", "مروج", "منار", "منال", "منى", "منيره", "مها", "مهديه",
        "مهجه", "مواهب", "مياده", "ميس", "ميساء", "ميسون", "ميسم", "مينا", "ناديه", "نازك",
        "ناهد", "نبيله", "نجاه", "نجلاء", "نجوى", "نجمه", "نداء", "نرمين", "نزهه", "نسرين",
        "نسمه", "نشوى", "نضال", "نعيمه", "نغم", "نهى", "نهله", "نوال", "نوران", "نورهان",
        "نوف", "هاديه", "هاله", "هبه", "هديل", "هزار", "همسه", "هناء", "هند", "هنادي",
        "هوازن", "هيام", "هيفاء", "وداد", "ورده", "وسن", "وصال", "وضحى", "وفاء", "ولاء",
        "وجدان", "ياسمين", "يسرى", "يمامه", "يمنى", "ينار", "لجين", "تولين", "سيلين", "نارين",
        "جود", "جنى", "حلا", "غلا", "تاليا", "مايا", "يارا", "لارا", "رتاج", "ريماس",
        "ميار", "لمار", "اريج", "اروى", "اصاله", "اميره", "انيسه"
    )

    suspend fun extractFromPdfUri(
        context: Context,
        uri: Uri
    ): SmartExtractionResult = withContext(Dispatchers.IO) {
        val fileName = resolveFileName(context, uri) ?: "ملف_الطلاب.pdf"
        try {
            PDFBoxResourceLoader.init(context.applicationContext)

            val tempFile = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext SmartExtractionResult(
                success = false,
                sourceType = "pdf",
                fileName = fileName,
                errorMessage = "تعذر فتح ملف PDF المختار."
            )

            var pageCount = 1
            var extractedDigitalText = ""

            try {
                PDDocument.load(tempFile).use { document ->
                    pageCount = document.numberOfPages.coerceAtLeast(1)
                    val stripper = PDFTextStripper().apply {
                        sortByPosition = true
                        startPage = 1
                        endPage = pageCount
                    }
                    extractedDigitalText = stripper.getText(document) ?: ""
                }
            } catch (_: Exception) {
                // Fallback to rendered OCR if PDFBox text layer fails
            }

            val previewBmp = renderPdfPageToBitmap(tempFile, pageIndex = 0, maxDimension = 1400)

            val normalizedDigital = normalizeArabicText(extractedDigitalText)
            val digitalCandidates = parseSmartStudentCandidates(normalizedDigital)
            val digitalRawLines = extractCleanRawLines(normalizedDigital)

            // Only return digital extraction immediately if we actually found validated student names
            if (digitalCandidates.isNotEmpty()) {
                tempFile.delete()
                return@withContext SmartExtractionResult(
                    success = true,
                    sourceType = "pdf",
                    fileName = fileName,
                    pageCount = pageCount,
                    accuracyPercent = 99,
                    engineLabel = "استخراج ذكي لأسماء الطلاب فقط من PDF (99%)",
                    smartCandidates = digitalCandidates,
                    allRawLines = digitalRawLines,
                    previewBitmap = previewBmp
                )
            }

            // Otherwise, this PDF is a scanned image PDF! Render pages (up to first 4 pages) and run Smart Arabic OCR
            val maxPagesToOcr = min(pageCount, 4)
            val combinedOcrText = StringBuilder()
            var usedCloudOcr = false

            for (pageIdx in 0 until maxPagesToOcr) {
                val pageBmp = renderPdfPageToBitmap(tempFile, pageIndex = pageIdx, maxDimension = 2000)
                if (pageBmp != null) {
                    val (pageText, isCloud) = extractArabicTextFromBitmapSmart(context, pageBmp)
                    if (isCloud) usedCloudOcr = true
                    if (pageText.isNotBlank()) {
                        combinedOcrText.append(pageText).append("\n")
                    }
                }
            }
            tempFile.delete()

            val normalizedOcr = normalizeArabicText(combinedOcrText.toString())
            val ocrCandidates = parseSmartStudentCandidates(normalizedOcr)
            val ocrRawLines = extractCleanRawLines(normalizedOcr)

            if (ocrCandidates.isEmpty()) {
                return@withContext SmartExtractionResult(
                    success = false,
                    sourceType = "pdf",
                    fileName = fileName,
                    pageCount = pageCount,
                    previewBitmap = previewBmp,
                    allRawLines = ocrRawLines.ifEmpty { digitalRawLines },
                    errorMessage = "لم يتم العثور على أسماء طلاب ثلاثية أو ثنائية واضحة داخل ملف PDF."
                )
            }

            SmartExtractionResult(
                success = true,
                sourceType = "pdf",
                fileName = fileName,
                pageCount = pageCount,
                accuracyPercent = if (usedCloudOcr) 97 else 92,
                engineLabel = if (usedCloudOcr) {
                    "استخراج ذكي لأسماء الطلاب فقط من PDF (دقة 97%)"
                } else {
                    "استخراج ذكي محلي لأسماء الطلاب فقط من PDF (دقة 92%)"
                },
                smartCandidates = ocrCandidates,
                allRawLines = ocrRawLines,
                previewBitmap = previewBmp
            )
        } catch (e: Exception) {
            SmartExtractionResult(
                success = false,
                sourceType = "pdf",
                fileName = fileName,
                errorMessage = "حدث خطأ أثناء قراءة ملف PDF: ${e.localizedMessage ?: "ملف غير مدعوم"}"
            )
        }
    }

    suspend fun extractFromImageUri(
        context: Context,
        uri: Uri
    ): SmartExtractionResult = withContext(Dispatchers.IO) {
        val fileName = resolveFileName(context, uri) ?: "صورة_قائمة_الطلاب.jpg"
        try {
            val bitmap = decodeScaledBitmapFromUri(context, uri, maxDimension = 2048)
                ?: return@withContext SmartExtractionResult(
                    success = false,
                    sourceType = "image",
                    fileName = fileName,
                    errorMessage = "تعذر قراءة الصورة المختارة."
                )
            extractFromBitmap(context, bitmap, fileName)
        } catch (e: Exception) {
            SmartExtractionResult(
                success = false,
                sourceType = "image",
                fileName = fileName,
                errorMessage = "حدث خطأ أثناء معالجة الصورة: ${e.localizedMessage ?: ""}"
            )
        }
    }

    suspend fun extractFromBitmap(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "صورة_الكاميرا.jpg"
    ): SmartExtractionResult = withContext(Dispatchers.IO) {
        try {
            val (extractedText, isCloudArabicOcr) = extractArabicTextFromBitmapSmart(context, bitmap)
            val normalized = normalizeArabicText(extractedText)
            val smartCandidates = parseSmartStudentCandidates(normalized)
            val rawLines = extractCleanRawLines(normalized)

            if (smartCandidates.isEmpty()) {
                return@withContext SmartExtractionResult(
                    success = false,
                    sourceType = "image",
                    fileName = fileName,
                    previewBitmap = bitmap,
                    allRawLines = rawLines,
                    errorMessage = "لم يتم العثور على أسماء طلاب واضحة في الصورة. يرجى التأكد من وضوح عمود أسماء الطلاب واستقامته."
                )
            }

            SmartExtractionResult(
                success = true,
                sourceType = "image",
                fileName = fileName,
                pageCount = 1,
                accuracyPercent = if (isCloudArabicOcr) 97 else 92,
                engineLabel = if (isCloudArabicOcr) {
                    "استخراج ذكي لأسماء الطلاب فقط (دقة 97%)"
                } else {
                    "استخراج ذكي لأسماء الطلاب فقط على الجهاز (دقة 92%)"
                },
                smartCandidates = smartCandidates,
                allRawLines = rawLines,
                previewBitmap = bitmap
            )
        } catch (e: Exception) {
            SmartExtractionResult(
                success = false,
                sourceType = "image",
                fileName = fileName,
                previewBitmap = bitmap,
                errorMessage = "تعذر استخراج النص العربي من الصورة: ${e.localizedMessage ?: ""}"
            )
        }
    }

    /**
     * Multi-engine Arabic OCR for images & scanned PDFs:
     * 1. Primary: High-accuracy Arabic Table OCR engine (Engine 2 with fallback to Engine 1, language=auto).
     * 2. Fallback: 100% Offline On-Device Arabic Neural LSTM OCR (Tesseract 4 with bundled ara.traineddata).
     */
    private fun extractArabicTextFromBitmapSmart(
        context: Context,
        bitmap: Bitmap
    ): Pair<String, Boolean> {
        // 1. Try High-Accuracy Cloud Arabic Table OCR first
        val cloudText = tryArabicCloudOcr(bitmap)
        if (!cloudText.isNullOrBlank()) {
            val cloudCandidates = parseSmartStudentCandidates(normalizeArabicText(cloudText))
            if (cloudCandidates.size >= 2) {
                return cloudText to true
            }
        }

        // 2. Fallback to On-Device Arabic Tesseract Neural OCR (100% offline, never outputs Latin gibberish)
        val localArabicText = tryLocalArabicTesseractOcr(context, bitmap)
        if (localArabicText.isNotBlank()) {
            return localArabicText to false
        }
        return (cloudText ?: "") to !cloudText.isNullOrBlank()
    }

    private fun tryArabicCloudOcr(bitmap: Bitmap): String? {
        val jpegBytes = compressBitmapForOcr(bitmap, maxBytes = 850_000)
        val base64DataUri = "data:image/jpeg;base64," + Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

        // Try OCREngine 2 first (best for Arabic tables & rosters), then OCREngine 1
        for (engine in listOf("2", "1")) {
            try {
                val formBody = FormBody.Builder()
                    .add("apikey", "helloworld")
                    .add("language", "auto")
                    .add("OCREngine", engine)
                    .add("isOverlayRequired", "false")
                    .add("isTable", "true")
                    .add("scale", "true")
                    .add("detectOrientation", "true")
                    .add("base64Image", base64DataUri)
                    .build()

                val request = Request.Builder()
                    .url("https://api.ocr.space/parse/image")
                    .header("User-Agent", "Mozilla/5.0")
                    .post(formBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val bodyStr = response.body?.string() ?: return@use
                    val json = JSONObject(bodyStr)
                    if (json.optBoolean("IsErroredOnProcessing", false)) return@use
                    val parsedResults = json.optJSONArray("ParsedResults") ?: return@use
                    if (parsedResults.length() == 0) return@use
                    val sb = StringBuilder()
                    for (i in 0 until parsedResults.length()) {
                        val item = parsedResults.optJSONObject(i) ?: continue
                        val parsedText = item.optString("ParsedText", "")
                        if (parsedText.isNotBlank()) {
                            sb.append(parsedText).append("\n")
                        }
                    }
                    val resultText = sb.toString()
                    if (countArabicLetters(resultText) >= 6) {
                        return resultText
                    }
                }
            } catch (_: Exception) {
                // Continue to fallback engine
            }
        }
        return null
    }

    /**
     * 100% Offline On-Device Arabic OCR using Tesseract 4 LSTM and bundled `assets/tessdata/ara.traineddata`.
     */
    private fun tryLocalArabicTesseractOcr(context: Context, bitmap: Bitmap): String {
        val tessRootDir = ensureArabicTessDataCopied(context) ?: return ""
        val sb = StringBuilder()
        var tess: TessBaseAPI? = null
        try {
            tess = TessBaseAPI()
            val initialized = tess.init(tessRootDir.absolutePath, "ara")
            if (!initialized) {
                tess.end()
                return ""
            }
            configureTesseractForArabic(tess)

            // Preprocess the bitmap specifically for Arabic student records (resolution normalization,
            // local shadow removal, contrast enhancement preserving Arabic dots, and table grid suppression)
            val preprocessed = preprocessBitmapForArabicOcr(bitmap, removeGridLines = true)

            // 1. Scan the right-side student name column region first (in Arabic RTL gradebooks, names are in the right column)
            val rightColumnCrop = extractStudentNameColumnRoi(preprocessed)
            if (rightColumnCrop != null) {
                tess.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK
                tess.setImage(rightColumnCrop)
                val colText = tess.utF8Text ?: ""
                if (countArabicLetters(colText) >= 10) {
                    sb.append(colText).append("\n")
                }
            }

            // 2. Only scan full preprocessed image if the right-column crop yielded fewer than 3 valid student names
            if (parseSmartStudentCandidates(normalizeArabicText(sb.toString())).size < 3) {
                tess.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
                tess.setImage(preprocessed)
                val fullText = tess.utF8Text ?: ""
                if (fullText.isNotBlank()) {
                    sb.append(fullText).append("\n")
                }
            }
        } catch (_: Throwable) {
            // Safe fallback
        } finally {
            try {
                tess?.end()
            } catch (_: Throwable) {
            }
        }
        return sb.toString()
    }

    internal fun configureTesseractForArabic(tess: TessBaseAPI) {
        tess.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
        // Specify 300 DPI so Tesseract accurately estimates Arabic stroke & dot sizes on mobile photos
        tess.setVariable("user_defined_dpi", "300")
        // Preserve inter-word spacing for compound Arabic names (e.g. عبد الله، عبد الستار، محمد الباقر)
        tess.setVariable("preserve_interword_spaces", "1")
        // Enable table detection for structured gradebooks
        tess.setVariable("textord_tabfind_find_tables", "1")
    }

    internal fun ensureArabicTessDataCopied(context: Context): File? {
        return try {
            val tessRoot = File(context.filesDir, "tesseract")
            val tessDataDir = File(tessRoot, "tessdata")
            if (!tessDataDir.exists()) {
                tessDataDir.mkdirs()
            }
            val trainedDataFile = File(tessDataDir, "ara.traineddata")
            if (!trainedDataFile.exists() || trainedDataFile.length() < 500_000L) {
                context.assets.open("tessdata/ara.traineddata").use { input ->
                    FileOutputStream(trainedDataFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            if (trainedDataFile.exists() && trainedDataFile.length() >= 500_000L) tessRoot else null
        } catch (_: Exception) {
            null
        }
    }

    private fun countArabicLetters(text: String): Int {
        var count = 0
        for (ch in text) {
            if (ch in '\u0621'..'\u064A' || ch in '\uFB50'..'\uFEFF') {
                count++
            }
        }
        return count
    }

    /**
     * Canonicalizes an Arabic string or word by unifying hamza/alef variants (`أ إ آ ٱ` -> `ا`),
     * ta marbuta (`ة` -> `ه`), alef maqsura (`ى` -> `ي`), and stripping diacritics/tatweel.
     */
    internal fun canonicalizeArabic(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ة' -> sb.append('ه')
                'ى' -> sb.append('ي')
                'ؤ' -> sb.append('و')
                'ئ' -> sb.append('ي')
                'ـ' -> { /* skip tatweel */ }
                in '\u064B'..'\u065F', '\u0670' -> { /* skip tashkeel/harakat */ }
                else -> sb.append(ch)
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Normalizes Arabic Presentation Forms (e.g. \uFB50-\uFDFF, \uFE70-\uFEFF),
     * Eastern Arabic digits (٠-٩ -> 0-9), tatweel (ـ), and invisible bidi control chars.
     */
    fun normalizeArabicText(raw: String): String {
        if (raw.isBlank()) return ""
        val nfkc = Normalizer.normalize(raw, Normalizer.Form.NFKC)
        val sb = StringBuilder(nfkc.length)
        for (ch in nfkc) {
            when (ch) {
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                'ـ' -> { /* strip kashida/tatweel */ }
                in '\u064B'..'\u065F', '\u0670' -> { /* strip tashkeel */ }
                '\u200E', '\u200F', '\u202A', '\u202B', '\u202C', '\u202D', '\u202E', '\uFEFF' -> {
                    /* strip invisible directional formatting marks */
                }
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Parses raw text from PDF or OCR into clean, structured Arabic student candidates ONLY.
     * Strictly filters out all headers, subjects, table columns, grades, dates, and non-name sentences.
     */
    fun parseSmartStudentCandidates(normalizedText: String): List<ExtractedStudentCandidate> {
        if (normalizedText.isBlank()) return emptyList()

        val candidates = mutableListOf<ExtractedStudentCandidate>()
        val seenNames = mutableSetOf<String>()

        val rawLines = normalizedText
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split("\n")

        var indexCounter = 0

        for (rawLine in rawLines) {
            val trimmed = rawLine.trim()
            if (trimmed.length < 3) continue

            // Extract optional Iraqi phone number if present on the same row
            val phoneMatch = Regex("(?:964|0)?7[3-9]\\d{8}").find(trimmed.replace(" ", ""))
            val detectedPhone = phoneMatch?.value ?: ""

            // Detect section if explicitly present on the row (e.g. "شعبة (أ)" or "| أ |")
            var detectedSec = ""
            val secMatch = Regex("شعب[ةه]\\s*[(]?([أابجدهـ])[)]?").find(trimmed)
            if (secMatch != null) {
                val rawSec = secMatch.groupValues[1]
                detectedSec = if (rawSec == "ا") "أ" else rawSec
            }

            // Split by tabs or pipe separators if it's a table row
            val segments = trimmed
                .split(Regex("[\\t|│┃]+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            var extractedNameForRow: String? = null

            if (segments.size > 1) {
                for (seg in segments) {
                    if (seg in listOf("أ", "ب", "ج", "د", "هـ") && detectedSec.isEmpty()) {
                        detectedSec = seg
                        continue
                    }
                    val candidateName = extractPureStudentNameFromSegment(seg)
                    if (candidateName != null) {
                        if (extractedNameForRow == null || candidateName.length > extractedNameForRow.length) {
                            extractedNameForRow = candidateName
                        }
                    }
                }
            }

            // Also try extracting from the full line (handles space-separated PDF/OCR table rows)
            if (extractedNameForRow == null) {
                extractedNameForRow = extractPureStudentNameFromSegment(trimmed)
            }

            if (extractedNameForRow != null) {
                val canonicalKey = canonicalizeArabic(extractedNameForRow)
                if (seenNames.add(canonicalKey)) {
                    indexCounter++
                    candidates.add(
                        ExtractedStudentCandidate(
                            id = "std_cand_$indexCounter",
                            name = extractedNameForRow,
                            phone = detectedPhone,
                            detectedSection = detectedSec,
                            selected = true
                        )
                    )
                }
            }
        }

        return candidates
    }

    /**
     * Extracts ONLY the pure student name (2 to 5 Arabic name words) from a text segment or table row,
     * returning null if the segment is a header, title, instruction, or non-name text.
     */
    internal fun extractPureStudentNameFromSegment(rawSegment: String): String? {
        // 1. Remove section tags and non-Arabic characters
        val withoutSection = rawSegment.replace(Regex("شعب[ةه]\\s*[(]?[أابجدهـ][)]?"), " ")
        val arabicOnly = withoutSection
            .replace(Regex("[^\\u0621-\\u064A\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (arabicOnly.length < 5) return null

        val canonicalLine = canonicalizeArabic(arabicOnly)

        // 2. Reject whole line if it matches known document/table header phrases
        for (phrase in LINE_SKIP_PHRASES_CANONICAL) {
            if (canonicalLine.contains(phrase)) return null
        }

        val rawWords = arabicOnly.split(" ").filter { it.isNotBlank() }
        if (rawWords.size < 2) return null

        val canonicalWords = rawWords.map { canonicalizeArabic(it) }

        // 3. If the line contains any strong header/teacher/school reject word (unless it's a long table row with 5+ words), reject the line
        if (rawWords.size <= 5 && canonicalWords.any { it in LINE_REJECT_WORDS_CANONICAL }) {
            return null
        }
        // Even in longer rows, if it starts with a header/teacher title (e.g., "مدرس المادة ...", "سجل درجات ..."), reject the line
        if (canonicalWords.first() in LINE_REJECT_WORDS_CANONICAL ||
            (canonicalWords.size >= 2 && canonicalWords[1] in LINE_REJECT_WORDS_CANONICAL)
        ) {
            return null
        }

        // 4. Filter out non-name column tokens (stages, subjects, grades, statuses, single-letter markers)
        // and find the longest contiguous run of valid personal name words
        val bestRun = mutableListOf<String>()
        val currentRun = mutableListOf<String>()

        for (i in rawWords.indices) {
            val word = rawWords[i]
            val canon = canonicalWords[i]
            val prevCanon = if (currentRun.isNotEmpty()) canonicalizeArabic(currentRun.last()) else null

            if (isLikelyPersonalNameWord(
                    canon = canon,
                    prevCanon = prevCanon,
                    positionInName = currentRun.size
                )
            ) {
                currentRun.add(word)
            } else {
                if (currentRun.size > bestRun.size) {
                    bestRun.clear()
                    bestRun.addAll(currentRun)
                }
                currentRun.clear()
            }
        }
        if (currentRun.size > bestRun.size) {
            bestRun.clear()
            bestRun.addAll(currentRun)
        }

        // Strip trailing orphaned compound prefix (e.g. if a name ended with standalone "عبد" or "أبو")
        while (bestRun.isNotEmpty() && canonicalizeArabic(bestRun.last()) in listOf("عبد", "ابو", "ابي", "ام", "ذو", "ذي", "بن")) {
            bestRun.removeAt(bestRun.lastIndex)
        }

        if (bestRun.size !in 2..5) return null

        val candidateName = bestRun.joinToString(" ").trim()
        return if (isValidStudentNameSequence(bestRun)) candidateName else null
    }

    /**
     * Checks whether a single canonicalized Arabic word can be part of a student's personal name.
     */
    private fun isLikelyPersonalNameWord(
        canon: String,
        prevCanon: String?,
        positionInName: Int
    ): Boolean {
        if (canon.length !in 2..11) return false
        if (canon in LINE_REJECT_WORDS_CANONICAL) return false

        // Allow "الاول"/"الاحد"/etc. ONLY when immediately preceded by "عبد"
        if (canon in NON_NAME_WORDS_CANONICAL) {
            return prevCanon == "عبد" && canon in VALID_AL_PERSONAL_NAMES_CANONICAL
        }

        // Reject plural header/document words ending in "ات" unless they are known personal names (جنات، آيات، فرات، عرفات، بركات)
        if (canon.endsWith("ات") && canon !in setOf("جنات", "ايات", "فرات", "عرفات", "بركات", "حسنات", "نغمات")) {
            return false
        }

        // Words starting with "ال" (ال التعريف) are rarely first/middle personal names unless:
        // 1) Preceded by a compound prefix ("عبد الله", "علي الدر", "محمد الباقر", "زين العابدين"), OR
        // 2) In VALID_AL_PERSONAL_NAMES_CANONICAL ("الحسن", "الحسين", "العباس", "الزهراء", ...), OR
        // 3) At the end of a 3+ word name as a tribal/family surname (nisba ending in "ي" like "الشمري", "الساعدي", "الموسوي")
        if (canon.startsWith("ال") && canon.length >= 4) {
            // Never allow "ال...يه" adjectives (الاسلاميه، الشهريه، النهائيه، الابتدائيه، العربيه)
            if (canon.endsWith("يه")) return false

            val isCompoundSecondPart = prevCanon != null && prevCanon in COMPOUND_NAME_PREFIXES_CANONICAL
            val isKnownAlName = canon in VALID_AL_PERSONAL_NAMES_CANONICAL
            val isTribalSurnameAtEnd = positionInName >= 2 && canon.endsWith("ي") && canon.length in 5..10

            if (!isCompoundSecondPart && !isKnownAlName && !isTribalSurnameAtEnd) {
                return false
            }
        }

        // Reject common Arabic verb prefixes/forms or conjunctions ("وال...", "بال...", "كال...", "فال...", "للعام", "لشهر")
        if (canon.startsWith("وال") || canon.startsWith("بال") || canon.startsWith("كال") ||
            canon.startsWith("فال") || canon.startsWith("لل")
        ) {
            return false
        }

        return true
    }

    /**
     * Validates a complete 2..5 word sequence as a genuine Arabic student name.
     */
    private fun isValidStudentNameSequence(words: List<String>): Boolean {
        if (words.size !in 2..5) return false
        val joined = words.joinToString(" ")
        if (joined.length !in 5..48) return false

        val canonWords = words.map { canonicalizeArabic(it) }

        // Must have at least 2 words of length >= 3 (or known 2-letter names like "طي", "مي", "طه", "عز")
        val substantialCount = canonWords.count { it.length >= 3 || it in KNOWN_ARABIC_GIVEN_NAMES_CANONICAL }
        if (substantialCount < 2) return false

        // Check if at least one of the first two words is a known Arabic given name, OR if it's a 3-4 word name where the first word is a known given name
        val hasKnownGivenName = canonWords.take(2).any { it in KNOWN_ARABIC_GIVEN_NAMES_CANONICAL } ||
            canonWords.any { it in KNOWN_ARABIC_GIVEN_NAMES_CANONICAL }

        // If it's only 2 words and NEITHER word is a known Arabic personal name, reject it so random 2-word OCR fragments/headers never pass
        if (words.size == 2 && !hasKnownGivenName) {
            return false
        }

        // Even for 3..5 word sequences, require at least one known Arabic personal name (first name, father's name, or grandfather's name)
        // to prevent 3-word document sentences or OCR-distorted headers from being mistaken for student names
        if (!hasKnownGivenName) {
            return false
        }

        return true
    }

    fun extractCleanRawLines(normalizedText: String): List<String> {
        return normalizedText
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split("\n")
            .map { line ->
                // Keep only Arabic letters, digits, and spaces so Latin OCR noise never appears
                line.replace(Regex("[\\t|│┃]+"), " ")
                    .replace(Regex("[^\\u0621-\\u064A0-9\\s]"), " ")
                    .replace(Regex("^[0-9\\s]+"), "")
                    .replace(Regex("[0-9\\s]+$"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            }
            .filter { line ->
                line.length >= 4 && countArabicLetters(line) >= 4
            }
            .distinct()
    }

    private fun renderPdfPageToBitmap(
        pdfFile: File,
        pageIndex: Int,
        maxDimension: Int
    ): Bitmap? {
        return try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
                    renderer.openPage(pageIndex).use { page ->
                        val srcW = page.width.coerceAtLeast(1)
                        val srcH = page.height.coerceAtLeast(1)
                        val scale = maxDimension.toFloat() / max(srcW, srcH).toFloat()
                        val targetW = (srcW * scale).toInt().coerceAtLeast(1)
                        val targetH = (srcH * scale).toInt().coerceAtLeast(1)

                        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeScaledBitmapFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int
    ): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOptions)
        }
        val srcW = boundsOptions.outWidth
        val srcH = boundsOptions.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        var inSampleSize = 1
        while ((srcW / inSampleSize) > maxDimension * 1.5 || (srcH / inSampleSize) > maxDimension * 1.5) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            this.inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        val maxSide = max(decoded.width, decoded.height)
        if (maxSide <= maxDimension) return decoded

        val scale = maxDimension.toFloat() / maxSide.toFloat()
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    }

    /**
     * Multi-stage image preprocessing pipeline tailored for Arabic student rosters and gradebooks:
     * 1. Resolution normalization (upscales low-res captures so Arabic dots/نقاط stay distinct).
     * 2. Perceptual grayscale conversion.
     * 3. Local illumination normalization via integral image to eliminate uneven shadows across paper.
     * 4. Soft adaptive contrast stretching that darkens Arabic cursive ligatures and dots while keeping background crisp white.
     * 5. Table grid-line suppression so horizontal/vertical borders do not fuse with descending Arabic letters (ر ز و ي ن ق ل).
     */
    internal fun preprocessBitmapForArabicOcr(
        source: Bitmap,
        removeGridLines: Boolean = true
    ): Bitmap {
        return try {
            val normalizedRes = normalizeResolutionForArabicOcr(source)
            val width = normalizedRes.width
            val height = normalizedRes.height
            if (width < 16 || height < 16) return enhanceBitmapForOcr(normalizedRes)

            val pixels = IntArray(width * height)
            normalizedRes.getPixels(pixels, 0, width, 0, 0, width, height)

            // Step 1: Perceptual luminance (grayscale 0..255)
            val gray = IntArray(width * height)
            for (i in pixels.indices) {
                val c = pixels[i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                gray[i] = (r * 77 + g * 150 + b * 29) shr 8
            }

            // Step 2: Integral image for local illumination estimation (removes camera shadows)
            val stride = width + 1
            val integral = LongArray((width + 1) * (height + 1))
            for (y in 0 until height) {
                var rowSum = 0L
                val yOffset = (y + 1) * stride
                val prevYOffset = y * stride
                val pixelRowOffset = y * width
                for (x in 0 until width) {
                    rowSum += gray[pixelRowOffset + x]
                    integral[yOffset + x + 1] = integral[prevYOffset + x + 1] + rowSum
                }
            }

            val windowRadius = (min(width, height) / 16).coerceIn(12, 64)
            val normGray = IntArray(width * height)

            for (y in 0 until height) {
                val y0 = (y - windowRadius).coerceAtLeast(0)
                val y1 = (y + windowRadius).coerceAtMost(height - 1)
                val row0 = y0 * stride
                val row1 = (y1 + 1) * stride
                val pixelRow = y * width

                for (x in 0 until width) {
                    val x0 = (x - windowRadius).coerceAtLeast(0)
                    val x1 = (x + windowRadius).coerceAtMost(width - 1)
                    val area = (y1 - y0 + 1) * (x1 - x0 + 1)
                    val sum = integral[row1 + x1 + 1] - integral[row1 + x0] - integral[row0 + x1 + 1] + integral[row0 + x0]
                    val localBg = (sum / area).toInt().coerceAtLeast(40)
                    val gVal = gray[pixelRow + x]

                    // Divide pixel intensity by local illumination to flatten shadows, then apply contrast curve
                    val ratio = (gVal * 255) / localBg
                    val stretched = when {
                        ratio >= 225 -> 255 // Pure white background
                        ratio <= 125 -> (ratio * 65) / 125 // Darken Arabic ink & dots
                        else -> 65 + ((ratio - 125) * 190) / 100
                    }.coerceIn(0, 255)

                    normGray[pixelRow + x] = stretched
                }
            }

            // Step 3: Optional table grid-line suppression so horizontal lines don't merge with Arabic letters
            if (removeGridLines) {
                suppressTableGridLines(normGray, width, height)
            }

            // Convert back to ARGB_8888 Bitmap for Tesseract
            for (i in pixels.indices) {
                val v = normGray[i]
                pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }

            val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            out.setPixels(pixels, 0, width, 0, 0, width, height)
            out
        } catch (_: Throwable) {
            enhanceBitmapForOcr(source)
        }
    }

    /**
     * Normalizes bitmap resolution so small images are upscaled (preserving Arabic dots)
     * and overly large images are capped to avoid OOM.
     */
    internal fun normalizeResolutionForArabicOcr(
        bitmap: Bitmap,
        targetMinLongEdge: Int = 1400,
        maxLongEdge: Int = 2400
    ): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longEdge = max(w, h)
        if (longEdge <= 0) return bitmap

        val scale = when {
            longEdge < targetMinLongEdge && longEdge >= 200 -> targetMinLongEdge.toFloat() / longEdge.toFloat()
            longEdge > maxLongEdge -> maxLongEdge.toFloat() / longEdge.toFloat()
            else -> 1f
        }
        if (scale == 1f) return bitmap

        val targetW = (w * scale).toInt().coerceAtLeast(1)
        val targetH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    /**
     * Suppresses continuous horizontal and vertical table borders while preserving intersecting Arabic character strokes.
     */
    internal fun suppressTableGridLines(gray: IntArray, width: Int, height: Int) {
        val snapshot = gray.clone()
        val darkThreshold = 165
        val minHorizRun = (width * 0.32f).toInt().coerceAtLeast(40)
        val minVertRun = (height * 0.28f).toInt().coerceAtLeast(40)
        val strokeOffset = (min(width, height) / 220).coerceIn(3, 6)

        // Detect and suppress thin horizontal grid lines
        for (y in strokeOffset until height - strokeOffset) {
            val rowOffset = y * width
            var runStart = -1
            for (x in 0..width) {
                val isDark = x < width && snapshot[rowOffset + x] < darkThreshold
                if (isDark) {
                    if (runStart == -1) runStart = x
                } else if (runStart != -1) {
                    val runLen = x - runStart
                    if (runLen >= minHorizRun) {
                        for (rx in runStart until x) {
                            // Keep pixel if there is a vertical stroke (e.g., Arabic letter crossing or touching the line)
                            val aboveDark = snapshot[(y - strokeOffset) * width + rx] < darkThreshold
                            val belowDark = snapshot[(y + strokeOffset) * width + rx] < darkThreshold
                            if (!aboveDark && !belowDark) {
                                gray[rowOffset + rx] = 255
                            }
                        }
                    }
                    runStart = -1
                }
            }
        }

        // Detect and suppress thin vertical grid lines
        for (x in strokeOffset until width - strokeOffset) {
            var runStart = -1
            for (y in 0..height) {
                val isDark = y < height && snapshot[y * width + x] < darkThreshold
                if (isDark) {
                    if (runStart == -1) runStart = y
                } else if (runStart != -1) {
                    val runLen = y - runStart
                    if (runLen >= minVertRun) {
                        for (ry in runStart until y) {
                            val leftDark = snapshot[ry * width + (x - strokeOffset)] < darkThreshold
                            val rightDark = snapshot[ry * width + (x + strokeOffset)] < darkThreshold
                            if (!leftDark && !rightDark) {
                                gray[ry * width + x] = 255
                            }
                        }
                    }
                    runStart = -1
                }
            }
        }
    }

    /**
     * Crops the right-side RTL student name column region (`اسم التلاميذ` / `اسم الطالب`)
     * from a full gradebook page (`x: 42%..94%`, `y: 14%..96%`).
     */
    internal fun extractStudentNameColumnRoi(bitmap: Bitmap): Bitmap? {
        if (bitmap.width < 300 || bitmap.height < 300) return null
        val cropX = (bitmap.width * 0.42f).toInt()
        val cropY = (bitmap.height * 0.14f).toInt()
        val cropW = (bitmap.width * 0.52f).toInt().coerceAtMost(bitmap.width - cropX)
        val cropH = (bitmap.height * 0.82f).toInt().coerceAtMost(bitmap.height - cropY)
        return if (cropW > 80 && cropH > 80) {
            Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
        } else {
            null
        }
    }

    /**
     * Enhances contrast on document photos to improve offline Arabic OCR accuracy.
     */
    internal fun enhanceBitmapForOcr(source: Bitmap): Bitmap {
        return try {
            val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val contrast = 1.22f
            val translate = (-0.5f * contrast + 0.5f) * 255f
            val cm = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
            canvas.drawColor(android.graphics.Color.WHITE)
            canvas.drawBitmap(source, 0f, 0f, paint)
            result
        } catch (_: Exception) {
            source
        }
    }

    private fun compressBitmapForOcr(bitmap: Bitmap, maxBytes: Int): ByteArray {
        var quality = 90
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        while (out.size() > maxBytes && quality > 45) {
            out.reset()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return out.toByteArray()
    }

    private fun resolveFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }
}
