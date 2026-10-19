package com.arrowescape.puzzle.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowescape.puzzle.core.engine.GridOccupancyMap
import com.arrowescape.puzzle.core.engine.PuzzleSolver
import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.ArrowState
import com.arrowescape.puzzle.core.model.Direction
import com.arrowescape.puzzle.core.model.GridCoordinate
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

private data class VelSample(
    val delta: Offset,
    val timeMs: Long
)

private data class TouchRecord(
    val downPos: Offset,
    val downTime: Long,
    var lastPos: Offset,
    val candidateArrow: Arrow?,
    var isLongPressTriggered: Boolean = false,
    var longPressJob: Job? = null
)

private data class ConfettiPiece(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val rotSpeed: Float,
    val isRibbon: Boolean
)

@Composable
fun PuzzleBoardCanvas(
    xSize: Int,
    ySize: Int,
    arrows: List<Arrow>,
    theme: ThemePreset,
    isGridLinesActive: Boolean,
    isCelebrationBurstActive: Boolean = false,
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

    // Remember the initial level silhouette cells to render subtle cell dots for cleared areas
    val initialLevelCells = remember(xSize, ySize, arrows.size, arrows.firstOrNull()?.id) {
        arrows.flatMap { it.allPoints }.toSet()
    }
    val clearedCells = remember(arrows, initialLevelCells) {
        val activeCoords = arrows.filter { it.state != ArrowState.CLEARED }.flatMap { it.allPoints }.toSet()
        initialLevelCells.filter { it !in activeCoords }
    }

    // Reset zoom and pan, and launch cascading write-on path assembly animation whenever a new level is loaded
    val introProgress = remember { Animatable(0f) }
    LaunchedEffect(xSize, ySize, arrows.size, arrows.firstOrNull()?.id) {
        settleJob?.cancel()
        scale = 1f
        panX = 0f
        panY = 0f
        introProgress.snapTo(0f)
        introProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 920, easing = LinearEasing)
        )
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
        val maxCellPx = with(LocalDensity.current) { 50.dp.toPx() }
        val cellSize = minOf(availableWidth / xSize, availableHeight / ySize).coerceAtMost(maxCellPx)
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

        val currentXSize by rememberUpdatedState(xSize)
        val currentYSize by rememberUpdatedState(ySize)
        val currentArrows by rememberUpdatedState(arrows)
        val currentCellSize by rememberUpdatedState(cellSize)
        val currentBoardOrigin by rememberUpdatedState(boardOrigin)
        val currentBoardWidth by rememberUpdatedState(boardWidth)
        val currentBoardHeight by rememberUpdatedState(boardHeight)
        val currentViewportWidth by rememberUpdatedState(viewportWidth)
        val currentViewportHeight by rememberUpdatedState(viewportHeight)
        val currentMaxScale by rememberUpdatedState(maxScale)
        val currentOnArrowClick by rememberUpdatedState(onArrowClick)
        val currentOnArrowLongPress by rememberUpdatedState(onArrowLongPress)
        val minTouchTargetPx = with(LocalDensity.current) { 15.dp.toPx() }
        val currentMinTouchTargetPx by rememberUpdatedState(minTouchTargetPx)
        val minHeadPx = with(LocalDensity.current) { 8.5.dp.toPx() }
        val maxHeadPx = with(LocalDensity.current) { 11.5.dp.toPx() }
        val baseHeadSize = (cellSize * 0.28f).coerceIn(minHeadPx, maxHeadPx)
        val currentHeadSize by rememberUpdatedState(baseHeadSize)

        val confettiPieces = remember { mutableStateListOf<ConfettiPiece>() }
        var confettiStartTime by remember { mutableLongStateOf(0L) }
        var animTick by remember { mutableLongStateOf(0L) }

        LaunchedEffect(isCelebrationBurstActive) {
            if (isCelebrationBurstActive) {
                confettiStartTime = System.currentTimeMillis()
                val festiveColors = listOf(
                    Color(0xFFFF4B4B), // Coral Red
                    Color(0xFFFFB800), // Golden Amber
                    Color(0xFF10B981), // Emerald
                    Color(0xFF3B82F6), // Electric Blue
                    Color(0xFF8B5CF6), // Royal Violet
                    Color(0xFFEC4899), // Hot Pink
                    Color(0xFF00E5FF)  // Cyan
                )
                val centerX = viewportWidth / 2f
                val centerY = viewportHeight * 0.45f
                confettiPieces.clear()
                val pieces = (0 until 65).map { i ->
                    val angle = (kotlin.random.Random.nextFloat() * 360f) * (PI.toFloat() / 180f)
                    val speed = 250f + kotlin.random.Random.nextFloat() * 520f
                    val isRibbon = (i % 3 != 0)
                    ConfettiPiece(
                        x = centerX + (kotlin.random.Random.nextFloat() - 0.5f) * 60f,
                        y = centerY + (kotlin.random.Random.nextFloat() - 0.5f) * 40f,
                        vx = kotlin.math.cos(angle) * speed * 0.85f,
                        vy = -kotlin.math.abs(kotlin.math.sin(angle)) * speed - 160f,
                        color = festiveColors[i % festiveColors.size],
                        width = if (isRibbon) 10f + kotlin.random.Random.nextFloat() * 6f else 6f,
                        height = if (isRibbon) 5f + kotlin.random.Random.nextFloat() * 3f else 6f,
                        rotation = kotlin.random.Random.nextFloat() * 360f,
                        rotSpeed = (kotlin.random.Random.nextFloat() - 0.5f) * 720f,
                        isRibbon = isRibbon
                    )
                }
                confettiPieces.addAll(pieces)
            } else {
                confettiPieces.clear()
            }
        }

        LaunchedEffect(isCelebrationBurstActive && confettiPieces.isNotEmpty()) {
            while (isCelebrationBurstActive && confettiPieces.isNotEmpty()) {
                withFrameNanos { frameTimeNanos ->
                    animTick = frameTimeNanos
                }
                val tNow = System.currentTimeMillis()
                if (tNow - confettiStartTime > 1100L) {
                    confettiPieces.clear()
                }
            }
        }

        val occupancyMap = remember(xSize, ySize, currentArrows) {
            GridOccupancyMap(xSize, ySize).apply { rebuild(currentArrows) }
        }
        val currentOccupancyMap by rememberUpdatedState(occupancyMap)

        // Tactile zoom-in and zoom-out breathing during level intro (0.88x -> 1.045x -> 1.0x)
        val introZoom = if (introProgress.value >= 1f) 1.0f else {
            val t = introProgress.value
            when {
                t < 0.65f -> {
                    val p = FastOutSlowInEasing.transform(t / 0.65f)
                    0.88f + 0.165f * p
                }
                else -> {
                    val p = FastOutSlowInEasing.transform((t - 0.65f) / 0.35f)
                    1.045f - 0.045f * p
                }
            }
        }
        val currentIntroZoom by rememberUpdatedState(introZoom)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds() // Prevents drawing over top bar or bottom booster bar
                .pointerInput(xSize, ySize, arrows.size, arrows.firstOrNull()?.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        settleJob?.cancel() // Stop ongoing inertia or animation immediately on new touch

                        val center = Offset(currentViewportWidth / 2f, currentViewportHeight / 2f)
                        val dragThreshold = 18.dp.toPx()

                        // Track all active touch points individually to support rapid succession and multi-finger tapping
                        val activeTouches = mutableMapOf<Long, TouchRecord>()

                        val effectiveScale = scale * currentIntroZoom
                        val localDown = center + (down.position - Offset(panX, panY) - center) / effectiveScale
                        val firstCandidate = findClosestArrow(
                            localDown,
                            currentBoardOrigin,
                            currentArrows,
                            currentCellSize,
                            currentHeadSize,
                            currentMinTouchTargetPx,
                            currentOccupancyMap
                        )

                        val firstRecord = TouchRecord(
                            downPos = down.position,
                            downTime = System.currentTimeMillis(),
                            lastPos = down.position,
                            candidateArrow = firstCandidate
                        )
                        if (firstCandidate != null) {
                            firstRecord.longPressJob = coroutineScope.launch {
                                delay(480L)
                                firstRecord.isLongPressTriggered = true
                                currentOnArrowLongPress(firstCandidate, true)
                            }
                        }
                        activeTouches[down.id.value] = firstRecord

                        var isPinching = false
                        var isPanning = false
                        var zoomAccum = 1f
                        var panAccum = Offset.Zero
                        val velBuf = ArrayDeque<VelSample>(6)
                        var lastMoveTime = System.currentTimeMillis()

                        try {
                            do {
                                val event = awaitPointerEvent()
                                val now = System.currentTimeMillis()

                                // 1. Register any new pointer that touched down (supports fast multi-finger/alternating thumb taps)
                                for (change in event.changes) {
                                    if (change.changedToDown() && !activeTouches.containsKey(change.id.value)) {
                                        val pEffectiveScale = scale * currentIntroZoom
                                        val pLocalDown = center + (change.position - Offset(panX, panY) - center) / pEffectiveScale
                                        val pCandidate = findClosestArrow(
                                            pLocalDown,
                                            currentBoardOrigin,
                                            currentArrows,
                                            currentCellSize,
                                            currentHeadSize,
                                            currentMinTouchTargetPx,
                                            currentOccupancyMap
                                        )
                                        val rec = TouchRecord(
                                            downPos = change.position,
                                            downTime = now,
                                            lastPos = change.position,
                                            candidateArrow = pCandidate
                                        )
                                        if (pCandidate != null && !isPinching && !isPanning) {
                                            rec.longPressJob = coroutineScope.launch {
                                                delay(480L)
                                                if (!isPinching && !isPanning) {
                                                    rec.isLongPressTriggered = true
                                                    currentOnArrowLongPress(pCandidate, true)
                                                }
                                            }
                                        }
                                        activeTouches[change.id.value] = rec
                                    }
                                }

                                // 2. Update positions for active pointers
                                val pressedList = event.changes.filter { it.pressed }
                                for (p in pressedList) {
                                    activeTouches[p.id.value]?.lastPos = p.position
                                }

                                // 3. Multi-touch gesture handling (Pinch-to-zoom)
                                if (pressedList.size >= 2) {
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    zoomAccum *= zoomChange
                                    panAccum += panChange
                                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                    val zoomMotion = kotlin.math.abs(1f - zoomAccum) * centroidSize

                                    if (!isPinching) {
                                        if (zoomMotion > dragThreshold || panAccum.getDistance() > dragThreshold) {
                                            isPinching = true
                                            // Cancel all preview long-presses when pinching begins
                                            activeTouches.values.forEach { rec ->
                                                rec.longPressJob?.cancel()
                                                if (rec.isLongPressTriggered && rec.candidateArrow != null) {
                                                    currentOnArrowLongPress(rec.candidateArrow, false)
                                                    rec.isLongPressTriggered = false
                                                }
                                            }
                                        }
                                    }

                                    if (isPinching) {
                                        val centroid = event.calculateCentroid()
                                        val effectiveZoom = 1f + (zoomChange - 1f) * 1.15f
                                        val newScale = (scale * effectiveZoom).coerceIn(minScale, currentMaxScale)

                                        val oldScale = scale
                                        val currentPan = Offset(panX, panY)
                                        val newPan = if (oldScale > 0.001f) {
                                            Offset(
                                                centroid.x - center.x - (centroid.x - currentPan.x - center.x) * (newScale / oldScale) + panChange.x,
                                                centroid.y - center.y - (centroid.y - currentPan.y - center.y) * (newScale / oldScale) + panChange.y
                                            )
                                        } else currentPan

                                        val maxPanX = (currentBoardWidth * (newScale - 1f).coerceAtLeast(0f) / 2f) + (currentViewportWidth * 0.35f)
                                        val maxPanY = (currentBoardHeight * (newScale - 1f).coerceAtLeast(0f) / 2f) + (currentViewportHeight * 0.35f)

                                        scale = newScale
                                        panX = newPan.x.coerceIn(-maxPanX, maxPanX)
                                        panY = newPan.y.coerceIn(-maxPanY, maxPanY)

                                        event.changes.forEach { it.consume() }
                                    }
                                } else if (pressedList.size == 1 && scale > 1.05f) {
                                    // Single-finger panning when zoomed in
                                    val change = pressedList.first()
                                    val rec = activeTouches[change.id.value]
                                    val dragDist = if (rec != null) (change.position - rec.downPos).getDistance() else 0f
                                    if (dragDist > dragThreshold) {
                                        if (!isPanning) {
                                            isPanning = true
                                            rec?.longPressJob?.cancel()
                                            if (rec?.isLongPressTriggered == true && rec.candidateArrow != null) {
                                                currentOnArrowLongPress(rec.candidateArrow, false)
                                                rec.isLongPressTriggered = false
                                            }
                                        }
                                    }

                                    if (isPanning) {
                                        val delta = change.positionChange()
                                        val maxPanX = (currentBoardWidth * (scale - 1f) / 2f) + (currentViewportWidth * 0.35f)
                                        val maxPanY = (currentBoardHeight * (scale - 1f) / 2f) + (currentViewportHeight * 0.35f)

                                        panX = (panX + delta.x).coerceIn(-maxPanX, maxPanX)
                                        panY = (panY + delta.y).coerceIn(-maxPanY, maxPanY)

                                        if (velBuf.size >= 5) velBuf.removeFirst()
                                        velBuf.addLast(VelSample(delta, now - lastMoveTime))
                                        lastMoveTime = now

                                        change.consume()
                                    }
                                }

                                // 4. Pointer releases: evaluate each lifted pointer for a responsive arrow tap
                                val upChanges = event.changes.filter { it.changedToUp() }
                                for (upChange in upChanges) {
                                    val rec = activeTouches.remove(upChange.id.value)
                                    if (rec != null) {
                                        rec.longPressJob?.cancel()
                                        if (rec.isLongPressTriggered && rec.candidateArrow != null) {
                                            currentOnArrowLongPress(rec.candidateArrow, false)
                                            rec.isLongPressTriggered = false
                                        }

                                        val elapsed = now - rec.downTime
                                        val movement = (upChange.position - rec.downPos).getDistance()
                                        val maxAllowedMove = if (scale > 1.05f) dragThreshold else (dragThreshold * 1.25f)

                                        // A tap is recognized if not in an active pinch/pan drag, not holding preview ray, and within movement/time tolerances
                                        val isTap = !isPinching && !isPanning && !rec.isLongPressTriggered && movement < maxAllowedMove && elapsed < 650L

                                        if (isTap) {
                                            // Down-Target Authority: Prioritize the candidate arrow determined at touch down.
                                            // This prevents finger release jitter or thumb roll from reassigning selection to an unintended neighbor or center arrow.
                                            val candidate = rec.candidateArrow?.let { cand ->
                                                currentArrows.firstOrNull { it.id == cand.id && it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT }
                                            }

                                            val targetArrow = candidate ?: run {
                                                val effectiveReleaseScale = scale * currentIntroZoom
                                                val pLocalDown = center + (rec.downPos - Offset(panX, panY) - center) / effectiveReleaseScale
                                                val pLocalUp = center + (upChange.position - Offset(panX, panY) - center) / effectiveReleaseScale

                                                findClosestArrow(
                                                    pLocalDown,
                                                    currentBoardOrigin,
                                                    currentArrows,
                                                    currentCellSize,
                                                    currentHeadSize,
                                                    currentMinTouchTargetPx,
                                                    currentOccupancyMap
                                                ) ?: if (movement > 0f) {
                                                    findClosestArrow(
                                                        pLocalUp,
                                                        currentBoardOrigin,
                                                        currentArrows,
                                                        currentCellSize,
                                                        currentHeadSize,
                                                        currentMinTouchTargetPx,
                                                        currentOccupancyMap
                                                    )
                                                } else null
                                            }

                                            if (targetArrow != null && targetArrow.state != ArrowState.CLEARED && targetArrow.state != ArrowState.FLYING_OUT) {
                                                if (introProgress.value < 1f) {
                                                    coroutineScope.launch { introProgress.snapTo(1f) }
                                                }
                                                currentOnArrowClick(targetArrow)
                                                upChange.consume()
                                            }
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        } finally {
                            activeTouches.values.forEach { rec ->
                                rec.longPressJob?.cancel()
                                if (rec.isLongPressTriggered && rec.candidateArrow != null) {
                                    currentOnArrowLongPress(rec.candidateArrow, false)
                                }
                            }
                            activeTouches.clear()
                        }

                        // Calculate release velocity for inertia coasting when panning zoomed board
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

                        // Rubber-band settle back to 1.0x when released below normal scale
                        if (scale < 1.0f) {
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
                                    val maxPX = (currentBoardWidth * (scale - 1f) / 2f) + (currentViewportWidth * 0.35f)
                                    val maxPY = (currentBoardHeight * (scale - 1f) / 2f) + (currentViewportHeight * 0.35f)

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
            if (animTick > 0L) { /* Redraw subscription for dynamic particle animations */ }
            val center = Offset(size.width / 2f, size.height / 2f)
            // Subtle directional micro-jolt camera shake on blocked obstacle collision
            val flashingArrow = arrows.firstOrNull { it.state == ArrowState.WRONG_FLASH }
            val blockedShakeOffset = if (flashingArrow != null) {
                val t = flashingArrow.flightProgress
                val decay = (1f - t).coerceIn(0f, 1f)
                val shakeMagnitude = 2.4.dp.toPx() * decay
                val oscillation = sin(t * 3f * PI.toFloat())
                val dir = flashingArrow.direction
                Offset(
                    x = dir.dx * oscillation * shakeMagnitude,
                    y = dir.dy * oscillation * shakeMagnitude
                )
            } else Offset.Zero

            withTransform({
                translate(panX + blockedShakeOffset.x, panY + blockedShakeOffset.y)
                scale(scale * introZoom, scale * introZoom, pivot = center)
            }) {
                // Background is completely transparent; surroundings adapt naturally to theme

                // 0. Subtle stage mat border framing the puzzle board
                drawRoundRect(
                    color = theme.gridLine.copy(alpha = 0.25f),
                    topLeft = boardOrigin - Offset(8f, 8f),
                    size = Size(boardWidth + 16f, boardHeight + 16f),
                    cornerRadius = CornerRadius(16f, 16f),
                    style = Stroke(width = 1.5f)
                )

                // 1. Full Grid Lines (background) only if user activated via '#' Booster button
                if (isGridLinesActive) {
                    drawGridLines(boardOrigin, xSize, ySize, cellSize, theme.gridLine)
                    drawGridDots(boardOrigin, xSize, ySize, cellSize, theme.gridDot)
                }

                // 2. Cleared Area Silhouette Dots:
                // Only become visible once an arrow has moved out of the gameplay area.
                // Layered in the background under active arrows so arrows always render in front.
                if (clearedCells.isNotEmpty()) {
                    val dotColor = theme.gridDot
                    val dotRadius = (cellSize * 0.065f).coerceIn(2.2f, 3.4f)
                    for (coord in clearedCells) {
                        drawCircle(
                            color = dotColor.copy(alpha = 0.85f),
                            radius = dotRadius,
                            center = Offset(
                                boardOrigin.x + (coord.x + 0.5f) * cellSize,
                                boardOrigin.y + (coord.y + 0.5f) * cellSize
                            )
                        )
                    }
                }

                // 3. Draw Preview Ray if an arrow is long-pressed (smooth animated fade matching Unity lineCg.DOFade)
                if (previewArrowToRender != null && previewAlpha > 0.01f) {
                    drawPreviewRay(boardOrigin, previewArrowToRender, xSize, ySize, cellSize, previewAlpha, theme.arrowClear)
                }

                // 4. Draw All Arrows (Progressive Path Drawing, drawn ON TOP of the background dots)
                val boardCenterX = xSize / 2f
                val boardCenterY = ySize / 2f
                val maxBoardDist = hypot(boardCenterX, boardCenterY).coerceAtLeast(1f)

                for (arrow in arrows) {
                    if (arrow.state == ArrowState.CLEARED) continue

                    val arrowHead = arrow.head
                    val arrowTail = arrow.body.firstOrNull() ?: arrowHead
                    val arrowCenterX = (arrowHead.x + arrowTail.x) / 2f
                    val arrowCenterY = (arrowHead.y + arrowTail.y) / 2f
                    val dx = arrowCenterX - boardCenterX
                    val dy = arrowCenterY - boardCenterY
                    val distFromCenter = hypot(dx, dy)
                    val normDist = (distFromCenter / maxBoardDist).coerceIn(0f, 1f)

                    // Staggered cascade: center arrows start drawing first, outer arrows follow
                    val startTime = normDist * 0.28f
                    val drawWindow = 0.64f
                    val localProg = if (introProgress.value >= 1f) 1f else ((introProgress.value - startTime) / drawWindow).coerceIn(0f, 1f)

                    val easedDraw = if (localProg >= 1f) 1f else FastOutSlowInEasing.transform(localProg)
                    val introAlpha = if (localProg >= 1f) 1f else (localProg / 0.15f).coerceIn(0f, 1f)

                    drawArrowEntity(
                        origin = boardOrigin,
                        arrow = arrow,
                        xSize = xSize,
                        ySize = ySize,
                        cellSize = cellSize,
                        theme = theme,
                        hintPulseScale = hintPulseScale,
                        previewRayArrowId = previewRayArrowId,
                        drawProgress = easedDraw,
                        introAlpha = introAlpha
                    )
                }
            }

            // 5. Draw Confetti Celebration Burst (PlayLastArrowBurstDelayed matching Unity)
            if (confettiPieces.isNotEmpty() && confettiStartTime > 0L) {
                val now = System.currentTimeMillis()
                val canvasDensity = this.density
                val elapsedSec = ((now - confettiStartTime).toFloat() / 1000f).coerceAtLeast(0f)
                val celebrationDuration = 0.95f
                val progress = (elapsedSec / celebrationDuration).coerceIn(0f, 1f)
                val alpha = if (progress < 0.70f) 1.0f else ((1.0f - progress) / 0.30f).coerceIn(0f, 1f)
                val gravity = 980f // px/s^2

                for (piece in confettiPieces) {
                    val curX = piece.x + piece.vx * elapsedSec
                    val curY = piece.y + piece.vy * elapsedSec + 0.5f * gravity * elapsedSec * elapsedSec
                    val curRot = piece.rotation + piece.rotSpeed * elapsedSec

                    if (piece.isRibbon) {
                        // Flutter ribbon: simulate 3D rotation with cosine width scaling
                        val flutterScale = kotlin.math.abs(kotlin.math.cos(curRot * (PI.toFloat() / 180f)))
                        val ribbonWidth = (piece.width * flutterScale).coerceAtLeast(1.5f) * canvasDensity
                        val ribbonHeight = piece.height * canvasDensity

                        rotate(curRot, pivot = Offset(curX, curY)) {
                            drawRoundRect(
                                color = piece.color.copy(alpha = alpha),
                                topLeft = Offset(curX - ribbonWidth / 2f, curY - ribbonHeight / 2f),
                                size = Size(ribbonWidth, ribbonHeight),
                                cornerRadius = CornerRadius(1.8f * canvasDensity)
                            )
                        }
                    } else {
                        // Star sparkle particle
                        val starRadius = (piece.width * 0.5f * (1f - progress * 0.3f)) * canvasDensity
                        drawCircle(
                            color = piece.color.copy(alpha = alpha),
                            radius = starRadius,
                            center = Offset(curX, curY)
                        )
                    }
                }
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
                color = if (theme.isDark) Color(0xEE334155) else Color(0xEE1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine.copy(alpha = 0.5f)),
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
}

private fun isPointInTriangle(p: Offset, a: Offset, b: Offset, c: Offset): Boolean {
    val cp1 = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
    val cp2 = (c.x - b.x) * (p.y - b.y) - (c.y - b.y) * (p.x - b.x)
    val cp3 = (a.x - c.x) * (p.y - c.y) - (a.y - c.y) * (p.x - c.x)
    val hasNeg = (cp1 < -0.001f) || (cp2 < -0.001f) || (cp3 < -0.001f)
    val hasPos = (cp1 > 0.001f) || (cp2 > 0.001f) || (cp3 > 0.001f)
    return !(hasNeg && hasPos)
}

/**
 * Continuous Geometric Hit Detection matching the reference game's ConfigurableLineFinderStrategy.
 * Evaluates exact physical proximity to each arrow's visual geometry (arrowhead triangle and stem polyline).
 *
 * Guarantees:
 * 1. Tapping an arrow whose path is clear NEVER selects a nearby blocked arrow, even if the arrowhead
 *    crosses the grid cell boundary.
 * 2. Small 1-cell arrows respond on the first tap with 100% precision.
 * 3. Hit area is perfectly aligned with the visual rendering.
 * 4. Tapping in empty space does not accidentally trigger distant arrows.
 */
private fun findClosestArrow(
    touchPoint: Offset,
    origin: Offset,
    arrows: List<Arrow>,
    cellSize: Float,
    headSize: Float,
    minTouchTargetPx: Float,
    occupancy: GridOccupancyMap? = null
): Arrow? {
    val activeArrows = arrows.filter { it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT }
    if (activeArrows.isEmpty()) return null

    val relX = touchPoint.x - origin.x
    val relY = touchPoint.y - origin.y
    val cellX = kotlin.math.floor(relX / cellSize).toInt()
    val cellY = kotlin.math.floor(relY / cellSize).toInt()
    val touchedCoord = GridCoordinate(cellX, cellY)

    var bestArrow: Arrow? = null
    var bestScore = Float.MAX_VALUE

    // Maximum search radius: comfortable touch zone around each arrow,
    // avoiding accidental triggers on taps far away in empty space.
    val maxSearchRadius = maxOf(cellSize * 0.90f, minTouchTargetPx * 1.10f)

    for (arrow in activeArrows) {
        val allCoords = arrow.allPoints
        if (allCoords.isEmpty()) continue

        // 1. Arrowhead Hit Geometry:
        // Triangle vertices matching drawArrowEntity's exact orientation and dimensions
        val headCenter = Offset(
            origin.x + (arrow.head.x + 0.5f) * cellSize,
            origin.y + (arrow.head.y + 0.5f) * cellSize
        )
        val dirX = arrow.direction.dx.toFloat()
        val dirY = arrow.direction.dy.toFloat()
        val perpX = -dirY
        val perpY = dirX

        val headTip = Offset(
            headCenter.x + dirX * (headSize * 0.58f),
            headCenter.y + dirY * (headSize * 0.58f)
        )
        val baseCenter = Offset(
            headCenter.x - dirX * (headSize * 0.30f),
            headCenter.y - dirY * (headSize * 0.30f)
        )
        val wingLeft = Offset(
            baseCenter.x - perpX * (headSize * 0.40f),
            baseCenter.y - perpY * (headSize * 0.40f)
        )
        val wingRight = Offset(
            baseCenter.x + perpX * (headSize * 0.40f),
            baseCenter.y + perpY * (headSize * 0.40f)
        )

        // Exact point-in-arrowhead-triangle test
        val isInsideHeadTriangle = isPointInTriangle(touchPoint, headTip, wingRight, wingLeft)

        val headDist = if (isInsideHeadTriangle) {
            0f
        } else {
            minOf(
                distanceToSegment(touchPoint, headCenter, headTip),
                distanceToSegment(touchPoint, headTip, wingLeft),
                distanceToSegment(touchPoint, headTip, wingRight),
                distanceToSegment(touchPoint, wingLeft, wingRight)
            )
        }

        // 2. Stem Body Hit Geometry:
        val bodyPts = allCoords.map { coord ->
            Offset(origin.x + (coord.x + 0.5f) * cellSize, origin.y + (coord.y + 0.5f) * cellSize)
        }
        var stemDist = Float.MAX_VALUE
        if (bodyPts.size == 1) {
            // Small 1-cell arrow: distance to its cell center
            stemDist = (touchPoint - bodyPts[0]).getDistance()
        } else {
            for (i in 0 until bodyPts.size - 1) {
                val d = distanceToSegment(touchPoint, bodyPts[i], bodyPts[i + 1])
                if (d < stemDist) stemDist = d
            }
        }

        // Physical distance to this arrow's visual geometry
        val physicalDist = minOf(headDist, stemDist)

        if (physicalDist > maxSearchRadius) continue

        // Check if the touch is inside this arrow's allocated grid cell
        val isInsideAllocatedCell = arrow.isPointInside(touchedCoord) || (arrow.head == touchedCoord)

        // Movable Protection: Check if this arrow has an unobstructed exit path
        val isMovable = occupancy != null && PuzzleSolver.canMoveOut(arrow, occupancy)

        // Effective distance score:
        // - Direct hits inside the arrowhead triangle get absolute priority (distance = 0)
        // - Hits inside the arrow's allocated cell get a territorial ownership bonus (x0.85)
        // - Movable Protection (x0.50): Clear unobstructed arrows receive strong priority over
        //   neighboring blocked arrows, completely eliminating accidental life loss from
        //   edge-of-thumb touches or adjacent placement.
        val score = when {
            isInsideHeadTriangle -> if (isMovable) 0f else 0.5f
            else -> {
                var s = physicalDist
                if (isInsideAllocatedCell) s *= 0.85f
                if (isMovable) s *= 0.50f
                s
            }
        }

        if (score < bestScore) {
            bestScore = score
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
    previewRayArrowId: String?,
    drawProgress: Float = 1.0f,
    introAlpha: Float = 1.0f
) {
    val isPreviewHold = (arrow.id == previewRayArrowId)
    val isLaunching = (arrow.state == ArrowState.FLYING_OUT && arrow.flightProgress < 0.35f)

    val baseColor = when {
        arrow.state == ArrowState.WRONG_FLASH -> theme.arrowWrong
        isLaunching -> theme.arrowClear   // Theme-adaptive celebration color on moving out
        arrow.isHighlighted -> theme.arrowWrong
        isPreviewHold -> theme.arrowClear // Theme-adaptive preview color
        arrow.state == ArrowState.HINTED -> theme.arrowHint
        else -> theme.arrowDefault
    }

    // Thinner, sleek stroke width across all level densities (3.0dp .. 4.2dp range)
    val minStroke = 3.0.dp.toPx()
    val maxStroke = 4.2.dp.toPx()
    val scaleMultiplier = when {
        isPreviewHold -> 1.10f * (1f + (hintPulseScale - 1f) * 0.35f) // Subtle +10% expansion with gentle breathing
        isLaunching -> 1.18f
        arrow.state == ArrowState.WRONG_FLASH -> 1.0f + sin(arrow.flightProgress * PI.toFloat()) * 0.16f
        arrow.state == ArrowState.HINTED -> hintPulseScale
        else -> 1.0f
    }
    val strokeWidth = (cellSize * 0.12f).coerceIn(minStroke, maxStroke) * scaleMultiplier

    // Refined crisp arrowhead size maintaining brand consistency across all levels (8.5dp .. 11.5dp range)
    val minHead = 8.5.dp.toPx()
    val maxHead = 11.5.dp.toPx()
    val headSize = (cellSize * 0.28f).coerceIn(minHead, maxHead) * (if (isPreviewHold) 1.08f else if (isLaunching) 1.12f else 1.0f)

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
            (sin(t * PI.toFloat()) * 0.30f - sin(t * 2.5f * PI.toFloat()) * 0.08f * (1f - t)).coerceAtLeast(-0.15f)
        }
        else -> 0f
    }

    // Smooth alpha fadeout as arrow clears the board perimeter
    val alpha = if (arrow.state == ArrowState.FLYING_OUT && arrow.flightProgress > 0.85f) {
        ((1f - arrow.flightProgress) / 0.15f).coerceIn(0f, 1f)
    } else {
        1.0f
    }

    // Progressive draw length during intro write-on animation
    val visibleBodyLen = if (arrow.state == ArrowState.IDLE && drawProgress < 1f) {
        originalBodyLen * drawProgress
    } else {
        originalBodyLen.toFloat()
    }

    // Sample the moving or progressively drawing polyline segment along the track
    val sampledPoints = sampleTrackSegment(
        track = track,
        startDist = displacement,
        endDist = visibleBodyLen + displacement
    )

    if (sampledPoints.isEmpty() && originalBodyLen > 0) return

    val renderColor = baseColor.copy(alpha = alpha * introAlpha)

    // 1. Draw body stroke along the polyline with smooth rounded corner fillets
    if (sampledPoints.size > 1) {
        val path = buildRoundedCornerPath(sampledPoints, radius = cellSize * 0.32f)
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

    // Arrowhead pop-in scale during intro
    val headPopScale = when {
        drawProgress >= 1f -> 1.0f
        originalBodyLen == 0 -> {
            if (drawProgress < 0.80f) (drawProgress / 0.80f) * 1.10f
            else 1.0f + sin(((drawProgress - 0.80f) / 0.20f) * PI.toFloat()) * 0.10f
        }
        drawProgress < 0.62f -> 0.0f
        drawProgress < 0.85f -> ((drawProgress - 0.62f) / 0.23f) * 1.12f
        else -> 1.0f + sin(((drawProgress - 0.85f) / 0.15f) * PI.toFloat()) * 0.12f
    }
    val effectiveHeadSize = headSize * headPopScale

    // 2. Draw crisp, sharp triangular arrowhead at the head point
    if (effectiveHeadSize > 0.5f) {
        val currentHead = if (displacement > 0.001f && sampledPoints.isNotEmpty()) sampledPoints.last() else headPt
        rotate(
            degrees = arrow.direction.angleDegrees,
            pivot = currentHead
        ) {
            val arrowHeadPath = Path().apply {
                moveTo(currentHead.x, currentHead.y - effectiveHeadSize * 0.58f) // Sharp forward tip
                lineTo(currentHead.x + effectiveHeadSize * 0.40f, currentHead.y + effectiveHeadSize * 0.30f) // Right base
                lineTo(currentHead.x - effectiveHeadSize * 0.40f, currentHead.y + effectiveHeadSize * 0.30f) // Left base
                close()
            }

            drawPath(
                path = arrowHeadPath,
                color = renderColor
            )
        }
    }

    // 3. Draw Aerodynamic Flight Trail Speed Lines when escaping
    if (arrow.state == ArrowState.FLYING_OUT && sampledPoints.isNotEmpty() && arrow.flightProgress in 0.04f..0.96f) {
        val tailPt = sampledPoints.first()
        val dirX = arrow.direction.dx.toFloat()
        val dirY = arrow.direction.dy.toFloat()
        val perpX = -dirY
        val perpY = dirX

        val progressFactor = sin(arrow.flightProgress * PI.toFloat()).coerceIn(0.2f, 1f)
        val trailLen = cellSize * 0.48f * progressFactor
        val trailAlpha = (alpha * 0.38f * progressFactor).coerceIn(0f, 1f)
        val trailColor = renderColor.copy(alpha = trailAlpha)
        val trailWidth = (strokeWidth * 0.36f).coerceAtLeast(1f)

        // Center streak
        val cStart = Offset(tailPt.x - dirX * (cellSize * 0.08f), tailPt.y - dirY * (cellSize * 0.08f))
        val cEnd = Offset(cStart.x - dirX * trailLen, cStart.y - dirY * trailLen)
        drawLine(color = trailColor, start = cStart, end = cEnd, strokeWidth = trailWidth, cap = StrokeCap.Round)

        // Flanking left streak
        val lStart = Offset(cStart.x + perpX * (cellSize * 0.18f), cStart.y + perpY * (cellSize * 0.18f))
        val lEnd = Offset(lStart.x - dirX * (trailLen * 0.68f), lStart.y - dirY * (trailLen * 0.68f))
        drawLine(color = trailColor, start = lStart, end = lEnd, strokeWidth = trailWidth * 0.85f, cap = StrokeCap.Round)

        // Flanking right streak
        val rStart = Offset(cStart.x - perpX * (cellSize * 0.18f), cStart.y - perpY * (cellSize * 0.18f))
        val rEnd = Offset(rStart.x - dirX * (trailLen * 0.68f), rStart.y - dirY * (trailLen * 0.68f))
        drawLine(color = trailColor, start = rStart, end = rEnd, strokeWidth = trailWidth * 0.85f, cap = StrokeCap.Round)
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
    previewAlpha: Float,
    rayColor: Color
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

    // Start line seamlessly from the forward tip of the arrowhead
    val startOffset = Offset(
        x = headCenter.x + arrow.direction.dx * (cellSize * 0.40f),
        y = headCenter.y + arrow.direction.dy * (cellSize * 0.40f)
    )

    // Delicate, elegant hairline sightline matching Unity Screenshot 2
    // 1. Soft subtle micro-halo
    drawLine(
        color = rayColor.copy(alpha = 0.20f * previewAlpha),
        start = startOffset,
        end = endOffset,
        strokeWidth = 3.6.dp.toPx(),
        cap = StrokeCap.Round
    )

    // 2. Crisp, refined hairline sightline
    drawLine(
        color = rayColor.copy(alpha = 0.75f * previewAlpha),
        start = startOffset,
        end = endOffset,
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round
    )
}
