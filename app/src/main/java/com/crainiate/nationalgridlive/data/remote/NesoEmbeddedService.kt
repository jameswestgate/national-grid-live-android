package com.crainiate.nationalgridlive.data.remote

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One half-hourly embedded (distribution-connected) reading, in MW. */
data class EmbeddedReading(val instant: Instant, val windMw: Double, val solarMw: Double)

/**
 * Fetches NESO's rolling embedded solar/wind CSV (`demanddataupdate.csv`) and
 * parses it into [EmbeddedReading]s. Mirrors the iOS `NESOClient`:
 * `SETTLEMENT_PERIOD` is numbered in LOCAL UK time (Europe/London), so each row's
 * instant is `date 00:00 Europe/London + (period-1) * 30min`.
 *
 * The CSV also contains near-future forecast rows, so callers should pick the row
 * nearest "now" for the live view, or average rows within a window for day/week.
 */
class NesoEmbeddedService {

    private val zone = ZoneId.of("Europe/London")

    suspend fun fetchEmbedded(): List<EmbeddedReading> = parse(Http.get(Endpoints.NESO_EMBEDDED, "text/csv"))

    private fun parse(csv: String): List<EmbeddedReading> {
        val lines = csv.split('\n').filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val header = lines.first().split(',').map { unquote(it).uppercase() }
        val iDate = header.indexOf("SETTLEMENT_DATE")
        val iPeriod = header.indexOf("SETTLEMENT_PERIOD")
        val iWind = header.indexOf("EMBEDDED_WIND_GENERATION")
        val iSolar = header.indexOf("EMBEDDED_SOLAR_GENERATION")
        if (iDate < 0 || iPeriod < 0 || iWind < 0 || iSolar < 0) return emptyList()

        return lines.drop(1).mapNotNull { line ->
            val cols = line.split(',')
            if (cols.size <= maxOf(iDate, iPeriod, iWind, iSolar)) return@mapNotNull null
            runCatching {
                val date = LocalDate.parse(unquote(cols[iDate]))
                val period = unquote(cols[iPeriod]).toInt()
                val instant = date.atStartOfDay(zone)
                    .plusMinutes((period - 1) * 30L)
                    .toInstant()
                EmbeddedReading(
                    instant = instant,
                    windMw = unquote(cols[iWind]).toDoubleOrNull() ?: 0.0,
                    solarMw = unquote(cols[iSolar]).toDoubleOrNull() ?: 0.0
                )
            }.getOrNull()
        }
    }

    private fun unquote(s: String): String = s.trim().removeSurrounding("\"")

    companion object {
        /** Embedded reading nearest to (but not after) [now] — for the live view. */
        fun List<EmbeddedReading>.latestAt(now: Instant): EmbeddedReading? =
            filter { !it.instant.isAfter(now) }.maxByOrNull { it.instant }

        /** Mean embedded wind & solar (GW) over [from]..[to] — for day/week. */
        fun List<EmbeddedReading>.meanGwInWindow(from: Instant, to: Instant): Pair<Double, Double> {
            val rows = filter { !it.instant.isBefore(from) && !it.instant.isAfter(to) }
            if (rows.isEmpty()) return 0.0 to 0.0
            return rows.map { it.windMw }.average() / 1000.0 to rows.map { it.solarMw }.average() / 1000.0
        }
    }
}
