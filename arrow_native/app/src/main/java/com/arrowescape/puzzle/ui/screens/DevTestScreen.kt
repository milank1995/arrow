package com.arrowescape.puzzle.ui.screens

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
import com.arrowescape.puzzle.ui.screens.VictoryScreen
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DevTestScreen(
    theme: ThemePreset,
    audioHapticManager: AudioHapticManager,
    onBackToHomeClick: () -> Unit,
    onThemeSelect: (ThemePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentLevelNumber by remember { mutableIntStateOf(50) } // Default to a dense, challenging level
    var levelData by remember { mutableStateOf<LevelData?>(null) }
    var arrows by remember { mutableStateOf<List<Arrow>>(emptyList()) }
    var occupancyMap by remember { mutableStateOf(GridOccupancyMap(4, 4)) }

    var isGridLinesActive by remember { mutableStateOf(false) }
    var isCelebrationBurstActive by remember { mutableStateOf(false) }
    var previewRayArrowId by remember { mutableStateOf<String?>(null) }
    var cheerMessage by remember { mutableStateOf<String?>(null) }
    var cheerFromLeft by remember { mutableStateOf(true) }
    var comboCount by remember { mutableIntStateOf(0) }
    var isLevelComplete by remember { mutableStateOf(false) }

    var showLevelSelectDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    fun loadLevel(levelNum: Int) {
        val boundedLevel = levelNum.coerceIn(1, 1001)
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
        previewRayArrowId = null
    }

    LaunchedEffect(Unit) {
        loadLevel(currentLevelNumber)
    }

    fun launchArrowFlight(arrow: Arrow) {
        val newCombo = comboCount + 1
        comboCount = newCombo
        audioHapticManager.playArrowFly(newCombo)

        val cheer = when (newCombo) {
            3 -> "Nice!"
            5 -> "Great!"
            8 -> "Awesome!"
            12 -> "Unstoppable!"
            16 -> "Masterful!"
            else -> null
        }
        cheerMessage = cheer
        if (cheer != null) cheerFromLeft = !cheerFromLeft

        occupancyMap.release(arrow)

        arrows = arrows.map {
            if (it.id == arrow.id) it.copyWithState(ArrowState.FLYING_OUT, highlighted = false) else it
        }

        coroutineScope.launch {
            val durationMs = 520L
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val rawProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                val easedProgress = rawProgress * (0.35f + 0.65f * rawProgress)
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

            arrows = arrows.map {
                if (it.id == currentArrow.id) it.copyWithState(ArrowState.WRONG_FLASH, highlighted = true) else it
            }

            coroutineScope.launch {
                val durationMs = 280L
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
                        isTestMode = true,
                        onPrevLevelClick = {
                            if (currentLevelNumber > 1) {
                                loadLevel(currentLevelNumber - 1)
                            }
                        },
                        onNextLevelClick = {
                            if (currentLevelNumber < 1001) {
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
                        previewRayArrowId = previewRayArrowId,
                        onArrowClick = { arrow ->
                            onArrowClicked(arrow)
                        },
                        onArrowLongPress = { arrow, isPressed ->
                            previewRayArrowId = if (isPressed) arrow.id else null
                        }
                    )
                }

                CheerOverlay(
                    message = cheerMessage,
                    fromLeft = cheerFromLeft,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
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
                onDismiss = { showLevelSelectDialog = false }
            )
        }
    }
}

@Composable
private fun LevelSelectorDialog(
    currentLevel: Int,
    onLevelSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var inputLevelText by remember { mutableStateOf(currentLevel.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
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
                        text = "Select Level to Test",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Text(
                    text = "Choose an authentic original game puzzle (1 to 1001):",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Quick presets for varied grid sizes and complexities
                Text(
                    text = "Curated Layout Complexities:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val quickLevels = listOf(
                    1 to "Intro (5x5)",
                    5 to "Simple (5x5)",
                    20 to "Medium (6x6)",
                    50 to "Dense (20x24)",
                    100 to "Labyrinth (45 arr)",
                    500 to "Master (50+ arr)",
                    999 to "Mega (37x53, 80+)",
                    1000 to "Spiral Milestone",
                    1001 to "Final Labyrinth (81)"
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
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "L$lvl",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isCurrent) Color(0xFF4338CA) else Color(0xFF1E293B)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = desc,
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
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

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Level Number Input
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputLevelText,
                        onValueChange = { inputLevelText = it },
                        label = { Text("Level # (1-1001)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val lvl = inputLevelText.toIntOrNull()?.coerceIn(1, 1001) ?: currentLevel
                            onLevelSelected(lvl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Load", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
