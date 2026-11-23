package id.shoko.arta.data.repo

import androidx.room.withTransaction
import id.shoko.arta.core.money.Dec
import id.shoko.arta.core.time.Clock
import id.shoko.arta.data.db.ArtaDatabase
import id.shoko.arta.data.db.FxRateEntity
import id.shoko.arta.data.db.InstrumentEntity
import id.shoko.arta.data.db.LocationEntity
import id.shoko.arta.data.db.PositionEntity
import id.shoko.arta.data.db.PriceHistoryEntity
import id.shoko.arta.data.db.PriceLatestEntity
import id.shoko.arta.data.db.SnapshotDailyEntity
import id.shoko.arta.data.db.SnapshotItemEntity
import id.shoko.arta.data.db.TargetAllocationEntity
import id.shoko.arta.data.db.TxnEntity
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.LocationType
import id.shoko.arta.domain.model.PriceSource
import id.shoko.arta.domain.model.QuantityUnit
import id.shoko.arta.domain.model.TxnType
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

/**
 * Data demo (SPEC §5): total Rp 1.284.560.000, naik ±0,49% hari ini.
 * Hanya tersedia di build debug (BuildConfig.DEMO_DATA_AVAILABLE) kecuali diaktifkan manual.
 */
object DemoData {

    private class L(val id: String, val name: String, val type: LocationType)
    private class I(
        val id: String, val cls: AssetClass, val symbol: String, val name: String, val cur: String,
        val unit: QuantityUnit, val upl: Int, val src: PriceSource, val ref: String?,
        val price: String?, val changePct: String?,
    )
    private class P(val loc: String, val inst: String, val qty: String, val avg: String? = null)

    private val locations = listOf(
        L("demo-stockbit", "Stockbit", LocationType.BROKER),
        L("demo-indodax", "Indodax", LocationType.CRYPTO_EXCHANGE),
        L("demo-brankas", "Brankas rumah", LocationType.PHYSICAL),
        L("demo-bca", "BCA", LocationType.BANK),
        L("demo-pegadaian", "Pegadaian", LocationType.PLATFORM),
        L("demo-gotrade", "Gotrade", LocationType.BROKER),
        L("demo-jago", "Bank Jago", LocationType.BANK),
        L("demo-bibit", "Bibit", LocationType.PLATFORM),
    )

    private val instruments = listOf(
        I("demo-bbca", AssetClass.STOCK_IDX, "BBCA", "Bank Central Asia", "IDR", QuantityUnit.LOT, 100, PriceSource.YAHOO, "BBCA.JK", "9250", "1.9"),
        I("demo-bmri", AssetClass.STOCK_IDX, "BMRI", "Bank Mandiri", "IDR", QuantityUnit.LOT, 100, PriceSource.YAHOO, "BMRI.JK", "6100", "2.4"),
        I("demo-tlkm", AssetClass.STOCK_IDX, "TLKM", "Telkom Indonesia", "IDR", QuantityUnit.LOT, 100, PriceSource.YAHOO, "TLKM.JK", "2980", "-0.7"),
        I("demo-asii", AssetClass.STOCK_IDX, "ASII", "Astra International", "IDR", QuantityUnit.LOT, 100, PriceSource.YAHOO, "ASII.JK", "5200", "1.2"),
        I("demo-antm", AssetClass.STOCK_IDX, "ANTM", "Aneka Tambang", "IDR", QuantityUnit.LOT, 100, PriceSource.YAHOO, "ANTM.JK", "1620", "3.8"),
        I("demo-idr", AssetClass.CASH, "IDR", "Rupiah", "IDR", QuantityUnit.CURRENCY, 1, PriceSource.NONE, null, null, null),
        I("demo-btc", AssetClass.CRYPTO, "BTC", "Bitcoin", "IDR", QuantityUnit.COIN, 1, PriceSource.COINGECKO, "bitcoin", "1650000000", "-1.2"),
        I("demo-eth", AssetClass.CRYPTO, "ETH", "Ethereum", "IDR", QuantityUnit.COIN, 1, PriceSource.COINGECKO, "ethereum", "55184000", "-1.9"),
        I("demo-antam", AssetClass.GOLD, "ANTAM", "Emas Antam", "IDR", QuantityUnit.GRAM, 1, PriceSource.MANUAL, null, "2354000", "0.8"),
        I("demo-voo", AssetClass.ETF, "VOO", "Vanguard S&P 500 ETF", "USD", QuantityUnit.UNIT, 1, PriceSource.YAHOO, "VOO", "561.995", "1.1"),
        I("demo-rd", AssetClass.MUTUAL_FUND, "RD-CAMPURAN", "Reksa dana campuran", "IDR", QuantityUnit.UNIT, 1, PriceSource.MANUAL, null, "1605.70", "0.1"),
    )

