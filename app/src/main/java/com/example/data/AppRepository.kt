package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {
    val allHistory: Flow<List<ChargingHistory>> = db.chargingHistoryDao.getAllHistory()
    val allStats: Flow<List<BatteryStats>> = db.batteryStatsDao.getRecentStats()
    val userSettings: Flow<UserSettings?> = db.userSettingsDao.getSettingsFlow()

    suspend fun insertHistory(history: ChargingHistory) {
        db.chargingHistoryDao.insertHistory(history)
    }

    suspend fun deleteHistory(history: ChargingHistory) {
        db.chargingHistoryDao.deleteHistory(history)
    }

    suspend fun deleteHistoryById(id: Int) {
        db.chargingHistoryDao.deleteHistoryById(id)
    }

    suspend fun clearAllHistory() {
        db.chargingHistoryDao.clearAllHistory()
    }

    suspend fun insertStats(stats: BatteryStats) {
        db.batteryStatsDao.insertStats(stats)
        // Auto-prune stats older than 3 days to keep database lightweight
        val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
        db.batteryStatsDao.pruneOldStats(threeDaysAgo)
    }

    suspend fun getSettingsDirect(): UserSettings {
        return db.userSettingsDao.getSettingsDirect() ?: UserSettings().also {
            db.userSettingsDao.saveSettings(it)
        }
    }

    suspend fun saveSettings(settings: UserSettings) {
        db.userSettingsDao.saveSettings(settings)
    }

    fun getStatsSinceFlow(since: Long): Flow<List<BatteryStats>> {
        return db.batteryStatsDao.getStatsSince(since)
    }
}
