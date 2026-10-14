package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.ArrowState
import com.arrowescape.puzzle.core.model.Direction
import com.arrowescape.puzzle.core.model.GridCoordinate
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()

    // Zoom & pan states
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPos by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom and pan whenever a new level is loaded or grid dimensions change
    LaunchedEffect(xSize, ySize, arrows.size) {
        scale = 1f
        panOffset = Offset.Zero
    }

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
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(arrows, cellSize, xSize, ySize, scale, panOffset) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startTime = System.currentTimeMillis()
                        val startPos = down.position
                        var isMultiTouch = false
                        var pastSlop = false
                        val touchSlop = viewConfiguration.touchSlop

                        // Calculate board local coordinate from touch position using inverse transform
                        val center = Offset(width / 2f, height / 2f)
                        val localStart = center + (startPos - panOffset - center) / scale
                        val gridX = (localStart.x / cellSize).toInt()
                        val gridY = (localStart.y / cellSize).toInt()
                        val candidateArrow = if (gridX in 0 until xSize && gridY in 0 until ySize) {
                            val coord = GridCoordinate(gridX, gridY)
                            arrows.firstOrNull {
                                it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT && it.isPointInside(coord)
                            }
                        } else null

                        var isLongPressTriggered = false
                        var longPressJob: Job? = null
                        if (candidateArrow != null) {
                            longPressJob = coroutineScope.launch {
                                delay(380L)
                                if (!pastSlop && !isMultiTouch) {
                                    isLongPressTriggered = true
                                    onArrowLongPress(candidateArrow, true)
                                }
                            }
                        }

                        try {
                            var zoomAccum = 1f
                            var panAccum = Offset.Zero

                            do {
                                val event = awaitPointerEvent()
                                val pressedList = event.changes.filter { it.pressed }
                                if (pressedList.size >= 2) {
                                    isMultiTouch = true
                                    longPressJob?.cancel()
                                    if (isLongPressTriggered && candidateArrow != null) {
                                        onArrowLongPress(candidateArrow, false)
                                        isLongPressTriggered = false
                                    }

                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()

                                    if (!pastSlop) {
                                        zoomAccum *= zoomChange
                                        panAccum += panChange
                                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                        val zoomMotion = kotlin.math.abs(1f - zoomAccum) * centroidSize
                                        if (zoomMotion > touchSlop || panAccum.getDistance() > touchSlop) {
                                            pastSlop = true
                                        }
                                    }

                                    if (pastSlop) {
                                        val newScale = (scale * zoomChange).coerceIn(1f, 3.5f)
                                        val maxPanX = (width * (newScale - 1f)) / 2f
                                        val maxPanY = (height * (newScale - 1f)) / 2f

                                        scale = newScale
                                        panOffset = if (newScale > 1.02f) {
                                            Offset(
                                                (panOffset.x + panChange.x).coerceIn(-maxPanX, maxPanX),
                                                (panOffset.y + panChange.y).coerceIn(-maxPanY, maxPanY)
                                            )
                                        } else {
                                            Offset.Zero
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                } else if (pressedList.size == 1) {
                                    val change = pressedList.first()
                                    val dragDist = (change.position - startPos).getDistance()
                                    if (dragDist > touchSlop) {
                                        pastSlop = true
                                        longPressJob?.cancel()
                                        if (isLongPressTriggered && candidateArrow != null) {
                                            onArrowLongPress(candidateArrow, false)
                                            isLongPressTriggered = false
                                        }
                                    }

                                    if (pastSlop && scale > 1.05f) {
                                        val panDelta = change.positionChange()
                                        val maxPanX = (width * (scale - 1f)) / 2f
                                        val maxPanY = (height * (scale - 1f)) / 2f
                                        panOffset = Offset(
                                            (panOffset.x + panDelta.x).coerceIn(-maxPanX, maxPanX),
                                            (panOffset.y + panDelta.y).coerceIn(-maxPanY, maxPanY)
                                        )
                                        change.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        } finally {
                            longPressJob?.cancel()
                            if (isLongPressTriggered && candidateArrow != null) {
                                onArrowLongPress(candidateArrow, false)
                            }
                        }

                        val elapsed = System.currentTimeMillis() - startTime
                        if (!pastSlop && !isMultiTouch && !isLongPressTriggered && elapsed < 450) {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < 320 && (startPos - lastTapPos).getDistance() < touchSlop * 2.5f) {
                                // Double-tap detected -> Reset zoom smoothly
                                if (scale > 1.05f) {
                                    scale = 1f
                                    panOffset = Offset.Zero
                                }
                                lastTapTime = 0L
                            } else {
                                lastTapTime = now
                                lastTapPos = startPos

                                // Spawn tap particle at local board coordinate
                                tapParticles.add(TapParticle(origin = localStart, color = Color(0xFF64748B)))

                                if (candidateArrow != null) {
                                    onArrowClick(candidateArrow)
                                }
                            }
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            withTransform({
                translate(panOffset.x, panOffset.y)
                scale(scale, scale, pivot = center)
            }) {
                // Pure white canvas background
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

        // Floating Reset Zoom pill button shown when zoomed in
        if (scale > 1.05f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clickable {
                        scale = 1f
                        panOffset = Offset.Zero
                    },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xDD1E293B),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "↺",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reset %.1fx".format(scale),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Launch animations for tap particles
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

    // Clamped stroke width ensures consistent, bold arrow lines on any grid density (5x5 up to 37x37)
    val minStroke = 4.dp.toPx()
    val maxStroke = 9.dp.toPx()
    val strokeWidth = (cellSize * 0.18f).coerceIn(minStroke, maxStroke) * (if (arrow.state == ArrowState.HINTED) hintPulseScale else 1.0f)

    // Clamped arrowhead size to maintain sharp, distinct triangular heads
    val minHead = 12.dp.toPx()
    val maxHead = 26.dp.toPx()
    val headSize = (cellSize * 0.48f).coerceIn(minHead, maxHead)

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
