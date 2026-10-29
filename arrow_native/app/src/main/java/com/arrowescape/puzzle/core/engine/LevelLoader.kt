package com.arrowescape.puzzle.core.engine

import android.content.Context
import com.arrowescape.puzzle.core.model.Arrow
import com.arrowescape.puzzle.core.model.Direction
import com.arrowescape.puzzle.core.model.GridCoordinate
import com.arrowescape.puzzle.core.model.LevelData
import com.arrowescape.puzzle.core.model.LevelDifficulty
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

enum class TestLevelCategory(
    val title: String,
    val subtitle: String,
    val totalLevels: Int,
    val folder: String
) {
    EXISTING_1K(
        title = "Existing 1K Levels",
        subtitle = "1,001 Levels (Sorted Smallest to Largest)",
        totalLevels = 1001,
        folder = "levels/test_existing_1k"
    ),
    UNITY_NORMAL(
        title = "New Unity Normal Levels",
        subtitle = "8,445 Normal Levels (Sorted Smallest to Largest)",
        totalLevels = 8445,
        folder = "levels/test_unity_normal"
    ),
    SHAPE_PUZZLES(
        title = "Shape Puzzle Levels",
        subtitle = "788 Recognizable Shapes (Sorted Smallest to Largest)",
        totalLevels = 788,
        folder = "levels/test_shape_puzzles"
    ),
    LARGE_PUZZLES(
        title = "Large Puzzles",
        subtitle = "46 Mega Grids (250–437 Arrows)",
        totalLevels = 46,
        folder = "levels/test_large_puzzles"
    )
}

object LevelLoader {

    const val TOTAL_LEVELS = 2000
    const val CHUNK_SIZE = 1000

    // AES-256 Secret Key (32 bytes)
    private val AES_KEY = "AmazeGo_Arrow_Security_Key_2026!".toByteArray(Charsets.UTF_8)

    // In-memory cache of the currently active decrypted pack (1,000 levels)
    private var cachedPackIndex: Int = -1
    private var cachedPackLevels: JSONArray? = null

    // In-memory cache for daily challenge pack (300 levels)
    private var cachedDailyLevels: JSONArray? = null

    // In-memory cache for test category packs
    private var cachedTestCategory: TestLevelCategory? = null
    private var cachedTestPackIndex: Int = -1
    private var cachedTestPackLevels: JSONArray? = null

    /**
     * Loads an authentic level from the AES-256 encrypted binary packs (Levels 1 to 2,000).
     * Automatically handles chunk caching in memory for sub-millisecond level loads.
     */
    fun loadLevel(context: Context, levelNumber: Int): LevelData {
        val actualLevelNumber = ((levelNumber - 1) % TOTAL_LEVELS) + 1
        val packIndex = (actualLevelNumber - 1) / CHUNK_SIZE
        val indexInPack = (actualLevelNumber - 1) % CHUNK_SIZE

        try {
            val packArray = getOrLoadPack(context, packIndex)
            if (packArray != null && indexInPack < packArray.length()) {
                val levelJson = packArray.getJSONObject(indexInPack)
                val level = parseLevelJsonObject(actualLevelNumber, levelJson)
                if (level != null && level.arrows.isNotEmpty()) {
                    return level
                }
            }
        } catch (e: Exception) {
            // Fallback handled below
        }

        return LevelGenerator.generateProceduralLevel(levelNumber, levelNumber.toLong())
    }

    /**
     * Loads a daily level from the encrypted daily_pack.dat.
     */
    fun loadDailyLevel(context: Context, dailyIndex: Int): LevelData {
        val actualIndex = ((dailyIndex - 1) % 300)
        try {
            val dailyArray = getOrLoadDailyPack(context)
            if (dailyArray != null && actualIndex < dailyArray.length()) {
                val levelJson = dailyArray.getJSONObject(actualIndex)
                val level = parseLevelJsonObject(dailyIndex, levelJson)
                if (level != null && level.arrows.isNotEmpty()) {
                    return level
                }
            }
        } catch (e: Exception) {
            // Fall back to standard level
        }

        return loadLevel(context, dailyIndex)
    }

