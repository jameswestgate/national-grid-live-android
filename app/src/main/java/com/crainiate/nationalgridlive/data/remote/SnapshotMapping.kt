package com.crainiate.nationalgridlive.data.remote

import com.crainiate.nationalgridlive.data.model.CategoryReading
import com.crainiate.nationalgridlive.data.model.FuelCategory
import com.crainiate.nationalgridlive.data.model.FuelReading
import com.crainiate.nationalgridlive.data.model.FuelType
import com.crainiate.nationalgridlive.data.model.GridSnapshot
import com.crainiate.nationalgridlive.data.model.GridTimeSeries
import com.crainiate.nationalgridlive.data.model.Interconnector
import com.crainiate.nationalgridlive.data.model.InterconnectorReading
import com.crainiate.nationalgridlive.data.model.Period
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt

/**
 * Parses the hosted `snapshot.json` into period-averaged [GridSnapshot]s (for the
 * cards) and [GridTimeSeries] (for the Trends charts). Each series stores parallel
 * arrays plus a flat `fuels`/`interconnectors` array that ALTERNATES name then
 * value-array, e.g. `["gas", [..], "wind", [..], ...]`.
 */
object SnapshotParser {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class SnapshotDto(val year: SeriesDto, val allTime: SeriesDto)

    @Serializable
    private data class SeriesDto(
        val dates: List<String> = emptyList(),
        val demand: List<Double?> = emptyList(),
        val emissions: List<Double?> = emptyList(),
        val price: List<Double?> = emptyList(),
        val fuels: List<JsonElement> = emptyList(),
        val interconnectors: List<JsonElement> = emptyList()
    )

    class Parsed(
        private val snapshots: Map<Period, GridSnapshot>,
        private val series: Map<Period, GridTimeSeries>
    ) {
        fun snapshot(period: Period): GridSnapshot? = snapshots[period]
        fun series(period: Period): GridTimeSeries? = series[period]
    }

    fun parse(jsonText: String): Parsed {
        val dto = json.decodeFromString(SnapshotDto.serializer(), jsonText)
        return Parsed(
            snapshots = mapOf(
                Period.Year to dto.year.toSnapshot(Period.Year),
                Period.AllTime to dto.allTime.toSnapshot(Period.AllTime)
            ),
            series = mapOf(
                Period.Year to dto.year.toSeries(Period.Year),
                Period.AllTime to dto.allTime.toSeries(Period.AllTime)
            )
        )
    }

    /* ---------------- period means (cards) ---------------- */

    private fun SeriesDto.toSnapshot(period: Period): GridSnapshot {
        val fuelMeans = HashMap<FuelType, Double>()
        var pumpedMean: Double? = null
        var i = 0
        while (i + 1 < fuels.size) {
            val name = fuels[i].jsonPrimitive.content
            val mean = fuels[i + 1].toDoubleList().meanOrZero()
            val type = fuelByKey(name)
            when {
                type != null -> fuelMeans[type] = mean
                name.equals("pumped", ignoreCase = true) -> pumpedMean = mean
            }
            i += 2
        }

        val icMeans = HashMap<Interconnector, Double>()
        var j = 0
        while (j + 1 < interconnectors.size) {
            val name = interconnectors[j].jsonPrimitive.content
            interconnectorByKey(name)?.let { icMeans[it] = interconnectors[j + 1].toDoubleList().meanOrZero() }
            j += 2
        }
        val icReadings = Interconnector.entries.map { InterconnectorReading(it, icMeans[it] ?: 0.0) }
        val transfers = icReadings.sumOf { it.gigawatts }

        val categories = FuelCategory.entries.mapNotNull { category ->
            val members = FuelType.entries
                .filter { it.category == category && fuelMeans.containsKey(it) }
                .map { FuelReading(it, fuelMeans.getValue(it)) }
            if (members.isEmpty()) null
            else CategoryReading(category, members.sumOf { it.gigawatts }, members)
        }
        val generation = categories.sumOf { it.gigawatts }

        return GridSnapshot(
            periodLabel = period.label,
            priceGbpPerMwh = price.meanOrZero(),
            emissionsGPerKwh = emissions.meanOrZero().roundToInt(),
            demandGw = generation + transfers,
            transfersGw = transfers,
            categories = categories,
            interconnectors = icReadings,
            pumpedGw = pumpedMean
        )
    }

    /* ---------------- full series (charts) ---------------- */

    private fun SeriesDto.toSeries(period: Period): GridTimeSeries {
        val fuelSeries = HashMap<FuelType, List<Double?>>()
        var i = 0
        while (i + 1 < fuels.size) {
            fuelByKey(fuels[i].jsonPrimitive.content)?.let { fuelSeries[it] = fuels[i + 1].toDoubleList() }
            i += 2
        }
        val icSeries = HashMap<Interconnector, List<Double?>>()
        var j = 0
        while (j + 1 < interconnectors.size) {
            interconnectorByKey(interconnectors[j].jsonPrimitive.content)?.let { icSeries[it] = interconnectors[j + 1].toDoubleList() }
            j += 2
        }
        return GridTimeSeries(
            period = period,
            granularity = GridTimeSeries.granularityFor(period),
            dates = dates,
            price = price,
            emissions = emissions,
            demand = demand,
            fuels = fuelSeries,
            interconnectors = icSeries
        )
    }

    /* ---------------- helpers ---------------- */

    private fun JsonElement.toDoubleList(): List<Double?> =
        jsonArray.map { if (it is JsonNull) null else it.jsonPrimitive.doubleOrNull }

    private fun fuelByKey(key: String): FuelType? = when (key.lowercase()) {
        "gas" -> FuelType.Gas
        "coal" -> FuelType.Coal
        "wind" -> FuelType.Wind
        "solar" -> FuelType.Solar
        "hydro" -> FuelType.Hydro
        "nuclear" -> FuelType.Nuclear
        "biomass" -> FuelType.Biomass
        else -> null // "pumped" handled separately (storage)
    }

    private fun interconnectorByKey(key: String): Interconnector? = when (key.lowercase()) {
        "france" -> Interconnector.France
        "ireland" -> Interconnector.Ireland
        "netherlands" -> Interconnector.Netherlands
        "belgium" -> Interconnector.Belgium
        "norway" -> Interconnector.Norway
        "denmark" -> Interconnector.Denmark
        else -> null
    }

    private fun List<Double?>.meanOrZero(): Double {
        val finite = filterNotNull().filter { it.isFinite() }
        return if (finite.isEmpty()) 0.0 else finite.average()
    }
}
