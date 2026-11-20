package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ExtractedStudentCandidate
import com.example.data.SmartExtractionResult
import com.example.data.SmartStudentExtractor
import com.example.data.WaadDatabase
import com.example.data.WaadRepository
import com.example.model.ActivationState
import com.example.model.AppSettings
import com.example.model.BonusModalState
import com.example.model.CalculatedResult
import com.example.model.DeleteStructTarget
import com.example.model.DeptConfig
import com.example.model.DiceModalState
import com.example.model.LiveLessonInfo
import com.example.model.ScheduleNotificationSettings
import com.example.model.ScheduledClass
import com.example.model.SectionCurriculumNote
import com.example.model.ShareModalState
import com.example.model.SpeedConfig
import com.example.model.StageConfig
import com.example.model.StatsSummary
import com.example.model.Student
import com.example.model.SubjectGrades
import com.example.model.WaadCalculator
import com.example.notifications.LessonNotificationHelper
import com.example.update.DriveUpdateConfig
import com.example.update.GoogleDriveUpdateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.random.Random

data class WaadUiState(
    val darkMode: Boolean = false,
    val savedPin: String = "",
    val isLocked: Boolean = false,
    val pinInput: String = "",
    val activationState: ActivationState = ActivationState(
        isActivated = false,
        isExpired = false,
        remainingTrialDays = 7,
        subRemainingDays = 0,
        deviceID = "10000"
    ),
    val activationInput: String = "",
    val activationError: String = "",
    val isUnlockModalOpen: Boolean = false,
    val students: List<Student> = emptyList(),
    val grades: Map<String, SubjectGrades> = emptyMap(),
    val attendance: Map<String, String> = emptyMap(),
    val studentTags: Map<String, List<String>> = emptyMap(),
    val teacherSchedule: Map<String, ScheduledClass> = emptyMap(),
    val sectionNotes: Map<String, SectionCurriculumNote> = emptyMap(),
    val missingHomework: Map<String, Int> = emptyMap(),
    val notificationSettings: ScheduleNotificationSettings = ScheduleNotificationSettings(),
    val settings: AppSettings = AppSettings(),
    val settingsFormMinistry: String = "وزارة التربية",
    val settingsFormSchool: String = "اعدادية الزيتون المهنية للبنين",
    val settingsFormTeacher: String = "الأستاذ : وعد بزيع",
    val settingsFormYear: String = "2025 - 2026",
    val view: String = "dashboard", // dashboard, list, entry, attendance, schedule, settings
    val toast: String? = null,
    val selDeptIdx: Int = 0,
    val selStageIdx: Int = 0,
    val globalSec: String = "",
    val globalSub: String = "",
    val search: String = "",
    val sortFilter: String = "alpha", // alpha, high, low, fail
    val isBulkOpen: Boolean = false,
    val bulkText: String = "",
    val bulkTargetSec: String = "",
    val isExtractingSmart: Boolean = false,
    val smartExtractionSourceLabel: String = "",
    val smartExtractionResult: SmartExtractionResult? = null,
    val diceModal: DiceModalState = DiceModalState(),
    val isCertificateModalOpen: Boolean = false,
    val profileStudent: Student? = null,
    val bonusModal: BonusModalState = BonusModalState(),
    val attendanceDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val shareModal: ShareModalState = ShareModalState(),
    val speedConfig: SpeedConfig = SpeedConfig(),
    val isSpeedMode: Boolean = false,
    val isListeningVoice: Boolean = false,
    val isPrintMode: Boolean = false,
    val formId: String? = null,
    val formName: String = "",
    val formSection: String = "أ",
    val formPhone: String = "",
    val formIsIntisab: Boolean = false,
    val deleteModal: DeleteStructTarget? = null,
    val isDeleteMode: Boolean = false,
    val deleteStudentConfirmId: String? = null,
    val archiveConfirmOpen: Boolean = false,
    val driveUpdateConfig: DriveUpdateConfig = DriveUpdateConfig()
) {
    val currentDept: DeptConfig
        get() = settings.structure.getOrElse(selDeptIdx) {
            settings.structure.firstOrNull() ?: DeptConfig(
                name = "عام",
                stages = listOf(StageConfig(name = "عام", subjects = listOf("عام"), sections = listOf("أ")))
            )
        }

    val currentStage: StageConfig
        get() = currentDept.stages.getOrElse(selStageIdx) {
            currentDept.stages.firstOrNull() ?: StageConfig(
                name = "عام",
                maxGrade = 100,
                subjects = listOf("عام",),
                sections = listOf("أ")
            )
        }

    val stageSections: List<String>
        get() = currentStage.sections.ifEmpty { listOf("أ") }

    val maxAllowedGrade: Int
        get() = WaadCalculator.getStudentMaxGrade(null, settings, currentDept, currentStage)

    val failThreshold: Int
        get() = if (maxAllowedGrade == 10) 5 else 50

    val hasUnsavedSettings: Boolean
        get() = settingsFormMinistry != settings.ministryName ||
            settingsFormSchool != settings.schoolName ||
            settingsFormTeacher != settings.teacherName ||
            settingsFormYear != settings.academicYear

    val currentLiveLesson: LiveLessonInfo
        get() = WaadCalculator.computeCurrentLiveLesson(teacherSchedule)
}

