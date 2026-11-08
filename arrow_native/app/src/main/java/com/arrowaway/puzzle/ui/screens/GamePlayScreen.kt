package com.arrowaway.puzzle.ui.screens

import com.arrowaway.puzzle.BuildConfig
import com.arrowaway.puzzle.ui.components.AutoSolveButton

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import com.arrowaway.puzzle.ui.components.HeartIndicatorPill
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowaway.puzzle.core.model.Arrow
import com.arrowaway.puzzle.core.model.ArrowState
import com.arrowaway.puzzle.game.GameUiState
import com.arrowaway.puzzle.ui.components.BoosterBar
import com.arrowaway.puzzle.ui.components.CheerOverlay
import com.arrowaway.puzzle.ui.components.GameHeader
import com.arrowaway.puzzle.ui.components.GameOverDialog
import com.arrowaway.puzzle.ui.components.HintRefillDialog
import com.arrowaway.puzzle.ui.components.PauseDialog
import com.arrowaway.puzzle.ui.components.PuzzleBoardCanvas
import com.arrowaway.puzzle.ui.components.GameSettingsDialog
import com.arrowaway.puzzle.ui.screens.VictoryScreen
import com.arrowaway.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.delay

@Composable
fun GamePlayScreen(
    uiState: GameUiState,
    onArrowClick: (Arrow) -> Unit,
    onArrowLongPress: (Arrow, isPressed: Boolean) -> Unit,
    onHintClick: () -> Unit,
    onHammerClick: () -> Unit,
    onWandClick: () -> Unit,
    onGridClick: () -> Unit,
    onThemeSelect: (ThemePreset) -> Unit,
    onNextLevelClick: () -> Unit,
    onStartDailyChallengeClick: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onRestartClick: () -> Unit,
    onBackToHomeClick: () -> Unit,
    onClaimFreeLives: () -> Unit = {},
    onClaimAdHints: () -> Unit = {},
    onDismissHintAdPrompt: () -> Unit = {},
    onToggleSound: () -> Unit = {},
    onToggleVibration: () -> Unit = {},
    onToggleHintButton: () -> Unit = {},
    onToggleGridButton: () -> Unit = {},
    onHintTimerChange: (Int) -> Unit = {},
    onSettingsClick: (() -> Unit)? = null,
    onClaimDoubleCoins: (Int) -> Unit = {},
    onIdlePromptSound: () -> Unit = {},
    onPlayStarSound: (Int) -> Unit = {},
    onPlayCoinSound: () -> Unit = {},
    onPlayClickSound: () -> Unit = {},
    onDismissPreviewRays: () -> Unit = {},
    onAutoSolveClick: () -> Unit = {},
    onToggleHandGesture: () -> Unit = {},
    onToggleLandscapeMode: () -> Unit = {},
    onGameplayStarted: () -> Unit = {},
    onPauseStateChanged: (Boolean) -> Unit = {},
    onPrevShapeClick: () -> Unit = {},
    onNextShapeClick: () -> Unit = {},
    onSkipCelebration: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(uiState.currentLevelNumber, uiState.activeDailyDate) {
        onGameplayStarted()
    }
    var showPauseDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    LaunchedEffect(showPauseDialog, showSettingsDialog) {
        onPauseStateChanged(showPauseDialog || showSettingsDialog)
    }
    var hudToastMessage by remember { mutableStateOf<String?>(null) }
    var hudToastTimer by remember { mutableLongStateOf(0L) }

    // Intercept Android back gesture / button during gameplay
    BackHandler(enabled = true) {
        when {
            showSettingsDialog -> showSettingsDialog = false
            showPauseDialog -> showPauseDialog = false
            uiState.showHintAdPrompt -> onDismissHintAdPrompt()
            uiState.isGameOver -> onRestartClick()
            else -> showPauseDialog = true
        }
    }

    val level = uiState.levelData
    val resolvedShapeName = remember(uiState.isCustomShapeMode, level?.shapeName, uiState.activeCustomShapeFileName) {
        if (uiState.isCustomShapeMode) {
            val rawName = level?.shapeName?.takeIf { it.isNotBlank() }
                ?: uiState.activeCustomShapeFileName?.removeSuffix(".json")?.replace("_", " ")
                ?: "Shape"
            com.arrowaway.puzzle.core.engine.LevelLoader.resolveCustomShapeDisplayName(rawName)
        } else {
            level?.shapeName
        }
    }
    val theme = uiState.activeTheme
    val remainingArrowsCount = uiState.arrows.count { 
        it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT 
    }

    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isIdlePromptActive by remember { mutableStateOf(false) }
    var hasPlayedIdleSoundForPeriod by remember { mutableStateOf(false) }
    var prevHeartsRemaining by remember { mutableIntStateOf(uiState.heartsRemaining) }

    val isGameActivelyPlaying = !showPauseDialog && 
                                !showSettingsDialog && 
                                !uiState.isGamePaused && 
                                !uiState.isAppInBackground && 
                                !uiState.isLevelComplete && 
                                !uiState.isGameOver && 
                                remainingArrowsCount > 0

    // Heart loss / mistake triggers stuck assistance immediately during active play (only when hint button is visible and hint timer is not off)
    LaunchedEffect(uiState.heartsRemaining, isGameActivelyPlaying, uiState.isHintButtonVisible, uiState.hintTimerSeconds) {
        if (uiState.heartsRemaining < prevHeartsRemaining && isGameActivelyPlaying && uiState.isHintButtonVisible && uiState.hintTimerSeconds > 0) {
            isIdlePromptActive = true
        }
        prevHeartsRemaining = uiState.heartsRemaining
    }

    // Dynamically configurable idle timer triggers stuck assistance strictly during active gameplay (disabled when Hint Button is false or timer is Off)
    LaunchedEffect(lastInteractionTime, isGameActivelyPlaying, uiState.hintTimerSeconds, uiState.isHintButtonVisible) {
        if (!isGameActivelyPlaying || !uiState.isHintButtonVisible || uiState.hintTimerSeconds <= 0) {
            isIdlePromptActive = false
            hasPlayedIdleSoundForPeriod = false
            return@LaunchedEffect
        }
        isIdlePromptActive = false
        hasPlayedIdleSoundForPeriod = false
        delay(uiState.hintTimerSeconds * 1000L) // Configurable idle duration (default 10s, 0=Off, range 0-55s)
        if (isGameActivelyPlaying && uiState.isHintButtonVisible && uiState.hintTimerSeconds > 0) {
            isIdlePromptActive = true
        }
    }

    LaunchedEffect(isIdlePromptActive) {
        if (isIdlePromptActive && !hasPlayedIdleSoundForPeriod) {
            hasPlayedIdleSoundForPeriod = true
            onIdlePromptSound()
        }
    }

    // Quick HUD toast auto-dismiss timer
    LaunchedEffect(hudToastTimer) {
        if (hudToastTimer > 0L) {
            delay(1200L)
            hudToastMessage = null
        }
    }

    if (uiState.isLevelComplete) {
        val baseCoins = if (uiState.isDailyChallengeMode) 100 else 50 + (uiState.comboCount * 5)
        val isDailyUnlockMilestone = uiState.currentLevelNumber == 10 && 
            !uiState.isDailyChallengeMode && 
            !uiState.hasSeenDailyUnlockCelebration

        VictoryScreen(
            levelNumber = uiState.currentLevelNumber,
            coinsEarned = baseCoins,
            streakDays = uiState.streakDays,
            totalCoins = uiState.coins,
            theme = theme,
            isStreakEarnedToday = uiState.isStreakEarnedThisLevel,
            heartsRemaining = uiState.heartsRemaining,
            maxHearts = uiState.maxHearts,
            shapeName = resolvedShapeName,
            isDailyChallenge = uiState.isDailyChallengeMode,
            completionTimeSeconds = uiState.levelCompletionTimeSeconds,
            movesCount = uiState.levelMovesCount,
            score = uiState.levelFinalScore,
            accuracyPercent = uiState.levelAccuracyPercent,
            mistakesCount = uiState.levelMistakesCount,
            hintsUsed = uiState.levelHintsUsedCount,
            starsEarned = uiState.levelStarsEarned,
            isDailyUnlockMilestone = isDailyUnlockMilestone,
            onStartDailyChallengeClick = onStartDailyChallengeClick,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onClaimDoubleCoins = onClaimDoubleCoins,
            onNextLevelClick = onNextLevelClick,
            onHomeClick = onBackToHomeClick,
            onPlayStarSound = onPlayStarSound,
            onPlayCoinSound = onPlayCoinSound,
            onPlayClickSound = onPlayClickSound,
            modifier = modifier
        )
    } else {
        val isInitialLevel1 = uiState.currentLevelNumber == 1 && !uiState.isDailyChallengeMode && !uiState.isCustomShapeMode

        Surface(
            color = theme.background,
            modifier = modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                if (isLandscape) {
                    // =========================================================================
                    // LANDSCAPE AD RECORDING LAYOUT: Left Header | Center Canvas | Right Boosters
                    // =========================================================================
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .displayCutoutPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. LEFT SIDEBAR: Pause, Hearts & Shape Name, Settings Gear
                        Column(
                            modifier = Modifier
                                .widthIn(min = 68.dp, max = 80.dp)
                                .fillMaxHeight()
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top: Pause Pod
                            Surface(
                                shape = CircleShape,
                                color = theme.boosterBg,
                                border = BorderStroke(1.dp, theme.gridLine),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        onPlayClickSound()
                                        showPauseDialog = true
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = "Pause",
                                        tint = theme.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Middle: Hearts & Level/Shape Badge
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HeartIndicatorPill(
                                    heartsRemaining = uiState.heartsRemaining,
                                    maxHearts = uiState.maxHearts,
                                    theme = theme
                                )

                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = theme.textPrimary.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, theme.gridLine.copy(alpha = 0.45f)),
                                    shadowElevation = 0.dp
                                ) {
                                    Text(
                                        text = when {
                                            uiState.isDailyChallengeMode -> "Daily"
                                            uiState.isCustomShapeMode && resolvedShapeName != null -> resolvedShapeName
                                            else -> "Level ${uiState.currentLevelNumber}"
                                        },
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.textPrimary.copy(alpha = 0.85f),
                                        letterSpacing = 0.3.sp,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                if (uiState.isCustomShapeMode) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = theme.boosterBg,
                                            border = BorderStroke(1.dp, theme.gridLine),
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    onPlayClickSound()
                                                    onPrevShapeClick()
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Prev Shape",
                                                    tint = theme.textPrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = theme.boosterBg,
                                            border = BorderStroke(1.dp, theme.gridLine),
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    onPlayClickSound()
                                                    onNextShapeClick()
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "Next Shape",
                                                    tint = theme.textPrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Bottom: Settings Gear Pod
                            Surface(
                                shape = CircleShape,
                                color = theme.boosterBg,
                                border = BorderStroke(1.dp, theme.gridLine),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        onPlayClickSound()
                                        showSettingsDialog = true
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = theme.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // 2. CENTER STAGE: Full-Height Main Puzzle Canvas
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (level != null) {
                                PuzzleBoardCanvas(
                                    xSize = level.xSize,
                                    ySize = level.ySize,
                                    arrows = uiState.arrows,
                                    theme = theme,
                                    isGridLinesActive = uiState.isGridLinesActive,
                                    isCelebrationBurstActive = uiState.isCelebrationBurstActive,
                                    restartTrigger = uiState.restartTrigger,
                                    previewRayArrowIds = uiState.previewRayArrowIds,
                                    activeBoosterAnimation = uiState.activeBoosterAnimation,
                                    currentLevelNumber = uiState.currentLevelNumber,
                                    isDailyChallengeMode = uiState.isDailyChallengeMode,
                                    isCustomShapeMode = uiState.isCustomShapeMode,
                                    showTutorialHand = (uiState.currentLevelNumber == 1 && !uiState.isDailyChallengeMode && !uiState.isCustomShapeMode),
                                    isAutoSolving = uiState.isAutoSolving,
                                    isHandGestureEnabled = uiState.isHandGestureEnabled,
                                    onStopAutoSolve = onAutoSolveClick,
                                    onArrowClick = { arrow ->
                                        lastInteractionTime = System.currentTimeMillis()
                                        isIdlePromptActive = false
                                        onArrowClick(arrow)
                                    },
                                    onArrowLongPress = { arrow, isPressed ->
                                        lastInteractionTime = System.currentTimeMillis()
                                        isIdlePromptActive = false
                                        onArrowLongPress(arrow, isPressed)
                                    },
                                    onDismissPreviewRays = onDismissPreviewRays,
                                    onSkipCelebration = onSkipCelebration
                                )
                            }

                            CheerOverlay(
                                message = uiState.cheerMessage,
                                theme = uiState.activeTheme,
                                fromLeft = uiState.cheerFromLeft,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 4.dp)
                            )
                        }

                        // 3. RIGHT SIDEBAR: Vertical Booster Controls
                        Column(
                            modifier = Modifier
                                .widthIn(min = 60.dp, max = 74.dp)
                                .fillMaxHeight()
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (uiState.isHintButtonVisible) {
                                val effectiveHints = if (uiState.isDailyChallengeMode || uiState.isCustomShapeMode) {
                                    uiState.levelFreeHintsRemaining
                                } else if (uiState.currentLevelNumber <= 10) {
                                    999
                                } else {
                                    uiState.levelFreeHintsRemaining
                                }
                                val isFreeHint = (uiState.currentLevelNumber <= 10 && !uiState.isDailyChallengeMode && !uiState.isCustomShapeMode)
                                val isAdState = !isFreeHint && effectiveHints <= 0

                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clickable {
                                            lastInteractionTime = System.currentTimeMillis()
                                            isIdlePromptActive = false
                                            onHintClick()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isIdlePromptActive) Color(0xFFFEF3C7) else theme.boosterBg,
                                        border = BorderStroke(1.dp, if (isIdlePromptActive) Color(0xFFF59E0B) else theme.gridLine),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                imageVector = Icons.Default.Lightbulb,
                                                contentDescription = "Hint",
                                                tint = if (isIdlePromptActive) Color(0xFFD97706) else Color(0xFFF59E0B),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isAdState) Color(0xFFEAB308) else theme.boosterBg,
                                        border = BorderStroke(1.dp, if (isAdState) Color.White.copy(alpha = 0.8f) else theme.gridLine),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.TopEnd)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text(
                                                text = if (isFreeHint) "∞" else if (isAdState) "AD" else "$effectiveHints",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isAdState) Color.White else theme.textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            if (uiState.isGridButtonVisible) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (uiState.isGridLinesActive) theme.arrowDefault.copy(alpha = 0.18f) else theme.boosterBg,
                                    border = BorderStroke(1.dp, if (uiState.isGridLinesActive) theme.arrowDefault else theme.gridLine),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            lastInteractionTime = System.currentTimeMillis()
                                            isIdlePromptActive = false
                                            onGridClick()
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            text = "#",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (uiState.isGridLinesActive) theme.arrowDefault else theme.textPrimary.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // =========================================================================
                    // PORTRAIT LAYOUT: Top Header | Center Canvas | Bottom Boosters (Untouched)
                    // =========================================================================
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .displayCutoutPadding()
                    ) {
                    // 1. TOP GAMEPLAY HEADER (Hidden on Level 1 FTUE; Shown on all regular levels)
                    if (!isInitialLevel1 && level != null) {
                        val totalArrowsCount = uiState.levelData?.arrows?.size ?: uiState.arrows.size
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GameHeader(
                                levelNumber = uiState.currentLevelNumber,
                                heartsRemaining = uiState.heartsRemaining,
                                maxHearts = uiState.maxHearts,
                                remainingArrowsCount = remainingArrowsCount,
                                totalArrowsCount = totalArrowsCount,
                                theme = theme,
                                isDailyChallenge = uiState.isDailyChallengeMode,
                                dailyDateString = uiState.activeDailyDate,
                                shapeName = resolvedShapeName,
                                isTestMode = false,
                                showShapeNavigation = uiState.isCustomShapeMode,
                                onPrevShapeClick = {
                                    onPlayClickSound()
                                    onPrevShapeClick()
                                },
                                onNextShapeClick = {
                                    onPlayClickSound()
                                    onNextShapeClick()
                                },
                                showArrowCount = false,
                                showHearts = true,
                                onPauseClick = {
                                    onPlayClickSound()
                                    showPauseDialog = true
                                },
                                onSettingsClick = {
                                    onPlayClickSound()
                                    showSettingsDialog = true
                                },
                                modifier = Modifier.widthIn(max = 480.dp)
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
                        if (level != null) {
                            PuzzleBoardCanvas(
                                xSize = level.xSize,
                                ySize = level.ySize,
                                arrows = uiState.arrows,
                                theme = theme,
                                isGridLinesActive = uiState.isGridLinesActive,
                                isCelebrationBurstActive = uiState.isCelebrationBurstActive,
                                restartTrigger = uiState.restartTrigger,
                                previewRayArrowIds = uiState.previewRayArrowIds,
                                activeBoosterAnimation = uiState.activeBoosterAnimation,
                                currentLevelNumber = uiState.currentLevelNumber,
                                isDailyChallengeMode = uiState.isDailyChallengeMode,
                                isCustomShapeMode = uiState.isCustomShapeMode,
                                showTutorialHand = (uiState.currentLevelNumber == 1 && !uiState.isDailyChallengeMode && !uiState.isCustomShapeMode),
                                isAutoSolving = uiState.isAutoSolving,
                                isHandGestureEnabled = uiState.isHandGestureEnabled,
                                onStopAutoSolve = onAutoSolveClick,
                                onArrowClick = { arrow ->
                                    lastInteractionTime = System.currentTimeMillis()
                                    isIdlePromptActive = false
                                    onArrowClick(arrow)
                                },
                                onArrowLongPress = { arrow, isPressed ->
                                    lastInteractionTime = System.currentTimeMillis()
                                    isIdlePromptActive = false
                                    onArrowLongPress(arrow, isPressed)
                                },
                                onDismissPreviewRays = onDismissPreviewRays,
                                onSkipCelebration = onSkipCelebration
                            )
                        }

                        // Milestone notification banner positioned directly below milestone indicator
                        CheerOverlay(
                            message = uiState.cheerMessage,
                            theme = uiState.activeTheme,
                            fromLeft = uiState.cheerFromLeft,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 4.dp)
                        )

                        // Auto-Solve / Auto-Play is cleanly moved to Settings Menu for ad recording without on-screen clutter
                    }

                    // 3. BOTTOM BOOSTER CONTROLS (Hidden on initial Level 1 to keep gameplay focused & clean)
                    if (!isInitialLevel1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val effectiveHints = if (uiState.isDailyChallengeMode || uiState.isCustomShapeMode) {
                                uiState.levelFreeHintsRemaining
                            } else if (uiState.currentLevelNumber <= 10) {
                                999
                            } else {
                                uiState.levelFreeHintsRemaining
                            }
                            BoosterBar(
                                hintsCount = effectiveHints,
                                isGridLinesActive = uiState.isGridLinesActive,
                                isFreeHintLevel = (uiState.currentLevelNumber <= 10 && !uiState.isDailyChallengeMode && !uiState.isCustomShapeMode),
                                isIdlePromptActive = isIdlePromptActive,
                                levelNumber = if (uiState.isCustomShapeMode) null else uiState.currentLevelNumber,
                                shapeName = resolvedShapeName,
                                remainingArrowsCount = if (uiState.isCustomShapeMode) remainingArrowsCount else null,
                                isDailyChallenge = uiState.isDailyChallengeMode,
                                theme = theme,
                                isHintVisible = uiState.isHintButtonVisible,
                                isGridVisible = uiState.isGridButtonVisible,
                                onHintClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    isIdlePromptActive = false
                                    onHintClick()
                                },
                                onGridClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    isIdlePromptActive = false
                                    onGridClick()
                                },
                                modifier = Modifier.widthIn(max = 440.dp)
                            )
                        }
                    }
                }

                } // End of portrait Column

                // 4. SUBTLE HUD FLOATING TOAST (Shown on quick Sound toggle without modal interruption)
                AnimatedVisibility(
                    visible = hudToastMessage != null,
                    enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { -it / 2 },
                    exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { -it / 2 },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 70.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = if (theme.isDark) Color(0xFF1E293B).copy(alpha = 0.95f) else Color(0xFF0F172A).copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        shadowElevation = 8.dp
                    ) {
                        Text(
                            text = hudToastMessage ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
                }

                // 5. DEDICATED GAME PAUSED MODAL DIALOG
                if (showPauseDialog) {
                    PauseDialog(
                        activeTheme = theme,
                        onThemeSelect = onThemeSelect,
                        onRestartClick = {
                            showPauseDialog = false
                            onRestartClick()
                        },
                        onHomeClick = {
                            showPauseDialog = false
                            onBackToHomeClick()
                        },
                        onDismiss = {
                            showPauseDialog = false
                        }
                    )
                }

                // 6. DEDICATED IN-GAME SETTINGS MODAL DIALOG
                if (showSettingsDialog) {
                    GameSettingsDialog(
                        isSoundEnabled = uiState.isSoundEnabled,
                        isVibrationEnabled = uiState.isVibrationEnabled,
                        isHintButtonVisible = uiState.isHintButtonVisible,
                        isGridButtonVisible = uiState.isGridButtonVisible,
                        hintTimerSeconds = uiState.hintTimerSeconds,
                        activeTheme = theme,
                        isAutoSolving = uiState.isAutoSolving,
                        isHandGestureEnabled = uiState.isHandGestureEnabled,
                        onToggleSound = onToggleSound,
                        onToggleVibration = onToggleVibration,
                        onToggleHintButton = onToggleHintButton,
                        onToggleGridButton = onToggleGridButton,
                        onHintTimerChange = onHintTimerChange,
                        onThemeSelect = onThemeSelect,
                        onToggleAutoSolve = onAutoSolveClick,
                        onToggleHandGesture = onToggleHandGesture,
                        onToggleLandscapeMode = onToggleLandscapeMode,
                        isLandscapeModeEnabled = uiState.isLandscapeModeEnabled,
                        onDismiss = {
                            showSettingsDialog = false
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    )
                }

                // 6. GAME OVER DIALOG
                if (uiState.isGameOver) {
                    val isStartingLevels = (uiState.currentLevelNumber <= 10 && !uiState.isDailyChallengeMode)
                    GameOverDialog(
                        theme = theme,
                        isStartingLevels = isStartingLevels,
                        hasGlobalLives = false,
                        onClaimLives = onClaimFreeLives,
                        onRestartClick = onRestartClick
                    )
                }

                // 7. HINT REFILL REWARDED AD DIALOG (Level 11+)
                if (uiState.showHintAdPrompt) {
                    HintRefillDialog(
                        theme = theme,
                        onWatchAdClick = onClaimAdHints,
                        onDismiss = onDismissHintAdPrompt
                    )
                }
            }
        }
    }
}
