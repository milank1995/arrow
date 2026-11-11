# Arrow Away — Change Requests for v1.0.5

- **Project:** `arrow_native`
- **Branch:** `feature/curated-levels-pack` (published code, currently 1.0.4 / versionCode 6)
- **Paths:** relative to `app/src/main/java/com/arrowaway/puzzle/` unless stated otherwise.
- **Why this release:** data from 4–10 Nov shows rewarded ads failing for 72% of players who tap "Watch Ad" (92 taps → 26 ads shown). GA4 now records the reason: mostly `Unable to obtain a JavascriptEngine` (ad requested while the app is in the background) caused by an endless 20–25 s retry loop. Real "No fill" is rare (16).
- **Scope:** keep this release small. Only CR-7 to CR-11.

---

## CR-7 — Fix the ad loading retry loop (must do)

**Problem:**
- `ads/AdManager.kt` retries a failed load forever on a fixed timer:
  - `preloadRewardedAd` → `postDelayed(…, 20000L)` (~line 179)
  - `preloadInterstitialAd` → `postDelayed(…, 25000L)` (~line 289)
- This keeps running in the background, which produces the JavascriptEngine errors.
- One user logged about 290 failures in a week.

**Changes:**
1. **Exponential backoff per format.**
   - Delays: 30 s → 60 s → 120 s → 240 s → 600 s. Stay at 600 s after that.
   - Keep a separate attempt counter for rewarded and for interstitial.
   - Reset the counter to 0 on `onAdLoaded`.
2. **No requests while the app is in the background.**
   - Add `androidx.lifecycle:lifecycle-process:2.8.7`.
   - Observe `ProcessLifecycleOwner` (e.g. in `ArrowAwayApplication`).
   - `ON_STOP`: set `isAppInForeground = false` and cancel any pending retry runnables. Keep references to the runnables and call `mainHandler.removeCallbacks(runnable)`.
   - `ON_START`: set `isAppInForeground = true`, then call `preloadRewardedAd` and `preloadInterstitialAd`. Both still return early if an ad is cached or already loading.
   - `preloadRewardedAd` and `preloadInterstitialAd` must return early when `!isAppInForeground`.
3. **Ad expiry.**
   - Save the load time of each cached ad.
   - If a cached ad is older than 55 minutes, discard it and load a fresh one. AdMob ads expire after 1 hour.
4. **Better failure analytics.** Add these parameters to the existing `ad_load_failed` event:
   - `retry_attempt` (Int)
   - `next_retry_sec` (Int)
   - `in_foreground` (Boolean)

   Keep the existing `ad_type`, `error_code` and `error_message`.

**Acceptance:**
- **Airplane mode:** failures are logged at about 30 s, 60 s, 120 s… apart, not every 20 s.
- **Backgrounded app:** with the app in the background for 5 minutes, there are zero ad requests or failures (check Logcat tag `ArrowAwayAds`).
- **Return to foreground:** one load attempt per format, then normal behaviour.
- **Test ads:** with test ads on (debug build), rewarded and interstitial still load and show as before.

---

## CR-8 — Only offer "Watch Ad" when an ad is ready (must do)

**Problem:**
- "Watch Ad (+3 Lives)" (`ui/components/GameOverDialog.kt` ~line 120) and "Watch Ad (+1 Hint)" (`ui/components/HintRefillDialog.kt` ~line 115) are always enabled.
- When no ad is cached, `AdManager.showRewardedAd` shows the toast "Ad request failed. Please try again later." That is a bad experience, and the ad is lost.

**Changes:**
1. **Ready state in `AdManager`:** expose `val rewardedReady: StateFlow<Boolean>`. Set it true in `onAdLoaded`, and false when the ad is shown, fails, or expires.
2. **Preload early:** when a gameplay level starts (campaign or daily), call `AdManager.preloadRewardedAd(context)`. The ad is then usually ready before the player needs it. Call it from `MainActivity` when the screen becomes `AppScreen.GAMEPLAY`, or from the level-load path.
3. **Both dialogs:**
   - **Ready:** the button reads "Watch Ad (+3 Lives)" / "Watch Ad (+1 Hint)" and is enabled.
   - **Not ready:** the button reads "Loading ad…" with a small spinner and is disabled. Trigger `preloadRewardedAd` when the dialog opens.
   - **Still not ready after 8 s:** show "No ad available right now" and keep the existing secondary option (close / give up).
   - **No toasts:** remove the "Ad request failed" toast in this path.
