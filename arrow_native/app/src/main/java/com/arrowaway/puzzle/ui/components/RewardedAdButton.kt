package com.arrowaway.puzzle.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arrowaway.puzzle.ads.AdManager.RewardedStatus
import com.arrowaway.puzzle.ui.theme.ThemePreset
import kotlinx.coroutines.delay

/** How long "Loading ad…" may spin before we tell the player no ad is available. */
private const val LOADING_TIMEOUT_MS = 8000L

/** A load still running after the timeout is shown to the player as "no ad available". */
fun effectiveRewardedStatus(status: RewardedStatus, timedOut: Boolean): RewardedStatus =
    if (status == RewardedStatus.LOADING && timedOut) RewardedStatus.NO_FILL else status

/** What a rewarded ad gives, so messages can name it. */
enum class RewardKind(val noun: String, val adLabel: String) {
    HINT("hint", "Hint ads"),
    LIFE("life", "Ads")
}

/**
 * Friendly, blame-free reason why no rewarded ad can play. Never says "your internet" or
 * "your ad blocker"; only "You're offline" because the player can fix that right away.
 */
fun rewardedStatusMessage(status: RewardedStatus, reward: RewardKind = RewardKind.HINT): String? = when (status) {
    RewardedStatus.NO_INTERNET -> "You're offline. Connect to the internet to get a free ${reward.noun}."
    RewardedStatus.BLOCKED -> "${reward.adLabel} can't load on this connection right now."
    RewardedStatus.NO_FILL -> "No ads available right now. Please try again in a moment."
    RewardedStatus.ERROR -> "The ad couldn't load. Please try again in a moment."
    RewardedStatus.LOADING, RewardedStatus.READY -> null
}

/**
 * The once-a-day free hint is allowed ONLY when AdMob answered "no ad available" (NO_FILL).
 * Never for offline, blocked ads, load errors, a slow load, or when ads look blocked.
 * (The once-per-day limit itself is enforced by UserPreferencesRepository.claimNoFillFreeHint.)
 */
fun isNoFillFreeHintEligible(status: RewardedStatus, stillLoading: Boolean, adBlockSuspected: Boolean): Boolean =
    status == RewardedStatus.NO_FILL && !stillLoading && !adBlockSuspected

/** Shown when an ad is still loading after the wait limit. */
const val REWARDED_SLOW_MESSAGE = "The ad is taking a while. Please try again in a moment."

/** Shown in the hint bubble while a hint ad loads. */
const val REWARDED_LOADING_MESSAGE = "Loading ad…"

/**
 * "Watch Ad" button for reward dialogs.
 * - READY: shows [readyText] and plays the ad.
 * - LOADING: spinner + "Loading ad…" (disabled) for up to 8 s.
 * - NO_INTERNET / NO_FILL / ERROR: a plain-language reason above a "Retry" button.
 *
 * [onRequestAd] is called when the dialog opens and on every Retry.
 */
@Composable
fun RewardedAdButton(
    status: RewardedStatus,
    readyText: String,
    containerColor: Color,
    theme: ThemePreset,
    onWatchAd: () -> Unit,
    onRequestAd: () -> Unit,
    modifier: Modifier = Modifier,
    reward: RewardKind = RewardKind.LIFE
) {
    var attempt by remember { mutableIntStateOf(0) }
    var timedOut by remember { mutableStateOf(false) }

    LaunchedEffect(attempt) {
        timedOut = false
        onRequestAd()
        delay(LOADING_TIMEOUT_MS)
        timedOut = true
    }

    val shown = effectiveRewardedStatus(status, timedOut)
    val message = if (status == RewardedStatus.LOADING && timedOut) REWARDED_SLOW_MESSAGE
        else rewardedStatusMessage(shown, reward)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
        if (message != null) {
            Text(
                text = message,
                fontSize = 13.sp,
                color = theme.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        AppPrimaryButton(
            text = when (shown) {
                RewardedStatus.READY -> readyText
                RewardedStatus.LOADING -> "Loading ad…"
                else -> "Retry"
            },
            onClick = {
                if (shown == RewardedStatus.READY) onWatchAd() else attempt++
            },
            enabled = shown != RewardedStatus.LOADING,
            isLoading = shown == RewardedStatus.LOADING,
            theme = theme,
            containerColor = containerColor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
