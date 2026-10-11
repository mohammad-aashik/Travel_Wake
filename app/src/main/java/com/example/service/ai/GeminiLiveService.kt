package com.example.service.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

enum class LiveState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    READY,
    LISTENING,
    SPEAKING,
    ERROR
}

data class LiveTranscriptItem(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "user" or "gemini"
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val isPartial: Boolean = false
)

class GeminiLiveService(private val context: Context) : TextToSpeech.OnInitListener {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val httpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite read for persistent WebSocket
        .build()

    private var webSocket: WebSocket? = null
    private val isRunning = AtomicBoolean(false)

    // State flows
    private val _state = MutableStateFlow(LiveState.DISCONNECTED)
    val state: StateFlow<LiveState> = _state.asStateFlow()

    private val _statusMessage = MutableStateFlow("Tap to start live voice session with Gemini 3.8 Live")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _transcripts = MutableStateFlow<List<LiveTranscriptItem>>(emptyList())
    val transcripts: StateFlow<List<LiveTranscriptItem>> = _transcripts.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    // Audio Playback
    private var audioTrack: AudioTrack? = null
    private val playbackSampleRate = 24000
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // Audio Recording
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val recordSampleRate = 16000

    private val currentAiTurnText = StringBuilder()

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (_: Exception) { }
        initAudioTrack()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsReady = true
        }
    }

    private fun initAudioTrack() {
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                playbackSampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(playbackSampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBufSize, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            audioTrack?.play()
        } catch (_: Exception) { }
    }

    fun startSession() {
        if (_state.value != LiveState.DISCONNECTED && _state.value != LiveState.ERROR) {
            return
        }

        if (!GeminiConfig.isConfigured) {
            _state.value = LiveState.ERROR
            _statusMessage.value = "Gemini API Key missing. Please configure GEMINI_API_KEY in AI Studio Secrets."
            return
        }

        _state.value = LiveState.CONNECTING
        _statusMessage.value = "Connecting to Gemini 3.8 Live API..."

        val request = Request.Builder()
            .url(GeminiConfig.getLiveWebSocketUrl())
            .build()

        isRunning.set(true)
        webSocket = httpClient.newWebSocket(request, createWebSocketListener())
    }

    fun stopSession() {
        isRunning.set(false)
        stopRecording()
        try {
            webSocket?.close(1000, "User ended session")
        } catch (_: Exception) { }
        webSocket = null
        audioTrack?.pause()
        audioTrack?.flush()
        _state.value = LiveState.DISCONNECTED
        _statusMessage.value = "Session disconnected"
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _state.value = LiveState.CONNECTED
            _statusMessage.value = "Connected to Gemini 3.8 Live! Initializing session..."

            // Send BidiGenerateContentSetup message
            sendSetupMessage(webSocket)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleServerMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            _state.value = LiveState.DISCONNECTED
            _statusMessage.value = "Connection closed: $reason"
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _state.value = LiveState.ERROR
            val msg = t.localizedMessage ?: "WebSocket error"
            _statusMessage.value = "Live API Error: $msg"
        }
    }

    private fun sendSetupMessage(ws: WebSocket) {
        try {
            val setupJson = JSONObject().apply {
                put("setup", JSONObject().apply {
                    put("model", "models/${GeminiConfig.MODEL_LIVE}")
                    put("generationConfig", JSONObject().apply {
                        val modalities = JSONArray().apply {
                            put("AUDIO")
                            put("TEXT")
                        }
                        put("responseModalities", modalities)
                        put("speechConfig", JSONObject().apply {
                            put("voiceConfig", JSONObject().apply {
                                put("prebuiltVoiceConfig", JSONObject().apply {
                                    put("voiceName", "Aoede")
                                })
                            })
                        })
                    })
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put(
                                    "text",
                                    "You are TravelWake Live Voice Assistant, powered by Gemini 3.8 Live. You assist travelers on trains, buses, and transit journeys with hands-free route navigation, wake-up alerts, ETA checks, transit stops, and cheerful travel companionship. Keep your spoken answers brief, natural, energetic, and concise (1 to 2 sentences per response)."
                                )
                            })
                        })
                    })
                })
            }
            ws.send(setupJson.toString())
        } catch (e: Exception) {
            _state.value = LiveState.ERROR
            _statusMessage.value = "Failed to send setup message: ${e.message}"
        }
    }

    private fun handleServerMessage(jsonString: String) {
        try {
            val json = JSONObject(jsonString)

            // Setup Complete event
            if (json.has("setupComplete")) {
                _state.value = LiveState.READY
                _statusMessage.value = "Gemini 3.8 Live ready. Tap mic to speak or type below."
                addTranscript("gemini", "Hello! I'm your TravelWake Live companion. How can I assist your journey today?")
                speakWithTtsIfReady("Hello! I'm your TravelWake Live companion. How can I assist your journey today?")
                return
            }

            // Server Content
            val serverContent = json.optJSONObject("serverContent")
            if (serverContent != null) {
                // Interrupted
                if (serverContent.optBoolean("interrupted", false)) {
                    audioTrack?.pause()
                    audioTrack?.flush()
                    tts?.stop()
                    _state.value = LiveState.READY
                    return
                }

                val modelTurn = serverContent.optJSONObject("modelTurn")
                if (modelTurn != null) {
                    _state.value = LiveState.SPEAKING
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)

                            // Text part
                            val text = part.optString("text", "")
                            if (text.isNotBlank()) {
                                currentAiTurnText.append(text)
                                updateCurrentAiTranscript(currentAiTurnText.toString())
                            }

                            // Audio PCM part
                            val inlineData = part.optJSONObject("inlineData")
                            if (inlineData != null) {
                                val mimeType = inlineData.optString("mimeType", "")
                                val base64Data = inlineData.optString("data", "")
                                if (base64Data.isNotBlank()) {
                                    playPcmChunk(base64Data)
                                }
                            }
                        }
                    }
                }

                // Turn Complete
                if (serverContent.optBoolean("turnComplete", false)) {
                    val fullTurnText = currentAiTurnText.toString().trim()
                    if (fullTurnText.isNotBlank()) {
                        // If no audio chunk was received or audioTrack failed, fallback to TTS
                        if (audioTrack == null || audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                            speakWithTtsIfReady(fullTurnText)
                        }
                    }
                    currentAiTurnText.clear()
                    _state.value = if (recordingJob?.isActive == true) LiveState.LISTENING else LiveState.READY
                }
            }
        } catch (_: Exception) { }
    }

    private fun playPcmChunk(base64Data: String) {
        try {
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                initAudioTrack()
            }
            audioTrack?.let { track ->
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    track.play()
                }
                track.write(audioBytes, 0, audioBytes.size)
            }
            _audioLevel.value = 0.8f
        } catch (_: Exception) { }
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return

        if (_state.value == LiveState.DISCONNECTED || _state.value == LiveState.ERROR) {
            startSession()
        }

        addTranscript("user", text)
        _state.value = LiveState.SPEAKING
        _statusMessage.value = "Gemini is thinking..."

        scope.launch {
            // Wait for session readiness if still connecting
            var attempts = 0
            while (_state.value == LiveState.CONNECTING && attempts++ < 20) {
                delay(150)
            }

            try {
                val messageJson = JSONObject().apply {
                    put("clientContent", JSONObject().apply {
                        val turnsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("text", text)
                                    })
                                })
                            })
                        }
                        put("turns", turnsArray)
                        put("turnComplete", true)
                    })
                }
                webSocket?.send(messageJson.toString())
            } catch (e: Exception) {
                _statusMessage.value = "Failed to send: ${e.message}"
            }
        }
    }

    fun startRecording() {
        if (_state.value == LiveState.DISCONNECTED || _state.value == LiveState.ERROR) {
            startSession()
        }

        _state.value = LiveState.LISTENING
        _statusMessage.value = "Listening to your voice..."

        recordingJob?.cancel()
        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                val minBufSize = AudioRecord.getMinBufferSize(
                    recordSampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    recordSampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBufSize, 4096)
                )

                audioRecord?.startRecording()

                val buffer = ByteArray(2048)
                while (isActive && _state.value == LiveState.LISTENING) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readBytes > 0) {
                        // Calculate peak audio amplitude for UI visualizer
                        var maxAmp = 0
                        for (i in 0 until readBytes step 2) {
                            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                            val abs = Math.abs(sample.toShort().toInt())
                            if (abs > maxAmp) maxAmp = abs
                        }
                        _audioLevel.value = (maxAmp / 32767f).coerceIn(0f, 1f)

                        // Send real-time audio chunk to Gemini Live API
                        val pcmChunkBase64 = Base64.encodeToString(buffer, 0, readBytes, Base64.NO_WRAP)
                        val realtimeInputJson = JSONObject().apply {
                            put("realtimeInput", JSONObject().apply {
                                val mediaChunks = JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("mimeType", "audio/pcm;rate=$recordSampleRate")
                                        put("data", pcmChunkBase64)
                                    })
                                }
                                put("mediaChunks", mediaChunks)
                            })
                        }
                        webSocket?.send(realtimeInputJson.toString())
                    }
                    delay(50)
                }
            } catch (e: Exception) {
                _statusMessage.value = "Mic recording note: ${e.message}"
            } finally {
                stopRecording()
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) { }
        audioRecord = null
        _audioLevel.value = 0f
        if (_state.value == LiveState.LISTENING) {
            _state.value = LiveState.READY
            _statusMessage.value = "Microphone paused. Gemini 3.8 Live is ready."
        }
    }

    private fun addTranscript(sender: String, text: String) {
        val list = _transcripts.value.toMutableList()
        list.add(LiveTranscriptItem(sender = sender, text = text))
        _transcripts.value = list
    }

    private fun updateCurrentAiTranscript(text: String) {
        val list = _transcripts.value.toMutableList()
        val lastIndex = list.indexOfLast { it.sender == "gemini" && it.isPartial }
        if (lastIndex >= 0) {
            list[lastIndex] = list[lastIndex].copy(text = text)
        } else {
            list.add(LiveTranscriptItem(sender = "gemini", text = text, isPartial = true))
        }
        _transcripts.value = list
    }

    private fun speakWithTtsIfReady(text: String) {
        if (isTtsReady) {
            try {
                tts?.speak(text, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
            } catch (_: Exception) { }
        }
    }

    fun cleanup() {
        stopSession()
        try {
            audioTrack?.release()
            tts?.shutdown()
        } catch (_: Exception) { }
    }
}
