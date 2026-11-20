package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.CalculatedResult
import com.example.model.StatsSummary
import com.example.model.Student
import com.example.model.SubjectGrades
import com.example.ui.WaadUiState
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Purple600
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlin.math.max
import kotlin.math.min

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.example.model.WaadCalculator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeedGradingScreen(
    state: WaadUiState,
    speedList: List<Student>,
    instantMonthTotal: Int,
    onCloseSpeedMode: () -> Unit,
    onUpdateSpeedConfig: (Int?, String?, Int?) -> Unit,
    onGradeChange: (String, String, String) -> Boolean,
    onOpenBonus: (Student) -> Unit,
    onVoiceSpoken: (String) -> Unit,
    onVoiceCandidatesSpoken: (List<String>) -> Unit = { list ->
        list.firstOrNull()?.let(onVoiceSpoken)
    },
    onAdvanceField: () -> Unit = {},
    onFinishGrading: () -> Unit
) {
    BackHandler { onCloseSpeedMode() }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        keyboardController?.hide()
    }

    var showVoiceFallbackDialog by remember { mutableStateOf(false) }
    var voiceInputText by remember { mutableStateOf("") }
    var isInAppListening by remember { mutableStateOf(false) }
    var liveSpeechHint by remember { mutableStateOf("") }

    val activeIdx = state.speedConfig.activeIndex.coerceIn(0, max(0, speedList.size - 1))
    val currentStudent = speedList.getOrNull(activeIdx)
    val activeTab = state.speedConfig.activeTab
    val isMonthlyTab = activeTab != "mid" && activeTab != "final" && activeTab != "secondRound"
    val activeSubFieldIdx = state.speedConfig.activeSubFieldIdx.coerceIn(0, 2)
    val subFieldLabels = listOf("يومي", "عملي", "تحريري")
    val activeFieldTitle = if (isMonthlyTab) {
        subFieldLabels[activeSubFieldIdx]
    } else {
        when (activeTab) {
            "mid" -> "نصف السنة"
            "final" -> "النهائي"
            else -> "الدور الثاني"
        }
    }

    // Track whether the next digit pressed on the built-in keypad should replace or append
    var isFreshFieldSelection by remember(activeIdx, activeTab, activeSubFieldIdx) {
        mutableStateOf(true)
    }
    var autoAdvanceEnabled by remember { mutableStateOf(true) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isInAppListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                onVoiceCandidatesSpoken(matches)
            }
        }
    }

    fun buildSpeechIntent(targetLabel: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-IQ")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "🎙️ انطق درجة ($targetLabel) بصوت واضح (أو انطق 3 درجات معاً: يومي عملي تحريري)"
            )
        }
    }

    val directSpeechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                SpeechRecognizer.createSpeechRecognizer(context)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    DisposableEffect(directSpeechRecognizer) {
        onDispose {
            try {
                directSpeechRecognizer?.destroy()
            } catch (_: Exception) {
            }
        }
    }

    fun launchSpeechRecognizer() {
        val intent = buildSpeechIntent(activeFieldTitle)
        if (directSpeechRecognizer != null) {
            try {
                directSpeechRecognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isInAppListening = true
                        liveSpeechHint = "🎙️ تفضل بالنطق الآن لدرجة ($activeFieldTitle)..."
                    }

                    override fun onBeginningOfSpeech() {
                        liveSpeechHint = "🔊 جاري الاستماع..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isInAppListening = false
                        liveSpeechHint = "⏳ جاري تحليل الرقم المنطوق..."
                    }

                    override fun onError(error: Int) {
                        isInAppListening = false
                        liveSpeechHint = ""
                        try {
                            speechLauncher.launch(intent)
                        } catch (_: Exception) {
                            showVoiceFallbackDialog = true
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isInAppListening = false
                        liveSpeechHint = ""
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onVoiceCandidatesSpoken(matches)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                        if (!partial.isNullOrBlank()) {
                            liveSpeechHint = "سمعت: \"$partial\""
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                directSpeechRecognizer.startListening(intent)
                return
            } catch (_: Exception) {
                isInAppListening = false
            }
        }

        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            showVoiceFallbackDialog = true
        }
    }

    val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchSpeechRecognizer()
        } else {
            showVoiceFallbackDialog = true
        }
    }

    val bgColor = if (state.darkMode) Slate900 else Slate100
    val cardBg = if (state.darkMode) Slate800 else Color.White
    val cardBorder = if (state.darkMode) Slate700 else Slate200

    // Distinct semantic colors for the 3 monthly sub-fields:
    // 0 -> يومي (Sky Blue), 1 -> عملي (Violet Purple), 2 -> تحريري (Amber Gold)
    val skyAccent = if (state.darkMode) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val violetAccent = if (state.darkMode) Color(0xFFA78BFA) else Color(0xFF7C3AED)
    val amberAccent = if (state.darkMode) Amber400 else Amber500
    val subFieldAccents = listOf(skyAccent, violetAccent, amberAccent)
    val activeFieldAccent = if (isMonthlyTab) subFieldAccents[activeSubFieldIdx] else amberAccent

    // Helper to compute any student's score on a given tab
    fun getStudentTabScore(studentId: String, tabId: String): Int? {
        val g = state.grades["${studentId}_${state.globalSub}"] ?: return null
        return if (tabId.startsWith("m")) {
            val m = tabId.removePrefix("m").toIntOrNull() ?: 1
            val s = (m - 1) * 3 + 1
            val a = g.getField("v$s")
            val b = g.getField("v${s + 1}")
            val c = g.getField("v${s + 2}")
            if (a.isBlank() && b.isBlank() && c.isBlank()) null
            else (a.toIntOrNull() ?: 0) + (b.toIntOrNull() ?: 0) + (c.toIntOrNull() ?: 0)
        } else {
            val v = g.getField(tabId)
            if (v.isBlank()) null else v.toIntOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Clean Top Bar: Back Button | Subject Name | Student Counter
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = BorderStroke(1.5.dp, cardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (state.darkMode) Slate700 else Slate100)
                        .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                        .clickable { onCloseSpeedMode() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("speed_mode_back_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (state.darkMode) Color.White else Slate800,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "رجوع",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = if (state.darkMode) Color.White else Slate800
                        )
                    }
                }

                Text(
                    text = state.globalSub,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = if (state.darkMode) Color.White else Slate900
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Indigo600.copy(alpha = 0.14f))
                        .border(1.dp, Indigo500, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (speedList.isNotEmpty()) "الطالب ${activeIdx + 1} من ${speedList.size}" else "0 / 0",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = if (state.darkMode) Indigo400 else Indigo600
                    )
                }
            }
        }

        if (currentStudent != null) {
            val stdGrades = state.grades["${currentStudent.id}_${state.globalSub}"] ?: SubjectGrades()
            val calcRes = remember(stdGrades, state.maxAllowedGrade) {
                WaadCalculator.calculateResults(stdGrades, state.maxAllowedGrade)
            }
            val mIdx = activeTab.removePrefix("m").toIntOrNull() ?: 1
            val startFieldNum = (mIdx - 1) * 3 + 1
            val v1Str = stdGrades.getField("v$startFieldNum")
            val v2Str = stdGrades.getField("v${startFieldNum + 1}")
            val v3Str = stdGrades.getField("v${startFieldNum + 2}")
            val hasAnyInActiveMonth = v1Str.isNotBlank() || v2Str.isNotBlank() || v3Str.isNotBlank()
            val liveMonthSum = (v1Str.toIntOrNull() ?: 0) +
                (v2Str.toIntOrNull() ?: 0) +
                (v3Str.toIntOrNull() ?: 0)
            val displayTabTotal = if (isMonthlyTab) {
                liveMonthSum
            } else {
                stdGrades.getField(activeTab).toIntOrNull() ?: 0
            }
            val isPassingTab = displayTabTotal >= state.failThreshold

            val currentFieldName = if (isMonthlyTab) {
                "v${startFieldNum + activeSubFieldIdx}"
            } else {
                activeTab
            }
            val currentFieldValue = stdGrades.getField(currentFieldName)

            fun handleKeypadDigit(digitStr: String) {
                val base = if (isFreshFieldSelection) "" else currentFieldValue
                val candidate = if (base == "0") digitStr else (base + digitStr)
                val num = candidate.toIntOrNull() ?: return
                val accepted = onGradeChange(currentStudent.id, currentFieldName, num.toString())
                if (accepted) {
                    isFreshFieldSelection = false
                    if (autoAdvanceEnabled) {
                        val shouldAutoAdvance = if (state.maxAllowedGrade == 10) {
                            num >= 2 || num == 0 || candidate.length >= 2
                        } else {
                            candidate.length >= 2 && (num * 10 > state.maxAllowedGrade)
                        }
                        if (shouldAutoAdvance) {
                            onAdvanceField()
                        }
                    }
                }
            }

            fun handleQuickPresetNumber(presetNum: Int) {
                val accepted = onGradeChange(currentStudent.id, currentFieldName, presetNum.toString())
                if (accepted) {
                    isFreshFieldSelection = true
                    if (autoAdvanceEnabled) {
                        onAdvanceField()
                    }
                }
            }

            fun handleBackspace() {
                if (currentFieldValue.isNotEmpty()) {
                    val shortened = currentFieldValue.dropLast(1)
                    onGradeChange(currentStudent.id, currentFieldName, shortened)
                    isFreshFieldSelection = false
                }
            }

            fun handleClearField() {
                onGradeChange(currentStudent.id, currentFieldName, "")
                isFreshFieldSelection = true
            }

            // 2. Student Identity Card + Clearly Bordered 3-Cell Summary Table
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.5.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Indigo600)
                                    .border(1.dp, Indigo400, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${activeIdx + 1}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = currentStudent.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    ),
                                    color = if (state.darkMode) Color.White else Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "شعبة (${currentStudent.section})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Indigo500
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Amber500.copy(alpha = 0.15f))
                                    .border(1.2.dp, Amber500, RoundedCornerShape(10.dp))
                                    .clickable { onOpenBonus(currentStudent) }
                                    .testTag("speed_bonus_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "درجة تشجيعية",
                                    tint = Amber500,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isInAppListening) Rose600 else Indigo600
                                    )
                                    .border(
                                        1.2.dp,
                                        if (isInAppListening) Rose500 else Indigo400,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        if (isInAppListening) {
                                            try {
                                                directSpeechRecognizer?.stopListening()
                                            } catch (_: Exception) {
                                            }
                                            isInAppListening = false
                                        } else {
                                            val hasPerm = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasPerm) {
                                                launchSpeechRecognizer()
                                            } else {
                                                audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                    .testTag("speed_voice_mic_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "انطق الدرجة صوتياً",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (isInAppListening) "يستمع..." else "رصد صوتي",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Clearly Bordered 3-Column Summary Table with Vertical Dividers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (state.darkMode) Slate900 else Slate50)
                            .border(1.2.dp, cardBorder, RoundedCornerShape(10.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BorderedSummaryCell(
                            label = "معدل ف1",
                            value = if (calcRes.hasAvg1) "${calcRes.avg1}" else "-",
                            valueColor = skyAccent,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(cardBorder)
                        )
                        BorderedSummaryCell(
                            label = "نصف السنة",
                            value = if (calcRes.hasMid) "${calcRes.mid}" else "-",
                            valueColor = amberAccent,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(cardBorder)
                        )
                        BorderedSummaryCell(
                            label = "معدل ف2",
                            value = if (calcRes.hasAvg2) "${calcRes.avg2}" else "-",
                            valueColor = violetAccent,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(cardBorder)
                        )
                        BorderedSummaryCell(
                            label = "السعي السنوي",
                            value = if (calcRes.hasSaei) "${calcRes.saei}" else "-",
                            valueColor = if (!calcRes.hasSaei) Slate400 else if (calcRes.saei >= state.failThreshold) Emerald500 else Rose500,
                            modifier = Modifier.weight(1.1f)
                        )
                    }

                    if (liveSpeechHint.isNotBlank()) {
                        Text(
                            text = liveSpeechHint,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = Rose500,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 3. Clean Semester & Month Tabs (Text only, crisp borders, no extra number clutter)
            val tabs = listOf(
                "m1" to "ف1 - شهر 1",
                "m2" to "ف1 - شهر 2",
                "mid" to "نصف السنة",
                "m3" to "ف2 - شهر 1",
                "m4" to "ف2 - شهر 2",
                "final" to "النهائي",
                "secondRound" to "الدور الثاني"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEach { (tabId, label) ->
                    val selected = state.speedConfig.activeTab == tabId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) Indigo600
                                else cardBg
                            )
                            .border(
                                if (selected) 1.5.dp else 1.2.dp,
                                if (selected) Indigo400 else cardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onUpdateSpeedConfig(null, tabId, 0) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("speed_tab_$tabId")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = if (selected) Color.White else (if (state.darkMode) Slate300 else Slate700)
                        )
                    }
                }
            }

            // 4. Clearly Framed Grade Fields + Dedicated Month Total Box + Single Clean Keypad
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(18.dp),
                color = cardBg,
                border = BorderStroke(1.5.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isMonthlyTab) {
                        // A. 3 Clearly Bordered Grade Input Boxes (يومي | عملي | تحريري)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                Triple(0, "يومي", skyAccent),
                                Triple(1, "عملي", violetAccent),
                                Triple(2, "تحريري", amberAccent)
                            ).forEach { (i, fieldLabel, fieldAccent) ->
                                val fieldName = "v${startFieldNum + i}"
                                val currentVal = stdGrades.getField(fieldName)
                                val isTarget = activeSubFieldIdx == i

                                InteractiveGradeFieldBox(
                                    stepNum = i + 1,
                                    label = fieldLabel,
                                    value = currentVal,
                                    isSelected = isTarget,
                                    accentColor = fieldAccent,
                                    darkMode = state.darkMode,
                                    cardBg = if (state.darkMode) Slate900 else Slate50,
                                    cardBorder = cardBorder,
                                    onClick = { onUpdateSpeedConfig(null, null, i) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("grade_input_$fieldName")
                                )
                            }
                        }

                        // B. Separate Full-Width Bordered Month Total Box (مفروز بحدود واضحة ومستقلة)
                        val totalBorderColor = if (isPassingTab) Emerald500 else Rose500
                        val totalBgColor = if (isPassingTab) {
                            Emerald500.copy(alpha = if (state.darkMode) 0.16f else 0.1f)
                        } else {
                            Rose500.copy(alpha = if (state.darkMode) 0.16f else 0.1f)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(totalBgColor)
                                .border(1.8.dp, totalBorderColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("speed_month_total_box"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "مجموع الشهر:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = if (state.darkMode) Color.White else Slate900
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (autoAdvanceEnabled) Indigo500.copy(alpha = 0.15f)
                                            else Slate500.copy(alpha = 0.15f)
                                        )
                                        .border(
                                            1.dp,
                                            if (autoAdvanceEnabled) Indigo500 else Slate400,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { autoAdvanceEnabled = !autoAdvanceEnabled }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (autoAdvanceEnabled) "انتقال تلقائي: مفعل ✓" else "انتقال تلقائي: متوقف",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (autoAdvanceEnabled) (if (state.darkMode) Indigo400 else Indigo600) else Slate400
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (state.darkMode) Slate900 else Color.White)
                                    .border(1.5.dp, totalBorderColor, RoundedCornerShape(9.dp))
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${if (hasAnyInActiveMonth) liveMonthSum else 0} / ${state.maxAllowedGrade}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    ),
                                    color = totalBorderColor
                                )
                            }
                        }
                    } else {
                        val examTitle = when (activeTab) {
                            "mid" -> "درجة امتحان نصف السنة"
                            "final" -> "درجة الامتحان النهائي"
                            else -> "درجة امتحان الدور الثاني"
                        }
                        val currentExamVal = stdGrades.getField(activeTab)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (state.darkMode) Slate900 else Slate50)
                                    .border(2.dp, Amber500, RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                                    .testTag("grade_input_$activeTab"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = examTitle,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (state.darkMode) Amber400 else Amber500
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .height(1.dp)
                                        .background(Amber500.copy(alpha = 0.3f))
                                )
                                Text(
                                    text = currentExamVal.ifBlank { "-" },
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (state.darkMode) Color.White else Slate900
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (state.darkMode) Slate900 else Slate50)
                                    .border(
                                        2.dp,
                                        if (isPassingTab) Emerald500 else Rose500,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "النتيجة النهائية",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (isPassingTab) Emerald500 else Rose500
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .height(1.dp)
                                        .background((if (isPassingTab) Emerald500 else Rose500).copy(alpha = 0.3f))
                                )
                                Text(
                                    text = "${calcRes.finalResult} / ${state.maxAllowedGrade}",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (isPassingTab) Emerald500 else Rose500
                                )
                            }
                        }
                    }

                    // C. Single Clean Bordered Numeric Keypad
                    BuiltInSpeedKeypad(
                        darkMode = state.darkMode,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        activeAccent = activeFieldAccent,
                        maxAllowedGrade = state.maxAllowedGrade,
                        nextStepLabel = if (isMonthlyTab) {
                            when (activeSubFieldIdx) {
                                0 -> "تثبيت اليومي ⬅️ الانتقال للعملي"
                                1 -> "تثبيت العملي ⬅️ الانتقال للتحريري"
                                else -> "تثبيت التحريري ⬅️ الطالب التالي ✅"
                            }
                        } else {
                            "تثبيت الدرجة ⬅️ الطالب التالي ✅"
                        },
                        isLastFieldInStudent = !isMonthlyTab || activeSubFieldIdx == 2,
                        onDigitClick = { handleKeypadDigit(it.toString()) },
                        onFullTenClick = { handleQuickPresetNumber(10) },
                        onBackspaceClick = { handleBackspace() },
                        onClearClick = { handleClearField() },
                        onAdvanceClick = {
                            isFreshFieldSelection = true
                            onAdvanceField()
                        }
                    )
                }
            }

            // 5. Previous / Next Student Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clickable {
                            onUpdateSpeedConfig(max(0, activeIdx - 1), null, 0)
                        }
                        .testTag("speed_prev_student_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    border = BorderStroke(1.5.dp, cardBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (state.darkMode) Slate300 else Slate700,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "طالب سابق",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = if (state.darkMode) Slate300 else Slate700
                        )
                    }
                }

                Button(
                    onClick = {
                        if (activeIdx < speedList.size - 1) {
                            onUpdateSpeedConfig(activeIdx + 1, null, 0)
                        } else {
                            onFinishGrading()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    modifier = Modifier
                        .weight(1.8f)
                        .height(46.dp)
                        .testTag("speed_next_student_button")
                ) {
                    Text(
                        text = if (activeIdx < speedList.size - 1) "الطالب التالي (${activeIdx + 2}/${speedList.size})" else "إنهاء الرصد ✅",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            // Empty Section State
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا يوجد طلاب في هذه الشعبة.",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Slate400
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onCloseSpeedMode,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) {
                        Text(
                            text = "العودة للوحة التحكم",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showVoiceFallbackDialog && currentStudent != null) {
        val stdGrades = state.grades["${currentStudent.id}_${state.globalSub}"] ?: SubjectGrades()
        val mIdx = activeTab.removePrefix("m").toIntOrNull() ?: 1

        AlertDialog(
            onDismissRequest = { showVoiceFallbackDialog = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🎙️ الرصد الصوتي والذكي المتسلسل",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                    Text(
                        text = "الطالب: ${currentStudent.name} (${activeIdx + 1}/${speedList.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Indigo500
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isMonthlyTab) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0 to "يومي", 1 to "عملي", 2 to "تحريري").forEach { (idx, label) ->
                                val fName = "v${(mIdx - 1) * 3 + idx + 1}"
                                val fVal = stdGrades.getField(fName).ifBlank { "-" }
                                val isCur = activeSubFieldIdx == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isCur) Indigo600
                                            else Indigo500.copy(alpha = 0.12f)
                                        )
                                        .border(1.dp, Indigo500, RoundedCornerShape(10.dp))
                                        .clickable { onUpdateSpeedConfig(null, null, idx) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = if (isCur) Color.White else Indigo500
                                        )
                                        Text(
                                            text = fVal,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                            color = if (isCur) Color.White else Slate500
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "أدخل درجة ($activeFieldTitle) رقماً أو كتابةً (أو 3 درجات معاً مثل: 10 15 40) وسينتقل تلقائياً بالترتيب (يومي ⬅️ عملي ⬅️ تحريري ⬅️ الطالب التالي):",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500
                    )

                    OutlinedTextField(
                        value = voiceInputText,
                        onValueChange = { voiceInputText = it },
                        placeholder = { Text("اكتب الرقم (مثلاً: 15 أو خمسة وعشرين)...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val txt = voiceInputText.trim()
                                if (txt.isNotEmpty()) {
                                    voiceInputText = ""
                                    onVoiceSpoken(txt)
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val quickPresets = if (state.maxAllowedGrade == 10) {
                        listOf("0", "2", "4", "5", "6", "7", "8", "9", "10")
                    } else if (isMonthlyTab && activeSubFieldIdx < 2) {
                        listOf("5", "8", "10", "12", "15", "18", "20", "25", "30")
                    } else {
                        listOf("20", "25", "30", "35", "40", "45", "50", "60", "70", "80", "90", "100")
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickPresets.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Indigo500.copy(alpha = 0.14f))
                                    .border(1.dp, Indigo500.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        voiceInputText = ""
                                        onVoiceSpoken(preset)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = Indigo600
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val txt = voiceInputText.trim()
                        if (txt.isNotEmpty()) {
                            voiceInputText = ""
                            onVoiceSpoken(txt)
                        } else {
                            onAdvanceField()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    val btnLabel = if (isMonthlyTab) {
                        when (activeSubFieldIdx) {
                            0 -> "رصد اليومي ⬅️ العملي"
                            1 -> "رصد العملي ⬅️ التحريري"
                            else -> "رصد التحريري ⬅️ الطالب التالي"
                        }
                    } else {
                        "رصد الدرجة ⬅️ الطالب التالي"
                    }
                    Text(btnLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoiceFallbackDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun BorderedSummaryCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 5.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = Slate400,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            ),
            color = valueColor,
            maxLines = 1
        )
    }
}

@Composable
private fun InteractiveGradeFieldBox(
    stepNum: Int,
    label: String,
    value: String,
    isSelected: Boolean,
    accentColor: Color,
    darkMode: Boolean,
    cardBg: Color,
    cardBorder: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boxBorderColor = if (isSelected) accentColor else cardBorder
    val headerBgColor = if (isSelected) {
        accentColor
    } else {
        accentColor.copy(alpha = if (darkMode) 0.18f else 0.12f)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(
                width = if (isSelected) 2.5.dp else 1.5.dp,
                color = boxBorderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Framed Header Band separated by a horizontal border line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBgColor)
                .padding(vertical = 5.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$stepNum. $label",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                ),
                color = if (isSelected) Color.White else accentColor,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.2.dp)
                .background(boxBorderColor)
        )

        // Framed Number Cell
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    if (isSelected) accentColor.copy(alpha = if (darkMode) 0.14f else 0.07f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.ifBlank { "-" },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp
                ),
                color = if (value.isBlank()) {
                    Slate400.copy(alpha = 0.5f)
                } else if (isSelected) {
                    accentColor
                } else {
                    if (darkMode) Color.White else Slate900
                }
            )
        }
    }
}

@Composable
private fun BuiltInSpeedKeypad(
    darkMode: Boolean,
    cardBg: Color,
    cardBorder: Color,
    activeAccent: Color,
    maxAllowedGrade: Int,
    nextStepLabel: String,
    isLastFieldInStudent: Boolean,
    onDigitClick: (Int) -> Unit,
    onFullTenClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onAdvanceClick: () -> Unit
) {
    val keyBg = if (darkMode) Slate900 else Slate50
    val keyBorder = if (darkMode) Slate600 else Slate300
    val keyText = if (darkMode) Color.White else Slate900

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (darkMode) Slate800 else Slate100)
            .border(1.5.dp, cardBorder, RoundedCornerShape(14.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: 1, 2, 3 | Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(1, 2, 3).forEach { digit ->
                SpeedKeyButton(
                    text = "$digit",
                    bg = keyBg,
                    border = keyBorder,
                    textColor = keyText,
                    onClick = { onDigitClick(digit) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("keypad_digit_$digit")
                )
            }
            SpeedKeyButton(
                text = "⌫ حذف",
                bg = Rose500.copy(alpha = 0.14f),
                border = Rose500,
                textColor = Rose500,
                isActionKey = true,
                onClick = onBackspaceClick,
                modifier = Modifier
                    .weight(1.05f)
                    .testTag("keypad_backspace")
            )
        }

        // Row 2: 4, 5, 6 | Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(4, 5, 6).forEach { digit ->
                SpeedKeyButton(
                    text = "$digit",
                    bg = keyBg,
                    border = keyBorder,
                    textColor = keyText,
                    onClick = { onDigitClick(digit) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("keypad_digit_$digit")
                )
            }
            SpeedKeyButton(
                text = "تصفير",
                bg = Amber500.copy(alpha = 0.15f),
                border = Amber500,
                textColor = if (darkMode) Amber400 else Amber500,
                isActionKey = true,
                onClick = onClearClick,
                modifier = Modifier
                    .weight(1.05f)
                    .testTag("keypad_clear")
            )
        }

        // Row 3: 7, 8, 9 | 0
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(7, 8, 9).forEach { digit ->
                SpeedKeyButton(
                    text = "$digit",
                    bg = keyBg,
                    border = keyBorder,
                    textColor = keyText,
                    onClick = { onDigitClick(digit) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("keypad_digit_$digit")
                )
            }
            SpeedKeyButton(
                text = "0",
                bg = keyBg,
                border = activeAccent,
                textColor = activeAccent,
                onClick = { onDigitClick(0) },
                modifier = Modifier
                    .weight(1.05f)
                    .testTag("keypad_digit_0")
            )
        }

        // Row 4: Full-mark 10 button (if maxGrade == 10) + Sequential Advance button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (maxAllowedGrade == 10) {
                SpeedKeyButton(
                    text = "10 كاملة",
                    bg = Emerald500.copy(alpha = 0.16f),
                    border = Emerald500,
                    textColor = Emerald500,
                    isActionKey = true,
                    onClick = onFullTenClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("keypad_full_10")
                )
            }

            Button(
                onClick = onAdvanceClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLastFieldInStudent) Emerald500 else activeAccent
                ),
                modifier = Modifier
                    .weight(if (maxAllowedGrade == 10) 2.15f else 1f)
                    .height(46.dp)
                    .testTag("keypad_advance_field_button")
            ) {
                Text(
                    text = nextStepLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SpeedKeyButton(
    text: String,
    bg: Color,
    border: Color,
    textColor: Color,
    isActionKey: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(1.5.dp, border),
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = if (isActionKey) {
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                } else {
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 21.sp
                    )
                },
                color = textColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfficialPrintPreviewScreen(
    state: WaadUiState,
    filteredStudents: List<Student>,
    stats: StatsSummary,
    calculateResult: (String, String) -> CalculatedResult,
    onTriggerSystemPrint: (
        templateType: String,
        monthKey: String,
        colorTheme: Boolean,
        isLandscape: Boolean,
        showSignatures: Boolean,
        showSupervisorSign: Boolean,
        blankColumnsCount: Int,
        sortOrder: String
    ) -> Unit,
    onExportExcel: () -> Unit = {},
    onSharePrintSummary: (templateType: String, monthKey: String) -> Unit = { _, _ -> },
    onSecChange: (String) -> Unit = {},
    onSubChange: (String) -> Unit = {},
    onClosePrintMode: () -> Unit
) {
    BackHandler { onClosePrintMode() }

    val activeSub = state.globalSub.ifBlank { state.currentStage.subjects.firstOrNull() ?: "---" }

    var templateType by remember { mutableStateOf("annual") } // "annual", "monthly", "attendance", "blank_roster"
    var monthKey by remember { mutableStateOf("m1") } // "m1", "m2", "m3", "m4"
    var colorTheme by remember { mutableStateOf(true) }
    var isLandscape by remember { mutableStateOf(true) }
    var showSignatures by remember { mutableStateOf(true) }
    var showSupervisorSign by remember { mutableStateOf(false) }
    var blankColumnsCount by remember { mutableStateOf(8) }
    var sortOrder by remember { mutableStateOf("alpha") } // "alpha", "top_score", "passed_only", "failed_only"
    var isControlPanelExpanded by remember { mutableStateOf(true) }

    val sortedStudents = remember(filteredStudents, sortOrder, activeSub, state.grades, state.maxAllowedGrade, state.failThreshold) {
        when (sortOrder) {
            "top_score" -> filteredStudents.sortedByDescending { s ->
                val res = calculateResult(s.id, activeSub)
                val effectiveSaei = min(state.maxAllowedGrade, res.saei + res.qarar)
                res.secondRoundResult ?: if (res.hasFinalResult) res.finalResult else effectiveSaei
            }
            "passed_only" -> filteredStudents.filter { s ->
                val res = calculateResult(s.id, activeSub)
                val effectiveSaei = min(state.maxAllowedGrade, res.saei + res.qarar)
                val finalScore = res.secondRoundResult ?: if (res.hasFinalResult) res.finalResult else effectiveSaei
                res.hasSaei && finalScore >= state.failThreshold
            }
            "failed_only" -> filteredStudents.filter { s ->
                val res = calculateResult(s.id, activeSub)
                val effectiveSaei = min(state.maxAllowedGrade, res.saei + res.qarar)
                val finalScore = res.secondRoundResult ?: if (res.hasFinalResult) res.finalResult else effectiveSaei
                res.hasSaei && finalScore < state.failThreshold
            }
            else -> filteredStudents.sortedBy { it.name }
        }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate100)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // 1. Top Command & Export Header Bar
        Surface(
            color = Slate900,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(listOf(Indigo500, Purple600))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "استوديو الطباعة والتقارير الرسمية",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                            Text(
                                text = "${state.currentDept.name} • ${state.currentStage.name} (${sortedStudents.size} طالب)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Indigo400
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = { isControlPanelExpanded = !isControlPanelExpanded },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isControlPanelExpanded) Indigo600 else Slate800,
                            border = BorderStroke(1.dp, Indigo400.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (isControlPanelExpanded) "إخفاء الإعدادات ▲" else "تخصيص السجل ⚙️",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            onClick = onClosePrintMode,
                            shape = RoundedCornerShape(10.dp),
                            color = Rose600,
                            modifier = Modifier.testTag("close_print_preview_button")
                        ) {
                            Text(
                                text = "إغلاق ✕",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Direct Export & Print Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onTriggerSystemPrint(
                                templateType,
                                monthKey,
                                colorTheme,
                                isLandscape,
                                showSignatures,
                                showSupervisorSign,
                                blankColumnsCount,
                                sortOrder
                            )
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(42.dp)
                            .testTag("print_now_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "طباعة / حفظ PDF",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = Color.White,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onExportExcel,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("print_export_excel_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                    ) {
                        Text(
                            text = "📊 تصدير Excel",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = Color.White,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = { onSharePrintSummary(templateType, monthKey) },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("print_share_summary_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                    ) {
                        Text(
                            text = "📤 مشاركة السجل",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 2. Interactive Print Studio Customization Panel
        if (isControlPanelExpanded) {
            Surface(
                color = Color.White,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Template Selector Pills
                    Text(
                        text = "📑 اختر نوع القالب الرسمي:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = Slate600
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val templates = listOf(
                            "annual" to "📊 السجل السنوي (14 عموداً)",
                            "monthly" to "📝 سجل المتابعة الشهري",
                            "attendance" to "📅 سجل الغياب والإنذارات",
                            "blank_roster" to "🗒️ سجل ورقي فارغ للرصد"
                        )
                        templates.forEach { (key, label) ->
                            val selected = templateType == key
                            Surface(
                                onClick = { templateType = key },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) Indigo600 else Slate100,
                                border = BorderStroke(1.dp, if (selected) Indigo600 else Slate300),
                                modifier = Modifier.testTag("print_template_$key")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = if (selected) Color.White else Slate800,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Conditional Sub-controls for Monthly or Blank Roster
                    if (templateType == "monthly") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الشهر:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = Indigo600
                            )
                            val months = listOf(
                                "m1" to "فصل 1 - شهر 1",
                                "m2" to "فصل 1 - شهر 2",
                                "m3" to "فصل 2 - شهر 1",
                                "m4" to "فصل 2 - شهر 2"
                            )
                            months.forEach { (mKey, mLabel) ->
                                val sel = monthKey == mKey
                                Surface(
                                    onClick = { monthKey = mKey },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sel) Purple600 else Slate100
                                ) {
                                    Text(
                                        text = mLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (sel) Color.White else Slate700,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    } else if (templateType == "blank_roster") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "عدد الحقول الفارغة:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = Indigo600
                            )
                            listOf(6, 8, 10, 12).forEach { cols ->
                                val sel = blankColumnsCount == cols
                                Surface(
                                    onClick = { blankColumnsCount = cols },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sel) Purple600 else Slate100
                                ) {
                                    Text(
                                        text = "$cols حقول",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (sel) Color.White else Slate700,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Section & Subject Quick Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الشعبة:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = Slate600
                        )
                        val sections = listOf("" to "الكل") + state.stageSections.map { it to "شعبة ($it)" }
                        sections.forEach { (secVal, secName) ->
                            val sel = state.globalSec == secVal
                            Surface(
                                onClick = { onSecChange(secVal) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (sel) Slate900 else Slate100,
                                border = BorderStroke(1.dp, if (sel) Slate900 else Slate300)
                            ) {
                                Text(
                                    text = secName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (sel) Color.White else Slate700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (state.currentStage.subjects.size > 1 && templateType != "attendance") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "المادة:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = Slate600
                            )
                            state.currentStage.subjects.forEach { subItem ->
                                val sel = activeSub == subItem
                                Surface(
                                    onClick = { onSubChange(subItem) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sel) Indigo600 else Slate100,
                                    border = BorderStroke(1.dp, if (sel) Indigo600 else Slate300)
                                ) {
                                    Text(
                                        text = subItem,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (sel) Color.White else Slate700,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Sort & Print Style Toggles Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val sorts = listOf(
                            "alpha" to "🔤 أبجدي",
                            "top_score" to "🏆 الأعلى درجة",
                            "passed_only" to "✅ الناجحون فقط",
                            "failed_only" to "⚠️ المكملون/الراسبون"
                        )
                        sorts.forEach { (sKey, sLabel) ->
                            val sel = sortOrder == sKey
                            Surface(
                                onClick = { sortOrder = sKey },
                                shape = RoundedCornerShape(8.dp),
                                color = if (sel) Amber500 else Slate100
                            ) {
                                Text(
                                    text = sLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = if (sel) Color.White else Slate700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = { colorTheme = !colorTheme },
                            shape = RoundedCornerShape(8.dp),
                            color = if (colorTheme) Color(0xFFEEF2FF) else Slate200,
                            border = BorderStroke(1.dp, if (colorTheme) Indigo400 else Slate400)
                        ) {
                            Text(
                                text = if (colorTheme) "🎨 ملون رسمي" else "⚫ ليزري أبيض وأسود",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = if (colorTheme) Indigo600 else Slate900,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            onClick = { isLandscape = !isLandscape },
                            shape = RoundedCornerShape(8.dp),
                            color = Slate100,
                            border = BorderStroke(1.dp, Slate300)
                        ) {
                            Text(
                                text = if (isLandscape) "📐 ورقة A4 أفقية" else "📄 ورقة A4 عمودية",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = Slate800,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            onClick = { showSupervisorSign = !showSupervisorSign },
                            shape = RoundedCornerShape(8.dp),
                            color = if (showSupervisorSign) Color(0xFFECFDF5) else Slate100,
                            border = BorderStroke(1.dp, if (showSupervisorSign) Emerald500 else Slate300)
                        ) {
                            Text(
                                text = if (showSupervisorSign) "👔 توقيع المشرف: مفعل" else "👔 + توقيع المشرف",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = if (showSupervisorSign) Color(0xFF047857) else Slate700,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Printable A4 Paper Sheet Preview
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, Slate300)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Official Ministry & School Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.settings.ministryName,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                color = Color.Black
                            )
                            Text(
                                text = state.settings.schoolName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = if (colorTheme) Indigo600 else Color.Black
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1.3f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = reportTitle,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    textDecoration = TextDecoration.Underline
                                ),
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "العام الدراسي: ${state.settings.academicYear}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Slate700
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "المرحلة: ${state.currentDept.name} / ${state.currentStage.name}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.Black
                            )
                            Text(
                                text = "الشعبة: ${state.globalSec.ifBlank { "الكل" }} | المادة: $activeSub",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = if (colorTheme) Purple600 else Color.Black
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (colorTheme) Indigo600 else Color.Black)
                    )

                    val headerBg = if (colorTheme) Color(0xFFEEF2FF) else Slate100

                    // Dynamic Table Content by Template Type
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier.border(2.dp, if (colorTheme) Indigo600 else Color.Black)
                        ) {
                            when (templateType) {
                                "monthly" -> {
                                    val mIdx = monthKey.removePrefix("m").toIntOrNull()?.coerceIn(1, 4) ?: 1
                                    val startField = (mIdx - 1) * 3 + 1
                                    Row(
                                        modifier = Modifier.background(headerBg),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PrintCell("ت", 34.dp, isHeader = true)
                                        PrintCell("اسم الطالب الرباعي", 165.dp, isHeader = true, alignRight = true)
                                        PrintCell("الشعبة", 48.dp, isHeader = true)
                                        PrintCell("اليومي", 58.dp, isHeader = true)
                                        PrintCell("العملي", 58.dp, isHeader = true)
                                        PrintCell("التحريري", 64.dp, isHeader = true)
                                        PrintCell("المجموع (${state.maxAllowedGrade})", 82.dp, isHeader = true)
                                        PrintCell("الواجبات", 74.dp, isHeader = true)
                                        PrintCell("الغيابات", 64.dp, isHeader = true)
                                        PrintCell("التقدير", 88.dp, isHeader = true)
                                    }

                                    sortedStudents.forEachIndexed { idx, std ->
                                        val g = state.grades["${std.id}_$activeSub"] ?: SubjectGrades()
                                        val vDaily = g.getField("v$startField").toIntOrNull()
                                        val vPrac = g.getField("v${startField + 1}").toIntOrNull()
                                        val vWrit = g.getField("v${startField + 2}").toIntOrNull()
                                        val hasAny = vDaily != null || vPrac != null || vWrit != null
                                        val sum = (vDaily ?: 0) + (vPrac ?: 0) + (vWrit ?: 0)
                                        val hwMiss = state.missingHomework["${std.id}_$activeSub"] ?: 0
                                        val absCount = state.attendance.count { (k, v) -> k.startsWith("${std.id}_") && v == "absent" }
                                        val evalLabel = if (!hasAny) "-" else when {
                                            sum >= (state.maxAllowedGrade * 0.9).toInt() -> "امتياز 🌟"
                                            sum >= (state.maxAllowedGrade * 0.8).toInt() -> "جيد جداً"
                                            sum >= (state.maxAllowedGrade * 0.7).toInt() -> "جيد"
                                            sum >= state.failThreshold -> "متوسط"
                                            else -> "ضعيف ⚠️"
                                        }
                                        val rowBg = if (colorTheme && idx % 2 == 1) Slate50 else Color.Transparent
                                        val sumTxtColor = if (!colorTheme || !hasAny) Color.Black else if (sum >= state.failThreshold) Color(0xFF047857) else Rose600

                                        Row(
                                            modifier = Modifier.background(rowBg),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PrintCell("${idx + 1}", 34.dp)
                                            PrintCell(std.name, 165.dp, alignRight = true)
                                            PrintCell(std.section, 48.dp)
                                            PrintCell("${vDaily ?: "-"}", 58.dp)
                                            PrintCell("${vPrac ?: "-"}", 58.dp)
                                            PrintCell("${vWrit ?: "-"}", 64.dp)
                                            PrintCell(if (hasAny) "$sum" else "-", 82.dp, textColor = sumTxtColor)
                                            PrintCell(
                                                if (hwMiss > 0) "$hwMiss نقص" else "ملتزم ✓",
                                                74.dp,
                                                textColor = if (colorTheme && hwMiss > 0) Amber500 else Color.Black
                                            )
                                            PrintCell(
                                                if (absCount > 0) "$absCount يوم" else "0",
                                                64.dp,
                                                textColor = if (colorTheme && absCount > 0) Rose600 else Color.Black
                                            )
                                            PrintCell(evalLabel, 88.dp, textColor = sumTxtColor)
                                        }
                                    }
                                }

                                "attendance" -> {
                                    Row(
                                        modifier = Modifier.background(headerBg),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PrintCell("ت", 34.dp, isHeader = true)
                                        PrintCell("اسم الطالب", 165.dp, isHeader = true, alignRight = true)
                                        PrintCell("الشعبة", 48.dp, isHeader = true)
                                        PrintCell("اليوم (${state.attendanceDate})", 95.dp, isHeader = true)
                                        PrintCell("الحضور", 58.dp, isHeader = true)
                                        PrintCell("الغياب", 58.dp, isHeader = true)
                                        PrintCell("الإجازات", 58.dp, isHeader = true)
                                        PrintCell("الالتزام", 62.dp, isHeader = true)
                                        PrintCell("حالة الإنذار الوزاري", 130.dp, isHeader = true)
                                        PrintCell("هاتف ولي الأمر", 95.dp, isHeader = true)
                                    }

                                    sortedStudents.forEachIndexed { idx, std ->
                                        val records = state.attendance.filterKeys { it.startsWith("${std.id}_") }
                                        val absCount = records.values.count { it == "absent" }
                                        val excCount = records.values.count { it == "excused" }
                                        val presCount = records.values.count { it == "present" }
                                        val todaySt = state.attendance["${std.id}_${state.attendanceDate}"] ?: "present"
                                        val todayLabel = when (todaySt) {
                                            "absent" -> "غائب ❌"
                                            "excused" -> "إجازة 🕒"
                                            else -> "حاضر ✅"
                                        }
                                        val totalTracked = (presCount + absCount + excCount).coerceAtLeast(1)
                                        val rate = ((presCount + excCount) * 100) / totalTracked
                                        val warnText = when {
                                            absCount >= 21 -> "فصل بالغياب ⛔"
                                            absCount >= 7 -> "إنذار نهائي ($absCount) ⛔"
                                            absCount >= 5 -> "إنذار ثاني ($absCount) 🚨"
                                            absCount >= 3 -> "إنذار أول ($absCount) ⚠️"
                                            absCount >= 1 -> "تنبيه أولي ($absCount)"
                                            else -> "ملتزم تماماً ✅"
                                        }
                                        val warnCol = if (!colorTheme) Color.Black else when {
                                            absCount >= 5 -> Rose600
                                            absCount >= 3 -> Amber500
                                            else -> Color(0xFF047857)
                                        }
                                        val rowBg = if (colorTheme && idx % 2 == 1) Slate50 else Color.Transparent

                                        Row(
                                            modifier = Modifier.background(rowBg),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PrintCell("${idx + 1}", 34.dp)
                                            PrintCell(std.name, 165.dp, alignRight = true)
                                            PrintCell(std.section, 48.dp)
                                            PrintCell(todayLabel, 95.dp)
                                            PrintCell("$presCount", 58.dp)
                                            PrintCell(
                                                "$absCount",
                                                58.dp,
                                                textColor = if (colorTheme && absCount > 0) Rose600 else Color.Black
                                            )
                                            PrintCell("$excCount", 58.dp)
                                            PrintCell("$rate%", 62.dp)
                                            PrintCell(warnText, 130.dp, textColor = warnCol)
                                            PrintCell(std.phone.ifBlank { "-" }, 95.dp)
                                        }
                                    }
                                }

                                "blank_roster" -> {
                                    val cols = blankColumnsCount.coerceIn(4, 14)
                                    Row(
                                        modifier = Modifier.background(headerBg),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PrintCell("ت", 34.dp, isHeader = true)
                                        PrintCell("اسم الطالب", 170.dp, isHeader = true, alignRight = true)
                                        PrintCell("الشعبة", 48.dp, isHeader = true)
                                        for (c in 1..cols) {
                                            PrintCell("حقل $c", 54.dp, isHeader = true)
                                        }
                                        PrintCell("الملاحظات", 110.dp, isHeader = true)
                                    }
                                    sortedStudents.forEachIndexed { idx, std ->
                                        val rowBg = if (colorTheme && idx % 2 == 1) Slate50 else Color.Transparent
                                        Row(
                                            modifier = Modifier.background(rowBg),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PrintCell("${idx + 1}", 34.dp)
                                            PrintCell(std.name, 170.dp, alignRight = true)
                                            PrintCell(std.section, 48.dp)
                                            for (c in 1..cols) {
                                                PrintCell("", 54.dp)
                                            }
                                            PrintCell("", 110.dp)
                                        }
                                    }
                                }

                                else -> {
                                    // Default Annual 14-Column Ledger
                                    Row(
                                        modifier = Modifier.background(headerBg),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PrintCell("ت", 34.dp, isHeader = true)
                                        PrintCell("اسم الطالب", 155.dp, isHeader = true, alignRight = true)
                                        PrintCell("ف1 ش1", 46.dp, isHeader = true)
                                        PrintCell("ف1 ش2", 46.dp, isHeader = true)
                                        PrintCell("معدل ف1", 52.dp, isHeader = true)
                                        PrintCell("نصف السنة", 60.dp, isHeader = true)
                                        PrintCell("ف2 ش1", 46.dp, isHeader = true)
                                        PrintCell("ف2 ش2", 46.dp, isHeader = true)
                                        PrintCell("معدل ف2", 52.dp, isHeader = true)
                                        PrintCell("السعي السنوي", 68.dp, isHeader = true)
                                        PrintCell("النهائي", 50.dp, isHeader = true)
                                        PrintCell("قرار", 44.dp, isHeader = true)
                                        PrintCell("دور ثان", 52.dp, isHeader = true, bgColor = if (colorTheme) Color(0xFFFFFBEB) else Color.Transparent)
                                        PrintCell("النتيجة", 62.dp, isHeader = true)
                                    }

                                    sortedStudents.forEachIndexed { idx, std ->
                                        val res = calculateResult(std.id, activeSub)
                                        val effectiveSaei = min(state.maxAllowedGrade, res.saei + res.qarar)
                                        val finalScore = res.secondRoundResult
                                            ?: if (res.hasFinalResult) res.finalResult else effectiveSaei
                                        val isPassing = finalScore >= state.failThreshold
                                        val status = if (res.hasSaei) {
                                            if (isPassing) "ناجح ✓" else "راسب"
                                        } else {
                                            "-"
                                        }
                                        val statusColor = if (!colorTheme || !res.hasSaei) {
                                            Color.Black
                                        } else if (isPassing) {
                                            Color(0xFF047857)
                                        } else {
                                            Rose600
                                        }
                                        val rowBg = if (colorTheme && idx % 2 == 1) Slate50 else Color.Transparent

                                        Row(
                                            modifier = Modifier.background(rowBg),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PrintCell("${idx + 1}", 34.dp)
                                            PrintCell("${std.name} (${std.section})", 155.dp, alignRight = true)
                                            PrintCell(if (res.hasM1) "${res.m1}" else "-", 46.dp)
                                            PrintCell(if (res.hasM2) "${res.m2}" else "-", 46.dp)
                                            PrintCell(
                                                if (res.hasAvg1) "${res.avg1}" else "-",
                                                52.dp,
                                                bgColor = if (colorTheme) Color(0xFFF8FAFC) else Color.Transparent
                                            )
                                            PrintCell(if (res.hasMid) "${res.mid}" else "-", 60.dp)
                                            PrintCell(if (res.hasM3) "${res.m3}" else "-", 46.dp)
                                            PrintCell(if (res.hasM4) "${res.m4}" else "-", 46.dp)
                                            PrintCell(
                                                if (res.hasAvg2) "${res.avg2}" else "-",
                                                52.dp,
                                                bgColor = if (colorTheme) Color(0xFFF8FAFC) else Color.Transparent
                                            )
                                            PrintCell(
                                                if (res.isExempt) "✨ معفي" else if (res.hasSaei) "$effectiveSaei" else "-",
                                                68.dp,
                                                textColor = if (colorTheme && res.isExempt) Indigo600 else Color.Black
                                            )
                                            PrintCell(if (res.hasFinal) "${res.finalExam}" else "-", 50.dp)
                                            PrintCell(
                                                if (res.qarar > 0) "+${res.qarar}" else "-",
                                                44.dp,
                                                textColor = if (colorTheme && res.qarar > 0) Amber500 else Color.Black
                                            )
                                            PrintCell(
                                                if (res.hasSecondRound) "${res.secondRoundExam}" else "-",
                                                52.dp,
                                                bgColor = if (colorTheme) Color(0xFFFFFBEB) else Color.Transparent
                                            )
                                            PrintCell(status, 62.dp, textColor = statusColor)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Summary Stats Table & Official Signatures
                    if (showSignatures) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (colorTheme) Indigo600 else Color.Black)
                            ) {
                                Row(modifier = Modifier.background(headerBg)) {
                                    SummaryCell("العدد الكلي", Modifier.weight(1f), isHeader = true)
                                    SummaryCell("المشتركون", Modifier.weight(1f), isHeader = true)
                                    SummaryCell("الناجحون", Modifier.weight(1f), isHeader = true)
                                    SummaryCell("المكملون / الراسبون", Modifier.weight(1.2f), isHeader = true)
                                    SummaryCell("نسبة النجاح", Modifier.weight(1f), isHeader = true)
                                }
                                Row {
                                    SummaryCell("${stats.total}", Modifier.weight(1f))
                                    SummaryCell("${stats.activeCount}", Modifier.weight(1f))
                                    SummaryCell("${stats.passed}", Modifier.weight(1f))
                                    SummaryCell("${stats.failed}", Modifier.weight(1.2f))
                                    SummaryCell("${stats.rate}%", Modifier.weight(1f))
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "توقيع مدرس المادة",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = state.settings.teacherName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Black
                                    )
                                }

                                if (showSupervisorSign) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "توقيع المشرف الاختصاصي",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = ".................................",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.Black
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "مصادقة وتوقيع مدير المدرسة",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = ".................................",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrintCell(
    text: String,
    width: Dp,
    isHeader: Boolean = false,
    alignRight: Boolean = false,
    bgColor: Color = Color.Transparent,
    textColor: Color = Color.Black
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(34.dp)
            .background(bgColor)
            .border(0.5.dp, Color.Black)
            .padding(horizontal = 4.dp),
        contentAlignment = if (alignRight) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isHeader) FontWeight.Black else FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SummaryCell(
    text: String,
    modifier: Modifier = Modifier,
    isHeader: Boolean = false
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .border(0.5.dp, Color.Black)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 10.sp
            ),
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

