package com.crainiate.nationalgridlive.data.remote

import java.io.IOException

/**
 * Fetches the hosted backfill snapshot (Past year / All time aggregates). HTTPS,
 * so no network-security config is needed. Returns the raw JSON for the parser.
 */
class SnapshotService {
    @Throws(IOException::class)
    suspend fun fetchSnapshotJson(): String = Http.get(Endpoints.SNAPSHOT)
}
