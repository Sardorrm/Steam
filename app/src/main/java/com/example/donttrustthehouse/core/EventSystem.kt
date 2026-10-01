package com.example.donttrustthehouse.core

import com.example.donttrustthehouse.audio.HorrorAudioEngine
import com.example.donttrustthehouse.model.HorrorEvent
import com.example.donttrustthehouse.model.HorrorEventType
import com.example.donttrustthehouse.model.Vector2D
import kotlin.random.Random

class EventSystem(
    private val audioEngine: HorrorAudioEngine,
    private val houseDirector: HouseDirector
) {
    private var lastEventTime = 0L
    private val eventCooldownMs = 12_000L // 12 seconds cooldown

    var currentActiveEvent: HorrorEvent? = null
        private set

    var eventActiveUntil = 0L
        private set

    fun triggerEvent(type: HorrorEventType, onTeleportScare: ((Vector2D) -> Unit)? = null): HorrorEvent? {
        val now = System.currentTimeMillis()
        if (now - lastEventTime < eventCooldownMs) return null

        lastEventTime = now
        eventActiveUntil = now + 4000L // show banner for 4 seconds

        val event = when (type) {
            HorrorEventType.MONSTER_AWAKENS -> {
                audioEngine.playMonsterWake()
                HorrorEvent(
                    type = type,
                    title = "The Entity Awakens",
                    description = "A deep growl reverberates through the walls. It is now hunting you."
                )
            }
            HorrorEventType.HEAR_OWN_VOICE -> {
                audioEngine.playWhisper()
                HorrorEvent(
                    type = type,
                    title = "Dissonant Whispers",
                    description = "You hear your own voice faintly whispering in the darkness ahead..."
                )
            }
            HorrorEventType.HEAR_OWN_FOOTSTEPS -> {
                audioEngine.playFootstep(true)
                audioEngine.playFootstep(false)
                HorrorEvent(
                    type = type,
                    title = "Phantom Footsteps",
                    description = "Wet footsteps echo behind you, out of sync with your movement."
                )
            }
            HorrorEventType.FALSE_EXIT -> {
                audioEngine.playDoorOpen()
                HorrorEvent(
                    type = type,
                    title = "False Exit",
                    description = "The creak of the exit door echoes... but nothing is unlocked."
                )
            }
            HorrorEventType.TELEPORT_SCARE -> {
                audioEngine.playStaticScare()
                val offset = Vector2D((Random.nextFloat() * 4f - 2f), (Random.nextFloat() * 4f - 2f))
                onTeleportScare?.invoke(offset)
                HorrorEvent(
                    type = type,
                    title = "Spatial Distortion",
                    description = "Reality tears for a brief second. You are not where you were..."
                )
            }
            HorrorEventType.LIGHTS_FLICKER -> {
                audioEngine.playStaticScare()
                HorrorEvent(
                    type = type,
                    title = "Power Surge",
                    description = "The lights violently flicker and hum with high-voltage static."
                )
            }
            HorrorEventType.GENERIC_SCARE -> {
                audioEngine.playWhisper()
                HorrorEvent(
                    type = type,
                    title = "Unsettling Presence",
                    description = "The house watches your every step with cold hostility."
                )
            }
        }

        currentActiveEvent = event
        return event
    }

    fun triggerSmartEvent(onTeleportScare: ((Vector2D) -> Unit)? = null): HorrorEvent? {
        val nextType = houseDirector.getNextEventType()
        return triggerEvent(nextType, onTeleportScare)
    }

    fun checkActiveEventTimeout() {
        if (currentActiveEvent != null && System.currentTimeMillis() > eventActiveUntil) {
            currentActiveEvent = null
        }
    }
}
