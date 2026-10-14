package com.arrowescape.puzzle.core.engine

import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.Direction
import com.arrowescape.puzzle.core.model.GridCoordinate
import com.arrowescape.puzzle.core.model.LevelData
import com.arrowescape.puzzle.core.model.LevelDifficulty
import kotlin.random.Random

object LevelGenerator {

    fun generateLevel(levelNumber: Int, seed: Long = levelNumber.toLong()): LevelData {
        val random = Random(seed)

        // Calculate grid dimensions and complexity scaling
        val (width, height, arrowCount, maxBends, difficulty) = when {
            levelNumber <= 2 -> Config(4, 4, targetArrows = 4, maxBends = 0, LevelDifficulty.EASY)
            levelNumber <= 5 -> Config(5, 5, targetArrows = 6, maxBends = 1, LevelDifficulty.EASY)
            levelNumber <= 15 -> Config(6, 6, targetArrows = 10, maxBends = 1, LevelDifficulty.MEDIUM)
            levelNumber <= 30 -> Config(7, 7, targetArrows = 14, maxBends = 2, LevelDifficulty.MEDIUM)
            levelNumber <= 60 -> Config(7, 8, targetArrows = 18, maxBends = 2, LevelDifficulty.HARD)
            else -> {
                val w = minOf(9, 6 + (levelNumber / 40))
                val h = minOf(11, 7 + (levelNumber / 30))
                Config(w, h, targetArrows = minOf(36, 12 + levelNumber / 4), maxBends = 2, LevelDifficulty.HARD)
            }
        }

        val occupancy = GridOccupancyMap(width, height)
        val generatedArrows = mutableListOf<Arrow>()
        val totalCells = width * height

        var attempts = 0
        val maxAttempts = 600

        // Reverse-construction loop
        while (generatedArrows.size < arrowCount && attempts < maxAttempts) {
            attempts++

            // Pick a random available cell for the arrow head
            val headX = random.nextInt(width)
            val headY = random.nextInt(height)
            val head = GridCoordinate(headX, headY)

            if (!occupancy.isCellFree(head)) continue

            // Pick a direction that has a clear path to the grid boundary
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

            // Build body backwards
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
                // Chance to bend if allowed
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

            // Create arrow
            val arrow = Arrow(
                id = "arrow_${generatedArrows.size}",
                head = head,
                direction = chosenDirection,
                body = body,
                bendCount = bends,
                colorIndex = generatedArrows.size % 4
            )

            // Register in occupancy map
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
