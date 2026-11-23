package id.shoko.arta

import id.shoko.arta.core.money.Dec
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.FxRate
import id.shoko.arta.domain.model.Instrument
import id.shoko.arta.domain.model.Location
import id.shoko.arta.domain.model.LocationType
import id.shoko.arta.domain.model.Position
import id.shoko.arta.domain.model.PriceInfo
import id.shoko.arta.domain.model.PriceSource
import id.shoko.arta.domain.model.QuantityUnit
import id.shoko.arta.domain.model.Txn
import id.shoko.arta.domain.model.TxnType
import id.shoko.arta.domain.valuation.FxTable
import id.shoko.arta.domain.valuation.PortfolioValuation
import id.shoko.arta.domain.valuation.ValuationEngine
import java.math.BigDecimal

/** Data demo SPEC §5 sebagai objek domain (tanpa Room). Total Rp 1.284.560.000. */
object TestPortfolio {
    const val NOW = 1_763_876_880_000L

    fun bd(s: String) = BigDecimal(s)

    val locations = listOf(
        Location("stockbit", "Stockbit", LocationType.BROKER),
        Location("indodax", "Indodax", LocationType.CRYPTO_EXCHANGE),
        Location("brankas", "Brankas rumah", LocationType.PHYSICAL),
        Location("bca", "BCA", LocationType.BANK),
        Location("pegadaian", "Pegadaian", LocationType.PLATFORM),
        Location("gotrade", "Gotrade", LocationType.BROKER),
        Location("jago", "Bank Jago", LocationType.BANK),
        Location("bibit", "Bibit", LocationType.PLATFORM),
    ).associateBy { it.id }

    private fun stock(sym: String) = Instrument(sym, AssetClass.STOCK_IDX, sym, sym, "IDR", QuantityUnit.LOT, BigDecimal(100), PriceSource.YAHOO, "$sym.JK")

    val instruments = listOf(
        stock("BBCA"), stock("BMRI"), stock("TLKM"), stock("ASII"), stock("ANTM"),
        Instrument("IDR", AssetClass.CASH, "IDR", "Rupiah", "IDR", QuantityUnit.CURRENCY, BigDecimal.ONE, PriceSource.NONE),
        Instrument("BTC", AssetClass.CRYPTO, "BTC", "Bitcoin", "IDR", QuantityUnit.COIN, BigDecimal.ONE, PriceSource.COINGECKO, "bitcoin"),
        Instrument("ETH", AssetClass.CRYPTO, "ETH", "Ethereum", "IDR", QuantityUnit.COIN, BigDecimal.ONE, PriceSource.COINGECKO, "ethereum"),
        Instrument("ANTAM", AssetClass.GOLD, "ANTAM", "Emas Antam", "IDR", QuantityUnit.GRAM, BigDecimal.ONE, PriceSource.MANUAL),
        Instrument("VOO", AssetClass.ETF, "VOO", "Vanguard S&P 500", "USD", QuantityUnit.UNIT, BigDecimal.ONE, PriceSource.YAHOO, "VOO"),
        Instrument("RD", AssetClass.MUTUAL_FUND, "RD", "RD campuran", "IDR", QuantityUnit.UNIT, BigDecimal.ONE, PriceSource.MANUAL),
    ).associateBy { it.id }

    private fun price(id: String, p: String, cur: String = "IDR", prev: String? = null) =
        PriceInfo(id, bd(p), cur, NOW, NOW, PriceSource.YAHOO, prev?.let(::bd))

    val prices = listOf(
        price("BBCA", "9250", prev = "9075"), price("BMRI", "6100", prev = "5960"), price("TLKM", "2980", prev = "3000"),
        price("ASII", "5200", prev = "5140"), price("ANTM", "1620", prev = "1560"),
        price("BTC", "1650000000", prev = "1670000000"), price("ETH", "55184000", prev = "56250000"),
        price("ANTAM", "2354000"), price("VOO", "561.995", "USD", prev = "555.9"), price("RD", "1605.70"),
    ).associateBy { it.instrumentId }

    val fx = FxTable(listOf(FxRate("USD", "IDR", bd("16000"), NOW, NOW, "test")))

    val positions = listOf(
        Position("p1", "stockbit", "BBCA", bd("150")), Position("p2", "stockbit", "BMRI", bd("120")),
        Position("p3", "stockbit", "TLKM", bd("200")), Position("p4", "stockbit", "ASII", bd("100")),
        Position("p5", "stockbit", "ANTM", bd("250")), Position("p6", "stockbit", "IDR", bd("47009200")),
        Position("p7", "indodax", "BTC", bd("0.1")), Position("p8", "indodax", "ETH", bd("1.2")),
        Position("p9", "brankas", "ANTAM", bd("76.4")), Position("p10", "bca", "IDR", bd("128500000")),
        Position("p11", "pegadaian", "ANTAM", bd("43.65")), Position("p12", "gotrade", "VOO", bd("10")),
        Position("p13", "jago", "IDR", bd("77035100")), Position("p14", "bibit", "RD", bd("40000")),
    )

    val txns = listOf(
        Txn("t1", "p1", TxnType.BUY, bd("150"), bd("8400"), null, "IDR", NOW - 1000),
        Txn("t2", "p2", TxnType.BUY, bd("120"), bd("5450"), null, "IDR", NOW - 1000),
        Txn("t3", "p3", TxnType.BUY, bd("200"), bd("3400"), null, "IDR", NOW - 1000),
        Txn("t4", "p4", TxnType.BUY, bd("100"), bd("4850"), null, "IDR", NOW - 1000),
    )

    val targets = mapOf(
        AssetClass.STOCK_IDX to bd("30"), AssetClass.GOLD to bd("20"), AssetClass.CRYPTO to bd("15"),
        AssetClass.CASH to bd("20"), AssetClass.ETF to bd("10"), AssetClass.MUTUAL_FUND to bd("5"),
    )

    fun valuation(base: String = "IDR"): PortfolioValuation {
        val byPos = txns.groupBy { it.positionId }
        val pvs = positions.map { p ->
            val inst = instruments.getValue(p.instrumentId)
            ValuationEngine.valuePosition(p, inst, locations.getValue(p.locationId), prices[inst.id], byPos[p.id].orEmpty(), fx, base, null, NOW)
        }
        return ValuationEngine.aggregate(base, pvs)
    }

    @Suppress("unused") private val mc = Dec.MC
}
