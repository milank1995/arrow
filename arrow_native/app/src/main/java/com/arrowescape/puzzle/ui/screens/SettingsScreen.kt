package com.arrowescape.puzzle.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.theme.ThemePreset
import com.arrowescape.puzzle.ui.theme.ThemePresets

@Composable
fun SettingsScreen(
    isSoundEnabled: Boolean,
    isVibrationEnabled: Boolean,
    activeTheme: ThemePreset,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
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
                putExtra(Intent.EXTRA_SUBJECT, "Arrow Escape Feedback (v$versionName)")
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

    val screenBg = if (activeTheme.isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (activeTheme.isDark) Color(0xFF1E293B) else Color.White
    val cardBorder = if (activeTheme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val itemBg = if (activeTheme.isDark) Color(0xFF273549) else Color(0xFFF8FAFC)

    Surface(
        color = screenBg,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
        ) {
            // TOP APP BAR: Back Button + Centered Title + Visual Balance Spacer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                Surface(
                    shape = CircleShape,
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    shadowElevation = 1.dp,
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

                // Centered Clean Title (Matching overall UI design)
                Text(
                    text = "Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = activeTheme.textPrimary,
                    style = androidx.compose.ui.text.TextStyle(
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                    )
                )

                // Dummy Spacer for symmetry
                Spacer(modifier = Modifier.size(42.dp))
            }

            // SCROLLABLE SETTINGS CONTENT (Responsive width container max 560dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                ) {
                    // =========================================================
                    // SECTION 1: BOARD THEME SELECTOR
                    // =========================================================
                    SectionHeader(
                        title = "BOARD THEME",
                        icon = Icons.Default.Palette,
                        theme = activeTheme
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal scrolling Theme Swatch row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemePresets.all.forEach { preset ->
                            val isSelected = (preset.name == activeTheme.name)
                            ThemeCard(
                                preset = preset,
                                isSelected = isSelected,
                                activeTheme = activeTheme,
                                onSelect = { onThemeSelect(preset) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // =========================================================
                    // SECTION 2: AUDIO & HAPTICS (Grouped Card)
                    // =========================================================
                    SectionHeader(
                        title = "AUDIO & HAPTICS",
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        theme = activeTheme
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Sound Effects Row
                            SettingsToggleRow(
                                title = "Sound Effects",
                                icon = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                                iconBg = if (isSoundEnabled) activeTheme.arrowHint.copy(alpha = 0.16f) else activeTheme.gridLine.copy(alpha = 0.35f),
                                iconTint = if (isSoundEnabled) activeTheme.arrowHint else activeTheme.textSecondary,
                                isChecked = isSoundEnabled,
                                onCheckedChange = { onToggleSound() },
                                activeTheme = activeTheme
                            )

                            androidx.compose.material3.HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.6f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Haptic Vibration Row
                            SettingsToggleRow(
                                title = "Haptic Vibration",
                                icon = Icons.Default.Vibration,
                                iconBg = if (isVibrationEnabled) activeTheme.arrowDefault.copy(alpha = 0.16f) else activeTheme.gridLine.copy(alpha = 0.35f),
                                iconTint = if (isVibrationEnabled) activeTheme.arrowDefault else activeTheme.textSecondary,
                                isChecked = isVibrationEnabled,
                                onCheckedChange = { onToggleVibration() },
                                activeTheme = activeTheme
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // =========================================================
                    // SECTION 3: SUPPORT & COMMUNITY (Grouped Card)
                    // =========================================================
                    SectionHeader(
                        title = "SUPPORT & COMMUNITY",
                        icon = Icons.Default.Star,
                        theme = activeTheme
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingsLinkRow(
                                title = "Rate Us",
                                subtitle = "Support us with 5 stars on Play Store",
                                icon = Icons.Default.Star,
                                iconBg = activeTheme.arrowHint.copy(alpha = 0.16f),
                                iconTint = activeTheme.arrowHint,
                                onClick = openRateUs,
                                activeTheme = activeTheme
                            )

                            androidx.compose.material3.HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.6f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            SettingsLinkRow(
                                title = "Feedback & Support",
                                subtitle = "Contact the development team",
                                icon = Icons.Default.Email,
                                iconBg = activeTheme.arrowClear.copy(alpha = 0.16f),
                                iconTint = activeTheme.arrowClear,
                                onClick = openEmail,
                                activeTheme = activeTheme
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // =========================================================
                    // SECTION 4: LEGAL & PRIVACY (Grouped Card)
                    // =========================================================
                    SectionHeader(
                        title = "LEGAL & PRIVACY",
                        icon = Icons.Default.Lock,
                        theme = activeTheme
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingsLinkRow(
                                title = "Privacy Policy",
                                subtitle = "How we protect your data",
                                icon = Icons.Default.Lock,
                                iconBg = activeTheme.gridLine.copy(alpha = 0.4f),
                                iconTint = activeTheme.textSecondary,
                                onClick = { openUrl("https://arrowescape.com/privacy") },
                                activeTheme = activeTheme
                            )

                            androidx.compose.material3.HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.6f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            SettingsLinkRow(
                                title = "Terms of Service",
                                subtitle = "Terms and conditions of use",
                                icon = Icons.Default.Policy,
                                iconBg = activeTheme.gridLine.copy(alpha = 0.4f),
                                iconTint = activeTheme.textSecondary,
                                onClick = { openUrl("https://arrowescape.com/terms") },
                                activeTheme = activeTheme
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // =========================================================
                    // SECTION 5: BRANDING & VERSION FOOTER
                    // =========================================================
                    Text(
                        text = "Arrow Decode • Version $versionName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = activeTheme.textSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    theme: ThemePreset
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = theme.arrowDefault,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textSecondary,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun ThemeCard(
    preset: ThemePreset,
    isSelected: Boolean,
    activeTheme: ThemePreset,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = preset.background,
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) activeTheme.arrowDefault else preset.gridLine
        ),
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        modifier = Modifier
            .size(width = 104.dp, height = 76.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color dots representing preset palette
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(preset.arrowDefault)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(preset.gridDot)
                    )
                }

                if (isSelected) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(activeTheme.arrowDefault)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = if (activeTheme.isDark) Color(0xFF0F172A) else Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Text(
                text = preset.name,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = preset.textPrimary,
                maxLines = 1
            )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = title,
                fontSize = 15.sp,
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
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
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

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = activeTheme.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = activeTheme.textSecondary
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = activeTheme.textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}
