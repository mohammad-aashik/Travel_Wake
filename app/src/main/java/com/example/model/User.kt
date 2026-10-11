package com.example.model

data class UserProfile(
    val userId: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String = "",
    val preferredLanguage: String = "en",
    val country: String = "Sri Lanka",
    val countryCode: String = "LK",
    val isGuest: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val profilePictureUri: String = ""
)

data class SavedPlace(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: DestinationType,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val radiusMeters: Float = 200f,
    val defaultAlertMinutes: Int = 7,
    val preferredTransportMode: TransportMode = TransportMode.BUS,
    val notes: String = "",
    val countryCode: String = "LK"
)

data class TravelHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val destinationName: String,
    val transportMode: TransportMode,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val totalDistanceMeters: Double,
    val initialEtaMinutes: Int,
    val finalEtaMinutes: Int,
    val alertMinutesSelected: Int,
    val status: TripCompletionStatus,
    val endReason: String = "",
    val startLocationName: String = "",
    val destinationCountry: String = ""
)

enum class TripCompletionStatus(val label: String) {
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    INTERRUPTED("Interrupted"),
    INCOMPLETE("Incomplete"),
    LOCATION_LOST("Location Lost"),
    DESTINATION_PASSED("Destination Passed")
}

enum class UnitSystem(val label: String) {
    METRIC("Kilometers & Meters"),
    IMPERIAL("Miles & Feet")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    DARK("Dark Theme"),
    LIGHT("Light Theme")
}

data class AppSettings(
    val language: String = "en",
    val country: String = "Sri Lanka",
    val countryCode: String = "LK",
    val searchWorldwide: Boolean = false,
    val isAutoCountryDetectEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val defaultRadiusMeters: Float = 200f,
    val defaultAlertMinutes: Int = 7,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val saveHistoryEnabled: Boolean = true,
    val multiStageWakeEnabled: Boolean = true,
    val bluetoothWarningEnabled: Boolean = true,
    val simulationModeEnabled: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val selectedRingtoneId: String = "metro_chime",
    val selectedRingtoneTitle: String = "Metro Door Chime",
    val selectedRingtoneUri: String? = null,
    val alarmVolume: Float = 0.9f,
    val vibrationPattern: String = "Rapid Escalating"
)
