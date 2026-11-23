package id.shoko.arta.domain

import id.shoko.arta.TestPortfolio
import id.shoko.arta.TestPortfolio.bd
import id.shoko.arta.domain.analytics.Allocation
import id.shoko.arta.domain.analytics.Concentration
import id.shoko.arta.domain.analytics.Insight
import id.shoko.arta.domain.analytics.InsightEngine
import id.shoko.arta.domain.analytics.Scenario
import id.shoko.arta.domain.analytics.SimulationEngine
import id.shoko.arta.domain.analytics.UsdExposure
import id.shoko.arta.domain.model.AssetClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class AnalyticsTest {
    private val v = TestPortfolio.valuation()
    private fun r(x: BigDecimal, s: Int = 1) = x.setScale(s, RoundingMode.HALF_UP)

    @Test fun `konsentrasi Stockbit lewat batas 25 persen`() {
        val c = Concentration.analyze(v, bd("25"))
        val largest = c.largest!!
        assertEquals("Stockbit", largest.location.name)
        assertEquals(bd("32.0"), r(largest.sharePct))
        assertTrue(largest.overLimit)
        assertEquals(0, bd("89919200").compareTo(largest.excessBase))
        assertEquals(1, c.overLimit.size)
    }

    @Test fun `konsentrasi tidak lewat bila batas 40`() {
        assertTrue(Concentration.analyze(v, bd("40")).overLimit.isEmpty())
    }

    @Test fun `alokasi vs target`() {
        val items = Allocation.analyze(v, TestPortfolio.targets).associateBy { it.assetClass }
        assertEquals(bd("3.0"), r(items.getValue(AssetClass.CRYPTO).diffPct!!))
        // 1.284.560.000 × (18,0002 − 15)% ≈ Rp 38,5 jt
        assertEquals(bd("38.5"), items.getValue(AssetClass.CRYPTO).diffBase!!.divide(bd("1000000")).setScale(1, RoundingMode.HALF_UP))
        // Kas RDN Stockbit (Rp 47 jt) termasuk Tabungan: 252,5 jt = 19,7% → −0,3 poin; saham 28,3% → −1,7 poin.
        assertEquals(bd("-0.3"), r(items.getValue(AssetClass.CASH).diffPct!!))
        assertEquals(bd("-1.7"), r(items.getValue(AssetClass.STOCK_IDX).diffPct!!))
        assertEquals(bd("-3.0"), r(items.getValue(AssetClass.ETF).diffPct!!))
        assertEquals(bd("0.0"), r(items.getValue(AssetClass.MUTUAL_FUND).diffPct!!))
    }

    @Test fun `validasi target total 100`() {
        assertTrue(Allocation.isValidTarget(TestPortfolio.targets))
        assertFalse(Allocation.isValidTarget(TestPortfolio.targets + (AssetClass.OTHER to bd("1"))))
    }

    @Test fun `eksposur USD 47 persen`() {
        val u = UsdExposure.analyze(v)
        assertEquals(bd("47"), u.sharePct.setScale(0, RoundingMode.HALF_UP))
        assertEquals(setOf(AssetClass.CRYPTO, AssetClass.GOLD, AssetClass.ETF), u.byClass.keys)
        // ±5% kurs → ±Rp 30,2 jt
        assertEquals(bd("30.2"), u.impactOf(BigDecimal(5)).divide(bd("1000000")).setScale(1, RoundingMode.HALF_UP))
    }

    @Test fun `simulasi kripto turun 30 persen menurunkan total 5,4 persen`() {
        val res = SimulationEngine().run(v, Scenario(cryptoPct = -30))
        assertEquals(bd("-5.4"), r(res.diffPct))
        assertEquals(0, bd("-69366240").compareTo(res.diff))
        assertEquals(bd("13"), res.shareAfter(AssetClass.CRYPTO).setScale(0, RoundingMode.HALF_UP))
        val cash = res.byClass.first { it.assetClass == AssetClass.CASH }
        assertEquals(0, cash.diff.signum())
    }

    @Test fun `simulasi nol tidak mengubah apa pun`() {
        val res = SimulationEngine().run(v, Scenario())
        assertEquals(0, res.diff.signum())
    }

    @Test fun `simulasi rupiah melemah menaikkan aset USD`() {
        val res = SimulationEngine().run(v, Scenario(usdIdrPct = 10))
        val etf = res.byClass.first { it.assetClass == AssetClass.ETF }
        assertEquals(0, etf.after.compareTo(bd("89919200").multiply(bd("1.1"))))
        // Saham tidak terpengaruh kurs.
        assertEquals(0, res.byClass.first { it.assetClass == AssetClass.STOCK_IDX }.diff.signum())
    }

    @Test fun `reksa dana mengikuti setengah IHSG`() {
        val res = SimulationEngine().run(v, Scenario(ihsgPct = -10))
        val rd = res.byClass.first { it.assetClass == AssetClass.MUTUAL_FUND }
        assertEquals(0, rd.diff.compareTo(bd("-3211400"))) // 64.228.000 × −5%
    }

    @Test fun `preset pasar turun`() {
        val res = SimulationEngine().run(v, Scenario.MARKET_DOWN)
        assertTrue(res.diff.signum() < 0)
    }

    @Test fun `insight prioritas konsentrasi`() {
        val i = InsightEngine.pick(v, bd("25"), bd("3"), TestPortfolio.targets, { it.name })
        assertTrue(i is Insight.ConcentrationOver)
        assertEquals("Porsi Stockbit 32%, di atas batas 25% yang kamu atur.", i.text)
    }

    @Test fun `insight alokasi bila tidak ada konsentrasi`() {
        val targets = TestPortfolio.targets + (AssetClass.CRYPTO to bd("5")) + (AssetClass.CASH to bd("30"))
        val i = InsightEngine.pick(v, bd("40"), bd("3"), targets, { "Kripto" })
        assertTrue(i.toString(), i is Insight.AllocationDrift)
    }
}
