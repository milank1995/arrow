package com.arrowescape.puzzle.ui.screens

import com.arrowescape.puzzle.BuildConfig
import com.arrowescape.puzzle.ui.components.AutoSolveButton

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.arrowescape.puzzle.audio.AudioHapticManager
import com.arrowescape.puzzle.core.engine.GridOccupancyMap
import com.arrowescape.puzzle.core.engine.LevelLoader
import com.arrowescape.puzzle.core.engine.PuzzleSolver
import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.ArrowState
import com.arrowescape.puzzle.core.model.LevelData
import com.arrowescape.puzzle.ui.components.BoosterBar
import com.arrowescape.puzzle.ui.components.CheerOverlay
import com.arrowescape.puzzle.ui.components.GameHeader
import com.arrowescape.puzzle.ui.components.PuzzleBoardCanvas
import com.arrowescape.puzzle.ui.components.ThemeCustomizationDialog
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

@Composable
fun DevTestScreen(
    theme: ThemePreset,
    audioHapticManager: AudioHapticManager,
    onBackToHomeClick: () -> Unit,
    onPlayInRegularGame: ((Int) -> Unit)? = null,
    onThemeSelect: (ThemePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    BackHandler(onBack = onBackToHomeClick)

    var currentLevelNumber by remember { mutableIntStateOf(1) }
    var levelData by remember { mutableStateOf<LevelData?>(null) }
    var arrows by remember { mutableStateOf<List<Arrow>>(emptyList()) }
    var occupancyMap by remember { mutableStateOf(GridOccupancyMap(4, 4)) }

    var isGridLinesActive by remember { mutableStateOf(false) }
    var isCelebrationBurstActive by remember { mutableStateOf(false) }
    var previewRayArrowIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var cheerMessage by remember { mutableStateOf<String?>(null) }
    var cheerFromLeft by remember { mutableStateOf(true) }
    var comboCount by remember { mutableIntStateOf(0) }
    var isLevelComplete by remember { mutableStateOf(false) }
    var restartTrigger by remember { mutableLongStateOf(0L) }
    var milestone25Fired by remember { mutableStateOf(false) }
    var milestone50Fired by remember { mutableStateOf(false) }
    var milestone70Fired by remember { mutableStateOf(false) }
    var milestone90Fired by remember { mutableStateOf(false) }
    var lastMilestoneTimestamp by remember { mutableLongStateOf(0L) }
    var cheerDismissJob by remember { mutableStateOf<Job?>(null) }

    var showLevelSelectDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var isAutoSolving by remember { mutableStateOf(false) }
    var autoSolveJob by remember { mutableStateOf<Job?>(null) }

    fun loadLevel(levelNum: Int) {
        autoSolveJob?.cancel()
        autoSolveJob = null
        isAutoSolving = false
        val boundedLevel = levelNum.coerceIn(1, 150)
        val level = LevelLoader.loadLevel(context, boundedLevel)
        levelData = level
        arrows = level.arrows
        occupancyMap = GridOccupancyMap(level.xSize, level.ySize).apply {
            rebuild(level.arrows)
        }
        currentLevelNumber = boundedLevel
        isLevelComplete = false
        isCelebrationBurstActive = false
        comboCount = 0
        cheerMessage = null
        milestone25Fired = false
        milestone50Fired = false
        milestone70Fired = false
        milestone90Fired = false
        lastMilestoneTimestamp = 0L
        cheerDismissJob?.cancel()
        previewRayArrowIds = emptySet()
        restartTrigger = System.currentTimeMillis()
    }

    LaunchedEffect(Unit) {
        loadLevel(currentLevelNumber)
    }

    fun launchArrowFlight(arrow: Arrow) {
        val newCombo = comboCount + 1
        comboCount = newCombo
        audioHapticManager.playArrowFly(newCombo)

        occupancyMap.release(arrow)

        val totalArrows = arrows.size
        val clearedCount = arrows.count {
            it.state == ArrowState.CLEARED || it.state == ArrowState.FLYING_OUT
        } + 1

        var milestoneCheer: String? = null
        val now = System.currentTimeMillis()
        if (clearedCount < totalArrows && totalArrows >= 6 && currentLevelNumber > 2 && (now - lastMilestoneTimestamp >= 3500L)) {
            val threshold90 = ceil(totalArrows * 0.90f).toInt()
            val threshold70 = ceil(totalArrows * 0.70f).toInt()
            val threshold50 = ceil(totalArrows * 0.50f).toInt()
            val threshold25 = ceil(totalArrows * 0.25f).toInt()

            if (!milestone90Fired && clearedCount >= threshold90) {
                milestone25Fired = true
                milestone50Fired = true
                milestone70Fired = true
                milestone90Fired = true
                lastMilestoneTimestamp = now
                milestoneCheer = "90% · Puzzle Complete"
            } else if (!milestone70Fired && clearedCount >= threshold70) {
                milestone25Fired = true
                milestone50Fired = true
                milestone70Fired = true
                lastMilestoneTimestamp = now
                milestoneCheer = "70% · Puzzle Complete"
            } else if (!milestone50Fired && clearedCount >= threshold50) {
                milestone25Fired = true
                milestone50Fired = true
                lastMilestoneTimestamp = now
                milestoneCheer = "50% · Puzzle Complete"
            } else if (!milestone25Fired && clearedCount >= threshold25) {
                milestone25Fired = true
                lastMilestoneTimestamp = now
                milestoneCheer = "25% · Puzzle Complete"
            }
        }

        val comboPraise = if (milestoneCheer == null && (now - lastMilestoneTimestamp >= 2500L)) {
            when (newCombo) {
                5 -> listOf("On Fire!", "In the Flow!", "Pure Rhythm!", "Grooving!").random()
                10 -> listOf("Arrow Maestro!", "Laser Focus!", "Brilliant!", "Spectacular!").random()
                15 -> listOf("Galaxy Brain!", "Flawless!", "Untouchable!", "Masterful!").random()
                20 -> listOf("Living Legend!", "Perfection!", "Supreme Flow!").random()
                else -> null
            }
        } else null

        val cheer = milestoneCheer ?: comboPraise
        val isMilestone = (cheer != null && cheer == milestoneCheer)
        if (cheer != null) {
            cheerMessage = cheer
            cheerFromLeft = !cheerFromLeft
            cheerDismissJob?.cancel()
            val displayDuration = if (isMilestone) 1800L else 1000L
            cheerDismissJob = coroutineScope.launch {
                delay(displayDuration)
                cheerMessage = null
            }
        }

        arrows = arrows.map {
            if (it.id == arrow.id) it.copyWithState(ArrowState.FLYING_OUT, highlighted = false) else it
        }

        coroutineScope.launch {
            val durationMs = 560L
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val rawProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                val easedProgress = if (rawProgress < 0.5f) {
                    2f * rawProgress * rawProgress
                } else {
                    val t = rawProgress - 1f
                    1f - 2f * t * t
                }
                arrows = arrows.map {
                    if (it.id == arrow.id && it.state == ArrowState.FLYING_OUT) {
                        it.copy(flightProgress = easedProgress)
                    } else it
                }
                if (rawProgress >= 1f) break
                delay(16)
            }

            val finalArrows = arrows.map {
                if (it.id == arrow.id) it.copyWithState(ArrowState.CLEARED, highlighted = false) else it
            }
            arrows = finalArrows

            val allCleared = finalArrows.all { it.state == ArrowState.CLEARED }
            if (allCleared) {
                isCelebrationBurstActive = true
                audioHapticManager.playLevelWin()
                delay(950)
                isLevelComplete = true
                isCelebrationBurstActive = false
            } else {
                // Auto-release unblocked arrows if previously highlighted
                delay(120)
                val candidates = arrows.filter { it.isHighlighted && it.state == ArrowState.IDLE }
                for (cand in candidates) {
                    if (PuzzleSolver.canMoveOut(cand, occupancyMap)) {
                        launchArrowFlight(cand)
                        break
                    }
                }
            }
        }
    }

    fun onArrowClicked(clickedArrow: Arrow) {
        if (isLevelComplete) return
        val currentArrow = arrows.firstOrNull { it.id == clickedArrow.id } ?: clickedArrow
        if (currentArrow.state == ArrowState.CLEARED || currentArrow.state == ArrowState.FLYING_OUT) return

        // Always dismiss all path previews when user clicks an arrow to move it
        previewRayArrowIds = emptySet()

        // Automatically hide Grid Guide upon tapping an arrow
        if (isGridLinesActive) {
            isGridLinesActive = false
        }

        audioHapticManager.playTap()

        val canEscape = PuzzleSolver.canMoveOut(currentArrow, occupancyMap)

        if (canEscape) {
            launchArrowFlight(currentArrow)
        } else {
            // Collision: visual spring bounce recoil, but unlimited lives (no game over)
            audioHapticManager.playWrongMove()
            comboCount = 0
            cheerMessage = null

            val dist = PuzzleSolver.getDistanceToBlocker(currentArrow, occupancyMap)
            val durationMs = (300L + (dist * 40L).toLong()).coerceIn(360L, 640L)

            arrows = arrows.map {
                if (it.id == currentArrow.id) {
                    it.copyWithState(
                        newState = ArrowState.WRONG_FLASH,
                        progress = 0f,
                        highlighted = true,
                        collisionDist = dist
                    )
                } else it
            }

            coroutineScope.launch {
                val startTime = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val rawProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                    arrows = arrows.map {
                        if (it.id == currentArrow.id && it.state == ArrowState.WRONG_FLASH) {
                            it.copy(flightProgress = rawProgress)
                        } else it
                    }
                    if (rawProgress >= 1f) break
                    delay(16)
                }

                arrows = arrows.map {
                    if (it.id == currentArrow.id && it.state == ArrowState.WRONG_FLASH) {
                        it.copyWithState(ArrowState.IDLE, 0f, highlighted = true)
                    } else it
                }
            }
        }
    }

    fun toggleAutoSolve() {
        if (isAutoSolving) {
            autoSolveJob?.cancel()
            autoSolveJob = null
            isAutoSolving = false
        } else {
            if (isLevelComplete) return
            isAutoSolving = true
            autoSolveJob = coroutineScope.launch {
                while (isAutoSolving && !isLevelComplete) {
                    val uncleared = arrows.filter { it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT }
                    if (uncleared.isEmpty()) break

                    val movable = PuzzleSolver.findAllMovableArrows(arrows, occupancyMap)
                    val nextArrow = PuzzleSolver.getBestHint(arrows, occupancyMap) ?: movable.firstOrNull()

                    if (nextArrow != null) {
                        onArrowClicked(nextArrow)
                        delay(220)
                    } else {
                        delay(60)
                    }
                }
                isAutoSolving = false
            }
        }
    }

    val remainingArrowsCount = arrows.count { it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT }
    val totalArrowsCount = levelData?.arrows?.size ?: arrows.size
    val currentLevel = levelData

    Surface(
        color = theme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
        ) {
            // 1. TOP GAMEPLAY HEADER (Matches original game header exactly)
            if (currentLevel != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GameHeader(
                        levelNumber = currentLevelNumber,
                        heartsRemaining = 3, // Unlimited lives: full hearts always displayed
                        maxHearts = 3,
                        remainingArrowsCount = remainingArrowsCount,
                        totalArrowsCount = totalArrowsCount,
                        theme = theme,
                        shapeName = levelData?.shapeName,
                        isTestMode = true,
                        onPrevLevelClick = {
                            if (currentLevelNumber > 1) {
                                loadLevel(currentLevelNumber - 1)
                            }
                        },
                        onNextLevelClick = {
                            if (currentLevelNumber < 150) {
                                loadLevel(currentLevelNumber + 1)
                            }
                        },
                        onLevelNumberClick = {
                            showLevelSelectDialog = true
                        },
                        onBackClick = onBackToHomeClick,
                        onThemeClick = { showThemeDialog = true },
                        modifier = Modifier.widthIn(max = 540.dp)
                    )
                }
            }

            // 2. MAIN PUZZLE BOARD CANVAS & CHEER OVERLAY
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (currentLevel != null) {
                    PuzzleBoardCanvas(
                        xSize = currentLevel.xSize,
                        ySize = currentLevel.ySize,
                        arrows = arrows,
                        theme = theme,
                        isGridLinesActive = isGridLinesActive,
                        isCelebrationBurstActive = isCelebrationBurstActive,
                        restartTrigger = restartTrigger,
                        previewRayArrowIds = previewRayArrowIds,
                        currentLevelNumber = currentLevelNumber,
                        showTutorialHand = false,
                        onArrowClick = { arrow ->
                            onArrowClicked(arrow)
                        },
                        onArrowLongPress = { arrow, _ ->
                            previewRayArrowIds = if (arrow.id in previewRayArrowIds) {
                                previewRayArrowIds - arrow.id
                            } else {
                                previewRayArrowIds + arrow.id
                            }
                        },
                        onDismissPreviewRays = {
                            previewRayArrowIds = emptySet()
                        }
                    )
                }

                CheerOverlay(
                    message = cheerMessage,
                    theme = theme,
                    fromLeft = cheerFromLeft,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                )

                // Developer Auto-Solve Testing Tool (Debug builds only)
                if (BuildConfig.DEBUG) {
                    AutoSolveButton(
                        isAutoSolving = isAutoSolving,
                        onClick = {
                            audioHapticManager.playClick()
                            toggleAutoSolve()
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 12.dp)
                    )
                }
            }

            // 3. BOTTOM BOOSTER CONTROLS (Identical to original game, plus instant restart)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                BoosterBar(
                    hintsCount = 999, // Unlimited hints in test mode
                    isGridLinesActive = isGridLinesActive,
                    theme = theme,
                    onHintClick = {
                        val hintTarget = PuzzleSolver.getBestHint(arrows, occupancyMap)
                        if (hintTarget != null) {
                            audioHapticManager.playTap()
                            arrows = arrows.map {
                                if (it.id == hintTarget.id) it.copyWithState(ArrowState.HINTED) else it
                            }
                        }
                    },
                    onGridClick = {
                        isGridLinesActive = !isGridLinesActive
                    },
                    onRestartClick = {
                        audioHapticManager.playTap()
                        loadLevel(currentLevelNumber)
                    },
                    modifier = Modifier.widthIn(max = 540.dp)
                )
            }
        }

        // VICTORY SCREEN (Dedicated screen with Ad integration placeholders)
        if (isLevelComplete) {
            VictoryScreen(
                levelNumber = currentLevelNumber,
                coinsEarned = 50 + (comboCount * 5),
                streakDays = 1,
                totalCoins = 250,
                theme = theme,
                heartsRemaining = 3,
                maxHearts = 3,
                isDailyChallenge = false,
                onClaimDoubleCoins = {},
                onNextLevelClick = {
                    loadLevel(currentLevelNumber + 1)
                },
                onHomeClick = onBackToHomeClick
            )
        }

        // THEME CUSTOMIZATION DIALOG
        if (showThemeDialog) {
            ThemeCustomizationDialog(
                activeTheme = theme,
                onThemeSelect = onThemeSelect,
                onDismiss = { showThemeDialog = false }
            )
        }

        // CLEAN LEVEL SELECTOR MODAL
        if (showLevelSelectDialog) {
            LevelSelectorDialog(
                currentLevel = currentLevelNumber,
                onLevelSelected = { lvl ->
                    showLevelSelectDialog = false
                    loadLevel(lvl)
                },
                onPlayInRegularGame = { lvl ->
                    showLevelSelectDialog = false
                    onPlayInRegularGame?.invoke(lvl)
                },
                onDismiss = { showLevelSelectDialog = false }
            )
        }
    }
}

