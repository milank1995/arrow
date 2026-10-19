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
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.launch

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

    // Subtle idle attention pulse + radiating halo for the Hint icon
    val idleTransition = rememberInfiniteTransition(label = "idleHintTransition")
    val idlePulseScale by idleTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idlePulse"
    )
    val idleHaloScale by idleTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idleHaloScale"
    )
    val idleHaloAlpha by idleTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idleHaloAlpha"
    )

    // Clean, transparent, unclipped bottom bar with ample bottom padding
    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 44.dp), // Generous bottom padding to clear navigation bar
            horizontalArrangement = Arrangement.spacedBy(if (onRestartClick != null) 38.dp else 56.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. HINT ICON (Unclipped container, larger icon, badge securely positioned)
            val combinedHintScale = hintPressScale.value * if (isIdlePromptActive) idlePulseScale else 1f

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 68.dp, height = 76.dp)
                    .scale(combinedHintScale)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        coroutineScope.launch {
                            hintPressScale.animateTo(0.82f, tween(60))
                            hintPressScale.animateTo(1.12f, tween(90))
                            hintPressScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        onHintClick()
                    }
            ) {
                // Radiating idle halo ring
                if (isIdlePromptActive) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = 2.dp)
                            .scale(idleHaloScale)
                            .clip(CircleShape)
                            .border(1.5.dp, theme.arrowHint.copy(alpha = idleHaloAlpha), CircleShape)
                    )
                }

                // Generously Sized Vector Lightbulb (42dp for easy recognition)
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Hint",
                    tint = theme.arrowHint,
                    modifier = Modifier
                        .size(42.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 2.dp)
                )

                // Clean Badge Pill: Count Number or Mini Play Icon (No "AD Free" text)
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = theme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, theme.gridLine),
                    shadowElevation = 2.dp,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hintsCount > 0) {
                            Text(
                                text = "$hintsCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textPrimary
                            )
                        } else {
                            // Clean play triangle icon
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Watch Ad for Free Hint",
                                tint = theme.arrowHint,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // 2. GRID GUIDE ICON (Unclipped container, larger icon, intact eye badge)
            val gridColor = if (isGridLinesActive) theme.arrowDefault else theme.textSecondary.copy(alpha = 0.75f)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 68.dp, height = 76.dp)
                    .scale(gridPressScale.value)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        coroutineScope.launch {
                            gridPressScale.animateTo(0.82f, tween(60))
                            gridPressScale.animateTo(1.12f, tween(90))
                            gridPressScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        onGridClick()
                    }
            ) {
                // Generously Sized Vector Grid Icon (38dp) with smooth rounded stroke caps
                Canvas(
                    modifier = Modifier
                        .size(38.dp)
                        .align(Alignment.Center)
                ) {
                    val strokeW = 2.8.dp.toPx()
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

                // Intact Miniature Eye Badge at top-right (NOT clipped)
                Surface(
                    shape = CircleShape,
                    color = if (isGridLinesActive) theme.arrowDefault else theme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isGridLinesActive) theme.arrowDefault else theme.gridLine
                    ),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Grid Visibility",
                            tint = if (isGridLinesActive) {
                                if (theme.isDark) Color.Black else Color.White
                            } else theme.textSecondary,
                            modifier = Modifier.size(13.dp)
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
                        .size(width = 68.dp, height = 76.dp)
                        .scale(restartPressScale.value)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                restartPressScale.animateTo(0.82f, tween(60))
                                restartPressScale.animateTo(1.12f, tween(90))
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
                            .size(38.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}
