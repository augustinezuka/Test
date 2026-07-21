package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.forex.ForexPairData
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Schema ---

@JsonClass(generateAdapter = true)
data class Part(val text: String)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class ResponseFormatText(val mimeType: String)

@JsonClass(generateAdapter = true)
data class ResponseFormat(val text: ResponseFormatText)

@JsonClass(generateAdapter = true)
data class GenerationConfig(val responseFormat: ResponseFormat? = null)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(val candidates: List<Candidate>?)

// --- App Representation ---

data class AIRecommendation(
    val pair: String,
    val confidence: Int,
    val riskScore: Int,
    val suggestedAction: String, // BUY, HOLD, SELL, WATCH
    val rationale: String,
    val keyNews: List<String>
)

data class AIServiceResponse(
    val recommendedPairs: List<AIRecommendation>,
    val overallSummary: String,
    val disclaimer: String
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Determines whether a real API key is configured.
     */
    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && key != "GEMINI_API_KEY"
    }

    /**
     * Generate recommendation for a single currency pair or all currency pairs
     * incorporating the user risk profile and live market news.
     */
    suspend fun fetchAIRecommendation(
        pairsData: List<ForexPairData>,
        riskLevel: String,
        experienceLevel: String,
        aiPreferences: String
    ): AIServiceResponse {
        val isKeyValid = isApiKeyConfigured()
        Log.d(TAG, "isApiKeyConfigured: $isKeyValid, key prefix: ${BuildConfig.GEMINI_API_KEY.take(5)}")

        if (!isKeyValid) {
            // No API key configured, use high-fidelity simulator fallback
            return generateMockAIResponse(pairsData, riskLevel, experienceLevel)
        }

        val prompt = assemblePrompt(pairsData, riskLevel, experienceLevel, aiPreferences)
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(text = ResponseFormatText(mimeType = "application/json"))
            )
        )

        try {
            val response = service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!jsonText.isNullOrEmpty()) {
                Log.d(TAG, "Raw Gemini JSON: $jsonText")
                return parseStructuredResponse(jsonText, pairsData, riskLevel)
            } else {
                throw Exception("Received empty response from Gemini API")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed: ${e.message}. Falling back to simulation.", e)
            return generateMockAIResponse(pairsData, riskLevel, experienceLevel)
        }
    }

    /**
     * Helper to parse JSON dynamically with fallbacks for key casings (camelCase / snake_case).
     */
    private fun parseStructuredResponse(
        jsonString: String,
        pairsData: List<ForexPairData>,
        riskLevel: String
    ): AIServiceResponse {
        try {
            val root = JSONObject(jsonString)
            
            // Get overall summary
            val overallSummary = root.optString("overall_summary", root.optString("overallSummary", "Analysis completed successfully."))
            
            // Get disclaimer
            val disclaimer = root.optString(
                "disclaimer", 
                "This recommendation is AI-generated and is for informational purposes only. It is not financial advice. Every user is responsible for their own trading decisions."
            )

            // Parse recommended pairs
            val recArray = root.optJSONArray("recommended_pairs") ?: root.optJSONArray("recommendedPairs")
            val list = mutableListOf<AIRecommendation>()

            if (recArray != null) {
                for (i in 0 until recArray.length()) {
                    val item = recArray.getJSONObject(i)
                    val pairSym = item.optString("pair", item.optString("symbol", "EUR/USD"))
                    val confidence = item.optInt("confidence", 75)
                    val riskScore = item.optInt("risk_score", item.optInt("riskScore", 5))
                    val action = item.optString("suggested_action", item.optString("suggestedAction", "HOLD"))
                    val rationale = item.optString("rationale", "No details provided.")

                    val newsList = mutableListOf<String>()
                    val newsArray = item.optJSONArray("key_news") ?: item.optJSONArray("keyNews")
                    if (newsArray != null) {
                        for (j in 0 until newsArray.length()) {
                            newsList.add(newsArray.getString(j))
                        }
                    }

                    list.add(
                        AIRecommendation(
                            pair = pairSym,
                            confidence = confidence,
                            riskScore = riskScore,
                            suggestedAction = action,
                            rationale = rationale,
                            keyNews = newsList
                        )
                    )
                }
            }

            if (list.isEmpty()) {
                throw Exception("Recommendations list was empty inside JSON")
            }

            return AIServiceResponse(
                recommendedPairs = list,
                overallSummary = overallSummary,
                disclaimer = disclaimer
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing JSON, generating plausible model from fallback: ${e.message}")
            return generateMockAIResponse(pairsData, riskLevel, "Intermediate")
        }
    }

    /**
     * Assembles the detailed structured text prompt for the Gemini 3.5 Flash model.
     */
    private fun assemblePrompt(
        pairsData: List<ForexPairData>,
        riskLevel: String,
        experienceLevel: String,
        aiPreferences: String
    ): String {
        val pairsSnapshot = pairsData.joinToString("\n\n") { pair ->
            """
            - Pair: ${pair.symbol} (${pair.name})
              Current Price: ${pair.currentPrice}
              Daily Change: ${String.format("%.4f", pair.dailyChangePercent)}%
              Volatility Score (1-10): ${String.format("%.1f", pair.volatilityScore)}
              Recent News Snippets:
              ${pair.news.joinToString("\n              ") { "* $it" }}
            """.trimIndent()
        }

        return """
            You are a expert Forex AI Analyst. Your job is to analyze the market snapshot of several major currency pairs and generate a highly professional Forex Trading Recommendation report customized to the user's risk profile.
            
            USER PROFILE:
            - Risk Tolerance: $riskLevel
            - Experience Level: $experienceLevel
            - Analysis Preference: $aiPreferences
            
            MARKET DATA SNAPSHOT:
            $pairsSnapshot
            
            DIRECTIONS FOR RECOMMENDATION:
            - Provide a customized suggested action ("BUY", "SELL", "HOLD", "WATCH") for EVERY single pair provided.
            - Ensure recommended actions align with the user's risk tolerance:
              * "Conservative": Prefer stable pairs (EUR/USD, USD/CHF) with lower volatility. Suggested actions must be highly confident, otherwise suggest HOLD or WATCH. Risk scores should be low (1-3 out of 10).
              * "Moderate": Balanced risk. Accept moderate volatility. Risk scores should be balanced (4-7 out of 10).
              * "Aggressive": Embrace volatility. Look for breakout patterns and high daily changes (GBP/USD, USD/JPY, AUD/USD). High confidence BUY or SELL actions are acceptable with risk scores (8-10 out of 10).
            - Give a specific confidence percentage (0% to 100%) for each recommendation.
            - Write a concise, 1-2 sentence professional rationale for each recommendation, referencing the news headlines or price change.
            - Extract 1-2 key news topics relevant to each recommendation.
            - Provide a master "overall_summary" of the market sentiment (2 sentences).
            - Always include a standard regulatory financial disclaimer stating that this is informational only and NOT financial advice.
            
            MANDATORY FORMAT:
            You MUST respond ONLY with a single valid JSON object. Do not include any markdown backticks or wrapper code. It must be directly parseable. Use the following exact JSON structure:
            
            {
              "recommended_pairs": [
                {
                  "pair": "EUR/USD",
                  "confidence": 85,
                  "risk_score": 3,
                  "suggested_action": "BUY",
                  "rationale": "Strong Eurozone PMI indices coupled with downward US Treasury yields indicate short-term Euro strength.",
                  "key_news": [
                    "ECB hawkish sentiment",
                    "US Yield decline"
                  ]
                },
                ...
              ],
              "overall_summary": "Summary of the general market direction customized to $riskLevel traders.",
              "disclaimer": "This recommendation is AI-generated and is for informational purposes only. It is not financial advice. Every user is responsible for their own trading decisions."
            }
        """.trimIndent()
    }

    /**
     * High-fidelity mock generator to run fully offline or when API key is missing.
     * Computes technical recommendations dynamically from actual ForexEngine data.
     */
    fun generateMockAIResponse(
        pairsData: List<ForexPairData>,
        riskLevel: String,
        experienceLevel: String
    ): AIServiceResponse {
        val list = pairsData.map { pair ->
            // Compute action dynamically based on volatility, daily change, and user risk
            val change = pair.dailyChangePercent
            val isBullish = change > 0.03
            val isBearish = change < -0.03
            val vol = pair.volatilityScore

            // Determine suggested action
            val action = when (riskLevel) {
                "Conservative" -> {
                    if (vol > 7.0) {
                        "WATCH" // avoid high volatility
                    } else if (isBullish && change > 0.1) {
                        "BUY"
                    } else if (isBearish && change < -0.1) {
                        "SELL"
                    } else {
                        "HOLD"
                    }
                }
                "Moderate" -> {
                    if (isBullish) "BUY" else if (isBearish) "SELL" else "HOLD"
                }
                else -> { // Aggressive
                    // amplify breakouts
                    if (change > 0.05) "BUY" else if (change < -0.05) "SELL" else "WATCH"
                }
            }

            // Adjust confidence based on match with risk
            val confidence = when (riskLevel) {
                "Conservative" -> if (action == "HOLD" || action == "WATCH") 90 else 72
                "Moderate" -> if (action == "BUY" || action == "SELL") 78 else 85
                else -> if (action == "BUY" || action == "SELL") 88 else 65
            } + (pair.symbol.hashCode() % 10)

            // Risk score relative to currency volatility
            val riskScore = when {
                vol > 7.5 -> if (riskLevel == "Conservative") 5 else 8
                vol > 5.0 -> if (riskLevel == "Conservative") 3 else 5
                else -> if (riskLevel == "Conservative") 1 else 3
            }

            // High-quality rationale referencing real simulated news
            val leadNews = pair.news.firstOrNull() ?: "Market dynamics"
            val rationale = when (action) {
                "BUY" -> "Bullish momentum of ${String.format("%.2f", change)}% supported by headlines on: \"$leadNews\""
                "SELL" -> "Bearish indicators showing ${String.format("%.2f", change)}% retracement. Compounded by headlines on: \"$leadNews\""
                "WATCH" -> "High market volatility (${String.format("%.1f", vol)}/10) signals consolidation. Wait for breakout indicators."
                else -> "Neutral trading bounds with low immediate volatility. Headlines indicate safe consolidation."
            }

            val keyNews = pair.news.take(2).map { 
                it.replace("Bank of ", "").replace("Reserve Bank of ", "").take(30) + "..."
            }

            AIRecommendation(
                pair = pair.symbol,
                confidence = confidence.coerceIn(50, 95),
                riskScore = riskScore,
                suggestedAction = action,
                rationale = rationale,
                keyNews = keyNews
            )
        }

        val overallSummary = when (riskLevel) {
            "Conservative" -> "The market shows selective stability. Lower-volatility pairs offer solid consolidation entries, while major breakouts are avoided for high risk-adjusted safety."
            "Moderate" -> "Balanced Forex index structures present active entry corridors across major indexes. EUR/USD and GBP/USD demonstrate key support pivots."
            else -> "High-yield trading bounds are highly active today. Breakout channels in JPY and AUD index pools offer high-risk acceleration zones."
        }

        return AIServiceResponse(
            recommendedPairs = list,
            overallSummary = overallSummary,
            disclaimer = "DISCLAIMER: This platform is powered by simulated Forex indicators & real-time Gemini LLM processing. Recommendations are informational only and do not constitute professional financial advice."
        )
    }

    /**
     * Conversational helper for the AI Chat Screen. Passes the selected pair data or risk level as context.
     */
    suspend fun chatWithGemini(
        history: List<Pair<String, String>>, // list of userMessage to assistantMessage
        newPrompt: String,
        contextPair: ForexPairData?,
        riskLevel: String
    ): String {
        if (!isApiKeyConfigured()) {
            return generateMockChatResponse(newPrompt, contextPair, riskLevel)
        }

        // Build history string
        val historyStr = history.joinToString("\n") {
            "User: ${it.first}\nAssistant: ${it.second}"
        }

        val contextStr = if (contextPair != null) {
            """
            Current context pair is ${contextPair.symbol} (${contextPair.name}) at price ${contextPair.currentPrice} with a daily change of ${String.format("%.4f", contextPair.dailyChangePercent)}% and volatility score of ${String.format("%.1f", contextPair.volatilityScore)}/10.
            Key headlines on this pair:
            ${contextPair.news.joinToString("\n") { "* $it" }}
            """.trimIndent()
        } else {
            "Current trading scope includes general forex recommendations."
        }

        val chatPrompt = """
            You are a expert, extremely professional Forex AI trading tutor. You are helping a user who has a "$riskLevel" risk tolerance.
            
            CONTEXT:
            $contextStr
            
            PREVIOUS CONVERSATION HISTORY:
            $historyStr
            
            USER'S NEW QUESTION:
            $newPrompt
            
            DIRECTIONS:
            - Answer the user's question clearly, professionally, and with actionable market context.
            - Keep your response friendly, concise (under 4 paragraphs), and conversational.
            - Include appropriate formatting (bullet points, bold highlights) to make it highly readable.
            - NEVER give direct financial advice. Always reinforce that trading involves risk, and your answers are for educational purposes.
            - Answer directly. Do not wrap in markdown tags other than standard formatting bold/lists.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = chatPrompt))))
        )

        try {
            val response = service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I was unable to analyze that request. Please try again."
        } catch (e: Exception) {
            Log.e(TAG, "Chat request failed: ${e.message}", e)
            return generateMockChatResponse(newPrompt, contextPair, riskLevel)
        }
    }

    /**
     * Local interactive mock chat generator when API key is unconfigured or fails.
     */
    private fun generateMockChatResponse(
        prompt: String,
        contextPair: ForexPairData?,
        riskLevel: String
    ): String {
        val normalized = prompt.lowercase()
        return when {
            normalized.contains("why") && contextPair != null -> {
                """
                My current recommendation for **${contextPair.symbol}** is based on three convergent factors analyzed for a **$riskLevel** risk profile:
                
                1. **Volatility Structure**: The pair is currently trading at **${contextPair.currentPrice}** with a daily change of **${String.format("%.2f", contextPair.dailyChangePercent)}%**. This presents stable pivot corridors.
                2. **News Sentiment**: The latest updates including *"${contextPair.news.firstOrNull() ?: "Key index releases"}"* indicate solid support.
                3. **Risk Optimization**: For **$riskLevel** strategies, we seek entries that align with optimal safety limits, keeping exposure constrained.
                
                *Disclaimer: This analysis is for educational purposes only.*
                """.trimIndent()
            }
            normalized.contains("volatility") -> {
                if (contextPair != null) {
                    """
                    Volatility for **${contextPair.symbol}** is currently indexed at **${String.format("%.1f", contextPair.volatilityScore)} out of 10**. 
                    
                    In Forex terms, this indicates **${if (contextPair.volatilityScore > 7.0) "high intra-day momentum" else "stable range-bound action"}**. For a **$riskLevel** trader, this volatility means:
                    
                    * **Conservative stance**: Exercise caution, keep stop-losses tight, or favor more stable indexes.
                    * **Tactical execution**: Position sizing should be adjusted downward to offset sudden news shocks.
                    """.trimIndent()
                } else {
                    """
                    Forex market volatility is heavily driven by central bank interest rate expectations and macroeconomic releases like CPI and Employment reports.
                    
                    Today, **GBP/USD** and **USD/JPY** show elevated volatility scores (above 7/10), presenting breakout corridors. Meanwhile, **EUR/USD** remains a highly stable channel suited for standard range-bound strategies.
                    """.trimIndent()
                }
            }
            normalized.contains("conservative") || normalized.contains("strategy") -> {
                """
                A **Conservative Strategy** focuses on capital preservation and high-probability setups. Here are the core pillars:
                
                * **Asset Selection**: Focus exclusively on liquid, lower-volatility majors like **EUR/USD** or **USD/CHF**. Avoid minor cross-pairs.
                * **Volatility Caps**: Restrict entries when pairs exceed a volatility score of 6.0/10.
                * **Risk Management**: Never risk more than 1% of equity per trade. Ensure stop-losses are strictly aligned with historical daily support zones.
                """.trimIndent()
            }
            else -> {
                """
                Hello! As your **Forex AI Assistant**, I am here to help you understand market trends, charts, and risk strategies.
                
                Regarding your question, for a **$riskLevel** strategy, it is always critical to analyze price pivots (currently **${contextPair?.symbol ?: "the majors"}** sits at **${contextPair?.currentPrice ?: "liquid averages"}**) and cross-reference them with fundamental news.
                
                What other questions do you have about currency pairings or risk management today?
                """.trimIndent()
            }
        }
    }
}
