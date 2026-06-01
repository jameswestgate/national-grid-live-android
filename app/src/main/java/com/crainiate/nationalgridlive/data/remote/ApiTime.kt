package com.crainiate.nationalgridlive.data.remote

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * ISO-8601 time helpers for the live cache. Keys are produced by [iso] in a fixed
 * UTC `yyyy-MM-dd'T'HH:mm:ss'Z'` format so they sort lexically == chronologically
 * (the cache relies on string `max()`/`>=` comparisons). [parse] is tolerant of
 * the various shapes the APIs return (seconds optional, e.g. `...T14:00Z`).
 */
object ApiTime {
    private val fmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    /** Minute-precision ISO for the Carbon Intensity range endpoint. */
    val minuteFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'").withZone(ZoneOffset.UTC)

    fun iso(instant: Instant): String = fmt.format(instant)

    fun parse(s: String): Instant? = runCatching {
        OffsetDateTime.parse(s).toInstant()
    }.recoverCatching { Instant.parse(s) }.getOrNull()

    /** Floor [instant] to the start of its [intervalSeconds] bucket. */
    fun bucket(instant: Instant, intervalSeconds: Long): Instant {
        val s = instant.epochSecond
        return Instant.ofEpochSecond(Math.floorDiv(s, intervalSeconds) * intervalSeconds)
    }

    /** Convenience: ISO key for the bucket containing [instant]. */
    fun bucketKey(instant: Instant, intervalSeconds: Long): String = iso(bucket(instant, intervalSeconds))

    const val FIVE_MIN = 5L * 60
    const val HALF_HOUR = 30L * 60
}
