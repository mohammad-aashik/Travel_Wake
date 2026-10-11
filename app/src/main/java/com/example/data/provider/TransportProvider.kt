package com.example.data.provider

import com.example.model.TransportMode

data class TransitRouteInfo(
    val routeId: String,
    val routeName: String,
    val transportMode: TransportMode,
    val description: String,
    val totalDistanceKm: Double,
    val estimatedDurationMinutes: Int,
    val stopCount: Int,
    val averageSpeedKmh: Float,
    val liveDelayMinutes: Int = 0,
    val hasLiveVehicleTracking: Boolean = false
)

data class TransitStop(
    val stopId: String,
    val stopName: String,
    val latitude: Double,
    val longitude: Double,
    val sequence: Int
)

interface TransportProvider {
    suspend fun getAvailableRoutes(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double,
        mode: TransportMode
    ): List<TransitRouteInfo>

    suspend fun getLiveDelays(routeId: String): Int
}

class DefaultGlobalTransportProvider : TransportProvider {

    override suspend fun getAvailableRoutes(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double,
        mode: TransportMode
    ): List<TransitRouteInfo> {
        val directDistKm = calculateHaversineKm(startLat, startLng, destLat, destLng)

        // Route distance is typically 1.15 to 1.35x direct straight-line distance on transit networks
        val primaryDistKm = directDistKm * 1.22
        val altDistKm = directDistKm * 1.38

        val (primarySpeed, modeName) = when (mode) {
            TransportMode.BUS -> 26f to "Bus Express Line"
            TransportMode.TRAIN -> 58f to "Regional Railway Line"
            TransportMode.METRO -> 42f to "Metro Line A"
            TransportMode.TRAM -> 22f to "City Tramway"
            TransportMode.FERRY -> 24f to "Waterway Ferry Shuttle"
            TransportMode.WALKING -> 4.8f to "Pedestrian Way"
            TransportMode.CAR_TAXI -> 40f to "Highway & Arterial"
            else -> 30f to "Transit Route"
        }

        val primaryDurationMin = ((primaryDistKm / primarySpeed) * 60).toInt().coerceAtLeast(2)
        val altDurationMin = ((altDistKm / (primarySpeed * 0.9f)) * 60).toInt().coerceAtLeast(3)

        val route1 = TransitRouteInfo(
            routeId = "route_direct",
            routeName = "Primary $modeName",
            transportMode = mode,
            description = "Fastest direct trajectory with scheduled priority corridors",
            totalDistanceKm = primaryDistKm,
            estimatedDurationMinutes = primaryDurationMin,
            stopCount = (primaryDistKm / 1.5).toInt().coerceAtLeast(2),
            averageSpeedKmh = primarySpeed,
            liveDelayMinutes = 0,
            hasLiveVehicleTracking = true
        )

        val route2 = TransitRouteInfo(
            routeId = "route_alt",
            routeName = "Secondary / Local $modeName",
            transportMode = mode,
            description = "Alternative urban local line stopping at all interchanges",
            totalDistanceKm = altDistKm,
            estimatedDurationMinutes = altDurationMin,
            stopCount = (altDistKm / 0.8).toInt().coerceAtLeast(4),
            averageSpeedKmh = primarySpeed * 0.85f,
            liveDelayMinutes = 2,
            hasLiveVehicleTracking = false
        )

        return listOf(route1, route2)
    }

    override suspend fun getLiveDelays(routeId: String): Int {
        // Return real or minimal simulated corridor delay
        return if (routeId == "route_alt") 2 else 0
    }

    private fun calculateHaversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
