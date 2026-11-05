package com.arrowaway.puzzle

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
        assertEquals("Level 1 must have 4 arrows", 4, level!!.arrows.size)

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
        assertEquals("Level 1 should be cleared in exactly 4 steps", 4, steps)
    }

    @Test
    fun testGlassesPuzzleSolvability() {
        val file = java.io.File("src/main/assets/New Own Shape/Glasses.json")
        assertTrue("Glasses.json must exist in assets", file.exists())
        val json = file.readText()
        val level = com.arrowaway.puzzle.core.engine.LevelLoader.parseLevelJson(999, json)
        assertNotNull("Glasses level should parse successfully", level)
        assertEquals("ShapeName must be Glasses", "Glasses", level!!.shapeName)
        assertEquals("Glasses must have 156 arrows", 156, level.arrows.size)
        assertEquals("XSize must be 60", 60, level.xSize)
        assertEquals("YSize must be 35", 35, level.ySize)

        val occupancy = GridOccupancyMap(level.xSize, level.ySize).apply {
            rebuild(level.arrows)
        }

        val initialMovable = PuzzleSolver.findAllMovableArrows(level.arrows, occupancy)
        assertTrue("Glasses puzzle must have at least one valid starting move", initialMovable.isNotEmpty())

        val currentArrows = level.arrows.map { it.copy() }.toMutableList()
        var steps = 0
        val maxSteps = currentArrows.size + 10

        while (currentArrows.any { it.state != ArrowState.CLEARED } && steps < maxSteps) {
            steps++
            val movable = PuzzleSolver.findAllMovableArrows(
                currentArrows.filter { it.state != ArrowState.CLEARED },
                occupancy
            ).firstOrNull()

            assertNotNull("Glasses puzzle reached deadlock at step $steps", movable)
            occupancy.release(movable!!)
            val idx = currentArrows.indexOfFirst { it.id == movable.id }
            currentArrows[idx] = movable.copyWithState(ArrowState.CLEARED)
        }

        assertTrue("All 156 arrows in Glasses puzzle must be solved without deadlock", currentArrows.all { it.state == ArrowState.CLEARED })
        assertEquals("Glasses puzzle should be cleared in exactly 156 steps", 156, steps)
    }

    private fun verifyCustomShapeSolvability(
        fileName: String,
        expectedShapeName: String,
        expectedArrows: Int,
        expectedX: Int,
        expectedY: Int
    ) {
        val file = java.io.File("src/main/assets/New Own Shape/$fileName")
        assertTrue("$fileName must exist in assets", file.exists())
        val json = file.readText()
        val level = com.arrowaway.puzzle.core.engine.LevelLoader.parseLevelJson(999, json)
        assertNotNull("$fileName level should parse successfully", level)
        assertEquals("ShapeName must be $expectedShapeName", expectedShapeName, level!!.shapeName)
        assertEquals("$fileName must have $expectedArrows arrows", expectedArrows, level.arrows.size)
        assertEquals("XSize must be $expectedX", expectedX, level.xSize)
        assertEquals("YSize must be $expectedY", expectedY, level.ySize)

        val occupancy = GridOccupancyMap(level.xSize, level.ySize).apply {
            rebuild(level.arrows)
        }

        val initialMovable = PuzzleSolver.findAllMovableArrows(level.arrows, occupancy)
        assertTrue("$fileName puzzle must have at least one valid starting move", initialMovable.isNotEmpty())

        val currentArrows = level.arrows.map { it.copy() }.toMutableList()
        var steps = 0
        val maxSteps = currentArrows.size + 10

        while (currentArrows.any { it.state != ArrowState.CLEARED } && steps < maxSteps) {
            steps++
            val movable = PuzzleSolver.findAllMovableArrows(
                currentArrows.filter { it.state != ArrowState.CLEARED },
                occupancy
            ).firstOrNull()

            assertNotNull("$fileName puzzle reached deadlock at step $steps", movable)
            occupancy.release(movable!!)
            val idx = currentArrows.indexOfFirst { it.id == movable.id }
            currentArrows[idx] = movable.copyWithState(ArrowState.CLEARED)
        }

        assertTrue("All $expectedArrows arrows in $fileName puzzle must be solved without deadlock", currentArrows.all { it.state == ArrowState.CLEARED })
        assertEquals("$fileName puzzle should be cleared in exactly $expectedArrows steps", expectedArrows, steps)
    }

    @Test
    fun testCarPuzzleSolvability() {
        verifyCustomShapeSolvability("Car.json", "Car", 120, 39, 30)
    }

    @Test
    fun testCustomDesign1PuzzleSolvability() {
        verifyCustomShapeSolvability("CustomDesign1.json", "CustomDesign1", 544, 75, 64)
    }

    @Test
    fun testGhostPuzzleSolvability() {
        verifyCustomShapeSolvability("Ghost.json", "Ghost", 160, 39, 38)
    }

    @Test
    fun testPotPuzzleSolvability() {
        verifyCustomShapeSolvability("Pot.json", "Pot", 180, 38, 38)
    }

    @Test
    fun testTelephonePuzzleSolvability() {
        verifyCustomShapeSolvability("Telephone.json", "Telephone", 130, 34, 32)
    }

    @Test
    fun testAndroidPuzzleSolvability() {
        verifyCustomShapeSolvability("Android.json", "Android", 100, 28, 40)
    }

    @Test
    fun testHeadphonePuzzleSolvability() {
        verifyCustomShapeSolvability("Headphone.json", "Headphone", 260, 75, 86)
    }

    @Test
    fun testRecyclePuzzleSolvability() {
        verifyCustomShapeSolvability("Recycle.json", "Recycle", 150, 33, 42)
    }

    @Test
    fun testRocketPuzzleSolvability() {
        verifyCustomShapeSolvability("Rocket.json", "Rocket", 180, 33, 49)
    }

    @Test
    fun testTeddyPuzzleSolvability() {
        verifyCustomShapeSolvability("Teddy.json", "Teddy", 300, 48, 64)
    }

    @Test
    fun testCartoon2PuzzleSolvability() {
        verifyCustomShapeSolvability("Cartoon2.json", "Cartoon 2", 200, 48, 76)
    }

    @Test
    fun testFrogPuzzleSolvability() {
        verifyCustomShapeSolvability("Frog.json", "Frog", 250, 55, 81)
    }

    @Test
    fun testPigPuzzleSolvability() {
        verifyCustomShapeSolvability("Pig.json", "Pig", 160, 44, 40)
    }

    @Test
    fun testPuppyPuzzleSolvability() {
        verifyCustomShapeSolvability("Puppy.json", "Puppy", 300, 64, 76)
    }

    @Test
    fun testWomanFacePuzzleSolvability() {
        verifyCustomShapeSolvability("WomanFace.json", "WomanFace", 317, 60, 85)
    }

    @Test
    fun testDuckPuzzleSolvability() {
        verifyCustomShapeSolvability("Duck.json", "Duck", 350, 68, 68)
    }

    @Test
    fun testFaceMaskPuzzleSolvability() {
        verifyCustomShapeSolvability("FaceMask.json", "Face Mask", 120, 42, 42)
    }

    @Test
    fun testSnakePuzzleSolvability() {
        verifyCustomShapeSolvability("Snake.json", "Snake", 200, 50, 50)
    }

    @Test
    fun testSunflower2PuzzleSolvability() {
        verifyCustomShapeSolvability("Sunflower2.json", "Sun Flower 2", 550, 78, 78)
    }

    @Test
    fun testCustomDesign2PuzzleSolvability() {
        verifyCustomShapeSolvability("CustomDesign2.json", "Unknown Shape 9", 500, 78, 88)
    }

    @Test
    fun testCat2PuzzleSolvability() {
        verifyCustomShapeSolvability("Cat2.json", "Cat 2", 400, 78, 78)
    }

    @Test
    fun testChildBirdPuzzleSolvability() {
        verifyCustomShapeSolvability("ChildBird.json", "Child Bird", 133, 40, 40)
    }

    @Test
    fun testTurtlePuzzleSolvability() {
        verifyCustomShapeSolvability("Turtle.json", "Turtles", 250, 62, 37)
    }

    @Test
    fun testCustomDesign3PuzzleSolvability() {
        verifyCustomShapeSolvability("CustomDesign3.json", "Unknown Shape 8", 750, 96, 96)
    }

    @Test
    fun testWarriorFacePuzzleSolvability() {
        verifyCustomShapeSolvability("WarriorFace.json", "Warrior Face", 350, 70, 70)
    }

    @Test
    fun testPaintboardPuzzleSolvability() {
        verifyCustomShapeSolvability("Paintboard.json", "Paintboard", 150, 48, 49)
    }

    @Test
    fun testPlanetPuzzleSolvability() {
        verifyCustomShapeSolvability("Planet.json", "Planet", 198, 51, 51)
    }

    @Test
    fun testScrewdriverPuzzleSolvability() {
        verifyCustomShapeSolvability("Screwdriver.json", "Screw Driver", 92, 44, 44)
    }

    @Test
    fun testSkullPuzzleSolvability() {
        verifyCustomShapeSolvability("Skull.json", "Skull", 200, 37, 37)
    }

    @Test
    fun testSnakeFacePuzzleSolvability() {
        verifyCustomShapeSolvability("SnakeFace.json", "Snake Face", 90, 45, 36)
    }
}

