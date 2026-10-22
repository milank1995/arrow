package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.launch

/**
 * Modern, borderless, responsive booster controls.
 * Features an illuminated, realistic glowing lightbulb with dynamic badge states:
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

    // Subtle breathing pulse for Hint icon when stuck or idle
    val idleTransition = rememberInfiniteTransition(label = "idleHintTransition")
    val idlePulseScale by idleTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isIdlePromptActive) 1.10f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isIdlePromptActive) 800 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idlePulse"
    )

    // Gentle ambient aura breathing for the illuminated bulb
    val auraGlow by idleTransition.animateFloat(
        initialValue = if (isIdlePromptActive) 0.55f else 0.40f,
        targetValue = if (isIdlePromptActive) 0.95f else 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isIdlePromptActive) 800 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraGlow"
    )

    val amberBulbColor = Color(0xFFD97706)

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
            // 1. ENHANCED ILLUMINATED HINT ICON (Glowing, layered, realistic glass & filament)
            val combinedHintScale = hintPressScale.value * idlePulseScale

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 68.dp, height = 76.dp)
                    .scale(combinedHintScale)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        coroutineScope.launch {
                            hintPressScale.animateTo(0.85f, tween(60))
                            hintPressScale.animateTo(1.12f, tween(90))
                            hintPressScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        onHintClick()
                    }
            ) {
                // Layer 1 & 2: Realistic Glowing Light Bulb (Aura, Glass Dome, Reflection, Filament, Screw Base)
                RealisticLightbulbArt(
                    auraGlow = auraGlow,
                    isHighlighted = isIdlePromptActive,
                    modifier = Modifier
                        .size(width = 54.dp, height = 62.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-2).dp)
                )

                // Layer 3: Jewel Badge Docked at Bottom (∞, Count, or Video Icon)
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = when {
                        isFreeHintLevel -> if (theme.isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)
                        hintsCount > 0 -> theme.surface
                        else -> Color(0xFFD97706) // Vibrant amber for video ad CTA
                    },
                    border = BorderStroke(
                        width = 1.2.dp,
                        color = when {
                            isFreeHintLevel -> if (theme.isDark) Color(0xFF78350F) else Color(0xFFFDE68A)
                            hintsCount > 0 -> theme.gridLine
                            else -> Color(0xFFFBBF24)
                        }
                    ),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .widthIn(min = 28.dp)
                            .padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            // State A: Unlimited Hints -> Professional Infinity Symbol (∞)
                            isFreeHintLevel -> {
                                Text(
                                    text = "∞",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (theme.isDark) Color(0xFFFDE047) else Color(0xFFB45309),
                                    lineHeight = 15.sp,
                                    modifier = Modifier.offset(y = (-1).dp)
                                )
                            }
                            // State B: Limited Hints -> 1, 2, 3
                            hintsCount > 0 -> {
                                Text(
                                    text = "$hintsCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = theme.textPrimary
                                )
                            }
                            // State C: Exhausted Hints -> Sleek Video Camera Icon for Ad Reward
                            else -> {
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
                    .size(width = 64.dp, height = 72.dp)
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
                        .size(width = 64.dp, height = 72.dp)
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
 * Custom high-fidelity lightbulb art rendered directly with Canvas vector math.
 * Features:
 * - Radiant ambient aura with smooth radial falloff
 * - Pear-shaped glass envelope with warm amber-golden gradient
 * - Realistic curved specular glass reflections giving 3D depth
 * - Tungsten filament bridge with incandescent luminescence
 * - Metallic ribbed screw base and bottom contact point
 */
