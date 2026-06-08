package com.crainiate.nationalgridlive.ui.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.crainiate.nationalgridlive.MainActivity
import com.crainiate.nationalgridlive.R
import com.crainiate.nationalgridlive.data.model.FuelType
import com.crainiate.nationalgridlive.data.model.GridSnapshot
import com.crainiate.nationalgridlive.ui.theme.FuelColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/* ---------------- colours ----------------
 * Solid surface matching the app's page background (the colour behind the cards /
 * nav). The app uses a FIXED brand scheme (dynamicColor=false → NOT Material You),
 * switching only on system light/dark — so the widget mirrors that with day/night
 * ColorProviders built from the same md_* tokens (Theme.kt / Color.kt). */
private val WidgetBg: ColorProvider = ColorProvider(R.color.widget_background)
private val WidgetText: ColorProvider = ColorProvider(R.color.widget_text)
private val WidgetTextDim: ColorProvider = ColorProvider(R.color.widget_text_dim)

/* ---------------- shared scaffold + text styles ---------------- */

/** Transparent, rounded widget surface that opens the app on tap. */
@Composable
private fun WidgetScaffold(content: @Composable () -> Unit) {
    val context = LocalContext.current
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                // Solid surface matching the app's page background.
                .background(WidgetBg)
                .cornerRadius(20.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
                // Tight horizontal padding to claw back width for a narrower widget.
                .padding(horizontal = 10.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { content() }
    }
}

private fun heroValue() =
    TextStyle(color = WidgetText, fontSize = 30.sp, fontWeight = FontWeight.Bold)

// Price / carbon numbers — primary (dark) text, like the demand number.
private fun heroNum() =
    TextStyle(color = WidgetText, fontSize = 17.sp, fontWeight = FontWeight.Medium)

// Units (GW, £, g/kWh) and glyph values — dim secondary text.
private fun caption(color: ColorProvider = WidgetTextDim) =
    TextStyle(color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)

private fun label() =
    TextStyle(color = WidgetTextDim, fontSize = 15.sp, fontWeight = FontWeight.Medium)

private fun value() =
    TextStyle(color = WidgetText, fontSize = 30.sp, fontWeight = FontWeight.Bold)

private fun gw1(v: Double): String = String.format(Locale.UK, "%.1f", v)

/* ---------------- shared glyph row ---------------- */

/** One source in a glyph row: a fuel-coloured glyph + its GW value. */
private data class GlyphChip(val iconRes: Int, val color: Color, val value: Double)

/** A compressed row of glyph chips. Each glyph sits in a tinted circular badge
 *  (fuel colour @ 18%, glyph on top) — the same treatment as the Live page source
 *  rows. Each chip is its own inner Row so the outer Row never exceeds Glance's
 *  10-direct-child limit (which silently drops trailing children). */
@Composable
private fun GlyphRow(
    chips: List<GlyphChip>,
    badgeSize: Dp = 30.dp,
    iconSize: Dp = 16.dp,
    gap: Dp = 3.dp,
    chipSpacing: Dp = 6.dp,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        chips.forEachIndexed { index, chip ->
            Row(
                modifier = if (index > 0) GlanceModifier.padding(start = chipSpacing)
                else GlanceModifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier.size(badgeSize)
                        .cornerRadius(badgeSize / 2)
                        .background(ColorProvider(chip.color.copy(alpha = 0.18f))),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(chip.iconRes),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(ColorProvider(chip.color)),
                        modifier = GlanceModifier.size(iconSize)
                    )
                }
                Spacer(GlanceModifier.width(gap))
                Text(gw1(chip.value), style = caption(WidgetText))
            }
        }
    }
}

/* ---------------- Live-minimal (the one Android widget) ---------------- */

@Composable
fun LiveMinimalWidgetContent(snapshot: GridSnapshot?) {
    WidgetScaffold {
        if (snapshot == null) {
            Text("National Grid", style = label())
            Spacer(GlanceModifier.height(2.dp))
            Text("—", style = value())
            return@WidgetScaffold
        }
        // Hero line: demand (rounded, no decimal) · price · carbon (as "NNN g").
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${snapshot.equationDemand.roundToInt()}", style = heroValue())
            Text(" GW", style = caption())
            Spacer(GlanceModifier.width(6.dp))
            // Price + carbon: numbers in the primary (dark) colour, units dim.
            Text("£", style = caption())
            Text("${snapshot.priceGbpPerMwh.roundToInt()}", style = heroNum())
            Spacer(GlanceModifier.width(6.dp))
            Text("${snapshot.emissionsGPerKwh}", style = heroNum())
            Text(" g/kWh", style = caption())
        }
        Spacer(GlanceModifier.height(8.dp))
        // Sub line: the 4 largest sources by |value| across generation fuels,
        // interconnectors and storage (NOT the generation total), each a tinted glyph.
        val chips = sourceChips(snapshot)
            .sortedByDescending { abs(it.value) }
            .take(4)
        GlyphRow(chips)
    }
}

/** Every individual source — generation fuels, interconnectors and storage —
 *  as an icon/colour/value chip (the generation total is deliberately omitted). */
private fun sourceChips(snapshot: GridSnapshot): List<GlyphChip> = buildList {
    snapshot.allFuels.forEach {
        add(GlyphChip(fuelIconRes(it.type), FuelColors.color(it.type), it.gigawatts))
    }
    snapshot.interconnectors.forEach {
        add(GlyphChip(R.drawable.ic_arrows, FuelColors.color(it.interconnector), it.gigawatts))
    }
    snapshot.pumpedGw?.let {
        add(GlyphChip(R.drawable.ic_battery, FuelColors.pumped, it))
    }
}

private fun fuelIconRes(type: FuelType): Int = when (type) {
    FuelType.Gas, FuelType.Coal -> R.drawable.ic_flame
    FuelType.Wind -> R.drawable.ic_air
    FuelType.Solar -> R.drawable.ic_sun
    FuelType.Hydro -> R.drawable.ic_drop
    FuelType.Nuclear -> R.drawable.ic_bolt
    FuelType.Biomass -> R.drawable.ic_leaf
}
