package com.clearout.app.ui.swipe

import com.clearout.app.domain.model.Photo

sealed interface SwipeUiState {
    data object Loading : SwipeUiState
    data class Ready(
        val currentPhoto: Photo,
        val nextPhoto: Photo?,
        val remainingCount: Int,
        val toDeleteCount: Int,
        val sessionXPGained: Int,
        val streakDays: Int
    ) : SwipeUiState
    data object Empty : SwipeUiState
    data class Error(val message: String) : SwipeUiState
}
