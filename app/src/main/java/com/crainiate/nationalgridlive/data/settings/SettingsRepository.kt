package com.crainiate.nationalgridlive.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiny persisted settings store (SharedPreferences) exposed as [StateFlow]s so
 * Compose can react. Initialised once from [com.crainiate.nationalgridlive.NationalGridLiveApp].
 */
object SettingsRepository {

    private const val PREFS = "settings"
    private const val KEY_THEME = "appTheme"
    private const val KEY_VISUALISATION = "generationVisualisation"

    private lateinit var prefs: SharedPreferences

    private val _theme = MutableStateFlow(AppTheme.System)
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _visualisation = MutableStateFlow(GenerationVisualisation.Bar)
    val visualisation: StateFlow<GenerationVisualisation> = _visualisation.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _theme.value = prefs.getString(KEY_THEME, null)?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
            ?: AppTheme.System
        _visualisation.value = prefs.getString(KEY_VISUALISATION, null)
            ?.let { runCatching { GenerationVisualisation.valueOf(it) }.getOrNull() }
            ?: GenerationVisualisation.Bar
    }

    fun setTheme(value: AppTheme) {
        _theme.value = value
        prefs.edit().putString(KEY_THEME, value.name).apply()
    }

    fun setVisualisation(value: GenerationVisualisation) {
        _visualisation.value = value
        prefs.edit().putString(KEY_VISUALISATION, value.name).apply()
    }
}
