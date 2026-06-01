package com.crainiate.nationalgridlive.ui.live

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
import com.crainiate.nationalgridlive.ui.components.StatCardsRow
import com.crainiate.nationalgridlive.ui.components.StatItem
import com.crainiate.nationalgridlive.ui.components.StorageCard
import kotlin.math.roundToInt

@Composable
fun LiveScreen(viewModel: LiveViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val online by viewModel.online.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh(force = false) }
    GridScreenContainer(
        title = "Live",
        state = state,
        refreshing = refreshing,
        onRefresh = { viewModel.refresh(force = true) },
        online = online
    ) { snapshot ->
        StatCardsRow(
            listOf(
                StatItem("Time", snapshot.periodLabel),
                StatItem("Price", "£${snapshot.priceGbpPerMwh.roundToInt()}", "/MWh"),
                StatItem("Emissions", snapshot.emissionsGPerKwh.toString(), "g/kWh")
            )
        )
        DemandCard(snapshot)
        GenerationCard(snapshot)
        InterconnectorsCard(snapshot)
        StorageCard(snapshot)
    }
}
