package id.shoko.arta.domain.valuation

import id.shoko.arta.core.money.Dec
import id.shoko.arta.domain.model.Txn
import id.shoko.arta.domain.model.TxnType
import java.math.BigDecimal

/**
 * Metode average cost.
 * - Kuantitas transaksi dalam satuan posisi (lot untuk saham BEI); harga per unit instrumen (per lembar/gram/koin).
 * - BUY menambah kuantitas & biaya (qty × unitsPerLot × harga + fee).
 * - SELL mengurangi kuantitas dengan biaya rata-rata saat itu; hasil jual − fee − biaya keluar = realized P/L.
 */
data class CostResult(
    val hasBuy: Boolean,
    /** Biaya rata-rata per satuan posisi (mis. per lot), termasuk fee. */
    val averageCostPerPositionUnit: BigDecimal?,
    /** Biaya rata-rata per unit instrumen (mis. per lembar) — untuk ditampilkan "avg 8.400". */
    val averageCostPerInstrumentUnit: BigDecimal?,
    val heldQuantity: BigDecimal,
    val realizedPl: BigDecimal,
    val currency: String?,
)

object AverageCost {
    fun compute(txns: List<Txn>, unitsPerLot: BigDecimal = BigDecimal.ONE): CostResult {
        var qty = BigDecimal.ZERO
        var cost = BigDecimal.ZERO
        var realized = BigDecimal.ZERO
        var hasBuy = false
        var currency: String? = null
        for (t in txns.sortedBy { it.executedAt }) {
            when (t.type) {
                TxnType.BUY -> {
                    val price = t.price ?: continue
                    hasBuy = true
                    if (currency == null) currency = t.currency
                    cost = cost.add(t.quantity.multiply(unitsPerLot, Dec.MC).multiply(price, Dec.MC))
                        .add(t.fee ?: BigDecimal.ZERO)
                    qty = qty.add(t.quantity)
                }
                TxnType.SELL -> {
                    if (qty.signum() <= 0) continue
                    val sellQty = t.quantity.min(qty)
                    val avg = cost.divide(qty, Dec.MC)
                    val costOut = avg.multiply(sellQty, Dec.MC)
                    if (t.price != null) {
                        val proceeds = sellQty.multiply(unitsPerLot, Dec.MC).multiply(t.price, Dec.MC)
                            .subtract(t.fee ?: BigDecimal.ZERO)
                        realized = realized.add(proceeds.subtract(costOut))
                    }
                    cost = cost.subtract(costOut)
                    qty = qty.subtract(sellQty)
                }
                else -> Unit
            }
        }
        val avgPos = if (hasBuy && qty.signum() > 0) cost.divide(qty, Dec.MC) else null
        val avgInst = avgPos?.divide(unitsPerLot, Dec.MC)
        return CostResult(hasBuy, avgPos, avgInst, qty, realized, currency)
    }
}
