package com.crainiate.nationalgridlive.data.repository

import com.crainiate.nationalgridlive.data.model.GridSnapshot
import com.crainiate.nationalgridlive.data.model.GridTimeSeries
import com.crainiate.nationalgridlive.data.model.Period
import kotlinx.coroutines.flow.StateFlow

/**
 * Source of grid data for the screens.
 *
 * Two implementations are provided:
 *  - [MockGridRepository] — instant, deterministic data matching the design
 *    mockups. Used by default so the app builds and runs out of the box.
 *  - [RemoteGridRepository] — fetches live data from the open APIs (Elexon BMRS,
 *    Carbon Intensity) and history from the hosted backfill `snapshot.json`.
 *    See that class + the README for the remaining JSON→domain mapping.
 */
interface GridRepository {
    /** Latest near-real-time reading (last settlement period). */
    suspend fun live(): GridSnapshot

    /**
     * The latest reading composed from the on-disk cache only — no network.
     * Used by the home-screen widgets for an instant render in the shared app
     * sandbox; null when nothing has been cached yet.
     */
    suspend fun cachedLive(): GridSnapshot? = null

    /** Period-averaged reading for the Historic screen. */
    suspend fun historic(period: Period): GridSnapshot

    /** Time series for the Historic Trends charts (empty when unavailable). */
    suspend fun series(period: Period): GridTimeSeries

    /** True while the live APIs are reachable; false after a failed refresh. */
    val online: StateFlow<Boolean>

    /** Invalidate caches so the next read re-fetches (pull-to-refresh). */
    suspend fun refresh()
}
