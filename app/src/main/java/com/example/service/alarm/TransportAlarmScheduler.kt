package com.example.service.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import com.example.model.DestinationSnapshot
import com.example.model.TransitRoute
import com.example.model.TransportMode
import com.example.receiver.ArrivalAlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Result data class for an estimated destination arrival time
 */
data class TransportArrivalTimeEstimate(
    val distanceKm: Double,
    val durationMinutes: Int,
    val arrivalTimeMillis: Long,
    val formattedArrivalTime: String,
    val formattedDuration: String
)

/**
 * Result data class when a phone alarm is scheduled based on transport search arrival
 */
data class AlarmScheduleResult(
    val success: Boolean,
    val destinationName: String,
    val routeName: String,
    val estimatedDurationMinutes: Int,
    val arrivalTimeMillis: Long,
    val alarmTimeMillis: Long,
    val alarmHour: Int,
    val alarmMinute: Int,
    val alertMinutesBefore: Int,
    val formattedArrivalTime: String,
    val formattedAlarmTime: String,
    val systemClockSet: Boolean,
    val inAppAlarmSet: Boolean,
    val message: String
)

object TransportAlarmScheduler {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    /**
     * Calculates the estimated arrival time for a transport destination
     */
    fun estimateArrivalTime(
        destination: DestinationSnapshot,
        userLat: Double? = null,
        userLng: Double? = null,
        route: TransitRoute? = null
    ): TransportArrivalTimeEstimate {
        val now = System.currentTimeMillis()

        // 1. Calculate approximate transit distance in km
        val distanceKm: Double = if (userLat != null && userLng != null && userLat != 0.0 && userLng != 0.0) {
            val directDistKm = calculateHaversineKm(userLat, userLng, destination.latitude, destination.longitude)
            // Account for transit curvature
            when (destination.transportMode) {
                TransportMode.BUS -> directDistKm * 1.25
                TransportMode.TRAIN -> directDistKm * 1.15
                TransportMode.METRO -> directDistKm * 1.15
                TransportMode.TRAM -> directDistKm * 1.20
                TransportMode.FERRY -> directDistKm * 1.10
                TransportMode.WALKING -> directDistKm * 1.20
                else -> directDistKm * 1.22
            }
        } else if (route != null && route.majorStops.isNotEmpty()) {
            // Estimate based on stops along the route
            val stopIndex = route.majorStops.indexOfFirst {
                it.name.equals(destination.name, ignoreCase = true) ||
                (Math.abs(it.latitude - destination.latitude) < 0.005 && Math.abs(it.longitude - destination.longitude) < 0.005)
            }
            if (stopIndex > 0) {
                stopIndex * 2.8 // approx 2.8 km per inter-station section
            } else {
                12.0 // default commuter leg in km
            }
        } else {
            12.0 // standard urban transit distance in km
        }

        // 2. Average operational speed per transit mode in km/h
        val avgSpeedKmh = when (destination.transportMode) {
            TransportMode.BUS -> 26.0
            TransportMode.TRAIN -> 52.0
            TransportMode.METRO -> 36.0
            TransportMode.TRAM -> 20.0
            TransportMode.FERRY -> 18.0
            TransportMode.CAR_TAXI -> 35.0
            TransportMode.WALKING -> 4.8
            else -> 28.0
        }

        // 3. Compute duration in minutes (including initial waiting / boarding buffer)
        val travelMinutes = ((distanceKm / avgSpeedKmh) * 60.0).roundToInt()
        val totalDurationMinutes = (travelMinutes + 4).coerceAtLeast(3)

        val arrivalMillis = now + (totalDurationMinutes * 60 * 1000L)
        val formattedArrival = timeFormat.format(Date(arrivalMillis))
        val formattedDuration = if (totalDurationMinutes >= 60) {
            val hours = totalDurationMinutes / 60
            val mins = totalDurationMinutes % 60
            "${hours}h ${mins}m"
        } else {
            "${totalDurationMinutes} mins"
        }

        return TransportArrivalTimeEstimate(
            distanceKm = distanceKm,
            durationMinutes = totalDurationMinutes,
            arrivalTimeMillis = arrivalMillis,
            formattedArrivalTime = formattedArrival,
            formattedDuration = formattedDuration
        )
    }

