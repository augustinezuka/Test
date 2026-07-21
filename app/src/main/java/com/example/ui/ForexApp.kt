package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.viewmodel.ForexViewModel
import com.example.ui.theme.*

object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val WATCHLIST = "watchlist"
    const val SEARCH = "search"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val DETAIL = "detail"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForexApp(
    viewModel: ForexViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Collect profile to check if onboarding is needed
    val profile by viewModel.userProfile.collectAsState()
    var checkedOnboarding by remember { mutableStateOf(false) }

    // On start, if profile is conservative/moderate/aggressive (which are defaults),
    // we can either let them onboard or go straight to dashboard. Let's make sure
    // if experience/risk are default placeholders we could show onboarding once.
    // Let's go to onboarding by default if they haven't finished it in this session,
    // or let them access it from Settings. To make it extremely user-friendly:
    // We launch onboarding on first run.
    LaunchedEffect(profile, checkedOnboarding) {
        if (!checkedOnboarding) {
            // Check if profile exists and has been configured
            if (profile.riskLevel.isEmpty()) {
                navController.navigate(Routes.ONBOARDING) {
                    popUpTo(0)
                }
            }
            checkedOnboarding = true
        }
    }

    Scaffold(
        bottomBar = {
            // Only show bottom navigation if we are NOT on the onboarding screen
            if (currentRoute != Routes.ONBOARDING && currentRoute != null && !currentRoute.startsWith(Routes.DETAIL)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp) // Premium floating layout
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(CosmicCard)
                            .border(1.dp, BorderSlate.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        val navItems = listOf(
                            Triple(Routes.DASHBOARD, Icons.Default.TrendingUp, "Rates"),
                            Triple(Routes.WATCHLIST, Icons.Default.Star, "Watchlist"),
                            Triple(Routes.SEARCH, Icons.Default.Public, "Search"),
                            Triple(Routes.CHAT, Icons.Default.Chat, "AI Tutor"),
                            Triple(Routes.SETTINGS, Icons.Default.Settings, "Settings")
                        )

                        navItems.forEach { (route, icon, label) ->
                            val isSelected = currentRoute == route
                            val tintColor = if (isSelected) ElectricBlue else TextGrey

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    .testTag("nav_${label.lowercase().replace(" ", "_")}"),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = tintColor,
                                        modifier = Modifier.size(if (isSelected) 22.dp else 19.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = label,
                                        color = tintColor,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(width = 12.dp, height = 3.dp)
                                                .clip(RoundedCornerShape(1.5.dp))
                                                .background(ElectricBlue)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(5.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = CosmicBlack
    ) { innerPadding ->
        // Use responsive constraint: safe maximum width for tablets
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBlack)
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier.fillMaxSize()
            ) {
                // Onboarding Route
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onFinish = {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }

                // Dashboard Route
                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onPairSelected = { symbol ->
                            // Replace slash to make path safe
                            val safeSymbol = symbol.replace("/", "_")
                            navController.navigate("${Routes.DETAIL}/$safeSymbol")
                        }
                    )
                }

                // Watchlist Route
                composable(Routes.WATCHLIST) {
                    WatchlistScreen(
                        viewModel = viewModel,
                        onPairSelected = { symbol ->
                            val safeSymbol = symbol.replace("/", "_")
                            navController.navigate("${Routes.DETAIL}/$safeSymbol")
                        }
                    )
                }

                // Google Search Route
                composable(Routes.SEARCH) {
                    SearchScreen(
                        viewModel = viewModel
                    )
                }

                // AI Chat Route
                composable(
                    route = "${Routes.CHAT}?symbol={symbol}",
                    arguments = listOf(
                        navArgument("symbol") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val rawSymbol = backStackEntry.arguments?.getString("symbol")
                    val symbol = rawSymbol?.replace("_", "/")
                    ChatScreen(
                        preloadedPair = symbol,
                        viewModel = viewModel
                    )
                }

                // General Chat fallback
                composable(Routes.CHAT) {
                    ChatScreen(
                        preloadedPair = null,
                        viewModel = viewModel
                    )
                }

                // Currency Pair Detail Route
                composable(
                    route = "${Routes.DETAIL}/{symbol}",
                    arguments = listOf(
                        navArgument("symbol") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val safeSymbol = backStackEntry.arguments?.getString("symbol") ?: "EUR_USD"
                    val symbol = safeSymbol.replace("_", "/")
                    DetailScreen(
                        symbol = symbol,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToChat = { sym ->
                            val sSym = sym.replace("/", "_")
                            navController.navigate("${Routes.CHAT}?symbol=$sSym")
                        }
                    )
                }

                // Settings Route
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onResetOnboarding = {
                            navController.navigate(Routes.ONBOARDING)
                        }
                    )
                }
            }
        }
    }
}