@Composable
private fun LevelSelectorDialog(
    currentLevel: Int,
    onLevelSelected: (Int) -> Unit,
    onPlayInRegularGame: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputLevelText by remember { mutableStateOf(currentLevel.toString()) }
    var isShapesOnly by remember { mutableStateOf(false) }

    val manifest = remember { LevelLoader.getCuratedManifest(context) }
    val shapeLevels = remember(manifest) { manifest.filter { it.isShape } }

    fun getShapeEmoji(name: String): String {
        return when {
            name.contains("Heart", ignoreCase = true) -> "❤️"
            name.contains("Palm", ignoreCase = true) -> "🌴"
            name.contains("Balloon", ignoreCase = true) -> "🎈"
            name.contains("Cat", ignoreCase = true) -> "🐱"
            name.contains("Jellyfish", ignoreCase = true) -> "🪼"
            name.contains("Vase", ignoreCase = true) -> "🏺"
            name.contains("Apple", ignoreCase = true) -> "🍎"
            name.contains("Shark", ignoreCase = true) -> "🦈"
            name.contains("Robot", ignoreCase = true) -> "🤖"
            name.contains("Tower", ignoreCase = true) -> "🗼"
            name.contains("Sunflower", ignoreCase = true) -> "🌻"
            name.contains("Magnolia", ignoreCase = true) -> "🌸"
            name.contains("Conch", ignoreCase = true) -> "🐚"
            name.contains("Panda", ignoreCase = true) -> "🐼"
            name.contains("Butterfly", ignoreCase = true) -> "🦋"
            name.contains("Swan", ignoreCase = true) -> "🦢"
            name.contains("Bird", ignoreCase = true) -> "🐦"
            name.contains("Elephant", ignoreCase = true) -> "🐘"
            name.contains("Whale", ignoreCase = true) -> "🐋"
            name.contains("Strawberry", ignoreCase = true) -> "🍓"
            name.contains("Lion", ignoreCase = true) -> "🦁"
            name.contains("Crab", ignoreCase = true) -> "🦀"
            name.contains("Chameleon", ignoreCase = true) -> "🦎"
            name.contains("Rose", ignoreCase = true) -> "🌹"
            name.contains("Thinker", ignoreCase = true) -> "🧠"
            name.contains("Window", ignoreCase = true) -> "🪟"
            else -> "🐾"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Dev Level Selector",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // FILTER BUTTONS ROW (All Levels vs Shape Puzzles Only)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. All Levels Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isShapesOnly) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                        border = if (!isShapesOnly) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isShapesOnly = false }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "📋 All (150)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isShapesOnly) Color.White else Color(0xFF334155)
                            )
                        }
                    }

                    // 2. Shape Puzzles Only Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isShapesOnly) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                        border = if (isShapesOnly) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isShapesOnly = true }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "🧩 Shapes Only (30)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isShapesOnly) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isShapesOnly) {
                    Text(
                        text = "Authentic Shape Puzzle Levels (Every 5 Levels):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        items(shapeLevels.size) { i ->
                            val item = shapeLevels[i]
                            val isCurrent = (item.levelNumber == currentLevel)
                            val emoji = getShapeEmoji(item.shapeName ?: "")
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCurrent) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCurrent) Color(0xFF6366F1) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLevelSelected(item.levelNumber) }
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "L${item.levelNumber}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isCurrent) Color(0xFF4338CA) else Color(0xFF1E293B)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$emoji ${item.shapeName}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF334155)
                                        )
                                    }
                                    Text(
                                        text = "${item.arrowCount} arr",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Quick presets for milestones
                    Text(
                        text = "Milestones & Key Shapes:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val quickLevels = listOf(
                        1 to "Intro (4 arr)",
                        5 to "🌴 Palm Tree (23)",
                        10 to "🎈 Balloon (33)",
                        15 to "🐱 Umbrella Cat (33)",
                        20 to "🪼 Jellyfish (35)",
                        25 to "🏺 Flower Vase (46)",
                        30 to "🍎 Apple (48)",
                        35 to "🦈 Kitten Shark (50)",
                        40 to "🤖 Robot (52)",
                        45 to "🍷 Wine Cat (54)",
                        50 to "🐈‍⬛ Black Cat (57)",
                        100 to "🐦 Winter Bird (110)",
                        150 to "🧠 The Thinker (262)"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickLevels.chunked(2).forEach { rowPair ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowPair.forEach { (lvl, desc) ->
                                    val isCurrent = (lvl == currentLevel)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCurrent) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isCurrent) Color(0xFF6366F1) else Color(0xFFE2E8F0)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onLevelSelected(lvl) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                text = "L$lvl",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isCurrent) Color(0xFF4338CA) else Color(0xFF1E293B)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = desc,
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                if (rowPair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Level Number Input
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputLevelText,
                        onValueChange = { inputLevelText = it },
                        label = { Text("Level # (1-150)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val lvl = inputLevelText.toIntOrNull()?.coerceIn(1, 150) ?: currentLevel
                            onLevelSelected(lvl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Test", fontWeight = FontWeight.Bold)
                    }
                    if (onPlayInRegularGame != null) {
                        Button(
                            onClick = {
                                val lvl = inputLevelText.toIntOrNull()?.coerceIn(1, 150) ?: currentLevel
                                onPlayInRegularGame(lvl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Play ▶", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