    /**
     * Sets a phone alarm based on destination arrival time identified from transport search.
     *
     * 1. Opens / sets the system phone alarm clock in the Android Clock app.
     * 2. Schedules an exact/wake alarm with Android's AlarmManager for guaranteed TravelWake ringtone & vibration.
     *
     * @param context Application / Activity context
     * @param destination The selected destination snapshot from transport search
     * @param alertMinutesBefore How many minutes prior to arrival to sound the alarm (e.g. 5, 7, 10 min)
     * @param userLat Current latitude if known
     * @param userLng Current longitude if known
     * @param route Optional transit route if identified
     * @param openSystemClockApp Whether to present the system Clock app UI to the user
     */
    fun setPhoneAlarmForArrival(
        context: Context,
        destination: DestinationSnapshot,
        alertMinutesBefore: Int = 5,
        userLat: Double? = null,
        userLng: Double? = null,
        route: TransitRoute? = null,
        openSystemClockApp: Boolean = true
    ): AlarmScheduleResult {
        // Step 1: Calculate arrival time
        val estimate = estimateArrivalTime(
            destination = destination,
            userLat = userLat,
            userLng = userLng,
            route = route
        )

        // Step 2: Determine target alarm trigger timestamp (arrival minus lead time buffer)
        val now = System.currentTimeMillis()
        val calculatedAlarmMillis = estimate.arrivalTimeMillis - (alertMinutesBefore * 60 * 1000L)
        // Ensure alarm is in the future (minimum 1 minute from current time)
        val alarmTimeMillis = calculatedAlarmMillis.coerceAtLeast(now + 60 * 1000L)

        // Step 3: Extract hour and minute of alarm
        val calendar = Calendar.getInstance().apply {
            timeInMillis = alarmTimeMillis
        }
        val alarmHour = calendar.get(Calendar.HOUR_OF_DAY)
        val alarmMinute = calendar.get(Calendar.MINUTE)

        val formattedArrivalTime = estimate.formattedArrivalTime
        val formattedAlarmTime = timeFormat.format(Date(alarmTimeMillis))

        var systemClockSet = false
        var inAppAlarmSet = false

        // Step 4: Schedule system phone alarm via AlarmClock.ACTION_SET_ALARM
        try {
            val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, alarmHour)
                putExtra(AlarmClock.EXTRA_MINUTES, alarmMinute)
                val routeLabel = if (destination.routeName.isNotBlank() && destination.routeName != "Direct / Default Route") {
                    " (${destination.routeName})"
                } else ""
                putExtra(AlarmClock.EXTRA_MESSAGE, "TravelWake: Arrive at ${destination.name}$routeLabel")
                putExtra(AlarmClock.EXTRA_VIBRATE, true)
                putExtra(AlarmClock.EXTRA_SKIP_UI, !openSystemClockApp)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (alarmIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(alarmIntent)
                systemClockSet = true
            } else {
                // Try launching anyway for system clock handlers
                context.startActivity(alarmIntent)
                systemClockSet = true
            }
        } catch (_: Exception) {
            systemClockSet = false
        }

        // Step 5: Schedule exact alarm via AlarmManager in TravelWake
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                val receiverIntent = Intent(context, ArrivalAlarmReceiver::class.java).apply {
                    putExtra("destination_name", destination.name)
                    putExtra("route_name", destination.routeName)
                    putExtra("alarm_time_millis", alarmTimeMillis)
                }

                val requestCode = (destination.name.hashCode() + destination.latitude.toInt()).and(0x7FFFFFFF)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    receiverIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        alarmTimeMillis,
                        pendingIntent
                    )
                }
                inAppAlarmSet = true
            }
        } catch (_: Exception) {
            inAppAlarmSet = false
        }

        val success = systemClockSet || inAppAlarmSet
        val message = if (success) {
            "Alarm scheduled for $formattedAlarmTime ($alertMinutesBefore mins before arrival at $formattedArrivalTime)"
        } else {
            "Could not schedule system alarm. Please check alarm permissions."
        }

        return AlarmScheduleResult(
            success = success,
            destinationName = destination.name,
            routeName = destination.routeName,
            estimatedDurationMinutes = estimate.durationMinutes,
            arrivalTimeMillis = estimate.arrivalTimeMillis,
            alarmTimeMillis = alarmTimeMillis,
            alarmHour = alarmHour,
            alarmMinute = alarmMinute,
            alertMinutesBefore = alertMinutesBefore,
            formattedArrivalTime = formattedArrivalTime,
            formattedAlarmTime = formattedAlarmTime,
            systemClockSet = systemClockSet,
            inAppAlarmSet = inAppAlarmSet,
            message = message
        )
    }

    /**
     * Cancel an existing alarm scheduled via AlarmManager
     */
    fun cancelScheduledAlarm(context: Context, destination: DestinationSnapshot) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val receiverIntent = Intent(context, ArrivalAlarmReceiver::class.java)
            val requestCode = (destination.name.hashCode() + destination.latitude.toInt()).and(0x7FFFFFFF)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                receiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {
            // Ignore cancel exceptions
        }
    }

    private fun calculateHaversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
