package id.shoko.arta.data.prices

import id.shoko.arta.core.time.Clock
import id.shoko.arta.data.db.DatabaseHolder
import id.shoko.arta.data.db.FxRateEntity
import id.shoko.arta.data.db.PriceHistoryEntity
import id.shoko.arta.data.db.PriceLatestEntity
import id.shoko.arta.data.db.toDomain
import id.shoko.arta.data.settings.SettingsRepository
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.GoldReference
import id.shoko.arta.domain.model.Instrument
import id.shoko.arta.domain.model.PriceSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

data class RefreshResult(val updated: Int, val failedSources: Set<String>, val at: Long)

/**
 * Mengambil harga & kurs dari provider publik, menyimpannya ke cache (price_latest, price_history, fx_rate).
 * Kegagalan sumber tidak mengosongkan nilai: harga lama dipakai dan ditandai basi.
 */
class PriceRepository(
    private val holder: DatabaseHolder,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val coinGecko: PriceProvider,
    private val yahoo: PriceProvider,
    private val goldSpot: PriceProvider,
    private val fxProvider: FxProvider,
) {
    private val mutex = Mutex()
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    private val _lastResult = MutableStateFlow<RefreshResult?>(null)
    val lastResult: StateFlow<RefreshResult?> = _lastResult.asStateFlow()

    /** Refresh bila data terakhir lebih tua dari [maxAgeMillis]. */
    suspend fun refreshIfOlderThan(maxAgeMillis: Long): RefreshResult? {
        val db = holder.db.value ?: return null
        val latest = db.priceDao().getLatest().filter { it.source != PriceSource.MANUAL.name }.maxOfOrNull { it.fetchedAt }
        if (latest != null && clock.nowMillis() - latest < maxAgeMillis) return null
        return refresh()
    }

    suspend fun refresh(): RefreshResult? = mutex.withLock {
        val db = holder.db.value ?: return null
        _refreshing.value = true
        try {
            val s = settings.current()
            val instruments = db.instrumentDao().getInUse().map { it.toDomain() }
            val failed = mutableSetOf<String>()
            val quotes = mutableListOf<PriceQuote>()

            suspend fun run(name: String, list: List<Instrument>, p: PriceProvider) {
                if (list.isEmpty()) return
                try {
                    val r = p.fetch(list)
                    quotes += r
                    if (r.size < list.size) failed += name
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    failed += name
                }
            }

            run("CoinGecko", instruments.filter { it.priceSource == PriceSource.COINGECKO }, coinGecko)
            run("Yahoo Finance", instruments.filter { it.priceSource == PriceSource.YAHOO }, yahoo)
            if (s.goldReference == GoldReference.SPOT) {
                run("Emas spot", instruments.filter { it.assetClass == AssetClass.GOLD || it.priceSource == PriceSource.GOLD_SPOT }, goldSpot)
            }

            val now = clock.nowMillis()
            val instById = instruments.associateBy { it.id }
            val latest = quotes.map { q ->
                val inst = instById[q.instrumentId]
                val src = if (inst?.assetClass == AssetClass.GOLD && s.goldReference == GoldReference.SPOT) PriceSource.GOLD_SPOT
                else inst?.priceSource ?: PriceSource.MANUAL
                PriceLatestEntity(q.instrumentId, q.price, q.currency, q.asOf.toEpochMilli(), now, src.name, q.previousClose)
            }
            val today = clock.today().toString()
            val history = quotes.map { q -> PriceHistoryEntity(q.instrumentId, LocalDate.ofInstant(q.asOf, clock.zone()).toString().let { d -> if (d > today) today else d }, q.price, q.currency) }
            if (latest.isNotEmpty()) {
                db.priceDao().upsertLatest(latest)
                db.priceDao().upsertHistory(history)
            }

            // Kurs: semua mata uang yang dipakai → disimpan sebagai pasangan USD→X.
            val currencies = buildSet {
                add(s.baseCurrency)
                instruments.forEach { add(it.quoteCurrency) }
                quotes.forEach { add(it.currency) }
                db.locationDao().getAll().forEach { add(it.defaultCurrency) }
            } - "USD"
            if (currencies.isNotEmpty()) {
                try {
                    val fx = fxProvider.fetch("USD", currencies)
                    if (fx.isNotEmpty()) {
                        db.fxDao().upsert(fx.map { FxRateEntity(it.base, it.quote, it.rate, it.asOf.toEpochMilli(), now, it.source) })
                    } else failed += "Kurs"
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    failed += "Kurs"
                }
            }
            RefreshResult(latest.size, failed, now).also { _lastResult.value = it }
        } finally {
            _refreshing.value = false
        }
    }
}
