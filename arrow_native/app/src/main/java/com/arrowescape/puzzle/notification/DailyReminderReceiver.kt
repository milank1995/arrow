package com.arrowescape.puzzle.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.arrowescape.puzzle.data.UserPreferencesRepository
import java.time.LocalDate

class DailyReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DAILY_REMINDER = "com.arrowescape.puzzle.ACTION_DAILY_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val repository = UserPreferencesRepository(context)

        // Reschedule next occurrence for tomorrow 7:30 PM
        NotificationHelper.scheduleDailyReminderAlarm(context)

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Just rescheduled the alarm after device reboot, don't spam notification immediately
            return
        }

        // Check conditions:
        // 1. Reminders enabled in settings
        // 2. Player has completed Level 5 (currentLevel > 5)
        // 3. Today's puzzle has NOT already been completed
        val remindersEnabled = repository.isDailyRemindersEnabledSynchronous()
        val currentLevel = repository.getCurrentLevelSynchronous()
        val todayStr = LocalDate.now().toString()
        val alreadyCompletedToday = repository.isDailyCompletedTodaySynchronous(todayStr)

        if (remindersEnabled && currentLevel > 5 && !alreadyCompletedToday) {
            val streak = repository.getStreakDaysSynchronous()
            NotificationHelper.postDailyChallengeNotification(context, streak)
        }
    }
}
