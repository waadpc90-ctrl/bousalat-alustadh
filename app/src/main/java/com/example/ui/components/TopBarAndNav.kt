package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.WaadUiState
import com.example.ui.theme.Amber400
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Indigo700
import com.example.ui.theme.Purple600
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800

val BrandGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Indigo600)
)

@Composable
fun AppLogoIcon(
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        fun sx(x: Float) = (x / 100f) * w
        fun sy(y: Float) = (y / 100f) * h

        // 1. Open Academic Gradebook / Ledger Base (السجل الأكاديمي المفتوح)
        val leftPage = Path().apply {
            moveTo(sx(48f), sy(80f))
            cubicTo(sx(37f), sy(76f), sx(25f), sy(76f), sx(14f), sy(80f))
            lineTo(sx(14f), sy(91f))
            cubicTo(sx(25f), sy(87f), sx(37f), sy(87f), sx(48f), sy(91f))
            close()
        }
        drawPath(path = leftPage, color = Color(0xFFE0E7FF))

        val rightPage = Path().apply {
            moveTo(sx(52f), sy(80f))
            cubicTo(sx(63f), sy(76f), sx(75f), sy(76f), sx(86f), sy(80f))
            lineTo(sx(86f), sy(91f))
            cubicTo(sx(75f), sy(87f), sx(63f), sy(87f), sx(52f), sy(91f))
            close()
        }
        drawPath(path = rightPage, color = Color.White)

        // Golden Book Spine
        val spine = Path().apply {
            moveTo(sx(47.5f), sy(79.5f))
            lineTo(sx(52.5f), sy(79.5f))
            lineTo(sx(52.5f), sy(92.5f))
            lineTo(sx(47.5f), sy(92.5f))
            close()
        }
        drawPath(path = spine, color = Color(0xFFF59E0B))

        // 2. Outer Compass Bezel & Dial (إطار وميناء البوصلة الاحترافية)
        val compassCenter = Offset(sx(50f), sy(53f))
        val outerRadius = sx(30f)
        val innerRadius = sx(23.5f)

        // Dark Navy Inner Face
        drawCircle(
            color = Color(0xFF1E1B4B),
            radius = outerRadius,
            center = compassCenter
        )
        // Outer Crisp White Ring
        drawCircle(
            color = Color.White,
            radius = outerRadius,
            center = compassCenter,
            style = Stroke(width = sx(4.8f))
        )
        // Inner Precision Golden Ring
        drawCircle(
            color = Amber400.copy(alpha = 0.45f),
            radius = innerRadius,
            center = compassCenter,
            style = Stroke(width = sx(1.8f))
        )

        // Analytics / Grade Bars inside Dial
        val bar1 = Path().apply {
            moveTo(sx(34f), sy(58f))
            lineTo(sx(39f), sy(58f))
            lineTo(sx(39f), sy(67f))
            lineTo(sx(34f), sy(67f))
            close()
        }
        drawPath(path = bar1, color = Indigo400.copy(alpha = 0.35f))

        val bar2 = Path().apply {
            moveTo(sx(42f), sy(51f))
            lineTo(sx(47f), sy(51f))
            lineTo(sx(47f), sy(67f))
            lineTo(sx(42f), sy(67f))
            close()
        }
        drawPath(path = bar2, color = Indigo400.copy(alpha = 0.5f))

        val bar3 = Path().apply {
            moveTo(sx(58f), sy(45f))
            lineTo(sx(63f), sy(45f))
            lineTo(sx(63f), sy(67f))
            lineTo(sx(58f), sy(67f))
            close()
        }
        drawPath(path = bar3, color = Indigo400.copy(alpha = 0.65f))

        // Compass Cardinal Tick Marks (N, E, S, W)
        val tickStroke = sx(3.2f)
        drawLine(
            color = Amber400,
            start = Offset(sx(74.5f), sy(53f)),
            end = Offset(sx(79.5f), sy(53f)),
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Amber400,
            start = Offset(sx(20.5f), sy(53f)),
            end = Offset(sx(25.5f), sy(53f)),
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Amber400,
            start = Offset(sx(50f), sy(77.5f)),
            end = Offset(sx(50f), sy(82.5f)),
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )

        // 3. 3D Faceted Compass Needle (إبرة البوصلة الذهبية والفضية)
        val swLight = Path().apply {
            moveTo(sx(50f), sy(53f))
            lineTo(sx(44.5f), sy(47.5f))
            lineTo(sx(32f), sy(71f))
            close()
        }
        drawPath(path = swLight, color = Color(0xFFCBD5E1))

        val swDark = Path().apply {
            moveTo(sx(50f), sy(53f))
            lineTo(sx(55.5f), sy(58.5f))
            lineTo(sx(32f), sy(71f))
            close()
        }
        drawPath(path = swDark, color = Color(0xFF94A3B8))

        val neBrightGold = Path().apply {
            moveTo(sx(50f), sy(53f))
            lineTo(sx(44.5f), sy(47.5f))
            lineTo(sx(68.5f), sy(34.5f))
            close()
        }
        drawPath(path = neBrightGold, color = Color(0xFFFDE047))

        val neRichAmber = Path().apply {
            moveTo(sx(50f), sy(53f))
            lineTo(sx(55.5f), sy(58.5f))
            lineTo(sx(68.5f), sy(34.5f))
            close()
        }
        drawPath(path = neRichAmber, color = Color(0xFFF59E0B))

        // Center Pivot Jewel
        drawCircle(
            color = Color.White,
            radius = sx(5.6f),
            center = compassCenter
        )
        drawCircle(
            color = Color(0xFFD97706),
            radius = sx(2.8f),
            center = compassCenter
        )

        // 4. Academic Graduation Mortarboard Cap at Top (قبعة الأستاذ والتفوق)
        val capBase = Path().apply {
            moveTo(sx(34f), sy(18f))
            lineTo(sx(34f), sy(25f))
            cubicTo(sx(34f), sy(28.5f), sx(41f), sy(30.5f), sx(50f), sy(30.5f))
            cubicTo(sx(59f), sy(30.5f), sx(66f), sy(28.5f), sx(66f), sy(25f))
            lineTo(sx(66f), sy(18f))
            close()
        }
        drawPath(path = capBase, color = Color(0xFFE0E7FF))

        val capDiamond = Path().apply {
            moveTo(sx(50f), sy(5f))
            lineTo(sx(77f), sy(15f))
            lineTo(sx(50f), sy(25f))
            lineTo(sx(23f), sy(15f))
            close()
        }
        drawPath(path = capDiamond, color = Color.White)

        // Cap Center Gold Button & Hanging Tassel
        drawCircle(
            color = Amber400,
            radius = sx(2.4f),
            center = Offset(sx(50f), sy(15f))
        )
        drawLine(
            color = Amber400,
            start = Offset(sx(70.5f), sy(17.5f)),
            end = Offset(sx(74f), sy(27.5f)),
            strokeWidth = sx(2.8f),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color(0xFFF59E0B),
            radius = sx(3.2f),
            center = Offset(sx(74f), sy(27.5f))
        )
    }
}

