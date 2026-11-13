package com.arrowaway.puzzle.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
    val freeHints: Int = 0,
    val freeHammers: Int = 2,
    val freeWands: Int = 2,
    val streakDays: Int = 0,
    val lastDailyDate: String = "",
    val lastStreakDate: String = "",
    val completedDailyDates: Set<String> = emptySet(),
    val levelStars: Map<Int, Int> = emptyMap(),
    val hasPlayedBefore: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val themeIndex: Int = 0,
    val globalLives: Int = 6,
    val hasSeenDailyUnlockCelebration: Boolean = false,
    val hasSeenHintAdIntro: Boolean = false,
    val hasSeenDailyHintAdIntro: Boolean = false,
    val dailyRemindersEnabled: Boolean = true,
    val hasRequestedNotificationPermission: Boolean = false,
    val notificationPermissionAskCount: Int = 0,
    val isDevMode: Boolean = com.arrowaway.puzzle.BuildConfig.DEBUG,
    val isHintButtonVisible: Boolean = true,
    val isGridButtonVisible: Boolean = true,
    val hintTimerSeconds: Int = 10,
    val lastReviewPromptTimestamp: Long = 0L,
    val isAnalyticsEnabled: Boolean = !com.arrowaway.puzzle.BuildConfig.DEBUG,
    val isHandGestureEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private val KEY_LEVEL = intPreferencesKey("current_level")
    private val KEY_COINS = intPreferencesKey("coins_balance")
    private val KEY_HINTS = intPreferencesKey("hints_count")
    private val KEY_CLAIMED_MILESTONES = stringSetPreferencesKey("claimed_milestone_levels")
    private val KEY_HAMMERS = intPreferencesKey("hammers_count")
    private val KEY_WANDS = intPreferencesKey("wands_count")
    private val KEY_STREAK = intPreferencesKey("streak_days")
    private val KEY_LAST_DAILY = stringPreferencesKey("last_daily_date")
    private val KEY_LAST_STREAK_DATE = stringPreferencesKey("last_streak_date")
    private val KEY_COMPLETED_DAILY_DATES = stringSetPreferencesKey("completed_daily_dates")
    private val KEY_LEVEL_STARS_MAP = stringPreferencesKey("level_stars_map")
    private val KEY_HAS_PLAYED_BEFORE = booleanPreferencesKey("has_played_before")
    private val KEY_SOUND = booleanPreferencesKey("sound_enabled")
    private val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
    private val KEY_THEME = intPreferencesKey("selected_theme_index")
    private val KEY_GLOBAL_LIVES = intPreferencesKey("global_lives_bank")
    private val KEY_DAILY_UNLOCK_SEEN = booleanPreferencesKey("daily_unlock_celebration_seen")
    private val KEY_HINT_AD_INTRO_SEEN = booleanPreferencesKey("hint_ad_intro_seen")
    private val KEY_NO_FILL_FREE_HINT_DATE = stringPreferencesKey("no_fill_free_hint_date")
    private val KEY_DAILY_HINT_AD_INTRO_SEEN = booleanPreferencesKey("daily_hint_ad_intro_seen")
    private val KEY_DAILY_REMINDERS_ENABLED = booleanPreferencesKey("daily_reminders_enabled")
    private val KEY_NOTIFICATION_PERMISSION_REQUESTED = booleanPreferencesKey("has_requested_notification_permission")
    private val KEY_NOTIFICATION_ASK_COUNT = intPreferencesKey("notification_permission_ask_count")
    private val KEY_DEV_MODE = booleanPreferencesKey("is_dev_mode_enabled")
    private val KEY_HINT_BUTTON_VISIBLE = booleanPreferencesKey("hint_button_visible")
    private val KEY_GRID_BUTTON_VISIBLE = booleanPreferencesKey("grid_button_visible")
    private val KEY_HINT_TIMER_SECONDS = intPreferencesKey("hint_timer_seconds")
    private val KEY_LAST_REVIEW_PROMPT_TIMESTAMP = longPreferencesKey("last_review_prompt_timestamp")
    private val KEY_ANALYTICS_ENABLED = booleanPreferencesKey("analytics_tracking_enabled")
    private val KEY_HAND_GESTURE_ENABLED = booleanPreferencesKey("is_hand_gesture_enabled")

    private val SYNC_PREFS = "arrow_sync_prefs"
    private val KEY_SYNC_HAS_PLAYED = "has_played_before"

    fun hasPlayedBeforeSynchronous(): Boolean {
        val sp = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_SYNC_HAS_PLAYED, false)
    }

    fun isDailyCompletedTodaySynchronous(todayDate: String): Boolean {
        val sp = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
        val set = sp.getStringSet("completed_dates", emptySet()) ?: emptySet()
        return set.contains(todayDate)
    }

    fun isDailyRemindersEnabledSynchronous(): Boolean {
        val sp = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
        return sp.getBoolean("daily_reminders_enabled", true)
    }

    fun getCurrentLevelSynchronous(): Int {
        val sp = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
        return sp.getInt("current_level", 1)
    }

    fun getStreakDaysSynchronous(): Int {
        val sp = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
        val savedStreak = sp.getInt("streak_days", 0)
        val lastDateStr = sp.getString("last_streak_date", null)
            ?: sp.getString("last_daily_date", null)
        val hasPlayed = sp.getBoolean(KEY_SYNC_HAS_PLAYED, false)
        if (lastDateStr.isNullOrEmpty()) {
            return if (hasPlayed && savedStreak > 0) savedStreak else 0
        }
        return try {
            val lastDate = LocalDate.parse(lastDateStr)
            val today = LocalDate.now()
            val diff = ChronoUnit.DAYS.between(lastDate, today)
            if (diff >= 2L) 0 else savedStreak
        } catch (e: Exception) {
            savedStreak
        }
    }

    fun isStreakClaimedTodaySynchronous(): Boolean {
        val sp = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
        val lastDateStr = sp.getString("last_streak_date", null)
        val today = LocalDate.now().toString()
        return lastDateStr == today
    }

    fun getLastReviewPromptTimestampSynchronous(): Long {
        val sp = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
        return sp.getLong("last_review_prompt_timestamp", 0L)
    }

    suspend fun recordReviewPromptShown(timestamp: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
            .edit().putLong("last_review_prompt_timestamp", timestamp).apply()
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_REVIEW_PROMPT_TIMESTAMP] = timestamp
        }
    }

    private fun calculateDisplayStreak(lastDateStr: String, todayStr: String, savedStreak: Int, hasPlayed: Boolean): Int {
        if (lastDateStr.isEmpty()) {
            return if (hasPlayed && savedStreak > 0) savedStreak else 0
        }
        return try {
            val lastDate = LocalDate.parse(lastDateStr)
            val today = LocalDate.parse(todayStr)
            val diffDays = ChronoUnit.DAYS.between(lastDate, today)
            when {
                diffDays <= 1L -> savedStreak
                else -> 0
            }
        } catch (e: Exception) {
            savedStreak
        }
    }

    val userGameData: Flow<UserGameData> = context.dataStore.data.map { prefs ->
        val syncPlayed = hasPlayedBeforeSynchronous()
        val hasPlayed = syncPlayed || (prefs[KEY_HAS_PLAYED_BEFORE] ?: ((prefs[KEY_LEVEL] ?: 1) > 1 || (prefs[KEY_COINS] != null && (prefs[KEY_COINS] ?: 100) != 100)))
        if (hasPlayed && !syncPlayed) {
            context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SYNC_HAS_PLAYED, true).apply()
        }

        val todayStr = LocalDate.now().toString()
        val lastStreakDate = prefs[KEY_LAST_STREAK_DATE] ?: prefs[KEY_LAST_DAILY] ?: ""
        val rawStreak = prefs[KEY_STREAK] ?: 0
        val displayStreak = calculateDisplayStreak(lastStreakDate, todayStr, rawStreak, hasPlayed)

        val rawLevel = prefs[KEY_LEVEL] ?: 1
        val starsMap = parseLevelStars(prefs[KEY_LEVEL_STARS_MAP] ?: "")
        val maxCompletedLevel = starsMap.keys.maxOrNull() ?: 0
        val sanitizedLevel = rawLevel.coerceIn(1, (maxCompletedLevel + 1).coerceAtLeast(1))
        val rawCoins = prefs[KEY_COINS] ?: 100
        val sanitizedCoins = rawCoins.coerceIn(0, 50_000)

        UserGameData(
            currentLevel = sanitizedLevel,
            coins = sanitizedCoins,
            freeHints = prefs[KEY_HINTS] ?: 0,
            freeHammers = prefs[KEY_HAMMERS] ?: 2,
            freeWands = prefs[KEY_WANDS] ?: 2,
            streakDays = displayStreak,
            lastDailyDate = prefs[KEY_LAST_DAILY] ?: "",
            lastStreakDate = lastStreakDate,
            completedDailyDates = prefs[KEY_COMPLETED_DAILY_DATES] ?: emptySet(),
            levelStars = parseLevelStars(prefs[KEY_LEVEL_STARS_MAP] ?: ""),
            hasPlayedBefore = hasPlayed,
            soundEnabled = prefs[KEY_SOUND] ?: true,
            vibrationEnabled = prefs[KEY_VIBRATION] ?: true,
            themeIndex = prefs[KEY_THEME] ?: 0,
            globalLives = prefs[KEY_GLOBAL_LIVES] ?: 6,
            hasSeenDailyUnlockCelebration = prefs[KEY_DAILY_UNLOCK_SEEN] ?: false,
            hasSeenHintAdIntro = prefs[KEY_HINT_AD_INTRO_SEEN] ?: false,
            hasSeenDailyHintAdIntro = prefs[KEY_DAILY_HINT_AD_INTRO_SEEN] ?: false,
            dailyRemindersEnabled = prefs[KEY_DAILY_REMINDERS_ENABLED] ?: true,
            hasRequestedNotificationPermission = prefs[KEY_NOTIFICATION_PERMISSION_REQUESTED] ?: false,
            notificationPermissionAskCount = prefs[KEY_NOTIFICATION_ASK_COUNT] ?: 0,
            isDevMode = if (com.arrowaway.puzzle.BuildConfig.DEBUG) (prefs[KEY_DEV_MODE] ?: true) else false,
            isHintButtonVisible = prefs[KEY_HINT_BUTTON_VISIBLE] ?: true,
            isGridButtonVisible = prefs[KEY_GRID_BUTTON_VISIBLE] ?: true,
            hintTimerSeconds = (prefs[KEY_HINT_TIMER_SECONDS] ?: 10).coerceIn(0, 55),
            lastReviewPromptTimestamp = prefs[KEY_LAST_REVIEW_PROMPT_TIMESTAMP] ?: 0L,
            isAnalyticsEnabled = prefs[KEY_ANALYTICS_ENABLED] ?: (!com.arrowaway.puzzle.BuildConfig.DEBUG),
            isHandGestureEnabled = prefs[KEY_HAND_GESTURE_ENABLED] ?: true
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
        context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
            .edit().putInt("current_level", level + 1).apply()
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
        context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
            .edit().putInt("current_level", level).apply()
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
                val count = prefs[key] ?: if (booster == "hint") 0 else 2
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

    /**
     * Grants the recurring milestone reward for level milestones (5, 10, 15...).
     * The check and grant happen atomically in DataStore so rewards cannot be double-claimed.
     */
    suspend fun claimMilestoneReward(levelNumber: Int, hints: Int = 1): Boolean {
        var granted = false
        context.dataStore.edit { prefs ->
            val claimed = prefs[KEY_CLAIMED_MILESTONES] ?: emptySet()
            val key = levelNumber.toString()
            if (!claimed.contains(key)) {
                prefs[KEY_CLAIMED_MILESTONES] = claimed + key
                prefs[KEY_HINTS] = (prefs[KEY_HINTS] ?: 0) + hints
                granted = true
            }
        }
        return granted
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

    suspend fun recordStreakOnLevelCompletion(): Pair<Boolean, Int> {
        val today = LocalDate.now().toString()
        var isClaimedToday = false
        var resultingStreak = 1

        context.dataStore.edit { prefs ->
            val lastDateStr = prefs[KEY_LAST_STREAK_DATE] ?: prefs[KEY_LAST_DAILY] ?: ""
            val currentStreak = prefs[KEY_STREAK] ?: 0

            if (lastDateStr == today) {
                // Already claimed today
                isClaimedToday = false
                resultingStreak = currentStreak.coerceAtLeast(1)
            } else {
                // First level completed today!
                isClaimedToday = true
                resultingStreak = if (lastDateStr.isEmpty()) {
                    1
                } else {
                    try {
                        val lastDate = LocalDate.parse(lastDateStr)
                        val diff = ChronoUnit.DAYS.between(lastDate, LocalDate.parse(today))
                        if (diff == 1L) {
                            currentStreak + 1
                        } else {
                            1
                        }
                    } catch (e: Exception) {
                        1
                    }
                }
                prefs[KEY_STREAK] = resultingStreak
                prefs[KEY_LAST_STREAK_DATE] = today
            }
        }

        context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE).edit()
            .putInt("streak_days", resultingStreak)
            .putString("last_streak_date", today)
            .apply()

        return Pair(isClaimedToday, resultingStreak)
    }

    suspend fun recordDailyCompletion(dateString: String): Boolean {
        context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE).let { sp ->
            val set = (sp.getStringSet("completed_dates", emptySet()) ?: emptySet()).toMutableSet()
            set.add(dateString)
            sp.edit().putStringSet("completed_dates", set).apply()
        }
        var isFirstTimeForDate = false
        context.dataStore.edit { prefs ->
            val completed = (prefs[KEY_COMPLETED_DAILY_DATES] ?: emptySet()).toMutableSet()
            isFirstTimeForDate = completed.add(dateString)
            prefs[KEY_COMPLETED_DAILY_DATES] = completed
        }
        return isFirstTimeForDate
    }

    suspend fun markDailyUnlockCelebrationSeen() {
        context.dataStore.edit { prefs ->
            prefs[KEY_DAILY_UNLOCK_SEEN] = true
        }
    }

    /**
     * The "no ad available" free hint: at most once per calendar day. Check and claim happen in
     * one DataStore transaction, so it can never be granted twice on the same day.
     */
    suspend fun claimNoFillFreeHint(today: String): Boolean {
        var granted = false
        context.dataStore.edit { prefs ->
            if (prefs[KEY_NO_FILL_FREE_HINT_DATE] != today) {
                prefs[KEY_NO_FILL_FREE_HINT_DATE] = today
                granted = true
            }
        }
        return granted
    }

    suspend fun markHintAdIntroSeen(isDailyChallenge: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[if (isDailyChallenge) KEY_DAILY_HINT_AD_INTRO_SEEN else KEY_HINT_AD_INTRO_SEEN] = true
        }
    }

    suspend fun markNotificationPermissionRequested() {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATION_PERMISSION_REQUESTED] = true
        }
    }

    suspend fun incrementNotificationAskCount(): Int {
        var count = 0
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_NOTIFICATION_ASK_COUNT] ?: 0
            count = current + 1
            prefs[KEY_NOTIFICATION_ASK_COUNT] = count
            prefs[KEY_NOTIFICATION_PERMISSION_REQUESTED] = true
        }
        return count
    }

    suspend fun setDailyRemindersEnabled(enabled: Boolean) {
        context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("daily_reminders_enabled", enabled).apply()
        context.dataStore.edit { prefs ->
            prefs[KEY_DAILY_REMINDERS_ENABLED] = enabled
        }
    }

    suspend fun setDevMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEV_MODE] = enabled
        }
    }

    suspend fun setHintButtonVisible(visible: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HINT_BUTTON_VISIBLE] = visible
        }
    }

    suspend fun setGridButtonVisible(visible: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GRID_BUTTON_VISIBLE] = visible
        }
    }

    suspend fun setHintTimerSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(0, 55)
        context.dataStore.edit { prefs ->
            prefs[KEY_HINT_TIMER_SECONDS] = clamped
        }
    }

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ANALYTICS_ENABLED] = enabled
        }
    }

    suspend fun setHandGestureEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAND_GESTURE_ENABLED] = enabled
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

    suspend fun resetGameProgress() {
        context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE).edit()
            .putInt("current_level", 1)
            .putInt("streak_days", 0)
            .remove("last_streak_date")
            .remove("last_daily_date")
            .remove("completed_dates")
            .putBoolean(KEY_SYNC_HAS_PLAYED, false)
            .apply()

        context.dataStore.edit { prefs ->
            prefs[KEY_LEVEL] = 1
            prefs[KEY_COINS] = 100
            prefs[KEY_HINTS] = 0
            prefs[KEY_HAMMERS] = 2
            prefs[KEY_WANDS] = 2
            prefs[KEY_STREAK] = 0
            prefs[KEY_LAST_STREAK_DATE] = ""
            prefs[KEY_LAST_DAILY] = ""
            prefs[KEY_COMPLETED_DAILY_DATES] = emptySet()
            prefs[KEY_LEVEL_STARS_MAP] = ""
            prefs[KEY_GLOBAL_LIVES] = 6
            prefs[KEY_DAILY_UNLOCK_SEEN] = false
            prefs[KEY_HINT_AD_INTRO_SEEN] = false
            prefs[KEY_DAILY_HINT_AD_INTRO_SEEN] = false
            prefs[KEY_HAS_PLAYED_BEFORE] = false
            prefs[KEY_CLAIMED_MILESTONES] = emptySet()
        }
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
