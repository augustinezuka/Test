package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserProfile
import com.example.data.api.AIServiceResponse
import com.example.data.api.GeminiClient
import com.example.data.forex.ForexEngine
import com.example.data.forex.ForexPairData
import com.example.data.forex.ForexRepository
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

    // Raw market pairs data
    private val _pairsFlow = MutableStateFlow<List<ForexPairData>>(emptyList())
    val pairsFlow: StateFlow<List<ForexPairData>> = _pairsFlow.asStateFlow()

    // Screen-level market UI state
    private val _marketUiState = MutableStateFlow<MarketUiState>(MarketUiState.Loading)
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

    init {
        // Start automatic price ticker simulation
        startPriceTicker()
        // Ensure default profile exists
        viewModelScope.launch {
            repository.getOrCreateProfile()
        }
    }

    /**
     * Periodically updates market prices to simulate live exchange activity (every 10 seconds).
     */
    private fun startPriceTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                try {
                    val freshData = ForexEngine.getAllPairsData()
                    _pairsFlow.value = freshData
                    if (_marketUiState.value is MarketUiState.Loading || _marketUiState.value is MarketUiState.Success) {
                        _marketUiState.value = MarketUiState.Success(freshData)
                    }
                    
                    // Reactive Price Alert Trigger Checks
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
                } catch (e: Exception) {
                    Log.e(TAG, "Ticker failed to refresh", e)
                    if (_pairsFlow.value.isEmpty()) {
                        _marketUiState.value = MarketUiState.Error("Failed to fetch live currency rates.")
                    }
                }
                delay(10000) // 10 seconds interval
            }
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

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.updateProfile(current.copy(notificationsEnabled = enabled))
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
}
