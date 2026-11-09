# Arrow Away: Tap Logic Puzzle — Release Notes

This document maintains a high-level changelog of all official production releases for **Arrow Away: Tap Logic Puzzle**. Each entry provides a brief overview of key additions, improvements, and fixes.

---

## Release History Overview

| Version Name | Version Code | Git Commit | Tag | Date | Status |
|---|---|---|---|---|---|
| **1.0.4** | `6` | Pending | Pending | Nov 2026 | **Prepared (Production Ready)** |
| **1.0.3** | `5` | `96e5c2d` | `v1.0.3` | Nov 2026 | **Published on Google Play** |
| **1.0.2** | `3` | `51d607a9` | `v1.0.2` | Nov 2026 | **Published on Google Play** |
| **1.0.1** | `2` | `b2476eb1` | `v1.0.1` | Nov 2026 | **Published on Google Play** |
| **1.0.0** | `1` | `a1517c90` | `v1.0.0` | Nov 2026 | **Initial Production Launch** |

---

## [1.0.4] - Version Code: 6 - Commit: Pending

### Key Highlights:
* **Dynamic Daily Challenge System & Progression Tuning:**
  * **Dynamic Dual-Pool Daily Puzzles:** Introduced an adaptive daily pool system. Early-game players (< Level 20) are automatically routed to a curated 44-level easy daily pool (81-100 arrows) to prevent early cognitive churn, while experienced players receive high-challenge puzzles (101-252 arrows).
  * **Server-Driven Unlock Threshold:** Centralized Daily Challenge unlock rules into a unified single source of truth (`DailyChallengeRules`), allowing the unlock level threshold to be tuned dynamically via Firebase Remote Config (`daily_challenge_unlock_level = 5`).
  * **Cold-Start Race Protection:** Guarded against asynchronous DataStore initialization delay on cold notification launches by falling back to synchronously mirrored player levels and streaks.
* **Smart Notification Pre-Prompt & Android 13+ Compliance:**
  * **Soft-Ask Dialog:** Implemented a non-intrusive pre-prompt explaining the value of daily reminders before requesting the OS `POST_NOTIFICATIONS` permission, avoiding accidental permanent denial.
  * **Second-Chance Prompt:** Added an intelligent re-prompt when a player achieves a 3-day streak.
  * **Channel Importance Upgrade:** Upgraded notification channel to `daily_challenge_channel_v2` with default importance and dedicated vector branding (`ic_stat_notification`).
  * **Settings Synchronization:** Added a deep-link dialog to app notification settings for permanently denied states, while strictly preserving user choices made in the in-game settings.
* **Content Protection & Dev Mode Hardening:**
  * Strictly restricted Dev Mode gestures, tools, and debug panels to `BuildConfig.DEBUG` builds; release builds force dev mode off.
  * Enforced DataStore sanity limits to prevent coin balance tampering and level skipping.
* **Policy Compliance & Quality:**
  * Integrated Google UMP (User Messaging Platform) GDPR/privacy consent form for EEA/UK compliance.
  * Fixed daily coin payout duplication so completion rewards are strictly granted once per day.
  * Resolved Android cut-out layout lint warning for display cutouts on Android 8.1+ (`values-v27/themes.xml`).

---

## [1.0.3] - Version Code: 5 - Commit: `96e5c2d`

### Key Highlights:
* **High-Challenge Daily Challenge Expansion (235 Levels):**
  * **Curated >100 Arrow Pool:** Integrated 235 Daily Challenge levels (`DailyLevels_1.json` through `DailyLevels_235.json`) with arrow counts strictly exceeding 100 arrows (ranging from 101 to 252 arrows) for a rewarding, deep daily puzzle experience.
  * **Isomorphic Transformations:** Applied mathematical reflections and orthogonal rotations across all 235 puzzles, ensuring completely unique coordinate geometry with 0 raw design duplication while preserving 100% solvability.
  * **Automated Verification:** Verified 100% solvability and constraint satisfaction across all 235 levels via unit test suite (`testDailyLevelsParsingAndSolvability`).
  * **Developer Review Suite:** Added an internal Daily Level Review browser strictly within Developer Mode (`if (isDevMode)`) for instant auditing, board rendering inspection, and level jumping, maintaining a clean production Home Screen.

* **Curated Levels (1–150) Polish & Bug Fixes:**
  * **Shape Orientation Fixes:** Corrected upright orientation across 14 canonical shape levels (Heart, Star, Butterfly, Fish, Mushroom, House, Arrow, Duck, Rabbit, Sword, Anchor, Boat, Tree, Cross) by aligning coordinate transforms.
  * **Level 138 Collision Fix:** Resolved piece collision in Level 138 to ensure valid, smooth solvable clearance path.
  * **Unified Palette:** Standardized levels to single coherent color scheme across all 150 campaign puzzles.
