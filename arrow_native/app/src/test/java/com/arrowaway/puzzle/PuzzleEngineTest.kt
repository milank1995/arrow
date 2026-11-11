package com.arrowaway.puzzle

import com.arrowaway.puzzle.core.DailyChallengeRules
import com.arrowaway.puzzle.core.engine.LevelLoader
import com.arrowaway.puzzle.ads.AdManager
import com.arrowaway.puzzle.review.InAppReviewManager


import com.arrowaway.puzzle.core.engine.GridOccupancyMap
import com.arrowaway.puzzle.core.engine.LevelGenerator
import com.arrowaway.puzzle.core.engine.PuzzleSolver
import com.arrowaway.puzzle.core.model.Arrow
import com.arrowaway.puzzle.core.model.ArrowState
import com.arrowaway.puzzle.core.model.Direction
import com.arrowaway.puzzle.core.model.GridCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class PuzzleEngineTest {

    @Test
    fun testDirectRayClearance() {
        // Grid 4x4
        val occupancy = GridOccupancyMap(4, 4)

        // Arrow pointing UP at (1, 1), should escape to (1, 0) -> (1, -1) [OUT]
        val arrow1 = Arrow(
            id = "a1",
            head = GridCoordinate(1, 1),
            direction = Direction.UP,
            body = listOf(GridCoordinate(1, 2), GridCoordinate(1, 1))
        )
        occupancy.occupy(arrow1)

        assertTrue("Arrow pointing towards open edge should be clear", PuzzleSolver.canMoveOut(arrow1, occupancy))
    }

    @Test
    fun testCollisionDetection() {
        val occupancy = GridOccupancyMap(4, 4)

        // Arrow 1 at (1, 2) pointing UP
        val arrow1 = Arrow(
            id = "a1",
            head = GridCoordinate(1, 2),
            direction = Direction.UP,
            body = listOf(GridCoordinate(1, 3), GridCoordinate(1, 2))
        )

        // Arrow 2 blocking it directly at (1, 1)
        val blocker = Arrow(
            id = "a2",
            head = GridCoordinate(1, 1),
            direction = Direction.RIGHT,
            body = listOf(GridCoordinate(0, 1), GridCoordinate(1, 1))
        )

        occupancy.occupy(arrow1)
        occupancy.occupy(blocker)

        assertFalse("Arrow 1 should be blocked by Arrow 2", PuzzleSolver.canMoveOut(arrow1, occupancy))
        assertEquals("Blocker must be identified as arrow 2", blocker.id, PuzzleSolver.findBlockingArrow(arrow1, occupancy)?.id)

        // Blocker points RIGHT towards open edge (2, 1) and (3, 1), should be free
        assertTrue("Blocker should be free to escape", PuzzleSolver.canMoveOut(blocker, occupancy))

        // When blocker escapes and is released:
        occupancy.release(blocker)

        // Arrow 1 should now be free
        assertTrue("Arrow 1 should be free once blocker clears", PuzzleSolver.canMoveOut(arrow1, occupancy))
    }

    @Test
    fun testLevelGenerationAndSolvability() {
        // Test 10 procedurally generated levels
        for (lvl in 1..10) {
            val level = LevelGenerator.generateLevel(lvl)
            assertTrue("Generated level must have arrows", level.arrows.isNotEmpty())

            val occupancy = GridOccupancyMap(level.xSize, level.ySize).apply {
                rebuild(level.arrows)
            }

            // At least one arrow must be movable from the start
            val initialMovable = PuzzleSolver.findAllMovableArrows(level.arrows, occupancy)
            assertTrue("Level $lvl must have at least one valid starting move", initialMovable.isNotEmpty())

            // Simulate solving the level completely
            val currentArrows = level.arrows.map { it.copy() }.toMutableList()
            var steps = 0
            val maxSteps = currentArrows.size + 5

            while (currentArrows.any { it.state != ArrowState.CLEARED } && steps < maxSteps) {
                steps++
                val nextMovable = PuzzleSolver.findAllMovableArrows(
                    currentArrows.filter { it.state != ArrowState.CLEARED },
                    occupancy
                ).firstOrNull()

                assertNotNull("Level $lvl reached deadlock at step $steps", nextMovable)
                occupancy.release(nextMovable!!)

                val idx = currentArrows.indexOfFirst { it.id == nextMovable.id }
                currentArrows[idx] = nextMovable.copyWithState(ArrowState.CLEARED)
            }

            assertTrue("Level $lvl should be 100% solvable without deadlocks", currentArrows.all { it.state == ArrowState.CLEARED })
        }
    }

    @Test
    fun testAuthenticLevelLoaderParsing() {
        val sampleJson = """
            {"XSize":9,"YSize":9,"Arrows":[{"Dx":1,"Dy":0,"X":6,"Y":6,"Indices":[60,59,58,57,56,47,38,29,20],"BendCount":1},{"Dx":0,"Dy":-1,"X":3,"Y":2,"Indices":[21,30,39,40,41,32],"BendCount":2},{"Dx":-1,"Dy":0,"X":3,"Y":5,"Indices":[48,49,50,51,42,33,24,23,22,31],"BendCount":3}],"MaxHearts":3,"Difficulty":"","DiffScore":0,"DiffDNA":""}
        """.trimIndent()

        val level = com.arrowaway.puzzle.core.engine.LevelLoader.parseLevelJson(2, sampleJson)
        assertNotNull("Level 2 should parse successfully", level)
        assertEquals(9, level!!.xSize)
        assertEquals(9, level.ySize)
        assertEquals(3, level.arrows.size)

        // Arrow 1 should point RIGHT (Dx=1, Dy=0)
        assertEquals(Direction.RIGHT, level.arrows[0].direction)

        // Solvability test on parsed level
        val occupancy = GridOccupancyMap(level.xSize, level.ySize).apply {
            rebuild(level.arrows)
        }
        val movable = PuzzleSolver.findAllMovableArrows(level.arrows, occupancy)
        assertTrue("Parsed level must have at least one movable arrow", movable.isNotEmpty())
    }

    @Test
    fun testBlockedArrowHighlightAndAutoRelease() {
        val occupancy = GridOccupancyMap(5, 5)

        // Arrow A pointing UP at (2, 2) -> blocked by Arrow B at (2, 1)
        val arrowA = Arrow(
            id = "aA",
            head = GridCoordinate(2, 2),
            direction = Direction.UP,
            body = listOf(GridCoordinate(2, 3), GridCoordinate(2, 2))
        )

        // Arrow B at (2, 1) pointing RIGHT -> free to escape
        val arrowB = Arrow(
            id = "aB",
            head = GridCoordinate(2, 1),
            direction = Direction.RIGHT,
            body = listOf(GridCoordinate(1, 1), GridCoordinate(2, 1))
        )

        occupancy.occupy(arrowA)
        occupancy.occupy(arrowB)

        // 1. Initial blocked state
        assertFalse(PuzzleSolver.canMoveOut(arrowA, occupancy))
        val highlightedA = arrowA.copyWithState(ArrowState.IDLE, highlighted = true)
        assertTrue(highlightedA.isHighlighted)

        // 2. Arrow B escapes and releases occupancy
        assertTrue(PuzzleSolver.canMoveOut(arrowB, occupancy))
        occupancy.release(arrowB)

        // 3. Arrow A is now eligible for automatic release
        assertTrue("Arrow A must now be clear for auto-release", PuzzleSolver.canMoveOut(highlightedA, occupancy))
        val releasedA = highlightedA.copyWithState(ArrowState.FLYING_OUT, highlighted = false)
        assertFalse("Highlight must clear upon release", releasedA.isHighlighted)
    }

    @Test
    fun testCellOccupancyAuthorityAndNeighborSeparation() {
        // Arrow 1 at (2, 2) pointing RIGHT towards cell (3, 2)
        val arrow1 = Arrow(
            id = "arrow_1",
            head = GridCoordinate(2, 2),
            direction = Direction.RIGHT,
            body = listOf(GridCoordinate(1, 2), GridCoordinate(2, 2))
        )

        // Arrow 2 at (3, 2) pointing UP
        val arrow2 = Arrow(
            id = "arrow_2",
            head = GridCoordinate(3, 2),
            direction = Direction.UP,
            body = listOf(GridCoordinate(3, 3), GridCoordinate(3, 2))
        )

        // Verify cell (3, 2) strictly belongs to Arrow 2, NOT Arrow 1
        val touchedCoord = GridCoordinate(3, 2)
        val activeArrows = listOf(arrow1, arrow2)
        val owner = activeArrows.firstOrNull { it.isPointInside(touchedCoord) }

        assertEquals("Cell (3, 2) must be owned exclusively by arrow 2", "arrow_2", owner?.id)
        assertFalse("Arrow 1 must not own cell (3, 2)", arrow1.isPointInside(touchedCoord))
    }

    @Test
    fun testCuratedLevelHintCalculation() {
        val file = java.io.File("src/main/assets/levels/Level_97.json")
        assertTrue("Level_97.json must exist", file.exists())
        val json = file.readText()
        val level = com.arrowaway.puzzle.core.engine.LevelLoader.parseLevelJson(97, json)
        assertNotNull("Level should parse", level)
        val occupancy = GridOccupancyMap(level!!.xSize, level.ySize).apply {
            rebuild(level.arrows)
        }
        val hint = PuzzleSolver.getBestHint(level.arrows, occupancy)
        assertNotNull("Curated level must have a valid movable hint", hint)
    }

    @Test
    fun testCuratedLevel1Solvability() {
        val file = java.io.File("src/main/assets/levels/Level_1.json")
        assertTrue("Level_1.json must exist in assets", file.exists())
        val json = file.readText()
        val level = com.arrowaway.puzzle.core.engine.LevelLoader.parseLevelJson(1, json)
        assertNotNull("Level 1 should parse successfully", level)
        val expectedArrows = level!!.arrows.size
        assertTrue("Level 1 must have arrows", expectedArrows > 0)

        val occupancy = GridOccupancyMap(level.xSize, level.ySize).apply {
            rebuild(level.arrows)
        }

        val currentArrows = level.arrows.map { it.copy() }.toMutableList()
        var steps = 0
        val maxSteps = currentArrows.size + 5

        while (currentArrows.any { it.state != ArrowState.CLEARED } && steps < maxSteps) {
            steps++
            val movable = PuzzleSolver.findAllMovableArrows(
                currentArrows.filter { it.state != ArrowState.CLEARED },
                occupancy
            ).firstOrNull()

            assertNotNull("Level 1 reached deadlock at step $steps", movable)
            occupancy.release(movable!!)
            val idx = currentArrows.indexOfFirst { it.id == movable.id }
            currentArrows[idx] = movable.copyWithState(ArrowState.CLEARED)
        }

        assertTrue("All arrows must be solved without deadlock", currentArrows.all { it.state == ArrowState.CLEARED })
        assertEquals("Level 1 should be cleared in exactly $expectedArrows steps", expectedArrows, steps)
    }

    @Test
    fun testDailyLevelsParsingAndSolvability() {
        val levelLoader = com.arrowaway.puzzle.core.engine.LevelLoader
        val sampleDailyFiles = listOf(1, 10, 50, 100, 200, 235)
        val assetsDir = java.io.File("src/main/assets/levels")

        for (dailyIdx in sampleDailyFiles) {
            val file = java.io.File(assetsDir, "DailyLevels_$dailyIdx.json")
            assertTrue("Daily level file must exist: ${file.name}", file.exists())
            val jsonStr = file.readText()
            val level = levelLoader.parseLevelJson(dailyIdx, jsonStr)
            assertNotNull("Daily level $dailyIdx must parse cleanly", level)
            assertTrue("Daily level $dailyIdx must have > 100 arrows", level!!.arrows.size > 100)

            val occupancy = GridOccupancyMap(level.xSize, level.ySize).apply {
                rebuild(level.arrows)
            }
            val currentArrows = level.arrows.map { it.copy() }.toMutableList()
            var steps = 0
            val maxSteps = currentArrows.size + 10

            while (currentArrows.any { it.state != ArrowState.CLEARED } && steps < maxSteps) {
                steps++
                val movable = PuzzleSolver.findAllMovableArrows(
                    currentArrows.filter { it.state != ArrowState.CLEARED },
                    occupancy
                ).firstOrNull()

                assertNotNull("Daily Level $dailyIdx reached deadlock at step $steps", movable)
                occupancy.release(movable!!)
                val idx = currentArrows.indexOfFirst { it.id == movable.id }
                currentArrows[idx] = movable.copyWithState(ArrowState.CLEARED)
            }

            assertTrue("Daily Level $dailyIdx must clear all arrows completely", currentArrows.all { it.state == ArrowState.CLEARED })
        }
    }

    @Test
    fun testDailyChallengeRulesUnlockBehavior() {
        // Default unlock level is 5
        assertFalse(DailyChallengeRules.isDailyUnlocked(currentLevel = 5, lastCompletedLevel = 4, hasSeenCelebration = false))
        assertTrue(DailyChallengeRules.isDailyUnlocked(currentLevel = 6, lastCompletedLevel = 5, hasSeenCelebration = false))

        // Custom unlock level at 10
        assertFalse(DailyChallengeRules.isDailyUnlocked(currentLevel = 5, lastCompletedLevel = 4, hasSeenCelebration = false, unlockLevel = 10))
        assertFalse(DailyChallengeRules.isDailyUnlocked(currentLevel = 10, lastCompletedLevel = 9, hasSeenCelebration = false, unlockLevel = 10))
        assertTrue(DailyChallengeRules.isDailyUnlocked(currentLevel = 11, lastCompletedLevel = 10, hasSeenCelebration = false, unlockLevel = 10))

        // Player who already saw unlock celebration keeps access permanently
        assertTrue(DailyChallengeRules.isDailyUnlocked(currentLevel = 2, lastCompletedLevel = 1, hasSeenCelebration = true, unlockLevel = 50))
    }

    @Test
    fun testEasyDailyLevelsParsingAndSolvability() {
        val assetsDir = java.io.File("src/main/assets/levels")
        val levelLoader = LevelLoader

        assertEquals("Total easy daily levels constant must be 44", 44, LevelLoader.TOTAL_EASY_DAILY_LEVELS)

        for (easyIdx in 1..LevelLoader.TOTAL_EASY_DAILY_LEVELS) {
            val file = java.io.File(assetsDir, "DailyEasy_$easyIdx.json")
            assertTrue("Easy daily level file must exist: ${file.name}", file.exists())
            val jsonStr = file.readText()
            val level = levelLoader.parseLevelJson(easyIdx, jsonStr)
            assertNotNull("Easy daily level $easyIdx must parse cleanly", level)
            assertTrue("Easy daily level $easyIdx must have <= 100 arrows", level!!.arrows.size <= 100)
            assertTrue("Easy daily level $easyIdx must have >= 80 arrows", level.arrows.size >= 80)
        }

        // Test solvability on sample easy daily levels (first, middle, last)
        val sampleEasyFiles = listOf(1, 10, 22, 35, 44)
        for (easyIdx in sampleEasyFiles) {
            val file = java.io.File(assetsDir, "DailyEasy_$easyIdx.json")
            val jsonStr = file.readText()
            val level = levelLoader.parseLevelJson(easyIdx, jsonStr)!!

            val occupancy = GridOccupancyMap(level.xSize, level.ySize).apply {
                rebuild(level.arrows)
            }
            val currentArrows = level.arrows.map { it.copy() }.toMutableList()
            var steps = 0
            val maxSteps = currentArrows.size + 10

            while (currentArrows.any { it.state != ArrowState.CLEARED } && steps < maxSteps) {
                steps++
                val movable = PuzzleSolver.findAllMovableArrows(
                    currentArrows.filter { it.state != ArrowState.CLEARED },
                    occupancy
                ).firstOrNull()

                assertNotNull("Easy Daily Level $easyIdx reached deadlock at step $steps", movable)
                occupancy.release(movable!!)
                val idx = currentArrows.indexOfFirst { it.id == movable.id }
                currentArrows[idx] = movable.copyWithState(ArrowState.CLEARED)
            }

            assertTrue("Easy Daily Level $easyIdx must clear all arrows completely", currentArrows.all { it.state == ArrowState.CLEARED })
        }
    }

    @Test
    fun testAdManagerBackoffSequence() {
        assertEquals(30, AdManager.getBackoffDelaySec(0))
        assertEquals(60, AdManager.getBackoffDelaySec(1))
        assertEquals(120, AdManager.getBackoffDelaySec(2))
        assertEquals(240, AdManager.getBackoffDelaySec(3))
        assertEquals(600, AdManager.getBackoffDelaySec(4))
        assertEquals(600, AdManager.getBackoffDelaySec(5))
        assertEquals(600, AdManager.getBackoffDelaySec(99))
    }

    @Test
    fun testInAppReviewEligibilityFirstDailyCompleteAndSafetyRules() {
        // First daily completed triggers review prompt
        val firstDailyResult = InAppReviewManager.checkReviewEligibility(
            currentLevel = 5,
            starsEarned = 3,
            scoreEarned = 100,
            currentStreakDays = 1,
            lastPromptTimestamp = 0L,
            isDailyMode = true,
            completedDailiesCount = 1
        )
        assertTrue("First daily completed must trigger review", firstDailyResult.shouldPrompt)
        assertEquals("first_daily_complete", firstDailyResult.triggerReason)

        // Second daily without streak does not trigger
        val secondDailyResult = InAppReviewManager.checkReviewEligibility(
            currentLevel = 5,
            starsEarned = 2,
            scoreEarned = 80,
            currentStreakDays = 2,
            lastPromptTimestamp = 0L,
            isDailyMode = true,
            completedDailiesCount = 2
        )
        assertFalse("Second daily without 3-day streak should not trigger review", secondDailyResult.shouldPrompt)

        // 3-day streak on daily triggers review
        val streak3Result = InAppReviewManager.checkReviewEligibility(
            currentLevel = 5,
            starsEarned = 3,
            scoreEarned = 100,
            currentStreakDays = 3,
            lastPromptTimestamp = 0L,
            isDailyMode = true,
            completedDailiesCount = 3
        )
        assertTrue("3-day daily streak must trigger review", streak3Result.shouldPrompt)
        assertEquals("daily_streak_3", streak3Result.triggerReason)

        // Safety Rule: Cooldown active (within 30 days) prevents prompt
        val recentPromptTime = System.currentTimeMillis() - (5 * 24 * 3600 * 1000L) // 5 days ago
        val cooldownResult = InAppReviewManager.checkReviewEligibility(
            currentLevel = 5,
            starsEarned = 3,
            scoreEarned = 100,
            currentStreakDays = 1,
            lastPromptTimestamp = recentPromptTime,
            isDailyMode = true,
            completedDailiesCount = 1
        )
        assertFalse("Review prompt must be blocked during 30-day cooldown", cooldownResult.shouldPrompt)

        // Safety Rule: Never prompt on campaign levels < 5
        val earlyLevelResult = InAppReviewManager.checkReviewEligibility(
            currentLevel = 4,
            starsEarned = 3,
            scoreEarned = 100,
            currentStreakDays = 0,
            lastPromptTimestamp = 0L,
            minEligibleLevel = 5,
            isDailyMode = false
        )
        assertFalse("Never prompt before Level 5 in campaign mode", earlyLevelResult.shouldPrompt)
    }

    @Test
    fun testRewardedErrorMapping() {
        assertEquals(AdManager.RewardedStatus.NO_INTERNET, AdManager.statusForLoadError(2)) // network / DNS
        assertEquals(AdManager.RewardedStatus.NO_FILL, AdManager.statusForLoadError(3))     // no fill
        assertEquals(AdManager.RewardedStatus.NO_FILL, AdManager.statusForLoadError(9))     // mediation no fill
        assertEquals(AdManager.RewardedStatus.ERROR, AdManager.statusForLoadError(0))       // internal
        assertEquals(AdManager.RewardedStatus.ERROR, AdManager.statusForLoadError(1))       // invalid request
    }

    @Test
    fun testRewardedButtonStateAndMessages() {
        val s = AdManager.RewardedStatus.LOADING
        assertEquals(AdManager.RewardedStatus.LOADING, com.arrowaway.puzzle.ui.components.effectiveRewardedStatus(s, timedOut = false))
        assertEquals(AdManager.RewardedStatus.NO_FILL, com.arrowaway.puzzle.ui.components.effectiveRewardedStatus(s, timedOut = true))
        assertEquals(AdManager.RewardedStatus.READY, com.arrowaway.puzzle.ui.components.effectiveRewardedStatus(AdManager.RewardedStatus.READY, timedOut = true))
        assertNull(com.arrowaway.puzzle.ui.components.rewardedStatusMessage(AdManager.RewardedStatus.READY))
        assertNull(com.arrowaway.puzzle.ui.components.rewardedStatusMessage(AdManager.RewardedStatus.LOADING))
        assertEquals("No internet connection. Connect to watch an ad.",
            com.arrowaway.puzzle.ui.components.rewardedStatusMessage(AdManager.RewardedStatus.NO_INTERNET))
    }
}
