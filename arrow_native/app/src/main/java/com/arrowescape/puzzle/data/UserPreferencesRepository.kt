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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_game_prefs")

data class UserGameData(
    val currentLevel: Int = 1,
    val coins: Int = 100,
    val freeHints: Int = 3,
    val freeHammers: Int = 2,
    val freeWands: Int = 2,
    val streakDays: Int = 1,
    val lastDailyDate: String = "",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val themeIndex: Int = 0
)

class UserPreferencesRepository(private val context: Context) {

    private val KEY_LEVEL = intPreferencesKey("current_level")
    private val KEY_COINS = intPreferencesKey("coins_balance")
    private val KEY_HINTS = intPreferencesKey("hints_count")
    private val KEY_HAMMERS = intPreferencesKey("hammers_count")
    private val KEY_WANDS = intPreferencesKey("wands_count")
    private val KEY_STREAK = intPreferencesKey("streak_days")
    private val KEY_LAST_DAILY = stringPreferencesKey("last_daily_date")
    private val KEY_SOUND = booleanPreferencesKey("sound_enabled")
    private val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
    private val KEY_THEME = intPreferencesKey("selected_theme_index")

    val userGameData: Flow<UserGameData> = context.dataStore.data.map { prefs ->
        UserGameData(
            currentLevel = prefs[KEY_LEVEL] ?: 1,
            coins = prefs[KEY_COINS] ?: 100,
            freeHints = prefs[KEY_HINTS] ?: 3,
            freeHammers = prefs[KEY_HAMMERS] ?: 2,
            freeWands = prefs[KEY_WANDS] ?: 2,
            streakDays = prefs[KEY_STREAK] ?: 1,
            lastDailyDate = prefs[KEY_LAST_DAILY] ?: "",
            soundEnabled = prefs[KEY_SOUND] ?: true,
            vibrationEnabled = prefs[KEY_VIBRATION] ?: true,
            themeIndex = prefs[KEY_THEME] ?: 0
        )
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
                val count = prefs[key] ?: 2
                if (count > 0) {
                    prefs[key] = count - 1
                    success = true
                } else {
                    // Try to spend coins if booster is 0
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
            prefs[KEY_LAST_DAILY] = dateString
            val currentStreak = prefs[KEY_STREAK] ?: 0
            prefs[KEY_STREAK] = currentStreak + 1
        }
    }
}
