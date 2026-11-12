package com.arrowaway.puzzle.ui.screens

import com.arrowaway.puzzle.BuildConfig
import com.arrowaway.puzzle.ui.components.AutoSolveButton
import androidx.compose.ui.draw.clip

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow

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
import com.arrowaway.puzzle.audio.AudioHapticManager
import com.arrowaway.puzzle.core.engine.GridOccupancyMap
import com.arrowaway.puzzle.core.engine.LevelLoader
import com.arrowaway.puzzle.core.engine.PuzzleSolver
import com.arrowaway.puzzle.core.model.Arrow
import com.arrowaway.puzzle.core.model.ArrowState
import com.arrowaway.puzzle.core.model.LevelData
import com.arrowaway.puzzle.ui.components.BoosterBar
import com.arrowaway.puzzle.ui.components.CheerOverlay
import com.arrowaway.puzzle.ui.components.GameHeader
import com.arrowaway.puzzle.ui.components.PuzzleBoardCanvas
import com.arrowaway.puzzle.ui.components.ThemeCustomizationDialog
import com.arrowaway.puzzle.ui.theme.ThemePreset
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
    initialDailyMode: Boolean = false,
    initialShapesMode: Boolean = false,
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

    var showLevelSelectDialog by remember { mutableStateOf(initialShapesMode) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var isAutoSolving by remember { mutableStateOf(false) }
    var autoSolveJob by remember { mutableStateOf<Job?>(null) }
    var isDailyMode by remember { mutableStateOf(initialDailyMode) }

    fun loadLevel(levelNum: Int, daily: Boolean = isDailyMode) {
        autoSolveJob?.cancel()
        autoSolveJob = null
        isAutoSolving = false
        val maxLevel = if (daily) LevelLoader.TOTAL_DAILY_LEVELS else LevelLoader.TOTAL_CAMPAIGN_LEVELS
        val boundedLevel = levelNum.coerceIn(1, maxLevel)
        val level = if (daily) {
            LevelLoader.loadDailyLevel(context, boundedLevel)
        } else {
            LevelLoader.loadLevel(context, boundedLevel)
        }
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

    LaunchedEffect(initialDailyMode) {
        isDailyMode = initialDailyMode
        loadLevel(if (initialDailyMode) 1 else currentLevelNumber, initialDailyMode)
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
                delay(1800)
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
            // 1. TOP GAMEPLAY HEADER
            if (currentLevel != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    GameHeader(
                        levelNumber = currentLevelNumber,
                        heartsRemaining = 3, // Unlimited lives: full hearts always displayed
                        maxHearts = 3,
                        remainingArrowsCount = remainingArrowsCount,
                        totalArrowsCount = totalArrowsCount,
                        theme = theme,
                        shapeName = if (isDailyMode) "Daily Challenge" else levelData?.shapeName,
                        isTestMode = true,
                        onPrevLevelClick = {
                            if (currentLevelNumber > 1) {
                                loadLevel(currentLevelNumber - 1)
                            }
                        },
                        onNextLevelClick = {
                            val maxLevel = if (isDailyMode) LevelLoader.TOTAL_DAILY_LEVELS else LevelLoader.TOTAL_CAMPAIGN_LEVELS
                            if (currentLevelNumber < maxLevel) {
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

                    // Mode Switcher Pill (Campaign vs Daily Challenges)
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = theme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                        shadowElevation = 1.dp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = if (!isDailyMode) theme.arrowClear else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .clickable {
                                        if (isDailyMode) {
                                            isDailyMode = false
                                            loadLevel(1, daily = false)
                                        }
                                    }
                            ) {
                                Text(
                                    text = "Unique Shapes (1–${LevelLoader.TOTAL_CAMPAIGN_LEVELS})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isDailyMode) Color.White else theme.textSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = if (isDailyMode) Color(0xFF2563EB) else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .clickable {
                                        if (!isDailyMode) {
                                            isDailyMode = true
                                            loadLevel(1, daily = true)
                                        }
                                    }
                            ) {
                                Text(
                                    text = "Daily Challenges (1–235)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDailyMode) Color.White else theme.textSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
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
                    shapeName = levelData?.shapeName,
                    unitySource = levelData?.unitySource,
                    remainingArrowsCount = arrows.count { it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT },
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
                isDailyMode = isDailyMode,
                onLevelSelected = { lvl ->
                    showLevelSelectDialog = false
                    loadLevel(lvl)
                },
                onPlayInRegularGame = if (!isDailyMode) { lvl ->
                    showLevelSelectDialog = false
                    onPlayInRegularGame?.invoke(lvl)
                } else null,
                onDismiss = { showLevelSelectDialog = false }
            )
        }
    }
}

@Composable
private fun LevelSelectorDialog(
    currentLevel: Int,
    isDailyMode: Boolean = false,
    onLevelSelected: (Int) -> Unit,
    onPlayInRegularGame: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputLevelText by remember { mutableStateOf(currentLevel.toString()) }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val manifest = remember { LevelLoader.getCuratedManifest(context) }
    val shapeLevels = remember(manifest) { manifest.filter { it.isShape } }

    val categories = remember(shapeLevels) {
        val cats = mutableListOf("All" to "All (${shapeLevels.size})")
        val grouped = shapeLevels.groupBy { it.category ?: "Objects & Symbols" }
        listOf(
            "Animals & Wildlife" to "🐾 Animals",
            "Objects & Symbols" to "💎 Objects",
            "Flora & Nature" to "🌿 Nature",
            "Food & Refreshments" to "🍎 Food",
            "Mythology & Zodiac" to "🔮 Zodiac",
            "Music, Art & Culture" to "🎨 Art",
            "World Landmarks" to "🏛️ Landmarks",
            "Vehicles & Travel" to "🚀 Vehicles"
        ).forEach { (catKey, catLabel) ->
            val count = grouped[catKey]?.size ?: 0
            if (count > 0) {
                cats.add(catKey to "$catLabel ($count)")
            }
        }
        cats
    }

    val filteredShapes = remember(shapeLevels, selectedCategory, searchQuery) {
        shapeLevels.filter { item ->
            val matchesCategory = (selectedCategory == "All") || (item.category == selectedCategory)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim()
                item.shapeName?.contains(q, ignoreCase = true) == true ||
                item.levelNumber.toString() == q ||
                item.category?.contains(q, ignoreCase = true) == true
            }
            matchesCategory && matchesSearch
        }
    }

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
                        text = if (isDailyMode) "Daily Challenge Level Selector" else "⭐ Unique Shape Levels (350)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!isDailyMode) {
                    // Category Chips Filter Row (Scrollable)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp)
                    ) {
                        categories.forEach { (catKey, catLabel) ->
                            val isSelected = (selectedCategory == catKey)
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = if (isSelected) Color(0xFF6366F1) else Color(0xFFF1F5F9),
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .clickable { selectedCategory = catKey }
                            ) {
                                Text(
                                    text = catLabel,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name (Heart, Sphinx) or Level #", fontSize = 11.5.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isDailyMode) {
                    Text(
                        text = "Daily Challenge Key Milestones:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val dailyPresets = listOf(
                        1 to "Start (143 arr)",
                        25 to "Dense (126 arr)",
                        50 to "Dense (119 arr)",
                        100 to "Expert (126 arr)",
                        150 to "Expert (128 arr)",
                        200 to "Expert (116 arr)",
                        250 to "Dense (126 arr)",
                        235 to "Finale (140 arr)"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        dailyPresets.chunked(2).forEach { rowPair ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowPair.forEach { (lvl, desc) ->
                                    val isCurrent = (lvl == currentLevel)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCurrent) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isCurrent) Color(0xFF2563EB) else Color(0xFFE2E8F0)
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
                                                text = "D$lvl",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isCurrent) Color(0xFF1D4ED8) else Color(0xFF1E293B)
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
                } else {
                    // Filtered Unique Shapes List
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "⭐ Unique Shape Levels (${filteredShapes.size}):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = if (selectedCategory == "All") "100% Solvable" else selectedCategory,
                            fontSize = 11.sp,
                            color = Color(0xFF6366F1),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        items(filteredShapes.size) { i ->
                            val item = filteredShapes[i]
                            val isCurrent = (item.levelNumber == currentLevel)
                            val emoji = item.emoji ?: getShapeEmoji(item.shapeName ?: "")
                            Surface(
                                shape = RoundedCornerShape(12.dp),
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
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (item.difficulty) {
                                                "Easy" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                "Medium" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                                "Hard" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                            }
                                        ) {
                                            Text(
                                                text = "L${item.levelNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = when (item.difficulty) {
                                                    "Easy" -> Color(0xFF059669)
                                                    "Medium" -> Color(0xFF2563EB)
                                                    "Hard" -> Color(0xFFD97706)
                                                    else -> Color(0xFFDC2626)
                                                },
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "$emoji ${item.shapeName}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isCurrent) Color(0xFF4338CA) else Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = "${item.category ?: "Unique Shape"} • ${item.xSize}x${item.ySize}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${item.arrowCount} arr",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF475569)
                                        )
                                        if (onPlayInRegularGame != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { onPlayInRegularGame(item.levelNumber) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play in Regular Game",
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (false) {
                    // Legacy quick levels placeholder
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
                    val maxLevelNum = if (isDailyMode) LevelLoader.TOTAL_DAILY_LEVELS else LevelLoader.TOTAL_CAMPAIGN_LEVELS
                    OutlinedTextField(
                        value = inputLevelText,
                        onValueChange = { inputLevelText = it },
                        label = { Text(if (isDailyMode) "Daily Level (1-235)" else "Level # (1-${LevelLoader.TOTAL_CAMPAIGN_LEVELS})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val lvl = inputLevelText.toIntOrNull()?.coerceIn(1, maxLevelNum) ?: currentLevel
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
                                val lvl = inputLevelText.toIntOrNull()?.coerceIn(1, LevelLoader.TOTAL_CAMPAIGN_LEVELS) ?: currentLevel
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
