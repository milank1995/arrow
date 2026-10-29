package com.arrowescape.puzzle.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import com.arrowescape.puzzle.core.engine.TestLevelCategory
import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.ArrowState
import com.arrowescape.puzzle.core.model.LevelData
import com.arrowescape.puzzle.ui.components.BoosterBar
import com.arrowescape.puzzle.ui.components.CheerOverlay
import com.arrowescape.puzzle.ui.components.GameHeader
import com.arrowescape.puzzle.ui.components.PuzzleBoardCanvas
import com.arrowescape.puzzle.ui.components.ThemeCustomizationDialog
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DevTestScreen(
    initialCategory: TestLevelCategory = TestLevelCategory.EXISTING_1K,
    theme: ThemePreset,
    audioHapticManager: AudioHapticManager,
    onBackToHomeClick: () -> Unit,
    onThemeSelect: (ThemePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    BackHandler(onBack = onBackToHomeClick)

    var currentCategory by remember { mutableStateOf(initialCategory) }
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

    var showLevelSelectDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    fun loadLevel(levelNum: Int, category: TestLevelCategory = currentCategory) {
        val maxLevels = category.totalLevels
        val boundedLevel = levelNum.coerceIn(1, maxLevels)
        val level = LevelLoader.loadTestLevel(context, category, boundedLevel)
        currentCategory = category
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
        previewRayArrowIds = emptySet()
        restartTrigger = System.currentTimeMillis()
    }

    LaunchedEffect(initialCategory) {
        loadLevel(1, initialCategory)
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
            val durationMs = 820L
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val rawProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                val easedProgress = rawProgress * rawProgress * (3f - 2f * rawProgress)
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

        previewRayArrowIds = emptySet()

        if (isGridLinesActive) {
            isGridLinesActive = false
        }

        audioHapticManager.playTap()

        val canEscape = PuzzleSolver.canMoveOut(currentArrow, occupancyMap)

        if (canEscape) {
            launchArrowFlight(currentArrow)
        } else {
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
            // 1. TOP GAMEPLAY HEADER & CATEGORY BAR
            if (currentLevel != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .widthIn(max = 540.dp)
                ) {
                    GameHeader(
                        levelNumber = currentLevelNumber,
                        heartsRemaining = 3,
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
                            if (currentLevelNumber < currentCategory.totalLevels) {
                                loadLevel(currentLevelNumber + 1)
                            }
                        },
                        onLevelNumberClick = {
                            showLevelSelectDialog = true
                        },
                        onBackClick = onBackToHomeClick,
                        onThemeClick = { showThemeDialog = true }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // CATEGORY SWITCHER TABS
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        TestLevelCategory.entries.forEach { cat ->
                            val isSelected = (cat == currentCategory)
                            val shortLabel = when (cat) {
                                TestLevelCategory.EXISTING_1K -> "📁 1K Existing"
                                TestLevelCategory.UNITY_NORMAL -> "🎮 Unity Normal"
                                TestLevelCategory.SHAPE_PUZZLES -> "⭐ Shape Puzzles"
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) theme.arrowDefault else Color(0x14000000),
                                border = if (isSelected) null else BorderStroke(1.dp, Color(0x1F000000)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (currentCategory != cat) {
                                            audioHapticManager.playTap()
                                            loadLevel(1, cat)
                                        }
                                    }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = shortLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else theme.textPrimary
                                    )
                                    Text(
                                        text = "${cat.totalLevels} Lvls",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else theme.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // LEVEL METRICS BADGE (Arrow count, Grid size, Category progress, Shape name)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x0F000000),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            val shapePrefix = if (currentLevel.shapeName.isNotEmpty()) {
                                "🐾 ${currentLevel.shapeName}  •  "
                            } else ""
                            Text(
                                text = "${shapePrefix}Lvl $currentLevelNumber of ${currentCategory.totalLevels}  •  ${arrows.size} Arrows  •  Grid ${currentLevel.xSize}×${currentLevel.ySize}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.textPrimary.copy(alpha = 0.85f)
                            )
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
                        showTutorialHand = false, // Never show tutorial hand gesture during test mode
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

                // Floating Shape Name Badge in Game Play Area
                if (currentLevel != null && currentLevel.shapeName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = theme.surface.copy(alpha = 0.92f),
                        border = BorderStroke(1.2.dp, theme.arrowDefault.copy(alpha = 0.40f)),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "🐾",
                                fontSize = 13.sp
                            )
                            Text(
                                text = currentLevel.shapeName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                CheerOverlay(
                    message = cheerMessage,
                    fromLeft = cheerFromLeft,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (currentLevel?.shapeName?.isNotBlank() == true) 48.dp else 8.dp)
                )
            }

            // 3. BOTTOM BOOSTER CONTROLS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                BoosterBar(
                    hintsCount = 999,
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

        // VICTORY SCREEN
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
                    if (currentLevelNumber < currentCategory.totalLevels) {
                        loadLevel(currentLevelNumber + 1)
                    } else {
                        loadLevel(1)
                    }
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

        // LEVEL SELECTOR MODAL
        if (showLevelSelectDialog) {
            LevelSelectorDialog(
                currentCategory = currentCategory,
                currentLevel = currentLevelNumber,
                onCategorySelected = { cat ->
                    loadLevel(1, cat)
                },
                onLevelSelected = { lvl, cat ->
                    showLevelSelectDialog = false
                    loadLevel(lvl, cat)
                },
                onDismiss = { showLevelSelectDialog = false }
            )
        }
    }
}

@Composable
private fun LevelSelectorDialog(
    currentCategory: TestLevelCategory,
    currentLevel: Int,
    onCategorySelected: (TestLevelCategory) -> Unit,
    onLevelSelected: (Int, TestLevelCategory) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(currentCategory) }
    var inputLevelText by remember { mutableStateOf(currentLevel.toString()) }
    val maxLimit = selectedCategory.totalLevels

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
                    Column {
                        Text(
                            text = "Select Test Level",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "${selectedCategory.title} (${selectedCategory.totalLevels} Total)",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // CATEGORY SELECTOR CHIPS INSIDE DIALOG
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TestLevelCategory.entries.forEach { cat ->
                        val isCatSelected = (cat == selectedCategory)
                        val catShort = when (cat) {
                            TestLevelCategory.EXISTING_1K -> "📁 1K Old"
                            TestLevelCategory.UNITY_NORMAL -> "🎮 Normal"
                            TestLevelCategory.SHAPE_PUZZLES -> "⭐ Shapes"
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCatSelected) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedCategory = cat
                                    inputLevelText = "1"
                                    onCategorySelected(cat)
                                }
                        ) {
                            Text(
                                text = catShort,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = if (isCatSelected) Color.White else Color(0xFF475569),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            )
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
                        label = { Text("Enter Level # (1-$maxLimit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val parsed = inputLevelText.replace(",", "").replace(".", "").replace(" ", "").trim().toIntOrNull()
                            val lvl = parsed?.coerceIn(1, maxLimit) ?: 1
                            onLevelSelected(lvl, selectedCategory)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Load", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Quick Milestone Jumps (Smallest → Largest):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val quickLevels = when (selectedCategory) {
                    TestLevelCategory.EXISTING_1K -> listOf(
                        1 to "Tutorial (3 arr)",
                        50 to "Early (28 arr)",
                        100 to "Flow (45 arr)",
                        250 to "Mid (65 arr)",
                        500 to "Milestone (100 arr)",
                        750 to "Complex (130 arr)",
                        1001 to "Finale (229 arr)"
                    )
                    TestLevelCategory.UNITY_NORMAL -> listOf(
                        1 to "Intro (4 arr)",
                        500 to "Early (25 arr)",
                        1500 to "Growing (40 arr)",
                        3000 to "Medium (60 arr)",
                        5000 to "Dense (90 arr)",
                        7000 to "Expert (130 arr)",
                        8445 to "Apex (223 arr)"
                    )
                    TestLevelCategory.SHAPE_PUZZLES -> listOf(
                        1 to "Intro Shape (19 arr)",
                        100 to "Early (50 arr)",
                        250 to "Mid (75 arr)",
                        400 to "Solid (100 arr)",
                        600 to "Dense (130 arr)",
                        750 to "Grand (180 arr)",
                        800 to "Sphinx (437 arr)"
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickLevels.chunked(2).forEach { rowPair ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowPair.forEach { (lvl, desc) ->
                                val isCurrent = (lvl == currentLevel && selectedCategory == currentCategory)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCurrent) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCurrent) Color(0xFF6366F1) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onLevelSelected(lvl, selectedCategory) }
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
        }
    }
}
