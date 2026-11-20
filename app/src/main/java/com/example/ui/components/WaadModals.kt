package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CalculatedResult
import com.example.model.Student
import com.example.model.WaadCalculator
import com.example.ui.WaadUiState
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
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
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlin.math.min

@Composable
fun PinLockScreen(
    state: WaadUiState,
    onPinInputChange: (String) -> Unit
) {
    val bg = if (state.darkMode) Slate900 else Slate50
    val cardBg = if (state.darkMode) Slate800 else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = cardBg,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Indigo600)
                )
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Indigo500.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Indigo500,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "التطبيق مقفل",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = if (state.darkMode) Color.White else Slate900
                    )
                    Text(
                        text = "يرجى إدخال رمز المرور (PIN) للوصول إلى السجلات.",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = state.pinInput,
                        onValueChange = onPinInputChange,
                        placeholder = {
                            Text(
                                text = "رمز المرور (PIN)",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = Slate400
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lock_screen_pin_input")
                    )
                    if (state.pinInput.isNotEmpty() && state.pinInput != state.savedPin) {
                        Text(
                            text = "رمز المرور غير صحيح",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Rose500
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivationModalOrLockScreen(
    state: WaadUiState,
    isFullScreenLock: Boolean,
    onActivationInputChange: (String) -> Unit,
    onActivateClick: () -> Unit,
    onContactWhatsApp: () -> Unit,
    onDismiss: () -> Unit
) {
    val content: @Composable () -> Unit = {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = if (state.darkMode) Slate800 else Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Rose500.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Rose500,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = if (isFullScreenLock) "انتهت الفترة التجريبية" else "تفعيل النسخة السنوية",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = if (state.darkMode) Color.White else Slate900
                )

                Text(
                    text = if (state.activationState.isActivated) {
                        "التطبيق مفعل حالياً — متبقي ${state.activationState.subRemainingDays} يوم في الاشتراك السنوي."
                    } else {
                        "يرجى إدخال كود التفعيل أو مراسلة الأستاذ وعد عبر واتساب لتفعيل النسخة السنوية."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Slate400,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (state.darkMode) Slate900 else Slate50)
                        .border(1.dp, if (state.darkMode) Slate700 else Slate200, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "المعرف: ${state.activationState.deviceID}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = Indigo500
                    )
                }

                Button(
                    onClick = onContactWhatsApp,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "طلب التفعيل عبر واتساب",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                }

                OutlinedTextField(
                    value = state.activationInput,
                    onValueChange = onActivationInputChange,
                    placeholder = {
                        Text(
                            text = "أدخل كود التفعيل (WAAD-XXXX-XXXX)",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = Slate400
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        textDirection = TextDirection.Ltr
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activation_code_input")
                )

                if (state.activationError.isNotEmpty()) {
                    Text(
                        text = state.activationError,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Rose500
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onActivateClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(46.dp)
                            .testTag("submit_activation_button")
                    ) {
                        Text(
                            text = "تفعيل الترخيص",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }
                    if (!isFullScreenLock) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = "إغلاق",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    if (isFullScreenLock) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (state.darkMode) Slate900 else Slate100)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    } else {
        Dialog(onDismissRequest = onDismiss) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentProfileModal(
    state: WaadUiState,
    student: Student,
    calculateResult: (String, String) -> CalculatedResult,
    onToggleTag: (String, String) -> Unit,
    onApplyQarar: (String, String) -> Unit,
    onUpdateMissingHomework: (Student, String, Int, Boolean) -> Unit = { _, _, _, _ -> },
    onOpenBonus: (Student) -> Unit,
    onOpenFullReportShare: (Student) -> Unit,
    onDismiss: () -> Unit
) {
    val cardBg = if (state.darkMode) Slate800 else Color.White
    val subBoxBg = if (state.darkMode) Slate900 else Slate50
    val borderCol = if (state.darkMode) Slate700 else Slate200

    val stdTags = state.studentTags[student.id] ?: emptyList()
    val absentsCount = state.attendance.count { (k, v) -> k.startsWith("${student.id}_") && v == "absent" }
    val presentsCount = state.attendance.count { (k, v) -> k.startsWith("${student.id}_") && v == "present" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(28.dp),
            color = cardBg,
            shadowElevation = 14.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Indigo600)
                )

                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Indigo500.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = student.name.firstOrNull()?.toString() ?: "ط",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = Indigo500
                                )
                            }
                            Column {
                                Text(
                                    text = student.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (state.darkMode) Color.White else Slate900
                                )
                                Text(
                                    text = "${student.deptName} — ${student.stageName} (شعبة ${student.section})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Slate400
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Amber500.copy(alpha = 0.2f))
                                    .clickable { onOpenBonus(student) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Amber500,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "تشجيعية",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = Amber500
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Slate400.copy(alpha = 0.15f))
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = if (state.darkMode) Color.White else Slate700,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(borderCol)
                    )

                    // Attendance & Phone Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileInfoBox(
                            label = "حضور",
                            value = "$presentsCount",
                            valueColor = Emerald500,
                            bg = subBoxBg,
                            border = borderCol,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileInfoBox(
                            label = "غياب",
                            value = "$absentsCount",
                            valueColor = Rose500,
                            bg = subBoxBg,
                            border = borderCol,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileInfoBox(
                            label = "هاتف ولي الأمر",
                            value = student.phone.ifBlank { "غير مسجل" },
                            valueColor = Indigo500,
                            bg = subBoxBg,
                            border = borderCol,
                            modifier = Modifier.weight(1.3f)
                        )
                    }

                    // Behavior Tags
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "وسوم وسلوكيات الطالب:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = Slate400
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            WaadCalculator.BEHAVIOR_TAGS.forEach { tag ->
                                val active = stdTags.contains(tag)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (active) Indigo600
                                            else (if (state.darkMode) Slate900 else Slate100)
                                        )
                                        .border(
                                            1.dp,
                                            if (active) Indigo500 else borderCol,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onToggleTag(student.id, tag) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = if (active) Color.White else (if (state.darkMode) Slate400 else Slate600)
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(borderCol)
                    )

                    // Full Gradebook for All Subjects
                    Text(
                        text = "كشف الدرجات الكامل لجميع الفصول:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = Indigo500
                    )

                    state.currentStage.subjects.forEach { sub ->
                        val res = calculateResult(student.id, sub)
                        val effectiveSaei = min(state.maxAllowedGrade, res.saei + res.qarar)
                        val finalScore = res.secondRoundResult
                            ?: if (res.hasFinalResult) res.finalResult else effectiveSaei
                        val missingHw = state.missingHomework["${student.id}_$sub"] ?: 0

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = subBoxBg,
                            border = BorderStroke(1.dp, borderCol)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📖 $sub",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                        color = Indigo400
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (res.qarar > 0) Amber500 else Color.Transparent)
                                                .border(1.dp, Amber500.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                .clickable { onApplyQarar(student.id, sub) }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (res.qarar > 0) "قرار +${res.qarar} ✓" else "+ منح قرار",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                                color = if (res.qarar > 0) Color.White else Amber500
                                            )
                                        }

                                        val isPassing = finalScore >= state.failThreshold
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (!res.hasSaei) Color.Transparent
                                                    else if (isPassing) Emerald500.copy(alpha = 0.14f)
                                                    else Rose500.copy(alpha = 0.14f)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "السعي: ${if (res.isExempt) "✨ معفي" else if (res.hasSaei) "$effectiveSaei" else "-"}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                                color = if (!res.hasSaei) Slate400 else if (isPassing) Emerald500 else Rose500
                                            )
                                        }
                                    }
                                }

                                // Row 1: Semester 1 + Mid-Year
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MiniGradeCell("ف1 ش1", if (res.hasM1) "${res.m1}" else "-", res.hasM1, state.darkMode, Modifier.weight(1f))
                                    MiniGradeCell("ف1 ش2", if (res.hasM2) "${res.m2}" else "-", res.hasM2, state.darkMode, Modifier.weight(1f))
                                    MiniGradeCell("معدل ف1", if (res.hasAvg1) "${res.avg1}" else "-", res.hasAvg1, state.darkMode, Modifier.weight(1f), Indigo400)
                                    MiniGradeCell("نصف سنة", if (res.hasMid) "${res.mid}" else "-", res.hasMid, state.darkMode, Modifier.weight(1f), Amber500)
                                }

                                // Row 2: Semester 2 + Final Result
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MiniGradeCell("ف2 ش1", if (res.hasM3) "${res.m3}" else "-", res.hasM3, state.darkMode, Modifier.weight(1f))
                                    MiniGradeCell("ف2 ش2", if (res.hasM4) "${res.m4}" else "-", res.hasM4, state.darkMode, Modifier.weight(1f))
                                    MiniGradeCell("معدل ف2", if (res.hasAvg2) "${res.avg2}" else "-", res.hasAvg2, state.darkMode, Modifier.weight(1f), Indigo400)
                                    val resultStr = if (res.hasSecondRoundResult) "${res.secondRoundResult} (د2)" else if (res.hasFinalResult) "${res.finalResult}" else "-"
                                    MiniGradeCell("النتيجة", resultStr, res.hasFinalResult || res.hasSecondRoundResult, state.darkMode, Modifier.weight(1f), Emerald500)
                                }

                                // Row 3: Missing Homework quick controls inside Student Profile
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (missingHw > 0) Rose500.copy(alpha = 0.1f)
                                            else Emerald500.copy(alpha = 0.08f)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (missingHw > 0) "⚠️ نقص واجب: $missingHw مرات" else "✅ ملتزم بالواجبات البيتية",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (missingHw > 0) Rose500 else Emerald600
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Rose600)
                                                .clickable { onUpdateMissingHomework(student, sub, 1, false) }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "+ تسجيل نقص",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black
                                                ),
                                                color = Color.White,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                        if (missingHw > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Emerald600)
                                                    .clickable { onUpdateMissingHomework(student, sub, 0, true) }
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "تصفير",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black
                                                    ),
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenFullReportShare(student)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إرسال تقرير شامل",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                            modifier = Modifier
                                .weight(0.8f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = "إغلاق",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoBox(
    label: String,
    value: String,
    valueColor: Color,
    bg: Color,
    border: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Slate400
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = valueColor
            )
        }
    }
}

