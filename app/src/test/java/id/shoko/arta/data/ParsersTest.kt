package id.shoko.arta.data

import id.shoko.arta.data.prices.CoinGeckoProvider
import id.shoko.arta.data.prices.FrankfurterFxProvider
import id.shoko.arta.data.prices.GoldSpotProvider
import id.shoko.arta.data.prices.Parsers
import id.shoko.arta.data.prices.YahooFinanceProvider
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.domain.model.Instrument
import id.shoko.arta.domain.model.PriceSource
import id.shoko.arta.domain.model.QuantityUnit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class ParsersTest {
    private fun fixture(name: String) = javaClass.getResource("/fixtures/$name")!!.readText()

    @Test fun `coingecko idr dan usd`() {
        val idr = Parsers.coinGecko(fixture("coingecko_simple_price.json"), "IDR").associateBy { it.id }
        assertEquals(BigDecimal("1650000000"), idr.getValue("bitcoin").price)
        assertEquals(BigDecimal("55184000.12"), idr.getValue("ethereum").price) // presisi desimal terjaga
        assertEquals(1763876880L, idr.getValue("bitcoin").lastUpdated!!.epochSecond)
        val usd = Parsers.coinGecko(fixture("coingecko_simple_price.json"), "usd").associateBy { it.id }
        assertEquals(BigDecimal("103125.5"), usd.getValue("bitcoin").price)
        assertNull(usd.getValue("ethereum").change24hPct)
    }

    @Test fun `penutupan sebelumnya dari perubahan 24 jam`() {
        val prev = Parsers.previousFromChange(BigDecimal("1650000000"), BigDecimal("-1.1976047904191618"))!!
        assertEquals(BigDecimal("1670000000"), prev.setScale(0, RoundingMode.HALF_UP))
    }

    @Test fun `yahoo chart`() {
        val c = Parsers.yahooChart(fixture("yahoo_chart_bbca.json"))!!
        assertEquals("BBCA.JK", c.symbol)
        assertEquals("IDR", c.currency)
        assertEquals(0, BigDecimal("9250").compareTo(c.price))
        assertEquals(0, BigDecimal("9075").compareTo(c.previousClose))
    }

    @Test fun `yahoo tanpa regularMarketPrice memakai close terakhir`() {
        val c = Parsers.yahooChart(fixture("yahoo_chart_noprice.json"))!!
        assertEquals(0, BigDecimal("560.1").compareTo(c.price))
        assertEquals(0, BigDecimal("555.9").compareTo(c.previousClose))
    }

    @Test fun `yahoo error tidak crash`() {
        assertNull(Parsers.yahooChart(fixture("yahoo_chart_error.json")))
        assertNull(Parsers.yahooChart("<html>rate limited</html>".let { "{}" }))
    }

    @Test fun frankfurter() {
        val r = Parsers.frankfurter(fixture("frankfurter_latest.json"))!!
        assertEquals("USD", r.base)
        assertEquals(LocalDate.of(2026, 11, 20), r.date)
        assertEquals(BigDecimal("16012.37"), r.rates["IDR"])
    }

    private fun mockClient(handler: (String) -> Pair<HttpStatusCode, String>) = HttpClient(MockEngine { req ->
        val (status, body) = handler(req.url.toString())
        respond(body, status, headersOf("Content-Type", "application/json"))
    })

    private val btc = Instrument("btc", AssetClass.CRYPTO, "BTC", "Bitcoin", "IDR", QuantityUnit.COIN, BigDecimal.ONE, PriceSource.COINGECKO, "bitcoin")
    private val bbca = Instrument("bbca", AssetClass.STOCK_IDX, "BBCA", "BCA", "IDR", QuantityUnit.LOT, BigDecimal(100), PriceSource.YAHOO, null)
    private val gold = Instrument("g", AssetClass.GOLD, "ANTAM", "Antam", "IDR", QuantityUnit.GRAM, BigDecimal.ONE, PriceSource.MANUAL, null)

    @Test fun `provider coingecko memanggil batch`() = runTest {
        var url = ""
        val p = CoinGeckoProvider(mockClient { url = it; HttpStatusCode.OK to fixture("coingecko_simple_price.json") })
        val q = p.fetch(listOf(btc))
        assertTrue(url, url.contains("ids=bitcoin") && url.contains("vs_currencies=idr%2Cusd"))
        assertEquals(1, q.size)
        assertEquals("IDR", q[0].currency)
    }

    @Test fun `provider yahoo memakai KODE JK dan mengabaikan 4xx`() = runTest {
        val urls = mutableListOf<String>()
        val p = YahooFinanceProvider(mockClient { u -> urls += u; if (u.contains("BBCA.JK")) HttpStatusCode.OK to fixture("yahoo_chart_bbca.json") else HttpStatusCode.TooManyRequests to "" })
        val etf = bbca.copy(id = "x", assetClass = AssetClass.ETF, symbol = "XXX", priceSource = PriceSource.YAHOO)
        val q = p.fetch(listOf(bbca, etf))
        assertEquals(1, q.size)
        assertTrue(urls.any { it.contains("/v8/finance/chart/BBCA.JK") })
    }

    @Test fun `emas spot per gram`() = runTest {
        val body = """{"chart":{"result":[{"meta":{"currency":"USD","symbol":"GC=F","regularMarketPrice":3110.348,"previousClose":3079.2442,"regularMarketTime":1763876880}}],"error":null}}"""
        val p = GoldSpotProvider(YahooFinanceProvider(mockClient { HttpStatusCode.OK to body }))
        val q = p.fetch(listOf(gold)).single()
        assertEquals(BigDecimal("100.00"), q.price.setScale(2, RoundingMode.HALF_UP)) // 3110,348 / 31,1034768
        assertEquals("USD", q.currency)
    }

    @Test fun `provider kurs`() = runTest {
        val p = FrankfurterFxProvider(mockClient { HttpStatusCode.OK to fixture("frankfurter_latest.json") })
        val r = p.fetch("USD", setOf("IDR", "SGD"))
        assertEquals(2, r.size)
        assertEquals(BigDecimal("16012.37"), r.first { it.quote == "IDR" }.rate)
    }
}
