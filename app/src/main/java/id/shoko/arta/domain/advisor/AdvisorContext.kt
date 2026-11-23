package id.shoko.arta.domain.advisor

import id.shoko.arta.domain.analytics.Allocation
import id.shoko.arta.domain.analytics.AllocationItem
import id.shoko.arta.domain.analytics.Concentration
import id.shoko.arta.domain.analytics.ConcentrationResult
import id.shoko.arta.domain.analytics.SimulationEngine
import id.shoko.arta.domain.analytics.UsdExposure
import id.shoko.arta.domain.analytics.UsdExposureResult
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.valuation.PortfolioValuation
import java.math.BigDecimal

/** Semua angka yang dibutuhkan Advisor, dihitung lokal. */
class AdvisorContext(
    val valuation: PortfolioValuation,
    val concentrationLimitPct: BigDecimal,
    val notifyMovePct: BigDecimal,
    val targets: Map<AssetClass, BigDecimal>,
    val classLabel: (AssetClass) -> String,
    val simulationEngine: SimulationEngine = SimulationEngine(),
    val asOfIso: String,
) {
    val concentration: ConcentrationResult by lazy { Concentration.analyze(valuation, concentrationLimitPct) }
    val allocation: List<AllocationItem> by lazy { Allocation.analyze(valuation, targets) }
    val usd: UsdExposureResult by lazy { UsdExposure.analyze(valuation) }
    val isEmpty: Boolean get() = valuation.positions.isEmpty() || valuation.total.signum() == 0
}
