package id.shoko.arta.feature

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.shoko.arta.AppContainer
import id.shoko.arta.data.repo.CsvExporter
import id.shoko.arta.data.repo.InstrumentDraft
import id.shoko.arta.data.repo.PortfolioRepository
import id.shoko.arta.data.repo.PortfolioState
import id.shoko.arta.data.repo.SnapshotPoint
import id.shoko.arta.data.settings.Settings
import id.shoko.arta.domain.analytics.Scenario
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.LocationType
import id.shoko.arta.domain.model.TxnType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * ViewModel bersama (scope Activity) untuk data portofolio, harga, dan aksi pencatatan.
 * Semua pekerjaan jaringan & DB di luar main thread.
 */
class AppViewModel(val container: AppContainer) : ViewModel() {

    val state: StateFlow<PortfolioState?> = container.portfolio.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val snapshots: StateFlow<List<SnapshotPoint>> = container.snapshots.points
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings: StateFlow<Settings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())
    val refreshing get() = container.priceRepository.refreshing
    val lastRefresh get() = container.priceRepository.lastResult

    /** Skenario simulasi dibagi antara tab Simulasi dan kartu di Advisor. */
    private val _scenario = MutableStateFlow(Scenario())
    val scenario: StateFlow<Scenario> = _scenario.asStateFlow()
    fun setScenario(s: Scenario) { _scenario.value = s }

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun showMessage(m: String) { _message.value = m }
    fun consumeMessage() { _message.value = null }

    init {
        viewModelScope.launch {
            // Snapshot hari ini saat app dibuka bila belum ada.
            val st = state.filterNotNull().first()
            if (st.valuation.positions.isNotEmpty() && !container.snapshots.hasToday()) container.snapshots.writeToday(st.valuation)
        }
        viewModelScope.launch {
            // Interval 5 menit hanya berlaku saat aplikasi terbuka (WorkManager minimal 15 menit).
            while (true) {
                val minutes = settings.value.priceIntervalMin.coerceAtLeast(5)
                delay(minutes * 60_000L)
                refreshNow(silent = true)
            }
        }
    }

    /** Dipanggil saat app dibuka/di-resume: refresh bila data > 5 menit. */
    fun onResume() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (container.priceRepository.refreshIfOlderThan(5 * 60_000L) != null) container.afterPriceRefresh()
            }
        }
    }

    fun refreshNow(silent: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val r = runCatching { container.priceRepository.refresh() }.getOrNull()
            runCatching { container.afterPriceRefresh() }
            if (!silent && r != null) {
                _message.value = if (r.failedSources.isEmpty()) "Harga diperbarui" else "Sebagian harga gagal diperbarui: ${r.failedSources.joinToString()}"
            }
        }
    }

    // ---------- Pencatatan ----------

    fun addLocation(name: String, type: LocationType, onDone: (String) -> Unit = {}) {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { container.portfolio.addLocation(name, type) }
            onDone(id)
        }
    }

    suspend fun addLocationNow(name: String, type: LocationType): String = withContext(Dispatchers.IO) { container.portfolio.addLocation(name, type) }

    fun updateLocation(id: String, name: String, type: LocationType, note: String?) = io { container.portfolio.updateLocation(id, name, type, note) }
    fun setLocationArchived(id: String, archived: Boolean) = io { container.portfolio.setLocationArchived(id, archived) }
    fun deleteLocation(id: String) = io { container.portfolio.deleteLocation(id) }

    fun addPosition(
        locationId: String, draft: InstrumentDraft, quantity: BigDecimal,
        buy: PortfolioRepository.BuyInput?, manualPrice: BigDecimal?, onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                container.portfolio.addPosition(locationId, draft, quantity, buy, manualPrice)
                container.settings.setOnboardingDone(true)
            }
            onDone()
            refreshNow(silent = true)
        }
    }

    fun setQuantity(positionId: String, q: BigDecimal) = io { container.portfolio.setQuantity(positionId, q) }
    fun recordTrade(positionId: String, type: TxnType, qty: BigDecimal, price: BigDecimal?, fee: BigDecimal?, at: Long, note: String?) =
        io { container.portfolio.recordTrade(positionId, type, qty, price, fee, at, note) }
    fun deleteTxn(id: String) = io { container.portfolio.deleteTxn(id) }
    fun setPositionArchived(id: String, archived: Boolean) = io { container.portfolio.setPositionArchived(id, archived) }
    fun deletePosition(id: String) = io { container.portfolio.deletePosition(id) }
    fun setManualPrice(instrumentId: String, price: BigDecimal, currency: String) = io { container.portfolio.setManualPrice(instrumentId, price, currency) }
    fun setTargets(t: Map<AssetClass, BigDecimal>) = io { container.portfolio.setTargets(t) }
    fun observeTxns(positionId: String) = container.portfolio.observeTxns(positionId)

    fun seedDemo() {
        viewModelScope.launch {
            container.seedDemo()
            _message.value = "Data demo diisi"
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val st = state.value ?: container.portfolio.currentState()
            val ok = runCatching {
                container.context.contentResolver.openOutputStream(uri, "wt")?.use { CsvExporter.write(it, st.data, st.valuation) }
                    ?: error("no stream")
            }.isSuccess
            _message.value = if (ok) "Ekspor selesai. Simpan file di tempat aman — file tidak terenkripsi." else "Ekspor gagal"
        }
    }

    private fun io(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { block() }
    }
}
