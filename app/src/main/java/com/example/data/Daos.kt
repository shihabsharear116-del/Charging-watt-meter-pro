package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargingHistoryDao {
    @Query("SELECT * FROM charging_history ORDER BY startTime DESC")
    fun getAllHistory(): Flow<List<ChargingHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ChargingHistory)

    @Delete
    suspend fun deleteHistory(history: ChargingHistory)

    @Query("DELETE FROM charging_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Int)

    @Query("DELETE FROM charging_history")
    suspend fun clearAllHistory()
}

@Dao
interface BatteryStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: BatteryStats)

    @Query("SELECT * FROM battery_stats ORDER BY timestamp DESC")
    fun getAllStats(): Flow<List<BatteryStats>>

    @Query("SELECT * FROM battery_stats WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun getStatsSince(since: Long): Flow<List<BatteryStats>>

    @Query("SELECT * FROM battery_stats ORDER BY timestamp DESC LIMIT 100")
    fun getRecentStats(): Flow<List<BatteryStats>>

    @Query("DELETE FROM battery_stats WHERE timestamp < :expiration")
    suspend fun pruneOldStats(expiration: Long)
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettingsDirect(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettings)
}
