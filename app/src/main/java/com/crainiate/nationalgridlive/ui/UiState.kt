package com.crainiate.nationalgridlive.ui

import com.crainiate.nationalgridlive.data.model.GridSnapshot

/** Generic screen state for the data-backed screens. */
sealed interface GridUiState {
    data object Loading : GridUiState
    data class Ready(val snapshot: GridSnapshot) : GridUiState
    data class Failed(val message: String) : GridUiState
}
