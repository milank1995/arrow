package com.arrowescape.puzzle.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_game_prefs")

data class UserGameData(
    val currentLevel: Int = 1,
    val coins: Int = 100,
    val freeHints: Int = 3,
    val freeHammers: Int = 2,
    val freeWands: Int = 2,
    val streakDays: Int = 1,
    val lastDailyDate: String = "",
    val completedDailyDates: Set<String> = emptySet(),
    val levelStars: Map<Int, Int> = emptyMap(),
    val hasPlayedBefore: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val themeIndex: Int = 0,
    val globalLives: Int = 6
)

class UserPreferencesRepository(private val context: Context) {

    private val KEY_LEVEL = intPreferencesKey("current_level")
    private val KEY_COINS = intPreferencesKey("coins_balance")
    private val KEY_HINTS = intPreferencesKey("hints_count")
    private val KEY_HAMMERS = intPreferencesKey("hammers_count")
    private val KEY_WANDS = intPreferencesKey("wands_count")
    private val KEY_STREAK = intPreferencesKey("streak_days")
    private val KEY_LAST_DAILY = stringPreferencesKey("last_daily_date")
    private val KEY_COMPLETED_DAILY_DATES = stringSetPreferencesKey("completed_daily_dates")
    private val KEY_LEVEL_STARS_MAP = stringPreferencesKey("level_stars_map")
    private val KEY_HAS_PLAYED_BEFORE = booleanPreferencesKey("has_played_before")
    private val KEY_SOUND = booleanPreferencesKey("sound_enabled")
    private val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
    private val KEY_THEME = intPreferencesKey("selected_theme_index")
    private val KEY_GLOBAL_LIVES = intPreferencesKey("global_lives_bank")

    private val SYNC_PREFS = "arrow_sync_prefs"
    private val KEY_SYNC_HAS_PLAYED = "has_played_before"