@Composable
fun WaadHeaderBar(
    state: WaadUiState,
    activeSettingsTab: String = "school",
    onToggleDarkMode: () -> Unit,
    onOpenUnlockModal: () -> Unit,
    onOpenSchoolSettings: () -> Unit = {},
    onOpenUpdateModal: () -> Unit = {},
    onOpenSettings: () -> Unit
) {
    val bgColor = if (state.darkMode) Slate800 else Color.White
    val borderColor = if (state.darkMode) Slate700 else Slate200
    val isSchoolSettingsActive = state.view == "settings" && activeSettingsTab == "school"
    val isAppSettingsActive = state.view == "settings" && activeSettingsTab == "app"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(4.dp, RoundedCornerShape(13.dp), spotColor = Indigo600)
                        .clip(RoundedCornerShape(13.dp))
                        .background(BrandGradient)
                        .border(1.dp, Amber400.copy(alpha = 0.55f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AppLogoIcon(size = 29.dp)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val installedVersion = com.example.update.GoogleDriveUpdateManager.getInstalledVersionName(context)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "بوصلة الأستاذ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            ),
                            color = if (state.darkMode) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (state.darkMode) Indigo500.copy(alpha = 0.22f)
                                    else Color(0xFFEEF2FF)
                                )
                                .border(
                                    1.dp,
                                    if (state.darkMode) Indigo400.copy(alpha = 0.5f)
                                    else Color(0xFFC7D2FE),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                .testTag("header_app_version_badge")
                        ) {
                            Text(
                                text = "v$installedVersion",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                ),
                                color = if (state.darkMode) Amber400 else Indigo700,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenSchoolSettings() }
                            .padding(vertical = 1.dp)
                    ) {
                        Text(
                            text = state.settings.teacherName.ifBlank { "الأستاذ : وعد بزيع" },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (state.darkMode) Indigo400 else Indigo600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Dark Mode Toggle
                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (state.darkMode) Slate700 else Slate50)
                        .border(1.dp, if (state.darkMode) Slate600 else Slate200, RoundedCornerShape(10.dp))
                        .clickable { onToggleDarkMode() }
                        .testTag("toggle_dark_mode_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "الوضع الليلي",
                        tint = if (state.darkMode) Amber400 else Slate600,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Trial / Subscription Badge (Single-line pill, never wraps)
                if (!state.activationState.isActivated) {
                    Box(
                        modifier = Modifier
                            .height(35.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (state.darkMode) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFFFBEB))
                            .border(
                                1.dp,
                                if (state.darkMode) Color(0xFFB45309) else Color(0xFFFCD34D),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onOpenUnlockModal() }
                            .padding(horizontal = 7.dp)
                            .testTag("trial_status_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "باقي ${state.activationState.remainingTrialDays} يوم",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.5.sp
                            ),
                            color = if (state.darkMode) Amber400 else Color(0xFFB45309),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .height(35.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (state.darkMode) Color(0xFF312E81).copy(alpha = 0.5f) else Color(0xFFEEF2FF))
                            .border(
                                1.dp,
                                if (state.darkMode) Color(0xFF4338CA) else Color(0xFFC7D2FE),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onOpenUnlockModal() }
                            .padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "سنوي ✓",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.5.sp
                            ),
                            color = if (state.darkMode) Indigo400 else Indigo700,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // School & Teacher Info Button (قبعة معلومات المدرسة والمدرس)
                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSchoolSettingsActive) {
                                Brush.linearGradient(listOf(Indigo600, Purple600))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        if (state.darkMode) Color(0xFF312E81).copy(alpha = 0.5f) else Color(0xFFEEF2FF),
                                        if (state.darkMode) Color(0xFF312E81).copy(alpha = 0.5f) else Color(0xFFEEF2FF)
                                    )
                                )
                            }
                        )
                        .border(
                            1.dp,
                            if (isSchoolSettingsActive) Amber400.copy(alpha = 0.8f)
                            else if (state.darkMode) Indigo500.copy(alpha = 0.45f)
                            else Color(0xFFC7D2FE),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onOpenSchoolSettings() }
                        .testTag("header_school_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "معلومات المدرسة والمدرس",
                        tint = if (isSchoolSettingsActive) Color.White else (if (state.darkMode) Indigo400 else Indigo600),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // App Settings Button (ترس إعدادات التطبيق)
                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isAppSettingsActive) {
                                Brush.linearGradient(listOf(Indigo600, Purple600))
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        if (state.darkMode) Slate700 else Slate50,
                                        if (state.darkMode) Slate700 else Slate50
                                    )
                                )
                            }
                        )
                        .border(
                            1.dp,
                            if (isAppSettingsActive) Amber400.copy(alpha = 0.8f)
                            else if (state.darkMode) Slate600
                            else Slate200,
                            RoundedCornerShape(11.dp)
                        )
                        .clickable { onOpenSettings() }
                        .testTag("header_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "إعدادات التطبيق",
                        tint = if (isAppSettingsActive) Color.White else (if (state.darkMode) Slate200 else Slate600),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(borderColor)
        )
    }
}

