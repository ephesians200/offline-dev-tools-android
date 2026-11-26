@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.codingtools.app.ui

import android.content.pm.ApplicationInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.codingtools.app.ads.Ads
import com.codingtools.app.billing.findActivity
import com.codingtools.app.data.LocalStore
import com.codingtools.app.tools.Category
import com.codingtools.app.tools.Registry
import com.codingtools.app.tools.Tool
import com.codingtools.app.tools.isPremium

private val mainRoutes = setOf("home", "categories", "library", "settings", "search")

/** Onboarding and the paywall are shown without the banner. */
private fun noAds(route: String?) = route == "onboarding" || route == "paywall"

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val store = LocalStore.current

    // Reduce motion = the in-app switch OR the system "remove animations" / animator scale of 0.
    val systemOff = remember(ctx) { android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val reduce = store.reduceMotion || systemOff

    // Fail fast in debug builds if a tool names a custom screen that does not exist.
    LaunchedEffect(Unit) {
        if (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            val missing = Registry.all.filter { it.custom != null && it.custom !in customScreenIds }.map { it.id }
            check(missing.isEmpty()) { "No screen registered for custom tools: $missing" }
        }
    }

    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    fun go(r: String) {
        nav.navigate(r) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
    }

    // First run: onboarding → one-time paywall → home. Afterwards straight to home.
    val startRoute = remember {
        when {
            !store.onboardingDone -> "onboarding"
            !store.paywallSeen && !store.premium -> "paywall"
            else -> "home"
        }
    }
    // The route is null for the first frame; fall back to the start route so onboarding never flashes the bottom bar.
    val current = route ?: startRoute
    val showBar = current in mainRoutes
    fun closePaywall() {
        if (!store.paywallSeen) { store.markPaywallSeen(); nav.navigate("home") { popUpTo("paywall") { inclusive = true } } }
        else nav.popBackStack()
    }
    fun finishOnboarding() {
        if (!store.onboardingDone) {
            store.markOnboardingDone()
            if (store.premium) { store.markPaywallSeen(); nav.navigate("home") { popUpTo("onboarding") { inclusive = true } } }
            else nav.navigate("paywall") { popUpTo("onboarding") { inclusive = true } }
        } else nav.popBackStack()
    }

    // Ads (and Google's consent prompt) start only for free users, and only after the first-run screens are out of the way.
    val activity = remember(ctx) { ctx.findActivity() }
    LaunchedEffect(store.premium, store.paywallSeen) {
        if (!store.premium && store.paywallSeen && activity != null) Ads.init(activity)
    }
    val showAds = !store.premium && Ads.ready && !noAds(current)

    // Tabs cross-fade with a tiny scale; detail screens (tool, category) slide in from the right.
    // popEnter/popExit are also what drives the predictive-back gesture preview.
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (reduce) EnterTransition.None else fadeIn(tween(220, 60)) + scaleIn(tween(220, 60), initialScale = 0.96f)
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { if (reduce) ExitTransition.None else fadeOut(tween(90)) }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { if (reduce) EnterTransition.None else fadeIn(tween(220)) }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (reduce) ExitTransition.None else fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.96f)
    }
    val slideIn: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (reduce) EnterTransition.None else slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 4 } + fadeIn(tween(300))
    }
    val slideOutPop: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (reduce) ExitTransition.None else slideOutHorizontally(tween(250, easing = FastOutSlowInEasing)) { it / 4 } + fadeOut(tween(250))
    }

    CompositionLocalProvider(LocalReduceMotion provides reduce) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Column {
                    // Persistent banner: directly above the app's bottom bar, or above the system nav buttons on detail screens.
                    if (showAds) AdBanner(aboveSystemBar = !showBar)
                    AnimatedVisibility(
                        showBar,
                        enter = if (reduce) EnterTransition.None else slideInVertically(tween(260)) { it } + fadeIn(tween(200)),
                        exit = if (reduce) ExitTransition.None else slideOutVertically(tween(200)) { it } + fadeOut(tween(150))
                    ) { BottomBar(route) { go(it) } }
                }
            }
        ) { pad ->
            NavHost(
                nav, startDestination = startRoute,
                // Screens without the bottom bar must keep clear of the gesture / 3-button navigation bar.
                modifier = Modifier.padding(pad).then(if (showBar || showAds) Modifier else Modifier.navigationBarsPadding()),
                enterTransition = enter, exitTransition = exit, popEnterTransition = popEnter, popExitTransition = popExit
            ) {
                composable("home") { HomeScreen(openTool = { nav.navigate("tool/$it") }, openSearch = { go("search") }, openCategory = { nav.navigate("category/${it.name}") }) }
                composable("categories") { CategoriesScreen { nav.navigate("category/${it.name}") } }
                composable("category/{c}", enterTransition = slideIn, popExitTransition = slideOutPop) { be ->
                    val c = runCatching { Category.valueOf(be.arguments?.getString("c") ?: "") }.getOrNull()
                    if (c != null) CategoryScreen(c, onBack = { nav.popBackStack() }, openTool = { nav.navigate("tool/$it") })
                }
                composable("search") { SearchScreen { nav.navigate("tool/$it") } }
                composable("library") { LibraryScreen { nav.navigate("tool/$it") } }
                composable("settings") { SettingsScreen(openPaywall = { nav.navigate("paywall") }, openOnboarding = { nav.navigate("onboarding") }) }
                composable("onboarding") { OnboardingScreen(onDone = { finishOnboarding() }) }
                composable("paywall") {
                    BackHandler { closePaywall() }
                    PaywallScreen(null) { closePaywall() }
                }
                composable("tool/{id}", enterTransition = slideIn, popExitTransition = slideOutPop) { be ->
                    val t = Registry.byId[be.arguments?.getString("id") ?: ""]
                    if (t != null) ToolHost(t) { nav.popBackStack() }
                }
            }
        }
    }
}