* **Camera & Touch Controls:**
  * **Viewport Drag Boundary Clamping:** Implemented minimum visible area constraint (`calculatePanLimits`) ensuring at least 20% of the puzzle remains visible on screen when panning, preventing the puzzle from drifting completely off-screen.
  * **Zoom Stroke Floor:** Clamped rendering line stroke floor at `1.2 dp` so delicate puzzle shapes retain crisp outline clarity even when zoomed far out.
* **In-App Review Timing & Emotional High Trigger:**
  * Adjusted Google Play In-App Review prompt to trigger upon completing **Level 8** with high performance ($\ge 2$ stars remaining), capturing players during an optimal moment of game satisfaction.
* **Monetization & Analytics Diagnostics:**
  * **Rewarded Ad Completion:** Connected `ad_reward_completed` event in `AdManager` to accurately track rewarded video completion rate.
  * **Ad Health Diagnostics:** Added `ad_load_failed` and `ad_display_failed` analytics tracking with error codes to monitor AdMob/ad network health and protect ad revenue.
  * **Player Progression Insights:** Added Firebase User Property `max_level_reached` and tracked Game Over decisions (`replay`, `return_home`) for churn and funnel analysis.
  * **Telemetry Cleanliness & AdMob Collision Prevention:** Deduplicated redundant `screen_view` and `level_start` calls, and renamed custom impression logging to `ad_shown` to prevent collisions with AdMob's automated, revenue-carrying `ad_impression` event.

---

## [1.0.2] - Version Code: 3 - Commit: `51d607a9`

### Key Highlights:
* **Anti-Bypass Ad Protection:**
  * Implemented a persistent pending ad state (`5-minute TTL`) to prevent skipping interstitial ads by force-quitting the app.
  * Tapping "PLAY" after an interrupted ad gracefully serves the pending interstitial; auto-skips in 0 ms if offline or blocked.
  * Unified the Victory Screen so tapping either "Next Level" or "Home" consistently checks interstitial pacing.
* **Persistent Ad Cooldown:**
  * Persisted the 6-minute ad cooldown timestamp across app restarts in `SharedPreferences`.
* **Cinematic Level Completion Celebration:**
  * Extended completion delay from 950 ms to **1.8 seconds**.
  * Replaced momentary dot flash with a **persistent glowing golden silhouette hold** so players can clearly see the solved shape.
  * Added ambient radial golden bloom and 16 stardust sparkle particles.
  * Added **Tap-to-Skip**: Players can tap the board during celebration to immediately proceed to the Victory Screen.
* **UI & Streak Polish:**
  * Animated Victory screen streak indicator with flame-to-chip transition.
  * Added Game Progress Reset with confirmation dialog in Settings.
  * Streamlined in-game settings modal (Sound, Themes, Controls).
  * Removed deprecated Lock Puzzle feature and simplified dev options.
* **Documentation:**
  * Added complete [**`ADS_SPECIFICATION.md`**](ADS_SPECIFICATION.md) covering inventory, pacing, Remote Config schema, and policy compliance.

---

## [1.0.1] - Version Code: 2 - Commit: `b2476eb1`

### Key Highlights:
* **Tactile 3D App Icon & Branding:**
  * Updated app icon to the new tactile 3D 5-arrow puzzle logo with zero-crop safe-zone squircle padding.
  * Eliminated corner rendering artifacts and enlarged branding for high-density Android displays.
* **Store Listing Assets:**
  * Updated Google Play Store listing feature graphics and device screenshots (Phone & 7-inch Tablet).
* **Settings Improvements:**
  * Disabled hint timer settings when the hint button is turned off.
  * Added informational dialogs explaining hint timers and grid guides.
  * Auto-reset grid guide lines on level transition.

---

## [1.0.0] - Version Code: 1 - Commit: `a1517c90`

### Key Highlights:
* **Initial Production Release on Google Play:**
  * Core gameplay mechanics: Arrow clearance, ray collision detection, and tactile puzzle interactions.
  * Campaign levels with progressive grid sizes and curated difficulty curves.
  * Daily Challenge mode with date-based puzzles and calendar tracking.
  * Booster items: Hint system, Hammer breaker, Magic Wand auto-clear, and Corridor Guide lines.
  * 6 Tactile Theme Presets (Clean Light, Warm Sepia, Midnight Dark, Calm Pastel, Mint Fresh, Sunset Rose).
  * Native audio soundscapes and rich haptic vibration feedback.
  * Google AdMob integration (Adaptive Banners, Interstitials, Rewarded Ads) with Firebase Remote Config toggles.
  * Native debug symbol packaging and R8 ProGuard optimizations for Android 14/15.

---

## Guidelines for Future Releases

When publishing future releases:
1. Increment `versionCode` by 1 and update `versionName` in `arrow_native/app/build.gradle.kts`.
2. Add a new row to the **Release History Overview** table with the version, code, commit hash, and tag.
3. Add a concise section highlighting major gameplay, UI, performance, or monetization changes.
4. Tag the release commit using `git tag -a v<versionName> <commitHash> -m "Release <versionName> (versionCode <versionCode>)"` and push with `git push origin v<versionName>`.
