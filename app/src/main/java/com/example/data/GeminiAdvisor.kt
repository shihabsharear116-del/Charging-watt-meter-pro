package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.service.ChargingMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiAdvisor {
    private const val TAG = "GeminiAdvisor"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getBatteryAdvice(metrics: ChargingMetrics): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "null") {
            Log.w(TAG, "No valid Gemini API key found, spawning local chemical rules advisor.")
            return@withContext getOfflineBatteryAdvice(metrics)
        }

        val prompt = """
            You are the Charging Watt Meter Pro AI Battery Advisor.
            Analyze this device status:
            - Battery Level: ${metrics.percentage}%
            - Power: ${String.format("%.1f", metrics.watt)}W
            - Voltage: ${String.format("%.2f", metrics.voltage)}V
            - Current: ${String.format("%.2f", metrics.current)}A
            - Temperature: ${metrics.temperature}°C
            - Health: ${metrics.health}
            - Source: ${metrics.source}
            - Speed: ${metrics.chargingType}

            Write a professional diagnostic report (max 100 words).
            Discuss battery health impact, temperature safety, and rate of heat dissipation.
            Keep it clear, technical, helpful, and concise. No generic introductory remarks.
        """.trimIndent()

        try {
            val jsonRequestBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val partObj = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(partObj)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
            }

            val requestBody = jsonRequestBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP error: ${response.code} - ${response.message}")
                    return@withContext getOfflineBatteryAdvice(metrics)
                }

                val bodyString = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                val textResponse = firstPart?.optString("text")

                if (!textResponse.isNullOrBlank()) {
                    return@withContext textResponse.trim()
                } else {
                    return@withContext getOfflineBatteryAdvice(metrics)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call crashed: ${e.message}", e)
            return@withContext getOfflineBatteryAdvice(metrics)
        }
    }

    private fun getOfflineBatteryAdvice(m: ChargingMetrics): String {
        return buildString {
            append("⚡ ")
            if (m.isCharging) {
                append("Currently receiving ${String.format("%.1f", m.watt)}W from ${m.source} using ${m.chargingType}. ")
                if (m.temperature >= 38.0) {
                    append("Warning: Current temperature of ${m.temperature}°C is high. ")
                    append("To preserve anode safety and mitigate Lithium plating, keep background apps closed and let the device cool. ")
                } else {
                    append("Thermal levels look safe (${m.temperature}°C). ")
                }

                if (m.percentage in 15..80) {
                    append("Your battery is in its optimal charge zone (15% to 80%). Active Charging is currently operating efficiently.")
                } else if (m.percentage > 80) {
                    append("Battery is past 80%. Consider unplugging to reduce voltage-saturating stress, as chemical wear accelerates near top levels.")
                } else {
                    append("Battery level is quite low. Charger is supplying full standard voltage to boost recovery.")
                }
            } else {
                append("Unplugged. Battery is sitting at ${m.percentage}% under a temperature of ${m.temperature}°C. ")
                append("Battery health classification is reported as: ${m.health}. Avoid deep discharges below 15% to protect electrolyte lifespan.")
            }
        }
    }
}
