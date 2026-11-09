package com.arrowaway.puzzle.ui.screens

import com.arrowaway.puzzle.ui.components.BannerAdView
import com.arrowaway.puzzle.ui.components.GridGuideIcon

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PrivacyTip
import com.arrowaway.puzzle.ads.ConsentManager
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import kotlin.math.roundToInt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
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
import com.arrowaway.puzzle.ui.theme.ThemePreset
import com.arrowaway.puzzle.ui.theme.ThemePresets

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
    isDevMode: Boolean = false,
    isHintButtonVisible: Boolean = true,
    isGridButtonVisible: Boolean = true,
    hintTimerSeconds: Int = 10,
    isAnalyticsEnabled: Boolean = true,
    onToggleAnalytics: () -> Unit = {},
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onToggleDailyReminders: () -> Unit = {},
    onToggleHintButton: () -> Unit = {},
    onToggleGridButton: () -> Unit = {},
    onHintTimerChange: (Int) -> Unit = {},
    onToggleDevMode: () -> Unit = {},
    onResetGameData: () -> Unit = {},
    onThemeSelect: (ThemePreset) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)
    val context = LocalContext.current
    var showHintTimerInfoDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    // Shown only to users in regions where they must be able to change consent (EEA, UK, etc.)
    val privacyOptionsRequired = remember { ConsentManager.isPrivacyOptionsRequired(context) }

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
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@arrowaway.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Arrow Away Feedback (v$versionName)")
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
                    // SECTION 3: GAMEPLAY CONTROLS (Hint & Grid Settings)
                    // =========================================================
                    Text(
                        text = "GAMEPLAY CONTROLS",
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
                            // Hint Button Visibility Row
                            SettingsToggleRow(
                                title = "Hint Button",
                                icon = Icons.Default.Lightbulb,
                                iconBg = itemBg,
                                iconTint = Color(0xFFF59E0B),
                                isChecked = isHintButtonVisible,
                                onCheckedChange = { onToggleHintButton() },
                                activeTheme = activeTheme
                            )

                            HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.6f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Grid Guide (#) Button Visibility Row
                            SettingsToggleRow(
                                title = "Grid Guide (#) Button",
                                iconBg = itemBg,
                                isChecked = isGridButtonVisible,
                                onCheckedChange = { onToggleGridButton() },
                                activeTheme = activeTheme,
                                customIcon = {
                                    GridGuideIcon(
                                        tint = activeTheme.arrowDefault,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )

                            HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.6f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Hint Timer Configuration Row (Automatically disabled when Hint Button is false)
                            val isTimerActive = isHintButtonVisible
                            val timerControlsAlpha = if (isTimerActive) 1f else 0.45f

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                                .background(itemBg)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = "Hint Timer",
                                                tint = activeTheme.textPrimary.copy(alpha = timerControlsAlpha),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Hint Timer",
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = activeTheme.textPrimary.copy(alpha = timerControlsAlpha),
                                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                            )

                                            // Small information (i) icon
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .clickable { showHintTimerInfoDialog = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Info,
                                                    contentDescription = "Hint Timer Info",
                                                    tint = activeTheme.textSecondary.copy(alpha = if (isTimerActive) 0.85f else 0.55f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    val chipText = when {
                                        !isTimerActive -> "Disabled"
                                        hintTimerSeconds <= 0 -> "Off"
                                        else -> "${hintTimerSeconds}s"
                                    }
                                    val isChipActive = isTimerActive && hintTimerSeconds > 0

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isChipActive) activeTheme.arrowDefault.copy(alpha = 0.15f) else activeTheme.gridLine.copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, if (isChipActive) activeTheme.arrowDefault.copy(alpha = 0.35f) else activeTheme.gridLine.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = chipText,
                                            fontSize = if (isChipActive) 13.sp else 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isChipActive) activeTheme.arrowDefault else activeTheme.textSecondary.copy(alpha = 0.55f),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

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
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // =========================================================
                    // SECTION 4: GAME PROGRESS (Reset data & scores)
                    // =========================================================
                    Text(
                        text = "GAME PROGRESS",
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
                            SettingsLinkRow(
                                title = "Reset Game Progress",
                                icon = Icons.Default.Refresh,
                                iconBg = if (activeTheme.name == "Warm Sepia") Color(0xFFFDE8E8) else Color(0xFFFEE2E2),
                                iconTint = Color(0xFFDC2626),
                                onClick = { showResetConfirmDialog = true },
                                activeTheme = activeTheme
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // =========================================================
                    // SECTION 5: ABOUT & SUPPORT (Clean Consolidated Card)
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
                                title = "Rate Arrow Away",
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

                            // Privacy Policy Row (In-app popup)
                            SettingsLinkRow(
                                title = "Privacy Policy",
                                icon = Icons.Default.Lock,
                                iconBg = itemBg,
                                iconTint = activeTheme.textSecondary,
                                onClick = { showPrivacyPolicyDialog = true },
                                activeTheme = activeTheme
                            )

                            if (privacyOptionsRequired) {
                                HorizontalDivider(
                                    color = cardBorder.copy(alpha = 0.6f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                // Privacy Options Row (re-opens the UMP consent form)
                                SettingsLinkRow(
                                    title = "Privacy Options",
                                    icon = Icons.Default.PrivacyTip,
                                    iconBg = itemBg,
                                    iconTint = activeTheme.textSecondary,
                                    onClick = {
                                        (context as? android.app.Activity)?.let { activity ->
                                            ConsentManager.showPrivacyOptionsForm(activity)
                                        }
                                    },
                                    activeTheme = activeTheme
                                )
                            }

                            HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.6f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Terms & Conditions Row (In-app popup)
                            SettingsLinkRow(
                                title = "Terms & Conditions",
                                icon = Icons.Default.Policy,
                                iconBg = itemBg,
                                iconTint = activeTheme.textSecondary,
                                onClick = { showTermsDialog = true },
                                activeTheme = activeTheme
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // =========================================================
                    // DEVELOPER & TESTING (Gated strictly behind isDevMode)
                    // 100% hidden from regular users in production builds
                    // =========================================================
                    if (isDevMode) {
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "DEVELOPER & TESTING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = cardBg,
                            border = BorderStroke(1.2.dp, Color(0xFFDC2626).copy(alpha = 0.35f)),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SettingsToggleRow(
                                    title = "Analytics Tracking",
                                    icon = Icons.Default.Analytics,
                                    iconBg = itemBg,
                                    iconTint = if (isAnalyticsEnabled) Color(0xFF3B82F6) else activeTheme.textSecondary.copy(alpha = 0.4f),
                                    isChecked = isAnalyticsEnabled,
                                    onCheckedChange = { onToggleAnalytics() },
                                    activeTheme = activeTheme
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // =========================================================
                    // SECTION 5: VERSION FOOTER
                    // Secret: Press & hold for 10s to toggle Dev / Prod mode
                    // =========================================================
                    val haptic = LocalHapticFeedback.current
                    val coroutineScope = rememberCoroutineScope()

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(8.dp))
                            .pointerInput(isDevMode, onToggleDevMode) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val startPos = down.position

                                    val timerJob: Job = coroutineScope.launch {
                                        delay(10_000L)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleDevMode()
                                    }

                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Main)
                                            val pointer = event.changes.firstOrNull { it.id == down.id }
                                            if (pointer == null || !pointer.pressed) {
                                                break
                                            }
                                            val distance = (pointer.position - startPos).getDistance()
                                            if (distance > 120f) {
                                                break
                                            }
                                            pointer.consume()
                                        }
                                    } finally {
                                        timerJob.cancel()
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isDevMode) "Version $versionName • DEV" else "Version $versionName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDevMode) Color(0xFFDC2626) else activeTheme.textSecondary.copy(alpha = 0.65f),
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            BannerAdView(modifier = Modifier.padding(vertical = 4.dp))
        }

        // RESET GAME PROGRESS CONFIRMATION MODAL DIALOG
        if (showResetConfirmDialog) {
            Dialog(
                onDismissRequest = { showResetConfirmDialog = false },
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
                        // Warning Icon Pod
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Reset Progress",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Reset Game Progress?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.textPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "This will reset your campaign level back to Level 1, stars, coins, and streak.\n\nYour theme and sound settings will be preserved.\n\nThis action cannot be undone.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 18.sp,
                            color = activeTheme.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Destructive Reset Button
                        Button(
                            onClick = {
                                showResetConfirmDialog = false
                                onResetGameData()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "Reset Everything",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Safe Cancel Button
                        Button(
                            onClick = { showResetConfirmDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = activeTheme.textPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text(
                                text = "Keep Playing",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // HINT TIMER EXPLANATION MODAL DIALOG
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
                        // Header Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Hint Timer",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.textPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

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

        // PRIVACY POLICY MODAL DIALOG
        if (showPrivacyPolicyDialog) {
            Dialog(
                onDismissRequest = { showPrivacyPolicyDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardBg,
                    border = BorderStroke(1.2.dp, cardBorder),
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .widthIn(max = 350.dp)
                        .fillMaxWidth(0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Privacy Policy",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.textPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Your privacy is important to us. Here is how Arrow Away handles your information:\n\n" +
                                       "• No Account Needed: We do not ask for your name, email, phone number, or contacts.\n\n" +
                                       "• Local Game Data: Your progress, coins, and settings are stored only on your device.\n\n" +
                                       "• Analytics & Stability: Google Firebase collects gameplay events, device info, and crash reports to help us improve the game and fix bugs.\n\n" +
                                       "• Advertisements: Google AdMob shows ads and may use your device's advertising ID. Where required by law, we ask for your consent first.\n\n" +
                                       "• Your Choices: Where available, change your ad consent in Settings > Privacy Options. You can reset your advertising ID in your device settings.\n\n" +
                                       "Full policy: sites.google.com/view/arrow-away/home\n" +
                                       "Contact: application.smart.developer@gmail.com",
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = activeTheme.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { showPrivacyPolicyDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeTheme.arrowDefault,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
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

        // TERMS & CONDITIONS MODAL DIALOG
        if (showTermsDialog) {
            Dialog(
                onDismissRequest = { showTermsDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardBg,
                    border = BorderStroke(1.2.dp, cardBorder),
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .widthIn(max = 350.dp)
                        .fillMaxWidth(0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Policy,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Terms & Conditions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.textPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Welcome to Arrow Away! By downloading and playing, you agree to these simple terms:\n\n" +
                                       "• Entertainment Use: Arrow Away is a puzzle game provided for personal, non-commercial entertainment. Please play fairly and enjoy the challenges!\n\n" +
                                       "• Virtual Items: In-game items like hints, boosters, and coins are purely virtual game features with no real-world monetary value. They cannot be sold, traded, or redeemed.\n\n" +
                                       "• Updates & Content: We continually improve the game by adding new puzzles, features, and optimizations. Game content may be updated from time to time.\n\n" +
                                       "• As-Is Availability: While we work hard to provide a smooth, enjoyable experience, the game is provided 'as is'. We are not responsible for lost progress resulting from device resets or clearing app data.\n\n" +
                                       "Thank you for playing and supporting Arrow Away!",
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = activeTheme.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { showTermsDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeTheme.arrowDefault,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
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
    iconBg: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeTheme: ThemePreset,
    icon: ImageVector? = null,
    iconTint: Color = Color.Unspecified,
    customIcon: (@Composable () -> Unit)? = null
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
                if (customIcon != null) {
                    customIcon()
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
