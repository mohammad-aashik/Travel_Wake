package com.example.data.provider

import com.example.model.DestinationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class NominatimGeocodingProvider : GeocodingProvider {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Curated global landmarks and public transport hubs across all continents
    private val globalTransitHubs = listOf(
        // Asia & Sri Lanka (Primary Local Hubs)
        GeocodeResult("hub_sl_1", "Colombo Fort Railway Station", "Colombo Fort Railway Station, Olcott Mawatha, Colombo, Sri Lanka", 6.9344, 79.8503, DestinationType.RAILWAY_STATION, "Colombo", "Sri Lanka"),
        GeocodeResult("hub_sl_2", "Bastian Mawatha Bus Station", "Bastian Mawatha Private Bus Stand, Colombo 11, Sri Lanka", 6.9351, 79.8550, DestinationType.BUS_STAND, "Colombo", "Sri Lanka"),
        GeocodeResult("hub_sl_3", "Pettah Central Bus Stand", "Central Bus Stand (CTB), Olcott Mawatha, Pettah, Colombo, Sri Lanka", 6.9360, 79.8530, DestinationType.BUS_STAND, "Colombo", "Sri Lanka"),
        GeocodeResult("hub_sl_4", "Kandy Railway Station", "Kandy Railway Station, William Gopallawa Mawatha, Kandy, Sri Lanka", 7.2906, 80.6289, DestinationType.RAILWAY_STATION, "Kandy", "Sri Lanka"),
        GeocodeResult("hub_sl_5", "Galle Railway Station", "Galle Railway Station, Main Street, Galle, Sri Lanka", 6.0353, 80.2158, DestinationType.RAILWAY_STATION, "Galle", "Sri Lanka"),
        GeocodeResult("hub_sl_6", "Jaffna Railway Station", "Jaffna Railway Station, Station Road, Jaffna, Sri Lanka", 9.6644, 80.0244, DestinationType.RAILWAY_STATION, "Jaffna", "Sri Lanka"),
        GeocodeResult("hub_sl_7", "Bandaranaike International Airport", "Bandaranaike Airport (CMB), Katunayake, Sri Lanka", 7.1808, 79.8841, DestinationType.AIRPORT, "Katunayake", "Sri Lanka"),
        GeocodeResult("hub_sl_8", "Negombo Bus Stand", "Negombo Central Bus Terminal, Main Street, Negombo, Sri Lanka", 7.2089, 79.8358, DestinationType.BUS_STAND, "Negombo", "Sri Lanka"),
        GeocodeResult("hub_sl_9", "Gampaha Railway Station", "Gampaha Station, Station Road, Gampaha, Sri Lanka", 7.0911, 79.9998, DestinationType.RAILWAY_STATION, "Gampaha", "Sri Lanka"),
        GeocodeResult("hub_sl_10", "Matara Railway Station", "Matara Railway Terminal, Station Rd, Matara, Sri Lanka", 5.9482, 80.5488, DestinationType.RAILWAY_STATION, "Matara", "Sri Lanka"),
        GeocodeResult("hub_sl_11", "Kurunegala Central Bus Stand", "Kurunegala Bus Stand, Puttalam Rd, Kurunegala, Sri Lanka", 7.4863, 80.3623, DestinationType.BUS_STAND, "Kurunegala", "Sri Lanka"),
        GeocodeResult("hub_sl_12", "Anuradhapura Railway Station", "Anuradhapura Town Station, Station Rd, Anuradhapura, Sri Lanka", 8.3444, 80.4037, DestinationType.RAILWAY_STATION, "Anuradhapura", "Sri Lanka"),

        // Other Asia & India Hubs
        GeocodeResult("hub_1", "Tokyo Station", "Tokyo Station, Chiyoda City, Tokyo, Japan", 35.681236, 139.767125, DestinationType.RAILWAY_STATION, "Tokyo", "Japan"),
        GeocodeResult("hub_2", "Shinjuku Station", "Shinjuku Station, Shinjuku, Tokyo, Japan", 35.689607, 139.700571, DestinationType.METRO_STATION, "Tokyo", "Japan"),
        GeocodeResult("hub_3", "Singapore Changi Airport", "Changi Airport (SIN), Airport Blvd, Singapore", 1.364420, 103.991531, DestinationType.AIRPORT, "Singapore", "Singapore"),
        GeocodeResult("hub_4", "Dhoby Ghaut MRT", "Dhoby Ghaut Interchange, Orchard Rd, Singapore", 1.299066, 103.845763, DestinationType.METRO_STATION, "Singapore", "Singapore"),
        GeocodeResult("hub_8", "New Delhi Railway Station", "New Delhi Railway Station, Bhavbhuti Marg, Delhi, India", 28.6427, 77.2195, DestinationType.RAILWAY_STATION, "Delhi", "India"),
        GeocodeResult("hub_9", "Chennai Central", "Puratchi Thalaivar Dr. M.G.R. Central Railway Station, Chennai, India", 13.0827, 80.2707, DestinationType.RAILWAY_STATION, "Chennai", "India"),
        GeocodeResult("hub_ind_1", "Mumbai CSMT", "Chhatrapati Shivaji Maharaj Terminus, Fort, Mumbai, India", 18.9401, 72.8356, DestinationType.RAILWAY_STATION, "Mumbai", "India"),
        GeocodeResult("hub_ind_2", "Bengaluru Majestic (KSR)", "Krantivira Sangolli Rayanna Bengaluru Station, Majestic, Bengaluru, India", 12.9774, 77.5694, DestinationType.RAILWAY_STATION, "Bengaluru", "India"),
        GeocodeResult("hub_10", "Hong Kong Central Station", "Central Station, Connaught Road Central, Hong Kong", 22.2820, 114.1582, DestinationType.METRO_STATION, "Hong Kong", "Hong Kong"),
        GeocodeResult("hub_11", "Seoul Station", "Seoul Station, Tongil-ro, Jung-gu, Seoul, South Korea", 37.5546, 126.9706, DestinationType.RAILWAY_STATION, "Seoul", "South Korea"),
        GeocodeResult("hub_12", "KL Sentral", "Kuala Lumpur Sentral, Jalan Stesen Sentral, KL, Malaysia", 3.1342, 101.6865, DestinationType.RAILWAY_STATION, "Kuala Lumpur", "Malaysia"),
        GeocodeResult("hub_13", "Bangkok Krung Thep Aphiwat", "Krung Thep Aphiwat Central Terminal, Chatuchak, Bangkok, Thailand", 13.8038, 100.5404, DestinationType.RAILWAY_STATION, "Bangkok", "Thailand"),
        GeocodeResult("hub_14", "Dubai Mall Metro Station", "Burj Khalifa / Dubai Mall Metro Station, Sheikh Zayed Rd, Dubai, UAE", 25.1995, 55.2773, DestinationType.METRO_STATION, "Dubai", "United Arab Emirates"),

        // Europe
        GeocodeResult("hub_15", "Gare du Nord", "Gare du Nord, Rue de Dunkerque, 75010 Paris, France", 48.8809, 2.3553, DestinationType.RAILWAY_STATION, "Paris", "France"),
        GeocodeResult("hub_16", "London King's Cross", "King's Cross Station, Euston Road, London N1 9AL, UK", 51.5308, -0.1238, DestinationType.RAILWAY_STATION, "London", "United Kingdom"),
        GeocodeResult("hub_17", "London Victoria Station", "Victoria Station, Victoria St, London SW1E 5ND, UK", 51.4952, -0.1439, DestinationType.RAILWAY_STATION, "London", "United Kingdom"),
        GeocodeResult("hub_18", "Berlin Hauptbahnhof", "Berlin Hauptbahnhof, Europaplatz 1, 10557 Berlin, Germany", 52.5251, 13.3694, DestinationType.RAILWAY_STATION, "Berlin", "Germany"),
        GeocodeResult("hub_19", "Amsterdam Centraal", "Amsterdam Centraal, Stationsplein, 1012 AB Amsterdam, Netherlands", 52.3791, 4.9003, DestinationType.RAILWAY_STATION, "Amsterdam", "Netherlands"),
        GeocodeResult("hub_20", "Zürich Hauptbahnhof", "Zürich HB, Bahnhofplatz, 8001 Zürich, Switzerland", 47.3779, 8.5403, DestinationType.RAILWAY_STATION, "Zurich", "Switzerland"),
        GeocodeResult("hub_21", "Milano Centrale", "Milano Centrale, Piazza Duca d'Aosta, 20124 Milano, Italy", 45.4862, 9.2045, DestinationType.RAILWAY_STATION, "Milan", "Italy"),
        GeocodeResult("hub_22", "Madrid Atocha", "Estación de Madrid-Atocha, Plaza del Emperador Carlos V, Madrid, Spain", 40.4068, -3.6908, DestinationType.RAILWAY_STATION, "Madrid", "Spain"),

        // Americas
        GeocodeResult("hub_23", "New York Penn Station", "Pennsylvania Station, 8th Ave, New York, NY 10001, USA", 40.7505, -73.9934, DestinationType.RAILWAY_STATION, "New York", "United States"),
        GeocodeResult("hub_24", "Grand Central Terminal", "Grand Central Terminal, 89 E 42nd St, New York, NY 10017, USA", 40.7527, -73.9772, DestinationType.RAILWAY_STATION, "New York", "United States"),
        GeocodeResult("hub_25", "Union Station Toronto", "Union Station, 65 Front St W, Toronto, ON M5J 1E6, Canada", 43.6453, -79.3806, DestinationType.RAILWAY_STATION, "Toronto", "Canada"),
        GeocodeResult("hub_26", "Chicago Union Station", "Union Station, 225 S Canal St, Chicago, IL 60606, USA", 41.8787, -87.6403, DestinationType.RAILWAY_STATION, "Chicago", "United States"),
        GeocodeResult("hub_27", "San Francisco Ferry Building", "San Francisco Ferry Building, 1 Ferry Building, San Francisco, CA, USA", 37.7955, -122.3937, DestinationType.FERRY_TERMINAL, "San Francisco", "United States"),
        GeocodeResult("hub_28", "Estação da Sé", "Estação da Sé, Praça da Sé, São Paulo, Brazil", -23.5505, -46.6333, DestinationType.METRO_STATION, "São Paulo", "Brazil"),

        // Oceania
        GeocodeResult("hub_29", "Sydney Central Station", "Central Station, Eddy Ave, Haymarket NSW 2000, Australia", -33.8832, 151.2070, DestinationType.RAILWAY_STATION, "Sydney", "Australia"),
        GeocodeResult("hub_30", "Melbourne Flinders Street", "Flinders Street Railway Station, Melbourne VIC 3000, Australia", -37.8180, 144.9671, DestinationType.RAILWAY_STATION, "Melbourne", "Australia"),
        GeocodeResult("hub_31", "Auckland Britomart", "Waitematā (Britomart) Railway Station, Auckland 1010, New Zealand", -36.8443, 174.7684, DestinationType.RAILWAY_STATION, "Auckland", "New Zealand"),

        // Africa
        GeocodeResult("hub_32", "Cape Town Station", "Cape Town Railway Station, Adderley St, Cape Town, South Africa", -33.9221, 18.4231, DestinationType.RAILWAY_STATION, "Cape Town", "South Africa"),
        GeocodeResult("hub_33", "Ramses Railway Station", "Ramses Station, Al Shohadaa, Cairo, Egypt", 30.0632, 31.2468, DestinationType.RAILWAY_STATION, "Cairo", "Egypt")
    )

    override suspend fun searchPlaces(
        query: String,
        userLat: Double?,
        userLng: Double?,
        preferredCountryCode: String?,
        searchWorldwide: Boolean
    ): List<GeocodeResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        val countryCode = preferredCountryCode?.trim()?.lowercase()

        if (trimmed.isEmpty()) {
            val prioritizedList = (if (!countryCode.isNullOrEmpty() && !searchWorldwide) {
                val inCountry = globalTransitHubs.filter { it.country.equals(getCountryName(countryCode), ignoreCase = true) }
                val others = globalTransitHubs.filter { !it.country.equals(getCountryName(countryCode), ignoreCase = true) }
                (inCountry + others)
            } else {
                globalTransitHubs
            }).distinctBy { it.placeId }
              .distinctBy { "${it.name.trim().lowercase()}_${it.city.trim().lowercase()}" }

            return@withContext prioritizedList.take(8).map { hub ->
                hub.copy(
                    distanceKmFromCurrent = calculateDistanceKm(userLat, userLng, hub.latitude, hub.longitude)
                )
            }
        }

        val networkResults = mutableListOf<GeocodeResult>()

        fun executeSearch(countryFilter: String?): List<GeocodeResult> {
            val results = mutableListOf<GeocodeResult>()
            try {
                val encoded = URLEncoder.encode(trimmed, "UTF-8")
                val countryParam = if (!countryFilter.isNullOrEmpty()) "&countrycodes=$countryFilter" else ""
                val url = "https://nominatim.openstreetmap.org/search?q=$encoded$countryParam&format=json&addressdetails=1&limit=10"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "TravelWake-Android-App/1.0")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val array = JSONArray(bodyString)
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val lat = obj.optDouble("lat", 0.0)
                            val lon = obj.optDouble("lon", 0.0)
                            val displayName = obj.optString("display_name", "")
                            val addressObj = obj.optJSONObject("address")
                            val city = addressObj?.optString("city")
                                ?: addressObj?.optString("town")
                                ?: addressObj?.optString("municipality")
                                ?: addressObj?.optString("county")
                                ?: ""
                            val country = addressObj?.optString("country", "") ?: ""
                            val typeStr = obj.optString("type", "")
                            val classStr = obj.optString("class", "")

                            val destType = inferDestinationType(classStr, typeStr, displayName)
                            val name = displayName.split(",").firstOrNull()?.trim() ?: displayName

                            results.add(
                                GeocodeResult(
                                    placeId = "osm_${obj.optString("place_id", "$i")}",
                                    name = name,
                                    fullDisplayName = displayName,
                                    latitude = lat,
                                    longitude = lon,
                                    type = destType,
                                    city = city,
                                    country = country,
                                    distanceKmFromCurrent = calculateDistanceKm(userLat, userLng, lat, lon)
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore network error and fall back
            }
            return results
        }

        if (!countryCode.isNullOrEmpty() && !searchWorldwide) {
            // First search within the prioritized country
            val inCountry = executeSearch(countryCode)
            networkResults.addAll(inCountry)

            // If few or no results, fallback to worldwide search so user never gets stuck
            if (networkResults.size < 3) {
                val global = executeSearch(null)
                networkResults.addAll(global)
            }
        } else {
            networkResults.addAll(executeSearch(null))
        }

        // Search local curated hubs with country awareness
        val localMatches = globalTransitHubs.filter { hub ->
            hub.name.contains(trimmed, ignoreCase = true) ||
            hub.fullDisplayName.contains(trimmed, ignoreCase = true) ||
            hub.city.contains(trimmed, ignoreCase = true) ||
            hub.country.contains(trimmed, ignoreCase = true)
        }.map { hub ->
            hub.copy(
                distanceKmFromCurrent = calculateDistanceKm(userLat, userLng, hub.latitude, hub.longitude)
            )
        }

        // Combine and prioritize preferred country without duplicate items
        val combined = (networkResults + localMatches)
            .distinctBy { it.placeId }
            .distinctBy { "${it.name.trim().lowercase()}_${it.city.trim().lowercase()}" }
            .distinctBy { "${(it.latitude * 1000).toInt()}_${(it.longitude * 1000).toInt()}" }

        if (combined.isNotEmpty()) {
            val prefCountryName = countryCode?.let { getCountryName(it) }
            val sorted = combined.sortedWith(
                compareByDescending<GeocodeResult> {
                    // Match preferred country first if not worldwide search
                    if (!searchWorldwide && prefCountryName != null && it.country.contains(prefCountryName, ignoreCase = true)) 1 else 0
                }.thenBy {
                    it.distanceKmFromCurrent ?: Double.MAX_VALUE
                }
            )
            sorted
        } else {
            // Return closest or preferred curated hubs
            val fallback = if (!countryCode.isNullOrEmpty()) {
                globalTransitHubs.filter { it.country.equals(getCountryName(countryCode), ignoreCase = true) }.take(5)
            } else {
                globalTransitHubs.take(5)
            }
            fallback.map { hub ->
                hub.copy(
                    distanceKmFromCurrent = calculateDistanceKm(userLat, userLng, hub.latitude, hub.longitude)
                )
            }
        }
    }

    private fun getCountryName(code: String): String {
        return when (code.lowercase()) {
            "lk" -> "Sri Lanka"
            "in" -> "India"
            "gb", "uk" -> "United Kingdom"
            "sg" -> "Singapore"
            "au" -> "Australia"
            "us" -> "United States"
            "jp" -> "Japan"
            "de" -> "Germany"
            "fr" -> "France"
            "ca" -> "Canada"
            "ae" -> "United Arab Emirates"
            "my" -> "Malaysia"
            "th" -> "Thailand"
            else -> code
        }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): GeocodeResult? = withContext(Dispatchers.IO) {
        try {
            val url = "https://nominatim.openstreetmap.org/reverse?lat=$latitude&lon=$longitude&format=json&addressdetails=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TravelWake-Android-App/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val obj = org.json.JSONObject(body)
                    val displayName = obj.optString("display_name", "")
                    val addressObj = obj.optJSONObject("address")
                    val city = addressObj?.optString("city")
                        ?: addressObj?.optString("town")
                        ?: addressObj?.optString("suburb")
                        ?: ""
                    val country = addressObj?.optString("country", "") ?: ""
                    val name = displayName.split(",").firstOrNull()?.trim() ?: "Location (${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)})"

                    return@withContext GeocodeResult(
                        placeId = "rev_${obj.optString("place_id", "0")}",
                        name = name,
                        fullDisplayName = displayName,
                        latitude = latitude,
                        longitude = longitude,
                        type = DestinationType.EXACT_LOCATION,
                        city = city,
                        country = country,
                        distanceKmFromCurrent = 0.0
                    )
                }
            }
        } catch (_: Exception) {
            // Fallback
        }

        // Fallback to closest known hub or coordinate label
        val closest = globalTransitHubs.minByOrNull {
            calculateDistanceKm(latitude, longitude, it.latitude, it.longitude) ?: Double.MAX_VALUE
        }
        val dist = closest?.let { calculateDistanceKm(latitude, longitude, it.latitude, it.longitude) } ?: 999.0
        if (dist < 1.0 && closest != null) {
            return@withContext closest.copy(distanceKmFromCurrent = dist)
        }

        GeocodeResult(
            placeId = "coord_${latitude}_${longitude}",
            name = "Point (${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)})",
            fullDisplayName = "Lat: $latitude, Lng: $longitude",
            latitude = latitude,
            longitude = longitude,
            type = DestinationType.EXACT_LOCATION,
            distanceKmFromCurrent = 0.0
        )
    }

    private fun inferDestinationType(classStr: String, typeStr: String, text: String): DestinationType {
        val lower = "$classStr $typeStr $text".lowercase()
        return when {
            lower.contains("subway") || lower.contains("metro") || lower.contains("underground") -> DestinationType.METRO_STATION
            lower.contains("railway") || lower.contains("train") || lower.contains("station") -> DestinationType.RAILWAY_STATION
            lower.contains("bus_stop") || lower.contains("bus stop") -> DestinationType.BUS_STOP
            lower.contains("bus") || lower.contains("terminal") -> DestinationType.BUS_STAND
            lower.contains("tram") -> DestinationType.TRAM_STOP
            lower.contains("ferry") || lower.contains("boat") || lower.contains("port") -> DestinationType.FERRY_TERMINAL
            lower.contains("airport") || lower.contains("aerodrome") -> DestinationType.AIRPORT
            lower.contains("university") || lower.contains("college") || lower.contains("campus") -> DestinationType.UNIVERSITY
            lower.contains("office") || lower.contains("workplace") -> DestinationType.WORKPLACE
            else -> DestinationType.LANDMARK
        }
    }

    private fun calculateDistanceKm(lat1: Double?, lon1: Double?, lat2: Double, lon2: Double): Double? {
        if (lat1 == null || lon1 == null) return null
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
