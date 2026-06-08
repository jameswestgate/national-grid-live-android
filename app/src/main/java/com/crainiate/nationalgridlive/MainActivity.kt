package com.crainiate.nationalgridlive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crainiate.nationalgridlive.data.settings.SettingsRepository
import com.crainiate.nationalgridlive.ui.NationalGridApp
import com.crainiate.nationalgridlive.ui.theme.NationalGridLiveTheme
import com.crainiate.nationalgridlive.ui.widget.GridWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Optional deep link: `adb shell am start -e startTab historic …`.
        val startTab = intent?.getStringExtra("startTab")
        setContent {
            val theme by SettingsRepository.theme.collectAsStateWithLifecycle()
            val colorScheme by SettingsRepository.colorScheme.collectAsStateWithLifecycle()
            val darkTheme = theme.isDark(isSystemInDarkTheme())
            NationalGridLiveTheme(darkTheme = darkTheme, colorScheme = colorScheme) {
                NationalGridApp(startTab = startTab)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Push the latest in-app data to any home-screen widgets (cache re-render,
        // no fetch) — the analogue of iOS WidgetCenter.reloadAllTimelines().
        CoroutineScope(Dispatchers.IO).launch { GridWidgets.updateAll(applicationContext) }
    }
}
