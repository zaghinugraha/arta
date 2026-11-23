package id.shoko.arta.domain.advisor

import id.shoko.arta.core.money.MoneyFormat
import id.shoko.arta.domain.analytics.InsightEngine
import id.shoko.arta.domain.analytics.Scenario
import id.shoko.arta.domain.model.AssetClass
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Jawaban template (tanpa LLM) dengan angka dari engine lokal.
 * Dipakai bila API key kosong, Advisor tidak boleh membaca portofolio, offline, atau error.
 */
class TemplateResponder {

    fun respond(intent: AdvisorIntent, ctx: AdvisorContext, scenario: Scenario? = null, llmAvailable: Boolean = false): AdvisorReply {
        if (ctx.isEmpty && intent != AdvisorIntent.FREE) {
            return AdvisorReply("Belum ada posisi yang tercatat. Tambahkan lokasi dan posisi lewat tombol + agar aku bisa membaca portofoliomu.")
        }
        return when (intent) {
            AdvisorIntent.CONCENTRATION -> AdvisorReply(concentration(ctx))
            AdvisorIntent.TARGET -> AdvisorReply(target(ctx))
            AdvisorIntent.USD_EXPOSURE -> AdvisorReply(usd(ctx))
            AdvisorIntent.SIMULATION -> simulation(ctx, scenario ?: Scenario(cryptoPct = -30))
            AdvisorIntent.GREETING -> AdvisorReply(greeting(ctx))
            AdvisorIntent.FREE -> AdvisorReply(if (llmAvailable) FREE_LLM_DOWN else FREE_NO_LLM)
        }
    }

    fun greeting(ctx: AdvisorContext): String {
        val v = ctx.valuation
        val move = v.changeTodayPct
        val dir = when (move.signum()) { 1 -> "naik"; -1 -> "turun"; else -> "tidak berubah" }
        val biggest = v.byClass.filter { it.changeTodayBase.signum() != 0 }
            .maxByOrNull { it.changeTodayBase.abs() }
        val driver = if (biggest != null && move.signum() != 0) ", terutama dari ${lower(ctx.classLabel(biggest.key))}" else ""
        val first = if (move.signum() == 0) "Kekayaanmu tidak berubah hari ini."
        else "Kekayaanmu $dir ${MoneyFormat.pct(move.abs(), 2)} hari ini$driver."
        val insight = InsightEngine.pick(v, ctx.concentrationLimitPct, ctx.notifyMovePct, ctx.targets, ctx.classLabel)
        val second = when (insight) {
            is id.shoko.arta.domain.analytics.Insight.ConcentrationOver ->
                " Satu hal yang perlu diperhatikan: porsi ${insight.locationName} sekarang ${MoneyFormat.pct(insight.sharePct)}, di atas batas ${MoneyFormat.pct(insight.limitPct)} yang kamu atur."
            is id.shoko.arta.domain.analytics.Insight.BigMove -> ""
            else -> " " + insight.text
        }
        return first + second
    }

    fun concentration(ctx: AdvisorContext): String {
        val c = ctx.concentration
        val largest = c.largest ?: return "Belum ada lokasi yang tercatat."
        val limit = MoneyFormat.pct(c.limitPct)
        val cur = ctx.valuation.baseCurrency
        return if (largest.overLimit) {
            val others = c.overLimit.drop(1)
            val extra = if (others.isEmpty()) "" else " Lokasi lain yang juga lewat batas: " + others.joinToString { "${it.location.name} (${MoneyFormat.pct(it.sharePct)})" } + "."
            "${largest.location.name} memegang ${MoneyFormat.pct(largest.sharePct)} kekayaanmu, sedangkan batasmu $limit. " +
                "Untuk kembali ke $limit, porsi di sana perlu berkurang sekitar ${MoneyFormat.compact(largest.excessBase, cur)}, " +
                "atau bisa juga menunggu aset di lokasi lain tumbuh.$extra"
        } else {
            "Lokasi terbesarmu ${largest.location.name} memegang ${MoneyFormat.pct(largest.sharePct)} kekayaanmu, masih di bawah batas $limit. Tidak ada lokasi yang melewati batas."
        }
    }

    fun target(ctx: AdvisorContext): String {
        val items = ctx.allocation.filter { it.targetPct != null }
        if (items.isEmpty()) return "Kamu belum mengatur target alokasi. Atur di Pengaturan → Target alokasi."
        val cur = ctx.valuation.baseCurrency
        val targetList = items.joinToString { "${short(ctx, it.assetClass)} ${MoneyFormat.number(it.targetPct!!)}" }
        val threshold = BigDecimal("0.5")
        val over = items.filter { it.diffPct!! > threshold }.sortedByDescending { it.diffPct }
        val under = items.filter { it.diffPct!! < threshold.negate() }.sortedBy { it.diffPct }
        if (over.isEmpty() && under.isEmpty()) return "Alokasimu sudah sangat dekat dengan target ($targetList). Tidak ada kelas yang menyimpang lebih dari 0,5 poin."
        fun describe(list: List<id.shoko.arta.domain.analytics.AllocationItem>, word: String): String =
            list.joinToString { "${short(ctx, it.assetClass)} $word ${MoneyFormat.pct(it.diffPct!!.abs())} (±${MoneyFormat.compact(it.diffBase!!.abs(), cur)})" }
        val parts = buildList {
            if (over.isNotEmpty()) add(describe(over, "lebih"))
            if (under.isNotEmpty()) add(describe(under, "kurang"))
        }
        return "Dibanding target alokasimu ($targetList): " + parts.joinToString(". ") + "."
    }

