package com.crainiate.nationalgridlive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crainiate.nationalgridlive.data.model.GridSnapshot
import com.crainiate.nationalgridlive.ui.theme.FuelColors

/** Elevated card with an inline header (title + total GW + "% of demand") and a list of rows. */
@Composable
fun SourceListCard(
    title: String,
    totalGw: Double,
    percentOfDemand: Double,
    modifier: Modifier = Modifier,
    rows: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(gw1(totalGw), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            " GW",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Text(
                        "${percent(percentOfDemand)} of demand",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.size(6.dp))
            rows()
        }
    }
}

/** The Interconnectors card — 6 per-country net flows (alphabetical). */
@Composable
fun InterconnectorsCard(snapshot: GridSnapshot, modifier: Modifier = Modifier) {
    if (snapshot.interconnectors.isEmpty()) return
    val total = snapshot.transfersGw
    SourceListCard("Interconnectors", total, snapshot.shareOfDemand(total), modifier) {
        snapshot.interconnectors.forEachIndexed { index, reading ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 4.dp))
            val color = FuelColors.color(reading.interconnector)
            val barFraction = if (total != 0.0) (reading.gigawatts / total).toFloat() else 0f
            SourceRow(
                icon = Icons.AutoMirrored.Rounded.CompareArrows,
                iconColor = color,
                name = reading.interconnector.displayName,
                valueGw = reading.gigawatts,
                percentOfDemand = snapshot.shareOfDemand(reading.gigawatts),
                barFraction = barFraction,
                barColor = color
            )
        }
    }
}

/** The Storage card — pumped storage + a battery "no data" placeholder. */
@Composable
fun StorageCard(snapshot: GridSnapshot, modifier: Modifier = Modifier) {
    val pumped = snapshot.pumpedGw ?: return
    SourceListCard("Storage", pumped, snapshot.shareOfDemand(pumped), modifier) {
        SourceRow(
            icon = Icons.Rounded.Bolt,
            iconColor = FuelColors.pumped,
            name = "Pumped storage",
            valueGw = pumped,
            percentOfDemand = snapshot.shareOfDemand(pumped),
            barFraction = snapshot.shareOfDemand(kotlin.math.abs(pumped)).toFloat(),
            barColor = FuelColors.pumped
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 4.dp))
        PlaceholderRow(
            iconColor = FuelColors.batteryStorage,
            name = "Battery storage",
            note = "No data"
        )
    }
}

@Composable
private fun PlaceholderRow(iconColor: androidx.compose.ui.graphics.Color, name: String, note: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(percent = 50)).background(iconColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
