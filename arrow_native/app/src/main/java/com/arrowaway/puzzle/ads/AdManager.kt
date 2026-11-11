package com.arrowaway.puzzle.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
     * Set to false for production release so live AdMob ad units are loaded in Release builds.
     */
    var forceTestAds: Boolean = false

    private var cachedRewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    private var cachedInterstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private const val PREFS_NAME = "arrow_ad_prefs"
    private const val KEY_LAST_INTERSTITIAL_TIME = "last_interstitial_time_ms"
    private const val KEY_PENDING_INTERSTITIAL_TIME = "pending_interstitial_time_ms"
    const val PENDING_INTERSTITIAL_TTL_MS = 5 * 60 * 1000L // 5 minutes TTL

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    const val AD_EXPIRY_MS = 55 * 60 * 1000L // 55 minutes (AdMob ads expire after 60 min)
    val BACKOFF_DELAYS_SEC = intArrayOf(30, 60, 120, 240, 600)

    fun getBackoffDelaySec(attempt: Int): Int {
        val index = attempt.coerceIn(0, BACKOFF_DELAYS_SEC.size - 1)
        return BACKOFF_DELAYS_SEC[index]
    }

    var isAppInForeground: Boolean = true
        private set

    private var rewardedRetryRunnable: Runnable? = null
    private var interstitialRetryRunnable: Runnable? = null
    private var rewardedRetryAttempt: Int = 0
    private var interstitialRetryAttempt: Int = 0

    private var rewardedAdLoadedTimeMs: Long = 0L
    private var interstitialAdLoadedTimeMs: Long = 0L

    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    fun isRewardedAdExpired(): Boolean {
        if (cachedRewardedAd == null) return false
        val elapsed = System.currentTimeMillis() - rewardedAdLoadedTimeMs
        return elapsed >= AD_EXPIRY_MS
    }

    fun isInterstitialAdExpired(): Boolean {
        if (cachedInterstitialAd == null) return false
        val elapsed = System.currentTimeMillis() - interstitialAdLoadedTimeMs
        return elapsed >= AD_EXPIRY_MS
    }

    fun onAppForegrounded(context: Context) {
        isAppInForeground = true
        Log.d(TAG, "App foregrounded. Checking ad cache freshness and preloading if needed.")
        val appContext = context.applicationContext
        if (!isInitialized) return

        if (isRewardedAdExpired()) {
            Log.d(TAG, "Cached rewarded ad expired (>55m). Discarding.")
            cachedRewardedAd = null
            _rewardedReady.value = false
        }
        if (cachedRewardedAd == null && !isRewardedLoading) {
            preloadRewardedAd(appContext)
        }

        if (isInterstitialAdExpired()) {
            Log.d(TAG, "Cached interstitial ad expired (>55m). Discarding.")
            cachedInterstitialAd = null
        }
        if (cachedInterstitialAd == null && !isInterstitialLoading) {
            preloadInterstitialAd(appContext)
        }
    }

    fun onAppBackgrounded() {
        isAppInForeground = false
        Log.d(TAG, "App backgrounded. Cancelling pending ad retry timers.")
        rewardedRetryRunnable?.let { mainHandler.removeCallbacks(it) }
        rewardedRetryRunnable = null
        interstitialRetryRunnable?.let { mainHandler.removeCallbacks(it) }
        interstitialRetryRunnable = null
    }

    fun getLastInterstitialTime(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_INTERSTITIAL_TIME, 0L)
    }

    fun setLastInterstitialTime(context: Context, timestamp: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_INTERSTITIAL_TIME, timestamp)
            .apply()
    }

    fun getPendingInterstitialTime(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_PENDING_INTERSTITIAL_TIME, 0L)
    }

    fun setPendingInterstitialTime(context: Context, timestamp: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_PENDING_INTERSTITIAL_TIME, timestamp)
            .apply()
    }

    fun clearPendingInterstitial(context: Context) {
        setPendingInterstitialTime(context, 0L)
    }

    fun hasPendingInterstitial(context: Context): Boolean {
        if (!RemoteConfigManager.isAdsEnabled || !RemoteConfigManager.isInterstitialAdsEnabled) {
            clearPendingInterstitial(context)
            return false
        }
        val pendingTime = getPendingInterstitialTime(context)
        if (pendingTime <= 0L) return false

        val elapsed = System.currentTimeMillis() - pendingTime
        return if (elapsed in 0L..PENDING_INTERSTITIAL_TTL_MS) {
            true
        } else {
            // Expired (> 5 minutes) -> discard gracefully so returning users aren't surprised
            clearPendingInterstitial(context)
            false
        }
    }

    private val isMobileAdsInitializeCalled = java.util.concurrent.atomic.AtomicBoolean(false)

    /** True once the Mobile Ads SDK has been started (only happens after consent allows it). */
    val isInitialized: Boolean
        get() = isMobileAdsInitializeCalled.get()

    /**
     * Starts the Mobile Ads SDK. Must only be called once [ConsentManager.canRequestAds] is true.
     * Safe to call multiple times; only the first call has an effect.
     */
    fun initialize(context: Context) {
        if (!isMobileAdsInitializeCalled.compareAndSet(false, true)) return
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
        val appContext = context.applicationContext
        if (!isInitialized) return // No ad requests until consent allows the SDK to start
        if (!RemoteConfigManager.isAdsEnabled || !RemoteConfigManager.isRewardedAdsEnabled) return
        if (!isAppInForeground) {
            Log.d(TAG, "Skipping rewarded preload: app is in background.")
            return
        }

        if (isRewardedAdExpired()) {
            Log.d(TAG, "Cached rewarded ad expired (>55m). Discarding.")
            cachedRewardedAd = null
            _rewardedReady.value = false
        }
        if (cachedRewardedAd != null || isRewardedLoading) return

        rewardedRetryRunnable?.let { mainHandler.removeCallbacks(it) }
        rewardedRetryRunnable = null

        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()
        val unitId = getRewardedAdUnitId()

        RewardedAd.load(appContext, unitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                cachedRewardedAd = ad
                rewardedAdLoadedTimeMs = System.currentTimeMillis()
                isRewardedLoading = false
                rewardedRetryAttempt = 0
                _rewardedReady.value = true
                Log.d(TAG, "Rewarded ad successfully preloaded (Unit: $unitId).")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                cachedRewardedAd = null
                isRewardedLoading = false
                _rewardedReady.value = false
                Log.w(TAG, "Rewarded ad failed to preload: ${loadAdError.message} (code: ${loadAdError.code})")

                val delaySec = getBackoffDelaySec(rewardedRetryAttempt)
                val attempt = rewardedRetryAttempt
                rewardedRetryAttempt++

                AnalyticsManager.getInstance(appContext).logAdLoadFailed(
                    adType = "rewarded",
                    errorCode = loadAdError.code,
                    errorMessage = loadAdError.message,
                    retryAttempt = attempt,
                    nextRetrySec = delaySec,
                    inForeground = isAppInForeground
                )

                if (isAppInForeground) {
                    val runnable = Runnable {
                        rewardedRetryRunnable = null
                        preloadRewardedAd(appContext)
                    }
                    rewardedRetryRunnable = runnable
                    mainHandler.postDelayed(runnable, delaySec * 1000L)
                }
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
            onAdClosed(false)
            return
        }

        if (isRewardedAdExpired()) {
            Log.d(TAG, "Cached rewarded ad expired (>55m) at show time. Discarding.")
            cachedRewardedAd = null
            _rewardedReady.value = false
        }

        val ad = cachedRewardedAd
        if (ad == null) {
            analytics.logAdDisplayFailed(
                adType = "rewarded",
                errorCode = -1,
                errorMessage = "Ad not cached / ready",
                placement = placement
            )
            preloadRewardedAd(activity.applicationContext)
            onAdClosed(false)
            return
        }

        var rewardGranted = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed full screen.")
                analytics.logAdImpression(adType = "rewarded", adPlacement = placement)
                cachedRewardedAd = null
                _rewardedReady.value = false
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed. Reward granted: $rewardGranted")
                analytics.logAdDismissed(adType = "rewarded", adPlacement = placement, rewardEarned = rewardGranted)
                _rewardedReady.value = false
                preloadRewardedAd(activity.applicationContext)
                onAdClosed(rewardGranted)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                analytics.logAdDisplayFailed(
                    adType = "rewarded",
                    errorCode = adError.code,
                    errorMessage = adError.message,
                    placement = placement
                )
                cachedRewardedAd = null
                _rewardedReady.value = false
                preloadRewardedAd(activity.applicationContext)
                onAdClosed(false)
            }
        }

        ad.show(activity) { rewardItem ->
            rewardGranted = true
            Log.i(TAG, "User earned reward: ${rewardItem.type} amount=${rewardItem.amount}")
            analytics.logAdRewardCompleted(
                adPlacement = placement,
                rewardType = if (rewardItem.type.isNullOrEmpty()) "reward" else rewardItem.type,
                rewardAmount = rewardItem.amount
            )
            onRewardEarned()
        }
    }

    // =========================================================================
    // 2. Interstitial Ads (Paced Level Transitions)
    // =========================================================================

    fun preloadInterstitialAd(context: Context) {
        val appContext = context.applicationContext
        if (!isInitialized) return // No ad requests until consent allows the SDK to start
        if (!RemoteConfigManager.isAdsEnabled || !RemoteConfigManager.isInterstitialAdsEnabled) return
        if (!isAppInForeground) {
            Log.d(TAG, "Skipping interstitial preload: app is in background.")
            return
        }

        if (isInterstitialAdExpired()) {
            Log.d(TAG, "Cached interstitial ad expired (>55m). Discarding.")
            cachedInterstitialAd = null
        }
        if (cachedInterstitialAd != null || isInterstitialLoading) return

        interstitialRetryRunnable?.let { mainHandler.removeCallbacks(it) }
        interstitialRetryRunnable = null

        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        val unitId = getInterstitialAdUnitId()

        InterstitialAd.load(appContext, unitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                cachedInterstitialAd = ad
                interstitialAdLoadedTimeMs = System.currentTimeMillis()
                isInterstitialLoading = false
                interstitialRetryAttempt = 0
                Log.d(TAG, "Interstitial ad successfully preloaded (Unit: $unitId).")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                cachedInterstitialAd = null
                isInterstitialLoading = false
                Log.w(TAG, "Interstitial ad failed to preload: ${loadAdError.message}")

                val delaySec = getBackoffDelaySec(interstitialRetryAttempt)
                val attempt = interstitialRetryAttempt
                interstitialRetryAttempt++

                AnalyticsManager.getInstance(appContext).logAdLoadFailed(
                    adType = "interstitial",
                    errorCode = loadAdError.code,
                    errorMessage = loadAdError.message,
                    retryAttempt = attempt,
                    nextRetrySec = delaySec,
                    inForeground = isAppInForeground
                )

                if (isAppInForeground) {
                    val runnable = Runnable {
                        interstitialRetryRunnable = null
                        preloadInterstitialAd(appContext)
                    }
                    interstitialRetryRunnable = runnable
                    mainHandler.postDelayed(runnable, delaySec * 1000L)
                }
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
            val lastTime = getLastInterstitialTime(activity)
            if (lastTime > 0L && (now - lastTime) < cooldownMs) {
                val remainingSec = (cooldownMs - (now - lastTime)) / 1000L
                Log.d(TAG, "Interstitial skipped due to active cooldown ($remainingSec seconds remaining).")
                onDismissed()
                return
            }
        }

        if (isInterstitialAdExpired()) {
            Log.d(TAG, "Cached interstitial ad expired (>55m) at show time. Discarding.")
            cachedInterstitialAd = null
        }

        val analytics = AnalyticsManager.getInstance(activity)
        val ad = cachedInterstitialAd
        if (ad == null) {
            preloadInterstitialAd(activity.applicationContext)
            onDismissed()
            return
        }

        // Record pending ad timestamp to prevent force-quit bypass
        setPendingInterstitialTime(activity, System.currentTimeMillis())

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showing.")
                analytics.logAdImpression(adType = "interstitial", adPlacement = "level_transition_$currentLevel")
                cachedInterstitialAd = null
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed.")
                analytics.logAdDismissed(adType = "interstitial", adPlacement = "level_transition_$currentLevel")
                clearPendingInterstitial(activity)
                setLastInterstitialTime(activity, System.currentTimeMillis())
                preloadInterstitialAd(activity.applicationContext)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                analytics.logAdDisplayFailed(
                    adType = "interstitial",
                    errorCode = adError.code,
                    errorMessage = adError.message,
                    placement = "level_transition_$currentLevel"
                )
                clearPendingInterstitial(activity)
                cachedInterstitialAd = null
                preloadInterstitialAd(activity.applicationContext)
                onDismissed()
            }
        }

        ad.show(activity)
    }

    /**
     * Displays a pending interstitial ad that was interrupted (e.g. by force quitting).
     * If the ad fails or cannot be shown, clears the pending state and proceeds gracefully.
     */
    fun showPendingInterstitialAd(
        activity: Activity,
        onDismissed: () -> Unit
    ) {
        if (!RemoteConfigManager.isAdsEnabled || !RemoteConfigManager.isInterstitialAdsEnabled) {
            clearPendingInterstitial(activity)
            onDismissed()
            return
        }

        val analytics = AnalyticsManager.getInstance(activity)
        val ad = cachedInterstitialAd
        if (ad == null) {
            Log.d(TAG, "Pending interstitial could not be shown (not cached). Resuming game.")
            clearPendingInterstitial(activity)
            preloadInterstitialAd(activity.applicationContext)
            onDismissed()
            return
        }

        // Refresh timestamp while showing pending ad
        setPendingInterstitialTime(activity, System.currentTimeMillis())

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Pending interstitial showing.")
                analytics.logAdImpression(adType = "interstitial", adPlacement = "pending_resume")
                cachedInterstitialAd = null
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Pending interstitial dismissed.")
                analytics.logAdDismissed(adType = "interstitial", adPlacement = "pending_resume")
                clearPendingInterstitial(activity)
                setLastInterstitialTime(activity, System.currentTimeMillis())
                preloadInterstitialAd(activity.applicationContext)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Pending interstitial failed to show: ${adError.message}")
                analytics.logAdDisplayFailed(
                    adType = "interstitial",
                    errorCode = adError.code,
                    errorMessage = adError.message,
                    placement = "pending_resume"
                )
                clearPendingInterstitial(activity)
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
