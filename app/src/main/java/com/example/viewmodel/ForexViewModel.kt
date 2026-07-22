package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserProfile
import com.example.data.api.AIServiceResponse
import com.example.data.api.GeminiClient
import com.example.data.forex.ForexEngine
import com.example.data.forex.ForexPairData
import com.example.data.forex.ForexRepository
import com.example.data.forex.LiveForexWebSocketService
import com.example.data.logger.AppLogger
import com.example.data.logger.LogCategory
import com.example.data.logger.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface MarketUiState {
    object Loading : MarketUiState
    data class Success(val pairs: List<ForexPairData>) : MarketUiState
    data class Error(val message: String) : MarketUiState
}

sealed interface RecommendationUiState {
    object Idle : RecommendationUiState
    object Loading : RecommendationUiState
    data class Success(val report: AIServiceResponse) : RecommendationUiState
    data class Error(val message: String) : RecommendationUiState
}

data class PriceAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val symbol: String,
    val targetPrice: Double,
    val isAbove: Boolean,
    val isEnabled: Boolean = true,
    val isTriggered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class AppNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: String, // "PRICE_ALERT", "AI_SIGNAL", "SYSTEM"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

class ForexViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ForexViewModel"
    private val repository = ForexRepository(application)

    // Real-time custom price alerts
    private val _priceAlerts = MutableStateFlow<List<PriceAlert>>(
        listOf(
            PriceAlert(symbol = "EUR/USD", targetPrice = 1.0920, isAbove = true),
            PriceAlert(symbol = "GBP/USD", targetPrice = 1.2820, isAbove = false)
        )
    )
    val priceAlerts: StateFlow<List<PriceAlert>> = _priceAlerts.asStateFlow()

    // Real-time in-app and system notifications history
    private val _notifications = MutableStateFlow<List<AppNotification>>(
        listOf(
            AppNotification(
                title = "Welcome to Forex AI Pro!",
                message = "Your high-fidelity real-time currency trading companion is active. Set custom price alerts in Settings.",
                type = "SYSTEM"
            )
        )
    )
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    // Raw market pairs data
    private val initialPairsData = LiveForexWebSocketService.livePairsFlow.value.ifEmpty { ForexEngine.getAllPairsData() }
    private val _pairsFlow = MutableStateFlow<List<ForexPairData>>(initialPairsData)
    val pairsFlow: StateFlow<List<ForexPairData>> = _pairsFlow.asStateFlow()

    // Screen-level market UI state
    private val _marketUiState = MutableStateFlow<MarketUiState>(
        if (initialPairsData.isNotEmpty()) MarketUiState.Success(initialPairsData) else MarketUiState.Loading
    )
    val marketUiState: StateFlow<MarketUiState> = _marketUiState.asStateFlow()

    // Active AI Recommendation Report State
    private val _recommendationUiState = MutableStateFlow<RecommendationUiState>(RecommendationUiState.Idle)
    val recommendationUiState: StateFlow<RecommendationUiState> = _recommendationUiState.asStateFlow()

    // User profile state
    val userProfile: StateFlow<UserProfile> = repository.userProfileFlow
        .map { it ?: repository.getOrCreateProfile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    // Watchlist state
    val watchlistSymbols: StateFlow<Set<String>> = repository.watchlistFlow
        .map { items -> items.map { it.symbol }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    // AI Chat History: List of userMsg to assistantMsg
    private val _chatHistory = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val chatHistory: StateFlow<List<Pair<String, String>>> = _chatHistory.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    // API Key status flow (for warning banner)
    val isRealApiKeyConfigured = flow {
        emit(GeminiClient.isApiKeyConfigured())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isWsConnected: StateFlow<Boolean> = LiveForexWebSocketService.isConnected

    init {
        // Start live WebSocket stream for actual market quotes
        LiveForexWebSocketService.start()
        observeLiveWebSocketStream()
        
        // Ensure default profile exists
        viewModelScope.launch {
            repository.getOrCreateProfile()
        }
    }

    /**
     * Observes real-time WebSocket ticks coming from LiveForexWebSocketService.
     */
    private fun observeLiveWebSocketStream() {
        viewModelScope.launch {
            LiveForexWebSocketService.livePairsFlow.collect { freshData ->
                if (freshData.isNotEmpty()) {
                    _pairsFlow.value = freshData
                    _marketUiState.value = MarketUiState.Success(freshData)
                    checkPriceAlerts(freshData)
                }
            }
        }
    }

    private fun checkPriceAlerts(freshData: List<ForexPairData>) {
        val currentAlerts = _priceAlerts.value
        var alertsUpdated = false
        val updatedAlerts = currentAlerts.map { alert ->
            if (alert.isEnabled && !alert.isTriggered) {
                val matchingPair = freshData.find { it.symbol == alert.symbol }
                if (matchingPair != null) {
                    val curPrice = matchingPair.currentPrice
                    val hasCrossed = if (alert.isAbove) {
                        curPrice >= alert.targetPrice
                    } else {
                        curPrice <= alert.targetPrice
                    }
                    if (hasCrossed) {
                        alertsUpdated = true
                        val direction = if (alert.isAbove) "surpassed" else "dropped below"
                        val trendStr = if (curPrice > alert.targetPrice) "Bullish expansion 📈" else "Corrective pullback 📉"
                        val dailyChange = matchingPair.dailyChangePercent
                        val volatility = matchingPair.volatilityScore
                        val leadNews = matchingPair.news.firstOrNull()?.replace(".", "") ?: "technical pivot breakout"
                        
                        val title = "AI Alert: ${alert.symbol} Key Pivot Reached! 🤖✨"
                        val message = "${alert.symbol} has $direction your trigger price of ${String.format("%.4f", alert.targetPrice)} (Current: ${String.format("%.4f", curPrice)}). Today's change is ${String.format("%.2f", dailyChange)}% ($trendStr) with volatility at ${String.format("%.1f", volatility)}/10. Catalyst: '$leadNews'. Monitor closely for breakout entries."
                        
                        triggerNotification(
                            title = title,
                            message = message,
                            type = "PRICE_ALERT"
                        )
                        alert.copy(isEnabled = false, isTriggered = true)
                    } else {
                        alert
                    }
                } else {
                    alert
                }
            } else {
                alert
            }
        }
        if (alertsUpdated) {
            _priceAlerts.value = updatedAlerts
        }
    }

    fun refreshMarketData() {
        viewModelScope.launch {
            _marketUiState.value = MarketUiState.Loading
            LiveForexWebSocketService.triggerManualRefresh()
            delay(600)
            val freshData = LiveForexWebSocketService.livePairsFlow.value.ifEmpty { ForexEngine.getAllPairsData() }
            _pairsFlow.value = freshData
            _marketUiState.value = MarketUiState.Success(freshData)
            AppLogger.logInfo(LogCategory.SYSTEM, "Refresh", "Market quotes manually refreshed")
        }
    }

    /**
     * Triggers the structured Gemini recommendation report for all major indices.
     */
    fun generateAIRecommendations() {
        viewModelScope.launch {
            _recommendationUiState.value = RecommendationUiState.Loading
            try {
                // Read current pairs and profile
                val activePairs = _pairsFlow.value.ifEmpty { ForexEngine.getAllPairsData() }
                val profile = repository.getOrCreateProfile()
                
                val response = repository.generateRecommendations(activePairs, profile)
                _recommendationUiState.value = RecommendationUiState.Success(response)

                // Trigger Smart AI prediction notification
                val topPick = response.recommendedPairs.maxByOrNull { it.confidence }
                val title = "AI Market Intelligence Signal! 🤖📊"
                val message = if (topPick != null) {
                    "Top pick: ${topPick.pair} (${topPick.suggestedAction}) at ${topPick.confidence}% confidence. Catalyst: '${topPick.rationale}'. Global Outlook: ${response.overallSummary}"
                } else {
                    "Forex AI successfully generated new recommendation signals: ${response.overallSummary}"
                }

                triggerNotification(
                    title = title,
                    message = message,
                    type = "AI_SIGNAL"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed generating recommendations", e)
                _recommendationUiState.value = RecommendationUiState.Error("AI generation failed. Please verify internet connection.")
            }
        }
    }

    // --- Profile & Risk Profile Settings ---

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(themeMode = mode))
        }
    }

    fun updatePrimaryColor(hex: String) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(primaryColorHex = hex))
        }
    }

    fun updateExternalApiSettings(apiKey: String, apiUrl: String) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(
                externalApiKey = apiKey,
                externalApiUrl = apiUrl
            ))
        }
    }

    fun updateRiskProfile(
        riskLevel: String,
        experienceLevel: String,
        preferredPairs: String,
        aiPreferences: String
    ) {
        viewModelScope.launch {
            val current = userProfile.value
            val updated = current.copy(
                riskLevel = riskLevel,
                experience = experienceLevel,
                preferredPairs = preferredPairs,
                aiPreferences = aiPreferences
            )
            repository.updateProfile(updated)
            // Re-trigger recommendations to align with new risk settings immediately
            generateAIRecommendations()
        }
    }

    // Real-time system logs flow
    val systemLogs: StateFlow<List<LogEntry>> = AppLogger.logsFlow

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(notificationsEnabled = enabled))
            AppLogger.logInfo(LogCategory.SYSTEM, "Settings", "Local notifications set to: $enabled")
        }
    }

    fun toggleHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(hapticFeedbackEnabled = enabled))
            AppLogger.logInfo(LogCategory.SYSTEM, "Settings", "Haptic feedback set to: $enabled")
        }
    }

    fun toggleAudioAlerts(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(audioAlertsEnabled = enabled))
            AppLogger.logInfo(LogCategory.SYSTEM, "Settings", "Audio alert chimes set to: $enabled")
        }
    }

    fun toggleDataSaver(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(dataSaverEnabled = enabled))
            AppLogger.logInfo(LogCategory.SYSTEM, "Settings", "Data Saver mode set to: $enabled")
        }
    }

    fun updateAutoRefreshRate(rateSec: Int) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(autoRefreshRateSec = rateSec))
            AppLogger.logInfo(LogCategory.SYSTEM, "Settings", "Auto refresh rate changed to: ${rateSec}s")
        }
    }

    fun clearSystemLogs() {
        AppLogger.clearLogs()
    }

    fun simulateApiFailureLog() {
        AppLogger.simulateApiFailure()
    }

    fun getExportableLogs(): String {
        return AppLogger.exportLogsAsString()
    }

    fun runSystemDiagnostics() {
        viewModelScope.launch(Dispatchers.IO) {
            AppLogger.logInfo(LogCategory.SYSTEM, "DiagnosticTest", "Initiating System & API Diagnostics Ping Sequence...")
            
            // Ping Binance REST
            val startRest = System.currentTimeMillis()
            try {
                val client = okhttp3.OkHttpClient.Builder().connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS).build()
                val req = okhttp3.Request.Builder().url("https://api.binance.com/api/v3/ping").build()
                val resp = client.newCall(req).execute()
                val elapsed = System.currentTimeMillis() - startRest
                if (resp.isSuccessful) {
                    AppLogger.logSuccess(LogCategory.API_REST, "Diag_Binance", "Binance API Ping OK", "Response 200 OK", 200, elapsed)
                } else {
                    AppLogger.logError(LogCategory.API_REST, "Diag_Binance", "Binance API Ping Failed HTTP ${resp.code}", "Endpoint returned status ${resp.code}", resp.code, elapsed)
                }
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - startRest
                AppLogger.logError(LogCategory.API_REST, "Diag_Binance", "Binance API Ping Exception", e.localizedMessage, 500, elapsed)
            }

            // Ping WebSocket status
            val isWs = isWsConnected.value
            if (isWs) {
                AppLogger.logSuccess(LogCategory.WEBSOCKET, "Diag_WebSocket", "WebSocket Stream Active", "Stream wss://stream.binance.com:9443 is healthy")
            } else {
                AppLogger.logWarn(LogCategory.WEBSOCKET, "Diag_WebSocket", "WebSocket Disconnected", "Stream offline, using REST fallback pollers")
            }

            // Ping Gemini Key configuration
            if (isRealApiKeyConfigured.value) {
                AppLogger.logSuccess(LogCategory.GEMINI_AI, "Diag_GeminiKey", "Gemini AI Key Valid", "Pro API credentials detected in BuildConfig")
            } else {
                AppLogger.logInfo(LogCategory.GEMINI_AI, "Diag_GeminiKey", "Gemini Heuristic Engine Active", "Using built-in client prediction model")
            }
        }
    }

    // --- Watchlist Interactions ---

    fun toggleWatchlist(symbol: String) {
        viewModelScope.launch {
            val isPresent = watchlistSymbols.value.contains(symbol)
            if (isPresent) {
                repository.removeFromWatchlist(symbol)
            } else {
                repository.addToWatchlist(symbol)
            }
        }
    }

    // --- AI Chat Interactions ---

    fun sendChatMessage(messageText: String, activePairSymbol: String? = null) {
        if (messageText.isBlank()) return
        
        viewModelScope.launch {
            // Append user message immediately
            val updatedHistory = _chatHistory.value.toMutableList()
            updatedHistory.add(messageText to "")
            _chatHistory.value = updatedHistory
            _chatLoading.value = true

            try {
                val contextPair = activePairSymbol?.let { ForexEngine.getPairData(it) }
                val profile = userProfile.value

                // Send to repository
                val reply = repository.sendChatMessage(
                    chatHistory = updatedHistory.dropLast(1), // history before current turn
                    newMessage = messageText,
                    contextPair = contextPair,
                    riskLevel = profile.riskLevel
                )

                // Update the last assistant response
                val finalHistory = _chatHistory.value.toMutableList()
                if (finalHistory.isNotEmpty() && finalHistory.last().first == messageText) {
                    finalHistory[finalHistory.lastIndex] = messageText to reply
                }
                _chatHistory.value = finalHistory
            } catch (e: Exception) {
                Log.e(TAG, "Chat delivery failed", e)
                val finalHistory = _chatHistory.value.toMutableList()
                if (finalHistory.isNotEmpty() && finalHistory.last().first == messageText) {
                    finalHistory[finalHistory.lastIndex] = messageText to "I experienced an issue communicating with the AI. Please verify your credentials and try again."
                }
                _chatHistory.value = finalHistory
            } finally {
                _chatLoading.value = false
            }
        }
    }

    fun clearChat() {
        _chatHistory.value = emptyList()
    }

    // --- Price Alert CRUD Functions ---

    fun addPriceAlert(symbol: String, targetPrice: Double, isAbove: Boolean) {
        val newAlert = PriceAlert(symbol = symbol, targetPrice = targetPrice, isAbove = isAbove)
        _priceAlerts.value = _priceAlerts.value + newAlert
    }

    fun toggleAlertEnabled(id: String) {
        _priceAlerts.value = _priceAlerts.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled, isTriggered = false) else it
        }
    }

    fun deletePriceAlert(id: String) {
        _priceAlerts.value = _priceAlerts.value.filter { it.id != id }
    }

    // --- In-App & System Notification Control APIs ---

    fun triggerNotification(title: String, message: String, type: String) {
        val newNotification = AppNotification(title = title, message = message, type = type)
        _notifications.value = listOf(newNotification) + _notifications.value

        // Post a real system notification if enabled in profile settings
        if (userProfile.value.notificationsEnabled) {
            postSystemNotification(title, message)
        }
    }

    private fun postSystemNotification(title: String, message: String) {
        val context = getApplication<Application>()
        val channelId = "forex_alerts_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Forex Price & AI Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for custom price limits and AI generated recommendations"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed posting system notification: ${e.message}")
        }
    }

    fun markAllNotificationsAsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun deleteNotification(id: String) {
        _notifications.value = _notifications.value.filter { it.id != id }
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }
}
