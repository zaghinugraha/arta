package id.shoko.arta.domain

import id.shoko.arta.TestPortfolio
import id.shoko.arta.TestPortfolio.bd
import id.shoko.arta.domain.advisor.AdvisorContext
import id.shoko.arta.domain.advisor.AdvisorIntent
import id.shoko.arta.domain.advisor.AdvisorService
import id.shoko.arta.domain.advisor.PromptBuilder
import id.shoko.arta.domain.advisor.QuestionParser
import id.shoko.arta.domain.advisor.TemplateResponder
import id.shoko.arta.domain.model.AssetClass
import id.shoko.arta.llm.ChatTurn
import id.shoko.arta.llm.LlmClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvisorTest {
    private val labels = mapOf(
        AssetClass.STOCK_IDX to "Saham BEI", AssetClass.GOLD to "Emas", AssetClass.CRYPTO to "Kripto",
        AssetClass.CASH to "Tabungan", AssetClass.ETF to "ETF", AssetClass.MUTUAL_FUND to "Reksa dana", AssetClass.OTHER to "Aset lain",
    )
    private val ctx = AdvisorContext(
        TestPortfolio.valuation(), bd("25"), bd("3"), TestPortfolio.targets, { labels.getValue(it) }, asOfIso = "2026-11-23T06:48:00+07:00",
    )
    private val t = TemplateResponder()

    @Test fun `parser chip dan pertanyaan simulasi`() {
        assertEquals(AdvisorIntent.CONCENTRATION, QuestionParser.parse("Cek konsentrasi").intent)
        assertEquals(AdvisorIntent.TARGET, QuestionParser.parse("Bandingkan dengan target").intent)
        assertEquals(AdvisorIntent.USD_EXPOSURE, QuestionParser.parse("Eksposur USD").intent)
        val q = QuestionParser.parse("Kalau kripto turun 30%, dampaknya gimana?")
        assertEquals(AdvisorIntent.SIMULATION, q.intent)
        assertEquals(-30, q.scenario!!.cryptoPct)
        assertEquals(10, QuestionParser.parse("kalau rupiah melemah 10 persen?").scenario!!.usdIdrPct)
        assertEquals(15, QuestionParser.parse("bagaimana jika emas naik 15%").scenario!!.goldPct)
        assertEquals(AdvisorIntent.FREE, QuestionParser.parse("apa kabar?").intent)
    }

    @Test fun `template konsentrasi`() {
        val s = t.concentration(ctx)
        assertTrue(s, s.startsWith("Stockbit memegang 32% kekayaanmu, sedangkan batasmu 25%."))
        assertTrue(s, s.contains("Rp 89,9 jt"))
    }

    @Test fun `template target`() {
        val s = t.target(ctx)
        assertTrue(s, s.contains("kripto lebih 3% (±Rp 38,5 jt)"))
        // Kas RDN Stockbit dihitung sebagai Tabungan, jadi tabungan hanya −0,3 poin (tidak disebut).
        assertTrue(s, s.contains("ETF kurang 3% (±Rp 38,5 jt)"))
    }

    @Test fun `template eksposur USD`() {
        val s = t.usd(ctx)
        assertTrue(s, s.startsWith("Sekitar 47% kekayaanmu"))
        assertTrue(s, s.contains("Rp 30,2 jt"))
    }

    @Test fun `template simulasi dengan kartu`() {
        val r = t.respond(AdvisorIntent.SIMULATION, ctx, QuestionParser.parse("kalau kripto turun 30%").scenario)
        assertTrue(r.text, r.text.contains("Rp 69,4 jt"))
        assertTrue(r.text, r.text.contains("dari 18% ke 13%"))
        assertTrue(r.text, r.text.contains("Tabungan Rp 252,5 jt tidak terpengaruh"))
        assertNotNull(r.card)
        val card = r.card!!
        assertEquals("-5.40", card.diffPct)
        assertEquals(-30, card.cryptoPct)
    }

    @Test fun `ringkasan untuk LLM tanpa nama lokasi dan nominal`() {
        val json = PromptBuilder.summaryJson(ctx, QuestionParser.parse("kripto turun 30%").scenario).toString()
        assertFalse(json, json.contains("Stockbit"))
        assertFalse(json, json.contains("BBCA"))
        assertFalse(json, json.contains("1284560000"))
        assertTrue(json, json.contains("\"largest_location_pct\":32"))
        assertTrue(json, json.contains("\"usd_exposure_pct\":47"))
        assertTrue(json, json.contains("\"total_change_pct\":-5.4"))
    }

    private class FakeLlm(private val configured: Boolean, private val reply: () -> String) : LlmClient {
        var calls = 0
        override val isConfigured get() = configured
        override suspend fun complete(system: String, messages: List<ChatTurn>): Result<String> { calls++; return Result.success(reply()) }
    }

    @Test fun `tanpa konfigurasi memakai template`() = runTest {
        val llm = FakeLlm(false) { "x" }
        val r = AdvisorService(llm).answer("Cek konsentrasi", ctx, emptyList(), allowPortfolioRead = true)
        assertEquals(0, llm.calls)
        assertFalse(r.usedLlm)
        assertTrue(r.text.contains("Stockbit"))
    }

    @Test fun `izin baca portofolio mati memakai template`() = runTest {
        val llm = FakeLlm(true) { "x" }
        val r = AdvisorService(llm).answer("Cek konsentrasi", ctx, emptyList(), allowPortfolioRead = false)
        assertEquals(0, llm.calls)
        assertFalse(r.usedLlm)
    }

    @Test fun `placeholder LLM yang belum diimplementasi jatuh ke template`() = runTest {
        val llm = FakeLlm(true) { TODO("belum") }
        val r = AdvisorService(llm).answer("Eksposur USD", ctx, emptyList(), allowPortfolioRead = true)
        assertFalse(r.usedLlm)
        assertTrue(r.text.contains("47%"))
    }

    @Test fun `jawaban LLM diisi placeholder nominal lokal`() = runTest {
        val llm = FakeLlm(true) { "Kelebihannya sekitar {excess_amount}." }
        val r = AdvisorService(llm).answer("Cek konsentrasi", ctx, emptyList(), allowPortfolioRead = true)
        assertTrue(r.usedLlm)
        assertEquals("Kelebihannya sekitar Rp 89,9 jt.", r.text)
    }
}
