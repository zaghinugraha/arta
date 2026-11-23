package id.shoko.arta

import android.content.Context
import id.shoko.arta.core.money.MoneyFormat
import id.shoko.arta.core.security.KeystoreManager
import id.shoko.arta.core.security.PassphraseStore
import id.shoko.arta.core.time.Clock
import id.shoko.arta.core.time.SystemClock
import id.shoko.arta.data.db.DatabaseHolder
import id.shoko.arta.data.prices.CoinGeckoProvider
import id.shoko.arta.data.prices.FrankfurterFxProvider
import id.shoko.arta.data.prices.GoldSpotProvider
import id.shoko.arta.data.prices.PriceRepository
import id.shoko.arta.data.prices.YahooFinanceProvider
import id.shoko.arta.data.repo.AdvisorRepository
import id.shoko.arta.data.repo.DemoData
import id.shoko.arta.data.repo.PortfolioRepository
import id.shoko.arta.data.repo.SnapshotRepository
import id.shoko.arta.data.settings.SettingsRepository
import id.shoko.arta.domain.advisor.AdvisorService
import id.shoko.arta.core.ui.AssetClassRes
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.llm.ProviderLlmClient
import id.shoko.arta.work.MoveNotifier
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** DI manual (ringan, tanpa Hilt/Koin). */
class AppContainer(val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val clock: Clock = SystemClock

    val settings = SettingsRepository(context)
    val keystore = KeystoreManager(context)
    val passphraseStore = PassphraseStore(context, keystore)
    val dbHolder = DatabaseHolder(context)

    val http: HttpClient by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = 8_000
                requestTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }
            install(HttpRequestRetry) {
                maxRetries = 1
                retryOnExceptionOrServerErrors()
                exponentialDelay()
            }
            // Sengaja TANPA plugin Logging: jangan pernah me-log key, nominal, atau isi percakapan.
        }
    }

    private val yahoo by lazy { YahooFinanceProvider(http) }

    val portfolio = PortfolioRepository(dbHolder, settings, clock)
    val snapshots = SnapshotRepository(dbHolder, clock)
    val advisorRepo = AdvisorRepository(dbHolder, clock)
    val priceRepository by lazy {
        PriceRepository(dbHolder, settings, clock, CoinGeckoProvider(http), yahoo, GoldSpotProvider(yahoo), FrankfurterFxProvider(http))
    }
    val llmClient by lazy { ProviderLlmClient(http) }
    val advisorService by lazy { AdvisorService(llmClient) }

    /** Label kelas aset untuk teks template (UI memakai strings.xml). */
    val classLabel: (AssetClass) -> String = { c -> context.getString(AssetClassRes.label(c)) }

    private val openMutex = Mutex()

    /**
     * Untuk worker latar: buka DB bila bisa tanpa interaksi pengguna.
     * Bila passphrase dibungkus kunci ber-autentikasi dan aplikasi tidak sedang terbuka, return false.
     */
    suspend fun openDatabaseInBackground(): Boolean = openMutex.withLock {
        if (dbHolder.isOpen) return true
        if (passphraseStore.isAuthWrapped() != false) return false
        return withContext(Dispatchers.IO) {
            when (val r = passphraseStore.loadOrCreate(requireAuthForNew = false)) {
                is PassphraseStore.LoadResult.Ok -> runCatching { dbHolder.open(r.passphrase) }.isSuccess.also { r.passphrase.fill(0) }
                else -> false
            }
        }
    }

    /** Setelah refresh harga: snapshot hari ini & cek notifikasi pergerakan. */
    @Suppress("UNUSED_PARAMETER")
    suspend fun afterPriceRefresh(forceSnapshot: Boolean = false) {
        if (!dbHolder.isOpen) return
        val state = portfolio.currentState()
        val v = state.valuation
        if (v.positions.isEmpty()) return
        snapshots.writeToday(v)
        val s = state.settings
        val today = clock.today().toString()
        if (s.notifyMoveEnabled && s.lastMoveNotifiedDate != today && v.changeTodayPct.abs() > s.notifyMove) {
            val dir = if (v.changeTodayPct.signum() >= 0) context.getString(R.string.notif_move_up) else context.getString(R.string.notif_move_down)
            MoveNotifier.notify(context, context.getString(R.string.notif_move_text, dir, MoneyFormat.pct(v.changeTodayPct.abs(), 1)))
            settings.setLastMoveNotified(today)
        }
    }

    suspend fun seedDemo() = withContext(Dispatchers.IO) {
        DemoData.seed(dbHolder.require(), clock)
        settings.setOnboardingDone(true)
    }

    /** Hapus semua data: DB, kunci, setelan. */
    suspend fun wipeEverything() = withContext(Dispatchers.IO) {
        dbHolder.deleteFiles()
        passphraseStore.destroy()
        settings.clearAll()
        id.shoko.arta.work.WorkScheduler.cancelAll(context)
    }
}
