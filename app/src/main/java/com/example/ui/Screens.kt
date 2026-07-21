package com.example.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLEncoder
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.data.forex.ForexPairData
import com.example.viewmodel.ForexViewModel
import com.example.viewmodel.MarketUiState
import com.example.viewmodel.RecommendationUiState
import com.example.ui.components.ForexChart
import com.example.ui.theme.*
import kotlinx.coroutines.launch

// --- Onboarding Setup Screen ---

@Composable
fun OnboardingScreen(
    viewModel: ForexViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var risk by remember { mutableStateOf("Moderate") }
    var experience by remember { mutableStateOf("Intermediate") }
    val pairs = listOf("EUR/USD", "GBP/USD", "USD/JPY", "AUD/USD", "USD/CAD", "NZD/USD", "USD/CHF")
    val selectedPairs = remember { mutableStateListOf("EUR/USD", "GBP/USD", "USD/JPY") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = "App Logo",
                tint = ElectricBlue,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Forex AI Recommendations",
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Configure your trading profile to align the Gemini recommendation engine with your financial goals.",
                color = TextGrey,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Trading Risk Option
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Risk Tolerance Level",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val riskLevels = listOf("Conservative", "Moderate", "Aggressive")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        riskLevels.forEach { level ->
                            val selected = risk == level
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) ElectricBlue else BorderSlate)
                                    .clickable { risk = level }
                                    .padding(vertical = 12.dp)
                                    .testTag("risk_btn_$level"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = level,
                                    color = if (selected) Color.White else TextGrey,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when(risk) {
                            "Conservative" -> "Preserves capital. Recommends stable majors (EUR/USD, USD/CHF) and acts only on high-confidence triggers."
                            "Moderate" -> "Balanced returns. Accepts moderate technical index fluctuations with regular range adjustments."
                            else -> "Breakout prioritization. Embraces volatility across GBP, JPY, and commodity pairings for rapid entries."
                        },
                        color = TextGrey,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Trading Experience Option
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Trading Experience Level",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val experiences = listOf("Beginner", "Intermediate", "Advanced")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        experiences.forEach { exp ->
                            val selected = experience == exp
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) NeonCyan else BorderSlate)
                                    .clickable { experience = exp }
                                    .padding(vertical = 12.dp)
                                    .testTag("exp_btn_$exp"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = exp,
                                    color = if (selected) CosmicBlack else TextGrey,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Preferred Pairs Checklist
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. Preferred Pairs",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    pairs.forEach { pair ->
                        val checked = selectedPairs.contains(pair)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selectedPairs.remove(pair) else selectedPairs.add(pair)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (checked) selectedPairs.remove(pair) else selectedPairs.add(pair)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = ElectricBlue,
                                    checkmarkColor = TextWhite
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(pair, color = TextWhite, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.updateRiskProfile(
                        riskLevel = risk,
                        experienceLevel = experience,
                        preferredPairs = selectedPairs.joinToString(","),
                        aiPreferences = "Technical & News Sentiment"
                    )
                    onFinish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(50.dp)
                    .testTag("onboarding_finish_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Launch Platform",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- Dashboard Screen ---

@Composable
fun DashboardScreen(
    viewModel: ForexViewModel,
    onPairSelected: (String) -> Unit,
    onNavigateToChat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val marketState by viewModel.marketUiState.collectAsState()
    val recommendationState by viewModel.recommendationUiState.collectAsState()
    val isRealKey by viewModel.isRealApiKeyConfigured.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    // Real-time linkages with Gemini recommendation outputs
    val firstRec = (recommendationState as? RecommendationUiState.Success)?.report?.recommendedPairs?.firstOrNull()
    val displaySymbol = firstRec?.pair ?: "EUR/USD"
    val displayAction = firstRec?.suggestedAction ?: "STRONG BUY"
    val confidencePercent = firstRec?.confidence ?: 87

    val marketPairs = (marketState as? MarketUiState.Success)?.pairs ?: emptyList()
    val pairData = marketPairs.find { it.symbol == displaySymbol }
    val displayPrice = pairData?.currentPrice?.let { String.format("%.4f", it) } ?: "1.0842"
    val displayChange = pairData?.dailyChangePercent ?: 0.24
    val isUp = displayChange >= 0

    val overallSummary = when (val state = recommendationState) {
        is RecommendationUiState.Success -> state.report.overallSummary
        is RecommendationUiState.Loading -> "Querying Gemini 3.5 Flash Model... Analyzing technical trend patterns and market-wide sentiment..."
        is RecommendationUiState.Error -> "Unable to process dynamic API metrics. Running fallback analytics: ${state.message}"
        else -> "Tap the evaluation trigger to generate a dynamic structured AI recommendation report aligned with your ${profile.riskLevel.lowercase()} risk tolerance."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack)
    ) {
        // Beautiful Geometric Terminal Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "FxAI Terminal",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(BullishGreen)
                        )
                        Text(
                            text = "Market Live",
                            color = TextGrey,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Right side Action Tray: Notification Bell & User Profile Risk Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val notifications by viewModel.notifications.collectAsState()
                val unreadCount = notifications.count { !it.isRead }
                var showNotificationsCenter by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CosmicCard)
                        .border(1.dp, BorderSlate, RoundedCornerShape(10.dp))
                        .clickable { showNotificationsCenter = true }
                        .testTag("notification_bell_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (unreadCount > 0) NeonCyan else TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color.Red)
                            )
                        }
                    }
                }

                if (showNotificationsCenter) {
                    NotificationsCenterDialog(
                        notifications = notifications,
                        onDismiss = { showNotificationsCenter = false },
                        onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
                        onDeleteNotification = { id -> viewModel.deleteNotification(id) },
                        onClearAll = { viewModel.clearAllNotifications() }
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(TagBackground)
                        .border(1.dp, BorderSlate, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = profile.riskLevel,
                            color = TextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Warning banner for simulation mode
        if (!isRealKey) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9100).copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Simulation Active",
                        tint = Color(0xFFFF9100),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Demo Mode: Running offline AI indicators. Add GEMINI_API_KEY in Secrets for live processing.",
                        color = TextWhite,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Master Summary Dashboard Card in SpecCardBlue
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, GridLineSlate.copy(alpha = 0.5f))
        ) {
            val primaryColor = ElectricBlue
            val isLight = MaterialTheme.colorScheme.background == LightBgColor
            val gradientColors = if (isLight) {
                listOf(primaryColor.copy(alpha = 0.15f), LightCardColor)
            } else {
                listOf(primaryColor.copy(alpha = 0.22f), CosmicCard)
            }
            val textColor = if (isLight) Color(0xFF001E2F) else TextWhite

            Column(
                modifier = Modifier
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(colors = gradientColors)
                    )
                    .padding(16.dp)
            ) {
                // Header of Recommendation Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(primaryColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TOP RECOMMENDATION",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        if (recommendationState is RecommendationUiState.Loading) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = primaryColor
                            )
                        }
                    }

                    // Improved Refresh Button and Text!
                    TextButton(
                        onClick = { viewModel.generateAIRecommendations() },
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(primaryColor.copy(alpha = 0.15f))
                            .testTag("re_evaluate_ai_btn"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Ask Gemini",
                                tint = primaryColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (recommendationState is RecommendationUiState.Loading) "Analyzing..." else "Ask Gemini",
                                color = primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Core Asset & Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = displaySymbol,
                            color = textColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Action: $displayAction",
                            color = if (displayAction.contains("SELL")) BearishRed else BullishGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = displayPrice,
                            color = textColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isUp) BullishGreen else BearishRed,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${if (isUp) "+" else ""}${String.format("%.2f", displayChange)}%",
                                color = if (isUp) BullishGreen else BearishRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Confidence Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI CONFIDENCE INDICATOR",
                            color = TextGrey,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$confidencePercent%",
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { confidencePercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = primaryColor,
                        trackColor = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Summary Rationale text
                Text(
                    text = overallSummary,
                    color = textColor.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                if (recommendationState is RecommendationUiState.Success) {
                    val report = (recommendationState as RecommendationUiState.Success).report
                    if (!report.thinkingProcess.isNullOrBlank()) {
                        var isThinkingExpanded by remember { mutableStateOf(false) }
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("rec_thinking_card"),
                            colors = CardDefaults.cardColors(containerColor = CosmicBlack.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, BorderSlate.copy(alpha = 0.7f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isThinkingExpanded = !isThinkingExpanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = "Thinking Icon",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "AI DEEP THINKING PROCESS",
                                            color = NeonCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isThinkingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle Thinking",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (isThinkingExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = report.thinkingProcess,
                                        color = TextWhite.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = report.disclaimer,
                        color = TextGrey.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        lineHeight = 11.sp
                    )
                }
            }
        }

        // Sub-Tab Selector Segmented Row
        var activeSubTab by remember { mutableStateOf("Live Rates") } // "Live Rates", "Calendar", "Calculator"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CosmicCard)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Live Rates", "Calendar", "Calculator").forEach { tab ->
                val isSelected = activeSubTab == tab
                Button(
                    onClick = { activeSubTab = tab },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("dashboard_sub_tab_${tab.lowercase().replace(" ", "_")}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) ElectricBlue else Color.Transparent,
                        contentColor = if (isSelected) Color.White else TextGrey
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = tab,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dynamic Sub-Feature Render Block
        when (activeSubTab) {
            "Live Rates" -> {
                // Live Quotes Ticker header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Ticker Indexes",
                        color = TextGrey,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(BullishGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulating 10s Feeds", color = TextGrey, fontSize = 10.sp)
                    }
                }

                // Live Rate Cards
                when (val state = marketState) {
                    is MarketUiState.Loading -> {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ElectricBlue)
                        }
                    }
                    is MarketUiState.Error -> {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(state.message, color = BearishRed)
                        }
                    }
                    is MarketUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.pairs) { pair ->
                                ForexPairRowCard(
                                    pair = pair,
                                    isWatchlisted = viewModel.watchlistSymbols.collectAsState().value.contains(pair.symbol),
                                    onWatchlistToggle = { viewModel.toggleWatchlist(pair.symbol) },
                                    onClick = { onPairSelected(pair.symbol) }
                                )
                            }
                        }
                    }
                }
            }
            "Calendar" -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    EconomicCalendarView(
                        onNavigateToChat = { query ->
                            viewModel.sendChatMessage(query)
                            onNavigateToChat()
                        }
                    )
                }
            }
            "Calculator" -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    PositionCalculatorView()
                }
            }
        }
    }
}

