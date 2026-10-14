package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.ArrowState
import com.arrowescape.puzzle.core.model.Direction
import com.arrowescape.puzzle.core.model.GridCoordinate
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class TapParticle(
    val origin: Offset,
    val progress: Animatable<Float, *> = Animatable(0f),
    val color: Color
)

@Composable
fun PuzzleBoardCanvas(
    xSize: Int,
    ySize: Int,
    arrows: List<Arrow>,
    theme: ThemePreset,
    isGridLinesActive: Boolean,
    previewRayArrowId: String?,
    onArrowClick: (Arrow) -> Unit,
    onArrowLongPress: (Arrow, isPressed: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Hint pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val hintPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintPulse"
    )

    // Tap ripple particles list
    val tapParticles = remember { mutableStateListOf<TapParticle>() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .aspectRatio(xSize.toFloat() / ySize.toFloat())
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val cellSize = minOf(width / xSize, height / ySize)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(xSize.toFloat() / ySize.toFloat())
                .pointerInput(arrows, cellSize) {
                    detectTapGestures(
                        onPress = { offset ->
                            val clickedCoord = GridCoordinate(
                                x = (offset.x / cellSize).toInt().coerceIn(0, xSize - 1),
                                y = (offset.y / cellSize).toInt().coerceIn(0, ySize - 1)
                            )
                            val tappedArrow = arrows.firstOrNull { 
                                it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT && it.isPointInside(clickedCoord) 
                            }
                            if (tappedArrow != null) {
                                onArrowLongPress(tappedArrow, true)
                                tryAwaitRelease()
                                onArrowLongPress(tappedArrow, false)
                            }
                        },
                        onTap = { offset ->
                            // Spawn tap ripple particle effect
                            val particle = TapParticle(origin = offset, color = Color(0xFF64748B))
                            tapParticles.add(particle)

                            val clickedCoord = GridCoordinate(
                                x = (offset.x / cellSize).toInt().coerceIn(0, xSize - 1),
                                y = (offset.y / cellSize).toInt().coerceIn(0, ySize - 1)
                            )
                            val tappedArrow = arrows.firstOrNull { 
                                it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT && it.isPointInside(clickedCoord) 
                            }
                            if (tappedArrow != null) {
                                onArrowClick(tappedArrow)
                            }
                        }
                    )
                }
        ) {
            // Draw pure white background
            drawRect(color = Color.White)

            // 1. Grid Lines / Dots only if user activated via '#' Booster button
            if (isGridLinesActive) {
                drawGridLines(xSize, ySize, cellSize, theme.gridLine)
                drawGridDots(xSize, ySize, cellSize, theme.gridDot)
            }

            // 2. Draw Preview Ray if an arrow is long-pressed
            val previewArrow = arrows.firstOrNull { it.id == previewRayArrowId }
            if (previewArrow != null) {
                drawPreviewRay(previewArrow, xSize, ySize, cellSize, theme.arrowHint)
            }

            // 3. Draw All Arrows (Static, flying out, or wrong-move bouncing)
            for (arrow in arrows) {
                if (arrow.state == ArrowState.CLEARED) continue

                drawArrowEntity(
                    arrow = arrow,
                    xSize = xSize,
                    ySize = ySize,
                    cellSize = cellSize,
                    theme = theme,
                    hintPulseScale = hintPulseScale
                )
            }

            // 4. Draw Tap Particles
            drawTapParticles(tapParticles)
        }
    }

    // Launch animations for particles
    tapParticles.forEach { particle ->
        LaunchedEffect(particle) {
            particle.progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
            tapParticles.remove(particle)
        }
    }
}

