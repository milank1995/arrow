package com.arrowescape.puzzle

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arrowescape.puzzle.game.GameViewModel
import com.arrowescape.puzzle.ui.components.DailyCalendarDialog
import com.arrowescape.puzzle.ui.components.SoundSettingsDialog
import com.arrowescape.puzzle.ui.screens.DevTestScreen
import com.arrowescape.puzzle.ui.screens.GamePlayScreen
import com.arrowescape.puzzle.ui.screens.HomeScreen
import com.arrowescape.puzzle.ui.screens.SettingsScreen

enum class AppScreen {
    HOME,
    GAMEPLAY,
    DEV_TEST,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by gameViewModel.uiState.collectAsState()
            val initialScreen = remember {
                if (gameViewModel.hasPlayedBeforeSynchronous()) AppScreen.HOME else AppScreen.GAMEPLAY
            }
            var currentScreen by remember { mutableStateOf(initialScreen) }
            var showDailyCalendarDialog by remember { mutableStateOf(false) }
            var showSoundSettingsDialog by remember { mutableStateOf(false) }

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
                                    isSoundOn = uiState.isSoundEnabled,
                                    onPlayClick = {
                                        gameViewModel.playClick()
                                        currentScreen = AppScreen.GAMEPLAY
                                    },
                                    onDailyChallengeClick = {
                                        gameViewModel.playClick()
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
                                        gameViewModel.playClick()
                                        gameViewModel.cycleTheme()
                                    },
                                    onSoundToggle = {
                                        gameViewModel.toggleSound()
                                    },
                                    onSettingsClick = {
                                        gameViewModel.playClick()
                                        currentScreen = AppScreen.SETTINGS
                                    },
                                    onDevTestClick = {
                                        gameViewModel.playClick()
                                        currentScreen = AppScreen.DEV_TEST
                                    },
                                    onSelectLevel = { level ->
                                        gameViewModel.playClick()
                                        gameViewModel.selectLevel(level)
                                        currentScreen = AppScreen.GAMEPLAY
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
                                    onDismissPreviewRays = {
                                        gameViewModel.dismissAllPreviewRays()
                                    },
                                    onHintClick = {
                                        gameViewModel.playClick()
                                        gameViewModel.useHint()
                                    },
                                    onHammerClick = {
                                        gameViewModel.playClick()
                                        gameViewModel.toggleHammerMode()
                                    },
                                    onWandClick = {
                                        gameViewModel.playClick()
                                        gameViewModel.useMagicWand()
                                    },
                                    onGridClick = {
                                        gameViewModel.playClick()
                                        gameViewModel.toggleGridLines()
                                    },
                                    onThemeSelect = { selectedTheme ->
                                        gameViewModel.playClick()
                                        gameViewModel.setTheme(selectedTheme)
                                    },
                                    onNextLevelClick = {
                                        gameViewModel.playClick()
                                        gameViewModel.markHasPlayedBefore()
                                        val wasDaily = uiState.isDailyChallengeMode
                                        gameViewModel.nextLevel()
                                        if (wasDaily) {
                                            currentScreen = AppScreen.HOME
                                        }
                                    },
                                    onClaimDoubleCoins = { amount ->
                                        gameViewModel.claimDoubleCoins(amount)
                                    },
                                    onRestartClick = {
                                        gameViewModel.playClick()
                                        gameViewModel.restartLevel()
                                    },
                                    onBackToHomeClick = {
                                        gameViewModel.playClick()
                                        gameViewModel.markHasPlayedBefore()
                                        if (uiState.isLevelComplete) {
                                            gameViewModel.onVictoryHomeClick()
                                        } else if (uiState.isDailyChallengeMode) {
                                            gameViewModel.loadLevel(uiState.currentLevelNumber)
                                        } else if (uiState.isGameOver) {
                                            gameViewModel.loadLevel(uiState.currentLevelNumber)
                                        }
                                        currentScreen = AppScreen.HOME
                                    },
                                    onClaimFreeLives = {
                                        gameViewModel.claimFreeLives(3)
                                    },
                                    onClaimAdHints = {
                                        gameViewModel.claimAdHints(3)
                                    },
                                    onDismissHintAdPrompt = {
                                        gameViewModel.dismissHintAdPrompt()
                                    },
                                    onToggleSound = {
                                        gameViewModel.toggleSound()
                                    },
                                    onToggleVibration = {
                                        gameViewModel.toggleVibration()
                                    },
                                    onSettingsClick = {
                                        gameViewModel.playClick()
                                        currentScreen = AppScreen.SETTINGS
                                    },
                                    onIdlePromptSound = {
                                        gameViewModel.playHintNudge()
                                    },
                                    onPlayStarSound = { starIdx ->
                                        gameViewModel.playStarDrop(starIdx)
                                    },
                                    onPlayCoinSound = {
                                        gameViewModel.playCoinCollect()
                                    },
                                    onPlayClickSound = {
                                        gameViewModel.playClick()
                                    }
                                )
                            }
                            AppScreen.DEV_TEST -> {
                                DevTestScreen(
                                    theme = uiState.activeTheme,
                                    audioHapticManager = gameViewModel.audioHapticManager,
                                    onBackToHomeClick = {
                                        currentScreen = AppScreen.HOME
                                    },
                                    onThemeSelect = { selectedTheme ->
                                        gameViewModel.setTheme(selectedTheme)
                                    }
                                )
                            }
                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    isSoundEnabled = uiState.isSoundEnabled,
                                    isVibrationEnabled = uiState.isVibrationEnabled,
                                    activeTheme = uiState.activeTheme,
                                    onToggleSound = {
                                        gameViewModel.toggleSound()
                                    },
                                    onToggleVibration = {
                                        gameViewModel.toggleVibration()
                                    },
                                    onThemeSelect = { selectedTheme ->
                                        gameViewModel.playClick()
                                        gameViewModel.setTheme(selectedTheme)
                                    },
                                    onBackClick = {
                                        gameViewModel.playClick()
                                        currentScreen = AppScreen.HOME
                                    }
                                )
                            }
                        }
                    }

                    // Daily Calendar Modal Dialog
                    if (showDailyCalendarDialog) {
                        BackHandler { showDailyCalendarDialog = false }
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

                    // Audio & Sound Settings Menu Modal Dialog
                    if (showSoundSettingsDialog) {
                        SoundSettingsDialog(
                            isSoundEnabled = uiState.isSoundEnabled,
                            isVibrationEnabled = uiState.isVibrationEnabled,
                            activeTheme = uiState.activeTheme,
                            onToggleSound = {
                                gameViewModel.toggleSound()
                            },
                            onToggleVibration = {
                                gameViewModel.toggleVibration()
                            },
                            onThemeSelect = { selectedTheme ->
                                gameViewModel.playClick()
                                gameViewModel.setTheme(selectedTheme)
                            },
                            onThemeClick = {
                                gameViewModel.playClick()
                                gameViewModel.cycleTheme()
                            },
                            onTestSound = {
                                gameViewModel.playTap()
                            },
                            onDismiss = {
                                showSoundSettingsDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}
