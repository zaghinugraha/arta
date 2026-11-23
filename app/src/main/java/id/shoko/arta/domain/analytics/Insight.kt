package id.shoko.arta.domain.analytics

import id.shoko.arta.core.money.MoneyFormat
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.valuation.PortfolioValuation
import java.math.BigDecimal

sealed interface Insight {
    val text: String

    data class ConcentrationOver(val locationName: String, val sharePct: BigDecimal, val limitPct: BigDecimal) : Insight {
        override val text = "Porsi $locationName ${MoneyFormat.pct(sharePct)}, di atas batas ${MoneyFormat.pct(limitPct)} yang kamu atur."
    }
    data class BigMove(val changePct: BigDecimal) : Insight {
        override val text = if (changePct.signum() >= 0)
            "Kekayaanmu naik ${MoneyFormat.pct(changePct, 1)} hari ini, lebih besar dari ambang notifikasimu."
        else "Kekayaanmu turun ${MoneyFormat.pct(changePct.abs(), 1)} hari ini, lebih besar dari ambang notifikasimu."
    }
    data class AllocationDrift(val assetClass: AssetClass, val label: String, val diffPct: BigDecimal) : Insight {
        override val text = if (diffPct.signum() > 0)
            "Porsi $label ${MoneyFormat.pct(diffPct.abs())} poin di atas target alokasimu."
        else "Porsi $label ${MoneyFormat.pct(diffPct.abs())} poin di bawah target alokasimu."
    }
    data class StalePrices(val count: Int) : Insight {
        override val text = "$count harga belum diperbarui. Nilai memakai harga terakhir yang tersimpan."
    }
    data object AllGood : Insight {
        override val text = "Semua lokasi di bawah batas konsentrasi dan alokasi dekat dengan target."
    }
    data object Empty : Insight {
        override val text = "Tambah posisi pertamamu untuk melihat insight harian."
    }
}

/**
 * Prioritas: (1) konsentrasi lewat batas, (2) gerak harian > ambang,
 * (3) alokasi menyimpang > 5 poin dari target, (4) harga basi.
 */
object InsightEngine {
    val DRIFT_THRESHOLD: BigDecimal = BigDecimal(5)

    fun pick(
        p: PortfolioValuation,
        concentrationLimitPct: BigDecimal,
        notifyMovePct: BigDecimal,
        targets: Map<AssetClass, BigDecimal>,
        classLabel: (AssetClass) -> String,
        markStale: Boolean = true,
    ): Insight {
        if (p.positions.isEmpty() || p.total.signum() == 0) return Insight.Empty
        Concentration.analyze(p, concentrationLimitPct).overLimit.firstOrNull()?.let {
            return Insight.ConcentrationOver(it.location.name, it.sharePct, concentrationLimitPct)
        }
        val move = p.changeTodayPct
        if (move.abs() > notifyMovePct) return Insight.BigMove(move)
        Allocation.analyze(p, targets)
            .filter { it.diffPct != null && it.diffPct.abs() > DRIFT_THRESHOLD }
            .maxByOrNull { it.diffPct!!.abs() }
            ?.let { return Insight.AllocationDrift(it.assetClass, id.shoko.arta.domain.advisor.TemplateResponder.lower(classLabel(it.assetClass)), it.diffPct!!) }
        if (markStale && p.staleCount > 0) return Insight.StalePrices(p.staleCount)
        return Insight.AllGood
    }
}
