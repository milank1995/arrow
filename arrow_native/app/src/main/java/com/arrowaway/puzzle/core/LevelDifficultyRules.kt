package com.arrowaway.puzzle.core

/**
 * Single source of truth for how a level's difficulty is presented to the player,
 * so the in-game difficulty label and milestone rewards always agree.
 */
object LevelDifficultyRules {

    /** Campaign milestones: every 5th level is presented as a Hard level (5, 10, 15, ...). */
    fun isHardLevel(levelNumber: Int, isDailyChallenge: Boolean): Boolean =
        !isDailyChallenge && levelNumber > 0 && levelNumber % 5 == 0

    /** Label shown on the victory screen. Matches the labels used before this helper existed. */
    fun difficultyLabel(levelNumber: Int, isDailyChallenge: Boolean): String = when {
        isDailyChallenge -> "Hard"
        isHardLevel(levelNumber, isDailyChallenge) -> "Hard"
        levelNumber > 10 -> "Medium"
        else -> "Normal"
    }

    /**
     * Recurring milestone reward: every 5 levels (5, 10, 15, 20, 25...),
     * the player receives exactly 1 free bonus hint.
     */
    const val MILESTONE_REWARD_HINTS = 1

    fun isMilestoneRewardLevel(levelNumber: Int, isDailyChallenge: Boolean): Boolean =
        !isDailyChallenge && levelNumber >= 5 && levelNumber % 5 == 0
}
