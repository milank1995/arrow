package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val initialXRatio: Float,
    val speed: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float,
    val rotationSpeed: Float,
    val color: Color,
    val widthPx: Float,
    val heightPx: Float,
    val delayRatio: Float
)

@Composable
fun ConfettiBurstEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 28
) {
    val palette = remember {
        listOf(
            Color(0xFFF59E0B), // Gold
            Color(0xFF10B981), // Emerald
            Color(0xFFF43F5E), // Rose
            Color(0xFF8B5CF6), // Violet
            Color(0xFF0EA5E9), // Cyan
            Color(0xFFF97316)  // Amber Orange
        )
    }

    val particles = remember {
        val rand = Random(42)
        List(particleCount) {
            ConfettiParticle(
                initialXRatio = rand.nextFloat(),
                speed = 0.6f + rand.nextFloat() * 0.8f,
                swayAmplitude = 20f + rand.nextFloat() * 35f,
                swayFrequency = 1.5f + rand.nextFloat() * 2.5f,
                rotationSpeed = (rand.nextFloat() * 400f) - 200f,
                color = palette[rand.nextInt(palette.size)],
                widthPx = 8f + rand.nextFloat() * 8f,
                heightPx = 14f + rand.nextFloat() * 12f,
                delayRatio = rand.nextFloat() * 0.8f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confettiLoop")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        for (p in particles) {
            val adjustedProgress = (progress.value + p.delayRatio) % 1.0f
            val y = (adjustedProgress * (canvasHeight + 100f)) - 50f
            val sway = sin(adjustedProgress * Math.PI * 2.0 * p.swayFrequency).toFloat() * p.swayAmplitude
            val x = (p.initialXRatio * canvasWidth) + sway

            val rot = adjustedProgress * p.rotationSpeed

            rotate(rot, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = 0.85f),
                    topLeft = Offset(x - p.widthPx / 2f, y - p.heightPx / 2f),
                    size = Size(p.widthPx, p.heightPx)
                )
            }
        }
    }
}
