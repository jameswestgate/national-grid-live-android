package com.crainiate.nationalgridlive.data.cache

import com.crainiate.nationalgridlive.data.remote.ApiTime
import kotlinx.serialization.Serializable

/**
 * Append-only on-disk store of the raw readings from the live APIs, keyed by
 * ISO-8601 bucket-start strings (so it round-trips JSON and sorts chronologically).
 * Ported from the iOS `LiveDataStore`. Trimmed to the last 7 days on every refresh.
 */
@Serializable
data class LiveDataStore(
    /** 5-min buckets: ISO → (Elexon code → MW). */
    val generation: MutableMap<String, MutableMap<String, Double>> = mutableMapOf(),
    /** 30-min buckets: ISO → emissions gCO₂/kWh. */
    val emissions: MutableMap<String, Int> = mutableMapOf(),
    /** 30-min buckets: ISO → market-index £/MWh. */
    val price: MutableMap<String, Double> = mutableMapOf(),
    /** 30-min buckets: ISO → embedded wind MW (NESO). */
    val embeddedWind: MutableMap<String, Double> = mutableMapOf(),
    /** 30-min buckets: ISO → embedded solar MW (NESO). */
    val embeddedSolar: MutableMap<String, Double> = mutableMapOf(),
    var lastFetchedAt: String? = null
) {
    fun trim(cutoffIso: String) {
        generation.keys.retainAll { it >= cutoffIso }
        emissions.keys.retainAll { it >= cutoffIso }
        price.keys.retainAll { it >= cutoffIso }
        embeddedWind.keys.retainAll { it >= cutoffIso }
        embeddedSolar.keys.retainAll { it >= cutoffIso }
    }

    val latestGenerationIso: String? get() = generation.keys.maxOrNull()
    val latestEmissionsIso: String? get() = emissions.keys.maxOrNull()
    val latestPriceIso: String? get() = price.keys.maxOrNull()
    val latestEmbeddedIso: String? get() = embeddedWind.keys.maxOrNull()

    fun latestGenerationInstant() = latestGenerationIso?.let(ApiTime::parse)
    fun latestEmissionsInstant() = latestEmissionsIso?.let(ApiTime::parse)
    fun latestPriceInstant() = latestPriceIso?.let(ApiTime::parse)
    fun latestEmbeddedInstant() = latestEmbeddedIso?.let(ApiTime::parse)
}
