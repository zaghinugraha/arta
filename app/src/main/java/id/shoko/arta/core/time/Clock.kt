package id.shoko.arta.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Abstraksi waktu agar logika bisa diuji. */
interface Clock {
    fun now(): Instant
    fun zone(): ZoneId
    fun today(): LocalDate = LocalDate.ofInstant(now(), zone())
    fun nowMillis(): Long = now().toEpochMilli()
}

object SystemClock : Clock {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}

class FixedClock(private val instant: Instant, private val zoneId: ZoneId = ZoneId.of("Asia/Jakarta")) : Clock {
    override fun now(): Instant = instant
    override fun zone(): ZoneId = zoneId
}

val WIB: ZoneId = ZoneId.of("Asia/Jakarta")
