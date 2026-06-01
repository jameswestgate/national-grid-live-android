package com.crainiate.nationalgridlive.ui.historic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crainiate.nationalgridlive.ui.components.DemandCard
import com.crainiate.nationalgridlive.ui.components.GenerationCard
import com.crainiate.nationalgridlive.ui.components.GridScreenContainer
import com.crainiate.nationalgridlive.ui.components.InterconnectorsCard
import com.crainiate.nationalgridlive.ui.components.PeriodSelector
import com.crainiate.nationalgridlive.ui.components.StatCardsRow
import com.crainiate.nationalgridlive.ui.components.StatItem
import com.crainiate.nationalgridlive.ui.components.StorageCard
import com.crainiate.nationalgridlive.ui.components.TrendsSection
import kotlin.math.roundToInt

@Composable
fun HistoricScreen(viewModel: HistoricViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val series by viewModel.series.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val online by viewModel.online.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh(force = false) }
    GridScreenContainer(
        title = "Historic",
        state = state,
        refreshing = refreshing,
        onRefresh = { viewModel.refresh(force = true) },
        online = online,
        topContent = { PeriodSelector(selected = period, onSelect = viewModel::select) }
    ) { snapshot ->
        StatCardsRow(
            listOf(
                StatItem("Period", period.label.removePrefix("Past ").replaceFirstChar { it.uppercase() }),
                StatItem("Price", "£${snapshot.priceGbpPerMwh.roundToInt()}", "/MWh"),
                StatItem("Emissions", snapshot.emissionsGPerKwh.toString(), "g/kWh")
            )
        )
        DemandCard(snapshot)
        GenerationCard(snapshot)
        InterconnectorsCard(snapshot)
        StorageCard(snapshot)
        series?.let { TrendsSection(it) }
    }
}
