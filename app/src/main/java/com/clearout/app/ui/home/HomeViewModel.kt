package com.clearout.app.ui.home

import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearout.app.data.datastore.GamificationDataStore
import com.clearout.app.data.datastore.GamificationState
import com.clearout.app.domain.repository.MediaStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val gamification: GamificationState? = null,
    val totalPhotosCount: Int = 0,
    val totalStorageBytes: Long = 0,
    val freeStorageBytes: Long = 0,
    val usedStorageBytes: Long = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dataStore: GamificationDataStore,
    private val repository: MediaStoreRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Collect gamification stats
            dataStore.gamificationFlow.collect { stats ->
                _state.value = _state.value.copy(
                    gamification = stats,
                    isLoading = false
                )
            }
        }
        loadStorageAndGalleryMetadata()
    }

    fun loadStorageAndGalleryMetadata() {
        viewModelScope.launch {
            try {
                val stat = StatFs(Environment.getDataDirectory().path)
                val totalBytes = stat.totalBytes
                val freeBytes = stat.freeBytes
                val usedBytes = totalBytes - freeBytes
                val photos = repository.getPhotos()
                _state.value = _state.value.copy(
                    totalPhotosCount = photos.size,
                    totalStorageBytes = totalBytes,
                    freeStorageBytes = freeBytes,
                    usedStorageBytes = usedBytes
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Wipes all gamification progress (streak, XP, totals).
     * Triggered from the overflow Settings menu on the Home screen.
     * Onboarding state is preserved — the user will NOT see the intro again.
     */
    fun resetStats() {
        viewModelScope.launch {
            dataStore.resetGamification()
        }
    }
}
