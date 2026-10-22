package com.arrowescape.puzzle.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowescape.puzzle.ui.theme.ThemePreset

@Composable
fun SettingsDialog(
    isSoundEnabled: Boolean,
    isVibrationEnabled: Boolean,
    activeTheme: ThemePreset,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val dialogWidth = minOf(screenWidth - 36.dp, 360.dp)

    val openUrl = { url: String ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    val openEmail = {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@arrowescape.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Arrow Escape Feedback (v1.0.0)")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    val openRateUs = {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openUrl("https://play.google.com/store/apps/details?id=${context.packageName}")
        }
    }

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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // 1. HEADER: Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(activeTheme.arrowDefault.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = activeTheme.arrowDefault,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Settings",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.textPrimary
                        )
                    }

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

                // SECTION 1: AUDIO & HAPTICS (Keeping standardized toggles)
                Text(
                    text = "AUDIO & HAPTICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeTheme.textSecondary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Sound Effects Toggle Row
                SettingsToggleRow(
                    title = "Sound Effects",
                    icon = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                    iconBg = if (isSoundEnabled) Color(0xFFFEF3C7) else Color(0xFFE2E8F0),
                    iconTint = if (isSoundEnabled) Color(0xFFD97706) else Color(0xFF64748B),
                    isChecked = isSoundEnabled,
                    onCheckedChange = { onToggleSound() },
                    activeTheme = activeTheme
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Haptic Vibration Toggle Row
                SettingsToggleRow(
                    title = "Haptic Vibration",
                    icon = Icons.Default.Vibration,
                    iconBg = if (isVibrationEnabled) Color(0xFFE0E7FF) else Color(0xFFE2E8F0),
                    iconTint = if (isVibrationEnabled) Color(0xFF4F46E5) else Color(0xFF64748B),
                    isChecked = isVibrationEnabled,
                    onCheckedChange = { onToggleVibration() },
                    activeTheme = activeTheme
                )

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 2: SUPPORT & COMMUNITY
                Text(
                    text = "SUPPORT & COMMUNITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeTheme.textSecondary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SettingsLinkRow(
                    title = "Rate Us",
                    subtitle = "Support us with 5 stars on Play Store",
                    icon = Icons.Default.Star,
                    iconBg = Color(0xFFFFFBEB),
                    iconTint = Color(0xFFF59E0B),
                    onClick = openRateUs,
                    activeTheme = activeTheme
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsLinkRow(
                    title = "Feedback & Support",
                    subtitle = "Contact the development team",
                    icon = Icons.Default.Email,
                    iconBg = Color(0xFFECFDF5),
                    iconTint = Color(0xFF10B981),
                    onClick = openEmail,
                    activeTheme = activeTheme
                )

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 3: LEGAL & PRIVACY (Store requirement)
                Text(
                    text = "LEGAL & PRIVACY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeTheme.textSecondary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SettingsLinkRow(
                    title = "Privacy Policy",
                    subtitle = "How we protect your data",
                    icon = Icons.Default.Lock,
                    iconBg = Color(0xFFF3F4F6),
                    iconTint = Color(0xFF6B7280),
                    onClick = { openUrl("https://arrowescape.com/privacy") },
                    activeTheme = activeTheme
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsLinkRow(
                    title = "Terms of Service",
                    subtitle = "Terms and conditions of use",
                    icon = Icons.Default.Policy,
                    iconBg = Color(0xFFF3F4F6),
                    iconTint = Color(0xFF6B7280),
                    onClick = { openUrl("https://arrowescape.com/terms") },
                    activeTheme = activeTheme
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4. DONE BUTTON
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
                    Text(
                        text = "DONE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (activeTheme.isDark) Color(0xFF0F172A) else Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. VERSION FOOTER
                Text(
                    text = "Arrow Escape • Version 1.0.0 (Build 1)",
                    fontSize = 11.sp,
                    color = activeTheme.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
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
        color = if (activeTheme.isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
        border = BorderStroke(
            1.dp,
            if (activeTheme.isDark) Color(0xFF475569) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconBg)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

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
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = activeTheme.arrowDefault,
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = if (activeTheme.isDark) Color(0xFF475569) else Color(0xFFE2E8F0),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun SettingsLinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit,
    activeTheme: ThemePreset
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (activeTheme.isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
        border = BorderStroke(
            1.dp,
            if (activeTheme.isDark) Color(0xFF475569) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconBg)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = activeTheme.textPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = activeTheme.textSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = activeTheme.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
