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
import androidx.compose.material.icons.filled.Lightbulb
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
 * Clean, flat In-Game Settings Dialog accessible from the Game Area header.
 * Ordered cleanly:
 * 1. THEME (Real-time swatch picker)
 * 2. SOUND (Sound Effects, Haptic Vibration)
 * 3. GAMEPLAY CONTROLS (Hint Button, Grid Guide Button with gameplay icon, Hint Timer slider with Off option)
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
    val dialogWidth = minOf(screenWidth - 36.dp, 350.dp)
    val maxScrollHeight = (screenHeight - 120.dp).coerceIn(300.dp, 600.dp)

    var showHintTimerInfoDialog by remember { mutableStateOf(false) }

    val cardBg = if (activeTheme.isDark) Color(0xFF1E293B) else Color.White
    val cardBorder = if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val dividerColor = activeTheme.gridLine.copy(alpha = 0.35f)
    val itemBg = if (activeTheme.isDark) Color(0xFF334155).copy(alpha = 0.55f) else Color(0xFFF1F5F9)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 16.dp,
            modifier = Modifier
                .width(dialogWidth)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // DIALOG HEADER (Title + Close Button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeTheme.textPrimary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = activeTheme.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable content column
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxScrollHeight)
                        .verticalScroll(rememberScrollState())
                ) {
                    // =========================================================
                    // SECTION 1: THEME
                    // =========================================================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "THEME",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = activeTheme.textSecondary
                        )

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

                    // Real-time Swatches Row
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
                                    .size(width = 44.dp, height = 32.dp)
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // =========================================================
                    // SECTION 2: SOUND
                    // =========================================================
                    Text(
                        text = "SOUND",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = activeTheme.textSecondary,
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                    )

                    GameSettingToggleRow(
                        title = "Sound Effects",
                        icon = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                        iconBg = if (isSoundEnabled) Color(0xFFFEF3C7) else Color(0xFFE2E8F0),
                        iconTint = if (isSoundEnabled) Color(0xFFD97706) else Color(0xFF64748B),
                        isChecked = isSoundEnabled,
                        onCheckedChange = { onToggleSound() },
                        activeTheme = activeTheme
                    )

                    HorizontalDivider(color = dividerColor, thickness = 0.8.dp)

                    GameSettingToggleRow(
                        title = "Haptic Vibration",
                        icon = Icons.Default.Vibration,
                        iconBg = if (isVibrationEnabled) Color(0xFFE0E7FF) else Color(0xFFE2E8F0),
                        iconTint = if (isVibrationEnabled) Color(0xFF4F46E5) else Color(0xFF64748B),
                        isChecked = isVibrationEnabled,
                        onCheckedChange = { onToggleVibration() },
                        activeTheme = activeTheme
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // =========================================================
                    // SECTION 3: GAMEPLAY CONTROLS
                    // =========================================================
                    Text(
                        text = "GAMEPLAY CONTROLS",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = activeTheme.textSecondary,
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                    )

                    GameSettingToggleRow(
                        title = "Hint Button",
                        icon = Icons.Default.Lightbulb,
                        iconBg = itemBg,
                        iconTint = Color(0xFFF59E0B),
                        isChecked = isHintButtonVisible,
                        onCheckedChange = { onToggleHintButton() },
                        activeTheme = activeTheme
                    )

                    HorizontalDivider(color = dividerColor, thickness = 0.8.dp)

                    // Grid Guide (#) Button with authentic gameplay area (#) icon
                    GameSettingToggleRow(
                        title = "Grid Guide (#) Button",
                        iconBg = itemBg,
                        isChecked = isGridButtonVisible,
                        onCheckedChange = { onToggleGridButton() },
                        activeTheme = activeTheme,
                        customIcon = {
                            GridGuideIcon(
                                tint = activeTheme.arrowDefault,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    HorizontalDivider(color = dividerColor, thickness = 0.8.dp)

                    // Hint Timer Slider Row (with OFF option)
                    val isTimerActive = isHintButtonVisible
                    val timerControlsAlpha = if (isTimerActive) 1f else 0.45f

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
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

                            // Value chip (Disabled, Off, or Xs)
                            val chipText = when {
                                !isTimerActive -> "Disabled"
                                hintTimerSeconds <= 0 -> "Off"
                                else -> "${hintTimerSeconds}s"
                            }
                            val isChipActive = isTimerActive && hintTimerSeconds > 0

                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (isChipActive) activeTheme.arrowDefault.copy(alpha = 0.15f) else activeTheme.gridLine.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isChipActive) activeTheme.arrowDefault.copy(alpha = 0.35f) else activeTheme.gridLine.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = chipText,
                                    fontSize = if (isChipActive) 12.sp else 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isChipActive) activeTheme.arrowDefault else activeTheme.textSecondary.copy(alpha = 0.55f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Slider with 0 (Off) to 55s range in 5s steps (steps = 10 between min and max)
                        Slider(
                            value = hintTimerSeconds.coerceIn(0, 55).toFloat(),
                            onValueChange = { raw ->
                                val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(0, 55)
                                onHintTimerChange(snapped)
                            },
                            enabled = isTimerActive,
                            valueRange = 0f..55f,
                            steps = 10,
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
                                text = "Off",
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

                    Spacer(modifier = Modifier.height(18.dp))

                    // 4. DONE BUTTON
                    AppPrimaryButton(
                        text = "Done",
                        onClick = onDismiss,
                        theme = activeTheme,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
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
 * Clean, flat toggle row with support for ImageVector or custom vector icon composable.
 */
@Composable
private fun GameSettingToggleRow(
    title: String,
    iconBg: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeTheme: ThemePreset,
    icon: ImageVector? = null,
    iconTint: Color = activeTheme.textPrimary,
    customIcon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 7.dp),
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
                if (customIcon != null) {
                    customIcon()
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
