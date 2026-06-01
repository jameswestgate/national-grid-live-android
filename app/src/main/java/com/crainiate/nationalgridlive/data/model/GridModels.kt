package com.crainiate.nationalgridlive.data.model

/** A high-level generation category, mirroring the iOS app + grid.iamkate.com. */
enum class FuelCategory(val displayName: String) {
    Fossil("Fossil Fuels"),
    Renewable("Renewables"),
    Other("Other Sources")
}

/** An individual generation fuel. */
enum class FuelType(val displayName: String, val category: FuelCategory) {
    Gas("Gas", FuelCategory.Fossil),
    Coal("Coal", FuelCategory.Fossil),
    Wind("Wind", FuelCategory.Renewable),
    Solar("Solar", FuelCategory.Renewable),
    Hydro("Hydroelectric", FuelCategory.Renewable),
    Nuclear("Nuclear", FuelCategory.Other),
    Biomass("Biomass", FuelCategory.Other)
}

/** A cross-border interconnector, grouped by country (matches grid.iamkate.com). */
enum class Interconnector(val displayName: String) {
    Belgium("Belgium"),
    Denmark("Denmark"),
    France("France"),
    Ireland("Ireland"),
    Netherlands("Netherlands"),
    Norway("Norway")
}

/** One fuel's contribution, in gigawatts. */
data class FuelReading(val type: FuelType, val gigawatts: Double)

/** One interconnector's net flow, in gigawatts (negative = exporting). */
data class InterconnectorReading(val interconnector: Interconnector, val gigawatts: Double)

/** A category total plus its member fuels. */
data class CategoryReading(
    val category: FuelCategory,
    val gigawatts: Double,
    val fuels: List<FuelReading>
)

/**
 * A single point-in-time (or period-averaged) view of the grid — everything the
 * Live and Historic screens render. Percentages are computed as a share of demand,
 * matching the iOS app and Kate's dashboard.
 */
data class GridSnapshot(
    val periodLabel: String,        // "16:05" for live, "Past day" etc. for historic
    val priceGbpPerMwh: Double,
    val emissionsGPerKwh: Int,
    val demandGw: Double,
    val transfersGw: Double,
    val categories: List<CategoryReading>,
    val interconnectors: List<InterconnectorReading> = emptyList(),
    val pumpedGw: Double? = null    // pumped-storage flow (negative = pumping/charging)
) {
    val generationGw: Double get() = categories.sumOf { it.gigawatts }
    val generationShareOfDemand: Double
        get() = if (demandGw > 0) generationGw / demandGw else 0.0

    fun shareOfDemand(gw: Double): Double = if (demandGw > 0) gw / demandGw else 0.0
    fun shareOfGeneration(gw: Double): Double = if (generationGw > 0) gw / generationGw else 0.0

    val allFuels: List<FuelReading> get() = categories.flatMap { it.fuels }
}

/** Historic look-back windows. */
enum class Period(val label: String) {
    Day("Past day"),
    Week("Past week"),
    Year("Past year"),
    AllTime("All time")
}
