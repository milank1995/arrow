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
import com.arrowescape.puzzle.ui.theme.ThemePreset

/**
 * Minimalist, theme-adaptive floating glass capsule for milestone celebrations and combo praise.
 * - In Light & Sepia themes: Renders as a crisp, warm, elevated surface with high-contrast text.
 * - In Dark themes: Renders as a sleek, luminous frosted slate capsule.
 * - Non-blocking, smooth vertical slide-in and gentle exit.
 */
@Composable
fun CheerOverlay(
    message: String?,
    theme: ThemePreset,
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

            // Compute theme-harmonic background, border, and typography colors
            val isDarkTheme = theme.isDark
            val capsuleBg = if (isDarkTheme) {
                Color(0xEE1E293B) // Frosted slate-800 glass in dark mode
            } else {
                theme.surface.copy(alpha = 0.96f) // Crisp elevated surface in light/sepia
            }

            val capsuleBorder = if (isDarkTheme) {
                tier.accentColor.copy(alpha = 0.35f)
            } else {
                tier.lightAccentColor.copy(alpha = 0.30f)
            }

            val badgeTextColor = if (isDarkTheme) tier.accentColor else tier.lightAccentColor
            val mainTextColor = if (isDarkTheme) Color(0xFFF8FAFC) else theme.textPrimary
            val dotColor = if (isDarkTheme) Color(0xFF64748B) else theme.textSecondary.copy(alpha = 0.55f)

            val shadowAmbient = if (isDarkTheme) tier.accentColor.copy(alpha = 0.25f) else Color(0x14000000)
            val shadowSpot = if (isDarkTheme) tier.accentColor.copy(alpha = 0.35f) else tier.lightAccentColor.copy(alpha = 0.20f)

            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 4.dp,
                        shape = pillShape,
                        ambientColor = shadowAmbient,
                        spotColor = shadowSpot
                    )
                    .clip(pillShape)
                    .background(capsuleBg)
                    .border(
                        width = 1.dp,
                        color = capsuleBorder,
                        shape = pillShape
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Contextual emoji icon
                    Text(
                        text = tier.icon,
                        fontSize = 12.sp,
                        lineHeight = 12.sp
                    )

                    // Clear multiplier / milestone badge (e.g. "COMBO x5" or "50%")
                    if (tier.badge != null) {
                        Text(
                            text = tier.badge,
                            color = badgeTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp
                        )

                        Text(
                            text = "·",
                            color = dotColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Main crisp praise message
                    Text(
                        text = tier.text,
                        color = mainTextColor,
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
    val accentColor: Color,
    val lightAccentColor: Color
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
        val match = Regex("^([0-9]+%)\\s*(.*)$").find(trimmed)
        if (match != null) {
            badge = match.groupValues[1]
            text = match.groupValues[2].ifEmpty { "Puzzle Complete" }
        } else {
            badge = null
            text = trimmed
        }
    }

    val upperBadge = badge?.uppercase() ?: ""
    val upperText = text.uppercase()

    return when {
        // Milestone 25% (Fresh Emerald Green)
        upperBadge.contains("25%") -> CheerTier(
            icon = "🌱",
            badge = badge,
            text = text,
            accentColor = Color(0xFF34D399),
            lightAccentColor = Color(0xFF059669)
        )

        // Milestone 50% (Luminous Gold / Amber)
        upperBadge.contains("50%") -> CheerTier(
            icon = "👑",
            badge = badge,
            text = text,
            accentColor = Color(0xFFFBBF24),
            lightAccentColor = Color(0xFFD97706)
        )

        // Milestone 70% (Electric Cyan / Sky Blue)
        upperBadge.contains("70%") || upperBadge.contains("75%") -> CheerTier(
            icon = "⚡",
            badge = badge,
            text = text,
            accentColor = Color(0xFF38BDF8),
            lightAccentColor = Color(0xFF0284C7)
        )

        // Milestone 90% (Royal Violet / Magenta)
        upperBadge.contains("90%") || upperBadge.contains("80%") -> CheerTier(
            icon = "🎯",
            badge = badge,
            text = text,
            accentColor = Color(0xFFA855F7),
            lightAccentColor = Color(0xFF7C3AED)
        )

        // Combo Praise: Fire / Heat / Energy (e.g. "On Fire!", "In the Flow!")
        upperText.contains("FIRE") || upperText.contains("FLOW") || upperText.contains("RHYTHM") || upperText.contains("GROOVING") -> CheerTier(
            icon = "🔥",
            badge = null,
            text = text,
            accentColor = Color(0xFFF97316),
            lightAccentColor = Color(0xFFEA580C)
        )

        // Combo Praise: Focus / Precision (e.g. "Laser Focus!", "Arrow Maestro!", "Brilliant!")
        upperText.contains("MAESTRO") || upperText.contains("FOCUS") || upperText.contains("BRILLIANT") || upperText.contains("SHARP") -> CheerTier(
            icon = "⭐",
            badge = null,
            text = text,
            accentColor = Color(0xFF38BDF8),
            lightAccentColor = Color(0xFF0284C7)
        )

        // Combo Praise: Master / Flawless (e.g. "Flawless!", "Untouchable!", "Perfection!", "Legend")
        upperText.contains("FLAWLESS") || upperText.contains("UNTOUCHABLE") || upperText.contains("PERFECTION") || upperText.contains("GALAXY") || upperText.contains("LEGEND") -> CheerTier(
            icon = "💥",
            badge = null,
            text = text,
            accentColor = Color(0xFFA855F7),
            lightAccentColor = Color(0xFF7C3AED)
        )

        // Fallback default
        else -> CheerTier(
            icon = "✨",
            badge = badge,
            text = text,
            accentColor = Color(0xFF38BDF8),
            lightAccentColor = Color(0xFF0284C7)
        )
    }
}
