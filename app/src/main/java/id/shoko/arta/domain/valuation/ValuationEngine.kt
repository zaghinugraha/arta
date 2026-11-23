package id.shoko.arta.domain.valuation

import id.shoko.arta.core.money.Dec
import id.shoko.arta.core.money.percentOf
import id.shoko.arta.core.money.sumOfDec
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.Instrument
import id.shoko.arta.domain.model.Location
import id.shoko.arta.domain.model.Position
import id.shoko.arta.domain.model.PriceInfo
import id.shoko.arta.domain.model.PriceSource
import id.shoko.arta.domain.model.Txn
import java.math.BigDecimal

data class PositionValuation(
    val position: Position,
    val instrument: Instrument,
    val location: Location,
    val price: PriceInfo?,
    /** Harga per unit instrumen (per lembar/gram/koin) di mata uang kuotasi. */
    val unitPrice: BigDecimal?,
    val valueBase: BigDecimal,
    val changeTodayBase: BigDecimal,
    val costBasisBase: BigDecimal?,
    val unrealizedPlBase: BigDecimal?,
    val hasHistory: Boolean,
    val averageCost: BigDecimal?,
    /** true bila nilai tidak bisa dihitung (harga/kurs belum ada). */
    val missingPrice: Boolean,
    val isStale: Boolean,
) {
    val changeTodayPct: BigDecimal get() {
        val prev = valueBase.subtract(changeTodayBase)
        return percentOf(changeTodayBase, prev)
    }
    val unrealizedPlPct: BigDecimal? get() =
        if (unrealizedPlBase == null || costBasisBase == null) null else percentOf(unrealizedPlBase, costBasisBase)
}

data class GroupValuation<K>(
    val key: K,
    val valueBase: BigDecimal,
    val changeTodayBase: BigDecimal,
    val sharePct: BigDecimal,
    val positions: List<PositionValuation>,
) {
    val changeTodayPct: BigDecimal get() = percentOf(changeTodayBase, valueBase.subtract(changeTodayBase))
}

data class PortfolioValuation(
    val baseCurrency: String,
    val positions: List<PositionValuation>,
    val total: BigDecimal,
    val changeToday: BigDecimal,
    val byClass: List<GroupValuation<AssetClass>>,
    val byLocation: List<GroupValuation<Location>>,
    val lastPriceAt: Long?,
    val staleCount: Int,
) {
    val changeTodayPct: BigDecimal get() = percentOf(changeToday, total.subtract(changeToday))

    fun classValue(c: AssetClass): BigDecimal = byClass.firstOrNull { it.key == c }?.valueBase ?: BigDecimal.ZERO

    companion object {
        fun empty(base: String) = PortfolioValuation(base, emptyList(), BigDecimal.ZERO, BigDecimal.ZERO, emptyList(), emptyList(), null, 0)
    }
}

/**
 * nilai_posisi_base = quantity × units_per_lot × price × fx(quote_currency → base)
 * CASH: nilai = quantity × fx(currency → base)
 */
object ValuationEngine {

    fun unitsOf(position: Position, instrument: Instrument): BigDecimal =
        position.quantity.multiply(instrument.unitsPerLot, Dec.MC)

    fun valuePosition(
        position: Position,
        instrument: Instrument,
        location: Location,
        price: PriceInfo?,
        txns: List<Txn>,
        fx: FxTable,
        base: String,
        staleAfterMillis: Long?,
        nowMillis: Long,
    ): PositionValuation {
        val units = unitsOf(position, instrument)
        val isCash = instrument.assetClass == AssetClass.CASH || instrument.priceSource == PriceSource.NONE

        val unitPrice: BigDecimal? = if (isCash) BigDecimal.ONE else price?.price
        val priceCurrency = if (isCash) instrument.quoteCurrency else (price?.currency ?: instrument.quoteCurrency)
        val rate = fx.rate(priceCurrency, base)

        val missing = unitPrice == null || rate == null
        val valueBase = if (missing) BigDecimal.ZERO else units.multiply(unitPrice!!, Dec.MC).multiply(rate!!, Dec.MC)

        val changeToday = if (!missing && !isCash && price?.previousClose != null && instrument.priceSource != PriceSource.MANUAL) {
            units.multiply(price.price.subtract(price.previousClose), Dec.MC).multiply(rate!!, Dec.MC)
        } else BigDecimal.ZERO

        val cost = AverageCost.compute(txns, instrument.unitsPerLot)
        val hasHistory = cost.hasBuy
        val costBasisBase: BigDecimal?
        val pl: BigDecimal?
        if (hasHistory && !missing && !isCash && cost.averageCostPerPositionUnit != null) {
            val costRate = fx.rate(cost.currency ?: priceCurrency, base)
            if (costRate != null) {
                // Biaya pokok untuk kuantitas yang dimiliki saat ini (position.quantity = sumber kebenaran).
                costBasisBase = position.quantity.multiply(cost.averageCostPerPositionUnit, Dec.MC).multiply(costRate, Dec.MC)
                pl = valueBase.subtract(costBasisBase)
            } else {
                costBasisBase = null; pl = null
            }
        } else {
            costBasisBase = null; pl = null
        }

        val stale = if (isCash || price == null || staleAfterMillis == null) false
        else nowMillis - price.fetchedAt > staleAfterMillis || nowMillis - price.asOf > staleAfterMillis.coerceAtLeast(MAX_SOURCE_AGE)

        return PositionValuation(
            position = position,
            instrument = instrument,
            location = location,
            price = price,
            unitPrice = unitPrice,
            valueBase = valueBase,
            changeTodayBase = changeToday,
            costBasisBase = costBasisBase,
            unrealizedPlBase = pl,
            hasHistory = hasHistory,
            averageCost = cost.averageCostPerInstrumentUnit,
            missingPrice = missing,
            isStale = stale,
        )
    }

    /** Sumber harian (kurs ECB, harga manual) dianggap basi bila > ~1 hari kerja (3 hari kalender, menutup akhir pekan). */
    private const val MAX_SOURCE_AGE = 3L * 24 * 60 * 60 * 1000

    fun aggregate(base: String, positions: List<PositionValuation>): PortfolioValuation {
        val total = positions.sumOfDec { it.valueBase }
        val change = positions.sumOfDec { it.changeTodayBase }
        val byClass = positions.groupBy { it.instrument.assetClass }
            .map { (k, list) -> group(k, list, total) }
            .sortedByDescending { it.valueBase }
        val byLocation = positions.groupBy { it.location.id }
            .map { (_, list) -> group(list.first().location, list, total) }
            .sortedByDescending { it.valueBase }
        val lastPrice = positions.mapNotNull { it.price?.fetchedAt }.maxOrNull()
        return PortfolioValuation(
            baseCurrency = base,
            positions = positions,
            total = total,
            changeToday = change,
            byClass = byClass,
            byLocation = byLocation,
            lastPriceAt = lastPrice,
            staleCount = positions.count { it.isStale },
        )
    }

    private fun <K> group(key: K, list: List<PositionValuation>, total: BigDecimal): GroupValuation<K> {
        val v = list.sumOfDec { it.valueBase }
        return GroupValuation(
            key = key,
            valueBase = v,
            changeTodayBase = list.sumOfDec { it.changeTodayBase },
            sharePct = percentOf(v, total),
            positions = list.sortedByDescending { it.valueBase },
        )
    }
}
