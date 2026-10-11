package com.example.model

/**
 * Represents a public transport route (bus route, train line, metro corridor, etc.)
 */
data class TransitRoute(
    val id: String,
    val routeNumber: String,
    val name: String,
    val mode: TransportMode,
    val countryCode: String,
    val cityName: String,
    val originTerminal: String,
    val destinationTerminal: String,
    val frequencyDescription: String = "Regular service",
    val operatingHours: String = "05:00 - 23:00",
    val majorStops: List<TransitRouteStop> = emptyList(),
    val description: String = ""
)

data class TransitRouteStop(
    val stopId: String,
    val name: String,
    val sequenceNumber: Int,
    val latitude: Double,
    val longitude: Double,
    val type: DestinationType,
    val isMajorHub: Boolean = false,
    val fullAddress: String = ""
)
