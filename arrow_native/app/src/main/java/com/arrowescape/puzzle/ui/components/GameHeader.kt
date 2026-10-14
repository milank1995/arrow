package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.theme.ThemePreset

@Composable
fun GameHeader(
    levelNumber: Int,
    heartsRemaining: Int,
    maxHearts: Int,
    remainingArrowsCount: Int,
    totalArrowsCount: Int = remainingArrowsCount,
    theme: ThemePreset,
    onBackClick: () -> Unit,
    onThemeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clearedCount = (totalArrowsCount - remainingArrowsCount).coerceAtLeast(0)
    val targetProgress = if (totalArrowsCount > 0) clearedCount.toFloat() / totalArrowsCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "headerProgress"
    )

    Surface(
        color = Color.White,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // LEFT: Pause icon + Theme palette pod
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pause button (two vertical bars)
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Theme palette button inside rounded pod
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEF2F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onThemeClick,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Themes",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // CENTER: 3 Red Hearts + "Level X"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 3 Hearts Row with rich coral-red tint and depth
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until maxHearts) {
                            val isAlive = i < heartsRemaining
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Heart",
                                tint = if (isAlive) Color(0xFFFF4B4B) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Level text directly below hearts
                    Text(
                        text = "Level $levelNumber",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }

                // RIGHT: Remaining Arrows Badge (solid bold ↗ icon + count, matching Screenshot 1)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(0xFFEEF2F6))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            drawPath(path = path, color = Color(0xFF1E293B))
                        }

                        Text(
                            text = "$remainingArrowsCount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }

            // Subtle, thin progress bar along the bottom border of the header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color(0xFFF1F5F9))
            ) {
                if (animatedProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topEnd = 1.25.dp, bottomEnd = 1.25.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6), // Vibrant Electric Blue
                                        Color(0xFF06B6D4)  // Cyan
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
