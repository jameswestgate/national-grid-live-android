package com.crainiate.nationalgridlive.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    Live("live", "Live", Icons.Rounded.Bolt),
    Historic("historic", "Historic", Icons.AutoMirrored.Rounded.ShowChart),
    About("about", "About", Icons.Rounded.Info),
    Settings("settings", "Settings", Icons.Rounded.Settings);

    companion object {
        fun fromRoute(route: String?): Destination =
            entries.firstOrNull { it.route == route } ?: Live
    }
}
