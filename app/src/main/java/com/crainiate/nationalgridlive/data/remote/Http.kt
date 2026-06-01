package com.crainiate.nationalgridlive.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Shared OkHttp client + a tiny suspend GET helper for the open data APIs. */
object Http {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
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
