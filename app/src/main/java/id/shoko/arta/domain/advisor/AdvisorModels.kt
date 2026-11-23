package id.shoko.arta.domain.advisor

import id.shoko.arta.domain.analytics.Scenario
import kotlinx.serialization.Serializable

enum class AdvisorIntent { CONCENTRATION, TARGET, USD_EXPOSURE, SIMULATION, GREETING, FREE }

/** Kartu yang ditanam di jawaban Advisor (disimpan sebagai card_json). */
@Serializable
data class SimulationCard(
    val label: String,
    val cryptoPct: Int,
    val ihsgPct: Int,
    val goldPct: Int,
    val usdIdrPct: Int,
    /** Angka disimpan sebagai string desimal (BigDecimal.toPlainString()). */
    val before: String,
    val after: String,
    val diffPct: String,
) {
    val scenario: Scenario get() = Scenario(cryptoPct, ihsgPct, goldPct, usdIdrPct)
}

data class AdvisorReply(
    val text: String,
    val card: SimulationCard? = null,
    val usedLlm: Boolean = false,
)

data class ParsedQuestion(val intent: AdvisorIntent, val scenario: Scenario? = null)
