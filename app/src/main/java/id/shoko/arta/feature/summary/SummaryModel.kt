package id.shoko.arta.feature.summary

import id.shoko.arta.core.money.Dec
import id.shoko.arta.core.money.percentOf
import id.shoko.arta.data.repo.SnapshotPoint
import java.math.BigDecimal
import java.time.LocalDate

enum class ChartRange(val label: String, val days: Long?) {
    D1("1H", 1), M1("1B", 30), M3("3B", 90), Y1("1T", 365), ALL("Semua", null)
}

object SummaryModel {
    /** Deret grafik: snapshot dalam rentang + total saat ini sebagai titik terakhir (hari ini). */
    fun series(points: List<SnapshotPoint>, currentTotal: BigDecimal, today: LocalDate, range: ChartRange): List<BigDecimal> {
        val past = points.filter { it.date.isBefore(today) }
        val inRange = when (range.days) {
            null -> past
            else -> past.filter { !it.date.isBefore(today.minusDays(range.days)) }
        }
        val base = if (range == ChartRange.D1) past.takeLast(1) else inRange
        return base.map { it.total } + currentTotal
    }

    /** Perubahan sejak penutupan sebelum [start] (snapshot terakhir sebelum tanggal itu). */
    fun changeSince(points: List<SnapshotPoint>, currentTotal: BigDecimal, start: LocalDate): BigDecimal? {
        val ref = points.lastOrNull { it.date.isBefore(start) } ?: points.firstOrNull { !it.date.isBefore(start) } ?: return null
        if (ref.total.signum() == 0) return null
        return percentOf(currentTotal.subtract(ref.total), ref.total)
    }

    fun monthToDate(points: List<SnapshotPoint>, current: BigDecimal, today: LocalDate) = changeSince(points, current, today.withDayOfMonth(1))
    fun yearToDate(points: List<SnapshotPoint>, current: BigDecimal, today: LocalDate) = changeSince(points, current, today.withDayOfYear(1))

    fun toDoubles(series: List<BigDecimal>): List<Double> {
        if (series.isEmpty()) return emptyList()
        val min = series.min()
        // Kurangi minimum dulu (BigDecimal) agar presisi Double tidak hilang pada angka miliaran.
        return series.map { it.subtract(min).divide(BigDecimal.ONE, Dec.MC).toDouble() }
    }
}
