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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.components.AppPrimaryButton
import com.arrowescape.puzzle.ui.components.ConfettiBurstEffect
import com.arrowescape.puzzle.ui.components.SculptedStar3D
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom arched podium shape reproducing the Unity victory card crest.
 * Features a soft rising arch contour that reaches its peak at the center apex
 * where the primary hero star is docked.
 */
class ArchedPodiumCardShape(
    private val archHeight: Dp = 16.dp,
    private val cornerRadius: Dp = 28.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val archPx = with(density) { archHeight.toPx() }
        val radiusPx = with(density) { cornerRadius.toPx() }
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(0f, archPx + radiusPx)
            quadraticTo(0f, archPx, radiusPx * 0.7f, archPx * 0.82f)
            cubicTo(
                w * 0.25f, archPx * 0.35f,
                w * 0.38f, 0f,
                w * 0.5f, 0f
            )
            cubicTo(
                w * 0.62f, 0f,
                w * 0.75f, archPx * 0.35f,
                w - radiusPx * 0.7f, archPx * 0.82f
            )
            quadraticTo(w, archPx, w, archPx + radiusPx)
            lineTo(w, h - radiusPx)
            quadraticTo(w, h, w - radiusPx, h)
            lineTo(radiusPx, h)
            quadraticTo(0f, h, 0f, h - radiusPx)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Modern, clean, and highly engaging Victory Screen matching the New Unity benchmark.
 * Features:
 * - Triumphant 3D sculpted star podium crowning an arched crest card.
 * - Gentle revolving sunburst + festive drifting confetti particles.
 * - Meaningful gameplay performance metrics with rank badge (S/A/B).
 * - Engaging social benchmark ("Faster than 84.7% of players!").
 * - Horizontal level progression track with completed, active, and upcoming nodes.
 * - Prominent hero CTA button with docked floating streak win badge ("👑 x5 Win").
 * - Minimalist "Home" text link eliminating duplicate button clutter.
 * - 100% theme-adaptive & fully responsive across all device sizes.
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
    shapeName: String? = null,
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
    // Dynamic theme-based sunburst rays palette
    val (ctaGrad1, ctaGrad2) = when (theme.name) {
        "Warm Sepia" -> Pair(Color(0xFFE09F3E), Color(0xFFC57B22))
        "Midnight Dark" -> Pair(Color(0xFF38BDF8), Color(0xFF0284C7))
        "Calm Pastel" -> Pair(Color(0xFF8B5CF6), Color(0xFF7C3AED))
        "Mint Fresh" -> Pair(Color(0xFF059669), Color(0xFF047857))
        "Sunset Rose" -> Pair(Color(0xFFE11D48), Color(0xFFBE123C))
        else -> Pair(Color(0xFF10B981), Color(0xFF059669)) // Clean Light
    }

    // Entrance Animations
    val contentScale = remember { Animatable(0.92f) }
    val contentAlpha = remember { Animatable(0f) }
    val star1Scale = remember { Animatable(0f) }
    val star2Scale = remember { Animatable(0f) }
    val star3Scale = remember { Animatable(0f) }
    val sunburstAlpha = remember { Animatable(0f) }

    // Milestone Unlock Celebration Animation States
    val flameScale = remember { Animatable(0f) }
    val flameAlpha = remember { Animatable(0f) }
    val flightProgress = remember { Animatable(0f) }

    if (isDailyUnlockMilestone) {
        LaunchedEffect(Unit) {
            delay(750)
            launch { flameAlpha.animateTo(1f, tween(240)) }
            flameScale.animateTo(
                targetValue = 1.35f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            flameScale.animateTo(1.0f, tween(180))
            delay(350)
            flightProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(650, easing = FastOutSlowInEasing)
            )
            flameAlpha.animateTo(0f, tween(120))
            delay(150)
            onRequestNotificationPermission()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "victorySunburst")
    val sunburstRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
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

    // Dynamic Praise Headline
    val headlineText = when {
        isDailyChallenge -> "Flawless!"
        starsEarned == 3 -> "Flawless!"
        starsEarned == 2 -> "Great Job!"
        else -> "Level Complete!"
    }

    // Engaging Social Benchmark Percentile (Unity Reference)
    val playerPercentile = remember(accuracyPercent, completionTimeSeconds, starsEarned) {
        when (starsEarned) {
            3 -> {
                val timeBonus = ((90 - completionTimeSeconds).coerceIn(0, 60) * 0.15f)
                val accBonus = ((accuracyPercent - 70).coerceIn(0, 30) * 0.22f)
                (80.0f + timeBonus + accBonus).coerceIn(82.4f, 98.9f)
            }
            2 -> {
                val accBonus = ((accuracyPercent - 50).coerceIn(0, 40) * 0.35f)
                (58.0f + accBonus).coerceIn(60.2f, 79.8f)
            }
            else -> {
                (40.0f + (accuracyPercent.coerceIn(30, 80) * 0.25f)).coerceIn(42.0f, 58.5f)
            }
        }
    }

    val subtitleText = when {
        isDailyChallenge -> "Daily puzzle mastered!"
        starsEarned >= 2 -> String.format(java.util.Locale.US, "Faster than %.1f%% of players!", playerPercentile)
        else -> "Level complete! Keep sharpening your skills!"
    }

    // Dynamic stats derived from authentic gameplay results
    val difficultyText = when {
        isDailyChallenge -> "Hard"
        levelNumber % 5 == 0 -> "Hard"
        levelNumber > 10 -> "Medium"
        else -> "Normal"
    }
    val timeFormatted = String.format(java.util.Locale.US, "%02d:%02d", completionTimeSeconds / 60, completionTimeSeconds % 60)

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
                val center = Offset(size.width / 2f, size.height * 0.26f)
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
                    .padding(horizontal = 24.dp, vertical = if (isCompact) 14.dp else 22.dp)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (requiresScroll) Arrangement.spacedBy(16.dp) else Arrangement.SpaceBetween,
                modifier = contentModifier
            ) {
                // Top breathing clearance
                if (!requiresScroll) {
                    Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 14.dp))
                }

                // 1. HERO SECTION: Crowning 3D Star Arch + Arched Card (Unity Reference)
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 380.dp)
                        .scale(contentScale.value)
                ) {
                    // Arched Card Surface Container
                    Surface(
                        shape = ArchedPodiumCardShape(archHeight = 16.dp, cornerRadius = 28.dp),
                        color = theme.surface,
                        shadowElevation = if (theme.isDark) 6.dp else 10.dp,
                        border = BorderStroke(1.2.dp, theme.gridLine),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 38.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            theme.surface,
                                            theme.background.copy(alpha = 0.40f)
                                        )
                                    )
                                )
                                .padding(horizontal = 22.dp, vertical = 22.dp)
                        ) {
                            // Clearance space for the overlapping top star podium arch
                            Spacer(modifier = Modifier.height(28.dp))

                            // Praise Headline ("Flawless!")
                            Text(
                                text = headlineText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp,
                                color = theme.textPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Social benchmark subtitle ("Faster than 84.7% of players!")
                            Text(
                                text = subtitleText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (theme.isDark) Color(0xFFFBBF24) else Color(0xFFC57B22),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Inner Performance Metrics Card (Unity Clean Key-Value List)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (theme.isDark) theme.background.copy(alpha = 0.65f)
                                        else theme.background.copy(alpha = 0.45f)
                                    )
                                    .border(1.dp, theme.gridLine.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                            ) {
                                // Row 1: Difficulty
                                PerformanceMetricRow(
                                    label = "Difficulty",
                                    value = difficultyText,
                                    theme = theme
                                )

                                // Row 2: Time with S/A/B Gold Rank Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Time",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = theme.textSecondary
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val rankLetter = if (completionTimeSeconds <= 60 && accuracyPercent >= 85) "S"
                                        else if (completionTimeSeconds <= 120) "A"
                                        else "B"

                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFFEF3C7),
                                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = rankLetter,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFB45309),
                                                    style = TextStyle(
                                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                                    )
                                                )
                                            }
                                        }
                                        Text(
                                            text = timeFormatted,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.textPrimary
                                        )
                                    }
                                }

                                // Row 3: Score
                                PerformanceMetricRow(
                                    label = "Score",
                                    value = "$score",
                                    theme = theme
                                )

                                // Row 4: Accuracy
                                PerformanceMetricRow(
                                    label = "Accuracy",
                                    value = "$accuracyPercent%",
                                    theme = theme
                                )
                            }
                        }
                    }

                    // Crowning 3D Star Podium Arch (Sitting seamlessly along the arched crest)
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                    ) {
                        // Left Star (Tilted -9°, size 52dp)
                        SculptedStar3D(
                            isEarned = starsEarned >= 1,
                            size = 52.dp,
                            rotation = -9f,
                            scale = star1Scale.value,
                            modifier = Modifier.offset(y = 12.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        // Center Hero Star (Elevated at peak, size 72dp, with radiant halo aura)
                        SculptedStar3D(
                            isEarned = starsEarned >= 2,
                            size = 72.dp,
                            scale = star2Scale.value,
                            haloAura = true,
                            modifier = Modifier.offset(y = (-6).dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        // Right Star (Tilted +9°, size 52dp)
                        SculptedStar3D(
                            isEarned = starsEarned >= 3,
                            size = 52.dp,
                            rotation = 9f,
                            scale = star3Scale.value,
                            modifier = Modifier.offset(y = 12.dp)
                        )
                    }
                }

                // 2. HORIZONTAL LEVEL PROGRESSION TRACK (Unity Reference 210-211-212-213-214)
                LevelProgressionTrack(
                    currentLevel = levelNumber,
                    isDailyChallenge = isDailyChallenge,
                    theme = theme
                )

                // 3. ACTION BUTTONS: Level 5 Milestone vs Standard Hero CTA
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 380.dp)
                    ) {
                        // Hero Primary CTA with Docked Floating Streak Pill
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AppPrimaryButton(
                                text = if (isDailyChallenge) "Continue" else "Next Level",
                                onClick = {
                                    onPlayClickSound()
                                    onNextLevelClick()
                                },
                                theme = theme,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Floating Streak Win Pill docked on the top-right corner
                            if (streakDays >= 1) {
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = Color(0xFFF59E0B),
                                    border = BorderStroke(1.2.dp, Color(0xFFFDE68A)),
                                    shadowElevation = 3.dp,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-11).dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                                )
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "👑",
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = if (streakDays > 1) "x$streakDays Win" else "1 Win",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            style = TextStyle(
                                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Secondary Action: Minimalist "Home" text link
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onPlayClickSound()
                                    onHomeClick()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Home",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.textSecondary,
                                textDecoration = TextDecoration.Underline,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                    }
                }

                // Bottom breathing clearance
                if (!requiresScroll) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Milestone Flying Flame Animation Overlay
            if (isDailyUnlockMilestone && flameAlpha.value > 0f) {
                val screenWidth = maxWidth
                val screenHeight = maxHeight
                val targetFlightX = -(screenWidth / 2f - 84.dp)
                val targetFlightY = -(screenHeight / 2f - 48.dp)
                val currentProgress = flightProgress.value
                val currentFlightX = targetFlightX * currentProgress
                val currentFlightY = targetFlightY * currentProgress
                val currentFlameSize = lerp(68.dp, 24.dp, currentProgress)
                val auraAlpha = (1f - currentProgress).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = currentFlightX, y = currentFlightY)
                        .alpha(flameAlpha.value),
                    contentAlignment = Alignment.Center
                ) {
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

/**
 * Sleek 5-node level progression road/track matching the Unity aesthetic.
 * Shows completed levels in soft theme tint, the current level highlighted with bold prominence,
 * and upcoming levels in subtle outline.
 */
@Composable
private fun LevelProgressionTrack(
    currentLevel: Int,
    isDailyChallenge: Boolean,
    theme: ThemePreset,
    modifier: Modifier = Modifier
) {
    val displayLevels = remember(currentLevel, isDailyChallenge) {
        if (isDailyChallenge) {
            listOf("M", "T", "W", "T", "F")
        } else {
            val start = (currentLevel - 2).coerceAtLeast(1)
            (start until start + 5).map { it.toString() }
        }
    }
    val activeIndex = if (isDailyChallenge) 4 else (currentLevel - (currentLevel - 2).coerceAtLeast(1)).coerceIn(0, 4)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Connecting track bar behind nodes
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(4.dp)
                .clip(CircleShape)
                .background(theme.gridLine.copy(alpha = 0.8f))
        )

        // Stepper Nodes Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            displayLevels.forEachIndexed { index, label ->
                val isCompleted = index < activeIndex
                val isCurrent = index == activeIndex

                val nodeSize = if (isCurrent) 38.dp else 32.dp
                val bgColor = when {
                    isCurrent -> theme.arrowDefault
                    isCompleted -> if (theme.isDark) theme.surface else theme.arrowDefault.copy(alpha = 0.35f)
                    else -> theme.surface
                }
                val textColor = when {
                    isCurrent -> Color.White
                    isCompleted -> if (theme.isDark) theme.textPrimary else theme.arrowDefault
                    else -> theme.textSecondary.copy(alpha = 0.7f)
                }
                val borderColor = when {
                    isCurrent -> Color.White.copy(alpha = 0.35f)
                    isCompleted -> Color.Transparent
                    else -> theme.gridLine
                }

                Surface(
                    shape = CircleShape,
                    color = bgColor,
                    border = BorderStroke(if (isCurrent) 2.dp else 1.dp, borderColor),
                    shadowElevation = if (isCurrent) 3.dp else 0.dp,
                    modifier = Modifier.size(nodeSize)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = label,
                            fontSize = if (isCurrent) 13.sp else 12.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                            color = textColor,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Key-Value performance row with muted left label and crisp right-aligned value.
 */
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
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary
        )
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

    val slideOffsetY = remember { Animatable(180f) }
    val heroAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp)
            .offset(y = slideOffsetY.value.dp)
            .alpha(heroAlpha.value)
    ) {
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = Color(0xFFF59E0B).copy(alpha = 0.12f),
            border = BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = pulseAlpha)),
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

        AppPrimaryButton(
            text = "Start Daily Challenge",
            onClick = onStartDailyChallengeClick,
            theme = theme,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onNextLevelClick()
                }
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Next Level",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.textSecondary,
                textDecoration = TextDecoration.Underline,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }
    }
}
