package id.shoko.arta.domain

import id.shoko.arta.TestPortfolio.bd
import id.shoko.arta.domain.model.FxRate
import id.shoko.arta.domain.model.Txn
import id.shoko.arta.domain.model.TxnType
import id.shoko.arta.domain.valuation.AverageCost
import id.shoko.arta.domain.valuation.FxTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class FxAndCostTest {
    private fun eq(expected: String, actual: BigDecimal?, scale: Int = 6) =
        assertEquals(bd(expected).setScale(scale, RoundingMode.HALF_UP), actual!!.setScale(scale, RoundingMode.HALF_UP))

    private val fx = FxTable(listOf(
        FxRate("USD", "IDR", bd("16000"), 0, 0, "t"),
        FxRate("USD", "SGD", bd("1.25"), 0, 0, "t"),
    ))

    @Test fun `kurs langsung terbalik dan silang`() {
        eq("16000", fx.rate("USD", "IDR"))
        eq("0.0000625", fx.rate("IDR", "USD"), 10)
        eq("12800", fx.rate("SGD", "IDR")) // 1/1.25 × 16000
        eq("1", fx.rate("IDR", "IDR"))
        assertNull(fx.rate("EUR", "IDR"))
    }

    @Test fun `konversi nominal`() {
        eq("1600000", fx.convert(bd("100"), "USD", "IDR"))
    }

    private fun t(type: TxnType, q: String, p: String?, fee: String? = null, at: Long) =
        Txn("t$at", "p", type, bd(q), p?.let(::bd), fee?.let(::bd), "IDR", at)

    @Test fun `average cost dengan fee dan jual`() {
        val r = AverageCost.compute(
            listOf(
                t(TxnType.BUY, "10", "1000", "500", 1),   // biaya 10×100×1000 + 500 = 1.000.500
                t(TxnType.BUY, "10", "1200", "500", 2),   // biaya 1.200.500 → total 2.201.000 untuk 20 lot
                t(TxnType.SELL, "5", "1500", "300", 3),   // keluar 5 × 110.050 = 550.250; hasil 750.000 − 300
            ),
            unitsPerLot = BigDecimal(100),
        )
        assertTrue(r.hasBuy)
        eq("15", r.heldQuantity)
        eq("110050", r.averageCostPerPositionUnit)
        eq("1100.5", r.averageCostPerInstrumentUnit)
        eq("199450", r.realizedPl) // 749.700 − 550.250
    }

    @Test fun `tanpa BUY tidak punya riwayat`() {
        val r = AverageCost.compute(listOf(t(TxnType.ADJUST, "5", null, at = 1)))
        assertFalse(r.hasBuy)
        assertNull(r.averageCostPerPositionUnit)
    }

    @Test fun `jual habis lalu beli lagi`() {
        val r = AverageCost.compute(listOf(
            t(TxnType.BUY, "1", "100", at = 1), t(TxnType.SELL, "1", "150", at = 2), t(TxnType.BUY, "2", "200", at = 3),
        ))
        eq("2", r.heldQuantity)
        eq("200", r.averageCostPerPositionUnit)
        eq("50", r.realizedPl)
    }
}
