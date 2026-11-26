package com.codingtools.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingtools.app.billing.LocalBilling
import com.codingtools.app.billing.findActivity
import com.codingtools.app.data.LocalStore
import com.codingtools.app.tools.PRO_FALLBACK_PRICE
import com.codingtools.app.tools.Registry
import com.codingtools.app.tools.Tool
import com.codingtools.app.tools.freeToolCount
import com.codingtools.app.tools.proToolCount

/**
 * The Pro paywall. Used three ways: after onboarding on a fresh install, from Settings, and inline in place of a
 * locked tool ([lockedTool] != null). [onClose] dismisses it (leaves the user on the free tier).
 */
@Composable
fun PaywallScreen(lockedTool: Tool? = null, onClose: () -> Unit) {
    val store = LocalStore.current
    val billing = LocalBilling.current
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { billing.message = null }
    // Bought (or restored) while the standalone paywall is open: close it. The inline version is replaced by the tool itself.
    LaunchedEffect(store.premium) { if (store.premium && lockedTool == null) onClose() }

    val price = billing.price ?: PRO_FALLBACK_PRICE
    val total = Registry.all.size
    val pro = proToolCount
    val free = freeToolCount

    Column(Modifier.fillMaxSize()) {
        BlueHeader(bottomRadius = 36.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close", tint = Color.White) }
            }
            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Star, null, tint = Color(0xFFFFD166), modifier = Modifier.size(34.dp))
                }
                Text("Go Pro", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Text(
                    if (lockedTool != null) "“${lockedTool.title}” is a Pro tool" else "Unlock every advanced tool",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp)
                )
                Text("One payment of $price · yours forever", color = Blue200, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Everything in Pro", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Benefit(Icons.Rounded.LockOpen, "$pro advanced tools unlocked",
                "Code Editor & Runner, SQLite Playground, REST Client, Secure Vault, Argon2 / bcrypt / scrypt, Protobuf, CBOR and ASN.1 decoders, Kubernetes and docker-compose generators, jq, XPath and more.")
            Benefit(Icons.Rounded.Block, "No ads, ever", "The banner disappears from every screen the moment you upgrade.")
            Benefit(Icons.Rounded.AllInclusive, "Future Pro tools included", "New advanced tools are added regularly. You get them all at no extra cost.")
            Benefit(Icons.Rounded.Payments, "One payment. No subscription.", "Pay $price once. No renewals, no trial that quietly bills you, no account to create.")
            Benefit(Icons.Rounded.CloudOff, "Still 100% offline and private", "Pro tools run on your device like every other tool. Nothing you type is uploaded.")
            Benefit(Icons.Rounded.Restore, "Works on all your devices", "Reinstalled or got a new phone? Tap Restore purchase with the same Google account.")
            Benefit(Icons.Rounded.Favorite, "Supports independent development", "Your purchase keeps this app growing.")

            SoftCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlanColumn("Free", "$free tools", "Banner ads", Modifier.weight(1f), highlighted = false)
                    PlanColumn("Pro", "All $total tools", "No ads", Modifier.weight(1f), highlighted = true)
                }
            }
        }

        Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                billing.message?.let {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp))
                }
                PillButton("Unlock Pro · $price", Icons.Rounded.LockOpen, Modifier.fillMaxWidth().height(52.dp), loading = billing.busy) {
                    ctx.findActivity()?.let { billing.buy(it) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton({ billing.restore() }) { Text("Restore purchase") }
                    TextButton(onClose) { Text(if (lockedTool != null) "Maybe later" else "Not now") }
                }
                Text("One-time purchase charged to your Google Play account.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        IconTile(icon, size = 42.dp)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun PlanColumn(name: String, tools: String, ads: String, modifier: Modifier, highlighted: Boolean) {
    val bg = if (highlighted) Blue500.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(bg).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (highlighted) Blue500 else MaterialTheme.colorScheme.onSurface)
        Text(tools, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        Text(ads, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
