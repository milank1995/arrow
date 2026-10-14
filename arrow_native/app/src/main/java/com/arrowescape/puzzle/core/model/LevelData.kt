package com.arrowescape.puzzle.core.model

enum class LevelDifficulty(val label: String, val maxBends: Int) {
    EASY("Easy", 0),
    MEDIUM("Medium", 1),
    HARD("Hard", 2)
}

data class LevelData(
    val levelNumber: Int,
    val xSize: Int,
    val ySize: Int,
    val arrows: List<Arrow>,
    val maxHearts: Int = 3,
    val difficulty: LevelDifficulty = LevelDifficulty.EASY,
    val diffScore: Int = 100,
    val diffDna: String = ""
) {
    val totalArrows: Int get() = arrows.size
}
