package com.arrowescape.puzzle.ui.components

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
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowescape.puzzle.ui.theme.ThemePreset
import com.arrowescape.puzzle.ui.theme.ThemePresets

/**
 * Streamlined, zero-clutter Pause Menu dialog.
 * Presents clear game controls (Resume, Restart, Home), compact audio toggles,
 * and a minimalist theme swatch strip without distracting text fatigue.
 */
@Composable
fun SoundSettingsDialog(
    isSoundEnabled: Boolean,
    isVibrationEnabled: Boolean,
    activeTheme: ThemePreset,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onThemeClick: () -> Unit = {},
    onThemeSelect: ((ThemePreset) -> Unit)? = null,
    onRestartClick: (() -> Unit)? = null,
    onHomeClick: (() -> Unit)? = null,
    onTestSound: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val dialogWidth = minOf(screenWidth - 36.dp, 380.dp)
    val isPausedMode = (onRestartClick != null || onHomeClick != null)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = if (activeTheme.isDark) Color(0xFF1E293B) else Color.White,
            border = BorderStroke(
                width = 1.dp,
                color = if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
            ),
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
                // 1. HEADER ROW: Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPausedMode) "Game Paused" else "Settings",
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

                Spacer(modifier = Modifier.height(18.dp))

                // 2. PRIMARY ACTION: RESUME GAME
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.arrowDefault
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            tint = if (activeTheme.isDark) Color(0xFF0F172A) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPausedMode) "Resume Game" else "Done",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTheme.isDark) Color(0xFF0F172A) else Color.White
                        )
                    }
                }

                // 3. SECONDARY ACTIONS ROW: RESTART & HOME
                if (isPausedMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onRestartClick != null) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onRestartClick()
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (activeTheme.isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = activeTheme.textPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Restart",
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Restart",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (onHomeClick != null) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onHomeClick()
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (activeTheme.isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = activeTheme.textPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Home",
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Home",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. COMPACT AUDIO CONTROLS (Side-by-side switches, zero paragraph text)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactAudioToggle(
                        title = "Sound",
                        icon = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                        iconColor = if (isSoundEnabled) Color(0xFFD97706) else Color(0xFF64748B),
                        isChecked = isSoundEnabled,
                        onCheckedChange = { onToggleSound() },
                        activeTheme = activeTheme,
                        modifier = Modifier.weight(1f)
                    )

                    CompactAudioToggle(
                        title = "Haptics",
                        icon = Icons.Default.Vibration,
                        iconColor = if (isVibrationEnabled) Color(0xFF4F46E5) else Color(0xFF64748B),
                        isChecked = isVibrationEnabled,
                        onCheckedChange = { onToggleVibration() },
                        activeTheme = activeTheme,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. COMPACT IN-MENU THEME STRIP (Minimalist, instant tap switching)
                Column(modifier = Modifier.fillMaxWidth()) {
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
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Board Theme",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeTheme.textPrimary
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
                                        if (onThemeSelect != null) {
                                            onThemeSelect(preset)
                                        } else {
                                            onThemeClick()
                                        }
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(preset.arrowDefault)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = if (preset.isDark) Color.White else Color.Black,
                                            modifier = Modifier.size(11.dp)
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
private fun CompactAudioToggle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeTheme: ThemePreset,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (activeTheme.isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
        border = BorderStroke(
            width = 1.dp,
            color = if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!isChecked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = activeTheme.textPrimary
                )
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(width = 34.dp, height = 20.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = if (activeTheme.isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                )
            )
        }
    }
}
