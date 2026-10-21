package com.arrowescape.puzzle.ui.components

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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LevelVictoryDialog(
    levelNumber: Int,
    coinsEarned: Int,
    streakDays: Int,
    theme: ThemePreset,
    isDailyChallenge: Boolean = false,
    heartsRemaining: Int = 3,
    maxHearts: Int = 3,
    onNextLevelClick: () -> Unit
) {
    val cardScale = remember { Animatable(0.72f) }
    val cardAlpha = remember { Animatable(0f) }
    val star1Scale = remember { Animatable(0f) }
    val star2Scale = remember { Animatable(0f) }
    val star3Scale = remember { Animatable(0f) }
    val star1Rotation = remember { Animatable(-35f) }
    val star2Rotation = remember { Animatable(0f) }
    val star3Rotation = remember { Animatable(35f) }
    val animatedCoins = remember { Animatable(0f) }
    val haloAlpha = remember { Animatable(0f) }

    // Stars earned based on hearts remaining
    val starsEarned = when {
        isDailyChallenge -> 3
        heartsRemaining >= maxHearts -> 3
        heartsRemaining >= 2 -> 2
        else -> 1
    }

    val infiniteTransition = rememberInfiniteTransition(label = "victoryEffects")
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloRotation"
    )

    val buttonPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonPulse"
    )

    LaunchedEffect(Unit) {
        launch {
            cardScale.animateTo(1.0f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow))
        }
        launch {
            cardAlpha.animateTo(1.0f, tween(180))
        }
        launch {
            haloAlpha.animateTo(0.85f, tween(400))
        }

        // Staggered star drops
        delay(140)
        launch {
            star1Rotation.animateTo(-12f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        launch {
            star1Scale.animateTo(1.35f, tween(130, easing = FastOutSlowInEasing))
            star1Scale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }

        delay(130)
        launch {
            star2Scale.animateTo(1.48f, tween(150, easing = FastOutSlowInEasing))
            star2Scale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }

        delay(130)
        launch {
            star3Rotation.animateTo(12f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        launch {
            star3Scale.animateTo(1.35f, tween(130, easing = FastOutSlowInEasing))
            star3Scale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }

        delay(80)
        launch {
            animatedCoins.animateTo(coinsEarned.toFloat(), tween(450, easing = FastOutSlowInEasing))
        }
    }

    val headlineText = when {
        isDailyChallenge -> "DAILY SOLVED!"
        starsEarned == 3 -> "PERFECT!"
        starsEarned == 2 -> "EXCELLENT!"
        else -> "LEVEL COMPLETE!"
    }

    val sublineText = when {
        isDailyChallenge -> "Daily Challenge completed smoothly"
        starsEarned == 3 -> "Level $levelNumber cleared with zero mistakes!"
        starsEarned == 2 -> "Level $levelNumber cleared with 2 stars"
        else -> "Level $levelNumber cleared! Keep going"
    }

    val activeStarColor = Color(0xFFFFB800)
    val inactiveStarColor = if (theme.isDark) Color(0xFF475569) else Color(0xFFCBD5E1)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = theme.surface),
                elevation = CardDefaults.cardElevation(20.dp),
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .scale(cardScale.value)
                    .border(
                        width = 1.dp,
                        color = if (theme.isDark) Color(0x33FFFFFF) else Color(0x1F000000),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    // 1. CELEBRATION HALO & STARS DISPLAY
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient rotating festive sunburst
                        Canvas(
                            modifier = Modifier
                                .size(200.dp)
                                .scale(haloAlpha.value)
                        ) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val rayCount = 12
                            val maxRadius = size.width / 2f
                            rotate(haloRotation, pivot = center) {
                                for (i in 0 until rayCount) {
                                    val angle = (i * 360f / rayCount) * (Math.PI.toFloat() / 180f)
                                    val sweep = (10f) * (Math.PI.toFloat() / 180f)
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
                                                Color(0x33FFB800),
                                                Color(0x12FF9900),
                                                Color.Transparent
                                            ),
                                            center = center,
                                            radius = maxRadius
                                        )
                                    )
                                }
                            }
                        }

                        // Tri-Star Arc (Left, Center Big, Right)
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Star 1 (Left)
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star 1",
                                tint = if (starsEarned >= 1) activeStarColor else inactiveStarColor,
                                modifier = Modifier
                                    .size(42.dp)
                                    .scale(star1Scale.value)
                                    .rotate(star1Rotation.value)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            // Star 2 (Center - Hero Star)
                            Box(contentAlignment = Alignment.Center) {
                                if (starsEarned >= 2) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0x40FFB800),
                                                        Color.Transparent
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Star 2",
                                    tint = if (starsEarned >= 2) activeStarColor else inactiveStarColor,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .scale(star2Scale.value)
                                        .rotate(star2Rotation.value)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Star 3 (Right)
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star 3",
                                tint = if (starsEarned >= 3) activeStarColor else inactiveStarColor,
                                modifier = Modifier
                                    .size(42.dp)
                                    .scale(star3Scale.value)
                                    .rotate(star3Rotation.value)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. CONGRATULATORY TITLES
                    Text(
                        text = headlineText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = theme.textPrimary,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = sublineText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // 3. STATS & REWARDS POD
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Coins Reward Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (theme.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (theme.isDark) Color(0x33FBBF24) else Color(0x22FBBF24)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = "Coins",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "+${animatedCoins.value.toInt()}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textPrimary
                                    )
                                    Text(
                                        text = "Coins",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = theme.textSecondary
                                    )
                                }
                            }
                        }

                        // Streak Flame Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (theme.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (theme.isDark) Color(0x33EF4444) else Color(0x22EF4444)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEE2E2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "Streak",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "$streakDays Day",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textPrimary
                                    )
                                    Text(
                                        text = "Streak",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = theme.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 4. ACTION BUTTON (NEXT LEVEL / CONTINUE)
                    Button(
                        onClick = onNextLevelClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.arrowDefault
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .scale(buttonPulse)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isDailyChallenge) "CLAIM REWARD" else "CONTINUE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = if (theme.isDark) Color(0xFF0F172A) else Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "➔",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (theme.isDark) Color(0xFF0F172A) else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
