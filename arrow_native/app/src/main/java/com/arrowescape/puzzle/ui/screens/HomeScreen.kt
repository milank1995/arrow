package com.arrowescape.puzzle.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.game.GameUiState
import com.arrowescape.puzzle.ui.components.LevelSelectDialog
import com.arrowescape.puzzle.ui.theme.ThemePreset

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
    onSelectLevel: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showLevelSelectDialog by remember { mutableStateOf(false) }
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

    Surface(
        color = Color(0xFFF8FAFC),
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
            // 1. TOP BAR: Streak Pill (left) & Dev Test Lab + Settings Gear Pod (right)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                // Streak Pill Indicator
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = Color(0xFFFFFBEB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(text = "🔥", fontSize = 16.sp)
                        Text(
                            text = "${uiState.streakDays} Day Streak",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                }

                // Right Group: Dev Test Lab Pill + Quick Sound Toggle + Settings Menu Pod
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Dev Test Lab Pill
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = Color(0xFFFEF3C7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .height(40.dp)
                            .clickable(onClick = onDevTestClick)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(text = "🧪", fontSize = 15.sp)
                            Text(
                                text = "Test Lab",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    // Quick Sound Toggle Button (Speaker Icon)
                    Surface(
                        shape = CircleShape,
                        color = if (isSoundOn) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSoundOn) Color(0xFFFDE68A) else Color(0xFFE2E8F0)
                        ),
                        shadowElevation = 1.dp,
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(
                            onClick = onSoundToggle,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isSoundOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                                contentDescription = if (isSoundOn) "Mute Sound" else "Enable Sound",
                                tint = if (isSoundOn) Color(0xFFD97706) else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Settings / Audio Menu Gear Button
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp,
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Audio & Settings Menu",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // 2. CENTER HERO: Tactile Stage Card (Celebratory Stars, Large Number, Stage Badge) - NO "ARROW ESCAPE"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEDE9FE)),
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .fillMaxWidth(0.85f)
                        .clickable { showLevelSelectDialog = true }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFAFAFE),
                                        Color(0xFFF5F3FF)
                                    )
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 28.dp)
                    ) {
                        // 3 Celebratory Mastery Stars for Current Level
                        val currentStars = uiState.levelStars[uiState.currentLevelNumber] ?: 0
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val starSizes = listOf(22.dp, 30.dp, 22.dp)
                            for (s in 1..3) {
                                val isEarned = (currentStars >= s) || (currentStars == 0 && uiState.currentLevelNumber == 1)
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Star $s",
                                    tint = if (isEarned) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                                    modifier = Modifier.size(starSizes[s - 1])
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bold, prominent Level Number
                        Text(
                            text = "${uiState.currentLevelNumber}",
                            fontSize = 88.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF6366F1),
                            lineHeight = 90.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Refined "LEVEL" tracking badge
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = Color(0xFFEEF2FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7D2FE))
                        ) {
                            Text(
                                text = "L E V E L",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 6.sp,
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to choose level 🗺️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // 3. BOTTOM BUTTONS: 3D Tactile Play Pill + Modern Slate Daily Challenge Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Primary Play 3D Pill Button with physical bevel shadow
                Box(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth(0.88f)
                        .height(68.dp)
                        .scale(playScale)
                        .clickable(onClick = onPlayClick),
                    contentAlignment = Alignment.Center
                ) {
                    // 3D Bottom Bevel Layer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color(0xFF047857))
                    )
                    // Top Surface Gradient Layer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF34D399), // Emerald highlight
                                        Color(0xFF059669)  // Emerald body
                                    )
                                )
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Crisp circular pod with play icon
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Icon",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            // Centered bold "PLAY" text
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "PLAY",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = Color.White
                                )
                            }

                            // Balancer spacer
                            Spacer(modifier = Modifier.size(46.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Challenge Capsule: Locked below Level 5 (Unity DailyChallengeUnlockLevel = 5), dynamic when unlocked
                val isUnlocked = uiState.currentLevelNumber >= 5
                val todayStr = remember { LocalDate.now().toString() }
                val isCompletedToday = uiState.completedDailyDates.contains(todayStr)

                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = if (isUnlocked && !isCompletedToday) Color(0xFF1E293B) else Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isUnlocked && !isCompletedToday) Color(0xFFD97706) else Color(0xFF334155)
                    ),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth(0.88f)
                        .height(62.dp)
                        .clickable(onClick = onDailyChallengeClick)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        // Dynamic Icon Badge
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !isUnlocked -> Color(0xFF334155)
                                        isCompletedToday -> Color(0xFF064E3B)
                                        else -> Color(0xFF78350F)
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
                                    !isUnlocked -> Color(0xFFFBBF24)
                                    isCompletedToday -> Color(0xFF34D399)
                                    else -> Color(0xFFFBBF24)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Text column
                        Column {
                            Text(
                                text = "Daily Challenge",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = when {
                                    !isUnlocked -> "Unlocks at Level 5"
                                    isCompletedToday -> "✓ Completed Today"
                                    else -> "Today's Challenge • +100 Coins"
                                },
                                fontSize = 12.sp,
                                color = when {
                                    !isUnlocked -> Color(0xFF94A3B8)
                                    isCompletedToday -> Color(0xFF34D399)
                                    else -> Color(0xFFFDE68A)
                                },
                                fontWeight = if (isUnlocked && !isCompletedToday) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLevelSelectDialog) {
        LevelSelectDialog(
            currentLevelNumber = uiState.currentLevelNumber,
            lastCompletedLevelNumber = uiState.lastCompletedLevelNumber,
            levelStars = uiState.levelStars,
            theme = theme,
            onSelectLevel = { selectedLevel ->
                onSelectLevel(selectedLevel)
            },
            onDismiss = { showLevelSelectDialog = false }
        )
    }
}
