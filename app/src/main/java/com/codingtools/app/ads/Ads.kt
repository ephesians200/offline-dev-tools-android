package com.codingtools.app.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Starts the Google Mobile Ads SDK, but only after the User Messaging Platform (UMP) consent flow says ads may
 * be requested (it shows Google's consent form where the law requires one, e.g. EEA / UK).
 * Never called for Pro users. [ready] flips to true once the banner may be shown.
 */
object Ads {
    var ready by mutableStateOf(false)
        private set
    private var started = false
    private var sdkStarted = false

    fun init(activity: Activity) {
        if (started) return
        started = true
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity, ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { startSdk(activity, info) }
            },
            { startSdk(activity, info) }   // consent info failed: still honour a consent state saved by an earlier session
        )
        // A previous session may already have collected consent, so ads can start right away.
        startSdk(activity, info)
    }

    private fun startSdk(ctx: Context, info: ConsentInformation) {
        if (sdkStarted || !info.canRequestAds()) return
        sdkStarted = true
        val app = ctx.applicationContext
        // Google recommends initialising off the main thread.
        Thread { MobileAds.initialize(app) { ready = true } }.start()
    }
}
