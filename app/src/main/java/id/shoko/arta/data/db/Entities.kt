package id.shoko.arta.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

// Semua uang/kuantitas: TEXT desimal (BigDecimal.toPlainString()) lewat TypeConverter.
// Waktu: INTEGER epoch millis UTC. ID: TEXT UUID.

@Entity(tableName = "location")
data class LocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    @ColumnInfo(name = "default_currency") val defaultCurrency: String = "IDR",
    val note: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    val archived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "instrument",
    indices = [Index(value = ["asset_class", "symbol"], unique = true)],
)
data class InstrumentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "asset_class") val assetClass: String,
    val symbol: String,
    val name: String,
    @ColumnInfo(name = "quote_currency") val quoteCurrency: String,
    val unit: String,
    @ColumnInfo(name = "units_per_lot") val unitsPerLot: BigDecimal,
    @ColumnInfo(name = "price_source") val priceSource: String,
    @ColumnInfo(name = "source_ref") val sourceRef: String? = null,
)

@Entity(
    tableName = "position",
    foreignKeys = [
        ForeignKey(entity = LocationEntity::class, parentColumns = ["id"], childColumns = ["location_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = InstrumentEntity::class, parentColumns = ["id"], childColumns = ["instrument_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["location_id", "instrument_id"], unique = true), Index("instrument_id")],
)
data class PositionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "location_id") val locationId: String,
    @ColumnInfo(name = "instrument_id") val instrumentId: String,
    val quantity: BigDecimal,
    val note: String? = null,
    val archived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "txn",
    foreignKeys = [ForeignKey(entity = PositionEntity::class, parentColumns = ["id"], childColumns = ["position_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("position_id")],
)
data class TxnEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "position_id") val positionId: String,
    val type: String,
    val quantity: BigDecimal,
    val price: BigDecimal? = null,
    val fee: BigDecimal? = null,
    val currency: String,
    @ColumnInfo(name = "executed_at") val executedAt: Long,
    val note: String? = null,
)

@Entity(
    tableName = "price_latest",
    foreignKeys = [ForeignKey(entity = InstrumentEntity::class, parentColumns = ["id"], childColumns = ["instrument_id"], onDelete = ForeignKey.CASCADE)],
)
data class PriceLatestEntity(
    @PrimaryKey @ColumnInfo(name = "instrument_id") val instrumentId: String,
    val price: BigDecimal,
    val currency: String,
    @ColumnInfo(name = "as_of") val asOf: Long,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
    val source: String,
    /** Tambahan dari spesifikasi: penutupan sebelumnya dari sumber, untuk perubahan hari ini. */
    @ColumnInfo(name = "prev_close") val prevClose: BigDecimal? = null,
)

@Entity(
    tableName = "price_history",
    primaryKeys = ["instrument_id", "date"],
    foreignKeys = [ForeignKey(entity = InstrumentEntity::class, parentColumns = ["id"], childColumns = ["instrument_id"], onDelete = ForeignKey.CASCADE)],
)
data class PriceHistoryEntity(
    @ColumnInfo(name = "instrument_id") val instrumentId: String,
    /** yyyy-MM-dd */
    val date: String,
    val close: BigDecimal,
    val currency: String,
)

@Entity(tableName = "fx_rate", primaryKeys = ["base", "quote"])
data class FxRateEntity(
    val base: String,
    val quote: String,
    val rate: BigDecimal,
    @ColumnInfo(name = "as_of") val asOf: Long,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
    val source: String,
)

@Entity(tableName = "snapshot_daily")
data class SnapshotDailyEntity(
    @PrimaryKey val date: String,
    @ColumnInfo(name = "total_base") val totalBase: BigDecimal,
    @ColumnInfo(name = "base_currency") val baseCurrency: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "snapshot_item",
    primaryKeys = ["date", "asset_class"],
    foreignKeys = [ForeignKey(entity = SnapshotDailyEntity::class, parentColumns = ["date"], childColumns = ["date"], onDelete = ForeignKey.CASCADE)],
)
data class SnapshotItemEntity(
    val date: String,
    @ColumnInfo(name = "asset_class") val assetClass: String,
    @ColumnInfo(name = "value_base") val valueBase: BigDecimal,
)

@Entity(tableName = "target_allocation")
data class TargetAllocationEntity(
    @PrimaryKey @ColumnInfo(name = "asset_class") val assetClass: String,
    val percent: BigDecimal,
)

@Entity(tableName = "advisor_message", indices = [Index("conversation_id")])
data class AdvisorMessageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    val role: String,
    val content: String,
    @ColumnInfo(name = "card_json") val cardJson: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
