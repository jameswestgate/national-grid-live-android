package com.crainiate.nationalgridlive.data.remote

import android.util.Log
import com.crainiate.nationalgridlive.data.cache.LiveDataCache
import com.crainiate.nationalgridlive.data.cache.LiveDataStore
import com.crainiate.nationalgridlive.data.model.CategoryReading
import com.crainiate.nationalgridlive.data.model.FuelCategory
import com.crainiate.nationalgridlive.data.model.ChartGranularity
import com.crainiate.nationalgridlive.data.model.FuelReading
import com.crainiate.nationalgridlive.data.model.FuelType
import com.crainiate.nationalgridlive.data.model.GridSnapshot
import com.crainiate.nationalgridlive.data.model.GridTimeSeries
import com.crainiate.nationalgridlive.data.model.Interconnector
import com.crainiate.nationalgridlive.data.model.InterconnectorReading
import com.crainiate.nationalgridlive.data.model.Period
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Ported from the iOS `LiveDataAggregator`. Polls the open APIs, merges only the
 * deltas since the last refresh into an append-only on-disk [LiveDataStore]
 * (trimmed to 7 days), and composes views from the cache:
 *  - **current** — latest 5-min FUELINST slot + the matching 30-min emissions/
 *    price/embedded buckets (anchor logic mirrors the website's KPI selection).
 *  - **day / week** — mean of all cached buckets over the last 24h / 7d.
 *
 * Because each refresh asks each source for `[lastBucket, now]`, the only large
 * fetch is the first-launch FUELINST window (capped at 24h) — never the whole
 * 7-day series. The 30-min sources (price/emissions/embedded) seed the full week
 * cheaply; generation fills to a true 7-day window as the app is used.
 */
class LiveDataAggregator(
    private val cache: LiveDataCache,
    private val api: GridApiClient = GridApiClient()
) {
    private val mutex = Mutex()
    @Volatile private var store: LiveDataStore? = null
    @Volatile private var lastRefresh: Instant? = null

    private val _online = MutableStateFlow(true)
    /** False after a refresh attempt where the live (FUELINST) fetch failed. */
    val online: StateFlow<Boolean> = _online.asStateFlow()

    /** Force the next [current]/[window]/[series] call to re-fetch (pull-to-refresh). */
    fun invalidate() { lastRefresh = null }

    suspend fun current(): GridSnapshot {
        val s = refreshed()
        val snapshot = composeCurrent(s, Instant.now())
        check(snapshot.generationGw > 0.0) { "no live data available" }
        return snapshot
    }

    suspend fun window(period: Period): GridSnapshot {
        val s = refreshed()
        val now = Instant.now()
        val span = if (period == Period.Week) SEVEN_DAYS else ONE_DAY
        val snapshot = composeWindow(s, now.minusSeconds(span), now, period.label)
        check(snapshot.generationGw > 0.0) { "no windowed data available" }
        return snapshot
    }

    /** Bucketed time series for the Trends charts (Day = 30-min, Week = hourly). */
    suspend fun series(period: Period): GridTimeSeries {
        val s = refreshed()
        val now = Instant.now()
        val (span, stride, gran) = if (period == Period.Week) {
            Triple(SEVEN_DAYS, ONE_HOUR, ChartGranularity.Hour)
        } else {
            Triple(ONE_DAY, ApiTime.HALF_HOUR, ChartGranularity.HalfHour)
        }
        val end = ApiTime.bucket(now, stride)
        var b = ApiTime.bucket(now.minusSeconds(span), stride)

        val dates = ArrayList<String>()
        val price = ArrayList<Double?>()
        val emissions = ArrayList<Double?>()
        val demand = ArrayList<Double?>()
        val fuels = FuelType.entries.associateWith { ArrayList<Double?>() }
        val ics = Interconnector.entries.associateWith { ArrayList<Double?>() }

        while (!b.isAfter(end)) {
            val be = b.plusSeconds(stride)
            val agg = aggregateRange(s, ApiTime.iso(b), ApiTime.iso(be))
            dates.add(ApiTime.iso(b))
            price.add(agg.price)
            emissions.add(agg.emissions?.toDouble())
            if (agg.hasGen) {
                demand.add(agg.fuelGw.values.sum() + agg.icGw.values.sum())
                FuelType.entries.forEach { fuels.getValue(it).add(agg.fuelGw[it] ?: 0.0) }
                Interconnector.entries.forEach { ics.getValue(it).add(agg.icGw[it] ?: 0.0) }
            } else {
                demand.add(null)
                FuelType.entries.forEach { fuels.getValue(it).add(null) }
                Interconnector.entries.forEach { ics.getValue(it).add(null) }
            }
            b = be
        }
        return GridTimeSeries(period, gran, dates, price, emissions, demand, fuels, ics)
    }

    /* ---------------- incremental refresh ---------------- */

    private suspend fun refreshed(): LiveDataStore = mutex.withLock {
        val now = Instant.now()
        var s = store ?: cache.read() ?: LiveDataStore()
        val recent = lastRefresh?.let { Duration.between(it, now).seconds < MIN_REFRESH_SECONDS } ?: false
        if (recent && s.generation.isNotEmpty()) {
            store = s
            return@withLock s
        }
        s = fetchAndMerge(s, now)
        store = s
        lastRefresh = now
        s
    }

    private suspend fun fetchAndMerge(s: LiveDataStore, now: Instant): LiveDataStore {
        val sevenDaysAgo = now.minusSeconds(SEVEN_DAYS)
        val seed = now.minusSeconds(ONE_DAY)

        // FUELINST (5-min): incremental from the last cached bucket, but cap the
        // catch-up at 24h so a long absence never triggers a multi-MB pull.
        val genFrom = s.latestGenerationInstant()
            ?.let { maxOf(it, now.minusSeconds(ONE_DAY)) } ?: seed
        // 30-min sources are cheap even over the full week → seed the whole window.
        val emisFrom = s.latestEmissionsInstant() ?: sevenDaysAgo
        val priceFrom = s.latestPriceInstant() ?: sevenDaysAgo
        val embedFrom = s.latestEmbeddedInstant() ?: sevenDaysAgo

        val genResult = runCatching { api.fetchFuelInst(genFrom, now) }
        genResult.onSuccess { items ->
            items.forEach { item ->
                val t = ApiTime.parse(item.startTime) ?: return@forEach
                val key = ApiTime.bucketKey(t, ApiTime.FIVE_MIN)
                s.generation.getOrPut(key) { mutableMapOf() }[item.fuelType] = item.mw
            }
        }.onFailure { Log.w(TAG, "generation fetch failed", it) }
        _online.value = genResult.isSuccess

        runCatching { api.fetchMarketIndex(priceFrom, now) }.onSuccess { items ->
            items.forEach { item ->
                val t = ApiTime.parse(item.startTime) ?: return@forEach
                s.price[ApiTime.bucketKey(t, ApiTime.HALF_HOUR)] = item.price
            }
        }.onFailure { Log.w(TAG, "price fetch failed", it) }

        runCatching { api.fetchCarbonRange(emisFrom, now) }.onSuccess { items ->
            items.forEach { item ->
                val t = ApiTime.parse(item.from) ?: return@forEach
                s.emissions[ApiTime.bucketKey(t, ApiTime.HALF_HOUR)] = item.value
            }
        }.onFailure { Log.w(TAG, "emissions fetch failed", it) }

        runCatching { api.fetchEmbedded() }.onSuccess { rows ->
            rows.filter { !it.instant.isBefore(embedFrom) }.forEach { row ->
                val key = ApiTime.bucketKey(row.instant, ApiTime.HALF_HOUR)
                s.embeddedWind[key] = row.windMw
                s.embeddedSolar[key] = row.solarMw
            }
        }.onFailure { Log.w(TAG, "embedded (NESO) fetch failed", it) }

        s.trim(ApiTime.iso(sevenDaysAgo))
        s.lastFetchedAt = ApiTime.iso(now)
        cache.write(s)
        return s
    }

    /* ---------------- current point (iOS anchor logic) ---------------- */

    private fun composeCurrent(s: LiveDataStore, now: Instant): GridSnapshot {
        val anchorKey = s.emissions.keys.maxOrNull()
            ?: s.price.keys.maxOrNull()
            ?: s.embeddedWind.keys.maxOrNull()
        val anchorStart = anchorKey?.let(ApiTime::parse)

        // Generation slot: latest 5-min start within the half-hour AFTER the
        // anchor (the current fuel mix); else the latest available slot.
        val slotKey: String? = if (anchorStart != null) {
            val anchorEnd = anchorStart.plusSeconds(ApiTime.HALF_HOUR)
            val limit = anchorEnd.plusSeconds(ApiTime.HALF_HOUR)
            s.generation.keys
                .filter { k -> ApiTime.parse(k)?.let { !it.isBefore(anchorEnd) && it.isBefore(limit) } == true }
                .maxOrNull() ?: s.generation.keys.maxOrNull()
        } else {
            s.generation.keys.maxOrNull()
        }

        val fuelGw = HashMap<FuelType, Double>()
        val icGw = HashMap<Interconnector, Double>()
        var pumped: Double? = null
        slotKey?.let { s.generation[it] }?.forEach { (code, mw) ->
            val gw = mw / 1000.0
            val fuel = FuelCodeMap.fuel(code)
            val ic = FuelCodeMap.interconnector(code)
            when {
                fuel != null -> fuelGw[fuel] = (fuelGw[fuel] ?: 0.0) + gw
                ic != null -> icGw[ic] = (icGw[ic] ?: 0.0) + gw
                code == FuelCodeMap.PUMPED -> pumped = (pumped ?: 0.0) + gw
            }
        }

        // Embedded for the half-hour containing the slot (or the latest before it).
        val slotStart = slotKey?.let(ApiTime::parse)
        if (slotStart != null) {
            val hhKey = ApiTime.bucketKey(slotStart, ApiTime.HALF_HOUR)
            val embKey = if (s.embeddedWind.containsKey(hhKey)) hhKey
            else s.embeddedWind.keys.filter { it <= hhKey }.maxOrNull()
            if (embKey != null) {
                fuelGw[FuelType.Wind] = (fuelGw[FuelType.Wind] ?: 0.0) + (s.embeddedWind[embKey] ?: 0.0) / 1000.0
                fuelGw[FuelType.Solar] = (fuelGw[FuelType.Solar] ?: 0.0) + (s.embeddedSolar[embKey] ?: 0.0) / 1000.0
            }
        }

        val emissions = anchorKey?.let { s.emissions[it] } ?: 0
        val price = anchorKey?.let { s.price[it] } ?: 0.0
        val label = (slotStart ?: now).let {
            ZonedDateTime.ofInstant(it, LONDON).format(HH_MM)
        }
        return buildSnapshot(label, fuelGw, icGw, pumped, emissions, price)
    }

    /* ---------------- windowed means (day / week) ---------------- */

    private fun composeWindow(s: LiveDataStore, from: Instant, to: Instant, label: String): GridSnapshot {
        val agg = aggregateRange(s, ApiTime.iso(from), ApiTime.iso(to))
        return buildSnapshot(label, agg.fuelGw, agg.icGw, agg.pumped, agg.emissions ?: 0, agg.price ?: 0.0)
    }

    /** Per-fuel / per-interconnector means over [fromIso, toIso) — the building block for both windows and series. */
    private data class RangeAgg(
        val fuelGw: Map<FuelType, Double>,
        val icGw: Map<Interconnector, Double>,
        val pumped: Double?,
        val emissions: Int?,
        val price: Double?,
        val hasGen: Boolean
    )

    private fun aggregateRange(s: LiveDataStore, fromIso: String, toIso: String): RangeAgg {
        val genKeys = s.generation.keys.filter { it >= fromIso && it < toIso }
        val fuelSum = HashMap<FuelType, Double>()
        val icSum = HashMap<Interconnector, Double>()
        var pumpedSum = 0.0
        genKeys.forEach { key ->
            s.generation[key]?.forEach { (code, mw) ->
                val gw = mw / 1000.0
                val fuel = FuelCodeMap.fuel(code)
                val ic = FuelCodeMap.interconnector(code)
                when {
                    fuel != null -> fuelSum[fuel] = (fuelSum[fuel] ?: 0.0) + gw
                    ic != null -> icSum[ic] = (icSum[ic] ?: 0.0) + gw
                    code == FuelCodeMap.PUMPED -> pumpedSum += gw
                }
            }
        }
        val n = genKeys.size
        val fuelGw = HashMap<FuelType, Double>()
        val icGw = HashMap<Interconnector, Double>()
        var pumped: Double? = null
        if (n > 0) {
            fuelSum.forEach { (fuel, sum) -> fuelGw[fuel] = sum / n }
            icSum.forEach { (ic, sum) -> icGw[ic] = sum / n }
            pumped = pumpedSum / n

            val embKeys = s.embeddedWind.keys.filter { it >= fromIso && it < toIso }
            if (embKeys.isNotEmpty()) {
                fuelGw[FuelType.Wind] = (fuelGw[FuelType.Wind] ?: 0.0) +
                    embKeys.mapNotNull { s.embeddedWind[it] }.average() / 1000.0
                fuelGw[FuelType.Solar] = (fuelGw[FuelType.Solar] ?: 0.0) +
                    embKeys.mapNotNull { s.embeddedSolar[it] }.average() / 1000.0
            }
        }

        val emisKeys = s.emissions.keys.filter { it >= fromIso && it < toIso }
        val emissions = if (emisKeys.isEmpty()) null else emisKeys.mapNotNull { s.emissions[it] }.average().roundToInt()
        val priceKeys = s.price.keys.filter { it >= fromIso && it < toIso }
        val price = if (priceKeys.isEmpty()) null else priceKeys.mapNotNull { s.price[it] }.average()

        return RangeAgg(fuelGw, icGw, pumped, emissions, price, n > 0)
    }

    /* ---------------- shared snapshot builder ---------------- */

    private fun buildSnapshot(
        label: String,
        fuelGw: Map<FuelType, Double>,
        icGw: Map<Interconnector, Double>,
        pumpedGw: Double?,
        emissions: Int,
        price: Double
    ): GridSnapshot {
        fun g(fuel: FuelType) = fuelGw[fuel] ?: 0.0
        val categories = listOfNotNull(
            category(FuelCategory.Fossil, FuelType.Gas to g(FuelType.Gas), FuelType.Coal to g(FuelType.Coal)),
            category(FuelCategory.Renewable, FuelType.Wind to g(FuelType.Wind), FuelType.Solar to g(FuelType.Solar), FuelType.Hydro to g(FuelType.Hydro)),
            category(FuelCategory.Other, FuelType.Nuclear to g(FuelType.Nuclear), FuelType.Biomass to g(FuelType.Biomass))
        )
        val interconnectors = Interconnector.entries.map { InterconnectorReading(it, icGw[it] ?: 0.0) }
        // Transfers = interconnectors + storage (pumped), matching grid.iamkate.com's
        // `Transfers` grouping. Generation already excludes pumped; demand = gen + transfers.
        val transfers = interconnectors.sumOf { it.gigawatts } + (pumpedGw ?: 0.0)
        return GridSnapshot(
            periodLabel = label,
            priceGbpPerMwh = price,
            emissionsGPerKwh = emissions,
            demandGw = categories.sumOf { it.gigawatts } + transfers,
            transfersGw = transfers,
            categories = categories,
            interconnectors = interconnectors,
            pumpedGw = pumpedGw
        )
    }

    private fun category(category: FuelCategory, vararg fuels: Pair<FuelType, Double>): CategoryReading {
        // Keep every member fuel, even at 0.00 (e.g. Solar at night) — matches Kate,
        // which shows "Solar 0.00 GW". Zero segments are skipped only in the bars.
        val members = fuels.map { FuelReading(it.first, it.second) }
        return CategoryReading(category, members.sumOf { it.gigawatts }, members)
    }

    private companion object {
        const val TAG = "LiveDataAggregator"
        const val ONE_HOUR = 60L * 60
        const val ONE_DAY = 24L * 60 * 60
        const val SEVEN_DAYS = 7L * 24 * 60 * 60
        const val MIN_REFRESH_SECONDS = 4L * 60   // dedup refreshes across tabs
        val LONDON: ZoneId = ZoneId.of("Europe/London")
        val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