private fun DrawScope.drawArrowEntity(
    arrow: Arrow,
    xSize: Int,
    ySize: Int,
    cellSize: Float,
    theme: ThemePreset,
    hintPulseScale: Float
) {
    val baseColor = when (arrow.state) {
        ArrowState.HINTED -> theme.arrowHint
        ArrowState.WRONG_FLASH -> theme.arrowWrong
        else -> theme.arrowDefault
    }

    val strokeWidth = cellSize * 0.16f * (if (arrow.state == ArrowState.HINTED) hintPulseScale else 1.0f)
    val headSize = cellSize * 0.44f

    // Build the full motion track: body points + escape extension along arrow.direction
    val track = mutableListOf<Offset>()
    for (coord in arrow.body) {
        track.add(Offset((coord.x + 0.5f) * cellSize, (coord.y + 0.5f) * cellSize))
    }
    val headPt = track.last()
    val extSteps = maxOf(xSize, ySize) + arrow.body.size + 4
    for (s in 1..extSteps) {
        track.add(
            Offset(
                headPt.x + arrow.direction.dx * s * cellSize,
                headPt.y + arrow.direction.dy * s * cellSize
            )
        )
    }

    val originalBodyLen = (arrow.body.size - 1).coerceAtLeast(0)
    val maxDisplacement = (maxOf(xSize, ySize) + arrow.body.size + 2).toFloat()

    // Compute displacement in cell units based on state and flightProgress
    val displacement = when (arrow.state) {
        ArrowState.FLYING_OUT -> arrow.flightProgress * maxDisplacement
        ArrowState.WRONG_FLASH -> {
            // Spring bounce forward and back into obstacle
            sin(arrow.flightProgress * PI.toFloat()) * 0.22f
        }
        else -> 0f
    }

    // Smooth alpha fadeout as arrow clears the board perimeter
    val alpha = if (arrow.state == ArrowState.FLYING_OUT && arrow.flightProgress > 0.85f) {
        ((1f - arrow.flightProgress) / 0.15f).coerceIn(0f, 1f)
    } else {
        1.0f
    }

    // Sample the moving polyline segment along the track
    val sampledPoints = sampleTrackSegment(
        track = track,
        startDist = displacement,
        endDist = originalBodyLen + displacement
    )

    if (sampledPoints.isEmpty()) return

    val renderColor = baseColor.copy(alpha = alpha)

    // 1. Draw body stroke along the moving polyline
    if (sampledPoints.size > 1) {
        val path = Path().apply {
            moveTo(sampledPoints[0].x, sampledPoints[0].y)
            for (i in 1 until sampledPoints.size) {
                lineTo(sampledPoints[i].x, sampledPoints[i].y)
            }
        }
        drawPath(
            path = path,
            color = renderColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    // 2. Draw sharp triangular arrowhead at the moving head point
    val currentHead = sampledPoints.last()
    rotate(
        degrees = arrow.direction.angleDegrees,
        pivot = currentHead
    ) {
        val arrowHeadPath = Path().apply {
            moveTo(currentHead.x, currentHead.y - headSize * 0.55f) // Forward tip
            lineTo(currentHead.x + headSize * 0.40f, currentHead.y + headSize * 0.32f) // Right base
            lineTo(currentHead.x - headSize * 0.40f, currentHead.y + headSize * 0.32f) // Left base
            close()
        }

        drawPath(
            path = arrowHeadPath,
            color = renderColor
        )
    }
}

/**
 * Samples a continuous polyline slice from a polyline track between startDist and endDist (in vertex units).
 * Enables the arrow to bend smoothly around corners like a train on tracks during flight.
 */
private fun sampleTrackSegment(
    track: List<Offset>,
    startDist: Float,
    endDist: Float
): List<Offset> {
    if (track.isEmpty() || startDist >= track.size - 1) return emptyList()

    val pts = mutableListOf<Offset>()

    // Start point interpolation
    val sIdx = startDist.toInt().coerceIn(0, track.size - 1)
    val sFrac = (startDist - sIdx).coerceIn(0f, 1f)
    val p0 = track[sIdx]
    val p1 = track.getOrElse(sIdx + 1) { p0 }
    pts.add(lerpOffset(p0, p1, sFrac))

    // Intermediate whole track points
    val eIdx = minOf(endDist.toInt(), track.size - 2)
    for (i in (sIdx + 1)..eIdx) {
        pts.add(track[i])
    }

    // End point (head) interpolation
    if (endDist > eIdx && eIdx < track.size - 1) {
        val eFrac = (endDist - eIdx).coerceIn(0f, 1f)
        val q0 = track[eIdx]
        val q1 = track.getOrElse(eIdx + 1) { q0 }
        pts.add(lerpOffset(q0, q1, eFrac))
    }

    return pts
}

private fun lerpOffset(p0: Offset, p1: Offset, t: Float): Offset {
    return Offset(
        x = p0.x + (p1.x - p0.x) * t,
        y = p0.y + (p1.y - p0.y) * t
    )
}

private fun DrawScope.drawTapParticles(particles: List<TapParticle>) {
    for (p in particles) {
        val prog = p.progress.value
        val alpha = (1f - prog).coerceIn(0f, 1f)
        if (alpha <= 0f) continue

        // Expanding soft ring
        val radius = 10f + prog * 45f
        drawCircle(
            color = p.color.copy(alpha = alpha * 0.6f),
            radius = radius,
            center = p.origin,
            style = Stroke(width = 2.5f)
        )

        // 4 sparkling dots traveling outward
        val sparkDist = 12f + prog * 38f
        val angles = listOf(45.0, 135.0, 225.0, 315.0)
        for (deg in angles) {
            val rad = deg * PI / 180.0
            val sparkCenter = Offset(
                x = (p.origin.x + sparkDist * cos(rad)).toFloat(),
                y = (p.origin.y + sparkDist * sin(rad)).toFloat()
            )
            drawCircle(
                color = p.color.copy(alpha = alpha * 0.8f),
                radius = 3f * (1f - prog * 0.5f),
                center = sparkCenter
            )
        }
    }
}

private fun DrawScope.drawGridLines(xSize: Int, ySize: Int, cellSize: Float, color: Color) {
    for (x in 0..xSize) {
        drawLine(
            color = color,
            start = Offset(x * cellSize, 0f),
            end = Offset(x * cellSize, ySize * cellSize),
            strokeWidth = 1.2f
        )
    }
    for (y in 0..ySize) {
        drawLine(
            color = color,
            start = Offset(0f, y * cellSize),
            end = Offset(xSize * cellSize, y * cellSize),
            strokeWidth = 1.2f
        )
    }
}

private fun DrawScope.drawGridDots(xSize: Int, ySize: Int, cellSize: Float, color: Color) {
    for (x in 0..xSize) {
        for (y in 0..ySize) {
            drawCircle(
                color = color,
                radius = 3.0f,
                center = Offset(x * cellSize, y * cellSize)
            )
        }
    }
}

private fun DrawScope.drawPreviewRay(
    arrow: Arrow,
    xSize: Int,
    ySize: Int,
    cellSize: Float,
    color: Color
) {
    val headCenter = Offset(
        x = (arrow.head.x + 0.5f) * cellSize,
        y = (arrow.head.y + 0.5f) * cellSize
    )

    var endOffset = headCenter
    when (arrow.direction) {
        Direction.UP -> endOffset = Offset(headCenter.x, -cellSize)
        Direction.DOWN -> endOffset = Offset(headCenter.x, (ySize + 1) * cellSize)
        Direction.LEFT -> endOffset = Offset(-cellSize, headCenter.y)
        Direction.RIGHT -> endOffset = Offset((xSize + 1) * cellSize, headCenter.y)
    }

    drawLine(
        color = color.copy(alpha = 0.5f),
        start = headCenter,
        end = endOffset,
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
}
