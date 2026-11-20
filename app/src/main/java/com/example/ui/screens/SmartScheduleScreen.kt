package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ScheduledClass
import com.example.model.StageConfig
import com.example.model.WaadCalculator
import com.example.notifications.LessonNotificationHelper
import com.example.ui.WaadUiState
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber500
import com.example.ui.theme.Blue500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Indigo900
import com.example.ui.theme.Purple600
import com.example.ui.theme.Purple900
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
import java.util.Locale

private data class EditingSlotTarget(
    val day: String,
    val lessonNum: Int
)

@Composable
fun ScheduleScreen(
    state: WaadUiState,
    onUpdateSlot: (String, Int, ScheduledClass) -> Unit,
    onCopyDaySchedule: (String, String) -> Unit = { _, _ -> },
    onClearDaySchedule: (String) -> Unit = {},
    onUpdateNotificationSettings: (Boolean, Int) -> Unit = { _, _ -> },
    onTriggerLessonNotification: (String?, Int?) -> Unit = { _, _ -> },
    onJumpToScheduledClass: (ScheduledClass, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val liveLesson = state.currentLiveLesson
    val initialDay = if (liveLesson.dayName in WaadCalculator.WEEK_DAYS) {
        liveLesson.dayName
    } else {
        "الأحد"
    }

    var selectedDay by remember { mutableStateOf(initialDay) }
    var viewMode by remember { mutableStateOf("timeline") } // "timeline" or "matrix"
    var editingTarget by remember { mutableStateOf<EditingSlotTarget?>(null) }
    var showCopyDayDialog by remember { mutableStateOf(false) }

    var pendingActionAfterPerm by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingActionAfterPerm?.invoke()
        } else {
            onUpdateNotificationSettings(false, state.notificationSettings.leadMinutes)
        }
        pendingActionAfterPerm = null
    }

    fun runWithNotificationPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !LessonNotificationHelper.hasNotificationPermission(context)
        ) {
            pendingActionAfterPerm = action
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    val totalWeeklyAssigned = remember(state.teacherSchedule) {
        state.teacherSchedule.values.count { it.subject.isNotBlank() }
    }
    val selectedDayAssignedCount = remember(state.teacherSchedule, selectedDay) {
        (1..6).count { l -> state.teacherSchedule["${selectedDay}_$l"]?.subject?.isNotBlank() == true }
    }
    val selectedDayTotalBreakMinutes = remember(state.teacherSchedule, selectedDay) {
        WaadCalculator.calculateDayTotalBreakMinutes(selectedDay, state.teacherSchedule)
    }

    val cardBg = if (state.darkMode) Slate800 else Color.White
    val cardBorder = if (state.darkMode) Slate700 else Slate200

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Smart Radar & Teacher Notification Control Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Indigo900, Purple900, Color(0xFF1E1B4B))
                    )
                )
                .border(1.dp, Indigo500.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header Title + Weekly Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Amber400.copy(alpha = 0.2f))
                                .border(1.dp, Amber400.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Amber400,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "مركز الحصص والجدول الذكي",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                            Text(
                                text = "عرض أسماء الحصص والتنبيه التلقائي للمدرس",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Slate300
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalWeeklyAssigned / 30",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                color = Amber400
                            )
                            Text(
                                text = "حصة أسبوعياً",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }

                // Active / Live Lesson Highlight Inside Schedule Hub
                val liveScheduled = liveLesson.scheduledClass
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.09f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (liveLesson.isLiveTimeWindow) Emerald500 else Amber400)
                                )
                                Text(
                                    text = "البث الزمني (${liveLesson.dayName} • ${liveLesson.lessonOrdinalName})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = Amber400
                                )
                                Text(
                                    text = "${liveLesson.startTime} - ${liveLesson.endTime}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Slate300
                                )
                            }

                            if (liveScheduled != null && liveScheduled.subject.isNotBlank()) {
                                val liveDept = state.settings.structure.getOrElse(liveScheduled.deptIdx) { state.currentDept }
                                val liveStage = liveDept.stages.getOrElse(liveScheduled.stageIdx) { state.currentStage }
                                val titlePart = if (liveScheduled.lessonTitle.isNotBlank()) " (${liveScheduled.lessonTitle})" else ""
                                Text(
                                    text = "اسم الحصة: ${liveScheduled.subject}$titlePart",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                                Text(
                                    text = "${liveDept.name} • ${liveStage.name} • شعبة (${liveScheduled.section})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Slate300
                                )
                            } else {
                                Text(
                                    text = "لا توجد حصة مسجلة في هذا التوقيت",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Slate300
                                )
                            }
                        }

                        if (liveScheduled != null && liveScheduled.subject.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Emerald500)
                                    .clickable { onJumpToScheduledClass(liveScheduled, "list") }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "دخول الحصة 🚀",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Teacher Notification Control Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.28f))
                        .border(
                            1.dp,
                            if (state.notificationSettings.enabled) Emerald500.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.14f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                Icon(
                                    imageVector = if (state.notificationSettings.enabled) {
                                        Icons.Default.NotificationsActive
                                    } else {
                                        Icons.Default.NotificationsOff
                                    },
                                    contentDescription = null,
                                    tint = if (state.notificationSettings.enabled) Color(0xFF34D399) else Slate400,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "إشعارات تنبيه المدرس بالحصص",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (state.notificationSettings.enabled) {
                                            "مفعّلة • يصلك إشعار باسم الحصة والصف والشعبة تلقائياً"
                                        } else {
                                            "متوقفة • فعّل الزر لتلقي تنبيهات بمواعيد الحصص"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (state.notificationSettings.enabled) Color(0xFFA7F3D0) else Slate400
                                    )
                                }
                            }

                            Switch(
                                checked = state.notificationSettings.enabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        runWithNotificationPermission {
                                            onUpdateNotificationSettings(true, state.notificationSettings.leadMinutes)
                                        }
                                    } else {
                                        onUpdateNotificationSettings(false, state.notificationSettings.leadMinutes)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Emerald500,
                                    uncheckedThumbColor = Slate300,
                                    uncheckedTrackColor = Slate700
                                ),
                                modifier = Modifier.testTag("schedule_notifications_switch")
                            )
                        }

                        // Lead time chips (4 equal horizontal pills)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val leadOptions = listOf(
                                0 to "عند البدء",
                                5 to "قبل 5 د",
                                10 to "قبل 10 د",
                                15 to "قبل 15 د"
                            )
                            leadOptions.forEach { (mins, label) ->
                                val isSel = state.notificationSettings.leadMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSel) Indigo500 else Color.White.copy(alpha = 0.1f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSel) Amber400 else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            runWithNotificationPermission {
                                                onUpdateNotificationSettings(true, mins)
                                            }
                                        }
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = if (isSel) Color.White else Slate300,
                                        maxLines = 1,
                                        softWrap = false,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Instant Test Alert button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Amber500)
                                .clickable {
                                    runWithNotificationPermission {
                                        val firstSlotLesson = (1..6).firstOrNull { l ->
                                            state.teacherSchedule["${selectedDay}_$l"]?.subject?.isNotBlank() == true
                                        } ?: liveLesson.lessonNum
                                        onTriggerLessonNotification(selectedDay, firstSlotLesson)
                                    }
                                }
                                .testTag("test_lesson_notification_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Slate900,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "تجربة التنبيه الفوري",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = Slate900,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. View Mode Switcher (العرض الزمني التفاعلي vs الشبكة الأسبوعية الشاملة)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScheduleModeTabButton(
                    title = "واجهة الحصص الزمنية",
                    icon = Icons.Default.Timeline,
                    selected = viewMode == "timeline",
                    darkMode = state.darkMode,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("schedule_mode_timeline"),
                    onClick = { viewMode = "timeline" }
                )
                ScheduleModeTabButton(
                    title = "الشبكة الأسبوعية الشاملة",
                    icon = Icons.Default.CalendarViewWeek,
                    selected = viewMode == "matrix",
                    darkMode = state.darkMode,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("schedule_mode_matrix"),
                    onClick = { viewMode = "matrix" }
                )
            }
        }

        if (viewMode == "timeline") {
            // 3A. Interactive Day Selector Ribbon (Uniform height & layout for all 5 days)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WaadCalculator.WEEK_DAYS.forEach { day ->
                    val isSelected = day == selectedDay
                    val isToday = day == liveLesson.dayName
                    val dayCount = (1..6).count { l ->
                        state.teacherSchedule["${day}_$l"]?.subject?.isNotBlank() == true
                    }

                    val pillBg = when {
                        isSelected -> Brush.verticalGradient(listOf(Indigo600, Purple600))
                        state.darkMode -> Brush.verticalGradient(listOf(Slate800, Slate800))
                        else -> Brush.verticalGradient(listOf(Color.White, Color.White))
                    }
                    val borderCol = when {
                        isSelected -> Amber400
                        isToday -> Indigo500
                        state.darkMode -> Slate700
                        else -> Slate200
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(62.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(pillBg)
                            .border(1.5.dp, borderCol, RoundedCornerShape(16.dp))
                            .clickable { selectedDay = day }
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .testTag("schedule_day_tab_$day"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = if (isSelected) Color.White else (if (state.darkMode) Color.White else Slate900),
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (dayCount > 0) "$dayCount حصص" else "شاغر",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isSelected) Color(0xFFFDE047) else Slate400,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Day Sub-Header with Dynamic Break Summary + Copy & Clear Day Tools
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "📅 حصص يوم $selectedDay",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = if (state.darkMode) Color.White else Slate900
                            )
                            Text(
                                text = "المجدولة: $selectedDayAssignedCount من 6 • الشواغر: ${6 - selectedDayAssignedCount}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Slate400
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (state.darkMode) Slate700 else Indigo500.copy(alpha = 0.1f))
                                    .border(
                                        1.dp,
                                        if (state.darkMode) Slate600 else Indigo500.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { showCopyDayDialog = true }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                    .testTag("copy_day_schedule_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = if (state.darkMode) Amber400 else Indigo600,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "نسخ اليوم",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (state.darkMode) Color.White else Indigo600
                                    )
                                }
                            }

                            if (selectedDayAssignedCount > 0 || (1..6).any { state.teacherSchedule.containsKey("${selectedDay}_$it") }) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Rose500.copy(alpha = 0.12f))
                                        .border(
                                            1.dp,
                                            Rose500.copy(alpha = 0.35f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onClearDaySchedule(selectedDay) }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                        .testTag("clear_day_schedule_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = Rose500,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "تفريغ",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = Rose500
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Dynamic Daily Breaks Summary Pill (محسوب من فارق الوقت بين الحصص)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (state.darkMode) Amber500.copy(alpha = 0.12f) else Color(0xFFFFFBEB)
                            )
                            .border(
                                1.dp,
                                Amber500.copy(alpha = 0.35f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "☕", fontSize = 14.sp)
                                Text(
                                    text = "إجمالي وقت الفرص والاستراحات اليومي:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = if (state.darkMode) Amber400 else Color(0xFFB45309)
                                )
                            }
                            Text(
                                text = "$selectedDayTotalBreakMinutes دقيقة (فارق وقت الحصص)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = if (state.darkMode) Color.White else Color(0xFF92400E)
                            )
                        }
                    }
                }
            }

            // 3B. Vertical Timeline of 6 Lessons for Selected Day
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (lessonNum in 1..6) {
                    val slotKey = "${selectedDay}_$lessonNum"
                    val slot = state.teacherSchedule[slotKey]
                    val isLiveSlot = selectedDay == liveLesson.dayName &&
                        lessonNum == liveLesson.lessonNum &&
                        liveLesson.isLiveTimeWindow

                    InnovativeLessonTimelineCard(
                        day = selectedDay,
                        lessonNum = lessonNum,
                        slot = slot,
                        isLiveSlot = isLiveSlot,
                        state = state,
                        onEditSlot = {
                            editingTarget = EditingSlotTarget(day = selectedDay, lessonNum = lessonNum)
                        },
                        onToggleSlotNotify = {
                            if (slot != null && slot.subject.isNotBlank()) {
                                val nextNotify = !slot.notifyEnabled
                                onUpdateSlot(selectedDay, lessonNum, slot.copy(notifyEnabled = nextNotify))
                            }
                        },
                        onTriggerAlertNow = {
                            runWithNotificationPermission {
                                onTriggerLessonNotification(selectedDay, lessonNum)
                            }
                        },
                        onEnterClassList = {
                            if (slot != null && slot.subject.isNotBlank()) {
                                onJumpToScheduledClass(slot, "list")
                            }
                        },
                        onEnterClassAttendance = {
                            if (slot != null && slot.subject.isNotBlank()) {
                                onJumpToScheduledClass(slot, "attendance")
                            }
                        }
                    )

                    // Dynamic Break / Recess Divider calculated from time difference between Lesson N and Lesson N+1
                    if (lessonNum < 6) {
                        val (_, currentEnd) = WaadCalculator.resolveLessonTimes(lessonNum, slot)
                        val nextSlot = state.teacherSchedule["${selectedDay}_${lessonNum + 1}"]
                        val (nextStart, _) = WaadCalculator.resolveLessonTimes(lessonNum + 1, nextSlot)
                        val breakMinutes = WaadCalculator.calculateBreakMinutes(currentEnd, nextStart) ?: 0

                        RecessBreakBanner(
                            fromLesson = lessonNum,
                            toLesson = lessonNum + 1,
                            breakStart = currentEnd,
                            breakEnd = nextStart,
                            breakMinutes = breakMinutes,
                            darkMode = state.darkMode,
                            onClickAdjustBreak = {
                                editingTarget = EditingSlotTarget(day = selectedDay, lessonNum = lessonNum + 1)
                            }
                        )
                    }
                }
            }
        } else {
            // 4. Weekly Matrix Overview (الشبكة الأسبوعية الشاملة)
            WeeklyScheduleMatrixCard(
                state = state,
                liveDay = liveLesson.dayName,
                liveLessonNum = liveLesson.lessonNum,
                onSelectCell = { day, lessonNum ->
                    selectedDay = day
                    editingTarget = EditingSlotTarget(day = day, lessonNum = lessonNum)
                }
            )
        }
    }

    // 5. Interactive Lesson Slot Editor Modal
    val activeTarget = editingTarget
    if (activeTarget != null) {
        val currentSlot = state.teacherSchedule["${activeTarget.day}_${activeTarget.lessonNum}"]
        LessonSlotEditorModal(
            day = activeTarget.day,
            lessonNum = activeTarget.lessonNum,
            initialSlot = currentSlot,
            state = state,
            onDismiss = { editingTarget = null },
            onSave = { updatedSlot ->
                onUpdateSlot(activeTarget.day, activeTarget.lessonNum, updatedSlot)
                editingTarget = null
            },
            onClear = {
                onUpdateSlot(
                    activeTarget.day,
                    activeTarget.lessonNum,
                    ScheduledClass(subject = "")
                )
                editingTarget = null
            }
        )
    }

    // 6. Copy Day Schedule Modal
    if (showCopyDayDialog) {
        CopyDayScheduleModal(
            sourceDay = selectedDay,
            darkMode = state.darkMode,
            onDismiss = { showCopyDayDialog = false },
            onCopyToDay = { targetDay ->
                onCopyDaySchedule(selectedDay, targetDay)
                selectedDay = targetDay
                showCopyDayDialog = false
            }
        )
    }
}

