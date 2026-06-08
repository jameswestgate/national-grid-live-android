package com.crainiate.nationalgridlive.ui.settings

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crainiate.nationalgridlive.data.settings.AppColorScheme
import com.crainiate.nationalgridlive.data.settings.AppTheme
import com.crainiate.nationalgridlive.data.settings.GenerationVisualisation
import com.crainiate.nationalgridlive.data.settings.SettingsRepository
import com.crainiate.nationalgridlive.ui.components.ScreenTitle

@Composable
fun SettingsScreen() {
    val theme by SettingsRepository.theme.collectAsStateWithLifecycle()
    val colorScheme by SettingsRepository.colorScheme.collectAsStateWithLifecycle()
    val visualisation by SettingsRepository.visualisation.collectAsStateWithLifecycle()
    val showLegends by SettingsRepository.showGraphLegends.collectAsStateWithLifecycle()
    var showWidgetHelp by remember { mutableStateOf(false) }
    if (showWidgetHelp) WidgetHelpDialog(onDismiss = { showWidgetHelp = false })

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

        Label("Theme colour", topPadding = 18.dp)
        ColorSchemeRow(selected = colorScheme, onSelect = SettingsRepository::setColorScheme)

        Label("Home screen", topPadding = 24.dp)
        AddWidgetRow(onClick = { showWidgetHelp = true })
    }
}

@Composable
private fun AddWidgetRow(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
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
            "Add a widget",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Walkthrough for adding the home-screen widget. Android can't place a widget for
 *  the user on most launchers, so — like the iOS app's Lock Screen explainer — this
 *  just describes the gesture. */
@Composable
private fun WidgetHelpDialog(onDismiss: () -> Unit) {
    val steps = listOf(
        "Touch and hold an empty part of your Home screen, then tap “Widgets”.",
        "Find “National Grid: Live” in the list.",
        "Touch and hold “Live Minimal”, then drag it onto your Home screen."
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } },
        title = { Text("Add a widget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                steps.forEachIndexed { index, step ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            step,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    "The Live Minimal widget shows demand, price and carbon with the top live sources, and refreshes automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/** A row of circular colour swatches for the "Theme colour" setting. */
@Composable
private fun ColorSchemeRow(selected: AppColorScheme, onSelect: (AppColorScheme) -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppColorScheme.entries.forEach { scheme ->
            SchemeSwatch(
                label = scheme.displayName,
                color = swatchColor(scheme, context),
                selected = scheme == selected,
                onClick = { onSelect(scheme) }
            )
        }
    }
}

@Composable
private fun SchemeSwatch(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(4.dp)
    ) {
        Box(
            Modifier
                .size(46.dp)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                )
                .padding(if (selected) 5.dp else 3.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A recognisable swatch colour per scheme (more saturated than the actual subtle
 *  tints so the dots are distinguishable). Wallpaper shows the live system accent. */
private fun swatchColor(scheme: AppColorScheme, context: Context): Color = when (scheme) {
    AppColorScheme.Green -> Color(0xFF5B8C5A)
    AppColorScheme.Sage -> Color(0xFF5E9E9E)
    AppColorScheme.Grey -> Color(0xFF9AA0A0)
    AppColorScheme.Dynamic ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(context).primary
        else Color(0xFF7E8C8C)
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
