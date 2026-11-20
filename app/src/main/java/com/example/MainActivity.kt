package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ScheduledClass
import com.example.model.WaadCalculator
import com.example.notifications.LessonNotificationHelper
import com.example.ui.WaadViewModel
import com.example.ui.components.AcademicYearResetModal
import com.example.ui.components.ActivationModalOrLockScreen
import com.example.ui.components.BonusGradeModal
import com.example.ui.components.BulkAddModal
import com.example.ui.components.ConfirmDangerDialog
import com.example.ui.components.PinLockScreen
import com.example.ui.components.RandomStudentDiceModal
import com.example.ui.components.SmartExtractionLoadingDialog
import com.example.ui.components.SmartImportReviewModal
import com.example.ui.components.StudentProfileModal
import com.example.ui.components.TopStudentsCertificatesModal
import com.example.ui.components.WaadBottomNav
import com.example.ui.components.WaadHeaderBar
import com.example.ui.components.WhatsAppShareModal
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OfficialPrintPreviewScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpeedGradingScreen
import com.example.ui.screens.StudentEntryScreen
import com.example.ui.screens.StudentsListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate900
import com.example.update.GoogleDriveUpdateDialog
import com.example.update.GoogleDriveUpdateManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val latestIntentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LessonNotificationHelper.ensureNotificationChannel(this)
        latestIntentState.value = intent
        setContent {
            val context = LocalContext.current
            val vm: WaadViewModel = viewModel(factory = WaadViewModel.provideFactory(context))
            val state by vm.uiState.collectAsStateWithLifecycle()
            val activeIntent by latestIntentState

            LaunchedEffect(activeIntent) {
                val curIntent = activeIntent ?: return@LaunchedEffect
                if (curIntent.getBooleanExtra(LessonNotificationHelper.EXTRA_FROM_NOTIFICATION, false)) {
                    val deptIdx = curIntent.getIntExtra(LessonNotificationHelper.EXTRA_DEPT_IDX, 0)
                    val stageIdx = curIntent.getIntExtra(LessonNotificationHelper.EXTRA_STAGE_IDX, 0)
                    val section = curIntent.getStringExtra(LessonNotificationHelper.EXTRA_SECTION) ?: "أ"
                    val subject = curIntent.getStringExtra(LessonNotificationHelper.EXTRA_SUBJECT) ?: ""
                    if (subject.isNotBlank()) {
                        vm.jumpToScheduledClass(
                            ScheduledClass(
                                deptIdx = deptIdx,
                                stageIdx = stageIdx,
                                section = section,
                                subject = subject
                            ),
                            targetView = "list"
                        )
                    }
                    curIntent.removeExtra(LessonNotificationHelper.EXTRA_FROM_NOTIFICATION)
                }

                // Handle external update command sent from the separate standalone updater file (bousalat://update?driveUrl=...)
                val dataUri = curIntent.data
                if (dataUri != null && dataUri.scheme == "bousalat" && dataUri.host == "update") {
                    val driveUrlParam = (dataUri.getQueryParameter("driveUrl")
                        ?: dataUri.getQueryParameter("url")
                        ?: "").trim()
                    val versionParam = dataUri.getQueryParameter("version") ?: ""
                    val notesParam = dataUri.getQueryParameter("notes") ?: ""

                    if (driveUrlParam.isNotBlank() && GoogleDriveUpdateManager.isTrustedUpdateUrl(driveUrlParam)) {
                        if (versionParam.isNotBlank()) {
                            vm.updateDriveTargetVersion(versionParam)
                        }
                        if (notesParam.isNotBlank()) {
                            vm.updateDriveReleaseNotes(notesParam)
                        }
                        vm.updateDriveUrl(driveUrlParam)
                        vm.setDriveUpdateDialogOpen(true)
                    } else if (driveUrlParam.isNotBlank()) {
                        vm.showToast("⚠️ تم رفض رابط التحديث لأنه لا ينتمي إلى نطاق كوكل درايف الآمن (HTTPS)")
                    }
                    curIntent.data = null
                }
            }

            MyApplicationTheme(darkTheme = state.darkMode) {
                WaadSmartApp(vm = vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestIntentState.value = intent
    }
}

@Composable
fun WaadSmartApp(
    vm: WaadViewModel
) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsStateWithLifecycle()

    // Automatically listen for remote OTA updates broadcast by the developer's standalone tool
    LaunchedEffect(Unit) {
        vm.startRemoteUpdateBroadcastListener(context)
    }

    LaunchedEffect(state.teacherSchedule, state.notificationSettings, state.settings) {
        LessonNotificationHelper.scheduleAllLessonAlarms(
            context = context,
            schedule = state.teacherSchedule,
            settings = state.settings,
            notifSettings = state.notificationSettings
        )
    }

    var pendingBackupJson by remember { mutableStateOf<String?>(null) }
    var pendingUpdateManifestJson by remember { mutableStateOf<String?>(null) }
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }
    var settingsTab by remember { mutableStateOf("school") }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val json = pendingBackupJson
        if (uri != null && json != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                vm.showToast("تم تصدير النسخة الاحتياطية بنجاح 💾")
            } catch (e: Exception) {
                vm.showToast("تعذر حفظ الملف ⚠️")
            }
        }
        pendingBackupJson = null
    }

    val saveUpdateJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val json = pendingUpdateManifestJson
        if (uri != null && json != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                vm.showToast("تم حفظ ملف التحديث (bousalat_alustadh_update.json) لرفعه على كوكل درايف ☁️")
            } catch (e: Exception) {
                vm.showToast("تعذر حفظ ملف التحديث ⚠️")
            }
        }
        pendingUpdateManifestJson = null
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                }
                if (!content.isNullOrBlank()) {
                    vm.restoreFromBackupJsonString(content)
                } else {
                    vm.showToast("ملف النسخة الاحتياطية فارغ ❌")
                }
            } catch (e: Exception) {
                vm.showToast("ملف النسخة الاحتياطية غير صالح ❌")
            }
        }
    }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        val csv = pendingCsvContent
        if (uri != null && csv != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                vm.showToast("تم تصدير ملف Excel بنجاح 📊")
            } catch (e: Exception) {
                vm.showToast("تعذر حفظ ملف Excel ⚠️")
            }
        }
        pendingCsvContent = null
    }

    val pickPdfForStudentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            vm.extractStudentsFromPdf(context, uri)
        }
    }

    val pickImageForStudentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            vm.extractStudentsFromImage(context, uri)
        }
    }

    val pickImageFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            vm.extractStudentsFromImage(context, uri)
        }
    }

    val captureCameraForStudentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            vm.extractStudentsFromCameraBitmap(context, bitmap)
        }
    }

    fun triggerPickPdfForStudents() {
        try {
            pickPdfForStudentsLauncher.launch(arrayOf("application/pdf"))
        } catch (e: Exception) {
            vm.showToast("تعذر فتح مستعرض ملفات PDF ⚠️")
        }
    }

    fun triggerPickImageForStudents() {
        try {
            pickImageForStudentsLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (e: Exception) {
            try {
                pickImageFallbackLauncher.launch(arrayOf("image/*"))
            } catch (_: Exception) {
                vm.showToast("تعذر فتح معرض الصور ⚠️")
            }
        }
    }

    fun triggerCaptureCameraForStudents() {
        try {
            captureCameraForStudentsLauncher.launch(null)
        } catch (e: Exception) {
            vm.showToast("تعذر تشغيل الكاميرا ⚠️")
        }
    }

    fun triggerSaveBackup() {
        val json = vm.generateBackupJsonString()
        pendingBackupJson = json
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        try {
            saveBackupLauncher.launch("WAAD_BACKUP_$dateStr.json")
        } catch (e: Exception) {
            sharePlainText(context, json, "WAAD_BACKUP_$dateStr.json")
            vm.showToast("تم تجهيز النسخة الاحتياطية للمشاركة 💾")
        }
    }

    fun triggerExportUpdateJson() {
        val json = vm.buildDriveUpdateManifestJsonString()
        pendingUpdateManifestJson = json
        try {
            saveUpdateJsonLauncher.launch("bousalat_alustadh_update.json")
        } catch (e: Exception) {
            sharePlainText(context, json, "bousalat_alustadh_update.json")
            vm.showToast("تم تجهيز ملف التحديث (bousalat_alustadh_update.json) للمشاركة ☁️")
        }
    }

    fun triggerExportCsv() {
        val csv = vm.buildExcelCsvContent() ?: return
        pendingCsvContent = csv
        val sub = state.globalSub.ifBlank { state.currentStage.subjects.firstOrNull() ?: "عام" }
        val fileName = "سجل_${state.currentStage.name}_$sub.csv"
        try {
            saveCsvLauncher.launch(fileName)
        } catch (e: Exception) {
            sharePlainText(context, csv, fileName)
            vm.showToast("تم تجهيز ملف Excel للمشاركة 📊")
        }
    }

    fun openWhatsAppOrShare(phoneRaw: String, message: String) {
        var phone = phoneRaw.replace(Regex("[^0-9]"), "")
        if (phone.startsWith("07")) {
            phone = "964" + phone.substring(1)
        }
        val encoded = Uri.encode(message)
        val url = if (phone.isNotEmpty()) {
            "https://wa.me/$phone?text=$encoded"
        } else {
            "https://api.whatsapp.com/send?text=$encoded"
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            sharePlainText(context, message, "تقرير الطالب")
        }
    }

    // Back button handling for secondary screens
    if (!state.isSpeedMode && !state.isPrintMode && state.view != "dashboard") {
        BackHandler {
            vm.setView("dashboard")
        }
    }

    // 1. PIN Lock Screen
    if (state.isLocked) {
        PinLockScreen(
            state = state,
            onPinInputChange = vm::onPinInputChange
        )
        return
    }

    // 2. Trial Expired Full-Screen Lock
    if (!state.activationState.isActivated && state.activationState.isExpired) {
        ActivationModalOrLockScreen(
            state = state,
            isFullScreenLock = true,
            onActivationInputChange = vm::onActivationInputChange,
            onActivateClick = vm::handleActivation,
            onContactWhatsApp = {
                val msg = "مرحباً أستاذ وعد، أود تفعيل تطبيق بوصلة الأستاذ.\nمعرف جهازي هو: ${state.activationState.deviceID}"
                openWhatsAppOrShare(WaadCalculator.ADMIN_WHATSAPP, msg)
            },
            onDismiss = {}
        )
        return
    }

    val filteredStudents = remember(
        state.students,
        state.grades,
        state.selDeptIdx,
        state.selStageIdx,
        state.globalSec,
        state.globalSub,
        state.search,
        state.sortFilter,
        state.settings
    ) {
        vm.getFilteredStudentsList(state)
    }

    val stats = remember(
        state.students,
        state.grades,
        state.selDeptIdx,
        state.selStageIdx,
        state.globalSec,
        state.globalSub,
        state.settings
    ) {
        vm.computeStats(state)
    }

    val speedList = remember(
        state.students,
        state.selDeptIdx,
        state.selStageIdx,
        state.globalSec,
        state.settings
    ) {
        vm.getSpeedList(state)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (state.darkMode) Slate900 else Slate50)
    ) {
        when {
            state.isPrintMode -> {
                OfficialPrintPreviewScreen(
                    state = state,
                    filteredStudents = filteredStudents,
                    stats = stats,
                    calculateResult = { stdId, sub -> vm.calculateStudentResult(stdId, sub, state) },
                    onTriggerSystemPrint = { templateType, monthKey, colorTheme, isLandscape, showSignatures, showSupervisorSign, blankColumnsCount, sortOrder ->
                        val html = vm.buildPrintableHtml(
                            templateType = templateType,
                            monthKey = monthKey,
                            colorTheme = colorTheme,
                            isLandscape = isLandscape,
                            showSignatures = showSignatures,
                            showSupervisorSign = showSupervisorSign,
                            blankColumnsCount = blankColumnsCount,
                            sortOrder = sortOrder
                        )
                        triggerAndroidHtmlPrint(
                            context = context,
                            htmlContent = html,
                            jobName = "سجل_${state.currentStage.name}_${state.globalSub}",
                            isLandscape = isLandscape
                        )
                    },
                    onExportExcel = { triggerExportCsv() },
                    onSharePrintSummary = { templateType, monthKey ->
                        val text = vm.buildPrintSummaryShareText(templateType, monthKey)
                        sharePlainText(context, text, "ملخص السجل الرسمي — ${state.currentStage.name}")
                    },
                    onSecChange = vm::setGlobalSec,
                    onSubChange = vm::setGlobalSub,
                    onClosePrintMode = { vm.setPrintMode(false) }
                )
            }

            state.isSpeedMode -> {
                SpeedGradingScreen(
                    state = state,
                    speedList = speedList,
                    instantMonthTotal = vm.getInstantMonthTotal(state),
                    onCloseSpeedMode = { vm.setSpeedMode(false) },
                    onUpdateSpeedConfig = vm::updateSpeedConfig,
                    onGradeChange = vm::handleGradeChange,
                    onOpenBonus = vm::openBonusModal,
                    onVoiceSpoken = vm::onVoiceRecognitionSpoken,
                    onVoiceCandidatesSpoken = vm::onVoiceCandidatesSpoken,
                    onAdvanceField = vm::advanceSpeedFieldOrStudent,
                    onFinishGrading = {
                        vm.setSpeedMode(false)
                        vm.showToast("تم إكمال الرصد بنجاح ✅")
                    }
                )
            }

            else -> {
                Scaffold(
                    containerColor = if (state.darkMode) Slate900 else Slate50,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        WaadHeaderBar(
                            state = state,
                            activeSettingsTab = settingsTab,
                            onToggleDarkMode = vm::toggleDarkMode,
                            onOpenUnlockModal = { vm.setUnlockModalOpen(true) },
                            onOpenSchoolSettings = {
                                settingsTab = "school"
                                vm.setView("settings")
                            },
                            onOpenUpdateModal = {
                                vm.setDriveUpdateDialogOpen(true)
                            },
                            onOpenSettings = {
                                settingsTab = "app"
                                vm.setView("settings")
                            }
                        )
                    },
                    bottomBar = {
                        WaadBottomNav(
                            currentView = state.view,
                            darkMode = state.darkMode,
                            onSelectView = vm::setView,
                            onToggleEntry = {
                                if (state.view == "entry") {
                                    vm.setView("dashboard")
                                } else {
                                    vm.setView("entry")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    val navOrder = remember {
                        mapOf(
                            "dashboard" to 0,
                            "list" to 1,
                            "entry" to 2,
                            "attendance" to 3,
                            "schedule" to 4,
                            "settings" to 5
                        )
                    }

                    AnimatedContent(
                        targetState = state.view,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        transitionSpec = {
                            val initialIdx = navOrder[initialState] ?: 0
                            val targetIdx = navOrder[targetState] ?: 0
                            // In RTL, moving to a higher index (e.g. dashboard -> list) moves right-to-left
                            val direction = if (targetIdx >= initialIdx) -1 else 1

                            (slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = 0.8f,
                                    stiffness = 380f
                                ),
                                initialOffsetX = { fullWidth -> (fullWidth * 0.28f * direction).toInt() }
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = 240)
                            ) + scaleIn(
                                initialScale = 0.94f,
                                animationSpec = spring(
                                    dampingRatio = 0.82f,
                                    stiffness = 400f
                                )
                            )).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = 400f
                                    ),
                                    targetOffsetX = { fullWidth -> -(fullWidth * 0.22f * direction).toInt() }
                                ) + fadeOut(
                                    animationSpec = tween(durationMillis = 180)
                                ) + scaleOut(
                                    targetScale = 0.95f,
                                    animationSpec = tween(durationMillis = 180)
                                )
                            )
                        },
                        label = "bottom_nav_screen_transition"
                    ) { activeView ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            when (activeView) {
                                "dashboard" -> {
                                    DashboardScreen(
                                        state = state,
                                        stats = stats,
                                        filteredCount = filteredStudents.size,
                                        onSelectDept = vm::selectDepartment,
                                        onSelectStage = vm::selectStage,
                                        onSelectSec = vm::setGlobalSec,
                                        onSelectSub = vm::setGlobalSub,
                                        onJumpToLiveClass = vm::jumpToLiveClass,
                                        onOpenSchedule = { vm.setView("schedule") },
                                        onTriggerLessonAlert = { vm.triggerLessonNotificationNow(context) },
                                        onPickRandomStudent = vm::pickRandomStudent,
                                        onOpenCertificates = { vm.setCertificateModalOpen(true) },
                                        onStartGradingMode = vm::startGradingMode,
                                        onStartPrintMode = { vm.setPrintMode(true) },
                                        onExportExcel = { triggerExportCsv() }
                                    )
                                }

                                "list" -> {
                                    StudentsListScreen(
                                        state = state,
                                        filteredStudents = filteredStudents,
                                        onSelectDept = vm::selectDepartment,
                                        onSelectStage = vm::selectStage,
                                        onSearchChange = vm::setSearch,
                                        onOpenBulkAdd = { vm.setBulkModalOpen(true) },
                                        onPickPdf = ::triggerPickPdfForStudents,
                                        onPickImage = ::triggerPickImageForStudents,
                                        onCaptureCamera = ::triggerCaptureCameraForStudents,
                                        onSelectSec = vm::setGlobalSec,
                                        onSelectSub = vm::setGlobalSub,
                                        onSelectSort = vm::setSortFilter,
                                        onOpenProfile = { vm.setProfileStudent(it) },
                                        onOpenBonus = vm::openBonusModal,
                                        onOpenShare = { vm.openShareModal(it, "single") },
                                        onEditStudent = vm::startEditStudent,
                                        onDeleteStudent = { vm.requestDeleteStudent(it) },
                                        onDeleteStudentsBulk = vm::deleteStudentsBulk,
                                        onSaveSectionNote = vm::saveSectionCurriculumNote,
                                        onUpdateMissingHomework = vm::updateStudentMissingHomework,
                                        onDeductDailyForMissingHomework = vm::deductDailyGradeForMissingHomework
                                    )
                                }

                                "entry" -> {
                                    StudentEntryScreen(
                                        state = state,
                                        filteredCount = filteredStudents.size,
                                        onSelectDept = vm::selectDepartment,
                                        onSelectStage = vm::selectStage,
                                        onNameChange = vm::onFormNameChange,
                                        onSectionChange = vm::onFormSectionChange,
                                        onPhoneChange = vm::onFormPhoneChange,
                                        onIntisabChange = vm::onFormIntisabChange,
                                        onSaveStudent = vm::handleSaveStudent,
                                        onOpenBulkAdd = { vm.setBulkModalOpen(true) },
                                        onTargetSecChange = vm::onBulkTargetSecChange,
                                        onBulkTextChange = vm::onBulkTextChange,
                                        onConfirmBulkAdd = vm::handleSmartBulkAdd,
                                        onPickPdf = ::triggerPickPdfForStudents,
                                        onPickImage = ::triggerPickImageForStudents,
                                        onCaptureCamera = ::triggerCaptureCameraForStudents
                                    )
                                }

                                "attendance" -> {
                                    AttendanceScreen(
                                        state = state,
                                        filteredStudents = filteredStudents,
                                        onSelectDept = vm::selectDepartment,
                                        onSelectStage = vm::selectStage,
                                        onDateChange = vm::setAttendanceDate,
                                        onSelectSec = vm::setGlobalSec,
                                        onStatusChange = vm::setStudentAttendanceStatus,
                                        onToggleIntisab = vm::toggleStudentIntisabStatus,
                                        onMarkBatchAttendance = vm::markBatchAttendance,
                                        onShareDailyAbsenceReport = { stds, dateStr ->
                                            val report = vm.buildDailyAbsenceReportText(stds, dateStr)
                                            sharePlainText(context, report, "الموقف اليومي للغياب ($dateStr)")
                                        },
                                        onNotifyParentAbsenceWhatsApp = { std, dateStr, status, totAbs ->
                                            val msg = vm.buildParentAbsenceWhatsAppMessage(std, dateStr, status, totAbs)
                                            openWhatsAppOrShare(std.phone.trim(), msg)
                                        },
                                        onOpenPrintAttendance = { vm.setPrintMode(true) }
                                    )
                                }

                                "schedule" -> {
                                    ScheduleScreen(
                                        state = state,
                                        onUpdateSlot = { day, lesson, slot ->
                                            vm.updateScheduleSlot(day, lesson, slot, context)
                                        },
                                        onCopyDaySchedule = { fromDay, toDay ->
                                            vm.copyDaySchedule(fromDay, toDay, context)
                                        },
                                        onClearDaySchedule = { day ->
                                            vm.clearDaySchedule(day, context)
                                        },
                                        onUpdateNotificationSettings = { enabled, lead ->
                                            vm.updateNotificationSettings(enabled, lead, context)
                                        },
                                        onTriggerLessonNotification = { day, lesson ->
                                            vm.triggerLessonNotificationNow(context, day, lesson)
                                        },
                                        onJumpToScheduledClass = { slot, targetView ->
                                            vm.jumpToScheduledClass(slot, targetView)
                                        }
                                    )
                                }

                                "settings" -> {
                                    SettingsScreen(
                                        state = state,
                                        activeTab = settingsTab,
                                        onSelectTab = { settingsTab = it },
                                        onPinChange = vm::updateSavedPin,
                                        onRequestArchiveAndReset = { vm.setArchiveConfirmOpen(true) },
                                        onDownloadBackup = { triggerSaveBackup() },
                                        onRestoreBackup = {
                                            try {
                                                restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                            } catch (e: Exception) {
                                                vm.showToast("تعذر فتح مستعرض الملفات ⚠️")
                                            }
                                        },
                                        onMinistryChange = { vm.onSettingsFormChange(ministry = it) },
                                        onSchoolChange = { vm.onSettingsFormChange(school = it) },
                                        onTeacherChange = { vm.onSettingsFormChange(teacher = it) },
                                        onYearChange = { vm.onSettingsFormChange(year = it) },
                                        onSaveOfficialSettings = vm::handleSaveSettingsForm,
                                        onToggleDeleteMode = vm::toggleDeleteMode,
                                        onSelectDept = vm::selectDepartment,
                                        onSelectStage = vm::selectStage,
                                        onAddDept = vm::addDept,
                                        onAddStage = vm::addStage,
                                        onToggleStageMaxGrade = vm::toggleStageMaxGrade,
                                        onAddSub = vm::addSub,
                                        onAddSec = vm::addSecToStage,
                                        onRequestDeleteStruct = vm::requestDeleteStruct,
                                        onDriveUrlChange = vm::updateDriveUrl,
                                        onDriveVersionChange = vm::updateDriveTargetVersion,
                                        onDriveNotesChange = vm::updateDriveReleaseNotes,
                                        onToggleDriveShowOnStartup = vm::toggleDriveShowOnStartup,
                                        onPasteDriveUrlFromClipboard = {
                                            val clip = GoogleDriveUpdateManager.readFromClipboard(context)
                                            if (clip.isNotBlank()) {
                                                vm.updateDriveUrl(clip)
                                                vm.startAutomaticDriveApkUpdate(context, openDialog = false)
                                            } else {
                                                vm.showToast("الحافظة فارغة — انسخ رابط كوكل درايف أولاً ⚠️")
                                            }
                                        },
                                        onCopyDriveUrlToClipboard = {
                                            val urlToCopy = GoogleDriveUpdateManager.toDirectDownloadUrl(state.driveUpdateConfig.driveUrl)
                                                .ifBlank { state.driveUpdateConfig.driveUrl }
                                            if (urlToCopy.isNotBlank()) {
                                                GoogleDriveUpdateManager.copyToClipboard(context, "Google Drive Update Link", urlToCopy)
                                                vm.showToast("تم نسخ رابط تحديث كوكل درايف المباشر 📋")
                                            }
                                        },
                                        onSaveAndShowDriveUpdateNow = {
                                            vm.startAutomaticDriveApkUpdate(context, openDialog = false)
                                        },
                                        onInstallReadyDriveApk = {
                                            vm.installReadyDownloadedApk(context)
                                        },
                                        onCheckRemoteDriveUpdateNow = vm::checkRemoteDriveUpdateNow,
                                        onExportDriveUpdateJsonFile = { triggerExportUpdateJson() },
                                        onShareDriveUpdateWhatsApp = {
                                            val msg = vm.buildDriveUpdateShareMessage()
                                            openWhatsAppOrShare("", msg)
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }
                }
            }
        }

        // Modals
        if (state.isUnlockModalOpen) {
            ActivationModalOrLockScreen(
                state = state,
                isFullScreenLock = false,
                onActivationInputChange = vm::onActivationInputChange,
                onActivateClick = vm::handleActivation,
                onContactWhatsApp = {
                    val msg = "مرحباً أستاذ وعد، أود تفعيل تطبيق بوصلة الأستاذ.\nمعرف جهازي هو: ${state.activationState.deviceID}"
                    openWhatsAppOrShare(WaadCalculator.ADMIN_WHATSAPP, msg)
                },
                onDismiss = { vm.setUnlockModalOpen(false) }
            )
        }

        state.profileStudent?.let { profStd ->
            StudentProfileModal(
                state = state,
                student = profStd,
                calculateResult = { stdId, sub -> vm.calculateStudentResult(stdId, sub, state) },
                onToggleTag = vm::toggleStudentTag,
                onApplyQarar = { stdId, sub -> vm.handleApplyQarar(stdId, sub, 5) },
                onUpdateMissingHomework = vm::updateStudentMissingHomework,
                onOpenBonus = vm::openBonusModal,
                onOpenFullReportShare = { vm.openShareModal(it, "all") },
                onDismiss = { vm.setProfileStudent(null) }
            )
        }

        if (state.bonusModal.isOpen) {
            BonusGradeModal(
                state = state,
                onSubjectChange = vm::onBonusSubjectChange,
                onMonthFieldChange = vm::onBonusMonthFieldChange,
                onApplyBonus = vm::applyBonusIncrement,
                onDismiss = vm::closeBonusModal
            )
        }

        if (state.shareModal.isOpen && state.shareModal.student != null) {
            val previewMsg = remember(
                state.shareModal.student,
                state.shareModal.reportScope,
                state.grades,
                state.studentTags,
                state.attendance,
                state.sectionNotes,
                state.missingHomework,
                state.settings
            ) {
                vm.buildWhatsAppMessage(state.shareModal.student, state.shareModal.reportScope, state)
            }
            WhatsAppShareModal(
                state = state,
                previewMessage = previewMsg,
                onPhoneChange = { vm.updateShareModal(phone = it) },
                onScopeChange = { vm.updateShareModal(scope = it) },
                onSendWhatsApp = {
                    vm.saveSharePhoneIfNeeded()
                    val phone = state.shareModal.parentPhone.trim()
                    vm.closeShareModal()
                    openWhatsAppOrShare(phone, previewMsg)
                },
                onDismiss = vm::closeShareModal
            )
        }

        if (state.isBulkOpen) {
            BulkAddModal(
                state = state,
                onTargetSecChange = vm::onBulkTargetSecChange,
                onBulkTextChange = vm::onBulkTextChange,
                onConfirmBulkAdd = vm::handleSmartBulkAdd,
                onPickPdf = ::triggerPickPdfForStudents,
                onPickImage = ::triggerPickImageForStudents,
                onCaptureCamera = ::triggerCaptureCameraForStudents,
                onDismiss = { vm.setBulkModalOpen(false) }
            )
        }

        if (state.isExtractingSmart) {
            SmartExtractionLoadingDialog(
                darkMode = state.darkMode,
                statusLabel = state.smartExtractionSourceLabel,
                onCancel = vm::dismissSmartExtractionModal
            )
        }

        if (state.smartExtractionResult != null) {
            SmartImportReviewModal(
                state = state,
                onTargetSecChange = vm::onBulkTargetSecChange,
                onToggleCandidate = vm::toggleSmartCandidateSelection,
                onToggleAll = vm::toggleAllSmartCandidates,
                onUpdateCandidateName = vm::updateSmartCandidateName,
                onDeleteCandidate = vm::deleteSmartCandidate,
                onLoadAllRawLines = vm::loadAllRawLinesIntoCandidates,
                onCopyToBulkText = vm::copySmartCandidatesToBulkText,
                onConfirmDirectImport = vm::confirmSmartExtractedStudentsDirect,
                onPickAnotherPdf = ::triggerPickPdfForStudents,
                onPickAnotherImage = ::triggerPickImageForStudents,
                onDismiss = vm::dismissSmartExtractionModal
            )
        }

        if (state.diceModal.isOpen) {
            RandomStudentDiceModal(
                state = state,
                onReroll = vm::pickRandomStudent,
                onDismiss = vm::closeDiceModal
            )
        }

        if (state.isCertificateModalOpen) {
            TopStudentsCertificatesModal(
                state = state,
                filteredStudents = filteredStudents,
                calculateResult = { stdId, sub -> vm.calculateStudentResult(stdId, sub, state) },
                onShareStudentReport = { vm.openShareModal(it, "all") },
                onDismiss = { vm.setCertificateModalOpen(false) }
            )
        }

        if (state.deleteModal != null) {
            ConfirmDangerDialog(
                darkMode = state.darkMode,
                title = "تأكيد الحذف ⚠️",
                message = "هل أنت متأكد من الحذف؟\nسيتم مسح البيانات والطلاب المرتبطين بهذا القسم نهائياً من السجل.",
                confirmText = "نعم، احذف 🗑️",
                onConfirm = vm::confirmDeleteStruct,
                onDismiss = vm::cancelDeleteStruct
            )
        }

        if (state.deleteStudentConfirmId != null) {
            ConfirmDangerDialog(
                darkMode = state.darkMode,
                title = "حذف الطالب ⚠️",
                message = "هل أنت متأكد من حذف هذا الطالب وجميع درجاته نهائياً من السجل؟",
                confirmText = "حذف نهائي 🗑️",
                onConfirm = vm::confirmDeleteStudent,
                onDismiss = { vm.requestDeleteStudent(null) }
            )
        }

        if (state.archiveConfirmOpen) {
            AcademicYearResetModal(
                darkMode = state.darkMode,
                totalStudentsCount = state.students.size,
                onConfirmReset = { deleteAllStudents ->
                    triggerSaveBackup()
                    vm.confirmArchiveAndReset(deleteAllStudents = deleteAllStudents)
                },
                onDismiss = { vm.setArchiveConfirmOpen(false) }
            )
        }

        if (state.driveUpdateConfig.isUpdateDialogOpen) {
            GoogleDriveUpdateDialog(
                config = state.driveUpdateConfig,
                darkMode = state.darkMode,
                onDriveUrlChange = vm::updateDriveUrl,
                onPasteDriveLink = {
                    val clip = GoogleDriveUpdateManager.readFromClipboard(context)
                    if (clip.isNotBlank()) {
                        vm.updateDriveUrl(clip)
                        vm.startAutomaticDriveApkUpdate(context, openDialog = true)
                    } else {
                        vm.showToast("الحافظة فارغة — انسخ رابط كوكل درايف أولاً ⚠️")
                    }
                },
                onStartAutoUpdateNow = {
                    vm.startAutomaticDriveApkUpdate(context, openDialog = true)
                },
                onInstallReadyApk = {
                    vm.installReadyDownloadedApk(context)
                },
                onOpenInGoogleDrive = {
                    val raw = state.driveUpdateConfig.driveUrl.trim()
                    if (raw.isBlank()) {
                        vm.showToast("يرجى لصق رابط كوكل درايف أولاً ⚠️")
                    } else {
                        val opened = GoogleDriveUpdateManager.openDriveUpdateUrl(
                            context = context,
                            rawUrl = raw,
                            useDirectDownload = false
                        )
                        if (!opened) {
                            vm.showToast("تعذر فتح كوكل درايف ⚠️")
                        }
                    }
                },
                onCopyDriveLink = {
                    val raw = state.driveUpdateConfig.driveUrl.trim()
                    if (raw.isBlank()) {
                        vm.showToast("لا يوجد رابط لنسخه ⚠️")
                    } else {
                        val direct = GoogleDriveUpdateManager.toDirectDownloadUrl(raw)
                        GoogleDriveUpdateManager.copyToClipboard(context, "Google Drive APK Update", direct)
                        vm.showToast("تم نسخ رابط التحديث المباشر من كوكل درايف 📋")
                    }
                },
                onDismiss = { vm.setDriveUpdateDialogOpen(false) }
            )
        }

        // Floating Toast Banner
        AnimatedVisibility(
            visible = state.toast != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Slate900,
                shadowElevation = 10.dp
            ) {
                Text(
                    text = state.toast ?: "",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}

private fun sharePlainText(context: Context, content: String, subject: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    } catch (_: Exception) {
    }
}

private fun triggerAndroidHtmlPrint(
    context: Context,
    htmlContent: String,
    jobName: String,
    isLandscape: Boolean = true
) {
    try {
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val adapter = view.createPrintDocumentAdapter(jobName)
                val mediaSize = if (isLandscape) {
                    PrintAttributes.MediaSize.ISO_A4.asLandscape()
                } else {
                    PrintAttributes.MediaSize.ISO_A4.asPortrait()
                }
                val attrs = PrintAttributes.Builder()
                    .setMediaSize(mediaSize)
                    .build()
                printManager?.print(jobName, adapter, attrs)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    } catch (_: Exception) {
    }
}