@Composable
fun ForexPairRowCard(
    pair: ForexPairData,
    isWatchlisted: Boolean,
    onWatchlistToggle: () -> Unit,
    onClick: () -> Unit
) {
    val isUp = pair.dailyChangePercent >= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("pair_card_${pair.symbol.replace("/", "_")}"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, BorderSlate)
    ) {
        val isLight = MaterialTheme.colorScheme.background == LightBgColor
        Row(
            modifier = Modifier
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = if (isLight) {
                            listOf(Color(0xFFFFFFFF), Color(0xFFF4F7FC))
                        } else {
                            listOf(Color(0xFF131926), Color(0xFF0D121F))
                        }
                    )
                )
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pair.symbol,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onWatchlistToggle,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (isWatchlisted) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Watchlist",
                            tint = if (isWatchlisted) NeonCyan else TextGrey,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = pair.name,
                    color = TextGrey,
                    fontSize = 11.sp
                )
            }

            // Price Indicators
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = String.format("%.4f", pair.currentPrice),
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = "Trend direction",
                        tint = if (isUp) BullishGreen else BearishRed,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${if (isUp) "+" else ""}${String.format("%.2f", pair.dailyChangePercent)}%",
                        color = if (isUp) BullishGreen else BearishRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Quick summary badge
            val recAction = when {
                pair.dailyChangePercent > 0.05 -> "BUY"
                pair.dailyChangePercent < -0.05 -> "SELL"
                else -> "HOLD"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when (recAction) {
                            "BUY" -> BullishGreen.copy(alpha = 0.15f)
                            "SELL" -> BearishRed.copy(alpha = 0.15f)
                            else -> TextGrey.copy(alpha = 0.15f)
                        }
                    )
                    .border(
                        1.dp,
                        when (recAction) {
                            "BUY" -> BullishGreen
                            "SELL" -> BearishRed
                            else -> TextGrey
                        },
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = recAction,
                    color = when (recAction) {
                        "BUY" -> BullishGreen
                        "SELL" -> BearishRed
                        else -> TextWhite
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- Detail Screen ---

@Composable
fun DetailScreen(
    symbol: String,
    viewModel: ForexViewModel,
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pairs by viewModel.pairsFlow.collectAsState()
    
    // Fallback if direct live collection isn't seeded yet
    val pairData = remember(pairs, symbol) {
        pairs.firstOrNull { it.symbol == symbol } ?: com.example.data.forex.ForexEngine.getPairData(symbol)
    }

    val isWatchlisted = viewModel.watchlistSymbols.collectAsState().value.contains(symbol)
    val recommendationState by viewModel.recommendationUiState.collectAsState()

    // Grab matching recommendation if exists in active state
    val specificRec = remember(recommendationState, symbol) {
        if (recommendationState is RecommendationUiState.Success) {
            (recommendationState as RecommendationUiState.Success).report.recommendedPairs.firstOrNull { it.pair == symbol }
        } else {
            null
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Navigation Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_btn")) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = pairData.symbol, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(text = pairData.name, color = TextGrey, fontSize = 12.sp)
                }
                IconButton(onClick = { viewModel.toggleWatchlist(symbol) }) {
                    Icon(
                        imageVector = if (isWatchlisted) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Watchlist toggle",
                        tint = if (isWatchlisted) NeonCyan else TextWhite
                    )
                }
            }
        }

        // Live Rate Header Card
        item {
            val isUp = pairData.dailyChangePercent >= 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = String.format("%.4f", pairData.currentPrice),
                        color = TextWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = "Trend direction",
                            tint = if (isUp) BullishGreen else BearishRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (isUp) "+" else ""}${String.format("%.2f", pairData.dailyChangePercent)}%",
                            color = if (isUp) BullishGreen else BearishRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Volatility Gauge
                Column(horizontalAlignment = Alignment.End) {
                    Text("Volatility", color = TextGrey, fontSize = 11.sp)
                    Text(
                        text = "${String.format("%.1f", pairData.volatilityScore)}/10",
                        color = if (pairData.volatilityScore > 7.0) BearishRed else NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Custom Interactive Chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("30-Day Historical Candle Index", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    ForexChart(history = pairData.history, modifier = Modifier.weight(1f))
                }
            }
        }

        // Gemini AI Recommendation Block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Gemini Recommendation Report",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (specificRec != null) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (specificRec.suggestedAction) {
                                                "BUY" -> BullishGreen.copy(alpha = 0.15f)
                                                "SELL" -> BearishRed.copy(alpha = 0.15f)
                                                "WATCH" -> NeonCyan.copy(alpha = 0.15f)
                                                else -> TextGrey.copy(alpha = 0.15f)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            when (specificRec.suggestedAction) {
                                                "BUY" -> BullishGreen
                                                "SELL" -> BearishRed
                                                "WATCH" -> NeonCyan
                                                else -> TextGrey
                                            },
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Action: ${specificRec.suggestedAction}",
                                        color = when (specificRec.suggestedAction) {
                                            "BUY" -> BullishGreen
                                            "SELL" -> BearishRed
                                            "WATCH" -> NeonCyan
                                            else -> TextWhite
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Text(
                                    text = "Confidence: ${specificRec.confidence}%",
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Rationale:",
                                color = TextGrey,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = specificRec.rationale,
                                color = TextWhite,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Key News Indicators Evaluated:",
                                color = TextGrey,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            specificRec.keyNews.forEach { topic ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(NeonCyan)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(topic, color = TextWhite, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        // Generate dynamic local preview recommendation
                        val localSimRec = remember(pairData) {
                            com.example.data.api.GeminiClient.generateMockAIResponse(
                                pairsData = listOf(pairData),
                                riskLevel = viewModel.userProfile.value.riskLevel,
                                experienceLevel = viewModel.userProfile.value.experience
                            ).recommendedPairs.firstOrNull()
                        }

                        if (localSimRec != null) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when (localSimRec.suggestedAction) {
                                                    "BUY" -> BullishGreen.copy(alpha = 0.15f)
                                                    "SELL" -> BearishRed.copy(alpha = 0.15f)
                                                    "WATCH" -> NeonCyan.copy(alpha = 0.15f)
                                                    else -> TextGrey.copy(alpha = 0.15f)
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                when (localSimRec.suggestedAction) {
                                                    "BUY" -> BullishGreen
                                                    "SELL" -> BearishRed
                                                    "WATCH" -> NeonCyan
                                                    else -> TextGrey
                                                },
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Action: ${localSimRec.suggestedAction}",
                                            color = when (localSimRec.suggestedAction) {
                                                "BUY" -> BullishGreen
                                                "SELL" -> BearishRed
                                                "WATCH" -> NeonCyan
                                                else -> TextWhite
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Text(
                                        text = "Confidence: ${localSimRec.confidence}%",
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Simulated Analysis:",
                                    color = TextGrey,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = localSimRec.rationale,
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Key Topics:",
                                    color = TextGrey,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                localSimRec.keyNews.forEach { topic ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(ElectricBlue)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(topic, color = TextWhite, fontSize = 12.sp)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.generateAIRecommendations() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                                ) {
                                    Text("Analyze with Live Gemini API")
                                }
                            }
                        }
                    }
                }
            }
        }

        // News Snippets
        item {
            Text(
                text = "Market News Snippets",
                color = TextGrey,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(pairData.news) { newsTitle ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Announcement,
                        contentDescription = "News",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = newsTitle,
                        color = TextWhite,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Ask assistant trigger
        item {
            Button(
                onClick = { onNavigateToChat(symbol) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(50.dp)
                    .testTag("detail_chat_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat icon")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Discuss ${symbol} with AI Assistant")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- AI Chat Screen ---

@Composable
fun ChatScreen(
    preloadedPair: String?,
    viewModel: ForexViewModel,
    modifier: Modifier = Modifier
) {
    val chatLogs by viewModel.chatHistory.collectAsState()
    val loading by viewModel.chatLoading.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Rapid questions chips
    val chipOptions = listOf(
        "Why do you recommend EUR/USD?",
        "Explain volatility indices today.",
        "Which pair fits a conservative style?"
    )

    // Scroll to bottom when logs update
    LaunchedEffect(chatLogs.size, loading) {
        if (chatLogs.isNotEmpty()) {
            scrollState.animateScrollToItem(chatLogs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack)
            .padding(16.dp)
    ) {
        // Chat Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("AI Trading Assistant", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = if (preloadedPair != null) "Discussing $preloadedPair • Strategy: ${profile.riskLevel}" else "Strategy Tutor • ${profile.riskLevel}",
                    color = TextGrey,
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = { viewModel.clearChat() },
                modifier = Modifier.testTag("clear_chat_btn")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = TextGrey)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat logs bubble list
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (chatLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Zen chat logo",
                                tint = ElectricBlue.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ask anything about trading strategies, volatility bounds, or daily currency breakdowns.",
                                color = TextGrey,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(chatLogs) { log ->
                    // User bubble
                    ChatBubble(text = log.first, isUser = true)
                    
                    // Assistant bubble (only if populated)
                    if (log.second.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ChatBubble(text = log.second, isUser = false)
                    }
                }
            }

            if (loading) {
                item {
                    var thinkingStep by remember { mutableStateOf(0) }
                    val thinkingTexts = listOf(
                        "Analyzing market liquidity & historical corridors...",
                        "Cross-referencing global rate statements & news indices...",
                        "Synthesizing risk profile thresholds...",
                        "Assembling detailed algorithmic recommendations..."
                    )
                    LaunchedEffect(Unit) {
                        while (true) {
                            kotlinx.coroutines.delay(2500)
                            thinkingStep = (thinkingStep + 1) % thinkingTexts.size
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Deep Thinking active...", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = thinkingTexts[thinkingStep],
                            color = TextGrey,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(start = 24.dp)
                        )
                    }
                }
            }
        }

        // Rapid starter chips
        if (chatLogs.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chipOptions.forEach { opt ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicCard)
                            .border(1.dp, BorderSlate, RoundedCornerShape(8.dp))
                            .clickable { viewModel.sendChatMessage(opt, preloadedPair) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opt,
                            color = TextWhite,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                placeholder = { Text("Ask about currency models...", color = TextGrey, fontSize = 13.sp) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = CosmicCard,
                    unfocusedContainerColor = CosmicCard,
                    disabledContainerColor = CosmicCard,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendChatMessage(textInput, preloadedPair)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricBlue)
                    .size(48.dp)
                    .testTag("chat_send_btn")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

private fun extractThinking(text: String): Pair<String?, String> {
    val startTag = "<thinking>"
    val endTag = "</thinking>"
    val startIndex = text.indexOf(startTag)
    val endIndex = text.indexOf(endTag)
    
    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
        val thinking = text.substring(startIndex + startTag.length, endIndex).trim()
        val remaining = (text.substring(0, startIndex) + text.substring(endIndex + endTag.length)).trim()
        return Pair(thinking, remaining)
    }
    return Pair(null, text)
}

@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val parsed = remember(text) { extractThinking(text) }
        val thinking = parsed.first
        val contentText = parsed.second

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // Expandable Thinking Process block
            if (!isUser && !thinking.isNullOrBlank()) {
                var isThinkingExpanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicCard)
                        .border(1.dp, BorderSlate.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .clickable { isThinkingExpanded = !isThinkingExpanded }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Thinking",
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Thinking Process",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (isThinkingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        if (isThinkingExpanded) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = thinking,
                                color = TextGrey,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }

            // Actual chat response bubble
            if (contentText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isUser) 12.dp else 0.dp,
                                bottomEnd = if (isUser) 0.dp else 12.dp
                            )
                        )
                        .background(if (isUser) ElectricBlue else CosmicCard)
                        .border(
                            width = 1.dp,
                            color = if (isUser) ElectricBlue else BorderSlate,
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isUser) 12.dp else 0.dp,
                                bottomEnd = if (isUser) 0.dp else 12.dp
                            )
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = contentText,
                        color = if (isUser) Color.White else TextWhite,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// --- Watchlist Screen ---

@Composable
fun WatchlistScreen(
    viewModel: ForexViewModel,
    onPairSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val marketState by viewModel.marketUiState.collectAsState()
    val watchlist by viewModel.watchlistSymbols.collectAsState()
    val alerts by viewModel.priceAlerts.collectAsState()

    var alertSymbol by remember { mutableStateOf("EUR/USD") }
    var alertPrice by remember { mutableStateOf("1.0900") }
    var alertIsAbove by remember { mutableStateOf(true) }

    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack)
    ) {
        // Watchlist Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Star, contentDescription = "Favorites", tint = NeonCyan, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Watchlist & Active Alerts", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Price Alerts Manager Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicCard),
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Alerts", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set Custom Price Alerts", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Selection Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Symbol selector row
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("Currency Pair", color = TextGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(BorderSlate).padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    listOf("EUR/USD", "GBP/USD", "USD/JPY").forEach { p ->
                                        val isSel = alertSymbol == p
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSel) ElectricBlue else Color.Transparent)
                                                .clickable { alertSymbol = p }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(p.replace("/USD", "").replace("USD/", ""), color = if (isSel) Color.White else TextWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Price target input
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Target Price", color = TextGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = alertPrice,
                                    onValueChange = { alertPrice = it },
                                    modifier = Modifier.fillMaxWidth().testTag("alert_price_input"),
                                    textStyle = TextStyle(color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricBlue,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedLabelColor = ElectricBlue,
                                        unfocusedLabelColor = TextGrey
                                    ),
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Direction selector & ADD button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Condition:", color = TextGrey, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(BorderSlate).padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    listOf(true, false).forEach { dir ->
                                        val isSelected = alertIsAbove == dir
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSelected) ElectricBlue else Color.Transparent)
                                                .clickable { alertIsAbove = dir }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(if (dir) "Above (>=)" else "Below (<=)", color = if (isSelected) Color.White else TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val pr = alertPrice.toDoubleOrNull()
                                    if (pr != null) {
                                        viewModel.addPriceAlert(alertSymbol, pr, alertIsAbove)
                                        android.widget.Toast.makeText(context, "Alert configured successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Please enter a valid target price.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.height(34.dp).testTag("create_alert_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                    Text("Add Alert", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Alerts Active List
                        if (alerts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = BorderSlate.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Active Price Alerts", color = TextGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                alerts.forEach { alert ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BorderSlate.copy(alpha = 0.3f))
                                            .border(1.dp, BorderSlate.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(
                                                imageVector = if (alert.isTriggered) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                                contentDescription = null,
                                                tint = if (alert.isTriggered) BearishRed else if (alert.isEnabled) NeonCyan else TextGrey,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Column {
                                                Text(alert.symbol, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = "${if (alert.isAbove) "Price >= " else "Price <= "}${String.format("%.4f", alert.targetPrice)}",
                                                    color = TextGrey,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (alert.isTriggered) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(BearishRed.copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("TRIGGERED", color = BearishRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Switch(
                                                    checked = alert.isEnabled,
                                                    onCheckedChange = { viewModel.toggleAlertEnabled(alert.id) },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = ElectricBlue,
                                                        checkedTrackColor = ElectricBlue.copy(alpha = 0.4f)
                                                    )
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.deletePriceAlert(alert.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextGrey, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Divider or Header for watchlisted indices
            item {
                Text(
                    text = "Pinned Watchlist Indices",
                    color = TextGrey,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                )
            }

            // original watchlist indices
            if (watchlist.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.StarBorder,
                                contentDescription = "Empty Watchlist",
                                tint = TextGrey.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your watchlist is empty.",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap the star icon beside currency labels on the dashboard to pin favorites here for easy tracking.",
                                color = TextGrey,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp)
                            )
                        }
                    }
                }
            } else {
                when (val state = marketState) {
                    is MarketUiState.Loading -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = ElectricBlue)
                            }
                        }
                    }
                    is MarketUiState.Error -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text(state.message, color = BearishRed, fontSize = 12.sp)
                            }
                        }
                    }
                    is MarketUiState.Success -> {
                        val filteredPairs = state.pairs.filter { watchlist.contains(it.symbol) }
                        items(filteredPairs) { pair ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                ForexPairRowCard(
                                    pair = pair,
                                    isWatchlisted = true,
                                    onWatchlistToggle = { viewModel.toggleWatchlist(pair.symbol) },
                                    onClick = { onPairSelected(pair.symbol) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Settings Screen ---

@Composable
fun SettingsScreen(
    viewModel: ForexViewModel,
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val isRealKey by viewModel.isRealApiKeyConfigured.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = ElectricBlue, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Settings & Security", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }

        // Display Theme Preference Switcher Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Display Mode", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Set your preferred visual aesthetic for the platform.", color = TextGrey, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Light", "Dark", "Auto").forEach { mode ->
                            val isSelected = profile.themeMode == mode
                            Button(
                                onClick = { viewModel.updateThemeMode(mode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("theme_btn_$mode"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) ElectricBlue else BorderSlate,
                                    contentColor = if (isSelected) Color.White else TextWhite
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(mode, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Dynamic Accent Theme & Custom Color Picker Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dynamic Accent Color", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Customize the application primary accent color instantly.", color = TextGrey, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Presets Grid
                    Text("SELECT PRESET", color = TextGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val presets = listOf(
                        "#2563EB" to "Sapphire Blue",
                        "#10B981" to "Emerald Jade",
                        "#8B5CF6" to "Mystic Amethyst",
                        "#EF4444" to "Crimson Ruby",
                        "#F59E0B" to "Radiant Amber",
                        "#0EA5E9" to "Electric Cyan",
                        "#EC4899" to "Neon Pink"
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presets.take(4).forEach { (hex, name) ->
                            val isSelected = profile.primaryColorHex.lowercase() == hex.lowercase()
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { viewModel.updatePrimaryColor(hex) }
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) TextWhite else Color.Transparent,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presets.drop(4).forEach { (hex, name) ->
                            val isSelected = profile.primaryColorHex.lowercase() == hex.lowercase()
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { viewModel.updatePrimaryColor(hex) }
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) TextWhite else Color.Transparent,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Slider-based Custom Color Picker (HUE Slider)
                    Text("CUSTOM HUE PICKER", color = TextGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val currentHSV = remember(profile.primaryColorHex) {
                        val hsv = FloatArray(3)
                        try {
                            android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(profile.primaryColorHex), hsv)
                        } catch (e: Exception) {
                            android.graphics.Color.colorToHSV(android.graphics.Color.parseColor("#2563EB"), hsv)
                        }
                        hsv
                    }
                    
                    var sliderHue by remember(profile.primaryColorHex) { mutableStateOf(currentHSV[0]) }
                    
                    Slider(
                        value = sliderHue,
                        onValueChange = { h ->
                            sliderHue = h
                            val hsv = floatArrayOf(h, 0.85f, 0.95f)
                            val colorInt = android.graphics.Color.HSVToColor(hsv)
                            val hexStr = String.format("#%06X", 0xFFFFFF and colorInt)
                            viewModel.updatePrimaryColor(hexStr)
                        },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(sliderHue, 0.85f, 0.95f))),
                            activeTrackColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(sliderHue, 0.85f, 0.95f))).copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("hue_color_picker_slider")
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Hex:", color = TextGrey, fontSize = 12.sp)
                        Text(
                            text = profile.primaryColorHex.uppercase(),
                            color = Color(android.graphics.Color.parseColor(profile.primaryColorHex)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // External API Integration Settings Card
        item {
            var extUrl by remember(profile.externalApiUrl) { mutableStateOf(profile.externalApiUrl) }
            var extKey by remember(profile.externalApiKey) { mutableStateOf(profile.externalApiKey) }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Dns, contentDescription = "API Server", tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("External Financial API", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Integrate custom market providers or external news endpoints.", color = TextGrey, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = extUrl,
                        onValueChange = { extUrl = it },
                        label = { Text("API Endpoint URL", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("api_url_input"),
                        textStyle = TextStyle(color = TextWhite, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = BorderSlate,
                            focusedLabelColor = ElectricBlue,
                            unfocusedLabelColor = TextGrey
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = extKey,
                        onValueChange = { extKey = it },
                        label = { Text("API Credential Key", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
                        textStyle = TextStyle(color = TextWhite, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = BorderSlate,
                            focusedLabelColor = ElectricBlue,
                            unfocusedLabelColor = TextGrey
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = { 
                            viewModel.updateExternalApiSettings(extKey, extUrl)
                            android.widget.Toast.makeText(context, "Credentials saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("save_api_settings_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Save Credentials", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Risk Profile details Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Risk Profiling Profile", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Risk Tolerance:", color = TextGrey, fontSize = 13.sp)
                        Text(profile.riskLevel, color = ElectricBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Experience level:", color = TextGrey, fontSize = 13.sp)
                        Text(profile.experience, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Preferred index lists:", color = TextGrey, fontSize = 13.sp)
                        Text(profile.preferredPairs, color = TextWhite, fontSize = 11.sp, maxLines = 1)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onResetOnboarding,
                        modifier = Modifier.fillMaxWidth().testTag("reset_profile_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BorderSlate)
                    ) {
                        Text("Update Risk Profile Settings")
                    }
                }
            }
        }

        // Mock Ticker alert Notifications
        item {
            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                viewModel.toggleNotifications(isGranted)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Local Notifications Ticker", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Receive alert popups on significant daily rate shifts.", color = TextGrey, fontSize = 11.sp)
                    }
                    Switch(
                        checked = profile.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.toggleNotifications(enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricBlue,
                            checkedTrackColor = ElectricBlue.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        // Required mandated security warnings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("API Credentials Status", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isRealKey) BullishGreen else Color(0xFFFF9100))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRealKey) "Gemini API Active (Connected)" else "Simulation Engine (Using Fallbacks)",
                            color = if (isRealKey) BullishGreen else Color(0xFFFF9100),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                        color = TextGrey,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Justify
                    )
                }
            }
        }

        // Regulatory standard disclaimer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Gavel, contentDescription = "Disclaimer", tint = TextGrey, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Regulatory Standard Disclaimer", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This platform does not provide actual financial advice. Every analysis generated by the AI is for educational and informational purposes only. Currency trading involves severe exposure to leverage and volatility, and you remain solely responsible for your own capital decisions. Past performance does not guarantee future indicators.",
                        color = TextGrey,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Justify
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// --- Google Search Screen ---

@Composable
fun SearchScreen(
    viewModel: ForexViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf<String?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoadingWeb by remember { mutableStateOf(false) }

    val presetSuggestions = listOf(
        "EUR/USD trend news",
        "Fed Interest Rates live",
        "DXY Index live chart",
        "Forex daily market analysis",
        "Gemini AI Forex prediction"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack)
    ) {
        // Search Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "Google Market Search",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Live macroeconomic news & web tracking",
                    color = TextGrey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Search Input Bar Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            border = BorderStroke(1.dp, BorderSlate),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Google for Forex news...", color = TextGrey, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextGrey
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextGrey
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("google_search_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BorderSlate,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (searchQuery.trim().isNotEmpty()) {
                            val encoded = URLEncoder.encode(searchQuery.trim(), "UTF-8")
                            currentUrl = "https://www.google.com/search?q=$encoded"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("google_search_btn")
                ) {
                    Text("Search", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Suggestion Chips
        if (currentUrl == null) {
            Text(
                text = "POPULAR FOREX SEARCHES",
                color = TextGrey,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
            )

            // Dynamic horizontal container for popular searches (wrapped with single-line or row padding)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetSuggestions.take(3).forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(TagBackground)
                            .border(1.dp, BorderSlate, RoundedCornerShape(20.dp))
                            .clickable {
                                searchQuery = suggestion
                                val encoded = URLEncoder.encode(suggestion, "UTF-8")
                                currentUrl = "https://www.google.com/search?q=$encoded"
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = suggestion,
                            color = TextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Welcome State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Search Google",
                        tint = ElectricBlue.copy(alpha = 0.5f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Integrated Web Search Terminal",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Instantly explore global live markets, currency forums, FOMC schedules, and expert financial analysis directly from the Google ecosystem inside this terminal.",
                        color = TextGrey,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            // Browser toolbar & Webview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicCard)
                    .border(1.dp, BorderSlate)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            if (webViewInstance?.canGoBack() == true) {
                                webViewInstance?.goBack()
                            }
                        },
                        enabled = webViewInstance?.canGoBack() == true
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (webViewInstance?.canGoBack() == true) ElectricBlue else TextGrey.copy(alpha = 0.4f)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (webViewInstance?.canGoForward() == true) {
                                webViewInstance?.goForward()
                            }
                        },
                        enabled = webViewInstance?.canGoForward() == true
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (webViewInstance?.canGoForward() == true) ElectricBlue else TextGrey.copy(alpha = 0.4f)
                        )
                    }

                    IconButton(
                        onClick = {
                            webViewInstance?.reload()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = ElectricBlue
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            currentUrl = null
                            searchQuery = ""
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = ElectricBlue
                        )
                    }
                }
            }

            if (isLoadingWeb) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = ElectricBlue,
                    trackColor = Color.Transparent
                )
            }

            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoadingWeb = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoadingWeb = false
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        webViewInstance = this
                        currentUrl?.let { loadUrl(it) }
                    }
                },
                update = { webView ->
                    if (webView.url != currentUrl && currentUrl != null) {
                        currentUrl?.let { webView.loadUrl(it) }
                    }
                    webViewInstance = webView
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

// --- Economic Calendar Sub-Feature ---

@Composable
fun EconomicCalendarView(
    onNavigateToChat: (String) -> Unit
) {
    val events = remember {
        listOf(
            EconomicEvent("15:30", "USD", "Non-Farm Payrolls (NFP)", "HIGH", "200K", "185K", "215K", "The primary measure of US job creation. High values usually strengthen the USD."),
            EconomicEvent("11:00", "EUR", "ECB Interest Rate Decision", "HIGH", "4.25%", "4.50%", "4.25%", "Refined monetary policy statement. Lower rates generally weaken the Euro."),
            EconomicEvent("14:30", "GBP", "CPI Inflation Rate YoY", "HIGH", "2.1%", "2.0%", "2.2%", "Measures UK consumer price index shifts. Higher values may force BoE interest hikes."),
            EconomicEvent("17:45", "USD", "FOMC Meeting Minutes", "MEDIUM", "N/A", "N/A", "N/A", "Detailed record of US Federal Reserve policy meetings. Drives technical trend sentiment."),
            EconomicEvent("09:15", "CHF", "S foreign currency reserves", "LOW", "710B", "705B", "712B", "Reflects Swiss National Bank Forex interventions.")
        )
    }

    var reminderSetMap by remember { mutableStateOf(emptyMap<String, Boolean>()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Economic Events Tracker",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "High-impact macro milestones dictating currency volatility index vectors.",
                color = TextGrey,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
        }

        items(events) { event ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Country Flag Badge / Currency indicator
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ElectricBlue.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(event.currency, color = ElectricBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            // Impact Badge
                            val impactColor = when (event.impact) {
                                "HIGH" -> BearishRed
                                "MEDIUM" -> Color(0xFFFF9100)
                                else -> TextGrey
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(impactColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("${event.impact} IMPACT", color = impactColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(event.time, color = TextGrey, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(event.title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(event.description, color = TextGrey, fontSize = 11.sp, lineHeight = 15.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text("ACTUAL", color = TextGrey, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(event.actual, color = if (event.actual != "N/A") TextWhite else TextGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("FORECAST", color = TextGrey, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(event.forecast, color = TextGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("PREVIOUS", color = TextGrey, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(event.previous, color = TextGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val isReminderSet = reminderSetMap[event.title] == true
                            // Set Reminder button
                            IconButton(
                                onClick = {
                                    val nextState = !isReminderSet
                                    reminderSetMap = reminderSetMap + (event.title to nextState)
                                    val msg = if (nextState) "Reminder active for ${event.title}" else "Reminder removed"
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isReminderSet) NeonCyan.copy(alpha = 0.15f) else BorderSlate)
                            ) {
                                Icon(
                                    imageVector = if (isReminderSet) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = "Set Notification Alert",
                                    tint = if (isReminderSet) NeonCyan else TextGrey,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Ask AI Tutor
                            IconButton(
                                onClick = {
                                    val query = "Explain how the economic event '${event.title}' for currency ${event.currency} affects foreign exchange markets and what trading strategies apply."
                                    onNavigateToChat(query)
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BorderSlate)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Analyze with AI Tutor",
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class EconomicEvent(
    val time: String,
    val currency: String,
    val title: String,
    val impact: String,
    val forecast: String,
    val previous: String,
    val actual: String,
    val description: String
)

// --- Position Size and Risk Calculator Sub-Feature ---

@Composable
fun PositionCalculatorView() {
    var accountCurrency by remember { mutableStateOf("USD") }
    var accountBalance by remember { mutableStateOf("10000") }
    var riskPercent by remember { mutableStateOf("1.0") }
    var stopLossPips by remember { mutableStateOf("20") }
    var selectedPair by remember { mutableStateOf("EUR/USD") }

    val computedLotSize = remember(accountBalance, riskPercent, stopLossPips, selectedPair) {
        try {
            val balance = accountBalance.toDoubleOrNull() ?: 0.0
            val risk = riskPercent.toDoubleOrNull() ?: 0.0
            val stopLoss = stopLossPips.toDoubleOrNull() ?: 1.0
            
            // Standard risk computation: Cash Risk = Balance * (Risk / 100)
            val cashRisk = balance * (risk / 100.0)
            
            // Pip value multiplier
            // For standard pairs like EUR/USD, 1 pip for 1 standard lot (100,000 units) is $10 USD.
            // For USD/JPY, pip size calculation accounts for currency quote. For simplicity, we approximate:
            val pipValueMultiplier = if (selectedPair.contains("JPY")) 8.5 else 10.0
            
            // Standard Lots = Cash Risk / (Stop Loss * Pip Value of 1 Lot)
            val rawLots = cashRisk / (stopLoss * pipValueMultiplier)
            
            // Coerce/format
            val finalLots = rawLots.coerceAtLeast(0.01).coerceAtMost(100.0)
            val totalUnits = (finalLots * 100000.0).toInt()
            
            Triple(String.format("%.2f", finalLots), String.format("%,d", totalUnits), String.format("%.2f", cashRisk))
        } catch (e: Exception) {
            Triple("0.00", "0", "0.00")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Position Size & Risk Calculator",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Determine the optimal lot sizes to manage trading risk securely according to your threshold rules.",
                color = TextGrey,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicCard),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Account Balance
                    OutlinedTextField(
                        value = accountBalance,
                        onValueChange = { accountBalance = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Account Balance ($accountCurrency)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("calc_balance_input"),
                        textStyle = TextStyle(color = TextWhite, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = BorderSlate,
                            focusedLabelColor = ElectricBlue,
                            unfocusedLabelColor = TextGrey
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Risk Percent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = riskPercent,
                            onValueChange = { riskPercent = it.filter { char -> char.isDigit() || char == '.' } },
                            label = { Text("Risk Ratio (%)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("calc_risk_input"),
                            textStyle = TextStyle(color = TextWhite, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = BorderSlate,
                                focusedLabelColor = ElectricBlue,
                                unfocusedLabelColor = TextGrey
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = stopLossPips,
                            onValueChange = { stopLossPips = it.filter { char -> char.isDigit() } },
                            label = { Text("Stop Loss (Pips)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("calc_sl_input"),
                            textStyle = TextStyle(color = TextWhite, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = BorderSlate,
                                focusedLabelColor = ElectricBlue,
                                unfocusedLabelColor = TextGrey
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Currency Selector & Pair Choice Row
                    Text("Currency Pair Option", color = TextGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("EUR/USD", "GBP/USD", "USD/JPY", "AUD/USD").forEach { pair ->
                            val isSelected = selectedPair == pair
                            Button(
                                onClick = { selectedPair = pair },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) ElectricBlue else BorderSlate,
                                    contentColor = if (isSelected) Color.White else TextWhite
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(pair, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Result Card with glowing gradient border
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF131926), Color(0xFF111827))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Text("CALCULATED EXPOSURE", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Recommended Lot Size", color = TextGrey, fontSize = 11.sp)
                            Text("${computedLotSize.first} Lots", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Risk Capital Value", color = TextGrey, fontSize = 11.sp)
                            Text("$${computedLotSize.third} USD", color = BearishRed, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderSlate.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Standard Units", color = TextGrey, fontSize = 10.sp)
                            Text("${computedLotSize.second} units", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        val safetyVerdict = when {
                            (riskPercent.toDoubleOrNull() ?: 0.0) <= 1.0 -> "Safe (Low Risk)"
                            (riskPercent.toDoubleOrNull() ?: 0.0) <= 3.0 -> "Moderate Exposure"
                            else -> "Alert: Aggressive Risk!"
                        }
                        val verdictColor = when {
                            (riskPercent.toDoubleOrNull() ?: 0.0) <= 1.0 -> BullishGreen
                            (riskPercent.toDoubleOrNull() ?: 0.0) <= 3.0 -> Color(0xFFFF9100)
                            else -> BearishRed
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Risk Profile Assessment", color = TextGrey, fontSize = 10.sp)
                            Text(safetyVerdict, color = verdictColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsCenterDialog(
    notifications: List<com.example.viewmodel.AppNotification>,
    onDismiss: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onClearAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicCard),
            border = BorderStroke(1.dp, BorderSlate),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Notifications Center",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Dialog",
                            tint = TextGrey,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions Bar
                if (notifications.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onMarkAllAsRead,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Mark all as read",
                                color = ElectricBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = onClearAll,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Clear all",
                                color = BearishRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Scrollable Notifications List
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = TextGrey.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No notifications yet",
                                color = TextGrey,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Price alerts and AI generation signals will appear here.",
                                color = TextGrey.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notifications) { item ->
                            val itemBg = if (item.isRead) CosmicCard else BorderSlate.copy(alpha = 0.25f)
                            val itemBorder = if (item.isRead) BorderSlate.copy(alpha = 0.4f) else NeonCyan.copy(alpha = 0.5f)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(itemBg)
                                    .border(1.dp, itemBorder, RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Notification Type Icon
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when (item.type) {
                                                "PRICE_ALERT" -> BearishRed.copy(alpha = 0.15f)
                                                "AI_SIGNAL" -> NeonCyan.copy(alpha = 0.15f)
                                                else -> ElectricBlue.copy(alpha = 0.15f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (item.type) {
                                            "PRICE_ALERT" -> Icons.Default.TrendingUp
                                            "AI_SIGNAL" -> Icons.Default.Lightbulb
                                            else -> Icons.Default.Info
                                        },
                                        contentDescription = null,
                                        tint = when (item.type) {
                                            "PRICE_ALERT" -> BearishRed
                                            "AI_SIGNAL" -> NeonCyan
                                            else -> ElectricBlue
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Text Content
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.message,
                                        color = TextWhite.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = android.text.format.DateUtils.getRelativeTimeSpanString(
                                            item.timestamp,
                                            System.currentTimeMillis(),
                                            android.text.format.DateUtils.MINUTE_IN_MILLIS
                                        ).toString(),
                                        color = TextGrey,
                                        fontSize = 9.sp
                                    )
                                }

                                // Delete Button
                                IconButton(
                                    onClick = { onDeleteNotification(item.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Notification",
                                        tint = TextGrey.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
