@file:OptIn(ExperimentalFoundationApi::class)

package com.codingtools.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingtools.app.billing.LocalBilling
import com.codingtools.app.tools.Category
import com.codingtools.app.tools.PRO_FALLBACK_PRICE
import com.codingtools.app.tools.Registry
import com.codingtools.app.tools.freeToolCount
import com.codingtools.app.tools.isPremium
import com.codingtools.app.tools.proToolCount
import kotlinx.coroutines.launch

private const val CATS_PER_PAGE = 6

/** First-launch walkthrough (also reachable from Settings → How it works). [onDone] runs on "Get started" or "Skip". */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val catPages = remember { Category.values().toList().chunked(CATS_PER_PAGE) }
    val pageCount = 2 + catPages.size + 1
    val pager = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pageCount - 1

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
                when {
                    i == 0 -> WelcomePage()
                    i == 1 -> HowPage()
                    i < 2 + catPages.size -> CategoriesPage(catPages[i - 2], i - 1, catPages.size)
                    else -> PlansPage()
                }
            }
            if (!last) TextButton(onDone, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 8.dp)) { Text("Skip", color = Color.White) }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                repeat(pageCount) { i ->
                    val on = pager.currentPage == i
                    Box(Modifier.padding(3.dp).size(if (on) 18.dp else 6.dp, 6.dp).clip(CircleShape).background(if (on) Blue500 else MaterialTheme.colorScheme.outline))
                }
            }
            PillButton(if (last) "Get started" else "Next") {
                if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            }
        }
    }
}

@Composable
private fun Page(icon: ImageVector, title: String, subtitle: String, body: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BlueHeader(bottomRadius = 40.dp) {
            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(38.dp))
                }
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
                Text(subtitle, color = Color.White.copy(alpha = 0.82f), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = body)
    }
}

@Composable
private fun Point(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        IconTile(icon, size = 42.dp)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun Step(n: Int, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Blue500), contentAlignment = Alignment.Center) {
            Text("$n", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun WelcomePage() = Page(Icons.Rounded.Terminal, "280+ Offline Dev Tools", "Your developer toolbox, no internet needed") {
    Text("Format, convert, encode, inspect and debug, all from your phone. Pick a tool, paste your data and get the answer instantly.", fontSize = 15.sp)
    Point(Icons.Rounded.Bolt, "Instant", "Results appear as you tap Run, and cheap tools even update live while you type.")
    Point(Icons.Rounded.CloudOff, "Works offline", "Every tool runs on your phone, so it works on a plane, in a basement or behind a firewall.")
    Point(Icons.Rounded.Lock, "Private by design", "No account and no sign-up. What you type into a tool is never uploaded.")
}

@Composable
private fun HowPage() = Page(Icons.Rounded.PlayArrow, "How it works", "From idea to answer in a few taps") {
    Step(1, "Find a tool", "Tap the round search button, browse the Categories tab, or pick a card on Home.")
    Step(2, "Paste or type your input", "Choose a mode (for example Format, Minify or Validate). Not sure what to enter? Tap Sample to load an example.")
    Step(3, "Get the result", "Copy it or share it. Light tools update live as you type; heavier ones run when you tap Run.")
    Step(4, "Keep your favourites close", "Tap the star to pin a tool. Your recent and favourite tools live in the Library tab.")
    Step(5, "Share text straight in", "In any app, choose Share → 280+ Offline Dev Tools and the text lands in the next tool you open.")
}

@Composable
private fun CategoriesPage(cats: List<Category>, n: Int, of: Int) =
    Page(Icons.Rounded.Apps, "What's inside ($n/$of)", "Every tool, grouped by what you're trying to do") {
        cats.forEach { c ->
            val tools = Registry.byCategory(c)
            val pro = tools.count { it.isPremium }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                IconTile(categoryIcon(c), size = 42.dp)
                Column(Modifier.weight(1f)) {
                    Text(c.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(blurb(c), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    Text(if (pro > 0) "${tools.size} tools · $pro Pro" else "${tools.size} tools · all free", fontSize = 11.sp, color = Blue500, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }

@Composable
private fun PlansPage() {
    val price = LocalBilling.current.price ?: PRO_FALLBACK_PRICE
    Page(Icons.Rounded.Star, "Start free, upgrade once", "No subscription, ever") {
        SoftCard {
            Text("Free", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("${freeToolCount} tools, free forever: formatters, converters, encoders, generators, regex, date tools, calculators and most cheat sheets. Supported by a small banner ad.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        SoftCard {
            Text("Pro · $price once", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Blue500)
            Text("Unlocks all ${proToolCount} advanced tools (code runner, SQLite, REST client, secure vault, protobuf and more), removes every ad and includes future Pro tools. Restore it any time with the same Google account.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

private fun blurb(c: Category): String = when (c) {
    Category.FORMAT -> "Beautify, minify and validate JSON, XML, YAML, TOML, SQL, HTML and CSS."
    Category.CONVERT -> "Switch between JSON, YAML, XML and CSV, convert units and number bases, turn cURL into code."
    Category.CODEC -> "Base64, URL, JWT, Unicode, hex, Punycode, QR codes and classic ciphers."
    Category.CRYPTO -> "Hashes, HMAC, password generator, AES, key pairs, bcrypt / Argon2 and an encrypted vault."
    Category.GENERATE -> "UUIDs, fake data, lorem ipsum, licences, .gitignore, Dockerfiles and project scaffolds."
    Category.TEXT -> "Regex tester, text and JSON diff, find & replace, counters, slugs and line tools."
    Category.DATETIME -> "Unix timestamps, time zones, date maths and a cron explainer with next runs."
    Category.WEB -> "Colour converter, contrast checker, gradients, CSS generators, meta tags and SVG tools."
    Category.NETWORK -> "Subnet calculator, chmod, URL parser, REST client, DNS / ping / port check and OpenAPI viewer."
    Category.DATA -> "JSONPath, jq, XPath, SQL helpers and a full SQLite playground that runs on your phone."
    Category.MATH -> "Programmer and scientific calculators, bitwise visualiser, IEEE 754 inspector and more."
    Category.CODE -> "Code runner, secret scanner, complexity, stack trace parser, log viewer and snippet manager."
    Category.LOWLEVEL -> "Hex dumps, struct layout, protobuf, CBOR, ASN.1, compression and a file inspector."
    Category.MOBILE -> "plist and localisation converters, deep-link builder, printf tester and Gradle snippets."
    Category.DEVOPS -> "Semver, commit linter, changelog, docker-compose, Kubernetes, GitHub Actions and .env manager."
    Category.REFERENCE -> "Cheat sheets for Git, HTTP, Linux, regex, CSS, Docker, OWASP, assembly and much more."
    Category.LEARN -> "Algorithm visualisers, recursion trees, a DFA simulator and spaced-repetition flashcards."
    Category.PRODUCTIVITY -> "Pomodoro timer, command templates, tool pipelines and rubber-duck notepad."
}
