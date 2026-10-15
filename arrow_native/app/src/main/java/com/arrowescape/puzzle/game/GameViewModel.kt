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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val previewRayArrowId: String? = null,
    val activeTheme: ThemePreset = ThemePresets.CleanLight,
    val currentLevelNumber: Int = 1,
    val coins: Int = 100,
    val streakDays: Int = 1,
    val hintsAvailable: Int = 3,
    val hammersAvailable: Int = 2,
    val wandsAvailable: Int = 2
)

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
                        currentLevelNumber = data.currentLevel,
                        coins = data.coins,
                        streakDays = data.streakDays,
                        hintsAvailable = data.freeHints,
                        hammersAvailable = data.freeHammers,
                        wandsAvailable = data.freeWands,
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
                isLevelComplete = false,
                isGameOver = false,
                comboCount = 0,
                cheerMessage = null,
                isHammerModeActive = false,
                previewRayArrowId = null
            )
        }
    }

    fun loadDailyLevel(dailyIndex: Int = 1) {
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
                previewRayArrowId = null
            )
        }
    }

    fun onArrowClicked(clickedArrow: Arrow) {
        if (_uiState.value.isLevelComplete || _uiState.value.isGameOver) return
        val currentArrow = _uiState.value.arrows.firstOrNull { it.id == clickedArrow.id } ?: clickedArrow
        if (currentArrow.state == ArrowState.CLEARED || currentArrow.state == ArrowState.FLYING_OUT) return

        // Check if Hammer booster mode is active
        if (_uiState.value.isHammerModeActive) {
            executeHammerStrike(currentArrow)
            return
        }

        audioHapticManager.playTap()

        val canEscape = PuzzleSolver.canMoveOut(currentArrow, occupancyMap)

        if (canEscape) {
            // SUCCESSFUL MOVE
            audioHapticManager.playArrowFly()
            val newCombo = _uiState.value.comboCount + 1
            val cheer = calculateCheerMessage(newCombo)

            // Update arrow state to flying out
            occupancyMap.release(currentArrow)

            _uiState.update { state ->
                val updatedArrows = state.arrows.map {
                    if (it.id == currentArrow.id) it.copyWithState(ArrowState.FLYING_OUT) else it
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
                val durationMs = 520L
                val startTime = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val rawProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                    // Smooth, dynamic kinematic exit curve with immediate responsiveness and accelerating escape
                    val easedProgress = rawProgress * (0.35f + 0.65f * rawProgress)
                    _uiState.update { state ->
                        val updated = state.arrows.map {
                            if (it.id == currentArrow.id && it.state == ArrowState.FLYING_OUT) {
                                it.copy(flightProgress = easedProgress)
                            } else it
                        }
                        state.copy(arrows = updated)
                    }
                    if (rawProgress >= 1f) break
                    delay(16)
                }

                val finalArrows = _uiState.value.arrows.map {
                    if (it.id == currentArrow.id) it.copyWithState(ArrowState.CLEARED) else it
                }
                val allCleared = finalArrows.all { it.state == ArrowState.CLEARED }
                if (allCleared) {
                    triggerLevelVictory(finalArrows)
                } else {
                    _uiState.update { state ->
                        state.copy(arrows = finalArrows)
                    }
                }
            }
        } else {
            // WRONG MOVE: Collision
            audioHapticManager.playWrongMove()

            val newHearts = (_uiState.value.heartsRemaining - 1).coerceAtLeast(0)
            val isOver = newHearts <= 0

            _uiState.update { state ->
                val flashedArrows = state.arrows.map {
                    if (it.id == currentArrow.id) it.copyWithState(ArrowState.WRONG_FLASH) else it
                }
                state.copy(
                    arrows = flashedArrows,
                    heartsRemaining = newHearts,
                    comboCount = 0,
                    cheerMessage = null,
                    isGameOver = isOver
                )
            }

            // Animate collision spring-bounce and reset
            viewModelScope.launch {
                val durationMs = 280L
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
                            it.copyWithState(ArrowState.IDLE, 0f)
                        } else it
                    }
                    state.copy(arrows = resetArrows)
                }
            }
        }
    }

    fun onArrowLongPress(arrow: Arrow, isPressed: Boolean) {
        _uiState.update {
            it.copy(previewRayArrowId = if (isPressed) arrow.id else null)
        }
    }

    fun useHint() {
        val hintTarget = PuzzleSolver.getBestHint(_uiState.value.arrows, occupancyMap) ?: return
        viewModelScope.launch {
            if (repository.useBooster("hint")) {
                audioHapticManager.playBoosterUse()
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
                audioHapticManager.playBoosterUse()
                occupancyMap.release(target)

                val updated = _uiState.value.arrows.map {
                    if (it.id == target.id) it.copyWithState(ArrowState.CLEARED) else it
                }
                val allCleared = updated.all { it.state == ArrowState.CLEARED }
                _uiState.update { it.copy(isHammerModeActive = false) }

                if (allCleared) {
                    triggerLevelVictory(updated)
                } else {
                    _uiState.update { it.copy(arrows = updated) }
                }
            }
        }
    }

    fun useMagicWand() {
        val target = PuzzleSolver.getMagicWandTarget(_uiState.value.arrows, occupancyMap) ?: return
        viewModelScope.launch {
            if (repository.useBooster("wand")) {
                audioHapticManager.playBoosterUse()
                occupancyMap.release(target)

                _uiState.update { state ->
                    val updated = state.arrows.map {
                        if (it.id == target.id) it.copyWithState(ArrowState.FLYING_OUT) else it
                    }
                    state.copy(arrows = updated)
                }

                delay(280)
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
        val nextLvl = _uiState.value.currentLevelNumber + 1
        viewModelScope.launch {
            repository.updateLevel(nextLvl)
            loadLevel(nextLvl)
        }
    }

    fun restartLevel() {
        loadLevel(_uiState.value.currentLevelNumber)
    }

    private suspend fun triggerLevelVictory(finalArrows: List<Arrow>) {
        _uiState.update { state ->
            state.copy(arrows = finalArrows, isCelebrationBurstActive = true)
        }
        audioHapticManager.playLevelWin()
        delay(950)
        _uiState.update { state ->
            state.copy(isLevelComplete = true, isCelebrationBurstActive = false)
        }
        handleLevelComplete()
    }

    private fun handleLevelComplete() {
        viewModelScope.launch {
            repository.addCoins(25)
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
