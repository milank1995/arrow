package com.arrowaway.puzzle

import com.arrowaway.puzzle.core.engine.GridOccupancyMap
import com.arrowaway.puzzle.core.engine.LevelLoader
import com.arrowaway.puzzle.core.engine.PuzzleSolver
import com.arrowaway.puzzle.core.model.LevelData
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Checks the original onboarding levels 1-10: they parse, stay in bounds, never overlap,
 * are solvable with the game's own blocking rule, follow the planned pacing, and match the manifest.
 */
class IntroLevelsTest {

    private val levelsDir: File = listOf("src/main/assets/levels", "app/src/main/assets/levels")
        .map { File(it) }.first { it.isDirectory }

    private val expectedArrows = mapOf(1 to 3, 2 to 4, 3 to 6, 4 to 9, 5 to 23, 6 to 15, 7 to 18, 8 to 25, 9 to 36, 10 to 51)

    private fun load(n: Int): LevelData {
        val json = File(levelsDir, "Level_$n.json").readText()
        val level = LevelLoader.parseLevelJson(n, json)
        assertNotNull("Level $n should parse", level)
        return level!!
    }

    /** Greedy clear using PuzzleSolver.canMoveOut; returns the number of rounds, or -1 if stuck. */
    private fun clearRounds(level: LevelData): Int {
        var remaining = level.arrows
        var rounds = 0
        while (remaining.isNotEmpty()) {
            val map = GridOccupancyMap(level.xSize, level.ySize).apply { rebuild(remaining) }
            val free = remaining.filter { PuzzleSolver.canMoveOut(it, map) }
            if (free.isEmpty()) return -1
            remaining = remaining - free.toSet()
            rounds++
        }
        return rounds
    }

    @Test
    fun onboardingLevelsAreValidAndSolvable() {
        for (n in 1..10) {
            val level = load(n)
            assertEquals("Level $n arrow count", expectedArrows[n], level.arrows.size)
            val cells = level.arrows.flatMap { it.allPoints }
            assertEquals("Level $n arrows must not overlap", cells.size, cells.toSet().size)
            cells.forEach {
                assertTrue("Level $n cell $it out of bounds",
                    it.x in 0 until level.xSize && it.y in 0 until level.ySize)
            }
            assertTrue("Level $n must be solvable", clearRounds(level) > 0)
            assertEquals("Level $n hearts (lives are limited to 3)", 3, level.maxHearts)
        }
    }

    @Test
    fun onboardingPacingTeachesThenSpikes() {
        assertEquals("Level 1: every arrow free at once", 1, clearRounds(load(1)))
        assertEquals("Level 2: pinwheel, one arrow at a time", 4, clearRounds(load(2)))
        for (n in 1..4) {
            val level = load(n)
            assertTrue("Level $n board should stay small", level.xSize <= 8 && level.ySize <= 8)
        }
        // Hard milestones need more steps than the levels on either side of them.
        assertTrue("Level 5 harder than 4 and 6", clearRounds(load(5)) > clearRounds(load(4)) && clearRounds(load(5)) > clearRounds(load(6)))
        assertTrue("Level 10 hardest of the ten", (1..9).all { clearRounds(load(10)) > clearRounds(load(it)) })
    }

    @Test
    fun manifestMatchesLevelFiles() {
        val manifest = JSONArray(File(levelsDir, "manifest.json").readText())
        for (n in 1..10) {
            val entry = (0 until manifest.length()).map { manifest.getJSONObject(it) }
                .first { it.getInt("LevelNumber") == n }
            val level = load(n)
            assertEquals("manifest ArrowCount $n", level.arrows.size, entry.getInt("ArrowCount"))
            assertEquals("manifest XSize $n", level.xSize, entry.getInt("XSize"))
            assertEquals("manifest YSize $n", level.ySize, entry.getInt("YSize"))
        }
    }
}
