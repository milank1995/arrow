package com.arrowaway.puzzle

import com.arrowaway.puzzle.ads.AdManager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import androidx.core.content.ContextCompat
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.arrowaway.puzzle.game.GameViewModel
import com.arrowaway.puzzle.notification.NotificationHelper
import com.arrowaway.puzzle.ui.components.DailyStartPromptDialog
import com.arrowaway.puzzle.ui.components.SoundSettingsDialog
import com.arrowaway.puzzle.ui.screens.DailyChallengeScreen
import com.arrowaway.puzzle.ui.screens.DevTestScreen
import com.arrowaway.puzzle.ui.screens.GamePlayScreen
import com.arrowaway.puzzle.ui.screens.HomeScreen
import com.arrowaway.puzzle.ui.screens.NewOwnShapeScreen
import com.arrowaway.puzzle.ui.screens.SettingsScreen
import java.time.LocalDate

enum class AppScreen {
    HOME,
    GAMEPLAY,
    DAILY_CHALLENGE,
    DEV_TEST,
    SETTINGS,
    NEW_OWN_SHAPE
}

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun hideSystemBars() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        hideSystemBars()

        setContent {
            val uiState by gameViewModel.uiState.collectAsState()
            val initialScreen = remember {
                if (intent.getStringExtra("EXTRA_NAVIGATE_TO") == "daily_challenge") {
                    intent.removeExtra("EXTRA_NAVIGATE_TO")
                    gameViewModel.startDailyChallenge()
                    AppScreen.GAMEPLAY
                } else if (gameViewModel.hasPlayedBeforeSynchronous()) {
                    AppScreen.HOME
                } else {
                    AppScreen.GAMEPLAY
                }
            }
            var currentScreen by remember { mutableStateOf(initialScreen) }
            var showSoundSettingsDialog by remember { mutableStateOf(false) }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                            gameViewModel.onAppBackgrounded()
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            gameViewModel.onAppForegrounded(isGameplayScreen = currentScreen == AppScreen.GAMEPLAY)
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val inAppReviewManager = remember { com.arrowaway.puzzle.review.InAppReviewManager(this@MainActivity) }

            LaunchedEffect(uiState.reviewPromptTrigger) {
                if (uiState.reviewPromptTrigger > 0L) {
                    inAppReviewManager.launchReviewFlow {
                        gameViewModel.onReviewPromptHandled()
                    }
                }
            }

            LaunchedEffect(currentScreen) {
                gameViewModel.analytics.logScreenView(currentScreen.name)
                if (currentScreen == AppScreen.GAMEPLAY) {
                    gameViewModel.onNavigateToGameplay()
                } else {
                    gameViewModel.onNavigateAwayFromGameplay()
                }
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                gameViewModel.onNotificationPermissionResult(isGranted)
            }

            LaunchedEffect(intent) {
                if (intent.getStringExtra("EXTRA_NAVIGATE_TO") == "daily_challenge") {
                    intent.removeExtra("EXTRA_NAVIGATE_TO")
                    gameViewModel.startDailyChallenge()
                    currentScreen = AppScreen.GAMEPLAY
                }
            }

            // Automatically request Android notification permission on app launch if daily challenge unlock level has already been reached
            LaunchedEffect(uiState.currentLevelNumber, uiState.lastCompletedLevelNumber, uiState.hasRequestedNotificationPermission) {
                val unlockLevel = com.arrowaway.puzzle.config.RemoteConfigManager.dailyChallengeUnlockLevel
                val isPastLevel10 = uiState.currentLevelNumber > unlockLevel || uiState.lastCompletedLevelNumber >= unlockLevel
                if (isPastLevel10 && !uiState.hasRequestedNotificationPermission) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasPermission) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    gameViewModel.markNotificationPermissionRequested()
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
                                    isDevMode = uiState.isDevMode,
                                    onPlayClick = {
                                        gameViewModel.playClick()
                                        currentScreen = AppScreen.GAMEPLAY
                                    },
                                    onDailyChallengeClick = {
                                        gameViewModel.playClick()
                                        if (!com.arrowaway.puzzle.config.RemoteConfigManager.isDailyChallengeEnabled) {
                                            Toast.makeText(this@MainActivity, "Daily Challenges are temporarily offline for maintenance.", Toast.LENGTH_SHORT).show()
                                            return@HomeScreen
                                        }
                                        val unlockLevel = com.arrowaway.puzzle.config.RemoteConfigManager.dailyChallengeUnlockLevel
                                        val isUnlocked = uiState.currentLevelNumber > unlockLevel || 
                                            uiState.lastCompletedLevelNumber >= unlockLevel || 
                                            uiState.hasSeenDailyUnlockCelebration

                                        if (isUnlocked) {
                                            currentScreen = AppScreen.DAILY_CHALLENGE
                                        } else {
                                            val completed = uiState.lastCompletedLevelNumber.coerceAtLeast(uiState.currentLevelNumber - 1)
                                            val needed = (unlockLevel - completed).coerceAtLeast(1)
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Complete Level $unlockLevel to unlock Daily Challenges! ($needed level${if (needed > 1) "s" else ""} left)",
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
                                    onNewOwnShapeClick = {
                                        gameViewModel.playClick()
                                        currentScreen = AppScreen.NEW_OWN_SHAPE
                                    },
                                    onLockPuzzleClick = {
                                        gameViewModel.playClick()
                                        gameViewModel.startLockPuzzle()
                                        currentScreen = AppScreen.GAMEPLAY
                                    },
                                    onDevTestClick = {
                                        gameViewModel.playClick()
                                        currentScreen = AppScreen.DEV_TEST
                                    },
                                    onLevelChange = { newLevel ->
                                        gameViewModel.playClick()
                                        gameViewModel.loadLevel(newLevel)
                                    },
                                    onTestNotificationClick = {
                                        gameViewModel.playClick()
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !NotificationHelper.hasNotificationPermission(this@MainActivity)) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            NotificationHelper.postDailyChallengeNotification(this@MainActivity, streakDays = uiState.streakDays)
                                            Toast.makeText(this@MainActivity, "Daily notification sent! Check notification tray.", Toast.LENGTH_SHORT).show()
                                        }
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
                                        gameViewModel.resetGridLines()
                                        if (uiState.currentLevelNumber == 10 && !uiState.hasSeenDailyUnlockCelebration) {
                                            gameViewModel.markDailyUnlockCelebrationSeen()
                                        }
                                        val wasDaily = uiState.isDailyChallengeMode
                                        val wasLock = uiState.isLockPuzzleMode
                                        val wasCustom = uiState.isCustomShapeMode || uiState.isGlassesPuzzleMode
                                        val curLevel = uiState.currentLevelNumber

                                        val advance = {
                                            gameViewModel.nextLevel()
                                            currentScreen = when {
                                                wasDaily -> AppScreen.DAILY_CHALLENGE
                                                wasCustom -> AppScreen.NEW_OWN_SHAPE
                                                else -> AppScreen.HOME
                                            }
                                        }

                                        if (!wasDaily && !wasLock && !wasCustom) {
                                            AdManager.showInterstitialAd(
                                                activity = this@MainActivity,
                                                currentLevel = curLevel,
                                                onDismissed = { advance() }
                                            )
                                        } else {
                                            advance()
                                        }
                                    },
                                    onStartDailyChallengeClick = {
                                        gameViewModel.playClick()
                                        gameViewModel.markDailyUnlockCelebrationSeen()
                                        val today = LocalDate.now().toString()
                                        gameViewModel.startDailyChallenge(today)
                                        currentScreen = AppScreen.GAMEPLAY
                                    },
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                this@MainActivity,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (!hasPermission) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        }
                                        gameViewModel.markNotificationPermissionRequested()
                                    },
                                    onClaimDoubleCoins = { amount ->
                                        gameViewModel.claimDoubleCoins(amount)
                                    },
                                    onRestartClick = {
                                        gameViewModel.playClick()
                                        gameViewModel.restartLevel()
                                    },
                                    onBackToHomeClick = {
                                        gameViewModel.onAbandonGameplay("back_to_home")
                                        gameViewModel.playClick()
                                        gameViewModel.markHasPlayedBefore()
                                        gameViewModel.resetGridLines()
                                        val wasDaily = uiState.isDailyChallengeMode
                                        val wasCustom = uiState.isCustomShapeMode || uiState.isGlassesPuzzleMode
                                        if (uiState.isLevelComplete) {
                                            gameViewModel.onVictoryHomeClick()
                                        } else if (uiState.isDailyChallengeMode || uiState.isLockPuzzleMode || wasCustom) {
                                            gameViewModel.loadLevel(uiState.currentLevelNumber)
                                        } else if (uiState.isGameOver) {
                                            gameViewModel.loadLevel(uiState.currentLevelNumber)
                                        }
                                        currentScreen = when {
                                            wasDaily -> AppScreen.DAILY_CHALLENGE
                                            wasCustom -> AppScreen.NEW_OWN_SHAPE
                                            else -> AppScreen.HOME
                                        }
                                    },
                                    onClaimFreeLives = {
                                        if (uiState.currentLevelNumber <= 10 && !uiState.isDailyChallengeMode && !uiState.isLockPuzzleMode && !uiState.isCustomShapeMode && !uiState.isGlassesPuzzleMode) {
                                            gameViewModel.claimFreeLives(3)
                                        } else {
                                            AdManager.showRewardedAd(
                                                activity = this@MainActivity,
                                                placement = "lives_expiry",
                                                onRewardEarned = {
                                                    gameViewModel.claimFreeLives(3)
                                                },
                                                onAdClosed = { _ -> }
                                            )
                                        }
                                    },
                                    onClaimAdHints = {
                                        AdManager.showRewardedAd(
                                            activity = this@MainActivity,
                                            placement = "hint_refill",
                                            onRewardEarned = {
                                                gameViewModel.claimAdHints(1)
                                            },
                                            onAdClosed = { _ -> }
                                        )
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
                                    onAutoSolveClick = {
                                        gameViewModel.toggleAutoSolve()
                                    },
                                    onGameplayStarted = {
                                        gameViewModel.onGameplayStarted()
                                    },
                                    onPauseStateChanged = { isPaused ->
                                        gameViewModel.setGamePaused(isPaused)
                                    }
                                )
                            }
                            AppScreen.DEV_TEST -> {
                                if (uiState.isDevMode) {
                                    DevTestScreen(
                                        theme = uiState.activeTheme,
                                        audioHapticManager = gameViewModel.audioHapticManager,
                                        onBackToHomeClick = {
                                            currentScreen = AppScreen.HOME
                                        },
                                        onPlayInRegularGame = { selectedLevel ->
                                            gameViewModel.playClick()
                                            gameViewModel.loadLevel(selectedLevel)
                                            currentScreen = AppScreen.GAMEPLAY
                                        },
                                        onThemeSelect = { selectedTheme ->
                                            gameViewModel.setTheme(selectedTheme)
                                        }
                                    )
                                } else {
                                    currentScreen = AppScreen.HOME
                                }
                            }
                            AppScreen.NEW_OWN_SHAPE -> {
                                NewOwnShapeScreen(
                                    theme = uiState.activeTheme,
                                    onBackClick = { currentScreen = AppScreen.HOME },
                                    onLevelSelected = { fileName ->
                                        gameViewModel.playClick()
                                        gameViewModel.startCustomShapeLevel(fileName)
                                        currentScreen = AppScreen.GAMEPLAY
                                    }
                                )
                            }
                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    isSoundEnabled = uiState.isSoundEnabled,
                                    isVibrationEnabled = uiState.isVibrationEnabled,
                                    dailyRemindersEnabled = uiState.dailyRemindersEnabled,
                                    activeTheme = uiState.activeTheme,
                                    isDevMode = uiState.isDevMode,
                                    isHintButtonVisible = uiState.isHintButtonVisible,
                                    isGridButtonVisible = uiState.isGridButtonVisible,
                                    hintTimerSeconds = uiState.hintTimerSeconds,
                                    onToggleHintButton = {
                                        gameViewModel.playClick()
                                        val newVal = !uiState.isHintButtonVisible
                                        gameViewModel.analytics.logSettingPreferenceChanged("hint_button_visibility", uiState.isHintButtonVisible, newVal)
                                        gameViewModel.setHintButtonVisible(newVal)
                                    },
                                    onToggleGridButton = {
                                        gameViewModel.playClick()
                                        val newVal = !uiState.isGridButtonVisible
                                        gameViewModel.analytics.logSettingPreferenceChanged("grid_button_visibility", uiState.isGridButtonVisible, newVal)
                                        gameViewModel.setGridButtonVisible(newVal)
                                    },
                                    onHintTimerChange = { newSec ->
                                        gameViewModel.analytics.logSettingPreferenceChanged("hint_timer_duration", uiState.hintTimerSeconds, newSec)
                                        gameViewModel.setHintTimerSeconds(newSec)
                                    },
                                    onToggleDevMode = {
                                        val isNowDev = gameViewModel.toggleDevMode()
                                        gameViewModel.analytics.logDevModeToggled(isNowDev)
                                        val msg = if (isNowDev) {
                                            "🛠️ Development Mode Enabled"
                                        } else {
                                            "🌟 Production Mode Enabled"
                                        }
                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    onToggleSound = {
                                        gameViewModel.analytics.logSettingPreferenceChanged("sound", uiState.isSoundEnabled, !uiState.isSoundEnabled)
                                        gameViewModel.toggleSound()
                                    },
                                    onToggleVibration = {
                                        gameViewModel.analytics.logSettingPreferenceChanged("vibration", uiState.isVibrationEnabled, !uiState.isVibrationEnabled)
                                        gameViewModel.toggleVibration()
                                    },
                                    onToggleDailyReminders = {
                                        gameViewModel.playClick()
                                        val isCurrentlyEnabled = uiState.dailyRemindersEnabled
                                        gameViewModel.analytics.logSettingPreferenceChanged("daily_reminders", isCurrentlyEnabled, !isCurrentlyEnabled)
                                        if (!isCurrentlyEnabled) {
                                            // User wants to turn ON reminders: check OS permission
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                val hasPermission = ContextCompat.checkSelfPermission(
                                                    this@MainActivity,
                                                    Manifest.permission.POST_NOTIFICATIONS
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (!hasPermission) {
                                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                } else {
                                                    gameViewModel.toggleDailyReminders()
                                                }
                                            } else {
                                                gameViewModel.toggleDailyReminders()
                                            }
                                        } else {
                                            gameViewModel.toggleDailyReminders()
                                        }
                                    },
                                    onThemeSelect = { selectedTheme ->
                                        gameViewModel.playClick()
                                        gameViewModel.analytics.logSettingPreferenceChanged("theme", uiState.activeTheme.name, selectedTheme.name)
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

                    // Game Start Daily Challenge Prompt Modal on Home screen
                    if (uiState.showDailyLaunchPrompt && currentScreen == AppScreen.HOME) {
                        DailyStartPromptDialog(
                            streakDays = uiState.streakDays,
                            theme = uiState.activeTheme,
                            onPlayTodayClick = {
                                gameViewModel.playClick()
                                gameViewModel.dismissDailyLaunchPrompt()
                                val today = LocalDate.now().toString()
                                gameViewModel.startDailyChallenge(today)
                                currentScreen = AppScreen.GAMEPLAY
                            },
                            onDismiss = {
                                gameViewModel.playClick()
                                gameViewModel.dismissDailyLaunchPrompt()
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
