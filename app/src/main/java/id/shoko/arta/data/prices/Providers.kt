package id.shoko.arta.data.prices

import id.shoko.arta.core.money.Dec
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.Instrument
import id.shoko.arta.domain.model.PriceSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import java.time.Instant

private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) Arta/0.1"

class HttpStatusException(val code: Int) : Exception("HTTP $code")

private suspend fun HttpClient.getText(url: String, params: Map<String, String> = emptyMap()): String {
    val resp = get(url) {
        header("User-Agent", USER_AGENT)
        header("Accept", "application/json")
        params.forEach { (k, v) -> parameter(k, v) }
    }
    if (!resp.status.isSuccess()) throw HttpStatusException(resp.status.value)
    return resp.bodyAsText()
}

/**
 * CoinGecko (API publik tanpa key; batas ±5–30 panggilan/menit). Semua koin dalam satu panggilan.
 * Endpoint: https://api.coingecko.com/api/v3/simple/price
 */
class CoinGeckoProvider(private val http: HttpClient, private val baseUrl: String = "https://api.coingecko.com/api/v3") : PriceProvider {
    override val source = PriceSource.COINGECKO

    override suspend fun fetch(instruments: List<Instrument>): List<PriceQuote> {
        if (instruments.isEmpty()) return emptyList()
        val ids = instruments.map { it.sourceRef ?: it.symbol }.distinct()
        val body = http.getText(
            "$baseUrl/simple/price",
            mapOf(
                "ids" to ids.joinToString(","),
                "vs_currencies" to "idr,usd",
                "include_last_updated_at" to "true",
                "include_24hr_change" to "true",
            ),
        )
        val byCurrency = instruments.groupBy { it.quoteCurrency.lowercase() }
        return byCurrency.flatMap { (cur, list) ->
            val parsed = Parsers.coinGecko(body, cur).associateBy { it.id }
            list.mapNotNull { inst ->
                val p = parsed[inst.sourceRef ?: inst.symbol] ?: return@mapNotNull null
                PriceQuote(inst.id, p.price, cur.uppercase(), p.lastUpdated ?: Instant.now(), Parsers.previousFromChange(p.price, p.change24hPct))
            }
        }
    }
}

/**
 * Yahoo Finance (tidak resmi): saham BEI `KODE.JK` (tertunda ±15 menit) dan ETF.
 * Satu panggilan per simbol karena endpoint quote batch butuh crumb/cookie.
 * Kegagalan per simbol diabaikan (harga lama tetap dipakai dan ditandai basi).
 */
class YahooFinanceProvider(private val http: HttpClient, private val baseUrl: String = "https://query1.finance.yahoo.com") : PriceProvider {
    override val source = PriceSource.YAHOO

    suspend fun chart(symbol: String): Parsers.YahooChart? =
        Parsers.yahooChart(http.getText("$baseUrl/v8/finance/chart/$symbol", mapOf("range" to "1d", "interval" to "1d")))

    override suspend fun fetch(instruments: List<Instrument>): List<PriceQuote> = instruments.mapNotNull { inst ->
        val ref = inst.sourceRef ?: defaultRef(inst)
        try {
            val c = chart(ref) ?: return@mapNotNull null
            PriceQuote(inst.id, c.price, c.currency, c.marketTime ?: Instant.now(), c.previousClose)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun defaultRef(inst: Instrument): String =
            if (inst.assetClass == AssetClass.STOCK_IDX) "${inst.symbol.uppercase()}.JK" else inst.symbol.uppercase()
    }
}

/**
 * Harga spot emas per gram (USD) dari harga XAU/USD per troy ounce ÷ 31,1034768.
 * Default memakai kontrak berjangka emas Yahoo (`GC=F`) sebagai proksi spot — mudah diganti.
 */
class GoldSpotProvider(private val yahoo: YahooFinanceProvider, private val symbol: String = "GC=F") : PriceProvider {
    override val source = PriceSource.GOLD_SPOT

    override suspend fun fetch(instruments: List<Instrument>): List<PriceQuote> {
        if (instruments.isEmpty()) return emptyList()
        val c = yahoo.chart(symbol) ?: return emptyList()
        val perGram = c.price.divide(Dec.GRAMS_PER_TROY_OUNCE, Dec.MC)
        val prevPerGram = c.previousClose?.divide(Dec.GRAMS_PER_TROY_OUNCE, Dec.MC)
        return instruments.map { PriceQuote(it.id, perGram, c.currency, c.marketTime ?: Instant.now(), prevPerGram) }
    }
}

/**
 * Frankfurter — kurs referensi ECB (mencakup IDR), hanya update hari kerja.
 * Endpoint: https://api.frankfurter.dev/v1/latest
 */
class FrankfurterFxProvider(private val http: HttpClient, private val baseUrl: String = "https://api.frankfurter.dev/v1") : FxProvider {
    override suspend fun fetch(base: String, quotes: Set<String>): List<FxQuote> {
        val q = quotes - base
        if (q.isEmpty()) return emptyList()
        val r = Parsers.frankfurter(http.getText("$baseUrl/latest", mapOf("base" to base, "symbols" to q.sorted().joinToString(","))))
            ?: return emptyList()
        val asOf = r.date?.let(Parsers::ecbDateToInstant) ?: Instant.now()
        return r.rates.map { (k, v) -> FxQuote(r.base, k, v, asOf, "ECB/Frankfurter") }
    }
}
