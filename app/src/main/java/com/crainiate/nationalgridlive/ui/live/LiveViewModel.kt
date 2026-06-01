package com.crainiate.nationalgridlive.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crainiate.nationalgridlive.data.repository.DataModule
import com.crainiate.nationalgridlive.data.repository.GridRepository
import com.crainiate.nationalgridlive.ui.GridUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiveViewModel : ViewModel() {

    private val repository: GridRepository = DataModule.gridRepository

    private val _state = MutableStateFlow<GridUiState>(GridUiState.Loading)
    val state = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    val online = repository.online

    init { viewModelScope.launch { reload(initial = true) } }

    /** [force] = pull-to-refresh (re-fetch); otherwise a cheap resume refresh. */
    fun refresh(force: Boolean) {
        viewModelScope.launch {
            if (force) {
                _refreshing.value = true
                repository.refresh()
            }
            reload(initial = false)
            _refreshing.value = false
        }
    }

    private suspend fun reload(initial: Boolean) {
        if (initial) _state.value = GridUiState.Loading
        val result = runCatching { repository.live() }.getOrNull()
        when {
            result != null -> _state.value = GridUiState.Ready(result)
            initial -> _state.value = GridUiState.Failed("Couldn't load live data")
            // on a refresh failure keep showing the last data
        }
    }
}
