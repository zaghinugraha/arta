package id.shoko.arta.domain.analytics

import id.shoko.arta.core.money.Dec
import id.shoko.arta.core.money.percentOf
import id.shoko.arta.core.money.sumOfDec
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.valuation.PortfolioValuation
import id.shoko.arta.domain.valuation.PositionValuation
import java.math.BigDecimal

/** Skenario dalam persen (−30 = turun 30%). */
data class Scenario(
    val cryptoPct: Int = 0,
    val ihsgPct: Int = 0,
    val goldPct: Int = 0,
    val usdIdrPct: Int = 0,
) {
    val isZero: Boolean get() = cryptoPct == 0 && ihsgPct == 0 && goldPct == 0 && usdIdrPct == 0

    companion object {
        val MARKET_DOWN = Scenario(cryptoPct = -30, ihsgPct = -10, goldPct = 5, usdIdrPct = 5)
        val RUPIAH_WEAK = Scenario(ihsgPct = -5, usdIdrPct = 10)
        val RESET = Scenario()

        const val CRYPTO_MIN = -50; const val CRYPTO_MAX = 50
        const val IHSG_MIN = -30; const val IHSG_MAX = 30
        const val GOLD_MIN = -20; const val GOLD_MAX = 20
        const val USD_MIN = -10; const val USD_MAX = 20
        const val STEP = 5
    }
}

data class ClassImpact(
    val assetClass: AssetClass,
    val before: BigDecimal,
    val after: BigDecimal,
) {
    val diff: BigDecimal get() = after.subtract(before)
}

data class SimulationResult(
    val scenario: Scenario,
    val totalBefore: BigDecimal,
    val totalAfter: BigDecimal,
    val byClass: List<ClassImpact>,
) {
    val diff: BigDecimal get() = totalAfter.subtract(totalBefore)
    val diffPct: BigDecimal get() = percentOf(diff, totalBefore)
    fun shareBefore(c: AssetClass): BigDecimal = percentOf(byClass.firstOrNull { it.assetClass == c }?.before ?: BigDecimal.ZERO, totalBefore)
    fun shareAfter(c: AssetClass): BigDecimal = percentOf(byClass.firstOrNull { it.assetClass == c }?.after ?: BigDecimal.ZERO, totalAfter)
}

/**
 * Model (ARCHITECTURE §6.4):
 *   saham'    = saham × (1 + i)
 *   rd'       = rd × (1 + β × i)              β = [mutualFundIhsgBeta], default 0,5
 *   kripto'   = kripto × (1 + k) × (1 + u)
 *   emas'     = emas × (1 + e) × (1 + u)
 *   etf'      = etf × (1 + u)                 (ETF berkuotasi IDR diperlakukan seperti saham: × (1 + i))
 *   tabungan' = tabungan                      (bagian valas USD: × (1 + u))
 *   lainnya'  = tetap                         (bagian USD: × (1 + u))
 */
class SimulationEngine(private val mutualFundIhsgBeta: BigDecimal = DEFAULT_MF_BETA) {

    fun run(p: PortfolioValuation, s: Scenario): SimulationResult {
        val k = frac(s.cryptoPct); val i = frac(s.ihsgPct); val e = frac(s.goldPct); val u = frac(s.usdIdrPct)
        val one = BigDecimal.ONE
        val usdFactor = if (p.baseCurrency == "USD") one else one.add(u)
        val after = p.positions.map { pv -> pv to factorFor(pv, k, i, e, usdFactor).multiply(pv.valueBase, Dec.MC) }
        val classes = p.positions.map { it.instrument.assetClass }.distinct()
            .sortedBy { AssetClass.displayOrder.indexOf(it) }
        val byClass = classes.map { c ->
            ClassImpact(
                assetClass = c,
                before = p.positions.filter { it.instrument.assetClass == c }.sumOfDec { it.valueBase },
                after = after.filter { it.first.instrument.assetClass == c }.sumOfDec { it.second },
            )
        }
        return SimulationResult(s, p.total, after.sumOfDec { it.second }, byClass)
    }

    private fun factorFor(pv: PositionValuation, k: BigDecimal, i: BigDecimal, e: BigDecimal, usd: BigDecimal): BigDecimal {
        val one = BigDecimal.ONE
        val isUsdQuoted = pv.instrument.quoteCurrency == "USD"
        return when (pv.instrument.assetClass) {
            AssetClass.STOCK_IDX -> one.add(i)
            AssetClass.MUTUAL_FUND -> one.add(mutualFundIhsgBeta.multiply(i, Dec.MC))
            AssetClass.CRYPTO -> one.add(k).multiply(usd, Dec.MC)
            AssetClass.GOLD -> one.add(e).multiply(usd, Dec.MC)
            AssetClass.ETF -> if (isUsdQuoted || pv.instrument.quoteCurrency != "IDR") usd else one.add(i)
            AssetClass.CASH, AssetClass.OTHER -> if (isUsdQuoted) usd else one
        }
    }

    private fun frac(pct: Int): BigDecimal = BigDecimal(pct).divide(Dec.HUNDRED, Dec.MC)

    companion object {
        val DEFAULT_MF_BETA: BigDecimal = BigDecimal("0.5")
    }
}