    fun hasPlayedBeforeSynchronous(): Boolean {
        val sp = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_SYNC_HAS_PLAYED, false)
    }

    val userGameData: Flow<UserGameData> = context.dataStore.data.map { prefs ->
        val syncPlayed = hasPlayedBeforeSynchronous()
        val hasPlayed = syncPlayed || (prefs[KEY_HAS_PLAYED_BEFORE] ?: ((prefs[KEY_LEVEL] ?: 1) > 1 || (prefs[KEY_COINS] != null && (prefs[KEY_COINS] ?: 100) != 100)))
        if (hasPlayed && !syncPlayed) {
            context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SYNC_HAS_PLAYED, true).apply()
        }
        UserGameData(
            currentLevel = prefs[KEY_LEVEL] ?: 1,
            coins = prefs[KEY_COINS] ?: 100,
            freeHints = prefs[KEY_HINTS] ?: 3,
            freeHammers = prefs[KEY_HAMMERS] ?: 2,
            freeWands = prefs[KEY_WANDS] ?: 2,
            streakDays = prefs[KEY_STREAK] ?: 1,
            lastDailyDate = prefs[KEY_LAST_DAILY] ?: "",
            completedDailyDates = prefs[KEY_COMPLETED_DAILY_DATES] ?: emptySet(),
            levelStars = parseLevelStars(prefs[KEY_LEVEL_STARS_MAP] ?: ""),
            hasPlayedBefore = hasPlayed,
            soundEnabled = prefs[KEY_SOUND] ?: true,
            vibrationEnabled = prefs[KEY_VIBRATION] ?: true,
            themeIndex = prefs[KEY_THEME] ?: 0,
            globalLives = prefs[KEY_GLOBAL_LIVES] ?: 6
        )
    }

    private fun parseLevelStars(encoded: String): Map<Int, Int> {
        if (encoded.isBlank()) return emptyMap()
        val map = mutableMapOf<Int, Int>()
        encoded.split(";").forEach { pair ->
            val parts = pair.split(":")
            if (parts.size == 2) {
                val lvl = parts[0].toIntOrNull()
                val stars = parts[1].toIntOrNull()
                if (lvl != null && stars != null) {
                    map[lvl] = stars
                }
            }
        }
        return map
    }

    private fun encodeLevelStars(map: Map<Int, Int>): String {
        return map.entries.joinToString(";") { "${it.key}:${it.value}" }
    }

    suspend fun recordLevelCompletion(level: Int, starsEarned: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_LEVEL] ?: 1
            if (level >= current) {
                prefs[KEY_LEVEL] = level + 1
            }
            val starsMap = parseLevelStars(prefs[KEY_LEVEL_STARS_MAP] ?: "").toMutableMap()
            val existing = starsMap[level] ?: 0
            if (starsEarned > existing) {
                starsMap[level] = starsEarned
                prefs[KEY_LEVEL_STARS_MAP] = encodeLevelStars(starsMap)
            }
        }
    }

    suspend fun updateLevel(level: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_LEVEL] ?: 1
            if (level > current) {
                prefs[KEY_LEVEL] = level
            }
        }
    }

    suspend fun addCoins(amount: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_COINS] ?: 100
            prefs[KEY_COINS] = current + amount
        }
    }

    suspend fun useBooster(booster: String): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val key = when (booster) {
                "hint" -> KEY_HINTS
                "hammer" -> KEY_HAMMERS
                "wand" -> KEY_WANDS
                else -> null
            }
            if (key != null) {
                val count = prefs[key] ?: if (booster == "hint") 3 else 2
                if (count > 0) {
                    prefs[key] = count - 1
                    success = true
                } else if (booster != "hint") {
                    // Try to spend coins if hammer/wand booster is 0
                    val coins = prefs[KEY_COINS] ?: 100
                    if (coins >= 50) {
                        prefs[KEY_COINS] = coins - 50
                        success = true
                    }
                }
            }
        }
        return success
    }

    suspend fun addHints(amount: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_HINTS] ?: 0
            prefs[KEY_HINTS] = current + amount
        }
    }

    suspend fun setTheme(index: Int) {
        context.dataStore.edit { it[KEY_THEME] = index }
    }

    suspend fun toggleSound() {
        context.dataStore.edit { it[KEY_SOUND] = !(it[KEY_SOUND] ?: true) }
    }

    suspend fun toggleVibration() {
        context.dataStore.edit { it[KEY_VIBRATION] = !(it[KEY_VIBRATION] ?: true) }
    }

    suspend fun recordDailyCompletion(dateString: String) {
        context.dataStore.edit { prefs ->
            val completed = (prefs[KEY_COMPLETED_DAILY_DATES] ?: emptySet()).toMutableSet()
            val isFirstTimeForDate = completed.add(dateString)
            prefs[KEY_COMPLETED_DAILY_DATES] = completed

            // Unity DailyCompleteReward = 100 coins
            if (isFirstTimeForDate) {
                val currentCoins = prefs[KEY_COINS] ?: 100
                prefs[KEY_COINS] = currentCoins + 100

                val lastDateStr = prefs[KEY_LAST_DAILY] ?: ""
                val currentStreak = prefs[KEY_STREAK] ?: 0
                val newStreak = calculateStreak(lastDateStr, dateString, currentStreak)
                prefs[KEY_STREAK] = newStreak
                prefs[KEY_LAST_DAILY] = dateString
            }
        }
    }

    private fun calculateStreak(lastDateStr: String, currentDateStr: String, currentStreak: Int): Int {
        if (lastDateStr.isEmpty()) return 1
        return try {
            val lastDate = LocalDate.parse(lastDateStr)
            val currentDate = LocalDate.parse(currentDateStr)
            val diffDays = ChronoUnit.DAYS.between(lastDate, currentDate)
            when (diffDays) {
                0L -> currentStreak.coerceAtLeast(1)
                1L -> currentStreak + 1
                else -> 1
            }
        } catch (e: Exception) {
            currentStreak + 1
        }
    }

    suspend fun markHasPlayedBefore() {
        context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SYNC_HAS_PLAYED, true).apply()
        context.dataStore.edit { it[KEY_HAS_PLAYED_BEFORE] = true }
    }

    suspend fun useGlobalLives(amount: Int = 3): Boolean {
        var used = false
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_GLOBAL_LIVES] ?: 6
            if (current >= amount) {
                prefs[KEY_GLOBAL_LIVES] = current - amount
                used = true
            } else {
                used = false
            }
        }
        return used
    }
}
