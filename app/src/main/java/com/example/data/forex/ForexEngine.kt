package com.example.data.forex

import java.util.Random
import kotlin.math.sin

data class PricePoint(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class ForexPairData(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val dailyChangePercent: Double,
    val volatilityScore: Double, // 1 to 10
    val history: List<PricePoint>,
    val news: List<String>
)

object ForexEngine {
    private val random = Random()

    val pairsList = listOf(
        "EUR/USD" to "Euro / US Dollar",
        "GBP/USD" to "British Pound / US Dollar",
        "USD/JPY" to "US Dollar / Japanese Yen",
        "AUD/USD" to "Australian Dollar / US Dollar",
        "USD/CAD" to "US Dollar / Canadian Dollar",
        "NZD/USD" to "New Zealand Dollar / US Dollar",
        "USD/CHF" to "US Dollar / Swiss Franc"
    )

    // Base prices for simulation
    private val basePrices = mapOf(
        "EUR/USD" to 1.0850,
        "GBP/USD" to 1.2680,
        "USD/JPY" to 155.40,
        "AUD/USD" to 0.6620,
        "USD/CAD" to 1.3650,
        "NZD/USD" to 0.6110,
        "USD/CHF" to 0.9020
    )

    // Generate simulated news headlines per pair
    private val newsTemplates = mapOf(
        "EUR" to listOf(
            "ECB policymakers express caution over rapid rate cuts.",
            "Eurozone manufacturing PMI rises slightly, beating expectations.",
            "German economic sentiment rebounds amid easing inflation.",
            "Slowing wage growth in Eurozone keeps ECB on hold."
        ),
        "USD" to listOf(
            "US inflation remains sticky; Federal Reserve signals higher for longer.",
            "US Treasury yields climb to monthly highs on robust retail sales.",
            "Non-Farm Payrolls beat forecasts, highlighting job market strength.",
            "Fed Minutes show intense debate over inflation trajectory."
        ),
        "GBP" to listOf(
            "Bank of England signals potential summer rate cut.",
            "UK inflation falls closer to Bank of England's 2% target.",
            "UK GDP shows modest recovery, exiting technical recession.",
            "British wage growth persists, complicating central bank decision."
        ),
        "JPY" to listOf(
            "Bank of Japan intervenes in currency market to prop up Yen.",
            "BOJ Governor Ueda hints at further interest rate normalization.",
            "Japan's household spending falls, dampening economic recovery.",
            "Yen remains vulnerable as yield differentials persist."
        ),
        "AUD" to listOf(
            "Reserve Bank of Australia keeps rates on hold, warns of inflation.",
            "Australian labor market remains tight, supporting local dollar.",
            "Commodity price rebound boosts Aussie dollar outlook."
        ),
        "CAD" to listOf(
            "Bank of Canada cuts interest rates by 25 basis points.",
            "Canadian inflation falls to 2.5%, opening door to further easing.",
            "Oil price fluctuations create headwinds for Canadian Dollar."
        ),
        "CHF" to listOf(
            "Swiss National Bank surprises market with second rate cut.",
            "Swiss franc attracts safe-haven flows amid global uncertainty."
        )
    )

    fun getPairName(symbol: String): String {
        return pairsList.firstOrNull { it.first == symbol }?.second ?: "Currency Pair"
    }

    /**
     * Generates a realistic price history and current stats for a pair.
     * Uses a seed based on the symbol and the current day to ensure daily consistency
     * but dynamic intraday simulation.
     */
    fun getPairData(symbol: String): ForexPairData {
        val liveData = LiveForexWebSocketService.getPairData(symbol)
        if (liveData != null) {
            return liveData
        }
        val basePrice = basePrices[symbol] ?: 1.0000
        val seed = symbol.hashCode().toLong() + (System.currentTimeMillis() / 86400000)
        val pairRandom = Random(seed)

        // Calculate a volatility score (1-10) based on typical currency characteristics
        val volatility = when (symbol) {
            "USD/JPY", "GBP/USD" -> 7.5
            "EUR/USD", "USD/CAD" -> 5.0
            "AUD/USD", "NZD/USD" -> 6.8
            else -> 4.0
        } + pairRandom.nextDouble() * 1.5

        // Generate 30 days of historical data
        val history = mutableListOf<PricePoint>()
        val now = System.currentTimeMillis()
        var lastPrice = basePrice

        // Add historical prices ending with today
        for (i in 30 downTo 1) {
            val dayMillis = now - (i * 86400000)
            // Use sin/cos with some random walk to simulate realistic trends
            val trend = sin(i.toDouble() / 5.0) * 0.015
            val noise = (pairRandom.nextDouble() - 0.5) * 0.010
            
            val open = lastPrice
            val close = lastPrice + trend + noise
            val high = maxOf(open, close) + pairRandom.nextDouble() * 0.005
            val low = minOf(open, close) - pairRandom.nextDouble() * 0.005
            val volume = 10000 + pairRandom.nextDouble() * 15000

            history.add(
                PricePoint(
                    timestamp = dayMillis,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume
                )
            )
            lastPrice = close
        }

        // Current price incorporates small immediate fluctuations
        val currentSeed = System.currentTimeMillis() / 15000 // updates every 15s
        val intraDayRandom = Random(symbol.hashCode().toLong() + currentSeed)
        val fluctuation = (intraDayRandom.nextDouble() - 0.5) * (basePrice * 0.001)
        val currentPrice = lastPrice + fluctuation

        val dayOpenPrice = history.lastOrNull()?.open ?: basePrice
        val dailyChangePercent = ((currentPrice - dayOpenPrice) / dayOpenPrice) * 100.0

        // Pull relevant news
        val baseCurrencies = symbol.split("/")
        val news = mutableListOf<String>()
        baseCurrencies.forEach { cur ->
            newsTemplates[cur]?.let { templates ->
                // Grab 2 random news articles from templates
                val templatesCopy = templates.toMutableList()
                val n1 = templatesCopy.removeAt(intraDayRandom.nextInt(templatesCopy.size))
                val n2 = templatesCopy.removeAt(intraDayRandom.nextInt(templatesCopy.size))
                news.add(n1)
                news.add(n2)
            }
        }

        return ForexPairData(
            symbol = symbol,
            name = getPairName(symbol),
            currentPrice = currentPrice,
            dailyChangePercent = dailyChangePercent,
            volatilityScore = volatility,
            history = history,
            news = news
        )
    }

    /**
     * Helper to get data for all pairs.
     */
    fun getAllPairsData(): List<ForexPairData> {
        val liveList = LiveForexWebSocketService.livePairsFlow.value
        if (liveList.isNotEmpty()) {
            return liveList
        }
        return pairsList.map { getPairData(it.first) }
    }
}
