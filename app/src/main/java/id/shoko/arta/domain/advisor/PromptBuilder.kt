package id.shoko.arta.domain.advisor

import id.shoko.arta.core.money.MoneyFormat
import id.shoko.arta.core.money.percentOf
import id.shoko.arta.domain.analytics.Scenario
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Membuat ringkasan agregat untuk LLM: hanya persentase & batas.
 * Tanpa nama lokasi, tanpa simbol, tanpa nominal persis. Nominal disisipkan lokal lewat placeholder.
 */
object PromptBuilder {

    const val SYSTEM_PROMPT = "Kamu adalah asisten informasi portofolio di aplikasi Arta. Jawab dalam bahasa Indonesia yang singkat dan jelas. " +
        "Gunakan hanya angka yang ada di data JSON; jangan mengarang angka. Jelaskan dampak dan pilihan secara netral, " +
        "bukan rekomendasi beli/jual spesifik. Jika data tidak cukup, katakan apa yang kurang. " +
        "Akhiri tanpa disclaimer panjang; aplikasi sudah menampilkannya.\n\n" +
        "Bila perlu menyebut nominal rupiah, JANGAN menulis angkanya; tulis placeholder persis berikut dan aplikasi akan menggantinya: " +
        "{total}, {change_today}, {excess_amount} (kelebihan lokasi terbesar di atas batas), {usd_impact_5pct} (dampak kurs 5%), {simulation_after}, {simulation_diff}."

    fun summaryJson(ctx: AdvisorContext, scenario: Scenario? = null): JsonObject {
        val v = ctx.valuation
        fun r(x: BigDecimal, scale: Int = 0) = JsonPrimitive(x.setScale(scale, RoundingMode.HALF_UP))
        return buildJsonObject {
            put("base_currency", v.baseCurrency)
            put("as_of", ctx.asOfIso)
            put("total_change_today_pct", r(v.changeTodayPct, 2))
            putJsonObject("allocation_pct") {
                v.byClass.forEach { put(it.key.code, r(it.sharePct)) }
            }
            if (ctx.targets.isNotEmpty()) putJsonObject("target_pct") {
                ctx.targets.forEach { (k, t) -> put(k.code, r(t)) }
            }
            putJsonObject("change_today_pct_by_class") {
                v.byClass.forEach { put(it.key.code, r(it.changeTodayPct, 1)) }
            }
            put("location_count", v.byLocation.size)
            ctx.concentration.largest?.let { put("largest_location_pct", r(it.sharePct)) }
            put("locations_over_limit", ctx.concentration.overLimit.size)
            put("concentration_limit_pct", r(ctx.concentrationLimitPct))
            put("usd_exposure_pct", r(ctx.usd.sharePct))
            put("stale_price_count", v.staleCount)
            scenario?.let { s ->
                val res = ctx.simulationEngine.run(v, s)
                putJsonObject("simulation") {
                    if (s.cryptoPct != 0) put("crypto", s.cryptoPct)
                    if (s.ihsgPct != 0) put("ihsg", s.ihsgPct)
                    if (s.goldPct != 0) put("gold", s.goldPct)
                    if (s.usdIdrPct != 0) put("usd_idr", s.usdIdrPct)
                    put("total_change_pct", r(res.diffPct, 1))
                    putJsonObject("allocation_after_pct") {
                        res.byClass.forEach { put(it.assetClass.code, r(percentOf(it.after, res.totalAfter))) }
                    }
                }
            }
        }
    }

    fun userMessage(question: String, ctx: AdvisorContext, scenario: Scenario?): String =
        "Data portofolio (JSON):\n" + summaryJson(ctx, scenario).toString() + "\n\nPertanyaan: " + question

    /** Mengganti placeholder nominal dengan angka yang dihitung lokal. */
    fun fillPlaceholders(text: String, ctx: AdvisorContext, scenario: Scenario?): String {
        val cur = ctx.valuation.baseCurrency
        val sim = scenario?.let { ctx.simulationEngine.run(ctx.valuation, it) }
        val map = mapOf(
            "{total}" to MoneyFormat.compact(ctx.valuation.total, cur),
            "{change_today}" to MoneyFormat.compact(ctx.valuation.changeToday, cur, signed = true),
            "{excess_amount}" to MoneyFormat.compact(ctx.concentration.largest?.excessBase ?: BigDecimal.ZERO, cur),
            "{usd_impact_5pct}" to MoneyFormat.compact(ctx.usd.impactOf(BigDecimal(5)), cur),
            "{simulation_after}" to (sim?.let { MoneyFormat.compact(it.totalAfter, cur) } ?: "—"),
            "{simulation_diff}" to (sim?.let { MoneyFormat.compact(it.diff, cur, signed = true) } ?: "—"),
        )
        var out = text
        map.forEach { (k, v) -> out = out.replace(k, v) }
        return out
    }
}
