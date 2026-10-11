package com.example.ui.screens

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveTrip
import com.example.ui.theme.AlarmAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LimitedYellow
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.ReadyGreen
import com.example.ui.theme.TransitCyan

@Composable
fun SimulationDrawer(
    activeTrip: ActiveTrip?,
    onClose: () -> Unit,
    onSimulateLocationUpdate: (Location) -> Unit,
    onTriggerSimulatedAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var simSpeedKmh by remember { mutableFloatStateOf(45f) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NightCardSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TransitCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Science, contentDescription = null, tint = TransitCyan)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Transit Simulation Lab",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Test real-world movement & edge cases safely",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onClose, modifier = Modifier.testTag("close_simulator_button")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        if (activeTrip == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "No active journey is currently running. Start a trip first to test movement, delays, and arrival triggers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Speed Slider
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NightCardBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Simulated Vehicle Speed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = "${simSpeedKmh.toInt()} km/h", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TransitCyan)
                        }
                        Slider(
                            value = simSpeedKmh,
                            onValueChange = { simSpeedKmh = it },
                            valueRange = 0f..120f
                        )
                    }
                }
            }

            // Scenario 1: Step Closer to Destination (Moving Forward)
            item {
                Button(
                    onClick = {
                        val dest = activeTrip.destination
                        val currentLat = activeTrip.currentLat ?: (dest.latitude - 0.05)
                        val currentLng = activeTrip.currentLng ?: (dest.longitude - 0.05)

                        // Move ~25% closer towards destination
                        val stepLat = currentLat + (dest.latitude - currentLat) * 0.25
                        val stepLng = currentLng + (dest.longitude - currentLng) * 0.25

                        val loc = Location("simulation").apply {
                            latitude = stepLat
                            longitude = stepLng
                            speed = simSpeedKmh / 3.6f
                            accuracy = 15f
                            time = System.currentTimeMillis()
                        }
                        onSimulateLocationUpdate(loc)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.FastForward, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Advance Towards Destination (+25%)")
                }
            }

            // Scenario 2: Traffic Jam / Station Stop (Speed = 0)
            item {
                OutlinedButton(
                    onClick = {
                        val dest = activeTrip.destination
                        val loc = Location("simulation").apply {
                            latitude = activeTrip.currentLat ?: (dest.latitude - 0.02)
                            longitude = activeTrip.currentLng ?: (dest.longitude - 0.02)
                            speed = 0f
                            accuracy = 18f
                            time = System.currentTimeMillis()
                        }
                        onSimulateLocationUpdate(loc)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.StopCircle, contentDescription = null, tint = LimitedYellow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulate Traffic Jam / Station Stop (0 km/h)")
                }
            }

            // Scenario 3: Destination Overshoot (Passes Destination)
            item {
                OutlinedButton(
                    onClick = {
                        val dest = activeTrip.destination
                        // Position slightly past destination
                        val overshootLat = dest.latitude + 0.015
                        val overshootLng = dest.longitude + 0.015
                        val loc = Location("simulation").apply {
                            latitude = overshootLat
                            longitude = overshootLng
                            speed = 35f / 3.6f
                            accuracy = 20f
                            time = System.currentTimeMillis()
                        }
                        onSimulateLocationUpdate(loc)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulate Destination Overshoot")
                }
            }

            // Scenario 4: Wrong Direction (Moving Away)
            item {
                OutlinedButton(
                    onClick = {
                        val dest = activeTrip.destination
                        val currentLat = activeTrip.currentLat ?: dest.latitude
                        val currentLng = activeTrip.currentLng ?: dest.longitude
                        // Move 3km directly away
                        val loc = Location("simulation").apply {
                            latitude = currentLat - 0.03
                            longitude = currentLng - 0.03
                            speed = 40f / 3.6f
                            accuracy = 25f
                            time = System.currentTimeMillis()
                        }
                        onSimulateLocationUpdate(loc)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.TrendingDown, contentDescription = null, tint = LimitedYellow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulate Wrong Direction (Moving Away)")
                }
            }

            // Scenario 5: Inside Arrival Geofence (Immediate Arrival)
            item {
                Button(
                    onClick = {
                        val dest = activeTrip.destination
                        // Coords right at the destination center
                        val loc = Location("simulation").apply {
                            latitude = dest.latitude
                            longitude = dest.longitude
                            speed = 2f / 3.6f
                            accuracy = 10f
                            time = System.currentTimeMillis()
                        }
                        // Send twice to simulate multiple verified readings
                        onSimulateLocationUpdate(loc)
                        onSimulateLocationUpdate(loc)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ReadyGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulate Arrival Inside Radius (Trigger Wake)", fontWeight = FontWeight.Bold)
                }
            }

            // Scenario 6: Test Alarm Trigger Directly
            item {
                OutlinedButton(
                    onClick = onTriggerSimulatedAlarm,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlarmAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fire Main Alarm Modal Directly", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
