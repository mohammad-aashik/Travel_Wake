package com.example.service.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Represents a ringtone option for TravelWake arrival wake-up alarm.
 */
data class RingtoneOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String, // "Transit Themes", "Device Alarms", "Custom Sounds"
    val uriString: String? = null,
    val isCustom: Boolean = false
)

/**
 * Manages ringtone previewing, selection, and playback for TravelWake.
 * Supports synthesized transit alarms (no binary file overhead),
 * Android system alarm ringtones, and custom user-selected audio files.
 */
object AlarmSoundManager {

    private const val TAG = "AlarmSoundManager"
    private const val SAMPLE_RATE = 22050

    // Curated Transit Alarm Themes
    val TRANSIT_PRESETS = listOf(
        RingtoneOption(
            id = "metro_chime",
            title = "Metro Door Chime",
            subtitle = "Pleasant dual-bell ding-dong transit chime",
            category = "Transit Themes"
        ),
        RingtoneOption(
            id = "express_horn",
            title = "Express Train Horn & Bell",
            subtitle = "Dual-tone train horn with rhythmic alert pulse",
            category = "Transit Themes"
        ),
        RingtoneOption(
            id = "rapid_siren",
            title = "Rapid High-Pitch Pulse",
            subtitle = "High-frequency penetrating alert for heavy sleepers",
            category = "Transit Themes"
        ),
        RingtoneOption(
            id = "gentle_radar",
            title = "Gentle Sunrise Radar",
            subtitle = "Ascending melodic arpeggio for gradual wake-up",
            category = "Transit Themes"
        ),
        RingtoneOption(
            id = "digital_beacon",
            title = "Digital Transit Beacon",
            subtitle = "Frequency-modulated modern electronic beacon",
            category = "Transit Themes"
        ),
        RingtoneOption(
            id = "heavy_sleeper",
            title = "High-Intensity Wake Siren",
            subtitle = "Piercing alternating frequencies for deep sleepers",
            category = "Transit Themes"
        ),
        RingtoneOption(
            id = "system_default",
            title = "Default System Alarm",
            subtitle = "Your phone's standard Android alarm alert",
            category = "System Alarms"
        )
    )

    private val _isPlayingPreview = MutableStateFlow(false)
    val isPlayingPreview: StateFlow<Boolean> = _isPlayingPreview.asStateFlow()

    private val _currentPlayingId = MutableStateFlow<String?>(null)
    val currentPlayingId: StateFlow<String?> = _currentPlayingId.asStateFlow()

