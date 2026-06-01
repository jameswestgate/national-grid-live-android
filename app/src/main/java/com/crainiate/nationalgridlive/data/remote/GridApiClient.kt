package com.crainiate.nationalgridlive.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

/** One FUELINST 5-minute reading. */
data class FuelInstItem(val startTime: String, val fuelType: String, val mw: Double)

/** One market-index price reading (30-min). */
data class PriceItem(val startTime: String, val price: Double)

/** One Carbon Intensity reading (30-min); actual, falling back to forecast. */
data class EmissionItem(val from: String, val value: Int)

/**
 * Low-level fetchers for the open data APIs. Each returns typed items for a time
 * window; the [LiveDataAggregator] buckets and merges them into the rolling cache.
 */
class GridApiClient(
    private val neso: NesoEmbeddedService = NesoEmbeddedService()
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Elexon FUELINST (5-min) over [from]..[to]. */
    suspend fun fetchFuelInst(from: Instant, to: Instant): List<FuelInstItem> {
        val url = "${Endpoints.FUELINST}?publishDateTimeFrom=${ApiTime.iso(from)}" +
            "&publishDateTimeTo=${ApiTime.iso(to)}&format=json"
        return dataArray(Http.get(url)).mapNotNull { el ->
            val o = el.jsonObject
            val start = o["startTime"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val fuel = o["fuelType"]?.jsonPrimitive?.content ?: return@mapNotNull null
            FuelInstItem(start, fuel, o["generation"]?.jsonPrimitive?.doubleOrNull ?: 0.0)
        }
    }

    /** Elexon market-index price (30-min) over [from]..[to]. */
    suspend fun fetchMarketIndex(from: Instant, to: Instant): List<PriceItem> {
        val url = "${Endpoints.MARKET_INDEX}?from=${ApiTime.iso(from)}&to=${ApiTime.iso(to)}&dataProviders=APXMIDP"
        return dataArray(Http.get(url)).mapNotNull { el ->
            val o = el.jsonObject
            val start = o["startTime"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val price = o["price"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            PriceItem(start, price)
        }
    }

    /** Carbon Intensity (30-min) over [from]..[to] via the date-range endpoint (≤14 days). */
    suspend fun fetchCarbonRange(from: Instant, to: Instant): List<EmissionItem> {
        val url = "${Endpoints.CARBON_INTENSITY}/${ApiTime.minuteFmt.format(from)}/${ApiTime.minuteFmt.format(to)}"
        val data = json.parseToJsonElement(Http.get(url)).jsonObject["data"]!!.jsonArray
        return data.mapNotNull { el ->
            val o = el.jsonObject
            val fromStr = o["from"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val intensity = o["intensity"]!!.jsonObject
            val v = intensity["actual"]?.jsonPrimitive?.intOrNull
                ?: intensity["forecast"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            EmissionItem(fromStr, v)
        }
    }

    /** NESO embedded solar/wind — the rolling CSV (one download, filtered locally). */
    suspend fun fetchEmbedded(): List<EmbeddedReading> = neso.fetchEmbedded()

    /** `/stream` returns a bare array; other endpoints wrap it in `{ "data": [...] }`. */
    private fun dataArray(text: String): JsonArray {
        val element = json.parseToJsonElement(text)
        return (element as? JsonArray) ?: element.jsonObject["data"]!!.jsonArray
    }
}