    private val positions = listOf(
        P("demo-stockbit", "demo-bbca", "150", "8400"),
        P("demo-stockbit", "demo-bmri", "120", "5450"),
        P("demo-stockbit", "demo-tlkm", "200", "3400"),
        P("demo-stockbit", "demo-asii", "100", "4850"),
        P("demo-stockbit", "demo-antm", "250"),
        P("demo-stockbit", "demo-idr", "47009200"),
        P("demo-indodax", "demo-btc", "0.1"),
        P("demo-indodax", "demo-eth", "1.2"),
        P("demo-brankas", "demo-antam", "76.4"),
        P("demo-bca", "demo-idr", "128500000"),
        P("demo-pegadaian", "demo-antam", "43.65"),
        P("demo-gotrade", "demo-voo", "10"),
        P("demo-jago", "demo-idr", "77035100"),
        P("demo-bibit", "demo-rd", "40000"),
    )

    private val targets = mapOf(
        AssetClass.STOCK_IDX to 30, AssetClass.GOLD to 20, AssetClass.CRYPTO to 15,
        AssetClass.CASH to 20, AssetClass.ETF to 10, AssetClass.MUTUAL_FUND to 5,
    )

    const val USD_IDR = "16000"

    suspend fun seed(db: ArtaDatabase, clock: Clock) = db.withTransaction {
        val now = clock.nowMillis()
        locations.forEachIndexed { i, l ->
            db.locationDao().upsert(LocationEntity(l.id, l.name, l.type.name, "IDR", null, i, false, now, now))
        }
        instruments.forEach { x ->
            db.instrumentDao().upsert(
                InstrumentEntity(x.id, x.cls.code, x.symbol, x.name, x.cur, x.unit.name, BigDecimal(x.upl), x.src.name, x.ref)
            )
        }
        val today = clock.today()
        val latest = instruments.filter { it.price != null }.map { x ->
            val price = BigDecimal(x.price)
            val prev = x.changePct?.let { price.divide(BigDecimal.ONE.add(BigDecimal(it).divide(Dec.HUNDRED, Dec.MC)), Dec.MC) }
                ?.setScale(6, RoundingMode.HALF_UP)
            PriceLatestEntity(x.id, price, x.cur, now, now, x.src.name, prev)
        }
        db.priceDao().upsertLatest(latest)
        db.priceDao().upsertHistory(latest.flatMap { p ->
            listOfNotNull(
                PriceHistoryEntity(p.instrumentId, today.toString(), p.price, p.currency),
                p.prevClose?.let { PriceHistoryEntity(p.instrumentId, today.minusDays(1).toString(), it, p.currency) },
            )
        })
        db.fxDao().upsert(listOf(FxRateEntity("USD", "IDR", BigDecimal(USD_IDR), now, now, "Demo")))

        positions.forEach { p ->
            val id = "pos-${p.loc}-${p.inst}"
            db.positionDao().upsert(PositionEntity(id, p.loc, p.inst, BigDecimal(p.qty), null, false, now, now))
            if (p.avg != null) {
                db.txnDao().insert(
                    TxnEntity("txn-$id", id, TxnType.BUY.name, BigDecimal(p.qty), BigDecimal(p.avg), null, "IDR", now - 200L * 86_400_000L, "Demo")
                )
            }
        }
        db.targetDao().replaceAll(targets.map { (k, v) -> TargetAllocationEntity(k.code, BigDecimal(v)) })

        // Riwayat snapshot ±2 tahun: jalan acak naik pelan yang berakhir di total hari ini.
        val endTotal = BigDecimal("1284560000")
        val days = 730
        val rnd = Random(42)
        val shares = mapOf(
            AssetClass.STOCK_IDX to "0.32", AssetClass.GOLD to "0.22", AssetClass.CRYPTO to "0.18",
            AssetClass.CASH to "0.16", AssetClass.ETF to "0.07", AssetClass.MUTUAL_FUND to "0.05",
        )
        var factor = 1.0
        val factors = DoubleArray(days + 1)
        factors[days] = 1.0
        for (d in days - 1 downTo 0) {
            // Tren ±12%/tahun + noise harian.
            factor /= (1.0 + 0.00031 + (rnd.nextDouble() - 0.5) * 0.012)
            factors[d] = factor
        }
        // Hari ini −0,49% dari kemarin agar konsisten dengan "naik 0,49% hari ini".
        factors[days - 1] = 1.0 / 1.0049
        for (d in 0 until days) {
            val date = today.minusDays((days - d).toLong()).toString()
            val total = endTotal.multiply(BigDecimal(factors[d]), Dec.MC).setScale(0, RoundingMode.HALF_UP)
            db.snapshotDao().replaceSnapshot(
                SnapshotDailyEntity(date, total, "IDR", now),
                shares.map { (c, s) -> SnapshotItemEntity(date, c.code, total.multiply(BigDecimal(s), Dec.MC).setScale(0, RoundingMode.HALF_UP)) },
            )
        }
    }
}