@Composable
private fun ScheduleModeTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    darkMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) Indigo600 else Color.Transparent
    val textColor = when {
        selected -> Color.White
        darkMode -> Slate300
        else -> Slate600
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Amber400 else textColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = textColor
            )
        }
    }
}

@Composable
private fun RecessBreakBanner(
    fromLesson: Int,
    toLesson: Int,
    breakStart: String,
    breakEnd: String,
    breakMinutes: Int,
    darkMode: Boolean,
    onClickAdjustBreak: () -> Unit = {}
) {
    val isMajorRecess = breakMinutes >= 15
    val isOverlap = breakMinutes < 0
    val isZeroGap = breakMinutes == 0

    val bgCol = when {
        isOverlap -> Rose500.copy(alpha = if (darkMode) 0.16f else 0.1f)
        isMajorRecess -> if (darkMode) Amber500.copy(alpha = 0.14f) else Color(0xFFFFFBEB)
        isZeroGap -> if (darkMode) Slate800.copy(alpha = 0.6f) else Slate100
        else -> if (darkMode) Indigo500.copy(alpha = 0.12f) else Color(0xFFEEF2FF)
    }
    val borderCol = when {
        isOverlap -> Rose500.copy(alpha = 0.45f)
        isMajorRecess -> Amber500.copy(alpha = 0.45f)
        isZeroGap -> if (darkMode) Slate700 else Slate200
        else -> Indigo500.copy(alpha = 0.35f)
    }
    val titleColor = when {
        isOverlap -> Rose500
        isMajorRecess -> if (darkMode) Amber400 else Color(0xFFB45309)
        isZeroGap -> Slate400
        else -> if (darkMode) Indigo400 else Indigo600
    }
    val badgeColor = when {
        isOverlap -> Rose500
        isMajorRecess -> if (darkMode) Slate200 else Color(0xFF92400E)
        isZeroGap -> Slate400
        else -> if (darkMode) Slate300 else Indigo900
    }
    val iconEmoji = when {
        isOverlap -> "⚠️"
        isMajorRecess -> "☕"
        isZeroGap -> "⚡"
        else -> "⏱️"
    }
    val labelText = when {
        isOverlap -> "تداخل زمني بين الحصة $fromLesson والحصة $toLesson"
        isMajorRecess -> "الفرصة الكبرى واستراحة المدرس (بين $fromLesson و $toLesson)"
        isZeroGap -> "انتقال مباشر بدون فاصل زمني (بين $fromLesson و $toLesson)"
        else -> "فرصة استراحة بين الحصة $fromLesson والحصة $toLesson"
    }
    val durationText = when {
        isOverlap -> "$breakStart ↔ $breakEnd (تداخل ${-breakMinutes} د)"
        isZeroGap -> "$breakStart (0 د)"
        else -> "$breakStart - $breakEnd ($breakMinutes د)"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgCol)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .clickable { onClickAdjustBreak() }
            .padding(horizontal = 12.dp, vertical = if (isMajorRecess) 8.dp else 6.dp)
            .testTag("break_banner_${fromLesson}_$toLesson")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = iconEmoji, fontSize = if (isMajorRecess) 15.sp else 13.sp)
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = if (isMajorRecess) 12.sp else 11.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = durationText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                ),
                color = badgeColor
            )
        }
    }
}

