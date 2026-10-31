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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
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
 * Modern, clean, and highly engaging Victory Screen matching the Unity reference benchmark.
 * Features:
 * - Full-bleed radiating top sunburst & luminous ambient glow.
 * - Triumphant 3D sculpted star podium crowning an arched crest card.
 * - Restored clean Score metric and performance counters strip (🎯 Accuracy | ✕ Mistakes | 💡 Hints).
 * - Enhanced 5-node level progression stepper road.
 * - Large, prominent action buttons (Next Level + Home) placed directly below the level number.
 * - 100% theme-adaptive & fully responsive across all device sizes with ZERO scrolling.
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
    mistakesCount: Int = 0,
    hintsUsed: Int = 0,
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
            animation = tween(22000, easing = LinearEasing),
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
            sunburstAlpha.animateTo(0.95f, tween(500))
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

    // Dynamic stats derived from authentic gameplay results
    val difficultyText = when {
        isDailyChallenge -> "Hard"
        levelNumber % 5 == 0 -> "Hard"
        levelNumber > 10 -> "Medium"
        else -> "Normal"
    }
    val timeFormatted = String.format(Locale.US, "%02d:%02d", completionTimeSeconds / 60, completionTimeSeconds % 60)
    val scoreFormatted = NumberFormat.getNumberInstance(Locale.US).format(score)

    Surface(
        color = theme.background,
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val availableHeight = maxHeight
            val isVeryCompact = availableHeight < 640.dp
            val isCompact = availableHeight < 720.dp

            // Full-Bleed Radiant Sunburst Layer (Reaching all the way to top edges & corners)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(sunburstAlpha.value)
            ) {
                val center = Offset(size.width / 2f, size.height * 0.35f)
                val rayCount = 14
                val maxRadius = hypot(size.width.toDouble(), size.height.toDouble()).toFloat() * 1.15f

                // Luminous top ambient glow circle behind the stats card
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ctaGrad1.copy(alpha = 0.38f),
                            ctaGrad2.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius * 0.65f
                    ),
                    center = center,
                    radius = maxRadius * 0.65f
                )

                // Revolving radiant sunburst rays spanning edge-to-edge across entire window
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
                                    ctaGrad1.copy(alpha = 0.28f),
                                    ctaGrad2.copy(alpha = 0.10f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = maxRadius
                            )
                        )
                    }
                }
            }

            // Drifting Confetti Particles Layer (Full bleed)
            ConfettiBurstEffect(modifier = Modifier.fillMaxSize(), particleCount = 26)

            // Responsive Scaling Parameters for Zero-Scroll Guarantee
            val cardArchHeight = if (isVeryCompact) 12.dp else 16.dp
            val starPodiumHeight = if (isVeryCompact) 68.dp else 78.dp
            val centerStarSize = if (isVeryCompact) 58.dp else 68.dp
            val sideStarSize = if (isVeryCompact) 42.dp else 48.dp
            val cardPaddingTop = if (isVeryCompact) 30.dp else 38.dp
            val cardInnerTopSpacer = if (isVeryCompact) 32.dp else 40.dp
            val headlineFontSize = if (isVeryCompact) 25.sp else 28.sp
            val primaryBtnHeight = if (isVeryCompact) 52.dp else 58.dp
            val secBtnHeight = if (isVeryCompact) 42.dp else 48.dp
            val contentPadHoriz = if (isVeryCompact) 18.dp else 22.dp

            // Main Screen Content: Perfectly Centered & Compact with Insets Padding and Zero-Scroll Guarantee
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .displayCutoutPadding()
                    .padding(horizontal = contentPadHoriz, vertical = 8.dp)
            ) {
                // Top flexible breathing room
                Spacer(modifier = Modifier.weight(if (isCompact) 0.5f else 1.0f))

                // 1. HERO SECTION: Crowning 3D Star Arch + Arched Card
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 380.dp)
                        .scale(contentScale.value)
                ) {
                    // Arched Card Container
                    Surface(
                        shape = ArchedPodiumCardShape(archHeight = cardArchHeight, cornerRadius = 26.dp),
                        color = theme.surface,
                        shadowElevation = if (theme.isDark) 6.dp else 10.dp,
                        border = BorderStroke(1.2.dp, theme.gridLine),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = cardPaddingTop)
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
                                .padding(horizontal = 20.dp, vertical = if (isVeryCompact) 14.dp else 18.dp)
                        ) {
                            // Top clearance space for overlapping stars arch
                            Spacer(modifier = Modifier.height(cardInnerTopSpacer))

                            // Praise Headline ("Flawless!" / "Great Job!" / "Level Complete!")
                            Text(
                                text = headlineText,
                                fontSize = headlineFontSize,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp,
                                color = theme.textPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(if (isVeryCompact) 10.dp else 14.dp))

                            // Inner Key-Value Performance Panel (Difficulty | Time | Score)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(if (isVeryCompact) 8.dp else 11.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (theme.isDark) theme.background.copy(alpha = 0.65f)
                                        else theme.background.copy(alpha = 0.45f)
                                    )
                                    .border(1.dp, theme.gridLine.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                                    .padding(horizontal = 18.dp, vertical = if (isVeryCompact) 10.dp else 13.dp)
                            ) {
                                // Row 1: Difficulty
                                PerformanceMetricRow(
                                    label = "Difficulty",
                                    value = difficultyText,
                                    theme = theme
                                )

                                // Row 2: Time (Clean formatted time without S rank badge)
                                PerformanceMetricRow(
                                    label = "Time",
                                    value = timeFormatted,
                                    theme = theme
                                )

                                // Row 3: Score (Restored clean Score metric)
                                PerformanceMetricRow(
                                    label = "Score",
                                    value = scoreFormatted,
                                    theme = theme
                                )
                            }

                            Spacer(modifier = Modifier.height(if (isVeryCompact) 8.dp else 11.dp))

                            // Performance Counters Strip: Accuracy | Wrong Moves | Hints (Reference Image 2)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (theme.isDark) theme.background.copy(alpha = 0.75f) else theme.boosterBg.copy(alpha = 0.65f),
                                border = BorderStroke(1.dp, theme.gridLine.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = if (isVeryCompact) 7.dp else 9.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Accuracy
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(text = "🎯", fontSize = 13.sp)
                                        Text(
                                            text = "$accuracyPercent%",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.textPrimary
                                        )
                                    }

                                    // Divider
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(14.dp)
                                            .background(theme.gridLine.copy(alpha = 0.6f))
                                    )

                                    // 2. Wrong Moves / Mistakes
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "✕",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFEF4444)
                                        )
                                        Text(
                                            text = "$mistakesCount",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (mistakesCount > 0) Color(0xFFEF4444) else theme.textPrimary
                                        )
                                    }

                                    // Divider
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(14.dp)
                                            .background(theme.gridLine.copy(alpha = 0.6f))
                                    )

                                    // 3. Hints Used
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = "💡", fontSize = 13.sp)
                                        Text(
                                            text = "$hintsUsed",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hintsUsed > 0) Color(0xFFF59E0B) else theme.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Crowning 3D Star Podium Arch (Top layer, overlapping card arch crest)
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(starPodiumHeight)
                    ) {
                        // Left Star (Tilted -9°)
                        SculptedStar3D(
                            isEarned = starsEarned >= 1,
                            size = sideStarSize,
                            rotation = -9f,
                            scale = star1Scale.value,
                            modifier = Modifier.offset(y = 5.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Center Hero Star (Elevated at apex, radiant halo aura)
                        SculptedStar3D(
                            isEarned = starsEarned >= 2,
                            size = centerStarSize,
                            scale = star2Scale.value,
                            haloAura = true,
                            modifier = Modifier.offset(y = (-12).dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Right Star (Tilted +9°)
                        SculptedStar3D(
                            isEarned = starsEarned >= 3,
                            size = sideStarSize,
                            rotation = 9f,
                            scale = star3Scale.value,
                            modifier = Modifier.offset(y = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isVeryCompact) 10.dp else 14.dp))

                // 2. HORIZONTAL LEVEL PROGRESSION ROAD (Clean 5-Node Stepper)
                LevelProgressionTrack(
                    currentLevel = levelNumber,
                    isDailyChallenge = isDailyChallenge,
                    isVeryCompact = isVeryCompact,
                    theme = theme
                )

                Spacer(modifier = Modifier.height(if (isVeryCompact) 10.dp else 14.dp))

                // 3. LARGE PROMINENT ACTION BUTTONS (Placed DIRECTLY below Level Number)
                if (isDailyUnlockMilestone) {
                    DailyChallengeUnlockHero(
                        theme = theme,
                        primaryBtnHeight = primaryBtnHeight,
                        secBtnHeight = secBtnHeight,
                        onStartDailyChallengeClick = {
                            onPlayClickSound()
                            onStartDailyChallengeClick()
                        },
                        onNextLevelClick = {
                            onPlayClickSound()
                            onNextLevelClick()
                        },
                        onHomeClick = {
                            onPlayClickSound()
                            onHomeClick()
                        }
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(if (isVeryCompact) 8.dp else 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 380.dp)
                    ) {
                        // Hero Primary CTA Button ("Next Level")
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = theme.arrowDefault,
                            shadowElevation = 5.dp,
                            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(primaryBtnHeight)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onPlayClickSound()
                                    onNextLevelClick()
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = if (isDailyChallenge) "Continue" else "Next Level",
                                    fontSize = if (isVeryCompact) 16.5.sp else 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = Color.White,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }
                        }

                        // Subtle Secondary Button ("Home") - Noticeable but understated compared to hero Next Level
                        val homeBgColor = if (theme.isDark) {
                            theme.surface.copy(alpha = 0.55f)
                        } else {
                            theme.boosterBg.copy(alpha = 0.65f)
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = homeBgColor,
                            border = BorderStroke(1.dp, theme.gridLine.copy(alpha = 0.55f)),
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(secBtnHeight)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onPlayClickSound()
                                    onHomeClick()
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "Home",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = theme.textSecondary,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }
                        }
                    }
                }

                // Bottom flexible breathing room
                Spacer(modifier = Modifier.weight(if (isCompact) 0.5f else 1.0f))
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
 * 5-node horizontal level progression road/stepper.
 * Shows completed levels in soft theme tint, the current level highlighted with bold prominence
 * and a crown badge, and upcoming levels in subtle outline.
 */
@Composable
private fun LevelProgressionTrack(
    currentLevel: Int,
    isDailyChallenge: Boolean,
    isVeryCompact: Boolean,
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
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Connecting track bar behind nodes
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .height(3.5.dp)
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

                val nodeSize = if (isCurrent) (if (isVeryCompact) 36.dp else 40.dp) else (if (isVeryCompact) 28.dp else 32.dp)
                val bgColor = when {
                    isCurrent -> when (theme.name) {
                        "Warm Sepia" -> Color(0xFF7C3AED) // Vibrant purple node as in Unity reference
                        else -> theme.arrowDefault
                    }
                    isCompleted -> if (theme.isDark) theme.surface else theme.arrowDefault.copy(alpha = 0.35f)
                    else -> theme.surface
                }
                val textColor = when {
                    isCurrent -> Color.White
                    isCompleted -> if (theme.isDark) theme.textPrimary else theme.arrowDefault
                    else -> theme.textSecondary.copy(alpha = 0.7f)
                }
                val borderColor = when {
                    isCurrent -> Color.White.copy(alpha = 0.45f)
                    isCompleted -> Color.Transparent
                    else -> theme.gridLine
                }

                Box(contentAlignment = Alignment.TopCenter) {
                    Surface(
                        shape = CircleShape,
                        color = bgColor,
                        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, borderColor),
                        shadowElevation = if (isCurrent) 4.dp else 0.dp,
                        modifier = Modifier.size(nodeSize)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = label,
                                fontSize = if (isCurrent) 13.5.sp else 12.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                color = textColor,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                        }
                    }

                    // Mini Crown badge over current completed node
                    if (isCurrent) {
                        Text(
                            text = "👑",
                            fontSize = 11.sp,
                            modifier = Modifier.offset(y = (-11).dp)
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
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = theme.textPrimary
        )
    }
}

@Composable
private fun DailyChallengeUnlockHero(
    theme: ThemePreset,
    primaryBtnHeight: Dp,
    secBtnHeight: Dp,
    onStartDailyChallengeClick: () -> Unit,
    onNextLevelClick: () -> Unit,
    onHomeClick: () -> Unit,
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

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = theme.arrowDefault,
            shadowElevation = 5.dp,
            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.22f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(primaryBtnHeight)
                .clip(RoundedCornerShape(18.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onStartDailyChallengeClick()
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Start Daily Challenge",
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = Color.White,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            }
        }

        val secBgColor = if (theme.isDark) {
            theme.surface.copy(alpha = 0.55f)
        } else {
            theme.boosterBg.copy(alpha = 0.65f)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Next Level Button
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = secBgColor,
                border = BorderStroke(1.dp, theme.gridLine.copy(alpha = 0.55f)),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(secBtnHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onNextLevelClick()
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Next Level",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.textSecondary,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }

            // Home Button
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = secBgColor,
                border = BorderStroke(1.dp, theme.gridLine.copy(alpha = 0.55f)),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(secBtnHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onHomeClick()
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Home",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.textSecondary,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }
        }
    }
}
