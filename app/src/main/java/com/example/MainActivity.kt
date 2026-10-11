package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AlarmStage
import com.example.model.CountryData
import com.example.model.TripState
import com.example.ui.Localization
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.Strings
import com.example.ui.components.AlarmTriggerDialog
import com.example.ui.components.CountrySelectionDialog
import com.example.ui.components.TravelWakeTopBar
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.DestinationSelectionScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveTripScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SavedPlacesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SimulationDrawer
import com.example.ui.screens.TransitGroundingScreen
import com.example.ui.screens.VoiceConversationScreen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Public
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.NightNavy
import com.example.ui.theme.TransitCyan
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

            // Permissions Launcher
            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                val fineLocation = perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                val coarseLocation = perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
                viewModel.setLocationPermissionGranted(fineLocation || coarseLocation)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notif = perms[Manifest.permission.POST_NOTIFICATIONS] ?: false
                    viewModel.setNotificationPermissionGranted(notif)
                }
            }

            LaunchedEffect(Unit) {
                val fineLocationGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                viewModel.setLocationPermissionGranted(fineLocationGranted)

                val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                viewModel.setNotificationPermissionGranted(notifGranted)

                val permissionsToRequest = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionsLauncher.launch(permissionsToRequest.toTypedArray())
            }

            // State Collections
            val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val activeTrip by viewModel.activeTrip.collectAsStateWithLifecycle()
            val savedPlaces by viewModel.savedPlaces.collectAsStateWithLifecycle()
            val travelHistory by viewModel.travelHistory.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val readiness by viewModel.readiness.collectAsStateWithLifecycle()
            val hasLocationPerm by viewModel.hasLocationPermission.collectAsStateWithLifecycle()
            val hasNotifPerm by viewModel.hasNotificationPermission.collectAsStateWithLifecycle()
            val isGpsEnabled by viewModel.isGpsEnabled.collectAsStateWithLifecycle()
            val showSimulator by viewModel.showSimulator.collectAsStateWithLifecycle()
            val testAlarmStage by viewModel.testAlarmTriggered.collectAsStateWithLifecycle()
            val authErrorMessage by viewModel.authErrorMessage.collectAsStateWithLifecycle()
            val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
            val activeTripRestored by viewModel.activeTripRestoredEvent.collectAsStateWithLifecycle()
            val dataPreservationReport by viewModel.dataPreservationReport.collectAsStateWithLifecycle()
            val lastAlarmScheduleResult by viewModel.lastAlarmScheduleResult.collectAsStateWithLifecycle()

            var showCountryPicker by remember { mutableStateOf(false) }

            val language = appSettings.language

            MyApplicationTheme(themeMode = appSettings.themeMode) {
                // Back Button Handling
                BackHandler(enabled = drawerState.isOpen || showSimulator || (currentScreen != Screen.Home && currentScreen != Screen.Onboarding)) {
                    if (drawerState.isOpen) {
                        scope.launch { drawerState.close() }
                    } else if (showSimulator) {
                        viewModel.toggleSimulator(false)
                    } else {
                        viewModel.navigateTo(Screen.Home)
                    }
                }

                // Modal Navigation Drawer (Top-right menu ☰)
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = currentScreen != Screen.Onboarding,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier.width(300.dp),
                            drawerContainerColor = NightCardSurface
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "TW",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = Localization.get(Strings.APP_NAME, language),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Dynamic Transit Wake",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text("Home Journey") },
                                selected = currentScreen == Screen.Home,
                                onClick = {
                                    viewModel.navigateTo(Screen.Home)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            if (activeTrip != null && activeTrip!!.state != TripState.IDLE) {
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Navigation, contentDescription = null, tint = TransitCyan) },
                                    label = { Text(Localization.get(Strings.ACTIVE_TRIP, language)) },
                                    selected = currentScreen == Screen.LiveTrip,
                                    onClick = {
                                        viewModel.navigateTo(Screen.LiveTrip)
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                                label = { Text(Localization.get(Strings.SAVED_PLACES, language)) },
                                selected = currentScreen == Screen.SavedPlaces,
                                onClick = {
                                    viewModel.navigateTo(Screen.SavedPlaces)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.History, contentDescription = null) },
                                label = { Text(Localization.get(Strings.TRAVEL_HISTORY, language)) },
                                selected = currentScreen == Screen.History,
                                onClick = {
                                    viewModel.navigateTo(Screen.History)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                                label = { Text(Localization.get(Strings.ACCOUNT, language)) },
                                selected = currentScreen == Screen.Account,
                                onClick = {
                                    viewModel.navigateTo(Screen.Account)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            NavigationDrawerItem(
                                icon = {
                                    val drawerCountry = remember(appSettings.countryCode) {
                                        CountryData.findByCode(appSettings.countryCode)
                                    }
                                    Text(text = if (appSettings.searchWorldwide) "🌍" else drawerCountry.flagEmoji, fontSize = 18.sp)
                                },
                                label = {
                                    val drawerCountry = remember(appSettings.countryCode) {
                                        CountryData.findByCode(appSettings.countryCode)
                                    }
                                    Text(if (appSettings.searchWorldwide) "Worldwide (${drawerCountry.name})" else drawerCountry.name)
                                },
                                selected = false,
                                onClick = {
                                    showCountryPicker = true
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text(Localization.get(Strings.SETTINGS, language)) },
                                selected = currentScreen == Screen.Settings,
                                onClick = {
                                    viewModel.navigateTo(Screen.Settings)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Science, contentDescription = null, tint = TransitCyan) },
                                label = { Text(Localization.get(Strings.SIMULATION_MODE, language)) },
                                selected = showSimulator,
                                onClick = {
                                    viewModel.toggleSimulator(true)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = TransitCyan) },
                                label = { Text("Live Voice (Gemini 3.8)") },
                                selected = currentScreen == Screen.VoiceLive,
                                onClick = {
                                    viewModel.navigateTo(Screen.VoiceLive)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Public, contentDescription = null, tint = TransitCyan) },
                                label = { Text("AI Transit Grounding") },
                                selected = currentScreen == Screen.GroundedTransit,
                                onClick = {
                                    viewModel.navigateTo(Screen.GroundedTransit)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            if (currentScreen != Screen.Onboarding) {
                                TravelWakeTopBar(
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                    currentLanguage = language
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // Active Screen Switcher
                            when (currentScreen) {
                                Screen.Onboarding -> {
                                    OnboardingScreen(
                                        currentLanguage = language,
                                        onLanguageChange = { newLang ->
                                            viewModel.updateSettings(appSettings.copy(language = newLang))
                                        },
                                        onSetupCompleteGuest = { country ->
                                            viewModel.completeSetupGuest(country)
                                        },
                                        onSetupCompleteRegister = { name, email, pass, phone, country ->
                                            viewModel.completeSetupRegister(name, email, pass, phone, country)
                                        },
                                        onSetupCompleteLogin = { email, pass ->
                                            viewModel.completeSetupLogin(email, pass)
                                        },
                                        authErrorMessage = authErrorMessage,
                                        isAuthLoading = isAuthLoading
                                    )
                                }
                                Screen.Home -> {
                                    HomeScreen(
                                        activeTrip = activeTrip,
                                        savedPlaces = savedPlaces,
                                        recentHistory = travelHistory,
                                        readiness = readiness,
                                        unitSystem = appSettings.unitSystem,
                                        language = language,
                                        countryCode = appSettings.countryCode,
                                        searchWorldwide = appSettings.searchWorldwide,
                                        hasLocationPermission = hasLocationPerm,
                                        hasNotificationPermission = hasNotifPerm,
                                        isGpsEnabled = isGpsEnabled,
                                        onStartTripClick = { viewModel.navigateTo(Screen.DestinationSelect) },
                                        onOpenLiveTripClick = { viewModel.navigateTo(Screen.LiveTrip) },
                                        onCountryClick = { showCountryPicker = true },
                                        onQuickStartPlace = { place ->
                                            val snapshot = com.example.model.DestinationSnapshot(
                                                name = place.name,
                                                latitude = place.latitude,
                                                longitude = place.longitude,
                                                type = place.type,
                                                radiusMeters = place.radiusMeters,
                                                country = "Global",
                                                city = "Transit Hub",
                                                fullAddress = place.address,
                                                transportMode = place.preferredTransportMode,
                                                routeName = "Direct Route"
                                            )
                                            viewModel.startTrip(snapshot, place.defaultAlertMinutes)
                                        },
                                        onSavedPlacesClick = { viewModel.navigateTo(Screen.SavedPlaces) },
                                        onHistoryClick = { viewModel.navigateTo(Screen.History) },
                                        onOpenSimulator = { viewModel.toggleSimulator(true) },
                                        onVoiceLiveClick = { viewModel.navigateTo(Screen.VoiceLive) },
                                        onGroundedTransitClick = { viewModel.navigateTo(Screen.GroundedTransit) },
                                        geocodingProvider = viewModel.geocodingProvider,
                                        userLat = activeTrip?.currentLat,
                                        userLng = activeTrip?.currentLng,
                                        onDestinationConfirmed = { snapshot, alertMinutes ->
                                            viewModel.startTrip(snapshot, alertMinutes)
                                        },
                                        scheduledAlarm = lastAlarmScheduleResult,
                                        onDismissScheduledAlarm = { viewModel.clearAlarmScheduleResult() },
                                        onStartTrackingScheduledArrival = { result ->
                                            val snapshot = com.example.model.DestinationSnapshot(
                                                name = result.destinationName,
                                                latitude = 6.9271,
                                                longitude = 79.8612,
                                                type = com.example.model.DestinationType.RAILWAY_STATION,
                                                radiusMeters = 300f,
                                                country = "Global",
                                                city = "Transit Hub",
                                                fullAddress = result.destinationName,
                                                transportMode = com.example.model.TransportMode.BUS,
                                                routeName = result.routeName
                                            )
                                            viewModel.startTrip(snapshot, result.alertMinutesBefore)
                                        }
                                    )
                                }
                                Screen.DestinationSelect -> {
                                    DestinationSelectionScreen(
                                        geocodingProvider = viewModel.geocodingProvider,
                                        userLat = activeTrip?.currentLat,
                                        userLng = activeTrip?.currentLng,
                                        language = language,
                                        preferredCountryCode = appSettings.countryCode,
                                        searchWorldwide = appSettings.searchWorldwide,
                                        onCountryChange = { viewModel.selectCountry(it) },
                                        onWorldwideToggle = { viewModel.setSearchWorldwide(it) },
                                        onBackClick = { viewModel.navigateTo(Screen.Home) },
                                        onAIGroundingClick = { viewModel.navigateTo(Screen.GroundedTransit) },
                                        onConfirmTrip = { snapshot, alertMins ->
                                            viewModel.startTrip(snapshot, alertMins)
                                        }
                                    )
                                }
                                Screen.LiveTrip -> {
                                    if (activeTrip != null) {
                                        LiveTripScreen(
                                            trip = activeTrip!!,
                                            unitSystem = appSettings.unitSystem,
                                            language = language,
                                            onBackClick = { viewModel.navigateTo(Screen.Home) },
                                            onPauseClick = { viewModel.pauseTrip() },
                                            onResumeClick = { viewModel.resumeTrip() },
                                            onWakeMeNowClick = { viewModel.triggerWakeMeNow() },
                                            onChangeDestinationClick = { viewModel.navigateTo(Screen.DestinationSelect) },
                                            onChangeAlertMinutesClick = { viewModel.updateAlertMinutes(it) },
                                            onEndTripClick = { viewModel.endTrip() }
                                        )
                                    } else {
                                        HomeScreen(
                                            activeTrip = null,
                                            savedPlaces = savedPlaces,
                                            recentHistory = travelHistory,
                                            readiness = readiness,
                                            unitSystem = appSettings.unitSystem,
                                            language = language,
                                            hasLocationPermission = hasLocationPerm,
                                            hasNotificationPermission = hasNotifPerm,
                                            isGpsEnabled = isGpsEnabled,
                                            onStartTripClick = { viewModel.navigateTo(Screen.DestinationSelect) },
                                            onOpenLiveTripClick = { viewModel.navigateTo(Screen.LiveTrip) },
                                            onQuickStartPlace = {},
                                            onSavedPlacesClick = { viewModel.navigateTo(Screen.SavedPlaces) },
                                            onHistoryClick = { viewModel.navigateTo(Screen.History) },
                                            onOpenSimulator = { viewModel.toggleSimulator(true) },
                                            geocodingProvider = viewModel.geocodingProvider,
                                            onDestinationConfirmed = { snapshot, alertMinutes ->
                                                viewModel.startTrip(snapshot, alertMinutes)
                                            },
                                            scheduledAlarm = lastAlarmScheduleResult,
                                            onDismissScheduledAlarm = { viewModel.clearAlarmScheduleResult() }
                                        )
                                    }
                                }
                                Screen.SavedPlaces -> {
                                    SavedPlacesScreen(
                                        savedPlaces = savedPlaces,
                                        language = language,
                                        onBackClick = { viewModel.navigateTo(Screen.Home) },
                                        onAddPlace = { viewModel.savePlace(it) },
                                        onDeletePlace = { viewModel.deletePlace(it) },
                                        onRemoveDuplicates = { viewModel.removeDuplicateItems() },
                                        onSelectPlaceForTrip = { place ->
                                            val snapshot = com.example.model.DestinationSnapshot(
                                                name = place.name,
                                                latitude = place.latitude,
                                                longitude = place.longitude,
                                                type = place.type,
                                                radiusMeters = place.radiusMeters,
                                                country = "Global",
                                                city = "Transit Hub",
                                                fullAddress = place.address,
                                                transportMode = place.preferredTransportMode,
                                                routeName = "Direct Corridor"
                                            )
                                            viewModel.startTrip(snapshot, place.defaultAlertMinutes)
                                        }
                                    )
                                }
                                Screen.History -> {
                                    HistoryScreen(
                                        history = travelHistory,
                                        language = language,
                                        onBackClick = { viewModel.navigateTo(Screen.Home) },
                                        onClearHistory = { viewModel.clearHistory() }
                                    )
                                }
                                Screen.Settings -> {
                                    SettingsScreen(
                                        settings = appSettings,
                                        language = language,
                                        dataPreservationReport = dataPreservationReport,
                                        onBackClick = { viewModel.navigateTo(Screen.Home) },
                                        onUpdateSettings = { viewModel.updateSettings(it) },
                                        onTestAlarmClick = { viewModel.testAlarm() },
                                        onDeleteAccountClick = { viewModel.deleteAccount() },
                                        onVerifyIntegrity = { viewModel.validateDataIntegrityNow() },
                                        onCreateBackup = { viewModel.createSafetyBackupNow() },
                                        onSyncCloud = { viewModel.syncCloudAccountNow() }
                                    )
                                }
                                Screen.Account -> {
                                    AccountScreen(
                                        currentUser = currentUser,
                                        language = language,
                                        defaultCountryCode = appSettings.countryCode,
                                        onBackClick = { viewModel.navigateTo(Screen.Home) },
                                        onLogin = { email, pass -> viewModel.login(email, pass) },
                                        onRegister = { name, email, pass, phone -> viewModel.register(name, email, pass, phone) },
                                        onContinueAsGuest = { viewModel.continueAsGuest() },
                                        onLogout = { viewModel.logout() },
                                        onDeleteAccount = { viewModel.deleteAccount() }
                                    )
                                }
                                Screen.VoiceLive -> {
                                    VoiceConversationScreen(
                                        liveService = viewModel.geminiLiveService,
                                        onBackClick = { viewModel.navigateTo(Screen.Home) }
                                    )
                                }
                                Screen.GroundedTransit -> {
                                    TransitGroundingScreen(
                                        viewModel = viewModel,
                                        onBackClick = { viewModel.navigateTo(Screen.Home) },
                                        onPlaceSelectedAsDestination = { place ->
                                            viewModel.startTripToGroundedPlace(place)
                                        }
                                    )
                                }
                            }

                            // Simulation Overlay Drawer
                            if (showSimulator) {
                                SimulationDrawer(
                                    activeTrip = activeTrip,
                                    onClose = { viewModel.toggleSimulator(false) },
                                    onSimulateLocationUpdate = { loc ->
                                        viewModel.simulateLocation(loc)
                                    },
                                    onTriggerSimulatedAlarm = {
                                        viewModel.testAlarm()
                                    }
                                )
                            }

                            // Alarm Trigger Dialog (when arrival or alert triggered)
                            val effectiveAlarmStage = testAlarmStage ?: activeTrip?.alarmStage
                            if (effectiveAlarmStage != null && effectiveAlarmStage != AlarmStage.NO_ALARM) {
                                AlarmTriggerDialog(
                                    stage = effectiveAlarmStage,
                                    destinationName = activeTrip?.destination?.name ?: "Arrival Destination",
                                    remainingDistanceMeters = activeTrip?.remainingDistanceMeters,
                                    remainingMinutes = activeTrip?.remainingMinutes,
                                    transportMode = activeTrip?.destination?.transportMode ?: com.example.model.TransportMode.BUS,
                                    language = language,
                                    onImAwakeClick = { viewModel.dismissAlarm() },
                                    onSnoozeClick = { viewModel.snoozeAlarm() },
                                    onEndTripClick = { viewModel.endTrip() }
                                )
                            }

                            // Global Country Selection Dialog
                            if (showCountryPicker) {
                                CountrySelectionDialog(
                                    currentCountryCode = appSettings.countryCode,
                                    searchWorldwide = appSettings.searchWorldwide,
                                    language = language,
                                    onCountrySelected = { country ->
                                        viewModel.selectCountry(country)
                                        showCountryPicker = false
                                    },
                                    onWorldwideToggle = { enabled ->
                                        viewModel.setSearchWorldwide(enabled)
                                    },
                                    onDismissRequest = { showCountryPicker = false }
                                )
                            }

                            // Active Trip Restored Across App Update Banner
                            if (activeTripRestored != null) {
                                Card(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Active Trip Restored Across Update",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = "Heading to: ${activeTripRestored!!.destination.name}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                        TextButton(onClick = { viewModel.dismissRestoredTripBanner() }) {
                                            Text("Dismiss", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
