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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
                .padding(top = 4.dp, bottom = 22.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Modern Floating Booster Dock Tray
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = theme.surface.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine.copy(alpha = 0.5f)),
                shadowElevation = 5.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. HINT BOOSTER POD
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.scale(hintScale.value)
                    ) {
                        Box(contentAlignment = Alignment.BottomCenter) {
                            Surface(
                                shape = CircleShape,
                                color = theme.boosterBg,
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFDE68A)), // Warm amber accent ring
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(58.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            hintScale.animateTo(0.86f, tween(60))
                                            hintScale.animateTo(1.10f, tween(90))
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
                                        tint = Color(0xFFF59E0B), // Warm amber lightbulb
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

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Hint",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary
                        )
                    }

                    // 2. GRID GUIDE (#) BOOSTER POD (With Unity-style miniature Eye Badge)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.scale(gridScale.value)
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Surface(
                                shape = CircleShape,
                                color = if (isGridLinesActive) {
                                    if (theme.isDark) theme.arrowDefault.copy(alpha = 0.25f) else Color(0xFFEFF6FF)
                                } else {
                                    theme.boosterBg
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isGridLinesActive) Color(0xFF3B82F6) else theme.gridLine
                                ),
                                shadowElevation = if (isGridLinesActive) 3.dp else 2.dp,
                                modifier = Modifier
                                    .size(58.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            gridScale.animateTo(0.86f, tween(60))
                                            gridScale.animateTo(1.10f, tween(90))
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
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGridLinesActive) Color(0xFF2563EB) else theme.textPrimary
                                    )
                                }
                            }

                            // Miniature Eye Badge at top-right (matching Unity reference)
                            Surface(
                                modifier = Modifier.offset(x = 4.dp, y = (-2).dp),
                                shape = CircleShape,
                                color = if (isGridLinesActive) Color(0xFF2563EB) else theme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isGridLinesActive) Color(0xFF2563EB) else theme.gridLine
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
                                        tint = if (isGridLinesActive) Color.White else theme.textPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Grid",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGridLinesActive) Color(0xFF2563EB) else theme.textPrimary
                        )
                    }
                }
            }
        }
    }
}
