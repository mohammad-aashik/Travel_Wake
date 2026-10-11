package com.example.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TravelWakeApp
import com.example.data.provider.NominatimGeocodingProvider
import com.example.data.repository.TravelWakeRepository
import com.example.model.ActiveTrip
import com.example.model.AlarmStage
import com.example.model.AppSettings
import com.example.model.DestinationSnapshot
import com.example.model.ReliabilityLevel
import com.example.model.SavedPlace
import com.example.model.TravelHistoryItem
import com.example.model.TripCompletionStatus
import com.example.model.UserProfile
import com.example.service.TrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.model.Country
import com.example.model.TransitRoute
import com.example.service.ai.GeminiGroundingResult
import com.example.service.ai.GeminiLiveService
import com.example.service.ai.GeminiTransitService
import com.example.service.ai.GroundedPlace
import com.example.service.alarm.AlarmScheduleResult
import com.example.service.alarm.AlarmSoundManager
import com.example.service.alarm.TransportAlarmScheduler

sealed class Screen {
    object Home : Screen()
    object DestinationSelect : Screen()
    object LiveTrip : Screen()
    object SavedPlaces : Screen()
    object History : Screen()
    object Settings : Screen()
    object Account : Screen()
    object Onboarding : Screen()
    object VoiceLive : Screen()
    object GroundedTransit : Screen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TravelWakeRepository = (application as TravelWakeApp).repository
    val geocodingProvider = NominatimGeocodingProvider()

    // Gemini AI Services
    val geminiTransitService = GeminiTransitService()
    val geminiLiveService = GeminiLiveService(application)

    // Maps Grounding State
    private val _mapsGroundingLoading = MutableStateFlow(false)
    val mapsGroundingLoading: StateFlow<Boolean> = _mapsGroundingLoading.asStateFlow()

    private val _mapsGroundingResult = MutableStateFlow<GeminiGroundingResult?>(null)
    val mapsGroundingResult: StateFlow<GeminiGroundingResult?> = _mapsGroundingResult.asStateFlow()

    // Search Grounding State
    private val _searchGroundingLoading = MutableStateFlow(false)
    val searchGroundingLoading: StateFlow<Boolean> = _searchGroundingLoading.asStateFlow()

    private val _searchGroundingResult = MutableStateFlow<GeminiGroundingResult?>(null)
    val searchGroundingResult: StateFlow<GeminiGroundingResult?> = _searchGroundingResult.asStateFlow()

    // Navigation & UI State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _showSimulator = MutableStateFlow(false)
    val showSimulator: StateFlow<Boolean> = _showSimulator.asStateFlow()

    private val _testAlarmTriggered = MutableStateFlow<AlarmStage?>(null)
    val testAlarmTriggered: StateFlow<AlarmStage?> = _testAlarmTriggered.asStateFlow()

    private val _lastAlarmScheduleResult = MutableStateFlow<AlarmScheduleResult?>(null)
    val lastAlarmScheduleResult: StateFlow<AlarmScheduleResult?> = _lastAlarmScheduleResult.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Permissions State
    private val _hasLocationPermission = MutableStateFlow(true)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(true)
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    private val _isGpsEnabled = MutableStateFlow(true)
    val isGpsEnabled: StateFlow<Boolean> = _isGpsEnabled.asStateFlow()

    // Flows from Repository
    val activeTrip: StateFlow<ActiveTrip?> = repository.activeTrip
    val savedPlaces: StateFlow<List<SavedPlace>> = repository.savedPlaces
    val travelHistory: StateFlow<List<TravelHistoryItem>> = repository.travelHistory
    val currentUser: StateFlow<UserProfile?> = repository.currentUser
    val appSettings: StateFlow<AppSettings> = repository.settings
    val activeTripRestoredEvent: StateFlow<ActiveTrip?> = repository.activeTripRestoredEvent
    val dataPreservationReport: StateFlow<com.example.data.local.DataPreservationManager.ValidationReport?> = repository.dataPreservationReport

