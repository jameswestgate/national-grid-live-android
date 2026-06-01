package com.crainiate.nationalgridlive.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crainiate.nationalgridlive.data.model.CategoryReading
import com.crainiate.nationalgridlive.data.model.FuelCategory
import com.crainiate.nationalgridlive.data.model.FuelReading
import com.crainiate.nationalgridlive.data.model.FuelType
import com.crainiate.nationalgridlive.data.model.GridSnapshot
import com.crainiate.nationalgridlive.data.model.Interconnector
import com.crainiate.nationalgridlive.data.model.InterconnectorReading
import com.crainiate.nationalgridlive.ui.components.DemandCard
import com.crainiate.nationalgridlive.ui.components.GenerationCard
import com.crainiate.nationalgridlive.ui.components.ScreenTitle
import com.crainiate.nationalgridlive.ui.components.StatCardsRow
import com.crainiate.nationalgridlive.ui.components.StatItem
import com.crainiate.nationalgridlive.ui.theme.NationalGridLiveTheme

private val sample = GridSnapshot(
    periodLabel = "16:05",
    priceGbpPerMwh = 106.93,
    emissionsGPerKwh = 82,
    demandGw = 26.7,
    transfersGw = 6.6,
    categories = listOf(
        CategoryReading(FuelCategory.Fossil, 3.39, listOf(FuelReading(FuelType.Gas, 3.39))),
        CategoryReading(
            FuelCategory.Renewable, 12.61,
            listOf(
                FuelReading(FuelType.Wind, 3.96),
                FuelReading(FuelType.Solar, 8.53),
                FuelReading(FuelType.Hydro, 0.12)
            )
        ),
        CategoryReading(
            FuelCategory.Other, 3.87,
            listOf(FuelReading(FuelType.Nuclear, 3.55), FuelReading(FuelType.Biomass, 0.32))
        )
    ),
    interconnectors = listOf(
        InterconnectorReading(Interconnector.Belgium, 1.00),
        InterconnectorReading(Interconnector.Denmark, 0.0),
        InterconnectorReading(Interconnector.France, 3.49),
        InterconnectorReading(Interconnector.Ireland, -0.34),
        InterconnectorReading(Interconnector.Netherlands, 1.05),
        InterconnectorReading(Interconnector.Norway, 1.40)
    ),
    pumpedGw = 0.20
)

@Composable
private fun PreviewBody() {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenTitle("Live")
        StatCardsRow(
            listOf(
                StatItem("Time", sample.periodLabel),
                StatItem("Price", "£107", "/MWh"),
                StatItem("Emissions", "82", "g/kWh")
            )
        )
        DemandCard(sample)
        GenerationCard(sample)
    }
}

@Preview(name = "Live · light", showBackground = true, heightDp = 1000)
@Composable
fun LivePreviewLight() {
    NationalGridLiveTheme(darkTheme = false) { PreviewBody() }
}

@Preview(name = "Live · dark", showBackground = true, heightDp = 1000)
@Composable
fun LivePreviewDark() {
    NationalGridLiveTheme(darkTheme = true) { PreviewBody() }
}
