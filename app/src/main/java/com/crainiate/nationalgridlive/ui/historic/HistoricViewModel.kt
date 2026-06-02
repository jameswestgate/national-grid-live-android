package com.crainiate.nationalgridlive.ui.historic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crainiate.nationalgridlive.data.model.GridTimeSeries
import com.crainiate.nationalgridlive.data.model.Period
import com.crainiate.nationalgridlive.data.repository.DataModule
import com.crainiate.nationalgridlive.data.repository.GridRepository
import com.crainiate.nationalgridlive.ui.GridUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoricViewModel : ViewModel() {

    private val repository: GridRepository = DataModule.gridRepository

    private val _period = MutableStateFlow(Period.Day)
    val period = _period.asStateFlow()

    private val _state = MutableStateFlow<GridUiState>(GridUiState.Loading)
    val state = _state.asStateFlow()

    private val _series = MutableStateFlow<GridTimeSeries?>(null)
    val series = _series.asStateFlow()

    /** All four periods' series — the charts share one y-axis per metric across
     *  every tab (the site's Axes.php behaviour), so the axes need them all. */
    private val _axesSeries = MutableStateFlow<List<GridTimeSeries>>(emptyList())
    val axesSeries = _axesSeries.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    val online = repository.online

    init {
        load(Period.Day, initial = true)
        viewModelScope.launch { loadAxesSeries() }
    }

    fun select(period: Period) {
        if (period == _period.value && _state.value is GridUiState.Ready) return
        _period.value = period
        load(period, initial = true)
    }

    /** [force] = pull-to-refresh; otherwise a cheap resume refresh of the current period. */
    fun refresh(force: Boolean) {
        viewModelScope.launch {
            if (force) {
                _refreshing.value = true
                repository.refresh()
            }
            loadSuspend(_period.value, initial = false)
            if (force) loadAxesSeries()
            _refreshing.value = false
        }
    }

    private suspend fun loadAxesSeries() {
        _axesSeries.value = Period.entries
            .mapNotNull { p -> runCatching { repository.series(p) }.getOrNull() }
            .filter { !it.isEmpty }
    }

    private fun load(period: Period, initial: Boolean) {
        viewModelScope.launch { loadSuspend(period, initial) }
    }

    private suspend fun loadSuspend(period: Period, initial: Boolean) {
        if (initial) {
            _state.value = GridUiState.Loading
            _series.value = null
        }
        val snapshot = runCatching { repository.historic(period) }.getOrNull()
        when {
            snapshot != null -> _state.value = GridUiState.Ready(snapshot)
            initial -> _state.value = GridUiState.Failed("Couldn't load historic data")
        }
        // Charts are best-effort and load after the cards.
        _series.value = runCatching { repository.series(period) }.getOrNull()
    }
}
