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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
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
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val hintPressScale = remember { Animatable(1f) }
    val gridPressScale = remember { Animatable(1f) }

    // Subtle idle attention pulse + radiating halo for the Hint button
    val idleTransition = rememberInfiniteTransition(label = "idleHintTransition")
    val idlePulseScale by idleTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idlePulse"
    )
    val idleHaloScale by idleTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.48f,
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

    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 26.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Semi-Transparent Frosted Dock Tray (Clean & Lightweight)
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = theme.surface.copy(alpha = 0.55f),
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine.copy(alpha = 0.50f)),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. HINT BOOSTER POD (Icon-driven, semi-transparent, idle prompt aura)
                    val combinedHintScale = hintPressScale.value * if (isIdlePromptActive) idlePulseScale else 1f

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.scale(combinedHintScale)
                    ) {
                        // Radiating Attention Halo Ring (only visible when idle)
                        if (isIdlePromptActive) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .scale(idleHaloScale)
                                    .clip(CircleShape)
                                    .border(2.dp, theme.arrowHint.copy(alpha = idleHaloAlpha), CircleShape)
                            )
                        }

                        Box(contentAlignment = Alignment.BottomCenter) {
                            // Semi-transparent pod circle
                            Surface(
                                shape = CircleShape,
                                color = theme.boosterBg.copy(alpha = 0.65f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isIdlePromptActive) theme.arrowHint else theme.arrowHint.copy(alpha = 0.5f)
                                ),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .size(58.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            hintPressScale.animateTo(0.86f, tween(60))
                                            hintPressScale.animateTo(1.10f, tween(90))
                                            hintPressScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                        }
                                        onHintClick()
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = "Hint",
                                        tint = theme.arrowHint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // Count or [AD] Free Badge overlapping bottom
                            Surface(
                                modifier = Modifier.offset(y = 8.dp),
                                shape = RoundedCornerShape(percent = 50),
                                color = theme.surface.copy(alpha = 0.90f),
                                shadowElevation = 1.dp,
                                border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    if (hintsCount > 0) {
                                        Text(
                                            text = "$hintsCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.textPrimary
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(theme.textPrimary)
                                                .padding(horizontal = 3.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "AD",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (theme.isDark) theme.background else Color.White
                                            )
                                        }
                                        Text(
                                            text = "Free",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = theme.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. GRID GUIDE (#) BOOSTER POD (Icon-driven, semi-transparent, Unity-style Eye Badge)
                    Box(
                        contentAlignment = Alignment.TopEnd,
                        modifier = Modifier.scale(gridPressScale.value)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isGridLinesActive) {
                                theme.arrowDefault.copy(alpha = 0.20f)
                            } else {
                                theme.boosterBg.copy(alpha = 0.65f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isGridLinesActive) theme.arrowDefault else theme.gridLine
                            ),
                            shadowElevation = if (isGridLinesActive) 2.dp else 1.dp,
                            modifier = Modifier
                                .size(58.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        gridPressScale.animateTo(0.86f, tween(60))
                                        gridPressScale.animateTo(1.10f, tween(90))
                                        gridPressScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                    onGridClick()
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "#",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGridLinesActive) theme.arrowDefault else theme.textPrimary
                                )
                            }
                        }

                        // Miniature Eye Badge at top-right (matching Unity reference)
                        Surface(
                            modifier = Modifier.offset(x = 4.dp, y = (-2).dp),
                            shape = CircleShape,
                            color = if (isGridLinesActive) theme.arrowDefault else theme.surface.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isGridLinesActive) theme.arrowDefault else theme.gridLine
                            ),
                            shadowElevation = 1.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Grid Guides",
                                    tint = if (isGridLinesActive) {
                                        if (theme.isDark) Color.Black else Color.White
                                    } else theme.textPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
