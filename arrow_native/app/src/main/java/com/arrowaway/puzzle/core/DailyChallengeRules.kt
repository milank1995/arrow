package com.arrowaway.puzzle.core

import com.arrowaway.puzzle.config.RemoteConfigManager

object DailyChallengeRules {
    /**
     * Determines whether the Daily Challenge feature is unlocked for the player.
     * A player who already unlocked dailies (e.g. seen celebration) keeps access forever.
     */
    fun isDailyUnlocked(
        currentLevel: Int,
        lastCompletedLevel: Int = currentLevel - 1,
        hasSeenCelebration: Boolean = false,
        unlockLevel: Int = RemoteConfigManager.dailyChallengeUnlockLevel
    ): Boolean {
        return hasSeenCelebration || currentLevel > unlockLevel || lastCompletedLevel >= unlockLevel
    }
}
