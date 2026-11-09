# Arrow Away — Change Requests for v1.0.4

- **Project:** `arrow_native` (Kotlin + Jetpack Compose, minSdk 26, targetSdk 36)
- **Branch:** `feature/curated-levels-pack` (the published code)
- **Context:** read `HANDOFF-2026-11-09.md` first. It lists the uncommitted consent (UMP) work already in the working tree. Do not revert it.

All paths below are relative to `app/src/main/java/com/arrowaway/puzzle/` unless stated otherwise.

---

## CR-1 — Daily Challenge unlock level must come from Remote Config everywhere

**Why:** we want to move the unlock from level 10 to level 5 without an app update. `RemoteConfigManager.dailyChallengeUnlockLevel` exists, but several places hard-code `10`.

**Changes:**
1. **`ui/screens/HomeScreen.kt` ~line 116–117:** replace `uiState.currentLevelNumber > 10 || uiState.lastCompletedLevelNumber >= 10 || …` with the Remote Config value (`> unlockLevel` / `>= unlockLevel`). Keep the existing `hasSeenDailyUnlockCelebration` condition.
2. **`ui/screens/HomeScreen.kt` ~line 559:** change `"Unlocks after Level 10"` to `"Unlocks after Level $unlockLevel"`.
3. **`notification/DailyReminderReceiver.kt` ~line 35:** replace `currentLevel > 10` with `currentLevel > RemoteConfigManager.dailyChallengeUnlockLevel`. Also fix the stale comment that says "Level 5" or "Level 10".
4. **Check every other use:** `GameViewModel.kt` ~line 156 and `MainActivity.kt` ~lines 187 and 228 already use Remote Config. Leave them.
5. **Single source of truth:** add a helper so all callers use the same rule, e.g. `fun isDailyUnlocked(currentLevel: Int, lastCompleted: Int, seenCelebration: Boolean): Boolean`. Put it in `GameViewModel` or a small `DailyChallengeRules` object.
6. **Defaults stay 10:** keep the Remote Config default `daily_challenge_unlock_level` at **10** in `RemoteConfigManager` (`DEFAULT_APP_CONFIG_JSON`) and in the Firebase console. The owner changes it to 5 in Firebase after release (see CR-6).
7. **Players who already unlocked keep it:** a player who already unlocked dailies must never be locked again if the value is raised later. The `hasSeenDailyUnlockCelebration` flag already covers this; keep it.

**Do not change:** the interstitial start level (`interstitial_min_level` = 10) or the in-app review milestone (level 8).

**Acceptance:**
- Remote Config set to 5: after completing level 5, the unlock celebration appears, the Home Daily button is unlocked, the locked text reads "Unlocks after Level 5", and the 19:30 reminder is allowed.
- Remote Config at 10: behaviour is identical to today.

---

## CR-2 — Easier daily puzzles for new players

**Why:** all 235 daily levels have 101–252 arrows (median 134), but campaign level 5 has 55 arrows and level 10 has 70. A new player meeting a 134-arrow puzzle is likely to give up.

**Easy levels already exist in git history.** Commit `7932741` contained 279 daily levels. Commit `4380169` removed the **44 levels with 81–100 arrows**.

**Changes:**
1. **Recover the 44 easy levels:** restore the files from `7932741` with `ArrowCount <= 100` (path `arrow_native/app/src/main/assets/levels/DailyLevels_N.json`). Save them as `assets/levels/DailyEasy_1.json` … `DailyEasy_44.json`, renumbered and sorted by `ArrowCount` ascending.
   - Run each through the existing solvability test (pattern of `testDailyLevelsParsingAndSolvability` in `app/src/test/.../PuzzleEngineTest.kt`).
   - Make sure none duplicates a current daily or campaign level.
   - Optional: if the team can create 50–80 arrow dailies, add them to the start of the easy pool.
2. **`core/engine/LevelLoader.kt`:** add `const val TOTAL_EASY_DAILY_LEVELS = 44` and `fun loadEasyDailyLevel(context, index)`, mirroring `loadDailyLevel`, with the same fallback behaviour.
3. **`game/GameViewModel.kt` `startDailyChallenge`:** choose the pool by player progress.
   - **Easy pool** if `currentLevel < easyDailyUntilLevel` (Remote Config `app_config.easy_daily_until_level`, default **20**).
   - **Otherwise** the current pool.
   - Easy index: `((dayOfYear - 1) % TOTAL_EASY_DAILY_LEVELS) + 1`.
4. **Calendar rules:** the date stays the key for completion and streaks. Only the level content changes. A player who replays a past date after passing the threshold may get a different level. That is acceptable, but completion and streak must still be recorded per date.
5. **Remote Config:** add `easy_daily_until_level` to `RemoteConfigManager` (default 20) and to `DEFAULT_APP_CONFIG_JSON`.
6. **Analytics:** add a `daily_pool` parameter (`"easy"` / `"standard"`) to the existing `daily_challenge_start` and `daily_challenge_complete` events.

**Acceptance:**
- A player below level 20 gets an 81–100 arrow daily.
- A player at level 20 or above gets the existing 101+ pool.
- Streaks and completion keep working across the switch.
- Unit tests pass, including solvability of all easy levels.

---

## CR-3 — Android notification permission flow (POST_NOTIFICATIONS, Android 13+)

**Current behaviour to keep:**
- The system prompt appears when the Daily Challenge unlocks.
- `NotificationHelper.postDailyChallengeNotification` returns early without permission.
- On Android 12 and below, permission is automatic.

