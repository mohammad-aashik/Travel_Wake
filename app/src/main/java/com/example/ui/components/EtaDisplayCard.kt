package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveTrip
import com.example.model.AlarmStage
import com.example.model.ETAConfidence
import com.example.model.FreshnessState
import com.example.model.MovementState
import com.example.model.UnitSystem
import com.example.ui.Localization
import com.example.ui.Strings
import com.example.ui.theme.AlarmAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LimitedYellow
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.ReadyGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EtaDisplayCard(
    trip: ActiveTrip,
    unitSystem: UnitSystem,
    language: String,
    modifier: Modifier = Modifier
) {
    val distanceFormatted = if (unitSystem == UnitSystem.METRIC) {
        if (trip.remainingDistanceMeters >= 1000) {
            "%.1f km".format(trip.remainingDistanceMeters / 1000.0)
        } else {
            "${trip.remainingDistanceMeters.toInt()} m"
        }
    } else {
        val miles = trip.remainingDistanceMeters * 0.000621371
        if (miles >= 0.2) {
            "%.1f mi".format(miles)
        } else {
            "${(trip.remainingDistanceMeters * 3.28084).toInt()} ft"
        }
    }

    val etaTimeString = trip.dynamicEtaMillis?.let {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it))
    } ?: "--:--"

    val confidenceColor = when (trip.etaConfidence) {
        ETAConfidence.HIGH -> ReadyGreen
        ETAConfidence.MEDIUM -> LimitedYellow
        ETAConfidence.LOW -> AlertRed
        ETAConfidence.UNAVAILABLE -> Color.Gray
    }

    val freshnessLabel = when {
        trip.state == com.example.model.TripState.PAUSED -> "PAUSED"
        trip.isWrongDirection -> "WRONG DIRECTION"
        trip.isOvershoot -> "OVERSHOOT"
        else -> "LIVE • ${trip.etaSource.label}"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NightCardBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = NightCardSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Source and Freshness Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(confidenceColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = freshnessLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = confidenceColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Confidence pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = trip.etaConfidence.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Dynamic ETA & Minutes Left
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = Localization.get(Strings.DYNAMIC_ETA, language),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = etaTimeString,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Localization.get(Strings.REMAINING_TIME, language),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val minutesText = trip.remainingMinutes?.let { "$it min" } ?: "--"
                    Text(
                        text = minutesText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-metrics: Remaining distance, Speed, Alert Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance
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
                            text = distanceFormatted,
                            style = MaterialTheme.typography.bodyMedium,
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
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Speed",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${trip.currentSpeedKmh.toInt()} km/h",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Alert setting
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = AlarmAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Alert At",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${trip.alertMinutesBefore} min",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
