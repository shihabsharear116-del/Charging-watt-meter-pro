package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.BatteryStats
import com.example.data.ChargingHistory
import com.example.data.GeminiAdvisor
import com.example.data.UserSettings
import com.example.service.ChargingMetrics
import com.example.service.ChargingStateTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface AiAdviceState {
    object Idle : AiAdviceState
    object Loading : AiAdviceState
    data class Success(val advice: String) : AiAdviceState
    data class Error(val message: String) : AiAdviceState
}

data class DashboardUiState(
    val metrics: ChargingMetrics = ChargingMetrics(),
    val settings: UserSettings = UserSettings(),
    val history: List<ChargingHistory> = emptyList(),
    val stats: List<BatteryStats> = emptyList(),
    val searchQuery: String = "",
    val filterType: String = "All",
    val aiAdvice: AiAdviceState = AiAdviceState.Idle
)

private data class StateQuad(
    val metrics: ChargingMetrics,
    val settings: UserSettings?,
    val history: List<ChargingHistory>,
    val stats: List<BatteryStats>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository
    private val searchQuery = MutableStateFlow("")
    private val filterType = MutableStateFlow("All")
    private val aiAdvice = MutableStateFlow<AiAdviceState>(AiAdviceState.Idle)

    val uiState: StateFlow<DashboardUiState>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AppRepository(db)

        val flowQuad = combine(
            ChargingStateTracker.metrics,
            repository.userSettings,
            repository.allHistory,
            repository.allStats
        ) { metrics, settings, history, stats ->
            StateQuad(metrics, settings, history, stats)
        }

        uiState = combine(
            flowQuad,
            searchQuery,
            filterType,
            aiAdvice
        ) { quad, query, filter, advice ->
            val metrics = quad.metrics
            val settings = quad.settings ?: UserSettings()
            val history = quad.history
            val stats = quad.stats
            
            // Apply Search and Filter queries
            val filteredHistory = history.filter { item ->
                val matchesQuery = item.date.contains(query, ignoreCase = true) || 
                        item.chargingType.contains(query, ignoreCase = true)
                val matchesFilter = if (filter == "All") true else item.chargingType.contains(filter, ignoreCase = true)
                matchesQuery && matchesFilter
            }

            DashboardUiState(
                metrics = metrics,
                settings = settings,
                history = filteredHistory,
                stats = stats,
                searchQuery = query,
                filterType = filter,
                aiAdvice = advice
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState()
        )
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateFilterType(filter: String) {
        filterType.value = filter
    }

    fun togglePremium() {
        viewModelScope.launch {
            val current = uiState.value.settings
            val updated = current.copy(isPremium = !current.isPremium)
            repository.saveSettings(updated)
        }
    }

    fun saveAlarmSettings(
        batteryFullAlert: Boolean,
        batteryFullLevel: Int,
        temperatureAlert: Boolean,
        temperatureLevel: Float,
        chargerConnectedAlert: Boolean,
        chargerDisconnectedAlert: Boolean
    ) {
        viewModelScope.launch {
            val current = uiState.value.settings
            val updated = current.copy(
                batteryFullAlert = batteryFullAlert,
                batteryFullLevel = batteryFullLevel,
                temperatureAlert = temperatureAlert,
                temperatureLevel = temperatureLevel,
                chargerConnectedAlert = chargerConnectedAlert,
                chargerDisconnectedAlert = chargerDisconnectedAlert
            )
            repository.saveSettings(updated)
        }
    }

    fun changeTheme(themeName: String) {
        viewModelScope.launch {
            val current = uiState.value.settings
            val updated = current.copy(customTheme = themeName)
            repository.saveSettings(updated)
        }
    }

    fun changeLanguage(langCode: String) {
        viewModelScope.launch {
            val current = uiState.value.settings
            val updated = current.copy(language = langCode)
            repository.saveSettings(updated)
        }
    }

    fun saveM3uPlaylists(playlists: String) {
        viewModelScope.launch {
            val current = uiState.value.settings
            val updated = current.copy(m3uPlaylists = playlists)
            repository.saveSettings(updated)
        }
    }

    fun deleteHistoryItem(item: ChargingHistory) {
        viewModelScope.launch {
            repository.deleteHistory(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }



    fun fetchAiAdvice() {
        viewModelScope.launch {
            aiAdvice.value = AiAdviceState.Loading
            try {
                val metrics = uiState.value.metrics
                val adviceResult = GeminiAdvisor.getBatteryAdvice(metrics)
                aiAdvice.value = AiAdviceState.Success(adviceResult)
            } catch (e: Exception) {
                aiAdvice.value = AiAdviceState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    // Native CSV Exporter
    fun exportHistoryToCSV(context: Context): Uri? {
        val list = uiState.value.history
        if (list.isEmpty()) return null
        
        val csvHeader = "ID,Date,Start Time,End Time,Start %,End %,Max Watts,Avg Watts,Avg Temp,Charging Type\n"
        val csvData = StringBuilder(csvHeader)
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        list.forEach { item ->
            val startFormatted = sdf.format(Date(item.startTime))
            val endFormatted = sdf.format(Date(item.endTime))
            csvData.append("${item.id},")
                .append("${item.date},")
                .append("$startFormatted,")
                .append("$endFormatted,")
                .append("${item.startPercent}%,")
                .append("${item.endPercent}%,")
                .append("${String.format("%.1f", item.maxWatt)}W,")
                .append("${String.format("%.1f", item.avgWatt)}W,")
                .append("${String.format("%.1f", item.avgTemp)}°C,")
                .append("\"${item.chargingType}\"\n")
        }

        return try {
            val cachePath = File(context.cacheDir, "exports")
            cachePath.mkdirs()
            val file = File(cachePath, "charging_history_export.csv")
            val stream = FileOutputStream(file)
            stream.write(csvData.toString().toByteArray())
            stream.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e("MainViewModel", "CSV export failed: ${e.message}", e)
            null
        }
    }

    // Native PDF Exporter using Android’s standard PdfDocument class (No bloated libraries needed!)
    fun exportHistoryToPDF(context: Context): Uri? {
        val list = uiState.value.history
        if (list.isEmpty()) return null

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size (595 x 842 px)
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val paintTitle = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isFakeBoldText = true
        }

        val paintHeader = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            isFakeBoldText = true
        }

        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 9f
        }

        val paintLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        // Draw header branding
        canvas.drawText("Charging Watt Meter Pro • Charging Audit Report", 30f, 45f, paintTitle)
        canvas.drawText("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 30f, 65f, paintText)
        canvas.drawLine(30f, 75f, 565f, 75f, paintLine)

        // Draw Table headers
        var yPosition = 95f
        canvas.drawText("Date", 35f, yPosition, paintHeader)
        canvas.drawText("Session Times", 110f, yPosition, paintHeader)
        canvas.drawText("Start/End", 240f, yPosition, paintHeader)
        canvas.drawText("Watt (Avg/Max)", 320f, yPosition, paintHeader)
        canvas.drawText("Temp", 420f, yPosition, paintHeader)
        canvas.drawText("Charging Speed Type", 470f, yPosition, paintHeader)
        canvas.drawLine(30f, yPosition + 5f, 565f, yPosition + 5f, paintLine)

        yPosition += 20f
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

        list.take(30).forEachIndexed { index, item ->
            // Check for page overflow
            if (yPosition > 800f) {
                document.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, index + 2).create()
                page = document.startPage(newPageInfo)
                canvas = page.canvas
                yPosition = 50f
                canvas.drawText("Charging History Record Continued...", 30f, yPosition, paintHeader)
                canvas.drawLine(30f, yPosition + 5f, 565f, yPosition + 5f, paintLine)
                yPosition += 20f
            }

            val times = "${sdf.format(Date(item.startTime))} - ${sdf.format(Date(item.endTime))}"
            val range = "${item.startPercent}% → ${item.endPercent}%"
            val watts = "${String.format("%.1f", item.avgWatt)}W / ${String.format("%.1f", item.maxWatt)}W"

            canvas.drawText(item.date, 35f, yPosition, paintText)
            canvas.drawText(times, 110f, yPosition, paintText)
            canvas.drawText(range, 240f, yPosition, paintText)
            canvas.drawText(watts, 320f, yPosition, paintText)
            canvas.drawText("${String.format("%.1f", item.avgTemp)}°C", 420f, yPosition, paintText)
            
            // Format safety name
            val trimmedType = item.chargingType.replace(" Charging", "")
            canvas.drawText(trimmedType, 470f, yPosition, paintText)

            yPosition += 20f
        }

        document.finishPage(page)

        return try {
            val cachePath = File(context.cacheDir, "exports")
            cachePath.mkdirs()
            val file = File(cachePath, "charging_audit_report.pdf")
            val stream = FileOutputStream(file)
            document.writeTo(stream)
            document.close()
            stream.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e("MainViewModel", "PDF export failed: ${e.message}", e)
            null
        }
    }
}
