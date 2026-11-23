package id.shoko.arta.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Format angka gaya Indonesia (titik ribuan, koma desimal, tanda minus "−").
 * Pembulatan hanya dilakukan di sini, saat tampil.
 */
object MoneyFormat {
    const val MINUS = "−"
    const val HIDDEN = "Rp ••••"

    private val symbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private fun pattern(decimals: Int, minDecimals: Int = decimals): DecimalFormat {
        val p = if (decimals == 0) "#,##0" else "#,##0." + "0".repeat(minDecimals) + "#".repeat(decimals - minDecimals)
        return DecimalFormat(p, symbols).apply { roundingMode = RoundingMode.HALF_UP }
    }

    fun currencyPrefix(code: String): String = when (code) {
        "IDR" -> "Rp "
        "USD" -> "$"
        else -> "$code "
    }

    /** "1.284.560.000" */
    fun number(v: BigDecimal, decimals: Int = 0, minDecimals: Int = decimals): String =
        pattern(decimals, minDecimals).format(v.abs()).let { if (v.signum() < 0 && it.any { c -> c in '1'..'9' }) MINUS + it else it }

    /** "Rp 1.284.560.000" */
    fun full(v: BigDecimal, currency: String = "IDR"): String {
        val decimals = if (currency == "IDR") 0 else 2
        val sign = if (v.signum() < 0) MINUS else ""
        return sign + currencyPrefix(currency) + pattern(decimals).format(v.abs())
    }

    /** "+Rp 6.294.300" / "−Rp 1.200" */
    fun signedFull(v: BigDecimal, currency: String = "IDR"): String {
        val sign = when (v.signum()) { 1 -> "+"; -1 -> MINUS; else -> "" }
        val decimals = if (currency == "IDR") 0 else 2
        return sign + currencyPrefix(currency) + pattern(decimals).format(v.abs())
    }

    /** Ringkas: "Rp 411,1 jt", "Rp 1,28 M", "Rp 950 rb". */
    fun compact(v: BigDecimal, currency: String = "IDR", signed: Boolean = false): String {
        val abs = v.abs()
        val sign = when {
            v.signum() < 0 -> MINUS
            signed && v.signum() > 0 -> "+"
            else -> ""
        }
        val prefix = currencyPrefix(currency)
        if (currency != "IDR") {
            val body = when {
                abs >= BigDecimal("1000000") -> pattern(2, 0).format(abs.divide(BigDecimal("1000000"), Dec.MC)) + " jt"
                else -> pattern(2).format(abs)
            }
            return sign + prefix + body
        }
        val t = BigDecimal("1000000000000")
        val m = BigDecimal("1000000000")
        val jt = BigDecimal("1000000")
        val rb = BigDecimal("1000")
        val body = when {
            abs >= t -> pattern(2, 0).format(abs.divide(t, Dec.MC)) + " T"
            abs >= m -> pattern(2).format(abs.divide(m, Dec.MC)) + " M"
            abs >= jt -> pattern(1).format(abs.divide(jt, Dec.MC)) + " jt"
            abs >= rb -> pattern(0).format(abs.divide(rb, Dec.MC)) + " rb"
            else -> pattern(0).format(abs)
        }
        return sign + prefix + body
    }

    /** "+1,5%" / "−1,4%" / "0,0%" */
    fun signedPct(p: BigDecimal, decimals: Int = 1): String {
        val r = p.setScale(decimals, RoundingMode.HALF_UP)
        val sign = when (r.signum()) { 1 -> "+"; -1 -> MINUS; else -> "" }
        return sign + pattern(decimals).format(r.abs()) + "%"
    }

    /** "32%" / "0,49%" */
    fun pct(p: BigDecimal, decimals: Int = 0): String {
        val r = p.setScale(decimals, RoundingMode.HALF_UP)
        val sign = if (r.signum() < 0) MINUS else ""
        return sign + pattern(decimals).format(r.abs()) + "%"
    }

    /** Kuantitas tanpa nol di belakang: "0,05", "150", "25,5". */
    fun quantity(q: BigDecimal, maxDecimals: Int = 8): String {
        val s = q.setScale(maxDecimals, RoundingMode.HALF_UP).stripTrailingZeros()
        val d = s.scale().coerceAtLeast(0)
        return number(s, d, 0)
    }

    /** Harga satuan: IDR tanpa desimal bila >= 1000, lain 2–8 desimal. */
    fun price(p: BigDecimal, currency: String): String {
        val decimals = when {
            currency == "IDR" && p.abs() >= BigDecimal(1000) -> 0
            currency == "IDR" -> 2
            p.abs() >= BigDecimal.ONE -> 2
            else -> 6
        }
        return currencyPrefix(currency) + pattern(decimals, if (decimals > 2) 2 else decimals).format(p)
    }
}
