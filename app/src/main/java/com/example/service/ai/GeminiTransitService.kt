package com.example.service.ai

import com.example.model.DestinationSnapshot
import com.example.model.DestinationType
import com.example.model.TransportMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class GroundedPlace(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val transportMode: TransportMode = TransportMode.BUS,
    val destinationType: DestinationType = DestinationType.BUS_STAND,
    val mapsUrl: String? = null,
    val sourceTitle: String? = null
) {
    fun toDestinationSnapshot(radiusMeters: Float = 200f): DestinationSnapshot {
        return DestinationSnapshot(
            id = id,
            name = name,
            fullAddress = address,
            latitude = latitude,
            longitude = longitude,
            type = destinationType,
            transportMode = transportMode,
            radiusMeters = radiusMeters
        )
    }
}

data class GroundingCitation(
    val title: String,
    val uri: String
)

sealed class GeminiGroundingResult {
    data class Success(
        val responseText: String,
        val places: List<GroundedPlace> = emptyList(),
        val citations: List<GroundingCitation> = emptyList(),
        val searchQueries: List<String> = emptyList(),
        val rawMetadata: String? = null
    ) : GeminiGroundingResult()

    data class Error(val message: String, val isApiKeyMissing: Boolean = false) : GeminiGroundingResult()
}

