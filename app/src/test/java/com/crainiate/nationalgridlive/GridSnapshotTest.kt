package com.crainiate.nationalgridlive

import com.crainiate.nationalgridlive.data.model.CategoryReading
import com.crainiate.nationalgridlive.data.model.FuelCategory
import com.crainiate.nationalgridlive.data.model.FuelReading
import com.crainiate.nationalgridlive.data.model.FuelType
import com.crainiate.nationalgridlive.data.model.GridSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class GridSnapshotTest {

    private val snapshot = GridSnapshot(
        periodLabel = "test",
        priceGbpPerMwh = 100.0,
        emissionsGPerKwh = 100,
        demandGw = 25.0,
        transfersGw = 5.0,
        categories = listOf(
            CategoryReading(FuelCategory.Fossil, 8.0, listOf(FuelReading(FuelType.Gas, 8.0))),
            CategoryReading(FuelCategory.Renewable, 12.0, listOf(FuelReading(FuelType.Wind, 12.0)))
        )
    )

    @Test fun generationIsSumOfCategories() {
        assertEquals(20.0, snapshot.generationGw, 1e-6)
    }

    @Test fun shareOfDemandUsesDemand() {
        assertEquals(0.32, snapshot.shareOfDemand(8.0), 1e-6)
    }

    @Test fun shareOfGenerationUsesGeneration() {
        assertEquals(0.4, snapshot.shareOfGeneration(8.0), 1e-6)
    }
}