4. **Copy fix:** in `HintRefillDialog.kt`, the comment says "+3 Hints" but the reward is 1 hint. Fix the comment only; the reward stays at 1.
5. **Analytics:**
   - Log `ad_reward_prompt_shown` with a new parameter `ad_ready` (true/false).
   - If the dialog closes without an ad because none was ready, log `ad_reward_unavailable` (`placement`).

**Acceptance:**
- **Airplane mode:** the dialogs show "Loading ad…", then "No ad available right now". No toast appears.
- **Online:** the button is enabled within a few seconds of the level starting. Tapping it plays the ad and grants the reward.
- **Daily Challenge** behaves the same way.

---

## CR-9 — In-app review prompt: earlier and more often at happy moments (should do)

**Current behaviour** (`review/InAppReviewManager.kt`, called from `game/GameViewModel.kt` ~line 1199):
- The prompt appears at level 8 with 2 or more stars, at a daily streak of 3 or 7, or after a perfect 3-star 100-point clear from level 8.
- There's a 30-day cooldown.
- **Result: only 3 prompts all week.** The app has no public rating yet, which hurts ranking and store conversion.

**Changes:**
1. **Remote Config only (owner, no code):** set `app_config.review_prompt_min_level` from **8 to 5**. It's already read via `RemoteConfigManager.reviewPromptMinLevel`.
2. **Code:** add one new trigger in `checkReviewEligibility` for the **first Daily Challenge completed** (`isDailyMode` and the player's total completed dailies == 1).
   - Pass the count in from `GameViewModel`: `_uiState.value.completedDailyDates.size`, counted after today's date is added.
   - Trigger reason: `first_daily_complete`.
3. **Keep the safety rules:**
   - The 30-day cooldown stays.
   - Never prompt after a failed or restarted level.
   - Never prompt before level 5.

**Acceptance (debug build):** `FakeReviewManager` fires on:
- Level 5 cleared with 2 or more stars.
- The first daily completed.
- A 3-day streak.

It does not fire on level 4, after a fail, or within the cooldown.

---

## CR-10 — Verify `level_number` reaches GA4 (check first, fix only if needed)

**Problem:**
- In GA4 the custom dimension "Level number" (`level_number`, registered 9 Nov) shows `(not set)` for every level event on 10–11 Nov.
- Dimensions registered a day later (ad error message, max level) work.
- The code sends `putInt("level_number", …)` correctly (`analytics/AnalyticsManager.kt` lines 136, 148, 165, 187, 202).

**Steps:**
1. **Debug build.** Analytics is off by default in debug builds (`isAnalyticsEnabled = !BuildConfig.DEBUG`), so turn on the analytics toggle in Settings first.
2. **Enable DebugView:**

   ```
   adb shell setprop debug.firebase.analytics.app com.arrowaway.puzzle
   ```

3. **Play one level, then check Firebase → DebugView:**
   - The `level_start` and `level_complete` events must show the parameter `level_number` with the right value.
4. **If the parameter is present:** no code change. It's a GA4 processing delay. Note this in the PR and close the CR.
5. **If it's missing or renamed:** fix the bundle key and add a unit test for the bundle contents.

---

## CR-11 — Release housekeeping

1. **Version:** `versionCode = 7`, `versionName = "1.0.5"` in `app/build.gradle.kts`.
2. **Release notes (en-US):**
   - Smoother ads: no more "Ad request failed" pop-ups.
   - Ads now load only while you're playing.
   - Small fixes and improvements.
3. **Tests:**
   - All unit tests pass.
   - Add a small unit test for the backoff sequence: 30, 60, 120, 240, 600, 600.

---

## Not in this release
- **Interstitial pacing:** start level 10 → 6 and cooldown 360 s → 210 s. This is a Remote Config change by the owner, made **one week after 1.0.5 is live**, once ad loading is fixed.
- **Level difficulty changes:** wait for real level data (CR-10).
- **Custom-shapes branch:** not shipping yet.
  - Before it ships, remove Pikachu, Mickey Mouse, JCB and Android.
  - Fix Face and FrogAndBalloon (head direction mismatch).
  - Fix Whale (overlapping cells).
  - Crop oversized grids.
- **Level encryption and `allowBackup`:** still open from CR-4. Not part of this release.

## After release (owner, in consoles)
1. **Remote Config:** `review_prompt_min_level` = 5 (CR-9).
2. **Play Console:** upload the new listing (`STORE-LISTING-2026-11-11.md`, `store-assets/samples/`).
3. **One week later, compare with 4–10 Nov:**

   | Metric | 4–10 Nov baseline |
   |---|---|
   | Rewarded "Watch Ad" taps → ads shown | 28% |
   | `ad_load_failed` per user | ~290 |
   | Revenue per user | $0.002 |
   | Ratings count | 0 |
