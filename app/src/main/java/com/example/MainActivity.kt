package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.ChargingApp
import com.example.viewmodel.MainViewModel
import com.example.service.ChargingMonitorService

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()

    private val terminateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            if (intent.action == "com.example.ACTION_TERMINATE_APP") {
                Log.d("MainActivity", "Received termination broadcast. Closing activity task.")
                finishAndRemoveTask()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register shutdown broadcast receiver
        val filter = android.content.IntentFilter("com.example.ACTION_TERMINATE_APP")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(terminateReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(terminateReceiver, filter)
        }
        
        // Dynamically request notification permissions on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        
        // Start foreground charging service meter on app launch
        try {
            val serviceIntent = Intent(this, ChargingMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start ChargingMonitorService: ${e.message}", e)
        }
        
        enableEdgeToEdge()
        setContent {
            ChargingApp(viewModel = viewModel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(terminateReceiver)
        } catch (e: Exception) {
            // Ignore if already unregistered
        }
    }
}
