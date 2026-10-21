package com.arrowescape.puzzle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

/**
 * Dedicated Sound & Vibration Menu popup.
 * Focused strictly on audio and haptics control with zero unrelated clutter.
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
    val dialogWidth = minOf(screenWidth - 36.dp, 340.dp)

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
                // 1. HEADER: Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sound & Vibration",
                        fontSize = 18.sp,
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

                // 2. SOUND EFFECTS TOGGLE ROW
                SettingToggleItem(
                    title = "Sound Effects",
                    icon = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                    iconBg = if (isSoundEnabled) Color(0xFFFEF3C7) else Color(0xFFE2E8F0),
                    iconTint = if (isSoundEnabled) Color(0xFFD97706) else Color(0xFF64748B),
                    isChecked = isSoundEnabled,
                    onCheckedChange = { onToggleSound() },
                    activeTheme = activeTheme
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. HAPTIC VIBRATION TOGGLE ROW
                SettingToggleItem(
                    title = "Haptic Vibration",
                    icon = Icons.Default.Vibration,
                    iconBg = if (isVibrationEnabled) Color(0xFFE0E7FF) else Color(0xFFE2E8F0),
                    iconTint = if (isVibrationEnabled) Color(0xFF4F46E5) else Color(0xFF64748B),
                    isChecked = isVibrationEnabled,
                    onCheckedChange = { onToggleVibration() },
                    activeTheme = activeTheme
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 4. DONE BUTTON
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.arrowDefault
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTheme.isDark) Color(0xFF0F172A) else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeTheme: ThemePreset
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
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
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = activeTheme.textPrimary
                )
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(width = 36.dp, height = 22.dp),
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
