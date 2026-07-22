package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val riskLevel: String = "Moderate",
    val experience: String = "Intermediate",
    val preferredPairs: String = "EUR/USD,GBP/USD,USD/JPY",
    val notificationsEnabled: Boolean = true,
    val aiPreferences: String = "Technical & Sentiment",
    val themeMode: String = "Dark", // "Light", "Dark", "Auto"
    val externalApiKey: String = "", // Custom API integration key
    val externalApiUrl: String = "https://api.polygon.io/v2/reference/news", // Default external financial market news feed
    val primaryColorHex: String = "#2563EB", // Dynamic theme primary color
    val hapticFeedbackEnabled: Boolean = true,
    val audioAlertsEnabled: Boolean = true,
    val dataSaverEnabled: Boolean = false,
    val autoRefreshRateSec: Int = 5
)

@Entity(tableName = "watchlist_items")
data class WatchlistItem(
    @PrimaryKey val symbol: String,
    val addedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

@Entity(tableName = "recommendation_history")
data class RecommendationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val confidence: Int,
    val suggestedAction: String,
    val rationale: String,
    val riskScore: Int,
    val keyNews: String, // Comma-separated or JSON string
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recommendation_logs")
data class RecommendationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val prompt: String,
    val response: String,
    val marketSnapshot: String,
    val newsSnapshot: String,
    val createdAt: Long = System.currentTimeMillis()
)