@Composable
fun ScopeSelectorBanner(
    state: WaadUiState,
    filteredCount: Int,
    onSelectDept: (Int) -> Unit,
    onSelectStage: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = Indigo600.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF3730A3), Indigo600, Color(0xFF6D28D9))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Column(
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
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Amber400)
                    )
                    Text(
                        text = "النطاق الفعال (${state.currentStage.name} • من ${state.maxAllowedGrade})",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        ),
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$filteredCount طالب",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        ),
                        color = Color(0xFFFDE047),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val deptOptions = state.settings.structure.mapIndexed { idx, d -> idx to d.name }
                val stageOptions = state.currentDept.stages.mapIndexed { idx, s -> idx to s.name }

                WaadDropdownSelector(
                    selectedLabel = state.currentDept.name,
                    options = deptOptions,
                    onSelect = onSelectDept,
                    modifier = Modifier.weight(1f),
                    containerColor = Color.White.copy(alpha = 0.15f),
                    borderColor = Color.White.copy(alpha = 0.28f),
                    textColor = Color.White,
                    height = 40.dp
                )

                WaadDropdownSelector(
                    selectedLabel = state.currentStage.name,
                    options = stageOptions,
                    onSelect = onSelectStage,
                    modifier = Modifier.weight(1f),
                    containerColor = Color.White.copy(alpha = 0.15f),
                    borderColor = Color.White.copy(alpha = 0.28f),
                    textColor = Color.White,
                    height = 40.dp
                )
            }
        }
    }
}

