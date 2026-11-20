package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.model.Student
import com.example.model.WaadCalculator
import com.example.ui.WaadUiState
import com.example.ui.components.ScopeSelectorBanner
import com.example.ui.components.WaadDropdownSelector
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Indigo900
import com.example.ui.theme.Purple900
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class QuickDayItem(
    val dateStr: String,
    val dayNameAr: String,
    val shortDate: String,
    val isToday: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceScreen(
    state: WaadUiState,
    filteredStudents: List<Student>,
    onSelectDept: (Int) -> Unit = {},
    onSelectStage: (Int) -> Unit = {},
    onDateChange: (String) -> Unit,
    onSelectSec: (String) -> Unit,
    onStatusChange: (String, String) -> Unit,
    onToggleIntisab: (String) -> Unit = {},
    onMarkBatchAttendance: (List<String>, String) -> Unit = { _, _ -> },
    onShareDailyAbsenceReport: (List<Student>, String) -> Unit = { _, _ -> },
    onNotifyParentAbsenceWhatsApp: (Student, String, String, Int) -> Unit = { _, _, _, _ -> },
    onOpenPrintAttendance: () -> Unit = {}
) {
    val context = LocalContext.current
    val darkMode = state.darkMode
    val cardBg = if (darkMode) Slate800 else Color.White
    val borderColor = if (darkMode) Slate700 else Slate200
    val textPrimary = if (darkMode) Color.White else Slate800
    val intisabPurple = Color(0xFF8B5CF6)

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("all") } // "all", "absent", "excused", "intisab", "warnings"
    var isRollCallMode by remember { mutableStateOf(false) }
    var rollCallIndex by remember { mutableIntStateOf(0) }

    // Generate last 7 days strip (including today)
    val last7Days = remember {
        val sdfFull = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sdfShort = SimpleDateFormat("MM/dd", Locale.US)
        val arabicDays = mapOf(
            Calendar.SUNDAY to "الأحد",
            Calendar.MONDAY to "الإثنين",
            Calendar.TUESDAY to "الثلاثاء",
            Calendar.WEDNESDAY to "الأربعاء",
            Calendar.THURSDAY to "الخميس",
            Calendar.FRIDAY to "الجمعة",
            Calendar.SATURDAY to "السبت"
        )
        (0..6).map { offset ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            val dStr = sdfFull.format(cal.time)
            val dayAr = when (offset) {
                0 -> "اليوم"
                1 -> "أمس"
                else -> arabicDays[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
            }
            QuickDayItem(
                dateStr = dStr,
                dayNameAr = dayAr,
                shortDate = sdfShort.format(cal.time),
                isToday = offset == 0
            )
        }
    }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    // Calculate daily counters for the active scope (excluding Intisab students from present/absent counts)
    val effectiveStatuses = remember(filteredStudents, state.attendance, state.attendanceDate, state.studentTags) {
        filteredStudents.associate { std ->
            std.id to WaadCalculator.resolveEffectiveAttendanceStatus(
                studentId = std.id,
                dateStr = state.attendanceDate,
                attendance = state.attendance,
                studentTags = state.studentTags
            )
        }
    }

    val presentCount = remember(effectiveStatuses) {
        effectiveStatuses.values.count { it == "present" }
    }
    val absentCount = remember(effectiveStatuses) {
        effectiveStatuses.values.count { it == "absent" }
    }
    val excusedCount = remember(effectiveStatuses) {
        effectiveStatuses.values.count { it == "excused" }
    }
    val intisabCount = remember(effectiveStatuses) {
        effectiveStatuses.values.count { it == "intisab" }
    }
    val warningStudentsCount = remember(filteredStudents, state.attendance, state.studentTags) {
        filteredStudents.count { std ->
            !WaadCalculator.isStudentIntisab(std.id, state.studentTags) &&
                state.attendance.count { (k, v) -> k.startsWith("${std.id}_") && v == "absent" } >= 3
        }
    }
    val attendanceRate = remember(filteredStudents.size, presentCount, excusedCount, intisabCount) {
        val regularTotal = (filteredStudents.size - intisabCount).coerceAtLeast(0)
        if (regularTotal == 0) 100
        else (((presentCount + excusedCount) * 100) / regularTotal).coerceIn(0, 100)
    }

    // Filtered list based on tab + search query
    val displayedStudents = remember(
        filteredStudents,
        activeFilter,
        searchQuery,
        effectiveStatuses,
        state.attendance,
        state.studentTags
    ) {
        val q = searchQuery.trim()
        filteredStudents.filter { std ->
            val matchesSearch = q.isEmpty() || std.name.contains(q, ignoreCase = true)
            if (!matchesSearch) return@filter false
            val todaySt = effectiveStatuses[std.id] ?: "present"
            val isIntisabStd = WaadCalculator.isStudentIntisab(std.id, state.studentTags) || todaySt == "intisab"
            val totAbs = if (isIntisabStd) 0 else state.attendance.count { (k, v) -> k.startsWith("${std.id}_") && v == "absent" }
            when (activeFilter) {
                "absent" -> todaySt == "absent"
                "excused" -> todaySt == "excused"
                "intisab" -> isIntisabStd
                "warnings" -> !isIntisabStd && totAbs >= 3
                else -> true
            }
        }
    }

    val safeRollIndex = if (filteredStudents.isEmpty()) 0 else rollCallIndex.coerceIn(0, filteredStudents.size - 1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_screen_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 0. Active Scope Banner Card (Dept & Stage Switcher)
        ScopeSelectorBanner(
            state = state,
            filteredCount = filteredStudents.size,
            onSelectDept = onSelectDept,
            onSelectStage = onSelectStage
        )

        // 1. Hero Live Attendance Command Center
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Indigo900, Purple900, Color(0xFF1E1B4B))
                    )
                )
                .border(1.dp, Indigo400.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Top Row: Title + Quick Share & Print Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500)
                            )
                            Text(
                                text = "منظومة الرصد الذكي للحضور والغياب",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${state.currentDept.name} • ${state.currentStage.name} (${
                                if (state.globalSec.isBlank()) "كل الشعب" else "شعبة ${state.globalSec}"
                            })",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Indigo400
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = { onShareDailyAbsenceReport(filteredStudents, state.attendanceDate) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier.testTag("share_daily_absence_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "موقف اليوم",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            onClick = onOpenPrintAttendance,
                            shape = RoundedCornerShape(12.dp),
                            color = Indigo600,
                            border = BorderStroke(1.dp, Indigo400.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("print_attendance_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "سجل الطباعة",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 5 Live Stat Cards Row (including Intisab)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AttendanceHeroStatBox(
                        title = "حاضر ✅",
                        value = "$presentCount",
                        accentColor = Emerald500,
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceHeroStatBox(
                        title = "غائب ❌",
                        value = "$absentCount",
                        accentColor = Rose500,
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceHeroStatBox(
                        title = "إجازة 🕒",
                        value = "$excusedCount",
                        accentColor = Amber400,
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceHeroStatBox(
                        title = "انتساب 🏠",
                        value = "$intisabCount",
                        accentColor = Color(0xFFC084FC),
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceHeroStatBox(
                        title = "نسبة الدوام",
                        value = "$attendanceRate%",
                        accentColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1.1f)
                    )
                }

                // Segmented Multi-Color Attendance Bar
                if (filteredStudents.isNotEmpty()) {
                    val total = filteredStudents.size.toFloat()
                    val presFrac = (presentCount / total).coerceIn(0f, 1f)
                    val excFrac = (excusedCount / total).coerceIn(0f, 1f)
                    val intisabFrac = (intisabCount / total).coerceIn(0f, 1f)
                    val absFrac = (absentCount / total).coerceIn(0f, 1f)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            if (presFrac > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(presFrac)
                                        .fillMaxHeight()
                                        .background(Emerald500)
                                )
                            }
                            if (excFrac > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(excFrac)
                                        .fillMaxHeight()
                                        .background(Amber400)
                                )
                            }
                            if (intisabFrac > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(intisabFrac)
                                        .fillMaxHeight()
                                        .background(intisabPurple)
                                )
                            }
                            if (absFrac > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(absFrac)
                                        .fillMaxHeight()
                                        .background(Rose500)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📅 التاريخ الفعال: ${state.attendanceDate}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            if (intisabCount > 0) {
                                Text(
                                    text = "🏠 $intisabCount طالب انتساب (معفي من الدروس اليومية)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFFD8B4FE)
                                )
                            } else if (warningStudentsCount > 0) {
                                Text(
                                    text = "🚨 $warningStudentsCount طلاب لديهم إنذارات غياب",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = Amber400
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Quick 7-Day Timeline Strip + Custom Date Picker
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 2.dp
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
                        text = "🗓️ شريط التنقل السريع بين الأيام:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = if (darkMode) Indigo400 else Indigo600
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (state.attendanceDate != todayStr) {
                            Surface(
                                onClick = { onDateChange(todayStr) },
                                shape = RoundedCornerShape(10.dp),
                                color = Emerald500.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "العودة لليوم ↩️",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = Emerald500,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                val cal = Calendar.getInstance()
                                val parts = state.attendanceDate.split("-")
                                val initYear = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
                                val initMonth = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
                                val initDay = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)

                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val formatted = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                        onDateChange(formatted)
                                    },
                                    initYear,
                                    initMonth,
                                    initDay
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (darkMode) Slate700 else Slate100,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier.testTag("attendance_date_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Indigo500,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "اختر تاريخاً آخر",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = textPrimary
                                )
                            }
                        }
                    }
                }

                // 7-Day Horizontal Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    last7Days.forEach { dayItem ->
                        val isSelected = state.attendanceDate == dayItem.dateStr
                        val dayAbsents = filteredStudents.count {
                            state.attendance["${it.id}_${dayItem.dateStr}"] == "absent"
                        }
                        Surface(
                            onClick = { onDateChange(dayItem.dateStr) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Indigo600 else (if (darkMode) Slate900 else Slate50),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Indigo400 else borderColor
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(66.dp)
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = dayItem.dayNameAr,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = if (isSelected) Color.White else textPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = dayItem.shortDate,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else Slate400
                                )
                                if (dayAbsents > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) Color.White.copy(alpha = 0.22f)
                                                else Rose500.copy(alpha = 0.14f)
                                            )
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "$dayAbsents غياب",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp
                                            ),
                                            color = if (isSelected) Color.White else Rose500
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "✓",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        ),
                                        color = if (isSelected) Emerald500 else Slate400
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Smart Batch Actions & Fast Roll-Call Radar Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fast Roll-Call Mode Button
            Surface(
                onClick = {
                    isRollCallMode = !isRollCallMode
                    if (isRollCallMode) rollCallIndex = 0
                },
                modifier = Modifier
                    .weight(1.15f)
                    .height(48.dp)
                    .testTag("toggle_roll_call_mode_btn"),
                shape = RoundedCornerShape(16.dp),
                color = if (isRollCallMode) Amber500 else Indigo600,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRollCallMode) "إيقاف المناداة السريعة ✕" else "⚡ وضع المناداة السريع",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }

            // Mark All Present One-Tap Button
            Surface(
                onClick = {
                    onMarkBatchAttendance(filteredStudents.map { it.id }, "present")
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("mark_all_present_btn"),
                shape = RoundedCornerShape(16.dp),
                color = Emerald500,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "الكل حاضر ✅",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }

        // 4. Interactive Roll-Call Flashcard Deck (when Roll-Call Mode is active)
        AnimatedVisibility(
            visible = isRollCallMode && filteredStudents.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val currentStd = filteredStudents.getOrNull(safeRollIndex)
            if (currentStd != null) {
                val curStatus = effectiveStatuses[currentStd.id] ?: "present"
                val isIntisabStd = WaadCalculator.isStudentIntisab(currentStd.id, state.studentTags) || curStatus == "intisab"
                val totAbs = if (isIntisabStd) 0 else state.attendance.count { (k, v) -> k.startsWith("${currentStd.id}_") && v == "absent" }
                val progress = (safeRollIndex + 1).toFloat() / filteredStudents.size.coerceAtLeast(1)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roll_call_card"),
                    shape = RoundedCornerShape(24.dp),
                    color = if (darkMode) Slate900 else Color(0xFFEEF2FF),
                    border = BorderStroke(2.dp, if (isIntisabStd) intisabPurple else Indigo500),
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (safeRollIndex > 0) rollCallIndex = safeRollIndex - 1 },
                                enabled = safeRollIndex > 0
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "الطالب السابق",
                                    tint = if (safeRollIndex > 0) Indigo500 else Slate400
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🎙️ المناداة السريعة (${safeRollIndex + 1} من ${filteredStudents.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = Indigo600
                                )
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .width(140.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = Indigo600,
                                    trackColor = Indigo400.copy(alpha = 0.25f)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (safeRollIndex < filteredStudents.size - 1) {
                                        rollCallIndex = safeRollIndex + 1
                                    }
                                },
                                enabled = safeRollIndex < filteredStudents.size - 1
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "الطالب التالي",
                                    tint = if (safeRollIndex < filteredStudents.size - 1) Indigo500 else Slate400
                                )
                            }
                        }

                        // Student Name & Info
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentStd.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Indigo600.copy(alpha = 0.14f))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "شعبة (${currentStd.section})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = Indigo600
                                    )
                                }
                                if (isIntisabStd) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(intisabPurple.copy(alpha = 0.16f))
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "🏠 طالب انتساب (معفي من الدروس — امتحانات فقط)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = intisabPurple
                                        )
                                    }
                                } else if (totAbs > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Rose500.copy(alpha = 0.14f))
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "إجمالي الغياب: $totAbs يوم",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = Rose600
                                        )
                                    }
                                }
                            }
                        }

                        // 4 Roll-Call Action Buttons with Auto-Advance (حاضر / غائب / إجازة / انتساب)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val advanceNext = {
                                if (safeRollIndex < filteredStudents.size - 1) {
                                    rollCallIndex = safeRollIndex + 1
                                } else {
                                    isRollCallMode = false
                                }
                            }

                            Button(
                                onClick = {
                                    onStatusChange(currentStd.id, "present")
                                    advanceNext()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (curStatus == "present") Emerald500 else Emerald500.copy(alpha = 0.75f)
                                )
                            ) {
                                Text(
                                    text = if (isIntisabStd) "حاضر امتحان ✅" else "حاضر ✅",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = {
                                    onStatusChange(currentStd.id, "absent")
                                    advanceNext()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (curStatus == "absent") Rose600 else Rose500.copy(alpha = 0.8f)
                                )
                            ) {
                                Text(
                                    text = "غائب ❌",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = {
                                    onStatusChange(currentStd.id, "excused")
                                    advanceNext()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (curStatus == "excused") Amber500 else Amber500.copy(alpha = 0.8f)
                                )
                            ) {
                                Text(
                                    text = "إجازة 🕒",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = {
                                    onStatusChange(currentStd.id, "intisab")
                                    advanceNext()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (curStatus == "intisab") intisabPurple else intisabPurple.copy(alpha = 0.8f)
                                )
                            ) {
                                Text(
                                    text = "انتساب 🏠",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Section Filter, Search Bar & Smart Filter Tabs
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Section Selector
                    Box(modifier = Modifier.weight(0.95f)) {
                        val secOptions = listOf("" to "كل الشعب (${state.stageSections.joinToString("،")})") +
                            state.stageSections.map { it to "شعبة ($it)" }
                        val selectedIdx = secOptions.indexOfFirst { it.first == state.globalSec }.coerceAtLeast(0)

                        WaadDropdownSelector(
                            selectedLabel = secOptions[selectedIdx].second,
                            options = secOptions,
                            onSelect = onSelectSec,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("attendance_sec_selector"),
                            containerColor = if (darkMode) Slate900 else Slate50,
                            borderColor = borderColor,
                            textColor = textPrimary,
                            height = 50.dp
                        )
                    }

                    // Instant Search Box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "ابحث عن طالب...",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "مسح",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.15f)
                            .height(52.dp)
                            .testTag("attendance_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = borderColor
                        )
                    )
                }

                // Filter Tabs Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filters = listOf(
                        Triple("all", "الكل (${filteredStudents.size})", Indigo600),
                        Triple("absent", "❌ غائب اليوم ($absentCount)", Rose600),
                        Triple("excused", "🕒 إجازة ($excusedCount)", Amber500),
                        Triple("intisab", "🏠 طلاب الانتساب ($intisabCount)", intisabPurple),
                        Triple("warnings", "🚨 إنذارات الغياب ($warningStudentsCount)", Color(0xFFB91C1C))
                    )
                    filters.forEach { (fKey, fLabel, fColor) ->
                        val selected = activeFilter == fKey
                        Surface(
                            onClick = { activeFilter = fKey },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) fColor else (if (darkMode) Slate900 else Slate100),
                            border = BorderStroke(1.dp, if (selected) fColor else borderColor),
                            modifier = Modifier.testTag("attendance_filter_$fKey")
                        ) {
                            Text(
                                text = fLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = if (selected) Color.White else textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. Student Attendance Cards List
        if (displayedStudents.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when (activeFilter) {
                            "absent" -> "🎉 لا يوجد أي طالب غائب في هذا اليوم!"
                            "intisab" -> "🏠 لا يوجد طلاب انتساب مسجلون في هذه الشعبة حالياً (يمكنك الضغط على زر [انتساب 🏠] أمام أي طالب لتحديده)"
                            "warnings" -> "✅ لا يوجد طلاب متجاوزون لحد الإنذار في هذه الشعبة"
                            else -> "لا يوجد طلاب مطابقون للبحث في هذا النطاق"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            displayedStudents.forEachIndexed { idx, std ->
                val curStatus = effectiveStatuses[std.id] ?: "present"
                val isTaggedIntisab = WaadCalculator.isStudentIntisab(std.id, state.studentTags)
                val isIntisabEffective = isTaggedIntisab || curStatus == "intisab"
                val stdRecords = state.attendance.filterKeys { it.startsWith("${std.id}_") }
                val absentsCount = if (isIntisabEffective) 0 else stdRecords.values.count { it == "absent" }
                val excusedTotal = stdRecords.values.count { it == "excused" }
                val presentTotal = stdRecords.values.count { it == "present" }

                // Iraqi Ministry Warning Level Calculation (Intisab students are exempt from daily lesson warnings)
                val (warnLabel, warnBg, warnTextCol) = if (isIntisabEffective) {
                    Triple(
                        "🏠 طالب انتساب (امتحانات فقط)",
                        intisabPurple.copy(alpha = if (darkMode) 0.22f else 0.14f),
                        if (darkMode) Color(0xFFD8B4FE) else Color(0xFF6D28D9)
                    )
                } else {
                    when {
                        absentsCount >= 21 -> Triple("⛔ فصل بالغياب ($absentsCount غياب)", Color(0xFFFEE2E2), Color(0xFF991B1B))
                        absentsCount >= 7 -> Triple("⛔ إنذار نهائي ($absentsCount غياب)", Color(0xFFFEE2E2), Rose600)
                        absentsCount >= 5 -> Triple("🚨 إنذار ثاني ($absentsCount غياب)", Color(0xFFFFE4E6), Rose600)
                        absentsCount >= 3 -> Triple("⚠️ إنذار أول ($absentsCount غياب)", Color(0xFFFEF3C7), Color(0xFFB45309))
                        absentsCount >= 1 -> Triple("📌 غياب: $absentsCount يوم", if (darkMode) Slate700 else Slate100, if (darkMode) Amber400 else Slate700)
                        else -> Triple("🌟 التزام مثالي", Emerald500.copy(alpha = 0.12f), Emerald500)
                    }
                }

                val cardBorderColor = when {
                    curStatus == "intisab" || isTaggedIntisab -> intisabPurple.copy(alpha = 0.65f)
                    curStatus == "absent" -> Rose500.copy(alpha = 0.65f)
                    curStatus == "excused" -> Amber500.copy(alpha = 0.65f)
                    absentsCount >= 5 -> Rose500.copy(alpha = 0.4f)
                    else -> borderColor
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attendance_card_${std.id}"),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.5.dp, cardBorderColor),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top Row: Index + Student Name + Section + Ministry Warning / Intisab Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (curStatus) {
                                                "absent" -> Rose500.copy(alpha = 0.15f)
                                                "excused" -> Amber500.copy(alpha = 0.15f)
                                                "intisab" -> intisabPurple.copy(alpha = 0.18f)
                                                else -> Indigo500.copy(alpha = 0.12f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = when (curStatus) {
                                            "absent" -> Rose600
                                            "excused" -> Amber500
                                            "intisab" -> intisabPurple
                                            else -> Indigo600
                                        }
                                    )
                                }

                                Column {
                                    Text(
                                        text = std.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "شعبة (${std.section})",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Indigo500
                                        )
                                        if (isIntisabEffective) {
                                            Text(
                                                text = "• معفي من الدروس اليومية (لا يُحسب حضور ولا غياب)",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = intisabPurple
                                            )
                                        } else if (excusedTotal > 0 || presentTotal > 0) {
                                            Text(
                                                text = "• حضور: $presentTotal | إجازة: $excusedTotal",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = Slate400
                                            )
                                        }
                                    }
                                }
                            }

                            // Ministry Warning / Intisab Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(warnBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = warnLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    ),
                                    color = warnTextCol
                                )
                            }
                        }

                        // Middle Row: 7-Day Visual Attendance Fingerprint Strip + 4 Action Pills (حاضر / غائب / إجازة / انتساب)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 7-Day Mini Fingerprint Dots
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "بصمة آخر 7 أيام:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    color = Slate400
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    last7Days.forEach { dItem ->
                                        val st = WaadCalculator.resolveEffectiveAttendanceStatus(
                                            studentId = std.id,
                                            dateStr = dItem.dateStr,
                                            attendance = state.attendance,
                                            studentTags = state.studentTags
                                        )
                                        val dotColor = when (st) {
                                            "absent" -> Rose500
                                            "excused" -> Amber400
                                            "intisab" -> intisabPurple
                                            "present" -> if (state.attendance.containsKey("${std.id}_${dItem.dateStr}")) Emerald500 else (if (darkMode) Slate700 else Slate200)
                                            else -> if (darkMode) Slate700 else Slate200
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(if (dItem.dateStr == state.attendanceDate) 11.dp else 8.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                                .then(
                                                    if (dItem.dateStr == state.attendanceDate) {
                                                        Modifier.border(1.5.dp, Indigo500, CircleShape)
                                                    } else Modifier
                                                )
                                        )
                                    }
                                }
                            }

                            // 4 Status Pills (حاضر / غائب / إجازة / انتساب)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AttendancePill(
                                    label = if (isTaggedIntisab) "حاضر امتحان ✅" else "حاضر ✅",
                                    active = curStatus == "present",
                                    activeColor = Emerald500,
                                    darkMode = darkMode,
                                    onClick = { onStatusChange(std.id, "present") }
                                )
                                AttendancePill(
                                    label = "غائب ❌",
                                    active = curStatus == "absent",
                                    activeColor = Rose500,
                                    darkMode = darkMode,
                                    onClick = { onStatusChange(std.id, "absent") }
                                )
                                AttendancePill(
                                    label = "إجازة 🕒",
                                    active = curStatus == "excused",
                                    activeColor = Amber500,
                                    darkMode = darkMode,
                                    onClick = { onStatusChange(std.id, "excused") }
                                )
                                AttendancePill(
                                    label = "انتساب 🏠",
                                    active = curStatus == "intisab",
                                    activeColor = intisabPurple,
                                    darkMode = darkMode,
                                    onClick = { onStatusChange(std.id, "intisab") }
                                )
                            }
                        }

                        // Intisab helper bar (allows returning student to regular attendance with 1 tap)
                        if (isIntisabEffective) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = intisabPurple.copy(alpha = if (darkMode) 0.16f else 0.08f),
                                border = BorderStroke(1.dp, intisabPurple.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (curStatus == "present") {
                                            "✅ تم تسجيل حضور الطالب في الامتحان لهذا اليوم"
                                        } else {
                                            "🏠 طالب انتساب: لا يُسجل له حضور ولا غياب في الدروس اليومية"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp
                                        ),
                                        color = if (darkMode) Color(0xFFD8B4FE) else Color(0xFF6D28D9),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(intisabPurple.copy(alpha = 0.2f))
                                            .clickable { onToggleIntisab(std.id) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .testTag("cancel_intisab_btn_${std.id}")
                                    ) {
                                        Text(
                                            text = "إلغاء الانتساب ↩️",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.5.sp
                                            ),
                                            color = if (darkMode) Color.White else Color(0xFF5B21B6)
                                        )
                                    }
                                }
                            }
                        }

                        // Instant Parent WhatsApp Notification Row (Shown when regular student is absent/excused today or has >= 3 absences)
                        if (!isIntisabEffective && (curStatus == "absent" || curStatus == "excused" || absentsCount >= 3)) {
                            Surface(
                                onClick = {
                                    onNotifyParentAbsenceWhatsApp(
                                        std,
                                        state.attendanceDate,
                                        curStatus,
                                        absentsCount
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("whatsapp_absence_alert_${std.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (curStatus == "absent") Rose500.copy(alpha = 0.1f)
                                else Color(0xFF059669).copy(alpha = 0.1f),
                                border = BorderStroke(
                                    1.dp,
                                    if (curStatus == "absent") Rose500.copy(alpha = 0.35f)
                                    else Color(0xFF059669).copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (curStatus == "absent") {
                                            "💬 إبلاغ ولي الأمر بغياب اليوم عبر واتساب فوراً"
                                        } else if (curStatus == "excused") {
                                            "💬 إرسال تأكيد الإجازة لولي الأمر عبر واتساب"
                                        } else {
                                            "💬 إرسال تنبيه لولي الأمر بتكرار الغياب ($absentsCount أيام)"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (curStatus == "absent") Rose600 else Color(0xFF047857)
                                    )
                                    Text(
                                        text = "إرسال ⬅️",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (curStatus == "absent") Rose600 else Color(0xFF047857)
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
private fun AttendanceHeroStatBox(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = accentColor
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1
        )
    }
}

@Composable
private fun AttendancePill(
    label: String,
    active: Boolean,
    activeColor: Color,
    darkMode: Boolean,
    onClick: () -> Unit
) {
    val bg = if (active) activeColor else (if (darkMode) Slate700 else Slate50)
    val border = if (active) activeColor else (if (darkMode) Slate600 else Slate200)
    val textColor = if (active) Color.White else (if (darkMode) Slate300 else Slate600)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 10.sp
            ),
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}
