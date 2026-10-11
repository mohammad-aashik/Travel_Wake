package com.example.model

enum class TransportMode(val label: String, val iconName: String) {
    BUS("Bus", "DirectionsBus"),
    TRAIN("Train", "Train"),
    METRO("Metro", "Subway"),
    TRAM("Tram", "Tram"),
    FERRY("Ferry", "DirectionsBoat"),
    PUBLIC_TRANSIT("Public Transit", "DepartureBoard"),
    WALKING("Walking", "DirectionsWalk"),
    CAR_TAXI("Car / Taxi", "LocalTaxi"),
    OTHER("Other", "Commute")
}

enum class DestinationType(val label: String) {
    EXACT_LOCATION("Exact Location"),
    BUS_STOP("Bus Stop"),
    RAILWAY_STATION("Railway Station"),
    METRO_STATION("Metro Station"),
    TRAM_STOP("Tram Stop"),
    BUS_STAND("Bus Stand"),
    FERRY_TERMINAL("Ferry Terminal"),
    AIRPORT("Airport"),
    UNIVERSITY("University"),
    WORKPLACE("Workplace"),
    LANDMARK("Landmark"),
    HOME("Home"),
    CUSTOM("Custom")
}

enum class TripState {
    IDLE,
    PREPARING,
    TRACKING,
    PAUSED,
    LOCATION_UNAVAILABLE,
    ALARM_TRIGGERED,
    ARRIVED,
    CANCELLED,
    INTERRUPTED,
    INCOMPLETE,
    LOCATION_LOST
}

enum class ETASource(val label: String) {
    LIVE_DATA("Live Transit Data"),
    GPS_TRAFFIC("GPS + Traffic"),
    GPS_ROUTE("GPS + Route"),
    GPS_KINEMATICS("GPS Recent Movement"),
    LAST_RELIABLE("Last Reliable ETA"),
    FALLBACK_DISTANCE("Basic Distance Fallback"),
    UNAVAILABLE("Data Unavailable")
}

enum class ETAConfidence(val label: String) {
    HIGH("High Confidence"),
    MEDIUM("Medium Confidence"),
    LOW("Low Confidence"),
    UNAVAILABLE("Unavailable")
}

enum class FreshnessState {
    FRESH,   // 0 - 30s
    AGING,   // 31 - 90s
    STALE,   // 91 - 180s
    EXPIRED  // > 180s
}

enum class MovementState {
    MOVING,
    SLOW,
    STOPPED,
    UNKNOWN
}

enum class AlarmStage {
    NO_ALARM,
    EARLY_WARNING_TRIGGERED,
    MAIN_ALARM_TRIGGERED,
    SNOOZED,
    FINAL_WARNING_TRIGGERED,
    ARRIVAL_CONFIRMED
}

enum class ReliabilityLevel {
    READY,      // Green
    LIMITED,    // Yellow
    NOT_RELIABLE // Red
}

/**
 * Immutable snapshot of the destination stored when the trip starts.
 * Edits to saved places will never alter this snapshot.
 */
data class DestinationSnapshot(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: DestinationType,
    val radiusMeters: Float = 200f,
    val country: String = "",
    val city: String = "",
    val fullAddress: String = "",
    val transportMode: TransportMode = TransportMode.BUS,
    val routeName: String = "Direct / Default Route"
)

data class ActiveTrip(
    val tripId: String = java.util.UUID.randomUUID().toString(),
    val destination: DestinationSnapshot,
    val startTimeMillis: Long = System.currentTimeMillis(),
    val alertMinutesBefore: Int = 7,
    val state: TripState = TripState.TRACKING,
    val alarmStage: AlarmStage = AlarmStage.NO_ALARM,
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val currentSpeedKmh: Float = 0f,
    val currentAccuracyMeters: Float = 0f,
    val remainingDistanceMeters: Double = 0.0,
    val dynamicEtaMillis: Long? = null,
    val remainingMinutes: Int? = null,
    val etaSource: ETASource = ETASource.GPS_KINEMATICS,
    val etaConfidence: ETAConfidence = ETAConfidence.MEDIUM,
    val etaLastUpdatedMillis: Long = System.currentTimeMillis(),
    val movementState: MovementState = MovementState.UNKNOWN,
    val consecutiveReadingsInsideRadius: Int = 0,
    val isWrongDirection: Boolean = false,
    val isOvershoot: Boolean = false,
    val activeControllerDeviceId: String = "this_device"
)
