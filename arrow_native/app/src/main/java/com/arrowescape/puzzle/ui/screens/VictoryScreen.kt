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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

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
    onClaimDoubleCoins: (Int) -> Unit = {},
    onNextLevelClick: () -> Unit,
    onHomeClick: () -> Unit,
    onPlayStarSound: (Int) -> Unit = {},
    onPlayCoinSound: () -> Unit = {},
    onPlayClickSound: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var hasClaimedDouble by remember { mutableStateOf(false) }

    val contentScale = remember { Animatable(0.88f) }
    val contentAlpha = remember { Animatable(0f) }

    val star1Scale = remember { Animatable(0f) }
    val star2Scale = remember { Animatable(0f) }
    val star3Scale = remember { Animatable(0f) }
    val star1Rotation = remember { Animatable(-35f) }
    val star3Rotation = remember { Animatable(35f) }

    val animatedCoins = remember { Animatable(0f) }
    val haloAlpha = remember { Animatable(0f) }

    // Accurate star rating:
    // 3 Stars: 0 mistakes (heartsRemaining >= maxHearts)
    // 2 Stars: 1 mistake (heartsRemaining >= maxHearts - 1)
    // 1 Star: 2+ mistakes
    val starsEarned = when {
        isDailyChallenge -> 3
        heartsRemaining >= maxHearts -> 3
        heartsRemaining >= maxHearts - 1 -> 2
        else -> 1
    }

    val infiniteTransition = rememberInfiniteTransition(label = "victoryEffects")
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloRotation"
    )

    val buttonPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonPulse"
    )

    LaunchedEffect(Unit) {
        launch {
            contentScale.animateTo(1.0f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
        }
        launch {
            contentAlpha.animateTo(1.0f, tween(180))
        }
        launch {
            haloAlpha.animateTo(0.9f, tween(400))
        }

        // Staggered star drops with authentic cheer sounds
        delay(120)
        launch {
            star1Rotation.animateTo(-12f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        launch {
            onPlayStarSound(1)
            star1Scale.animateTo(1.30f, tween(130, easing = FastOutSlowInEasing))
            star1Scale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }

        delay(140)
        launch {
            onPlayStarSound(2)
            star2Scale.animateTo(1.42f, tween(140, easing = FastOutSlowInEasing))
            star2Scale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }

        delay(140)
        launch {
            star3Rotation.animateTo(12f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        launch {
            onPlayStarSound(3)
            star3Scale.animateTo(1.30f, tween(130, easing = FastOutSlowInEasing))
            star3Scale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }

        delay(100)
        launch {
            onPlayCoinSound()
            val targetCoin = if (hasClaimedDouble) coinsEarned * 2 else coinsEarned
            animatedCoins.animateTo(targetCoin.toFloat(), tween(400, easing = FastOutSlowInEasing))
        }
    }

    val headlineText = when {
        isDailyChallenge -> "DAILY CLEARED!"
        starsEarned == 3 -> "PERFECT!"
        starsEarned == 2 -> "EXCELLENT!"
        else -> "LEVEL COMPLETE!"
    }

    val sublineText = when {
        isDailyChallenge -> "Daily puzzle solved smoothly"
        starsEarned == 3 -> "Level $levelNumber cleared with 0 mistakes!"
        starsEarned == 2 -> "Level $levelNumber cleared with 2 stars"
        else -> "Level $levelNumber cleared! Keep going"
    }

    val activeStarColor = Color(0xFFFFB800)
    val inactiveStarColor = if (theme.isDark) Color(0xFF475569) else Color(0xFFCBD5E1)

    Surface(
        color = theme.background,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val availableHeight = maxHeight
            // Requires vertical scroll only on extreme small-screen heights (e.g. landscape or split screen)
            val requiresScroll = availableHeight < 520.dp
            val isCompactHeight = availableHeight < 680.dp

            val contentModifier = if (requiresScroll) {
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = if (isCompactHeight) 8.dp else 16.dp)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = contentModifier
            ) {
                // 1. TOP NAV BAR: Home icon (left) & Level (center) & Total Coins balance pill (right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Button
                    IconButton(
                        onClick = {
                            onPlayClickSound()
                            onHomeClick()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (theme.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = theme.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Level label in center
                    Text(
                        text = if (isDailyChallenge) "DAILY CHALLENGE" else "LEVEL $levelNumber",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = theme.textSecondary
                    )

                    // Total Coins Balance Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (theme.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0x22FBBF24)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Balance",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "$totalCoins",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textPrimary
                            )
                        }
                    }
                }

                // Dynamic vertical spacer above hero to center elements
                if (!requiresScroll) {
                    Spacer(modifier = Modifier.weight(if (isCompactHeight) 0.6f else 1.0f))
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 2. HERO CELEBRATION SECTION (Sunburst, Tri-Star Arc, Headline, Subtitle)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp)
                        .scale(contentScale.value)
                ) {
                    // Ambient Rotating Sunburst & Stars
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactHeight) 115.dp else 135.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Sunburst Canvas
                        Canvas(
                            modifier = Modifier
                                .size(if (isCompactHeight) 200.dp else 230.dp)
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
                                                Color(0x38FFB800),
                                                Color(0x14FF9900),
                                                Color.Transparent
                                            ),
                                            center = center,
                                            radius = maxRadius
                                        )
                                    )
                                }
                            }
                        }

                        // Tri-Star Arc
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
                                    .size(if (isCompactHeight) 36.dp else 40.dp)
                                    .scale(star1Scale.value)
                                    .rotate(star1Rotation.value)
                            )

                            Spacer(modifier = Modifier.width(if (isCompactHeight) 12.dp else 16.dp))

                            // Star 2 (Center Hero Star)
                            Box(contentAlignment = Alignment.Center) {
                                if (starsEarned >= 2) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isCompactHeight) 52.dp else 60.dp)
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0x50FFB800),
                                                        Color.Transparent
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Hero Star",
                                    tint = if (starsEarned >= 2) activeStarColor else inactiveStarColor,
                                    modifier = Modifier
                                        .size(if (isCompactHeight) 50.dp else 56.dp)
                                        .scale(star2Scale.value)
                                )
                            }

                            Spacer(modifier = Modifier.width(if (isCompactHeight) 12.dp else 16.dp))

                            // Star 3 (Right)
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star 3",
                                tint = if (starsEarned >= 3) activeStarColor else inactiveStarColor,
                                modifier = Modifier
                                    .size(if (isCompactHeight) 36.dp else 40.dp)
                                    .scale(star3Scale.value)
                                    .rotate(star3Rotation.value)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 10.dp))

                    Text(
                        text = headlineText,
                        fontSize = if (isCompactHeight) 24.sp else 28.sp,
                        fontWeight = FontWeight.Black,
                        color = theme.textPrimary,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 3.dp else 5.dp))

                    Text(
                        text = sublineText,
                        fontSize = if (isCompactHeight) 13.sp else 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                // Dynamic vertical spacer between Hero and Stats
                if (!requiresScroll) {
                    Spacer(modifier = Modifier.weight(if (isCompactHeight) 0.6f else 1.0f))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 3. REWARDS & STATS POD
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp)
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
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "Coins",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
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
                                    text = if (hasClaimedDouble) "2× Coins" else "Coins",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = theme.textSecondary
                                )
                            }
                        }
                    }

                    // Streak Card
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
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "$streakDays Day",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = "Streak",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = theme.textSecondary
                                )
                            }
                        }
                    }
                }

                // Dynamic vertical spacer between Stats and Actions
                if (!requiresScroll) {
                    Spacer(modifier = Modifier.weight(if (isCompactHeight) 0.6f else 1.0f))
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 4. MONETIZATION & ADS POD
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp)
                ) {
                    // Rewarded Video: "Watch Ad to 2x Coins" Button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (hasClaimedDouble) {
                            if (theme.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                        } else {
                            if (theme.isDark) Color(0xFF292524) else Color(0xFFFFFBEB)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (hasClaimedDouble) Color.Transparent else Color(0xFFF59E0B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = !hasClaimedDouble) {
                                hasClaimedDouble = true
                                onPlayCoinSound()
                                onClaimDoubleCoins(coinsEarned)
                            }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            if (hasClaimedDouble) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Claimed",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "2× REWARD CLAIMED!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Watch Ad",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "CLAIM 2× COINS (+${coinsEarned * 2})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = Color(0xFFD97706)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = Color(0xFFF59E0B)
                                ) {
                                    Text(
                                        text = "AD",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // DEDICATED BANNER AD CONTAINER (320x50 Standard Mobile Banner Slot)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (theme.isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (theme.isDark) Color(0x33FFFFFF) else Color(0x1F000000)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Top-left Ad Tag
                            Surface(
                                shape = RoundedCornerShape(bottomEnd = 8.dp),
                                color = if (theme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = "AD",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Banner Ad placeholder content
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Banner Ad Placement",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = theme.textSecondary
                                )
                                Text(
                                    text = "AdMob / Unity Ads (320x50 Banner Slot)",
                                    fontSize = 9.sp,
                                    color = theme.textSecondary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Dynamic vertical spacer between Ads and Primary CTA
                if (!requiresScroll) {
                    Spacer(modifier = Modifier.weight(if (isCompactHeight) 0.8f else 1.2f))
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // 5. PRIMARY CONTINUE CTA BUTTON (NEXT LEVEL)
                Button(
                    onClick = {
                        onPlayClickSound()
                        onNextLevelClick()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.arrowDefault
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 5.dp,
                        pressedElevation = 2.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp)
                        .height(54.dp)
                        .scale(buttonPulse)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isDailyChallenge) "CLAIM & RETURN HOME" else "NEXT LEVEL",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (theme.isDark) Color(0xFF0F172A) else Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "➔",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (theme.isDark) Color(0xFF0F172A) else Color.White
                        )
                    }
                }

                // Bottom dynamic vertical spacer
                if (!requiresScroll) {
                    Spacer(modifier = Modifier.weight(if (isCompactHeight) 0.4f else 0.8f))
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
