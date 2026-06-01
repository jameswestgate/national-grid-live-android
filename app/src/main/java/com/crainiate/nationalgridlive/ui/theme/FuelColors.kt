package com.crainiate.nationalgridlive.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.crainiate.nationalgridlive.data.model.FuelCategory
import com.crainiate.nationalgridlive.data.model.FuelType
import com.crainiate.nationalgridlive.data.model.Interconnector

/**
 * Fixed brand palette — verbatim from grid.iamkate.com's grid.css (CC0). These
 * encode the *data*, so unlike the Material You surface/accent roles they do NOT
 * follow the wallpaper; they stay constant in light and dark.
 */
object FuelColors {
    // Individual fuels
    val gas = Color(0xFFEE9944)
    val coal = Color(0xFFAA3355)
    val solar = Color(0xFFEEDD00)
    val wind = Color(0xFF99DD55)   // lime — intentionally distinct from renewable green
    val hydro = Color(0xFF22CCBB)
    val nuclear = Color(0xFF0099CC)
    val biomass = Color(0xFF3366BB)

    // Categories
    val fossil = Color(0xFFCC4455)
    val renewable = Color(0xFF55BB55)
    val other = Color(0xFF2277CC)

    // Interconnectors (grid.css — Kate reuses fuel hues per country)
    val france = Color(0xFFEE9944)
    val ireland = Color(0xFFEEDD00)
    val netherlands = Color(0xFF99DD55)
    val norway = Color(0xFF22CCBB)
    val belgium = Color(0xFF881177)
    val denmark = Color(0xFFCC6666)

    // Storage
    val pumped = Color(0xFF0099CC)
    val batteryStorage = Color(0xFF663399)

    fun color(fuel: FuelType): Color = when (fuel) {
        FuelType.Gas -> gas
        FuelType.Coal -> coal
        FuelType.Solar -> solar
        FuelType.Wind -> wind
        FuelType.Hydro -> hydro
        FuelType.Nuclear -> nuclear
        FuelType.Biomass -> biomass
    }

    fun color(category: FuelCategory): Color = when (category) {
        FuelCategory.Fossil -> fossil
        FuelCategory.Renewable -> renewable
        FuelCategory.Other -> other
    }

    fun icon(fuel: FuelType): ImageVector = when (fuel) {
        FuelType.Gas, FuelType.Coal -> Icons.Rounded.LocalFireDepartment
        FuelType.Wind -> Icons.Rounded.Air
        FuelType.Solar -> Icons.Rounded.WbSunny
        FuelType.Hydro -> Icons.Rounded.WaterDrop
        FuelType.Nuclear -> Icons.Rounded.Bolt
        FuelType.Biomass -> Icons.Rounded.Spa
    }

    fun icon(category: FuelCategory): ImageVector = when (category) {
        FuelCategory.Fossil -> Icons.Rounded.LocalFireDepartment
        FuelCategory.Renewable -> Icons.Rounded.Eco
        FuelCategory.Other -> Icons.Rounded.MoreHoriz
    }

    fun color(interconnector: Interconnector): Color = when (interconnector) {
        Interconnector.France -> france
        Interconnector.Ireland -> ireland
        Interconnector.Netherlands -> netherlands
        Interconnector.Norway -> norway
        Interconnector.Belgium -> belgium
        Interconnector.Denmark -> denmark
    }
}
