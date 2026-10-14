package com.arrowescape.puzzle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.core.model.LevelDifficulty
import com.arrowescape.puzzle.ui.theme.ThemePreset

@Composable
fun GameHeader(
    levelNumber: Int,
    difficulty: LevelDifficulty,
    heartsRemaining: Int,
    maxHearts: Int,
    coins: Int,
    theme: ThemePreset,
    onBackClick: () -> Unit,
    onThemeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = theme.surface,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Back button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(theme.background)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = theme.textPrimary
                )
            }

            // Level & Difficulty Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Level $levelNumber",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary
                )
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (difficulty) {
                                LevelDifficulty.EASY -> Color(0xFF10B981).copy(alpha = 0.15f)
                                LevelDifficulty.MEDIUM -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                LevelDifficulty.HARD -> Color(0xFFEF4444).copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = difficulty.label.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when (difficulty) {
                            LevelDifficulty.EASY -> Color(0xFF059669)
                            LevelDifficulty.MEDIUM -> Color(0xFFD97706)
                            LevelDifficulty.HARD -> Color(0xFFDC2626)
                        }
                    )
                }
            }

            // Stats: Hearts & Theme Switcher
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Hearts counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Hearts",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$heartsRemaining",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFFEF4444)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Theme switcher button
                IconButton(
                    onClick = onThemeClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(theme.background)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Cycle Theme",
                        tint = theme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
