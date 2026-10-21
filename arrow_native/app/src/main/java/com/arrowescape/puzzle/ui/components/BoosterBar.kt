package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.launch

/**
 * Modern, borderless, airy booster controls.
 * Replaces hard box/pill containers with clean floating icons and subtle badges.
 */
@Composable
fun BoosterBar(
    hintsCount: Int,
    isGridLinesActive: Boolean,
    isIdlePromptActive: Boolean = false,
    theme: ThemePreset,
    onHintClick: () -> Unit,
    onGridClick: () -> Unit,
    onRestartClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val hintPressScale = remember { Animatable(1f) }
    val gridPressScale = remember { Animatable(1f) }

    // Subtle breathing pulse for Hint icon when stuck
    val idleTransition = rememberInfiniteTransition(label = "idleHintTransition")
    val idlePulseScale by idleTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idlePulse"
    )

    // Smart Idle-Reveal: Ghost state (alpha 0.45) while solving, waking to 1.0 when stuck
    val hintGhostAlpha by animateFloatAsState(
        targetValue = if (isIdlePromptActive) 1.0f else 0.45f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "hintGhostAlpha"
    )

    // Rich warm amber color for high-contrast lightbulb
    val amberBulbColor = Color(0xFFD97706)

    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(if (onRestartClick != null) 36.dp else 52.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. HINT ICON (Clean, borderless, floating with delicate badge)
            val combinedHintScale = hintPressScale.value * if (isIdlePromptActive) idlePulseScale else 1f

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 64.dp, height = 72.dp)
                    .alpha(hintGhostAlpha)
                    .scale(combinedHintScale)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        coroutineScope.launch {
                            hintPressScale.animateTo(0.85f, tween(60))
                            hintPressScale.animateTo(1.10f, tween(90))
                            hintPressScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        onHintClick()
                    }
            ) {
                // High-contrast, vibrant warm amber lightbulb icon
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Hint",
                    tint = amberBulbColor,
                    modifier = Modifier
                        .size(38.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 2.dp)
                )

                // Delicate badge pill docked at bottom
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = theme.surface,
                    border = BorderStroke(1.dp, theme.gridLine),
                    shadowElevation = 1.dp,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hintsCount > 0) {
                            Text(
                                text = "$hintsCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Watch Ad for Free Hint",
                                tint = amberBulbColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // 2. GRID GUIDE ICON (Clean, borderless, floating with miniature eye badge)
            val gridColor = if (isGridLinesActive) theme.arrowDefault else theme.textSecondary.copy(alpha = 0.75f)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 64.dp, height = 72.dp)
                    .scale(gridPressScale.value)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        coroutineScope.launch {
                            gridPressScale.animateTo(0.85f, tween(60))
                            gridPressScale.animateTo(1.10f, tween(90))
                            gridPressScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        onGridClick()
                    }
            ) {
                // Crisp Grid icon
                Canvas(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                ) {
                    val strokeW = 2.6.dp.toPx()
                    val pad = 3.dp.toPx()
                    val w = size.width
                    val h = size.height

                    // 2 horizontal grid bars
                    drawLine(
                        color = gridColor,
                        start = Offset(pad, h * 0.36f),
                        end = Offset(w - pad, h * 0.36f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(pad, h * 0.64f),
                        end = Offset(w - pad, h * 0.64f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    // 2 vertical grid bars
                    drawLine(
                        color = gridColor,
                        start = Offset(w * 0.36f, pad),
                        end = Offset(w * 0.36f, h - pad),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(w * 0.64f, pad),
                        end = Offset(w * 0.64f, h - pad),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }

                // Miniature Eye Badge
                Surface(
                    shape = CircleShape,
                    color = if (isGridLinesActive) theme.arrowDefault else theme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isGridLinesActive) theme.arrowDefault else theme.gridLine
                    ),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Grid Visibility",
                            tint = if (isGridLinesActive) {
                                if (theme.isDark) Color.Black else Color.White
                            } else theme.textSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // 3. RESTART ICON (Shown in test mode)
            if (onRestartClick != null) {
                val restartPressScale = remember { Animatable(1f) }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 64.dp, height = 72.dp)
                        .scale(restartPressScale.value)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                restartPressScale.animateTo(0.85f, tween(60))
                                restartPressScale.animateTo(1.10f, tween(90))
                                restartPressScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                            onRestartClick()
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart Level",
                        tint = theme.textPrimary,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}
