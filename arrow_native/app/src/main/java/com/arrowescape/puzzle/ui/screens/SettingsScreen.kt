package com.arrowescape.puzzle.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.theme.ThemePreset
import com.arrowescape.puzzle.ui.theme.ThemePresets

/**
 * Modern, minimal, theme-adaptive Settings page.
 * Features:
 * - Symmetrical 2-option theme selector (Clean Light vs Warm Sepia) without scrolling.
 * - Grouped card hierarchy for Audio/Notifications and About/Support.
 * - Clean single-line rows with redundant subtitles removed.
 * - Full theme-adaptive background immersion with ambient blooming aura.
 * - Minimal version footer ("Version X.X.X").
 */
@Composable
fun SettingsScreen(
    isSoundEnabled: Boolean,
    isVibrationEnabled: Boolean,
    dailyRemindersEnabled: Boolean = true,
    activeTheme: ThemePreset,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onToggleDailyReminders: () -> Unit = {},
    onThemeSelect: (ThemePreset) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)
    val context = LocalContext.current

    // Dynamically query application version name from PackageManager
    val versionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

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
                putExtra(Intent.EXTRA_SUBJECT, "Arrow Decode Feedback (v$versionName)")
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

    val screenBg = activeTheme.background
    val cardBg = activeTheme.surface
    val cardBorder = activeTheme.gridLine
    val itemBg = if (activeTheme.name == "Warm Sepia") Color(0xFFF8F2E6) else Color(0xFFF8FAFC)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // Soft Ambient Radial Aura behind content
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height * 0.45f
            val auraRadius = size.height * 0.40f
            val auraColor = if (activeTheme.name == "Warm Sepia") {
                Color(0xFFE09F3E).copy(alpha = 0.14f)
            } else {
                Color(0xFF1E293B).copy(alpha = 0.06f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(auraColor, Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = Offset(centerX, centerY)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
        ) {
            // TOP APP BAR: Circular Back Button + Centered Title + Visual Balance Spacer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Back Button matching Home & Game Area style
                Surface(
                    shape = CircleShape,
                    color = cardBg,
                    border = BorderStroke(1.2.dp, cardBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = activeTheme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Centered Clean Title
                Text(
                    text = "Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = activeTheme.textPrimary,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )

                // Dummy Spacer for optical symmetry
                Spacer(modifier = Modifier.size(42.dp))
            }

            // SCROLLABLE SETTINGS CONTENT (Constrained max 340dp width)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .fillMaxWidth()
                ) {
                    // =========================================================
                    // SECTION 1: BOARD THEME (Simplified Dual Selector: No Scrolling!)
                    // =========================================================
                    Text(
                        text = "BOARD THEME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = activeTheme.textSecondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Warm Sepia Option Card
                        ThemeOptionCard(
                            preset = ThemePresets.WarmSepia,
                            isSelected = (activeTheme.name == ThemePresets.WarmSepia.name),
                            activeTheme = activeTheme,
                            onSelect = { onThemeSelect(ThemePresets.WarmSepia) },
                            modifier = Modifier.weight(1f)
                        )

                        // Clean Light Option Card
                        ThemeOptionCard(
                            preset = ThemePresets.CleanLight,
                            isSelected = (activeTheme.name == ThemePresets.CleanLight.name),
                            activeTheme = activeTheme,
                            onSelect = { onThemeSelect(ThemePresets.CleanLight) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // =========================================================
                    // SECTION 2: AUDIO & NOTIFICATIONS (Consolidated Grouped Card)
                    // =========================================================
                    Text(
                        text = "AUDIO & NOTIFICATIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = activeTheme.textSecondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = cardBg,
                        border = BorderStroke(1.2.dp, cardBorder),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Sound Effects Row
                            SettingsToggleRow(
                                title = "Sound Effects",
                                icon = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                                iconBg = itemBg,
                                iconTint = activeTheme.textPrimary,
                                isChecked = isSoundEnabled,
                                onCheckedChange = { onToggleSound() },
                                activeTheme = activeTheme
                            )

                            HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.6f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Haptic Vibration Row
                            SettingsToggleRow(
                                title = "Haptic Vibration",
                                icon = Icons.Default.Vibration,
                                iconBg = itemBg,
                                iconTint = activeTheme.textPrimary,
                                isChecked = isVibrationEnabled,
                                onCheckedChange = { onToggleVibration() },
                                activeTheme = activeTheme
                            )

                            HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.6f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Daily Reminder Row
                            SettingsToggleRow(
                                title = "Daily Reminder",
                                icon = Icons.Default.NotificationsActive,
                                iconBg = itemBg,
                                iconTint = activeTheme.textPrimary,
                                isChecked = dailyRemindersEnabled,
                                onCheckedChange = { onToggleDailyReminders() },
                                activeTheme = activeTheme
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // =========================================================
                    // SECTION 3: ABOUT & SUPPORT (Clean Consolidated Card)
                    // =========================================================
                    Text(
                        text = "ABOUT & SUPPORT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = activeTheme.textSecondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = cardBg,
                        border = BorderStroke(1.2.dp, cardBorder),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Rate Us Row
                            SettingsLinkRow(
                                title = "Rate Arrow Decode",
                                icon = Icons.Default.Star,
                                iconBg = itemBg,
                                iconTint = Color(0xFFD97706),
                                onClick = openRateUs,
                                activeTheme = activeTheme
                            )

                            HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.6f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Feedback & Support Row
                            SettingsLinkRow(
                                title = "Feedback & Support",
                                icon = Icons.Default.Email,
                                iconBg = itemBg,
                                iconTint = activeTheme.textPrimary,
                                onClick = openEmail,
                                activeTheme = activeTheme
                            )

                            HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.6f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Privacy Policy Row
                            SettingsLinkRow(
                                title = "Privacy Policy",
                                icon = Icons.Default.Lock,
                                iconBg = itemBg,
                                iconTint = activeTheme.textSecondary,
                                onClick = { openUrl("https://arrowescape.com/privacy") },
                                activeTheme = activeTheme
                            )

                            HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.6f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Terms of Service Row
                            SettingsLinkRow(
                                title = "Terms of Service",
                                icon = Icons.Default.Policy,
                                iconBg = itemBg,
                                iconTint = activeTheme.textSecondary,
                                onClick = { openUrl("https://arrowescape.com/terms") },
                                activeTheme = activeTheme
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // =========================================================
                    // SECTION 4: VERSION FOOTER (Arrow Decode Removed)
                    // =========================================================
                    Text(
                        text = "Version $versionName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = activeTheme.textSecondary.copy(alpha = 0.65f),
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * Clean, tactile dual-card theme option with palette swatch preview.
 */
@Composable
private fun ThemeOptionCard(
    preset: ThemePreset,
    isSelected: Boolean,
    activeTheme: ThemePreset,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = activeTheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.2.dp,
            color = if (isSelected) activeTheme.textPrimary else activeTheme.gridLine
        ),
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onSelect)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Swatch Box previewing the theme's background and default arrow color
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(preset.background)
                    .then(
                        if (preset.name == "Clean Light") {
                            Modifier.background(
                                color = preset.background,
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Subtle border on the swatch
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Transparent)
                        .then(
                            androidx.compose.ui.Modifier.padding(0.dp)
                        )
                )
                Canvas(modifier = Modifier.size(44.dp)) {
                    drawRoundRect(
                        color = preset.gridLine,
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
                    )
                }

                // Inner palette dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(preset.arrowDefault)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = preset.name,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = activeTheme.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Selection indicator badge
            if (isSelected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(activeTheme.textPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = activeTheme.surface,
                        modifier = Modifier.size(11.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

/**
 * Minimal, clean settings toggle row with theme-harmonized tactile switch.
 */
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(iconBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = title,
                fontSize = 14.5.sp,
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

/**
 * Minimal, single-line link row with chevron indicator.
 */
@Composable
private fun SettingsLinkRow(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit,
    activeTheme: ThemePreset
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
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
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(iconBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = activeTheme.textPrimary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = activeTheme.textSecondary.copy(alpha = 0.45f),
            modifier = Modifier.size(13.dp)
        )
    }
}
