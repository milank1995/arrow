package com.arrowaway.puzzle.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.arrowaway.puzzle.BuildConfig
import com.arrowaway.puzzle.analytics.AnalyticsManager
import com.arrowaway.puzzle.analytics.CrashLogger
import com.arrowaway.puzzle.config.RemoteConfigManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Production AdManager for Arrow Away: Tap Logic Puzzle.
 * Manages Google Mobile Ads (AdMob) with background preloading, safe test IDs,
 * Remote Config live switches, failure handling, and strict pacing.
 */
object AdManager {

    private const val TAG = "ArrowAwayAds"

    // Official Google Sample Test Ad Unit IDs (Guarantees 100% test ad fill without AdMob account policy risks)
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    /**
     * Enforces official Google test ads across all ad units for integration testing & verification.
     * Set to false when deploying the final production build to the Google Play Store.
     */
    var forceTestAds: Boolean = true

    private var cachedRewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    private var cachedInterstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var lastInterstitialTimeMs: Long = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    fun initialize(context: Context) {
        try {
            // Register emulator as test device
            val requestConfig = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(requestConfig)

            MobileAds.initialize(context) { initializationStatus ->
                Log.i(TAG, "Google Mobile Ads initialized successfully (TestAds=$isTestAdsActive): $initializationStatus")
                preloadRewardedAd(context)
                preloadInterstitialAd(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdMob initialization failed: ${e.message}")
            CrashLogger.recordException(e, "AdManager.initialize")
        }
    }

    val isTestAdsActive: Boolean
        get() = forceTestAds || BuildConfig.DEBUG

    // Resolve active Ad Unit ID based on environment
    fun getBannerAdUnitId(): String {
        return if (isTestAdsActive) TEST_BANNER_ID else RemoteConfigManager.getBannerAdUnitId()
    }

    fun getInterstitialAdUnitId(): String {
        return if (isTestAdsActive) TEST_INTERSTITIAL_ID else RemoteConfigManager.getInterstitialAdUnitId()
    }

    fun getRewardedAdUnitId(): String {
        return if (isTestAdsActive) TEST_REWARDED_ID else RemoteConfigManager.getRewardedAdUnitId()
    }

    // =========================================================================
    // 1. Rewarded Ads (Hints & Second Chance Lives)
    // =========================================================================

    fun preloadRewardedAd(context: Context) {
        if (!RemoteConfigManager.isAdsEnabled || !RemoteConfigManager.isRewardedAdsEnabled) return
        if (cachedRewardedAd != null || isRewardedLoading) return

        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()
        val unitId = getRewardedAdUnitId()

        RewardedAd.load(context, unitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                cachedRewardedAd = ad
                isRewardedLoading = false
                Log.d(TAG, "Rewarded ad successfully preloaded (Unit: $unitId).")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                cachedRewardedAd = null
                isRewardedLoading = false
                Log.w(TAG, "Rewarded ad failed to preload: ${loadAdError.message} (code: ${loadAdError.code})")
                // Retry after 20 seconds with exponential backoff buffer
                mainHandler.postDelayed({ preloadRewardedAd(context) }, 20000L)
            }
        })
    }

