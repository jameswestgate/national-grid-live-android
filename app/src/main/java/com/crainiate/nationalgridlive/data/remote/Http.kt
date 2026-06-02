package com.crainiate.nationalgridlive.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Shared OkHttp client + a tiny suspend GET helper for the open data APIs. */
object Http {
    // Generous read/call timeouts: the one-time coverage backfill pulls ~8 days
    // of FUELINST (~8 MB) in a single request, which can stall past a short
    // read timeout on slow connections (or the emulator's NAT).
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class)
    suspend fun get(url: String, accept: String = "application/json"): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Accept", accept)
                .header("User-Agent", "national-grid-live-android")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
                response.body?.string() ?: throw IOException("Empty body for $url")
            }
        }
}
