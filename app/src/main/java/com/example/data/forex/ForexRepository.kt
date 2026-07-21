package com.example.data.forex

import android.content.Context
import android.util.Log
import com.example.data.*
import com.example.data.api.AIServiceResponse
import com.example.data.api.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ForexRepository(private val context: Context) {
    private val TAG = "ForexRepository"
    private val database = AppDatabase.getDatabase(context)
    private val userProfileDao = database.userProfileDao()
    private val watchlistDao = database.watchlistDao()
    private val recommendationDao = database.recommendationDao()

    // Flow streams
    val userProfileFlow: Flow<UserProfile?> = userProfileDao.getUserProfileFlow()
    val watchlistFlow: Flow<List<WatchlistItem>> = watchlistDao.getWatchlistFlow()
    val recommendationHistoryFlow: Flow<List<RecommendationEntity>> = recommendationDao.getAllRecommendationHistoryFlow()
    val logsFlow: Flow<List<RecommendationLogEntity>> = recommendationDao.getLogsFlow()

    init {
        // We will seed the default profile when repository initializes
    }

    suspend fun getOrCreateProfile(): UserProfile = withContext(Dispatchers.IO) {
        val existing = userProfileDao.getUserProfile()
        if (existing != null) {
            existing
        } else {
            val defaultProfile = UserProfile()
            userProfileDao.insertOrUpdate(defaultProfile)
            defaultProfile
        }
    }

    suspend fun updateProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        userProfileDao.insertOrUpdate(profile)
    }

    // Watchlist functions
    suspend fun addToWatchlist(symbol: String) = withContext(Dispatchers.IO) {
        watchlistDao.insert(WatchlistItem(symbol = symbol))
    }

    suspend fun removeFromWatchlist(symbol: String) = withContext(Dispatchers.IO) {
        watchlistDao.delete(symbol)
    }

    suspend fun isWatchlisted(symbol: String): Boolean = withContext(Dispatchers.IO) {
        watchlistDao.isWatchlisted(symbol)
    }

    /**
     * Triggers a live market recommendation generation via Gemini (or the high-fidelity simulator fallback),
     * and persists the resulting parsed JSON model in local history and logs.
     */
    suspend fun generateRecommendations(
        pairsData: List<ForexPairData>,
        userProfile: UserProfile
    ): AIServiceResponse = withContext(Dispatchers.IO) {
        Log.d(TAG, "Generating recommendation report for risk level: ${userProfile.riskLevel}")

        val result = GeminiClient.fetchAIRecommendation(
            pairsData = pairsData,
            riskLevel = userProfile.riskLevel,
            experienceLevel = userProfile.experience,
            aiPreferences = userProfile.aiPreferences
        )

        // Save each recommendation item into Room local history
        result.recommendedPairs.forEach { rec ->
            recommendationDao.insertRecommendation(
                RecommendationEntity(
                    symbol = rec.pair,
                    confidence = rec.confidence,
                    suggestedAction = rec.suggestedAction,
                    rationale = rec.rationale,
                    riskScore = rec.riskScore,
                    keyNews = rec.keyNews.joinToString(",")
                )
            )
        }

        // Save execution details into Recommendation Logs
        recommendationDao.insertLog(
            RecommendationLogEntity(
                symbol = pairsData.joinToString(",") { it.symbol },
                prompt = "Assembled payload representing ${pairsData.size} currency indices and user risk level ${userProfile.riskLevel}.",
                response = result.overallSummary,
                marketSnapshot = pairsData.joinToString("|") { "${it.symbol}:${it.currentPrice}" },
                newsSnapshot = pairsData.flatMap { it.news }.take(5).joinToString(";")
            )
        )

        result
    }

    /**
     * Specific helper to chat with Gemini on a selected currency pair context.
     */
    suspend fun sendChatMessage(
        chatHistory: List<Pair<String, String>>,
        newMessage: String,
        contextPair: ForexPairData?,
        riskLevel: String
    ): String {
        return GeminiClient.chatWithGemini(
            history = chatHistory,
            newPrompt = newMessage,
            contextPair = contextPair,
            riskLevel = riskLevel
        )
    }
}
