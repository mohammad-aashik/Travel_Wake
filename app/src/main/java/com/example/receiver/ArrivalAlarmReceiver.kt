package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.TravelWakeApp
import com.example.service.TrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArrivalAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val destName = intent.getStringExtra("destination_name") ?: "Your Destination"
        val app = context.applicationContext as? TravelWakeApp

        app?.repository?.triggerAlarmFromScheduledArrival(destName)

        // Ensure tracking service wakes up and triggers alarm playback if needed
        try {
            val serviceIntent = Intent(context, TrackingService::class.java).apply {
                action = TrackingService.ACTION_WAKE_NOW
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (_: Exception) {
            // Service start fallback handled by in-app repository alarm state
        }
    }
}
