package com.crainiate.nationalgridlive.data.repository

import com.crainiate.nationalgridlive.data.model.CategoryReading
import com.crainiate.nationalgridlive.data.model.FuelCategory
import com.crainiate.nationalgridlive.data.model.FuelReading
import com.crainiate.nationalgridlive.data.model.FuelType
import com.crainiate.nationalgridlive.data.model.GridSnapshot
import com.crainiate.nationalgridlive.data.model.GridTimeSeries
import com.crainiate.nationalgridlive.data.model.Interconnector
import com.crainiate.nationalgridlive.data.model.InterconnectorReading
import com.crainiate.nationalgridlive.data.model.Period
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Deterministic sample data — the exact numbers from the design mockups, so the
 * app renders identically to the screenshots in `docs/` with no network.
 */
class MockGridRepository : GridRepository {

    override suspend fun live(): GridSnapshot {
        return GridSnapshot(
            periodLabel = "16:05",
            priceGbpPerMwh = 106.93,
            emissionsGPerKwh = 82,
            demandGw = 26.7,
            transfersGw = 6.6,
            categories = listOf(
                CategoryReading(
                    FuelCategory.Fossil, 3.39,
                    listOf(FuelReading(FuelType.Gas, 3.39))
                ),
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
                    listOf(
                        FuelReading(FuelType.Nuclear, 3.55),
                        FuelReading(FuelType.Biomass, 0.32)
                    )
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
    }

    override suspend fun historic(period: Period): GridSnapshot {
        return when (period) {
            Period.Day -> GridSnapshot(
                periodLabel = period.label,
                priceGbpPerMwh = 106.56,
                emissionsGPerKwh = 140,
                demandGw = 25.5,
                transfersGw = 4.6,
                categories = listOf(
                    CategoryReading(
                        FuelCategory.Fossil, 7.37,
                        listOf(FuelReading(FuelType.Gas, 7.37))
                    ),
                    CategoryReading(
                        FuelCategory.Renewable, 9.54,
                        listOf(
                            FuelReading(FuelType.Wind, 4.52),
                            FuelReading(FuelType.Solar, 4.81),
                            FuelReading(FuelType.Hydro, 0.21)
                        )
                    ),
                    CategoryReading(
                        FuelCategory.Other, 3.99,
                        listOf(
                            FuelReading(FuelType.Nuclear, 3.70),
                            FuelReading(FuelType.Biomass, 0.29)
                        )
                    )
                )
            )
            // Year matches the verified backfill aggregates (see tools repo README).
            Period.Year -> GridSnapshot(
                periodLabel = period.label,
                priceGbpPerMwh = 80.6,
                emissionsGPerKwh = 122,
                demandGw = 30.5,
                transfersGw = 3.1,
                categories = listOf(
                    CategoryReading(
                        FuelCategory.Fossil, 8.30,
                        listOf(FuelReading(FuelType.Gas, 8.22), FuelReading(FuelType.Coal, 0.08))
                    ),
                    CategoryReading(
                        FuelCategory.Renewable, 14.66,
                        listOf(
                            FuelReading(FuelType.Wind, 10.75),
                            FuelReading(FuelType.Solar, 1.89),
                            FuelReading(FuelType.Hydro, 2.02)
                        )
                    ),
                    CategoryReading(
                        FuelCategory.Other, 4.44,
                        listOf(FuelReading(FuelType.Nuclear, 3.82), FuelReading(FuelType.Biomass, 0.62))
                    )
                )
            )
            Period.Week -> GridSnapshot(
                periodLabel = period.label,
                priceGbpPerMwh = 88.2,
                emissionsGPerKwh = 118,
                demandGw = 29.1,
                transfersGw = 3.6,
                categories = listOf(
                    CategoryReading(FuelCategory.Fossil, 7.9, listOf(FuelReading(FuelType.Gas, 7.9))),
                    CategoryReading(
                        FuelCategory.Renewable, 13.8,
                        listOf(
                            FuelReading(FuelType.Wind, 9.4),
                            FuelReading(FuelType.Solar, 2.6),
                            FuelReading(FuelType.Hydro, 1.8)
                        )
                    ),
                    CategoryReading(
                        FuelCategory.Other, 4.3,
                        listOf(FuelReading(FuelType.Nuclear, 3.7), FuelReading(FuelType.Biomass, 0.6))
                    )
                )
            )
            Period.AllTime -> GridSnapshot(
                periodLabel = period.label,
                priceGbpPerMwh = 72.4,
                emissionsGPerKwh = 196,
                demandGw = 32.8,
                transfersGw = 2.4,
                categories = listOf(
                    CategoryReading(FuelCategory.Fossil, 12.1, listOf(FuelReading(FuelType.Gas, 11.6), FuelReading(FuelType.Coal, 0.5))),
                    CategoryReading(
                        FuelCategory.Renewable, 11.0,
                        listOf(
                            FuelReading(FuelType.Wind, 7.6),
                            FuelReading(FuelType.Solar, 1.5),
                            FuelReading(FuelType.Hydro, 1.9)
                        )
                    ),
                    CategoryReading(
                        FuelCategory.Other, 5.3,
                        listOf(FuelReading(FuelType.Nuclear, 4.6), FuelReading(FuelType.Biomass, 0.7))
                    )
                )
            )
        }
    }

    override suspend fun series(period: Period): GridTimeSeries = GridTimeSeries.empty(period)

    override val online: StateFlow<Boolean> = MutableStateFlow(true)

    override suspend fun refresh() {}
}
