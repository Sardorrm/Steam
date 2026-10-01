package com.example.donttrustthehouse.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.sin

class HorrorAudioEngine(private var context: Context? = null) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val sampleRate = 22050
    private var isMuted = false

    val ambientManager = BackgroundAudioManager()

    private var vibrator: Vibrator? = null

    init {
        context?.let { attachContext(it) }
    }

    fun attachContext(ctx: Context) {
        this.context = ctx
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        ambientManager.setMuted(muted)
    }

    fun isMuted(): Boolean = isMuted

    fun startAmbient() = ambientManager.start()

    fun stopAmbient() = ambientManager.stop()

    fun pauseAmbient() = ambientManager.pause()

    fun resumeAmbient() = ambientManager.resume()

    fun updateAmbient(
        roomName: String,
        dangerProximity: Float,
        isHiding: Boolean,
        isFlashlightOn: Boolean,
        sanity: Float = 1.0f
    ) {
        ambientManager.updateContext(roomName, dangerProximity, isHiding, isFlashlightOn, sanity)
    }

    private fun playPcmAsync(durationMs: Int, generator: (timeSec: Double) -> Double) {
        if (isMuted) return
        scope.launch {
            try {
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(100)
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val sampleValue = generator(t).coerceIn(-1.0, 1.0)
                    buffer[i] = (sampleValue * Short.MAX_VALUE * 0.7).toInt().toShort()
                }

                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2.coerceAtLeast(minBufferSize))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                delay(durationMs.toLong() + 100)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Ignore audio failure gracefully
            }
        }
    }

    fun playFootstep(isLoud: Boolean) {
        val duration = if (isLoud) 70 else 40
        val baseFreq = if (isLoud) 90.0 else 120.0
        val volume = if (isLoud) 0.6 else 0.25

        playPcmAsync(duration) { t ->
            val decay = (1.0 - t / (duration / 1000.0)).coerceAtLeast(0.0)
            val noise = (Math.random() * 2.0 - 1.0) * 0.4
            (sin(2.0 * Math.PI * baseFreq * t) * 0.6 + noise) * decay * volume
        }

        if (isLoud) {
            vibrate(15, 60)
        }
    }

    fun playHeartbeat(dangerRatio: Float) {
        // Double thump
        val volume = (0.2 + dangerRatio * 0.7).coerceIn(0.2, 1.0)
        playPcmAsync(120) { t ->
            val freq = 55.0 - t * 40.0
            val env = sin(t * Math.PI / 0.12).coerceAtLeast(0.0)
            sin(2.0 * Math.PI * freq * t) * env * volume
        }

        vibrate((20 + dangerRatio * 40).toLong(), (80 + dangerRatio * 150).toInt().coerceIn(1, 255))
    }

    fun playDoorOpen() {
        playPcmAsync(350) { t ->
            val freq = 180.0 + sin(t * 15.0) * 60.0
            val env = sin(t * Math.PI / 0.35).coerceAtLeast(0.0)
            val creak = sin(2.0 * Math.PI * freq * t)
            creak * env * 0.4
        }
        vibrate(30, 70)
    }

    fun playMonsterWake() {
        playPcmAsync(1200) { t ->
            val freq = 45.0 + sin(t * 6.0) * 15.0
            val noise = (Math.random() * 2.0 - 1.0) * 0.3
            val env = sin(t * Math.PI / 1.2).coerceAtLeast(0.0)
            (sin(2.0 * Math.PI * freq * t) * 0.7 + noise) * env * 0.75
        }
        vibrate(300, 180)
    }

    fun playMonsterSpot() {
        // High screech sting
        playPcmAsync(400) { t ->
            val freq = 600.0 - t * 400.0
            val noise = (Math.random() * 2.0 - 1.0) * 0.5
            val env = (1.0 - t / 0.4).coerceAtLeast(0.0)
            (sin(2.0 * Math.PI * freq * t) * 0.5 + noise) * env * 0.9
        }
        vibrate(250, 240)
    }

    fun playPlayerCaught() {
        playPcmAsync(800) { t ->
            val freq = 80.0 - t * 50.0
            val noise = (Math.random() * 2.0 - 1.0) * 0.7
            val env = (1.0 - t / 0.8).coerceAtLeast(0.0)
            (sin(2.0 * Math.PI * freq * t) * 0.4 + noise) * env * 0.9
        }
        vibrate(400, 255)
    }

    fun playWhisper() {
        playPcmAsync(700) { t ->
            val noise = (Math.random() * 2.0 - 1.0)
            val flutter = sin(t * 30.0) * 0.3 + 0.7
            val env = sin(t * Math.PI / 0.7).coerceAtLeast(0.0)
            noise * flutter * env * 0.4
        }
        vibrate(80, 50)
    }

    fun playStaticScare() {
        playPcmAsync(500) { t ->
            val noise = (Math.random() * 2.0 - 1.0)
            val freq = 200.0 + sin(t * 40.0) * 100.0
            val env = (1.0 - t / 0.5).coerceAtLeast(0.0)
            (noise * 0.7 + sin(2.0 * Math.PI * freq * t) * 0.3) * env * 0.8
        }
        vibrate(150, 200)
    }

    fun playInsanitySting() {
        playPcmAsync(850) { t ->
            // High pitched ear ringing tinnitus (2800 Hz) with eerie slow decay and beat frequency
            val freq = 2750.0 + sin(t * 14.0) * 35.0
            val subBeat = sin(2.0 * Math.PI * 5.0 * t) * 0.25
            val env = (1.0 - t / 0.85).coerceAtLeast(0.0)
            val sine = sin(2.0 * Math.PI * freq * t)
            (sine * 0.45 + subBeat) * env * 0.6
        }
        vibrate(120, 110)
    }

    fun playItemPickup() {
        playPcmAsync(160) { t ->
            // Metallic chime
            val freq = 520.0 + t * 400.0
            val env = (1.0 - t / 0.16).coerceAtLeast(0.0)
            sin(2.0 * Math.PI * freq * t) * env * 0.65
        }
        vibrate(50, 80)
    }

    fun playItemUse() {
        playPcmAsync(280) { t ->
            // Soft calming hum
            val freq = 340.0 - t * 120.0
            val env = sin(t * Math.PI / 0.28).coerceAtLeast(0.0)
            sin(2.0 * Math.PI * freq * t) * env * 0.5
        }
        vibrate(80, 60)
    }

    fun playTalismanWard() {
        playPcmAsync(900) { t ->
            // Supernatural holy ward resonance
            val freq = 440.0 + sin(t * 20.0) * 60.0
            val env = (1.0 - t / 0.9).coerceAtLeast(0.0)
            val chorus = sin(2.0 * Math.PI * freq * 1.5 * t) * 0.3
            (sin(2.0 * Math.PI * freq * t) * 0.5 + chorus) * env * 0.85
        }
        vibrate(300, 200)
    }

    fun playDoorUnlock() {
        playPcmAsync(200) { t ->
            // Mechanical lock tumbler click
            val noise = (Math.random() * 2.0 - 1.0) * 0.5
            val click = sin(2.0 * Math.PI * 800.0 * t) * 0.5
            val env = (1.0 - t / 0.2).coerceAtLeast(0.0)
            (noise + click) * env * 0.7
        }
        vibrate(90, 120)
    }

    private fun vibrate(durationMs: Long, amplitude: Int = 100) {
        try {
            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(durationMs)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore vibration errors gracefully
        }
    }

    fun release() {
        stopAmbient()
    }
}
