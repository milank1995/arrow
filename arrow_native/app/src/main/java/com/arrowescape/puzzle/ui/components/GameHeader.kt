package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.theme.ThemePreset
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameHeader(
    levelNumber: Int,
    heartsRemaining: Int,
    maxHearts: Int,
    remainingArrowsCount: Int,
    totalArrowsCount: Int = remainingArrowsCount,
    theme: ThemePreset,
    isDailyChallenge: Boolean = false,
    dailyDateString: String? = null,
    shapeName: String? = null,
    isTestMode: Boolean = false,
    onPrevLevelClick: (() -> Unit)? = null,
    onNextLevelClick: (() -> Unit)? = null,
    onLevelNumberClick: (() -> Unit)? = null,
    onPauseClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onSoundToggle: (() -> Unit)? = null,
    onSoundLongPress: (() -> Unit)? = null,
    onThemeClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    isSoundOn: Boolean = true,
    modifier: Modifier = Modifier
) {
    val clearedCount = (totalArrowsCount - remainingArrowsCount).coerceAtLeast(0)
    val targetProgress = if (totalArrowsCount > 0) clearedCount.toFloat() / totalArrowsCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "headerProgress"
    )

    val effectivePause = onPauseClick ?: onBackClick ?: {}
    val effectiveSoundToggle = onSoundToggle ?: onSettingsClick ?: {}
    val effectiveSoundLongPress = onSoundLongPress ?: onSettingsClick

    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // LEFT: Clean, minimal HUD pods (Pause || + Dual-Action Sound 🔊/🔇)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Pause button inside rounded tactile pod
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = theme.boosterBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = effectivePause)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause Game",
                                tint = theme.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 2. Dual-action Sound button: Quick tap toggles audio, Hold for settings
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = theme.boosterBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = effectiveSoundToggle,
                                onLongClick = effectiveSoundLongPress
                            )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isSoundOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                                contentDescription = if (isSoundOn) "Mute Sound (Hold for Settings)" else "Unmute Sound (Hold for Settings)",
                                tint = if (isSoundOn) theme.textPrimary else theme.textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Optional Dev Theme button (shown only in test mode)
                    if (isTestMode && onThemeClick != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = theme.boosterBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onThemeClick)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Themes",
                                    tint = theme.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // CENTER: 3 Red Hearts + "Level X"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    var prevHearts by remember { mutableIntStateOf(heartsRemaining) }
                    var brokenHeartIndex by remember { mutableStateOf<Int?>(null) }

                    LaunchedEffect(heartsRemaining) {
                        if (heartsRemaining < prevHearts) {
                            brokenHeartIndex = heartsRemaining
                        } else if (heartsRemaining >= maxHearts) {
                            brokenHeartIndex = null
                        }
                        prevHearts = heartsRemaining
                    }

                    // 3 Hearts Row with rich coral-red tint and depth + animated break explosion
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until maxHearts) {
                            val isAlive = i < heartsRemaining
                            val isBreaking = (i == brokenHeartIndex)
                            AnimatedHeart(
                                isAlive = isAlive,
                                isBreaking = isBreaking,
                                onBreakFinished = {
                                    if (brokenHeartIndex == i) {
                                        brokenHeartIndex = null
                                    }
                                }
                            )
                        }
                    }

                    // Level text directly below hearts
                    if (isTestMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            if (onPrevLevelClick != null) {
                                IconButton(
                                    onClick = onPrevLevelClick,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous Level",
                                        tint = theme.textPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = theme.boosterBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                                modifier = Modifier.clickable(enabled = onLevelNumberClick != null) {
                                    onLevelNumberClick?.invoke()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Level $levelNumber",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textPrimary
                                    )
                                    if (onLevelNumberClick != null) {
                                        Text(
                                            text = "▾",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.textSecondary
                                        )
                                    }
                                }
                            }
                            if (onNextLevelClick != null) {
                                IconButton(
                                    onClick = onNextLevelClick,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next Level",
                                        tint = theme.textPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = when {
                                isDailyChallenge -> "Daily Challenge"
                                !shapeName.isNullOrBlank() -> "Level $levelNumber • $shapeName"
                                else -> "Level $levelNumber"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }

                // RIGHT: Remaining Arrows Goal Badge
                val badgeScale = remember { Animatable(1f) }
                var prevRemainingCount by remember { mutableIntStateOf(remainingArrowsCount) }

                LaunchedEffect(remainingArrowsCount) {
                    if (remainingArrowsCount < prevRemainingCount) {
                        badgeScale.animateTo(1.18f, tween(65, easing = FastOutSlowInEasing))
                        badgeScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                    prevRemainingCount = remainingArrowsCount
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = theme.boosterBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .height(40.dp)
                        .scale(badgeScale.value)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        // Bold solid directional arrow icon matching Screenshot 1
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(15.dp)) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(size.width * 0.95f, size.height * 0.05f)
                                lineTo(size.width * 0.95f, size.height * 0.65f)
                                lineTo(size.width * 0.72f, size.height * 0.42f)
                                lineTo(size.width * 0.22f, size.height * 0.92f)
                                lineTo(size.width * 0.08f, size.height * 0.78f)
                                lineTo(size.width * 0.58f, size.height * 0.28f)
                                lineTo(size.width * 0.35f, size.height * 0.05f)
                                close()
                            }
                            drawPath(path = path, color = theme.textPrimary)
                        }

                        Text(
                            text = "$remainingArrowsCount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary
                        )
                    }
                }
            }

            // Sleek continuous HUD progress bar under header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(theme.gridLine)
            ) {
                if (animatedProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        theme.arrowClear,
                                        theme.arrowDefault
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

private data class HeartShard(
    val vx: Float,
    val vy: Float,
    val size: Float
)

@Composable
private fun AnimatedHeart(
    isAlive: Boolean,
    isBreaking: Boolean,
    onBreakFinished: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val breakProgress = remember { Animatable(0f) }

    val shards = remember {
        (0 until 7).map { i ->
            val angle = (i * 51.4f + kotlin.random.Random.nextFloat() * 20f - 10f) * (PI.toFloat() / 180f)
            val speed = 28f + kotlin.random.Random.nextFloat() * 22f
            HeartShard(
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                size = 2.4f + kotlin.random.Random.nextFloat() * 1.6f
            )
        }
    }

    LaunchedEffect(isBreaking) {
        if (isBreaking) {
            breakProgress.snapTo(0f)
            // Recoil scale pop + shake rotation + break shards explosion matching Unity HeartAnimationController
            launch {
                scale.animateTo(1.38f, tween(90, easing = FastOutSlowInEasing))
                scale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            launch {
                rotation.animateTo(-18f, tween(65))
                rotation.animateTo(15f, tween(75))
                rotation.animateTo(-8f, tween(65))
                rotation.animateTo(0f, tween(65))
            }
            launch {
                breakProgress.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
                onBreakFinished()
            }
        }
    }

    val heartColor = when {
        isBreaking -> {
            val t = breakProgress.value
            // Cross-fade from bright coral-red to inactive slate-gray
            Color(
                red = androidx.compose.ui.util.lerp(1f, 0.796f, t),
                green = androidx.compose.ui.util.lerp(0.294f, 0.835f, t),
                blue = androidx.compose.ui.util.lerp(0.294f, 0.882f, t),
                alpha = 1f
            )
        }
        isAlive -> Color(0xFFFF4B4B)
        else -> Color(0xFFCBD5E1)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(22.dp)
            .scale(scale.value)
            .rotate(rotation.value)
            .drawWithContent {
                drawContent()
                // Render break shard particles bursting outward
                if (isBreaking && breakProgress.value < 1f) {
                    val p = breakProgress.value
                    val shardAlpha = (1f - p).coerceIn(0f, 1f)
                    val density = this.density
                    val center = Offset(size.width / 2f, size.height / 2f)

                    for (shard in shards) {
                        val pos = center + Offset(shard.vx * p * density, shard.vy * p * density)
                        val radius = (shard.size * (1f - p * 0.5f)) * density
                        drawCircle(
                            color = Color(0xFFFF4B4B).copy(alpha = shardAlpha),
                            radius = radius,
                            center = pos
                        )
                    }
                }
            }
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Heart",
            tint = heartColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

