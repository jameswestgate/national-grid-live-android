package com.crainiate.nationalgridlive.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crainiate.nationalgridlive.data.model.GridSnapshot

/** One KPI tile: small caps label over a bold value with an optional small unit. */
data class StatItem(val label: String, val value: String, val unit: String? = null)

@Composable
fun StatCardsRow(items: List<StatItem>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            Surface(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
                    Text(
                        text = item.label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        item.unit?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The "demand = generation + transfers" summary card (secondary container). */
@Composable
fun DemandCard(snapshot: GridSnapshot, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Seg("Demand", snapshot.demandGw, Modifier.weight(1f))
            Operator("=")
            Seg("Generation", snapshot.generationGw, Modifier.weight(1f))
            Operator("+")
            Seg("Transfers", snapshot.transfersGw, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Seg(label: String, value: Double, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = gw1(value), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
            Text(
                text = "GW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 2.dp, bottom = 1.dp)
            )
        }
    }
}

@Composable
private fun Operator(symbol: String) {
    Text(
        text = symbol,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.55f),
        textAlign = TextAlign.Center,
        modifier = Modifier.wrapContentWidth().padding(horizontal = 6.dp)
    )
}
