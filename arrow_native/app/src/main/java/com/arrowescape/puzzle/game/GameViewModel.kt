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
import com.arrowescape.puzzle.ui.theme.ThemePreset
import com.arrowescape.puzzle.ui.theme.ThemePresets
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val hammersAvailable: Int = 2,
    val wandsAvailable: Int = 2,
    val isDailyChallengeMode: Boolean = false,
    val activeDailyDate: String? = null,
    val activeDailyIndex: Int = 1,
    val completedDailyDates: Set<String> = emptySet(),
    val levelStars: Map<Int, Int> = emptyMap(),
    val hasPlayedBefore: Boolean = false,
    val globalLivesAvailable: Int = 6,
    val activeBoosterAnimation: BoosterEffectAnimation? = null,
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

    init {
        viewModelScope.launch {
            repository.userGameData.collect { data ->
                val theme = ThemePresets.all.getOrElse(data.themeIndex) { ThemePresets.CleanLight }
                audioHapticManager.isSoundEnabled = data.soundEnabled
                audioHapticManager.isVibrationEnabled = data.vibrationEnabled

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
                        activeTheme = theme
                    )
                }

                // If not loaded yet, load current level
                if (_uiState.value.levelData == null) {
                    loadLevel(data.currentLevel)
                }
            }
        }
    }

    fun loadLevel(levelNumber: Int) {
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
                activeDailyDate = null,
                activeDailyIndex = 1,
                restartTrigger = System.currentTimeMillis()
            )
        }
    }

    fun startDailyChallenge(dateString: String = LocalDate.now().toString()) {
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
                activeDailyDate = date.toString(),
                activeDailyIndex = dailyIndex,
                restartTrigger = System.currentTimeMillis()
            )
        }
    }

    fun onArrowClicked(clickedArrow: Arrow) {
        if (_uiState.value.isLevelComplete || _uiState.value.isGameOver) return
        val currentArrow = _uiState.value.arrows.firstOrNull { it.id == clickedArrow.id } ?: clickedArrow
        if (currentArrow.state == ArrowState.CLEARED || currentArrow.state == ArrowState.FLYING_OUT) return

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

        val canEscape = PuzzleSolver.canMoveOut(currentArrow, occupancyMap)

        if (canEscape) {
            launchArrowFlight(currentArrow)
        } else {
            // WRONG MOVE: Collision
            audioHapticManager.playWrongMove()

            val isAlreadyHighlighted = currentArrow.isHighlighted
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

            val dist = PuzzleSolver.getDistanceToBlocker(currentArrow, occupancyMap)
            val durationMs = (300L + (dist * 40L).toLong()).coerceIn(360L, 640L)

            _uiState.update { state ->
                val flashedArrows = state.arrows.map {
                    if (it.id == currentArrow.id) {
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
            viewModelScope.launch {
                val startTime = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val rawProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                    _uiState.update { state ->
                        val updated = state.arrows.map {
                            if (it.id == currentArrow.id && it.state == ArrowState.WRONG_FLASH) {
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
                        if (it.id == currentArrow.id && it.state == ArrowState.WRONG_FLASH) {
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

        // Animate flight smoothly at 60fps and transition to CLEARED
        viewModelScope.launch {
            val durationMs = 820L
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val rawProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                // Smooth continuous unified flight easing (whole arrow moves together smoothly)
                val easedProgress = rawProgress * rawProgress * (3f - 2f * rawProgress)
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
                // Check once the active arrow has COMPLETELY cleared the board (strictly sequential exits)
                checkAndAutoReleaseHighlightedArrows()
            }
        }
    }

    private fun checkAndAutoReleaseHighlightedArrows() {
        viewModelScope.launch {
            // Guard: wait if any arrow is currently in flight to ensure strictly sequential exits
            if (_uiState.value.arrows.any { it.state == ArrowState.FLYING_OUT }) {
                return@launch
            }
            delay(120) // Brief natural pause between sequential exits matching Unity
            if (_uiState.value.arrows.any { it.state == ArrowState.FLYING_OUT }) {
                return@launch
            }
            val highlightedCandidates = _uiState.value.arrows.filter {
                it.isHighlighted && it.state == ArrowState.IDLE
            }
            for (candidate in highlightedCandidates) {
                if (PuzzleSolver.canMoveOut(candidate, occupancyMap)) {
                    launchArrowFlight(candidate)
                    break
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

    fun useHint() {
        val hintTarget = PuzzleSolver.getBestHint(_uiState.value.arrows, occupancyMap) ?: return
        viewModelScope.launch {
            if (repository.useBooster("hint")) {
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
        if (_uiState.value.isDailyChallengeMode) {
            // When completing a daily level, reload current campaign level so player returns to campaign
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

    fun restartLevel() {
        if (_uiState.value.isDailyChallengeMode) {
            val dateStr = _uiState.value.activeDailyDate ?: LocalDate.now().toString()
            startDailyChallenge(dateStr)
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
        _uiState.update { state ->
            state.copy(
                arrows = finalArrows,
                isCelebrationBurstActive = true,
                lastCompletedLevelNumber = completedLvl
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
            val baseCoins = if (_uiState.value.isDailyChallengeMode) 100 else 50 + (_uiState.value.comboCount * 5)
            if (_uiState.value.isDailyChallengeMode) {
                val dateStr = _uiState.value.activeDailyDate ?: LocalDate.now().toString()
                repository.recordDailyCompletion(dateStr)
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
