package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Student
import com.example.model.WaadCalculator
import com.example.ui.WaadUiState
import com.example.ui.components.ScopeSelectorBanner
import com.example.ui.components.SmartImportToolbar
import com.example.ui.components.WaadDropdownSelector
import com.example.ui.theme.Amber400
import com.example.ui.theme.Amber500
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
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun StudentsListScreen(
    state: WaadUiState,
    filteredStudents: List<Student>,
    onSelectDept: (Int) -> Unit,
    onSelectStage: (Int) -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenBulkAdd: () -> Unit = {},
    onPickPdf: () -> Unit = {},
    onPickImage: () -> Unit = {},
    onCaptureCamera: () -> Unit = {},
    onSelectSec: (String) -> Unit,
    onSelectSub: (String) -> Unit = {},
    onSelectSort: (String) -> Unit,
    onOpenProfile: (Student) -> Unit,
    onOpenBonus: (Student) -> Unit,
    onOpenShare: (Student) -> Unit,
    onEditStudent: (Student) -> Unit,
    onDeleteStudent: (String) -> Unit,
    onDeleteStudentsBulk: (List<String>) -> Unit = {},
    onSaveSectionNote: (String, String, String, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onUpdateMissingHomework: (Student, String, Int, Boolean) -> Unit = { _, _, _, _ -> },
    onDeductDailyForMissingHomework: (Student, String, String, Int) -> Unit = { _, _, _, _ -> }
) {
    val cardBg = if (state.darkMode) Slate800 else Color.White
    val cardBorder = if (state.darkMode) Slate700 else Slate200

    val activeSub = state.globalSub.ifBlank { state.currentStage.subjects.firstOrNull() ?: "عام" }
    var activeNoteSec by remember(state.selDeptIdx, state.selStageIdx) {
        mutableStateOf(state.globalSec.ifBlank { state.stageSections.firstOrNull() ?: "أ" })
    }
    LaunchedEffect(state.globalSec) {
        if (state.globalSec.isNotBlank()) {
            activeNoteSec = state.globalSec
        }
    }

    var isEditingSectionNote by remember { mutableStateOf(false) }
    var homeworkTargetStudent by remember { mutableStateOf<Student?>(null) }

    // Multi-selection state for bulk student deletion
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedStudentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Prune selected IDs whenever the filtered list or stage/department changes
    val filteredIdSet = remember(filteredStudents) { filteredStudents.map { it.id }.toSet() }
    LaunchedEffect(filteredIdSet) {
        val pruned = selectedStudentIds.intersect(filteredIdSet)
        if (pruned.size != selectedStudentIds.size) {
            selectedStudentIds = pruned
        }
        if (filteredStudents.isEmpty() && isSelectionMode) {
            isSelectionMode = false
        }
    }

    val allFilteredSelected = filteredStudents.isNotEmpty() && selectedStudentIds.size == filteredStudents.size

    val currentNoteKey = WaadCalculator.buildSectionCurriculumKey(
        deptName = state.currentDept.name,
        stageName = state.currentStage.name,
        section = activeNoteSec,
        subject = activeSub
    )
    val currentSectionNote = state.sectionNotes[currentNoteKey]

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScopeSelectorBanner(
            state = state,
            filteredCount = filteredStudents.size,
            onSelectDept = onSelectDept,
            onSelectStage = onSelectStage
        )

        // Section Homework & Curriculum Progress Card (مفكرة الواجبات وموقف المنهج لكل شعبة)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Indigo900, Purple900)
                    )
                )
                .border(1.dp, Indigo500.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                .padding(14.dp)
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Amber400.copy(alpha = 0.2f))
                                .border(1.dp, Amber400.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = Amber400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "مفكرة الواجبات وموقف المنهج ($activeSub)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                            Text(
                                text = "تذكير سريع بما أخذته كل شعبة والواجب المطلوب منها",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Slate300
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Amber500)
                            .clickable { isEditingSectionNote = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("edit_section_homework_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Slate900,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "تحديث الواجب",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = Slate900
                            )
                        }
                    }
                }

                // Section selector tabs inside the Homework card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.stageSections.forEach { sec ->
                        val isSel = sec == activeNoteSec
                        val secKey = WaadCalculator.buildSectionCurriculumKey(
                            deptName = state.currentDept.name,
                            stageName = state.currentStage.name,
                            section = sec,
                            subject = activeSub
                        )
                        val hasNote = state.sectionNotes[secKey]?.let {
                            it.lastTopic.isNotBlank() || it.homework.isNotBlank()
                        } == true

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
                                    if (isSel) Amber400 else Color.White.copy(alpha = 0.16f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    activeNoteSec = sec
                                    onSelectSec(sec)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "شعبة ($sec)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = if (isSel) Color.White else Slate300,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (hasNote) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isSel) Amber400 else Emerald500)
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Section Topic & Homework Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.28f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                        .clickable { isEditingSectionNote = true }
                        .padding(12.dp)
                ) {
                    if (currentSectionNote != null &&
                        (currentSectionNote.lastTopic.isNotBlank() || currentSectionNote.homework.isNotBlank())
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📌 شعبة ($activeNoteSec) — $activeSub",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = Amber400
                                )
                                if (currentSectionNote.updatedAt.isNotBlank()) {
                                    Text(
                                        text = "آخر تحديث: ${currentSectionNote.updatedAt}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Slate300
                                    )
                                }
                            }
                            if (currentSectionNote.lastTopic.isNotBlank()) {
                                Text(
                                    text = "📖 وصلنا بالشرح إلى: ${currentSectionNote.lastTopic}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            if (currentSectionNote.homework.isNotBlank()) {
                                Text(
                                    text = "📝 الواجب البيتي المطلوب: ${currentSectionNote.homework}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "لم يتم تسجيل واجب أو موقف منهج لشعبة ($activeNoteSec) بعد. اضغط هنا لإضافته.",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Slate300,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "+ إضافة",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = Amber400
                            )
                        }
                    }
                }
            }
        }

        // Filter & Search Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.search,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            text = "بحث عن طالب...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (state.darkMode) Slate700 else Slate50,
                        unfocusedContainerColor = if (state.darkMode) Slate700 else Slate50,
                        focusedBorderColor = Indigo500,
                        unfocusedBorderColor = if (state.darkMode) Slate600 else Slate200
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_search_input")
                )

                val secOptions = listOf("" to "كل الشعب") + state.stageSections.map { it to "شعبة ($it)" }
                val subOptions = state.currentStage.subjects.map { it to "مادة: $it" }
                val sortOptions = listOf(
                    "alpha" to "ترتيب أبجدي",
                    "high" to "الأعلى درجات",
                    "low" to "الأقل درجات",
                    "fail" to "الراسبون / المكملون"
                )
                val currentSortLabel = sortOptions.find { it.first == state.sortFilter }?.second ?: "ترتيب أبجدي"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WaadDropdownSelector(
                        selectedLabel = if (state.globalSec.isEmpty()) "كل الشعب" else "شعبة (${state.globalSec})",
                        options = secOptions,
                        onSelect = onSelectSec,
                        modifier = Modifier.weight(1f),
                        containerColor = if (state.darkMode) Slate700 else Slate50,
                        borderColor = if (state.darkMode) Slate600 else Slate200,
                        textColor = if (state.darkMode) Color.White else Slate900
                    )
                    if (subOptions.isNotEmpty()) {
                        WaadDropdownSelector(
                            selectedLabel = "مادة: $activeSub",
                            options = subOptions,
                            onSelect = onSelectSub,
                            modifier = Modifier.weight(1f),
                            containerColor = if (state.darkMode) Slate700 else Slate50,
                            borderColor = if (state.darkMode) Slate600 else Slate200,
                            textColor = if (state.darkMode) Color.White else Slate900
                        )
                    }
                    WaadDropdownSelector(
                        selectedLabel = currentSortLabel,
                        options = sortOptions,
                        onSelect = onSelectSort,
                        modifier = Modifier.weight(1f),
                        containerColor = if (state.darkMode) Slate700 else Slate50,
                        borderColor = if (state.darkMode) Slate600 else Slate200,
                        textColor = if (state.darkMode) Color.White else Slate900
                    )
                }

                // Multi-Selection & Bulk Delete Control Bar
                if (filteredStudents.isNotEmpty()) {
                    val activeBulkBar = isSelectionMode || selectedStudentIds.isNotEmpty()
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (activeBulkBar) {
                            if (state.darkMode) Rose600.copy(alpha = 0.16f) else Rose500.copy(alpha = 0.08f)
                        } else {
                            if (state.darkMode) Slate700.copy(alpha = 0.65f) else Slate50
                        },
                        border = BorderStroke(
                            1.dp,
                            if (activeBulkBar) Rose500.copy(alpha = 0.5f) else (if (state.darkMode) Slate600 else Slate200)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (activeBulkBar) Indigo600
                                                else Indigo500.copy(alpha = 0.14f)
                                            )
                                            .clickable {
                                                if (activeBulkBar) {
                                                    isSelectionMode = false
                                                    selectedStudentIds = emptySet()
                                                } else {
                                                    isSelectionMode = true
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("toggle_multi_select_students_button")
                                    ) {
                                        Text(
                                            text = if (activeBulkBar) "إلغاء التحديد ✕" else "☑️ تحديد أسماء للحذف",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            ),
                                            color = if (activeBulkBar) Color.White else Indigo500
                                        )
                                    }

                                    if (activeBulkBar) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (allFilteredSelected) Slate700 else Emerald600
                                                )
                                                .clickable {
                                                    selectedStudentIds = if (allFilteredSelected) {
                                                        emptySet()
                                                    } else {
                                                        filteredStudents.map { it.id }.toSet()
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .testTag("select_all_students_button")
                                        ) {
                                            Text(
                                                text = if (allFilteredSelected) "إلغاء الكل" else "تحديد الكل (${filteredStudents.size})",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp
                                                ),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                if (activeBulkBar) {
                                    Text(
                                        text = "المحدد: ${selectedStudentIds.size} طالب",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        ),
                                        color = if (selectedStudentIds.isNotEmpty()) Rose500 else Slate400
                                    )
                                } else {
                                    Text(
                                        text = "اضغط على المربع بجانب أي اسم لتحديد عدة طلاب",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp
                                        ),
                                        color = Slate400,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (selectedStudentIds.isNotEmpty()) {
                                Button(
                                    onClick = { showBulkDeleteConfirmDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .testTag("delete_selected_students_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "حذف الطلاب المحددين (${selectedStudentIds.size} طالب) نهائياً 🗑️",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Student Items
        if (filteredStudents.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Indigo500.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = Indigo500,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = "لا توجد أسماء طلاب مسجلة في هذا النطاق",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = if (state.darkMode) Color.White else Slate900,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "اضغط على زر (+) في الشريط السفلي لإضافة طلاب فرادى أو دفعة واحدة.",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                filteredStudents.forEachIndexed { idx, student ->
                    val missingHwCount = state.missingHomework["${student.id}_$activeSub"] ?: 0
                    val isSelected = student.id in selectedStudentIds
                    val activeBulkBar = isSelectionMode || selectedStudentIds.isNotEmpty()

                    val toggleThisStudent = {
                        selectedStudentIds = if (isSelected) {
                            selectedStudentIds - student.id
                        } else {
                            selectedStudentIds + student.id
                        }
                        if (!isSelectionMode && !isSelected) {
                            isSelectionMode = true
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (activeBulkBar) {
                                    toggleThisStudent()
                                } else {
                                    onOpenProfile(student)
                                }
                            }
                            .testTag("student_card_${student.id}"),
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) {
                            if (state.darkMode) Rose600.copy(alpha = 0.18f) else Color(0xFFFFF1F2)
                        } else {
                            cardBg
                        },
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) Rose500 else (if (state.darkMode) Slate700 else Slate200)
                        ),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 6.dp)
                            ) {
                                // Direct Multi-Select Checkbox Badge
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) Rose600
                                            else (if (state.darkMode) Slate700 else Slate100)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Rose500 else (if (state.darkMode) Slate600 else Slate300),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { toggleThisStudent() }
                                        .testTag("select_student_checkbox_${student.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "محدد للحذف",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            ),
                                            color = Indigo500
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = student.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (state.darkMode) Color.White else Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (state.darkMode) Slate700 else Indigo500.copy(alpha = 0.1f))
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "شعبة ${student.section}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Black
                                                ),
                                                color = if (state.darkMode) Indigo400 else Indigo600,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                        if (WaadCalculator.isStudentIntisab(student.id, state.studentTags)) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.16f))
                                                    .border(0.8.dp, Color(0xFF8B5CF6).copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "🏠 انتساب",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black
                                                    ),
                                                    color = if (state.darkMode) Color(0xFFD8B4FE) else Color(0xFF6D28D9),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                        if (missingHwCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Rose500.copy(alpha = 0.16f))
                                                    .border(0.8.dp, Rose500.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                    .clickable { homeworkTargetStudent = student }
                                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "نقص واجب: $missingHwCount",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black
                                                    ),
                                                    color = Rose500,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                StudentActionIcon(
                                    icon = Icons.AutoMirrored.Filled.MenuBook,
                                    tint = if (missingHwCount > 0) Color.White else Purple600,
                                    bgColor = if (missingHwCount > 0) Rose600 else Purple600.copy(alpha = 0.14f),
                                    contentDescription = "تسجيل ومتابعة الواجب البيتي",
                                    badgeText = if (missingHwCount > 0) "$missingHwCount" else null,
                                    onClick = { homeworkTargetStudent = student }
                                )
                                StudentActionIcon(
                                    icon = Icons.Default.Star,
                                    tint = Amber500,
                                    bgColor = Amber500.copy(alpha = 0.14f),
                                    contentDescription = "منح درجة تشجيعية",
                                    onClick = { onOpenBonus(student) }
                                )
                                StudentActionIcon(
                                    icon = Icons.AutoMirrored.Filled.Chat,
                                    tint = Emerald600,
                                    bgColor = Emerald600.copy(alpha = 0.14f),
                                    contentDescription = "إرسال عبر واتساب",
                                    onClick = { onOpenShare(student) }
                                )
                                StudentActionIcon(
                                    icon = Icons.Default.Edit,
                                    tint = Indigo600,
                                    bgColor = Indigo600.copy(alpha = 0.14f),
                                    contentDescription = "تعديل الطالب",
                                    onClick = { onEditStudent(student) }
                                )
                                StudentActionIcon(
                                    icon = Icons.Default.Delete,
                                    tint = Rose600,
                                    bgColor = Rose600.copy(alpha = 0.14f),
                                    contentDescription = "حذف الطالب",
                                    onClick = { onDeleteStudent(student.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal 0: Bulk Student Deletion Confirmation
    if (showBulkDeleteConfirmDialog && selectedStudentIds.isNotEmpty()) {
        val countToDelete = selectedStudentIds.size
        Dialog(onDismissRequest = { showBulkDeleteConfirmDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = if (state.darkMode) Slate800 else Color.White,
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
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Rose500.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Rose600,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "حذف جماعي ($countToDelete طالب) ⚠️",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = Rose500
                        )
                        Text(
                            text = "هل أنت متأكد من حذف ($countToDelete) من الطلاب المحددين مع جميع درجاتهم وسجل حضورهم نهائياً؟",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (state.darkMode) Slate300 else Slate600,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val ids = selectedStudentIds.toList()
                                    showBulkDeleteConfirmDialog = false
                                    selectedStudentIds = emptySet()
                                    isSelectionMode = false
                                    onDeleteStudentsBulk(ids)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                                    .testTag("confirm_bulk_delete_students_button")
                            ) {
                                Text(
                                    text = "نعم، احذف ($countToDelete) 🗑️",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }
                            Button(
                                onClick = { showBulkDeleteConfirmDialog = false },
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

    // Modal 1: Section Homework & Curriculum Progress Editor
    if (isEditingSectionNote) {
        SectionHomeworkEditorDialog(
            deptName = state.currentDept.name,
            stageName = state.currentStage.name,
            section = activeNoteSec,
            subject = activeSub,
            initialTopic = currentSectionNote?.lastTopic ?: "",
            initialHomework = currentSectionNote?.homework ?: "",
            darkMode = state.darkMode,
            onDismiss = { isEditingSectionNote = false },
            onSave = { topic, hw ->
                onSaveSectionNote(
                    state.currentDept.name,
                    state.currentStage.name,
                    activeNoteSec,
                    activeSub,
                    topic,
                    hw
                )
                isEditingSectionNote = false
            }
        )
    }

    // Modal 2: Student Missing Homework Tracker
    val targetStd = homeworkTargetStudent
    if (targetStd != null) {
        StudentHomeworkTrackerDialog(
            state = state,
            student = targetStd,
            initialSubject = activeSub,
            onSelectGlobalSub = onSelectSub,
            onUpdateMissingHomework = onUpdateMissingHomework,
            onDeductDailyForMissingHomework = onDeductDailyForMissingHomework,
            onDismiss = { homeworkTargetStudent = null }
        )
    }
}

@Composable
private fun SectionHomeworkEditorDialog(
    deptName: String,
    stageName: String,
    section: String,
    subject: String,
    initialTopic: String,
    initialHomework: String,
    darkMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var topicText by remember { mutableStateOf(initialTopic) }
    var homeworkText by remember { mutableStateOf(initialHomework) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = if (darkMode) Slate800 else Color.White,
            border = BorderStroke(1.dp, if (darkMode) Slate700 else Slate200)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📝 مفكرة شعبة ($section) — مادة $subject",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = if (darkMode) Color.White else Slate900
                        )
                        Text(
                            text = "$deptName • $stageName",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Slate400
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (darkMode) Slate700 else Slate100)
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

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "📖 أين وصلنا في الشرح مع هذه الشعبة؟ (الموضوع / الصفحة):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = if (darkMode) Amber400 else Indigo600
                    )
                    OutlinedTextField(
                        value = topicText,
                        onValueChange = { topicText = it },
                        placeholder = {
                            Text(
                                text = "مثال: الكسور العشرية (وصلنا إلى صفحة 45)...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "📝 الواجب البيتي المطلوب للحصة القادمة:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = Emerald500
                    )
                    OutlinedTextField(
                        value = homeworkText,
                        onValueChange = { homeworkText = it },
                        placeholder = {
                            Text(
                                text = "مثال: حل تمارين (4-1) في الدفتر صفحة 46...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val hwPresets = listOf(
                        "حل تمارين الكتاب في الدفتر",
                        "تحضير الدرس القادم",
                        "مراجعة لامتحان يومي",
                        "كتابة الملخص والقاعدة",
                        "لا يوجد واجب"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        hwPresets.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (darkMode) Slate700 else Slate100)
                                    .clickable { homeworkText = preset }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (darkMode) Slate300 else Slate700
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSave(topicText, homeworkText) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حفظ المفكرة",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = { onSave("", "") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (darkMode) Slate700 else Slate200
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "مسح",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = if (darkMode) Color.White else Slate800
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentHomeworkTrackerDialog(
    state: WaadUiState,
    student: Student,
    initialSubject: String,
    onSelectGlobalSub: (String) -> Unit,
    onUpdateMissingHomework: (Student, String, Int, Boolean) -> Unit,
    onDeductDailyForMissingHomework: (Student, String, String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val darkMode = state.darkMode
    val stageSubjects = state.currentStage.subjects.ifEmpty { listOf(initialSubject.ifBlank { "عام" }) }
    var selectedSub by remember(student.id, initialSubject) {
        mutableStateOf(
            if (initialSubject in stageSubjects) initialSubject else stageSubjects.first()
        )
    }
    var selectedMonthField by remember(student.id) { mutableStateOf("v1") }
    var inlineFeedback by remember(student.id, selectedSub) { mutableStateOf<String?>(null) }

    val missingCount = state.missingHomework["${student.id}_$selectedSub"] ?: 0
    val studentGrades = state.grades["${student.id}_$selectedSub"]
    val currentDailyScore = studentGrades?.getField(selectedMonthField)?.toIntOrNull()

    val monthOptions = listOf(
        "v1" to "ف1-ش1",
        "v4" to "ف1-ش2",
        "v7" to "ف2-ش1",
        "v10" to "ف2-ش2"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(26.dp),
            color = if (darkMode) Slate800 else Color.White,
            border = BorderStroke(1.dp, if (darkMode) Slate700 else Slate200),
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .background(if (missingCount > 0) Rose600 else Purple600)
                )

                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📝 تسجيل ومتابعة الواجب البيتي",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = if (darkMode) Color.White else Slate900
                            )
                            Text(
                                text = "${student.name} • شعبة (${student.section})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Indigo500
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (darkMode) Slate700 else Slate100)
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

                    // Subject Selector Chips (if multiple subjects exist)
                    if (stageSubjects.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            stageSubjects.forEach { sub ->
                                val isSel = sub == selectedSub
                                val subMissing = state.missingHomework["${student.id}_$sub"] ?: 0
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSel) Indigo600
                                            else (if (darkMode) Slate700 else Slate100)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSel) Indigo400 else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            selectedSub = sub
                                            onSelectGlobalSub(sub)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (subMissing > 0) "$sub ($subMissing)" else sub,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = if (isSel) Color.White else (if (darkMode) Slate300 else Slate700),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Counter Box with +1 / -1 stepper
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (missingCount > 0) Rose500.copy(alpha = 0.12f)
                                else Emerald500.copy(alpha = 0.12f)
                            )
                            .border(
                                1.dp,
                                if (missingCount > 0) Rose500.copy(alpha = 0.4f) else Emerald500.copy(alpha = 0.4f),
                                RoundedCornerShape(18.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Increment (+1) Stepper Button
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Rose600)
                                        .clickable {
                                            val next = missingCount + 1
                                            onUpdateMissingHomework(student, selectedSub, 1, false)
                                            inlineFeedback = "⚠️ تم زيادة عدد مرات نقص الواجب إلى ($next)"
                                        }
                                        .testTag("stepper_increment_missing_hw"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+1",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = Color.White
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (missingCount > 0) "نقص واجب: $missingCount مرات" else "ملتزم بالواجبات ✅",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = if (missingCount > 0) Rose500 else Emerald600
                                    )
                                    Text(
                                        text = "مادة: $selectedSub",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Slate400
                                    )
                                }

                                // Decrement (-1) Stepper Button
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (missingCount > 0) Amber500
                                            else (if (darkMode) Slate700 else Slate200)
                                        )
                                        .clickable(enabled = missingCount > 0) {
                                            val next = (missingCount - 1).coerceAtLeast(0)
                                            onUpdateMissingHomework(student, selectedSub, -1, false)
                                            inlineFeedback = if (next == 0) {
                                                "✅ تم تصفير سجل نقص الواجب"
                                            } else {
                                                "تم تقليل العدد إلى ($next)"
                                            }
                                        }
                                        .testTag("stepper_decrement_missing_hw"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "-1",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = if (missingCount > 0) Slate900 else Slate400
                                    )
                                }
                            }

                            if (inlineFeedback != null) {
                                Text(
                                    text = inlineFeedback ?: "",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = if (missingCount > 0) Amber500 else Emerald500,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "يظهر هذا السجل تلقائياً في تقرير ولي الأمر عبر واتساب",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Slate400,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Primary 1-Click Action: Record +1 Missing Homework & Close immediately
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Rose600)
                            .clickable {
                                onUpdateMissingHomework(student, selectedSub, 1, false)
                                onDismiss()
                            }
                            .testTag("confirm_record_missing_hw_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚠️ تسجيل نقص واجب اليوم (+1) وحفظ",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Optional Daily Grade Deduction Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (darkMode) Slate900.copy(alpha = 0.6f) else Slate50)
                            .border(1.dp, if (darkMode) Slate700 else Slate200, RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "خصم من درجة اليومي مع تسجيل النقص (اختياري):",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = if (darkMode) Slate300 else Slate700
                                )
                                Text(
                                    text = "اليومي: ${currentDailyScore ?: "غير مرصود"}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = Indigo500
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                monthOptions.forEach { (field, label) ->
                                    val isSel = selectedMonthField == field
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSel) Indigo500
                                                else (if (darkMode) Slate800 else Color.White)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSel) Indigo400 else (if (darkMode) Slate700 else Slate200),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedMonthField = field },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            ),
                                            color = if (isSel) Color.White else Slate400,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Amber500.copy(alpha = 0.2f))
                                    .border(1.dp, Amber500.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        onDeductDailyForMissingHomework(student, selectedSub, selectedMonthField, 1)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "تسجيل نقص (+1) وخصم (-1) من درجة اليومي",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (darkMode) Amber400 else Color(0xFFB45309),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Bottom Row: Reset (if > 0) + Save/Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (missingCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Emerald600)
                                    .clickable {
                                        onUpdateMissingHomework(student, selectedSub, 0, true)
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "تصفير السجل ✅",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (darkMode) Slate700 else Slate200)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "تم / إغلاق",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                color = if (darkMode) Color.White else Slate800,
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

@Composable
private fun StudentActionIcon(
    icon: ImageVector,
    tint: Color,
    bgColor: Color,
    contentDescription: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
        if (!badgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = Rose600,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun StudentEntryScreen(
    state: WaadUiState,
    filteredCount: Int = 0,
    onSelectDept: (Int) -> Unit = {},
    onSelectStage: (Int) -> Unit = {},
    onNameChange: (String) -> Unit,
    onSectionChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onIntisabChange: (Boolean) -> Unit = {},
    onSaveStudent: () -> Unit,
    onOpenBulkAdd: () -> Unit = {},
    onTargetSecChange: (String) -> Unit = {},
    onBulkTextChange: (String) -> Unit = {},
    onConfirmBulkAdd: () -> Unit = {},
    onPickPdf: () -> Unit = {},
    onPickImage: () -> Unit = {},
    onCaptureCamera: () -> Unit = {}
) {
    val cardBg = if (state.darkMode) Slate800 else Color.White
    val cardBorder = if (state.darkMode) Slate700 else Slate200

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScopeSelectorBanner(
            state = state,
            filteredCount = filteredCount,
            onSelectDept = onSelectDept,
            onSelectStage = onSelectStage
        )

        // Smart High-Accuracy PDF & Image Import Toolbar at the very top of Entry Screen
        SmartImportToolbar(
            darkMode = state.darkMode,
            onPickPdf = onPickPdf,
            onPickImage = onPickImage,
            onCaptureCamera = onCaptureCamera,
            onOpenBulkAdd = onOpenBulkAdd
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.formId != null) "تعديل بيانات الطالب" else "إضافة طالب جديد",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = Indigo500
                    )

                    if (state.formId == null) {
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Purple600)
                                .clickable { onOpenBulkAdd() }
                                .padding(horizontal = 12.dp)
                                .testTag("bulk_add_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GroupAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "إدخال جماعي",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(cardBorder)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "اسم الطالب الثلاثي:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = Slate400
                    )
                    OutlinedTextField(
                        value = state.formName,
                        onValueChange = onNameChange,
                        placeholder = { Text("مثال: أحمد محمد علي", color = Slate400) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (state.darkMode) Slate700 else Slate50,
                            unfocusedContainerColor = if (state.darkMode) Slate700 else Slate50,
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = if (state.darkMode) Slate600 else Slate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("entry_student_name_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "الشعبة:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = Slate400
                        )
                        val secOptions = state.stageSections.map { it to "شعبة ($it)" }
                        WaadDropdownSelector(
                            selectedLabel = "شعبة (${state.formSection.ifBlank { state.stageSections.firstOrNull() ?: "أ" }})",
                            options = secOptions,
                            onSelect = onSectionChange,
                            containerColor = if (state.darkMode) Slate700 else Slate50,
                            borderColor = if (state.darkMode) Slate600 else Slate200,
                            textColor = if (state.darkMode) Color.White else Slate900,
                            height = 54.dp
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "هاتف ولي الأمر (اختياري):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = Slate400
                        )
                        OutlinedTextField(
                            value = state.formPhone,
                            onValueChange = onPhoneChange,
                            placeholder = { Text("07xxxxxxxxx", color = Slate400) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                textDirection = TextDirection.Ltr
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (state.darkMode) Slate700 else Slate50,
                                unfocusedContainerColor = if (state.darkMode) Slate700 else Slate50,
                                focusedBorderColor = Indigo500,
                                unfocusedBorderColor = if (state.darkMode) Slate600 else Slate200
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("entry_student_phone_input")
                        )
                    }
                }

                // Attendance System Mode Selector (دوام حضوري منتظم vs طالب انتساب)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "نظام دوام الطالب:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = Slate400
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isRegular = !state.formIsIntisab
                        val intisabPurple = Color(0xFF8B5CF6)
                        Surface(
                            onClick = { onIntisabChange(false) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("entry_mode_regular_btn"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isRegular) Indigo600 else (if (state.darkMode) Slate700 else Slate100),
                            border = BorderStroke(
                                1.dp,
                                if (isRegular) Indigo400 else (if (state.darkMode) Slate600 else Slate200)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🏫 دوام حضوري منتظم",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (isRegular) Color.White else (if (state.darkMode) Slate300 else Slate700),
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            onClick = { onIntisabChange(true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("entry_mode_intisab_btn"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (state.formIsIntisab) intisabPurple else (if (state.darkMode) Slate700 else Slate100),
                            border = BorderStroke(
                                1.dp,
                                if (state.formIsIntisab) intisabPurple else (if (state.darkMode) Slate600 else Slate200)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🏠 طالب انتساب (امتحانات)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (state.formIsIntisab) Color.White else (if (state.darkMode) Slate300 else Slate700),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (state.formIsIntisab) {
                        Text(
                            text = "🏠 طالب الانتساب لا يحضر الدروس اليومية: لن يُحتسب عليه حضور ولا غياب يومي، ويتم تحضيره في أيام الامتحانات فقط.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            ),
                            color = if (state.darkMode) Color(0xFFD8B4FE) else Color(0xFF6D28D9)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onSaveStudent,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("save_student_button")
                ) {
                    Text(
                        text = if (state.formId != null) "حفظ التعديلات ✅" else "إضافة الطالب للسجل ➕",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                }
            }
        }

        // Dedicated Inline Bulk Entry Card on Entry Screen
        if (state.formId == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                border = BorderStroke(1.dp, if (state.darkMode) Purple600.copy(alpha = 0.45f) else Purple600.copy(alpha = 0.35f)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Purple600.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null,
                                tint = Purple600,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "الإدخال الجماعي للطلاب (دفعة واحدة)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = if (state.darkMode) Purple600 else Purple600
                            )
                            Text(
                                text = "استورد الأسماء من ملف PDF أو صورة، أو الصق القائمة نصياً",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الشعبة المراد الإضافة إليها:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = Slate400
                        )
                        val secOptions = state.stageSections.map { it to "شعبة ($it)" }
                        Box(modifier = Modifier.width(150.dp)) {
                            WaadDropdownSelector(
                                selectedLabel = "شعبة (${state.bulkTargetSec.ifBlank { state.stageSections.firstOrNull() ?: "أ" }})",
                                options = secOptions,
                                onSelect = onTargetSecChange,
                                containerColor = if (state.darkMode) Slate700 else Slate50,
                                borderColor = if (state.darkMode) Slate600 else Slate200,
                                textColor = if (state.darkMode) Color.White else Slate900,
                                height = 46.dp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.bulkText,
                        onValueChange = onBulkTextChange,
                        placeholder = {
                            Text(
                                text = "1. أحمد محمد علي\n2. حسين كريم جاسم\n3. زينب علاء حسن",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (state.darkMode) Slate700 else Slate50,
                            unfocusedContainerColor = if (state.darkMode) Slate700 else Slate50,
                            focusedBorderColor = Purple600,
                            unfocusedBorderColor = if (state.darkMode) Slate600 else Slate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("entry_bulk_students_input")
                    )

                    Button(
                        onClick = onConfirmBulkAdd,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple600),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("entry_confirm_bulk_add_button")
                    ) {
                        Text(
                            text = "اعتماد وإضافة القائمة الجماعية ✅",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
