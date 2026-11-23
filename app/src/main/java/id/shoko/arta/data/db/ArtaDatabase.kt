package id.shoko.arta.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        LocationEntity::class, InstrumentEntity::class, PositionEntity::class, TxnEntity::class,
        PriceLatestEntity::class, PriceHistoryEntity::class, FxRateEntity::class,
        SnapshotDailyEntity::class, SnapshotItemEntity::class, TargetAllocationEntity::class,
        AdvisorMessageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ArtaDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun instrumentDao(): InstrumentDao
    abstract fun positionDao(): PositionDao
    abstract fun txnDao(): TxnDao
    abstract fun priceDao(): PriceDao
    abstract fun fxDao(): FxDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun targetDao(): TargetDao
    abstract fun advisorDao(): AdvisorDao

    companion object {
        const val NAME = "arta.db"
    }
}
