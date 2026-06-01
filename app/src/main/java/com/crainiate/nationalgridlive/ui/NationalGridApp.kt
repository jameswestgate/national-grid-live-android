package com.crainiate.nationalgridlive.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.crainiate.nationalgridlive.ui.about.AboutScreen
import com.crainiate.nationalgridlive.ui.historic.HistoricScreen
import com.crainiate.nationalgridlive.ui.live.LiveScreen
import com.crainiate.nationalgridlive.ui.nav.Destination
import com.crainiate.nationalgridlive.ui.settings.SettingsScreen

@Composable
fun NationalGridApp(startTab: String? = null) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = Destination.fromRoute(backStackEntry?.destination?.route)
    val startRoute = Destination.fromRoute(startTab).route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Destination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = current == dest,
                        onClick = {
                            if (current != dest) navController.navigate(dest.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Live.route) { LiveScreen() }
            composable(Destination.Historic.route) { HistoricScreen() }
            composable(Destination.About.route) { AboutScreen() }
            composable(Destination.Settings.route) { SettingsScreen() }
        }
    }
}
