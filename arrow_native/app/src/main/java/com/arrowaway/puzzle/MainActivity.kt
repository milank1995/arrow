package com.arrowaway.puzzle

import com.arrowaway.puzzle.ads.AdManager
import com.arrowaway.puzzle.ads.ConsentManager
import com.arrowaway.puzzle.core.DailyChallengeRules

import android.Manifest
import android.content.Intent
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import com.arrowaway.puzzle.ui.components.effectiveRewardedStatus
import com.arrowaway.puzzle.ui.components.rewardedStatusMessage
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.arrowaway.puzzle.game.GameViewModel
import com.arrowaway.puzzle.notification.NotificationHelper
import com.arrowaway.puzzle.ui.components.NotificationPrePromptDialog
import com.arrowaway.puzzle.ui.components.NotificationSettingsDialog
import com.arrowaway.puzzle.ui.components.SoundSettingsDialog
import com.arrowaway.puzzle.ui.screens.DailyChallengeScreen
import com.arrowaway.puzzle.ui.screens.DevTestScreen
import com.arrowaway.puzzle.ui.screens.GamePlayScreen
import com.arrowaway.puzzle.ui.screens.HomeScreen
import com.arrowaway.puzzle.ui.screens.SettingsScreen
import java.time.LocalDate

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

    private fun launchGameplayWithPendingAdCheck(onProceed: () -> Unit) {
        if (AdManager.hasPendingInterstitial(this)) {
            AdManager.showPendingInterstitialAd(this) {
                onProceed()
            }
        } else {
            onProceed()
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = NotificationHelper.hasNotificationPermission(this)
            val state = gameViewModel.uiState.value
            if (!hasPermission && state.dailyRemindersEnabled) {
                gameViewModel.setDailyRemindersEnabled(false)
                NotificationHelper.cancelDailyReminderAlarm(this)
            } else if (hasPermission && !state.dailyRemindersEnabled && awaitingNotificationSettingsReturn) {
                // Only re-enable when the player just granted permission via the Android settings link.
                // Otherwise a player who turned reminders off in-app would have them switched back on.
                gameViewModel.setDailyRemindersEnabled(true)
                NotificationHelper.scheduleDailyReminderAlarm(this)
            }
        }
        awaitingNotificationSettingsReturn = false
    }

    /** Set when we send the player to Android notification settings; consumed on the next onResume. */
    private var awaitingNotificationSettingsReturn = false

    /** The pending "load an ad for a hint" wait, so a new tap or a screen change can't stack them. */
    private var hintAdJob: Job? = null

    override fun onSaveInstanceState(outState: android.os.Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_AWAITING_NOTIFICATION_SETTINGS, awaitingNotificationSettingsReturn)
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
        if (savedInstanceState != null) {
            awaitingNotificationSettingsReturn = savedInstanceState.getBoolean(KEY_AWAITING_NOTIFICATION_SETTINGS, false)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        hideSystemBars()

        // Show the GDPR / US-state consent form when required, then start ads if consent allows.
        ConsentManager.gatherConsent(this) {
            if (ConsentManager.canRequestAds(this)) {
                AdManager.initialize(applicationContext)
            }
        }

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val uiState by gameViewModel.uiState.collectAsState()
            val isRewardedAdReady by AdManager.rewardedReady.collectAsState()
            val rewardedStatus by AdManager.rewardedStatus.collectAsState()
            val rewardedUnavailableReason = when (rewardedStatus) {
                AdManager.RewardedStatus.LOADING -> "still_loading"
                else -> rewardedStatus.name.lowercase()
            }
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
            var devTestInitialDailyMode by remember { mutableStateOf(false) }

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
                    inAppReviewManager.launchReviewFlow { success ->
                        gameViewModel.analytics.logInAppReviewCompleted(success)
                        gameViewModel.onReviewPromptHandled()
                    }
                }
            }

            LaunchedEffect(currentScreen) {
                gameViewModel.analytics.logScreenView(currentScreen.name)
                if (currentScreen == AppScreen.GAMEPLAY) {
                    gameViewModel.onNavigateToGameplay()
                    AdManager.preloadRewardedAd(this@MainActivity)
                } else {
                    gameViewModel.onNavigateAwayFromGameplay()
                }
            }

            // Track ad reward prompt impressions
            LaunchedEffect(uiState.isGameOver) {
                if (uiState.isGameOver && (uiState.currentLevelNumber > 10 || uiState.isDailyChallengeMode)) {
                    gameViewModel.analytics.logAdRewardPromptShown("lives_expiry", isRewardedAdReady)
                }
            }

            // Hint button with 0 hints (level 11+ / Daily): play a rewarded ad for 1 hint, or say why not
            val requestHintAd: () -> Unit = {
                val tapState = gameViewModel.uiState.value
                if (!tapState.isHintAdLoading) {
                    gameViewModel.clearHintErrorMessage()
                    val ctx = this@MainActivity
                    val isReady = AdManager.rewardedReady.value && !AdManager.isRewardedAdExpired()
                    gameViewModel.analytics.logAdRewardPromptShown("hint_refill", isReady)

                    val playHintAd = {
                        AdManager.showRewardedAd(
                            activity = ctx,
                            placement = "hint_refill",
                            onRewardEarned = { gameViewModel.claimAdHints(1) },
                            onAdClosed = { _ -> gameViewModel.setHintAdLoading(false) }
                        )
                    }
                    // Same player-facing messages as the reward dialogs (offline, ads blocked, no fill)
                    val showUnavailable = { status: AdManager.RewardedStatus ->
                        gameViewModel.analytics.logAdRewardUnavailable(
                            "hint_refill",
                            if (status == AdManager.RewardedStatus.LOADING) "still_loading" else status.name.lowercase()
                        )
                        gameViewModel.showHintError(
                            rewardedStatusMessage(effectiveRewardedStatus(status, timedOut = true))
                                ?: rewardedStatusMessage(AdManager.RewardedStatus.NO_FILL).orEmpty()
                        )
                    }
                    val recentFailure = AdManager.rewardedStatus.value

                    when {
                        // 1. Offline: answer instantly
                        !AdManager.isOnline(ctx) -> {
                            AdManager.requestRewardedNow(ctx)
                            showUnavailable(AdManager.RewardedStatus.NO_INTERNET)
                        }
                        // 2. Ad already loaded: play it straight away
                        isReady -> playHintAd()
                        // 3. The last load just failed (ads blocked by DNS, no fill): show why
                        //    instantly and retry quietly, so the next tap can succeed
                        AdManager.rewardedFailedRecently(HINT_AD_RECENT_FAILURE_MS) -> {
                            AdManager.requestRewardedNow(ctx)
                            showUnavailable(recentFailure)
                        }
                        // 4. Otherwise load now and wait briefly
                        else -> {
                            gameViewModel.setHintAdLoading(true)
                            AdManager.requestRewardedNow(ctx)
                            hintAdJob?.cancel()
                            hintAdJob = coroutineScope.launch {
                                val result = withTimeoutOrNull(HINT_AD_MAX_WAIT_MS) {
                                    AdManager.rewardedStatus.first { it != AdManager.RewardedStatus.LOADING }
                                } ?: AdManager.RewardedStatus.LOADING
                                gameViewModel.setHintAdLoading(false)
                                // Never pop an ad after the player has left this board
                                val now = gameViewModel.uiState.value
                                val stillOnBoard = currentScreen == AppScreen.GAMEPLAY &&
                                    now.currentLevelNumber == tapState.currentLevelNumber &&
                                    now.isDailyChallengeMode == tapState.isDailyChallengeMode &&
                                    !now.isLevelComplete && !now.isGameOver
                                if (!stillOnBoard) return@launch
                                if (result == AdManager.RewardedStatus.READY && !AdManager.isRewardedAdExpired()) {
                                    playHintAd()
                                } else {
                                    showUnavailable(result)
                                }
                            }
                        }
                    }
                }
            }

            var showNotificationPrePrompt by remember { mutableStateOf(false) }
            var prePromptType by remember { mutableStateOf("unlock") }
            var showNotificationSettingsDialog by remember { mutableStateOf(false) }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                gameViewModel.analytics.logNotificationPermissionResult(if (isGranted) "granted" else "denied")
                gameViewModel.onNotificationPermissionResult(isGranted)
            }

            LaunchedEffect(intent) {
                if (intent.getStringExtra("EXTRA_NAVIGATE_TO") == "daily_challenge") {
                    intent.removeExtra("EXTRA_NAVIGATE_TO")
                    val activeStreak = maxOf(uiState.streakDays, gameViewModel.getStreakDaysSynchronous())
                    gameViewModel.analytics.logDailyNotificationClicked(activeStreak, "play_challenge")
                    gameViewModel.startDailyChallenge()
                    currentScreen = AppScreen.GAMEPLAY
                }
            }

            // Soft-ask notification pre-prompt on unlock (first time) or 3-day streak (second chance)
            LaunchedEffect(uiState.currentLevelNumber, uiState.lastCompletedLevelNumber, uiState.notificationPermissionAskCount, uiState.streakDays) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = NotificationHelper.hasNotificationPermission(this@MainActivity)
                    if (!hasPermission) {
                        val isUnlocked = DailyChallengeRules.isDailyUnlocked(
                            currentLevel = uiState.currentLevelNumber,
                            lastCompletedLevel = uiState.lastCompletedLevelNumber,
                            hasSeenCelebration = uiState.hasSeenDailyUnlockCelebration
                        )
                        if (isUnlocked && uiState.notificationPermissionAskCount == 0) {
                            prePromptType = "unlock"
                            showNotificationPrePrompt = true
                            gameViewModel.analytics.logNotificationPrePromptShown("unlock")
                        } else if (uiState.streakDays >= 3 && uiState.notificationPermissionAskCount == 1) {
                            prePromptType = "streak_3"
                            showNotificationPrePrompt = true
                            gameViewModel.analytics.logNotificationPrePromptShown("streak_3")
                        }
                    }
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
                                        launchGameplayWithPendingAdCheck {
                                            currentScreen = AppScreen.GAMEPLAY
                                        }
                                    },
                                    onDailyChallengeClick = {
                                        gameViewModel.playClick()
                                        if (!com.arrowaway.puzzle.config.RemoteConfigManager.isDailyChallengeEnabled) {
                                            Toast.makeText(this@MainActivity, "Daily Challenges are temporarily offline for maintenance.", Toast.LENGTH_SHORT).show()
                                            return@HomeScreen
                                        }
                                        val unlockLevel = com.arrowaway.puzzle.config.RemoteConfigManager.dailyChallengeUnlockLevel
                                        val isUnlocked = DailyChallengeRules.isDailyUnlocked(
                                            currentLevel = uiState.currentLevelNumber,
                                            lastCompletedLevel = uiState.lastCompletedLevelNumber,
                                            hasSeenCelebration = uiState.hasSeenDailyUnlockCelebration,
                                            unlockLevel = unlockLevel
                                        )

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
                                    showDailyReminder = uiState.showDailyLaunchPrompt &&
                                        com.arrowaway.puzzle.config.RemoteConfigManager.isDailyChallengeEnabled,
                                    onDailyReminderShown = {
                                        gameViewModel.analytics.logDailyReminderShown(uiState.streakDays)
                                    },
                                    onDailyReminderPlay = {
                                        gameViewModel.playClick()
                                        gameViewModel.analytics.logDailyReminderAction("play", uiState.streakDays)
                                        gameViewModel.dismissDailyLaunchPrompt()
                                        val today = LocalDate.now().toString()
                                        gameViewModel.startDailyChallenge(today)
                                        launchGameplayWithPendingAdCheck {
                                            currentScreen = AppScreen.GAMEPLAY
                                        }
                                    },
                                    onDailyReminderDismiss = {
                                        gameViewModel.playClick()
                                        gameViewModel.analytics.logDailyReminderAction("dismiss", uiState.streakDays)
                                        gameViewModel.dismissDailyLaunchPrompt()
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
                                        devTestInitialDailyMode = false
                                        currentScreen = AppScreen.DEV_TEST
                                    },
                                    onReviewDailyLevelsClick = {
                                        gameViewModel.playClick()
                                        devTestInitialDailyMode = true
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
                                    onSkipCelebration = {
                                        gameViewModel.skipCelebrationToVictory()
                                    },
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
                                        val unlockLevel = com.arrowaway.puzzle.config.RemoteConfigManager.dailyChallengeUnlockLevel
                                        if (uiState.currentLevelNumber == unlockLevel && !uiState.hasSeenDailyUnlockCelebration) {
                                            gameViewModel.markDailyUnlockCelebrationSeen()
                                        }
                                        val wasDaily = uiState.isDailyChallengeMode
                                        val curLevel = if (wasDaily) 999 else uiState.currentLevelNumber

                                        val advance = {
                                            gameViewModel.nextLevel()
                                            if (wasDaily) {
                                                currentScreen = AppScreen.DAILY_CHALLENGE
                                            }
                                        }

                                        AdManager.showInterstitialAd(
                                            activity = this@MainActivity,
                                            currentLevel = curLevel,
                                            onDismissed = { advance() }
                                        )
                                    },
                                    onStartDailyChallengeClick = {
                                        gameViewModel.playClick()
                                        gameViewModel.markDailyUnlockCelebrationSeen()
                                        val today = LocalDate.now().toString()
                                        gameViewModel.startDailyChallenge(today)
                                        currentScreen = AppScreen.GAMEPLAY
                                    },
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !NotificationHelper.hasNotificationPermission(this@MainActivity)) {
                                            prePromptType = "unlock"
                                            showNotificationPrePrompt = true
                                            gameViewModel.analytics.logNotificationPrePromptShown("unlock")
                                        }
                                    },
                                    onClaimDoubleCoins = { amount ->
                                        gameViewModel.claimDoubleCoins(amount)
                                    },
                                    onRestartClick = {
                                        if (uiState.isGameOver && (uiState.currentLevelNumber > 10 || uiState.isDailyChallengeMode) && !isRewardedAdReady) {
                                            gameViewModel.analytics.logAdRewardUnavailable("lives_expiry", rewardedUnavailableReason)
                                        }
                                        gameViewModel.playClick()
                                        gameViewModel.restartLevel()
                                    },
                                    onBackToHomeClick = {
                                        gameViewModel.onAbandonGameplay("back_to_home")
                                        gameViewModel.playClick()
                                        gameViewModel.markHasPlayedBefore()
                                        gameViewModel.resetGridLines()
                                        val wasDaily = uiState.isDailyChallengeMode
                                        val isVictory = uiState.isLevelComplete
                                        val curLevel = if (wasDaily) 999 else uiState.currentLevelNumber

                                        val exitToHome = {
                                            if (isVictory) {
                                                gameViewModel.onVictoryHomeClick()
                                            } else if (wasDaily) {
                                                gameViewModel.loadLevel(uiState.currentLevelNumber)
                                            } else if (uiState.isGameOver) {
                                                gameViewModel.loadLevel(uiState.currentLevelNumber)
                                            }
                                            currentScreen = if (wasDaily) AppScreen.DAILY_CHALLENGE else AppScreen.HOME
                                        }

                                        if (isVictory) {
                                            AdManager.showInterstitialAd(
                                                activity = this@MainActivity,
                                                currentLevel = curLevel,
                                                onDismissed = { exitToHome() }
                                            )
                                        } else {
                                            exitToHome()
                                        }
                                    },
                                    onRequestAdHint = {
                                        if (!gameViewModel.hasSeenHintAdIntroForCurrentMode) {
                                            // First time out of hints: explain the rewarded ad before playing one
                                            gameViewModel.showHintAdIntro()
                                        } else {
                                            requestHintAd()
                                        }
                                    },
                                    onHintAdIntroWatch = {
                                        gameViewModel.closeHintAdIntro()
                                        requestHintAd()
                                    },
                                    onHintAdIntroDismiss = {
                                        gameViewModel.analytics.logAdRewardUnavailable("hint_refill", "declined_intro")
                                        gameViewModel.closeHintAdIntro()
                                    },
                                    onDismissHintError = {
                                        gameViewModel.clearHintErrorMessage()
                                    },
                                    onClaimFreeLives = {
                                        if (uiState.currentLevelNumber <= 10 && !uiState.isDailyChallengeMode) {
                                            gameViewModel.claimFreeLives(3)
                                        } else {
                                            AdManager.showRewardedAd(
                                                activity = this@MainActivity,
                                                placement = "lives_expiry",
                                                onRewardEarned = {
                                                    gameViewModel.claimFreeLives(LIVES_PER_REWARDED_AD)
                                                },
                                                onAdClosed = { _ -> }
                                            )
                                        }
                                    },
                                    onToggleSound = {
                                        gameViewModel.toggleSound()
                                    },
                                    onToggleVibration = {
                                        gameViewModel.toggleVibration()
                                    },
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
                                    },
                                    rewardedStatus = rewardedStatus,
                                    onTriggerAdPreload = {
                                        AdManager.requestRewardedNow(this@MainActivity)
                                    }
                                )
                            }
                            AppScreen.DEV_TEST -> {
                                if (uiState.isDevMode) {
                                    DevTestScreen(
                                        theme = uiState.activeTheme,
                                        audioHapticManager = gameViewModel.audioHapticManager,
                                        initialDailyMode = devTestInitialDailyMode,
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
                                    isAnalyticsEnabled = uiState.isAnalyticsEnabled,
                                    onToggleAnalytics = {
                                        gameViewModel.playClick()
                                        val isNowEnabled = gameViewModel.toggleAnalytics()
                                        val msg = if (isNowEnabled) {
                                            "📊 Analytics tracking enabled"
                                        } else {
                                            "🚫 Analytics tracking disabled for testing"
                                        }
                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                    },
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
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                val hasPermission = NotificationHelper.hasNotificationPermission(this@MainActivity)
                                                if (!hasPermission) {
                                                    val canShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                                                        this@MainActivity,
                                                        Manifest.permission.POST_NOTIFICATIONS
                                                    )
                                                    if (!canShowRationale && uiState.notificationPermissionAskCount > 0) {
                                                        showNotificationSettingsDialog = true
                                                    } else {
                                                        prePromptType = "settings"
                                                        showNotificationPrePrompt = true
                                                        gameViewModel.analytics.logNotificationPrePromptShown("settings")
                                                    }
                                                } else {
                                                    gameViewModel.setDailyRemindersEnabled(true)
                                                    NotificationHelper.scheduleDailyReminderAlarm(this@MainActivity)
                                                }
                                            } else {
                                                gameViewModel.setDailyRemindersEnabled(true)
                                                NotificationHelper.scheduleDailyReminderAlarm(this@MainActivity)
                                            }
                                        } else {
                                            gameViewModel.setDailyRemindersEnabled(false)
                                            NotificationHelper.cancelDailyReminderAlarm(this@MainActivity)
                                        }
                                    },
                                    onThemeSelect = { selectedTheme ->
                                        gameViewModel.playClick()
                                        gameViewModel.analytics.logSettingPreferenceChanged("theme", uiState.activeTheme.name, selectedTheme.name)
                                        gameViewModel.setTheme(selectedTheme)
                                    },
                                    onResetGameData = {
                                        gameViewModel.resetGameData()
                                        currentScreen = AppScreen.GAMEPLAY
                                        Toast.makeText(this@MainActivity, "Game progress reset to Level 1", Toast.LENGTH_SHORT).show()
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
                                        launchGameplayWithPendingAdCheck {
                                            currentScreen = AppScreen.GAMEPLAY
                                        }
                                    },
                                    onBackClick = {
                                        gameViewModel.playClick()
                                        currentScreen = AppScreen.HOME
                                    }
                                )
                            }
                        }
                    }

                    // Notification Pre-Prompt Soft Ask Dialog
                    if (showNotificationPrePrompt) {
                        NotificationPrePromptDialog(
                            theme = uiState.activeTheme,
                            isStreakPrompt = (prePromptType == "streak_3"),
                            streakDays = uiState.streakDays,
                            onTurnOnClick = {
                                showNotificationPrePrompt = false
                                gameViewModel.incrementNotificationAskCount()
                                gameViewModel.analytics.logNotificationPrePromptResult(prePromptType, "accept")
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onNotNowClick = {
                                showNotificationPrePrompt = false
                                gameViewModel.incrementNotificationAskCount()
                                gameViewModel.analytics.logNotificationPrePromptResult(prePromptType, "not_now")
                            }
                        )
                    }

                    // Notification Settings Deep-link Dialog (for permanently denied permissions)
                    if (showNotificationSettingsDialog) {
                        NotificationSettingsDialog(
                            theme = uiState.activeTheme,
                            onOpenSettingsClick = {
                                showNotificationSettingsDialog = false
                                awaitingNotificationSettingsReturn = true
                                try {
                                    val settingsIntent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                                    }
                                    startActivity(settingsIntent)
                                } catch (e: Exception) {
                                    val settingsIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.fromParts("package", packageName, null)
                                    }
                                    startActivity(settingsIntent)
                                }
                            },
                            onDismiss = {
                                showNotificationSettingsDialog = false
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

    companion object {
        private const val KEY_AWAITING_NOTIFICATION_SETTINGS = "key_awaiting_notification_settings"
    }
}

/** Hint button: a failure this recent is shown at once instead of waiting on another load. */
private const val HINT_AD_RECENT_FAILURE_MS = 20_000L

/** Hint button: longest the spinner shows before we say no ad is available. */
private const val HINT_AD_MAX_WAIT_MS = 2_500L

/** Lives restored by one rewarded ad on level 11+ and the Daily Challenge (levels 1-10 refill 3 for free). */
private const val LIVES_PER_REWARDED_AD = 1
