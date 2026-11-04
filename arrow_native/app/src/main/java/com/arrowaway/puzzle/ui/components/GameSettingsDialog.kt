package com.arrowaway.puzzle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowaway.puzzle.ui.theme.ThemePreset
import com.arrowaway.puzzle.ui.theme.ThemePresets
import kotlin.math.roundToInt

/**
 * Modern In-Game Settings Dialog accessible directly from the Game Area header.
 * Provides instant real-time configuration for:
 * 1. Audio & Haptics (Sound Effects, Vibration)
 * 2. Gameplay Controls (Hint Button, Grid Guide Button, Hint Timer Duration Slider)
 * 3. Board Theme Selection (Real-time live swatch preview)
 */
@Composable
fun GameSettingsDialog(
    isSoundEnabled: Boolean,
    isVibrationEnabled: Boolean,
    isHintButtonVisible: Boolean,
    isGridButtonVisible: Boolean,
    hintTimerSeconds: Int,
    activeTheme: ThemePreset,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onToggleHintButton: () -> Unit,
    onToggleGridButton: () -> Unit,
    onHintTimerChange: (Int) -> Unit,
    onThemeSelect: (ThemePreset) -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val dialogWidth = minOf(screenWidth - 32.dp, 360.dp)
    val maxScrollHeight = (screenHeight - 140.dp).coerceIn(280.dp, 560.dp)

    var showHintTimerInfoDialog by remember { mutableStateOf(false) }

    val cardBg = if (activeTheme.isDark) Color(0xFF1E293B) else Color.White
    val cardBorder = if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val itemBg = if (activeTheme.isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFF1F5F9)
    val sectionCardBg = if (activeTheme.isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFF8FAFC)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 18.dp,
            modifier = Modifier
                .width(dialogWidth)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // 1. DIALOG HEADER (Title + Close Button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTheme.textPrimary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = activeTheme.textPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxScrollHeight)
                        .verticalScroll(rememberScrollState())
                ) {
                    // =========================================================
                    // SECTION 1: AUDIO & HAPTICS
                    // =========================================================
                    Text(
                        text = "AUDIO & HAPTICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = activeTheme.textSecondary,
                        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = sectionCardBg,
                        border = BorderStroke(1.dp, cardBorder.copy(alpha = 0.8f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Sound Effects Toggle
                            GameSettingToggleRow(
                                title = "Sound Effects",
                                icon = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                                iconBg = if (isSoundEnabled) Color(0xFFFEF3C7) else Color(0xFFE2E8F0),
                                iconTint = if (isSoundEnabled) Color(0xFFD97706) else Color(0xFF64748B),
                                isChecked = isSoundEnabled,
                                onCheckedChange = { onToggleSound() },
                                activeTheme = activeTheme
                            )

                            HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.5f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )

                            // Haptic Vibration Toggle
                            GameSettingToggleRow(
                                title = "Haptic Vibration",
                                icon = Icons.Default.Vibration,
                                iconBg = if (isVibrationEnabled) Color(0xFFE0E7FF) else Color(0xFFE2E8F0),
                                iconTint = if (isVibrationEnabled) Color(0xFF4F46E5) else Color(0xFF64748B),
                                isChecked = isVibrationEnabled,
                                onCheckedChange = { onToggleVibration() },
                                activeTheme = activeTheme
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // =========================================================
                    // SECTION 2: GAMEPLAY CONTROLS
                    // =========================================================
                    Text(
                        text = "GAMEPLAY CONTROLS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = activeTheme.textSecondary,
                        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = sectionCardBg,
                        border = BorderStroke(1.dp, cardBorder.copy(alpha = 0.8f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Hint Button Visibility
                            GameSettingToggleRow(
                                title = "Hint Button",
                                icon = Icons.Default.Lightbulb,
                                iconBg = itemBg,
                                iconTint = Color(0xFFF59E0B),
                                isChecked = isHintButtonVisible,
                                onCheckedChange = { onToggleHintButton() },
                                activeTheme = activeTheme
                            )

                            HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.5f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )

                            // Grid Guide (#) Button Visibility
                            GameSettingToggleRow(
                                title = "Grid Guide (#) Button",
                                icon = Icons.Default.GridOn,
                                iconBg = itemBg,
                                iconTint = activeTheme.arrowDefault,
                                isChecked = isGridButtonVisible,
                                onCheckedChange = { onToggleGridButton() },
                                activeTheme = activeTheme
                            )

                            HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.5f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )

                            // Hint Timer Configuration Row
                            val isTimerActive = isHintButtonVisible
                            val timerControlsAlpha = if (isTimerActive) 1f else 0.45f

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(itemBg)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = "Hint Timer",
                                                tint = activeTheme.textPrimary.copy(alpha = timerControlsAlpha),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Hint Timer",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = activeTheme.textPrimary.copy(alpha = timerControlsAlpha),
                                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                            )

                                            // (i) info icon
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .clickable { showHintTimerInfoDialog = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Info,
                                                    contentDescription = "Hint Timer Info",
                                                    tint = activeTheme.textSecondary.copy(alpha = if (isTimerActive) 0.85f else 0.55f),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        color = if (isTimerActive) activeTheme.arrowDefault.copy(alpha = 0.15f) else activeTheme.gridLine.copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, if (isTimerActive) activeTheme.arrowDefault.copy(alpha = 0.35f) else activeTheme.gridLine.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = if (isTimerActive) "${hintTimerSeconds}s" else "Disabled",
                                            fontSize = if (isTimerActive) 12.sp else 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isTimerActive) activeTheme.arrowDefault else activeTheme.textSecondary.copy(alpha = 0.55f),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Slider(
                                    value = hintTimerSeconds.toFloat(),
                                    onValueChange = { onHintTimerChange(it.roundToInt().coerceIn(5, 55)) },
                                    enabled = isTimerActive,
                                    valueRange = 5f..55f,
                                    steps = 9,
                                    colors = SliderDefaults.colors(
                                        thumbColor = activeTheme.arrowDefault,
                                        activeTrackColor = activeTheme.arrowDefault,
                                        inactiveTrackColor = activeTheme.gridLine.copy(alpha = 0.5f),
                                        disabledThumbColor = activeTheme.arrowDefault.copy(alpha = 0.3f),
                                        disabledActiveTrackColor = activeTheme.arrowDefault.copy(alpha = 0.25f),
                                        disabledInactiveTrackColor = activeTheme.gridLine.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "5s",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = activeTheme.textSecondary.copy(alpha = if (isTimerActive) 0.65f else 0.3f)
                                    )
                                    Text(
                                        text = "55s",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = activeTheme.textSecondary.copy(alpha = if (isTimerActive) 0.65f else 0.3f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // =========================================================
                    // SECTION 3: BOARD THEME
                    // =========================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Theme",
                                tint = activeTheme.arrowDefault,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "BOARD THEME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                color = activeTheme.textSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = activeTheme.arrowDefault.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = activeTheme.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeTheme.arrowDefault,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Swatches row with real-time reactive selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemePresets.all.forEach { preset ->
                            val isSelected = (preset.name == activeTheme.name)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = preset.background,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) activeTheme.arrowDefault else preset.gridLine
                                ),
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .size(width = 46.dp, height = 34.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onThemeSelect(preset)
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (isSelected) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(preset.arrowDefault)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = preset.surface,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(preset.arrowDefault)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. BOTTOM DONE BUTTON
                    AppPrimaryButton(
                        text = "Done",
                        onClick = onDismiss,
                        theme = activeTheme,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // HINT TIMER EXPLANATION MODAL POPUP
        if (showHintTimerInfoDialog) {
            Dialog(
                onDismissRequest = { showHintTimerInfoDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardBg,
                    border = BorderStroke(1.2.dp, cardBorder),
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .widthIn(max = 330.dp)
                        .fillMaxWidth(0.88f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(activeTheme.arrowDefault.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Hint Timer",
                                tint = activeTheme.arrowDefault,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Hint Timer",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.textPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "When you haven't made a move for the set time, the Hint button will softly pulse to remind you that help is available.",
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = activeTheme.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { showHintTimerInfoDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeTheme.arrowDefault,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Text(
                                text = "Got It",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean, compact toggle row designed specifically for the in-game settings dialog.
 */
@Composable
private fun GameSettingToggleRow(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeTheme: ThemePreset
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = activeTheme.textPrimary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = activeTheme.arrowDefault,
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = activeTheme.gridLine.copy(alpha = 0.5f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
