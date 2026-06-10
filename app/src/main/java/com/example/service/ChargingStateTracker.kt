package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ChargingMetrics(
    val percentage: Int = 0,
    val isCharging: Boolean = false,
    val voltage: Double = 0.0, // in Volts
    val current: Double = 0.0, // in Amperes
    val watt: Double = 0.0, // calculated
    val temperature: Double = 0.0, // in Celsius
    val health: String = "Good",
    val source: String = "AC",
    val chargingType: String = "Normal Charging",
    val remainingSeconds: Long = -1L,
    val maxWattObserved: Double = 0.0,
    val sumWattObserved: Double = 0.0,
    val countObservations: Int = 0,
    val sessionStartTime: Long = 0L,
    val startPercent: Int = 0
)

object ChargingStateTracker {
    private val _metrics = MutableStateFlow(ChargingMetrics())
    val metrics: StateFlow<ChargingMetrics> = _metrics

    fun update(updater: (ChargingMetrics) -> ChargingMetrics) {
        _metrics.value = updater(_metrics.value)
    }

    fun reset() {
        _metrics.value = ChargingMetrics()
    }
}
