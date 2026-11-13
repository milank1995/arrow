package com.arrowaway.puzzle.review

import android.app.Activity
import android.util.Log
import com.arrowaway.puzzle.BuildConfig
import com.arrowaway.puzzle.analytics.CrashLogger
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager

/**
 * Result data class for in-app review prompt eligibility and trigger reasoning.
 */
data class ReviewTriggerResult(
    val shouldPrompt: Boolean,
    val triggerReason: String = ""
)

/**
 * Manages Google Play In-App Review requests according to the "Emotional High Principle".
 * Protects user goodwill via a strict 30-day cooldown and targeted milestone triggers.
 * Primary milestone: Level 8 (avoiding competing prompts on Level 10).
 */
class InAppReviewManager(private val activity: Activity) {

    private val reviewManager: ReviewManager = if (BuildConfig.DEBUG) {
        // In local debug builds, FakeReviewManager simulates the API without requiring Play Store installation
        FakeReviewManager(activity)
    } else {
        ReviewManagerFactory.create(activity)
    }

    companion object {
        private const val TAG = "ArrowAwayReview"
        const val DEFAULT_COOLDOWN_DAYS = 30
        const val MIN_ELIGIBLE_LEVEL = 8

        /**
         * Checks whether the player is in an optimal "Emotional High" state for a rating prompt,
         * returning the eligibility decision and trigger reason.
         */
        fun checkReviewEligibility(
            currentLevel: Int,
            starsEarned: Int,
            scoreEarned: Int,
            currentStreakDays: Int,
            lastPromptTimestamp: Long,
            cooldownDays: Int = DEFAULT_COOLDOWN_DAYS,
            minEligibleLevel: Int = MIN_ELIGIBLE_LEVEL,
            isDailyMode: Boolean = false,
            completedDailiesCount: Int = 0
        ): ReviewTriggerResult {
            val now = System.currentTimeMillis()
            val cooldownMillis = cooldownDays.toLong() * 24L * 60L * 60L * 1000L

            // Rule 1: Strict cooldown check (30 days default)
            if (lastPromptTimestamp > 0 && (now - lastPromptTimestamp) < cooldownMillis) {
                val daysRemaining = (cooldownMillis - (now - lastPromptTimestamp)) / (24 * 3600 * 1000)
                Log.d(TAG, "Review prompt skipped: Active cooldown. Days remaining: $daysRemaining")
                return ReviewTriggerResult(false)
            }

            // Rule 2: Never prompt during early onboarding (Levels 1-4)
            if (!isDailyMode && currentLevel < 5) {
                return ReviewTriggerResult(false)
            }

            // Trigger Scenario A: Milestone Victory at minEligibleLevel (default Level 8) with >= 2 stars
            if (!isDailyMode && currentLevel == minEligibleLevel && starsEarned >= 2) {
                Log.i(TAG, "Review prompt eligible: Milestone Level $minEligibleLevel completed!")
                return ReviewTriggerResult(true, "milestone_level_$minEligibleLevel")
            }

            // Trigger Scenario B1: First Daily Challenge Completed Ever
            if (isDailyMode && completedDailiesCount == 1) {
                Log.i(TAG, "Review prompt eligible: First Daily Challenge completed ever!")
                return ReviewTriggerResult(true, "first_daily_complete")
            }

            // Trigger Scenario B2: 3-Day or 7-Day Daily Challenge Streak Reached
            if (isDailyMode && (currentStreakDays == 3 || currentStreakDays == 7)) {
                Log.i(TAG, "Review prompt eligible: Daily Challenge streak milestone ($currentStreakDays days)!")
                return ReviewTriggerResult(true, "daily_streak_$currentStreakDays")
            }

            // Trigger Scenario C: High Mastery Triumph (100 Pts, 3 Stars on Level >= minEligibleLevel)
            if (!isDailyMode && currentLevel >= minEligibleLevel && starsEarned == 3 && scoreEarned >= 100) {
                Log.i(TAG, "Review prompt eligible: Perfect 100 Pt & 3-Star mastery on Level $currentLevel!")
                return ReviewTriggerResult(true, "perfect_mastery_level_$currentLevel")
            }

            return ReviewTriggerResult(false)
        }

        /**
         * Backward-compatible boolean helper.
         */
        fun shouldPromptReview(
            currentLevel: Int,
            starsEarned: Int,
            scoreEarned: Int,
            currentStreakDays: Int,
            lastPromptTimestamp: Long,
            cooldownDays: Int = DEFAULT_COOLDOWN_DAYS,
            minEligibleLevel: Int = MIN_ELIGIBLE_LEVEL,
            isDailyMode: Boolean = false,
            completedDailiesCount: Int = 0
        ): Boolean {
            return checkReviewEligibility(
                currentLevel = currentLevel,
                starsEarned = starsEarned,
                scoreEarned = scoreEarned,
                currentStreakDays = currentStreakDays,
                lastPromptTimestamp = lastPromptTimestamp,
                cooldownDays = cooldownDays,
                minEligibleLevel = minEligibleLevel,
                isDailyMode = isDailyMode,
                completedDailiesCount = completedDailiesCount
            ).shouldPrompt
        }
    }

    /**
     * Requests the ReviewInfo object from Google Play and displays the native bottom sheet dialog.
     */
    fun launchReviewFlow(onComplete: (success: Boolean) -> Unit) {
        CrashLogger.log("InAppReviewManager: Requesting ReviewInfo from Google Play")

        val requestTask = reviewManager.requestReviewFlow()
        requestTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo: ReviewInfo = task.result
                CrashLogger.log("InAppReviewManager: Launching review sheet")
                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    // Note: The Play Store API flow always returns success once the sheet is dismissed or submitted.
                    CrashLogger.log("InAppReviewManager: Review flow completed")
                    onComplete(true)
                }
            } else {
                val error = task.exception
                Log.w(TAG, "In-app review request failed: ${error?.message}")
                CrashLogger.recordException(error ?: Exception("Review request failed"), "InAppReviewManager.launchReviewFlow")
                onComplete(false)
            }
        }
    }
}
