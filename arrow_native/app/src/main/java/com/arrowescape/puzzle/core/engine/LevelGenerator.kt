package com.arrowescape.puzzle.core.engine

import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.Direction
import com.arrowescape.puzzle.core.model.GridCoordinate
import com.arrowescape.puzzle.core.model.LevelData
import com.arrowescape.puzzle.core.model.LevelDifficulty
import kotlin.random.Random

object LevelGenerator {

    fun generateLevel(levelNumber: Int, seed: Long = levelNumber.toLong()): LevelData {
        return when (levelNumber) {
            1 -> createLevel1()
            2 -> createLevel2()
            3 -> createLevel3()
            else -> generateProceduralLevel(levelNumber, seed)
        }
    }

    /**
     * Level 1: Clean introductory 2-arrow puzzle
     */
    private fun createLevel1(): LevelData {
        val a1 = Arrow(
            id = "arrow_1",
            head = GridCoordinate(3, 1),
            direction = Direction.RIGHT,
            body = listOf(GridCoordinate(1, 1), GridCoordinate(2, 1), GridCoordinate(3, 1)),
            bendCount = 0,
            colorIndex = 0
        )
        val a2 = Arrow(
            id = "arrow_2",
            head = GridCoordinate(2, 2),
            direction = Direction.UP,
            body = listOf(GridCoordinate(2, 3), GridCoordinate(2, 2)),
            bendCount = 0,
            colorIndex = 0
        )
        return LevelData(
            levelNumber = 1,
            xSize = 5,
            ySize = 5,
            arrows = listOf(a1, a2),
            maxHearts = 3,
            difficulty = LevelDifficulty.EASY,
            diffScore = 20,
            diffDna = "LVL_1_5x5_2"
        )
    }

    /**
     * Level 2: Exact 100% replication of Screenshot 2
     * 3 Arrows on a 5x5 board:
     * 1. Outer Arrow: starts bottom-left, goes up, turns right, points right.
     * 2. Middle Arrow: S-shape loop, head points left (blocked by outer).
     * 3. Inner Arrow: inverted U-arch, head points down.
     */
    private fun createLevel2(): LevelData {
        val a1 = Arrow(
            id = "arrow_1",
            head = GridCoordinate(4, 0),
            direction = Direction.RIGHT,
            body = listOf(
                GridCoordinate(0, 4),
                GridCoordinate(0, 3),
                GridCoordinate(0, 2),
                GridCoordinate(0, 1),
                GridCoordinate(0, 0),
                GridCoordinate(1, 0),
                GridCoordinate(2, 0),
                GridCoordinate(3, 0),
                GridCoordinate(4, 0)
            ),
            bendCount = 1,
            colorIndex = 0
        )

        val a2 = Arrow(
            id = "arrow_2",
            head = GridCoordinate(1, 1),
            direction = Direction.LEFT,
            body = listOf(
                GridCoordinate(2, 3),
                GridCoordinate(2, 4),
                GridCoordinate(3, 4),
                GridCoordinate(4, 4),
                GridCoordinate(4, 3),
                GridCoordinate(4, 2),
                GridCoordinate(4, 1),
                GridCoordinate(3, 1),
                GridCoordinate(2, 1),
                GridCoordinate(1, 1)
            ),
            bendCount = 3,
            colorIndex = 0
        )

        val a3 = Arrow(
            id = "arrow_3",
            head = GridCoordinate(1, 4),
            direction = Direction.DOWN,
            body = listOf(
                GridCoordinate(3, 3),
                GridCoordinate(3, 2),
                GridCoordinate(2, 2),
                GridCoordinate(1, 2),
                GridCoordinate(1, 3),
                GridCoordinate(1, 4)
            ),
            bendCount = 2,
            colorIndex = 0
        )

        return LevelData(
            levelNumber = 2,
            xSize = 5,
            ySize = 5,
            arrows = listOf(a1, a2, a3),
            maxHearts = 3,
            difficulty = LevelDifficulty.EASY,
            diffScore = 55,
            diffDna = "LVL_2_5x5_3"
        )
    }

