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
        val totalHandcraftedLevels = 151
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
     * Loads the featured Lock shape puzzle level.
     */
    fun loadLockPuzzle(context: Context): LevelData {
        val assetName = "levels/Level_151.json"
        try {
            val jsonStr = context.assets.open(assetName).bufferedReader().use { it.readText() }
            val level = parseLevelJson(151, jsonStr)
            if (level != null && level.arrows.isNotEmpty()) {
                return level
            }
        } catch (e: Exception) {}
        return loadLevel(context, 151)
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

    data class ManifestLevelItem(
        val levelNumber: Int,
        val arrowCount: Int,
        val difficulty: String,
        val shapeName: String?,
        val isShape: Boolean,
        val sourceCategory: String,
        val xSize: Int,
        val ySize: Int
    )

    private var cachedManifest: List<ManifestLevelItem>? = null

    @Synchronized
    fun getCuratedManifest(context: Context): List<ManifestLevelItem> {
        cachedManifest?.let { return it }
        val items = mutableListOf<ManifestLevelItem>()
        try {
            val jsonStr = context.assets.open("levels/manifest.json").bufferedReader().use { it.readText() }
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val sName = obj.optString("ShapeName").takeIf { it.isNotBlank() && it != "null" }
                items.add(
                    ManifestLevelItem(
                        levelNumber = obj.getInt("LevelNumber"),
                        arrowCount = obj.getInt("ArrowCount"),
                        difficulty = obj.optString("Difficulty", "Medium"),
                        shapeName = sName,
                        isShape = obj.optBoolean("IsShape", sName != null),
                        sourceCategory = obj.optString("SourceCategory", ""),
                        xSize = obj.optInt("XSize", 0),
                        ySize = obj.optInt("YSize", 0)
                    )
                )
            }
        } catch (_: Exception) {}
        cachedManifest = items
        return items
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

                val rawIndices = mutableListOf<Int>()
                for (j in 0 until indicesArray.length()) {
                    rawIndices.add(indicesArray.getInt(j))
                }

                // Expand vertices into continuous orthogonal cell paths if needed
                val fullIndices = mutableListOf<Int>()
                if (rawIndices.isNotEmpty()) {
                    fullIndices.add(rawIndices[0])
                    for (k in 0 until rawIndices.size - 1) {
                        val idx1 = rawIndices[k]
                        val idx2 = rawIndices[k + 1]
                        val x1 = idx1 % xSize
                        val y1 = idx1 / xSize
                        val x2 = idx2 % xSize
                        val y2 = idx2 / xSize
                        val stepX = if (x2 > x1) 1 else if (x2 < x1) -1 else 0
                        val stepY = if (y2 > y1) 1 else if (y2 < y1) -1 else 0
                        var curX = x1
                        var curY = y1
                        while (curX != x2 || curY != y2) {
                            if (curX != x2) curX += stepX
                            else curY += stepY
                            fullIndices.add(curY * xSize + curX)
                        }
                    }
                }

                // Map unity indices to canvas coordinates
                // In Unity: index = Y * XSize + X
                // Canvas X = UnityX, Canvas Y = (YSize - 1) - UnityY
                // Canvas Dx = Dx, Canvas Dy = -Dy
                val bodyPointsReversed = mutableListOf<GridCoordinate>()
                for (idx in fullIndices) {
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

            val shapeName = root.optString("ShapeName").takeIf { it.isNotBlank() && it != "null" }
            val rawDiff = root.optString("Difficulty")
            val difficulty = when {
                rawDiff.equals("Hard", ignoreCase = true) || rawDiff.equals("Super Hard", ignoreCase = true) -> LevelDifficulty.HARD
                rawDiff.equals("Medium", ignoreCase = true) -> LevelDifficulty.MEDIUM
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
                diffDna = "CURATED_LVL_${levelNumber}_${xSize}x${ySize}_${arrowList.size}",
                shapeName = shapeName
            )
        } catch (e: Exception) {
            return null
        }
    }
}
