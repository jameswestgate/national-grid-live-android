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

    // Equation display values (match grid.iamkate.com exactly): the site rounds
    // each category total and transfers to 1 dp BEFORE summing (State/Demand.php),
    // so the displayed "Demand = Generation + Transfers" always adds up on
    // screen; the Generation card headline GW uses the same rounded-sum
    // (PieChart.php). Percentages, by contrast, use the FULL-precision values
    // (Datum::getTotal()) — keep shareOfDemand()/demandGw for those.

    /** Generation as displayed in the equation: Σ of 1 dp-rounded category totals. */
    val equationGeneration: Double get() = categories.sumOf { r1(it.gigawatts) }

    /** Transfers as displayed in the equation: 1 dp-rounded (may be negative). */
    val equationTransfers: Double get() = r1(transfersGw)

    /** Demand as displayed in the equation: sum of the two rounded terms above. */
    val equationDemand: Double get() = equationGeneration + equationTransfers

    // kotlin.math.round ties away from zero — same as PHP round() / Swift rounded()
    private fun r1(v: Double): Double = kotlin.math.round(v * 10.0) / 10.0
}

/** Historic look-back windows. */
enum class Period(val label: String) {
    Day("Past day"),
    Week("Past week"),
    Year("Past year"),
    AllTime("All time")
}
