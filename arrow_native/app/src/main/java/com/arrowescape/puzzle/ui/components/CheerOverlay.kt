package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimalist, elegant floating glass capsule for milestone celebrations and combo praise.
 * Designed to be clean, subtle, and non-distracting while providing positive gameplay feedback.
 */
@Composable
fun CheerOverlay(
    message: String?,
    fromLeft: Boolean = false,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(
            initialOffsetY = { -it / 2 },
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutVertically(
            targetOffsetY = { -it / 3 },
            animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
        ) + fadeOut(animationSpec = tween(180)),
        modifier = modifier
    ) {
        if (message != null) {
            val tier = parseCheerInfo(message)
            val pillShape = CircleShape

            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 4.dp,
                        shape = pillShape,
                        ambientColor = tier.glowColor.copy(alpha = 0.30f),
                        spotColor = tier.glowColor.copy(alpha = 0.40f)
                    )
                    .clip(pillShape)
                    .background(Color(0xE60F172A)) // Sleek dark slate frosted glass (~90% opacity)
                    .border(
                        width = 1.dp,
                        color = tier.borderColor,
                        shape = pillShape
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Small contextual icon
                    Text(
                        text = tier.icon,
                        fontSize = 12.sp,
                        lineHeight = 12.sp
                    )

                    // Optional milestone / combo prefix (e.g. "50%" or "5x")
                    if (tier.badge != null) {
                        Text(
                            text = tier.badge,
                            color = tier.accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp
                        )

                        Text(
                            text = "·",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Main crisp praise message
                    Text(
                        text = tier.text,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }
    }
}

private data class CheerTier(
    val icon: String,
    val badge: String?,
    val text: String,
    val borderColor: Color,
    val glowColor: Color,
    val accentColor: Color
)

private fun parseCheerInfo(rawMessage: String): CheerTier {
    val trimmed = rawMessage.trim()
    val parts = trimmed.split("·", limit = 2).map { it.trim() }

    val badge: String?
    val text: String
    if (parts.size == 2) {
        badge = parts[0]
        text = parts[1]
    } else {
        badge = null
        text = trimmed
    }

    val upperBadge = badge?.uppercase() ?: ""
    val upperText = text.uppercase()

    return when {
        // Milestone 25% (Emerald)
        upperBadge.contains("25%") -> CheerTier(
            icon = "🌱",
            badge = badge,
            text = text,
            borderColor = Color(0x6610B981),
            glowColor = Color(0xFF10B981),
            accentColor = Color(0xFF34D399)
        )

        // Milestone 50% (Luminous Gold / Amber)
        upperBadge.contains("50%") -> CheerTier(
            icon = "👑",
            badge = badge,
            text = text,
            borderColor = Color(0x88F59E0B),
            glowColor = Color(0xFFF59E0B),
            accentColor = Color(0xFFFBBF24)
        )

        // Milestone 75% (Royal Violet / Fuchsia)
        upperBadge.contains("75%") -> CheerTier(
            icon = "⚡",
            badge = badge,
            text = text,
            borderColor = Color(0x66A855F7),
            glowColor = Color(0xFFA855F7),
            accentColor = Color(0xFFC084FC)
        )

        // Milestone 90% (Soft Coral / Rose)
        upperBadge.contains("90%") -> CheerTier(
            icon = "🎯",
            badge = badge,
            text = text,
            borderColor = Color(0x66F43F5E),
            glowColor = Color(0xFFF43F5E),
            accentColor = Color(0xFFFB7185)
        )

        // Combo 3x (Sky Azure)
        upperBadge.contains("3X") || upperText.contains("NICE") -> CheerTier(
            icon = "⚡",
            badge = badge,
            text = text,
            borderColor = Color(0x550284C7),
            glowColor = Color(0xFF0284C7),
            accentColor = Color(0xFF38BDF8)
        )

        // Combo 5x (Emerald Energy)
        upperBadge.contains("5X") || upperText.contains("GREAT") || upperText.contains("ON FIRE") -> CheerTier(
            icon = "🔥",
            badge = badge,
            text = text,
            borderColor = Color(0x66059669),
            glowColor = Color(0xFF059669),
            accentColor = Color(0xFF34D399)
        )

        // Combo 8x (Luminous Amber)
        upperBadge.contains("8X") || upperText.contains("AWESOME") || upperText.contains("MAESTRO") -> CheerTier(
            icon = "⭐",
            badge = badge,
            text = text,
            borderColor = Color(0x66F59E0B),
            glowColor = Color(0xFFF59E0B),
            accentColor = Color(0xFFFBBF24)
        )

        // Combo 12x (Royal Purple)
        upperBadge.contains("12X") || upperText.contains("GALAXY") || upperText.contains("UNSTOPPABLE") -> CheerTier(
            icon = "💥",
            badge = badge,
            text = text,
            borderColor = Color(0x667C3AED),
            glowColor = Color(0xFF7C3AED),
            accentColor = Color(0xFFA855F7)
        )

        // Combo 16x+ (Legendary Crimson / Rose)
        upperBadge.contains("16X") || upperText.contains("MASTERFUL") || upperText.contains("ASCENDED") -> CheerTier(
            icon = "👑",
            badge = badge,
            text = text,
            borderColor = Color(0x66E11D48),
            glowColor = Color(0xFFE11D48),
            accentColor = Color(0xFFF43F5E)
        )

        // Fallback default
        else -> CheerTier(
            icon = "✨",
            badge = badge,
            text = text,
            borderColor = Color(0x550088F5),
            glowColor = Color(0xFF0088F5),
            accentColor = Color(0xFF38BDF8)
        )
    }
}