    private var activeAudioTrack: AudioTrack? = null
    private var activeRingtone: Ringtone? = null
    private var activeMediaPlayer: MediaPlayer? = null
    private var previewJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Retrieves all available ringtones, combining transit presets and installed system alarms.
     */
    fun getAvailableRingtones(context: Context): List<RingtoneOption> {
        val all = mutableListOf<RingtoneOption>()
        all.addAll(TRANSIT_PRESETS)

        // Query device system alarms
        try {
            val manager = RingtoneManager(context)
            manager.setType(RingtoneManager.TYPE_ALARM)
            val cursor = manager.cursor
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: "Alarm Tone"
                val uri = manager.getRingtoneUri(cursor.position)
                if (uri != null) {
                    all.add(
                        RingtoneOption(
                            id = "device_alarm_${cursor.position}",
                            title = title,
                            subtitle = "System Alarm Tone",
                            category = "Device Alarms",
                            uriString = uri.toString(),
                            isCustom = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load system alarms: ${e.message}")
        }

        return all
    }

    /**
     * Plays a preview of the specified ringtone option for the user.
     * Automatically times out after 7 seconds if not stopped.
     */
    fun playPreview(
        context: Context,
        option: RingtoneOption,
        volume: Float = 0.9f
    ) {
        stopAudio()

        _isPlayingPreview.value = true
        _currentPlayingId.value = option.id

        previewJob = scope.launch {
            try {
                if (option.uriString != null) {
                    playUriSound(context, Uri.parse(option.uriString), volume, loop = true)
                } else if (option.id == "system_default") {
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    playUriSound(context, defaultUri, volume, loop = true)
                } else {
                    playSynthesizedTone(option.id, volume, loop = true)
                }

                // Automatically stop preview after 7 seconds
                delay(7000)
                stopAudio()
            } catch (e: Exception) {
                Log.e(TAG, "Error playing preview: ${e.message}", e)
                stopAudio()
            }
        }
    }

    /**
     * Plays full alarm playback (e.g., during active arrival or test alarm).
     */
    fun playAlarm(
        context: Context,
        ringtoneId: String,
        uriString: String? = null,
        volume: Float = 0.9f
    ) {
        stopAudio()
        _isPlayingPreview.value = true
        _currentPlayingId.value = ringtoneId

        previewJob = scope.launch {
            try {
                if (!uriString.isNullOrBlank()) {
                    playUriSound(context, Uri.parse(uriString), volume, loop = true)
                } else if (ringtoneId == "system_default") {
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    playUriSound(context, defaultUri, volume, loop = true)
                } else {
                    playSynthesizedTone(ringtoneId, volume, loop = true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing alarm sound: ${e.message}", e)
                stopAudio()
            }
        }
    }

    /**
     * Stops all active audio previews or alarm sound tracks.
     */
    fun stopAudio() {
        previewJob?.cancel()
        previewJob = null

        try {
            activeRingtone?.stop()
            activeRingtone = null
        } catch (_: Exception) {}

        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
            activeMediaPlayer = null
        } catch (_: Exception) {}

        try {
            activeAudioTrack?.stop()
            activeAudioTrack?.release()
            activeAudioTrack = null
        } catch (_: Exception) {}

        _isPlayingPreview.value = false
        _currentPlayingId.value = null
    }

    private fun playUriSound(context: Context, uri: Uri?, volume: Float, loop: Boolean) {
        if (uri == null) return
        try {
            val ringtone = RingtoneManager.getRingtone(context, uri)
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.volume = volume.coerceIn(0f, 1f)
                    ringtone.isLooping = loop
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                activeRingtone = ringtone
                ringtone.play()
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "RingtoneManager failed for uri, attempting MediaPlayer: ${e.message}")
        }

        // Fallback to MediaPlayer
        try {
            val player = MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setVolume(volume, volume)
                isLooping = loop
                prepare()
                start()
            }
            activeMediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer failed: ${e.message}")
            // Fallback to synthesized tone
            playSynthesizedTone("metro_chime", volume, loop)
        }
    }

    private fun playSynthesizedTone(toneId: String, volume: Float, loop: Boolean) {
        val samples = generateSamples(toneId)
        if (samples.isEmpty()) return

        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, samples.size * 2)

            val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    android.media.AudioManager.STREAM_ALARM,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STATIC
                )
            }

