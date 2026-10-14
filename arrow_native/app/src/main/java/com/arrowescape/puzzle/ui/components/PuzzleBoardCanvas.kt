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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private data class TapParticle(
    val origin: Offset,
    val progress: Animatable<Float, *> = Animatable(0f),
    val color: Color
)

private data class VelSample(
    val delta: Offset,
    val timeMs: Long
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

    // Smooth scale and pan states
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPos by remember { mutableStateOf(Offset.Zero) }

    // Remember the initial level silhouette cells to render subtle cell dots for cleared areas
    val initialLevelCells = remember(xSize, ySize, arrows.size) {
        arrows.flatMap { it.body }.toSet()
    }

    // Reset zoom and pan whenever a new level is loaded or grid dimensions change
    LaunchedEffect(xSize, ySize, arrows.size) {
        settleJob?.cancel()
        scale = 1f
        panX = 0f
        panY = 0f
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

    // Smooth animated preview beam alpha matching Unity's CanvasGroup fade (lineCg.DOFade)
    val previewAlpha by animateFloatAsState(
        targetValue = if (previewRayArrowId != null) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "previewAlpha"
    )
    var lastPreviewArrow by remember { mutableStateOf<Arrow?>(null) }
    val activePreviewArrow = arrows.firstOrNull { it.id == previewRayArrowId }
    if (activePreviewArrow != null) {
        lastPreviewArrow = activePreviewArrow
    }
    val previewArrowToRender = if (previewAlpha > 0.01f) (activePreviewArrow ?: lastPreviewArrow) else null

    // Viewport-wide canvas: adapts flexibly to full available gameplay height and width
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val viewportWidth = constraints.maxWidth.toFloat()
        val viewportHeight = constraints.maxHeight.toFloat()

        // Fit board into viewport with comfortable margin (92% width, 90% height)
        val availableWidth = viewportWidth * 0.92f
        val availableHeight = viewportHeight * 0.90f
        val cellSize = minOf(availableWidth / xSize, availableHeight / ySize)
        val boardWidth = cellSize * xSize
        val boardHeight = cellSize * ySize
        val boardOrigin = Offset(
            (viewportWidth - boardWidth) / 2f,
            (viewportHeight - boardHeight) / 2f
        )

        // Dynamic zoom limits matching Unity's ComputeZoomLimitsForLevel (minCellScreenPx / maxCellScreenPx)
        val minScale = 0.75f
        val targetMaxCellPx = maxOf(viewportWidth * 0.50f, 450f)
        val maxScale = (targetMaxCellPx / cellSize).coerceIn(5.0f, 18.0f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds() // Prevents drawing over top bar or bottom booster bar
                .pointerInput(arrows, cellSize, xSize, ySize, maxScale, boardOrigin) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        settleJob?.cancel() // Stop ongoing inertia or animation immediately on new touch

                        val startTime = System.currentTimeMillis()
                        val startPos = down.position
                        var lastPos = down.position
                        var isMultiTouch = false
                        var isDragging = false
                        val dragThreshold = 18.dp.toPx() // Intentional drag threshold (avoids dropping taps on micro-jitter)

                        val center = Offset(viewportWidth / 2f, viewportHeight / 2f)

                        // Locate candidate arrow under touch using high-precision geometric distance test
                        val localDown = center + (startPos - Offset(panX, panY) - center) / scale
                        val candidateArrow = findClosestArrow(localDown, boardOrigin, arrows, cellSize, xSize, ySize)

                        var isLongPressTriggered = false
                        var longPressJob: Job? = null
                        if (candidateArrow != null) {
                            longPressJob = coroutineScope.launch {
                                delay(380L)
                                if (!isDragging && !isMultiTouch) {
                                    isLongPressTriggered = true
                                    onArrowLongPress(candidateArrow, true)
                                }
                            }
                        }

                        // Velocity tracking buffer for inertia coasting
                        val velBuf = ArrayDeque<VelSample>(6)
                        var lastMoveTime = System.currentTimeMillis()

                        try {
                            var zoomAccum = 1f
                            var panAccum = Offset.Zero

                            do {
                                val event = awaitPointerEvent()
                                val now = System.currentTimeMillis()
                                val pressedList = event.changes.filter { it.pressed }

                                if (pressedList.size >= 2) {
                                    isMultiTouch = true
                                    longPressJob?.cancel()
                                    if (isLongPressTriggered && candidateArrow != null) {
                                        onArrowLongPress(candidateArrow, false)
                                        isLongPressTriggered = false
                                    }

                                    val centroid = event.calculateCentroid()
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()

                                    if (!isDragging) {
                                        zoomAccum *= zoomChange
                                        panAccum += panChange
                                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                        val zoomMotion = kotlin.math.abs(1f - zoomAccum) * centroidSize
                                        if (zoomMotion > dragThreshold || panAccum.getDistance() > dragThreshold) {
                                            isDragging = true
                                        }
                                    }

                                    if (isDragging) {
                                        // Pinch sensitivity multiplier for responsive zoom feel
                                        val effectiveZoom = 1f + (zoomChange - 1f) * 1.15f
                                        val newScale = (scale * effectiveZoom).coerceIn(minScale, maxScale)

                                        // Keep the world point under the pinch centroid stationary
                                        val oldScale = scale
                                        val currentPan = Offset(panX, panY)
                                        val newPan = if (oldScale > 0.001f) {
                                            Offset(
                                                centroid.x - center.x - (centroid.x - currentPan.x - center.x) * (newScale / oldScale) + panChange.x,
                                                centroid.y - center.y - (centroid.y - currentPan.y - center.y) * (newScale / oldScale) + panChange.y
                                            )
                                        } else currentPan

                                        val maxPanX = (boardWidth * (newScale - 1f).coerceAtLeast(0f) / 2f) + (viewportWidth * 0.35f)
                                        val maxPanY = (boardHeight * (newScale - 1f).coerceAtLeast(0f) / 2f) + (viewportHeight * 0.35f)

                                        scale = newScale
                                        panX = newPan.x.coerceIn(-maxPanX, maxPanX)
                                        panY = newPan.y.coerceIn(-maxPanY, maxPanY)

                                        event.changes.forEach { it.consume() }
                                    }
                                } else if (pressedList.size == 1) {
                                    val change = pressedList.first()
                                    lastPos = change.position
                                    val dragDist = (lastPos - startPos).getDistance()

                                    if (dragDist > dragThreshold) {
                                        isDragging = true
                                        longPressJob?.cancel()
                                        if (isLongPressTriggered && candidateArrow != null) {
                                            onArrowLongPress(candidateArrow, false)
                                            isLongPressTriggered = false
                                        }
                                    }

                                    if (isDragging && scale > 1.02f) {
                                        val delta = change.positionChange()
                                        val maxPanX = (boardWidth * (scale - 1f) / 2f) + (viewportWidth * 0.35f)
                                        val maxPanY = (boardHeight * (scale - 1f) / 2f) + (viewportHeight * 0.35f)

                                        panX = (panX + delta.x).coerceIn(-maxPanX, maxPanX)
                                        panY = (panY + delta.y).coerceIn(-maxPanY, maxPanY)

                                        // Push into velocity buffer
                                        if (velBuf.size >= 5) velBuf.removeFirst()
                                        velBuf.addLast(VelSample(delta, now - lastMoveTime))
                                        lastMoveTime = now

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

                        // Calculate release velocity for inertia coasting
                        var inertiaVel = Offset.Zero
                        if (velBuf.isNotEmpty()) {
                            var totalWeight = 0f
                            var weightedVx = 0f
                            var weightedVy = 0f
                            velBuf.forEachIndexed { idx, sample ->
                                val w = (idx + 1).toFloat()
                                val dt = sample.timeMs.coerceIn(5L, 50L) / 1000f
                                weightedVx += (sample.delta.x / dt) * w
                                weightedVy += (sample.delta.y / dt) * w
                                totalWeight += w
                            }
                            if (totalWeight > 0f) {
                                inertiaVel = Offset(weightedVx / totalWeight, weightedVy / totalWeight)
                            }
                        }

                        // Post-gesture handling: Settle / Coasting / Taps
                        val elapsed = System.currentTimeMillis() - startTime
                        val totalMovement = (lastPos - startPos).getDistance()

                        // A tap is recognized if not multi-touch, no heavy drag, and duration < 450ms
                        if (!isMultiTouch && !isLongPressTriggered && totalMovement < dragThreshold && elapsed < 450) {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < 320 && (startPos - lastTapPos).getDistance() < dragThreshold * 1.5f) {
                                // Double tap: Toggle between 1.0x overview and comfortable zoomed inspection
                                settleJob = coroutineScope.launch {
                                    if (scale > 1.15f) {
                                        launch {
                                            val sAnim = Animatable(scale)
                                            sAnim.animateTo(1.0f, tween(250, easing = FastOutSlowInEasing)) {
                                                scale = value
                                            }
                                        }
                                        launch {
                                            val pxAnim = Animatable(panX)
                                            pxAnim.animateTo(0f, tween(250, easing = FastOutSlowInEasing)) {
                                                panX = value
                                            }
                                        }
                                        launch {
                                            val pyAnim = Animatable(panY)
                                            pyAnim.animateTo(0f, tween(250, easing = FastOutSlowInEasing)) {
                                                panY = value
                                            }
                                        }
                                    } else {
                                        val targetZoom = minOf(2.8f, maxScale)
                                        val targetPanX = -(startPos.x - center.x) * (targetZoom - 1f)
                                        val targetPanY = -(startPos.y - center.y) * (targetZoom - 1f)
                                        val maxPX = (boardWidth * (targetZoom - 1f) / 2f) + (viewportWidth * 0.35f)
                                        val maxPY = (boardHeight * (targetZoom - 1f) / 2f) + (viewportHeight * 0.35f)

                                        launch {
                                            val sAnim = Animatable(scale)
                                            sAnim.animateTo(targetZoom, tween(250, easing = FastOutSlowInEasing)) {
                                                scale = value
                                            }
                                        }
                                        launch {
                                            val pxAnim = Animatable(panX)
                                            pxAnim.animateTo(targetPanX.coerceIn(-maxPX, maxPX), tween(250, easing = FastOutSlowInEasing)) {
                                                panX = value
                                            }
                                        }
                                        launch {
                                            val pyAnim = Animatable(panY)
                                            pyAnim.animateTo(targetPanY.coerceIn(-maxPY, maxPY), tween(250, easing = FastOutSlowInEasing)) {
                                                panY = value
                                            }
                                        }
                                    }
                                }
                                lastTapTime = 0L
                            } else {
                                lastTapTime = now
                                lastTapPos = startPos

                                // Try hit test at lastPos (where finger lifted), fallback to startPos
                                val localUp = center + (lastPos - Offset(panX, panY) - center) / scale
                                val tappedArrow = findClosestArrow(localUp, boardOrigin, arrows, cellSize, xSize, ySize)
                                    ?: candidateArrow

                                // Spawn tap particle at local board coordinate (Unity TapParticleEffect color #0088F5)
                                tapParticles.add(TapParticle(origin = localUp, color = Color(0xFF0088F5)))

                                if (tappedArrow != null) {
                                    onArrowClick(tappedArrow)
                                }
                            }
                        } else if (scale < 1.0f) {
                            // Rubber-band settle back to 1.0x when released below normal scale
                            settleJob = coroutineScope.launch {
                                launch {
                                    val sAnim = Animatable(scale)
                                    sAnim.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) {
                                        scale = value
                                    }
                                }
                                launch {
                                    val pxAnim = Animatable(panX)
                                    pxAnim.animateTo(0f, tween(200, easing = FastOutSlowInEasing)) {
                                        panX = value
                                    }
                                }
                                launch {
                                    val pyAnim = Animatable(panY)
                                    pyAnim.animateTo(0f, tween(200, easing = FastOutSlowInEasing)) {
                                        panY = value
                                    }
                                }
                            }
                        } else if (inertiaVel != Offset.Zero && scale > 1.02f) {
                            // Inertia momentum coasting with friction decay
                            settleJob = coroutineScope.launch {
                                var vx = inertiaVel.x * 0.75f
                                var vy = inertiaVel.y * 0.75f
                                while (hypot(vx, vy) > 30f) {
                                    val curPX = panX + vx * 0.016f
                                    val curPY = panY + vy * 0.016f
                                    val maxPX = (boardWidth * (scale - 1f) / 2f) + (viewportWidth * 0.35f)
                                    val maxPY = (boardHeight * (scale - 1f) / 2f) + (viewportHeight * 0.35f)

                                    if (curPX !in -maxPX..maxPX) vx = -vx * 0.25f
                                    if (curPY !in -maxPY..maxPY) vy = -vy * 0.25f

                                    panX = curPX.coerceIn(-maxPX, maxPX)
                                    panY = curPY.coerceIn(-maxPY, maxPY)

                                    vx *= 0.91f
                                    vy *= 0.91f
                                    delay(16L)
                                }
                            }
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            withTransform({
                translate(panX, panY)
                scale(scale, scale, pivot = center)
            }) {
                // Background is completely transparent; surroundings adapt naturally to theme

                // 1. Subtle level silhouette cell dots (revealed when arrows clear, matching Unity Screenshot 1)
                for (coord in initialLevelCells) {
                    drawCircle(
                        color = Color(0xFF94A3B8).copy(alpha = 0.35f),
                        radius = (cellSize * 0.055f).coerceIn(1.8f, 3.2f),
                        center = Offset(
                            boardOrigin.x + (coord.x + 0.5f) * cellSize,
                            boardOrigin.y + (coord.y + 0.5f) * cellSize
                        )
                    )
                }

                // 2. Full Grid Lines / Dots only if user activated via '#' Booster button
                if (isGridLinesActive) {
                    drawGridLines(boardOrigin, xSize, ySize, cellSize, theme.gridLine)
                    drawGridDots(boardOrigin, xSize, ySize, cellSize, theme.gridDot)
                }

                // 3. Draw Preview Ray if an arrow is long-pressed (smooth animated fade matching Unity lineCg.DOFade)
                if (previewArrowToRender != null && previewAlpha > 0.01f) {
                    drawPreviewRay(boardOrigin, previewArrowToRender, xSize, ySize, cellSize, previewAlpha)
                }

                // 4. Draw All Arrows (Static, flying out, or wrong-move bouncing)
                for (arrow in arrows) {
                    if (arrow.state == ArrowState.CLEARED) continue

                    drawArrowEntity(
                        origin = boardOrigin,
                        arrow = arrow,
                        xSize = xSize,
                        ySize = ySize,
                        cellSize = cellSize,
                        theme = theme,
                        hintPulseScale = hintPulseScale,
                        previewRayArrowId = previewRayArrowId
                    )
                }

                // 4. Draw Tap Particles
                drawTapParticles(tapParticles)
            }
        }

        // Floating Reset Zoom pill button shown whenever zoomed in
        if (scale > 1.05f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clickable {
                        settleJob?.cancel()
                        settleJob = coroutineScope.launch {
                            launch {
                                val sAnim = Animatable(scale)
                                sAnim.animateTo(1.0f, tween(240, easing = FastOutSlowInEasing)) {
                                    scale = value
                                }
                            }
                            launch {
                                val pxAnim = Animatable(panX)
                                pxAnim.animateTo(0f, tween(240, easing = FastOutSlowInEasing)) {
                                    panX = value
                                }
                            }
                            launch {
                                val pyAnim = Animatable(panY)
                                pyAnim.animateTo(0f, tween(240, easing = FastOutSlowInEasing)) {
                                    panY = value
                                }
                            }
                        }
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

/**
 * Finds the arrow physically selected by the touch point.
 * Phase 1: Direct Grid Cell Lookup (100% accurate hit matching Unity's Box Collider on arrow cells)
 * Phase 2: Proximity fallback for taps on borders, arrowheads, or near cells.
 */
private fun findClosestArrow(
    touchPoint: Offset,
    origin: Offset,
    arrows: List<Arrow>,
    cellSize: Float,
    xSize: Int,
    ySize: Int
): Arrow? {
    // 1. Precise Grid Cell Lookup (100% accurate hit matching Unity's Box Collider on arrow cells)
    val col = kotlin.math.floor((touchPoint.x - origin.x) / cellSize).toInt()
    val row = kotlin.math.floor((touchPoint.y - origin.y) / cellSize).toInt()
    if (col in 0 until xSize && row in 0 until ySize) {
        val directHit = arrows.firstOrNull { arrow ->
            arrow.state != ArrowState.CLEARED && arrow.state != ArrowState.FLYING_OUT &&
            arrow.body.any { it.x == col && it.y == row }
        }
        if (directHit != null) return directHit
    }

    // 2. Proximity fallback for taps on borders, arrowheads, or near cells
    val hitThreshold = cellSize * 0.70f
    var bestArrow: Arrow? = null
    var bestDist = Float.MAX_VALUE

    for (arrow in arrows) {
        if (arrow.state == ArrowState.CLEARED || arrow.state == ArrowState.FLYING_OUT) continue

        val bodyPts = arrow.body.map { coord ->
            Offset(origin.x + (coord.x + 0.5f) * cellSize, origin.y + (coord.y + 0.5f) * cellSize)
        }
        if (bodyPts.isEmpty()) continue

        var minArrowDist = Float.MAX_VALUE

        // Distance to body segments
        for (i in 0 until bodyPts.size - 1) {
            val d = distanceToSegment(touchPoint, bodyPts[i], bodyPts[i + 1])
            if (d < minArrowDist) minArrowDist = d
        }

        // If single cell arrow
        if (bodyPts.size == 1) {
            val d = (touchPoint - bodyPts[0]).getDistance()
            if (d < minArrowDist) minArrowDist = d
        }

        // Distance to arrowhead tip
        val headCenter = bodyPts.last()
        val headTip = Offset(
            headCenter.x + arrow.direction.dx * (cellSize * 0.48f),
            headCenter.y + arrow.direction.dy * (cellSize * 0.48f)
        )
        val headDist = distanceToSegment(touchPoint, headCenter, headTip)
        if (headDist < minArrowDist) minArrowDist = headDist

        if (minArrowDist <= hitThreshold && minArrowDist < bestDist) {
            bestDist = minArrowDist
            bestArrow = arrow
        }
    }

    return bestArrow
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val vx = b.x - a.x
    val vy = b.y - a.y
    val segLenSq = vx * vx + vy * vy
    if (segLenSq == 0f) {
        return (p - a).getDistance()
    }
    val t = (((p.x - a.x) * vx + (p.y - a.y) * vy) / segLenSq).coerceIn(0f, 1f)
    val projX = a.x + t * vx
    val projY = a.y + t * vy
    return hypot(p.x - projX, p.y - projY)
}

private fun DrawScope.drawArrowEntity(
    origin: Offset,
    arrow: Arrow,
    xSize: Int,
    ySize: Int,
    cellSize: Float,
    theme: ThemePreset,
    hintPulseScale: Float,
    previewRayArrowId: String?
) {
    val isPreviewHold = (arrow.id == previewRayArrowId)
    val isLaunching = (arrow.state == ArrowState.FLYING_OUT && arrow.flightProgress < 0.35f)

    val baseColor = when {
        arrow.state == ArrowState.WRONG_FLASH -> theme.arrowWrong
        isPreviewHold -> Color(0xFF0647DB) // Deep Vivid Sapphire Blue for Long-Press Hold (Unity LongPressBlueArrow)
        isLaunching -> Color(0xFF0088F5)   // Electric Dodger Blue for Click Pop Takeoff (Unity correctArrowAnimColor)
        arrow.state == ArrowState.HINTED -> theme.arrowHint
        else -> theme.arrowDefault
    }

    // Clamped stroke width with expansion pulse when held or clicked
    val minStroke = 4.dp.toPx()
    val maxStroke = 9.dp.toPx()
    val scaleMultiplier = when {
        isPreviewHold -> 1.30f * hintPulseScale
        isLaunching -> 1.22f
        arrow.state == ArrowState.WRONG_FLASH -> 1.15f
        arrow.state == ArrowState.HINTED -> hintPulseScale
        else -> 1.0f
    }
    val strokeWidth = (cellSize * 0.18f).coerceIn(minStroke, maxStroke) * scaleMultiplier

    // Clamped arrowhead size to maintain sharp, distinct triangular heads
    val minHead = 12.dp.toPx()
    val maxHead = 26.dp.toPx()
    val headSize = (cellSize * 0.48f).coerceIn(minHead, maxHead) * (if (isPreviewHold) 1.22f else if (isLaunching) 1.15f else 1.0f)

    // Build the full motion track: body points + escape extension along arrow.direction
    val track = mutableListOf<Offset>()
    for (coord in arrow.body) {
        track.add(Offset(origin.x + (coord.x + 0.5f) * cellSize, origin.y + (coord.y + 0.5f) * cellSize))
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
            // Physical impact punch forward into obstacle and elastic recoil spring back (Unity DOTween punch)
            val t = arrow.flightProgress
            (sin(t * PI.toFloat()) * 0.32f - sin(t * 2f * PI.toFloat()) * 0.10f).coerceAtLeast(-0.15f)
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

    // 1. Draw body stroke along the moving polyline with smooth rounded corner fillets
    if (sampledPoints.size > 1) {
        val path = buildRoundedCornerPath(sampledPoints, radius = cellSize * 0.30f)
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

    // 2. Draw refined broader triangular arrowhead at the moving head point
    val currentHead = sampledPoints.last()
    rotate(
        degrees = arrow.direction.angleDegrees,
        pivot = currentHead
    ) {
        val arrowHeadPath = Path().apply {
            moveTo(currentHead.x, currentHead.y - headSize * 0.58f) // Forward tip
            lineTo(currentHead.x + headSize * 0.45f, currentHead.y + headSize * 0.34f) // Right base
            lineTo(currentHead.x - headSize * 0.45f, currentHead.y + headSize * 0.34f) // Left base
            close()
        }

        drawPath(
            path = arrowHeadPath,
            color = renderColor
        )
    }
}

/**
 * Builds a smooth path with circular fillet arcs on every 90° turn using quadratic beziers.
 * Replicates Unity's GenerateRoundedPath for smooth, polished arcade visuals.
 */
private fun buildRoundedCornerPath(points: List<Offset>, radius: Float): Path {
    val path = Path()
    if (points.isEmpty()) return path
    if (points.size == 1) {
        path.moveTo(points[0].x, points[0].y)
        return path
    }
    if (points.size == 2) {
        path.moveTo(points[0].x, points[0].y)
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size - 1) {
        val prev = points[i - 1]
        val cur = points[i]
        val next = points[i + 1]

        val vIn = cur - prev
        val lenIn = hypot(vIn.x, vIn.y)
        val vOut = next - cur
        val lenOut = hypot(vOut.x, vOut.y)

        if (lenIn < 0.001f || lenOut < 0.001f) {
            path.lineTo(cur.x, cur.y)
            continue
        }

        val uIn = Offset(vIn.x / lenIn, vIn.y / lenIn)
        val uOut = Offset(vOut.x / lenOut, vOut.y / lenOut)

        // Check if turn is straight
        val dot = uIn.x * uOut.x + uIn.y * uOut.y
        if (dot > 0.999f) {
            path.lineTo(cur.x, cur.y)
            continue
        }

        // Corner radius constrained by incoming and outgoing segment lengths
        val r = minOf(radius, lenIn * 0.44f, lenOut * 0.44f)
        val p1 = Offset(cur.x - uIn.x * r, cur.y - uIn.y * r)
        val p2 = Offset(cur.x + uOut.x * r, cur.y + uOut.y * r)

        path.lineTo(p1.x, p1.y)
        path.quadraticTo(cur.x, cur.y, p2.x, p2.y)
    }

    path.lineTo(points.last().x, points.last().y)
    return path
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

        // 1. Expanding soft shockwave ring (Unity TapParticleEffect)
        val ringRadius = (6.dp.toPx() + prog * 30.dp.toPx())
        drawCircle(
            color = p.color.copy(alpha = alpha * 0.70f),
            radius = ringRadius,
            center = p.origin,
            style = Stroke(width = 2.2.dp.toPx())
        )

        // 2. Center flash dot
        val centerDotRadius = (4.5.dp.toPx() * (1f - prog)).coerceAtLeast(0f)
        if (centerDotRadius > 0.5f) {
            drawCircle(
                color = p.color.copy(alpha = alpha * 0.85f),
                radius = centerDotRadius,
                center = p.origin
            )
        }

        // 3. 4 orbital sparkle dots traveling outward
        val sparkDist = (8.dp.toPx() + prog * 32.dp.toPx())
        val angles = listOf(45.0, 135.0, 225.0, 315.0)
        for (deg in angles) {
            val rad = deg * PI / 180.0
            val sparkCenter = Offset(
                x = (p.origin.x + sparkDist * cos(rad)).toFloat(),
                y = (p.origin.y + sparkDist * sin(rad)).toFloat()
            )
            drawCircle(
                color = p.color.copy(alpha = alpha * 0.85f),
                radius = (2.5.dp.toPx() * (1f - prog * 0.6f)).coerceAtLeast(0.5f),
                center = sparkCenter
            )
        }
    }
}

private fun DrawScope.drawGridLines(origin: Offset, xSize: Int, ySize: Int, cellSize: Float, color: Color) {
    for (x in 0..xSize) {
        drawLine(
            color = color,
            start = Offset(origin.x + x * cellSize, origin.y),
            end = Offset(origin.x + x * cellSize, origin.y + ySize * cellSize),
            strokeWidth = 1.2f
        )
    }
    for (y in 0..ySize) {
        drawLine(
            color = color,
            start = Offset(origin.x, origin.y + y * cellSize),
            end = Offset(origin.x + xSize * cellSize, origin.y + y * cellSize),
            strokeWidth = 1.2f
        )
    }
}

private fun DrawScope.drawGridDots(origin: Offset, xSize: Int, ySize: Int, cellSize: Float, color: Color) {
    for (x in 0..xSize) {
        for (y in 0..ySize) {
            drawCircle(
                color = color,
                radius = 3.0f,
                center = Offset(origin.x + x * cellSize, origin.y + y * cellSize)
            )
        }
    }
}

private fun DrawScope.drawPreviewRay(
    origin: Offset,
    arrow: Arrow,
    xSize: Int,
    ySize: Int,
    cellSize: Float,
    previewAlpha: Float
) {
    val headCenter = Offset(
        x = origin.x + (arrow.head.x + 0.5f) * cellSize,
        y = origin.y + (arrow.head.y + 0.5f) * cellSize
    )

    var endOffset = headCenter
    when (arrow.direction) {
        Direction.UP -> endOffset = Offset(headCenter.x, origin.y - cellSize * 1.5f)
        Direction.DOWN -> endOffset = Offset(headCenter.x, origin.y + (ySize + 1.5f) * cellSize)
        Direction.LEFT -> endOffset = Offset(origin.x - cellSize * 1.5f, headCenter.y)
        Direction.RIGHT -> endOffset = Offset(origin.x + (xSize + 1.5f) * cellSize, headCenter.y)
    }

    // Unity reference: LongPressBlue #0088F5
    val beamColor = Color(0xFF0088F5)

    // 1. Soft diffused outer glow aura
    drawLine(
        color = beamColor.copy(alpha = 0.22f * previewAlpha),
        start = headCenter,
        end = endOffset,
        strokeWidth = 11.dp.toPx(),
        cap = StrokeCap.Round
    )

    // 2. Concentrated inner glow
    drawLine(
        color = beamColor.copy(alpha = 0.45f * previewAlpha),
        start = headCenter,
        end = endOffset,
        strokeWidth = 5.5.dp.toPx(),
        cap = StrokeCap.Round
    )

    // 3. High-contrast crisp core guidance ray
    drawLine(
        color = beamColor.copy(alpha = 0.95f * previewAlpha),
        start = headCenter,
        end = endOffset,
        strokeWidth = 2.8.dp.toPx(),
        cap = StrokeCap.Round
    )

    // 4. Directional exit beacon chevron / arrowhead at destination edge
    rotate(
        degrees = arrow.direction.angleDegrees,
        pivot = endOffset
    ) {
        val exitHeadSize = 15.dp.toPx()
        val exitHeadPath = Path().apply {
            moveTo(endOffset.x, endOffset.y - exitHeadSize * 0.58f)
            lineTo(endOffset.x + exitHeadSize * 0.46f, endOffset.y + exitHeadSize * 0.36f)
            lineTo(endOffset.x - exitHeadSize * 0.46f, endOffset.y + exitHeadSize * 0.36f)
            close()
        }
        drawPath(path = exitHeadPath, color = beamColor.copy(alpha = 0.95f * previewAlpha))
    }

    // 5. Pulsing beacon ring and dot at origin head
    drawCircle(
        color = beamColor.copy(alpha = 0.65f * previewAlpha),
        radius = 8.dp.toPx(),
        center = headCenter,
        style = Stroke(width = 2.dp.toPx())
    )
    drawCircle(
        color = beamColor.copy(alpha = 0.95f * previewAlpha),
        radius = 3.5.dp.toPx(),
        center = headCenter
    )
}
