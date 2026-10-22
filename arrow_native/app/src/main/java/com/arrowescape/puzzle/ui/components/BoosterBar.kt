package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.launch

/**
 * Modern, borderless, responsive booster controls.
 * Features an illuminated, modern lightbulb with distinct visual states:
 * - Inactive state: Clean white bulb with well-defined dark outline (minimalist, zen).
 * - Active state: Radiant golden yellow bulb with incandescent lighting & breathing glow aura.
 * Dynamic docked badge:
 * - Unlimited Hints (Levels 1-10): Sleek infinity symbol (∞)
 * - Limited Hints (Level 11+ balance): Numeric counter (1, 2, 3)
 * - Ad-Unlock State (0 balance): Professional video camera icon
 */
@Composable
fun BoosterBar(
    hintsCount: Int,
    isGridLinesActive: Boolean,
    isFreeHintLevel: Boolean = false,
    isIdlePromptActive: Boolean = false,
    theme: ThemePreset,
    onHintClick: () -> Unit,
    onGridClick: () -> Unit,
    onRestartClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val hintPressScale = remember { Animatable(1f) }
    val gridPressScale = remember { Animatable(1f) }

    // Smooth animation transition between Inactive (0f) and Active (1f)
    val activeProgress by animateFloatAsState(
        targetValue = if (isIdlePromptActive) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "bulbActiveProgress"
    )

    // Subtle breathing pulse for Hint icon when highlighted / active
    val idleTransition = rememberInfiniteTransition(label = "idleHintTransition")
    val idlePulseScale by idleTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isIdlePromptActive) 1.09f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idlePulse"
    )

    // Radiant aura breathing for the active illuminated bulb
    val auraGlow by idleTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraGlow"
    )

    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(if (onRestartClick != null) 36.dp else 52.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. REFINED MODERN HINT ICON (Inactive White vs Active Glowing Yellow)
            val combinedHintScale = hintPressScale.value * idlePulseScale

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 64.dp, height = 70.dp)
                    .scale(combinedHintScale)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        coroutineScope.launch {
                            hintPressScale.animateTo(0.85f, tween(60))
                            hintPressScale.animateTo(1.10f, tween(90))
                            hintPressScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        onHintClick()
                    }
            ) {
                // Layer 1: Refined Modern Lightbulb Art (Centered for starting levels, slightly raised when badge is present)
                ModernLightbulbArt(
                    activeProgress = activeProgress,
                    auraGlow = auraGlow,
                    theme = theme,
                    modifier = Modifier
                        .size(width = 46.dp, height = 52.dp)
                        .align(Alignment.Center)
                        .offset(y = if (isFreeHintLevel) 0.dp else (-6).dp)
                )

                // Layer 2: Badge (ONLY shown when NOT on free hint starting levels)
                if (!isFreeHintLevel) {
                    val badgeBgColor = if (hintsCount > 0) theme.surface else Color(0xFFD97706)
                    val badgeBorderColor = if (hintsCount > 0) theme.gridLine else Color(0xFFFBBF24)

                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = badgeBgColor,
                        border = BorderStroke(1.2.dp, badgeBorderColor),
                        shadowElevation = if (hintsCount > 0) 1.dp else 3.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-4).dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(19.dp)
                                .widthIn(min = 28.dp)
                                .padding(horizontal = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hintsCount > 0) {
                                // Limited Hints: 1, 2, or 3
                                Text(
                                    text = "$hintsCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = theme.textPrimary
                                )
                            } else {
                                // Exhausted Hints: Watch Video Ad Icon
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Watch Video Ad for 3 Hints",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. GRID GUIDE ICON (Clean, borderless, floating with miniature eye badge)
            val gridColor = if (isGridLinesActive) theme.arrowDefault else theme.textSecondary.copy(alpha = 0.75f)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 64.dp, height = 70.dp)
                    .scale(gridPressScale.value)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        coroutineScope.launch {
                            gridPressScale.animateTo(0.85f, tween(60))
                            gridPressScale.animateTo(1.10f, tween(90))
                            gridPressScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        onGridClick()
                    }
            ) {
                // Crisp Grid icon
                Canvas(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                ) {
                    val strokeW = 2.6.dp.toPx()
                    val pad = 3.dp.toPx()
                    val w = size.width
                    val h = size.height

                    // 2 horizontal grid bars
                    drawLine(
                        color = gridColor,
                        start = Offset(pad, h * 0.36f),
                        end = Offset(w - pad, h * 0.36f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(pad, h * 0.64f),
                        end = Offset(w - pad, h * 0.64f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    // 2 vertical grid bars
                    drawLine(
                        color = gridColor,
                        start = Offset(w * 0.36f, pad),
                        end = Offset(w * 0.36f, h - pad),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(w * 0.64f, pad),
                        end = Offset(w * 0.64f, h - pad),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }

                // Miniature Eye Badge
                Surface(
                    shape = CircleShape,
                    color = if (isGridLinesActive) theme.arrowDefault else theme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isGridLinesActive) theme.arrowDefault else theme.gridLine
                    ),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Grid Visibility",
                            tint = if (isGridLinesActive) {
                                if (theme.isDark) Color.Black else Color.White
                            } else theme.textSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // 3. RESTART ICON (Shown in test mode)
            if (onRestartClick != null) {
                val restartPressScale = remember { Animatable(1f) }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 64.dp, height = 70.dp)
                        .scale(restartPressScale.value)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                restartPressScale.animateTo(0.85f, tween(60))
                                restartPressScale.animateTo(1.10f, tween(90))
                                restartPressScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                            onRestartClick()
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart Level",
                        tint = theme.textPrimary,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}

/**
 * Modern, polished lightbulb art rendered directly via Canvas vector geometry:
 * - Inactive state (activeProgress = 0):
 *   Clean white glass fill with well-defined dark outline, matching the arrows & grid style.
 * - Active state (activeProgress = 1):
 *   Vibrant warm yellow with incandescent filament glow and radiant aura breathing.
 */
@Composable
private fun ModernLightbulbArt(
    activeProgress: Float,
    auraGlow: Float,
    theme: ThemePreset,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w * 0.5f
        val domeCenterY = h * 0.35f
        val domeRadius = w * 0.34f

        // 1. Ambient Radiant Aura (Only visible when active / highlighted)
        if (activeProgress > 0.01f) {
            val auraRadius = w * 0.52f
            val auraAlpha = (activeProgress * auraGlow).coerceIn(0f, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFEF08A).copy(alpha = auraAlpha * 0.80f),
                        Color(0xFFF59E0B).copy(alpha = auraAlpha * 0.35f),
                        Color(0xFFD97706).copy(alpha = auraAlpha * 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, domeCenterY),
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = Offset(centerX, domeCenterY)
            )
        }

        // 2. Modern Glass Envelope Geometry (Circular dome with elegant conical neck taper)
        val bulbPath = Path().apply {
            // Start at left neck junction
            moveTo(w * 0.36f, h * 0.70f)
            // Left taper outwards to dome equator
            cubicTo(
                w * 0.32f, h * 0.62f,
                w * 0.16f, h * 0.48f,
                w * 0.16f, domeCenterY
            )
            // Top circular dome
            cubicTo(
                w * 0.16f, domeCenterY - domeRadius * 1.02f,
                w * 0.84f, domeCenterY - domeRadius * 1.02f,
                w * 0.84f, domeCenterY
            )
            // Right taper inwards to neck junction
            cubicTo(
                w * 0.84f, h * 0.48f,
                w * 0.68f, h * 0.62f,
                w * 0.64f, h * 0.70f
            )
            close()
        }

        // Inactive vs Active Glass Fill
        val inactiveFill = if (theme.isDark) Color(0xFF1E293B) else Color.White

        // Draw Inactive base fill
        drawPath(
            path = bulbPath,
            color = inactiveFill,
            style = Fill
        )

        // If transitioning or active, overlay radiant golden gradient
        if (activeProgress > 0.01f) {
            drawPath(
                path = bulbPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFBEB).copy(alpha = activeProgress),
                        Color(0xFFFEF08A).copy(alpha = activeProgress),
                        Color(0xFFF59E0B).copy(alpha = activeProgress),
                        Color(0xFFD97706).copy(alpha = activeProgress)
                    ),
                    center = Offset(centerX - w * 0.03f, domeCenterY - h * 0.02f),
                    radius = domeRadius * 1.25f
                )
            )
        }

        // Well-defined glass outline stroke
        val inactiveStrokeColor = if (theme.isDark) theme.textPrimary.copy(alpha = 0.9f) else Color(0xFF334155)
        val activeStrokeColor = Color(0xFFB45309)
        val currentStrokeColor = lerp(inactiveStrokeColor, activeStrokeColor, activeProgress)

        drawPath(
            path = bulbPath,
            color = currentStrokeColor,
            style = Stroke(
                width = 1.8.dp.toPx(),
                join = StrokeJoin.Round
            )
        )

        // 3. Tungsten Filament Wire
        val filamentPath = Path().apply {
            // Left lead
            moveTo(w * 0.42f, h * 0.68f)
            lineTo(w * 0.42f, domeCenterY + h * 0.06f)
            // Center loop
            cubicTo(
                w * 0.42f, domeCenterY - h * 0.06f,
                w * 0.58f, domeCenterY - h * 0.06f,
                w * 0.58f, domeCenterY + h * 0.06f
            )
            // Right lead
            lineTo(w * 0.58f, h * 0.68f)
        }

        // Inactive filament: clean subtle wire
        val inactiveFilamentColor = if (theme.isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
        val activeFilamentHalo = Color(0xFFFBBF24).copy(alpha = activeProgress * 0.75f)
        val activeFilamentCore = lerp(inactiveFilamentColor, Color(0xFFFFFBEB), activeProgress)

        // Glow halo around filament when active
        if (activeProgress > 0.01f) {
            drawPath(
                path = filamentPath,
                color = activeFilamentHalo,
                style = Stroke(
                    width = 3.0.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Crisp inner wire
        drawPath(
            path = filamentPath,
            color = activeFilamentCore,
            style = Stroke(
                width = 1.3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 5. Metallic Ribbed Screw Base (Connecting neck to contact foot)
        val inactiveMetalBase = if (theme.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
        val activeMetalBase = Color(0xFFFEF08A)
        val currentMetalColor = lerp(inactiveMetalBase, activeMetalBase, activeProgress)

        val strokeBase = Stroke(width = 1.0.dp.toPx())

        // Thread 1
        val thread1Rect = RoundRect(
            left = w * 0.38f,
            top = h * 0.70f,
            right = w * 0.62f,
            bottom = h * 0.755f,
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = currentMetalColor,
            topLeft = Offset(thread1Rect.left, thread1Rect.top),
            size = Size(thread1Rect.width, thread1Rect.height),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = currentStrokeColor,
            topLeft = Offset(thread1Rect.left, thread1Rect.top),
            size = Size(thread1Rect.width, thread1Rect.height),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = strokeBase
        )

        // Thread 2 (slightly narrower)
        val thread2Rect = RoundRect(
            left = w * 0.40f,
            top = h * 0.76f,
            right = w * 0.60f,
            bottom = h * 0.81f,
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = currentMetalColor,
            topLeft = Offset(thread2Rect.left, thread2Rect.top),
            size = Size(thread2Rect.width, thread2Rect.height),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = currentStrokeColor,
            topLeft = Offset(thread2Rect.left, thread2Rect.top),
            size = Size(thread2Rect.width, thread2Rect.height),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = strokeBase
        )

        // Bottom Contact Terminal Point
        val contactPath = Path().apply {
            moveTo(w * 0.43f, h * 0.815f)
            lineTo(w * 0.57f, h * 0.815f)
            quadraticTo(w * 0.50f, h * 0.865f, w * 0.43f, h * 0.815f)
            close()
        }
        val contactColor = if (theme.isDark) Color(0xFF0F172A) else Color(0xFF475569)
        drawPath(
            path = contactPath,
            color = contactColor,
            style = Fill
        )
    }
}