    fun usd(ctx: AdvisorContext): String {
        val u = ctx.usd
        if (ctx.valuation.baseCurrency == "USD") return "Mata uang dasarmu USD, jadi eksposur kurs dihitung terhadap rupiah tidak berlaku."
        if (u.usdLinkedBase.signum() == 0) return "Tidak ada asetmu yang nilainya bergantung pada kurs dolar."
        val classes = u.byClass.keys.sortedBy { AssetClass.displayOrder.indexOf(it) }.joinToString { short(ctx, it) }
        val impact = MoneyFormat.compact(u.impactOf(BigDecimal(5)), ctx.valuation.baseCurrency)
        return "Sekitar ${MoneyFormat.pct(u.sharePct)} kekayaanmu ($classes) ikut bergerak bersama kurs dolar. " +
            "Kalau rupiah melemah 5%, nilai bagian ini naik sekitar $impact; kalau menguat 5%, turun dengan besar yang sama."
    }

    fun simulation(ctx: AdvisorContext, s: Scenario): AdvisorReply {
        val r = ctx.simulationEngine.run(ctx.valuation, s)
        val cur = ctx.valuation.baseCurrency
        val label = scenarioLabel(s)
        val sentences = mutableListOf<String>()
        val moved = r.byClass.filter { it.diff.signum() != 0 }.sortedByDescending { it.diff.abs() }
        moved.firstOrNull()?.let { m ->
            val dir = if (m.diff.signum() < 0) "turun" else "naik"
            sentences += "Nilai ${lower(ctx.classLabel(m.assetClass))} akan $dir sekitar ${MoneyFormat.compact(m.diff.abs(), cur)}."
        }
        val totalPhrase = "Total kekayaanmu jadi ${MoneyFormat.compact(r.totalAfter, cur)} (${MoneyFormat.signedPct(r.diffPct)})"
        val primary = primaryClass(s)
        sentences += if (primary != null && r.byClass.any { it.assetClass == primary }) {
            "$totalPhrase, dan porsi ${lower(ctx.classLabel(primary))} berubah dari ${MoneyFormat.pct(r.shareBefore(primary))} ke ${MoneyFormat.pct(r.shareAfter(primary))}."
        } else "$totalPhrase."
        r.byClass.firstOrNull { it.assetClass == AssetClass.CASH && it.diff.signum() == 0 }?.let {
            sentences += "${ctx.classLabel(AssetClass.CASH)} ${MoneyFormat.compact(it.before, cur)} tidak terpengaruh."
        }
        val card = SimulationCard(
            label = label,
            cryptoPct = s.cryptoPct, ihsgPct = s.ihsgPct, goldPct = s.goldPct, usdIdrPct = s.usdIdrPct,
            before = r.totalBefore.setScale(0, RoundingMode.HALF_UP).toPlainString(),
            after = r.totalAfter.setScale(0, RoundingMode.HALF_UP).toPlainString(),
            diffPct = r.diffPct.setScale(2, RoundingMode.HALF_UP).toPlainString(),
        )
        return AdvisorReply(sentences.joinToString(" "), card)
    }

    private fun short(ctx: AdvisorContext, c: AssetClass): String = when (c) {
        AssetClass.STOCK_IDX -> "saham"
        AssetClass.MUTUAL_FUND -> "RD"
        AssetClass.ETF -> "ETF"
        else -> lower(ctx.classLabel(c))
    }

    private fun primaryClass(s: Scenario): AssetClass? = when {
        s.cryptoPct != 0 -> AssetClass.CRYPTO
        s.ihsgPct != 0 -> AssetClass.STOCK_IDX
        s.goldPct != 0 -> AssetClass.GOLD
        else -> null
    }

    companion object {
        /** Huruf kecil untuk kata biasa, tapi akronim (BEI, ETF, USD) dipertahankan. */
        fun lower(label: String): String = label.split(" ").joinToString(" ") { w ->
            if (w.length > 1 && w.all { it.isUpperCase() || !it.isLetter() }) w else w.lowercase()
        }

        const val FREE_LLM_DOWN = "Maaf, AI sedang tidak bisa dihubungi. Coba salah satu saran di bawah, atau tanya \"kalau kripto turun 30%\"."
        const val FREE_NO_LLM = "AI Advisor belum dikonfigurasi, jadi aku hanya bisa menjawab saran di bawah dan pertanyaan simulasi " +
            "seperti \"kalau kripto turun 30%\". Pemilik aplikasi bisa mengisi API key di local.properties."

        fun scenarioLabel(s: Scenario): String {
            fun p(v: Int) = (if (v > 0) "+" else if (v < 0) MoneyFormat.MINUS else "") + kotlin.math.abs(v) + "%"
            val parts = buildList {
                if (s.cryptoPct != 0) add("kripto ${p(s.cryptoPct)}")
                if (s.ihsgPct != 0) add("IHSG ${p(s.ihsgPct)}")
                if (s.goldPct != 0) add("emas ${p(s.goldPct)}")
                if (s.usdIdrPct != 0) add("USD/IDR ${p(s.usdIdrPct)}")
            }
            return if (parts.isEmpty()) "Simulasi" else "Simulasi · " + parts.joinToString(", ")
        }
    }
}
