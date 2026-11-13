package com.arrowaway.puzzle.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arrowaway.puzzle.analytics.AnalyticsManager
import com.arrowaway.puzzle.analytics.CrashLogger
import com.arrowaway.puzzle.audio.AudioHapticManager
import com.arrowaway.puzzle.config.RemoteConfigManager
import com.arrowaway.puzzle.core.DailyChallengeRules
import com.arrowaway.puzzle.core.LevelDifficultyRules
import com.arrowaway.puzzle.review.InAppReviewManager
import com.arrowaway.puzzle.core.engine.GridOccupancyMap
import com.arrowaway.puzzle.core.engine.LevelGenerator
import com.arrowaway.puzzle.core.engine.LevelLoader
import com.arrowaway.puzzle.core.engine.PuzzleSolver
import com.arrowaway.puzzle.core.model.Arrow
import com.arrowaway.puzzle.core.model.ArrowState
import com.arrowaway.puzzle.core.model.LevelData
import com.arrowaway.puzzle.data.UserPreferencesRepository
import com.arrowaway.puzzle.notification.NotificationHelper
import com.arrowaway.puzzle.ui.theme.ThemePreset
import com.arrowaway.puzzle.ui.theme.ThemePresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil

sealed class BoosterEffectAnimation {
    data class Hammer(val targetArrow: Arrow, val progress: Float) : BoosterEffectAnimation()
    data class Wand(val targetArrow: Arrow, val progress: Float) : BoosterEffectAnimation()
}

data class GameUiState(
    val levelData: LevelData? = null,
    val arrows: List<Arrow> = emptyList(),
    val heartsRemaining: Int = 3,
    val maxHearts: Int = 3,
    val isLevelComplete: Boolean = false,
    val isCelebrationBurstActive: Boolean = false,
    val isGameOver: Boolean = false,
    val comboCount: Int = 0,
    val cheerMessage: String? = null,
    val cheerFromLeft: Boolean = true,
    val isGridLinesActive: Boolean = false,
    val isHammerModeActive: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val previewRayArrowIds: Set<String> = emptySet(),
    val activeTheme: ThemePreset = ThemePresets.WarmSepia,
    val currentLevelNumber: Int = 1,
    val lastCompletedLevelNumber: Int = 1,
    val coins: Int = 100,
    val streakDays: Int = 0,
    val isStreakEarnedThisLevel: Boolean = false,
    val hintsAvailable: Int = 0,
    val isHintAdLoading: Boolean = false,
    val hintErrorMessage: String? = null,
    val dailyComplimentaryHintsRemaining: Int = 1,
    val levelFreeHintsRemaining: Int = 1,
    val hammersAvailable: Int = 2,
    val wandsAvailable: Int = 2,
    val isDailyChallengeMode: Boolean = false,
    val activeDailyDate: String? = null,
    val activeDailyIndex: Int = 1,
    val activeDailyPool: String = "standard",
    val completedDailyDates: Set<String> = emptySet(),
    val levelStars: Map<Int, Int> = emptyMap(),
    val hasPlayedBefore: Boolean = false,
    val globalLivesAvailable: Int = 6,
    val showHintAdPrompt: Boolean = false,
    val activeBoosterAnimation: BoosterEffectAnimation? = null,
    val levelCompletionTimeSeconds: Int = 0,
    val levelMovesCount: Int = 0,
    val levelFinalScore: Int = 0,
    val levelAccuracyPercent: Int = 100,
    val levelMistakesCount: Int = 0,
    val levelHintsUsedCount: Int = 0,
    val levelStarsEarned: Int = 3,
    /** Hints granted by a one-time milestone reward on the level just completed (0 = none). */
    val milestoneRewardHints: Int = 0,
    val hasSeenDailyUnlockCelebration: Boolean = false,
    val dailyRemindersEnabled: Boolean = true,
    val hasRequestedNotificationPermission: Boolean = false,
    val notificationPermissionAskCount: Int = 0,
    val showDailyLaunchPrompt: Boolean = false,
    val restartTrigger: Long = 0L,
    val isAutoSolving: Boolean = false,
    val isDevMode: Boolean = com.arrowaway.puzzle.BuildConfig.DEBUG,
    val isGamePaused: Boolean = false,
    val isAppInBackground: Boolean = false,
    val isHintButtonVisible: Boolean = true,
    val isGridButtonVisible: Boolean = true,
    val hintTimerSeconds: Int = 10,
    val reviewPromptTrigger: Long = 0L,
    val isAnalyticsEnabled: Boolean = !com.arrowaway.puzzle.BuildConfig.DEBUG
) {
    val previewRayArrowId: String? get() = previewRayArrowIds.firstOrNull()
}

class GameViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repository = UserPreferencesRepository(app)
    val audioHapticManager = AudioHapticManager(app)
    val analytics = AnalyticsManager.getInstance(app)

    private var sessionId = java.util.UUID.randomUUID().toString()
    private var sessionStartTimeMs: Long = System.currentTimeMillis()
    private var lastBackgroundTimeMs: Long = 0L
    private var levelsAttemptedInSession: Int = 0
    private var levelsCompletedInSession: Int = 0
    private val levelAttemptCountMap = mutableMapOf<Int, Int>()
    private var levelStartWallTimeMs: Long = 0L
    private var hasInteractedWithArrowThisLevel: Boolean = false
    private var hasLoggedSessionStart: Boolean = false

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var occupancyMap = GridOccupancyMap(4, 4)
    private var cheerDismissJob: Job? = null
    private var celebrationJob: Job? = null
    private var accumulatedActivePlayTimeMs: Long = 0L
    private var currentSegmentStartTimeMs: Long = 0L
    private var isTimerRunning: Boolean = false
    private var isGamePaused: Boolean = false
    private var isAppInBackground: Boolean = false
    private var isGameplayScreenActive: Boolean = false
    private var hasGameplayStarted: Boolean = false
    private var levelMovesAttempted: Int = 0
    private var levelMistakesCount: Int = 0
    private var levelHintsUsedCount: Int = 0
    private var hasDismissedDailyLaunchPromptThisSession: Boolean = false
    private var milestone25Fired: Boolean = false
    private var milestone50Fired: Boolean = false
    private var milestone70Fired: Boolean = false
    private var milestone90Fired: Boolean = false
    private var lastMilestoneTimestamp: Long = 0L

    private fun resetMilestoneFlags() {
        milestone25Fired = false
        milestone50Fired = false
        milestone70Fired = false
        milestone90Fired = false
        lastMilestoneTimestamp = 0L
    }

    init {
        viewModelScope.launch {
            repository.userGameData.collect { data ->
                val theme = ThemePresets.all.getOrElse(data.themeIndex) { ThemePresets.WarmSepia }
                audioHapticManager.isSoundEnabled = data.soundEnabled
                audioHapticManager.isVibrationEnabled = data.vibrationEnabled

                val todayStr = LocalDate.now().toString()
                val isDailyCompletedToday = data.completedDailyDates.contains(todayStr)
                val isDailyUnlocked = DailyChallengeRules.isDailyUnlocked(
                    currentLevel = data.currentLevel,
                    hasSeenCelebration = data.hasSeenDailyUnlockCelebration
                )
                val shouldShowDailyPrompt = isDailyUnlocked && !isDailyCompletedToday && !hasDismissedDailyLaunchPromptThisSession

                _uiState.update { current ->
                    current.copy(
                        currentLevelNumber = if (current.levelData == null) data.currentLevel else current.currentLevelNumber,
                        coins = data.coins,
                        streakDays = data.streakDays,
                        hintsAvailable = data.freeHints,
                        hammersAvailable = data.freeHammers,
                        wandsAvailable = data.freeWands,
                        completedDailyDates = data.completedDailyDates,
                        levelStars = data.levelStars,
                        hasPlayedBefore = data.hasPlayedBefore,
                        globalLivesAvailable = data.globalLives,
                        lastCompletedLevelNumber = if (current.levelData == null) maxOf(0, data.currentLevel - 1) else current.lastCompletedLevelNumber,
                        isSoundEnabled = data.soundEnabled,
                        isVibrationEnabled = data.vibrationEnabled,
                        activeTheme = theme,
                        hasSeenDailyUnlockCelebration = data.hasSeenDailyUnlockCelebration,
                        dailyRemindersEnabled = data.dailyRemindersEnabled,
                        hasRequestedNotificationPermission = data.hasRequestedNotificationPermission,
                        notificationPermissionAskCount = data.notificationPermissionAskCount,
                        showDailyLaunchPrompt = shouldShowDailyPrompt,
                        isDevMode = data.isDevMode,
                        isHintButtonVisible = data.isHintButtonVisible,
                        isGridButtonVisible = data.isGridButtonVisible,
                        hintTimerSeconds = data.hintTimerSeconds,
                        isAnalyticsEnabled = data.isAnalyticsEnabled
                    )
                }
                analytics.setAnalyticsEnabled(data.isAnalyticsEnabled)
                analytics.setUserPropertyMaxLevel(data.currentLevel)
                analytics.setUserPropertyStreakDays(data.streakDays)
                analytics.setUserPropertyTotalLevelsCompleted(data.levelStars.size)

                if (!hasLoggedSessionStart) {
                    hasLoggedSessionStart = true
                    analytics.logAppSessionStart(
                        sessionId = sessionId,
                        isColdStart = true,
                        theme = theme.name,
                        currentLevel = data.currentLevel
                    )
                    CrashLogger.setCustomKey("current_level", data.currentLevel)
                    CrashLogger.setCustomKey("theme", theme.name)
                }

                // If not loaded yet, load current level
                if (_uiState.value.levelData == null) {
                    loadLevel(data.currentLevel)
                }
            }
        }
    }

    fun markNotificationPermissionRequested() {
        viewModelScope.launch {
            repository.markNotificationPermissionRequested()
            _uiState.update { it.copy(hasRequestedNotificationPermission = true) }
        }
    }

    fun dismissDailyLaunchPrompt() {
        hasDismissedDailyLaunchPromptThisSession = true
        _uiState.update { it.copy(showDailyLaunchPrompt = false) }
    }

    fun toggleDevMode(): Boolean {
        if (!com.arrowaway.puzzle.BuildConfig.DEBUG) return false
        val newMode = !_uiState.value.isDevMode
        viewModelScope.launch {
            repository.setDevMode(newMode)
        }
        _uiState.update { it.copy(isDevMode = newMode) }
        return newMode
    }

    fun incrementNotificationAskCount() {
        viewModelScope.launch {
            val count = repository.incrementNotificationAskCount()
            _uiState.update { it.copy(notificationPermissionAskCount = count, hasRequestedNotificationPermission = true) }
        }
    }

    fun toggleAnalytics(): Boolean {
        val newState = !_uiState.value.isAnalyticsEnabled
        _uiState.update { it.copy(isAnalyticsEnabled = newState) }
        analytics.setAnalyticsEnabled(newState)
        viewModelScope.launch {
            repository.setAnalyticsEnabled(newState)
        }
        return newState
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isAnalyticsEnabled = enabled) }
        analytics.setAnalyticsEnabled(enabled)
        viewModelScope.launch {
            repository.setAnalyticsEnabled(enabled)
        }
    }

    fun markDailyUnlockCelebrationSeen() {
        viewModelScope.launch {
            repository.markDailyUnlockCelebrationSeen()
            _uiState.update { it.copy(hasSeenDailyUnlockCelebration = true) }
        }
    }

    fun getStreakDaysSynchronous(): Int = repository.getStreakDaysSynchronous()

    fun onNotificationPermissionResult(isGranted: Boolean) {
        viewModelScope.launch {
            repository.setDailyRemindersEnabled(isGranted)
            _uiState.update { it.copy(dailyRemindersEnabled = isGranted) }
            if (isGranted) {
                NotificationHelper.scheduleDailyReminderAlarm(app)
            } else {
                NotificationHelper.cancelDailyReminderAlarm(app)
            }
        }
    }

    fun setDailyRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDailyRemindersEnabled(enabled)
            _uiState.update { it.copy(dailyRemindersEnabled = enabled) }
            if (enabled) {
                NotificationHelper.scheduleDailyReminderAlarm(app)
            } else {
                NotificationHelper.cancelDailyReminderAlarm(app)
            }
        }
    }

    fun toggleDailyReminders() {
        setDailyRemindersEnabled(!_uiState.value.dailyRemindersEnabled)
    }

    fun resumeActiveTimer() {
        if (!hasGameplayStarted) return
        if (!isGameplayScreenActive) return
        if (isGamePaused || isAppInBackground) return
        if (_uiState.value.isLevelComplete || _uiState.value.isGameOver) return
        if (!isTimerRunning) {
            currentSegmentStartTimeMs = android.os.SystemClock.elapsedRealtime()
            isTimerRunning = true
        }
    }

    fun pauseActiveTimer() {
        if (isTimerRunning) {
            val now = android.os.SystemClock.elapsedRealtime()
            accumulatedActivePlayTimeMs += (now - currentSegmentStartTimeMs).coerceAtLeast(0L)
            currentSegmentStartTimeMs = 0L
            isTimerRunning = false
        }
    }

    fun getActivePlayTimeSeconds(): Int {
        val current = if (isTimerRunning && currentSegmentStartTimeMs > 0L) {
            (android.os.SystemClock.elapsedRealtime() - currentSegmentStartTimeMs).coerceAtLeast(0L)
        } else 0L
        val totalMs = accumulatedActivePlayTimeMs + current
        return (totalMs / 1000L).toInt().coerceAtLeast(1)
    }

    fun setGamePaused(paused: Boolean) {
        isGamePaused = paused
        _uiState.update { it.copy(isGamePaused = paused) }
        if (paused) {
            pauseActiveTimer()
            val remaining = _uiState.value.arrows.count { it.state != ArrowState.CLEARED }
            analytics.logPuzzlePaused(_uiState.value.currentLevelNumber, remaining, getActivePlayTimeSeconds().toLong())
        } else {
            resumeActiveTimer()
            analytics.logPuzzleResumed(_uiState.value.currentLevelNumber, 0L)
        }
    }

    fun onAppBackgrounded(activeScreen: String = "GAMEPLAY") {
        isAppInBackground = true
        lastBackgroundTimeMs = System.currentTimeMillis()
        _uiState.update { it.copy(isAppInBackground = true) }
        pauseActiveTimer()
        val wasPuzzleActive = isGameplayScreenActive && !_uiState.value.isLevelComplete && !_uiState.value.isGameOver
        analytics.logAppBackground(activeScreen, wasPuzzleActive)
    }

    fun onAppForegrounded(isGameplayScreen: Boolean) {
        isAppInBackground = false
        isGameplayScreenActive = isGameplayScreen
        _uiState.update { it.copy(isAppInBackground = false) }
        if (lastBackgroundTimeMs > 0L) {
            val diffSec = (System.currentTimeMillis() - lastBackgroundTimeMs) / 1000L
            analytics.logAppForeground(diffSec)
            if (diffSec >= 1800L) {
                val sessionDuration = (System.currentTimeMillis() - sessionStartTimeMs) / 1000L
                analytics.logAppSessionEnd(sessionId, sessionDuration, levelsAttemptedInSession, levelsCompletedInSession)
                sessionId = java.util.UUID.randomUUID().toString()
                sessionStartTimeMs = System.currentTimeMillis()
                levelsAttemptedInSession = 0
                levelsCompletedInSession = 0
                analytics.logAppSessionStart(sessionId, false, _uiState.value.activeTheme.name, _uiState.value.currentLevelNumber)
            }
        }
        if (isGameplayScreen && !isGamePaused) {
            resumeActiveTimer()
        }
    }

    fun onAbandonGameplay(exitPoint: String = "back_button") {
        if (!_uiState.value.isLevelComplete && !_uiState.value.isGameOver && hasGameplayStarted) {
            val elapsed = getActivePlayTimeSeconds().toLong()
            val remaining = _uiState.value.arrows.count { it.state != ArrowState.CLEARED }
            analytics.logLevelAbandoned(_uiState.value.currentLevelNumber, elapsed, remaining, exitPoint)
        }
    }

    fun onReviewPromptHandled() {
        viewModelScope.launch {
            repository.recordReviewPromptShown(System.currentTimeMillis())
        }
    }

    fun resetGridLines() {
        if (_uiState.value.isGridLinesActive) {
            _uiState.update { it.copy(isGridLinesActive = false) }
        }
    }

    fun onNavigateToGameplay() {
        isGameplayScreenActive = true
        resetGridLines()
        if (!isGamePaused && !isAppInBackground) {
            resumeActiveTimer()
        }
    }

    fun onNavigateAwayFromGameplay() {
        isGameplayScreenActive = false
        pauseActiveTimer()
        resetGridLines()
    }

    fun setHintButtonVisible(visible: Boolean) {
        viewModelScope.launch { repository.setHintButtonVisible(visible) }
        _uiState.update { it.copy(isHintButtonVisible = visible) }
    }

    fun setGridButtonVisible(visible: Boolean) {
        viewModelScope.launch { repository.setGridButtonVisible(visible) }
        _uiState.update { it.copy(isGridButtonVisible = visible) }
    }

    fun setHintTimerSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(0, 55)
        viewModelScope.launch { repository.setHintTimerSeconds(clamped) }
        _uiState.update { it.copy(hintTimerSeconds = clamped) }
    }

    private fun resetLevelMetrics() {
        accumulatedActivePlayTimeMs = 0L
        currentSegmentStartTimeMs = 0L
        isTimerRunning = false
        hasGameplayStarted = false
        isGamePaused = false
        levelMovesAttempted = 0
        levelMistakesCount = 0
        levelHintsUsedCount = 0
        resetMilestoneFlags()
    }

    fun onGameplayStarted() {
        hasGameplayStarted = true
        resumeActiveTimer()
        levelMovesAttempted = 0
        levelMistakesCount = 0
        levelHintsUsedCount = 0
    }

    fun loadLevel(levelNumber: Int) {
        celebrationJob?.cancel()
        stopAutoSolve()
        resetLevelMetrics()
        levelsAttemptedInSession++
        val attempts = (levelAttemptCountMap[levelNumber] ?: 0) + 1
        levelAttemptCountMap[levelNumber] = attempts
        levelStartWallTimeMs = System.currentTimeMillis()
        hasInteractedWithArrowThisLevel = false

        com.arrowaway.puzzle.ads.AdManager.preloadRewardedAd(app)
        val level = LevelLoader.loadLevel(app, levelNumber)
        occupancyMap = GridOccupancyMap(level.xSize, level.ySize).apply {
            rebuild(level.arrows)
        }

        analytics.logLevelStart(
            levelNumber = levelNumber,
            gameMode = "campaign",
            totalArrows = level.arrows.size,
            gridDimensions = "${level.xSize}x${level.ySize}",
            attemptNumber = attempts
        )
        CrashLogger.setCustomKey("current_level", levelNumber)
        CrashLogger.setCustomKey("game_mode", "campaign")

        _uiState.update {
            it.copy(
                levelData = level,
                arrows = level.arrows,
                heartsRemaining = level.maxHearts,
                maxHearts = level.maxHearts,
                currentLevelNumber = levelNumber,
                isLevelComplete = false,
                milestoneRewardHints = 0,
                isStreakEarnedThisLevel = false,
                isGameOver = false,
                comboCount = 0,
                cheerMessage = null,
                isHammerModeActive = false,
                isGridLinesActive = false,
                previewRayArrowIds = emptySet(),
                isDailyChallengeMode = false,
                activeDailyDate = null,
                activeDailyIndex = 1,
                dailyComplimentaryHintsRemaining = if (levelNumber <= 10) 999 else 0,
                levelFreeHintsRemaining = if (levelNumber <= 10) 999 else 0,
                showHintAdPrompt = false,
                isHintAdLoading = false,
                hintErrorMessage = null,
                isGamePaused = false,
                restartTrigger = System.currentTimeMillis()
            )
        }
    }

    fun startDailyChallenge(dateString: String = LocalDate.now().toString()) {
        stopAutoSolve()
        resetLevelMetrics()
        levelsAttemptedInSession++
        levelStartWallTimeMs = System.currentTimeMillis()
        hasInteractedWithArrowThisLevel = false

        val date = try {
            LocalDate.parse(dateString)
        } catch (e: Exception) {
            LocalDate.now()
        }
        val dayOfYear = date.dayOfYear
        val easyDailyUntilLevel = RemoteConfigManager.easyDailyUntilLevel
        // uiState may still hold the default level if DataStore hasn't loaded yet (e.g. app opened from the
        // daily notification), so also read the synchronously mirrored level and use the higher value.
        val playerLevel = maxOf(_uiState.value.currentLevelNumber, repository.getCurrentLevelSynchronous())
        val useEasyPool = playerLevel < easyDailyUntilLevel
        val poolSize = if (useEasyPool) LevelLoader.TOTAL_EASY_DAILY_LEVELS else LevelLoader.TOTAL_DAILY_LEVELS
        val dailyIndex = ((dayOfYear - 1) % poolSize) + 1
        val level = if (useEasyPool) {
            LevelLoader.loadEasyDailyLevel(app, dailyIndex)
        } else {
            LevelLoader.loadDailyLevel(app, dailyIndex)
        }
        com.arrowaway.puzzle.ads.AdManager.preloadRewardedAd(app)
        val dailyPool = if (useEasyPool) "easy" else "standard"
        occupancyMap = GridOccupancyMap(level.xSize, level.ySize).apply {
            rebuild(level.arrows)
        }

        analytics.logDailyChallengeStart(
            puzzleDate = date.toString(),
            dailyLevelIndex = dailyIndex,
            arrowCount = level.arrows.size,
            dailyPool = dailyPool
        )
        analytics.logLevelStart(
            levelNumber = dailyIndex,
            gameMode = "daily",
            totalArrows = level.arrows.size,
            gridDimensions = "${level.xSize}x${level.ySize}",
            attemptNumber = 1
        )
        CrashLogger.setCustomKey("current_level", dailyIndex)
        CrashLogger.setCustomKey("game_mode", "daily")

        _uiState.update {
            it.copy(
                levelData = level,
                arrows = level.arrows,
                heartsRemaining = level.maxHearts,
                maxHearts = level.maxHearts,
                isLevelComplete = false,
                milestoneRewardHints = 0,
                isStreakEarnedThisLevel = false,
                isGameOver = false,
                comboCount = 0,
                cheerMessage = null,
                isHammerModeActive = false,
                isGridLinesActive = false,
                previewRayArrowIds = emptySet(),
                isDailyChallengeMode = true,
                activeDailyDate = date.toString(),
                activeDailyIndex = dailyIndex,
                activeDailyPool = dailyPool,
                dailyComplimentaryHintsRemaining = 0,
                levelFreeHintsRemaining = 0,
                showHintAdPrompt = false,
                isHintAdLoading = false,
                hintErrorMessage = null,
                isGamePaused = false,
                restartTrigger = System.currentTimeMillis()
            )
        }
    }

    fun onArrowClicked(clickedArrow: Arrow) {
        if (_uiState.value.isLevelComplete || _uiState.value.isGameOver) return

        if (!hasInteractedWithArrowThisLevel) {
            hasInteractedWithArrowThisLevel = true
            val inspectionSec = ((System.currentTimeMillis() - levelStartWallTimeMs) / 1000L).coerceAtLeast(0L)
            analytics.logLevelFirstInteraction(_uiState.value.currentLevelNumber, inspectionSec)
        }

        val currentArrow = _uiState.value.arrows.firstOrNull { it.id == clickedArrow.id } ?: clickedArrow
        // Multi-tap prevention: Ignore clicks on arrows that are actively flying out or already cleared.
        // WRONG_FLASH arrows CAN be re-tapped immediately (user tapping blocked arrow again is valid input).
        if (currentArrow.state == ArrowState.FLYING_OUT || currentArrow.state == ArrowState.CLEARED) return
        // If arrow is still in WRONG_FLASH, snap it back to IDLE first, then process the tap immediately
        if (currentArrow.state == ArrowState.WRONG_FLASH) {
            _uiState.update { state ->
                state.copy(arrows = state.arrows.map {
                    if (it.id == currentArrow.id) it.copyWithState(ArrowState.IDLE, 0f, highlighted = it.isHighlighted)
                    else it
                })
            }
        }

        if (!hasGameplayStarted) {
            hasGameplayStarted = true
            resumeActiveTimer()
        }

        markHasPlayedBefore()

        // Always dismiss all path previews when user clicks an arrow to move it
        if (_uiState.value.previewRayArrowIds.isNotEmpty()) {
            _uiState.update { it.copy(previewRayArrowIds = emptySet()) }
        }

        // Check if Hammer booster mode is active
        if (_uiState.value.isHammerModeActive) {
            executeHammerStrike(currentArrow)
            return
        }

        // Automatically hide Grid Guide upon tapping an arrow
        if (_uiState.value.isGridLinesActive) {
            _uiState.update { it.copy(isGridLinesActive = false) }
        }

        audioHapticManager.playTap()

        levelMovesAttempted++
        // Re-fetch after potential WRONG_FLASH → IDLE snap to ensure correct state is used
        val freshArrow = _uiState.value.arrows.firstOrNull { it.id == currentArrow.id } ?: currentArrow
        val canEscape = PuzzleSolver.canMoveOut(freshArrow, occupancyMap)

        if (canEscape) {
            launchArrowFlight(freshArrow)
        } else {
            // WRONG MOVE: Collision
            levelMistakesCount++
            audioHapticManager.playWrongMove()

            val isAlreadyMarkedIncorrect = freshArrow.isHighlighted
            val newHearts = if (isAlreadyMarkedIncorrect) {
                // Do not deduct a life when user taps an arrow that has already been marked as incorrect
                _uiState.value.heartsRemaining
            } else {
                (_uiState.value.heartsRemaining - 1).coerceAtLeast(0)
            }

            val dist = PuzzleSolver.getDistanceToBlocker(freshArrow, occupancyMap)
            val durationMs = (280L + (dist * 35L).toLong()).coerceIn(320L, 560L)

            val isOver = newHearts <= 0
            if (isOver) {
                audioHapticManager.playLevelFail()
                pauseActiveTimer()
                val elapsedSec = getActivePlayTimeSeconds().toLong()
                val arrowsRemaining = _uiState.value.arrows.count { it.state != ArrowState.CLEARED }
                analytics.logLevelFailed(_uiState.value.currentLevelNumber, elapsedSec, arrowsRemaining, levelHintsUsedCount)
                CrashLogger.log("Level Failed: ${_uiState.value.currentLevelNumber}, remaining=$arrowsRemaining")
            }
            analytics.logArrowTapWrong(_uiState.value.currentLevelNumber, newHearts, freshArrow.id)

            _uiState.update { state ->
                val flashedArrows = state.arrows.map {
                    if (it.id == freshArrow.id) {
                        it.copyWithState(
                            newState = ArrowState.WRONG_FLASH,
                            progress = 0f,
                            highlighted = true,
                            collisionDist = dist
                        )
                    } else it
                }
                state.copy(
                    arrows = flashedArrows,
                    heartsRemaining = newHearts,
                    comboCount = 0,
                    cheerMessage = null,
                    isGameOver = isOver
                )
            }

            // Animate collision spring-bounce and reset to IDLE (keeping highlighted = true)
            val flashArrowId = freshArrow.id
            viewModelScope.launch {
                val startTime = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val rawProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                    _uiState.update { state ->
                        val updated = state.arrows.map {
                            if (it.id == flashArrowId && it.state == ArrowState.WRONG_FLASH) {
                                it.copy(flightProgress = rawProgress)
                            } else it
                        }
                        state.copy(arrows = updated)
                    }
                    if (rawProgress >= 1f) break
                    delay(16)
                }

                _uiState.update { state ->
                    val resetArrows = state.arrows.map {
                        if (it.id == flashArrowId && it.state == ArrowState.WRONG_FLASH) {
                            it.copyWithState(ArrowState.IDLE, 0f, highlighted = true)
                        } else it
                    }
                    state.copy(arrows = resetArrows)
                }
            }
        }
    }

    private fun launchArrowFlight(arrow: Arrow) {
        val newCombo = _uiState.value.comboCount + 1
        audioHapticManager.playArrowFly(newCombo)

        // Release grid occupancy immediately so following arrows can move
        occupancyMap.release(arrow)

        val totalArrows = _uiState.value.arrows.size
        val clearedCount = _uiState.value.arrows.count {
            it.state == ArrowState.CLEARED || it.state == ArrowState.FLYING_OUT
        } + 1

        val currentLvl = _uiState.value.currentLevelNumber
        val isSpecialMode = _uiState.value.isDailyChallengeMode

        val milestoneCheer = evaluateMilestone(clearedCount, totalArrows, currentLvl, isSpecialMode)
        // Combo praise appears with no "Combo x5" label, silenced around milestones to avoid overlap
        val comboCheer = if (milestoneCheer == null && (System.currentTimeMillis() - lastMilestoneTimestamp >= 2500L)) {
            calculateComboPraise(newCombo)
        } else null

        val cheer = milestoneCheer ?: comboCheer
        val isMilestone = (cheer != null && cheer == milestoneCheer)

        _uiState.update { state ->
            val updatedArrows = state.arrows.map {
                if (it.id == arrow.id) it.copyWithState(ArrowState.FLYING_OUT, highlighted = false) else it
            }
            state.copy(
                arrows = updatedArrows,
                comboCount = newCombo,
                cheerMessage = cheer ?: state.cheerMessage,
                cheerFromLeft = if (cheer != null) !state.cheerFromLeft else state.cheerFromLeft
            )
        }

        // Auto-dismiss: Snappy 1000ms for combo praise so it disappears quickly; 1800ms for milestones
        if (cheer != null) {
            val displayDurationMs = if (isMilestone) 1800L else 1000L
            cheerDismissJob?.cancel()
            cheerDismissJob = viewModelScope.launch {
                delay(displayDurationMs)
                _uiState.update { it.copy(cheerMessage = null) }
            }
        }

        // Animate flight at 60fps — 560ms InOutQuad: visible arc with responsive speed
        viewModelScope.launch {
            val durationMs = 560L
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val rawProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                // InOutQuad: smooth ramp-up, visible travel, smooth settle — arrow clearly flies
                val easedProgress = if (rawProgress < 0.5f) {
                    2f * rawProgress * rawProgress
                } else {
                    val t = rawProgress - 1f
                    1f - 2f * t * t
                }
                _uiState.update { state ->
                    val updated = state.arrows.map {
                        if (it.id == arrow.id && it.state == ArrowState.FLYING_OUT) {
                            it.copy(flightProgress = easedProgress)
                        } else it
                    }
                    state.copy(arrows = updated)
                }
                if (rawProgress >= 1f) break
                delay(16)
            }

            val finalArrows = _uiState.value.arrows.map {
                if (it.id == arrow.id) it.copyWithState(ArrowState.CLEARED, highlighted = false) else it
            }
            val allCleared = finalArrows.all { it.state == ArrowState.CLEARED }
            if (allCleared) {
                triggerLevelVictory(finalArrows)
            } else {
                _uiState.update { state ->
                    state.copy(arrows = finalArrows)
                }
                // Trigger cascading auto-release for any highlighted arrows that are now unblocked
                checkAndAutoReleaseHighlightedArrows()
            }
        }
    }

    private var autoReleaseJob: Job? = null

    /**
     * Simultaneous auto-release matching Unity behavior:
     * When blocking arrows exit, ALL currently highlighted+unblocked arrows release at the same
     * time — not one-by-one. This is more responsive, visually satisfying, and matches the
     * Unity reference exactly.
     *
     * A brief 80ms delay is used to let the triggering arrow's occupancy release propagate,
     * then all eligible arrows launch simultaneously.
     */
    private fun checkAndAutoReleaseHighlightedArrows() {
        autoReleaseJob?.cancel()
        autoReleaseJob = viewModelScope.launch {
            // Brief pause to let the triggering arrow's occupancy release be visible to canMoveOut
            delay(50)

            if (!isActive || _uiState.value.isLevelComplete || _uiState.value.isGameOver) return@launch

            // Find ALL highlighted arrows that are now free to move simultaneously
            val candidates = _uiState.value.arrows.filter {
                it.isHighlighted && it.state == ArrowState.IDLE && PuzzleSolver.canMoveOut(it, occupancyMap)
            }

            // Launch all eligible arrows simultaneously (Unity behavior)
            for (candidate in candidates) {
                launchArrowFlight(candidate)
            }

            // After this batch, check again for any newly unblocked arrows (cascading)
            // Wait for current flights to land before checking further
            if (candidates.isNotEmpty()) {
                delay(450) // Wait for 400ms flight + small buffer
                if (!isActive || _uiState.value.isLevelComplete || _uiState.value.isGameOver) return@launch
                // Recursively check for the next cascade wave
                val nextCandidates = _uiState.value.arrows.filter {
                    it.isHighlighted && it.state == ArrowState.IDLE && PuzzleSolver.canMoveOut(it, occupancyMap)
                }
                for (candidate in nextCandidates) {
                    launchArrowFlight(candidate)
                }
            }
        }
    }

    fun setTheme(theme: ThemePreset) {
        viewModelScope.launch {
            val index = ThemePresets.all.indexOfFirst { it.name == theme.name }.coerceAtLeast(0)
            repository.setTheme(index)
            _uiState.update { it.copy(activeTheme = theme) }
        }
    }

    fun onArrowLongPress(arrow: Arrow, isPressed: Boolean = true) {
        _uiState.update { state ->
            val current = state.previewRayArrowIds
            val updated = if (arrow.id in current) {
                current - arrow.id
            } else {
                current + arrow.id
            }
            state.copy(previewRayArrowIds = updated)
        }
    }

    fun dismissAllPreviewRays() {
        if (_uiState.value.previewRayArrowIds.isNotEmpty()) {
            _uiState.update { it.copy(previewRayArrowIds = emptySet()) }
        }
    }

    fun dismissPreviewRay() {
        dismissAllPreviewRays()
    }

    val isFreeHintLevel: Boolean get() = (_uiState.value.currentLevelNumber <= 10 && !_uiState.value.isDailyChallengeMode)

    fun setHintAdLoading(loading: Boolean) {
        _uiState.update { it.copy(isHintAdLoading = loading) }
    }

    fun showHintError(message: String) {
        _uiState.update { it.copy(isHintAdLoading = false, hintErrorMessage = message) }
    }

    fun clearHintErrorMessage() {
        _uiState.update { it.copy(hintErrorMessage = null) }
    }

    fun useHint() {
        viewModelScope.launch(Dispatchers.Default) {
            val hintTarget = PuzzleSolver.getBestHint(_uiState.value.arrows, occupancyMap) ?: return@launch
            levelHintsUsedCount++
            val isFree = isFreeHintLevel
            analytics.logHintButtonClicked(
                levelNumber = _uiState.value.currentLevelNumber,
                hintsBalance = _uiState.value.hintsAvailable,
                wasIdleHighlightActive = false,
                isFreeHint = isFree
            )
            val remainingArrows = _uiState.value.arrows.count { it.state != ArrowState.CLEARED }
            analytics.logHintApplied(_uiState.value.currentLevelNumber, hintTarget.id, remainingArrows)

            if (isFreeHintLevel) {
                // Unlimited free hints for starting levels (1-10) in campaign mode
                audioHapticManager.playBoosterUse("hint")
                _uiState.update { state ->
                    val updated = state.arrows.map {
                        if (it.id == hintTarget.id) it.copyWithState(ArrowState.HINTED) else it
                    }
                    state.copy(arrows = updated)
                }
            } else {
                // Level 11+ or Daily Challenge: deduct 1 hint from persistent inventory
                if (_uiState.value.hintsAvailable > 0) {
                    val used = repository.useBooster("hint")
                    if (used) {
                        audioHapticManager.playBoosterUse("hint")
                        _uiState.update { state ->
                            val updated = state.arrows.map {
                                if (it.id == hintTarget.id) it.copyWithState(ArrowState.HINTED) else it
                            }
                            state.copy(arrows = updated)
                        }
                    }
                }
            }
        }
    }

    fun dismissHintAdPrompt() {
        _uiState.update { it.copy(showHintAdPrompt = false) }
    }

    fun addHints(amount: Int = 1) {
        viewModelScope.launch {
            repository.addHints(amount)
            audioHapticManager.playBoosterUse("hint")
        }
    }

    fun claimAdHints(amount: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(showHintAdPrompt = false, isHintAdLoading = false) }
            // Auto-execute 1 hint on the board for instant relief
            val hintTarget = PuzzleSolver.getBestHint(_uiState.value.arrows, occupancyMap)
            if (hintTarget != null) {
                audioHapticManager.playBoosterUse("hint")
                _uiState.update { state ->
                    val updated = state.arrows.map {
                        if (it.id == hintTarget.id) it.copyWithState(ArrowState.HINTED) else it
                    }
                    state.copy(arrows = updated)
                }
                if (amount > 1) {
                    repository.addHints(amount - 1)
                }
            } else {
                repository.addHints(amount)
            }
        }
    }

    fun toggleHammerMode() {
        _uiState.update {
            it.copy(isHammerModeActive = !it.isHammerModeActive)
        }
    }

    private fun executeHammerStrike(target: Arrow) {
        viewModelScope.launch {
            if (repository.useBooster("hammer")) {
                audioHapticManager.playBoosterUse("hammer")
                _uiState.update { it.copy(isHammerModeActive = false) }

                // Animate hammer swing and shatter over 520ms
                val durationMs = 520L
                val startTime = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                    _uiState.update { state ->
                        state.copy(activeBoosterAnimation = BoosterEffectAnimation.Hammer(target, progress))
                    }
                    if (progress >= 1f) break
                    delay(16)
                }

                _uiState.update { it.copy(activeBoosterAnimation = null) }
                occupancyMap.release(target)

                val updated = _uiState.value.arrows.map {
                    if (it.id == target.id) it.copyWithState(ArrowState.CLEARED) else it
                }
                val allCleared = updated.all { it.state == ArrowState.CLEARED }

                if (allCleared) {
                    triggerLevelVictory(updated)
                } else {
                    _uiState.update { it.copy(arrows = updated) }
                    checkAndAutoReleaseHighlightedArrows()
                }
            }
        }
    }

    fun useMagicWand() {
        val target = PuzzleSolver.getMagicWandTarget(_uiState.value.arrows, occupancyMap) ?: return
        viewModelScope.launch {
            if (repository.useBooster("wand")) {
                audioHapticManager.playBoosterUse("wand")

                // Animate magical wand sweep & celestial dissolution over 640ms
                val durationMs = 640L
                val startTime = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                    _uiState.update { state ->
                        state.copy(activeBoosterAnimation = BoosterEffectAnimation.Wand(target, progress))
                    }
                    if (progress >= 1f) break
                    delay(16)
                }

                _uiState.update { it.copy(activeBoosterAnimation = null) }
                occupancyMap.release(target)

                val updated = _uiState.value.arrows.map {
                    if (it.id == target.id) it.copyWithState(ArrowState.CLEARED) else it
                }
                val allCleared = updated.all { it.state == ArrowState.CLEARED }
                if (allCleared) {
                    triggerLevelVictory(updated)
                } else {
                    _uiState.update { state ->
                        state.copy(arrows = updated)
                    }
                    checkAndAutoReleaseHighlightedArrows()
                }
            }
        }
    }

    fun toggleGridLines() {
        _uiState.update {
            val newState = !it.isGridLinesActive
            analytics.logGridButtonToggled(it.currentLevelNumber, if (newState) "enabled" else "disabled")
            it.copy(isGridLinesActive = newState)
        }
    }

    fun cycleTheme() {
        viewModelScope.launch {
            val nextIndex = (_uiState.value.activeTheme.name.let {
                ThemePresets.all.indexOfFirst { t -> t.name == it }
            } + 1) % ThemePresets.all.size
            repository.setTheme(nextIndex)
        }
    }

    fun nextLevel() {
        resetGridLines()
        if (_uiState.value.isDailyChallengeMode) {
            // When completing a daily or special level, reload current campaign level so player returns to campaign
            loadLevel(_uiState.value.currentLevelNumber)
        } else {
            val nextLvl = _uiState.value.currentLevelNumber + 1
            viewModelScope.launch {
                repository.updateLevel(nextLvl)
                loadLevel(nextLvl)
            }
        }
    }

    
    // ==========================================
    // DEVELOPER / TESTING AUTO-SOLVE CONTROLS
    // ==========================================
    private var autoSolveJob: Job? = null

    fun toggleAutoSolve() {
        if (_uiState.value.isAutoSolving) {
            stopAutoSolve()
        } else {
            startAutoSolve()
        }
    }

    fun stopAutoSolve() {
        autoSolveJob?.cancel()
        autoSolveJob = null
        if (_uiState.value.isAutoSolving) {
            _uiState.update { it.copy(isAutoSolving = false) }
        }
    }

    fun startAutoSolve() {
        if (_uiState.value.isLevelComplete || _uiState.value.isGameOver) return
        stopAutoSolve()
        _uiState.update { it.copy(isAutoSolving = true) }

        autoSolveJob = viewModelScope.launch {
            while (_uiState.value.isAutoSolving && !_uiState.value.isLevelComplete && !_uiState.value.isGameOver) {
                val currentArrows = _uiState.value.arrows
                val uncleared = currentArrows.filter { it.state != ArrowState.CLEARED && it.state != ArrowState.FLYING_OUT }
                if (uncleared.isEmpty()) break

                val movable = PuzzleSolver.findAllMovableArrows(currentArrows, occupancyMap)
                val nextArrow = PuzzleSolver.getBestHint(currentArrows, occupancyMap) ?: movable.firstOrNull()

                if (nextArrow != null) {
                    onArrowClicked(nextArrow)
                    delay(220) // Smooth interval between taps for responsive cascading flight
                } else {
                    delay(60)
                }
            }
            _uiState.update { it.copy(isAutoSolving = false) }
        }
    }

    fun onVictoryHomeClick() {
        stopAutoSolve()
        if (_uiState.value.isDailyChallengeMode) {
            loadLevel(_uiState.value.currentLevelNumber)
        } else {
            val nextLvl = _uiState.value.currentLevelNumber + 1
            viewModelScope.launch {
                repository.updateLevel(nextLvl)
                loadLevel(nextLvl)
            }
        }
    }

    fun hasPlayedBeforeSynchronous(): Boolean {
        return repository.hasPlayedBeforeSynchronous()
    }

    fun markHasPlayedBefore() {
        viewModelScope.launch {
            repository.markHasPlayedBefore()
        }
    }

    fun restartLevel(restartSource: String = "game_over") {
        if (_uiState.value.isGameOver) {
            analytics.logGameOverDecision(_uiState.value.currentLevelNumber, "restart", _uiState.value.isDailyChallengeMode)
        }
        celebrationJob?.cancel()
        analytics.logLevelRestart(_uiState.value.currentLevelNumber, restartSource)
        stopAutoSolve()
        resetGridLines()
        if (_uiState.value.isDailyChallengeMode) {
            val dateStr = _uiState.value.activeDailyDate ?: LocalDate.now().toString()
            startDailyChallenge(dateStr)
        } else {
            loadLevel(_uiState.value.currentLevelNumber)
        }
    }

    fun claimFreeLives(count: Int = 3) {
        val decision = if (_uiState.value.currentLevelNumber <= 10 && !_uiState.value.isDailyChallengeMode) "claim_free_lives" else "rewarded_ad"
        analytics.logGameOverDecision(_uiState.value.currentLevelNumber, decision, _uiState.value.isDailyChallengeMode)
        viewModelScope.launch {
            audioHapticManager.playClick()
            resumeActiveTimer()
            _uiState.update {
                it.copy(
                    heartsRemaining = count,
                    isGameOver = false
                )
            }
        }
    }

    fun playHintNudge() {
        audioHapticManager.playHintNudge()
    }

    private suspend fun triggerLevelVictory(finalArrows: List<Arrow>) {
        val completedLvl = _uiState.value.currentLevelNumber
        pauseActiveTimer()
        val elapsedSec = getActivePlayTimeSeconds()
        val totalMoves = levelMovesAttempted.coerceAtLeast(1)
        val mistakes = levelMistakesCount
        val accuracy = (((totalMoves - mistakes).toFloat() / totalMoves.toFloat()) * 100).toInt().coerceIn(10, 100)

        val stars = when {
            _uiState.value.isDailyChallengeMode -> 3
            _uiState.value.heartsRemaining >= _uiState.value.maxHearts -> 3
            _uiState.value.heartsRemaining >= _uiState.value.maxHearts - 1 -> 2
            else -> 1
        }

        // Option A: 100-Point Skill Mastery Score (0 - 100 Pts)
        val baseClear = 50
        val healthPoints = (_uiState.value.heartsRemaining.coerceIn(0, 3) * 10)
        val hintPoints = when (levelHintsUsedCount) {
            0 -> 10
            1 -> 5
            else -> 0
        }
        val totalArrows = finalArrows.size
        val targetPaceSeconds = maxOf(25, totalArrows * 3)
        val timeEfficiency = ((targetPaceSeconds - elapsedSec).toFloat() / targetPaceSeconds.toFloat()).coerceIn(0f, 1f)
        val speedPoints = kotlin.math.round(timeEfficiency * 10f).toInt().coerceIn(0, 10)

        val finalScore = (baseClear + healthPoints + hintPoints + speedPoints).coerceIn(50, 100)

        levelsCompletedInSession++
        val gameMode = if (_uiState.value.isDailyChallengeMode) "daily" else "campaign"
        analytics.logLevelComplete(
            levelNumber = completedLvl,
            gameMode = gameMode,
            activePlayTimeSec = elapsedSec.toLong(),
            score = finalScore,
            heartsRemaining = _uiState.value.heartsRemaining,
            mistakesCount = mistakes,
            hintsUsed = levelHintsUsedCount,
            starsEarned = stars
        )
        CrashLogger.log("Level Complete: $completedLvl, mode=$gameMode, score=$finalScore, stars=$stars")

        val lastPrompt = repository.getLastReviewPromptTimestampSynchronous()
        val cooldown = RemoteConfigManager.reviewPromptCooldownDays
        val minLevel = RemoteConfigManager.reviewPromptMinLevel
        val totalDailies = if (_uiState.value.isDailyChallengeMode) {
            val dateStr = _uiState.value.activeDailyDate ?: LocalDate.now().toString()
            val set = _uiState.value.completedDailyDates.toMutableSet().apply { add(dateStr) }
            set.size
        } else {
            _uiState.value.completedDailyDates.size
        }

        val reviewEligibility = InAppReviewManager.checkReviewEligibility(
            currentLevel = completedLvl,
            starsEarned = stars,
            scoreEarned = finalScore,
            currentStreakDays = _uiState.value.streakDays,
            lastPromptTimestamp = lastPrompt,
            cooldownDays = cooldown,
            minEligibleLevel = minLevel,
            isDailyMode = _uiState.value.isDailyChallengeMode,
            completedDailiesCount = totalDailies
        )

        if (reviewEligibility.shouldPrompt) {
            analytics.logInAppReviewPrompted(
                levelNumber = completedLvl,
                triggerReason = reviewEligibility.triggerReason,
                scoreEarned = finalScore,
                starsEarned = stars,
                streakDays = _uiState.value.streakDays
            )
        }

        if (!_uiState.value.isDailyChallengeMode) {
            analytics.setUserPropertyMaxLevel(completedLvl)
        }

        _uiState.update { state ->
            state.copy(
                arrows = finalArrows,
                isCelebrationBurstActive = true,
                lastCompletedLevelNumber = completedLvl,
                levelCompletionTimeSeconds = elapsedSec,
                levelMovesCount = totalMoves,
                levelFinalScore = finalScore,
                levelAccuracyPercent = accuracy,
                levelMistakesCount = mistakes,
                levelHintsUsedCount = levelHintsUsedCount,
                levelStarsEarned = stars,
                reviewPromptTrigger = if (reviewEligibility.shouldPrompt) System.currentTimeMillis() else 0L
            )
        }
        audioHapticManager.playLevelWin()
        val streakResult = repository.recordStreakOnLevelCompletion()
        celebrationJob?.cancel()
        celebrationJob = viewModelScope.launch {
            delay(1800)
            completeVictoryTransition(completedLvl, streakResult)
        }
    }

    private fun completeVictoryTransition(completedLvl: Int, streakResult: Pair<Boolean, Int>) {
        _uiState.update { state ->
            if (state.isLevelComplete) return@update state
            state.copy(
                isLevelComplete = true,
                isCelebrationBurstActive = false,
                isStreakEarnedThisLevel = streakResult.first,
                streakDays = streakResult.second
            )
        }
        handleLevelComplete(completedLvl)
    }

    fun skipCelebrationToVictory() {
        if (_uiState.value.isCelebrationBurstActive && !_uiState.value.isLevelComplete) {
            val job = celebrationJob
            celebrationJob = null
            job?.cancel()
            val completedLvl = _uiState.value.lastCompletedLevelNumber.takeIf { it > 0 } ?: _uiState.value.currentLevelNumber
            val streakResult = Pair(_uiState.value.isStreakEarnedThisLevel, _uiState.value.streakDays)
            completeVictoryTransition(completedLvl, streakResult)
        }
    }

    fun claimDoubleCoins(amount: Int) {
        viewModelScope.launch {
            repository.addCoins(amount)
            audioHapticManager.playCoinCollect()
        }
    }

    fun toggleSound() {
        viewModelScope.launch {
            repository.toggleSound()
        }
    }

    fun toggleVibration() {
        viewModelScope.launch {
            repository.toggleVibration()
        }
    }

    fun playClick() {
        audioHapticManager.playClick()
    }

    fun playTap() {
        audioHapticManager.playTap()
    }

    fun playCoinCollect() {
        audioHapticManager.playCoinCollect()
    }

    fun playStarDrop(starIndex: Int) {
        audioHapticManager.playStarDrop(starIndex)
    }

    fun resetGameData() {
        audioHapticManager.playClick()
        viewModelScope.launch {
            repository.resetGameProgress()
            _uiState.update { current ->
                current.copy(
                    currentLevelNumber = 1,
                    lastCompletedLevelNumber = 0,
                    coins = 100,
                    streakDays = 0,
                    isStreakEarnedThisLevel = false,
                    hintsAvailable = 0,
                    hammersAvailable = 2,
                    wandsAvailable = 2,
                    completedDailyDates = emptySet(),
                    levelStars = emptyMap(),
                    hasPlayedBefore = false,
                    globalLivesAvailable = 6,
                    hasSeenDailyUnlockCelebration = false,
                    isLevelComplete = false,
                    milestoneRewardHints = 0,
                    isGameOver = false,
                    comboCount = 0
                )
            }
            loadLevel(1)
        }
    }

    fun selectLevel(levelNumber: Int) {
        val maxUnlocked = maxOf(_uiState.value.currentLevelNumber, _uiState.value.lastCompletedLevelNumber)
        if (levelNumber in 1..maxUnlocked) {
            loadLevel(levelNumber)
        }
    }

    private fun handleLevelComplete(completedLevel: Int) {
        val stars = when {
            _uiState.value.isDailyChallengeMode -> 3
            _uiState.value.heartsRemaining >= _uiState.value.maxHearts -> 3
            _uiState.value.heartsRemaining >= _uiState.value.maxHearts - 1 -> 2
            else -> 1
        }
        viewModelScope.launch {
            if (_uiState.value.isDailyChallengeMode) {
                val dateStr = _uiState.value.activeDailyDate ?: LocalDate.now().toString()
                val isFirstCompletion = repository.recordDailyCompletion(dateStr)
                val dailyRewardCoins = if (isFirstCompletion) RemoteConfigManager.dailyCompletionCoins else 0
                if (dailyRewardCoins > 0) {
                    repository.addCoins(dailyRewardCoins)
                }
                analytics.logDailyChallengeComplete(
                    puzzleDate = dateStr,
                    completionTimeSec = _uiState.value.levelCompletionTimeSeconds.toLong(),
                    newStreakDays = _uiState.value.streakDays,
                    rewardCoinsClaimed = dailyRewardCoins,
                    dailyPool = _uiState.value.activeDailyPool
                )
            } else {
                repository.recordLevelCompletion(completedLevel, stars)
                val baseCoins = 50 + (_uiState.value.comboCount * 5)
                repository.addCoins(baseCoins)
                if (LevelDifficultyRules.isMilestoneRewardLevel(completedLevel, isDailyChallenge = false)) {
                    val rewardHints = LevelDifficultyRules.MILESTONE_REWARD_HINTS
                    if (repository.claimMilestoneReward(completedLevel, rewardHints)) {
                        _uiState.update { it.copy(milestoneRewardHints = rewardHints) }
                    }
                }
            }
        }
    }

    private fun evaluateMilestone(
        clearedCount: Int,
        totalArrows: Int,
        levelNumber: Int,
        isSpecialMode: Boolean
    ): String? {
        // Do not fire if puzzle is complete or board has too few arrows
        if (clearedCount >= totalArrows || totalArrows < 6) return null

        // Skip initial tutorial levels where only a handful of arrows are required (Levels 1 & 2)
        if (!isSpecialMode && levelNumber in 1..2) return null

        val now = System.currentTimeMillis()
        // Prevent any overlapping notifications for a few moments before and after (3.5s cooldown buffer)
        if (now - lastMilestoneTimestamp < 3500L) {
            return null
        }

        val threshold90 = ceil(totalArrows * 0.90f).toInt()
        val threshold70 = ceil(totalArrows * 0.70f).toInt()
        val threshold50 = ceil(totalArrows * 0.50f).toInt()
        val threshold25 = ceil(totalArrows * 0.25f).toInt()

        if (!milestone90Fired && clearedCount >= threshold90) {
            milestone25Fired = true
            milestone50Fired = true
            milestone70Fired = true
            milestone90Fired = true
            lastMilestoneTimestamp = now
            return "90% · Puzzle Complete"
        }

        if (!milestone70Fired && clearedCount >= threshold70) {
            milestone25Fired = true
            milestone50Fired = true
            milestone70Fired = true
            lastMilestoneTimestamp = now
            return "70% · Puzzle Complete"
        }

        if (!milestone50Fired && clearedCount >= threshold50) {
            milestone25Fired = true
            milestone50Fired = true
            lastMilestoneTimestamp = now
            return "50% · Puzzle Complete"
        }

        if (!milestone25Fired && clearedCount >= threshold25) {
            milestone25Fired = true
            lastMilestoneTimestamp = now
            return "25% · Puzzle Complete"
        }

        return null
    }

    private fun calculateComboPraise(combo: Int): String? {
        return when (combo) {
            5 -> listOf("On Fire!", "In the Flow!", "Pure Rhythm!", "Grooving!").random()
            10 -> listOf("Arrow Maestro!", "Laser Focus!", "Brilliant!", "Spectacular!").random()
            15 -> listOf("Galaxy Brain!", "Flawless!", "Untouchable!", "Masterful!").random()
            20 -> listOf("Living Legend!", "Perfection!", "Supreme Flow!").random()
            else -> null
        }
    }
}
