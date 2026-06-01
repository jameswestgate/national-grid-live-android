package com.crainiate.nationalgridlive.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crainiate.nationalgridlive.data.model.ChartGranularity
import com.crainiate.nationalgridlive.data.model.GridTimeSeries
import com.crainiate.nationalgridlive.data.model.Interconnector
import com.crainiate.nationalgridlive.data.model.FuelType
import com.crainiate.nationalgridlive.ui.theme.FuelColors
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class ChartLine(val color: Color, val values: List<Double?>)

/** A minimal multi-line chart: 3 y gridlines + labels, x labels, null-gap-aware. */
@Composable
fun LineChart(
    lines: List<ChartLine>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    includeZero: Boolean = true,
    yFormat: (Double) -> String
) {
    val all = lines.flatMap { it.values }.filterNotNull().filter { it.isFinite() }
    if (all.size < 2) return

    var lo = all.min()
    var hi = all.max()
    if (includeZero) { lo = minOf(lo, 0.0); hi = maxOf(hi, 0.0) }
    if (hi <= lo) hi = lo + 1.0
    hi += (hi - lo) * 0.08
    val mid = (hi + lo) / 2.0

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val n = lines.maxOf { it.values.size }

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(height)) {
            Column(
                Modifier.width(46.dp).fillMaxHeight().padding(end = 6.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(yFormat(hi), style = labelStyle, color = labelColor)
                Text(yFormat(mid), style = labelStyle, color = labelColor)
                Text(yFormat(lo), style = labelStyle, color = labelColor)
            }
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                val w = size.width
                val h = size.height
                fun yPos(v: Double) = (h - (v - lo) / (hi - lo) * h).toFloat()
                listOf(hi, mid, lo).forEach { gv ->
                    val y = yPos(gv)
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }
                if (lo < 0.0 && hi > 0.0) {
                    val zy = yPos(0.0)
                    drawLine(gridColor, Offset(0f, zy), Offset(w, zy), strokeWidth = 2.5f)
                }
                if (n >= 2) {
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
                }
            }
        }
        if (xLabels.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 46.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                xLabels.forEach { Text(it, style = labelStyle, color = labelColor) }
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
            Spacer(Modifier.height(14.dp))
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

@Composable
fun TrendsSection(series: GridTimeSeries, modifier: Modifier = Modifier) {
    if (series.isEmpty) return
    val xs = xLabels(series)
    val metric = MaterialTheme.colorScheme.primary

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Trends",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        ChartCard("Price (£/MWh)") {
            LineChart(listOf(ChartLine(metric, series.price)), xs) { "£${it.roundToInt()}" }
        }
        ChartCard("Carbon intensity (g/kWh)") {
            LineChart(listOf(ChartLine(metric, series.emissions)), xs) { "${it.roundToInt()}" }
        }
        ChartCard("Demand (GW)") {
            LineChart(listOf(ChartLine(metric, series.demand)), xs) { fmtGw(it) }
        }

        val fuelEntries = FuelType.entries.filter { series.fuels[it]?.any { v -> v != null } == true }
        if (fuelEntries.isNotEmpty()) {
            ChartCard("Generation by source (GW)") {
                LineChart(fuelEntries.map { ChartLine(FuelColors.color(it), series.fuels.getValue(it)) }, xs) { fmtGw(it) }
                Legend(fuelEntries.map { it.displayName to FuelColors.color(it) })
            }
        }

        val icEntries = Interconnector.entries.filter { series.interconnectors[it]?.any { v -> v != null } == true }
        if (icEntries.isNotEmpty()) {
            ChartCard("Transfers by interconnector (GW)") {
                LineChart(icEntries.map { ChartLine(FuelColors.color(it), series.interconnectors.getValue(it)) }, xs) { fmtGw(it) }
                Legend(icEntries.map { it.displayName to FuelColors.color(it) })
            }
        }
    }
}

private fun fmtGw(v: Double): String = if (v == v.roundToInt().toDouble()) "${v.roundToInt()}" else String.format(Locale.UK, "%.1f", v)

private val LONDON = ZoneId.of("Europe/London")
private val HH_MM = DateTimeFormatter.ofPattern("HH:mm")
private val D_MMM = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
private val MMM_YY = DateTimeFormatter.ofPattern("MMM yy", Locale.UK)

private fun xLabels(series: GridTimeSeries): List<String> {
    val d = series.dates
    if (d.size < 2) return emptyList()
    return listOf(0, d.size / 2, d.size - 1).map { formatDate(d[it], series.granularity) }
}

private fun formatDate(value: String, granularity: ChartGranularity): String = runCatching {
    when (granularity) {
        ChartGranularity.HalfHour, ChartGranularity.Hour ->
            OffsetDateTime.parse(value).atZoneSameInstant(LONDON).format(HH_MM)
        ChartGranularity.Day -> LocalDate.parse(value).format(D_MMM)
        ChartGranularity.Month -> YearMonth.parse(value).format(MMM_YY)
    }
}.getOrDefault(value)
