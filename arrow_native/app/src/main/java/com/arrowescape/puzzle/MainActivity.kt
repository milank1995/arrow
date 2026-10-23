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
import com.arrowescape.puzzle.ui.components.DailyUnlockDialog
import com.arrowescape.puzzle.ui.components.SoundSettingsDialog
import com.arrowescape.puzzle.ui.screens.DailyChallengeScreen
import com.arrowescape.puzzle.ui.screens.DevTestScreen
import com.arrowescape.puzzle.ui.screens.GamePlayScreen
import com.arrowescape.puzzle.ui.screens.HomeScreen
import com.arrowescape.puzzle.ui.screens.SettingsScreen

enum class AppScreen {
    HOME,
    GAMEPLAY,
    DAILY_CHALLENGE,
    DEV_TEST,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by gameViewModel.uiState.collectAsState()
            val initialScreen = remember {
                if (intent.getStringExtra("EXTRA_NAVIGATE_TO") == "daily_challenge") {
                    AppScreen.DAILY_CHALLENGE
                } else if (gameViewModel.hasPlayedBeforeSynchronous()) {
                    AppScreen.HOME
                } else {
                    AppScreen.GAMEPLAY
                }
            }
            var currentScreen by remember { mutableStateOf(initialScreen) }
            var showSoundSettingsDialog by remember { mutableStateOf(false) }

            LaunchedEffect(intent) {
                if (intent.getStringExtra("EXTRA_NAVIGATE_TO") == "daily_challenge") {
                    currentScreen = AppScreen.DAILY_CHALLENGE
                }
            }

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
                                            currentScreen = AppScreen.DAILY_CHALLENGE
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
                                            currentScreen = AppScreen.DAILY_CHALLENGE
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
                                        val wasDaily = uiState.isDailyChallengeMode
                                        if (uiState.isLevelComplete) {
                                            gameViewModel.onVictoryHomeClick()
                                        } else if (uiState.isDailyChallengeMode) {
                                            gameViewModel.loadLevel(uiState.currentLevelNumber)
                                        } else if (uiState.isGameOver) {
                                            gameViewModel.loadLevel(uiState.currentLevelNumber)
                                        }
                                        currentScreen = if (wasDaily) AppScreen.DAILY_CHALLENGE else AppScreen.HOME
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
                                    },
                                    onGameplayStarted = {
                                        gameViewModel.onGameplayStarted()
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
                                    dailyRemindersEnabled = uiState.dailyRemindersEnabled,
                                    activeTheme = uiState.activeTheme,
                                    onToggleSound = {
                                        gameViewModel.toggleSound()
                                    },
                                    onToggleVibration = {
                                        gameViewModel.toggleVibration()
                                    },
                                    onToggleDailyReminders = {
                                        gameViewModel.toggleDailyReminders()
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
                            AppScreen.DAILY_CHALLENGE -> {
                                DailyChallengeScreen(
                                    completedDates = uiState.completedDailyDates,
                                    streakDays = uiState.streakDays,
                                    theme = uiState.activeTheme,
                                    onPlayDate = { dateString ->
                                        gameViewModel.playClick()
                                        gameViewModel.startDailyChallenge(dateString)
                                        currentScreen = AppScreen.GAMEPLAY
                                    },
                                    onBackClick = {
                                        gameViewModel.playClick()
                                        currentScreen = AppScreen.HOME
                                    }
                                )
                            }
                        }
                    }

                    // Celebratory Milestone Modal Dialog when Daily Challenge unlocks at Level 5
                    if (uiState.showDailyUnlockDialog && currentScreen == AppScreen.HOME) {
                        DailyUnlockDialog(
                            theme = uiState.activeTheme,
                            onPlayTodayClick = { optInReminders ->
                                gameViewModel.playClick()
                                gameViewModel.dismissDailyUnlockDialog(optInReminders)
                                currentScreen = AppScreen.DAILY_CHALLENGE
                            },
                            onDismiss = { optInReminders ->
                                gameViewModel.playClick()
                                gameViewModel.dismissDailyUnlockDialog(optInReminders)
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
