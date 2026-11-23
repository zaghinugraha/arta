package id.shoko.arta.domain.model

import java.math.BigDecimal

/** Model domain murni (tanpa Room) agar engine bisa diuji tanpa Android. */
data class Location(
    val id: String,
    val name: String,
    val type: LocationType,
    val defaultCurrency: String = "IDR",
    val note: String? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

data class Instrument(
    val id: String,
    val assetClass: AssetClass,
    val symbol: String,
    val name: String,
    val quoteCurrency: String,
    val unit: QuantityUnit,
    val unitsPerLot: BigDecimal = BigDecimal.ONE,
    val priceSource: PriceSource,
    val sourceRef: String? = null,
)

data class Position(
    val id: String,
    val locationId: String,
    val instrumentId: String,
    val quantity: BigDecimal,
    val note: String? = null,
    val archived: Boolean = false,
)

data class Txn(
    val id: String,
    val positionId: String,
    val type: TxnType,
    val quantity: BigDecimal,
    val price: BigDecimal?,
    val fee: BigDecimal?,
    val currency: String,
    val executedAt: Long,
    val note: String? = null,
)

data class PriceInfo(
    val instrumentId: String,
    val price: BigDecimal,
    val currency: String,
    val asOf: Long,
    val fetchedAt: Long,
    val source: PriceSource,
    /** Harga penutupan kemarin (untuk perubahan hari ini), bila diketahui. */
    val previousClose: BigDecimal? = null,
)

/** Kurs: 1 [base] = [rate] [quote]. */
data class FxRate(
    val base: String,
    val quote: String,
    val rate: BigDecimal,
    val asOf: Long,
    val fetchedAt: Long,
    val source: String,
)
