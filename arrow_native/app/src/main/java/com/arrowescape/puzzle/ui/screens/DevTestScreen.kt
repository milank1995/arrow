package com.arrowescape.puzzle.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.arrowescape.puzzle.audio.AudioHapticManager
import com.arrowescape.puzzle.core.engine.DevTestLayoutGenerator
import com.arrowescape.puzzle.core.engine.DevTestPreset
import com.arrowescape.puzzle.core.engine.GridOccupancyMap
import com.arrowescape.puzzle.core.engine.PuzzleSolver
import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.ArrowState
import com.arrowescape.puzzle.core.model.LevelData
import com.arrowescape.puzzle.ui.components.PuzzleBoardCanvas
import com.arrowescape.puzzle.ui.components.ThemeCustomizationDialog
import com.arrowescape.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.Job
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

    // Active Layout & State
    var activePreset by remember { mutableStateOf(DevTestPreset.MICRO_SWARM) }
    var currentSeed by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var authenticLevelInput by remember { mutableIntStateOf(100) }
    var customWidth by remember { mutableIntStateOf(10) }
    var customHeight by remember { mutableIntStateOf(10) }
    var customArrowCount by remember { mutableIntStateOf(35) }

    var levelData by remember { mutableStateOf<LevelData?>(null) }
    var arrows by remember { mutableStateOf<List<Arrow>>(emptyList()) }
    var occupancyMap by remember { mutableStateOf(GridOccupancyMap(10, 10)) }

    // UI & Diagnostic State
    var previewRayArrowId by remember { mutableStateOf<String?>(null) }
    var isGridLinesActive by remember { mutableStateOf(false) }
    var isCelebrationBurstActive by remember { mutableStateOf(false) }
    var isLevelComplete by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showHud by remember { mutableStateOf(true) }
    var showAllMovableHighlights by remember { mutableStateOf(false) }

    // Interaction Logging
    var totalClicks by remember { mutableIntStateOf(0) }
    var successfulEscapes by remember { mutableIntStateOf(0) }
    var collisionMistakes by remember { mutableIntStateOf(0) }
    var lastInteractedArrow by remember { mutableStateOf<Arrow?>(null) }
    var lastClickEventLog by remember { mutableStateOf("Tap any arrow to test") }

    // Helper to generate or load level
    fun loadTestLevel(preset: DevTestPreset, seed: Long = System.currentTimeMillis()) {
        currentSeed = seed
        val level = DevTestLayoutGenerator.generatePreset(
            preset = preset,
            context = context,
            seed = seed,
            customWidth = customWidth,
            customHeight = customHeight,
            customArrowCount = customArrowCount,
            authenticLevelNum = authenticLevelInput
        )
        levelData = level
        arrows = level.arrows
        occupancyMap = GridOccupancyMap(level.xSize, level.ySize).apply {
            rebuild(level.arrows)
        }
        isLevelComplete = false
        isCelebrationBurstActive = false
        totalClicks = 0
        successfulEscapes = 0
        collisionMistakes = 0
        lastInteractedArrow = null
        lastClickEventLog = "Loaded ${preset.title} (${level.arrows.size} arrows)"
    }

    // Initial load
    LaunchedEffect(Unit) {
        loadTestLevel(activePreset, currentSeed)
    }

    // Arrow flight execution
    fun executeArrowFlight(target: Arrow) {
        audioHapticManager.playArrowFly(successfulEscapes + 1)
        occupancyMap.release(target)

        arrows = arrows.map {
            if (it.id == target.id) it.copyWithState(ArrowState.FLYING_OUT, highlighted = false) else it
        }

        coroutineScope.launch {
            val durationMs = 480L
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val rawProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                val easedProgress = rawProgress * (0.35f + 0.65f * rawProgress)
                arrows = arrows.map {
                    if (it.id == target.id && it.state == ArrowState.FLYING_OUT) {
                        it.copy(flightProgress = easedProgress)
                    } else it
                }
                if (rawProgress >= 1f) break
                delay(16)
            }

            val finalArrows = arrows.map {
                if (it.id == target.id) it.copyWithState(ArrowState.CLEARED, highlighted = false) else it
            }
            arrows = finalArrows

            val remaining = finalArrows.count { it.state != ArrowState.CLEARED }
            if (remaining == 0) {
                isCelebrationBurstActive = true
                audioHapticManager.playLevelWin()
                delay(900)
                isLevelComplete = true
                isCelebrationBurstActive = false
            }
        }
    }

    // Handle arrow click in Dev Test Mode
    fun onArrowClicked(clickedArrow: Arrow) {
        val currentArrow = arrows.firstOrNull { it.id == clickedArrow.id } ?: clickedArrow
        if (currentArrow.state == ArrowState.CLEARED || currentArrow.state == ArrowState.FLYING_OUT) return

        totalClicks++
        lastInteractedArrow = currentArrow
        audioHapticManager.playTap()

        val canEscape = PuzzleSolver.canMoveOut(currentArrow, occupancyMap)

        if (canEscape) {
            successfulEscapes++
            lastClickEventLog = "✓ Cleared ${currentArrow.id} (len=${currentArrow.body.size}, bends=${currentArrow.bendCount})"
            executeArrowFlight(currentArrow)
        } else {
            collisionMistakes++
            val blocker = PuzzleSolver.findBlockingArrow(currentArrow, occupancyMap)
            val blockerDesc = if (blocker != null) blocker.id else "obstacle"
            lastClickEventLog = "✕ Collision! ${currentArrow.id} hit by $blockerDesc"

            audioHapticManager.playWrongMove()

            // Bounce recoil animation (UNLIMITED LIVES: No hearts subtracted!)
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

    val remainingCount = arrows.count { it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT }
    val totalCount = levelData?.arrows?.size ?: arrows.size
    val movableNowCount = arrows.count { it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT && PuzzleSolver.canMoveOut(it, occupancyMap) }

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
            // 1. DEV TOP BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Back Button Pod
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = theme.boosterBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                        shadowElevation = 1.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(
                            onClick = onBackToHomeClick,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit Test Lab",
                                tint = theme.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Test Lab Badge + Preset Name Pill
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = Color(0xFFFEF3C7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .height(38.dp)
                            .clickable { showPresetDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(text = "🧪", fontSize = 14.sp)
                            Text(
                                text = activePreset.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                text = "▼",
                                fontSize = 9.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }

                    // Right Pods: Theme + HUD Toggle
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Unlimited Lives Indicator Pill
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = Color(0xFFFFECEF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB3BA)),
                            shadowElevation = 1.dp,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(text = "❤️", fontSize = 13.sp)
                                Text(
                                    text = "∞ Lives",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }

                        // Theme selector button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = theme.boosterBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                            shadowElevation = 1.dp,
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(
                                onClick = { showThemeDialog = true },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Themes",
                                    tint = theme.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Collapsible Diagnostics HUD Strip
                AnimatedVisibility(visible = showHud) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = theme.boosterBg.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.gridLine),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Grid: ${levelData?.xSize ?: 0}x${levelData?.ySize ?: 0}  |  Left: $remainingCount/$totalCount  |  Movable: $movableNowCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = theme.textPrimary
                                )
                                Text(
                                    text = "Hits: $successfulEscapes  Miss: $collisionMistakes",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF059669)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = lastClickEventLog,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                color = if (lastClickEventLog.startsWith("✓")) Color(0xFF059669) else if (lastClickEventLog.startsWith("✕")) Color(0xFFDC2626) else theme.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 2. MAIN PUZZLE BOARD CANVAS
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val currentLevel = levelData
                if (currentLevel != null) {
                    // Mark all movable arrows with subtle preview if showAllMovableHighlights is toggled on
                    val renderedArrows = if (showAllMovableHighlights) {
                        arrows.map { a ->
                            val isFree = a.state != ArrowState.CLEARED && a.state != ArrowState.FLYING_OUT && PuzzleSolver.canMoveOut(a, occupancyMap)
                            if (isFree && a.state == ArrowState.IDLE) a.copyWithState(ArrowState.HINTED) else a
                        }
                    } else {
                        arrows
                    }

                    PuzzleBoardCanvas(
                        xSize = currentLevel.xSize,
                        ySize = currentLevel.ySize,
                        arrows = renderedArrows,
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
            }

            // 3. DEV TEST TOOLBAR
            Surface(
                color = theme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Free Hint Button
                    DevToolButton(
                        icon = Icons.Default.Lightbulb,
                        label = "Hint",
                        badge = "Free",
                        tint = Color(0xFFF59E0B),
                        onClick = {
                            val hintTarget = PuzzleSolver.getBestHint(arrows, occupancyMap)
                            if (hintTarget != null) {
                                audioHapticManager.playTap()
                                arrows = arrows.map {
                                    if (it.id == hintTarget.id) it.copyWithState(ArrowState.HINTED) else it
                                }
                                lastClickEventLog = "💡 Hint: Suggested ${hintTarget.id}"
                            }
                        }
                    )

                    // Show Movable Toggle
                    DevToolButton(
                        icon = if (showAllMovableHighlights) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        label = if (showAllMovableHighlights) "Hide Movable" else "Show Movable",
                        badge = "$movableNowCount",
                        tint = if (showAllMovableHighlights) Color(0xFF10B981) else theme.textPrimary,
                        onClick = {
                            showAllMovableHighlights = !showAllMovableHighlights
                        }
                    )

                    // Grid Lines Toggle
                    DevToolButton(
                        icon = Icons.Default.GridOn,
                        label = "Grid",
                        tint = if (isGridLinesActive) Color(0xFF3B82F6) else theme.textSecondary,
                        onClick = {
                            isGridLinesActive = !isGridLinesActive
                        }
                    )

                    // Replay Current Layout
                    DevToolButton(
                        icon = Icons.Default.Refresh,
                        label = "Restart",
                        tint = theme.textPrimary,
                        onClick = {
                            audioHapticManager.playTap()
                            loadTestLevel(activePreset, currentSeed)
                        }
                    )

                    // Generate New Seed
                    DevToolButton(
                        icon = Icons.Default.Casino,
                        label = "New Seed",
                        tint = Color(0xFF8B5CF6),
                        onClick = {
                            audioHapticManager.playTap()
                            loadTestLevel(activePreset, System.currentTimeMillis())
                        }
                    )

                    // Preset Selector
                    DevToolButton(
                        icon = Icons.AutoMirrored.Filled.List,
                        label = "Presets",
                        tint = Color(0xFF0284C7),
                        onClick = {
                            showPresetDialog = true
                        }
                    )
                }
            }
        }

        // PRESET SELECTION DIALOG
        if (showPresetDialog) {
            PresetSelectorDialog(
                currentPreset = activePreset,
                customWidth = customWidth,
                customHeight = customHeight,
                customArrowCount = customArrowCount,
                authenticLevel = authenticLevelInput,
                onPresetSelected = { preset ->
                    activePreset = preset
                    showPresetDialog = false
                    loadTestLevel(preset, System.currentTimeMillis())
                },
                onCustomParamsChanged = { w, h, count ->
                    customWidth = w
                    customHeight = h
                    customArrowCount = count
                    activePreset = DevTestPreset.CUSTOM_GRID
                    showPresetDialog = false
                    loadTestLevel(DevTestPreset.CUSTOM_GRID, System.currentTimeMillis())
                },
                onAuthenticLevelSelected = { lvl ->
                    authenticLevelInput = lvl
                    activePreset = DevTestPreset.AUTHENTIC_MASTER
                    showPresetDialog = false
                    loadTestLevel(DevTestPreset.AUTHENTIC_MASTER, System.currentTimeMillis())
                },
                onDismiss = { showPresetDialog = false }
            )
        }

        // THEME SELECTION DIALOG
        if (showThemeDialog) {
            ThemeCustomizationDialog(
                activeTheme = theme,
                onThemeSelect = onThemeSelect,
                onDismiss = { showThemeDialog = false }
            )
        }

        // VICTORY MODAL (TEST MODE)
        if (isLevelComplete) {
            AlertDialog(
                onDismissRequest = { isLevelComplete = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎉 ", fontSize = 24.sp)
                        Text(
                            text = "Test Completed!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "All ${totalCount} arrows were successfully tested and cleared!",
                            fontSize = 14.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Total Taps: $totalClicks",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Success Escapes: $successfulEscapes",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF059669)
                                )
                                Text(
                                    text = "Collision Checks: $collisionMistakes",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isLevelComplete = false
                            loadTestLevel(activePreset, System.currentTimeMillis())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("Next Random Grid")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            isLevelComplete = false
                            loadTestLevel(activePreset, currentSeed)
                        }
                    ) {
                        Text("Replay Same Grid")
                    }
                }
            )
        }
    }
}

