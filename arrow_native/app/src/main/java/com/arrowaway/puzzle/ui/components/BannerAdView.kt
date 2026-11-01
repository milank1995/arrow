package com.arrowaway.puzzle.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arrowaway.puzzle.ads.AdManager
import com.arrowaway.puzzle.config.RemoteConfigManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Standard Banner Ad Component for Arrow Away.
 * Displayed on Settings, Daily Challenge, and Victory screens.
 * Automatically checks Remote Config toggles and disposes AdView when detached.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier
) {
    if (!RemoteConfigManager.isAdsEnabled || !RemoteConfigManager.isBannerAdsEnabled) {
        return
    }

    val context = LocalContext.current
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = AdManager.getBannerAdUnitId()
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("BannerAdView", "Banner ad loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w("BannerAdView", "Banner ad failed to load: ${error.message} (code ${error.code})")
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
