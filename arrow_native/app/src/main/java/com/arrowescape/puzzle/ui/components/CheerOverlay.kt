package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modern arcade running-style banner that glides horizontally beneath the header.
 * Alternates directional flow:
 *   - fromLeft = true: Glides smoothly from Left -> Center, pauses, then exits toward Right.
 *   - fromLeft = false: Glides smoothly from Right -> Center, pauses, then exits toward Left.
 */
@Composable
fun CheerOverlay(
    message: String?,
    fromLeft: Boolean = false,
    modifier: Modifier = Modifier
) {
    val enterTransition = if (fromLeft) {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth - 180 },
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(250))
    } else {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth + 180 },
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(250))
    }

    val exitTransition = if (fromLeft) {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth + 180 },
            animationSpec = tween(durationMillis = 500, easing = FastOutLinearInEasing)
        ) + fadeOut(animationSpec = tween(250))
    } else {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth - 180 },
            animationSpec = tween(durationMillis = 500, easing = FastOutLinearInEasing)
        ) + fadeOut(animationSpec = tween(250))
    }

    AnimatedVisibility(
        visible = message != null,
        enter = enterTransition,
        exit = exitTransition,
        modifier = modifier
    ) {
        if (message != null) {
            val tier = getCheerTier(message)
            val pillShape = RoundedCornerShape(percent = 50)

            Box(
                modifier = Modifier
                    .shadow(elevation = 6.dp, shape = pillShape, ambientColor = tier.glowColor, spotColor = tier.glowColor)
                    .clip(pillShape)
                    .background(Brush.horizontalGradient(tier.gradientColors))
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.20f))
                        ),
                        shape = pillShape
                    )
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Small circular icon pod
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tier.icon,
                            fontSize = 12.sp
                        )
                    }

                    // Impressive, high-contrast running banner text
                    Text(
                        text = message.uppercase(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }
    }
}

private data class CheerTier(
    val icon: String,
    val gradientColors: List<Color>,
    val glowColor: Color
)

private fun getCheerTier(message: String): CheerTier {
    val clean = message.trim().uppercase()
    return when {
        clean.contains("NICE") -> CheerTier(
            icon = "⚡",
            gradientColors = listOf(Color(0xFF0284C7), Color(0xFF06B6D4)), // Electric Azure & Cyan
            glowColor = Color(0xFF0284C7)
        )
        clean.contains("GREAT") -> CheerTier(
            icon = "🔥",
            gradientColors = listOf(Color(0xFF059669), Color(0xFF10B981)), // Emerald Energy
            glowColor = Color(0xFF059669)
        )
        clean.contains("AWESOME") -> CheerTier(
            icon = "⭐",
            gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFEA580C)), // Luminous Amber & Flame
            glowColor = Color(0xFFF59E0B)
        )
        clean.contains("UNSTOPPABLE") -> CheerTier(
            icon = "💥",
            gradientColors = listOf(Color(0xFF7C3AED), Color(0xFFEC4899)), // Royal Violet & Neon Magenta
            glowColor = Color(0xFF7C3AED)
        )
        clean.contains("MASTERFUL") -> CheerTier(
            icon = "👑",
            gradientColors = listOf(Color(0xFFE11D48), Color(0xFFF59E0B)), // Crimson & Radiant Gold
            glowColor = Color(0xFFE11D48)
        )
        else -> CheerTier(
            icon = "✨",
            gradientColors = listOf(Color(0xFF0088F5), Color(0xFF06B6D4)), // Game Default Dodger Blue
            glowColor = Color(0xFF0088F5)
        )
    }
}