    /**
     * Shows a rewarded ad.
     * Guarantees that rewards are ONLY granted if the user completes the video.
     */
    fun showRewardedAd(
        activity: Activity,
        placement: String,
        onRewardEarned: () -> Unit,
        onAdClosed: (earned: Boolean) -> Unit = {}
    ) {
        val analytics = AnalyticsManager.getInstance(activity)
        analytics.logAdRewardPromptAccepted(placement)

        if (!RemoteConfigManager.isAdsEnabled || !RemoteConfigManager.isRewardedAdsEnabled) {
            Toast.makeText(activity, "Ad request failed. Please try again later.", Toast.LENGTH_SHORT).show()
            onAdClosed(false)
            return
        }

        val ad = cachedRewardedAd
        if (ad == null) {
            Toast.makeText(activity, "Ad request failed. Please try again later.", Toast.LENGTH_SHORT).show()
            preloadRewardedAd(activity.applicationContext)
            onAdClosed(false)
            return
        }

        var rewardGranted = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed full screen.")
                cachedRewardedAd = null
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed. Reward granted: $rewardGranted")
                preloadRewardedAd(activity.applicationContext)
                onAdClosed(rewardGranted)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                Toast.makeText(activity, "Ad request failed. Please try again later.", Toast.LENGTH_SHORT).show()
                cachedRewardedAd = null
                preloadRewardedAd(activity.applicationContext)
                onAdClosed(false)
            }
        }

        ad.show(activity) { rewardItem ->
            rewardGranted = true
            Log.i(TAG, "User earned reward: ${rewardItem.type} amount=${rewardItem.amount}")
            onRewardEarned()
        }
    }

    // =========================================================================
    // 2. Interstitial Ads (Paced Level Transitions)
    // =========================================================================

    fun preloadInterstitialAd(context: Context) {
        if (!RemoteConfigManager.isAdsEnabled || !RemoteConfigManager.isInterstitialAdsEnabled) return
        if (cachedInterstitialAd != null || isInterstitialLoading) return

        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        val unitId = getInterstitialAdUnitId()

        InterstitialAd.load(context, unitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                cachedInterstitialAd = ad
                isInterstitialLoading = false
                Log.d(TAG, "Interstitial ad successfully preloaded (Unit: $unitId).")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                cachedInterstitialAd = null
                isInterstitialLoading = false
                Log.w(TAG, "Interstitial ad failed to preload: ${loadAdError.message}")
                mainHandler.postDelayed({ preloadInterstitialAd(context) }, 25000L)
            }
        })
    }

    /**
     * Shows an interstitial ad between level transitions.
     * In production:
     * - Only shows on Level > 10 (Onboarding protection)
     * - Only shows if 5-7 min cooldown has elapsed
     * In test mode:
     * - Shows during level completion so developer can immediately verify interstitial ad display.
     */
    fun showInterstitialAd(
        activity: Activity,
        currentLevel: Int,
        ignorePacingForTesting: Boolean = false,
        onDismissed: () -> Unit
    ) {
        if (!RemoteConfigManager.isAdsEnabled || !RemoteConfigManager.isInterstitialAdsEnabled) {
            onDismissed()
            return
        }

        // Enforce strict pacing in production builds
        if (!ignorePacingForTesting && !isTestAdsActive) {
            // Rule 1: Never show interstitials on Levels 1 to 10
            if (currentLevel <= RemoteConfigManager.interstitialMinLevel) {
                onDismissed()
                return
            }

            // Rule 2: Enforce cooldown between interstitials (default 360s)
            val now = System.currentTimeMillis()
            val cooldownMs = RemoteConfigManager.interstitialCooldownSeconds * 1000L
            if (lastInterstitialTimeMs > 0L && (now - lastInterstitialTimeMs) < cooldownMs) {
                val remainingSec = (cooldownMs - (now - lastInterstitialTimeMs)) / 1000L
                Log.d(TAG, "Interstitial skipped due to active cooldown ($remainingSec seconds remaining).")
                onDismissed()
                return
            }
        }

        val ad = cachedInterstitialAd
        if (ad == null) {
            preloadInterstitialAd(activity.applicationContext)
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showing.")
                cachedInterstitialAd = null
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed.")
                lastInterstitialTimeMs = System.currentTimeMillis()
                preloadInterstitialAd(activity.applicationContext)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                cachedInterstitialAd = null
                preloadInterstitialAd(activity.applicationContext)
                onDismissed()
            }
        }

        ad.show(activity)
    }

    // Direct verification helpers
    fun showTestInterstitial(activity: Activity, onDismissed: () -> Unit = {}) {
        showInterstitialAd(activity, currentLevel = 999, ignorePacingForTesting = true, onDismissed = onDismissed)
    }

    fun showTestRewarded(activity: Activity, onRewardEarned: () -> Unit = {}, onAdClosed: (Boolean) -> Unit = {}) {
        showRewardedAd(activity, placement = "test_verification", onRewardEarned = onRewardEarned, onAdClosed = onAdClosed)
    }
}