    /**
     * Level 3: Exact 100% replication of Screenshot 3
     * 5 Arrows on a 10x10 board:
     * 1. Giant outer frame arrow: head at (0, 0) pointing UP.
     * 2. Arrow pointing LEFT at (1, 2).
     * 3. Arrow pointing RIGHT at (8, 4).
     * 4. Arrow pointing LEFT at (1, 6).
     * 5. Arrow pointing RIGHT at (8, 8).
     */
    private fun createLevel3(): LevelData {
        // Outer U-frame Arrow 1
        val body1 = mutableListOf<GridCoordinate>()
        body1.addAll(listOf(GridCoordinate(9, 7), GridCoordinate(9, 8), GridCoordinate(9, 9)))
        for (x in 8 downTo 0) {
            body1.add(GridCoordinate(x, 9))
        }
        for (y in 8 downTo 0) {
            body1.add(GridCoordinate(0, y))
        }
        val a1 = Arrow(
            id = "arrow_1",
            head = GridCoordinate(0, 0),
            direction = Direction.UP,
            body = body1,
            bendCount = 2,
            colorIndex = 0
        )

        // Arrow 2: pointing LEFT at (1, 2)
        val body2 = mutableListOf<GridCoordinate>()
        body2.addAll(listOf(GridCoordinate(8, 3), GridCoordinate(8, 2)))
        for (x in 7 downTo 1) {
            body2.add(GridCoordinate(x, 2))
        }
        val a2 = Arrow(
            id = "arrow_2",
            head = GridCoordinate(1, 2),
            direction = Direction.LEFT,
            body = body2,
            bendCount = 1,
            colorIndex = 0
        )

        // Arrow 3: pointing RIGHT at (8, 4)
        val body3 = mutableListOf<GridCoordinate>()
        body3.addAll(listOf(GridCoordinate(1, 5), GridCoordinate(1, 4)))
        for (x in 2..8) {
            body3.add(GridCoordinate(x, 4))
        }
        val a3 = Arrow(
            id = "arrow_3",
            head = GridCoordinate(8, 4),
            direction = Direction.RIGHT,
            body = body3,
            bendCount = 1,
            colorIndex = 0
        )

        // Arrow 4: pointing LEFT at (1, 6)
        val body4 = mutableListOf<GridCoordinate>()
        body4.addAll(listOf(GridCoordinate(8, 7), GridCoordinate(8, 6)))
        for (x in 7 downTo 1) {
            body4.add(GridCoordinate(x, 6))
        }
        val a4 = Arrow(
            id = "arrow_4",
            head = GridCoordinate(1, 6),
            direction = Direction.LEFT,
            body = body4,
            bendCount = 1,
            colorIndex = 0
        )

        // Arrow 5: pointing RIGHT at (8, 8)
        val body5 = mutableListOf<GridCoordinate>()
        body5.add(GridCoordinate(1, 8))
        for (x in 2..8) {
            body5.add(GridCoordinate(x, 8))
        }
        val a5 = Arrow(
            id = "arrow_5",
            head = GridCoordinate(8, 8),
            direction = Direction.RIGHT,
            body = body5,
            bendCount = 0,
            colorIndex = 0
        )

        return LevelData(
            levelNumber = 3,
            xSize = 10,
            ySize = 10,
            arrows = listOf(a1, a2, a3, a4, a5),
            maxHearts = 3,
            difficulty = LevelDifficulty.EASY,
            diffScore = 80,
            diffDna = "LVL_3_10x10_5"
        )
    }

    private fun generateProceduralLevel(levelNumber: Int, seed: Long): LevelData {
        val random = Random(seed)

        val (width, height, arrowCount, maxBends, difficulty) = when {
            levelNumber <= 5 -> Config(5, 5, targetArrows = 6, maxBends = 1, LevelDifficulty.EASY)
            levelNumber <= 15 -> Config(6, 6, targetArrows = 9, maxBends = 1, LevelDifficulty.MEDIUM)
            levelNumber <= 30 -> Config(7, 7, targetArrows = 12, maxBends = 2, LevelDifficulty.MEDIUM)
            levelNumber <= 60 -> Config(8, 8, targetArrows = 16, maxBends = 2, LevelDifficulty.HARD)
            else -> {
                val w = minOf(9, 6 + (levelNumber / 40))
                val h = minOf(11, 7 + (levelNumber / 30))
                Config(w, h, targetArrows = minOf(30, 12 + levelNumber / 4), maxBends = 2, LevelDifficulty.HARD)
            }
        }

        val occupancy = GridOccupancyMap(width, height)
        val generatedArrows = mutableListOf<Arrow>()

        var attempts = 0
        val maxAttempts = 700

        while (generatedArrows.size < arrowCount && attempts < maxAttempts) {
            attempts++

            val headX = random.nextInt(width)
            val headY = random.nextInt(height)
            val head = GridCoordinate(headX, headY)

            if (!occupancy.isCellFree(head)) continue

            val directions = Direction.entries.shuffled(random)
            var chosenDirection: Direction? = null

            for (dir in directions) {
                var ray = head.step(dir)
                var rayClear = true
                while (occupancy.isInside(ray)) {
                    if (!occupancy.isCellFree(ray)) {
                        rayClear = false
                        break
                    }
                    ray = ray.step(dir)
                }
                if (rayClear) {
                    chosenDirection = dir
                    break
                }
            }

            if (chosenDirection == null) continue

            val body = mutableListOf<GridCoordinate>()
            body.add(head)

            val bodyLength = if (maxBends > 0 && random.nextFloat() > 0.4f) {
                random.nextInt(2, 5)
            } else {
                random.nextInt(1, 3)
            }

            var currentPoint = head
            var currentBackDir = chosenDirection.opposite
            var bends = 0

            for (step in 1 until bodyLength) {
                if (bends < maxBends && random.nextFloat() < 0.45f) {
                    val turnDirs = currentBackDir.perpendiculars().shuffled(random)
                    val turnChoice = turnDirs.firstOrNull { occupancy.isCellFree(currentPoint.step(it)) }
                    if (turnChoice != null) {
                        currentBackDir = turnChoice
                        bends++
                    }
                }

                val nextPoint = currentPoint.step(currentBackDir)
                if (occupancy.isInside(nextPoint) && occupancy.isCellFree(nextPoint) && !body.contains(nextPoint)) {
                    body.add(nextPoint)
                    currentPoint = nextPoint
                } else {
                    break
                }
            }

            val arrow = Arrow(
                id = "arrow_${generatedArrows.size + 1}",
                head = head,
                direction = chosenDirection,
                body = body,
                bendCount = bends,
                colorIndex = 0
            )

            occupancy.occupy(arrow)
            generatedArrows.add(arrow)
        }

        val maxHearts = if (difficulty == LevelDifficulty.HARD) 5 else 3
        val diffScore = (generatedArrows.size * 10) + (generatedArrows.sumOf { it.bendCount } * 15)

        return LevelData(
            levelNumber = levelNumber,
            xSize = width,
            ySize = height,
            arrows = generatedArrows,
            maxHearts = maxHearts,
            difficulty = difficulty,
            diffScore = diffScore,
            diffDna = "LVL_${levelNumber}_${width}x${height}_${generatedArrows.size}"
        )
    }

    private data class Config(
        val width: Int,
        val height: Int,
        val targetArrows: Int,
        val maxBends: Int,
        val difficulty: LevelDifficulty
    )
}
