package com.crainiate.nationalgridlive.data.repository

import android.content.Context
import com.crainiate.nationalgridlive.data.cache.LiveDataCache
import com.crainiate.nationalgridlive.data.remote.LiveDataAggregator
import java.io.File

/**
 * Minimal manual dependency graph. [init] (from the Application) wires the real
 * [RemoteGridRepository] with an on-disk live cache; before init (e.g. Compose
 * previews / unit tests) the getter returns [MockGridRepository].
 */
object DataModule {
    @Volatile private var repository: GridRepository? = null

    fun init(context: Context) {
        val cache = LiveDataCache(File(context.applicationContext.filesDir, "live-store.json"))
        repository = RemoteGridRepository(aggregator = LiveDataAggregator(cache = cache))
    }

    val gridRepository: GridRepository get() = repository ?: MockGridRepository()
}
