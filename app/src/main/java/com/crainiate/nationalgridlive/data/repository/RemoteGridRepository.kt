package com.crainiate.nationalgridlive.data.repository

import android.util.Log
import com.crainiate.nationalgridlive.data.model.GridSnapshot
import com.crainiate.nationalgridlive.data.model.GridTimeSeries
import com.crainiate.nationalgridlive.data.model.Period
import com.crainiate.nationalgridlive.data.remote.LiveDataAggregator
import com.crainiate.nationalgridlive.data.remote.SnapshotParser
import com.crainiate.nationalgridlive.data.remote.SnapshotService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Real data source.
 *  - [live] + Historic **Day / Week** are composed by [LiveDataAggregator] from an
 *    incremental, on-disk 7-day rolling cache of the open APIs (Elexon FUELINST +
 *    NESO embedded + Carbon Intensity + market-index) — only deltas are fetched.
 *  - Historic **Year / All time** come from the hosted backfill `snapshot.json`.
 *
 * Every path falls back to [MockGridRepository] on error, so the UI always has data.
 */
class RemoteGridRepository(
    private val aggregator: LiveDataAggregator,
    private val snapshots: SnapshotService = SnapshotService(),
    private val fallback: GridRepository = MockGridRepository()
) : GridRepository {

    private val snapshotMutex = Mutex()
    @Volatile private var cachedSnapshot: SnapshotParser.Parsed? = null

    override val online: StateFlow<Boolean> = aggregator.online

    override suspend fun refresh() {
        aggregator.invalidate()
        snapshotMutex.withLock { cachedSnapshot = null }
    }

    override suspend fun live(): GridSnapshot = try {
        aggregator.current()
    } catch (t: Throwable) {
        Log.w(TAG, "Live aggregation failed, using fallback", t)
        fallback.live()
    }

    /** Cache-only current reading for the widgets (no network); null if uncached. */
    override suspend fun cachedLive(): GridSnapshot? = aggregator.cachedCurrent()

    override suspend fun historic(period: Period): GridSnapshot = when (period) {
        Period.Day, Period.Week -> try {
            aggregator.window(period)
        } catch (t: Throwable) {
            Log.w(TAG, "Window aggregation failed ($period), using fallback", t)
            fallback.historic(period)
        }
        Period.Year, Period.AllTime -> try {
            snapshot().snapshot(period) ?: fallback.historic(period)
        } catch (t: Throwable) {
            Log.w(TAG, "Snapshot fetch/parse failed, using fallback", t)
            fallback.historic(period)
        }
    }

    override suspend fun series(period: Period): GridTimeSeries = try {
        when (period) {
            Period.Day, Period.Week -> aggregator.series(period)
            Period.Year, Period.AllTime -> snapshot().series(period) ?: GridTimeSeries.empty(period)
        }
    } catch (t: Throwable) {
        Log.w(TAG, "Series fetch failed ($period)", t)
        GridTimeSeries.empty(period)
    }

    private suspend fun snapshot(): SnapshotParser.Parsed {
        cachedSnapshot?.let { return it }
        return snapshotMutex.withLock {
            cachedSnapshot ?: SnapshotParser.parse(snapshots.fetchSnapshotJson()).also { cachedSnapshot = it }
        }
    }

    private companion object {
        const val TAG = "RemoteGridRepository"
    }
}
