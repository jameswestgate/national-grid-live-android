package com.crainiate.nationalgridlive.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crainiate.nationalgridlive.data.model.ChartGranularity
import com.crainiate.nationalgridlive.data.model.FuelCategory
import com.crainiate.nationalgridlive.data.model.FuelType
import com.crainiate.nationalgridlive.data.model.GridTimeSeries
import com.crainiate.nationalgridlive.data.model.Interconnector
import com.crainiate.nationalgridlive.data.model.Period
import com.crainiate.nationalgridlive.data.settings.SettingsRepository
import com.crainiate.nationalgridlive.ui.theme.FuelColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

data class ChartLine(val color: Color, val values: List<Double?>)

/** One row of the selection tooltip: optional colour dot + label, plus a value. */
class TooltipRow(val label: String?, val color: Color?, val value: String)

/**
 * Y-axis spec computed with the site's algorithm (`Axes.php`): ONE shared range
 * per metric across ALL FOUR period series, zero always included, the step from
 * a fixed ladder, bounds rounded outwards to step multiples — so switching tabs
 * never rescales a graph, exactly like grid.iamkate.com.
 */
data class MetricAxis(val minimum: Double, val maximum: Double, val step: Double) {
    val gridValues: List<Double>
        get() = generateSequence(minimum) { it + step }.takeWhile { it <= maximum + step / 2 }.toList()

    companion object {
        fun of(values: List<Double?>): MetricAxis {
            var lo = 0.0
            var hi = 0.0
            values.forEach { v ->
                if (v != null && v.isFinite()) {
                    if (v < lo) lo = v
                    if (v > hi) hi = v
                }
            }
            val range = hi - lo
            val step = when {
                range > 2000 -> 500.0
                range > 1000 -> 200.0
                range > 500 -> 100.0
                range > 200 -> 50.0
                range > 100 -> 20.0
                range > 50 -> 10.0
                range > 20 -> 5.0
                range > 10 -> 2.0
                else -> 1.0
            }
            return MetricAxis(step * floor(lo / step), step * ceil(hi / step), step)
        }
    }
}

/**
 * Multi-line chart drawn to the site's spec: 250dp plot, a gridline + label at
 * every axis step (y labels in the caption grey, x labels in the value black),
 * null-gap-aware lines, and a tap/scrub selection that pins a white tooltip
 * card until the user taps off the graph.
 */
