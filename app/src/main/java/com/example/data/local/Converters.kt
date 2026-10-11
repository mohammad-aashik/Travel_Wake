package com.example.data.local

import androidx.room.TypeConverter
import com.example.model.AlarmStage
import com.example.model.DestinationType
import com.example.model.ETAConfidence
import com.example.model.ETASource
import com.example.model.MovementState
import com.example.model.TransportMode
import com.example.model.TripCompletionStatus
import com.example.model.TripState

class Converters {
    @TypeConverter
    fun fromTransportMode(value: TransportMode): String = value.name

    @TypeConverter
    fun toTransportMode(value: String): TransportMode = try {
        TransportMode.valueOf(value)
    } catch (e: Exception) {
        TransportMode.BUS
    }

    @TypeConverter
    fun fromDestinationType(value: DestinationType): String = value.name

    @TypeConverter
    fun toDestinationType(value: String): DestinationType = try {
        DestinationType.valueOf(value)
    } catch (e: Exception) {
        DestinationType.EXACT_LOCATION
    }

    @TypeConverter
    fun fromTripState(value: TripState): String = value.name

    @TypeConverter
    fun toTripState(value: String): TripState = try {
        TripState.valueOf(value)
    } catch (e: Exception) {
        TripState.IDLE
    }

    @TypeConverter
    fun fromAlarmStage(value: AlarmStage): String = value.name

    @TypeConverter
    fun toAlarmStage(value: String): AlarmStage = try {
        AlarmStage.valueOf(value)
    } catch (e: Exception) {
        AlarmStage.NO_ALARM
    }

    @TypeConverter
    fun fromETASource(value: ETASource): String = value.name

    @TypeConverter
    fun toETASource(value: String): ETASource = try {
        ETASource.valueOf(value)
    } catch (e: Exception) {
        ETASource.GPS_KINEMATICS
    }

    @TypeConverter
    fun fromETAConfidence(value: ETAConfidence): String = value.name

    @TypeConverter
    fun toETAConfidence(value: String): ETAConfidence = try {
        ETAConfidence.valueOf(value)
    } catch (e: Exception) {
        ETAConfidence.MEDIUM
    }

    @TypeConverter
    fun fromMovementState(value: MovementState): String = value.name

    @TypeConverter
    fun toMovementState(value: String): MovementState = try {
        MovementState.valueOf(value)
    } catch (e: Exception) {
        MovementState.UNKNOWN
    }

    @TypeConverter
    fun fromTripCompletionStatus(value: TripCompletionStatus): String = value.name

    @TypeConverter
    fun toTripCompletionStatus(value: String): TripCompletionStatus = try {
        TripCompletionStatus.valueOf(value)
    } catch (e: Exception) {
        TripCompletionStatus.COMPLETED
    }
}
