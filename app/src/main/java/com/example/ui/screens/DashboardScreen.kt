package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StatsSummary
import com.example.ui.WaadUiState
import com.example.ui.components.ScopeSelectorBanner
import com.example.ui.components.WaadDropdownSelector
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber500
import com.example.ui.theme.Blue500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Indigo900
import com.example.ui.theme.Purple600
import com.example.ui.theme.Purple900
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun DashboardScreen(
    state: WaadUiState,
    stats: StatsSummary,
    filteredCount: Int,
    onSelectDept: (Int) -> Unit,
    onSelectStage: (Int) -> Unit,
    onSelectSec: (String) -> Unit,
    onSelectSub: (String) -> Unit,
    onJumpToLiveClass: () -> Unit,
    onOpenSchedule: () -> Unit,
    onTriggerLessonAlert: () -> Unit = {},
    onPickRandomStudent: () -> Unit,
    onOpenCertificates: () -> Unit,
    onStartGradingMode: () -> Unit,
    onStartPrintMode: () -> Unit,
    onExportExcel: () -> Unit
) {
    val liveLesson = state.currentLiveLesson
    val cardBg = if (state.darkMode) Slate800 else Color.White
    val cardBorder = if (state.darkMode) Slate700 else Slate200

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Live Lesson Card (الحصة الحالية وعرض اسم الحصة والتنبيهات)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.horizontalGradient(listOf(Indigo900, Purple900)))
                .border(1.dp, Indigo600.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Amber400,
                            modifier = Modifier.size(19.dp)
                        )
                        Column {
                            Text(
                                text = "الحصة الحالية (${liveLesson.dayName})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                            Text(
                                text = "⏰ التوقيت: ${liveLesson.startTime} - ${liveLesson.endTime}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Slate300
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (state.notificationSettings.enabled) Emerald500.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.12f)
                                )
                                .border(
                                    1.dp,
                                    if (state.notificationSettings.enabled) Emerald500 else Color.White.copy(alpha = 0.25f),
                                    CircleShape
                                )
                                .clickable { onTriggerLessonAlert() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("live_lesson_alert_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.notificationSettings.enabled) {
                                        Icons.Default.NotificationsActive
                                    } else {
                                        Icons.Default.NotificationsOff
                                    },
                                    contentDescription = "تنبيه الحصة",
                                    tint = if (state.notificationSettings.enabled) Color(0xFF6EE7B7) else Slate300,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "تنبيه",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Amber400)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = liveLesson.lessonOrdinalName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Slate900
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )

                val scheduled = liveLesson.scheduledClass
                if (scheduled != null && scheduled.subject.isNotBlank()) {
                    val schedDept = state.settings.structure.getOrElse(scheduled.deptIdx) { state.currentDept }
                    val schedStage = schedDept.stages.getOrElse(scheduled.stageIdx) { state.currentStage }
                    val secNoteKey = com.example.model.WaadCalculator.buildSectionCurriculumKey(
                        deptName = schedDept.name,
                        stageName = schedStage.name,
                        section = scheduled.section,
                        subject = scheduled.subject
                    )
                    val secNote = state.sectionNotes[secNoteKey]

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "اسم الحصة: ${liveLesson.lessonOrdinalName} — ${scheduled.subject}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFFFDE047)
                            )
                            if (scheduled.lessonTitle.isNotBlank()) {
                                Text(
                                    text = "📌 موضوع الحصة: ${scheduled.lessonTitle}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "🏫 ${schedDept.name} • ${schedStage.name} • شعبة (${scheduled.section})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Slate300
                            )
                            if (secNote != null && secNote.lastTopic.isNotBlank()) {
                                Text(
                                    text = "📖 وصلنا إلى: ${secNote.lastTopic}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFA5B4FC)
                                )
                            }
                            if (secNote != null && secNote.homework.isNotBlank()) {
                                Text(
                                    text = "📝 واجب الشعبة: ${secNote.homework}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = onJumpToLiveClass,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                modifier = Modifier.testTag("live_class_enter_button")
                            ) {
                                Text(
                                    text = "دخول الحصة 🚀",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .clickable { onOpenSchedule() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "جدول الحصص 📅",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${liveLesson.lessonOrdinalName} — غير معيّنة حالياً",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFFFDE047)
                            )
                            Text(
                                text = "اضغط على إعداد لتعيين اسم الحصة والصف وتفعيل التنبيه.",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Slate300
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable { onOpenSchedule() }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("live_class_setup_button")
                        ) {
                            Text(
                                text = "إعداد الجدول ⚙️",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // 2. Scope Selector Banner
        ScopeSelectorBanner(
            state = state,
            filteredCount = filteredCount,
            onSelectDept = onSelectDept,
            onSelectStage = onSelectStage
        )

        // 3. Stats Grid (2x2)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    valueText = stats.total.toString(),
                    labelText = state.currentStage.name,
                    icon = Icons.Default.Groups,
                    valueColor = if (state.darkMode) Color.White else Slate900,
                    accentColor = Indigo500,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    valueText = "${stats.rate}%",
                    labelText = stats.labelText,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    valueColor = Emerald500,
                    accentColor = Emerald500,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    valueText = stats.passed.toString(),
                    labelText = "الناجحون",
                    icon = Icons.Default.CheckCircle,
                    valueColor = Blue500,
                    accentColor = Blue500,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    valueText = stats.failed.toString(),
                    labelText = "المكملون / الراسبون",
                    icon = Icons.Default.Warning,
                    valueColor = Rose500,
                    accentColor = Rose500,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Random Student Dice & Top Students Certificates
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Purple600, Indigo600)))
                    .clickable { onPickRandomStudent() }
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .testTag("random_dice_button")
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
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "قرعة عشوائية",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Amber500, Color(0xFFEA580C))))
                    .clickable { onOpenCertificates() }
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .testTag("top_certificates_button")
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
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "شهادات الأوائل",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 5. Grading & Reports Card (رصد الدرجات والتقارير)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "رصد الدرجات والتقارير",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = if (state.darkMode) Color.White else Slate900
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(cardBorder)
                )

                val secOptions = listOf("" to "كل الشعب") + state.stageSections.map { it to "شعبة ($it)" }
                val subOptions = listOf("" to "اختر المادة...") + state.currentStage.subjects.map { it to it }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WaadDropdownSelector(
                        selectedLabel = if (state.globalSec.isEmpty()) "كل الشعب" else "شعبة (${state.globalSec})",
                        options = secOptions,
                        onSelect = onSelectSec,
                        modifier = Modifier.weight(1f),
                        containerColor = if (state.darkMode) Slate700 else Slate50,
                        borderColor = if (state.darkMode) Slate600 else Slate200,
                        textColor = if (state.darkMode) Color.White else Slate900,
                        height = 42.dp
                    )
                    WaadDropdownSelector(
                        selectedLabel = state.globalSub.ifEmpty { "اختر المادة..." },
                        options = subOptions,
                        onSelect = onSelectSub,
                        modifier = Modifier.weight(1f),
                        containerColor = if (state.darkMode) Slate700 else Slate50,
                        borderColor = if (state.darkMode) Slate600 else Slate200,
                        textColor = if (state.darkMode) Color.White else Slate900,
                        height = 42.dp
                    )
                }

                val hasSubject = state.globalSub.isNotBlank() || state.currentStage.subjects.isNotEmpty()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionPillButton(
                        text = "رصد",
                        icon = Icons.Default.Edit,
                        bgColor = Indigo600,
                        enabled = true,
                        tag = "start_grading_button",
                        onClick = onStartGradingMode,
                        modifier = Modifier.weight(1f)
                    )
                    ActionPillButton(
                        text = "طباعة",
                        icon = Icons.Default.Print,
                        bgColor = Blue500,
                        enabled = hasSubject,
                        tag = "start_print_button",
                        onClick = onStartPrintMode,
                        modifier = Modifier.weight(1f)
                    )
                    ActionPillButton(
                        text = "إكسل",
                        icon = Icons.Default.TableChart,
                        bgColor = Emerald600,
                        enabled = hasSubject,
                        tag = "export_excel_button",
                        onClick = onExportExcel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    valueText: String,
    labelText: String,
    icon: ImageVector,
    valueColor: Color,
    accentColor: Color,
    cardBg: Color,
    cardBorder: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp
                        ),
                        color = valueColor,
                        maxLines = 1
                    )
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.75f))
            )
        }
    }
}

@Composable
private fun ActionPillButton(
    text: String,
    icon: ImageVector,
    bgColor: Color,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) bgColor else bgColor.copy(alpha = 0.4f))
            .clickable(enabled = enabled) { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
        }
    }
}
