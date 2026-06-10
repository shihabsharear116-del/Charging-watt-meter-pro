package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.service.ChargingMonitorService

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_POWER_CONNECTED || 
            action == Intent.ACTION_POWER_DISCONNECTED || 
            action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                val serviceIntent = Intent(context, ChargingMonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("PowerConnectionReceiver", "Failed to start ChargingMonitorService from background: ${e.message}", e)
            }
        }
    }
}
