package com.codingtools.app.billing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.codingtools.app.data.Store
import com.codingtools.app.tools.PRO_PRODUCT_ID

/**
 * Google Play Billing Library 9.1.0 wrapper for the single one-time "Pro" product.
 *
 *  - There is no server: ownership is read from Google Play ([refresh]) on start-up and every resume.
 *  - The result is cached in [Store.premium] so Pro keeps working offline.
 *  - If Play answers successfully and the purchase is gone (refund / different account), Pro is revoked.
 *    If Play cannot be reached the cached value is kept, so offline users never lose what they paid for.
 */
class BillingManager(context: Context, private val store: Store) : PurchasesUpdatedListener {

    /** Localised price from Google Play (e.g. "$4.99"); null until the product details have loaded. */
    var price by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    /** Short user-facing status / error text for the paywall. */
    var message by mutableStateOf<String?>(null)

    private var details: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Connect (if needed), load the product and sync ownership. Safe to call repeatedly. */
    fun start() {
        if (client.isReady) { loadProduct(); refresh(); return }
        if (client.connectionState == BillingClient.ConnectionState.CONNECTING) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) { loadProduct(); refresh() }
            }
            // enableAutoServiceReconnection() re-connects for us.
            override fun onBillingServiceDisconnected() {}
        })
    }

    fun close() { if (client.isReady) client.endConnection() }

    private fun loadProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRO_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                details = queryResult.productDetailsList.firstOrNull()
                @Suppress("DEPRECATION")
                price = details?.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
    }

    /** Asks Google Play what the user owns. Also what "Restore purchase" calls. */
    fun refresh() {
        if (!client.isReady) { start(); return }
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) apply(purchases, authoritative = true)
        }
    }

    /** "Restore purchase" button: refresh, then tell the user what happened. */
    fun restore() {
        message = null
        busy = true
        if (!client.isReady) { busy = false; message = "Google Play isn't available right now. Check your connection and try again."; start(); return }
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { result, purchases ->
            busy = false
            if (result.responseCode != BillingClient.BillingResponseCode.OK) { message = "Couldn't reach Google Play. Try again later."; return@queryPurchasesAsync }
            apply(purchases, authoritative = true)
            if (!store.premium && message == null) message = "No Pro purchase found on this Google account."
        }
    }

    fun buy(activity: Activity) {
        message = null
        val d = details
        if (d == null) { message = "Google Play isn't ready yet. Check your connection and try again."; start(); return }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()))
            .build()
        busy = true
        val r = client.launchBillingFlow(activity, params)
        if (r.responseCode != BillingClient.BillingResponseCode.OK) { busy = false; message = "Couldn't open Google Play (code ${r.responseCode})." }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        busy = false
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> apply(purchases.orEmpty(), authoritative = false)
            BillingClient.BillingResponseCode.USER_CANCELED -> {}
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            else -> message = "Purchase didn't complete (code ${result.responseCode})."
        }
    }

    private fun apply(purchases: List<Purchase>, authoritative: Boolean) {
        val mine = purchases.filter { PRO_PRODUCT_ID in it.products }
        val owned = mine.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        when {
            owned != null -> {
                store.setPremium(true)
                message = null
                if (!owned.isAcknowledged) acknowledge(owned)   // must happen within 3 days or Google refunds it
            }
            mine.any { it.purchaseState == Purchase.PurchaseState.PENDING } ->
                message = "Your payment is pending. Pro unlocks automatically once it completes."
            authoritative -> store.setPremium(false)            // refunded / other account
        }
    }

    private fun acknowledge(p: Purchase) {
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { /* retried by the next refresh() if it failed */ }
    }
}

val LocalBilling = staticCompositionLocalOf<BillingManager> { error("BillingManager not provided") }

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