@Composable
fun LineChart(
    lines: List<ChartLine>,
    axis: MetricAxis,
    xLabels: List<Pair<Float, String>>,
    yFormat: (Double) -> String,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    onSelect: (Int) -> Unit = {},
    tooltip: ((Int) -> Pair<String, List<TooltipRow>>?)? = null
) {
    val n = lines.maxOfOrNull { it.values.size } ?: 0
    if (n < 2) return

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val yLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val xLabelColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = MaterialTheme.typography.labelMedium
    val lo = axis.minimum
    val hi = axis.maximum

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(250.dp)) {
            Column(
                Modifier.width(46.dp).fillMaxHeight().padding(end = 6.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                axis.gridValues.reversed().forEach {
                    Text(yFormat(it), style = labelStyle, color = yLabelColor, maxLines = 1)
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(
                    Modifier
                        .matchParentSize()
                        .pointerInput(n) {
                            detectTapGestures { pos ->
                                onSelect(((pos.x / size.width) * (n - 1)).roundToInt().coerceIn(0, n - 1))
                            }
                        }
                        .pointerInput(n) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                onSelect(((change.position.x / size.width) * (n - 1)).roundToInt().coerceIn(0, n - 1))
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    fun yPos(v: Double) = (h - (v - lo) / (hi - lo) * h).toFloat()
                    axis.gridValues.forEach { gv ->
                        val y = yPos(gv)
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    }
                    lines.forEach { line ->
                        val path = Path()
                        var started = false
                        line.values.forEachIndexed { i, v ->
                            if (v == null || !v.isFinite()) { started = false; return@forEachIndexed }
                            val x = i.toFloat() / (n - 1) * w
                            val y = yPos(v)
                            if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
                        }
                        drawPath(
                            path,
                            color = line.color,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                    selectedIndex?.let { idx ->
                        val x = idx.toFloat() / (n - 1) * w
                        drawLine(yLabelColor.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx())
                    }
                }
                if (selectedIndex != null && tooltip != null) {
                    tooltip(selectedIndex)?.let { (title, rows) ->
                        val frac = selectedIndex / (n - 1f)
                        Box(
                            Modifier.matchParentSize().padding(4.dp),
                            contentAlignment = BiasAlignment(frac * 2f - 1f, -1f)
                        ) {
                            TooltipCard(title, rows)
                        }
                    }
                }
            }
        }
        if (xLabels.isNotEmpty()) {
            XAxisLabels(xLabels, labelStyle, xLabelColor)
        }
    }
}

/** X-axis labels centred at fractional positions under the plot, clamped to its edges. */
@Composable
private fun XAxisLabels(labels: List<Pair<Float, String>>, style: TextStyle, color: Color) {
    Layout(
        content = { labels.forEach { Text(it.second, style = style, color = color, maxLines = 1) } },
        modifier = Modifier.fillMaxWidth().padding(start = 46.dp, top = 4.dp)
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { i, p ->
                val cx = (labels[i].first * constraints.maxWidth).toInt()
                p.place((cx - p.width / 2).coerceIn(0, (constraints.maxWidth - p.width).coerceAtLeast(0)), 0)
            }
        }
    }
}

/** The pinned selection's floating value card — the app's card surface and label styling. */
@Composable
private fun TooltipCard(title: String, rows: List<TooltipRow>) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 3.dp
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            rows.forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (row.color != null) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(row.color))
                        Spacer(Modifier.width(5.dp))
                    }
                    if (row.label != null) {
                        Text(
                            row.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(row.value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            // ≈22dp of air between the heading and the plot, matching the
            // app's other card graphics (and the iOS chart cards).
            Spacer(Modifier.height(22.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(entries: List<Pair<String, Color>>) {
    FlowRow(
        Modifier.fillMaxWidth().padding(start = 46.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        entries.forEach { (name, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(5.dp))
                Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/* ---------------- the Trends section ---------------- */

/** The site draws the pumped transfers line in `.pumped { color: #09c; }`. */
private val PumpedLineColor = Color(0xFF0099CC)

/** The site draws the demand graph's transfers line in `.transfers { color: #aaa; }`. */
private val TransfersLineColor = Color(0xFF9E9E9E)

@Composable
fun TrendsSection(
    series: GridTimeSeries,
    axesSeries: List<GridTimeSeries>,
    modifier: Modifier = Modifier
) {
    if (series.isEmpty) return
    val showLegends by SettingsRepository.showGraphLegends.collectAsStateWithLifecycle()

    // The series each tab PLOTS, matching the rows the site's graphs draw:
    // day = 48 half-hours, week = 7 days (native), year = 52 Monday-start
    // weeks, all-time = one point per calendar year.
    val plotted = remember(series) { chartSeries(series) }
    // Shared per-metric axes scanned across all four periods' plotted series.
    val axes = remember(axesSeries, series) {
        ChartAxes((axesSeries.ifEmpty { listOf(series) }).map { chartSeries(it) })
    }
    val xs = remember(plotted) { xLabels(plotted) }
    val dates = plotted.dates
    val period = plotted.period
    val lineColor = MaterialTheme.colorScheme.onSurface

    // Pinned tooltip: one chart at a time, persists after the touch lifts,
    // cleared by tapping anywhere off the graphs (charts consume their taps)
    // and reset when the period's series changes.
    var selection by remember(plotted) { mutableStateOf<Pair<String, Int>?>(null) }
    fun selected(id: String): Int? = selection?.takeIf { it.first == id }?.second

    Column(
        modifier
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures { selection = null } },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ChartCard("Price per MWh") {
            LineChart(
                lines = listOf(ChartLine(lineColor, plotted.price)),
                axis = axes.price,
                xLabels = xs,
                yFormat = { fmt(it, 0, prefix = "£") },
                selectedIndex = selected("price"),
                onSelect = { selection = "price" to it },
                tooltip = { i ->
                    plotted.price.getOrNull(i)?.let { v ->
                        tooltipTitle(dates[i], period) to listOf(TooltipRow(null, null, fmt(v, 2, prefix = "£")))
                    }
                }
            )
        }

        ChartCard("Emissions per kWh") {
            LineChart(
                lines = listOf(ChartLine(lineColor, plotted.emissions)),
                axis = axes.emissions,
                xLabels = xs,
                yFormat = { fmt(it, 0, suffix = "g") },
                selectedIndex = selected("emissions"),
                onSelect = { selection = "emissions" to it },
                tooltip = { i ->
                    plotted.emissions.getOrNull(i)?.let { v ->
                        tooltipTitle(dates[i], period) to listOf(TooltipRow(null, null, fmt(v, 0, suffix = "g")))
                    }
                }
            )
        }

        // The site's Demand graph is FIVE lines: demand plus the equation
        // groups (fossils / renewables / others / transfers).
        val demandLines = remember(plotted) { DemandLines(plotted) }
        val demandEntries = listOf(
            Triple("Demand", lineColor, demandLines.demand),
            Triple("Fossil fuels", FuelColors.color(FuelCategory.Fossil), demandLines.fossils),
            Triple("Renewables", FuelColors.color(FuelCategory.Renewable), demandLines.renewables),
            Triple("Other sources", FuelColors.color(FuelCategory.Other), demandLines.others),
            Triple("Transfers", TransfersLineColor, demandLines.transfers)
        )
        ChartCard("Demand") {
            LineChart(
                lines = demandEntries.map { ChartLine(it.second, it.third) },
                axis = axes.demand,
                xLabels = xs,
                yFormat = { fmt(it, 0, suffix = "GW") },
                selectedIndex = selected("demand"),
                onSelect = { selection = "demand" to it },
                tooltip = { i ->
                    val rows = demandEntries.mapNotNull { (label, color, values) ->
                        values.getOrNull(i)?.let { TooltipRow(label, color, fmt(it, 1, suffix = "GW")) }
                    }
                    if (rows.isEmpty()) null else tooltipTitle(dates[i], period) to rows
                }
            )
            if (showLegends) Legend(demandEntries.map { it.first to it.second })
        }

        ChartCard("Generation") {
            LineChart(
                lines = FuelType.entries.map { ChartLine(FuelColors.color(it), plotted.fuels[it] ?: emptyList()) },
                axis = axes.generation,
                xLabels = xs,
                yFormat = { fmt(it, 0, suffix = "GW") },
                selectedIndex = selected("generation"),
                onSelect = { selection = "generation" to it },
                tooltip = { i ->
                    val rows = FuelType.entries.mapNotNull { fuel ->
                        plotted.fuels[fuel]?.getOrNull(i)?.let {
                            TooltipRow(fuel.displayName, FuelColors.color(fuel), fmt(it, 2, suffix = "GW"))
                        }
                    }
                    if (rows.isEmpty()) null else tooltipTitle(dates[i], period) to rows
                }
            )
            if (showLegends) Legend(FuelType.entries.map { it.displayName to FuelColors.color(it) })
        }

        ChartCard("Transfers") {
            LineChart(
                lines = Interconnector.entries.map { ChartLine(FuelColors.color(it), plotted.interconnectors[it] ?: emptyList()) } +
                    ChartLine(PumpedLineColor, plotted.pumped),
                axis = axes.transfers,
                xLabels = xs,
                yFormat = { fmt(it, 0, suffix = "GW") },
                selectedIndex = selected("transfers"),
                onSelect = { selection = "transfers" to it },
                tooltip = { i ->
                    val rows = Interconnector.entries.mapNotNull { ic ->
                        plotted.interconnectors[ic]?.getOrNull(i)?.let {
                            TooltipRow(ic.displayName, FuelColors.color(ic), fmt(it, 2, suffix = "GW"))
                        }
                    } + listOfNotNull(plotted.pumped.getOrNull(i)?.let {
                        TooltipRow("Pumped storage", PumpedLineColor, fmt(it, 2, suffix = "GW"))
                    })
                    if (rows.isEmpty()) null else tooltipTitle(dates[i], period) to rows
                }
            )
            if (showLegends) Legend(
                Interconnector.entries.map { it.displayName to FuelColors.color(it) } +
                    ("Pumped storage" to PumpedLineColor)
            )
        }
    }
}

/* ---------------- shared axes + demand lines ---------------- */

private class ChartAxes(all: List<GridTimeSeries>) {
    val price = MetricAxis.of(all.flatMap { it.price })
    val emissions = MetricAxis.of(all.flatMap { it.emissions })
    val demand = MetricAxis.of(all.flatMap { DemandLines(it).allValues })
    val generation = MetricAxis.of(all.flatMap { s -> FuelType.entries.flatMap { s.fuels[it] ?: emptyList() } })
    val transfers = MetricAxis.of(all.flatMap { s ->
        Interconnector.entries.flatMap { s.interconnectors[it] ?: emptyList() } + s.pumped
    })
}

/** The five lines of the site's Demand graph, derived per bucket. */
private class DemandLines(private val s: GridTimeSeries) {
    private fun sum(types: List<FuelType>): List<Double?> = s.dates.indices.map { i ->
        val vals = types.mapNotNull { s.fuels[it]?.getOrNull(i) }
        if (vals.isEmpty()) null else vals.sum()
    }

    val demand: List<Double?> = s.demand
    val fossils = sum(listOf(FuelType.Gas, FuelType.Coal))
    val renewables = sum(listOf(FuelType.Wind, FuelType.Solar, FuelType.Hydro))
    val others = sum(listOf(FuelType.Nuclear, FuelType.Biomass))
    val transfers: List<Double?> = s.dates.indices.map { i ->
        val ics = Interconnector.entries.mapNotNull { s.interconnectors[it]?.getOrNull(i) }
        val pumped = s.pumped.getOrNull(i)
        if (ics.isEmpty() && pumped == null) null else ics.sum() + (pumped ?: 0.0)
    }

    val allValues: List<Double?> get() = demand + fossils + renewables + others + transfers
}

/* ---------------- chart-only series grouping ---------------- */

/** Year → 52 Monday-start weeks; All time → one point per calendar year. */
private fun chartSeries(s: GridTimeSeries): GridTimeSeries = when (s.period) {
    Period.Year -> groupSeries(s, ChartGranularity.Day) { date ->
        runCatching {
            LocalDate.parse(date).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
        }.getOrDefault(date)
    }
    Period.AllTime -> groupSeries(s, ChartGranularity.Month) { date -> date.take(4) + "-01" }
    else -> s
}

private fun groupSeries(
    s: GridTimeSeries,
    granularity: ChartGranularity,
    keyOf: (String) -> String
): GridTimeSeries {
    val keys = ArrayList<String>()
    val groups = LinkedHashMap<String, MutableList<Int>>()
    s.dates.forEachIndexed { i, d ->
        val k = keyOf(d)
        groups.getOrPut(k) { keys.add(k); ArrayList() }.add(i)
    }

    fun mean(arr: List<Double?>, idx: List<Int>): Double? {
        val vals = idx.mapNotNull { arr.getOrNull(it) }
        return if (vals.isEmpty()) null else vals.average()
    }
    fun gather(arr: List<Double?>): List<Double?> = keys.map { mean(arr, groups.getValue(it)) }

    return GridTimeSeries(
        period = s.period,
        granularity = granularity,
        dates = keys,
        price = gather(s.price),
        emissions = gather(s.emissions),
        demand = gather(s.demand),
        fuels = s.fuels.mapValues { gather(it.value) },
        interconnectors = s.interconnectors.mapValues { gather(it.value) },
        pumped = gather(s.pumped)
    )
}

/* ---------------- labels + formatting ---------------- */

private val LONDON = ZoneId.of("Europe/London")
private val HH_MM = DateTimeFormatter.ofPattern("HH:mm")
private val H_MMA = DateTimeFormatter.ofPattern("h:mma", Locale.UK)
/** Single-letter weekdays (M T W T F S S) — full names can't fit 7 unrotated slots. */
private val WEEKDAY_NARROW = DateTimeFormatter.ofPattern("EEEEE", Locale.UK)
private val EEEE_FMT = DateTimeFormatter.ofPattern("EEEE", Locale.UK)
/** "Jul 25" — half the width of the site's 14/07/2025; the tooltip has the exact date. */
private val MMM_YY = DateTimeFormatter.ofPattern("MMM yy", Locale.UK)
private val DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.UK)

/**
 * Label positions per period, mirroring the site (`Tabs.php` timeStep with its
 * centred offset): day = every 6 hours, week = every day, year = quarterly,
 * all-time = every year.
 */
private fun xLabels(s: GridTimeSeries): List<Pair<Float, String>> {
    val n = s.dates.size
    if (n < 2) return emptyList()
    val step = when (s.period) {
        Period.Day -> 12
        Period.Week -> 1
        Period.Year -> 13
        Period.AllTime -> 1
    }
    val offset = ceil(step / 2.0).toInt()
    return (0 until n)
        .filter { (it + offset) % step == 0 }
        .map { i -> (i / (n - 1f)) to xLabel(s.dates[i], s.period) }
}

private fun xLabel(value: String, period: Period): String = runCatching {
    when (period) {
        Period.Day -> OffsetDateTime.parse(value).atZoneSameInstant(LONDON).format(HH_MM)
        Period.Week -> LocalDate.parse(value).format(WEEKDAY_NARROW)
        Period.Year -> LocalDate.parse(value).format(MMM_YY)
        Period.AllTime -> YearMonth.parse(value).year.toString()
    }
}.getOrDefault(value)

/** The tooltip's time heading ("8:45pm" / "Monday" / "14/07/2025" / "2025"). */
private fun tooltipTitle(value: String, period: Period): String = runCatching {
    when (period) {
        Period.Day -> OffsetDateTime.parse(value).atZoneSameInstant(LONDON).format(H_MMA).lowercase()
        Period.Week -> LocalDate.parse(value).format(EEEE_FMT)
        Period.Year -> LocalDate.parse(value).format(DMY)
        Period.AllTime -> YearMonth.parse(value).year.toString()
    }
}.getOrDefault(value)

private fun fmt(v: Double, decimals: Int, prefix: String = "", suffix: String = ""): String {
    val sign = if (v < 0) "−" else ""
    return sign + prefix + String.format(Locale.UK, "%.${decimals}f", abs(v)) + suffix
}
