package com.arrowescape.puzzle.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.BuildConfig
import com.arrowescape.puzzle.game.GameUiState
import com.arrowescape.puzzle.ui.components.SculptedStar3D
import com.arrowescape.puzzle.ui.components.StreakChip
import com.arrowescape.puzzle.ui.theme.ThemePreset
import java.time.LocalDate

/**
 * Modern, minimal, theme-adaptive homepage for Arrow Decode.
 * Features:
 * - Centered vertical hierarchy: Level Card -> Play Button -> Daily Challenge Button.
 * - Compact vertical orientation (max 284dp width) with generous side margins to avoid horizontal stretching.
 * - Statuesque portrait Level Card displaying 3D podium stars and level numeral with zero clutter.
 * - Substantial 62dp primary Play CTA and 62dp Daily Challenge feature card.
 * - Soft, subtle ambient radial aura with faint artistic watermark silhouettes (no spinning rays).
 * - Clean separation: 0 dev clutter in production; quick steppers & dev shelf gated behind isDevMode.
 */
@Composable
fun HomeScreen(
    uiState: GameUiState,
    theme: ThemePreset,
    isSoundOn: Boolean,
    onPlayClick: () -> Unit,
    onDailyChallengeClick: () -> Unit,
    onThemeClick: () -> Unit,
    onSoundToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    onLockPuzzleClick: () -> Unit = {},
    onDevTestClick: () -> Unit = {},
    onLevelChange: (Int) -> Unit = {},
    isDevMode: Boolean = BuildConfig.DEBUG,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "playPulse")
    val playScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playScale"
    )

    val dailyReadyAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dailyReadyAlpha"
    )

    // Daily Challenge status
    val isDailyUnlocked = uiState.currentLevelNumber > 10 ||
            uiState.lastCompletedLevelNumber >= 10 ||
            uiState.hasSeenDailyUnlockCelebration
    val todayStr = remember { LocalDate.now().toString() }
    val isDailyCompletedToday = uiState.completedDailyDates.contains(todayStr)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        // 1. ATMOSPHERIC BACKGROUND: Soft Blooming Radial Aura + Faint Watermark Silhouettes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height * 0.48f
            val auraRadius = size.height * 0.42f

            val auraColor = when (theme.name) {
                "Warm Sepia" -> Color(0xFFE09F3E).copy(alpha = 0.16f)
                "Midnight Dark" -> Color(0xFF38BDF8).copy(alpha = 0.14f)
                "Mint Fresh" -> Color(0xFF059669).copy(alpha = 0.14f)
                "Clean Light" -> Color(0xFF1E293B).copy(alpha = 0.08f)
                "Calm Pastel" -> Color(0xFF9333EA).copy(alpha = 0.14f)
                "Sunset Rose" -> Color(0xFFE11D48).copy(alpha = 0.14f)
                else -> theme.arrowDefault.copy(alpha = 0.12f)
            }

            // Blooming Radial Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(auraColor, Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = Offset(centerX, centerY)
            )

            // Faint decorative watermark arrows (3–3.5% opacity) for atmospheric depth
            drawWatermarkArrow(
                center = Offset(size.width * 0.88f, size.height * 0.16f),
                size = 140.dp.toPx(),
                angleDegrees = 25f,
                color = theme.textPrimary.copy(alpha = 0.035f)
            )
            drawWatermarkArrow(
                center = Offset(size.width * 0.12f, size.height * 0.84f),
                size = 160.dp.toPx(),
                angleDegrees = -20f,
                color = theme.textPrimary.copy(alpha = 0.03f)
            )
        }

        // 2. FOREGROUND CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // TOP STATUS BAR (Fixed at Top)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                // Streak Chip
                StreakChip(
                    streakDays = uiState.streakDays,
                    theme = theme
                )

                // Optional Dev Badge
                if (isDevMode) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "🛠️ DEV BUILD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEF4444),
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                // Settings Gear Button
                Surface(
                    shape = CircleShape,
                    color = theme.surface,
                    border = BorderStroke(1.2.dp, theme.gridLine),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings Menu",
                            tint = theme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // OPTICALLY CENTERED MAIN BODY STACK
            // Unified in the vertical center of available space with compact max 284dp width
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .widthIn(max = 284.dp)
                        .fillMaxWidth()
                ) {
                    // Brand Header with insignia
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Subtle architectural brand insignia (minimalist chevron motif)
                        Canvas(modifier = Modifier.size(24.dp, 14.dp)) {
                            val path = Path().apply {
                                moveTo(2.dp.toPx(), 12.dp.toPx())
                                lineTo(12.dp.toPx(), 3.dp.toPx())
                                lineTo(22.dp.toPx(), 12.dp.toPx())
                            }
                            drawPath(
                                path = path,
                                color = theme.textSecondary.copy(alpha = 0.55f),
                                style = Stroke(
                                    width = 2.2.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                            drawCircle(
                                color = theme.textSecondary.copy(alpha = 0.55f),
                                radius = 1.6.dp.toPx(),
                                center = Offset(12.dp.toPx(), 8.dp.toPx())
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "ARROW DECODE",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 5.sp,
                            color = theme.textPrimary.copy(alpha = 0.92f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "MINDFUL PUZZLE",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp,
                            color = theme.textSecondary.copy(alpha = 0.60f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 1. LEVEL CARD (Pure & Statuesque: 3D Stars + Large Level Number only)
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = theme.surface,
                        shadowElevation = if (theme.isDark) 6.dp else 10.dp,
                        border = BorderStroke(1.5.dp, theme.gridLine),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            theme.surface,
                                            theme.background.copy(alpha = 0.35f)
                                        )
                                    )
                                )
                                .padding(horizontal = 20.dp, vertical = 26.dp)
                        ) {
                            // 3 Celebratory Mastery Stars for Current Level
                            val currentStars = uiState.levelStars[uiState.currentLevelNumber] ?: 0
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.height(44.dp)
                            ) {
                                SculptedStar3D(
                                    isEarned = (currentStars >= 1) || (uiState.currentLevelNumber > 1 && currentStars == 0),
                                    size = 26.dp,
                                    rotation = -8f,
                                    modifier = Modifier.offset(y = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                SculptedStar3D(
                                    isEarned = (currentStars >= 2) || (uiState.currentLevelNumber > 1 && currentStars == 0),
                                    size = 36.dp,
                                    haloAura = (currentStars >= 2),
                                    modifier = Modifier.offset(y = (-4).dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                SculptedStar3D(
                                    isEarned = (currentStars >= 3) || (uiState.currentLevelNumber > 1 && currentStars == 0),
                                    size = 26.dp,
                                    rotation = 8f,
                                    modifier = Modifier.offset(y = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Bold, prominent Level Number with optional Dev Steppers
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isDevMode) {
                                    Surface(
                                        shape = CircleShape,
                                        color = theme.background,
                                        border = BorderStroke(1.2.dp, theme.gridLine),
                                        shadowElevation = 1.dp,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                val prev = (uiState.currentLevelNumber - 1).coerceAtLeast(1)
                                                onLevelChange(prev)
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "◀",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = theme.textPrimary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                Text(
                                    text = "${uiState.currentLevelNumber}",
                                    fontSize = 94.sp,
                                    fontWeight = FontWeight.Black,
                                    color = theme.textPrimary,
                                    lineHeight = 94.sp,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                    modifier = if (isDevMode) {
                                        Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable(onClick = onDevTestClick)
                                    } else Modifier
                                )

                                if (isDevMode) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = theme.background,
                                        border = BorderStroke(1.2.dp, theme.gridLine),
                                        shadowElevation = 1.dp,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                val next = (uiState.currentLevelNumber + 1).coerceAtMost(151)
                                                onLevelChange(next)
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "▶",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = theme.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. PLAY BUTTON (Directly Below Level Card — 62dp Tactile CTA)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = theme.arrowDefault,
                        shadowElevation = if (theme.isDark) 4.dp else 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .scale(playScale)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onPlayClick)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDevMode) "PLAY LEVEL ${uiState.currentLevelNumber}" else "PLAY",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                color = Color.White,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. DAILY CHALLENGE BUTTON (Below Play Button — 62dp Feature Card)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = theme.surface,
                        border = BorderStroke(
                            width = if (isDailyUnlocked && !isDailyCompletedToday) 1.5.dp else 1.2.dp,
                            color = when {
                                !isDailyUnlocked -> theme.gridLine
                                isDailyCompletedToday -> if (theme.name == "Warm Sepia") Color(0xFFB45309) else theme.arrowClear
                                else -> theme.arrowHint.copy(alpha = dailyReadyAlpha)
                            }
                        ),
                        shadowElevation = if (isDailyUnlocked && !isDailyCompletedToday) 2.dp else 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onDailyChallengeClick)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp)
                        ) {
                            // Dynamic Theme-Harmonized Icon Pod
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            !isDailyUnlocked -> theme.gridLine.copy(alpha = 0.5f)
                                            isDailyCompletedToday -> if (theme.name == "Warm Sepia") Color(0xFFB45309).copy(alpha = 0.18f) else theme.arrowClear.copy(alpha = 0.18f)
                                            else -> theme.arrowHint.copy(alpha = 0.18f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        !isDailyUnlocked -> Icons.Default.Lock
                                        isDailyCompletedToday -> Icons.Default.Check
                                        else -> Icons.Default.DateRange
                                    },
                                    contentDescription = if (isDailyUnlocked) "Daily Challenge" else "Locked",
                                    tint = when {
                                        !isDailyUnlocked -> theme.textSecondary
                                        isDailyCompletedToday -> if (theme.name == "Warm Sepia") Color(0xFFB45309) else theme.arrowClear
                                        else -> theme.arrowHint
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Text column with status indicator
                            Column(
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Daily Challenge",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textPrimary,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (isDailyUnlocked && !isDailyCompletedToday) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                        Text(
                                            text = "Today's Puzzle Ready",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = theme.arrowHint,
                                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                        )
                                    }
                                } else {
                                    Text(
                                        text = when {
                                            !isDailyUnlocked -> "Unlocks after Level 10"
                                            isDailyCompletedToday -> "✓ Completed Today"
                                            else -> "Special Daily Puzzle"
                                        },
                                        fontSize = 11.sp,
                                        color = when {
                                            !isDailyUnlocked -> theme.textSecondary
                                            isDailyCompletedToday -> if (theme.name == "Warm Sepia") Color(0xFFB45309) else theme.arrowClear
                                            else -> theme.textSecondary
                                        },
                                        fontWeight = if (isDailyCompletedToday) FontWeight.SemiBold else FontWeight.Medium,
                                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                    )
                                }
                            }

                            // Trailing Chip Badge
                            if (isDailyUnlocked && !isDailyCompletedToday) {
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = theme.arrowHint.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, theme.arrowHint.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "READY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = theme.arrowHint,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            } else if (isDailyCompletedToday) {
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = theme.arrowClear.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, theme.arrowClear.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "DONE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = theme.arrowClear,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Dev Secondary Shelf (Only visible in Dev Mode)
                    if (isDevMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Jump Level Button
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = theme.surface,
                                border = BorderStroke(1.dp, theme.gridLine),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = onDevTestClick)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "🎯 Jump Level",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textPrimary
                                    )
                                }
                            }

                            // Lock Puzzle Button
                            val isLockCompleted = (uiState.levelStars[151] ?: 0) > 0 || uiState.lastCompletedLevelNumber >= 151
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = theme.surface,
                                border = BorderStroke(1.dp, theme.gridLine),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = onLockPuzzleClick)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = if (isLockCompleted) "✓ Lock Puzzle" else "🔓 Lock Puzzle",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLockCompleted) {
                                            if (theme.name == "Warm Sepia") Color(0xFFB45309) else theme.arrowClear
                                        } else {
                                            theme.textPrimary
                                        }
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

/**
 * Renders a faint artistic chevron arrow watermark silhouette in the background canvas.
 */
private fun DrawScope.drawWatermarkArrow(
    center: Offset,
    size: Float,
    angleDegrees: Float,
    color: Color
) {
    rotate(degrees = angleDegrees, pivot = center) {
        val half = size / 2f
        val path = Path().apply {
            moveTo(center.x - half * 0.4f, center.y - half)
            lineTo(center.x + half * 0.5f, center.y)
            lineTo(center.x - half * 0.4f, center.y + half)
            lineTo(center.x - half * 0.1f, center.y)
            close()
        }
        drawPath(path = path, color = color)
    }
}
