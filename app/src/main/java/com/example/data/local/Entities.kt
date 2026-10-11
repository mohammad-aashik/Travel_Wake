package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.AlarmStage
import com.example.model.DestinationType
import com.example.model.ETAConfidence
import com.example.model.ETASource
import com.example.model.MovementState
import com.example.model.TransportMode
import com.example.model.TripCompletionStatus
import com.example.model.TripState

@Entity(tableName = "active_trip")
data class ActiveTripEntity(
    @PrimaryKey val tripId: String,
    val destName: String,
    val destLat: Double,
    val destLng: Double,
    val destType: DestinationType,
    val destRadiusMeters: Float,
    val destCountry: String,
    val destCity: String,
    val destFullAddress: String,
    val transportMode: TransportMode,
    val routeName: String,
    val startTimeMillis: Long,
    val alertMinutesBefore: Int,
    val state: TripState,
    val alarmStage: AlarmStage,
    val currentLat: Double?,
    val currentLng: Double?,
    val currentSpeedKmh: Float,
    val currentAccuracyMeters: Float,
    val remainingDistanceMeters: Double,
    val dynamicEtaMillis: Long?,
    val remainingMinutes: Int?,
    val etaSource: ETASource,
    val etaConfidence: ETAConfidence,
    val etaLastUpdatedMillis: Long,
    val movementState: MovementState,
    val consecutiveReadingsInsideRadius: Int,
    val isWrongDirection: Boolean,
    val isOvershoot: Boolean,
    val activeControllerDeviceId: String
)

@Entity(tableName = "travel_history")
data class TravelHistoryEntity(
    @PrimaryKey val id: String,
    val destinationName: String,
    val transportMode: TransportMode,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val totalDistanceMeters: Double,
    val initialEtaMinutes: Int,
    val finalEtaMinutes: Int,
    val alertMinutesSelected: Int,
    val status: TripCompletionStatus,
    val endReason: String,
    val startLocationName: String = "",
    val destinationCountry: String = ""
)

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: DestinationType,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val radiusMeters: Float,
    val defaultAlertMinutes: Int,
    val preferredTransportMode: TransportMode,
    val notes: String = "",
    val countryCode: String = "LK"
)

@Entity(tableName = "user_account")
data class UserAccountEntity(
    @PrimaryKey val userId: String,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val phoneNumber: String,
    val preferredLanguage: String,
    val country: String = "Sri Lanka",
    val countryCode: String = "LK",
    val isGuest: Boolean,
    val createdAtMillis: Long,
    val isLoggedIn: Boolean,
    val profilePictureUri: String = ""
)
