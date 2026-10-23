package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.launch

/**
 * Standardized, clean, modern flat button design system for Arrow Decode.
 * Features:
 * - Clean flat geometry with 16dp rounded corners.
 * - Solid theme-accent background with subtle ambient blur elevation (zero bulky 3D bevels).
 * - Ergonomic tactile press scaling with authentic spring rebound.
 * - Precise mathematical centering and clean Title Case typography.
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    theme: ThemePreset,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color = Color.White
) {
    val coroutineScope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1.0f) }
    val bgColor = containerColor ?: theme.arrowDefault

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) bgColor else bgColor.copy(alpha = 0.45f),
        shadowElevation = if (enabled) 2.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(pressScale.value)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                coroutineScope.launch {
                    pressScale.animateTo(0.97f, tween(60))
                    pressScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                onClick()
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = contentColor,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }
    }
}

/**
 * Standardized secondary button for auxiliary actions (Home, Cancel, Replay).
 */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    theme: ThemePreset,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    enabled: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1.0f) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = theme.surface,
        border = BorderStroke(1.2.dp, theme.gridLine),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(pressScale.value)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                coroutineScope.launch {
                    pressScale.animateTo(0.98f, tween(60))
                    pressScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                onClick()
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.textPrimary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }
    }
}
