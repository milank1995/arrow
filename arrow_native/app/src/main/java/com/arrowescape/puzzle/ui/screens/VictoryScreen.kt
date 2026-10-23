package com.arrowescape.puzzle.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.components.AppPrimaryButton
import com.arrowescape.puzzle.ui.components.AppSecondaryButton
import com.arrowescape.puzzle.ui.components.ConfettiBurstEffect
import com.arrowescape.puzzle.ui.components.SculptedStar3D
import com.arrowescape.puzzle.ui.components.StreakChip
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modern, card-anchored Victory Screen matching the Unity One design benchmark (Image 4).
 * Features:
 * - Triumphant 3D sculpted star podium arch crowning the hero card.
 * - Gentle ambient revolving sunburst + festive drifting confetti particles.
 * - Meaningful gameplay performance statistics (Difficulty, Time, Score, Mistakes, Accuracy).
 * - Standardized tactile 3D theme CTA buttons (Next Level + Home).
 * - 100% theme-adaptive & fully responsive across all device screen ratios.
 */
@Composable
fun VictoryScreen(
    levelNumber: Int,
    coinsEarned: Int,
    streakDays: Int,
    totalCoins: Int,
    theme: ThemePreset,
    heartsRemaining: Int = 3,
    maxHearts: Int = 3,
    isDailyChallenge: Boolean = false,
    completionTimeSeconds: Int = 0,
    movesCount: Int = 0,
    score: Int = 0,
    accuracyPercent: Int = 100,
    starsEarned: Int = 3,
    isDailyUnlockMilestone: Boolean = false,
    onStartDailyChallengeClick: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onClaimDoubleCoins: (Int) -> Unit = {},
    onNextLevelClick: () -> Unit,
    onHomeClick: () -> Unit,
    onPlayStarSound: (Int) -> Unit = {},
    onPlayCoinSound: () -> Unit = {},
    onPlayClickSound: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Dynamic theme-based CTA button palette
    val (ctaGrad1, ctaGrad2, ctaBevel) = when (theme.name) {
        "Warm Sepia" -> Triple(Color(0xFFE09F3E), Color(0xFFC57B22), Color(0xFF8C5314))
        "Midnight Dark" -> Triple(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0369A1))
        "Calm Pastel" -> Triple(Color(0xFF8B5CF6), Color(0xFF7C3AED), Color(0xFF6D28D9))
        "Mint Fresh" -> Triple(Color(0xFF059669), Color(0xFF047857), Color(0xFF064E3B))
        "Sunset Rose" -> Triple(Color(0xFFE11D48), Color(0xFFBE123C), Color(0xFF9F1239))
        else -> Triple(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857)) // Clean Light
    }

    // Entrance Animations
    val contentScale = remember { Animatable(0.90f) }
    val contentAlpha = remember { Animatable(0f) }
    val star1Scale = remember { Animatable(0f) }
    val star2Scale = remember { Animatable(0f) }
    val star3Scale = remember { Animatable(0f) }
    val sunburstAlpha = remember { Animatable(0f) }

    // Fire Flight Animation States for Level 5 Daily Challenge Unlock Celebration
    val flameScale = remember { Animatable(0f) }
    val flameAlpha = remember { Animatable(0f) }
    val flightProgress = remember { Animatable(0f) }
    var isStreakDocked by remember { mutableStateOf(!isDailyUnlockMilestone) }
    val dockPulseScale = remember { Animatable(1f) }

    if (isDailyUnlockMilestone) {
        LaunchedEffect(Unit) {
            // 1. Wait for stars to land and celebrate (~750ms)
            delay(750)

            // 2. Central Flame Pop: Scales up and glows in the center
            launch {
                flameAlpha.animateTo(1f, tween(240))
            }
            flameScale.animateTo(
                targetValue = 1.35f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            flameScale.animateTo(1.0f, tween(180))

            delay(350) // Let the player see the flame in center

            // 3. Flight: Smoothly glide from center to top-left corner
            flightProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(650, easing = FastOutSlowInEasing)
            )

            // 4. Docking: Lands in top-left, fades traveling flame, blooms streak chip
            isStreakDocked = true
            flameAlpha.animateTo(0f, tween(120))

            dockPulseScale.snapTo(0.82f)
            launch {
                dockPulseScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }

            // Automatically prompt the native Android notification permission dialog at celebration moment
            delay(150)
            onRequestNotificationPermission()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "victorySunburst")
    val sunburstRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunburstRotation"
    )

    LaunchedEffect(Unit) {
        launch {
            contentScale.animateTo(1.0f, spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow))
        }
        launch {
            contentAlpha.animateTo(1.0f, tween(200))
        }
        launch {
            sunburstAlpha.animateTo(0.85f, tween(500))
        }

        // Staggered star drops with authentic audio feedback
        delay(140)
        launch {
            onPlayStarSound(1)
            star1Scale.animateTo(1.25f, tween(130, easing = FastOutSlowInEasing))
            star1Scale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }

        delay(150)
        launch {
            onPlayStarSound(2)
            star2Scale.animateTo(1.35f, tween(140, easing = FastOutSlowInEasing))
            star2Scale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }

        delay(150)
        launch {
            onPlayStarSound(3)
            star3Scale.animateTo(1.25f, tween(130, easing = FastOutSlowInEasing))
            star3Scale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    val headlineText = when {
        isDailyChallenge -> "Well Done!"
        starsEarned == 3 -> "Well Done!"
        starsEarned == 2 -> "Great Job!"
        else -> "Level Complete!"
    }

    // Dynamic stats derived from authentic gameplay results
    val difficultyText = when {
        isDailyChallenge -> "Hard"
        levelNumber % 5 == 0 -> "Hard"
        levelNumber > 10 -> "Medium"
        else -> "Normal"
    }
    val timeFormatted = String.format("%02d:%02d", completionTimeSeconds / 60, completionTimeSeconds % 60)

    Surface(
        color = theme.background,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val availableHeight = maxHeight
            val requiresScroll = availableHeight < 640.dp
            val isCompact = availableHeight < 720.dp

            // Ambient Radiant Sunburst Layer
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(sunburstAlpha.value)
            ) {
                val center = Offset(size.width / 2f, size.height * 0.28f)
                val rayCount = 14
                val maxRadius = size.width * 0.95f

                rotate(sunburstRotation, pivot = center) {
                    for (i in 0 until rayCount) {
                        val angle = (i * 360f / rayCount) * (Math.PI.toFloat() / 180f)
                        val sweep = 8.5f * (Math.PI.toFloat() / 180f)
                        val p1 = Offset(
                            center.x + cos(angle - sweep) * maxRadius,
                            center.y + sin(angle - sweep) * maxRadius
                        )
                        val p2 = Offset(
                            center.x + cos(angle + sweep) * maxRadius,
                            center.y + sin(angle + sweep) * maxRadius
                        )
                        val path = Path().apply {
                            moveTo(center.x, center.y)
                            lineTo(p1.x, p1.y)
                            lineTo(p2.x, p2.y)
                            close()
                        }
                        drawPath(
                            path = path,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ctaGrad1.copy(alpha = 0.22f),
                                    ctaGrad2.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = maxRadius
                            )
                        )
                    }
                }
            }

            // Drifting Confetti Particles Layer
            ConfettiBurstEffect(modifier = Modifier.fillMaxSize(), particleCount = 26)

            // Responsive calculation for Flame Flight Trajectory from Screen Center to Top-Left
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val targetFlightX = -(screenWidth / 2f - 84.dp)
            val targetFlightY = -(screenHeight / 2f - if (isCompact) 48.dp else 56.dp)

            val currentProgress = flightProgress.value
            val currentFlightX = targetFlightX * currentProgress
            val currentFlightY = targetFlightY * currentProgress
            val currentFlameSize = lerp(68.dp, 24.dp, currentProgress)
            val auraAlpha = (1f - currentProgress).coerceIn(0f, 1f)

            // Content Container with responsive vertical constraints
            val scrollState = rememberScrollState()
            val contentModifier = if (requiresScroll) {
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .verticalScroll(scrollState)
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = if (isCompact) 12.dp else 20.dp)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (requiresScroll) Arrangement.Top else Arrangement.SpaceBetween,
                modifier = contentModifier
            ) {
                // 1. TOP BAR: Home Navigation Button / Docked Streak & 100% Centered Level Badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .height(44.dp)
                ) {
                    // Left Slot: Home Button or Docked Streak Chip
                    Box(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        if (isDailyUnlockMilestone) {
                            if (isStreakDocked) {
                                StreakChip(
                                    streakDays = streakDays,
                                    theme = theme,
                                    modifier = Modifier.scale(dockPulseScale.value)
                                )
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = theme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, theme.gridLine),
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(42.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        onPlayClickSound()
                                        onHomeClick()
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Home",
                                        tint = theme.textPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Level Badge (Guaranteed 100% Mathematically Centered across all devices)
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = theme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, theme.gridLine),
                        shadowElevation = 2.dp,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(
                            text = if (isDailyChallenge) "DAILY CHALLENGE" else "LEVEL $levelNumber",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp,
                            color = theme.textSecondary,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }

                if (!requiresScroll) {
                    Spacer(modifier = Modifier.weight(if (isCompact) 0.5f else 0.8f))
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // 2. HERO SECTION: Crowning 3D Star Arch + Anchored Stats Card (Image 4 Benchmark)
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 380.dp)
                        .scale(contentScale.value)
                ) {
                    // Card Surface Container (shifted down so stars overlap top border)
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = theme.surface,
                        shadowElevation = if (theme.isDark) 6.dp else 10.dp,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, theme.gridLine),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 42.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            theme.surface,
                                            theme.background.copy(alpha = 0.45f)
                                        )
                                    )
                                )
                                .padding(horizontal = 22.dp, vertical = 24.dp)
                        ) {
                            // Clearance space for the overlapping top star podium
                            Spacer(modifier = Modifier.height(28.dp))

                            // Celebratory Headline
                            Text(
                                text = headlineText,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = theme.textPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Performance Metrics 2x2 Clean Grid
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(theme.background.copy(alpha = 0.35f))
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    PerformanceMetricCell(
                                        label = "Time",
                                        value = timeFormatted,
                                        theme = theme,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PerformanceMetricCell(
                                        label = "Score",
                                        value = "$score",
                                        theme = theme,
                                        alignEnd = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                HorizontalDivider(
                                    color = theme.gridLine.copy(alpha = 0.45f),
                                    thickness = 1.dp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    PerformanceMetricCell(
                                        label = "Accuracy",
                                        value = "$accuracyPercent%",
                                        theme = theme,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PerformanceMetricCell(
                                        label = "Moves",
                                        value = "$movesCount",
                                        theme = theme,
                                        alignEnd = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Secondary Streak & Difficulty Ribbon
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = "Difficulty:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = theme.textSecondary
                                    )
                                    Text(
                                        text = difficultyText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textPrimary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = "🔥", fontSize = 12.sp)
                                    Text(
                                        text = "${streakDays} Day Streak",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (theme.isDark) Color(0xFFFDE047) else Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                    }

                    // Crowning 3D Star Podium Arch (Top layer, overlaps card crest)
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                    ) {
                        // Left Star (Tilted -10°)
                        SculptedStar3D(
                            isEarned = starsEarned >= 1,
                            size = 46.dp,
                            rotation = -10f,
                            scale = star1Scale.value,
                            modifier = Modifier.offset(y = 10.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Center Hero Star (Elevated, 1.35x size, with ambient halo aura)
                        SculptedStar3D(
                            isEarned = starsEarned >= 2,
                            size = 66.dp,
                            scale = star2Scale.value,
                            haloAura = true,
                            modifier = Modifier.offset(y = (-4).dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Right Star (Tilted +10°)
                        SculptedStar3D(
                            isEarned = starsEarned >= 3,
                            size = 46.dp,
                            rotation = 10f,
                            scale = star3Scale.value,
                            modifier = Modifier.offset(y = 10.dp)
                        )
                    }
                }

                if (!requiresScroll) {
                    Spacer(modifier = Modifier.weight(if (isCompact) 0.6f else 1.0f))
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 3. ACTION BUTTONS: Level 5 Daily Challenge Unlock Experience vs Standard CTAs
                if (isDailyUnlockMilestone) {
                    DailyChallengeUnlockHero(
                        theme = theme,
                        onStartDailyChallengeClick = {
                            onPlayClickSound()
                            onStartDailyChallengeClick()
                        },
                        onNextLevelClick = {
                            onPlayClickSound()
                            onNextLevelClick()
                        }
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 380.dp)
                    ) {
                        // Primary Modern Flat CTA
                        AppPrimaryButton(
                            text = if (isDailyChallenge) "Continue" else "Next Level",
                            onClick = {
                                onPlayClickSound()
                                onNextLevelClick()
                            },
                            theme = theme,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Secondary Home Button (Shown only in Campaign Mode to eliminate redundant Daily Challenge duplicate)
                        if (!isDailyChallenge) {
                            AppSecondaryButton(
                                text = "Home",
                                onClick = {
                                    onPlayClickSound()
                                    onHomeClick()
                                },
                                theme = theme,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 4. Overlaid Animated Flying Flame for Level 5 Unlock Celebration
            if (isDailyUnlockMilestone && flameAlpha.value > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = currentFlightX, y = currentFlightY)
                        .alpha(flameAlpha.value),
                    contentAlignment = Alignment.Center
                ) {
                    // Ambient Radiant Flame Aura
                    Box(
                        modifier = Modifier
                            .size(currentFlameSize * 2.3f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFF59E0B).copy(alpha = 0.55f * auraAlpha),
                                        Color(0xFFF97316).copy(alpha = 0.22f * auraAlpha),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Text(
                        text = "🔥",
                        fontSize = (currentFlameSize.value * 0.74f).sp,
                        modifier = Modifier.scale(flameScale.value)
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyChallengeUnlockHero(
    theme: ThemePreset,
    onStartDailyChallengeClick: () -> Unit,
    onNextLevelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dailyUnlockPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Smooth Upward Slide-In Animation from Bottom
    val slideOffsetY = remember { Animatable(180f) }
    val heroAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Starts smoothly at 1050ms right as the flame is gliding to top-left
        delay(1050)
        launch {
            slideOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            heroAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(420, easing = FastOutSlowInEasing)
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp)
            .offset(y = slideOffsetY.value.dp)
            .alpha(heroAlpha.value)
    ) {
        // Sleek Minimalist Unlocked Badge Pill (Clean, unobtrusive milestone indicator)
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = Color(0xFFF59E0B).copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = pulseAlpha)),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DAILY CHALLENGE UNLOCKED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp,
                    color = Color(0xFFD97706)
                )
            }
        }

        // Primary Button: "Start Daily Challenge"
        AppPrimaryButton(
            text = "Start Daily Challenge",
            onClick = onStartDailyChallengeClick,
            theme = theme,
            modifier = Modifier.fillMaxWidth()
        )

        // Secondary Button: "Next Level"
        AppSecondaryButton(
            text = "Next Level",
            onClick = onNextLevelClick,
            theme = theme,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PerformanceMetricCell(
    label: String,
    value: String,
    theme: ThemePreset,
    alignEnd: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = theme.textSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary
        )
    }
}

@Composable
private fun PerformanceMetricRow(
    label: String,
    value: String,
    theme: ThemePreset
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = theme.textSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary
        )
    }
}

@Composable
private fun SecondaryStatItem(
    icon: String,
    value: String,
    theme: ThemePreset
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = icon,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary
        )
    }
}
