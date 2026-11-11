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

fun rewardedStatusMessage(status: RewardedStatus): String? = when (status) {
    RewardedStatus.NO_INTERNET -> "No internet connection. Connect to watch an ad."
    RewardedStatus.NO_FILL -> "No ads available right now. Please try again in a moment."
    RewardedStatus.ERROR -> "Ad couldn't load. Please try again later."
    RewardedStatus.LOADING, RewardedStatus.READY -> null
}

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
    modifier: Modifier = Modifier
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
    val message = rewardedStatusMessage(shown)

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
