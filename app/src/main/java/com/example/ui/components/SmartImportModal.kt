package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun SmartImportToolbar(
    darkMode: Boolean,
    onPickPdf: () -> Unit,
    onPickImage: () -> Unit,
    onCaptureCamera: () -> Unit,
    onOpenBulkAdd: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    if (darkMode) {
                        listOf(Indigo900.copy(alpha = 0.85f), Purple900.copy(alpha = 0.85f))
                    } else {
                        listOf(Color(0xFFEEF2FF), Color(0xFFF5F3FF))
                    }
                )
            )
            .border(
                1.dp,
                if (darkMode) Indigo500.copy(alpha = 0.45f) else Indigo500.copy(alpha = 0.3f),
                RoundedCornerShape(18.dp)
            )
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (darkMode) Amber400 else Indigo600,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "الاستخراج الذكي من ملف PDF أو صورة",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = if (darkMode) Color.White else Slate900
                        )
                        Text(
                            text = "يتعرف تلقائياً على أسماء الطلاب بالعربية ويستبعد العناوين والدرجات",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (darkMode) Slate300 else Slate600
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Emerald500.copy(alpha = 0.16f))
                        .border(1.dp, Emerald500.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "دعم العربية ✨",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = if (darkMode) Color(0xFF6EE7B7) else Emerald600,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmartSourceActionButton(
                    icon = Icons.Default.PictureAsPdf,
                    label = "ملف PDF",
                    subLabel = "قوائم رقمية/مصورة",
                    containerColor = Rose600,
                    onClick = onPickPdf,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_pdf_button")
                )

                SmartSourceActionButton(
                    icon = Icons.Default.Image,
                    label = "صورة قائمة",
                    subLabel = "من الاستوديو",
                    containerColor = Indigo600,
                    onClick = onPickImage,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("import_image_button")
                )

                SmartSourceActionButton(
                    icon = Icons.Default.CameraAlt,
                    label = "الكاميرا",
                    subLabel = "تصوير مباشر",
                    containerColor = Emerald600,
                    onClick = onCaptureCamera,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("capture_camera_button")
                )
            }

            if (onOpenBulkAdd != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (darkMode) Slate800.copy(alpha = 0.85f) else Color.White)
                        .border(
                            1.dp,
                            if (darkMode) Indigo500.copy(alpha = 0.35f) else Indigo500.copy(alpha = 0.25f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onOpenBulkAdd() }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                        .testTag("open_bulk_add_from_students_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = if (darkMode) Amber400 else Indigo600,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "إدخال جماعي سريع (لصق قائمة أسماء نصية)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = if (darkMode) Color.White else Indigo600
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartSourceActionButton(
    icon: ImageVector,
    label: String,
    subLabel: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun SmartExtractionLoadingDialog(
    darkMode: Boolean,
    statusLabel: String,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = if (darkMode) Slate800 else Color.White,
            border = BorderStroke(1.dp, if (darkMode) Slate700 else Slate200),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Indigo500.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Indigo500,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "الاستخراج الذكي عالي الدقة ✨",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = if (darkMode) Color.White else Slate900,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = statusLabel.ifBlank { "جاري قراءة الملف وتنقية أسماء الطلاب..." },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (darkMode) Slate300 else Slate600,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "يتم استبعاد الترويسات والدرجات والأرقام التسلسلية تلقائياً",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Slate400,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SmartImportReviewModal(
    state: WaadUiState,
    onTargetSecChange: (String) -> Unit,
    onToggleCandidate: (String) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onUpdateCandidateName: (String, String) -> Unit,
    onDeleteCandidate: (String) -> Unit,
    onLoadAllRawLines: () -> Unit,
    onCopyToBulkText: () -> Unit,
    onConfirmDirectImport: () -> Unit,
    onPickAnotherPdf: () -> Unit,
    onPickAnotherImage: () -> Unit,
    onDismiss: () -> Unit
) {
    val result = state.smartExtractionResult ?: return
    val darkMode = state.darkMode
    val cardBg = if (darkMode) Slate800 else Color.White
    val borderCol = if (darkMode) Slate700 else Slate200

    val candidates = result.smartCandidates
    val selectedCount = candidates.count { it.selected && it.name.trim().length >= 2 }
    val allSelected = candidates.isNotEmpty() && candidates.all { it.selected }
    var editingCandidateId by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(28.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderCol),
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Gradient Accent Strip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Indigo600, Purple600, Emerald500)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (result.sourceType == "pdf") Rose500.copy(alpha = 0.15f)
                                        else Indigo500.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (result.sourceType == "pdf") {
                                        Icons.Default.PictureAsPdf
                                    } else {
                                        Icons.Default.Image
                                    },
                                    contentDescription = null,
                                    tint = if (result.sourceType == "pdf") Rose500 else Indigo500,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "نتائج الاستخراج الذكي للطلاب ✨",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = if (darkMode) Color.White else Slate900
                                )
                                Text(
                                    text = "${result.fileName} • (${result.pageCount} صفحة)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Slate400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
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

                    // Accuracy & Preview Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (darkMode) Slate900 else Slate50)
                            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (result.previewBitmap != null) {
                            Image(
                                bitmap = result.previewBitmap.asImageBitmap(),
                                contentDescription = "معاينة المستند",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 52.dp, height = 64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (result.success) Emerald500.copy(alpha = 0.16f)
                                            else Rose500.copy(alpha = 0.16f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (result.success) {
                                            "الدقة: ${result.accuracyPercent}% ✓"
                                        } else {
                                            "تنبيه في القراءة"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (result.success) Emerald500 else Rose500
                                    )
                                }

                                if (result.success) {
                                    Text(
                                        text = "تم اكتشاف ${candidates.size} اسم",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = Indigo500
                                    )
                                }
                            }

                            Text(
                                text = result.engineLabel.ifBlank { result.errorMessage ?: "" },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (darkMode) Slate300 else Slate600
                            )
                        }
                    }

                    if (!result.success || candidates.isEmpty()) {
                        // Error or Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Amber500,
                                modifier = Modifier.size(38.dp)
                            )
                            Text(
                                text = result.errorMessage ?: "لم يتم العثور على أسماء طلاب صالحة في هذا الملف.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (darkMode) Slate300 else Slate700,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onPickAnotherPdf,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(
                                        text = "اختيار ملف PDF آخر",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = Color.White
                                    )
                                }
                                Button(
                                    onClick = onPickAnotherImage,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(
                                        text = "اختيار صورة أخرى",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    } else {
                        // Target Section & Mode Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إضافة إلى الشعبة:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = if (darkMode) Slate300 else Slate700
                            )
                            val secOpts = state.stageSections.map { it to "شعبة ($it)" }
                            Box(modifier = Modifier.width(145.dp)) {
                                WaadDropdownSelector(
                                    selectedLabel = "شعبة (${state.bulkTargetSec.ifBlank { state.stageSections.firstOrNull() ?: "أ" }})",
                                    options = secOpts,
                                    onSelect = onTargetSecChange,
                                    containerColor = if (darkMode) Slate700 else Slate50,
                                    borderColor = if (darkMode) Slate600 else Slate200,
                                    textColor = if (darkMode) Color.White else Slate900,
                                    height = 42.dp
                                )
                            }
                        }

                        // Select All + Pure Names Filter Badge Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (darkMode) Slate700 else Slate100)
                                    .clickable { onToggleAll(!allSelected) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (allSelected) Emerald500 else Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (allSelected) "إلغاء تحديد الكل" else "تحديد الكل (${candidates.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = if (darkMode) Color.White else Slate800
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Emerald500.copy(alpha = 0.14f))
                                    .border(1.dp, Emerald500.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "أسماء الطلاب فقط ✓",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = if (darkMode) Color(0xFF6EE7B7) else Emerald600,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Scrollable List of Extracted Candidates
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(min = 160.dp, max = 290.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (darkMode) Slate900 else Slate50)
                                .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            candidates.forEachIndexed { idx, cand ->
                                val isEditing = editingCandidateId == cand.id

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (cand.selected) {
                                        if (darkMode) Slate800 else Color.White
                                    } else {
                                        if (darkMode) Slate800.copy(alpha = 0.45f) else Slate100.copy(alpha = 0.6f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (cand.selected) Indigo500.copy(alpha = 0.4f) else borderCol
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { onToggleCandidate(cand.id) }
                                            ) {
                                                Icon(
                                                    imageVector = if (cand.selected) {
                                                        Icons.Default.CheckBox
                                                    } else {
                                                        Icons.Default.CheckBoxOutlineBlank
                                                    },
                                                    contentDescription = null,
                                                    tint = if (cand.selected) Emerald500 else Slate400,
                                                    modifier = Modifier.size(20.dp)
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(RoundedCornerShape(7.dp))
                                                        .background(Indigo500.copy(alpha = 0.14f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${idx + 1}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                                        color = Indigo500
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = cand.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 13.sp
                                                        ),
                                                        color = if (darkMode) Color.White else Slate900,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (cand.phone.isNotBlank() || cand.detectedSection.isNotBlank()) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            if (cand.detectedSection.isNotBlank()) {
                                                                Text(
                                                                    text = "شعبة (${cand.detectedSection})",
                                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                                        fontSize = 9.5.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    ),
                                                                    color = Indigo400
                                                                )
                                                            }
                                                            if (cand.phone.isNotBlank()) {
                                                                Text(
                                                                    text = "📞 ${cand.phone}",
                                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                                        fontSize = 9.5.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    ),
                                                                    color = Emerald500
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Indigo500.copy(alpha = 0.12f))
                                                        .clickable {
                                                            editingCandidateId = if (isEditing) null else cand.id
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.EditNote,
                                                        contentDescription = "تعديل الاسم",
                                                        tint = Indigo500,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Rose500.copy(alpha = 0.12f))
                                                        .clickable { onDeleteCandidate(cand.id) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "حذف السطر",
                                                        tint = Rose500,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (isEditing) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            OutlinedTextField(
                                                value = cand.name,
                                                onValueChange = { onUpdateCandidateName(cand.id, it) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Indigo500,
                                                    unfocusedBorderColor = borderCol
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Action Buttons
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onConfirmDirectImport,
                                enabled = selectedCount > 0,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("confirm_smart_import_direct_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "اعتماد وإضافة المحددين للسجل فوراً ($selectedCount طالب) ✅",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onCopyToBulkText,
                                    enabled = selectedCount > 0,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Blue500),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .heightIn(min = 46.dp)
                                        .testTag("copy_smart_import_to_text_button")
                                ) {
                                    Text(
                                        text = "نقل لحقل النص للتعديل ✍️",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Button(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (darkMode) Slate700 else Slate200
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .heightIn(min = 46.dp)
                                ) {
                                    Text(
                                        text = "إلغاء",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = if (darkMode) Color.White else Slate800
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
