package com.crainiate.nationalgridlive.data.cache

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/** JSON-file persistence for [LiveDataStore] (e.g. `filesDir/live-store.json`). */
class LiveDataCache(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun read(): LiveDataStore? = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) null
            else json.decodeFromString(LiveDataStore.serializer(), file.readText())
        }.getOrElse {
            Log.w(TAG, "Failed to read live store", it); null
        }
    }

    suspend fun write(store: LiveDataStore) = withContext(Dispatchers.IO) {
        runCatching { file.writeText(json.encodeToString(LiveDataStore.serializer(), store)) }
            .onFailure { Log.w(TAG, "Failed to write live store", it) }
        Unit
    }

    private companion object { const val TAG = "LiveDataCache" }
}
