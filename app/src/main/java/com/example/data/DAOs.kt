package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE id = 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfile)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist_items ORDER BY addedAt DESC")
    fun getWatchlistFlow(): Flow<List<WatchlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistItem)

    @Query("DELETE FROM watchlist_items WHERE symbol = :symbol")
    suspend fun delete(symbol: String)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_items WHERE symbol = :symbol)")
    suspend fun isWatchlisted(symbol: String): Boolean
}

@Dao
interface RecommendationDao {
    @Query("SELECT * FROM recommendation_history ORDER BY createdAt DESC")
    fun getAllRecommendationHistoryFlow(): Flow<List<RecommendationEntity>>

    @Query("SELECT * FROM recommendation_history WHERE symbol = :symbol ORDER BY createdAt DESC")
    fun getRecommendationHistoryFlow(symbol: String): Flow<List<RecommendationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendation(entity: RecommendationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entity: RecommendationLogEntity)

    @Query("SELECT * FROM recommendation_logs ORDER BY createdAt DESC")
    fun getLogsFlow(): Flow<List<RecommendationLogEntity>>
}
