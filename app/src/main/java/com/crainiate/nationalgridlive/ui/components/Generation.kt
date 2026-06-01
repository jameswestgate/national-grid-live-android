package com.crainiate.nationalgridlive.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crainiate.nationalgridlive.data.model.FuelCategory
import com.crainiate.nationalgridlive.data.model.FuelType
import com.crainiate.nationalgridlive.data.model.GridSnapshot
import com.crainiate.nationalgridlive.data.settings.GenerationVisualisation
import com.crainiate.nationalgridlive.data.settings.SettingsRepository
import com.crainiate.nationalgridlive.ui.theme.FuelColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.hypot

enum class Chevron { None, Right, Down }

/** What a tapped bar/donut segment refers to. */
sealed interface GenerationSelection {
    data class Category(val category: FuelCategory) : GenerationSelection
    data class Fuel(val type: FuelType) : GenerationSelection
}

/* ---------------- thin progress line ---------------- */

@Composable
fun ThinBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
    }
}

/* ---------------- stacked category/fuel bars ---------------- */

private data class BarSeg(
    val label: String,
    val fraction: Float,
    val color: Color,
    val selection: GenerationSelection
)

@Composable
fun GenerationBars(
    snapshot: GridSnapshot,
    onSelect: (GenerationSelection) -> Unit,
    modifier: Modifier = Modifier
) {
    val categorySegs = snapshot.categories.map {
        BarSeg(
            it.category.displayName,
            snapshot.shareOfGeneration(it.gigawatts).toFloat(),
            FuelColors.color(it.category),
            GenerationSelection.Category(it.category)
        )
    }
    val fuelSegs = snapshot.allFuels.map {
        BarSeg(
            it.type.displayName,
            snapshot.shareOfGeneration(it.gigawatts).toFloat(),
            FuelColors.color(it.type),
            GenerationSelection.Fuel(it.type)
        )
    }
    // Both bars span the same width over the same total with NO inter-segment gaps,
    // so the fuel boundaries line up exactly under their category boundaries
    // (Gas+Coal == Fossil, Wind starts under Renewables, Hydro ends with it).
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        BarRow(categorySegs, onSelect)
        BarRow(fuelSegs, onSelect)
    }
}

@Composable
private fun BarRow(segments: List<BarSeg>, onSelect: (GenerationSelection) -> Unit) {
    Row(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(8.dp))) {
        segments.forEach { seg ->
            if (seg.fraction <= 0f) return@forEach
            Box(
                Modifier
                    .weight(seg.fraction)
                    .fillMaxHeight()
                    .background(seg.color)
                    .clickable { onSelect(seg.selection) },
                contentAlignment = Alignment.CenterStart
            ) {
                if (seg.fraction > 0.10f) {
                    Text(
                        text = seg.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onColor(seg.color),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.padding(start = 9.dp, end = 4.dp)
                    )
                }
            }
        }
    }
}

/* ---------------- donut (the iOS "signature" graphic) ---------------- */

/**
 * Two concentric rings (matching the iOS donut): INNER = the three category
 * totals, OUTER = individual fuels nesting under their category. No centre label;
 * tapping a slice (inner or outer) selects that category/fuel via [onSelect].
 */
