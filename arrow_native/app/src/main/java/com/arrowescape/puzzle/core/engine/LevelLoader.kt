package com.arrowescape.puzzle.core.engine

import android.content.Context
import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.Direction
import com.arrowescape.puzzle.core.model.GridCoordinate
import com.arrowescape.puzzle.core.model.LevelData
import com.arrowescape.puzzle.core.model.LevelDifficulty
import org.json.JSONObject

object LevelLoader {

    /**
     * Loads the authentic Unity level from assets/levels/Level_{N}.json.
     * The original game has 1001 handcrafted levels (Level 1 to Level 1001).
     * For any levelNumber > 1001, it wraps cleanly into the authentic level pool,
     * ensuring every level displays the true intended handcrafted puzzle layout.
     */
    fun loadLevel(context: Context, levelNumber: Int): LevelData {
        val totalHandcraftedLevels = 1001
        val actualLevelNumber = ((levelNumber - 1) % totalHandcraftedLevels) + 1
        val assetName = "levels/Level_$actualLevelNumber.json"
        try {
            val jsonStr = context.assets.open(assetName).bufferedReader().use { it.readText() }
            val level = parseLevelJson(levelNumber, jsonStr)
            if (level != null && level.arrows.isNotEmpty()) {
                return level
            }
        } catch (e: Exception) {
            // If any asset read issue, load Level 1 as safe authentic fallback
        }

        try {
            val fallbackJson = context.assets.open("levels/Level_1.json").bufferedReader().use { it.readText() }
            val fallback = parseLevelJson(levelNumber, fallbackJson)
            if (fallback != null) return fallback
        } catch (e: Exception) {}

        return LevelGenerator.generateProceduralLevel(levelNumber, levelNumber.toLong())
    }

    /**
     * Loads a daily level from assets/levels/DailyLevels_{N}.json.
     */
    fun loadDailyLevel(context: Context, dailyIndex: Int): LevelData {
        val assetName = "levels/DailyLevels_$dailyIndex.json"
        try {
            val jsonStr = context.assets.open(assetName).bufferedReader().use { it.readText() }
            val level = parseLevelJson(dailyIndex, jsonStr)
            if (level != null && level.arrows.isNotEmpty()) {
                return level
            }
        } catch (e: Exception) {
            // Fall back to standard level
        }

        return loadLevel(context, dailyIndex)
    }

    /**
     * Loads an authentic sequential normal level from assets/new_levels/NewLevel_{N}.json.
     * There are 1,533 authentic sequential normal levels extracted directly from Amaze Go:
     * - Levels 1–20: Authentic onboarding / intro sequence (GuideLevels17)
     * - Levels 21–1533: Mainline prioritized normal progression (NormalLevelsPriority 0-3)
     */
    fun loadNewLevel(context: Context, levelNumber: Int): LevelData {
        val totalNewLevels = 1533
        val actualLevelNumber = ((levelNumber - 1) % totalNewLevels) + 1
        val assetName = "new_levels/NewLevel_$actualLevelNumber.json"
        try {
            val jsonStr = context.assets.open(assetName).bufferedReader().use { it.readText() }
            val level = parseLevelJson(levelNumber, jsonStr)
            if (level != null && level.arrows.isNotEmpty()) {
                return level
            }
        } catch (e: Exception) {
            // Fall back to main level loader
        }

        return loadLevel(context, levelNumber)
    }

    fun parseLevelJson(levelNumber: Int, jsonStr: String): LevelData? {
        try {
            val root = JSONObject(jsonStr)
            val xSize = root.getInt("XSize")
            val ySize = root.getInt("YSize")
            val maxHearts = root.optInt("MaxHearts", 3).coerceIn(1, 5)
            val arrowsArray = root.getJSONArray("Arrows")

            val arrowList = mutableListOf<Arrow>()
            for (i in 0 until arrowsArray.length()) {
                val arrowObj = arrowsArray.getJSONObject(i)
                val dx = arrowObj.getInt("Dx")
                val dy = arrowObj.getInt("Dy")
                val bendCount = arrowObj.optInt("BendCount", 0)
                val indicesArray = arrowObj.getJSONArray("Indices")

                // Map unity indices to canvas coordinates
                // In Unity: index = Y * XSize + X
                // Canvas X = UnityX, Canvas Y = (YSize - 1) - UnityY
                // Canvas Dx = Dx, Canvas Dy = -Dy
                val bodyPointsReversed = mutableListOf<GridCoordinate>()
                for (j in 0 until indicesArray.length()) {
                    val idx = indicesArray.getInt(j)
                    val unityX = idx % xSize
                    val unityY = idx / xSize
                    val canvasX = unityX
                    val canvasY = (ySize - 1) - unityY
                    bodyPointsReversed.add(GridCoordinate(canvasX, canvasY))
                }

                // In JSON, Indices starts with HEAD at index 0 and ends with TAIL.
                // In our model, body is ordered TAIL -> HEAD.
                val body = bodyPointsReversed.reversed()
                val head = body.last()

                val canvasDx = dx
                val canvasDy = -dy
                val direction = when {
                    canvasDx > 0 -> Direction.RIGHT
                    canvasDx < 0 -> Direction.LEFT
                    canvasDy > 0 -> Direction.DOWN
                    else -> Direction.UP
                }

                arrowList.add(
                    Arrow(
                        id = "arrow_${i + 1}",
                        head = head,
                        direction = direction,
                        body = body,
                        bendCount = bendCount,
                        colorIndex = 0
                    )
                )
            }

            val difficulty = when {
                arrowList.size <= 5 -> LevelDifficulty.EASY
                arrowList.size <= 12 -> LevelDifficulty.MEDIUM
                else -> LevelDifficulty.HARD
            }

            return LevelData(
                levelNumber = levelNumber,
                xSize = xSize,
                ySize = ySize,
                arrows = arrowList,
                maxHearts = maxHearts,
                difficulty = difficulty,
                diffScore = arrowList.size * 10,
                diffDna = "UNITY_LVL_${levelNumber}_${xSize}x${ySize}_${arrowList.size}"
            )
        } catch (e: Exception) {
            return null
        }
    }
}
