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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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
 * deltas since the last refresh into an append-only on-disk [LiveDataStore],
 * and composes views that match grid.iamkate.com exactly:
 *  - **current** — latest 5-min FUELINST slot + the latest COMPLETE half-hour's
 *    embedded/price/emissions (Kate's `Database.php` latest-state merge).
 *  - **day** — the last 48 complete half-hours; **week** — the 7 most recent
 *    complete UTC days, excluding today.
 *
 * The store reaches back to the UTC midnight 7 days before today's (the week
 * window start, ~8 days); a source whose cache doesn't reach that far is
 * re-fetched in full once (first launch / upgrade / long offline gap), after
 * which refreshes are incremental from the newest cached bucket. Embedded
 * rows are upserted wholesale every refresh, mirroring the site.
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
        val (from, to) = windowBounds(s, period)
        val snapshot = composeWindow(s, from, to, period.label)
        check(snapshot.generationGw > 0.0) { "no windowed data available" }
        return snapshot
    }

    /**
     * Site-matching window bounds (half-open `[from, to)`):
     *  - **Day** = the last 48 COMPLETE half-hours, the newest being
     *    floor((latest 5-min reading − 25 min)/30 min) — never a partial bucket
     *    (Kate: `past_half_hours ORDER BY time DESC LIMIT 48`).
     *  - **Week** = the 7 most recent complete UTC days, EXCLUDING the partial
     *    current day (Kate: `past_days ORDER BY time DESC LIMIT 1,7`).
     */
    private fun windowBounds(s: LiveDataStore, period: Period): Pair<Instant, Instant> {
        val latest = s.latestGenerationInstant() ?: Instant.now()
        return if (period == Period.Week) {
            val todayMidnight = ApiTime.bucket(latest, ONE_DAY)
            todayMidnight.minusSeconds(SEVEN_DAYS) to todayMidnight
        } else {
            val lastComplete = ApiTime.bucket(latest.minusSeconds(25 * 60), ApiTime.HALF_HOUR)
            lastComplete.minusSeconds(47 * ApiTime.HALF_HOUR) to lastComplete.plusSeconds(ApiTime.HALF_HOUR)
        }
    }

    /** Bucketed time series for the Trends charts (Day = 30-min, Week = hourly). */
    suspend fun series(period: Period): GridTimeSeries {
        val s = refreshed()
        val (from, to) = windowBounds(s, period)
        val (stride, gran) = if (period == Period.Week) {
            ONE_HOUR to ChartGranularity.Hour
        } else {
            ApiTime.HALF_HOUR to ChartGranularity.HalfHour
        }
        val end = to.minusSeconds(stride)   // last bucket starts one stride before the window end
        var b = ApiTime.bucket(from, stride)

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
                // demand = generation (fuels excl. pumped) + transfers (ICs + pumped)
                demand.add(agg.fuelGw.values.sum() + agg.icGw.values.sum() + (agg.pumped ?: 0.0))
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

    // The whole fetch+parse+merge runs on IO: the one-time coverage backfill
    // parses ~46k FUELINST items, far too heavy for the main dispatcher (it
    // blocked the first frame long enough for an ActivityManager start-timeout
    // kill on a slow emulator).
    private suspend fun fetchAndMerge(s: LiveDataStore, now: Instant): LiveDataStore = withContext(Dispatchers.IO) {
        // The site's "Past week" averages the 7 most recent COMPLETE UTC days
        // (excluding today), so the store must reach back to the UTC midnight 7
        // days before today's — up to ~8 days of data. When a source's cache
        // doesn't reach that far (first launch, upgrade, long offline gap) we
        // re-fetch its whole window once; afterwards refreshes are incremental
        // from the newest cached bucket. (FUELINST accepts the full ~8-day range
        // in one request, ~8 MB; the market index is chunked — its API rejects
        // ranges over 7 days; Carbon Intensity allows 14 days.)
        val coverageStart = ApiTime.bucket(now, ONE_DAY).minusSeconds(SEVEN_DAYS)

        // 1h slack so a missing bucket right at the boundary doesn't force a
        // full re-fetch on every refresh.
        fun fetchFrom(latest: Instant?, earliest: Instant?): Instant =
            if (latest != null && earliest != null &&
                !earliest.isAfter(coverageStart.plusSeconds(ONE_HOUR))) latest else coverageStart

        val genFrom = fetchFrom(s.latestGenerationInstant(), s.earliestGenerationInstant())
        val emisFrom = fetchFrom(s.latestEmissionsInstant(), s.earliestEmissionsInstant())
        val priceFrom = fetchFrom(s.latestPriceInstant(), s.earliestPriceInstant())

        val genResult = runCatching { api.fetchFuelInst(genFrom, now) }
        genResult.onSuccess { items ->
            items.forEach { item ->
                val t = ApiTime.parse(item.startTime) ?: return@forEach
                val key = ApiTime.bucketKey(t, ApiTime.FIVE_MIN)
                s.generation.getOrPut(key) { mutableMapOf() }[item.fuelType] = item.mw
            }
        }.onFailure { Log.w(TAG, "generation fetch failed", it) }
        _online.value = genResult.isSuccess

        runCatching { fetchMarketIndexChunked(priceFrom, now) }.onSuccess { items ->
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
            // Upsert EVERY row on every refresh: NESO revises recent (often
            // forecast-flagged) periods and the site re-reads the whole CSV each
            // cron run (ON DUPLICATE KEY UPDATE), so frozen first-fetch values
            // drift from it. The old `latestEmbedded` incremental filter was also
            // defeated by the CSV's future forecast rows (max key ≈ +7 days),
            // which blocked all updates forever. The CSV is one download anyway.
            rows.forEach { row ->
                val key = ApiTime.bucketKey(row.instant, ApiTime.HALF_HOUR)
                s.embeddedWind[key] = row.windMw
                s.embeddedSolar[key] = row.solarMw
            }
        }.onFailure { Log.w(TAG, "embedded (NESO) fetch failed", it) }

        s.trim(ApiTime.iso(coverageStart))
        s.lastFetchedAt = ApiTime.iso(now)
        cache.write(s)
        s
    }

    /** The market-index endpoint rejects ranges over 7 days; fetch in chunks. */
    private suspend fun fetchMarketIndexChunked(from: Instant, to: Instant): List<PriceItem> {
        val out = ArrayList<PriceItem>()
        var start = from
        val maxChunk = (6.5 * 24 * 60 * 60).toLong()
        while (start.isBefore(to)) {
            val end = minOf(start.plusSeconds(maxChunk), to)
            out += api.fetchMarketIndex(start, end)
            start = end
        }
        return out
    }

    /* ---------------- current point (Kate's latest-state rule) ---------------- */

    private fun composeCurrent(s: LiveDataStore, now: Instant): GridSnapshot {
        // Kate's Database.php composes the live state as merge(latest complete
        // half-hour row, latest five-minute row): the latest 5-min FUELINST slot
        // supplies the displayed time + fuel mix, and the latest COMPLETE
        // half-hour — floor((slot − 25 min) / 30 min) — supplies embedded
        // solar/wind, price AND emissions. A 14:00 reading therefore pairs with
        // the 13:30 embedded row even though NESO has already published a
        // fresher 14:00 forecast row (verified vs the site 2026-06-02; iOS
        // LiveDataAggregator.currentPoint mirrors the same rule).
        val slotKey = s.generation.keys.maxOrNull()
        val slotStart = slotKey?.let(ApiTime::parse)
        val anchorKey = slotStart?.let { ApiTime.bucketKey(it.minusSeconds(25L * 60), ApiTime.HALF_HOUR) }

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

        // Embedded wind/solar from the anchor half-hour (or the latest row
        // before it if NESO is lagging) — never the period containing the slot.
        if (anchorKey != null) {
            val embKey = if (s.embeddedWind.containsKey(anchorKey)) anchorKey
            else s.embeddedWind.keys.filter { it <= anchorKey }.maxOrNull()
            if (embKey != null) {
                fuelGw[FuelType.Wind] = (fuelGw[FuelType.Wind] ?: 0.0) + (s.embeddedWind[embKey] ?: 0.0) / 1000.0
                fuelGw[FuelType.Solar] = (fuelGw[FuelType.Solar] ?: 0.0) + (s.embeddedSolar[embKey] ?: 0.0) / 1000.0
            }
        }

        // Price/emissions from the same anchor, falling back to their most
        // recent value ≤ anchor (the site propagates previous values forward).
        val emissions = anchorKey?.let { k ->
            s.emissions[k] ?: s.emissions.keys.filter { it <= k }.maxOrNull()?.let { s.emissions[it] }
        } ?: 0
        val price = anchorKey?.let { k ->
            s.price[k] ?: s.price.keys.filter { it <= k }.maxOrNull()?.let { s.price[it] }
        } ?: 0.0
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
