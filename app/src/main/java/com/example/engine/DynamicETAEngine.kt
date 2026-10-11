package com.example.engine

import android.location.Location
import com.example.model.ActiveTrip
import com.example.model.AlarmStage
import com.example.model.DestinationSnapshot
import com.example.model.ETAConfidence
import com.example.model.ETASource
import com.example.model.FreshnessState
import com.example.model.MovementState
import com.example.model.TransportMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ETACalculationResult(
    val remainingDistanceMeters: Double,
    val dynamicEtaMillis: Long?,
    val remainingMinutes: Int?,
    val etaSource: ETASource,
    val etaConfidence: ETAConfidence,
    val freshnessState: FreshnessState,
    val movementState: MovementState,
    val consecutiveReadingsInsideRadius: Int,
    val isWrongDirection: Boolean,
    val isOvershoot: Boolean,
    val recommendedAlarmStage: AlarmStage
)

class DynamicETAEngine {

    // Moving average speed tracker
    private val speedHistory = mutableListOf<Float>()
    private var previousDistanceMeters: Double? = null
    private var previousLocationTimestamp: Long? = null
    private var previousBearingToDest: Float? = null

    fun reset() {
        speedHistory.clear()
        previousDistanceMeters = null
        previousLocationTimestamp = null
        previousBearingToDest = null
    }

