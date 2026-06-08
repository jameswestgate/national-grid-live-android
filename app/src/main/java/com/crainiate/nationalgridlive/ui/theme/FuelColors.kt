package com.crainiate.nationalgridlive.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
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
        FuelType.Gas -> Icons.Rounded.LocalFireDepartment
        FuelType.Coal -> Icons.Rounded.Landscape     // mountain (coal mine hill), like iOS
        FuelType.Wind -> Icons.Rounded.Air
        FuelType.Solar -> Icons.Rounded.WbSunny
        FuelType.Hydro -> Icons.Rounded.WaterDrop
        FuelType.Nuclear -> AtomIcon                  // atom, like the iOS SF Symbol
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

/**
 * Atom glyph — three elliptical electron orbits (0/60/120) + a nucleus — matching
 * the iOS SF Symbol "atom" used for Nuclear. The same path strings back the
 * widget's res/drawable/ic_atom.xml, so the app and widget render identically.
 * Drawn in black; call sites tint it via Icon(tint = …) to the fuel colour.
 */
val AtomIcon: ImageVector by lazy {
    val orbit = addPathNodes("M1.5,12 a10.5,4 0 1,0 21,0 a10.5,4 0 1,0 -21,0")
    val nucleus = addPathNodes("M10,12 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z")
    ImageVector.Builder(
        name = "Atom",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        listOf(0f, 60f, 120f).forEach { angle ->
            addGroup(rotate = angle, pivotX = 12f, pivotY = 12f)
            addPath(pathData = orbit, stroke = SolidColor(Color.Black), strokeLineWidth = 1.4f)
            clearGroup()
        }
        addPath(pathData = nucleus, fill = SolidColor(Color.Black))
    }.build()
}
