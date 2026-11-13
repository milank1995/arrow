package com.arrowaway.puzzle.config

import android.util.Log
import com.arrowaway.puzzle.BuildConfig
import com.arrowaway.puzzle.analytics.CrashLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.json.JSONObject

/**
 * Minimal, production-lean Remote Config Manager for Arrow Away.
 * Consolidates all essential live-tuning into `app_config` and `ads_config` JSON parameters.
 * Version information is resolved dynamically from the codebase (BuildConfig).
 */
object RemoteConfigManager {

    private const val TAG = "ArrowAwayRemoteConfig"

    const val KEY_APP_CONFIG = "app_config"
    const val KEY_ADS_CONFIG = "ads_config"

    // Production Default JSON payload containing essential gameplay parameters (no hardcoded versions)
    val DEFAULT_APP_CONFIG_JSON = """
        {
          "maintenance_mode": false,
          "support_email": "support@arrowaway.com",
          "default_hint_timer_sec": 10,
          "daily_challenge_unlock_level": 5,
          "daily_completion_coins": 100,
          "easy_daily_until_level": 20,
          "is_daily_challenge_enabled": true,
          "review_prompt_min_level": 8,
          "review_prompt_cooldown_days": 30
        }
    """.trimIndent()

    // Production Default Ads JSON payload matching user requirements
    val DEFAULT_ADS_CONFIG_JSON = """
        {
          "ads_enabled": true,
          "banner_ads_enabled": true,
          "interstitial_ads_enabled": true,
          "rewarded_ads_enabled": true,
          "app_open_ads_enabled": false,
          "interstitial_cooldown_seconds": 360,
          "interstitial_min_level": 10,
          "unit_ids": {
            "banner": "ca-app-pub-8262479902787896/1869486914",
            "interstitial": "ca-app-pub-8262479902787896/8466684854",
            "app_open": "",
            "rewarded": "ca-app-pub-8262479902787896/4324434877"
          }
        }
    """.trimIndent()

    val DEFAULTS: Map<String, Any> = mapOf(
        KEY_APP_CONFIG to DEFAULT_APP_CONFIG_JSON,
        KEY_ADS_CONFIG to DEFAULT_ADS_CONFIG_JSON,
        "daily_challenge_unlock_level" to 5L,
        "easy_daily_until_level" to 20L,
        "review_prompt_min_level" to 8L
    )

    private var remoteConfig: FirebaseRemoteConfig? = null

    fun initialize() {
        try {
            if (FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isEmpty()) {
                Log.w(TAG, "FirebaseApp not initialized, skipping RemoteConfig init")
                return
            }

            val config = FirebaseRemoteConfig.getInstance()
            val interval = if (BuildConfig.DEBUG) 0L else 3600L
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(interval)
                .build()

            config.setConfigSettingsAsync(configSettings)
            config.setDefaultsAsync(DEFAULTS)
            remoteConfig = config

            fetchAndActivate()
        } catch (e: Exception) {
            Log.w(TAG, "RemoteConfig init failed or running in mock environment: ${e.message}")
        }
    }

