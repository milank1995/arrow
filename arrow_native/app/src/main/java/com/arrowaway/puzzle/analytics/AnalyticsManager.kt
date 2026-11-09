package com.arrowaway.puzzle.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.arrowaway.puzzle.BuildConfig
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Production Analytics facade for Arrow Away: Tap Logic Puzzle.
 * Encapsulates the 7-domain micro-event taxonomy specified in the architecture blueprint.
 * Safely dispatches events to Firebase Analytics and mirrors in Logcat for local dev/testing.
 */
class AnalyticsManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private var firebaseAnalytics: FirebaseAnalytics? = null

    @Volatile
    private var isAnalyticsEnabled: Boolean = !BuildConfig.DEBUG

    init {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(appContext)
            firebaseAnalytics?.setAnalyticsCollectionEnabled(isAnalyticsEnabled)
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAnalytics init failed or running in offline mock environment: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ArrowAwayAnalytics"

        @Volatile
        private var INSTANCE: AnalyticsManager? = null

        fun getInstance(context: Context): AnalyticsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnalyticsManager(context).also { INSTANCE = it }
            }
        }
    }

    /**
     * Dynamically enable or disable analytics tracking.
     * When disabled, all event logging to Firebase Analytics is silenced, preventing
     * developer/test events from polluting production analytics.
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        isAnalyticsEnabled = enabled
        try {
            firebaseAnalytics?.setAnalyticsCollectionEnabled(enabled)
            Log.i(TAG, "Analytics collection enabled state set to: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set analytics collection enabled to $enabled: ${e.message}")
        }
    }

    fun isAnalyticsEnabled(): Boolean = isAnalyticsEnabled

    private fun logEvent(eventName: String, params: Bundle = Bundle()) {
        if (!isAnalyticsEnabled) {
            // Silenced during local testing/development to protect analytics accuracy
            return
        }
        try {
            firebaseAnalytics?.logEvent(eventName, params)
            Log.d(TAG, "[Event: $eventName] params: $params")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event $eventName: ${e.message}")
        }
    }

    // =========================================================================
    // Domain 1: App Lifecycle, Sessions & Navigation
    // =========================================================================

    fun logAppSessionStart(sessionId: String, isColdStart: Boolean, theme: String, currentLevel: Int) {
        val bundle = Bundle().apply {
            putString("session_id", sessionId)
            putBoolean("is_cold_start", isColdStart)
            putString("theme", theme)
            putInt("current_level", currentLevel)
        }
        logEvent("app_session_start", bundle)
    }

    fun logAppSessionEnd(sessionId: String, durationSec: Long, levelsAttempted: Int, levelsCompleted: Int) {
        val bundle = Bundle().apply {
            putString("session_id", sessionId)
            putLong("duration_sec", durationSec)
            putInt("levels_attempted", levelsAttempted)
            putInt("levels_completed", levelsCompleted)
        }
        logEvent("app_session_end", bundle)
    }

    fun logAppForeground(timeInBackgroundSec: Long) {
        val bundle = Bundle().apply {
            putLong("time_in_background_sec", timeInBackgroundSec)
        }
        logEvent("app_foreground", bundle)
    }

    fun logAppBackground(activeScreen: String, wasPuzzleActive: Boolean) {
        val bundle = Bundle().apply {
            putString("active_screen", activeScreen)
            putBoolean("was_puzzle_active", wasPuzzleActive)
        }
        logEvent("app_background", bundle)
    }

    fun logScreenView(screenName: String, previousScreen: String? = null) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString("screen_name", screenName)
            if (previousScreen != null) {
                putString("previous_screen", previousScreen)
            }
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    // =========================================================================
    // Domain 2: Core Puzzle Gameplay & Progression
    // =========================================================================

    fun logLevelStart(
        levelNumber: Int,
        gameMode: String,
        totalArrows: Int,
        gridDimensions: String,
        attemptNumber: Int
    ) {
        val bundle = Bundle().apply {
            putInt("level_number", levelNumber)
            putString("game_mode", gameMode)
            putInt("total_arrows", totalArrows)
            putString("grid_dimensions", gridDimensions)
            putInt("attempt_number", attemptNumber)
            putString(FirebaseAnalytics.Param.LEVEL_NAME, "Level_$levelNumber")
        }
        logEvent(FirebaseAnalytics.Event.LEVEL_START, bundle)
    }

    fun logLevelFirstInteraction(levelNumber: Int, inspectionTimeSec: Long) {
        val bundle = Bundle().apply {
            putInt("level_number", levelNumber)
            putLong("inspection_time_sec", inspectionTimeSec)
        }
        logEvent("level_first_interaction", bundle)
    }

    fun logLevelComplete(
        levelNumber: Int,
        gameMode: String,
        activePlayTimeSec: Long,
        score: Int,
        heartsRemaining: Int,
        mistakesCount: Int,
        hintsUsed: Int,
        starsEarned: Int
    ) {
        val bundle = Bundle().apply {
            putInt("level_number", levelNumber)
            putString("game_mode", gameMode)
            putLong("active_play_time_sec", activePlayTimeSec)
            putInt("score", score)
            putInt("hearts_remaining", heartsRemaining)
            putInt("mistakes_count", mistakesCount)
            putInt("hints_used", hintsUsed)
            putInt("stars_earned", starsEarned)
            putString(FirebaseAnalytics.Param.LEVEL_NAME, "Level_$levelNumber")
            putLong(FirebaseAnalytics.Param.SCORE, score.toLong())
        }
        logEvent(FirebaseAnalytics.Event.LEVEL_END, bundle)
        logEvent("level_complete", bundle)
    }

    fun logLevelFailed(
        levelNumber: Int,
        activePlayTimeSec: Long,
        arrowsRemaining: Int,
        hintsUsed: Int
    ) {
        val bundle = Bundle().apply {
            putInt("level_number", levelNumber)
            putLong("active_play_time_sec", activePlayTimeSec)
            putInt("arrows_remaining", arrowsRemaining)
            putInt("hints_used", hintsUsed)
        }
        logEvent("level_failed", bundle)
    }

    fun logLevelAbandoned(
        levelNumber: Int,
        activePlayTimeSec: Long,
        arrowsRemaining: Int,
        exitPoint: String
    ) {
        val bundle = Bundle().apply {
            putInt("level_number", levelNumber)
            putLong("active_play_time_sec", activePlayTimeSec)
            putInt("arrows_remaining", arrowsRemaining)
            putString("exit_point", exitPoint)
        }
        logEvent("level_abandoned", bundle)
    }

    fun logLevelRestart(levelNumber: Int, restartSource: String) {
        val bundle = Bundle().apply {
            putInt("level_number", levelNumber)
            putString("restart_source", restartSource)
        }
        logEvent("level_restart", bundle)
    }

    fun logPuzzlePaused(levelNumber: Int, arrowsRemaining: Int, activeTimeAtPauseSec: Long) {
        val bundle = Bundle().apply {
            putInt("level_number", levelNumber)
            putInt("arrows_remaining", arrowsRemaining)
            putLong("active_time_at_pause_sec", activeTimeAtPauseSec)
        }
        logEvent("puzzle_paused", bundle)
    }

    fun logPuzzleResumed(levelNumber: Int, pauseDurationSec: Long) {
        val bundle = Bundle().apply {
            putInt("level_number", levelNumber)
            putLong("pause_duration_sec", pauseDurationSec)
        }
        logEvent("puzzle_resumed", bundle)
    }

    // =========================================================================
    // Domain 3: Boosters & Micro-Interactions
    // =========================================================================

    fun logHintButtonClicked(
        levelNumber: Int,
        hintsBalance: Int,
        wasIdleHighlightActive: Boolean,
        isFreeHint: Boolean
    ) {
        val bundle = Bundle().apply {
            putInt("level_number", levelNumber)
            putInt("hints_balance", hintsBalance)
            putBoolean("was_idle_highlight_active", wasIdleHighlightActive)
            putBoolean("is_free_hint", isFreeHint)
        }
        logEvent("hint_button_clicked", bundle)
    }

    fun logHintApplied(levelNumber: Int, targetArrowId: String, remainingArrowsBeforeHint: Int) {
        val bundle = Bundle().apply {
            putInt("level_number", levelNumber)
            putString("target_arrow_id", targetArrowId)
            putInt("remaining_arrows_before_hint", remainingArrowsBeforeHint)
        }
        logEvent("hint_applied", bundle)
    }

    fun logGridButtonToggled(levelNumber: Int, newState: String) {
        val bundle = Bundle().apply {
            putInt("level_number", levelNumber)
            putString("new_state", newState)
        }
        logEvent("grid_button_toggled", bundle)
    }

    fun logArrowTapWrong(levelNumber: Int, heartsLeftAfterHit: Int, arrowId: String) {
        val bundle = Bundle().apply {
            putInt("level_number", levelNumber)
            putInt("hearts_left_after_hit", heartsLeftAfterHit)
            putString("arrow_id", arrowId)
        }
        logEvent("arrow_tap_wrong", bundle)
    }

    // =========================================================================
    // Domain 4: Daily Challenge & Calendar
    // =========================================================================

    fun logDailyChallengeViewed(completedMonthCount: Int, currentStreak: Int) {
        val bundle = Bundle().apply {
            putInt("completed_month_count", completedMonthCount)
            putInt("current_streak", currentStreak)
        }
        logEvent("daily_challenge_viewed", bundle)
    }

    fun logDailyDateSelected(selectedDate: String, isToday: Boolean, isReplay: Boolean) {
        val bundle = Bundle().apply {
            putString("selected_date", selectedDate)
            putBoolean("is_today", isToday)
            putBoolean("is_replay", isReplay)
        }
        logEvent("daily_date_selected", bundle)
    }

    fun logDailyChallengeStart(
        puzzleDate: String,
        dailyLevelIndex: Int,
        arrowCount: Int,
        dailyPool: String = "standard"
    ) {
        val bundle = Bundle().apply {
            putString("puzzle_date", puzzleDate)
            putInt("daily_level_index", dailyLevelIndex)
            putInt("arrow_count", arrowCount)
            putString("daily_pool", dailyPool)
        }
        logEvent("daily_challenge_start", bundle)
    }

    fun logDailyChallengeComplete(
        puzzleDate: String,
        completionTimeSec: Long,
        newStreakDays: Int,
        rewardCoinsClaimed: Int,
        dailyPool: String = "standard"
    ) {
        val bundle = Bundle().apply {
            putString("puzzle_date", puzzleDate)
            putLong("completion_time_sec", completionTimeSec)
            putInt("new_streak_days", newStreakDays)
            putInt("reward_coins_claimed", rewardCoinsClaimed)
            putString("daily_pool", dailyPool)
        }
        logEvent("daily_challenge_complete", bundle)
    }

    fun logDailyNotificationClicked(streakDays: Int, actionButton: String) {
        val bundle = Bundle().apply {
            putInt("streak_days", streakDays)
            putString("action_button", actionButton)
        }
        logEvent("daily_notification_clicked", bundle)
    }

    fun logNotificationPrePromptShown(promptType: String) {
        val bundle = Bundle().apply {
            putString("prompt_type", promptType)
        }
        logEvent("notification_preprompt_shown", bundle)
    }

    fun logNotificationPrePromptResult(promptType: String, result: String) {
        val bundle = Bundle().apply {
            putString("prompt_type", promptType)
            putString("result", result)
        }
        logEvent("notification_preprompt_result", bundle)
    }

    fun logNotificationPermissionResult(result: String) {
        val bundle = Bundle().apply {
            putString("result", result)
        }
        logEvent("notification_permission_result", bundle)
    }

    // =========================================================================
    // Domain 5: Settings & Customization
    // =========================================================================

    fun logSettingsOpened(currentTheme: String) {
        val bundle = Bundle().apply {
            putString("current_theme", currentTheme)
        }
        logEvent("settings_opened", bundle)
    }

    fun logSettingPreferenceChanged(settingKey: String, oldValue: Any, newValue: Any) {
        val bundle = Bundle().apply {
            putString("setting_key", settingKey)
            putString("old_value", oldValue.toString())
            putString("new_value", newValue.toString())
        }
        logEvent("setting_preference_changed", bundle)
    }

    fun logDevModeToggled(isDevMode: Boolean) {
        val bundle = Bundle().apply {
            putBoolean("is_dev_mode", isDevMode)
        }
        logEvent("dev_mode_toggled", bundle)
    }

    // =========================================================================
    // Domain 6: Economy & Monetization (Ad Readiness)
    // =========================================================================

    fun logAdRewardPromptShown(adPlacement: String) {
        val bundle = Bundle().apply {
            putString("ad_placement", adPlacement)
        }
        logEvent("ad_reward_prompt_shown", bundle)
    }

    fun logAdRewardPromptAccepted(adPlacement: String) {
        val bundle = Bundle().apply {
            putString("ad_placement", adPlacement)
        }
        logEvent("ad_reward_prompt_accepted", bundle)
    }

    fun logAdRewardCompleted(adPlacement: String, rewardType: String, rewardAmount: Int) {
        val bundle = Bundle().apply {
            putString("ad_placement", adPlacement)
            putString("reward_type", rewardType)
            putInt("reward_amount", rewardAmount)
        }
        logEvent("ad_reward_completed", bundle)
    }

    fun logAdImpression(adType: String, adPlacement: String) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
            putString("ad_placement", adPlacement)
        }
        // Not "ad_impression": that name is reserved for the AdMob SDK's automatic event, which carries revenue.
        logEvent("ad_shown", bundle)
    }

    fun logAdDismissed(adType: String, adPlacement: String, rewardEarned: Boolean = false) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
            putString("ad_placement", adPlacement)
            putBoolean("reward_earned", rewardEarned)
        }
        logEvent("ad_dismissed", bundle)
    }

    fun logAdLoadFailed(adType: String, errorCode: Int, errorMessage: String) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
            putInt("error_code", errorCode)
            putString("error_message", errorMessage)
        }
        logEvent("ad_load_failed", bundle)
    }

    fun logAdDisplayFailed(adType: String, errorCode: Int, errorMessage: String, placement: String = "") {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
            putInt("error_code", errorCode)
            putString("error_message", errorMessage)
            if (placement.isNotEmpty()) {
                putString("ad_placement", placement)
            }
        }
        logEvent("ad_display_failed", bundle)
    }

    fun logVirtualCurrencySpent(
        currencyType: String = "coins",
        amount: Int,
        itemPurchased: String,
        balanceAfter: Int
    ) {
        val bundle = Bundle().apply {
            putString("currency_type", currencyType)
            putInt("amount", amount)
            putString("item_purchased", itemPurchased)
            putInt("balance_after", balanceAfter)
            putLong(FirebaseAnalytics.Param.VALUE, amount.toLong())
            putString(FirebaseAnalytics.Param.ITEM_NAME, itemPurchased)
        }
        logEvent(FirebaseAnalytics.Event.SPEND_VIRTUAL_CURRENCY, bundle)
        logEvent("virtual_currency_spent", bundle)
    }

    // =========================================================================
    // Domain 7: In-App Errors & Unexpected Behavior
    // =========================================================================

    fun logNonFatalException(exceptionClass: String, message: String, location: String) {
        val bundle = Bundle().apply {
            putString("exception_class", exceptionClass)
            putString("message", message)
            putString("location", location)
        }
        logEvent("non_fatal_exception", bundle)
    }

    fun logAssetLoadFallback(requestedLevel: Int, fallbackLevel: Int) {
        val bundle = Bundle().apply {
            putInt("requested_level", requestedLevel)
            putInt("fallback_level", fallbackLevel)
        }
        logEvent("asset_load_fallback", bundle)
    }

    // =========================================================================
    // Domain 8: In-App Review & Rating Feedback
    // =========================================================================

    fun logInAppReviewPrompted(
        levelNumber: Int,
        triggerReason: String,
        scoreEarned: Int,
        starsEarned: Int,
        streakDays: Int
    ) {
        val bundle = Bundle().apply {
            putInt("level_number", levelNumber)
            putString("trigger_reason", triggerReason)
            putInt("score_earned", scoreEarned)
            putInt("stars_earned", starsEarned)
            putInt("streak_days", streakDays)
        }
        logEvent("in_app_review_prompted", bundle)
    }

    fun logInAppReviewCompleted(success: Boolean) {
        val bundle = Bundle().apply {
            putBoolean("success", success)
        }
        logEvent("in_app_review_completed", bundle)
    }

    // =========================================================================
    // Domain 9: User Properties (Progress & Player Segmentation)
    // =========================================================================

    fun setUserPropertyMaxLevel(maxLevel: Int) {
        try {
            firebaseAnalytics?.setUserProperty("max_level_reached", maxLevel.toString())
            Log.d(TAG, "User property updated: max_level_reached = $maxLevel")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set user property max_level_reached: ${e.message}")
        }
    }

    fun setUserPropertyTotalLevelsCompleted(totalCompleted: Int) {
        try {
            firebaseAnalytics?.setUserProperty("total_levels_completed", totalCompleted.toString())
        } catch (_: Exception) {}
    }

    fun setUserPropertyStreakDays(streakDays: Int) {
        try {
            firebaseAnalytics?.setUserProperty("streak_days", streakDays.toString())
        } catch (_: Exception) {}
    }

    // =========================================================================
    // Domain 10: Game Over Decision Funnel (Rewarded Ad vs Restart)
    // =========================================================================

    fun logGameOverDecision(levelNumber: Int, decision: String, isDailyMode: Boolean) {
        val bundle = Bundle().apply {
            putInt("level_number", levelNumber)
            putString("decision", decision)
            putBoolean("is_daily_mode", isDailyMode)
        }
        logEvent("game_over_decision", bundle)
    }
}
