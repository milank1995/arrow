package com.arrowescape.puzzle.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arrowescape.puzzle.audio.AudioHapticManager
import com.arrowescape.puzzle.core.engine.GridOccupancyMap
import com.arrowescape.puzzle.core.engine.LevelGenerator
import com.arrowescape.puzzle.core.engine.LevelLoader
import com.arrowescape.puzzle.core.engine.PuzzleSolver
import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.ArrowState
import com.arrowescape.puzzle.core.model.LevelData
import com.arrowescape.puzzle.data.UserPreferencesRepository
import com.arrowescape.puzzle.notification.NotificationHelper
import com.arrowescape.puzzle.ui.theme.ThemePreset
import com.arrowescape.puzzle.ui.theme.ThemePresets
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
    val activeTheme: ThemePreset = ThemePresets.CleanLight,
    val currentLevelNumber: Int = 1,
    val lastCompletedLevelNumber: Int = 1,
    val coins: Int = 100,
    val streakDays: Int = 1,
    val hintsAvailable: Int = 3,
    val dailyComplimentaryHintsRemaining: Int = 1,
    val hammersAvailable: Int = 2,
    val wandsAvailable: Int = 2,
    val isDailyChallengeMode: Boolean = false,
    val isLockPuzzleMode: Boolean = false,
    val activeDailyDate: String? = null,
    val activeDailyIndex: Int = 1,
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
    val levelStarsEarned: Int = 3,
    val hasSeenDailyUnlockCelebration: Boolean = false,
    val dailyRemindersEnabled: Boolean = true,
    val hasRequestedNotificationPermission: Boolean = false,
    val showDailyLaunchPrompt: Boolean = false,
    val restartTrigger: Long = 0L
) {
    val previewRayArrowId: String? get() = previewRayArrowIds.firstOrNull()
}

class GameViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repository = UserPreferencesRepository(app)
    val audioHapticManager = AudioHapticManager(app)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var occupancyMap = GridOccupancyMap(4, 4)
    private var cheerDismissJob: Job? = null
    private var levelStartTime: Long = 0L
    private var hasGameplayStarted: Boolean = false
    private var levelMovesAttempted: Int = 0
    private var levelMistakesCount: Int = 0
    private var hasDismissedDailyLaunchPromptThisSession: Boolean = false

    init {
        viewModelScope.launch {
            repository.userGameData.collect { data ->
                val theme = ThemePresets.all.getOrElse(data.themeIndex) { ThemePresets.CleanLight }
                audioHapticManager.isSoundEnabled = data.soundEnabled
                audioHapticManager.isVibrationEnabled = data.vibrationEnabled

                val todayStr = LocalDate.now().toString()
                val isDailyCompletedToday = data.completedDailyDates.contains(todayStr)
                val isDailyUnlocked = data.currentLevel > 5 || data.hasSeenDailyUnlockCelebration
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
                        lastCompletedLevelNumber = if (current.levelData == null) maxOf(1, data.currentLevel - 1) else current.lastCompletedLevelNumber,
                        isSoundEnabled = data.soundEnabled,
                        isVibrationEnabled = data.vibrationEnabled,
                        activeTheme = theme,
                        hasSeenDailyUnlockCelebration = data.hasSeenDailyUnlockCelebration,
                        dailyRemindersEnabled = data.dailyRemindersEnabled,
                        hasRequestedNotificationPermission = data.hasRequestedNotificationPermission,
                        showDailyLaunchPrompt = shouldShowDailyPrompt
                    )
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

    fun markDailyUnlockCelebrationSeen() {
        viewModelScope.launch {
            repository.markDailyUnlockCelebrationSeen()
            _uiState.update { it.copy(hasSeenDailyUnlockCelebration = true) }
        }
    }

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
        viewModelScope.launch {
            val nextState = !_uiState.value.dailyRemindersEnabled
            repository.setDailyRemindersEnabled(nextState)
            _uiState.update { it.copy(dailyRemindersEnabled = nextState) }
            if (nextState) {
                NotificationHelper.scheduleDailyReminderAlarm(app)
            } else {
                NotificationHelper.cancelDailyReminderAlarm(app)
            }
        }
    }

    fun onGameplayStarted() {
        levelStartTime = System.currentTimeMillis()
        hasGameplayStarted = true
        levelMovesAttempted = 0
        levelMistakesCount = 0
    }

    fun loadLevel(levelNumber: Int) {
        levelStartTime = 0L
        hasGameplayStarted = false
        levelMovesAttempted = 0
        levelMistakesCount = 0
        val level = LevelLoader.loadLevel(app, levelNumber)
        occupancyMap = GridOccupancyMap(level.xSize, level.ySize).apply {
            rebuild(level.arrows)
        }

        _uiState.update {
            it.copy(
                levelData = level,
                arrows = level.arrows,
                heartsRemaining = level.maxHearts,
                maxHearts = level.maxHearts,
                currentLevelNumber = levelNumber,
                isLevelComplete = false,
                isGameOver = false,
                comboCount = 0,
                cheerMessage = null,
                isHammerModeActive = false,
                previewRayArrowIds = emptySet(),
                isDailyChallengeMode = false,
                isLockPuzzleMode = false,
                activeDailyDate = null,
                activeDailyIndex = 1,
                dailyComplimentaryHintsRemaining = 1,
                showHintAdPrompt = false,
                restartTrigger = System.currentTimeMillis()
            )
        }
    }

    fun startLockPuzzle() {
        levelStartTime = 0L
        hasGameplayStarted = false
        levelMovesAttempted = 0
        levelMistakesCount = 0
        val level = LevelLoader.loadLockPuzzle(app)
        occupancyMap = GridOccupancyMap(level.xSize, level.ySize).apply {
            rebuild(level.arrows)
        }

        _uiState.update {
            it.copy(
                levelData = level,
                arrows = level.arrows,
                heartsRemaining = level.maxHearts,
                maxHearts = level.maxHearts,
                isLevelComplete = false,
                isGameOver = false,
                comboCount = 0,
                cheerMessage = null,
                isHammerModeActive = false,
                previewRayArrowIds = emptySet(),
                isDailyChallengeMode = false,
                isLockPuzzleMode = true,
                activeDailyDate = null,
                activeDailyIndex = 1,
                dailyComplimentaryHintsRemaining = 1,
                showHintAdPrompt = false,
                restartTrigger = System.currentTimeMillis()
            )
        }
    }

    fun startDailyChallenge(dateString: String = LocalDate.now().toString()) {
        levelStartTime = 0L
        hasGameplayStarted = false
        levelMovesAttempted = 0
        levelMistakesCount = 0
        val date = try {
            LocalDate.parse(dateString)
        } catch (e: Exception) {
            LocalDate.now()
        }
        val dayOfYear = date.dayOfYear
        val dailyIndex = ((dayOfYear - 1) % 99) + 1
        val level = LevelLoader.loadDailyLevel(app, dailyIndex)
        occupancyMap = GridOccupancyMap(level.xSize, level.ySize).apply {
            rebuild(level.arrows)
        }

        _uiState.update {
            it.copy(
                levelData = level,
                arrows = level.arrows,
                heartsRemaining = level.maxHearts,
                maxHearts = level.maxHearts,
                isLevelComplete = false,
                isGameOver = false,
                comboCount = 0,
                cheerMessage = null,
                isHammerModeActive = false,
                previewRayArrowIds = emptySet(),
                isDailyChallengeMode = true,
                isLockPuzzleMode = false,
                activeDailyDate = date.toString(),
                activeDailyIndex = dailyIndex,
                dailyComplimentaryHintsRemaining = 1,
                restartTrigger = System.currentTimeMillis()
            )
        }
    }

    fun onArrowClicked(clickedArrow: Arrow) {
        if (_uiState.value.isLevelComplete || _uiState.value.isGameOver) return
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

        if (!hasGameplayStarted || levelStartTime == 0L) {
            levelStartTime = System.currentTimeMillis()
            hasGameplayStarted = true
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

            val isAlreadyHighlighted = freshArrow.isHighlighted
            val newHearts = if (isAlreadyHighlighted) {
                // If user clicks on the same highlighted, blocked arrow again, deduct only one life for that interaction
                _uiState.value.heartsRemaining
            } else {
                (_uiState.value.heartsRemaining - 1).coerceAtLeast(0)
            }
            val isOver = newHearts <= 0

            if (isOver) {
                audioHapticManager.playLevelFail()
            }

            val dist = PuzzleSolver.getDistanceToBlocker(freshArrow, occupancyMap)
            val durationMs = (280L + (dist * 35L).toLong()).coerceIn(320L, 560L)

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
        val cheer = calculateCheerMessage(newCombo)

        // Release grid occupancy immediately so following arrows can move
        occupancyMap.release(arrow)

        _uiState.update { state ->
            val updatedArrows = state.arrows.map {
                if (it.id == arrow.id) it.copyWithState(ArrowState.FLYING_OUT, highlighted = false) else it
            }
            state.copy(
                arrows = updatedArrows,
                comboCount = newCombo,
                cheerMessage = cheer,
                cheerFromLeft = if (cheer != null) !state.cheerFromLeft else state.cheerFromLeft
            )
        }

        // Auto-dismiss in-app cheer notification after 1.8 seconds
        cheerDismissJob?.cancel()
        if (cheer != null) {
            cheerDismissJob = viewModelScope.launch {
                delay(1800)
                _uiState.update { it.copy(cheerMessage = null) }
            }
        }

        // Animate flight smoothly at 60fps matching Unity (smooth 780ms duration) and transition to CLEARED
        viewModelScope.launch {
            val durationMs = 780L
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val rawProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                // Smooth continuous unified flight easing with natural acceleration matching Unity
                val easedProgress = rawProgress * rawProgress * (0.22f + 0.78f * rawProgress)
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
            delay(80)

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

    fun useHint() {
        viewModelScope.launch(Dispatchers.Default) {
            val hintTarget = PuzzleSolver.getBestHint(_uiState.value.arrows, occupancyMap) ?: return@launch
            if (isFreeHintLevel) {
                // Unlimited free hints for starting levels (1-10) in campaign mode
                audioHapticManager.playBoosterUse("hint")
                _uiState.update { state ->
                    val updated = state.arrows.map {
                        if (it.id == hintTarget.id) it.copyWithState(ArrowState.HINTED) else it
                    }
                    state.copy(arrows = updated)
                }
            } else if (_uiState.value.isDailyChallengeMode) {
                if (_uiState.value.dailyComplimentaryHintsRemaining > 0) {
                    // Strictly 1 hint in Daily Challenge mode
                    audioHapticManager.playBoosterUse("hint")
                    _uiState.update { state ->
                        val updated = state.arrows.map {
                            if (it.id == hintTarget.id) it.copyWithState(ArrowState.HINTED) else it
                        }
                        state.copy(
                            arrows = updated,
                            dailyComplimentaryHintsRemaining = 0
                        )
                    }
                } else {
                    // Out of hints in Daily Challenge -> prompt rewarded ad dialog
                    _uiState.update { it.copy(showHintAdPrompt = true) }
                }
            } else {
                if (_uiState.value.hintsAvailable > 0) {
                    if (repository.useBooster("hint")) {
                        audioHapticManager.playBoosterUse("hint")
                        _uiState.update { state ->
                            val updated = state.arrows.map {
                                if (it.id == hintTarget.id) it.copyWithState(ArrowState.HINTED) else it
                            }
                            state.copy(
                                arrows = updated,
                                hintsAvailable = maxOf(0, state.hintsAvailable - 1)
                            )
                        }
                    }
                } else {
                    // Out of hints on Level 11+ or daily -> prompt rewarded ad dialog
                    _uiState.update { it.copy(showHintAdPrompt = true) }
                }
            }
        }
    }

    fun dismissHintAdPrompt() {
        _uiState.update { it.copy(showHintAdPrompt = false) }
    }

    fun claimAdHints(amount: Int = 3) {
        viewModelScope.launch {
            if (_uiState.value.isDailyChallengeMode) {
                _uiState.update { it.copy(dailyComplimentaryHintsRemaining = 1, showHintAdPrompt = false) }
                val hintTarget = PuzzleSolver.getBestHint(_uiState.value.arrows, occupancyMap)
                if (hintTarget != null) {
                    audioHapticManager.playBoosterUse("hint")
                    _uiState.update { state ->
                        val updated = state.arrows.map {
                            if (it.id == hintTarget.id) it.copyWithState(ArrowState.HINTED) else it
                        }
                        state.copy(arrows = updated, dailyComplimentaryHintsRemaining = 0)
                    }
                }
            } else {
                repository.addHints(amount)
                _uiState.update { it.copy(showHintAdPrompt = false) }

                // Immediately auto-execute 1 hint on the board for instant relief
                val hintTarget = PuzzleSolver.getBestHint(_uiState.value.arrows, occupancyMap)
                if (hintTarget != null) {
                    if (repository.useBooster("hint")) {
                        audioHapticManager.playBoosterUse("hint")
                        _uiState.update { state ->
                            val updated = state.arrows.map {
                                if (it.id == hintTarget.id) it.copyWithState(ArrowState.HINTED) else it
                            }
                            state.copy(
                                arrows = updated,
                                hintsAvailable = maxOf(0, state.hintsAvailable - 1)
                            )
                        }
                    }
                }
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
            it.copy(isGridLinesActive = !it.isGridLinesActive)
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
        if (_uiState.value.isDailyChallengeMode || _uiState.value.isLockPuzzleMode) {
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

    fun onVictoryHomeClick() {
        if (_uiState.value.isDailyChallengeMode || _uiState.value.isLockPuzzleMode) {
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

    fun restartLevel() {
        if (_uiState.value.isDailyChallengeMode) {
            val dateStr = _uiState.value.activeDailyDate ?: LocalDate.now().toString()
            startDailyChallenge(dateStr)
        } else if (_uiState.value.isLockPuzzleMode) {
            startLockPuzzle()
        } else {
            loadLevel(_uiState.value.currentLevelNumber)
        }
    }

    fun claimFreeLives(count: Int = 3) {
        viewModelScope.launch {
            repository.useGlobalLives(count)
            audioHapticManager.playClick()
            val restoredHearts = minOf(_uiState.value.maxHearts, count).coerceAtLeast(1)
            _uiState.update {
                it.copy(
                    heartsRemaining = restoredHearts,
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
        val now = System.currentTimeMillis()
        val actualStartTime = if (levelStartTime > 0L) levelStartTime else now - 4000L
        val elapsedSec = ((now - actualStartTime) / 1000L).toInt().coerceAtLeast(1)
        val totalMoves = levelMovesAttempted.coerceAtLeast(1)
        val mistakes = levelMistakesCount
        val accuracy = (((totalMoves - mistakes).toFloat() / totalMoves.toFloat()) * 100).toInt().coerceIn(10, 100)

        val stars = when {
            _uiState.value.isDailyChallengeMode -> 3
            _uiState.value.heartsRemaining >= _uiState.value.maxHearts -> 3
            _uiState.value.heartsRemaining >= _uiState.value.maxHearts - 1 -> 2
            else -> 1
        }

        // Scale-proportional score rewarding board size, pace, health, and streak
        val totalArrows = finalArrows.size
        val arrowPoints = totalArrows * 50
        val targetPaceSeconds = maxOf(30, totalArrows * 4)
        val timeBonus = ((targetPaceSeconds - elapsedSec).coerceAtLeast(0) * 5)
        val healthBonus = _uiState.value.heartsRemaining * 250
        val streakBonus = _uiState.value.streakDays * 100
        val modeClearBonus = if (_uiState.value.isDailyChallengeMode) 2000 else (500 + completedLvl * 25)
        val finalScore = modeClearBonus + arrowPoints + timeBonus + healthBonus + streakBonus

        _uiState.update { state ->
            state.copy(
                arrows = finalArrows,
                isCelebrationBurstActive = true,
                lastCompletedLevelNumber = completedLvl,
                levelCompletionTimeSeconds = elapsedSec,
                levelMovesCount = totalMoves,
                levelFinalScore = finalScore,
                levelAccuracyPercent = accuracy,
                levelStarsEarned = stars
            )
        }
        audioHapticManager.playLevelWin()
        delay(950)
        _uiState.update { state ->
            state.copy(isLevelComplete = true, isCelebrationBurstActive = false)
        }
        handleLevelComplete(completedLvl)
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
            val baseCoins = if (_uiState.value.isDailyChallengeMode || _uiState.value.isLockPuzzleMode) 100 else 50 + (_uiState.value.comboCount * 5)
            if (_uiState.value.isDailyChallengeMode) {
                val dateStr = _uiState.value.activeDailyDate ?: LocalDate.now().toString()
                repository.recordDailyCompletion(dateStr)
            } else if (_uiState.value.isLockPuzzleMode) {
                repository.recordLevelCompletion(151, stars)
            } else {
                repository.recordLevelCompletion(completedLevel, stars)
            }
            repository.addCoins(baseCoins)
        }
    }

    private fun calculateCheerMessage(combo: Int): String? {
        return when (combo) {
            3 -> "Nice!"
            5 -> "Great!"
            8 -> "Awesome!"
            12 -> "Unstoppable!"
            16 -> "Masterful!"
            else -> null
        }
    }
}
