package com.arrowaway.puzzle

import com.arrowaway.puzzle.core.LevelDifficultyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelDifficultyRulesTest {

    /** The victory-screen rule used before LevelDifficultyRules existed. */
    private fun legacyLabel(levelNumber: Int, isDaily: Boolean) = when {
        isDaily -> "Hard"
        levelNumber % 5 == 0 -> "Hard"
        levelNumber > 10 -> "Medium"
        else -> "Normal"
    }

    @Test
    fun victoryLabelsAreUnchanged() {
        for (n in 1..300) {
            assertEquals("campaign level $n", legacyLabel(n, false), LevelDifficultyRules.difficultyLabel(n, false))
            assertEquals("daily level $n", legacyLabel(n, true), LevelDifficultyRules.difficultyLabel(n, true))
        }
    }

    @Test
    fun hardTagMatchesVictoryLabel() {
        for (n in 1..300) {
            assertEquals("level $n", LevelDifficultyRules.difficultyLabel(n, false) == "Hard",
                LevelDifficultyRules.isHardLevel(n, false))
        }
        assertTrue(LevelDifficultyRules.isHardLevel(5, false))
        assertTrue(LevelDifficultyRules.isHardLevel(10, false))
        assertFalse(LevelDifficultyRules.isHardLevel(4, false))
        assertFalse("no in-game Hard tag in daily mode", LevelDifficultyRules.isHardLevel(10, true))
    }

    @Test
    fun milestoneRewardOnlyForCampaignLevel10() {
        assertTrue(LevelDifficultyRules.isMilestoneRewardLevel(10, false))
        assertFalse(LevelDifficultyRules.isMilestoneRewardLevel(10, true))
        assertFalse(LevelDifficultyRules.isMilestoneRewardLevel(5, false))
        assertFalse(LevelDifficultyRules.isMilestoneRewardLevel(20, false))
        assertEquals(2, LevelDifficultyRules.MILESTONE_REWARD_HINTS)
    }
}
