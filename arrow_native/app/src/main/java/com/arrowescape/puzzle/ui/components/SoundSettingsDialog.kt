package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowescape.puzzle.ui.theme.ThemePreset
import com.arrowescape.puzzle.ui.theme.ThemePresets

/**
 * Unified Pause & Settings Menu Dialog.
 * Combines Game Pause actions (Resume, Restart, Home), Audio & Haptics switches,
 * and an in-menu Theme Swatch strip for instant customization.
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
    val screenHeight = configuration.screenHeightDp.dp
    val dialogWidth = minOf(screenWidth - 32.dp, 420.dp)
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
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. HEADER ROW: Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isPausedMode) "Game Paused" else "Settings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.textPrimary
                        )
                        Text(
                            text = if (isPausedMode) "Take a breath & configure settings" else "In-game audio, haptics & theme",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = activeTheme.textSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = activeTheme.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. AUDIO & HAPTIC SWITCHES
                SettingToggleItem(
                    title = "Sound Effects",
                    subtitle = if (isSoundEnabled) "Arrow taps, fly whoosh & fanfare" else "All in-game sound muted",
                    icon = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                    iconBg = if (isSoundEnabled) Color(0xFFFEF3C7) else Color(0xFFE2E8F0),
                    iconTint = if (isSoundEnabled) Color(0xFFD97706) else Color(0xFF64748B),
                    isChecked = isSoundEnabled,
                    onCheckedChange = { onToggleSound() },
                    activeTheme = activeTheme
                )

                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleItem(
                    title = "Haptic Vibration",
                    subtitle = if (isVibrationEnabled) "Tactile feedback for moves & combos" else "Vibration disabled",
                    icon = Icons.Default.Vibration,
                    iconBg = if (isVibrationEnabled) Color(0xFFE0E7FF) else Color(0xFFE2E8F0),
                    iconTint = if (isVibrationEnabled) Color(0xFF4F46E5) else Color(0xFF64748B),
                    isChecked = isVibrationEnabled,
                    onCheckedChange = { onToggleVibration() },
                    activeTheme = activeTheme
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. IN-MENU THEME SELECTION STRIP
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
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Board Theme",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = activeTheme.textPrimary
                            )
                        }

                        // Current active theme badge
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = activeTheme.arrowDefault.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, activeTheme.arrowDefault.copy(alpha = 0.35f))
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Theme Swatch Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemePresets.all.forEach { preset ->
                            val isSelected = (preset.name == activeTheme.name)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (activeTheme.isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) activeTheme.arrowDefault else if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (isSelected) 3.dp else 0.dp,
                                modifier = Modifier
                                    .clickable {
                                        if (onThemeSelect != null) {
                                            onThemeSelect(preset)
                                        } else {
                                            onThemeClick()
                                        }
                                    }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    // Swatch color preview box
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp, 32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(preset.background)
                                            .border(1.dp, preset.gridLine, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Miniature arrow preview dot / chip
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
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = preset.name.split(" ").firstOrNull() ?: preset.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) activeTheme.textPrimary else activeTheme.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. IN-GAME NAVIGATION CONTROLS (Home + Restart if paused in gameplay)
                if (isPausedMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onHomeClick != null) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onHomeClick()
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (activeTheme.isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = activeTheme.textPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Home",
                                        modifier = Modifier.size(18.dp)
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

                        if (onRestartClick != null) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onRestartClick()
                                },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (activeTheme.isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = activeTheme.textPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Restart",
                                        modifier = Modifier.size(18.dp)
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
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 5. PRIMARY RESUME / DONE BUTTON
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.arrowDefault
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isPausedMode) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = if (activeTheme.isDark) Color(0xFF0F172A) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isPausedMode) "Resume Game" else "Done",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTheme.isDark) Color(0xFF0F172A) else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeTheme: ThemePreset
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (activeTheme.isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
        border = BorderStroke(
            width = 1.dp,
            color = if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = activeTheme.textPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = activeTheme.textSecondary
                    )
                }
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
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
