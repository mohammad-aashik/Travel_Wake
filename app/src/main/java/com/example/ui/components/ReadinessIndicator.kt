package com.example.ui.components

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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReliabilityLevel
import com.example.ui.Localization
import com.example.ui.Strings
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LimitedYellow
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.ReadyGreen

@Composable
fun ReadinessCard(
    readiness: ReliabilityLevel,
    language: String,
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    isGpsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusTitle, statusDesc) = when (readiness) {
        ReliabilityLevel.READY -> Triple(
            ReadyGreen,
            "SYSTEMS READY",
            "GPS and wake engine are active. Arrival alarms will trigger reliably."
        )
        ReliabilityLevel.LIMITED -> Triple(
            LimitedYellow,
            "LIMITED READINESS",
            "Background restrictions or battery saver may impact precision."
        )
        ReliabilityLevel.NOT_RELIABLE -> Triple(
            AlertRed,
            "ACTION REQUIRED",
            "Location or notification permissions needed for wake alarm."
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NightCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = NightCardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Localization.get(Strings.TRAVEL_READINESS, language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusTitle,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-status item indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReadinessPill(
                    icon = Icons.Default.LocationOn,
                    label = "GPS",
                    isOk = hasLocationPermission && isGpsEnabled
                )
                ReadinessPill(
                    icon = Icons.Default.NotificationsActive,
                    label = "Alarm Sound",
                    isOk = hasNotificationPermission
                )
                ReadinessPill(
                    icon = Icons.Default.BatteryChargingFull,
                    label = "Background",
                    isOk = true
                )
                ReadinessPill(
                    icon = Icons.Default.Shield,
                    label = "Engine",
                    isOk = true
                )
            }
        }
    }
}

@Composable
private fun ReadinessPill(
    icon: ImageVector,
    label: String,
    isOk: Boolean
) {
    val color = if (isOk) ReadyGreen else LimitedYellow
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
