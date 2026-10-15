package com.arrowescape.puzzle

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arrowescape.puzzle.game.GameViewModel
import com.arrowescape.puzzle.ui.components.DailyCalendarDialog
import com.arrowescape.puzzle.ui.screens.GamePlayScreen
import com.arrowescape.puzzle.ui.screens.HomeScreen

enum class AppScreen {
    HOME,
    GAMEPLAY
}

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by gameViewModel.uiState.collectAsState()
            var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
            var showDailyCalendarDialog by remember { mutableStateOf(false) }

            MaterialTheme {
                Surface(
                    color = uiState.activeTheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Crossfade(targetState = currentScreen, label = "screenTransition") { screen ->
                        when (screen) {
                            AppScreen.HOME -> {
                                HomeScreen(
                                    uiState = uiState,
                                    theme = uiState.activeTheme,
                                    isSoundOn = gameViewModel.audioHapticManager.isSoundEnabled,
                                    onPlayClick = {
                                        currentScreen = AppScreen.GAMEPLAY
                                    },
                                    onDailyChallengeClick = {
                                        if (uiState.currentLevelNumber >= 5) {
                                            showDailyCalendarDialog = true
                                        } else {
                                            val needed = 5 - uiState.currentLevelNumber
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Complete $needed more level${if (needed > 1) "s" else ""} to unlock Daily Challenges!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onThemeClick = {
                                        gameViewModel.cycleTheme()
                                    },
                                    onSoundToggle = {
                                        gameViewModel.audioHapticManager.isSoundEnabled =
                                            !gameViewModel.audioHapticManager.isSoundEnabled
                                    }
                                )
                            }
                            AppScreen.GAMEPLAY -> {
                                GamePlayScreen(
                                    uiState = uiState,
                                    onArrowClick = { arrow ->
                                        gameViewModel.onArrowClicked(arrow)
                                    },
                                    onArrowLongPress = { arrow, isPressed ->
                                        gameViewModel.onArrowLongPress(arrow, isPressed)
                                    },
                                    onHintClick = {
                                        gameViewModel.useHint()
                                    },
                                    onHammerClick = {
                                        gameViewModel.toggleHammerMode()
                                    },
                                    onWandClick = {
                                        gameViewModel.useMagicWand()
                                    },
                                    onGridClick = {
                                        gameViewModel.toggleGridLines()
                                    },
                                    onThemeSelect = { selectedTheme ->
                                        gameViewModel.setTheme(selectedTheme)
                                    },
                                    onNextLevelClick = {
                                        val wasDaily = uiState.isDailyChallengeMode
                                        gameViewModel.nextLevel()
                                        if (wasDaily) {
                                            currentScreen = AppScreen.HOME
                                        }
                                    },
                                    onRestartClick = {
                                        gameViewModel.restartLevel()
                                    },
                                    onBackToHomeClick = {
                                        if (uiState.isDailyChallengeMode) {
                                            gameViewModel.loadLevel(uiState.currentLevelNumber)
                                        }
                                        currentScreen = AppScreen.HOME
                                    },
                                    onIdlePromptSound = {
                                        gameViewModel.playHintNudge()
                                    }
                                )
                            }
                        }
                    }

                    // Daily Calendar Modal Dialog
                    if (showDailyCalendarDialog) {
                        DailyCalendarDialog(
                            completedDates = uiState.completedDailyDates,
                            streakDays = uiState.streakDays,
                            theme = uiState.activeTheme,
                            onPlayDate = { dateString ->
                                gameViewModel.startDailyChallenge(dateString)
                                currentScreen = AppScreen.GAMEPLAY
                            },
                            onDismiss = {
                                showDailyCalendarDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}
