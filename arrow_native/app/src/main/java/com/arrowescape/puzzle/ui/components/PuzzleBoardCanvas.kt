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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import com.arrowescape.puzzle.game.BoosterEffectAnimation
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
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
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
    var longPressJob: Job? = null,
    var candidateArrow: Arrow? = null
) {
    fun hasDragged(threshold: Float): Boolean = maxDisplacement > threshold
}


@Composable
fun PuzzleBoardCanvas(
    xSize: Int,
    ySize: Int,
    arrows: List<Arrow>,
    theme: ThemePreset,
    isGridLinesActive: Boolean,
    isCelebrationBurstActive: Boolean = false,
    restartTrigger: Long = 0L,
    previewRayArrowIds: Set<String> = emptySet(),
    activeBoosterAnimation: BoosterEffectAnimation? = null,
    currentLevelNumber: Int = 0,
    isDailyChallengeMode: Boolean = false,
    showTutorialHand: Boolean = (currentLevelNumber == 1 && !isDailyChallengeMode),
    onArrowClick: (Arrow) -> Unit,
    onArrowLongPress: (Arrow, isPressed: Boolean) -> Unit,
    onDismissPreviewRays: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Smooth scale and pan states (starts initially zoomed in at 1.38f entry scale)
    var scale by remember { mutableFloatStateOf(1.38f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var panInertiaJob by remember { mutableStateOf<Job?>(null) }
    var introZoomJob by remember { mutableStateOf<Job?>(null) }

    // Remember the initial level silhouette cells to render subtle cell dots for cleared areas
    val initialLevelCells = remember(xSize, ySize, arrows.size, arrows.firstOrNull()?.id, restartTrigger) {
        arrows.flatMap { it.allPoints }.toSet()
    }
    val clearedCellTimestamps = remember(xSize, ySize, arrows.firstOrNull()?.id, restartTrigger) { mutableMapOf<GridCoordinate, Long>() }
    val clearedCells = remember(arrows, initialLevelCells) {
        // Only show dots for truly CLEARED arrows.
        // FLYING_OUT arrows are still "in transit" on screen — their cells must NOT show dots yet.
        val activeCoords = arrows
            .filter { it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT }
            .flatMap { it.allPoints }.toSet()
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

    // Cascading write-on path assembly animation progress
    val introProgress = remember { Animatable(0f) }

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


    // Filter active arrows selected for simultaneous path preview
    val activePreviewArrows = remember(arrows, previewRayArrowIds) {
        arrows.filter { it.id in previewRayArrowIds && it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT }
    }

    // Smooth animated preview beam alpha matching Unity's CanvasGroup fade (lineCg.DOFade)
    val previewAlpha by animateFloatAsState(
        targetValue = if (activePreviewArrows.isNotEmpty()) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "previewAlpha"
    )

    // Viewport-wide canvas: adapts flexibly to full available gameplay height and width
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val viewportWidth = constraints.maxWidth.toFloat()
        val viewportHeight = constraints.maxHeight.toFloat()

        // Fit board into viewport with comfortable margin matching Old Unity (88% width, 78% height)
        val availableWidth = viewportWidth * 0.88f
        val availableHeight = viewportHeight * 0.78f
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

        // Default overview fit scale is strictly 1.0f (full puzzle visible) matching New Unity
        val density = LocalDensity.current
        val defaultScale = 1.0f
        val currentDefaultScale by rememberUpdatedState(defaultScale)

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
        val currentOnDismissPreviewRays by rememberUpdatedState(onDismissPreviewRays)
        val minTouchTargetPx = with(LocalDensity.current) { 15.dp.toPx() }
        val currentMinTouchTargetPx by rememberUpdatedState(minTouchTargetPx)
        val minStrokePx = with(LocalDensity.current) { 2.0.dp.toPx() }
        val maxStrokePx = with(LocalDensity.current) { 5.5.dp.toPx() }
        val baseStrokeWidth = (cellSize * 0.24f).coerceIn(minStrokePx, maxStrokePx)
        val baseHeadWidth = baseStrokeWidth * 2.05f
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
                panInertiaJob?.cancel()
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
                panInertiaJob = coroutineScope.launch {
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

        // Initial Render & Restart Zoom Behavior (Matching New Unity implementation):
        // 1. When the puzzle loads or restarts, it initially appears ZOOMED IN at entryScale (1.38f)
        // 2. Camera smoothly ZOOMS OUT to defaultScale (1.0f) over 850ms with FastOutSlowInEasing
        // 3. Arrow rendering animation runs across 1450ms (concurrent with camera zoom-out)
        LaunchedEffect(currentLevelNumber, restartTrigger) {
            panInertiaJob?.cancel()
            panX = 0f
            panY = 0f
            scale = 1.38f
            introProgress.snapTo(0f)

            // Step 1: Smoothly zoom OUT from zoomed-in entry scale (1.38f) to default fit scale (1.0f)
            introZoomJob = launch {
                val sAnim = Animatable(1.38f)
                sAnim.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
                ) {
                    scale = value
                }
            }

            // Step 2: Concurrently render all arrows at full smoothness (1450ms, untouched)
            launch {
                introProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1450, easing = LinearEasing)
                )
            }
        }

        var animTick by remember { mutableLongStateOf(0L) }
        val celebrationWaveProgress = remember { Animatable(0f) }

        // Level Completion Animation:
        // 1. MoveGridToCenterAndResetScale: Camera smoothly zooms OUT to 1.0f and centers the complete puzzle
        // 2. AnimateDots_CenterOut: Circular wave ripples outward from center across all dots
        LaunchedEffect(isCelebrationBurstActive) {
            if (isCelebrationBurstActive) {
                panInertiaJob?.cancel()
                introZoomJob?.cancel()
                launch {
                    val sAnim = Animatable(scale)
                    sAnim.animateTo(
                        targetValue = 1.0f,
                        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                    ) { scale = value }
                }
                launch {
                    val pxAnim = Animatable(panX)
                    pxAnim.animateTo(0f, tween(durationMillis = 650, easing = FastOutSlowInEasing)) { panX = value }
                }
                launch {
                    val pyAnim = Animatable(panY)
                    pyAnim.animateTo(0f, tween(durationMillis = 650, easing = FastOutSlowInEasing)) { panY = value }
                }
                // Dot ripple wave spreading outward
                launch {
                    celebrationWaveProgress.snapTo(0f)
                    celebrationWaveProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 850, easing = LinearEasing)
                    )
                }
            } else {
                celebrationWaveProgress.snapTo(0f)
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

        // Build occupancy map live from the current arrows, excluding FLYING_OUT and CLEARED arrows.
        val occupancyMap = GridOccupancyMap(xSize, ySize).apply {
            rebuild(arrows.filter { it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT })
        }
        val currentOccupancyMap by rememberUpdatedState(occupancyMap)

        val hasUnclearedArrows = arrows.any { it.state != ArrowState.CLEARED }

        // Tutorial hand indicator: active strictly during Level 1 until all arrows are cleared
        val tutorialTargetArrow = remember(arrows, currentLevelNumber, isDailyChallengeMode, showTutorialHand, occupancyMap) {
            if (showTutorialHand && currentLevelNumber == 1 && !isDailyChallengeMode) {
                arrows.firstOrNull { it.state == ArrowState.IDLE && PuzzleSolver.canMoveOut(it, occupancyMap) }
                    ?: arrows.firstOrNull { it.state == ArrowState.IDLE }
            } else null
        }

        LaunchedEffect(isCelebrationBurstActive, celebrationWaveProgress.isRunning, activeBoosterAnimation, showTutorialHand, hasUnclearedArrows) {
            while (isCelebrationBurstActive || celebrationWaveProgress.isRunning || activeBoosterAnimation != null || (showTutorialHand && hasUnclearedArrows)) {
                withFrameNanos { frameTimeNanos ->
                    animTick = frameTimeNanos
                }
            }
        }

        // Board scale is held strictly at 1.0f during initial arrow rendering overview
        val introZoom = 1.0f
        val currentIntroZoom by rememberUpdatedState(introZoom)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds() // Prevents drawing over top bar or bottom booster bar
                .pointerInput(xSize, ySize) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        panInertiaJob?.cancel() // Stop ongoing pan inertia immediately on new touch

                        val center = Offset(currentViewportWidth / 2f, currentViewportHeight / 2f)
                        val dragThreshold = with(density) { 10.dp.toPx() }
                        val pinchMotionThreshold = with(density) { 20.dp.toPx() }
                        val panGestureThreshold = with(density) { 14.dp.toPx() }

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

                        // Preview ray on long-press (holding down on an arrow without dragging)
                        val downArrow = findClosestArrow(
                            screenToLocal(down.position),
                            currentBoardOrigin,
                            currentArrows,
                            currentCellSize,
                            currentHeadSize,
                            currentMinTouchTargetPx,
                            currentOccupancyMap,
                            currentClearedCells
                        )
                        val firstRecord = TouchRecord(
                            downPos = down.position,
                            downTime = System.currentTimeMillis(),
                            lastPos = down.position,
                            candidateArrow = downArrow
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
                                        val cand = findClosestArrow(
                                            screenToLocal(change.position),
                                            currentBoardOrigin,
                                            currentArrows,
                                            currentCellSize,
                                            currentHeadSize,
                                            currentMinTouchTargetPx,
                                            currentOccupancyMap,
                                            currentClearedCells
                                        )
                                        val rec = TouchRecord(
                                            downPos = change.position,
                                            downTime = now,
                                            lastPos = change.position,
                                            candidateArrow = cand
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
                                        introZoomJob?.cancel()
                                        panInertiaJob?.cancel()
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
                                } else if (pressedList.size == 1) {
                                    // Single-finger dragging and repositioning in any direction at any scale (matching Unity)
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
                                        val maxPanX = (currentBoardWidth * scale / 2f) + (currentViewportWidth * 0.45f)
                                        val maxPanY = (currentBoardHeight * scale / 2f) + (currentViewportHeight * 0.45f)

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
                                        // A tap registers if pointer did not drag/pan, no pinch occurred,
                                        // not holding preview ray, and released within reasonable tap duration.
                                        val isTap = !isPinching && !isPanning && !isDrag && !rec.isLongPressTriggered && elapsed < 650L

                                        if (isTap) {
                                            // Down-target: the arrow identified at touch-DOWN time.
                                            // We intentionally allow WRONG_FLASH here — the VM will snap it to IDLE.
                                            // We only exclude CLEARED since those truly no longer exist on the board.
                                            val downTarget = rec.candidateArrow?.let { cand ->
                                                currentArrows.firstOrNull { it.id == cand.id && it.state != ArrowState.CLEARED }
                                            }

                                            val upTarget = findClosestArrow(
                                                screenToLocal(upChange.position),
                                                currentBoardOrigin,
                                                currentArrows,
                                                currentCellSize,
                                                currentHeadSize,
                                                currentMinTouchTargetPx,
                                                currentOccupancyMap,
                                                currentClearedCells
                                            )

                                            // Down-Target Authority with Release & Midpoint Fallback:
                                            // 1. If pointer hit an arrow at touch-down, use that arrow (prevents thumb roll drift).
                                            // 2. If touch-down was in empty margin/gap, use release position (upTarget).
                                            // 3. Fallback to midpoint if finger tapped quickly in small margin.
                                            val targetArrow = downTarget ?: upTarget ?: run {
                                                val midScreenPos = Offset(
                                                    (rec.downPos.x + upChange.position.x) / 2f,
                                                    (rec.downPos.y + upChange.position.y) / 2f
                                                )
                                                findClosestArrow(
                                                    screenToLocal(midScreenPos),
                                                    currentBoardOrigin,
                                                    currentArrows,
                                                    currentCellSize,
                                                    currentHeadSize,
                                                    currentMinTouchTargetPx,
                                                    currentOccupancyMap,
                                                    currentClearedCells
                                                )
                                            }

                                            // Only exclude truly CLEARED arrows. FLYING_OUT is handled by the VM guard.
                                            // This prevents silent tap drops during the brief race between touch-up
                                            // and state transition to FLYING_OUT.
                                            if (targetArrow != null && targetArrow.state != ArrowState.CLEARED) {
                                                if (introProgress.value < 1f) {
                                                    coroutineScope.launch { introProgress.snapTo(1f) }
                                                }
                                                currentOnArrowClick(targetArrow)
                                                upChange.consume()
                                            } else if (targetArrow == null && previewRayArrowIds.isNotEmpty()) {
                                                // Tap on empty space: dismiss all active preview rays
                                                currentOnDismissPreviewRays()
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
                            panInertiaJob = coroutineScope.launch {
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
                        } else if (inertiaVel != Offset.Zero) {
                            // Inertia momentum coasting with friction decay (matching Unity's SwipeInertia)
                            panInertiaJob = coroutineScope.launch {
                                var vx = inertiaVel.x * 0.75f
                                var vy = inertiaVel.y * 0.75f
                                val maxPX = (currentBoardWidth * scale / 2f) + (currentViewportWidth * 0.45f)
                                val maxPY = (currentBoardHeight * scale / 2f) + (currentViewportHeight * 0.45f)
                                while (hypot(vx, vy) > 25f) {
                                    val curPX = panX + vx * 0.016f
                                    val curPY = panY + vy * 0.016f

                                    if (curPX !in -maxPX..maxPX) vx = -vx * 0.25f
                                    if (curPY !in -maxPY..maxPY) vy = -vy * 0.25f

                                    panX = curPX.coerceIn(-maxPX, maxPX)
                                    panY = curPY.coerceIn(-maxPY, maxPY)

                                    vx *= 0.92f
                                    vy *= 0.92f
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

                // 2. Grid Alignment & Silhouette Dots:
                // Subtle architectural grid dots across the board eliminate "random blank gaps" and unify the UI.
                // On level completion, dots ripple outward in a brilliant celebration wave (AnimateDots_CenterOut).
                val dotColor = theme.gridDot
                val dotRadius = (cellSize * 0.052f).coerceIn(0.85.dp.toPx(), 2.2.dp.toPx())
                val now = System.currentTimeMillis()
                val isWaveActive = celebrationWaveProgress.value > 0f

                val dotsCenterX = (xSize - 1) / 2f
                val dotsCenterY = (ySize - 1) / 2f
                val maxDotsDist = hypot(dotsCenterX, dotsCenterY).coerceAtLeast(1f)

                for (cy in 0 until ySize) {
                    for (cx in 0 until xSize) {
                        val coord = GridCoordinate(cx, cy)
                        val isCleared = clearedCells.contains(coord)

                        if (isCleared) {
                            val dotCenter = Offset(
                                boardOrigin.x + (cx + 0.5f) * cellSize,
                                boardOrigin.y + (cy + 0.5f) * cellSize
                            )

                            if (isWaveActive) {
                                // AnimateDots_CenterOut: circular ripple wave spreading from center outward
                                val dotDist = hypot(cx - dotsCenterX, cy - dotsCenterY)
                                val normDist = (dotDist / maxDotsDist).coerceIn(0f, 1f)
                                val waveProg = celebrationWaveProgress.value
                                val dotHitTime = normDist * 0.65f
                                val dotLocalProg = ((waveProg - dotHitTime) / 0.35f).coerceIn(0f, 1f)

                                if (dotLocalProg in 0.001f..0.999f) {
                                    val waveCurve = sin(dotLocalProg * PI.toFloat())
                                    val popScale = 1.0f + 1.25f * waveCurve
                                    val waveColor = Color(0xFFFFB800) // Golden celebration highlight

                                    // Radiant halo
                                    drawCircle(
                                        color = waveColor.copy(alpha = waveCurve * 0.35f),
                                        radius = dotRadius * (1.2f + 2.4f * waveCurve),
                                        center = dotCenter
                                    )
                                    // Golden popping dot
                                    drawCircle(
                                        color = androidx.compose.ui.graphics.lerp(dotColor.copy(alpha = 0.45f), waveColor, waveCurve),
                                        radius = dotRadius * popScale,
                                        center = dotCenter
                                    )
                                } else {
                                    drawCircle(
                                        color = dotColor.copy(alpha = 0.45f),
                                        radius = dotRadius,
                                        center = dotCenter
                                    )
                                }
                            } else {
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
                                    center = dotCenter
                                )
                            }
                        }
                    }
                }

                // 3. Draw Preview Rays for all active preview arrows simultaneously (smooth animated fade matching Unity lineCg.DOFade)
                if (activePreviewArrows.isNotEmpty() && previewAlpha > 0.01f) {
                    for (previewArrow in activePreviewArrows) {
                        drawPreviewRay(boardOrigin, previewArrow, xSize, ySize, cellSize, previewAlpha, theme.arrowClear)
                    }
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
                    val startTime = normDist * 0.40f
                    val drawWindow = 0.58f
                    val localProg = if (introProgress.value >= 1f) 1f else ((introProgress.value - startTime) / drawWindow).coerceIn(0f, 1f)

                    val easedDraw = if (localProg >= 1f) 1f else CubicBezierEasing(0.35f, 0.0f, 0.25f, 1.0f).transform(localProg)
                    val introAlpha = if (localProg >= 1f) 1f else (localProg / 0.12f).coerceIn(0f, 1f)

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
                        previewRayArrowIds = previewRayArrowIds,
                        activeBoosterAnimation = activeBoosterAnimation,
                        drawProgress = easedDraw,
                        introAlpha = introAlpha
                    )
                }

                // 4B. Active Booster FX Animation (Hammer Smash & Shatter, Magic Wand Stardust Dissolution)
                if (activeBoosterAnimation != null) {
                    drawBoosterAnimation(boardOrigin, activeBoosterAnimation, cellSize, theme)
                }

                // 4C. Interactive Tutorial Guidance Hand (Level 1 only, matching Unity)
                if (showTutorialHand && tutorialTargetArrow != null && introProgress.value >= 1f) {
                    drawTutorialGuidance(boardOrigin, tutorialTargetArrow, cellSize, theme)
                }

            }
        }

        // Floating Reset Zoom pill button shown whenever zoomed in or panned away from default
        val isZoomedOrPanned = kotlin.math.abs(scale - defaultScale) > 0.08f || kotlin.math.abs(panX) > 20f || kotlin.math.abs(panY) > 20f
        if (!isCelebrationBurstActive && isZoomedOrPanned && introProgress.value >= 1f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clickable {
                        panInertiaJob?.cancel()
                        panInertiaJob = coroutineScope.launch {
                            launch {
                                val sAnim = Animatable(scale)
                                sAnim.animateTo(defaultScale, tween(260, easing = FastOutSlowInEasing)) {
                                    scale = value
                                }
                            }
                            launch {
                                val pxAnim = Animatable(panX)
                                pxAnim.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) {
                                    panX = value
                                }
                            }
                            launch {
                                val pyAnim = Animatable(panY)
                                pyAnim.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) {
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
                        text = "\u21BA",
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

        // Prominent "Tap to move" banner positioned directly above the puzzle grid with a dynamic responsive gap
        if (showTutorialHand && currentLevelNumber == 1 && !isDailyChallengeMode && hasUnclearedArrows && introProgress.value >= 1f) {
            val density = LocalDensity.current
            val dynamicGapPx = with(density) {
                (boardHeight * 0.20f).coerceIn(28.dp.toPx(), 46.dp.toPx())
            }
            val pillHeightPx = with(density) { 52.dp.toPx() }
            val pillTopPx = (boardOrigin.y - dynamicGapPx - pillHeightPx).coerceAtLeast(with(density) { 24.dp.toPx() })
            val pillTopDp = with(density) { pillTopPx.toDp() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = pillTopDp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = if (theme.isDark) Color(0xFF334155).copy(alpha = 0.88f) else Color(0xFFDCE4F0).copy(alpha = 0.88f),
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = "Tap to move",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (theme.isDark) Color.White else Color(0xFF1E293B),
                        modifier = Modifier.padding(horizontal = 42.dp, vertical = 13.dp),
                        textAlign = TextAlign.Center
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
    minTouchTargetPx: Float,
    occupancy: GridOccupancyMap? = null,
    clearedCells: Set<GridCoordinate> = emptySet()
): Arrow? {
    val activeArrows = arrows.filter { it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT }
    if (activeArrows.isEmpty()) return null

    val strokeWidth = (cellSize * 0.20f).coerceIn(1.0f, cellSize * 0.35f)
    val headWidth = (cellSize * 0.54f).coerceAtLeast(strokeWidth * 1.8f).coerceAtMost(cellSize * 0.68f)
    val headLength = headWidth * 0.88f
    val tipForward = cellSize * 0.35f

    // 1. Direct Arrowhead Hit Priority:
    // If the touch lands inside an active arrowhead triangle, select it with top priority!
    for (arrow in activeArrows) {
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

        if (isPointInTriangle(touchPoint, headTip, wingRight, wingLeft)) {
            return arrow
        }
    }

    // 2. DISCRETE CELL OCCUPANCY AUTHORITY (100% Infallible Hit Testing)
    // In Unity, each arrow collider occupies its respective grid cells.
    // If the tap lands directly inside a cell owned by an active (non-flying) arrow, that arrow wins.
    // Flying arrows are already excluded from activeArrows above, so they cannot steal taps here.
    val relX = touchPoint.x - origin.x
    val relY = touchPoint.y - origin.y
    val cellX = kotlin.math.floor(relX / cellSize).toInt()
    val cellY = kotlin.math.floor(relY / cellSize).toInt()
    val touchedCoord = GridCoordinate(cellX, cellY)

    val cellOccupant = occupancy?.get(touchedCoord)?.takeIf {
        it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT
    } ?: activeArrows.firstOrNull { it.isPointInside(touchedCoord) }

    if (cellOccupant != null) {
        return cellOccupant
    }
    // NOTE: Phase 3 (flying-cell absorption guard) has been removed.
    // Flying arrows are already excluded from hit detection via Phase 1 and Phase 2.
    // The old guard was incorrectly absorbing taps on neighboring arrows during flight.

    // 4. Proximity Search for Taps Near Arrows, in Margins, or Slightly Across Cell Borders:
    // Comfortable search radius: ensures single-tap responsiveness across all grid sizes
    val maxSearchRadius = maxOf(cellSize * 0.90f, minTouchTargetPx)

    fun computeArrowDistance(arrow: Arrow): Float {
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

        val headDist = minOf(
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

        return minOf(headDist, stemDist)
    }

    data class CandidateHit(val arrow: Arrow, val dist: Float, val isMovable: Boolean)
    val candidates = activeArrows.mapNotNull { arrow ->
        val dist = computeArrowDistance(arrow)
        if (dist <= maxSearchRadius) {
            val isMovable = occupancy != null && PuzzleSolver.canMoveOut(arrow, occupancy)
            CandidateHit(arrow, dist, isMovable)
        } else null
    }

    if (candidates.isEmpty()) return null

    val sorted = candidates.sortedBy { it.dist }
    val best = sorted.first()

    // Tie-breaker: If two arrows are within 3.5px of each other, favor the movable arrow
    val tieCandidates = sorted.filter { it.dist <= best.dist + 3.5f }
    return tieCandidates.firstOrNull { it.isMovable }?.arrow ?: best.arrow
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
    previewRayArrowIds: Set<String>,
    activeBoosterAnimation: BoosterEffectAnimation? = null,
    drawProgress: Float = 1.0f,
    introAlpha: Float = 1.0f
) {
    // If shattered by hammer, hide standard arrow geometry during explosion
    if (activeBoosterAnimation is BoosterEffectAnimation.Hammer && activeBoosterAnimation.targetArrow.id == arrow.id && activeBoosterAnimation.progress >= 0.40f) {
        return
    }

    val isPreviewHold = arrow.id in previewRayArrowIds
    val isFlying = (arrow.state == ArrowState.FLYING_OUT)

    val baseColor = when {
        isFlying -> theme.arrowClear
        arrow.state == ArrowState.WRONG_FLASH -> theme.arrowWrong
        arrow.isHighlighted -> theme.arrowWrong
        isPreviewHold -> theme.arrowClear
        arrow.state == ArrowState.HINTED -> theme.arrowHint
        else -> theme.arrowDefault
    }

    // Exact Unity One stroke: slender, crisp lines (24% of cell size, bounded for all grid scales)
    // Guarantees generous gap between parallel channels matching Image 2 perfectly
    val minStroke = 2.0.dp.toPx()
    val maxStroke = 5.5.dp.toPx()
    val scaleMultiplier = when {
        isPreviewHold -> 1.0f
        isFlying -> 1.08f
        arrow.state == ArrowState.WRONG_FLASH -> 1.0f + sin(arrow.flightProgress * PI.toFloat()) * 0.16f
        arrow.state == ArrowState.HINTED -> 1.0f
        else -> 1.0f
    }
    val strokeWidth = (cellSize * 0.24f).coerceIn(minStroke, maxStroke) * scaleMultiplier

    // Exact Unity One arrowhead proportions bounded strictly within the cell (never overlaps neighbor cells)
    val headWidth = (cellSize * 0.52f).coerceAtLeast(strokeWidth * 1.9f).coerceAtMost(cellSize * 0.68f) *
        (if (isPreviewHold) 1.08f else if (isFlying) 1.10f else 1.0f)
    val headLength = headWidth * 0.90f

    // Build the full motion track: body points + escape extension along arrow.direction
    val track = mutableListOf<Offset>()
    for (coord in arrow.body) {
        track.add(Offset(origin.x + (coord.x + 0.5f) * cellSize, origin.y + (coord.y + 0.5f) * cellSize))
    }
    val headPt = track.last()
    val extSteps = maxOf(xSize, ySize) * 2 + arrow.body.size + 12
    for (s in 1..extSteps) {
        track.add(
            Offset(
                headPt.x + arrow.direction.dx * s * cellSize,
                headPt.y + arrow.direction.dy * s * cellSize
            )
        )
    }

    val originalBodyLen = (arrow.body.size - 1).coerceAtLeast(0)

    // Smooth exit flight distance: calculates distance to board edge plus full arrow clearance
    val distToEdge = when (arrow.direction) {
        Direction.UP -> (arrow.head.y + 1).toFloat()
        Direction.DOWN -> (ySize - arrow.head.y).toFloat()
        Direction.LEFT -> (arrow.head.x + 1).toFloat()
        Direction.RIGHT -> (xSize - arrow.head.x).toFloat()
    }
    val exitDistance = (distToEdge + arrow.body.size + 3.0f).coerceAtLeast(6f)

    val stepsToBlocker = if (arrow.collisionDistance > 0f) {
        arrow.collisionDistance
    } else {
        getStepsToBlocker(arrow, allArrows, xSize, ySize)
    }

    val displacement = when (arrow.state) {
        ArrowState.FLYING_OUT -> arrow.flightProgress * exitDistance
        ArrowState.WRONG_FLASH -> {
            val t = arrow.flightProgress
            // Moves all the way to the position where another arrow or obstacle blocks its path
            val targetSurge = (stepsToBlocker - 0.32f).coerceAtLeast(0.65f)
            if (t < 0.40f) {
                // Forward travel to collision point
                val u = t / 0.40f
                sin(u * (PI.toFloat() / 2f)) * targetSurge
            } else {
                // Smooth elastic recoil and return to original position
                val p = (t - 0.40f) / 0.60f
                val recoilDecay = (1f - p)
                targetSurge * recoilDecay * kotlin.math.cos(p * (PI.toFloat() / 2f)) -
                    sin(p * 2f * PI.toFloat()) * 0.08f * recoilDecay
            }
        }
        else -> 0f
    }

    // Keep arrow solid during flight across grid; subtle fade-out at very end when off-board
    val alpha = if (arrow.state == ArrowState.FLYING_OUT && arrow.flightProgress > 0.94f) {
        ((1f - arrow.flightProgress) / 0.06f).coerceIn(0f, 1f)
    } else {
        1.0f
    }

    val visibleBodyLen = if (arrow.state == ArrowState.IDLE && drawProgress < 1f) {
        originalBodyLen * drawProgress
    } else {
        originalBodyLen.toFloat()
    }

    // Symmetrical head tip and tail reach: both extend cleanly from cell center
    val tipForward = (cellSize * 0.35f)
    val tailOffset = (cellSize * 0.32f).coerceAtLeast(strokeWidth * 0.45f)
    // Stem enters smoothly into the interior of the triangular arrowhead base
    val stemHeadOffset = (tipForward - headLength * 0.65f).coerceAtLeast(0f)

    val startDist = displacement
    val endDist = visibleBodyLen + displacement

    val sampledTrackPoints = sampleTrackSegment(
        track = track,
        startDist = startDist,
        endDist = endDist
    )

    if (sampledTrackPoints.isEmpty()) return

    // Dynamically compute the tangent vector along track at the sampled tail position:
    // Guarantees the tail slides smoothly along the path and bends around turns without anchoring to old starting point
    val sIdx = startDist.toInt().coerceIn(0, track.size - 2)
    val sVec = track[sIdx + 1] - track[sIdx]
    val sLen = hypot(sVec.x, sVec.y).coerceAtLeast(0.001f)
    val forwardAtTail = Offset(sVec.x / sLen, sVec.y / sLen)
    val dynamicTailDir = Offset(-forwardAtTail.x, -forwardAtTail.y)

    // Dynamically compute the tangent vector along track at the sampled head position
    val eIdx = endDist.toInt().coerceIn(0, track.size - 2)
    val eVec = track[eIdx + 1] - track[eIdx]
    val eLen = hypot(eVec.x, eVec.y).coerceAtLeast(0.001f)
    val forwardAtHead = Offset(eVec.x / eLen, eVec.y / eLen)

    // Clean, continuous polyline without backtracking, tail blending or tip bulb artifacts:
    // First point starts at extended tail; last point terminates inside the wide arrowhead base
    val sampledPoints = mutableListOf<Offset>()
    if (originalBodyLen == 0) {
        // Single cell arrow: solid unified line from extended tail to arrowhead stem base
        val centerPt = sampledTrackPoints.first()
        val firstPt = centerPt + dynamicTailDir * tailOffset
        val fullLastPt = centerPt + forwardAtHead * stemHeadOffset
        val targetLastPt = if (arrow.state == ArrowState.IDLE && drawProgress < 1f) {
            firstPt + (fullLastPt - firstPt) * drawProgress
        } else {
            fullLastPt
        }
        sampledPoints.add(firstPt)
        sampledPoints.add(targetLastPt)
    } else {
        // Multi-cell arrow: clean continuous polyline through all sampled track points
        val firstPt = sampledTrackPoints.first() + dynamicTailDir * tailOffset
        val lastPt = sampledTrackPoints.last() + forwardAtHead * stemHeadOffset
        sampledPoints.add(firstPt)
        if (sampledTrackPoints.size > 2) {
            for (i in 1 until sampledTrackPoints.size - 1) {
                sampledPoints.add(sampledTrackPoints[i])
            }
        }
        sampledPoints.add(lastPt)
    }

    val wandFade = if (activeBoosterAnimation is BoosterEffectAnimation.Wand && activeBoosterAnimation.targetArrow.id == arrow.id) {
        (1f - activeBoosterAnimation.progress).coerceIn(0f, 1f)
    } else 1.0f

    val renderColor = baseColor.copy(alpha = alpha * introAlpha * wandFade)

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
            if (drawProgress < 0.45f) 0.0f
            else if (drawProgress < 0.85f) ((drawProgress - 0.45f) / 0.40f) * 1.10f
            else 1.10f - ((drawProgress - 0.85f) / 0.15f) * 0.10f
        }
        drawProgress < 0.30f -> 0.0f
        drawProgress < 0.85f -> ((drawProgress - 0.30f) / 0.55f) * 1.10f
        else -> 1.10f - ((drawProgress - 0.85f) / 0.15f) * 0.10f
    }

    // Physical squash-and-stretch on blocked obstacle collision impact:
    // Slight compression along movement axis upon striking blocker, springing back elastically
    val squashFactor = if (arrow.state == ArrowState.WRONG_FLASH) {
        val t = arrow.flightProgress
        if (t in 0.36f..0.52f) {
            val p = sin(((t - 0.36f) / 0.16f) * PI.toFloat())
            1.0f - 0.12f * p
        } else 1.0f
    } else 1.0f

    val effectiveHeadLength = headLength * headPopScale * squashFactor
    val effectiveHeadWidth = headWidth * headPopScale * (2.0f - squashFactor)

    // Current Head Position
    val currentHead = if (originalBodyLen == 0) {
        if (arrow.state == ArrowState.IDLE && drawProgress < 1f) {
            val centerPt = sampledTrackPoints.first()
            val firstPt = centerPt + dynamicTailDir * tailOffset
            val fullHead = centerPt
            firstPt + (fullHead - firstPt) * drawProgress
        } else {
            sampledTrackPoints.first()
        }
    } else {
        sampledTrackPoints.last()
    }

    // 2. Draw clean, crisp Unity One triangular spearhead seamlessly covering the stem extension
    if (effectiveHeadWidth > 0.5f) {
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

    // 3. Collision Impact Shockwave & Spark Burst when striking a blocking obstacle
    if (arrow.state == ArrowState.WRONG_FLASH && arrow.flightProgress >= 0.38f) {
        val t = arrow.flightProgress
        val impactPhase = ((t - 0.38f) / 0.45f).coerceIn(0f, 1f)
        val impactDecay = (1f - impactPhase).coerceIn(0f, 1f)
        val headDir = Offset(arrow.direction.dx.toFloat(), arrow.direction.dy.toFloat())
        val impactPoint = Offset(currentHead.x + headDir.x * tipForward, currentHead.y + headDir.y * tipForward)
        val shockRadius = cellSize * (0.08f + 0.38f * impactPhase)
        val shockAlpha = (impactDecay * 0.90f).coerceIn(0f, 1f)

        // A. Expanding dynamic impact shockwave ring
        drawCircle(
            color = theme.arrowWrong.copy(alpha = shockAlpha),
            radius = shockRadius,
            center = impactPoint,
            style = Stroke(
                width = 2.4.dp.toPx() * impactDecay,
                cap = StrokeCap.Round
            )
        )

        // B. High-energy impact spark flares splashing away from obstacle
        val sparkDist = cellSize * (0.10f + 0.28f * impactPhase)
        val sparkLen = cellSize * 0.09f * impactDecay
        val baseAngle = arrow.direction.angleDegrees + 180f
        val sparkAngles = listOf(-60f, -25f, 25f, 60f)
        for (angleDeg in sparkAngles) {
            val rad = (baseAngle + angleDeg) * (PI.toFloat() / 180f)
            val dirVec = Offset(sin(rad), -kotlin.math.cos(rad))
            val sparkStart = impactPoint + dirVec * sparkDist
            val sparkEnd = sparkStart + dirVec * sparkLen
            drawLine(
                color = theme.arrowWrong.copy(alpha = shockAlpha),
                start = sparkStart,
                end = sparkEnd,
                strokeWidth = 1.6.dp.toPx() * impactDecay,
                cap = StrokeCap.Round
            )
        }

        // C. Crisp luminous flash at instant of impact
        if (impactPhase < 0.28f) {
            val flashAlpha = ((0.28f - impactPhase) / 0.28f) * 0.75f
            drawCircle(
                color = Color.White.copy(alpha = flashAlpha),
                radius = cellSize * 0.14f * (1f - impactPhase),
                center = impactPoint
            )
        }
    }

    // 4. Draw Aerodynamic Flight Trail Speed Lines when escaping
    if (arrow.state == ArrowState.FLYING_OUT && sampledPoints.isNotEmpty() && arrow.flightProgress in 0.04f..0.96f) {
        val tailPt = sampledPoints.first()
        val dirX = forwardAtTail.x
        val dirY = forwardAtTail.y
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
    if (track.size < 2) return track.toList()
    val maxIdx = track.size - 1
    val s = startDist.coerceIn(0f, maxIdx.toFloat())
    val e = endDist.coerceIn(0f, maxIdx.toFloat())
    if (s > e) return emptyList()

    val pts = mutableListOf<Offset>()

    // Start point
    val sIdx = s.toInt().coerceIn(0, maxIdx - 1)
    val sFrac = s - sIdx
    val startPt = lerpOffset(track[sIdx], track[sIdx + 1], sFrac)
    pts.add(startPt)

    // Intermediate whole integer track points strictly between s and e
    val firstInt = sIdx + 1
    val lastInt = kotlin.math.floor(e).toInt()
    for (i in firstInt..minOf(lastInt, maxIdx - 1)) {
        if (i.toFloat() > s + 0.0001f && i.toFloat() < e - 0.0001f) {
            pts.add(track[i])
        }
    }

    // End point (if e > s and not virtually identical)
    if (e > s) {
        val eIdx = e.toInt().coerceIn(0, maxIdx - 1)
        val eFrac = e - eIdx
        val endPt = lerpOffset(track[eIdx], track[eIdx + 1], eFrac)
        if ((endPt - startPt).getDistance() > 0.01f) {
            pts.add(endPt)
        }
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

/**
 * Renders high-energy visual effects for booster power-ups (Hammer Strike & Shatter, Magic Wand Stardust Dissolution).
 */
private fun DrawScope.drawBoosterAnimation(
    origin: Offset,
    animation: BoosterEffectAnimation,
    cellSize: Float,
    theme: ThemePreset
) {
    when (animation) {
        is BoosterEffectAnimation.Hammer -> {
            val arrow = animation.targetArrow
            val progress = animation.progress
            val head = arrow.head
            val tail = arrow.body.firstOrNull() ?: head
            val arrowCenter = Offset(
                origin.x + ((head.x + tail.x) / 2f + 0.5f) * cellSize,
                origin.y + ((head.y + tail.y) / 2f + 0.5f) * cellSize
            )

            if (progress < 0.40f) {
                // 1. Hammer Swing Down (accelerated dynamic swing curve)
                val swingP = progress / 0.40f
                val easedSwing = swingP * swingP
                val hammerAngle = -55f + easedSwing * 65f
                val hammerPivot = Offset(arrowCenter.x + cellSize * 0.75f, arrowCenter.y - cellSize * 1.3f)

                rotate(degrees = hammerAngle, pivot = hammerPivot) {
                    val handleLen = cellSize * 1.5f
                    val handleWidth = cellSize * 0.16f
                    // Wooden Handle
                    drawRoundRect(
                        color = Color(0xFFB45309),
                        topLeft = Offset(arrowCenter.x - handleWidth / 2f, arrowCenter.y - handleLen),
                        size = Size(handleWidth, handleLen),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                    // Metal Mallet Head
                    val headW = cellSize * 0.9f
                    val headH = cellSize * 0.52f
                    drawRoundRect(
                        color = Color(0xFF64748B),
                        topLeft = Offset(arrowCenter.x - headW / 2f, arrowCenter.y - cellSize * 0.35f),
                        size = Size(headW, headH),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                    // Metallic Highlight Bevel
                    drawRoundRect(
                        color = Color(0xFF94A3B8),
                        topLeft = Offset(arrowCenter.x - headW / 2f + 2.dp.toPx(), arrowCenter.y - cellSize * 0.35f + 2.dp.toPx()),
                        size = Size(headW - 4.dp.toPx(), headH * 0.35f),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            } else {
                // 2. Shatter & Fragment Explosion (Impact at t = 0.40)
                val shatterP = (progress - 0.40f) / 0.60f
                val decay = 1f - shatterP
                val maxShardDist = cellSize * 1.4f * shatterP

                // A. Ground Impact Shockwave Ring
                drawCircle(
                    color = theme.arrowClear.copy(alpha = (decay * 0.85f).coerceIn(0f, 1f)),
                    radius = cellSize * 0.2f + cellSize * 1.15f * shatterP,
                    center = arrowCenter,
                    style = Stroke(width = 3.dp.toPx() * decay, cap = StrokeCap.Round)
                )

                // B. Radiating Arrow Shard Fragments (12 angular shards)
                val shardColors = listOf(
                    theme.arrowClear,
                    Color(0xFFF59E0B),
                    Color(0xFF38BDF8),
                    Color(0xFFEC4899),
                    Color(0xFF10B981)
                )
                for (i in 0 until 12) {
                    val angle = (i * 30f + 15f) * (PI.toFloat() / 180f)
                    val speed = 0.75f + (i % 3) * 0.25f
                    val shardX = arrowCenter.x + cos(angle) * maxShardDist * speed
                    val shardY = arrowCenter.y + sin(angle) * maxShardDist * speed + (shatterP * shatterP * cellSize * 0.55f)
                    val shardSize = cellSize * 0.16f * decay
                    val shardColor = shardColors[i % shardColors.size].copy(alpha = decay.coerceIn(0f, 1f))
                    val spinDir = if (i % 2 == 0) 1.0f else -1.0f

                    rotate(degrees = shatterP * 360f * spinDir, pivot = Offset(shardX, shardY)) {
                        drawRect(
                            color = shardColor,
                            topLeft = Offset(shardX - shardSize / 2f, shardY - shardSize / 2f),
                            size = Size(shardSize, shardSize)
                        )
                    }
                }
            }
        }

        is BoosterEffectAnimation.Wand -> {
            val arrow = animation.targetArrow
            val progress = animation.progress
            val head = arrow.head
            val tail = arrow.body.firstOrNull() ?: head
            val headPos = Offset(origin.x + (head.x + 0.5f) * cellSize, origin.y + (head.y + 0.5f) * cellSize)
            val tailPos = Offset(origin.x + (tail.x + 0.5f) * cellSize, origin.y + (tail.y + 0.5f) * cellSize)
            val wandTip = lerpOffset(headPos, tailPos, progress)

            // Radiant golden stardust halo around wand tip
            drawCircle(
                color = Color(0xFFFDE047).copy(alpha = 0.50f * (1f - progress * 0.3f)),
                radius = cellSize * 0.55f,
                center = wandTip
            )

            // Sparkling stardust trail particles (12 spiral sparkles)
            val sparkColors = listOf(Color(0xFFFEF08A), Color(0xFF38BDF8), Color(0xFFF472B6), Color.White)
            for (i in 0 until 12) {
                val sparkPhase = (progress * 3f + i * 0.25f) % 1f
                val sparkAngle = (i * 30f + progress * 360f) * (PI.toFloat() / 180f)
                val sparkDist = cellSize * 0.45f * sparkPhase
                val sparkPos = Offset(
                    wandTip.x + cos(sparkAngle) * sparkDist,
                    wandTip.y + sin(sparkAngle) * sparkDist
                )
                val sparkAlpha = (1f - sparkPhase).coerceIn(0f, 1f)
                val sparkRadius = cellSize * 0.08f * (1f - sparkPhase * 0.5f)

                drawCircle(
                    color = sparkColors[i % sparkColors.size].copy(alpha = sparkAlpha),
                    radius = sparkRadius,
                    center = sparkPos
                )
            }

            // Elegant Magic Wand Body (silver shaft + golden star tip)
            val wandAngle = -40f + progress * 20f
            rotate(degrees = wandAngle, pivot = wandTip) {
                val shaftLen = cellSize * 1.3f
                val shaftW = cellSize * 0.12f
                // Shaft
                drawRoundRect(
                    color = Color(0xFFE2E8F0),
                    topLeft = Offset(wandTip.x - shaftW / 2f, wandTip.y),
                    size = Size(shaftW, shaftLen),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                // Golden Star Head
                drawCircle(
                    color = Color(0xFFF59E0B),
                    radius = cellSize * 0.18f,
                    center = wandTip
                )
                drawCircle(
                    color = Color(0xFFFEF08A),
                    radius = cellSize * 0.10f,
                    center = wandTip
                )
            }
        }
    }
}

/**
 * Draws animated pulsing guidance rings and pointer finger on Level 1-3 for first-time players.
 */
/**
 * Draws an authentic, realistic human hand vector with a smooth 3D tap gesture for Level 1.
 * Dynamically adapts to ANY arrow design, orientation, or layout on Level 1 without code changes.
 */
private fun DrawScope.drawTutorialGuidance(
    origin: Offset,
    targetArrow: Arrow,
    cellSize: Float,
    theme: ThemePreset
) {
    // 1. Dynamically target the visual center / stem of ANY arrow design
    val allCoords = targetArrow.allPoints
    val midCoord = allCoords[allCoords.size / 2]
    val targetPoint = Offset(
        origin.x + (midCoord.x.toFloat() + 0.5f) * cellSize,
        origin.y + (midCoord.y.toFloat() + 0.5f) * cellSize
    )

    // 2. Realistic, natural human hand dimensions (natural 3/4 pointing hand)
    val handWidth = maxOf(cellSize * 0.70f, 66.dp.toPx()).coerceAtMost(86.dp.toPx())
    val handHeight = maxOf(cellSize * 0.92f, 80.dp.toPx()).coerceAtMost(104.dp.toPx())
    val fw = handWidth * 0.24f // Natural index finger width
    val fl = handHeight * 0.44f // Index finger length

    // 3. Dynamically determine optimal approach angle based on arrow exit path and screen clearance
    // Ensures the hand never obstructs the arrow's exit trajectory and stays cleanly inside screen bounds
    val candidateAngles = when (targetArrow.direction) {
        Direction.UP -> listOf(-38f, 38f)
        Direction.DOWN -> listOf(142f, -142f)
        Direction.RIGHT -> listOf(38f, 142f)
        Direction.LEFT -> listOf(-38f, -142f)
    }

    val chosenAngle = candidateAngles.firstOrNull { angle ->
        val rad = angle * (PI.toFloat() / 180f)
        val bodyCenterX = targetPoint.x - sin(rad) * handHeight * 0.55f
        val bodyCenterY = targetPoint.y + cos(rad) * handHeight * 0.55f
        bodyCenterX in (handWidth * 0.55f)..(size.width - handWidth * 0.55f) &&
        bodyCenterY in (handHeight * 0.55f)..(size.height - handHeight * 0.55f)
    } ?: candidateAngles.first()

    val now = System.currentTimeMillis()
    val cycleTime = 1200L
    val cycle = ((now % cycleTime).toFloat()) / cycleTime.toFloat()

    // 4. Smooth 3D tap gesture animation rhythm: Press down -> Brief hold -> Spring release -> Rest pause
    val (tapProgress, scaleFactor) = when {
        // Press down with physical depth (0ms to 280ms)
        cycle < 0.24f -> {
            val t = cycle / 0.24f
            val ease = (1f - cos(t * PI.toFloat())) / 2f
            ease to (1f - 0.12f * ease)
        }
        // Brief contact hold (280ms to 320ms)
        cycle in 0.24f..0.30f -> {
            1f to 0.88f
        }
        // Spring release back (320ms to 520ms)
        cycle in 0.30f..0.50f -> {
            val t = (cycle - 0.30f) / 0.20f
            val ease = (1f + cos(t * PI.toFloat())) / 2f
            ease to (0.88f + 0.12f * (1f - ease))
        }
        // Rest pause (520ms to 1200ms)
        else -> {
            0f to 1.0f
        }
    }

    val handRad = chosenAngle * (PI.toFloat() / 180f)
    val dirX = sin(handRad)
    val dirY = -cos(handRad)

    val restDistance = 16.dp.toPx()
    val tapDisplacement = tapProgress * restDistance
    val currentDistance = restDistance - tapDisplacement

    val tipPos = Offset(
        targetPoint.x - dirX * currentDistance,
        targetPoint.y - dirY * currentDistance
    )

    val shadowOffsetX = 2.5.dp.toPx()
    val shadowOffsetY = 3.5.dp.toPx()
    val outlineStrokePx = 2.0.dp.toPx()
    val creaseStrokePx = 1.4.dp.toPx()

    // 5. Realistic Natural Human Hand Vector Path (index finger pointing at (0, 0))
    val handPath = Path().apply {
        // Natural rounded fingertip pad
        moveTo(-fw * 0.5f, fw * 0.40f)
        cubicTo(-fw * 0.5f, 0f, fw * 0.5f, 0f, fw * 0.5f, fw * 0.40f)

        // Outer side of index finger flaring naturally to first knuckle
        lineTo(fw * 0.55f, fl * 0.88f)

        // First knuckle (middle finger)
        cubicTo(fw * 0.60f, fl * 0.96f, handWidth * 0.42f, fl * 1.05f, handWidth * 0.42f, fl * 1.25f)

        // Smooth, natural rounded contour of curled fingers (curved fist arch, no jagged lobes)
        cubicTo(handWidth * 0.42f, handHeight * 0.68f, handWidth * 0.36f, handHeight * 0.82f, handWidth * 0.22f, handHeight * 0.90f)

        // Outer heel of palm to wrist
        lineTo(handWidth * 0.10f, handHeight * 1.02f)

        // Wrist cuff base
        cubicTo(0f, handHeight * 1.05f, -handWidth * 0.14f, handHeight * 1.05f, -handWidth * 0.24f, handHeight * 1.0f)

        // Base of palm / thenar muscle (left side)
        cubicTo(-handWidth * 0.30f, handHeight * 0.90f, -handWidth * 0.34f, handHeight * 0.76f, -handWidth * 0.32f, handHeight * 0.62f)

        // Natural folded thumb (folded gracefully along the palm, not sticking out sideways)
        cubicTo(-handWidth * 0.38f, handHeight * 0.52f, -handWidth * 0.34f, fl * 1.05f, -handWidth * 0.20f, fl * 0.95f)
        cubicTo(-handWidth * 0.15f, fl * 0.92f, -fw * 0.55f, fl * 0.88f, -fw * 0.52f, fl * 0.82f)

        // Inner side of index finger back up to tip
        lineTo(-fw * 0.5f, fw * 0.40f)
        close()
    }

    // Realistic folded thumb crease
    val creaseThumb = Path().apply {
        moveTo(-handWidth * 0.20f, fl * 0.95f)
        cubicTo(-handWidth * 0.10f, handHeight * 0.55f, -handWidth * 0.08f, handHeight * 0.68f, -handWidth * 0.16f, handHeight * 0.80f)
    }

    // Natural knuckle folds of curled fingers
    val creaseMiddle = Path().apply {
        moveTo(handWidth * 0.40f, fl * 1.25f)
        cubicTo(handWidth * 0.28f, fl * 1.23f, handWidth * 0.16f, fl * 1.22f, handWidth * 0.06f, fl * 1.24f)
    }

    val creaseRing = Path().apply {
        moveTo(handWidth * 0.34f, handHeight * 0.70f)
        cubicTo(handWidth * 0.24f, handHeight * 0.69f, handWidth * 0.14f, handHeight * 0.68f, handWidth * 0.05f, handHeight * 0.70f)
    }

    // Subtle middle joint crease on index finger
    val creaseFingerJoint = Path().apply {
        moveTo(-fw * 0.42f, fl * 0.46f)
        lineTo(fw * 0.42f, fl * 0.46f)
    }

    val handBorder = if (theme.isDark) Color(0xFF0F172A) else Color(0xFF1E293B)

    withTransform({
        translate(tipPos.x, tipPos.y)
        rotate(chosenAngle, pivot = Offset.Zero)
        scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
    }) {
        // Soft tactile drop shadow
        withTransform({ translate(shadowOffsetX, shadowOffsetY) }) {
            drawPath(path = handPath, color = Color(0x30000000))
        }

        // Crisp white glove / hand fill
        drawPath(path = handPath, color = Color.White)

        // Natural knuckle and thumb crease details
        drawPath(
            path = creaseMiddle,
            color = handBorder.copy(alpha = 0.75f),
            style = Stroke(width = creaseStrokePx, cap = StrokeCap.Round)
        )
        drawPath(
            path = creaseRing,
            color = handBorder.copy(alpha = 0.75f),
            style = Stroke(width = creaseStrokePx, cap = StrokeCap.Round)
        )
        drawPath(
            path = creaseThumb,
            color = handBorder.copy(alpha = 0.75f),
            style = Stroke(width = creaseStrokePx, cap = StrokeCap.Round)
        )
        drawPath(
            path = creaseFingerJoint,
            color = handBorder.copy(alpha = 0.45f),
            style = Stroke(width = creaseStrokePx * 0.9f, cap = StrokeCap.Round)
        )

        // Bold, crisp, clean dark outline
        drawPath(
            path = handPath,
            color = handBorder,
            style = Stroke(width = outlineStrokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
