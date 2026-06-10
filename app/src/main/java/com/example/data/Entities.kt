package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charging_history")
data class ChargingHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val startTime: Long,
    val endTime: Long,
    val maxWatt: Double,
    val avgWatt: Double,
    val avgTemp: Double,
    val startPercent: Int,
    val endPercent: Int,
    val chargingType: String
)

@Entity(tableName = "battery_stats")
data class BatteryStats(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val watt: Double,
    val voltage: Double,
    val current: Double,
    val temperature: Double,
    val percentage: Int
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val isPremium: Boolean = true,
    val batteryFullAlert: Boolean = true,
    val batteryFullLevel: Int = 85,
    val temperatureAlert: Boolean = true,
    val temperatureLevel: Float = 40.0f,
    val chargerConnectedAlert: Boolean = true,
    val chargerDisconnectedAlert: Boolean = true,
    val customTheme: String = "Emerald Green", // Elegant Dark, Cosmic Dark, Emerald Green, Pure Dark, Light Mode
    val m3uPlaylists: String = "", // Configured M3U/M3U8 URLs separated by semicolon
    val language: String = "bn"
)
