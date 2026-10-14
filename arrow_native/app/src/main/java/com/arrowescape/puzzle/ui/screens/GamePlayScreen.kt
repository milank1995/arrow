package com.arrowescape.puzzle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arrowescape.puzzle.game.GameUiState
import com.arrowescape.puzzle.ui.components.BoosterBar
import com.arrowescape.puzzle.ui.components.CheerOverlay
import com.arrowescape.puzzle.ui.components.GameHeader
import com.arrowescape.puzzle.ui.components.GameOverDialog
import com.arrowescape.puzzle.ui.components.LevelVictoryDialog
import com.arrowescape.puzzle.ui.components.PuzzleBoardCanvas
import com.arrowescape.puzzle.core.model.Arrow

@Composable
fun GamePlayScreen(
    uiState: GameUiState,
    onArrowClick: (Arrow) -> Unit,
    onArrowLongPress: (Arrow, isPressed: Boolean) -> Unit,
    onHintClick: () -> Unit,
    onHammerClick: () -> Unit,
    onWandClick: () -> Unit,
    onGridClick: () -> Unit,
    onThemeClick: () -> Unit,
    onNextLevelClick: () -> Unit,
    onRestartClick: () -> Unit,
    onBackToHomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val level = uiState.levelData
    val theme = uiState.activeTheme

    Surface(
        color = theme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. TOP GAMEPLAY HEADER
            if (level != null) {
                GameHeader(
                    levelNumber = uiState.currentLevelNumber,
                    difficulty = level.difficulty,
                    heartsRemaining = uiState.heartsRemaining,
                    maxHearts = uiState.maxHearts,
                    coins = uiState.coins,
                    theme = theme,
                    onBackClick = onBackToHomeClick,
                    onThemeClick = onThemeClick
                )
            }

            // 2. MAIN PUZZLE BOARD CANVAS & CHEER OVERLAY
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                if (level != null) {
                    PuzzleBoardCanvas(
                        xSize = level.xSize,
                        ySize = level.ySize,
                        arrows = uiState.arrows,
                        theme = theme,
                        isGridLinesActive = uiState.isGridLinesActive,
                        previewRayArrowId = uiState.previewRayArrowId,
                        onArrowClick = onArrowClick,
                        onArrowLongPress = onArrowLongPress
                    )
                }

                // Combo praise floating overlay
                CheerOverlay(
                    message = uiState.cheerMessage,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 3. BOTTOM BOOSTER CONTROLS
            BoosterBar(
                hintsCount = uiState.hintsAvailable,
                hammersCount = uiState.hammersAvailable,
                wandsCount = uiState.wandsAvailable,
                isHammerActive = uiState.isHammerModeActive,
                isGridLinesActive = uiState.isGridLinesActive,
                theme = theme,
                onHintClick = onHintClick,
                onHammerClick = onHammerClick,
                onWandClick = onWandClick,
                onGridClick = onGridClick
            )
        }

        // VICTORY MODAL
        if (uiState.isLevelComplete) {
            LevelVictoryDialog(
                levelNumber = uiState.currentLevelNumber,
                coinsEarned = 25,
                streakDays = uiState.streakDays,
                theme = theme,
                onNextLevelClick = onNextLevelClick
            )
        }

        // GAME OVER MODAL
        if (uiState.isGameOver) {
            GameOverDialog(
                theme = theme,
                onRestartClick = onRestartClick,
                onHomeClick = onBackToHomeClick
            )
        }
    }
}
