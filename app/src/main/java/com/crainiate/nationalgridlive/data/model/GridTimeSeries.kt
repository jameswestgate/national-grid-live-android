package com.crainiate.nationalgridlive.data.model

/** Point spacing of a [GridTimeSeries] (drives x-axis label formatting). */
enum class ChartGranularity { HalfHour, Hour, Day, Month }

/**
 * A time series for the Historic "Trends" charts: parallel arrays aligned to
 * [dates], with nullable values for gaps. Fuels/interconnectors are charted as
 * multi-line series. Day/Week come from the live cache; Year/All-time from the
 * hosted snapshot.
 */
data class GridTimeSeries(
    val period: Period,
    val granularity: ChartGranularity,
    val dates: List<String>,
    val price: List<Double?>,
    val emissions: List<Double?>,
    val demand: List<Double?>,
    val fuels: Map<FuelType, List<Double?>>,
    val interconnectors: Map<Interconnector, List<Double?>>,
    /** Pumped storage per bucket — drawn as a TRANSFER line on the site's graphs. */
    val pumped: List<Double?> = emptyList()
) {
    val isEmpty: Boolean get() = dates.size < 2

    companion object {
        fun empty(period: Period) = GridTimeSeries(
            period, ChartGranularity.Day, emptyList(), emptyList(), emptyList(),
            emptyList(), emptyMap(), emptyMap()
        )

        fun granularityFor(period: Period): ChartGranularity = when (period) {
            Period.Day -> ChartGranularity.HalfHour
            // The site's week tab plots its 7 daily rows, not hourly buckets.
            Period.Week -> ChartGranularity.Day
            Period.Year -> ChartGranularity.Day
            Period.AllTime -> ChartGranularity.Month
        }
    }
}