    fun processLocationUpdate(
        currentTrip: ActiveTrip,
        location: Location,
        liveTransitDelayMinutes: Int = 0,
        isTransitProviderAvailable: Boolean = true
    ): ETACalculationResult {
        val dest = currentTrip.destination
        val now = System.currentTimeMillis()

        // 1. Calculate distance to destination
        val directDistanceMeters = calculateDistanceMeters(
            location.latitude,
            location.longitude,
            dest.latitude,
            dest.longitude
        )

        // Estimated transit route distance (accounting for roads / tracks)
        val routeMultiplier = when (dest.transportMode) {
            TransportMode.BUS, TransportMode.CAR_TAXI -> 1.25
            TransportMode.TRAIN, TransportMode.METRO -> 1.15
            TransportMode.TRAM -> 1.20
            TransportMode.FERRY -> 1.10
            TransportMode.WALKING -> 1.18
            else -> 1.20
        }
        val remainingRouteMeters = directDistanceMeters * routeMultiplier

        // 2. Track speed and movement state
        val rawSpeedKmh = (if (location.hasSpeed()) location.speed * 3.6f else 0f).coerceAtLeast(0f)
        if (rawSpeedKmh > 1.0f) {
            speedHistory.add(rawSpeedKmh)
            if (speedHistory.size > 10) speedHistory.removeAt(0)
        }

        val movingAverageSpeedKmh = if (speedHistory.isNotEmpty()) {
            speedHistory.average().toFloat()
        } else {
            getDefaultSpeedForMode(dest.transportMode)
        }

        val movementState = when {
            rawSpeedKmh >= 8.0f -> MovementState.MOVING
            rawSpeedKmh in 1.5f..7.9f -> MovementState.SLOW
            location.hasSpeed() && rawSpeedKmh < 1.5f -> MovementState.STOPPED
            else -> MovementState.UNKNOWN
        }

        // 3. Direction and Overshoot / Wrong Direction Analysis
        val bearingToDest = calculateBearing(
            location.latitude,
            location.longitude,
            dest.latitude,
            dest.longitude
        )

        var isWrongDirection = false
        var isOvershoot = false

        val prevDist = previousDistanceMeters
        if (prevDist != null && directDistanceMeters > prevDist + 60.0 && rawSpeedKmh > 5.0f) {
            // Distance is actively increasing while moving
            if (prevDist < dest.radiusMeters * 2.5) {
                // Was previously very close to destination and is now moving away
                isOvershoot = true
            } else {
                isWrongDirection = true
            }
        }

        previousDistanceMeters = directDistanceMeters
        previousLocationTimestamp = now
        previousBearingToDest = bearingToDest

        // 4. Consecutive readings inside destination radius (GPS uncertainty protection)
        // If destination radius is 50m but accuracy is 80m, do NOT declare arrival immediately!
        val gpsAccuracy = if (location.hasAccuracy()) location.accuracy else 30f
        val effectiveRadius = dest.radiusMeters

        var consecutiveInside = currentTrip.consecutiveReadingsInsideRadius
        val isInsideRadius = directDistanceMeters <= effectiveRadius

        if (isInsideRadius) {
            // Require accuracy to be reasonable relative to radius or multiple confirmations
            if (gpsAccuracy <= effectiveRadius * 1.5f || directDistanceMeters <= effectiveRadius * 0.5f) {
                consecutiveInside++
            }
        } else {
            consecutiveInside = 0
        }

        // 5. Dynamic ETA Calculation
        val effectiveSpeedKmh = when {
            movingAverageSpeedKmh > 5f -> movingAverageSpeedKmh
            rawSpeedKmh > 5f -> rawSpeedKmh
            else -> getDefaultSpeedForMode(dest.transportMode)
        }

        val calculatedMinutesPureKinematics = ((remainingRouteMeters / 1000.0) / effectiveSpeedKmh * 60.0).toInt().coerceAtLeast(1)

        val rawEstimatedMinutes: Int
        val etaSource: ETASource

        if (isTransitProviderAvailable && liveTransitDelayMinutes >= 0) {
            rawEstimatedMinutes = (calculatedMinutesPureKinematics + liveTransitDelayMinutes).coerceAtLeast(1)
            etaSource = if (liveTransitDelayMinutes > 0) ETASource.GPS_TRAFFIC else ETASource.LIVE_DATA
        } else if (rawSpeedKmh > 5f) {
            rawEstimatedMinutes = calculatedMinutesPureKinematics
            etaSource = ETASource.GPS_KINEMATICS
        } else {
            rawEstimatedMinutes = calculatedMinutesPureKinematics
            etaSource = ETASource.GPS_ROUTE
        }

        // 6. Abnormal ETA Jump Rejection / Smoothing
        val lastMinutes = currentTrip.remainingMinutes
        val smoothedMinutes = if (lastMinutes != null && lastMinutes > 0) {
            val jump = kotlin.math.abs(rawEstimatedMinutes - lastMinutes)
            if (jump > 20 && directDistanceMeters > 500) {
                // Reject massive impossible jump (e.g. 15min -> 120min or 40min -> 3min in single update)
                (lastMinutes * 0.7 + rawEstimatedMinutes * 0.3).toInt()
            } else {
                rawEstimatedMinutes
            }
        } else {
            rawEstimatedMinutes
        }

        // 7. ETA Confidence
        val etaConfidence = when {
            isWrongDirection || isOvershoot -> ETAConfidence.LOW
            gpsAccuracy > 100f -> ETAConfidence.LOW
            etaSource == ETASource.LIVE_DATA && gpsAccuracy < 40f -> ETAConfidence.HIGH
            etaSource == ETASource.GPS_KINEMATICS && speedHistory.size >= 3 -> ETAConfidence.HIGH
            directDistanceMeters < 500 -> ETAConfidence.HIGH
            else -> ETAConfidence.MEDIUM
        }

        val dynamicEtaMillis = now + (smoothedMinutes * 60L * 1000L)

        // 8. Freshness
        val timeSinceLocation = now - location.time
        val freshnessState = when {
            timeSinceLocation <= 30_000L -> FreshnessState.FRESH
            timeSinceLocation <= 90_000L -> FreshnessState.AGING
            timeSinceLocation <= 180_000L -> FreshnessState.STALE
            else -> FreshnessState.EXPIRED
        }

        // 9. Multi-Stage Alarm State Determination
        // Rule: ARRIVAL CONFIRMATION has priority!
        val alertThresholdMin = currentTrip.alertMinutesBefore
        val currentStage = currentTrip.alarmStage

        val recommendedStage = when {
            // Arrived: inside radius with multiple confirmed readings or very close
            (consecutiveInside >= 2 || (isInsideRadius && directDistanceMeters <= 40.0)) -> {
                AlarmStage.ARRIVAL_CONFIRMED
            }
            // Final warning: 1-2 minutes remaining or within 350m
            smoothedMinutes <= 2 || directDistanceMeters <= 350.0 -> {
                if (currentStage == AlarmStage.SNOOZED) {
                    AlarmStage.SNOOZED
                } else {
                    AlarmStage.FINAL_WARNING_TRIGGERED
                }
            }
            // Main alarm: within user-selected alert time (e.g. 7 min before arrival)
            smoothedMinutes <= alertThresholdMin -> {
                if (currentStage == AlarmStage.SNOOZED) {
                    AlarmStage.SNOOZED
                } else if (currentStage == AlarmStage.FINAL_WARNING_TRIGGERED || currentStage == AlarmStage.ARRIVAL_CONFIRMED) {
                    currentStage
                } else {
                    AlarmStage.MAIN_ALARM_TRIGGERED
                }
            }
            // Early warning: ~15 min before arrival
            smoothedMinutes <= 15 -> {
                if (currentStage == AlarmStage.NO_ALARM) {
                    AlarmStage.EARLY_WARNING_TRIGGERED
                } else {
                    currentStage
                }
            }
            else -> currentStage
        }

        return ETACalculationResult(
            remainingDistanceMeters = directDistanceMeters,
            dynamicEtaMillis = dynamicEtaMillis,
            remainingMinutes = smoothedMinutes,
            etaSource = etaSource,
            etaConfidence = etaConfidence,
            freshnessState = freshnessState,
            movementState = movementState,
            consecutiveReadingsInsideRadius = consecutiveInside,
            isWrongDirection = isWrongDirection,
            isOvershoot = isOvershoot,
            recommendedAlarmStage = recommendedStage
        )
    }

    private fun getDefaultSpeedForMode(mode: TransportMode): Float {
        return when (mode) {
            TransportMode.BUS -> 25f
            TransportMode.TRAIN -> 55f
            TransportMode.METRO -> 40f
            TransportMode.TRAM -> 20f
            TransportMode.FERRY -> 22f
            TransportMode.WALKING -> 4.5f
            TransportMode.CAR_TAXI -> 38f
            else -> 30f
        }
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)

        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }
}
