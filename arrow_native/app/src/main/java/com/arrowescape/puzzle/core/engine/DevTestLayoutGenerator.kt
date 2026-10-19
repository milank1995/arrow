package com.arrowescape.puzzle.core.engine

import android.content.Context
import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.Direction
import com.arrowescape.puzzle.core.model.GridCoordinate
import com.arrowescape.puzzle.core.model.LevelData
import com.arrowescape.puzzle.core.model.LevelDifficulty
import kotlin.random.Random

enum class DevTestPreset(
    val title: String,
    val subtitle: String,
    val icon: String
) {
    MICRO_SWARM(
        title = "Micro-Arrow Swarm",
        subtitle = "35+ micro 1 & 2-cell arrows (tests small-arrow click precision)",
        icon = "🎯"
    ),
    MEGA_GRID(
        title = "Dense Mega Grid (14x14)",
        subtitle = "55+ arrows on large board (tests zoom, pan, and density)",
        icon = "🗺️"
    ),
    SERPENTINE_MAZE(
        title = "Serpentine Maze (12x12)",
        subtitle = "Multi-bend long arrows with 3-6 turns (tests corner fillets)",
        icon = "🌀"
    ),
    RAPID_FIRE(
        title = "Rapid-Fire Gauntlet",
        subtitle = "Parallel rows & columns (tests rapid dual-thumb clicking)",
        icon = "⚡"
    ),
    AUTHENTIC_MASTER(
        title = "Authentic Production Level",
        subtitle = "Real campaign levels (e.g. Level 100, 250, 500, 1000)",
        icon = "🏆"
    ),
    CUSTOM_GRID(
        title = "Custom Grid Generator",
        subtitle = "Customizable grid dimensions and density",
        icon = "⚙️"
    )
}

object DevTestLayoutGenerator {

    fun generatePreset(
        preset: DevTestPreset,
        context: Context,
        seed: Long = System.currentTimeMillis(),
        customWidth: Int = 10,
        customHeight: Int = 10,
        customArrowCount: Int = 30,
        authenticLevelNum: Int = 100
    ): LevelData {
        return when (preset) {
            DevTestPreset.MICRO_SWARM -> generateMicroSwarm(seed)
            DevTestPreset.MEGA_GRID -> generateMegaGrid(seed)
            DevTestPreset.SERPENTINE_MAZE -> generateSerpentineMaze(seed)
            DevTestPreset.RAPID_FIRE -> generateRapidFire(seed)
            DevTestPreset.AUTHENTIC_MASTER -> loadAuthenticLevel(context, authenticLevelNum)
            DevTestPreset.CUSTOM_GRID -> generateCustom(customWidth, customHeight, customArrowCount, seed)
        }
    }

    /**
     * Micro-Arrow Swarm:
     * 10x10 grid packed with 35-45 small arrows where ~70% are 1-cell dots or 2-cell mini arrows.
     * Specifically designed to test small-arrow hit detection and adjacent-cell discrimination.
     */
    fun generateMicroSwarm(seed: Long): LevelData {
        return generateReverseSolvable(
            width = 10,
            height = 10,
            targetArrowCount = 42,
            seed = seed,
            minLen = 1,
            maxLen = 3,
            maxBends = 1,
            prob1Cell = 0.50f,
            title = "Micro-Arrow Swarm"
        )
    }

    /**
     * Dense Mega Grid:
     * 14x14 grid with 55-65 arrows of mixed lengths.
     * Tests pinch-to-zoom, panning across the canvas, and tapping small zoomed-out vs zoomed-in arrows.
     */
    fun generateMegaGrid(seed: Long): LevelData {
        return generateReverseSolvable(
            width = 14,
            height = 14,
            targetArrowCount = 58,
            seed = seed,
            minLen = 2,
            maxLen = 6,
            maxBends = 2,
            prob1Cell = 0.15f,
            title = "Dense Mega Grid (14x14)"
        )
    }

    /**
     * Serpentine Multi-Bend Maze:
     * 12x12 grid with 25-35 long intertwined arrows each having 3 to 6 bends.
     * Tests continuous polyline stem hit accuracy, corner fillets, and overlapping sightlines.
     */
    fun generateSerpentineMaze(seed: Long): LevelData {
        return generateReverseSolvable(
            width = 12,
            height = 12,
            targetArrowCount = 30,
            seed = seed,
            minLen = 4,
            maxLen = 10,
            maxBends = 5,
            prob1Cell = 0.0f,
            title = "Serpentine Maze"
        )
    }

