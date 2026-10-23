package com.arrowescape.puzzle.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import java.time.LocalDate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.game.GameUiState
import com.arrowescape.puzzle.ui.components.AppPrimaryButton
import com.arrowescape.puzzle.ui.components.SculptedStar3D
import com.arrowescape.puzzle.ui.components.StreakChip
import com.arrowescape.puzzle.ui.theme.ThemePreset

/**
 * Modern, minimal, theme-adaptive homepage.
 * Pure linear progression:
 * - Current level only (no past or future levels).
 * - No level selection / jumping.
 * - No redundant coin counter.
 * - 100% reactive to ThemePreset across all elements with mathematically centered typography.
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
    onDevTestClick: () -> Unit = {},
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

    // Dynamic theme-based CTA button palette
    val (ctaGrad1, ctaGrad2, ctaBevel) = when (theme.name) {
        "Warm Sepia" -> Triple(Color(0xFFE09F3E), Color(0xFFC57B22), Color(0xFF8C5314))
        "Midnight Dark" -> Triple(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0369A1))
        "Calm Pastel" -> Triple(Color(0xFF8B5CF6), Color(0xFF7C3AED), Color(0xFF6D28D9))
        "Mint Fresh" -> Triple(Color(0xFF059669), Color(0xFF047857), Color(0xFF064E3B))
        "Sunset Rose" -> Triple(Color(0xFFE11D48), Color(0xFFBE123C), Color(0xFF9F1239))
        else -> Triple(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857)) // Clean Light
    }

    Surface(
        color = theme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // 1. TOP STATUS BAR: Streak Pill (left) & Settings Gear (right)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                // Streak Pill Indicator
                StreakChip(
                    streakDays = uiState.streakDays,
                    theme = theme
                )

                // Settings Gear Button
                Surface(
                    shape = CircleShape,
                    color = theme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, theme.gridLine),
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
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 2. CENTER HERO: Minimalist Title + Current Stage Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Minimalist Brand Title
                Text(
                    text = "ARROW DECODE",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    color = theme.textPrimary.copy(alpha = 0.90f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "MINDFUL PUZZLE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = theme.textSecondary.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Hero Stage Card (Strictly Current Level Only)
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = theme.surface,
                    shadowElevation = if (theme.isDark) 5.dp else 9.dp,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, theme.gridLine),
                    modifier = Modifier
                        .widthIn(max = 330.dp)
                        .fillMaxWidth(0.85f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        theme.surface,
                                        theme.background.copy(alpha = 0.45f)
                                    )
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 26.dp)
                    ) {
                        // 3 Celebratory Mastery Stars for Current Level
                        val currentStars = uiState.levelStars[uiState.currentLevelNumber] ?: 0
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(38.dp)
                        ) {
                            SculptedStar3D(
                                isEarned = (currentStars >= 1) || (uiState.currentLevelNumber > 1 && currentStars == 0),
                                size = 24.dp,
                                rotation = -8f,
                                modifier = Modifier.offset(y = 3.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            SculptedStar3D(
                                isEarned = (currentStars >= 2) || (uiState.currentLevelNumber > 1 && currentStars == 0),
                                size = 32.dp,
                                haloAura = (currentStars >= 2),
                                modifier = Modifier.offset(y = (-2).dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            SculptedStar3D(
                                isEarned = (currentStars >= 3) || (uiState.currentLevelNumber > 1 && currentStars == 0),
                                size = 24.dp,
                                rotation = 8f,
                                modifier = Modifier.offset(y = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bold, prominent Level Number in theme color
                        Text(
                            text = "${uiState.currentLevelNumber}",
                            fontSize = 84.sp,
                            fontWeight = FontWeight.Black,
                            color = theme.textPrimary,
                            lineHeight = 84.sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Refined "LEVEL" tracking badge
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = theme.background,
                            border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine)
                        ) {
                            Text(
                                text = "L E V E L",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 5.sp,
                                color = theme.textSecondary,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 3. BOTTOM ACTIONS: Theme-Adaptive Daily Challenge + 3D Tactile Play CTA
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Daily Challenge Card: Standardized 16dp corner radius & 54dp height matching design system
                val isUnlocked = uiState.currentLevelNumber > 5 || uiState.lastCompletedLevelNumber >= 5 || uiState.hasSeenDailyUnlockCelebration
                val todayStr = remember { LocalDate.now().toString() }
                val isCompletedToday = uiState.completedDailyDates.contains(todayStr)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = theme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isUnlocked && !isCompletedToday) 1.5.dp else 1.2.dp,
                        color = when {
                            !isUnlocked -> theme.gridLine
                            isCompletedToday -> if (theme.name == "Warm Sepia") Color(0xFFB45309) else theme.arrowClear
                            else -> theme.arrowHint.copy(alpha = dailyReadyAlpha)
                        }
                    ),
                    shadowElevation = if (isUnlocked && !isCompletedToday) 2.dp else 1.dp,
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth(0.88f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
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
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        !isUnlocked -> theme.gridLine
                                        isCompletedToday -> if (theme.name == "Warm Sepia") Color(0xFFB45309).copy(alpha = 0.18f) else theme.arrowClear.copy(alpha = 0.18f)
                                        else -> theme.arrowHint.copy(alpha = 0.18f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    !isUnlocked -> Icons.Default.Lock
                                    isCompletedToday -> Icons.Default.Check
                                    else -> Icons.Default.DateRange
                                },
                                contentDescription = if (isUnlocked) "Daily Challenge" else "Locked",
                                tint = when {
                                    !isUnlocked -> theme.textSecondary
                                    isCompletedToday -> if (theme.name == "Warm Sepia") Color(0xFFB45309) else theme.arrowClear
                                    else -> theme.arrowHint
                                },
                                modifier = Modifier.size(18.dp)
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
                            if (isUnlocked && !isCompletedToday) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(theme.arrowHint)
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
                                        !isUnlocked -> "Unlocks after Level 5"
                                        isCompletedToday -> "✓ Completed Today"
                                        else -> "Special Daily Puzzle"
                                    },
                                    fontSize = 11.sp,
                                    color = when {
                                        !isUnlocked -> theme.textSecondary
                                        isCompletedToday -> if (theme.name == "Warm Sepia") Color(0xFFB45309) else theme.arrowClear
                                        else -> theme.textSecondary
                                    },
                                    fontWeight = if (isCompletedToday) FontWeight.SemiBold else FontWeight.Medium,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }
                        }

                        // Trailing Chip Badge
                        if (isUnlocked && !isCompletedToday) {
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = theme.arrowHint.copy(alpha = 0.16f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, theme.arrowHint.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "READY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = theme.arrowHint,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        } else if (isCompletedToday) {
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = theme.arrowClear.copy(alpha = 0.16f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, theme.arrowClear.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "DONE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = theme.arrowClear,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Play Flat Button
                AppPrimaryButton(
                    text = "Play Level ${uiState.currentLevelNumber}",
                    onClick = onPlayClick,
                    theme = theme,
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth(0.88f)
                        .scale(playScale)
                )
            }
        }
    }
}