@Composable
private fun MiniGradeCell(
    label: String,
    value: String,
    active: Boolean,
    darkMode: Boolean,
    modifier: Modifier = Modifier,
    highlightColor: Color? = null
) {
    val bg = if (!active) Color.Transparent
    else highlightColor?.copy(alpha = 0.1f) ?: (if (darkMode) Slate800 else Color.White)
    val border = if (!active) Slate400.copy(alpha = 0.3f)
    else highlightColor?.copy(alpha = 0.35f) ?: (if (darkMode) Slate600 else Slate300)
    val txtColor = highlightColor ?: (if (darkMode) Color.White else Slate900)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = Slate400
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
            color = if (active) txtColor else Slate400.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun BonusGradeModal(
    state: WaadUiState,
    onSubjectChange: (String) -> Unit,
    onMonthFieldChange: (String) -> Unit,
    onApplyBonus: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val modal = state.bonusModal
    val student = modal.student ?: return
    val curStudentMax = WaadCalculator.getStudentMaxGrade(
        student,
        state.settings,
        state.currentDept,
        state.currentStage
    )
    val baseVal = modal.baseGrade
    val otherSum = modal.otherFieldsSum

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = if (state.darkMode) Slate800 else Color.White,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Amber500)
                )
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⭐ منح درجة تشجيعية",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = if (state.darkMode) Color.White else Slate900
                        )
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Amber500
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val subOpts = state.currentStage.subjects.map { it to it }
                        val monthOpts = listOf(
                            "v1" to "ف1 - شهر 1",
                            "v4" to "ف1 - شهر 2",
                            "v7" to "ف2 - شهر 1",
                            "v10" to "ف2 - شهر 2"
                        )
                        val selectedMonthLabel = monthOpts.find { it.first == modal.monthField }?.second ?: "ف1 - شهر 1"

                        WaadDropdownSelector(
                            selectedLabel = modal.subject,
                            options = subOpts,
                            onSelect = onSubjectChange,
                            modifier = Modifier.weight(1f),
                            containerColor = if (state.darkMode) Slate700 else Slate50,
                            borderColor = if (state.darkMode) Slate600 else Slate200,
                            textColor = if (state.darkMode) Color.White else Slate900
                        )

                        WaadDropdownSelector(
                            selectedLabel = selectedMonthLabel,
                            options = monthOpts,
                            onSelect = onMonthFieldChange,
                            modifier = Modifier.weight(1f),
                            containerColor = if (state.darkMode) Slate700 else Slate50,
                            borderColor = if (state.darkMode) Slate600 else Slate200,
                            textColor = if (state.darkMode) Color.White else Slate900
                        )
                    }

                    // Preview Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Amber500.copy(alpha = 0.12f))
                            .border(1.dp, Amber500.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "درجة اليومي",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400
                            )
                            Text(
                                text = "$baseVal",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = if (state.darkMode) Color.White else Slate900
                            )
                        }
                        Text(
                            text = "➕",
                            style = MaterialTheme.typography.titleMedium,
                            color = Amber500
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "التشجيع",
                                style = MaterialTheme.typography.labelSmall,
                                color = Amber500
                            )
                            Text(
                                text = "+${modal.selectedBonus}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = Amber500
                            )
                        }
                    }

                    Text(
                        text = "اختر الإضافة:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = Slate400
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 5).forEach { pts ->
                            val wouldExceed = (baseVal + otherSum + pts) > curStudentMax
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (wouldExceed) Slate600.copy(alpha = 0.4f) else Emerald500)
                                    .clickable(enabled = !wouldExceed) { onApplyBonus(pts) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+$pts",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate700)
                            .clickable { onApplyBonus(0) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "إعادة للدرجة الأصلية",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate200
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = "إغلاق ✅",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsAppShareModal(
    state: WaadUiState,
    previewMessage: String,
    onPhoneChange: (String) -> Unit,
    onScopeChange: (String) -> Unit,
    onSendWhatsApp: () -> Unit,
    onDismiss: () -> Unit
) {
    val modal = state.shareModal
    val student = modal.student ?: return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = if (state.darkMode) Slate800 else Color.White,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Emerald500)
                )
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "إرسال تقرير لولي الأمر 📱",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = if (state.darkMode) Color.White else Slate900
                            )
                            Text(
                                text = "${student.name} (${student.section})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Slate400
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Emerald500.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = modal.parentPhone,
                        onValueChange = onPhoneChange,
                        placeholder = { Text("رقم هاتف ولي الأمر (07xxxxxxxxx)", color = Slate400) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            textDirection = TextDirection.Ltr
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("single" to "المادة الحالية", "all" to "كشف شامل").forEach { (scopeId, label) ->
                            val active = modal.reportScope == scopeId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) Emerald600 else Slate700)
                                    .clickable { onScopeChange(scopeId) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (state.darkMode) Slate900 else Slate50)
                            .border(1.dp, if (state.darkMode) Slate700 else Slate200, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = previewMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = if (state.darkMode) Slate300 else Slate600
                        )
                    }

                    Button(
                        onClick = onSendWhatsApp,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إرسال عبر واتساب الآن",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Text(
                            text = "إلغاء",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BulkAddModal(
    state: WaadUiState,
    onTargetSecChange: (String) -> Unit,
    onBulkTextChange: (String) -> Unit,
    onConfirmBulkAdd: () -> Unit,
    onPickPdf: () -> Unit = {},
    onPickImage: () -> Unit = {},
    onCaptureCamera: () -> Unit = {},
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = if (state.darkMode) Slate800 else Color.White,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Indigo600)
                )
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "إضافة شعبة كاملة (PDF / صورة / نص)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = Indigo400
                    )

                    SmartImportToolbar(
                        darkMode = state.darkMode,
                        onPickPdf = onPickPdf,
                        onPickImage = onPickImage,
                        onCaptureCamera = onCaptureCamera
                    )

                    val secOpts = state.stageSections.map { it to "شعبة ($it)" }
                    WaadDropdownSelector(
                        selectedLabel = "شعبة (${state.bulkTargetSec.ifBlank { state.stageSections.firstOrNull() ?: "أ" }})",
                        options = secOpts,
                        onSelect = onTargetSecChange,
                        containerColor = if (state.darkMode) Slate700 else Slate50,
                        borderColor = if (state.darkMode) Slate600 else Slate200,
                        textColor = if (state.darkMode) Color.White else Slate900
                    )

                    OutlinedTextField(
                        value = state.bulkText,
                        onValueChange = onBulkTextChange,
                        placeholder = { Text("أو الصق الأسماء هنا (كل اسم في سطر)...", color = Slate400) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("bulk_add_textarea")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("إلغاء", color = Color.White)
                        }

                        Button(
                            onClick = onConfirmBulkAdd,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                            modifier = Modifier
                                .weight(2f)
                                .height(46.dp)
                                .testTag("confirm_bulk_add_button")
                        ) {
                            Text(
                                text = "اعتماد القائمة",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RandomStudentDiceModal(
    state: WaadUiState,
    onReroll: () -> Unit,
    onDismiss: () -> Unit
) {
    val modal = state.diceModal

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = if (state.darkMode) Slate800 else Color.White,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Purple600)
                )
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Purple600.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = Purple600,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "قرعة الطالب العشوائية",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = if (state.darkMode) Color.White else Slate900
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (modal.isRolling) Purple600.copy(alpha = 0.1f)
                                else Emerald500.copy(alpha = 0.12f)
                            )
                            .border(
                                1.dp,
                                if (modal.isRolling) Purple600.copy(alpha = 0.4f)
                                else Emerald500.copy(alpha = 0.4f),
                                RoundedCornerShape(18.dp)
                            )
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = modal.selectedStudent?.name ?: "جاري الاختيار...",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = if (modal.isRolling) Purple600 else Emerald600,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onReroll,
                            enabled = !modal.isRolling,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple600),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = "إعادة القرعة 🎲",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("إغلاق", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopStudentsCertificatesModal(
    state: WaadUiState,
    filteredStudents: List<Student>,
    calculateResult: (String, String) -> CalculatedResult,
    onShareStudentReport: (Student) -> Unit,
    onDismiss: () -> Unit
) {
    val activeSub = state.globalSub.ifBlank { state.currentStage.subjects.firstOrNull() ?: "" }
    val topList = if (activeSub.isBlank()) {
        emptyList()
    } else {
        filteredStudents
            .map { s -> s to calculateResult(s.id, activeSub).saei }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(5)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = if (state.darkMode) Slate800 else Color.White,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Amber500)
                )
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Amber500,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "قائمة الأوائل والمتفوقين (${activeSub.ifBlank { "عام" }})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = Amber500
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Slate400,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onDismiss() }
                        )
                    }

                    if (topList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد درجات سعي مرصودة بعد في هذه المادة.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Slate400,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            topList.forEachIndexed { idx, (std, saei) ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDismiss()
                                            onShareStudentReport(std)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (state.darkMode) Slate900 else Color(0xFFFFFBEB),
                                    border = BorderStroke(
                                        1.dp,
                                        if (state.darkMode) Slate700 else Color(0xFFFDE68A)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(Amber500),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${idx + 1}",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                                    color = Color.White
                                                )
                                            }
                                            Text(
                                                text = std.name,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                                color = if (state.darkMode) Color.White else Slate900
                                            )
                                        }
                                        Text(
                                            text = "$saei درجة",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                            color = Amber600
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = "إغلاق",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDangerDialog(
    darkMode: Boolean,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = if (darkMode) Slate800 else Color.White,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Rose600)
                )
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Rose500.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Rose600,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Rose500
                    )

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConfirm,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = confirmText,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("إلغاء", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AcademicYearResetModal(
    darkMode: Boolean,
    totalStudentsCount: Int,
    onConfirmReset: (deleteAllStudents: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var deleteAllStudents by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = if (darkMode) Slate800 else Color.White,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Rose600)
                )
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Rose500.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Rose600,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Text(
                        text = "أرشفة وتصفير السنة الدراسية ⚠️",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Rose500,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "سيتم حفظ نسخة احتياطية كاملة أولاً، ثم تصفير السجلات لبدء عام دراسي جديد:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (darkMode) Slate300 else Slate600,
                        textAlign = TextAlign.Center
                    )

                    // Option 1: Complete Reset including deleting all student names (Default = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteAllStudents = true }
                            .testTag("reset_option_delete_all_students"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (deleteAllStudents) {
                            if (darkMode) Rose600.copy(alpha = 0.2f) else Color(0xFFFFF1F2)
                        } else {
                            if (darkMode) Slate900 else Slate50
                        },
                        border = BorderStroke(
                            1.5.dp,
                            if (deleteAllStudents) Rose500 else (if (darkMode) Slate700 else Slate200)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (deleteAllStudents) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (deleteAllStudents) Rose500 else Slate400,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تصفير شامل (حذف جميع أسماء الطلاب والدرجات)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                    color = if (deleteAllStudents) Rose500 else (if (darkMode) Color.White else Slate900)
                                )
                                Text(
                                    text = "يمسح أسماء الطلاب المسجلين ($totalStudentsCount طالب) وجميع الدرجات والغيابات لبدء عام جديد فارغ تماماً.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Slate400
                                )
                            }
                        }
                    }

                    // Option 2: Reset grades & attendance only while keeping student names
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteAllStudents = false }
                            .testTag("reset_option_keep_students"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (!deleteAllStudents) {
                            if (darkMode) Indigo600.copy(alpha = 0.2f) else Color(0xFFEEF2FF)
                        } else {
                            if (darkMode) Slate900 else Slate50
                        },
                        border = BorderStroke(
                            1.5.dp,
                            if (!deleteAllStudents) Indigo500 else (if (darkMode) Slate700 else Slate200)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (!deleteAllStudents) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (!deleteAllStudents) Indigo500 else Slate400,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تصفير الدرجات والغيابات فقط (مع بقاء الأسماء)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                    color = if (!deleteAllStudents) Indigo400 else (if (darkMode) Color.White else Slate900)
                                )
                                Text(
                                    text = "يبقي أسماء الطلاب الحالية كما هي ويمسح درجاتهم وغياباتهم فقط.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Slate400
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onConfirmReset(deleteAllStudents) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("confirm_academic_year_reset_button")
                        ) {
                            Text(
                                text = if (deleteAllStudents) "أرشفة وتصفير شامل ✅" else "أرشفة وتصفير الدرجات ✅",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(46.dp)
                        ) {
                            Text("إلغاء", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

