package com.codingtools.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.fragment.app.FragmentActivity
import com.codingtools.app.billing.BillingManager
import com.codingtools.app.billing.LocalBilling
import com.codingtools.app.data.LocalStore
import com.codingtools.app.data.Shared
import com.codingtools.app.data.Store
import com.codingtools.app.ui.AppRoot
import com.codingtools.app.ui.CodingToolsTheme

/** FragmentActivity (a ComponentActivity subclass) because androidx.biometric.BiometricPrompt needs one. */
class MainActivity : FragmentActivity() {
    private lateinit var store: Store
    private lateinit var billing: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShare(intent)
        store = Store(applicationContext)
        billing = BillingManager(applicationContext, store).also { it.start() }
        setContent {
            val dark = when (store.theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
            CompositionLocalProvider(LocalStore provides store, LocalBilling provides billing) {
                CodingToolsTheme(dark) { AppRoot() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Picks up purchases made outside the app, restores after a reinstall, and revokes refunds.
        billing.refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) billing.close()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    private fun handleShare(i: Intent?) {
        if (i?.action == Intent.ACTION_SEND && i.type?.startsWith("text/") == true) {
            Shared.text = i.getStringExtra(Intent.EXTRA_TEXT)
        }
    }
}