    // Combined Reliability Calculation
    val readiness: StateFlow<ReliabilityLevel> = combine(
        hasLocationPermission,
        hasNotificationPermission,
        isGpsEnabled
    ) { loc, notif, gps ->
        when {
            !loc || !gps -> ReliabilityLevel.NOT_RELIABLE
            !notif -> ReliabilityLevel.LIMITED
            else -> ReliabilityLevel.READY
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReliabilityLevel.READY)

    init {
        // Auto-navigate to Onboarding if first launch
        viewModelScope.launch {
            appSettings.collect { settings ->
                if (!settings.hasCompletedOnboarding && _currentScreen.value == Screen.Home) {
                    _currentScreen.value = Screen.Onboarding
                }
            }
        }

        // Auto-navigate to LiveTrip if an active trip was restored across app update
        viewModelScope.launch {
            activeTripRestoredEvent.collect { restoredTrip ->
                if (restoredTrip != null && _currentScreen.value == Screen.Home) {
                    _currentScreen.value = Screen.LiveTrip
                }
            }
        }
    }

    fun dismissRestoredTripBanner() {
        repository.dismissTripRestoredBanner()
    }

    fun validateDataIntegrityNow() {
        viewModelScope.launch {
            repository.validateDataPreservationNow()
        }
    }

    fun createSafetyBackupNow() {
        viewModelScope.launch {
            repository.createSafetyBackupNow()
        }
    }

    fun syncCloudAccountNow() {
        repository.syncCloudAccount()
    }

    fun navigateTo(screen: Screen) {
        _authErrorMessage.value = null
        _currentScreen.value = screen
    }

    fun selectCountry(country: Country) {
        viewModelScope.launch {
            val updated = appSettings.value.copy(
                country = country.name,
                countryCode = country.code
            )
            repository.updateSettings(updated)
        }
    }

    fun setSearchWorldwide(enabled: Boolean) {
        viewModelScope.launch {
            val updated = appSettings.value.copy(searchWorldwide = enabled)
            repository.updateSettings(updated)
        }
    }

    fun completeSetupGuest(country: Country) {
        viewModelScope.launch {
            val updated = appSettings.value.copy(
                country = country.name,
                countryCode = country.code,
                hasCompletedOnboarding = true
            )
            repository.updateSettings(updated)
            repository.startGuestSession()
            _currentScreen.value = Screen.Home
        }
    }

    fun completeSetupRegister(name: String, email: String, pass: String, phone: String, country: Country) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            val updated = appSettings.value.copy(
                country = country.name,
                countryCode = country.code
            )
            repository.updateSettings(updated)
            val result = repository.registerUser(
                fullName = name,
                email = email,
                password = pass,
                phoneNumber = phone,
                language = updated.language,
                country = country.name,
                countryCode = country.code
            )
            _isAuthLoading.value = false
            result.onSuccess {
                repository.updateSettings(updated.copy(hasCompletedOnboarding = true))
                _currentScreen.value = Screen.Home
            }.onFailure { err ->
                _authErrorMessage.value = err.message ?: "Registration failed."
            }
        }
    }

    fun completeSetupLogin(email: String, pass: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            val result = repository.loginUser(email, pass)
            _isAuthLoading.value = false
            result.onSuccess {
                repository.updateSettings(appSettings.value.copy(hasCompletedOnboarding = true))
                _currentScreen.value = Screen.Home
            }.onFailure { err ->
                _authErrorMessage.value = err.message ?: "Login failed."
            }
        }
    }

    fun toggleSimulator(show: Boolean) {
        _showSimulator.value = show
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _hasLocationPermission.value = granted
    }

    fun setNotificationPermissionGranted(granted: Boolean) {
        _hasNotificationPermission.value = granted
    }

    fun setGpsEnabled(enabled: Boolean) {
        _isGpsEnabled.value = enabled
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.updateSettings(appSettings.value.copy(hasCompletedOnboarding = true))
            _currentScreen.value = Screen.Home
        }
    }

    fun startTrip(destination: DestinationSnapshot, alertMinutes: Int) {
        viewModelScope.launch {
            repository.startTrip(destination, alertMinutes)
            TrackingService.startService(getApplication())
            _currentScreen.value = Screen.LiveTrip
        }
    }

    fun pauseTrip() {
        viewModelScope.launch {
            repository.pauseTrip()
        }
    }

    fun resumeTrip() {
        viewModelScope.launch {
            repository.resumeTrip()
        }
    }

    fun updateAlertMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.changeAlertMinutes(minutes)
        }
    }

    fun triggerWakeMeNow() {
        viewModelScope.launch {
            repository.triggerWakeMeNow()
            _testAlarmTriggered.value = AlarmStage.MAIN_ALARM_TRIGGERED
        }
    }

    fun endTrip() {
        viewModelScope.launch {
            repository.endTrip(TripCompletionStatus.COMPLETED)
            TrackingService.stopService(getApplication())
            _testAlarmTriggered.value = null
            _currentScreen.value = Screen.Home
        }
    }

    fun dismissAlarm() {
        AlarmSoundManager.stopAudio()
        viewModelScope.launch {
            repository.dismissAlarm()
            _testAlarmTriggered.value = null
        }
    }

    fun snoozeAlarm() {
        AlarmSoundManager.stopAudio()
        viewModelScope.launch {
            repository.snoozeAlarm(2)
            _testAlarmTriggered.value = null
        }
    }

    fun testAlarm() {
        _testAlarmTriggered.value = AlarmStage.MAIN_ALARM_TRIGGERED
        val settings = appSettings.value
        if (settings.soundEnabled) {
            AlarmSoundManager.playAlarm(
                context = getApplication(),
                ringtoneId = settings.selectedRingtoneId,
                uriString = settings.selectedRingtoneUri,
                volume = settings.alarmVolume
            )
        }
    }

    /**
     * Sets a phone alarm based on destination arrival time identified from transport search.
     */
    fun setPhoneAlarmBasedOnArrival(
        destination: DestinationSnapshot,
        alertMinutesBefore: Int = 5,
        route: TransitRoute? = null,
        openSystemClockApp: Boolean = true
    ): AlarmScheduleResult {
        val currentTrip = activeTrip.value
        val userLat = currentTrip?.currentLat
        val userLng = currentTrip?.currentLng
        val result = TransportAlarmScheduler.setPhoneAlarmForArrival(
            context = getApplication(),
            destination = destination,
            alertMinutesBefore = alertMinutesBefore,
            userLat = userLat,
            userLng = userLng,
            route = route,
            openSystemClockApp = openSystemClockApp
        )
        _lastAlarmScheduleResult.value = result
        return result
    }

    fun clearAlarmScheduleResult() {
        _lastAlarmScheduleResult.value = null
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    fun savePlace(place: SavedPlace) {
        viewModelScope.launch {
            repository.addSavedPlace(place)
        }
    }

    fun deletePlace(id: String) {
        viewModelScope.launch {
            repository.deleteSavedPlace(id)
        }
    }

    fun removeDuplicateItems() {
        viewModelScope.launch {
            repository.removeDuplicateItems()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            repository.loginUser(email, pass)
            _currentScreen.value = Screen.Home
        }
    }

    fun register(name: String, email: String, pass: String, phone: String) {
        viewModelScope.launch {
            repository.registerUser(name, email, pass, phone)
            _currentScreen.value = Screen.Home
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            repository.startGuestSession()
            _currentScreen.value = Screen.Home
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _currentScreen.value = Screen.Home
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            repository.deleteAccount()
            _currentScreen.value = Screen.Home
        }
    }

    // Simulation feed
    fun simulateLocation(location: Location) {
        viewModelScope.launch {
            repository.updateLocation(location)
        }
    }

    // Google Maps Grounding (gemini-3.5-flash with googleMaps)
    fun queryGoogleMapsGrounding(prompt: String, lat: Double? = null, lng: Double? = null) {
        viewModelScope.launch {
            _mapsGroundingLoading.value = true
            _mapsGroundingResult.value = null
            val result = geminiTransitService.queryWithGoogleMaps(prompt, lat, lng)
            _mapsGroundingResult.value = result
            _mapsGroundingLoading.value = false
        }
    }

    // Google Search Grounding (gemini-3.5-flash with googleSearch)
    fun queryGoogleSearchGrounding(prompt: String) {
        viewModelScope.launch {
            _searchGroundingLoading.value = true
            _searchGroundingResult.value = null
            val result = geminiTransitService.queryWithGoogleSearch(prompt)
            _searchGroundingResult.value = result
            _searchGroundingLoading.value = false
        }
    }

    fun startTripToGroundedPlace(place: GroundedPlace, alertMinutes: Int = 7) {
        val snapshot = com.example.model.DestinationSnapshot(
            id = place.id,
            name = place.name,
            latitude = place.latitude,
            longitude = place.longitude,
            type = place.destinationType,
            fullAddress = place.address,
            transportMode = place.transportMode,
            radiusMeters = 200f
        )
        startTrip(snapshot, alertMinutes)
    }

    fun saveGroundedPlaceAsSaved(place: GroundedPlace) {
        val savedPlace = com.example.model.SavedPlace(
            id = place.id,
            name = place.name,
            type = place.destinationType,
            latitude = place.latitude,
            longitude = place.longitude,
            address = place.address,
            preferredTransportMode = place.transportMode,
            countryCode = appSettings.value.countryCode
        )
        savePlace(savedPlace)
    }

    override fun onCleared() {
        super.onCleared()
        geminiLiveService.cleanup()
    }
}