            track.write(samples, 0, samples.size)
            track.setVolume(volume.coerceIn(0.1f, 1f))
            if (loop) {
                track.setLoopPoints(0, samples.size, -1)
            }
            activeAudioTrack = track
            track.play()
        } catch (e: Exception) {
            Log.w(TAG, "AudioTrack synthesis error (expected in headless test): ${e.message}")
        }
    }

    /**
     * Synthesizes 16-bit PCM waveform samples for transit alarm ringtones.
     */
    internal fun generateSamples(toneId: String): ShortArray {
        return when (toneId) {
            "metro_chime" -> generateMetroChime()
            "express_horn" -> generateExpressHorn()
            "rapid_siren" -> generateRapidPulse()
            "gentle_radar" -> generateGentleRadar()
            "digital_beacon" -> generateDigitalBeacon()
            "heavy_sleeper" -> generateHeavySleeper()
            else -> generateMetroChime()
        }
    }

    /**
     * Metro Door Chime: Two harmonic bell tones (D5: 587 Hz -> A4: 440 Hz) with natural decay.
     */
    private fun generateMetroChime(): ShortArray {
        val durationSec = 1.8f
        val numSamples = (durationSec * SAMPLE_RATE).toInt()
        val buffer = ShortArray(numSamples)

        val chime1Samples = (0.7f * SAMPLE_RATE).toInt()
        val freq1 = 587.33 // D5
        val freq2 = 440.00 // A4

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val sample = if (i < chime1Samples) {
                val decay = exp(-4.0 * t)
                val fundamental = sin(2.0 * PI * freq1 * t)
                val harmonic = 0.3 * sin(2.0 * PI * (freq1 * 2.0) * t)
                (fundamental + harmonic) * decay
            } else {
                val t2 = (i - chime1Samples).toDouble() / SAMPLE_RATE
                val decay = exp(-3.5 * t2)
                val fundamental = sin(2.0 * PI * freq2 * t2)
                val harmonic = 0.3 * sin(2.0 * PI * (freq2 * 2.0) * t2)
                (fundamental + harmonic) * decay
            }
            buffer[i] = (sample * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    /**
     * Express Train Horn & Bell: Triad chord (Eb4: 311 Hz, F#4: 370 Hz, Bb4: 466 Hz).
     */
    private fun generateExpressHorn(): ShortArray {
        val durationSec = 2.0f
        val numSamples = (durationSec * SAMPLE_RATE).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // 2 bursts: 0.0-0.5s and 0.8-1.5s
            val isBurst1 = t in 0.0..0.45
            val isBurst2 = t in 0.6..1.4
            val sample = if (isBurst1 || isBurst2) {
                val burstT = if (isBurst1) t else (t - 0.6)
                val flutter = 1.0 + 0.15 * sin(2.0 * PI * 18.0 * burstT)
                val tone1 = sin(2.0 * PI * 311.13 * burstT)
                val tone2 = sin(2.0 * PI * 369.99 * burstT)
                val tone3 = 0.7 * sin(2.0 * PI * 466.16 * burstT)
                ((tone1 + tone2 + tone3) / 2.7) * flutter
            } else {
                // Bell ding between bursts
                val bellT = (t - 0.45)
                0.25 * sin(2.0 * PI * 880.0 * bellT) * exp(-6.0 * bellT)
            }
            buffer[i] = (sample * 26000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    /**
     * Rapid High-Pitch Pulse: Piercing alternating beeps (900 Hz & 1250 Hz, 100ms each).
     */
    private fun generateRapidPulse(): ShortArray {
        val durationSec = 1.6f
        val numSamples = (durationSec * SAMPLE_RATE).toInt()
        val buffer = ShortArray(numSamples)
        val cycleSamples = (0.2f * SAMPLE_RATE).toInt()
        val halfCycle = cycleSamples / 2

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val posInCycle = i % cycleSamples
            val freq = if (posInCycle < halfCycle) 920.0 else 1250.0
            // slight pause between beeps
            val envelope = if ((posInCycle % (halfCycle / 2)) < (halfCycle * 0.8)) 1.0 else 0.05
            val sample = sin(2.0 * PI * freq * t) * envelope
            buffer[i] = (sample * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    /**
     * Gentle Sunrise Radar: Ascending 4-tone arpeggio (C5, E5, G5, C6) with soft envelope.
     */
    private fun generateGentleRadar(): ShortArray {
        val durationSec = 2.2f
        val numSamples = (durationSec * SAMPLE_RATE).toInt()
        val buffer = ShortArray(numSamples)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val noteDuration = 0.45

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIndex = (t / noteDuration).toInt().coerceIn(0, notes.size - 1)
            val noteT = t - (noteIndex * noteDuration)
            val freq = notes[noteIndex]
            val decay = exp(-3.0 * noteT)
            val sample = (sin(2.0 * PI * freq * noteT) + 0.25 * sin(2.0 * PI * (freq * 2) * noteT)) * decay
            buffer[i] = (sample * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    /**
     * Digital Transit Beacon: Frequency-modulated synth (700 Hz to 1300 Hz sweep).
     */
    private fun generateDigitalBeacon(): ShortArray {
        val durationSec = 1.5f
        val numSamples = (durationSec * SAMPLE_RATE).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val sweepFreq = 800.0 + 400.0 * sin(2.0 * PI * 3.5 * t)
            val sample = sin(2.0 * PI * sweepFreq * t)
            buffer[i] = (sample * 25000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    /**
     * High-Intensity Wake Siren: Multi-tone piercing alarm for heavy sleepers.
     */
    private fun generateHeavySleeper(): ShortArray {
        val durationSec = 1.8f
        val numSamples = (durationSec * SAMPLE_RATE).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val sweep = if ((t % 0.4) < 0.2) 1100.0 else 1800.0
            val sub = 440.0
            val sample = 0.6 * sin(2.0 * PI * sweep * t) + 0.4 * sin(2.0 * PI * sub * t)
            buffer[i] = (sample * 29000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }
}
