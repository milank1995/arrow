package com.arrowescape.puzzle.ui.screens

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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.ArrowState
import com.arrowescape.puzzle.game.GameUiState
import com.arrowescape.puzzle.ui.components.BoosterBar
import com.arrowescape.puzzle.ui.components.CheerOverlay
import com.arrowescape.puzzle.ui.components.GameHeader
import com.arrowescape.puzzle.ui.components.GameOverDialog
import com.arrowescape.puzzle.ui.components.LevelVictoryDialog
import com.arrowescape.puzzle.ui.components.PuzzleBoardCanvas
import com.arrowescape.puzzle.ui.components.ThemeCustomizationDialog
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
    onRestartClick: () -> Unit,
    onBackToHomeClick: () -> Unit,
    onIdlePromptSound: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    val level = uiState.levelData
    val theme = uiState.activeTheme
    val remainingArrowsCount = uiState.arrows.count { 
        it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT 
    }

    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isIdlePromptActive by remember { mutableStateOf(false) }
    var hasPlayedIdleSoundForPeriod by remember { mutableStateOf(false) }

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
            // 1. TOP GAMEPLAY HEADER (Matches Screenshot 2 & 3)
            if (level != null) {
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
                if (level != null) {
                    PuzzleBoardCanvas(
                        xSize = level.xSize,
                        ySize = level.ySize,
                        arrows = uiState.arrows,
                        theme = theme,
                        isGridLinesActive = uiState.isGridLinesActive,
                        isCelebrationBurstActive = uiState.isCelebrationBurstActive,
                        previewRayArrowId = uiState.previewRayArrowId,
                        onArrowClick = { arrow ->
                            lastInteractionTime = System.currentTimeMillis()
                            onArrowClick(arrow)
                        },
                        onArrowLongPress = { arrow, isPressed ->
                            lastInteractionTime = System.currentTimeMillis()
                            onArrowLongPress(arrow, isPressed)
                        }
                    )
                }

                // Running-style combo praise notification banner positioned below header and above gameplay
                CheerOverlay(
                    message = uiState.cheerMessage,
                    fromLeft = uiState.cheerFromLeft,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }

            // 3. BOTTOM BOOSTER CONTROLS (Matches Screenshot 2 & 3: Hint & Grid pods)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                BoosterBar(
                    hintsCount = uiState.hintsAvailable,
                    isGridLinesActive = uiState.isGridLinesActive,
                    isIdlePromptActive = isIdlePromptActive,
                    theme = theme,
                    onHintClick = {
                        lastInteractionTime = System.currentTimeMillis()
                        onHintClick()
                    },
                    onGridClick = {
                        lastInteractionTime = System.currentTimeMillis()
                        onGridClick()
                    },
                    modifier = Modifier.widthIn(max = 540.dp)
                )
            }
        }

        // VICTORY DIALOG
        if (uiState.isLevelComplete) {
            LevelVictoryDialog(
                levelNumber = uiState.currentLevelNumber,
                coinsEarned = 50 + (uiState.comboCount * 5),
                streakDays = uiState.streakDays,
                theme = theme,
                onNextLevelClick = onNextLevelClick
            )
        }

        // GAME OVER DIALOG
        if (uiState.isGameOver) {
            GameOverDialog(
                theme = theme,
                onRestartClick = onRestartClick,
                onHomeClick = onBackToHomeClick
            )
        }

        // THEME CUSTOMIZATION DIALOG
        if (showThemeDialog) {
            ThemeCustomizationDialog(
                activeTheme = theme,
                onThemeSelect = { selectedTheme ->
                    onThemeSelect(selectedTheme)
                },
                onDismiss = {
                    showThemeDialog = false
                }
            )
        }
    }
}
