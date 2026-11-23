package id.shoko.arta.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM location ORDER BY sort_order, created_at")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM location ORDER BY sort_order, created_at")
    suspend fun getAll(): List<LocationEntity>

    @Query("SELECT * FROM location WHERE id = :id")
    suspend fun get(id: String): LocationEntity?

    @Query("SELECT * FROM location WHERE id = :id")
    fun observe(id: String): Flow<LocationEntity?>

    @Upsert
    suspend fun upsert(e: LocationEntity)

    @Query("UPDATE location SET archived = :archived, updated_at = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long)

    @Query("DELETE FROM location WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COALESCE(MAX(sort_order), 0) FROM location")
    suspend fun maxSortOrder(): Int
}

@Dao
interface InstrumentDao {
    @Query("SELECT * FROM instrument")
    fun observeAll(): Flow<List<InstrumentEntity>>

    @Query("SELECT * FROM instrument")
    suspend fun getAll(): List<InstrumentEntity>

    @Query("SELECT * FROM instrument WHERE id = :id")
    suspend fun get(id: String): InstrumentEntity?

    @Query("SELECT * FROM instrument WHERE asset_class = :assetClass AND symbol = :symbol")
    suspend fun find(assetClass: String, symbol: String): InstrumentEntity?

    @Query(
        "SELECT DISTINCT i.* FROM instrument i JOIN position p ON p.instrument_id = i.id " +
            "JOIN location l ON l.id = p.location_id WHERE p.archived = 0 AND l.archived = 0"
    )
    suspend fun getInUse(): List<InstrumentEntity>

    @Upsert
    suspend fun upsert(e: InstrumentEntity)
}

@Dao
interface PositionDao {
    @Query("SELECT * FROM position")
    fun observeAll(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM position")
    suspend fun getAll(): List<PositionEntity>

    @Query("SELECT * FROM position WHERE id = :id")
    suspend fun get(id: String): PositionEntity?

    @Query("SELECT * FROM position WHERE location_id = :locationId AND instrument_id = :instrumentId")
    suspend fun find(locationId: String, instrumentId: String): PositionEntity?

    @Upsert
    suspend fun upsert(e: PositionEntity)

    @Query("UPDATE position SET quantity = :quantity, updated_at = :now WHERE id = :id")
    suspend fun setQuantity(id: String, quantity: String, now: Long)

    @Query("UPDATE position SET archived = :archived, updated_at = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long)

    @Query("DELETE FROM position WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface TxnDao {
    @Query("SELECT * FROM txn ORDER BY executed_at")
    fun observeAll(): Flow<List<TxnEntity>>

    @Query("SELECT * FROM txn ORDER BY executed_at")
    suspend fun getAll(): List<TxnEntity>

    @Query("SELECT * FROM txn WHERE position_id = :positionId ORDER BY executed_at DESC")
    fun observeFor(positionId: String): Flow<List<TxnEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(e: TxnEntity)

    @Query("DELETE FROM txn WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PriceDao {
    @Query("SELECT * FROM price_latest")
    fun observeLatest(): Flow<List<PriceLatestEntity>>

    @Query("SELECT * FROM price_latest")
    suspend fun getLatest(): List<PriceLatestEntity>

    @Query("SELECT * FROM price_latest WHERE instrument_id = :id")
    suspend fun getLatest(id: String): PriceLatestEntity?

    @Upsert
    suspend fun upsertLatest(list: List<PriceLatestEntity>)

    @Upsert
    suspend fun upsertHistory(list: List<PriceHistoryEntity>)

    @Query("SELECT * FROM price_history WHERE instrument_id = :id AND date < :beforeDate ORDER BY date DESC LIMIT 1")
    suspend fun lastCloseBefore(id: String, beforeDate: String): PriceHistoryEntity?
}

@Dao
interface FxDao {
    @Query("SELECT * FROM fx_rate")
    fun observeAll(): Flow<List<FxRateEntity>>

    @Query("SELECT * FROM fx_rate")
    suspend fun getAll(): List<FxRateEntity>

    @Upsert
    suspend fun upsert(list: List<FxRateEntity>)
}

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM snapshot_daily ORDER BY date")
    fun observeDaily(): Flow<List<SnapshotDailyEntity>>

    @Query("SELECT * FROM snapshot_daily WHERE date = :date")
    suspend fun getDaily(date: String): SnapshotDailyEntity?

    @Upsert
    suspend fun upsertDaily(e: SnapshotDailyEntity)

    @Upsert
    suspend fun upsertItems(list: List<SnapshotItemEntity>)

    @Query("DELETE FROM snapshot_item WHERE date = :date")
    suspend fun deleteItems(date: String)

    @Transaction
    suspend fun replaceSnapshot(daily: SnapshotDailyEntity, items: List<SnapshotItemEntity>) {
        upsertDaily(daily)
        deleteItems(daily.date)
        upsertItems(items)
    }
}

@Dao
interface TargetDao {
    @Query("SELECT * FROM target_allocation")
    fun observeAll(): Flow<List<TargetAllocationEntity>>

    @Query("DELETE FROM target_allocation")
    suspend fun clear()

    @Upsert
    suspend fun upsert(list: List<TargetAllocationEntity>)

    @Transaction
    suspend fun replaceAll(list: List<TargetAllocationEntity>) {
        clear(); upsert(list)
    }
}

@Dao
interface AdvisorDao {
    @Query("SELECT * FROM advisor_message WHERE conversation_id = :conversationId ORDER BY created_at")
    fun observeConversation(conversationId: String): Flow<List<AdvisorMessageEntity>>

    @Query("SELECT * FROM advisor_message WHERE conversation_id = :conversationId ORDER BY created_at")
    suspend fun getConversation(conversationId: String): List<AdvisorMessageEntity>

    @Query(
        "SELECT m.* FROM advisor_message m JOIN (SELECT conversation_id, MIN(created_at) AS first_at FROM advisor_message " +
            "WHERE role = 'USER' GROUP BY conversation_id) f ON f.conversation_id = m.conversation_id AND f.first_at = m.created_at " +
            "ORDER BY m.created_at DESC"
    )
    fun observeConversationStarts(): Flow<List<AdvisorMessageEntity>>

    @Insert
    suspend fun insert(e: AdvisorMessageEntity)

    @Query("DELETE FROM advisor_message")
    suspend fun clear()

    @Delete
    suspend fun delete(e: AdvisorMessageEntity)
}
