package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.DepartureBoard
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveTrip
import com.example.model.AlarmStage
import com.example.model.TransportMode
import com.example.model.TripState
import com.example.model.UnitSystem
import com.example.service.alarm.AlarmScheduleResult
import com.example.service.alarm.TransportAlarmScheduler
import com.example.ui.Localization
import com.example.ui.Strings
import com.example.ui.theme.AlarmAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.ReadyGreen
import com.example.ui.theme.TransitCyan
import com.example.ui.theme.TravelIndigoLight
import com.example.ui.theme.TravelIndigoPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Approach progression stages as transport moves closer to destination
 */
enum class TransportApproachStage(
    val title: String,
    val subtitle: String,
    val color: Color
) {
    IN_TRANSIT(
        title = "IN TRANSIT",
        subtitle = "On route to destination",
        color = TravelIndigoLight
    ),
    APPROACHING(
        title = "APPROACHING DESTINATION",
        subtitle = "Vehicle nearing target stop",
        color = TransitCyan
    ),
    WAKE_ALERT_ZONE(
        title = "WAKE ALERT ZONE",
        subtitle = "Wake alarm scheduled to sound",
        color = AlarmAmber
    ),
    ARRIVING_NOW(
        title = "ARRIVING NOW",
        subtitle = "Prepare to disembark",
        color = ReadyGreen
    )
}

/**
 * Formatted countdown state broken down by unit
 */
data class CountdownDisplayState(
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val formatted: String,
    val totalRemainingSeconds: Long,
    val isArrived: Boolean
)

object CountdownTimerUtils {

    /**
     * Calculates remaining countdown time state given a target timestamp
     */
    fun calculateCountdown(targetMillis: Long, currentMillis: Long): CountdownDisplayState {
        val remainingMillis = (targetMillis - currentMillis).coerceAtLeast(0L)
        val totalSecs = remainingMillis / 1000L
        val hours = totalSecs / 3600L
        val minutes = (totalSecs % 3600L) / 60L
        val seconds = totalSecs % 60L

        val formatted = if (totalSecs <= 0L) {
            "00:00"
        } else if (hours > 0L) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

        return CountdownDisplayState(
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            formatted = formatted,
            totalRemainingSeconds = totalSecs,
            isArrived = totalSecs <= 0L
        )
    }

    /**
     * Determines approach stage based on distance, time remaining, and alert threshold
     */
    fun determineApproachStage(
        remainingDistanceMeters: Double,
        remainingSeconds: Long,
        radiusMeters: Float,
        alertMinutesBefore: Int,
        alarmStage: AlarmStage
    ): TransportApproachStage {
        val alertThresholdSeconds = (alertMinutesBefore * 60L).coerceAtLeast(60L)
        val effectiveRadius = radiusMeters.toDouble()

        return when {
            // Arriving inside destination radius or within final 75 seconds
            remainingDistanceMeters <= effectiveRadius * 1.4 || (remainingSeconds in 1..75) -> {
                TransportApproachStage.ARRIVING_NOW
            }
            // In wake alarm territory
            alarmStage != AlarmStage.NO_ALARM || remainingSeconds <= alertThresholdSeconds -> {
                TransportApproachStage.WAKE_ALERT_ZONE
            }
            // Approaching within 4.5km or within 12 minutes
            remainingDistanceMeters <= 4500.0 || remainingSeconds <= (12 * 60L) -> {
                TransportApproachStage.APPROACHING
            }
            else -> {
                TransportApproachStage.IN_TRANSIT
            }
        }
    }
}

/**
 * Real-Time Countdown Timer Component for the Main Screen.
 * Updates dynamically every second as the transport approaches the destination,
 * showing live countdown digits, arrival time, approach stage, and route progress.
 */
