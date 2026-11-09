package com.arrowaway.puzzle

import android.app.Application
import com.arrowaway.puzzle.ads.AdManager
import com.arrowaway.puzzle.ads.ConsentManager
import com.arrowaway.puzzle.config.RemoteConfigManager
import com.arrowaway.puzzle.notification.NotificationHelper

class ArrowAwayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        NotificationHelper.scheduleDailyReminderAlarm(this)
        RemoteConfigManager.initialize()
        // Returning users whose consent is already stored (or who are outside consent regions)
        // can start ads right away. Everyone else starts after MainActivity gathers consent.
        if (ConsentManager.canRequestAds(this)) {
            AdManager.initialize(this)
        }
    }
}
