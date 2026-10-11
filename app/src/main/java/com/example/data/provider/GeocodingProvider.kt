package com.example.data.provider

import com.example.model.DestinationType

data class GeocodeResult(
    val placeId: String,
    val name: String,
    val fullDisplayName: String,
    val latitude: Double,
    val longitude: Double,
    val type: DestinationType,
    val city: String = "",
    val country: String = "",
    val distanceKmFromCurrent: Double? = null
)

interface GeocodingProvider {
    suspend fun searchPlaces(
        query: String,
        userLat: Double?,
        userLng: Double?,
        preferredCountryCode: String? = null,
        searchWorldwide: Boolean = false
    ): List<GeocodeResult>
    suspend fun reverseGeocode(latitude: Double, longitude: Double): GeocodeResult?
}
