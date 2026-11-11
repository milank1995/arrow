package com.arrowaway.puzzle

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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

        // Track whole-app foreground/background transitions for AdMob lifecycle management
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                AdManager.onAppForegrounded(this@ArrowAwayApplication)
            }

            override fun onStop(owner: LifecycleOwner) {
                AdManager.onAppBackgrounded()
            }
        })

        // Returning users whose consent is already stored (or who are outside consent regions)
        // can start ads right away. Everyone else starts after MainActivity gathers consent.
        if (ConsentManager.canRequestAds(this)) {
            AdManager.initialize(this)
        }
    }
}
