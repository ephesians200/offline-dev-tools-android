@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.codingtools.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.codingtools.app.BuildConfig
import com.codingtools.app.R
import com.codingtools.app.billing.LocalBilling
import com.codingtools.app.data.LocalStore
import com.codingtools.app.tools.Category
import com.codingtools.app.tools.Registry
import com.codingtools.app.tools.PRO_FALLBACK_PRICE
import com.codingtools.app.tools.Tool
import com.codingtools.app.tools.isPremium
import com.codingtools.app.tools.proToolCount
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val popularIds = listOf("json", "regex", "base64", "timestamp", "color", "hash", "uuid", "curl", "diff", "jwt", "cron", "sqlite", "prog-calc", "url", "data-convert", "case")
private val featuredIds = listOf("json", "regex", "jwt", "timestamp", "sqlite", "data-convert", "color", "hash")

/** Clock dial. [s] scales the whole thing (the original design was 232dp; 0.78 saves ~60dp of header). */
@Composable
private fun Dial(s: Float = 0.78f) {
    val reduce = LocalReduceMotion.current
    var now by remember { mutableStateOf(LocalTime.now()) }
    // A counting-up tick (not seconds-of-minute) so the arc never animates backwards at :00.
    var tick by remember { mutableIntStateOf(now.second) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000 - System.currentTimeMillis() % 1000); tick++; now = LocalTime.now() }   // aligned to the real second: no drift
    }
    val sweepAnim by animateFloatAsState(tick.toFloat(), motion(reduce, tween(1000, easing = LinearEasing)), label = "sweep")
    Box(Modifier.size((232 * s).dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding((14 * s).dp)) {
            val sweep = ((sweepAnim % 60f) / 60f * 360f).coerceAtLeast(2f)   // read inside draw: only the draw phase updates
            val sw = (16 * s).dp.toPx()
            val r = size.minDimension / 2
            drawCircle(Color.White.copy(alpha = 0.10f), radius = r - sw / 2, style = Stroke(sw))
            drawArc(Blue500, -90f, sweep, false, topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
            drawCircle(Color(0xFF0A2F6B), radius = r - sw - (12 * s).dp.toPx())
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0 - 90)
                val rr = r - sw - (24 * s).dp.toPx()
                drawCircle(Color.White.copy(alpha = if (i % 3 == 0) 0.8f else 0.35f), radius = (if (i % 3 == 0) 2.5f else 1.8f).times(s).dp.toPx(), center = Offset(center.x + (cos(a) * rr).toFloat(), center.y + (sin(a) * rr).toFloat()))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(now.format(DateTimeFormatter.ofPattern("h:mm")), color = Color.White, fontSize = (42 * s).sp, fontWeight = FontWeight.Bold)
            Text(now.format(DateTimeFormatter.ofPattern("a")).lowercase(), color = Color.White.copy(alpha = 0.8f), fontSize = (18 * s).sp)
            Text("dev time", color = Blue200, fontSize = 11.sp)
        }
        val badges = listOf(Triple(-45.0, Icons.Rounded.Check, Blue500), Triple(125.0, Icons.Rounded.DataObject, Color(0xFF0F4FC2)), Triple(80.0, Icons.Rounded.Code, Color(0xFF0F4FC2)))
        badges.forEach { (deg, ic, bg) ->
            val rad = Math.toRadians(deg)
            Box(
                Modifier.offset(x = (cos(rad) * 96 * s).dp, y = (sin(rad) * 96 * s).dp).size((38 * s).dp).clip(CircleShape).background(bg).border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(ic, null, tint = Color.White, modifier = Modifier.size((20 * s).dp)) }
        }
    }
}

