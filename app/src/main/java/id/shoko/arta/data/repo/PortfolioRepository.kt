package id.shoko.arta.data.repo

import androidx.room.withTransaction
import id.shoko.arta.core.time.Clock
import id.shoko.arta.data.db.ArtaDatabase
import id.shoko.arta.data.db.DatabaseHolder
import id.shoko.arta.data.db.InstrumentEntity
import id.shoko.arta.data.db.LocationEntity
import id.shoko.arta.data.db.PositionEntity
import id.shoko.arta.data.db.PriceHistoryEntity
import id.shoko.arta.data.db.PriceLatestEntity
import id.shoko.arta.data.db.TargetAllocationEntity
import id.shoko.arta.data.db.TxnEntity
import id.shoko.arta.data.db.toDomain
import id.shoko.arta.data.settings.Settings
import id.shoko.arta.data.settings.SettingsRepository
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.FxRate
import id.shoko.arta.domain.model.Instrument
import id.shoko.arta.domain.model.Location
import id.shoko.arta.domain.model.LocationType
import id.shoko.arta.domain.model.Position
import id.shoko.arta.domain.model.PriceInfo
import id.shoko.arta.domain.model.PriceSource
import id.shoko.arta.domain.model.Txn
import id.shoko.arta.domain.model.TxnType
import id.shoko.arta.domain.valuation.FxTable
import id.shoko.arta.domain.valuation.PortfolioValuation
import id.shoko.arta.domain.valuation.ValuationEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.util.UUID

/** Data mentah portofolio yang sudah dipetakan ke model domain. */
data class PortfolioData(
    val locations: List<Location>,
    val instruments: Map<String, Instrument>,
    val positions: List<Position>,
    val txns: List<Txn>,
    val prices: Map<String, PriceInfo>,
    val fx: List<FxRate>,
    val targets: Map<AssetClass, BigDecimal>,
)

