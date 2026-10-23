package com.arrowescape.puzzle

import android.app.Application
import com.arrowescape.puzzle.notification.NotificationHelper

class ArrowEscapeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        NotificationHelper.scheduleDailyReminderAlarm(this)
    }
}