class WaadViewModel(
    private val repository: WaadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WaadUiState())
    val uiState: StateFlow<WaadUiState> = _uiState.asStateFlow()

    private var toastJob: Job? = null
    private val arabicCollator: Collator = Collator.getInstance(Locale.forLanguageTag("ar"))

    init {
        val dark = repository.loadDarkMode()
        val pin = repository.loadSavedPin()
        val devId = repository.getOrCreateDeviceId()
        val token = repository.getLicenseToken()
        val actDate = repository.getActivationDateMs()
        val instDate = repository.getInstallDateMs()
        val loadedSettings = repository.loadSettings()
        val loadedSchedule = repository.loadSchedule()
        val loadedNotes = repository.loadSectionNotes()
        val loadedMissingHw = repository.loadMissingHomework()
        val loadedNotif = repository.loadNotificationSettings()
        val loadedDriveConfig = repository.loadDriveUpdateConfig()
        val firstStage = loadedSettings.structure.firstOrNull()?.stages?.firstOrNull()
        val defaultSub = firstStage?.subjects?.firstOrNull() ?: ""
        val defaultSec = firstStage?.sections?.firstOrNull() ?: "أ"

        val alreadyHandledOnStartup = GoogleDriveUpdateManager.isUpdateAlreadyHandled(
            context = null,
            config = loadedDriveConfig
        )
        val initialDriveConfig = if (alreadyHandledOnStartup) {
            val cleaned = loadedDriveConfig.copy(
                showPromptOnStartup = false,
                isUpdateDialogOpen = false
            )
            repository.saveDriveUpdateConfig(cleaned)
            cleaned
        } else {
            loadedDriveConfig.copy(
                isUpdateDialogOpen = loadedDriveConfig.showPromptOnStartup && loadedDriveConfig.driveUrl.isNotBlank()
            )
        }

        _uiState.update {
            it.copy(
                darkMode = dark,
                savedPin = pin,
                isLocked = pin.isNotBlank(),
                activationState = WaadCalculator.computeActivationState(
                    deviceID = devId,
                    savedToken = token,
                    activationDateMs = actDate,
                    installDateMs = instDate
                ),
                settings = loadedSettings,
                settingsFormMinistry = loadedSettings.ministryName,
                settingsFormSchool = loadedSettings.schoolName,
                settingsFormTeacher = loadedSettings.teacherName,
                settingsFormYear = loadedSettings.academicYear,
                teacherSchedule = loadedSchedule,
                sectionNotes = loadedNotes,
                missingHomework = loadedMissingHw,
                notificationSettings = loadedNotif,
                driveUpdateConfig = initialDriveConfig,
                globalSub = defaultSub,
                formSection = defaultSec
            )
        }

        viewModelScope.launch {
            repository.ensureInitialDataSeeded()
            repository.ensureRichScheduleIfMinimal()
            // Reload schedule in case seed or enrichment updated it
            _uiState.update { it.copy(teacherSchedule = repository.loadSchedule()) }
            // Check remote update manifest only if not already handled and strictly newer than current version
            if (initialDriveConfig.driveUrl.isNotBlank() && initialDriveConfig.showPromptOnStartup) {
                val checked = GoogleDriveUpdateManager.resolveOrCheckDriveUpdate(
                    rawInput = initialDriveConfig.driveUrl,
                    currentConfig = _uiState.value.driveUpdateConfig
                )
                val isHandled = GoogleDriveUpdateManager.isUpdateAlreadyHandled(
                    context = null,
                    config = checked
                )
                val finalStartupConfig = checked.copy(
                    showPromptOnStartup = !isHandled,
                    isUpdateDialogOpen = !isHandled
                )
                repository.saveDriveUpdateConfig(finalStartupConfig)
                _uiState.update { it.copy(driveUpdateConfig = finalStartupConfig) }
            }
        }

        viewModelScope.launch {
            repository.studentsFlow.collect { list ->
                _uiState.update { state ->
                    val updatedProfile = state.profileStudent?.let { prof ->
                        list.find { it.id == prof.id }
                    }
                    state.copy(students = list, profileStudent = updatedProfile)
                }
            }
        }

        viewModelScope.launch {
            repository.studentTagsFlow.collect { map ->
                _uiState.update { it.copy(studentTags = map) }
            }
        }

        viewModelScope.launch {
            repository.gradesFlow.collect { map ->
                _uiState.update { it.copy(grades = map) }
            }
        }

        viewModelScope.launch {
            repository.attendanceFlow.collect { map ->
                _uiState.update { it.copy(attendance = map) }
            }
        }
    }

    fun showToast(msg: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toast = msg) }
        toastJob = viewModelScope.launch {
            delay(2600)
            _uiState.update { it.copy(toast = null) }
        }
    }

    fun toggleDarkMode() {
        val next = !_uiState.value.darkMode
        repository.saveDarkMode(next)
        _uiState.update { it.copy(darkMode = next) }
    }

    fun onPinInputChange(value: String) {
        val clean = value.filter { it.isDigit() }.take(4)
        val saved = _uiState.value.savedPin
        if (clean == saved && saved.isNotBlank()) {
            _uiState.update { it.copy(isLocked = false, pinInput = "") }
            showToast("تم فتح القفل بنجاح 🔓")
        } else {
            _uiState.update { it.copy(pinInput = clean) }
        }
    }

    fun updateSavedPin(newPin: String) {
        val clean = newPin.filter { it.isDigit() }.take(4)
        repository.savePin(clean)
        _uiState.update { it.copy(savedPin = clean) }
        if (clean.length == 4) {
            showToast("تم تفعيل رمز القفل (PIN) بنجاح 🛡️")
        } else if (clean.isEmpty()) {
            showToast("تم إلغاء قفل التطبيق")
        }
    }

    fun setUnlockModalOpen(open: Boolean) {
        _uiState.update { it.copy(isUnlockModalOpen = open, activationError = "") }
    }

    fun onActivationInputChange(value: String) {
        _uiState.update { it.copy(activationInput = value, activationError = "") }
    }

    fun handleActivation() {
        val state = _uiState.value
        val code = state.activationInput.trim().uppercase()
        if (WaadCalculator.verifyActivationKey(state.activationState.deviceID, code)) {
            val now = System.currentTimeMillis()
            repository.saveActivation(code, now)
            _uiState.update {
                it.copy(
                    activationState = WaadCalculator.computeActivationState(
                        deviceID = state.activationState.deviceID,
                        savedToken = code,
                        activationDateMs = now,
                        installDateMs = repository.getInstallDateMs(),
                        nowMs = now
                    ),
                    isUnlockModalOpen = false,
                    activationError = ""
                )
            }
            showToast("تم تفعيل التطبيق بنجاح لمدة عام كامل 🎉")
        } else {
            _uiState.update { it.copy(activationError = "كود التفعيل غير صحيح ❌") }
        }
    }

    fun setView(newView: String) {
        _uiState.update { state ->
            val defaultSec = state.stageSections.firstOrNull() ?: "أ"
            if (newView == "entry") {
                state.copy(
                    view = newView,
                    formId = null,
                    formName = "",
                    formSection = defaultSec,
                    formPhone = "",
                    formIsIntisab = false,
                    bulkTargetSec = state.bulkTargetSec.takeIf { it in state.stageSections } ?: defaultSec
                )
            } else {
                state.copy(view = newView, formId = null, formIsIntisab = false)
            }
        }
    }

    fun selectDepartment(deptIdx: Int) {
        _uiState.update { state ->
            val dept = state.settings.structure.getOrElse(deptIdx) { state.currentDept }
            val firstStage = dept.stages.firstOrNull()
            val defaultSec = firstStage?.sections?.firstOrNull() ?: "أ"
            state.copy(
                selDeptIdx = deptIdx,
                selStageIdx = 0,
                globalSec = "",
                globalSub = firstStage?.subjects?.firstOrNull() ?: "",
                formSection = defaultSec,
                bulkTargetSec = defaultSec
            )
        }
    }

    fun selectStage(stageIdx: Int) {
        _uiState.update { state ->
            val stage = state.currentDept.stages.getOrElse(stageIdx) { state.currentStage }
            val defaultSec = stage.sections.firstOrNull() ?: "أ"
            state.copy(
                selStageIdx = stageIdx,
                globalSec = "",
                globalSub = stage.subjects.firstOrNull() ?: "",
                formSection = defaultSec,
                bulkTargetSec = defaultSec
            )
        }
    }

    fun setGlobalSec(sec: String) {
        _uiState.update { it.copy(globalSec = sec) }
    }

    fun setGlobalSub(sub: String) {
        _uiState.update { it.copy(globalSub = sub) }
    }

    fun setSearch(query: String) {
        _uiState.update { it.copy(search = query) }
    }

    fun setSortFilter(sort: String) {
        _uiState.update { it.copy(sortFilter = sort) }
    }

    fun jumpToLiveClass() {
        val state = _uiState.value
        val live = state.currentLiveLesson.scheduledClass
        if (live == null || live.subject.isBlank()) {
            showToast("لم تقم بتعيين جدول لهذه الحصة في إعدادات الجدول ⚠️")
            setView("schedule")
            return
        }
        jumpToScheduledClass(live, targetView = "list")
    }

    fun jumpToScheduledClass(scheduled: ScheduledClass, targetView: String = "list") {
        val state = _uiState.value
        val safeDeptIdx = scheduled.deptIdx.coerceIn(0, (state.settings.structure.size - 1).coerceAtLeast(0))
        val dept = state.settings.structure.getOrNull(safeDeptIdx)
        val safeStageIdx = scheduled.stageIdx.coerceIn(0, ((dept?.stages?.size ?: 1) - 1).coerceAtLeast(0))
        val stageName = dept?.stages?.getOrNull(safeStageIdx)?.name ?: ""
        _uiState.update {
            it.copy(
                selDeptIdx = safeDeptIdx,
                selStageIdx = safeStageIdx,
                globalSec = scheduled.section,
                globalSub = scheduled.subject,
                view = targetView
            )
        }
        val titleSuffix = if (scheduled.lessonTitle.isNotBlank()) " - ${scheduled.lessonTitle}" else ""
        showToast("تم الانتقال إلى $stageName شعبة (${scheduled.section}) • مادة (${scheduled.subject}$titleSuffix) ✅")
    }

    fun calculateStudentResult(stdId: String, subject: String, state: WaadUiState = _uiState.value): CalculatedResult {
        val key = "${stdId}_$subject"
        return WaadCalculator.calculateResults(state.grades[key], state.maxAllowedGrade)
    }

    fun getSpeedList(state: WaadUiState = _uiState.value): List<Student> {
        val deptName = state.currentDept.name
        val stageName = state.currentStage.name
        return state.students
            .filter {
                it.deptName == deptName &&
                    it.stageName == stageName &&
                    (state.globalSec.isEmpty() || it.section == state.globalSec)
            }
            .sortedWith { a, b -> arabicCollator.compare(a.name, b.name) }
    }

    fun getFilteredStudentsList(state: WaadUiState = _uiState.value): List<Student> {
        val deptName = state.currentDept.name
        val stageName = state.currentStage.name
        val activeSub = state.globalSub.ifEmpty { state.currentStage.subjects.firstOrNull() ?: "" }
        val q = state.search.trim().lowercase()

        var list = state.students.filter { s ->
            (q.isEmpty() || s.name.lowercase().contains(q)) &&
                s.deptName == deptName &&
                s.stageName == stageName &&
                (state.globalSec.isEmpty() || s.section == state.globalSec)
        }

        if (state.sortFilter == "fail" && activeSub.isNotEmpty()) {
            list = list.filter { s ->
                val r = calculateStudentResult(s.id, activeSub, state)
                val score = r.secondRoundResult ?: if (r.hasFinalResult) r.finalResult else (r.saei + r.qarar)
                r.hasSaei && score < state.failThreshold
            }
        }

        return when {
            state.sortFilter == "high" && activeSub.isNotEmpty() ->
                list.sortedByDescending { calculateStudentResult(it.id, activeSub, state).saei }
            state.sortFilter == "low" && activeSub.isNotEmpty() ->
                list.sortedBy { calculateStudentResult(it.id, activeSub, state).saei }
            else ->
                list.sortedWith { a, b -> arabicCollator.compare(a.name, b.name) }
        }
    }

    fun computeStats(state: WaadUiState = _uiState.value): StatsSummary {
        val deptName = state.currentDept.name
        val stageName = state.currentStage.name
        val filtered = state.students.filter {
            it.deptName == deptName &&
                it.stageName == stageName &&
                (state.globalSec.isEmpty() || it.section == state.globalSec)
        }
        var passed = 0
        var failed = 0
        var activeCount = 0
        val maxGrade = state.maxAllowedGrade
        val failThresh = state.failThreshold

        for (s in filtered) {
            if (state.globalSub.isNotEmpty()) {
                val res = calculateStudentResult(s.id, state.globalSub, state)
                if (res.hasSaei) {
                    activeCount++
                    val finalScore = res.secondRoundResult
                        ?: if (res.hasFinalResult) res.finalResult else min(maxGrade, res.saei + res.qarar)
                    if (finalScore >= failThresh) passed++ else failed++
                }
            } else {
                val subs = state.currentStage.subjects
                if (subs.isNotEmpty()) {
                    var sum = 0.0
                    var validSubs = 0
                    for (sub in subs) {
                        val r = calculateStudentResult(s.id, sub, state)
                        if (r.hasSaei) {
                            val score = r.secondRoundResult
                                ?: if (r.hasFinalResult) r.finalResult else min(maxGrade, r.saei + r.qarar)
                            sum += score
                            validSubs++
                        }
                    }
                    if (validSubs > 0) {
                        activeCount++
                        if ((sum / validSubs) >= failThresh) passed++ else failed++
                    }
                }
            }
        }
        val total = filtered.size
        val rate = if (activeCount > 0) String.format(Locale.US, "%.1f", (passed.toDouble() / activeCount) * 100.0) else "0.0"
        val label = if (state.globalSub.isNotEmpty()) "نسبة (${state.globalSub})" else "نسبة النجاح العامة"
        return StatsSummary(
            total = total,
            activeCount = activeCount,
            passed = passed,
            failed = failed,
            rate = rate,
            labelText = label
        )
    }

    // Student Entry & Edit
    fun onFormNameChange(name: String) = _uiState.update { it.copy(formName = name) }
    fun onFormSectionChange(sec: String) = _uiState.update { it.copy(formSection = sec) }
    fun onFormPhoneChange(phone: String) = _uiState.update { it.copy(formPhone = phone) }
    fun onFormIntisabChange(isIntisab: Boolean) = _uiState.update { it.copy(formIsIntisab = isIntisab) }

    fun startEditStudent(student: Student) {
        _uiState.update {
            it.copy(
                formId = student.id,
                formName = student.name,
                formSection = student.section,
                formPhone = student.phone,
                formIsIntisab = WaadCalculator.isStudentIntisab(student.id, it.studentTags),
                view = "entry"
            )
        }
    }

    fun handleSaveStudent() {
        val state = _uiState.value
        val cleanName = state.formName.trim().replace(Regex("\\s+"), " ")
        val sec = state.formSection.ifBlank { state.stageSections.firstOrNull() ?: "أ" }
        if (cleanName.isBlank()) {
            showToast("يرجى إكمال الاسم والشعبة ⚠️")
            return
        }

        val existingEditing = state.formId?.let { id -> state.students.find { it.id == id } }
        val targetDept = existingEditing?.deptName ?: state.currentDept.name
        val targetStage = existingEditing?.stageName ?: state.currentStage.name
        val normalizedInput = WaadCalculator.normalizeStudentNameForDedup(cleanName)

        val duplicateStudent = state.students.firstOrNull { std ->
            std.id != state.formId &&
                std.deptName == targetDept &&
                std.stageName == targetStage &&
                std.section == sec &&
                WaadCalculator.normalizeStudentNameForDedup(std.name) == normalizedInput
        }
        if (duplicateStudent != null) {
            showToast("⚠️ الطالب ($cleanName) مسجل مسبقاً في شعبة ($sec) — تم منع تكرار الاسم")
            return
        }

        viewModelScope.launch {
            if (state.formId != null) {
                val updated = Student(
                    id = state.formId,
                    name = cleanName,
                    section = sec,
                    phone = state.formPhone.trim(),
                    deptName = targetDept,
                    stageName = targetStage
                )
                val baseTags = (state.studentTags[state.formId] ?: emptyList())
                    .filterNot { it == WaadCalculator.INTISAB_TAG || it.contains("انتساب") }
                val finalTags = if (state.formIsIntisab) {
                    listOf(WaadCalculator.INTISAB_TAG) + baseTags
                } else {
                    baseTags
                }
                repository.saveStudent(updated, finalTags)
                if (state.formIsIntisab) {
                    repository.saveAttendance("${updated.id}_${state.attendanceDate}", "intisab")
                } else if (state.attendance["${updated.id}_${state.attendanceDate}"] == "intisab") {
                    repository.saveAttendance("${updated.id}_${state.attendanceDate}", "present")
                }
                _uiState.update {
                    it.copy(
                        formId = null,
                        formName = "",
                        formSection = state.stageSections.firstOrNull() ?: "أ",
                        formPhone = "",
                        formIsIntisab = false,
                        view = "list"
                    )
                }
                showToast("تم تحديث بيانات الطالب بنجاح ✅")
            } else {
                val newStd = Student(
                    id = "${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}",
                    name = cleanName,
                    section = sec,
                    phone = state.formPhone.trim(),
                    deptName = targetDept,
                    stageName = targetStage
                )
                val initialTags = if (state.formIsIntisab) listOf(WaadCalculator.INTISAB_TAG) else emptyList()
                repository.saveStudent(newStd, initialTags)
                if (state.formIsIntisab) {
                    repository.saveAttendance("${newStd.id}_${state.attendanceDate}", "intisab")
                }
                _uiState.update {
                    it.copy(
                        formId = null,
                        formName = "",
                        formPhone = "",
                        formIsIntisab = false
                    )
                }
                val intisabSuffix = if (state.formIsIntisab) " (طالب انتساب 🏠)" else ""
                showToast("تمت إضافة الطالب ($cleanName)$intisabSuffix بنجاح ✅")
            }
        }
    }

    fun requestDeleteStudent(studentId: String?) {
        _uiState.update { it.copy(deleteStudentConfirmId = studentId) }
    }

    fun confirmDeleteStudent() {
        val id = _uiState.value.deleteStudentConfirmId ?: return
        viewModelScope.launch {
            repository.deleteStudent(id)
            val cleanedHw = _uiState.value.missingHomework.filterKeys { !it.startsWith("${id}_") }
            if (cleanedHw.size != _uiState.value.missingHomework.size) {
                repository.saveMissingHomework(cleanedHw)
            }
            _uiState.update {
                it.copy(
                    deleteStudentConfirmId = null,
                    profileStudent = null,
                    missingHomework = cleanedHw
                )
            }
            showToast("تم حذف الطالب بنجاح 🗑️")
        }
    }

    fun deleteStudentsBulk(studentIds: List<String>) {
        val uniqueIds = studentIds.distinct()
        if (uniqueIds.isEmpty()) return
        val idSet = uniqueIds.toSet()
        viewModelScope.launch {
            repository.deleteStudentsBulk(uniqueIds)
            val cleanedHw = _uiState.value.missingHomework.filterKeys { key ->
                val stdId = key.substringBefore("_")
                stdId !in idSet
            }
            if (cleanedHw.size != _uiState.value.missingHomework.size) {
                repository.saveMissingHomework(cleanedHw)
            }
            _uiState.update {
                it.copy(
                    deleteStudentConfirmId = null,
                    profileStudent = if (it.profileStudent?.id in idSet) null else it.profileStudent,
                    missingHomework = cleanedHw
                )
            }
            showToast("تم حذف ${uniqueIds.size} طالب مع سجلاتهم نهائياً 🗑️")
        }
    }

    // Bulk Add
    fun setBulkModalOpen(open: Boolean) {
        _uiState.update {
            it.copy(
                isBulkOpen = open,
                bulkTargetSec = it.bulkTargetSec.ifBlank { it.stageSections.firstOrNull() ?: "أ" }
            )
        }
    }

    fun onBulkTextChange(text: String) = _uiState.update { it.copy(bulkText = text) }
    fun onBulkTargetSecChange(sec: String) = _uiState.update { it.copy(bulkTargetSec = sec) }

    fun handleSmartBulkAdd() {
        val state = _uiState.value
        val raw = state.bulkText.trim()
        if (raw.isEmpty()) {
            showToast("يرجى لصق أو كتابة أسماء الطلاب أولاً ⚠️")
            return
        }

        val lines = raw.split("\n")
        val defaultSec = state.bulkTargetSec.ifBlank { state.stageSections.firstOrNull() ?: "أ" }
        val newStds = mutableListOf<Student>()
        val now = System.currentTimeMillis()
        val curDept = state.currentDept.name
        val curStage = state.currentStage.name

        // Track existing (section + normalizedName) keys in this department & stage to prevent duplicates
        val existingSectionNames = state.students
            .asSequence()
            .filter { it.deptName == curDept && it.stageName == curStage }
            .map { "${it.section}__${WaadCalculator.normalizeStudentNameForDedup(it.name)}" }
            .toMutableSet()

        var skippedDuplicates = 0

        lines.forEachIndexed { idx, line ->
            val text = line.trim()
            if (text.length < 2) return@forEachIndexed
            var namePart = text
            var secPart = defaultSec

            if (text.contains("\t")) {
                val p = text.split("\t")
                namePart = p[0]
                if (p.getOrNull(1)?.trim()?.isNotEmpty() == true) secPart = p[1].trim()
            } else if (text.contains(",")) {
                val p = text.split(",")
                namePart = p[0]
                if (p.getOrNull(1)?.trim()?.isNotEmpty() == true) secPart = p[1].trim()
            }
            namePart = namePart
                .replace(Regex("^[\\d\\s\\-\\.•*#)]+"), "")
                .trim()
                .replace(Regex("\\s+"), " ")
            if (namePart.length >= 2) {
                val normKey = "${secPart}__${WaadCalculator.normalizeStudentNameForDedup(namePart)}"
                if (normKey in existingSectionNames) {
                    skippedDuplicates++
                } else {
                    existingSectionNames.add(normKey)
                    newStds.add(
                        Student(
                            id = "${now}_${idx}_${Random.nextInt(100, 999)}",
                            name = namePart,
                            section = secPart,
                            phone = "",
                            deptName = curDept,
                            stageName = curStage
                        )
                    )
                }
            }
        }

        if (newStds.isNotEmpty()) {
            viewModelScope.launch {
                repository.saveStudentsBulk(newStds)
                _uiState.update { it.copy(bulkText = "", isBulkOpen = false) }
                val dupNote = if (skippedDuplicates > 0) " (وتم تجاهل $skippedDuplicates اسم مكرر تلقائياً)" else ""
                showToast("تمت إضافة ${newStds.size} طالب ✅$dupNote")
            }
        } else if (skippedDuplicates > 0) {
            _uiState.update { it.copy(bulkText = "", isBulkOpen = false) }
            showToast("⚠️ جميع الأسماء المدخلة ($skippedDuplicates) مسجلة مسبقاً في هذه الشعبة — لم يتم تكرار أي اسم")
        } else {
            showToast("لم يتم العثور على أسماء صالحة ⚠️")
        }
    }

    // Smart High-Accuracy PDF & Image Student Extraction
    fun extractStudentsFromPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExtractingSmart = true,
                    smartExtractionSourceLabel = "جاري تحليل واستخراج أسماء الطلاب من ملف PDF... 📄",
                    smartExtractionResult = null,
                    bulkTargetSec = it.bulkTargetSec.ifBlank { it.stageSections.firstOrNull() ?: "أ" }
                )
            }
            val result = SmartStudentExtractor.extractFromPdfUri(context, uri)
            _uiState.update {
                it.copy(
                    isExtractingSmart = false,
                    smartExtractionResult = result
                )
            }
            if (!result.success) {
                showToast(result.errorMessage ?: "تعذر استخراج الأسماء من ملف PDF ⚠️")
            } else {
                showToast("✨ تم استخراج ${result.smartCandidates.size} طالب بدقة ${result.accuracyPercent}%")
            }
        }
    }

    fun extractStudentsFromImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExtractingSmart = true,
                    smartExtractionSourceLabel = "جاري المسح الضوئي الذكي لاستخراج أسماء الطلاب من الصورة... 🖼️",
                    smartExtractionResult = null,
                    bulkTargetSec = it.bulkTargetSec.ifBlank { it.stageSections.firstOrNull() ?: "أ" }
                )
            }
            val result = SmartStudentExtractor.extractFromImageUri(context, uri)
            _uiState.update {
                it.copy(
                    isExtractingSmart = false,
                    smartExtractionResult = result
                )
            }
            if (!result.success) {
                showToast(result.errorMessage ?: "تعذر استخراج الأسماء من الصورة ⚠️")
            } else {
                showToast("✨ تم استخراج ${result.smartCandidates.size} طالب بدقة ${result.accuracyPercent}%")
            }
        }
    }

    fun extractStudentsFromCameraBitmap(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExtractingSmart = true,
                    smartExtractionSourceLabel = "جاري قراءة قائمة الطلاب الملتقطة بالكاميرا... 📷",
                    smartExtractionResult = null,
                    bulkTargetSec = it.bulkTargetSec.ifBlank { it.stageSections.firstOrNull() ?: "أ" }
                )
            }
            val result = SmartStudentExtractor.extractFromBitmap(context, bitmap, "قائمة_مصورة_بالكاميرا.jpg")
            _uiState.update {
                it.copy(
                    isExtractingSmart = false,
                    smartExtractionResult = result
                )
            }
            if (!result.success) {
                showToast(result.errorMessage ?: "تعذر قراءة القائمة من الصورة ⚠️")
            } else {
                showToast("✨ تم استخراج ${result.smartCandidates.size} طالب بدقة ${result.accuracyPercent}%")
            }
        }
    }

    fun toggleSmartCandidateSelection(candidateId: String) {
        _uiState.update { state ->
            val currentRes = state.smartExtractionResult ?: return@update state
            val updatedList = currentRes.smartCandidates.map { cand ->
                if (cand.id == candidateId) cand.copy(selected = !cand.selected) else cand
            }
            state.copy(smartExtractionResult = currentRes.copy(smartCandidates = updatedList))
        }
    }

    fun toggleAllSmartCandidates(selectAll: Boolean) {
        _uiState.update { state ->
            val currentRes = state.smartExtractionResult ?: return@update state
            val updatedList = currentRes.smartCandidates.map { it.copy(selected = selectAll) }
            state.copy(smartExtractionResult = currentRes.copy(smartCandidates = updatedList))
        }
    }

    fun updateSmartCandidateName(candidateId: String, newName: String) {
        _uiState.update { state ->
            val currentRes = state.smartExtractionResult ?: return@update state
            val updatedList = currentRes.smartCandidates.map { cand ->
                if (cand.id == candidateId) cand.copy(name = newName) else cand
            }
            state.copy(smartExtractionResult = currentRes.copy(smartCandidates = updatedList))
        }
    }

    fun deleteSmartCandidate(candidateId: String) {
        _uiState.update { state ->
            val currentRes = state.smartExtractionResult ?: return@update state
            val updatedList = currentRes.smartCandidates.filterNot { it.id == candidateId }
            state.copy(smartExtractionResult = currentRes.copy(smartCandidates = updatedList))
        }
    }

    fun loadAllRawLinesIntoCandidates() {
        _uiState.update { state ->
            val currentRes = state.smartExtractionResult ?: return@update state
            if (currentRes.allRawLines.isEmpty()) return@update state
            val rawCandidates = currentRes.allRawLines.mapIndexed { idx, line ->
                ExtractedStudentCandidate(
                    id = "raw_cand_$idx",
                    name = line,
                    selected = true
                )
            }
            state.copy(smartExtractionResult = currentRes.copy(smartCandidates = rawCandidates))
        }
    }

    fun dismissSmartExtractionModal() {
        _uiState.update {
            it.copy(
                isExtractingSmart = false,
                smartExtractionResult = null
            )
        }
    }

    fun copySmartCandidatesToBulkText() {
        val state = _uiState.value
        val res = state.smartExtractionResult ?: return
        val seenNorm = state.bulkText.lines()
            .map { it.substringBefore("\t").substringBefore(",").trim() }
            .filter { it.length >= 2 }
            .map { WaadCalculator.normalizeStudentNameForDedup(it) }
            .toMutableSet()

        val selectedNames = mutableListOf<String>()
        var skippedInCopy = 0
        for (cand in res.smartCandidates) {
            val cleanName = cand.name.trim().replace(Regex("\\s+"), " ")
            if (!cand.selected || cleanName.length < 2) continue
            val norm = WaadCalculator.normalizeStudentNameForDedup(cleanName)
            if (norm in seenNorm) {
                skippedInCopy++
                continue
            }
            seenNorm.add(norm)
            selectedNames.add(
                if (cand.detectedSection.isNotBlank()) "$cleanName\t${cand.detectedSection}"
                else cleanName
            )
        }
        if (selectedNames.isEmpty() && skippedInCopy == 0) {
            showToast("يرجى تحديد اسم طالب واحد على الأقل ⚠️")
            return
        }
        if (selectedNames.isEmpty() && skippedInCopy > 0) {
            _uiState.update { it.copy(smartExtractionResult = null) }
            showToast("⚠️ الأسماء المحددة موجودة بالفعل في حقل الإدخال الجماعي")
            return
        }
        val combined = if (state.bulkText.isBlank()) {
            selectedNames.joinToString("\n")
        } else {
            state.bulkText.trim() + "\n" + selectedNames.joinToString("\n")
        }
        _uiState.update {
            it.copy(
                bulkText = combined,
                smartExtractionResult = null
            )
        }
        val dupNote = if (skippedInCopy > 0) " (تجاهل $skippedInCopy مكرر)" else ""
        showToast("✍️ تم نقل ${selectedNames.size} اسم إلى حقل الإدخال الجماعي للمراجعة$dupNote")
    }

    fun confirmSmartExtractedStudentsDirect() {
        val state = _uiState.value
        val res = state.smartExtractionResult ?: return
        val selected = res.smartCandidates.filter { it.selected && it.name.trim().length >= 2 }
        if (selected.isEmpty()) {
            showToast("يرجى تحديد طالب واحد على الأقل للإضافة ⚠️")
            return
        }

        val defaultSec = state.bulkTargetSec.ifBlank { state.stageSections.firstOrNull() ?: "أ" }
        val curDept = state.currentDept.name
        val curStage = state.currentStage.name
        val existingSectionNames = state.students
            .asSequence()
            .filter { it.deptName == curDept && it.stageName == curStage }
            .map { "${it.section}__${WaadCalculator.normalizeStudentNameForDedup(it.name)}" }
            .toMutableSet()

        val now = System.currentTimeMillis()
        val newStds = mutableListOf<Student>()
        var skippedDuplicates = 0

        selected.forEachIndexed { idx, cand ->
            val cleanName = cand.name.trim().replace(Regex("\\s+"), " ")
            if (cleanName.length < 2) return@forEachIndexed
            val sec = cand.detectedSection.takeIf { it in state.stageSections } ?: defaultSec
            val normKey = "${sec}__${WaadCalculator.normalizeStudentNameForDedup(cleanName)}"
            if (normKey in existingSectionNames) {
                skippedDuplicates++
            } else {
                existingSectionNames.add(normKey)
                newStds.add(
                    Student(
                        id = "${now}_smart_${idx}_${Random.nextInt(100, 999)}",
                        name = cleanName,
                        section = sec,
                        phone = cand.phone.trim(),
                        deptName = curDept,
                        stageName = curStage
                    )
                )
            }
        }

        if (newStds.isEmpty() && skippedDuplicates > 0) {
            _uiState.update {
                it.copy(
                    smartExtractionResult = null,
                    isBulkOpen = false
                )
            }
            showToast("⚠️ جميع الأسماء المحددة ($skippedDuplicates) موجودة مسبقاً في شعبة ($defaultSec) — لم يتم تكرارها")
            return
        }

        viewModelScope.launch {
            repository.saveStudentsBulk(newStds)
            _uiState.update {
                it.copy(
                    smartExtractionResult = null,
                    isBulkOpen = false
                )
            }
            val dupNote = if (skippedDuplicates > 0) " (وتم تجاهل $skippedDuplicates اسم مكرر تلقائياً)" else ""
            showToast("🎉 تم استيراد وإضافة ${newStds.size} طالب إلى شعبة ($defaultSec) بنجاح ✅$dupNote")
        }
    }

    // Random Student Picker (Dice)
    fun pickRandomStudent() {
        val state = _uiState.value
        val list = state.students.filter {
            it.deptName == state.currentDept.name &&
                it.stageName == state.currentStage.name &&
                (state.globalSec.isEmpty() || it.section == state.globalSec)
        }
        if (list.isEmpty()) {
            showToast("لا يوجد طلاب متاحون في هذا النطاق للقرعة ⚠️")
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(diceModal = DiceModalState(isOpen = true, selectedStudent = null, isRolling = true))
            }
            repeat(12) {
                val randomStd = list[Random.nextInt(list.size)]
                _uiState.update {
                    it.copy(diceModal = it.diceModal.copy(selectedStudent = randomStd))
                }
                delay(80)
            }
            val finalWinner = list[Random.nextInt(list.size)]
            _uiState.update {
                it.copy(diceModal = DiceModalState(isOpen = true, selectedStudent = finalWinner, isRolling = false))
            }
        }
    }

    fun closeDiceModal() {
        _uiState.update { it.copy(diceModal = DiceModalState()) }
    }

    fun setCertificateModalOpen(open: Boolean) {
        _uiState.update { it.copy(isCertificateModalOpen = open) }
    }

    fun setProfileStudent(student: Student?) {
        _uiState.update { it.copy(profileStudent = student) }
    }

    fun toggleStudentTag(studentId: String, tagText: String) {
        val state = _uiState.value
        val student = state.students.find { it.id == studentId } ?: return
        val currentTags = state.studentTags[studentId] ?: emptyList()
        val updated = if (currentTags.contains(tagText)) {
            currentTags - tagText
        } else {
            currentTags + tagText
        }
        viewModelScope.launch {
            repository.saveStudent(student, updated)
        }
    }

    fun toggleStudentIntisabStatus(studentId: String) {
        val state = _uiState.value
        val student = state.students.find { it.id == studentId } ?: return
        val currentlyIntisab = WaadCalculator.isStudentIntisab(studentId, state.studentTags)
        val currentTags = (state.studentTags[studentId] ?: emptyList())
            .filterNot { it == WaadCalculator.INTISAB_TAG || it.contains("انتساب") }
        val updatedTags = if (currentlyIntisab) {
            currentTags
        } else {
            listOf(WaadCalculator.INTISAB_TAG) + currentTags
        }
        val dateKey = "${studentId}_${state.attendanceDate}"
        val nextAttendanceStatus = if (currentlyIntisab) "present" else "intisab"
        viewModelScope.launch {
            repository.saveStudent(student, updatedTags)
            repository.saveAttendance(dateKey, nextAttendanceStatus)
            if (!currentlyIntisab) {
                showToast("🏠 تم تحويل (${student.name}) إلى طالب انتساب — معفي من الدروس اليومية")
            } else {
                showToast("🏫 تم إرجاع (${student.name}) إلى الدوام الحضوري المنتظم ✅")
            }
        }
    }

    // Attendance
    fun setAttendanceDate(dateStr: String) {
        _uiState.update { it.copy(attendanceDate = dateStr) }
    }

    fun setStudentAttendanceStatus(studentId: String, status: String) {
        val state = _uiState.value
        val key = "${studentId}_${state.attendanceDate}"
        viewModelScope.launch {
            repository.saveAttendance(key, status)
            if (status == "intisab") {
                val student = state.students.find { it.id == studentId }
                if (student != null && !WaadCalculator.isStudentIntisab(studentId, state.studentTags)) {
                    val existingTags = state.studentTags[studentId] ?: emptyList()
                    repository.saveStudent(student, listOf(WaadCalculator.INTISAB_TAG) + existingTags)
                }
                showToast("🏠 تم تسجيل الطالب كـ (انتساب) — لا يُحتسب عليه حضور ولا غياب في الدروس اليومية")
                return@launch
            }
            val updatedMap = state.attendance.toMutableMap().apply { put(key, status) }
            val stdAbsents = updatedMap.count { (k, v) -> k.startsWith("${studentId}_") && v == "absent" }
            if (status == "absent") {
                when (stdAbsents) {
                    3 -> showToast("⚠️ تنبيه: الطالب وصل إلى 3 أيام غياب (إنذار أولي)!")
                    5 -> showToast("🚨 تنبيه قانوني: الطالب وصل إلى 5 أيام غياب (إنذار ثاني)!")
                    7 -> showToast("⛔ خطر قانوني: الطالب وصل إلى 7 أيام غياب (إنذار نهائي)!")
                    10 -> showToast("⚠️ تنبيه قانوني: الطالب وصل إلى 10 أيام غياب!")
                    15 -> showToast("⚠️ تنبيه قانوني: الطالب وصل إلى 15 يوم غياب!")
                    21 -> showToast("❌ تنبيه قانوني: الطالب تجاوز 21 يوم غياب (فصل بالغياب)!")
                }
            }
        }
    }

    fun markBatchAttendance(studentIds: List<String>, status: String) {
        if (studentIds.isEmpty()) {
            showToast("لا يوجد طلاب في هذا النطاق للتحضير الجماعي ⚠️")
            return
        }
        val state = _uiState.value
        val dateStr = state.attendanceDate
        var intisabPreservedCount = 0
        val entries = studentIds.associate { id ->
            val isIntisabStd = WaadCalculator.isStudentIntisab(id, state.studentTags) ||
                state.attendance["${id}_$dateStr"] == "intisab"
            val effectiveTarget = if (status == "present" && isIntisabStd) {
                intisabPreservedCount++
                "intisab"
            } else {
                status
            }
            "${id}_$dateStr" to effectiveTarget
        }
        viewModelScope.launch {
            repository.saveAttendancesBulk(entries)
            val label = when (status) {
                "present" -> "حاضر ✅"
                "absent" -> "غائب ❌"
                "excused" -> "إجازة 🕒"
                "intisab" -> "انتساب 🏠"
                else -> status
            }
            val regularMarked = (studentIds.size - intisabPreservedCount).coerceAtLeast(0)
            val intisabSuffix = if (intisabPreservedCount > 0) {
                " (وبقي $intisabPreservedCount طالب انتساب معفياً 🏠)"
            } else ""
            showToast("⚡ تم تسجيل ($regularMarked) طالب بحالة ($label) ليوم $dateStr$intisabSuffix")
        }
    }

    fun buildParentAbsenceWhatsAppMessage(
        student: Student,
        dateStr: String,
        status: String,
        totalAbsents: Int
    ): String {
        val state = _uiState.value
        val statusAr = when (status) {
            "absent" -> "غائب بدون عذر رسمي ❌"
            "excused" -> "مجاز زمنياً / بعذر 🕒"
            "intisab" -> "طالب انتساب (معفي من الدروس اليومية — يحضر في الامتحانات فقط) 🏠"
            else -> "حاضر ✅"
        }
        val warningLine = if (status == "intisab") {
            "🏠 *نظام الدوام:* طالب انتساب رسمي (مطالب بالحضور في أيام الامتحانات فقط)\n"
        } else {
            when {
                totalAbsents >= 7 -> "⛔ *حالة الإنذار:* إنذار نهائي (تجاوز الحد المسموح للغيابات: $totalAbsents أيام)\n"
                totalAbsents >= 5 -> "🚨 *حالة الإنذار:* إنذار ثاني ($totalAbsents أيام غياب)\n"
                totalAbsents >= 3 -> "⚠️ *حالة الإنذار:* إنذار أول ($totalAbsents أيام غياب)\n"
                totalAbsents >= 1 -> "📌 *إجمالي الغيابات المسجلة:* $totalAbsents يوم\n"
                else -> ""
            }
        }
        return buildString {
            append("السلام عليكم ورحمة الله وبركاته 🌹\n")
            append("*إشعار متابعة الدوام اليومي — ${state.settings.schoolName}*\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("👤 *اسم الطالب:* ${student.name}\n")
            append("📚 *الصف والشعبة:* ${student.stageName} (${student.section})\n")
            append("📅 *تاريخ اليوم:* $dateStr\n")
            append("📋 *حالة الدوام اليوم:* $statusAr\n")
            if (warningLine.isNotEmpty()) append(warningLine)
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("نرجو من ولي الأمر المحترم متابعة التزام الطالب بالدوام الرسمي حرصاً على مستقبله العلمي.\n")
            if (state.settings.teacherName.isNotBlank()) {
                append("👨‍🏫 *الأستاذ:* ${state.settings.teacherName}")
            }
        }
    }

    fun buildDailyAbsenceReportText(students: List<Student>, dateStr: String): String {
        val state = _uiState.value
        val statusById = students.associate { std ->
            std.id to WaadCalculator.resolveEffectiveAttendanceStatus(
                studentId = std.id,
                dateStr = dateStr,
                attendance = state.attendance,
                studentTags = state.studentTags
            )
        }
        val presentList = students.filter { statusById[it.id] == "present" }
        val absentList = students.filter { statusById[it.id] == "absent" }
        val excusedList = students.filter { statusById[it.id] == "excused" }
        val intisabList = students.filter { statusById[it.id] == "intisab" }
        val regularTotal = (students.size - intisabList.size).coerceAtLeast(0)
        val rate = if (regularTotal > 0) ((presentList.size + excusedList.size) * 100) / regularTotal else 100
        val secLabel = state.globalSec.ifBlank { "كل الشعب" }

        return buildString {
            append("📋 *الموقف اليومي للحضور والغياب*\n")
            append("🏫 *المدرسة:* ${state.settings.schoolName}\n")
            append("📚 *المرحلة:* ${state.currentDept.name} - ${state.currentStage.name} ($secLabel)\n")
            append("📅 *التاريخ:* $dateStr\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("✅ *الحاضرون:* ${presentList.size}   |   ❌ *الغائبون:* ${absentList.size}   |   🕒 *المجازون:* ${excusedList.size}")
            if (intisabList.isNotEmpty()) {
                append("   |   🏠 *الانتساب:* ${intisabList.size}")
            }
            append("\n")
            append("📊 *نسبة الدوام الحضوري:* $rate%\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            if (absentList.isEmpty()) {
                append("🎉 *لا يوجد أي غياب مسجل لهذا اليوم (حضور كامل 100%).*\n")
            } else {
                append("❌ *قائمة الطلاب الغائبين:*\n")
                absentList.forEachIndexed { idx, std ->
                    val totAbs = state.attendance.count { (k, v) -> k.startsWith("${std.id}_") && v == "absent" }
                    val warnTag = when {
                        totAbs >= 7 -> " [إنذار نهائي ⛔ - $totAbs غياب]"
                        totAbs >= 5 -> " [إنذار ثاني 🚨 - $totAbs غياب]"
                        totAbs >= 3 -> " [إنذار أول ⚠️ - $totAbs غياب]"
                        else -> " ($totAbs غياب)"
                    }
                    append("${idx + 1}. ${std.name} (شعبة ${std.section})$warnTag\n")
                }
            }
            if (excusedList.isNotEmpty()) {
                append("\n🕒 *قائمة الطلاب المجازين:*\n")
                excusedList.forEachIndexed { idx, std ->
                    append("${idx + 1}. ${std.name} (شعبة ${std.section})\n")
                }
            }
            if (intisabList.isNotEmpty()) {
                append("\n🏠 *طلاب الانتساب (معفون من الدروس اليومية — امتحانات فقط):*\n")
                intisabList.forEachIndexed { idx, std ->
                    append("${idx + 1}. ${std.name} (شعبة ${std.section})\n")
                }
            }
            if (state.settings.teacherName.isNotBlank()) {
                append("\n✍️ *إعداد الأستاذ:* ${state.settings.teacherName}")
            }
        }
    }

    // Bonus Modal
    fun openBonusModal(student: Student) {
        val state = _uiState.value
        val sub = state.globalSub.ifBlank { state.currentStage.subjects.firstOrNull() ?: "" }
        if (sub.isBlank()) {
            showToast("يرجى إضافة مواد أولاً ⚠️")
            return
        }
        val key = "${student.id}_$sub"
        val g = state.grades[key] ?: SubjectGrades()
        val currentVal = g.v1.toIntOrNull() ?: 0
        val otherSum = WaadCalculator.getMonthOtherSum(g, "v1")
        _uiState.update {
            it.copy(
                bonusModal = BonusModalState(
                    isOpen = true,
                    student = student,
                    subject = sub,
                    monthField = "v1",
                    baseGrade = currentVal,
                    otherFieldsSum = otherSum,
                    selectedBonus = 0
                )
            )
        }
    }

    fun onBonusSubjectChange(newSub: String) {
        val state = _uiState.value
        val std = state.bonusModal.student ?: return
        val key = "${std.id}_$newSub"
        val g = state.grades[key] ?: SubjectGrades()
        val valNum = g.getField(state.bonusModal.monthField).toIntOrNull() ?: 0
        val otherSum = WaadCalculator.getMonthOtherSum(g, state.bonusModal.monthField)
        _uiState.update {
            it.copy(
                bonusModal = it.bonusModal.copy(
                    subject = newSub,
                    baseGrade = valNum,
                    otherFieldsSum = otherSum,
                    selectedBonus = 0
                )
            )
        }
    }

    fun onBonusMonthFieldChange(newField: String) {
        val state = _uiState.value
        val std = state.bonusModal.student ?: return
        val key = "${std.id}_${state.bonusModal.subject}"
        val g = state.grades[key] ?: SubjectGrades()
        val valNum = g.getField(newField).toIntOrNull() ?: 0
        val otherSum = WaadCalculator.getMonthOtherSum(g, newField)
        _uiState.update {
            it.copy(
                bonusModal = it.bonusModal.copy(
                    monthField = newField,
                    baseGrade = valNum,
                    otherFieldsSum = otherSum,
                    selectedBonus = 0
                )
            )
        }
    }

    fun applyBonusIncrement(bonusPts: Int) {
        val state = _uiState.value
        val modal = state.bonusModal
        val std = modal.student ?: return
        val curMax = WaadCalculator.getStudentMaxGrade(std, state.settings, state.currentDept, state.currentStage)
        val base = modal.baseGrade
        val otherSum = modal.otherFieldsSum
        val newFieldVal = base + bonusPts
        val totalMonthWithBonus = newFieldVal + otherSum

        if (bonusPts > 0 && totalMonthWithBonus > curMax) {
            showToast("❌ ممنوع! مجموع الشهر ($totalMonthWithBonus) يتجاوز سقف المرحلة ($curMax)")
            return
        }

        val key = "${std.id}_${modal.subject}"
        val currentGrades = state.grades[key] ?: SubjectGrades()
        val updatedGrades = currentGrades.withField(modal.monthField, newFieldVal.toString())
        viewModelScope.launch {
            repository.saveGrade(key, updatedGrades)
            _uiState.update {
                it.copy(bonusModal = it.bonusModal.copy(selectedBonus = bonusPts))
            }
            if (bonusPts > 0) {
                showToast("⭐ تمت إضافة الدرجة التشجيعية (+$bonusPts) بنجاح")
            } else {
                showToast("تمت إعادة الدرجة الأصلية")
            }
        }
    }

    fun closeBonusModal() {
        _uiState.update { it.copy(bonusModal = BonusModalState()) }
    }

    // Qarar (+5 decision points across all subjects per student)
    fun handleApplyQarar(studentId: String, subject: String, neededPts: Int = 5) {
        val state = _uiState.value
        val key = "${studentId}_$subject"
        val currentGrades = state.grades[key] ?: SubjectGrades()

        if (currentGrades.qarar > 0) {
            val updated = currentGrades.copy(qarar = 0)
            viewModelScope.launch {
                repository.saveGrade(key, updated)
                showToast("تم إلغاء درجة القرار لمادة ($subject)")
            }
            return
        }

        val prefix = "${studentId}_"
        val usedAcrossOtherSubjects = state.grades
            .filterKeys { it.startsWith(prefix) && it != key }
            .values
            .sumOf { it.qarar.coerceAtLeast(0) }

        val remainingAllowance = (5 - usedAcrossOtherSubjects).coerceAtLeast(0)
        if (remainingAllowance <= 0) {
            showToast("⚠️ استنفد الطالب رصيد درجات القرار السنوي بالكامل (5 درجات موزعة على المواد)")
            return
        }
        if (neededPts > remainingAllowance) {
            showToast("⚠️ الرصيد المتبقي للطالب من درجات القرار هو ($remainingAllowance) فقط، بينما يحتاج ($neededPts) للنجاح")
            return
        }

        val updated = currentGrades.copy(qarar = neededPts)
        viewModelScope.launch {
            repository.saveGrade(key, updated)
            val leftAfter = remainingAllowance - neededPts
            showToast("⚖️ تم منح +$neededPts درجة قرار لمادة ($subject) • المتبقي من الرصيد السنوي: $leftAfter/5 ✅")
        }
    }

    // Speed Mode & Grade Entry
    fun startGradingMode() {
        val state = _uiState.value
        val sub = state.globalSub.ifBlank { state.currentStage.subjects.firstOrNull() ?: "" }
        if (sub.isBlank()) {
            showToast("يرجى اختيار المادة للرصد ⚠️")
            return
        }
        _uiState.update {
            it.copy(
                globalSub = sub,
                speedConfig = SpeedConfig(activeIndex = 0, activeTab = "m1", activeSubFieldIdx = 0),
                isSpeedMode = true
            )
        }
    }

    fun setSpeedMode(open: Boolean) {
        _uiState.update { it.copy(isSpeedMode = open) }
    }

    fun updateSpeedConfig(index: Int? = null, tab: String? = null, subFieldIdx: Int? = null) {
        _uiState.update { state ->
            val resolvedSubIdx = when {
                subFieldIdx != null -> subFieldIdx
                index != null && index != state.speedConfig.activeIndex -> 0
                tab != null && tab != state.speedConfig.activeTab -> 0
                else -> state.speedConfig.activeSubFieldIdx
            }
            state.copy(
                speedConfig = state.speedConfig.copy(
                    activeIndex = index ?: state.speedConfig.activeIndex,
                    activeTab = tab ?: state.speedConfig.activeTab,
                    activeSubFieldIdx = resolvedSubIdx
                )
            )
        }
    }

    fun getInstantMonthTotal(state: WaadUiState = _uiState.value): Int {
        val list = getSpeedList(state)
        val std = list.getOrNull(state.speedConfig.activeIndex) ?: return 0
        if (state.globalSub.isBlank()) return 0
        val g = state.grades["${std.id}_${state.globalSub}"] ?: SubjectGrades()
        val t = state.speedConfig.activeTab
        if (t == "mid") return g.mid.toIntOrNull() ?: 0
        if (t == "final") return g.finalExam.toIntOrNull() ?: 0
        if (t == "secondRound") return g.secondRound.toIntOrNull() ?: 0
        val mIdx = t.removePrefix("m").toIntOrNull() ?: 1
        val start = (mIdx - 1) * 3 + 1
        return (g.getField("v$start").toIntOrNull() ?: 0) +
            (g.getField("v${start + 1}").toIntOrNull() ?: 0) +
            (g.getField("v${start + 2}").toIntOrNull() ?: 0)
    }

    fun handleGradeChange(curStdId: String, fieldName: String, rawValue: String): Boolean {
        val state = _uiState.value
        if (curStdId.isBlank() || state.globalSub.isBlank()) return false
        val key = "${curStdId}_${state.globalSub}"
        val currentStdGrades = state.grades[key] ?: SubjectGrades()

        if (rawValue.isEmpty()) {
            val updated = currentStdGrades.withField(fieldName, "")
            _uiState.update { it.copy(grades = it.grades + (key to updated)) }
            viewModelScope.launch { repository.saveGrade(key, updated) }
            return true
        }

        val num = rawValue.toIntOrNull()
        if (num == null || num < 0) return false
        val maxGrade = state.maxAllowedGrade

        if (fieldName.startsWith("v")) {
            val otherSum = WaadCalculator.getMonthOtherSum(currentStdGrades, fieldName)
            if (num + otherSum > maxGrade) {
                showToast("❌ مرفوض! مجموع الشهر سيتجاوز ($maxGrade).")
                return false
            }
        } else {
            if (num > maxGrade) {
                showToast("❌ الدرجة مرفوضة! سقف الدرجة هو $maxGrade")
                return false
            }
        }

        val updated = currentStdGrades.withField(fieldName, num.toString())
        _uiState.update { it.copy(grades = it.grades + (key to updated)) }
        viewModelScope.launch { repository.saveGrade(key, updated) }
        return true
    }

    fun advanceSpeedFieldOrStudent() {
        val state = _uiState.value
        val list = getSpeedList(state)
        val activeTab = state.speedConfig.activeTab
        val isMonthlyTab = activeTab != "mid" && activeTab != "final" && activeTab != "secondRound"
        val curIdx = state.speedConfig.activeIndex
        val curSubIdx = state.speedConfig.activeSubFieldIdx

        if (isMonthlyTab && curSubIdx < 2) {
            val nextSubIdx = curSubIdx + 1
            val nextLabel = if (nextSubIdx == 1) "العملي" else "التحريري"
            updateSpeedConfig(subFieldIdx = nextSubIdx)
            showToast("⬅️ انتقل المؤشر إلى حقل $nextLabel")
        } else {
            if (curIdx < list.size - 1) {
                updateSpeedConfig(index = curIdx + 1, subFieldIdx = 0)
            } else {
                showToast("✅ وصلت إلى آخر طالب في القائمة")
            }
        }
    }

    fun onVoiceCandidatesSpoken(candidates: List<String>) {
        if (candidates.isEmpty()) return
        val state = _uiState.value
        val maxGrade = state.maxAllowedGrade
        val list = getSpeedList(state)
        val curStd = list.getOrNull(state.speedConfig.activeIndex) ?: return
        val firstRaw = candidates.first().trim()

        // Check quick spoken control commands first
        val combinedText = candidates.joinToString(" ")
        when {
            combinedText.contains("مسح") || combinedText.contains("حذف") || combinedText.contains("تصفير") -> {
                val activeTab = state.speedConfig.activeTab
                val fieldName = when (activeTab) {
                    "mid" -> "mid"
                    "final" -> "final"
                    "secondRound" -> "secondRound"
                    else -> {
                        val mIdx = activeTab.removePrefix("m").toIntOrNull() ?: 1
                        "v${(mIdx - 1) * 3 + state.speedConfig.activeSubFieldIdx + 1}"
                    }
                }
                handleGradeChange(curStd.id, fieldName, "")
                showToast("🗑️ تم مسح الحقل الحالي")
                return
            }
            combinedText.contains("تخطي") || combinedText.contains("التالي") -> {
                advanceSpeedFieldOrStudent()
                return
            }
            combinedText.contains("السابق") || combinedText.contains("رجوع") -> {
                val curIdx = state.speedConfig.activeIndex
                val curSubIdx = state.speedConfig.activeSubFieldIdx
                val activeTab = state.speedConfig.activeTab
                val isMonthly = activeTab != "mid" && activeTab != "final" && activeTab != "secondRound"
                if (isMonthly && curSubIdx > 0) {
                    updateSpeedConfig(subFieldIdx = curSubIdx - 1)
                } else if (curIdx > 0) {
                    updateSpeedConfig(index = curIdx - 1, subFieldIdx = 0)
                }
                return
            }
        }

        val parsedNums = WaadCalculator.parseBestSpokenCandidates(candidates, maxGrade)
        if (parsedNums.isEmpty()) {
            showToast("لم يتم التعرف على رقم واضح في: \"$firstRaw\" ⚠️")
            return
        }

        val activeTab = state.speedConfig.activeTab
        val isMonthlyTab = activeTab != "mid" && activeTab != "final" && activeTab != "secondRound"
        val subFieldLabels = listOf("اليومي", "العملي", "التحريري")

        if (!isMonthlyTab) {
            val valNum = parsedNums.first()
            if (valNum !in 0..maxGrade) {
                showToast("❌ الرقم المنطوق ($valNum) يتجاوز السقف ($maxGrade)")
                return
            }
            val accepted = handleGradeChange(curStd.id, activeTab, valNum.toString())
            if (accepted) {
                showToast("🎙️ تم رصد ($valNum) للطالب (${curStd.name}) ✅")
                viewModelScope.launch {
                    delay(450)
                    val curIdx = _uiState.value.speedConfig.activeIndex
                    if (curIdx < list.size - 1) {
                        updateSpeedConfig(index = curIdx + 1, subFieldIdx = 0)
                    }
                }
            }
            return
        }

        // Monthly Tab (m1, m2, m3, m4): Sequential progression يومي (0) -> عملي (1) -> تحريري (2) -> الطالب التالي
        val mIdx = activeTab.removePrefix("m").toIntOrNull() ?: 1
        var targetSubIdx = state.speedConfig.activeSubFieldIdx.coerceIn(0, 2)
        val enteredSummary = mutableListOf<String>()
        var completedThirdField = false

        for (valNum in parsedNums) {
            if (targetSubIdx > 2) break
            if (valNum !in 0..maxGrade) {
                showToast("❌ الرقم ($valNum) يتجاوز السقف ($maxGrade)")
                return
            }
            val tabField = "v${(mIdx - 1) * 3 + targetSubIdx + 1}"
            val accepted = handleGradeChange(curStd.id, tabField, valNum.toString())
            if (!accepted) return
            enteredSummary.add("${subFieldLabels[targetSubIdx]}: $valNum")
            if (targetSubIdx == 2) {
                completedThirdField = true
            }
            targetSubIdx++
        }

        if (completedThirdField) {
            showToast("🎙️ تم رصد (${enteredSummary.joinToString(" ، ")}) للطالب (${curStd.name}) — الانتقال للطالب التالي ✅")
            viewModelScope.launch {
                delay(450)
                val curIdx = _uiState.value.speedConfig.activeIndex
                if (curIdx < list.size - 1) {
                    updateSpeedConfig(index = curIdx + 1, subFieldIdx = 0)
                } else {
                    updateSpeedConfig(subFieldIdx = 0)
                    showToast("🎉 اكتمل رصد جميع طلاب الشعبة في هذا الشهر!")
                }
            }
        } else {
            val nextSubIdx = targetSubIdx.coerceIn(0, 2)
            updateSpeedConfig(subFieldIdx = nextSubIdx)
            val nextLabel = subFieldLabels[nextSubIdx]
            showToast("🎙️ تم رصد (${enteredSummary.joinToString(" ، ")}) ⬅️ الآن أدخل درجة ($nextLabel)")
        }
    }

    fun onVoiceRecognitionSpoken(spokenText: String) {
        onVoiceCandidatesSpoken(listOf(spokenText))
    }

    // Print Mode
    fun setPrintMode(open: Boolean) {
        val state = _uiState.value
        if (open && state.globalSub.isBlank()) {
            val firstSub = state.currentStage.subjects.firstOrNull() ?: ""
            if (firstSub.isBlank()) {
                showToast("يرجى اختيار المادة أولاً للطباعة ⚠️")
                return
            }
            _uiState.update { it.copy(globalSub = firstSub, isPrintMode = true) }
        } else {
            _uiState.update { it.copy(isPrintMode = open) }
        }
    }

    // Share / WhatsApp Modal
    fun openShareModal(student: Student, scope: String = "single") {
        _uiState.update {
            it.copy(
                shareModal = ShareModalState(
                    isOpen = true,
                    student = student,
                    parentPhone = student.phone,
                    reportScope = scope
                )
            )
        }
    }

    fun updateShareModal(phone: String? = null, scope: String? = null) {
        _uiState.update {
            it.copy(
                shareModal = it.shareModal.copy(
                    parentPhone = phone ?: it.shareModal.parentPhone,
                    reportScope = scope ?: it.shareModal.reportScope
                )
            )
        }
    }

    fun closeShareModal() {
        _uiState.update { it.copy(shareModal = ShareModalState()) }
    }

    fun saveSharePhoneIfNeeded() {
        val state = _uiState.value
        val std = state.shareModal.student ?: return
        val cleanPhone = state.shareModal.parentPhone.trim()
        if (cleanPhone != std.phone) {
            val tags = state.studentTags[std.id] ?: emptyList()
            viewModelScope.launch {
                repository.saveStudent(std.copy(phone = cleanPhone), tags)
            }
        }
    }

    fun buildWhatsAppMessage(student: Student?, scope: String, state: WaadUiState = _uiState.value): String {
        if (student == null) return ""
        val activeSub = state.globalSub.ifBlank { state.currentStage.subjects.firstOrNull() ?: "المادة العامة" }
        val subs = if (scope == "single") listOf(activeSub) else state.currentStage.subjects.ifEmpty { listOf(activeSub) }
        val stdMax = WaadCalculator.getStudentMaxGrade(student, state.settings, state.currentDept, state.currentStage)
        val stdFail = if (stdMax == 10) 5 else 50

        val tags = state.studentTags[student.id] ?: emptyList()
        val absentsCount = state.attendance.count { (k, v) -> k.startsWith("${student.id}_") && v == "absent" }

        val sb = StringBuilder()
        sb.append("السلام عليكم ورحمة الله وبركاته 🌹\n")
        sb.append("*إشعار درجات ومستوى الطالب الأكاديمي*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🏫 *المدرسة:* ${state.settings.schoolName}\n")
        sb.append("👤 *الطالب:* ${student.name}\n")
        sb.append("📚 *المرحلة:* ${student.deptName} - ${student.stageName} (${student.section})\n")
        sb.append("📅 *العام الدراسي:* ${state.settings.academicYear}\n")
        sb.append("⭐ *تقييم السلوك والمستوى:* ${if (tags.isNotEmpty()) tags.joinToString(" ، ") else "جيد جداً (طبيعي)"}\n")
        if (absentsCount > 0) {
            sb.append("⚠️ *أيام الغياب المسجلة:* $absentsCount يوم\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        for (sub in subs) {
            val res = WaadCalculator.calculateResults(state.grades["${student.id}_$sub"], stdMax)
            sb.append("📖 *مادة: $sub*\n")
            if (res.hasM1 || res.hasM2) {
                sb.append("• الفصل الأول: ش1 (${if (res.hasM1) res.m1 else "-"}) | ش2 (${if (res.hasM2) res.m2 else "-"}) | المعدل (${if (res.hasAvg1) res.avg1 else "-"})\n")
            }
            if (res.hasMid) {
                sb.append("• نصف السنة: (${res.mid})\n")
            }
            if (res.hasM3 || res.hasM4) {
                sb.append("• الفصل الثاني: ش1 (${if (res.hasM3) res.m3 else "-"}) | ش2 (${if (res.hasM4) res.m4 else "-"}) | المعدل (${if (res.hasAvg2) res.avg2 else "-"})\n")
            }
            val saeiStr = if (res.hasSaei) min(stdMax, res.saei + res.qarar).toString() else "-"
            val exemptStr = if (res.isExempt) " ✨ (معفي)" else ""
            sb.append("• السعي السنوي: ($saeiStr)$exemptStr\n")
            if (res.hasFinalResult) {
                sb.append("• الدرجة النهائية: (${res.finalResult})\n")
            }
            if (res.hasSecondRoundResult && res.secondRoundResult != null) {
                val statusStr = if (res.secondRoundResult >= stdFail) "ناجح دور ثان ✅" else "راسب ⚠️"
                sb.append("• نتيجة الدور الثاني: (${res.secondRoundResult}) - $statusStr\n")
            }
            val missingHwCount = state.missingHomework["${student.id}_$sub"] ?: 0
            if (missingHwCount > 0) {
                sb.append("• الواجبات البيتية: ⚠️ لم ينجز الواجب ($missingHwCount مرات)\n")
            } else {
                sb.append("• الواجبات البيتية: ملتزم بإنجاز الواجبات ✅\n")
            }
            val secKey = WaadCalculator.buildSectionCurriculumKey(
                deptName = student.deptName,
                stageName = student.stageName,
                section = student.section,
                subject = sub
            )
            val secNote = state.sectionNotes[secKey]
            if (secNote != null && secNote.homework.isNotBlank()) {
                sb.append("• الواجب المطلوب حالياً: ${secNote.homework}\n")
            }
            sb.append("────────────────────\n")
        }
        if (state.settings.teacherName.isNotBlank()) {
            sb.append("\n👨‍🏫 *المدرس:* ${state.settings.teacherName}\n")
        }
        return sb.toString()
    }

    // Section Curriculum & Homework Tracker
    fun saveSectionCurriculumNote(
        deptName: String,
        stageName: String,
        section: String,
        subject: String,
        lastTopic: String,
        homework: String
    ) {
        if (subject.isBlank()) {
            showToast("يرجى اختيار المادة أولاً ⚠️")
            return
        }
        val key = WaadCalculator.buildSectionCurriculumKey(deptName, stageName, section, subject)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val updatedMap = _uiState.value.sectionNotes.toMutableMap()
        if (lastTopic.trim().isEmpty() && homework.trim().isEmpty()) {
            updatedMap.remove(key)
        } else {
            updatedMap[key] = SectionCurriculumNote(
                lastTopic = lastTopic.trim(),
                homework = homework.trim(),
                updatedAt = today
            )
        }
        val finalNotesMap = updatedMap.toMap()
        repository.saveSectionNotes(finalNotesMap)
        _uiState.update { it.copy(sectionNotes = finalNotesMap) }
        showToast("📝 تم حفظ موقف المنهج والواجب لشعبة ($section) - مادة ($subject) ✅")
    }

    fun updateStudentMissingHomework(student: Student, subject: String, delta: Int, reset: Boolean = false) {
        val activeSub = subject.ifBlank {
            _uiState.value.globalSub.ifBlank {
                _uiState.value.currentStage.subjects.firstOrNull() ?: ""
            }
        }
        if (activeSub.isBlank()) {
            showToast("يرجى اختيار المادة أولاً ⚠️")
            return
        }
        val key = "${student.id}_$activeSub"
        val currentCount = _uiState.value.missingHomework[key] ?: 0
        val nextCount = if (reset) 0 else (currentCount + delta).coerceAtLeast(0)
        val updatedMap = _uiState.value.missingHomework.toMutableMap()
        if (nextCount == 0) {
            updatedMap.remove(key)
        } else {
            updatedMap[key] = nextCount
        }
        val finalMap = updatedMap.toMap()
        repository.saveMissingHomework(finalMap)
        _uiState.update { it.copy(missingHomework = finalMap) }
        if (nextCount == 0) {
            showToast("✅ تم تصفير سجل نقص الواجب للطالب (${student.name})")
        } else if (delta > 0) {
            showToast("⚠️ تم تسجيل نقص واجب على (${student.name}) — المجموع: $nextCount")
        } else {
            showToast("تم تعديل عدد مرات نقص الواجب إلى: $nextCount")
        }
    }

    fun deductDailyGradeForMissingHomework(
        student: Student,
        subject: String,
        monthField: String = "v1",
        deductPts: Int = 1
    ) {
        val activeSub = subject.ifBlank {
            _uiState.value.globalSub.ifBlank {
                _uiState.value.currentStage.subjects.firstOrNull() ?: ""
            }
        }
        if (activeSub.isBlank()) {
            showToast("يرجى اختيار المادة أولاً ⚠️")
            return
        }
        val key = "${student.id}_$activeSub"
        val currentCount = _uiState.value.missingHomework[key] ?: 0
        val nextCount = currentCount + 1
        val updatedHwMap = _uiState.value.missingHomework.toMutableMap().apply {
            put(key, nextCount)
        }.toMap()
        repository.saveMissingHomework(updatedHwMap)

        val currentGrades = _uiState.value.grades[key] ?: SubjectGrades()
        val curDailyVal = currentGrades.getField(monthField).toIntOrNull()
        if (curDailyVal != null && curDailyVal > 0 && deductPts > 0) {
            val newDailyVal = (curDailyVal - deductPts).coerceAtLeast(0)
            val updatedGrades = currentGrades.withField(monthField, newDailyVal.toString())
            _uiState.update {
                it.copy(
                    missingHomework = updatedHwMap,
                    grades = it.grades + (key to updatedGrades)
                )
            }
            viewModelScope.launch {
                repository.saveGrade(key, updatedGrades)
            }
            showToast("⚠️ تم تسجيل نقص واجب وخصم (-$deductPts) من اليومي للطالب (${student.name}) ✅")
        } else {
            _uiState.update { it.copy(missingHomework = updatedHwMap) }
            showToast("⚠️ تم تسجيل نقص واجب على (${student.name}) — المجموع: $nextCount")
        }
    }

    // Schedule & Notification Management
    fun updateScheduleSlot(day: String, lesson: Int, newClass: ScheduledClass, context: Context? = null) {
        val key = "${day}_$lesson"
        val (defStart, defEnd) = WaadCalculator.getDefaultLessonTimes(lesson)
        val normalized = newClass.copy(
            startTime = newClass.startTime.ifBlank { defStart },
            endTime = newClass.endTime.ifBlank { defEnd }
        )
        val hasCustomTimes = normalized.startTime != defStart || normalized.endTime != defEnd
        val updated = _uiState.value.teacherSchedule.toMutableMap().apply {
            if (normalized.subject.isBlank() && !hasCustomTimes) {
                remove(key)
            } else {
                put(key, normalized)
            }
        }
        repository.saveSchedule(updated)
        _uiState.update { it.copy(teacherSchedule = updated) }
        if (context != null) {
            LessonNotificationHelper.scheduleAllLessonAlarms(
                context = context,
                schedule = updated,
                settings = _uiState.value.settings,
                notifSettings = _uiState.value.notificationSettings
            )
        }
        val ordinal = WaadCalculator.getLessonOrdinalName(lesson)
        if (normalized.subject.isNotBlank()) {
            showToast("تم حفظ $ordinal (${normalized.subject}) ليوم $day ✅")
        } else if (hasCustomTimes) {
            showToast("تم تحديث توقيت $ordinal (شاغرة: ${normalized.startTime} - ${normalized.endTime}) وحساب الفرص تلقائياً ⏱️")
        } else {
            showToast("تم تفريغ $ordinal ليوم $day (شاغرة) 🗑️")
        }
    }

    fun clearDaySchedule(day: String, context: Context? = null) {
        val current = _uiState.value.teacherSchedule.toMutableMap()
        for (lesson in 1..6) {
            current.remove("${day}_$lesson")
        }
        val updated = current.toMap()
        repository.saveSchedule(updated)
        _uiState.update { it.copy(teacherSchedule = updated) }
        if (context != null) {
            LessonNotificationHelper.scheduleAllLessonAlarms(
                context = context,
                schedule = updated,
                settings = _uiState.value.settings,
                notifSettings = _uiState.value.notificationSettings
            )
        }
        showToast("تم تفريغ جدول يوم $day بالكامل (جميع الحصص شاغرة) 🗑️")
    }

    fun copyDaySchedule(fromDay: String, toDay: String, context: Context? = null) {
        if (fromDay == toDay) return
        val current = _uiState.value.teacherSchedule.toMutableMap()
        var copiedCount = 0
        for (lesson in 1..6) {
            val src = current["${fromDay}_$lesson"]
            if (src != null) {
                current["${toDay}_$lesson"] = src
                if (src.subject.isNotBlank()) copiedCount++
            } else {
                current.remove("${toDay}_$lesson")
            }
        }
        repository.saveSchedule(current)
        _uiState.update { it.copy(teacherSchedule = current) }
        if (context != null) {
            LessonNotificationHelper.scheduleAllLessonAlarms(
                context = context,
                schedule = current,
                settings = _uiState.value.settings,
                notifSettings = _uiState.value.notificationSettings
            )
        }
        showToast("تم نسخ جدول وتوقيتات يوم $fromDay ($copiedCount حصص) إلى يوم $toDay 📋")
    }

    fun updateNotificationSettings(enabled: Boolean, leadMinutes: Int, context: Context? = null) {
        val next = ScheduleNotificationSettings(enabled = enabled, leadMinutes = leadMinutes)
        repository.saveNotificationSettings(next)
        _uiState.update { it.copy(notificationSettings = next) }
        if (context != null) {
            LessonNotificationHelper.scheduleAllLessonAlarms(
                context = context,
                schedule = _uiState.value.teacherSchedule,
                settings = _uiState.value.settings,
                notifSettings = next
            )
        }
        if (enabled) {
            val timingLabel = if (leadMinutes == 0) "عند بدء الحصة" else "قبل $leadMinutes دقائق"
            showToast("🔔 تم تفعيل إشعارات الحصص ($timingLabel)")
        } else {
            showToast("🔕 تم إيقاف إشعارات الحصص")
        }
    }

    fun triggerLessonNotificationNow(context: Context, dayName: String? = null, lessonNum: Int? = null) {
        val state = _uiState.value
        val live = state.currentLiveLesson
        val targetDay = dayName ?: live.dayName
        val targetLesson = lessonNum ?: live.lessonNum
        val slot = state.teacherSchedule["${targetDay}_$targetLesson"]
            ?: live.scheduledClass
            ?: state.teacherSchedule.values.firstOrNull { it.subject.isNotBlank() }

        if (slot == null || slot.subject.isBlank()) {
            showToast("يرجى تعيين حصة واحدة على الأقل في الجدول لإرسال التنبيه ⚠️")
            return
        }

        val dept = state.settings.structure.getOrElse(slot.deptIdx) { state.currentDept }
        val stage = dept.stages.getOrElse(slot.stageIdx) { state.currentStage }
        val (startStr, endStr) = WaadCalculator.resolveLessonTimes(targetLesson, slot)

        val sent = LessonNotificationHelper.showLessonNotification(
            context = context,
            dayName = targetDay,
            lessonNum = targetLesson,
            deptIdx = slot.deptIdx,
            stageIdx = slot.stageIdx,
            deptName = dept.name,
            stageName = stage.name,
            section = slot.section,
            subject = slot.subject,
            lessonTitle = slot.lessonTitle,
            startTime = startStr,
            endTime = endStr,
            leadMinutes = 0
        )

        val ordinal = WaadCalculator.getLessonOrdinalName(targetLesson)
        if (sent) {
            showToast("🔔 تم إرسال إشعار ($ordinal — ${slot.subject}) إلى شريط الإشعارات!")
        } else {
            showToast("⚠️ يرجى السماح بصلاحية الإشعارات لتنبيه المدرس بالحصص")
        }
    }

    // Settings & Structure Management
    fun onSettingsFormChange(
        ministry: String? = null,
        school: String? = null,
        teacher: String? = null,
        year: String? = null
    ) {
        _uiState.update {
            it.copy(
                settingsFormMinistry = ministry ?: it.settingsFormMinistry,
                settingsFormSchool = school ?: it.settingsFormSchool,
                settingsFormTeacher = teacher ?: it.settingsFormTeacher,
                settingsFormYear = year ?: it.settingsFormYear
            )
        }
    }

    fun handleSaveSettingsForm() {
        val state = _uiState.value
        val updated = state.settings.copy(
            ministryName = state.settingsFormMinistry.trim(),
            schoolName = state.settingsFormSchool.trim(),
            teacherName = state.settingsFormTeacher.trim(),
            academicYear = state.settingsFormYear.trim()
        )
        repository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
        showToast("تم حفظ الإعدادات الرسمية بنجاح ✅")
    }

    fun toggleDeleteMode() {
        _uiState.update { it.copy(isDeleteMode = !it.isDeleteMode) }
    }

    fun addDept(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val state = _uiState.value
        val newStruct = state.settings.structure + DeptConfig(name = clean, stages = emptyList())
        val updated = state.settings.copy(structure = newStruct)
        repository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
        showToast("تمت إضافة المرحلة ✅")
    }

    fun addStage(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val state = _uiState.value
        val struct = state.settings.structure.toMutableList()
        val dept = struct.getOrNull(state.selDeptIdx) ?: return
        val isPrimaryLow = dept.name.contains("ابتدائ") && !clean.contains("خامس") && !clean.contains("سادس")
        val newStage = StageConfig(
            name = clean,
            maxGrade = if (isPrimaryLow) 10 else 100,
            subjects = emptyList(),
            sections = listOf("أ")
        )
        struct[state.selDeptIdx] = dept.copy(stages = dept.stages + newStage)
        val updated = state.settings.copy(structure = struct)
        repository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
        showToast("تمت إضافة الصف ✅")
    }

    fun toggleStageMaxGrade() {
        val state = _uiState.value
        val struct = state.settings.structure.toMutableList()
        val dept = struct.getOrNull(state.selDeptIdx) ?: return
        val stages = dept.stages.toMutableList()
        val stage = stages.getOrNull(state.selStageIdx) ?: return
        val nextMax = if (stage.maxGrade == 10) 100 else 10
        stages[state.selStageIdx] = stage.copy(maxGrade = nextMax)
        struct[state.selDeptIdx] = dept.copy(stages = stages)
        val updated = state.settings.copy(structure = struct)
        repository.saveSettings(updated)
        _uiState.update { it.copy(settings = updated) }
        showToast("تم تغيير نظام تقييم (${stage.name}) إلى $nextMax")
    }

    fun addSub(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val state = _uiState.value
        val struct = state.settings.structure.toMutableList()
        val dept = struct.getOrNull(state.selDeptIdx) ?: return
        val stages = dept.stages.toMutableList()
        val stage = stages.getOrNull(state.selStageIdx) ?: return
        if (!stage.subjects.contains(clean)) {
            stages[state.selStageIdx] = stage.copy(subjects = stage.subjects + clean)
            struct[state.selDeptIdx] = dept.copy(stages = stages)
            val updated = state.settings.copy(structure = struct)
            repository.saveSettings(updated)
            _uiState.update {
                it.copy(
                    settings = updated,
                    globalSub = it.globalSub.ifBlank { clean }
                )
            }
            showToast("تمت إضافة المادة ✅")
        } else {
            showToast("هذه المادة موجودة مسبقاً")
        }
    }

    fun addSecToStage(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val state = _uiState.value
        val struct = state.settings.structure.toMutableList()
        val dept = struct.getOrNull(state.selDeptIdx) ?: return
        val stages = dept.stages.toMutableList()
        val stage = stages.getOrNull(state.selStageIdx) ?: return
        if (!stage.sections.contains(clean)) {
            stages[state.selStageIdx] = stage.copy(sections = stage.sections + clean)
            struct[state.selDeptIdx] = dept.copy(stages = stages)
            val updated = state.settings.copy(structure = struct)
            repository.saveSettings(updated)
            _uiState.update { it.copy(settings = updated) }
            showToast("تمت إضافة الشعبة ✅")
        } else {
            showToast("هذه الشعبة موجودة مسبقاً")
        }
    }

    fun requestDeleteStruct(type: String, idx: Int) {
        _uiState.update { it.copy(deleteModal = DeleteStructTarget(type, idx)) }
    }

    fun cancelDeleteStruct() {
        _uiState.update { it.copy(deleteModal = null) }
    }

    fun confirmDeleteStruct() {
        val state = _uiState.value
        val target = state.deleteModal ?: return
        val struct = state.settings.structure.toMutableList()

        viewModelScope.launch {
            var nextDeptIdx = state.selDeptIdx
            var nextStageIdx = state.selStageIdx
            var nextGlobalSub = state.globalSub

            when (target.type) {
                "d" -> {
                    val dept = struct.getOrNull(target.idx)
                    if (dept != null) {
                        val removedIds = state.students.filter { it.deptName == dept.name }.map { it.id }
                        if (removedIds.isNotEmpty()) {
                            repository.deleteStudentsByDept(dept.name, removedIds)
                        }
                        struct.removeAt(target.idx)
                        nextDeptIdx = 0
                        nextStageIdx = 0
                    }
                }
                "s" -> {
                    val dept = struct.getOrNull(state.selDeptIdx)
                    val stage = dept?.stages?.getOrNull(target.idx)
                    if (dept != null && stage != null) {
                        val removedIds = state.students
                            .filter { it.deptName == dept.name && it.stageName == stage.name }
                            .map { it.id }
                        if (removedIds.isNotEmpty()) {
                            repository.deleteStudentsByStage(dept.name, stage.name, removedIds)
                        }
                        val stages = dept.stages.toMutableList().apply { removeAt(target.idx) }
                        struct[state.selDeptIdx] = dept.copy(stages = stages)
                        nextStageIdx = 0
                    }
                }
                "b" -> {
                    val dept = struct.getOrNull(state.selDeptIdx)
                    val stage = dept?.stages?.getOrNull(state.selStageIdx)
                    if (dept != null && stage != null && target.idx in stage.subjects.indices) {
                        val subName = stage.subjects[target.idx]
                        val subs = stage.subjects.toMutableList().apply { removeAt(target.idx) }
                        val stages = dept.stages.toMutableList().apply {
                            set(state.selStageIdx, stage.copy(subjects = subs))
                        }
                        struct[state.selDeptIdx] = dept.copy(stages = stages)
                        if (nextGlobalSub == subName) nextGlobalSub = subs.firstOrNull() ?: ""
                    }
                }
                "sec" -> {
                    val dept = struct.getOrNull(state.selDeptIdx)
                    val stage = dept?.stages?.getOrNull(state.selStageIdx)
                    if (dept != null && stage != null && target.idx in stage.sections.indices) {
                        val secs = stage.sections.toMutableList().apply { removeAt(target.idx) }
                        val stages = dept.stages.toMutableList().apply {
                            set(state.selStageIdx, stage.copy(sections = secs))
                        }
                        struct[state.selDeptIdx] = dept.copy(stages = stages)
                    }
                }
            }

            val updatedSettings = state.settings.copy(structure = struct)
            repository.saveSettings(updatedSettings)
            val refreshedHw = repository.loadMissingHomework()
            _uiState.update {
                it.copy(
                    settings = updatedSettings,
                    selDeptIdx = nextDeptIdx,
                    selStageIdx = nextStageIdx,
                    globalSub = nextGlobalSub,
                    missingHomework = refreshedHw,
                    deleteModal = null
                )
            }
            showToast("تم الحذف وتنظيف السجلات بنجاح 🗑️")
        }
    }

    // Backup, Restore & Archive
    fun setArchiveConfirmOpen(open: Boolean) {
        _uiState.update { it.copy(archiveConfirmOpen = open) }
    }

    fun confirmArchiveAndReset(deleteAllStudents: Boolean = true) {
        val state = _uiState.value
        viewModelScope.launch {
            repository.resetFullAcademicYear(
                deleteStudents = deleteAllStudents,
                currentStudents = state.students
            )
            _uiState.update {
                it.copy(
                    archiveConfirmOpen = false,
                    missingHomework = emptyMap(),
                    sectionNotes = emptyMap(),
                    profileStudent = null
                )
            }
            if (deleteAllStudents) {
                showToast("تمت الأرشفة وتصفير العام الدراسي بالكامل (مسح الطلاب والدرجات والغيابات) ✅")
            } else {
                showToast("تمت الأرشفة وتصفير الدرجات والغيابات مع الإبقاء على أسماء الطلاب ✅")
            }
        }
    }

    fun generateBackupJsonString(): String {
        val state = _uiState.value
        return repository.exportFullBackupJson(
            students = state.students,
            grades = state.grades,
            attendance = state.attendance,
            studentTags = state.studentTags,
            teacherSchedule = state.teacherSchedule,
            settings = state.settings,
            sectionNotes = state.sectionNotes,
            missingHomework = state.missingHomework
        )
    }

    fun restoreFromBackupJsonString(jsonStr: String) {
        viewModelScope.launch {
            val res = repository.importFullBackupJson(jsonStr)
            if (res != null) {
                val (newSettings, newSchedule) = res
                val firstStage = newSettings.structure.firstOrNull()?.stages?.firstOrNull()
                val defaultSub = firstStage?.subjects?.firstOrNull() ?: ""
                val defaultSec = firstStage?.sections?.firstOrNull() ?: "أ"
                _uiState.update {
                    it.copy(
                        settings = newSettings,
                        settingsFormMinistry = newSettings.ministryName,
                        settingsFormSchool = newSettings.schoolName,
                        settingsFormTeacher = newSettings.teacherName,
                        settingsFormYear = newSettings.academicYear,
                        teacherSchedule = newSchedule,
                        sectionNotes = repository.loadSectionNotes(),
                        missingHomework = repository.loadMissingHomework(),
                        notificationSettings = repository.loadNotificationSettings(),
                        selDeptIdx = 0,
                        selStageIdx = 0,
                        globalSec = "",
                        globalSub = defaultSub,
                        formSection = defaultSec,
                        bulkTargetSec = defaultSec
                    )
                }
                showToast("تمت استعادة النسخة الاحتياطية وتحديث المتغيرات بنجاح 🔄")
            } else {
                showToast("ملف النسخة الاحتياطية غير صالح ❌")
            }
        }
    }

    private fun escapeHtml(raw: String): String {
        return raw
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun escapeCsvCell(raw: String): String {
        var s = raw.replace("\"", "\"\"")
        if (s.isNotEmpty() && (s[0] == '=' || s[0] == '+' || s[0] == '-' || s[0] == '@' || s[0] == '\t' || s[0] == '\r')) {
            s = "'$s"
        }
        return "\"$s\""
    }

    fun buildExcelCsvContent(): String? {
        val state = _uiState.value
        val sub = state.globalSub.ifBlank { state.currentStage.subjects.firstOrNull() ?: "" }
        if (sub.isBlank()) {
            showToast("يرجى اختيار المادة أولاً لتصدير الكشف ⚠️")
            return null
        }
        val headers = listOf(
            "ت", "اسم الطالب", "الشعبة", "شهر 1", "شهر 2", "معدل ف1", "نصف السنة",
            "شهر 1", "شهر 2", "معدل ف2", "السعي السنوي", "النهائي", "الدور الثاني", "القرار", "النتيجة النهائية"
        )
        val filtered = getFilteredStudentsList(state)
        val rows = filtered.mapIndexed { idx, s ->
            val r = calculateStudentResult(s.id, sub, state)
            val finalScore = r.secondRoundResult ?: if (r.hasFinalResult) r.finalResult else (r.saei + r.qarar)
            val status = if (r.hasSaei) (if (finalScore >= state.failThreshold) "ناجح" else "راسب") else "غير مرصود"
            listOf(
                (idx + 1).toString(),
                escapeCsvCell(s.name),
                escapeCsvCell(s.section),
                if (r.hasM1) r.m1.toString() else "-",
                if (r.hasM2) r.m2.toString() else "-",
                if (r.hasAvg1) r.avg1.toString() else "-",
                if (r.hasMid) r.mid.toString() else "-",
                if (r.hasM3) r.m3.toString() else "-",
                if (r.hasM4) r.m4.toString() else "-",
                if (r.hasAvg2) r.avg2.toString() else "-",
                if (r.hasSaei) r.saei.toString() else "-",
                if (r.hasFinal) r.finalExam.toString() else "-",
                if (r.hasSecondRound) r.secondRoundExam.toString() else "-",
                r.qarar.toString(),
                escapeCsvCell(status)
            ).joinToString(",")
        }
        return "\uFEFF" + (listOf(headers.joinToString(",")) + rows).joinToString("\n")
    }

    fun buildPrintableHtml(
        templateType: String = "annual",
        monthKey: String = "m1",
        colorTheme: Boolean = true,
        isLandscape: Boolean = true,
        showSignatures: Boolean = true,
        showSupervisorSign: Boolean = false,
        blankColumnsCount: Int = 8,
        sortOrder: String = "alpha"
    ): String {
        val state = _uiState.value
        val sub = state.globalSub.ifBlank { state.currentStage.subjects.firstOrNull() ?: "---" }
        val safeSub = escapeHtml(sub)
        val baseFiltered = getFilteredStudentsList(state)
        val maxGrade = state.maxAllowedGrade
        val failThresh = state.failThreshold

        val filtered = when (sortOrder) {
            "top_score" -> baseFiltered.sortedByDescending { s ->
                val res = calculateStudentResult(s.id, sub, state)
                val effectiveSaei = min(maxGrade, res.saei + res.qarar)
                res.secondRoundResult ?: if (res.hasFinalResult) res.finalResult else effectiveSaei
            }
            "passed_only" -> baseFiltered.filter { s ->
                val res = calculateStudentResult(s.id, sub, state)
                val effectiveSaei = min(maxGrade, res.saei + res.qarar)
                val finalScore = res.secondRoundResult ?: if (res.hasFinalResult) res.finalResult else effectiveSaei
                res.hasSaei && finalScore >= failThresh
            }
            "failed_only" -> baseFiltered.filter { s ->
                val res = calculateStudentResult(s.id, sub, state)
                val effectiveSaei = min(maxGrade, res.saei + res.qarar)
                val finalScore = res.secondRoundResult ?: if (res.hasFinalResult) res.finalResult else effectiveSaei
                res.hasSaei && finalScore < failThresh
            }
            else -> baseFiltered.sortedBy { it.name }
        }

        val stats = computeStats(state)
        val pageOrientation = if (isLandscape) "landscape" else "portrait"
        val thBg = if (colorTheme) "#eef2ff" else "#f3f4f6"
        val accentBorder = if (colorTheme) "#312e81" else "#000000"

        val reportTitle = when (templateType) {
            "monthly" -> {
                val mLabel = when (monthKey) {
                    "m1" -> "الفصل الأول - الشهر الأول"
                    "m2" -> "الفصل الأول - الشهر الثاني"
                    "m3" -> "الفصل الثاني - الشهر الأول"
                    "m4" -> "الفصل الثاني - الشهر الثاني"
                    else -> "المتابعة الشهرية"
                }
                "سجل المتابعة اليومي والشهري التفصيلي ($mLabel)"
            }
            "attendance" -> "سجل الحضور والغياب والإنذارات الوزاري الرسمي"
            "blank_roster" -> "سجل الرصد الورقي الفارغ (قائمة الدرجات والمتابعة الصفية)"
            else -> "سجل درجات الطلاب السنوي الشامل"
        }

        val tableHtml = when (templateType) {
            "monthly" -> {
                val mIdx = monthKey.removePrefix("m").toIntOrNull()?.coerceIn(1, 4) ?: 1
                val startField = (mIdx - 1) * 3 + 1
                val rows = filtered.mapIndexed { idx, s ->
                    val g = state.grades["${s.id}_$sub"] ?: SubjectGrades()
                    val vDaily = g.getField("v$startField").toIntOrNull()
                    val vPractical = g.getField("v${startField + 1}").toIntOrNull()
                    val vWritten = g.getField("v${startField + 2}").toIntOrNull()
                    val hasAny = vDaily != null || vPractical != null || vWritten != null
                    val sum = (vDaily ?: 0) + (vPractical ?: 0) + (vWritten ?: 0)
                    val hwMissing = state.missingHomework["${s.id}_$sub"] ?: 0
                    val absCount = state.attendance.count { (k, v) -> k.startsWith("${s.id}_") && v == "absent" }
                    val evalLabel = if (!hasAny) "-" else when {
                        sum >= (maxGrade * 0.9).toInt() -> "امتياز 🌟"
                        sum >= (maxGrade * 0.8).toInt() -> "جيد جداً"
                        sum >= (maxGrade * 0.7).toInt() -> "جيد"
                        sum >= failThresh -> "متوسط / مقبول"
                        else -> "ضعيف (راسب)"
                    }
                    val sumColor = if (!colorTheme || !hasAny) "#000" else if (sum >= failThresh) "#047857" else "#dc2626"
                    """
                    <tr>
                        <td>${idx + 1}</td>
                        <td style="text-align:right;padding-right:8px;">${escapeHtml(s.name)}</td>
                        <td>${escapeHtml(s.section)}</td>
                        <td>${vDaily ?: "-"}</td>
                        <td>${vPractical ?: "-"}</td>
                        <td>${vWritten ?: "-"}</td>
                        <td style="color:$sumColor;font-size:12px;">${if (hasAny) sum else "-"}</td>
                        <td style="color:${if (colorTheme && hwMissing > 0) "#d97706" else "#000"};">${if (hwMissing > 0) "$hwMissing نقص" else "ملتزم"}</td>
                        <td style="color:${if (colorTheme && absCount > 0) "#dc2626" else "#000"};">${if (absCount > 0) "$absCount يوم" else "0"}</td>
                        <td>$evalLabel</td>
                    </tr>
                    """.trimIndent()
                }.joinToString("\n")

                """
                <table>
                    <thead>
                        <tr>
                            <th>ت</th>
                            <th>اسم الطالب الرباعي</th>
                            <th>الشعبة</th>
                            <th>اليومي</th>
                            <th>العملي / النشاط</th>
                            <th>الامتحان التحريري</th>
                            <th>مجموع الشهر (من $maxGrade)</th>
                            <th>الواجبات البيتية</th>
                            <th>أيام الغياب</th>
                            <th>التقدير والمستوى</th>
                        </tr>
                    </thead>
                    <tbody>$rows</tbody>
                </table>
                """.trimIndent()
            }
            "attendance" -> {
                val rows = filtered.mapIndexed { idx, s ->
                    val records = state.attendance.filterKeys { it.startsWith("${s.id}_") }
                    val isIntisabStd = WaadCalculator.isStudentIntisab(s.id, state.studentTags)
                    val todayStatus = WaadCalculator.resolveEffectiveAttendanceStatus(
                        studentId = s.id,
                        dateStr = state.attendanceDate,
                        attendance = state.attendance,
                        studentTags = state.studentTags
                    )
                    val isIntisabEffective = isIntisabStd || todayStatus == "intisab"
                    val absCount = if (isIntisabEffective) 0 else records.values.count { it == "absent" }
                    val excCount = records.values.count { it == "excused" }
                    val presCount = records.values.count { it == "present" }
                    val todayLabel = when (todayStatus) {
                        "absent" -> "غائب ❌"
                        "excused" -> "إجازة 🕒"
                        "intisab" -> "انتساب 🏠"
                        else -> if (isIntisabStd) "حاضر بالامتحان ✅" else "حاضر ✅"
                    }
                    val totalTracked = (presCount + absCount + excCount).coerceAtLeast(1)
                    val rateStr = if (isIntisabEffective) "معفي (انتساب)" else "${((presCount + excCount) * 100) / totalTracked}%"
                    val warnBadge = if (isIntisabEffective) {
                        "طالب انتساب (امتحانات فقط) 🏠"
                    } else {
                        when {
                            absCount >= 21 -> "فصل بالغياب (تجاوز 21 يوم) ⛔"
                            absCount >= 7 -> "إنذار نهائي ($absCount غياب) ⛔"
                            absCount >= 5 -> "إنذار ثاني ($absCount غياب) 🚨"
                            absCount >= 3 -> "إنذار أول ($absCount غياب) ⚠️"
                            absCount >= 1 -> "تنبيه أولي ($absCount غياب)"
                            else -> "ملتزم تماماً ✅"
                        }
                    }
                    val warnColor = if (!colorTheme) "#000" else when {
                        isIntisabEffective -> "#6d28d9"
                        absCount >= 5 -> "#dc2626"
                        absCount >= 3 -> "#d97706"
                        else -> "#047857"
                    }
                    """
                    <tr>
                        <td>${idx + 1}</td>
                        <td style="text-align:right;padding-right:8px;">${escapeHtml(s.name)}${if (isIntisabEffective) " (انتساب)" else ""}</td>
                        <td>${escapeHtml(s.section)}</td>
                        <td>$todayLabel</td>
                        <td>${if (isIntisabEffective && presCount == 0) "-" else presCount.toString()}</td>
                        <td style="color:${if (colorTheme && absCount > 0) "#dc2626" else "#000"};">${if (isIntisabEffective) "معفي" else absCount.toString()}</td>
                        <td>${if (isIntisabEffective && excCount == 0) "-" else excCount.toString()}</td>
                        <td>$rateStr</td>
                        <td style="color:$warnColor;">$warnBadge</td>
                        <td>${escapeHtml(s.phone.ifBlank { "-" })}</td>
                    </tr>
                    """.trimIndent()
                }.joinToString("\n")

                """
                <table>
                    <thead>
                        <tr>
                            <th>ت</th>
                            <th>اسم الطالب</th>
                            <th>الشعبة</th>
                            <th>موقف يوم (${escapeHtml(state.attendanceDate)})</th>
                            <th>أيام الحضور</th>
                            <th>أيام الغياب</th>
                            <th>الإجازات</th>
                            <th>نسبة الالتزام</th>
                            <th>الموقف القانوني والإنذارات</th>
                            <th>هاتف ولي الأمر</th>
                        </tr>
                    </thead>
                    <tbody>$rows</tbody>
                </table>
                """.trimIndent()
            }
            "blank_roster" -> {
                val cols = blankColumnsCount.coerceIn(4, 14)
                val blankHeaders = (1..cols).joinToString("") { "<th>حقل $it</th>" }
                val blankCells = (1..cols).joinToString("") { "<td style=\"height:22px;\"></td>" }
                val rows = filtered.mapIndexed { idx, s ->
                    """
                    <tr>
                        <td>${idx + 1}</td>
                        <td style="text-align:right;padding-right:8px;">${escapeHtml(s.name)}</td>
                        <td>${escapeHtml(s.section)}</td>
                        $blankCells
                        <td></td>
                    </tr>
                    """.trimIndent()
                }.joinToString("\n")

                """
                <table>
                    <thead>
                        <tr>
                            <th style="width:32px;">ت</th>
                            <th style="width:210px;">اسم الطالب</th>
                            <th style="width:45px;">الشعبة</th>
                            $blankHeaders
                            <th style="width:110px;">الملاحظات</th>
                        </tr>
                    </thead>
                    <tbody>$rows</tbody>
                </table>
                """.trimIndent()
            }
            else -> {
                val rowsHtml = filtered.mapIndexed { idx, s ->
                    val res = calculateStudentResult(s.id, sub, state)
                    val effectiveSaei = min(maxGrade, res.saei + res.qarar)
                    val finalScore = res.secondRoundResult ?: if (res.hasFinalResult) res.finalResult else effectiveSaei
                    val status = if (res.hasSaei) (if (finalScore >= failThresh) "ناجح" else "راسب") else "-"
                    val statusColor = if (!colorTheme || !res.hasSaei) "#000" else if (finalScore >= failThresh) "#047857" else "#dc2626"
                    """
                    <tr>
                        <td>${idx + 1}</td>
                        <td style="text-align:right;padding-right:6px;">${escapeHtml(s.name)} (${escapeHtml(s.section)})</td>
                        <td>${if (res.hasM1) res.m1 else "-"}</td>
                        <td>${if (res.hasM2) res.m2 else "-"}</td>
                        <td style="background:${if (colorTheme) "#f8fafc" else "transparent"};">${if (res.hasAvg1) res.avg1 else "-"}</td>
                        <td>${if (res.hasMid) res.mid else "-"}</td>
                        <td>${if (res.hasM3) res.m3 else "-"}</td>
                        <td>${if (res.hasM4) res.m4 else "-"}</td>
                        <td style="background:${if (colorTheme) "#f8fafc" else "transparent"};">${if (res.hasAvg2) res.avg2 else "-"}</td>
                        <td style="font-weight:900;">${if (res.isExempt) "معفي ✨" else if (res.hasSaei) effectiveSaei else "-"}</td>
                        <td>${if (res.hasFinal) res.finalExam else "-"}</td>
                        <td>${if (res.qarar > 0) "+${res.qarar}" else "-"}</td>
                        <td style="background:${if (colorTheme) "#fffbeb" else "transparent"};">${if (res.hasSecondRound) res.secondRoundExam else "-"}</td>
                        <td style="color:$statusColor;font-weight:900;">$status</td>
                    </tr>
                    """.trimIndent()
                }.joinToString("\n")

                """
                <table>
                    <thead>
                        <tr>
                            <th rowspan="2">ت</th>
                            <th rowspan="2">اسم الطالب</th>
                            <th colspan="3">الفصل الأول</th>
                            <th rowspan="2">نصف السنة</th>
                            <th colspan="3">الفصل الثاني</th>
                            <th rowspan="2">السعي السنوي</th>
                            <th rowspan="2">النهائي</th>
                            <th rowspan="2">قرار</th>
                            <th rowspan="2">دور ثان</th>
                            <th rowspan="2">النتيجة</th>
                        </tr>
                        <tr>
                            <th>ش1</th><th>ش2</th><th>معدل</th>
                            <th>ش1</th><th>ش2</th><th>معدل</th>
                        </tr>
                    </thead>
                    <tbody>
                        $rowsHtml
                    </tbody>
                </table>
                """.trimIndent()
            }
        }

        val footerHtml = if (showSignatures) {
            val supervisorBlock = if (showSupervisorSign) {
                "<div>توقيع المشرف الاختصاصي<br><br>.................................</div>"
            } else ""
            """
            <div class="footer">
                <table style="width: 44%; margin-bottom: 0;">
                    <thead>
                        <tr><th>العدد الكلي</th><th>المشتركون</th><th>الناجحون</th><th>المكملون / الراسبون</th><th>نسبة النجاح</th></tr>
                    </thead>
                    <tbody>
                        <tr><td>${stats.total}</td><td>${stats.activeCount}</td><td>${stats.passed}</td><td>${stats.failed}</td><td>${stats.rate}%</td></tr>
                    </tbody>
                </table>
                <div>توقيع مدرس المادة<br><br>${escapeHtml(state.settings.teacherName)}</div>
                $supervisorBlock
                <div>مصادقة وتوقيع مدير المدرسة<br><br>.................................</div>
            </div>
            """.trimIndent()
        } else ""

        return """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="UTF-8">
            <style>
                @page { size: $pageOrientation; margin: 0.7cm; }
                body { font-family: sans-serif; direction: rtl; color: #000; margin: 0; padding: 10px; }
                .header { display: flex; justify-content: space-between; border-bottom: 3px double $accentBorder; padding-bottom: 10px; margin-bottom: 12px; font-weight: bold; font-size: 13px; }
                table { width: 100%; border-collapse: collapse; border: 2px solid $accentBorder; margin-bottom: 14px; }
                th, td { border: 1px solid #000; padding: 5px 4px; font-size: 11px; text-align: center; font-weight: bold; }
                th { background-color: $thBg; color: #000; }
                tr:nth-child(even) td { background-color: ${if (colorTheme) "#f8fafc" else "transparent"}; }
                .footer { display: flex; justify-content: space-between; align-items: center; margin-top: 14px; font-weight: bold; font-size: 12px; }
            </style>
        </head>
        <body>
            <div class="header">
                <div><div>${escapeHtml(state.settings.ministryName)}</div><div>${escapeHtml(state.settings.schoolName)}</div></div>
                <div style="text-align:center;"><div style="font-size:16px;text-decoration:underline;">$reportTitle</div><div>العام الدراسي: ${escapeHtml(state.settings.academicYear)}</div></div>
                <div style="text-align:left;"><div>المرحلة: ${escapeHtml(state.currentDept.name)} / ${escapeHtml(state.currentStage.name)}</div><div>الشعبة: ${escapeHtml(state.globalSec.ifBlank { "الكل" })} | المادة: $safeSub</div></div>
            </div>
            $tableHtml
            $footerHtml
        </body>
        </html>
        """.trimIndent()
    }

    fun buildPrintSummaryShareText(templateType: String, monthKey: String): String {
        val state = _uiState.value
        val sub = state.globalSub.ifBlank { state.currentStage.subjects.firstOrNull() ?: "المادة" }
        val filtered = getFilteredStudentsList(state)
        val stats = computeStats(state)
        val secLabel = state.globalSec.ifBlank { "كل الشعب" }
        val templateTitle = when (templateType) {
            "monthly" -> "سجل المتابعة الشهري ($monthKey)"
            "attendance" -> "سجل الحضور والغياب الرسمي"
            "blank_roster" -> "قائمة أسماء الطلاب الرسمية"
            else -> "السجل السنوي الشامل للدرجات"
        }

        return buildString {
            append("📑 *$templateTitle*\n")
            append("🏫 *المدرسة:* ${state.settings.schoolName}\n")
            append("📚 *الصف:* ${state.currentDept.name} - ${state.currentStage.name} ($secLabel)\n")
            append("📖 *المادة:* $sub | *العام:* ${state.settings.academicYear}\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("👥 *العدد الكلي:* ${stats.total} | ✅ *الناجحون:* ${stats.passed} | ⚠️ *المكملون/الراسبون:* ${stats.failed} | 📈 *النسبة:* ${stats.rate}%\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            filtered.take(40).forEachIndexed { idx, s ->
                when (templateType) {
                    "attendance" -> {
                        val isIntisab = WaadCalculator.isStudentIntisab(s.id, state.studentTags) ||
                            state.attendance["${s.id}_${state.attendanceDate}"] == "intisab"
                        if (isIntisab) {
                            append("${idx + 1}. ${s.name} (${s.section}) ⬅️ طالب انتساب 🏠 (معفي من الدروس اليومية — امتحانات فقط)\n")
                        } else {
                            val abs = state.attendance.count { (k, v) -> k.startsWith("${s.id}_") && v == "absent" }
                            val exc = state.attendance.count { (k, v) -> k.startsWith("${s.id}_") && v == "excused" }
                            append("${idx + 1}. ${s.name} (${s.section}) ⬅️ غياب: $abs | إجازة: $exc\n")
                        }
                    }
                    "monthly" -> {
                        val mIdx = monthKey.removePrefix("m").toIntOrNull()?.coerceIn(1, 4) ?: 1
                        val start = (mIdx - 1) * 3 + 1
                        val g = state.grades["${s.id}_$sub"] ?: SubjectGrades()
                        val d = g.getField("v$start").ifBlank { "-" }
                        val p = g.getField("v${start + 1}").ifBlank { "-" }
                        val w = g.getField("v${start + 2}").ifBlank { "-" }
                        val sum = (d.toIntOrNull() ?: 0) + (p.toIntOrNull() ?: 0) + (w.toIntOrNull() ?: 0)
                        append("${idx + 1}. ${s.name} (${s.section}) ⬅️ يومي:$d | عملي:$p | تحريري:$w | المجموع: $sum\n")
                    }
                    else -> {
                        val res = calculateStudentResult(s.id, sub, state)
                        val saei = if (res.hasSaei) min(state.maxAllowedGrade, res.saei + res.qarar).toString() else "-"
                        val fin = res.secondRoundResult ?: if (res.hasFinalResult) res.finalResult else "-"
                        append("${idx + 1}. ${s.name} (${s.section}) ⬅️ السعي: $saei | النهائي: $fin\n")
                    }
                }
            }
            if (state.settings.teacherName.isNotBlank()) {
                append("━━━━━━━━━━━━━━━━━━━━\n")
                append("👨‍🏫 *أستاذ المادة:* ${state.settings.teacherName}")
            }
        }
    }

    // =====================================================
    // Google Drive App Update Management (التحديث التلقائي من كوكل درايف)
    // =====================================================
    fun updateDriveUrl(rawInput: String) {
        val current = _uiState.value.driveUpdateConfig
        // Support pasting either a Google Drive URL or a full JSON manifest
        val parsedJson = GoogleDriveUpdateManager.parseUpdateManifestJson(rawInput, current)
        val updated = (parsedJson ?: current.copy(driveUrl = rawInput)).copy(
            showPromptOnStartup = true,
            downloadStatusMessage = "",
            downloadProgressPercent = 0
        )
        repository.saveDriveUpdateConfig(updated)
        _uiState.update { it.copy(driveUpdateConfig = updated) }
    }

    fun updateDriveTargetVersion(versionName: String) {
        val clean = versionName.removePrefix("v").removePrefix("V").trim()
        val updated = _uiState.value.driveUpdateConfig.copy(targetVersionName = clean)
        repository.saveDriveUpdateConfig(updated)
        _uiState.update { it.copy(driveUpdateConfig = updated) }
    }

    fun updateDriveReleaseNotes(notes: String) {
        val updated = _uiState.value.driveUpdateConfig.copy(releaseNotes = notes)
        repository.saveDriveUpdateConfig(updated)
        _uiState.update { it.copy(driveUpdateConfig = updated) }
    }

    fun toggleDriveShowOnStartup(enabled: Boolean) {
        val updated = _uiState.value.driveUpdateConfig.copy(showPromptOnStartup = enabled)
        repository.saveDriveUpdateConfig(updated)
        _uiState.update { it.copy(driveUpdateConfig = updated) }
        showToast(
            if (enabled) "تم تفعيل فحص وتحديث التطبيق تلقائياً عند الفتح ✅"
            else "تم إيقاف الفحص التلقائي للتحديث عند الفتح"
        )
    }

    fun setDriveUpdateDialogOpen(open: Boolean) {
        val current = _uiState.value.driveUpdateConfig
        val updated = if (!open) {
            current.copy(
                isUpdateDialogOpen = false,
                showPromptOnStartup = false,
                acknowledgedBroadcastId = current.lastBroadcastId.ifBlank { current.acknowledgedBroadcastId }
            )
        } else {
            current.copy(isUpdateDialogOpen = true)
        }
        repository.saveDriveUpdateConfig(updated, commitImmediately = !open)
        _uiState.update {
            it.copy(driveUpdateConfig = updated)
        }
    }

    fun startAutomaticDriveApkUpdate(context: Context, openDialog: Boolean = true) {
        val current = _uiState.value.driveUpdateConfig
        val rawUrl = current.driveUrl.trim()
        if (rawUrl.isBlank()) {
            _uiState.update {
                it.copy(driveUpdateConfig = current.copy(isUpdateDialogOpen = true))
            }
            return
        }
        if (current.isAutoDownloading) return

        val fileIdOrUrl = GoogleDriveUpdateManager.extractDriveFileId(rawUrl) ?: rawUrl
        val normalizedTargetVer = GoogleDriveUpdateManager.normalizeVersionName(current.targetVersionName)

        // Immediately persist that the user clicked "موافق" for this broadcast/version/file
        // so that the update prompt never repeats even after the APK is installed and the app restarts.
        val savingConfig = current.copy(
            showPromptOnStartup = false,
            isUpdateDialogOpen = openDialog,
            isAutoDownloading = true,
            downloadProgressPercent = 2,
            downloadedSizeText = "",
            downloadStatusMessage = "جاري تنزيل التحديث...",
            acknowledgedBroadcastId = current.lastBroadcastId.ifBlank { current.acknowledgedBroadcastId },
            acknowledgedDriveFileId = fileIdOrUrl,
            acknowledgedVersionName = normalizedTargetVer.ifBlank { current.acknowledgedVersionName },
            acknowledgedVersionCode = maxOf(current.targetVersionCode, current.acknowledgedVersionCode)
        )
        repository.saveDriveUpdateConfig(savingConfig, commitImmediately = true)
        _uiState.update { it.copy(driveUpdateConfig = savingConfig) }

        viewModelScope.launch {
            val outcome = GoogleDriveUpdateManager.downloadAndInstallApkAutomatically(
                context = context,
                rawUrl = rawUrl,
                onProgress = { percent, sizeLabel, statusMsg ->
                    _uiState.update { state ->
                        state.copy(
                            driveUpdateConfig = state.driveUpdateConfig.copy(
                                isAutoDownloading = percent < 100,
                                downloadProgressPercent = percent,
                                downloadedSizeText = sizeLabel,
                                downloadStatusMessage = statusMsg
                            )
                        )
                    }
                }
            )

            val updatedAfterDownload = _uiState.value.driveUpdateConfig.copy(
                isAutoDownloading = false,
                downloadProgressPercent = if (outcome.success) 100 else 0,
                downloadStatusMessage = "",
                downloadedApkReadyPath = outcome.apkFile?.absolutePath ?: "",
                isUpdateDialogOpen = false,
                showPromptOnStartup = false,
                acknowledgedBroadcastId = savingConfig.acknowledgedBroadcastId,
                acknowledgedDriveFileId = savingConfig.acknowledgedDriveFileId,
                acknowledgedVersionName = savingConfig.acknowledgedVersionName,
                acknowledgedVersionCode = savingConfig.acknowledgedVersionCode
            )
            repository.saveDriveUpdateConfig(updatedAfterDownload, commitImmediately = true)
            _uiState.update { state ->
                state.copy(driveUpdateConfig = updatedAfterDownload)
            }
            showToast(outcome.message)
        }
    }

    fun installReadyDownloadedApk(context: Context) {
        val path = _uiState.value.driveUpdateConfig.downloadedApkReadyPath
        if (path.isBlank()) {
            startAutomaticDriveApkUpdate(context, openDialog = true)
            return
        }
        val file = java.io.File(path)
        val launched = GoogleDriveUpdateManager.installDownloadedApkFile(context, file)
        if (launched) {
            showToast("تم فتح مثبت التحديث التلقائي 📲")
        } else {
            startAutomaticDriveApkUpdate(context, openDialog = true)
        }
    }

    private var broadcastPollingJob: Job? = null

    /**
     * Automatically listens for remote update broadcasts sent by the developer from the
     * standalone admin tool (`ملف_تحديث_التطبيق_المستقل.html`). When a new broadcast is detected
     * with a version strictly newer than the installed app, the update dialog opens once.
     */
    fun startRemoteUpdateBroadcastListener(context: Context) {
        if (broadcastPollingJob?.isActive == true) return
        val appContext = context.applicationContext
        broadcastPollingJob = viewModelScope.launch {
            while (true) {
                checkAndApplyRemoteBroadcast(appContext)
                delay(25_000L)
            }
        }
    }

    private suspend fun checkAndApplyRemoteBroadcast(context: Context) {
        val current = _uiState.value.driveUpdateConfig
        if (current.isAutoDownloading) return

        val broadcast = GoogleDriveUpdateManager.fetchLatestRemoteBroadcast() ?: return
        if (!broadcast.active) {
            // Developer cancelled/stopped the active update broadcast from the standalone tool
            if (current.lastBroadcastId != broadcast.broadcastId || current.isUpdateDialogOpen) {
                val cleared = current.copy(
                    showPromptOnStartup = false,
                    isMandatory = false,
                    isUpdateDialogOpen = false,
                    lastBroadcastId = broadcast.broadcastId,
                    acknowledgedBroadcastId = broadcast.broadcastId
                )
                repository.saveDriveUpdateConfig(cleared, commitImmediately = true)
                _uiState.update { it.copy(driveUpdateConfig = cleared) }
            }
            return
        }

        if (broadcast.driveUrl.isBlank()) return

        // Verify current installed version vs remote broadcast version and check if already acknowledged
        val alreadyHandled = GoogleDriveUpdateManager.isUpdateAlreadyHandled(
            context = context,
            config = current,
            broadcastId = broadcast.broadcastId,
            driveUrl = broadcast.driveUrl,
            targetVersionName = broadcast.versionName,
            targetVersionCode = broadcast.versionCode
        )

        if (alreadyHandled) {
            if (current.isUpdateDialogOpen || current.showPromptOnStartup || current.lastBroadcastId != broadcast.broadcastId) {
                val synced = current.copy(
                    showPromptOnStartup = false,
                    isUpdateDialogOpen = false,
                    lastBroadcastId = broadcast.broadcastId.ifBlank { current.lastBroadcastId }
                )
                repository.saveDriveUpdateConfig(synced)
                _uiState.update { it.copy(driveUpdateConfig = synced) }
            }
            return
        }

        val isNewBroadcast = broadcast.broadcastId.isNotBlank() && broadcast.broadcastId != current.lastBroadcastId
        if (isNewBroadcast) {
            val updatedConfig = current.copy(
                driveUrl = broadcast.driveUrl,
                targetVersionName = broadcast.versionName,
                targetVersionCode = broadcast.versionCode,
                releaseNotes = broadcast.releaseNotes,
                showPromptOnStartup = true,
                isMandatory = broadcast.isMandatory,
                isUpdateDialogOpen = true,
                isAutoDownloading = false,
                downloadProgressPercent = 0,
                downloadStatusMessage = "",
                downloadedApkReadyPath = "",
                lastBroadcastId = broadcast.broadcastId
            )
            repository.saveDriveUpdateConfig(updatedConfig, commitImmediately = true)
            _uiState.update { it.copy(driveUpdateConfig = updatedConfig) }
        }
    }

    fun checkRemoteDriveUpdateNow() {
        val current = _uiState.value.driveUpdateConfig
        if (current.driveUrl.isBlank()) {
            _uiState.update {
                it.copy(driveUpdateConfig = current.copy(isUpdateDialogOpen = true))
            }
            return
        }
        _uiState.update {
            it.copy(driveUpdateConfig = current.copy(isCheckingRemote = true))
        }
        viewModelScope.launch {
            val resolved = GoogleDriveUpdateManager.resolveOrCheckDriveUpdate(
                rawInput = current.driveUrl,
                currentConfig = current
            )
            repository.saveDriveUpdateConfig(resolved)
            _uiState.update {
                it.copy(driveUpdateConfig = resolved.copy(isUpdateDialogOpen = true, isCheckingRemote = false))
            }
        }
    }

    fun buildDriveUpdateManifestJsonString(): String {
        return GoogleDriveUpdateManager.buildUpdateManifestJson(_uiState.value.driveUpdateConfig)
    }

    fun buildDriveUpdateShareMessage(): String {
        val cfg = _uiState.value.driveUpdateConfig
        val ver = cfg.targetVersionName.ifBlank { GoogleDriveUpdateManager.CURRENT_APP_VERSION_NAME }
        val viewUrl = GoogleDriveUpdateManager.toDriveViewUrl(cfg.driveUrl)
        val directUrl = GoogleDriveUpdateManager.toDirectDownloadUrl(cfg.driveUrl)
        return buildString {
            append("📲 *تحديث جديد لتطبيق بوصلة الأستاذ (الإصدار v$ver)*\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("✨ *ما الجديد في هذا التحديث؟*\n")
            append("${cfg.releaseNotes.ifBlank { "تحسينات شاملة في سجل الدرجات والحضور والغياب." }}\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            if (directUrl.isNotBlank()) {
                append("⬇️ *رابط التحميل المباشر من كوكل درايف (APK):*\n$directUrl\n\n")
            }
            if (viewUrl.isNotBlank() && viewUrl != directUrl) {
                append("☁️ *رابط صفحة الملف على كوكل درايف:*\n$viewUrl\n")
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = WaadDatabase.getInstance(context)
                    val prefs = context.applicationContext.getSharedPreferences("waad_prefs_v180", Context.MODE_PRIVATE)
                    val repo = WaadRepository(db.waadDao(), prefs)
                    return WaadViewModel(repo) as T
                }
            }
    }
}
