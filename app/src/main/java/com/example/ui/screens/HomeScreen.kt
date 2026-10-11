package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveTrip
import com.example.model.DestinationSnapshot
import com.example.model.DestinationType
import com.example.model.ReliabilityLevel
import com.example.model.SavedPlace
import com.example.model.TransportMode
import com.example.model.TravelHistoryItem
import com.example.model.TripState
import com.example.model.UnitSystem
import com.example.ui.Localization
import com.example.ui.Strings
import com.example.ui.components.EtaDisplayCard
import com.example.ui.components.ReadinessCard
import com.example.ui.theme.AlarmAmber
import com.example.ui.theme.LimitedYellow
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.ReadyGreen
import com.example.ui.theme.TransitCyan
import com.example.ui.theme.TravelIndigoContainer
import com.example.ui.theme.TravelIndigoLight
import com.example.ui.theme.TravelIndigoPrimary

import com.example.model.CountryData
import com.example.data.provider.NominatimGeocodingProvider
import com.example.service.alarm.AlarmScheduleResult
import com.example.ui.components.MainTransitSearchBar
import com.example.ui.components.RealtimeArrivalCountdownTimer

@Composable
fun HomeScreen(
    activeTrip: ActiveTrip?,
    savedPlaces: List<SavedPlace>,
    recentHistory: List<TravelHistoryItem>,
    readiness: ReliabilityLevel,
    unitSystem: UnitSystem,
    language: String,
    countryCode: String = "LK",
    searchWorldwide: Boolean = false,
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    isGpsEnabled: Boolean,
    onStartTripClick: () -> Unit,
    onOpenLiveTripClick: () -> Unit,
    onCountryClick: () -> Unit = {},
    onQuickStartPlace: (SavedPlace) -> Unit,
    onSavedPlacesClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onOpenSimulator: () -> Unit,
    onVoiceLiveClick: () -> Unit = {},
    onGroundedTransitClick: () -> Unit = {},
    geocodingProvider: NominatimGeocodingProvider? = null,
    userLat: Double? = null,
    userLng: Double? = null,
    onDestinationConfirmed: (DestinationSnapshot, Int) -> Unit = { _, _ -> },
    scheduledAlarm: AlarmScheduleResult? = null,
    onDismissScheduledAlarm: () -> Unit = {},
    onStartTrackingScheduledArrival: (AlarmScheduleResult) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val countryObj = remember(countryCode) {
        CountryData.findByCode(countryCode)
    }

    val uniqueSavedPlaces = remember(savedPlaces) {
        savedPlaces
            .distinctBy { it.id }
            .distinctBy { it.name.trim().lowercase() }
    }

    val uniqueHistory = remember(recentHistory) {
        recentHistory
            .distinctBy { it.id }
            .distinctBy { "${it.destinationName.trim().lowercase()}_${it.startTimeMillis / 1000}" }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real-Time Countdown Timer for Active Journey or Transport Arrival Alarm
        if (activeTrip != null && activeTrip.state != TripState.IDLE) {
            item {
                RealtimeArrivalCountdownTimer(
                    trip = activeTrip,
                    unitSystem = unitSystem,
                    language = language,
                    onOpenLiveTripClick = onOpenLiveTripClick
                )
            }
        } else if (scheduledAlarm != null) {
            item {
                RealtimeArrivalCountdownTimer(
                    trip = null,
                    scheduledAlarm = scheduledAlarm,
                    unitSystem = unitSystem,
                    language = language,
                    onDismissScheduledAlarm = onDismissScheduledAlarm,
                    onStartTrackingScheduledArrival = {
                        onStartTrackingScheduledArrival(scheduledAlarm)
                    }
                )
            }
        }

        // Dedicated Public Transport Routes & Destination Search Bar
        item {
            MainTransitSearchBar(
                geocodingProvider = geocodingProvider,
                userLat = userLat,
                userLng = userLng,
                countryCode = countryCode,
                searchWorldwide = searchWorldwide,
                savedPlaces = uniqueSavedPlaces,
                unitSystem = unitSystem,
                language = language,
                onDestinationSelected = { destination, alertMinutes ->
                    onDestinationConfirmed(destination, alertMinutes)
                },
                onOpenFullPlanner = { _ ->
                    onStartTripClick()
                },
                onVoiceLiveClick = onVoiceLiveClick,
                onGroundedTransitClick = onGroundedTransitClick
            )
        }

        // Hero Quick Action: Start New Journey
        item {
            HeroStartTripCard(
                language = language,
                onStartTripClick = onStartTripClick
            )
        }

        // AI Travel Intelligence: Voice Conversations & Grounded Transit
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Voice Button (Gemini 3.8 Live)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NightCardSurface),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onVoiceLiveClick() }
                        .border(
                            width = 1.dp,
                            color = TransitCyan.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("home_live_voice_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TransitCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = TransitCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Live Voice AI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Gemini 3.8 Live conversation",
                            style = MaterialTheme.typography.labelSmall,
                            color = TransitCyan,
                            fontSize = 11.sp
                        )
                    }
                }

                // AI Transit Grounding (Google Maps & Search)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NightCardSurface),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onGroundedTransitClick() }
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("home_grounded_transit_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "AI Transit Grounding",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Google Maps & Search data",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Active Country & Transit Hub Pill
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onCountryClick() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (searchWorldwide) "🌍" else countryObj.flagEmoji,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (searchWorldwide) "Worldwide Transit Search Active"
                                   else "${countryObj.name} • ${countryObj.defaultCity}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (searchWorldwide) "Searching stations across all countries"
                                   else "Hubs: ${countryObj.sampleHubs.take(2).joinToString(", ")} (Tap to change)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Travel Readiness Status Card
        item {
            ReadinessCard(
                readiness = readiness,
                language = language,
                hasLocationPermission = hasLocationPermission,
                hasNotificationPermission = hasNotificationPermission,
                isGpsEnabled = isGpsEnabled
            )
        }

        // Saved Places Quick Shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Localization.get(Strings.SAVED_PLACES, language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onSavedPlacesClick() }
                )
            }
        }

        if (uniqueSavedPlaces.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NightCardSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No saved places yet. Add your home or workplace for 1-tap trips.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uniqueSavedPlaces.take(3), key = { it.id }) { place ->
                SavedPlaceItemCard(
                    place = place,
                    onQuickStart = { onQuickStartPlace(place) }
                )
            }
        }

        // Recent Trips Header
        if (uniqueHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Localization.get(Strings.TRAVEL_HISTORY, language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onHistoryClick() }
                    )
                }
            }

            items(uniqueHistory.take(2), key = { it.id }) { historyItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NightCardSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = historyItem.destinationName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${historyItem.transportMode.label} • ${historyItem.status.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = historyItem.status.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (historyItem.status == com.example.model.TripCompletionStatus.COMPLETED) ReadyGreen else LimitedYellow
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroStartTripCard(
    language: String,
    onStartTripClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NightCardBorder, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = NightCardSurface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TravelIndigoContainer.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sleep Peacefully",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dynamic arrival calculations wake you exactly before your stop, regardless of delays or traffic.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onStartTripClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_trip_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TravelIndigoLight,
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Localization.get(Strings.START_TRIP, language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveTripHomeBanner(
    trip: ActiveTrip,
    unitSystem: UnitSystem,
    language: String,
    onOpenLiveTripClick: () -> Unit
) {
    RealtimeArrivalCountdownTimer(
        trip = trip,
        unitSystem = unitSystem,
        language = language,
        onOpenLiveTripClick = onOpenLiveTripClick
    )
}

@Composable
private fun SavedPlaceItemCard(
    place: SavedPlace,
    onQuickStart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onQuickStart() },
        colors = CardDefaults.cardColors(containerColor = NightCardSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                val icon = when (place.type) {
                    DestinationType.HOME -> Icons.Default.Home
                    DestinationType.WORKPLACE -> Icons.Default.Work
                    DestinationType.RAILWAY_STATION -> Icons.Default.Train
                    else -> Icons.Default.Place
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = TransitCyan, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${place.preferredTransportMode.label} • ${place.defaultAlertMinutes}m alert",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Go",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