data class PortfolioState(
    val data: PortfolioData,
    val settings: Settings,
    val valuation: PortfolioValuation,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioRepository(
    private val holder: DatabaseHolder,
    private val settingsRepo: SettingsRepository,
    private val clock: Clock,
) {
    private val db: ArtaDatabase get() = holder.require()

    val data: Flow<PortfolioData> = holder.db.filterNotNull().flatMapLatest { d ->
        combine(
            listOf(
                d.locationDao().observeAll(),
                d.instrumentDao().observeAll(),
                d.positionDao().observeAll(),
                d.txnDao().observeAll(),
                d.priceDao().observeLatest(),
                d.fxDao().observeAll(),
                d.targetDao().observeAll(),
            )
        ) { arr ->
            @Suppress("UNCHECKED_CAST")
            PortfolioData(
                locations = (arr[0] as List<LocationEntity>).map { it.toDomain() },
                instruments = (arr[1] as List<InstrumentEntity>).associate { it.id to it.toDomain() },
                positions = (arr[2] as List<PositionEntity>).map { it.toDomain() },
                txns = (arr[3] as List<TxnEntity>).map { it.toDomain() },
                prices = (arr[4] as List<PriceLatestEntity>).associate { it.instrumentId to it.toDomain() },
                fx = (arr[5] as List<id.shoko.arta.data.db.FxRateEntity>).map { it.toDomain() },
                targets = (arr[6] as List<TargetAllocationEntity>).associate { AssetClass.fromCode(it.assetClass) to it.percent },
            )
        }
    }

    val state: Flow<PortfolioState> = combine(data, settingsRepo.settings) { d, s ->
        PortfolioState(d, s, value(d, s))
    }

    val valuation: Flow<PortfolioValuation> = state.map { it.valuation }

    fun value(d: PortfolioData, s: Settings): PortfolioValuation {
        val fx = FxTable(d.fx)
        val locById = d.locations.associateBy { it.id }
        val txByPos = d.txns.groupBy { it.positionId }
        val now = clock.nowMillis()
        val pvs = d.positions.filter { !it.archived }.mapNotNull { p ->
            val loc = locById[p.locationId] ?: return@mapNotNull null
            if (loc.archived) return@mapNotNull null
            val inst = d.instruments[p.instrumentId] ?: return@mapNotNull null
            ValuationEngine.valuePosition(p, inst, loc, d.prices[inst.id], txByPos[p.id].orEmpty(), fx, s.baseCurrency, s.staleAfterMillis, now)
        }
        return ValuationEngine.aggregate(s.baseCurrency, pvs)
    }

    suspend fun currentState(): PortfolioState = state.first()

    // ---------- Lokasi ----------

    suspend fun addLocation(name: String, type: LocationType, currency: String = "IDR", note: String? = null): String {
        val now = clock.nowMillis()
        val id = UUID.randomUUID().toString()
        db.locationDao().upsert(
            LocationEntity(id, name.trim(), type.name, currency, note, db.locationDao().maxSortOrder() + 1, false, now, now)
        )
        return id
    }

    suspend fun updateLocation(id: String, name: String, type: LocationType, note: String?) {
        val e = db.locationDao().get(id) ?: return
        db.locationDao().upsert(e.copy(name = name.trim(), type = type.name, note = note, updatedAt = clock.nowMillis()))
    }

    suspend fun setLocationArchived(id: String, archived: Boolean) = db.locationDao().setArchived(id, archived, clock.nowMillis())
    suspend fun deleteLocation(id: String) = db.locationDao().delete(id)

    // ---------- Instrumen & posisi ----------

    suspend fun ensureInstrument(d: InstrumentDraft): String {
        val dao = db.instrumentDao()
        dao.find(d.assetClass.code, d.symbol)?.let { return it.id }
        val id = UUID.randomUUID().toString()
        dao.upsert(
            InstrumentEntity(id, d.assetClass.code, d.symbol, d.name, d.quoteCurrency, d.unit.name, d.unitsPerLot, d.priceSource.name, d.sourceRef)
        )
        return id
    }

    data class BuyInput(val price: BigDecimal, val fee: BigDecimal?, val executedAt: Long)

    /**
     * Tambah posisi. Bila posisi instrumen yang sama sudah ada di lokasi itu, kuantitas ditambahkan.
     * Bila [buy] diisi, dicatat sebagai transaksi BUY.
     */
    suspend fun addPosition(locationId: String, draft: InstrumentDraft, quantity: BigDecimal, buy: BuyInput?, manualPrice: BigDecimal?): String =
        db.withTransaction {
            val instrumentId = ensureInstrument(draft)
            val now = clock.nowMillis()
            val existing = db.positionDao().find(locationId, instrumentId)
            val positionId = existing?.id ?: UUID.randomUUID().toString()
            val newQty = (existing?.quantity ?: BigDecimal.ZERO).add(quantity)
            db.positionDao().upsert(
                PositionEntity(positionId, locationId, instrumentId, newQty, existing?.note, false, existing?.createdAt ?: now, now)
            )
            if (buy != null) {
                db.txnDao().insert(
                    TxnEntity(UUID.randomUUID().toString(), positionId, TxnType.BUY.name, quantity, buy.price, buy.fee, draft.quoteCurrency, buy.executedAt)
                )
            }
            if (manualPrice != null) setManualPriceInternal(instrumentId, manualPrice, draft.quoteCurrency)
            positionId
        }

    suspend fun setQuantity(positionId: String, quantity: BigDecimal) {
        db.withTransaction {
            val p = db.positionDao().get(positionId) ?: return@withTransaction
            val diff = quantity.subtract(p.quantity)
            db.positionDao().setQuantity(positionId, quantity.toPlainString(), clock.nowMillis())
            if (diff.signum() != 0) {
                val inst = db.instrumentDao().get(p.instrumentId)
                db.txnDao().insert(
                    TxnEntity(UUID.randomUUID().toString(), positionId, TxnType.ADJUST.name, diff, null, null, inst?.quoteCurrency ?: "IDR", clock.nowMillis(), "Ubah jumlah")
                )
            }
        }
    }

    /** Catat beli/jual: transaksi + perubahan kuantitas dalam satu transaksi DB. */
    suspend fun recordTrade(positionId: String, type: TxnType, quantity: BigDecimal, price: BigDecimal?, fee: BigDecimal?, executedAt: Long, note: String?) {
        require(type == TxnType.BUY || type == TxnType.SELL || type == TxnType.DEPOSIT || type == TxnType.WITHDRAW || type == TxnType.DIVIDEND)
        db.withTransaction {
            val p = db.positionDao().get(positionId) ?: return@withTransaction
            val inst = db.instrumentDao().get(p.instrumentId) ?: return@withTransaction
            db.txnDao().insert(
                TxnEntity(UUID.randomUUID().toString(), positionId, type.name, quantity, price, fee, inst.quoteCurrency, executedAt, note)
            )
            val newQty = when (type) {
                TxnType.BUY, TxnType.DEPOSIT -> p.quantity.add(quantity)
                TxnType.SELL, TxnType.WITHDRAW -> p.quantity.subtract(quantity).max(BigDecimal.ZERO)
                else -> p.quantity
            }
            if (newQty.compareTo(p.quantity) != 0) db.positionDao().setQuantity(positionId, newQty.toPlainString(), clock.nowMillis())
        }
    }

    suspend fun deleteTxn(id: String) = db.txnDao().delete(id)
    suspend fun setPositionArchived(id: String, archived: Boolean) = db.positionDao().setArchived(id, archived, clock.nowMillis())
    suspend fun deletePosition(id: String) = db.positionDao().delete(id)

    fun observeTxns(positionId: String): Flow<List<Txn>> =
        holder.db.filterNotNull().flatMapLatest { it.txnDao().observeFor(positionId) }.map { l -> l.map { it.toDomain() } }

    // ---------- Harga manual ----------

    suspend fun setManualPrice(instrumentId: String, price: BigDecimal, currency: String) = setManualPriceInternal(instrumentId, price, currency)

    private suspend fun setManualPriceInternal(instrumentId: String, price: BigDecimal, currency: String) {
        val now = clock.nowMillis()
        val today = clock.today()
        val prev = db.priceDao().lastCloseBefore(instrumentId, today.toString())?.close
        db.priceDao().upsertLatest(listOf(PriceLatestEntity(instrumentId, price, currency, now, now, PriceSource.MANUAL.name, prev)))
        db.priceDao().upsertHistory(listOf(PriceHistoryEntity(instrumentId, today.toString(), price, currency)))
    }

    // ---------- Target alokasi ----------

    suspend fun setTargets(targets: Map<AssetClass, BigDecimal>) {
        db.targetDao().replaceAll(targets.filterValues { it.signum() > 0 }.map { (k, v) -> TargetAllocationEntity(k.code, v) })
    }
}
