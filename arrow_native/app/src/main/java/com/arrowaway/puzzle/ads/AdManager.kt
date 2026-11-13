package com.arrowaway.puzzle.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

    /** What the "Watch Ad" buttons should show. LOADING is also the state before the first request. */
    /**
     * LOADING is also the state before the first request. BLOCKED = the request failed with a
     * network error while the device is online and ads look blocked (Private DNS / ad blocker).
     */
    enum class RewardedStatus { LOADING, READY, NO_INTERNET, NO_FILL, BLOCKED, ERROR }

    private val _rewardedStatus = MutableStateFlow(RewardedStatus.LOADING)
    val rewardedStatus: StateFlow<RewardedStatus> = _rewardedStatus.asStateFlow()

    private fun setRewardedStatus(status: RewardedStatus) {
        _rewardedStatus.value = status
        _rewardedReady.value = status == RewardedStatus.READY
    }

    /**
     * Why the last rewarded load failed (NO_FILL / BLOCKED / ERROR / NO_INTERNET), kept until an ad
     * loads successfully. A tap can then answer instantly instead of waiting on another request
     * that will most likely fail the same way.
     */
    @Volatile private var lastRewardedFailure: RewardedStatus? = null
    @Volatile private var lastFailureWasNetworkError = false

    /** The known reason no rewarded ad is available, or null if none is known (or an ad is cached). */
    fun knownRewardedFailure(): RewardedStatus? =
        if (cachedRewardedAd != null && !isRewardedAdExpired()) null else lastRewardedFailure

    /**
     * Player-facing reason for a rewarded load failure.
     * - Offline -> NO_INTERNET.
     * - Online but the request couldn't reach the ad server (network error code, or an "unable to
     *   resolve host" / connection message, which DNS blockers produce) -> BLOCKED, shown as
     *   "Ads can't load on this connection right now" (true for blocking and flaky networks alike).
     * - Ad server name sinkholed (0.0.0.0 / no answer) -> BLOCKED whatever the code.
     * - Private DNS on and an unexplained internal error -> BLOCKED.
     * - No fill -> NO_FILL (the only case that can earn the daily free hint).
     * - Anything else -> ERROR.
     */
    fun classifyRewardedFailure(
        errorCode: Int,
        errorMessage: String?,
        online: Boolean,
        adDomainBlocked: Boolean,
        privateDnsActive: Boolean
    ): RewardedStatus {
        if (!online) return RewardedStatus.NO_INTERNET
        if (adDomainBlocked) return RewardedStatus.BLOCKED
        if (isConnectionFailure(errorCode, errorMessage)) return RewardedStatus.BLOCKED
        return when (errorCode) {
            AdRequest.ERROR_CODE_NO_FILL, AdRequest.ERROR_CODE_MEDIATION_NO_FILL -> RewardedStatus.NO_FILL
            AdRequest.ERROR_CODE_INTERNAL_ERROR -> if (privateDnsActive) RewardedStatus.BLOCKED else RewardedStatus.ERROR
            else -> RewardedStatus.ERROR
        }
    }

    /** True when the request never reached the ad server (network error or DNS / connect failure text). */
    fun isConnectionFailure(errorCode: Int, errorMessage: String?): Boolean {
        if (errorCode == AdRequest.ERROR_CODE_NETWORK_ERROR) return true
        val m = errorMessage?.lowercase() ?: return false
        return CONNECTION_FAILURE_HINTS.any { m.contains(it) }
    }

    private val CONNECTION_FAILURE_HINTS = listOf(
        "unable to resolve host", "unknownhost", "no address associated",
        "failed to connect", "connection refused", "error while connecting", "network error"
    )

    // ---- Ad-block detection (Private DNS / DNS ad blockers) ----
    private const val AD_PROBE_HOST = "googleads.g.doubleclick.net"
    @Volatile private var adDomainBlocked = false
    @Volatile private var adBlockProbeRunning = false

    /** True when ads look blocked on this connection (ad domain sinkholed, or Private DNS on). */
    fun isAdBlockSuspected(context: Context): Boolean = adDomainBlocked || isPrivateDnsActive(context)

    private fun isPrivateDnsActive(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) return false
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            cm.getLinkProperties(cm.activeNetwork)?.isPrivateDnsActive == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Background check (never on a tap): does the ad server's name resolve to a real address?
     * DNS ad blockers answer 0.0.0.0 / 127.0.0.1 / :: or nothing at all.
     */
    @Volatile private var lastAdBlockProbeMs = 0L

    fun refreshAdBlockSignal(context: Context) {
        val now = System.currentTimeMillis()
        if (adBlockProbeRunning || now - lastAdBlockProbeMs < 15_000L) return
        adBlockProbeRunning = true
        lastAdBlockProbeMs = now
        val appContext = context.applicationContext
        Thread {
            val blocked = try {
                val addrs = java.net.InetAddress.getAllByName(AD_PROBE_HOST)
                addrs.isEmpty() || addrs.all { it.isAnyLocalAddress || it.isLoopbackAddress }
            } catch (e: java.net.UnknownHostException) {
                isOnline(appContext) // online but the ad server name doesn't resolve
            } catch (e: Exception) {
                false
            }
            mainHandler.post {
                adDomainBlocked = blocked
                adBlockProbeRunning = false
                // Re-label an unexplained failure once we know ads are blocked
                if (lastRewardedFailure == RewardedStatus.ERROR &&
                    (blocked || (lastFailureWasNetworkError && isAdBlockSuspected(appContext)))
                ) {
                    lastRewardedFailure = RewardedStatus.BLOCKED
                    if (_rewardedStatus.value == RewardedStatus.ERROR) setRewardedStatus(RewardedStatus.BLOCKED)
                }
            }
        }.apply { isDaemon = true; name = "ad-block-probe" }.start()
    }

    /** Maps an AdMob load error code to a player-facing category (raw message still goes to analytics). */
    fun statusForLoadError(errorCode: Int): RewardedStatus = when (errorCode) {
        AdRequest.ERROR_CODE_NETWORK_ERROR -> RewardedStatus.NO_INTERNET
        AdRequest.ERROR_CODE_NO_FILL, AdRequest.ERROR_CODE_MEDIATION_NO_FILL -> RewardedStatus.NO_FILL
        else -> RewardedStatus.ERROR
    }

    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true // If we can't tell, let the ad request decide
        }
    }

    /**
     * Called when a reward dialog opens or the player taps Retry: loads right away (skipping the
     * backoff wait), or reports NO_INTERNET immediately when the device is offline.
     */
    fun requestRewardedNow(context: Context) {
        if (cachedRewardedAd != null && !isRewardedAdExpired()) {
            setRewardedStatus(RewardedStatus.READY)
            return
        }
        if (!isOnline(context)) {
            setRewardedStatus(RewardedStatus.NO_INTERNET)
            return
        }
        setRewardedStatus(RewardedStatus.LOADING)
        preloadRewardedAd(context)
    }

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
            setRewardedStatus(RewardedStatus.LOADING)
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

            refreshAdBlockSignal(context)
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
            setRewardedStatus(RewardedStatus.LOADING)
        }
        if (cachedRewardedAd != null || isRewardedLoading) return

        rewardedRetryRunnable?.let { mainHandler.removeCallbacks(it) }
        rewardedRetryRunnable = null

        isRewardedLoading = true
        setRewardedStatus(RewardedStatus.LOADING)
        val adRequest = AdRequest.Builder().build()
        val unitId = getRewardedAdUnitId()

        RewardedAd.load(appContext, unitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                cachedRewardedAd = ad
                rewardedAdLoadedTimeMs = System.currentTimeMillis()
                isRewardedLoading = false
                rewardedRetryAttempt = 0
                lastRewardedFailure = null
                lastFailureWasNetworkError = false
                setRewardedStatus(RewardedStatus.READY)
                Log.d(TAG, "Rewarded ad successfully preloaded (Unit: $unitId).")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                cachedRewardedAd = null
                isRewardedLoading = false
                // A "network error" while the device is online usually means ads are blocked
                // (private DNS / ad blocker), so don't tell the player their internet is off.
                val online = isOnline(appContext)
                val finalStatus = classifyRewardedFailure(
                    errorCode = loadAdError.code,
                    errorMessage = loadAdError.message,
                    online = online,
                    adDomainBlocked = adDomainBlocked,
                    privateDnsActive = isPrivateDnsActive(appContext)
                )
                lastFailureWasNetworkError = online && isConnectionFailure(loadAdError.code, loadAdError.message)
                lastRewardedFailure = finalStatus
                setRewardedStatus(finalStatus)
                // Refresh the ad-block signal after any failure (cheap, throttled, off the main thread)
                refreshAdBlockSignal(appContext)
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
            setRewardedStatus(RewardedStatus.LOADING)
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
                setRewardedStatus(RewardedStatus.LOADING)
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed. Reward granted: $rewardGranted")
                analytics.logAdDismissed(adType = "rewarded", adPlacement = placement, rewardEarned = rewardGranted)
                setRewardedStatus(RewardedStatus.LOADING)
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
                setRewardedStatus(RewardedStatus.LOADING)
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
                AnalyticsManager.getInstance(activity).logInterstitialSkipped("cooldown", currentLevel)
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
            Log.d(TAG, "Interstitial skipped: no ad loaded yet.")
            analytics.logInterstitialSkipped("not_ready", currentLevel)
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
