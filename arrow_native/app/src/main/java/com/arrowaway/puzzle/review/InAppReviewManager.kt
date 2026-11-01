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
 * Manages Google Play In-App Review requests according to the "Emotional High Principle".
 * Protects user goodwill via a strict 30-day cooldown and targeted milestone triggers.
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
        const val MIN_ELIGIBLE_LEVEL = 10

        /**
         * Determines whether the player is in an optimal "Emotional High" state for a rating prompt.
         */
        fun shouldPromptReview(
            currentLevel: Int,
            starsEarned: Int,
            scoreEarned: Int,
            currentStreakDays: Int,
            lastPromptTimestamp: Long,
            cooldownDays: Int = DEFAULT_COOLDOWN_DAYS,
            isDailyMode: Boolean = false
        ): Boolean {
            val now = System.currentTimeMillis()
            val cooldownMillis = cooldownDays.toLong() * 24L * 60L * 60L * 1000L

            // Rule 1: Strict cooldown check (30 days default)
            if (lastPromptTimestamp > 0 && (now - lastPromptTimestamp) < cooldownMillis) {
                Log.d(TAG, "Review prompt skipped: Active cooldown. Days remaining: ${(cooldownMillis - (now - lastPromptTimestamp)) / (24 * 3600 * 1000)}")
                return false
            }

            // Rule 2: Never prompt during early onboarding (Levels 1-4)
            if (!isDailyMode && currentLevel < 5) {
                return false
            }

            // Trigger Scenario A: Milestone Level 10 Victory (Unlocks Daily Challenges)
            if (!isDailyMode && currentLevel == 10 && starsEarned >= 2) {
                Log.i(TAG, "Review prompt eligible: Milestone Level 10 completed!")
                return true
            }

            // Trigger Scenario B: 3-Day or 7-Day Daily Challenge Streak Reached
            if (isDailyMode && (currentStreakDays == 3 || currentStreakDays == 7)) {
                Log.i(TAG, "Review prompt eligible: Daily Challenge streak milestone ($currentStreakDays days)!")
                return true
            }

            // Trigger Scenario C: High Mastery Triumph (100 Pts, 3 Stars on Level >= 10)
            if (currentLevel >= MIN_ELIGIBLE_LEVEL && starsEarned == 3 && scoreEarned >= 100) {
                Log.i(TAG, "Review prompt eligible: Perfect 100 Pt & 3-Star mastery on Level $currentLevel!")
                return true
            }

            return false
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
