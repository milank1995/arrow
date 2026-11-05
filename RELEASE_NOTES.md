# Arrow Away: Tap Logic Puzzle — Release Notes

This document maintains a high-level changelog of all official production releases for **Arrow Away: Tap Logic Puzzle**. Each entry provides a brief overview of key additions, improvements, and fixes.

---

## Release History Overview

| Version Name | Version Code | Git Commit | Tag | Date | Status |
|---|---|---|---|---|---|
| **1.0.2** | `3` | `51d607a9` | `v1.0.2` | Nov 2026 | **Prepared (Production Ready)** |
| **1.0.1** | `2` | `b2476eb1` | `v1.0.1` | Nov 2026 | **Published on Google Play** |
| **1.0.0** | `1` | `a1517c90` | `v1.0.0` | Nov 2026 | **Initial Production Launch** |

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
