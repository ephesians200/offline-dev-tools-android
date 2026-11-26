package com.codingtools.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Tiny local store (SharedPreferences, app-private, never leaves the device). */
class Store(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("codingtools", Context.MODE_PRIVATE)
    val favorites = mutableStateListOf<String>().apply { addAll(sp.getString("fav", "")!!.split(",").filter { it.isNotBlank() }) }
    val recents = mutableStateListOf<String>().apply { addAll(sp.getString("recent", "")!!.split(",").filter { it.isNotBlank() }) }
    var theme by mutableStateOf(sp.getString("theme", "system")!!)
        private set
    /** User switch in Settings; the system "remove animations" setting is honoured on top of this. */
    var reduceMotion by mutableStateOf(sp.getBoolean("reduce_motion", false))
        private set
    /** Restore the last input of each tool (never for the security / hashing category). */
    var rememberInput by mutableStateOf(sp.getBoolean("remember_input", true))
        private set

    /** First-launch flags: onboarding and the one-time "fresh install" paywall each show once. */
    var onboardingDone by mutableStateOf(sp.getBoolean("onboarding_done", false))
        private set
    var paywallSeen by mutableStateOf(sp.getBoolean("paywall_seen", false))
        private set
    /** Cached "Pro unlocked" state. Google Play is the source of truth (BillingManager re-checks it every launch). */
    @set:JvmName("setPremiumInternal")
    var premium by mutableStateOf(sp.getBoolean("pro", false))
        private set

    fun markOnboardingDone() { onboardingDone = true; sp.edit().putBoolean("onboarding_done", true).apply() }
    fun markPaywallSeen() { paywallSeen = true; sp.edit().putBoolean("paywall_seen", true).apply() }
    fun setPremium(v: Boolean) { if (premium != v) { premium = v; sp.edit().putBoolean("pro", v).apply() } }

    fun toggleFav(id: String) { if (id in favorites) favorites.remove(id) else favorites.add(0, id); sp.edit().putString("fav", favorites.joinToString(",")).apply() }
    fun touch(id: String) { recents.remove(id); recents.add(0, id); while (recents.size > 30) recents.removeAt(recents.size - 1); sp.edit().putString("recent", recents.joinToString(",")).apply() }
    fun clearRecents() { recents.clear(); sp.edit().remove("recent").apply() }
    fun clearFavs() { favorites.clear(); sp.edit().remove("fav").apply() }
    fun updateTheme(t: String) { theme = t; sp.edit().putString("theme", t).apply() }
    fun updateReduceMotion(v: Boolean) { reduceMotion = v; sp.edit().putBoolean("reduce_motion", v).apply() }
    fun updateRememberInput(v: Boolean) { rememberInput = v; sp.edit().putBoolean("remember_input", v).apply(); if (!v) clearInputs() }
    /** Removes every saved "last input" (keys starting with in_). */
    fun clearInputs() { val e = sp.edit(); sp.all.keys.filter { it.startsWith("in_") }.forEach { e.remove(it) }; e.apply() }
    fun remove(k: String) { sp.edit().remove(k).apply() }
    fun get(k: String, d: String = ""): String = sp.getString(k, d) ?: d
    fun put(k: String, v: String) { sp.edit().putString(k, v).apply() }
}

val LocalStore = staticCompositionLocalOf<Store> { error("Store not provided") }

/** Text received through the Android share sheet ("Share → 280+ Offline Dev Tools"). */
object Shared {
    var text by mutableStateOf<String?>(null)
    fun take(): String? { val t = text; text = null; return t }
}