    /**
     * Loads a test level from the dedicated test categories:
     * - Existing 1K Levels (1,001 levels sorted smallest to largest)
     * - New Unity Normal Levels (6,743 levels sorted smallest to largest)
     * - Shape Puzzle Levels (15,427 levels sorted smallest to largest)
     */
    fun loadTestLevel(context: Context, category: TestLevelCategory, levelNumber: Int): LevelData {
        val boundedLevel = ((levelNumber - 1) % category.totalLevels) + 1
        val packIndex = (boundedLevel - 1) / CHUNK_SIZE
        val indexInPack = (boundedLevel - 1) % CHUNK_SIZE

        try {
            val packArray = getOrLoadTestPack(context, category, packIndex)
            if (packArray != null && indexInPack < packArray.length()) {
                val levelJson = packArray.getJSONObject(indexInPack)
                val level = parseLevelJsonObject(boundedLevel, levelJson)
                if (level != null && level.arrows.isNotEmpty()) {
                    return level
                }
            }
        } catch (e: Exception) {
            // Fallback handled below
        }

        return LevelGenerator.generateProceduralLevel(levelNumber, levelNumber.toLong())
    }

    @Synchronized
    private fun getOrLoadTestPack(context: Context, category: TestLevelCategory, packIndex: Int): JSONArray? {
        if (cachedTestCategory == category && cachedTestPackIndex == packIndex && cachedTestPackLevels != null) {
            return cachedTestPackLevels
        }

        val fileName = String.format("%s/level_pack_%02d.dat", category.folder, packIndex)
        return try {
            context.assets.open(fileName).use { inputStream ->
                val jsonStr = decryptAndDecompress(inputStream)
                val array = JSONArray(jsonStr)
                cachedTestCategory = category
                cachedTestPackIndex = packIndex
                cachedTestPackLevels = array
                array
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Unified interface: loads from the authentic master sequence (1 to 14,040).
     */
    fun loadNewLevel(context: Context, levelNumber: Int): LevelData {
        return loadLevel(context, levelNumber)
    }

    @Synchronized
    private fun getOrLoadPack(context: Context, packIndex: Int): JSONArray? {
        if (cachedPackIndex == packIndex && cachedPackLevels != null) {
            return cachedPackLevels
        }

        val fileName = String.format("levels/level_pack_%02d.dat", packIndex)
        return try {
            context.assets.open(fileName).use { inputStream ->
                val jsonStr = decryptAndDecompress(inputStream)
                val array = JSONArray(jsonStr)
                cachedPackIndex = packIndex
                cachedPackLevels = array
                array
            }
        } catch (e: Exception) {
            null
        }
    }

    @Synchronized
    private fun getOrLoadDailyPack(context: Context): JSONArray? {
        if (cachedDailyLevels != null) {
            return cachedDailyLevels
        }

        return try {
            context.assets.open("levels/daily_pack.dat").use { inputStream ->
                val jsonStr = decryptAndDecompress(inputStream)
                val array = JSONArray(jsonStr)
                cachedDailyLevels = array
                array
            }
        } catch (e: Exception) {
            null
        }
    }

    fun decryptAndDecompress(inputStream: InputStream): String {
        val iv = ByteArray(16)
        var bytesRead = 0
        while (bytesRead < 16) {
            val r = inputStream.read(iv, bytesRead, 16 - bytesRead)
            if (r == -1) break
            bytesRead += r
        }
        val ciphertext = inputStream.readBytes()

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(AES_KEY, "AES")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decrypted = cipher.doFinal(ciphertext)

        val gzipIn = GZIPInputStream(ByteArrayInputStream(decrypted))
        return gzipIn.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    fun parseLevelJson(levelNumber: Int, jsonStr: String): LevelData? {
        return try {
            parseLevelJsonObject(levelNumber, JSONObject(jsonStr))
        } catch (e: Exception) {
            null
        }
    }

    fun parseLevelJsonObject(levelNumber: Int, root: JSONObject): LevelData? {
        try {
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

            val hash = root.optString("Hash", "")
            val shapeName = root.optString("ShapeName", "")
            val dna = when {
                shapeName.isNotEmpty() -> shapeName
                hash.isNotEmpty() -> hash
                else -> "AMZ_LVL_${levelNumber}_${xSize}x${ySize}_${arrowList.size}"
            }

            return LevelData(
                levelNumber = levelNumber,
                xSize = xSize,
                ySize = ySize,
                arrows = arrowList,
                maxHearts = maxHearts,
                difficulty = difficulty,
                diffScore = arrowList.size * 10,
                diffDna = dna,
                shapeName = shapeName
            )
        } catch (e: Exception) {
            return null
        }
    }
}