@Composable
private fun InnovativeLessonTimelineCard(
    day: String,
    lessonNum: Int,
    slot: ScheduledClass?,
    isLiveSlot: Boolean,
    state: WaadUiState,
    onEditSlot: () -> Unit,
    onToggleSlotNotify: () -> Unit,
    onTriggerAlertNow: () -> Unit,
    onEnterClassList: () -> Unit,
    onEnterClassAttendance: () -> Unit
) {
    val ordinalName = WaadCalculator.getLessonOrdinalName(lessonNum)
    val (startTime, endTime) = WaadCalculator.resolveLessonTimes(lessonNum, slot)
    val isAssigned = slot != null && slot.subject.isNotBlank()

    val accentPalette = listOf(
        Indigo500,
        Emerald600,
        Purple600,
        Blue500,
        Amber500,
        Rose600
    )
    val accentColor = accentPalette[(lessonNum - 1) % accentPalette.size]

    val cardBg = if (state.darkMode) Slate800 else Color.White
    val borderCol = when {
        isLiveSlot -> Emerald500
        isAssigned -> accentColor.copy(alpha = 0.45f)
        state.darkMode -> Slate700
        else -> Slate200
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline Number Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isAssigned) accentColor else (if (state.darkMode) Slate700 else Slate200)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lessonNum.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = if (isAssigned) Color.White else Slate500
                )
            }
            Text(
                text = startTime,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                ),
                color = if (isAssigned) accentColor else Slate400
            )
            Text(
                text = endTime,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Slate400
            )
        }

        // Main Lesson Card
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable { onEditSlot() }
                .testTag("lesson_card_${day}_$lessonNum"),
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(if (isLiveSlot) 2.dp else 1.dp, borderCol),
            shadowElevation = if (isAssigned) 2.dp else 0.dp
        ) {
            if (slot != null && slot.subject.isNotBlank()) {
                val dept = state.settings.structure.getOrElse(slot.deptIdx) { state.currentDept }
                val stage = dept.stages.getOrElse(slot.stageIdx) { state.currentStage }
                val sectionStudentsCount = state.students.count {
                    it.deptName == dept.name &&
                        it.stageName == stage.name &&
                        it.section == slot.section
                }

                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Row: Lesson Ordinal Name + Live Badge + Notification Toggle + Edit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.14f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = ordinalName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = accentColor
                                )
                            }

                            if (isLiveSlot) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Emerald500)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "🔴 جارية الآن",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Per-Lesson Notification Toggle Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (slot.notifyEnabled) Emerald500.copy(alpha = 0.14f)
                                        else (if (state.darkMode) Slate700 else Slate100)
                                    )
                                    .clickable { onToggleSlotNotify() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("lesson_notify_toggle_${day}_$lessonNum")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (slot.notifyEnabled) {
                                            Icons.Default.NotificationsActive
                                        } else {
                                            Icons.Default.NotificationsNone
                                        },
                                        contentDescription = "تبديل تنبيه الحصة",
                                        tint = if (slot.notifyEnabled) Emerald600 else Slate400,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (slot.notifyEnabled) "مفعّل" else "صامت",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = if (slot.notifyEnabled) Emerald600 else Slate400
                                    )
                                }
                            }

                            // Instant Alert Trigger Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Amber500.copy(alpha = 0.16f))
                                    .clickable { onTriggerAlertNow() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("lesson_instant_alert_${day}_$lessonNum")
                            ) {
                                Text(
                                    text = "🔔 تنبيه",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = Amber500
                                )
                            }
                        }
                    }

                    // Prominent Lesson Name & Subject Banner (عرض اسم الحصة بوضوح)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (state.darkMode) Slate900.copy(alpha = 0.65f) else Slate50
                            )
                            .border(
                                1.dp,
                                if (state.darkMode) Slate700 else Slate200,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "اسم الحصة: ${slot.subject}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (state.darkMode) Color.White else Slate900
                                )
                            }

                            if (slot.lessonTitle.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Amber500,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "موضوع الحصة: ${slot.lessonTitle}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (state.darkMode) Amber400 else Indigo600
                                    )
                                }
                            } else {
                                Text(
                                    text = "اضغط لإضافة موضوع أو عنوان تفصيلي للحصة",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Slate400
                                )
                            }

                            val secNoteKey = WaadCalculator.buildSectionCurriculumKey(
                                deptName = dept.name,
                                stageName = stage.name,
                                section = slot.section,
                                subject = slot.subject
                            )
                            val secNote = state.sectionNotes[secNoteKey]
                            if (secNote != null && (secNote.lastTopic.isNotBlank() || secNote.homework.isNotBlank())) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (state.darkMode) Slate800 else Color.White)
                                        .border(1.dp, Emerald500.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (secNote.lastTopic.isNotBlank()) {
                                            Text(
                                                text = "📖 وصلنا مع شعبة (${slot.section}): ${secNote.lastTopic}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (state.darkMode) Slate200 else Slate700
                                            )
                                        }
                                        if (secNote.homework.isNotBlank()) {
                                            Text(
                                                text = "📝 الواجب البيتي: ${secNote.homework}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                                color = Emerald600
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Stage, Department & Section Metadata Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (state.darkMode) Slate700 else Slate100)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "🏫 ${dept.name} • ${stage.name}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = if (state.darkMode) Color.White else Slate700
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Indigo500.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "شعبة (${slot.section}) • $sectionStudentsCount طالب",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = if (state.darkMode) Indigo400 else Indigo600
                            )
                        }
                    }

                    // Action Buttons Row: Enter Class Grades, Attendance, Edit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor)
                                .clickable { onEnterClassList() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "سجل الشعبة",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (state.darkMode) Slate700 else Slate100)
                                .clickable { onEnterClassAttendance() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "الغياب",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (state.darkMode) Color.White else Slate800
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (state.darkMode) Slate700 else Slate100)
                                .clickable { onEditSlot() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Indigo500,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "تعديل",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (state.darkMode) Color.White else Slate800
                                )
                            }
                        }
                    }
                }
            } else {
                // Free / Unassigned Lesson Card (حصة شاغرة)
                val durationMin = WaadCalculator.calculateLessonDurationMinutes(startTime, endTime) ?: 45
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "$ordinalName — حصة شاغرة",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = if (state.darkMode) Slate300 else Slate600
                        )
                        Text(
                            text = "التوقيت: $startTime - $endTime ($durationMin د) • اضغط لتعيين الحصة أو التوقيت",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Slate400
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Indigo500.copy(alpha = 0.14f))
                            .border(1.dp, Indigo500.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { onEditSlot() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Indigo500,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "تعيين حصة",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Indigo500
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyScheduleMatrixCard(
    state: WaadUiState,
    liveDay: String,
    liveLessonNum: Int,
    onSelectCell: (String, Int) -> Unit
) {
    val cardBg = if (state.darkMode) Slate800 else Color.White
    val cardBorder = if (state.darkMode) Slate700 else Slate200

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 الشبكة الأسبوعية للحصص (اضغط على أي حصة للتعديل)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = if (state.darkMode) Color.White else Slate900
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header Row: Day + 6 Lessons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .width(74.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Indigo900),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "اليوم / الحصة",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = Amber400
                        )
                    }

                    for (lessonNum in 1..6) {
                        val (defStart, _) = WaadCalculator.getDefaultLessonTimes(lessonNum)
                        Box(
                            modifier = Modifier
                                .width(112.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (state.darkMode) Slate700 else Slate100),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = WaadCalculator.getLessonOrdinalName(lessonNum),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = if (state.darkMode) Color.White else Slate900
                                )
                                Text(
                                    text = defStart,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = Slate400
                                )
                            }
                        }
                    }
                }

                // Rows for Sunday .. Thursday
                WaadCalculator.WEEK_DAYS.forEach { day ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val isToday = day == liveDay
                        Box(
                            modifier = Modifier
                                .width(74.dp)
                                .height(68.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isToday) Indigo600 else (if (state.darkMode) Slate700 else Slate100)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = if (isToday) Color.White else (if (state.darkMode) Color.White else Slate900),
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        }

                        for (lessonNum in 1..6) {
                            val slot = state.teacherSchedule["${day}_$lessonNum"]
                            val hasClass = slot != null && slot.subject.isNotBlank()
                            val isLiveCell = day == liveDay && lessonNum == liveLessonNum

                            val cellBg = when {
                                isLiveCell && hasClass -> Emerald500.copy(alpha = 0.18f)
                                hasClass -> Indigo500.copy(alpha = 0.12f)
                                state.darkMode -> Slate900
                                else -> Slate50
                            }
                            val cellBorder = when {
                                isLiveCell -> Emerald500
                                hasClass -> Indigo500.copy(alpha = 0.45f)
                                state.darkMode -> Slate700
                                else -> Slate200
                            }

                            Box(
                                modifier = Modifier
                                    .width(112.dp)
                                    .height(68.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cellBg)
                                    .border(1.dp, cellBorder, RoundedCornerShape(12.dp))
                                    .clickable { onSelectCell(day, lessonNum) }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (slot != null && slot.subject.isNotBlank()) {
                                    val dept = state.settings.structure.getOrElse(slot.deptIdx) { state.currentDept }
                                    val stage = dept.stages.getOrElse(slot.stageIdx) { state.currentStage }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = slot.subject,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                            color = if (state.darkMode) Amber400 else Indigo600,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (slot.lessonTitle.isNotBlank()) {
                                            Text(
                                                text = slot.lessonTitle,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = if (state.darkMode) Color.White else Slate700,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${stage.name} (${slot.section})",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Slate400,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "+ شاغرة",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Slate400
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
private fun LessonSlotEditorModal(
    day: String,
    lessonNum: Int,
    initialSlot: ScheduledClass?,
    state: WaadUiState,
    onDismiss: () -> Unit,
    onSave: (ScheduledClass) -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val safeStruct = state.settings.structure
    val (defStart, defEnd) = WaadCalculator.getDefaultLessonTimes(lessonNum)

    val initDeptIdx = (initialSlot?.deptIdx ?: state.selDeptIdx).coerceIn(0, (safeStruct.size - 1).coerceAtLeast(0))
    val initDept = safeStruct.getOrNull(initDeptIdx)
    val initStageIdx = (initialSlot?.stageIdx ?: state.selStageIdx).coerceIn(
        0,
        ((initDept?.stages?.size ?: 1) - 1).coerceAtLeast(0)
    )
    val initStage = initDept?.stages?.getOrNull(initStageIdx)

    var selDeptIdx by remember { mutableIntStateOf(initDeptIdx) }
    var selStageIdx by remember { mutableIntStateOf(initStageIdx) }
    var selSubject by remember {
        mutableStateOf(
            initialSlot?.subject?.takeIf { it.isNotBlank() }
                ?: initStage?.subjects?.firstOrNull()
                ?: ""
        )
    }
    var selSection by remember {
        mutableStateOf(
            initialSlot?.section?.takeIf { it.isNotBlank() }
                ?: initStage?.sections?.firstOrNull()
                ?: "أ"
        )
    }
    var lessonTitle by remember { mutableStateOf(initialSlot?.lessonTitle ?: "") }
    var startTime by remember { mutableStateOf(initialSlot?.startTime?.takeIf { it.isNotBlank() } ?: defStart) }
    var endTime by remember { mutableStateOf(initialSlot?.endTime?.takeIf { it.isNotBlank() } ?: defEnd) }
    var notifyEnabled by remember { mutableStateOf(initialSlot?.notifyEnabled ?: true) }

    val activeDept = safeStruct.getOrElse(selDeptIdx) { safeStruct.firstOrNull() }
    val activeStages = activeDept?.stages ?: emptyList()
    val activeStage = activeStages.getOrElse(selStageIdx) {
        activeStages.firstOrNull() ?: StageConfig(name = "عام", subjects = listOf("عام"), sections = listOf("أ"))
    }

    fun pickTime(current: String, onPicked: (String) -> Unit) {
        val parts = current.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                onPicked(String.format(Locale.US, "%02d:%02d", hourOfDay, minute))
            },
            h,
            m,
            true
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(26.dp),
            color = if (state.darkMode) Slate800 else Color.White,
            border = BorderStroke(1.dp, if (state.darkMode) Slate700 else Slate200)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تخصيص ${WaadCalculator.getLessonOrdinalName(lessonNum)} — يوم $day",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = if (state.darkMode) Color.White else Slate900
                        )
                        Text(
                            text = "حدد المادة، الصف، اسم/موضوع الحصة، وتوقيت المنبّه",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Slate400
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (state.darkMode) Slate700 else Slate100)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 1. Custom Lesson Name / Topic Input (اسم / عنوان الحصة)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "✨ اسم الحصة / موضوع الدرس (يظهر في واجهة الحصص والإشعار):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = if (state.darkMode) Amber400 else Indigo600
                    )
                    OutlinedTextField(
                        value = lessonTitle,
                        onValueChange = { lessonTitle = it },
                        placeholder = {
                            Text(
                                text = "مثال: الكسور العشرية، مراجعة الفصل الأول، اختبار يومي...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lesson_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = if (state.darkMode) Slate600 else Slate300,
                            focusedTextColor = if (state.darkMode) Color.White else Slate900,
                            unfocusedTextColor = if (state.darkMode) Color.White else Slate900
                        )
                    )

                    // Quick Preset Chips for Lesson Title
                    val quickTitles = listOf("درس منهجي", "حل تمارين", "مراجعة شاملة", "اختبار يومي", "امتحان شهري")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickTitles.forEach { preset ->
                            val isMatch = lessonTitle == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isMatch) Indigo500 else (if (state.darkMode) Slate700 else Slate100)
                                    )
                                    .clickable { lessonTitle = preset }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isMatch) Color.White else Slate400
                                )
                            }
                        }
                    }
                }

                // 2. Department Selector
                if (safeStruct.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "🏫 المرحلة الدراسية:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = if (state.darkMode) Color.White else Slate800
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            safeStruct.forEachIndexed { idx, dept ->
                                val selected = idx == selDeptIdx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (selected) Indigo600 else (if (state.darkMode) Slate700 else Slate100)
                                        )
                                        .clickable {
                                            selDeptIdx = idx
                                            selStageIdx = 0
                                            val firstStg = dept.stages.firstOrNull()
                                            selSubject = firstStg?.subjects?.firstOrNull() ?: ""
                                            selSection = firstStg?.sections?.firstOrNull() ?: "أ"
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dept.name,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = if (selected) Color.White else Slate400
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Stage Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "📚 الصف الدراسي:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = if (state.darkMode) Color.White else Slate800
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeStages.forEachIndexed { idx, stg ->
                            val selected = idx == selStageIdx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) Purple600 else (if (state.darkMode) Slate700 else Slate100)
                                    )
                                    .clickable {
                                        selStageIdx = idx
                                        selSubject = stg.subjects.firstOrNull() ?: ""
                                        selSection = stg.sections.firstOrNull() ?: "أ"
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = stg.name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (selected) Color.White else Slate400
                                )
                            }
                        }
                    }
                }

                // 4. Subject Selector (المادة الدراسية)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "📘 المادة الدراسية (اسم الحصة الأساسي):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = if (state.darkMode) Color.White else Slate800
                    )
                    val subjects = activeStage.subjects.ifEmpty { listOf("عام") }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subjects.forEach { sub ->
                            val selected = sub == selSubject
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) Emerald600 else (if (state.darkMode) Slate700 else Slate100)
                                    )
                                    .clickable { selSubject = sub }
                                    .padding(horizontal = 16.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                    color = if (selected) Color.White else (if (state.darkMode) Color.White else Slate800)
                                )
                            }
                        }
                    }
                }

                // 5. Section Selector (الشعبة)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "👥 الشعبة الدراسية:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = if (state.darkMode) Color.White else Slate800
                    )
                    val sections = activeStage.sections.ifEmpty { listOf("أ") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sections.forEach { sec ->
                            val selected = sec == selSection
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) Blue500 else (if (state.darkMode) Slate700 else Slate100)
                                    )
                                    .clickable { selSection = sec }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "شعبة ($sec)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (selected) Color.White else Slate400
                                )
                            }
                        }
                    }
                }

                // 6. Bell Time & Notification Switch
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (state.darkMode) Slate900 else Slate50,
                    border = BorderStroke(1.dp, if (state.darkMode) Slate700 else Slate200)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Amber500,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "توقيت الحصة (بداية - نهاية):",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (state.darkMode) Color.White else Slate900
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Indigo500.copy(alpha = 0.14f))
                                    .clickable {
                                        startTime = defStart
                                        endTime = defEnd
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "الوقت الافتراضي",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = Indigo500
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (state.darkMode) Slate800 else Color.White)
                                    .border(1.dp, if (state.darkMode) Slate600 else Slate300, RoundedCornerShape(12.dp))
                                    .clickable { pickTime(startTime) { startTime = it } }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "من: $startTime",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                    color = if (state.darkMode) Color.White else Slate900
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (state.darkMode) Slate800 else Color.White)
                                    .border(1.dp, if (state.darkMode) Slate600 else Slate300, RoundedCornerShape(12.dp))
                                    .clickable { pickTime(endTime) { endTime = it } }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "إلى: $endTime",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                    color = if (state.darkMode) Color.White else Slate900
                                )
                            }
                        }

                        // Live Calculated Lesson Duration & Break Gaps (حساب وقت الفرص تلقائياً من فارق الوقت)
                        val lessonDurationMin = WaadCalculator.calculateLessonDurationMinutes(startTime, endTime) ?: 0
                        val prevLessonEnd = if (lessonNum > 1) {
                            WaadCalculator.resolveLessonTimes(
                                lessonNum - 1,
                                state.teacherSchedule["${day}_${lessonNum - 1}"]
                            ).second
                        } else null
                        val prevBreakMin = if (prevLessonEnd != null) {
                            WaadCalculator.calculateBreakMinutes(prevLessonEnd, startTime)
                        } else null
                        val nextLessonStart = if (lessonNum < 6) {
                            WaadCalculator.resolveLessonTimes(
                                lessonNum + 1,
                                state.teacherSchedule["${day}_${lessonNum + 1}"]
                            ).first
                        } else null
                        val nextBreakMin = if (nextLessonStart != null) {
                            WaadCalculator.calculateBreakMinutes(endTime, nextLessonStart)
                        } else null

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (state.darkMode) Slate800 else Color(0xFFFFFBEB)
                                )
                                .border(
                                    1.dp,
                                    Amber500.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "⏱️ مدة هذه الحصة: ${if (lessonDurationMin > 0) "$lessonDurationMin دقيقة" else "تحقق من التوقيت ⚠️"}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = if (state.darkMode) Amber400 else Color(0xFFB45309)
                                )
                                if (prevBreakMin != null && prevLessonEnd != null) {
                                    Text(
                                        text = if (prevBreakMin >= 0) {
                                            "☕ الفرصة قبل الحصة (مع الحصة ${lessonNum - 1}: $prevLessonEnd ← $startTime): $prevBreakMin دقيقة"
                                        } else {
                                            "⚠️ تداخل مع نهاية الحصة ${lessonNum - 1} ($prevLessonEnd): ${-prevBreakMin} دقيقة"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (prevBreakMin >= 0) (if (state.darkMode) Slate300 else Slate700) else Rose500
                                    )
                                }
                                if (nextBreakMin != null && nextLessonStart != null) {
                                    Text(
                                        text = if (nextBreakMin >= 0) {
                                            "☕ الفرصة بعد الحصة (مع الحصة ${lessonNum + 1}: $endTime ← $nextLessonStart): $nextBreakMin دقيقة"
                                        } else {
                                            "⚠️ تداخل مع بداية الحصة ${lessonNum + 1} ($nextLessonStart): ${-nextBreakMin} دقيقة"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (nextBreakMin >= 0) (if (state.darkMode) Slate300 else Slate700) else Rose500
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔔 تنبيه المدرس بموعد هذه الحصة",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = if (state.darkMode) Color.White else Slate900
                            )
                            Switch(
                                checked = notifyEnabled,
                                onCheckedChange = { notifyEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Emerald500
                                )
                            )
                        }
                    }
                }

                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onSave(
                                    ScheduledClass(
                                        deptIdx = selDeptIdx,
                                        stageIdx = selStageIdx,
                                        section = selSection,
                                        subject = selSubject.ifBlank { activeStage.subjects.firstOrNull() ?: "عام" },
                                        lessonTitle = lessonTitle.trim(),
                                        startTime = startTime,
                                        endTime = endTime,
                                        notifyEnabled = notifyEnabled
                                    )
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .testTag("save_lesson_slot_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "حفظ الحصة",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }

                        if (initialSlot != null && (initialSlot.subject.isNotBlank() || initialSlot.startTime != defStart || initialSlot.endTime != defEnd)) {
                            Button(
                                onClick = onClear,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("clear_lesson_slot_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تفريغ",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Save Custom Time Only while keeping slot Vacant (حصة شاغرة مع حفظ التوقيت لحساب الفرص)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (state.darkMode) Slate700 else Slate100)
                            .border(
                                1.dp,
                                if (state.darkMode) Slate600 else Slate300,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onSave(
                                    ScheduledClass(
                                        deptIdx = selDeptIdx,
                                        stageIdx = selStageIdx,
                                        section = selSection,
                                        subject = "",
                                        lessonTitle = "",
                                        startTime = startTime,
                                        endTime = endTime,
                                        notifyEnabled = false
                                    )
                                )
                            }
                            .padding(vertical = 10.dp)
                            .testTag("save_vacant_time_only_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⏱️ إبقاء الحصة شاغرة مع حفظ التوقيت ($startTime - $endTime) لحساب الفرص",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = if (state.darkMode) Amber400 else Indigo600
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CopyDayScheduleModal(
    sourceDay: String,
    darkMode: Boolean,
    onDismiss: () -> Unit,
    onCopyToDay: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = if (darkMode) Slate800 else Color.White,
            border = BorderStroke(1.dp, if (darkMode) Slate700 else Slate200)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "📋 نسخ جدول يوم $sourceDay إلى:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = if (darkMode) Color.White else Slate900
                )
                Text(
                    text = "اختر اليوم الذي تريد نسخ جميع حصص يوم $sourceDay إليه:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Slate400
                )

                WaadCalculator.WEEK_DAYS.filter { it != sourceDay }.forEach { targetDay ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (darkMode) Slate700 else Slate100)
                            .clickable { onCopyToDay(targetDay) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 يوم $targetDay",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = if (darkMode) Color.White else Slate900
                            )
                            Text(
                                text = "نسخ الآن ←",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Indigo500
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (darkMode) Slate700 else Slate200
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "إلغاء",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = if (darkMode) Color.White else Slate800
                    )
                }
            }
        }
    }
}