@Composable
fun GenerationDonut(
    snapshot: GridSnapshot,
    onSelect: (GenerationSelection) -> Unit,
    modifier: Modifier = Modifier
) {
    val total = snapshot.generationGw
    Box(modifier.fillMaxWidth().height(208.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .size(196.dp)
                .pointerInput(snapshot) {
                    detectTapGestures { pos ->
                        if (total <= 0) return@detectTapGestures
                        val full = minOf(size.width, size.height) / 2f
                        val dx = pos.x - size.width / 2f
                        val dy = pos.y - size.height / 2f
                        val rFrac = hypot(dx, dy) / full
                        var ang = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
                        if (ang < 0f) ang += 360f
                        when {
                            rFrac in 0.44f..0.705f -> { // inner ring → category
                                var s = 0f
                                snapshot.categories.forEach { c ->
                                    val sweep = (c.gigawatts / total).toFloat() * 360f
                                    if (ang >= s && ang < s + sweep) {
                                        onSelect(GenerationSelection.Category(c.category)); return@detectTapGestures
                                    }
                                    s += sweep
                                }
                            }
                            rFrac in 0.72f..1.0f -> { // outer ring → fuel
                                var s = 0f
                                snapshot.categories.forEach { c ->
                                    c.fuels.forEach { f ->
                                        val sweep = (f.gigawatts / total).toFloat() * 360f
                                        if (ang >= s && ang < s + sweep) {
                                            onSelect(GenerationSelection.Fuel(f.type)); return@detectTapGestures
                                        }
                                        s += sweep
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val full = size.minDimension / 2f
            val gapDeg = 1.6f

            fun ring(rcFrac: Float, strokeFrac: Float, color: Color, startDeg: Float, sweepDeg: Float) {
                if (sweepDeg <= 0f) return
                val rc = full * rcFrac
                val d = 2f * rc
                drawArc(
                    color = color,
                    startAngle = startDeg,
                    sweepAngle = sweepDeg,
                    useCenter = false,
                    topLeft = Offset(size.width / 2f - rc, size.height / 2f - rc),
                    size = Size(d, d),
                    style = Stroke(width = full * strokeFrac, cap = StrokeCap.Butt)
                )
            }

            fun share(gw: Double) = if (total > 0) (gw / total).toFloat() else 0f

            var start = -90f
            snapshot.categories.forEach { category ->
                val sweep = share(category.gigawatts) * 360f
                ring(0.5725f, 0.265f, FuelColors.color(category.category), start + gapDeg / 2f, sweep - gapDeg)
                start += sweep
            }
            start = -90f
            snapshot.categories.forEach { category ->
                category.fuels.forEach { fuel ->
                    val sweep = share(fuel.gigawatts) * 360f
                    ring(0.86f, 0.28f, FuelColors.color(fuel.type), start + gapDeg / 2f, sweep - gapDeg)
                    start += sweep
                }
            }
        }
    }
}

/* ---------------- a single source row ---------------- */

@Composable
fun SourceRow(
    icon: ImageVector,
    iconColor: Color,
    name: String,
    valueGw: Double,
    percentOfDemand: Double,
    barFraction: Float,
    barColor: Color,
    modifier: Modifier = Modifier,
    nameWeight: FontWeight = FontWeight.Medium,
    badgeSize: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    chevron: Chevron = Chevron.None,
    reserveChevronSpace: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    Row(
        rowModifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(badgeSize).clip(RoundedCornerShape(percent = 50)).background(iconColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(iconSize))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = nameWeight, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            ThinBar(barFraction, barColor)
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(gw(valueGw), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text(
                    " GW",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
            Text(percent(percentOfDemand), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when (chevron) {
            Chevron.None -> if (reserveChevronSpace) Spacer(Modifier.width(22.dp))
            Chevron.Right -> ChevronIcon(Icons.Rounded.ChevronRight)
            Chevron.Down -> ChevronIcon(Icons.Rounded.ExpandMore)
        }
    }
}

@Composable
private fun ChevronIcon(icon: ImageVector) {
    Box(Modifier.width(22.dp), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/* ---------------- the generation hero card ---------------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GenerationCard(snapshot: GridSnapshot, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(setOf(FuelCategory.Renewable)) }
    val visualisation by SettingsRepository.visualisation.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val requesters = remember { FuelCategory.entries.associateWith { BringIntoViewRequester() } }

    // Tapping a bar/donut slice expands that category and scrolls it into view.
    fun select(selection: GenerationSelection) {
        val category = when (selection) {
            is GenerationSelection.Category -> selection.category
            is GenerationSelection.Fuel -> selection.type.category
        }
        expanded = expanded + category
        scope.launch {
            delay(60) // let the expansion lay out before scrolling
            requesters[category]?.bringIntoView()
        }
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Generation",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp
                )
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(gw1(snapshot.generationGw), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            " GW",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Text(
                        "${percent(snapshot.generationShareOfDemand)} of demand",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            when (visualisation) {
                GenerationVisualisation.Bar -> GenerationBars(snapshot, onSelect = ::select)
                GenerationVisualisation.Donut -> GenerationDonut(snapshot, onSelect = ::select)
                GenerationVisualisation.Hidden -> {}
            }
            Spacer(Modifier.height(8.dp))

            snapshot.categories.forEachIndexed { index, category ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 4.dp))
                }
                val isExpanded = category.category in expanded
                val toggle = {
                    expanded = if (isExpanded) expanded - category.category else expanded + category.category
                }
                val header: @Composable () -> Unit = {
                    SourceRow(
                        icon = FuelColors.icon(category.category),
                        iconColor = FuelColors.color(category.category),
                        name = category.category.displayName,
                        valueGw = category.gigawatts,
                        percentOfDemand = snapshot.shareOfDemand(category.gigawatts),
                        barFraction = snapshot.shareOfGeneration(category.gigawatts).toFloat(),
                        barColor = FuelColors.color(category.category),
                        chevron = if (isExpanded) Chevron.Down else Chevron.Right,
                        onClick = toggle
                    )
                }

                Column(Modifier.bringIntoViewRequester(requesters.getValue(category.category))) {
                    if (isExpanded) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp)) {
                                header()
                                AnimatedVisibility(visible = true) {
                                    Column {
                                        category.fuels.forEach { fuel ->
                                            SourceRow(
                                                icon = FuelColors.icon(fuel.type),
                                                iconColor = FuelColors.color(fuel.type),
                                                name = fuel.type.displayName,
                                                valueGw = fuel.gigawatts,
                                                percentOfDemand = snapshot.shareOfDemand(fuel.gigawatts),
                                                barFraction = snapshot.shareOfGeneration(fuel.gigawatts).toFloat(),
                                                barColor = FuelColors.color(fuel.type),
                                                nameWeight = FontWeight.Normal,
                                                badgeSize = 34.dp,
                                                iconSize = 19.dp,
                                                reserveChevronSpace = true,
                                                modifier = Modifier.padding(start = 10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        header()
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Percentages show each source's share of total demand.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
    }
}
