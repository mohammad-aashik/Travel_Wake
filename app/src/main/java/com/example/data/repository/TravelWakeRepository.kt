package com.example.data.repository

import android.content.Context
import android.location.Location
import com.example.data.local.ActiveTripEntity
import com.example.data.local.DataPreservationManager
import com.example.data.local.SavedPlaceEntity
import com.example.data.local.TravelHistoryEntity
import com.example.data.local.TravelWakeDatabase
import com.example.data.local.UserAccountEntity
import com.example.data.provider.DefaultGlobalTransportProvider
import com.example.data.provider.GeocodeResult
import com.example.data.provider.NominatimGeocodingProvider
import com.example.data.provider.TransportProvider
import com.example.engine.DynamicETAEngine
import com.example.model.ActiveTrip
import com.example.model.AlarmStage
import com.example.model.AppSettings
import com.example.model.DestinationSnapshot
import com.example.model.DestinationType
import com.example.model.ReliabilityLevel
import com.example.model.SavedPlace
import com.example.model.ThemeMode
import com.example.model.TransportMode
import com.example.model.TravelHistoryItem
import com.example.model.TripCompletionStatus
import com.example.model.TripState
import com.example.model.UnitSystem
import com.example.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class TravelWakeRepository(
    private val context: Context,
    private val database: TravelWakeDatabase = TravelWakeDatabase.getDatabase(context)
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val geocodingProvider = NominatimGeocodingProvider()
    val transportProvider: TransportProvider = DefaultGlobalTransportProvider()
    val etaEngine = DynamicETAEngine()

    // Settings state backed by SharedPreferences
    private val prefs = context.getSharedPreferences("travelwake_app_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettingsFromPrefs())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Active trip in memory mapped from Room
    val activeTrip: StateFlow<ActiveTrip?> = database.tripDao().observeActiveTrip()
        .map { entity -> entity?.toModel() }
        .stateIn(scope, SharingStarted.Eagerly, null)

    // Saved places mapped from Room with duplicate items removed
    val savedPlaces: StateFlow<List<SavedPlace>> = database.savedPlaceDao().observeSavedPlaces()
        .map { list ->
            list.map { it.toModel() }
                .distinctBy { it.id }
                .distinctBy { it.name.trim().lowercase() }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    // History mapped from Room with duplicate items removed
    val travelHistory: StateFlow<List<TravelHistoryItem>> = database.historyDao().observeHistory()
        .map { list ->
            list.map { it.toModel() }
                .distinctBy { it.id }
                .distinctBy { "${it.destinationName.trim().lowercase()}_${it.startTimeMillis / 1000}" }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    // Logged in user profile
    val currentUser: StateFlow<UserProfile?> = database.userDao().observeLoggedInUser()
        .map { it?.toModel() }
        .stateIn(scope, SharingStarted.Eagerly, null)

    // Alarm triggering event for UI dialog / tone
    private val _alarmTriggerEvent = MutableStateFlow<AlarmStage?>(null)
    val alarmTriggerEvent: StateFlow<AlarmStage?> = _alarmTriggerEvent.asStateFlow()

    // Overall Travel Readiness status
    private val _travelReadiness = MutableStateFlow(ReliabilityLevel.READY)
    val travelReadiness: StateFlow<ReliabilityLevel> = _travelReadiness.asStateFlow()

    // Last known GPS location
    private val _lastKnownLocation = MutableStateFlow<Location?>(null)
    val lastKnownLocation: StateFlow<Location?> = _lastKnownLocation.asStateFlow()

    // Data Preservation & Active Trip Recovery state
    private val _activeTripRestoredEvent = MutableStateFlow<ActiveTrip?>(null)
    val activeTripRestoredEvent: StateFlow<ActiveTrip?> = _activeTripRestoredEvent.asStateFlow()

    private val _dataPreservationReport = MutableStateFlow<DataPreservationManager.ValidationReport?>(null)
    val dataPreservationReport: StateFlow<DataPreservationManager.ValidationReport?> = _dataPreservationReport.asStateFlow()

    init {
        scope.launch {
            // Step 1: Validate data integrity across update/launch
            val report = DataPreservationManager.validateDatabaseIntegrity(context, database)
            _dataPreservationReport.value = report

            // Step 2: Check and recover active trip if app was updated or restarted mid-journey
            recoverActiveTripAfterUpdateIfNeeded()

            // Step 3: Remove duplicate items (saved places & travel history)
            removeDuplicateItems()

            // Step 4: Seed default places only if user has never saved any places
            seedDefaultSavedPlacesIfEmpty()

            // Step 5: Ensure cloud account synchronization is active for registered passengers
            val loggedInUser = database.userDao().getLoggedInUser()
            if (loggedInUser != null && !loggedInUser.isGuest) {
                syncCloudAccount()
            }
        }
    }

    /**
     * Active Trip Recovery across app updates:
     * Restores trip, destination snapshot, coordinates, route, alert minutes, alarm stage,
     * re-initializes background GPS tracking service without resetting the trip to Cancelled or Completed.
     */
    suspend fun recoverActiveTripAfterUpdateIfNeeded() = withContext(Dispatchers.IO) {
        val tripEntity = database.tripDao().getActiveTrip() ?: return@withContext
        val trip = tripEntity.toModel()

        // Only recover if trip was actively running or paused
        if (trip.state == TripState.TRACKING || trip.state == TripState.ALARM_TRIGGERED || trip.state == TripState.PAUSED) {
            android.util.Log.i("TravelWakeRepo", "Active trip preserved across app update: ${trip.destination.name} (State: ${trip.state})")
            _activeTripRestoredEvent.value = trip

            // If it was tracking or triggered, resume tracking service
            if (trip.state == TripState.TRACKING || trip.state == TripState.ALARM_TRIGGERED) {
                try {
                    com.example.service.TrackingService.startService(context)
                } catch (e: Exception) {
                    android.util.Log.e("TravelWakeRepo", "Could not start tracking service during recovery: ${e.message}", e)
                }
            }
        }
    }

    fun dismissTripRestoredBanner() {
        _activeTripRestoredEvent.value = null
    }

    suspend fun validateDataPreservationNow(): DataPreservationManager.ValidationReport = withContext(Dispatchers.IO) {
        val report = DataPreservationManager.validateDatabaseIntegrity(context, database)
        _dataPreservationReport.value = report
        report
    }

    suspend fun createSafetyBackupNow(): Boolean = withContext(Dispatchers.IO) {
        DataPreservationManager.createPreMigrationBackup(context, TravelWakeDatabase.DATABASE_NAME)
    }

    fun syncCloudAccount() {
        scope.launch {
            val user = database.userDao().getLoggedInUser()?.toModel() ?: return@launch
            if (user.isGuest) return@launch
            val places = database.savedPlaceDao().getAllSavedPlaces().map { it.toModel() }
            val history = database.historyDao().getRecentHistory().map { it.toModel() }
            DataPreservationManager.syncUserAccountToCloud(context, user, places, history, _settings.value)
        }
    }

    private suspend fun seedDefaultSavedPlacesIfEmpty() {
        val existing = database.savedPlaceDao().getAllSavedPlaces()
        if (existing.isEmpty()) {
            val defaults = listOf(
                SavedPlaceEntity(
                    id = "preset_home",
                    name = "Home",
                    type = DestinationType.HOME,
                    latitude = 6.9271,
                    longitude = 79.8612,
                    address = "Residential Neighborhood",
                    radiusMeters = 200f,
                    defaultAlertMinutes = 7,
                    preferredTransportMode = TransportMode.BUS
                ),
                SavedPlaceEntity(
                    id = "preset_work",
                    name = "Work / Office",
                    type = DestinationType.WORKPLACE,
                    latitude = 6.9344,
                    longitude = 79.8503,
                    address = "Central Business District",
                    radiusMeters = 200f,
                    defaultAlertMinutes = 10,
                    preferredTransportMode = TransportMode.METRO
                ),
                SavedPlaceEntity(
                    id = "preset_station",
                    name = "Main Central Station",
                    type = DestinationType.RAILWAY_STATION,
                    latitude = 51.5308,
                    longitude = -0.1238,
                    address = "Central Interchange Terminal",
                    radiusMeters = 300f,
                    defaultAlertMinutes = 7,
                    preferredTransportMode = TransportMode.TRAIN
                )
            )
            defaults.forEach { database.savedPlaceDao().insertSavedPlace(it) }
        }
    }

    suspend fun startTrip(
        destination: DestinationSnapshot,
        alertMinutesBefore: Int = 7
    ) = withContext(Dispatchers.IO) {
        etaEngine.reset()
        val trip = ActiveTrip(
            destination = destination,
            alertMinutesBefore = alertMinutesBefore,
            state = TripState.TRACKING,
            alarmStage = AlarmStage.NO_ALARM
        )
        database.tripDao().saveActiveTrip(trip.toEntity())
    }

    suspend fun updateLocation(location: Location) = withContext(Dispatchers.IO) {
        _lastKnownLocation.value = location
        val currentTrip = database.tripDao().getActiveTrip()?.toModel() ?: return@withContext

        if (currentTrip.state != TripState.TRACKING && currentTrip.state != TripState.ALARM_TRIGGERED) {
            return@withContext
        }

        val calculation = etaEngine.processLocationUpdate(
            currentTrip = currentTrip,
            location = location
        )

        val updatedTrip = currentTrip.copy(
            currentLat = location.latitude,
            currentLng = location.longitude,
            currentSpeedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f,
            currentAccuracyMeters = if (location.hasAccuracy()) location.accuracy else 25f,
            remainingDistanceMeters = calculation.remainingDistanceMeters,
            dynamicEtaMillis = calculation.dynamicEtaMillis,
            remainingMinutes = calculation.remainingMinutes,
            etaSource = calculation.etaSource,
            etaConfidence = calculation.etaConfidence,
            etaLastUpdatedMillis = System.currentTimeMillis(),
            movementState = calculation.movementState,
            consecutiveReadingsInsideRadius = calculation.consecutiveReadingsInsideRadius,
            isWrongDirection = calculation.isWrongDirection,
            isOvershoot = calculation.isOvershoot,
            alarmStage = calculation.recommendedAlarmStage,
            state = if (calculation.recommendedAlarmStage == AlarmStage.ARRIVAL_CONFIRMED) {
                TripState.ARRIVED
            } else if (calculation.recommendedAlarmStage == AlarmStage.MAIN_ALARM_TRIGGERED ||
                       calculation.recommendedAlarmStage == AlarmStage.FINAL_WARNING_TRIGGERED) {
                TripState.ALARM_TRIGGERED
            } else {
                currentTrip.state
            }
        )

        database.tripDao().saveActiveTrip(updatedTrip.toEntity())

        // Check if alarm triggered
        if (calculation.recommendedAlarmStage == AlarmStage.MAIN_ALARM_TRIGGERED ||
            calculation.recommendedAlarmStage == AlarmStage.FINAL_WARNING_TRIGGERED ||
            calculation.recommendedAlarmStage == AlarmStage.ARRIVAL_CONFIRMED) {
            _alarmTriggerEvent.value = calculation.recommendedAlarmStage
        }
    }

    suspend fun pauseTrip() = withContext(Dispatchers.IO) {
        val trip = database.tripDao().getActiveTrip()?.toModel() ?: return@withContext
        val updated = trip.copy(state = TripState.PAUSED)
        database.tripDao().saveActiveTrip(updated.toEntity())
    }

    suspend fun resumeTrip() = withContext(Dispatchers.IO) {
        val trip = database.tripDao().getActiveTrip()?.toModel() ?: return@withContext
        val updated = trip.copy(state = TripState.TRACKING)
        database.tripDao().saveActiveTrip(updated.toEntity())
    }

    suspend fun triggerWakeMeNow() = withContext(Dispatchers.IO) {
        val trip = database.tripDao().getActiveTrip()?.toModel() ?: return@withContext
        val updated = trip.copy(
            alarmStage = AlarmStage.MAIN_ALARM_TRIGGERED,
            state = TripState.ALARM_TRIGGERED
        )
        database.tripDao().saveActiveTrip(updated.toEntity())
        _alarmTriggerEvent.value = AlarmStage.MAIN_ALARM_TRIGGERED
    }

    suspend fun snoozeAlarm(snoozeMinutes: Int = 2) = withContext(Dispatchers.IO) {
        val trip = database.tripDao().getActiveTrip()?.toModel() ?: return@withContext
        val updated = trip.copy(
            alarmStage = AlarmStage.SNOOZED,
            state = TripState.TRACKING
        )
        database.tripDao().saveActiveTrip(updated.toEntity())
        _alarmTriggerEvent.value = null
    }

    suspend fun dismissAlarm() = withContext(Dispatchers.IO) {
        _alarmTriggerEvent.value = null
    }

    suspend fun changeAlertMinutes(minutes: Int) = withContext(Dispatchers.IO) {
        val trip = database.tripDao().getActiveTrip()?.toModel() ?: return@withContext
        val updated = trip.copy(alertMinutesBefore = minutes)
        database.tripDao().saveActiveTrip(updated.toEntity())
    }

    suspend fun changeDestination(newDest: DestinationSnapshot) = withContext(Dispatchers.IO) {
        val trip = database.tripDao().getActiveTrip()?.toModel() ?: return@withContext
        etaEngine.reset()
        val updated = trip.copy(
            destination = newDest,
            alarmStage = AlarmStage.NO_ALARM,
            state = TripState.TRACKING
        )
        database.tripDao().saveActiveTrip(updated.toEntity())
    }

    suspend fun endTrip(status: TripCompletionStatus = TripCompletionStatus.COMPLETED) = withContext(Dispatchers.IO) {
        val trip = database.tripDao().getActiveTrip()?.toModel()
        _alarmTriggerEvent.value = null
        if (trip != null && _settings.value.saveHistoryEnabled) {
            val history = TravelHistoryEntity(
                id = java.util.UUID.randomUUID().toString(),
                destinationName = trip.destination.name,
                transportMode = trip.destination.transportMode,
                startTimeMillis = trip.startTimeMillis,
                endTimeMillis = System.currentTimeMillis(),
                totalDistanceMeters = trip.remainingDistanceMeters,
                initialEtaMinutes = 30,
                finalEtaMinutes = trip.remainingMinutes ?: 0,
                alertMinutesSelected = trip.alertMinutesBefore,
                status = status,
                endReason = if (status == TripCompletionStatus.COMPLETED) "Arrived at destination" else "Ended by passenger",
                startLocationName = if (trip.currentLat != null) "Departure Point" else "",
                destinationCountry = trip.destination.country
            )
            database.historyDao().insertHistory(history)
            syncCloudAccount()
        }
        database.tripDao().clearActiveTrip()
    }

    // Saved places
    suspend fun addSavedPlace(place: SavedPlace) = withContext(Dispatchers.IO) {
        val existing = database.savedPlaceDao().getAllSavedPlaces()
        val duplicate = existing.firstOrNull {
            it.name.trim().equals(place.name.trim(), ignoreCase = true) ||
            (Math.abs(it.latitude - place.latitude) < 0.0001 && Math.abs(it.longitude - place.longitude) < 0.0001)
        }
        if (duplicate != null) {
            // Update existing record rather than inserting a duplicate item
            database.savedPlaceDao().insertSavedPlace(place.toEntity().copy(id = duplicate.id))
        } else {
            database.savedPlaceDao().insertSavedPlace(place.toEntity())
        }
        syncCloudAccount()
    }

    /**
     * Purges duplicate saved places and travel history records from Room database.
     * Preserves original unique entries while ensuring no repeated items exist.
     */
    suspend fun removeDuplicateItems(): Int = withContext(Dispatchers.IO) {
        var removedCount = 0

        // 1. Remove duplicate saved places
        val places = database.savedPlaceDao().getAllSavedPlaces()
        val seenPlaceKeys = mutableSetOf<String>()
        val seenPlaceCoords = mutableSetOf<String>()
        for (p in places) {
            val nameKey = p.name.trim().lowercase()
            val coordKey = "${(p.latitude * 1000).toInt()}_${(p.longitude * 1000).toInt()}"
            if (seenPlaceKeys.contains(nameKey) || seenPlaceCoords.contains(coordKey)) {
                database.savedPlaceDao().deleteSavedPlace(p.id)
                removedCount++
            } else {
                seenPlaceKeys.add(nameKey)
                seenPlaceCoords.add(coordKey)
            }
        }

        // 2. Remove duplicate travel history items
        val history = database.historyDao().getRecentHistory()
        val seenHistoryKeys = mutableSetOf<String>()
        for (h in history) {
            val hKey = "${h.destinationName.trim().lowercase()}_${h.startTimeMillis / 1000}"
            if (seenHistoryKeys.contains(hKey)) {
                database.historyDao().deleteHistory(h.id)
                removedCount++
            } else {
                seenHistoryKeys.add(hKey)
            }
        }

        if (removedCount > 0) {
            syncCloudAccount()
        }
        removedCount
    }

    suspend fun deleteSavedPlace(id: String) = withContext(Dispatchers.IO) {
        database.savedPlaceDao().deleteSavedPlace(id)
        syncCloudAccount()
    }

    // History
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        database.historyDao().clearAllHistory()
    }

    suspend fun deleteHistoryItem(id: String) = withContext(Dispatchers.IO) {
        database.historyDao().deleteHistory(id)
    }

    // Settings
    private fun loadSettingsFromPrefs(): AppSettings {
        val lang = prefs.getString("setting_language", "en") ?: "en"
        val country = prefs.getString("setting_country", "Sri Lanka") ?: "Sri Lanka"
        val countryCode = prefs.getString("setting_country_code", "LK") ?: "LK"
        val searchWorldwide = prefs.getBoolean("setting_search_worldwide", false)
        val isAutoDetect = prefs.getBoolean("setting_auto_country_detect", true)
        val hasCompletedOnboarding = prefs.getBoolean("setting_has_completed_onboarding", false)
        val sound = prefs.getBoolean("setting_sound_enabled", true)
        val vib = prefs.getBoolean("setting_vibration_enabled", true)
        val multiStage = prefs.getBoolean("setting_multistage_wake", true)
        val radius = prefs.getFloat("setting_default_radius", 200f)
        val alertMin = prefs.getInt("setting_default_alert_min", 7)
        val themeStr = prefs.getString("setting_theme", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val theme = try { ThemeMode.valueOf(themeStr) } catch (_: Exception) { ThemeMode.DARK }
        val ringtoneId = prefs.getString("setting_ringtone_id", "metro_chime") ?: "metro_chime"
        val ringtoneTitle = prefs.getString("setting_ringtone_title", "Metro Door Chime") ?: "Metro Door Chime"
        val ringtoneUri = prefs.getString("setting_ringtone_uri", null)
        val alarmVolume = prefs.getFloat("setting_alarm_volume", 0.9f)
        val vibPattern = prefs.getString("setting_vibration_pattern", "Rapid Escalating") ?: "Rapid Escalating"

        return AppSettings(
            language = lang,
            country = country,
            countryCode = countryCode,
            searchWorldwide = searchWorldwide,
            isAutoCountryDetectEnabled = isAutoDetect,
            hasCompletedOnboarding = hasCompletedOnboarding,
            soundEnabled = sound,
            vibrationEnabled = vib,
            multiStageWakeEnabled = multiStage,
            defaultRadiusMeters = radius,
            defaultAlertMinutes = alertMin,
            themeMode = theme,
            selectedRingtoneId = ringtoneId,
            selectedRingtoneTitle = ringtoneTitle,
            selectedRingtoneUri = ringtoneUri,
            alarmVolume = alarmVolume,
            vibrationPattern = vibPattern
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        prefs.edit()
            .putString("setting_language", newSettings.language)
            .putString("setting_country", newSettings.country)
            .putString("setting_country_code", newSettings.countryCode)
            .putBoolean("setting_search_worldwide", newSettings.searchWorldwide)
            .putBoolean("setting_auto_country_detect", newSettings.isAutoCountryDetectEnabled)
            .putBoolean("setting_has_completed_onboarding", newSettings.hasCompletedOnboarding)
            .putBoolean("setting_sound_enabled", newSettings.soundEnabled)
            .putBoolean("setting_vibration_enabled", newSettings.vibrationEnabled)
            .putBoolean("setting_multistage_wake", newSettings.multiStageWakeEnabled)
            .putFloat("setting_default_radius", newSettings.defaultRadiusMeters)
            .putInt("setting_default_alert_min", newSettings.defaultAlertMinutes)
            .putString("setting_theme", newSettings.themeMode.name)
            .putString("setting_ringtone_id", newSettings.selectedRingtoneId)
            .putString("setting_ringtone_title", newSettings.selectedRingtoneTitle)
            .putString("setting_ringtone_uri", newSettings.selectedRingtoneUri)
            .putFloat("setting_alarm_volume", newSettings.alarmVolume)
            .putString("setting_vibration_pattern", newSettings.vibrationPattern)
            .apply()
    }

    // User authentication
    suspend fun registerUser(
        fullName: String,
        email: String,
        password: String,
        phoneNumber: String = "",
        language: String = _settings.value.language,
        country: String = _settings.value.country,
        countryCode: String = _settings.value.countryCode
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val existing = database.userDao().getUserByEmail(email)
        if (existing != null) {
            return@withContext Result.failure(Exception("An account with this email already exists."))
        }
        database.userDao().logoutAll()
        val userId = java.util.UUID.randomUUID().toString()
        val hash = hashPassword(password)
        val entity = UserAccountEntity(
            userId = userId,
            fullName = fullName,
            email = email,
            passwordHash = hash,
            phoneNumber = phoneNumber,
            preferredLanguage = language,
            country = country,
            countryCode = countryCode,
            isGuest = false,
            createdAtMillis = System.currentTimeMillis(),
            isLoggedIn = true
        )
        database.userDao().saveUser(entity)
        val profile = entity.toModel()
        syncCloudAccount()
        Result.success(profile)
    }

    suspend fun loginUser(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val user = database.userDao().getUserByEmail(email)
            ?: return@withContext Result.failure(Exception("Account not found. Please check your email or sign up."))

        val hash = hashPassword(password)
        if (user.passwordHash != hash) {
            return@withContext Result.failure(Exception("Incorrect password."))
        }

        database.userDao().logoutAll()
        database.userDao().setLoggedIn(user.userId)
        // Recover any cloud-synced places and travel history without creating duplicates
        DataPreservationManager.recoverUserAccountFromCloud(context, user.email, database)
        val profile = user.toModel()
        syncCloudAccount()
        Result.success(profile)
    }

    suspend fun startGuestSession(): UserProfile = withContext(Dispatchers.IO) {
        database.userDao().logoutAll()
        val guest = UserAccountEntity(
            userId = "guest_${System.currentTimeMillis()}",
            fullName = "Guest Traveler",
            email = "guest@travelwake.local",
            passwordHash = "",
            phoneNumber = "",
            preferredLanguage = _settings.value.language,
            country = _settings.value.country,
            countryCode = _settings.value.countryCode,
            isGuest = true,
            createdAtMillis = System.currentTimeMillis(),
            isLoggedIn = true
        )
        database.userDao().saveUser(guest)
        guest.toModel()
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        database.userDao().logoutAll()
    }

    suspend fun deleteAccount() = withContext(Dispatchers.IO) {
        val user = database.userDao().getLoggedInUser()
        if (user != null) {
            database.userDao().deleteUser(user.userId)
            database.historyDao().clearAllHistory()
            database.savedPlaceDao().clearSavedPlaces()
            database.tripDao().clearActiveTrip()
        }
    }

    fun triggerTestAlarm() {
        _alarmTriggerEvent.value = AlarmStage.MAIN_ALARM_TRIGGERED
    }

    fun triggerAlarmFromScheduledArrival(destinationName: String) {
        _alarmTriggerEvent.value = AlarmStage.MAIN_ALARM_TRIGGERED
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Mapping extensions
    private fun ActiveTrip.toEntity() = ActiveTripEntity(
        tripId = tripId,
        destName = destination.name,
        destLat = destination.latitude,
        destLng = destination.longitude,
        destType = destination.type,
        destRadiusMeters = destination.radiusMeters,
        destCountry = destination.country,
        destCity = destination.city,
        destFullAddress = destination.fullAddress,
        transportMode = destination.transportMode,
        routeName = destination.routeName,
        startTimeMillis = startTimeMillis,
        alertMinutesBefore = alertMinutesBefore,
        state = state,
        alarmStage = alarmStage,
        currentLat = currentLat,
        currentLng = currentLng,
        currentSpeedKmh = currentSpeedKmh,
        currentAccuracyMeters = currentAccuracyMeters,
        remainingDistanceMeters = remainingDistanceMeters,
        dynamicEtaMillis = dynamicEtaMillis,
        remainingMinutes = remainingMinutes,
        etaSource = etaSource,
        etaConfidence = etaConfidence,
        etaLastUpdatedMillis = etaLastUpdatedMillis,
        movementState = movementState,
        consecutiveReadingsInsideRadius = consecutiveReadingsInsideRadius,
        isWrongDirection = isWrongDirection,
        isOvershoot = isOvershoot,
        activeControllerDeviceId = activeControllerDeviceId
    )

    private fun ActiveTripEntity.toModel() = ActiveTrip(
        tripId = tripId,
        destination = DestinationSnapshot(
            name = destName,
            latitude = destLat,
            longitude = destLng,
            type = destType,
            radiusMeters = destRadiusMeters,
            country = destCountry,
            city = destCity,
            fullAddress = destFullAddress,
            transportMode = transportMode,
            routeName = routeName
        ),
        startTimeMillis = startTimeMillis,
        alertMinutesBefore = alertMinutesBefore,
        state = state,
        alarmStage = alarmStage,
        currentLat = currentLat,
        currentLng = currentLng,
        currentSpeedKmh = currentSpeedKmh,
        currentAccuracyMeters = currentAccuracyMeters,
        remainingDistanceMeters = remainingDistanceMeters,
        dynamicEtaMillis = dynamicEtaMillis,
        remainingMinutes = remainingMinutes,
        etaSource = etaSource,
        etaConfidence = etaConfidence,
        etaLastUpdatedMillis = etaLastUpdatedMillis,
        movementState = movementState,
        consecutiveReadingsInsideRadius = consecutiveReadingsInsideRadius,
        isWrongDirection = isWrongDirection,
        isOvershoot = isOvershoot,
        activeControllerDeviceId = activeControllerDeviceId
    )

    private fun SavedPlace.toEntity() = SavedPlaceEntity(
        id = id,
        name = name,
        type = type,
        latitude = latitude,
        longitude = longitude,
        address = address,
        radiusMeters = radiusMeters,
        defaultAlertMinutes = defaultAlertMinutes,
        preferredTransportMode = preferredTransportMode,
        notes = notes,
        countryCode = countryCode
    )

    private fun SavedPlaceEntity.toModel() = SavedPlace(
        id = id,
        name = name,
        type = type,
        latitude = latitude,
        longitude = longitude,
        address = address,
        radiusMeters = radiusMeters,
        defaultAlertMinutes = defaultAlertMinutes,
        preferredTransportMode = preferredTransportMode,
        notes = notes,
        countryCode = countryCode
    )

    private fun TravelHistoryEntity.toModel() = TravelHistoryItem(
        id = id,
        destinationName = destinationName,
        transportMode = transportMode,
        startTimeMillis = startTimeMillis,
        endTimeMillis = endTimeMillis,
        totalDistanceMeters = totalDistanceMeters,
        initialEtaMinutes = initialEtaMinutes,
        finalEtaMinutes = finalEtaMinutes,
        alertMinutesSelected = alertMinutesSelected,
        status = status,
        endReason = endReason,
        startLocationName = startLocationName,
        destinationCountry = destinationCountry
    )

    private fun UserAccountEntity.toModel() = UserProfile(
        userId = userId,
        fullName = fullName,
        email = email,
        phoneNumber = phoneNumber,
        preferredLanguage = preferredLanguage,
        country = country,
        countryCode = countryCode,
        isGuest = isGuest,
        createdAtMillis = createdAtMillis,
        profilePictureUri = profilePictureUri
    )
}