    fun fetchAndActivate(onComplete: (Boolean) -> Unit = {}) {
        val config = remoteConfig ?: run {
            onComplete(false)
            return
        }

        config.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val updated = task.result
                Log.i(TAG, "RemoteConfig fetch succeeded. Config updated: $updated")
                CrashLogger.log("RemoteConfig: Fetch and activate succeeded (updated=$updated)")
                onComplete(true)
            } else {
                Log.w(TAG, "RemoteConfig fetch failed: ${task.exception?.message}")
                CrashLogger.recordException(task.exception ?: Exception("RemoteConfig fetch failed"), "RemoteConfigManager.fetchAndActivate")
                onComplete(false)
            }
        }
    }

    private val configJson: JSONObject
        get() {
            val appRaw = remoteConfig?.getString(KEY_APP_CONFIG).takeIf { !it.isNullOrBlank() }
            val raw = appRaw ?: DEFAULT_APP_CONFIG_JSON
            return try {
                JSONObject(raw)
            } catch (e: Exception) {
                try {
                    JSONObject(DEFAULT_APP_CONFIG_JSON)
                } catch (ex: Exception) {
                    JSONObject()
                }
            }
        }

    private val adsJson: JSONObject?
        get() {
            val directAdsRaw = remoteConfig?.getString(KEY_ADS_CONFIG).takeIf { !it.isNullOrBlank() }
            if (directAdsRaw != null) {
                try { return JSONObject(directAdsRaw) } catch (_: Exception) {}
            }
            val nested = configJson.optJSONObject("ads")
            if (nested != null) return nested
            return try { JSONObject(DEFAULT_ADS_CONFIG_JSON) } catch (_: Exception) { null }
        }

    // =========================================================================
    // Dynamic App Versioning (Fetched directly from codebase / BuildConfig)
    // =========================================================================

    val appVersionName: String
        get() = BuildConfig.VERSION_NAME

    val appVersionCode: Int
        get() = BuildConfig.VERSION_CODE

    val minRequiredVersion: Int
        get() = BuildConfig.VERSION_CODE

    // =========================================================================
    // Essential Live-Tuning Gameplay Parameters
    // =========================================================================

    val maintenanceMode: Boolean
        get() = configJson.optBoolean("maintenance_mode", false)

    val supportEmail: String
        get() = configJson.optString("support_email", "support@arrowaway.com")

    val defaultHintTimerSec: Int
        get() = configJson.optInt("default_hint_timer_sec", 10)

    val hintTimerRangeMin: Int
        get() = configJson.optInt("hint_timer_range_min", 5)

    val hintTimerRangeMax: Int
        get() = configJson.optInt("hint_timer_range_max", 55)

    val initialFreeHints: Int
        get() = configJson.optInt("initial_free_hints", 3)

    val dailyChallengeUnlockLevel: Int
        get() {
            val direct = remoteConfig?.getString("daily_challenge_unlock_level")?.trim()
            if (!direct.isNullOrBlank()) {
                direct.toIntOrNull()?.let { return it }
            }
            return configJson.optInt("daily_challenge_unlock_level", 5)
        }

    val dailyCompletionCoins: Int
        get() = configJson.optInt("daily_completion_coins", 100)

    val easyDailyUntilLevel: Int
        get() {
            val direct = remoteConfig?.getString("easy_daily_until_level")?.trim()
            if (!direct.isNullOrBlank()) {
                direct.toIntOrNull()?.let { return it }
            }
            return configJson.optInt("easy_daily_until_level", 20)
        }

    val streakGracePeriodHours: Int
        get() = configJson.optInt("streak_grace_period_hours", 4)

    val isDailyChallengeEnabled: Boolean
        get() = configJson.optBoolean("is_daily_challenge_enabled", true)

    val reviewPromptMinLevel: Int
        get() {
            val direct = remoteConfig?.getString("review_prompt_min_level")?.trim()
            if (!direct.isNullOrBlank()) {
                direct.toIntOrNull()?.let { return it }
            }
            return configJson.optInt("review_prompt_min_level", 8)
        }

    val reviewPromptCooldownDays: Int
        get() = configJson.optInt("review_prompt_cooldown_days", 30)

    // =========================================================================
    // Ads Configuration Parameters
    // =========================================================================

    val isAdsEnabled: Boolean
        get() = adsJson?.optBoolean("ads_enabled", true)
            ?: configJson.optBoolean("ads_enabled", true)

    val isBannerAdsEnabled: Boolean
        get() = adsJson?.optBoolean("banner_ads_enabled", true)
            ?: configJson.optBoolean("banner_ads_enabled", true)

    val isInterstitialAdsEnabled: Boolean
        get() = adsJson?.optBoolean("interstitial_ads_enabled", true)
            ?: configJson.optBoolean("interstitial_ads_enabled", true)

    val isRewardedAdsEnabled: Boolean
        get() = adsJson?.optBoolean("rewarded_ads_enabled", true)
            ?: configJson.optBoolean("rewarded_ads_enabled", true)

    val isAppOpenAdsEnabled: Boolean
        get() = adsJson?.optBoolean("app_open_ads_enabled", false)
            ?: false

    val interstitialMinLevel: Int
        get() = adsJson?.optInt("interstitial_min_level", 10) ?: 10

    val interstitialCooldownSeconds: Int
        get() = adsJson?.optInt("interstitial_cooldown_seconds", 360) ?: 360

    fun getBannerAdUnitId(): String {
        return adsJson?.optJSONObject("unit_ids")?.optString("banner")?.takeIf { it.isNotBlank() }
            ?: "ca-app-pub-8262479902787896/1869486914"
    }

    fun getInterstitialAdUnitId(): String {
        return adsJson?.optJSONObject("unit_ids")?.optString("interstitial")?.takeIf { it.isNotBlank() }
            ?: "ca-app-pub-8262479902787896/8466684854"
    }

    fun getRewardedAdUnitId(): String {
        return adsJson?.optJSONObject("unit_ids")?.optString("rewarded")?.takeIf { it.isNotBlank() }
            ?: "ca-app-pub-8262479902787896/4324434877"
    }
}
