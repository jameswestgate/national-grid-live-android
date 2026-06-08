package com.crainiate.nationalgridlive.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.GlanceId
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.crainiate.nationalgridlive.data.repository.DataModule
import com.crainiate.nationalgridlive.data.model.GridSnapshot
import java.util.concurrent.TimeUnit

/* ---------------- data bridge (shared app sandbox — no App Group needed) ---------------- */

/** Bridges the home-screen widgets to the app's own data layer. */
object WidgetRepository {
    /** Instant, cache-only snapshot for rendering; null if nothing cached yet. */
    suspend fun cached(): GridSnapshot? = DataModule.gridRepository.cachedLive()

    /** Background fetch that writes the shared cache (used by the worker). */
    suspend fun refresh(): GridSnapshot? =
        runCatching { DataModule.gridRepository.live() }.getOrNull()
}

/* ---------------- widget ----------------
 * Android ships a single comprehensive widget (Live Minimal). iOS keeps three
 * smaller Lock Screen widgets because it has far less space — see the project
 * widgets memory note. */

class LiveMinimalWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetRepository.cached()
        if (snapshot == null) GridWidgets.refreshNow(context)
        provideContent { LiveMinimalWidgetContent(snapshot) }
    }
}

/** Refresh data when the widget is added/updated and keep a periodic background
 *  refresh running while it exists. */
class LiveMinimalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiveMinimalWidget()

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, manager, ids)
        GridWidgets.refreshNow(context)
        GridWidgets.schedulePeriodic(context)
    }
}

/* ---------------- refresh worker + scheduling ---------------- */

/** Fetches fresh data into the shared cache, then re-renders both widgets. */
class WidgetUpdateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        WidgetRepository.refresh()
        GridWidgets.updateAll(applicationContext)
        return Result.success()
    }
}

object GridWidgets {
    private const val PERIODIC = "widget-update"
    private const val ONESHOT = "widget-refresh-now"

    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Re-render the widget from the current cache (no fetch). */
    suspend fun updateAll(context: Context) {
        LiveMinimalWidget().updateAll(context)
    }

    /** One-off fetch + re-render (widget added, or app backgrounded). */
    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONESHOT, ExistingWorkPolicy.REPLACE, request)
    }

    /** ~30-min background refresh while any widget exists (data publishes every
     *  5 min, but a 30-min widget cadence is the battery-friendly floor). */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES)
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
