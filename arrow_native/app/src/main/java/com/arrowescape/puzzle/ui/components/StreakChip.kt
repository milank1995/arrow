package com.arrowescape.puzzle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.theme.ThemePreset

/**
 * Standardized Streak Capsule Pill used consistently across the Home screen and Victory screen.
 */
@Composable
fun StreakChip(
    streakDays: Int,
    theme: ThemePreset,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = theme.surface,
        border = BorderStroke(1.dp, theme.gridLine),
        shadowElevation = 1.dp,
        modifier = modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Text(text = "🔥", fontSize = 15.sp)
            Text(
                text = "$streakDays Day Streak",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (theme.isDark) Color(0xFFFDE047) else Color(0xFFD97706)
            )
        }
    }
}
