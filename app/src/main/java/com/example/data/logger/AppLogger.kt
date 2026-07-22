package com.example.data.logger

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR
}

enum class LogCategory {
    API_REST,
    WEBSOCKET,
    GEMINI_AI,
    SYSTEM,
    ALERT,
    DATABASE
}

data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val category: LogCategory,
    val tag: String,
    val message: String,
    val details: String? = null,
    val statusCode: Int? = null,
    val latencyMs: Long? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
}

object AppLogger {
    private const val MAX_LOGS = 200
    private const val TAG = "AppLogger"

    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    init {
        // Log initial startup entry
        logInfo(
            category = LogCategory.SYSTEM,
            tag = "SystemInit",
            message = "Forex Terminal System Logging Initialized",
            details = "Logging engine active. Capturing WebSocket streams, REST endpoints, Gemini AI requests, and system exceptions."
        )
    }

    private fun addEntry(entry: LogEntry) {
        val current = _logsFlow.value.toMutableList()
        current.add(0, entry) // Newest first
        if (current.size > MAX_LOGS) {
            _logsFlow.value = current.take(MAX_LOGS)
        } else {
            _logsFlow.value = current
        }

        val logMsg = "[${entry.category}] [${entry.tag}] ${entry.message} ${entry.details ?: ""}"
        when (entry.level) {
            LogLevel.INFO -> Log.i(TAG, logMsg)
            LogLevel.SUCCESS -> Log.i(TAG, "SUCCESS: $logMsg")
            LogLevel.WARN -> Log.w(TAG, logMsg)
            LogLevel.ERROR -> Log.e(TAG, logMsg)
        }
    }

    fun logInfo(category: LogCategory, tag: String, message: String, details: String? = null) {
        addEntry(LogEntry(level = LogLevel.INFO, category = category, tag = tag, message = message, details = details))
    }

    fun logSuccess(
        category: LogCategory,
        tag: String,
        message: String,
        details: String? = null,
        statusCode: Int? = 200,
        latencyMs: Long? = null
    ) {
        addEntry(
            LogEntry(
                level = LogLevel.SUCCESS,
                category = category,
                tag = tag,
                message = message,
                details = details,
                statusCode = statusCode,
                latencyMs = latencyMs
            )
        )
    }

    fun logWarn(category: LogCategory, tag: String, message: String, details: String? = null) {
        addEntry(LogEntry(level = LogLevel.WARN, category = category, tag = tag, message = message, details = details))
    }

    fun logError(
        category: LogCategory,
        tag: String,
        message: String,
        errorDetails: String? = null,
        statusCode: Int? = null,
        latencyMs: Long? = null
    ) {
        addEntry(
            LogEntry(
                level = LogLevel.ERROR,
                category = category,
                tag = tag,
                message = message,
                details = errorDetails,
                statusCode = statusCode,
                latencyMs = latencyMs
            )
        )
    }

    fun clearLogs() {
        _logsFlow.value = emptyList()
        logInfo(LogCategory.SYSTEM, "SystemLogger", "Terminal logs cleared by user.")
    }

    fun simulateApiFailure() {
        val scenarios = listOf(
            Triple("REST_TIMEOUT", 504, "Gateway Timeout: Binance REST endpoint /api/v3/ticker/24hr exceeded 10000ms limit"),
            Triple("GEMINI_429_RATE_LIMIT", 429, "HTTP 429 Resource Exhausted: Gemini 3.5 Flash quota exceeded. Switching to client heuristic analysis."),
            Triple("WS_SOCKET_DROP", 1006, "WebSocket Abnormal Closure: wss://stream.binance.com:9443 socket connection reset by peer"),
            Triple("EXT_API_AUTH_500", 401, "HTTP 401 Unauthorized: Custom Financial API key failed signature check at endpoint https://api.forex-data.org/v1/quotes"),
            Triple("DATABASE_CORRUPT_ERR", 500, "SQLiteException: Row conflict encountered while inserting trade transaction ID #TRD-8829")
        )

        val randomScenario = scenarios.random()
        logError(
            category = when {
                randomScenario.first.contains("GEMINI") -> LogCategory.GEMINI_AI
                randomScenario.first.contains("WS") -> LogCategory.WEBSOCKET
                randomScenario.first.contains("DATABASE") -> LogCategory.DATABASE
                else -> LogCategory.API_REST
            },
            tag = randomScenario.first,
            message = "Simulated Network & System Request Failure",
            errorDetails = randomScenario.third,
            statusCode = randomScenario.second,
            latencyMs = (150..2400).random().toLong()
        )
    }

    fun exportLogsAsString(): String {
        return _logsFlow.value.joinToString("\n") { entry ->
            "[${entry.formattedTime}] [${entry.level}] [${entry.category}] [${entry.tag}] ${entry.message}" +
                    (if (entry.statusCode != null) " (Code: ${entry.statusCode})" else "") +
                    (if (entry.latencyMs != null) " (${entry.latencyMs}ms)" else "") +
                    (if (!entry.details.isNullOrBlank()) "\n   Details: ${entry.details}" else "")
        }
    }
}
