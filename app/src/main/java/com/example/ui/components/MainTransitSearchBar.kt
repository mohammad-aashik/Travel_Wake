package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DepartureBoard
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.provider.GeocodeResult
import com.example.data.provider.NominatimGeocodingProvider
import com.example.data.provider.TransitRouteCatalog
import com.example.service.alarm.TransportAlarmScheduler
import com.example.model.DestinationSnapshot
import com.example.model.DestinationType
import com.example.model.SavedPlace
import com.example.model.TransitRoute
import com.example.model.TransitRouteStop
import com.example.model.TransportMode
import com.example.model.UnitSystem
import com.example.ui.Localization
import com.example.ui.Strings
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.ReadyGreen
import com.example.ui.theme.TransitCyan
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class SearchFilterTab(val title: String) {
    ALL("All"),
    ROUTES("Routes"),
    STATIONS("Stops & Stations"),
    SAVED("Saved Places")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTransitSearchBar(
    geocodingProvider: NominatimGeocodingProvider?,
    userLat: Double?,
    userLng: Double?,
    countryCode: String,
    searchWorldwide: Boolean,
    savedPlaces: List<SavedPlace>,
    unitSystem: UnitSystem,
    language: String,
    onDestinationSelected: (DestinationSnapshot, alertMinutes: Int) -> Unit,
    onOpenFullPlanner: (initialQuery: String) -> Unit,
    onVoiceLiveClick: () -> Unit,
    onGroundedTransitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(SearchFilterTab.ALL) }

    var geocodeResults by remember { mutableStateOf<List<GeocodeResult>>(emptyList()) }
    var routeResults by remember { mutableStateOf<List<TransitRoute>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Expanded route ID for stops breakdown
    var expandedRouteId by remember { mutableStateOf<String?>(null) }

    // Quick journey confirmation sheet
    var pendingDestination by remember { mutableStateOf<DestinationSnapshot?>(null) }
    var alertMinutesSelected by remember { mutableIntStateOf(7) }

    // Load initial routes & suggestions
    LaunchedEffect(countryCode, searchWorldwide) {
        routeResults = TransitRouteCatalog.searchRoutes(
            query = "",
            preferredCountryCode = countryCode,
            searchWorldwide = searchWorldwide
        )
    }

    // Trigger debounced search when user inputs text
    fun performSearch(queryText: String) {
        val trimmed = queryText.trim()
        searchJob?.cancel()

        // Synchronous route search
        routeResults = TransitRouteCatalog.searchRoutes(
            query = trimmed,
            preferredCountryCode = countryCode,
            searchWorldwide = searchWorldwide
        )

        // Asynchronous geocoding provider search
        if (geocodingProvider != null) {
            searchJob = coroutineScope.launch {
                delay(250)
                isSearching = true
                try {
                    val rawResults = geocodingProvider.searchPlaces(
                        query = trimmed,
                        userLat = userLat,
                        userLng = userLng,
                        preferredCountryCode = if (searchWorldwide) null else countryCode,
                        searchWorldwide = searchWorldwide
                    )
                    geocodeResults = rawResults
                        .distinctBy { it.placeId }
                        .distinctBy { "${it.name.trim().lowercase()}_${it.city.trim().lowercase()}" }
                        .distinctBy { "${(it.latitude * 500).toInt()}_${(it.longitude * 500).toInt()}" }
                } catch (_: Exception) {
                    geocodeResults = emptyList()
                } finally {
                    isSearching = false
                }
            }
        }
    }

    // Filter saved places matching query
    val matchingSavedPlaces = remember(searchQuery, savedPlaces) {
        if (searchQuery.isBlank()) {
            savedPlaces.take(3)
        } else {
            val q = searchQuery.trim().lowercase()
            savedPlaces.filter {
                it.name.lowercase().contains(q) ||
                it.address.lowercase().contains(q)
            }
        }
    }

    val isResultsExpanded = isFocused || searchQuery.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("main_transit_search_container")
    ) {
        // Main Search Bar Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = NightCardSurface
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else NightCardBorder
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        performSearch(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .testTag("main_search_bar_input"),
                    placeholder = {
                        Text(
                            text = "Search destination or route (e.g. 138, Train, Fort)...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Transit",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        performSearch("")
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("main_search_clear_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(end = 6.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Voice AI quick mic button
                            IconButton(
                                onClick = onVoiceLiveClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("main_search_mic_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Search & Gemini Live",
                                    tint = TransitCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            // If user pressed search on keyboard, configure top result if available
                            if (routeResults.isNotEmpty() && routeResults.first().majorStops.isNotEmpty()) {
                                val topStop = routeResults.first().majorStops.first()
                                pendingDestination = DestinationSnapshot(
                                    name = topStop.name,
                                    latitude = topStop.latitude,
                                    longitude = topStop.longitude,
                                    type = topStop.type,
                                    radiusMeters = 200f,
                                    country = routeResults.first().countryCode,
                                    city = routeResults.first().cityName,
                                    fullAddress = topStop.fullAddress,
                                    transportMode = routeResults.first().mode,
                                    routeName = routeResults.first().name
                                )
                            } else if (geocodeResults.isNotEmpty()) {
                                val top = geocodeResults.first()
                                pendingDestination = DestinationSnapshot(
                                    name = top.name,
                                    latitude = top.latitude,
                                    longitude = top.longitude,
                                    type = top.type,
                                    radiusMeters = 200f,
                                    country = top.country,
                                    city = top.city,
                                    fullAddress = top.fullDisplayName,
                                    transportMode = TransportMode.BUS,
                                    routeName = "Direct Corridor"
                                )
                            }
                        }
                    )
                )

                // Quick Filter Tabs (When active or typing)
                AnimatedVisibility(
                    visible = isResultsExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SearchFilterTab.values().forEach { tab ->
                                val isSelected = selectedFilter == tab
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFilter = tab },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        when (tab) {
                                            SearchFilterTab.ALL -> Icon(Icons.Default.Public, null, modifier = Modifier.size(14.dp))
                                            SearchFilterTab.ROUTES -> Icon(Icons.Default.DirectionsBus, null, modifier = Modifier.size(14.dp))
                                            SearchFilterTab.STATIONS -> Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp))
                                            SearchFilterTab.SAVED -> Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp))
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search Results & Route Browser Section
        AnimatedVisibility(
            visible = isResultsExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NightCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NightCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("main_search_results_container")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Header with Results Count & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Transit Routes & Popular Destinations"
                                   else "Search Results for \"$searchQuery\"",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                isFocused = false
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Close suggestions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // SECTION 1: PUBLIC TRANSPORT ROUTES
                    if (selectedFilter == SearchFilterTab.ALL || selectedFilter == SearchFilterTab.ROUTES) {
                        if (routeResults.isNotEmpty()) {
                            Text(
                                text = "PUBLIC TRANSPORT ROUTES (${routeResults.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TransitCyan,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            routeResults.take(4).forEach { route ->
                                RouteResultCard(
                                    route = route,
                                    isExpanded = expandedRouteId == route.id,
                                    onToggleExpand = {
                                        expandedRouteId = if (expandedRouteId == route.id) null else route.id
                                    },
                                    onSelectStop = { stop ->
                                        pendingDestination = DestinationSnapshot(
                                            name = stop.name,
                                            latitude = stop.latitude,
                                            longitude = stop.longitude,
                                            type = stop.type,
                                            radiusMeters = 200f,
                                            country = route.countryCode,
                                            city = route.cityName,
                                            fullAddress = stop.fullAddress.ifBlank { "${stop.name}, ${route.cityName}" },
                                            transportMode = route.mode,
                                            routeName = route.name
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // SECTION 2: DESTINATIONS & STOPS (Geocoded)
                    if (selectedFilter == SearchFilterTab.ALL || selectedFilter == SearchFilterTab.STATIONS) {
                        if (geocodeResults.isNotEmpty()) {
                            Text(
                                text = "STOPS & DESTINATIONS (${geocodeResults.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )

                            geocodeResults.take(4).forEach { place ->
                                PlaceResultCard(
                                    place = place,
                                    unitSystem = unitSystem,
                                    onSelectPlace = {
                                        pendingDestination = DestinationSnapshot(
                                            name = place.name,
                                            latitude = place.latitude,
                                            longitude = place.longitude,
                                            type = place.type,
                                            radiusMeters = 200f,
                                            country = place.country,
                                            city = place.city,
                                            fullAddress = place.fullDisplayName,
                                            transportMode = inferTransportMode(place.type),
                                            routeName = "Direct Route"
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }

                    // SECTION 3: SAVED PLACES
                    if (selectedFilter == SearchFilterTab.ALL || selectedFilter == SearchFilterTab.SAVED) {
                        if (matchingSavedPlaces.isNotEmpty()) {
                            Text(
                                text = "SAVED PLACES (${matchingSavedPlaces.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ReadyGreen,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )

                            matchingSavedPlaces.forEach { savedPlace ->
                                SavedPlaceResultCard(
                                    savedPlace = savedPlace,
                                    onSelectPlace = {
                                        pendingDestination = DestinationSnapshot(
                                            name = savedPlace.name,
                                            latitude = savedPlace.latitude,
                                            longitude = savedPlace.longitude,
                                            type = savedPlace.type,
                                            radiusMeters = savedPlace.radiusMeters,
                                            country = "Global",
                                            city = "Saved Place",
                                            fullAddress = savedPlace.address,
                                            transportMode = savedPlace.preferredTransportMode,
                                            routeName = "Saved Route"
                                        )
                                        alertMinutesSelected = savedPlace.defaultAlertMinutes
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }

                    // Bottom Action Bar: Open Full Journey Planner or Google Maps Grounding
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = NightCardBorder
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                onOpenFullPlanner(searchQuery)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("main_search_open_full_planner"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Full Planner", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onGroundedTransitClick()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("main_search_ai_grounding_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Maps AI", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Quick Confirm & Start Journey
    pendingDestination?.let { destination ->
        val context = LocalContext.current
        var alarmResultMsg by remember { mutableStateOf<String?>(null) }
        val arrivalEstimate = remember(destination, userLat, userLng) {
            TransportAlarmScheduler.estimateArrivalTime(
                destination = destination,
                userLat = userLat,
                userLng = userLng
            )
        }
        val targetAlarmMillis = remember(arrivalEstimate, alertMinutesSelected) {
            (arrivalEstimate.arrivalTimeMillis - (alertMinutesSelected * 60 * 1000L))
                .coerceAtLeast(System.currentTimeMillis() + 60 * 1000L)
        }
        val formattedTargetAlarm = remember(targetAlarmMillis) {
            java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(targetAlarmMillis))
        }

        ModalBottomSheet(
            onDismissRequest = {
                pendingDestination = null
                alarmResultMsg = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = NightCardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("quick_journey_confirm_sheet")
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getTransportModeIcon(destination.transportMode),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = destination.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = destination.fullAddress.ifBlank { "${destination.city}, ${destination.country}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Estimated Arrival Time Card (Identified from transport search)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("destination_arrival_estimate_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ESTIMATED ARRIVAL TIME",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = arrivalEstimate.formattedArrivalTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Travel Time: ${arrivalEstimate.formattedDuration} (${String.format(java.util.Locale.US, "%.1f", arrivalEstimate.distanceKm)} km)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Alarm: $formattedTargetAlarm",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TransitCyan
                            )
                        }
                    }
                }

                if (alarmResultMsg != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = ReadyGreen.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ReadyGreen.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("alarm_scheduled_feedback_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = ReadyGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = alarmResultMsg ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Wake alarm lead time selector
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = TransitCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Wake Me Before Arrival",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "$alertMinutesSelected mins ahead",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = TransitCyan
                            )
                        }

                        Slider(
                            value = alertMinutesSelected.toFloat(),
                            onValueChange = { alertMinutesSelected = it.roundToInt() },
                            valueRange = 2f..20f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = TransitCyan,
                                activeTrackColor = TransitCyan
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Button: Set Phone Alarm based on Arrival Time
                Button(
                    onClick = {
                        val result = TransportAlarmScheduler.setPhoneAlarmForArrival(
                            context = context,
                            destination = destination,
                            alertMinutesBefore = alertMinutesSelected,
                            userLat = userLat,
                            userLng = userLng,
                            openSystemClockApp = true
                        )
                        alarmResultMsg = result.message
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("set_phone_alarm_button")
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set Phone Alarm ($formattedTargetAlarm)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons: Dismiss & Start Wake Journey
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            pendingDestination = null
                            alarmResultMsg = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Dismiss")
                    }

                    Button(
                        onClick = {
                            val snap = destination
                            val mins = alertMinutesSelected
                            pendingDestination = null
                            alarmResultMsg = null
                            onDestinationSelected(snap, mins)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ReadyGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("start_quick_journey_confirm_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Wake Journey", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RouteResultCard(
    route: TransitRoute,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectStop: (TransitRouteStop) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, NightCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("route_result_card_${route.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Main Route Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Route Number Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TransitCyan.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = getTransportModeIcon(route.mode),
                            contentDescription = null,
                            tint = TransitCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = route.routeNumber,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TransitCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${route.originTerminal} ➔ ${route.destinationTerminal}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand Route",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Route Details & Stops List (When expanded)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "${route.frequencyDescription} • ${route.operatingHours}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SELECT YOUR DESTINATION STOP ALONG THIS ROUTE:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    route.majorStops.forEach { stop ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectStop(stop) }
                                .padding(vertical = 6.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (stop.isMajorHub) TransitCyan else MaterialTheme.colorScheme.outline)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stop.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (stop.isMajorHub) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (stop.fullAddress.isNotBlank()) {
                                        Text(
                                            text = stop.fullAddress,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ReadyGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Wake Here",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ReadyGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceResultCard(
    place: GeocodeResult,
    unitSystem: UnitSystem,
    onSelectPlace: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectPlace() }
            .testTag("destination_result_card_${place.placeId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getDestinationTypeIcon(place.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = place.fullDisplayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                place.distanceKmFromCurrent?.let { distKm ->
                    val distanceText = if (unitSystem == UnitSystem.METRIC) {
                        "%.1f km".format(distKm)
                    } else {
                        "%.1f mi".format(distKm * 0.621371)
                    }
                    Text(
                        text = distanceText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TransitCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Select",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedPlaceResultCard(
    savedPlace: SavedPlace,
    onSelectPlace: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectPlace() }
            .testTag("saved_result_card_${savedPlace.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ReadyGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = ReadyGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = savedPlace.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = savedPlace.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ReadyGreen)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Start",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
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
        TransportMode.PUBLIC_TRANSIT -> Icons.Default.DepartureBoard
        else -> Icons.Default.Place
    }
}

private fun getDestinationTypeIcon(type: DestinationType): ImageVector {
    return when (type) {
        DestinationType.RAILWAY_STATION -> Icons.Default.Train
        DestinationType.METRO_STATION -> Icons.Default.Subway
        DestinationType.BUS_STAND, DestinationType.BUS_STOP -> Icons.Default.DirectionsBus
        DestinationType.AIRPORT -> Icons.Default.Flight
        DestinationType.FERRY_TERMINAL -> Icons.Default.DirectionsBoat
        else -> Icons.Default.Place
    }
}

private fun inferTransportMode(type: DestinationType): TransportMode {
    return when (type) {
        DestinationType.RAILWAY_STATION -> TransportMode.TRAIN
        DestinationType.METRO_STATION -> TransportMode.METRO
        DestinationType.BUS_STAND, DestinationType.BUS_STOP -> TransportMode.BUS
        DestinationType.FERRY_TERMINAL -> TransportMode.FERRY
        else -> TransportMode.BUS
    }
}
