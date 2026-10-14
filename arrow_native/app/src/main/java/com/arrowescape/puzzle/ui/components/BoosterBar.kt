package com.arrowescape.puzzle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.theme.ThemePreset

@Composable
fun BoosterBar(
    hintsCount: Int,
    hammersCount: Int,
    wandsCount: Int,
    isHammerActive: Boolean,
    isGridLinesActive: Boolean,
    theme: ThemePreset,
    onHintClick: () -> Unit,
    onHammerClick: () -> Unit,
    onWandClick: () -> Unit,
    onGridClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = theme.surface,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Hint Booster
            BoosterButton(
                icon = Icons.Default.Lightbulb,
                label = "Hint",
                badgeText = "$hintsCount",
                isActive = false,
                theme = theme,
                onClick = onHintClick
            )

            // 2. Hammer Booster
            BoosterButton(
                icon = Icons.Default.Build,
                label = "Hammer",
                badgeText = "$hammersCount",
                isActive = isHammerActive,
                theme = theme,
                onClick = onHammerClick
            )

            // 3. Magic Wand Booster
            BoosterButton(
                icon = Icons.Default.AutoFixHigh,
                label = "Wand",
                badgeText = "$wandsCount",
                isActive = false,
                theme = theme,
                onClick = onWandClick
            )

            // 4. Grid Lines Toggle
            BoosterButton(
                icon = if (isGridLinesActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                label = "Grid",
                badgeText = if (isGridLinesActive) "ON" else "OFF",
                isActive = isGridLinesActive,
                theme = theme,
                onClick = onGridClick
            )
        }
    }
}

@Composable
private fun BoosterButton(
    icon: ImageVector,
    label: String,
    badgeText: String,
    isActive: Boolean,
    theme: ThemePreset,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = if (isActive) Color(0xFFF59E0B) else theme.arrowDefault,
                    contentColor = Color.White
                ) {
                    Text(text = badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) Color(0xFFF59E0B).copy(alpha = 0.2f) else theme.background
                    )
                    .border(
                        width = if (isActive) 2.dp else 1.dp,
                        color = if (isActive) Color(0xFFF59E0B) else theme.gridDot,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) Color(0xFFF59E0B) else theme.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = theme.textSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
