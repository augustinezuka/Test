package com.example.data.forex

import android.util.Log
import com.example.data.logger.AppLogger
import com.example.data.logger.LogCategory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object LiveForexWebSocketService {
    private const val TAG = "LiveForexWS"

    private const val BINANCE_COM_WS_URL = "wss://stream.binance.com:9443/ws/!miniTicker@arr"
    private const val BINANCE_US_WS_URL = "wss://stream.binance.us:9443/ws/!miniTicker@arr"

    private const val BINANCE_COM_REST_URL = "https://api.binance.com/api/v3/ticker/24hr"
    private const val BINANCE_US_REST_URL = "https://api.binance.us/api/v3/ticker/24hr"
    private const val FALLBACK_EXCHANGE_API = "https://open.er-api.com/v6/latest/USD"

    private var currentWsUrlIndex = 0
    private val wsCandidateUrls = listOf(BINANCE_US_WS_URL, BINANCE_COM_WS_URL)
    private val restCandidateUrls = listOf(BINANCE_US_REST_URL, BINANCE_COM_REST_URL, FALLBACK_EXCHANGE_API)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _livePairsFlow = MutableStateFlow<List<ForexPairData>>(emptyList())
    val livePairsFlow: StateFlow<List<ForexPairData>> = _livePairsFlow.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var isStarted = false

    // Map of forex symbol to pair data
    private val pairDataMap = ConcurrentHashMap<String, ForexPairData>()

    init {
        seedInitialData()
    }

    private fun seedInitialData() {
        ForexEngine.pairsList.forEach { (symbol, _) ->
            if (!pairDataMap.containsKey(symbol)) {
                val basePrice = when (symbol) {
                    "EUR/USD" -> 1.0850
                    "GBP/USD" -> 1.2680
                    "USD/JPY" -> 155.40
                    "AUD/USD" -> 0.6620
                    "USD/CAD" -> 1.3650
                    "NZD/USD" -> 0.6110
                    "USD/CHF" -> 0.9020
                    else -> 1.0000
                }
                pairDataMap[symbol] = ForexPairData(
                    symbol = symbol,
                    name = ForexEngine.getPairName(symbol),
                    currentPrice = basePrice,
                    dailyChangePercent = 0.18,
                    volatilityScore = 5.5,
                    history = generateLiveHistory(symbol, basePrice),
                    news = listOf(
                        "Market liquidity active across global $symbol currency channels.",
                        "Trader sentiment monitors momentum pivots near key technical support lines."
                    )
                )
            }
        }
        publishPairs()
    }

    // Binance symbol mapping to Forex Symbol
    private val binanceSymbolToForexMap = mapOf(
        "EURUSDT" to "EUR/USD",
        "GBPUSDT" to "GBP/USD",
        "USDTJPY" to "USD/JPY",
        "AUDUSDT" to "AUD/USD",
        "USDCAD" to "USD/CAD",
        "USDTCHF" to "USD/CHF",
        "NZDUSDT" to "NZD/USD",
        "USDJPY" to "USD/JPY",
        "USDCHF" to "USD/CHF"
    )

    fun start() {
        if (isStarted) return
        isStarted = true
        Log.d(TAG, "Starting Live Forex WebSocket Service...")

        scope.launch {
            // 1. Initial live REST fetch
            fetchInitialLiveRestData()
            
            // 2. Connect WebSocket
            connectWebSocket()
            
            // 3. Periodic fallback poll every 12 seconds if WS is disconnected
            while (isActive) {
                delay(12000)
                if (!_isConnected.value) {
                    fetchInitialLiveRestData()
                }
            }
        }
    }

    fun triggerManualRefresh() {
        scope.launch {
            fetchInitialLiveRestData()
        }
    }

    private suspend fun fetchInitialLiveRestData() {
        for (url in restCandidateUrls) {
            val startTime = System.currentTimeMillis()
            try {
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                val elapsed = System.currentTimeMillis() - startTime

                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrEmpty()) {
                        if (url.contains("binance")) {
                            parseBinance24hrRest(bodyStr)
                        } else {
                            parseFallbackExchangeApi(bodyStr)
                        }
                        AppLogger.logSuccess(
                            category = LogCategory.API_REST,
                            tag = "REST_Feed_Sync",
                            message = "24hr Ticker quotes loaded successfully",
                            details = "Endpoint: $url | Size: ${bodyStr.length} bytes",
                            statusCode = response.code,
                            latencyMs = elapsed
                        )
                        return
                    }
                } else {
                    val is451 = response.code == 451
                    AppLogger.logError(
                        category = LogCategory.API_REST,
                        tag = if (is451) "REST_HTTP_451_GeoBlock" else "REST_Feed_Err",
                        message = if (is451) "HTTP 451 Legal/Geo-Block: $url is restricted in cloud server region." else "REST HTTP ${response.code} ${response.message}",
                        errorDetails = "Failed request to $url. Switched to fallback endpoint.",
                        statusCode = response.code,
                        latencyMs = elapsed
                    )
                }
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - startTime
                Log.w(TAG, "REST fetch failed for $url: ${e.message}")
                AppLogger.logError(
                    category = LogCategory.API_REST,
                    tag = "REST_Exception",
                    message = "REST request exception for $url: ${e.localizedMessage}",
                    errorDetails = e.stackTraceToString().take(250),
                    latencyMs = elapsed
                )
            }
        }
    }

    private fun connectWebSocket() {
        val targetWsUrl = wsCandidateUrls[currentWsUrlIndex]
        try {
            val request = Request.Builder().url(targetWsUrl).build()
            AppLogger.logInfo(
                category = LogCategory.WEBSOCKET,
                tag = "WS_Init",
                message = "Initiating WebSocket connection...",
                details = "Endpoint [$currentWsUrlIndex]: $targetWsUrl"
            )
            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connected successfully to $targetWsUrl")
                    _isConnected.value = true
                    AppLogger.logSuccess(
                        category = LogCategory.WEBSOCKET,
                        tag = "WS_Connected",
                        message = "Live exchange stream connected and active!",
                        details = "Endpoint: $targetWsUrl",
                        statusCode = response.code
                    )
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        parseWebSocketMessage(text)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing WebSocket message", e)
                        AppLogger.logWarn(
                            category = LogCategory.WEBSOCKET,
                            tag = "WS_ParseErr",
                            message = "Failed to parse incoming WS ticker payload",
                            details = "Payload snippet: ${text.take(100)} | Error: ${e.localizedMessage}"
                        )
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    val responseCode = response?.code
                    Log.e(TAG, "WebSocket failure on $targetWsUrl: code=$responseCode, msg=${t.message}")
                    _isConnected.value = false

                    if (responseCode == 451) {
                        AppLogger.logError(
                            category = LogCategory.WEBSOCKET,
                            tag = "WS_451_GeoBlocked",
                            message = "HTTP 451 Geo-Restricted: $targetWsUrl blocks cloud IP ranges. Public APIs do not require API keys, but cloud IPs are location-restricted.",
                            errorDetails = "Switching to secondary candidate endpoint & REST ticker mode.",
                            statusCode = 451
                        )
                    } else {
                        AppLogger.logError(
                            category = LogCategory.WEBSOCKET,
                            tag = "WS_Failure",
                            message = "WebSocket transport error on $targetWsUrl: ${t.localizedMessage ?: "Connection reset"}",
                            errorDetails = "Response code: ${responseCode ?: "N/A"}",
                            statusCode = responseCode
                        )
                    }

                    // Cycle to next candidate WS URL and reconnect
                    currentWsUrlIndex = (currentWsUrlIndex + 1) % wsCandidateUrls.size
                    scope.launch {
                        delay(3000)
                        connectWebSocket()
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed: $reason. Reconnecting in 3s...")
                    _isConnected.value = false
                    AppLogger.logWarn(
                        category = LogCategory.WEBSOCKET,
                        tag = "WS_Closed",
                        message = "WebSocket connection closed by remote peer (Code $code)",
                        details = "Reason: $reason"
                    )
                    scope.launch {
                        delay(3000)
                        connectWebSocket()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating WebSocket connection to $targetWsUrl", e)
            AppLogger.logError(
                category = LogCategory.WEBSOCKET,
                tag = "WS_InitFail",
                message = "Failed to instantiate WebSocket client for $targetWsUrl",
                errorDetails = e.localizedMessage
            )
        }
    }

    private fun parseWebSocketMessage(text: String) {
        val array = JSONArray(text)
        var updated = false

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val binanceSymbol = item.optString("s")
            val forexSymbol = binanceSymbolToForexMap[binanceSymbol] ?: continue

            val closePrice = item.optString("c").toDoubleOrNull() ?: continue
            val openPrice = item.optString("o").toDoubleOrNull() ?: closePrice
            val highPrice = item.optString("h").toDoubleOrNull() ?: closePrice
            val lowPrice = item.optString("l").toDoubleOrNull() ?: closePrice

            val dailyChangePercent = if (openPrice > 0) ((closePrice - openPrice) / openPrice) * 100.0 else 0.0

            updatePairData(
                symbol = forexSymbol,
                currentPrice = closePrice,
                dailyChangePercent = dailyChangePercent,
                high = highPrice,
                low = lowPrice
            )
            updated = true
        }

        if (updated) {
            publishPairs()
        }
    }

    private fun parseBinance24hrRest(bodyStr: String) {
        val array = JSONArray(bodyStr)
        var updated = false

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val binanceSymbol = item.optString("symbol")
            val forexSymbol = binanceSymbolToForexMap[binanceSymbol] ?: continue

            val lastPrice = item.optString("lastPrice").toDoubleOrNull() ?: continue
            val priceChangePercent = item.optString("priceChangePercent").toDoubleOrNull() ?: 0.0
            val highPrice = item.optString("highPrice").toDoubleOrNull() ?: lastPrice
            val lowPrice = item.optString("lowPrice").toDoubleOrNull() ?: lastPrice

            updatePairData(
                symbol = forexSymbol,
                currentPrice = lastPrice,
                dailyChangePercent = priceChangePercent,
                high = highPrice,
                low = lowPrice
            )
            updated = true
        }

        if (updated) {
            publishPairs()
        }
    }

    private fun parseFallbackExchangeApi(bodyStr: String) {
        val json = JSONObject(bodyStr)
        val rates = json.optJSONObject("rates") ?: return

        // Base USD rates
        val eurRate = rates.optDouble("EUR", 0.92)
        val gbpRate = rates.optDouble("GBP", 0.79)
        val jpyRate = rates.optDouble("JPY", 155.0)
        val audRate = rates.optDouble("AUD", 1.51)
        val cadRate = rates.optDouble("CAD", 1.36)
        val nzdRate = rates.optDouble("NZD", 1.63)
        val chfRate = rates.optDouble("CHF", 0.90)

        val pairs = mapOf(
            "EUR/USD" to if (eurRate > 0) 1.0 / eurRate else 1.0850,
            "GBP/USD" to if (gbpRate > 0) 1.0 / gbpRate else 1.2680,
            "USD/JPY" to jpyRate,
            "AUD/USD" to if (audRate > 0) 1.0 / audRate else 0.6620,
            "USD/CAD" to cadRate,
            "NZD/USD" to if (nzdRate > 0) 1.0 / nzdRate else 0.6110,
            "USD/CHF" to chfRate
        )

        pairs.forEach { (symbol, price) ->
            updatePairData(
                symbol = symbol,
                currentPrice = price,
                dailyChangePercent = 0.15,
                high = price * 1.004,
                low = price * 0.996
            )
        }
        publishPairs()
    }

    private fun updatePairData(
        symbol: String,
        currentPrice: Double,
        dailyChangePercent: Double,
        high: Double,
        low: Double
    ) {
        val existing = pairDataMap[symbol]
        val name = ForexEngine.getPairName(symbol)

        val history = if (existing != null && existing.history.isNotEmpty()) {
            val updatedList = existing.history.toMutableList()
            // Append current live tick
            val last = updatedList.last()
            updatedList[updatedList.lastIndex] = last.copy(
                close = currentPrice,
                high = maxOf(last.high, high, currentPrice),
                low = minOf(last.low, low, currentPrice)
            )
            updatedList
        } else {
            generateLiveHistory(symbol, currentPrice)
        }

        val volatility = when (symbol) {
            "USD/JPY", "GBP/USD" -> 7.8
            "EUR/USD", "USD/CAD" -> 5.2
            "AUD/USD", "NZD/USD" -> 6.9
            else -> 4.5
        } + (abs(dailyChangePercent) * 0.5)

        val news = existing?.news ?: listOf(
            "Live market volatility driven by dynamic real-time liquidity ticks.",
            "Central bank rate monitors tracking $symbol channel momentum."
        )

        pairDataMap[symbol] = ForexPairData(
            symbol = symbol,
            name = name,
            currentPrice = currentPrice,
            dailyChangePercent = dailyChangePercent,
            volatilityScore = volatility.coerceIn(1.0, 10.0),
            history = history,
            news = news
        )
    }

    private fun generateLiveHistory(symbol: String, basePrice: Double): List<PricePoint> {
        val history = mutableListOf<PricePoint>()
        val now = System.currentTimeMillis()
        var current = basePrice

        for (i in 30 downTo 1) {
            val timestamp = now - (i * 86400000)
            val trend = kotlin.math.sin(i.toDouble() / 4.0) * (basePrice * 0.005)
            val open = current
            val close = current + trend
            val high = maxOf(open, close) * 1.002
            val low = minOf(open, close) * 0.998

            history.add(
                PricePoint(
                    timestamp = timestamp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = 12000.0 + (i * 300)
                )
            )
            current = close
        }
        return history
    }

    private fun publishPairs() {
        val orderedSymbols = ForexEngine.pairsList.map { it.first }
        val sortedList = orderedSymbols.mapNotNull { pairDataMap[it] }
        if (sortedList.isNotEmpty()) {
            _livePairsFlow.value = sortedList
        }
    }

    fun getPairData(symbol: String): ForexPairData? {
        return pairDataMap[symbol] ?: ForexEngine.getPairData(symbol)
    }
}
