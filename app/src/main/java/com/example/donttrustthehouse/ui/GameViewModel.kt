package com.example.donttrustthehouse.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.donttrustthehouse.audio.HorrorAudioEngine
import com.example.donttrustthehouse.core.GameManager
import com.example.donttrustthehouse.core.GameUIState
import com.example.donttrustthehouse.model.GameItem
import com.example.donttrustthehouse.model.MonsterState
import com.example.donttrustthehouse.model.PlayerState
import com.example.donttrustthehouse.model.RoomState
import com.example.donttrustthehouse.model.Vector2D
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * GameViewModel manages:
 * 1. Player's position, movement, and camera rotation
 * 2. Player inventory (collecting, using, dropping, and inspecting survival items)
 * 3. Monster proximity level (danger telemetry and alert state)
 * 4. Room states across the dynamic house architecture
 */
class GameViewModel(
    val gameManager: GameManager = GameManager(HorrorAudioEngine(null))
) : ViewModel() {

    val uiState: StateFlow<GameUIState> = gameManager.uiState

    // 1. Player Position & State
    val playerPosition: StateFlow<Vector2D> = gameManager.uiState
        .map { it.player.position }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = gameManager.uiState.value.player.position
        )

    val playerState: StateFlow<PlayerState> = gameManager.uiState
        .map { it.player }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = gameManager.uiState.value.player
        )

    // 2. Inventory Management
    val inventory: StateFlow<List<GameItem>> = gameManager.uiState
        .map { it.inventory }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = gameManager.uiState.value.inventory
        )

    val selectedItem: StateFlow<GameItem?> = gameManager.uiState
        .map { it.selectedItem }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = gameManager.uiState.value.selectedItem
        )

    // 3. Monster Proximity Level
    val monsterProximity: StateFlow<Float> = gameManager.uiState
        .map { it.dangerProximity }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = gameManager.uiState.value.dangerProximity
        )

    val monsterState: StateFlow<MonsterState> = gameManager.uiState
        .map { it.monster?.data?.state ?: MonsterState.IDLE }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MonsterState.IDLE
        )

    // 4. Room States
    val currentRoomState: StateFlow<RoomState?> = gameManager.uiState
        .map { it.currentRoomState }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = gameManager.uiState.value.currentRoomState
        )

    val roomStates: StateFlow<Map<String, RoomState>> = gameManager.uiState
        .map { it.roomStates }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = gameManager.uiState.value.roomStates
        )

    init {
        if (!gameManager.uiState.value.isPlaying) {
            gameManager.startGame()
        }
    }

    fun initAudio(context: Context) {
        gameManager.audioEngine.attachContext(context)
    }

    fun startGame() {
        gameManager.startGame()
    }

    // Player position and locomotion methods
    fun updatePlayerPosition(newPos: Vector2D) {
        gameManager.setPlayerPosition(newPos)
    }

    fun setMovementInput(input: Vector2D) {
        gameManager.setMovementInput(input)
    }

    fun rotateCamera(deltaAngle: Float) {
        gameManager.rotateCamera(deltaAngle)
    }

    fun toggleFlashlight() {
        gameManager.toggleFlashlight()
    }

    fun toggleCrouch() {
        gameManager.toggleCrouch()
    }

    fun triggerInteract() {
        gameManager.triggerInteract()
    }

    // Inventory operations
    fun pickUpItem(item: GameItem) {
        gameManager.triggerInteract()
    }

    fun useItem(item: GameItem) {
        gameManager.useInventoryItem(item)
    }

    fun dropItem(item: GameItem) {
        gameManager.dropInventoryItem(item)
    }

    fun selectItem(item: GameItem?) {
        gameManager.selectItem(item)
    }

    fun dismissNotification() {
        gameManager.dismissNotification()
    }

    fun toggleAudioMute() {
        gameManager.toggleAudioMute()
    }

    fun pauseAudio() {
        gameManager.pauseAudio()
    }

    fun resumeAudio() {
        gameManager.resumeAudio()
    }

    fun stop() {
        gameManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        gameManager.stop()
    }

    companion object {
        fun provideFactory(context: Context? = null): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val audio = HorrorAudioEngine(context)
                    val manager = GameManager(audio)
                    return GameViewModel(manager) as T
                }
            }
    }
}
