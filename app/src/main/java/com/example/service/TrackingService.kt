package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.TravelWakeApp
import com.example.model.AlarmStage
import com.example.service.alarm.AlarmSoundManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var ringtonePlayer: Ringtone? = null
    private var vibrator: Vibrator? = null

    companion object {
        const val CHANNEL_ID_TRACKING = "travelwake_tracking_channel"
        const val CHANNEL_ID_ALARM = "travelwake_alarm_channel"
        const val NOTIFICATION_ID_TRACKING = 1001
        const val NOTIFICATION_ID_ALARM = 1002

        const val ACTION_START = "com.example.travelwake.START"
        const val ACTION_PAUSE = "com.example.travelwake.PAUSE"
        const val ACTION_RESUME = "com.example.travelwake.RESUME"
        const val ACTION_WAKE_NOW = "com.example.travelwake.WAKE_NOW"
        const val ACTION_END_TRIP = "com.example.travelwake.END_TRIP"
        const val ACTION_SNOOZE = "com.example.travelwake.SNOOZE"

        fun startService(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_END_TRIP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initVibrator()
        acquireWakeLock()
        observeActiveTrip()
        observeAlarmEvents()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? TravelWakeApp
        val repo = app?.repository

        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID_TRACKING, buildTrackingNotification("Tracking started...", "Calculating dynamic ETA..."))
                startLocationUpdates()
            }
            ACTION_PAUSE -> {
                serviceScope.launch { repo?.pauseTrip() }
            }
            ACTION_RESUME -> {
                serviceScope.launch { repo?.resumeTrip() }
            }
            ACTION_WAKE_NOW -> {
                serviceScope.launch { repo?.triggerWakeMeNow() }
            }
            ACTION_SNOOZE -> {
                stopAlarmPlayback()
                serviceScope.launch { repo?.snoozeAlarm(2) }
            }
            ACTION_END_TRIP -> {
                stopAlarmPlayback()
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun observeActiveTrip() {
        val app = application as? TravelWakeApp ?: return
        serviceScope.launch {
            app.repository.activeTrip.collectLatest { trip ->
                if (trip == null) {
                    stopAlarmPlayback()
                    stopLocationUpdates()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    val destName = trip.destination.name
                    val distKm = "%.1f km".format(trip.remainingDistanceMeters / 1000.0)
                    val etaMin = trip.remainingMinutes?.let { "$it min" } ?: "Calculating..."
                    val title = "Approaching $destName"
                    val content = "Distance: $distKm | Dynamic ETA: $etaMin (${trip.etaConfidence.label})"

                    val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notifManager.notify(NOTIFICATION_ID_TRACKING, buildTrackingNotification(title, content))
                }
            }
        }
    }

    private fun observeAlarmEvents() {
        val app = application as? TravelWakeApp ?: return
        serviceScope.launch {
            app.repository.alarmTriggerEvent.collectLatest { stage ->
                if (stage == AlarmStage.MAIN_ALARM_TRIGGERED ||
                    stage == AlarmStage.FINAL_WARNING_TRIGGERED ||
                    stage == AlarmStage.ARRIVAL_CONFIRMED) {
                    startAlarmPlayback(stage)
                } else {
                    stopAlarmPlayback()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L)
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val app = application as? TravelWakeApp ?: return
                serviceScope.launch {
                    app.repository.updateLocation(loc)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            // Handled in UI readiness
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    private fun startAlarmPlayback(stage: AlarmStage) {
        // Audio playback using user-selected custom ringtone
        val app = applicationContext as? TravelWakeApp
        val settings = app?.repository?.settings?.value
        val soundEnabled = settings?.soundEnabled ?: true
        if (soundEnabled) {
            val ringtoneId = settings?.selectedRingtoneId ?: "metro_chime"
            val uriStr = settings?.selectedRingtoneUri
            val volume = settings?.alarmVolume ?: 0.9f
            AlarmSoundManager.playAlarm(
                context = applicationContext,
                ringtoneId = ringtoneId,
                uriString = uriStr,
                volume = volume
            )
        }

        // Vibration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 500, 200, 500, 200, 800)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 500, 200, 500), 0)
        }

        // High priority heads-up notification
        val label = when (stage) {
            AlarmStage.ARRIVAL_CONFIRMED -> "YOU HAVE ARRIVED AT YOUR DESTINATION!"
            AlarmStage.FINAL_WARNING_TRIGGERED -> "FINAL CALL: PREPARE TO DISEMBARK NOW!"
            else -> "WAKE UP: DESTINATION APPROACHING!"
        }

        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(NOTIFICATION_ID_ALARM, buildAlarmNotification(label))
    }

    private fun stopAlarmPlayback() {
        try {
            AlarmSoundManager.stopAudio()
            ringtonePlayer?.stop()
            ringtonePlayer = null
            vibrator?.cancel()
            val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifManager.cancel(NOTIFICATION_ID_ALARM)
        } catch (_: Exception) {}
    }

    private fun buildTrackingNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val wakeNowIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_WAKE_NOW }
        val wakeNowPending = PendingIntent.getService(this, 1, wakeNowIntent, PendingIntent.FLAG_IMMUTABLE)

        val endTripIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_END_TRIP }
        val endTripPending = PendingIntent.getService(this, 2, endTripIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID_TRACKING)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_play, "Wake Me Now", wakeNowPending)
            .addAction(android.R.drawable.ic_delete, "End Trip", endTripPending)
            .build()
    }

    private fun buildAlarmNotification(alertTitle: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_SNOOZE }
        val snoozePending = PendingIntent.getService(this, 3, snoozeIntent, PendingIntent.FLAG_IMMUTABLE)

        val endTripIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_END_TRIP }
        val endTripPending = PendingIntent.getService(this, 4, endTripIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID_ALARM)
            .setContentTitle(alertTitle)
            .setContentText("Tap to open TravelWake or disembark safely.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_input_get, "Snooze 2m", snoozePending)
            .addAction(android.R.drawable.ic_delete, "I'm Awake", endTripPending)
            .build()
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TravelWake::TrackingWakeLock").apply {
            acquire()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val trackingChannel = NotificationChannel(
                CHANNEL_ID_TRACKING,
                "Trip Tracking & Dynamic ETA",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows continuous arrival estimates while sleeping"
            }

            val alarmChannel = NotificationChannel(
                CHANNEL_ID_ALARM,
                "Wake-Up Alarm & Arrival Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Triggers arrival wake-up tone and vibration"
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(trackingChannel)
            manager?.createNotificationChannel(alarmChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmPlayback()
        stopLocationUpdates()
        serviceScope.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
