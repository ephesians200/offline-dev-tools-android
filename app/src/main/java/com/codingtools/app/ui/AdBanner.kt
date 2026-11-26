package com.codingtools.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.codingtools.app.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Persistent anchored-adaptive banner. It lives in the Scaffold's bottom slot, so it stays on screen while the user
 * moves between tabs and tools.
 *  - [aboveSystemBar]: on screens without the app's own bottom bar the banner sits directly above the system
 *    back / home / recents buttons (or gesture bar); otherwise it sits directly above the app's bottom bar.
 *  - The banner collapses to zero height until an ad has actually loaded, so an offline or no-fill device
 *    never shows an empty strip.
 */
@Composable
fun AdBanner(aboveSystemBar: Boolean, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    @Suppress("DEPRECATION")
    val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp) }
    var loaded by remember(adSize) { mutableStateOf(false) }

    val adView = remember(adSize) {
        AdView(ctx).apply {
            setAdSize(adSize)
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            adListener = object : AdListener() {
                override fun onAdLoaded() { loaded = true }
                override fun onAdFailedToLoad(error: LoadAdError) { loaded = false }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(adView, lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) { Lifecycle.Event.ON_PAUSE -> adView.pause(); Lifecycle.Event.ON_RESUME -> adView.resume(); else -> {} }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); adView.destroy() }
    }

    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Box(Modifier.fillMaxWidth().height(if (loaded) adSize.height.dp else 0.dp), contentAlignment = Alignment.Center) {
            // requiredSize: the AdView is always measured at its real size, even while the wrapper is collapsed.
            AndroidView({ adView }, Modifier.requiredSize(widthDp.dp, adSize.height.dp))
        }
        // A little breathing room above the app's bottom bar / centre search button so taps don't land on the ad by accident.
        if (!aboveSystemBar && loaded) Spacer(Modifier.height(8.dp))
        if (aboveSystemBar) Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
