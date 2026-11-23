package id.shoko.arta.domain.analytics

import id.shoko.arta.core.money.Dec
import id.shoko.arta.core.money.percentOf
import id.shoko.arta.core.money.sumOfDec
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.Location
import id.shoko.arta.domain.valuation.PortfolioValuation
import id.shoko.arta.domain.valuation.PositionValuation
import java.math.BigDecimal

// ---------- Konsentrasi ----------

data class ConcentrationItem(
    val location: Location,
    val valueBase: BigDecimal,
    val sharePct: BigDecimal,
    val overLimit: Boolean,
    /** Kelebihan nominal = nilai_lokasi − batas × total (0 bila tidak lewat). */
    val excessBase: BigDecimal,
)

data class ConcentrationResult(
    val limitPct: BigDecimal,
    val items: List<ConcentrationItem>,
) {
    val largest: ConcentrationItem? get() = items.maxByOrNull { it.sharePct }
    val overLimit: List<ConcentrationItem> get() = items.filter { it.overLimit }
}

object Concentration {
    fun analyze(p: PortfolioValuation, limitPct: BigDecimal): ConcentrationResult {
        val limitFrac = limitPct.divide(Dec.HUNDRED, Dec.MC)
        val items = p.byLocation.map { g ->
            val over = p.total.signum() > 0 && g.sharePct > limitPct
            ConcentrationItem(
                location = g.key,
                valueBase = g.valueBase,
                sharePct = g.sharePct,
                overLimit = over,
                excessBase = if (over) g.valueBase.subtract(p.total.multiply(limitFrac, Dec.MC)) else BigDecimal.ZERO,
            )
        }.sortedByDescending { it.sharePct }
        return ConcentrationResult(limitPct, items)
    }
}

// ---------- Alokasi vs target ----------

data class AllocationItem(
    val assetClass: AssetClass,
    val actualPct: BigDecimal,
    val targetPct: BigDecimal?,
    /** actual − target, poin persen. */
    val diffPct: BigDecimal?,
    /** Selisih nominal (positif = lebih dari target). */
    val diffBase: BigDecimal?,
    val valueBase: BigDecimal,
)

object Allocation {
    fun analyze(p: PortfolioValuation, targets: Map<AssetClass, BigDecimal>): List<AllocationItem> {
        val classes = (p.byClass.map { it.key } + targets.keys).distinct()
        return classes.map { c ->
            val value = p.classValue(c)
            val actual = percentOf(value, p.total)
            val target = targets[c]
            val diff = target?.let { actual.subtract(it) }
            AllocationItem(
                assetClass = c,
                actualPct = actual,
                targetPct = target,
                diffPct = diff,
                diffBase = diff?.let { p.total.multiply(it, Dec.MC).divide(Dec.HUNDRED, Dec.MC) },
                valueBase = value,
            )
        }.sortedWith(compareBy { AssetClass.displayOrder.indexOf(it.assetClass) })
    }

    fun isValidTarget(targets: Map<AssetClass, BigDecimal>): Boolean =
        targets.values.sumOfDec { it }.compareTo(Dec.HUNDRED) == 0 && targets.values.all { it.signum() >= 0 }
}

// ---------- Eksposur USD ----------

data class UsdExposureResult(
    val usdLinkedBase: BigDecimal,
    val sharePct: BigDecimal,
    val byClass: Map<AssetClass, BigDecimal>,
) {
    /** Dampak nominal bila rupiah melemah/menguat [movePct] persen. */
    fun impactOf(movePct: BigDecimal): BigDecimal = usdLinkedBase.multiply(movePct, Dec.MC).divide(Dec.HUNDRED, Dec.MC)
}

object UsdExposure {
    /**
     * Posisi yang nilainya ikut kurs USD: kripto, emas (harga dunia dalam USD), dan
     * instrumen/kas yang dikuotasi USD (mis. ETF AS, tabungan valas USD).
     */
    fun isUsdLinked(pv: PositionValuation): Boolean {
        val c = pv.instrument.assetClass
        return c == AssetClass.CRYPTO || c == AssetClass.GOLD || pv.instrument.quoteCurrency == "USD"
    }

    fun analyze(p: PortfolioValuation): UsdExposureResult {
        if (p.baseCurrency == "USD") return UsdExposureResult(BigDecimal.ZERO, BigDecimal.ZERO, emptyMap())
        val linked = p.positions.filter(::isUsdLinked)
        val sum = linked.sumOfDec { it.valueBase }
        return UsdExposureResult(
            usdLinkedBase = sum,
            sharePct = percentOf(sum, p.total),
            byClass = linked.groupBy { it.instrument.assetClass }.mapValues { (_, l) -> l.sumOfDec { it.valueBase } },
        )
    }
}