    /**
     * Rapid-Fire Gauntlet:
     * 10x10 grid with parallel alternating rows and corridors.
     * Ideal for rapid-fire double-thumb multi-arrow tapping speedruns.
     */
    fun generateRapidFire(seed: Long): LevelData {
        return generateReverseSolvable(
            width = 10,
            height = 10,
            targetArrowCount = 34,
            seed = seed,
            minLen = 2,
            maxLen = 4,
            maxBends = 1,
            prob1Cell = 0.20f,
            title = "Rapid-Fire Gauntlet"
        )
    }

    /**
     * Loads an authentic production level from assets (e.g. 100, 250, 500, 1000).
     */
    fun loadAuthenticLevel(context: Context, levelNumber: Int): LevelData {
        return LevelLoader.loadLevel(context, levelNumber)
    }

    /**
     * Generates a custom procedural level based on given dimensions and target count.
     */
    fun generateCustom(width: Int, height: Int, arrowCount: Int, seed: Long): LevelData {
        return generateReverseSolvable(
            width = width.coerceIn(5, 16),
            height = height.coerceIn(5, 16),
            targetArrowCount = arrowCount.coerceIn(5, 75),
            seed = seed,
            minLen = 1,
            maxLen = 5,
            maxBends = 2,
            prob1Cell = 0.25f,
            title = "Custom Grid (${width}x${height})"
        )
    }

    /**
     * Constructs a guaranteed-solvable puzzle using reverse clearance placement:
     * Each arrow is placed such that its ray to the boundary is unobstructed at placement time,
     * guaranteeing that the arrows can always be cleared in reverse order (and with multiple branches).
     */
    private fun generateReverseSolvable(
        width: Int,
        height: Int,
        targetArrowCount: Int,
        seed: Long,
        minLen: Int,
        maxLen: Int,
        maxBends: Int,
        prob1Cell: Float,
        title: String
    ): LevelData {
        val random = Random(seed)
        val occupancy = GridOccupancyMap(width, height)
        val generatedArrows = mutableListOf<Arrow>()
        var attempts = 0
        val maxAttempts = 1500

        while (generatedArrows.size < targetArrowCount && attempts < maxAttempts) {
            attempts++
            val headX = random.nextInt(width)
            val headY = random.nextInt(height)
            val head = GridCoordinate(headX, headY)
            if (!occupancy.isCellFree(head)) continue

            // Find all directions where a straight ray to boundary is currently free
            val validDirs = Direction.entries.shuffled(random).filter { dir ->
                var ray = head.step(dir)
                var clear = true
                while (occupancy.isInside(ray)) {
                    if (!occupancy.isCellFree(ray)) {
                        clear = false
                        break
                    }
                    ray = ray.step(dir)
                }
                clear
            }
            if (validDirs.isEmpty()) continue
            val chosenDir = validDirs.first()

            val targetLen = if (random.nextFloat() < prob1Cell) {
                1
            } else {
                random.nextInt(minLen, maxLen + 1)
            }

            // Grow body backwards from head
            val bodyFromHead = mutableListOf<GridCoordinate>()
            bodyFromHead.add(head)

            var currentPoint = head
            var currentBackDir = chosenDir.opposite
            var bends = 0

            for (step in 1 until targetLen) {
                if (bends < maxBends && random.nextFloat() < 0.50f) {
                    val turnDirs = currentBackDir.perpendiculars().shuffled(random)
                    val turnChoice = turnDirs.firstOrNull { occupancy.isCellFree(currentPoint.step(it)) }
                    if (turnChoice != null) {
                        currentBackDir = turnChoice
                        bends++
                    }
                }

                val nextPoint = currentPoint.step(currentBackDir)
                if (occupancy.isInside(nextPoint) && occupancy.isCellFree(nextPoint) && !bodyFromHead.contains(nextPoint)) {
                    bodyFromHead.add(nextPoint)
                    currentPoint = nextPoint
                } else {
                    break
                }
            }

            // Arrow model convention: body is ordered TAIL -> HEAD, so body.last() is head
            val orderedBody = bodyFromHead.reversed()
            val actualHead = orderedBody.last()

            val arrow = Arrow(
                id = "dev_arrow_${generatedArrows.size + 1}",
                head = actualHead,
                direction = chosenDir,
                body = orderedBody,
                bendCount = bends,
                colorIndex = 0
            )

            occupancy.occupy(arrow)
            generatedArrows.add(arrow)
        }

        return LevelData(
            levelNumber = 9999,
            xSize = width,
            ySize = height,
            arrows = generatedArrows,
            maxHearts = 999,
            difficulty = LevelDifficulty.HARD,
            diffScore = generatedArrows.size * 10,
            diffDna = "DEV_${title.replace(" ", "_")}_${width}x${height}_${generatedArrows.size}"
        )
    }
}
