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
import androidx.compose.ui.graphics.Brush
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
    var maxDisplacement: Float = 0f,
    var totalMotion: Float = 0f,
    var isLongPressTriggered: Boolean = false,
    var longPressJob: Job? = null
) {
    fun hasDragged(threshold: Float): Boolean = maxDisplacement > threshold
}

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
    restartTrigger: Long = 0L,
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
    val initialLevelCells = remember(xSize, ySize, arrows.size, arrows.firstOrNull()?.id, restartTrigger) {
        arrows.flatMap { it.allPoints }.toSet()
    }
    val clearedCellTimestamps = remember(xSize, ySize, arrows.firstOrNull()?.id, restartTrigger) { mutableMapOf<GridCoordinate, Long>() }
    val clearedCells = remember(arrows, initialLevelCells) {
        val activeCoords = arrows.filter { it.state != ArrowState.CLEARED }.flatMap { it.allPoints }.toSet()
        val cleared = initialLevelCells.filter { it !in activeCoords }.toSet()
        val now = System.currentTimeMillis()
        for (coord in cleared) {
            if (!clearedCellTimestamps.containsKey(coord)) {
                clearedCellTimestamps[coord] = now
            }
        }
        cleared
    }
    val currentClearedCells by rememberUpdatedState(clearedCells)

    // Reset zoom and pan, and launch cascading write-on path assembly animation whenever a new level is loaded or restarted
    val introProgress = remember { Animatable(0f) }
    LaunchedEffect(xSize, ySize, arrows.size, arrows.firstOrNull()?.id, restartTrigger) {
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

    // Hint pulse and radiant glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val hintPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintPulse"
    )
    val hintGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintGlowAlpha"
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

        // Fit board into viewport with comfortable margin matching New Unity One (82% width, 80% height)
        val availableWidth = viewportWidth * 0.82f
        val availableHeight = viewportHeight * 0.80f
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
        val minStrokePx = with(LocalDensity.current) { 2.0.dp.toPx() }
        val maxStrokePx = with(LocalDensity.current) { 5.0.dp.toPx() }
        val baseStrokeWidth = (cellSize * 0.23f).coerceIn(minStrokePx, maxStrokePx)
        val baseHeadWidth = baseStrokeWidth * 2.0f
        val currentHeadSize by rememberUpdatedState(baseHeadWidth)

        // Smooth Auto-Framing: gently glides zoom and pan to center the hinted arrow if off-screen
        val hintedArrow = arrows.firstOrNull { it.state == ArrowState.HINTED }
        LaunchedEffect(hintedArrow?.id) {
            if (hintedArrow == null) return@LaunchedEffect
            if (introProgress.value < 1f) return@LaunchedEffect

            val head = hintedArrow.head
            val tail = hintedArrow.body.firstOrNull() ?: head
            val arrowBoardX = boardOrigin.x + ((head.x + tail.x) / 2f + 0.5f) * cellSize
            val arrowBoardY = boardOrigin.y + ((head.y + tail.y) / 2f + 0.5f) * cellSize

            val center = Offset(viewportWidth / 2f, viewportHeight / 2f)
            val currentScreenX = center.x + (arrowBoardX - center.x) * scale + panX
            val currentScreenY = center.y + (arrowBoardY - center.y) * scale + panY

            val isVisibleInViewport = currentScreenX in (viewportWidth * 0.18f..viewportWidth * 0.82f) &&
                    currentScreenY in (viewportHeight * 0.18f..viewportHeight * 0.82f)

            if (!isVisibleInViewport || scale > 1.6f) {
                settleJob?.cancel()
                val targetScale = scale.coerceIn(1.0f, 1.40f)

                var targetPanX = -(arrowBoardX - center.x) * targetScale
                var targetPanY = -(arrowBoardY - center.y) * targetScale

                val maxPanX = (boardWidth * (targetScale - 1f).coerceAtLeast(0f) / 2f) + (viewportWidth * 0.20f)
                val maxPanY = (boardHeight * (targetScale - 1f).coerceAtLeast(0f) / 2f) + (viewportHeight * 0.20f)

                targetPanX = targetPanX.coerceIn(-maxPanX, maxPanX)
                targetPanY = targetPanY.coerceIn(-maxPanY, maxPanY)

                val startScale = scale
                val startPanX = panX
                val startPanY = panY

                val anim = Animatable(0f)
                settleJob = coroutineScope.launch {
                    anim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing)
                    ) {
                        val p = value
                        scale = startScale + (targetScale - startScale) * p
                        panX = startPanX + (targetPanX - startPanX) * p
                        panY = startPanY + (targetPanY - startPanY) * p
                    }
                }
            }
        }

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

        LaunchedEffect(clearedCells.size) {
            if (clearedCells.isNotEmpty()) {
                val animStart = System.currentTimeMillis()
                while (System.currentTimeMillis() - animStart < 350L) {
                    withFrameNanos { frameTimeNanos ->
                        animTick = frameTimeNanos
                    }
                }
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
                        val dragThreshold = 14.dp.toPx()
                        val pinchMotionThreshold = 22.dp.toPx()
                        val panGestureThreshold = 18.dp.toPx()

                        var hadMultiTouch = false
                        var isPinching = false
                        var isPanning = false
                        var zoomAccum = 1f
                        var panAccum = Offset.Zero
                        val velBuf = ArrayDeque<VelSample>(6)
                        var lastMoveTime = System.currentTimeMillis()

                        val activeTouches = mutableMapOf<Long, TouchRecord>()

                        // Converts a screen point to local board space accounting for pan, scale, and intro zoom
                        fun screenToLocal(screenPos: Offset): Offset {
                            val effScale = scale * currentIntroZoom
                            return center + (screenPos - Offset(panX, panY) - center) / effScale
                        }

                        val firstRecord = TouchRecord(
                            downPos = down.position,
                            downTime = System.currentTimeMillis(),
                            lastPos = down.position
                        )

                        // Preview ray on long-press (holding down on an arrow without dragging)
                        val downArrow = findClosestArrow(
                            screenToLocal(down.position),
                            currentBoardOrigin,
                            currentArrows,
                            currentCellSize,
                            currentHeadSize,
                            currentMinTouchTargetPx
                        )
                        if (downArrow != null) {
                            firstRecord.longPressJob = coroutineScope.launch {
                                delay(480L)
                                if (!isPinching && !isPanning && !firstRecord.hasDragged(dragThreshold)) {
                                    firstRecord.isLongPressTriggered = true
                                    currentOnArrowLongPress(downArrow, true)
                                }
                            }
                        }
                        activeTouches[down.id.value] = firstRecord

                        try {
                            do {
                                val event = awaitPointerEvent()
                                val now = System.currentTimeMillis()

                                // 1. Register any new pointer that touched down (multi-touch tracking)
                                for (change in event.changes) {
                                    if (change.changedToDown() && !activeTouches.containsKey(change.id.value)) {
                                        hadMultiTouch = true
                                        val rec = TouchRecord(
                                            downPos = change.position,
                                            downTime = now,
                                            lastPos = change.position
                                        )
                                        activeTouches[change.id.value] = rec
                                    }
                                }

                                // 2. Update motion tracking for active pointers
                                val pressedList = event.changes.filter { it.pressed }
                                for (p in pressedList) {
                                    val rec = activeTouches[p.id.value]
                                    if (rec != null) {
                                        val delta = p.position - rec.lastPos
                                        rec.totalMotion += delta.getDistance()
                                        rec.maxDisplacement = maxOf(rec.maxDisplacement, (p.position - rec.downPos).getDistance())
                                        rec.lastPos = p.position
                                    }
                                }

                                // 3. Multi-touch gesture handling (Pinch-to-zoom)
                                if (pressedList.size >= 2) {
                                    hadMultiTouch = true
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    zoomAccum *= zoomChange
                                    panAccum += panChange
                                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                    val zoomMotion = kotlin.math.abs(1f - zoomAccum) * centroidSize

                                    if (!isPinching) {
                                        if (zoomMotion > pinchMotionThreshold || panAccum.getDistance() > panGestureThreshold) {
                                            isPinching = true
                                            // Cancel pending preview long-presses when pinching begins
                                            activeTouches.values.forEach { rec ->
                                                rec.longPressJob?.cancel()
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
                                    if (rec != null && rec.hasDragged(dragThreshold)) {
                                        if (!isPanning) {
                                            isPanning = true
                                            rec.longPressJob?.cancel()
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

                                        val elapsed = now - rec.downTime
                                        val isDrag = rec.hasDragged(dragThreshold)

                                        // Strict separation of tap from zooming, panning, and dragging:
                                        // A tap ONLY registers if the pointer did not drag/pan, no pinch occurred,
                                        // not holding preview ray, and released within reasonable tap duration.
                                        val isTap = !isPinching && !isPanning && !hadMultiTouch && !isDrag && !rec.isLongPressTriggered && elapsed < 550L

                                        if (isTap) {
                                            // 1. First check the user's intended touch-down location (unaffected by thumb-roll lift drift)
                                            val downLocalPos = screenToLocal(rec.downPos)
                                            val downTarget = findClosestArrow(
                                                downLocalPos,
                                                currentBoardOrigin,
                                                currentArrows,
                                                currentCellSize,
                                                currentHeadSize,
                                                currentMinTouchTargetPx
                                            )

                                            // 2. If downTarget is null (e.g. boundary margin touch), evaluate at tap release centroid
                                            val targetArrow = downTarget ?: run {
                                                val tapScreenPos = Offset(
                                                    (rec.downPos.x + upChange.position.x) / 2f,
                                                    (rec.downPos.y + upChange.position.y) / 2f
                                                )
                                                findClosestArrow(
                                                    screenToLocal(tapScreenPos),
                                                    currentBoardOrigin,
                                                    currentArrows,
                                                    currentCellSize,
                                                    currentHeadSize,
                                                    currentMinTouchTargetPx
                                                )
                                            }

                                            if (targetArrow != null && targetArrow.state != ArrowState.CLEARED && targetArrow.state != ArrowState.FLYING_OUT) {
                                                if (introProgress.value < 1f) {
                                                    coroutineScope.launch { introProgress.snapTo(1f) }
                                                }
                                                currentOnArrowClick(targetArrow)
                                                upChange.consume()
                                            } else if (targetArrow == null && previewRayArrowId != null) {
                                                // Tap on empty space: dismiss the active preview ray
                                                val dummy = currentArrows.firstOrNull()
                                                if (dummy != null) {
                                                    currentOnArrowLongPress(dummy, false)
                                                }
                                            }
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        } finally {
                            activeTouches.values.forEach { rec ->
                                rec.longPressJob?.cancel()
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
            // Directional micro-jolt camera shake on blocked obstacle collision impact
            val flashingArrow = arrows.firstOrNull { it.state == ArrowState.WRONG_FLASH }
            val blockedShakeOffset = if (flashingArrow != null) {
                val t = flashingArrow.flightProgress
                // Peak impact occurs at t = 0.42 when arrow reaches the blocker
                if (t >= 0.38f) {
                    val impactPhase = ((t - 0.38f) / 0.62f).coerceIn(0f, 1f)
                    val decay = (1f - impactPhase).coerceIn(0f, 1f)
                    val shakeMagnitude = 2.6.dp.toPx() * decay
                    val oscillation = sin(impactPhase * 4f * PI.toFloat())
                    val dir = flashingArrow.direction
                    Offset(
                        x = dir.dx * oscillation * shakeMagnitude,
                        y = dir.dy * oscillation * shakeMagnitude
                    )
                } else Offset.Zero
            } else Offset.Zero

            withTransform({
                translate(panX + blockedShakeOffset.x, panY + blockedShakeOffset.y)
                scale(scale * introZoom, scale * introZoom, pivot = center)
            }) {
                // Background is completely transparent; surroundings adapt naturally to theme

                

                // 1. Full Grid Lines (background) only if user activated via '#' Booster button
                if (isGridLinesActive) {
                    drawCorridorGuideLines(boardOrigin, xSize, ySize, cellSize, arrows, theme)

                }

                // 2. Cleared Area Silhouette Dots:
                // Only become visible once an arrow has moved out of the gameplay area.
                // Delicate pinpoints with tactile pop-in matching New Unity One pixel-for-pixel
                if (clearedCells.isNotEmpty()) {
                    val dotColor = theme.gridDot
                    val minDotPx = 0.85.dp.toPx()
                    val maxDotPx = 2.0.dp.toPx()
                    val dotRadius = (cellSize * 0.055f).coerceIn(minDotPx, maxDotPx)
                    val now = System.currentTimeMillis()

                    for (coord in clearedCells) {
                        val spawnTime = clearedCellTimestamps[coord] ?: 0L
                        val elapsed = (now - spawnTime).coerceAtLeast(0L)
                        val animDuration = 220f
                        val animProgress = if (spawnTime == 0L || elapsed >= animDuration) 1.0f else (elapsed / animDuration).coerceIn(0f, 1f)
                        val popScale = when {
                            animProgress >= 1f -> 1.0f
                            animProgress < 0.65f -> (animProgress / 0.65f) * 1.15f
                            else -> 1.15f - ((animProgress - 0.65f) / 0.35f) * 0.15f
                        }
                        val alpha = (animProgress * 0.45f).coerceIn(0f, 0.45f)

                        drawCircle(
                            color = dotColor.copy(alpha = alpha),
                            radius = dotRadius * popScale,
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
                        allArrows = arrows,
                        xSize = xSize,
                        ySize = ySize,
                        cellSize = cellSize,
                        theme = theme,
                        hintPulseScale = hintPulseScale,
                        hintGlowAlpha = hintGlowAlpha,
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
                        text = "â†º",
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
 * Pure Geometric Hit Detection matching both Unity versions (RaycastTry2D & ConfigurableLineFinderStrategy).
 * Evaluates exact physical Euclidean proximity to each arrow's rendered visual shape (arrowhead and stem).
 *
 * Guarantees:
 * 1. The arrow physically closest to the finger ALWAYS wins. Zero distance distortion.
 * 2. Zero "movable stealing": Blocked and movable arrows are treated with absolute geometric fairness.
 *    A blocked arrow tapped by the user will ALWAYS respond with its wobble feedback.
 * 3. Accurate response for all arrow sizes (including 1-cell arrows) without neighbor interference.
 * 4. Taps far away in empty space (> maxSearchRadius) return null, preventing phantom clicks.
 */
private fun findClosestArrow(
    touchPoint: Offset,
    origin: Offset,
    arrows: List<Arrow>,
    cellSize: Float,
    headSize: Float,
    minTouchTargetPx: Float
): Arrow? {
    val activeArrows = arrows.filter { it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT }
    if (activeArrows.isEmpty()) return null

    val flyingArrows = arrows.filter { it.state == ArrowState.FLYING_OUT }

    val headWidth = headSize
    val headLength = headWidth * 1.05f
    val tipForward = (cellSize * 0.32f).coerceAtLeast(headLength * 0.55f)

    // Helper to compute exact physical distance from touchPoint to any arrow's visual geometry
    fun computeArrowDistance(arrow: Arrow): Pair<Float, Boolean> {
        val headCenter = Offset(
            origin.x + (arrow.head.x + 0.5f) * cellSize,
            origin.y + (arrow.head.y + 0.5f) * cellSize
        )
        val dirX = arrow.direction.dx.toFloat()
        val dirY = arrow.direction.dy.toFloat()
        val perpX = -dirY
        val perpY = dirX

        val headTip = Offset(headCenter.x + dirX * tipForward, headCenter.y + dirY * tipForward)
        val wingRight = Offset(
            headTip.x - dirX * headLength + perpX * (headWidth * 0.50f),
            headTip.y - dirY * headLength + perpY * (headWidth * 0.50f)
        )
        val wingLeft = Offset(
            headTip.x - dirX * headLength - perpX * (headWidth * 0.50f),
            headTip.y - dirY * headLength - perpY * (headWidth * 0.50f)
        )

        val isInsideHead = isPointInTriangle(touchPoint, headTip, wingRight, wingLeft)
        val headDist = if (isInsideHead) 0f else minOf(
            distanceToSegment(touchPoint, headCenter, headTip),
            distanceToSegment(touchPoint, headTip, wingLeft),
            distanceToSegment(touchPoint, headTip, wingRight),
            distanceToSegment(touchPoint, wingLeft, wingRight)
        )

        val bodyPts = arrow.allPoints.map { coord ->
            Offset(origin.x + (coord.x + 0.5f) * cellSize, origin.y + (coord.y + 0.5f) * cellSize)
        }
        var stemDist = Float.MAX_VALUE
        if (bodyPts.size == 1) {
            stemDist = (touchPoint - bodyPts[0]).getDistance()
        } else {
            for (i in 0 until bodyPts.size - 1) {
                val d = distanceToSegment(touchPoint, bodyPts[i], bodyPts[i + 1])
                if (d < stemDist) stemDist = d
            }
        }

        return Pair(minOf(headDist, stemDist), isInsideHead)
    }

    // 1. Direct Arrowhead Hit Priority:
    // If the touch lands inside an active arrowhead triangle, select it with top priority!
    for (arrow in activeArrows) {
        val (_, isInsideHead) = computeArrowDistance(arrow)
        if (isInsideHead) {
            return arrow
        }
    }

    // 2. Direct Cell Occupancy Authority (Zero Neighbor Stealing):
    // Every grid cell on the board can belong to at most one active arrow.
    // If the touch point falls within a cell occupied by an active arrow,
    // that arrow is 100% authoritative. An adjacent arrow's tip or stem
    // in a neighboring cell can NEVER steal this tap!
    val relX = touchPoint.x - origin.x
    val relY = touchPoint.y - origin.y
    val cellX = kotlin.math.floor(relX / cellSize).toInt()
    val cellY = kotlin.math.floor(relY / cellSize).toInt()
    val touchedCoord = GridCoordinate(cellX, cellY)

    val cellOwner = activeArrows.firstOrNull { it.isPointInside(touchedCoord) }
    if (cellOwner != null) {
        return cellOwner
    }

    // 3. Flying-Arrow Trajectory Absorption (Fix for multi-tap race condition):
    // If the touched cell belongs to an arrow currently escaping,
    // absorb the tap as a no-op so rapid taps NEVER hit an unintended neighbor!
    if (flyingArrows.any { it.isPointInside(touchedCoord) }) {
        return null
    }

    // 4. True Geometric Proximity Search for Boundary & Margin Touches:
    // Comfortable touch radius: generous enough for clean single-tap response on mobile screens
    val maxTouchRadius = maxOf(cellSize * 0.75f, minTouchTargetPx)

    var bestArrow: Arrow? = null
    var minDistance = Float.MAX_VALUE

    for (arrow in activeArrows) {
        val (dist, _) = computeArrowDistance(arrow)
        if (dist < minDistance) {
            minDistance = dist
            bestArrow = arrow
        }
    }

    return if (minDistance <= maxTouchRadius) bestArrow else null
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

private fun getStepsToBlocker(arrow: Arrow, arrows: List<Arrow>, xSize: Int, ySize: Int): Float {
    val occupiedCoords = mutableSetOf<GridCoordinate>()
    for (a in arrows) {
        if (a.id != arrow.id && a.state != ArrowState.CLEARED && a.state != ArrowState.FLYING_OUT) {
            occupiedCoords.addAll(a.allPoints)
        }
    }
    var steps = 1
    var rayCoord = arrow.head.step(arrow.direction)
    while (rayCoord.x in 0 until xSize && rayCoord.y in 0 until ySize) {
        if (occupiedCoords.contains(rayCoord)) {
            return steps.toFloat()
        }
        steps++
        rayCoord = rayCoord.step(arrow.direction)
    }
    return steps.toFloat().coerceAtLeast(1f)
}

private fun DrawScope.drawArrowEntity(
    origin: Offset,
    arrow: Arrow,
    allArrows: List<Arrow>,
    xSize: Int,
    ySize: Int,
    cellSize: Float,
    theme: ThemePreset,
    hintPulseScale: Float,
    hintGlowAlpha: Float,
    previewRayArrowId: String?,
    drawProgress: Float = 1.0f,
    introAlpha: Float = 1.0f
) {
    val isPreviewHold = (arrow.id == previewRayArrowId)
    val isFlying = (arrow.state == ArrowState.FLYING_OUT)

    val baseColor = when {
        isFlying -> theme.arrowClear
        arrow.state == ArrowState.WRONG_FLASH -> theme.arrowWrong
        arrow.isHighlighted -> theme.arrowWrong
        isPreviewHold -> theme.arrowClear
        arrow.state == ArrowState.HINTED -> theme.arrowHint
        else -> theme.arrowDefault
    }

    // Exact Unity One stroke: slender, crisp lines (20% of cell size, 1.6dp .. 4.5dp)
    // Guarantees generous 3x-4x gap between parallel channels matching Image 2 perfectly
    val minStroke = 2.0.dp.toPx()
    val maxStroke = 5.0.dp.toPx()
    val scaleMultiplier = when {
        isPreviewHold -> 1.10f * (1f + (hintPulseScale - 1f) * 0.35f)
        isFlying -> 1.08f
        arrow.state == ArrowState.WRONG_FLASH -> 1.0f + sin(arrow.flightProgress * PI.toFloat()) * 0.16f
        arrow.state == ArrowState.HINTED -> hintPulseScale
        else -> 1.0f
    }
    val strokeWidth = (cellSize * 0.23f).coerceIn(minStroke, maxStroke) * scaleMultiplier

    // Exact Unity One arrowhead proportions (2.0x stroke width, 1.05x aspect ratio)
    val headWidth = strokeWidth * 2.0f * (if (isPreviewHold) 1.08f else if (isFlying) 1.10f else 1.0f)
    val headLength = headWidth * 1.05f

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

    val stepsToBlocker = if (arrow.collisionDistance > 0f) {
        arrow.collisionDistance
    } else {
        getStepsToBlocker(arrow, allArrows, xSize, ySize)
    }

    val displacement = when (arrow.state) {
        ArrowState.FLYING_OUT -> arrow.flightProgress * maxDisplacement
        ArrowState.WRONG_FLASH -> {
            val t = arrow.flightProgress
            // Moves all the way to the position where another arrow or obstacle blocks its path
            val targetSurge = (stepsToBlocker - 0.12f).coerceAtLeast(0.85f)
            if (t < 0.42f) {
                // Forward travel to collision point
                val u = t / 0.42f
                sin(u * (PI.toFloat() / 2f)) * targetSurge
            } else {
                // Smooth elastic recoil and return to original position
                val p = (t - 0.42f) / 0.58f
                val recoilDecay = (1f - p)
                targetSurge * recoilDecay * kotlin.math.cos(p * (PI.toFloat() / 2f)) -
                    sin(p * 2f * PI.toFloat()) * 0.08f * recoilDecay
            }
        }
        else -> 0f
    }

    val alpha = if (arrow.state == ArrowState.FLYING_OUT && arrow.flightProgress > 0.85f) {
        ((1f - arrow.flightProgress) / 0.15f).coerceIn(0f, 1f)
    } else {
        1.0f
    }

    val visibleBodyLen = if (arrow.state == ArrowState.IDLE && drawProgress < 1f) {
        originalBodyLen * drawProgress
    } else {
        originalBodyLen.toFloat()
    }

    val headDirX = arrow.direction.dx.toFloat()
    val headDirY = arrow.direction.dy.toFloat()
    val headDir = Offset(headDirX, headDirY)

    val tailDir = if (arrow.body.size >= 2) {
        val b0 = arrow.body[0]
        val b1 = arrow.body[1]
        val tdx = (b0.x - b1.x).toFloat()
        val tdy = (b0.y - b1.y).toFloat()
        val len = maxOf(0.001f, kotlin.math.sqrt(tdx * tdx + tdy * tdy))
        Offset(tdx / len, tdy / len)
    } else {
        Offset(-headDirX, -headDirY)
    }

    // Symmetrical head tip and tail reach: both extend by exactly 0.32 * cellSize from cell center
    val tipForward = (cellSize * 0.32f).coerceAtLeast(headLength * 0.55f)
    val tailOffset = (tipForward - strokeWidth * 0.50f).coerceAtLeast(0f)
    // Stem enters slightly past the base into the widest part of the triangle (no gap, no notch, no tip bulb)
    val stemHeadOffset = tipForward - headLength + (strokeWidth * 0.25f)

    val sampledTrackPoints = sampleTrackSegment(
        track = track,
        startDist = displacement,
        endDist = visibleBodyLen + displacement
    )

    if (sampledTrackPoints.isEmpty() && originalBodyLen > 0) return

    // Clean, continuous polyline without backtracking or tip bulb artifacts:
    // First point starts at extended tail; last point terminates inside the wide arrowhead base
    val sampledPoints = mutableListOf<Offset>()
    if (sampledTrackPoints.isNotEmpty()) {
        val firstPt = sampledTrackPoints.first() + tailDir * tailOffset
        val lastPt = sampledTrackPoints.last() + headDir * stemHeadOffset
        sampledPoints.add(firstPt)
        if (sampledTrackPoints.size > 2) {
            for (i in 1 until sampledTrackPoints.size - 1) {
                sampledPoints.add(sampledTrackPoints[i])
            }
        }
        sampledPoints.add(lastPt)
    } else {
        // Single cell arrow: clean line from extended tail to arrowhead base
        sampledPoints.add(headPt + tailDir * tailOffset)
        sampledPoints.add(headPt + headDir * stemHeadOffset)
    }

    val renderColor = baseColor.copy(alpha = alpha * introAlpha)

    // 0. Draw Radiant Multi-Theme Glow Aura when Hinted (supports all themes dynamically)
    if (arrow.state == ArrowState.HINTED && sampledPoints.size > 1) {
        val haloPath = buildRoundedCornerPath(sampledPoints, radius = cellSize * 0.16f)
        // Outer soft radiant aura
        drawPath(
            path = haloPath,
            color = theme.arrowHint.copy(alpha = hintGlowAlpha * 0.35f * introAlpha),
            style = Stroke(
                width = strokeWidth * 2.8f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        // Inner luminous core halo
        drawPath(
            path = haloPath,
            color = theme.arrowHint.copy(alpha = hintGlowAlpha * 0.70f * introAlpha),
            style = Stroke(
                width = strokeWidth * 1.8f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    // 1. Draw body stroke along the polyline with crisp, architectural corner fillets (16% of cell)
    // Seamlessly bridges right into the interior of the arrowhead
    if (sampledPoints.size > 1) {
        val path = buildRoundedCornerPath(sampledPoints, radius = cellSize * 0.16f)
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
    val effectiveHeadWidth = headWidth * headPopScale
    val effectiveHeadLength = headLength * headPopScale

    // 2. Draw clean, crisp Unity One triangular spearhead seamlessly covering the stem extension
    if (effectiveHeadWidth > 0.5f) {
        val currentHead = if (displacement > 0.001f && sampledTrackPoints.isNotEmpty()) sampledTrackPoints.last() else headPt
        rotate(
            degrees = arrow.direction.angleDegrees,
            pivot = currentHead
        ) {
            // Radiant glow for arrowhead when Hinted
            if (arrow.state == ArrowState.HINTED) {
                val glowHeadWidth = effectiveHeadWidth * 1.45f
                val glowHeadLength = effectiveHeadLength * 1.40f
                val glowHeadPath = Path().apply {
                    moveTo(currentHead.x, currentHead.y - tipForward - (glowHeadLength - effectiveHeadLength) * 0.35f)
                    lineTo(currentHead.x + glowHeadWidth * 0.50f, currentHead.y - tipForward + glowHeadLength)
                    lineTo(currentHead.x - glowHeadWidth * 0.50f, currentHead.y - tipForward + glowHeadLength)
                    close()
                }
                drawPath(
                    path = glowHeadPath,
                    color = theme.arrowHint.copy(alpha = hintGlowAlpha * 0.40f * introAlpha)
                )
            }

            val arrowHeadPath = Path().apply {
                // Forward sharp tip
                moveTo(currentHead.x, currentHead.y - tipForward)
                // Right base wing
                lineTo(currentHead.x + effectiveHeadWidth * 0.50f, currentHead.y - tipForward + effectiveHeadLength)
                // Left base wing
                lineTo(currentHead.x - effectiveHeadWidth * 0.50f, currentHead.y - tipForward + effectiveHeadLength)
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
 * Builds a smooth path with circular fillet arcs on every 90-degree turn using quadratic beziers.
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
        val r = minOf(radius, lenIn * 0.30f, lenOut * 0.30f)
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

private fun DrawScope.drawCorridorGuideLines(
    origin: Offset,
    xSize: Int,
    ySize: Int,
    cellSize: Float,
    arrows: List<Arrow>,
    theme: ThemePreset
) {
    val activeArrows = arrows.filter {
        it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT
    }
    if (activeArrows.isEmpty()) return

    // Directional corridor extraction:
    // Group active arrows by direction so lines ONLY extend forward in the direction
    // each arrow is pointing, without displaying the opposite backwards line.
    val upCorridors = mutableMapOf<Int, Float>() // col -> maxTailY
    val downCorridors = mutableMapOf<Int, Float>() // col -> minTailY
    val leftCorridors = mutableMapOf<Int, Float>() // row -> maxTailX
    val rightCorridors = mutableMapOf<Int, Float>() // row -> minTailX

    for (arrow in activeArrows) {
        val headCol = arrow.head.x
        val headRow = arrow.head.y

        when (arrow.direction) {
            Direction.UP -> {
                var tailY = headRow
                while (arrow.allPoints.contains(GridCoordinate(headCol, tailY + 1))) {
                    tailY++
                }
                val curMax = upCorridors[headCol] ?: Float.NEGATIVE_INFINITY
                upCorridors[headCol] = maxOf(curMax, tailY.toFloat())
            }
            Direction.DOWN -> {
                var tailY = headRow
                while (arrow.allPoints.contains(GridCoordinate(headCol, tailY - 1))) {
                    tailY--
                }
                val curMin = downCorridors[headCol] ?: Float.POSITIVE_INFINITY
                downCorridors[headCol] = minOf(curMin, tailY.toFloat())
            }
            Direction.LEFT -> {
                var tailX = headCol
                while (arrow.allPoints.contains(GridCoordinate(tailX + 1, headRow))) {
                    tailX++
                }
                val curMax = leftCorridors[headRow] ?: Float.NEGATIVE_INFINITY
                leftCorridors[headRow] = maxOf(curMax, tailX.toFloat())
            }
            Direction.RIGHT -> {
                var tailX = headCol
                while (arrow.allPoints.contains(GridCoordinate(tailX - 1, headRow))) {
                    tailX--
                }
                val curMin = rightCorridors[headRow] ?: Float.POSITIVE_INFINITY
                rightCorridors[headRow] = minOf(curMin, tailX.toFloat())
            }
        }
    }

    val fadeMargin = cellSize * 2.0f
    val boardLeft = origin.x
    val boardRight = origin.x + xSize * cellSize
    val boardTop = origin.y
    val boardBottom = origin.y + ySize * cellSize

    // Slightly increased thickness for clear visibility while remaining subtle
    val strokeW = (cellSize * 0.046f).coerceIn(1.5.dp.toPx(), 2.4.dp.toPx())
    val guideColor = theme.arrowDefault.copy(alpha = if (theme.isDark) 0.38f else 0.28f)

    // 1. UP Corridors: line extends upwards from arrow body to top margin
    for ((col, maxTailY) in upCorridors) {
        val posX = origin.x + (col + 0.5f) * cellSize
        val startY = boardTop - fadeMargin
        val endY = origin.y + (maxTailY + 0.5f) * cellSize
        val totalLength = (endY - startY).coerceAtLeast(1f)
        val fadeRatio = (fadeMargin / totalLength).coerceIn(0.01f, 0.45f)

        val brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.Transparent,
                fadeRatio to guideColor,
                1.0f to guideColor
            ),
            startY = startY,
            endY = endY
        )
        drawLine(
            brush = brush,
            start = Offset(posX, startY),
            end = Offset(posX, endY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }

    // 2. DOWN Corridors: line extends downwards from arrow body to bottom margin
    for ((col, minTailY) in downCorridors) {
        val posX = origin.x + (col + 0.5f) * cellSize
        val startY = origin.y + (minTailY + 0.5f) * cellSize
        val endY = boardBottom + fadeMargin
        val totalLength = (endY - startY).coerceAtLeast(1f)
        val solidLength = (boardBottom - startY).coerceAtLeast(0f)
        val solidRatio = (solidLength / totalLength).coerceIn(0.55f, 0.99f)

        val brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to guideColor,
                solidRatio to guideColor,
                1.0f to Color.Transparent
            ),
            startY = startY,
            endY = endY
        )
        drawLine(
            brush = brush,
            start = Offset(posX, startY),
            end = Offset(posX, endY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }

    // 3. LEFT Corridors: line extends leftwards from arrow body to left margin
    for ((row, maxTailX) in leftCorridors) {
        val posY = origin.y + (row + 0.5f) * cellSize
        val startX = boardLeft - fadeMargin
        val endX = origin.x + (maxTailX + 0.5f) * cellSize
        val totalLength = (endX - startX).coerceAtLeast(1f)
        val fadeRatio = (fadeMargin / totalLength).coerceIn(0.01f, 0.45f)

        val brush = Brush.horizontalGradient(
            colorStops = arrayOf(
                0.0f to Color.Transparent,
                fadeRatio to guideColor,
                1.0f to guideColor
            ),
            startX = startX,
            endX = endX
        )
        drawLine(
            brush = brush,
            start = Offset(startX, posY),
            end = Offset(endX, posY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }

    // 4. RIGHT Corridors: line extends rightwards from arrow body to right margin
    for ((row, minTailX) in rightCorridors) {
        val posY = origin.y + (row + 0.5f) * cellSize
        val startX = origin.x + (minTailX + 0.5f) * cellSize
        val endX = boardRight + fadeMargin
        val totalLength = (endX - startX).coerceAtLeast(1f)
        val solidLength = (boardRight - startX).coerceAtLeast(0f)
        val solidRatio = (solidLength / totalLength).coerceIn(0.55f, 0.99f)

        val brush = Brush.horizontalGradient(
            colorStops = arrayOf(
                0.0f to guideColor,
                solidRatio to guideColor,
                1.0f to Color.Transparent
            ),
            startX = startX,
            endX = endX
        )
        drawLine(
            brush = brush,
            start = Offset(startX, posY),
            end = Offset(endX, posY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
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
