package com.arrowescape.puzzle.ui.screens

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
import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.ArrowState
import com.arrowescape.puzzle.game.GameUiState
import com.arrowescape.puzzle.ui.components.BoosterBar
import com.arrowescape.puzzle.ui.components.CheerOverlay
import com.arrowescape.puzzle.ui.components.GameHeader
import com.arrowescape.puzzle.ui.components.GameOverDialog
import com.arrowescape.puzzle.ui.components.HintRefillDialog
import com.arrowescape.puzzle.ui.components.PauseDialog
import com.arrowescape.puzzle.ui.components.PuzzleBoardCanvas
import com.arrowescape.puzzle.ui.components.SoundSettingsDialog
import com.arrowescape.puzzle.ui.screens.VictoryScreen
import com.arrowescape.puzzle.ui.theme.ThemePreset
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
    onSettingsClick: (() -> Unit)? = null,
    onClaimDoubleCoins: (Int) -> Unit = {},
    onIdlePromptSound: () -> Unit = {},
    onPlayStarSound: (Int) -> Unit = {},
    onPlayCoinSound: () -> Unit = {},
    onPlayClickSound: () -> Unit = {},
    onDismissPreviewRays: () -> Unit = {},
    onGameplayStarted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(uiState.currentLevelNumber, uiState.activeDailyDate) {
        onGameplayStarted()
    }
    var showPauseDialog by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var hudToastMessage by remember { mutableStateOf<String?>(null) }
    var hudToastTimer by remember { mutableLongStateOf(0L) }

    // Intercept Android back gesture / button during gameplay
    BackHandler(enabled = true) {
        when {
            showSoundDialog -> showSoundDialog = false
            showPauseDialog -> showPauseDialog = false
            uiState.showHintAdPrompt -> onDismissHintAdPrompt()
            uiState.isGameOver -> onRestartClick()
            else -> showPauseDialog = true
        }
    }

    val level = uiState.levelData
    val theme = uiState.activeTheme
    val remainingArrowsCount = uiState.arrows.count { 
        it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT 
    }

    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isIdlePromptActive by remember { mutableStateOf(false) }
    var hasPlayedIdleSoundForPeriod by remember { mutableStateOf(false) }
    var prevHeartsRemaining by remember { mutableIntStateOf(uiState.heartsRemaining) }

    // Heart loss / mistake triggers stuck assistance immediately
    LaunchedEffect(uiState.heartsRemaining) {
        if (uiState.heartsRemaining < prevHeartsRemaining) {
            isIdlePromptActive = true
        }
        prevHeartsRemaining = uiState.heartsRemaining
    }

    // 8-second idle timer triggers stuck assistance
    LaunchedEffect(lastInteractionTime, remainingArrowsCount, uiState.isLevelComplete, uiState.isGameOver) {
        if (uiState.isLevelComplete || uiState.isGameOver || remainingArrowsCount == 0) {
            isIdlePromptActive = false
            return@LaunchedEffect
        }
        isIdlePromptActive = false
        hasPlayedIdleSoundForPeriod = false
        delay(8000L) // 8s idle period
        isIdlePromptActive = true
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
        val isDailyUnlockMilestone = uiState.currentLevelNumber == 5 && 
            !uiState.isDailyChallengeMode && 
            !uiState.hasSeenDailyUnlockCelebration

        VictoryScreen(
            levelNumber = uiState.currentLevelNumber,
            coinsEarned = baseCoins,
            streakDays = uiState.streakDays,
            totalCoins = uiState.coins,
            theme = theme,
            heartsRemaining = uiState.heartsRemaining,
            maxHearts = uiState.maxHearts,
            shapeName = level?.shapeName,
            isDailyChallenge = uiState.isDailyChallengeMode,
            completionTimeSeconds = uiState.levelCompletionTimeSeconds,
            movesCount = uiState.levelMovesCount,
            score = uiState.levelFinalScore,
            accuracyPercent = uiState.levelAccuracyPercent,
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
        val isInitialLevel1 = uiState.currentLevelNumber == 1 && !uiState.isDailyChallengeMode

        Surface(
            color = theme.background,
            modifier = modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                                shapeName = level?.shapeName,
                                onPauseClick = {
                                    onPlayClickSound()
                                    showPauseDialog = true
                                },
                                onSoundToggle = {
                                    val nextSound = !uiState.isSoundEnabled
                                    onToggleSound()
                                    hudToastMessage = if (nextSound) "Sound On 🔊" else "Sound Muted 🔇"
                                    hudToastTimer = System.currentTimeMillis()
                                },
                                onSoundLongPress = {
                                    onPlayClickSound()
                                    showSoundDialog = true
                                },
                                isSoundOn = uiState.isSoundEnabled,
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
                                onDismissPreviewRays = onDismissPreviewRays
                            )
                        }

                        // Running-style combo praise notification banner positioned below header
                        CheerOverlay(
                            message = uiState.cheerMessage,
                            fromLeft = uiState.cheerFromLeft,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                        )
                    }

                    // 3. BOTTOM BOOSTER CONTROLS (Hidden on initial Level 1 to keep gameplay focused & clean)
                    if (!isInitialLevel1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val effectiveHints = if (uiState.isDailyChallengeMode) {
                                uiState.dailyComplimentaryHintsRemaining
                            } else {
                                uiState.hintsAvailable
                            }
                            BoosterBar(
                                hintsCount = effectiveHints,
                                isGridLinesActive = uiState.isGridLinesActive,
                                isFreeHintLevel = (uiState.currentLevelNumber <= 10 && !uiState.isDailyChallengeMode),
                                isIdlePromptActive = isIdlePromptActive,
                                theme = theme,
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
                                modifier = Modifier.widthIn(max = 540.dp)
                            )
                        }
                    }
                }

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

                // 6. DEDICATED SOUND & VIBRATION MODAL DIALOG
                if (showSoundDialog) {
                    SoundSettingsDialog(
                        isSoundEnabled = uiState.isSoundEnabled,
                        isVibrationEnabled = uiState.isVibrationEnabled,
                        activeTheme = theme,
                        onToggleSound = onToggleSound,
                        onToggleVibration = onToggleVibration,
                        onDismiss = {
                            showSoundDialog = false
                        }
                    )
                }

                // 6. GAME OVER DIALOG
                if (uiState.isGameOver) {
                    GameOverDialog(
                        theme = theme,
                        hasGlobalLives = (uiState.globalLivesAvailable >= 3),
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
