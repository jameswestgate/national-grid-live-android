package com.crainiate.nationalgridlive.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crainiate.nationalgridlive.data.settings.AppTheme
import com.crainiate.nationalgridlive.data.settings.GenerationVisualisation
import com.crainiate.nationalgridlive.data.settings.SettingsRepository
import com.crainiate.nationalgridlive.ui.components.ScreenTitle

@Composable
fun SettingsScreen() {
    val theme by SettingsRepository.theme.collectAsStateWithLifecycle()
    val visualisation by SettingsRepository.visualisation.collectAsStateWithLifecycle()
    val showLegends by SettingsRepository.showGraphLegends.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ScreenTitle("Settings")

        Label("Charts")
        SegmentedRow(
            options = GenerationVisualisation.entries,
            selected = visualisation,
            labelOf = { it.displayName },
            onSelect = SettingsRepository::setVisualisation
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Show graph legends",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = showLegends, onCheckedChange = SettingsRepository::setShowGraphLegends)
        }

        Label("Appearance", topPadding = 24.dp)
        SegmentedRow(
            options = AppTheme.entries,
            selected = theme,
            labelOf = { it.displayName },
            onSelect = SettingsRepository::setTheme
        )

        Label("Lock screen", topPadding = 24.dp)
        WidgetRow()
    }
}

@Composable
private fun WidgetRow() {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Widgets,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            "Widget",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                "Coming soon",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun Label(text: String, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = topPadding, bottom = 10.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(labelOf(option), style = MaterialTheme.typography.labelLarge) }
            )
        }
    }
}