@Composable
private fun RealisticLightbulbArt(
    auraGlow: Float,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w * 0.5f
        val domeCenterY = h * 0.36f
        val domeRadius = w * 0.33f

        // 1. Ambient Radiant Aura (Radial Glow Falloff)
        val auraRadius = w * if (isHighlighted) 0.58f else 0.50f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFEF08A).copy(alpha = (auraGlow * 0.75f).coerceIn(0f, 1f)),
                    Color(0xFFF59E0B).copy(alpha = (auraGlow * 0.35f).coerceIn(0f, 1f)),
                    Color(0xFFD97706).copy(alpha = (auraGlow * 0.12f).coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                center = Offset(centerX, domeCenterY),
                radius = auraRadius
            ),
            radius = auraRadius,
            center = Offset(centerX, domeCenterY)
        )

        // 2. Pear-shaped Glass Bulb Body
        val bulbPath = Path().apply {
            // Start at left neck junction
            moveTo(w * 0.365f, h * 0.68f)
            // Left curve outwards to dome equator
            cubicTo(
                w * 0.27f, h * 0.58f,
                w * 0.17f, h * 0.46f,
                w * 0.17f, domeCenterY
            )
            // Top circular dome
            cubicTo(
                w * 0.17f, domeCenterY - domeRadius * 1.05f,
                w * 0.83f, domeCenterY - domeRadius * 1.05f,
                w * 0.83f, domeCenterY
            )
            // Right curve inwards towards neck
            cubicTo(
                w * 0.83f, h * 0.46f,
                w * 0.73f, h * 0.58f,
                w * 0.635f, h * 0.68f
            )
            close()
        }

        // Fill glass envelope with radiant warm incandescent gradient
        drawPath(
            path = bulbPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFBEB), // Bright white-hot center
                    Color(0xFFFEF08A), // Warm yellow brilliance
                    Color(0xFFF59E0B), // Radiant amber body
                    Color(0xFFD97706)  // Rich golden amber perimeter
                ),
                center = Offset(centerX - w * 0.04f, domeCenterY - h * 0.03f),
                radius = domeRadius * 1.25f
            )
        )

        // Glass boundary stroke for crisp definition
        drawPath(
            path = bulbPath,
            color = Color(0xFFB45309).copy(alpha = 0.80f),
            style = Stroke(
                width = 1.3.dp.toPx(),
                join = StrokeJoin.Round
            )
        )

        // 3. Specular 3D Curved Glass Highlight (Glossy reflection)
        val specPath = Path().apply {
            moveTo(w * 0.26f, domeCenterY + h * 0.04f)
            cubicTo(
                w * 0.25f, domeCenterY - h * 0.08f,
                w * 0.34f, domeCenterY - domeRadius * 0.75f,
                w * 0.44f, domeCenterY - domeRadius * 0.82f
            )
        }
        drawPath(
            path = specPath,
            color = Color.White.copy(alpha = 0.75f),
            style = Stroke(
                width = 2.0.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
        // Secondary subtle specular gleam dot
        drawCircle(
            color = Color.White.copy(alpha = 0.60f),
            radius = 1.2.dp.toPx(),
            center = Offset(w * 0.27f, domeCenterY + h * 0.09f)
        )

        // 4. Tungsten Filament (Twin stems & incandescent loop)
        val stemColor = Color(0xFFB45309).copy(alpha = 0.75f)
        val stemWidth = 1.1.dp.toPx()
        // Left stem
        drawLine(
            color = stemColor,
            start = Offset(w * 0.44f, h * 0.66f),
            end = Offset(w * 0.44f, domeCenterY + h * 0.06f),
            strokeWidth = stemWidth,
            cap = StrokeCap.Round
        )
        // Right stem
        drawLine(
            color = stemColor,
            start = Offset(w * 0.56f, h * 0.66f),
            end = Offset(w * 0.56f, domeCenterY + h * 0.06f),
            strokeWidth = stemWidth,
            cap = StrokeCap.Round
        )

        // Filament looped coil
        val filamentPath = Path().apply {
            moveTo(w * 0.44f, domeCenterY + h * 0.06f)
            cubicTo(
                w * 0.44f, domeCenterY - h * 0.07f,
                w * 0.56f, domeCenterY - h * 0.07f,
                w * 0.56f, domeCenterY + h * 0.06f
            )
        }
        // Filament outer halo glow
        drawPath(
            path = filamentPath,
            color = Color(0xFFFBBF24).copy(alpha = 0.60f),
            style = Stroke(
                width = 3.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
        // Filament white-hot inner wire
        drawPath(
            path = filamentPath,
            color = Color(0xFFFFFBEB),
            style = Stroke(
                width = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // 5. Metallic Ribbed Screw Base (Connecting neck to contact)
        val metalBrush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF94A3B8),
                Color(0xFFF1F5F9),
                Color(0xFF64748B)
            ),
            startX = w * 0.35f,
            endX = w * 0.65f
        )
        val strokeBase = Stroke(width = 0.8.dp.toPx())

        // Thread 1
        val thread1Rect = RoundRect(
            left = w * 0.38f,
            top = h * 0.68f,
            right = w * 0.62f,
            bottom = h * 0.74f,
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            brush = metalBrush,
            topLeft = Offset(thread1Rect.left, thread1Rect.top),
            size = Size(thread1Rect.width, thread1Rect.height),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF475569),
            topLeft = Offset(thread1Rect.left, thread1Rect.top),
            size = Size(thread1Rect.width, thread1Rect.height),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = strokeBase
        )

        // Thread 2 (slightly narrower)
        val thread2Rect = RoundRect(
            left = w * 0.40f,
            top = h * 0.745f,
            right = w * 0.60f,
            bottom = h * 0.80f,
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            brush = metalBrush,
            topLeft = Offset(thread2Rect.left, thread2Rect.top),
            size = Size(thread2Rect.width, thread2Rect.height),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF475569),
            topLeft = Offset(thread2Rect.left, thread2Rect.top),
            size = Size(thread2Rect.width, thread2Rect.height),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = strokeBase
        )

        // Thread 3 / Bottom Contact Point
        val contactPath = Path().apply {
            moveTo(w * 0.42f, h * 0.805f)
            lineTo(w * 0.58f, h * 0.805f)
            quadraticTo(w * 0.50f, h * 0.855f, w * 0.42f, h * 0.805f)
            close()
        }
        drawPath(
            path = contactPath,
            color = Color(0xFF334155),
            style = Fill
        )
    }
}