**Changes:**
1. **Pre-prompt dialog:** before calling `notificationPermissionLauncher.launch(POST_NOTIFICATIONS)` automatically, show our own dialog: "Get a reminder when each new Daily Challenge is ready?" with **Turn on** / **Not now**.
   - Launch the Android prompt only on **Turn on**.
   - "Not now" does **not** count as an Android denial.
   - Applies to `MainActivity.kt` ~line 185 (auto request) and the unlock celebration `onRequestNotificationPermission`.
2. **Second chance:** if the user picked "Not now" or denied the Android prompt once, ask again **once** when their daily streak first reaches 3 ("Protect your streak with a reminder?"). Store the attempt count in DataStore, e.g. `notification_permission_ask_count`. Never ask automatically more than twice in total.
3. **Permanently denied:** when the user turns on "Daily Reminder" in Settings but Android will not show the prompt (`!shouldShowRequestPermissionRationale` after a previous denial), show a dialog with a button that opens the app's notification settings (`Settings.ACTION_APP_NOTIFICATION_SETTINGS` with `EXTRA_APP_PACKAGE`).
4. **Keep the toggle in sync:** in `MainActivity.onResume`, re-check `NotificationHelper.hasNotificationPermission()`. If permission was revoked in Android settings, set `dailyRemindersEnabled = false` and cancel the alarm. If it was granted there and reminders were previously wanted, re-enable and schedule.
5. **Small icon:** `NotificationHelper` uses `setSmallIcon(R.mipmap.ic_launcher)`. Create a white-on-transparent vector `res/drawable/ic_stat_notification.xml` (simple arrow glyph) and use it. Optionally `setColor(...)` with the brand colour.
6. **Channel importance:** change `IMPORTANCE_HIGH` to `IMPORTANCE_DEFAULT` and `PRIORITY_HIGH` to `PRIORITY_DEFAULT`. Existing installs keep the old importance unless the channel ID changes. Accept that, or create a new channel ID such as `daily_challenge_channel_v2` and delete the old one.
7. **Click tracking:** when the app opens with `EXTRA_NAVIGATE_TO == "daily_challenge"`, make sure `analytics.logDailyNotificationClicked(...)` fires once.
8. **New analytics events:** `notification_preprompt_shown`, `notification_preprompt_result` (`accept` / `not_now`), `notification_permission_result` (`granted` / `denied`).

**Acceptance (Android 13+ device):**
- **Fresh install:** no prompt before the unlock level.
- **At unlock:** our dialog appears first. "Not now" shows no system prompt, and at a 3-day streak the dialog appears again exactly once.
- **Revoked in Android settings:** the in-app toggle shows Off after returning to the app.
- **Notification look:** the notification shows a proper monochrome icon.

---

## CR-4 — Level content protection

**Current risk:** all 385 levels ship as plain JSON in `assets/levels/`. Unzipping the APK exposes them. The hidden dev-mode gesture also unlocks every level in the live app.

1. **(Must do) Dev mode only in debug builds:**
   - In `ui/screens/SettingsScreen.kt` (~line 747, the 10-second press on the version text), register the gesture only when `BuildConfig.DEBUG` is true.
   - In `GameViewModel.toggleDevMode()`, return early unless `BuildConfig.DEBUG`.
   - On startup in release builds, force any stored `isDevMode = true` back to `false`.
   - `MainActivity` `AppScreen.DEV_TEST` is already gated by `isDevMode`; keep that.
2. **(Recommended) Encrypt level assets at build time:**
   - Add a Gradle task (or a script in `tools/`) that bundles all level JSON into one encrypted file, e.g. `assets/levels.bin` (AES-GCM, gzip before encryption). Leave no plain `.json` level files in the release APK.
   - `LevelLoader` decrypts and decompresses in memory and caches the parsed index.
   - Do not store the key as one plain string. Derive it at runtime from several pieces, and rely on R8 (already enabled, `isMinifyEnabled = true`).
   - Keep the plain JSON in the source tree for development, and keep the unit tests reading from source.
   - Update `PuzzleEngineTest` to also decrypt `levels.bin` and confirm it matches the source JSON.
3. **(Recommended) Saved data:**
   - In `AndroidManifest.xml`, either set `android:allowBackup="false"`, or add `dataExtractionRules` / `fullBackupContent` that exclude the DataStore preferences file. Discuss with the owner first: backups also restore progress for honest users.
   - Add sanity limits when reading DataStore. For example, coins are capped to a sensible maximum, and `currentLevel` can't exceed the highest completed level + 1.
4. **(Optional, later) Date manipulation:** do not allow a daily date more than one day after the last trusted time (the time of the last successful Remote Config fetch, saved in DataStore).

**Acceptance:**
- Release build: holding the version text does nothing.
- `unzip app-release.apk` shows no readable level JSON.
- All levels still load in the game, and unit tests pass.

---

## CR-5 — Housekeeping in the same release
1. **Version:** bump to `versionCode = 6`, `versionName = "1.0.4"` in `app/build.gradle.kts` and update the release notes.
2. **Lint:** move `android:windowLayoutInDisplayCutoutMode` from `res/values/themes.xml` to `res/values-v27/themes.xml`. This fixes the lint NewApi error.
3. **Consent work:** test the UMP consent flow on a device (see `HANDOFF-2026-11-09.md` §1b), then commit it together with these changes or before them.
4. **Out of scope unless the owner asks:**
   - Daily coin double-pay.
   - Daily replay coin farming.
   - Release signing.

---

## CR-6 — After the release is live (owner, in consoles)
1. **Firebase Remote Config `app_config`:**
   - Set `daily_challenge_unlock_level` to **5**.
   - Add `easy_daily_until_level` = **20**.
   - Publish.
2. **GA4:** compare reach of levels 5 and 10 (custom dimension `level_number`) and D1/D7 retention for 2–3 weeks before and after.
3. **Google Ads:** re-add the EU countries now that the consent form ships.
