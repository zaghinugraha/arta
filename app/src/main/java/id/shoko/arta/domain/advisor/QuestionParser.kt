package id.shoko.arta.domain.advisor

import id.shoko.arta.domain.analytics.Scenario

/**
 * Parsing sederhana untuk pertanyaan Advisor.
 * Mengenali chip saran dan pola "kalau <aset> turun/naik <n>%".
 */
object QuestionParser {
    private val subjectRegex = Regex("(kripto|crypto|bitcoin|btc|koin|saham|ihsg|bei|emas|gold|dolar|dollar|usd|rupiah|idr)")
    private val moveRegex = Regex("(turun|naik|melemah|menguat|jatuh|anjlok|drop|minus|plus)")
    private val numberRegex = Regex("(\\d+(?:[.,]\\d+)?)\\s*(%|persen)?")

    fun parse(raw: String): ParsedQuestion {
        val q = raw.lowercase().trim()
        when {
            q.contains("konsentrasi") || q.contains("terlalu besar") || q.contains("terbesar") -> return ParsedQuestion(AdvisorIntent.CONCENTRATION)
            q.contains("target") || q.contains("alokasi") -> return ParsedQuestion(AdvisorIntent.TARGET)
            (q.contains("eksposur") || q.contains("exposure")) -> return ParsedQuestion(AdvisorIntent.USD_EXPOSURE)
        }
        parseScenario(q)?.let { return ParsedQuestion(AdvisorIntent.SIMULATION, it) }
        if (q.contains("usd") || q.contains("dolar")) return ParsedQuestion(AdvisorIntent.USD_EXPOSURE)
        return ParsedQuestion(AdvisorIntent.FREE)
    }

    fun parseScenario(q: String): Scenario? {
        val subject = subjectRegex.find(q)?.value ?: return null
        val move = moveRegex.find(q)?.value ?: return null
        val number = numberRegex.findAll(q).map { it.groupValues[1] }.firstOrNull() ?: return null
        val n = number.replace(',', '.').toDoubleOrNull()?.let { Math.round(it).toInt() } ?: return null
        if (n == 0 || n > 100) return null
        val down = move in setOf("turun", "melemah", "jatuh", "anjlok", "drop", "minus")
        val signed = if (down) -n else n
        return when (subject) {
            "kripto", "crypto", "bitcoin", "btc", "koin" -> Scenario(cryptoPct = signed)
            "saham", "ihsg", "bei" -> Scenario(ihsgPct = signed)
            "emas", "gold" -> Scenario(goldPct = signed)
            "dolar", "dollar", "usd" -> Scenario(usdIdrPct = signed)
            // Rupiah melemah = USD/IDR naik.
            "rupiah", "idr" -> Scenario(usdIdrPct = -signed)
            else -> null
        }
    }
}
