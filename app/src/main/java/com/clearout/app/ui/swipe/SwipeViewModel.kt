package com.clearout.app.ui.swipe

import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearout.app.data.datastore.GamificationDataStore
import com.clearout.app.domain.model.Photo
import com.clearout.app.domain.model.SwipeAction
import com.clearout.app.domain.repository.MediaStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val repository: MediaStoreRepository,
    private val dataStore: GamificationDataStore
) : ViewModel() {

    private val photos = mutableListOf<Photo>()
    private val toDelete = mutableListOf<Photo>()
    private val undoStack = ArrayDeque<Pair<Photo, SwipeAction>>()

    private var currentIndex = 0
    private var streakDays = 0
    private var sessionXP = 0

    private val _state = MutableStateFlow<SwipeUiState>(SwipeUiState.Loading)
    val state: StateFlow<SwipeUiState> = _state.asStateFlow()

    init {
        // Collect gamification details to show active streak
        viewModelScope.launch {
            dataStore.gamificationFlow.collect { stats ->
                streakDays = stats.streak
                emitCurrentState()
            }
        }
        loadGallery()
    }

    fun loadGallery() {
        viewModelScope.launch {
            _state.value = SwipeUiState.Loading
            try {
                val galleryPhotos = repository.getPhotos()
                photos.clear()
                photos.addAll(galleryPhotos)
                toDelete.clear()
                undoStack.clear()
                currentIndex = 0
                emitCurrentState()
            } catch (e: Exception) {
                _state.value = SwipeUiState.Error(e.message ?: "Failed to query media store.")
            }
        }
    }

    fun onSwipe(action: SwipeAction) {
        val currentReadyState = _state.value as? SwipeUiState.Ready ?: return
        val photo = currentReadyState.currentPhoto

        if (action == SwipeAction.DELETE) {
            toDelete.add(photo)
        }

        // Keep undo queue limited to 5 elements
        undoStack.addLast(photo to action)
        if (undoStack.size > 5) {
            undoStack.removeFirst()
        }

        // Calculate XP Gain dynamically: +1 XP per swipe
        sessionXP += 1

        currentIndex++
        emitCurrentState()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val (lastPhoto, lastAction) = undoStack.removeLast()

        if (lastAction == SwipeAction.DELETE) {
            toDelete.remove(lastPhoto)
        }

        if (sessionXP > 0) sessionXP -= 1

        currentIndex--
        emitCurrentState()
    }

    fun getToDeleteList(): List<Photo> = toDelete.toList()

    fun buildDeleteIntentSender(): IntentSender? {
        val uris = toDelete.map { it.uri }
        return repository.buildDeleteRequest(uris)
    }

    fun onDeleteConfirmed() {
        viewModelScope.launch {
            val count = toDelete.size
            val bytesFreed = toDelete.sumOf { it.sizeBytes }

            // Persist metrics inside DataStore (triggers level updates)
            dataStore.recordSession(count, bytesFreed)

            // Remove the deleted items from the local list
            photos.removeAll(toDelete)
            toDelete.clear()
            undoStack.clear()

            // Reset current viewing index
            currentIndex = 0
            emitCurrentState()
        }
    }

    fun onDeleteCancelled() {
        // Keeps the pile intact so they can try confirming again or undoing
    }

    fun isUndoAvailable(): Boolean = undoStack.isNotEmpty()

    private fun emitCurrentState() {
        if (currentIndex >= photos.size) {
            _state.value = SwipeUiState.Empty
        } else {
            val current = photos[currentIndex]
            val next = if (currentIndex + 1 < photos.size) photos[currentIndex + 1] else null
            _state.value = SwipeUiState.Ready(
                currentPhoto = current,
                nextPhoto = next,
                remainingCount = photos.size - currentIndex,
                toDeleteCount = toDelete.size,
                sessionXPGained = sessionXP,
                streakDays = streakDays
            )
        }
    }
}
