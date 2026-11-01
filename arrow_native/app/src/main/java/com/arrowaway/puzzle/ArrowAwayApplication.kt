package com.arrowaway.puzzle

import android.app.Application
import com.arrowaway.puzzle.ads.AdManager
import com.arrowaway.puzzle.config.RemoteConfigManager
import com.arrowaway.puzzle.notification.NotificationHelper

class ArrowAwayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        NotificationHelper.scheduleDailyReminderAlarm(this)
        RemoteConfigManager.initialize()
        AdManager.initialize(this)
    }
}
