package com.arrowescape.puzzle

import com.arrowescape.puzzle.core.engine.GridOccupancyMap
import com.arrowescape.puzzle.core.engine.LevelGenerator
import com.arrowescape.puzzle.core.engine.PuzzleSolver
import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.ArrowState
import com.arrowescape.puzzle.core.model.Direction
import com.arrowescape.puzzle.core.model.GridCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

        val level = com.arrowescape.puzzle.core.engine.LevelLoader.parseLevelJson(2, sampleJson)
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
}
