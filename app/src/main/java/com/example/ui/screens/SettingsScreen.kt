package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.WaadUiState
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Indigo950
import com.example.ui.theme.Purple600
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: WaadUiState,
    activeTab: String = "school",
    onSelectTab: (String) -> Unit = {},
    onPinChange: (String) -> Unit,
    onRequestArchiveAndReset: () -> Unit,
    onDownloadBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onMinistryChange: (String) -> Unit,
    onSchoolChange: (String) -> Unit,
    onTeacherChange: (String) -> Unit,
    onYearChange: (String) -> Unit,
    onSaveOfficialSettings: () -> Unit,
    onToggleDeleteMode: () -> Unit,
    onSelectDept: (Int) -> Unit,
    onSelectStage: (Int) -> Unit,
    onAddDept: (String) -> Unit,
    onAddStage: (String) -> Unit,
    onToggleStageMaxGrade: () -> Unit,
    onAddSub: (String) -> Unit,
    onAddSec: (String) -> Unit,
    onRequestDeleteStruct: (String, Int) -> Unit,
    onDriveUrlChange: (String) -> Unit = {},
    onDriveVersionChange: (String) -> Unit = {},
    onDriveNotesChange: (String) -> Unit = {},
    onToggleDriveShowOnStartup: (Boolean) -> Unit = {},
    onPasteDriveUrlFromClipboard: () -> Unit = {},
    onCopyDriveUrlToClipboard: () -> Unit = {},
    onSaveAndShowDriveUpdateNow: () -> Unit = {},
    onInstallReadyDriveApk: () -> Unit = {},
    onCheckRemoteDriveUpdateNow: () -> Unit = {},
    onExportDriveUpdateJsonFile: () -> Unit = {},
    onShareDriveUpdateWhatsApp: () -> Unit = {}
) {
    val cardBg = if (state.darkMode) Slate800 else Color.White
    val cardBorder = if (state.darkMode) Slate700 else Slate200

    var newDeptText by remember { mutableStateOf("") }
    var newStageText by remember { mutableStateOf("") }
    var newSubText by remember { mutableStateOf("") }
    var newSecText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Segmented Switcher: Separate School & Teacher Settings from App Settings
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isSchoolTab = activeTab == "school"
                val isAppTab = activeTab == "app"

                // Tab 1: معلومات المدرسة والمدرس
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSchoolTab) {
                                Brush.linearGradient(listOf(Indigo600, Purple600))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        if (state.darkMode) Slate900 else Slate50,
                                        if (state.darkMode) Slate900 else Slate50
                                    )
                                )
                            }
                        )
                        .clickable { onSelectTab("school") }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .testTag("tab_school_teacher_settings"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = if (isSchoolTab) Color.White else (if (state.darkMode) Slate300 else Slate600),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "المدرسة والمدرس",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = if (isSchoolTab) Color.White else (if (state.darkMode) Slate300 else Slate600)
                        )
                        if (state.hasUnsavedSettings) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Amber500)
                            )
                        }
                    }
                }

                // Tab 2: إعدادات التطبيق
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isAppTab) {
                                Brush.linearGradient(listOf(Indigo600, Purple600))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        if (state.darkMode) Slate900 else Slate50,
                                        if (state.darkMode) Slate900 else Slate50
                                    )
                                )
                            }
                        )
                        .clickable { onSelectTab("app") }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .testTag("tab_app_system_settings"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (isAppTab) Color.White else (if (state.darkMode) Slate300 else Slate600),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "إعدادات التطبيق",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = if (isAppTab) Color.White else (if (state.darkMode) Slate300 else Slate600)
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(160)))
            },
            label = "settings_tab_transition"
        ) { currentTab ->
            if (currentTab == "app") {
                // =====================================================
                // SECTION 1: إعدادات التطبيق والنظام (App Settings)
                // =====================================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. App Identity, Version & Icon Banner
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val installedVersionName = com.example.update.GoogleDriveUpdateManager.getInstalledVersionName(context)
                    val installedVersionCode = com.example.update.GoogleDriveUpdateManager.getInstalledVersionCode(context)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(Indigo950, Slate900)))
                            .border(1.dp, Indigo600.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(com.example.ui.components.BrandGradient)
                                        .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    com.example.ui.components.AppLogoIcon(size = 40.dp)
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "بوصلة الأستاذ — إعدادات التطبيق",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                        color = Color(0xFFA5B4FC)
                                    )
                                    Text(
                                        text = "إعدادات النظام والأمان والنسخ الاحتياطي",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = Slate400
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Emerald500.copy(alpha = 0.18f))
                                                .border(1.dp, Emerald500.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                                .testTag("settings_app_version_badge")
                                        ) {
                                            Text(
                                                text = "الإصدار الحالي: v$installedVersionName ($installedVersionCode)",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp
                                                ),
                                                color = Color(0xFF6EE7B7)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. PIN Lock Card (Standalone Card)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Emerald500.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Emerald500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "حماية وقفل التطبيق (PIN)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (state.darkMode) Color.White else Slate900
                                    )
                                    Text(
                                        text = "قفل التطبيق برمز سري عند الفتح لحماية سجلات الطلاب",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = state.savedPin,
                                onValueChange = onPinChange,
                                placeholder = {
                                    Text(
                                        text = "أدخل 4 أرقام لتفعيل القفل (أو اتركه فارغاً للإلغاء)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400
                                    )
                                },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = if (state.darkMode) Slate900 else Slate50,
                                    unfocusedContainerColor = if (state.darkMode) Slate900 else Slate50,
                                    focusedBorderColor = Indigo500,
                                    unfocusedBorderColor = if (state.darkMode) Slate600 else Slate200
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_pin_input")
                            )
                        }
                    }

                    // 3. Backup & Restore Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(Slate900, Indigo950)))
                            .border(1.dp, Slate800, RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = Indigo400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "النسخ الاحتياطي والاسترجاع",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "حفظ نسخة احتياطية شاملة لبيانات التطبيق أو استرجاعها",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.1f))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onDownloadBackup,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 50.dp)
                                        .testTag("backup_save_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "حفظ نسخة",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = onRestoreBackup,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Amber600),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 50.dp)
                                        .testTag("backup_restore_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Upload,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "استرجاع",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // 4. End Academic Year Card (Standalone Card)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Rose500.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = Rose500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "إنهاء العام الدراسي وتصفير السجلات",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (state.darkMode) Color.White else Slate900
                                    )
                                    Text(
                                        text = "حفظ نسخة احتياطية تلقائية ثم تصفير الطلاب والدرجات للعام الجديد",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400
                                    )
                                }
                            }

                            Button(
                                onClick = onRequestArchiveAndReset,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("archive_reset_button")
                            ) {
                                Text(
                                    text = "تصفير السجلات وإنهاء العام الدراسي ⚠️",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                // =====================================================
                // SECTION 2: معلومات وإعدادات المدرسة والمدرس (School & Teacher)
                // =====================================================
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (state.darkMode) Slate900 else Slate50,
                    unfocusedContainerColor = if (state.darkMode) Slate900 else Slate50,
                    focusedBorderColor = Indigo500,
                    unfocusedBorderColor = if (state.darkMode) Slate600 else Slate200,
                    focusedLabelColor = if (state.darkMode) Indigo400 else Indigo600,
                    unfocusedLabelColor = Slate400
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card 1: معلومات المدرسة والمدرس الرسمية
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(Indigo500.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = Indigo500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "معلومات المدرسة والمدرس الرسمية",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (state.darkMode) Color.White else Slate900
                                    )
                                    Text(
                                        text = "تظهر هذه المعلومات في الترويسة الرسمية وتقارير الطباعة والشهادات",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(cardBorder)
                            )

                            OutlinedTextField(
                                value = state.settingsFormMinistry,
                                onValueChange = onMinistryChange,
                                label = { Text("اسم الوزارة / المديرية") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = Indigo500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = state.settingsFormSchool,
                                onValueChange = onSchoolChange,
                                label = { Text("اسم المدرسة") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = Indigo500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = state.settingsFormTeacher,
                                    onValueChange = onTeacherChange,
                                    label = { Text("اسم المدرس") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Indigo500,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = fieldColors,
                                    modifier = Modifier.weight(1.15f)
                                )
                                OutlinedTextField(
                                    value = state.settingsFormYear,
                                    onValueChange = onYearChange,
                                    label = { Text("العام الدراسي") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = Indigo500,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = fieldColors,
                                    modifier = Modifier.weight(0.85f)
                                )
                            }

                            Button(
                                onClick = onSaveOfficialSettings,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.hasUnsavedSettings) Emerald600 else Indigo600
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("save_official_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.hasUnsavedSettings) {
                                        "حفظ التعديلات الجديدة الآن ✓"
                                    } else {
                                        "حفظ معلومات المدرسة والمدرس ✓"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Card 2: إعدادات الهيكلية الدراسية (المراحل، الصفوف، المواد، الشُعب)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Structure Header + Safe Delete Lock Toggle
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
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(Indigo500.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = null,
                                            tint = Indigo500,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "الهيكلية الدراسية",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                            color = if (state.darkMode) Color.White else Slate900
                                        )
                                        Text(
                                            text = "المراحل • الصفوف • المواد • الشُعب",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate400
                                        )
                                    }
                                }

                                val lockBg = if (state.isDeleteMode) {
                                    if (state.darkMode) Color(0xFF4C0519) else Color(0xFFFFF1F2)
                                } else {
                                    if (state.darkMode) Color(0xFF022C22) else Color(0xFFECFDF5)
                                }
                                val lockBorder = if (state.isDeleteMode) Rose600 else Emerald600
                                val lockTextTint = if (state.isDeleteMode) {
                                    if (state.darkMode) Color(0xFFFDA4AF) else Rose600
                                } else {
                                    if (state.darkMode) Color(0xFF6EE7B7) else Emerald600
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(lockBg)
                                        .border(1.dp, lockBorder.copy(alpha = 0.65f), CircleShape)
                                        .clickable { onToggleDeleteMode() }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                        .testTag("toggle_delete_mode_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (state.isDeleteMode) Icons.Default.LockOpen else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = lockTextTint,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (state.isDeleteMode) "وضع الحذف مفعّل" else "حماية التعديل (مُقفل)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = lockTextTint,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(cardBorder)
                            )

                            // 1. Departments (المراحل الدراسية)
                            StructureSectionBox(
                                title = "1. المراحل الدراسية",
                                titleColor = Indigo500,
                                bgColor = if (state.darkMode) Slate900.copy(alpha = 0.5f) else Color(0xFFEEF2FF).copy(alpha = 0.7f),
                                borderColor = if (state.darkMode) Slate700 else Color(0xFFE0E7FF),
                                darkMode = state.darkMode,
                                inputValue = newDeptText,
                                onInputChange = { newDeptText = it },
                                placeholder = "اسم المرحلة...",
                                buttonColor = Indigo600,
                                onAddClick = {
                                    onAddDept(newDeptText)
                                    newDeptText = ""
                                }
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    state.settings.structure.forEachIndexed { idx, dept ->
                                        val selected = state.selDeptIdx == idx
                                        StructureChip(
                                            label = dept.name,
                                            selected = selected,
                                            activeBorderColor = Indigo500,
                                            activeTextColor = if (state.darkMode) Color(0xFFA5B4FC) else Indigo600,
                                            darkMode = state.darkMode,
                                            showDelete = state.isDeleteMode,
                                            onClick = { onSelectDept(idx) },
                                            onDelete = { onRequestDeleteStruct("d", idx) }
                                        )
                                    }
                                }
                            }

                            // 2. Stages (صفوف المرحلة)
                            StructureSectionBox(
                                title = "2. صفوف (${state.currentDept.name})",
                                titleColor = Amber500,
                                bgColor = if (state.darkMode) Slate900.copy(alpha = 0.5f) else Color(0xFFFFFBEB).copy(alpha = 0.7f),
                                borderColor = if (state.darkMode) Slate700 else Color(0xFFFEF3C7),
                                darkMode = state.darkMode,
                                inputValue = newStageText,
                                onInputChange = { newStageText = it },
                                placeholder = "اسم الصف...",
                                buttonColor = Amber500,
                                headerTrailing = {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(if (state.darkMode) Color(0xFF78350F).copy(alpha = 0.45f) else Color.White)
                                            .border(
                                                1.dp,
                                                if (state.darkMode) Amber500.copy(alpha = 0.6f) else Color(0xFFFCD34D),
                                                RoundedCornerShape(9.dp)
                                            )
                                            .clickable { onToggleStageMaxGrade() }
                                            .padding(horizontal = 9.dp, vertical = 4.dp)
                                            .testTag("toggle_stage_max_grade_button")
                                    ) {
                                        Text(
                                            text = "نظام لـ (${state.currentStage.name}): من ${state.maxAllowedGrade} 🔄",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = if (state.darkMode) Color(0xFFFCD34D) else Color(0xFF92400E),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                },
                                onAddClick = {
                                    onAddStage(newStageText)
                                    newStageText = ""
                                }
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    state.currentDept.stages.forEachIndexed { idx, stage ->
                                        val selected = state.selStageIdx == idx
                                        StructureChip(
                                            label = stage.name,
                                            selected = selected,
                                            activeBorderColor = Amber500,
                                            activeTextColor = if (state.darkMode) Color(0xFFFCD34D) else Color(0xFFB45309),
                                            darkMode = state.darkMode,
                                            showDelete = state.isDeleteMode,
                                            onClick = { onSelectStage(idx) },
                                            onDelete = { onRequestDeleteStruct("s", idx) }
                                        )
                                    }
                                }
                            }

                            // 3. Subjects (مواد الصف)
                            StructureSectionBox(
                                title = "3. مواد (${state.currentStage.name})",
                                titleColor = Emerald500,
                                bgColor = if (state.darkMode) Slate900.copy(alpha = 0.5f) else Color(0xFFECFDF5).copy(alpha = 0.7f),
                                borderColor = if (state.darkMode) Slate700 else Color(0xFFD1FAE5),
                                darkMode = state.darkMode,
                                inputValue = newSubText,
                                onInputChange = { newSubText = it },
                                placeholder = "اسم المادة...",
                                buttonColor = Emerald500,
                                onAddClick = {
                                    onAddSub(newSubText)
                                    newSubText = ""
                                }
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    state.currentStage.subjects.forEachIndexed { idx, sub ->
                                        StructureChip(
                                            label = sub,
                                            selected = false,
                                            activeBorderColor = Emerald500,
                                            activeTextColor = Emerald600,
                                            darkMode = state.darkMode,
                                            showDelete = state.isDeleteMode,
                                            onClick = {},
                                            onDelete = { onRequestDeleteStruct("b", idx) }
                                        )
                                    }
                                }
                            }

                            // 4. Sections (شُعب الصف)
                            StructureSectionBox(
                                title = "4. شُعب (${state.currentStage.name})",
                                titleColor = Purple600,
                                bgColor = if (state.darkMode) Slate900.copy(alpha = 0.5f) else Color(0xFFFAF5FF).copy(alpha = 0.7f),
                                borderColor = if (state.darkMode) Slate700 else Color(0xFFF3E8FF),
                                darkMode = state.darkMode,
                                inputValue = newSecText,
                                onInputChange = { newSecText = it },
                                placeholder = "اسم الشعبة (مثل: د، هـ)...",
                                buttonColor = Purple600,
                                onAddClick = {
                                    onAddSec(newSecText)
                                    newSecText = ""
                                }
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    state.currentStage.sections.forEachIndexed { idx, sec ->
                                        StructureChip(
                                            label = "شعبة ($sec)",
                                            selected = true,
                                            activeBorderColor = Purple600,
                                            activeTextColor = if (state.darkMode) Color(0xFFD8B4FE) else Color(0xFF6B21A8),
                                            darkMode = state.darkMode,
                                            showDelete = state.isDeleteMode,
                                            onClick = {},
                                            onDelete = { onRequestDeleteStruct("sec", idx) }
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
}

@Composable
private fun StructureSectionBox(
    title: String,
    titleColor: Color,
    bgColor: Color,
    borderColor: Color,
    darkMode: Boolean = false,
    inputValue: String,
    onInputChange: (String) -> Unit,
    placeholder: String,
    buttonColor: Color,
    headerTrailing: @Composable (() -> Unit)? = null,
    onAddClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    color = titleColor
                )
                headerTrailing?.invoke()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = onInputChange,
                    placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall, color = Slate400) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (darkMode) Slate800 else Color.White,
                        unfocusedContainerColor = if (darkMode) Slate800 else Color.White,
                        focusedBorderColor = buttonColor,
                        unfocusedBorderColor = if (darkMode) Slate700 else Slate200
                    ),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(buttonColor)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            content()
        }
    }
}

@Composable
private fun StructureChip(
    label: String,
    selected: Boolean,
    activeBorderColor: Color,
    activeTextColor: Color,
    darkMode: Boolean,
    showDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val bg = if (selected) {
        if (darkMode) Slate700 else Color.White
    } else {
        if (darkMode) Slate800 else Color.White.copy(alpha = 0.85f)
    }
    val border = if (selected) activeBorderColor else (if (darkMode) Slate700 else Slate200)
    val textColor = if (selected) activeTextColor else (if (darkMode) Slate300 else Slate600)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
        if (showDelete) {
            Box(
                modifier = Modifier
                    .clickable { onDelete() }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "حذف",
                    tint = Rose500,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
