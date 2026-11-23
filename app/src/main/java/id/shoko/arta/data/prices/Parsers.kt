package id.shoko.arta.data.prices

import id.shoko.arta.core.money.Dec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Parser respons harga — fungsi murni agar bisa diuji dengan fixture JSON.
 * Angka dibaca langsung dari teks JSON ke BigDecimal (tanpa lewat Double).
 */
object Parsers {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun JsonElement?.dec(): BigDecimal? =
        (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content?.toBigDecimalOrNull()

    private fun JsonElement?.long(): Long? = (this as? JsonPrimitive)?.content?.toBigDecimalOrNull()?.toLong()
    private fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

    data class CoinGeckoPrice(val id: String, val price: BigDecimal, val change24hPct: BigDecimal?, val lastUpdated: Instant?)

    /**
     * CoinGecko `/simple/price?ids=..&vs_currencies=idr,usd&include_last_updated_at=true&include_24hr_change=true`
     * {"bitcoin":{"idr":1650000000,"idr_24h_change":-1.4,"usd":103000,"last_updated_at":1763876880}}
     */
    fun coinGecko(body: String, vsCurrency: String): List<CoinGeckoPrice> {
        val root = json.parseToJsonElement(body) as? JsonObject ?: return emptyList()
        val vs = vsCurrency.lowercase()
        return root.mapNotNull { (id, el) ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val price = o[vs].dec() ?: return@mapNotNull null
            CoinGeckoPrice(
                id = id,
                price = price,
                change24hPct = o["${vs}_24h_change"].dec(),
                lastUpdated = o["last_updated_at"].long()?.let(Instant::ofEpochSecond),
            )
        }
    }

    /** Hitung penutupan "kemarin" dari perubahan 24 jam: p / (1 + c/100). */
    fun previousFromChange(price: BigDecimal, changePct: BigDecimal?): BigDecimal? {
        if (changePct == null) return null
        val denom = BigDecimal.ONE.add(changePct.divide(Dec.HUNDRED, Dec.MC))
        if (denom.signum() <= 0) return null
        return price.divide(denom, Dec.MC)
    }

    data class YahooChart(
        val symbol: String,
        val currency: String,
        val price: BigDecimal,
        val marketTime: Instant?,
        val previousClose: BigDecimal?,
    )

    /**
     * Yahoo Finance `/v8/finance/chart/{symbol}?range=1d&interval=1d` (tidak resmi, bisa berubah).
     * {"chart":{"result":[{"meta":{"currency":"IDR","symbol":"BBCA.JK","regularMarketPrice":9250,
     *   "regularMarketTime":1763876880,"chartPreviousClose":9075,"previousClose":9075}}],"error":null}}
     */
    fun yahooChart(body: String): YahooChart? {
        val root = json.parseToJsonElement(body) as? JsonObject ?: return null
        val chart = root["chart"] as? JsonObject ?: return null
        val result = (chart["result"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return null
        val meta = result["meta"] as? JsonObject ?: return null
        val price = meta["regularMarketPrice"].dec() ?: lastClose(result) ?: return null
        return YahooChart(
            symbol = meta["symbol"].str() ?: "",
            currency = meta["currency"].str()?.uppercase() ?: "USD",
            price = price,
            marketTime = meta["regularMarketTime"].long()?.let(Instant::ofEpochSecond),
            previousClose = meta["previousClose"].dec() ?: meta["chartPreviousClose"].dec(),
        )
    }

    private fun lastClose(result: JsonObject): BigDecimal? = runCatching {
        val quote = result["indicators"]!!.jsonObject["quote"]!!.jsonArray.first().jsonObject
        quote["close"]!!.jsonArray.lastOrNull { it !is JsonNull }.dec()
    }.getOrNull()

    data class FxResult(val base: String, val date: LocalDate?, val rates: Map<String, BigDecimal>)

    /** Frankfurter `/v1/latest?base=USD&symbols=IDR` → {"amount":1.0,"base":"USD","date":"2026-11-20","rates":{"IDR":16000.5}} */
    fun frankfurter(body: String): FxResult? {
        val root = json.parseToJsonElement(body) as? JsonObject ?: return null
        val base = root["base"].str() ?: return null
        val rates = (root["rates"] as? JsonObject)?.mapNotNull { (k, v) -> v.dec()?.let { k to it } }?.toMap() ?: return null
        val date = root["date"].str()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return FxResult(base, date, rates)
    }

    /** Tanggal ECB → instant (jam 16.00 CET ≈ 15.00 UTC). */
    fun ecbDateToInstant(date: LocalDate): Instant = date.atTime(15, 0).toInstant(ZoneOffset.UTC)
}
