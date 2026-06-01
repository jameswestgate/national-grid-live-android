package com.crainiate.nationalgridlive.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crainiate.nationalgridlive.ui.components.ScreenTitle

private val PARAGRAPHS = listOf(
    "Between 12th January 1882, when the world's first coal-fired power station opened at 57 Holborn Viaduct in London, and 30th September 2024, when Great Britain's last coal-fired power station closed, the country burnt 4.6 billion tonnes of coal, emitting 10.6 billion tonnes of carbon dioxide.",
    "In 2001 the European Union updated the Large Combustion Plant Directive, obliging power stations to limit their emissions or close by 2015. Most older coal-fired power stations in Great Britain closed in response. The government's introduction of a carbon price floor in 2013, and its subsequent increase in 2015, made coal uncompetitive with gas, which rapidly replaced coal in the country's energy mix.",
    "At the same time, renewable power generation was steadily rising. Great Britain's exposed position in the north-east Atlantic makes it one of the best locations in the world for wind power, and the shallow waters of the North Sea host several of the world's largest offshore wind farms.",
    "Between 5:30pm and 6:00pm on 5th December 2025, British wind farms averaged a record 23.94GW of generation."
)

private val MILESTONES = listOf(
    "23GW" to "5th December 2025",
    "22GW" to "5th December 2024",
    "21GW" to "10th January 2023",
    "20GW" to "2nd November 2022",
    "15GW" to "18th December 2018",
    "10GW" to "8th December 2016"
)

private val SOURCES = listOf(
    "Elexon BMRS Insights",
    "Carbon Intensity API © National Grid ESO and University of Oxford (CC BY 4.0)",
    "NESO Data Portal (NESO Open Licence)"
)

@Composable
fun AboutScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenTitle("About")

        AboutCard {
            SectionTitle("The energy transition")
            PARAGRAPHS.forEach { Body(it) }
            Text(
                "Wind power records are set regularly",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp)
            )
            MilestonesTable()
        }

        AboutCard {
            SectionTitle("Data")
            Body("Live data is fetched directly from three public APIs. Historical aggregates are served by an open backfill snapshot.")
            SOURCES.forEach { Bullet(it) }
        }

        AboutCard {
            SectionTitle("Original design")
            Body("Inspired by National Grid: Live by Kate Morley (grid.iamkate.com), released under CC0 1.0 Universal.")
        }
    }
}

@Composable
private fun MilestonesTable() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(top = 2.dp)
    ) {
        TableRow("Power", "Date first achieved", header = true)
        MILESTONES.forEachIndexed { index, (power, date) ->
            TableRow(power, date, stripe = index % 2 == 0)
        }
    }
}

@Composable
private fun TableRow(power: String, date: String, header: Boolean = false, stripe: Boolean = false) {
    val weight = if (header) FontWeight.SemiBold else FontWeight.Normal
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (stripe) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(power, style = MaterialTheme.typography.bodyMedium, fontWeight = weight, modifier = Modifier.width(76.dp))
        Text(date, style = MaterialTheme.typography.bodyMedium, fontWeight = weight, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AboutCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            "·",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(14.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
