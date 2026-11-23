package id.shoko.arta.domain

import id.shoko.arta.TestPortfolio
import id.shoko.arta.TestPortfolio.bd
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.FxRate
import id.shoko.arta.domain.model.Instrument
import id.shoko.arta.domain.model.Location
import id.shoko.arta.domain.model.LocationType
import id.shoko.arta.domain.model.Position
import id.shoko.arta.domain.model.PriceInfo
import id.shoko.arta.domain.model.PriceSource
import id.shoko.arta.domain.model.QuantityUnit
import id.shoko.arta.domain.valuation.FxTable
import id.shoko.arta.domain.valuation.ValuationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ValuationEngineTest {
    private val loc = Location("l", "L", LocationType.BROKER)
    private val now = 1_000_000_000L

    private fun assertDec(expected: String, actual: BigDecimal) =
        assertEquals("expected $expected but was ${actual.toPlainString()}", 0, bd(expected).compareTo(actual))

    @Test fun `saham BEI dihitung per lot x 100 lembar`() {
        val inst = Instrument("BBCA", AssetClass.STOCK_IDX, "BBCA", "BCA", "IDR", QuantityUnit.LOT, BigDecimal(100), PriceSource.YAHOO)
        val pv = ValuationEngine.valuePosition(
            Position("p", "l", "BBCA", bd("150")), inst, loc,
            PriceInfo("BBCA", bd("9250"), "IDR", now, now, PriceSource.YAHOO, bd("9075")),
            emptyList(), FxTable.EMPTY, "IDR", null, now,
        )
        assertDec("138750000", pv.valueBase)
        assertDec("2625000", pv.changeTodayBase) // 150 × 100 × (9250 − 9075)
    }

    @Test fun `emas per gram`() {
        val inst = Instrument("G", AssetClass.GOLD, "ANTAM", "Antam", "IDR", QuantityUnit.GRAM, BigDecimal.ONE, PriceSource.MANUAL)
        val pv = ValuationEngine.valuePosition(
            Position("p", "l", "G", bd("25")), inst, loc, PriceInfo("G", bd("2354000"), "IDR", now, now, PriceSource.MANUAL, bd("2300000")),
            emptyList(), FxTable.EMPTY, "IDR", null, now,
        )
        assertDec("58850000", pv.valueBase)
        assertDec("0", pv.changeTodayBase) // MANUAL → perubahan hari ini 0
    }

    @Test fun `ETF USD dikonversi dengan kurs`() {
        val inst = Instrument("VOO", AssetClass.ETF, "VOO", "VOO", "USD", QuantityUnit.UNIT, BigDecimal.ONE, PriceSource.YAHOO)
        val fx = FxTable(listOf(FxRate("USD", "IDR", bd("16000"), now, now, "t")))
        val pv = ValuationEngine.valuePosition(
            Position("p", "l", "VOO", bd("10")), inst, loc, PriceInfo("VOO", bd("561.995"), "USD", now, now, PriceSource.YAHOO),
            emptyList(), fx, "IDR", null, now,
        )
        assertDec("89919200", pv.valueBase)
    }

    @Test fun `kas valas USD dikali kurs, kas IDR apa adanya`() {
        val usd = Instrument("USD", AssetClass.CASH, "USD", "Dolar", "USD", QuantityUnit.CURRENCY, BigDecimal.ONE, PriceSource.NONE)
        val idr = Instrument("IDR", AssetClass.CASH, "IDR", "Rupiah", "IDR", QuantityUnit.CURRENCY, BigDecimal.ONE, PriceSource.NONE)
        val fx = FxTable(listOf(FxRate("USD", "IDR", bd("16000"), now, now, "t")))
        val a = ValuationEngine.valuePosition(Position("a", "l", "USD", bd("1000")), usd, loc, null, emptyList(), fx, "IDR", null, now)
        val b = ValuationEngine.valuePosition(Position("b", "l", "IDR", bd("5000000")), idr, loc, null, emptyList(), fx, "IDR", null, now)
        assertDec("16000000", a.valueBase)
        assertDec("5000000", b.valueBase)
        assertFalse(a.missingPrice)
    }

    @Test fun `harga belum ada tidak crash dan ditandai`() {
        val inst = Instrument("X", AssetClass.CRYPTO, "X", "X", "IDR", QuantityUnit.COIN, BigDecimal.ONE, PriceSource.COINGECKO)
        val pv = ValuationEngine.valuePosition(Position("p", "l", "X", bd("1")), inst, loc, null, emptyList(), FxTable.EMPTY, "IDR", null, now)
        assertTrue(pv.missingPrice)
        assertDec("0", pv.valueBase)
    }

    @Test fun `harga basi setelah ambang`() {
        val inst = Instrument("X", AssetClass.CRYPTO, "X", "X", "IDR", QuantityUnit.COIN, BigDecimal.ONE, PriceSource.COINGECKO)
        val old = now - 31 * 60_000L
        val pv = ValuationEngine.valuePosition(
            Position("p", "l", "X", bd("1")), inst, loc, PriceInfo("X", bd("10"), "IDR", old, old, PriceSource.COINGECKO),
            emptyList(), FxTable.EMPTY, "IDR", 30 * 60_000L, now,
        )
        assertTrue(pv.isStale)
        assertDec("10", pv.valueBase) // nilai tidak dikosongkan
    }

    @Test fun `total data demo sesuai spesifikasi`() {
        val v = TestPortfolio.valuation()
        assertDec("1284560000", v.total)
        assertEquals(8, v.byLocation.size)
        assertDec("411059200", v.byLocation.first { it.key.id == "stockbit" }.valueBase)
        assertEquals("stockbit", v.byLocation.first().key.id)
        assertDec("231220800", v.classValue(AssetClass.CRYPTO))
    }

    @Test fun `unrealized PL hanya untuk posisi dengan riwayat beli`() {
        val v = TestPortfolio.valuation()
        val stockbit = v.byLocation.first { it.key.id == "stockbit" }.positions
        val bbca = stockbit.first { it.instrument.symbol == "BBCA" }
        assertDec("12750000", bbca.unrealizedPlBase!!) // 150 lot × 100 × (9250 − 8400)
        assertDec("8400", bbca.averageCost!!)
        val antm = stockbit.first { it.instrument.symbol == "ANTM" }
        assertEquals(null, antm.unrealizedPlBase)
        assertFalse(antm.hasHistory)
    }

    @Test fun `basis USD membalik kurs`() {
        val v = TestPortfolio.valuation("USD")
        assertDec("80285", v.total.setScale(0, java.math.RoundingMode.HALF_UP)) // 1.284.560.000 / 16.000
    }
}
