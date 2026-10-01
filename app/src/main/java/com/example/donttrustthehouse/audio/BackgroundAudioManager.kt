package com.example.donttrustthehouse.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Dynamic background audio manager that procedurally generates and streams an
 * evolving, unsettling psychological horror ambient soundscape.
 *
 * It features multi-layered real-time sound synthesis:
 *  1. Infrasonic Sub-Bass Drone (35Hz-55Hz) with psychological room-dependent resonance.
 *  2. Dissonant Tritone & Minor Second Clusters (devil's intervals with acoustic beating).
 *  3. Dynamic Ghostly Wind / Draft / Pipe Hiss through resonant filter modulation.
 *  4. Organic Procedural House Creaks, settling floorboards, distant dripping, and metallic chimes.
 *  5. Claustrophobic Hiding Acoustics (intense low-pass muffled exterior + intimate breathing & pulse).
 *  6. Escalating Tension Curve reacting in real-time to monster proximity and danger levels.
 */
class BackgroundAudioManager {

    private val sampleRate = 22050
    private val bufferSize = 2048 // ~92ms audio chunks for low latency & smooth synthesis

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var streamJob: Job? = null

    @Volatile
    private var isPlaying = false

    @Volatile
    private var isPaused = false

    @Volatile
    private var isMuted = false

    @Volatile
    private var masterVolume = 0.85f

    // Dynamic inputs from the game
    @Volatile
    private var targetDanger = 0f

    @Volatile
    private var targetRoom = "living_room"

    @Volatile
    private var targetHiding = false

    @Volatile
    private var targetFlashlight = true

    @Volatile
    private var targetSanity = 1.0f

    // Internal smoothed state
    private var currentDanger = 0f
    private var currentMuffleAlpha = 0f // 0f = open, 1f = heavily muffled
    private var currentSanity = 1.0f

    // Oscillator phase accumulators
    private var subPhase = 0.0
    private var rootPhase = 0.0
    private var tritonePhase = 0.0
    private var minorSecondPhase = 0.0
    private var discordHighPhase = 0.0
    private var lfoSlowPhase = 0.0
    private var lfoFastPhase = 0.0
    private var breathPhase = 0.0

    // Filter states for pink/band-passed noise
    private var noiseFilterState = 0.0
    private var muffleFilterState = 0.0

    // Organic procedural scare/creak events embedded in ambient stream
    private var creakTimer = 2.0f
    private var isCreaking = false
    private var creakElapsed = 0f
    private var creakDuration = 0.4f
    private var creakBaseFreq = 220.0

    // Water droplet (for bathroom / basement)
    private var dripTimer = 3.5f
    private var isDripping = false
    private var dripElapsed = 0f
    private var dripFreq = 950.0

    // Metallic chime / pipe clink
    private var chimeTimer = 5.0f
    private var isChiming = false
    private var chimeElapsed = 0f
    private var chimeFreq = 1400.0

    fun start() {
        if (isPlaying) return
        isPlaying = true
        isPaused = false

        streamJob?.cancel()
        streamJob = scope.launch {
            audioStreamingLoop()
        }
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
        if (!isPlaying) {
            start()
        }
    }

    fun stop() {
        isPlaying = false
        isPaused = false
        streamJob?.cancel()
        streamJob = null
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun isMuted(): Boolean = isMuted

    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
    }

    fun getMasterVolume(): Float = masterVolume

    /**
     * Updates the atmospheric parameters dynamically based on player location,
     * monster proximity, stealth/hiding state, and lighting.
     */
    fun updateContext(
        roomName: String,
        dangerProximity: Float,
        isHiding: Boolean,
        isFlashlightOn: Boolean,
        sanity: Float = 1.0f
    ) {
        targetRoom = roomName.lowercase()
        targetDanger = dangerProximity.coerceIn(0f, 1f)
        targetHiding = isHiding
        targetFlashlight = isFlashlightOn
        targetSanity = sanity.coerceIn(0f, 1f)
    }

    private suspend fun audioStreamingLoop() {
        var audioTrack: AudioTrack? = null
        try {
            val minTrackBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val trackBufferSize = max(minTrackBufferSize, bufferSize * 4)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(trackBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack.play()

            val pcmBuffer = ShortArray(bufferSize)

            while (scope.isActive && isPlaying) {
                if (isPaused || isMuted || masterVolume <= 0.001f) {
                    pcmBuffer.fill(0)
                    audioTrack.write(pcmBuffer, 0, pcmBuffer.size)
                    delay(30)
                    continue
                }

                // Synthesize one chunk of dynamic ambient soundscape
                synthesizeAmbientChunk(pcmBuffer)

                // Stream directly to hardware
                val written = audioTrack.write(pcmBuffer, 0, pcmBuffer.size)
                if (written < 0) {
                    Log.w("BackgroundAudioManager", "AudioTrack write error: $written")
                    delay(10)
                }
            }
        } catch (e: Exception) {
            Log.e("BackgroundAudioManager", "Exception in ambient audio loop", e)
        } finally {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Synthesizes a single chunk of multi-layered horror ambiance.
     */
    private fun synthesizeAmbientChunk(buffer: ShortArray) {
        val dt = 1.0 / sampleRate
        val chunkDurationSec = buffer.size.toDouble() / sampleRate

        // Smoothly interpolate danger & hiding muffle state towards targets
        val dangerStep = (targetDanger - currentDanger) * 0.08f
        val targetMuffle = if (targetHiding) 0.88f else 0.0f
        val muffleStep = (targetMuffle - currentMuffleAlpha) * 0.06f
        val sanityStep = (targetSanity - currentSanity) * 0.05f

        // Room-specific fundamental frequencies & acoustic personality
        val (baseSubFreq, roomDissonanceFactor, roomNoiseTone, roomReverb) = when {
            targetRoom.contains("basement") -> Quadruple(36.0, 1.4, 0.45, 0.85) // Cavernous, deep, rumbling
            targetRoom.contains("bathroom") -> Quadruple(58.0, 1.2, 0.75, 0.90) // Cold, whistling pipe hiss, high echo
            targetRoom.contains("kitchen") -> Quadruple(48.0, 1.1, 0.60, 0.50)  // Chilling draft, thin unsettling drone
            targetRoom.contains("bedroom") -> Quadruple(42.0, 1.0, 0.40, 0.60)  // Oppressive dead silence, wooden floor creaks
            else -> Quadruple(44.0, 1.0, 0.50, 0.65) // Living room / corridors: classic eerie Victorian dread
        }

        // Advance random event timers
        updateOrganicEventTimers(chunkDurationSec.toFloat())

        for (i in buffer.indices) {
            currentDanger += dangerStep / buffer.size
            currentMuffleAlpha += muffleStep / buffer.size
            currentSanity += sanityStep / buffer.size
            val insanity = (1.0f - currentSanity).coerceIn(0f, 1f)

            // 1. Infrasonic Sub-Bass Pulse (35Hz - 60Hz)
            // Throbs gently; accelerates and distorts as monster approaches
            val subPulseRate = 0.15 + currentDanger * 1.8 // LFO speed
            lfoSlowPhase += subPulseRate * 2.0 * PI * dt
            val subTremolo = (0.65 + 0.35 * sin(lfoSlowPhase))

            // Sub frequency glides up slightly with panic
            val subFreq = baseSubFreq + (currentDanger * 14.0) + (sin(lfoSlowPhase * 0.5) * 1.5)
            subPhase += subFreq * 2.0 * PI * dt
            val subSine = sin(subPhase)
            // Soft saturation on sub-bass for a warm, heavy dread feeling
            val subWave = (subSine - 0.15 * subSine * subSine * subSine) * subTremolo * 0.45

            // 2. Dissonant Tritone Harmonic Drones (Devil's Interval)
            // Root around 110Hz (A2), tritone at ~155.5Hz (Eb), minor 2nd at 116.5Hz (Bb)
            val rootFreq = 108.0 + currentDanger * 25.0
            val tritoneFreq = rootFreq * 1.4142 // Square root of 2 = exact Tritone interval
            val minorSecondFreq = rootFreq * 1.0594 // Minor second = dissonant beating

            rootPhase += rootFreq * 2.0 * PI * dt
            tritonePhase += tritoneFreq * 2.0 * PI * dt
            minorSecondPhase += minorSecondFreq * 2.0 * PI * dt

            // Slow acoustic beating modulation
            lfoFastPhase += (0.4 + currentDanger * 2.5) * 2.0 * PI * dt
            val tritoneVol = (0.08 + currentDanger * 0.35) * roomDissonanceFactor
            val beatMod = 0.5 + 0.5 * sin(lfoFastPhase)

            val dissonantCluster = (
                sin(rootPhase) * 0.22 +
                sin(tritonePhase) * tritoneVol * beatMod +
                sin(minorSecondPhase) * (tritoneVol * 0.75) * (1.0 - beatMod)
            )

            // High Discordant Shriek Layer when danger > 0.5
            var highDiscord = 0.0
            if (currentDanger > 0.45f) {
                val discordAmount = ((currentDanger - 0.45f) / 0.55f).coerceIn(0f, 1f)
                val highFreq = 720.0 + sin(lfoFastPhase * 2.0) * 80.0 + (currentDanger * 200.0)
                discordHighPhase += highFreq * 2.0 * PI * dt
                highDiscord = sin(discordHighPhase) * (discordAmount * 0.18)
            }

            // 3. Dynamic Ghostly Wind / Draft / Pipe Whistle
            // Resonant low-pass filtered noise that wanders slowly
            val whiteNoise = (Random.nextDouble() * 2.0 - 1.0)
            val filterAlpha = (0.04 + roomNoiseTone * 0.05 + sin(lfoSlowPhase * 1.5) * 0.02).coerceIn(0.01, 0.15)
            noiseFilterState += filterAlpha * (whiteNoise - noiseFilterState)
            val windDraft = noiseFilterState * (0.28 + (if (!targetFlashlight) 0.12 else 0.0))

            // 4. Organic Procedural Events (Creaks, Drips, Chimes)
            var organicSound = 0.0

            // Wood Creak
            if (isCreaking) {
                val t = creakElapsed.toDouble()
                val progress = (t / creakDuration).coerceIn(0.0, 1.0)
                val env = sin(progress * PI)
                val creakFreqMod = creakBaseFreq + sin(progress * 15.0) * 45.0
                organicSound += sin(t * 2.0 * PI * creakFreqMod) * env * 0.25
                organicSound += (Random.nextDouble() * 2.0 - 1.0) * env * 0.08 // Wood splinter grit
            }

            // Water Drip Echo
            if (isDripping) {
                val t = dripElapsed.toDouble()
                val env = exp(-t * 25.0) // Quick decaying droplet ping
                val f = dripFreq - t * 400.0
                organicSound += sin(t * 2.0 * PI * f) * env * 0.35 * roomReverb
            }

            // Metallic Pipe / Chime Resonance
            if (isChiming) {
                val t = chimeElapsed.toDouble()
                val env = exp(-t * 4.5) // Lingering metallic resonance
                val f = chimeFreq
                organicSound += (sin(t * 2.0 * PI * f) * 0.6 + sin(t * 2.0 * PI * (f * 1.5)) * 0.4) * env * 0.20
            }

            // 5. Claustrophobic Hiding Presence (Muffled breathing & binaural heart)
            var hidingAtmosphere = 0.0
            if (currentMuffleAlpha > 0.1f) {
                // Inhale / Exhale breath cycle (~0.3 Hz)
                breathPhase += 0.32 * 2.0 * PI * dt
                val breathCycle = sin(breathPhase)
                val isExhaling = breathCycle < 0
                val breathNoise = (Random.nextDouble() * 2.0 - 1.0) * (abs(breathCycle) * 0.18)
                val breathHum = sin(breathPhase) * 0.12
                hidingAtmosphere = (breathNoise + breathHum) * currentMuffleAlpha
            }

            // 6. Insanity Hallucination Drone (Tinnitus ringing & micro-whispers on low sanity)
            var insanityAtmosphere = 0.0
            if (insanity > 0.25f) {
                val ringingFreq = 2600.0 + sin(lfoSlowPhase * 3.0) * 120.0
                val ringWave = sin(breathPhase * 80.0 + ringingFreq * dt) * (insanity - 0.25f) * 0.12
                val psychicFlutter = (Random.nextDouble() * 2.0 - 1.0) * ((insanity * insanity) * 0.14)
                insanityAtmosphere = ringWave + psychicFlutter
            }

            // Combined raw ambient mix
            val rawMix = (subWave + dissonantCluster + highDiscord + windDraft + organicSound + hidingAtmosphere + insanityAtmosphere)

            // 6. Apply Hiding Muffle (Low-Pass Filter)
            // Outside sounds get strongly filtered down when crouched inside closet
            val lpfAlpha = if (currentMuffleAlpha > 0.05f) {
                // Cutoff between 250Hz and 1200Hz based on muffle depth
                (0.08 * (1.0 - currentMuffleAlpha * 0.75)).coerceIn(0.015, 0.12)
            } else {
                0.95 // Fully open bypass
            }
            muffleFilterState += lpfAlpha * (rawMix - muffleFilterState)
            val finalSample = muffleFilterState * masterVolume

            // 7. Clamp and store in 16-bit PCM buffer
            val clamped = finalSample.coerceIn(-1.0, 1.0)
            buffer[i] = (clamped * Short.MAX_VALUE * 0.85).toInt().toShort()
        }
    }

    private fun updateOrganicEventTimers(deltaSec: Float) {
        // Wood creak generator
        if (isCreaking) {
            creakElapsed += deltaSec
            if (creakElapsed >= creakDuration) {
                isCreaking = false
            }
        } else {
            creakTimer -= deltaSec
            if (creakTimer <= 0f) {
                isCreaking = true
                creakElapsed = 0f
                creakDuration = Random.nextFloat() * 0.35f + 0.25f
                creakBaseFreq = Random.nextDouble(160.0, 320.0)
                // Next creak in 4 to 12 seconds
                creakTimer = Random.nextFloat() * 8.0f + 4.0f
            }
        }

        // Water drip generator (predominantly in bathroom & basement)
        if (isDripping) {
            dripElapsed += deltaSec
            if (dripElapsed >= 0.25f) {
                isDripping = false
            }
        } else {
            dripTimer -= deltaSec
            if (dripTimer <= 0f) {
                val isMoistRoom = targetRoom.contains("bathroom") || targetRoom.contains("basement")
                if (isMoistRoom || Random.nextFloat() < 0.25f) {
                    isDripping = true
                    dripElapsed = 0f
                    dripFreq = Random.nextDouble(850.0, 1400.0)
                }
                dripTimer = Random.nextFloat() * (if (isMoistRoom) 4.0f else 12.0f) + 2.5f
            }
        }

        // Metallic chime / distant pipe clink
        if (isChiming) {
            chimeElapsed += deltaSec
            if (chimeElapsed >= 1.2f) {
                isChiming = false
            }
        } else {
            chimeTimer -= deltaSec
            if (chimeTimer <= 0f) {
                if (Random.nextFloat() < 0.45f) {
                    isChiming = true
                    chimeElapsed = 0f
                    chimeFreq = Random.nextDouble(1100.0, 1850.0)
                }
                chimeTimer = Random.nextFloat() * 10.0f + 6.0f
            }
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