@Composable
fun HomeScreen(openTool: (String) -> Unit, openSearch: () -> Unit, openCategory: (Category) -> Unit) {
    val store = LocalStore.current
    val reduce = LocalReduceMotion.current
    // Rotate the featured list by day-of-year so page 0 really is a different "tool of the day" each day.
    val featured = remember {
        val base = featuredIds.mapNotNull { Registry.byId[it] }
        if (base.isEmpty()) base else { val k = LocalDate.now().dayOfYear % base.size; base.drop(k) + base.take(k) }
    }
    val popular = remember { popularIds.mapNotNull { Registry.byId[it] } }
    val pager = rememberPagerState(pageCount = { featured.size })
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragged, reduce, featured.size) {
        if (!dragged && !reduce && featured.size > 1) while (true) { delay(5000); pager.animateScrollToPage((pager.currentPage + 1) % featured.size) }
    }
    var tab by remember { mutableIntStateOf(0) }
    val list: List<Tool> = when (tab) {
        0 -> popular
        1 -> store.recents.mapNotNull { Registry.byId[it] }
        else -> store.favorites.mapNotNull { Registry.byId[it] }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            BlueHeader(bottomRadius = 44.dp) {
                Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 14.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("280+", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Text("Offline Dev Tools", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("Developer tools that work without internet", color = Blue200, fontSize = 12.sp)
                    }
                    IconButton(onClick = openSearch) { Icon(Icons.Rounded.Search, "Search", tint = Color.White) }
                    BadgedBox(badge = { if (store.favorites.isNotEmpty()) Badge(containerColor = Coral) { Text("${store.favorites.size}") } }) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Terminal, null, tint = Color.White) }
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) { Dial() }
                Spacer(Modifier.height(64.dp))
            }
        }
        item {
            Column(Modifier.layout { m, c -> val p = m.measure(c); val up = 56.dp.roundToPx(); layout(p.width, p.height - up) { p.place(0, -up) } }) {
                HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 22.dp), pageSpacing = 12.dp) { i ->
                    val t = featured[i]
                    val shape = RoundedCornerShape(22.dp)
                    Surface(
                        Modifier.fillMaxWidth()
                            .then(if (reduce) Modifier else Modifier.graphicsLayer {
                                // Neighbouring cards shrink and fade a little: a depth effect while swiping.
                                val a = abs((pager.currentPage - i) + pager.currentPageOffsetFraction).coerceIn(0f, 1f)
                                scaleX = 1f - 0.06f * a; scaleY = scaleX; alpha = 1f - 0.3f * a
                            })
                            .cardClickable(shape, 8.dp, Blue900.copy(alpha = 0.25f)) { openTool(t.id) },
                        shape = shape, color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Folder, null, tint = Blue500, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(t.category.title, color = Blue500, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (t.isPremium && !store.premium) { ProBadge(); Spacer(Modifier.width(6.dp)) }
                                Text("Offline", color = Color(0xFF1E9E6A), fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFDDF7EA)).padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(t.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconTile(categoryIcon(t.category), size = 30.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(if (i == 0) "Tool of the day" else "Featured", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                Text("Open ›", color = Blue500, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(featured.size) { i ->
                        val on = pager.currentPage == i
                        val w by animateDpAsState(if (on) 18.dp else 6.dp, motion(reduce, spring(dampingRatio = 0.6f, stiffness = 400f)), label = "dotW")
                        val c by animateColorAsState(if (on) Blue500 else MaterialTheme.colorScheme.outline, motion(reduce, tween(200)), label = "dotC")
                        Box(Modifier.padding(3.dp).size(w, 6.dp).clip(CircleShape).background(c))
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("My Tools", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                listOf("Popular", "Recent", "Favourites").forEachIndexed { i, n ->
                    val sel = tab == i
                    val bg by animateColorAsState(if (sel) Blue500 else MaterialTheme.colorScheme.primaryContainer, motion(reduce, tween(180)), label = "tabBg")
                    val fg by animateColorAsState(if (sel) Color.White else MaterialTheme.colorScheme.onPrimaryContainer, motion(reduce, tween(180)), label = "tabFg")
                    Text(n, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = fg,
                        modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(10.dp)).background(bg).clickable { tab = i }.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
        if (list.isEmpty()) item { Text(if (tab == 1) "Tools you open will appear here." else "Tap ☆ on any tool to pin it here.", Modifier.padding(horizontal = 22.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        itemsIndexed(list.take(16), key = { _, t -> t.id }) { idx, t ->
            ToolRow(t, { openTool(t.id) }, Modifier.animateItem().padding(horizontal = 18.dp, vertical = 5.dp).staggeredEntrance(idx))
        }
        item { SectionTitle("Browse by category") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(Category.values().toList()) { c ->
                    val shape = RoundedCornerShape(16.dp)
                    Surface(Modifier.cardClickable(shape, 2.dp) { openCategory(c) }, color = MaterialTheme.colorScheme.surface, shape = shape) {
                        Column(Modifier.width(112.dp).padding(12.dp)) {
                            IconTile(categoryIcon(c), size = 38.dp)
                            Text(c.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                            Text("${Registry.byCategory(c).size} tools", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoriesScreen(open: (Category) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Categories", "${Category.values().size} groups · ${Registry.all.size} tools")
        LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            gridItems(Category.values().toList()) { c ->
                SoftCard(onClick = { open(c) }) {
                    IconTile(categoryIcon(c))
                    Text(c.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${Registry.byCategory(c).size} tools", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun CategoryScreen(c: Category, onBack: () -> Unit, openTool: (String) -> Unit) {
    val tools = remember(c) { Registry.byCategory(c) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(c.title, "${tools.size} tools", onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(tools, key = { it.id }) { t -> ToolRow(t, { openTool(t.id) }) }
        }
    }
}

@Composable
fun SearchScreen(openTool: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<Category?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val results = remember(q, cat) { Registry.search(q).filter { cat == null || it.category == cat }.let { if (q.isBlank()) it.sortedBy { t -> t.title } else it } }
    Column(Modifier.fillMaxSize()) {
        BlueHeader(bottomRadius = 28.dp) {
            Text("Command palette", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(start = 20.dp, top = 12.dp))
            OutlinedTextField(
                q, { q = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).focusRequester(focus),
                placeholder = { Text("Search ${Registry.all.size} tools: json, jwt, regex, cron, sql…", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.White) },
                trailingIcon = { if (q.isNotEmpty()) IconButton({ q = "" }) { Icon(Icons.Rounded.Close, "Clear", tint = Color.White) } },
                singleLine = true, shape = RoundedCornerShape(50), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White.copy(alpha = 0.6f), unfocusedBorderColor = Color.White.copy(alpha = 0.25f), cursorColor = Color.White, focusedContainerColor = Color.White.copy(alpha = 0.12f), unfocusedContainerColor = Color.White.copy(alpha = 0.08f))
            )
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(cat == null, { cat = null }, { Text("All") }) }
                items(Category.values().toList()) { c -> FilterChip(cat == c, { cat = if (cat == c) null else c }, { Text(c.shortName) }) }
            }
            Spacer(Modifier.height(12.dp))
        }
        Text("${results.size} result${if (results.size == 1) "" else "s"}", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(results, key = { it.id }) { t -> ToolRow(t, { openTool(t.id) }, Modifier.animateItem()) }
        }
    }
}

@Composable
fun LibraryScreen(openTool: (String) -> Unit) {
    val store = LocalStore.current
    var tab by remember { mutableIntStateOf(0) }
    val list = (if (tab == 0) store.recents.toList() else store.favorites.toList()).mapNotNull { Registry.byId[it] }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Library", "Recent and favourite tools") {
            TextButton({ if (tab == 0) store.clearRecents() else store.clearFavs() }) { Text("Clear", color = Color.White) }
        }
        TabRow(tab, containerColor = Color.Transparent) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Recent (${store.recents.size})") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Favourites (${store.favorites.size})") })
        }
        if (list.isEmpty()) Text(if (tab == 0) "Nothing yet. Open a tool and it shows up here." else "Tap the star on a tool to pin it.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(list, key = { it.id }) { t -> ToolRow(t, { openTool(t.id) }, Modifier.animateItem()) } }
    }
}

@Composable
fun SettingsScreen(openPaywall: () -> Unit, openOnboarding: () -> Unit) {
    val store = LocalStore.current
    val billing = LocalBilling.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings", "${stringResource(R.string.app_name)} · v${BuildConfig.VERSION_NAME}")
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SoftCard {
                if (store.premium) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Rounded.Verified, tint = Color(0xFF1E9E6A), bg = Color(0xFFDDF7EA))
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Pro unlocked", fontWeight = FontWeight.Bold)
                            Text("Thank you! All ${proToolCount} advanced tools are open and ads are off.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Text("Go Pro", fontWeight = FontWeight.Bold)
                    Text("Unlock ${proToolCount} advanced tools and remove ads. One payment of ${billing.price ?: PRO_FALLBACK_PRICE}, no subscription.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton("See Pro", Icons.Rounded.Star) { openPaywall() }
                        PillButton("Restore purchase", filled = false) { billing.restore() }
                    }
                    billing.message?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)) }
                }
                TextButton(openOnboarding) { Text("How it works") }
            }
            SoftCard {
                Text("Theme", fontWeight = FontWeight.Bold)
                listOf("system" to "Follow system", "light" to "Light", "dark" to "Dark").forEach { (k, n) ->
                    Row(Modifier.fillMaxWidth().clickable { store.updateTheme(k) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(store.theme == k, { store.updateTheme(k) }); Text(n)
                    }
                }
            }
            SoftCard {
                Text("Behaviour", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reduce motion")
                        Text("Turns off screen transitions and decorative animation. Also follows the system \"remove animations\" setting.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(store.reduceMotion, { store.updateReduceMotion(it) })
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Remember last input")
                        Text("Restores what you typed in each tool after the app restarts. Never saved for hashing & security tools.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(store.rememberInput, { store.updateRememberInput(it) })
                }
                TextButton({ store.clearInputs() }) { Text("Clear saved inputs") }
            }
            SoftCard {
                Text("Privacy & offline", fontWeight = FontWeight.Bold)
                Text("Every tool runs on your device and works offline. There is no account and no server of ours, and nothing you type into a tool is ever uploaded. Free users see a banner ad from Google AdMob, which may use your device's advertising ID and asks for your consent where the law requires it. Pro removes all ads. Purchases are handled by Google Play. The REST client, DNS lookup, ping and port check connect only to the host you type, and only when you press the button.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
            SoftCard {
                Text("Share-to-tool", fontWeight = FontWeight.Bold)
                Text("In any app choose Share → 280+ Offline Dev Tools. The text is dropped into the next tool you open.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
            SoftCard {
                Text("Library", fontWeight = FontWeight.Bold)
                Text("${Registry.all.size} tools in ${Category.values().size} categories", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton("Clear recents", filled = false) { store.clearRecents() }
                    PillButton("Clear favourites", filled = false) { store.clearFavs() }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