class GeminiTransitService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Maps Grounding query with gemini-3.5-flash and googleMaps tool
     */
    suspend fun queryWithGoogleMaps(
        prompt: String,
        currentLatitude: Double? = null,
        currentLongitude: Double? = null
    ): GeminiGroundingResult = withContext(Dispatchers.IO) {
        if (!GeminiConfig.isConfigured) {
            return@withContext GeminiGroundingResult.Error(
                "Gemini API Key is not configured. Please add GEMINI_API_KEY to AI Studio Secrets.",
                isApiKeyMissing = true
            )
        }

        try {
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                // Google Maps Grounding Tool
                val toolsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleMaps", JSONObject())
                    })
                }
                put("tools", toolsArray)

                // Optional location context in toolConfig
                if (currentLatitude != null && currentLongitude != null) {
                    put("toolConfig", JSONObject().apply {
                        put("googleMaps", JSONObject().apply {
                            put("location", JSONObject().apply {
                                put("latitude", currentLatitude)
                                put("longitude", currentLongitude)
                            })
                        })
                    })
                }

                // System Instruction to format station & coordinate details
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put(
                                "text",
                                "You are TravelWake's transit navigation expert. Always use the Google Maps tool to ground places, train stations, bus stops, ferry terminals, and transit hubs. When mentioning transit destinations or places, clearly provide their exact name, address, latitude, and longitude (e.g. Coordinates: [lat, lng]) so travelers can set arrival wake alarms accurately."
                            )
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(GeminiConfig.getRestUrl(GeminiConfig.MODEL_FLASH))
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext parseApiError(responseBody, response.code)
            }

            parseGroundingResponse(responseBody, prompt)
        } catch (e: Exception) {
            GeminiGroundingResult.Error("Network error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Search Grounding query with gemini-3.5-flash and googleSearch tool
     */
    suspend fun queryWithGoogleSearch(
        prompt: String
    ): GeminiGroundingResult = withContext(Dispatchers.IO) {
        if (!GeminiConfig.isConfigured) {
            return@withContext GeminiGroundingResult.Error(
                "Gemini API Key is not configured. Please add GEMINI_API_KEY to AI Studio Secrets.",
                isApiKeyMissing = true
            )
        }

        try {
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                // Google Search Grounding Tool
                val toolsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                }
                put("tools", toolsArray)

                // System Instruction for up-to-date transit schedules, delays, weather
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put(
                                "text",
                                "You are TravelWake's real-time transit status and advisory companion. Always use Google Search grounding to retrieve current live schedules, train/bus delays, track maintenance, strike notices, route advisories, and weather forecasts. Cite sources and provide concise commuter tips."
                            )
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(GeminiConfig.getRestUrl(GeminiConfig.MODEL_FLASH))
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext parseApiError(responseBody, response.code)
            }

            parseGroundingResponse(responseBody, prompt)
        } catch (e: Exception) {
            GeminiGroundingResult.Error("Network error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Combined Grounding query with gemini-3.5-flash using both Google Maps & Google Search tools
     */
    suspend fun queryCombinedGrounding(
        prompt: String,
        currentLatitude: Double? = null,
        currentLongitude: Double? = null
    ): GeminiGroundingResult = withContext(Dispatchers.IO) {
        if (!GeminiConfig.isConfigured) {
            return@withContext GeminiGroundingResult.Error(
                "Gemini API Key is not configured. Please add GEMINI_API_KEY to AI Studio Secrets.",
                isApiKeyMissing = true
            )
        }

        try {
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                val toolsArray = JSONArray().apply {
                    put(JSONObject().apply { put("googleMaps", JSONObject()) })
                    put(JSONObject().apply { put("googleSearch", JSONObject()) })
                }
                put("tools", toolsArray)

                if (currentLatitude != null && currentLongitude != null) {
                    put("toolConfig", JSONObject().apply {
                        put("googleMaps", JSONObject().apply {
                            put("location", JSONObject().apply {
                                put("latitude", currentLatitude)
                                put("longitude", currentLongitude)
                            })
                        })
                    })
                }

                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put(
                                "text",
                                "You are TravelWake's combined transit intelligence engine. Leverage both Google Maps and Google Search grounding to deliver verified transit hub locations, geo-coordinates, real-time operating conditions, delays, and passenger advice."
                            )
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(GeminiConfig.getRestUrl(GeminiConfig.MODEL_FLASH))
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext parseApiError(responseBody, response.code)
            }

            parseGroundingResponse(responseBody, prompt)
        } catch (e: Exception) {
            GeminiGroundingResult.Error("Network error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun parseApiError(responseBody: String, statusCode: Int): GeminiGroundingResult.Error {
        return try {
            val json = JSONObject(responseBody)
            val errorObj = json.optJSONObject("error")
            val message = errorObj?.optString("message") ?: "HTTP error $statusCode"
            GeminiGroundingResult.Error("Gemini API Error ($statusCode): $message")
        } catch (_: Exception) {
            GeminiGroundingResult.Error("Gemini API Error ($statusCode): $responseBody")
        }
    }

    private fun parseGroundingResponse(responseBody: String, originalPrompt: String): GeminiGroundingResult {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            return GeminiGroundingResult.Error("No response candidates returned from Gemini.")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")

        val textBuilder = StringBuilder()
        if (parts != null) {
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                textBuilder.append(part.optString("text", ""))
            }
        }
        val fullText = textBuilder.toString()

        val citations = mutableListOf<GroundingCitation>()
        val searchQueries = mutableListOf<String>()
        val places = mutableListOf<GroundedPlace>()

        val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
        if (groundingMetadata != null) {
            // Search queries
            val queriesArray = groundingMetadata.optJSONArray("webSearchQueries")
            if (queriesArray != null) {
                for (i in 0 until queriesArray.length()) {
                    searchQueries.add(queriesArray.getString(i))
                }
            }

            // Grounding chunks (both web and maps)
            val chunks = groundingMetadata.optJSONArray("groundingChunks")
            if (chunks != null) {
                for (i in 0 until chunks.length()) {
                    val chunk = chunks.getJSONObject(i)
                    val web = chunk.optJSONObject("web")
                    if (web != null) {
                        val title = web.optString("title", "Web Source")
                        val uri = web.optString("uri", "")
                        if (uri.isNotBlank()) {
                            citations.add(GroundingCitation(title, uri))
                        }
                    }

                    val maps = chunk.optJSONObject("maps")
                    if (maps != null) {
                        val title = maps.optString("title", "Maps Location")
                        val uri = maps.optString("uri", "")
                        val address = maps.optString("address", title)
                        val lat = maps.optDouble("latitude", Double.NaN)
                        val lng = maps.optDouble("longitude", Double.NaN)

                        if (!lat.isNaN() && !lng.isNaN()) {
                            places.add(
                                GroundedPlace(
                                    name = title,
                                    address = address,
                                    latitude = lat,
                                    longitude = lng,
                                    mapsUrl = uri.ifBlank { null }
                                )
                            )
                        } else if (uri.isNotBlank()) {
                            citations.add(GroundingCitation(title, uri))
                        }
                    }
                }
            }
        }

        // Also extract coordinates if present in the text (e.g. "Coordinates: 6.9344, 79.8503" or "[6.9344, 79.8503]")
        extractPlacesFromText(fullText, places, originalPrompt)

        return GeminiGroundingResult.Success(
            responseText = fullText,
            places = places.distinctBy { "${it.name}_${"%.3f".format(it.latitude)}_${"%.3f".format(it.longitude)}" },
            citations = citations.distinctBy { it.uri },
            searchQueries = searchQueries,
            rawMetadata = groundingMetadata?.toString()
        )
    }

    private fun extractPlacesFromText(
        text: String,
        places: MutableList<GroundedPlace>,
        queryContext: String
    ) {
        // Pattern for latitude, longitude coordinates
        val coordPattern = Pattern.compile("(?i)(?:coords?|coordinates?|lat(?:itude)?/long(?:itude)?|location)?[^0-9-]*(-?\\d{1,2}\\.\\d{3,7})\\s*[,/ ]\\s*(-?\\d{1,3}\\.\\d{3,7})")
        val matcher = coordPattern.matcher(text)

        var foundIndex = 0
        while (matcher.find()) {
            try {
                val lat = matcher.group(1)?.toDoubleOrNull() ?: continue
                val lng = matcher.group(2)?.toDoubleOrNull() ?: continue

                if (lat in -90.0..90.0 && lng in -180.0..180.0) {
                    val surroundingText = text.substring(
                        maxOf(0, matcher.start() - 80),
                        minOf(text.length, matcher.end() + 80)
                    )

                    // Find plausible place name in surrounding text
                    val placeName = extractPlausibleName(surroundingText) ?: "Transit Hub #${++foundIndex}"
                    val mode = when {
                        placeName.contains("train", true) || placeName.contains("station", true) || placeName.contains("railway", true) -> TransportMode.TRAIN
                        placeName.contains("metro", true) || placeName.contains("subway", true) || placeName.contains("mrt", true) -> TransportMode.METRO
                        placeName.contains("ferry", true) || placeName.contains("boat", true) -> TransportMode.FERRY
                        else -> TransportMode.BUS
                    }

                    val type = when (mode) {
                        TransportMode.TRAIN -> DestinationType.RAILWAY_STATION
                        TransportMode.METRO -> DestinationType.METRO_STATION
                        TransportMode.FERRY -> DestinationType.FERRY_TERMINAL
                        else -> DestinationType.BUS_STAND
                    }

                    places.add(
                        GroundedPlace(
                            name = placeName,
                            address = "Grounded via Google Maps & Gemini: $placeName",
                            latitude = lat,
                            longitude = lng,
                            transportMode = mode,
                            destinationType = type
                        )
                    )
                }
            } catch (_: Exception) { }
        }
    }

    private fun extractPlausibleName(snippet: String): String? {
        val lines = snippet.lines()
        for (line in lines) {
            val trimmed = line.trim().removePrefix("-").removePrefix("*").trim()
            if (trimmed.length in 5..50 && (trimmed.contains("station", true) || trimmed.contains("stand", true) || trimmed.contains("terminal", true) || trimmed.contains("airport", true) || trimmed.contains("stop", true))) {
                return trimmed
            }
        }
        return null
    }
}
