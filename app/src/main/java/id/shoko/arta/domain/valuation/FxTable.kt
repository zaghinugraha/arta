package id.shoko.arta.domain.valuation

import id.shoko.arta.core.money.Dec
import id.shoko.arta.domain.model.FxRate
import java.math.BigDecimal

/**
 * Tabel kurs yang bisa mengonversi langsung, terbalik, atau silang lewat USD.
 */
class FxTable(rates: Collection<FxRate>) {
    private val map: Map<Pair<String, String>, BigDecimal> =
        rates.associate { (it.base to it.quote) to it.rate }

    fun rate(from: String, to: String): BigDecimal? {
        if (from == to) return BigDecimal.ONE
        map[from to to]?.let { return it }
        map[to to from]?.let { if (it.signum() != 0) return BigDecimal.ONE.divide(it, Dec.MC) }
        // Silang lewat USD
        if (from != "USD" && to != "USD") {
            val a = rate(from, "USD") ?: return null
            val b = rate("USD", to) ?: return null
            return a.multiply(b, Dec.MC)
        }
        return null
    }

    fun convert(amount: BigDecimal, from: String, to: String): BigDecimal? =
        rate(from, to)?.let { amount.multiply(it, Dec.MC) }

    companion object {
        val EMPTY = FxTable(emptyList())
    }
}