@Composable
fun <T> WaadDropdownSelector(
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Slate50,
    borderColor: Color = Slate200,
    textColor: Color = Color(0xFF0F172A),
    height: Dp = 40.dp,
    fontSize: TextUnit = 12.sp
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable { if (options.isNotEmpty()) expanded = true }
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = fontSize
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    }
                )
            }
        }
    }
}

@Composable
fun WaadBottomNav(
    currentView: String,
    darkMode: Boolean,
    onSelectView: (String) -> Unit,
    onToggleEntry: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val isBottomTabSelected = currentView in listOf("dashboard", "list", "attendance", "schedule")

    val activeSlotIndex = when (currentView) {
        "dashboard" -> 0
        "list" -> 1
        "entry" -> 2
        "attendance" -> 3
        "schedule" -> 4
        else -> 0
    }

    val targetAccentColor = when (currentView) {
        "dashboard" -> if (darkMode) Indigo400 else Indigo600
        "list" -> if (darkMode) Color(0xFFC084FC) else Purple600
        "entry" -> Rose500
        "attendance" -> if (darkMode) Color(0xFF34D399) else Color(0xFF059669)
        "schedule" -> if (darkMode) Amber400 else Color(0xFFD97706)
        else -> if (darkMode) Indigo400 else Indigo600
    }

    val animatedAccent by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "nav_accent_color"
    )

    val fabRotation by animateFloatAsState(
        targetValue = if (currentView == "entry") 135f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_rotation"
    )

    val fabScale by animateFloatAsState(
        targetValue = if (currentView == "entry") 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "fab_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val dockBg = if (darkMode) {
        Brush.verticalGradient(
            listOf(
                Slate800.copy(alpha = 0.98f),
                Color(0xFF0F172A).copy(alpha = 0.99f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.98f),
                Slate50.copy(alpha = 0.99f)
            )
        )
    }
    val dockBorder = if (darkMode) Slate700 else Slate200

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (darkMode) 18.dp else 12.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = animatedAccent.copy(alpha = 0.35f),
                    spotColor = animatedAccent.copy(alpha = 0.45f)
                ),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        dockBorder,
                        animatedAccent.copy(alpha = 0.45f),
                        dockBorder
                    )
                )
            )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(dockBg)
                    .height(68.dp)
            ) {
                val slotWidth = maxWidth / 5
                val indicatorOffset by animateDpAsState(
                    targetValue = slotWidth * activeSlotIndex,
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = 360f
                    ),
                    label = "sliding_pill_offset"
                )

                val pillScale by animateFloatAsState(
                    targetValue = if (isBottomTabSelected) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "pill_visibility_scale"
                )

                // 1. Sliding Liquid Capsule + Top Neon Beam Indicator
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(slotWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glowing background pill for non-FAB tabs
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .scale(pillScale)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        animatedAccent.copy(alpha = if (darkMode) 0.24f else 0.14f),
                                        animatedAccent.copy(alpha = if (darkMode) 0.08f else 0.05f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = animatedAccent.copy(alpha = if (darkMode) 0.38f else 0.24f),
                                shape = RoundedCornerShape(20.dp)
                            )
                    )

                    // Top glowing neon beam inside safe bounds (no corner clipping)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .scale(pillScale)
                            .width(24.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(animatedAccent)
                    )
                }

                // 2. Interactive Navigation Items Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(
                        label = "الرئيسية",
                        icon = Icons.Default.Dashboard,
                        selected = currentView == "dashboard",
                        activeColor = if (darkMode) Indigo400 else Indigo600,
                        darkMode = darkMode,
                        tag = "nav_dashboard",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (currentView != "dashboard") {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectView("dashboard")
                            }
                        }
                    )

                    BottomNavItem(
                        label = "الطلاب",
                        icon = Icons.Default.Group,
                        selected = currentView == "list",
                        activeColor = if (darkMode) Color(0xFFC084FC) else Purple600,
                        darkMode = darkMode,
                        tag = "nav_students",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (currentView != "list") {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectView("list")
                            }
                        }
                    )

                    // Center Elevated Action Button Slot
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Subtle pulsing halo behind the center button
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .scale(pulseGlow)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (currentView == "entry") Rose500.copy(alpha = 0.25f)
                                    else Indigo500.copy(alpha = 0.2f)
                                )
                        )

                        Box(
                            modifier = Modifier
                                .scale(fabScale)
                                .size(48.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(18.dp),
                                    spotColor = if (currentView == "entry") Rose500 else Indigo600
                                )
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (currentView == "entry") {
                                        Brush.linearGradient(listOf(Rose500, Color(0xFFE11D48)))
                                    } else {
                                        BrandGradient
                                    }
                                )
                                .border(
                                    width = 2.dp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleEntry()
                                }
                                .testTag("nav_add_student_fab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "إضافة طالب",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(25.dp)
                                    .rotate(fabRotation)
                            )
                        }
                    }

                    BottomNavItem(
                        label = "الحضور",
                        icon = Icons.Default.CalendarMonth,
                        selected = currentView == "attendance",
                        activeColor = if (darkMode) Color(0xFF34D399) else Color(0xFF059669),
                        darkMode = darkMode,
                        tag = "nav_attendance",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (currentView != "attendance") {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectView("attendance")
                            }
                        }
                    )

                    BottomNavItem(
                        label = "الجدول",
                        icon = Icons.Default.Schedule,
                        selected = currentView == "schedule",
                        activeColor = if (darkMode) Amber400 else Color(0xFFD97706),
                        darkMode = darkMode,
                        tag = "nav_schedule",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (currentView != "schedule") {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectView("schedule")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    activeColor: Color,
    darkMode: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val inactiveColor = if (darkMode) Slate400 else Slate600.copy(alpha = 0.75f)

    val itemColor by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 250),
        label = "nav_item_color"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.22f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nav_icon_scale"
    )

    val iconOffsetY by animateDpAsState(
        targetValue = if (selected) (-2).dp else 1.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nav_icon_offset_y"
    )

    val dotWidth by animateDpAsState(
        targetValue = if (selected) 14.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_dot_width"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 6.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = itemColor,
            modifier = Modifier
                .size(22.dp)
                .offset(y = iconOffsetY)
                .scale(iconScale)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                fontSize = if (selected) 11.sp else 10.sp
            ),
            color = itemColor,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(dotWidth)
                .height(3.dp)
                .clip(CircleShape)
                .background(activeColor)
        )
    }
}