@Composable
fun ToolHost(tool: Tool, onBack: () -> Unit) {
    val store = LocalStore.current
    when {
        // Pro tools: every way of opening one (list, search, recents, favourites, deep link) lands here.
        tool.isPremium && !store.premium -> PaywallScreen(tool, onClose = onBack)
        tool.custom != null -> CustomScreen(tool, onBack)
        tool.reference != null -> ReferenceScreen(tool, onBack)
        else -> ToolScreen(tool, onBack)
    }
}

@Composable
private fun BottomBar(route: String?, onNav: (String) -> Unit) {
    val reduce = LocalReduceMotion.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            Modifier.fillMaxWidth().padding(top = 30.dp),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), shadowElevation = 14.dp, color = MaterialTheme.colorScheme.surface
        ) {
            Row(Modifier.navigationBarsPadding().height(66.dp).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                NavItem(Icons.Rounded.Home, "Home", route == "home") { onNav("home") }
                NavItem(Icons.Rounded.Apps, "Categories", route == "categories") { onNav("categories") }
                Spacer(Modifier.width(76.dp))
                NavItem(Icons.Rounded.Bookmarks, "Library", route == "library") { onNav("library") }
                NavItem(Icons.Rounded.Settings, "Settings", route == "settings") { onNav("settings") }
            }
        }
        // Centre button: opens the command palette. Springs down on press and tilts its magnifier.
        val src = remember { MutableInteractionSource() }
        val pressed by src.collectIsPressedAsState()
        val scale by animateFloatAsState(if (pressed) 0.9f else 1f, motion(reduce, spring(dampingRatio = 0.5f, stiffness = 500f)), label = "fabScale")
        val tilt by animateFloatAsState(if (pressed) -15f else 0f, motion(reduce, spring(dampingRatio = 0.5f, stiffness = 400f)), label = "fabTilt")
        Surface(
            Modifier.size(66.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                .clickable(interactionSource = src, indication = null) { onNav("search") },
            shape = CircleShape, color = Blue500, shadowElevation = 10.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Search, "Search tools", tint = Color.White, modifier = Modifier.size(30.dp).graphicsLayer { rotationZ = tilt })
            }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val reduce = LocalReduceMotion.current
    // Unselected uses onSurfaceVariant (≈5:1 contrast) instead of the faint outline colour (≈1.6:1).
    val tint by animateColorAsState(if (selected) Blue500 else MaterialTheme.colorScheme.onSurfaceVariant, motion(reduce, tween(180)), label = "navTint")
    val dot by animateDpAsState(if (selected) 5.dp else 0.dp, motion(reduce, spring(dampingRatio = 0.5f, stiffness = 500f)), label = "navDot")
    val lift by animateDpAsState(if (selected) (-2).dp else 0.dp, motion(reduce, spring(dampingRatio = 0.6f, stiffness = 500f)), label = "navLift")
    Column(Modifier.clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(26.dp).offset(y = lift))
        Box(Modifier.padding(top = 4.dp).height(5.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(dot).clip(CircleShape).background(Blue500))
        }
    }
}
