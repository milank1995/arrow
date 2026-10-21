package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowescape.puzzle.ui.theme.ThemePreset

@Composable
fun LevelSelectDialog(
    currentLevelNumber: Int,
    lastCompletedLevelNumber: Int,
    levelStars: Map<Int, Int>,
    theme: ThemePreset,
    onSelectLevel: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val totalLevels = 100
    val pageSize = 20
    val totalPages = (totalLevels + pageSize - 1) / pageSize
    val initialPage = ((currentLevelNumber - 1) / pageSize).coerceIn(0, totalPages - 1)

    var currentPage by remember { mutableIntStateOf(initialPage) }
    val maxUnlocked = maxOf(currentLevelNumber, (levelStars.keys.maxOrNull() ?: 0) + 1)
    val totalStarsEarned = remember(levelStars) { levelStars.values.sum() }

    val infiniteTransition = rememberInfiniteTransition(label = "levelPulse")
    val currentPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "currentPulseScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (theme.isDark) Color(0xFF1E293B) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. HEADER ROW: Title + Total Stars Count + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Level Journey",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (theme.isDark) Color.White else Color(0xFF0F172A)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$totalStarsEarned Stars Collected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (theme.isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (theme.isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (theme.isDark) Color(0xFFE2E8F0) else Color(0xFF475569),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. CHAPTER / PAGE NAVIGATION BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (theme.isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Chapter",
                            tint = if (currentPage > 0) {
                                if (theme.isDark) Color.White else Color(0xFF1E293B)
                            } else Color(0xFF94A3B8).copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    val pageStart = currentPage * pageSize + 1
                    val pageEnd = minOf((currentPage + 1) * pageSize, totalLevels)
                    Text(
                        text = "Chapter ${currentPage + 1} • Levels $pageStart–$pageEnd",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (theme.isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                    )

                    IconButton(
                        onClick = { if (currentPage < totalPages - 1) currentPage++ },
                        enabled = currentPage < totalPages - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Chapter",
                            tint = if (currentPage < totalPages - 1) {
                                if (theme.isDark) Color.White else Color(0xFF1E293B)
                            } else Color(0xFF94A3B8).copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. 4x5 GRID OF LEVEL TILES
                val startLvl = currentPage * pageSize + 1
                val endLvl = minOf((currentPage + 1) * pageSize, totalLevels)
                val levelList = remember(currentPage) { (startLvl..endLvl).toList() }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(levelList.size) { index ->
                        val lvl = levelList[index]
                        val isCurrent = lvl == currentLevelNumber
                        val isUnlocked = lvl <= maxUnlocked
                        val stars = levelStars[lvl] ?: 0

                        val containerColor = when {
                            isCurrent -> if (theme.isDark) Color(0xFF312E81) else Color(0xFFEEF2FF)
                            isUnlocked -> if (theme.isDark) Color(0xFF334155) else Color(0xFFF8FAFC)
                            else -> if (theme.isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF1F5F9).copy(alpha = 0.7f)
                        }

                        val borderColor = when {
                            isCurrent -> Color(0xFF6366F1)
                            isUnlocked -> if (theme.isDark) Color(0xFF475569) else Color(0xFFE2E8F0)
                            else -> Color.Transparent
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = containerColor,
                            border = androidx.compose.foundation.BorderStroke(if (isCurrent) 2.dp else 1.dp, borderColor),
                            shadowElevation = if (isCurrent) 4.dp else 0.dp,
                            modifier = Modifier
                                .aspectRatio(1.0f)
                                .scale(if (isCurrent) currentPulseScale else 1f)
                                .clickable(enabled = isUnlocked) {
                                    onSelectLevel(lvl)
                                    onDismiss()
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (isUnlocked) {
                                    Text(
                                        text = "$lvl",
                                        fontSize = 16.sp,
                                        fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                        color = when {
                                            isCurrent -> Color(0xFF4F46E5)
                                            theme.isDark -> Color.White
                                            else -> Color(0xFF1E293B)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Mini Star Rating (1..3 Stars)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (s in 1..3) {
                                            val isEarned = s <= stars
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (isEarned) Color(0xFFF59E0B) else Color(0xFFCBD5E1).copy(alpha = 0.5f),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                } else {
                                    // Locked Level
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = if (theme.isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$lvl",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (theme.isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
