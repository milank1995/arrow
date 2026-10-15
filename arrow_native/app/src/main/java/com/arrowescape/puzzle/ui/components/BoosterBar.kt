package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    theme: ThemePreset,
    onHintClick: () -> Unit,
    onGridClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val hintScale = remember { Animatable(1f) }
    val gridScale = remember { Animatable(1f) }

    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Hint Companion Pod
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .scale(hintScale.value)
            ) {
                Surface(
                    shape = CircleShape,
                    color = theme.boosterBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(62.dp)
                        .clickable {
                            coroutineScope.launch {
                                hintScale.animateTo(0.85f, tween(60))
                                hintScale.animateTo(1.12f, tween(90))
                                hintScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
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
                            tint = theme.textPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Count or [AD] Free Badge overlapping bottom
                Surface(
                    modifier = Modifier.offset(y = 8.dp),
                    shape = RoundedCornerShape(percent = 50),
                    color = theme.surface,
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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

            Spacer(modifier = Modifier.size(16.dp))

            // 2. Grid (#) Companion Pod
            Box(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .scale(gridScale.value),
                contentAlignment = Alignment.Center
            ) {
                val gridPodBg = if (isGridLinesActive) {
                    if (theme.isDark) theme.arrowDefault else Color(0xFF3B82F6)
                } else {
                    theme.boosterBg
                }
                val gridPodTextColor = if (isGridLinesActive) {
                    if (theme.isDark) Color(0xFF0F172A) else Color.White
                } else {
                    theme.textPrimary
                }

                Surface(
                    shape = CircleShape,
                    color = gridPodBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isGridLinesActive) Color.Transparent else theme.gridLine
                    ),
                    shadowElevation = if (isGridLinesActive) 4.dp else 2.dp,
                    modifier = Modifier
                        .size(62.dp)
                        .clickable {
                            coroutineScope.launch {
                                gridScale.animateTo(0.85f, tween(60))
                                gridScale.animateTo(1.12f, tween(90))
                                gridScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
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
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = gridPodTextColor
                        )
                    }
                }
            }
        }
    }
}