@Composable
private fun DevToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    badge: String? = null,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = tint,
                    modifier = Modifier.padding(top = 0.dp, end = 0.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF475569)
        )
    }
}

@Composable
private fun PresetSelectorDialog(
    currentPreset: DevTestPreset,
    customWidth: Int,
    customHeight: Int,
    customArrowCount: Int,
    authenticLevel: Int,
    onPresetSelected: (DevTestPreset) -> Unit,
    onCustomParamsChanged: (Int, Int, Int) -> Unit,
    onAuthenticLevelSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(currentPreset) }
    var widthInput by remember { mutableStateOf(customWidth.toString()) }
    var heightInput by remember { mutableStateOf(customHeight.toString()) }
    var arrowCountInput by remember { mutableStateOf(customArrowCount.toString()) }
    var levelNumberInput by remember { mutableStateOf(authenticLevel.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🛠️ Test Grid Presets",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val presets = listOf(
                        DevTestPreset.MICRO_SWARM,
                        DevTestPreset.MEGA_GRID,
                        DevTestPreset.SERPENTINE_MAZE,
                        DevTestPreset.RAPID_FIRE
                    )

                    items(presets) { preset ->
                        val isSelected = (preset == currentPreset)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) Color(0xFF6366F1) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPresetSelected(preset) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(text = preset.icon, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color(0xFF4338CA) else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Authentic Level Selection Row
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentPreset == DevTestPreset.AUTHENTIC_MASTER) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (currentPreset == DevTestPreset.AUTHENTIC_MASTER) Color(0xFF6366F1) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🏆", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Authentic Production Level",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "Load real levels from assets (1 to 1534)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Quick Level chips
                                    listOf(50, 100, 250, 500, 1000).forEach { lvl ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFE2E8F0),
                                            modifier = Modifier.clickable { onAuthenticLevelSelected(lvl) }
                                        ) {
                                            Text(
                                                text = "L$lvl",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF334155),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = levelNumberInput,
                                        onValueChange = { levelNumberInput = it },
                                        label = { Text("Level #") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            val lvl = levelNumberInput.toIntOrNull()?.coerceIn(1, 1534) ?: 100
                                            onAuthenticLevelSelected(lvl)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                    ) {
                                        Text("Load")
                                    }
                                }
                            }
                        }
                    }

                    // Custom Grid Configuration Row
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentPreset == DevTestPreset.CUSTOM_GRID) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (currentPreset == DevTestPreset.CUSTOM_GRID) Color(0xFF6366F1) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "⚙️", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Custom Procedural Generator",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "Set dimensions (5-16) and arrow density",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = widthInput,
                                        onValueChange = { widthInput = it },
                                        label = { Text("Width") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = heightInput,
                                        onValueChange = { heightInput = it },
                                        label = { Text("Height") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = arrowCountInput,
                                        onValueChange = { arrowCountInput = it },
                                        label = { Text("Arrows") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        val w = widthInput.toIntOrNull()?.coerceIn(5, 16) ?: 10
                                        val h = heightInput.toIntOrNull()?.coerceIn(5, 16) ?: 10
                                        val count = arrowCountInput.toIntOrNull()?.coerceIn(5, 75) ?: 35
                                        onCustomParamsChanged(w, h, count)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Generate Custom Grid")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
