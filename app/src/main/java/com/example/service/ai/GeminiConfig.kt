package com.example.service.ai

import com.example.BuildConfig

object GeminiConfig {
    const val MODEL_FLASH = "gemini-3.5-flash"
    const val MODEL_LIVE = "gemini-3.8-live"

    private const val BASE_REST_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val BASE_WS_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"

    val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

    fun getRestUrl(model: String = MODEL_FLASH): String {
        return "$BASE_REST_URL/$model:generateContent?key=$apiKey"
    }

    fun getLiveWebSocketUrl(): String {
        return "$BASE_WS_URL?key=$apiKey"
    }
}
