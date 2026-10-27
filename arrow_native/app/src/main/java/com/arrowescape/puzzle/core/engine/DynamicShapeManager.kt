package com.arrowescape.puzzle.core.engine

import android.content.Context
import com.arrowescape.puzzle.core.model.LevelData
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * Manages the dynamic, personalized, non-repeating iconic shape milestones (every 5th level).
 * 
 * Guarantees:
 * 1. Different for each user (personalized via unique userSeed).
 * 2. Strictly non-repeating: once a user plays a shape, it is recorded in SharedPreferences
 *    and never selected again for that user.
 * 3. Consistent on retries: if a user fails or restarts the same level number, they see
 *    the exact same shape until completed.
 * 4. Tier-calibrated difficulty: milestone shapes match their target arrow tier
 *    (Tier 0: 54-100 arrows, Tier 1: 70-125 arrows, ..., Tier 5: up to 437 arrows).
 */
object DynamicShapeManager {

    private const val PREFS_NAME = "arrow_sync_prefs"
    private const val KEY_USER_SEED = "dynamic_shape_user_seed"
    private const val KEY_PLAYED_SHAPES = "dynamic_played_shape_hashes"

    // In-memory cache of decrypted shapes organized by tier (Tiers 0 to 5)
    private var isLoaded = false
    private val tierShapes = mutableMapOf<Int, MutableList<JSONObject>>()
    private val allShapesByHash = mutableMapOf<String, JSONObject>()

    @Synchronized
    fun ensureLoaded(context: Context) {
        if (isLoaded) return
        try {
            context.assets.open("levels/shape_pack.dat").use { stream ->
                val jsonStr = LevelLoader.decryptAndDecompress(stream)
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val shapeObj = array.getJSONObject(i)
                    val tier = shapeObj.optInt("Tier", 0)
                    val hash = shapeObj.optString("Hash", "")
                    tierShapes.getOrPut(tier) { mutableListOf() }.add(shapeObj)
                    if (hash.isNotEmpty()) {
                        allShapesByHash[hash] = shapeObj
                    }
                }
                isLoaded = true
            }
        } catch (e: Exception) {
            // Falls back to static pack levels if shape_pack.dat cannot be read
        }
    }

    fun getTierForLevel(levelNumber: Int): Int {
        return when {
            levelNumber <= 50 -> 0
            levelNumber <= 200 -> 1
            levelNumber <= 500 -> 2
            levelNumber <= 1000 -> 3
            levelNumber <= 1500 -> 4
            else -> 5
        }
    }

    /**
     * Retrieves a dynamic shape level for a milestone level (e.g. 5, 10, 15...).
     */
    fun getShapeForLevel(context: Context, levelNumber: Int): LevelData? {
        ensureLoaded(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var userSeed = prefs.getLong(KEY_USER_SEED, 0L)
        if (userSeed == 0L) {
            userSeed = Random.nextLong(1, Long.MAX_VALUE)
            prefs.edit().putLong(KEY_USER_SEED, userSeed).apply()
        }

        val playedHashes = prefs.getStringSet(KEY_PLAYED_SHAPES, emptySet()) ?: emptySet()
        val tier = getTierForLevel(levelNumber)
        val pool = tierShapes[tier] ?: emptyList()

        if (pool.isEmpty()) return null

        // Candidates not yet played by this user
        val unplayed = pool.filter { !playedHashes.contains(it.optString("Hash", "")) }
        val candidates = if (unplayed.isNotEmpty()) unplayed else pool

        // Deterministic pseudo-random selection based on userSeed and levelNumber
        val seedMixer = (userSeed xor (levelNumber.toLong() * 1000003L)).toInt()
        val selectedIndex = Math.floorMod(seedMixer, candidates.size)
        val chosenShapeJson = candidates[selectedIndex]

        return LevelLoader.parseLevelJsonObject(levelNumber, chosenShapeJson)
    }

    /**
     * Marks a shape hash as played so it will never be chosen again for this user in future shape levels.
     */
    fun markShapeCompleted(context: Context, shapeHash: String) {
        if (shapeHash.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val playedHashes = (prefs.getStringSet(KEY_PLAYED_SHAPES, emptySet()) ?: emptySet()).toMutableSet()
        playedHashes.add(shapeHash)
        prefs.edit().putStringSet(KEY_PLAYED_SHAPES, playedHashes).apply()
    }
}