@Composable
fun RealtimeArrivalCountdownTimer(
    trip: ActiveTrip?,
    scheduledAlarm: AlarmScheduleResult? = null,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    language: String = "en",
    onOpenLiveTripClick: () -> Unit = {},
    onDismissScheduledAlarm: () -> Unit = {},
    onStartTrackingScheduledArrival: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (trip == null && scheduledAlarm == null) return

    // 1-second live ticker
    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(trip?.tripId, scheduledAlarm?.destinationName) {
        while (isActive) {
            delay(1000L)
            currentMillis = System.currentTimeMillis()
        }
    }

    // Resolve target arrival timestamp
    val targetArrivalMillis = remember(trip, scheduledAlarm, currentMillis) {
        if (trip != null) {
            if (trip.dynamicEtaMillis != null && trip.dynamicEtaMillis > currentMillis - 60_000L) {
                trip.dynamicEtaMillis
            } else if (trip.remainingMinutes != null && trip.remainingMinutes > 0) {
                currentMillis + (trip.remainingMinutes * 60_000L)
            } else {
                // Estimate via kinematics fallback
                val estimate = TransportAlarmScheduler.estimateArrivalTime(
                    destination = trip.destination,
                    userLat = trip.currentLat,
                    userLng = trip.currentLng
                )
                estimate.arrivalTimeMillis
            }
        } else {
            scheduledAlarm?.arrivalTimeMillis ?: (currentMillis + 15 * 60_000L)
        }
    }

    // Live countdown state
    val countdown = CountdownTimerUtils.calculateCountdown(
        targetMillis = targetArrivalMillis,
        currentMillis = currentMillis
    )

    // Destination snapshot & details
    val destinationName = trip?.destination?.name ?: scheduledAlarm?.destinationName ?: "Destination"
    val routeName = trip?.destination?.routeName?.takeIf { it.isNotBlank() && it != "Direct / Default Route" }
        ?: scheduledAlarm?.routeName?.takeIf { it.isNotBlank() && it != "Direct / Default Route" }
        ?: "Public Transit Corridor"
    val transportMode = trip?.destination?.transportMode ?: TransportMode.BUS
    val alertMinutesBefore = trip?.alertMinutesBefore ?: scheduledAlarm?.alertMinutesBefore ?: 7
    val radiusMeters = trip?.destination?.radiusMeters ?: 250f
    val remainingDistanceMeters = trip?.remainingDistanceMeters ?: 6500.0

    // Approach Stage
    val approachStage = CountdownTimerUtils.determineApproachStage(
        remainingDistanceMeters = remainingDistanceMeters,
        remainingSeconds = countdown.totalRemainingSeconds,
        radiusMeters = radiusMeters,
        alertMinutesBefore = alertMinutesBefore,
        alarmStage = trip?.alarmStage ?: AlarmStage.NO_ALARM
    )

    // Formatted Arrival Clock Time
    val formattedArrivalTime = remember(targetArrivalMillis) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(targetArrivalMillis))
    }

    // Alarm countdown
    val alarmTargetMillis = targetArrivalMillis - (alertMinutesBefore * 60 * 1000L)
    val alarmCountdown = CountdownTimerUtils.calculateCountdown(
        targetMillis = alarmTargetMillis,
        currentMillis = currentMillis
    )

    // Journey completion progress
    val progress = remember(countdown.totalRemainingSeconds, remainingDistanceMeters, trip?.startTimeMillis) {
        if (trip != null) {
            val elapsedSecs = ((currentMillis - trip.startTimeMillis) / 1000L).coerceAtLeast(0L)
            val totalSecs = (elapsedSecs + countdown.totalRemainingSeconds).coerceAtLeast(60L)
            (elapsedSecs.toFloat() / totalSecs.toFloat()).coerceIn(0.06f, 0.98f)
        } else {
            0.45f
        }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "countdown_progress_anim"
    )

    // Beacon pulse animation for approaching & arriving stages
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val stageColor by animateColorAsState(
        targetValue = approachStage.color,
        animationSpec = tween(durationMillis = 400),
        label = "stage_color"
    )

    // Formatted remaining distance
    val formattedDistance = if (unitSystem == UnitSystem.METRIC) {
        if (remainingDistanceMeters >= 1000.0) {
            String.format(Locale.US, "%.1f km", remainingDistanceMeters / 1000.0)
        } else {
            "${remainingDistanceMeters.toInt()} m"
        }
    } else {
        val miles = remainingDistanceMeters * 0.000621371
        if (miles >= 0.2) {
            String.format(Locale.US, "%.1f mi", miles)
        } else {
            "${(remainingDistanceMeters * 3.28084).toInt()} ft"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        stageColor.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable { onOpenLiveTripClick() }
            .testTag("realtime_countdown_timer_card"),
        colors = CardDefaults.cardColors(containerColor = NightCardSurface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Approach Status Badge with Pulsing Beacon & Transit Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Approach Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(stageColor.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("countdown_approach_badge")
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(if (approachStage == TransportApproachStage.ARRIVING_NOW || approachStage == TransportApproachStage.WAKE_ALERT_ZONE) pulseScale else 1.0f)
                            .clip(CircleShape)
                            .background(stageColor)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = approachStage.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = stageColor,
                        letterSpacing = 0.6.sp
                    )
                }

                // Mode Icon & Route Label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = getTransportModeIcon(transportMode),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = routeName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Destination Name
            Text(
                text = destinationName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Hero: Digital Real-Time Countdown Timer & ETA Clock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Countdown Digits
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = stageColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (countdown.isArrived) "ARRIVED" else "TIME TO DESTINATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = countdown.formatted,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = if (countdown.isArrived) ReadyGreen else stageColor,
                        modifier = Modifier.testTag("countdown_timer_digits")
                    )
                }

                // Dynamic Arrival Clock Time
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "EXPECTED AT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedArrivalTime,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = approachStage.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = stageColor,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Journey Progress Line with Moving Transit Icon
            Column(modifier = Modifier.fillMaxWidth().testTag("countdown_progress_bar")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Journey Approach Progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(animatedProgress * 100).toInt()}% completed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = stageColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = stageColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-metrics Bar: Remaining Distance, Transit Speed, Phone Wake Alarm
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance to destination
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Distance",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedDistance,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Speed
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = TransitCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Speed",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val speedText = if (trip != null && trip.currentSpeedKmh > 1.0f) {
                            "${trip.currentSpeedKmh.toInt()} km/h"
                        } else {
                            "Transit Avg"
                        }
                        Text(
                            text = speedText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Wake Alarm Countdown
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("countdown_alarm_time_text")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = AlarmAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Alarm (${alertMinutesBefore}m prior)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val alarmText = if (alarmCountdown.isArrived) {
                            "ALARM READY"
                        } else {
                            "in ${alarmCountdown.formatted}"
                        }
                        Text(
                            text = alarmText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (alarmCountdown.isArrived) AlarmAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Open Live Map / Radar or Start Tracking
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (trip != null) {
                    Button(
                        onClick = onOpenLiveTripClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("countdown_open_live_trip_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DepartureBoard,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "View Live Radar & Transit",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else if (scheduledAlarm != null) {
                    OutlinedButton(
                        onClick = onDismissScheduledAlarm,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.7f)
                    ) {
                        Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onStartTrackingScheduledArrival,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("countdown_start_scheduled_tracking_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start Live GPS",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

private fun getTransportModeIcon(mode: TransportMode): ImageVector {
    return when (mode) {
        TransportMode.BUS -> Icons.Default.DirectionsBus
        TransportMode.TRAIN -> Icons.Default.Train
        TransportMode.METRO -> Icons.Default.Subway
        TransportMode.TRAM -> Icons.Default.Tram
        TransportMode.FERRY -> Icons.Default.DirectionsBoat
        TransportMode.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
        TransportMode.CAR_TAXI -> Icons.Default.LocalTaxi
        else -> Icons.Default.Commute
    }
}
